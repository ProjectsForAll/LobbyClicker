package gg.drak.lobbyclicker.realm;

import gg.drak.lobbyclicker.math.CookieMath;
import gg.drak.lobbyclicker.prestige.PrestigeManager;
import gg.drak.lobbyclicker.settings.PlayerSettings;
import gg.drak.lobbyclicker.upgrades.UpgradeType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
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
    private BigDecimal totalCookiesEarned;
    private long timesClicked;
    private EnumMap<UpgradeType, Integer> upgrades;
    private int prestigeLevel;
    private BigDecimal aura;
    private boolean realmPublic;

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
        this.timesClicked = 0;
        this.upgrades = new EnumMap<>(UpgradeType.class);
        for (UpgradeType type : UpgradeType.values()) {
            upgrades.put(type, 0);
        }
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
            baseCps = baseCps.add(type.getCpsPerLevel().multiply(BigDecimal.valueOf(getUpgradeCount(type))));
        }
        return baseCps.multiply(PrestigeManager.getUpgradeMultiplier(prestigeLevel));
    }

    public BigDecimal getCpc() {
        BigDecimal baseCpc = BigDecimal.ONE;
        for (UpgradeType type : UpgradeType.values()) {
            baseCpc = baseCpc.add(type.getCpcPerLevel().multiply(BigDecimal.valueOf(getUpgradeCount(type))));
        }
        return baseCpc.multiply(PrestigeManager.getClickMultiplier(prestigeLevel, aura))
                .add(PrestigeManager.getBaseClickAdditive(prestigeLevel));
    }

    public BigDecimal getClickerEntropy() {
        BigDecimal entropy = BigDecimal.valueOf(timesClicked);
        for (UpgradeType type : UpgradeType.values()) {
            entropy = entropy.add(BigDecimal.valueOf((long) getUpgradeCount(type) * type.getEntropyWeight()));
        }
        entropy = entropy.add(totalCookiesEarned.divide(CookieMath.ONE_HUNDRED, 0, RoundingMode.FLOOR));
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

    // --- Reset (keeps nothing profile-specific) ---

    public void reset() {
        this.cookies = BigDecimal.ZERO;
        this.totalCookiesEarned = BigDecimal.ZERO;
        this.timesClicked = 0;
        for (UpgradeType type : UpgradeType.values()) {
            upgrades.put(type, 0);
        }
        this.prestigeLevel = 0;
        this.aura = BigDecimal.ZERO;
        this.lastCurrentDigitCount = 0;
        this.lastTotalDigitCount = 0;
        this.lastEntropyDigitCount = 0;
        this.lastEntropyLeadDigit = 0;
    }

    /**
     * Merge another profile's data into this one additively (for transfers).
     * cookies add, totalEarned add, upgrades max, prestige add, aura add, clicks add.
     */
    public void mergeFrom(RealmProfile other) {
        this.cookies = this.cookies.add(other.cookies);
        this.totalCookiesEarned = this.totalCookiesEarned.add(other.totalCookiesEarned);
        this.timesClicked += other.timesClicked;
        for (UpgradeType type : UpgradeType.values()) {
            this.upgrades.put(type, Math.max(this.getUpgradeCount(type), other.getUpgradeCount(type)));
        }
        this.prestigeLevel += other.prestigeLevel;
        this.aura = this.aura.add(other.aura);
    }
}
