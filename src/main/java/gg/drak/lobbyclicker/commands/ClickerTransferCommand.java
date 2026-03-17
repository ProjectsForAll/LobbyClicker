package gg.drak.lobbyclicker.commands;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.TransferConfirmGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClickerTransferCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /clickertransfer <player>");
            return true;
        }

        PlayerData data = PlayerManager.getPlayer(player.getUniqueId().toString()).orElse(null);
        if (data == null || !data.isFullyLoaded()) {
            player.sendMessage(ChatColor.RED + "Your data is not loaded yet.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found or not online: " + args[0]);
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "You cannot transfer to yourself.");
            return true;
        }

        new TransferConfirmGui(player, data, target.getUniqueId().toString()).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
