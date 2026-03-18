package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.*;

public class BanListGui extends PaginationMonitor {
    private final PlayerData data;

    public BanListGui(Player player, PlayerData data, int page) {
        super(player, "ban-list", MonitorStyle.title(ChatColor.RED, "Banned Players"), page);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        Player player = (Player) event.getPlayer();

        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new SocialMainGui(p, data).open());

        List<String> banList = new ArrayList<>(data.getBans());

        populatePagedContent(banList, (uuid, slot) -> {
            String name = uuid.substring(0, 8);
            try { String n = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName(); if (n != null) name = n; } catch (Exception ignored) {}

            Icon icon = playerHeadIcon(uuid,
                    ChatColor.RED + name,
                    p -> {
                        data.getBans().remove(uuid);
                        LobbyClicker.getDatabase().deleteBanThreaded(data.getIdentifier(), uuid);
                        RedisSyncHandler.publishBanRemove(data.getIdentifier(), uuid);
                        p.sendMessage(ChatColor.YELLOW + "Unbanned player.");
                        new BanListGui(p, data, page).open();
                    },
                    "", ChatColor.YELLOW + "Click to unban");
            addItem(slot, icon);
        });

        addPaginationArrows(banList, newPage -> new BanListGui(player, data, newPage).open());
    }
}
