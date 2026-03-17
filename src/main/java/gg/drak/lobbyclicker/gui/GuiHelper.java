package gg.drak.lobbyclicker.gui;

import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.UUID;

/**
 * Shared utility for building GUI icons across all GUIs.
 * For banner displays, see {@link BannerUtil}.
 */
public class GuiHelper {

    public static Icon createIcon(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return new Icon(item);
    }

    public static Icon filler() {
        return createIcon(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    public static Icon fillerBlack() {
        return createIcon(Material.BLACK_STAINED_GLASS_PANE, " ");
    }

    public static Icon backButton(String label) {
        return createIcon(Material.ARROW, ChatColor.RED + label, "", ChatColor.GRAY + "Go back");
    }

    public static Icon homeButton() {
        return createIcon(Material.DARK_OAK_DOOR, ChatColor.GOLD + "Main Menu",
                "", ChatColor.GRAY + "Return to Cookie Clicker");
    }

    public static Icon playerHead(Player player, String name, String... lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) meta.setLore(Arrays.asList(lore));
            head.setItemMeta(meta);
        }
        return new Icon(head);
    }

    public static Icon playerHead(String uuid, String name, String... lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid))); } catch (Exception ignored) {}
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) meta.setLore(Arrays.asList(lore));
            head.setItemMeta(meta);
        }
        return new Icon(head);
    }
}
