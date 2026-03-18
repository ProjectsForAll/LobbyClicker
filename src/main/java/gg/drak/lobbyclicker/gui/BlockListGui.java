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

public class BlockListGui extends PaginationMonitor {
    private final PlayerData data;

    public BlockListGui(Player player, PlayerData data, int page) {
        super(player, "block-list", MonitorStyle.title(ChatColor.DARK_RED, "Blocked Players"), page);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        Player player = (Player) event.getPlayer();

        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new SocialMainGui(p, data).open());

        List<String> blockList = new ArrayList<>(data.getBlocks());

        populatePagedContent(blockList, (uuid, slot) -> {
            String name = uuid.substring(0, 8);
            try { String n = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName(); if (n != null) name = n; } catch (Exception ignored) {}

            Icon icon = playerHeadIcon(uuid,
                    ChatColor.DARK_RED + name,
                    p -> {
                        data.getBlocks().remove(uuid);
                        data.getBans().remove(uuid);
                        LobbyClicker.getDatabase().deleteBlockThreaded(data.getIdentifier(), uuid);
                        LobbyClicker.getDatabase().deleteBanThreaded(data.getIdentifier(), uuid);
                        RedisSyncHandler.publishBlockRemove(data.getIdentifier(), uuid);
                        RedisSyncHandler.publishBanRemove(data.getIdentifier(), uuid);
                        p.sendMessage(ChatColor.YELLOW + "Unblocked player.");
                        new BlockListGui(p, data, page).open();
                    },
                    "", ChatColor.YELLOW + "Click to unblock");
            addItem(slot, icon);
        });

        addPaginationArrows(blockList, newPage -> new BlockListGui(player, data, newPage).open());
    }
}
