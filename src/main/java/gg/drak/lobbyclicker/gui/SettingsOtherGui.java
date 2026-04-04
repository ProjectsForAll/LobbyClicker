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

public class SettingsOtherGui extends SimpleGuiMonitor {
    private final PlayerData data;
    private final Consumer<Player> backToPlayerSettings;

    private static final SettingType[] SETTINGS = {
            SettingType.ALLOW_FRIEND_REQUESTS,
            SettingType.AUTO_ACCEPT_FRIENDS,
            SettingType.PUBLIC_FARM,
            SettingType.ALLOW_FRIEND_JOINS,
            SettingType.ALLOW_OFFLINE_REALM,
    };

    private static final String[] DESCRIPTIONS = {
            "Allow others to send you friend requests",
            "Automatically accept incoming friend requests",
            "Let anyone visit your clicker realm",
            "Let friends visit your clicker realm",
            "Let friends visit your realm while you're offline",
    };

    public SettingsOtherGui(Player player, PlayerData data) {
        this(player, data, p -> new PlayerSettingsGui(p, data, p2 -> new SettingsMainGui(p2, data).open()).open());
    }

    public SettingsOtherGui(Player player, PlayerData data, Consumer<Player> backToPlayerSettings) {
        super(player, "settings-other", MonitorStyle.title(ChatColor.RED, "Other Settings"), MonitorStyle.ROWS_SMALL);
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
        for (int i = 0; i < SETTINGS.length && i < contentSlots.length; i++) {
            SettingType type = SETTINGS[i];
            boolean on = data.getSettings().getBool(type);

            Icon icon = MonitorStyle.toggleButton(type.displayName(), on, DESCRIPTIONS[i]);
            icon.onClick(e -> {
                data.getSettings().toggle(type);
                if (type == SettingType.PUBLIC_FARM) {
                    data.setRealmPublic(data.getSettings().getBool(SettingType.PUBLIC_FARM));
                }
                data.save(true);
                RedisSyncHandler.publishSettingsSync(data);
                new SettingsOtherGui(player, data, backToPlayerSettings).open();
            });
            addItem(contentSlots[i], icon);
        }
    }
}
