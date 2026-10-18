package dev.philiox.axiomaddons.tools;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.block_maps.BlockColourMap;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.tools.Tool;
import imgui.moulberry92.ImGui;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Brush that ages blocks: hold right click and paint, cracked, mossy and similar coloured variants are mixed in.
 */
public class WeatheringTool extends AddonTool {

    private static final int MODE_CURATED = 0;
    private static final int MODE_COLOUR = 1;
    private static final int MODE_BOTH = 2;
    private static final String[] MODE_KEYS = {"curated", "colour", "both"};

    private static final Map<Block, Block[]> VARIANTS = new HashMap<>();

    static {
        variants(Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS);
        variants(Blocks.STONE_BRICK_STAIRS, Blocks.MOSSY_STONE_BRICK_STAIRS);
        variants(Blocks.STONE_BRICK_SLAB, Blocks.MOSSY_STONE_BRICK_SLAB);
        variants(Blocks.STONE_BRICK_WALL, Blocks.MOSSY_STONE_BRICK_WALL);
        variants(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
        variants(Blocks.COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE_STAIRS);
        variants(Blocks.COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE_SLAB);
        variants(Blocks.COBBLESTONE_WALL, Blocks.MOSSY_COBBLESTONE_WALL);
        variants(Blocks.STONE, Blocks.COBBLESTONE, Blocks.ANDESITE);
        variants(Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS);
        variants(Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES);
        variants(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
        variants(Blocks.NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS);
        variants(Blocks.END_STONE_BRICKS, Blocks.END_STONE);
        variants(Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE);
        variants(Blocks.TUFF_BRICKS, Blocks.TUFF);
        variants(Blocks.CUT_SANDSTONE, Blocks.SANDSTONE);
        variants(Blocks.SMOOTH_SANDSTONE, Blocks.SANDSTONE);
        variants(Blocks.POLISHED_ANDESITE, Blocks.ANDESITE);
        variants(Blocks.POLISHED_DIORITE, Blocks.DIORITE);
        variants(Blocks.POLISHED_GRANITE, Blocks.GRANITE);
        variants(Blocks.COPPER_BLOCK, Blocks.EXPOSED_COPPER);
        variants(Blocks.EXPOSED_COPPER, Blocks.WEATHERED_COPPER);
        variants(Blocks.WEATHERED_COPPER, Blocks.OXIDIZED_COPPER);
        variants(Blocks.CUT_COPPER, Blocks.EXPOSED_CUT_COPPER);
        variants(Blocks.EXPOSED_CUT_COPPER, Blocks.WEATHERED_CUT_COPPER);
        variants(Blocks.WEATHERED_CUT_COPPER, Blocks.OXIDIZED_CUT_COPPER);
    }

    private static void variants(Block block, Block... aged) {
        VARIANTS.put(block, aged);
    }

    /** A cracked, mossy or oxidized version of the block with the same shape, or null if there is none. */
    static @Nullable BlockState agedVariant(BlockState state, Random random) {
        Block[] aged = VARIANTS.get(state.getBlock());
        return aged == null ? null : aged[random.nextInt(aged.length)].withPropertiesOf(state);
    }

    private final int[] radius = {4};
    private final float[] strength = {0.4f};
    private final int[] mode = {MODE_BOTH};
    private final int[] colourVariety = {5};

    private final LongOpenHashSet processed = new LongOpenHashSet();
    private final Random random = new Random();
    private boolean painting = false;

    @Override
    protected String id() {
        return "weathering";
    }

    @Override
    public String name() {
        return t("tool.weathering");
    }

    @Override
    public char iconChar() {
        // Axiom's roughen tool icon
        return (char) 59670;
    }

    @Override
    public void reset() {
        super.reset();
        this.painting = false;
        this.processed.clear();
    }

    @Override
    public UserAction.ActionResult callAction(UserAction action, Object data) {
        if (action == UserAction.RIGHT_MOUSE) {
            this.reset();
            this.painting = true;
            return UserAction.ActionResult.USED_STOP;
        }
        if (action == UserAction.ESCAPE && this.painting) {
            this.reset();
            return UserAction.ActionResult.USED_STOP;
        }
        return UserAction.ActionResult.NOT_HANDLED;
    }

    @Override
    public void render(AxiomWorldRenderContext rc) {
        RayCaster.RaycastResult hit = raycast();
        if (this.painting) {
            if (Tool.isMouseDown(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
                if (hit != null) {
                    this.paint(hit.blockPos());
                }
            } else {
                // Stroke finished
                this.commitPreview("history.weathering");
                this.reset();
            }
        }
        if (hit != null) {
            Tool.renderRaycastOverlay(rc, hit.blockPos());
        }
        this.renderPreview(rc);
    }

    private void paint(BlockPos center) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        int r = Math.max(1, this.radius[0]);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z > r * r + r) {
                        continue;
                    }
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!this.processed.add(pos.asLong())) {
                        continue;
                    }
                    if (this.random.nextFloat() > this.strength[0]) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    BlockState aged = this.age(state);
                    if (aged != null && aged != state) {
                        this.preview.addBlock(pos.getX(), pos.getY(), pos.getZ(), aged);
                    }
                }
            }
        }
    }

    private @Nullable BlockState age(BlockState state) {
        boolean curated = this.mode[0] != MODE_COLOUR;
        boolean colour = this.mode[0] != MODE_CURATED;

        Block[] aged = curated ? VARIANTS.get(state.getBlock()) : null;
        if (aged != null && (!colour || this.random.nextBoolean())) {
            return aged[this.random.nextInt(aged.length)].withPropertiesOf(state);
        }
        if (colour && BlockColourMap.isFullSolidOpaque(state)) {
            Vec3 lab = BlockColourMap.getLab(state.getBlock());
            if (lab == null) {
                return null;
            }
            int flags = BlockColourMap.FLAG_SOLID | BlockColourMap.FLAG_OPAQUE | BlockColourMap.FLAG_FULL_CUBE
                | BlockColourMap.FLAG_NO_ORES | BlockColourMap.FLAG_NO_GLAZED_TERRACOTTA | BlockColourMap.FLAG_NO_TILE_ENTITIES;
            List<BlockState> similar = BlockFilter.allowed(
                BlockColourMap.getNearestLabN(lab.x, lab.y, lab.z, Math.max(2, this.colourVariety[0]) * 3, flags), true);
            similar = similar.subList(0, Math.min(similar.size(), Math.max(2, this.colourVariety[0])));
            if (similar.isEmpty()) {
                return null;
            }
            return similar.get(this.random.nextInt(similar.size()));
        }
        return aged != null ? aged[this.random.nextInt(aged.length)].withPropertiesOf(state) : null;
    }

    @Override
    public void displayImguiOptions() {
        ImGuiHelper.separatorWithText(this.name());
        TreeTool.combo("weathering.mode", this.mode, MODE_KEYS, "weathering.mode.");
        ImGui.sliderInt(t("weathering.radius"), this.radius, 1, 16);
        ImGui.sliderFloat(t("weathering.strength"), this.strength, 0.05f, 1.0f, "%.2f");
        if (this.mode[0] != MODE_CURATED) {
            ImGui.sliderInt(t("weathering.variety"), this.colourVariety, 2, 12);
            BlockFilter.displayOptions();
        }
        ImGui.textWrapped(t("weathering.hint"));
    }

    @Override
    public void writeSettings(CompoundTag tag) {
        tag.putInt("Radius", this.radius[0]);
        tag.putFloat("Strength", this.strength[0]);
        tag.putInt("Mode", this.mode[0]);
        tag.putInt("ColourVariety", this.colourVariety[0]);
    }

    @Override
    public void loadSettings(CompoundTag tag) {
        this.radius[0] = tag.getIntOr("Radius", 4);
        this.strength[0] = tag.getFloatOr("Strength", 0.4f);
        this.mode[0] = Math.clamp(tag.getIntOr("Mode", MODE_BOTH), 0, MODE_KEYS.length - 1);
        this.colourVariety[0] = tag.getIntOr("ColourVariety", 5);
    }

}
