package dev.philiox.axiomaddons;

import dev.philiox.axiomaddons.ExtraShapes.ShapeDefinition;
import dev.philiox.axiomaddons.ShapePack.PackShape;
import imgui.moulberry92.ImGui;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static dev.philiox.axiomaddons.ShapePack.header;
import static dev.philiox.axiomaddons.ShapePack.slider;
import static dev.philiox.axiomaddons.ShapePack.sq;
import static dev.philiox.axiomaddons.ShapePack.t;

/**
 * Snowman, Gift, Candy Cane, Ghost, Octopus, Coral, Tree Stump, Anchor and Trophy.
 *
 * Most of them work in block units (normalized coordinate times half the size), so round parts stay round
 * even if the box is not a cube. Snowman, Gift and Candy Cane can colour themselves.
 */
final class FunShapes {

    static final String[] COLOUR_KEYS = {"white", "red", "green", "blue", "yellow", "purple", "pink", "light_blue", "orange", "black"};

    private FunShapes() {
    }

    static List<PackShape> all() {
        return List.of(new Snowman(), new Gift(), new CandyCane(), new Ghost(), new Octopus(), new Coral(),
            new TreeStump(), new Anchor(), new Trophy());
    }

    // ---- Helpers ----

    static boolean checkbox(String key, boolean value) {
        return ImGui.checkbox(t(key), value);
    }

