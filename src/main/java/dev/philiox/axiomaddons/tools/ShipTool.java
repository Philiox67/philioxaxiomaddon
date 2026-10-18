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
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

/**
 * Previews a detailed sailing ship at the cursor, right click places it. The bow points where you look.
 */
public class ShipTool extends AddonTool {

    private static final String[] DIRECTION_KEYS = {"view", "north", "east", "south", "west"};

    private final int[] wood = {1};
    private final int[] sailColour = {0};
    private boolean stripes = false;
    private final int[] length = {41};
    private final int[] width = {11};
    private final int[] depth = {6};
    private final int[] draft = {2};
    private final int[] masts = {3};
    private final int[] mastHeight = {26};
    private final int[] sailTiers = {3};
    private final float[] sailWidth = {1.2f};
    private boolean billow = true;
    private boolean crowsNest = true;
    private final int[] bowsprit = {6};
    private boolean jib = true;
    private final int[] sternStories = {2};
    private final int[] storyHeight = {4};
    private final float[] sternLength = {0.25f};
    private boolean forecastle = true;
    private boolean windows = true;
    private boolean cannonPorts = true;
    private boolean lanterns = true;
    private boolean darkBottom = true;
    private boolean clearInside = true;
    private final int[] direction = {0};

    private ChunkedBlockRegion ship = new ChunkedBlockRegion();
    private BlockPos origin = null;
    private Rotation builtRotation = null;
    private boolean dirty = true;

    @Override
    protected String id() {
        return "ship";
    }

    @Override
    public String name() {
        return t("tool.ship");
    }

    @Override
    public char iconChar() {
        // Axiom's modelling tool icon
        return (char) 59688;
    }

    @Override
    public void reset() {
        super.reset();
        this.origin = null;
    }

    @Override
    public UserAction.ActionResult callAction(UserAction action, Object data) {
        if (action == UserAction.RIGHT_MOUSE) {
            if (this.origin != null && !this.ship.isEmpty()) {
                ChunkedBlockRegion placed = new ChunkedBlockRegion();
                int ox = this.origin.getX();
                int oy = this.origin.getY();
                int oz = this.origin.getZ();
                this.ship.forEachEntry((x, y, z, state) -> placed.addBlock(x + ox, y + oy, z + oz, state));
                RegionHelper.pushBlockRegionChange(placed, t("history.ship"));
            }
            return UserAction.ActionResult.USED_STOP;
        }
        return UserAction.ActionResult.NOT_HANDLED;
    }

