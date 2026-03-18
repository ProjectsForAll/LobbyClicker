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

import java.util.*;

public class FriendRequestsGui extends PaginationMonitor {
    private final PlayerData data;

    public FriendRequestsGui(Player player, PlayerData data, int page) {
        super(player, "friend-requests", MonitorStyle.title(ChatColor.GOLD, "Incoming Requests"), page);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new FriendsMenuGui(p, data).open());

        List<String> requests = new ArrayList<>(data.getIncomingFriendRequests());

        if (requests.isEmpty()) {
            setContent(10, GuiHelper.createIcon(Material.PAPER,
                    ChatColor.GRAY + "No incoming requests",
                    "", ChatColor.GRAY + "You have no pending friend requests."));
        }

        populatePagedContent(requests, (senderUuid, slot) -> {
            String senderName = senderUuid.substring(0, 8);
            try {
                String n = Bukkit.getOfflinePlayer(UUID.fromString(senderUuid)).getName();
                if (n != null) senderName = n;
            } catch (Exception ignored) {}

            String finalName = senderName;

            // Build lore with actions listed
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(senderUuid))); } catch (Exception ignored) {}
                meta.setDisplayName(ChatColor.YELLOW + senderName);
                meta.setLore(Arrays.asList(
                        "",
                        ChatColor.GRAY + "Wants to be your friend!",
                        "",
                        ChatColor.GREEN + "Left-click: Accept",
                        ChatColor.RED + "Right-click: Deny",
                        ChatColor.DARK_RED + "Shift-click: Deny & Block"
                ));
                head.setItemMeta(meta);
            }

            Icon icon = new Icon(head);
            icon.onClick(e -> {
                if (e.isShiftClick()) {
                    // Deny & Block
                    data.getIncomingFriendRequests().remove(senderUuid);
                    data.getBans().add(senderUuid);
                    data.getBlocks().add(senderUuid);
                    LobbyClicker.getDatabase().deleteFriendRequestThreaded(senderUuid, data.getIdentifier());
                    LobbyClicker.getDatabase().pushBanThreaded(data.getIdentifier(), senderUuid);
                    LobbyClicker.getDatabase().pushBlockThreaded(data.getIdentifier(), senderUuid);
                    RedisSyncHandler.publishFriendRequestDelete(senderUuid, data.getIdentifier());
                    RedisSyncHandler.publishBanAdd(data.getIdentifier(), senderUuid);
                    RedisSyncHandler.publishBlockAdd(data.getIdentifier(), senderUuid);
                    PlayerManager.getPlayer(senderUuid).ifPresent(sd -> sd.getOutgoingFriendRequests().remove(data.getIdentifier()));
                    player.sendMessage(ChatColor.DARK_RED + "Denied and blocked " + finalName);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.8f);
                } else if (e.isRightClick()) {
                    // Deny
                    data.getIncomingFriendRequests().remove(senderUuid);
                    LobbyClicker.getDatabase().deleteFriendRequestThreaded(senderUuid, data.getIdentifier());
                    RedisSyncHandler.publishFriendRequestDelete(senderUuid, data.getIdentifier());
                    PlayerManager.getPlayer(senderUuid).ifPresent(sd -> sd.getOutgoingFriendRequests().remove(data.getIdentifier()));
                    player.sendMessage(ChatColor.RED + "Denied " + finalName + "'s request.");
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
                } else {
                    // Accept
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
                    player.sendMessage(ChatColor.GREEN + "Accepted " + finalName + "'s friend request!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
                new FriendRequestsGui(player, data, page).open();
            });
            addItem(slot, icon);
        });

        addPaginationArrows(requests, newPage -> new FriendRequestsGui(player, data, newPage).open());
    }
}
