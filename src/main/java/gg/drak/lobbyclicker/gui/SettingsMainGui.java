package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MenuMonitor;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class SettingsMainGui extends MenuMonitor {
    private final PlayerData data;

    public SettingsMainGui(Player player, PlayerData data) {
        super(player, "settings-main", MonitorStyle.title("Settings"), MonitorStyle.ROWS_SMALL);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);

        Icon playerSettings = MonitorStyle.menuButton(Material.COMPARATOR, ChatColor.YELLOW,
                "Player Settings", "Sounds, volumes, preferences");
        playerSettings.onClick(e -> new PlayerSettingsGui(player, data).open());
        addOption(playerSettings);

        Icon realmSettings = MonitorStyle.menuButton(Material.BEACON, ChatColor.LIGHT_PURPLE,
                "Realm Settings", "Manage members, realm options");
        realmSettings.onClick(e -> new RealmSettingsGui(player, data).open());
        addOption(realmSettings);

        buildMenu(p -> new ClickerGui(p, data).open());
    }
}
