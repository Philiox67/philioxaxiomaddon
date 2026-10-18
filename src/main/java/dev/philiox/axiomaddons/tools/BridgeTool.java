package dev.philiox.axiomaddons.tools;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.tools.Tool;
import imgui.moulberry92.ImGui;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import org.jetbrains.annotations.Nullable;

/**
 * Right click two points, a bridge between them is previewed, Enter places it.
 * The deck uses Axiom's active block.
 */
public class BridgeTool extends AddonTool {

    private static final int STYLE_ARCH = 0;
    private static final int STYLE_SUSPENSION = 1;
    private static final int STYLE_FLAT = 2;
    private static final String[] STYLE_KEYS = {"arch", "suspension", "flat"};

    private static final String[] RAILING_KEYS = {"none", "oak_fence", "spruce_fence", "dark_oak_fence",
        "stone_brick_wall", "cobblestone_wall", "iron_bars", "deck"};
    private static final Block[] RAILING_BLOCKS = {null, Blocks.OAK_FENCE, Blocks.SPRUCE_FENCE, Blocks.DARK_OAK_FENCE,
        Blocks.STONE_BRICK_WALL, Blocks.COBBLESTONE_WALL, Blocks.IRON_BARS, null};
    private static final int RAILING_NONE = 0;
    private static final int RAILING_DECK = 7;

    private static final int MAX_PILLAR_DEPTH = 64;

    private final int[] style = {STYLE_ARCH};
    private final int[] width = {3};
    private final float[] curveHeight = {4.0f};
    private final int[] railing = {1};
    private boolean pillars = false;
    private final int[] pillarSpacing = {8};

    private @Nullable BlockPos start = null;
    private @Nullable BlockPos end = null;
    private @Nullable BlockPos lastCursor = null;
    private BlockState lastDeckBlock = null;
    private boolean dirty = true;

    @Override
    protected String id() {
        return "bridge";
    }

    @Override
    public String name() {
        return t("tool.bridge");
    }

    @Override
    public char iconChar() {
        // Axiom's path tool icon
        return (char) 59648;
    }

    @Override
    public void reset() {
        super.reset();
        this.start = null;
        this.end = null;
        this.lastCursor = null;
    }

    @Override
    public UserAction.ActionResult callAction(UserAction action, Object data) {
        switch (action) {
            case RIGHT_MOUSE -> {
                RayCaster.RaycastResult hit = raycast();
                if (hit == null) {
                    return UserAction.ActionResult.NOT_HANDLED;
                }
                if (this.start == null || this.end != null) {
                    this.start = hit.blockPos();
                    this.end = null;
                } else {
                    this.end = hit.blockPos();
                }
                this.dirty = true;
                return UserAction.ActionResult.USED_STOP;
            }
            case ENTER -> {
                if (this.end != null && !this.preview.isEmpty()) {
                    this.commitPreview("history.bridge");
                    this.reset();
                    return UserAction.ActionResult.USED_STOP;
                }
            }
            case ESCAPE, DELETE -> {
                if (this.start != null) {
                    this.reset();
                    return UserAction.ActionResult.USED_STOP;
                }
            }
            default -> {
            }
        }
        return UserAction.ActionResult.NOT_HANDLED;
    }

    @Override
    public String listenForEnter() {
        return this.end != null && !this.preview.isEmpty() ? t("bridge.place") : null;
    }

    @Override
    public String listenForEsc() {
        return this.start != null ? t("bridge.cancel") : null;
    }

    @Override
    public void render(AxiomWorldRenderContext rc) {
        RayCaster.RaycastResult hit = raycast();
        if (hit != null) {
            Tool.renderRaycastOverlay(rc, hit.blockPos());
        }
        if (this.start == null) {
            this.preview.clear();
            return;
        }

        // Before the second point is set, the bridge follows the cursor
        BlockPos target = this.end != null ? this.end : (hit != null ? hit.blockPos() : null);
        BlockState deckBlock = Tool.getActiveBlock();
        if (target != null && (this.dirty || !target.equals(this.lastCursor) || deckBlock != this.lastDeckBlock)) {
            this.dirty = false;
            this.lastCursor = target;
            this.lastDeckBlock = deckBlock;
            this.preview.clear();
            this.build(this.start, target, deckBlock);
        }
        this.renderPreview(rc);
    }

