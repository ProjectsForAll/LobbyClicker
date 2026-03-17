package gg.drak.lobbyclicker.data;

import gg.drak.thebase.objects.Identifiable;
import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.events.own.PlayerCreationEvent;
import gg.drak.lobbyclicker.math.CookieMath;
import gg.drak.lobbyclicker.prestige.PrestigeManager;
import gg.drak.lobbyclicker.settings.PlayerSettings;
import gg.drak.lobbyclicker.upgrades.UpgradeType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter @Setter
public class PlayerData implements Identifiable {
    private String identifier;
    private String name;
    private BigDecimal cookies;
    private BigDecimal totalCookiesEarned;
    private long timesClicked;
    private EnumMap<UpgradeType, Integer> upgrades;
    private PlayerSettings settings;
    private boolean realmPublic;
    private int prestigeLevel;
    private BigDecimal aura;
    private AtomicBoolean fullyLoaded;

    // Social data (loaded from DB on join)
    private Set<String> friends;
    private Set<String> bans;
    private Set<String> blocks;
    private Set<String> incomingFriendRequests;
    private Set<String> outgoingFriendRequests;

    // Milestone tracking (not persisted) - tracks digit counts for instant detection
    private int lastCurrentDigitCount;
    private int lastTotalDigitCount;
    private int lastEntropyDigitCount;
    private int lastEntropyLeadDigit;

    public PlayerData(String identifier, String name) {
        this.identifier = identifier;
        this.name = name;
        this.cookies = BigDecimal.ZERO;
        this.totalCookiesEarned = BigDecimal.ZERO;
        this.timesClicked = 0;
        this.upgrades = new EnumMap<>(UpgradeType.class);
        for (UpgradeType type : UpgradeType.values()) {
            upgrades.put(type, 0);
        }
        this.settings = new PlayerSettings();
        this.realmPublic = false;
        this.prestigeLevel = 0;
        this.aura = BigDecimal.ZERO;
        this.fullyLoaded = new AtomicBoolean(false);
        this.friends = ConcurrentHashMap.newKeySet();
        this.bans = ConcurrentHashMap.newKeySet();
        this.blocks = ConcurrentHashMap.newKeySet();
        this.incomingFriendRequests = ConcurrentHashMap.newKeySet();
        this.outgoingFriendRequests = ConcurrentHashMap.newKeySet();
        this.lastCurrentDigitCount = 0;
        this.lastTotalDigitCount = 0;
        this.lastEntropyDigitCount = 0;
        this.lastEntropyLeadDigit = 0;
    }

    public PlayerData(String identifier, String name, BigDecimal cookies, BigDecimal totalCookiesEarned,
                      long timesClicked, String upgradeData, String settingsData, boolean realmPublic) {
        this(identifier, name, cookies, totalCookiesEarned, timesClicked, upgradeData, settingsData, realmPublic, 0, BigDecimal.ZERO);
    }

    public PlayerData(String identifier, String name, BigDecimal cookies, BigDecimal totalCookiesEarned,
                      long timesClicked, String upgradeData, String settingsData, boolean realmPublic,
                      int prestigeLevel, BigDecimal aura) {
        this(identifier, name);
        this.cookies = cookies;
        this.totalCookiesEarned = totalCookiesEarned;
        this.timesClicked = timesClicked;
        this.upgrades = deserializeUpgrades(upgradeData);
        this.settings = new PlayerSettings(settingsData);
        this.realmPublic = realmPublic;
        this.prestigeLevel = prestigeLevel;
        this.aura = aura;
        this.lastCurrentDigitCount = CookieMath.digitCount(cookies);
        this.lastTotalDigitCount = CookieMath.digitCount(totalCookiesEarned);
        BigDecimal entropy = this.getClickerEntropy();
        this.lastEntropyDigitCount = CookieMath.digitCount(entropy);
        this.lastEntropyLeadDigit = CookieMath.leadDigit(entropy);
    }

    public PlayerData(Player player) {
        this(player.getUniqueId().toString(), player.getName());
    }

