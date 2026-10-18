package dev.philiox.axiomaddons.tools;

import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.RegionHelper;
import dev.philiox.axiomaddons.ShapeSmoothing;
import dev.philiox.axiomaddons.mixin.ChunkRenderOverriderAccessor;
import imgui.moulberry92.ImGui;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Turns the build in Axiom's selection into a ruin: collapsed tops, holes, broken glass and doors, rubble on the
 * ground, cracked and mossy blocks, moss and vines. The result is shown in the world before it is applied.
 */
public class RuinTool extends AddonTool {

    private static final String OVERRIDE_ID = "Philiox Ruins";
    private static final long MAX_VOLUME = 2_000_000L;
    private static final int RUBBLE_SPREAD = 3;

    private final float[] decay = {0.5f};
    private final float[] collapse = {0.6f};
    private final float[] holes = {0.4f};
    private final float[] rubble = {0.5f};
    private final float[] weathering = {0.5f};
    private final float[] overgrowth = {0.4f};
    private boolean breakFragile = true;
    private boolean livePreview = true;
    private final int[] variant = {1};

    private final Long2ObjectOpenHashMap<BlockState> changes = new Long2ObjectOpenHashMap<>();
    private SelectionBuffer lastSelection = null;
    private boolean dirty = true;
    private boolean overriding = false;
    private int removedCount = 0;
    private int addedCount = 0;
    private boolean tooLarge = false;

    @Override
    protected String id() {
        return "ruins";
    }

    @Override
    public String name() {
        return t("tool.ruins");
    }

    @Override
    public char iconChar() {
        // Axiom's shatter tool icon
        return (char) 59665;
    }

    @Override
    public void reset() {
        super.reset();
        this.releaseOverride();
        this.changes.clear();
        this.lastSelection = null;
        this.dirty = true;
    }

    @Override
    public void toolDeselected() {
        this.releaseOverride();
        // Show the preview again when the tool is picked again
        this.dirty = true;
    }

    @Override
    public UserAction.ActionResult callAction(UserAction action, Object data) {
        if (action == UserAction.ENTER && !this.changes.isEmpty()) {
            this.apply();
            return UserAction.ActionResult.USED_STOP;
        }
        return UserAction.ActionResult.NOT_HANDLED;
    }

    @Override
    public String listenForEnter() {
        return this.changes.isEmpty() ? null : t("ruins.apply");
    }

    @Override
    public void render(AxiomWorldRenderContext rc) {
        SelectionBuffer selection = Selection.getSelectionBuffer();
        if (selection != this.lastSelection) {
            this.lastSelection = selection;
            this.dirty = true;
        }
        if (this.dirty) {
            this.dirty = false;
            this.compute(selection);
            this.updateOverride();
        }
    }

    // ---- Preview ----

