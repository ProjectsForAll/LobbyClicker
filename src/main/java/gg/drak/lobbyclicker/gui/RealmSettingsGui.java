package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MenuMonitor;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.realm.RealmProfile;
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

        // Delete
        RealmProfile profile = data.getActiveProfile();
        Icon delete = GuiHelper.createIcon(Material.FLINT_AND_STEEL,
                ChatColor.RED + "" + ChatColor.BOLD + "Delete?",
                "", ChatColor.GRAY + "Permanently delete this profile");
        if (profile != null) {
            delete.onClick(e -> new ProfileDeleteConfirmGui(player, data, profile).open());
        }
        addOption(delete);

        // Rename
        Icon rename = GuiHelper.createIcon(Material.NAME_TAG,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Rename",
                "", ChatColor.GRAY + "Rename this realm profile");
        rename.onClick(e -> {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Type the new profile name in chat. Type " + ChatColor.WHITE + "cancel" + ChatColor.YELLOW + " to cancel.");
            gg.drak.lobbyclicker.utils.ChatInput.request(player, input -> {
                if (input == null || input.equalsIgnoreCase("cancel")) {
                    new RealmSettingsGui(player, data).open();
                    return;
                }
                String newName = input.trim();
                if (newName.length() > 32) newName = newName.substring(0, 32);
                if (newName.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Invalid name.");
                    new RealmSettingsGui(player, data).open();
                    return;
                }
                if (profile != null) {
                    profile.setProfileName(newName);
                    data.save(true);
                    player.sendMessage(ChatColor.GREEN + "Profile renamed to: " + ChatColor.WHITE + newName);
                }
                new RealmSettingsGui(player, data).open();
            });
        });
        addOption(rename);

        // Manage Members
        Icon members = MonitorStyle.menuButton(Material.PLAYER_HEAD, ChatColor.GREEN,
                "Manage Members", "Manage friends and contributors");
        members.onClick(e -> new RealmMembersGui(player, data).open());
        addOption(members);

        // Public Realm toggle
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

        // Reset Realm
        Icon reset = GuiHelper.createIcon(Material.FLINT_AND_STEEL,
                ChatColor.RED + "" + ChatColor.BOLD + "Reset Realm?",
                "", ChatColor.GRAY + "Reset all realm data",
                ChatColor.GRAY + "Keeps: settings, friends");
        reset.onClick(e -> new RealmResetConfirmGui(player, data).open());
        addOption(reset);

        buildMenu(p -> new SettingsMainGui(p, data).open());
    }
}
