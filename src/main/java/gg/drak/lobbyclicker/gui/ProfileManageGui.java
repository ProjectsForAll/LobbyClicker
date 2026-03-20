package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.SimpleGuiMonitor;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

/**
 * Profile management GUI — Realm Settings for a specific profile.
 * Options: Delete, Rename, Manage Members, Public Realm toggle, Reset Realm.
 */
public class ProfileManageGui extends SimpleGuiMonitor {
    private final PlayerData data;
    private final RealmProfile profile;

    public ProfileManageGui(Player player, PlayerData data, RealmProfile profile) {
        super(player, "profile-manage",
                MonitorStyle.title(ChatColor.LIGHT_PURPLE, profile.getProfileName() + " Settings"),
                MonitorStyle.ROWS_SMALL);
        this.data = data;
        this.profile = profile;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        Player player = (Player) event.getPlayer();
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new ProfileSelectorGui(p, data).open());

        boolean isActive = profile.getProfileId().equals(data.getActiveProfileId());

        // Delete
        Icon delete = GuiHelper.createIcon(Material.FLINT_AND_STEEL,
                ChatColor.RED + "" + ChatColor.BOLD + "Delete?",
                "", ChatColor.GRAY + "Permanently delete this profile",
                "", ChatColor.RED + "This cannot be undone!");
        if (isActive) {
            // Can't delete the active profile
            delete = GuiHelper.createIcon(Material.FLINT_AND_STEEL,
                    ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Delete?",
                    "", ChatColor.RED + "Cannot delete the active profile",
                    ChatColor.GRAY + "Switch to another profile first.");
        } else {
            delete.onClick(e -> new ProfileDeleteConfirmGui(player, data, profile).open());
        }
        setContent(0, delete);

        // Rename
        Icon rename = GuiHelper.createIcon(Material.NAME_TAG,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Rename",
                "", ChatColor.GRAY + "Current: " + ChatColor.WHITE + profile.getProfileName(),
                "", ChatColor.YELLOW + "Click to rename");
        rename.onClick(e -> {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Type the new profile name in chat. Type " + ChatColor.WHITE + "cancel" + ChatColor.YELLOW + " to cancel.");
            gg.drak.lobbyclicker.utils.ChatInput.request(player, input -> {
                if (input == null || input.equalsIgnoreCase("cancel")) {
                    new ProfileManageGui(player, data, profile).open();
                    return;
                }
                String newName = input.trim();
                if (newName.length() > 32) newName = newName.substring(0, 32);
                if (newName.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Invalid name.");
                    new ProfileManageGui(player, data, profile).open();
                    return;
                }
                profile.setProfileName(newName);
                data.save(true);
                player.sendMessage(ChatColor.GREEN + "Profile renamed to: " + ChatColor.WHITE + newName);
                new ProfileManageGui(player, data, profile).open();
            });
        });
        setContent(2, rename);

        // Manage Members (only if this is the active profile)
        if (isActive) {
            Icon members = GuiHelper.createIcon(Material.PLAYER_HEAD,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "Manage Members",
                    "", ChatColor.GRAY + "Manage friends and contributors");
            members.onClick(e -> new RealmMembersGui(player, data).open());
            setContent(3, members);
        } else {
            setContent(3, GuiHelper.createIcon(Material.GRAY_DYE,
                    ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Manage Members",
                    "", ChatColor.RED + "Switch to this profile first"));
        }

        // Public Realm toggle
        boolean isPublic = profile.isRealmPublic();
        Icon publicToggle = MonitorStyle.toggleButton("Public Realm", isPublic, "Let anyone visit this realm");
        publicToggle.onClick(e -> {
            profile.setRealmPublic(!profile.isRealmPublic());
            data.save(true);
            new ProfileManageGui(player, data, profile).open();
        });
        setContent(4, publicToggle);

        // Reset Realm
        Icon reset = GuiHelper.createIcon(Material.FLINT_AND_STEEL,
                ChatColor.RED + "" + ChatColor.BOLD + "Reset Realm?",
                "", ChatColor.GRAY + "Reset all realm data",
                ChatColor.GRAY + "Keeps: settings, friends");
        if (isActive) {
            reset.onClick(e -> new RealmResetConfirmGui(player, data).open());
        } else {
            reset = GuiHelper.createIcon(Material.FLINT_AND_STEEL,
                    ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Reset Realm?",
                    "", ChatColor.RED + "Switch to this profile first");
        }
        setContent(6, reset);
    }
}
