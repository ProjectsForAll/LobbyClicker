package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.SimpleGuiMonitor;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;

public class MoneyActionsGui extends SimpleGuiMonitor {
    private final PlayerData viewerData;
    private final String targetUuid;
    private final String returnTo;

    public MoneyActionsGui(Player player, PlayerData viewerData, String targetUuid, String returnTo) {
        super(player, "money-actions", MonitorStyle.title("gold", "Money Actions"), MonitorStyle.ROWS_SMALL);
        this.viewerData = viewerData;
        this.targetUuid = targetUuid;
        this.returnTo = returnTo;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        Player player = (Player) event.getPlayer();
        setPlayerContext(viewerData, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new PlayerActionGui(p, viewerData, targetUuid, returnTo).open());

        String targetName = targetUuid.substring(0, 8);
        try { String n = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(targetUuid)).getName(); if (n != null) targetName = n; } catch (Exception ignored) {}

        // Pay button
        Icon pay = GuiHelper.createIcon(Material.GOLD_INGOT,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Pay Cookies",
                "", ChatColor.GRAY + "Send cookies to " + ChatColor.WHITE + targetName);
        pay.onClick(e -> new PaymentGui(player, viewerData, targetUuid, BigDecimal.ZERO).open());
        setContent(2, pay);

        // Gamble button
        Icon gamble = GuiHelper.createIcon(Material.EMERALD,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Gamble",
                "", ChatColor.GRAY + "Bet cookies against " + ChatColor.WHITE + targetName,
                ChatColor.GRAY + "50/50 chance. Winner takes all!");
        gamble.onClick(e -> new GambleGui(player, viewerData, targetUuid, BigDecimal.ZERO).open());
        setContent(4, gamble);
    }
}
