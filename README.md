# Survival Stats

A server-side Fabric mod for Minecraft 1.21.11. Tracks scoreboard stats and rotates a sidebar display through them. Players need no client mod.

## Features

- Tracks Deaths, Player Kills, Mob Kills, Jumps, Distance Walked, Playtime, Damage Dealt, Damage Taken, Sleeps, Fish Caught.
- Auto-creates all scoreboard objectives on first server start.
- Rotates the sidebar through configured stats every 5 seconds (configurable).
- Shows Deaths in the tab list and Mob Kills below player nameplates.
- Config file at `config/survivalstats/config.json`.
- Admin command `/stats reload` to reapply config without restart.
- Admin command `/stats rotate` to manually advance the sidebar.

## Building

### Option A: GitHub Actions (recommended)

1. Push this repo to GitHub.
2. The workflow at `.github/workflows/build.yml` runs automatically.
3. Download the built jar from the workflow run's Artifacts section.
4. To create a tagged release, push a tag like `v1.0.0`. The jar attaches to the GitHub Release.

### Option B: Build locally

Requires JDK 21 and Gradle 8.14+.

```
gradle wrapper --gradle-version 8.14
./gradlew build
```

The jar appears in `build/libs/survivalstats-1.0.0.jar`.

## Installing

1. Drop `survivalstats-1.0.0.jar` and `fabric-api-0.141.3+1.21.11.jar` into the server's `mods/` folder.
2. Start the server.
3. Stats start tracking immediately. The sidebar begins rotating within 5 seconds.

## Configuration

After first launch, edit `config/survivalstats/config.json`.

```json
{
  "rotationIntervalTicks": 100,
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

Run `/stats reload` after editing.

## Available objective IDs

`Deaths`, `PlayerKills`, `MobKills`, `Jumps`, `Distance`, `PlayTime`, `DmgDealt`, `DmgTaken`, `Sleeps`, `Fish`.

## License

MIT.
