package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.redis.RedisManager;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class FriendsListGui extends BaseGui {
    private final PlayerData data;
    private final int page;
    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public FriendsListGui(Player player, PlayerData data, int page) {
        super(player, "friends-list", ChatColor.GREEN + "" + ChatColor.BOLD + "Friends (Page " + (page + 1) + ")", 6);
        this.data = data;
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

        List<String> friendsList = new ArrayList<>(data.getFriends());
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, friendsList.size());

        RedisManager redis = LobbyClicker.getRedisManager();

        for (int i = start; i < end; i++) {
            String friendUuid = friendsList.get(i);
            int slotIndex = i - start;
            if (slotIndex >= ITEM_SLOTS.length) break;

            String friendName = friendUuid;
            PlayerData friendData = PlayerManager.getPlayer(friendUuid).orElse(null);
            if (friendData != null) friendName = friendData.getName();
            else {
                try { friendName = Bukkit.getOfflinePlayer(UUID.fromString(friendUuid)).getName(); } catch (Exception ignored) {}
            }
            if (friendName == null) friendName = friendUuid.substring(0, 8);

            boolean localOnline = Bukkit.getPlayer(UUID.fromString(friendUuid)) != null;
            boolean crossServerOnline = false;
            String crossServerName = null;

            if (!localOnline && redis != null) {
                RedisManager.CrossServerPlayer csp = redis.getCrossServerPlayer(friendUuid);
                if (csp != null) {
                    crossServerOnline = true;
                    crossServerName = csp.getPrettyName();
                    if (friendName.equals(friendUuid) || friendName.equals(friendUuid.substring(0, 8))) {
                        friendName = csp.getName();
                    }
                }
            }

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(friendUuid))); } catch (Exception ignored) {}

                ChatColor nameColor;
                List<String> lore = new ArrayList<>();
                lore.add("");

                if (localOnline) {
                    nameColor = ChatColor.GREEN;
                    lore.add(ChatColor.GREEN + "Online");
                } else if (crossServerOnline) {
                    nameColor = ChatColor.AQUA;
                    lore.add(ChatColor.AQUA + "Online" + ChatColor.GRAY + " (" + crossServerName + ")");
                } else {
                    nameColor = ChatColor.GRAY;
                    lore.add(ChatColor.GRAY + "Offline");
                }

                lore.add("");
                lore.add(ChatColor.YELLOW + "Click for actions");

                meta.setDisplayName(nameColor + friendName);
                meta.setLore(lore);
                head.setItemMeta(meta);
            }

            Icon icon = new Icon(head);
            String finalFriendUuid = friendUuid;
            icon.onClick(e -> new PlayerActionGui(player, data, finalFriendUuid, "friends").open());
            addItem(ITEM_SLOTS[slotIndex], icon);
        }

        if (page > 0) {
            Icon prev = GuiHelper.createIcon(Material.ARROW, ChatColor.YELLOW + "Previous Page");
            prev.onClick(e -> new FriendsListGui(player, data, page - 1).open());
            addItem(45, prev);
        }
        if (end < friendsList.size()) {
            Icon next = GuiHelper.createIcon(Material.ARROW, ChatColor.YELLOW + "Next Page");
            next.onClick(e -> new FriendsListGui(player, data, page + 1).open());
            addItem(53, next);
        }

        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new SocialMainGui(player, data).open());
        addItem(49, back);
    }
}
