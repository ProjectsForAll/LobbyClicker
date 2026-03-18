package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MenuMonitor;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class FriendsMenuGui extends MenuMonitor {
    private final PlayerData data;

    public FriendsMenuGui(Player player, PlayerData data) {
        super(player, "friends-menu", MonitorStyle.title(ChatColor.GREEN, "Manage Friends"), MonitorStyle.ROWS_SMALL);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);

        int friendCount = data.getFriends().size();
        Icon friends = MonitorStyle.menuButton(Material.TOTEM_OF_UNDYING, ChatColor.GREEN,
                "Friends", friendCount + " friend(s)");
        friends.onClick(e -> new FriendsListGui(player, data, 0).open());
        addOption(friends);

        int incomingCount = data.getIncomingFriendRequests().size();
        Icon incoming = MonitorStyle.menuButton(
                incomingCount > 0 ? Material.WRITABLE_BOOK : Material.BOOK, ChatColor.GOLD,
                "Incoming Requests", incomingCount > 0 ? incomingCount + " pending" : "No pending requests");
        incoming.onClick(e -> new FriendRequestsGui(player, data, 0).open());
        addOption(incoming);

        int outgoingCount = data.getOutgoingFriendRequests().size();
        Icon outgoing = MonitorStyle.menuButton(Material.PAPER, ChatColor.YELLOW,
                "Outgoing Requests", outgoingCount > 0 ? outgoingCount + " sent" : "No outgoing requests");
        outgoing.onClick(e -> new OutgoingRequestsGui(player, data, 0).open());
        addOption(outgoing);

        buildMenu(p -> new SocialMainGui(p, data).open());
    }
}
