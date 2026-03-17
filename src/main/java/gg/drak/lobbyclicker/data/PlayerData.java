package gg.drak.lobbyclicker.data;

import gg.drak.thebase.objects.Identifiable;
import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.events.own.PlayerCreationEvent;
import gg.drak.lobbyclicker.upgrades.UpgradeType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter @Setter
public class PlayerData implements Identifiable {
    private String identifier;
    private String name;
    private double cookies;
    private double totalCookiesEarned;
    private long timesClicked;
    private EnumMap<UpgradeType, Integer> upgrades;
    private AtomicBoolean fullyLoaded;
    private boolean soundEnabled = true;

    public PlayerData(String identifier, String name) {
        this.identifier = identifier;
        this.name = name;
        this.cookies = 0;
        this.totalCookiesEarned = 0;
        this.timesClicked = 0;
        this.upgrades = new EnumMap<>(UpgradeType.class);
        for (UpgradeType type : UpgradeType.values()) {
            upgrades.put(type, 0);
        }
        this.fullyLoaded = new AtomicBoolean(false);
    }

    public PlayerData(String identifier, String name, double cookies, double totalCookiesEarned, long timesClicked, String upgradeData) {
        this(identifier, name);
        this.cookies = cookies;
        this.totalCookiesEarned = totalCookiesEarned;
        this.timesClicked = timesClicked;
        this.upgrades = deserializeUpgrades(upgradeData);
    }

    public PlayerData(Player player) {
        this(player.getUniqueId().toString(), player.getName());
    }

    public PlayerData(String uuid) {
        this(uuid, "");
    }

    public void addCookies(double amount) {
        this.cookies += amount;
        this.totalCookiesEarned += amount;
    }

    public void removeCookies(double amount) {
        this.cookies -= amount;
    }

    public boolean canAfford(double amount) {
        return this.cookies >= amount;
    }

    public double getCps() {
        double cps = 0;
        for (UpgradeType type : UpgradeType.values()) {
            cps += type.getCpsPerLevel() * getUpgradeCount(type);
        }
        return cps;
    }

    public double getCpc() {
        double cpc = 1;
        for (UpgradeType type : UpgradeType.values()) {
            cpc += type.getCpcPerLevel() * getUpgradeCount(type);
        }
        return cpc;
    }

    public long getClickerEntropy() {
        long entropy = timesClicked;
        for (UpgradeType type : UpgradeType.values()) {
            entropy += (long) getUpgradeCount(type) * type.getEntropyWeight();
        }
        entropy += (long) (totalCookiesEarned / 100.0);
        return entropy;
    }

    public int getUpgradeCount(UpgradeType type) {
        return upgrades.getOrDefault(type, 0);
    }

    public void setUpgradeCount(UpgradeType type, int count) {
        upgrades.put(type, count);
    }

    public boolean buyUpgrade(UpgradeType type) {
        double cost = type.getCost(getUpgradeCount(type));
        if (!canAfford(cost)) return false;
        removeCookies(cost);
        setUpgradeCount(type, getUpgradeCount(type) + 1);
        return true;
    }

    public String serializeUpgrades() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<UpgradeType, Integer> entry : upgrades.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey().name()).append(":").append(entry.getValue());
        }
        return sb.toString();
    }

    public static EnumMap<UpgradeType, Integer> deserializeUpgrades(String data) {
        EnumMap<UpgradeType, Integer> map = new EnumMap<>(UpgradeType.class);
        for (UpgradeType type : UpgradeType.values()) {
            map.put(type, 0);
        }
        if (data != null && !data.isEmpty()) {
            for (String part : data.split(";")) {
                String[] kv = part.split(":");
                if (kv.length == 2) {
                    try {
                        UpgradeType type = UpgradeType.valueOf(kv[0]);
                        map.put(type, Integer.parseInt(kv[1]));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        return map;
    }

    public Optional<Player> asPlayer() {
        try {
            return Optional.ofNullable(Bukkit.getPlayer(UUID.fromString(identifier)));
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Failed to get player from identifier: " + identifier, e);
            return Optional.empty();
        }
    }

    public Optional<OfflinePlayer> asOfflinePlayer() {
        try {
            return Optional.of(Bukkit.getOfflinePlayer(UUID.fromString(identifier)));
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Failed to get offline player from identifier: " + identifier, e);
            return Optional.empty();
        }
    }

    public boolean isOnline() {
        return asPlayer().isPresent();
    }

    public void load() {
        PlayerManager.loadPlayer(this);
    }

    public void unload() {
        PlayerManager.unloadPlayer(this);
    }

    public void save() {
        PlayerManager.savePlayer(this);
    }

    public void save(boolean async) {
        PlayerManager.savePlayer(this, async);
    }

    public void augment(CompletableFuture<Optional<PlayerData>> future, boolean isGet) {
        fullyLoaded.set(false);

        future.whenComplete((data, error) -> {
            if (error != null) {
                LobbyClicker.getInstance().logWarning("Failed to augment player data", error);
                this.fullyLoaded.set(true);
                return;
            }

            if (data.isPresent()) {
                PlayerData newData = data.get();
                this.name = newData.getName();
                this.cookies = newData.getCookies();
                this.totalCookiesEarned = newData.getTotalCookiesEarned();
                this.timesClicked = newData.getTimesClicked();
                this.upgrades = newData.getUpgrades();
            } else {
                if (!isGet) {
                    new PlayerCreationEvent(this).fire();
                    this.save();
                }
            }

            this.fullyLoaded.set(true);
        });
    }

    public boolean isFullyLoaded() {
        return fullyLoaded.get();
    }

    public void saveAndUnload(boolean async) {
        save(async);
        unload();
    }

    public void saveAndUnload() {
        saveAndUnload(true);
    }

    public PlayerData waitUntilFullyLoaded() {
        while (!isFullyLoaded()) {
            Thread.onSpinWait();
        }
        return this;
    }
}
