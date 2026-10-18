package dev.philiox.axiomaddons;

import com.moulberry.axiom.editor.ImGuiHelper;
import dev.philiox.axiomaddons.ExtraShapes.ShapeDefinition;
import dev.philiox.axiomaddons.ExtraShapes.ShapeTest;
import imgui.moulberry92.ImGui;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Tornado, Skull, Heart, Crescent, Crystal, Onion Dome and Helix. Same conventions as {@link ExtraShapes}:
 * tests get normalized coordinates where the bounding box spans -1 to 1 on every axis.
 */
final class MoreShapes {

    private static final int STYLE_FLAT = 0;
    private static final int STYLE_3D = 1;
    private static final int ORIENTATION_STANDING = 0;
    private static final int ORIENTATION_LYING = 1;

    private static final float[] tornadoBottom = {0.1f};
    private static final float[] tornadoCurve = {2.0f};
    private static final float[] tornadoSway = {0.3f};
    private static final float[] tornadoTwists = {1.0f};
    private static boolean tornadoSwirl = false;
    private static final int[] tornadoArms = {3};
    private static final float[] tornadoGap = {0.35f};

    private static final int TEETH_UPPER = 0;
    private static final int TEETH_BOTH = 1;

    private static final float[] skullEyeSize = {0.27f};
    private static final float[] skullEyeSpacing = {0.36f};
    private static final float[] skullEyeDepth = {0.5f};
    private static final float[] skullNoseSize = {1.0f};
    private static final float[] skullJawWidth = {0.6f};
    private static final float[] skullMouthOpen = {0.0f};
    private static boolean skullTeeth = true;
    private static final int[] skullTeethRows = {TEETH_BOTH};
    private static final int[] skullTeethCount = {6};
    private static final float[] skullTeethWidth = {0.5f};
    private static final float[] skullToothLength = {0.12f};
    private static final float[] skullToothGap = {0.3f};
    private static boolean skullPointedTeeth = false;
    private static final float[] skullMissingTeeth = {0.0f};
    private static final int[] skullDents = {0};
    private static final float[] skullDentSize = {0.18f};
    private static final float[] skullDentDepth = {0.5f};
    private static final int[] skullSeed = {1};

    private static final int[] heartStyle = {STYLE_3D};
    private static final float[] heartThickness = {0.5f};
    private static final int[] heartOrientation = {ORIENTATION_STANDING};

    private static final float[] crescentOffset = {0.45f};
    private static final float[] crescentInner = {0.85f};
    private static final int[] crescentStyle = {STYLE_3D};
    private static final float[] crescentThickness = {0.4f};
    private static final int[] crescentOrientation = {ORIENTATION_STANDING};

    private static final int[] crystalCount = {5};
    private static final float[] crystalTip = {0.3f};
    private static final int[] crystalVariant = {1};

    private static final float[] onionNeck = {0.55f};
    private static final float[] onionBulge = {0.35f};
    private static final float[] onionTip = {0.6f};

    private static final float[] helixTurns = {2.0f};
    private static final float[] helixInner = {0.3f};
    private static final int[] helixThickness = {1};
    private static boolean helixPillar = true;

    private MoreShapes() {
    }

    static boolean displayOptions(int shape) {
        return switch (shape) {
            case ExtraShapes.TORNADO -> displayTornadoOptions();
            case ExtraShapes.SKULL -> displaySkullOptions();
            case ExtraShapes.HEART -> displayFlatOr3dOptions("heart", heartStyle, heartThickness, heartOrientation);
            case ExtraShapes.CRESCENT -> displayCrescentOptions();
            case ExtraShapes.CRYSTAL -> displayCrystalOptions();
            case ExtraShapes.ONION_DOME -> displayOnionOptions();
            case ExtraShapes.HELIX -> displayHelixOptions();
            default -> false;
        };
    }

    static @Nullable ShapeDefinition createDefinition(int shape, float sizeX, float sizeY, float sizeZ) {
        return switch (shape) {
            case ExtraShapes.TORNADO -> ShapeDefinition.simple(createTornadoTest());
            case ExtraShapes.SKULL -> ShapeDefinition.simple(createSkullTest(sizeX, sizeY));
            case ExtraShapes.HEART -> ShapeDefinition.simple(createHeartTest());
            case ExtraShapes.CRESCENT -> ShapeDefinition.simple(createCrescentTest());
            case ExtraShapes.CRYSTAL -> ShapeDefinition.simple(createCrystalTest());
            case ExtraShapes.ONION_DOME -> ShapeDefinition.simple(createOnionTest());
            case ExtraShapes.HELIX -> ShapeDefinition.simple(createHelixTest(sizeY));
            default -> null;
        };
    }

