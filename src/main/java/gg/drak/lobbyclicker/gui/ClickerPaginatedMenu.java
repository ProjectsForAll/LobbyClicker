package gg.drak.lobbyclicker.gui;

import host.plas.bou.gui.GuiType;
import host.plas.bou.gui.menus.PaginatedMenu;
import host.plas.bou.utils.obj.ManagedInventory;
import mc.obliviate.inventory.Icon;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

/**
 * LobbyClicker wrapper around BOU 1.19 {@link PaginatedMenu}.
 */
public class ClickerPaginatedMenu extends PaginatedMenu {

    public ClickerPaginatedMenu(@NotNull Player player, GuiType type, ManagedInventory fullSlots,
                                int currentPage, int slotsPerPage,
                                int padLeft, int padRight, int padTop, int padBottom,
                                BiFunction<Player, Integer, Icon> whenNotFilled,
                                BiFunction<Player, Integer, Icon> whenFilled) {
        super(player, type, fullSlots, currentPage, slotsPerPage, padLeft, padRight, padTop, padBottom,
                whenNotFilled, whenFilled);
    }

    public ClickerPaginatedMenu(@NotNull Player player, GuiType type, ManagedInventory fullSlots,
                                int slotsPerPage, int padLeft, int padRight, int padTop, int padBottom) {
        super(player, type, fullSlots, slotsPerPage, padLeft, padRight, padTop, padBottom);
    }
}
