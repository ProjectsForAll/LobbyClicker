package gg.drak.lobbyclicker.tasks;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.math.CookieMath;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import gg.drak.lobbyclicker.settings.SettingType;
import gg.drak.lobbyclicker.social.PendingTransaction;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;

public class CookieTask extends BukkitRunnable {
    private int tickCounter = 0;
    private static final int AUTO_SAVE_INTERVAL = 300; // 5 minutes in seconds

    @Override
    public void run() {
        tickCounter++;

        for (PlayerData data : PlayerManager.getLoadedPlayers()) {
            if (!data.isOnline()) continue;
            if (!data.isFullyLoaded()) continue;

            BigDecimal cps = data.getCps();
            if (cps.signum() > 0) {
                BigDecimal boosterMult = gg.drak.lobbyclicker.boosters.BoosterManager.getMultiplier(
                        data.getIdentifier(), gg.drak.lobbyclicker.boosters.BoosterEffect.CPS_MULTIPLIER);
                data.addCookies(cps.multiply(boosterMult));
            }

            // Milestone checks run every tick (every 1 second) for instant detection
            checkMilestones(data);

            // Push data snapshot to Redis for cross-server sync
            if (LobbyClicker.getRedisManager() != null) {
                RedisSyncHandler.publishDataSync(data);
            }
        }

        if (tickCounter >= AUTO_SAVE_INTERVAL) {
            tickCounter = 0;
            for (PlayerData data : PlayerManager.getLoadedPlayers()) {
                if (data.isFullyLoaded()) {
                    data.save(true);
                }
            }
        }

        // Clean expired pending transactions every 30 seconds
        if (tickCounter % 30 == 0) {
            PendingTransaction.cleanExpired();
        }
    }

    private void checkMilestones(PlayerData data) {
        Player player = data.asPlayer().orElse(null);
        if (player == null) return;

        // Current cookies milestone (new digit = sound)
        int currentDigits = CookieMath.digitCount(data.getCookies());
        int prevCurrentDigits = data.getLastCurrentDigitCount();
        if (currentDigits > prevCurrentDigits && prevCurrentDigits > 0
                && data.getSettings().isSoundEnabled(SettingType.SOUND_MILESTONE_CURRENT)) {
            float vol = data.getSettings().getVolume(SettingType.VOLUME_MILESTONE_CURRENT);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, vol, 1.5f);
        }
        data.setLastCurrentDigitCount(currentDigits);

        // Total cookies milestone (new digit = sound)
        int totalDigits = CookieMath.digitCount(data.getTotalCookiesEarned());
        int prevTotalDigits = data.getLastTotalDigitCount();
        if (totalDigits > prevTotalDigits && prevTotalDigits > 0
                && data.getSettings().isSoundEnabled(SettingType.SOUND_MILESTONE_TOTAL)) {
            float vol = data.getSettings().getVolume(SettingType.VOLUME_MILESTONE_TOTAL);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, vol, 1.2f);
        }
        data.setLastTotalDigitCount(totalDigits);

        // Clicker Entropy milestones
        BigDecimal entropy = data.getClickerEntropy();
        int entropyDigits = CookieMath.digitCount(entropy);
        int prevEntropyDigits = data.getLastEntropyDigitCount();
        int entropyLead = CookieMath.leadDigit(entropy);
        int prevEntropyLead = data.getLastEntropyLeadDigit();

        if (data.getSettings().isSoundEnabled(SettingType.SOUND_MILESTONE_ENTROPY)) {
            float vol = data.getSettings().getVolume(SettingType.VOLUME_MILESTONE_ENTROPY);

            if (entropyDigits > prevEntropyDigits && prevEntropyDigits > 0) {
                // New digit = legendary sound (best sound)
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, vol, 1.0f);
                player.sendMessage("\u00a76\u00a7l\u2B50 Entropy Milestone! \u00a7eNew digit reached!");
            } else if (entropy.compareTo(BigDecimal.valueOf(100)) >= 0 && entropyLead > prevEntropyLead && prevEntropyLead > 0
                    && entropyDigits == prevEntropyDigits) {
                // Leftmost digit increased (same digit count, above 100) = epic sound
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, vol, 0.8f);
                player.sendMessage("\u00a7d\u00a7l\u2728 Entropy Rising! \u00a7eClicker Entropy climbed to \u00a7d" + gg.drak.lobbyclicker.utils.FormatUtils.format(entropy) + "\u00a7e!");
            }
        }
        data.setLastEntropyDigitCount(entropyDigits);
        data.setLastEntropyLeadDigit(entropyLead);
    }
}
