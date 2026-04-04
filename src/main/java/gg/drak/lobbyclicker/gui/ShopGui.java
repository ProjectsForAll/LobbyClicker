package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MenuMonitor;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class ShopGui extends MenuMonitor {
    private final PlayerData viewerData;
    private final PlayerData ownerData;

    public ShopGui(Player player, PlayerData viewerData, PlayerData ownerData) {
        super(player, "shop", MonitorStyle.title("green", "Shop"), MonitorStyle.ROWS_SMALL);
        this.viewerData = viewerData;
        this.ownerData = ownerData;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(viewerData, ownerData);

        Icon cookieHelpers = MonitorStyle.menuButton(Material.CHEST, "green",
                "Cookie Helpers", "Buy helpers to earn more cookies!");
        cookieHelpers.onClick(e -> new UpgradeGui(player, viewerData, ownerData).open());
        addOption(cookieHelpers);

        Icon upgrades = MonitorStyle.menuButton(Material.DIAMOND, "aqua",
                "Upgrades", "One-time boosts and bonuses!");
        upgrades.onClick(e -> new ClickerUpgradeGui(player, viewerData, ownerData).open());
        addOption(upgrades);

        buildMenu(p -> {
            if (ownerData.getIdentifier().equals(viewerData.getIdentifier())) {
                new ClickerGui(p, viewerData).open();
            } else {
                new ClickerGui(p, viewerData, ownerData).open();
            }
        });
    }
}
