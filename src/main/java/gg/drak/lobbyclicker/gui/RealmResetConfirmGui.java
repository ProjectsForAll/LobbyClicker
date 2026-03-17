package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.realm.RealmProfile;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class RealmResetConfirmGui extends BaseGui {
    private final PlayerData data;
    private final boolean confirmed;

    public RealmResetConfirmGui(Player player, PlayerData data) {
        this(player, data, false);
    }

    private RealmResetConfirmGui(Player player, PlayerData data, boolean confirmed) {
        super(player, "realm-reset", ChatColor.RED + "" + ChatColor.BOLD + "Reset Realm?", 3);
        this.data = data;
        this.confirmed = confirmed;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        // Home button
        Icon home = GuiHelper.homeButton();
        home.onClick(e -> new ClickerGui(player, data).open());
        addItem(0, home);

        // Info
        addItem(4, GuiHelper.createIcon(Material.TNT,
                ChatColor.RED + "" + ChatColor.BOLD + "Realm Reset",
                "",
                ChatColor.RED + "This will reset:",
                ChatColor.GRAY + "  - Cookies",
                ChatColor.GRAY + "  - Total Cookies Earned",
                ChatColor.GRAY + "  - All Upgrades",
                ChatColor.GRAY + "  - Prestige Level",
                ChatColor.GRAY + "  - Clicker Aura",
                ChatColor.GRAY + "  - Times Clicked",
                "",
                ChatColor.GREEN + "This will be kept:",
                ChatColor.GRAY + "  - Player Settings",
                ChatColor.GRAY + "  - Friends List",
                ChatColor.GRAY + "  - Blocks List"));

        if (!confirmed) {
            Icon confirm = GuiHelper.createIcon(Material.ORANGE_WOOL,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "Click to Confirm",
                    "", ChatColor.YELLOW + "Click once more to reset");
            confirm.onClick(e -> new RealmResetConfirmGui(player, data, true).open());
            addItem(13, confirm);
        } else {
            Icon finalConfirm = GuiHelper.createIcon(Material.RED_WOOL,
                    ChatColor.RED + "" + ChatColor.BOLD + "CONFIRM RESET",
                    "", ChatColor.RED + "" + ChatColor.BOLD + "This cannot be undone!",
                    "", ChatColor.YELLOW + "Click to reset your realm");
            finalConfirm.onClick(e -> {
                RealmProfile profile = data.getActiveProfile();
                if (profile != null) {
                    profile.reset();
                    data.save(true);
                    player.sendMessage(ChatColor.RED + "Your realm has been reset.");
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                }
                new ClickerGui(player, data).open();
            });
            addItem(13, finalConfirm);
        }

        // Cancel
        Icon cancel = GuiHelper.createIcon(Material.GREEN_WOOL, ChatColor.GREEN + "" + ChatColor.BOLD + "Cancel",
                "", ChatColor.GRAY + "Go back without resetting");
        cancel.onClick(e -> new RealmSettingsGui(player, data).open());
        addItem(22, cancel);
    }
}
