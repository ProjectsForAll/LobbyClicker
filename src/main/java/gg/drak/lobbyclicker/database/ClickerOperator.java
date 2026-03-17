package gg.drak.lobbyclicker.database;

import gg.drak.thebase.async.AsyncUtils;
import host.plas.bou.sql.DBOperator;
import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;

import host.plas.bou.sql.ConnectorSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        String s1 = Statements.getStatement(Statements.StatementType.CREATE_TABLES, getConnectorSet());
        execute(s1, stmt -> {});
    }

    @Override
    public void ensureDatabase() {
        String s1 = Statements.getStatement(Statements.StatementType.CREATE_DATABASE, getConnectorSet());
        execute(s1, stmt -> {});
    }

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
                    stmt.setString(5, playerData.serializeUpgrades());
                } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to set values for statement: " + s1, e);
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
                try {
                    stmt.setString(1, uuid);
                } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to set values for statement: " + s1, e);
                }
            }, rs -> {
                try {
                    if (rs.next()) {
                        String name = rs.getString("Name");
                        double cookies = rs.getDouble("Cookies");
                        double totalEarned = rs.getDouble("TotalCookiesEarned");
                        String upgrades = rs.getString("Upgrades");

                        PlayerData playerData = new PlayerData(uuid, name, cookies, totalEarned, upgrades);
                        ref.set(Optional.of(playerData));
                    }
                } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to get values from result set for statement: " + s1, e);
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
                        String uuid = rs.getString("Uuid");
                        String name = rs.getString("Name");
                        double cookies = rs.getDouble("Cookies");
                        double totalEarned = rs.getDouble("TotalCookiesEarned");
                        String upgrades = rs.getString("Upgrades");

                        players.add(new PlayerData(uuid, name, cookies, totalEarned, upgrades));
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
                        String uuid = rs.getString("Uuid");
                        String name = rs.getString("Name");
                        double totalEarned = rs.getDouble("TotalCookiesEarned");

                        PlayerData data = new PlayerData(uuid, name, 0, totalEarned, "");
                        entries.add(data);
                    }
                } catch (Throwable e) {
                    LobbyClicker.getInstance().logWarning("Failed to get leaderboard data: " + s1, e);
                }
            });

            return entries;
        });
    }
}
