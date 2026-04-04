package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.monitor.SimpleGuiMonitor;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.realm.RealmRole;
import gg.drak.lobbyclicker.redis.RedisManager;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import gg.drak.lobbyclicker.settings.SettingType;
import gg.drak.lobbyclicker.social.RealmManager;
import gg.drak.lobbyclicker.idle.OfflineCookieEarnings;
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
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ClickerGui extends SimpleGuiMonitor {
    private final PlayerData viewerData;
    private final PlayerData ownerData;
    private final boolean isVisiting;

    private static final int[] GOLDEN_COOKIE_SLOTS = {
            10, 11, 12, 19, 20, 21,
            28, 29, 30, 37, 38, 39
    };
    private static final Random RANDOM = new Random();
    private static final ConcurrentHashMap<UUID, ClickerGui> OPEN_GUIS = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<UUID, ClickerGui> getOpenGuis() { return OPEN_GUIS; }
    public static void registerGui(UUID uuid, ClickerGui gui) { OPEN_GUIS.put(uuid, gui); }
    public static void unregisterGui(UUID uuid) { OPEN_GUIS.remove(uuid); }

    private int goldenCookieSlot = -1;
    private int goldenCookieTicksLeft = 0;
    private ItemStack savedGoldenSlotItem = null;
    private BukkitTask goldenCookieTask;
    private boolean showBanners = true;

    // Golden cookie timer state — persisted per-player across GUI reopens
    private static final ConcurrentHashMap<UUID, GoldenCookieState> GOLDEN_STATE = new ConcurrentHashMap<>();

    private static class GoldenCookieState {
        int nextSpawnCountdown;
        int frenzyCountdown;
        int frenzyRemaining = -1;
        int frenzySpawnInterval;
    }

    public static void clearGoldenState(UUID uuid) {
        GOLDEN_STATE.remove(uuid);
    }

    // Click tracking — static per-player cache so data persists across GUI reopens
    private static final ConcurrentHashMap<UUID, java.util.Deque<ClickRecord>> CLICK_HISTORY = new ConcurrentHashMap<>();

    private java.util.Deque<ClickRecord> getClickHistory() {
        return CLICK_HISTORY.computeIfAbsent(java.util.UUID.fromString(ownerData.getIdentifier()),
                k -> new java.util.concurrent.ConcurrentLinkedDeque<>());
    }

    public static void clearClickHistory(UUID uuid) {
        CLICK_HISTORY.remove(uuid);
    }

    private static class ClickRecord {
        final String clickerName;
        final BigDecimal amount;
        final long timestamp;
        ClickRecord(String clickerName, BigDecimal amount) {
            this.clickerName = clickerName;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
        }
    }

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

        if (!isVisiting) {
            OfflineCookieEarnings.applyAndNotify(player, viewerData);
        }

        fillMonitorBorder();

        updateStats();
        updateDigitDisplay();
        updateServerBalance();
        updateClickInfoPane();
        addCookieItem(player);

        boolean simpleMode = LobbyClicker.getMainConfig().isSimpleMode();
        boolean socialEnabled = LobbyClicker.getMainConfig().isSocialFeaturesEnabled();

        if (!simpleMode) {
            // Quick action: Mail (index 9)
            Icon mail = GuiHelper.createIcon(Material.WRITABLE_BOOK,
                    ChatColor.YELLOW + "" + ChatColor.BOLD + "Mail",
                    "",
                    ChatColor.GRAY + "See:",
                    ChatColor.WHITE + " - Incoming Friend Requests",
                    ChatColor.WHITE + " - Payment Requests",
                    ChatColor.WHITE + " - Gambling Requests");
            mail.onClick(e -> {
                unregisterGui(player.getUniqueId());
                if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                new MailGui(player, viewerData).open();
            });
            addItem(9, mail);

            // Quick action: Friends (index 18, below Mail)
            String friendHeadUuid = viewerData.getIdentifier();
            long longestOnline = -1;
            for (String fUuid : viewerData.getFriends()) {
                Player fp = Bukkit.getPlayer(java.util.UUID.fromString(fUuid));
                if (fp != null && fp.isOnline()) {
                    long ticks = fp.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
                    if (ticks > longestOnline) {
                        longestOnline = ticks;
                        friendHeadUuid = fUuid;
                    }
                }
            }
            Icon friends = GuiHelper.playerHead(friendHeadUuid,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "Friends",
                    "", ChatColor.GRAY + "View your friends list");
            friends.onClick(e -> {
                unregisterGui(player.getUniqueId());
                if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                new FriendsListGui(player, viewerData, 0).open();
            });
            addItem(18, friends);

            // Quick action: Quests (index 17, right side of row 2)
            gg.drak.lobbyclicker.realm.RealmProfile questProfile = viewerData.getActiveProfile();
            int questsCompleted = 0;
            if (questProfile != null) {
                for (gg.drak.lobbyclicker.quests.Quest q : gg.drak.lobbyclicker.quests.Quest.values()) {
                    if (questProfile.hasCompletedQuest(q)) questsCompleted++;
                }
            }
            int questsTotal = gg.drak.lobbyclicker.quests.Quest.values().length;
            Icon quests = GuiHelper.createIcon(Material.WRITTEN_BOOK,
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Quests",
                    "", ChatColor.GRAY + "Progress: " + questsCompleted + "/" + questsTotal,
                    "", ChatColor.YELLOW + "Click to view quests");
            quests.onClick(e -> {
                unregisterGui(player.getUniqueId());
                if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                new QuestsGui(player, viewerData).open();
            });
            addItem(17, quests);

            // Quick action: Boosters (index 26, right side of row 3)
            int activeCount = gg.drak.lobbyclicker.boosters.BoosterManager.getActiveBoosters(viewerData.getIdentifier()).size();
            Icon boosters = GuiHelper.createIcon(Material.BREWING_STAND,
                    ChatColor.AQUA + "" + ChatColor.BOLD + "Boosters",
                    "", activeCount > 0 ? ChatColor.GREEN + "" + activeCount + " active booster" + (activeCount != 1 ? "s" : "")
                            : ChatColor.GRAY + "No active boosters",
                    "", ChatColor.YELLOW + "Click to manage boosters");
            boosters.onClick(e -> {
                unregisterGui(player.getUniqueId());
                if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                new BoostersMenuGui(player, viewerData).open();
            });
            addItem(26, boosters);
        }

        // === BOTTOM ROW ACTION BAR ===
        int b = (getSize() / 9 - 1) * 9; // bottom row start index

        boolean showSocial = !simpleMode && socialEnabled;

        // Slot 1 (b+0): Social button — hidden when social is disabled; settings moves here
        if (showSocial) {
            Icon social = GuiHelper.playerHead(player,
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Social",
                    "", ChatColor.GRAY + "Friends, realms, players");
            social.onClick(e -> {
                unregisterGui(player.getUniqueId());
                if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                new SocialMainGui(player, viewerData, isVisiting ? ownerData : null).open();
            });
            addItem(b, social);
        }

        int settingsSlot = showSocial ? b + 1 : b;
        Icon settings = GuiHelper.createIcon(Material.COMPARATOR,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Settings",
                "", ChatColor.GRAY + "Configure preferences");
        settings.onClick(e -> {
            unregisterGui(player.getUniqueId());
            if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
            if (simpleMode || !LobbyClicker.getMainConfig().isRealmSettingsMenuEnabled()) {
                Consumer<Player> back = isVisiting
                        ? p -> new ClickerGui(p, viewerData, ownerData).open()
                        : p -> new ClickerGui(p, viewerData).open();
                new PlayerSettingsGui(player, viewerData, back).open();
            } else {
                new SettingsMainGui(player, viewerData, isVisiting ? ownerData : null).open();
            }
        });
        addItem(settingsSlot, settings);

        // Slot 4 (b+3): Shop button
        if (!isVisiting) {
            Icon shop = GuiHelper.createIcon(Material.CHEST,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "Shop",
                    "", ChatColor.GRAY + "Buy helpers and upgrades!");
            shop.onClick(e -> {
                unregisterGui(player.getUniqueId());
                new ShopGui(player, viewerData, ownerData).open();
            });
            addItem(b + 3, shop);
        } else {
            RealmProfile ownerProfile = ownerData.getActiveProfile();
            RealmRole viewerRole = ownerProfile != null
                    ? ownerProfile.getRole(viewerData.getIdentifier())
                    : RealmRole.VISITOR;

            if (viewerRole.canBuyUpgrades()) {
                Icon shop = GuiHelper.createIcon(Material.CHEST,
                        ChatColor.GREEN + "" + ChatColor.BOLD + "Shop",
                        "", ChatColor.DARK_GREEN + "Role: " + viewerRole.getDisplayName());
                shop.onClick(e -> {
                    unregisterGui(player.getUniqueId());
                    new ShopGui(player, viewerData, ownerData).open();
                });
                addItem(b + 3, shop);
            } else {
                addItem(b + 3, GuiHelper.createIcon(Material.IRON_BARS,
                        ChatColor.GRAY + "" + ChatColor.BOLD + "Locked",
                        "", ChatColor.GRAY + "Need Gardener role"));
            }

            // Register as viewer
            RealmManager.addViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
        }

        // Slot 6 (b+5): Leaderboard button
        Icon leaderboard = GuiHelper.createIcon(Material.OAK_SIGN,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Leaderboard",
                "", ChatColor.GRAY + "Top cookie earners");
        leaderboard.onClick(e -> {
            unregisterGui(player.getUniqueId());
            if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
            new LeaderboardGui(player, viewerData, isVisiting ? ownerData : null).open();
        });
        addItem(b + 5, leaderboard);

        // Slot 7 (b+6): Prestige button
        Icon prestige = GuiHelper.createIcon(Material.BEACON,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Prestige",
                "", ChatColor.GRAY + "Level: " + ChatColor.WHITE + ownerData.getPrestigeLevel(),
                ChatColor.GRAY + "Aura: " + ChatColor.WHITE + FormatUtils.format(ownerData.getAura()));
        prestige.onClick(e -> {
            unregisterGui(player.getUniqueId());
            if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
            new PrestigeGui(player, viewerData, isVisiting ? ownerData : null).open();
        });
        addItem(b + 6, prestige);

        // Slot 8 (b+7): Profiles button (hidden when realm management menu is disabled — same as auto single-profile flow)
        if (!simpleMode && LobbyClicker.getMainConfig().isRealmSettingsMenuEnabled()) {
            Icon profiles = GuiHelper.createIcon(Material.BOOK,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "Profiles",
                    "", ChatColor.GRAY + "Switch realm profiles");
            profiles.onClick(e -> {
                unregisterGui(player.getUniqueId());
                if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                new ProfileSelectorGui(player, viewerData, isVisiting ? ownerData : null).open();
            });
            addItem(b + 7, profiles);
        }

        // Slot 9 (b+8): Close or My Realm button
        if (isVisiting) {
            Icon myRealm = GuiHelper.createIcon(Material.COOKIE,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "My Realm",
                    "", ChatColor.GRAY + "Return to your realm");
            myRealm.onClick(e -> {
                unregisterGui(player.getUniqueId());
                RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                new ClickerGui(player, viewerData).open();
            });
            addItem(b + 8, myRealm);
        } else {
            Icon close = GuiHelper.createIcon(Material.BARRIER, ChatColor.RED + "Close");
            close.onClick(e -> {
                unregisterGui(player.getUniqueId());
                player.closeInventory();
            });
            addItem(b + 8, close);
        }

        initGoldenCookieTimers();
        startGoldenCookieTask(player);

        // Register for global refresh
        registerGui(player.getUniqueId(), this);
    }

    public void refreshDisplay() {
        updateStats();
        updateDigitDisplay();
        updateServerBalance();
        updateClickInfoPane();
        updateCookieLore();
    }

    /**
     * Update the cookie item's lore without re-registering the click handler.
     */
    private void updateCookieLore() {
        ItemStack cookieItem = getInventory().getItem(22);
        if (cookieItem != null && cookieItem.getType() == Material.COOKIE) {
            ItemMeta meta = cookieItem.getItemMeta();
            if (meta != null) {
                meta.setLore(java.util.Arrays.asList(
                        MenuText.itemLine(""),
                        MenuText.itemLine(ChatColor.YELLOW + "Click to earn cookies!"),
                        MenuText.itemLine(ChatColor.GRAY + "Per click: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCpc())),
                        MenuText.itemLine(ChatColor.GRAY + "Your clicks: " + ChatColor.WHITE + FormatUtils.format(viewerData.getGlobalClicks()))));
                cookieItem.setItemMeta(meta);
            }
        }
    }

    private void addCookieItem(Player player) {
        // Create cookie icon WITHOUT a click handler — clicking is handled by onCookieClick()
        Icon cookie = GuiHelper.createIcon(Material.COOKIE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Cookie",
                "", ChatColor.YELLOW + "Click to earn cookies!",
                ChatColor.GRAY + "Per click: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCpc()),
                ChatColor.GRAY + "Your clicks: " + ChatColor.WHITE + FormatUtils.format(viewerData.getGlobalClicks()));
        addItem(22, cookie);
    }

    private long lastProcessedClickTime = 0;

    /**
     * Intercept all inventory clicks. For the cookie slot, handle it here
     * and block all other processing paths.
     */
    @Override
    public boolean onClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getSlot() == 22 && event.getClickedInventory() != null
                && event.getClickedInventory().equals(getInventory())) {
            event.setCancelled(true);
            handleCookieClick(event);
            return false;
        }
        return super.onClick(event);
    }

    @Override
    public boolean furtherClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getSlot() == 22 && event.getClickedInventory() != null
                && event.getClickedInventory().equals(getInventory())) {
            return false;
        }
        return super.furtherClick(event);
    }

    private void handleCookieClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!event.isLeftClick()) return;

        // Deduplicate: require minimum 100ms gap between processed clicks.
        // Minecraft can fire multiple InventoryClickEvents for a single physical click,
        // and fixed-bucket dedup (time/50) fails at bucket boundaries.
        long now = System.currentTimeMillis();
        if (now - lastProcessedClickTime < 100) return;
        lastProcessedClickTime = now;

        if (!viewerData.tryClick()) return;

            // Record click for the green info pane (apply CPC booster multiplier)
            BigDecimal clickAmount = ownerData.getCpc().multiply(
                    gg.drak.lobbyclicker.boosters.BoosterManager.getMultiplier(
                            ownerData.getIdentifier(), gg.drak.lobbyclicker.boosters.BoosterEffect.CPC_MULTIPLIER));
            getClickHistory().addFirst(new ClickRecord(viewerData.getName(), clickAmount));
            long cutoff = now - 30 * 60 * 1000;
            getClickHistory().removeIf(r -> r.timestamp < cutoff);

            // Check if the owner is on a remote server (OBO player)
            boolean ownerIsRemote = isVisiting && !ownerData.isOnline()
                    && LobbyClicker.getRedisManager() != null
                    && LobbyClicker.getRedisManager().isPlayerOnlineRemotely(ownerData.getIdentifier());

            if (ownerIsRemote) {
                RedisSyncHandler.publishClick(ownerData.getIdentifier(), viewerData.getIdentifier(), viewerData.getName());
            } else {
                ownerData.addCookies(clickAmount);
                ownerData.setTimesClicked(ownerData.getTimesClicked() + 1);
                gg.drak.lobbyclicker.realm.RealmProfile profile = ownerData.getActiveProfile();
                if (profile != null) {
                    if (ownerData.getIdentifier().equals(viewerData.getIdentifier())) {
                        profile.setOwnerClicks(profile.getOwnerClicks() + 1);
                    } else {
                        profile.setOtherClicks(profile.getOtherClicks() + 1);
                    }
                }
            }

            viewerData.setGlobalClicks(viewerData.getGlobalClicks() + 1);
            updateStats();
            updateDigitDisplay();

            if (viewerData.getSettings().isSoundEnabled(SettingType.SOUND_CLICKER)) {
                float vol = viewerData.getSettings().getVolume(SettingType.VOLUME_CLICKER);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, vol, 2.0f);
            }

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
    }

    private void updateStats() {
        String title = isVisiting
                ? ChatColor.GOLD + "" + ChatColor.BOLD + ownerData.getName() + "'s Stats"
                : ChatColor.GOLD + "" + ChatColor.BOLD + "Cookie Stats";
        Icon stats = GuiHelper.createIcon(Material.NETHER_STAR, title,
                "",
                ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCookies()),
                ChatColor.GRAY + "Total Earned: " + ChatColor.WHITE + FormatUtils.format(ownerData.getLifetimeCookiesEarned()),
                ChatColor.GRAY + "Earned This Prestige: " + ChatColor.WHITE + FormatUtils.format(ownerData.getTotalCookiesEarned()),
                "",
                ChatColor.GRAY + "Per Click: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCpc()),
                ChatColor.GRAY + "Per Second: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCps()),
                ChatColor.GRAY + "Realm Clicks: " + ChatColor.WHITE + FormatUtils.format(ownerData.getTimesClicked()),
                "",
                ChatColor.LIGHT_PURPLE + "Prestige: " + ChatColor.WHITE + ownerData.getPrestigeLevel(),
                ChatColor.LIGHT_PURPLE + "Aura: " + ChatColor.WHITE + FormatUtils.format(ownerData.getAura()),
                ChatColor.LIGHT_PURPLE + "Clicker Entropy: " + ChatColor.WHITE + FormatUtils.format(ownerData.getClickerEntropy()),
                "",
                ChatColor.AQUA + "Your Global Clicks: " + ChatColor.WHITE + FormatUtils.format(viewerData.getGlobalClicks()),
                "",
                ChatColor.YELLOW + "Click to toggle banner display");
        stats.onClick(e -> {
            showBanners = !showBanners;
            updateStats();
            updateDigitDisplay();
            updateServerBalance();
        });
        addItem(4, stats);
    }

    /**
     * Display the total server cookie balance as black banners at indexes 1-3 (left of info star).
     * Shows the sum of all loaded players' cookies.
     */
    private void updateServerBalance() {
        if (!showBanners) {
            Icon blackPane = GuiHelper.createIcon(Material.BLACK_STAINED_GLASS_PANE, " ");
            for (int i = 0; i < 4; i++) {
                addItem(i, blackPane);
            }
            return;
        }
        boolean simpleMode = LobbyClicker.getMainConfig().isSimpleMode();
        BigDecimal displayValue;
        String loreText;
        BannerChar.BannerColor baseColor;

        if (simpleMode) {
            displayValue = ownerData.getLifetimeCookiesEarned();
            loreText = ChatColor.GRAY + "Your Total Earned: " + ChatColor.WHITE + FormatUtils.format(displayValue);
            baseColor = BannerChar.BannerColor.BLUE;
        } else {
            displayValue = BigDecimal.ZERO;
            for (gg.drak.lobbyclicker.realm.RealmProfile profile : gg.drak.lobbyclicker.realm.ProfileManager.getAllLoadedProfiles()) {
                displayValue = displayValue.add(profile.getLifetimeCookiesEarned());
            }
            loreText = ChatColor.GRAY + "Server Total Earned: " + ChatColor.WHITE + FormatUtils.format(displayValue);
            baseColor = BannerChar.BannerColor.BLACK;
        }
        String[] serverDisplay = BannerUtil.parseBannerDisplay(displayValue);
        for (int i = 0; i < 4; i++) {
            addItem(i, BannerChar.of(serverDisplay[i], baseColor, BannerChar.BannerColor.WHITE).toIcon(loreText));
        }
    }

    private void updateDigitDisplay() {
        if (!showBanners) {
            Icon blackPane = GuiHelper.createIcon(Material.BLACK_STAINED_GLASS_PANE, " ");
            for (int i = 0; i < 4; i++) {
                addItem(5 + i, blackPane);
            }
            return;
        }
        boolean simpleMode = LobbyClicker.getMainConfig().isSimpleMode();
        BannerChar.BannerColor baseColor = simpleMode ? BannerChar.BannerColor.BLUE : BannerChar.BannerColor.RED;
        String cookieDisplay = ChatColor.GRAY + "Current Cookies: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCookies());
        String[] display = BannerUtil.parseBannerDisplay(ownerData.getCookies());
        for (int i = 0; i < 4; i++) {
            addItem(5 + i, BannerChar.of(display[i], baseColor, BannerChar.BannerColor.WHITE).toIcon(cookieDisplay));
        }
    }

    /**
     * Green glass pane below the cookie (index 31) showing click stats and visitors.
     */
    private void updateClickInfoPane() {
        boolean simpleMode = LobbyClicker.getMainConfig().isSimpleMode();
        long now = System.currentTimeMillis();
        BigDecimal gained5s = BigDecimal.ZERO, gained30s = BigDecimal.ZERO, gained5m = BigDecimal.ZERO, gained30m = BigDecimal.ZERO;

        for (ClickRecord r : getClickHistory()) {
            long age = now - r.timestamp;
            if (age <= 5_000) gained5s = gained5s.add(r.amount);
            if (age <= 30_000) gained30s = gained30s.add(r.amount);
            if (age <= 5 * 60_000) gained5m = gained5m.add(r.amount);
            if (age <= 30 * 60_000) gained30m = gained30m.add(r.amount);
        }

        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GREEN + "Cookies from clicks:");
        lore.add(ChatColor.GRAY + "  5s: " + ChatColor.WHITE + FormatUtils.format(gained5s));
        lore.add(ChatColor.GRAY + "  30s: " + ChatColor.WHITE + FormatUtils.format(gained30s));
        lore.add(ChatColor.GRAY + "  5m: " + ChatColor.WHITE + FormatUtils.format(gained5m));
        lore.add(ChatColor.GRAY + "  30m: " + ChatColor.WHITE + FormatUtils.format(gained30m));

        if (!simpleMode) {
            // Recent clickers (social feature)
            java.util.LinkedHashSet<String> recentClickers = new java.util.LinkedHashSet<>();
            for (ClickRecord r : getClickHistory()) {
                long age = now - r.timestamp;
                if (age <= 60_000 && recentClickers.size() < 5) {
                    if (!isVisiting || !r.clickerName.equals(ownerData.getName())) {
                        if (!(ownerData.getIdentifier().equals(viewerData.getIdentifier()) && r.clickerName.equals(ownerData.getName()))) {
                            recentClickers.add(r.clickerName);
                        }
                    }
                }
            }

            // Visitors (social feature)
            java.util.Set<String> viewerUuids = RealmManager.getViewers(ownerData.getIdentifier());
            java.util.List<String> visitorNames = new java.util.ArrayList<>();
            for (String vuuid : viewerUuids) {
                if (ownerData.getIdentifier().equals(viewerData.getIdentifier()) && vuuid.equals(viewerData.getIdentifier())) continue;
                String name = vuuid.substring(0, 8);
                java.util.Optional<gg.drak.lobbyclicker.data.PlayerData> pd = gg.drak.lobbyclicker.data.PlayerManager.getPlayer(vuuid);
                if (pd.isPresent()) name = pd.get().getName();
                else {
                    try { String n = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(vuuid)).getName(); if (n != null) name = n; } catch (Exception ignored) {}
                }
                visitorNames.add(name);
                if (visitorNames.size() >= 5) break;
            }

            if (!recentClickers.isEmpty()) {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Recent clickers (1m):");
                for (String name : recentClickers) {
                    lore.add(ChatColor.GRAY + "  " + ChatColor.WHITE + name);
                }
            }

            if (!visitorNames.isEmpty()) {
                lore.add("");
                lore.add(ChatColor.AQUA + "Realm visitors:");
                for (String name : visitorNames) {
                    lore.add(ChatColor.GRAY + "  " + ChatColor.WHITE + name);
                }
            }

            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to view all visitors");
        }

        Icon pane = GuiHelper.createIcon(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Activity",
                lore.toArray(new String[0]));
        if (!simpleMode) {
            pane.onClick(e -> {
                unregisterGui(player.getUniqueId());
                if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                new RealmViewersGui(player, viewerData).open();
            });
        }
        addItem(31, pane);
    }

    // --- Golden Cookie System ---

    private GoldenCookieState goldenState;

    private void initGoldenCookieTimers() {
        UUID ownerUuid = java.util.UUID.fromString(ownerData.getIdentifier());
        goldenState = GOLDEN_STATE.get(ownerUuid);
        if (goldenState == null) {
            goldenState = new GoldenCookieState();
            goldenState.nextSpawnCountdown = getWeightedSpawnDelay();
            goldenState.frenzyCountdown = 1800 + RANDOM.nextInt(901);
            goldenState.frenzyRemaining = -1;
            GOLDEN_STATE.put(ownerUuid, goldenState);
        }
    }

    /**
     * Generate a spawn delay (in seconds) between 30 and 300,
     * weighted quadratically toward shorter times.
     * Applies the golden cookie frequency multiplier from purchased upgrades.
     */
    private int getWeightedSpawnDelay() {
        double raw = Math.pow(RANDOM.nextDouble(), 2.0); // quadratic bias toward 0
        int baseDelay = 30 + (int) (raw * 270); // 30s to 300s
        double freqMult = ownerData.getEffectMultiplier(
                gg.drak.lobbyclicker.upgrades.ClickerUpgradeEffect.GOLDEN_FREQ_MULTIPLIER).doubleValue();
        double boosterFreqMult = gg.drak.lobbyclicker.boosters.BoosterManager.getMultiplier(
                ownerData.getIdentifier(), gg.drak.lobbyclicker.boosters.BoosterEffect.GOLDEN_FREQ).doubleValue();
        double questFreqMult = 1.0;
        gg.drak.lobbyclicker.realm.RealmProfile profile = ownerData.getActiveProfile();
        if (profile != null) {
            questFreqMult = profile.getQuestBonusMultiplier(gg.drak.lobbyclicker.quests.QuestEffect.GOLDEN_FREQ_PERCENT).doubleValue();
        }
        return Math.max(5, (int) (baseDelay / freqMult / boosterFreqMult / questFreqMult));
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

        if (goldenState.frenzyRemaining > 0) {
            goldenState.frenzyRemaining--;
            goldenState.nextSpawnCountdown--;
            if (goldenState.nextSpawnCountdown <= 0) {
                spawnGoldenCookie(player);
                goldenState.nextSpawnCountdown = goldenState.frenzySpawnInterval;
            }
            if (goldenState.frenzyRemaining <= 0) {
                goldenState.frenzyRemaining = -1;
                goldenState.nextSpawnCountdown = getWeightedSpawnDelay();
                goldenState.frenzyCountdown = 1800 + RANDOM.nextInt(901);
                player.sendMessage(ChatColor.GOLD + "Cookie Frenzy has ended!");
            }
        } else {
            goldenState.nextSpawnCountdown--;
            goldenState.frenzyCountdown--;
            if (goldenState.nextSpawnCountdown <= 0) {
                spawnGoldenCookie(player);
                goldenState.nextSpawnCountdown = getWeightedSpawnDelay();
            }
            if (goldenState.frenzyCountdown <= 0) {
                goldenState.frenzyRemaining = 300;
                goldenState.frenzySpawnInterval = 10 + RANDOM.nextInt(21);
                goldenState.nextSpawnCountdown = goldenState.frenzySpawnInterval;
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
        savedGoldenSlotItem = getInventory().getItem(slot);

        // Reward: entropy * random multiplier (0.1x to 2.0x), scaled by reward upgrades
        BigDecimal multiplier = BigDecimal.valueOf(0.1 + RANDOM.nextDouble() * 1.9);
        BigDecimal bonus = ownerData.getClickerEntropy().multiply(multiplier);
        BigDecimal rewardMult = ownerData.getEffectMultiplier(
                gg.drak.lobbyclicker.upgrades.ClickerUpgradeEffect.GOLDEN_REWARD_MULTIPLIER);
        BigDecimal boosterRewardMult = gg.drak.lobbyclicker.boosters.BoosterManager.getMultiplier(
                ownerData.getIdentifier(), gg.drak.lobbyclicker.boosters.BoosterEffect.GOLDEN_REWARD);
        bonus = bonus.multiply(rewardMult).multiply(boosterRewardMult);
        final BigDecimal finalBonus = bonus;

        // Tier based on raw multiplier: top 15% = block, middle 35% = ingot, bottom 50% = nugget
        double normalized = multiplier.doubleValue() / 2.0;
        Material material;
        String tierName;
        if (normalized >= 0.85) {
            material = Material.GOLD_BLOCK;
            tierName = "Jackpot";
        } else if (normalized >= 0.50) {
            material = Material.GOLD_INGOT;
            tierName = "Golden Cookie";
        } else {
            material = Material.GOLD_NUGGET;
            tierName = "Lucky Cookie";
        }

        // Apply duration multiplier from upgrades
        // 10 second base duration, scaled by duration upgrades
        double durMult = ownerData.getEffectMultiplier(
                gg.drak.lobbyclicker.upgrades.ClickerUpgradeEffect.GOLDEN_DURATION_MULTIPLIER).doubleValue();
        goldenCookieTicksLeft = Math.max(2, (int) (10 * durMult));

        Icon golden = GuiHelper.createIcon(material,
                ChatColor.GOLD + "" + ChatColor.BOLD + tierName + "!",
                "", ChatColor.YELLOW + "Click for +" + FormatUtils.format(finalBonus) + " cookies!",
                ChatColor.GRAY + "Hurry, it won't last long!");
        golden.onClick(e -> {
            if (goldenCookieSlot < 0) return;
            ownerData.addCookies(finalBonus);
            gg.drak.lobbyclicker.realm.RealmProfile gcProfile = ownerData.getActiveProfile();
            if (gcProfile != null) gcProfile.setGoldenCookiesCollected(gcProfile.getGoldenCookiesCollected() + 1);
            int clickedSlot = goldenCookieSlot;
            goldenCookieSlot = -1;
            goldenCookieTicksLeft = 0;
            getInventory().setItem(clickedSlot, savedGoldenSlotItem);
            savedGoldenSlotItem = null;
            updateStats();
            updateDigitDisplay();
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + tierName + "! " +
                    ChatColor.YELLOW + "+" + FormatUtils.format(finalBonus) + " cookies");
            if (viewerData.getSettings().isSoundEnabled(SettingType.SOUND_CLICKER)) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            // Broadcast to all other players in this realm
            String broadcastMsg = ChatColor.GOLD + viewerData.getName() + ChatColor.YELLOW +
                    " picked up a golden cookie worth " + ChatColor.GOLD + FormatUtils.format(finalBonus) + ChatColor.YELLOW + " cookies!";
            // Notify owner if different from clicker
            if (!ownerData.getIdentifier().equals(viewerData.getIdentifier())) {
                ownerData.asPlayer().ifPresent(op -> op.sendMessage(broadcastMsg));
            }
            // Notify all realm viewers except the clicker
            for (String vuuid : RealmManager.getViewers(ownerData.getIdentifier())) {
                if (vuuid.equals(viewerData.getIdentifier())) continue;
                Player vp = Bukkit.getPlayer(java.util.UUID.fromString(vuuid));
                if (vp != null) vp.sendMessage(broadcastMsg);
            }
        });
        addItem(slot, golden);
        if (viewerData.getSettings().isSoundEnabled(SettingType.SOUND_CLICKER)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.5f);
        }
    }

    private void removeGoldenCookie() {
        if (goldenCookieSlot >= 0) {
            getInventory().setItem(goldenCookieSlot, savedGoldenSlotItem);
            savedGoldenSlotItem = null;
            goldenCookieSlot = -1;
            goldenCookieTicksLeft = 0;
        }
    }
}