    // ---- Options ----

    private static String t(String key) {
        return I18n.get("philioxaxiomaddons." + key);
    }

    private static boolean checkbox(String key, boolean value, Runnable toggle) {
        if (ImGui.checkbox(t(key), value)) {
            toggle.run();
            return true;
        }
        return false;
    }

    private static boolean displayTornadoOptions() {
        ImGuiHelper.separatorWithText(t("shape.tornado"));
        boolean changed = ImGui.sliderFloat(t("tornado.bottom"), tornadoBottom, 0.02f, 0.6f, "%.2f");
        changed |= ImGui.sliderFloat(t("tornado.curve"), tornadoCurve, 0.5f, 4.0f, "%.2f");
        changed |= ImGui.sliderFloat(t("tornado.sway"), tornadoSway, 0.0f, 0.9f, "%.2f");
        changed |= ImGui.sliderFloat(t("tornado.twists"), tornadoTwists, 0.0f, 4.0f, "%.2f");
        changed |= checkbox("tornado.swirl", tornadoSwirl, () -> tornadoSwirl = !tornadoSwirl);
        if (tornadoSwirl) {
            changed |= ImGui.sliderInt(t("tornado.arms"), tornadoArms, 1, 8);
            changed |= ImGui.sliderFloat(t("tornado.gap"), tornadoGap, 0.05f, 0.9f, "%.2f");
        }
        ImGui.textWrapped(t("tornado.hint"));
        return changed;
    }

    private static boolean displaySkullOptions() {
        ImGuiHelper.separatorWithText(t("shape.skull"));
        boolean changed = ImGui.sliderFloat(t("skull.eye_size"), skullEyeSize, 0.12f, 0.4f, "%.2f");
        changed |= ImGui.sliderFloat(t("skull.eye_spacing"), skullEyeSpacing, 0.2f, 0.55f, "%.2f");
        changed |= ImGui.sliderFloat(t("skull.eye_depth"), skullEyeDepth, 0.1f, 1.0f, "%.2f");
        changed |= ImGui.sliderFloat(t("skull.nose_size"), skullNoseSize, 0.0f, 2.0f, "%.2f");
        changed |= ImGui.sliderFloat(t("skull.jaw_width"), skullJawWidth, 0.35f, 0.85f, "%.2f");
        changed |= ImGui.sliderFloat(t("skull.mouth_open"), skullMouthOpen, 0.0f, 0.3f, "%.2f");

        ImGuiHelper.separatorWithText(t("skull.teeth"));
        changed |= checkbox("skull.teeth", skullTeeth, () -> skullTeeth = !skullTeeth);
        if (skullTeeth) {
            changed |= ExtraShapes.combo("philioxaxiomaddons.skull.teeth_rows", skullTeethRows,
                "philioxaxiomaddons.skull.teeth_rows.upper", "philioxaxiomaddons.skull.teeth_rows.both");
            changed |= ImGui.sliderInt(t("skull.teeth_count"), skullTeethCount, 2, 16);
            changed |= ImGui.sliderFloat(t("skull.teeth_width"), skullTeethWidth, 0.2f, 0.8f, "%.2f");
            changed |= ImGui.sliderFloat(t("skull.tooth_length"), skullToothLength, 0.04f, 0.3f, "%.2f");
            changed |= ImGui.sliderFloat(t("skull.tooth_gap"), skullToothGap, 0.0f, 0.8f, "%.2f");
            changed |= checkbox("skull.pointed_teeth", skullPointedTeeth, () -> skullPointedTeeth = !skullPointedTeeth);
            changed |= ImGui.sliderFloat(t("skull.missing_teeth"), skullMissingTeeth, 0.0f, 0.9f, "%.2f");
        }

        ImGuiHelper.separatorWithText(t("skull.dents"));
        changed |= ImGui.sliderInt(t("skull.dent_count"), skullDents, 0, 24);
        if (skullDents[0] > 0) {
            changed |= ImGui.sliderFloat(t("skull.dent_size"), skullDentSize, 0.05f, 0.45f, "%.2f");
            changed |= ImGui.sliderFloat(t("skull.dent_depth"), skullDentDepth, 0.05f, 1.0f, "%.2f");
        }
        changed |= ImGui.sliderInt(t("skull.seed"), skullSeed, 1, 100);
        ImGui.textWrapped(t("skull.hint"));
        return changed;
    }

