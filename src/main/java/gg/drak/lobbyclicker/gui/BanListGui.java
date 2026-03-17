package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class BanListGui extends BaseGui {
    private final PlayerData data;
    private final int page;
    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43
    };

    public BanListGui(Player player, PlayerData data, int page) {
        super(player, "ban-list", ChatColor.RED + "" + ChatColor.BOLD + "Banned Players", 6);
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

        List<String> banList = new ArrayList<>(data.getBans());
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, banList.size());

        for (int i = start; i < end; i++) {
            String uuid = banList.get(i);
            int slotIndex = i - start;
            if (slotIndex >= ITEM_SLOTS.length) break;

            String name = uuid.substring(0, 8);
            try { String n = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName(); if (n != null) name = n; } catch (Exception ignored) {}

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid))); } catch (Exception ignored) {}
                meta.setDisplayName(ChatColor.RED + name);
                meta.setLore(Arrays.asList("", ChatColor.YELLOW + "Click to unban"));
                head.setItemMeta(meta);
            }

            Icon icon = new Icon(head);
            String finalUuid = uuid;
            icon.onClick(e -> {
                data.getBans().remove(finalUuid);
                LobbyClicker.getDatabase().deleteBanThreaded(data.getIdentifier(), finalUuid);
                RedisSyncHandler.publishBanRemove(data.getIdentifier(), finalUuid);
                player.sendMessage(ChatColor.YELLOW + "Unbanned player.");
                new BanListGui(player, data, page).open();
            });
            addItem(ITEM_SLOTS[slotIndex], icon);
        }

        if (page > 0) {
            Icon prev = GuiHelper.createIcon(Material.ARROW, ChatColor.YELLOW + "Previous Page");
            prev.onClick(e -> new BanListGui(player, data, page - 1).open());
            addItem(45, prev);
        }
        if (end < banList.size()) {
            Icon next = GuiHelper.createIcon(Material.ARROW, ChatColor.YELLOW + "Next Page");
            next.onClick(e -> new BanListGui(player, data, page + 1).open());
            addItem(53, next);
        }

        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new SocialMainGui(player, data).open());
        addItem(49, back);
    }
}
