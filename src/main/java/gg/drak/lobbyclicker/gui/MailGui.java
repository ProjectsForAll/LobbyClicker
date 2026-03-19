package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.SimpleGuiMonitor;
import gg.drak.lobbyclicker.social.PendingTransaction;
import gg.drak.lobbyclicker.social.TransactionType;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MailGui extends SimpleGuiMonitor {
    private final PlayerData data;

    public MailGui(Player player, PlayerData data) {
        super(player, "mail", MonitorStyle.title(ChatColor.YELLOW, "Mail"), MonitorStyle.ROWS_SMALL);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        Player player = (Player) event.getPlayer();
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new ClickerGui(p, data).open());

        Set<String> incomingRequests = data.getIncomingFriendRequests();
        List<PendingTransaction> pending = PendingTransaction.getAllForReceiver(data.getIdentifier());

        int friendReqCount = incomingRequests.size();
        long paymentCount = pending.stream().filter(tx -> tx.getType() == TransactionType.PAYMENT).count();
        long gambleCount = pending.stream().filter(tx -> tx.getType() == TransactionType.GAMBLE).count();

        // Friend Requests
        Icon friendReqs = GuiHelper.createIcon(
                friendReqCount > 0 ? Material.WRITABLE_BOOK : Material.BOOK,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Friend Requests",
                "",
                friendReqCount > 0
                        ? ChatColor.YELLOW + "" + friendReqCount + " incoming request" + (friendReqCount != 1 ? "s" : "")
                        : ChatColor.GRAY + "No incoming requests",
                "",
                friendReqCount > 0 ? ChatColor.YELLOW + "Click to view" : "");
        if (friendReqCount > 0) {
            friendReqs.onClick(e -> new FriendRequestsGui(player, data).open());
        }
        setContent(1, friendReqs);

        // Payments
        Icon payments = GuiHelper.createIcon(
                paymentCount > 0 ? Material.GOLD_INGOT : Material.IRON_INGOT,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Payment Requests",
                "",
                paymentCount > 0
                        ? ChatColor.YELLOW + "" + paymentCount + " pending payment" + (paymentCount != 1 ? "s" : "")
                        : ChatColor.GRAY + "No pending payments",
                "",
                paymentCount > 0 ? ChatColor.YELLOW + "Click to view" : "");
        if (paymentCount > 0) {
            payments.onClick(e -> {
                PendingTransaction tx = PendingTransaction.getForReceiver(data.getIdentifier(), TransactionType.PAYMENT);
                if (tx != null) {
                    new PaymentAcceptGui(player, data, tx).open();
                } else {
                    player.sendMessage(ChatColor.RED + "No pending payments.");
                }
            });
        }
        setContent(3, payments);

        // Gambles
        Icon gambles = GuiHelper.createIcon(
                gambleCount > 0 ? Material.EMERALD : Material.IRON_NUGGET,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Gambling Requests",
                "",
                gambleCount > 0
                        ? ChatColor.YELLOW + "" + gambleCount + " pending bet" + (gambleCount != 1 ? "s" : "")
                        : ChatColor.GRAY + "No pending bets",
                "",
                gambleCount > 0 ? ChatColor.YELLOW + "Click to view" : "");
        if (gambleCount > 0) {
            gambles.onClick(e -> {
                PendingTransaction tx = PendingTransaction.getForReceiver(data.getIdentifier(), TransactionType.GAMBLE);
                if (tx != null) {
                    new GambleAcceptGui(player, data, tx).open();
                } else {
                    player.sendMessage(ChatColor.RED + "No pending bets.");
                }
            });
        }
        setContent(5, gambles);
    }
}
