package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.social.RealmManager;
import mc.obliviate.inventory.Gui;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class SocialMainGui extends Gui {
    private final PlayerData data;

    public SocialMainGui(Player player, PlayerData data) {
        super(player, "social-main", ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Social Menu", 3);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        int friendCount = data.getFriends().size();
        Icon friends = GuiHelper.createIcon(Material.TOTEM_OF_UNDYING,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Friends",
                "", ChatColor.GRAY + "You have " + ChatColor.WHITE + friendCount + ChatColor.GRAY + " friend(s)",
                "", ChatColor.YELLOW + "Click to view");
        friends.onClick(e -> new FriendsListGui(player, data, 0).open());
        addItem(10, friends);

        int requestCount = data.getIncomingFriendRequests().size();
        Icon requests = GuiHelper.createIcon(
                requestCount > 0 ? Material.WRITABLE_BOOK : Material.BOOK,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Friend Requests",
                "", ChatColor.GRAY + "Pending: " + ChatColor.WHITE + requestCount,
                "", ChatColor.YELLOW + "Click to manage");
        requests.onClick(e -> new FriendRequestsGui(player, data, 0).open());
        addItem(11, requests);

        int viewerCount = RealmManager.getViewers(data.getIdentifier()).size();
        Icon viewers = GuiHelper.createIcon(Material.SPYGLASS,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Realm Viewers",
                "", ChatColor.GRAY + "Currently viewing: " + ChatColor.WHITE + viewerCount,
                "", ChatColor.YELLOW + "Click to view");
        viewers.onClick(e -> new RealmViewersGui(player, data).open());
        addItem(12, viewers);

        // All Players - use viewer's head
        Icon allPlayers = GuiHelper.playerHead(player,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "All Players",
                "", ChatColor.GRAY + "Browse online players",
                "", ChatColor.YELLOW + "Click to view");
        allPlayers.onClick(e -> new AllPlayersGui(player, data, 0).open());
        addItem(14, allPlayers);

        Icon bans = GuiHelper.createIcon(Material.IRON_DOOR,
                ChatColor.RED + "" + ChatColor.BOLD + "Manage Bans",
                "", ChatColor.GRAY + "Banned: " + ChatColor.WHITE + data.getBans().size(),
                "", ChatColor.YELLOW + "Click to manage");
        bans.onClick(e -> new BanListGui(player, data, 0).open());
        addItem(15, bans);

        Icon blocks = GuiHelper.createIcon(Material.BARRIER,
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "Manage Blocks",
                "", ChatColor.GRAY + "Blocked: " + ChatColor.WHITE + data.getBlocks().size(),
                "", ChatColor.YELLOW + "Click to manage");
        blocks.onClick(e -> new BlockListGui(player, data, 0).open());
        addItem(16, blocks);

        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new ClickerGui(player, data).open());
        addItem(22, back);
    }
}
