package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.boosters.BoosterManager;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MenuMonitor;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class BoostersMenuGui extends MenuMonitor {
    private final PlayerData data;

    public BoostersMenuGui(Player player, PlayerData data) {
        super(player, "boosters-menu", MonitorStyle.title("aqua", "Boosters"), MonitorStyle.ROWS_SMALL);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);

        int activeCount = BoosterManager.getActiveBoosters(data.getIdentifier()).size();

        Icon buy = GuiHelper.createIcon(Material.EMERALD,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Buy Boosters",
                "", ChatColor.GRAY + "Purchase temporary boosts",
                "", ChatColor.YELLOW + "Click to open");
        buy.onClick(e -> new BuyBoostersGui(player, data, 0).open());
        addOption(buy);

        Icon active = GuiHelper.createIcon(Material.BREWING_STAND,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Active Boosters",
                "",
                activeCount > 0
                        ? ChatColor.GREEN + "" + activeCount + " active booster" + (activeCount != 1 ? "s" : "")
                        : ChatColor.GRAY + "No active boosters",
                "", ChatColor.YELLOW + "Click to open");
        active.onClick(e -> new ActiveBoostersGui(player, data, 0).open());
        addOption(active);

        buildMenu(p -> new ClickerGui(p, data).open());
    }
}
