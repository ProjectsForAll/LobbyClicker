package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.ConfirmationMonitor;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.realm.RealmProfile;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class RealmResetConfirmGui extends ConfirmationMonitor {
    private final PlayerData data;

    public RealmResetConfirmGui(Player player, PlayerData data) {
        super(player, "realm-reset", MonitorStyle.title(ChatColor.RED, "Reset Realm?"));
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);

        Icon info = MonitorStyle.infoItem(Material.FLINT_AND_STEEL,
                ChatColor.RED + "" + ChatColor.BOLD + "Realm Reset",
                "",
                ChatColor.RED + "This will reset:",
                ChatColor.GRAY + "  - Cookies, Upgrades, Prestige",
                ChatColor.GRAY + "  - Aura, Clicks",
                "",
                ChatColor.GREEN + "Keeps: Settings, Friends, Blocks");

        buildConfirmation(info, "Reset Realm", "This cannot be undone!", p -> {
            RealmProfile profile = data.getActiveProfile();
            if (profile != null) {
                profile.reset();
                data.save(true);
                p.sendMessage(ChatColor.RED + "Your realm has been reset.");
                p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            }
            new ClickerGui(p, data).open();
        }, p -> new RealmSettingsGui(p, data).open());
    }
}
