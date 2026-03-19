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
    private int nextSpawnCountdown;
    private int frenzyCountdown;
    private int frenzyRemaining = -1;
    private int frenzySpawnInterval;

    // Click tracking for the green info pane
    private final java.util.Deque<ClickRecord> recentClicks = new java.util.concurrent.ConcurrentLinkedDeque<>();

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

        fillMonitorBorder();

        updateStats();
        updateDigitDisplay();
        updateServerBalance();
        updateClickInfoPane();
        addCookieItem(player);

        // Mail icon (owner only, 2nd row 1st slot = index 9)
        if (!isVisiting) {
            Icon mail = GuiHelper.createIcon(Material.WRITABLE_BOOK,
                    ChatColor.YELLOW + "" + ChatColor.BOLD + "Mail",
                    "",
                    ChatColor.GRAY + "See:",
                    ChatColor.WHITE + " - Incoming Friend Requests",
                    ChatColor.WHITE + " - Payment Requests",
                    ChatColor.WHITE + " - Gambling Requests");
            mail.onClick(e -> {
                stopGoldenCookieTask();
                unregisterGui(player.getUniqueId());
                new MailGui(player, viewerData).open();
            });
            addItem(9, mail);
        }

        // === BOTTOM ROW ACTION BAR ===
        int b = (getSize() / 9 - 1) * 9; // bottom row start index

        // Slot 1 (b+0): Social button
        Icon social = GuiHelper.playerHead(player,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Social",
                "", ChatColor.GRAY + "Friends, realms, players");
        social.onClick(e -> {
            stopGoldenCookieTask();
            unregisterGui(player.getUniqueId());
            if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
            new SocialMainGui(player, viewerData, isVisiting ? ownerData : null).open();
        });
        addItem(b, social);

        // Slot 2 (b+1): Settings button
        Icon settings = GuiHelper.createIcon(Material.COMPARATOR,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Settings",
                "", ChatColor.GRAY + "Configure preferences");
        settings.onClick(e -> {
            stopGoldenCookieTask();
            new SettingsMainGui(player, viewerData, isVisiting ? ownerData : null).open();
        });
        addItem(b + 1, settings);

        // Slot 4 (b+3): Shop button
        if (!isVisiting) {
            Icon shop = GuiHelper.createIcon(Material.CHEST,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "Shop",
                    "", ChatColor.GRAY + "Buy helpers and upgrades!");
            shop.onClick(e -> {
                stopGoldenCookieTask();
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
                    stopGoldenCookieTask();
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
            stopGoldenCookieTask();
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
            stopGoldenCookieTask();
            unregisterGui(player.getUniqueId());
            if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
            new PrestigeGui(player, viewerData, isVisiting ? ownerData : null).open();
        });
        addItem(b + 6, prestige);

        // Slot 8 (b+7): Profiles button
        Icon profiles = GuiHelper.createIcon(Material.BOOK,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Profiles",
                "", ChatColor.GRAY + "Switch realm profiles");
        profiles.onClick(e -> {
            stopGoldenCookieTask();
            unregisterGui(player.getUniqueId());
            if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
            new ProfileSelectorGui(player, viewerData, isVisiting ? ownerData : null).open();
        });
        addItem(b + 7, profiles);

        // Slot 9 (b+8): Close or My Realm button
        if (isVisiting) {
            Icon myRealm = GuiHelper.createIcon(Material.COOKIE,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "My Realm",
                    "", ChatColor.GRAY + "Return to your realm");
            myRealm.onClick(e -> {
                stopGoldenCookieTask();
                unregisterGui(player.getUniqueId());
                RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
                new ClickerGui(player, viewerData).open();
            });
            addItem(b + 8, myRealm);
        } else {
            Icon close = GuiHelper.createIcon(Material.BARRIER, ChatColor.RED + "Close");
            close.onClick(e -> {
                stopGoldenCookieTask();
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
                        "", ChatColor.YELLOW + "Click to earn cookies!",
                        ChatColor.GRAY + "Per click: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCpc())));
                cookieItem.setItemMeta(meta);
            }
        }
    }

    private void addCookieItem(Player player) {
        // Create cookie icon WITHOUT a click handler — clicking is handled by onCookieClick()
        Icon cookie = GuiHelper.createIcon(Material.COOKIE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Cookie",
                "", ChatColor.YELLOW + "Click to earn cookies!",
                ChatColor.GRAY + "Per click: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCpc()));
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
            recentClicks.addFirst(new ClickRecord(viewerData.getName(), clickAmount));
            long cutoff = now - 30 * 60 * 1000;
            recentClicks.removeIf(r -> r.timestamp < cutoff);

            // Check if the owner is on a remote server (OBO player)
            boolean ownerIsRemote = isVisiting && !ownerData.isOnline()
                    && LobbyClicker.getRedisManager() != null
                    && LobbyClicker.getRedisManager().isPlayerOnlineRemotely(ownerData.getIdentifier());

            if (ownerIsRemote) {
                RedisSyncHandler.publishClick(ownerData.getIdentifier(), viewerData.getIdentifier(), viewerData.getName());
            } else {
                ownerData.addCookies(ownerData.getCpc());
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
                ChatColor.AQUA + "Your Global Clicks: " + ChatColor.WHITE + FormatUtils.format(viewerData.getGlobalClicks()));
        addItem(4, stats);
    }

    /**
     * Display the total server cookie balance as black banners at indexes 1-3 (left of info star).
     * Shows the sum of all loaded players' cookies.
     */
    private void updateServerBalance() {
        BigDecimal totalServerCookies = BigDecimal.ZERO;
        for (gg.drak.lobbyclicker.realm.RealmProfile profile : gg.drak.lobbyclicker.realm.ProfileManager.getAllLoadedProfiles()) {
            totalServerCookies = totalServerCookies.add(profile.getLifetimeCookiesEarned());
        }
        String serverLore = ChatColor.GRAY + "Server Total Earned: " + ChatColor.WHITE + FormatUtils.format(totalServerCookies);
        String[] serverDisplay = BannerUtil.parseBannerDisplay(totalServerCookies);
        // Show all 4 chars at indexes 0-3 (to the left of the info star at index 4)
        for (int i = 0; i < 4; i++) {
            addItem(i, BannerChar.of(serverDisplay[i], BannerChar.BannerColor.BLACK, BannerChar.BannerColor.WHITE).toIcon(serverLore));
        }
    }

    private void updateDigitDisplay() {
        String cookieDisplay = ChatColor.GRAY + "Current Cookies: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCookies());
        String[] display = BannerUtil.parseBannerDisplay(ownerData.getCookies());
        for (int i = 0; i < 4; i++) {
            addItem(5 + i, BannerChar.of(display[i], BannerChar.BannerColor.RED, BannerChar.BannerColor.WHITE).toIcon(cookieDisplay));
        }
    }

    /**
     * Green glass pane below the cookie (index 31) showing click stats and visitors.
     */
    private void updateClickInfoPane() {
        long now = System.currentTimeMillis();
        BigDecimal gained5s = BigDecimal.ZERO, gained30s = BigDecimal.ZERO, gained5m = BigDecimal.ZERO, gained30m = BigDecimal.ZERO;
        java.util.LinkedHashSet<String> recentClickers = new java.util.LinkedHashSet<>();

        for (ClickRecord r : recentClicks) {
            long age = now - r.timestamp;
            if (age <= 5_000) gained5s = gained5s.add(r.amount);
            if (age <= 30_000) gained30s = gained30s.add(r.amount);
            if (age <= 5 * 60_000) gained5m = gained5m.add(r.amount);
            if (age <= 30 * 60_000) gained30m = gained30m.add(r.amount);

            // Track clickers from last minute (exclude owner if owner=viewer)
            if (age <= 60_000 && recentClickers.size() < 5) {
                if (!isVisiting || !r.clickerName.equals(ownerData.getName())) {
                    // Don't include owner if owner = gui viewer
                    if (!(ownerData.getIdentifier().equals(viewerData.getIdentifier()) && r.clickerName.equals(ownerData.getName()))) {
                        recentClickers.add(r.clickerName);
                    }
                }
            }
        }

        // Visitors (from RealmManager)
        java.util.Set<String> viewerUuids = RealmManager.getViewers(ownerData.getIdentifier());
        java.util.List<String> visitorNames = new java.util.ArrayList<>();
        for (String vuuid : viewerUuids) {
            // Don't include owner if owner = gui viewer
            if (ownerData.getIdentifier().equals(viewerData.getIdentifier()) && vuuid.equals(viewerData.getIdentifier())) continue;
            String name = vuuid.substring(0, 8);
            gg.drak.lobbyclicker.data.PlayerManager.getPlayer(vuuid).ifPresent(d -> {});
            java.util.Optional<gg.drak.lobbyclicker.data.PlayerData> pd = gg.drak.lobbyclicker.data.PlayerManager.getPlayer(vuuid);
            if (pd.isPresent()) name = pd.get().getName();
            else {
                try { String n = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(vuuid)).getName(); if (n != null) name = n; } catch (Exception ignored) {}
            }
            visitorNames.add(name);
            if (visitorNames.size() >= 5) break;
        }

        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GREEN + "Cookies from clicks:");
        lore.add(ChatColor.GRAY + "  5s: " + ChatColor.WHITE + FormatUtils.format(gained5s));
        lore.add(ChatColor.GRAY + "  30s: " + ChatColor.WHITE + FormatUtils.format(gained30s));
        lore.add(ChatColor.GRAY + "  5m: " + ChatColor.WHITE + FormatUtils.format(gained5m));
        lore.add(ChatColor.GRAY + "  30m: " + ChatColor.WHITE + FormatUtils.format(gained30m));

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

        Icon pane = GuiHelper.createIcon(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Activity",
                lore.toArray(new String[0]));
        pane.onClick(e -> {
            stopGoldenCookieTask();
            unregisterGui(player.getUniqueId());
            if (isVisiting) RealmManager.removeViewer(ownerData.getIdentifier(), viewerData.getIdentifier());
            new RealmViewersGui(player, viewerData).open();
        });
        addItem(31, pane);
    }

    // --- Golden Cookie System ---

    private void initGoldenCookieTimers() {
        nextSpawnCountdown = getWeightedSpawnDelay();
        frenzyCountdown = 1800 + RANDOM.nextInt(901);
        frenzyRemaining = -1;
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
        return Math.max(5, (int) (baseDelay / freqMult));
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
                nextSpawnCountdown = getWeightedSpawnDelay();
                frenzyCountdown = 1800 + RANDOM.nextInt(901);
                player.sendMessage(ChatColor.GOLD + "Cookie Frenzy has ended!");
            }
        } else {
            nextSpawnCountdown--;
            frenzyCountdown--;
            if (nextSpawnCountdown <= 0) {
                spawnGoldenCookie(player);
                nextSpawnCountdown = getWeightedSpawnDelay();
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
        savedGoldenSlotItem = getInventory().getItem(slot);

        // Reward: entropy * random multiplier (0.1x to 2.0x), scaled by reward upgrades
        BigDecimal multiplier = BigDecimal.valueOf(0.1 + RANDOM.nextDouble() * 1.9);
        BigDecimal bonus = ownerData.getClickerEntropy().multiply(multiplier);
        BigDecimal rewardMult = ownerData.getEffectMultiplier(
                gg.drak.lobbyclicker.upgrades.ClickerUpgradeEffect.GOLDEN_REWARD_MULTIPLIER);
        bonus = bonus.multiply(rewardMult);
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
