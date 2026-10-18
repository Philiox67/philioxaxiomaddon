package dev.philiox.axiomaddons.tools;

import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Random;

/**
 * Procedural trees out of logs and leaves.
 */
final class TreeGenerator {

    static final int OAK = 0;
    static final int SPRUCE = 1;
    static final int BIRCH = 2;
    static final int PALM = 3;
    static final int DEAD = 4;
    static final int WILLOW = 5;

    record WoodSet(String key, Block log, Block leaves) {
    }

    static final WoodSet[] WOOD_SETS = {
        new WoodSet("oak", Blocks.OAK_LOG, Blocks.OAK_LEAVES),
        new WoodSet("spruce", Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES),
        new WoodSet("birch", Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES),
        new WoodSet("jungle", Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES),
        new WoodSet("acacia", Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES),
        new WoodSet("dark_oak", Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES),
        new WoodSet("mangrove", Blocks.MANGROVE_LOG, Blocks.MANGROVE_LEAVES),
        new WoodSet("cherry", Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES),
        new WoodSet("pale_oak", Blocks.PALE_OAK_LOG, Blocks.PALE_OAK_LEAVES),
        new WoodSet("azalea", Blocks.OAK_LOG, Blocks.FLOWERING_AZALEA_LEAVES),
        new WoodSet("crimson", Blocks.CRIMSON_STEM, Blocks.NETHER_WART_BLOCK),
        new WoodSet("warped", Blocks.WARPED_STEM, Blocks.WARPED_WART_BLOCK)
    };

    record Settings(int type, int height, float crown, int trunkWidth, float branches, WoodSet wood) {
    }

    private TreeGenerator() {
    }

    static void generate(ChunkedBlockRegion region, BlockPos base, Settings settings, long seed) {
        Builder builder = new Builder(region, settings.wood(), new Random(seed));
        double x = base.getX() + 0.5;
        double y = base.getY();
        double z = base.getZ() + 0.5;
        switch (settings.type()) {
            case SPRUCE -> spruce(builder, x, y, z, settings);
            case BIRCH -> birch(builder, x, y, z, settings);
            case PALM -> palm(builder, x, y, z, settings);
            case DEAD -> dead(builder, x, y, z, settings);
            case WILLOW -> willow(builder, x, y, z, settings);
            default -> oak(builder, x, y, z, settings);
        }
        builder.finish();
    }

    // ---- Tree types ----

    private static void oak(Builder b, double x, double y, double z, Settings s) {
        int h = s.height();
        double leanAngle = b.random.nextDouble() * Math.PI * 2;
        double lean = b.random.nextDouble() * 0.12 * h;
        double topX = x + Math.cos(leanAngle) * lean;
        double topY = y + h * 0.8;
        double topZ = z + Math.sin(leanAngle) * lean;
        b.trunk(x, y, z, topX, topY, topZ, s.trunkWidth());

        int branchCount = 2 + Math.round(s.branches() * 5);
        for (int i = 0; i < branchCount; i++) {
            double along = 0.5 + b.random.nextDouble() * 0.35;
            double startX = x + (topX - x) * along;
            double startY = y + (topY - y) * along;
            double startZ = z + (topZ - z) * along;
            double angle = i * Math.PI * 2 / branchCount + (b.random.nextDouble() - 0.5) * 0.9;
            double elevation = Math.toRadians(25 + b.random.nextDouble() * 30);
            double length = h * (0.25 + b.random.nextDouble() * 0.17) * s.crown();
            double endX = startX + Math.cos(angle) * Math.cos(elevation) * length;
            double endY = startY + Math.sin(elevation) * length;
            double endZ = startZ + Math.sin(angle) * Math.cos(elevation) * length;
            b.logLine(startX, startY, startZ, endX, endY, endZ, s.trunkWidth() >= 2 ? 0.8 : 0);
            double radius = (1.8 + h * 0.11) * s.crown() * (0.8 + b.random.nextDouble() * 0.4);
            b.leafBlob(endX, endY, endZ, radius, radius * 0.75, radius);
        }
        double radius = (2.2 + h * 0.16) * s.crown();
        b.leafBlob(topX, topY + 1, topZ, radius, radius * 0.8, radius);
    }

