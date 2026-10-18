package dev.philiox.axiomaddons.tools;

import com.moulberry.axiom.tools.ToolManager;

/**
 * Adds the addon's tools to Axiom's tool list, after Axiom's own tools.
 */
public final class AddonTools {

    private static boolean registered = false;

    private AddonTools() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ToolManager.addTool(new TreeTool());
        ToolManager.addTool(new BridgeTool());
        ToolManager.addTool(new ShipTool());
        ToolManager.addTool(new WaveTool());
        ToolManager.addTool(new OceanPainterTool());
        ToolManager.addTool(new WeatheringTool());
        ToolManager.addTool(new RuinTool());
        ToolManager.addTool(new GradientFillTool());
        ToolManager.addTool(new ColorPaletteTool());
        ToolManager.addTool(new BlockDisplayTool());
    }

}
