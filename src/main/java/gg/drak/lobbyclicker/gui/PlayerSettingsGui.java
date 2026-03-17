package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class PlayerSettingsGui extends BaseGui {
    private final PlayerData data;

    public PlayerSettingsGui(Player player, PlayerData data) {
        super(player, "player-settings", ChatColor.YELLOW + "" + ChatColor.BOLD + "Player Settings", 3);
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

        Icon sounds = GuiHelper.createIcon(Material.NOTE_BLOCK,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Sound Toggles",
                "", ChatColor.GRAY + "Turn sounds on/off");
        sounds.onClick(e -> new SettingsSoundGui(player, data).open());
        addItem(11, sounds);

        Icon volumes = GuiHelper.createIcon(Material.JUKEBOX,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Sound Volumes",
                "", ChatColor.GRAY + "Adjust volume levels (0-2)");
        volumes.onClick(e -> new SettingsVolumeGui(player, data).open());
        addItem(13, volumes);

        Icon other = GuiHelper.createIcon(Material.REDSTONE,
                ChatColor.RED + "" + ChatColor.BOLD + "Other Settings",
                "", ChatColor.GRAY + "Friend requests, public farm, etc.");
        other.onClick(e -> new SettingsOtherGui(player, data).open());
        addItem(15, other);

        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new SettingsMainGui(player, data).open());
        addItem(22, back);
    }
}
