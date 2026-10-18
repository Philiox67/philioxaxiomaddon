package dev.philiox.axiomaddons.tools;

import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Builds a detailed sailing ship out of planks, stairs, slabs, logs, fences, fence gates, trapdoors, doors,
 * glass panes, wool and lanterns.
 *
 * Local space: the stern is at x = 0, the bow points to +X, the keel bottom is at y = 0, z = 0 is the center line.
 */
final class ShipBuilder {

    record Wood(String key, Block planks, Block stairs, Block slab, Block fence, Block gate, Block log,
                Block stripped, Block trapdoor, Block door) {
    }

    static final Wood[] WOODS = {
        new Wood("oak", Blocks.OAK_PLANKS, Blocks.OAK_STAIRS, Blocks.OAK_SLAB, Blocks.OAK_FENCE, Blocks.OAK_FENCE_GATE,
            Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG, Blocks.OAK_TRAPDOOR, Blocks.OAK_DOOR),
        new Wood("spruce", Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_FENCE_GATE,
            Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_TRAPDOOR, Blocks.SPRUCE_DOOR),
        new Wood("dark_oak", Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_FENCE_GATE,
            Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DARK_OAK_TRAPDOOR, Blocks.DARK_OAK_DOOR),
        new Wood("birch", Blocks.BIRCH_PLANKS, Blocks.BIRCH_STAIRS, Blocks.BIRCH_SLAB, Blocks.BIRCH_FENCE, Blocks.BIRCH_FENCE_GATE,
            Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG, Blocks.BIRCH_TRAPDOOR, Blocks.BIRCH_DOOR),
        new Wood("jungle", Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_SLAB, Blocks.JUNGLE_FENCE, Blocks.JUNGLE_FENCE_GATE,
            Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG, Blocks.JUNGLE_TRAPDOOR, Blocks.JUNGLE_DOOR),
        new Wood("acacia", Blocks.ACACIA_PLANKS, Blocks.ACACIA_STAIRS, Blocks.ACACIA_SLAB, Blocks.ACACIA_FENCE, Blocks.ACACIA_FENCE_GATE,
            Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG, Blocks.ACACIA_TRAPDOOR, Blocks.ACACIA_DOOR),
        new Wood("mangrove", Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_STAIRS, Blocks.MANGROVE_SLAB, Blocks.MANGROVE_FENCE, Blocks.MANGROVE_FENCE_GATE,
            Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG, Blocks.MANGROVE_TRAPDOOR, Blocks.MANGROVE_DOOR),
        new Wood("cherry", Blocks.CHERRY_PLANKS, Blocks.CHERRY_STAIRS, Blocks.CHERRY_SLAB, Blocks.CHERRY_FENCE, Blocks.CHERRY_FENCE_GATE,
            Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG, Blocks.CHERRY_TRAPDOOR, Blocks.CHERRY_DOOR),
        new Wood("pale_oak", Blocks.PALE_OAK_PLANKS, Blocks.PALE_OAK_STAIRS, Blocks.PALE_OAK_SLAB, Blocks.PALE_OAK_FENCE, Blocks.PALE_OAK_FENCE_GATE,
            Blocks.PALE_OAK_LOG, Blocks.STRIPPED_PALE_OAK_LOG, Blocks.PALE_OAK_TRAPDOOR, Blocks.PALE_OAK_DOOR),
        new Wood("bamboo", Blocks.BAMBOO_PLANKS, Blocks.BAMBOO_STAIRS, Blocks.BAMBOO_SLAB, Blocks.BAMBOO_FENCE, Blocks.BAMBOO_FENCE_GATE,
            Blocks.BAMBOO_BLOCK, Blocks.STRIPPED_BAMBOO_BLOCK, Blocks.BAMBOO_TRAPDOOR, Blocks.BAMBOO_DOOR),
        new Wood("crimson", Blocks.CRIMSON_PLANKS, Blocks.CRIMSON_STAIRS, Blocks.CRIMSON_SLAB, Blocks.CRIMSON_FENCE, Blocks.CRIMSON_FENCE_GATE,
            Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM, Blocks.CRIMSON_TRAPDOOR, Blocks.CRIMSON_DOOR),
        new Wood("warped", Blocks.WARPED_PLANKS, Blocks.WARPED_STAIRS, Blocks.WARPED_SLAB, Blocks.WARPED_FENCE, Blocks.WARPED_FENCE_GATE,
            Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM, Blocks.WARPED_TRAPDOOR, Blocks.WARPED_DOOR)
    };

