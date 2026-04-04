package gg.drak.lobbyclicker.gui.monitor;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.BaseGui;
import gg.drak.lobbyclicker.gui.GuiHelper;
import gg.drak.lobbyclicker.gui.MenuText;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

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

    public SimpleGuiMonitor(@NotNull Player player, String id, String titleMiniMessage, int rows) {
        super(player, id, titleMiniMessage, rows);
    }

    public void setPlayerContext(PlayerData viewer, PlayerData viewed) {
        this.viewerData = viewer;
        this.viewedData = (viewed != null && !viewed.getIdentifier().equals(viewer.getIdentifier())) ? viewed : null;
    }

    protected void fillMonitorBorder() {
        int rows = getSize() / 9;
        Icon blackPane = pane(Material.BLACK_STAINED_GLASS_PANE);
        Icon yellowPane = pane(Material.YELLOW_STAINED_GLASS_PANE);

        fillGui(pane(Material.AIR));

        for (int col = 0; col < 9; col++) {
            addItem(col, blackPane);
        }
        int bottomStart = (rows - 1) * 9;
        for (int col = 0; col < 9; col++) {
            addItem(bottomStart + col, blackPane);
        }
        for (int row = 1; row < rows - 1; row++) {
            addItem(row * 9, yellowPane);
            addItem(row * 9 + 8, yellowPane);
        }

        if (viewedData != null && !gg.drak.lobbyclicker.LobbyClicker.getMainConfig().isSimpleMode()) {
            Icon realmHead = GuiHelper.playerHead(viewedData.getIdentifier(),
                    "<gold><bold>" + MenuText.esc(viewedData.getName() + "'s Realm") + "</bold></gold>",
                    "",
                    "<gray>" + MenuText.esc("Click to return to their realm") + "</gray>");
            realmHead.onClick(e -> new gg.drak.lobbyclicker.gui.ClickerGui(player, viewerData, viewedData).open());
            addItem(0, realmHead);
        }
    }

    protected void buildStandardActionBar(Consumer<Player> backAction) {
        int bottomStart = (getSize() / 9 - 1) * 9;

        if (viewerData != null) {
            Icon myInfo = GuiHelper.createIcon(Material.NETHER_STAR,
                    MenuText.title("My Info"),
                    "",
                    MenuText.grayWhite("Cookies: ", FormatUtils.format(viewerData.getCookies())),
                    MenuText.grayWhite("CPS: ", FormatUtils.format(viewerData.getCps())),
                    MenuText.grayWhite("CPC: ", FormatUtils.format(viewerData.getCpc())),
                    MenuText.grayWhite("Prestige: ", String.valueOf(viewerData.getPrestigeLevel())));
            addItem(bottomStart, myInfo);
        }

        if (viewedData != null && !gg.drak.lobbyclicker.LobbyClicker.getMainConfig().isSimpleMode()) {
            Icon viewedInfo = GuiHelper.playerHead(viewedData.getIdentifier(),
                    "<aqua><bold>" + MenuText.esc(viewedData.getName() + "'s Info") + "</bold></aqua>",
                    "",
                    MenuText.grayWhite("Cookies: ", FormatUtils.format(viewedData.getCookies())),
                    MenuText.grayWhite("CPS: ", FormatUtils.format(viewedData.getCps())),
                    MenuText.grayWhite("CPC: ", FormatUtils.format(viewedData.getCpc())),
                    MenuText.grayWhite("Prestige: ", String.valueOf(viewedData.getPrestigeLevel())));
            addItem(bottomStart + 1, viewedInfo);
        }

        Icon back = GuiHelper.createIcon(Material.DARK_OAK_DOOR,
                "<red><bold>" + MenuText.esc("Back") + "</bold></red>",
                "",
                "<gray>" + MenuText.esc("Go back") + "</gray>");
        back.onClick(e -> backAction.accept(player));
        addItem(bottomStart + 7, back);

        boolean simple = gg.drak.lobbyclicker.LobbyClicker.getMainConfig().isSimpleMode();
        Icon myRealm = GuiHelper.createIcon(Material.COOKIE,
                "<gold><bold>" + MenuText.esc(simple ? "My Cookie" : "My Realm") + "</bold></gold>",
                "",
                "<gray>" + MenuText.esc(simple ? "Return to your clicker" : "Return to your realm") + "</gray>");
        myRealm.onClick(e -> {
            if (viewerData != null) {
                new gg.drak.lobbyclicker.gui.ClickerGui(player, viewerData).open();
            }
        });
        addItem(bottomStart + 8, myRealm);
    }

    public int[] getContentSlots() {
        int rows = getSize() / 9;
        int contentRows = rows - 2;
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

    public void setContent(int contentIndex, Icon icon) {
        int[] slots = getContentSlots();
        if (contentIndex >= 0 && contentIndex < slots.length) {
            addItem(slots[contentIndex], icon);
        }
    }

    public void setSlot(int index, Icon icon) {
        addItem(index, icon);
    }

    private static Icon pane(Material material) {
        if (material == Material.AIR) return new Icon(new ItemStack(Material.AIR));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            MenuText.hideVanillaTooltips(meta);
            item.setItemMeta(meta);
        }
        return new Icon(item);
    }

    public static Icon icon(Material material, String mmName, String... mmLore) {
        return GuiHelper.createIcon(material, mmName, mmLore);
    }
}
