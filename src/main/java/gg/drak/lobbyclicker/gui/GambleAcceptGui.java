package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.social.PendingTransaction;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Gui;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

public class GambleAcceptGui extends Gui {
    private final PlayerData receiverData;
    private final PendingTransaction transaction;
    private static final Random RANDOM = new Random();

    public GambleAcceptGui(Player player, PlayerData receiverData, PendingTransaction transaction) {
        super(player, "gamble-accept", ChatColor.GOLD + "" + ChatColor.BOLD + "Incoming Bet", 3);
        this.receiverData = receiverData;
        this.transaction = transaction;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        String senderName = transaction.getSenderUuid().substring(0, 8);
        try { String n = Bukkit.getOfflinePlayer(UUID.fromString(transaction.getSenderUuid())).getName(); if (n != null) senderName = n; } catch (Exception ignored) {}

        BigDecimal betAmount = transaction.getAmount();
        boolean canAfford = receiverData.canAfford(betAmount);

        addItem(13, GuiHelper.createIcon(Material.EMERALD,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Bet: " + FormatUtils.format(betAmount) + " cookies",
                "",
                ChatColor.GRAY + "From: " + ChatColor.WHITE + senderName,
                ChatColor.GRAY + "Your balance: " + ChatColor.WHITE + FormatUtils.format(receiverData.getCookies()),
                "",
                ChatColor.GRAY + "50/50 chance. Winner takes " + ChatColor.GOLD + FormatUtils.format(betAmount) + ChatColor.GRAY + " from the loser.",
                "",
                canAfford ? ChatColor.GREEN + "You can afford this bet." : ChatColor.RED + "You cannot afford this bet!"));

        if (canAfford) {
            Icon accept = GuiHelper.createIcon(Material.LIME_WOOL, ChatColor.GREEN + "" + ChatColor.BOLD + "Accept Bet");
            accept.onClick(e -> {
                // Validate
                PendingTransaction tx = PendingTransaction.get(transaction.getSenderUuid(), receiverData.getIdentifier());
                if (tx == null) {
                    player.sendMessage(ChatColor.RED + "This bet has expired.");
                    player.closeInventory();
                    return;
                }
                PlayerData senderData = PlayerManager.getPlayer(tx.getSenderUuid()).orElse(null);
                if (senderData == null || !senderData.canAfford(tx.getAmount())) {
                    player.sendMessage(ChatColor.RED + "The other player can no longer afford this bet.");
                    PendingTransaction.remove(tx.getSenderUuid(), receiverData.getIdentifier());
                    player.closeInventory();
                    return;
                }
                if (!receiverData.canAfford(tx.getAmount())) {
                    player.sendMessage(ChatColor.RED + "You can no longer afford this bet.");
                    player.closeInventory();
                    return;
                }

                PendingTransaction.remove(tx.getSenderUuid(), receiverData.getIdentifier());

                // 50/50
                boolean senderWins = RANDOM.nextBoolean();
                PlayerData winner = senderWins ? senderData : receiverData;
                PlayerData loser = senderWins ? receiverData : senderData;

                loser.removeCookies(tx.getAmount());
                winner.addCookies(tx.getAmount());
                winner.save(true);
                loser.save(true);

                String winnerName = winner.getName();
                String loserName = loser.getName();
                String amountStr = FormatUtils.format(tx.getAmount());

                // Notify both
                winner.asPlayer().ifPresent(wp -> {
                    wp.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "You won the bet! " +
                            ChatColor.YELLOW + "+" + amountStr + " cookies from " + loserName);
                    wp.playSound(wp.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                });
                loser.asPlayer().ifPresent(lp -> {
                    lp.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You lost the bet! " +
                            ChatColor.YELLOW + "-" + amountStr + " cookies to " + winnerName);
                    lp.playSound(lp.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                });

                new ClickerGui(player, receiverData).open();
            });
            addItem(11, accept);
        }

        Icon decline = GuiHelper.createIcon(Material.RED_WOOL, ChatColor.RED + "" + ChatColor.BOLD + "Decline");
        decline.onClick(e -> {
            PendingTransaction.remove(transaction.getSenderUuid(), receiverData.getIdentifier());
            Player sender = Bukkit.getPlayer(UUID.fromString(transaction.getSenderUuid()));
            if (sender != null) sender.sendMessage(ChatColor.RED + receiverData.getName() + " declined your bet.");
            player.sendMessage(ChatColor.RED + "Bet declined.");
            new ClickerGui(player, receiverData).open();
        });
        addItem(15, decline);
    }
}
