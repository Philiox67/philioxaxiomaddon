package dev.philiox.axiomaddons;

import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import imgui.moulberry92.ImGui;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;

/**
 * Extra shapes for Axiom's Shape Tool (solid category).
 * They are appended behind Axiom's own 13 solid shapes, starting at index 13.
 * Tornado to Helix live in {@link MoreShapes}, everything from {@link #PACK_START} on in {@link ShapePack}.
 */
public final class ExtraShapes {

    public static final int STAR = 13;
    public static final int EGG = 14;
    public static final int BOAT = 15;
    public static final int ROOF = 16;
    public static final int ARCH = 17;
    public static final int TORNADO = 18;
    public static final int SKULL = 19;
    public static final int HEART = 20;
    public static final int CRESCENT = 21;
    public static final int CRYSTAL = 22;
    public static final int ONION_DOME = 23;
    public static final int HELIX = 24;
    /** Index of the first {@link ShapePack} shape. */
    public static final int PACK_START = 25;

    private static final int AXIOM_SOLID_SHAPE_COUNT = 13;
    private static final int EXTRA_SHAPE_COUNT = PACK_START - AXIOM_SOLID_SHAPE_COUNT + ShapePack.SHAPES.size();

    private static final int STYLE_FLAT = 0;
    private static final int STYLE_3D = 1;

    private static final int ORIENTATION_STANDING = 0;
    private static final int ORIENTATION_LYING = 1;

    private static final int ROOF_GABLE = 0;
    private static final int ROOF_HIP = 1;
    private static final int ROOF_SHED = 2;
    private static final int ROOF_BARREL = 3;

    private static final int ARCH_ROUND = 0;
    private static final int ARCH_POINTED = 1;
    private static final int ARCH_STYLE_FREE = 0;
    private static final int ARCH_STYLE_WALL = 1;

    private static final int[][] NEIGHBOURS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private static final int[] starPoints = {5};
    private static final float[] starInnerRadius = {0.45f};
    private static final int[] starStyle = {STYLE_3D};
    private static final float[] starThickness = {0.35f};
    private static final float[] starEdgeThickness = {0.25f};
    private static final int[] starOrientation = {ORIENTATION_STANDING};

    private static final float[] eggPointiness = {0.25f};

    private static final int[] roofType = {ROOF_GABLE};
    private static boolean roofOpenBottom = true;

    private static final int[] archForm = {ARCH_ROUND};
    private static final int[] archStyle = {ARCH_STYLE_FREE};
    private static final int[] archThickness = {2};
    private static final float[] archRise = {0.45f};

    // Stays false if Axiom ever changes its shape list, then we neither add nor handle anything
    private static boolean active = false;

    // Diameters a shape asked for from its options (e.g. the ship size button), applied by the Shape Tool mixin
    private static float @Nullable [] requestedSize = null;

    private ExtraShapes() {
    }

    public static boolean isExtraShape(int shape) {
        return active && shape >= STAR && shape < AXIOM_SOLID_SHAPE_COUNT + EXTRA_SHAPE_COUNT;
    }

    /**
     * Diameters (X, Y, Z) used when the shape is picked while Axiom still has one uniform diameter,
     * for shapes that make little sense inside a cube.
     */
    static void requestSize(float[] size) {
        requestedSize = size.clone();
    }

    /** The size requested by the shape options since the last call, or null. */
    public static float @Nullable [] takeRequestedSize() {
        float[] size = requestedSize;
        requestedSize = null;
        return size;
    }

    public static float @Nullable [] defaultSize(int shape) {
        return switch (shape) {
            case BOAT -> ShipShape.defaultSize();
            case ROOF -> new float[] {21, 8, 15};
            case ARCH -> new float[] {11, 13, 3};
            case TORNADO -> new float[] {21, 41, 21};
            case CRYSTAL, ONION_DOME -> new float[] {15, 25, 15};
            case HELIX -> new float[] {15, 31, 15};
            default -> shape >= PACK_START && shape < PACK_START + ShapePack.SHAPES.size()
                ? ShapePack.SHAPES.get(shape - PACK_START).defaultSize()
                : null;
        };
    }

