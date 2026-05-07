# Survival Stats

A server-side Fabric mod for Minecraft 1.21.11. Tracks scoreboard stats and rotates a sidebar display through them. Players need no client mod.

## Features

- Tracks Deaths, Player Kills, Mob Kills, Jumps, Distance Walked, Playtime, Damage Dealt, Damage Taken, Sleeps, Fish Caught.
- Auto-creates all scoreboard objectives on first server start.
- Rotates the sidebar through configured stats every 5 seconds (configurable).
- Shows Deaths in the tab list and Mob Kills below player nameplates.
- Config file at `config/survivalstats/config.json`.
- In-game `/stats` commands to view and edit the config (saved automatically).

## Commands

All `/stats` commands require **op level 2** (game masters / `Commands.LEVEL_GAMEMASTERS`).

| Command | Description |
|--------|---------------|
| `/stats show` | Print interval, tab/below-name objectives, and full rotation list. |
| `/stats reload` | Reload config from disk and reapply displays. |
| `/stats rotate` | Manually advance the sidebar to the next objective in the rotation. |
| `/stats units metric` | Use kilometers for `Distance` conversion. |
| `/stats units imperial` | Use miles for `Distance` conversion. |
| `/stats reset` | Reset config to defaults, save, and reapply. |
| `/stats interval <ticks>` | Set rotation interval (minimum 1 tick). Resets the tick counter; rotation index is unchanged. |
| `/stats slot tab <objective>` | Set the tab list scoreboard to this objective. |
| `/stats slot tab none` | Clear the tab list display slot. |
| `/stats slot belowname <objective>` | Set the below-name scoreboard to this objective. |
| `/stats slot belowname none` | Clear the below-name display slot. |
| `/stats rotation list` | Print the rotation list with indices (same info as under `rotation` in `/stats show`). |
| `/stats rotation add <objective>` | Append an objective to the rotation. |
| `/stats rotation remove <objective>` | Remove the first matching objective from the rotation. |
| `/stats rotation insert <index> <objective>` | Insert at index (clamped to `0` … `size`). |
| `/stats rotation clear` | Clear the rotation (sidebar slot cleared; ticking stops until you add entries again). |
| `/stats rotation set <objectives...>` | Replace the rotation with space-separated objective names (greedy; rest of the line). |

Mutating commands write `config/survivalstats/config.json` immediately and reapply tab/below-name/sidebar where relevant. Objective arguments accept any scoreboard objective name; tab completion includes the mod’s default stats plus objectives already on the server.

`Distance` and `PlayTime` are derived display objectives:
- `Distance` is shown as whole kilometers (`metric`) or whole miles (`imperial`).
- `PlayTime` is encoded as `HHmmss` numeric format for sidebar compatibility (example: `021530` = 02:15:30).

## Building

### Option A: GitHub Actions (recommended)

1. Push this repo to GitHub.
2. The workflow at `.github/workflows/build.yml` runs automatically.
3. Download the built jar from the workflow run's Artifacts section.
4. To create a tagged release, push a tag like `v1.0.0`. The jar attaches to the GitHub Release.

### Option B: Build locally

Requires JDK 21 and Gradle **9.2+** (matches the Loom 1.14 plugin used in CI).

```
gradle wrapper --gradle-version 9.2.0
./gradlew build
```

The jar appears in `build/libs/survivalstats-1.0.0.jar`.

## Installing

1. Drop `survivalstats-1.0.0.jar` and `fabric-api-0.141.3+1.21.11.jar` into the server's `mods/` folder.
2. Start the server.
3. Stats start tracking immediately. The sidebar begins rotating within 5 seconds.

## Configuration

After first launch, edit `config/survivalstats/config.json` or use `/stats` in-game.

```json
{
  "rotationIntervalTicks": 100,
  "distanceUnit": "metric",
  "tabListObjective": "Deaths",
  "belowNameObjective": "MobKills",
  "rotation": [
    "Deaths",
    "PlayerKills",
    "MobKills",
    "Distance",
    "PlayTime"
  ]
}
```

20 ticks equals 1 second. Default 100 ticks equals 5 seconds.

Run `/stats reload` after hand-editing the file.

## Available objective IDs

`Deaths`, `PlayerKills`, `MobKills`, `Jumps`, `Distance`, `PlayTime`, `DmgDealt`, `DmgTaken`, `Sleeps`, `Fish`.

## License

MIT.
