package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Gui;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class LeaderboardGui extends Gui {
    private static final int[] LEADERBOARD_SLOTS = {10, 11, 12, 13, 14, 28, 29, 30, 31, 32};

    private final PlayerData data;

    public LeaderboardGui(Player player, PlayerData data) {
        super(player, "clicker-leaderboard", ChatColor.AQUA + "" + ChatColor.BOLD + "Leaderboard", 6);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();

        fillGui(createFiller(Material.BLACK_STAINED_GLASS_PANE));

        addItem(4, createIcon(Material.GOLD_BLOCK, ChatColor.GOLD + "" + ChatColor.BOLD + "Top Cookie Earners",
                new String[]{
                        "",
                        ChatColor.GRAY + "Ranked by total cookies earned"
                }));

        // Back button
        Icon back = createIcon(Material.ARROW, ChatColor.RED + "Back", new String[]{
                "",
                ChatColor.GRAY + "Return to Cookie Clicker"
        });
        back.onClick(e -> new ClickerGui(player, data).open());
        addItem(49, back);

        List<PlayerData> entries = PlayerManager.getLoadedPlayers().stream()
                .filter(PlayerData::isFullyLoaded)
                .sorted((a, b) -> b.getTotalCookiesEarned().compareTo(a.getTotalCookiesEarned()))
                .limit(LEADERBOARD_SLOTS.length)
                .collect(Collectors.toList());

        if (entries.isEmpty()) {
            addItem(22, createIcon(Material.PAPER, ChatColor.GRAY + "No entries yet",
                    new String[]{ChatColor.GRAY + "Be the first to earn cookies!"}));
            return;
        }

        for (int i = 0; i < entries.size(); i++) {
            PlayerData entry = entries.get(i);
            int rank = i + 1;
            int slot = LEADERBOARD_SLOTS[i];

            ChatColor rankColor;
            switch (rank) {
                case 1: rankColor = ChatColor.GOLD; break;
                case 2: rankColor = ChatColor.GRAY; break;
                case 3: rankColor = ChatColor.RED; break;
                default: rankColor = ChatColor.WHITE; break;
            }

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                try {
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(entry.getIdentifier())));
                } catch (Exception ignored) {}
                skullMeta.setDisplayName(rankColor + "#" + rank + " " + ChatColor.WHITE + entry.getName());
                skullMeta.setLore(Arrays.asList(
                        "",
                        ChatColor.GRAY + "Cookies: " + ChatColor.GOLD + FormatUtils.format(entry.getCookies()),
                        ChatColor.GRAY + "Total Earned: " + ChatColor.GOLD + FormatUtils.format(entry.getTotalCookiesEarned())
                ));
                head.setItemMeta(skullMeta);
            }

            addItem(slot, new Icon(head));
        }
    }

    private static Icon createFiller(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return new Icon(item);
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
