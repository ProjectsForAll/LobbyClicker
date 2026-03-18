package gg.drak.lobbyclicker.data;

import gg.drak.thebase.objects.Identifiable;
import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.events.own.PlayerCreationEvent;
import gg.drak.lobbyclicker.math.CookieMath;
import gg.drak.lobbyclicker.realm.ProfileManager;
import gg.drak.lobbyclicker.realm.RealmProfile;
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

/**
 * Player-level data. Global to the player across all realms.
 * Realm-specific data (cookies, upgrades, prestige, aura, bans, roles) lives on RealmProfile.
 * Delegation methods forward to the active profile for backwards compatibility.
 */
@Getter @Setter
public class PlayerData implements Identifiable {
    private String identifier;
    private String name;
    private PlayerSettings settings;        // global
    private AtomicBoolean fullyLoaded;

    // Active realm profile
    private String activeProfileId;

    // Social data - global (loaded from DB on join)
    private Set<String> friends;
    private Set<String> blocks;
    private Set<String> incomingFriendRequests;
    private Set<String> outgoingFriendRequests;

    // Global click counter (all clicks across all profiles + other players' realms)
    private long globalClicks;

    // Click rate limiting
    private transient long[] clickTimestamps = new long[20];
    private transient int clickIndex = 0;
    private transient long lastClickTime = 0;

    public PlayerData(String identifier, String name) {
        this.identifier = identifier;
        this.name = name;
        this.settings = new PlayerSettings();
        this.fullyLoaded = new AtomicBoolean(false);
        this.friends = ConcurrentHashMap.newKeySet();
        this.blocks = ConcurrentHashMap.newKeySet();
        this.incomingFriendRequests = ConcurrentHashMap.newKeySet();
        this.outgoingFriendRequests = ConcurrentHashMap.newKeySet();
        this.globalClicks = 0;
        this.clickTimestamps = new long[20];
        this.clickIndex = 0;
    }

    /**
     * Legacy constructor for DB loading. Creates PlayerData + a default RealmProfile with the given realm data.
     */
    public PlayerData(Player player) {
        this(player.getUniqueId().toString(), player.getName());
    }

    public PlayerData(String uuid) {
        this(uuid, "");
    }

    // --- Active profile access ---

    public RealmProfile getActiveProfile() {
        return ProfileManager.getProfile(activeProfileId).orElse(null);
    }

    // --- Delegation methods (forward to active profile for backwards compatibility) ---