    private static boolean displayFlatOr3dOptions(String name, int[] style, float[] thickness, int[] orientation) {
        ImGuiHelper.separatorWithText(t("shape." + name));
        boolean changed = ExtraShapes.combo("philioxaxiomaddons.star.style", style,
            "philioxaxiomaddons.star.style.flat", "philioxaxiomaddons.star.style.3d");
        changed |= ImGui.sliderFloat(t("star.thickness"), thickness, 0.05f, 1.0f, "%.2f");
        changed |= ExtraShapes.combo("philioxaxiomaddons.star.orientation", orientation,
            "philioxaxiomaddons.star.orientation.standing", "philioxaxiomaddons.star.orientation.lying");
        return changed;
    }

    private static boolean displayCrescentOptions() {
        boolean changed = displayFlatOr3dOptions("crescent", crescentStyle, crescentThickness, crescentOrientation);
        changed |= ImGui.sliderFloat(t("crescent.offset"), crescentOffset, 0.1f, 1.2f, "%.2f");
        changed |= ImGui.sliderFloat(t("crescent.inner"), crescentInner, 0.3f, 1.0f, "%.2f");
        return changed;
    }

    private static boolean displayCrystalOptions() {
        ImGuiHelper.separatorWithText(t("shape.crystal"));
        boolean changed = ImGui.sliderInt(t("crystal.count"), crystalCount, 1, 9);
        changed |= ImGui.sliderFloat(t("crystal.tip"), crystalTip, 0.05f, 0.7f, "%.2f");
        changed |= ImGui.sliderInt(t("crystal.variant"), crystalVariant, 1, 50);
        return changed;
    }

    private static boolean displayOnionOptions() {
        ImGuiHelper.separatorWithText(t("shape.onion_dome"));
        boolean changed = ImGui.sliderFloat(t("onion.neck"), onionNeck, 0.0f, 0.95f, "%.2f");
        changed |= ImGui.sliderFloat(t("onion.bulge"), onionBulge, 0.1f, 0.7f, "%.2f");
        changed |= ImGui.sliderFloat(t("onion.tip"), onionTip, 0.0f, 1.5f, "%.2f");
        return changed;
    }

    private static boolean displayHelixOptions() {
        ImGuiHelper.separatorWithText(t("shape.helix"));
        boolean changed = ImGui.sliderFloat(t("helix.turns"), helixTurns, 0.25f, 10.0f, "%.2f");
        changed |= ImGui.sliderFloat(t("helix.inner"), helixInner, 0.0f, 0.85f, "%.2f");
        changed |= ImGui.sliderInt(t("helix.thickness"), helixThickness, 1, 6);
        changed |= checkbox("helix.pillar", helixPillar, () -> helixPillar = !helixPillar);
        return changed;
    }

    // ---- Tornado ----

    /**
     * Funnel standing on its thin end, the centerline sways around the vertical axis.
     */
    private static ShapeTest createTornadoTest() {
        float bottom = Math.clamp(tornadoBottom[0], 0.01f, 1f);
        float curve = Math.max(0.1f, tornadoCurve[0]);
        float sway = Math.clamp(tornadoSway[0], 0f, 0.95f);
        double twists = tornadoTwists[0];
        boolean swirl = tornadoSwirl;
        int arms = Math.max(1, tornadoArms[0]);
        double keep = 1 - 2 * Math.clamp(tornadoGap[0], 0f, 1f);

        return (x, y, z) -> {
            if (y < -1 || y > 1) {
                return false;
            }
            double h = (y + 1) / 2;
            double radius = bottom + (1 - bottom) * Math.pow(h, curve);
            // Keeps the funnel inside the box: the sway uses only the room the radius leaves free
            double amplitude = sway * (1 - radius);
            double phase = 2 * Math.PI * twists * h;
            double dx = x - amplitude * Math.sin(phase);
            double dz = z - amplitude * Math.cos(phase);
            if (dx * dx + dz * dz > radius * radius) {
                return false;
            }
            if (!swirl) {
                return true;
            }
            double angle = Math.atan2(dz, dx);
            return Math.sin(arms * angle + 6 * Math.PI * h) <= keep;
        };
    }

