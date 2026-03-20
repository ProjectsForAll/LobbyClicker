package gg.drak.lobbyclicker.quests;

import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.upgrades.UpgradeType;
import lombok.Getter;
import org.bukkit.Material;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * All quests in the game. Each quest has a type, a threshold requirement,
 * and a cookie reward. Quests are checked against the player's active RealmProfile.
 */
@Getter
public enum Quest {

    // === Clicking quests === (CPC bonuses)
    FIRST_CLICK("First Click", "Click the cookie once.", Material.COOKIE, "10", QuestType.CLICKS, 1, QuestEffect.CPC_PERCENT, 1),
    HUNDRED_CLICKS("Hundred Clicks", "Click the cookie 100 times.", Material.COOKIE, "500", QuestType.CLICKS, 100, QuestEffect.CPC_PERCENT, 2),
    THOUSAND_CLICKS("Thousand Clicks", "Click the cookie 1,000 times.", Material.COOKIE, "5000", QuestType.CLICKS, 1000, QuestEffect.CPC_PERCENT, 3),
    CLICK_MASTER("Click Master", "Click the cookie 10,000 times.", Material.COOKIE, "50000", QuestType.CLICKS, 10000, QuestEffect.CPC_PERCENT, 5),
    CLICK_LEGEND("Click Legend", "Click the cookie 100,000 times.", Material.COOKIE, "500000", QuestType.CLICKS, 100000, QuestEffect.CPC_PERCENT, 10),

    // === Cookie milestones (lifetime) === (CPS bonuses)
    COOKIE_BEGINNER("Cookie Beginner", "Earn 1,000 cookies total.", Material.SUGAR, "100", QuestType.LIFETIME_COOKIES, 1000, QuestEffect.CPS_PERCENT, 1),
    COOKIE_COLLECTOR("Cookie Collector", "Earn 100,000 cookies total.", Material.SUGAR, "10000", QuestType.LIFETIME_COOKIES, 100000, QuestEffect.CPS_PERCENT, 2),
    COOKIE_MILLIONAIRE("Cookie Millionaire", "Earn 1,000,000 cookies total.", Material.SUGAR_CANE, "100000", QuestType.LIFETIME_COOKIES, 1000000, QuestEffect.CPS_PERCENT, 5),
    COOKIE_BILLIONAIRE("Cookie Billionaire", "Earn 1,000,000,000 cookies total.", Material.SUGAR_CANE, "100000000", QuestType.LIFETIME_COOKIES, 1000000000, QuestEffect.CPS_PERCENT, 10),
    COOKIE_TRILLIONAIRE("Cookie Trillionaire", "Earn 1,000,000,000,000 cookies total.", Material.CAKE, "100000000000", QuestType.LIFETIME_COOKIES, 1000000000000L, QuestEffect.CPS_PERCENT, 20),

    // === Upgrade milestones (total helpers) === (CPS bonuses)
    FIRST_HELPER("First Helper", "Buy 1 helper of any type.", Material.ARROW, "50", QuestType.TOTAL_HELPERS, 1, QuestEffect.CPS_PERCENT, 1),
    HELPER_ARMY("Helper Army", "Own 10 total helpers.", Material.ARROW, "1000", QuestType.TOTAL_HELPERS, 10, QuestEffect.CPS_PERCENT, 3),
    HELPER_EMPIRE("Helper Empire", "Own 50 total helpers.", Material.SPECTRAL_ARROW, "10000", QuestType.TOTAL_HELPERS, 50, QuestEffect.CPS_PERCENT, 5),
    HELPER_DYNASTY("Helper Dynasty", "Own 100 total helpers.", Material.SPECTRAL_ARROW, "100000", QuestType.TOTAL_HELPERS, 100, QuestEffect.CPS_PERCENT, 10),

    // === Clicker upgrade milestones === (CPC bonuses)
    UPGRADE_CONNOISSEUR("Upgrade Connoisseur", "Purchase 1 clicker upgrade.", Material.DIAMOND, "5000", QuestType.TOTAL_CLICKER_UPGRADES, 1, QuestEffect.CPC_PERCENT, 5),
    FULLY_UPGRADED("Fully Upgraded", "Purchase 5 clicker upgrades.", Material.DIAMOND_BLOCK, "50000", QuestType.TOTAL_CLICKER_UPGRADES, 5, QuestEffect.CPC_PERCENT, 10),

    // === Prestige === (CPS + CPC bonuses)
    FIRST_PRESTIGE("First Prestige", "Prestige for the first time.", Material.BEACON, "10000", QuestType.PRESTIGE, 1, QuestEffect.CPS_PERCENT, 5),
    PRESTIGE_MASTER("Prestige Master", "Reach prestige level 5.", Material.BEACON, "1000000", QuestType.PRESTIGE, 5, QuestEffect.CPC_PERCENT, 15),
    PRESTIGE_LEGEND("Prestige Legend", "Reach prestige level 10.", Material.BEACON, "100000000", QuestType.PRESTIGE, 10, QuestEffect.CPS_PERCENT, 25),

