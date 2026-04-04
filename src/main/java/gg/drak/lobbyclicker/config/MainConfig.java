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
        isSimpleMode();
        isSocialFeaturesEnabled();
        isRealmSettingsMenuEnabled();
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

    public int getMaxClicksPerSecond() {
        reloadResource();
        return getOrSetDefault("clicker.max-clicks-per-second", 20);
    }

    public boolean isSimpleMode() {
        reloadResource();
        return getOrSetDefault("clicker.simple-mode", false);
    }

    /**
     * When false, the Social hub button is hidden on the main clicker GUI and Settings shifts into its slot.
     */
    public boolean isSocialFeaturesEnabled() {
        reloadResource();
        return getOrSetDefault("clicker.gui.social-enabled", true);
    }

    /**
     * When false, opening Settings from the main clicker goes straight to Player Settings (no realm hub step).
     */
    public boolean isRealmSettingsMenuEnabled() {
        reloadResource();
        return getOrSetDefault("clicker.gui.realm-settings-enabled", true);
    }
}
