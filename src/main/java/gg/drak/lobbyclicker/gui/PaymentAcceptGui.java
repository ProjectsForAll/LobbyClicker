package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.social.PendingTransaction;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentAcceptGui extends BaseGui {
    private final PlayerData receiverData;
    private final PendingTransaction transaction;

    public PaymentAcceptGui(Player player, PlayerData receiverData, PendingTransaction transaction) {
        super(player, "payment-accept", ChatColor.GOLD + "" + ChatColor.BOLD + "Incoming Payment", 3);
        this.receiverData = receiverData;
        this.transaction = transaction;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        Icon home = GuiHelper.homeButton();
        home.onClick(e -> new ClickerGui(player, receiverData).open());
        addItem(0, home);

        String senderName = transaction.getSenderUuid().substring(0, 8);
        try { String n = Bukkit.getOfflinePlayer(UUID.fromString(transaction.getSenderUuid())).getName(); if (n != null) senderName = n; } catch (Exception ignored) {}

        BigDecimal payAmount = transaction.getAmount();

        addItem(13, GuiHelper.createIcon(Material.GOLD_INGOT,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Payment: " + FormatUtils.format(payAmount) + " cookies",
                "",
                ChatColor.GRAY + "From: " + ChatColor.WHITE + senderName,
                "",
                ChatColor.GREEN + "Accept to receive the cookies."));

        Icon accept = GuiHelper.createIcon(Material.LIME_DYE, ChatColor.GREEN + "" + ChatColor.BOLD + "Accept Payment");
        accept.onClick(e -> {
            PendingTransaction tx = PendingTransaction.get(transaction.getSenderUuid(), receiverData.getIdentifier());
            if (tx == null) {
                player.sendMessage(ChatColor.RED + "This payment has expired.");
                player.closeInventory();
                return;
            }
            PlayerData senderData = PlayerManager.getPlayer(tx.getSenderUuid()).orElse(null);
            if (senderData == null || !senderData.canAfford(tx.getAmount())) {
                player.sendMessage(ChatColor.RED + "The sender can no longer afford this payment.");
                PendingTransaction.remove(tx.getSenderUuid(), receiverData.getIdentifier());
                player.closeInventory();
                return;
            }

            PendingTransaction.remove(tx.getSenderUuid(), receiverData.getIdentifier());

            senderData.removeCookies(tx.getAmount());
            receiverData.addCookies(tx.getAmount());
            senderData.save(true);
            receiverData.save(true);

            String amountStr = FormatUtils.format(tx.getAmount());
            player.sendMessage(ChatColor.GREEN + "Received " + ChatColor.GOLD + amountStr + ChatColor.GREEN + " cookies!");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

            senderData.asPlayer().ifPresent(sp -> {
                sp.sendMessage(ChatColor.GREEN + receiverData.getName() + " accepted your payment of " + ChatColor.GOLD + amountStr + ChatColor.GREEN + " cookies!");
                sp.playSound(sp.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            });

            new ClickerGui(player, receiverData).open();
        });
        addItem(11, accept);

        Icon decline = GuiHelper.createIcon(Material.RED_DYE, ChatColor.RED + "" + ChatColor.BOLD + "Decline");
        decline.onClick(e -> {
            PendingTransaction.remove(transaction.getSenderUuid(), receiverData.getIdentifier());
            Player sender = Bukkit.getPlayer(UUID.fromString(transaction.getSenderUuid()));
            if (sender != null) sender.sendMessage(ChatColor.RED + receiverData.getName() + " declined your payment.");
            player.sendMessage(ChatColor.RED + "Payment declined.");
            new ClickerGui(player, receiverData).open();
        });
        addItem(15, decline);
    }
}
