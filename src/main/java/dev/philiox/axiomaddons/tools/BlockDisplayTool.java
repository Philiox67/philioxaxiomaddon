package dev.philiox.axiomaddons.tools;

import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.packets.AxiomServerboundDeleteEntity;
import com.moulberry.axiom.packets.AxiomServerboundSpawnEntity;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Turns the blocks of Axiom's selection into block display entities, through Axiom's own entity packets.
 */
public class BlockDisplayTool extends AddonTool {

    public static final String TAG = "philiox_display";

    private static final int PER_PACKET = 128;
    private static final int PACKETS_PER_FRAME = 2;
    private static final int WARN_COUNT = 5000;
    private static final long MAX_VOLUME = 2_000_000L;

    private boolean onlyVisible = true;
    private boolean removeBlocks = true;
    private final float[] scale = {1.0f};

    private final ArrayDeque<List<AxiomServerboundSpawnEntity.SpawnEntry>> pending = new ArrayDeque<>();
    private List<UUID> lastSpawned = new ArrayList<>();
    private int sentCount = 0;
    private int totalCount = 0;

    private SelectionBuffer countedSelection = null;
    private int countedVisible = -1;
    private int countedBlocks = -1;
    private boolean countedOnlyVisible = true;

    @Override
    protected String id() {
        return "block_display";
    }

    @Override
    public String name() {
        return t("tool.block_display");
    }

    @Override
    public char iconChar() {
        // Axiom's stamp tool icon
        return (char) 59675;
    }

    @Override
    public EnumSet<AxiomPermission> requiredPermissions() {
        return EnumSet.of(AxiomPermission.TOOL, AxiomPermission.ENTITY_SPAWN);
    }

    @Override
    public UserAction.ActionResult callAction(UserAction action, Object data) {
        if (action == UserAction.ENTER && this.pending.isEmpty()) {
            this.convert();
            return UserAction.ActionResult.USED_STOP;
        }
        return UserAction.ActionResult.NOT_HANDLED;
    }

    @Override
    public String listenForEnter() {
        SelectionBuffer selection = Selection.getSelectionBuffer();
        return selection != null && !selection.isEmpty() && this.pending.isEmpty() ? t("display.convert") : null;
    }

    @Override
    public void render(AxiomWorldRenderContext rc) {
        // Spread big conversions over several frames, so the server is not flooded
        for (int i = 0; i < PACKETS_PER_FRAME && !this.pending.isEmpty(); i++) {
            List<AxiomServerboundSpawnEntity.SpawnEntry> batch = this.pending.poll();
            new AxiomServerboundSpawnEntity(batch).send();
            this.sentCount += batch.size();
        }
    }

    private static boolean convertible(BlockState state) {
        // Air, fluids, barriers and blocks drawn by a block entity renderer (chests, signs) show nothing as block display
        return !state.isAir() && state.getRenderShape() == RenderShape.MODEL;
    }

