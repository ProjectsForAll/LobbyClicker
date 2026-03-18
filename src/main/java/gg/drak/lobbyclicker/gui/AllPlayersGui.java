package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.redis.RedisManager;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AllPlayersGui extends PaginationMonitor {
    private final PlayerData data;

    public AllPlayersGui(Player player, PlayerData data, int page) {
        super(player, "all-players", MonitorStyle.title(ChatColor.YELLOW, "All Players"), page);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        Player player = (Player) event.getPlayer();

        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new SocialMainGui(p, data).open());

        // Build combined list: local players + cross-server players
        List<PlayerEntry> entries = new ArrayList<>();

        // Local online players (exclude self)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().toString().equals(data.getIdentifier())) continue;
            entries.add(new PlayerEntry(p.getUniqueId().toString(), p.getName(), null, true));
        }

        // Cross-server players (if Redis is enabled)
        RedisManager redis = LobbyClicker.getRedisManager();
        if (redis != null) {
            for (RedisManager.CrossServerPlayer csp : redis.getCrossServerPlayerList()) {
                // Don't add if already listed as local
                if (Bukkit.getPlayer(UUID.fromString(csp.getUuid())) != null) continue;
                entries.add(new PlayerEntry(csp.getUuid(), csp.getName(), csp.getPrettyName(), false));
            }
        }

        populatePagedContent(entries, (entry, slot) -> {
            boolean isFriend = data.getFriends().contains(entry.uuid);

            List<String> lore = new ArrayList<>();
            if (isFriend) lore.add(ChatColor.GREEN + "Friend");
            if (!entry.local) lore.add(ChatColor.GRAY + "Server: " + ChatColor.WHITE + entry.server);
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click for actions");

            Icon icon = playerHeadIcon(entry.uuid,
                    (entry.local ? ChatColor.GREEN : ChatColor.AQUA) + entry.name,
                    p -> new PlayerActionGui(p, data, entry.uuid, "all").open(),
                    lore.toArray(new String[0]));
            addItem(slot, icon);
        });

        addPaginationArrows(entries, newPage -> new AllPlayersGui(player, data, newPage).open());
    }

    private static class PlayerEntry {
        final String uuid;
        final String name;
        final String server; // null for local
        final boolean local;

        PlayerEntry(String uuid, String name, String server, boolean local) {
            this.uuid = uuid;
            this.name = name;
            this.server = server;
            this.local = local;
        }
    }
}
