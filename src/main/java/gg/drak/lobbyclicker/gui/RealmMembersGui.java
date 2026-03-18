package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MenuMonitor;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class RealmMembersGui extends MenuMonitor {
    private final PlayerData data;

    public RealmMembersGui(Player player, PlayerData data) {
        super(player, "realm-members", MonitorStyle.title(ChatColor.GREEN, "Manage Members"), MonitorStyle.ROWS_SMALL);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);

        Icon friends = MonitorStyle.menuButton(Material.TOTEM_OF_UNDYING, ChatColor.GREEN,
                "Manage Friends", "View and manage friends", "Includes non-contributors");
        friends.onClick(e -> new RealmMemberListGui(player, data, true, 0).open());
        addOption(friends);

        Icon all = MonitorStyle.menuButton(Material.PLAYER_HEAD, ChatColor.AQUA,
                "Manage All", "All contributors + friends", "Paginated list");
        all.onClick(e -> new RealmMemberListGui(player, data, false, 0).open());
        addOption(all);

        buildMenu(p -> new RealmSettingsGui(p, data).open());
    }
}
