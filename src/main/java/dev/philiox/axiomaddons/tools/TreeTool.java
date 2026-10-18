package dev.philiox.axiomaddons.tools;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.tools.Tool;
import imgui.moulberry92.ImGui;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Previews a procedural tree at the cursor, right click places it.
 */
public class TreeTool extends AddonTool {

    private static final String[] TYPE_KEYS = {"oak", "spruce", "birch", "palm", "dead", "willow"};

    private final int[] type = {TreeGenerator.OAK};
    private final int[] height = {12};
    private final float[] crown = {1.0f};
    private final int[] trunkWidth = {1};
    private final float[] branches = {0.5f};
    private final int[] wood = {0};
    private boolean newSeedEachTree = true;

    private long seed = ThreadLocalRandom.current().nextLong();
    private BlockPos lastBase = null;
    private boolean dirty = true;

    @Override
    protected String id() {
        return "tree";
    }

    @Override
    public String name() {
        return t("tool.tree");
    }

    @Override
    public char iconChar() {
        // Axiom's rock tool icon
        return (char) 59654;
    }

    @Override
    public void reset() {
        super.reset();
        this.lastBase = null;
    }

    @Override
    public UserAction.ActionResult callAction(UserAction action, Object data) {
        if (action == UserAction.RIGHT_MOUSE) {
            if (!this.preview.isEmpty()) {
                this.commitPreview("history.tree");
                if (this.newSeedEachTree) {
                    this.seed = ThreadLocalRandom.current().nextLong();
                }
                this.dirty = true;
            }
            return UserAction.ActionResult.USED_STOP;
        }
        return UserAction.ActionResult.NOT_HANDLED;
    }

    @Override
    public void render(AxiomWorldRenderContext rc) {
        RayCaster.RaycastResult hit = raycast();
        if (hit == null) {
            this.preview.clear();
            this.lastBase = null;
            return;
        }
        BlockPos base = hit.blockPos().relative(hit.direction());
        if (this.dirty || !base.equals(this.lastBase)) {
            this.dirty = false;
            this.lastBase = base;
            this.preview.clear();
            TreeGenerator.generate(this.preview, base, this.settings(), this.seed);
        }
        Tool.renderRaycastOverlay(rc, hit.blockPos());
        this.renderPreview(rc);
    }

    private TreeGenerator.Settings settings() {
        int woodIndex = Math.clamp(this.wood[0], 0, TreeGenerator.WOOD_SETS.length - 1);
        return new TreeGenerator.Settings(this.type[0], Math.max(3, this.height[0]), this.crown[0],
            Math.max(1, this.trunkWidth[0]), this.branches[0], TreeGenerator.WOOD_SETS[woodIndex]);
    }

    @Override
    public void displayImguiOptions() {
        ImGuiHelper.separatorWithText(this.name());
        boolean changed = combo("tree.type", this.type, TYPE_KEYS, "tree.type.");
        String[] woodKeys = new String[TreeGenerator.WOOD_SETS.length];
        for (int i = 0; i < woodKeys.length; i++) {
            woodKeys[i] = TreeGenerator.WOOD_SETS[i].key();
        }
        changed |= combo("tree.wood", this.wood, woodKeys, "tree.wood.");
        changed |= ImGui.sliderInt(t("tree.height"), this.height, 4, 48);
        changed |= ImGui.sliderFloat(t("tree.crown"), this.crown, 0.4f, 2.5f, "%.2f");
        changed |= ImGui.sliderInt(t("tree.trunk_width"), this.trunkWidth, 1, 4);
        changed |= ImGui.sliderFloat(t("tree.branches"), this.branches, 0.0f, 1.0f, "%.2f");
        if (ImGui.checkbox(t("tree.new_seed_each"), this.newSeedEachTree)) {
            this.newSeedEachTree = !this.newSeedEachTree;
        }
        if (ImGui.button(t("tree.reroll"))) {
            this.seed = ThreadLocalRandom.current().nextLong();
            changed = true;
        }
        ImGui.textWrapped(t("tree.hint"));
        if (changed) {
            this.dirty = true;
        }
    }

    static boolean combo(String labelKey, int[] value, String[] optionKeys, String optionPrefix) {
        String[] options = new String[optionKeys.length];
        for (int i = 0; i < optionKeys.length; i++) {
            options[i] = t(optionPrefix + optionKeys[i]);
        }
        return ImGuiHelper.combo(t(labelKey), value, options);
    }

    @Override
    public void writeSettings(CompoundTag tag) {
        tag.putInt("Type", this.type[0]);
        tag.putInt("Height", this.height[0]);
        tag.putFloat("Crown", this.crown[0]);
        tag.putInt("TrunkWidth", this.trunkWidth[0]);
        tag.putFloat("Branches", this.branches[0]);
        tag.putInt("Wood", this.wood[0]);
        tag.putBoolean("NewSeedEachTree", this.newSeedEachTree);
    }

    @Override
    public void loadSettings(CompoundTag tag) {
        this.type[0] = tag.getIntOr("Type", TreeGenerator.OAK);
        this.height[0] = tag.getIntOr("Height", 12);
        this.crown[0] = tag.getFloatOr("Crown", 1.0f);
        this.trunkWidth[0] = tag.getIntOr("TrunkWidth", 1);
        this.branches[0] = tag.getFloatOr("Branches", 0.5f);
        this.wood[0] = tag.getIntOr("Wood", 0);
        this.newSeedEachTree = tag.getBooleanOr("NewSeedEachTree", true);
        this.dirty = true;
    }

}
