/*
 * Copyright (C) 2012-2014 Soomla Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soomla.store;

import android.text.TextUtils;
import com.soomla.SoomlaUtils;
import com.soomla.data.KeyValueStorage;
import com.soomla.store.data.StorageManager;
import com.soomla.store.data.StoreInfo;
import com.soomla.store.data.VirtualCurrencyStorage;
import com.soomla.store.data.VirtualGoodsStorage;
import com.soomla.store.domain.PurchasableVirtualItem;
import com.soomla.store.domain.VirtualItem;
import com.soomla.store.domain.virtualCurrencies.VirtualCurrency;
import com.soomla.store.domain.virtualGoods.EquippableVG;
import com.soomla.store.domain.virtualGoods.UpgradeVG;
import com.soomla.store.domain.virtualGoods.VirtualGood;
import com.soomla.store.exceptions.InsufficientFundsException;
import com.soomla.store.exceptions.NotEnoughGoodsException;
import com.soomla.store.exceptions.VirtualItemNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class will help you do your day to day virtual economy operations easily.
 * You can give or take items from your users. You can buy items or upgrade them.
 * You can also check their equipping status and change it.
 */
public class StoreInventory {

    /**
     * Buys the item with the given <code>itemId</code>.
     *
     * @param itemId id of item to be purchased
     * @param payload a string you want to be assigned to the purchase. This string
     *   is saved in a static variable and will be given bacl to you when the
     *   purchase is completed.
     * @throws InsufficientFundsException
     * @throws VirtualItemNotFoundException
     */
    public static void buy(String itemId, String payload) throws InsufficientFundsException,
            VirtualItemNotFoundException {
        PurchasableVirtualItem pvi = (PurchasableVirtualItem) StoreInfo.getVirtualItem(itemId);
        pvi.buy(payload);
    }

    /** VIRTUAL ITEMS **/

    /**
     * Retrieves the balance of the virtual item with the given <code>itemId</code>.
     *
     * @param itemId id of the virtual item to be fetched.
     * @return balance of the virtual item with the given <code>itemId</code>.
     * @throws VirtualItemNotFoundException
     */
    public static int getVirtualItemBalance(String itemId) throws VirtualItemNotFoundException {
        VirtualItem item = StoreInfo.getVirtualItem(itemId);
        return StorageManager.getVirtualItemStorage(item).getBalance(item.getItemId());
    }

    /**
     * Gives your user the given amount of the virtual item with the given <code>itemId</code>.
     * For example, when your user plays your game for the first time you GIVE him/her 1000 gems.
     *
     * NOTE: This action is different than buy -
     * You use <code>give(int amount)</code> to give your user something for free.
     * You use <code>buy()</code> to give your user something and you get something in return.
     *
     * @param itemId id of the virtual item to be given
     * @param amount amount of the item to be given
     * @throws VirtualItemNotFoundException
     */
    public static void giveVirtualItem(String itemId, int amount)
            throws VirtualItemNotFoundException  {
        VirtualItem item = StoreInfo.getVirtualItem(itemId);
        item.give(amount);
    }

    /**
     * Takes from your user the given amount of the virtual item with the given <code>itemId</code>.
     * For example, when your user requests a refund you need to TAKE the item he/she is returning.
     *
     * @param itemId id of the virtual item to be taken
     * @param amount amount of the item to be given
     * @throws VirtualItemNotFoundException
     */
    public static void takeVirtualItem(String itemId, int amount)
            throws VirtualItemNotFoundException  {
        VirtualItem item = StoreInfo.getVirtualItem(itemId);
        item.take(amount);
    }

    /** VIRTUAL GOODS **/

    /**
     * Equips the virtual good with the given <code>goodItemId</code>.
     * Equipping means that the user decides to currently use a specific virtual good.
     * For more details and examples see {@link EquippableVG}.
     *
     * @param goodItemId id of the virtual good to be equipped
     * @throws VirtualItemNotFoundException
     * @throws ClassCastException
     * @throws NotEnoughGoodsException
     */
    public static void equipVirtualGood(String goodItemId) throws
            VirtualItemNotFoundException, ClassCastException, NotEnoughGoodsException{
        EquippableVG good = (EquippableVG) StoreInfo.getVirtualItem(goodItemId);

        try {
            good.equip();
        } catch (NotEnoughGoodsException e) {
            SoomlaUtils.LogError("StoreInventory", "UNEXPECTED! Couldn't equip something");
            throw e;
        }
    }

