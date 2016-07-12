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

package com.soomla.store.domain.virtualGoods;

import com.soomla.store.data.StorageManager;
import com.soomla.store.domain.PurchasableVirtualItem;
import com.soomla.store.domain.VirtualItem;
import com.soomla.store.purchaseTypes.PurchaseType;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This is an abstract representation of the application's virtual goods.
 * Your game's virtual economy revolves around virtual goods. This class defines the abstract
 * and most common virtual goods while the descendants of this class define specific definitions
 * of virtual goods.
 *
 * Inheritance: VirtualGood >
 * {@link PurchasableVirtualItem} >
 * {@link VirtualItem}
 */
public abstract class VirtualGood extends PurchasableVirtualItem {

    /**
     * Constructor
     *
     * @param mName see parent
     * @param mDescription see parent
     * @param mItemId see parent
     * @param purchaseType see parent
     */
    public VirtualGood(String mName, String mDescription,
                       String mItemId, PurchaseType purchaseType) {
        super(mName, mDescription, mItemId, purchaseType);
    }

    /**
     * Constructor
     *
     * @param jsonObject see parent
     * @throws JSONException
     */
    public VirtualGood(JSONObject jsonObject) throws JSONException{
        super(jsonObject);
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public JSONObject toJSONObject(){
        return super.toJSONObject();
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public int resetBalance(int balance, boolean notify) {
        return StorageManager.getVirtualGoodsStorage().setBalance(this.getItemId(), balance, notify);
    }

    /** Private Members **/

    private static final String TAG = "SOOMLA VirtualGood"; //used for Log messages

}
