package dev.philiox.axiomaddons.mixin;

import com.moulberry.axiom.editor.windows.operations.AutoshadeWindow;
import dev.philiox.axiomaddons.tools.BlockFilter;
import imgui.moulberry92.ImGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shows the addon's block filter at the bottom of Axiom's Autoshade window.
 */
@Mixin(value = AutoshadeWindow.class, remap = false)
public abstract class AutoshadeWindowMixin {

    @Inject(method = "render",
        at = @At(value = "INVOKE", target = "Lcom/moulberry/axiom/editor/EditorWindowType;end()V", ordinal = 0))
    private static void philiox$showBlockFilter(CallbackInfo ci) {
        if (!ImGui.isWindowCollapsed()) {
            ImGui.separator();
            BlockFilter.displayOptions();
        }
    }

}
