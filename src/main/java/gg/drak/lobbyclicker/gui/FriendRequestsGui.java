package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import gg.drak.lobbyclicker.settings.SettingType;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendRequestsGui extends BaseGui {
    private final PlayerData data;
    private final int page;
    private static final int ITEMS_PER_PAGE = 7;
    // Each request takes 1 row: head + accept + deny + ban + block
    private static final int[] HEAD_SLOTS = {10, 19, 28, 37, 46, 1, 2};

    public FriendRequestsGui(Player player, PlayerData data, int page) {
        super(player, "friend-requests", ChatColor.GOLD + "" + ChatColor.BOLD + "Friend Requests", 6);
        this.data = data;
        this.page = page;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        // Home button
        Icon home = GuiHelper.homeButton();
        home.onClick(e -> new ClickerGui(player, data).open());
        addItem(0, home);

        List<String> requests = new ArrayList<>(data.getIncomingFriendRequests());
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, requests.size());

        if (requests.isEmpty()) {
            addItem(22, GuiHelper.createIcon(Material.PAPER, ChatColor.GRAY + "No pending requests",
                    "", ChatColor.GRAY + "You have no incoming friend requests."));
        }

        // Layout: each request gets a row with head + 4 action buttons
        int[] rows = {0, 1, 2, 3, 4};
        for (int i = start; i < end && (i - start) < rows.length; i++) {
            String senderUuid = requests.get(i);
            int row = rows[i - start];
            int baseSlot = row * 9;

            String senderName = senderUuid.substring(0, 8);
            try {
                String n = Bukkit.getOfflinePlayer(UUID.fromString(senderUuid)).getName();
                if (n != null) senderName = n;
            } catch (Exception ignored) {}

            // Player head (slot 1 of row)
            Icon head = GuiHelper.playerHead(senderUuid,
                    ChatColor.YELLOW + senderName,
                    "", ChatColor.GRAY + "Wants to be your friend!");
            addItem(baseSlot + 1, head);

            // Accept (slot 3)
            String finalSenderName = senderName;
            String finalSenderUuid = senderUuid;
            Icon accept = GuiHelper.createIcon(Material.LIME_DYE, ChatColor.GREEN + "Accept");
            accept.onClick(e -> {
                long now = System.currentTimeMillis();
                data.getIncomingFriendRequests().remove(finalSenderUuid);
                data.getFriends().add(finalSenderUuid);
                LobbyClicker.getDatabase().deleteFriendRequestThreaded(finalSenderUuid, data.getIdentifier());
                LobbyClicker.getDatabase().pushFriendThreaded(data.getIdentifier(), finalSenderUuid, now);
                RedisSyncHandler.publishFriendRequestDelete(finalSenderUuid, data.getIdentifier());
                RedisSyncHandler.publishFriendAdd(data.getIdentifier(), finalSenderUuid, now);
                PlayerManager.getPlayer(finalSenderUuid).ifPresent(sd -> {
                    sd.getOutgoingFriendRequests().remove(data.getIdentifier());
                    sd.getFriends().add(data.getIdentifier());
                    sd.asPlayer().ifPresent(sp -> sp.sendMessage(ChatColor.GREEN + data.getName() + " accepted your friend request!"));
                });
                player.sendMessage(ChatColor.GREEN + "Accepted " + finalSenderName + "'s friend request!");
                new FriendRequestsGui(player, data, page).open();
            });
            addItem(baseSlot + 3, accept);

            // Deny (slot 4)
            Icon deny = GuiHelper.createIcon(Material.RED_DYE, ChatColor.RED + "Deny");
            deny.onClick(e -> {
                data.getIncomingFriendRequests().remove(finalSenderUuid);
                LobbyClicker.getDatabase().deleteFriendRequestThreaded(finalSenderUuid, data.getIdentifier());
                RedisSyncHandler.publishFriendRequestDelete(finalSenderUuid, data.getIdentifier());
                PlayerManager.getPlayer(finalSenderUuid).ifPresent(sd -> sd.getOutgoingFriendRequests().remove(data.getIdentifier()));
                player.sendMessage(ChatColor.RED + "Denied " + finalSenderName + "'s request.");
                new FriendRequestsGui(player, data, page).open();
            });
            addItem(baseSlot + 4, deny);

            // Ban (slot 6)
            Icon ban = GuiHelper.createIcon(Material.IRON_DOOR, ChatColor.RED + "Ban");
            ban.onClick(e -> {
                data.getIncomingFriendRequests().remove(finalSenderUuid);
                data.getBans().add(finalSenderUuid);
                LobbyClicker.getDatabase().deleteFriendRequestThreaded(finalSenderUuid, data.getIdentifier());
                LobbyClicker.getDatabase().pushBanThreaded(data.getIdentifier(), finalSenderUuid);
                RedisSyncHandler.publishFriendRequestDelete(finalSenderUuid, data.getIdentifier());
                RedisSyncHandler.publishBanAdd(data.getIdentifier(), finalSenderUuid);
                PlayerManager.getPlayer(finalSenderUuid).ifPresent(sd -> sd.getOutgoingFriendRequests().remove(data.getIdentifier()));
                player.sendMessage(ChatColor.RED + "Denied and banned " + finalSenderName);
                new FriendRequestsGui(player, data, page).open();
            });
            addItem(baseSlot + 6, ban);

            // Block (slot 7)
            Icon block = GuiHelper.createIcon(Material.BARRIER, ChatColor.DARK_RED + "Block");
            block.onClick(e -> {
                data.getIncomingFriendRequests().remove(finalSenderUuid);
                data.getBans().add(finalSenderUuid);
                data.getBlocks().add(finalSenderUuid);
                LobbyClicker.getDatabase().deleteFriendRequestThreaded(finalSenderUuid, data.getIdentifier());
                LobbyClicker.getDatabase().pushBanThreaded(data.getIdentifier(), finalSenderUuid);
                LobbyClicker.getDatabase().pushBlockThreaded(data.getIdentifier(), finalSenderUuid);
                RedisSyncHandler.publishFriendRequestDelete(finalSenderUuid, data.getIdentifier());
                RedisSyncHandler.publishBanAdd(data.getIdentifier(), finalSenderUuid);
                RedisSyncHandler.publishBlockAdd(data.getIdentifier(), finalSenderUuid);
                PlayerManager.getPlayer(finalSenderUuid).ifPresent(sd -> sd.getOutgoingFriendRequests().remove(data.getIdentifier()));
                player.sendMessage(ChatColor.DARK_RED + "Denied and blocked " + finalSenderName);
                new FriendRequestsGui(player, data, page).open();
            });
            addItem(baseSlot + 7, block);
        }

        // Pagination
        if (page > 0) {
            Icon prev = GuiHelper.createIcon(Material.ARROW, ChatColor.YELLOW + "Previous Page");
            prev.onClick(e -> new FriendRequestsGui(player, data, page - 1).open());
            addItem(45, prev);
        }
        if (end < requests.size()) {
            Icon next = GuiHelper.createIcon(Material.ARROW, ChatColor.YELLOW + "Next Page");
            next.onClick(e -> new FriendRequestsGui(player, data, page + 1).open());
            addItem(53, next);
        }

        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new SocialMainGui(player, data).open());
        addItem(49, back);
    }
}
