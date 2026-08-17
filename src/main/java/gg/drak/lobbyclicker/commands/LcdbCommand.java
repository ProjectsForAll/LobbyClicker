package gg.drak.lobbyclicker.commands;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.database.ClickerOperator;
import gg.drak.lobbyclicker.realm.ProfileManager;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.upgrades.UpgradeType;
import host.plas.bou.sql.ConnectorSet;
import host.plas.bou.sql.DatabaseType;
import gg.drak.lobbyclicker.utils.FoliaScheduler;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LcdbCommand implements CommandExecutor, TabCompleter {
    private volatile boolean busy = false;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lobbyclicker.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "migrate":
                handleMigrate(sender, args);
                break;
            case "import":
                handleImport(sender);
                break;
            default:
                sendUsage(sender);
        }
        return true;
    }

    /**
     * /lcdb migrate <MYSQL|SQLITE>
     * Saves all in-memory data to the current DB, writes the new type to database-config.yml,
     * then swaps the live connection to the new database type.
     */
    private void handleMigrate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /lcdb migrate <MYSQL|SQLITE>");
            return;
        }

        DatabaseType targetType;
        try {
            targetType = DatabaseType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Unknown database type: " + args[1] + ". Use MYSQL or SQLITE.");
            return;
        }

        DatabaseType currentType = LobbyClicker.getDatabaseConfig().getDatabaseType();
        if (currentType == targetType) {
            sender.sendMessage(ChatColor.YELLOW + "Already using " + targetType.name() + ". Nothing to do.");
            return;
        }

        if (busy) {
            sender.sendMessage(ChatColor.RED + "A database operation is already in progress. Please wait.");
            return;
        }
        busy = true;

        sender.sendMessage(ChatColor.YELLOW + "Saving all in-memory data to current database...");

        // Save all loaded profiles and player data synchronously
        for (RealmProfile profile : ProfileManager.getAllLoadedProfiles()) {
            LobbyClicker.getDatabase().putProfileSync(profile);
        }
        PlayerManager.getLoadedPlayers().forEach(pd -> LobbyClicker.getDatabase().putPlayer(pd, false));

        sender.sendMessage(ChatColor.YELLOW + "Switching database type to " + targetType.name() + "...");

        // Write new type to config file
        try {
            java.io.File configFile = new java.io.File(LobbyClicker.getInstance().getDataFolder(), "database-config.yml");
            org.bukkit.configuration.file.YamlConfiguration yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
            yaml.set("database.type", targetType.name());
            yaml.save(configFile);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Warning: could not update database-config.yml: " + e.getMessage());
        }

        // Build new operator and swap
        ConnectorSet newSet = LobbyClicker.getDatabaseConfig().getConnectorSet();
        // The config now has the new type; rebuild ConnectorSet with updated type
        ConnectorSet rebuilt = new ConnectorSet(
                targetType,
                newSet.getHost(),
                newSet.getPort(),
                newSet.getDatabase(),
                newSet.getUsername(),
                newSet.getPassword(),
                newSet.getTablePrefix(),
                newSet.getSqliteFileName()
        );

        ClickerOperator newOp = new ClickerOperator(rebuilt);
        LobbyClicker.setDatabase(newOp);

        sender.sendMessage(ChatColor.GREEN + "Database migrated to " + targetType.name() + " successfully.");
        sender.sendMessage(ChatColor.GRAY + "Note: Player data was not copied — use /lcdb import to merge data from the old DB.");
        busy = false;
    }

    /**
     * /lcdb import
     * Reads all players and profiles from the OTHER database type (the one not currently active),
     * then aggregates them into the current database. Stats are summed, prestige is max, sets are union.
     */
    private void handleImport(CommandSender sender) {
        if (busy) {
            sender.sendMessage(ChatColor.RED + "A database operation is already in progress. Please wait.");
            return;
        }
        busy = true;

        DatabaseType currentType = LobbyClicker.getDatabaseConfig().getDatabaseType();
        DatabaseType sourceType = currentType == DatabaseType.MYSQL ? DatabaseType.SQLITE : DatabaseType.MYSQL;

        ConnectorSet cfg = LobbyClicker.getDatabaseConfig().getConnectorSet();
        ConnectorSet sourceSet = new ConnectorSet(
                sourceType,
                cfg.getHost(),
                cfg.getPort(),
                cfg.getDatabase(),
                cfg.getUsername(),
                cfg.getPassword(),
                cfg.getTablePrefix(),
                cfg.getSqliteFileName()
        );

        sender.sendMessage(ChatColor.YELLOW + "Importing from " + sourceType.name() + " into " + currentType.name() + "...");

        ClickerOperator sourceOp;
        try {
            sourceOp = new ClickerOperator(sourceSet);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Failed to connect to source database: " + e.getMessage());
            busy = false;
            return;
        }

        final ClickerOperator srcOpFinal = sourceOp;

        FoliaScheduler.runAsync(LobbyClicker.getInstance(), () -> {
            try {
                List<gg.drak.lobbyclicker.data.PlayerData> sourcePlayers = srcOpFinal.pullAllPlayersThreaded().join();
                sender.sendMessage(ChatColor.GRAY + "Found " + sourcePlayers.size() + " player(s) in source DB.");

                int profilesImported = 0;
                int playersImported = 0;

                for (gg.drak.lobbyclicker.data.PlayerData srcPlayer : sourcePlayers) {
                    List<RealmProfile> srcProfiles = srcOpFinal.pullProfilesByOwnerThreaded(srcPlayer.getIdentifier()).join();

                    for (RealmProfile srcProfile : srcProfiles) {
                        // Try to find matching profile in current DB by ProfileId
                        java.util.Optional<RealmProfile> existingOpt = LobbyClicker.getDatabase()
                                .pullProfileThreaded(srcProfile.getProfileId()).join();

                        if (existingOpt.isPresent()) {
                            // Aggregate: merge src into existing
                            RealmProfile merged = mergeProfiles(existingOpt.get(), srcProfile);
                            LobbyClicker.getDatabase().putProfileSync(merged);
                            // Also update in-memory if loaded
                            RealmProfile inMem = ProfileManager.getProfile(merged.getProfileId()).orElse(null);
                            if (inMem != null) {
                                applyMergedProfile(inMem, merged);
                            }
                        } else {
                            // Profile doesn't exist in current DB — insert as-is
                            LobbyClicker.getDatabase().putProfileSync(srcProfile);
                        }
                        profilesImported++;
                    }

                    // Also push the player row (upsert — preserves activeProfileId from current DB if present)
                    gg.drak.lobbyclicker.data.PlayerData existingPlayer = LobbyClicker.getDatabase()
                            .pullPlayerThreaded(srcPlayer.getIdentifier()).join().orElse(null);
                    if (existingPlayer == null) {
                        LobbyClicker.getDatabase().putPlayer(srcPlayer, false);
                    }
                    playersImported++;
                }

                final int fp = profilesImported;
                final int pp = playersImported;
                FoliaScheduler.runForSender(sender, LobbyClicker.getInstance(), () -> {
                    sender.sendMessage(ChatColor.GREEN + "Import complete: " + pp + " player(s), " + fp + " profile(s) processed.");
                    busy = false;
                });
            } catch (Exception e) {
                FoliaScheduler.runForSender(sender, LobbyClicker.getInstance(), () -> {
                    sender.sendMessage(ChatColor.RED + "Import failed: " + e.getMessage());
                    LobbyClicker.getInstance().logWarning("lcdb import failed", e);
                    busy = false;
                });
            }
        });
    }

    /**
     * Merge srcProfile into baseProfile (aggregate, not overwrite).
     * Returns a new merged profile with baseProfile's ID/owner/name.
     */
    private RealmProfile mergeProfiles(RealmProfile base, RealmProfile src) {
        RealmProfile merged = new RealmProfile(base.getProfileId(), base.getOwnerUuid(), base.getProfileName());

        // Sum numeric stats
        merged.setCookies(base.getCookies().add(src.getCookies()));
        merged.setTotalCookiesEarned(base.getTotalCookiesEarned().add(src.getTotalCookiesEarned()));
        merged.setLifetimeCookiesEarned(base.getLifetimeCookiesEarned().add(src.getLifetimeCookiesEarned()));
        merged.setGiftedCookies(base.getGiftedCookies().add(src.getGiftedCookies()));
        merged.setTimesClicked(base.getTimesClicked() + src.getTimesClicked());
        merged.setOwnerClicks(base.getOwnerClicks() + src.getOwnerClicks());
        merged.setOtherClicks(base.getOtherClicks() + src.getOtherClicks());
        merged.setGoldenCookiesCollected(base.getGoldenCookiesCollected() + src.getGoldenCookiesCollected());

        // Upgrades: max per building
        for (UpgradeType type : UpgradeType.values()) {
            merged.setUpgradeCount(type, Math.max(base.getUpgradeCount(type), src.getUpgradeCount(type)));
        }

        // Purchased upgrades: union
        merged.getPurchasedUpgrades().addAll(base.getPurchasedUpgrades());
        merged.getPurchasedUpgrades().addAll(src.getPurchasedUpgrades());

        // Completed quests: union
        merged.getCompletedQuests().addAll(base.getCompletedQuests());
        merged.getCompletedQuests().addAll(src.getCompletedQuests());

        // Prestige: max
        merged.setPrestigeLevel(Math.max(base.getPrestigeLevel(), src.getPrestigeLevel()));

        // Aura: max
        merged.setAura(base.getAura().max(src.getAura()));

        // Realm visibility: OR (public if either was public)
        merged.setRealmPublic(base.isRealmPublic() || src.isRealmPublic());

        // Roles and bans from base only (don't merge other server's role assignments)
        merged.getRoles().putAll(base.getRoles());
        merged.getBans().addAll(base.getBans());

        return merged;
    }

    /**
     * Apply the merged profile values into an already in-memory profile (for loaded players).
     */
    private void applyMergedProfile(RealmProfile target, RealmProfile merged) {
        target.setCookies(merged.getCookies());
        target.setTotalCookiesEarned(merged.getTotalCookiesEarned());
        target.setLifetimeCookiesEarned(merged.getLifetimeCookiesEarned());
        target.setGiftedCookies(merged.getGiftedCookies());
        target.setTimesClicked(merged.getTimesClicked());
        target.setOwnerClicks(merged.getOwnerClicks());
        target.setOtherClicks(merged.getOtherClicks());
        target.setGoldenCookiesCollected(merged.getGoldenCookiesCollected());
        for (UpgradeType type : UpgradeType.values()) {
            target.setUpgradeCount(type, merged.getUpgradeCount(type));
        }
        target.getPurchasedUpgrades().clear();
        target.getPurchasedUpgrades().addAll(merged.getPurchasedUpgrades());
        target.getCompletedQuests().clear();
        target.getCompletedQuests().addAll(merged.getCompletedQuests());
        target.setPrestigeLevel(merged.getPrestigeLevel());
        target.setAura(merged.getAura());
        target.setGoldenCookiesCollected(merged.getGoldenCookiesCollected());
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- LobbyClicker DB ---");
        sender.sendMessage(ChatColor.YELLOW + "/lcdb migrate <MYSQL|SQLITE>" + ChatColor.GRAY + " - Switch database type (saves first, does NOT copy data)");
        sender.sendMessage(ChatColor.YELLOW + "/lcdb import" + ChatColor.GRAY + " - Import & aggregate data from the other DB into the current one");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("migrate", "import").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            return List.of("MYSQL", "SQLITE").stream()
                    .filter(s -> s.startsWith(args[1].toUpperCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
