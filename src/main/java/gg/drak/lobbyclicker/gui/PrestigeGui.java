package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.SimpleGuiMonitor;
import gg.drak.lobbyclicker.prestige.PrestigeManager;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;

public class PrestigeGui extends SimpleGuiMonitor {
    private final PlayerData data;
    private boolean confirmed = false;

    public PrestigeGui(Player player, PlayerData data) {
        super(player, "prestige", ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Prestige", 6);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        Player player = (Player) event.getPlayer();

        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new ClickerGui(p, data).open());

        // Info sign at index 7 (slot 8)
        addItem(7, GuiHelper.createIcon(Material.OAK_SIGN,
                ChatColor.GOLD + "" + ChatColor.BOLD + "How Prestige Works",
                "",
                ChatColor.WHITE + "What is Prestige?",
                ChatColor.GRAY + "  Prestige resets your cookies and upgrades",
                ChatColor.GRAY + "  in exchange for permanent bonuses.",
                "",
                ChatColor.GREEN + "Benefits:",
                ChatColor.GRAY + "  +5% CPS per prestige level",
                ChatColor.GRAY + "  +10% CPC per prestige level",
                ChatColor.GRAY + "  +1 cookie/click per prestige level",
                ChatColor.GRAY + "  +1% CPC per aura point",
                "",
                ChatColor.YELLOW + "Aura:",
                ChatColor.GRAY + "  Excess cookies above the prestige cost",
                ChatColor.GRAY + "  are converted into Clicker Aura.",
                ChatColor.GRAY + "  Formula: (cookies - cost) / 1,000,000",
                "",
                ChatColor.RED + "What gets reset:",
                ChatColor.GRAY + "  Cookies, Total Earned, Upgrades, Clicks",
                "",
                ChatColor.GREEN + "What is kept:",
                ChatColor.GRAY + "  Settings, Friends, Prestige Level, Aura"));

        BigDecimal cost = PrestigeManager.getPrestigeCost(data.getPrestigeLevel());
        boolean canPrestige = PrestigeManager.canPrestige(data);
        BigDecimal auraGain = canPrestige ? PrestigeManager.calculateAuraGain(data) : BigDecimal.ZERO;

        // Info display - content slot 0 (center-left of content row)
        setContent(0, GuiHelper.createIcon(Material.NETHER_STAR,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Prestige Info",
                "",
                ChatColor.GRAY + "Current Prestige: " + ChatColor.WHITE + data.getPrestigeLevel(),
                ChatColor.GRAY + "Current Aura: " + ChatColor.WHITE + FormatUtils.format(data.getAura()),
                "",
                ChatColor.GRAY + "Cost: " + (canPrestige ? ChatColor.GREEN : ChatColor.RED) + FormatUtils.format(cost) + " cookies",
                ChatColor.GRAY + "Your cookies: " + ChatColor.WHITE + FormatUtils.format(data.getCookies()),
                "",
                canPrestige ? ChatColor.GREEN + "Aura gained: " + ChatColor.GOLD + FormatUtils.format(auraGain) : ChatColor.RED + "Not enough cookies!",
                "",
                ChatColor.YELLOW + "Resets: " + ChatColor.GRAY + "Cookies, upgrades, clicks",
                ChatColor.GREEN + "Keeps: " + ChatColor.GRAY + "Settings, friends, prestige, aura"));

        // Prestige button - content slot 3 (center of content row)
        if (canPrestige) {
            Icon prestige;
            if (!confirmed) {
                prestige = GuiHelper.createIcon(Material.BEACON,
                        ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Prestige",
                        "", ChatColor.YELLOW + "Click to confirm prestige",
                        ChatColor.RED + "This will reset your cookies and upgrades!");
                prestige.onClick(e -> {
                    confirmed = true;
                    new PrestigeGui(player, data).open();
                });
            } else {
                prestige = GuiHelper.createIcon(Material.BEACON,
                        ChatColor.GREEN + "" + ChatColor.BOLD + "CONFIRM PRESTIGE",
                        "", ChatColor.RED + "" + ChatColor.BOLD + "Are you sure?",
                        "", ChatColor.YELLOW + "Click again to prestige!");
                prestige.onClick(e -> {
                    if (!PrestigeManager.canPrestige(data)) {
                        player.sendMessage(ChatColor.RED + "You can no longer afford to prestige.");
                        new ClickerGui(player, data).open();
                        return;
                    }
                    BigDecimal gained = PrestigeManager.calculateAuraGain(data);
                    PrestigeManager.performPrestige(data);
                    data.save(true);
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Prestige! " +
                            ChatColor.YELLOW + "You are now prestige " + ChatColor.WHITE + data.getPrestigeLevel() +
                            ChatColor.YELLOW + ". Gained " + ChatColor.GOLD + FormatUtils.format(gained) + ChatColor.YELLOW + " aura.");
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    new ClickerGui(player, data).open();
                });
            }
            setContent(3, prestige);
        }
    }
}
