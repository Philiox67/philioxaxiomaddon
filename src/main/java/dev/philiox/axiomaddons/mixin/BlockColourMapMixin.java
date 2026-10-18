package dev.philiox.axiomaddons.mixin;

import com.moulberry.axiom.block_maps.BlockColourMap;
import dev.philiox.axiomaddons.PhilioxConfig;
import dev.philiox.axiomaddons.tools.BlockFilter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Applies the addon's block filter (custom blocks, blacklist) to Axiom's colour lookups, which Autoshade and
 * other Axiom colour features use.
 */
@Mixin(value = BlockColourMap.class, remap = false)
public abstract class BlockColourMapMixin {

    @Unique
    private static final int CANDIDATES = 48;

    @Inject(method = "getNearestLabN", at = @At("RETURN"), cancellable = true)
    private static void philiox$filterNearestN(double l, double a, double b, int count, int flags,
                                               CallbackInfoReturnable<List<BlockState>> cir) {
        if (PhilioxConfig.isFilterAxiom() && cir.getReturnValue() != null) {
            cir.setReturnValue(BlockFilter.allowed(cir.getReturnValue()));
        }
    }

    @Inject(method = "getNearestLab", at = @At("HEAD"), cancellable = true)
    private static void philiox$filterNearest(double l, double a, double b, int flags, CallbackInfoReturnable<BlockState> cir) {
        if (!PhilioxConfig.isFilterAxiom()) {
            return;
        }
        // Already filtered by the injection above. Usually the very best match is allowed, so try that first.
        BlockState best = BlockFilter.firstAllowed(BlockColourMap.getNearestLabN(l, a, b, 1, flags));
        if (best == null) {
            best = BlockFilter.firstAllowed(BlockColourMap.getNearestLabN(l, a, b, CANDIDATES, flags));
        }
        if (best != null) {
            cir.setReturnValue(best);
        }
    }

}
