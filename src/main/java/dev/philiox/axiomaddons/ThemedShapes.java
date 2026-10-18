package dev.philiox.axiomaddons;

import dev.philiox.axiomaddons.ExtraShapes.ShapeDefinition;
import dev.philiox.axiomaddons.FunShapes.Part;
import dev.philiox.axiomaddons.FunShapes.ShapePart;
import dev.philiox.axiomaddons.ShapePack.PackShape;
import imgui.moulberry92.ImGui;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static dev.philiox.axiomaddons.FunShapes.checkbox;
import static dev.philiox.axiomaddons.FunShapes.colour;
import static dev.philiox.axiomaddons.FunShapes.colourCombo;
import static dev.philiox.axiomaddons.FunShapes.insideSegments;
import static dev.philiox.axiomaddons.FunShapes.segment;
import static dev.philiox.axiomaddons.ShapePack.header;
import static dev.philiox.axiomaddons.ShapePack.slider;
import static dev.philiox.axiomaddons.ShapePack.sq;
import static dev.philiox.axiomaddons.ShapePack.t;

/**
 * Groin Vault, Flying Buttress, Pagoda Roof, Giant Roots, Basalt Columns, Seashell, Dragon Wing,
 * Hot Air Balloon and Fly Agaric. Everything is calculated in block units.
 */
final class ThemedShapes {

    private ThemedShapes() {
    }

