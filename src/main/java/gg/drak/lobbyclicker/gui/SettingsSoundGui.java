package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.settings.SettingType;
import mc.obliviate.inventory.Gui;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class SettingsSoundGui extends Gui {
    private final PlayerData data;

    public SettingsSoundGui(Player player, PlayerData data) {
        super(player, "settings-sound", ChatColor.GREEN + "" + ChatColor.BOLD + "Sound Toggles", 4);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        SettingType[] soundSettings = {
                SettingType.SOUND_MASTER, SettingType.SOUND_CLICKER,
                SettingType.SOUND_MILESTONE_CURRENT, SettingType.SOUND_MILESTONE_TOTAL,
                SettingType.SOUND_MILESTONE_ENTROPY,
                SettingType.SOUND_BUY, SettingType.SOUND_FRIEND_REQUEST,
                SettingType.SOUND_FRIEND_JOIN, SettingType.SOUND_FRIEND_LEAVE,
                SettingType.SOUND_RANDO_JOIN, SettingType.SOUND_RANDO_LEAVE,
                SettingType.SOUND_FRIEND_CLICKER, SettingType.SOUND_RANDO_CLICKER,
        };

        int[] slots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

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
                new SettingsSoundGui(player, data).open();
            });
            addItem(slots[i], icon);
        }

        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new SettingsMainGui(player, data).open());
        addItem(31, back);
    }
}
