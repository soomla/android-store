/*
 * Copyright (C) 2012 Soomla Inc.
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

package com.soomla.store.domain.virtualGoods;

import com.soomla.store.StoreUtils;
import com.soomla.store.data.StorageManager;
import com.soomla.store.purchaseTypes.PurchaseType;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * A LifetimeVG is a virtual good that is bought only once and kept forever.
 *
 * The LifetimeVG's characteristics are:
 *  1. Can only be purchased once.
 *  2. Your users cannot have more than one of this item.
 *
 * Real Games Examples: 'No Ads', 'Double Coins'
 *
 * This VirtualItem is purchasable.
 * In case you want this item to be available for purchase in the market (PurchaseWithMarket),
 * you will need to define the item in the market (Google Play, Amazon App Store, etc...).
 *
 * LifeTimeVG > VirtualGood > PurchasableVirtualItem > VirtualItem
 */
    public class LifetimeVG extends VirtualGood{

    /** Constructor
     *
     * @param mName see parent
     * @param mDescription see parent
     * @param mItemId see parent
     * @param purchaseType see parent
     */
    public LifetimeVG(String mName, String mDescription,
                      String mItemId,
                      PurchaseType purchaseType) {
        super(mName, mDescription, mItemId, purchaseType);
    }

    /**
     * Constructor
     *
     * @param jsonObject see parent
     * @throws JSONException
     */
    public LifetimeVG(JSONObject jsonObject) throws JSONException {
        super(jsonObject);
    }

    /**
     * see parent
     *
     * @return see parent
     */
    @Override
    public JSONObject toJSONObject() {
        return super.toJSONObject();
    }

    /**
     * Gives your user exactly one LifetimeVG
     *
     * @param amount the amount of the specific item to be given - if this input is greater than 1,
     *               we force the amount to equal 1, because a LifetimeVG can only be given once.
     * @return 1 to indicate that the user was given the good
     */
    @Override
    public int give(int amount, boolean notify) {
        if(amount > 1) {
            StoreUtils.LogDebug(TAG, "You tried to give more than one LifetimeVG."
                    + "Will try to give one anyway.");
            amount = 1;
        }

        int balance = StorageManager.getVirtualGoodsStorage().getBalance(this);

        if (balance < 1) {
            return StorageManager.getVirtualGoodsStorage().add(this, amount, notify);
        }
        return 1;
    }

    /**
     * Takes from your user exactly one LifetimeVG
     *
     * @param amount the amount of the specific item to be taken - if this input is greater than 1,
     *               we force amount to equal 1, because a LifetimeVG can only be given once and
     *               therefore, taken once.
     * @return 1 to indicate that the user was given the good
     */
    @Override
    public int take(int amount, boolean notify) {
        if (amount > 1) {
            amount = 1;
        }

        int balance = StorageManager.getVirtualGoodsStorage().getBalance(this);

        if (balance > 0) {
            return StorageManager.getVirtualGoodsStorage().remove(this, amount, notify);
        }
        return 0;
    }

    /**
     * Determines if the user is in a state that allows him to buy a LifetimeVG, by checking his
     * balance of LifetimeVGs.
     * From the definition of a LifetimeVG:
     * If the user has a balance of 0 - he can buy
     * If the user has a balance of 1 or more - he cannot buy more.
     *
     * @return true if he can buy, false otherwise
     */
    @Override
    protected boolean canBuy() {
        int balance = StorageManager.getVirtualGoodsStorage().getBalance(this);

        return balance < 1;
    }


    /** Private Members **/

    private static String TAG = "SOOMLA LifetimeVG"; //used for Log messages
}
