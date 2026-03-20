package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.monitor.ConfirmationMonitor;
import gg.drak.lobbyclicker.social.PendingTransaction;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.UUID;
import java.util.function.Consumer;

public class PaymentAcceptGui extends ConfirmationMonitor {
    private final PlayerData receiverData;
    private final PendingTransaction transaction;
    private final Consumer<Player> backAction;

    public PaymentAcceptGui(Player player, PlayerData receiverData, PendingTransaction transaction, Consumer<Player> backAction) {
        super(player, "payment-accept", ChatColor.GOLD + "" + ChatColor.BOLD + "Incoming Payment");
        this.receiverData = receiverData;
        this.transaction = transaction;
        this.backAction = backAction;
    }

    public PaymentAcceptGui(Player player, PlayerData receiverData, PendingTransaction transaction) {
        this(player, receiverData, transaction, p -> new MailGui(p, receiverData).open());
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(receiverData, null);

        String senderName = transaction.getSenderUuid().substring(0, 8);
        try { String n = Bukkit.getOfflinePlayer(UUID.fromString(transaction.getSenderUuid())).getName(); if (n != null) senderName = n; } catch (Exception ignored) {}

        Icon info = GuiHelper.createIcon(Material.GOLD_INGOT,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Payment: " + FormatUtils.format(transaction.getAmount()) + " cookies",
                "",
                ChatColor.GRAY + "From: " + ChatColor.WHITE + senderName,
                "",
                ChatColor.GREEN + "Accept to receive the cookies.");

        buildConfirmation(info, "Accept Payment", "Accept this payment?",
                p -> acceptPayment(p),
                backAction);
    }

    /** Execute the payment acceptance logic. Also callable directly from shift-click. */
    public static void acceptPayment(Player player, PlayerData receiverData, PendingTransaction transaction, Consumer<Player> backAction) {
        PendingTransaction tx = PendingTransaction.get(transaction.getSenderUuid(), receiverData.getIdentifier());
        if (tx == null) {
            player.sendMessage(ChatColor.RED + "This payment has expired.");
            backAction.accept(player);
            return;
        }
        PlayerData senderData = PlayerManager.getPlayer(tx.getSenderUuid()).orElse(null);
        if (senderData == null || !senderData.canAfford(tx.getAmount())) {
            player.sendMessage(ChatColor.RED + "The sender can no longer afford this payment.");
            PendingTransaction.remove(tx.getSenderUuid(), receiverData.getIdentifier());
            backAction.accept(player);
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
        backAction.accept(player);
    }

    private void acceptPayment(Player p) {
        acceptPayment(p, receiverData, transaction, backAction);
    }

    public static void declinePayment(Player player, PlayerData receiverData, PendingTransaction transaction, Consumer<Player> backAction) {
        PendingTransaction.remove(transaction.getSenderUuid(), receiverData.getIdentifier());
        Player sender = Bukkit.getPlayer(UUID.fromString(transaction.getSenderUuid()));
        if (sender != null) sender.sendMessage(ChatColor.RED + receiverData.getName() + " declined your payment.");
        player.sendMessage(ChatColor.RED + "Payment declined.");
        backAction.accept(player);
    }
}
