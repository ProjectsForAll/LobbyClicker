package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.realm.RealmProfile;
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

public class TransferAcceptGui extends BaseGui {
    private final PlayerData receiverData;
    private final PendingTransaction transaction;

    public TransferAcceptGui(Player player, PlayerData receiverData, PendingTransaction transaction) {
        super(player, "transfer-accept", ChatColor.GOLD + "" + ChatColor.BOLD + "Incoming Transfer", 3);
        this.receiverData = receiverData;
        this.transaction = transaction;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        // Home button
        Icon home = GuiHelper.homeButton();
        home.onClick(e -> {
            PendingTransaction.remove(transaction.getSenderUuid(), receiverData.getIdentifier());
            new ClickerGui(player, receiverData).open();
        });
        addItem(0, home);

        String senderName = transaction.getSenderUuid().substring(0, 8);
        try { String n = Bukkit.getOfflinePlayer(UUID.fromString(transaction.getSenderUuid())).getName(); if (n != null) senderName = n; } catch (Exception ignored) {}

        PlayerData senderData = PlayerManager.getPlayer(transaction.getSenderUuid()).orElse(null);
        String cookieStr = senderData != null ? FormatUtils.format(senderData.getCookies()) : "?";
        String totalStr = senderData != null ? FormatUtils.format(senderData.getTotalCookiesEarned()) : "?";
        String prestigeStr = senderData != null ? String.valueOf(senderData.getPrestigeLevel()) : "?";
        String auraStr = senderData != null ? FormatUtils.format(senderData.getAura()) : "?";

        addItem(4, GuiHelper.createIcon(Material.ENDER_CHEST,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Transfer from " + senderName,
                "",
                ChatColor.GRAY + "They want to transfer:",
                ChatColor.WHITE + "  Cookies: " + cookieStr,
                ChatColor.WHITE + "  Total Earned: " + totalStr,
                ChatColor.WHITE + "  Upgrades: " + ChatColor.GRAY + "(max of both)",
                ChatColor.WHITE + "  Prestige: " + prestigeStr,
                ChatColor.WHITE + "  Aura: " + auraStr,
                "",
                ChatColor.GREEN + "Data will be ADDED to your realm"));

        // Accept
        Icon accept = GuiHelper.createIcon(Material.LIME_DYE, ChatColor.GREEN + "" + ChatColor.BOLD + "Accept Transfer");
        accept.onClick(e -> {
            PendingTransaction tx = PendingTransaction.get(transaction.getSenderUuid(), receiverData.getIdentifier());
            if (tx == null) {
                player.sendMessage(ChatColor.RED + "This transfer has expired.");
                player.closeInventory();
                return;
            }
            PlayerData sd = PlayerManager.getPlayer(tx.getSenderUuid()).orElse(null);
            if (sd == null) {
                player.sendMessage(ChatColor.RED + "Sender is no longer online.");
                PendingTransaction.remove(tx.getSenderUuid(), receiverData.getIdentifier());
                player.closeInventory();
                return;
            }

            PendingTransaction.remove(tx.getSenderUuid(), receiverData.getIdentifier());

            // Merge sender's profile into receiver's profile
            RealmProfile senderProfile = sd.getActiveProfile();
            RealmProfile receiverProfile = receiverData.getActiveProfile();
            if (senderProfile != null && receiverProfile != null) {
                receiverProfile.mergeFrom(senderProfile);
                senderProfile.reset();
                sd.save(true);
                receiverData.save(true);
            }

            // Notify both
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Transfer accepted! " +
                    ChatColor.YELLOW + "Realm data has been merged.");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            sd.asPlayer().ifPresent(sp -> {
                sp.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + receiverData.getName() + " accepted your transfer! " +
                        ChatColor.YELLOW + "Your realm has been reset.");
                sp.playSound(sp.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.0f);
            });

            new ClickerGui(player, receiverData).open();
        });
        addItem(11, accept);

        // Decline
        Icon decline = GuiHelper.createIcon(Material.RED_DYE, ChatColor.RED + "" + ChatColor.BOLD + "Decline");
        decline.onClick(e -> {
            PendingTransaction.remove(transaction.getSenderUuid(), receiverData.getIdentifier());
            Player sender = Bukkit.getPlayer(UUID.fromString(transaction.getSenderUuid()));
            if (sender != null) sender.sendMessage(ChatColor.RED + receiverData.getName() + " declined your transfer.");
            player.sendMessage(ChatColor.RED + "Transfer declined.");
            new ClickerGui(player, receiverData).open();
        });
        addItem(15, decline);
    }
}
