package gg.drak.lobbyclicker.achievements;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.upgrades.UpgradeType;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class AchievementManager {
    private AchievementManager() {}

    public static int normalUnlocked(RealmProfile profile) {
        if (profile == null) return 0;
        int n = 0;
        for (Achievement a : profile.getCompletedAchievements()) {
            if (!a.isShadow()) n++;
        }
        return n;
    }

    public static BigDecimal milkMultiplier(RealmProfile profile) {
        return BigDecimal.ONE.add(BigDecimal.valueOf(normalUnlocked(profile)).multiply(new BigDecimal("0.04")));
    }

    public static void check(PlayerData data) {
        if (data == null) return;
        RealmProfile profile = data.getActiveProfile();
        if (profile == null) return;
        List<Achievement> unlocked = new ArrayList<>();
        for (Achievement a : AchievementCatalog.all()) {
            if (profile.hasCompletedAchievement(a)) continue;
            if (isMet(a, profile)) {
                profile.completeAchievement(a);
                unlocked.add(a);
            }
        }
        if (unlocked.isEmpty()) return;
        data.asPlayer().ifPresent(player -> notify(player, unlocked));
    }

    public static void unlockHereYouGo(PlayerData data) {
        if (data == null || data.getActiveProfile() == null) return;
        Achievement a = AchievementCatalog.byId("HERE_YOU_GO");
        if (a == null || data.getActiveProfile().hasCompletedAchievement(a)) return;
        data.getActiveProfile().completeAchievement(a);
        data.asPlayer().ifPresent(p -> notify(p, List.of(a)));
    }

    public static void markGoldenTiming(RealmProfile profile, boolean early, boolean late) {
        if (profile == null) return;
        if (early) profile.setGoldenEarly(true);
        if (late) profile.setGoldenLate(true);
    }

    private static void notify(Player player, List<Achievement> unlocked) {
        if (LobbyClicker.getMainConfig().isNotificationsDisabled()) return;
        for (Achievement a : unlocked) {
            String tag = a.isShadow() ? ChatColor.DARK_GRAY + "Shadow " : ChatColor.GOLD + "";
            player.sendMessage(tag + ChatColor.BOLD + "Achievement! " + ChatColor.YELLOW + a.getDisplayName());
            player.sendMessage(ChatColor.GRAY + a.getDescription());
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
        }
    }

    public static boolean isMet(Achievement a, RealmProfile p) {
        BigDecimal req = a.getRequirement();
        return switch (a.getType()) {
            case BAKED_THIS_ASCENSION -> p.getTotalCookiesEarned().compareTo(req) >= 0;
            case RAW_CPS -> p.getRawCps().compareTo(req) >= 0;
            case COOKIES_FROM_CLICKS -> p.getCookiesFromClicks().compareTo(req) >= 0;
            case BUILDING_COUNT -> a.getBuilding() != null
                    && BigDecimal.valueOf(p.getUpgradeCount(a.getBuilding())).compareTo(req) >= 0;
            case COOKIES_FROM_BUILDING -> a.getBuilding() != null
                    && p.getCookiesFromBuilding(a.getBuilding()).compareTo(req) >= 0;
            case TOTAL_BUILDINGS -> BigDecimal.valueOf(p.getTotalBuildingCount()).compareTo(req) >= 0;
            case UPGRADES_BOUGHT -> BigDecimal.valueOf(p.getPurchasedUpgrades().size()).compareTo(req) >= 0;
            case GOLDEN_CLICKED -> BigDecimal.valueOf(p.getGoldenCookiesCollected()).compareTo(req) >= 0;
            case GOLDEN_EARLY -> p.isGoldenEarly();
            case GOLDEN_LATE -> p.isGoldenLate();
            case PRESTIGE -> BigDecimal.valueOf(p.getPrestigeLevel()).compareTo(req) >= 0;
            case ONE_OF_EVERYTHING -> hasOneOfEach(p);
            case MATHEMATICIAN -> isMathematician(p);
            case BASE_10 -> isBase10(p);
            case CENTENNIAL -> hasAtLeastEach(p, a.getCentennialCount());
            case HARDCORE -> p.getPrestigeLevel() == 0
                    && p.getPurchasedUpgrades().isEmpty()
                    && p.getTotalCookiesEarned().compareTo(req) >= 0;
            case NEVERCLICK -> p.getPrestigeLevel() == 0
                    && p.getTimesClicked() <= 15
                    && p.getTotalCookiesEarned().compareTo(req) >= 0;
            case TRUE_NEVERCLICK -> p.getPrestigeLevel() == 0
                    && p.getTimesClicked() == 0
                    && p.getTotalCookiesEarned().compareTo(req) >= 0;
            case SPEED_BAKING -> p.getPrestigeLevel() == 0
                    && p.getTotalCookiesEarned().compareTo(new BigDecimal("1000000")) >= 0
                    && secondsSinceCreate(p) <= req.longValue();
            case HERE_YOU_GO -> p.hasCompletedAchievement(a);
        };
    }

    private static long secondsSinceCreate(RealmProfile p) {
        long created = p.getCreatedAtMillis();
        if (created <= 0) return Long.MAX_VALUE;
        return Math.max(0, (System.currentTimeMillis() - created) / 1000L);
    }

    private static boolean hasOneOfEach(RealmProfile p) {
        for (UpgradeType t : UpgradeType.values()) {
            if (p.getUpgradeCount(t) < 1) return false;
        }
        return true;
    }

    private static boolean hasAtLeastEach(RealmProfile p, int n) {
        for (UpgradeType t : UpgradeType.values()) {
            if (p.getUpgradeCount(t) < n) return false;
        }
        return true;
    }

    private static boolean isMathematician(RealmProfile p) {
        UpgradeType[] all = UpgradeType.values();
        int need = 1;
        for (int i = all.length - 1; i >= 0; i--) {
            if (p.getUpgradeCount(all[i]) < need) return false;
            need = Math.min(128, need * 2);
        }
        return true;
    }

    private static boolean isBase10(RealmProfile p) {
        UpgradeType[] all = UpgradeType.values();
        int need = 10;
        for (int i = all.length - 1; i >= 0; i--) {
            if (p.getUpgradeCount(all[i]) < need) return false;
            need += 10;
        }
        return true;
    }
}
