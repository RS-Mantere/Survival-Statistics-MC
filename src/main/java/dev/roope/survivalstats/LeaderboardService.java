package dev.roope.survivalstats;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class LeaderboardService {
    private static final long CACHE_TTL_MS = 30_000L;
    private static final Gson GSON = new Gson();

    private final MinecraftServer server;
    private long lastLoadedAtMs;
    private List<PlayerStatValue> cached = List.of();

    public LeaderboardService(MinecraftServer server) {
        this.server = server;
    }

    public List<PlayerStatValue> top(StatDef stat, int count) {
        long now = System.currentTimeMillis();
        if (now - lastLoadedAtMs > CACHE_TTL_MS) {
            cached = loadAllPlayers(stat);
            lastLoadedAtMs = now;
        }
        return cached.stream()
            .sorted(Comparator.comparingLong(PlayerStatValue::value).reversed())
            .limit(Math.max(1, count))
            .toList();
    }

    private List<PlayerStatValue> loadAllPlayers(StatDef stat) {
        Path statsDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("stats");
        if (!Files.exists(statsDir)) {
            return List.of();
        }

        List<PlayerStatValue> rows = new ArrayList<>();
        try (var paths = Files.list(statsDir)) {
            paths.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(path -> {
                String file = path.getFileName().toString();
                String uuidText = file.substring(0, file.length() - ".json".length());
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidText);
                } catch (IllegalArgumentException ignored) {
                    return;
                }
                long value = readStatValue(path, stat.customStatKey());
                String name = resolveName(uuid);
                rows.add(new PlayerStatValue(uuid, name, value));
            });
        } catch (IOException e) {
            SurvivalStats.LOGGER.error("Failed to read stats directory for leaderboard", e);
            return List.of();
        }
        return rows;
    }

    private static long readStatValue(Path file, String customStatKey) {
        try {
            JsonObject root = GSON.fromJson(Files.readString(file), JsonObject.class);
            if (root == null) return 0L;
            JsonObject stats = asObject(root.get("stats"));
            JsonObject custom = asObject(stats == null ? null : stats.get("minecraft:custom"));
            if (custom == null) return 0L;
            JsonElement valueEl = custom.get(customStatKey);
            return valueEl == null ? 0L : valueEl.getAsLong();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static JsonObject asObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private String resolveName(UUID uuid) {
        var player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            return player.getGameProfile().getName();
        }
        try {
            GameProfile profile = server.getProfileCache().get(uuid).orElse(null);
            if (profile != null && profile.getName() != null) {
                return profile.getName();
            }
        } catch (Exception ignored) {
        }
        return uuid.toString();
    }

    public record PlayerStatValue(UUID uuid, String name, long value) {
    }
}
