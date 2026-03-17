package gg.drak.lobbyclicker.events.own;

import gg.drak.thebase.events.components.BaseEvent;
import host.plas.bou.BukkitOfUtils;
import gg.drak.lobbyclicker.LobbyClicker;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OwnEvent extends BaseEvent {
    public OwnEvent() {
        super();
    }

    public LobbyClicker getPlugin() {
        return LobbyClicker.getInstance();
    }

    public BukkitOfUtils getBou() {
        return BukkitOfUtils.getInstance();
    }
}
