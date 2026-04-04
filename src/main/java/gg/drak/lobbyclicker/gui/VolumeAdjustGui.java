package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.SimpleGuiMonitor;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import gg.drak.lobbyclicker.settings.SettingType;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.function.Consumer;

/**
 * Granular volume adjustment GUI for a specific sound setting.
 * Allows +/- 0.01, +/- 0.1, +/- 1.0. Range: 0.00 to 5.00.
 * Volume is stored as an int (value * 100) in the setting.
 */
public class VolumeAdjustGui extends SimpleGuiMonitor {
    private final PlayerData data;
    private final SettingType volumeType;
    private final Consumer<Player> backToPlayerSettings;

    public VolumeAdjustGui(Player player, PlayerData data, SettingType volumeType) {
        this(player, data, volumeType, p -> new PlayerSettingsGui(p, data, p2 -> new SettingsMainGui(p2, data).open()).open());
    }

    public VolumeAdjustGui(Player player, PlayerData data, SettingType volumeType, Consumer<Player> backToPlayerSettings) {
        super(player, "volume-adjust", MonitorStyle.title(ChatColor.AQUA, "Volume: " + volumeType.displayName()), 3);
        this.data = data;
        this.volumeType = volumeType;
        this.backToPlayerSettings = backToPlayerSettings;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new SettingsVolumeGui(p, data, backToPlayerSettings).open());

        buildDisplay();
    }

    private void buildDisplay() {
        int rawValue = data.getSettings().get(volumeType); // stored as int (value * 100)
        double volume = rawValue / 100.0;
        String volStr = String.format("%.2f", volume);

        // Center display
        setContent(3, GuiHelper.createIcon(Material.NOTE_BLOCK,
                ChatColor.AQUA + "" + ChatColor.BOLD + volumeType.displayName(),
                "",
                ChatColor.GRAY + "Current Volume: " + ChatColor.WHITE + volStr,
                ChatColor.GRAY + "Range: " + ChatColor.WHITE + "0.00 - 5.00",
                "",
                ChatColor.GRAY + "Use buttons to adjust"));

        // -1.0 (content 0)
        Icon minus1 = GuiHelper.createIcon(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "-1.0");
        minus1.onClick(e -> adjust(-100));
        setContent(0, minus1);

        // -0.1 (content 1)
        Icon minus01 = GuiHelper.createIcon(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "-0.1");
        minus01.onClick(e -> adjust(-10));
        setContent(1, minus01);

        // -0.01 (content 2)
        Icon minus001 = GuiHelper.createIcon(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "-0.01");
        minus001.onClick(e -> adjust(-1));
        setContent(2, minus001);

        // +0.01 (content 4)
        Icon plus001 = GuiHelper.createIcon(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "+0.01");
        plus001.onClick(e -> adjust(1));
        setContent(4, plus001);

        // +0.1 (content 5)
        Icon plus01 = GuiHelper.createIcon(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "+0.1");
        plus01.onClick(e -> adjust(10));
        setContent(5, plus01);

        // +1.0 (content 6)
        Icon plus1 = GuiHelper.createIcon(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "+1.0");
        plus1.onClick(e -> adjust(100));
        setContent(6, plus1);
    }

    private void adjust(int delta) {
        int rawValue = data.getSettings().get(volumeType);
        int newValue = Math.max(0, Math.min(500, rawValue + delta)); // 0 to 500 (0.00 to 5.00)
        data.getSettings().set(volumeType, newValue);
        data.save(true);
        RedisSyncHandler.publishSettingsSync(data);

        // Preview sound
        float vol = newValue / 100.0f;
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, vol, 1.0f);

        new VolumeAdjustGui(player, data, volumeType, backToPlayerSettings).open();
    }
}