    public PlayerData(String uuid) {
        this(uuid, "");
    }

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
        entropy = entropy.add(totalCookiesEarned.divide(CookieMath.ONE_HUNDRED, 0, java.math.RoundingMode.FLOOR));
        // Factor in aura and prestige
        entropy = entropy.add(aura.multiply(BigDecimal.TEN));
        entropy = entropy.add(BigDecimal.valueOf((long) prestigeLevel * 1000));
        return entropy;
    }

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

    public Optional<Player> asPlayer() {
        try {
            return Optional.ofNullable(Bukkit.getPlayer(UUID.fromString(identifier)));
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Failed to get player from identifier: " + identifier, e);
            return Optional.empty();
        }
    }

    public Optional<OfflinePlayer> asOfflinePlayer() {
        try {
            return Optional.of(Bukkit.getOfflinePlayer(UUID.fromString(identifier)));
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Failed to get offline player from identifier: " + identifier, e);
            return Optional.empty();
        }
    }

    public boolean isOnline() {
        return asPlayer().isPresent();
    }

    public void load() {
        PlayerManager.loadPlayer(this);
    }

    public void unload() {
        PlayerManager.unloadPlayer(this);
    }

    public void save() {
        PlayerManager.savePlayer(this);
    }

    public void save(boolean async) {
        PlayerManager.savePlayer(this, async);
    }

    public void augment(CompletableFuture<Optional<PlayerData>> future, boolean isGet) {
        fullyLoaded.set(false);

        future.whenComplete((data, error) -> {
            if (error != null) {
                LobbyClicker.getInstance().logWarning("Failed to augment player data", error);
                this.fullyLoaded.set(true);
                return;
            }

            if (data.isPresent()) {
                PlayerData newData = data.get();
                this.name = newData.getName();
                this.cookies = newData.getCookies();
                this.totalCookiesEarned = newData.getTotalCookiesEarned();
                this.timesClicked = newData.getTimesClicked();
                this.upgrades = newData.getUpgrades();
                this.settings = newData.getSettings();
                this.realmPublic = newData.isRealmPublic();
                this.prestigeLevel = newData.getPrestigeLevel();
                this.aura = newData.getAura();
                this.lastCurrentDigitCount = CookieMath.digitCount(this.cookies);
                this.lastTotalDigitCount = CookieMath.digitCount(this.totalCookiesEarned);
                BigDecimal ent = this.getClickerEntropy();
                this.lastEntropyDigitCount = CookieMath.digitCount(ent);
                this.lastEntropyLeadDigit = CookieMath.leadDigit(ent);
            } else {
                if (!isGet) {
                    new PlayerCreationEvent(this).fire();
                    this.save();
                }
            }

            // Load social data async
            loadSocialData().thenRun(() -> this.fullyLoaded.set(true));
        });
    }

    private CompletableFuture<Void> loadSocialData() {
        String uuid = this.identifier;
        return CompletableFuture.allOf(
                LobbyClicker.getDatabase().pullFriendsThreaded(uuid).thenAccept(this.friends::addAll),
                LobbyClicker.getDatabase().pullBansThreaded(uuid).thenAccept(this.bans::addAll),
                LobbyClicker.getDatabase().pullBlocksThreaded(uuid).thenAccept(this.blocks::addAll),
                LobbyClicker.getDatabase().pullIncomingRequestsThreaded(uuid).thenAccept(this.incomingFriendRequests::addAll),
                LobbyClicker.getDatabase().pullOutgoingRequestsThreaded(uuid).thenAccept(this.outgoingFriendRequests::addAll)
        );
    }

    public boolean isFullyLoaded() {
        return fullyLoaded.get();
    }

    public void saveAndUnload(boolean async) {
        save(async);
        unload();
    }

    public void saveAndUnload() {
        saveAndUnload(true);
    }

    /**
     * Returns the digit count used for leaderboard sorting in the database.
     */
    public int getTotalCookiesDigits() {
        return CookieMath.digitCount(totalCookiesEarned);
    }

    public PlayerData waitUntilFullyLoaded() {
        while (!isFullyLoaded()) {
            Thread.onSpinWait();
        }
        return this;
    }
}
