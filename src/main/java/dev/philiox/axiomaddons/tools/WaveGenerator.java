package dev.philiox.axiomaddons.tools;

import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds ocean waves out of blocks, relative to the origin (0, 0, 0) at the foot of the crest.
 *
 * In wave space u points in the direction the wave travels (towards its front face), v is the height above the
 * base and w runs along the crest.
 */
final class WaveGenerator {

    static final int TYPE_BREAKING = 0;
    static final int TYPE_SWELL = 1;

    private static final int DEEP = 0;
    private static final int MID = 1;
    private static final int LIGHT = 2;
    private static final int FOAM = 3;

    /** deep, mid, light, foam, foam variation */
    static final BlockState[][] PALETTES = {
        palette(Blocks.BLUE_CONCRETE.defaultBlockState(), Blocks.CYAN_CONCRETE.defaultBlockState(),
            Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(), Blocks.WHITE_CONCRETE.defaultBlockState(),
            Blocks.WHITE_CONCRETE_POWDER.defaultBlockState()),
        palette(Blocks.CYAN_CONCRETE.defaultBlockState(), Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
            Blocks.LIGHT_BLUE_CONCRETE_POWDER.defaultBlockState(), Blocks.WHITE_CONCRETE.defaultBlockState(),
            Blocks.WHITE_CONCRETE_POWDER.defaultBlockState()),
        palette(Blocks.BLUE_WOOL.defaultBlockState(), Blocks.CYAN_WOOL.defaultBlockState(),
            Blocks.LIGHT_BLUE_WOOL.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState(),
            Blocks.WHITE_WOOL.defaultBlockState()),
        palette(Blocks.BLUE_STAINED_GLASS.defaultBlockState(), Blocks.CYAN_STAINED_GLASS.defaultBlockState(),
            Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), Blocks.WHITE_STAINED_GLASS.defaultBlockState(),
            Blocks.WHITE_STAINED_GLASS.defaultBlockState()),
        palette(Blocks.BLUE_ICE.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState(),
            Blocks.ICE.defaultBlockState(), Blocks.SNOW_BLOCK.defaultBlockState(),
            Blocks.SNOW_BLOCK.defaultBlockState())
    };

    private static BlockState[] palette(BlockState... states) {
        return states;
    }

    record Settings(int type, int height, int crestLength, float backLength, float frontLength, float curl,
                    float tubeSize, int lipThickness, boolean peel, int swellCrests, float wavelength, float steepness,
                    float endTaper, float variation, float chop, float foam, boolean hollow, int shellThickness,
                    int baseDepth, BlockState[] palette, long seed) {
    }

    /** Result of sampling one position. */
    private static final class Sample {
        boolean inside;
        int material;
    }

    private WaveGenerator() {
    }

    /**
     * @param angle direction the wave travels, in degrees (Minecraft yaw convention)
     */
    static void generate(ChunkedBlockRegion region, Settings s, double angle) {
        double radians = Math.toRadians(angle);
        double dirX = -Math.sin(radians);
        double dirZ = Math.cos(radians);
        double perpX = -dirZ;
        double perpZ = dirX;

        double uMin;
        double uMax;
        if (s.type() == TYPE_SWELL) {
            double total = Math.max(1, s.swellCrests()) * s.wavelength() * s.height();
            uMin = -total / 2;
            uMax = total / 2;
        } else {
            uMin = -s.backLength() * s.height() - 1;
            uMax = s.frontLength() * s.height() + s.tubeSize() * s.height() + 1;
        }
        double halfLength = s.crestLength() / 2.0;

        // Bounding box of the rotated wave footprint
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (double u : new double[] {uMin, uMax}) {
            for (double w : new double[] {-halfLength, halfLength}) {
                double x = dirX * u + perpX * w;
                double z = dirZ * u + perpZ * w;
                minX = Math.min(minX, (int) Math.floor(x));
                maxX = Math.max(maxX, (int) Math.ceil(x));
                minZ = Math.min(minZ, (int) Math.floor(z));
                maxZ = Math.max(maxZ, (int) Math.ceil(z));
            }
        }
        int maxY = s.height() + s.lipThickness() + 2;
        int minY = -Math.max(0, s.baseDepth());

        Sample sample = new Sample();
        Sample neighbour = new Sample();
        int[][] directions = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double cx = x + 0.5;
                double cz = z + 0.5;
                double u = cx * dirX + cz * dirZ;
                double w = cx * perpX + cz * perpZ;
                if (u < uMin || u > uMax || Math.abs(w) > halfLength) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    if (y < 0) {
                        // Sea floor slab under the whole wave
                        region.addBlock(x, y, z, s.palette()[DEEP]);
                        continue;
                    }
                    sampleWave(s, u, y + 0.5, w, sample);
                    if (!sample.inside) {
                        continue;
                    }
                    if (s.hollow() && y > 0 && !isSurface(s, cx, y + 0.5, cz, dirX, dirZ, perpX, perpZ, directions, neighbour)) {
                        continue;
                    }
                    region.addBlock(x, y, z, pick(s, sample.material, x, y, z));
                }
            }
        }
    }

    private static boolean isSurface(Settings s, double cx, double cy, double cz, double dirX, double dirZ,
                                     double perpX, double perpZ, int[][] directions, Sample neighbour) {
        for (int distance = 1; distance <= Math.max(1, s.shellThickness()); distance++) {
            for (int[] d : directions) {
                double nx = cx + d[0] * distance;
                double ny = cy + d[1] * distance;
                double nz = cz + d[2] * distance;
                sampleWave(s, nx * dirX + nz * dirZ, ny, nx * perpX + nz * perpZ, neighbour);
                if (!neighbour.inside) {
                    return true;
                }
            }
        }
        return false;
    }

    private static BlockState pick(Settings s, int material, int x, int y, int z) {
        BlockState[] palette = s.palette();
        if (material == FOAM) {
            return hash(x, y, z, s.seed()) > 0.5 ? palette[3] : palette[4];
        }
        return palette[material];
    }

    // ---- Wave shape ----

    private static void sampleWave(Settings s, double u, double v, double w, Sample out) {
        out.inside = false;
        out.material = DEEP;
        double halfLength = s.crestLength() / 2.0;
        double wn = w / halfLength;
        if (Math.abs(wn) > 1 || v < 0) {
            return;
        }

        // Height along the crest: tapered ends and some irregularity
        double taper = s.endTaper() <= 0 ? 1 : smoothstep(0, s.endTaper(), 1 - Math.abs(wn));
        double variation = 1 + s.variation() * noise1(w / Math.max(4, s.crestLength() * 0.25), s.seed());
        double height = s.height() * taper * variation;
        if (height < 1) {
            return;
        }

        // Choppy surface
        if (s.chop() > 0) {
            v += s.chop() * 1.5 * noise3(u / 5, v / 5, w / 5, s.seed() + 7);
        }

        if (s.type() == TYPE_SWELL) {
            sampleSwell(s, u, v, height, out);
        } else {
            double curl = s.curl() * (s.peel() ? (wn + 1) / 2 : 1);
            sampleBreaking(s, u, v, height, curl, out);
        }
        if (out.inside && out.material != FOAM) {
            // Colour bands from deep to light with a dithered border
            double t = v / height + (hash((int) Math.floor(u), (int) Math.floor(v), (int) Math.floor(w), s.seed()) - 0.5) * 0.12;
            if (out.material == DEEP) {
                out.material = t < 0.4 ? DEEP : t < 0.75 ? MID : LIGHT;
            }
        }
    }

    private static void sampleBreaking(Settings s, double u, double v, double height, double curl, Sample out) {
        double back = s.backLength() * height;
        double front = s.frontLength() * height;
        double radius = height * s.tubeSize() * (0.3 + 0.7 * curl);
        double centerU = radius * 0.6;
        double centerV = height - radius;
        double sweep = Math.min(260, curl * 250);
        double foam = s.foam();

        double du = u - centerU;
        double dv = v - centerV;
        double distance = Math.sqrt(du * du + dv * dv);
        double angle = Math.toDegrees(Math.atan2(dv, du));
        double lipEnd = 90 - sweep;

        // The lip: a ring around the tube that gets thinner towards its tip
        boolean ring = false;
        boolean lipTip = false;
        if (sweep > 1 && angle >= lipEnd && distance <= radius) {
            double along = (angle - lipEnd) / (180 - lipEnd);
            double thickness = s.lipThickness() * (0.35 + 0.65 * along);
            ring = distance >= radius - thickness;
            lipTip = along < 0.25;
        }
        boolean inTube = curl > 0.05 && distance < radius - s.lipThickness() * 0.35 && !ring;

        boolean body;
        if (u <= centerU) {
            double start = centerU - back;
            if (u < start) {
                return;
            }
            double rise = 0.5 - 0.5 * Math.cos(Math.PI * Math.clamp((u - start) / back, 0, 1));
            body = v <= height * rise;
        } else {
            double frontTop = height + (centerV - radius - height) * Math.min(1, curl * 1.6);
            double s01 = Math.clamp((u - centerU) / front, 0, 1);
            body = v <= Math.max(0, frontTop) * Math.pow(1 - s01, 1.6);
        }

        out.inside = ring || (body && !inTube);
        if (!out.inside) {
            return;
        }
        if (ring) {
            out.material = foam > 0 && lipTip ? FOAM : LIGHT;
            return;
        }
        // Foam on the crest and whitewater at the foot of the front face
        boolean crest = v > height - 1 - foam * 2 && u > centerU - back * 0.15 * (0.5 + foam);
        boolean whitewater = u > centerU + radius * 0.3 && v < height * 0.15 * foam * (0.5 + curl);
        out.material = foam > 0 && (crest || whitewater) ? FOAM : DEEP;
    }

    private static void sampleSwell(Settings s, double u, double v, double height, Sample out) {
        double wavelength = Math.max(2, s.wavelength() * s.height());
        double total = Math.max(1, s.swellCrests()) * wavelength;
        double edge = Math.abs(u) / (total / 2);
        if (edge > 1) {
            return;
        }
        double envelope = smoothstep(1, 0.6, edge);
        // Peaked crests and flat troughs, like a trochoid
        double phase = (1 + Math.cos(2 * Math.PI * u / wavelength)) / 2;
        double surface = height * envelope * Math.pow(phase, 1 + 2.5 * s.steepness());
        out.inside = v <= Math.max(1, surface);
        if (out.inside) {
            boolean crest = s.foam() > 0 && phase > 0.9 && v > surface - 1 - s.foam() && surface > height * 0.6;
            out.material = crest ? FOAM : DEEP;
        }
    }

    // ---- Math ----

    static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.clamp((x - edge0) / (edge1 - edge0), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static double noise1(double x, long seed) {
        int x0 = (int) Math.floor(x);
        double f = x - x0;
        f = f * f * (3 - 2 * f);
        double a = hash(x0, 0, 0, seed) * 2 - 1;
        double b = hash(x0 + 1, 0, 0, seed) * 2 - 1;
        return a + (b - a) * f;
    }

    static double noise3(double x, double y, double z, long seed) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int z0 = (int) Math.floor(z);
        double fx = x - x0;
        double fy = y - y0;
        double fz = z - z0;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        fz = fz * fz * (3 - 2 * fz);
        double result = 0;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    double weight = (dx == 1 ? fx : 1 - fx) * (dy == 1 ? fy : 1 - fy) * (dz == 1 ? fz : 1 - fz);
                    result += weight * (hash(x0 + dx, y0 + dy, z0 + dz, seed) * 2 - 1);
                }
            }
        }
        return result;
    }

    /** Deterministic value in 0..1. */
    static double hash(int x, int y, int z, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h & 0xFFFFFF) / (double) 0xFFFFFF;
    }

}
