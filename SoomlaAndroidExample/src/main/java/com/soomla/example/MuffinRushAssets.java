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


package com.soomla.example;

import com.soomla.store.IStoreAssets;
import com.soomla.store.domain.MarketItem;
import com.soomla.store.domain.VirtualCategory;
import com.soomla.store.domain.virtualCurrencies.VirtualCurrency;
import com.soomla.store.domain.virtualCurrencies.VirtualCurrencyPack;
import com.soomla.store.domain.virtualGoods.LifetimeVG;
import com.soomla.store.domain.virtualGoods.SingleUseVG;
import com.soomla.store.domain.virtualGoods.VirtualGood;
import com.soomla.store.purchaseTypes.PurchaseWithMarket;
import com.soomla.store.purchaseTypes.PurchaseWithVirtualItem;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class defines our game's economy, which includes virtual goods, virtual currencies
 * and currency packs, virtual categories, and non-consumable items.
 */
public class MuffinRushAssets implements IStoreAssets {

    /**
     * @{inheritDoc}
     */
    @Override
    public int getVersion() {
        return 0;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public VirtualCurrency[] getCurrencies(){
        return  new VirtualCurrency[] {
                MUFFIN_CURRENCY
        };
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public VirtualGood[] getGoods(){
        return new VirtualGood[] {
                MUFFINCAKE_GOOD, PAVLOVA_GOOD,
                CHOCLATECAKE_GOOD, CREAMCUP_GOOD,
                NO_ADS_GOOD
        };
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public VirtualCurrencyPack[] getCurrencyPacks(){
        return new VirtualCurrencyPack[] {
                TENMUFF_PACK, FIFTYMUFF_PACK, FOURHUNDMUFF_PACK, THOUSANDMUFF_PACK
        };
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public VirtualCategory[] getCategories() {
        return new VirtualCategory[]{
                GENERAL_CATEGORY
        };
    }

    /** Static Final Members **/

    public static final String MUFFIN_CURRENCY_ITEM_ID      = "currency_muffin";

    public static final String MUFFINCAKE_ITEM_ID           = "fruit_cake";

    public static final String PAVLOVA_ITEM_ID              = "pavlova";

    public static final String CHOCLATECAKE_ITEM_ID         = "chocolate_cake";

    public static final String CREAMCUP_ITEM_ID             = "cream_cup";

    public static final String TENMUFF_PACK_PRODUCT_ID      = "android.test.refunded";

    public static final String FIFTYMUFF_PACK_PRODUCT_ID    = "android.test.canceled";

    public static final String FOURHUNDMUFF_PACK_PRODUCT_ID = "android.test.purchased";

    public static final String THOUSANDMUFF_PACK_PRODUCT_ID = "android.test.item_unavailable";

    public static final String NO_ADS_PRODUCT_ID            = "no_ads";


    /** Virtual Currencies **/

    public static final VirtualCurrency MUFFIN_CURRENCY = new VirtualCurrency(
            "Muffins",                                  // name
            "",                                         // description
            MUFFIN_CURRENCY_ITEM_ID                     // item id
    );


    /** Virtual Currency Packs **/

    public static final VirtualCurrencyPack TENMUFF_PACK = new VirtualCurrencyPack(
            "10 Muffins",                               // name
            "Test refund of an item",                   // description
            "muffins_10",                               // item id
            10,                                         // number of currencies in the pack
            MUFFIN_CURRENCY_ITEM_ID,                    // the currency associated with this pack
            new PurchaseWithMarket(TENMUFF_PACK_PRODUCT_ID, 0.99));

    public static final VirtualCurrencyPack FIFTYMUFF_PACK = new VirtualCurrencyPack(
            "50 Muffins",                               // name
            "Test cancellation of an item",             // description
            "muffins_50",                               // item id
            50,                                         // number of currencies in the pack
            MUFFIN_CURRENCY_ITEM_ID,                    // the currency associated with this pack
            new PurchaseWithMarket(FIFTYMUFF_PACK_PRODUCT_ID, 1.99) // purchase type
    );

    public static final VirtualCurrencyPack FOURHUNDMUFF_PACK = new VirtualCurrencyPack(
            "400 Muffins",                              // name
            "Test purchase of an item",                 // description
            "muffins_400",                              // item id
            400,                                        // number of currencies in the pack
            MUFFIN_CURRENCY_ITEM_ID,                    // the currency associated with this pack
            new PurchaseWithMarket(FOURHUNDMUFF_PACK_PRODUCT_ID, 4.99) // purchase type
    );

    public static final VirtualCurrencyPack THOUSANDMUFF_PACK = new VirtualCurrencyPack(
            "1000 Muffins",                             // name
            "Test item unavailable",                    // description
            "muffins_1000",                             // item id
            1000,                                       // number of currencies in the pack
            MUFFIN_CURRENCY_ITEM_ID,                    // the currency associated with this pack
            new PurchaseWithMarket(THOUSANDMUFF_PACK_PRODUCT_ID, 8.99) // purchase type
    );


    /** Virtual Goods **/

    public static final VirtualGood MUFFINCAKE_GOOD = new SingleUseVG(
            "Fruit Cake",                                                   // name
            "Customers buy a double portion on each purchase of this cake", // description
            "fruit_cake",                                                   // item id
            new PurchaseWithVirtualItem(MUFFIN_CURRENCY_ITEM_ID, 225)       // purchase type
    );

    public static final VirtualGood PAVLOVA_GOOD = new SingleUseVG(
            "Pavlova",                                                      // name
            "Gives customers a sugar rush and they call their friends",     // description
            "pavlova",                                                      // item id
            new PurchaseWithVirtualItem(MUFFIN_CURRENCY_ITEM_ID, 175)       // purchase type
    );

    public static final VirtualGood CHOCLATECAKE_GOOD = new SingleUseVG(
            "Chocolate Cake",                                               // name
            "A classic cake to maximize customer satisfaction",             // description
            "chocolate_cake",                                               // item id
            new PurchaseWithVirtualItem(MUFFIN_CURRENCY_ITEM_ID, 250)       // purchase type
    );

    public static final VirtualGood CREAMCUP_GOOD = new SingleUseVG(
            "Cream Cup",                                                    // name
            "Increase bakery reputation with this original pastry",         // description
            "cream_cup",                                                    // item id
            new PurchaseWithVirtualItem(MUFFIN_CURRENCY_ITEM_ID, 50)        // purchase type
    );

    /** LifeTime Virtual Goods **/
    // Note: LifeTimeVG defined with PurchaseWithMarket represents a non-consumable item managed by Google
    public static final VirtualGood NO_ADS_GOOD = new LifetimeVG(
            "No Ads",                                                     // name
            "No More Ads!",                                                // description
            NO_ADS_PRODUCT_ID,                                                      // item id
            new PurchaseWithMarket(new MarketItem(                         // purchase type
                    "no_ads", 1.99))

    );

    /** Virtual Categories **/

    // The Muffin Rush theme doesn't support categories, so we just put everything under a general
    // category.
    public static final VirtualCategory GENERAL_CATEGORY = new VirtualCategory(
            "General", new ArrayList<String>(Arrays.asList(new String[]
            { MUFFINCAKE_ITEM_ID, PAVLOVA_ITEM_ID, CHOCLATECAKE_ITEM_ID, CREAMCUP_ITEM_ID }))
    );
}
