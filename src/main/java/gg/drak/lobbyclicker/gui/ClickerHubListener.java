package gg.drak.lobbyclicker.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Dispatches clicks for BOU {@link ClickerHubGui} inventories.
 */
public class ClickerHubListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof ClickerHubGui gui)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(gui.getInventory())) return;
        String key = gui.getKeyAtSlot(event.getRawSlot());
        if (key != null) {
            gui.handleClick(key, player);
        }
    }
}
