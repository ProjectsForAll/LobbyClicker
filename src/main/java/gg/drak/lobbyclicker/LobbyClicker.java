package gg.drak.lobbyclicker;

import host.plas.bou.BetterPlugin;
import gg.drak.lobbyclicker.commands.ClickerAdminCommand;
import gg.drak.lobbyclicker.commands.ClickerCommand;
import gg.drak.lobbyclicker.commands.ClickerDataCommand;
import gg.drak.lobbyclicker.commands.LeaderboardCommand;
import gg.drak.lobbyclicker.config.DatabaseConfig;
import gg.drak.lobbyclicker.config.MainConfig;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.database.ClickerOperator;
import gg.drak.lobbyclicker.events.MainListener;
import gg.drak.lobbyclicker.placeholders.ClickerPlaceholders;
import gg.drak.lobbyclicker.tasks.CookieTask;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;

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

        ClickerDataCommand dataCommand = new ClickerDataCommand();
        PluginCommand dataCmd = getCommand("clickerdata");
        if (dataCmd != null) {
            dataCmd.setExecutor(dataCommand);
            dataCmd.setTabCompleter(dataCommand);
        }

        // Start CPS task - runs every second (20 ticks)
        setCookieTask(new CookieTask());
        getCookieTask().runTaskTimer(this, 20L, 20L);

        // Register PlaceholderAPI expansion if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClickerPlaceholders().register();
            logInfo("PlaceholderAPI expansion registered.");
        }
    }

    @Override
    public void onBaseDisable() {
        if (getCookieTask() != null) {
            getCookieTask().cancel();
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
