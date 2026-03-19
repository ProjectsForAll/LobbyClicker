package gg.drak.lobbyclicker.gui.monitor;

import gg.drak.lobbyclicker.gui.GuiHelper;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Common styling utilities for uniform GUI appearance.
 * All monitors should use these for consistent look and feel.
 */
public class MonitorStyle {

    // --- Standard border materials ---
    public static final Material BORDER_DEFAULT = Material.GRAY_STAINED_GLASS_PANE;
    public static final Material BORDER_DARK = Material.BLACK_STAINED_GLASS_PANE;
    public static final Material BORDER_LIGHT = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
    public static final Material BORDER_ACCENT = Material.BLUE_STAINED_GLASS_PANE;

    // --- Standard row sizes ---
    public static final int ROWS_SMALL = 3;   // 27 slots, 1x7 content
    public static final int ROWS_MEDIUM = 4;  // 36 slots, 2x7 content
    public static final int ROWS_LARGE = 5;   // 45 slots, 3x7 content
    public static final int ROWS_FULL = 6;    // 54 slots, 4x7 content

    // --- Standard title formatting ---

    public static String title(String text) {
        return ChatColor.GOLD + "" + ChatColor.BOLD + text;
    }

    public static String title(ChatColor color, String text) {
        return color + "" + ChatColor.BOLD + text;
    }

    // --- Common button builders ---

    public static Icon confirmButton(String label) {
        return GuiHelper.createIcon(Material.LIME_DYE,
                ChatColor.GREEN + "" + ChatColor.BOLD + label);
    }

    public static Icon cancelButton(String label) {
        return GuiHelper.createIcon(Material.RED_DYE,
                ChatColor.RED + "" + ChatColor.BOLD + label);
    }

    public static Icon infoItem(Material material, String title, String... lore) {
        return GuiHelper.createIcon(material, title, lore);
    }

    public static Icon toggleButton(String label, boolean on, String description) {
        Material mat = on ? Material.LIME_DYE : Material.GRAY_DYE;
        String status = on ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF";
        return GuiHelper.createIcon(mat,
                ChatColor.YELLOW + label + " " + status,
                "", ChatColor.GRAY + description, "", ChatColor.GRAY + "Click to toggle");
    }

    /**
     * Creates a category/section button for navigation menus.
     */
    public static Icon menuButton(Material material, ChatColor color, String label, String... description) {
        String[] lore = new String[description.length + 2];
        lore[0] = "";
        for (int i = 0; i < description.length; i++) {
            lore[i + 1] = ChatColor.GRAY + description[i];
        }
        lore[lore.length - 1] = ChatColor.YELLOW + "Click to open";
        return GuiHelper.createIcon(material, color + "" + ChatColor.BOLD + label, lore);
    }

    /**
     * Creates a filler/spacer icon (invisible).
     */
    public static Icon filler() {
        return GuiHelper.filler();
    }

    public static Icon filler(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return new Icon(item);
    }

    // --- Standard slot positions for common layouts ---

    /**
     * For a 3-row bordered monitor (27 slots):
     * Content area is row 1, cols 1-7 (indexes 10-16).
     * Good positions for centered 2-option menus: indexes 12, 14
     * Good positions for centered 3-option menus: indexes 11, 13, 15
     */
    public static int[] centeredSlots3Row(int count) {
        switch (count) {
            case 1: return new int[]{13};
            case 2: return new int[]{12, 14};
            case 3: return new int[]{11, 13, 15};
            case 4: return new int[]{10, 12, 14, 16};
            case 5: return new int[]{10, 11, 13, 15, 16};
            default: return new int[]{10, 11, 12, 13, 14, 15, 16};
        }
    }

    /**
     * For a 4-row bordered monitor (36 slots):
     * Content area is rows 1-2, cols 1-7 (indexes 10-16, 19-25).
     */
    public static int[] centeredSlots4Row(int count) {
        if (count <= 7) return centeredSlots3Row(count);
        // Use both content rows
        int[] row1 = {10, 11, 12, 13, 14, 15, 16};
        int[] row2 = {19, 20, 21, 22, 23, 24, 25};
        int[] result = new int[Math.min(count, 14)];
        System.arraycopy(row1, 0, result, 0, Math.min(7, count));
        if (count > 7) System.arraycopy(row2, 0, result, 7, Math.min(7, count - 7));
        return result;
    }
}
