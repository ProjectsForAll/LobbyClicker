package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.redis.RedisManager;
import mc.obliviate.inventory.Gui;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AllPlayersGui extends Gui {
    private final PlayerData data;
    private final int page;
    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public AllPlayersGui(Player player, PlayerData data, int page) {
        super(player, "all-players", ChatColor.YELLOW + "" + ChatColor.BOLD + "All Players (Page " + (page + 1) + ")", 6);
        this.data = data;
        this.page = page;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        // Build combined list: local players + cross-server players
        List<PlayerEntry> entries = new ArrayList<>();

        // Local online players (exclude self)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().toString().equals(data.getIdentifier())) continue;
            entries.add(new PlayerEntry(p.getUniqueId().toString(), p.getName(), null, true));
        }

        // Cross-server players (if Redis is enabled)
        RedisManager redis = LobbyClicker.getRedisManager();
        if (redis != null) {
            for (RedisManager.CrossServerPlayer csp : redis.getCrossServerPlayerList()) {
                // Don't add if already listed as local
                if (Bukkit.getPlayer(java.util.UUID.fromString(csp.getUuid())) != null) continue;
                entries.add(new PlayerEntry(csp.getUuid(), csp.getName(), csp.getPrettyName(), false));
            }
        }

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, entries.size());

        for (int i = start; i < end; i++) {
            PlayerEntry entry = entries.get(i);
            int slotIndex = i - start;
            if (slotIndex >= ITEM_SLOTS.length) break;

            boolean isFriend = data.getFriends().contains(entry.uuid);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(java.util.UUID.fromString(entry.uuid))); } catch (Exception ignored) {}
                meta.setDisplayName((entry.local ? ChatColor.GREEN : ChatColor.AQUA) + entry.name);
                List<String> lore = new ArrayList<>();
                if (isFriend) lore.add(ChatColor.GREEN + "Friend");
                if (!entry.local) lore.add(ChatColor.GRAY + "Server: " + ChatColor.WHITE + entry.server);
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click for actions");
                meta.setLore(lore);
                head.setItemMeta(meta);
            }

            Icon icon = new Icon(head);
            icon.onClick(e -> new PlayerActionGui(player, data, entry.uuid, "all").open());
            addItem(ITEM_SLOTS[slotIndex], icon);
        }

        if (page > 0) {
            Icon prev = GuiHelper.createIcon(Material.ARROW, ChatColor.YELLOW + "Previous Page");
            prev.onClick(e -> new AllPlayersGui(player, data, page - 1).open());
            addItem(45, prev);
        }
        if (end < entries.size()) {
            Icon next = GuiHelper.createIcon(Material.ARROW, ChatColor.YELLOW + "Next Page");
            next.onClick(e -> new AllPlayersGui(player, data, page + 1).open());
            addItem(53, next);
        }

        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new SocialMainGui(player, data).open());
        addItem(49, back);
    }

    private static class PlayerEntry {
        final String uuid;
        final String name;
        final String server; // null for local
        final boolean local;

        PlayerEntry(String uuid, String name, String server, boolean local) {
            this.uuid = uuid;
            this.name = name;
            this.server = server;
            this.local = local;
        }
    }
}
