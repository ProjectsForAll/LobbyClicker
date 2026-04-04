package gg.drak.lobbyclicker.gui.monitor;

import gg.drak.lobbyclicker.gui.GuiHelper;
import gg.drak.lobbyclicker.gui.MenuText;
import mc.obliviate.inventory.Icon;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * A bordered monitor for two-step confirmations.
 */
public abstract class ConfirmationMonitor extends SimpleGuiMonitor {

    private boolean confirmed = false;

    public ConfirmationMonitor(Player player, String id, String titleMiniMessage) {
        super(player, id, titleMiniMessage, MonitorStyle.ROWS_SMALL);
    }

    protected void buildConfirmation(
            Icon infoItem,
            String confirmLabel,
            String confirmWarning,
            Consumer<Player> onConfirm,
            Consumer<Player> onCancel
    ) {
        fillMonitorBorder();
        buildStandardActionBar(onCancel);

        setContent(3, infoItem);

        Icon confirmIcon;
        if (!confirmed) {
            confirmIcon = MonitorStyle.confirmButton(confirmLabel);
            confirmIcon.onClick(e -> {
                confirmed = true;
                buildConfirmation(infoItem, confirmLabel, confirmWarning, onConfirm, onCancel);
            });
        } else {
            confirmIcon = GuiHelper.createIcon(Material.LIME_DYE,
                    "<green><bold>" + MenuText.esc("CONFIRM") + "</bold></green>",
                    "",
                    "<red><bold>" + MenuText.esc(confirmWarning) + "</bold></red>",
                    "",
                    "<yellow>" + MenuText.esc("Click again to confirm!") + "</yellow>");
            confirmIcon.onClick(e -> onConfirm.accept(player));
        }
        setContent(5, confirmIcon);

        Icon cancelIcon = MonitorStyle.cancelButton("Cancel");
        cancelIcon.onClick(e -> onCancel.accept(player));
        setContent(1, cancelIcon);
    }
}
