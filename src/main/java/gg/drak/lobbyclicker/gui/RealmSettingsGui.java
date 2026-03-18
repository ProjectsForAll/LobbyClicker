package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MenuMonitor;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import gg.drak.lobbyclicker.settings.SettingType;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class RealmSettingsGui extends MenuMonitor {
    private final PlayerData data;

    public RealmSettingsGui(Player player, PlayerData data) {
        super(player, "realm-settings", MonitorStyle.title(ChatColor.LIGHT_PURPLE, "Realm Settings"), MonitorStyle.ROWS_SMALL);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);

        Icon members = MonitorStyle.menuButton(Material.PLAYER_HEAD, ChatColor.GREEN,
                "Manage Members", "Manage friends and contributors");
        members.onClick(e -> new RealmMembersGui(player, data).open());
        addOption(members);

        boolean isPublic = data.isRealmPublic();
        Icon publicToggle = MonitorStyle.toggleButton("Public Realm", isPublic, "Let anyone visit your realm");
        publicToggle.onClick(e -> {
            data.setRealmPublic(!data.isRealmPublic());
            data.getSettings().set(SettingType.PUBLIC_FARM, data.isRealmPublic() ? 1 : 0);
            data.save(true);
            RedisSyncHandler.publishSettingsSync(data);
            new RealmSettingsGui(player, data).open();
        });
        addOption(publicToggle);

        Icon reset = MonitorStyle.menuButton(Material.TNT, ChatColor.RED,
                "Reset Realm", "Reset all realm data", "Keeps: settings, friends");
        reset.onClick(e -> new RealmResetConfirmGui(player, data).open());
        addOption(reset);

        buildMenu(p -> new SettingsMainGui(p, data).open());
    }
}
