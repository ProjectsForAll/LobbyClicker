package gg.drak.lobbyclicker.gui;

import host.plas.bou.gui.AbstractInventoryGui;
import host.plas.bou.gui.CornerColor;
import host.plas.bou.gui.GuiConfig;
import host.plas.bou.gui.GuiLayout;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Hub / confirm menus built on BOU 1.19 {@link AbstractInventoryGui}.
 */
public abstract class ClickerHubGui extends AbstractInventoryGui {
    private final Map<String, Consumer<Player>> actions = new HashMap<>();

    protected ClickerHubGui(@NotNull Player player, CornerColor cornerColor) {
        super(player, cornerColor);
    }

    protected ClickerHubGui(@NotNull Player player) {
        super(GuiConfig.builder(player).cornerColor(CornerColor.YELLOW).build());
    }

    protected void bind(int slot, String key, Consumer<Player> action) {
        bindSlot(slot, key);
        if (action != null) {
            actions.put(key, action);
        }
    }

    public void handleClick(String key, Player clicker) {
        Consumer<Player> action = actions.get(key);
        if (action != null) {
            action.accept(clicker);
        }
    }

    protected ItemStack[] newShell(int size, String title) {
        return beginShell(size, title, getCornerColor());
    }

    public void open() {
        buildAndOpen();
    }

    protected abstract void buildAndOpen();

    protected int backSlot(int size) {
        return GuiLayout.defaultBackSlot(size);
    }
}
