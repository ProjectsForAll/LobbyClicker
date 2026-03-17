package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import gg.drak.lobbyclicker.settings.SettingType;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class SettingsSoundGui extends BaseGui {
    private final PlayerData data;

    public SettingsSoundGui(Player player, PlayerData data) {
        super(player, "settings-sound", ChatColor.GREEN + "" + ChatColor.BOLD + "Sound Toggles", 4);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        // Home button
        Icon home = GuiHelper.homeButton();
        home.onClick(e -> new ClickerGui(player, data).open());
        addItem(0, home);

        SettingType[] soundSettings = {
                SettingType.SOUND_MASTER, SettingType.SOUND_CLICKER,
                SettingType.SOUND_MILESTONE_CURRENT, SettingType.SOUND_MILESTONE_TOTAL,
                SettingType.SOUND_MILESTONE_ENTROPY,
                SettingType.SOUND_BUY, SettingType.SOUND_FRIEND_REQUEST,
                SettingType.SOUND_FRIEND_JOIN, SettingType.SOUND_FRIEND_LEAVE,
                SettingType.SOUND_RANDO_JOIN, SettingType.SOUND_RANDO_LEAVE,
                SettingType.SOUND_FRIEND_CLICKER, SettingType.SOUND_RANDO_CLICKER,
        };

        int[] slots = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};

        for (int i = 0; i < soundSettings.length && i < slots.length; i++) {
            SettingType type = soundSettings[i];
            boolean on = data.getSettings().getBool(type);
            Material mat = on ? Material.LIME_DYE : Material.GRAY_DYE;
            String status = on ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF";
            String masterNote = type == SettingType.SOUND_MASTER ? ChatColor.DARK_GRAY + "(Master Switch)" : "";

            Icon icon = GuiHelper.createIcon(mat,
                    ChatColor.YELLOW + type.displayName() + " " + status,
                    masterNote, "", ChatColor.GRAY + "Click to toggle");
            icon.onClick(e -> {
                data.getSettings().toggle(type);
                data.save(true);
                RedisSyncHandler.publishSettingsSync(data);
                new SettingsSoundGui(player, data).open();
            });
            addItem(slots[i], icon);
        }

        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new PlayerSettingsGui(player, data).open());
        addItem(31, back);
    }
}
