package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.SimpleGuiMonitor;
import gg.drak.lobbyclicker.prestige.PrestigeManager;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;

public class PrestigeGui extends SimpleGuiMonitor {
    private final PlayerData data;
    private final PlayerData realmOwner;

    public PrestigeGui(Player player, PlayerData data) {
        this(player, data, null);
    }

    public PrestigeGui(Player player, PlayerData data, PlayerData realmOwner) {
        super(player, "prestige", ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Prestige", 3);
        this.data = data;
        this.realmOwner = realmOwner;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        Player player = (Player) event.getPlayer();

        setPlayerContext(data, realmOwner);
        fillMonitorBorder();
        buildStandardActionBar(p -> new ClickerGui(p, data).open());

        BigDecimal cost = PrestigeManager.getPrestigeCost(data.getPrestigeLevel());
        boolean canPrestige = PrestigeManager.canPrestige(data);
        BigDecimal auraGain = canPrestige ? PrestigeManager.calculateAuraGain(data) : BigDecimal.ZERO;

        // Info sign
        setContent(1, GuiHelper.createIcon(Material.OAK_SIGN,
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

        // Info display
        setContent(3, GuiHelper.createIcon(Material.NETHER_STAR,
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

        // Prestige button — opens confirmation screen
        if (canPrestige) {
            Icon prestige = GuiHelper.createIcon(Material.BEACON,
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Prestige",
                    "", ChatColor.YELLOW + "Click to prestige!",
                    "", ChatColor.GREEN + "Aura gained: " + ChatColor.GOLD + FormatUtils.format(auraGain));
            prestige.onClick(e -> new PrestigeConfirmGui(player, data).open());
            setContent(5, prestige);
        }
    }
}
