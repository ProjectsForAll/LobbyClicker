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

        // Pull all players from the current database
        LobbyClicker.getDatabase().pullAllPlayersThreaded().thenAccept(players -> {
            try {
                // Create a connector set for the target database
                ConnectorSet targetSet = new ConnectorSet(
                        targetType,
                        currentSet.getHost(),
                        currentSet.getPort(),
                        currentSet.getDatabase(),
                        currentSet.getUsername(),
                        currentSet.getPassword(),
                        currentSet.getTablePrefix(),
                        currentSet.getSqliteFileName()
                );

                // Create a new operator for the target and push all data
                ClickerOperator targetOp = new ClickerOperator(targetSet);

                for (PlayerData p : players) {
                    targetOp.putPlayer(p, false);
                }

                int count = players.size();

                // Switch the active database operator immediately
                Bukkit.getScheduler().runTask(LobbyClicker.getInstance(), () -> {
                    LobbyClicker.setDatabase(targetOp);

                    // Re-save all currently loaded (in-memory) players to the new DB
                    for (PlayerData loaded : PlayerManager.getLoadedPlayers()) {
                        if (loaded.isFullyLoaded()) {
                            loaded.save(true);
                        }
                    }

                    sender.sendMessage(ChatColor.GREEN + "Migration complete! " + count + " player(s) migrated.");
                    sender.sendMessage(ChatColor.GREEN + "Database switched to " + ChatColor.WHITE + targetType.name() + ChatColor.GREEN + " immediately.");
                    sender.sendMessage(ChatColor.YELLOW + "Update " + ChatColor.WHITE + "database.type" +
                            ChatColor.YELLOW + " in " + ChatColor.WHITE + "database-config.yml" +
                            ChatColor.YELLOW + " to " + ChatColor.WHITE + targetType.name() +
                            ChatColor.YELLOW + " so this persists across restarts.");
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

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- LobbyClicker Data ---");
        sender.sendMessage(ChatColor.YELLOW + "/clickerdata migrate to-db" + ChatColor.GRAY + " - Migrate data to MySQL");
        sender.sendMessage(ChatColor.YELLOW + "/clickerdata migrate to-sqlite" + ChatColor.GRAY + " - Migrate data to SQLite");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("migrate");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            completions.addAll(Arrays.asList("to-db", "to-sqlite"));
        }

        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}
