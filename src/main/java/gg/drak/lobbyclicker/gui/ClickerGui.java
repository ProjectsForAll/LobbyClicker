package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.math.CookieMath;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.realm.RealmRole;
import gg.drak.lobbyclicker.redis.RedisManager;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import gg.drak.lobbyclicker.settings.SettingType;
import gg.drak.lobbyclicker.social.RealmManager;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClickerGui extends BaseGui {
    private final PlayerData viewerData;
    private final PlayerData ownerData;
    private final boolean isVisiting;

    private static final int[] GOLDEN_COOKIE_SLOTS = {
            0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21,
            27, 28, 29, 30, 36, 37, 38, 39, 47
    };
    private static final Random RANDOM = new Random();
    private static final ConcurrentHashMap<UUID, ClickerGui> OPEN_GUIS = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<UUID, ClickerGui> getOpenGuis() { return OPEN_GUIS; }
    public static void registerGui(UUID uuid, ClickerGui gui) { OPEN_GUIS.put(uuid, gui); }
    public static void unregisterGui(UUID uuid) { OPEN_GUIS.remove(uuid); }

    private int goldenCookieSlot = -1;
    private int goldenCookieTicksLeft = 0;
    private BukkitTask goldenCookieTask;
    private int nextSpawnCountdown;
    private int frenzyCountdown;
    private int frenzyRemaining = -1;
    private int frenzySpawnInterval;

    // Own realm constructor
    public ClickerGui(Player player, PlayerData data) {
        this(player, data, data);
    }

    // Visiting constructor
    public ClickerGui(Player viewer, PlayerData viewerData, PlayerData ownerData) {
        super(viewer, "clicker-main",
                ownerData.getIdentifier().equals(viewerData.getIdentifier())
                        ? ChatColor.GOLD + "" + ChatColor.BOLD + "Cookie Clicker"
                        : ChatColor.GOLD + "" + ChatColor.BOLD + ownerData.getName() + "'s Realm",
                6);
        this.viewerData = viewerData;
        this.ownerData = ownerData;
        this.isVisiting = !viewerData.getIdentifier().equals(ownerData.getIdentifier());
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();

        fillGui(GuiHelper.filler());
        updateStats();
        updateDigitDisplay();
        addCookieItem(player);

        if (!isVisiting) {
            // Settings button
            Icon settings = GuiHelper.createIcon(Material.COMPARATOR,
                    ChatColor.YELLOW + "" + ChatColor.BOLD + "Settings",
                    "", ChatColor.GRAY + "Configure your preferences", "", ChatColor.YELLOW + "Click to open");
            settings.onClick(e -> {
                stopGoldenCookieTask();
                new SettingsMainGui(player, viewerData).open();
            });
            addItem(46, settings);

            // Prestige button
            Icon prestige = GuiHelper.createIcon(Material.BEACON,
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Prestige",
                    "", ChatColor.GRAY + "Level: " + ChatColor.WHITE + viewerData.getPrestigeLevel(),
                    ChatColor.GRAY + "Aura: " + ChatColor.WHITE + FormatUtils.format(viewerData.getAura()),
                    "", ChatColor.YELLOW + "Click to open");
            prestige.onClick(e -> {
                stopGoldenCookieTask();
                unregisterGui(player.getUniqueId());
                new PrestigeGui(player, viewerData).open();
            });
            addItem(49, prestige);

            // Upgrades button
            Icon upgrades = GuiHelper.createIcon(Material.CHEST,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "Upgrades",
                    "", ChatColor.GRAY + "Buy upgrades to earn", ChatColor.GRAY + "more cookies!", "", ChatColor.YELLOW + "Click to open");
            upgrades.onClick(e -> {
                stopGoldenCookieTask();
                unregisterGui(player.getUniqueId());
                new UpgradeGui(player, ownerData).open();
            });
            addItem(48, upgrades);

            // Profiles button
            Icon profiles = GuiHelper.createIcon(Material.BOOK,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "Profiles",
                    "", ChatColor.GRAY + "Switch realm profiles", "", ChatColor.YELLOW + "Click to open");
            profiles.onClick(e -> {
                stopGoldenCookieTask();
                unregisterGui(player.getUniqueId());
                new ProfileSelectorGui(player, viewerData).open();
            });
            addItem(47, profiles);
        } else {
            // Visitor: check role for upgrade access
            RealmProfile ownerProfile = ownerData.getActiveProfile();
            RealmRole viewerRole = ownerProfile != null
                    ? ownerProfile.getRole(viewerData.getIdentifier())
                    : RealmRole.VISITOR;

            if (viewerRole.canBuyUpgrades()) {
                // GARDENER+ can buy upgrades
                Icon upgrades = GuiHelper.createIcon(Material.CHEST,
                        ChatColor.GREEN + "" + ChatColor.BOLD + "Upgrades",
                        "", ChatColor.GRAY + "Buy upgrades for this realm!",
                        ChatColor.DARK_GREEN + "Role: " + viewerRole.getDisplayName(),
                        "", ChatColor.YELLOW + "Click to open");
                upgrades.onClick(e -> {
                    stopGoldenCookieTask();
                    unregisterGui(player.getUniqueId());
                    new UpgradeGui(player, ownerData).open();
                });
                addItem(48, upgrades);
            } else {
                addItem(48, GuiHelper.createIcon(Material.IRON_BARS,
                        ChatColor.GRAY + "" + ChatColor.BOLD + "Upgrades Locked",
                        "", ChatColor.GRAY + "You need Gardener role or higher"));
            }

            // Register as viewer
            RealmManager.addViewer(ownerData.getIdentifier(), viewerData.getIdentifier());

            // Back button (return to own clicker)
            Icon back = GuiHelper.createIcon(Material.ARROW,
                    ChatColor.RED + "" + ChatColor.BOLD + "Back",
                    "", ChatColor.GRAY + "Return to your realm");
            back.onClick(e -> {
                stopGoldenCookieTask();
                unregisterGui(player.getUniqueId());
                RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                new ClickerGui(player, viewerData).open();
            });
            addItem(46, back);

            // Main Menu button
            Icon mainMenu = GuiHelper.homeButton();
            mainMenu.onClick(e -> {
                stopGoldenCookieTask();
                unregisterGui(player.getUniqueId());
                RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                new ClickerGui(player, viewerData).open();
            });
            addItem(47, mainMenu);
        }

        // Social menu button (viewer's head)
        Icon social = GuiHelper.playerHead(player,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Social",
                "", ChatColor.GRAY + "Friends, realms, players", "", ChatColor.YELLOW + "Click to open");
        social.onClick(e -> {
            stopGoldenCookieTask();
            unregisterGui(player.getUniqueId());
            if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
            new SocialMainGui(player, viewerData).open();
        });
        addItem(45, social);

        // Leaderboard button
        Icon leaderboard = GuiHelper.createIcon(Material.OAK_SIGN,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Leaderboard",
                "", ChatColor.GRAY + "See the top cookie earners!", "", ChatColor.YELLOW + "Click to view");
        leaderboard.onClick(e -> {
            stopGoldenCookieTask();
            unregisterGui(player.getUniqueId());
            if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
            new LeaderboardGui(player, viewerData).open();
        });
        addItem(50, leaderboard);

        // Close button
        Icon close = GuiHelper.createIcon(Material.BARRIER, ChatColor.RED + "Close");
        close.onClick(e -> {
            stopGoldenCookieTask();
            unregisterGui(player.getUniqueId());
            if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
            player.closeInventory();
        });
        addItem(53, close);

        initGoldenCookieTimers();
        startGoldenCookieTask(player);

        // Register for global refresh
        registerGui(player.getUniqueId(), this);
    }

    public void refreshDisplay() {
        updateStats();
        updateDigitDisplay();
    }

    private void addCookieItem(Player player) {
        Icon cookie = GuiHelper.createIcon(Material.COOKIE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Cookie",
                "", ChatColor.YELLOW + "Click to earn cookies!",
                ChatColor.GRAY + "Per click: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCpc()));
        cookie.onClick(e -> {
            // Click rate limiting (20 CPS max)
            if (!viewerData.tryClick()) return;

            // Check if the owner is on a remote server (OO player)
            boolean ownerIsRemote = isVisiting && !ownerData.isOnline()
                    && LobbyClicker.getRedisManager() != null
                    && LobbyClicker.getRedisManager().isPlayerOnlineRemotely(ownerData.getIdentifier());

            if (ownerIsRemote) {
                // Forward click to home server via Redis — don't modify local data
                RedisSyncHandler.publishClick(ownerData.getIdentifier(), viewerData.getIdentifier(), viewerData.getName());
            } else {
                // Owner is local (or offline realm) — apply directly
                ownerData.addCookies(ownerData.getCpc());
                ownerData.setTimesClicked(ownerData.getTimesClicked() + 1);
            }

            updateStats();
            updateDigitDisplay();
            addCookieItem(player);

            if (viewerData.getSettings().isSoundEnabled(SettingType.SOUND_CLICKER)) {
                float vol = viewerData.getSettings().getVolume(SettingType.VOLUME_CLICKER);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, vol, 2.0f);
            }

            // Notify realm owner if visitor is clicking (local owner only)
            if (isVisiting && !ownerIsRemote) {
                Player owner = Bukkit.getPlayer(java.util.UUID.fromString(ownerData.getIdentifier()));
                if (owner != null) {
                    boolean isFriend = ownerData.getFriends().contains(viewerData.getIdentifier());
                    SettingType st = isFriend ? SettingType.SOUND_FRIEND_CLICKER : SettingType.SOUND_RANDO_CLICKER;
                    SettingType vt = isFriend ? SettingType.VOLUME_FRIEND_CLICKER : SettingType.VOLUME_RANDO_CLICKER;
                    if (ownerData.getSettings().isSoundEnabled(st)) {
                        owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, ownerData.getSettings().getVolume(vt), 1.5f);
                    }
                }
            }
        });
        addItem(22, cookie);
    }

    private void updateStats() {
        String title = isVisiting
                ? ChatColor.GOLD + "" + ChatColor.BOLD + ownerData.getName() + "'s Stats"
                : ChatColor.GOLD + "" + ChatColor.BOLD + "Cookie Stats";
        Icon stats = GuiHelper.createIcon(Material.NETHER_STAR, title,
                "",
                ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCookies()),
                ChatColor.GRAY + "Total Earned: " + ChatColor.WHITE + FormatUtils.format(ownerData.getTotalCookiesEarned()),
                "",
                ChatColor.GRAY + "Per Click: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCpc()),
                ChatColor.GRAY + "Per Second: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCps()),
                "",
                ChatColor.LIGHT_PURPLE + "Prestige: " + ChatColor.WHITE + ownerData.getPrestigeLevel(),
                ChatColor.LIGHT_PURPLE + "Aura: " + ChatColor.WHITE + FormatUtils.format(ownerData.getAura()),
                ChatColor.LIGHT_PURPLE + "Clicker Entropy: " + ChatColor.WHITE + FormatUtils.format(ownerData.getClickerEntropy()));
        addItem(4, stats);
    }

    private void updateDigitDisplay() {
        String cookieDisplay = ChatColor.GRAY + "Current Cookies: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCookies());
        String[] display = BannerUtil.parseBannerDisplay(ownerData.getCookies());
        for (int i = 0; i < 4; i++) {
            addItem(5 + i, BannerUtil.charBannerIcon(display[i], cookieDisplay));
        }
    }

    // --- Golden Cookie System ---

    private void initGoldenCookieTimers() {
        nextSpawnCountdown = 300 + RANDOM.nextInt(301);
        frenzyCountdown = 1800 + RANDOM.nextInt(901);
        frenzyRemaining = -1;
    }

    private void startGoldenCookieTask(Player player) {
        goldenCookieTask = Bukkit.getScheduler().runTaskTimer(LobbyClicker.getInstance(), () -> {
            if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(getInventory())) {
                stopGoldenCookieTask();
                unregisterGui(player.getUniqueId());
                if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                return;
            }
            tickGoldenCookie(player);
        }, 20L, 20L);
    }

    private void tickGoldenCookie(Player player) {
        if (goldenCookieSlot >= 0) {
            goldenCookieTicksLeft--;
            if (goldenCookieTicksLeft <= 0) removeGoldenCookie();
            return;
        }

        if (frenzyRemaining > 0) {
            frenzyRemaining--;
            nextSpawnCountdown--;
            if (nextSpawnCountdown <= 0) {
                spawnGoldenCookie(player);
                nextSpawnCountdown = frenzySpawnInterval;
            }
            if (frenzyRemaining <= 0) {
                frenzyRemaining = -1;
                nextSpawnCountdown = 300 + RANDOM.nextInt(301);
                frenzyCountdown = 1800 + RANDOM.nextInt(901);
                player.sendMessage(ChatColor.GOLD + "Cookie Frenzy has ended!");
            }
        } else {
            nextSpawnCountdown--;
            frenzyCountdown--;
            if (nextSpawnCountdown <= 0) {
                spawnGoldenCookie(player);
                nextSpawnCountdown = 300 + RANDOM.nextInt(301);
            }
            if (frenzyCountdown <= 0) {
                frenzyRemaining = 300;
                frenzySpawnInterval = 10 + RANDOM.nextInt(21);
                nextSpawnCountdown = frenzySpawnInterval;
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "COOKIE FRENZY! " +
                        ChatColor.YELLOW + "Golden cookies will appear rapidly for 5 minutes!");
                if (ownerData.getSettings().isSoundEnabled(SettingType.SOUND_CLICKER)) {
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
            }
        }
    }

    private void stopGoldenCookieTask() {
        if (goldenCookieTask != null && !goldenCookieTask.isCancelled()) {
            goldenCookieTask.cancel();
            goldenCookieTask = null;
        }
    }

    private void spawnGoldenCookie(Player player) {
        int slot = GOLDEN_COOKIE_SLOTS[RANDOM.nextInt(GOLDEN_COOKIE_SLOTS.length)];
        goldenCookieSlot = slot;
        goldenCookieTicksLeft = 3;

        BigDecimal multiplier = BigDecimal.valueOf(0.1 + RANDOM.nextDouble() * 1.9);
        BigDecimal bonus = ownerData.getClickerEntropy().multiply(multiplier);

        Icon golden = GuiHelper.createIcon(Material.GOLDEN_APPLE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Golden Cookie!",
                "", ChatColor.YELLOW + "Click for a cookie bonus!", ChatColor.GRAY + "Hurry, it won't last long!");
        golden.onClick(e -> {
            if (goldenCookieSlot < 0) return;
            // Golden cookies always apply locally (bonus is entropy-based, computed at spawn time)
            // For OO realms, the DATA_SYNC from home server will reconcile
            ownerData.addCookies(bonus);
            int clickedSlot = goldenCookieSlot;
            goldenCookieSlot = -1;
            goldenCookieTicksLeft = 0;
            addItem(clickedSlot, GuiHelper.filler());
            updateStats();
            updateDigitDisplay();
            addCookieItem(player);
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Golden Cookie! " +
                    ChatColor.YELLOW + "+" + FormatUtils.format(bonus) + " cookies");
            if (viewerData.getSettings().isSoundEnabled(SettingType.SOUND_CLICKER)) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        });
        addItem(slot, golden);
        if (viewerData.getSettings().isSoundEnabled(SettingType.SOUND_CLICKER)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.5f);
        }
    }

    private void removeGoldenCookie() {
        if (goldenCookieSlot >= 0) {
            addItem(goldenCookieSlot, GuiHelper.filler());
            goldenCookieSlot = -1;
            goldenCookieTicksLeft = 0;
        }
    }
}
