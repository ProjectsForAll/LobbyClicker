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

import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

public class GambleAcceptGui extends ConfirmationMonitor {
    private final PlayerData receiverData;
    private final PendingTransaction transaction;
    private final Consumer<Player> backAction;
    private static final Random RANDOM = new Random();

    public GambleAcceptGui(Player player, PlayerData receiverData, PendingTransaction transaction, Consumer<Player> backAction) {
        super(player, "gamble-accept", ChatColor.GOLD + "" + ChatColor.BOLD + "Incoming Bet");
        this.receiverData = receiverData;
        this.transaction = transaction;
        this.backAction = backAction;
    }

    public GambleAcceptGui(Player player, PlayerData receiverData, PendingTransaction transaction) {
        this(player, receiverData, transaction, p -> new MailGui(p, receiverData).open());
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(receiverData, null);

        String senderName = transaction.getSenderUuid().substring(0, 8);
        try { String n = Bukkit.getOfflinePlayer(UUID.fromString(transaction.getSenderUuid())).getName(); if (n != null) senderName = n; } catch (Exception ignored) {}

        boolean canAfford = receiverData.canAfford(transaction.getAmount());

        Icon info = GuiHelper.createIcon(Material.EMERALD,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Bet: " + FormatUtils.format(transaction.getAmount()) + " cookies",
                "",
                ChatColor.GRAY + "From: " + ChatColor.WHITE + senderName,
                ChatColor.GRAY + "Your balance: " + ChatColor.WHITE + FormatUtils.format(receiverData.getCookies()),
                "",
                ChatColor.GRAY + "50/50 chance. Winner takes " + ChatColor.GOLD + FormatUtils.format(transaction.getAmount()) + ChatColor.GRAY + " from the loser.",
                "",
                canAfford ? ChatColor.GREEN + "You can afford this bet." : ChatColor.RED + "You cannot afford this bet!");

        if (canAfford) {
            buildConfirmation(info, "Accept Bet", "Accept this bet?",
                    p -> acceptGamble(p),
                    backAction);
        } else {
            fillMonitorBorder();
            buildStandardActionBar(backAction);
            setContent(3, info);
            Icon declineIcon = gg.drak.lobbyclicker.gui.monitor.MonitorStyle.cancelButton("Decline");
            declineIcon.onClick(e -> declineGamble(player, receiverData, transaction, backAction));
            setContent(5, declineIcon);
        }
    }

    /** Execute the gamble acceptance logic. Also callable directly from shift-click. */
    public static void acceptGamble(Player player, PlayerData receiverData, PendingTransaction transaction, Consumer<Player> backAction) {
        PendingTransaction tx = PendingTransaction.get(transaction.getSenderUuid(), receiverData.getIdentifier());
        if (tx == null) {
            player.sendMessage(ChatColor.RED + "This bet has expired.");
            backAction.accept(player);
            return;
        }
        PlayerData senderData = PlayerManager.getPlayer(tx.getSenderUuid()).orElse(null);
        if (senderData == null || !senderData.canAfford(tx.getAmount())) {
            player.sendMessage(ChatColor.RED + "The other player can no longer afford this bet.");
            PendingTransaction.remove(tx.getSenderUuid(), receiverData.getIdentifier());
            backAction.accept(player);
            return;
        }
        if (!receiverData.canAfford(tx.getAmount())) {
            player.sendMessage(ChatColor.RED + "You can no longer afford this bet.");
            backAction.accept(player);
            return;
        }

        PendingTransaction.remove(tx.getSenderUuid(), receiverData.getIdentifier());

        boolean senderWins = RANDOM.nextBoolean();
        PlayerData winner = senderWins ? senderData : receiverData;
        PlayerData loser = senderWins ? receiverData : senderData;

        loser.removeCookies(tx.getAmount());
        winner.addCookies(tx.getAmount());
        winner.save(true);
        loser.save(true);

        String winnerName = winner.getName();
        String loserName = loser.getName();
        String amountStr = FormatUtils.format(tx.getAmount());

        winner.asPlayer().ifPresent(wp -> {
            wp.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "You won the bet! " +
                    ChatColor.YELLOW + "+" + amountStr + " cookies from " + loserName);
            wp.playSound(wp.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        });
        loser.asPlayer().ifPresent(lp -> {
            lp.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You lost the bet! " +
                    ChatColor.YELLOW + "-" + amountStr + " cookies to " + winnerName);
            lp.playSound(lp.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        });
        backAction.accept(player);
    }

    private void acceptGamble(Player p) {
        acceptGamble(p, receiverData, transaction, backAction);
    }

    public static void declineGamble(Player player, PlayerData receiverData, PendingTransaction transaction, Consumer<Player> backAction) {
        PendingTransaction.remove(transaction.getSenderUuid(), receiverData.getIdentifier());
        Player sender = Bukkit.getPlayer(UUID.fromString(transaction.getSenderUuid()));
        if (sender != null) sender.sendMessage(ChatColor.RED + receiverData.getName() + " declined your bet.");
        player.sendMessage(ChatColor.RED + "Bet declined.");
        backAction.accept(player);
    }
}
