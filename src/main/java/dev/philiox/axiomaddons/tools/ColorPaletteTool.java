package dev.philiox.axiomaddons.tools;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.block_maps.BlockColourMap;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.tools.Tool;
import dev.philiox.axiomaddons.mixin.EditorUIAccessor;
import imgui.moulberry92.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shows all blocks sorted by colour and the best matching blocks for a reference block.
 * Clicking a block makes it Axiom's active block, right clicking a block in the world makes it the reference.
 */
public class ColorPaletteTool extends AddonTool {

    private static final int BUTTON_SIZE = 32;
    private static final int GREY_CHROMA = 8;
    private static final int HUE_BUCKETS = 12;

    private record ColouredBlock(BlockState state, double lightness, double chroma, double hue) {
    }

    private final int[] matchCount = {16};
    private boolean onlyFullBlocks = true;

    private @Nullable List<ColouredBlock> sorted = null;
    private boolean sortedOnlyFull = true;
    private int sortedFilterVersion = -1;
    private @Nullable BlockState reference = null;

    @Override
    protected String id() {
        return "colour_palette";
    }

    @Override
    public String name() {
        return t("tool.colour_palette");
    }

    @Override
    public char iconChar() {
        // Axiom's painter tool icon
        return (char) 59664;
    }

    @Override
    public UserAction.ActionResult callAction(UserAction action, Object data) {
        if (action == UserAction.RIGHT_MOUSE) {
            RayCaster.RaycastResult hit = raycast();
            ClientLevel level = Minecraft.getInstance().level;
            if (hit != null && level != null) {
                this.reference = level.getBlockState(hit.blockPos());
                return UserAction.ActionResult.USED_STOP;
            }
        }
        return UserAction.ActionResult.NOT_HANDLED;
    }

    @Override
    public void render(AxiomWorldRenderContext rc) {
        RayCaster.RaycastResult hit = raycast();
        if (hit != null) {
            Tool.renderRaycastOverlay(rc, hit.blockPos());
        }
    }

    private List<ColouredBlock> sortedBlocks() {
        if (this.sorted == null || this.sortedOnlyFull != this.onlyFullBlocks || this.sortedFilterVersion != BlockFilter.version()) {
            this.sortedOnlyFull = this.onlyFullBlocks;
            this.sortedFilterVersion = BlockFilter.version();
            List<ColouredBlock> blocks = new ArrayList<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                BlockState state = block.defaultBlockState();
                if (state.isAir() || BlockColourMap.isHardIgnore(block) || !BlockFilter.isAllowed(state)) {
                    continue;
                }
                if (this.onlyFullBlocks && !BlockFilter.isFullBlock(state)) {
                    continue;
                }
                Vec3 lab = BlockColourMap.getLab(block);
                if (lab == null) {
                    continue;
                }
                double chroma = Math.sqrt(lab.y * lab.y + lab.z * lab.z);
                double hue = Math.atan2(lab.z, lab.y);
                blocks.add(new ColouredBlock(state, lab.x, chroma, hue < 0 ? hue + 2 * Math.PI : hue));
            }
            // Greys first from light to dark, then the colour wheel, each hue from light to dark
            blocks.sort(Comparator.comparingInt(ColorPaletteTool::hueBucket)
                .thenComparing(Comparator.comparingDouble(ColouredBlock::lightness).reversed()));
            this.sorted = blocks;
        }
        return this.sorted;
    }

    private static int hueBucket(ColouredBlock block) {
        if (block.chroma() < GREY_CHROMA) {
            return -1;
        }
        return (int) (block.hue() / (2 * Math.PI) * HUE_BUCKETS) % HUE_BUCKETS;
    }

    @Override
    public void displayImguiOptions() {
        ImGuiHelper.separatorWithText(t("palette.matching"));
        BlockState matchFor = this.reference != null ? this.reference : Tool.getActiveBlock();
        ImGui.textWrapped(t("palette.matching_for", matchFor.getBlock().getName().getString()));
        if (this.reference != null && ImGui.button(t("palette.use_active"))) {
            this.reference = null;
        }
        ImGui.sliderInt(t("palette.match_count"), this.matchCount, 4, 48);
        Vec3 lab = BlockColourMap.getLab(matchFor.getBlock());
        if (lab != null) {
            int flags = this.onlyFullBlocks
                ? BlockColourMap.FLAG_SOLID | BlockColourMap.FLAG_OPAQUE | BlockColourMap.FLAG_FULL_CUBE
                : 0;
            // Ask for extra candidates, the filter may drop some
            List<BlockState> matches = BlockFilter.allowed(
                BlockColourMap.getNearestLabN(lab.x, lab.y, lab.z, Math.max(1, this.matchCount[0]) * 3 + 16, flags),
                this.onlyFullBlocks);
            this.blockGrid(matches.subList(0, Math.min(matches.size(), Math.max(1, this.matchCount[0]))), 100_000);
        } else {
            ImGui.textWrapped(t("palette.no_colour"));
        }

        ImGuiHelper.separatorWithText(t("palette.all_blocks"));
        if (ImGui.checkbox(t("palette.only_full"), this.onlyFullBlocks)) {
            this.onlyFullBlocks = !this.onlyFullBlocks;
        }
        BlockFilter.displayOptions();
        List<ColouredBlock> blocks = this.sortedBlocks();
        if (blocks.isEmpty()) {
            ImGui.textWrapped(t("palette.no_colour"));
            return;
        }
        List<BlockState> states = new ArrayList<>(blocks.size());
        for (ColouredBlock block : blocks) {
            states.add(block.state());
        }
        if (ImGui.beginChild("##philiox_palette", 0, 360, true)) {
            this.blockGrid(states, 200_000);
        }
        ImGui.endChild();
        ImGui.textWrapped(t("palette.hint"));
    }

    /** Wrapping grid of block buttons, a click sets Axiom's active block. */
    private void blockGrid(List<BlockState> states, int idOffset) {
        float available = ImGui.getContentRegionAvailX();
        int perRow = Math.max(1, (int) (available / (BUTTON_SIZE + 8)));
        for (int i = 0; i < states.size(); i++) {
            BlockState state = states.get(i);
            if (i % perRow != 0) {
                ImGui.sameLine();
            }
            if (ImGuiHelper.blockStateButton(ServerCustomBlocks.getCustomOrVanillaStateFor(state), idOffset + i, BUTTON_SIZE)) {
                EditorUIAccessor.philiox$getActiveBlockHistory().setActive(ServerCustomBlocks.getCustomOrVanillaStateFor(state));
            }
            if (ImGui.isItemHovered()) {
                ImGuiHelper.tooltip(state.getBlock().getName().getString());
            }
        }
    }

    @Override
    public void writeSettings(CompoundTag tag) {
        tag.putInt("MatchCount", this.matchCount[0]);
        tag.putBoolean("OnlyFullBlocks", this.onlyFullBlocks);
    }

    @Override
    public void loadSettings(CompoundTag tag) {
        this.matchCount[0] = tag.getIntOr("MatchCount", 16);
        this.onlyFullBlocks = tag.getBooleanOr("OnlyFullBlocks", true);
    }

}
