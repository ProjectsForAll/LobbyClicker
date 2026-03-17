package gg.drak.lobbyclicker.commands;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.database.ClickerOperator;
import host.plas.bou.sql.ConnectorSet;
import host.plas.bou.sql.DatabaseType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClickerDataCommand implements CommandExecutor, TabCompleter {
    private boolean migrating = false;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("migrate")) {
            sendUsage(sender);
            return true;
        }

        String direction = args[1].toLowerCase();

        if (direction.equals("to-other")) {
            return handleMigrateToOther(sender, args);
        }

        DatabaseType targetType;
        switch (direction) {
            case "to-db":
                targetType = DatabaseType.MYSQL;
                break;
            case "to-sqlite":
                targetType = DatabaseType.SQLITE;
                break;
            default:
                sendUsage(sender);
                return true;
        }

        ConnectorSet currentSet = LobbyClicker.getDatabaseConfig().getConnectorSet();

        if (currentSet.getType() == targetType) {
            sender.sendMessage(ChatColor.RED + "You are already using " + targetType.name() + ".");
            return true;
        }

        if (migrating) {
            sender.sendMessage(ChatColor.RED + "A migration is already in progress.");
            return true;
        }

        migrating = true;
        sender.sendMessage(ChatColor.YELLOW + "Starting migration from " + currentSet.getType().name() + " to " + targetType.name() + "...");

        LobbyClicker.getDatabase().pullAllPlayersThreaded().thenAccept(players -> {
            try {
                ConnectorSet targetSet = new ConnectorSet(
                        targetType,
                        currentSet.getHost(), currentSet.getPort(), currentSet.getDatabase(),
                        currentSet.getUsername(), currentSet.getPassword(),
                        currentSet.getTablePrefix(), currentSet.getSqliteFileName()
                );

                ClickerOperator targetOp = new ClickerOperator(targetSet);
                for (PlayerData p : players) {
                    targetOp.putPlayer(p, false);
                }

                int count = players.size();
                Bukkit.getScheduler().runTask(LobbyClicker.getInstance(), () -> {
                    LobbyClicker.setDatabase(targetOp);
                    for (PlayerData loaded : PlayerManager.getLoadedPlayers()) {
                        if (loaded.isFullyLoaded()) loaded.save(true);
                    }
                    sender.sendMessage(ChatColor.GREEN + "Migration complete! " + count + " player(s) migrated.");
                    sender.sendMessage(ChatColor.GREEN + "Database switched to " + ChatColor.WHITE + targetType.name() + ChatColor.GREEN + " immediately.");
                    sender.sendMessage(ChatColor.YELLOW + "Update database-config.yml to persist across restarts.");
                    migrating = false;
                });
            } catch (Throwable e) {
                LobbyClicker.getInstance().logWarning("Migration failed", e);
                Bukkit.getScheduler().runTask(LobbyClicker.getInstance(), () -> {
                    sender.sendMessage(ChatColor.RED + "Migration failed: " + e.getMessage());
                    migrating = false;
                });
            }
        });

        return true;
    }

    private boolean handleMigrateToOther(CommandSender sender, String[] args) {
        // /clickerdata migrate to-other <host:port> <user> <password> <database> [prefix]
        if (args.length < 6) {
            sender.sendMessage(ChatColor.RED + "Usage: /clickerdata migrate to-other <host:port> <user> <password> <database> [prefix]");
            return true;
        }

        if (migrating) {
            sender.sendMessage(ChatColor.RED + "A migration is already in progress.");
            return true;
        }

        String hostPort = args[2];
        String user = args[3];
        String password = args[4];
        String database = args[5];
        String prefix = args.length >= 7 ? args[6] : LobbyClicker.getDatabaseConfig().getDatabaseTablePrefix();

        String host;
        int port;
        if (hostPort.contains(":")) {
            String[] parts = hostPort.split(":", 2);
            host = parts[0];
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid port: " + parts[1]);
                return true;
            }
        } else {
            host = hostPort;
            port = 3306;
        }

        migrating = true;
        sender.sendMessage(ChatColor.YELLOW + "Starting migration to " + host + ":" + port + "/" + database + "...");

        LobbyClicker.getDatabase().pullAllPlayersThreaded().thenAccept(players -> {
            try {
                ConnectorSet targetSet = new ConnectorSet(
                        DatabaseType.MYSQL, host, port, database, user, password, prefix, ""
                );

                ClickerOperator targetOp = new ClickerOperator(targetSet);
                for (PlayerData p : players) {
                    targetOp.putPlayer(p, false);
                }

                int count = players.size();
                Bukkit.getScheduler().runTask(LobbyClicker.getInstance(), () -> {
                    LobbyClicker.setDatabase(targetOp);
                    for (PlayerData loaded : PlayerManager.getLoadedPlayers()) {
                        if (loaded.isFullyLoaded()) loaded.save(true);
                    }
                    sender.sendMessage(ChatColor.GREEN + "Migration complete! " + count + " player(s) migrated to remote MySQL.");
                    sender.sendMessage(ChatColor.GREEN + "Database switched immediately.");
                    sender.sendMessage(ChatColor.YELLOW + "Update database-config.yml with the new credentials to persist:");
                    sender.sendMessage(ChatColor.WHITE + "  host: " + host);
                    sender.sendMessage(ChatColor.WHITE + "  port: " + port);
                    sender.sendMessage(ChatColor.WHITE + "  username: " + user);
                    sender.sendMessage(ChatColor.WHITE + "  database: " + database);
                    sender.sendMessage(ChatColor.WHITE + "  table-prefix: " + prefix);
                    sender.sendMessage(ChatColor.WHITE + "  type: MYSQL");
                    migrating = false;
                });
            } catch (Throwable e) {
                LobbyClicker.getInstance().logWarning("Migration to remote DB failed", e);
                Bukkit.getScheduler().runTask(LobbyClicker.getInstance(), () -> {
                    sender.sendMessage(ChatColor.RED + "Migration failed: " + e.getMessage());
                    migrating = false;
                });
            }
        });

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- LobbyClicker Data ---");
        sender.sendMessage(ChatColor.YELLOW + "/clickerdata migrate to-db" + ChatColor.GRAY + " - Migrate to MySQL (same config)");
        sender.sendMessage(ChatColor.YELLOW + "/clickerdata migrate to-sqlite" + ChatColor.GRAY + " - Migrate to SQLite");
        sender.sendMessage(ChatColor.YELLOW + "/clickerdata migrate to-other <host:port> <user> <pass> <db> [prefix]" + ChatColor.GRAY + " - Migrate to remote MySQL");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("migrate");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            completions.addAll(Arrays.asList("to-db", "to-sqlite", "to-other"));
        } else if (args.length == 3 && args[1].equalsIgnoreCase("to-other")) {
            completions.add("localhost:3306");
        } else if (args.length == 4 && args[1].equalsIgnoreCase("to-other")) {
            completions.add("root");
        } else if (args.length == 6 && args[1].equalsIgnoreCase("to-other")) {
            completions.add("lobbyclicker");
        } else if (args.length == 7 && args[1].equalsIgnoreCase("to-other")) {
            completions.add(LobbyClicker.getDatabaseConfig().getDatabaseTablePrefix());
        }

        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}
