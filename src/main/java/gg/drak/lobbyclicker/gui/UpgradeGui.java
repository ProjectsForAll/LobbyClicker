package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.upgrades.UpgradeType;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Gui;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UpgradeGui extends Gui {
    private final PlayerData data;

    private static final int[] UPGRADE_SLOTS = {19, 20, 21, 22, 28, 29, 30, 31};

    public UpgradeGui(Player player, PlayerData data) {
        super(player, "clicker-upgrades", ChatColor.GREEN + "" + ChatColor.BOLD + "Upgrades", 6);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();

        // Fill background
        fillGui(createFiller());

        // Cookie count display
        addItem(4, createCookieDisplay());

        // Upgrade items
        UpgradeType[] types = UpgradeType.values();
        for (int i = 0; i < types.length && i < UPGRADE_SLOTS.length; i++) {
            UpgradeType type = types[i];
            int slot = UPGRADE_SLOTS[i];
            addItem(slot, createUpgradeIcon(player, type));
        }

        // Back button
        Icon back = createIcon(Material.ARROW, ChatColor.RED + "Back", new String[]{
                "",
                ChatColor.GRAY + "Return to Cookie Clicker"
        });
        back.onClick(e -> new ClickerGui(player, data).open());
        addItem(49, back);
    }

    private Icon createUpgradeIcon(Player player, UpgradeType type) {
        int owned = data.getUpgradeCount(type);
        double cost = type.getCost(owned);
        boolean canAfford = data.canAfford(cost);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + type.getDescription());
        lore.add("");
        lore.add(ChatColor.GRAY + "Owned: " + ChatColor.WHITE + owned);

        if (type.getCpsPerLevel() > 0) {
            lore.add(ChatColor.GRAY + "CPS each: " + ChatColor.WHITE + "+" + FormatUtils.format(type.getCpsPerLevel()));
            lore.add(ChatColor.GRAY + "Total CPS: " + ChatColor.WHITE + "+" + FormatUtils.format(type.getCpsPerLevel() * owned));
        }
        if (type.getCpcPerLevel() > 0) {
            lore.add(ChatColor.GRAY + "CPC each: " + ChatColor.WHITE + "+" + FormatUtils.format(type.getCpcPerLevel()));
            lore.add(ChatColor.GRAY + "Total CPC: " + ChatColor.WHITE + "+" + FormatUtils.format(type.getCpcPerLevel() * owned));
        }

        lore.add("");
        lore.add(ChatColor.GRAY + "Cost: " + (canAfford ? ChatColor.GREEN : ChatColor.RED) + FormatUtils.format(cost) + " cookies");
        lore.add("");
        lore.add(canAfford ? ChatColor.YELLOW + "Click to buy!" : ChatColor.RED + "Not enough cookies!");

        String color = canAfford ? ChatColor.GREEN.toString() : ChatColor.RED.toString();
        Icon icon = createIcon(type.getMaterial(), color + ChatColor.BOLD + type.getDisplayName(), lore.toArray(new String[0]));

        icon.onClick(e -> {
            if (data.buyUpgrade(type)) {
                if (data.getSettings().isSoundEnabled(gg.drak.lobbyclicker.settings.SettingType.SOUND_BUY)) {
                    float vol = data.getSettings().getVolume(gg.drak.lobbyclicker.settings.SettingType.VOLUME_BUY);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, vol, 1.5f);
                }
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            }
            new UpgradeGui(player, data).open();
        });

        return icon;
    }

    private Icon createCookieDisplay() {
        return createIcon(Material.COOKIE, ChatColor.GOLD + "" + ChatColor.BOLD + "Your Cookies",
                new String[]{
                        "",
                        ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(data.getCookies()),
                        ChatColor.GRAY + "CPS: " + ChatColor.WHITE + FormatUtils.format(data.getCps()),
                        ChatColor.GRAY + "CPC: " + ChatColor.WHITE + FormatUtils.format(data.getCpc()),
                });
    }

    private static Icon createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return new Icon(item);
    }

    private static Icon createIcon(Material material, String name, String[] lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return new Icon(item);
    }
}