    private static boolean hidden(ClientLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            neighbour.setWithOffset(pos, direction);
            if (!level.getBlockState(neighbour).isSolidRender()) {
                return false;
            }
        }
        return true;
    }

    private void convert() {
        ClientLevel level = Minecraft.getInstance().level;
        SelectionBuffer selection = Selection.getSelectionBuffer();
        if (level == null || selection == null || selection.isEmpty()) {
            return;
        }
        BlockPos min = selection.min();
        BlockPos max = selection.max();
        if (volume(min, max) > MAX_VOLUME) {
            return;
        }
        double scale = Math.clamp(this.scale[0], 0.01f, 1f);
        ChunkedBlockRegion removal = new ChunkedBlockRegion();
        BlockState air = Blocks.AIR.defaultBlockState();
        List<UUID> spawned = new ArrayList<>();
        List<AxiomServerboundSpawnEntity.SpawnEntry> batch = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    if (!selection.contains(x, y, z)) {
                        continue;
                    }
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!convertible(state)) {
                        continue;
                    }
                    if (this.removeBlocks) {
                        removal.addBlock(x, y, z, air);
                    }
                    if (this.onlyVisible && hidden(level, pos)) {
                        continue;
                    }
                    UUID uuid = UUID.randomUUID();
                    Vec3 position = new Vec3(
                        min.getX() + (x - min.getX()) * scale,
                        min.getY() + (y - min.getY()) * scale,
                        min.getZ() + (z - min.getZ()) * scale);
                    batch.add(new AxiomServerboundSpawnEntity.SpawnEntry(uuid, position, 0, 0, null, displayTag(state, (float) scale)));
                    spawned.add(uuid);
                    if (batch.size() >= PER_PACKET) {
                        this.pending.add(batch);
                        batch = new ArrayList<>();
                    }
                }
            }
        }
        if (!batch.isEmpty()) {
            this.pending.add(batch);
        }
        if (spawned.isEmpty()) {
            return;
        }
        this.lastSpawned = spawned;
        this.sentCount = 0;
        this.totalCount = spawned.size();
        if (this.removeBlocks && !removal.isEmpty()) {
            RegionHelper.pushBlockRegionChange(removal, t("history.block_display"));
        }
    }

    private static CompoundTag displayTag(BlockState state, float scale) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:block_display");
        tag.put("block_state", NbtUtils.writeBlockState(state));

        CompoundTag transformation = new CompoundTag();
        transformation.put("left_rotation", floats(0, 0, 0, 1));
        transformation.put("right_rotation", floats(0, 0, 0, 1));
        transformation.put("translation", floats(0, 0, 0));
        transformation.put("scale", floats(scale, scale, scale));
        tag.put("transformation", transformation);

        ListTag tags = new ListTag();
        tags.add(StringTag.valueOf(TAG));
        tag.put("Tags", tags);
        return tag;
    }

    private static ListTag floats(float... values) {
        ListTag list = new ListTag();
        for (float value : values) {
            list.add(FloatTag.valueOf(value));
        }
        return list;
    }

    private void deleteLast() {
        if (this.lastSpawned.isEmpty()) {
            return;
        }
        this.pending.clear();
        for (int i = 0; i < this.lastSpawned.size(); i += 512) {
            new AxiomServerboundDeleteEntity(new ArrayList<>(this.lastSpawned.subList(i, Math.min(this.lastSpawned.size(), i + 512)))).send();
        }
        this.lastSpawned = new ArrayList<>();
        this.sentCount = 0;
        this.totalCount = 0;
    }

    private static long volume(BlockPos min, BlockPos max) {
        return (long) (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
    }

    /** Counts the blocks of the selection once per selection change, for the info text. */
    private void count(SelectionBuffer selection) {
        if (selection == this.countedSelection && this.countedOnlyVisible == this.onlyVisible) {
            return;
        }
        this.countedSelection = selection;
        this.countedOnlyVisible = this.onlyVisible;
        this.countedBlocks = -1;
        this.countedVisible = -1;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || selection == null || selection.isEmpty() || volume(selection.min(), selection.max()) > MAX_VOLUME) {
            return;
        }
        int blocks = 0;
        int visible = 0;
        BlockPos min = selection.min();
        BlockPos max = selection.max();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    if (!selection.contains(x, y, z)) {
                        continue;
                    }
                    pos.set(x, y, z);
                    if (!convertible(level.getBlockState(pos))) {
                        continue;
                    }
                    blocks++;
                    if (!this.onlyVisible || !hidden(level, pos)) {
                        visible++;
                    }
                }
            }
        }
        this.countedBlocks = blocks;
        this.countedVisible = visible;
    }

    @Override
    public void displayImguiOptions() {
        ImGuiHelper.separatorWithText(this.name());
        if (ImGui.checkbox(t("display.only_visible"), this.onlyVisible)) {
            this.onlyVisible = !this.onlyVisible;
        }
        if (ImGui.checkbox(t("display.remove_blocks"), this.removeBlocks)) {
            this.removeBlocks = !this.removeBlocks;
        }
        ImGui.sliderFloat(t("display.scale"), this.scale, 0.05f, 1.0f, "%.3f");

        SelectionBuffer selection = Selection.getSelectionBuffer();
        if (selection == null || selection.isEmpty()) {
            ImGui.textWrapped(t("gradient.no_selection"));
        } else if (volume(selection.min(), selection.max()) > MAX_VOLUME) {
            ImGui.textWrapped(t("display.too_large"));
        } else {
            this.count(selection);
            ImGui.textWrapped(t("display.count", this.countedVisible, this.countedBlocks));
            if (this.countedVisible > WARN_COUNT) {
                ImGui.textWrapped(t("display.warning"));
            }
            if (this.pending.isEmpty()) {
                if (ImGui.button(t("display.convert"))) {
                    this.convert();
                }
            }
        }

        if (!this.pending.isEmpty()) {
            ImGui.textWrapped(t("display.progress", this.sentCount, this.totalCount));
        }
        if (!this.lastSpawned.isEmpty()) {
            if (ImGui.button(t("display.delete_last", this.lastSpawned.size()))) {
                this.deleteLast();
            }
        }
        ImGui.textWrapped(t("display.hint", TAG));
    }

    @Override
    public void writeSettings(CompoundTag tag) {
        tag.putBoolean("OnlyVisible", this.onlyVisible);
        tag.putBoolean("RemoveBlocks", this.removeBlocks);
        tag.putFloat("Scale", this.scale[0]);
    }

    @Override
    public void loadSettings(CompoundTag tag) {
        this.onlyVisible = tag.getBooleanOr("OnlyVisible", true);
        this.removeBlocks = tag.getBooleanOr("RemoveBlocks", true);
        this.scale[0] = tag.getFloatOr("Scale", 1.0f);
    }

}
