package gg.drak.lobbyclicker.gui.monitor;

import gg.drak.lobbyclicker.gui.GuiHelper;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * A bordered monitor for two-step confirmations.
 * Shows an info item in the center, with confirm and cancel buttons.
 * Uses the standard action bar.
 */
public abstract class ConfirmationMonitor extends SimpleGuiMonitor {

    private boolean confirmed = false;

    public ConfirmationMonitor(Player player, String id, String title) {
        super(player, id, title, MonitorStyle.ROWS_SMALL);
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

        // Info in center of content row
        setContent(3, infoItem);

        // Confirm button
        Icon confirmIcon;
        if (!confirmed) {
            confirmIcon = MonitorStyle.confirmButton(confirmLabel);
            confirmIcon.onClick(e -> {
                confirmed = true;
                buildConfirmation(infoItem, confirmLabel, confirmWarning, onConfirm, onCancel);
            });
        } else {
            confirmIcon = GuiHelper.createIcon(Material.LIME_WOOL,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "CONFIRM",
                    "", ChatColor.RED + "" + ChatColor.BOLD + confirmWarning,
                    "", ChatColor.YELLOW + "Click again to confirm!");
            confirmIcon.onClick(e -> onConfirm.accept(player));
        }
        setContent(5, confirmIcon);

        // Cancel button
        Icon cancelIcon = MonitorStyle.cancelButton("Cancel");
        cancelIcon.onClick(e -> onCancel.accept(player));
        setContent(1, cancelIcon);
    }
}
