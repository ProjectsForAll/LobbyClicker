package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.monitor.ConfirmationMonitor;
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

public class TransferAcceptGui extends ConfirmationMonitor {
    private final PlayerData receiverData;
    private final PendingTransaction transaction;

    public TransferAcceptGui(Player player, PlayerData receiverData, PendingTransaction transaction) {
        super(player, "transfer-accept", ChatColor.GOLD + "" + ChatColor.BOLD + "Incoming Transfer");
        this.receiverData = receiverData;
        this.transaction = transaction;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(receiverData, null);

        String senderName = transaction.getSenderUuid().substring(0, 8);
        try { String n = Bukkit.getOfflinePlayer(UUID.fromString(transaction.getSenderUuid())).getName(); if (n != null) senderName = n; } catch (Exception ignored) {}

        PlayerData senderData = PlayerManager.getPlayer(transaction.getSenderUuid()).orElse(null);
        String cookieStr = senderData != null ? FormatUtils.format(senderData.getCookies()) : "?";
        String totalStr = senderData != null ? FormatUtils.format(senderData.getTotalCookiesEarned()) : "?";
        String prestigeStr = senderData != null ? String.valueOf(senderData.getPrestigeLevel()) : "?";
        String auraStr = senderData != null ? FormatUtils.format(senderData.getAura()) : "?";

        Icon info = GuiHelper.createIcon(Material.ENDER_CHEST,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Transfer from " + senderName,
                "",
                ChatColor.GRAY + "They want to transfer:",
                ChatColor.WHITE + "  Cookies: " + cookieStr,
                ChatColor.WHITE + "  Total Earned: " + totalStr,
                ChatColor.WHITE + "  Upgrades: " + ChatColor.GRAY + "(max of both)",
                ChatColor.WHITE + "  Prestige: " + prestigeStr + ChatColor.GRAY + " (max of both)",
                ChatColor.WHITE + "  Aura: " + auraStr + ChatColor.GRAY + " (max of both)",
                "",
                ChatColor.GREEN + "Data will be MERGED into your realm");

        buildConfirmation(info, "Accept Transfer", "This cannot be undone!",
                p -> {
                    PendingTransaction tx = PendingTransaction.get(transaction.getSenderUuid(), receiverData.getIdentifier());
                    if (tx == null) {
                        p.sendMessage(ChatColor.RED + "This transfer has expired.");
                        new ClickerGui(p, receiverData).open();
                        return;
                    }
                    PlayerData sd = PlayerManager.getPlayer(tx.getSenderUuid()).orElse(null);
                    if (sd == null) {
                        p.sendMessage(ChatColor.RED + "Sender is no longer online.");
                        PendingTransaction.remove(tx.getSenderUuid(), receiverData.getIdentifier());
                        new ClickerGui(p, receiverData).open();
                        return;
                    }

                    PendingTransaction.remove(tx.getSenderUuid(), receiverData.getIdentifier());

                    RealmProfile senderProfile = sd.getActiveProfile();
                    RealmProfile receiverProfile = receiverData.getActiveProfile();
                    if (senderProfile != null && receiverProfile != null) {
                        receiverProfile.mergeFrom(senderProfile);
                        senderProfile.reset();
                        sd.save(true);
                        receiverData.save(true);
                    }

                    p.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Transfer accepted! " +
                            ChatColor.YELLOW + "Realm data has been merged.");
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

                    sd.asPlayer().ifPresent(sp -> {
                        sp.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + receiverData.getName() + " accepted your transfer! " +
                                ChatColor.YELLOW + "Your realm has been reset.");
                        sp.playSound(sp.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.0f);
                    });

                    new ClickerGui(p, receiverData).open();
                },
                p -> {
                    PendingTransaction.remove(transaction.getSenderUuid(), receiverData.getIdentifier());
                    Player sender = Bukkit.getPlayer(UUID.fromString(transaction.getSenderUuid()));
                    if (sender != null) sender.sendMessage(ChatColor.RED + receiverData.getName() + " declined your transfer.");
                    p.sendMessage(ChatColor.RED + "Transfer declined.");
                    new ClickerGui(p, receiverData).open();
                });
    }
}
