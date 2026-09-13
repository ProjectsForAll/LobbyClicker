package gg.drak.lobbyclicker.golden;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.ClickerGui;
import gg.drak.lobbyclicker.gui.ClickerGuiHelper;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.settings.SettingType;
import gg.drak.lobbyclicker.social.RealmManager;
import gg.drak.lobbyclicker.upgrades.ClickerUpgradeEffect;
import gg.drak.lobbyclicker.utils.FormatUtils;
import gg.drak.lobbyclicker.utils.FoliaScheduler;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks golden cookie state per realm owner. Any GUI viewing this realm
 * can look up the holder to display and handle the golden cookie.
 *
 * <p>A spawned cookie's lifetime is wall-clock, not tick-counted: it expires
 * {@link #BASE_LIFETIME_MILLIS} after spawning regardless of whether any GUI is
 * open, so closing, reopening or navigating away neither cancels nor extends it.
 */
public class GoldenCookieHolder {
    private static final ConcurrentHashMap<String, GoldenCookieHolder> HOLDERS = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();
    private static final int[] GOLDEN_COOKIE_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30, 37, 38, 39};

    /** Base time a spawned cookie stays claimable, before duration upgrades. */
    private static final long BASE_LIFETIME_MILLIS = 30_000L;

    /** A claim within this window of spawn counts as "reflexes"; within this window of expiry counts as "clutch". */
    private static final long TIMING_WINDOW_MILLIS = 1_000L;

    /** Sentinel spawn id meaning "claim whatever is active", used by auto-collect. */
    private static final long ANY_SPAWN = -1L;

    private final String ownerUuid;

    // Current golden cookie state
    private long spawnId = 0;
    private int slot = -1;
    private CookieType cookieType = CookieType.NULL;
    private long spawnedAtMillis = 0;
    private long expiresAtMillis = 0;
    private BigDecimal reward = BigDecimal.ZERO;
    private String tierName = "";

    // Timer state
    private int nextSpawnCountdown;
    private int frenzyCountdown;
    private int frenzyRemaining = -1;
    private int frenzySpawnInterval;

    // Task
    private FoliaScheduler.PluginTask tickTask;

    private GoldenCookieHolder(String ownerUuid) {
        this.ownerUuid = ownerUuid;
        this.nextSpawnCountdown = getWeightedSpawnDelay();
        this.frenzyCountdown = 1800 + RANDOM.nextInt(901);
    }

    // --- Static access ---

    public static GoldenCookieHolder getOrCreate(String ownerUuid) {
        return HOLDERS.computeIfAbsent(ownerUuid, GoldenCookieHolder::new);
    }

    public static GoldenCookieHolder get(String ownerUuid) {
        return HOLDERS.get(ownerUuid);
    }

    public static void remove(String ownerUuid) {
        GoldenCookieHolder h = HOLDERS.remove(ownerUuid);
        if (h != null) h.stopTask();
    }

    public static void remove(UUID ownerUuid) {
        remove(ownerUuid.toString());
    }

    // --- Getters ---

    public int getSlot() { return slot; }
    public CookieType getCookieType() { return cookieType; }
    public BigDecimal getReward() { return reward; }
    public String getTierName() { return tierName; }
    public static int[] getSlots() { return GOLDEN_COOKIE_SLOTS; }

    /** Milliseconds until the active cookie vanishes, or 0 when none is claimable. */
    public long getMillisRemaining() {
        if (!hasActiveCookie()) return 0L;
        return Math.max(0L, expiresAtMillis - System.currentTimeMillis());
    }

    /**
     * True while a cookie is spawned and still within its lifetime. An expired cookie
     * is reaped here, so render paths see a consistent answer without the tick task.
     */
    public synchronized boolean hasActiveCookie() {
        if (slot < 0 || cookieType == CookieType.NULL) return false;
        if (System.currentTimeMillis() >= expiresAtMillis) {
            clearCookie();
            return false;
        }
        return true;
    }

    // --- Task management ---

    public void ensureTaskRunning() {
        if (tickTask != null && !tickTask.isCancelled()) return;
        Player owner = Bukkit.getPlayer(UUID.fromString(ownerUuid));
        if (owner != null) {
            tickTask = FoliaScheduler.runForEntityTimer(owner, LobbyClicker.getInstance(), this::tick, 20L, 20L);
        } else {
            tickTask = FoliaScheduler.runGlobalTimer(LobbyClicker.getInstance(), this::tick, 20L, 20L);
        }
    }

    public void stopTask() {
        if (tickTask != null && !tickTask.isCancelled()) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    public boolean isTaskRunning() {
        return tickTask != null && !tickTask.isCancelled();
    }

    // --- Core tick ---

    private void tick() {
        PlayerData ownerData = PlayerManager.getPlayer(ownerUuid).orElse(null);
        if (ownerData == null) {
            stopTask();
            return;
        }

        // A cookie left over from an earlier viewing session expires on its own clock;
        // clear the display for anyone who is watching now.
        if (slot >= 0 && !hasActiveCookie()) {
            pushClear();
            return;
        }

        if (!isAnyoneViewing()) {
            stopTask();
            return;
        }

        // A claimable cookie blocks further spawns; its expiry is wall-clock, not counted here.
        if (hasActiveCookie()) return;

        if (frenzyRemaining > 0) {
            frenzyRemaining--;
            nextSpawnCountdown--;
            if (nextSpawnCountdown <= 0) {
                spawnCookie(ownerData);
                nextSpawnCountdown = frenzySpawnInterval;
            }
            if (frenzyRemaining <= 0) {
                frenzyRemaining = -1;
                nextSpawnCountdown = getWeightedSpawnDelay();
                frenzyCountdown = 1800 + RANDOM.nextInt(901);
                if (!LobbyClicker.getMainConfig().isNotificationsDisabled()) {
                    Player owner = Bukkit.getPlayer(UUID.fromString(ownerUuid));
                    if (owner != null) {
                        FoliaScheduler.runForEntity(owner, LobbyClicker.getInstance(),
                                () -> owner.sendMessage(ChatColor.GOLD + "Cookie Frenzy has ended!"));
                    }
                }
            }
        } else {
            nextSpawnCountdown--;
            frenzyCountdown--;
            if (nextSpawnCountdown <= 0) {
                spawnCookie(ownerData);
                nextSpawnCountdown = getWeightedSpawnDelay();
            }
            if (frenzyCountdown <= 0) {
                frenzyRemaining = 300;
                frenzySpawnInterval = 10 + RANDOM.nextInt(21);
                nextSpawnCountdown = frenzySpawnInterval;
                if (!LobbyClicker.getMainConfig().isNotificationsDisabled()) {
                    Player owner = Bukkit.getPlayer(UUID.fromString(ownerUuid));
                    if (owner != null) {
                        FoliaScheduler.runForEntity(owner, LobbyClicker.getInstance(), () -> {
                            owner.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "COOKIE FRENZY! " +
                                    ChatColor.YELLOW + "Golden cookies will appear rapidly for 5 minutes!");
                            if (ownerData.getSettings().isSoundEnabled(SettingType.SOUND_CLICKER)) {
                                owner.playSound(owner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                            }
                        });
                    }
                }
            }
        }
    }

    private boolean isAnyoneViewing() {
        if (ClickerGui.getOpenGuis().containsKey(UUID.fromString(ownerUuid))) return true;
        for (String vuuid : RealmManager.getViewers(ownerUuid)) {
            if (ClickerGui.getOpenGuis().containsKey(UUID.fromString(vuuid))) return true;
        }
        return false;
    }

    // --- Spawn ---

    private void spawnCookie(PlayerData ownerData) {
        int newSlot = GOLDEN_COOKIE_SLOTS[RANDOM.nextInt(GOLDEN_COOKIE_SLOTS.length)];

        BigDecimal multiplier = BigDecimal.valueOf(0.1 + RANDOM.nextDouble() * 1.9);
        BigDecimal bonus = ownerData.getClickerEntropy().multiply(multiplier);
        bonus = bonus.multiply(ownerData.getEffectMultiplier(ClickerUpgradeEffect.GOLDEN_REWARD_MULTIPLIER));

        double normalized = multiplier.doubleValue() / 2.0;
        CookieType type;
        if (normalized >= 0.95) {
            type = CookieType.LEGENDARY;
        } else if (normalized >= 0.85) {
            type = CookieType.GRAND;
        } else if (normalized >= 0.50) {
            type = CookieType.MEDIUM;
        } else {
            type = CookieType.SMALL;
        }

        double durMult = ownerData.getEffectMultiplier(
                ClickerUpgradeEffect.GOLDEN_DURATION_MULTIPLIER).doubleValue();
        long lifetime = Math.max(1_000L, (long) (BASE_LIFETIME_MILLIS * durMult));

        long now = System.currentTimeMillis();
        synchronized (this) {
            this.spawnId++;
            this.slot = newSlot;
            this.cookieType = type;
            this.reward = bonus;
            this.tierName = type.getDisplayName();
            this.spawnedAtMillis = now;
            this.expiresAtMillis = now + lifetime;
        }

        pushRender();

        if (!LobbyClicker.getMainConfig().isNotificationsDisabled()) {
            notifyViewers(ownerUuid, p -> {
                PlayerData pd = PlayerManager.getPlayer(p.getUniqueId().toString()).orElse(null);
                if (pd != null && pd.getSettings().isSoundEnabled(SettingType.SOUND_CLICKER)) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.5f);
                }
            });
        }
    }

    // --- Claim ---

    /**
     * Claim the golden cookie for the realm owner. Returns true if this call is the one
     * that took it — concurrent claims from several viewers resolve to exactly one winner.
     */
    public boolean claim(Player clicker, PlayerData clickerData, PlayerData ownerData) {
        return claim(clicker, clickerData, ownerData, ANY_SPAWN);
    }

    /**
     * Claim only if the currently active cookie is the one identified by {@code spawnId}.
     * Icons captured for an earlier spawn are inert, so a click that arrives after that
     * cookie is gone cannot collect a later one in its place.
     */
    private boolean claim(Player clicker, PlayerData clickerData, PlayerData ownerData, long spawnId) {
        BigDecimal claimedReward;
        String claimedTierName;
        boolean early;
        boolean late;

        synchronized (this) {
            if (!hasActiveCookie()) return false;
            if (spawnId != ANY_SPAWN && spawnId != this.spawnId) return false;

            claimedReward = this.reward;
            claimedTierName = this.tierName;

            long now = System.currentTimeMillis();
            early = now - spawnedAtMillis <= TIMING_WINDOW_MILLIS;
            late = expiresAtMillis - now <= TIMING_WINDOW_MILLIS;

            clearCookie();
        }

        ownerData.addCookies(claimedReward);

        RealmProfile profile = ownerData.getActiveProfile();
        if (profile != null) {
            profile.setGoldenCookiesCollected(profile.getGoldenCookiesCollected() + 1);
            gg.drak.lobbyclicker.achievements.AchievementManager.markGoldenTiming(profile, early, late);
            gg.drak.lobbyclicker.achievements.AchievementManager.check(ownerData);
        }

        pushClear();
        refreshViewerDisplays();

        if (!LobbyClicker.getMainConfig().isNotificationsDisabled()) {
            clicker.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + claimedTierName + "! " +
                    ChatColor.YELLOW + "+" + FormatUtils.format(claimedReward) + " cookies");
            if (clickerData.getSettings().isSoundEnabled(SettingType.SOUND_CLICKER)) {
                clicker.playSound(clicker.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }

            String broadcastMsg = ChatColor.GOLD + clickerData.getName() + ChatColor.YELLOW +
                    " picked up a golden cookie worth " + ChatColor.GOLD + FormatUtils.format(claimedReward) +
                    ChatColor.YELLOW + " cookies!";
            if (!ownerData.getIdentifier().equals(clickerData.getIdentifier())) {
                ownerData.asPlayer().ifPresent(op -> op.sendMessage(broadcastMsg));
            }
            for (String vuuid : RealmManager.getViewers(ownerUuid)) {
                if (vuuid.equals(clickerData.getIdentifier())) continue;
                Player vp = Bukkit.getPlayer(UUID.fromString(vuuid));
                if (vp != null) vp.sendMessage(broadcastMsg);
            }
        }

        return true;
    }

    /**
     * Claim on behalf of a clicker who has auto-collect, either from the
     * {@code lobbyclicker.golden-cookie.auto-collect} permission or the realm's
     * Cookie Magnet upgrade. No-op when no cookie is claimable.
     */
    public boolean autoCollect(Player clicker, PlayerData clickerData, PlayerData ownerData) {
        if (!hasActiveCookie()) return false;
        return claim(clicker, clickerData, ownerData);
    }

    // --- Display propagation ---

    /** Draw the active cookie into every open GUI viewing this realm. */
    public void pushRender() {
        forEachOpenGui(ClickerGui::renderGoldenCookie);
    }

    /** Blank the golden cookie slot in every open GUI viewing this realm. */
    public void pushClear() {
        forEachOpenGui(ClickerGui::clearGoldenCookieSlot);
    }

    private void refreshViewerDisplays() {
        forEachOpenGui(ClickerGui::refreshDisplay);
    }

    private void forEachOpenGui(java.util.function.Consumer<ClickerGui> action) {
        ClickerGui ownerGui = ClickerGui.getOpenGuis().get(UUID.fromString(ownerUuid));
        if (ownerGui != null) runOnGuiOwner(ownerGui, action);
        for (String vuuid : RealmManager.getViewers(ownerUuid)) {
            ClickerGui gui = ClickerGui.getOpenGuis().get(UUID.fromString(vuuid));
            if (gui != null) runOnGuiOwner(gui, action);
        }
    }

    private void runOnGuiOwner(ClickerGui gui, java.util.function.Consumer<ClickerGui> action) {
        Player p = gui.getViewer();
        if (p == null || !p.isOnline()) return;
        FoliaScheduler.runForEntity(p, LobbyClicker.getInstance(), () -> action.accept(gui));
    }

    // --- Internal helpers ---

    private void clearCookie() {
        this.slot = -1;
        this.cookieType = CookieType.NULL;
        this.reward = BigDecimal.ZERO;
        this.tierName = "";
        this.spawnedAtMillis = 0;
        this.expiresAtMillis = 0;
    }

    private int getWeightedSpawnDelay() {
        PlayerData ownerData = PlayerManager.getPlayer(ownerUuid).orElse(null);
        if (ownerData == null) return 60;

        double raw = Math.pow(RANDOM.nextDouble(), 2.0);
        int baseDelay = 30 + (int) (raw * 270);
        double freqMult = ownerData.getEffectMultiplier(
                ClickerUpgradeEffect.GOLDEN_FREQ_MULTIPLIER).doubleValue();
        return Math.max(5, (int) (baseDelay / freqMult));
    }

    private static void notifyViewers(String ownerUuid, java.util.function.Consumer<Player> action) {
        Player owner = Bukkit.getPlayer(UUID.fromString(ownerUuid));
        if (owner != null) {
            FoliaScheduler.runForEntity(owner, LobbyClicker.getInstance(), () -> action.accept(owner));
        }
        for (String vuuid : RealmManager.getViewers(ownerUuid)) {
            Player vp = Bukkit.getPlayer(UUID.fromString(vuuid));
            if (vp != null) {
                FoliaScheduler.runForEntity(vp, LobbyClicker.getInstance(), () -> action.accept(vp));
            }
        }
    }

    /**
     * Build an Icon for the golden cookie at the current slot, for display in any GUI viewing this realm.
     */
    public synchronized Icon buildIcon(PlayerData clickerData, PlayerData ownerData) {
        if (!hasActiveCookie()) return null;
        final long boundSpawn = spawnId;
        Icon icon = ClickerGuiHelper.createIcon(cookieType.getMaterial(),
                ChatColor.GOLD + "" + ChatColor.BOLD + tierName + "!",
                "", ChatColor.YELLOW + "Click for +" + FormatUtils.format(reward) + " cookies!",
                ChatColor.GRAY + "Expires in " + ChatColor.WHITE + ((getMillisRemaining() + 999L) / 1000L) + "s");
        icon.onClick(e -> {
            Player p = (Player) e.getWhoClicked();
            claim(p, clickerData, ownerData, boundSpawn);
        });
        return icon;
    }
}
