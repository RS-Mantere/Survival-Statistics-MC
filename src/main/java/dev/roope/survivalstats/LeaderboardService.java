package dev.roope.survivalstats;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        Map<UUID, String> userCacheNames = loadUserCacheNames();

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
                String name = resolveName(uuid, userCacheNames);
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

    /**
     * Reads {@code usercache.json} in the world root (same layout as vanilla dedicated server).
     * Avoids {@code GameProfile} / {@code ProfileResolver} accessor differences across versions.
     */
    private Map<UUID, String> loadUserCacheNames() {
        Path path = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("usercache.json");
        if (!Files.exists(path)) {
            return Map.of();
        }
        try {
            JsonElement root = GSON.fromJson(Files.readString(path), JsonElement.class);
            if (root == null || !root.isJsonArray()) {
                return Map.of();
            }
            JsonArray arr = root.getAsJsonArray();
            Map<UUID, String> out = new HashMap<>();
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                if (!o.has("uuid") || !o.has("name")) continue;
                String uuidStr = o.get("uuid").getAsString();
                String name = o.get("name").getAsString();
                UUID parsed = parseUuidLenient(uuidStr);
                if (parsed != null && name != null && !name.isBlank()) {
                    out.put(parsed, name);
                }
            }
            return out;
        } catch (Exception e) {
            SurvivalStats.LOGGER.warn("Failed to read usercache.json for leaderboard display names", e);
            return Map.of();
        }
    }

    private static UUID parseUuidLenient(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
        }
        // Some older files store UUIDs without hyphens.
        String compact = raw.replace("-", "");
        if (compact.length() != 32) return null;
        try {
            return new UUID(
                Long.parseUnsignedLong(compact.substring(0, 16), 16),
                Long.parseUnsignedLong(compact.substring(16, 32), 16)
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveName(UUID uuid, Map<UUID, String> userCacheNames) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            return player.getName().getString();
        }
        String cached = userCacheNames.get(uuid);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        return uuid.toString();
    }

    public record PlayerStatValue(UUID uuid, String name, long value) {
    }
}
