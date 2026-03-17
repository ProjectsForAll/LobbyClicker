package gg.drak.lobbyclicker.events;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.ClickerGui;
import gg.drak.lobbyclicker.social.PendingTransaction;
import gg.drak.lobbyclicker.social.RealmManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MainListener extends AbstractConglomerate {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerManager.getOrCreatePlayer(player);

        // Notify Redis
        if (LobbyClicker.getRedisManager() != null) {
            LobbyClicker.getRedisManager().publishJoin(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        // Notify Redis
        if (LobbyClicker.getRedisManager() != null) {
            LobbyClicker.getRedisManager().publishQuit(player);
        }

        PlayerData data = PlayerManager.getOrCreatePlayer(player);
        data.saveAndUnload();

        // Clean up realm viewers
        RealmManager.removeViewerFromAll(uuid);
        RealmManager.removeAllViewersOf(uuid);

        // Clean up pending transactions
        PendingTransaction.removeAllFor(uuid);

        // Unregister GUI
        ClickerGui.unregisterGui(player.getUniqueId());
    }
}
