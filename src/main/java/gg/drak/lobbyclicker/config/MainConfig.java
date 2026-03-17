package gg.drak.lobbyclicker.config;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import gg.drak.lobbyclicker.LobbyClicker;

public class MainConfig extends SimpleConfiguration {
    public MainConfig() {
        super("config.yml", LobbyClicker.getInstance(), false);
    }

    @Override
    public void init() {

    }
}
