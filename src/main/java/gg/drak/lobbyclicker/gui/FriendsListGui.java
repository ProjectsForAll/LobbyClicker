package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.redis.RedisManager;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.*;

public class FriendsListGui extends PaginationMonitor {
    private final PlayerData data;

    public FriendsListGui(Player player, PlayerData data, int page) {
        super(player, "friends-list", MonitorStyle.title(ChatColor.GREEN, "Friends"), page);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new FriendsMenuGui(p, data).open());

        RedisManager redis = LobbyClicker.getRedisManager();

        // Sort friends: online first (local > cross-server > offline), then alphabetical
        List<String> friendsList = new ArrayList<>(data.getFriends());
        friendsList.sort((a, b) -> {
            int scoreA = getFriendSortScore(a, redis);
            int scoreB = getFriendSortScore(b, redis);
            if (scoreA != scoreB) return Integer.compare(scoreA, scoreB); // lower = more online
            String nameA = getFriendName(a, redis);
            String nameB = getFriendName(b, redis);
            return nameA.compareToIgnoreCase(nameB);
        });

        populatePagedContent(friendsList, (friendUuid, slot) -> {
            String friendName = friendUuid;
            PlayerData friendData = PlayerManager.getPlayer(friendUuid).orElse(null);
            if (friendData != null) friendName = friendData.getName();
            else {
                try { friendName = Bukkit.getOfflinePlayer(UUID.fromString(friendUuid)).getName(); } catch (Exception ignored) {}
            }
            if (friendName == null) friendName = friendUuid.substring(0, 8);

            boolean localOnline = Bukkit.getPlayer(UUID.fromString(friendUuid)) != null;
            boolean crossServerOnline = false;
            String crossServerName = null;

            if (!localOnline && redis != null) {
                RedisManager.CrossServerPlayer csp = redis.getCrossServerPlayer(friendUuid);
                if (csp != null) {
                    crossServerOnline = true;
                    crossServerName = csp.getPrettyName();
                    if (friendName.equals(friendUuid) || friendName.equals(friendUuid.substring(0, 8))) {
                        friendName = csp.getName();
                    }
                }
            }

            ChatColor nameColor;
            String statusLine;
            if (localOnline) {
                nameColor = ChatColor.GREEN;
                statusLine = ChatColor.GREEN + "Online";
            } else if (crossServerOnline) {
                nameColor = ChatColor.AQUA;
                statusLine = ChatColor.AQUA + "Online" + ChatColor.GRAY + " (" + crossServerName + ")";
            } else {
                nameColor = ChatColor.GRAY;
                statusLine = ChatColor.GRAY + "Offline";
            }

            String finalFriendUuid = friendUuid;
            Icon icon = playerHeadIcon(friendUuid,
                    nameColor + friendName,
                    p -> new PlayerActionGui(p, data, finalFriendUuid, "friends").open(),
                    "", statusLine, "", ChatColor.YELLOW + "Click for actions");
            addItem(slot, icon);
        });

        addPaginationArrows(friendsList, newPage -> new FriendsListGui(player, data, newPage).open());
    }

    /** 0=local online, 1=cross-server online, 2=offline */
    private static int getFriendSortScore(String uuid, RedisManager redis) {
        if (Bukkit.getPlayer(UUID.fromString(uuid)) != null) return 0;
        if (redis != null && redis.getCrossServerPlayer(uuid) != null) return 1;
        return 2;
    }

    private static String getFriendName(String uuid, RedisManager redis) {
        PlayerData pd = PlayerManager.getPlayer(uuid).orElse(null);
        if (pd != null) return pd.getName();
        if (redis != null) {
            RedisManager.CrossServerPlayer csp = redis.getCrossServerPlayer(uuid);
            if (csp != null) return csp.getName();
        }
        try {
            String n = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
            if (n != null) return n;
        } catch (Exception ignored) {}
        return uuid.substring(0, 8);
    }
}
