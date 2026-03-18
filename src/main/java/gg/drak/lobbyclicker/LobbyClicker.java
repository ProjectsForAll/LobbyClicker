package gg.drak.lobbyclicker;

import gg.drak.lobbyclicker.gui.BaseGui;
import gg.drak.lobbyclicker.gui.ClickerGui;
import host.plas.bou.BetterPlugin;
import gg.drak.lobbyclicker.commands.ClickerAdminCommand;
import gg.drak.lobbyclicker.commands.ClickerCommand;
import gg.drak.lobbyclicker.commands.ClickerDataCommand;
import gg.drak.lobbyclicker.commands.ClickerTransferCommand;
import gg.drak.lobbyclicker.commands.LeaderboardCommand;
import gg.drak.lobbyclicker.config.DatabaseConfig;
import gg.drak.lobbyclicker.config.MainConfig;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.database.ClickerOperator;
import gg.drak.lobbyclicker.events.MainListener;
import gg.drak.lobbyclicker.placeholders.ClickerPlaceholders;
import gg.drak.lobbyclicker.redis.RedisManager;
import gg.drak.lobbyclicker.tasks.CookieTask;
import gg.drak.lobbyclicker.tasks.GuiRefreshTask;
import host.plas.bou.gui.ScreenManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

@Getter @Setter
public final class LobbyClicker extends BetterPlugin {
    @Getter @Setter
    private static LobbyClicker instance;
    @Getter @Setter
    private static MainConfig mainConfig;
    @Getter @Setter
    private static DatabaseConfig databaseConfig;
    @Getter @Setter
    private static ClickerOperator database;
    @Getter @Setter
    private static MainListener mainListener;
    @Getter @Setter
    private static CookieTask cookieTask;
    @Getter @Setter
    private static GuiRefreshTask guiRefreshTask;
    @Getter @Setter
    private static RedisManager redisManager;

    public LobbyClicker() {
        super();
    }

    @Override
    public void onBaseEnabled() {
        setInstance(this);

        setMainConfig(new MainConfig());
        setDatabaseConfig(new DatabaseConfig());

        setDatabase(new ClickerOperator());

        setMainListener(new MainListener());

        // Register commands
        registerCommand("clicker", new ClickerCommand());
        registerCommand("leaderboard", new LeaderboardCommand());

        ClickerAdminCommand adminCommand = new ClickerAdminCommand();
        PluginCommand adminCmd = getCommand("clickeradmin");
        if (adminCmd != null) {
            adminCmd.setExecutor(adminCommand);
            adminCmd.setTabCompleter(adminCommand);
        }

        ClickerTransferCommand transferCommand = new ClickerTransferCommand();
        PluginCommand transferCmd = getCommand("clickertransfer");
        if (transferCmd != null) {
            transferCmd.setExecutor(transferCommand);
            transferCmd.setTabCompleter(transferCommand);
        }

        ClickerDataCommand dataCommand = new ClickerDataCommand();
        PluginCommand dataCmd = getCommand("clickerdata");
        if (dataCmd != null) {
            dataCmd.setExecutor(dataCommand);
            dataCmd.setTabCompleter(dataCommand);
        }

        // Start CPS task - runs every second (20 ticks)
        setCookieTask(new CookieTask());
        getCookieTask().runTaskTimer(this, 20L, 20L);

        // Start GUI refresh task - runs every second
        setGuiRefreshTask(new GuiRefreshTask());
        getGuiRefreshTask().runTaskTimer(this, 20L, 20L);

        // Initialize Redis if enabled
        if (getMainConfig().isRedisEnabled()) {
            setRedisManager(new RedisManager());
            getRedisManager().connect();
        }

        // Register PlaceholderAPI expansion if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClickerPlaceholders().register();
            logInfo("PlaceholderAPI expansion registered.");
        }

        // Load data for any players already online (handles plugin reload)
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerManager.getOrCreatePlayer(player);
        }
    }

    @Override
    public void onBaseDisable() {
        if (getCookieTask() != null) getCookieTask().cancel();
        if (getGuiRefreshTask() != null) getGuiRefreshTask().cancel();

        // Close all open GUIs
        for (Player player : Bukkit.getOnlinePlayers()) {
//            ScreenManager.getScreen(player).ifPresent(screen -> {
//                if (screen.getType() instanceof BaseGui.SimpleGuiType) {
//                    screen.close();
//                }
//            });
            player.closeInventory();
        }
        ClickerGui.getOpenGuis().clear();

        if (getRedisManager() != null) getRedisManager().shutdown();

        // Save all loaded profiles
        for (gg.drak.lobbyclicker.realm.RealmProfile profile : gg.drak.lobbyclicker.realm.ProfileManager.getAllLoadedProfiles()) {
            getDatabase().putProfileSync(profile);
        }

        PlayerManager.getLoadedPlayers().forEach(playerData -> {
            playerData.saveAndUnload(false);
        });
    }

    private void registerCommand(String name, Object executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor((org.bukkit.command.CommandExecutor) executor);
        }
    }
}
