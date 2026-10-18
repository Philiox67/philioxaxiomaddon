package dev.philiox.axiomaddons.tools;

import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.block_maps.BlockColourMap;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.tools.Tool;
import imgui.moulberry92.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Fills Axiom's selection with a colour gradient from block A to block B. In between it picks the blocks whose
 * texture colour (Lab) matches best, so e.g. white concrete to black wool gives a smooth grey ramp.
 */
public class GradientFillTool extends AddonTool {

    private static final int DIRECTION_UP = 0;
    private static final int DIRECTION_X = 1;
    private static final int DIRECTION_Z = 2;
    private static final int DIRECTION_RADIAL = 3;
    private static final String[] DIRECTION_KEYS = {"up", "x", "z", "radial"};

    private static final int STEPS = 256;
    private static final long MAX_VOLUME = 4_000_000L;
    // Many candidates, because the filters can drop a lot of them
    private static final int CANDIDATES = 64;

    private BlockState blockA = Blocks.WHITE_CONCRETE.defaultBlockState();
    private BlockState blockB = Blocks.BLACK_CONCRETE.defaultBlockState();
    private final int[] direction = {DIRECTION_UP};
    private boolean reverse = false;
    private final float[] dither = {0.25f};
    private boolean onlyExisting = true;
    private boolean fullBlocksOnly = true;

    private SelectionBuffer lastSelection = null;
    private int lastFilterVersion = -1;
    private boolean dirty = true;
    private boolean tooLarge = false;

    @Override
    protected String id() {
        return "gradient_fill";
    }

    @Override
    public String name() {
        return t("tool.gradient_fill");
    }

    @Override
    public char iconChar() {
        // Axiom's gradient painter icon
        return (char) 59674;
    }

    @Override
    public void reset() {
        super.reset();
        this.lastSelection = null;
        this.dirty = true;
    }

    @Override
    public UserAction.ActionResult callAction(UserAction action, Object data) {
        if (action == UserAction.ENTER && !this.preview.isEmpty()) {
            this.apply();
            return UserAction.ActionResult.USED_STOP;
        }
        return UserAction.ActionResult.NOT_HANDLED;
    }

    @Override
    public String listenForEnter() {
        return this.preview.isEmpty() ? null : t("gradient.apply");
    }

    private void apply() {
        this.commitPreview("history.gradient");
        this.dirty = true;
    }

    @Override
    public void render(AxiomWorldRenderContext rc) {
        SelectionBuffer selection = Selection.getSelectionBuffer();
        if (selection != this.lastSelection || BlockFilter.version() != this.lastFilterVersion) {
            this.lastSelection = selection;
            this.lastFilterVersion = BlockFilter.version();
            this.dirty = true;
        }
        if (this.dirty) {
            this.dirty = false;
            this.preview.clear();
            this.compute(selection);
        }
        this.renderPreview(rc);
    }

