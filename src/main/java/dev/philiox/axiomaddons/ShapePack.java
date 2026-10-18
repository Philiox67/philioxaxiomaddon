package dev.philiox.axiomaddons;

import com.moulberry.axiom.editor.ImGuiHelper;
import dev.philiox.axiomaddons.ExtraShapes.ShapeDefinition;
import dev.philiox.axiomaddons.ExtraShapes.ShapeTest;
import imgui.moulberry92.ImGui;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * The big shape pack, appended behind the shapes of {@link ExtraShapes} and {@link MoreShapes}.
 * Every entry brings its own options and test, coordinates are normalized to -1..1.
 */
final class ShapePack {

    interface PackShape {
        String key();

        default float @Nullable [] defaultSize() {
            return null;
        }

        boolean displayOptions();

        ShapeDefinition create(float sizeX, float sizeY, float sizeZ);
    }

    private ShapePack() {
    }

    static final List<PackShape> SHAPES = Stream.concat(Stream.of(
        new GreekColumn(), new Vase(), new Bell(),
        new Mushroom(), new Cloud(), new Rock(), new Planet(), new Pumpkin(),
        new Gem(), new Gear(), new Cross(), new Arrow(), new Lightning(), new TorusKnot(), new Dna(), new Shark(),
        new SantaHat(), new WitchHat()), Stream.concat(FunShapes.all().stream(), ThemedShapes.all().stream())).toList();

    // ---- Shared helpers ----

    static String t(String key) {
        return I18n.get("philioxaxiomaddons." + key);
    }

    static boolean slider(String key, float[] value, float min, float max) {
        return ImGui.sliderFloat(t(key), value, min, max, "%.2f");
    }

    static boolean slider(String key, int[] value, int min, int max) {
        return ImGui.sliderInt(t(key), value, min, max);
    }

    static boolean header(String key) {
        ImGuiHelper.separatorWithText(t("shape." + key));
        return false;
    }

    static double sq(double value) {
        return value * value;
    }

    static double h01(float y) {
        return (y + 1) / 2;
    }

    static boolean inBox(float x, float y, float z) {
        return Math.abs(x) <= 1 && Math.abs(y) <= 1 && Math.abs(z) <= 1;
    }

    /** Flat 2D outline in the u/v plane with a thickness along w, standing (X/Y) or lying (X/Z). */
    abstract static class FlatShape implements PackShape {
        final float[] thickness = {0.3f};
        final int[] orientation = {0};

        boolean displayFlatOptions() {
            boolean changed = slider("star.thickness", this.thickness, 0.05f, 1.0f);
            changed |= ExtraShapes.combo("philioxaxiomaddons.star.orientation", this.orientation,
                "philioxaxiomaddons.star.orientation.standing", "philioxaxiomaddons.star.orientation.lying");
            return changed;
        }

        abstract boolean outline(double u, double v);

