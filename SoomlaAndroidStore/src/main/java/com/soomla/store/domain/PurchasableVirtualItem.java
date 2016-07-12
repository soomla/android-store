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

package com.soomla.store.domain;

import com.soomla.SoomlaUtils;
import com.soomla.store.data.StoreJSONConsts;
import com.soomla.store.exceptions.InsufficientFundsException;
import com.soomla.store.purchaseTypes.PurchaseType;
import com.soomla.store.purchaseTypes.PurchaseWithMarket;
import com.soomla.store.purchaseTypes.PurchaseWithVirtualItem;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * A representation of a <code>VirtualItem</code> that you can actually purchase.
 */
public abstract class PurchasableVirtualItem extends VirtualItem {

    /**
     * Constructor.
     *
     * @param mName see parent
     * @param mDescription see parent
     * @param mItemId see parent
     * @param purchaseType the way this item is purchased
     */
    public PurchasableVirtualItem(String mName, String mDescription, String mItemId,
                                  PurchaseType purchaseType) {
        super(mName, mDescription, mItemId);

        mPurchaseType = purchaseType;

        mPurchaseType.setAssociatedItem(this);
    }

    /**
     * Constructor.
     *
     * @param jsonObject see parent
     * @throws JSONException
     */
    public PurchasableVirtualItem(JSONObject jsonObject) throws JSONException {
        super(jsonObject);

        JSONObject purchasableObj = jsonObject.getJSONObject(StoreJSONConsts.PURCHASABLE_ITEM);
        String purchaseType = purchasableObj.getString(StoreJSONConsts.PURCHASE_TYPE);

        if (purchaseType.equals(StoreJSONConsts.PURCHASE_TYPE_MARKET)) {
            JSONObject marketItemObj =
                    purchasableObj.getJSONObject(StoreJSONConsts.PURCHASE_MARKET_ITEM);

            mPurchaseType = new PurchaseWithMarket(new MarketItem(marketItemObj));
        } else if (purchaseType.equals(StoreJSONConsts.PURCHASE_TYPE_VI)) {
            String itemId = purchasableObj.getString(StoreJSONConsts.PURCHASE_VI_ITEMID);
            int amount = purchasableObj.getInt(StoreJSONConsts.PURCHASE_VI_AMOUNT);

            mPurchaseType = new PurchaseWithVirtualItem(itemId, amount);
        } else {
            SoomlaUtils.LogError(TAG, "IabPurchase type not recognized !");
        }

        if (mPurchaseType != null) {
            mPurchaseType.setAssociatedItem(this);
        }
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public JSONObject toJSONObject(){
        JSONObject parentJsonObject = super.toJSONObject();
        JSONObject jsonObject = new JSONObject();
        try {
            Iterator<?> keys = parentJsonObject.keys();
            while(keys.hasNext())
            {
                String key = (String)keys.next();
                jsonObject.put(key, parentJsonObject.get(key));
            }

            JSONObject purchasableObj = new JSONObject();

            if(mPurchaseType instanceof PurchaseWithMarket) {
                purchasableObj.put(StoreJSONConsts.PURCHASE_TYPE, StoreJSONConsts.PURCHASE_TYPE_MARKET);

                MarketItem mi = ((PurchaseWithMarket) mPurchaseType).getMarketItem();
                purchasableObj.put(StoreJSONConsts.PURCHASE_MARKET_ITEM, mi.toJSONObject());
            } else if(mPurchaseType instanceof PurchaseWithVirtualItem) {
                purchasableObj.put(StoreJSONConsts.PURCHASE_TYPE, StoreJSONConsts.PURCHASE_TYPE_VI);

                purchasableObj.put(StoreJSONConsts.PURCHASE_VI_ITEMID,
                        ((PurchaseWithVirtualItem) mPurchaseType).getTargetItemId());
                purchasableObj.put(StoreJSONConsts.PURCHASE_VI_AMOUNT,
                        ((PurchaseWithVirtualItem) mPurchaseType).getAmount());
            }

            jsonObject.put(StoreJSONConsts.PURCHASABLE_ITEM, purchasableObj);
        } catch (JSONException e) {
            SoomlaUtils.LogError(TAG, "An error occurred while generating JSON object.");
        }

        return jsonObject;
    }

    /**
     * Buys the <code>PurchasableVirtualItem</code>, after checking if the user is in a state that
     * allows him/her to buy. This action uses the associated <code>PurchaseType</code> to perform
     * the purchase.
     *
     * @param payload a string you want to be assigned to the purchase. This string
     *   is saved in a static variable and will be given bacl to you when the
     *   purchase is completed.
     * @throws InsufficientFundsException if the user does not have enough funds for buying.
     */
    public void buy(String payload) throws InsufficientFundsException {
        if (!canBuy()) return;

        mPurchaseType.buy(payload);
    }

    /**
     * Determines if user is in a state that allows him/her to buy a specific
     * <code>VirtualItem</code>.
     *
     * @return true if can buy, false otherwise
     */
    protected abstract boolean canBuy();


    /** Setters and Getters */

    public PurchaseType getPurchaseType() {
        return mPurchaseType;
    }


    /** Private Members */

    private static final String TAG = "SOOMLA PurchasableVirtualItem"; //used for Log messages

    private PurchaseType mPurchaseType; //the way this PurchasableVirtualItem is purchased.
}
