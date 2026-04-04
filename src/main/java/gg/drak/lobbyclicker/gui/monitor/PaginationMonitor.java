package gg.drak.lobbyclicker.gui.monitor;

import gg.drak.lobbyclicker.gui.BannerChar;
import gg.drak.lobbyclicker.gui.MenuText;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A bordered monitor with built-in pagination support.
 */
public abstract class PaginationMonitor extends SimpleGuiMonitor {

    protected int page;
    protected boolean wrapAround = true;

    public static final int[] PAGINATED_SLOTS = {
            11, 12, 13, 14, 15,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public PaginationMonitor(Player player, String id, String titleMiniMessage, int page) {
        super(player, id, titleMiniMessage + (page > 0
                ? " <gray>(" + MenuText.esc("Page") + " " + (page + 1) + ")</gray>"
                : ""), 6);
        this.page = page;
    }

    public void setWrapAround(boolean wrap) {
        this.wrapAround = wrap;
    }

    public int getItemsPerPage() {
        return PAGINATED_SLOTS.length;
    }

    protected <T> void populatePagedContent(List<T> items, BiConsumer<T, Integer> slotPlacer) {
        int start = page * PAGINATED_SLOTS.length;
        int end = Math.min(start + PAGINATED_SLOTS.length, items.size());
        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex >= PAGINATED_SLOTS.length) break;
            slotPlacer.accept(items.get(i), PAGINATED_SLOTS[slotIndex]);
        }
    }

    protected <T> void populatePage(List<T> items, Function<T, Icon> iconFactory) {
        int start = page * PAGINATED_SLOTS.length;
        int end = Math.min(start + PAGINATED_SLOTS.length, items.size());
        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex >= PAGINATED_SLOTS.length) break;
            addItem(PAGINATED_SLOTS[slotIndex], iconFactory.apply(items.get(i)));
        }
    }

    protected <T> void addPaginationArrows(List<T> items, Consumer<Integer> pageOpener) {
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / getItemsPerPage()));

        boolean hasPrev = page > 0;
        boolean canWrapPrev = wrapAround && totalPages > 1 && page == 0;
        String prevLore = hasPrev
                ? "<yellow>" + MenuText.esc("Page") + " " + page + "/" + totalPages + "</yellow>"
                : (canWrapPrev
                        ? "<gray>" + MenuText.esc("Go to last page") + "</gray>"
                        : "<gray>" + MenuText.esc("No previous page") + "</gray>");
        Icon prev = BannerChar.of("<", BannerChar.BannerColor.RED, BannerChar.BannerColor.WHITE).toIcon(prevLore);
        ItemStack prevStack = prev.getItem();
        ItemMeta prevMeta = prevStack.getItemMeta();
        if (prevMeta != null) {
            prevMeta.setDisplayName(MenuText.legacySection("<yellow><bold>← " + MenuText.esc("Previous") + "</bold></yellow>"));
            MenuText.hideVanillaTooltips(prevMeta);
            prevStack.setItemMeta(prevMeta);
        }
        if (hasPrev) {
            prev.onClick(e -> pageOpener.accept(page - 1));
        } else if (canWrapPrev) {
            prev.onClick(e -> pageOpener.accept(totalPages - 1));
        }
        addItem(10, prev);

        boolean hasNext = (page + 1) * PAGINATED_SLOTS.length < items.size();
        boolean canWrapNext = wrapAround && totalPages > 1 && !hasNext;
        String nextLore = hasNext
                ? "<yellow>" + MenuText.esc("Page") + " " + (page + 2) + "/" + totalPages + "</yellow>"
                : (canWrapNext
                        ? "<gray>" + MenuText.esc("Go to first page") + "</gray>"
                        : "<gray>" + MenuText.esc("No next page") + "</gray>");
        Icon next = BannerChar.of(">", BannerChar.BannerColor.RED, BannerChar.BannerColor.WHITE).toIcon(nextLore);
        ItemStack nextStack = next.getItem();
        ItemMeta nextMeta = nextStack.getItemMeta();
        if (nextMeta != null) {
            nextMeta.setDisplayName(MenuText.legacySection("<yellow><bold>" + MenuText.esc("Next") + " →</bold></yellow>"));
            MenuText.hideVanillaTooltips(nextMeta);
            nextStack.setItemMeta(nextMeta);
        }
        if (hasNext) {
            next.onClick(e -> pageOpener.accept(page + 1));
        } else if (canWrapNext) {
            next.onClick(e -> pageOpener.accept(0));
        }
        addItem(16, next);
    }

    public static Icon playerHeadIcon(String uuid, String mmName, Consumer<Player> onClick, String... mmLore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            try {
                UUID uid = UUID.fromString(uuid);
                Player onlinePlayer = Bukkit.getPlayer(uid);
                if (onlinePlayer != null) {
                    meta.setOwningPlayer(onlinePlayer);
                } else {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(uid));
                }
            } catch (Exception ignored) {}
            meta.setDisplayName(MenuText.legacySection(mmName));
            if (mmLore.length > 0) {
                List<String> lore = new ArrayList<>(mmLore.length);
                for (String line : mmLore) {
                    lore.add(MenuText.legacySection(line));
                }
                meta.setLore(lore);
            }
            MenuText.hideVanillaTooltips(meta);
            head.setItemMeta(meta);
        }
        Icon icon = new Icon(head);
        if (onClick != null) {
            icon.onClick(e -> {
                if (e.getWhoClicked() instanceof Player) {
                    onClick.accept((Player) e.getWhoClicked());
                }
            });
        }
        return icon;
    }
}
