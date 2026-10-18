package dev.philiox.axiomaddons.tools;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.RegionHelper;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Base for the addon's own Axiom tools: a ghost preview region that can be committed undoably.
 */
public abstract class AddonTool implements Tool {

    protected ChunkedBlockRegion preview = new ChunkedBlockRegion();

    /** Short id, used for the keybind id. */
    protected abstract String id();

    protected static String t(String key) {
        return I18n.get("philioxaxiomaddons." + key);
    }

    protected static String t(String key, Object... args) {
        return I18n.get("philioxaxiomaddons." + key, args);
    }

    protected static @Nullable RayCaster.RaycastResult raycast() {
        return Tool.raycastBlock();
    }

    @Override
    public void reset() {
        this.preview.clear();
    }

    protected void renderPreview(AxiomWorldRenderContext rc) {
        if (!this.preview.isEmpty()) {
            this.preview.render(rc, Vec3.ZERO, 0.75f, 0.25f);
        }
    }

    /** Pushes the preview into the world through Axiom's history, so it can be undone. */
    protected void commitPreview(String historyKey) {
        if (this.preview.isEmpty()) {
            return;
        }
        ChunkedBlockRegion region = this.preview;
        this.preview = new ChunkedBlockRegion();
        RegionHelper.pushBlockRegionChange(region, t(historyKey));
    }

    @Override
    public String keybindId() {
        return "philiox_" + this.id();
    }

    @Override
    public EnumSet<AxiomPermission> requiredPermissions() {
        return EnumSet.of(AxiomPermission.TOOL, AxiomPermission.BUILD_SECTION);
    }

}
