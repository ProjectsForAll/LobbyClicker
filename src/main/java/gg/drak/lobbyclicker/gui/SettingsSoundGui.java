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

import java.util.function.Consumer;

public class SettingsSoundGui extends SimpleGuiMonitor {
    private final PlayerData data;
    private final Consumer<Player> backToPlayerSettings;

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
        this(player, data, p -> new PlayerSettingsGui(p, data, p2 -> new SettingsMainGui(p2, data).open()).open());
    }

    public SettingsSoundGui(Player player, PlayerData data, Consumer<Player> backToPlayerSettings) {
        super(player, "settings-sound", MonitorStyle.title("green", "Sound Toggles"), MonitorStyle.ROWS_FULL);
        this.data = data;
        this.backToPlayerSettings = backToPlayerSettings;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(backToPlayerSettings);

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
                new SettingsSoundGui(player, data, backToPlayerSettings).open();
            });
            addItem(contentSlots[i], icon);
        }
    }
}
