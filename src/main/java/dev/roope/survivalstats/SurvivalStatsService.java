package dev.roope.survivalstats;

import dev.roope.survivalstats.display.RenderedTabList;
import dev.roope.survivalstats.display.TabListRenderer;
import dev.roope.survivalstats.display.TabListSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Scoreboard;

/**
 * Owns Survival Stats runtime state for a single server lifecycle.
 * Created on SERVER_STARTED, discarded on SERVER_STOPPING.
 */
public final class SurvivalStatsService {
    private final MinecraftServer server;
    private final StatReader statReader = new StatReader();
    private final TabListRenderer renderer = new TabListRenderer();
    private final TabListSender sender = new TabListSender();
    private Config config;
    private int tickCounter;

    public SurvivalStatsService(MinecraftServer server, Config config) {
        this.server = server;
        this.config = config;
    }

    public Config config() {
        return config;
    }

    public MinecraftServer server() {
        return server;
    }

    public void replaceConfig(Config newConfig) {
        this.config = newConfig;
        this.tickCounter = 0;
        this.sender.resetCache();
    }

    public void onServerStarted() {
        tickCounter = 0;
        clearLegacyDisplaySlots();
        renderForAllPlayers(true);
        SurvivalStats.LOGGER.info(
            "Survival Stats ready. Rendering tab-list stats every {} tick(s).",
            config.refreshIntervalTicks()
        );
    }

    public void onServerTick() {
        tickCounter++;
        if (tickCounter >= config.refreshIntervalTicks()) {
            tickCounter = 0;
            renderForAllPlayers(false);
        }
    }

    public void setRefreshIntervalTicks(int ticks) {
        config.setRefreshIntervalTicks(ticks);
        tickCounter = 0;
    }

    public void setDisplayEnabled(boolean enabled) {
        config.setDisplayEnabled(enabled);
        tickCounter = 0;
        renderForAllPlayers(true);
    }

    public void onPlayerJoin(ServerPlayer player) {
        if (!config.displayEnabled()) {
            sender.clear(player);
            return;
        }
        StatSnapshot snapshot = statReader.read(player);
        RenderedTabList rendered = renderer.render(config, snapshot);
        sender.sendIfChanged(player, rendered);
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        sender.forget(player);
    }

    public void renderForPlayer(ServerPlayer player, boolean force) {
        if (!config.displayEnabled()) {
            sender.clear(player);
            return;
        }
        StatSnapshot snapshot = statReader.read(player);
        RenderedTabList rendered = renderer.render(config, snapshot);
        if (force) {
            sender.forget(player);
        }
        sender.sendIfChanged(player, rendered);
    }

    public void renderForAllPlayers(boolean force) {
        if (!config.displayEnabled()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sender.clear(player);
            }
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            renderForPlayer(player, force);
        }
    }

    public void clearLegacyDisplaySlots() {
        Scoreboard scoreboard = server.getScoreboard();
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
        scoreboard.setDisplayObjective(DisplaySlot.LIST, null);
        scoreboard.setDisplayObjective(DisplaySlot.BELOW_NAME, null);
    }
}
