package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class RealmMembersGui extends BaseGui {
    private final PlayerData data;

    public RealmMembersGui(Player player, PlayerData data) {
        super(player, "realm-members", ChatColor.GREEN + "" + ChatColor.BOLD + "Manage Members", 3);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        // Home button
        Icon home = GuiHelper.homeButton();
        home.onClick(e -> new ClickerGui(player, data).open());
        addItem(0, home);

        // Manage Friends
        Icon friends = GuiHelper.createIcon(Material.TOTEM_OF_UNDYING,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Manage Friends",
                "", ChatColor.GRAY + "View and manage friends",
                ChatColor.GRAY + "Includes non-contributors");
        friends.onClick(e -> new RealmMemberListGui(player, data, true, 0).open());
        addItem(11, friends);

        // Manage All
        Icon all = GuiHelper.createIcon(Material.PLAYER_HEAD,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Manage All",
                "", ChatColor.GRAY + "All contributors + friends",
                ChatColor.GRAY + "Paginated list");
        all.onClick(e -> new RealmMemberListGui(player, data, false, 0).open());
        addItem(15, all);

        // Back
        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new RealmSettingsGui(player, data).open());
        addItem(22, back);
    }
}
