package com.pablo67340.guishop.listenable;

import org.bukkit.entity.Player;

import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.*;

import com.github.stefvanschie.inventoryframework.Gui;

import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.util.ConfigUtil;

public final class Sell {

    private Gui GUI;

    /**
     * Open the {@link Sell} GUI.
     *
     * @param player - The player the GUI will display to
     */
    public void open(Player player) {
        GUI = new Gui(Main.getINSTANCE(), 6, ConfigUtil.getSellTitle());
        GUI.setOnClose(this::onSellClose);
        StaticPane pane = new StaticPane(0, 0, 9, 6);
        GUI.addPane(pane);
        GUI.show(player);
    }

    /**
     * Sell items inside the {@link Sell} GUI.
     *
     * @param player - The player selling items
     */
    public void sell(Player player) {
        sellItems(player, GUI.getInventory().getContents());
        GUI.getInventory().clear();
    }

    /**
     * Sells the specified items on the behalf of a player
     *
     * @param player the player
     * @param items the items
     */
    public static void sellItems(Player player, ItemStack[] items) {
        double moneyToGive = 0;
        boolean couldntSell = false;
        int countSell = 0;
        for (ItemStack item : items) {

            if (item == null) {
                continue;
            }

            String itemString = Item.getItemStringForItemStack(item);
            
            Main.debugLog("checking if: "+itemString+" is in item table");
            
            Main.debugLog(Main.getINSTANCE().getITEMTABLE().toString());

            Item shopItem = Main.getINSTANCE().getITEMTABLE().get(itemString);
            if (shopItem == null || !shopItem.hasSellPrice()) {
                countSell += 1;
                couldntSell = true;
                player.getInventory().addItem(item);
                continue;
            }

            int quantity = item.getAmount();

            // buy price must be defined for dynamic pricing to work
            if (ConfigUtil.isDynamicPricing() && shopItem.isUseDynamicPricing() && shopItem.hasBuyPrice()) {
                moneyToGive += Main.getDYNAMICPRICING().calculateSellPrice(itemString, quantity,
                        shopItem.getBuyPriceAsDouble(), shopItem.getSellPriceAsDouble());
                Main.getDYNAMICPRICING().sellItem(itemString, quantity);
            } else {
                moneyToGive += quantity * shopItem.getSellPriceAsDouble();
            }

        }

        if (couldntSell) {
            player.sendMessage(ConfigUtil.getPrefix() + " " + ConfigUtil.getCantSell().replace("{count}", countSell + ""));
        }
        roundAndGiveMoney(player, moneyToGive);
    }

    /**
     * Rounds the amount and deposits it on behalf of the player.
     *
     * @param player the player
     * @param moneyToGive the amount to give
     */
    public static void roundAndGiveMoney(Player player, double moneyToGive) {
        Double moneyToGiveRounded = (double) Math.round(moneyToGive * 100) / 100;

        if (moneyToGiveRounded > 0) {
            Main.getECONOMY().depositPlayer(player, moneyToGiveRounded);

            player.sendMessage(ConfigUtil.getSold() + moneyToGiveRounded + ConfigUtil.getAdded());
        }
    }

    private void onSellClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        sell(player);
    }

}
