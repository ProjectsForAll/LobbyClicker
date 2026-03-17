package gg.drak.lobbyclicker.database;

import gg.drak.thebase.async.AsyncUtils;
import host.plas.bou.sql.DBOperator;
import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.math.CookieMath;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.realm.RealmRole;

import host.plas.bou.sql.ConnectorSet;
import host.plas.bou.sql.DatabaseType;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class ClickerOperator extends DBOperator {
    public ClickerOperator() {
        super(LobbyClicker.getDatabaseConfig().getConnectorSet(), LobbyClicker.getInstance());
    }

    public ClickerOperator(ConnectorSet connectorSet) {
        super(connectorSet, LobbyClicker.getInstance());
    }

    @Override
    public void ensureTables() {
        // Create new-schema tables
        execute(Statements.getStatement(Statements.StatementType.CREATE_TABLES, getConnectorSet()), stmt -> {});
        execute(Statements.getStatement(Statements.StatementType.CREATE_PROFILES_TABLE, getConnectorSet()), stmt -> {});
        execute(Statements.getStatement(Statements.StatementType.CREATE_PROFILE_ROLES_TABLE, getConnectorSet()), stmt -> {});
        execute(Statements.getStatement(Statements.StatementType.CREATE_PROFILE_BANS_TABLE, getConnectorSet()), stmt -> {});
        execute(Statements.getStatement(Statements.StatementType.CREATE_FRIENDS_TABLE, getConnectorSet()), stmt -> {});
        execute(Statements.getStatement(Statements.StatementType.CREATE_FRIEND_REQUESTS_TABLE, getConnectorSet()), stmt -> {});
        execute(Statements.getStatement(Statements.StatementType.CREATE_BANS_TABLE, getConnectorSet()), stmt -> {});
        execute(Statements.getStatement(Statements.StatementType.CREATE_BLOCKS_TABLE, getConnectorSet()), stmt -> {});

        // Add missing columns to Players table (for migration)
        String prefix = getConnectorSet().getTablePrefix();
        Set<String> existingPlayerCols = getColumnNames(prefix + "Players");

        if (!existingPlayerCols.contains("ActiveProfileId")) {
            execute("ALTER TABLE `" + prefix + "Players` ADD COLUMN `ActiveProfileId` " +
                    (getConnectorSet().getType() == DatabaseType.MYSQL ? "VARCHAR(36)" : "TEXT") +
                    " NOT NULL DEFAULT '';", stmt -> {});
            LobbyClicker.getInstance().logInfo("Added ActiveProfileId column to Players table.");
        }
        if (!existingPlayerCols.contains("Settings") && existingPlayerCols.contains("Cookies")) {
            // Old schema had Settings in Players table already, but just in case
            execute("ALTER TABLE `" + prefix + "Players` ADD COLUMN `Settings` TEXT NOT NULL DEFAULT '';", stmt -> {});
        }

        // --- AUTO-MIGRATION: old Players table (with Cookies column) to new Profiles table ---
        if (existingPlayerCols.contains("Cookies")) {
            migrateOldPlayersToProfiles(prefix);
        }
    }

    /**
     * Migrates data from the old flat Players table (which had Cookies, Upgrades, etc. inline)
     * into the new Profiles table. Creates one "Main" profile per player.
     */
    private void migrateOldPlayersToProfiles(String prefix) {
        // Check if any profiles exist already — if so, migration was done
        boolean[] hasProfiles = {false};
        executeQuery("SELECT COUNT(*) as cnt FROM `" + prefix + "Profiles`;", stmt -> {}, rs -> {
            try { if (rs.next() && rs.getInt("cnt") > 0) hasProfiles[0] = true; } catch (Throwable ignored) {}
        });
        if (hasProfiles[0]) return;

        LobbyClicker.getInstance().logInfo("Migrating old Players table data to Profiles table...");

        // Read all old player data
        List<Map<String, String>> oldPlayers = new ArrayList<>();
        executeQuery("SELECT * FROM `" + prefix + "Players`;", stmt -> {}, rs -> {
            try {
                java.sql.ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, String> row = new HashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        row.put(meta.getColumnName(i), rs.getString(i));
                    }
                    oldPlayers.add(row);
                }
            } catch (Throwable e) {
                LobbyClicker.getInstance().logWarning("Failed to read old players for migration", e);
            }
        });

        int migrated = 0;
        for (Map<String, String> row : oldPlayers) {
            String uuid = row.get("Uuid");
            if (uuid == null) continue;

            // Only migrate if this player has realm data (Cookies column exists in their row)
            String cookies = row.getOrDefault("Cookies", "0");
            String totalEarned = row.getOrDefault("TotalCookiesEarned", "0");
            String timesClicked = row.getOrDefault("TimesClicked", "0");
            String upgrades = row.getOrDefault("Upgrades", "");
            String prestige = row.getOrDefault("PrestigeLevel", "0");
            String aura = row.getOrDefault("Aura", "0");
            String realmPublic = row.getOrDefault("RealmPublic", "0");
            String settings = row.getOrDefault("Settings", "");

            // Generate a profile ID for this player's "Main" profile
            String profileId = UUID.randomUUID().toString();

            // Insert profile
            String pushProfile = Statements.getStatement(Statements.StatementType.PUSH_PROFILE, getConnectorSet());
            execute(pushProfile, stmt -> {
                try {
                    stmt.setString(1, profileId);
                    stmt.setString(2, uuid);
                    stmt.setString(3, "Main");
                    stmt.setString(4, cookies);
                    stmt.setString(5, totalEarned);
                    stmt.setInt(6, CookieMath.digitCount(CookieMath.parse(totalEarned)));
                    stmt.setLong(7, Long.parseLong(timesClicked));
                    stmt.setString(8, upgrades);
                    stmt.setInt(9, Integer.parseInt(prestige));
                    stmt.setString(10, aura);
                    stmt.setInt(11, Integer.parseInt(realmPublic));
                } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to migrate profile for " + uuid, e);
                }
            });

            // Update player row with ActiveProfileId and slim down
            String pushPlayer = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_MAIN, getConnectorSet());
            execute(pushPlayer, stmt -> {
                try {
                    stmt.setString(1, uuid);
                    stmt.setString(2, row.getOrDefault("Name", ""));
                    stmt.setString(3, settings);
                    stmt.setString(4, profileId);
                } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to update player row for " + uuid, e);
                }
            });

            // Migrate old bans to profile bans
            Set<String> bans = new HashSet<>();
            String pullBans = Statements.getStatement(Statements.StatementType.PULL_BANS, getConnectorSet());
            executeQuery(pullBans, stmt -> { try { stmt.setString(1, uuid); } catch (Throwable ignored) {} }, rs -> {
                try { while (rs.next()) bans.add(rs.getString("Banned")); } catch (Throwable ignored) {}
            });
            for (String banned : bans) {
                String pushBan = Statements.getStatement(Statements.StatementType.PUSH_PROFILE_BAN, getConnectorSet());
                execute(pushBan, stmt -> {
                    try {
                        stmt.setString(1, profileId);
                        stmt.setString(2, banned);
                        stmt.setInt(3, 1); // shadow ban (preserves old behavior)
                    } catch (Throwable ignored) {}
                });
            }

            migrated++;
        }

        if (migrated > 0) {
            LobbyClicker.getInstance().logInfo("Migrated " + migrated + " player(s) to profile system.");
        }
    }

    private Set<String> getColumnNames(String tableName) {
        Set<String> cols = new HashSet<>();
        executeQuery("SELECT * FROM `" + tableName + "` LIMIT 0;", stmt -> {}, rs -> {
            try {
                java.sql.ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    cols.add(meta.getColumnName(i));
                }
            } catch (Throwable ignored) {}
        });
        return cols;
    }

    @Override
    public void ensureDatabase() {
        String s1 = Statements.getStatement(Statements.StatementType.CREATE_DATABASE, getConnectorSet());
        execute(s1, stmt -> {});
    }

    // ===================== PLAYER CRUD (slim: uuid, name, settings, activeProfileId) =====================

    public void putPlayer(PlayerData playerData) {
        putPlayer(playerData, true);
    }

    public void putPlayer(PlayerData playerData, boolean async) {
        if (async) {
            putPlayerThreaded(playerData);
        } else {
            putPlayerThreaded(playerData).join();
        }
    }

    public CompletableFuture<Void> putPlayerThreaded(PlayerData playerData) {
        return AsyncUtils.executeAsync(() -> {
            ensureUsable();
            String s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_MAIN, getConnectorSet());
            execute(s1, stmt -> {
                try {
                    stmt.setString(1, playerData.getIdentifier());
                    stmt.setString(2, playerData.getName());
                    stmt.setString(3, playerData.getSettings().serialize());
                    stmt.setString(4, playerData.getActiveProfileId() != null ? playerData.getActiveProfileId() : "");
                } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to push player", e);
                }
            });

            // Also save active profile
            RealmProfile profile = playerData.getActiveProfile();
            if (profile != null) {
                putProfileSync(profile);
            }
        });
    }

    public CompletableFuture<Optional<PlayerData>> pullPlayerThreaded(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();
            String s1 = Statements.getStatement(Statements.StatementType.PULL_PLAYER_MAIN, getConnectorSet());
            AtomicReference<Optional<PlayerData>> ref = new AtomicReference<>(Optional.empty());
            executeQuery(s1, stmt -> {
                try { stmt.setString(1, uuid); } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to set pull param", e);
                }
            }, rs -> {
                try {
                    if (rs.next()) {
                        String name = rs.getString("Name");
                        String settings = "";
                        try { settings = rs.getString("Settings"); } catch (Throwable ignored) {}
                        String activeProfileId = "";
                        try { activeProfileId = rs.getString("ActiveProfileId"); } catch (Throwable ignored) {}

                        PlayerData data = new PlayerData(uuid, name);
                        data.setSettings(new gg.drak.lobbyclicker.settings.PlayerSettings(settings));
                        data.setActiveProfileId(activeProfileId != null && !activeProfileId.isEmpty() ? activeProfileId : null);

                        ref.set(Optional.of(data));
                    }
                } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to read player result set", e);
                }
            });
            return ref.get();
        });
    }

    /**
     * Pull all players (slim data only). Used by migration commands.
     */
    public CompletableFuture<List<PlayerData>> pullAllPlayersThreaded() {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();
            String s1 = Statements.getStatement(Statements.StatementType.PULL_ALL_PLAYERS, getConnectorSet());
            List<PlayerData> players = new ArrayList<>();
            executeQuery(s1, stmt -> {}, rs -> {
                try {
                    while (rs.next()) {
                        String uuid = rs.getString("Uuid");
                        String name = rs.getString("Name");
                        String settings = "";
                        try { settings = rs.getString("Settings"); } catch (Throwable ignored) {}
                        String activeProfileId = "";
                        try { activeProfileId = rs.getString("ActiveProfileId"); } catch (Throwable ignored) {}
                        PlayerData data = new PlayerData(uuid, name);
                        data.setSettings(new gg.drak.lobbyclicker.settings.PlayerSettings(settings));
                        data.setActiveProfileId(activeProfileId);
                        players.add(data);
                    }
                } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to pull all players", e);
                }
            });
            return players;
        });
    }

    // ===================== PROFILE CRUD =====================

    public void putProfileSync(RealmProfile profile) {
        String s = Statements.getStatement(Statements.StatementType.PUSH_PROFILE, getConnectorSet());
        execute(s, stmt -> {
            try {
                stmt.setString(1, profile.getProfileId());
                stmt.setString(2, profile.getOwnerUuid());
                stmt.setString(3, profile.getProfileName());
                stmt.setString(4, profile.getCookies().toPlainString());
                stmt.setString(5, profile.getTotalCookiesEarned().toPlainString());
                stmt.setInt(6, profile.getTotalCookiesDigits());
                stmt.setLong(7, profile.getTimesClicked());
                stmt.setString(8, profile.serializeUpgrades());
                stmt.setInt(9, profile.getPrestigeLevel());
                stmt.setString(10, profile.getAura().toPlainString());
                stmt.setInt(11, profile.isRealmPublic() ? 1 : 0);
            } catch (Throwable e) {
                LobbyClicker.getInstance().logWarning("Failed to push profile", e);
            }
        });

        // Save roles
        for (Map.Entry<String, RealmRole> entry : profile.getRoles().entrySet()) {
            String rs = Statements.getStatement(Statements.StatementType.PUSH_PROFILE_ROLE, getConnectorSet());
            execute(rs, st -> {
                try {
                    st.setString(1, profile.getProfileId());
                    st.setString(2, entry.getKey());
                    st.setString(3, entry.getValue().name());
                } catch (Throwable ignored) {}
            });
        }
    }

    public CompletableFuture<Void> putProfileThreaded(RealmProfile profile) {
        return AsyncUtils.executeAsync(() -> {
            ensureUsable();
            putProfileSync(profile);
        });
    }

    public CompletableFuture<List<RealmProfile>> pullProfilesByOwnerThreaded(String ownerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();
            String s = Statements.getStatement(Statements.StatementType.PULL_PROFILES_BY_OWNER, getConnectorSet());
            List<RealmProfile> profiles = new ArrayList<>();
            executeQuery(s, stmt -> {
                try { stmt.setString(1, ownerUuid); } catch (Throwable ignored) {}
            }, rs -> {
                try {
                    while (rs.next()) {
                        RealmProfile p = readProfileFromResultSet(rs);
                        if (p != null) profiles.add(p);
                    }
                } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to pull profiles", e);
                }
            });

            // Load roles and bans for each profile
            for (RealmProfile p : profiles) {
                loadProfileRolesSync(p);
                loadProfileBansSync(p);
            }

            return profiles;
        });
    }

    public CompletableFuture<Optional<RealmProfile>> pullProfileThreaded(String profileId) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();
            String s = Statements.getStatement(Statements.StatementType.PULL_PROFILE, getConnectorSet());
            AtomicReference<Optional<RealmProfile>> ref = new AtomicReference<>(Optional.empty());
            executeQuery(s, stmt -> {
                try { stmt.setString(1, profileId); } catch (Throwable ignored) {}
            }, rs -> {
                try {
                    if (rs.next()) {
                        RealmProfile p = readProfileFromResultSet(rs);
                        if (p != null) ref.set(Optional.of(p));
                    }
                } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to pull profile", e);
                }
            });
            ref.get().ifPresent(p -> {
                loadProfileRolesSync(p);
                loadProfileBansSync(p);
            });
            return ref.get();
        });
    }

    private RealmProfile readProfileFromResultSet(java.sql.ResultSet rs) {
        try {
            String profileId = rs.getString("ProfileId");
            String ownerUuid = rs.getString("OwnerUuid");
            String profileName = rs.getString("ProfileName");
            RealmProfile p = new RealmProfile(profileId, ownerUuid, profileName);
            p.setCookies(CookieMath.parse(rs.getString("Cookies")));
            p.setTotalCookiesEarned(CookieMath.parse(rs.getString("TotalCookiesEarned")));
            p.setTimesClicked(rs.getLong("TimesClicked"));
            p.setUpgrades(RealmProfile.deserializeUpgrades(rs.getString("Upgrades")));
            p.setPrestigeLevel(rs.getInt("PrestigeLevel"));
            p.setAura(CookieMath.parse(rs.getString("Aura")));
            p.setRealmPublic(rs.getInt("RealmPublic") != 0);
            p.setLastCurrentDigitCount(CookieMath.digitCount(p.getCookies()));
            p.setLastTotalDigitCount(CookieMath.digitCount(p.getTotalCookiesEarned()));
            BigDecimal entropy = p.getClickerEntropy();
            p.setLastEntropyDigitCount(CookieMath.digitCount(entropy));
            p.setLastEntropyLeadDigit(CookieMath.leadDigit(entropy));
            return p;
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Failed to read profile from result set", e);
            return null;
        }
    }

    private void loadProfileRolesSync(RealmProfile profile) {
        String s = Statements.getStatement(Statements.StatementType.PULL_PROFILE_ROLES, getConnectorSet());
        executeQuery(s, stmt -> {
            try { stmt.setString(1, profile.getProfileId()); } catch (Throwable ignored) {}
        }, rs -> {
            try {
                while (rs.next()) {
                    String playerUuid = rs.getString("PlayerUuid");
                    String roleName = rs.getString("Role");
                    try {
                        profile.setRole(playerUuid, RealmRole.valueOf(roleName));
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (Throwable ignored) {}
        });
    }

    private void loadProfileBansSync(RealmProfile profile) {
        String s = Statements.getStatement(Statements.StatementType.PULL_PROFILE_BANS, getConnectorSet());
        executeQuery(s, stmt -> {
            try { stmt.setString(1, profile.getProfileId()); } catch (Throwable ignored) {}
        }, rs -> {
            try {
                while (rs.next()) {
                    profile.getBans().add(rs.getString("BannedUuid"));
                }
            } catch (Throwable ignored) {}
        });
    }

    // ===================== FRIENDS =====================

    public CompletableFuture<Set<String>> pullFriendsThreaded(String uuid) {
        return pullUuidSetThreaded(Statements.StatementType.PULL_FRIENDS, uuid, "Uuid2");
    }

    public CompletableFuture<Void> pushFriendThreaded(String uuid1, String uuid2, long since) {
        return AsyncUtils.executeAsync(() -> {
            ensureUsable();
            String s = Statements.getStatement(Statements.StatementType.PUSH_FRIEND, getConnectorSet());
            execute(s, stmt -> { try { stmt.setString(1, uuid1); stmt.setString(2, uuid2); stmt.setLong(3, since); } catch (Throwable ignored) {} });
            execute(s, stmt -> { try { stmt.setString(1, uuid2); stmt.setString(2, uuid1); stmt.setLong(3, since); } catch (Throwable ignored) {} });
        });
    }

    public CompletableFuture<Void> deleteFriendThreaded(String uuid1, String uuid2) {
        return AsyncUtils.executeAsync(() -> {
            ensureUsable();
            String s = Statements.getStatement(Statements.StatementType.DELETE_FRIEND, getConnectorSet());
            execute(s, stmt -> { try { stmt.setString(1, uuid1); stmt.setString(2, uuid2); } catch (Throwable ignored) {} });
            execute(s, stmt -> { try { stmt.setString(1, uuid2); stmt.setString(2, uuid1); } catch (Throwable ignored) {} });
        });
    }

    // ===================== FRIEND REQUESTS =====================

    public CompletableFuture<Set<String>> pullIncomingRequestsThreaded(String uuid) {
        return pullUuidSetThreaded(Statements.StatementType.PULL_INCOMING_REQUESTS, uuid, "Sender");
    }

    public CompletableFuture<Set<String>> pullOutgoingRequestsThreaded(String uuid) {
        return pullUuidSetThreaded(Statements.StatementType.PULL_OUTGOING_REQUESTS, uuid, "Receiver");
    }

    public CompletableFuture<Void> pushFriendRequestThreaded(String sender, String receiver, long sentAt) {
        return AsyncUtils.executeAsync(() -> {
            ensureUsable();
            String s = Statements.getStatement(Statements.StatementType.PUSH_FRIEND_REQUEST, getConnectorSet());
            execute(s, stmt -> { try { stmt.setString(1, sender); stmt.setString(2, receiver); stmt.setLong(3, sentAt); } catch (Throwable ignored) {} });
        });
    }

    public CompletableFuture<Void> deleteFriendRequestThreaded(String sender, String receiver) {
        return AsyncUtils.executeAsync(() -> {
            ensureUsable();
            String s = Statements.getStatement(Statements.StatementType.DELETE_FRIEND_REQUEST, getConnectorSet());
            execute(s, stmt -> { try { stmt.setString(1, sender); stmt.setString(2, receiver); } catch (Throwable ignored) {} });
        });
    }

    // ===================== BANS (legacy + profile) =====================

    public CompletableFuture<Set<String>> pullBansThreaded(String uuid) {
        return pullUuidSetThreaded(Statements.StatementType.PULL_BANS, uuid, "Banned");
    }

    public CompletableFuture<Void> pushBanThreaded(String owner, String banned) {
        return pushPairThreaded(Statements.StatementType.PUSH_BAN, owner, banned);
    }

    public CompletableFuture<Void> deleteBanThreaded(String owner, String banned) {
        return pushPairThreaded(Statements.StatementType.DELETE_BAN, owner, banned);
    }

    // ===================== BLOCKS =====================

    public CompletableFuture<Set<String>> pullBlocksThreaded(String uuid) {
        return pullUuidSetThreaded(Statements.StatementType.PULL_BLOCKS, uuid, "Blocked");
    }

    public CompletableFuture<Void> pushBlockThreaded(String owner, String blocked) {
        return pushPairThreaded(Statements.StatementType.PUSH_BLOCK, owner, blocked);
    }

    public CompletableFuture<Void> deleteBlockThreaded(String owner, String blocked) {
        return pushPairThreaded(Statements.StatementType.DELETE_BLOCK, owner, blocked);
    }

    // ===================== HELPERS =====================

    private CompletableFuture<Set<String>> pullUuidSetThreaded(Statements.StatementType type, String uuid, String columnName) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();
            String s = Statements.getStatement(type, getConnectorSet());
            Set<String> result = new HashSet<>();
            executeQuery(s, stmt -> { try { stmt.setString(1, uuid); } catch (Throwable ignored) {} }, rs -> {
                try { while (rs.next()) result.add(rs.getString(columnName)); } catch (Throwable ignored) {}
            });
            return result;
        });
    }

    private CompletableFuture<Void> pushPairThreaded(Statements.StatementType type, String a, String b) {
        return AsyncUtils.executeAsync(() -> {
            ensureUsable();
            String s = Statements.getStatement(type, getConnectorSet());
            execute(s, stmt -> { try { stmt.setString(1, a); stmt.setString(2, b); } catch (Throwable ignored) {} });
        });
    }
}
