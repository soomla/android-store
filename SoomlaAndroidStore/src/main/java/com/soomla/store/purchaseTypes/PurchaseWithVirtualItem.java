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

package com.soomla.store.purchaseTypes;

import com.soomla.BusProvider;
import com.soomla.SoomlaUtils;
import com.soomla.store.data.StorageManager;
import com.soomla.store.data.StoreInfo;
import com.soomla.store.data.VirtualItemStorage;
import com.soomla.store.domain.VirtualItem;
import com.soomla.store.events.ItemPurchaseStartedEvent;
import com.soomla.store.events.ItemPurchasedEvent;
import com.soomla.store.exceptions.InsufficientFundsException;
import com.soomla.store.exceptions.VirtualItemNotFoundException;

/**
 * This type of purchase allows users to purchase <code>PurchasableVirtualItems</code> with other
 * <code>VirtualItems</code>.
 *
 * Real Game Example: Purchase a Sword in exchange for 100 Gems. 'Sword' is the item to be
 * purchased, 'Gem' is the target item, and 100 is the amount.
 */
public class PurchaseWithVirtualItem extends PurchaseType {

    /**
     * Constructor
     *
     * @param targetItemId the itemId of the <code>VirtualItem</code> that is used to "pay" in
     *                     order to make the purchase.
     * @param amount the number of target items needed in order to make the purchase.
     */
    public PurchaseWithVirtualItem(String targetItemId, int amount) {
        mTargetItemId = targetItemId;
        mAmount = amount;
    }

    /**
     * Buys the virtual item with other virtual items.
     *
     * @throws InsufficientFundsException
     */
    @Override
    public void buy(String payload) throws InsufficientFundsException{

        SoomlaUtils.LogDebug(TAG, "Trying to buy a " + getAssociatedItem().getName() + " with "
                + mAmount + " pieces of " + mTargetItemId);

        VirtualItem item = null;
        try {
            item = StoreInfo.getVirtualItem(mTargetItemId);
        } catch (VirtualItemNotFoundException e) {
            SoomlaUtils.LogError(TAG, "Target virtual item doesn't exist !");
            return;
        }

        BusProvider.getInstance().post(new ItemPurchaseStartedEvent(getAssociatedItem().getItemId()));

        VirtualItemStorage storage = StorageManager.getVirtualItemStorage(item);

        assert storage != null;
        int balance = storage.getBalance(item.getItemId());
        if (balance < mAmount){
            throw new InsufficientFundsException(mTargetItemId);
        }

        storage.remove(item.getItemId(), mAmount);

        getAssociatedItem().give(1);
        BusProvider.getInstance().post(new ItemPurchasedEvent(getAssociatedItem().getItemId(), payload));
    }


    /** Setters and Getters */

    public String getTargetItemId() {
        return mTargetItemId;
    }

    public int getAmount() {
        return mAmount;
    }

    public void setAmount(int mAmount) {
        this.mAmount = mAmount;
    }


    /** Private Members */

    //used for Log messages
    private static final String TAG = "SOOMLA PurchaseWithVirtualItem";

    //the itemId of the VirtualItem that is used to "pay" with in order to make the purchase
    private String mTargetItemId;

    private int mAmount; //the number of items to purchase.
}
