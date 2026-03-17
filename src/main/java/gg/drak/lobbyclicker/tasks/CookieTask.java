package gg.drak.lobbyclicker.tasks;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import org.bukkit.scheduler.BukkitRunnable;

public class CookieTask extends BukkitRunnable {
    private int tickCounter = 0;
    private static final int AUTO_SAVE_INTERVAL = 300; // 5 minutes in seconds

    @Override
    public void run() {
        tickCounter++;

        for (PlayerData data : PlayerManager.getLoadedPlayers()) {
            if (!data.isOnline()) continue;
            if (!data.isFullyLoaded()) continue;

            double cps = data.getCps();
            if (cps > 0) {
                data.addCookies(cps);
            }
        }

        if (tickCounter >= AUTO_SAVE_INTERVAL) {
            tickCounter = 0;
            for (PlayerData data : PlayerManager.getLoadedPlayers()) {
                if (data.isFullyLoaded()) {
                    data.save(true);
                }
            }
        }
    }
}
