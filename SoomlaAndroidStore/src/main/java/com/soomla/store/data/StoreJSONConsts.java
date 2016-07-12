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

package com.soomla.store.data;

/**
 * This class contains all static final String names of the keys/vals in the JSON being parsed all
 * around the sdk.
 */
public class StoreJSONConsts {

    public static final String STORE_CURRENCIES         = "currencies";
    public static final String STORE_CURRENCYPACKS      = "currencyPacks";
    public static final String STORE_GOODS              = "goods";
    public static final String STORE_CATEGORIES         = "categories";
    public static final String STORE_GOODS_SU           = "singleUse";
    public static final String STORE_GOODS_PA           = "goodPacks";
    public static final String STORE_GOODS_UP           = "goodUpgrades";
    public static final String STORE_GOODS_LT           = "lifetime";
    public static final String STORE_GOODS_EQ           = "equippable";

    public static final String CATEGORY_NAME            = "name";
    public static final String CATEGORY_GOODSITEMIDS    = "goods_itemIds";

    public static final String MARKETITEM_PRODUCT_ID    = "productId";
    public static final String MARKETITEM_ANDROID_ID    = "androidId";
    public static final String MARKETITEM_PRICE         = "price";
    public static final String MARKETITEM_MARKETPRICE   = "marketPrice";
    public static final String MARKETITEM_MARKETTITLE   = "marketTitle";
    public static final String MARKETITEM_MARKETDESC    = "marketDesc";
    public static final String MARKETITEM_MARKETCURRENCYCODE   = "marketCurrencyCode";
    public static final String MARKETITEM_MARKETPRICEMICROS    = "marketPriceMicros";

    public static final String EQUIPPABLE_EQUIPPING     = "equipping";

    // VGP = SingleUsePackVG
    public static final String VGP_GOOD_ITEMID          = "good_itemId";
    public static final String VGP_GOOD_AMOUNT          = "good_amount";

    // VGU = UpgradeVG
    public static final String VGU_GOOD_ITEMID          = "good_itemId";
    public static final String VGU_PREV_ITEMID          = "prev_itemId";
    public static final String VGU_NEXT_ITEMID          = "next_itemId";

    public static final String CURRENCYPACK_CURRENCYAMOUNT = "currency_amount";
    public static final String CURRENCYPACK_CURRENCYITEMID = "currency_itemId";

    /** IabPurchase Type **/
    public static final String PURCHASABLE_ITEM         = "purchasableItem";

    public static final String PURCHASE_TYPE            = "purchaseType";
    public static final String PURCHASE_TYPE_MARKET     = "market";
    public static final String PURCHASE_TYPE_VI         = "virtualItem";

    public static final String PURCHASE_MARKET_ITEM     = "marketItem";

    public static final String PURCHASE_VI_ITEMID       = "pvi_itemId";
    public static final String PURCHASE_VI_AMOUNT       = "pvi_amount";

    // VIR = VirtualItemReward
    public static final String VIR_ASSOCITEMID          = "associatedItemId";
    public static final String VIR_AMOUNT               = "amount";
}
