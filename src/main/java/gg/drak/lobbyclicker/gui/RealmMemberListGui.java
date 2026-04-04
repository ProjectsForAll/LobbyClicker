package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.MenuText;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.realm.RealmRole;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.*;

public class RealmMemberListGui extends PaginationMonitor {
    private final PlayerData data;
    private final boolean friendsOnly;

    public RealmMemberListGui(Player player, PlayerData data, boolean friendsOnly, int page) {
        super(player, "realm-member-list",
                friendsOnly ? MonitorStyle.title("green", "Friends") : MonitorStyle.title("aqua", "All Members"),
                page);
        this.data = data;
        this.friendsOnly = friendsOnly;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        Player player = (Player) event.getPlayer();

        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new RealmMembersGui(p, data).open());

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

        populatePagedContent(memberList, (uuid, slot) -> {
            String name = uuid.substring(0, 8);
            try {
                String n = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
                if (n != null) name = n;
            } catch (Exception ignored) {}

            boolean isFriend = data.getFriends().contains(uuid);
            RealmRole role = profile != null ? profile.getRole(uuid) : RealmRole.VISITOR;

            String roleStr = MenuText.grayWhite("Role: ", role.getDisplayName());
            String friendStr = isFriend
                    ? "<green>" + MenuText.esc("Friend") + "</green>"
                    : "<gray>" + MenuText.esc("Not a friend") + "</gray>";

            Icon head = playerHeadIcon(uuid,
                    "<white>" + MenuText.esc(name) + "</white>",
                    p -> new RealmPlayerManageGui(p, data, uuid, friendsOnly ? "friends" : "all").open(),
                    "", friendStr, roleStr, "", "<yellow>" + MenuText.esc("Click to manage") + "</yellow>");
            addItem(slot, head);
        });

        addPaginationArrows(memberList, newPage -> new RealmMemberListGui(player, data, friendsOnly, newPage).open());
    }
}
