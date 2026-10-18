package dev.philiox.axiomaddons;

import com.moulberry.axiom.editor.ImGuiHelper;
import dev.philiox.axiomaddons.ExtraShapes.ShapeDefinition;
import dev.philiox.axiomaddons.ExtraShapes.ShapeTest;
import imgui.moulberry92.ImGui;
import net.minecraft.client.resources.language.I18n;

/**
 * The Boat shape: a hull, optionally rigged as a sailing ship with masts, square sails, bowsprit and jib,
 * a stern castle with several stories and windows, and a forecastle.
 *
 * Length runs along X with the bow at +X, height along Y, width along Z.
 */
final class ShipShape {

    static final float[] ROWBOAT_SIZE = {31, 7, 13};
    static final float[] SHIP_SIZE = {51, 45, 17};

    private static final int MODE_HULL = 0;
    private static final int MODE_FRAME = 1;

    private static final int[] mode = {MODE_HULL};
    private static final float[] bowLength = {0.35f};
    private static final float[] sternTaper = {0.15f};
    private static final float[] hullRoundness = {2.0f};
    private static final float[] bowRise = {0.6f};
    private static final int[] ribSpacing = {4};
    private static boolean openTop = true;

    private static boolean ship = true;
    private static final float[] hullShare = {0.3f};
    private static final int[] masts = {3};
    private static final int[] mastWidth = {1};
    private static boolean sails = true;
    private static final int[] sailTiers = {3};
    private static final float[] sailWidth = {0.95f};
    private static final float[] sailBillow = {1.5f};
    private static boolean bowsprit = true;
    private static final float[] bowspritLength = {0.14f};
    private static boolean jib = true;
    private static final int[] sternStories = {2};
    private static final int[] storyHeight = {4};
    private static final float[] sternLength = {0.28f};
    private static boolean forecastle = true;
    private static boolean windows = true;

    private ShipShape() {
    }

    static float[] defaultSize() {
        return ship ? SHIP_SIZE : ROWBOAT_SIZE;
    }

    // ---- Options ----

    private static String t(String key) {
        return I18n.get("philioxaxiomaddons." + key);
    }

    private static boolean slider(String key, float[] value, float min, float max) {
        return ImGui.sliderFloat(t(key), value, min, max, "%.2f");
    }

    private static boolean slider(String key, int[] value, int min, int max) {
        return ImGui.sliderInt(t(key), value, min, max);
    }

    private static boolean checkbox(String key, boolean value, Runnable toggle) {
        if (ImGui.checkbox(t(key), value)) {
            toggle.run();
            return true;
        }
        return false;
    }

    static boolean displayOptions() {
        ImGuiHelper.separatorWithText(t("shape.boat"));
        boolean changed = checkbox("boat.ship", ship, () -> {
            ship = !ship;
            ExtraShapes.requestSize(ship ? SHIP_SIZE : ROWBOAT_SIZE);
        });
        if (ship && ImGui.button(t("boat.ship_size"))) {
            ExtraShapes.requestSize(SHIP_SIZE);
            changed = true;
        }

        ImGuiHelper.separatorWithText(t("boat.section.hull"));
        changed |= ExtraShapes.combo("philioxaxiomaddons.boat.mode", mode,
            "philioxaxiomaddons.boat.mode.hull", "philioxaxiomaddons.boat.mode.frame");
        changed |= slider("boat.bow_length", bowLength, 0.1f, 0.9f);
        changed |= slider("boat.stern_taper", sternTaper, 0.0f, 0.6f);
        changed |= slider("boat.hull_roundness", hullRoundness, 1.0f, 4.0f);
        changed |= slider("boat.bow_rise", bowRise, 0.0f, 1.0f);
        if (mode[0] == MODE_FRAME) {
            changed |= slider("boat.rib_spacing", ribSpacing, 2, 12);
        } else {
            changed |= checkbox("boat.open_top", openTop, () -> openTop = !openTop);
        }

        if (ship) {
            changed |= slider("boat.hull_share", hullShare, 0.15f, 0.8f);

            ImGuiHelper.separatorWithText(t("boat.section.rigging"));
            changed |= slider("boat.masts", masts, 0, 3);
            if (masts[0] > 0) {
                changed |= slider("boat.mast_width", mastWidth, 1, 3);
                changed |= checkbox("boat.sails", sails, () -> sails = !sails);
                if (sails) {
                    changed |= slider("boat.sail_tiers", sailTiers, 1, 4);
                    changed |= slider("boat.sail_width", sailWidth, 0.3f, 1.0f);
                    changed |= slider("boat.sail_billow", sailBillow, 0.0f, 5.0f);
                }
            }
            changed |= checkbox("boat.bowsprit", bowsprit, () -> bowsprit = !bowsprit);
            if (bowsprit) {
                changed |= slider("boat.bowsprit_length", bowspritLength, 0.05f, 0.3f);
                if (masts[0] > 0) {
                    changed |= checkbox("boat.jib", jib, () -> jib = !jib);
                }
            }

            if (mode[0] == MODE_HULL) {
                ImGuiHelper.separatorWithText(t("boat.section.castle"));
                changed |= slider("boat.stern_stories", sternStories, 0, 4);
                if (sternStories[0] > 0) {
                    changed |= slider("boat.stern_length", sternLength, 0.1f, 0.5f);
                }
                changed |= checkbox("boat.forecastle", forecastle, () -> forecastle = !forecastle);
                if (sternStories[0] > 0 || forecastle) {
                    changed |= slider("boat.story_height", storyHeight, 2, 8);
                    changed |= checkbox("boat.windows", windows, () -> windows = !windows);
                }
            }
        }
        ImGui.textWrapped(t(ship ? "boat.hint.ship" : mode[0] == MODE_FRAME ? "boat.hint.frame" : "boat.hint"));
        return changed;
    }

