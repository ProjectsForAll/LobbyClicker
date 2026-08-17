package gg.drak.lobbyclicker.tasks;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.gui.ClickerGui;
import gg.drak.lobbyclicker.gui.ClickerUpgradeGui;
import gg.drak.lobbyclicker.gui.LeaderboardGui;
import gg.drak.lobbyclicker.gui.UpgradeGui;
import gg.drak.lobbyclicker.utils.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuiRefreshTask {
    private FoliaScheduler.PluginTask task;

    public void start(LobbyClicker plugin) {
        task = FoliaScheduler.runGlobalTimer(plugin, this::run, 20L, 20L);
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void run() {
        refreshMap(ClickerGui.getOpenGuis());
        refreshMap(UpgradeGui.getOpenGuis());
        refreshMap(ClickerUpgradeGui.getOpenGuis());
        refreshLeaderboard(LeaderboardGui.getOpenGuis());
    }

    private <T> void refreshMap(ConcurrentHashMap<UUID, T> map) {
        for (Map.Entry<UUID, T> entry : map.entrySet()) {
            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                map.remove(uuid);
                continue;
            }
            Object gui = entry.getValue();
            FoliaScheduler.runForEntity(player, LobbyClicker.getInstance(), () -> {
                if (gui instanceof ClickerGui g) {
                    if (player.getOpenInventory().getTopInventory().equals(g.getInventory())) {
                        g.refreshDisplay();
                    } else {
                        map.remove(uuid);
                    }
                } else if (gui instanceof UpgradeGui g) {
                    if (player.getOpenInventory().getTopInventory().equals(g.getInventory())) {
                        g.refreshDisplay();
                    } else {
                        map.remove(uuid);
                    }
                } else if (gui instanceof ClickerUpgradeGui g) {
                    if (player.getOpenInventory().getTopInventory().equals(g.getInventory())) {
                        g.refreshDisplay();
                    } else {
                        map.remove(uuid);
                    }
                }
            });
        }
    }

    private void refreshLeaderboard(ConcurrentHashMap<UUID, LeaderboardGui> map) {
        for (Map.Entry<UUID, LeaderboardGui> entry : map.entrySet()) {
            UUID uuid = entry.getKey();
            LeaderboardGui gui = entry.getValue();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                map.remove(uuid);
                continue;
            }
            FoliaScheduler.runForEntity(player, LobbyClicker.getInstance(), () -> {
                if (player.getOpenInventory().getTopInventory().equals(gui.getInventory())) {
                    gui.refreshDisplay();
                } else {
                    map.remove(uuid);
                }
            });
        }
    }
}
