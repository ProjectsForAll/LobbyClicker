package gg.drak.lobbyclicker.gui;

import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared utility for building GUI icons across all GUIs.
 * Strings are MiniMessage source; they are converted to legacy § for Spigot ItemMeta.
 */
public class GuiHelper {

    public static Icon createIcon(Material material, String mmName, String... mmLore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MenuText.itemLine(mmName));
            if (mmLore != null && mmLore.length > 0) {
                List<String> lore = new ArrayList<>(mmLore.length);
                for (String line : mmLore) {
                    lore.add(MenuText.itemLine(line));
                }
                meta.setLore(lore);
            }
            MenuText.hideVanillaTooltips(meta);
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
        return createIcon(Material.ARROW,
                "<red>" + MenuText.esc(label) + "</red>",
                "",
                "<gray>" + MenuText.esc("Go back") + "</gray>");
    }

    public static Icon homeButton() {
        return createIcon(Material.DARK_OAK_DOOR,
                "<gold>" + MenuText.esc("Main Menu") + "</gold>",
                "",
                "<gray>" + MenuText.esc("Return to Cookie Clicker") + "</gray>");
    }

    public static Icon playerHead(Player player, String mmName, String... mmLore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(MenuText.itemLine(mmName));
            if (mmLore != null && mmLore.length > 0) {
                List<String> lore = new ArrayList<>(mmLore.length);
                for (String line : mmLore) {
                    lore.add(MenuText.itemLine(line));
                }
                meta.setLore(lore);
            }
            MenuText.hideVanillaTooltips(meta);
            head.setItemMeta(meta);
        }
        return new Icon(head);
    }

    public static Icon playerHead(String uuid, String mmName, String... mmLore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            try {
                UUID uid = UUID.fromString(uuid);
                Player onlinePlayer = Bukkit.getPlayer(uid);
                if (onlinePlayer != null) {
                    meta.setOwningPlayer(onlinePlayer);
                } else {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(uid));
                }
            } catch (Exception ignored) {}
            meta.setDisplayName(MenuText.itemLine(mmName));
            if (mmLore != null && mmLore.length > 0) {
                List<String> lore = new ArrayList<>(mmLore.length);
                for (String line : mmLore) {
                    lore.add(MenuText.itemLine(line));
                }
                meta.setLore(lore);
            }
            MenuText.hideVanillaTooltips(meta);
            head.setItemMeta(meta);
        }
        return new Icon(head);
    }

    public static void applyMenuMeta(ItemMeta meta, String mmName, String... mmLore) {
        if (meta == null) return;
        meta.setDisplayName(MenuText.itemLine(mmName));
        if (mmLore != null && mmLore.length > 0) {
            List<String> lore = new ArrayList<>(mmLore.length);
            for (String line : mmLore) {
                lore.add(MenuText.itemLine(line));
            }
            meta.setLore(lore);
        }
        MenuText.hideVanillaTooltips(meta);
    }
}