    public static String[] appendShapeNames(String[] names) {
        if (names.length != AXIOM_SOLID_SHAPE_COUNT) {
            active = false;
            return names;
        }
        active = true;
        String[] extended = Arrays.copyOf(names, AXIOM_SOLID_SHAPE_COUNT + EXTRA_SHAPE_COUNT);
        extended[STAR] = I18n.get("philioxaxiomaddons.shape.star");
        extended[EGG] = I18n.get("philioxaxiomaddons.shape.egg");
        extended[BOAT] = I18n.get("philioxaxiomaddons.shape.boat");
        extended[ROOF] = I18n.get("philioxaxiomaddons.shape.roof");
        extended[ARCH] = I18n.get("philioxaxiomaddons.shape.arch");
        extended[TORNADO] = I18n.get("philioxaxiomaddons.shape.tornado");
        extended[SKULL] = I18n.get("philioxaxiomaddons.shape.skull");
        extended[HEART] = I18n.get("philioxaxiomaddons.shape.heart");
        extended[CRESCENT] = I18n.get("philioxaxiomaddons.shape.crescent");
        extended[CRYSTAL] = I18n.get("philioxaxiomaddons.shape.crystal");
        extended[ONION_DOME] = I18n.get("philioxaxiomaddons.shape.onion_dome");
        extended[HELIX] = I18n.get("philioxaxiomaddons.shape.helix");
        for (int i = 0; i < ShapePack.SHAPES.size(); i++) {
            extended[PACK_START + i] = I18n.get("philioxaxiomaddons.shape." + ShapePack.SHAPES.get(i).key());
        }
        return extended;
    }

    /**
     * Draws the shape specific options, Axiom draws diameter, modifiers and rotation afterwards.
     * @return whether a setting changed and the preview has to be recalculated
     */
    public static boolean displayOptions(int shape) {
        return switch (shape) {
            case STAR -> displayStarOptions();
            case EGG -> displayEggOptions();
            case BOAT -> ShipShape.displayOptions();
            case ROOF -> displayRoofOptions();
            case ARCH -> displayArchOptions();
            default -> shape >= PACK_START
                ? shape < PACK_START + ShapePack.SHAPES.size() && ShapePack.SHAPES.get(shape - PACK_START).displayOptions()
                : MoreShapes.displayOptions(shape);
        };
    }

    private static boolean displayStarOptions() {
        ImGuiHelper.separatorWithText(I18n.get("philioxaxiomaddons.shape.star"));
        boolean changed = ImGui.sliderInt(I18n.get("philioxaxiomaddons.star.points"), starPoints, 3, 16);
        changed |= ImGui.sliderFloat(I18n.get("philioxaxiomaddons.star.inner_radius"), starInnerRadius, 0.1f, 0.95f, "%.2f");
        changed |= combo("philioxaxiomaddons.star.style", starStyle,
            "philioxaxiomaddons.star.style.flat", "philioxaxiomaddons.star.style.3d");
        changed |= ImGui.sliderFloat(I18n.get("philioxaxiomaddons.star.thickness"), starThickness, 0.05f, 1.0f, "%.2f");
        if (starStyle[0] == STYLE_3D) {
            changed |= ImGui.sliderFloat(I18n.get("philioxaxiomaddons.star.edge_thickness"), starEdgeThickness, 0.0f, 1.0f, "%.2f");
        }
        changed |= combo("philioxaxiomaddons.star.orientation", starOrientation,
            "philioxaxiomaddons.star.orientation.standing", "philioxaxiomaddons.star.orientation.lying");
        return changed;
    }

    private static boolean displayEggOptions() {
        ImGuiHelper.separatorWithText(I18n.get("philioxaxiomaddons.shape.egg"));
        return ImGui.sliderFloat(I18n.get("philioxaxiomaddons.egg.pointiness"), eggPointiness, 0.0f, 0.6f, "%.2f");
    }

    private static boolean displayRoofOptions() {
        ImGuiHelper.separatorWithText(I18n.get("philioxaxiomaddons.shape.roof"));
        boolean changed = combo("philioxaxiomaddons.roof.type", roofType,
            "philioxaxiomaddons.roof.type.gable", "philioxaxiomaddons.roof.type.hip",
            "philioxaxiomaddons.roof.type.shed", "philioxaxiomaddons.roof.type.barrel");
        if (ImGui.checkbox(I18n.get("philioxaxiomaddons.roof.open_bottom"), roofOpenBottom)) {
            roofOpenBottom = !roofOpenBottom;
            changed = true;
        }
        ImGui.textWrapped(I18n.get("philioxaxiomaddons.roof.hint"));
        return changed;
    }