    /**
     * Unequips the virtual good with the given <code>goodItemId</code>. Unequipping means that the
     * user decides to stop using the virtual good he/she is currently using.
     * For more details and examples see {@link EquippableVG}.
     *
     * @param goodItemId id of the virtual good to be unequipped
     * @throws VirtualItemNotFoundException
     * @throws ClassCastException
     */
    public static void unEquipVirtualGood(String goodItemId) throws
            VirtualItemNotFoundException, ClassCastException{
        EquippableVG good = (EquippableVG) StoreInfo.getVirtualItem(goodItemId);

        good.unequip();
    }

    /**
     * Checks if the virtual good with the given <code>goodItemId</code> is currently equipped.
     *
     * @param goodItemId id of the virtual good who we want to know if is equipped
     * @return true if the virtual good is equipped, false otherwise
     * @throws VirtualItemNotFoundException
     * @throws ClassCastException
     */
    public static boolean isVirtualGoodEquipped(String goodItemId) throws
            VirtualItemNotFoundException, ClassCastException{
        EquippableVG good = (EquippableVG) StoreInfo.getVirtualItem(goodItemId);

        return StorageManager.getVirtualGoodsStorage().isEquipped(good.getItemId());
    }

    /**
     * Retrieves the upgrade level of the virtual good with the given <code>goodItemId</code>.
     *
     * For Example:
     * Let's say there's a strength attribute to one of the characters in your game and you provide
     * your users with the ability to upgrade that strength on a scale of 1-3.
     * This is what you've created:
     *  1. <code>SingleUseVG</code> for "strength"
     *  2. <code>UpgradeVG</code> for strength 'level 1'.
     *  3. <code>UpgradeVG</code> for strength 'level 2'.
     *  4. <code>UpgradeVG</code> for strength 'level 3'.
     * In the example, this function will retrieve the upgrade level for "strength" (1, 2, or 3).
     *
     * @param goodItemId id of the virtual good whose upgrade level we want to know
     * @return upgrade level of the good with the given id
     * @throws VirtualItemNotFoundException
     */
    public static int getGoodUpgradeLevel(String goodItemId) throws VirtualItemNotFoundException {
        VirtualGood good = (VirtualGood) StoreInfo.getVirtualItem(goodItemId);
        String upgradeVGItemId = StorageManager.getVirtualGoodsStorage().getCurrentUpgrade(good.getItemId());
        UpgradeVG upgradeVG = null;
        try {
            upgradeVG = (UpgradeVG) StoreInfo.getVirtualItem(upgradeVGItemId);
        } catch (VirtualItemNotFoundException e) {
            SoomlaUtils.LogError("SOOMLA StoreInventory", "This is BAD! Can't find the current upgrade (" + upgradeVGItemId + ") of: " + good.getItemId());
            return 0;
        }
        if (upgradeVG == null) {
            return 0; //no upgrade
        }

        UpgradeVG first = StoreInfo.getGoodFirstUpgrade(goodItemId);
        int level = 1;
        while (!first.equals(upgradeVG)) {
            first = (UpgradeVG) StoreInfo.getVirtualItem(first.getNextItemId());
            level++;
        }

        return level;
    }

    /**
     * Retrieves the itemId of the current upgrade of the virtual good with the given
     * <code>goodItemId</code>.
     *
     * @param goodItemId id of the virtual good whose upgrade id we want to know
     * @return upgrade id if exists, or empty string otherwise
     * @throws VirtualItemNotFoundException
     */
    public static String getGoodCurrentUpgrade(String goodItemId)
            throws VirtualItemNotFoundException {
        VirtualGood good = (VirtualGood) StoreInfo.getVirtualItem(goodItemId);

        String upgradeVGItemId = StorageManager.getVirtualGoodsStorage().getCurrentUpgrade(good.getItemId());
        UpgradeVG upgradeVG = null;
        try {
            upgradeVG = (UpgradeVG) StoreInfo.getVirtualItem(upgradeVGItemId);
        } catch (VirtualItemNotFoundException e) {
            SoomlaUtils.LogDebug("SOOMLA StoreInventory", "This is BAD! Can't find the current upgrade (" + upgradeVGItemId + ") of: " + good.getItemId());
        }

        if (upgradeVG == null) {
            return "";
        }
        return upgradeVG.getItemId();
    }

