package dev.philiox.axiomaddons;

import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiStyle;
import imgui.moulberry92.flag.ImGuiCol;

import java.util.Arrays;

/**
 * "Philiox Modern" ImGui theme, registered as an extra base style next to Axiom's ImGui Dark and Light.
 */
public final class PhilioxTheme {

    public static final String ID = "PhilioxModern";
    public static final String NAME = "Philiox Modern";

    // Index inside Axiom's BuiltinStyles.BASE_STYLES, -1 until registered
    private static int index = -1;

    private PhilioxTheme() {
    }

    public static int index() {
        return index;
    }

    public static ImGuiStyle[] appendTo(ImGuiStyle[] baseStyles) {
        if (index >= 0) {
            return baseStyles;
        }
        ImGuiStyle[] extended = Arrays.copyOf(baseStyles, baseStyles.length + 1);
        index = baseStyles.length;
        extended[index] = create();
        return extended;
    }

    public static String[] appendName(String[] names) {
        if (index < 0 || names.length != index) {
            return names;
        }
        String[] extended = Arrays.copyOf(names, names.length + 1);
        extended[index] = NAME;
        return extended;
    }

    private static ImGuiStyle create() {
        ImGuiStyle style = new ImGuiStyle();
        ImGui.styleColorsDark(style);

        style.setWindowPadding(10, 10);
        style.setFramePadding(8, 5);
        style.setItemSpacing(8, 6);
        style.setItemInnerSpacing(6, 5);
        style.setCellPadding(6, 4);
        style.setIndentSpacing(18);
        style.setScrollbarSize(12);
        style.setGrabMinSize(10);

        style.setWindowRounding(8);
        style.setChildRounding(6);
        style.setFrameRounding(6);
        style.setPopupRounding(8);
        style.setScrollbarRounding(8);
        style.setGrabRounding(6);
        style.setTabRounding(6);

        style.setWindowBorderSize(1);
        style.setChildBorderSize(1);
        style.setPopupBorderSize(1);
        style.setFrameBorderSize(0);
        style.setTabBorderSize(0);
        style.setWindowTitleAlign(0.5f, 0.5f);

        // Dark slate surfaces with a soft blue accent
        color(style, ImGuiCol.Text, 0.92f, 0.93f, 0.96f, 1.00f);
        color(style, ImGuiCol.TextDisabled, 0.50f, 0.53f, 0.60f, 1.00f);
        color(style, ImGuiCol.WindowBg, 0.08f, 0.09f, 0.11f, 0.97f);
        color(style, ImGuiCol.ChildBg, 0.10f, 0.11f, 0.14f, 0.60f);
        color(style, ImGuiCol.PopupBg, 0.09f, 0.10f, 0.13f, 0.98f);
        color(style, ImGuiCol.Border, 0.22f, 0.24f, 0.30f, 0.60f);
        color(style, ImGuiCol.BorderShadow, 0.00f, 0.00f, 0.00f, 0.00f);

        color(style, ImGuiCol.FrameBg, 0.14f, 0.15f, 0.19f, 1.00f);
        color(style, ImGuiCol.FrameBgHovered, 0.19f, 0.21f, 0.27f, 1.00f);
        color(style, ImGuiCol.FrameBgActive, 0.23f, 0.26f, 0.34f, 1.00f);

        color(style, ImGuiCol.TitleBg, 0.07f, 0.08f, 0.10f, 1.00f);
        color(style, ImGuiCol.TitleBgActive, 0.11f, 0.13f, 0.18f, 1.00f);
        color(style, ImGuiCol.TitleBgCollapsed, 0.07f, 0.08f, 0.10f, 0.80f);
        color(style, ImGuiCol.MenuBarBg, 0.10f, 0.11f, 0.14f, 1.00f);

        color(style, ImGuiCol.ScrollbarBg, 0.08f, 0.09f, 0.11f, 0.50f);
        color(style, ImGuiCol.ScrollbarGrab, 0.24f, 0.27f, 0.34f, 1.00f);
        color(style, ImGuiCol.ScrollbarGrabHovered, 0.31f, 0.35f, 0.44f, 1.00f);
        color(style, ImGuiCol.ScrollbarGrabActive, 0.40f, 0.55f, 1.00f, 1.00f);

        color(style, ImGuiCol.CheckMark, 0.45f, 0.62f, 1.00f, 1.00f);
        color(style, ImGuiCol.SliderGrab, 0.40f, 0.55f, 1.00f, 0.90f);
        color(style, ImGuiCol.SliderGrabActive, 0.55f, 0.68f, 1.00f, 1.00f);

        color(style, ImGuiCol.Button, 0.18f, 0.20f, 0.26f, 1.00f);
        color(style, ImGuiCol.ButtonHovered, 0.40f, 0.55f, 1.00f, 0.75f);
        color(style, ImGuiCol.ButtonActive, 0.33f, 0.47f, 0.92f, 1.00f);

        color(style, ImGuiCol.Header, 0.40f, 0.55f, 1.00f, 0.30f);
        color(style, ImGuiCol.HeaderHovered, 0.40f, 0.55f, 1.00f, 0.50f);
        color(style, ImGuiCol.HeaderActive, 0.40f, 0.55f, 1.00f, 0.70f);

        color(style, ImGuiCol.Separator, 0.22f, 0.24f, 0.30f, 1.00f);
        color(style, ImGuiCol.SeparatorHovered, 0.40f, 0.55f, 1.00f, 0.70f);
        color(style, ImGuiCol.SeparatorActive, 0.40f, 0.55f, 1.00f, 1.00f);

        color(style, ImGuiCol.ResizeGrip, 0.40f, 0.55f, 1.00f, 0.15f);
        color(style, ImGuiCol.ResizeGripHovered, 0.40f, 0.55f, 1.00f, 0.60f);
        color(style, ImGuiCol.ResizeGripActive, 0.40f, 0.55f, 1.00f, 0.90f);

        color(style, ImGuiCol.Tab, 0.12f, 0.13f, 0.17f, 1.00f);
        color(style, ImGuiCol.TabHovered, 0.40f, 0.55f, 1.00f, 0.60f);
        color(style, ImGuiCol.TabSelected, 0.20f, 0.25f, 0.38f, 1.00f);
        color(style, ImGuiCol.TabSelectedOverline, 0.40f, 0.55f, 1.00f, 1.00f);
        color(style, ImGuiCol.TabDimmed, 0.10f, 0.11f, 0.14f, 1.00f);
        color(style, ImGuiCol.TabDimmedSelected, 0.15f, 0.18f, 0.26f, 1.00f);

        color(style, ImGuiCol.DockingPreview, 0.40f, 0.55f, 1.00f, 0.50f);
        color(style, ImGuiCol.DockingEmptyBg, 0.06f, 0.07f, 0.09f, 1.00f);

        color(style, ImGuiCol.TableHeaderBg, 0.13f, 0.15f, 0.20f, 1.00f);
        color(style, ImGuiCol.TableBorderStrong, 0.22f, 0.24f, 0.30f, 1.00f);
        color(style, ImGuiCol.TableBorderLight, 0.17f, 0.19f, 0.24f, 1.00f);
        color(style, ImGuiCol.TableRowBgAlt, 1.00f, 1.00f, 1.00f, 0.03f);

        color(style, ImGuiCol.TextSelectedBg, 0.40f, 0.55f, 1.00f, 0.35f);
        color(style, ImGuiCol.TextLink, 0.50f, 0.65f, 1.00f, 1.00f);
        color(style, ImGuiCol.NavCursor, 0.40f, 0.55f, 1.00f, 1.00f);
        color(style, ImGuiCol.ModalWindowDimBg, 0.00f, 0.00f, 0.00f, 0.55f);
        return style;
    }

    private static void color(ImGuiStyle style, int col, float r, float g, float b, float a) {
        style.setColor(col, r, g, b, a);
    }

}