        @Override
        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            boolean lying = this.orientation[0] == 1;
            double depth = this.thickness[0];
            return ShapeDefinition.simple((x, y, z) -> {
                if (!inBox(x, y, z)) {
                    return false;
                }
                double u = x;
                double v = lying ? -z : y;
                double w = lying ? y : z;
                return Math.abs(w) <= depth && this.outline(u, v);
            });
        }
    }

    /** Point in polygon (even odd rule). */
    static boolean insidePolygon(double[] xs, double[] ys, double px, double py) {
        boolean inside = false;
        for (int i = 0, j = xs.length - 1; i < xs.length; j = i++) {
            if ((ys[i] > py) != (ys[j] > py) && px < (xs[j] - xs[i]) * (py - ys[i]) / (ys[j] - ys[i]) + xs[i]) {
                inside = !inside;
            }
        }
        return inside;
    }

    /** Smooth 3D value noise in -1..1. */
    static double noise(double x, double y, double z, long seed) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int z0 = (int) Math.floor(z);
        double fx = smooth(x - x0);
        double fy = smooth(y - y0);
        double fz = smooth(z - z0);
        double result = 0;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    double weight = (dx == 1 ? fx : 1 - fx) * (dy == 1 ? fy : 1 - fy) * (dz == 1 ? fz : 1 - fz);
                    result += weight * hash(x0 + dx, y0 + dy, z0 + dz, seed);
                }
            }
        }
        return result;
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double hash(int x, int y, int z, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h & 0xFFFFFF) / (double) 0x7FFFFF - 1;
    }

    // ---- Architecture ----

    static final class GreekColumn implements PackShape {
        final int[] flutes = {16};
        final float[] fluteDepth = {0.1f};
        boolean capital = true;
        boolean base = true;

        public String key() {
            return "greek_column";
        }

        public float[] defaultSize() {
            return new float[] {9, 31, 9};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("pack.flutes", this.flutes, 0, 32);
            changed |= slider("pack.flute_depth", this.fluteDepth, 0.0f, 0.3f);
            if (ImGui.checkbox(t("pack.capital"), this.capital)) {
                this.capital = !this.capital;
                changed = true;
            }
            if (ImGui.checkbox(t("pack.base"), this.base)) {
                this.base = !this.base;
                changed = true;
            }
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            int flutes = this.flutes[0];
            double depth = this.fluteDepth[0];
            boolean capital = this.capital;
            boolean base = this.base;
            return ShapeDefinition.simple((x, y, z) -> {
                if (!inBox(x, y, z)) {
                    return false;
                }
                double h = h01(y);
                if (base && h < 0.06) {
                    return true;
                }
                if (capital && h > 0.94) {
                    return true;
                }
                double radial = Math.sqrt(x * x + z * z);
                if (base && h < 0.1) {
                    return radial <= 0.85;
                }
                if (capital && h > 0.88) {
                    // Echinus, widening towards the slab on top
                    return radial <= 0.68 + (h - 0.88) / 0.06 * 0.25;
                }
                // Shaft with a slight taper towards the top
                double radius = 0.7 - 0.08 * h;
                if (flutes > 0) {
                    double angle = Math.atan2(z, x);
                    radius *= 1 - depth * Math.max(0, Math.cos(flutes * angle));
                }
                return radial <= radius;
            });
        }
    }

    static final class Vase implements PackShape {
        final float[] foot = {0.5f};
        final float[] belly = {0.4f};
        final float[] neck = {0.4f};
        boolean lip = true;

        public String key() {
            return "vase";
        }

        public float[] defaultSize() {
            return new float[] {13, 21, 13};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("pack.foot", this.foot, 0.1f, 1.0f);
            changed |= slider("pack.belly", this.belly, 0.15f, 0.8f);
            changed |= slider("pack.neck", this.neck, 0.1f, 0.9f);
            if (ImGui.checkbox(t("pack.lip"), this.lip)) {
                this.lip = !this.lip;
                changed = true;
            }
            ImGui.textWrapped(t("pack.hollow_hint"));
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double foot = this.foot[0];
            double belly = this.belly[0];
            double neck = this.neck[0];
            boolean lip = this.lip;
            ShapeTest vase = createVase(foot, belly, neck, lip, false);
            return new ShapeDefinition(vase, createVase(foot, belly, neck, lip, true), false, null, null);
        }

        private static ShapeTest createVase(double foot, double belly, double neck, boolean lip, boolean openTop) {
            return (x, y, z) -> {
                if (y < -1 || (y > 1 && !openTop)) {
                    return false;
                }
                double h = Math.min(1, h01(y));
                double radius;
                if (h < belly) {
                    radius = foot + (1 - foot) * Math.sin(Math.PI / 2 * h / belly);
                } else {
                    double s = (h - belly) / (1 - belly);
                    radius = neck + (1 - neck) * Math.cos(Math.PI / 2 * Math.min(1, s / 0.6));
                }
                if (lip && h > 0.92) {
                    radius = Math.min(1, neck + 0.15);
                }
                return x * x + z * z <= radius * radius;
            };
        }
    }

    static final class Bell implements PackShape {
        final float[] flare = {0.45f};

        public String key() {
            return "bell";
        }

        public float[] defaultSize() {
            return new float[] {15, 17, 15};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("pack.flare", this.flare, 0.0f, 0.8f);
            ImGui.textWrapped(t("pack.hollow_hint"));
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double flare = this.flare[0];
            return new ShapeDefinition(createBell(flare, false), createBell(flare, true), false, null, null);
        }

        private static ShapeTest createBell(double flare, boolean openBottom) {
            double waist = 1 - flare;
            return (x, y, z) -> {
                if (y > 1 || (y < -1 && !openBottom)) {
                    return false;
                }
                double h = Math.max(0, h01(y));
                double radius;
                if (h < 0.3) {
                    radius = waist + flare * sq(1 - h / 0.3);
                } else if (h < 0.85) {
                    radius = waist - 0.05 * (h - 0.3) / 0.55;
                } else {
                    radius = (waist - 0.05) * Math.sqrt(Math.max(0, 1 - sq((h - 0.85) / 0.15)));
                }
                return x * x + z * z <= radius * radius;
            };
        }
    }

    // ---- Nature ----

    static final class Mushroom implements PackShape {
        final float[] stemWidth = {0.25f};
        final float[] capHeight = {0.45f};
        final float[] capCurl = {0.15f};

        public String key() {
            return "mushroom";
        }

        public boolean displayOptions() {
            header(this.key());
            return slider("pack.stem_width", this.stemWidth, 0.05f, 0.6f)
                | slider("pack.cap_height", this.capHeight, 0.15f, 0.9f)
                | slider("pack.cap_curl", this.capCurl, 0.0f, 0.5f);
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double stem = this.stemWidth[0];
            double capBottom = 1 - this.capHeight[0];
            double curl = this.capCurl[0];
            return ShapeDefinition.simple((x, y, z) -> {
                if (!inBox(x, y, z)) {
                    return false;
                }
                double h = h01(y);
                double radial = Math.sqrt(x * x + z * z);
                if (h < capBottom) {
                    // Cap rim curls down over the stem
                    double rimTop = capBottom - curl * (1 - capBottom);
                    if (h >= rimTop && radial >= 0.85) {
                        return radial <= 1;
                    }
                    return radial <= stem * (1 + 0.3 * (1 - h / capBottom));
                }
                return sq(radial) + sq((h - capBottom) / (1 - capBottom)) <= 1;
            });
        }
    }

    static final class Cloud implements PackShape {
        final int[] puffs = {8};
        final int[] variant = {1};
        boolean flatBottom = true;

        public String key() {
            return "cloud";
        }

        public float[] defaultSize() {
            return new float[] {31, 13, 19};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("pack.puffs", this.puffs, 2, 24) | slider("crystal.variant", this.variant, 1, 50);
            if (ImGui.checkbox(t("pack.flat_bottom"), this.flatBottom)) {
                this.flatBottom = !this.flatBottom;
                changed = true;
            }
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            int count = Math.max(1, this.puffs[0]);
            Random random = new Random(this.variant[0] * 31337L);
            double[][] puffs = new double[count][4];
            for (int i = 0; i < count; i++) {
                double radius = 0.3 + random.nextDouble() * 0.3;
                puffs[i][0] = (random.nextDouble() * 2 - 1) * (1 - radius);
                puffs[i][1] = (random.nextDouble() * 0.8 - 0.4) * (1 - radius);
                puffs[i][2] = (random.nextDouble() * 2 - 1) * (1 - radius) * 0.7;
                puffs[i][3] = radius;
            }
            boolean flat = this.flatBottom;
            return ShapeDefinition.simple((x, y, z) -> {
                if (!inBox(x, y, z) || (flat && y < -0.45)) {
                    return false;
                }
                for (double[] puff : puffs) {
                    if (sq(x - puff[0]) + sq((y - puff[1]) * 1.4) + sq(z - puff[2]) <= sq(puff[3])) {
                        return true;
                    }
                }
                return false;
            });
        }
    }

    static final class Rock implements PackShape {
        final float[] roughness = {0.3f};
        final float[] detail = {2.5f};
        final int[] variant = {1};
        boolean flatBottom = true;

        public String key() {
            return "rock";
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("pack.roughness", this.roughness, 0.0f, 0.7f)
                | slider("pack.detail", this.detail, 0.5f, 8.0f)
                | slider("crystal.variant", this.variant, 1, 50);
            if (ImGui.checkbox(t("pack.flat_bottom"), this.flatBottom)) {
                this.flatBottom = !this.flatBottom;
                changed = true;
            }
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double roughness = this.roughness[0];
            double detail = this.detail[0];
            long seed = this.variant[0] * 2654435761L;
            boolean flat = this.flatBottom;
            return ShapeDefinition.simple((x, y, z) -> {
                if (!inBox(x, y, z) || (flat && y < -0.6)) {
                    return false;
                }
                double distance = Math.sqrt(x * x + y * y + z * z);
                if (distance < 1e-6) {
                    return true;
                }
                double n = noise(x / distance * detail, y / distance * detail, z / distance * detail, seed) * 0.7
                    + noise(x * detail * 2.3, y * detail * 2.3, z * detail * 2.3, seed + 1) * 0.3;
                return distance <= (1 - roughness) + roughness * (n * 0.5 + 0.5);
            });
        }
    }

    static final class Planet implements PackShape {
        final int[] craters = {12};
        final float[] craterSize = {0.15f};
        final float[] craterDepth = {0.5f};
        final int[] variant = {1};
        boolean ring = false;

        public String key() {
            return "planet";
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("pack.craters", this.craters, 0, 40)
                | slider("pack.crater_size", this.craterSize, 0.04f, 0.4f)
                | slider("pack.crater_depth", this.craterDepth, 0.05f, 1.0f)
                | slider("crystal.variant", this.variant, 1, 50);
            if (ImGui.checkbox(t("pack.ring"), this.ring)) {
                this.ring = !this.ring;
                changed = true;
            }
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            boolean ring = this.ring;
            double radius = ring ? 0.55 : 1;
            int count = this.craters[0];
            Random random = new Random(this.variant[0] * 99991L);
            double[][] craters = new double[count][4];
            for (int i = 0; i < count; i++) {
                double dx = random.nextGaussian();
                double dy = random.nextGaussian();
                double dz = random.nextGaussian();
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double size = this.craterSize[0] * radius * (0.6 + random.nextDouble() * 0.8);
                double distance = radius + size * (1 - this.craterDepth[0]);
                craters[i] = new double[] {dx / length * distance, dy / length * distance, dz / length * distance, size};
            }
            double ringThickness = 2.0 / sizeY;
            return ShapeDefinition.simple((x, y, z) -> {
                if (!inBox(x, y, z)) {
                    return false;
                }
                double distanceSq = x * x + y * y + z * z;
                if (distanceSq <= radius * radius) {
                    for (double[] crater : craters) {
                        if (sq(x - crater[0]) + sq(y - crater[1]) + sq(z - crater[2]) < sq(crater[3])) {
                            return false;
                        }
                    }
                    return true;
                }
                if (ring && Math.abs(y + x * 0.25) <= ringThickness) {
                    double planar = Math.sqrt(x * x + z * z);
                    return planar >= 0.7 && planar <= 1;
                }
                return false;
            });
        }
    }

    static final class Pumpkin implements PackShape {
        final int[] ribs = {10};
        final float[] ribDepth = {0.12f};
        boolean stem = true;

        public String key() {
            return "pumpkin";
        }

        public float[] defaultSize() {
            return new float[] {21, 17, 21};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("pack.ribs", this.ribs, 3, 20) | slider("pack.rib_depth", this.ribDepth, 0.0f, 0.35f);
            if (ImGui.checkbox(t("pack.stem"), this.stem)) {
                this.stem = !this.stem;
                changed = true;
            }
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            int ribs = Math.max(1, this.ribs[0]);
            double depth = this.ribDepth[0];
            boolean stem = this.stem;
            double bodyTop = stem ? 0.75 : 1;
            return ShapeDefinition.simple((x, y, z) -> {
                if (!inBox(x, y, z)) {
                    return false;
                }
                double radial = Math.sqrt(x * x + z * z);
                if (stem && y > bodyTop - 0.1 && radial <= 0.12) {
                    return true;
                }
                double by = (y + 1) / (bodyTop + 1) * 2 - 1;
                double angle = Math.atan2(z, x);
                double scale = 1 - depth * 0.5 * (1 - Math.cos(ribs * angle));
                return sq(radial / scale) + by * by <= 1;
            });
        }
    }

    // ---- Symbols and objects ----

    static final class Gem implements PackShape {
        final int[] facets = {8};
        final float[] table = {0.55f};
        final float[] girdle = {0.35f};

        public String key() {
            return "gem";
        }

        public float[] defaultSize() {
            return new float[] {17, 15, 17};
        }

        public boolean displayOptions() {
            header(this.key());
            return slider("pack.facets", this.facets, 3, 16)
                | slider("pack.table", this.table, 0.1f, 0.9f)
                | slider("pack.girdle", this.girdle, -0.6f, 0.8f);
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            int facets = Math.max(3, this.facets[0]);
            double table = this.table[0];
            double girdle = this.girdle[0];
            double sector = 2 * Math.PI / facets;
            return ShapeDefinition.simple((x, y, z) -> {
                if (!inBox(x, y, z)) {
                    return false;
                }
                double radius = y >= girdle ? 1 - (1 - table) * (y - girdle) / (1 - girdle) : (y + 1) / (girdle + 1);
                double angle = Math.atan2(z, x);
                double local = angle - sector * Math.floor(angle / sector) - sector / 2;
                double polygon = Math.cos(sector / 2) / Math.cos(local);
                return Math.sqrt(x * x + z * z) <= radius * polygon;
            });
        }
    }

    static final class Gear implements PackShape {
        final int[] teeth = {12};
        final float[] toothDepth = {0.18f};
        final float[] hole = {0.25f};
        final float[] thickness = {0.4f};
        final int[] orientation = {0};

        public String key() {
            return "gear";
        }

        public float[] defaultSize() {
            return new float[] {25, 25, 5};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("pack.teeth", this.teeth, 4, 48)
                | slider("pack.tooth_depth", this.toothDepth, 0.03f, 0.45f)
                | slider("pack.hole", this.hole, 0.0f, 0.7f)
                | slider("star.thickness", this.thickness, 0.05f, 1.0f);
            changed |= ExtraShapes.combo("philioxaxiomaddons.star.orientation", this.orientation,
                "philioxaxiomaddons.star.orientation.standing", "philioxaxiomaddons.star.orientation.lying");
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            int teeth = Math.max(1, this.teeth[0]);
            double depth = this.toothDepth[0];
            double hole = this.hole[0];
            double thickness = this.thickness[0];
            boolean lying = this.orientation[0] == 1;
            return ShapeDefinition.simple((x, y, z) -> {
                if (!inBox(x, y, z)) {
                    return false;
                }
                double u = x;
                double v = lying ? -z : y;
                double w = lying ? y : z;
                if (Math.abs(w) > thickness) {
                    return false;
                }
                double radial = Math.sqrt(u * u + v * v);
                double angle = (Math.atan2(v, u) + Math.PI) / (2 * Math.PI) * teeth;
                double outer = angle - Math.floor(angle) < 0.5 ? 1 : 1 - depth;
                return radial <= outer && radial >= hole;
            });
        }
    }

    static final class Cross extends FlatShape {
        final float[] arm = {0.18f};
        final float[] crossHeight = {0.35f};

        public String key() {
            return "cross";
        }

        public float[] defaultSize() {
            return new float[] {13, 21, 3};
        }

        public boolean displayOptions() {
            header(this.key());
            return slider("pack.arm_width", this.arm, 0.05f, 0.45f)
                | slider("pack.cross_height", this.crossHeight, -0.3f, 0.8f)
                | this.displayFlatOptions();
        }

        boolean outline(double u, double v) {
            double arm = this.arm[0];
            return Math.abs(u) <= arm || Math.abs(v - this.crossHeight[0]) <= arm;
        }
    }

    static final class Arrow extends FlatShape {
        final float[] shaft = {0.18f};
        final float[] headLength = {0.35f};
        final float[] headWidth = {0.7f};

        public String key() {
            return "arrow";
        }

        public float[] defaultSize() {
            return new float[] {25, 13, 3};
        }

        public boolean displayOptions() {
            header(this.key());
            return slider("pack.shaft_width", this.shaft, 0.05f, 0.6f)
                | slider("pack.head_length", this.headLength, 0.1f, 0.9f)
                | slider("pack.head_width", this.headWidth, 0.2f, 1.0f)
                | this.displayFlatOptions();
        }

        boolean outline(double u, double v) {
            double headStart = 1 - 2 * this.headLength[0];
            if (u <= headStart) {
                return Math.abs(v) <= this.shaft[0];
            }
            return Math.abs(v) <= this.headWidth[0] * (1 - u) / (1 - headStart);
        }
    }

    static final class Lightning extends FlatShape {
        private static final double[] XS = {-0.10, 0.55, 0.15, 0.50, -0.35, -0.05, -0.45};
        private static final double[] YS = {1.00, 1.00, 0.25, 0.25, -1.00, -0.10, -0.10};

        public String key() {
            return "lightning";
        }

        public float[] defaultSize() {
            return new float[] {15, 25, 3};
        }

        public boolean displayOptions() {
            header(this.key());
            return this.displayFlatOptions();
        }

        boolean outline(double u, double v) {
            return insidePolygon(XS, YS, u, v);
        }
    }

    static final class TorusKnot implements PackShape {
        final int[] p = {2};
        final int[] q = {3};
        final float[] tube = {0.15f};

        public String key() {
            return "torus_knot";
        }

        public float[] defaultSize() {
            return new float[] {31, 15, 31};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("pack.knot_p", this.p, 1, 7) | slider("pack.knot_q", this.q, 1, 9);
            changed |= slider("pack.tube", this.tube, 0.04f, 0.4f);
            ImGui.textWrapped(t("pack.knot_hint"));
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            int p = Math.max(1, this.p[0]);
            int q = Math.max(1, this.q[0]);
            double tube = this.tube[0];
            double scale = 1 / (3 + tube * 3);
            int samples = 120 * Math.max(p, q);
            double[] xs = new double[samples];
            double[] ys = new double[samples];
            double[] zs = new double[samples];
            for (int i = 0; i < samples; i++) {
                double phi = 2 * Math.PI * i / samples;
                double r = Math.cos(q * phi) + 2;
                xs[i] = r * Math.cos(p * phi) * scale;
                zs[i] = r * Math.sin(p * phi) * scale;
                ys[i] = -Math.sin(q * phi) * scale * 2;
            }
            double tubeSq = sq(tube);
            return ShapeDefinition.simple((x, y, z) -> {
                if (!inBox(x, y, z)) {
                    return false;
                }
                for (int i = 0; i < samples; i++) {
                    if (sq(x - xs[i]) + sq((y - ys[i]) * 0.5) + sq(z - zs[i]) <= tubeSq) {
                        return true;
                    }
                }
                return false;
            });
        }
    }

    static final class Dna implements PackShape {
        final float[] turns = {2.0f};
        final float[] strand = {0.13f};
        final int[] rungSpacing = {3};

        public String key() {
            return "dna";
        }

        public float[] defaultSize() {
            return new float[] {15, 41, 15};
        }

        public boolean displayOptions() {
            header(this.key());
            return slider("helix.turns", this.turns, 0.25f, 8.0f)
                | slider("pack.strand", this.strand, 0.05f, 0.35f)
                | slider("pack.rung_spacing", this.rungSpacing, 0, 12);
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double turns = this.turns[0];
            double strand = this.strand[0];
            int spacing = this.rungSpacing[0];
            double radius = 1 - strand;
            double halfY = sizeY / 2;
            return ShapeDefinition.simple((x, y, z) -> {
                if (!inBox(x, y, z)) {
                    return false;
                }
                double angle = 2 * Math.PI * turns * h01(y);
                double ax = radius * Math.cos(angle);
                double az = radius * Math.sin(angle);
                if (sq(x - ax) + sq(z - az) <= sq(strand) || sq(x + ax) + sq(z + az) <= sq(strand)) {
                    return true;
                }
                if (spacing > 0 && Math.floorMod((int) Math.floor(y * halfY), spacing) == 0) {
                    // Rung through the center from one strand to the other
                    double along = (x * ax + z * az) / (radius * radius);
                    if (Math.abs(along) <= 1) {
                        return sq(x - ax * along) + sq(z - az * along) <= sq(strand * 0.6);
                    }
                }
                return false;
            });
        }
    }

    // ---- Animals ----

    /**
     * Shark swimming towards +X: torpedo body, dorsal fin, crescent tail, pectoral fins, optional open mouth,
     * eye sockets and a hammerhead.
     */
    static final class Shark implements PackShape {
        final int[] type = {0};
        final float[] body = {0.38f};
        final float[] dorsal = {1.0f};
        final float[] tail = {1.0f};
        boolean pectoral = true;
        final float[] mouth = {0.4f};
        final int[] finThickness = {1};
        boolean eyes = true;

        public String key() {
            return "shark";
        }

        public float[] defaultSize() {
            return new float[] {41, 21, 25};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = ExtraShapes.combo("philioxaxiomaddons.pack.shark_type", this.type,
                "philioxaxiomaddons.pack.shark_type.white", "philioxaxiomaddons.pack.shark_type.hammer");
            changed |= slider("pack.body_thickness", this.body, 0.2f, 0.6f);
            changed |= slider("pack.dorsal_size", this.dorsal, 0.0f, 1.5f);
            changed |= slider("pack.tail_size", this.tail, 0.3f, 1.3f);
            changed |= slider("pack.mouth_open", this.mouth, 0.0f, 1.0f);
            changed |= slider("pack.fin_thickness", this.finThickness, 1, 4);
            if (ImGui.checkbox(t("pack.pectoral"), this.pectoral)) {
                this.pectoral = !this.pectoral;
                changed = true;
            }
            if (ImGui.checkbox(t("pack.eyes"), this.eyes)) {
                this.eyes = !this.eyes;
                changed = true;
            }
            ImGui.textWrapped(t("pack.shark_hint"));
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            boolean hammer = this.type[0] == 1;
            double radius = this.body[0];
            double dorsal = this.dorsal[0];
            double tail = this.tail[0];
            boolean pectoral = this.pectoral;
            double mouth = this.mouth[0];
            boolean eyes = this.eyes;
            double finZ = Math.max(1, this.finThickness[0]) / sizeZ;
            double finY = Math.max(1, this.finThickness[0]) / sizeY;
            double center = -0.1;
            double tailEnd = -0.72;
            double nose = 0.98;

            return ShapeDefinition.simple((x, y, z) -> {
                if (!inBox(x, y, z)) {
                    return false;
                }
                double bodyRadius = bodyRadius(x, tailEnd, nose, radius);
                double dy = y - center;

                // Open mouth: the lower jaw drops away under the nose
                if (mouth > 0 && x > 0.6 && dy < -0.02) {
                    double gap = mouth * 0.3 * (x - 0.6) / (nose - 0.6);
                    if (dy > -0.02 - gap) {
                        return false;
                    }
                }
                if (eyes) {
                    double eyeRadius = Math.max(0.05, 1.2 / sizeX);
                    double side = bodyRadius * 0.85 * 0.85;
                    if (sq(x - 0.78) + sq(dy - 0.1) + sq(Math.abs(z) - side) < sq(eyeRadius)) {
                        return false;
                    }
                }

                // Torpedo body with a slightly flatter belly
                if (bodyRadius > 0) {
                    double vertical = dy < 0 ? dy / (bodyRadius * 0.8) : dy / bodyRadius;
                    if (sq(z / (bodyRadius * 0.85)) + sq(vertical) <= 1) {
                        return true;
                    }
                }
                if (hammer && x >= 0.8 && x <= 0.97 && Math.abs(z) <= 0.62 && Math.abs(dy) <= 0.09) {
                    return true;
                }

                if (Math.abs(z) <= finZ) {
                    double top = center + radius * 0.8;
                    if (dorsal > 0 && triangle(x, y, 0.05, top, 0.38, top, -0.12, top + 0.55 * dorsal)) {
                        return true;
                    }
                    if (triangle(x, y, -0.62, center + 0.08, -0.75, center - 0.04, -1.0, center + 0.8 * tail)) {
                        return true;
                    }
                    if (triangle(x, y, -0.62, center - 0.06, -0.75, center + 0.04, -0.92, center - 0.55 * tail)) {
                        return true;
                    }
                }
                if (pectoral && Math.abs(z) > 0.2) {
                    // Fins angle down and backwards from the sides of the body
                    double plane = center - radius * 0.45 - (Math.abs(z) - 0.25) * 0.35;
                    if (Math.abs(y - plane) <= finY && triangle(x, Math.abs(z), 0.38, 0.25, 0.2, 0.25, 0.02, 0.95)) {
                        return true;
                    }
                }
                return false;
            });
        }

        private static double bodyRadius(double x, double tailEnd, double nose, double radius) {
            double t = (x - tailEnd) / (nose - tailEnd);
            if (t < 0 || t > 1) {
                return 0;
            }
            if (t > 0.7) {
                return radius * Math.pow(Math.cos(Math.PI / 2 * (t - 0.7) / 0.3), 0.7);
            }
            return radius * Math.pow(Math.sin(Math.PI / 2 * t / 0.7), 0.6);
        }

        private static boolean triangle(double px, double py, double ax, double ay, double bx, double by, double cx, double cy) {
            double d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by);
            double d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy);
            double d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay);
            boolean negative = d1 < 0 || d2 < 0 || d3 < 0;
            boolean positive = d1 > 0 || d2 > 0 || d3 > 0;
            return !(negative && positive);
        }
    }

    // ---- Hats ----

    /** Points along a curve with a radius each, a shape is inside if it is within the radius of any point. */
    static boolean insideSweep(double x, double y, double z, double[][] points) {
        for (double[] point : points) {
            if (sq(x - point[0]) + sq(y - point[1]) + sq(z - point[2]) <= sq(point[3])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Santa hat: fur brim around the bottom, a cap that flops over towards +X and a pompom at the tip.
     * Each part can be built on its own, so they can get different blocks.
     */
    static final class SantaHat implements PackShape {
        private static final int PART_ALL = 0;
        private static final int PART_CAP = 1;
        private static final int PART_BRIM = 2;
        private static final int PART_POMPOM = 3;

        final int[] part = {PART_ALL};
        final float[] droop = {0.8f};
        final float[] pompom = {0.18f};
        final float[] brimHeight = {0.28f};

        public String key() {
            return "santa_hat";
        }

        public float[] defaultSize() {
            return new float[] {21, 25, 21};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = ExtraShapes.combo("philioxaxiomaddons.pack.part", this.part,
                "philioxaxiomaddons.pack.part.all", "philioxaxiomaddons.pack.part.cap",
                "philioxaxiomaddons.pack.part.fur_brim", "philioxaxiomaddons.pack.part.pompom");
            changed |= slider("pack.droop", this.droop, 0.0f, 1.0f);
            changed |= slider("pack.pompom", this.pompom, 0.0f, 0.35f);
            changed |= slider("pack.brim_height", this.brimHeight, 0.1f, 0.5f);
            ImGui.textWrapped(t("pack.hat_hint"));
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            int part = this.part[0];
            double pompomRadius = this.pompom[0];
            double brimHeight = this.brimHeight[0];
            double droop = this.droop[0];

            // Brim: a soft ring around the bottom
            double brimY = -1 + brimHeight / 2;
            double bandThickness = 0.3;
            double ringRadius = 1 - bandThickness / 2;

            // Cap: a tapering tube along a curve that rises, bends over and hangs down at the side
            double top = 1 - 0.05;
            double tipX = Math.min(0.95 - pompomRadius, 0.3 + droop * 0.6);
            double tipY = 0.65 - droop * 0.75;
            int samples = 96;
            double[][] cap = new double[samples][];
            double tipPointX = 0;
            double tipPointY = 0;
            for (int i = 0; i < samples; i++) {
                double s = i / (double) (samples - 1);
                double u = 1 - s;
                // Cubic Bezier: straight up, over the top, then down to the tip
                double cx = 3 * u * s * s * (tipX * 0.5) + s * s * s * tipX;
                double cy = u * u * u * brimY + 3 * u * u * s * top + 3 * u * s * s * (top + 0.05) + s * s * s * tipY;
                // Narrows quickly, so the hanging tip stays visible next to the cap
                double radius = ringRadius * Math.pow(u, 3) + Math.max(0.06, 1.0 / sizeX);
                cap[i] = new double[] {cx, cy, 0, radius};
                tipPointX = cx;
                tipPointY = cy;
            }
            double pompomX = tipPointX;
            double pompomY = tipPointY;

            return ShapeDefinition.simple((x, y, z) -> {
                if (!inBox(x, y, z)) {
                    return false;
                }
                double radial = Math.sqrt(x * x + z * z);
                boolean brim = sq((radial - ringRadius) / (bandThickness / 2)) + sq((y - brimY) / (brimHeight / 2)) <= 1;
                boolean ball = pompomRadius > 0 && sq(x - pompomX) + sq(y - pompomY) + z * z <= sq(pompomRadius);
                boolean capPart = y >= brimY && insideSweep(x, y, z, cap);
                return switch (part) {
                    case PART_CAP -> capPart && !brim && !ball;
                    case PART_BRIM -> brim;
                    case PART_POMPOM -> ball;
                    default -> brim || ball || capPart;
                };
            });
        }
    }

    /**
     * Witch hat: wide brim with an upturned edge, a tall cone with a crooked tip and a band around its base.
     */
    static final class WitchHat implements PackShape {
        private static final int PART_ALL = 0;
        private static final int PART_BRIM = 1;
        private static final int PART_CROWN = 2;
        private static final int PART_BAND = 3;

        final int[] part = {PART_ALL};
        final float[] crownRadius = {0.42f};
        final float[] bend = {0.5f};
        final float[] brimCurl = {0.4f};
        final int[] brimThickness = {1};
        final float[] bandHeight = {0.12f};

        public String key() {
            return "witch_hat";
        }

        public float[] defaultSize() {
            return new float[] {31, 31, 31};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = ExtraShapes.combo("philioxaxiomaddons.pack.part", this.part,
                "philioxaxiomaddons.pack.part.all", "philioxaxiomaddons.pack.part.brim",
                "philioxaxiomaddons.pack.part.crown", "philioxaxiomaddons.pack.part.band");
            changed |= slider("pack.crown_radius", this.crownRadius, 0.15f, 0.7f);
            changed |= slider("pack.bend", this.bend, 0.0f, 1.0f);
            changed |= slider("pack.brim_curl", this.brimCurl, 0.0f, 1.0f);
            changed |= slider("pack.brim_thickness", this.brimThickness, 1, 4);
            changed |= slider("pack.band_height", this.bandHeight, 0.0f, 0.3f);
            ImGui.textWrapped(t("pack.hat_hint"));
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            int part = this.part[0];
            double crownRadius = this.crownRadius[0];
            double bend = this.bend[0];
            double curl = this.brimCurl[0];
            double brimThickness = 2.0 * Math.max(1, this.brimThickness[0]) / sizeY;
            double bandHeight = this.bandHeight[0];
            // The curled edge rises, the crown starts on top of the brim
            double crownBase = -1 + brimThickness + curl * 0.15;
            double bandThickness = 2.0 / sizeX;

            return ShapeDefinition.simple((x, y, z) -> {
                if (!inBox(x, y, z)) {
                    return false;
                }
                double radial = Math.sqrt(x * x + z * z);

                // Brim: flat in the middle, the outer edge curls up
                double brimSurface = -1 + curl * 0.3 * Math.pow(radial, 4);
                boolean brim = radial <= 1 && y >= brimSurface && y <= brimSurface + brimThickness;

                // Crown: cone with a tip that bends towards +X near the top
                boolean crown = false;
                boolean band = false;
                if (y >= crownBase - brimThickness) {
                    double h = Math.clamp((y - crownBase) / (1 - crownBase), 0, 1);
                    double offset = bend * 0.45 * Math.pow(h, 3);
                    double radius = crownRadius * Math.pow(1 - h, 1.1) + 0.01;
                    double distance = Math.sqrt(sq(x - offset) + z * z);
                    crown = distance <= radius;
                    band = bandHeight > 0 && h <= bandHeight && distance <= radius + bandThickness;
                }
                return switch (part) {
                    case PART_BRIM -> brim;
                    case PART_CROWN -> crown && !band;
                    case PART_BAND -> band;
                    default -> brim || crown || band;
                };
            });
        }
    }

}
