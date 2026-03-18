package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.social.RealmManager;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.*;

public class RealmViewersGui extends PaginationMonitor {
    private final PlayerData data;

    public RealmViewersGui(Player player, PlayerData data) {
        this(player, data, 0);
    }

    public RealmViewersGui(Player player, PlayerData data, int page) {
        super(player, "realm-viewers", MonitorStyle.title(ChatColor.AQUA, "Realm Viewers"), page);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        Player player = (Player) event.getPlayer();

        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new SocialMainGui(p, data).open());

        List<String> viewers = new ArrayList<>(RealmManager.getViewers(data.getIdentifier()));

        if (viewers.isEmpty()) {
            addItem(22, GuiHelper.createIcon(Material.PAPER, ChatColor.GRAY + "No viewers",
                    "", ChatColor.GRAY + "Nobody is viewing your realm right now."));
            return;
        }

        populatePagedContent(viewers, (viewerUuid, slot) -> {
            String name = viewerUuid.substring(0, 8);
            PlayerData vd = PlayerManager.getPlayer(viewerUuid).orElse(null);
            if (vd != null) name = vd.getName();

            boolean isFriend = data.getFriends().contains(viewerUuid);

            Icon icon = playerHeadIcon(viewerUuid,
                    (isFriend ? ChatColor.GREEN : ChatColor.WHITE) + name,
                    p -> new PlayerActionGui(p, data, viewerUuid, "viewers").open(),
                    isFriend ? ChatColor.GREEN + "Friend" : ChatColor.GRAY + "Visitor",
                    "", ChatColor.YELLOW + "Click for actions");
            addItem(slot, icon);
        });

        addPaginationArrows(viewers, newPage -> new RealmViewersGui(player, data, newPage).open());
    }
}
