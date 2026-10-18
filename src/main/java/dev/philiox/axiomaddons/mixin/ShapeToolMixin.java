package dev.philiox.axiomaddons.mixin;

import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.shape.ShapeTool;
import dev.philiox.axiomaddons.ExtraShapes;
import dev.philiox.axiomaddons.PhilioxConfig;
import dev.philiox.axiomaddons.ShapeSmoothing;
import imgui.moulberry92.ImGui;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks the extra shapes (Star, Egg, Boat, Roof, Arch) into Axiom's Shape Tool.
 *
 * Axiom switches over shapeSolid[0] in displayImguiOptions, renderShapeBounds and recalculate and throws
 * for unknown indices. The first two only need sphere like behaviour (diameter XYZ, bounding box), so the
 * index is swapped to 0 (sphere) while they run. recalculate is replaced completely for our shapes.
 */
@Mixin(value = ShapeTool.class, remap = false)
public abstract class ShapeToolMixin {

    @Unique
    private static final String COMBO = "Lcom/moulberry/axiom/editor/ImGuiHelper;combo(Ljava/lang/String;[I[Ljava/lang/String;)Z";
    @Unique
    private static final int SPHERE = 0;

    @Shadow @Final private ChunkedBlockRegion chunkedBlockRegion;
    @Shadow private Gizmo gizmo;
    @Shadow private boolean recalculate;
    @Shadow private boolean decimalSizes;
    @Shadow private boolean centerEvenDiameters;
    @Shadow @Final private int[] shapeCategory;
    @Shadow @Final private int[] shapeSolid;
    @Shadow private boolean separateXYZ;
    @Shadow @Final private float[] sizeXYZ;
    @Shadow @Final private float[] sizeX;
    @Shadow @Final private float[] sizeY;
    @Shadow @Final private float[] sizeZ;
    @Shadow @Final private Quaternionf rotationQuaternion;
    @Shadow private boolean hollow;
    @Shadow private boolean metaball;
    @Shadow private boolean cutoff;
    @Shadow @Final private int[] cutoffAmount;

    @Unique
    private int philiox$swappedShape = -1;
    @Unique
    private int philiox$lastShownShape = -1;
    @Unique
    private boolean philiox$smoothingPass = false;

    @Shadow
    private BlockPos calculatePivot(float height) {
        throw new AssertionError();
    }

    @Shadow
    private void recalculate() {
        throw new AssertionError();
    }

    @Shadow
    public abstract void markDirty();

    @Unique
    private boolean philiox$isExtraShapeSelected() {
        return this.shapeCategory[0] == 0 && ExtraShapes.isExtraShape(this.shapeSolid[0]);
    }

    @Unique
    private void philiox$swapToSphere() {
        if (this.philiox$swappedShape < 0 && this.philiox$isExtraShapeSelected()) {
            this.philiox$swappedShape = this.shapeSolid[0];
            this.shapeSolid[0] = SPHERE;
        }
    }

    @Unique
    private void philiox$restoreShape() {
        if (this.philiox$swappedShape >= 0) {
            this.shapeSolid[0] = this.philiox$swappedShape;
            this.philiox$swappedShape = -1;
        }
    }

    // ---- UI ----

    // ordinal 0 is the category combo, ordinal 1 the solid shape combo
    @ModifyArg(method = "displayImguiOptions", at = @At(value = "INVOKE", target = COMBO, ordinal = 1), index = 2)
    private String[] philiox$addShapeNames(String[] names) {
        return ExtraShapes.appendShapeNames(names);
    }

    @Inject(method = "displayImguiOptions", at = @At(value = "INVOKE", target = COMBO, ordinal = 1, shift = At.Shift.AFTER))
    private void philiox$displayShapeOptions(CallbackInfo ci) {
        int previousShape = this.philiox$lastShownShape;
        this.philiox$lastShownShape = this.shapeSolid[0];

        // Stairs and slabs at the edges, for Axiom's own solid shapes and the addon shapes
        boolean smoothing = PhilioxConfig.isShapeSmoothing();
        if (ImGui.checkbox(I18n.get("philioxaxiomaddons.smoothing"), smoothing)) {
            PhilioxConfig.setShapeSmoothing(!smoothing);
            this.markDirty();
        }
        if (PhilioxConfig.isShapeSmoothing() && ShapeSmoothing.variantsFor(Tool.getActiveBlock()) == null) {
            ImGui.textWrapped(I18n.get("philioxaxiomaddons.smoothing.no_variants"));
        }

        if (!this.philiox$isExtraShapeSelected()) {
            return;
        }
        // Metaball is not supported for our shapes
        this.metaball = false;

        float[] size = ExtraShapes.defaultSize(this.shapeSolid[0]);
        if (size != null && previousShape != this.shapeSolid[0] && !this.separateXYZ) {
            this.philiox$applySize(size);
        }
        if (ExtraShapes.displayOptions(this.shapeSolid[0])) {
            this.markDirty();
        }
        float[] requested = ExtraShapes.takeRequestedSize();
        if (requested != null) {
            this.philiox$applySize(requested);
        }
        this.philiox$swapToSphere();
    }

    @Unique
    private void philiox$applySize(float[] size) {
        this.separateXYZ = true;
        this.sizeX[0] = size[0];
        this.sizeY[0] = size[1];
        this.sizeZ[0] = size[2];
        this.markDirty();
    }

    // Hide the metaball checkbox, ordinal 0 is the call in the sphere branch we redirect to
    @ModifyArg(method = "displayImguiOptions",
        at = @At(value = "INVOKE", target = "Lcom/moulberry/axiom/tools/shape/ShapeTool;displayGenericShape(ZZZ)Z", ordinal = 0),
        index = 1)
    private boolean philiox$hideMetaball(boolean showMetaball) {
        return this.philiox$swappedShape < 0 && showMetaball;
    }

