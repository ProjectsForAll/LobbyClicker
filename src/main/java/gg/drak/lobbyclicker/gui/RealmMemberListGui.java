package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.realm.RealmRole;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.*;

public class RealmMemberListGui extends BaseGui {
    private final PlayerData data;
    private final boolean friendsOnly;
    private final int page;

    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public RealmMemberListGui(Player player, PlayerData data, boolean friendsOnly, int page) {
        super(player, "realm-member-list",
                (friendsOnly ? ChatColor.GREEN + "" + ChatColor.BOLD + "Friends" : ChatColor.AQUA + "" + ChatColor.BOLD + "All Members")
                        + (page > 0 ? " (Page " + (page + 1) + ")" : ""),
                6);
        this.data = data;
        this.friendsOnly = friendsOnly;
        this.page = page;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        // Home button
        Icon home = GuiHelper.homeButton();
        home.onClick(e -> new ClickerGui(player, data).open());
        addItem(0, home);

        // Build member list
        Set<String> members = new LinkedHashSet<>();
        RealmProfile profile = data.getActiveProfile();

        if (friendsOnly) {
            members.addAll(data.getFriends());
        } else {
            // All contributors (anyone with a role) + all friends
            if (profile != null) {
                members.addAll(profile.getRoles().keySet());
            }
            members.addAll(data.getFriends());
        }

        List<String> memberList = new ArrayList<>(members);
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, memberList.size());

        for (int i = start; i < end; i++) {
            String uuid = memberList.get(i);
            int slotIndex = i - start;
            if (slotIndex >= ITEM_SLOTS.length) break;

            String name = uuid.substring(0, 8);
            try {
                String n = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
                if (n != null) name = n;
            } catch (Exception ignored) {}

            boolean isFriend = data.getFriends().contains(uuid);
            RealmRole role = profile != null ? profile.getRole(uuid) : RealmRole.VISITOR;

            String roleStr = ChatColor.GRAY + "Role: " + ChatColor.WHITE + role.getDisplayName();
            String friendStr = isFriend ? ChatColor.GREEN + "Friend" : ChatColor.GRAY + "Not a friend";

            String finalName = name;
            Icon head = GuiHelper.playerHead(uuid,
                    ChatColor.WHITE + name,
                    "", friendStr, roleStr, "", ChatColor.YELLOW + "Click to manage");
            head.onClick(e -> new RealmPlayerManageGui(player, data, uuid, friendsOnly ? "friends" : "all").open());
            addItem(ITEM_SLOTS[slotIndex], head);
        }

        // Pagination
        if (page > 0) {
            Icon prev = GuiHelper.createIcon(Material.ARROW, ChatColor.YELLOW + "Previous Page");
            prev.onClick(e -> new RealmMemberListGui(player, data, friendsOnly, page - 1).open());
            addItem(45, prev);
        }
        if (end < memberList.size()) {
            Icon next = GuiHelper.createIcon(Material.ARROW, ChatColor.YELLOW + "Next Page");
            next.onClick(e -> new RealmMemberListGui(player, data, friendsOnly, page + 1).open());
            addItem(53, next);
        }

        // Back
        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new RealmMembersGui(player, data).open());
        addItem(49, back);
    }
}