    private static boolean displayArchOptions() {
        ImGuiHelper.separatorWithText(I18n.get("philioxaxiomaddons.shape.arch"));
        boolean changed = combo("philioxaxiomaddons.arch.form", archForm,
            "philioxaxiomaddons.arch.form.round", "philioxaxiomaddons.arch.form.pointed");
        changed |= combo("philioxaxiomaddons.arch.style", archStyle,
            "philioxaxiomaddons.arch.style.free", "philioxaxiomaddons.arch.style.wall");
        changed |= ImGui.sliderInt(I18n.get("philioxaxiomaddons.arch.thickness"), archThickness, 1, 16);
        changed |= ImGui.sliderFloat(I18n.get("philioxaxiomaddons.arch.rise"), archRise, 0.1f, 1.0f, "%.2f");
        ImGui.textWrapped(I18n.get("philioxaxiomaddons.arch.hint"));
        return changed;
    }

    static boolean combo(String labelKey, int[] value, String... optionKeys) {
        String[] options = new String[optionKeys.length];
        for (int i = 0; i < optionKeys.length; i++) {
            options[i] = I18n.get(optionKeys[i]);
        }
        return ImGuiHelper.combo(I18n.get(labelKey), value, options);
    }

    /**
     * Same conventions as Axiom's SphereRasterization: sizes are diameters in blocks, the pivot is the
     * shape center, blocks below cutoffY (in rotated shape space) are skipped.
     */
    public static void rasterize(int shape, ChunkedBlockRegion region, BlockState block, BlockPos pivot,
                                 float sizeX, float sizeY, float sizeZ, boolean hollow, boolean centerEven,
                                 float cutoffY, @Nullable Quaternionf rotation) {
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            return;
        }
        ShapeDefinition definition = createDefinition(shape, sizeX, sizeY, sizeZ);
        if (definition == null) {
            return;
        }
        Quaternionf rot = rotation == null ? new Quaternionf() : rotation;

        float radiusX = (sizeX - 1) / 2f;
        float radiusY = (sizeY - 1) / 2f;
        float radiusZ = (sizeZ - 1) / 2f;
        int ceilX = (int) Math.ceil(radiusX);
        int ceilY = (int) Math.ceil(radiusY);
        int ceilZ = (int) Math.ceil(radiusZ);

        // Bounding box of the rotated shape
        Vector3f vec = new Vector3f();
        int extentX = 0;
        int extentY = 0;
        int extentZ = 0;
        for (int i = -1; i <= 1; i += 2) {
            for (int j = -1; j <= 1; j += 2) {
                for (int k = -1; k <= 1; k += 2) {
                    vec.set(ceilX * i, ceilY * j, ceilZ * k);
                    rot.transformInverse(vec);
                    extentX = Math.max(extentX, (int) Math.ceil(Math.abs(vec.x)));
                    extentY = Math.max(extentY, (int) Math.ceil(Math.abs(vec.y)));
                    extentZ = Math.max(extentZ, (int) Math.ceil(Math.abs(vec.z)));
                }
            }
        }

        Sampler sampler = new Sampler(rot,
            centerEven ? -(radiusX % 1f) : 0f,
            centerEven ? -(radiusY % 1f) : 0f,
            centerEven ? -(radiusZ % 1f) : 0f,
            2f / sizeX, 2f / sizeY, 2f / sizeZ);

        boolean surfaceOnly = hollow || definition.forceHollow();
        int pivotX = pivot.getX();
        int pivotY = pivot.getY();
        int pivotZ = pivot.getZ();

        // Stairs, slabs and walls at the edges. Not for filtered shapes like the boat frame.
        boolean smoothing = PhilioxConfig.isShapeSmoothing() && definition.filter() == null;
        int margin = smoothing ? 1 : 0;

