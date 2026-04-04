package gg.drak.lobbyclicker.data;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.math.CookieMath;
import org.bukkit.Bukkit;
import gg.drak.lobbyclicker.realm.ProfileManager;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.utils.FormatUtils;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Caches leaderboard data from the database + loaded players.
 * Updates every 30 seconds automatically, or on demand.
 */
public class LeaderboardCache {
    @Getter
    private static final List<LeaderboardEntry> entries = new CopyOnWriteArrayList<>();
    private static long lastUpdate = 0;
    private static long lastDbPull = 0;
    private static final long UPDATE_INTERVAL_MS = 500; // 0.5 seconds (fast in-memory refresh)
    private static final long DB_PULL_INTERVAL_MS = 30_000; // 30 seconds (slow DB pull for offline players)
    private static boolean updating = false;
    private static boolean dbPulling = false;
    private static final List<LeaderboardEntry> dbCache = new CopyOnWriteArrayList<>();

    /**
     * Get the cached leaderboard entries. Triggers a fast refresh if stale.
     */
    public static List<LeaderboardEntry> getLeaderboard() {
        if (System.currentTimeMillis() - lastUpdate > UPDATE_INTERVAL_MS && !updating) {
            refreshFromMemory();
        }
        return Collections.unmodifiableList(entries);
    }

    /**
     * Force a full refresh including DB pull.
     */
    public static void refreshAsync() {
        refreshDbCache();
        refreshFromMemory();
    }

    /**
     * Fast refresh from loaded in-memory profiles + cached DB entries.
     * Called every 500ms when the leaderboard GUI is viewed.
     */
    public static void refreshFromMemory() {
        if (updating) return;
        updating = true;

        try {
            List<LeaderboardEntry> newEntries = new ArrayList<>();
            for (RealmProfile profile : ProfileManager.getAllLoadedProfiles()) {
                String ownerName = PlayerManager.getPlayer(profile.getOwnerUuid())
                        .map(PlayerData::getName).orElse(null);
                if (ownerName == null) {
                    try {
                        ownerName = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(profile.getOwnerUuid())).getName();
                    } catch (Exception ignored) {}
                }
                if (ownerName == null) ownerName = profile.getOwnerUuid().substring(0, 8);

                newEntries.add(new LeaderboardEntry(
                        profile.getOwnerUuid(),
                        ownerName,
                        profile.getProfileId(),
                        profile.getProfileName(),
                        profile.getCookies(),
                        profile.getTotalCookiesEarned(),
                        profile.getLifetimeCookiesEarned(),
                        profile.getPrestigeLevel(),
                        profile.getCps()
                ));
            }

            // Merge cached DB entries (offline players)
            for (LeaderboardEntry dbEntry : dbCache) {
                boolean exists = newEntries.stream().anyMatch(e ->
                        e.getProfileId().equals(dbEntry.getProfileId()));
                if (!exists) {
                    newEntries.add(dbEntry);
                }
            }

            newEntries.sort((a, b) -> b.getLifetimeCookiesEarned().compareTo(a.getLifetimeCookiesEarned()));

            entries.clear();
            entries.addAll(newEntries);
            lastUpdate = System.currentTimeMillis();

            // Trigger a DB pull if stale
            if (System.currentTimeMillis() - lastDbPull > DB_PULL_INTERVAL_MS && !dbPulling) {
                refreshDbCache();
            }
        } finally {
            updating = false;
        }
    }

    /**
     * Async pull from DB to refresh the offline player cache.
     */
    private static void refreshDbCache() {
        if (dbPulling || LobbyClicker.getDatabase() == null) return;
        dbPulling = true;
        LobbyClicker.getDatabase().pullLeaderboardFromDb().thenAccept(dbEntries ->
                Bukkit.getScheduler().runTask(LobbyClicker.getInstance(), () -> {
                    dbCache.clear();
                    dbCache.addAll(dbEntries);
                    lastDbPull = System.currentTimeMillis();
                    dbPulling = false;
                })).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(LobbyClicker.getInstance(), () -> dbPulling = false);
            return null;
        });
    }

    @Getter
    public static class LeaderboardEntry {
        private final String playerUuid;
        private final String playerName;
        private final String profileId;
        private final String profileName;
        private final BigDecimal cookies;
        private final BigDecimal totalCookiesEarned;
        private final BigDecimal lifetimeCookiesEarned;
        private final int prestigeLevel;
        private final BigDecimal cps;

        public LeaderboardEntry(String playerUuid, String playerName, String profileId, String profileName,
                                BigDecimal cookies, BigDecimal totalCookiesEarned, BigDecimal lifetimeCookiesEarned,
                                int prestigeLevel, BigDecimal cps) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.profileId = profileId;
            this.profileName = profileName;
            this.cookies = cookies;
            this.totalCookiesEarned = totalCookiesEarned;
            this.lifetimeCookiesEarned = lifetimeCookiesEarned;
            this.prestigeLevel = prestigeLevel;
            this.cps = cps;
        }
    }
}