    /** Shows the ruin in the world through Axiom's chunk render overrider, if nobody else is using it. */
    private void updateOverride() {
        this.releaseOverride();
        if (!this.livePreview || this.changes.isEmpty()) {
            return;
        }
        try {
            if (ChunkRenderOverriderAccessor.philiox$getLockedBy() != null) {
                return;
            }
            ChunkRenderOverrider.acquire(OVERRIDE_ID);
            this.overriding = true;
            for (Long2ObjectMap.Entry<BlockState> entry : this.changes.long2ObjectEntrySet()) {
                long key = entry.getLongKey();
                ChunkRenderOverrider.setBlock(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key), entry.getValue());
            }
        } catch (Throwable e) {
            // Another tool holds the overrider, then there is just no live preview
            this.overriding = false;
        }
    }

    private void releaseOverride() {
        if (this.overriding) {
            try {
                ChunkRenderOverrider.release(OVERRIDE_ID);
            } catch (Throwable ignored) {
                // Already released by Axiom
            }
            this.overriding = false;
        }
    }

    private void apply() {
        this.releaseOverride();
        ChunkedBlockRegion region = new ChunkedBlockRegion();
        for (Long2ObjectMap.Entry<BlockState> entry : this.changes.long2ObjectEntrySet()) {
            long key = entry.getLongKey();
            region.addBlock(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key), entry.getValue());
        }
        RegionHelper.pushBlockRegionChange(region, t("history.ruins"));
        // Keep the selection, but do not ruin the ruin again until something is changed
        this.changes.clear();
        this.removedCount = 0;
        this.addedCount = 0;
    }

    // ---- Generation ----

    private static boolean isFragile(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.contains("glass") || path.endsWith("_door") || path.contains("torch") || path.contains("lantern")
            || path.contains("candle") || path.endsWith("_bed") || path.contains("banner") || path.contains("flower_pot");
    }

    private static boolean solid(@Nullable BlockState state) {
        return state != null && !state.isAir() && !state.canBeReplaced();
    }

    private void compute(SelectionBuffer selection) {
        this.changes.clear();
        this.removedCount = 0;
        this.addedCount = 0;
        this.tooLarge = false;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || selection == null || selection.isEmpty()) {
            return;
        }
        BlockPos min = selection.min();
        BlockPos max = selection.max();
        long volume = (long) (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        if (volume > MAX_VOLUME) {
            this.tooLarge = true;
            return;
        }

        long seed = this.variant[0] * 2862933555777941757L;
        Random random = new Random(seed);
        double master = 0.5 + this.decay[0];
        double collapse = Math.clamp(this.collapse[0] * master, 0, 1.5);
        double holes = Math.clamp(this.holes[0] * master, 0, 1.5);
        int height = max.getY() - min.getY() + 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();

        // 1. Decide which blocks break away
        LongOpenHashSet removed = new LongOpenHashSet();
        LongArrayList remaining = new LongArrayList();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                // Collapsed top: every column breaks off at a different height
                double cutNoise = 0.5 + 0.5 * WaveGenerator.noise3(x / 8.0, 0, z / 8.0, seed);
                double cut = max.getY() + 1 - collapse * height * (0.25 + 0.75 * cutNoise);
                for (int y = min.getY(); y <= max.getY(); y++) {
                    if (!selection.contains(x, y, z)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (state.isAir()) {
                        continue;
                    }
                    long key = pos.asLong();
                    double h = (y - min.getY()) / (double) height;
                    boolean breaks = false;
                    if (this.breakFragile && isFragile(state)) {
                        breaks = random.nextDouble() < 0.85;
                    }
                    if (!breaks && y > cut + (random.nextDouble() - 0.5) * 2) {
                        breaks = true;
                    }
                    if (!breaks && holes > 0) {
                        // Organic holes, more of them higher up
                        double n = 0.5 + 0.5 * WaveGenerator.noise3(x / 5.0, y / 5.0, z / 5.0, seed + 1);
                        breaks = n > 1 - holes * 0.55 * (0.4 + 0.6 * h);
                    }
                    if (!breaks) {
                        breaks = random.nextDouble() < this.decay[0] * 0.08 * (0.3 + h);
                    }
                    if (breaks) {
                        removed.add(key);
                    } else {
                        remaining.add(key);
                    }
                }
            }
        }
        // Loose single blocks without any neighbour fall down as well
        LongArrayList kept = new LongArrayList();
        for (int i = 0; i < remaining.size(); i++) {
            long key = remaining.getLong(i);
            int x = BlockPos.getX(key);
            int y = BlockPos.getY(key);
            int z = BlockPos.getZ(key);
            boolean supported = y == min.getY();
            for (Direction direction : Direction.values()) {
                long neighbour = BlockPos.asLong(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());
                if (!removed.contains(neighbour) && solid(level.getBlockState(pos.set(neighbour)))) {
                    supported = true;
                    break;
                }
            }
            if (supported) {
                kept.add(key);
            } else {
                removed.add(key);
            }
        }
        for (long key : removed) {
            this.changes.put(key, air);
        }
        this.removedCount = removed.size();

        // 2. Cracked and mossy blocks, broken wall tops become slabs
        for (int i = 0; i < kept.size(); i++) {
            long key = kept.getLong(i);
            BlockState state = level.getBlockState(pos.set(key));
            int x = BlockPos.getX(key);
            int y = BlockPos.getY(key);
            int z = BlockPos.getZ(key);
            long above = BlockPos.asLong(x, y + 1, z);
            boolean openAbove = removed.contains(above) || level.getBlockState(pos.set(x, y + 1, z)).isAir();
            BlockState result = null;
            if (openAbove && removed.contains(above) && random.nextDouble() < 0.35) {
                ShapeSmoothing.Variants variants = ShapeSmoothing.variantsFor(state);
                if (variants != null && variants.slab() != null) {
                    result = variants.slab().defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
                }
            }
            if (result == null && random.nextDouble() < this.weathering[0] * 0.6) {
                result = WeatheringTool.agedVariant(state, random);
            }
            if (result != null) {
                this.changes.put(key, result);
            }
        }

        // 3. Rubble: part of the fallen blocks piles up on the ground nearby
        if (this.rubble[0] > 0) {
            for (long key : removed) {
                if (random.nextDouble() >= this.rubble[0] * 0.5) {
                    continue;
                }
                BlockState fallen = level.getBlockState(pos.set(key));
                if (isFragile(fallen) || !solid(fallen)) {
                    continue;
                }
                int x = BlockPos.getX(key) + random.nextInt(RUBBLE_SPREAD * 2 + 1) - RUBBLE_SPREAD;
                int z = BlockPos.getZ(key) + random.nextInt(RUBBLE_SPREAD * 2 + 1) - RUBBLE_SPREAD;
                int y = this.findGround(level, removed, x, BlockPos.getY(key), z, min.getY() - 4, pos);
                if (y == Integer.MIN_VALUE) {
                    continue;
                }
                this.changes.put(BlockPos.asLong(x, y, z), this.rubbleBlock(fallen, random));
                this.addedCount++;
            }
        }

        // 4. Moss on top of the remains and vines hanging down the walls
        if (this.overgrowth[0] > 0) {
            for (int i = 0; i < kept.size(); i++) {
                long key = kept.getLong(i);
                int x = BlockPos.getX(key);
                int y = BlockPos.getY(key);
                int z = BlockPos.getZ(key);
                BlockState state = this.current(level, x, y, z, pos);
                if (!solid(state) || !state.isCollisionShapeFullBlock(level, pos.set(x, y, z))) {
                    continue;
                }
                if (this.isAir(level, x, y + 1, z, pos) && random.nextDouble() < this.overgrowth[0] * 0.35) {
                    this.changes.put(BlockPos.asLong(x, y + 1, z), Blocks.MOSS_CARPET.defaultBlockState());
                    this.addedCount++;
                }
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    if (random.nextDouble() >= this.overgrowth[0] * 0.06) {
                        continue;
                    }
                    int vx = x + side.getStepX();
                    int vz = z + side.getStepZ();
                    BooleanProperty attach = vineProperty(side.getOpposite());
                    int length = 1 + random.nextInt(4);
                    for (int step = 0; step < length; step++) {
                        int vy = y - step;
                        if (!this.isAir(level, vx, vy, vz, pos)) {
                            break;
                        }
                        this.changes.put(BlockPos.asLong(vx, vy, vz), Blocks.VINE.defaultBlockState().setValue(attach, true));
                        this.addedCount++;
                    }
                }
            }
        }
    }

    private static BooleanProperty vineProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> BlockStateProperties.NORTH;
            case SOUTH -> BlockStateProperties.SOUTH;
            case EAST -> BlockStateProperties.EAST;
            default -> BlockStateProperties.WEST;
        };
    }

    /** The block as it will be after the changes so far. */
    private BlockState current(ClientLevel level, int x, int y, int z, BlockPos.MutableBlockPos pos) {
        BlockState changed = this.changes.get(BlockPos.asLong(x, y, z));
        return changed != null ? changed : level.getBlockState(pos.set(x, y, z));
    }

    private boolean isAir(ClientLevel level, int x, int y, int z, BlockPos.MutableBlockPos pos) {
        BlockState state = this.current(level, x, y, z, pos);
        return state.isAir();
    }

    /** First free spot from the start height downwards that has something solid below it. */
    private int findGround(ClientLevel level, LongOpenHashSet removed, int x, int startY, int z, int minY,
                           BlockPos.MutableBlockPos pos) {
        for (int y = startY; y > minY; y--) {
            BlockState here = this.current(level, x, y, z, pos);
            BlockState below = this.current(level, x, y - 1, z, pos);
            if ((here.isAir() || here.canBeReplaced()) && solid(below) && !removed.contains(BlockPos.asLong(x, y - 1, z))) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private BlockState rubbleBlock(BlockState fallen, Random random) {
        ShapeSmoothing.Variants variants = ShapeSmoothing.variantsFor(fallen);
        double roll = random.nextDouble();
        if (variants != null && variants.slab() != null && roll < 0.45) {
            return variants.slab().defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
        }
        if (variants != null && variants.stairs() != null && roll < 0.65) {
            Direction facing = Direction.from2DDataValue(random.nextInt(4));
            return variants.stairs().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        }
        BlockState aged = WeatheringTool.agedVariant(fallen.getBlock().defaultBlockState(), random);
        return aged != null && roll < 0.85 ? aged : fallen.getBlock().defaultBlockState();
    }

    // ---- Options ----

    @Override
    public void displayImguiOptions() {
        ImGuiHelper.separatorWithText(this.name());
        boolean changed = ImGui.sliderFloat(t("ruins.decay"), this.decay, 0.0f, 1.0f, "%.2f");
        changed |= ImGui.sliderFloat(t("ruins.collapse"), this.collapse, 0.0f, 1.0f, "%.2f");
        changed |= ImGui.sliderFloat(t("ruins.holes"), this.holes, 0.0f, 1.0f, "%.2f");
        changed |= ImGui.sliderFloat(t("ruins.rubble"), this.rubble, 0.0f, 1.0f, "%.2f");
        changed |= ImGui.sliderFloat(t("ruins.weathering"), this.weathering, 0.0f, 1.0f, "%.2f");
        changed |= ImGui.sliderFloat(t("ruins.overgrowth"), this.overgrowth, 0.0f, 1.0f, "%.2f");
        if (ImGui.checkbox(t("ruins.break_fragile"), this.breakFragile)) {
            this.breakFragile = !this.breakFragile;
            changed = true;
        }
        changed |= ImGui.sliderInt(t("wave.variant"), this.variant, 1, 100);
        if (ImGui.button(t("tree.reroll"))) {
            this.variant[0] = ThreadLocalRandom.current().nextInt(1, 101);
            changed = true;
        }
        if (ImGui.checkbox(t("ruins.live_preview"), this.livePreview)) {
            this.livePreview = !this.livePreview;
            changed = true;
        }

        SelectionBuffer selection = Selection.getSelectionBuffer();
        if (this.tooLarge) {
            ImGui.textWrapped(t("ruins.too_large"));
        } else if (selection == null || selection.isEmpty()) {
            ImGui.textWrapped(t("gradient.no_selection"));
        } else {
            ImGui.textWrapped(t("ruins.count", this.removedCount, this.addedCount));
            if (this.livePreview && !this.overriding && !this.changes.isEmpty()) {
                ImGui.textWrapped(t("ruins.no_preview"));
            }
            if (!this.changes.isEmpty() && ImGui.button(t("ruins.apply"))) {
                this.apply();
            }
            ImGui.sameLine();
            if (ImGui.button(t("gradient.refresh"))) {
                changed = true;
            }
        }
        ImGui.textWrapped(t("ruins.hint"));
        if (changed) {
            this.dirty = true;
        }
    }

    @Override
    public void writeSettings(CompoundTag tag) {
        tag.putFloat("Decay", this.decay[0]);
        tag.putFloat("Collapse", this.collapse[0]);
        tag.putFloat("Holes", this.holes[0]);
        tag.putFloat("Rubble", this.rubble[0]);
        tag.putFloat("Weathering", this.weathering[0]);
        tag.putFloat("Overgrowth", this.overgrowth[0]);
        tag.putBoolean("BreakFragile", this.breakFragile);
        tag.putBoolean("LivePreview", this.livePreview);
        tag.putInt("Variant", this.variant[0]);
    }

    @Override
    public void loadSettings(CompoundTag tag) {
        this.decay[0] = tag.getFloatOr("Decay", 0.5f);
        this.collapse[0] = tag.getFloatOr("Collapse", 0.6f);
        this.holes[0] = tag.getFloatOr("Holes", 0.4f);
        this.rubble[0] = tag.getFloatOr("Rubble", 0.5f);
        this.weathering[0] = tag.getFloatOr("Weathering", 0.5f);
        this.overgrowth[0] = tag.getFloatOr("Overgrowth", 0.4f);
        this.breakFragile = tag.getBooleanOr("BreakFragile", true);
        this.livePreview = tag.getBooleanOr("LivePreview", true);
        this.variant[0] = tag.getIntOr("Variant", 1);
        this.dirty = true;
    }

}
