package dev.philiox.axiomaddons.mixin;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.editor.styles.StyleHelper;
import com.moulberry.axiom.editor.styles.StyleManager;
import dev.philiox.axiomaddons.PhilioxConfig;
import dev.philiox.axiomaddons.PhilioxTheme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Switches Axiom to "Philiox Modern" until Axiom has saved it once. After that the saved theme is respected,
 * so switching back to ImGui Dark in the style editor sticks.
 */
@Mixin(value = StyleManager.class, remap = false)
public abstract class StyleManagerMixin {

    @Inject(method = "initialize", at = @At("TAIL"))
    private static void philiox$applyModernTheme(CallbackInfo ci) {
        int modern = PhilioxTheme.index();
        if (modern < 0) {
            return;
        }
        if (StyleManager.getBaseStyleIndex() == modern) {
            // Axiom loaded our theme from its own config, from now on it is the user's choice
            if (!PhilioxConfig.isModernThemeApplied()) {
                PhilioxConfig.setModernThemeApplied();
            }
            return;
        }
        if (PhilioxConfig.isModernThemeApplied()) {
            return;
        }

        StyleManager.resetToBaseStyle(modern);
        StyleManager.getCustomThemeName().set("");
        // Axiom writes this into its config on the next save
        StyleHelper.Theme theme = StyleManager.createTheme();
        if (theme != null && Axiom.configuration != null) {
            Axiom.configuration.internal.savedCustomTheme = theme.convertToBase64();
        }
    }

}
