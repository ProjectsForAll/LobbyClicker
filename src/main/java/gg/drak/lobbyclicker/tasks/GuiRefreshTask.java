package gg.drak.lobbyclicker.tasks;

import gg.drak.lobbyclicker.gui.ClickerGui;
import gg.drak.lobbyclicker.gui.LeaderboardGui;
import gg.drak.lobbyclicker.gui.UpgradeGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuiRefreshTask extends BukkitRunnable {
    @Override
    public void run() {
        refreshMap(ClickerGui.getOpenGuis());
        refreshMap(UpgradeGui.getOpenGuis());
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
            if (gui instanceof ClickerGui) {
                ClickerGui g = (ClickerGui) gui;
                if (player.getOpenInventory().getTopInventory().equals(g.getInventory())) {
                    g.refreshDisplay();
                } else {
                    map.remove(uuid);
                }
            } else if (gui instanceof UpgradeGui) {
                UpgradeGui g = (UpgradeGui) gui;
                if (player.getOpenInventory().getTopInventory().equals(g.getInventory())) {
                    g.refreshDisplay();
                } else {
                    map.remove(uuid);
                }
            }
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
            if (player.getOpenInventory().getTopInventory().equals(gui.getInventory())) {
                gui.refreshDisplay();
            } else {
                map.remove(uuid);
            }
        }
    }
}
