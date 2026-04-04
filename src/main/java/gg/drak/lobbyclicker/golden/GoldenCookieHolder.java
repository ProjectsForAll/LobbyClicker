package gg.drak.lobbyclicker.golden;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.ClickerGui;
import gg.drak.lobbyclicker.gui.GuiHelper;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.settings.SettingType;
import gg.drak.lobbyclicker.social.RealmManager;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks golden cookie state per realm owner. Any GUI viewing this realm
 * can look up the holder to display and handle the golden cookie.
 */
public class GoldenCookieHolder {
    private static final ConcurrentHashMap<String, GoldenCookieHolder> HOLDERS = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();
    private static final int[] GOLDEN_COOKIE_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30, 37, 38, 39};

    private final String ownerUuid;

    // Current golden cookie state
    private int slot = -1;
    private CookieType cookieType = CookieType.NULL;
    private int ticksLeft = 0;
    private BigDecimal reward = BigDecimal.ZERO;
    private String tierName = "";

    // Timer state
    private int nextSpawnCountdown;
    private int frenzyCountdown;
    private int frenzyRemaining = -1;
    private int frenzySpawnInterval;

    // Task
    private BukkitTask tickTask;

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

    // --- Getters ---

    public int getSlot() { return slot; }
    public CookieType getCookieType() { return cookieType; }
    public BigDecimal getReward() { return reward; }
    public String getTierName() { return tierName; }
    public boolean hasActiveCookie() { return slot >= 0 && cookieType != CookieType.NULL; }
    public static int[] getSlots() { return GOLDEN_COOKIE_SLOTS; }

    // --- Task management ---

    public void ensureTaskRunning() {
        if (tickTask != null && !tickTask.isCancelled()) return;
        tickTask = Bukkit.getScheduler().runTaskTimer(LobbyClicker.getInstance(), this::tick, 20L, 20L);
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
        // Check if owner is still online or has viewers
        PlayerData ownerData = PlayerManager.getPlayer(ownerUuid).orElse(null);
        if (ownerData == null) {
            stopTask();
            return;
        }

        // Check if anyone is viewing this realm (owner on ClickerGui or visitors)
        boolean anyoneViewing = false;
        ClickerGui ownerGui = ClickerGui.getOpenGuis().get(UUID.fromString(ownerUuid));
        if (ownerGui != null) anyoneViewing = true;
        if (!anyoneViewing) {
            for (String vuuid : RealmManager.getViewers(ownerUuid)) {
                if (ClickerGui.getOpenGuis().containsKey(UUID.fromString(vuuid))) {
                    anyoneViewing = true;
                    break;
                }
            }
        }
        if (!anyoneViewing) {
            stopTask();
            return;
        }

        // If a golden cookie is active, count down
        if (slot >= 0) {
            ticksLeft--;
            if (ticksLeft <= 0) {
                clearCookie();
            }
            return;
        }

        // Timer logic
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
                    if (owner != null) owner.sendMessage(ChatColor.GOLD + "Cookie Frenzy has ended!");
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
                        owner.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "COOKIE FRENZY! " +
                                ChatColor.YELLOW + "Golden cookies will appear rapidly for 5 minutes!");
                        if (ownerData.getSettings().isSoundEnabled(SettingType.SOUND_CLICKER)) {
                            owner.playSound(owner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        }
                    }
                }
            }
        }
    }

    // --- Spawn ---

    private void spawnCookie(PlayerData ownerData) {
        int newSlot = GOLDEN_COOKIE_SLOTS[RANDOM.nextInt(GOLDEN_COOKIE_SLOTS.length)];

        BigDecimal multiplier = BigDecimal.valueOf(0.1 + RANDOM.nextDouble() * 1.9);
        BigDecimal bonus = ownerData.getClickerEntropy().multiply(multiplier);
        BigDecimal rewardMult = ownerData.getEffectMultiplier(
                gg.drak.lobbyclicker.upgrades.ClickerUpgradeEffect.GOLDEN_REWARD_MULTIPLIER);
        BigDecimal boosterRewardMult = gg.drak.lobbyclicker.LobbyClicker.getMainConfig().isBoostersEnabled()
                ? gg.drak.lobbyclicker.boosters.BoosterManager.getMultiplier(
                        ownerData.getIdentifier(), gg.drak.lobbyclicker.boosters.BoosterEffect.GOLDEN_REWARD)
                : java.math.BigDecimal.ONE;
        bonus = bonus.multiply(rewardMult).multiply(boosterRewardMult);

        double normalized = multiplier.doubleValue() / 2.0;
        CookieType type;
        String name;
        if (normalized >= 0.95) {
            type = CookieType.LEGENDARY;
            name = "Legendary Cookie";
        } else if (normalized >= 0.85) {
            type = CookieType.GRAND;
            name = "Jackpot";
        } else if (normalized >= 0.50) {
            type = CookieType.MEDIUM;
            name = "Golden Cookie";
        } else {
            type = CookieType.SMALL;
            name = "Lucky Cookie";
        }

        double durMult = ownerData.getEffectMultiplier(
                gg.drak.lobbyclicker.upgrades.ClickerUpgradeEffect.GOLDEN_DURATION_MULTIPLIER).doubleValue();

        this.slot = newSlot;
        this.cookieType = type;
        this.reward = bonus;
        this.tierName = name;
        this.ticksLeft = Math.max(2, (int) (10 * durMult));

        // Play spawn sound for anyone viewing
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
     * Claim the golden cookie. Cookies go to the REALM OWNER. Returns true if successful.
     */
    public boolean claim(Player clicker, PlayerData clickerData, PlayerData ownerData) {
        if (slot < 0 || cookieType == CookieType.NULL) return false;

        BigDecimal claimedReward = this.reward;
        String claimedTierName = this.tierName;

        // Add cookies to owner
        ownerData.addCookies(claimedReward);

        // Increment golden cookie counter on owner's profile
        RealmProfile profile = ownerData.getActiveProfile();
        if (profile != null) {
            profile.setGoldenCookiesCollected(profile.getGoldenCookiesCollected() + 1);
        }

        // Clear the cookie
        clearCookie();

        if (!LobbyClicker.getMainConfig().isNotificationsDisabled()) {
            // Notify clicker
            clicker.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + claimedTierName + "! " +
                    ChatColor.YELLOW + "+" + FormatUtils.format(claimedReward) + " cookies");
            if (clickerData.getSettings().isSoundEnabled(SettingType.SOUND_CLICKER)) {
                clicker.playSound(clicker.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }

            // Broadcast to others in realm
            String broadcastMsg = ChatColor.GOLD + clickerData.getName() + ChatColor.YELLOW +
                    " picked up a golden cookie worth " + ChatColor.GOLD + FormatUtils.format(claimedReward) + ChatColor.YELLOW + " cookies!";
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

    // --- Internal helpers ---

    private void clearCookie() {
        this.slot = -1;
        this.cookieType = CookieType.NULL;
        this.reward = BigDecimal.ZERO;
        this.tierName = "";
        this.ticksLeft = 0;
    }

    private int getWeightedSpawnDelay() {
        PlayerData ownerData = PlayerManager.getPlayer(ownerUuid).orElse(null);
        if (ownerData == null) return 60;

        double raw = Math.pow(RANDOM.nextDouble(), 2.0);
        int baseDelay = 30 + (int) (raw * 270);
        double freqMult = ownerData.getEffectMultiplier(
                gg.drak.lobbyclicker.upgrades.ClickerUpgradeEffect.GOLDEN_FREQ_MULTIPLIER).doubleValue();
        double boosterFreqMult = gg.drak.lobbyclicker.LobbyClicker.getMainConfig().isBoostersEnabled()
                ? gg.drak.lobbyclicker.boosters.BoosterManager.getMultiplier(
                        ownerData.getIdentifier(), gg.drak.lobbyclicker.boosters.BoosterEffect.GOLDEN_FREQ).doubleValue()
                : 1.0;
        double questFreqMult = 1.0;
        if (gg.drak.lobbyclicker.LobbyClicker.getMainConfig().isQuestsEnabled()) {
            RealmProfile profile = ownerData.getActiveProfile();
            if (profile != null) {
                questFreqMult = profile.getQuestBonusMultiplier(gg.drak.lobbyclicker.quests.QuestEffect.GOLDEN_FREQ_PERCENT).doubleValue();
            }
        }
        return Math.max(5, (int) (baseDelay / freqMult / boosterFreqMult / questFreqMult));
    }

    private static void notifyViewers(String ownerUuid, java.util.function.Consumer<Player> action) {
        Player owner = Bukkit.getPlayer(UUID.fromString(ownerUuid));
        if (owner != null) action.accept(owner);
        for (String vuuid : RealmManager.getViewers(ownerUuid)) {
            Player vp = Bukkit.getPlayer(UUID.fromString(vuuid));
            if (vp != null) action.accept(vp);
        }
    }

    /**
     * Build an Icon for the golden cookie at the current slot, for display in any GUI viewing this realm.
     */
    public Icon buildIcon(PlayerData clickerData, PlayerData ownerData) {
        if (!hasActiveCookie()) return null;
        Icon icon = GuiHelper.createIcon(cookieType.getMaterial(),
                ChatColor.GOLD + "" + ChatColor.BOLD + tierName + "!",
                "", ChatColor.YELLOW + "Click for +" + FormatUtils.format(reward) + " cookies!",
                ChatColor.GRAY + "Hurry, it won't last long!");
        icon.onClick(e -> {
            Player p = (Player) e.getWhoClicked();
            claim(p, clickerData, ownerData);
        });
        return icon;
    }
}
