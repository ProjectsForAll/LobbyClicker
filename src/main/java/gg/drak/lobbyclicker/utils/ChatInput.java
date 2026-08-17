package gg.drak.lobbyclicker.utils;

import gg.drak.lobbyclicker.LobbyClicker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.function.Consumer;

/**
 * Prompts a player to type something in chat and returns the result.
 * Auto-cancels after 30 seconds.
 */
public class ChatInput implements Listener {
    private final Player player;
    private final Consumer<String> callback;
    private final FoliaScheduler.PluginTask timeoutTask;

    private ChatInput(Player player, Consumer<String> callback) {
        this.player = player;
        this.callback = callback;
        this.timeoutTask = FoliaScheduler.runForEntityLater(player, LobbyClicker.getInstance(), () -> {
            HandlerList.unregisterAll(this);
            if (player.isOnline()) {
                FoliaScheduler.runForEntity(player, LobbyClicker.getInstance(), () -> callback.accept(null));
            }
        }, 600L); // 30 seconds
    }

    public static void request(Player player, Consumer<String> callback) {
        ChatInput listener = new ChatInput(player, callback);
        org.bukkit.Bukkit.getPluginManager().registerEvents(listener, LobbyClicker.getInstance());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        event.setCancelled(true);
        String message = event.getMessage().trim();
        timeoutTask.cancel();
        HandlerList.unregisterAll(this);
        FoliaScheduler.runForEntity(player, LobbyClicker.getInstance(), () -> callback.accept(message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        timeoutTask.cancel();
        HandlerList.unregisterAll(this);
    }
}
