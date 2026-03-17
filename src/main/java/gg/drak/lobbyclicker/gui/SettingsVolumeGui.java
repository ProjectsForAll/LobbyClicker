package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.settings.SettingType;
import mc.obliviate.inventory.Gui;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class SettingsVolumeGui extends Gui {
    private final PlayerData data;

    public SettingsVolumeGui(Player player, PlayerData data) {
        super(player, "settings-volume", ChatColor.AQUA + "" + ChatColor.BOLD + "Sound Volumes", 4);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        SettingType[] volumeSettings = {
                SettingType.VOLUME_CLICKER, SettingType.VOLUME_MILESTONE_CURRENT,
                SettingType.VOLUME_MILESTONE_TOTAL, SettingType.VOLUME_MILESTONE_ENTROPY,
                SettingType.VOLUME_BUY,
                SettingType.VOLUME_FRIEND_REQUEST, SettingType.VOLUME_FRIEND_JOIN,
                SettingType.VOLUME_FRIEND_LEAVE, SettingType.VOLUME_RANDO_JOIN,
                SettingType.VOLUME_RANDO_LEAVE, SettingType.VOLUME_FRIEND_CLICKER,
                SettingType.VOLUME_RANDO_CLICKER,
        };

        int[] slots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

        for (int i = 0; i < volumeSettings.length && i < slots.length; i++) {
            SettingType type = volumeSettings[i];
            int vol = data.getSettings().get(type);
            Material mat;
            switch (vol) {
                case 0: mat = Material.RED_STAINED_GLASS_PANE; break;
                case 2: mat = Material.LIME_STAINED_GLASS_PANE; break;
                default: mat = Material.YELLOW_STAINED_GLASS_PANE; break;
            }

            ItemStack item = new ItemStack(mat, Math.max(1, vol));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + type.displayName() + ChatColor.WHITE + " Volume: " + vol);
                meta.setLore(Arrays.asList("", ChatColor.GRAY + "Click to cycle (0 -> 1 -> 2 -> 0)"));
                item.setItemMeta(meta);
            }
            Icon icon = new Icon(item);
            icon.onClick(e -> {
                int next = (data.getSettings().get(type) + 1) % 3;
                data.getSettings().set(type, next);
                data.save(true);
                new SettingsVolumeGui(player, data).open();
            });
            addItem(slots[i], icon);
        }

        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new SettingsMainGui(player, data).open());
        addItem(31, back);
    }
}
