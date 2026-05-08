package dev.roope.survivalstats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    private ConfigManager() {
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance()
            .getConfigDir()
            .resolve("survivalstats")
            .resolve("config.json");
    }

    public static Config loadOrCreate() {
        Path configFile = getConfigPath();
        try {
            if (!Files.exists(configFile)) {
                Files.createDirectories(configFile.getParent());
                Config defaults = new Config();
                defaults.normalize();
                writeAtomic(configFile, GSON.toJson(defaults));
                SurvivalStats.LOGGER.info("Wrote default Survival Stats config to {}", configFile);
                return defaults;
            }
            String json = Files.readString(configFile);
            Config loaded;
            try {
                loaded = GSON.fromJson(json, Config.class);
            } catch (JsonParseException jpe) {
                Path backup = backupCorruptFile(configFile);
                SurvivalStats.LOGGER.error(
                    "Survival Stats config '{}' is unparseable JSON. Backed up to '{}' and falling back to defaults.",
                    configFile, backup, jpe
                );
                loaded = new Config();
            }
            if (loaded == null) loaded = new Config();
            loaded.normalize();
            return loaded;
        } catch (IOException e) {
            SurvivalStats.LOGGER.error("Failed to load Survival Stats config; using defaults.", e);
            Config fallback = new Config();
            fallback.normalize();
            return fallback;
        }
    }

    public static void save(Config config) throws IOException {
        Path path = getConfigPath();
        Files.createDirectories(path.getParent());
        config.normalize();
        writeAtomic(path, GSON.toJson(config));
    }

    private static void writeAtomic(Path target, String contents) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, contents);
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            // Fall back to non-atomic move on filesystems that disallow atomic cross-device moves.
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path backupCorruptFile(Path path) throws IOException {
        Path backup = path.resolveSibling(
            path.getFileName().toString() + ".broken-" + Instant.now().toEpochMilli());
        Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
        return backup;
    }
}
