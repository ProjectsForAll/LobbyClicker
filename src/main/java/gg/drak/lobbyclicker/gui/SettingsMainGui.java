package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MenuMonitor;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.function.Consumer;

public class SettingsMainGui extends MenuMonitor {
    private final PlayerData data;
    private final PlayerData realmOwner;

    public SettingsMainGui(Player player, PlayerData data) {
        this(player, data, null);
    }

    public SettingsMainGui(Player player, PlayerData data, PlayerData realmOwner) {
        super(player, "settings-main", MonitorStyle.title("Settings"), MonitorStyle.ROWS_SMALL);
        this.data = data;
        this.realmOwner = realmOwner;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, realmOwner);

        Consumer<Player> backToThisHub = realmOwner != null
                ? p -> new SettingsMainGui(p, data, realmOwner).open()
                : p -> new SettingsMainGui(p, data).open();

        Icon playerSettings = MonitorStyle.menuButton(Material.COMPARATOR, "yellow",
                "Player Settings", "Sounds, volumes, preferences");
        playerSettings.onClick(e -> new PlayerSettingsGui(player, data, backToThisHub).open());
        addOption(playerSettings);

        if (!LobbyClicker.getMainConfig().isSimpleMode()
                && LobbyClicker.getMainConfig().isRealmSettingsMenuEnabled()) {
            Icon realmSettings = MonitorStyle.menuButton(Material.BEACON, "light_purple",
                    "Realm Settings", "Manage members, realm options");
            realmSettings.onClick(e -> new RealmSettingsGui(player, data).open());
            addOption(realmSettings);
        }

        buildMenu(p -> {
            if (realmOwner != null) {
                new ClickerGui(p, data, realmOwner).open();
            } else {
                new ClickerGui(p, data).open();
            }
        });
    }
}
