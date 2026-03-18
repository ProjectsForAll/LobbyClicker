package gg.drak.lobbyclicker.gui.admin;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.GuiHelper;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.realm.ProfileManager;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.List;

/**
 * Admin GUI for managing a player's profiles. Allows viewing details and deleting profiles.
 */
public class AdminProfilesGui extends PaginationMonitor {
    private final String targetUuid;
    private final String targetName;

    public AdminProfilesGui(Player player, String targetUuid, String targetName) {
        this(player, targetUuid, targetName, 0);
    }

    public AdminProfilesGui(Player player, String targetUuid, String targetName, int page) {
        super(player, "admin-profiles", ChatColor.DARK_RED + "" + ChatColor.BOLD + targetName + "'s Profiles", page);
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        fillMonitorBorder();

        // Custom action bar
        int b = (getSize() / 9 - 1) * 9;
        Icon back = GuiHelper.createIcon(Material.DARK_OAK_DOOR,
                ChatColor.RED + "" + ChatColor.BOLD + "Back", "", ChatColor.GRAY + "Back to player list");
        back.onClick(e -> new AdminPlayerListGui(player).open());
        addItem(b + 7, back);
        Icon close = GuiHelper.createIcon(Material.BARRIER, ChatColor.RED + "Close");
        close.onClick(e -> player.closeInventory());
        addItem(b + 8, close);

        // Title
        addItem(4, GuiHelper.createIcon(Material.PLAYER_HEAD,
                ChatColor.GOLD + "" + ChatColor.BOLD + targetName + "'s Profiles",
                "",
                ChatColor.GRAY + "UUID: " + ChatColor.DARK_GRAY + targetUuid,
                "",
                ChatColor.RED + "Shift-click a profile to DELETE it",
                ChatColor.GRAY + "Left-click to view details"));

        // Load profiles - try loaded first, then pull from DB
        List<RealmProfile> profiles = ProfileManager.getProfilesForOwner(targetUuid);

        if (profiles.isEmpty()) {
            // Try loading from DB
            player.sendMessage(ChatColor.YELLOW + "Loading profiles from database...");
            LobbyClicker.getDatabase().pullProfilesByOwnerThreaded(targetUuid).thenAccept(dbProfiles -> {
                if (dbProfiles.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "No profiles found for " + targetName);
                } else {
                    for (RealmProfile p : dbProfiles) {
                        ProfileManager.loadProfile(p);
                    }
                    org.bukkit.Bukkit.getScheduler().runTask(LobbyClicker.getInstance(), () ->
                            new AdminProfilesGui(player, targetUuid, targetName, page).open());
                }
            });
            return;
        }

        populatePagedContent(profiles, (profile, slot) -> {
            PlayerData targetData = PlayerManager.getPlayer(targetUuid).orElse(null);
            boolean isActive = targetData != null && profile.getProfileId().equals(targetData.getActiveProfileId());

            Material mat = isActive ? Material.ENCHANTED_GOLDEN_APPLE : Material.CHEST_MINECART;
            String activeTag = isActive ? ChatColor.GREEN + " (Active)" : "";

            Icon icon = GuiHelper.createIcon(mat,
                    ChatColor.GOLD + "" + ChatColor.BOLD + profile.getProfileName() + activeTag,
                    "",
                    ChatColor.GRAY + "Profile ID: " + ChatColor.DARK_GRAY + profile.getProfileId().substring(0, 8) + "...",
                    ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(profile.getCookies()),
                    ChatColor.GRAY + "Total Earned: " + ChatColor.WHITE + FormatUtils.format(profile.getTotalCookiesEarned()),
                    ChatColor.GRAY + "Prestige: " + ChatColor.WHITE + profile.getPrestigeLevel(),
                    ChatColor.GRAY + "Aura: " + ChatColor.WHITE + FormatUtils.format(profile.getAura()),
                    ChatColor.GRAY + "Realm Clicks: " + ChatColor.WHITE + FormatUtils.format(profile.getTimesClicked()),
                    "",
                    isActive ? ChatColor.YELLOW + "This is the player's active profile" : "",
                    ChatColor.RED + "" + ChatColor.BOLD + "SHIFT-CLICK to DELETE");

            icon.onClick(e -> {
                if (e.isShiftClick()) {
                    // Delete the profile
                    if (isActive) {
                        player.sendMessage(ChatColor.RED + "Cannot delete the player's active profile!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }
                    if (profiles.size() <= 1) {
                        player.sendMessage(ChatColor.RED + "Cannot delete the player's only profile!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }

                    // Remove from memory and DB
                    ProfileManager.unloadProfile(profile.getProfileId());
                    LobbyClicker.getDatabase().deleteProfileFromDb(profile.getProfileId());

                    player.sendMessage(ChatColor.RED + "Deleted profile: " + ChatColor.WHITE + profile.getProfileName()
                            + ChatColor.RED + " (" + profile.getProfileId().substring(0, 8) + "...)");
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1f);

                    // Refresh
                    new AdminProfilesGui(player, targetUuid, targetName, page).open();
                } else {
                    // Just show info in chat
                    player.sendMessage(ChatColor.GOLD + "--- Profile: " + profile.getProfileName() + " ---");
                    player.sendMessage(ChatColor.GRAY + "ID: " + ChatColor.WHITE + profile.getProfileId());
                    player.sendMessage(ChatColor.GRAY + "Owner: " + ChatColor.WHITE + targetName + " (" + targetUuid + ")");
                    player.sendMessage(ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(profile.getCookies()));
                    player.sendMessage(ChatColor.GRAY + "Total Earned: " + ChatColor.WHITE + FormatUtils.format(profile.getTotalCookiesEarned()));
                    player.sendMessage(ChatColor.GRAY + "Prestige: " + ChatColor.WHITE + profile.getPrestigeLevel());
                    player.sendMessage(ChatColor.GRAY + "Aura: " + ChatColor.WHITE + FormatUtils.format(profile.getAura()));
                    player.sendMessage(ChatColor.GRAY + "Realm Clicks: " + ChatColor.WHITE + profile.getTimesClicked());
                    player.sendMessage(ChatColor.GRAY + "Owner Clicks: " + ChatColor.WHITE + profile.getOwnerClicks());
                    player.sendMessage(ChatColor.GRAY + "Other Clicks: " + ChatColor.WHITE + profile.getOtherClicks());
                }
            });
            addItem(slot, icon);
        });

        addPaginationArrows(profiles, newPage -> new AdminProfilesGui(player, targetUuid, targetName, newPage).open());
    }
}
