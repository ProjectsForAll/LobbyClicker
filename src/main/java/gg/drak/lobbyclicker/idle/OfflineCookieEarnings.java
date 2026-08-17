package gg.drak.lobbyclicker.idle;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.utils.FormatUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

/**
 * Credits passive CPS for time spent offline when the player opens their own clicker GUI
 * after being away at least {@link #MIN_AWAY_MS}.
 */
public final class OfflineCookieEarnings {

    private static final long MIN_AWAY_MS = 6L * 60 * 60 * 1000;
    /** Avoid unbounded accrual if someone returns after a very long absence. */
    private static final long MAX_ACCRUAL_SECONDS = 30L * 24 * 60 * 60;

    private OfflineCookieEarnings() {}

    public static void applyAndNotify(Player player, PlayerData data) {
        if (!data.isFullyLoaded() || !data.hasActiveProfile()) return;

        long last = data.getLastLogoutEpochMs();
        if (last <= 0) return;

        long away = System.currentTimeMillis() - last;
        if (away < MIN_AWAY_MS) return;

        long secondsOffline = Math.min(away / 1000, MAX_ACCRUAL_SECONDS);

        BigDecimal cps = data.getCps();
        data.setLastLogoutEpochMs(0);

        if (cps.signum() <= 0) {
            data.save(true);
            return;
        }

        BigDecimal gain = cps.multiply(BigDecimal.valueOf(secondsOffline));
        if (gain.signum() <= 0) {
            data.save(true);
            return;
        }

        data.addCookies(gain);
        gg.drak.lobbyclicker.realm.RealmProfile profile = data.getActiveProfile();
        if (profile != null) {
            BigDecimal raw = profile.getRawCps();
            if (raw.signum() > 0) {
                for (gg.drak.lobbyclicker.upgrades.UpgradeType type : gg.drak.lobbyclicker.upgrades.UpgradeType.values()) {
                    BigDecimal buildingRaw = profile.getBuildingRawCps(type);
                    if (buildingRaw.signum() <= 0) continue;
                    profile.addCookiesFromBuilding(type, gain.multiply(buildingRaw)
                            .divide(raw, java.math.RoundingMode.HALF_UP));
                }
            }
            gg.drak.lobbyclicker.achievements.AchievementManager.check(data);
        }
        data.save(true);

        player.sendMessage(ChatColor.GOLD + "While you were offline, you gained "
                + ChatColor.WHITE + FormatUtils.format(gain) + ChatColor.GOLD + " cookies!");
    }
}
