package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MenuMonitor;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.function.Consumer;

public class PlayerSettingsGui extends MenuMonitor {
    private final PlayerData data;
    private final Consumer<Player> menuBack;

    public PlayerSettingsGui(Player player, PlayerData data, Consumer<Player> menuBack) {
        super(player, "player-settings", MonitorStyle.title("yellow", "Player Settings"), MonitorStyle.ROWS_SMALL);
        this.data = data;
        this.menuBack = menuBack;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);

        Consumer<Player> reopenThis = p -> new PlayerSettingsGui(p, data, menuBack).open();

        Icon sounds = MonitorStyle.menuButton(Material.NOTE_BLOCK, "green",
                "Sound Toggles", "Turn sounds on/off");
        sounds.onClick(e -> new SettingsSoundGui(player, data, reopenThis).open());
        addOption(sounds);

        Icon volumes = MonitorStyle.menuButton(Material.JUKEBOX, "aqua",
                "Sound Volumes", "Adjust volume levels (0-2)");
        volumes.onClick(e -> new SettingsVolumeGui(player, data, reopenThis).open());
        addOption(volumes);

        Icon other = MonitorStyle.menuButton(Material.REDSTONE, "red",
                "Other Settings", "Friend requests, public farm, etc.");
        other.onClick(e -> new SettingsOtherGui(player, data, reopenThis).open());
        addOption(other);

        buildMenu(menuBack);
    }
}
