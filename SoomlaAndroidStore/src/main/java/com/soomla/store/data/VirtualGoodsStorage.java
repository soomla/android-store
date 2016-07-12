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

package com.soomla.store.data;

import android.text.TextUtils;
import com.soomla.BusProvider;
import com.soomla.SoomlaUtils;
import com.soomla.data.KeyValueStorage;
import com.soomla.store.domain.virtualGoods.UpgradeVG;
import com.soomla.store.events.GoodBalanceChangedEvent;
import com.soomla.store.events.GoodEquippedEvent;
import com.soomla.store.events.GoodUnEquippedEvent;
import com.soomla.store.events.GoodUpgradeEvent;
import com.soomla.store.exceptions.VirtualItemNotFoundException;

/**
 * This class provides basic storage operations on virtual goods.
 */
public class VirtualGoodsStorage extends VirtualItemStorage{

    /**
     * Constructor
     */
    public VirtualGoodsStorage() {
        mTag = "SOOMLA VirtualGoodsStorage";
    }

    /**
     * Removes any upgrade associated with the given <code>VirtualGood</code>.
     *
     * @param goodItemId the VirtualGood to remove upgrade from
     */
    public void removeUpgrades(String goodItemId) {
        removeUpgrades(goodItemId, true);
    }

    /**
     * Removes any upgrade associated with the given VirtualGood.
     *
     * @param goodItemId the virtual good to remove the upgrade from
     * @param notify if true post event to bus
     */
    public void removeUpgrades(String goodItemId, boolean notify) {
        SoomlaUtils.LogDebug(mTag, "Removing upgrade information from virtual good: " +
                goodItemId);

        String key = keyGoodUpgrade(goodItemId);

        KeyValueStorage.deleteKeyValue(key);

        if (notify) {
            BusProvider.getInstance().post(new GoodUpgradeEvent(goodItemId, null));
        }
    }

    /**
     * Assigns a specific upgrade to the given virtual good.
     *
     * @param goodItemId the virtual good to upgrade
     * @param upgradeVGItemId the upgrade to assign
     */
    public void assignCurrentUpgrade(String goodItemId, String upgradeVGItemId) {
        assignCurrentUpgrade(goodItemId, upgradeVGItemId, true);
    }

    /**
     * Assigns a specific upgrade to the given virtual good.
     *
     * @param goodItemId the VirtualGood to upgrade
     * @param upgradeVGItemId the upgrade to assign
     * @param notify if true post event to bus
     */
    public void assignCurrentUpgrade(String goodItemId, String upgradeVGItemId, boolean notify) {

        String upgradeItemId = getCurrentUpgrade(goodItemId);
        if (!TextUtils.isEmpty(upgradeItemId)) {
            try {
                UpgradeVG upgrade = (UpgradeVG) StoreInfo.getVirtualItem(upgradeItemId);

                if (upgrade != null && upgrade.getItemId().equals(
                        upgradeVGItemId)) {
                    return;
                }
            } catch (VirtualItemNotFoundException ignored) {

            }
        }

        SoomlaUtils.LogDebug(mTag, "Assigning upgrade " + upgradeVGItemId + " to virtual good: "
                + goodItemId);

        String key = keyGoodUpgrade(goodItemId);

        KeyValueStorage.setValue(key, upgradeVGItemId);

        if (notify) {
            BusProvider.getInstance().post(new GoodUpgradeEvent(goodItemId, upgradeVGItemId));
        }
    }

    /**
     * Retrieves the current upgrade for the given virtual good.
     *
     * @param goodItemId the virtual good to retrieve upgrade for
     * @return the current upgrade for the given virtual good
     */
    public String getCurrentUpgrade(String goodItemId) {
        SoomlaUtils.LogDebug(mTag, "Fetching upgrade to virtual good: " + goodItemId);

        String key = keyGoodUpgrade(goodItemId);

        String upItemId = KeyValueStorage.getValue(key);

        if (upItemId == null) {
            SoomlaUtils.LogDebug(mTag, "You tried to fetch the current upgrade of " + goodItemId
                    + " but there's not upgrade to it.");
            return null;
        }

        try {
            return upItemId;
        } catch (ClassCastException e) {
            SoomlaUtils.LogError(mTag,
                    "The current upgrade's itemId from the DB is not an UpgradeVG.");
        }

        return null;
    }

    /**
     * Checks if the given <code>EquippableVG</code> is currently equipped or not.
     *
     * @param goodItemId the <code>EquippableVG</code> to check the status for
     * @return true if the given good is equipped, false otherwise
     */
    public boolean isEquipped(String goodItemId){
        SoomlaUtils.LogDebug(mTag, "checking if virtual good with itemId: " + goodItemId +
                " is equipped.");

        String key = keyGoodEquipped(goodItemId);
        String val = KeyValueStorage.getValue(key);

        return val != null;
    }

    /**
     * Equips the given <code>EquippableVG</code>.
     *
     * @param goodItemId the <code>EquippableVG</code> to equip
     */
    public void equip(String goodItemId) {
        equip(goodItemId, true);
    }

    /**
     * Equips the given <code>EquippableVG</code>.
     *
     * @param goodItemId the EquippableVG to equip
     * @param notify if notify is true post event to bus
     */
    public void equip(String goodItemId, boolean notify) {
        if (isEquipped(goodItemId)) {
            return;
        }
        equipPriv(goodItemId, true, notify);
    }

    /**
     * UnEquips the given <code>EquippableVG</code>.
     *
     * @param goodItemId the <code>EquippableVG</code> to unequip
     */
    public void unequip(String goodItemId) {
        unequip(goodItemId, true);
    }

    /**
     * UnEquips the given <code>EquippableVG</code>.
     *
     * @param goodItemId the <code>EquippableVG</code> to unequip
     * @param notify if true post event to bus
     */
    public void unequip(String goodItemId, boolean notify) {
        if (!isEquipped(goodItemId)) {
            return;
        }
        equipPriv(goodItemId, false, notify);
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected String keyBalance(String itemId) {
        return keyGoodBalance(itemId);
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected void postBalanceChangeEvent(String itemId, int balance, int amountAdded) {
        BusProvider.getInstance().post(new GoodBalanceChangedEvent(itemId,
                balance, amountAdded));
    }

    /**
     * Helper function for <code>equip</code> and <code>unequip</code> functions.
     */
    private void equipPriv(String goodItemId, boolean equip, boolean notify){
        SoomlaUtils.LogDebug(mTag, (!equip ? "unequipping " : "equipping ") + goodItemId + ".");

        String key = keyGoodEquipped(goodItemId);

        if (equip) {
            KeyValueStorage.setValue(key, "");
            if (notify) {
                BusProvider.getInstance().post(new GoodEquippedEvent(goodItemId));
            }
        } else {
            KeyValueStorage.deleteKeyValue(key);
            if (notify) {
                BusProvider.getInstance().post(new GoodUnEquippedEvent(goodItemId));
            }
        }
    }


    private static String keyGoodBalance(String itemId) {
        return DB_KEY_GOOD_PREFIX + itemId + ".balance";
    }

    private static String keyGoodEquipped(String itemId) {
        return DB_KEY_GOOD_PREFIX + itemId + ".equipped";
    }

    private static String keyGoodUpgrade(String itemId) {
        return DB_KEY_GOOD_PREFIX + itemId + ".currentUpgrade";
    }

    public final static String DB_KEY_GOOD_PREFIX = "good.";
}
