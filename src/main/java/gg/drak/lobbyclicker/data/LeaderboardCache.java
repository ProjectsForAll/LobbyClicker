package gg.drak.lobbyclicker.data;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.math.CookieMath;
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
    private static final long UPDATE_INTERVAL_MS = 30_000; // 30 seconds
    private static boolean updating = false;

    /**
     * Get the cached leaderboard entries. Triggers an async refresh if stale.
     */
    public static List<LeaderboardEntry> getLeaderboard() {
        if (System.currentTimeMillis() - lastUpdate > UPDATE_INTERVAL_MS && !updating) {
            refreshAsync();
        }
        return Collections.unmodifiableList(entries);
    }

    /**
     * Force a refresh of the leaderboard data from the database.
     */
    public static void refreshAsync() {
        if (updating) return;
        updating = true;

        // Pull all profiles from loaded players first
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
                    profile.getPrestigeLevel(),
                    profile.getCps()
            ));
        }

        // Also pull from database for offline players
        if (LobbyClicker.getDatabase() != null) {
            LobbyClicker.getDatabase().pullLeaderboardFromDb().thenAccept(dbEntries -> {
                // Merge: add DB entries that aren't already in the loaded set
                for (LeaderboardEntry dbEntry : dbEntries) {
                    boolean exists = newEntries.stream().anyMatch(e ->
                            e.getProfileId().equals(dbEntry.getProfileId()));
                    if (!exists) {
                        newEntries.add(dbEntry);
                    }
                }

                // Sort by total cookies earned descending
                newEntries.sort((a, b) -> b.getTotalCookiesEarned().compareTo(a.getTotalCookiesEarned()));

                entries.clear();
                entries.addAll(newEntries);
                lastUpdate = System.currentTimeMillis();
                updating = false;
            }).exceptionally(ex -> {
                updating = false;
                return null;
            });
        } else {
            newEntries.sort((a, b) -> b.getTotalCookiesEarned().compareTo(a.getTotalCookiesEarned()));
            entries.clear();
            entries.addAll(newEntries);
            lastUpdate = System.currentTimeMillis();
            updating = false;
        }
    }

    @Getter
    public static class LeaderboardEntry {
        private final String playerUuid;
        private final String playerName;
        private final String profileId;
        private final String profileName;
        private final BigDecimal cookies;
        private final BigDecimal totalCookiesEarned;
        private final int prestigeLevel;
        private final BigDecimal cps;

        public LeaderboardEntry(String playerUuid, String playerName, String profileId, String profileName,
                                BigDecimal cookies, BigDecimal totalCookiesEarned, int prestigeLevel, BigDecimal cps) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.profileId = profileId;
            this.profileName = profileName;
            this.cookies = cookies;
            this.totalCookiesEarned = totalCookiesEarned;
            this.prestigeLevel = prestigeLevel;
            this.cps = cps;
        }
    }
}
