package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.ConfirmationMonitor;
import gg.drak.lobbyclicker.social.PendingTransaction;
import gg.drak.lobbyclicker.social.TransactionType;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;
import java.util.UUID;

public class TransferConfirmGui extends ConfirmationMonitor {
    private final PlayerData senderData;
    private final String targetUuid;

    public TransferConfirmGui(Player player, PlayerData senderData, String targetUuid) {
        super(player, "transfer-confirm", ChatColor.GOLD + "" + ChatColor.BOLD + "Transfer Realm");
        this.senderData = senderData;
        this.targetUuid = targetUuid;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(senderData, null);

        String targetName = targetUuid.substring(0, 8);
        try { String n = Bukkit.getOfflinePlayer(UUID.fromString(targetUuid)).getName(); if (n != null) targetName = n; } catch (Exception ignored) {}

        Icon info = GuiHelper.createIcon(Material.ENDER_CHEST,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Transfer to " + targetName,
                "",
                ChatColor.GRAY + "This will transfer:",
                ChatColor.WHITE + "  Cookies: " + FormatUtils.format(senderData.getCookies()),
                ChatColor.WHITE + "  Total Earned: " + FormatUtils.format(senderData.getTotalCookiesEarned()),
                ChatColor.WHITE + "  Upgrades: " + ChatColor.GRAY + "(max of both)",
                ChatColor.WHITE + "  Prestige: " + ChatColor.GRAY + "(max of both)",
                ChatColor.WHITE + "  Aura: " + ChatColor.GRAY + "(max of both)",
                "",
                ChatColor.RED + "" + ChatColor.BOLD + "Your realm will be RESET after transfer!",
                "",
                ChatColor.GRAY + "Data is MERGED into receiver's realm");

        String finalTargetName = targetName;
        buildConfirmation(info, "Send Transfer", "Your realm will be RESET!",
                p -> {
                    Player target = Bukkit.getPlayer(UUID.fromString(targetUuid));
                    if (target == null) {
                        p.sendMessage(ChatColor.RED + "Player is not online.");
                        return;
                    }
                    PendingTransaction tx = new PendingTransaction(senderData.getIdentifier(), targetUuid, BigDecimal.ZERO, TransactionType.TRANSFER);
                    PendingTransaction.add(tx);
                    p.sendMessage(ChatColor.GREEN + "Transfer request sent to " + finalTargetName + "! They have 60 seconds to accept.");
                    target.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + senderData.getName() + ChatColor.YELLOW +
                            " wants to transfer their realm to you! Open " + ChatColor.WHITE + "/clicker" + ChatColor.YELLOW + " to accept.");
                    new ClickerGui(p, senderData).open();
                },
                p -> new PlayerActionGui(p, senderData, targetUuid, "social").open());
    }
}
