package com.example.survivalstats;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringTokenizer;

public class SurvivalStats implements DedicatedServerModInitializer {
    public static final String MOD_ID = "survivalstats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final int DERIVED_SYNC_INTERVAL_TICKS = 20;
    private static final String DISTANCE_OBJECTIVE = "Distance";
    private static final String DISTANCE_RAW_OBJECTIVE = "DistanceRaw";
    private static final String PLAYTIME_OBJECTIVE = "PlayTime";
    private static final String PLAYTIME_RAW_OBJECTIVE = "PlayTimeRaw";

    private static final SuggestionProvider<CommandSourceStack> OBJECTIVE_SUGGESTIONS = (context, builder) -> {
        MinecraftServer server = context.getSource().getServer();
        if (server == null) {
            return builder.buildFuture();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (StatDef def : StatDef.ALL) {
            names.add(def.id);
        }
        names.addAll(server.getScoreboard().getObjectiveNames());
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static Config config;
    private static int tickCounter = 0;
    private static int rotationIndex = 0;
    private static int derivedSyncCounter = 0;
    private static MinecraftServer serverRef;

    @Override
    public void onInitializeServer() {
        LOGGER.info("Survival Stats initializing");

        config = ConfigManager.loadOrCreate();
        normalizeConfig();

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(buildStatsCommand()));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildStatsCommand() {
        return Commands.literal("stats")
            .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
            .then(Commands.literal("reload").executes(ctx -> {
                config = ConfigManager.loadOrCreate();
                normalizeConfig();
                if (serverRef != null) {
                    applyAll(serverRef);
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
            .then(Commands.literal("show").executes(this::cmdShow))
            .then(Commands.literal("units")
                .then(Commands.literal("metric").executes(ctx -> setDistanceUnit(ctx, "metric")))
                .then(Commands.literal("imperial").executes(ctx -> setDistanceUnit(ctx, "imperial"))))
            .then(Commands.literal("reset").executes(this::cmdReset))
            .then(Commands.literal("interval")
                .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                    .executes(this::cmdInterval)))
            .then(Commands.literal("slot")
                .then(Commands.literal("tab")
                    .then(Commands.literal("none").executes(ctx -> clearTabListSlot(ctx)))
                    .then(Commands.argument("objective", StringArgumentType.word())
                        .suggests(OBJECTIVE_SUGGESTIONS)
                        .executes(ctx -> setTabListObjective(ctx, StringArgumentType.getString(ctx, "objective")))))
                .then(Commands.literal("belowname")
                    .then(Commands.literal("none").executes(ctx -> clearBelowNameSlot(ctx)))
                    .then(Commands.argument("objective", StringArgumentType.word())
                        .suggests(OBJECTIVE_SUGGESTIONS)
                        .executes(ctx -> setBelowNameObjective(ctx, StringArgumentType.getString(ctx, "objective"))))))
            .then(Commands.literal("rotation")
                .then(Commands.literal("list").executes(this::cmdRotationList))
                .then(Commands.literal("clear").executes(this::cmdRotationClear))
                .then(Commands.literal("add")
                    .then(Commands.argument("objective", StringArgumentType.word())
                        .suggests(OBJECTIVE_SUGGESTIONS)
                        .executes(ctx -> {
                            String objective = StringArgumentType.getString(ctx, "objective");
                            config.rotation.add(objective);
                            normalizeRotationIndex();
                            ConfigManager.save(config);
                            if (serverRef != null) {
                                applyAll(serverRef);
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                "Added '" + objective + "' to sidebar rotation."), true);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("remove")
                    .then(Commands.argument("objective", StringArgumentType.word())
                        .suggests(OBJECTIVE_SUGGESTIONS)
                        .executes(ctx -> {
                            String objective = StringArgumentType.getString(ctx, "objective");
                            boolean removed = config.rotation.remove(objective);
                            if (!removed) {
                                ctx.getSource().sendFailure(Component.literal(
                                    "Objective not in rotation: " + objective));
                                return 0;
                            }
                            normalizeRotationIndex();
                            ConfigManager.save(config);
                            if (serverRef != null) {
                                applyAll(serverRef);
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                "Removed '" + objective + "' from sidebar rotation."), true);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("insert")
                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                        .then(Commands.argument("objective", StringArgumentType.word())
                            .suggests(OBJECTIVE_SUGGESTIONS)
                            .executes(ctx -> {
                                int index = IntegerArgumentType.getInteger(ctx, "index");
                                String objective = StringArgumentType.getString(ctx, "objective");
                                int size = config.rotation.size();
                                int idx = Math.min(Math.max(0, index), size);
                                config.rotation.add(idx, objective);
                                normalizeRotationIndex();
                                ConfigManager.save(config);
                                if (serverRef != null) {
                                    applyAll(serverRef);
                                }
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Inserted '" + objective + "' at index " + idx + "."), true);
                                return Command.SINGLE_SUCCESS;
                            }))))
                .then(Commands.literal("set")
                    .then(Commands.argument("objectives", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String objectivesArg = StringArgumentType.getString(ctx, "objectives");
                            List<String> next = new ArrayList<>();
                            StringTokenizer tokenizer = new StringTokenizer(objectivesArg.trim());
                            while (tokenizer.hasMoreTokens()) {
                                next.add(tokenizer.nextToken());
                            }
                            config.rotation.clear();
                            config.rotation.addAll(next);
                            normalizeRotationIndex();
                            ConfigManager.save(config);
                            if (serverRef != null) {
                                applyAll(serverRef);
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                "Sidebar rotation replaced with " + config.rotation.size() + " objective(s)."), true);
                            return Command.SINGLE_SUCCESS;
                        }))));
    }

    private int cmdShow(CommandContext<CommandSourceStack> ctx) {
        String nl = "\n";
        StringBuilder sb = new StringBuilder();
        sb.append("Survival Stats config:").append(nl);
        sb.append("rotationIntervalTicks: ").append(config.rotationIntervalTicks).append(nl);
        sb.append("distanceUnit: ").append(config.distanceUnit).append(nl);
        sb.append("tabListObjective: ").append(blankAsNone(config.tabListObjective)).append(nl);
        sb.append("belowNameObjective: ").append(blankAsNone(config.belowNameObjective)).append(nl);
        sb.append("rotation:").append(nl);
        if (config.rotation.isEmpty()) {
            sb.append("  (empty)").append(nl);
        } else {
            for (int i = 0; i < config.rotation.size(); i++) {
                sb.append("  ").append(i).append(": ").append(config.rotation.get(i)).append(nl);
            }
        }
        String msg = sb.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return Command.SINGLE_SUCCESS;
    }

    private int cmdReset(CommandContext<CommandSourceStack> ctx) {
        config = new Config();
        normalizeConfig();
        tickCounter = 0;
        rotationIndex = 0;
        ConfigManager.save(config);
        if (serverRef != null) {
            applyAll(serverRef);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Survival Stats config reset to defaults and saved."), true);
        return Command.SINGLE_SUCCESS;
    }

    private int cmdInterval(CommandContext<CommandSourceStack> ctx) {
        int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
        config.rotationIntervalTicks = ticks;
        tickCounter = 0;
        ConfigManager.save(config);
        if (serverRef != null) {
            applyAll(serverRef);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Rotation interval set to " + ticks + " ticks."), true);
        return Command.SINGLE_SUCCESS;
    }

    private int setDistanceUnit(CommandContext<CommandSourceStack> ctx, String unit) {
        config.distanceUnit = unit;
        ConfigManager.save(config);
        if (serverRef != null) {
            syncDerivedObjectives(serverRef);
            applyAll(serverRef);
        }
        String label = unit.equals("imperial") ? "imperial (miles)" : "metric (kilometers)";
        ctx.getSource().sendSuccess(() -> Component.literal("Distance unit set to " + label + "."), true);
        return Command.SINGLE_SUCCESS;
    }

    private int clearTabListSlot(CommandContext<CommandSourceStack> ctx) {
        config.tabListObjective = "";
        ConfigManager.save(config);
        if (serverRef != null) {
            serverRef.getScoreboard().setDisplayObjective(DisplaySlot.LIST, null);
            applyCurrentSidebar(serverRef);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Tab list scoreboard display cleared."), true);
        return Command.SINGLE_SUCCESS;
    }

    private int setTabListObjective(CommandContext<CommandSourceStack> ctx, String objective) {
        config.tabListObjective = objective;
        ConfigManager.save(config);
        if (serverRef != null) {
            applyAll(serverRef);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Tab list objective set to " + objective + "."), true);
        return Command.SINGLE_SUCCESS;
    }

    private int clearBelowNameSlot(CommandContext<CommandSourceStack> ctx) {
        config.belowNameObjective = "";
        ConfigManager.save(config);
        if (serverRef != null) {
            serverRef.getScoreboard().setDisplayObjective(DisplaySlot.BELOW_NAME, null);
            applyCurrentSidebar(serverRef);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Below-name scoreboard display cleared."), true);
        return Command.SINGLE_SUCCESS;
    }

    private int setBelowNameObjective(CommandContext<CommandSourceStack> ctx, String objective) {
        config.belowNameObjective = objective;
        ConfigManager.save(config);
        if (serverRef != null) {
            applyAll(serverRef);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Below-name objective set to " + objective + "."), true);
        return Command.SINGLE_SUCCESS;
    }

    private int cmdRotationList(CommandContext<CommandSourceStack> ctx) {
        String nl = "\n";
        StringBuilder sb = new StringBuilder();
        sb.append("Sidebar rotation:").append(nl);
        if (config.rotation.isEmpty()) {
            sb.append("  (empty)").append(nl);
        } else {
            for (int i = 0; i < config.rotation.size(); i++) {
                sb.append("  ").append(i).append(": ").append(config.rotation.get(i)).append(nl);
            }
        }
        String msg = sb.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return Command.SINGLE_SUCCESS;
    }

    private int cmdRotationClear(CommandContext<CommandSourceStack> ctx) {
        config.rotation.clear();
        normalizeRotationIndex();
        ConfigManager.save(config);
        if (serverRef != null) {
            applyCurrentSidebar(serverRef);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Sidebar rotation cleared."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static String blankAsNone(String s) {
        return (s == null || s.isBlank()) ? "(none)" : s;
    }

    private static void normalizeConfig() {
        if (config.distanceUnit == null || config.distanceUnit.isBlank()) {
            config.distanceUnit = "metric";
            return;
        }
        String normalized = config.distanceUnit.trim().toLowerCase();
        if (!normalized.equals("metric") && !normalized.equals("imperial")) {
            config.distanceUnit = "metric";
        } else {
            config.distanceUnit = normalized;
        }
    }

    private static void normalizeRotationIndex() {
        if (config.rotation.isEmpty()) {
            rotationIndex = 0;
        } else {
            rotationIndex = rotationIndex % config.rotation.size();
        }
    }

    private void onServerStarted(MinecraftServer server) {
        serverRef = server;
        ensureObjectives(server);
        syncDerivedObjectives(server);
        applyStaticDisplays(server);
        applyCurrentSidebar(server);
        LOGGER.info("Survival Stats ready. Rotating {} stats every {} ticks.",
            config.rotation.size(), config.rotationIntervalTicks);
    }

    private void onServerTick(MinecraftServer server) {
        derivedSyncCounter++;
        if (derivedSyncCounter >= DERIVED_SYNC_INTERVAL_TICKS) {
            derivedSyncCounter = 0;
            syncDerivedObjectives(server);
        }

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

    private void applyCurrentSidebar(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        if (config.rotation.isEmpty()) {
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
            return;
        }
        rotationIndex = rotationIndex % config.rotation.size();
        String name = config.rotation.get(rotationIndex);
        Objective obj = scoreboard.getObjective(name);
        if (obj != null) {
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, obj);
        }
    }

    private void applyAll(MinecraftServer server) {
        ensureObjectives(server);
        syncDerivedObjectives(server);
        applyStaticDisplays(server);
        applyCurrentSidebar(server);
    }

    private void syncDerivedObjectives(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective distanceRaw = scoreboard.getObjective(DISTANCE_RAW_OBJECTIVE);
        Objective playTimeRaw = scoreboard.getObjective(PLAYTIME_RAW_OBJECTIVE);
        Objective distanceOut = scoreboard.getObjective(DISTANCE_OBJECTIVE);
        Objective playTimeOut = scoreboard.getObjective(PLAYTIME_OBJECTIVE);
        if (distanceRaw == null || playTimeRaw == null || distanceOut == null || playTimeOut == null) {
            return;
        }

        boolean imperial = "imperial".equals(config.distanceUnit);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ScoreAccess rawDistanceScore = scoreboard.getOrCreatePlayerScore(player, distanceRaw);
            int rawCm = rawDistanceScore.get();
            int convertedDistance = imperial
                ? (int) Math.round(rawCm / 1609.344D)
                : (int) Math.round(rawCm / 100000.0D);
            scoreboard.getOrCreatePlayerScore(player, distanceOut).set(convertedDistance);

            ScoreAccess rawPlayTimeScore = scoreboard.getOrCreatePlayerScore(player, playTimeRaw);
            int ticks = rawPlayTimeScore.get();
            int totalSeconds = ticks / 20;
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            int seconds = totalSeconds % 60;
            int encoded = (hours * 10000) + (minutes * 100) + seconds;
            scoreboard.getOrCreatePlayerScore(player, playTimeOut).set(encoded);
        }
    }
}
