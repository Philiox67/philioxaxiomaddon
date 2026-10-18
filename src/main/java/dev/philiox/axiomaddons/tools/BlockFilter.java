package dev.philiox.axiomaddons.tools;

import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.tools.Tool;
import dev.philiox.axiomaddons.PhilioxConfig;
import imgui.moulberry92.ImGui;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared filter for every tool that picks blocks by colour (gradient fill, weathering, colour palette).
 * The settings are global and saved in the addon config.
 */
public final class BlockFilter {

    /**
     * Plugins like Nexo re-texture these blocks through the resource pack. Every state except the default one
     * shows a plugin texture, so they are treated as custom blocks.
     */
    private static final Set<Block> RESOURCE_PACK_CARRIERS = Set.of(
        Blocks.NOTE_BLOCK, Blocks.TRIPWIRE, Blocks.CHORUS_PLANT,
        Blocks.BROWN_MUSHROOM_BLOCK, Blocks.RED_MUSHROOM_BLOCK, Blocks.MUSHROOM_STEM);

    /** Full cubes that still look wrong in a gradient or are technical blocks. */
    private static final Set<Block> UNSUITABLE = Set.of(
        Blocks.BEDROCK, Blocks.BARRIER, Blocks.STRUCTURE_BLOCK, Blocks.STRUCTURE_VOID, Blocks.JIGSAW,
        Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.REPEATING_COMMAND_BLOCK,
        Blocks.SPAWNER, Blocks.TRIAL_SPAWNER, Blocks.VAULT, Blocks.HEAVY_CORE, Blocks.REINFORCED_DEEPSLATE,
        Blocks.TNT, Blocks.CRAFTING_TABLE, Blocks.FLETCHING_TABLE, Blocks.SMITHING_TABLE, Blocks.CARTOGRAPHY_TABLE,
        Blocks.BUDDING_AMETHYST, Blocks.INFESTED_STONE, Blocks.INFESTED_COBBLESTONE, Blocks.INFESTED_STONE_BRICKS,
        Blocks.INFESTED_MOSSY_STONE_BRICKS, Blocks.INFESTED_CRACKED_STONE_BRICKS, Blocks.INFESTED_CHISELED_STONE_BRICKS,
        Blocks.INFESTED_DEEPSLATE);

    /** Config prefix for a single blacklisted block state, e.g. one Nexo block inside the note block states. */
    private static final String STATE_PREFIX = "state:";

    /** Whole blocks (all states) and single states, the latter for plugin blocks that share one vanilla block. */
    private record Blacklist(Set<Block> blocks, Set<BlockState> states, List<BlockState> entries) {
    }

    private static @Nullable Blacklist blacklist = null;
    private static int version = 0;

    private BlockFilter() {
    }

    /** Increases whenever the filter changes, so tools know when to recalculate. */
    public static int version() {
        return version;
    }

    public static boolean isAllowed(@Nullable BlockState state) {
        if (state == null) {
            return false;
        }
        if (PhilioxConfig.isExcludeCustomBlocks() && isCustomBlock(state)) {
            return false;
        }
        Blacklist list = blacklist();
        return !list.blocks().contains(state.getBlock()) && !list.states().contains(state);
    }

