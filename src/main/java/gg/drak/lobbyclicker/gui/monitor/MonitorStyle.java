package gg.drak.lobbyclicker.gui.monitor;

import gg.drak.lobbyclicker.gui.GuiHelper;
import gg.drak.lobbyclicker.gui.MenuText;
import mc.obliviate.inventory.Icon;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

    // --- Standard title formatting (MiniMessage; small caps) ---

    public static String title(String text) {
        return MenuText.title(text);
    }

    /** @param namedColor MiniMessage color name, e.g. {@code gold}, {@code light_purple} */
    public static String title(String namedColor, String text) {
        return MenuText.title(namedColor, text);
    }

    // --- Common button builders ---

    public static Icon confirmButton(String label) {
        return GuiHelper.createIcon(Material.LIME_DYE,
                "<green><bold>" + MenuText.esc(label) + "</bold></green>");
    }

    public static Icon cancelButton(String label) {
        return GuiHelper.createIcon(Material.RED_DYE,
                "<red><bold>" + MenuText.esc(label) + "</bold></red>");
    }

    public static Icon infoItem(Material material, String mmTitle, String... mmLore) {
        return GuiHelper.createIcon(material, mmTitle, mmLore);
    }

    public static Icon toggleButton(String label, boolean on, String description) {
        Material mat = on ? Material.LIME_DYE : Material.GRAY_DYE;
        String status = on ? "<green>" + MenuText.esc("ON") + "</green>" : "<red>" + MenuText.esc("OFF") + "</red>";
        return GuiHelper.createIcon(mat,
                "<yellow>" + MenuText.esc(label) + " " + status + "</yellow>",
                "",
                "<gray>" + MenuText.esc(description) + "</gray>",
                "",
                "<gray>" + MenuText.esc("Click to toggle") + "</gray>");
    }

    /**
     * Creates a category/section button for navigation menus.
     *
     * @param labelColor MiniMessage color name for the label, e.g. {@code yellow}
     */
    public static Icon menuButton(Material material, String labelColor, String label, String... description) {
        String[] lore = new String[description.length + 2];
        lore[0] = "";
        for (int i = 0; i < description.length; i++) {
            lore[i + 1] = "<gray>" + MenuText.esc(description[i]) + "</gray>";
        }
        lore[lore.length - 1] = "<yellow>" + MenuText.esc("Click to open") + "</yellow>";
        return GuiHelper.createIcon(material,
                "<" + labelColor + "><bold>" + MenuText.esc(label) + "</bold></" + labelColor + ">",
                lore);
    }

    public static Icon filler() {
        return GuiHelper.filler();
    }

    public static Icon filler(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            MenuText.hideVanillaTooltips(meta);
            item.setItemMeta(meta);
        }
        return new Icon(item);
    }

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

    public static int[] centeredSlots4Row(int count) {
        if (count <= 7) return centeredSlots3Row(count);
        int[] row1 = {10, 11, 12, 13, 14, 15, 16};
        int[] row2 = {19, 20, 21, 22, 23, 24, 25};
        int[] result = new int[Math.min(count, 14)];
        System.arraycopy(row1, 0, result, 0, Math.min(7, count));
        if (count > 7) System.arraycopy(row2, 0, result, 7, Math.min(7, count - 7));
        return result;
    }
}
