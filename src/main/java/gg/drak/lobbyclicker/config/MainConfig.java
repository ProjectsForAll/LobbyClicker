package gg.drak.lobbyclicker.config;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import gg.drak.lobbyclicker.LobbyClicker;

import java.util.UUID;

public class MainConfig extends SimpleConfiguration {
    public MainConfig() {
        super("config.yml", LobbyClicker.getInstance(), false);
    }

    @Override
    public void init() {
        getServerId();
        getServerPrettyName();
        isRedisEnabled();
        getRedisHost();
        getRedisPort();
        getRedisPassword();
        getRedisChannelPrefix();
    }

    public String getServerId() {
        reloadResource();
        return getOrSetDefault("server.id", UUID.randomUUID().toString());
    }

    public String getServerPrettyName() {
        reloadResource();
        return getOrSetDefault("server.pretty-name", "Server");
    }

    public boolean isRedisEnabled() {
        reloadResource();
        return getOrSetDefault("redis.enabled", false);
    }

    public String getRedisHost() {
        reloadResource();
        return getOrSetDefault("redis.host", "localhost");
    }

    public int getRedisPort() {
        reloadResource();
        return getOrSetDefault("redis.port", 6379);
    }

    public String getRedisPassword() {
        reloadResource();
        return getOrSetDefault("redis.password", "");
    }

    public String getRedisChannelPrefix() {
        reloadResource();
        return getOrSetDefault("redis.channel-prefix", "lobbyclicker:");
    }
}
