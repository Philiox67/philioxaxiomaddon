package dev.philiox.axiomaddons.tools;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Places big ocean waves made of blocks: breaking waves with a curling lip and tube, or a rolling swell.
 * The wave is calculated once around the origin and only moved while the cursor moves.
 */
public class WaveTool extends AddonTool {

    private static final String[] TYPE_KEYS = {"breaking", "swell"};
    private static final String[] MATERIAL_KEYS = {"ocean", "tropical", "wool", "glass", "ice", "active"};
    private static final int MATERIAL_ACTIVE = 5;
    // Auto rotation snaps to this many degrees, so turning the camera does not rebuild the wave every frame
    private static final double ANGLE_STEP = 15;

    private final int[] type = {WaveGenerator.TYPE_BREAKING};
    private final int[] height = {18};
    private final int[] crestLength = {48};
    private final float[] backLength = {2.5f};
    private final float[] frontLength = {1.2f};
    private final float[] curl = {0.65f};
    private final float[] tubeSize = {0.42f};
    private final int[] lipThickness = {2};
    private boolean peel = false;
    private final int[] swellCrests = {3};
    private final float[] wavelength = {4.0f};
    private final float[] steepness = {0.5f};
    private final float[] endTaper = {0.25f};
    private final float[] variation = {0.15f};
    private final float[] chop = {0.3f};
    private final float[] foam = {0.5f};
    private final int[] material = {0};
    private boolean hollow = false;
    private final int[] shellThickness = {2};
    private final int[] baseDepth = {0};
    private boolean faceCamera = true;
    private final float[] angle = {0};
    private final int[] variant = {1};

    private ChunkedBlockRegion wave = new ChunkedBlockRegion();
    private BlockPos origin = null;
    private double builtAngle = Double.NaN;
    private BlockState builtActiveBlock = null;
    private boolean dirty = true;

    @Override
    protected String id() {
        return "wave";
    }

    @Override
    public String name() {
        return t("tool.wave");
    }

    @Override
    public char iconChar() {
        // Axiom's elevation tool icon
        return (char) 59671;
    }

    @Override
    public void reset() {
        super.reset();
        this.origin = null;
    }

    @Override
    public UserAction.ActionResult callAction(UserAction action, Object data) {
        if (action == UserAction.RIGHT_MOUSE) {
            if (this.origin != null && !this.wave.isEmpty()) {
                this.place();
            }
            return UserAction.ActionResult.USED_STOP;
        }
        return UserAction.ActionResult.NOT_HANDLED;
    }

    private void place() {
        ChunkedBlockRegion placed = new ChunkedBlockRegion();
        int ox = this.origin.getX();
        int oy = this.origin.getY();
        int oz = this.origin.getZ();
        this.wave.forEachEntry((x, y, z, state) -> placed.addBlock(x + ox, y + oy, z + oz, state));
        RegionHelper.pushBlockRegionChange(placed, t("history.wave"));
    }

    @Override
    public void render(AxiomWorldRenderContext rc) {
        RayCaster.RaycastResult hit = raycast();
        if (hit == null) {
            this.origin = null;
            return;
        }
        this.origin = hit.blockPos().relative(hit.direction());

        double wanted = this.angle[0];
        if (this.faceCamera && Minecraft.getInstance().player != null) {
            // The front faces the player, so the wave travels towards the camera
            double yaw = Minecraft.getInstance().player.getYRot() + 180;
            wanted = Math.round(yaw / ANGLE_STEP) * ANGLE_STEP;
        }
        BlockState active = Tool.getActiveBlock();
        boolean activeChanged = this.material[0] == MATERIAL_ACTIVE && active != this.builtActiveBlock;
        if (this.dirty || wanted != this.builtAngle || activeChanged) {
            this.dirty = false;
            this.builtAngle = wanted;
            this.builtActiveBlock = active;
            this.wave = new ChunkedBlockRegion();
            WaveGenerator.generate(this.wave, this.settings(active), wanted);
        }

        Tool.renderRaycastOverlay(rc, hit.blockPos());
        if (!this.wave.isEmpty()) {
            this.wave.render(rc, new Vec3(this.origin.getX(), this.origin.getY(), this.origin.getZ()), 0.75f, 0.25f);
        }
    }