    static List<PackShape> all() {
        return List.of(new GroinVault(), new FlyingButtress(), new PagodaRoof(), new GiantRoots(), new BasaltColumns(),
            new Seashell(), new DragonWing(), new HotAirBalloon(), new FlyAgaric());
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.clamp((x - edge0) / (edge1 - edge0), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static boolean triangle(double px, double py, double ax, double ay, double bx, double by, double cx, double cy) {
        double d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by);
        double d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy);
        double d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay);
        boolean negative = d1 < 0 || d2 < 0 || d3 < 0;
        boolean positive = d1 > 0 || d2 > 0 || d3 > 0;
        return !(negative && positive);
    }

    private static long hash(long a, long b, long seed) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return h;
    }

    private static double hash01(long a, long b, long seed) {
        return (hash(a, b, seed) & 0xFFFFFF) / (double) 0xFFFFFF;
    }

    // ---- Groin vault ----

    /**
     * Two barrel vaults crossing in every bay, carried by piers. Bays run along X.
     */
    static final class GroinVault implements PackShape {
        final int[] bays = {3};
        final float[] pierHeight = {0.45f};
        final int[] thickness = {1};
        boolean ribs = true;
        boolean piers = true;
        boolean fillAbove = false;

        public String key() {
            return "groin_vault";
        }

        public float[] defaultSize() {
            return new float[] {31, 15, 13};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("arch2.bays", this.bays, 1, 10);
            changed |= slider("arch2.pier_height", this.pierHeight, 0.0f, 0.8f);
            changed |= slider("arch2.vault_thickness", this.thickness, 1, 4);
            if (checkbox("arch2.ribs", this.ribs)) {
                this.ribs = !this.ribs;
                changed = true;
            }
            if (checkbox("arch2.piers", this.piers)) {
                this.piers = !this.piers;
                changed = true;
            }
            if (checkbox("arch2.fill_above", this.fillAbove)) {
                this.fillAbove = !this.fillAbove;
                changed = true;
            }
            ImGui.textWrapped(t("arch2.vault_hint"));
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            int bays = Math.max(1, this.bays[0]);
            double bayLength = 2 * hx / bays;
            int thickness = Math.max(1, this.thickness[0]);
            double spring = -hy + 2 * hy * this.pierHeight[0];
            double rise = Math.max(1, hy - spring - thickness);
            double pierHalf = Math.max(0.6, Math.min(bayLength, 2 * hz) * 0.08);
            boolean ribs = this.ribs;
            boolean piers = this.piers;
            boolean fill = this.fillAbove;

            ShapePart part = (bx, by, bz) -> {
                // Position inside the current bay, 0 in the middle of it
                double offset = bx + hx;
                double local = offset - Math.floor(offset / bayLength) * bayLength - bayLength / 2;
                double toBoundary = bayLength / 2 - Math.abs(local);
                if (piers && toBoundary <= pierHalf && Math.abs(bz) >= hz - 2 * pierHalf && by <= spring + 0.5) {
                    return Part.A;
                }
                if (by < spring - 1) {
                    return null;
                }
                double across = Math.clamp(Math.abs(bz) / hz, 0, 1);
                double along = Math.clamp(Math.abs(local) / (bayLength / 2), 0, 1);
                double barrelX = rise * Math.sqrt(1 - across * across);
                double barrelZ = rise * Math.sqrt(1 - along * along);
                // The open space is the union of both tunnels, the ceiling follows the higher one
                double intrados = spring + Math.max(barrelX, barrelZ);
                boolean rib = ribs && (Math.abs(barrelX - barrelZ) < 1.0 || toBoundary < 0.75);
                double lower = rib ? intrados - 1 : intrados;
                if (by >= lower && (fill || by <= intrados + thickness)) {
                    return rib ? Part.B : Part.A;
                }
                return null;
            };
            return part.definition(hx, hy, hz, false, p -> null);
        }
    }

    // ---- Flying buttress ----

    /**
     * Pier on the left, the flying arch leans over to the wall on the right. Flat in X/Y.
     */
    static final class FlyingButtress implements PackShape {
        final int[] pierWidth = {3};
        final int[] archThickness = {2};
        final float[] pierHeight = {0.6f};
        boolean pinnacle = true;

        public String key() {
            return "flying_buttress";
        }

        public float[] defaultSize() {
            return new float[] {21, 31, 3};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("arch2.pier_width", this.pierWidth, 2, 8);
            changed |= slider("arch2.arch_thickness", this.archThickness, 1, 5);
            changed |= slider("arch2.pier_height", this.pierHeight, 0.3f, 0.9f);
            if (checkbox("arch2.pinnacle", this.pinnacle)) {
                this.pinnacle = !this.pinnacle;
                changed = true;
            }
            ImGui.textWrapped(t("arch2.buttress_hint"));
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            double pierWidth = Math.min(this.pierWidth[0], hx);
            double pierRight = -hx + pierWidth;
            double pierCenter = -hx + pierWidth / 2;
            double thickness = this.archThickness[0];
            double pierTop = -hy + 2 * hy * this.pierHeight[0];
            double attach = hy - thickness;
            double arcLow = pierTop - 2 * hy * 0.15;
            double spire = Math.min(hy - pierTop, pierWidth * 2.5);
            boolean pinnacle = this.pinnacle;

            ShapePart part = (bx, by, bz) -> {
                if (Math.abs(bz) > hz) {
                    return null;
                }
                if (bx >= -hx && bx <= pierRight) {
                    if (by <= pierTop) {
                        return Part.A;
                    }
                    if (pinnacle && by <= pierTop + spire) {
                        double half = pierWidth / 2 * (1 - (by - pierTop) / spire);
                        if (Math.abs(bx - pierCenter) <= Math.max(0.5, half)) {
                            return Part.A;
                        }
                    }
                }
                if (bx >= pierRight - 0.5 && bx <= hx) {
                    double rx = hx - pierRight;
                    double fromWall = Math.clamp((hx - bx) / rx, 0, 1);
                    // Curved underside like a quarter ellipse, straight sloped top
                    double under = arcLow + (attach - arcLow) * Math.sqrt(1 - fromWall * fromWall);
                    double top = attach + thickness - (attach + thickness - pierTop) * fromWall;
                    top = Math.max(top, under + thickness);
                    if (by >= under && by <= top) {
                        return Part.A;
                    }
                }
                return null;
            };
            return part.definition(hx, hy, hz, false, p -> null);
        }
    }

    // ---- Pagoda roof ----

    /**
     * Several tiers of curved roofs with upturned corners, walls between them and a finial on top.
     */
    static final class PagodaRoof implements PackShape {
        final int[] tiers = {3};
        final float[] upturn = {0.5f};
        final int[] thickness = {1};
        boolean finial = true;
        boolean autoColours = true;

        public String key() {
            return "pagoda_roof";
        }

        public float[] defaultSize() {
            return new float[] {25, 31, 25};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = false;
            if (checkbox("fun.auto_colours", this.autoColours)) {
                this.autoColours = !this.autoColours;
                changed = true;
            }
            changed |= slider("arch2.tiers", this.tiers, 1, 6);
            changed |= slider("arch2.upturn", this.upturn, 0.0f, 1.0f);
            changed |= slider("arch2.roof_thickness", this.thickness, 1, 3);
            if (checkbox("arch2.finial", this.finial)) {
                this.finial = !this.finial;
                changed = true;
            }
            ImGui.textWrapped(t("arch2.pagoda_hint"));
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            int tiers = Math.max(1, this.tiers[0]);
            double halfWidth = Math.min(hx, hz);
            double finialHeight = this.finial ? 2 * hy * 0.12 : 0;
            double base = 2 * hy * 0.12;
            double tierHeight = (2 * hy - finialHeight - base) / tiers;
            double upturn = this.upturn[0];
            int thickness = Math.max(1, this.thickness[0]);
            boolean finial = this.finial;
            double finialRadius = Math.max(0.6, halfWidth * 0.06);

            ShapePart part = (bx, by, bz) -> {
                double m = Math.max(Math.abs(bx), Math.abs(bz));
                double previousTop = -hy;
                for (int tier = 0; tier < tiers; tier++) {
                    double width = halfWidth * (1 - 0.16 * tier);
                    double core = width * 0.55;
                    double eave = -hy + base + tier * tierHeight;
                    double rise = tierHeight * 0.6;
                    if (m <= width) {
                        // Concave roof: flat at the eaves, steep near the walls, corners curl up
                        double e = Math.clamp((width - m) / (width - core), 0, 1);
                        double surface = eave + rise * Math.pow(e, 1.8);
                        double corner = Math.min(Math.abs(bx), Math.abs(bz)) / width;
                        surface += upturn * tierHeight * 0.35 * Math.pow(corner, 3) * Math.pow(m / width, 8);
                        if (by >= surface && by <= surface + thickness) {
                            return Part.A;
                        }
                    }
                    // Walls of this tier, hidden under its roof
                    if (by >= previousTop && by <= eave + rise && m <= width * 0.62) {
                        return Part.B;
                    }
                    previousTop = eave + rise;
                }
                if (finial && by >= previousTop) {
                    double radial = Math.sqrt(bx * bx + bz * bz);
                    boolean ring = Math.floorMod((int) Math.floor(by - previousTop), 3) == 1;
                    if (radial <= (ring ? finialRadius * 2.2 : finialRadius)) {
                        return Part.C;
                    }
                }
                return null;
            };
            return part.definition(hx, hy, hz, this.autoColours, p -> switch (p) {
                case B -> Blocks.RED_TERRACOTTA.defaultBlockState();
                case C -> Blocks.GOLD_BLOCK.defaultBlockState();
                default -> Blocks.DEEPSLATE_TILES.defaultBlockState();
            });
        }
    }

    // ---- Giant roots ----

    /**
     * Thick roots arching out of the ground and diving back in, around an optional trunk stub.
     */
    static final class GiantRoots implements PackShape {
        final int[] count = {6};
        final float[] archHeight = {0.6f};
        final float[] thickness = {3.0f};
        final float[] spread = {0.85f};
        boolean trunk = true;
        final int[] variant = {1};

        public String key() {
            return "giant_roots";
        }

        public float[] defaultSize() {
            return new float[] {41, 17, 41};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("arch2.root_count", this.count, 1, 12);
            changed |= slider("arch2.arch_height", this.archHeight, 0.1f, 1.0f);
            changed |= slider("arch2.root_thickness", this.thickness, 1.0f, 8.0f);
            changed |= slider("arch2.spread", this.spread, 0.3f, 1.0f);
            if (checkbox("arch2.trunk", this.trunk)) {
                this.trunk = !this.trunk;
                changed = true;
            }
            changed |= slider("crystal.variant", this.variant, 1, 50);
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            double reach = Math.min(hx, hz);
            double thick = this.thickness[0];
            boolean trunk = this.trunk;
            double trunkRadius = reach * 0.18;
            double trunkTop = -hy + 2 * hy * 0.45;
            Random random = new Random(this.variant[0] * 31L + 7);

            List<double[]> segments = new ArrayList<>();
            int count = Math.max(1, this.count[0]);
            for (int k = 0; k < count; k++) {
                double angle = k * 2 * Math.PI / count + (random.nextDouble() - 0.5) * 0.7;
                double endAngle = angle + (random.nextDouble() - 0.5) * 0.6;
                double startRadius = trunk ? trunkRadius * 0.7 : reach * 0.1;
                double sx = Math.cos(angle) * startRadius;
                double sy = -hy + 2 * hy * 0.15;
                double sz = Math.sin(angle) * startRadius;
                double distance = (reach - thick) * this.spread[0] * (0.65 + 0.35 * random.nextDouble());
                double ex = Math.cos(endAngle) * distance;
                double ey = -hy - thick;
                double ez = Math.sin(endAngle) * distance;
                double arch = 2 * hy * this.archHeight[0] * (0.6 + 0.4 * random.nextDouble());
                double cx = (sx + ex) / 2;
                double cy = Math.max(sy, ey) + arch;
                double cz = (sz + ez) / 2;
                double previousX = sx;
                double previousY = sy;
                double previousZ = sz;
                double previousRadius = thick;
                int samples = 32;
                for (int i = 1; i <= samples; i++) {
                    double s = i / (double) samples;
                    double u = 1 - s;
                    double x = u * u * sx + 2 * u * s * cx + s * s * ex;
                    double y = u * u * sy + 2 * u * s * cy + s * s * ey;
                    double z = u * u * sz + 2 * u * s * cz + s * s * ez;
                    double radius = thick * (1 - 0.6 * s) + 0.5;
                    segments.add(segment(previousX, previousY, previousZ, x, y, z, previousRadius, radius));
                    previousX = x;
                    previousY = y;
                    previousZ = z;
                    previousRadius = radius;
                }
            }

            ShapePart part = (bx, by, bz) -> {
                if (by < -hy) {
                    return null;
                }
                if (trunk && by <= trunkTop && bx * bx + bz * bz <= sq(trunkRadius * (1 + 0.3 * Math.max(0, 1 - (by + hy) / (trunkRadius * 1.5))))) {
                    return Part.A;
                }
                return insideSegments(bx, by, bz, segments) ? Part.A : null;
            };
            return part.definition(hx, hy, hz, false, p -> null);
        }
    }

    // ---- Basalt columns ----

    /**
     * Field of hexagonal columns in different heights, like the Giant's Causeway.
     */
    static final class BasaltColumns implements PackShape {
        private static final int MODE_HILL = 0;
        private static final int MODE_RANDOM = 1;
        private static final int MODE_TERRACES = 2;

        final float[] columnSize = {2.5f};
        final float[] variation = {0.5f};
        final int[] mode = {MODE_HILL};
        boolean gaps = true;
        boolean autoColours = true;
        final int[] variant = {1};

        public String key() {
            return "basalt_columns";
        }

        public float[] defaultSize() {
            return new float[] {31, 15, 31};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = false;
            if (checkbox("fun.auto_colours", this.autoColours)) {
                this.autoColours = !this.autoColours;
                changed = true;
            }
            changed |= ExtraShapes.combo("philioxaxiomaddons.arch2.basalt_mode", this.mode,
                "philioxaxiomaddons.arch2.basalt_mode.hill", "philioxaxiomaddons.arch2.basalt_mode.random",
                "philioxaxiomaddons.arch2.basalt_mode.terraces");
            changed |= slider("arch2.column_size", this.columnSize, 1.0f, 6.0f);
            changed |= slider("arch2.variation", this.variation, 0.0f, 1.0f);
            if (checkbox("arch2.gaps", this.gaps)) {
                this.gaps = !this.gaps;
                changed = true;
            }
            changed |= slider("crystal.variant", this.variant, 1, 50);
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            double size = Math.max(1, this.columnSize[0]);
            double variation = this.variation[0];
            int mode = this.mode[0];
            boolean gaps = this.gaps;
            long seed = this.variant[0] * 977L;
            double sqrt3 = Math.sqrt(3);
            double apothem = size * sqrt3 / 2;

            ShapePart part = (bx, by, bz) -> {
                // Nearest hexagon center (pointy top hexagons, axial coordinates)
                double q = (sqrt3 / 3 * bx - bz / 3) / size;
                double r = (2.0 / 3 * bz) / size;
                double cubeX = q;
                double cubeZ = r;
                double cubeY = -cubeX - cubeZ;
                long rx = Math.round(cubeX);
                long ry = Math.round(cubeY);
                long rz = Math.round(cubeZ);
                double dx = Math.abs(rx - cubeX);
                double dy = Math.abs(ry - cubeY);
                double dz = Math.abs(rz - cubeZ);
                if (dx > dy && dx > dz) {
                    rx = -ry - rz;
                } else if (dz > dy) {
                    rz = -rx - ry;
                }
                double centerX = size * sqrt3 * (rx + rz / 2.0);
                double centerZ = size * 1.5 * rz;

                double localX = Math.abs(bx - centerX);
                double localZ = Math.abs(bz - centerZ);
                double hexDistance = Math.max(localX, localX * 0.5 + localZ * sqrt3 / 2);
                if (gaps && hexDistance > apothem - 0.45) {
                    return null;
                }

                double radial = Math.sqrt(sq(centerX / hx) + sq(centerZ / hz));
                if (radial > 1) {
                    return null;
                }
                double random = hash01(rx, rz, seed);
                double height = switch (mode) {
                    case MODE_RANDOM -> (1 - variation * random) * (1 - 0.3 * radial);
                    case MODE_TERRACES -> Math.floor((1 - Math.pow(radial, 1.3)) * 5 + random * variation) / 5;
                    default -> (1 - Math.pow(radial, 1.5)) * (1 - variation * 0.6 * random);
                };
                double top = -hy + Math.max(1, height * 2 * hy);
                if (by < -hy || by > top) {
                    return null;
                }
                return switch ((int) (hash(rx, rz, seed + 1) & 3)) {
                    case 1 -> Part.B;
                    case 2 -> Part.C;
                    case 3 -> Part.D;
                    default -> Part.A;
                };
            };
            return part.definition(hx, hy, hz, this.autoColours, p -> switch (p) {
                case B -> Blocks.SMOOTH_BASALT.defaultBlockState();
                case C -> Blocks.POLISHED_BASALT.defaultBlockState();
                case D -> Blocks.BLACKSTONE.defaultBlockState();
                default -> Blocks.BASALT.defaultBlockState();
            });
        }
    }

    // ---- Seashell ----

    /**
     * Spiral sea snail shell: a tube that winds down around the Y axis and grows wider, with a flared opening.
     */
    static final class Seashell implements PackShape {
        final float[] turns = {4.0f};
        final float[] spire = {0.6f};
        final float[] tube = {0.42f};
        boolean flare = true;
        boolean autoColours = true;
        final int[] colourA = {0};
        final int[] colourB = {8};

        public String key() {
            return "seashell";
        }

        public float[] defaultSize() {
            return new float[] {25, 25, 25};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = false;
            if (checkbox("fun.auto_colours", this.autoColours)) {
                this.autoColours = !this.autoColours;
                changed = true;
            }
            if (this.autoColours) {
                changed |= colourCombo("fun.stripe_a", this.colourA);
                changed |= colourCombo("fun.stripe_b", this.colourB);
            }
            changed |= slider("arch2.turns", this.turns, 1.5f, 8.0f);
            changed |= slider("arch2.spire", this.spire, 0.0f, 1.0f);
            changed |= slider("arch2.tube", this.tube, 0.2f, 0.7f);
            if (checkbox("arch2.flare", this.flare)) {
                this.flare = !this.flare;
                changed = true;
            }
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            double reach = Math.min(hx, hz);
            double tubeRelative = this.tube[0];
            double pathMax = reach / (1 + tubeRelative);
            double tubeMax = pathMax * tubeRelative;
            double growth = Math.log(20);
            double aperture = -hy + tubeMax;
            double spireHeight = Math.max(0, hy - aperture - tubeMax * 0.1) * this.spire[0];
            double turns = this.turns[0];
            boolean flare = this.flare;

            int samples = 240;
            double[][] points = new double[samples][];
            for (int i = 0; i < samples; i++) {
                double s = i / (double) (samples - 1);
                double g = Math.exp(growth * (s - 1));
                double angle = turns * 2 * Math.PI * s;
                double path = pathMax * g;
                double radius = tubeMax * g;
                if (flare && s > 0.88) {
                    radius *= 1 + 0.4 * (s - 0.88) / 0.12;
                }
                double y = aperture + spireHeight * (1 - g);
                points[i] = new double[] {path * Math.cos(angle), y, path * Math.sin(angle), Math.max(0.5, radius), s};
            }

            ShapePart part = (bx, by, bz) -> {
                for (double[] point : points) {
                    if (sq(bx - point[0]) + sq(by - point[1]) + sq(bz - point[2]) <= point[3] * point[3]) {
                        // Bands across the shell, following its growth
                        return Math.floorMod((int) Math.floor(point[4] * turns * 3), 2) == 0 ? Part.A : Part.B;
                    }
                }
                return null;
            };
            int a = this.colourA[0];
            int b = this.colourB[0];
            return part.definition(hx, hy, hz, this.autoColours, p -> colour(p == Part.A ? a : b));
        }
    }

    // ---- Dragon wing ----

    /**
     * Bat like dragon wing in the X/Y plane: arm and finger bones with a scalloped membrane between them.
     */
    static final class DragonWing implements PackShape {
        final float[] fold = {0.0f};
        final float[] cup = {0.3f};
        final float[] boneThickness = {1.5f};
        boolean autoColours = true;
        final int[] membraneColour = {9};

        public String key() {
            return "dragon_wing";
        }

        public float[] defaultSize() {
            return new float[] {41, 31, 7};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = false;
            if (checkbox("fun.auto_colours", this.autoColours)) {
                this.autoColours = !this.autoColours;
                changed = true;
            }
            if (this.autoColours) {
                changed |= colourCombo("arch2.membrane_colour", this.membraneColour);
            }
            changed |= slider("arch2.fold", this.fold, 0.0f, 1.0f);
            changed |= slider("arch2.cup", this.cup, 0.0f, 1.0f);
            changed |= slider("arch2.bone_thickness", this.boneThickness, 0.6f, 3.0f);
            ImGui.textWrapped(t("arch2.wing_hint"));
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            double bone = this.boneThickness[0];
            double fold = this.fold[0];
            double cup = this.cup[0];

            double rootX = -hx + bone + 0.5;
            double rootY = -hy * 0.15;
            double wristX = -hx * 0.15;
            double wristY = hy * 0.7;
            double[][] tips = {
                {hx - 1, hy * 0.85}, {hx - 1, hy * 0.05}, {hx * 0.65, -hy * 0.6}, {hx * 0.1, -hy + 1}
            };
            for (double[] tip : tips) {
                // Folding pulls the fingers towards the wrist and down
                tip[0] = wristX + (tip[0] - wristX) * (1 - 0.55 * fold);
                tip[1] = wristY + (tip[1] - wristY) * (1 - 0.3 * fold) - fold * hy * 0.3;
            }

            List<double[]> bones = new ArrayList<>();
            bones.add(segment(rootX, rootY, 0, wristX, wristY, 0, bone * 1.3, bone));
            for (double[] tip : tips) {
                bones.add(segment(wristX, wristY, 0, tip[0], tip[1], 0, bone, 0.5));
            }

            // Membrane panels: between neighbouring fingers and between the last finger and the arm
            List<double[]> panels = new ArrayList<>();
            for (int i = 0; i < tips.length - 1; i++) {
                panels.add(new double[] {tips[i][0], tips[i][1], tips[i + 1][0], tips[i + 1][1]});
            }
            panels.add(new double[] {tips[tips.length - 1][0], tips[tips.length - 1][1], rootX, rootY});

            ShapePart part = (bx, by, bz) -> {
                double zCenter = cup * (hz - 0.5) * Math.sin(Math.PI * Math.clamp((bx + hx) / (2 * hx), 0, 1));
                if (insideSegments(bx, by, bz - zCenter, bones)) {
                    return Part.B;
                }
                if (Math.abs(bz - zCenter) > 0.5) {
                    return null;
                }
                for (double[] panel : panels) {
                    if (!triangle(bx, by, wristX, wristY, panel[0], panel[1], panel[2], panel[3])) {
                        continue;
                    }
                    // Scalloped trailing edge: a circle bites into the membrane between two bones
                    double midX = (panel[0] + panel[2]) / 2;
                    double midY = (panel[1] + panel[3]) / 2;
                    double length = Math.sqrt(sq(panel[2] - panel[0]) + sq(panel[3] - panel[1]));
                    double outX = midX - wristX;
                    double outY = midY - wristY;
                    double outLength = Math.max(1e-6, Math.sqrt(outX * outX + outY * outY));
                    double centerX = midX + outX / outLength * length * 0.55;
                    double centerY = midY + outY / outLength * length * 0.55;
                    if (sq(bx - centerX) + sq(by - centerY) < sq(length * 0.62)) {
                        continue;
                    }
                    return Part.A;
                }
                return null;
            };
            int membrane = this.membraneColour[0];
            return part.definition(hx, hy, hz, this.autoColours,
                p -> p == Part.B ? Blocks.BONE_BLOCK.defaultBlockState() : colour(membrane));
        }
    }

    // ---- Hot air balloon ----

    /**
     * Striped balloon envelope, ropes and a basket.
     */
    static final class HotAirBalloon implements PackShape {
        final int[] gores = {12};
        final float[] basketSize = {0.25f};
        boolean ropes = true;
        boolean autoColours = true;
        final int[] colourA = {1};
        final int[] colourB = {4};

        public String key() {
            return "hot_air_balloon";
        }

        public float[] defaultSize() {
            return new float[] {21, 35, 21};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = false;
            if (checkbox("fun.auto_colours", this.autoColours)) {
                this.autoColours = !this.autoColours;
                changed = true;
            }
            if (this.autoColours) {
                changed |= colourCombo("fun.stripe_a", this.colourA);
                changed |= colourCombo("fun.stripe_b", this.colourB);
            }
            changed |= slider("arch2.gores", this.gores, 2, 24);
            changed |= slider("arch2.basket_size", this.basketSize, 0.12f, 0.45f);
            if (checkbox("arch2.ropes", this.ropes)) {
                this.ropes = !this.ropes;
                changed = true;
            }
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            double radius = Math.min(hx, hz);
            double basket = Math.max(1, radius * this.basketSize[0]);
            double basketHeight = Math.max(2, 2 * hy * 0.1);
            double ropeLength = 2 * hy * 0.12;
            double mouth = -hy + basketHeight + ropeLength;
            double envelope = hy - mouth;
            double mouthRadius = basket * 1.1;
            int gores = Math.max(2, this.gores[0]);
            boolean ropes = this.ropes;

            List<double[]> ropeSegments = new ArrayList<>();
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    ropeSegments.add(segment(sx * (basket - 0.5), -hy + basketHeight, sz * (basket - 0.5),
                        sx * mouthRadius * 0.7, mouth, sz * mouthRadius * 0.7, 0.5, 0.5));
                }
            }

            ShapePart part = (bx, by, bz) -> {
                if (by >= mouth && by <= hy) {
                    double h = (by - mouth) / envelope;
                    double envelopeRadius = h >= 0.6
                        ? radius * Math.sqrt(Math.max(0, 1 - sq((h - 0.6) / 0.4)))
                        : mouthRadius + (radius - mouthRadius) * Math.pow(Math.sin(Math.PI / 2 * h / 0.6), 1.3);
                    if (bx * bx + bz * bz <= envelopeRadius * envelopeRadius) {
                        double angle = (Math.atan2(bz, bx) + Math.PI) / (2 * Math.PI);
                        return Math.floorMod((int) Math.floor(angle * gores), 2) == 0 ? Part.A : Part.B;
                    }
                }
                if (by >= -hy && by <= -hy + basketHeight && Math.abs(bx) <= basket && Math.abs(bz) <= basket) {
                    // Open basket with a floor
                    if (by > -hy + 1 && Math.abs(bx) < basket - 1 && Math.abs(bz) < basket - 1) {
                        return null;
                    }
                    return Part.C;
                }
                if (ropes && insideSegments(bx, by, bz, ropeSegments)) {
                    return Part.D;
                }
                return null;
            };
            int a = this.colourA[0];
            int b = this.colourB[0];
            return part.definition(hx, hy, hz, this.autoColours, p -> switch (p) {
                case C -> Blocks.SPRUCE_PLANKS.defaultBlockState();
                case D -> Blocks.OAK_FENCE.defaultBlockState();
                case B -> colour(b);
                default -> colour(a);
            });
        }
    }

    // ---- Fly agaric ----

    /**
     * Red mushroom with white dots, a stem with a ring and a bulb at the bottom.
     */
    static final class FlyAgaric implements PackShape {
        final float[] capSize = {0.95f};
        final float[] capHeight = {0.55f};
        final float[] stemThickness = {0.22f};
        final int[] dots = {24};
        boolean ring = true;
        boolean autoColours = true;
        final int[] variant = {1};

        public String key() {
            return "fly_agaric";
        }

        public float[] defaultSize() {
            return new float[] {21, 25, 21};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = false;
            if (checkbox("fun.auto_colours", this.autoColours)) {
                this.autoColours = !this.autoColours;
                changed = true;
            }
            changed |= slider("pack.cap_height", this.capHeight, 0.2f, 0.9f);
            changed |= slider("arch2.cap_size", this.capSize, 0.5f, 1.0f);
            changed |= slider("pack.stem_width", this.stemThickness, 0.1f, 0.45f);
            changed |= slider("arch2.dots", this.dots, 0, 60);
            if (checkbox("arch2.ring", this.ring)) {
                this.ring = !this.ring;
                changed = true;
            }
            changed |= slider("crystal.variant", this.variant, 1, 50);
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            double capRadius = Math.min(hx, hz) * this.capSize[0];
            double capHeight = Math.min(capRadius * this.capHeight[0] * 1.6, hy);
            double capBase = hy - capHeight;
            double stem = capRadius * this.stemThickness[0];
            double ringY = capBase - (capBase + hy) * 0.25;
            boolean ring = this.ring;

            Random random = new Random(this.variant[0] * 104723L);
            int dotCount = this.dots[0];
            double[][] dots = new double[dotCount][];
            for (int i = 0; i < dotCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double elevation = Math.toRadians(8 + random.nextDouble() * 75);
                dots[i] = new double[] {
                    capRadius * Math.cos(elevation) * Math.cos(angle),
                    capBase + capHeight * Math.sin(elevation),
                    capRadius * Math.cos(elevation) * Math.sin(angle),
                    Math.max(1.0, capRadius * 0.12) * (0.7 + 0.6 * random.nextDouble())
                };
            }

            ShapePart part = (bx, by, bz) -> {
                double radial = Math.sqrt(bx * bx + bz * bz);
                // Cap: dome with a rim that curls down a bit
                double underside = capBase - capHeight * 0.15 * sq(radial / capRadius);
                if (by >= underside && sq(radial / capRadius) + sq((by - capBase) / capHeight) <= 1) {
                    for (double[] dot : dots) {
                        if (sq(bx - dot[0]) + sq(by - dot[1]) + sq(bz - dot[2]) <= dot[3] * dot[3]) {
                            return Part.B;
                        }
                    }
                    // Gills: the bottom layer under the cap
                    return by < underside + 1 && radial > stem ? Part.D : Part.A;
                }
                if (by >= -hy && by < capBase) {
                    double bulb = 0.6 * sq(Math.max(0, 1 - (by + hy) / (stem * 2)));
                    double stemRadius = stem * (1 + bulb) * (1 - 0.12 * (by + hy) / (capBase + hy));
                    if (radial <= stemRadius) {
                        return Part.C;
                    }
                    if (ring && by <= ringY && by >= ringY - 1.5 && radial <= stem * 1.6 + (ringY - by) * 0.4) {
                        return Part.D;
                    }
                }
                return null;
            };
            return part.definition(hx, hy, hz, this.autoColours, p -> switch (p) {
                case B -> Blocks.WHITE_CONCRETE.defaultBlockState();
                case C -> Blocks.MUSHROOM_STEM.defaultBlockState();
                case D -> Blocks.WHITE_TERRACOTTA.defaultBlockState();
                default -> Blocks.RED_CONCRETE.defaultBlockState();
            });
        }
    }

}