    /**
     * Upgrades the virtual good with the given <code>goodItemId</code> by doing the following:
     * 1. Checks if the good is currently upgraded or if this is the first time being upgraded.
     * 2. If the good is currently upgraded, upgrades to the next upgrade in the series, or in
     *    other words, <code>buy()</code>s the next upgrade. In case there are no more upgrades
     *    available(meaning the current upgrade is the last available), the function returns.
     * 3. If the good has never been upgraded before, the function upgrades it to the first
     *    available upgrade, or in other words, <code>buy()</code>s the first upgrade in the series.
     *
     * @param goodItemId the id of the virtual good to be upgraded
     * @throws VirtualItemNotFoundException
     * @throws InsufficientFundsException
     */
    public static void upgradeVirtualGood(String goodItemId)
            throws VirtualItemNotFoundException, InsufficientFundsException {
        VirtualGood good = (VirtualGood) StoreInfo.getVirtualItem(goodItemId);

        String upgradeVGItemId = StorageManager.getVirtualGoodsStorage().getCurrentUpgrade(good.getItemId());
        UpgradeVG upgradeVG = null;
        try {
            upgradeVG = (UpgradeVG) StoreInfo.getVirtualItem(upgradeVGItemId);
        } catch (VirtualItemNotFoundException e) {
            SoomlaUtils.LogDebug("SOOMLA StoreInventory", "This is BAD! Can't find the current upgrade (" + upgradeVGItemId + ") of: " + good.getItemId());
        }

        if (upgradeVG != null) {
            String nextItemId = upgradeVG.getNextItemId();
            if (TextUtils.isEmpty(nextItemId)) {
                return;
            }
            UpgradeVG vgu = (UpgradeVG) StoreInfo.getVirtualItem(nextItemId);
            vgu.buy("");
        } else {
            UpgradeVG first = StoreInfo.getGoodFirstUpgrade(goodItemId);
            if (first != null) {
                first.buy("");
            }
        }
    }

    /**
     * Upgrades the good with the given <code>upgradeItemId</code> for FREE (you are GIVING him/her
     * the upgrade). In case that the good is not an upgradeable item, an error message will be
     * produced. <code>forceUpgrade()</code> is different than <code>upgradeVirtualGood()<code>
     * because <code>forceUpgrade()</code> is a FREE upgrade.
     *
     * @param upgradeItemId id of the virtual good who we want to force an upgrade upon
     * @throws VirtualItemNotFoundException
     */
    public static void forceUpgrade(String upgradeItemId) throws VirtualItemNotFoundException {
        try {
            UpgradeVG upgradeVG = (UpgradeVG) StoreInfo.getVirtualItem(upgradeItemId);
            upgradeVG.give(1);
        } catch (ClassCastException ex) {
            SoomlaUtils.LogError("SOOMLA StoreInventory",
                    "The given itemId was of a non UpgradeVG VirtualItem. Can't force it.");
        }
    }

    /**
     * Removes all upgrades from the virtual good with the given <code>goodItemId</code>.
     *
     * @param goodItemId id of the virtual good we want to remove all upgrades from
     * @throws VirtualItemNotFoundException
     */
    public static void removeUpgrades(String goodItemId) throws VirtualItemNotFoundException {
        List<UpgradeVG> upgrades = StoreInfo.getGoodUpgrades(goodItemId);
        for (UpgradeVG upgrade : upgrades) {
            StorageManager.getVirtualGoodsStorage().remove(upgrade.getItemId(), 1, true);
        }
        VirtualGood good = (VirtualGood) StoreInfo.getVirtualItem(goodItemId);
        StorageManager.getVirtualGoodsStorage().removeUpgrades(good.getItemId());
    }

    public static HashMap<String, HashMap<String, Object>> allItemsBalances() {
        SoomlaUtils.LogDebug(TAG, "Fetching all items balances");

        HashMap<String, HashMap<String, Object>> itemsDict = new HashMap<String, HashMap<String, Object>>();

        SoomlaUtils.LogDebug(TAG, "Fetching balances for Currencies");
        // we're cloning the list to avoid situations where someone else tries to manipulate list while we iterate
        List<VirtualCurrency> currencies = new ArrayList<VirtualCurrency>(StoreInfo.getCurrencies());
        for(VirtualCurrency currency : currencies) {
            HashMap<String, Object> updatedValues = new HashMap<String, Object>();
            updatedValues.put("balance", StorageManager.getVirtualCurrencyStorage().getBalance(currency.getItemId()));

            itemsDict.put(currency.getItemId(), updatedValues);
        }

        SoomlaUtils.LogDebug(TAG, "Fetching balances for Goods");
        // we're cloning the list to avoid situations where someone else tries to manipulate list while we iterate
        List<VirtualGood> goods = new ArrayList<VirtualGood>(StoreInfo.getGoods());
        for(VirtualGood good : goods) {
            HashMap<String, Object> updatedValues = new HashMap<String, Object>();

            updatedValues.put("balance", StorageManager.getVirtualGoodsStorage().getBalance(good.getItemId()));

            if (good instanceof EquippableVG) {
                updatedValues.put("equipped", StorageManager.getVirtualGoodsStorage().isEquipped(good.getItemId()));
            }

            if (StoreInfo.hasUpgrades(good.getItemId())) {
                String vguId = StorageManager.getVirtualGoodsStorage().getCurrentUpgrade(good.getItemId());
                updatedValues.put("currentUpgrade", (TextUtils.isEmpty(vguId) ? "none" : vguId ));
            }

            itemsDict.put(good.getItemId(), updatedValues);
        }

        return itemsDict;
    }

