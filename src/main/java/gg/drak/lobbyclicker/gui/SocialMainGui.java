package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MenuMonitor;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.social.RealmManager;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class SocialMainGui extends MenuMonitor {
    private final PlayerData data;
    private final PlayerData realmOwner;

    public SocialMainGui(Player player, PlayerData data) {
        this(player, data, null);
    }

    public SocialMainGui(Player player, PlayerData data, PlayerData realmOwner) {
        super(player, "social-main", MonitorStyle.title("light_purple", "Social"), MonitorStyle.ROWS_SMALL);
        this.data = data;
        this.realmOwner = realmOwner;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, realmOwner);

        int friendCount = data.getFriends().size();
        int requestCount = data.getIncomingFriendRequests().size();
        String requestNote = requestCount > 0 ? ChatColor.YELLOW + " (" + requestCount + " incoming)" : "";
        Icon friends = MonitorStyle.menuButton(Material.TOTEM_OF_UNDYING, "green",
                "Manage Friends", friendCount + " friend(s)" + requestNote);
        friends.onClick(e -> new FriendsMenuGui(player, data).open());
        addOption(friends);

        int viewerCount = RealmManager.getViewers(data.getIdentifier()).size();
        Icon viewers = MonitorStyle.menuButton(Material.SPYGLASS, "aqua",
                "Realm Viewers", viewerCount + " viewing your realm");
        viewers.onClick(e -> new RealmViewersGui(player, data).open());
        addOption(viewers);

        Icon allPlayers = GuiHelper.playerHead(player,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "All Players",
                "", ChatColor.GRAY + "Browse online players", ChatColor.YELLOW + "Click to open");
        allPlayers.onClick(e -> new AllPlayersGui(player, data, 0).open());
        addOption(allPlayers);

        int banCount = data.getBans().size();
        Icon bans = MonitorStyle.menuButton(Material.IRON_DOOR, "red",
                "Manage Bans", "Banned: " + banCount);
        bans.onClick(e -> new BanListGui(player, data, 0).open());
        addOption(bans);

        int blockCount = data.getBlocks().size();
        Icon blocks = MonitorStyle.menuButton(Material.BARRIER, "dark_red",
                "Manage Blocks", "Blocked: " + blockCount);
        blocks.onClick(e -> new BlockListGui(player, data, 0).open());
        addOption(blocks);

        buildMenu(p -> new ClickerGui(p, data).open());
    }
}
