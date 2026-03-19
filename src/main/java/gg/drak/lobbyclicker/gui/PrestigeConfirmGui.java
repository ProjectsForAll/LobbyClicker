package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.ConfirmationMonitor;
import gg.drak.lobbyclicker.prestige.PrestigeManager;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;

public class PrestigeConfirmGui extends ConfirmationMonitor {
    private final PlayerData data;

    public PrestigeConfirmGui(Player player, PlayerData data) {
        super(player, "prestige-confirm", ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Confirm Prestige");
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);

        BigDecimal cost = PrestigeManager.getPrestigeCost(data.getPrestigeLevel());
        BigDecimal auraGain = PrestigeManager.canPrestige(data) ? PrestigeManager.calculateAuraGain(data) : BigDecimal.ZERO;

        Icon info = GuiHelper.createIcon(Material.BEACON,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Prestige",
                "",
                ChatColor.GRAY + "Level: " + ChatColor.WHITE + data.getPrestigeLevel() + " → " + (data.getPrestigeLevel() + 1),
                ChatColor.GRAY + "Cost: " + ChatColor.WHITE + FormatUtils.format(cost),
                ChatColor.GREEN + "Aura gained: " + ChatColor.GOLD + FormatUtils.format(auraGain),
                "",
                ChatColor.RED + "Resets: cookies, upgrades, clicks");

        buildConfirmation(info, "Prestige", "This cannot be undone!",
                p -> {
                    if (!PrestigeManager.canPrestige(data)) {
                        p.sendMessage(ChatColor.RED + "You can no longer afford to prestige.");
                        new ClickerGui(p, data).open();
                        return;
                    }
                    BigDecimal gained = PrestigeManager.calculateAuraGain(data);
                    PrestigeManager.performPrestige(data);
                    data.save(true);
                    p.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Prestige! " +
                            ChatColor.YELLOW + "You are now prestige " + ChatColor.WHITE + data.getPrestigeLevel() +
                            ChatColor.YELLOW + ". Gained " + ChatColor.GOLD + FormatUtils.format(gained) + ChatColor.YELLOW + " aura.");
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    new ClickerGui(p, data).open();
                },
                p -> new PrestigeGui(p, data).open());
    }
}
