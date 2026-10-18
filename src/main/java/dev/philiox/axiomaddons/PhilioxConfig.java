package dev.philiox.axiomaddons;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Tiny addon config in config/philioxaxiomaddons.properties.
 */
public final class PhilioxConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("philioxaxiomaddons");
    private static final String MODERN_THEME_APPLIED = "modernThemeApplied";
    private static final String EXCLUDE_CUSTOM_BLOCKS = "excludeCustomBlocks";
    private static final String BLOCK_BLACKLIST = "blockBlacklist";
    private static final String FILTER_AXIOM = "filterAxiom";
    private static final String SHAPE_SMOOTHING = "shapeSmoothing";

    private static Properties properties;

    private PhilioxConfig() {
    }

    /** Whether Axiom has already saved our theme once, after that the user's own theme choice wins. */
    public static boolean isModernThemeApplied() {
        return Boolean.parseBoolean(load().getProperty(MODERN_THEME_APPLIED, "false"));
    }

    public static void setModernThemeApplied() {
        load().setProperty(MODERN_THEME_APPLIED, "true");
        save();
    }

    /** Leave out server custom blocks (Nexo, ItemsAdder, Oraxen) when tools pick blocks by colour. */
    public static boolean isExcludeCustomBlocks() {
        return Boolean.parseBoolean(load().getProperty(EXCLUDE_CUSTOM_BLOCKS, "true"));
    }

    public static void setExcludeCustomBlocks(boolean exclude) {
        load().setProperty(EXCLUDE_CUSTOM_BLOCKS, Boolean.toString(exclude));
        save();
    }

    /** Whether the block filter also applies to Axiom's own colour features like Autoshade. */
    public static boolean isFilterAxiom() {
        return Boolean.parseBoolean(load().getProperty(FILTER_AXIOM, "true"));
    }

    public static void setFilterAxiom(boolean filter) {
        load().setProperty(FILTER_AXIOM, Boolean.toString(filter));
        save();
    }

    /** Smooth the edges of shapes with stairs and slabs. */
    public static boolean isShapeSmoothing() {
        return Boolean.parseBoolean(load().getProperty(SHAPE_SMOOTHING, "false"));
    }

    public static void setShapeSmoothing(boolean smoothing) {
        load().setProperty(SHAPE_SMOOTHING, Boolean.toString(smoothing));
        save();
    }

    /** Block ids that tools never pick by colour, e.g. "minecraft:sponge". */
    public static List<String> getBlockBlacklist() {
        String value = load().getProperty(BLOCK_BLACKLIST, "").trim();
        if (value.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(id -> !id.isEmpty()).toList();
    }

    public static void setBlockBlacklist(List<String> ids) {
        load().setProperty(BLOCK_BLACKLIST, String.join(",", ids));
        save();
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("philioxaxiomaddons.properties");
    }

    private static Properties load() {
        if (properties == null) {
            properties = new Properties();
            Path path = path();
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    properties.load(reader);
                } catch (IOException e) {
                    LOGGER.warn("Could not read {}", path, e);
                }
            }
        }
        return properties;
    }

    private static void save() {
        Path path = path();
        try (Writer writer = Files.newBufferedWriter(path)) {
            load().store(writer, "Philiox Axiom Addons");
        } catch (IOException e) {
            LOGGER.warn("Could not write {}", path, e);
        }
    }

}
