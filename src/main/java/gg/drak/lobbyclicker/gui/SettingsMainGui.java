package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class SettingsMainGui extends BaseGui {
    private final PlayerData data;

    public SettingsMainGui(Player player, PlayerData data) {
        super(player, "settings-main", ChatColor.YELLOW + "" + ChatColor.BOLD + "Settings", 3);
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

        // Player Settings
        Icon playerSettings = GuiHelper.createIcon(Material.COMPARATOR,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Player Settings",
                "", ChatColor.GRAY + "Sounds, volumes, preferences");
        playerSettings.onClick(e -> new PlayerSettingsGui(player, data).open());
        addItem(11, playerSettings);

        // Realm Settings
        Icon realmSettings = GuiHelper.createIcon(Material.BEACON,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Realm Settings",
                "", ChatColor.GRAY + "Manage members, realm options");
        realmSettings.onClick(e -> new RealmSettingsGui(player, data).open());
        addItem(15, realmSettings);

        // Back
        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new ClickerGui(player, data).open());
        addItem(22, back);

        // Close
        Icon close = GuiHelper.createIcon(Material.BARRIER, ChatColor.RED + "Close");
        close.onClick(e -> player.closeInventory());
        addItem(26, close);
    }
}
