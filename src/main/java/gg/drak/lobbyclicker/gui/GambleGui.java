package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.social.PendingTransaction;
import gg.drak.lobbyclicker.social.TransactionType;
import gg.drak.lobbyclicker.utils.ChatInput;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;
import java.util.UUID;

public class GambleGui extends BaseGui {
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

        // Home button
        Icon home = GuiHelper.homeButton();
        home.onClick(e -> new ClickerGui(player, senderData).open());
        addItem(0, home);

        String targetName = targetUuid.substring(0, 8);
        try { String n = Bukkit.getOfflinePlayer(UUID.fromString(targetUuid)).getName(); if (n != null) targetName = n; } catch (Exception ignored) {}

        boolean canAfford = senderData.canAfford(amount) && amount.signum() > 0;

        // "+" banner at slot 2 (index 2)
        String amountLore = ChatColor.GRAY + "Bet: " + ChatColor.GOLD + FormatUtils.format(amount);
        addItem(2, BannerChar.of("+", BannerChar.BannerColor.RED, BannerChar.BannerColor.WHITE).toIcon(amountLore));

        // Banner display at slots 3-6 (index 3-6)
        String[] display = BannerUtil.parseBannerDisplay(amount);
        for (int i = 0; i < 4; i++) {
            addItem(3 + i, BannerChar.of(display[i], BannerChar.BannerColor.RED, BannerChar.BannerColor.WHITE).toIcon(amountLore));
        }

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

        // Increase
        addAdjustButton(player, 15, 1, Material.LIME_STAINED_GLASS_PANE);
        addAdjustButton(player, 16, 10, Material.LIME_STAINED_GLASS_PANE);
        addAdjustButton(player, 17, 100, Material.LIME_STAINED_GLASS_PANE);

        // Larger adjustments
        addAdjustButton(player, 1, -1000, Material.RED_STAINED_GLASS_PANE);
        addAdjustButton(player, 7, 1000, Material.LIME_STAINED_GLASS_PANE);
        addAdjustButton(player, 8, 10000, Material.LIME_STAINED_GLASS_PANE);

        // None button
        Icon none = GuiHelper.createIcon(Material.RED_DYE, ChatColor.RED + "" + ChatColor.BOLD + "None",
                "", ChatColor.GRAY + "Reset to 0");
        none.onClick(e -> new GambleGui(player, senderData, targetUuid, BigDecimal.ZERO).open());
        addItem(12, none);

        // All button
        Icon all = GuiHelper.createIcon(Material.LIME_DYE, ChatColor.GREEN + "" + ChatColor.BOLD + "All",
                "", ChatColor.GRAY + "Set to max: " + ChatColor.GOLD + FormatUtils.format(senderData.getCookies()));
        all.onClick(e -> new GambleGui(player, senderData, targetUuid, senderData.getCookies()).open());
        addItem(14, all);

        // Custom amount button
        Icon custom = GuiHelper.createIcon(Material.OAK_SIGN, ChatColor.YELLOW + "" + ChatColor.BOLD + "Custom Amount",
                "", ChatColor.GRAY + "Type an amount in chat",
                ChatColor.GRAY + "e.g. " + ChatColor.WHITE + "2.5m" + ChatColor.GRAY + ", " + ChatColor.WHITE + "20000" + ChatColor.GRAY + ", " + ChatColor.WHITE + "5k");
        custom.onClick(e -> {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Type the bet amount in chat (e.g. 2.5m, 20000, 5k). Type " + ChatColor.WHITE + "cancel" + ChatColor.YELLOW + " to cancel.");
            ChatInput.request(player, input -> {
                if (input == null || input.equalsIgnoreCase("cancel")) {
                    new GambleGui(player, senderData, targetUuid, amount).open();
                    return;
                }
                BigDecimal parsed = FormatUtils.parseShorthand(input);
                if (parsed == null) {
                    player.sendMessage(ChatColor.RED + "Invalid amount: " + input);
                    new GambleGui(player, senderData, targetUuid, amount).open();
                    return;
                }
                // Cap to balance
                if (parsed.compareTo(senderData.getCookies()) > 0) parsed = senderData.getCookies();
                new GambleGui(player, senderData, targetUuid, parsed).open();
            });
        });
        addItem(20, custom);

        if (canAfford) {
            String finalTargetName = targetName;
            Icon send = GuiHelper.createIcon(Material.LIME_DYE, ChatColor.GREEN + "" + ChatColor.BOLD + "Send Bet Request",
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

        Icon cancel = GuiHelper.createIcon(Material.RED_DYE, ChatColor.RED + "Cancel");
        cancel.onClick(e -> new MoneyActionsGui(player, senderData, targetUuid, "social").open());
        addItem(18, cancel);
    }

    private void addAdjustButton(Player player, int slot, long change, Material mat) {
        String prefix = change > 0 ? ChatColor.GREEN + "+" : ChatColor.RED + "";
        Icon icon = GuiHelper.createIcon(mat, prefix + FormatUtils.format(Math.abs(change)));
        icon.onClick(e -> new GambleGui(player, senderData, targetUuid, amount.add(BigDecimal.valueOf(change))).open());
        addItem(slot, icon);
    }
}
