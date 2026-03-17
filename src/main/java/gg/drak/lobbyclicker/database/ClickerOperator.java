package gg.drak.lobbyclicker.database;

import gg.drak.thebase.async.AsyncUtils;
import host.plas.bou.sql.DBOperator;
import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;

import host.plas.bou.sql.ConnectorSet;
import host.plas.bou.sql.DatabaseType;

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
        // Create all tables
        execute(Statements.getStatement(Statements.StatementType.CREATE_TABLES, getConnectorSet()), stmt -> {});
        execute(Statements.getStatement(Statements.StatementType.CREATE_FRIENDS_TABLE, getConnectorSet()), stmt -> {});
        execute(Statements.getStatement(Statements.StatementType.CREATE_FRIEND_REQUESTS_TABLE, getConnectorSet()), stmt -> {});
        execute(Statements.getStatement(Statements.StatementType.CREATE_BANS_TABLE, getConnectorSet()), stmt -> {});
        execute(Statements.getStatement(Statements.StatementType.CREATE_BLOCKS_TABLE, getConnectorSet()), stmt -> {});

        // Migrate Players table: add any missing columns
        String prefix = getConnectorSet().getTablePrefix();
        boolean mysql = getConnectorSet().getType() == DatabaseType.MYSQL;

        Set<String> existing = new HashSet<>();
        executeQuery("SELECT * FROM `" + prefix + "Players` LIMIT 0;", stmt -> {}, rs -> {
            try {
                java.sql.ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    existing.add(meta.getColumnName(i));
                }
            } catch (Throwable e) {
                LobbyClicker.getInstance().logWarning("Failed to read table metadata", e);
            }
        });

        String[][] migrations = {
                {"Cookies",            mysql ? "DOUBLE NOT NULL DEFAULT 0" : "REAL NOT NULL DEFAULT 0"},
                {"TotalCookiesEarned", mysql ? "DOUBLE NOT NULL DEFAULT 0" : "REAL NOT NULL DEFAULT 0"},
                {"TimesClicked",       mysql ? "BIGINT NOT NULL DEFAULT 0" : "INTEGER NOT NULL DEFAULT 0"},
                {"Upgrades",           "TEXT NOT NULL DEFAULT ''"},
                {"Settings",           "TEXT NOT NULL DEFAULT ''"},
                {"RealmPublic",        mysql ? "TINYINT NOT NULL DEFAULT 0" : "INTEGER NOT NULL DEFAULT 0"},
        };

        for (String[] col : migrations) {
            if (!existing.contains(col[0])) {
                execute("ALTER TABLE `" + prefix + "Players` ADD COLUMN `" + col[0] + "` " + col[1] + ";", stmt -> {});
                LobbyClicker.getInstance().logInfo("Added missing column: " + col[0]);
            }
        }
    }

    @Override
    public void ensureDatabase() {
        String s1 = Statements.getStatement(Statements.StatementType.CREATE_DATABASE, getConnectorSet());
        execute(s1, stmt -> {});
    }

    // --- Player CRUD ---

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
                    stmt.setDouble(3, playerData.getCookies());
                    stmt.setDouble(4, playerData.getTotalCookiesEarned());
                    stmt.setLong(5, playerData.getTimesClicked());
                    stmt.setString(6, playerData.serializeUpgrades());
                    stmt.setString(7, playerData.getSettings().serialize());
                    stmt.setInt(8, playerData.isRealmPublic() ? 1 : 0);
                } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to set values for push statement", e);
                }
            });
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
                        double cookies = rs.getDouble("Cookies");
                        double totalEarned = rs.getDouble("TotalCookiesEarned");
                        long timesClicked = rs.getLong("TimesClicked");
                        String upgrades = rs.getString("Upgrades");
                        String settings = rs.getString("Settings");
                        boolean realmPublic = rs.getInt("RealmPublic") != 0;
                        ref.set(Optional.of(new PlayerData(uuid, name, cookies, totalEarned, timesClicked, upgrades, settings, realmPublic)));
                    }
                } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to read player result set", e);
                }
            });
            return ref.get();
        });
    }

    public CompletableFuture<List<PlayerData>> pullAllPlayersThreaded() {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();
            String s1 = Statements.getStatement(Statements.StatementType.PULL_ALL_PLAYERS, getConnectorSet());
            List<PlayerData> players = new ArrayList<>();
            executeQuery(s1, stmt -> {}, rs -> {
                try {
                    while (rs.next()) {
                        players.add(new PlayerData(
                                rs.getString("Uuid"), rs.getString("Name"),
                                rs.getDouble("Cookies"), rs.getDouble("TotalCookiesEarned"),
                                rs.getLong("TimesClicked"), rs.getString("Upgrades"),
                                rs.getString("Settings"), rs.getInt("RealmPublic") != 0
                        ));
                    }
                } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to pull all players", e);
                }
            });
            return players;
        });
    }

    public CompletableFuture<List<PlayerData>> pullLeaderboardThreaded() {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();
            String s1 = Statements.getStatement(Statements.StatementType.PULL_LEADERBOARD, getConnectorSet());
            List<PlayerData> entries = new ArrayList<>();
            executeQuery(s1, stmt -> {}, rs -> {
                try {
                    while (rs.next()) {
                        entries.add(new PlayerData(rs.getString("Uuid"), rs.getString("Name"),
                                0, rs.getDouble("TotalCookiesEarned"), 0, "", "", false));
                    }
                } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to pull leaderboard", e);
                }
            });
            return entries;
        });
    }

    // --- Friends ---

    public CompletableFuture<Set<String>> pullFriendsThreaded(String uuid) {
        return pullUuidSetThreaded(Statements.StatementType.PULL_FRIENDS, uuid, "Uuid2");
    }

    public CompletableFuture<Void> pushFriendThreaded(String uuid1, String uuid2, long since) {
        return AsyncUtils.executeAsync(() -> {
            ensureUsable();
            String s = Statements.getStatement(Statements.StatementType.PUSH_FRIEND, getConnectorSet());
            // Bidirectional
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

    // --- Friend Requests ---

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

    // --- Bans ---

    public CompletableFuture<Set<String>> pullBansThreaded(String uuid) {
        return pullUuidSetThreaded(Statements.StatementType.PULL_BANS, uuid, "Banned");
    }

    public CompletableFuture<Void> pushBanThreaded(String owner, String banned) {
        return pushPairThreaded(Statements.StatementType.PUSH_BAN, owner, banned);
    }

    public CompletableFuture<Void> deleteBanThreaded(String owner, String banned) {
        return pushPairThreaded(Statements.StatementType.DELETE_BAN, owner, banned);
    }

    // --- Blocks ---

    public CompletableFuture<Set<String>> pullBlocksThreaded(String uuid) {
        return pullUuidSetThreaded(Statements.StatementType.PULL_BLOCKS, uuid, "Blocked");
    }

    public CompletableFuture<Void> pushBlockThreaded(String owner, String blocked) {
        return pushPairThreaded(Statements.StatementType.PUSH_BLOCK, owner, blocked);
    }

    public CompletableFuture<Void> deleteBlockThreaded(String owner, String blocked) {
        return pushPairThreaded(Statements.StatementType.DELETE_BLOCK, owner, blocked);
    }

    // --- Helpers ---

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
