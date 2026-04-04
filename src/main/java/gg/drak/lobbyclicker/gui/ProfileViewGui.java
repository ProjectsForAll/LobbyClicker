package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.LeaderboardCache;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.SimpleGuiMonitor;
import gg.drak.lobbyclicker.realm.ProfileManager;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Detailed profile view page showing fine-grained stats about a player.
 * Can be opened from the leaderboard or from other contexts.
 */
public class ProfileViewGui extends SimpleGuiMonitor {
    private final PlayerData viewerData;
    private final String targetUuid;
    private final String targetName;
    private final Consumer<Player> backAction;

    // Stats can come from a loaded PlayerData or from a LeaderboardEntry
    private BigDecimal cookies = BigDecimal.ZERO;
    private BigDecimal totalEarned = BigDecimal.ZERO;
    private BigDecimal cps = BigDecimal.ZERO;
    private BigDecimal cpc = BigDecimal.ONE;
    private BigDecimal entropy = BigDecimal.ZERO;
    private BigDecimal aura = BigDecimal.ZERO;
    private int prestige = 0;
    private long realmClicks = 0;
    private long ownerClicks = 0;
    private long otherClicks = 0;
    private long globalClicks = 0;
    private String profileName = "Unknown";

    /**
     * View a loaded player's profile.
     */
    public ProfileViewGui(Player player, PlayerData viewerData, PlayerData targetData, Consumer<Player> backAction) {
        super(player, "profile-view", MonitorStyle.title("gold", targetData.getName() + "'s Profile"), MonitorStyle.ROWS_FULL);
        this.viewerData = viewerData;
        this.targetUuid = targetData.getIdentifier();
        this.targetName = targetData.getName();
        this.backAction = backAction;

        // Load from PlayerData
        this.cookies = targetData.getCookies();
        this.totalEarned = targetData.getTotalCookiesEarned();
        this.cps = targetData.getCps();
        this.cpc = targetData.getCpc();
        this.entropy = targetData.getClickerEntropy();
        this.aura = targetData.getAura();
        this.prestige = targetData.getPrestigeLevel();
        this.realmClicks = targetData.getTimesClicked();
        this.ownerClicks = targetData.getOwnerClicks();
        this.otherClicks = targetData.getOtherClicks();
        this.globalClicks = targetData.getGlobalClicks();
        RealmProfile profile = targetData.getActiveProfile();
        if (profile != null) this.profileName = profile.getProfileName();
    }

    /**
     * View from a leaderboard entry (limited data).
     */
    public ProfileViewGui(Player player, PlayerData viewerData, LeaderboardCache.LeaderboardEntry entry, Consumer<Player> backAction) {
        super(player, "profile-view", MonitorStyle.title("gold", entry.getPlayerName() + "'s Profile"), MonitorStyle.ROWS_FULL);
        this.viewerData = viewerData;
        this.targetUuid = entry.getPlayerUuid();
        this.targetName = entry.getPlayerName();
        this.backAction = backAction;

        this.cookies = entry.getCookies();
        this.totalEarned = entry.getTotalCookiesEarned();
        this.cps = entry.getCps();
        this.prestige = entry.getPrestigeLevel();
        this.profileName = entry.getProfileName();

        // Try to load more data if the player is loaded
        PlayerManager.getPlayer(targetUuid).ifPresent(data -> {
            this.cpc = data.getCpc();
            this.entropy = data.getClickerEntropy();
            this.aura = data.getAura();
            this.realmClicks = data.getTimesClicked();
            this.ownerClicks = data.getOwnerClicks();
            this.otherClicks = data.getOtherClicks();
            this.globalClicks = data.getGlobalClicks();
        });
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(viewerData, null);
        fillMonitorBorder();
        buildStandardActionBar(backAction);

        // Player head at top center
        addItem(4, GuiHelper.playerHead(targetUuid,
                ChatColor.GOLD + "" + ChatColor.BOLD + targetName,
                "", ChatColor.GRAY + "Profile: " + ChatColor.WHITE + profileName));

        // === Row 1: Economy stats ===
        setContent(0, GuiHelper.createIcon(Material.COOKIE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Cookies",
                "",
                ChatColor.GRAY + "Current: " + ChatColor.WHITE + FormatUtils.format(cookies),
                ChatColor.GRAY + "Total Earned: " + ChatColor.WHITE + FormatUtils.format(totalEarned)));

        setContent(1, GuiHelper.createIcon(Material.CLOCK,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Production",
                "",
                ChatColor.GRAY + "Per Second: " + ChatColor.WHITE + FormatUtils.format(cps),
                ChatColor.GRAY + "Per Click: " + ChatColor.WHITE + FormatUtils.format(cpc)));

        setContent(2, GuiHelper.createIcon(Material.BEACON,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Prestige",
                "",
                ChatColor.GRAY + "Level: " + ChatColor.WHITE + prestige,
                ChatColor.GRAY + "Aura: " + ChatColor.WHITE + FormatUtils.format(aura)));

        setContent(4, GuiHelper.createIcon(Material.ENDER_EYE,
                ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Entropy",
                "",
                ChatColor.GRAY + "Clicker Entropy: " + ChatColor.WHITE + FormatUtils.format(entropy)));

        // === Row 2: Click stats ===
        setContent(7, GuiHelper.createIcon(Material.STONE_BUTTON,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Click Stats",
                "",
                ChatColor.GRAY + "Realm Total: " + ChatColor.WHITE + FormatUtils.format(realmClicks),
                ChatColor.GRAY + "  Owner Clicks: " + ChatColor.WHITE + FormatUtils.format(ownerClicks),
                ChatColor.GRAY + "  Visitor Clicks: " + ChatColor.WHITE + FormatUtils.format(otherClicks),
                "",
                ChatColor.GRAY + "Global Clicks: " + ChatColor.WHITE + FormatUtils.format(globalClicks)));

        setContent(9, GuiHelper.createIcon(Material.NAME_TAG,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Profile Info",
                "",
                ChatColor.GRAY + "Name: " + ChatColor.WHITE + profileName,
                ChatColor.GRAY + "Owner: " + ChatColor.WHITE + targetName,
                ChatColor.GRAY + "UUID: " + ChatColor.DARK_GRAY + targetUuid));

        // Online status
        boolean isOnline = Bukkit.getPlayer(UUID.fromString(targetUuid)) != null;
        boolean isObo = !isOnline && gg.drak.lobbyclicker.LobbyClicker.getRedisManager() != null
                && gg.drak.lobbyclicker.LobbyClicker.getRedisManager().isPlayerOnlineAnywhere(targetUuid);
        String status;
        ChatColor statusColor;
        if (isOnline) { status = "Online (this server)"; statusColor = ChatColor.GREEN; }
        else if (isObo) { status = "Online (another server)"; statusColor = ChatColor.AQUA; }
        else { status = "Offline"; statusColor = ChatColor.GRAY; }

        setContent(11, GuiHelper.createIcon(isOnline ? Material.LIME_DYE : (isObo ? Material.LIGHT_BLUE_DYE : Material.GRAY_DYE),
                statusColor + "" + ChatColor.BOLD + "Status",
                "",
                statusColor + status));
    }
}