    // ---- Skull ----

    private record Dent(double x, double y, double z, double radius) {
    }

    /**
     * Skull facing +Z: cranium, cheekbones and jaw, minus eye sockets, nose, mouth, teeth gaps and dents.
     */
    private static ShapeTest createSkullTest(float sizeX, float sizeY) {
        double eye = Math.clamp(skullEyeSize[0], 0.05f, 0.5f);
        double eyeSpacing = skullEyeSpacing[0];
        double eyeStretch = eye * (0.8 + 2.4 * Math.clamp(skullEyeDepth[0], 0f, 1f));
        double nose = Math.max(0, skullNoseSize[0]);
        double jawWidth = Math.clamp(skullJawWidth[0], 0.2f, 0.95f);
        double open = Math.clamp(skullMouthOpen[0], 0f, 0.4f);

        boolean teeth = skullTeeth;
        boolean lowerTeeth = skullTeethRows[0] == TEETH_BOTH;
        int teethCount = Math.max(1, skullTeethCount[0]);
        double teethWidth = Math.clamp(skullTeethWidth[0], 0.05f, 0.95f);
        double toothLength = Math.max(0.01, skullToothLength[0]);
        double toothGap = Math.clamp(skullToothGap[0], 0f, 0.95f);
        boolean pointed = skullPointedTeeth;
        double missing = skullMissingTeeth[0];
        Random random = new Random(skullSeed[0] * 104729L);

        // Which teeth are missing, upper row first
        boolean[] missingTeeth = new boolean[teethCount * 2];
        for (int i = 0; i < missingTeeth.length; i++) {
            missingTeeth[i] = random.nextDouble() < missing;
        }

        // Dents sit on the cranium surface, away from the face
        int dentCount = Math.max(0, skullDents[0]);
        double dentSize = skullDentSize[0];
        double dentDepth = Math.clamp(skullDentDepth[0], 0f, 1f);
        Dent[] dents = new Dent[dentCount];
        for (int i = 0; i < dentCount; i++) {
            double dx;
            double dy;
            double dz;
            do {
                dx = random.nextDouble() * 2 - 1;
                dy = random.nextDouble() * 1.2 - 0.2;
                dz = random.nextDouble() * 2 - 1;
            } while (dx * dx + dy * dy + dz * dz < 0.05 || (dz > 0.4 && dy < 0.35));
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= length;
            dy /= length;
            dz /= length;
            double scale = 1 / Math.sqrt(sq(dx / 0.92) + sq(dy / 0.8) + sq(dz / 0.98));
            double size = dentSize * (0.7 + random.nextDouble() * 0.6);
            double offset = size * (1 - dentDepth);
            dents[i] = new Dent(dx * (scale + offset), 0.2 + dy * (scale + offset), dz * (scale + offset), size);
        }

        double upperLine = -0.72;
        double lowerLine = upperLine - open;
        double halfY = sizeY / 2;
        double mouthBlocks = upperLine * halfY;
        double jawCenter = -0.62 - open * 0.5;
        double jawHeight = 0.38 - open * 0.5;

        return (x, y, z) -> {
            if (Math.abs(x) > 1 || Math.abs(y) > 1 || Math.abs(z) > 1) {
                return false;
            }
            double cranium = sq(x / 0.92) + sq((y - 0.2) / 0.8) + sq(z / 0.98);
            double cheeks = quad(x / 0.8) + quad((y + 0.15) / 0.3) + quad((z - 0.25) / 0.7);
            double jaw = quad(x / jawWidth) + quad((y - jawCenter) / jawHeight) + quad((z - 0.2) / 0.7);
            if (cranium > 1 && cheeks > 1 && jaw > 1) {
                return false;
            }

            for (Dent dent : dents) {
                if (sq(x - dent.x()) + sq(y - dent.y()) + sq(z - dent.z()) < dent.radius() * dent.radius()) {
                    return false;
                }
            }

            // Eye sockets, stretched along Z so they dig into the face
            for (int side = -1; side <= 1; side += 2) {
                double socket = sq((x - side * eyeSpacing) / eye) + sq((y + 0.05) / (eye * 1.05)) + sq((z - 0.92) / eyeStretch);
                if (socket <= 1) {
                    return false;
                }
            }

            // Nose, narrow at the top and wider at the bottom
            if (nose > 0 && z > 0.6 && y < -0.22 && y > -0.22 - 0.28 * nose) {
                double t = (-0.22 - y) / (0.28 * nose);
                if (Math.abs(x) <= (0.04 + 0.13 * t) * Math.min(1.5, nose)) {
                    return false;
                }
            }

            // Open mouth
            if (open > 0 && y < upperLine && y > lowerLine && z > 0.2 && Math.abs(x) < jawWidth * 0.85) {
                return false;
            }

            if (teeth && z > 0.62 && Math.abs(x) < teethWidth) {
                if (open <= 0 && Math.abs(y * halfY - mouthBlocks) < 0.5) {
                    return false;
                }
                boolean upperRow = y >= upperLine && y <= upperLine + toothLength;
                boolean lowerRow = lowerTeeth && y <= lowerLine && y >= lowerLine - toothLength;
                if (upperRow || lowerRow) {
                    double cell = (x + teethWidth) / (2 * teethWidth) * teethCount;
                    int index = Math.min(teethCount - 1, (int) Math.floor(cell));
                    if (missingTeeth[index + (upperRow ? 0 : teethCount)]) {
                        return false;
                    }
                    double fromCenter = Math.abs(cell - index - 0.5) * 2;
                    double fromTip = upperRow ? (y - upperLine) / toothLength : (lowerLine - y) / toothLength;
                    double allowed = (1 - toothGap) * (pointed ? Math.clamp(fromTip, 0, 1) : 1);
                    if (fromCenter > allowed) {
                        return false;
                    }
                }
            }
            return true;
        };
    }

