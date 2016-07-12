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

import com.soomla.SoomlaEntity;
import com.soomla.store.data.StoreInfo;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This is the parent class of all virtual items in the application.
 * Almost every entity in your virtual economy will be a virtual item. There are many types
 * of virtual items, each one will extend this class. Each one of the various types extends
 * <code>VirtualItem</code> and adds its own behavior on top of it.
 */
public abstract class VirtualItem extends SoomlaEntity<VirtualItem> {

    /**
     * Constructor.
     *
     * @param mName the name of the virtual item
     * @param mDescription the description of the virtual item
     * @param mItemId the itemId of the virtual item
     */
    public VirtualItem(String mName, String mDescription, String mItemId) {
        super(mName, mDescription, mItemId);
    }

    /**
     * Constructor.
     * Generates an instance of <code>VirtualItem</code> from the given <code>JSONObject</code>.
     *
     * @param jsonObject A JSONObject representation of the wanted <code>VirtualItem</code>.
     * @throws JSONException
     */
    public VirtualItem(JSONObject jsonObject) throws JSONException{
        super(jsonObject);
    }

    /**
     * Converts the current <code>VirtualItem</code> to a JSONObject.
     *
     * @return A <code>JSONObject</code> representation of the current <code>VirtualItem</code>.
     */
    public JSONObject toJSONObject(){
        return super.toJSONObject();
    }

    /**
     * Gives your user the given amount of the specific virtual item.
     * For example, when your users play your game for the first time you GIVE them 1000 gems.
     *
     * NOTE: This action is different than <code>PurchasableVirtualItem</code>'s <code>buy()</code>:
     * You use <code>give(int amount)</code> to give your user something for free.
     * You use <code>buy()</code> to give your user something and get something in return.
     *
     * @param amount the amount of the specific item to be given
     * @return balance after the giving process
     */
    public int give(int amount) {
        return give(amount, true);
    }

    /**
     * Works like {@link #give(int)} but receives an argument, notify, to indicate
     * if there has been a change in the balance of the current virtual item.
     *
     * @param amount the amount of the specific item to be given
     * @param notify notify of change in user's balance of current virtual item
     * @return balance after the giving process
     */
    public abstract int give(int amount, boolean notify);

    /**
     * Takes from your user the given amount of the specific virtual item.
     * For example, when your user requests a refund, you need to TAKE the item he/she is returning.
     *
     * @param amount the amount of the specific item to be taken
     * @return balance after the taking process
     */
    public int take(int amount) {
        return take(amount, true);
    }

    /**
     * Works like {@link #take(int)} but receives an argument, notify, to indicate
     * if there has been a change in the balance of the current virtual item.
     *
     * @param amount the amount of the specific item to be taken
     * @param notify notify of change in user's balance of current virtual item
     * @return balance after the taking process
     */
    public abstract int take(int amount, boolean notify);

    /**
     * Resets this <code>VirtualItem</code>'s balance to the given balance.
     *
     * @param balance the balance of the current virtual item
     * @return balance after the reset process
     */
    public int resetBalance(int balance) {
        return resetBalance(balance, true);
    }

    /**
     * Works like {@link #resetBalance(int)} but receives an argument, notify, to indicate
     * if there has been a change in the balance of the current virtual item.
     *
     * @param balance the balance of the current virtual item
     * @param notify notify of change in user's balance of current virtual item
     * @return balance after the reset process
     */
    public abstract int resetBalance(int balance, boolean notify);

    /**
     * Saves the item updates to <code>StoreInfo</code> and to internal DB.
     */
    public void save() { save(true); }

    /**
     * Saves the item updates to <code>StoreInfo</code> and to internal DB
     * (if requested).
     *
     * @param saveToDB Should the item be saved to the internal DB
     */
    public void save(boolean saveToDB) {
        StoreInfo.save(this, saveToDB);
    }

    public String getItemId(){
        return mID;
    }


    /** Private Members **/

    private static final String TAG = "SOOMLA VirtualItem"; //used for Log messages
}