    // === Golden cookies === (golden frequency bonuses)
    LUCKY_FIND("Lucky Find", "Collect 1 golden cookie.", Material.GOLD_NUGGET, "500", QuestType.GOLDEN_COOKIES, 1, QuestEffect.GOLDEN_FREQ_PERCENT, 5),
    GOLDEN_HOARDER("Golden Hoarder", "Collect 10 golden cookies.", Material.GOLD_INGOT, "5000", QuestType.GOLDEN_COOKIES, 10, QuestEffect.GOLDEN_FREQ_PERCENT, 10),
    GOLDEN_LEGEND("Golden Legend", "Collect 50 golden cookies.", Material.GOLD_BLOCK, "50000", QuestType.GOLDEN_COOKIES, 50, QuestEffect.GOLDEN_FREQ_PERCENT, 20),

    // === CPS milestones === (CPS bonuses)
    PASSIVE_INCOME("Passive Income", "Reach 10 cookies per second.", Material.HOPPER, "500", QuestType.CPS, 10, QuestEffect.CPS_PERCENT, 2),
    COOKIE_FACTORY("Cookie Factory", "Reach 1,000 cookies per second.", Material.FURNACE, "50000", QuestType.CPS, 1000, QuestEffect.CPS_PERCENT, 5),
    COOKIE_CORPORATION("Cookie Corporation", "Reach 1,000,000 cookies per second.", Material.PISTON, "5000000", QuestType.CPS, 1000000, QuestEffect.CPS_PERCENT, 15),

    // === Entropy === (CPC bonuses)
    ENTROPY_RISING("Entropy Rising", "Reach 1,000 clicker entropy.", Material.ENDER_PEARL, "1000", QuestType.ENTROPY, 1000, QuestEffect.CPC_PERCENT, 3),
    ENTROPY_MASTER("Entropy Master", "Reach 1,000,000 clicker entropy.", Material.ENDER_EYE, "1000000", QuestType.ENTROPY, 1000000, QuestEffect.CPC_PERCENT, 10),

    // === Late-game clicking ===
    CLICK_GOD("Click God", "Click the cookie 1,000,000 times.", Material.COOKIE, "5000000", QuestType.CLICKS, 1000000, QuestEffect.CPC_PERCENT, 15),
    CLICK_TRANSCENDENT("Click Transcendent", "Click the cookie 10,000,000 times.", Material.COOKIE, "50000000", QuestType.CLICKS, 10000000, QuestEffect.CPC_PERCENT, 25),

    // === Late-game cookie milestones ===
    COOKIE_QUADRILLIONAIRE("Cookie Quadrillionaire", "Earn 1Qa cookies total.", Material.CAKE, "1000000000000", QuestType.LIFETIME_COOKIES, 1000000000000000L, QuestEffect.CPS_PERCENT, 30),
    COOKIE_QUINTILLIONAIRE("Cookie Quintillionaire", "Earn 1Qi cookies total.", Material.CAKE, "100000000000000", QuestType.LIFETIME_COOKIES, 1000000000000000000L, QuestEffect.CPS_PERCENT, 50),

    // === Late-game helpers ===
    HELPER_LEGION("Helper Legion", "Own 500 total helpers.", Material.TIPPED_ARROW, "500000", QuestType.TOTAL_HELPERS, 500, QuestEffect.CPS_PERCENT, 15),
    HELPER_INFINITE("Helper Infinite", "Own 2,000 total helpers.", Material.TIPPED_ARROW, "5000000", QuestType.TOTAL_HELPERS, 2000, QuestEffect.CPS_PERCENT, 25),
    HELPER_TRANSCENDENT("Helper Transcendent", "Own 10,000 total helpers.", Material.TIPPED_ARROW, "50000000", QuestType.TOTAL_HELPERS, 10000, QuestEffect.CPS_PERCENT, 40),

    // === Late-game clicker upgrades ===
    UPGRADE_MASTER("Upgrade Master", "Purchase 10 clicker upgrades.", Material.DIAMOND_BLOCK, "500000", QuestType.TOTAL_CLICKER_UPGRADES, 10, QuestEffect.CPC_PERCENT, 15),
    UPGRADE_COLLECTOR("Upgrade Collector", "Purchase 20 clicker upgrades.", Material.DIAMOND_BLOCK, "5000000", QuestType.TOTAL_CLICKER_UPGRADES, 20, QuestEffect.CPC_PERCENT, 25),
    UPGRADE_COMPLETIONIST("Upgrade Completionist", "Purchase all clicker upgrades.", Material.DIAMOND_BLOCK, "50000000", QuestType.TOTAL_CLICKER_UPGRADES, 34, QuestEffect.CPC_PERCENT, 50),