    /**
     * Strict full block check: full cube hitbox and outline, not see-through, no block entity
     * (vaults, trial spawners, chests, furnaces), no ores, no glazed terracotta and no technical blocks.
     */
    public static boolean isFullBlock(@Nullable BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        Block block = state.getBlock();
        if (UNSUITABLE.contains(block) || state.hasBlockEntity() || !state.canOcclude()) {
            return false;
        }
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        if (path.endsWith("_ore") || path.equals("ancient_debris") || path.contains("glazed_terracotta")) {
            return false;
        }
        try {
            return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
                && Block.isShapeFullBlock(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        } catch (RuntimeException e) {
            // Some blocks need a real world for their shape, they are not used then
            return false;
        }
    }

    /** First block of the list that passes the filter and, if wanted, the strict full block check. */
    public static @Nullable BlockState firstAllowed(@Nullable List<BlockState> candidates, boolean fullBlocksOnly) {
        if (candidates == null) {
            return null;
        }
        for (BlockState candidate : candidates) {
            if (isAllowed(candidate) && (!fullBlocksOnly || isFullBlock(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    public static List<BlockState> allowed(@Nullable List<BlockState> candidates, boolean fullBlocksOnly) {
        List<BlockState> result = new ArrayList<>();
        if (candidates != null) {
            for (BlockState candidate : candidates) {
                if (isAllowed(candidate) && (!fullBlocksOnly || isFullBlock(candidate))) {
                    result.add(candidate);
                }
            }
        }
        return result;
    }

    public static boolean isCustomBlock(BlockState state) {
        if (ServerCustomBlocks.getCustomStateFor(state) != null) {
            return true;
        }
        Block block = state.getBlock();
        return RESOURCE_PACK_CARRIERS.contains(block) && state != block.defaultBlockState();
    }

    /** First allowed block of the list, or null. */
    public static @Nullable BlockState firstAllowed(@Nullable List<BlockState> candidates) {
        return firstAllowed(candidates, false);
    }

    public static List<BlockState> allowed(@Nullable List<BlockState> candidates) {
        return allowed(candidates, false);
    }

    private static Blacklist blacklist() {
        if (blacklist == null) {
            Set<Block> blocks = new HashSet<>();
            Set<BlockState> states = new HashSet<>();
            List<BlockState> entries = new ArrayList<>();
            Set<String> blockIds = new HashSet<>();
            Set<String> stateIds = new HashSet<>();
            for (String entry : PhilioxConfig.getBlockBlacklist()) {
                if (entry.startsWith(STATE_PREFIX)) {
                    stateIds.add(entry.substring(STATE_PREFIX.length()));
                } else {
                    blockIds.add(entry);
                }
            }
            for (Block block : BuiltInRegistries.BLOCK) {
                if (blockIds.contains(id(block))) {
                    blocks.add(block);
                    entries.add(block.defaultBlockState());
                }
                if (!stateIds.isEmpty() && stateIds.stream().anyMatch(id -> id.startsWith("Block{" + id(block) + "}"))) {
                    for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                        if (stateIds.contains(state.toString())) {
                            states.add(state);
                            entries.add(state);
                        }
                    }
                }
            }
            blacklist = new Blacklist(blocks, states, entries);
        }
        return blacklist;
    }

    private static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    /** Custom blocks are stored as single states, normal blocks as the whole block. */
    private static String entryId(BlockState state, boolean singleState) {
        return singleState ? STATE_PREFIX + state : id(state.getBlock());
    }

    private static void saveBlacklist(List<String> ids) {
        List<String> sorted = new ArrayList<>(new HashSet<>(ids));
        sorted.sort(String::compareTo);
        PhilioxConfig.setBlockBlacklist(sorted);
        blacklist = null;
        version++;
    }

    private static List<String> currentIds() {
        Blacklist list = blacklist();
        List<String> ids = new ArrayList<>();
        for (BlockState entry : list.entries()) {
            ids.add(entryId(entry, list.states().contains(entry)));
        }
        return ids;
    }

    private static String describe(BlockState state, boolean singleState) {
        String name = state.getBlock().getName().getString();
        if (!singleState) {
            return name;
        }
        String text = state.toString();
        int properties = text.indexOf('[');
        return I18n.get("philioxaxiomaddons.filter.custom_entry", name, properties >= 0 ? text.substring(properties) : "");
    }

    /**
     * Collapsible "Block Filter" section, shown by every colour based tool.
     * @return whether the filter changed
     */
    public static boolean displayOptions() {
        if (!ImGui.collapsingHeader(I18n.get("philioxaxiomaddons.filter.title"))) {
            return false;
        }
        boolean changed = false;
        boolean exclude = PhilioxConfig.isExcludeCustomBlocks();
        if (ImGui.checkbox(I18n.get("philioxaxiomaddons.filter.exclude_custom"), exclude)) {
            PhilioxConfig.setExcludeCustomBlocks(!exclude);
            version++;
            changed = true;
        }

        boolean axiom = PhilioxConfig.isFilterAxiom();
        if (ImGui.checkbox(I18n.get("philioxaxiomaddons.filter.axiom"), axiom)) {
            PhilioxConfig.setFilterAxiom(!axiom);
            version++;
            changed = true;
        }

        if (ImGui.button(I18n.get("philioxaxiomaddons.filter.add_active"))) {
            BlockState active = Tool.getActiveBlock();
            List<String> ids = currentIds();
            // A Nexo block is one state of a note block, so only that state is blacklisted
            ids.add(entryId(active, isCustomBlock(active)));
            saveBlacklist(ids);
            changed = true;
        }
        ImGui.textWrapped(I18n.get("philioxaxiomaddons.filter.add_hint"));

        Blacklist list = blacklist();
        if (list.entries().isEmpty()) {
            ImGui.textWrapped(I18n.get("philioxaxiomaddons.filter.empty"));
            return changed;
        }
        ImGui.textWrapped(I18n.get("philioxaxiomaddons.filter.remove_hint"));
        float available = ImGui.getContentRegionAvailX();
        int perRow = Math.max(1, (int) (available / 40));
        BlockState removed = null;
        for (int i = 0; i < list.entries().size(); i++) {
            BlockState entry = list.entries().get(i);
            if (i % perRow != 0) {
                ImGui.sameLine();
            }
            if (ImGuiHelper.blockStateButton(ServerCustomBlocks.getCustomOrVanillaStateFor(entry), 300_000 + i, 32)) {
                removed = entry;
            }
            if (ImGui.isItemHovered()) {
                ImGuiHelper.tooltip(describe(entry, list.states().contains(entry)));
            }
        }
        if (removed != null) {
            List<String> ids = currentIds();
            ids.remove(entryId(removed, list.states().contains(removed)));
            saveBlacklist(ids);
            changed = true;
        }
        if (ImGui.button(I18n.get("philioxaxiomaddons.filter.clear"))) {
            saveBlacklist(List.of());
            changed = true;
        }
        return changed;
    }

}