    // ---- Shape ----

    static ShapeDefinition create(float sizeX, float sizeY, float sizeZ) {
        float blockX = 2 / sizeX;
        float blockY = 2 / sizeY;
        float blockZ = 2 / sizeZ;

        boolean rigged = ship && (masts[0] > 0 || bowsprit || sternStories[0] > 0 || forecastle);
        float deck = rigged ? -1 + 2 * Math.clamp(hullShare[0], 0.05f, 1f) : 1;
        float hullFront = ship && bowsprit ? 1 - 2 * Math.clamp(bowspritLength[0], 0.02f, 0.45f) : 1;

        Hull hull = new Hull(deck, hullFront);
        ShapeTest hullTest = (x, y, z) -> hull.contains(x, y, z, false);
        // Continues one block above the deck, so Hollow keeps the deck open
        ShapeTest openHullTest = (x, y, z) -> hull.contains(x, y, z, true) && y <= deck + blockY;

        boolean frame = mode[0] == MODE_FRAME;
        ShapeTest castles = ship && !frame ? createCastles(hull, deck, blockX, blockY, blockZ) : null;
        ShapeTest rigging = ship ? createRigging(hull, deck, hullFront, blockX, blockY, blockZ) : null;

        ShapeTest test = hullTest;
        if (castles != null) {
            test = or(test, castles);
        }
        if (rigging != null) {
            test = or(test, rigging);
        }

        if (frame) {
            // Keel along the bottom center, ribs every few blocks, gunwale along the top edge, transom at the stern
            float spacing = Math.max(2, ribSpacing[0]);
            float halfX = sizeX / 2;
            float halfZ = sizeZ / 2;
            ShapeTest filter = (x, y, z) -> {
                float blocksX = x * halfX;
                boolean rib = Math.abs(blocksX - spacing * Math.round(blocksX / spacing)) <= 0.5f;
                boolean keel = Math.abs(z * halfZ) <= 0.5f;
                boolean gunwale = y + blockY > deck;
                boolean transom = x - blockX < -1;
                return rib || keel || gunwale || transom;
            };
            return new ShapeDefinition(test, openHullTest, true, rigging, filter);
        }

        ShapeTest hullSurface = openTop ? openHullTest : hullTest;
        ShapeTest surface = castles != null ? or(hullSurface, castles) : hullSurface;
        return new ShapeDefinition(test, surface, false, rigging, null);
    }

    /** Hull spanning x -1..hullFront and y -1..deck. */
    private static final class Hull {
        private final float deck;
        private final float hullFront;
        private final float bowStart = 1 - 2 * Math.clamp(bowLength[0], 0.05f, 0.95f);
        private final float taper = Math.clamp(sternTaper[0], 0f, 0.95f);
        private final float roundness = Math.clamp(hullRoundness[0], 1f, 8f);
        private final float rise = Math.clamp(bowRise[0], 0f, 1f);