    private static void spruce(Builder b, double x, double y, double z, Settings s) {
        int h = s.height();
        double offset = (s.trunkWidth() - 1) / 2.0;
        double centerX = x + offset;
        double centerZ = z + offset;
        b.trunk(x, y, z, x, y + h, z, s.trunkWidth());

        int start = (int) Math.round(h * 0.2);
        double maxRadius = (1.2 + h * 0.22) * s.crown() + offset;
        for (int layer = start; layer <= h; layer++) {
            double rel = (layer - start) / (double) Math.max(1, h - start);
            double radius = (1 - rel) * maxRadius + 0.5;
            // Alternate wide and narrow layers for the typical stepped look
            if ((layer - start) % 2 == 1) {
                radius *= 0.7;
            }
            b.leafDisc(centerX, y + layer, centerZ, radius);
        }
        b.leaf(centerX, y + h + 1, centerZ);
        b.leaf(centerX, y + h + 2, centerZ);
    }

    private static void birch(Builder b, double x, double y, double z, Settings s) {
        int h = s.height();
        b.trunk(x, y, z, x, y + h, z, s.trunkWidth());
        double offset = (s.trunkWidth() - 1) / 2.0;
        double radius = (1.6 + h * 0.09) * s.crown() + offset;
        b.leafBlob(x + offset, y + h * 0.78, z + offset, radius, h * 0.32 * s.crown() + 1, radius);

        int branchCount = Math.round(s.branches() * 3);
        for (int i = 0; i < branchCount; i++) {
            double startY = y + h * (0.55 + b.random.nextDouble() * 0.25);
            double angle = b.random.nextDouble() * Math.PI * 2;
            double length = 1.5 + h * 0.08;
            double endX = x + Math.cos(angle) * length;
            double endZ = z + Math.sin(angle) * length;
            b.logLine(x, startY, z, endX, startY + length * 0.6, endZ, 0);
            b.leafBlob(endX, startY + length * 0.6, endZ, 1.6, 1.2, 1.6);
        }
    }

    private static void palm(Builder b, double x, double y, double z, Settings s) {
        int h = s.height();
        double leanAngle = b.random.nextDouble() * Math.PI * 2;
        double curve = h * 0.25 * (0.6 + b.random.nextDouble() * 0.6);
        double topX = x;
        double topZ = z;
        double previousX = x;
        double previousZ = z;
        for (int i = 0; i <= h; i++) {
            double rel = i / (double) h;
            double offset = curve * rel * rel;
            topX = x + Math.cos(leanAngle) * offset;
            topZ = z + Math.sin(leanAngle) * offset;
            b.trunk(previousX, y + Math.max(0, i - 1), previousZ, topX, y + i, topZ, s.trunkWidth());
            previousX = topX;
            previousZ = topZ;
        }
        double topY = y + h;

        int fronds = 5 + Math.round(s.branches() * 4);
        double length = (2.5 + h * 0.38) * s.crown();
        for (int i = 0; i < fronds; i++) {
            double angle = i * Math.PI * 2 / fronds + (b.random.nextDouble() - 0.5) * 0.5;
            double dirX = Math.cos(angle);
            double dirZ = Math.sin(angle);
            double steps = Math.max(4, length * 2.5);
            for (int step = 0; step <= steps; step++) {
                double t = step / steps;
                double px = topX + dirX * t * length;
                double py = topY + Math.sin(t * Math.PI * 0.8) * length * 0.25 - t * t * length * 0.45;
                double pz = topZ + dirZ * t * length;
                b.leaf(px, py, pz);
                if (t > 0.15 && t < 0.8) {
                    b.leaf(px - dirZ * 0.7, py, pz + dirX * 0.7);
                    b.leaf(px + dirZ * 0.7, py, pz - dirX * 0.7);
                }
            }
        }
        b.leafBlob(topX, topY + 0.5, topZ, 1.4, 0.9, 1.4);
    }

