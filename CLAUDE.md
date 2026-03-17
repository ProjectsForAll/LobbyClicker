# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LobbyClicker is a Minecraft Bukkit/Spigot plugin — "Cookie Clicker, but for Minecraft lobbies." Players click a cookie in a GUI to earn cookies, buy upgrades for cookies-per-click (CPC) and cookies-per-second (CPS), and compete on a leaderboard. Targets Spigot 1.21.11 with Folia support, depends on [BukkitOfUtils](https://github.com/Streamline-Essentials/BukkitOfUtils) (1.18.0).

## Build Commands

```bash
# Build the plugin JAR (output: target/LobbyClicker-<version>.jar)
./gradlew shadowJar

# Clean build
./gradlew clean shadowJar
```

There are no tests or linting configured in this project.

## Architecture

### Framework Dependency

The plugin extends `BetterPlugin` (from BukkitOfUtils/TheBase), not Bukkit's `JavaPlugin` directly. Configuration classes extend `SimpleConfiguration`, event listeners implement `ListenerConglomerate`, database operations extend `DBOperator`, and GUIs extend ObliviateInvs' `Gui` class (`mc.obliviate.inventory.Gui`).

### Component Wiring

`LobbyClicker.java` is the entry point with static references to all components, wired in `onEnable()`:
- **MainConfig / DatabaseConfig** — YAML-backed configs
- **ClickerOperator** — database layer for player cookie/upgrade data
- **MainListener** — player join/quit lifecycle
- **CookieTask** — repeating task (every 1s) that adds CPS cookies and auto-saves every 5 minutes
- **Commands** — `/clicker`, `/leaderboard`, `/clickeradmin`
- **ClickerPlaceholders** — PlaceholderAPI expansion (registered if PAPI is present)

### Player Data Flow

1. `PlayerJoinEvent` → `PlayerManager.getOrCreatePlayer()` → loads from DB or creates new `PlayerData`
2. Player data lives in a `ConcurrentSkipListSet` in `PlayerManager`
3. `CookieTask` runs every second: adds CPS cookies to online players, auto-saves every 5 min
4. `PlayerQuitEvent` → `saveAndUnload()` → persists to DB and removes from memory

### Cookie Clicker Game System

- **PlayerData** holds: cookies, totalCookiesEarned, and an `EnumMap<UpgradeType, Integer>` of upgrade counts
- **UpgradeType** enum defines 8 upgrades (Cursor, Grandma, Farm, Mine, Factory, Bank, Temple, Click Power) with base costs, cost multipliers (cost = baseCost * multiplier^owned), CPS and CPC per level
- **Upgrades** are serialized as semicolon-separated `TYPE:count` strings for DB storage

### GUI System (ObliviateInvs)

- **ClickerGui** — Main screen: clickable cookie center, stats display, upgrade/leaderboard buttons
- **UpgradeGui** — Shows all 8 upgrades with costs, owned count, effects; click to buy
- **LeaderboardGui** — Async-fetched top 10 players with player heads

### Database Layer

`Statements.java` contains dual SQL dialects (MySQL and SQLite) with template variables (`%database%`, `%table_prefix%`). Players table stores: Uuid, Name, Cookies, TotalCookiesEarned, Upgrades (TEXT).

### PlaceholderAPI

Placeholders: `%lobbyclicker_cookies%`, `%lobbyclicker_cookies_raw%`, `%lobbyclicker_total_cookies%`, `%lobbyclicker_total_cookies_raw%`, `%lobbyclicker_cps%`, `%lobbyclicker_cpc%`

## Build Configuration Notes

- `build.gradle` uses Shadow plugin for fat JAR in `target/`
- `dependencies.gradle` declares all dependencies separately
- `plugin.yml` uses Gradle property tokens (`${name}`, `${version}`, `${main}`) replaced at build time
- Plugin main class auto-derived from `group` + `name` in `gradle.properties` when set to "default"
- JitPack repo required for transitive deps (TheBase, UniversalScheduler, obliviate-invs)