    private WaveGenerator.Settings settings(BlockState active) {
        BlockState[] palette;
        if (this.material[0] == MATERIAL_ACTIVE) {
            palette = new BlockState[] {active, active, active,
                Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.WHITE_CONCRETE_POWDER.defaultBlockState()};
        } else {
            palette = WaveGenerator.PALETTES[Math.clamp(this.material[0], 0, WaveGenerator.PALETTES.length - 1)];
        }
        return new WaveGenerator.Settings(this.type[0], Math.max(2, this.height[0]), Math.max(2, this.crestLength[0]),
            this.backLength[0], this.frontLength[0], this.curl[0], this.tubeSize[0], Math.max(1, this.lipThickness[0]),
            this.peel, this.swellCrests[0], this.wavelength[0], this.steepness[0], this.endTaper[0], this.variation[0],
            this.chop[0], this.foam[0], this.hollow, this.shellThickness[0], this.baseDepth[0], palette,
            this.variant[0] * 6364136223846793005L);
    }

    @Override
    public void displayImguiOptions() {
        ImGuiHelper.separatorWithText(this.name());
        boolean changed = TreeTool.combo("wave.type", this.type, TYPE_KEYS, "wave.type.");
        changed |= ImGui.sliderInt(t("wave.height"), this.height, 4, 96);
        changed |= ImGui.sliderInt(t("wave.crest_length"), this.crestLength, 8, 256);

        if (this.type[0] == WaveGenerator.TYPE_BREAKING) {
            ImGuiHelper.separatorWithText(t("wave.section.breaking"));
            changed |= ImGui.sliderFloat(t("wave.curl"), this.curl, 0.0f, 1.0f, "%.2f");
            changed |= ImGui.sliderFloat(t("wave.tube_size"), this.tubeSize, 0.2f, 0.7f, "%.2f");
            changed |= ImGui.sliderInt(t("wave.lip_thickness"), this.lipThickness, 1, 10);
            changed |= ImGui.sliderFloat(t("wave.back_length"), this.backLength, 1.0f, 6.0f, "%.2f");
            changed |= ImGui.sliderFloat(t("wave.front_length"), this.frontLength, 0.4f, 3.0f, "%.2f");
            changed |= this.checkbox("wave.peel", this.peel, () -> this.peel = !this.peel);
        } else {
            ImGuiHelper.separatorWithText(t("wave.section.swell"));
            changed |= ImGui.sliderInt(t("wave.crests"), this.swellCrests, 1, 10);
            changed |= ImGui.sliderFloat(t("wave.wavelength"), this.wavelength, 1.0f, 8.0f, "%.2f");
            changed |= ImGui.sliderFloat(t("wave.steepness"), this.steepness, 0.0f, 1.0f, "%.2f");
        }

        ImGuiHelper.separatorWithText(t("wave.section.detail"));
        changed |= ImGui.sliderFloat(t("wave.end_taper"), this.endTaper, 0.0f, 0.5f, "%.2f");
        changed |= ImGui.sliderFloat(t("wave.variation"), this.variation, 0.0f, 0.6f, "%.2f");
        changed |= ImGui.sliderFloat(t("wave.chop"), this.chop, 0.0f, 1.0f, "%.2f");
        changed |= ImGui.sliderFloat(t("wave.foam"), this.foam, 0.0f, 1.0f, "%.2f");
        changed |= ImGui.sliderInt(t("wave.variant"), this.variant, 1, 100);
        if (ImGui.button(t("tree.reroll"))) {
            this.variant[0] = ThreadLocalRandom.current().nextInt(1, 101);
            changed = true;
        }

        ImGuiHelper.separatorWithText(t("wave.section.blocks"));
        changed |= TreeTool.combo("wave.material", this.material, MATERIAL_KEYS, "wave.material.");
        changed |= this.checkbox("wave.hollow", this.hollow, () -> this.hollow = !this.hollow);
        if (this.hollow) {
            changed |= ImGui.sliderInt(t("wave.shell_thickness"), this.shellThickness, 1, 4);
        }
        changed |= ImGui.sliderInt(t("wave.base_depth"), this.baseDepth, 0, 16);

        ImGuiHelper.separatorWithText(t("wave.section.direction"));
        changed |= this.checkbox("wave.face_camera", this.faceCamera, () -> this.faceCamera = !this.faceCamera);
        if (!this.faceCamera) {
            changed |= ImGui.sliderFloat(t("wave.angle"), this.angle, 0.0f, 359.0f, "%.0f");
        }
        ImGui.textWrapped(t("wave.hint"));
        if (changed) {
            this.dirty = true;
        }
    }