        private Hull(float deck, float hullFront) {
            this.deck = deck;
            this.hullFront = hullFront;
        }

        /** Hull local length coordinate, -1 at the stern and 1 at the bow tip. */
        double localX(double x) {
            return (x + 1) / (this.hullFront + 1) * 2 - 1;
        }

        double worldX(double localX) {
            return -1 + (localX + 1) / 2 * (this.hullFront + 1);
        }

        /** Half width of the deck at a hull local length position. */
        double deckHalfWidth(double hx) {
            if (hx > this.bowStart) {
                double t = (hx - this.bowStart) / (1 - this.bowStart);
                return Math.max(0, 1 - Math.pow(t, 1.7));
            }
            double t = (this.bowStart - hx) / (this.bowStart + 1);
            return 1 - this.taper * t * t;
        }

        boolean contains(float x, float rawY, float z, boolean ignoreTop) {
            if (x < -1 || x > this.hullFront) {
                return false;
            }
            double hx = this.localX(x);
            // Map the hull height range to -1..1
            double y = (rawY + 1) / (this.deck + 1) * 2 - 1;
            if (!ignoreTop && y > 1) {
                return false;
            }
            double keel = -1;
            if (hx > this.bowStart) {
                double t = (hx - this.bowStart) / (1 - this.bowStart);
                keel += this.rise * t * t;
            }
            if (y < keel) {
                return false;
            }
            // Cross section: narrow at the keel, full width at the deck. roundness 1 = V hull, 2+ = U hull
            double h = (Math.min(y, 1) - keel) / (1 - keel);
            double section = Math.pow(1 - Math.pow(1 - h, this.roundness), 1 / this.roundness);
            return Math.abs(z) <= this.deckHalfWidth(hx) * section;
        }
    }

    /** Stern castle with stepped stories and a forecastle, both with optional windows. */
    private static ShapeTest createCastles(Hull hull, float deck, float blockX, float blockY, float blockZ) {
        int stories = Math.max(0, sternStories[0]);
        boolean fore = forecastle;
        if (stories == 0 && !fore) {
            return null;
        }
        float story = Math.max(2, storyHeight[0]) * blockY;
        double sternEnd = hull.localX(-1) + Math.clamp(sternLength[0], 0.05f, 0.9f) * 2;
        double foreStart = 0.55;
        boolean withWindows = windows;

        return (x, y, z) -> {
            if (y <= deck || y > 1 || Math.abs(z) > 1 || x < -1) {
                return false;
            }
            double hx = hull.localX(x);
            double above = y - deck;

            if (stories > 0) {
                int level = (int) Math.floor(above / story);
                if (level < stories) {
                    // Each higher story is a bit shorter and narrower
                    double end = -1 + (sternEnd + 1) * (1 - 0.15 * level);
                    double halfWidth = hull.deckHalfWidth(hx) * (1 - 0.06 * level);
                    if (hx <= end && Math.abs(z) <= halfWidth) {
                        double inStory = (above - level * story) / story;
                        return !(withWindows && isWindow(x, z, inStory, halfWidth, blockX, blockZ, x - blockX * 1.5 < -1));
                    }
                }
            }
            if (fore && hx >= foreStart && above <= story * 0.8) {
                double halfWidth = hull.deckHalfWidth(hx);
                if (Math.abs(z) <= halfWidth) {
                    return !(withWindows && isWindow(x, z, above / (story * 0.8), halfWidth, blockX, blockZ, false));
                }
            }
            return false;
        };
    }

    /** Window openings in the outer walls, every third block at mid height. */
    private static boolean isWindow(double x, double z, double inStory, double halfWidth, float blockX, float blockZ, boolean sternWall) {
        if (inStory < 0.35 || inStory > 0.75) {
            return false;
        }
        boolean sideWall = Math.abs(z) > halfWidth - blockZ * 1.5;
        if (sideWall && Math.floorMod(Math.round(x / blockX), 3) == 1) {
            return true;
        }
        return sternWall && Math.floorMod(Math.round(z / blockZ), 3) == 1;
    }

