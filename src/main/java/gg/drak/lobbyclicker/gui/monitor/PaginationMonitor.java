package gg.drak.lobbyclicker.gui.monitor;

import gg.drak.lobbyclicker.gui.BannerChar;
import gg.drak.lobbyclicker.gui.BannerUtil;
import gg.drak.lobbyclicker.gui.GuiHelper;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A bordered monitor with built-in pagination support.
 *
 * Layout (6-row):
 * <pre>
 *   K K K K K K K K K     (row 0: top border)
 *   Y [<] . . . . [>] Y   (row 1: banner arrows at index 10 and 16)
 *   Y  . . . . . . .  Y   (row 2: content)
 *   Y  . . . . . . .  Y   (row 3: content)
 *   Y  . . . . . . .  Y   (row 4: content)
 *   [action bar]           (row 5: action bar)
 * </pre>
 *
 * Pagination arrows: index 10 ("<" banner) and index 16 (">" banner).
 * Always visible. Wrap around by default (last->first, first->last).
 * Paginated content slots: 11-15, 19-25, 28-34, 37-43 (5+7+7+7 = 26 items per page).
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

    public PaginationMonitor(Player player, String id, String title, int page) {
        super(player, id, title + (page > 0 ? " " + ChatColor.GRAY + "(Page " + (page + 1) + ")" : ""), 6);
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

    /**
     * Add prev/next pagination banner arrows at indexes 10 and 16.
     * Always shown. Wraps around by default (configurable via setWrapAround).
     */
    protected <T> void addPaginationArrows(List<T> items, Consumer<Integer> pageOpener) {
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / getItemsPerPage()));

        // "<" banner at index 10 (always visible)
        boolean hasPrev = page > 0;
        boolean canWrapPrev = wrapAround && totalPages > 1 && page == 0;
        String prevLore = hasPrev ? ChatColor.YELLOW + "Page " + page + "/" + totalPages
                : (canWrapPrev ? ChatColor.GRAY + "Go to last page" : ChatColor.GRAY + "No previous page");
        Icon prev = BannerChar.of("<", BannerChar.BannerColor.RED, BannerChar.BannerColor.WHITE).toIcon(prevLore);
        ItemStack prevStack = prev.getItem();
        ItemMeta prevMeta = prevStack.getItemMeta();
        if (prevMeta != null) {
            prevMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "\u2190 Previous");
            prevStack.setItemMeta(prevMeta);
        }
        if (hasPrev) {
            prev.onClick(e -> pageOpener.accept(page - 1));
        } else if (canWrapPrev) {
            prev.onClick(e -> pageOpener.accept(totalPages - 1));
        }
        addItem(10, prev);

        // ">" banner at index 16 (always visible)
        boolean hasNext = (page + 1) * PAGINATED_SLOTS.length < items.size();
        boolean canWrapNext = wrapAround && totalPages > 1 && !hasNext;
        String nextLore = hasNext ? ChatColor.YELLOW + "Page " + (page + 2) + "/" + totalPages
                : (canWrapNext ? ChatColor.GRAY + "Go to first page" : ChatColor.GRAY + "No next page");
        Icon next = BannerChar.of(">", BannerChar.BannerColor.RED, BannerChar.BannerColor.WHITE).toIcon(nextLore);
        ItemStack nextStack = next.getItem();
        ItemMeta nextMeta = nextStack.getItemMeta();
        if (nextMeta != null) {
            nextMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Next \u2192");
            nextStack.setItemMeta(nextMeta);
        }
        if (hasNext) {
            next.onClick(e -> pageOpener.accept(page + 1));
        } else if (canWrapNext) {
            next.onClick(e -> pageOpener.accept(0));
        }
        addItem(16, next);
    }

    /**
     * Creates a player head icon with name, lore, and click handler.
     */
    public static Icon playerHeadIcon(String uuid, String name, Consumer<Player> onClick, String... lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            try {
                UUID uid = UUID.fromString(uuid);
                // Prefer online player (has cached skin), fallback to offline
                Player onlinePlayer = Bukkit.getPlayer(uid);
                if (onlinePlayer != null) {
                    meta.setOwningPlayer(onlinePlayer);
                } else {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(uid));
                }
            } catch (Exception ignored) {}
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
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
