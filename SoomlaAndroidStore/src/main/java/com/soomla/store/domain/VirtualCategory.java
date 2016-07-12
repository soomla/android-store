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
import com.soomla.data.JSONConsts;
import com.soomla.store.data.StoreJSONConsts;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * This class is a definition of a category. A single category can be associated with many virtual
 * goods. You can use it to arrange virtual goods to their specific categories.
 */
public class VirtualCategory {

    /**
     * Constructor.
     *
     * @param name the category's name
     * @param goodsItemIds list of item ids of the virtual goods in this category
     */
    public VirtualCategory(String name, ArrayList<String> goodsItemIds) {
        mName = name;
        mGoodsItemIds = goodsItemIds;
    }

    /**
     * Constructor.
     * Generates an instance of <code>VirtualCategory</code> from the given <code>JSONObject</code>.
     *
     * @param jsonObject A JSONObject representation of the wanted <code>VirtualCategory</code>.
     * @throws JSONException
     */
    public VirtualCategory(JSONObject jsonObject) throws JSONException{
        mName = jsonObject.getString(StoreJSONConsts.CATEGORY_NAME);

        JSONArray goodsArr = jsonObject.getJSONArray(StoreJSONConsts.CATEGORY_GOODSITEMIDS);
        for(int i=0; i<goodsArr.length(); i++) {
            String goodItemId = goodsArr.getString(i);
            mGoodsItemIds.add(goodItemId);
        }
    }

    /**
     * Converts the current <code>VirtualCategory</code> to a <code>JSONObject</code>.
     *
     * @return A JSONObject representation of the current <code>VirtualCategory</code>.
     */
    public JSONObject toJSONObject(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(JSONConsts.SOOM_CLASSNAME, SoomlaUtils.getClassName(this));
            jsonObject.put(StoreJSONConsts.CATEGORY_NAME, mName);

            JSONArray goodsArr = new JSONArray();
            for(String goodItemId : mGoodsItemIds) {
                goodsArr.put(goodItemId);
            }

            jsonObject.put(StoreJSONConsts.CATEGORY_GOODSITEMIDS, goodsArr);
        } catch (JSONException e) {
            SoomlaUtils.LogError(TAG, "An error occurred while generating JSON object.");
        }

        return jsonObject;
    }


    /** Setters and Getters **/

    public String getName() {
        return mName;
    }

    public ArrayList<String> getGoodsItemIds() {
        return mGoodsItemIds;
    }


    /** Private members **/

    private static final String TAG = "SOOMLA VirtualCategory"; //used for Log messages

    //the list of itemIds of the VirtualGoods in this category
    private ArrayList<String> mGoodsItemIds = new ArrayList<String>();

    private String  mName; //the category's name

}
