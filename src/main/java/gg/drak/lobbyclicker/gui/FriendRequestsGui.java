package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class FriendRequestsGui extends PaginationMonitor {
    private final PlayerData data;
    private final Consumer<Player> backAction;

    public FriendRequestsGui(Player player, PlayerData data, Consumer<Player> backAction) {
        this(player, data, 0, backAction);
    }

    public FriendRequestsGui(Player player, PlayerData data) {
        this(player, data, 0, p -> new MailGui(p, data).open());
    }

    public FriendRequestsGui(Player player, PlayerData data, int page) {
        this(player, data, page, p -> new FriendsMenuGui(p, data).open());
    }

    public FriendRequestsGui(Player player, PlayerData data, int page, Consumer<Player> backAction) {
        super(player, "friend-requests", MonitorStyle.title(ChatColor.GOLD, "Incoming Requests"), page);
        this.data = data;
        this.backAction = backAction;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(backAction);

        List<String> requests = new ArrayList<>(data.getIncomingFriendRequests());

        if (requests.isEmpty()) {
            addItem(22, GuiHelper.createIcon(Material.PAPER,
                    ChatColor.GRAY + "No incoming requests",
                    "", ChatColor.GRAY + "You have no pending friend requests."));
        }

        populatePagedContent(requests, (senderUuid, slot) -> {
            if (senderUuid == null) return;
            String senderName = senderUuid.substring(0, 8);
            try { String n = Bukkit.getOfflinePlayer(UUID.fromString(senderUuid)).getName(); if (n != null) senderName = n; } catch (Exception ignored) {}

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(senderUuid))); } catch (Exception ignored) {}
                meta.setDisplayName(ChatColor.GREEN + senderName);
                meta.setLore(Arrays.asList(
                        "", ChatColor.GRAY + "Friend request",
                        "",
                        ChatColor.YELLOW + "Left-click: " + ChatColor.WHITE + "View details",
                        ChatColor.GREEN + "Shift+left-click: " + ChatColor.WHITE + "Accept",
                        ChatColor.RED + "Shift+right-click: " + ChatColor.WHITE + "Decline"));
                head.setItemMeta(meta);
            }
            Icon icon = new Icon(head);
            final String finalSenderName = senderName;
            icon.onClick(e -> {
                if (e.isShiftClick() && e.isLeftClick()) {
                    acceptRequest(player, senderUuid, finalSenderName);
                    new FriendRequestsGui(player, data, page, backAction).open();
                } else if (e.isShiftClick() && e.isRightClick()) {
                    declineRequest(player, senderUuid);
                    new FriendRequestsGui(player, data, page, backAction).open();
                } else if (e.isLeftClick()) {
                    // Open confirmation GUI
                    Icon info = GuiHelper.createIcon(Material.PLAYER_HEAD,
                            ChatColor.GREEN + "" + ChatColor.BOLD + "Friend Request from " + finalSenderName,
                            "", ChatColor.GRAY + "Accept this friend request?");
                    Consumer<Player> returnHere = p -> new FriendRequestsGui(p, data, page, backAction).open();
                    new FriendRequestConfirmGui(player, data, senderUuid, finalSenderName, returnHere).open();
                }
            });
            addItem(slot, icon);
        });

        addPaginationArrows(requests, newPage -> new FriendRequestsGui(player, data, newPage, backAction).open());
    }

    private void acceptRequest(Player player, String senderUuid, String senderName) {
        long now = System.currentTimeMillis();
        data.getIncomingFriendRequests().remove(senderUuid);
        data.getFriends().add(senderUuid);
        LobbyClicker.getDatabase().deleteFriendRequestThreaded(senderUuid, data.getIdentifier());
        LobbyClicker.getDatabase().pushFriendThreaded(data.getIdentifier(), senderUuid, now);
        RedisSyncHandler.publishFriendRequestDelete(senderUuid, data.getIdentifier());
        RedisSyncHandler.publishFriendAdd(data.getIdentifier(), senderUuid, now);
        PlayerManager.getPlayer(senderUuid).ifPresent(sd -> {
            sd.getOutgoingFriendRequests().remove(data.getIdentifier());
            sd.getFriends().add(data.getIdentifier());
            sd.asPlayer().ifPresent(sp -> sp.sendMessage(ChatColor.GREEN + data.getName() + " accepted your friend request!"));
        });
        player.sendMessage(ChatColor.GREEN + "You are now friends with " + senderName + "!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void declineRequest(Player player, String senderUuid) {
        data.getIncomingFriendRequests().remove(senderUuid);
        LobbyClicker.getDatabase().deleteFriendRequestThreaded(senderUuid, data.getIdentifier());
        RedisSyncHandler.publishFriendRequestDelete(senderUuid, data.getIdentifier());
        PlayerManager.getPlayer(senderUuid).ifPresent(sd -> sd.getOutgoingFriendRequests().remove(data.getIdentifier()));
        player.sendMessage(ChatColor.RED + "Declined friend request.");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
    }

    /** Static helper to accept — used by the confirmation GUI too. */
    static void acceptRequestStatic(Player player, PlayerData data, String senderUuid, String senderName) {
        long now = System.currentTimeMillis();
        data.getIncomingFriendRequests().remove(senderUuid);
        data.getFriends().add(senderUuid);
        LobbyClicker.getDatabase().deleteFriendRequestThreaded(senderUuid, data.getIdentifier());
        LobbyClicker.getDatabase().pushFriendThreaded(data.getIdentifier(), senderUuid, now);
        RedisSyncHandler.publishFriendRequestDelete(senderUuid, data.getIdentifier());
        RedisSyncHandler.publishFriendAdd(data.getIdentifier(), senderUuid, now);
        PlayerManager.getPlayer(senderUuid).ifPresent(sd -> {
            sd.getOutgoingFriendRequests().remove(data.getIdentifier());
            sd.getFriends().add(data.getIdentifier());
            sd.asPlayer().ifPresent(sp -> sp.sendMessage(ChatColor.GREEN + data.getName() + " accepted your friend request!"));
        });
        player.sendMessage(ChatColor.GREEN + "You are now friends with " + senderName + "!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    static void declineRequestStatic(Player player, PlayerData data, String senderUuid) {
        data.getIncomingFriendRequests().remove(senderUuid);
        LobbyClicker.getDatabase().deleteFriendRequestThreaded(senderUuid, data.getIdentifier());
        RedisSyncHandler.publishFriendRequestDelete(senderUuid, data.getIdentifier());
        PlayerManager.getPlayer(senderUuid).ifPresent(sd -> sd.getOutgoingFriendRequests().remove(data.getIdentifier()));
        player.sendMessage(ChatColor.RED + "Declined friend request.");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
    }
}
