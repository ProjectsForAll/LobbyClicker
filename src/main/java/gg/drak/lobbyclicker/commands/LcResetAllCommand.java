package gg.drak.lobbyclicker.commands;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.ClickerGui;
import gg.drak.lobbyclicker.gui.LeaderboardGui;
import gg.drak.lobbyclicker.gui.UpgradeGui;
import gg.drak.lobbyclicker.realm.ProfileManager;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import gg.drak.lobbyclicker.utils.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class LcResetAllCommand implements CommandExecutor, TabCompleter {
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
            case "soft": handleReset(sender, false); break;
            case "hard": handleReset(sender, true); break;
            default: sendUsage(sender);
        }
        return true;
    }

    private void handleReset(CommandSender sender, boolean hard) {
        if (busy) {
            sender.sendMessage(ChatColor.RED + "A reset is already in progress. Please wait.");
            return;
        }
        busy = true;

        String mode = hard ? "HARD" : "SOFT";
        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[lcresetall] Starting " + mode + " reset of ALL players...");

        FoliaScheduler.runAsync(LobbyClicker.getInstance(), () -> {
            try {
                List<PlayerData> allPlayers = LobbyClicker.getDatabase().pullAllPlayersThreaded().join();
                sender.sendMessage(ChatColor.GRAY + "Found " + allPlayers.size() + " player(s) in the database.");

                int processed = 0;
                for (PlayerData player : allPlayers) {
                    try {
                        if (hard) {
                            processHardReset(player);
                        } else {
                            processSoftReset(player);
                        }
                        processed++;
                    } catch (Exception e) {
                        LobbyClicker.getInstance().logWarning("[lcresetall] Failed to reset player " + player.getIdentifier(), e);
                    }
                }

                final int finalProcessed = processed;
                // Broadcast to all other servers so they evict their stale caches too
                RedisSyncHandler.publishResetAll(mode);
                // Apply in-memory changes on main thread: reload reset profiles, close GUIs
                FoliaScheduler.runGlobal(LobbyClicker.getInstance(), () -> {
                    // Close every open plugin inventory so stale data isn't displayed
                    for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                        FoliaScheduler.runForEntity(p, LobbyClicker.getInstance(), p::closeInventory);
                    }
                    ClickerGui.getOpenGuis().clear();
                    UpgradeGui.getOpenGuis().clear();
                    LeaderboardGui.getOpenGuis().clear();

                    // For each online player, evict their stale profiles and reload from DB
                    for (PlayerData inMem : PlayerManager.getLoadedPlayers()) {
                        // Unload all currently cached profiles for this player
                        ProfileManager.unloadAllForOwner(inMem.getIdentifier());

                        // Pull the reset profile fresh from DB and register it in-memory
                        LobbyClicker.getDatabase().pullProfilesByOwnerThreaded(inMem.getIdentifier())
                                .thenAccept(freshProfiles -> {
                                    // Should be exactly one profile after reset
                                    if (!freshProfiles.isEmpty()) {
                                        RealmProfile fresh = freshProfiles.get(0);
                                        ProfileManager.loadProfile(fresh);
                                        inMem.setActiveProfileId(fresh.getProfileId());
                                    } else {
                                        inMem.setActiveProfileId(null);
                                    }
                                });
                    }

                    sender.sendMessage(ChatColor.GREEN + "[lcresetall] " + mode + " reset complete: " + finalProcessed + " player(s) processed.");
                    busy = false;
                });

            } catch (Exception e) {
                FoliaScheduler.runGlobal(LobbyClicker.getInstance(), () -> {
                    sender.sendMessage(ChatColor.RED + "[lcresetall] Reset failed: " + e.getMessage());
                    LobbyClicker.getInstance().logWarning("[lcresetall] Reset failed", e);
                    busy = false;
                });
            }
        });
    }

    // ---- HARD RESET ----
    // Wipes every profile to zero, keeps only one "Main" profile per player.

    private void processHardReset(PlayerData player) {
        List<RealmProfile> profiles = LobbyClicker.getDatabase().pullProfilesByOwnerThreaded(player.getIdentifier()).join();

        if (profiles.isEmpty()) {
            // No profiles — nothing to do
            return;
        }

        // Keep the first profile (active one preferred), delete the rest
        RealmProfile keep = pickPrimaryProfile(player, profiles);
        for (RealmProfile p : profiles) {
            if (!p.getProfileId().equals(keep.getProfileId())) {
                LobbyClicker.getDatabase().deleteProfileFromDb(p.getProfileId());
            }
        }

        // Hard reset: zero everything
        keep.reset();
        keep.setLifetimeCookiesEarned(BigDecimal.ZERO);
        keep.setGiftedCookies(BigDecimal.ZERO);
        keep.setGoldenCookiesCollected(0);
        keep.setTimesClicked(0);
        keep.setOwnerClicks(0);
        keep.setOtherClicks(0);
        keep.setProfileName("Main");

        LobbyClicker.getDatabase().putProfileSync(keep);

        // Point the player row at this profile
        player.setActiveProfileId(keep.getProfileId());
        LobbyClicker.getDatabase().putPlayer(player, false);
    }

    // ---- SOFT RESET ----
    // Aggregates all profiles into one, downscales cookies and aura, converts aura to cookies.

    private void processSoftReset(PlayerData player) {
        List<RealmProfile> profiles = LobbyClicker.getDatabase().pullProfilesByOwnerThreaded(player.getIdentifier()).join();

        if (profiles.isEmpty()) return;

        // Step 1: aggregate all profiles into one merged summary
        BigDecimal totalCookies = BigDecimal.ZERO;
        BigDecimal totalAura = BigDecimal.ZERO;
        long totalClicks = 0;
        long totalOwnerClicks = 0;
        long totalOtherClicks = 0;
        long totalGoldenCookies = 0;
        Set<gg.drak.lobbyclicker.upgrades.ClickerUpgrade> mergedPurchased = new java.util.LinkedHashSet<>();
        Set<gg.drak.lobbyclicker.achievements.Achievement> mergedQuests = new java.util.LinkedHashSet<>();

        for (RealmProfile p : profiles) {
            totalCookies = totalCookies.add(p.getCookies());
            totalAura = totalAura.max(p.getAura()); // take max aura across profiles
            totalClicks += p.getTimesClicked();
            totalOwnerClicks += p.getOwnerClicks();
            totalOtherClicks += p.getOtherClicks();
            totalGoldenCookies += p.getGoldenCookiesCollected();
            mergedPurchased.addAll(p.getPurchasedUpgrades());
            mergedQuests.addAll(p.getCompletedQuests());
        }

        // Step 2: aura → bonus cookies before zeroing aura
        // Conversion: aura * 1000, then apply the same soft-keep formula to the total
        BigDecimal auraCookieValue = totalAura.multiply(BigDecimal.valueOf(1000));
        BigDecimal combinedCookies = totalCookies.add(auraCookieValue);

        // Step 3: apply exponential downscale: kept = combined^0.4
        // This is the "more keeps more but still loses a lot" curve:
        //   10^6  (1M)   → keeps ~1000
        //   10^9  (1B)   → keeps ~16K
        //   10^12 (1T)   → keeps ~250K
        //   10^18        → keeps ~63M
        //   10^30        → keeps ~1B
        //   10^50        → keeps ~316B
        BigDecimal keptCookies = softScaleCookies(combinedCookies);

        // Step 4: pick the primary profile, delete the others
        RealmProfile keep = pickPrimaryProfile(player, profiles);
        for (RealmProfile p : profiles) {
            if (!p.getProfileId().equals(keep.getProfileId())) {
                LobbyClicker.getDatabase().deleteProfileFromDb(p.getProfileId());
            }
        }

        // Step 5: apply the soft-reset state
        keep.reset(); // zeros upgrades, prestige, aura, purchased upgrades

        keep.setCookies(keptCookies.setScale(0, RoundingMode.FLOOR));
        // Treat the kept cookies as "earned" for leaderboard/lifetime tracking
        keep.setTotalCookiesEarned(keptCookies.setScale(0, RoundingMode.FLOOR));
        keep.setLifetimeCookiesEarned(keptCookies.setScale(0, RoundingMode.FLOOR));
        keep.setGiftedCookies(BigDecimal.ZERO);

        // Keep click counts and quest progress — these are achievements, not economy
        keep.setTimesClicked(totalClicks);
        keep.setOwnerClicks(totalOwnerClicks);
        keep.setOtherClicks(totalOtherClicks);
        keep.setGoldenCookiesCollected(totalGoldenCookies);
        keep.setCompletedQuests(mergedQuests);
        // Purchased upgrades reset (they depend on upgrade counts which are reset)
        keep.setPurchasedUpgrades(new java.util.LinkedHashSet<>());
        keep.setAura(BigDecimal.ZERO);
        keep.setPrestigeLevel(0);
        keep.setProfileName("Main");

        LobbyClicker.getDatabase().putProfileSync(keep);

        player.setActiveProfileId(keep.getProfileId());
        LobbyClicker.getDatabase().putPlayer(player, false);
    }

    /**
     * Exponential downscale: kept = value^0.4
     * Uses natural log to compute this for arbitrarily large BigDecimal values.
     * Returns 0 if input is 0 or negative.
     */
    private BigDecimal softScaleCookies(BigDecimal value) {
        if (value.signum() <= 0) return BigDecimal.ZERO;

        // x^0.4 = e^(0.4 * ln(x))
        // For very large BigDecimals, convert to double-precision log via log10
        // log10(x) * ln(10) = ln(x), then * 0.4 = exponent
        // Result can be huge so we stay in double for the exponent, but reconstruct as BigDecimal

        // Use log10(value) to handle numbers far beyond double range
        double log10 = log10BigDecimal(value);
        double exponent = 0.4 * log10; // 0.4 * log10(x) = log10(x^0.4)

        // x^0.4 = 10^exponent
        double intPart = Math.floor(exponent);
        double fracPart = exponent - intPart;

        // 10^fracPart as double (in [1, 10))
        double mantissa = Math.pow(10.0, fracPart);

        // Result = mantissa * 10^intPart
        if (intPart > 300) {
            // Too large even for BigDecimal display — cap at a sane maximum
            intPart = 300;
        }

        BigDecimal result = BigDecimal.valueOf(mantissa)
                .multiply(BigDecimal.TEN.pow((int) intPart, MathContext.DECIMAL64));

        return result.setScale(0, RoundingMode.FLOOR);
    }

    /**
     * Compute log10 of a BigDecimal, handling values beyond double range.
     * For a number like 1.23e50, returns 50 + log10(1.23).
     */
    private double log10BigDecimal(BigDecimal value) {
        if (value.signum() <= 0) return Double.NEGATIVE_INFINITY;
        // Scale the value to get the base-10 exponent from the plain string
        String plain = value.toPlainString();
        // Find position of decimal point (or end of integer part)
        int dotIdx = plain.indexOf('.');
        int intDigits = dotIdx >= 0 ? dotIdx : plain.length();
        // Strip leading zeros and decimal to get mantissa digits
        String intPart = dotIdx >= 0 ? plain.substring(0, dotIdx) : plain;
        long digitCount = intPart.length();
        // Approximate: log10(value) ≈ (digitCount - 1) + log10(leading digits as double)
        // Grab up to 15 significant digits for the double conversion
        String sig = (plain.replace(".", "")).replaceAll("^0+", "");
        if (sig.isEmpty()) return Double.NEGATIVE_INFINITY;
        String truncated = sig.substring(0, Math.min(15, sig.length()));
        double mantissa;
        try {
            mantissa = Double.parseDouble(truncated) / Math.pow(10, truncated.length() - 1);
        } catch (NumberFormatException e) {
            mantissa = 1.0;
        }
        // log10(value) = (intDigits - 1) + log10(mantissa)
        return (digitCount - 1) + Math.log10(mantissa);
    }

    private RealmProfile pickPrimaryProfile(PlayerData player, List<RealmProfile> profiles) {
        // Prefer the active profile if it's in the list
        if (player.getActiveProfileId() != null) {
            for (RealmProfile p : profiles) {
                if (p.getProfileId().equals(player.getActiveProfileId())) return p;
            }
        }
        return profiles.get(0);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- LobbyClicker Reset All ---");
        sender.sendMessage(ChatColor.YELLOW + "/lcresetall soft" + ChatColor.GRAY + " - Aggregate all profiles per player, downscale cookies/aura, reset upgrades & prestige. Players keep a fraction.");
        sender.sendMessage(ChatColor.YELLOW + "/lcresetall hard" + ChatColor.GRAY + " - Wipe every player to zero. No mercy.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("soft", "hard").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