    private boolean checkbox(String key, boolean value, Runnable toggle) {
        if (ImGui.checkbox(t(key), value)) {
            toggle.run();
            return true;
        }
        return false;
    }

    @Override
    public void writeSettings(CompoundTag tag) {
        tag.putInt("Type", this.type[0]);
        tag.putInt("Height", this.height[0]);
        tag.putInt("CrestLength", this.crestLength[0]);
        tag.putFloat("BackLength", this.backLength[0]);
        tag.putFloat("FrontLength", this.frontLength[0]);
        tag.putFloat("Curl", this.curl[0]);
        tag.putFloat("TubeSize", this.tubeSize[0]);
        tag.putInt("LipThickness", this.lipThickness[0]);
        tag.putBoolean("Peel", this.peel);
        tag.putInt("SwellCrests", this.swellCrests[0]);
        tag.putFloat("Wavelength", this.wavelength[0]);
        tag.putFloat("Steepness", this.steepness[0]);
        tag.putFloat("EndTaper", this.endTaper[0]);
        tag.putFloat("Variation", this.variation[0]);
        tag.putFloat("Chop", this.chop[0]);
        tag.putFloat("Foam", this.foam[0]);
        tag.putInt("Material", this.material[0]);
        tag.putBoolean("Hollow", this.hollow);
        tag.putInt("ShellThickness", this.shellThickness[0]);
        tag.putInt("BaseDepth", this.baseDepth[0]);
        tag.putBoolean("FaceCamera", this.faceCamera);
        tag.putFloat("Angle", this.angle[0]);
        tag.putInt("Variant", this.variant[0]);
    }

    @Override
    public void loadSettings(CompoundTag tag) {
        this.type[0] = Math.clamp(tag.getIntOr("Type", WaveGenerator.TYPE_BREAKING), 0, TYPE_KEYS.length - 1);
        this.height[0] = tag.getIntOr("Height", 18);
        this.crestLength[0] = tag.getIntOr("CrestLength", 48);
        this.backLength[0] = tag.getFloatOr("BackLength", 2.5f);
        this.frontLength[0] = tag.getFloatOr("FrontLength", 1.2f);
        this.curl[0] = tag.getFloatOr("Curl", 0.65f);
        this.tubeSize[0] = tag.getFloatOr("TubeSize", 0.42f);
        this.lipThickness[0] = tag.getIntOr("LipThickness", 2);
        this.peel = tag.getBooleanOr("Peel", false);
        this.swellCrests[0] = tag.getIntOr("SwellCrests", 3);
        this.wavelength[0] = tag.getFloatOr("Wavelength", 4.0f);
        this.steepness[0] = tag.getFloatOr("Steepness", 0.5f);
        this.endTaper[0] = tag.getFloatOr("EndTaper", 0.25f);
        this.variation[0] = tag.getFloatOr("Variation", 0.15f);
        this.chop[0] = tag.getFloatOr("Chop", 0.3f);
        this.foam[0] = tag.getFloatOr("Foam", 0.5f);
        this.material[0] = Math.clamp(tag.getIntOr("Material", 0), 0, MATERIAL_KEYS.length - 1);
        this.hollow = tag.getBooleanOr("Hollow", false);
        this.shellThickness[0] = tag.getIntOr("ShellThickness", 2);
        this.baseDepth[0] = tag.getIntOr("BaseDepth", 0);
        this.faceCamera = tag.getBooleanOr("FaceCamera", true);
        this.angle[0] = tag.getFloatOr("Angle", 0);
        this.variant[0] = tag.getIntOr("Variant", 1);
        this.dirty = true;
    }

}
