package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XMaterial;
import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.ShopDef;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.util.ConfigUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Level;

public final class Menu {

    /**
     * The GUI that is projected onto the screen when a {@link Player} opens the
     * {@link Menu}.
     */
    private final Gui GUI;

    private Boolean hasClicked = false;

    /**
     * A {@link Map} that will store our {@link Shop}s when the server first
     * starts.
     *
     * @key The index on the {@link Menu} that this shop is located at.
     * @value The shop.
     */
    public Menu() {
        this.GUI = new Gui(Main.getINSTANCE(), ConfigUtil.getMenuRows(), "" + ConfigUtil.getMenuTitle());
    }

    public void itemWarmup() {
        Main.getINSTANCE().getLogger().log(Level.INFO, "Warming Items...");
        long startTime = System.currentTimeMillis();
        Main.getINSTANCE().getShops().values().stream().filter(shopDef -> (shopDef.getItemType() == ItemType.SHOP)).forEachOrdered(shopDef -> {
            new Shop(null, shopDef.getShop(), shopDef.getName(), shopDef.getDescription(), shopDef.getLore(), this)
                    .loadItems();
        });
        long estimatedTime = System.currentTimeMillis() - startTime;
        Main.getINSTANCE().getLogger().log(Level.INFO, "Item warming completed in: {0}ms", estimatedTime);
    }

    /**
     * Preloads the configs into their corresponding objects.
     *
     * @param player - The player warming up the GUI
     */
    public void preLoad(Player player) {

        ShopPane page = new ShopPane(9, 1);

        Main.getINSTANCE().getShops().values().forEach(shopDef -> {
            if (shopDef.getItemType() != ItemType.SHOP || player.hasPermission("guishop.shop." + shopDef.getShop())
                    || player.hasPermission("guishop.shop.*") || player.isOp()) {
                page.addItem(buildMenuItem(shopDef.getItemID(), shopDef));
            } else {
                page.addBlankItem();
            }
        });

        GUI.addPane(page);

    }

    public GuiItem buildMenuItem(String itemID, ShopDef shopDef) {

        ItemStack itemStack = XMaterial.matchXMaterial(itemID).get().parseItem();

        if (shopDef.getItemType() != ItemType.BLANK) {
            setName(itemStack, shopDef.getName(), shopDef.getLore(), shopDef);
        }
        return new GuiItem(itemStack);
    }

    /**
     * Opens the GUI in this {@link Menu}.
     *
     * @param player - The player the GUI will display to
     */
    public void open(Player player) {

        if (!player.hasPermission("guishop.use") && !player.isOp()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(Main.getINSTANCE().getMainConfig().getString("no-permission"))));
            return;
        }

        if (Main.getINSTANCE().getMainConfig().getStringList("disabled-worlds").contains(player.getWorld().getName())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(Main.getINSTANCE().getMainConfig().getString("disabled-world"))));
            return;
        }

        preLoad(player);

        GUI.setOnTopClick(this::onShopClick);
        GUI.setOnBottomClick(event -> {
            event.setCancelled(true);
        });
        if (Main.getCREATOR().contains(player.getName())) {
            GUI.setOnClose(event -> onClose(event));
        }
        GUI.show(player);

    }

    /**
     * Sets the item's display name.
     */
    private ItemStack setName(ItemStack item, String name, List<String> lore, ShopDef shopDef) {
        ItemMeta IM = item.getItemMeta();

        if (name != null) {
            assert IM != null;
            IM.setDisplayName(name);
        }

        if (lore != null && !lore.isEmpty() && shopDef.getItemType() == ItemType.SHOP) {
            assert IM != null;
            IM.setLore(lore);
        }

        item.setItemMeta(IM);

        return item;

    }

    /**
     * Handle global inventory click events, check if inventory is for GUIShop,
     * if so, run logic.
     */
    private void onShopClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        hasClicked = true;

        ShopDef shopDef = new ArrayList<>(Main.getINSTANCE().getShops().values()).get(e.getSlot());

        if (shopDef.getItemType() == ItemType.SHOP) {
            openShop(player, shopDef);
        }

    }

    public Shop openShop(Player player, ShopDef shopDef) {

        if (shopDef.getItemType() == ItemType.SHOP) {
            /*
			 * The currently open shop associated with this Menu instance.
             */
            Shop openShop = new Shop(player, shopDef.getShop(), shopDef.getName(), shopDef.getDescription(), shopDef.getLore(),
                        this);
           

            openShop.loadItems();
            openShop.open(player);
            return openShop;
        }
        return null;
    }

    private void onClose(InventoryCloseEvent e) {
        if (!hasClicked) {
            Player p = (Player) e.getPlayer();
            if (Main.getCREATOR().contains(p.getName())) {
                Main.getCREATOR().remove(p.getName());
            }
        }
    }

}
