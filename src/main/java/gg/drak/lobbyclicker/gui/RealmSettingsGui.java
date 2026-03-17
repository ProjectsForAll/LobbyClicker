package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import gg.drak.lobbyclicker.settings.SettingType;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class RealmSettingsGui extends BaseGui {
    private final PlayerData data;

    public RealmSettingsGui(Player player, PlayerData data) {
        super(player, "realm-settings", ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Realm Settings", 3);
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

        // Manage Members
        Icon members = GuiHelper.createIcon(Material.PLAYER_HEAD,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Manage Members",
                "", ChatColor.GRAY + "Manage friends and contributors");
        members.onClick(e -> new RealmMembersGui(player, data).open());
        addItem(11, members);

        // Realm Public toggle
        boolean isPublic = data.isRealmPublic();
        Icon publicToggle = GuiHelper.createIcon(
                isPublic ? Material.LIME_DYE : Material.GRAY_DYE,
                ChatColor.YELLOW + "Public Realm " + (isPublic ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"),
                "", ChatColor.GRAY + "Let anyone visit your realm", "", ChatColor.GRAY + "Click to toggle");
        publicToggle.onClick(e -> {
            data.setRealmPublic(!data.isRealmPublic());
            data.getSettings().set(SettingType.PUBLIC_FARM, data.isRealmPublic() ? 1 : 0);
            data.save(true);
            RedisSyncHandler.publishSettingsSync(data);
            new RealmSettingsGui(player, data).open();
        });
        addItem(13, publicToggle);

        // Reset Realm
        Icon reset = GuiHelper.createIcon(Material.TNT,
                ChatColor.RED + "" + ChatColor.BOLD + "Reset Realm",
                "", ChatColor.GRAY + "Reset all realm data", ChatColor.RED + "Keeps: settings, friends");
        reset.onClick(e -> new RealmResetConfirmGui(player, data).open());
        addItem(15, reset);

        // Back
        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new SettingsMainGui(player, data).open());
        addItem(22, back);
    }
}
