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

package com.soomla.store.domain.virtualCurrencies;

import com.soomla.store.data.StorageManager;
import com.soomla.store.data.StoreInfo;
import com.soomla.store.domain.VirtualItem;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This is a representation of a game's virtual currency.
 * Each game can have multiple instances of a virtual currency, all kept in
 * {@link StoreInfo}
 *
 * Real Game Examples: 'Coin', 'Gem', 'Muffin'
 *
 * NOTE: This item is NOT purchasable!
 * However, a <code>VirtualCurrencyPack</code> IS purchasable.
 * For example, if the virtual currency in your game is a 'Coin' and you want to make a single
 * 'Coin' available for purchase you will need to define a <code>VirtualCurrencyPack</code> of 1
 * 'Coin'.
 */
public class VirtualCurrency extends VirtualItem {

    /**
     * Constructor
     * see parent
     *
     * @param mName the name of the virtual item
     * @param mDescription the description of the virtual item
     * @param itemId the itemId of the virtual item
     */
    public VirtualCurrency(String mName, String mDescription, String itemId) {
        super(mName, mDescription, itemId);
    }

    /**
     * Constructor
     *
     * @param jsonObject a JSONObject representation of the wanted VirtualItem
     * @throws JSONException
     */
    public VirtualCurrency(JSONObject jsonObject) throws JSONException {
        super(jsonObject);
    }

    /**
     * see parent
     *
     * @return see parent
     */
    @Override
    public JSONObject toJSONObject(){
        return super.toJSONObject();
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public int give(int amount, boolean notify) {
        return StorageManager.getVirtualCurrencyStorage().add(this.getItemId(), amount, notify);
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public int take(int amount, boolean notify) {
        return StorageManager.getVirtualCurrencyStorage().remove(this.getItemId(), amount, notify);
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public int resetBalance(int balance, boolean notify) {
        return StorageManager.getVirtualCurrencyStorage().setBalance(this.getItemId(), balance, notify);
    }
}
