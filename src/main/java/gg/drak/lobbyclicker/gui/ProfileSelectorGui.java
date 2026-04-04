package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Profile selector GUI. Shows profile slots as minecarts:
 * - Chest Minecart: slot with data
 * - Minecart (empty): available slot (no data yet)
 * - TNT Minecart: locked slot (no permission)
 *
 * Page limit: can only go 1 page past the page containing the highest used slot.
 * Back button: goes to loaded realm if active, otherwise close.
 */
public class ProfileSelectorGui extends PaginationMonitor {
    private final PlayerData data;
    private final PlayerData realmOwner;

    public ProfileSelectorGui(Player player, PlayerData data) {
        this(player, data, 0, null);
    }

    public ProfileSelectorGui(Player player, PlayerData data, PlayerData realmOwner) {
        this(player, data, 0, realmOwner);
    }

    public ProfileSelectorGui(Player player, PlayerData data, int page) {
        this(player, data, page, null);
    }

    public ProfileSelectorGui(Player player, PlayerData data, int page, PlayerData realmOwner) {
        super(player, "profile-selector", MonitorStyle.title("gold", "Profiles"), page);
        this.data = data;
        this.realmOwner = realmOwner;
        setWrapAround(false);
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        Player player = (Player) event.getPlayer();

        setPlayerContext(data, realmOwner);
        fillMonitorBorder();

        // Back: go to loaded realm if active, else close
        if (data.hasActiveProfile()) {
            buildStandardActionBar(p -> new ClickerGui(p, data).open());
        } else {
            // Replace back button with close/barrier
            buildStandardActionBar(p -> p.closeInventory());
            int b = (getSize() / 9 - 1) * 9;
            Icon close = GuiHelper.createIcon(Material.BARRIER,
                    ChatColor.RED + "" + ChatColor.BOLD + "Close");
            close.onClick(e -> player.closeInventory());
            addItem(b + 7, close); // overwrite back button slot
        }

        int maxProfiles = data.getMaxProfiles();
        List<RealmProfile> profiles = ProfileManager.getProfilesForOwner(data.getIdentifier());

        // Title info at top
        addItem(4, GuiHelper.createIcon(Material.BOOK,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Your Profiles",
                "",
                ChatColor.GRAY + "Profiles: " + ChatColor.WHITE + profiles.size() + "/" + maxProfiles,
                "",
                ChatColor.YELLOW + "Select a profile to play on"));

        // Build slot list: profiles + empty slots + locked slots
        List<SlotEntry> slots = new ArrayList<>();
        for (RealmProfile profile : profiles) {
            slots.add(new SlotEntry(profile, SlotState.HAS_DATA));
        }
        // Fill empty available slots
        for (int i = profiles.size(); i < maxProfiles; i++) {
            slots.add(new SlotEntry(null, SlotState.AVAILABLE));
        }
        // Fill locked slots to pad the page
        int totalVisible = maxProfiles + 5; // show a few locked slots beyond limit
        for (int i = maxProfiles; i < totalVisible; i++) {
            slots.add(new SlotEntry(null, SlotState.LOCKED));
        }

        // Enforce page limit: can only go 1 page past the page with highest used slot
        int highestUsedIndex = profiles.size() - 1;
        int highestUsedPage = highestUsedIndex >= 0 ? highestUsedIndex / getItemsPerPage() : 0;
        int maxPage = highestUsedPage + 1;
        // Clamp slots list to not exceed maxPage+1 pages worth
        int maxItems = (maxPage + 1) * getItemsPerPage();
        if (slots.size() > maxItems) {
            slots = new ArrayList<>(slots.subList(0, maxItems));
        }

        populatePagedContent(slots, (entry, slot) -> {
            Icon icon;
            switch (entry.state) {
                case HAS_DATA: {
                    RealmProfile profile = entry.profile;
                    boolean isActive = profile.getProfileId().equals(data.getActiveProfileId());
                    ChatColor nameColor = isActive ? ChatColor.GREEN : ChatColor.GOLD;
                    String activeTag = isActive ? ChatColor.GREEN + " (Active)" : "";

                    icon = GuiHelper.createIcon(Material.CHEST_MINECART,
                            nameColor + "" + ChatColor.BOLD + profile.getProfileName() + activeTag,
                            "",
                            ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(profile.getCookies()),
                            ChatColor.GRAY + "Total Earned: " + ChatColor.WHITE + FormatUtils.format(profile.getTotalCookiesEarned()),
                            ChatColor.GRAY + "Prestige: " + ChatColor.WHITE + profile.getPrestigeLevel(),
                            ChatColor.GRAY + "Aura: " + ChatColor.WHITE + FormatUtils.format(profile.getAura()),
                            "",
                            isActive ? ChatColor.GREEN + "Currently selected" : ChatColor.YELLOW + "Left-click to select",
                            ChatColor.LIGHT_PURPLE + "Right-click to manage");
                    icon.onClick(e -> {
                        if (e.isRightClick()) {
                            new ProfileManageGui(player, data, profile).open();
                            return;
                        }
                        data.setActiveProfileId(profile.getProfileId());
                        data.save(true);
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        new ClickerGui(player, data).open();
                    });
                    break;
                }
                case AVAILABLE: {
                    icon = GuiHelper.createIcon(Material.MINECART,
                            ChatColor.YELLOW + "" + ChatColor.BOLD + "Empty Slot",
                            "",
                            ChatColor.GRAY + "Click to create a new profile");
                    icon.onClick(e -> {
                        if (ProfileManager.getProfilesForOwner(data.getIdentifier()).size() >= data.getMaxProfiles()) {
                            player.sendMessage(ChatColor.RED + "You've reached your maximum number of profiles.");
                            return;
                        }
                        List<RealmProfile> current = ProfileManager.getProfilesForOwner(data.getIdentifier());
                        String name = "Realm " + (current.size() + 1);
                        RealmProfile newProfile = ProfileManager.createProfile(data.getIdentifier(), name);
                        LobbyClicker.getDatabase().putProfileThreaded(newProfile);
                        data.setActiveProfileId(newProfile.getProfileId());
                        data.save(true);
                        player.sendMessage(ChatColor.GREEN + "Created profile: " + ChatColor.WHITE + name);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        new ClickerGui(player, data).open();
                    });
                    break;
                }
                case LOCKED:
                default: {
                    icon = GuiHelper.createIcon(Material.TNT_MINECART,
                            ChatColor.RED + "" + ChatColor.BOLD + "Locked Slot",
                            "",
                            ChatColor.GRAY + "You need a higher permission",
                            ChatColor.GRAY + "to unlock more profile slots.");
                    break;
                }
            }
            addItem(slot, icon);
        });

        addPaginationArrows(slots, newPage -> new ProfileSelectorGui(player, data, newPage).open());
    }

    private enum SlotState { HAS_DATA, AVAILABLE, LOCKED }

    private static class SlotEntry {
        final RealmProfile profile;
        final SlotState state;
        SlotEntry(RealmProfile profile, SlotState state) {
            this.profile = profile;
            this.state = state;
        }
    }
}
