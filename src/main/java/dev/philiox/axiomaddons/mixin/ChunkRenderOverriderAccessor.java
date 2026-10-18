package dev.philiox.axiomaddons.mixin;

import com.moulberry.axiom.render.ChunkRenderOverrider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Axiom's render overrider can only be held by one tool at a time and throws otherwise,
 * so the ruin preview checks who holds it first.
 */
@Mixin(value = ChunkRenderOverrider.class, remap = false)
public interface ChunkRenderOverriderAccessor {

    @Accessor("lockedBy")
    static String philiox$getLockedBy() {
        throw new AssertionError();
    }

}
