package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.LeaderboardCache;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import org.bukkit.scheduler.BukkitTask;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Leaderboard showing each player's selected realm and highest-value realm.
 * If selected == highest, only one entry is shown.
 */
public class LeaderboardGui extends PaginationMonitor {
    private final PlayerData data;
    private final PlayerData realmOwner;

    private static final ConcurrentHashMap<UUID, LeaderboardGui> OPEN_GUIS = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<UUID, LeaderboardGui> getOpenGuis() { return OPEN_GUIS; }
    private BukkitTask refreshTask;

    public LeaderboardGui(Player player, PlayerData data) {
        this(player, data, 0, null);
    }

    public LeaderboardGui(Player player, PlayerData data, PlayerData realmOwner) {
        this(player, data, 0, realmOwner);
    }

    public LeaderboardGui(Player player, PlayerData data, int page) {
        this(player, data, page, null);
    }

    public LeaderboardGui(Player player, PlayerData data, int page, PlayerData realmOwner) {
        super(player, "clicker-leaderboard", MonitorStyle.title("aqua", "Leaderboard"), page);
        this.data = data;
        this.realmOwner = realmOwner;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        LeaderboardCache.refreshAsync();
        buildDisplay();
        OPEN_GUIS.put(player.getUniqueId(), this);

        // Refresh display every 1 second while open
        refreshTask = Bukkit.getScheduler().runTaskTimer(
                gg.drak.lobbyclicker.LobbyClicker.getInstance(), () -> {
                    if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(getInventory())) {
                        stopRefreshTask();
                        OPEN_GUIS.remove(player.getUniqueId());
                        return;
                    }
                    buildDisplay();
                }, 20L, 20L); // 1 second interval
    }

    private void stopRefreshTask() {
        if (refreshTask != null && !refreshTask.isCancelled()) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    public void refreshDisplay() {
        if (player == null || !player.isOnline()) return;
        buildDisplay();
    }

    private void buildDisplay() {
        setPlayerContext(data, realmOwner);
        fillMonitorBorder();
        buildStandardActionBar(p -> {
            stopRefreshTask();
            OPEN_GUIS.remove(player.getUniqueId());
            new ClickerGui(p, data).open();
        });

        // Build deduplicated entries: per player, show selected realm + highest realm (if different)
        List<LeaderboardCache.LeaderboardEntry> allEntries = LeaderboardCache.getLeaderboard();

        // Group by player UUID, keep the highest and the selected
        Map<String, List<LeaderboardCache.LeaderboardEntry>> byPlayer = new LinkedHashMap<>();
        for (LeaderboardCache.LeaderboardEntry entry : allEntries) {
            byPlayer.computeIfAbsent(entry.getPlayerUuid(), k -> new ArrayList<>()).add(entry);
        }

        // Build display list: for each player, show highest realm. If their selected realm is different, show that too.
        List<LeaderboardCache.LeaderboardEntry> displayEntries = new ArrayList<>();
        for (Map.Entry<String, List<LeaderboardCache.LeaderboardEntry>> playerEntries : byPlayer.entrySet()) {
            List<LeaderboardCache.LeaderboardEntry> entries = playerEntries.getValue();
            // Sort by total earned desc
            entries.sort((a, b) -> b.getLifetimeCookiesEarned().compareTo(a.getLifetimeCookiesEarned()));
            LeaderboardCache.LeaderboardEntry highest = entries.get(0);
            displayEntries.add(highest);

            // Check if the player's selected realm is different from their highest
            gg.drak.lobbyclicker.data.PlayerData pd = gg.drak.lobbyclicker.data.PlayerManager.getPlayer(playerEntries.getKey()).orElse(null);
            if (pd != null && pd.getActiveProfileId() != null) {
                String activeId = pd.getActiveProfileId();
                if (!activeId.equals(highest.getProfileId())) {
                    // Find the selected realm in entries
                    for (LeaderboardCache.LeaderboardEntry e : entries) {
                        if (e.getProfileId().equals(activeId)) {
                            displayEntries.add(e);
                            break;
                        }
                    }
                }
            }
        }

        // Sort final list by total earned desc
        displayEntries.sort((a, b) -> b.getLifetimeCookiesEarned().compareTo(a.getLifetimeCookiesEarned()));

        if (displayEntries.isEmpty()) {
            setContent(0, GuiHelper.createIcon(Material.PAPER, ChatColor.GRAY + "No entries yet",
                    ChatColor.GRAY + "Be the first to earn cookies!"));
            addPaginationArrows(displayEntries, newPage -> new LeaderboardGui(player, data, newPage).open());
            return;
        }

        populatePagedContent(displayEntries, (entry, slot) -> {
            int slotIndex = 0;
            for (int i = 0; i < PAGINATED_SLOTS.length; i++) {
                if (PAGINATED_SLOTS[i] == slot) { slotIndex = i; break; }
            }
            int rank = page * getItemsPerPage() + slotIndex + 1;

            ChatColor rankColor;
            switch (rank) {
                case 1: rankColor = ChatColor.GOLD; break;
                case 2: rankColor = ChatColor.GRAY; break;
                case 3: rankColor = ChatColor.RED; break;
                default: rankColor = ChatColor.WHITE; break;
            }

            // Check if this is the selected or highest realm
            gg.drak.lobbyclicker.data.PlayerData entryPd = gg.drak.lobbyclicker.data.PlayerManager.getPlayer(entry.getPlayerUuid()).orElse(null);
            boolean isSelected = entryPd != null && entry.getProfileId().equals(entryPd.getActiveProfileId());
            String tag = isSelected ? ChatColor.GREEN + " \u2605" : ChatColor.GRAY + " \u2606"; // filled/empty star

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                try { skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(entry.getPlayerUuid()))); } catch (Exception ignored) {}
                skullMeta.setDisplayName(MenuText.itemLine(rankColor + "#" + rank + " " + ChatColor.WHITE + entry.getPlayerName() + tag));
                skullMeta.setLore(Arrays.asList(
                        MenuText.itemLine(""),
                        MenuText.itemLine(ChatColor.GRAY + "Profile: " + ChatColor.WHITE + entry.getProfileName()
                                + (isSelected ? ChatColor.GREEN + " (Selected)" : "")),
                        MenuText.itemLine(ChatColor.GRAY + "Cookies: " + ChatColor.GOLD + FormatUtils.format(entry.getCookies())),
                        MenuText.itemLine(ChatColor.GRAY + "Total Earned: " + ChatColor.GOLD + FormatUtils.format(entry.getLifetimeCookiesEarned())),
                        MenuText.itemLine(ChatColor.GRAY + "Prestige: " + ChatColor.WHITE + entry.getPrestigeLevel()),
                        MenuText.itemLine(""),
                        MenuText.itemLine(ChatColor.YELLOW + "Click to view profile")
                ));
                head.setItemMeta(skullMeta);
            }

            Icon icon = new Icon(head);
            icon.onClick(e -> {
                stopRefreshTask();
                OPEN_GUIS.remove(player.getUniqueId());
                new ProfileViewGui(player, data, entry,
                        p -> new LeaderboardGui(p, data, page).open()).open();
            });
            addItem(slot, icon);
        });

        addPaginationArrows(displayEntries, newPage -> {
            stopRefreshTask();
            OPEN_GUIS.remove(player.getUniqueId());
            new LeaderboardGui(player, data, newPage).open();
        });
    }
}
