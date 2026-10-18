package dev.philiox.axiomaddons.mixin;

import com.moulberry.axiom.editor.styles.BuiltinStyles;
import dev.philiox.axiomaddons.PhilioxTheme;
import imgui.moulberry92.ImGuiStyle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Registers "Philiox Modern" as a third base style, selectable in Axiom's style editor and saved like the others.
 */
@Mixin(value = BuiltinStyles.class, remap = false)
public abstract class BuiltinStylesMixin {

    @Shadow @Final @Mutable
    public static ImGuiStyle[] BASE_STYLES;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void philiox$addModernStyle(CallbackInfo ci) {
        BASE_STYLES = PhilioxTheme.appendTo(BASE_STYLES);
    }

    @Inject(method = "getBaseStyleNames", at = @At("RETURN"), cancellable = true)
    private static void philiox$addModernStyleName(CallbackInfoReturnable<String[]> cir) {
        cir.setReturnValue(PhilioxTheme.appendName(cir.getReturnValue()));
    }

    @Inject(method = "getStringId", at = @At("HEAD"), cancellable = true)
    private static void philiox$modernStyleId(int index, CallbackInfoReturnable<String> cir) {
        if (index >= 0 && index == PhilioxTheme.index()) {
            cir.setReturnValue(PhilioxTheme.ID);
        }
    }

    @Inject(method = "getIndexForStringId", at = @At("HEAD"), cancellable = true)
    private static void philiox$modernStyleIndex(String id, CallbackInfoReturnable<Integer> cir) {
        if (PhilioxTheme.ID.equals(id) && PhilioxTheme.index() >= 0) {
            cir.setReturnValue(PhilioxTheme.index());
        }
    }

}
