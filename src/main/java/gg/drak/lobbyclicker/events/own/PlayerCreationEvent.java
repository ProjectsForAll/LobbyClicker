package gg.drak.lobbyclicker.events.own;

import gg.drak.lobbyclicker.data.PlayerData;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PlayerCreationEvent extends OwnEvent {
    private PlayerData data;

    public PlayerCreationEvent(PlayerData data) {
        super();
        this.data = data;
    }
}