    static final String[] SAIL_KEYS = {"white", "light_gray", "red", "black", "brown", "yellow", "blue", "green", "orange"};
    static final Block[] SAIL_COLOURS = {Blocks.WHITE_WOOL, Blocks.LIGHT_GRAY_WOOL, Blocks.RED_WOOL, Blocks.BLACK_WOOL,
        Blocks.BROWN_WOOL, Blocks.YELLOW_WOOL, Blocks.BLUE_WOOL, Blocks.GREEN_WOOL, Blocks.ORANGE_WOOL};

    record Settings(int length, int width, int depth, int masts, int mastHeight, int sailTiers, float sailWidth,
                    boolean billow, int sailColour, boolean stripes, int bowsprit, boolean jib, int sternStories,
                    int storyHeight, float sternLength, boolean forecastle, boolean windows, boolean cannonPorts,
                    boolean lanterns, boolean crowsNest, boolean darkBottom, boolean clearInside, Wood wood) {
    }

    private static final double BOW_START = 0.72;

    private final Settings s;
    private final Long2ObjectOpenHashMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();
    private final int length;
    private final int depth;
    private final int halfWidthMax;
    private final BlockState air = Blocks.AIR.defaultBlockState();

    private ShipBuilder(Settings settings) {
        this.s = settings;
        this.length = Math.max(9, settings.length());
        this.depth = Math.max(3, settings.depth());
        this.halfWidthMax = Math.max(2, (settings.width() - 1) / 2);
    }

    /**
     * Builds the ship into the region, centered on the origin, bow pointing in the rotated +X direction.
     * The keel bottom lies at y = 0 of the region.
     */
    static void build(Settings settings, Rotation rotation, ChunkedBlockRegion out) {
        ShipBuilder builder = new ShipBuilder(settings);
        builder.generate();
        builder.connectFences();
        int centerX = builder.length / 2;
        for (Long2ObjectMap.Entry<BlockState> entry : builder.blocks.long2ObjectEntrySet()) {
            long key = entry.getLongKey();
            int x = BlockPos.getX(key) - centerX;
            int y = BlockPos.getY(key);
            int z = BlockPos.getZ(key);
            int rx = switch (rotation) {
                case CLOCKWISE_90 -> -z;
                case CLOCKWISE_180 -> -x;
                case COUNTERCLOCKWISE_90 -> z;
                default -> x;
            };
            int rz = switch (rotation) {
                case CLOCKWISE_90 -> x;
                case CLOCKWISE_180 -> -z;
                case COUNTERCLOCKWISE_90 -> -x;
                default -> z;
            };
            out.addBlock(rx, y, rz, entry.getValue().rotate(rotation));
        }
    }

    // ---- Hull geometry ----

    private double deckHalfWidth(int x) {
        double t = x / (double) (this.length - 1);
        if (t > BOW_START) {
            double u = (t - BOW_START) / (1 - BOW_START);
            return this.halfWidthMax * (1 - Math.pow(u, 1.7));
        }
        if (t < 0.12) {
            double u = (0.12 - t) / 0.12;
            return this.halfWidthMax * (1 - 0.3 * u * u);
        }
        return this.halfWidthMax;
    }

    private int keel(int x) {
        double t = x / (double) (this.length - 1);
        if (t <= BOW_START) {
            return 0;
        }
        double u = (t - BOW_START) / (1 - BOW_START);
        return (int) Math.round(this.depth * 0.6 * u * u);
    }

    /** Hull volume including the deck layer at y = depth. */
    private boolean hull(int x, int y, int z) {
        if (x < 0 || x >= this.length || y > this.depth) {
            return false;
        }
        int keel = this.keel(x);
        if (y < keel) {
            return false;
        }
        double h = (y + 0.5 - keel) / (this.depth + 1 - keel);
        double halfWidth = this.deckHalfWidth(x) * Math.sqrt(Math.max(0, 1 - (1 - h) * (1 - h)));
        return Math.abs(z) <= halfWidth + 0.35;
    }

