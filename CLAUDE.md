# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LobbyClicker is a Minecraft Bukkit/Spigot plugin — "Cookie Clicker, but for Minecraft lobbies." Players click a cookie in a GUI to earn cookies, buy upgrades, compete on a leaderboard, visit friends' realms, gamble, and trade cookies. Targets Spigot 1.21.11, depends on BukkitOfUtils (1.18.0).

**Important context:** Everything non-admin should be done via the `/clicker` GUI — no new player-facing commands.

## Build Commands

```bash
./gradlew shadowJar          # output: target/LobbyClicker-<version>.jar
./gradlew clean shadowJar    # clean build
```

No tests or linting configured.

## Architecture

### Framework Dependency
Extends `BetterPlugin` (BukkitOfUtils/TheBase). Configs extend `SimpleConfiguration`, event listeners implement `ListenerConglomerate`, DB operations extend `DBOperator`, GUIs extend `mc.obliviate.inventory.Gui`. All GUI icons use the shared `GuiHelper` utility class.

### Component Wiring
`LobbyClicker.java` is the entry point with static references:
- **MainConfig / DatabaseConfig** — YAML-backed configs
- **ClickerOperator** — DB layer for all data (player, friends, bans, blocks)
- **MainListener** — player join/quit lifecycle + social cleanup
- **CookieTask** — 1s repeating task: CPS cookies, auto-save, milestone notifications, transaction cleanup

### Database Schema (5 tables)
- **Players** — Uuid, Name, Cookies, TotalCookiesEarned, TimesClicked, Upgrades, Settings, RealmPublic
- **Friends** — Uuid1, Uuid2, Since (bidirectional — A→B and B→A rows)
- **FriendRequests** — Sender, Receiver, SentAt
- **Bans** — Owner, Banned
- **Blocks** — Owner, Blocked

Schema migration runs in `ensureTables()` — checks existing columns via ResultSetMetaData before ALTER.

### Player Data Flow
1. Join → `PlayerManager.getOrCreatePlayer()` → loads from DB → `augment()` chains social data loading (friends/bans/blocks/requests)
2. In-memory: `ConcurrentSkipListSet<PlayerData>` in PlayerManager, social sets are `ConcurrentHashMap.newKeySet()`
3. Quit → save + unload + cleanup realm viewers + cleanup pending transactions

### Settings System
- `SettingType` enum: 27 settings (12 sound toggles, 11 volumes, 4 other)
- `PlayerSettings` class: EnumMap with serialize/deserialize, only stores non-default values
- Master sound toggle controls all sounds; individual toggles are AND'd with master

### GUI Navigation (from `/clicker`)
```
ClickerGui (main clicker + golden cookies)
├── [45] Social → SocialMainGui
│   ├── Friends → FriendsListGui → PlayerActionGui (visit/unfriend/pay/gamble)
│   ├── Realm Viewers → RealmViewersGui → PlayerActionGui
│   ├── All Players → AllPlayersGui → PlayerActionGui
│   ├── Bans → BanListGui (unban)
│   └── Blocks → BlockListGui (unblock)
├── [46] Settings → SettingsMainGui → Sound/Volume/Other GUIs
├── [48] Upgrades → UpgradeGui
├── [50] Leaderboard → LeaderboardGui
└── [53] Close
```

### Realm System
A "realm" = another player's ClickerGui in visitor mode. Visitors can only click (credits go to owner), upgrades/settings are hidden. Access: public realm OR (friend + allow_friend_joins), not banned/blocked. `RealmManager` tracks viewers in `ConcurrentHashMap<ownerUuid, Set<viewerUuid>>`.

### Gambling & Payments
- `PaymentGui` / `GambleGui` — amount picker with ±1/10/100/1K/10K buttons
- `PendingTransaction` — in-memory with 60s expiry, auto-cleaned by CookieTask
- Opening `/clicker` with a pending bet shows `GambleAcceptGui` first

### Notifications (CookieTask)
- 10s milestone: every 10 ticks, compares digit count of cookies/totalCookies to snapshot
- Realm join/leave sounds via RealmManager
- Click-on-realm sounds for owner
- All respect individual settings + master toggle + volume

## Build Configuration Notes
- Shadow plugin for fat JAR in `target/`
- `dependencies.gradle` declares deps separately
- `plugin.yml` uses Gradle property tokens, api-version 1.21
- JitPack repo required for transitive deps
