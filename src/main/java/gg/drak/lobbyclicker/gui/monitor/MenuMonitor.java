package gg.drak.lobbyclicker.gui.monitor;

import mc.obliviate.inventory.Icon;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A bordered monitor for simple navigation menus with centered options.
 * Options are automatically centered in the content area.
 * Uses the standard action bar (My Info, Viewed Info, Back, My Realm).
 */
public abstract class MenuMonitor extends SimpleGuiMonitor {

    private final List<Icon> options = new ArrayList<>();

    public MenuMonitor(Player player, String id, String title, int rows) {
        super(player, id, title, rows);
    }

    protected void addOption(Icon option) {
        options.add(option);
    }

    /**
     * Build the menu: fill border, center options, add standard action bar.
     */
    protected void buildMenu(Consumer<Player> backAction) {
        fillMonitorBorder();
        buildStandardActionBar(backAction);

        int[] contentSlots = getContentSlots();
        if (options.isEmpty() || contentSlots.length == 0) return;

        // Center options in the content area
        if (options.size() <= 7) {
            int[] centered = MonitorStyle.centeredSlots3Row(options.size());
            // If we have more than 3 rows, offset to center vertically
            int rows = getSize() / 9;
            int contentRows = rows - 2;
            if (contentRows > 1) {
                int middleRow = contentRows / 2;
                int rowOffset = middleRow * 9;
                int[] adjusted = new int[centered.length];
                for (int i = 0; i < centered.length; i++) {
                    adjusted[i] = centered[i] + rowOffset;
                }
                centered = adjusted;
            }
            for (int i = 0; i < options.size() && i < centered.length; i++) {
                addItem(centered[i], options.get(i));
            }
        } else {
            // Use content slots directly for larger menus
            for (int i = 0; i < options.size() && i < contentSlots.length; i++) {
                addItem(contentSlots[i], options.get(i));
            }
        }
    }
}
