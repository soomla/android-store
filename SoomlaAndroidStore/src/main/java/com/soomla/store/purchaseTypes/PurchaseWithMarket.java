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
import com.soomla.store.SoomlaStore;
import com.soomla.store.domain.MarketItem;
import com.soomla.store.events.ItemPurchaseStartedEvent;
import com.soomla.store.exceptions.InsufficientFundsException;

/**
 * This type of Purchase is used to let users purchase <code>PurchasableVirtualItem</code>s in the
 * market with real money $$$.
 *
 * Real Game Example: Purchase a Sword for $1.99.
 */
public class PurchaseWithMarket extends PurchaseType {

    /**
     * Constructor.
     * Constructs a PurchaseWithMarket object by constructing a new <code>MarketItem</code> object
     * with the given <code>productId</code> and price, and declaring it as UNMANAGED.
     *
     * @param productId the productId as it appears in the Market.
     * @param price the price in the Market.
     * @param isSubscription let PurchaseWithMarket know is current purchase is subscription.
     */
    public PurchaseWithMarket(String productId, double price, boolean isSubscription) {
        mMarketItem = new MarketItem(productId, price);
        mIsSubscription = isSubscription;
    }

    /**
     * Constructor.
     * Constructs a PurchaseWithMarket object by constructing a new <code>MarketItem</code> object
     * with the given <code>productId</code> and price, and declaring it as UNMANAGED.
     *
     * @param productId the productId as it appears in the Market.
     * @param price the price in the Market.
     */
    public PurchaseWithMarket(String productId, double price) {
        this(productId, price, false);
    }

    /**
     * Constructor.
     *
     * @param marketItem the representation of the item in the market
     * @param isSubscription let PurchaseWithMarket know is current purchase is subscription.
     */
    public PurchaseWithMarket(MarketItem marketItem, boolean isSubscription) {
        mMarketItem = marketItem;
        mIsSubscription = isSubscription;
    }

    /**
     * Constructor.
     *
     * @param marketItem the representation of the item in the market
     */
    public PurchaseWithMarket(MarketItem marketItem) {
        this(marketItem, false);
    }

    /**
     * Buys the virtual item with real money (from the Market).
     *
     * @throws InsufficientFundsException
     */
    @Override
    public void buy(String payload) throws InsufficientFundsException {
        SoomlaUtils.LogDebug(TAG, "Starting in-app purchase for productId: "
                + mMarketItem.getProductId());
        
        BusProvider.getInstance().post(new ItemPurchaseStartedEvent(getAssociatedItem().getItemId()));
        try {
            SoomlaStore.getInstance().buyWithMarket(mMarketItem, mIsSubscription, payload);
        } catch (IllegalStateException e) {
            SoomlaUtils.LogError(TAG, "Error when purchasing item");
        }
    }


    /** Setters and Getters */

    public MarketItem getMarketItem() {
        return mMarketItem;
    }

    public boolean isSubscription() {
        return mIsSubscription;
    }


    /** Private Members */

    private static final String TAG = "SOOMLA PurchaseWithMarket"; //used for Log messages

    private MarketItem mMarketItem; //the representation of the item in the market
    private boolean mIsSubscription; //allows SoomlaStore know if this purchase is subscription
}