    private static void dead(Builder b, double x, double y, double z, Settings s) {
        int h = s.height();
        b.trunk(x, y, z, x, y + h * 0.85, z, s.trunkWidth());
        int branchCount = 2 + Math.round(s.branches() * 4);
        for (int i = 0; i < branchCount; i++) {
            double startY = y + h * (0.4 + b.random.nextDouble() * 0.45);
            double angle = i * Math.PI * 2 / branchCount + b.random.nextDouble() * 0.8;
            double dirX = Math.cos(angle);
            double dirY = 0.5 + b.random.nextDouble() * 0.5;
            double dirZ = Math.sin(angle);
            deadBranch(b, x, startY, z, dirX, dirY, dirZ, h * 0.35 * s.crown(), 2);
        }
    }

    private static void deadBranch(Builder b, double x, double y, double z, double dirX, double dirY, double dirZ,
                                   double length, int depth) {
        double norm = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX /= norm;
        dirY /= norm;
        dirZ /= norm;
        double endX = x + dirX * length;
        double endY = y + dirY * length;
        double endZ = z + dirZ * length;
        b.logLine(x, y, z, endX, endY, endZ, 0);
        if (depth <= 0 || length < 2) {
            return;
        }
        int children = 1 + b.random.nextInt(2);
        for (int i = 0; i < children; i++) {
            double childX = dirX + (b.random.nextDouble() - 0.5) * 1.2;
            double childY = dirY + 0.3 + (b.random.nextDouble() - 0.5) * 0.6;
            double childZ = dirZ + (b.random.nextDouble() - 0.5) * 1.2;
            deadBranch(b, endX, endY, endZ, childX, childY, childZ, length * 0.6, depth - 1);
        }
    }

    private static void willow(Builder b, double x, double y, double z, Settings s) {
        int h = s.height();
        double topY = y + h * 0.7;
        b.trunk(x, y, z, x, topY, z, s.trunkWidth());
        double radius = (2.5 + h * 0.2) * s.crown();
        b.leafBlob(x, topY + 1, z, radius, radius * 0.5, radius);

        // Hanging curtains of leaves around the crown
        int strands = 10 + Math.round(s.branches() * 20);
        for (int i = 0; i < strands; i++) {
            double angle = b.random.nextDouble() * Math.PI * 2;
            double distance = radius * (0.6 + b.random.nextDouble() * 0.4);
            double sx = x + Math.cos(angle) * distance;
            double sz = z + Math.sin(angle) * distance;
            double strandLength = h * (0.3 + b.random.nextDouble() * 0.3);
            for (int j = 0; j < strandLength; j++) {
                b.leaf(sx, topY + 1 - j, sz);
            }
        }
    }

    // ---- Block placement ----

    private static final class Builder {
        private final ChunkedBlockRegion region;
        private final BlockState log;
        private final BlockState leaves;
        private final Random random;
        private final LongOpenHashSet leafPositions = new LongOpenHashSet();

        private Builder(ChunkedBlockRegion region, WoodSet wood, Random random) {
            this.region = region;
            this.log = wood.log().defaultBlockState();
            BlockState leafState = wood.leaves().defaultBlockState();
            // Placed leaves must not decay
            if (leafState.hasProperty(BlockStateProperties.PERSISTENT)) {
                leafState = leafState.setValue(BlockStateProperties.PERSISTENT, true);
            }
            this.leaves = leafState;
            this.random = random;
        }

        private void log(int x, int y, int z, Direction.Axis axis) {
            BlockState state = this.log.hasProperty(BlockStateProperties.AXIS)
                ? this.log.setValue(BlockStateProperties.AXIS, axis)
                : this.log;
            this.region.addBlock(x, y, z, state);
        }

