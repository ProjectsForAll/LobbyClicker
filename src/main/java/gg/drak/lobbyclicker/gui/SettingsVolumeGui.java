package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.settings.SettingType;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SettingsVolumeGui extends PaginationMonitor {
    private final PlayerData data;
    private final Consumer<Player> backToPlayerSettings;

    private static final List<SettingType> VOLUME_SETTINGS = Arrays.asList(
            SettingType.VOLUME_CLICKER, SettingType.VOLUME_MILESTONE_CURRENT,
            SettingType.VOLUME_MILESTONE_TOTAL, SettingType.VOLUME_MILESTONE_ENTROPY,
            SettingType.VOLUME_BUY,
            SettingType.VOLUME_FRIEND_REQUEST, SettingType.VOLUME_FRIEND_JOIN,
            SettingType.VOLUME_FRIEND_LEAVE, SettingType.VOLUME_RANDO_JOIN,
            SettingType.VOLUME_RANDO_LEAVE, SettingType.VOLUME_FRIEND_CLICKER,
            SettingType.VOLUME_RANDO_CLICKER
    );

    public SettingsVolumeGui(Player player, PlayerData data) {
        this(player, data, p -> new PlayerSettingsGui(p, data, p2 -> new SettingsMainGui(p2, data).open()).open());
    }

    public SettingsVolumeGui(Player player, PlayerData data, Consumer<Player> backToPlayerSettings) {
        super(player, "settings-volume", MonitorStyle.title(ChatColor.AQUA, "Sound Volumes"), 0);
        this.data = data;
        this.backToPlayerSettings = backToPlayerSettings;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(backToPlayerSettings);

        populatePage(VOLUME_SETTINGS, type -> {
            int rawVal = data.getSettings().get(type);
            double volume = rawVal / 100.0;
            String volStr = String.format("%.2f", volume);

            Material mat;
            if (volume <= 0) mat = Material.RED_STAINED_GLASS_PANE;
            else if (volume >= 2.0) mat = Material.LIME_STAINED_GLASS_PANE;
            else mat = Material.YELLOW_STAINED_GLASS_PANE;

            Icon icon = GuiHelper.createIcon(mat,
                    ChatColor.YELLOW + type.displayName(),
                    "",
                    ChatColor.GRAY + "Volume: " + ChatColor.WHITE + volStr,
                    "",
                    ChatColor.YELLOW + "Click to adjust");
            icon.onClick(e -> new VolumeAdjustGui(player, data, type, backToPlayerSettings).open());
            return icon;
        });

        addPaginationArrows(VOLUME_SETTINGS, newPage -> {}); // single page, no-op
    }
}
