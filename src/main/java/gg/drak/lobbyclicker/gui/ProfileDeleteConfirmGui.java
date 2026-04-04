package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.ConfirmationMonitor;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.realm.ProfileManager;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class ProfileDeleteConfirmGui extends ConfirmationMonitor {
    private final PlayerData data;
    private final RealmProfile profile;

    public ProfileDeleteConfirmGui(Player player, PlayerData data, RealmProfile profile) {
        super(player, "profile-delete", MonitorStyle.title("red", "Delete Profile?"));
        this.data = data;
        this.profile = profile;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);

        Icon info = GuiHelper.createIcon(Material.FLINT_AND_STEEL,
                ChatColor.RED + "" + ChatColor.BOLD + "Delete: " + profile.getProfileName(),
                "",
                ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(profile.getCookies()),
                ChatColor.GRAY + "Prestige: " + ChatColor.WHITE + profile.getPrestigeLevel(),
                "",
                ChatColor.RED + "" + ChatColor.BOLD + "This will permanently delete",
                ChatColor.RED + "" + ChatColor.BOLD + "this profile and all its data!");

        buildConfirmation(info, "Delete Profile", "This cannot be undone!", p -> {
            ProfileManager.unloadProfile(profile.getProfileId());
            LobbyClicker.getDatabase().deleteProfileFromDb(profile.getProfileId());
            p.sendMessage(ChatColor.RED + "Deleted profile: " + ChatColor.WHITE + profile.getProfileName());
            p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            new ProfileSelectorGui(p, data).open();
        }, p -> new ProfileManageGui(p, data, profile).open());
    }
}
