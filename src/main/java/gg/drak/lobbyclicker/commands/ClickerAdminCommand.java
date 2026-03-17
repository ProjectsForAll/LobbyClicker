package gg.drak.lobbyclicker.commands;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.upgrades.UpgradeType;
import gg.drak.lobbyclicker.utils.FormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClickerAdminCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String action = args[0].toLowerCase();

        // Handle save (no player argument needed)
        if (action.equals("save")) {
            int count = 0;
            for (PlayerData data : PlayerManager.getLoadedPlayers()) {
                if (data.isFullyLoaded()) {
                    data.save(true);
                    count++;
                }
            }
            sender.sendMessage(ChatColor.GREEN + "Force-saved " + count + " player(s).");
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or not online: " + args[1]);
            return true;
        }

        PlayerData data = PlayerManager.getPlayer(target.getUniqueId().toString()).orElse(null);
        if (data == null || !data.isFullyLoaded()) {
            sender.sendMessage(ChatColor.RED + "Player data is not loaded yet.");
            return true;
        }

        switch (action) {
            case "set": {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /clickeradmin set <player> <cookies>");
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    double diff = amount - data.getCookies();
                    data.setCookies(amount);
                    if (diff > 0) data.setTotalCookiesEarned(data.getTotalCookiesEarned() + diff);
                    data.save(true);
                    sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s cookies to " + FormatUtils.format(amount));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number: " + args[2]);
                }
                break;
            }
            case "add": {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /clickeradmin add <player> <cookies>");
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    data.addCookies(amount);
                    data.save(true);
                    sender.sendMessage(ChatColor.GREEN + "Added " + FormatUtils.format(amount) + " cookies to " + target.getName()
                            + ". Now has " + FormatUtils.format(data.getCookies()));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number: " + args[2]);
                }
                break;
            }
            case "reset": {
                data.setCookies(0);
                data.setTotalCookiesEarned(0);
                for (UpgradeType type : UpgradeType.values()) {
                    data.setUpgradeCount(type, 0);
                }
                data.save(true);
                sender.sendMessage(ChatColor.GREEN + "Reset all data for " + target.getName());
                break;
            }
            case "info": {
                sender.sendMessage(ChatColor.GOLD + "--- " + target.getName() + "'s Clicker Stats ---");
                sender.sendMessage(ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(data.getCookies()));
                sender.sendMessage(ChatColor.GRAY + "Total Earned: " + ChatColor.WHITE + FormatUtils.format(data.getTotalCookiesEarned()));
                sender.sendMessage(ChatColor.GRAY + "Times Clicked: " + ChatColor.WHITE + FormatUtils.format(data.getTimesClicked()));
                sender.sendMessage(ChatColor.GRAY + "Clicker Entropy: " + ChatColor.WHITE + FormatUtils.format(data.getClickerEntropy()));
                sender.sendMessage(ChatColor.GRAY + "CPS: " + ChatColor.WHITE + FormatUtils.format(data.getCps()));
                sender.sendMessage(ChatColor.GRAY + "CPC: " + ChatColor.WHITE + FormatUtils.format(data.getCpc()));
                for (UpgradeType type : UpgradeType.values()) {
                    int count = data.getUpgradeCount(type);
                    if (count > 0) {
                        sender.sendMessage(ChatColor.GRAY + "  " + type.getDisplayName() + ": " + ChatColor.WHITE + count);
                    }
                }
                break;
            }
            default:
                sendUsage(sender);
                break;
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- LobbyClicker Admin ---");
        sender.sendMessage(ChatColor.YELLOW + "/clickeradmin set <player> <cookies>" + ChatColor.GRAY + " - Set cookie count");
        sender.sendMessage(ChatColor.YELLOW + "/clickeradmin add <player> <cookies>" + ChatColor.GRAY + " - Add cookies");
        sender.sendMessage(ChatColor.YELLOW + "/clickeradmin reset <player>" + ChatColor.GRAY + " - Reset all data");
        sender.sendMessage(ChatColor.YELLOW + "/clickeradmin info <player>" + ChatColor.GRAY + " - View player stats");
        sender.sendMessage(ChatColor.YELLOW + "/clickeradmin save" + ChatColor.GRAY + " - Force save all players");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("set", "add", "reset", "info", "save"));
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("save")) {
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add"))) {
            completions.addAll(Arrays.asList("100", "1000", "10000", "100000"));
        }

        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}