    /** Masts, yards, square sails, bowsprit and jib. Always placed solid. */
    private static ShapeTest createRigging(Hull hull, float deck, float hullFront, float blockX, float blockY, float blockZ) {
        int mastCount = Math.clamp(masts[0], 0, 3);
        boolean withBowsprit = bowsprit && hullFront < 1;
        if (mastCount == 0 && !withBowsprit) {
            return null;
        }
        double[] localPositions = switch (mastCount) {
            case 1 -> new double[] {0.05};
            case 2 -> new double[] {-0.35, 0.3};
            case 3 -> new double[] {-0.5, 0.0, 0.45};
            default -> new double[0];
        };
        double[] heights = switch (mastCount) {
            case 2 -> new double[] {0.9, 1.0};
            case 3 -> new double[] {0.8, 1.0, 0.88};
            default -> new double[] {1.0};
        };
        double[] mastX = new double[mastCount];
        double[] mastTop = new double[mastCount];
        for (int i = 0; i < mastCount; i++) {
            mastX[i] = hull.worldX(localPositions[i]);
            mastTop[i] = deck + (1 - deck) * heights[i];
        }
        double mastHalfX = Math.max(1, mastWidth[0]) * blockX / 2;
        double mastHalfZ = Math.max(1, mastWidth[0]) * blockZ / 2;
        boolean withSails = sails;
        int tiers = Math.max(1, sailTiers[0]);
        double sailHalfWidth = Math.clamp(sailWidth[0], 0.1f, 1f);
        double billow = sailBillow[0] * blockX;

        double bowspritRise = (1 - deck) * 0.25;
        boolean withJib = withBowsprit && jib && mastCount > 0;
        // The jib runs from the bowsprit tip to the frontmost mast
        int foreMast = mastCount - 1;

        return (x, y, z) -> {
            if (Math.abs(x) > 1 || Math.abs(y) > 1 || Math.abs(z) > 1) {
                return false;
            }
            for (int i = 0; i < mastCount; i++) {
                if (Math.abs(x - mastX[i]) <= mastHalfX && Math.abs(z) <= mastHalfZ && y <= mastTop[i]) {
                    return true;
                }
                if (!withSails) {
                    continue;
                }
                double sailBottom = deck + blockY * 3;
                double sailTop = mastTop[i] - blockY;
                double tierHeight = (sailTop - sailBottom) / tiers;
                if (y < sailBottom || y > sailTop + blockY * 0.5 || tierHeight <= blockY) {
                    continue;
                }
                int tier = Math.min(tiers - 1, (int) Math.floor((y - sailBottom) / tierHeight));
                double tierBottom = sailBottom + tier * tierHeight;
                double tierTop = tierBottom + tierHeight;
                double halfWidth = sailHalfWidth * (1 - 0.18 * tier);
                // Yard: the horizontal spar on top of each sail
                if (Math.abs(y - tierTop) <= blockY * 0.5 && Math.abs(x - mastX[i]) <= blockX * 0.5 && Math.abs(z) <= halfWidth + blockZ) {
                    return true;
                }
                // Sail: billows towards the bow, most in the middle
                if (Math.abs(z) <= halfWidth && y < tierTop - blockY * 0.5) {
                    double across = z / halfWidth;
                    double up = (y - tierBottom) / tierHeight;
                    double bulge = billow * (1 - across * across) * Math.sin(Math.PI * up);
                    if (Math.abs(x - (mastX[i] + bulge)) <= blockX * 0.5) {
                        return true;
                    }
                }
            }
            if (withBowsprit && x >= hullFront - blockX * 2) {
                double line = deck + (x - hullFront) / (1 - hullFront) * bowspritRise;
                if (Math.abs(y - line) <= blockY * 0.6 && Math.abs(z) <= blockZ * 0.5) {
                    return true;
                }
            }
            if (withJib && Math.abs(z) <= blockZ * 0.5) {
                double tipX = 1;
                double tipY = deck + bowspritRise;
                double topX = mastX[foreMast];
                double topY = deck + (mastTop[foreMast] - deck) * 0.85;
                double baseY = deck + blockY * 2;
                return insideTriangle(x, y, tipX, tipY, topX, topY, topX, baseY);
            }
            return false;
        };
    }

    private static boolean insideTriangle(double px, double py, double ax, double ay, double bx, double by, double cx, double cy) {
        double d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by);
        double d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy);
        double d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay);
        boolean negative = d1 < 0 || d2 < 0 || d3 < 0;
        boolean positive = d1 > 0 || d2 > 0 || d3 > 0;
        return !(negative && positive);
    }

    private static ShapeTest or(ShapeTest a, ShapeTest b) {
        return (x, y, z) -> a.contains(x, y, z) || b.contains(x, y, z);
    }

}
