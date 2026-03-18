package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.SimpleGuiMonitor;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import gg.drak.lobbyclicker.settings.SettingType;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class SettingsSoundGui extends SimpleGuiMonitor {
    private final PlayerData data;

    private static final SettingType[] SOUND_SETTINGS = {
            SettingType.SOUND_MASTER, SettingType.SOUND_CLICKER,
            SettingType.SOUND_MILESTONE_CURRENT, SettingType.SOUND_MILESTONE_TOTAL,
            SettingType.SOUND_MILESTONE_ENTROPY,
            SettingType.SOUND_BUY, SettingType.SOUND_FRIEND_REQUEST,
            SettingType.SOUND_FRIEND_JOIN, SettingType.SOUND_FRIEND_LEAVE,
            SettingType.SOUND_RANDO_JOIN, SettingType.SOUND_RANDO_LEAVE,
            SettingType.SOUND_FRIEND_CLICKER, SettingType.SOUND_RANDO_CLICKER,
    };

    public SettingsSoundGui(Player player, PlayerData data) {
        super(player, "settings-sound", MonitorStyle.title(ChatColor.GREEN, "Sound Toggles"), MonitorStyle.ROWS_FULL);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new PlayerSettingsGui(p, data).open());

        int[] contentSlots = getContentSlots();
        for (int i = 0; i < SOUND_SETTINGS.length && i < contentSlots.length; i++) {
            SettingType type = SOUND_SETTINGS[i];
            boolean on = data.getSettings().getBool(type);
            Material mat = on ? Material.LIME_DYE : Material.GRAY_DYE;
            String status = on ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF";
            String masterNote = type == SettingType.SOUND_MASTER ? ChatColor.DARK_GRAY + "(Master Switch)" : "";

            Icon icon = GuiHelper.createIcon(mat,
                    ChatColor.YELLOW + type.displayName() + " " + status,
                    masterNote, "", ChatColor.GRAY + "Click to toggle");
            icon.onClick(e -> {
                data.getSettings().toggle(type);
                data.save(true);
                RedisSyncHandler.publishSettingsSync(data);
                new SettingsSoundGui(player, data).open();
            });
            addItem(contentSlots[i], icon);
        }
    }
}
