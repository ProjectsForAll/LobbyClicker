package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
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

public class TransferConfirmGui extends BaseGui {
    private final PlayerData senderData;
    private final String targetUuid;

    public TransferConfirmGui(Player player, PlayerData senderData, String targetUuid) {
        super(player, "transfer-confirm", ChatColor.GOLD + "" + ChatColor.BOLD + "Transfer Realm", 3);
        this.senderData = senderData;
        this.targetUuid = targetUuid;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        // Home button
        Icon home = GuiHelper.homeButton();
        home.onClick(e -> new ClickerGui(player, senderData).open());
        addItem(0, home);

        String targetName = targetUuid.substring(0, 8);
        try { String n = Bukkit.getOfflinePlayer(UUID.fromString(targetUuid)).getName(); if (n != null) targetName = n; } catch (Exception ignored) {}

        // Info
        addItem(4, GuiHelper.createIcon(Material.ENDER_CHEST,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Transfer to " + targetName,
                "",
                ChatColor.GRAY + "This will transfer:",
                ChatColor.WHITE + "  Cookies: " + FormatUtils.format(senderData.getCookies()),
                ChatColor.WHITE + "  Total Earned: " + FormatUtils.format(senderData.getTotalCookiesEarned()),
                ChatColor.WHITE + "  Upgrades: " + ChatColor.GRAY + "(max of both)",
                ChatColor.WHITE + "  Prestige: " + senderData.getPrestigeLevel(),
                ChatColor.WHITE + "  Aura: " + FormatUtils.format(senderData.getAura()),
                "",
                ChatColor.RED + "" + ChatColor.BOLD + "Your realm will be RESET after transfer!",
                "",
                ChatColor.GRAY + "Data is ADDED to receiver's realm"));

        // Confirm
        String finalTargetName = targetName;
        Icon confirm = GuiHelper.createIcon(Material.LIME_WOOL,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Confirm Transfer",
                "", ChatColor.YELLOW + "Send transfer request to " + finalTargetName);
        confirm.onClick(e -> {
            Player target = Bukkit.getPlayer(UUID.fromString(targetUuid));
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player is not online.");
                return;
            }
            // Use BigDecimal.ZERO as amount since transfer moves everything
            PendingTransaction tx = new PendingTransaction(senderData.getIdentifier(), targetUuid, BigDecimal.ZERO, TransactionType.TRANSFER);
            PendingTransaction.add(tx);
            player.sendMessage(ChatColor.GREEN + "Transfer request sent to " + finalTargetName + "! They have 60 seconds to accept.");
            target.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + senderData.getName() + ChatColor.YELLOW +
                    " wants to transfer their realm to you! Open " + ChatColor.WHITE + "/clicker" + ChatColor.YELLOW + " to accept.");
            new ClickerGui(player, senderData).open();
        });
        addItem(13, confirm);

        // Cancel
        Icon cancel = GuiHelper.createIcon(Material.RED_WOOL, ChatColor.RED + "Cancel");
        cancel.onClick(e -> new ClickerGui(player, senderData).open());
        addItem(22, cancel);
    }
}
