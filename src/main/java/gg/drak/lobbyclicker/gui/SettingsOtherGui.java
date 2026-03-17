package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.settings.SettingType;
import mc.obliviate.inventory.Gui;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class SettingsOtherGui extends Gui {
    private final PlayerData data;

    public SettingsOtherGui(Player player, PlayerData data) {
        super(player, "settings-other", ChatColor.RED + "" + ChatColor.BOLD + "Other Settings", 3);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        SettingType[] settings = {
                SettingType.ALLOW_FRIEND_REQUESTS,
                SettingType.AUTO_ACCEPT_FRIENDS,
                SettingType.PUBLIC_FARM,
                SettingType.ALLOW_FRIEND_JOINS,
                SettingType.ALLOW_OFFLINE_REALM,
        };

        String[] descriptions = {
                "Allow others to send you friend requests",
                "Automatically accept incoming friend requests",
                "Let anyone visit your clicker realm",
                "Let friends visit your clicker realm",
                "Let friends visit your realm while you're offline",
        };

        int[] slots = {10, 11, 12, 14, 15};

        for (int i = 0; i < settings.length; i++) {
            SettingType type = settings[i];
            boolean on = data.getSettings().getBool(type);
            Material mat = on ? Material.LIME_DYE : Material.GRAY_DYE;
            String status = on ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF";

            Icon icon = GuiHelper.createIcon(mat,
                    ChatColor.YELLOW + type.displayName() + " " + status,
                    "", ChatColor.GRAY + descriptions[i], "", ChatColor.GRAY + "Click to toggle");
            icon.onClick(e -> {
                data.getSettings().toggle(type);
                if (type == SettingType.PUBLIC_FARM) {
                    data.setRealmPublic(data.getSettings().getBool(SettingType.PUBLIC_FARM));
                }
                data.save(true);
                new SettingsOtherGui(player, data).open();
            });
            addItem(slots[i], icon);
        }

        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new SettingsMainGui(player, data).open());
        addItem(22, back);
    }
}
