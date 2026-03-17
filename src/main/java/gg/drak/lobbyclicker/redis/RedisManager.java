package gg.drak.lobbyclicker.redis;

import gg.drak.lobbyclicker.LobbyClicker;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class RedisManager {
    private RedisClient client;
    private StatefulRedisPubSubConnection<String, String> subConnection;
    private StatefulRedisPubSubConnection<String, String> pubConnection;
    private String channelPrefix;
    private String serverId;
    private String serverPrettyName;
    private BukkitTask heartbeatTask;

    @Getter
    private final ConcurrentHashMap<String, CrossServerPlayer> crossServerPlayers = new ConcurrentHashMap<>();

    public void connect() {
        try {
            String host = LobbyClicker.getMainConfig().getRedisHost();
            int port = LobbyClicker.getMainConfig().getRedisPort();
            String password = LobbyClicker.getMainConfig().getRedisPassword();
            channelPrefix = LobbyClicker.getMainConfig().getRedisChannelPrefix();
            serverId = LobbyClicker.getMainConfig().getServerId();
            serverPrettyName = LobbyClicker.getMainConfig().getServerPrettyName();

            RedisURI.Builder uriBuilder = RedisURI.builder().withHost(host).withPort(port);
            if (password != null && !password.isEmpty()) {
                uriBuilder.withPassword(password.toCharArray());
            }

            client = RedisClient.create(uriBuilder.build());

            // Subscribe connection — listens to both online and social channels
            subConnection = client.connectPubSub();
            subConnection.addListener(new RedisPubSubAdapter<>() {
                @Override
                public void message(String channel, String message) {
                    if (channel.equals(channelPrefix + "online")) {
                        handleOnlineMessage(message);
                    } else if (channel.equals(channelPrefix + "social")) {
                        // Run social handler on main thread for safe Bukkit API access
                        Bukkit.getScheduler().runTask(LobbyClicker.getInstance(), () ->
                                RedisSyncHandler.handleSocialMessage(message));
                    }
                }
            });
            RedisPubSubCommands<String, String> subSync = subConnection.sync();
            subSync.subscribe(channelPrefix + "online", channelPrefix + "social");

            // Publish connection
            pubConnection = client.connectPubSub();

            // Publish all currently online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                publishJoin(player);
            }

            // Heartbeat: re-publish online players every 30 seconds, clean stale entries
            heartbeatTask = Bukkit.getScheduler().runTaskTimerAsynchronously(LobbyClicker.getInstance(), () -> {
                long now = System.currentTimeMillis();
                crossServerPlayers.entrySet().removeIf(e -> now - e.getValue().getLastSeen() > 60_000);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    publishJoin(player);
                }
            }, 600L, 600L);

            LobbyClicker.getInstance().logInfo("Redis connected to " + host + ":" + port);
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Failed to connect to Redis", e);
        }
    }

    public void shutdown() {
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                publishQuit(player);
            }
            if (heartbeatTask != null) heartbeatTask.cancel();
            if (subConnection != null) subConnection.close();
            if (pubConnection != null) pubConnection.close();
            if (client != null) client.shutdown();
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Error shutting down Redis", e);
        }
    }

    // --- Online channel ---

    public void publishJoin(Player player) {
        if (pubConnection == null) return;
        try {
            // Format: JOIN:uuid:name:serverId:prettyName
            String msg = "JOIN:" + player.getUniqueId().toString() + ":" + player.getName()
                    + ":" + serverId + ":" + serverPrettyName;
            pubConnection.sync().publish(channelPrefix + "online", msg);
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Failed to publish join to Redis", e);
        }
    }

    public void publishQuit(Player player) {
        if (pubConnection == null) return;
        try {
            String msg = "QUIT:" + player.getUniqueId().toString() + ":" + serverId;
            pubConnection.sync().publish(channelPrefix + "online", msg);
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Failed to publish quit to Redis", e);
        }
    }

    // --- Social channel ---

    public void publishSocial(String message) {
        if (pubConnection == null) return;
        try {
            pubConnection.sync().publish(channelPrefix + "social", message);
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Failed to publish social message to Redis", e);
        }
    }

    // --- Message handlers ---

    private void handleOnlineMessage(String message) {
        try {
            String[] parts = message.split(":", 5);
            if (parts.length < 3) return;

            String action = parts[0];
            String uuid = parts[1];

            // Ignore messages about local players
            if (Bukkit.getPlayer(java.util.UUID.fromString(uuid)) != null) return;

            switch (action) {
                case "JOIN": {
                    if (parts.length < 5) return;
                    String name = parts[2];
                    String server = parts[3];
                    String prettyName = parts[4];
                    if (server.equals(serverId)) return;
                    crossServerPlayers.put(uuid, new CrossServerPlayer(uuid, name, server, prettyName));
                    break;
                }
                case "QUIT": {
                    String server = parts[2];
                    if (server.equals(serverId)) return;
                    crossServerPlayers.remove(uuid);
                    break;
                }
            }
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Failed to handle Redis online message: " + message, e);
        }
    }

    public Collection<CrossServerPlayer> getCrossServerPlayerList() {
        return crossServerPlayers.values();
    }

    public boolean isPlayerOnlineAnywhere(String uuid) {
        if (Bukkit.getPlayer(java.util.UUID.fromString(uuid)) != null) return true;
        return crossServerPlayers.containsKey(uuid);
    }

    public CrossServerPlayer getCrossServerPlayer(String uuid) {
        return crossServerPlayers.get(uuid);
    }

    @Getter
    public static class CrossServerPlayer {
        private final String uuid;
        private final String name;
        private final String server;
        private final String prettyName;
        private long lastSeen;

        public CrossServerPlayer(String uuid, String name, String server, String prettyName) {
            this.uuid = uuid;
            this.name = name;
            this.server = server;
            this.prettyName = prettyName;
            this.lastSeen = System.currentTimeMillis();
        }
    }
}
