package com.example.survivalstats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("survivalstats").resolve("config.json");
    }

    public static Config loadOrCreate() {
        Path configFile = getConfigPath();

        try {
            if (!Files.exists(configFile)) {
                Files.createDirectories(configFile.getParent());
                Config defaults = new Config();
                Files.writeString(configFile, GSON.toJson(defaults));
                SurvivalStats.LOGGER.info("Wrote default config to {}", configFile);
                return defaults;
            }
            String json = Files.readString(configFile);
            Config loaded = GSON.fromJson(json, Config.class);
            if (loaded == null) loaded = new Config();
            return loaded;
        } catch (IOException e) {
            SurvivalStats.LOGGER.error("Failed to load config, using defaults.", e);
            return new Config();
        }
    }

    public static void save(Config config) {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(config));
        } catch (IOException e) {
            SurvivalStats.LOGGER.error("Failed to save config", e);
        }
    }
}
