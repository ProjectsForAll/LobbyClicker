package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.social.RealmManager;
import mc.obliviate.inventory.Gui;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class RealmViewersGui extends Gui {
    private final PlayerData data;

    public RealmViewersGui(Player player, PlayerData data) {
        super(player, "realm-viewers", ChatColor.AQUA + "" + ChatColor.BOLD + "Realm Viewers", 4);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        Set<String> viewers = RealmManager.getViewers(data.getIdentifier());

        if (viewers.isEmpty()) {
            addItem(13, GuiHelper.createIcon(Material.PAPER, ChatColor.GRAY + "No viewers",
                    "", ChatColor.GRAY + "Nobody is viewing your realm right now."));
        } else {
            int slot = 10;
            for (String viewerUuid : viewers) {
                if (slot > 25) break;

                String name = viewerUuid.substring(0, 8);
                PlayerData vd = PlayerManager.getPlayer(viewerUuid).orElse(null);
                if (vd != null) name = vd.getName();

                boolean isFriend = data.getFriends().contains(viewerUuid);

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                if (meta != null) {
                    try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(viewerUuid))); } catch (Exception ignored) {}
                    meta.setDisplayName((isFriend ? ChatColor.GREEN : ChatColor.WHITE) + name);
                    meta.setLore(Arrays.asList(
                            isFriend ? ChatColor.GREEN + "Friend" : ChatColor.GRAY + "Visitor",
                            "", ChatColor.YELLOW + "Click for actions"
                    ));
                    head.setItemMeta(meta);
                }

                Icon icon = new Icon(head);
                String finalUuid = viewerUuid;
                icon.onClick(e -> new PlayerActionGui(player, data, finalUuid, "viewers").open());
                addItem(slot++, icon);
                if (slot == 17) slot = 19; // skip to next row
            }
        }

        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new SocialMainGui(player, data).open());
        addItem(31, back);
    }
}