    // ---- Heart ----

    private static ShapeTest createHeartTest() {
        boolean flat = heartStyle[0] == STYLE_FLAT;
        float thickness = Math.clamp(heartThickness[0], 0.01f, 1f);
        boolean lying = heartOrientation[0] == ORIENTATION_LYING;

        return (x, y, z) -> {
            float u = x;
            float v = lying ? -z : y;
            float w = lying ? y : z;
            if (Math.abs(u) > 1 || Math.abs(v) > 1 || Math.abs(w) > 1) {
                return false;
            }
            // Classic heart curves, scaled so the heart fills -1..1
            double hx = u * 1.14;
            double hy = v * 1.12 + 0.12;
            if (flat) {
                if (Math.abs(w) > thickness) {
                    return false;
                }
                double f = cube(hx * hx + hy * hy - 1) - hx * hx * hy * hy * hy;
                return f <= 0;
            }
            double hz = w * 0.7 / thickness;
            double f = cube(hx * hx + 2.25 * hz * hz + hy * hy - 1) - hx * hx * hy * hy * hy - 0.1125 * hz * hz * hy * hy * hy;
            return f <= 0;
        };
    }

    // ---- Crescent ----

    private static ShapeTest createCrescentTest() {
        boolean flat = crescentStyle[0] == STYLE_FLAT;
        float thickness = Math.clamp(crescentThickness[0], 0.01f, 1f);
        boolean lying = crescentOrientation[0] == ORIENTATION_LYING;
        float offset = crescentOffset[0];
        float inner = crescentInner[0];

        return (x, y, z) -> {
            float u = x;
            float v = lying ? -z : y;
            float w = lying ? y : z;
            double outerSq = u * u + v * v;
            if (outerSq > 1) {
                return false;
            }
            // The bite of the second circle opens the crescent towards +X
            if (sq(u - offset) + v * v <= inner * inner) {
                return false;
            }
            double depth = flat ? thickness : thickness * Math.sqrt(1 - outerSq);
            return Math.abs(w) <= depth;
        };
    }

    // ---- Crystal ----

    private record Crystal(double baseX, double baseY, double baseZ, double axisX, double axisY, double axisZ,
                           double length, double radius) {
    }

