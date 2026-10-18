package dev.philiox.axiomaddons.tools;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.tools.Tool;
import imgui.moulberry92.ImGui;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

/**
 * Hold right click and paint over the landscape: the painted area becomes a sea with waves made of blocks.
 * While painting a quick preview grows under the brush, on release the whole sea is calculated again with calm,
 * foamy shores and placed undoably.
 */
public class OceanPainterTool extends AddonTool {

    private static final int SEA_LEVEL_FIRST_CLICK = 0;
    private static final int SEA_LEVEL_FIXED = 1;
    private static final String[] SEA_LEVEL_KEYS = {"first_click", "fixed"};
    private static final String[] MATERIAL_KEYS = {"ocean", "tropical", "wool", "glass", "ice"};

    private static final int DEEP = 0;
    private static final int MID = 1;
    private static final int LIGHT = 2;
    private static final int FOAM = 3;
    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final int[] brushRadius = {12};
    private final int[] seaLevelMode = {SEA_LEVEL_FIRST_CLICK};
    private final int[] seaLevelOffset = {1};
    private final int[] fixedSeaLevel = {63};
    private final int[] maxDepth = {16};
    private final float[] amplitude = {3.0f};
    private final float[] wavelength = {18.0f};
    private final float[] steepness = {0.5f};
    private final float[] crossSea = {0.35f};
    private final float[] chop = {0.4f};
    private final float[] foam = {0.5f};
    private final int[] shoreFade = {6};
    private final int[] shoreFoam = {2};
    private boolean faceCamera = true;
    private final float[] angle = {0};
    private final int[] material = {0};
    private final int[] variant = {1};

    private boolean painting = false;
    private final LongOpenHashSet painted = new LongOpenHashSet();
    private int seaLevel = 63;
    private double strokeAngle = 0;

    @Override
    protected String id() {
        return "ocean_painter";
    }

    @Override
    public String name() {
        return t("tool.ocean_painter");
    }

    @Override
    public char iconChar() {
        // Axiom's noise painter icon
        return (char) 59663;
    }

    @Override
    public void reset() {
        super.reset();
        this.painting = false;
        this.painted.clear();
    }

    @Override
    public UserAction.ActionResult callAction(UserAction action, Object data) {
        if (action == UserAction.RIGHT_MOUSE) {
            RayCaster.RaycastResult hit = raycast();
            if (hit == null) {
                return UserAction.ActionResult.NOT_HANDLED;
            }
            this.reset();
            this.painting = true;
            this.seaLevel = this.seaLevelMode[0] == SEA_LEVEL_FIXED
                ? this.fixedSeaLevel[0]
                : hit.blockPos().getY() + this.seaLevelOffset[0];
            // The wave direction stays the same for the whole stroke
            this.strokeAngle = this.angle[0];
            if (this.faceCamera && Minecraft.getInstance().player != null) {
                this.strokeAngle = Minecraft.getInstance().player.getYRot() + 180;
            }
            return UserAction.ActionResult.USED_STOP;
        }
        if (action == UserAction.ESCAPE && this.painting) {
            this.reset();
            return UserAction.ActionResult.USED_STOP;
        }
        return UserAction.ActionResult.NOT_HANDLED;
    }

    @Override
    public String listenForEsc() {
        return this.painting ? t("bridge.cancel") : null;
    }