        for (int x = -extentX - margin; x <= extentX + margin; x++) {
            for (int y = -extentY - margin; y <= extentY + margin; y++) {
                for (int z = -extentZ - margin; z <= extentZ + margin; z++) {
                    sampler.sample(x, y, z);
                    if (sampler.shapeY < cutoffY) {
                        continue;
                    }
                    if (smoothing) {
                        BlockState smoothed = smoothedBlock(definition, sampler, block, surfaceOnly, x, y, z);
                        if (smoothed != null) {
                            region.addBlock(pivotX + x, pivotY + y, pivotZ + z, smoothed);
                        }
                        continue;
                    }
                    if (!sampler.test(definition.test())) {
                        continue;
                    }
                    if (!(definition.solid() != null && sampler.test(definition.solid()))) {
                        if (definition.filter() != null && !sampler.test(definition.filter())) {
                            continue;
                        }
                        if (surfaceOnly && !sampler.isSurface(definition.surfaceTest(), x, y, z)) {
                            continue;
                        }
                    }
                    region.addBlock(pivotX + x, pivotY + y, pivotZ + z, materialAt(definition, sampler, block));
                }
            }
        }
    }

    /** The shape's own colour at the last sample, or the active block. */
    private static BlockState materialAt(ShapeDefinition definition, Sampler sampler, BlockState fallback) {
        if (definition.material() == null) {
            return fallback;
        }
        BlockState material = sampler.material(definition.material());
        return material != null ? material : fallback;
    }

    /** The block for one position with smoothing on: full inside, stairs, slabs or walls at the edge. */
    private static @Nullable BlockState smoothedBlock(ShapeDefinition definition, Sampler sampler, BlockState block,
                                                      boolean surfaceOnly, int x, int y, int z) {
        boolean center = sampler.test(definition.test());
        int mask = 0;
        float insideX = 0;
        float insideY = 0;
        float insideZ = 0;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    float px = x + dx * 0.5f - 0.25f;
                    float py = y + dy * 0.5f - 0.25f;
                    float pz = z + dz * 0.5f - 0.25f;
                    if (sampler.testAt(definition.test(), px, py, pz)) {
                        mask |= ShapeSmoothing.bit(dx, dy, dz);
                        insideX = px;
                        insideY = py;
                        insideZ = pz;
                    }
                }
            }
        }
        if (mask == 0 && !center) {
            return null;
        }

        // The colour comes from the center, or from an inside part for blocks at the very edge
        BlockState base = block;
        if (definition.material() != null) {
            BlockState material = center
                ? sampler.material(definition.material())
                : sampler.materialAt(definition.material(), insideX, insideY, insideZ);
            base = material != null ? material : block;
        }
        if (center && definition.solid() != null && sampler.test(definition.solid())) {
            return base;
        }
        if (center && surfaceOnly && !sampler.isSurface(definition.surfaceTest(), x, y, z)) {
            return null;
        }
        ShapeSmoothing.Variants variants = ShapeSmoothing.variantsFor(base);
        if (variants == null) {
            return center ? base : null;
        }
        return ShapeSmoothing.forMask(mask, variants);
    }

    private static @Nullable ShapeDefinition createDefinition(int shape, float sizeX, float sizeY, float sizeZ) {
        return switch (shape) {
            case STAR -> ShapeDefinition.simple(createStarTest());
            case EGG -> ShapeDefinition.simple(createEggTest());
            case BOAT -> ShipShape.create(sizeX, sizeY, sizeZ);
            case ROOF -> createRoofDefinition(sizeX, sizeZ);
            case ARCH -> ShapeDefinition.simple(createArchTest(sizeX, sizeY));
            default -> shape >= PACK_START
                ? (shape < PACK_START + ShapePack.SHAPES.size() ? ShapePack.SHAPES.get(shape - PACK_START).create(sizeX, sizeY, sizeZ) : null)
                : MoreShapes.createDefinition(shape, sizeX, sizeY, sizeZ);
        };
    }

    // ---- Star ----

    private static ShapeTest createStarTest() {
        int points = Math.max(3, starPoints[0]);
        float inner = Math.clamp(starInnerRadius[0], 0.01f, 1f);
        boolean flat = starStyle[0] == STYLE_FLAT;
        float thickness = Math.clamp(starThickness[0], 0f, 1f);
        float edge = Math.clamp(starEdgeThickness[0], 0f, 1f);
        boolean lying = starOrientation[0] == ORIENTATION_LYING;

        double period = 2 * Math.PI / points;
        double halfPeriod = Math.PI / points;
        // Edge from the outer tip (1, 0) to the inner corner (inner*cos, inner*sin) in a folded sector
        double a = inner * Math.sin(halfPeriod);
        double b = 1 - inner * Math.cos(halfPeriod);

        return (x, y, z) -> {
            // Star outline in the u/v plane, depth along w. v points to the top tip.
            float u = x;
            float v = lying ? -z : y;
            float w = lying ? y : z;

            double r = Math.sqrt(u * u + v * v);
            double ratio = 0;
            if (r > 1e-6) {
                double t = Math.atan2(u, v) % period;
                if (t < 0) {
                    t += period;
                }
                double phi = t <= halfPeriod ? t : period - t;
                ratio = r * (a * Math.cos(phi) + b * Math.sin(phi)) / a;
            }
            if (ratio > 1) {
                return false;
            }
            double depth = flat ? thickness : thickness * (edge + (1 - edge) * (1 - ratio));
            return Math.abs(w) <= depth;
        };
    }

    // ---- Egg ----

    private static ShapeTest createEggTest() {
        // Radius at height y: sqrt(1 - y^2) * (1 - k*y), scaled so the widest point fills the box
        float k = Math.clamp(eggPointiness[0], 0f, 0.95f);
        double widestY = k == 0 ? 0 : (1 - Math.sqrt(1 + 8 * k * k)) / (4 * k);
        double maxRadius = Math.sqrt(1 - widestY * widestY) * (1 - k * widestY);
        double scaleSq = 1 / (maxRadius * maxRadius);

        return (x, y, z) -> {
            if (y < -1 || y > 1) {
                return false;
            }
            double taper = 1 - k * y;
            double radiusSq = (1 - y * y) * taper * taper * scaleSq;
            return x * x + z * z <= radiusSq;
        };
    }

    // ---- Roof ----

    private static ShapeDefinition createRoofDefinition(float sizeX, float sizeZ) {
        ShapeTest roof = createRoofTest(false, sizeX, sizeZ);
        ShapeTest surface = roofOpenBottom ? createRoofTest(true, sizeX, sizeZ) : roof;
        return new ShapeDefinition(roof, surface, false, null, null);
    }

    /**
     * Roof with the ridge along X, sloping down towards ±Z, eaves at the bottom of the box.
     * @param openBottom continue below the eaves, so the hollow check leaves the underside open
     */
    private static ShapeTest createRoofTest(boolean openBottom, float sizeX, float sizeZ) {
        int type = roofType[0];
        // Hip slopes towards ±X with the same pitch as towards ±Z
        float hipRatio = sizeX / sizeZ;

        return (x, y, z) -> {
            if (x < -1 || x > 1 || z < -1 || z > 1 || y > 1) {
                return false;
            }
            if (y < -1 && !openBottom) {
                return false;
            }
            double h = Math.max(0, (y + 1) / 2);
            double limit = switch (type) {
                case ROOF_HIP -> Math.min(1 - Math.abs(z), (1 - Math.abs(x)) * hipRatio);
                case ROOF_SHED -> (1 - z) / 2;
                case ROOF_BARREL -> Math.sqrt(Math.max(0, 1 - z * z));
                default -> 1 - Math.abs(z);
            };
            return h <= limit;
        };
    }

    // ---- Arch ----

    /**
     * Arch in the X/Y plane, depth along Z, open at the bottom.
     */
    private static ShapeTest createArchTest(float sizeX, float sizeY) {
        boolean pointed = archForm[0] == ARCH_POINTED;
        boolean wall = archStyle[0] == ARCH_STYLE_WALL;
        float thicknessX = 2f * Math.max(1, archThickness[0]) / sizeX;
        float thicknessY = 2f * Math.max(1, archThickness[0]) / sizeY;
        // Height where the curve starts, the rise is the share of the total height taken by the curve
        float springing = 1 - 2 * Math.clamp(archRise[0], 0.05f, 1f);
        float innerHalfWidth = 1 - thicknessX;
        float innerTop = 1 - thicknessY;

        return (x, y, z) -> {
            if (x < -1 || x > 1 || y < -1 || y > 1 || z < -1 || z > 1) {
                return false;
            }
            boolean outer = wall || insideArchOutline(x, y, 1, 1, springing, pointed);
            if (!outer) {
                return false;
            }
            return innerHalfWidth <= 0 || !insideArchOutline(x, y, innerHalfWidth, innerTop, springing, pointed);
        };
    }

    private static boolean insideArchOutline(float x, float y, float halfWidth, float top, float springing, boolean pointed) {
        if (Math.abs(x) > halfWidth || y > top) {
            return false;
        }
        if (y <= springing || top <= springing) {
            return true;
        }
        double u = Math.abs(x) / halfWidth;
        double v = (y - springing) / (top - springing);
        if (pointed) {
            // Two arcs with their centers on the opposite side, meeting in a point at v = 1
            return (u + 1) * (u + 1) + 3 * v * v <= 4;
        }
        return u * u + v * v <= 1;
    }

    // ---- Helpers ----

    private static ShapeTest or(ShapeTest a, ShapeTest b) {
        return (x, y, z) -> a.contains(x, y, z) || b.contains(x, y, z);
    }

    @FunctionalInterface
    interface ShapeTest {
        /** Coordinates are normalized, the shape's bounding box spans -1 to 1 on every axis. */
        boolean contains(float x, float y, float z);
    }

    /**
     * @param test          which blocks belong to the shape
     * @param surfaceTest   used for the hollow check, lets shapes stay open on one side
     * @param forceHollow   always only keep surface blocks, ignoring Axiom's hollow checkbox
     * @param solid         parts that are always placed in full, e.g. the boat mast
     * @param filter        only keep blocks matching this, e.g. the ribs of the boat frame
     */
    record ShapeDefinition(ShapeTest test, ShapeTest surfaceTest, boolean forceHollow,
                           @Nullable ShapeTest solid, @Nullable ShapeTest filter, @Nullable ShapeMaterial material) {

        ShapeDefinition(ShapeTest test, ShapeTest surfaceTest, boolean forceHollow,
                        @Nullable ShapeTest solid, @Nullable ShapeTest filter) {
            this(test, surfaceTest, forceHollow, solid, filter, null);
        }

        static ShapeDefinition simple(ShapeTest test) {
            return new ShapeDefinition(test, test, false, null, null);
        }

        /** Same shape, but the shape decides the block for each position. */
        ShapeDefinition withMaterial(@Nullable ShapeMaterial material) {
            return new ShapeDefinition(this.test, this.surfaceTest, this.forceHollow, this.solid, this.filter, material);
        }
    }

    /** Lets a shape colour itself, e.g. the stripes of the candy cane. Null means the active block. */
    @FunctionalInterface
    interface ShapeMaterial {
        /** Coordinates are normalized like in {@link ShapeTest}. */
        @Nullable BlockState at(float x, float y, float z);
    }

    private static final class Sampler {
        private final Quaternionf rotation;
        private final float offsetX, offsetY, offsetZ;
        private final float scaleX, scaleY, scaleZ;
        private final Vector3f vec = new Vector3f();
        private final Vector3f point = new Vector3f();

        // Result of the last sample() call
        private float shapeY;
        private float normalizedX, normalizedY, normalizedZ;

        private Sampler(Quaternionf rotation, float offsetX, float offsetY, float offsetZ,
                        float scaleX, float scaleY, float scaleZ) {
            this.rotation = rotation;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
        }

        private void sample(int x, int y, int z) {
            this.vec.set(x, y, z);
            this.rotation.transform(this.vec);
            this.shapeY = this.vec.y + this.offsetY;
            this.normalizedX = (this.vec.x + this.offsetX) * this.scaleX;
            this.normalizedY = this.shapeY * this.scaleY;
            this.normalizedZ = (this.vec.z + this.offsetZ) * this.scaleZ;
        }

        private boolean test(ShapeTest shapeTest) {
            return shapeTest.contains(this.normalizedX, this.normalizedY, this.normalizedZ);
        }

        private @Nullable BlockState material(ShapeMaterial material) {
            return material.at(this.normalizedX, this.normalizedY, this.normalizedZ);
        }

        private @Nullable BlockState materialAt(ShapeMaterial material, float x, float y, float z) {
            this.point.set(x, y, z);
            this.rotation.transform(this.point);
            return material.at(
                (this.point.x + this.offsetX) * this.scaleX,
                (this.point.y + this.offsetY) * this.scaleY,
                (this.point.z + this.offsetZ) * this.scaleZ);
        }

        /** Tests any point in block space, without changing the last sample. */
        private boolean testAt(ShapeTest shapeTest, float x, float y, float z) {
            this.point.set(x, y, z);
            this.rotation.transform(this.point);
            return shapeTest.contains(
                (this.point.x + this.offsetX) * this.scaleX,
                (this.point.y + this.offsetY) * this.scaleY,
                (this.point.z + this.offsetZ) * this.scaleZ);
        }

        private boolean isSurface(ShapeTest surfaceTest, int x, int y, int z) {
            float sampledX = this.normalizedX;
            float sampledY = this.normalizedY;
            float sampledZ = this.normalizedZ;
            float sampledShapeY = this.shapeY;
            boolean surface = false;
            for (int[] n : NEIGHBOURS) {
                this.sample(x + n[0], y + n[1], z + n[2]);
                if (!this.test(surfaceTest)) {
                    surface = true;
                    break;
                }
            }
            this.normalizedX = sampledX;
            this.normalizedY = sampledY;
            this.normalizedZ = sampledZ;
            this.shapeY = sampledShapeY;
            return surface;
        }
    }

}
