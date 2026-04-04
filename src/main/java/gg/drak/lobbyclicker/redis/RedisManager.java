package gg.drak.lobbyclicker.redis;

import gg.drak.lobbyclicker.LobbyClicker;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.LettuceFutures;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RedisManager {
    private RedisClient client;
    private StatefulRedisPubSubConnection<String, String> subConnection;
    private StatefulRedisPubSubConnection<String, String> pubConnection;
    @Getter
    private String channelPrefix;
    @Getter
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

            // Subscribe connection — listens to online, social, and data channels
            subConnection = client.connectPubSub();
            subConnection.addListener(new RedisPubSubAdapter<>() {
                @Override
                public void message(String channel, String message) {
                    if (channel.equals(channelPrefix + "online")) {
                        Bukkit.getScheduler().runTask(LobbyClicker.getInstance(), () -> handleOnlineMessage(message));
                    } else if (channel.equals(channelPrefix + "social")) {
                        Bukkit.getScheduler().runTask(LobbyClicker.getInstance(), () ->
                                RedisSyncHandler.handleSocialMessage(message));
                    } else if (channel.equals(channelPrefix + "data")) {
                        Bukkit.getScheduler().runTask(LobbyClicker.getInstance(), () ->
                                RedisSyncHandler.handleDataMessage(message, serverId));
                    }
                }
            });
            RedisPubSubCommands<String, String> subSync = subConnection.sync();
            subSync.subscribe(channelPrefix + "online", channelPrefix + "social", channelPrefix + "data");

            // Publish connection
            pubConnection = client.connectPubSub();

            // Publish all currently online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                publishJoin(player);
            }

            // Heartbeat on main thread: Bukkit.getOnlinePlayers() is not safe from async workers.
            heartbeatTask = Bukkit.getScheduler().runTaskTimer(LobbyClicker.getInstance(), () -> {
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
            if (heartbeatTask != null) {
                heartbeatTask.cancel();
                heartbeatTask = null;
            }

            final String prefix = channelPrefix;
            final String sid = serverId;
            final StatefulRedisPubSubConnection<String, String> pub = pubConnection;
            final StatefulRedisPubSubConnection<String, String> sub = subConnection;
            final RedisClient c = client;
            pubConnection = null;
            subConnection = null;
            client = null;

            List<String> quitMessages = new ArrayList<>();
            if (pub != null && prefix != null && sid != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    quitMessages.add("QUIT:" + player.getUniqueId() + ":" + sid);
                }
            }

            Thread closer = new Thread(() -> {
                try {
                    if (pub != null) {
                        List<RedisFuture<Long>> quits = new ArrayList<>();
                        for (String msg : quitMessages) {
                            quits.add(pub.async().publish(prefix + "online", msg));
                        }
                        if (!quits.isEmpty()) {
                            LettuceFutures.awaitAll(3, TimeUnit.SECONDS, quits.toArray(new RedisFuture[0]));
                        }
                    }
                    if (sub != null) sub.close();
                    if (pub != null) pub.close();
                    if (c != null) c.shutdown();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }, "LobbyClicker-RedisShutdown");
            closer.setDaemon(true);
            closer.start();
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Error shutting down Redis", e);
        }
    }

    // --- Online channel ---

    public void publishJoin(Player player) {
        // Format: JOIN:uuid:name:serverId:prettyName
        String msg = "JOIN:" + player.getUniqueId().toString() + ":" + player.getName()
                + ":" + serverId + ":" + serverPrettyName;
        publishAsync(channelPrefix + "online", msg, "join");
    }

    public void publishQuit(Player player) {
        String msg = "QUIT:" + player.getUniqueId().toString() + ":" + serverId;
        publishAsync(channelPrefix + "online", msg, "quit");
    }

    // --- Social channel ---

    public void publishSocial(String message) {
        publishAsync(channelPrefix + "social", message, "social");
    }

    // --- Data channel ---

    public void publishData(String message) {
        publishAsync(channelPrefix + "data", message, "data");
    }

    /**
     * Non-blocking publish so the main thread never waits on Redis I/O (sync publish blocks on CompletableFuture.get).
     */
    private void publishAsync(String channel, String message, String kind) {
        if (pubConnection == null) return;
        try {
            pubConnection.async().publish(channel, message).whenComplete((count, ex) -> {
                if (ex == null) return;
                Bukkit.getScheduler().runTask(LobbyClicker.getInstance(), () ->
                        LobbyClicker.getInstance().logWarning("Failed to publish " + kind + " to Redis", ex));
            });
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Failed to publish " + kind + " to Redis", e);
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

    public boolean isPlayerOnlineRemotely(String uuid) {
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
