package gg.drak.lobbyclicker.realm;

import gg.drak.lobbyclicker.math.CookieMath;
import gg.drak.lobbyclicker.prestige.PrestigeManager;
import gg.drak.lobbyclicker.quests.Quest;
import gg.drak.lobbyclicker.quests.QuestEffect;
import gg.drak.lobbyclicker.settings.PlayerSettings;
import gg.drak.lobbyclicker.upgrades.ClickerUpgrade;
import gg.drak.lobbyclicker.upgrades.ClickerUpgradeEffect;
import gg.drak.lobbyclicker.upgrades.UpgradeType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a realm profile. Each player can own multiple profiles (up to a configurable limit).
 * All realm-specific data lives here: cookies, upgrades, prestige, aura, bans, roles.
 * Player-global data (friends, blocks, settings) stays on PlayerData.
 */
@Getter @Setter
public class RealmProfile {
    private String profileId;       // Unique UUID for this profile
    private String ownerUuid;       // UUID of the player who owns this profile
    private String profileName;     // User-defined name

    // Realm economy
    private BigDecimal cookies;
    private BigDecimal totalCookiesEarned;    // resets on prestige ("earned this prestige")
    private BigDecimal lifetimeCookiesEarned; // never resets (all-time total)
    private long timesClicked;       // combined (owner + others)
    private long ownerClicks;        // clicks by the realm owner
    private long otherClicks;        // clicks by visitors
    private EnumMap<UpgradeType, Integer> upgrades;
    private Set<ClickerUpgrade> purchasedUpgrades;
    private int prestigeLevel;
    private BigDecimal aura;
    private boolean realmPublic;

    // Quests
    private Set<Quest> completedQuests;
    private long goldenCookiesCollected;

    // Per-profile relationships
    private Set<String> bans;                        // UUIDs banned from THIS profile
    private Map<String, RealmRole> roles;             // playerUuid -> role in THIS profile

    // Milestone tracking (transient, not persisted)
    private int lastCurrentDigitCount;
    private int lastTotalDigitCount;
    private int lastEntropyDigitCount;
    private int lastEntropyLeadDigit;

    public RealmProfile(String profileId, String ownerUuid, String profileName) {
        this.profileId = profileId;
        this.ownerUuid = ownerUuid;
        this.profileName = profileName;
        this.cookies = BigDecimal.ZERO;
        this.totalCookiesEarned = BigDecimal.ZERO;
        this.lifetimeCookiesEarned = BigDecimal.ZERO;
        this.timesClicked = 0;
        this.ownerClicks = 0;
        this.otherClicks = 0;
        this.upgrades = new EnumMap<>(UpgradeType.class);
        for (UpgradeType type : UpgradeType.values()) {
            upgrades.put(type, 0);
        }
        this.purchasedUpgrades = EnumSet.noneOf(ClickerUpgrade.class);
        this.completedQuests = EnumSet.noneOf(Quest.class);
        this.goldenCookiesCollected = 0;
        this.prestigeLevel = 0;
        this.aura = BigDecimal.ZERO;
        this.realmPublic = false;
        this.bans = ConcurrentHashMap.newKeySet();
        this.roles = new ConcurrentHashMap<>();
        this.lastCurrentDigitCount = 0;
        this.lastTotalDigitCount = 0;
        this.lastEntropyDigitCount = 0;
        this.lastEntropyLeadDigit = 0;
    }

    // --- Cookie operations ---

    public void addCookies(BigDecimal amount) {
        this.cookies = this.cookies.add(amount);
        this.totalCookiesEarned = this.totalCookiesEarned.add(amount);
        this.lifetimeCookiesEarned = this.lifetimeCookiesEarned.add(amount);
    }

    public void removeCookies(BigDecimal amount) {
        this.cookies = this.cookies.subtract(amount);
    }

    public boolean canAfford(BigDecimal amount) {
        return this.cookies.compareTo(amount) >= 0;
    }

    // --- Stats ---

    public BigDecimal getCps() {
        BigDecimal baseCps = BigDecimal.ZERO;
        for (UpgradeType type : UpgradeType.values()) {
            BigDecimal buildingCps = type.getCpsPerLevel().multiply(BigDecimal.valueOf(getUpgradeCount(type)));
            buildingCps = buildingCps.multiply(getBuildingMultiplier(type));
            baseCps = baseCps.add(buildingCps);
        }
        return baseCps.multiply(PrestigeManager.getUpgradeMultiplier(prestigeLevel))
                .multiply(getEffectMultiplier(ClickerUpgradeEffect.CPS_MULTIPLIER))
                .multiply(getQuestBonusMultiplier(QuestEffect.CPS_PERCENT));
    }