    /**
     * Cluster of hexagonal crystals growing out of the bottom center, one big crystal in the middle.
     */
    private static ShapeTest createCrystalTest() {
        int count = Math.max(1, crystalCount[0]);
        double tip = Math.clamp(crystalTip[0], 0.01f, 1f);
        Random random = new Random(crystalVariant[0] * 7919L);

        Crystal[] crystals = new Crystal[count];
        crystals[0] = new Crystal(0, -1, 0, 0, 1, 0, 2, 0.32);
        for (int i = 1; i < count; i++) {
            double angle = 2 * Math.PI * i / (count - 1) + (random.nextDouble() - 0.5) * 0.8;
            double tilt = Math.toRadians(18 + random.nextDouble() * 30);
            crystals[i] = new Crystal(
                Math.cos(angle) * 0.22, -1, Math.sin(angle) * 0.22,
                Math.sin(tilt) * Math.cos(angle), Math.cos(tilt), Math.sin(tilt) * Math.sin(angle),
                0.9 + random.nextDouble() * 0.7,
                0.12 + random.nextDouble() * 0.1);
        }

        return (x, y, z) -> {
            if (Math.abs(x) > 1 || Math.abs(y) > 1 || Math.abs(z) > 1) {
                return false;
            }
            for (Crystal c : crystals) {
                double px = x - c.baseX();
                double py = y - c.baseY();
                double pz = z - c.baseZ();
                double along = px * c.axisX() + py * c.axisY() + pz * c.axisZ();
                if (along < 0 || along > c.length()) {
                    continue;
                }
                double qx = px - c.axisX() * along;
                double qy = py - c.axisY() * along;
                double qz = pz - c.axisZ() * along;

                // Local frame perpendicular to the axis, for the hexagonal cross section
                double ux = c.axisZ();
                double uz = -c.axisX();
                double uLength = Math.sqrt(ux * ux + uz * uz);
                double a;
                double b;
                if (uLength < 1e-6) {
                    a = qx;
                    b = qz;
                } else {
                    ux /= uLength;
                    uz /= uLength;
                    double vx = c.axisY() * uz;
                    double vy = c.axisZ() * ux - c.axisX() * uz;
                    double vz = -c.axisY() * ux;
                    a = qx * ux + qz * uz;
                    b = qx * vx + qy * vy + qz * vz;
                }
                double hex = Math.max(Math.abs(a), Math.abs(a) * 0.5 + Math.abs(b) * 0.866);

                double tipStart = c.length() * (1 - tip);
                double radius = along <= tipStart ? c.radius() : c.radius() * (c.length() - along) / (c.length() - tipStart);
                if (hex <= radius) {
                    return true;
                }
            }
            return false;
        };
    }

    // ---- Onion dome ----

    /**
     * Onion dome around the Y axis: neck at the bottom, bulb, then a concave pointed tip.
     */
    private static ShapeTest createOnionTest() {
        double neck = Math.clamp(onionNeck[0], 0f, 1f);
        double bulge = Math.clamp(onionBulge[0], 0.05f, 0.95f);
        double tip = Math.max(0, onionTip[0]);

        return (x, y, z) -> {
            if (y < -1 || y > 1) {
                return false;
            }
            double h = (y + 1) / 2;
            double radius;
            if (h < bulge) {
                radius = neck + (1 - neck) * Math.sin(Math.PI / 2 * h / bulge);
            } else {
                double s = (h - bulge) / (1 - bulge);
                radius = Math.cos(Math.PI / 2 * s) * Math.pow(1 - s, tip);
            }
            return x * x + z * z <= radius * radius;
        };
    }

    // ---- Helix ----

    /**
     * Spiral ramp around the Y axis, optionally with a center pillar.
     */
    private static ShapeTest createHelixTest(float sizeY) {
        double turns = Math.max(0.05f, helixTurns[0]);
        double inner = Math.clamp(helixInner[0], 0f, 0.95f);
        // Height of one full turn and the ramp thickness, both in blocks
        double turnHeight = sizeY / turns;
        double thickness = Math.max(1, helixThickness[0]);
        boolean pillar = helixPillar;

        return (x, y, z) -> {
            if (y < -1 || y > 1) {
                return false;
            }
            double distance = Math.sqrt(x * x + z * z);
            if (distance > 1) {
                return false;
            }
            if (distance < inner) {
                return pillar;
            }
            double h = (y + 1) / 2;
            double angle = (Math.atan2(z, x) + Math.PI) / (2 * Math.PI);
            double progress = h * turns - angle;
            if (progress < 0) {
                return false;
            }
            double withinTurn = progress - Math.floor(progress);
            return withinTurn * turnHeight < thickness;
        };
    }

    // ---- Math ----

    private static double sq(double value) {
        return value * value;
    }

    private static double quad(double value) {
        double square = value * value;
        return square * square;
    }

    private static double cube(double value) {
        return value * value * value;
    }

}