    @Override
    public void render(AxiomWorldRenderContext rc) {
        RayCaster.RaycastResult hit = raycast();
        if (this.painting) {
            if (Tool.isMouseDown(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
                if (hit != null) {
                    this.paint(hit.blockPos());
                }
            } else {
                this.finish();
            }
        }
        if (hit != null) {
            Tool.renderRaycastOverlay(rc, hit.blockPos());
        }
        this.renderPreview(rc);
    }

    /** Adds the columns under the brush and previews their water right away (without shore effects). */
    private void paint(BlockPos center) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        int r = Math.max(1, this.brushRadius[0]);
        WaveField field = this.field();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r + r) {
                    continue;
                }
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                if (this.painted.add(column(x, z))) {
                    this.buildColumn(level, field, x, z, Integer.MAX_VALUE);
                }
            }
        }
    }

    /** Recalculates everything with calm shores and foam, then places it. */
    private void finish() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && !this.painted.isEmpty()) {
            Long2IntOpenHashMap shoreDistance = this.shoreDistances();
            this.preview.clear();
            WaveField field = this.field();
            LongIterator iterator = this.painted.iterator();
            while (iterator.hasNext()) {
                long key = iterator.nextLong();
                this.buildColumn(level, field, columnX(key), columnZ(key), shoreDistance.getOrDefault(key, Integer.MAX_VALUE));
            }
            this.commitPreview("history.ocean");
        }
        this.reset();
    }

    /** Distance of every painted column to the edge of the painted area, up to the shore width. */
    private Long2IntOpenHashMap shoreDistances() {
        int limit = Math.max(this.shoreFade[0], this.shoreFoam[0]) + 1;
        Long2IntOpenHashMap distances = new Long2IntOpenHashMap();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        LongIterator iterator = this.painted.iterator();
        while (iterator.hasNext()) {
            long key = iterator.nextLong();
            int x = columnX(key);
            int z = columnZ(key);
            for (int[] n : NEIGHBOURS) {
                if (!this.painted.contains(column(x + n[0], z + n[1]))) {
                    distances.put(key, 0);
                    queue.enqueue(key);
                    break;
                }
            }
        }
        while (!queue.isEmpty()) {
            long key = queue.dequeueLong();
            int distance = distances.get(key);
            if (distance >= limit) {
                continue;
            }
            int x = columnX(key);
            int z = columnZ(key);
            for (int[] n : NEIGHBOURS) {
                long next = column(x + n[0], z + n[1]);
                if (this.painted.contains(next) && !distances.containsKey(next)) {
                    distances.put(next, distance + 1);
                    queue.enqueue(next);
                }
            }
        }
        return distances;
    }

    private void buildColumn(ClientLevel level, WaveField field, int x, int z, int shoreDistance) {
        double shore = this.shoreFade[0] <= 0 ? 1 : WaveGenerator.smoothstep(0, this.shoreFade[0], shoreDistance);
        double[] wave = field.sample(x, z);
        double height = wave[0] * shore;
        int surface = this.seaLevel + (int) Math.round(height);

        // Find the ground: the first block that water could not replace
        int ground = Integer.MIN_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bottom = surface - Math.max(1, this.maxDepth[0]);
        for (int y = surface; y > bottom; y--) {
            BlockState state = level.getBlockState(pos.set(x, y, z));
            if (!state.isAir() && !state.canBeReplaced()) {
                ground = y;
                break;
            }
        }
        int from = ground == Integer.MIN_VALUE ? bottom + 1 : ground + 1;
        if (from > surface) {
            // Land is higher than the waves here
            return;
        }

        BlockState[] palette = WaveGenerator.PALETTES[Math.clamp(this.material[0], 0, WaveGenerator.PALETTES.length - 1)];
        double amplitude = Math.max(0.01, this.amplitude[0]);
        double crestHeight = (height + amplitude * 0.4) / (amplitude * 1.4);
        boolean shoreFoam = this.foam[0] > 0 && shoreDistance < this.shoreFoam[0];
        boolean crestFoam = this.foam[0] > 0 && wave[1] > 1 - this.foam[0] * 0.15 && shore > 0.5;
        for (int y = from; y <= surface; y++) {
            int material;
            int depthBelowSurface = surface - y;
            if (depthBelowSurface == 0 && (shoreFoam || crestFoam)) {
                material = FOAM;
            } else if (depthBelowSurface <= 1) {
                double t = crestHeight + (WaveGenerator.hash(x, y, z, this.variant[0]) - 0.5) * 0.15;
                material = t > 0.7 ? LIGHT : t > 0.35 ? MID : DEEP;
            } else {
                material = depthBelowSurface <= 3 ? MID : DEEP;
            }
            BlockState state = material == FOAM
                ? (WaveGenerator.hash(x, y, z, 99) > 0.5 ? palette[3] : palette[4])
                : palette[material];
            this.preview.addBlock(x, y, z, state);
        }
    }

    private WaveField field() {
        return new WaveField(this.strokeAngle, Math.max(0.5, this.amplitude[0]), Math.max(2, this.wavelength[0]),
            this.steepness[0], this.crossSea[0], this.chop[0], this.variant[0] * 0x5DEECE66DL);
    }

    /**
     * Height of the sea surface for any column: a main swell with peaked crests, a weaker crossing sea,
     * wobbly crest lines, wave groups and some chop.
     */
    private record WaveField(double angle, double amplitude, double wavelength, double steepness, double crossSea,
                             double chop, long seed) {

        /** @return {height above sea level in blocks, crest value 0..1} */
        double[] sample(int x, int z) {
            double radians = Math.toRadians(this.angle);
            double dirX = -Math.sin(radians);
            double dirZ = Math.cos(radians);
            double along = x * dirX + z * dirZ;

            // Crest lines are not perfectly straight
            double warp = WaveGenerator.noise3(x / (this.wavelength * 1.5), 0, z / (this.wavelength * 1.5), this.seed) * this.wavelength * 0.35;
            double phase = 2 * Math.PI * (along + warp) / this.wavelength;
            double crest = Math.pow((1 + Math.cos(phase)) / 2, 1 + 2 * this.steepness);
            // Waves come in groups, some higher than others
            double group = 0.65 + 0.35 * WaveGenerator.noise3(x / (this.wavelength * 3), 5, z / (this.wavelength * 3), this.seed + 1);
            double height = this.amplitude * group * (1.4 * crest - 0.4);

            if (this.crossSea > 0) {
                double crossRadians = radians + Math.toRadians(55);
                double crossAlong = x * -Math.sin(crossRadians) + z * Math.cos(crossRadians);
                double crossPhase = 2 * Math.PI * crossAlong / (this.wavelength * 0.6);
                height += this.amplitude * this.crossSea * 0.45 * Math.cos(crossPhase);
            }
            if (this.chop > 0) {
                height += this.chop * 1.2 * WaveGenerator.noise3(x / 3.5, 9, z / 3.5, this.seed + 2);
            }
            return new double[] {height, crest};
        }
    }

    private static long column(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static int columnX(long key) {
        return (int) (key >> 32);
    }

    private static int columnZ(long key) {
        return (int) key;
    }

    @Override
    public void displayImguiOptions() {
        ImGuiHelper.separatorWithText(this.name());
        ImGui.sliderInt(t("ocean.brush_radius"), this.brushRadius, 2, 64);
        TreeTool.combo("ocean.sea_level", this.seaLevelMode, SEA_LEVEL_KEYS, "ocean.sea_level.");
        if (this.seaLevelMode[0] == SEA_LEVEL_FIXED) {
            ImGui.sliderInt(t("ocean.fixed_sea_level"), this.fixedSeaLevel, -60, 300);
        } else {
            ImGui.sliderInt(t("ocean.sea_level_offset"), this.seaLevelOffset, -10, 30);
        }
        ImGui.sliderInt(t("ocean.max_depth"), this.maxDepth, 1, 64);

        ImGuiHelper.separatorWithText(t("ocean.section.waves"));
        ImGui.sliderFloat(t("ocean.amplitude"), this.amplitude, 0.5f, 24.0f, "%.1f");
        ImGui.sliderFloat(t("ocean.wavelength"), this.wavelength, 4.0f, 96.0f, "%.0f");
        ImGui.sliderFloat(t("wave.steepness"), this.steepness, 0.0f, 1.0f, "%.2f");
        ImGui.sliderFloat(t("ocean.cross_sea"), this.crossSea, 0.0f, 1.0f, "%.2f");
        ImGui.sliderFloat(t("wave.chop"), this.chop, 0.0f, 1.0f, "%.2f");
        ImGui.sliderFloat(t("wave.foam"), this.foam, 0.0f, 1.0f, "%.2f");
        ImGui.sliderInt(t("wave.variant"), this.variant, 1, 100);

        ImGuiHelper.separatorWithText(t("ocean.section.shore"));
        ImGui.sliderInt(t("ocean.shore_fade"), this.shoreFade, 0, 24);
        ImGui.sliderInt(t("ocean.shore_foam"), this.shoreFoam, 0, 6);

        ImGuiHelper.separatorWithText(t("wave.section.blocks"));
        TreeTool.combo("wave.material", this.material, MATERIAL_KEYS, "wave.material.");

        ImGuiHelper.separatorWithText(t("wave.section.direction"));
        if (ImGui.checkbox(t("ocean.face_camera"), this.faceCamera)) {
            this.faceCamera = !this.faceCamera;
        }
        if (!this.faceCamera) {
            ImGui.sliderFloat(t("wave.angle"), this.angle, 0.0f, 359.0f, "%.0f");
        }
        ImGui.textWrapped(t("ocean.hint"));
    }

    @Override
    public void writeSettings(CompoundTag tag) {
        tag.putInt("BrushRadius", this.brushRadius[0]);
        tag.putInt("SeaLevelMode", this.seaLevelMode[0]);
        tag.putInt("SeaLevelOffset", this.seaLevelOffset[0]);
        tag.putInt("FixedSeaLevel", this.fixedSeaLevel[0]);
        tag.putInt("MaxDepth", this.maxDepth[0]);
        tag.putFloat("Amplitude", this.amplitude[0]);
        tag.putFloat("Wavelength", this.wavelength[0]);
        tag.putFloat("Steepness", this.steepness[0]);
        tag.putFloat("CrossSea", this.crossSea[0]);
        tag.putFloat("Chop", this.chop[0]);
        tag.putFloat("Foam", this.foam[0]);
        tag.putInt("ShoreFade", this.shoreFade[0]);
        tag.putInt("ShoreFoam", this.shoreFoam[0]);
        tag.putBoolean("FaceCamera", this.faceCamera);
        tag.putFloat("Angle", this.angle[0]);
        tag.putInt("Material", this.material[0]);
        tag.putInt("Variant", this.variant[0]);
    }

    @Override
    public void loadSettings(CompoundTag tag) {
        this.brushRadius[0] = tag.getIntOr("BrushRadius", 12);
        this.seaLevelMode[0] = Math.clamp(tag.getIntOr("SeaLevelMode", SEA_LEVEL_FIRST_CLICK), 0, SEA_LEVEL_KEYS.length - 1);
        this.seaLevelOffset[0] = tag.getIntOr("SeaLevelOffset", 1);
        this.fixedSeaLevel[0] = tag.getIntOr("FixedSeaLevel", 63);
        this.maxDepth[0] = tag.getIntOr("MaxDepth", 16);
        this.amplitude[0] = tag.getFloatOr("Amplitude", 3.0f);
        this.wavelength[0] = tag.getFloatOr("Wavelength", 18.0f);
        this.steepness[0] = tag.getFloatOr("Steepness", 0.5f);
        this.crossSea[0] = tag.getFloatOr("CrossSea", 0.35f);
        this.chop[0] = tag.getFloatOr("Chop", 0.4f);
        this.foam[0] = tag.getFloatOr("Foam", 0.5f);
        this.shoreFade[0] = tag.getIntOr("ShoreFade", 6);
        this.shoreFoam[0] = tag.getIntOr("ShoreFoam", 2);
        this.faceCamera = tag.getBooleanOr("FaceCamera", true);
        this.angle[0] = tag.getFloatOr("Angle", 0);
        this.material[0] = Math.clamp(tag.getIntOr("Material", 0), 0, MATERIAL_KEYS.length - 1);
        this.variant[0] = tag.getIntOr("Variant", 1);
    }

}
