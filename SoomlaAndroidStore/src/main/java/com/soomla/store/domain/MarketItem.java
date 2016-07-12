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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A representation of an item in the market.
 * <code>MarketItem</code> is only used for <code>PurchaseWithMarket</code> purchase type.
 */
public class MarketItem {

    /**
     * Constructor.
     *
     * @param productId the id of the current item in the market
     * @param price the actual $$ cost of the current item in the market
     */
    public MarketItem(String productId, double price) {
        this.mProductId = productId;
        setPrice(price);
    }

    /**
     * Constructor.
     * Generates an instance of <code>MarketItem</code> from a <code>JSONObject</code>.
     *
     * @param jsonObject a <code>JSONObject</code> representation of the wanted
     *                   <code>MarketItem</code>.
     * @throws JSONException
     */
    public MarketItem(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has(StoreJSONConsts.MARKETITEM_ANDROID_ID)) {
            this.mProductId = jsonObject.getString(StoreJSONConsts.MARKETITEM_ANDROID_ID);
        } else {
            this.mProductId = jsonObject.getString(StoreJSONConsts.MARKETITEM_PRODUCT_ID);
        }
		setPrice(jsonObject.getDouble(StoreJSONConsts.MARKETITEM_PRICE));

        this.mMarketPriceAndCurrency = jsonObject.optString(StoreJSONConsts.MARKETITEM_MARKETPRICE);
        this.mMarketTitle = jsonObject.optString(StoreJSONConsts.MARKETITEM_MARKETTITLE);
        this.mMarketDescription = jsonObject.optString(StoreJSONConsts.MARKETITEM_MARKETDESC);
        this.mMarketCurrencyCode = jsonObject.optString(StoreJSONConsts.MARKETITEM_MARKETCURRENCYCODE);
        this.mMarketPriceMicros = jsonObject.optLong(StoreJSONConsts.MARKETITEM_MARKETPRICEMICROS);
    }

    /**
     * Converts the current <code>MarketItem</code> to a <code>JSONObject</code>.
     *
     * @return A <code>JSONObject</code> representation of the current <code>MarketItem</code>.
     */
    public JSONObject toJSONObject(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(JSONConsts.SOOM_CLASSNAME, SoomlaUtils.getClassName(this));

            jsonObject.put(StoreJSONConsts.MARKETITEM_ANDROID_ID, mProductId);
            jsonObject.put(StoreJSONConsts.MARKETITEM_PRICE, Double.valueOf(getPrice()));

            jsonObject.put(StoreJSONConsts.MARKETITEM_MARKETPRICE, mMarketPriceAndCurrency);
            jsonObject.put(StoreJSONConsts.MARKETITEM_MARKETTITLE, mMarketTitle);
            jsonObject.put(StoreJSONConsts.MARKETITEM_MARKETDESC, mMarketDescription);
            jsonObject.put(StoreJSONConsts.MARKETITEM_MARKETCURRENCYCODE, mMarketCurrencyCode);
            jsonObject.put(StoreJSONConsts.MARKETITEM_MARKETPRICEMICROS, Long.valueOf(mMarketPriceMicros));
        } catch (JSONException e) {
            SoomlaUtils.LogError(TAG, "An error occurred while generating JSON object.");
        }

        return jsonObject;
    }

    /** Setters and Getters **/

    public void setMarketInformation(String marketPriceAndCurrency, String marketTitle, String marketDescription, String marketCurrencyCode, long marketPriceMicros) {
        this.mMarketPriceAndCurrency = marketPriceAndCurrency;
        this.mMarketTitle = marketTitle;
        this.mMarketDescription = marketDescription;
        this.mMarketCurrencyCode = marketCurrencyCode;
        this.mMarketPriceMicros = marketPriceMicros;
    }

    public String getProductId() {
        return mProductId;
    }

    public double getPrice() {
        return mPrice;
    }

    public void setPrice(double price) {
	mPrice = Math.round(price * 100) / 100.0;
    }


    /** Realtime Infomation from the Google Play Store **/

    @Deprecated
    public String getMarketPrice() {
        return getMarketPriceAndCurrency();
    }

    public String getMarketPriceAndCurrency() { return mMarketPriceAndCurrency; }

    public String getMarketTitle() {
        return mMarketTitle;
    }

    public String getMarketDescription() {
        return mMarketDescription;
    }

    public String getMarketCurrencyCode() {
        return mMarketCurrencyCode;
    }

    public long getMarketPriceMicros() {
        return mMarketPriceMicros;
    }


    /** Private Members **/

    private static final String TAG = "SOOMLA MarketItem"; //used for Log messages

    private String mProductId; //id of this VirtualGood in the market

    private double mPrice; //the actual $$ cost of the current item in the market.

    private String mMarketPriceAndCurrency;

    private String mMarketTitle;

    private String mMarketDescription;

    private String mMarketCurrencyCode;

    private long mMarketPriceMicros;
}
