package gg.drak.lobbyclicker.gui.admin;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.GuiHelper;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Admin GUI showing ALL players from the database. Click a player to manage their profiles.
 * Loads player list from DB on first open, caches it.
 */
public class AdminPlayerListGui extends PaginationMonitor {

    // Cache of all players from DB
    private static final List<PlayerEntry> cachedPlayers = new CopyOnWriteArrayList<>();
    private static long lastCacheUpdate = 0;

    public AdminPlayerListGui(Player player) {
        this(player, 0);
    }

    public AdminPlayerListGui(Player player, int page) {
        super(player, "admin-players", ChatColor.DARK_RED + "" + ChatColor.BOLD + "All Players", page);
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        fillMonitorBorder();

        int b = (getSize() / 9 - 1) * 9;
        Icon back = GuiHelper.createIcon(Material.DARK_OAK_DOOR,
                ChatColor.RED + "" + ChatColor.BOLD + "Back", "", ChatColor.GRAY + "Back to admin panel");
        back.onClick(e -> new AdminMainGui(player).open());
        addItem(b + 7, back);
        Icon close = GuiHelper.createIcon(Material.BARRIER, ChatColor.RED + "Close");
        close.onClick(e -> player.closeInventory());
        addItem(b + 8, close);

        // Refresh icon
        addItem(4, GuiHelper.createIcon(Material.COMPASS,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Refresh",
                "", ChatColor.GRAY + "Click to reload player list from database"));
        // Use addItem with click to avoid icon.onClick in a createIcon call
        Icon refreshIcon = GuiHelper.createIcon(Material.COMPASS,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Refresh",
                "", ChatColor.GRAY + "Players: " + ChatColor.WHITE + cachedPlayers.size(),
                "", ChatColor.YELLOW + "Click to reload from database");
        refreshIcon.onClick(e -> {
            lastCacheUpdate = 0; // force refresh
            player.sendMessage(ChatColor.YELLOW + "Refreshing player list...");
            loadAndShow();
        });
        addItem(4, refreshIcon);

        // Load from cache or DB
        if (System.currentTimeMillis() - lastCacheUpdate > 60_000 || cachedPlayers.isEmpty()) {
            loadAndShow();
        } else {
            showPlayers();
        }
    }

    private void loadAndShow() {
        LobbyClicker.getDatabase().pullAllPlayersThreaded().thenAccept(players -> {
            cachedPlayers.clear();
            // Online players first
            for (Player p : Bukkit.getOnlinePlayers()) {
                cachedPlayers.add(new PlayerEntry(p.getUniqueId().toString(), p.getName(), true));
            }
            // DB players (skip already added online ones)
            Set<String> onlineUuids = new HashSet<>();
            for (Player p : Bukkit.getOnlinePlayers()) onlineUuids.add(p.getUniqueId().toString());
            for (PlayerData pd : players) {
                if (!onlineUuids.contains(pd.getIdentifier())) {
                    String name = pd.getName();
                    if (name == null || name.isEmpty()) name = pd.getIdentifier().substring(0, 8);
                    cachedPlayers.add(new PlayerEntry(pd.getIdentifier(), name, false));
                }
            }
            lastCacheUpdate = System.currentTimeMillis();

            Bukkit.getScheduler().runTask(LobbyClicker.getInstance(), this::showPlayers);
        });
    }

    private void showPlayers() {
        populatePagedContent(cachedPlayers, (entry, slot) -> {
            ChatColor nameColor = entry.online ? ChatColor.GREEN : ChatColor.GRAY;
            String status = entry.online ? ChatColor.GREEN + "Online" : ChatColor.GRAY + "Offline";

            // Use OfflinePlayer for skull to get proper skin
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                try {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(entry.uuid));
                    meta.setOwningPlayer(offlinePlayer);
                } catch (Exception ignored) {}
                meta.setDisplayName(nameColor + entry.name);
                meta.setLore(Arrays.asList(
                        "", status,
                        "", ChatColor.YELLOW + "Click to manage profiles"));
                head.setItemMeta(meta);
            }

            Icon icon = new Icon(head);
            icon.onClick(e -> new AdminProfilesGui(player, entry.uuid, entry.name).open());
            addItem(slot, icon);
        });

        addPaginationArrows(cachedPlayers, newPage -> new AdminPlayerListGui(player, newPage).open());
    }

    private static class PlayerEntry {
        final String uuid;
        final String name;
        final boolean online;
        PlayerEntry(String uuid, String name, boolean online) {
            this.uuid = uuid;
            this.name = name;
            this.online = online;
        }
    }
}