    private void compute(SelectionBuffer selection) {
        this.tooLarge = false;
        if (selection == null || selection.isEmpty()) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        Vec3 labA = BlockColourMap.getLab(this.blockA.getBlock());
        Vec3 labB = BlockColourMap.getLab(this.blockB.getBlock());
        if (level == null || labA == null || labB == null) {
            return;
        }
        BlockPos min = selection.min();
        BlockPos max = selection.max();
        long volume = (long) (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        if (volume > MAX_VOLUME) {
            this.tooLarge = true;
            return;
        }

        // Blocks along the gradient, looked up once per step
        int flags = BlockColourMap.FLAG_SOLID | BlockColourMap.FLAG_OPAQUE | BlockColourMap.FLAG_FULL_CUBE
            | BlockColourMap.FLAG_NO_ORES | BlockColourMap.FLAG_NO_GLAZED_TERRACOTTA | BlockColourMap.FLAG_NO_TILE_ENTITIES;
        BlockState[] ramp = new BlockState[STEPS];
        for (int i = 0; i < STEPS; i++) {
            double t = i / (double) (STEPS - 1);
            if (i == 0) {
                ramp[i] = this.blockA;
            } else if (i == STEPS - 1) {
                ramp[i] = this.blockB;
            } else {
                // Several candidates, so blacklisted and custom blocks can be skipped
                BlockState nearest = BlockFilter.firstAllowed(BlockColourMap.getNearestLabN(
                    labA.x + (labB.x - labA.x) * t, labA.y + (labB.y - labA.y) * t, labA.z + (labB.z - labA.z) * t,
                    CANDIDATES, flags), this.fullBlocksOnly);
                ramp[i] = nearest != null ? nearest : (t < 0.5 ? this.blockA : this.blockB);
            }
        }

        BlockPos center = selection.center();
        double maxRadius = 0.5 * Math.sqrt(sq(max.getX() - min.getX()) + sq(max.getY() - min.getY()) + sq(max.getZ() - min.getZ()));
        Random random = new Random(1234);
        float ditherAmount = this.dither[0];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    if (!selection.contains(x, y, z)) {
                        continue;
                    }
                    if (this.onlyExisting && level.getBlockState(pos.set(x, y, z)).isAir()) {
                        continue;
                    }
                    double t = switch (this.direction[0]) {
                        case DIRECTION_X -> fraction(x, min.getX(), max.getX());
                        case DIRECTION_Z -> fraction(z, min.getZ(), max.getZ());
                        case DIRECTION_RADIAL -> maxRadius <= 0 ? 0 : Math.sqrt(sq(x - center.getX()) + sq(y - center.getY()) + sq(z - center.getZ())) / maxRadius;
                        default -> fraction(y, min.getY(), max.getY());
                    };
                    if (this.reverse) {
                        t = 1 - t;
                    }
                    t += (random.nextDouble() - 0.5) * ditherAmount;
                    int index = (int) Math.round(Math.clamp(t, 0, 1) * (STEPS - 1));
                    this.preview.addBlock(x, y, z, ramp[index]);
                }
            }
        }
    }

    private static double fraction(int value, int min, int max) {
        return max == min ? 0 : (value - min) / (double) (max - min);
    }

    private static double sq(double value) {
        return value * value;
    }

    @Override
    public void displayImguiOptions() {
        ImGuiHelper.separatorWithText(this.name());

        ImGui.text(t("gradient.block_a"));
        ImGui.sameLine();
        if (ImGuiHelper.blockStateButton(ServerCustomBlocks.getCustomOrVanillaStateFor(this.blockA), 1, 32)) {
            this.blockA = Tool.getActiveBlock();
            this.dirty = true;
        }
        ImGui.sameLine();
        ImGui.text(t("gradient.block_b"));
        ImGui.sameLine();
        if (ImGuiHelper.blockStateButton(ServerCustomBlocks.getCustomOrVanillaStateFor(this.blockB), 2, 32)) {
            this.blockB = Tool.getActiveBlock();
            this.dirty = true;
        }
        ImGui.textWrapped(t("gradient.pick_hint"));
        if (ImGui.button(t("gradient.swap"))) {
            BlockState swap = this.blockA;
            this.blockA = this.blockB;
            this.blockB = swap;
            this.dirty = true;
        }

        boolean changed = TreeTool.combo("gradient.direction", this.direction, DIRECTION_KEYS, "gradient.direction.");
        if (ImGui.checkbox(t("gradient.reverse"), this.reverse)) {
            this.reverse = !this.reverse;
            changed = true;
        }
        changed |= ImGui.sliderFloat(t("gradient.dither"), this.dither, 0.0f, 1.0f, "%.2f");
        if (ImGui.checkbox(t("gradient.only_existing"), this.onlyExisting)) {
            this.onlyExisting = !this.onlyExisting;
            changed = true;
        }
        if (ImGui.checkbox(t("gradient.full_blocks"), this.fullBlocksOnly)) {
            this.fullBlocksOnly = !this.fullBlocksOnly;
            changed = true;
        }
        changed |= BlockFilter.displayOptions();
        if (changed) {
            this.dirty = true;
        }

        if (this.tooLarge) {
            ImGui.textWrapped(t("gradient.too_large"));
        } else if (this.lastSelection == null || this.lastSelection.isEmpty()) {
            ImGui.textWrapped(t("gradient.no_selection"));
        } else {
            if (ImGui.button(t("gradient.apply"))) {
                this.apply();
            }
            ImGui.sameLine();
            if (ImGui.button(t("gradient.refresh"))) {
                this.dirty = true;
            }
        }
    }

    @Override
    public void writeSettings(CompoundTag tag) {
        tag.putInt("Direction", this.direction[0]);
        tag.putBoolean("Reverse", this.reverse);
        tag.putFloat("Dither", this.dither[0]);
        tag.putBoolean("OnlyExisting", this.onlyExisting);
        tag.putBoolean("FullBlocksOnly", this.fullBlocksOnly);
    }

    @Override
    public void loadSettings(CompoundTag tag) {
        this.direction[0] = Math.clamp(tag.getIntOr("Direction", DIRECTION_UP), 0, DIRECTION_KEYS.length - 1);
        this.reverse = tag.getBooleanOr("Reverse", false);
        this.dither[0] = tag.getFloatOr("Dither", 0.25f);
        this.onlyExisting = tag.getBooleanOr("OnlyExisting", true);
        this.fullBlocksOnly = tag.getBooleanOr("FullBlocksOnly", true);
        this.dirty = true;
    }

}