    public BigDecimal getCookies() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getCookies() : BigDecimal.ZERO;
    }

    public void setCookies(BigDecimal cookies) {
        RealmProfile p = getActiveProfile();
        if (p != null) p.setCookies(cookies);
    }

    public BigDecimal getTotalCookiesEarned() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getTotalCookiesEarned() : BigDecimal.ZERO;
    }

    public void setTotalCookiesEarned(BigDecimal total) {
        RealmProfile p = getActiveProfile();
        if (p != null) p.setTotalCookiesEarned(total);
    }

    public long getTimesClicked() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getTimesClicked() : 0;
    }

    public void setTimesClicked(long clicks) {
        RealmProfile p = getActiveProfile();
        if (p != null) p.setTimesClicked(clicks);
    }

    public long getOwnerClicks() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getOwnerClicks() : 0;
    }

    public long getOtherClicks() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getOtherClicks() : 0;
    }

    public EnumMap<UpgradeType, Integer> getUpgrades() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getUpgrades() : new EnumMap<>(UpgradeType.class);
    }

    public void setUpgrades(EnumMap<UpgradeType, Integer> upgrades) {
        RealmProfile p = getActiveProfile();
        if (p != null) p.setUpgrades(upgrades);
    }

    public boolean isRealmPublic() {
        RealmProfile p = getActiveProfile();
        return p != null && p.isRealmPublic();
    }

    public void setRealmPublic(boolean realmPublic) {
        RealmProfile p = getActiveProfile();
        if (p != null) p.setRealmPublic(realmPublic);
    }

    public int getPrestigeLevel() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getPrestigeLevel() : 0;
    }

    public void setPrestigeLevel(int level) {
        RealmProfile p = getActiveProfile();
        if (p != null) p.setPrestigeLevel(level);
    }

    public BigDecimal getAura() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getAura() : BigDecimal.ZERO;
    }

    public void setAura(BigDecimal aura) {
        RealmProfile p = getActiveProfile();
        if (p != null) p.setAura(aura);
    }

    public Set<String> getBans() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getBans() : Collections.emptySet();
    }

    public void addCookies(BigDecimal amount) {
        RealmProfile p = getActiveProfile();
        if (p != null) p.addCookies(amount);
    }

    public void removeCookies(BigDecimal amount) {
        RealmProfile p = getActiveProfile();
        if (p != null) p.removeCookies(amount);
    }

    public boolean canAfford(BigDecimal amount) {
        RealmProfile p = getActiveProfile();
        return p != null && p.canAfford(amount);
    }

    public BigDecimal getCps() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getCps() : BigDecimal.ZERO;
    }

    public BigDecimal getCpc() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getCpc() : BigDecimal.ONE;
    }

    public BigDecimal getClickerEntropy() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getClickerEntropy() : BigDecimal.ZERO;
    }

    public int getUpgradeCount(UpgradeType type) {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getUpgradeCount(type) : 0;
    }

    public void setUpgradeCount(UpgradeType type, int count) {
        RealmProfile p = getActiveProfile();
        if (p != null) p.setUpgradeCount(type, count);
    }

    public boolean buyUpgrade(UpgradeType type) {
        RealmProfile p = getActiveProfile();
        return p != null && p.buyUpgrade(type);
    }

    public String serializeUpgrades() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.serializeUpgrades() : "";
    }

    public static EnumMap<UpgradeType, Integer> deserializeUpgrades(String data) {
        return RealmProfile.deserializeUpgrades(data);
    }

    // Milestone delegation
    public int getLastCurrentDigitCount() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getLastCurrentDigitCount() : 0;
    }

    public void setLastCurrentDigitCount(int v) {
        RealmProfile p = getActiveProfile();
        if (p != null) p.setLastCurrentDigitCount(v);
    }

    public int getLastTotalDigitCount() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getLastTotalDigitCount() : 0;
    }

    public void setLastTotalDigitCount(int v) {
        RealmProfile p = getActiveProfile();
        if (p != null) p.setLastTotalDigitCount(v);
    }

    public int getLastEntropyDigitCount() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getLastEntropyDigitCount() : 0;
    }

    public void setLastEntropyDigitCount(int v) {
        RealmProfile p = getActiveProfile();
        if (p != null) p.setLastEntropyDigitCount(v);
    }

    public int getLastEntropyLeadDigit() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getLastEntropyLeadDigit() : 0;
    }

    public void setLastEntropyLeadDigit(int v) {
        RealmProfile p = getActiveProfile();
        if (p != null) p.setLastEntropyLeadDigit(v);
    }

    public int getTotalCookiesDigits() {
        RealmProfile p = getActiveProfile();
        return p != null ? p.getTotalCookiesDigits() : 0;
    }

    // --- Click rate limiting ---

    /**
     * Returns true if the click is allowed (under configurable CPS limit).
     */
    public boolean tryClick() {
        long now = System.currentTimeMillis();

        // Minimum 25ms between clicks to prevent duplicate event counting
        if (now - lastClickTime < 25) return false;

        int maxCps = LobbyClicker.getMainConfig().getMaxClicksPerSecond();
        if (clickTimestamps.length != maxCps) {
            clickTimestamps = new long[maxCps];
            clickIndex = 0;
        }
        int idx = clickIndex % clickTimestamps.length;
        long oldest = clickTimestamps[idx];
        if (oldest != 0 && now - oldest < 1000) {
            return false;
        }
        clickTimestamps[idx] = now;
        clickIndex++;
        lastClickTime = now;
        return true;
    }

    // --- Player utility ---

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
                this.settings = newData.getSettings();
                this.activeProfileId = newData.getActiveProfileId();
                this.globalClicks = newData.getGlobalClicks();
            } else {
                if (!isGet) {
                    new PlayerCreationEvent(this).fire();
                    this.save();
                }
            }

            // Load profiles from DB, then social data
            loadProfiles().thenCompose(v -> loadSocialData()).thenRun(() -> this.fullyLoaded.set(true));
        });
    }

    /**
     * Load all profiles owned by this player from the database into ProfileManager.
     * If an activeProfileId is set, ensure that profile is loaded.
     */
    private CompletableFuture<Void> loadProfiles() {
        return LobbyClicker.getDatabase().pullProfilesByOwnerThreaded(this.identifier).thenAccept(profiles -> {
            for (RealmProfile profile : profiles) {
                ProfileManager.loadProfile(profile);
            }
            // If no active profile is set but profiles exist, use the first one
            if ((this.activeProfileId == null || this.activeProfileId.isEmpty()) && !profiles.isEmpty()) {
                this.activeProfileId = profiles.get(0).getProfileId();
            }
        });
    }

    private CompletableFuture<Void> loadSocialData() {
        String uuid = this.identifier;
        return CompletableFuture.allOf(
                LobbyClicker.getDatabase().pullFriendsThreaded(uuid).thenAccept(this.friends::addAll),
                LobbyClicker.getDatabase().pullBlocksThreaded(uuid).thenAccept(this.blocks::addAll),
                LobbyClicker.getDatabase().pullIncomingRequestsThreaded(uuid).thenAccept(this.incomingFriendRequests::addAll),
                LobbyClicker.getDatabase().pullOutgoingRequestsThreaded(uuid).thenAccept(this.outgoingFriendRequests::addAll)
        );
    }

    /**
     * Check if this player has selected a profile (has an active profile).
     */
    public boolean hasActiveProfile() {
        return activeProfileId != null && !activeProfileId.isEmpty() && getActiveProfile() != null;
    }

    /**
     * Get the max number of profiles this player can have, based on permissions.
     * Checks lobbyclicker.profiles.max.<amount> permissions.
     */
    public int getMaxProfiles() {
        Player player = asPlayer().orElse(null);
        if (player == null) return 1;

        int max = 1;
        for (int i = 1; i <= 100; i++) {
            if (player.hasPermission("lobbyclicker.profiles.max." + i)) {
                max = i;
            }
        }
        return max;
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

    public PlayerData waitUntilFullyLoaded() {
        while (!isFullyLoaded()) {
            Thread.onSpinWait();
        }
        return this;
    }
}
