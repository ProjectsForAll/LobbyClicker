package gg.drak.lobbyclicker.commands;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.ClickerGui;
import gg.drak.lobbyclicker.gui.GambleAcceptGui;
import gg.drak.lobbyclicker.social.PendingTransaction;
import gg.drak.lobbyclicker.social.TransactionType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClickerCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        PlayerData data = PlayerManager.getPlayer(player.getUniqueId().toString()).orElse(null);

        if (data == null || !data.isFullyLoaded()) {
            player.sendMessage(ChatColor.RED + "Your data is still loading. Please try again in a moment.");
            return true;
        }

        // Check for pending gamble bet
        PendingTransaction pendingBet = PendingTransaction.getForReceiver(data.getIdentifier());
        if (pendingBet != null && pendingBet.getType() == TransactionType.GAMBLE) {
            new GambleAcceptGui(player, data, pendingBet).open();
            return true;
        }

        new ClickerGui(player, data).open();
        return true;
    }
}
