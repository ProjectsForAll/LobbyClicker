package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import host.plas.bou.gui.CornerColor;
import host.plas.bou.gui.GuiLayout;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ShopGui extends ClickerHubGui {
    private final PlayerData viewerData;
    private final PlayerData ownerData;

    public ShopGui(Player player, PlayerData viewerData, PlayerData ownerData) {
        super(player, CornerColor.GREEN);
        this.viewerData = viewerData;
        this.ownerData = ownerData;
    }

    @Override
    protected void buildAndOpen() {
        int size = GuiLayout.SIZE_SMALL;
        ItemStack[] contents = newShell(size, MenuText.legacySection("<green><bold>Shop</bold></green>"));

        contents[11] = ClickerGuiHelper.createIcon(org.bukkit.Material.CHEST,
                "<green><bold>Cookie Helpers</bold></green>",
                "", "<gray>Buy helpers to earn more cookies!</gray>").getItem();
        bind(11, "helpers", p -> new UpgradeGui(p, viewerData, ownerData).open());

        contents[15] = ClickerGuiHelper.createIcon(org.bukkit.Material.DIAMOND,
                "<aqua><bold>Upgrades</bold></aqua>",
                "", "<gray>One-time boosts and bonuses!</gray>").getItem();
        bind(15, "upgrades", p -> new ClickerUpgradeGui(p, viewerData, ownerData).open());

        int back = backSlot(size);
        contents[back] = ClickerGuiHelper.backButton("Back").getItem();
        bind(back, "back", p -> {
            if (ownerData.getIdentifier().equals(viewerData.getIdentifier())) {
                new ClickerGui(p, viewerData).open();
            } else {
                new ClickerGui(p, viewerData, ownerData).open();
            }
        });

        finishAndOpen(contents);
    }
}
