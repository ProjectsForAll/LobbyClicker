package gg.drak.lobbyclicker.tasks;

import gg.drak.lobbyclicker.gui.ClickerGui;
import gg.drak.lobbyclicker.gui.UpgradeGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

public class GuiRefreshTask extends BukkitRunnable {
    @Override
    public void run() {
        // Refresh ClickerGui instances
        for (Map.Entry<UUID, ClickerGui> entry : ClickerGui.getOpenGuis().entrySet()) {
            UUID uuid = entry.getKey();
            ClickerGui gui = entry.getValue();

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                ClickerGui.unregisterGui(uuid);
                continue;
            }

            if (player.getOpenInventory().getTopInventory().equals(gui.getInventory())) {
                gui.refreshDisplay();
            } else {
                ClickerGui.unregisterGui(uuid);
            }
        }

        // Refresh UpgradeGui instances
        for (Map.Entry<UUID, UpgradeGui> entry : UpgradeGui.getOpenGuis().entrySet()) {
            UUID uuid = entry.getKey();
            UpgradeGui gui = entry.getValue();

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                UpgradeGui.unregisterGui(uuid);
                continue;
            }

            if (player.getOpenInventory().getTopInventory().equals(gui.getInventory())) {
                gui.refreshDisplay();
            } else {
                UpgradeGui.unregisterGui(uuid);
            }
        }
    }
}
