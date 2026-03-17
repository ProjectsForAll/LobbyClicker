package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
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
 * Profile selector GUI. Shown when a player does /clicker for the first time,
 * or when they click the "Profiles" button in the main clicker GUI.
 * Players select which profile slot to play on.
 */
public class ProfileSelectorGui extends BaseGui {
    private final PlayerData data;
    private final int page;

    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public ProfileSelectorGui(Player player, PlayerData data) {
        this(player, data, 0);
    }

    public ProfileSelectorGui(Player player, PlayerData data, int page) {
        super(player, "profile-selector",
                ChatColor.GOLD + "" + ChatColor.BOLD + "Profiles" + (page > 0 ? " (Page " + (page + 1) + ")" : ""),
                6);
        this.data = data;
        this.page = page;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        // Title info
        int maxProfiles = data.getMaxProfiles();
        List<RealmProfile> profiles = ProfileManager.getProfilesForOwner(data.getIdentifier());

        addItem(4, GuiHelper.createIcon(Material.BOOK,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Your Profiles",
                "",
                ChatColor.GRAY + "Profiles: " + ChatColor.WHITE + profiles.size() + "/" + maxProfiles,
                "",
                ChatColor.YELLOW + "Select a profile to play on"));

        // Display profiles
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, profiles.size());

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex >= ITEM_SLOTS.length) break;

            RealmProfile profile = profiles.get(i);
            boolean isActive = profile.getProfileId().equals(data.getActiveProfileId());

            Material mat = isActive ? Material.ENCHANTED_GOLDEN_APPLE : Material.COOKIE;
            ChatColor nameColor = isActive ? ChatColor.GREEN : ChatColor.GOLD;
            String activeTag = isActive ? ChatColor.GREEN + " (Active)" : "";

            Icon icon = GuiHelper.createIcon(mat,
                    nameColor + "" + ChatColor.BOLD + profile.getProfileName() + activeTag,
                    "",
                    ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(profile.getCookies()),
                    ChatColor.GRAY + "Total Earned: " + ChatColor.WHITE + FormatUtils.format(profile.getTotalCookiesEarned()),
                    ChatColor.GRAY + "Prestige: " + ChatColor.WHITE + profile.getPrestigeLevel(),
                    ChatColor.GRAY + "Aura: " + ChatColor.WHITE + FormatUtils.format(profile.getAura()),
                    "",
                    isActive ? ChatColor.GREEN + "Currently selected" : ChatColor.YELLOW + "Click to select");

            icon.onClick(e -> {
                data.setActiveProfileId(profile.getProfileId());
                data.save(true);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                new ClickerGui(player, data).open();
            });

            addItem(ITEM_SLOTS[slotIndex], icon);
        }

        // Create new profile button (if under limit)
        if (profiles.size() < maxProfiles) {
            Icon create = GuiHelper.createIcon(Material.LIME_WOOL,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "Create New Profile",
                    "",
                    ChatColor.GRAY + "Create a new realm profile",
                    ChatColor.GRAY + "Slots: " + ChatColor.WHITE + profiles.size() + "/" + maxProfiles);
            create.onClick(e -> {
                if (ProfileManager.getProfilesForOwner(data.getIdentifier()).size() >= data.getMaxProfiles()) {
                    player.sendMessage(ChatColor.RED + "You've reached your maximum number of profiles.");
                    return;
                }
                String name = "Realm " + (profiles.size() + 1);
                RealmProfile newProfile = ProfileManager.createProfile(data.getIdentifier(), name);
                // Save new profile to DB
                LobbyClicker.getDatabase().putProfileThreaded(newProfile);
                // Set as active
                data.setActiveProfileId(newProfile.getProfileId());
                data.save(true);
                player.sendMessage(ChatColor.GREEN + "Created profile: " + ChatColor.WHITE + name);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                new ClickerGui(player, data).open();
            });
            addItem(49, create);
        }

        // Pagination
        if (page > 0) {
            Icon prev = GuiHelper.createIcon(Material.ARROW, ChatColor.YELLOW + "Previous Page");
            prev.onClick(e -> new ProfileSelectorGui(player, data, page - 1).open());
            addItem(45, prev);
        }
        if (end < profiles.size()) {
            Icon next = GuiHelper.createIcon(Material.ARROW, ChatColor.YELLOW + "Next Page");
            next.onClick(e -> new ProfileSelectorGui(player, data, page + 1).open());
            addItem(53, next);
        }

        // Close
        Icon close = GuiHelper.createIcon(Material.BARRIER, ChatColor.RED + "Close");
        close.onClick(e -> player.closeInventory());
        addItem(8, close);
    }
}
