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
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentGui extends BaseGui {
    private final PlayerData senderData;
    private final String targetUuid;
    private final BigDecimal amount;

    public PaymentGui(Player player, PlayerData senderData, String targetUuid, BigDecimal amount) {
        super(player, "payment", ChatColor.GOLD + "" + ChatColor.BOLD + "Pay Cookies", 3);
        this.senderData = senderData;
        this.targetUuid = targetUuid;
        this.amount = amount.max(BigDecimal.ZERO);
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

        // "+" banner at slot 2 (index 2)
        String amountLore = ChatColor.GRAY + "Amount: " + ChatColor.GOLD + FormatUtils.format(amount);
        addItem(2, BannerChar.of("+", BannerChar.BannerColor.RED, BannerChar.BannerColor.WHITE).toIcon(amountLore));

        // Banner display at slots 3-6 (index 3-6)
        String[] display = BannerUtil.parseBannerDisplay(amount);
        for (int i = 0; i < 4; i++) {
            addItem(3 + i, BannerChar.of(display[i], BannerChar.BannerColor.RED, BannerChar.BannerColor.WHITE).toIcon(amountLore));
        }

        // Amount info display
        boolean canAfford = senderData.canAfford(amount) && amount.signum() > 0;
        addItem(13, GuiHelper.createIcon(Material.SUNFLOWER,
                ChatColor.GOLD + "" + ChatColor.BOLD + FormatUtils.format(amount) + " cookies",
                "",
                ChatColor.GRAY + "To: " + ChatColor.WHITE + targetName,
                ChatColor.GRAY + "Your balance: " + ChatColor.WHITE + FormatUtils.format(senderData.getCookies()),
                "", canAfford ? ChatColor.GREEN + "Ready to send!" : ChatColor.RED + "Cannot afford / invalid amount"));

        // Decrease buttons (left side)
        addAdjustButton(player, 9, -1, Material.RED_STAINED_GLASS_PANE);
        addAdjustButton(player, 10, -10, Material.RED_STAINED_GLASS_PANE);
        addAdjustButton(player, 11, -100, Material.RED_STAINED_GLASS_PANE);

        // Increase buttons (right side)
        addAdjustButton(player, 15, 1, Material.LIME_STAINED_GLASS_PANE);
        addAdjustButton(player, 16, 10, Material.LIME_STAINED_GLASS_PANE);
        addAdjustButton(player, 17, 100, Material.LIME_STAINED_GLASS_PANE);

        // Larger adjustments on row above
        addAdjustButton(player, 1, -1000, Material.RED_STAINED_GLASS_PANE);
        addAdjustButton(player, 7, 1000, Material.LIME_STAINED_GLASS_PANE);
        addAdjustButton(player, 8, 10000, Material.LIME_STAINED_GLASS_PANE);

        // Confirm
        if (canAfford) {
            String finalTargetName = targetName;
            Icon confirm = GuiHelper.createIcon(Material.LIME_DYE, ChatColor.GREEN + "" + ChatColor.BOLD + "Confirm Payment",
                    "", ChatColor.GRAY + "Send " + ChatColor.GOLD + FormatUtils.format(amount) + ChatColor.GRAY + " to " + finalTargetName);
            confirm.onClick(e -> {
                if (!senderData.canAfford(amount) || amount.signum() <= 0) {
                    player.sendMessage(ChatColor.RED + "Cannot afford this payment.");
                    return;
                }
                Player target = Bukkit.getPlayer(UUID.fromString(targetUuid));
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Player is not online.");
                    return;
                }
                PendingTransaction tx = new PendingTransaction(senderData.getIdentifier(), targetUuid, amount, TransactionType.PAYMENT);
                PendingTransaction.add(tx);
                player.sendMessage(ChatColor.GREEN + "Payment request sent to " + finalTargetName + "! They have 60 seconds to accept.");
                target.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + senderData.getName() + ChatColor.YELLOW +
                        " wants to pay you " + ChatColor.GOLD + FormatUtils.format(amount) + ChatColor.YELLOW +
                        " cookies! Open " + ChatColor.WHITE + "/clicker" + ChatColor.YELLOW + " to accept.");
                new SocialMainGui(player, senderData).open();
            });
            addItem(22, confirm);
        }

        // Cancel
        Icon cancel = GuiHelper.createIcon(Material.RED_DYE, ChatColor.RED + "Cancel");
        cancel.onClick(e -> new MoneyActionsGui(player, senderData, targetUuid, "social").open());
        addItem(18, cancel);
    }

    private void addAdjustButton(Player player, int slot, long change, Material mat) {
        String prefix = change > 0 ? ChatColor.GREEN + "+" : ChatColor.RED + "";
        Icon icon = GuiHelper.createIcon(mat, prefix + FormatUtils.format(Math.abs(change)));
        icon.onClick(e -> new PaymentGui(player, senderData, targetUuid, amount.add(BigDecimal.valueOf(change))).open());
        addItem(slot, icon);
    }
}
