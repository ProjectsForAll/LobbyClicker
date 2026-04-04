package gg.drak.lobbyclicker.gui;

import host.plas.bou.gui.GuiType;
import host.plas.bou.gui.InventorySheet;
import host.plas.bou.gui.ScreenManager;
import host.plas.bou.gui.screens.ScreenInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Base GUI class for all LobbyClicker GUIs.
 * Extends BukkitOfUtils' ScreenInstance (which extends OblivIate's Gui)
 * so that all GUIs are tracked by ScreenManager.
 */
public abstract class BaseGui extends ScreenInstance {

    /**
     * @param titleMiniMessage inventory title as MiniMessage (converted to legacy § for the GUI framework)
     */
    public BaseGui(@NotNull Player player, String id, String titleMiniMessage, int rows) {
        super(player, new SimpleGuiType(id, MenuText.legacySection(titleMiniMessage)), InventorySheet.empty(rows * 9), true);
        setTitle(MenuText.legacySection(titleMiniMessage));
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        // Register with ScreenManager so the plugin can track open GUIs
        ScreenManager.setScreen(player, this);
    }

    /**
     * Simple GuiType implementation that wraps an id and title string.
     */
    public record SimpleGuiType(String id, String title) implements GuiType {

        @Override
        public String name() {
            return id;
        }

        @Override
        public String toString() {
            return id;
        }

        @Override
        public String getTitle() {
            return title;
        }
    }
}
