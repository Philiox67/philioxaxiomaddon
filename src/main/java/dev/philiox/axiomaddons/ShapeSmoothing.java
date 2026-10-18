package dev.philiox.axiomaddons;

import com.moulberry.axiom.block_maps.HDVoxelMap;
import net.minecraft.core.Direction;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Turns the blocks at the edge of a shape into stairs, slabs and walls, based on which of the eight
 * half-block octants of a block lie inside the shape.
 *
 * Octant bit index: dx | dy << 1 | dz << 2, where d is 0 for the lower and 1 for the upper half of the axis.
 */
public final class ShapeSmoothing {

    public record Variants(BlockState full, @Nullable Block stairs, @Nullable Block slab, @Nullable Block wall) {
    }

    private static final int BOTTOM_LAYER = 0b0011_0011;
    private static final int TOP_LAYER = 0b1100_1100;

    private static final Map<Block, Optional<Variants>> CACHE = new HashMap<>();

    private ShapeSmoothing() {
    }

    /** Stairs, slab and wall belonging to the block, or null if it has neither stairs nor slab. */
    public static @Nullable Variants variantsFor(BlockState state) {
        Block block = state.getBlock();
        return CACHE.computeIfAbsent(block, key -> Optional.ofNullable(findVariants(state))).orElse(null);
    }

    private static @Nullable Variants findVariants(BlockState state) {
        Block block = state.getBlock();
        Block stairs = null;
        Block slab = null;
        Block wall = null;
        BlockFamily family = BlockFamilies.getAllFamilies()
            .filter(candidate -> candidate.getBaseBlock() == block)
            .findFirst().orElse(null);
        if (family != null) {
            stairs = family.get(BlockFamily.Variant.STAIRS);
            slab = family.get(BlockFamily.Variant.SLAB);
            wall = family.get(BlockFamily.Variant.WALL);
        }
        if (stairs == null || slab == null) {
            HDVoxelMap.HDVoxelBaseBlocks base = HDVoxelMap.getAssociatedBlocks(block);
            if (base != null) {
                stairs = stairs != null ? stairs : base.stair();
                slab = slab != null ? slab : base.slab();
            }
        }
        if (stairs == null && slab == null) {
            return null;
        }
        return new Variants(state, stairs, slab, wall);
    }

    public static int bit(int dx, int dy, int dz) {
        return 1 << (dx | dy << 1 | dz << 2);
    }

    /**
     * @param mask which octants are inside the shape
     * @return the block for that mask, or null for air
     */
    public static @Nullable BlockState forMask(int mask, Variants variants) {
        int count = Integer.bitCount(mask);
        if (count >= 7) {
            return variants.full();
        }
        if (count == 0) {
            return null;
        }
        int bottom = mask & BOTTOM_LAYER;
        int top = mask & TOP_LAYER;
        int bottomCount = Integer.bitCount(bottom);
        int topCount = Integer.bitCount(top);

        // A thin upright column: one octant in each layer, right above each other
        if (count == 2 && bottomCount == 1 && topCount == 1 && top == bottom << 2) {
            return variants.wall() != null ? variants.wall().defaultBlockState() : null;
        }
        if (count == 1) {
            return null;
        }
        if (bottomCount == 4 && topCount == 0 || bottomCount == 4 && topCount == 1) {
            return slab(variants, SlabType.BOTTOM);
        }
        if (topCount == 4 && bottomCount == 0 || topCount == 4 && bottomCount == 1) {
            return slab(variants, SlabType.TOP);
        }
        if (bottomCount == 4 && topCount == 2) {
            // The top layer sits two bits above the bottom layer
            Direction side = side(top >> 2);
            if (side != null) {
                return stairs(variants, side, Half.BOTTOM);
            }
        }
        if (topCount == 4 && bottomCount == 2) {
            Direction side = side(bottom);
            if (side != null) {
                return stairs(variants, side, Half.TOP);
            }
        }
        if (count >= 4) {
            return variants.full();
        }
        if (topCount == 0) {
            return slab(variants, SlabType.BOTTOM);
        }
        if (bottomCount == 0) {
            return slab(variants, SlabType.TOP);
        }
        return null;
    }

    /** Which side of the block two octants of one layer (given as bottom layer bits) are on. */
    private static @Nullable Direction side(int layer) {
        int east = bit(1, 0, 0) | bit(1, 0, 1);
        int west = bit(0, 0, 0) | bit(0, 0, 1);
        int south = bit(0, 0, 1) | bit(1, 0, 1);
        int north = bit(0, 0, 0) | bit(1, 0, 0);
        if (layer == east) {
            return Direction.EAST;
        }
        if (layer == west) {
            return Direction.WEST;
        }
        if (layer == south) {
            return Direction.SOUTH;
        }
        if (layer == north) {
            return Direction.NORTH;
        }
        return null;
    }

    private static BlockState slab(Variants variants, SlabType type) {
        if (variants.slab() == null) {
            return variants.full();
        }
        BlockState state = variants.slab().defaultBlockState();
        return state.hasProperty(BlockStateProperties.SLAB_TYPE) ? state.setValue(BlockStateProperties.SLAB_TYPE, type) : state;
    }

    private static BlockState stairs(Variants variants, Direction facing, Half half) {
        if (variants.stairs() == null) {
            return variants.full();
        }
        BlockState state = variants.stairs().defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        }
        if (state.hasProperty(BlockStateProperties.HALF)) {
            state = state.setValue(BlockStateProperties.HALF, half);
        }
        return state;
    }

}
