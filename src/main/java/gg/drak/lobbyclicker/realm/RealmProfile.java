package gg.drak.lobbyclicker.realm;

import gg.drak.lobbyclicker.achievements.Achievement;
import gg.drak.lobbyclicker.achievements.AchievementManager;
import gg.drak.lobbyclicker.math.CookieMath;
import gg.drak.lobbyclicker.prestige.PrestigeManager;
import gg.drak.lobbyclicker.upgrades.ClickerUpgrade;
import gg.drak.lobbyclicker.upgrades.ClickerUpgradeEffect;
import gg.drak.lobbyclicker.upgrades.UpgradeType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.LinkedHashSet;
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
    private BigDecimal giftedCookies;
    private long timesClicked;       // combined (owner + others)
    private long ownerClicks;        // clicks by the realm owner
    private long otherClicks;        // clicks by visitors
    private EnumMap<UpgradeType, Integer> upgrades;
    private Set<ClickerUpgrade> purchasedUpgrades;
    private int prestigeLevel;
    private BigDecimal aura;
    private boolean realmPublic;

    // Achievements
    private Set<Achievement> completedAchievements;
    private long goldenCookiesCollected;
    private BigDecimal cookiesFromClicks;
    private EnumMap<UpgradeType, BigDecimal> cookiesFromBuilding;
    private long createdAtMillis;
    private boolean goldenEarly;
    private boolean goldenLate;

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
        this.giftedCookies = BigDecimal.ZERO;
        this.timesClicked = 0;
        this.ownerClicks = 0;
        this.otherClicks = 0;
        this.upgrades = new EnumMap<>(UpgradeType.class);
        for (UpgradeType type : UpgradeType.values()) {
            upgrades.put(type, 0);
        }
        this.purchasedUpgrades = new LinkedHashSet<>();
        this.completedAchievements = new LinkedHashSet<>();
        this.goldenCookiesCollected = 0;
        this.cookiesFromClicks = BigDecimal.ZERO;
        this.cookiesFromBuilding = new EnumMap<>(UpgradeType.class);
        for (UpgradeType type : UpgradeType.values()) {
            cookiesFromBuilding.put(type, BigDecimal.ZERO);
        }
        this.createdAtMillis = System.currentTimeMillis();
        this.goldenEarly = false;
        this.goldenLate = false;
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

    public void addGiftedCookies(BigDecimal amount) {
        this.cookies = this.cookies.add(amount);
        this.giftedCookies = this.giftedCookies.add(amount);
    }

    public boolean canAfford(BigDecimal amount) {
        return this.cookies.compareTo(amount) >= 0;
    }

    // --- Stats ---

    public BigDecimal getBuildingRawCps(UpgradeType type) {
        int count = getUpgradeCount(type);
        if (count <= 0) return BigDecimal.ZERO;
        BigDecimal each = type.getCpsPerLevel().multiply(getBuildingMultiplier(type));
        if (type == UpgradeType.CURSOR) {
            each = each.add(getFingerBonus());
        }
        each = each.multiply(getSynergyMultiplier(type));
        return each.multiply(BigDecimal.valueOf(count));
    }

    public BigDecimal getRawCps() {
        BigDecimal baseCps = BigDecimal.ZERO;
        for (UpgradeType type : UpgradeType.values()) {
            baseCps = baseCps.add(getBuildingRawCps(type));
        }
        return baseCps;
    }

    public BigDecimal getCps() {
        return getRawCps()
                .multiply(PrestigeManager.getUpgradeMultiplier(prestigeLevel))
                .multiply(PrestigeManager.getAuraCpsMultiplier(aura))
                .multiply(getEffectMultiplier(ClickerUpgradeEffect.CPS_MULTIPLIER))
                .multiply(AchievementManager.milkMultiplier(this));
    }

    public BigDecimal getCpc() {
        BigDecimal click = BigDecimal.ONE.multiply(getEffectMultiplier(ClickerUpgradeEffect.CPC_MULTIPLIER));
        click = click.add(getFingerBonus());
        return click.multiply(PrestigeManager.getClickMultiplier(prestigeLevel, aura));
    }

    public BigDecimal getFingerBonus() {
        BigDecimal additive = BigDecimal.ZERO;
        BigDecimal mult = BigDecimal.ONE;
        for (ClickerUpgrade upgrade : purchasedUpgrades) {
            if (upgrade.getEffect() == ClickerUpgradeEffect.FINGER_ADDITIVE) {
                additive = additive.add(upgrade.getEffectValue());
            } else if (upgrade.getEffect() == ClickerUpgradeEffect.FINGER_MULTIPLIER) {
                mult = mult.multiply(upgrade.getEffectValue());
            }
        }
        if (additive.signum() == 0) return BigDecimal.ZERO;
        return additive.multiply(mult).multiply(BigDecimal.valueOf(getNonCursorBuildingCount()));
    }

    public int getNonCursorBuildingCount() {
        int total = 0;
        for (UpgradeType type : UpgradeType.values()) {
            if (type != UpgradeType.CURSOR) total += getUpgradeCount(type);
        }
        return total;
    }

    public int getTotalBuildingCount() {
        int total = 0;
        for (UpgradeType type : UpgradeType.values()) {
            total += getUpgradeCount(type);
        }
        return total;
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

    private BigDecimal getSynergyMultiplier(UpgradeType building) {
        BigDecimal bonus = BigDecimal.ZERO;
        for (ClickerUpgrade upgrade : purchasedUpgrades) {
            if (upgrade.getEffect() == ClickerUpgradeEffect.SYNERGY
                    && upgrade.getTargetBuilding() == building
                    && upgrade.getSynergyPartner() != null) {
                bonus = bonus.add(upgrade.getEffectValue()
                        .multiply(BigDecimal.valueOf(getUpgradeCount(upgrade.getSynergyPartner()))));
            }
        }
        return BigDecimal.ONE.add(bonus);
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

    // --- Achievements ---

    public boolean hasCompletedAchievement(Achievement achievement) {
        return completedAchievements.contains(achievement);
    }

    public void completeAchievement(Achievement achievement) {
        completedAchievements.add(achievement);
    }

    public Set<Achievement> getCompletedQuests() {
        return completedAchievements;
    }

    public void setCompletedQuests(Set<Achievement> achievements) {
        this.completedAchievements = achievements != null ? achievements : new LinkedHashSet<>();
    }

    public String serializeCompletedAchievements() {
        return Achievement.serialize(completedAchievements);
    }

    @Deprecated
    public String serializeCompletedQuests() {
        return serializeCompletedAchievements();
    }

    public static Set<Achievement> deserializeCompletedAchievements(String data) {
        return Achievement.deserialize(data);
    }

    public static Set<Achievement> deserializeCompletedQuests(String data) {
        return deserializeCompletedAchievements(data);
    }

    public void addCookiesFromClicks(BigDecimal amount) {
        this.cookiesFromClicks = this.cookiesFromClicks.add(amount);
    }

    public void addCookiesFromBuilding(UpgradeType type, BigDecimal amount) {
        cookiesFromBuilding.put(type, getCookiesFromBuilding(type).add(amount));
    }

    public BigDecimal getCookiesFromBuilding(UpgradeType type) {
        return cookiesFromBuilding.getOrDefault(type, BigDecimal.ZERO);
    }

    public void resetBuildingCookies() {
        for (UpgradeType type : UpgradeType.values()) {
            cookiesFromBuilding.put(type, BigDecimal.ZERO);
        }
    }

    public String serializeExtraStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("clicks:").append(cookiesFromClicks.toPlainString());
        sb.append(";created:").append(createdAtMillis);
        sb.append(";early:").append(goldenEarly ? "1" : "0");
        sb.append(";late:").append(goldenLate ? "1" : "0");
        for (UpgradeType type : UpgradeType.values()) {
            BigDecimal v = getCookiesFromBuilding(type);
            if (v.signum() > 0) {
                sb.append(";b_").append(type.name()).append(":").append(v.toPlainString());
            }
        }
        return sb.toString();
    }

    public void applyExtraStats(String data) {
        if (data == null || data.isEmpty()) return;
        for (String part : data.split(";")) {
            String[] kv = part.split(":", 2);
            if (kv.length != 2) continue;
            try {
                switch (kv[0]) {
                    case "clicks" -> cookiesFromClicks = new BigDecimal(kv[1]);
                    case "created" -> createdAtMillis = Long.parseLong(kv[1]);
                    case "early" -> goldenEarly = "1".equals(kv[1]);
                    case "late" -> goldenLate = "1".equals(kv[1]);
                    default -> {
                        if (kv[0].startsWith("b_")) {
                            UpgradeType type = UpgradeType.valueOf(kv[0].substring(2));
                            cookiesFromBuilding.put(type, new BigDecimal(kv[1]));
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
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
        this.cookiesFromClicks = BigDecimal.ZERO;
        resetBuildingCookies();
        this.goldenEarly = false;
        this.goldenLate = false;
        // achievements and goldenCookiesCollected survive prestige (permanent progress)
        this.prestigeLevel = 0;
        this.aura = BigDecimal.ZERO;
        this.giftedCookies = BigDecimal.ZERO;
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
        this.giftedCookies = this.giftedCookies.add(other.giftedCookies);
        this.timesClicked += other.timesClicked;
        this.ownerClicks += other.ownerClicks;
        this.otherClicks += other.otherClicks;
        for (UpgradeType type : UpgradeType.values()) {
            this.upgrades.put(type, Math.max(this.getUpgradeCount(type), other.getUpgradeCount(type)));
        }
        this.purchasedUpgrades.addAll(other.purchasedUpgrades);
        this.completedAchievements.addAll(other.completedAchievements);
        this.cookiesFromClicks = this.cookiesFromClicks.add(other.cookiesFromClicks);
        for (UpgradeType type : UpgradeType.values()) {
            this.cookiesFromBuilding.put(type, getCookiesFromBuilding(type).add(other.getCookiesFromBuilding(type)));
        }
        this.goldenCookiesCollected += other.goldenCookiesCollected;
        this.prestigeLevel = Math.max(this.prestigeLevel, other.prestigeLevel);
        // Aura is capped by what the merged lifetime total actually entitles the
        // player to. Taking a bare max of two auras against a summed lifetime can
        // leave aura above the curve, which would block prestiging forever
        // (gain = entitled - held would stay negative).
        BigDecimal mergedAura = this.aura.max(other.aura);
        BigDecimal entitled = PrestigeManager.totalAuraEarnable(this.lifetimeCookiesEarned);
        this.aura = mergedAura.min(entitled.max(BigDecimal.ZERO));
    }
}
