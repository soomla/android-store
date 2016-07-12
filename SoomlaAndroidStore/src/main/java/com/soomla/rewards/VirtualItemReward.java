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

package com.soomla.rewards;

import com.soomla.SoomlaUtils;
import com.soomla.store.StoreInventory;
import com.soomla.store.data.StoreJSONConsts;
import com.soomla.store.exceptions.VirtualItemNotFoundException;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A specific type of <code>Reward</code> that has an associated
 * virtual item.  The user is given this virtual item when the reward
 * is granted.  For example: a user can earn a life reward (<code>VirtualItemReward</code>)
 * which rewards the user with one life (<code>SingleUseVG</code>).
 *
 * Created by refaelos on 13/05/14.
 */
public class VirtualItemReward extends Reward {

    /**
     * Constructor
     *
     * @param rewardId see parent
     * @param name see parent
     * @param amount the amount to give of the associated item when the reward is given
     * @param associatedItemId the ID of the virtual item associated with this reward
     */
    public VirtualItemReward(String rewardId, String name, int amount, String associatedItemId) {
        super(rewardId, name);
        mAmount = amount;
        mAssociatedItemId = associatedItemId;
    }

    /**
     * Constructor.
     * Generates an instance of <code>VirtualItemReward</code> from the given <code>JSONObject</code>.
     *
     * @param jsonObject A JSONObject representation of the wanted <code>VirtualItemReward</code>.
     * @throws JSONException
     */
    public VirtualItemReward(JSONObject jsonObject) throws JSONException {
        super(jsonObject);
        mAssociatedItemId = jsonObject.getString(StoreJSONConsts.VIR_ASSOCITEMID);
        mAmount = jsonObject.getInt(StoreJSONConsts.VIR_AMOUNT);
    }

    /**
     * Converts the current <code>VirtualItemReward</code> to a JSONObject.
     *
     * @return A <code>JSONObject</code> representation of the current <code>VirtualItemReward</code>.
     */
    public JSONObject toJSONObject(){
        JSONObject jsonObject = super.toJSONObject();
        try {
            jsonObject.put(StoreJSONConsts.VIR_ASSOCITEMID, mAssociatedItemId);
            jsonObject.put(StoreJSONConsts.VIR_AMOUNT, mAmount);
        } catch (JSONException e) {
            SoomlaUtils.LogError(TAG, "An error occurred while generating JSON object.");
        }

        return jsonObject;
    }

    /**
     * Gives the user the item associated with this reward.
     *
     * @return <code>true</code> if the item was given successfully,
     * <code>false</code> otherwise
     */
    @Override
    public boolean giveInner() {
        try {
            StoreInventory.giveVirtualItem(mAssociatedItemId, mAmount);
        } catch (VirtualItemNotFoundException e) {
            SoomlaUtils.LogError(TAG, "(give) Couldn't find associated itemId: " + mAssociatedItemId);
            return false;
        }
        return true;
    }

    @Override
    protected boolean takeInner() {
        try {
            StoreInventory.takeVirtualItem(mAssociatedItemId, mAmount);
        } catch (VirtualItemNotFoundException e) {
            SoomlaUtils.LogError(TAG, "(take) Couldn't find associated itemId: " + mAssociatedItemId);
            return false;
        }

        return true;
    }

    /** Setters and Getters **/

    public int getAmount() {
        return mAmount;
    }

    public String getAssociatedItemId() {
        return mAssociatedItemId;
    }


    /** Private Members **/

    private static final String TAG = "SOOMLA VirtualItemReward";

    private int mAmount;
    private String mAssociatedItemId;
}