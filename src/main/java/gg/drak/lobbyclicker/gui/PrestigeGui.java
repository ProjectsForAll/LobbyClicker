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

        boolean canPrestige = PrestigeManager.canPrestige(data);
        BigDecimal auraGain = PrestigeManager.calculateAuraGain(data);

        setContent(1, ClickerGuiHelper.createIcon(Material.OAK_SIGN,
                ChatColor.GOLD + "" + ChatColor.BOLD + "How Prestige Works",
                "",
                ChatColor.WHITE + "What is Prestige?",
                ChatColor.GRAY + "  Ascend to convert baked cookies into Aura.",
                "",
                ChatColor.GREEN + "Benefits:",
                ChatColor.GRAY + "  +1% CPS per prestige level",
                ChatColor.GRAY + "  +1% CPS per Aura point",
                "",
                ChatColor.YELLOW + "Aura:",
                ChatColor.GRAY + "  floor(cbrt(all-time cookies / 1T)) - aura held",
                ChatColor.GRAY + "  Each point costs more than the last.",
                "",
                ChatColor.RED + "What gets reset:",
                ChatColor.GRAY + "  Cookies, upgrades, clicks (this run)",
                "",
                ChatColor.GREEN + "What is kept:",
                ChatColor.GRAY + "  Settings, friends, prestige, aura, achievements"));

        setContent(3, ClickerGuiHelper.createIcon(Material.NETHER_STAR,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Prestige Info",
                "",
                ChatColor.GRAY + "Current Prestige: " + ChatColor.WHITE + data.getPrestigeLevel(),
                ChatColor.GRAY + "Current Aura: " + ChatColor.WHITE + FormatUtils.format(data.getAura()),
                ChatColor.GRAY + "Baked this run: " + ChatColor.WHITE + FormatUtils.format(data.getTotalCookiesEarned()),
                ChatColor.GRAY + "Baked all-time: " + ChatColor.WHITE + FormatUtils.format(data.getLifetimeCookiesEarned()),
                "",
                canPrestige
                        ? ChatColor.GREEN + "Aura gained: " + ChatColor.GOLD + FormatUtils.format(auraGain)
                        : ChatColor.RED + "Need " + FormatUtils.format(PrestigeManager.cookiesUntilNextAura(data)) +
                          " more all-time cookies.",
                "",
                ChatColor.YELLOW + "Resets: " + ChatColor.GRAY + "Cookies, upgrades, clicks",
                ChatColor.GREEN + "Keeps: " + ChatColor.GRAY + "Settings, friends, prestige, aura"));

        if (canPrestige) {
            Icon prestige = ClickerGuiHelper.createIcon(Material.BEACON,
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Prestige",
                    "", ChatColor.YELLOW + "Click to prestige!",
                    "", ChatColor.GREEN + "Aura gained: " + ChatColor.GOLD + FormatUtils.format(auraGain));
            prestige.onClick(e -> new PrestigeConfirmGui(player, data).open());
            setContent(5, prestige);
        } else {
            setContent(5, ClickerGuiHelper.createIcon(Material.BEACON,
                    ChatColor.GRAY + "" + ChatColor.BOLD + "Prestige",
                    "", ChatColor.RED + "Not enough all-time cookies for more Aura",
                    "", ChatColor.GRAY + "Need " + ChatColor.WHITE +
                        FormatUtils.format(PrestigeManager.cookiesUntilNextAura(data)) + ChatColor.GRAY + " more",
                    ChatColor.GRAY + "All-time: " + ChatColor.WHITE + FormatUtils.format(data.getLifetimeCookiesEarned())));
        }
    }
}