    // === Mid-prestige milestones ===
    PRESTIGE_VETERAN("Prestige Veteran", "Reach prestige level 20.", Material.BEACON, "1000000000", QuestType.PRESTIGE, 20, QuestEffect.CPS_PERCENT, 30),
    PRESTIGE_CHAMPION("Prestige Champion", "Reach prestige level 35.", Material.BEACON, "10000000000", QuestType.PRESTIGE, 35, QuestEffect.CPC_PERCENT, 40),
    PRESTIGE_TITAN("Prestige Titan", "Reach prestige level 50.", Material.BEACON, "100000000000", QuestType.PRESTIGE, 50, QuestEffect.CPS_PERCENT, 50),
    PRESTIGE_IMMORTAL("Prestige Immortal", "Reach prestige level 75.", Material.BEACON, "1000000000000", QuestType.PRESTIGE, 75, QuestEffect.CPC_PERCENT, 75),
    PRESTIGE_TRANSCENDENT("Prestige Transcendent", "Reach prestige level 100.", Material.BEACON, "10000000000000", QuestType.PRESTIGE, 100, QuestEffect.CPS_PERCENT, 100),

    // === Late-game golden cookies ===
    GOLDEN_MASTER("Golden Master", "Collect 200 golden cookies.", Material.GOLD_BLOCK, "500000", QuestType.GOLDEN_COOKIES, 200, QuestEffect.GOLDEN_FREQ_PERCENT, 30),
    GOLDEN_TRANSCENDENT("Golden Transcendent", "Collect 1,000 golden cookies.", Material.GOLD_BLOCK, "5000000", QuestType.GOLDEN_COOKIES, 1000, QuestEffect.GOLDEN_FREQ_PERCENT, 50),

    // === Late-game CPS ===
    COOKIE_EMPIRE("Cookie Empire", "Reach 1B cookies per second.", Material.PISTON, "50000000", QuestType.CPS, 1000000000, QuestEffect.CPS_PERCENT, 25),
    COOKIE_DYNASTY("Cookie Dynasty", "Reach 1T cookies per second.", Material.PISTON, "5000000000", QuestType.CPS, 1000000000000L, QuestEffect.CPS_PERCENT, 50),
    COOKIE_UNIVERSE("Cookie Universe", "Reach 1Qa cookies per second.", Material.PISTON, "500000000000", QuestType.CPS, 1000000000000000L, QuestEffect.CPS_PERCENT, 100),

    // === Late-game entropy ===
    ENTROPY_GOD("Entropy God", "Reach 1B clicker entropy.", Material.ENDER_EYE, "10000000", QuestType.ENTROPY, 1000000000, QuestEffect.CPC_PERCENT, 20),
    ENTROPY_TRANSCENDENT("Entropy Transcendent", "Reach 1T clicker entropy.", Material.ENDER_EYE, "1000000000", QuestType.ENTROPY, 1000000000000L, QuestEffect.CPC_PERCENT, 50)
    ;

    private final String displayName;
    private final String description;
    private final Material material;
    private final BigDecimal reward;
    private final QuestType type;
    private final long requirement;
    private final QuestEffect effect;
    private final int effectPercent; // permanent bonus percent granted to the realm

    Quest(String displayName, String description, Material material, String reward,
          QuestType type, long requirement, QuestEffect effect, int effectPercent) {
        this.displayName = displayName;
        this.description = description;
        this.material = material;
        this.reward = new BigDecimal(reward);
        this.type = type;
        this.requirement = requirement;
        this.effect = effect;
        this.effectPercent = effectPercent;
    }

    /**
     * Describe the permanent effect this quest grants.
     */
    public String getEffectDescription() {
        String label;
        switch (effect) {
            case CPS_PERCENT: label = "CPS"; break;
            case CPC_PERCENT: label = "CPC"; break;
            case GOLDEN_FREQ_PERCENT: label = "Golden Cookie frequency"; break;
            default: label = "?";
        }
        return "+" + effectPercent + "% " + label;
    }

    /**
     * Get the current progress value for this quest from a profile.
     */
    public long getProgress(RealmProfile profile) {
        switch (type) {
            case CLICKS:
                return profile.getTimesClicked();
            case LIFETIME_COOKIES:
                return profile.getLifetimeCookiesEarned().longValue();
            case TOTAL_HELPERS: {
                int total = 0;
                for (UpgradeType ut : UpgradeType.values()) {
                    total += profile.getUpgradeCount(ut);
                }
                return total;
            }
            case TOTAL_CLICKER_UPGRADES:
                return profile.getPurchasedUpgrades().size();
            case PRESTIGE:
                return profile.getPrestigeLevel();
            case CPS:
                return profile.getCps().longValue();
            case ENTROPY:
                return profile.getClickerEntropy().longValue();
            case GOLDEN_COOKIES:
                return profile.getGoldenCookiesCollected();
            default:
                return 0;
        }
    }

    /**
     * Whether the quest's requirement is met (regardless of whether it has been claimed).
     */
    public boolean isCompleted(RealmProfile profile) {
        return getProgress(profile) >= requirement;
    }

    // --- Serialization (comma-separated enum names, same pattern as ClickerUpgrade) ---

    public static Set<Quest> deserialize(String data) {
        Set<Quest> set = EnumSet.noneOf(Quest.class);
        if (data != null && !data.isEmpty()) {
            for (String name : data.split(",")) {
                try {
                    set.add(Quest.valueOf(name.trim()));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return set;
    }

    public static String serialize(Set<Quest> quests) {
        if (quests == null || quests.isEmpty()) return "";
        return quests.stream().map(Enum::name).collect(Collectors.joining(","));
    }
}
