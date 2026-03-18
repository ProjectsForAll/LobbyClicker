package gg.drak.lobbyclicker.gui.monitor;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.BaseGui;
import gg.drak.lobbyclicker.gui.GuiHelper;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Base monitor screen with a standard LobbyClicker border:
 * <pre>
 *   K K K K K K K K K    (row 0: top edge = BLACK glass)
 *   Y . . . . . . . Y    (rows 1-N: left/right edges = YELLOW glass, inner = content)
 *   ...
 *   A A A A A A A A A    (bottom row: action bar, replaced by inheritors)
 * </pre>
 *
 * Yellow stained glass on left/right edges.
 * Black stained glass on top/bottom edges.
 * Bottom row is the "action bar" — inheritors fill it with buttons.
 */
public abstract class SimpleGuiMonitor extends BaseGui {

    protected PlayerData viewerData;
    protected PlayerData viewedData; // null if viewing own stuff

    public SimpleGuiMonitor(@NotNull Player player, String id, String title, int rows) {
        super(player, id, title, rows);
    }

    /**
     * Set the viewer and viewed player data for action bar info icons.
     */
    public void setPlayerContext(PlayerData viewer, PlayerData viewed) {
        this.viewerData = viewer;
        this.viewedData = (viewed != null && !viewed.getIdentifier().equals(viewer.getIdentifier())) ? viewed : null;
    }

    /**
     * Fill the monitor border: black top/bottom, yellow left/right, clear interior.
     */
    protected void fillMonitorBorder() {
        int rows = getSize() / 9;
        Icon blackPane = pane(Material.BLACK_STAINED_GLASS_PANE);
        Icon yellowPane = pane(Material.YELLOW_STAINED_GLASS_PANE);

        // Fill everything with air first (clear)
        fillGui(pane(Material.AIR));

        // Top row: all black
        for (int col = 0; col < 9; col++) {
            addItem(col, blackPane);
        }
        // Bottom row: all black (action bar will overwrite)
        int bottomStart = (rows - 1) * 9;
        for (int col = 0; col < 9; col++) {
            addItem(bottomStart + col, blackPane);
        }
        // Left and right edges for middle rows
        for (int row = 1; row < rows - 1; row++) {
            addItem(row * 9, yellowPane);         // left edge
            addItem(row * 9 + 8, yellowPane);     // right edge
        }
    }

    /**
     * Build the standard "other GUI" action bar (bottom row).
     * Slot 1: My Info (nether star), Slot 2: Viewed player info (head),
     * Slot 8: Back (dark oak door), Slot 9: My Realm (cookie).
     */
    protected void buildStandardActionBar(Consumer<Player> backAction) {
        int bottomStart = (getSize() / 9 - 1) * 9;

        // Slot 1 (index bottomStart+0): My Information
        if (viewerData != null) {
            Icon myInfo = GuiHelper.createIcon(Material.NETHER_STAR,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "My Info",
                    "",
                    ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(viewerData.getCookies()),
                    ChatColor.GRAY + "CPS: " + ChatColor.WHITE + FormatUtils.format(viewerData.getCps()),
                    ChatColor.GRAY + "CPC: " + ChatColor.WHITE + FormatUtils.format(viewerData.getCpc()),
                    ChatColor.GRAY + "Prestige: " + ChatColor.WHITE + viewerData.getPrestigeLevel());
            addItem(bottomStart, myInfo);
        }

        // Slot 2 (index bottomStart+1): Viewed player info (if viewing someone else)
        if (viewedData != null) {
            Icon viewedInfo = GuiHelper.playerHead(viewedData.getIdentifier(),
                    ChatColor.AQUA + "" + ChatColor.BOLD + viewedData.getName() + "'s Info",
                    "",
                    ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(viewedData.getCookies()),
                    ChatColor.GRAY + "CPS: " + ChatColor.WHITE + FormatUtils.format(viewedData.getCps()),
                    ChatColor.GRAY + "CPC: " + ChatColor.WHITE + FormatUtils.format(viewedData.getCpc()),
                    ChatColor.GRAY + "Prestige: " + ChatColor.WHITE + viewedData.getPrestigeLevel());
            addItem(bottomStart + 1, viewedInfo);
        }

        // Slot 8 (index bottomStart+7): Back button
        Icon back = GuiHelper.createIcon(Material.DARK_OAK_DOOR,
                ChatColor.RED + "" + ChatColor.BOLD + "Back",
                "", ChatColor.GRAY + "Go back");
        back.onClick(e -> backAction.accept(player));
        addItem(bottomStart + 7, back);

        // Slot 9 (index bottomStart+8): My Realm button
        Icon myRealm = GuiHelper.createIcon(Material.COOKIE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "My Realm",
                "", ChatColor.GRAY + "Return to your realm");
        myRealm.onClick(e -> {
            if (viewerData != null) {
                new gg.drak.lobbyclicker.gui.ClickerGui(player, viewerData).open();
            }
        });
        addItem(bottomStart + 8, myRealm);
    }

    /**
     * Get the content slot indexes (inner area, excluding all borders).
     * For a 6-row GUI: indexes 10-16, 19-25, 28-34, 37-43.
     */
    public int[] getContentSlots() {
        int rows = getSize() / 9;
        int contentRows = rows - 2; // exclude top and bottom rows
        if (contentRows <= 0) return new int[0];

        int[] slots = new int[contentRows * 7];
        int idx = 0;
        for (int row = 1; row <= contentRows; row++) {
            for (int col = 1; col <= 7; col++) {
                slots[idx++] = row * 9 + col;
            }
        }
        return slots;
    }

    public int getContentSlotCount() {
        return getContentSlots().length;
    }

    /**
     * Place an icon in a content slot by content index (0-based within inner area).
     */
    public void setContent(int contentIndex, Icon icon) {
        int[] slots = getContentSlots();
        if (contentIndex >= 0 && contentIndex < slots.length) {
            addItem(slots[contentIndex], icon);
        }
    }

    /**
     * Place an icon at a raw inventory index.
     */
    public void setSlot(int index, Icon icon) {
        addItem(index, icon);
    }

    private static Icon pane(Material material) {
        if (material == Material.AIR) return new Icon(new ItemStack(Material.AIR));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return new Icon(item);
    }

    public static Icon icon(Material material, String name, String... lore) {
        return GuiHelper.createIcon(material, name, lore);
    }
}
