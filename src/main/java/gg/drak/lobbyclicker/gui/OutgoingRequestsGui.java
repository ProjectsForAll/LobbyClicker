package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class OutgoingRequestsGui extends PaginationMonitor {
    private final PlayerData data;
    private final Consumer<Player> backAction;

    public OutgoingRequestsGui(Player player, PlayerData data, Consumer<Player> backAction) {
        this(player, data, 0, backAction);
    }

    public OutgoingRequestsGui(Player player, PlayerData data, int page) {
        this(player, data, page, p -> new FriendsMenuGui(p, data).open());
    }

    public OutgoingRequestsGui(Player player, PlayerData data, int page, Consumer<Player> backAction) {
        super(player, "outgoing-requests", MonitorStyle.title("yellow", "Outgoing Requests"), page);
        this.data = data;
        this.backAction = backAction;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(backAction);

        List<String> outgoing = new ArrayList<>(data.getOutgoingFriendRequests());

        if (outgoing.isEmpty()) {
            addItem(22, GuiHelper.createIcon(Material.PAPER,
                    ChatColor.GRAY + "No outgoing requests",
                    "", ChatColor.GRAY + "You haven't sent any friend requests."));
        }

        populatePagedContent(outgoing, (receiverUuid, slot) -> {
            if (receiverUuid == null) return;
            String name = receiverUuid.substring(0, 8);
            try { String n = Bukkit.getOfflinePlayer(UUID.fromString(receiverUuid)).getName(); if (n != null) name = n; } catch (Exception ignored) {}

            String finalName = name;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(receiverUuid))); } catch (Exception ignored) {}
                meta.setDisplayName(MenuText.itemLine(ChatColor.YELLOW + name));
                meta.setLore(Arrays.asList(
                        MenuText.itemLine(""),
                        MenuText.itemLine(ChatColor.GRAY + "Request sent"),
                        MenuText.itemLine(""),
                        MenuText.itemLine(ChatColor.RED + "Click to cancel request")));
                head.setItemMeta(meta);
            }

            Icon icon = new Icon(head);
            icon.onClick(e -> {
                data.getOutgoingFriendRequests().remove(receiverUuid);
                LobbyClicker.getDatabase().deleteFriendRequestThreaded(data.getIdentifier(), receiverUuid);
                RedisSyncHandler.publishFriendRequestDelete(data.getIdentifier(), receiverUuid);
                PlayerManager.getPlayer(receiverUuid).ifPresent(rd ->
                        rd.getIncomingFriendRequests().remove(data.getIdentifier()));
                player.sendMessage(ChatColor.RED + "Cancelled friend request to " + ChatColor.WHITE + finalName);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
                new OutgoingRequestsGui(player, data, page, backAction).open();
            });
            addItem(slot, icon);
        });

        addPaginationArrows(outgoing, newPage -> new OutgoingRequestsGui(player, data, newPage, backAction).open());
    }
}