    static boolean colourCombo(String key, int[] value) {
        String[] keys = new String[COLOUR_KEYS.length];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = "philioxaxiomaddons.colour." + COLOUR_KEYS[i];
        }
        return ExtraShapes.combo("philioxaxiomaddons." + key, value, keys);
    }

    static BlockState colour(int index) {
        Block block = switch (index) {
            case 1 -> Blocks.RED_CONCRETE;
            case 2 -> Blocks.GREEN_CONCRETE;
            case 3 -> Blocks.BLUE_CONCRETE;
            case 4 -> Blocks.YELLOW_CONCRETE;
            case 5 -> Blocks.PURPLE_CONCRETE;
            case 6 -> Blocks.PINK_CONCRETE;
            case 7 -> Blocks.LIGHT_BLUE_CONCRETE;
            case 8 -> Blocks.ORANGE_CONCRETE;
            case 9 -> Blocks.BLACK_CONCRETE;
            default -> Blocks.WHITE_CONCRETE;
        };
        return block.defaultBlockState();
    }

    /** Distance from a point to a segment, and the radius interpolated along it. */
    static boolean insideSegments(double x, double y, double z, List<double[]> segments) {
        for (double[] s : segments) {
            double dx = s[3] - s[0];
            double dy = s[4] - s[1];
            double dz = s[5] - s[2];
            double lengthSq = dx * dx + dy * dy + dz * dz;
            double t = lengthSq == 0 ? 0 : ((x - s[0]) * dx + (y - s[1]) * dy + (z - s[2]) * dz) / lengthSq;
            t = Math.clamp(t, 0, 1);
            double radius = s[6] + (s[7] - s[6]) * t;
            if (sq(x - (s[0] + dx * t)) + sq(y - (s[1] + dy * t)) + sq(z - (s[2] + dz * t)) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    static double[] segment(double x1, double y1, double z1, double x2, double y2, double z2, double r1, double r2) {
        return new double[] {x1, y1, z1, x2, y2, z2, r1, r2};
    }

    // ---- Snowman ----

    static final class Snowman implements PackShape {
        boolean hat = true;
        boolean scarf = true;
        boolean arms = true;
        boolean nose = true;
        boolean autoColours = true;

        public String key() {
            return "snowman";
        }

        public float[] defaultSize() {
            return new float[] {15, 33, 15};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = false;
            if (checkbox("fun.auto_colours", this.autoColours)) {
                this.autoColours = !this.autoColours;
                changed = true;
            }
            if (checkbox("fun.hat", this.hat)) {
                this.hat = !this.hat;
                changed = true;
            }
            if (checkbox("fun.scarf", this.scarf)) {
                this.scarf = !this.scarf;
                changed = true;
            }
            if (checkbox("fun.arms", this.arms)) {
                this.arms = !this.arms;
                changed = true;
            }
            if (checkbox("fun.nose", this.nose)) {
                this.nose = !this.nose;
                changed = true;
            }
            ImGui.textWrapped(t("fun.snowman_hint"));
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            double maxRadius = Math.max(1, Math.min(hx, hz) - 0.5);
            boolean hat = this.hat;
            boolean scarf = this.scarf;
            boolean arms = this.arms;
            boolean nose = this.nose;

            // Three balls stacked with some overlap, scaled to fit the box
            double c1 = 1;
            double c2 = c1 + 1.75 * 0.85;
            double c3 = c2 + 1.3 * 0.85;
            double total = c3 + 0.55 + (hat ? 0.55 * 1.2 : 0);
            double scale = Math.min(maxRadius, (2 * hy - 0.5) / total);
            double r1 = scale;
            double r2 = 0.75 * scale;
            double r3 = 0.55 * scale;
            double y1 = -hy + c1 * scale;
            double y2 = -hy + c2 * scale;
            double y3 = -hy + c3 * scale;
            double dot = Math.max(0.6, r3 * 0.14);
            double neck = (y2 + r2 + y3 - r3) / 2;
            double hatBase = y3 + r3 * 0.75;
            double brimTop = hatBase + Math.max(1, r3 * 0.12);
            double crownTop = hatBase + r3 * 1.15;

            List<double[]> armSegments = new ArrayList<>();
            for (int side = -1; side <= 1; side += 2) {
                armSegments.add(segment(side * r2 * 0.8, y2 + r2 * 0.2, 0, side * Math.min(hx - 0.5, r2 * 1.9), y2 + r2 * 0.95, 0, 0.6, 0.5));
            }

            ShapePart part = (bx, by, bz) -> {
                // Small details first, they sit on or in the balls
                for (int side = -1; side <= 1; side += 2) {
                    if (sq(bx - side * r3 * 0.35) + sq(by - (y3 + r3 * 0.25)) + sq(bz - r3 * 0.88) <= dot * dot) {
                        return Part.COAL;
                    }
                }
                for (double offset : new double[] {0.45, 0.05, -0.35}) {
                    double buttonY = y2 + r2 * offset;
                    double buttonZ = Math.sqrt(Math.max(0, r2 * r2 - sq(r2 * offset))) * 0.95;
                    if (sq(bx) + sq(by - buttonY) + sq(bz - buttonZ) <= dot * dot) {
                        return Part.COAL;
                    }
                }
                if (nose) {
                    double along = (bz - r3 * 0.8) / (r3 * 0.75);
                    if (along >= 0 && along <= 1 && sq(bx) + sq(by - y3) <= sq(r3 * 0.16 * (1 - along) + 0.35)) {
                        return Part.CARROT;
                    }
                }
                if (scarf) {
                    double radial = Math.sqrt(bx * bx + bz * bz);
                    if (sq(radial - r3 * 0.85) + sq(by - neck) <= sq(Math.max(0.8, r3 * 0.22))) {
                        return Part.SCARF;
                    }
                    double fromMiddle = Math.sqrt(bx * bx + sq(by - y2) + bz * bz);
                    if (Math.abs(bx - r3 * 0.45) <= Math.max(0.6, r3 * 0.16) && by <= neck && by >= neck - r3 * 1.1
                        && bz > 0 && fromMiddle >= r2 - 0.2 && fromMiddle <= r2 + 1.1) {
                        return Part.SCARF;
                    }
                }
                if (hat) {
                    double radial = Math.sqrt(bx * bx + bz * bz);
                    if (by >= hatBase && by <= brimTop && radial <= r3 * 1.05) {
                        return Part.HAT;
                    }
                    if (by >= hatBase && by <= crownTop && radial <= r3 * 0.62) {
                        return by <= brimTop + (crownTop - brimTop) * 0.25 ? Part.HAT_BAND : Part.HAT;
                    }
                }
                if (sq(bx) + sq(by - y1) + sq(bz) <= r1 * r1
                    || sq(bx) + sq(by - y2) + sq(bz) <= r2 * r2
                    || sq(bx) + sq(by - y3) + sq(bz) <= r3 * r3) {
                    return Part.SNOW;
                }
                if (arms && insideSegments(bx, by, bz, armSegments)) {
                    return Part.STICK;
                }
                return null;
            };
            return part.definition(hx, hy, hz, this.autoColours, p -> switch (p) {
                case COAL -> Blocks.COAL_BLOCK.defaultBlockState();
                case CARROT -> Blocks.ORANGE_CONCRETE.defaultBlockState();
                case SCARF, HAT_BAND -> Blocks.RED_WOOL.defaultBlockState();
                case HAT -> Blocks.BLACK_WOOL.defaultBlockState();
                case STICK -> Blocks.DARK_OAK_WOOD.defaultBlockState();
                default -> Blocks.SNOW_BLOCK.defaultBlockState();
            });
        }
    }

    enum Part {
        SNOW, COAL, CARROT, SCARF, HAT, HAT_BAND, STICK, BOX, RIBBON, STRIPE_A, STRIPE_B,
        // General parts for shapes with several materials
        A, B, C, D, E
    }

    /** Shape described in block units that tells which part a point belongs to, null for outside. */
    @FunctionalInterface
    interface ShapePart {
        Part at(double bx, double by, double bz);

        default ShapeDefinition definition(double hx, double hy, double hz, boolean colours,
                                           java.util.function.Function<Part, BlockState> palette) {
            ShapeDefinition definition = ShapeDefinition.simple((x, y, z) ->
                ShapePack.inBox(x, y, z) && this.at(x * hx, y * hy, z * hz) != null);
            if (!colours) {
                return definition;
            }
            return definition.withMaterial((x, y, z) -> {
                Part part = this.at(x * hx, y * hy, z * hz);
                return part == null ? null : palette.apply(part);
            });
        }
    }

    // ---- Gift ----

    static final class Gift implements PackShape {
        final int[] boxColour = {1};
        final int[] ribbonColour = {4};
        final float[] ribbonWidth = {0.15f};
        final float[] bow = {0.7f};
        boolean lid = true;
        boolean autoColours = true;

        public String key() {
            return "gift";
        }

        public float[] defaultSize() {
            return new float[] {15, 17, 15};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = false;
            if (checkbox("fun.auto_colours", this.autoColours)) {
                this.autoColours = !this.autoColours;
                changed = true;
            }
            if (this.autoColours) {
                changed |= colourCombo("fun.box_colour", this.boxColour);
                changed |= colourCombo("fun.ribbon_colour", this.ribbonColour);
            }
            changed |= slider("fun.ribbon_width", this.ribbonWidth, 0.05f, 0.4f);
            changed |= slider("fun.bow", this.bow, 0.0f, 1.0f);
            if (checkbox("fun.lid", this.lid)) {
                this.lid = !this.lid;
                changed = true;
            }
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            double bow = this.bow[0];
            double ribbon = this.ribbonWidth[0];
            boolean lid = this.lid;

            double bowHeight = bow * Math.min(hx, hz) * 0.55;
            double boxTop = hy - bowHeight;
            double lidHeight = lid ? Math.max(1, hy * 0.18) : 0;
            double inset = lid ? 0.9 : 1;
            double loopRadius = Math.max(1, bowHeight * 0.55);
            double loopTube = Math.max(0.7, bowHeight * 0.22);

            ShapePart part = (bx, by, bz) -> {
                double nx = bx / hx;
                double nz = bz / hz;
                boolean inLid = lid && by >= boxTop - lidHeight && by <= boxTop && Math.abs(nx) <= 1 && Math.abs(nz) <= 1;
                boolean inBody = by >= -hy && by < boxTop - lidHeight && Math.abs(nx) <= inset && Math.abs(nz) <= inset;
                if (inLid || inBody) {
                    return Math.abs(nx) <= ribbon || Math.abs(nz) <= ribbon ? Part.RIBBON : Part.BOX;
                }
                if (bow > 0) {
                    // Knot in the middle and two loops leaning outwards
                    if (sq(bx) + sq(by - boxTop - loopTube * 0.6) + sq(bz) <= sq(loopTube * 1.3)) {
                        return Part.RIBBON;
                    }
                    for (int side = -1; side <= 1; side += 2) {
                        double cx = side * loopRadius * 1.1;
                        double cy = boxTop + loopRadius * 0.85;
                        double inPlane = Math.sqrt(sq(bx - cx) + sq(by - cy));
                        if (by >= boxTop && sq(inPlane - loopRadius) + sq(bz) <= loopTube * loopTube) {
                            return Part.RIBBON;
                        }
                    }
                }
                return null;
            };
            int box = this.boxColour[0];
            int band = this.ribbonColour[0];
            return part.definition(hx, hy, hz, this.autoColours, p -> colour(p == Part.RIBBON ? band : box));
        }
    }

    // ---- Candy cane ----

    static final class CandyCane implements PackShape {
        final int[] colourA = {1};
        final int[] colourB = {0};
        final float[] thickness = {3.0f};
        final float[] stripeWidth = {2.0f};
        final float[] twist = {1.0f};
        final float[] hookLength = {0.35f};
        boolean autoColours = true;

        public String key() {
            return "candy_cane";
        }

        public float[] defaultSize() {
            return new float[] {15, 41, 7};
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
                changed |= slider("fun.stripe_width", this.stripeWidth, 1.0f, 8.0f);
                changed |= slider("fun.twist", this.twist, 0.0f, 3.0f);
            }
            changed |= slider("fun.cane_thickness", this.thickness, 1.0f, 8.0f);
            changed |= slider("fun.hook_length", this.hookLength, 0.0f, 1.0f);
            ImGui.textWrapped(t("fun.cane_hint"));
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            double tube = Math.max(0.8, Math.min(this.thickness[0], Math.min(hz, hx / 2)));
            double hookRadius = Math.max(tube, hx - tube);
            double centerY = hy - tube - hookRadius;
            double shaftBottom = -hy;
            double shaftLength = centerY - shaftBottom;
            // The hook goes from the left over the top to the right and then this far down
            double endAngle = -Math.PI / 2 * this.hookLength[0];
            double stripe = Math.max(0.5, this.stripeWidth[0]);
            double twist = this.twist[0];

            double tipX = hookRadius * Math.cos(endAngle);
            double tipY = centerY + hookRadius * Math.sin(endAngle);

            ShapePart part = (bx, by, bz) -> {
                double along;
                double around;
                if (by <= centerY && by >= shaftBottom && sq(bx + hookRadius) + bz * bz <= tube * tube) {
                    // Straight shaft on the left
                    along = by - shaftBottom;
                    around = Math.atan2(bz, bx + hookRadius);
                } else {
                    // Hook: arc from the left (angle pi) over the top down to endAngle, plus a round tip
                    double radial = Math.sqrt(bx * bx + sq(by - centerY)) - hookRadius;
                    double angle = Math.atan2(by - centerY, bx);
                    boolean inArc = angle >= endAngle && radial * radial + bz * bz <= tube * tube;
                    boolean inTip = sq(bx - tipX) + sq(by - tipY) + bz * bz <= tube * tube;
                    if (!inArc && !inTip) {
                        return null;
                    }
                    along = shaftLength + hookRadius * (Math.PI - Math.max(angle, endAngle));
                    around = Math.atan2(bz, radial);
                }
                // Spiral stripes: position along the cane shifted by the angle around it
                double phase = (along + twist * stripe * around / Math.PI) / stripe;
                return Math.floorMod((int) Math.floor(phase), 2) == 0 ? Part.STRIPE_A : Part.STRIPE_B;
            };
            int a = this.colourA[0];
            int b = this.colourB[0];
            return part.definition(hx, hy, hz, this.autoColours, p -> colour(p == Part.STRIPE_A ? a : b));
        }
    }

    // ---- Ghost ----

    static final class Ghost implements PackShape {
        final int[] waves = {6};
        final float[] waveDepth = {0.15f};
        final float[] flare = {0.3f};
        final float[] lean = {0.3f};
        boolean face = true;
        boolean arms = true;

        public String key() {
            return "ghost";
        }

        public float[] defaultSize() {
            return new float[] {21, 27, 21};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("fun.hem_waves", this.waves, 3, 14);
            changed |= slider("fun.hem_depth", this.waveDepth, 0.0f, 0.4f);
            changed |= slider("fun.flare", this.flare, 0.0f, 0.6f);
            changed |= slider("fun.lean", this.lean, 0.0f, 1.0f);
            if (checkbox("fun.face", this.face)) {
                this.face = !this.face;
                changed = true;
            }
            if (checkbox("fun.arms", this.arms)) {
                this.arms = !this.arms;
                changed = true;
            }
            ImGui.textWrapped(t("fun.ghost_hint"));
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            double flare = this.flare[0];
            double headRadius = Math.min(hx, hz) / (1 + flare) * 0.85;
            double headY = hy - headRadius;
            int waves = Math.max(1, this.waves[0]);
            double depth = this.waveDepth[0] * 2 * hy;
            double lean = this.lean[0];
            boolean face = this.face;
            boolean arms = this.arms;
            double height = headY + hy;

            ShapePart part = (bx, by, bz) -> {
                // The sheet trails behind (towards -X) the lower it gets
                double drop = Math.clamp((headY - by) / height, 0, 1);
                double sx = bx + lean * headRadius * 0.6 * drop;
                double angle = Math.atan2(bz, sx);
                double hem = -hy + depth * (0.5 + 0.5 * Math.sin(waves * angle));
                if (by < hem) {
                    return null;
                }
                boolean inside;
                if (by >= headY) {
                    inside = sq(sx) + sq(by - headY) + sq(bz) <= headRadius * headRadius;
                } else {
                    double radius = headRadius * (1 + flare * drop);
                    inside = sx * sx + bz * bz <= radius * radius;
                }
                if (!inside && arms) {
                    // Stubby arms stretched forward
                    for (int side = -1; side <= 1; side += 2) {
                        double armY = headY - headRadius * 0.9;
                        if (sq(sx - side * headRadius * 0.95) + sq(by - armY) * 2 + sq(bz - headRadius * 0.35) <= sq(headRadius * 0.28)) {
                            inside = true;
                        }
                    }
                }
                if (!inside) {
                    return null;
                }
                if (face && bz > headRadius * 0.45) {
                    for (int side = -1; side <= 1; side += 2) {
                        if (sq((sx - side * headRadius * 0.35) / (headRadius * 0.15)) + sq((by - headY - headRadius * 0.1) / (headRadius * 0.24)) <= 1) {
                            return null;
                        }
                    }
                    if (sq(sx / (headRadius * 0.17)) + sq((by - headY + headRadius * 0.4) / (headRadius * 0.2)) <= 1) {
                        return null;
                    }
                }
                return Part.SNOW;
            };
            return part.definition(hx, hy, hz, false, p -> null);
        }
    }

    // ---- Octopus ----

    static final class Octopus implements PackShape {
        final float[] length = {0.9f};
        final float[] curl = {0.6f};
        final float[] wiggle = {0.5f};
        final float[] thickness = {2.5f};
        boolean eyes = true;

        public String key() {
            return "octopus";
        }

        public float[] defaultSize() {
            return new float[] {41, 25, 41};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("fun.tentacle_length", this.length, 0.3f, 1.0f);
            changed |= slider("fun.curl", this.curl, 0.0f, 1.0f);
            changed |= slider("fun.wiggle", this.wiggle, 0.0f, 1.0f);
            changed |= slider("fun.tentacle_thickness", this.thickness, 1.0f, 6.0f);
            if (checkbox("fun.eyes", this.eyes)) {
                this.eyes = !this.eyes;
                changed = true;
            }
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            double reach = Math.min(hx, hz);
            double mantleRadius = reach * 0.3;
            double mantleHeight = Math.min(hy * 0.55, hy * 2 * 0.3);
            double mantleY = hy - mantleHeight;
            double thick = this.thickness[0];
            boolean eyes = this.eyes;
            double eyeRadius = Math.max(0.8, mantleRadius * 0.25);

            List<double[]> segments = new ArrayList<>();
            double startY = mantleY - mantleHeight * 0.7;
            double floor = -hy + thick;
            double length = (reach - thick) * this.length[0];
            int samples = 40;
            for (int k = 0; k < 8; k++) {
                double baseAngle = k * Math.PI / 4 + Math.PI / 8;
                double previousX = 0;
                double previousY = 0;
                double previousZ = 0;
                double previousRadius = 0;
                for (int i = 0; i <= samples; i++) {
                    double s = i / (double) samples;
                    double radial = mantleRadius * 0.55 + s * (length - mantleRadius * 0.55);
                    double curling = Math.max(0, (s - 0.6) / 0.4);
                    radial -= this.curl[0] * curling * curling * length * 0.25;
                    double angle = baseAngle + this.wiggle[0] * 0.35 * Math.sin(s * Math.PI * 3 + k);
                    double y = startY - (startY - floor) * Math.sin(Math.min(1, s * 1.4) * Math.PI / 2)
                        + this.curl[0] * curling * curling * hy * 0.8;
                    double x = Math.cos(angle) * radial;
                    double z = Math.sin(angle) * radial;
                    double radius = thick * (1 - 0.8 * s) + 0.5;
                    if (i > 0) {
                        segments.add(segment(previousX, previousY, previousZ, x, y, z, previousRadius, radius));
                    }
                    previousX = x;
                    previousY = y;
                    previousZ = z;
                    previousRadius = radius;
                }
            }

            ShapePart part = (bx, by, bz) -> {
                if (sq(bx / mantleRadius) + sq((by - mantleY) / mantleHeight) + sq(bz / mantleRadius) <= 1) {
                    return Part.SNOW;
                }
                if (eyes) {
                    for (int side = -1; side <= 1; side += 2) {
                        if (sq(bx - side * mantleRadius * 0.75) + sq(by - (mantleY - mantleHeight * 0.35)) + sq(bz - mantleRadius * 0.55) <= eyeRadius * eyeRadius) {
                            return Part.SNOW;
                        }
                    }
                }
                return insideSegments(bx, by, bz, segments) ? Part.SNOW : null;
            };
            return part.definition(hx, hy, hz, false, p -> null);
        }
    }

    // ---- Coral ----

    static final class Coral implements PackShape {
        private static final int TYPE_BRANCHING = 0;
        private static final int TYPE_FAN = 1;
        private static final int TYPE_BRAIN = 2;

        final int[] type = {TYPE_BRANCHING};
        final int[] depth = {4};
        final float[] thickness = {1.5f};
        final float[] spread = {0.6f};
        final int[] variant = {1};

        public String key() {
            return "coral";
        }

        public float[] defaultSize() {
            return new float[] {21, 25, 21};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = ExtraShapes.combo("philioxaxiomaddons.fun.coral_type", this.type,
                "philioxaxiomaddons.fun.coral_type.branching", "philioxaxiomaddons.fun.coral_type.fan",
                "philioxaxiomaddons.fun.coral_type.brain");
            if (this.type[0] != TYPE_BRAIN) {
                changed |= slider("fun.coral_depth", this.depth, 2, 6);
                changed |= slider("fun.coral_spread", this.spread, 0.2f, 1.0f);
            }
            changed |= slider("fun.coral_thickness", this.thickness, 0.6f, 4.0f);
            changed |= slider("crystal.variant", this.variant, 1, 50);
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            long seed = this.variant[0] * 48271L;
            double thick = this.thickness[0];

            if (this.type[0] == TYPE_BRAIN) {
                double radius = Math.min(Math.min(hx, hz), hy * 2) - 0.5;
                double groove = Math.max(1, thick);
                ShapePart part = (bx, by, bz) -> {
                    double distance = Math.sqrt(bx * bx + sq(by + hy) + bz * bz);
                    if (distance > radius || by < -hy) {
                        return null;
                    }
                    // Winding grooves near the surface, like a brain coral
                    double n = ShapePack.noise(bx / 3, by / 3, bz / 3, seed);
                    return distance > radius - groove * 1.5 && Math.abs(n) < 0.12 ? null : Part.SNOW;
                };
                return part.definition(hx, hy, hz, false, p -> null);
            }

            boolean fan = this.type[0] == TYPE_FAN;
            List<double[]> segments = new ArrayList<>();
            Random random = new Random(seed);
            branch(segments, random, 0, -hy, 0, 0, 1, 0, hy * 0.55, thick, this.depth[0], this.spread[0], fan);

            ShapePart part = (bx, by, bz) -> insideSegments(bx, by, bz, segments) ? Part.SNOW : null;
            return part.definition(hx, hy, hz, false, p -> null);
        }

        private static void branch(List<double[]> segments, Random random, double x, double y, double z,
                                   double dx, double dy, double dz, double length, double radius, int depth,
                                   double spread, boolean fan) {
            double ex = x + dx * length;
            double ey = y + dy * length;
            double ez = z + dz * length;
            segments.add(segment(x, y, z, ex, ey, ez, radius, radius * 0.8));
            if (depth <= 1) {
                return;
            }
            int children = 2 + (random.nextDouble() < 0.4 ? 1 : 0);
            for (int i = 0; i < children; i++) {
                double ndx = dx + (random.nextDouble() - 0.5) * 2 * spread;
                double ndy = dy + 0.3 + random.nextDouble() * 0.4;
                double ndz = fan ? 0 : dz + (random.nextDouble() - 0.5) * 2 * spread;
                double norm = Math.sqrt(ndx * ndx + ndy * ndy + ndz * ndz);
                branch(segments, random, ex, ey, ez, ndx / norm, ndy / norm, ndz / norm,
                    length * (0.6 + random.nextDouble() * 0.2), Math.max(0.5, radius * 0.75), depth - 1, spread, fan);
            }
        }
    }

    // ---- Tree stump ----

    static final class TreeStump implements PackShape {
        final int[] roots = {5};
        final float[] rootLength = {0.8f};
        final float[] jagged = {0.4f};
        boolean rings = true;
        final int[] variant = {1};

        public String key() {
            return "tree_stump";
        }

        public float[] defaultSize() {
            return new float[] {25, 13, 25};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("fun.roots", this.roots, 0, 9);
            changed |= slider("fun.root_length", this.rootLength, 0.2f, 1.0f);
            changed |= slider("fun.jagged", this.jagged, 0.0f, 1.0f);
            if (checkbox("fun.rings", this.rings)) {
                this.rings = !this.rings;
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
            double trunk = reach * 0.45;
            long seed = this.variant[0] * 69069L;
            double jagged = this.jagged[0];
            boolean rings = this.rings;
            double top = hy - 0.5 - jagged * hy * 0.5;

            List<double[]> roots = new ArrayList<>();
            Random random = new Random(seed);
            int count = this.roots[0];
            for (int i = 0; i < count; i++) {
                double angle = i * 2 * Math.PI / Math.max(1, count) + (random.nextDouble() - 0.5) * 0.6;
                double end = trunk + (reach - trunk - 0.5) * this.rootLength[0];
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                roots.add(segment(cos * trunk * 0.6, -hy + trunk * 0.5, sin * trunk * 0.6,
                    cos * end, -hy + 0.5, sin * end, trunk * 0.35, 0.6));
            }

            ShapePart part = (bx, by, bz) -> {
                if (by < -hy) {
                    return null;
                }
                double radial = Math.sqrt(bx * bx + bz * bz);
                double angle = Math.atan2(bz, bx);
                double bark = ShapePack.noise(Math.cos(angle) * 2, by / 3, Math.sin(angle) * 2, seed) * 0.08;
                double flare = trunk * 0.35 * sq(Math.max(0, 1 - (by + hy) / (trunk * 0.8)));
                // Broken, jagged top edge
                double topHere = top + jagged * hy * 0.5 * (0.5 + 0.5 * ShapePack.noise(bx / 2.5, 0, bz / 2.5, seed + 3));
                if (radial <= trunk * (1 + bark) + flare && by <= topHere) {
                    if (rings && by > topHere - 1 && radial < trunk * 0.9 && Math.floorMod((int) Math.floor(radial / 1.5), 2) == 1) {
                        return null;
                    }
                    return Part.SNOW;
                }
                return insideSegments(bx, by, bz, roots) ? Part.SNOW : null;
            };
            return part.definition(hx, hy, hz, false, p -> null);
        }
    }

    // ---- Anchor ----

    static final class Anchor implements PackShape {
        final float[] width = {2.0f};
        final float[] armSpread = {0.75f};
        boolean flukes = true;

        public String key() {
            return "anchor";
        }

        public float[] defaultSize() {
            return new float[] {21, 31, 3};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("fun.anchor_width", this.width, 1.0f, 5.0f);
            changed |= slider("fun.arm_spread", this.armSpread, 0.4f, 1.0f);
            if (checkbox("fun.flukes", this.flukes)) {
                this.flukes = !this.flukes;
                changed = true;
            }
            ImGui.textWrapped(t("fun.flat_hint"));
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            double w = this.width[0] / 2;
            double ringRadius = hx * 0.22;
            double ringY = hy - ringRadius - w;
            double stockY = ringY - ringRadius - w * 2;
            double stockHalf = hx * 0.55;
            double armRadius = (hx - w) * this.armSpread[0];
            double armY = -hy + armRadius + w;
            double tipAngle = Math.toRadians(200);
            boolean flukes = this.flukes;

            ShapePart part = (bx, by, bz) -> {
                if (Math.abs(bz) > hz) {
                    return null;
                }
                if (Math.abs(Math.sqrt(bx * bx + sq(by - ringY)) - ringRadius) <= w) {
                    return Part.SNOW;
                }
                if (Math.abs(by - stockY) <= w && Math.abs(bx) <= stockHalf) {
                    return Part.SNOW;
                }
                for (int side = -1; side <= 1; side += 2) {
                    if (sq(bx - side * stockHalf) + sq(by - stockY) <= sq(w * 1.6)) {
                        return Part.SNOW;
                    }
                }
                if (Math.abs(bx) <= w && by >= armY - armRadius && by <= ringY - ringRadius + 0.5) {
                    return Part.SNOW;
                }
                // Arms: lower part of a circle
                double angle = Math.atan2(by - armY, bx);
                if (angle < 0) {
                    angle += 2 * Math.PI;
                }
                if (angle >= tipAngle && angle <= 2 * Math.PI * 340 / 360
                    && Math.abs(Math.sqrt(bx * bx + sq(by - armY)) - armRadius) <= w) {
                    return Part.SNOW;
                }
                if (flukes) {
                    // Arrow shaped flukes at the arm tips, pointing up and outwards
                    for (int side = -1; side <= 1; side += 2) {
                        double tipX = side * armRadius * Math.cos(Math.toRadians(20));
                        double tipY = armY - armRadius * Math.sin(Math.toRadians(20));
                        double size = Math.max(2, armRadius * 0.35);
                        if (triangle(bx, by, tipX + side * size * 0.6, tipY + size,
                            tipX + side * size * 0.6, tipY - size * 0.3, tipX - side * size * 0.5, tipY - size * 0.1)) {
                            return Part.SNOW;
                        }
                    }
                }
                return null;
            };
            return part.definition(hx, hy, hz, false, p -> null);
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

    // ---- Trophy ----

    static final class Trophy implements PackShape {
        boolean handles = true;
        boolean hollowCup = true;
        final float[] cupWidth = {1.0f};

        public String key() {
            return "trophy";
        }

        public float[] defaultSize() {
            return new float[] {25, 29, 17};
        }

        public boolean displayOptions() {
            header(this.key());
            boolean changed = slider("fun.cup_width", this.cupWidth, 0.6f, 1.0f);
            if (checkbox("fun.handles", this.handles)) {
                this.handles = !this.handles;
                changed = true;
            }
            if (checkbox("fun.hollow_cup", this.hollowCup)) {
                this.hollowCup = !this.hollowCup;
                changed = true;
            }
            return changed;
        }

        public ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
            double hx = sizeX / 2;
            double hy = sizeY / 2;
            double hz = sizeZ / 2;
            double cup = Math.min(hz, hx * 0.65) * this.cupWidth[0];
            boolean handles = this.handles;
            boolean hollow = this.hollowCup;
            double handleRadius = cup * 0.32;
            double handleTube = Math.max(0.7, cup * 0.09);
            double handleY = -hy + 2 * hy * 0.72;

            ShapePart part = (bx, by, bz) -> {
                if (by < -hy || by > hy) {
                    return null;
                }
                double h = (by + hy) / (2 * hy);
                double radial = Math.sqrt(bx * bx + bz * bz);
                double radius;
                if (h < 0.06) {
                    radius = cup * 0.75;
                } else if (h < 0.1) {
                    radius = cup * 0.55;
                } else if (h < 0.16) {
                    radius = cup * (0.55 - 0.4 * (h - 0.1) / 0.06);
                } else if (h < 0.42) {
                    radius = h > 0.27 && h < 0.33 ? cup * 0.28 : cup * 0.15;
                } else if (h < 0.48) {
                    radius = cup * (0.15 + 0.45 * (h - 0.42) / 0.06);
                } else if (h < 0.97) {
                    radius = cup * (0.6 + 0.4 * Math.sin(Math.PI / 2 * (h - 0.48) / 0.49));
                } else {
                    radius = cup * 1.02;
                }
                radius = Math.max(radius, 0.6);
                if (radial <= radius) {
                    // Open cup with one block thick walls
                    if (hollow && h > 0.52 && radial < radius - 1) {
                        return null;
                    }
                    return Part.SNOW;
                }
                if (handles) {
                    for (int side = -1; side <= 1; side += 2) {
                        double cx = side * cup * 0.95;
                        double inPlane = Math.sqrt(sq(bx - cx) + sq(by - handleY));
                        if (bx * side > cup * 0.8 && sq(inPlane - handleRadius) + bz * bz <= handleTube * handleTube) {
                            return Part.SNOW;
                        }
                    }
                }
                return null;
            };
            return part.definition(hx, hy, hz, false, p -> null);
        }
    }

}