    private Rotation rotation() {
        Direction facing = switch (this.direction[0]) {
            case 1 -> Direction.NORTH;
            case 2 -> Direction.EAST;
            case 3 -> Direction.SOUTH;
            case 4 -> Direction.WEST;
            default -> Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getDirection() : Direction.EAST;
        };
        return switch (facing) {
            case SOUTH -> Rotation.CLOCKWISE_90;
            case WEST -> Rotation.CLOCKWISE_180;
            case NORTH -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    @Override
    public void render(AxiomWorldRenderContext rc) {
        RayCaster.RaycastResult hit = raycast();
        if (hit == null) {
            this.origin = null;
            return;
        }
        // The keel sits a bit below the clicked surface, so the ship floats in the water
        this.origin = hit.blockPos().relative(hit.direction()).below(Math.max(0, this.draft[0]));

        Rotation rotation = this.rotation();
        if (this.dirty || rotation != this.builtRotation) {
            this.dirty = false;
            this.builtRotation = rotation;
            this.ship = new ChunkedBlockRegion();
            ShipBuilder.build(this.settings(), rotation, this.ship);
        }
        Tool.renderRaycastOverlay(rc, hit.blockPos());
        if (!this.ship.isEmpty()) {
            this.ship.render(rc, new Vec3(this.origin.getX(), this.origin.getY(), this.origin.getZ()), 0.75f, 0.25f);
        }
    }

    private ShipBuilder.Settings settings() {
        ShipBuilder.Wood woodSet = ShipBuilder.WOODS[Math.clamp(this.wood[0], 0, ShipBuilder.WOODS.length - 1)];
        // Odd width, so the ship has a center line
        int shipWidth = this.width[0] | 1;
        return new ShipBuilder.Settings(this.length[0], shipWidth, this.depth[0], this.masts[0], this.mastHeight[0],
            this.sailTiers[0], this.sailWidth[0], this.billow, this.sailColour[0], this.stripes, this.bowsprit[0], this.jib,
            this.sternStories[0], this.storyHeight[0], this.sternLength[0], this.forecastle, this.windows, this.cannonPorts,
            this.lanterns, this.crowsNest, this.darkBottom, this.clearInside, woodSet);
    }

    private boolean checkbox(String key, boolean value, Runnable toggle) {
        if (ImGui.checkbox(t(key), value)) {
            toggle.run();
            return true;
        }
        return false;
    }

    @Override
    public void displayImguiOptions() {
        ImGuiHelper.separatorWithText(this.name());
        String[] woodKeys = new String[ShipBuilder.WOODS.length];
        for (int i = 0; i < woodKeys.length; i++) {
            woodKeys[i] = ShipBuilder.WOODS[i].key();
        }
        boolean changed = TreeTool.combo("ship.wood", this.wood, woodKeys, "ship.wood.");
        changed |= TreeTool.combo("ship.direction", this.direction, DIRECTION_KEYS, "ship.direction.");

        ImGuiHelper.separatorWithText(t("boat.section.hull"));
        changed |= ImGui.sliderInt(t("ship.length"), this.length, 15, 121);
        changed |= ImGui.sliderInt(t("ship.width"), this.width, 5, 31);
        changed |= ImGui.sliderInt(t("ship.depth"), this.depth, 3, 16);
        changed |= ImGui.sliderInt(t("ship.draft"), this.draft, 0, 10);
        changed |= this.checkbox("ship.dark_bottom", this.darkBottom, () -> this.darkBottom = !this.darkBottom);
        changed |= this.checkbox("ship.cannon_ports", this.cannonPorts, () -> this.cannonPorts = !this.cannonPorts);
        changed |= this.checkbox("ship.clear_inside", this.clearInside, () -> this.clearInside = !this.clearInside);

        ImGuiHelper.separatorWithText(t("boat.section.rigging"));
        changed |= ImGui.sliderInt(t("boat.masts"), this.masts, 0, 3);
        if (this.masts[0] > 0) {
            changed |= ImGui.sliderInt(t("ship.mast_height"), this.mastHeight, 6, 80);
            changed |= ImGui.sliderInt(t("boat.sail_tiers"), this.sailTiers, 1, 4);
            changed |= ImGui.sliderFloat(t("boat.sail_width"), this.sailWidth, 0.5f, 1.8f, "%.2f");
            changed |= TreeTool.combo("ship.sail_colour", this.sailColour, ShipBuilder.SAIL_KEYS, "ship.sail_colour.");
            changed |= this.checkbox("ship.stripes", this.stripes, () -> this.stripes = !this.stripes);
            changed |= this.checkbox("ship.billow", this.billow, () -> this.billow = !this.billow);
            changed |= this.checkbox("ship.crows_nest", this.crowsNest, () -> this.crowsNest = !this.crowsNest);
        }
        changed |= ImGui.sliderInt(t("ship.bowsprit"), this.bowsprit, 0, 24);
        if (this.bowsprit[0] > 0 && this.masts[0] > 0) {
            changed |= this.checkbox("boat.jib", this.jib, () -> this.jib = !this.jib);
        }

        ImGuiHelper.separatorWithText(t("boat.section.castle"));
        changed |= ImGui.sliderInt(t("boat.stern_stories"), this.sternStories, 0, 3);
        if (this.sternStories[0] > 0) {
            changed |= ImGui.sliderFloat(t("boat.stern_length"), this.sternLength, 0.12f, 0.45f, "%.2f");
            changed |= ImGui.sliderInt(t("boat.story_height"), this.storyHeight, 3, 6);
            changed |= this.checkbox("boat.windows", this.windows, () -> this.windows = !this.windows);
            changed |= this.checkbox("ship.lanterns", this.lanterns, () -> this.lanterns = !this.lanterns);
        }
        changed |= this.checkbox("boat.forecastle", this.forecastle, () -> this.forecastle = !this.forecastle);
        ImGui.textWrapped(t("ship.hint"));
        if (changed) {
            this.dirty = true;
        }
    }

    @Override
    public void writeSettings(CompoundTag tag) {
        tag.putInt("Wood", this.wood[0]);
        tag.putInt("SailColour", this.sailColour[0]);
        tag.putBoolean("Stripes", this.stripes);
        tag.putInt("Length", this.length[0]);
        tag.putInt("Width", this.width[0]);
        tag.putInt("Depth", this.depth[0]);
        tag.putInt("Draft", this.draft[0]);
        tag.putInt("Masts", this.masts[0]);
        tag.putInt("MastHeight", this.mastHeight[0]);
        tag.putInt("SailTiers", this.sailTiers[0]);
        tag.putFloat("SailWidth", this.sailWidth[0]);
        tag.putBoolean("Billow", this.billow);
        tag.putBoolean("CrowsNest", this.crowsNest);
        tag.putInt("Bowsprit", this.bowsprit[0]);
        tag.putBoolean("Jib", this.jib);
        tag.putInt("SternStories", this.sternStories[0]);
        tag.putInt("StoryHeight", this.storyHeight[0]);
        tag.putFloat("SternLength", this.sternLength[0]);
        tag.putBoolean("Forecastle", this.forecastle);
        tag.putBoolean("Windows", this.windows);
        tag.putBoolean("CannonPorts", this.cannonPorts);
        tag.putBoolean("Lanterns", this.lanterns);
        tag.putBoolean("DarkBottom", this.darkBottom);
        tag.putBoolean("ClearInside", this.clearInside);
        tag.putInt("Direction", this.direction[0]);
    }

    @Override
    public void loadSettings(CompoundTag tag) {
        this.wood[0] = Math.clamp(tag.getIntOr("Wood", 1), 0, ShipBuilder.WOODS.length - 1);
        this.sailColour[0] = Math.clamp(tag.getIntOr("SailColour", 0), 0, ShipBuilder.SAIL_COLOURS.length - 1);
        this.stripes = tag.getBooleanOr("Stripes", false);
        this.length[0] = tag.getIntOr("Length", 41);
        this.width[0] = tag.getIntOr("Width", 11);
        this.depth[0] = tag.getIntOr("Depth", 6);
        this.draft[0] = tag.getIntOr("Draft", 2);
        this.masts[0] = tag.getIntOr("Masts", 3);
        this.mastHeight[0] = tag.getIntOr("MastHeight", 26);
        this.sailTiers[0] = tag.getIntOr("SailTiers", 3);
        this.sailWidth[0] = tag.getFloatOr("SailWidth", 1.2f);
        this.billow = tag.getBooleanOr("Billow", true);
        this.crowsNest = tag.getBooleanOr("CrowsNest", true);
        this.bowsprit[0] = tag.getIntOr("Bowsprit", 6);
        this.jib = tag.getBooleanOr("Jib", true);
        this.sternStories[0] = tag.getIntOr("SternStories", 2);
        this.storyHeight[0] = tag.getIntOr("StoryHeight", 4);
        this.sternLength[0] = tag.getFloatOr("SternLength", 0.25f);
        this.forecastle = tag.getBooleanOr("Forecastle", true);
        this.windows = tag.getBooleanOr("Windows", true);
        this.cannonPorts = tag.getBooleanOr("CannonPorts", true);
        this.lanterns = tag.getBooleanOr("Lanterns", true);
        this.darkBottom = tag.getBooleanOr("DarkBottom", true);
        this.clearInside = tag.getBooleanOr("ClearInside", true);
        this.direction[0] = Math.clamp(tag.getIntOr("Direction", 0), 0, DIRECTION_KEYS.length - 1);
        this.dirty = true;
    }

}
