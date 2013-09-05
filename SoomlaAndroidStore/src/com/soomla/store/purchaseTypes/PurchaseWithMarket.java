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

package com.soomla.store.purchaseTypes;

import android.content.Context;
import android.content.SharedPreferences;
import com.soomla.store.*;
import com.soomla.store.data.ObscuredSharedPreferences;
import com.soomla.store.domain.GoogleMarketItem;
import com.soomla.store.events.ItemPurchaseStartedEvent;
import com.soomla.store.events.UnexpectedStoreErrorEvent;
import com.soomla.store.exceptions.InsufficientFundsException;

/**
 * This type of Purchase is used to let users purchase PurchasableVirtualItems with Google Play (with real $$).
 */
public class PurchaseWithMarket extends PurchaseType {

    /** Constructor
     *
     * @param productId is the productId to purchase in Google Play.
     * @param price is the price in Google Play.
     */
    public PurchaseWithMarket(String productId, double price) {
        mGoogleMarketItem = new GoogleMarketItem(productId, GoogleMarketItem.Managed.UNMANAGED, price);
    }

    /** Constructor
     *
     * @param googleMarketItem is the representation of the item in Google Play.
     */
    public PurchaseWithMarket(GoogleMarketItem googleMarketItem) {
        mGoogleMarketItem = googleMarketItem;
    }

    /**
     * see parent
     */
    @Override
    public void buy() throws InsufficientFundsException {
        SharedPreferences prefs = new ObscuredSharedPreferences(
                SoomlaApp.getAppContext().getSharedPreferences(StoreConfig.PREFS_NAME, Context.MODE_PRIVATE));
        String publicKey = prefs.getString(StoreConfig.PUBLIC_KEY, "");
        if (publicKey.length() == 0 || publicKey.equals("[YOUR PUBLIC KEY FROM GOOGLE PLAY]")) {
            StoreUtils.LogError(TAG, "You didn't provide a public key! You can't make purchases.");
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
            return;
        }

        StoreUtils.LogDebug(TAG, "Starting in-app purchase for productId: " + mGoogleMarketItem.getProductId());

        if (!StoreController.getInstance().buyWithGooglePlay(mGoogleMarketItem, "")) {
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
            return;
        }

        BusProvider.getInstance().post(new ItemPurchaseStartedEvent(getAssociatedItem()));

        // This is where the job of PurchaseWithMarket ends. The success/fail purchase event will be handled
        // by StoreController
    }

    public GoogleMarketItem getGoogleMarketItem() {
        return mGoogleMarketItem;
    }

    private static final String TAG = "SOOMLA PurchaseWithMarket";

    private GoogleMarketItem mGoogleMarketItem;
}
