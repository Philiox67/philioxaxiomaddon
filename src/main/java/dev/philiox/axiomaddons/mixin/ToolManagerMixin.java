package dev.philiox.axiomaddons.mixin;

import com.moulberry.axiom.tools.ToolManager;
import dev.philiox.axiomaddons.tools.AddonTools;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ToolManager.class, remap = false)
public abstract class ToolManagerMixin {

    @Inject(method = "initializeTools", at = @At("TAIL"))
    private static void philiox$addTools(CallbackInfo ci) {
        AddonTools.register();
    }

}