    private void build(BlockPos from, BlockPos to, BlockState deck) {
        double ax = from.getX() + 0.5;
        double az = from.getZ() + 0.5;
        double bx = to.getX() + 0.5;
        double bz = to.getZ() + 0.5;
        double dx = bx - ax;
        double dz = bz - az;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1) {
            return;
        }
        double dirX = dx / length;
        double dirZ = dz / length;
        double perpX = -dirZ;
        double perpZ = dirX;
        double halfWidth = (Math.max(1, this.width[0]) - 1) / 2.0;
        double sign = switch (this.style[0]) {
            case STYLE_SUSPENSION -> -1;
            case STYLE_FLAT -> 0;
            default -> 1;
        };
        double curve = sign * this.curveHeight[0];

        LongOpenHashSet deckPositions = new LongOpenHashSet();
        LongOpenHashSet railingPositions = new LongOpenHashSet();
        int[] lastLeft = null;
        int[] lastRight = null;

        int steps = (int) Math.ceil(length * 2);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double cx = ax + dx * t;
            double cz = az + dz * t;
            double cy = from.getY() + (to.getY() - from.getY()) * t + curve * 4 * t * (1 - t);
            int y = (int) Math.round(cy);

            for (double w = -halfWidth; w <= halfWidth + 1e-6; w += 0.5) {
                deckPositions.add(BlockPos.asLong((int) Math.floor(cx + perpX * w), y, (int) Math.floor(cz + perpZ * w)));
            }
            if (this.railing[0] != RAILING_NONE) {
                double edge = halfWidth + 1;
                lastLeft = this.addRailing(railingPositions, lastLeft,
                    (int) Math.floor(cx + perpX * edge), y + 1, (int) Math.floor(cz + perpZ * edge));
                lastRight = this.addRailing(railingPositions, lastRight,
                    (int) Math.floor(cx - perpX * edge), y + 1, (int) Math.floor(cz - perpZ * edge));
                // The railing posts stand on the deck
                deckPositions.add(BlockPos.asLong((int) Math.floor(cx + perpX * edge), y, (int) Math.floor(cz + perpZ * edge)));
                deckPositions.add(BlockPos.asLong((int) Math.floor(cx - perpX * edge), y, (int) Math.floor(cz - perpZ * edge)));
            }
        }

        LongIterator deckIterator = deckPositions.iterator();
        while (deckIterator.hasNext()) {
            long pos = deckIterator.nextLong();
            this.preview.addBlock(BlockPos.getX(pos), BlockPos.getY(pos), BlockPos.getZ(pos), deck);
        }

        if (this.railing[0] != RAILING_NONE) {
            Block railingBlock = this.railing[0] == RAILING_DECK ? deck.getBlock() : RAILING_BLOCKS[this.railing[0]];
            BlockState railingBase = this.railing[0] == RAILING_DECK ? deck : railingBlock.defaultBlockState();
            LongIterator railingIterator = railingPositions.iterator();
            while (railingIterator.hasNext()) {
                long pos = railingIterator.nextLong();
                int x = BlockPos.getX(pos);
                int y = BlockPos.getY(pos);
                int z = BlockPos.getZ(pos);
                if (deckPositions.contains(pos)) {
                    continue;
                }
                this.preview.addBlock(x, y, z, connect(railingBase, railingPositions, x, y, z));
            }
        }

        if (this.pillars) {
            this.buildPillars(ax, az, dx, dz, length, perpX, perpZ, halfWidth, from, to, curve, deck);
        }
    }

    /** Adds a railing block and fills diagonal steps so fences and walls stay connected. */
    private int[] addRailing(LongOpenHashSet positions, int @Nullable [] last, int x, int y, int z) {
        if (last != null && last[0] != x && last[2] != z) {
            positions.add(BlockPos.asLong(x, y, last[2]));
        }
        positions.add(BlockPos.asLong(x, y, z));
        return new int[] {x, y, z};
    }

    private void buildPillars(double ax, double az, double dx, double dz, double length, double perpX, double perpZ,
                              double halfWidth, BlockPos from, BlockPos to, double curve, BlockState deck) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        int spacing = Math.max(2, this.pillarSpacing[0]);
        for (double along = spacing; along < length - spacing / 2.0; along += spacing) {
            double t = along / length;
            double cx = ax + dx * t;
            double cz = az + dz * t;
            int y = (int) Math.round(from.getY() + (to.getY() - from.getY()) * t + curve * 4 * t * (1 - t));
            for (int side = -1; side <= 1; side += 2) {
                int px = (int) Math.floor(cx + perpX * halfWidth * side);
                int pz = (int) Math.floor(cz + perpZ * halfWidth * side);
                for (int depth = 1; depth <= MAX_PILLAR_DEPTH; depth++) {
                    BlockState existing = level.getBlockState(new BlockPos(px, y - depth, pz));
                    if (!existing.isAir() && !existing.canBeReplaced()) {
                        break;
                    }
                    this.preview.addBlock(px, y - depth, pz, deck);
                }
            }
        }
    }

    /** Sets fence, bars and wall connections towards neighbouring railing blocks. */
    private static BlockState connect(BlockState state, LongOpenHashSet railing, int x, int y, int z) {
        Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        BooleanProperty[] fenceProperties = {BlockStateProperties.NORTH, BlockStateProperties.EAST,
            BlockStateProperties.SOUTH, BlockStateProperties.WEST};
        EnumProperty<?>[] wallProperties = {BlockStateProperties.NORTH_WALL, BlockStateProperties.EAST_WALL,
            BlockStateProperties.SOUTH_WALL, BlockStateProperties.WEST_WALL};
        for (int i = 0; i < directions.length; i++) {
            Direction direction = directions[i];
            boolean neighbour = railing.contains(BlockPos.asLong(x + direction.getStepX(), y, z + direction.getStepZ()));
            if (state.hasProperty(fenceProperties[i])) {
                state = state.setValue(fenceProperties[i], neighbour);
            } else if (state.hasProperty(wallProperties[i])) {
                state = setWallSide(state, wallProperties[i], neighbour ? WallSide.LOW : WallSide.NONE);
            }
        }
        if (state.hasProperty(BlockStateProperties.UP)) {
            state = state.setValue(BlockStateProperties.UP, true);
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static BlockState setWallSide(BlockState state, EnumProperty<?> property, WallSide side) {
        return state.setValue((EnumProperty<WallSide>) property, side);
    }

    @Override
    public void displayImguiOptions() {
        ImGuiHelper.separatorWithText(this.name());
        boolean changed = TreeTool.combo("bridge.style", this.style, STYLE_KEYS, "bridge.style.");
        changed |= ImGui.sliderInt(t("bridge.width"), this.width, 1, 15);
        if (this.style[0] != STYLE_FLAT) {
            changed |= ImGui.sliderFloat(t("bridge.curve"), this.curveHeight, 0.0f, 32.0f, "%.1f");
        }
        changed |= TreeTool.combo("bridge.railing", this.railing, RAILING_KEYS, "bridge.railing.");
        if (ImGui.checkbox(t("bridge.pillars"), this.pillars)) {
            this.pillars = !this.pillars;
            changed = true;
        }
        if (this.pillars) {
            changed |= ImGui.sliderInt(t("bridge.pillar_spacing"), this.pillarSpacing, 3, 32);
        }
        ImGui.textWrapped(t(this.start == null ? "bridge.hint.start" : this.end == null ? "bridge.hint.end" : "bridge.hint.place"));
        if (changed) {
            this.dirty = true;
        }
    }

    @Override
    public void writeSettings(CompoundTag tag) {
        tag.putInt("Style", this.style[0]);
        tag.putInt("Width", this.width[0]);
        tag.putFloat("Curve", this.curveHeight[0]);
        tag.putInt("Railing", this.railing[0]);
        tag.putBoolean("Pillars", this.pillars);
        tag.putInt("PillarSpacing", this.pillarSpacing[0]);
    }

    @Override
    public void loadSettings(CompoundTag tag) {
        this.style[0] = tag.getIntOr("Style", STYLE_ARCH);
        this.width[0] = tag.getIntOr("Width", 3);
        this.curveHeight[0] = tag.getFloatOr("Curve", 4.0f);
        this.railing[0] = Math.clamp(tag.getIntOr("Railing", 1), 0, RAILING_KEYS.length - 1);
        this.pillars = tag.getBooleanOr("Pillars", false);
        this.pillarSpacing[0] = tag.getIntOr("PillarSpacing", 8);
        this.dirty = true;
    }

}
