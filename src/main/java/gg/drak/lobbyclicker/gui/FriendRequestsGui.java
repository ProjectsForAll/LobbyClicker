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
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendRequestsGui extends PaginationMonitor {
    private final PlayerData data;

    public FriendRequestsGui(Player player, PlayerData data) {
        this(player, data, 0);
    }

    public FriendRequestsGui(Player player, PlayerData data, int page) {
        super(player, "friend-requests", MonitorStyle.title(ChatColor.GOLD, "Incoming Requests"), page);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new MailGui(p, data).open());

        List<String> requests = new ArrayList<>(data.getIncomingFriendRequests());
        if (requests.isEmpty()) {
            setContent(3, GuiHelper.createIcon(Material.BARRIER,
                    ChatColor.RED + "No Requests",
                    "", ChatColor.GRAY + "You have no incoming friend requests."));
            return;
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
                meta.setLore(java.util.Arrays.asList(
                        "", ChatColor.YELLOW + "Click to accept",
                        ChatColor.RED + "Shift-click to decline"));
                head.setItemMeta(meta);
            }
            Icon icon = new Icon(head);
            icon.onClick(e -> {
                if (e.isShiftClick()) {
                    data.getIncomingFriendRequests().remove(senderUuid);
                    LobbyClicker.getDatabase().deleteFriendRequestThreaded(senderUuid, data.getIdentifier());
                    RedisSyncHandler.publishFriendRequestDelete(senderUuid, data.getIdentifier());
                    PlayerManager.getPlayer(senderUuid).ifPresent(sd -> sd.getOutgoingFriendRequests().remove(data.getIdentifier()));
                    player.sendMessage(ChatColor.RED + "Declined friend request.");
                } else {
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
                    player.sendMessage(ChatColor.GREEN + "Friend request accepted!");
                }
                new FriendRequestsGui(player, data).open();
            });
            addItem(slot, icon);
        });
        addPaginationArrows(requests, newPage -> {});
    }
}