    public static boolean resetAllItemsBalances(HashMap<String, HashMap<String, Object>> replaceBalances) {
        if (replaceBalances == null) {
            return false;
        }

        SoomlaUtils.LogDebug(TAG, "Resetting balances");

        clearCurrentState();

        SoomlaUtils.LogDebug(TAG, "Current state was cleared");

        try {
            for (String itemId : replaceBalances.keySet()) {
                HashMap<String, Object> updatedValues = replaceBalances.get(itemId);

                VirtualItem item = null;
                try {
                    item = StoreInfo.getVirtualItem(itemId);
                } catch (VirtualItemNotFoundException e) {
                    SoomlaUtils.LogError(TAG, "The given itemId " + itemId + " was not found. Can't force it.");
                    continue;
                }

                Object rawBalance = updatedValues.get("balance");
                if (rawBalance != null) {
                    Integer updatedBalance = (Integer) rawBalance;
                    if (item != null) {
                        item.resetBalance(updatedBalance, false);
                        SoomlaUtils.LogDebug(TAG, "finished balance sync for itemId: " + itemId);
                    }
                }

                Object rawEquippedState = updatedValues.get("equipped");
                if (rawEquippedState != null) {
                    try {
                        EquippableVG equippableItem = (EquippableVG) item;
                        if (equippableItem != null) {
                            Boolean equipState = (Boolean) rawEquippedState;
                            if (equipState) {
                                equippableItem.equip(false);
                            } else {
                                equippableItem.unequip(false);
                            }
                        }
                        SoomlaUtils.LogDebug(TAG, "finished equip balance sync for itemId: " + itemId);
                    } catch (NotEnoughGoodsException e) {
                        SoomlaUtils.LogError(TAG, "the item " + itemId + " was not purchased, so cannot be equipped");
                    } catch (ClassCastException exx) {
                        SoomlaUtils.LogError(TAG, "tried to equip a non-equippable item: " + itemId);
                    }
                }

                Object rawCurrentUpgrade = updatedValues.get("currentUpgrade");
                if (rawCurrentUpgrade != null) {
                    String currentUpgradeId = (String) rawCurrentUpgrade;
                    if (!TextUtils.isEmpty(currentUpgradeId)) {
                        try {
                            UpgradeVG upgradeVG = (UpgradeVG) StoreInfo.getVirtualItem(currentUpgradeId);
                            upgradeVG.give(1, false);

                            SoomlaUtils.LogDebug(TAG, "finished upgrade balance sync for itemId: " + itemId);
                        } catch (VirtualItemNotFoundException ex) {
                            SoomlaUtils.LogError(TAG, "The given upgradeId " + currentUpgradeId + " was not found. Can't force it.");
                        } catch (ClassCastException ex) {
                            SoomlaUtils.LogError(TAG, "The given upgradeId was of a non UpgradeVG VirtualItem. Can't force it.");
                        }
                    }
                }
            }

            return true;
        }
        catch (Exception e) {
            SoomlaUtils.LogError(TAG, "Unknown error has occurred while resetting item balances " + e.getMessage());
        }

        return false;
    }

    private static void clearCurrentState() {
        List<String> allKeys = KeyValueStorage.getEncryptedKeys();

        for (String key : allKeys) {
            if (key.startsWith(StoreInfo.DB_NONCONSUMABLE_KEY_PREFIX) ||
                    key.startsWith(VirtualCurrencyStorage.DB_CURRENCY_KEY_PREFIX) ||
                    key.startsWith(VirtualGoodsStorage.DB_KEY_GOOD_PREFIX)) {
                KeyValueStorage.deleteKeyValue(key);
            }
        }
    }

    private static final String TAG = "SOOMLA StoreInventory"; //used for Log messages
}
