package gg.drak.lobbyclicker.commands;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.config.MainConfig;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.ClickerGui;
import gg.drak.lobbyclicker.redis.RedisManager;
import gg.drak.lobbyclicker.upgrades.UpgradeType;
import gg.drak.lobbyclicker.utils.FormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClickerAdminCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            // No args: open admin GUI if player
            if (sender instanceof Player) {
                new gg.drak.lobbyclicker.gui.admin.AdminMainGui((Player) sender).open();
            } else {
                sendUsage(sender);
            }
            return true;
        }

        String action = args[0].toLowerCase();

        // No-arg actions
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

        if (action.equals("reload")) {
            // Reload configs
            LobbyClicker.setMainConfig(new MainConfig());
            sender.sendMessage(ChatColor.GREEN + "Config reloaded.");

            // Reconnect Redis
            if (LobbyClicker.getRedisManager() != null) {
                LobbyClicker.getRedisManager().shutdown();
                LobbyClicker.setRedisManager(null);
            }
            if (LobbyClicker.getMainConfig().isRedisEnabled()) {
                RedisManager rm = new RedisManager();
                rm.connect();
                LobbyClicker.setRedisManager(rm);
                sender.sendMessage(ChatColor.GREEN + "Redis reconnected.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Redis is disabled in config.");
            }
            return true;
        }

        // "profiles" subcommand - opens profile management GUI
        if (action.equals("profiles")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            if (args.length < 2) {
                // No player specified: open the player list GUI
                new gg.drak.lobbyclicker.gui.admin.AdminPlayerListGui((Player) sender).open();
                return true;
            }
            // Support offline players
            String profUuid = null;
            String profName = args[1];
            Player target2 = Bukkit.getPlayer(args[1]);
            if (target2 != null) {
                profUuid = target2.getUniqueId().toString();
                profName = target2.getName();
            } else {
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
                if (offline.hasPlayedBefore() || offline.isOnline()) {
                    profUuid = offline.getUniqueId().toString();
                    if (offline.getName() != null) profName = offline.getName();
                }
            }
            if (profUuid == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
            new gg.drak.lobbyclicker.gui.admin.AdminProfilesGui(
                    (Player) sender, profUuid, profName).open();
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        // Try online player first
        Player target = Bukkit.getPlayer(args[1]);
        String targetUuid = null;
        String targetName = args[1];

        if (target != null) {
            targetUuid = target.getUniqueId().toString();
            targetName = target.getName();
        } else {
            // Try offline player by name
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
            if (offline.hasPlayedBefore() || offline.isOnline()) {
                targetUuid = offline.getUniqueId().toString();
                if (offline.getName() != null) targetName = offline.getName();
            }
        }

        if (targetUuid == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }

        // Try to get loaded data, or load from DB for offline players
        PlayerData data = PlayerManager.getPlayer(targetUuid).orElse(null);
        boolean loadedForCommand = false;
        if (data == null || !data.isFullyLoaded()) {
            // Load from DB synchronously for admin commands
            java.util.Optional<PlayerData> optData = PlayerManager.getOrGetPlayer(targetUuid);
            if (optData.isPresent()) {
                data = optData.get().waitUntilFullyLoaded();
                loadedForCommand = true;
            } else {
                sender.sendMessage(ChatColor.RED + "Could not load player data for: " + targetName);
                return true;
            }
        }

        final String finalTargetName = targetName;
        final boolean shouldUnload = loadedForCommand && target == null; // unload after if we loaded an offline player

        switch (action) {
            case "openfor": {
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player must be online for openfor.");
                    break;
                }
                new ClickerGui(target, data).open();
                sender.sendMessage(ChatColor.GREEN + "Opened clicker GUI for " + finalTargetName);
                break;
            }
            case "set": {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /clickeradmin set <player> <cookies>");
                    return true;
                }
                try {
                    BigDecimal amount = new BigDecimal(args[2]);
                    BigDecimal diff = amount.subtract(data.getCookies());
                    data.setCookies(amount);
                    if (diff.signum() > 0) data.setTotalCookiesEarned(data.getTotalCookiesEarned().add(diff));
                    data.save(true);
                    sender.sendMessage(ChatColor.GREEN + "Set " + finalTargetName + "'s cookies to " + FormatUtils.format(amount));
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
                    BigDecimal amount = new BigDecimal(args[2]);
                    data.addCookies(amount);
                    data.save(true);
                    sender.sendMessage(ChatColor.GREEN + "Added " + FormatUtils.format(amount) + " cookies to " + finalTargetName
                            + ". Now has " + FormatUtils.format(data.getCookies()));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number: " + args[2]);
                }
                break;
            }
            case "reset": {
                data.setCookies(BigDecimal.ZERO);
                data.setTotalCookiesEarned(BigDecimal.ZERO);
                for (UpgradeType type : UpgradeType.values()) {
                    data.setUpgradeCount(type, 0);
                }
                data.save(true);
                sender.sendMessage(ChatColor.GREEN + "Reset all data for " + finalTargetName);
                break;
            }
            case "info": {
                sender.sendMessage(ChatColor.GOLD + "--- " + finalTargetName + "'s Clicker Stats ---");
                sender.sendMessage(ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(data.getCookies()));
                sender.sendMessage(ChatColor.GRAY + "Total Earned: " + ChatColor.WHITE + FormatUtils.format(data.getTotalCookiesEarned()));
                sender.sendMessage(ChatColor.GRAY + "Realm Clicks: " + ChatColor.WHITE + FormatUtils.format(data.getTimesClicked()));
                sender.sendMessage(ChatColor.GRAY + "Global Clicks: " + ChatColor.WHITE + FormatUtils.format(data.getGlobalClicks()));
                sender.sendMessage(ChatColor.GRAY + "Clicker Entropy: " + ChatColor.WHITE + FormatUtils.format(data.getClickerEntropy()));
                sender.sendMessage(ChatColor.GRAY + "CPS: " + ChatColor.WHITE + FormatUtils.format(data.getCps()));
                sender.sendMessage(ChatColor.GRAY + "CPC: " + ChatColor.WHITE + FormatUtils.format(data.getCpc()));
                sender.sendMessage(ChatColor.GRAY + "Prestige: " + ChatColor.WHITE + data.getPrestigeLevel());
                sender.sendMessage(ChatColor.GRAY + "Aura: " + ChatColor.WHITE + FormatUtils.format(data.getAura()));
                for (UpgradeType type : UpgradeType.values()) {
                    int count = data.getUpgradeCount(type);
                    if (count > 0) {
                        sender.sendMessage(ChatColor.GRAY + "  " + type.getDisplayName() + ": " + ChatColor.WHITE + count);
                    }
                }
                break;
            }
            case "upgrade": {
                if (args.length < 5) {
                    sender.sendMessage(ChatColor.RED + "Usage: /clickeradmin upgrade <player> <add|remove|set> <upgrade> <amount>");
                    return true;
                }
                // args[0]=upgrade, args[1]=player (parsed as target), args[2]=add/remove/set, args[3]=upgrade, args[4]=amount
                String subAction = args[2].toLowerCase();
                String upgradeName = args[3].toUpperCase();
                int amount;
                try {
                    amount = Integer.parseInt(args[4]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[4]);
                    return true;
                }
                UpgradeType upgradeType;
                try {
                    upgradeType = UpgradeType.valueOf(upgradeName);
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(ChatColor.RED + "Unknown upgrade: " + args[3]);
                    sender.sendMessage(ChatColor.GRAY + "Available: " + String.join(", ",
                            Arrays.stream(UpgradeType.values()).map(UpgradeType::name).toArray(String[]::new)));
                    return true;
                }
                int current = data.getUpgradeCount(upgradeType);
                int newCount;
                switch (subAction) {
                    case "add":
                        newCount = current + amount;
                        break;
                    case "remove":
                        newCount = Math.max(0, current - amount);
                        break;
                    case "set":
                        newCount = Math.max(0, amount);
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Usage: /clickeradmin upgrade <add|remove|set> <upgrade> <amount>");
                        return true;
                }
                data.setUpgradeCount(upgradeType, newCount);
                data.save(true);
                sender.sendMessage(ChatColor.GREEN + "Set " + finalTargetName + "'s " + upgradeType.getDisplayName()
                        + " to " + ChatColor.WHITE + newCount + ChatColor.GREEN + " (was " + current + ")");
                break;
            }
            default:
                sendUsage(sender);
                break;
        }

        // Unload offline player data if we loaded it just for this command
        if (shouldUnload) {
            data.saveAndUnload();
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
        sender.sendMessage(ChatColor.YELLOW + "/clickeradmin openfor <player>" + ChatColor.GRAY + " - Open clicker GUI for player");
        sender.sendMessage(ChatColor.YELLOW + "/clickeradmin reload" + ChatColor.GRAY + " - Reload config & reconnect Redis");
        sender.sendMessage(ChatColor.YELLOW + "/clickeradmin upgrade <player> <add|remove|set> <upgrade> <amount>" + ChatColor.GRAY + " - Manage upgrades");
        sender.sendMessage(ChatColor.YELLOW + "/clickeradmin profiles <player>" + ChatColor.GRAY + " - Manage player profiles (GUI)");
        sender.sendMessage(ChatColor.GRAY + "Run " + ChatColor.YELLOW + "/clickeradmin" + ChatColor.GRAY + " with no args to open the admin GUI.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("set", "add", "reset", "info", "save", "openfor", "reload", "upgrade", "profiles"));
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("save") && !args[0].equalsIgnoreCase("reload")) {
            // All commands with a player argument: arg2 = player name
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add")) {
                completions.addAll(Arrays.asList("100", "1000", "10000", "100000"));
            } else if (args[0].equalsIgnoreCase("upgrade")) {
                // /clickeradmin upgrade <player> <add|remove|set>
                completions.addAll(Arrays.asList("add", "remove", "set"));
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("upgrade")) {
            // /clickeradmin upgrade <player> <sub> <upgrade>
            for (UpgradeType type : UpgradeType.values()) {
                completions.add(type.name());
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("upgrade")) {
            // /clickeradmin upgrade <player> <sub> <upgrade> <amount>
            completions.addAll(Arrays.asList("1", "5", "10", "50", "100"));
        }

        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}
