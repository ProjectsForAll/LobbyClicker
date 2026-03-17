package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.social.PendingTransaction;
import gg.drak.lobbyclicker.social.TransactionType;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Gui;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;
import java.util.UUID;

public class GambleGui extends Gui {
    private final PlayerData senderData;
    private final String targetUuid;
    private final BigDecimal amount;

    public GambleGui(Player player, PlayerData senderData, String targetUuid, BigDecimal amount) {
        super(player, "gamble", ChatColor.GREEN + "" + ChatColor.BOLD + "Gamble", 3);
        this.senderData = senderData;
        this.targetUuid = targetUuid;
        this.amount = amount.max(BigDecimal.ZERO);
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        String targetName = targetUuid.substring(0, 8);
        try { String n = Bukkit.getOfflinePlayer(UUID.fromString(targetUuid)).getName(); if (n != null) targetName = n; } catch (Exception ignored) {}

        boolean canAfford = senderData.canAfford(amount) && amount.signum() > 0;

        addItem(13, GuiHelper.createIcon(Material.EMERALD,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Bet: " + FormatUtils.format(amount) + " cookies",
                "",
                ChatColor.GRAY + "Against: " + ChatColor.WHITE + targetName,
                ChatColor.GRAY + "Your balance: " + ChatColor.WHITE + FormatUtils.format(senderData.getCookies()),
                "", ChatColor.GRAY + "50/50 chance. Winner takes all!",
                "", canAfford ? ChatColor.GREEN + "Ready to send bet!" : ChatColor.RED + "Cannot afford / invalid amount"));

        // Decrease
        addAdjustButton(player, 9, -1, Material.RED_STAINED_GLASS_PANE);
        addAdjustButton(player, 10, -10, Material.RED_STAINED_GLASS_PANE);
        addAdjustButton(player, 11, -100, Material.RED_STAINED_GLASS_PANE);
        addAdjustButton(player, 0, -1000, Material.RED_STAINED_GLASS_PANE);
        addAdjustButton(player, 1, -10000, Material.RED_STAINED_GLASS_PANE);

        // Increase
        addAdjustButton(player, 15, 1, Material.LIME_STAINED_GLASS_PANE);
        addAdjustButton(player, 16, 10, Material.LIME_STAINED_GLASS_PANE);
        addAdjustButton(player, 17, 100, Material.LIME_STAINED_GLASS_PANE);
        addAdjustButton(player, 7, 1000, Material.LIME_STAINED_GLASS_PANE);
        addAdjustButton(player, 8, 10000, Material.LIME_STAINED_GLASS_PANE);

        if (canAfford) {
            String finalTargetName = targetName;
            Icon send = GuiHelper.createIcon(Material.LIME_WOOL, ChatColor.GREEN + "" + ChatColor.BOLD + "Send Bet Request",
                    "", ChatColor.GRAY + "Bet " + ChatColor.GOLD + FormatUtils.format(amount) + ChatColor.GRAY + " against " + finalTargetName);
            send.onClick(e -> {
                if (!senderData.canAfford(amount) || amount.signum() <= 0) {
                    player.sendMessage(ChatColor.RED + "Cannot afford this bet.");
                    return;
                }
                Player target = Bukkit.getPlayer(UUID.fromString(targetUuid));
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Player is not online.");
                    return;
                }
                PendingTransaction tx = new PendingTransaction(senderData.getIdentifier(), targetUuid, amount, TransactionType.GAMBLE);
                PendingTransaction.add(tx);
                player.sendMessage(ChatColor.GREEN + "Bet request sent to " + finalTargetName + "! They have 60 seconds to accept.");
                target.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + senderData.getName() + ChatColor.YELLOW +
                        " wants to bet " + ChatColor.GOLD + FormatUtils.format(amount) + ChatColor.YELLOW +
                        " cookies! Open " + ChatColor.WHITE + "/clicker" + ChatColor.YELLOW + " to accept.");
                new SocialMainGui(player, senderData).open();
            });
            addItem(22, send);
        }

        Icon cancel = GuiHelper.createIcon(Material.RED_WOOL, ChatColor.RED + "Cancel");
        cancel.onClick(e -> new PlayerActionGui(player, senderData, targetUuid, "social").open());
        addItem(18, cancel);
    }

    private void addAdjustButton(Player player, int slot, long change, Material mat) {
        String prefix = change > 0 ? ChatColor.GREEN + "+" : ChatColor.RED + "";
        Icon icon = GuiHelper.createIcon(mat, prefix + FormatUtils.format(Math.abs(change)));
        icon.onClick(e -> new GambleGui(player, senderData, targetUuid, amount.add(BigDecimal.valueOf(change))).open());
        addItem(slot, icon);
    }
}
