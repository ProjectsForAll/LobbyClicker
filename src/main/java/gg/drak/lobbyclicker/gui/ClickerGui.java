package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Gui;
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

import java.util.Arrays;
import java.util.Random;

public class ClickerGui extends Gui {
    private final PlayerData data;

    private static final int[] GOLDEN_COOKIE_SLOTS = {
            0, 1, 2, 3,
            9, 10, 11, 12,
            18, 19, 20, 21,
            27, 28, 29, 30,
            36, 37, 38, 39,
            45, 47
    };

    private static final Random RANDOM = new Random();
    private int goldenCookieSlot = -1;
    private int goldenCookieTicksLeft = 0;
    private BukkitTask goldenCookieTask;

    public ClickerGui(Player player, PlayerData data) {
        super(player, "clicker-main", ChatColor.GOLD + "" + ChatColor.BOLD + "Cookie Clicker", 6);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();

        // Fill background
        fillGui(createIcon(Material.GRAY_STAINED_GLASS_PANE, " ", null));

        // Stats display + digit display
        updateStats();
        updateDigitDisplay();

        // Cookie - the main clickable item
        addCookieItem(player);

        // Sound toggle button
        updateSoundToggle(player);

        // Upgrades button
        Icon upgrades = createIcon(Material.CHEST, ChatColor.GREEN + "" + ChatColor.BOLD + "Upgrades",
                new String[]{
                        "",
                        ChatColor.GRAY + "Buy upgrades to earn",
                        ChatColor.GRAY + "more cookies!",
                        "",
                        ChatColor.YELLOW + "Click to open"
                });
        upgrades.onClick(e -> {
            stopGoldenCookieTask();
            new UpgradeGui(player, data).open();
        });
        addItem(48, upgrades);

        // Leaderboard button
        Icon leaderboard = createIcon(Material.OAK_SIGN, ChatColor.AQUA + "" + ChatColor.BOLD + "Leaderboard",
                new String[]{
                        "",
                        ChatColor.GRAY + "See the top cookie earners!",
                        "",
                        ChatColor.YELLOW + "Click to view"
                });
        leaderboard.onClick(e -> {
            stopGoldenCookieTask();
            new LeaderboardGui(player, data).open();
        });
        addItem(50, leaderboard);

        // Close button
        Icon close = createIcon(Material.BARRIER, ChatColor.RED + "Close", new String[]{});
        close.onClick(e -> {
            stopGoldenCookieTask();
            player.closeInventory();
        });
        addItem(53, close);

        // Start golden cookie spawner
        startGoldenCookieTask(player);
    }

    private void addCookieItem(Player player) {
        Icon cookie = createIcon(Material.COOKIE, ChatColor.GOLD + "" + ChatColor.BOLD + "Cookie",
                new String[]{
                        "",
                        ChatColor.YELLOW + "Click to earn cookies!",
                        ChatColor.GRAY + "Per click: " + ChatColor.WHITE + FormatUtils.format(data.getCpc())
                });
        cookie.onClick(e -> {
            data.addCookies(data.getCpc());
            updateStats();
            updateDigitDisplay();
            addCookieItem(player);
            if (data.isSoundEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 2.0f);
            }
        });
        addItem(22, cookie);
    }

    private void updateStats() {
        Icon stats = createIcon(Material.NETHER_STAR, ChatColor.GOLD + "" + ChatColor.BOLD + "Cookie Stats",
                new String[]{
                        "",
                        ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(data.getCookies()),
                        ChatColor.GRAY + "Total Earned: " + ChatColor.WHITE + FormatUtils.format(data.getTotalCookiesEarned()),
                        "",
                        ChatColor.GRAY + "Per Click: " + ChatColor.WHITE + FormatUtils.format(data.getCpc()),
                        ChatColor.GRAY + "Per Second: " + ChatColor.WHITE + FormatUtils.format(data.getCps()),
                });
        addItem(4, stats);
    }

    private void updateDigitDisplay() {
        long cookieCount = (long) data.getCookies();
        String digits = String.valueOf(cookieCount);

        while (digits.length() < 4) {
            digits = "0" + digits;
        }

        for (int i = 0; i < 4; i++) {
            char digit = digits.charAt(i);
            int digitValue = digit - '0';
            Material pane = digitValue > 0 ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            addItem(5 + i, createIcon(pane, ChatColor.WHITE + "" + digit, null));
        }
    }

    private void updateSoundToggle(Player player) {
        boolean enabled = data.isSoundEnabled();
        Material mat = enabled ? Material.NOTE_BLOCK : Material.COBWEB;
        String status = enabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF";
        Icon toggle = createIcon(mat, ChatColor.YELLOW + "" + ChatColor.BOLD + "Sound: " + status,
                new String[]{
                        "",
                        ChatColor.GRAY + "Click to toggle click sound",
                });
        toggle.onClick(e -> {
            data.setSoundEnabled(!data.isSoundEnabled());
            updateSoundToggle(player);
            if (data.isSoundEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 2.0f);
            }
        });
        addItem(46, toggle);
    }

    // --- Golden Cookie ---

    private void startGoldenCookieTask(Player player) {
        goldenCookieTask = Bukkit.getScheduler().runTaskTimer(LobbyClicker.getInstance(), () -> {
            if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(getInventory())) {
                stopGoldenCookieTask();
                return;
            }

            if (goldenCookieSlot >= 0) {
                goldenCookieTicksLeft--;
                if (goldenCookieTicksLeft <= 0) {
                    removeGoldenCookie();
                }
                return;
            }

            // ~8% chance per second = roughly one every 12 seconds
            if (RANDOM.nextDouble() < 0.08) {
                spawnGoldenCookie(player);
            }
        }, 20L, 20L);
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

        double multiplier = 0.5 + RANDOM.nextDouble() * 9.5; // 50% to 1000%
        double bonus = data.getCookies() * multiplier;

        Icon golden = createIcon(Material.GOLDEN_APPLE, ChatColor.GOLD + "" + ChatColor.BOLD + "Golden Cookie!",
                new String[]{
                        "",
                        ChatColor.YELLOW + "Click for a cookie bonus!",
                        ChatColor.GRAY + "Hurry, it won't last long!"
                });
        golden.onClick(e -> {
            if (goldenCookieSlot < 0) return; // already collected or expired

            data.addCookies(bonus);
            int slot2 = goldenCookieSlot;
            goldenCookieSlot = -1;
            goldenCookieTicksLeft = 0;

            // Restore gray glass
            addItem(slot2, createIcon(Material.GRAY_STAINED_GLASS_PANE, " ", null));

            updateStats();
            updateDigitDisplay();
            addCookieItem(player);

            int percent = (int) (multiplier * 100);
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Golden Cookie! " +
                    ChatColor.YELLOW + "+" + FormatUtils.format(bonus) + " cookies " +
                    ChatColor.GRAY + "(" + percent + "% of your cookies)");

            if (data.isSoundEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        });
        addItem(slot, golden);

        if (data.isSoundEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.5f);
        }
    }

    private void removeGoldenCookie() {
        if (goldenCookieSlot >= 0) {
            addItem(goldenCookieSlot, createIcon(Material.GRAY_STAINED_GLASS_PANE, " ", null));
            goldenCookieSlot = -1;
            goldenCookieTicksLeft = 0;
        }
    }

    private static Icon createIcon(Material material, String name, String[] lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return new Icon(item);
    }
}