    @Inject(method = "displayImguiOptions", at = @At("RETURN"))
    private void philiox$restoreAfterOptions(CallbackInfo ci) {
        this.philiox$restoreShape();
    }

    // ---- Bounding box ----

    @Inject(method = "renderShapeBounds", at = @At("HEAD"))
    private void philiox$swapBeforeBounds(CallbackInfo ci) {
        this.philiox$swapToSphere();
    }

    @Inject(method = "renderShapeBounds", at = @At("RETURN"))
    private void philiox$restoreAfterBounds(CallbackInfo ci) {
        this.philiox$restoreShape();
    }

    // ---- Preview blocks ----

    @Inject(method = "recalculate", at = @At("HEAD"), cancellable = true)
    private void philiox$recalculateExtraShape(CallbackInfo ci) {
        // Without gizmo Axiom only clears the preview, let it do that itself
        if (this.gizmo == null || this.philiox$smoothingPass) {
            return;
        }
        if (!this.philiox$isExtraShapeSelected()) {
            if (this.shapeCategory[0] == 0 && PhilioxConfig.isShapeSmoothing()) {
                ShapeSmoothing.Variants variants = ShapeSmoothing.variantsFor(Tool.getActiveBlock());
                if (variants != null) {
                    ci.cancel();
                    this.philiox$smoothAxiomShape(variants);
                }
            }
            return;
        }
        ci.cancel();

        this.recalculate = false;
        this.chunkedBlockRegion.clear();

        BlockState block = Tool.getActiveBlock();
        boolean centerEven = this.centerEvenDiameters && !this.decimalSizes;

        float x = this.sizeX[0];
        float y = this.sizeY[0];
        float z = this.sizeZ[0];
        if (!this.separateXYZ) {
            x = y = z = this.sizeXYZ[0];
        }
        float cutoffY = this.cutoff && this.cutoffAmount[0] > 0
            ? y * (this.cutoffAmount[0] / 100f - 0.5f)
            : Float.NEGATIVE_INFINITY;

        ExtraShapes.rasterize(this.shapeSolid[0], this.chunkedBlockRegion, block, this.calculatePivot(y),
            x, y, z, this.hollow, centerEven, cutoffY, this.rotationQuaternion);
    }

    /**
     * Axiom's own shapes have no inside test we could sample, so Axiom builds the shape once normally and once
     * at double size. Every 2x2x2 group of the big version becomes one block of the normal size, as a full block,
     * stair, slab or wall depending on which of its eight halves are filled.
     */
    @Unique
    private void philiox$smoothAxiomShape(ShapeSmoothing.Variants variants) {
        float xyz = this.sizeXYZ[0];
        float x = this.sizeX[0];
        float y = this.sizeY[0];
        float z = this.sizeZ[0];
        BlockPos lowMin;
        BlockPos lowMax;
        this.philiox$smoothingPass = true;
        try {
            this.recalculate();
            lowMin = this.chunkedBlockRegion.min();
            lowMax = this.chunkedBlockRegion.max();
            if (lowMin == null || lowMax == null) {
                return;
            }
            this.sizeXYZ[0] = xyz * 2;
            this.sizeX[0] = x * 2;
            this.sizeY[0] = y * 2;
            this.sizeZ[0] = z * 2;
            this.recalculate();
        } finally {
            this.sizeXYZ[0] = xyz;
            this.sizeX[0] = x;
            this.sizeY[0] = y;
            this.sizeZ[0] = z;
            this.philiox$smoothingPass = false;
            this.recalculate = false;
        }

        BlockPos highMin = this.chunkedBlockRegion.min();
        BlockPos highMax = this.chunkedBlockRegion.max();
        if (highMin == null || highMax == null) {
            return;
        }
        // Scale the big version down by half around the center of the normal one
        double lowX = (lowMin.getX() + lowMax.getX() + 1) / 2.0;
        double lowY = (lowMin.getY() + lowMax.getY() + 1) / 2.0;
        double lowZ = (lowMin.getZ() + lowMax.getZ() + 1) / 2.0;
        double highX = (highMin.getX() + highMax.getX() + 1) / 2.0;
        double highY = (highMin.getY() + highMax.getY() + 1) / 2.0;
        double highZ = (highMin.getZ() + highMax.getZ() + 1) / 2.0;

        Long2IntOpenHashMap masks = new Long2IntOpenHashMap();
        this.chunkedBlockRegion.forEachEntry((bx, by, bz, state) -> {
            double px = lowX + (bx + 0.5 - highX) / 2;
            double py = lowY + (by + 0.5 - highY) / 2;
            double pz = lowZ + (bz + 0.5 - highZ) / 2;
            int cx = (int) Math.floor(px);
            int cy = (int) Math.floor(py);
            int cz = (int) Math.floor(pz);
            int bit = ShapeSmoothing.bit(px - cx >= 0.5 ? 1 : 0, py - cy >= 0.5 ? 1 : 0, pz - cz >= 0.5 ? 1 : 0);
            long key = BlockPos.asLong(cx, cy, cz);
            masks.put(key, masks.get(key) | bit);
        });

        this.chunkedBlockRegion.clear();
        for (Long2IntMap.Entry entry : masks.long2IntEntrySet()) {
            BlockState state = ShapeSmoothing.forMask(entry.getIntValue(), variants);
            if (state != null) {
                long key = entry.getLongKey();
                this.chunkedBlockRegion.addBlock(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key), state);
            }
        }
    }

}
