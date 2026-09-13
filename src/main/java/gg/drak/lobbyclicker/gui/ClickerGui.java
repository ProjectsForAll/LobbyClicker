package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.golden.GoldenCookieHolder;
import gg.drak.lobbyclicker.gui.monitor.SimpleGuiMonitor;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.realm.RealmRole;
import gg.drak.lobbyclicker.redis.RedisManager;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import gg.drak.lobbyclicker.settings.SettingType;
import gg.drak.lobbyclicker.social.RealmManager;
import gg.drak.lobbyclicker.upgrades.ClickerUpgradeEffect;
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
import gg.drak.lobbyclicker.utils.FoliaScheduler;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ClickerGui extends SimpleGuiMonitor {
    private final PlayerData viewerData;
    private final PlayerData ownerData;
    private final boolean isVisiting;

    /** Grants its holder auto-collection of golden cookies on any realm they click. */
    public static final String GOLDEN_AUTO_COLLECT_PERMISSION = "lobbyclicker.golden-cookie.auto-collect";

    private static final ConcurrentHashMap<UUID, ClickerGui> OPEN_GUIS = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<UUID, ClickerGui> getOpenGuis() { return OPEN_GUIS; }
    public static void registerGui(UUID uuid, ClickerGui gui) { OPEN_GUIS.put(uuid, gui); }
    public static void unregisterGui(UUID uuid) { OPEN_GUIS.remove(uuid); }

    private boolean showBanners = true;

    /** The slot this GUI currently has a golden cookie drawn in, or -1. */
    private int renderedGoldenSlot = -1;

    private FoliaScheduler.PluginTask lifecycleTask;

    public Player getViewer() { return player; }

    public static void clearGoldenState(UUID uuid) {
        GoldenCookieHolder.remove(uuid);
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
            Icon mail = ClickerGuiHelper.createIcon(Material.WRITABLE_BOOK,
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
            Icon friends = ClickerGuiHelper.playerHead(friendHeadUuid,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "Friends",
                    "", ChatColor.GRAY + "View your friends list");
            friends.onClick(e -> {
                unregisterGui(player.getUniqueId());
                if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                new FriendsListGui(player, viewerData, 0).open();
            });
            addItem(18, friends);

            gg.drak.lobbyclicker.realm.RealmProfile achProfile = viewerData.getActiveProfile();
            int achCompleted = achProfile == null ? 0 : gg.drak.lobbyclicker.achievements.AchievementManager.normalUnlocked(achProfile);
            int achTotal = 0;
            for (gg.drak.lobbyclicker.achievements.Achievement a : gg.drak.lobbyclicker.achievements.AchievementCatalog.all()) {
                if (!a.isShadow()) achTotal++;
            }
            Icon achievements = ClickerGuiHelper.createIcon(Material.WRITTEN_BOOK,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "Achievements",
                    "", ChatColor.GRAY + "Progress: " + achCompleted + "/" + achTotal,
                    "", ChatColor.YELLOW + "Click to view achievements");
            achievements.onClick(e -> {
                unregisterGui(player.getUniqueId());
                if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                new AchievementsGui(player, viewerData).open();
            });
            addItem(17, achievements);
        }

        // === BOTTOM ROW ACTION BAR ===
        int b = (getSize() / 9 - 1) * 9; // bottom row start index

        boolean showSocial = !simpleMode && socialEnabled;

        // Slot 1 (b+0): Social button — hidden when social is disabled; settings moves here
        if (showSocial) {
            Icon social = ClickerGuiHelper.playerHead(player,
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
        Icon settings = ClickerGuiHelper.createIcon(Material.COMPARATOR,
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
            Icon shop = ClickerGuiHelper.createIcon(Material.CHEST,
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
                Icon shop = ClickerGuiHelper.createIcon(Material.CHEST,
                        ChatColor.GREEN + "" + ChatColor.BOLD + "Shop",
                        "", ChatColor.DARK_GREEN + "Role: " + viewerRole.getDisplayName());
                shop.onClick(e -> {
                    unregisterGui(player.getUniqueId());
                    new ShopGui(player, viewerData, ownerData).open();
                });
                addItem(b + 3, shop);
            } else {
                addItem(b + 3, ClickerGuiHelper.createIcon(Material.IRON_BARS,
                        ChatColor.GRAY + "" + ChatColor.BOLD + "Locked",
                        "", ChatColor.GRAY + "Need Gardener role"));
            }

            // Register as viewer
            RealmManager.addViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
        }

        // Slot 6 (b+5): Leaderboard button
        Icon leaderboard = ClickerGuiHelper.createIcon(Material.OAK_SIGN,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Leaderboard",
                "", ChatColor.GRAY + "Top cookie earners");
        leaderboard.onClick(e -> {
            unregisterGui(player.getUniqueId());
            if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
            new LeaderboardGui(player, viewerData, isVisiting ? ownerData : null).open();
        });
        addItem(b + 5, leaderboard);

        // Slot 7 (b+6): Prestige button
        Icon prestige = ClickerGuiHelper.createIcon(Material.BEACON,
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
            Icon profiles = ClickerGuiHelper.createIcon(Material.BOOK,
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
            Icon myRealm = ClickerGuiHelper.createIcon(Material.COOKIE,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "My Realm",
                    "", ChatColor.GRAY + "Return to your realm");
            myRealm.onClick(e -> {
                unregisterGui(player.getUniqueId());
                RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                new ClickerGui(player, viewerData).open();
            });
            addItem(b + 8, myRealm);
        } else {
            Icon close = ClickerGuiHelper.createIcon(Material.BARRIER, ChatColor.RED + "Close");
            close.onClick(e -> {
                unregisterGui(player.getUniqueId());
                player.closeInventory();
            });
            addItem(b + 8, close);
        }

        // Register for global refresh before the holder looks for viewers
        registerGui(player.getUniqueId(), this);

        GoldenCookieHolder holder = goldenHolder();
        holder.ensureTaskRunning();
        renderGoldenCookie();
        startLifecycleTask(player);
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
        Icon cookie = ClickerGuiHelper.createIcon(Material.COOKIE,
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

            // Record click for the green info pane
            BigDecimal clickAmount = ownerData.getCpc();
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
                    profile.addCookiesFromClicks(clickAmount);
                    if (ownerData.getIdentifier().equals(viewerData.getIdentifier())) {
                        profile.setOwnerClicks(profile.getOwnerClicks() + 1);
                    } else {
                        profile.setOtherClicks(profile.getOtherClicks() + 1);
                    }
                    gg.drak.lobbyclicker.achievements.AchievementManager.check(ownerData);
                }
            }

            viewerData.setGlobalClicks(viewerData.getGlobalClicks() + 1);
            updateStats();
            updateDigitDisplay();

            // A remote owner's PlayerData here is a stale copy — clicks reach them over Redis
            // rather than being credited locally, and there is no such channel for a golden
            // cookie claim, so auto-collect stays on realms whose owner this server owns.
            if (!ownerIsRemote && hasGoldenAutoCollect()) {
                goldenHolder().autoCollect(player, viewerData, ownerData);
            }

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
        Icon stats = ClickerGuiHelper.createIcon(Material.NETHER_STAR, title,
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
            Icon blackPane = ClickerGuiHelper.createIcon(Material.BLACK_STAINED_GLASS_PANE, " ");
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
            Icon blackPane = ClickerGuiHelper.createIcon(Material.BLACK_STAINED_GLASS_PANE, " ");
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

        Icon pane = ClickerGuiHelper.createIcon(Material.LIME_STAINED_GLASS_PANE,
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

    /**
     * Golden cookie state lives in {@link GoldenCookieHolder}, keyed by realm owner, so a
     * spawned cookie survives this GUI closing and reopening and is shared by every viewer.
     */
    private GoldenCookieHolder goldenHolder() {
        return GoldenCookieHolder.getOrCreate(ownerData.getIdentifier());
    }

    /**
     * Whether clicking the cookie should also sweep up a waiting golden cookie. The
     * permission travels with the clicker; the Cookie Magnet upgrade belongs to the realm,
     * so it applies to everyone clicking there — matching the other GOLDEN_* effects, which
     * all read from the owner's profile.
     */
    private boolean hasGoldenAutoCollect() {
        return player.hasPermission(GOLDEN_AUTO_COLLECT_PERMISSION)
                || ownerData.hasEffect(ClickerUpgradeEffect.GOLDEN_AUTO_COLLECT);
    }

    /** Draw the realm's active golden cookie, if any, into its slot. */
    public void renderGoldenCookie() {
        GoldenCookieHolder holder = goldenHolder();
        Icon icon = holder.buildIcon(viewerData, ownerData);
        if (icon == null) {
            clearGoldenCookieSlot();
            return;
        }
        int slot = holder.getSlot();
        if (renderedGoldenSlot >= 0 && renderedGoldenSlot != slot) clearGoldenCookieSlot();
        renderedGoldenSlot = slot;
        addItem(slot, icon);
    }

    /**
     * Blank the golden cookie slot; interior slots of the monitor border are empty anyway.
     * The icon registration has to go as well as the item: click dispatch looks handlers up
     * by slot alone, so a registration left behind would keep accepting claims on thin air.
     *
     * <p>Dropping the registration also drops the AIR filler the border put there, leaving the
     * slot unregistered and so unpainted on redraw. That is right only while the interior
     * filler is AIR; give this slot its filler back if that ever changes.
     */
    public void clearGoldenCookieSlot() {
        if (renderedGoldenSlot < 0) return;
        getItems().remove(renderedGoldenSlot);
        getInventory().setItem(renderedGoldenSlot, null);
        renderedGoldenSlot = -1;
    }

    /**
     * Drop this GUI's registrations once the player is no longer looking at it. The golden
     * cookie timers belong to the holder and keep running without this task.
     */
    private void startLifecycleTask(Player player) {
        lifecycleTask = FoliaScheduler.runForEntityTimer(player, LobbyClicker.getInstance(), () -> {
            if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(getInventory())) {
                stopLifecycleTask();
                unregisterGui(player.getUniqueId());
                if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                return;
            }
            renderGoldenCookie();
        }, 20L, 20L);
    }

    private void stopLifecycleTask() {
        if (lifecycleTask != null && !lifecycleTask.isCancelled()) {
            lifecycleTask.cancel();
            lifecycleTask = null;
        }
    }
}
