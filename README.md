# Survival Stats

Server-side Fabric mod for Minecraft 1.21.11 that shows player statistics in the tab-list UI (`header/footer`) instead of using scoreboard display slots.

No client mod required.

## Features

- No scoreboard display dependencies (no sidebar/list/below-name rendering).
- Uses vanilla per-player stats storage (`world/stats/<uuid>.json`) as the data source.
- Renders a multiline stats panel in tab-list footer for each online player.
- Configurable refresh interval and distance unit.
- Optional global on/off switch for the tab-list overlay.
- `/stats top <stat> [count]` supports offline players by reading stats JSON files.
- Crash-safe config writes and corrupt-config backup behavior.

## Commands

All `/stats` commands require op level 2.

| Command | Description |
|--------|-------------|
| `/stats show` | Show config + your current stat values in chat. |
| `/stats display on` | Enable tab-list stats rendering globally. |
| `/stats display off` | Disable rendering and clear tab-list header/footer. |
| `/stats interval <ticks>` | Set refresh interval (minimum 1). |
| `/stats units metric` | Show distance as meters. |
| `/stats units imperial` | Show distance as feet. |
| `/stats refresh` | Force a fresh render (player-specific if run by player). |
| `/stats reload` | Reload config from disk and re-render. |
| `/stats reset` | Reset config to defaults and re-render. |
| `/stats top <stat> [count]` | Show leaderboard from online + offline stats files. |

Valid `stat` values for `/stats top`:
`Deaths`, `PlayerKills`, `MobKills`, `Jumps`, `Distance`, `PlayTime`, `DmgDealt`, `DmgTaken`, `Sleeps`, `Fish`.

## Config

File: `config/survivalstats/config.json`

```json
{
  "refreshIntervalTicks": 20,
  "displayEnabled": true,
  "distanceUnit": "metric"
}
```

Legacy config keys from older scoreboard-based versions are safely ignored/migrated.

## Migration note from scoreboard-based versions

On server start, the mod clears scoreboard display slots (`SIDEBAR`, `LIST`, `BELOW_NAME`) once so legacy UI state is removed.

The mod does **not** delete old scoreboard objectives. If you want to purge them, do it manually with vanilla `/scoreboard objectives remove ...`.

## Building

### Option A: GitHub Actions (recommended)

1. Push this repo to GitHub.
2. The workflow at `.github/workflows/build.yml` runs automatically.
3. Download the built jar from the workflow run's Artifacts section.
4. To create a tagged release, push a tag like `v1.1.0`. The jar attaches to the GitHub Release.

### Option B: Build locally

Requires JDK 21 and Gradle **9.2+** (matches the Loom 1.14 plugin used in CI).

```
gradle wrapper --gradle-version 9.2.0
./gradlew build
```

The jar appears in `build/libs/survivalstats-1.1.0.jar`.

### Running tests

```
./gradlew test
```

## Installing

1. Drop `survivalstats-1.1.0.jar` and `fabric-api-0.141.3+1.21.11.jar` into the server's `mods/` folder.
2. Start the server.
3. Stats start tracking immediately. Press and hold tab to see the custom stats panel.

## License

MIT.
