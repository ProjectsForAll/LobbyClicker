package gg.drak.lobbyclicker.quests;

import gg.drak.lobbyclicker.realm.RealmProfile;
import org.bukkit.Material;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static manager that generates 3 daily quests per day based on the date.
 * Uses a seeded Random from the date so all players get the same quests.
 * Dailies reset at midnight (server time) automatically since they are date-seeded.
 * All state is in-memory only and naturally resets on server restart or date change.
 */
public class DailyQuestManager {
    private static LocalDate currentDate;
    private static final List<DailyQuest> todayQuests = new ArrayList<>();

    // Per-player: tracks which of today's dailies have been claimed
    // Key: playerUuid, Value: set of daily quest IDs claimed today
    private static final ConcurrentHashMap<String, Set<String>> claimedToday = new ConcurrentHashMap<>();

    // Per-player: daily progress snapshots (baseline values when daily started)
    // Key: playerUuid, Value: map of QuestType -> baseline value
    private static final ConcurrentHashMap<String, Map<QuestType, Long>> dailyBaselines = new ConcurrentHashMap<>();

    /**
     * Check if the date has changed and regenerate dailies if needed.
     * Should be called when opening the QuestsGui.
     */
    public static void checkAndRefresh() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            currentDate = today;
            todayQuests.clear();
            claimedToday.clear();
            dailyBaselines.clear();
            generateDailies(today);
        }
    }

    /**
     * Get today's 3 daily quests (unmodifiable).
     */
    public static List<DailyQuest> getTodayQuests() {
        checkAndRefresh();
        return Collections.unmodifiableList(todayQuests);
    }

    /**
     * Whether a player has claimed a specific daily quest today.
     */
    public static boolean hasClaimed(String uuid, String questId) {
        Set<String> claimed = claimedToday.get(uuid);
        return claimed != null && claimed.contains(questId);
    }

    /**
     * Mark a daily quest as claimed for a player.
     */
    public static void claim(String uuid, String questId) {
        claimedToday.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(questId);
    }

    /**
     * Get the daily progress for a player (current value minus baseline).
     * If no baseline has been set, returns the current value (counts from 0).
     */
    public static long getDailyProgress(String uuid, QuestType type, long currentValue) {
        Map<QuestType, Long> baselines = dailyBaselines.get(uuid);
        if (baselines == null) return currentValue;
        Long baseline = baselines.get(type);
        if (baseline == null) return currentValue;
        return Math.max(0, currentValue - baseline);
    }

    /**
     * Set the baseline snapshot when a player first opens the quest GUI today.
     * Only sets if not already present for this player today.
     */
    public static void ensureBaseline(String uuid, RealmProfile profile) {
        dailyBaselines.computeIfAbsent(uuid, k -> {
            Map<QuestType, Long> map = new EnumMap<>(QuestType.class);
            map.put(QuestType.CLICKS, profile.getTimesClicked());
            map.put(QuestType.LIFETIME_COOKIES, profile.getLifetimeCookiesEarned().longValue());
            map.put(QuestType.GOLDEN_COOKIES, profile.getGoldenCookiesCollected());
            // CPS and entropy don't need baselines (they're current-value checks)
            return map;
        });
    }

    private static void generateDailies(LocalDate date) {
        Random rng = new Random(date.toEpochDay());

        // Template pools for daily quests
        DailyTemplate[] templates = {
            new DailyTemplate("Daily Clicker", "Click %d times today.", QuestType.CLICKS,
                new long[]{100, 250, 500, 1000, 2500}),
            new DailyTemplate("Cookie Earner", "Earn %d cookies today.", QuestType.LIFETIME_COOKIES,
                new long[]{10000, 50000, 100000, 500000, 1000000}),
            new DailyTemplate("Golden Hunter", "Collect %d golden cookies today.", QuestType.GOLDEN_COOKIES,
                new long[]{3, 5, 10, 15, 25}),
        };

        // Pick 3 different templates (shuffle then take first 3)
        List<DailyTemplate> pool = new ArrayList<>(Arrays.asList(templates));
        Collections.shuffle(pool, rng);

        String dateStr = date.toString().replace("-", "");
        for (int i = 0; i < Math.min(3, pool.size()); i++) {
            DailyTemplate t = pool.get(i);
            long req = t.thresholds[rng.nextInt(t.thresholds.length)];
            String id = t.type.name() + "_" + req + "_" + dateStr;
            String desc = String.format(t.descPattern, req);

            // Reward: scales based on type
            BigDecimal reward;
            switch (t.type) {
                case CLICKS:
                    reward = BigDecimal.valueOf(req * 10);
                    break;
                case GOLDEN_COOKIES:
                    reward = BigDecimal.valueOf(req * 5000);
                    break;
                default:
                    reward = BigDecimal.valueOf(req).divide(BigDecimal.TEN, 0, RoundingMode.FLOOR);
                    break;
            }
            // Minimum reward of 1000
            if (reward.compareTo(BigDecimal.valueOf(1000)) < 0) {
                reward = BigDecimal.valueOf(1000);
            }

            todayQuests.add(new DailyQuest(id, t.name, desc, t.material, reward, t.type, req));
        }
    }

    private static class DailyTemplate {
        final String name;
        final String descPattern;
        final Material material;
        final QuestType type;
        final long[] thresholds;

        DailyTemplate(String name, String descPattern, QuestType type, long[] thresholds) {
            this.name = name;
            this.descPattern = descPattern;
            this.type = type;
            this.thresholds = thresholds;
            // Material based on type
            switch (type) {
                case CLICKS:
                    this.material = Material.COOKIE;
                    break;
                case LIFETIME_COOKIES:
                    this.material = Material.SUGAR;
                    break;
                case GOLDEN_COOKIES:
                    this.material = Material.GOLD_NUGGET;
                    break;
                default:
                    this.material = Material.PAPER;
                    break;
            }
        }
    }
}
