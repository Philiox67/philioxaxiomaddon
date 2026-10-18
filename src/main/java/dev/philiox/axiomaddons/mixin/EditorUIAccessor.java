package dev.philiox.axiomaddons.mixin;

import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.palette.ActiveBlockHistory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Axiom only exposes a getter for the active block, the palette needs to set it.
 */
@Mixin(value = EditorUI.class, remap = false)
public interface EditorUIAccessor {

    @Accessor("activeBlockHistory")
    static ActiveBlockHistory philiox$getActiveBlockHistory() {
        throw new AssertionError();
    }

}