        /** A trunk of width x width parallel log lines, the corners are left out for width 3+. */
        private void trunk(double x0, double y0, double z0, double x1, double y1, double z1, int width) {
            for (int ox = 0; ox < width; ox++) {
                for (int oz = 0; oz < width; oz++) {
                    boolean corner = width >= 3 && (ox == 0 || ox == width - 1) && (oz == 0 || oz == width - 1);
                    if (!corner) {
                        this.logLine(x0 + ox, y0, z0 + oz, x1 + ox, y1, z1 + oz, 0);
                    }
                }
            }
        }

        private void logLine(double x0, double y0, double z0, double x1, double y1, double z1, double radius) {
            double dx = x1 - x0;
            double dy = y1 - y0;
            double dz = z1 - z0;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            Direction.Axis axis = Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) >= Math.abs(dz)
                ? Direction.Axis.Y
                : Math.abs(dx) >= Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
            int steps = Math.max(1, (int) Math.ceil(length * 2));
            int r = (int) Math.ceil(radius);
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                int px = (int) Math.floor(x0 + dx * t);
                int py = (int) Math.floor(y0 + dy * t);
                int pz = (int) Math.floor(z0 + dz * t);
                if (radius < 0.75) {
                    this.log(px, py, pz, axis);
                    continue;
                }
                for (int ox = -r; ox <= r; ox++) {
                    for (int oy = -r; oy <= r; oy++) {
                        for (int oz = -r; oz <= r; oz++) {
                            if (ox * ox + oy * oy + oz * oz <= radius * radius) {
                                this.log(px + ox, py + oy, pz + oz, axis);
                            }
                        }
                    }
                }
            }
        }

        private void leaf(double x, double y, double z) {
            this.leafPositions.add(BlockPos.asLong((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));
        }

        /** Ellipsoid of leaves with a slightly random edge. */
        private void leafBlob(double cx, double cy, double cz, double rx, double ry, double rz) {
            int ex = (int) Math.ceil(rx);
            int ey = (int) Math.ceil(ry);
            int ez = (int) Math.ceil(rz);
            int bx = (int) Math.floor(cx);
            int by = (int) Math.floor(cy);
            int bz = (int) Math.floor(cz);
            for (int x = -ex; x <= ex; x++) {
                for (int y = -ey; y <= ey; y++) {
                    for (int z = -ez; z <= ez; z++) {
                        double d = sq((x + 0.5 - (cx - bx)) / rx) + sq((y + 0.5 - (cy - by)) / ry) + sq((z + 0.5 - (cz - bz)) / rz);
                        if (d <= 1 - this.random.nextDouble() * 0.25) {
                            this.leafPositions.add(BlockPos.asLong(bx + x, by + y, bz + z));
                        }
                    }
                }
            }
        }

        private void leafDisc(double cx, double y, double cz, double radius) {
            int r = (int) Math.ceil(radius);
            int bx = (int) Math.floor(cx);
            int bz = (int) Math.floor(cz);
            int by = (int) Math.floor(y);
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    double distance = Math.sqrt(sq(x + 0.5 - (cx - bx)) + sq(z + 0.5 - (cz - bz)));
                    if (distance <= radius - this.random.nextDouble() * 0.35) {
                        this.leafPositions.add(BlockPos.asLong(bx + x, by, bz + z));
                    }
                }
            }
        }

        /** Leaves never replace logs. */
        private void finish() {
            LongIterator iterator = this.leafPositions.iterator();
            while (iterator.hasNext()) {
                long pos = iterator.nextLong();
                this.region.addBlockIfNotPresent(BlockPos.getX(pos), BlockPos.getY(pos), BlockPos.getZ(pos), this.leaves);
            }
        }

        private static double sq(double value) {
            return value * value;
        }
    }

}
