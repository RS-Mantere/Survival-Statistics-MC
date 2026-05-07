package com.example.survivalstats;

import com.mojang.brigadier.Command;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SurvivalStats implements DedicatedServerModInitializer {
    public static final String MOD_ID = "survivalstats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static Config config;
    private static int tickCounter = 0;
    private static int rotationIndex = 0;
    private static MinecraftServer serverRef;

    @Override
    public void onInitializeServer() {
        LOGGER.info("Survival Stats initializing");

        config = ConfigManager.loadOrCreate();

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("stats")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload").executes(ctx -> {
                    config = ConfigManager.loadOrCreate();
                    if (serverRef != null) {
                        ensureObjectives(serverRef);
                        applyStaticDisplays(serverRef);
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("Survival Stats config reloaded."), true);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("rotate").executes(ctx -> {
                    if (serverRef != null) {
                        advanceRotation(serverRef);
                    }
                    return Command.SINGLE_SUCCESS;
                }))
            );
        });
    }

    private void onServerStarted(MinecraftServer server) {
        serverRef = server;
        ensureObjectives(server);
        applyStaticDisplays(server);
        LOGGER.info("Survival Stats ready. Rotating {} stats every {} ticks.",
            config.rotation.size(), config.rotationIntervalTicks);
    }

    private void onServerTick(MinecraftServer server) {
        if (config.rotation.isEmpty()) return;
        tickCounter++;
        if (tickCounter >= config.rotationIntervalTicks) {
            tickCounter = 0;
            advanceRotation(server);
        }
    }

    private void advanceRotation(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        String objectiveName = config.rotation.get(rotationIndex);
        Objective obj = scoreboard.getObjective(objectiveName);
        if (obj != null) {
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, obj);
        } else {
            LOGGER.warn("Rotation objective '{}' not found, skipping.", objectiveName);
        }
        rotationIndex = (rotationIndex + 1) % config.rotation.size();
    }

    private void ensureObjectives(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        for (StatDef def : StatDef.ALL) {
            if (scoreboard.getObjective(def.id) == null) {
                ObjectiveCriteria criteria = ObjectiveCriteria.byName(def.criterion).orElse(null);
                if (criteria == null) {
                    LOGGER.error("Unknown criterion '{}' for objective '{}'", def.criterion, def.id);
                    continue;
                }
                scoreboard.addObjective(
                    def.id,
                    criteria,
                    Component.literal(def.displayName),
                    criteria.getDefaultRenderType(),
                    true,
                    null
                );
                LOGGER.info("Created objective {}", def.id);
            }
        }
    }

    private void applyStaticDisplays(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        applySlot(scoreboard, DisplaySlot.LIST, config.tabListObjective);
        applySlot(scoreboard, DisplaySlot.BELOW_NAME, config.belowNameObjective);
    }

    private void applySlot(Scoreboard scoreboard, DisplaySlot slot, String objectiveName) {
        if (objectiveName == null || objectiveName.isBlank()) return;
        Objective obj = scoreboard.getObjective(objectiveName);
        if (obj != null) {
            scoreboard.setDisplayObjective(slot, obj);
        } else {
            LOGGER.warn("Objective '{}' for slot {} not found.", objectiveName, slot);
        }
    }
}