    private boolean deck(int x, int z) {
        return this.hull(x, this.depth, z);
    }

    private boolean deckEdge(int x, int z) {
        return this.deck(x, z) && (!this.deck(x + 1, z) || !this.deck(x - 1, z) || !this.deck(x, z + 1) || !this.deck(x, z - 1));
    }

    // ---- Generation ----

    private void set(int x, int y, int z, BlockState state) {
        this.blocks.put(BlockPos.asLong(x, y, z), state);
    }

    private BlockState get(int x, int y, int z) {
        return this.blocks.get(BlockPos.asLong(x, y, z));
    }

    private static BlockState axis(Block block, Direction.Axis axis) {
        BlockState state = block.defaultBlockState();
        return state.hasProperty(BlockStateProperties.AXIS) ? state.setValue(BlockStateProperties.AXIS, axis) : state;
    }

    private static BlockState stairs(Block block, Direction facing, Half half) {
        return block.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
            .setValue(BlockStateProperties.HALF, half);
    }

    private void generate() {
        Wood wood = this.s.wood();
        BlockState planks = wood.planks().defaultBlockState();
        BlockState darkPlanks = (wood.planks() == Blocks.DARK_OAK_PLANKS ? Blocks.SPRUCE_PLANKS : Blocks.DARK_OAK_PLANKS).defaultBlockState();
        int deckY = this.depth;
        int waterline = (int) Math.round(this.depth * 0.45);

        // Hull shell, the inside stays empty (and is cleared of water)
        for (int x = 0; x < this.length; x++) {
            for (int y = 0; y < deckY; y++) {
                for (int z = -this.halfWidthMax; z <= this.halfWidthMax; z++) {
                    if (!this.hull(x, y, z)) {
                        continue;
                    }
                    boolean shell = !this.hull(x + 1, y, z) || !this.hull(x - 1, y, z) || !this.hull(x, y - 1, z)
                        || !this.hull(x, y, z + 1) || !this.hull(x, y, z - 1);
                    if (!shell) {
                        if (this.s.clearInside()) {
                            this.set(x, y, z, this.air);
                        }
                        continue;
                    }
                    BlockState state = this.s.darkBottom() && y < waterline ? darkPlanks : planks;
                    int outward = z > 0 ? 1 : -1;
                    boolean side = z != 0 && !this.hull(x, y, z + outward);
                    // Round the bilge: where the hull below is narrower, an upside down stair rounds the corner off
                    if (side && y > this.keel(x) && !this.hull(x, y - 1, z) && this.hull(x, y, z - outward)) {
                        state = stairs(wood.stairs(), outward > 0 ? Direction.NORTH : Direction.SOUTH, Half.TOP);
                    }
                    // Wale: a log band below the deck
                    if (side && y == deckY - 1) {
                        state = axis(wood.stripped(), Direction.Axis.X);
                    }
                    this.set(x, y, z, state);
                }
            }
        }

        // Keel, stem post at the bow and stern post
        int bowTip = this.length - 1;
        while (bowTip > 0 && !this.deck(bowTip, 0)) {
            bowTip--;
        }
        for (int x = 0; x < this.length; x++) {
            if (this.hull(x, this.keel(x), 0)) {
                this.set(x, this.keel(x), 0, axis(wood.log(), Direction.Axis.X));
            }
        }
        for (int y = this.keel(bowTip); y <= deckY + 1; y++) {
            this.set(bowTip, y, 0, axis(wood.log(), Direction.Axis.Y));
        }
        for (int y = 0; y <= deckY + 1; y++) {
            this.set(0, y, 0, axis(wood.log(), Direction.Axis.Y));
        }

        // Deck and railing
        for (int x = 0; x < this.length; x++) {
            for (int z = -this.halfWidthMax; z <= this.halfWidthMax; z++) {
                if (!this.deck(x, z)) {
                    continue;
                }
                this.set(x, deckY, z, planks);
                if (this.deckEdge(x, z) && !(x == bowTip && z == 0) && !(x == 0 && z == 0)) {
                    this.set(x, deckY + 1, z, wood.fence().defaultBlockState());
                } else if (this.s.clearInside()) {
                    this.set(x, deckY + 1, z, this.air);
                }
            }
        }
        // Boarding gates in the middle of both sides
        int middle = this.length / 2;
        for (int side = -1; side <= 1; side += 2) {
            int z = side * this.halfWidthMax;
            while (z != 0 && !this.deck(middle, z)) {
                z -= side;
            }
            if (z != 0) {
                this.set(middle, deckY + 1, z, wood.gate().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
            }
        }

        int sternEnd = this.s.sternStories() > 0 ? this.buildSternCastle(planks) : 0;
        int foreStart = this.s.forecastle() ? this.buildForecastle(planks) : this.length;
        if (this.s.cannonPorts()) {
            this.buildCannonPorts(sternEnd + 2, foreStart - 2);
        }
        this.buildRigging(bowTip);
    }

    private int buildSternCastle(BlockState planks) {
        Wood wood = this.s.wood();
        int deckY = this.depth;
        int storyHeight = Math.max(3, this.s.storyHeight());
        int castleLength = Math.max(3, (int) Math.round(this.length * this.s.sternLength()));
        int stories = this.s.sternStories();
        int roofY = deckY + stories * storyHeight;

        for (int level = 0; level < stories; level++) {
            int end = Math.max(2, (int) Math.round(castleLength * (1 - 0.12 * level)) - 1);
            double shrink = level * 0.5;
            int base = deckY + level * storyHeight;
            for (int x = 0; x <= end; x++) {
                for (int z = -this.halfWidthMax; z <= this.halfWidthMax; z++) {
                    if (!this.inCastle(x, z, end, shrink)) {
                        continue;
                    }
                    boolean wallX = !this.inCastle(x + 1, z, end, shrink) || !this.inCastle(x - 1, z, end, shrink);
                    boolean wallZ = !this.inCastle(x, z + 1, end, shrink) || !this.inCastle(x, z - 1, end, shrink);
                    for (int y = base + 1; y < base + storyHeight; y++) {
                        BlockState state;
                        if (wallX && wallZ) {
                            state = axis(wood.log(), Direction.Axis.Y);
                        } else if (wallX || wallZ) {
                            boolean window = this.s.windows() && y == base + 2 && Math.floorMod(x + z, 2) == 0;
                            state = window ? Blocks.GLASS_PANE.defaultBlockState() : planks;
                        } else {
                            state = this.air;
                        }
                        this.set(x, y, z, state);
                    }
                    // Floor of the next story, or the roof
                    this.set(x, base + storyHeight, z, planks);
                }
            }
            if (level == 0) {
                // Door in the front wall towards the bow
                this.set(end, base + 1, 0, wood.door().defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                    .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                    .setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT));
                this.set(end, base + 2, 0, wood.door().defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                    .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)
                    .setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT));
            }
        }

        // Roof railing with lanterns on the corners
        int topEnd = Math.max(2, (int) Math.round(castleLength * (1 - 0.12 * (stories - 1))) - 1);
        double topShrink = (stories - 1) * 0.5;
        for (int x = 0; x <= topEnd; x++) {
            for (int z = -this.halfWidthMax; z <= this.halfWidthMax; z++) {
                if (!this.inCastle(x, z, topEnd, topShrink)) {
                    continue;
                }
                boolean wallX = !this.inCastle(x + 1, z, topEnd, topShrink) || !this.inCastle(x - 1, z, topEnd, topShrink);
                boolean wallZ = !this.inCastle(x, z + 1, topEnd, topShrink) || !this.inCastle(x, z - 1, topEnd, topShrink);
                if (wallX || wallZ) {
                    this.set(x, roofY + 1, z, wood.fence().defaultBlockState());
                    if (this.s.lanterns() && wallX && wallZ) {
                        this.set(x, roofY + 2, z, Blocks.LANTERN.defaultBlockState());
                    }
                }
            }
        }
        return castleLength;
    }

    private boolean inCastle(int x, int z, int end, double shrink) {
        return x >= 0 && x <= end && this.deck(x, z) && Math.abs(z) <= this.deckHalfWidth(x) - shrink + 0.35;
    }

    private int buildForecastle(BlockState planks) {
        Wood wood = this.s.wood();
        int deckY = this.depth;
        int start = (int) Math.round(this.length * 0.8);
        int raisedY = deckY + 3;
        for (int x = start; x < this.length; x++) {
            for (int z = -this.halfWidthMax; z <= this.halfWidthMax; z++) {
                if (!this.deck(x, z)) {
                    continue;
                }
                boolean edge = this.deckEdge(x, z) || x == start;
                for (int y = deckY + 1; y < raisedY; y++) {
                    this.set(x, y, z, edge ? planks : this.air);
                }
                this.set(x, raisedY, z, planks);
                boolean stairOpening = x == start && Math.abs(z) <= 1;
                boolean railing = this.deckEdge(x, z) || (x == start && !stairOpening);
                if (railing) {
                    this.set(x, raisedY + 1, z, wood.fence().defaultBlockState());
                }
            }
        }
        // Stairs from the main deck up to the forecastle
        for (int z = -1; z <= 1; z++) {
            for (int step = 1; step <= 3; step++) {
                int x = start - 4 + step;
                if (this.deck(x, z)) {
                    this.set(x, deckY + step, z, stairs(wood.stairs(), Direction.EAST, Half.BOTTOM));
                    for (int below = deckY + 1; below < deckY + step; below++) {
                        this.set(x, below, z, planks);
                    }
                }
            }
        }
        return start;
    }

    private void buildCannonPorts(int fromX, int toX) {
        Wood wood = this.s.wood();
        int y = this.depth - 2;
        if (y <= this.keel(this.length / 2)) {
            return;
        }
        for (int x = fromX; x <= toX; x += 4) {
            for (int side = -1; side <= 1; side += 2) {
                int z = side * this.halfWidthMax;
                while (z != 0 && !this.hull(x, y, z)) {
                    z -= side;
                }
                if (z == 0) {
                    continue;
                }
                // Open trapdoor as port hatch on the outside of the hull wall
                this.set(x, y, z, wood.trapdoor().defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, side > 0 ? Direction.NORTH : Direction.SOUTH)
                    .setValue(BlockStateProperties.OPEN, true)
                    .setValue(BlockStateProperties.HALF, Half.TOP));
            }
        }
    }

    private void buildRigging(int bowTip) {
        Wood wood = this.s.wood();
        int deckY = this.depth;
        int count = Math.clamp(this.s.masts(), 0, 3);
        double[] positions = switch (count) {
            case 1 -> new double[] {0.5};
            case 2 -> new double[] {0.36, 0.64};
            case 3 -> new double[] {0.3, 0.52, 0.72};
            default -> new double[0];
        };
        double[] heights = switch (count) {
            case 2 -> new double[] {0.88, 1.0};
            case 3 -> new double[] {0.8, 1.0, 0.88};
            default -> new double[] {1.0};
        };
        int mainIndex = count == 3 ? 1 : count - 1;
        BlockState sail = SAIL_COLOURS[Math.clamp(this.s.sailColour(), 0, SAIL_COLOURS.length - 1)].defaultBlockState();
        BlockState stripe = (sail.is(Blocks.RED_WOOL) ? Blocks.WHITE_WOOL : Blocks.RED_WOOL).defaultBlockState();

        int foreX = -1;
        int foreTop = 0;
        for (int i = 0; i < count; i++) {
            int mastX = (int) Math.round(this.length * positions[i]);
            int height = Math.max(5, (int) Math.round(this.s.mastHeight() * heights[i]));
            int top = deckY + height;
            for (int y = 1; y <= top; y++) {
                this.set(mastX, y, 0, axis(wood.log(), Direction.Axis.Y));
            }
            this.set(mastX, top + 1, 0, wood.fence().defaultBlockState());
            if (mastX > foreX) {
                foreX = mastX;
                foreTop = top;
            }

            int sailTop = top - 2;
            if (this.s.crowsNest() && i == mainIndex) {
                int nestY = top - 3;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) {
                            continue;
                        }
                        this.set(mastX + dx, nestY, dz, wood.slab().defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP));
                        this.set(mastX + dx, nestY + 1, dz, wood.fence().defaultBlockState());
                    }
                }
                sailTop = nestY - 1;
            }

            int sailBottom = deckY + 3;
            int tiers = Math.max(1, this.s.sailTiers());
            int tierHeight = (sailTop - sailBottom + 1) / tiers;
            if (tierHeight < 2) {
                continue;
            }
            for (int tier = 0; tier < tiers; tier++) {
                int bottom = sailBottom + tier * tierHeight;
                int yard = bottom + tierHeight - 1;
                int halfWidth = Math.max(1, (int) Math.round(this.halfWidthMax * this.s.sailWidth() * (1 - 0.15 * tier)));
                // Yard: a fence spar across the mast on top of the sail
                for (int z = -halfWidth - 1; z <= halfWidth + 1; z++) {
                    if (z != 0) {
                        this.set(mastX, yard, z, wood.fence().defaultBlockState());
                    }
                }
                int rows = yard - bottom;
                for (int y = bottom; y < yard; y++) {
                    for (int z = -halfWidth; z <= halfWidth; z++) {
                        BlockState cloth = this.s.stripes() && Math.floorMod(z, 4) < 2 ? stripe : sail;
                        this.set(mastX + 1, y, z, cloth);
                        // The sail billows forward in the middle
                        boolean belly = Math.abs(z) <= halfWidth * 0.6 && y - bottom >= rows * 0.25 && y - bottom < rows * 0.8;
                        if (this.s.billow() && belly) {
                            this.set(mastX + 2, y, z, cloth);
                        }
                    }
                }
            }
        }

        // Bowsprit, rising one block every three
        int bowsprit = Math.max(0, this.s.bowsprit());
        int tipX = bowTip;
        int tipY = deckY + 1;
        for (int i = 1; i <= bowsprit; i++) {
            tipX = bowTip + i;
            tipY = deckY + 1 + i / 3;
            this.set(tipX, tipY, 0, axis(wood.log(), Direction.Axis.X));
        }
        if (bowsprit > 0) {
            this.set(tipX + 1, tipY, 0, wood.fence().defaultBlockState());
        }

        // Jib: triangle sail between bowsprit and the frontmost mast
        if (this.s.jib() && bowsprit > 0 && foreX >= 0) {
            int jibTop = deckY + (int) Math.round((foreTop - deckY) * 0.8);
            int startX = foreX + 3;
            for (int y = tipY + 1; y <= jibTop; y++) {
                double t = (y - tipY) / (double) Math.max(1, jibTop - tipY);
                int endX = (int) Math.round(tipX - t * (tipX - startX));
                for (int x = startX; x <= endX; x++) {
                    if (this.get(x, y, 0) == null || this.get(x, y, 0).isAir()) {
                        this.set(x, y, 0, sail);
                    }
                }
            }
        }
    }

    // ---- Connections ----

    /** Connects fences, gates and glass panes to their neighbours, like Minecraft would when placing them. */
    private void connectFences() {
        Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        BooleanProperty[] properties = {BlockStateProperties.NORTH, BlockStateProperties.EAST,
            BlockStateProperties.SOUTH, BlockStateProperties.WEST};
        for (Long2ObjectMap.Entry<BlockState> entry : this.blocks.long2ObjectEntrySet()) {
            BlockState state = entry.getValue();
            if (!(state.getBlock() instanceof CrossCollisionBlock)) {
                continue;
            }
            long key = entry.getLongKey();
            int x = BlockPos.getX(key);
            int y = BlockPos.getY(key);
            int z = BlockPos.getZ(key);
            BlockState connected = state;
            for (int i = 0; i < directions.length; i++) {
                BlockState neighbour = this.get(x + directions[i].getStepX(), y, z + directions[i].getStepZ());
                boolean connect = neighbour != null && (neighbour.getBlock() instanceof CrossCollisionBlock
                    || neighbour.getBlock() instanceof FenceGateBlock || neighbour.isSolidRender());
                connected = connected.setValue(properties[i], connect);
            }
            this.blocks.put(key, connected);
        }
    }

}
