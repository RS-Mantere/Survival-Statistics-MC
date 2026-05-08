package dev.roope.survivalstats;

import dev.roope.survivalstats.command.StatsCommand;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SurvivalStats implements DedicatedServerModInitializer {
    public static final String MOD_ID = "survivalstats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Set on SERVER_STARTED, cleared on SERVER_STOPPING. Volatile so the
     * tick thread observes the latest value if the server is restarted in-process.
     */
    private static volatile SurvivalStatsService service;

    @Override
    public void onInitializeServer() {
        LOGGER.info("Survival Stats initializing");

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onPlayerJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onPlayerDisconnect(handler.getPlayer()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
            StatsCommand.register(dispatcher, SurvivalStats::service));
    }

    private void onServerStarted(MinecraftServer server) {
        Config config = ConfigManager.loadOrCreate();
        SurvivalStatsService s = new SurvivalStatsService(server, config);
        s.onServerStarted();
        service = s;
    }

    private void onServerStopping(MinecraftServer server) {
        service = null;
    }

    private void onServerTick(MinecraftServer server) {
        SurvivalStatsService s = service;
        if (s != null) {
            s.onServerTick();
        }
    }

    private void onPlayerJoin(net.minecraft.server.level.ServerPlayer player) {
        SurvivalStatsService s = service;
        if (s != null) {
            s.onPlayerJoin(player);
        }
    }

    private void onPlayerDisconnect(net.minecraft.server.level.ServerPlayer player) {
        SurvivalStatsService s = service;
        if (s != null) {
            s.onPlayerDisconnect(player);
        }
    }

    public static SurvivalStatsService service() {
        return service;
    }
}
