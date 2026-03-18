package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MenuMonitor;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class PlayerSettingsGui extends MenuMonitor {
    private final PlayerData data;

    public PlayerSettingsGui(Player player, PlayerData data) {
        super(player, "player-settings", MonitorStyle.title(ChatColor.YELLOW, "Player Settings"), MonitorStyle.ROWS_SMALL);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);

        Icon sounds = MonitorStyle.menuButton(Material.NOTE_BLOCK, ChatColor.GREEN,
                "Sound Toggles", "Turn sounds on/off");
        sounds.onClick(e -> new SettingsSoundGui(player, data).open());
        addOption(sounds);

        Icon volumes = MonitorStyle.menuButton(Material.JUKEBOX, ChatColor.AQUA,
                "Sound Volumes", "Adjust volume levels (0-2)");
        volumes.onClick(e -> new SettingsVolumeGui(player, data).open());
        addOption(volumes);

        Icon other = MonitorStyle.menuButton(Material.REDSTONE, ChatColor.RED,
                "Other Settings", "Friend requests, public farm, etc.");
        other.onClick(e -> new SettingsOtherGui(player, data).open());
        addOption(other);

        buildMenu(p -> new SettingsMainGui(p, data).open());
    }
}
