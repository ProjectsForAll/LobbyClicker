package gg.drak.lobbyclicker.events;

import gg.drak.thebase.events.BaseEventHandler;
import host.plas.bou.events.ListenerConglomerate;
import gg.drak.lobbyclicker.LobbyClicker;
import org.bukkit.Bukkit;

public class AbstractConglomerate implements ListenerConglomerate {
    public AbstractConglomerate() {
        register();
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, LobbyClicker.getInstance());
        BaseEventHandler.bake(this, LobbyClicker.getInstance());
        LobbyClicker.getInstance().logInfo("Registered listeners for: &c" + this.getClass().getSimpleName());
    }
}
