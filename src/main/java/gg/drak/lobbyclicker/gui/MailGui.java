package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.SimpleGuiMonitor;
import gg.drak.lobbyclicker.social.PendingTransaction;
import gg.drak.lobbyclicker.social.TransactionType;
import mc.obliviate.inventory.Icon;
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
        super(player, "mail", MonitorStyle.title("yellow", "Mail"), MonitorStyle.ROWS_SMALL);
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
        java.util.function.Consumer<Player> returnToMail = p -> new MailGui(p, data).open();
        friendReqs.onClick(e -> new FriendRequestsGui(player, data, returnToMail).open());
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
                ChatColor.YELLOW + "Click to view");
        payments.onClick(e -> new PaymentRequestsGui(player, data, returnToMail).open());
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
                ChatColor.YELLOW + "Click to view");
        gambles.onClick(e -> new GambleRequestsGui(player, data, returnToMail).open());
        setContent(5, gambles);
    }
}