    public BigDecimal getCpc() {
        BigDecimal baseCpc = BigDecimal.ONE;
        for (UpgradeType type : UpgradeType.values()) {
            BigDecimal buildingCpc = type.getCpcPerLevel().multiply(BigDecimal.valueOf(getUpgradeCount(type)));
            buildingCpc = buildingCpc.multiply(getBuildingMultiplier(type));
            baseCpc = baseCpc.add(buildingCpc);
        }
        return baseCpc.multiply(PrestigeManager.getClickMultiplier(prestigeLevel, aura))
                .multiply(getEffectMultiplier(ClickerUpgradeEffect.CPC_MULTIPLIER))
                .multiply(getQuestBonusMultiplier(QuestEffect.CPC_PERCENT))
                .add(PrestigeManager.getBaseClickAdditive(prestigeLevel));
    }

    /**
     * Get the combined multiplier from all completed quests of a given effect type.
     * Each quest grants +X%, so the multiplier is 1 + (sum of all percents / 100).
     */
    public BigDecimal getQuestBonusMultiplier(QuestEffect effectType) {
        int totalPercent = 0;
        for (Quest quest : completedQuests) {
            if (quest.getEffect() == effectType) {
                totalPercent += quest.getEffectPercent();
            }
        }
        if (totalPercent == 0) return BigDecimal.ONE;
        return BigDecimal.ONE.add(BigDecimal.valueOf(totalPercent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
    }

    private BigDecimal getBuildingMultiplier(UpgradeType building) {
        BigDecimal mult = BigDecimal.ONE;
        for (ClickerUpgrade upgrade : purchasedUpgrades) {
            if (upgrade.getEffect() == ClickerUpgradeEffect.BUILDING_MULTIPLIER
                    && upgrade.getTargetBuilding() == building) {
                mult = mult.multiply(upgrade.getEffectValue());
            }
        }
        return mult;
    }

    /**
     * Get the combined multiplier from purchased upgrades of a given effect type targeting a specific building.
     */
    public BigDecimal getEffectMultiplier(ClickerUpgradeEffect effectType, UpgradeType targetBuilding) {
        BigDecimal mult = BigDecimal.ONE;
        for (ClickerUpgrade upgrade : purchasedUpgrades) {
            if (upgrade.getEffect() == effectType && upgrade.getTargetBuilding() == targetBuilding) {
                mult = mult.multiply(upgrade.getEffectValue());
            }
        }
        return mult;
    }

    /**
     * Get the combined multiplier from all purchased upgrades of a given effect type.
     */
    public BigDecimal getEffectMultiplier(ClickerUpgradeEffect effectType) {
        BigDecimal mult = BigDecimal.ONE;
        for (ClickerUpgrade upgrade : purchasedUpgrades) {
            if (upgrade.getEffect() == effectType) {
                mult = mult.multiply(upgrade.getEffectValue());
            }
        }
        return mult;
    }

    public BigDecimal getClickerEntropy() {
        BigDecimal entropy = BigDecimal.valueOf(timesClicked);
        for (UpgradeType type : UpgradeType.values()) {
            entropy = entropy.add(BigDecimal.valueOf((long) getUpgradeCount(type) * type.getEntropyWeight()));
        }
        entropy = entropy.add(lifetimeCookiesEarned.divide(CookieMath.ONE_HUNDRED, 0, RoundingMode.FLOOR));
        entropy = entropy.add(aura.multiply(BigDecimal.TEN));
        entropy = entropy.add(BigDecimal.valueOf((long) prestigeLevel * 1000));
        return entropy;
    }

    // --- Upgrades ---

    public int getUpgradeCount(UpgradeType type) {
        return upgrades.getOrDefault(type, 0);
    }

    public void setUpgradeCount(UpgradeType type, int count) {
        upgrades.put(type, count);
    }

    public boolean buyUpgrade(UpgradeType type) {
        if (prestigeLevel < type.getRequiredPrestigeLevel()) return false;
        BigDecimal cost = type.getCost(getUpgradeCount(type));
        if (!canAfford(cost)) return false;
        removeCookies(cost);
        setUpgradeCount(type, getUpgradeCount(type) + 1);
        return true;
    }

    public String serializeUpgrades() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<UpgradeType, Integer> entry : upgrades.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey().name()).append(":").append(entry.getValue());
        }
        return sb.toString();
    }

    // --- Purchased (one-time) upgrades ---

    public boolean hasPurchasedUpgrade(ClickerUpgrade upgrade) {
        return purchasedUpgrades.contains(upgrade);
    }

    public boolean buyClickerUpgrade(ClickerUpgrade upgrade) {
        if (purchasedUpgrades.contains(upgrade)) return false;
        if (!canAfford(upgrade.getCost())) return false;
        if (!upgrade.isUnlocked(this)) return false;
        removeCookies(upgrade.getCost());
        purchasedUpgrades.add(upgrade);
        return true;
    }

    public String serializePurchasedUpgrades() {
        return ClickerUpgrade.serialize(purchasedUpgrades);
    }

    // --- Completed quests ---

    public boolean hasCompletedQuest(Quest quest) {
        return completedQuests.contains(quest);
    }

    public void completeQuest(Quest quest) {
        completedQuests.add(quest);
    }

    public String serializeCompletedQuests() {
        return Quest.serialize(completedQuests);
    }

    public static Set<Quest> deserializeCompletedQuests(String data) {
        return Quest.deserialize(data);
    }

    public static EnumMap<UpgradeType, Integer> deserializeUpgrades(String data) {
        EnumMap<UpgradeType, Integer> map = new EnumMap<>(UpgradeType.class);
        for (UpgradeType type : UpgradeType.values()) {
            map.put(type, 0);
        }
        if (data != null && !data.isEmpty()) {
            for (String part : data.split(";")) {
                String[] kv = part.split(":");
                if (kv.length == 2) {
                    try {
                        UpgradeType type = UpgradeType.valueOf(kv[0]);
                        map.put(type, Integer.parseInt(kv[1]));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        return map;
    }

    // --- Roles ---

    public RealmRole getRole(String playerUuid) {
        return roles.getOrDefault(playerUuid, RealmRole.VISITOR);
    }

    public void setRole(String playerUuid, RealmRole role) {
        if (role == RealmRole.VISITOR) {
            roles.remove(playerUuid);
        } else {
            roles.put(playerUuid, role);
        }
    }

    public String serializeRoles() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, RealmRole> entry : roles.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append(":").append(entry.getValue().name());
        }
        return sb.toString();
    }

    public static Map<String, RealmRole> deserializeRoles(String data) {
        Map<String, RealmRole> map = new ConcurrentHashMap<>();
        if (data != null && !data.isEmpty()) {
            for (String part : data.split(";")) {
                String[] kv = part.split(":");
                if (kv.length == 2) {
                    try {
                        map.put(kv[0], RealmRole.valueOf(kv[1]));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        return map;
    }

    // --- Bans ---

    public boolean isBanned(String playerUuid) {
        return bans.contains(playerUuid);
    }

    // --- Digit count for leaderboard sorting ---

    public int getTotalCookiesDigits() {
        return CookieMath.digitCount(totalCookiesEarned);
    }

    public int getLifetimeCookiesDigits() {
        return CookieMath.digitCount(lifetimeCookiesEarned);
    }

    // --- Reset (keeps nothing profile-specific) ---

    public void reset() {
        this.cookies = BigDecimal.ZERO;
        this.totalCookiesEarned = BigDecimal.ZERO;
        this.timesClicked = 0;
        this.ownerClicks = 0;
        this.otherClicks = 0;
        for (UpgradeType type : UpgradeType.values()) {
            upgrades.put(type, 0);
        }
        this.purchasedUpgrades.clear();
        // completedQuests and goldenCookiesCollected survive prestige (permanent progress)
        this.prestigeLevel = 0;
        this.aura = BigDecimal.ZERO;
        this.lastCurrentDigitCount = 0;
        this.lastTotalDigitCount = 0;
        this.lastEntropyDigitCount = 0;
        this.lastEntropyLeadDigit = 0;
    }

    /**
     * Merge another profile's data into this one additively (for transfers).
     */
    public void mergeFrom(RealmProfile other) {
        this.cookies = this.cookies.add(other.cookies);
        this.totalCookiesEarned = this.totalCookiesEarned.add(other.totalCookiesEarned);
        this.lifetimeCookiesEarned = this.lifetimeCookiesEarned.add(other.lifetimeCookiesEarned);
        this.timesClicked += other.timesClicked;
        this.ownerClicks += other.ownerClicks;
        this.otherClicks += other.otherClicks;
        for (UpgradeType type : UpgradeType.values()) {
            this.upgrades.put(type, Math.max(this.getUpgradeCount(type), other.getUpgradeCount(type)));
        }
        this.purchasedUpgrades.addAll(other.purchasedUpgrades);
        this.completedQuests.addAll(other.completedQuests);
        this.goldenCookiesCollected += other.goldenCookiesCollected;
        this.prestigeLevel = Math.max(this.prestigeLevel, other.prestigeLevel);
        this.aura = this.aura.max(other.aura);
    }
}
