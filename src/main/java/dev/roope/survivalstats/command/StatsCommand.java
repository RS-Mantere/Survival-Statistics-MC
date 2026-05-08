package dev.roope.survivalstats.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.roope.survivalstats.Config;
import dev.roope.survivalstats.ConfigManager;
import dev.roope.survivalstats.DistanceUnit;
import dev.roope.survivalstats.LeaderboardService;
import dev.roope.survivalstats.StatDef;
import dev.roope.survivalstats.StatReader;
import dev.roope.survivalstats.StatSnapshot;
import dev.roope.survivalstats.SurvivalStats;
import dev.roope.survivalstats.SurvivalStatsService;
import dev.roope.survivalstats.display.StatFormatter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public final class StatsCommand {

    private StatsCommand() {
    }

    public static void register(
        CommandDispatcher<CommandSourceStack> dispatcher,
        Supplier<SurvivalStatsService> serviceProvider
    ) {
        dispatcher.register(Commands.literal("stats")
            .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))

            .then(Commands.literal("show")
                .executes(ctx -> show(ctx, serviceProvider)))

            .then(Commands.literal("reload")
                .executes(ctx -> reload(ctx, serviceProvider)))

            .then(Commands.literal("reset")
                .executes(ctx -> reset(ctx, serviceProvider)))

            .then(Commands.literal("interval")
                .then(Commands.argument("ticks", IntegerArgumentType.integer(Config.MIN_INTERVAL_TICKS))
                    .executes(ctx -> setInterval(ctx, serviceProvider,
                        IntegerArgumentType.getInteger(ctx, "ticks")))))

            .then(Commands.literal("units")
                .then(Commands.literal("metric")
                    .executes(ctx -> setUnit(ctx, serviceProvider, DistanceUnit.METRIC)))
                .then(Commands.literal("imperial")
                    .executes(ctx -> setUnit(ctx, serviceProvider, DistanceUnit.IMPERIAL))))

            .then(Commands.literal("display")
                .then(Commands.literal("on").executes(ctx -> setDisplay(ctx, serviceProvider, true)))
                .then(Commands.literal("off").executes(ctx -> setDisplay(ctx, serviceProvider, false))))

            .then(Commands.literal("refresh")
                .executes(ctx -> refresh(ctx, serviceProvider)))

            .then(Commands.literal("top")
                .then(Commands.argument("stat", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(StatDef.BY_ID.keySet(), builder))
                    .executes(ctx -> top(
                        ctx,
                        serviceProvider,
                        StringArgumentType.getString(ctx, "stat"),
                        10
                    ))
                    .then(Commands.argument("count", IntegerArgumentType.integer(1))
                        .executes(ctx -> top(
                            ctx,
                            serviceProvider,
                            StringArgumentType.getString(ctx, "stat"),
                            IntegerArgumentType.getInteger(ctx, "count")
                        ))))));
    }

    // --- handlers ------------------------------------------------------------

    private static int show(CommandContext<CommandSourceStack> ctx, Supplier<SurvivalStatsService> sp) {
        SurvivalStatsService service = require(sp, ctx);
        if (service == null) return 0;
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("This command requires a player context."));
            return 0;
        }

        StatSnapshot snapshot = new StatReader().read(player);
        Config config = service.config();
        StringBuilder sb = new StringBuilder();
        sb.append("Survival Stats:\n");
        sb.append("  refreshIntervalTicks: ").append(config.refreshIntervalTicks()).append('\n');
        sb.append("  displayEnabled: ").append(config.displayEnabled()).append('\n');
        sb.append("  distanceUnit: ").append(config.distanceUnit().configValue())
          .append(" (").append(config.distanceUnit().displayName()).append(")\n");
        sb.append('\n');
        for (StatDef def : StatDef.USER_STATS) {
            sb.append("  ").append(StatFormatter.formatRow(def, snapshot, config.distanceUnit())).append('\n');
        }
        String msg = sb.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx, Supplier<SurvivalStatsService> sp) {
        SurvivalStatsService service = require(sp, ctx);
        if (service == null) return 0;
        Config newConfig = ConfigManager.loadOrCreate();
        service.replaceConfig(newConfig);
        service.renderForAllPlayers(true);
        ctx.getSource().sendSuccess(() -> Component.literal("Survival Stats config reloaded."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx, Supplier<SurvivalStatsService> sp) {
        SurvivalStatsService service = require(sp, ctx);
        if (service == null) return 0;
        Config defaults = new Config();
        defaults.normalize();
        service.replaceConfig(defaults);
        if (!persist(ctx, defaults)) return 0;
        service.renderForAllPlayers(true);
        ctx.getSource().sendSuccess(
            () -> Component.literal("Survival Stats config reset to defaults and saved."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setInterval(
        CommandContext<CommandSourceStack> ctx, Supplier<SurvivalStatsService> sp, int ticks
    ) {
        SurvivalStatsService service = require(sp, ctx);
        if (service == null) return 0;
        Config config = service.config();
        service.setRefreshIntervalTicks(ticks);
        if (!persist(ctx, config)) return 0;
        ctx.getSource().sendSuccess(
            () -> Component.literal("Refresh interval set to " + config.refreshIntervalTicks() + " ticks."),
            true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setUnit(
        CommandContext<CommandSourceStack> ctx, Supplier<SurvivalStatsService> sp, DistanceUnit unit
    ) {
        SurvivalStatsService service = require(sp, ctx);
        if (service == null) return 0;
        Config config = service.config();
        config.setDistanceUnit(unit);
        if (!persist(ctx, config)) return 0;
        service.renderForAllPlayers(true);
        ctx.getSource().sendSuccess(
            () -> Component.literal("Distance unit set to " + unit.configValue()
                + " (" + unit.displayName() + ")."),
            true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setDisplay(
        CommandContext<CommandSourceStack> ctx, Supplier<SurvivalStatsService> sp, boolean enabled
    ) {
        SurvivalStatsService service = require(sp, ctx);
        if (service == null) return 0;
        service.setDisplayEnabled(enabled);
        if (!persist(ctx, service.config())) return 0;
        ctx.getSource().sendSuccess(
            () -> Component.literal("Tab-list statistics display " + (enabled ? "enabled." : "disabled.")), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int refresh(CommandContext<CommandSourceStack> ctx, Supplier<SurvivalStatsService> sp) {
        SurvivalStatsService service = require(sp, ctx);
        if (service == null) return 0;
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            service.renderForAllPlayers(true);
            ctx.getSource().sendSuccess(() -> Component.literal("Refreshed stats display for all online players."), true);
            return Command.SINGLE_SUCCESS;
        }
        service.renderForPlayer(player, true);
        ctx.getSource().sendSuccess(() -> Component.literal("Refreshed your stats display."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int top(
        CommandContext<CommandSourceStack> ctx,
        Supplier<SurvivalStatsService> sp,
        String statId,
        int count
    ) {
        SurvivalStatsService service = require(sp, ctx);
        if (service == null) return 0;
        StatDef def = StatDef.BY_ID.get(statId);
        if (def == null) {
            ctx.getSource().sendFailure(
                Component.literal("Unknown stat '" + statId + "'."));
            return 0;
        }
        LeaderboardService leaderboards = new LeaderboardService(service.server());
        List<LeaderboardService.PlayerStatValue> top = leaderboards.top(def, count);
        if (top.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No player stats found yet."));
            return 0;
        }
        StringBuilder sb = new StringBuilder("Top " + Math.min(count, top.size()) + " " + def.displayName() + ":\n");
        for (int i = 0; i < top.size(); i++) {
            LeaderboardService.PlayerStatValue row = top.get(i);
            sb.append("  ").append(i + 1).append(". ").append(row.name()).append(" - ")
                .append(StatFormatter.formatValue(def, row.value(), service.config().distanceUnit()))
                .append('\n');
        }
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return Command.SINGLE_SUCCESS;
    }

    // --- helpers -------------------------------------------------------------

    private static SurvivalStatsService require(
        Supplier<SurvivalStatsService> sp, CommandContext<CommandSourceStack> ctx
    ) {
        SurvivalStatsService service = sp.get();
        if (service == null) {
            ctx.getSource().sendFailure(
                Component.literal("Survival Stats service is not ready yet."));
        }
        return service;
    }

    private static boolean persist(CommandContext<CommandSourceStack> ctx, Config config) {
        try {
            ConfigManager.save(config);
            return true;
        } catch (IOException e) {
            SurvivalStats.LOGGER.error("Failed to save Survival Stats config", e);
            ctx.getSource().sendFailure(Component.literal(
                "Failed to save config: " + e.getClass().getSimpleName() + ": " + e.getMessage()));
            return false;
        }
    }
}
