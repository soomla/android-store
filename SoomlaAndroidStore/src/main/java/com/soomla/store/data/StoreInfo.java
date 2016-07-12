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

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.soomla.BusProvider;
import com.soomla.SoomlaApp;
import com.soomla.SoomlaConfig;
import com.soomla.SoomlaUtils;
import com.soomla.data.KeyValueStorage;
import com.soomla.store.IStoreAssets;
import com.soomla.store.StoreConfig;
import com.soomla.store.domain.PurchasableVirtualItem;
import com.soomla.store.domain.VirtualCategory;
import com.soomla.store.domain.VirtualItem;
import com.soomla.store.domain.virtualCurrencies.VirtualCurrency;
import com.soomla.store.domain.virtualCurrencies.VirtualCurrencyPack;
import com.soomla.store.domain.virtualGoods.EquippableVG;
import com.soomla.store.domain.virtualGoods.LifetimeVG;
import com.soomla.store.domain.virtualGoods.SingleUsePackVG;
import com.soomla.store.domain.virtualGoods.SingleUseVG;
import com.soomla.store.domain.virtualGoods.UpgradeVG;
import com.soomla.store.domain.virtualGoods.VirtualGood;
import com.soomla.store.events.UnexpectedStoreErrorEvent;
import com.soomla.store.exceptions.VirtualItemNotFoundException;
import com.soomla.store.purchaseTypes.PurchaseType;
import com.soomla.store.purchaseTypes.PurchaseWithMarket;
import com.soomla.store.purchaseTypes.PurchaseWithVirtualItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.IllegalArgumentException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * This class holds the store's metadata including:
 * virtual currencies,
 * virtual currency packs,
 * all kinds of virtual goods,
 * virtual categories, and
 * non-consumable items
 */
public class StoreInfo {

    /**
     * Initializes <code>StoreInfo</code>.
     * On first initialization, when the database doesn't have any previous version of the store
     * metadata, <code>StoreInfo</code> gets loaded from the given <code>IStoreAssets</code>.
     * After the first initialization, <code>StoreInfo</code> will be initialized from the database.
     * <p/>
     * IMPORTANT: If you want to override the current <code>StoreInfo</code>, you'll have to bump
     * the version of your implementation of <code>IStoreAssets</code> in order to remove the
     * metadata when the application loads. Bumping the version is done by returning a higher number
     * in {@link IStoreAssets#getVersion()}.
     */
    public static void setStoreAssets(IStoreAssets storeAssets) {
        try {
            validateStoreAssets(storeAssets);
            mCurrentAssetsVersion = storeAssets.getVersion();

            //checkAndResetMetadata();

            // we always initialize from the database, unless this is the first time the game is
            // loaded - in that case we initialize with setStoreAssets.
            // BUT we don't need to load from DB if metadata was reset.
            if (!loadFromDB()) {
                initializeWithStoreAssets(storeAssets);
            }
        } catch (IllegalArgumentException invalidStoreAssetsException) {
            SoomlaUtils.LogError(TAG, invalidStoreAssetsException.getMessage());
        }
    }

    /**
     * Another option to set the data of StoreInfo. This will usually be used from game engine
     * wrappers like Unity or Cocos2dx.
     *
     * @param version       the version of IStoreAssets.
     * @param storeMetaJSON the store metadata as JSON.
     */
    public static void setStoreAssets(int version, String storeMetaJSON) {
        SoomlaUtils.LogDebug(TAG, "trying to set json: " + storeMetaJSON);

        if (TextUtils.isEmpty(storeMetaJSON)) {
            SoomlaUtils.LogError(TAG, "The given store assets JSON can't be empty or null!");
            return;
        }

        mCurrentAssetsVersion = version;

//        checkAndResetMetadata();

        // we always initialize from the database, unless this is the first time the game is
        // loaded - in that case we initialize with setStoreAssets.
        if (!loadFromDB()) {
            SoomlaUtils.LogDebug(TAG, "didn't find anything in DB to load. continuing with store assets json.");
            try {
                fromJSONObject(new JSONObject(storeMetaJSON));
                save();
            } catch (JSONException e) {
                SoomlaUtils.LogError(TAG, "Can't parse store metadata json. That's a major issue." + storeMetaJSON);
                BusProvider.getInstance().post(
                        new UnexpectedStoreErrorEvent(UnexpectedStoreErrorEvent.ErrorCode.GENERAL));
            }
        }
    }

    /**
     * Initializes <code>StoreInfo</code> from the database. This action should be performed only
     * once during the lifetime of a session of the game. <code>SoomlaStore</code> automatically
     * initializes <code>StoreInfo</code>. Don't do it if you don't know what you're doing.
     *
     * @return success
     */
    public static boolean loadFromDB() {
        checkAndResetMetadata();

        String key = keyMetaStoreInfo();
        String val = KeyValueStorage.getValue(key);

        if (val == null || TextUtils.isEmpty(val)) {
            SoomlaUtils.LogDebug(TAG, "store json is not in DB yet.");
            return false;
        }

        // This is done in case old versions of the DB exist (especially from
        // Cocos2dx) which used jsonType instead of className
        if (val.contains("jsonType")) {
            SoomlaUtils.LogDebug(TAG, "the StoreInfo JSON is from an older version. " +
                    "we need to delete and let it be recreated.");
            KeyValueStorage.deleteKeyValue(key);
            return false;
        }

        SoomlaUtils.LogDebug(TAG, "the metadata-economy json (from DB) is " + val);

        try {
            fromJSONObject(new JSONObject(val));

            // everything went well... StoreInfo is initialized from the local DB.
            // it's ok to return now.

            return true;
        } catch (JSONException e) {
            SoomlaUtils.LogDebug(TAG, "Can't parse metadata json. Going to return false and make "
                    + "StoreInfo load from static data: " + val);
        }

        return false;

    }

    /**
     * Checks if the <code>VirtualGood</code>d with the given <code>goodItemId</code> has upgrades.
     *
     * @param goodItemId the item id of the <code>VirtualGood</code> to check if has upgrades.
     * @return true if upgrades found for the <code>VirtualGood</code> with the given
     * <code>goodItemId</code>, otherwise false.
     */
    public static boolean hasUpgrades(String goodItemId) {
        return mGoodsUpgrades.containsKey(goodItemId);
    }

    /**
     * Checks if a given PurchasableVirtualItem is a non-consumable item
     *
     * @param pvi The PurchasableVirtualItem to check
     * @return true pvi is a non-consumable item.
     */
    public static boolean isItemNonConsumable(PurchasableVirtualItem pvi) {
        return ((pvi instanceof LifetimeVG) && (pvi.getPurchaseType() instanceof PurchaseWithMarket));
    }


    /** Setters and Getters */

    /**
     * Retrieves a single <code>VirtualItem</code> that resides in the metadata.
     *
     * @param itemId the itemId of the required <code>VirtualItem</code>
     * @return virtual item for the given <code>itemId</code>
     * @throws VirtualItemNotFoundException if no <code>VirtualItem</code> with the given
     *                                      <code>itemId</code> was found.
     */
    public static VirtualItem getVirtualItem(String itemId) throws VirtualItemNotFoundException {
        VirtualItem item = mVirtualItems.get(itemId);
        if (item == null) {
            throw new VirtualItemNotFoundException("itemId", itemId);
        }

        return item;
    }

    /**
     * Retrieves a single <code>PurchasableVirtualItem</code> that resides in the metadata.
     * IMPORTANT: The retrieved <code>PurchasableVirtualItem</code> has a <code>PurchaseType</code>
     * of <code>PurchaseWithMarket</code> (This is why we fetch here with <code>productId</code>).
     *
     * @param productId the product id of the purchasable item to be fetched
     * @return <code>PurchasableVirtualItem</code>
     * @throws VirtualItemNotFoundException if no PurchasableVirtualItem with the given
     *                                      productId was found.
     */
    public static PurchasableVirtualItem getPurchasableItem(String productId)
            throws VirtualItemNotFoundException {
        PurchasableVirtualItem item = mPurchasableItems.get(productId);
        if (item == null) {
            throw new VirtualItemNotFoundException("productId", productId);
        }

        return item;
    }

    /**
     * Retrieves the <code>VirtualCategory</code> that the virtual good with the given
     * <code>goodItemId</code> belongs to.
     *
     * @param goodItemId the id of the virtual good whose category is to be fetched
     * @return the virtual category that the good with the given <code>goodItemId</code> belongs to
     * @throws VirtualItemNotFoundException if the given <code>goodItemId</code> is not found
     */
    public static VirtualCategory getCategory(String goodItemId)
            throws VirtualItemNotFoundException {
        VirtualCategory item = mGoodsCategories.get(goodItemId);
        if (item == null) {
            throw new VirtualItemNotFoundException("goodItemId", goodItemId);
        }

        return item;
    }

    /**
     * Retrieves the first <code>UpgradeVG</code> for the given <code>goodItemId</code>.
     *
     * @param goodItemId The item id of the <code>VirtualGood</code> whose upgrade we are looking
     *                   for.
     * @return The first upgrade for the virtual good with the given <code>goodItemId</code>, or
     * null if it has no upgrades.
     */
    public static UpgradeVG getGoodFirstUpgrade(String goodItemId) {
        List<UpgradeVG> upgrades = mGoodsUpgrades.get(goodItemId);
        if (upgrades != null) {
            for (UpgradeVG upgradeVG : upgrades) {
                if (TextUtils.isEmpty(upgradeVG.getPrevItemId())) {
                    return upgradeVG;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves the last <code>UpgradeVG</code> for the given <code>goodItemId</code>.
     *
     * @param goodItemId The item id of the <code>VirtualGood</code> whose upgrade we are looking
     *                   for.
     * @return The last upgrade for the virtual good with the given <code>goodItemId</code> or null
     * if there are no upgrades.
     */
    public static UpgradeVG getGoodLastUpgrade(String goodItemId) {
        List<UpgradeVG> upgrades = mGoodsUpgrades.get(goodItemId);
        if (upgrades != null) {
            for (UpgradeVG upgradeVG : upgrades) {
                if (TextUtils.isEmpty(upgradeVG.getNextItemId())) {
                    return upgradeVG;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all <code>UpgradeVGs</code> for the virtual good with the given
     * <code>goodItemId</code>.
     *
     * @param goodItemId The item id of the <code>VirtualGood</code> whose upgrades we are looking
     *                   for.
     * @return list of all UpgradeVGs for the virtual good with the given <code>goodItemId</code>
     */
    public static List<UpgradeVG> getGoodUpgrades(String goodItemId) {
        if (mGoodsUpgrades == null) {
            return new LinkedList<UpgradeVG>();
        }
        return mGoodsUpgrades.get(goodItemId);
    }

    public static List<VirtualCurrency> getCurrencies() {
        return mCurrencies;
    }

    public static List<VirtualCurrencyPack> getCurrencyPacks() {
        return mCurrencyPacks;
    }

    public static List<VirtualGood> getGoods() {
        return mGoods;
    }

    public static List<VirtualCategory> getCategories() {
        return mCategories;
    }

    public static List<String> getAllProductIds() {
        return new ArrayList<String>(mPurchasableItems.keySet());
    }


    /** Private functions **/
    /**
     * Checks if given storeAssets is correct IStoreAssets instance
     *
     * @param assetsArray array of successors of PurchasableVirtualItem class.
     * @return true if <code>marketId</code> duplicates exists and false otherwise.
     */
    private static boolean hasMarketIdDuplicates(PurchasableVirtualItem[] assetsArray) {
        HashSet<String> marketItemIds = new HashSet<String>();
        for (PurchasableVirtualItem pvi : assetsArray) {
            if (pvi.getPurchaseType() instanceof PurchaseWithMarket) {
                String currentMarketId = ((PurchaseWithMarket)pvi.getPurchaseType()).getMarketItem().getProductId();
                if (marketItemIds.contains(currentMarketId)) {
                    return true;
                }
                marketItemIds.add(currentMarketId);
            }
        }
        return false;
    }

    /**
     * Checks if given storeAssets is correct IStoreAssets instance
     *
     * @param storeAssets
     * @throws IllegalArgumentException
     */
    private static void validateStoreAssets(IStoreAssets storeAssets) throws IllegalArgumentException {
        if (storeAssets == null) {
            throw new IllegalArgumentException("The given store assets can't be null!");
        }

        if (storeAssets.getCurrencies() == null ||
                storeAssets.getCurrencyPacks() == null ||
                storeAssets.getGoods() == null ||
                storeAssets.getCategories() == null) {
            throw new IllegalArgumentException("All IStoreAssets methods shouldn't return NULL-pointer references!");
        }

        if (hasMarketIdDuplicates(storeAssets.getGoods())
                || hasMarketIdDuplicates(storeAssets.getCurrencyPacks())) {
            throw new IllegalArgumentException("The given store assets has duplicates at marketItem productId!");
        }
    }

    /**
     * Transforms given jsonObject to StoreInfo
     *
     * @param jsonObject
     * @throws JSONException
     */
    private static void fromJSONObject(JSONObject jsonObject) throws JSONException {

        mVirtualItems = new HashMap<String, VirtualItem>();
        mPurchasableItems = new HashMap<String, PurchasableVirtualItem>();
        mGoodsCategories = new HashMap<String, VirtualCategory>();
        mGoodsUpgrades = new HashMap<String, List<UpgradeVG>>();
        mCurrencyPacks = new LinkedList<VirtualCurrencyPack>();
        mGoods = new LinkedList<VirtualGood>();
        mCategories = new LinkedList<VirtualCategory>();
        mCurrencies = new LinkedList<VirtualCurrency>();

        if (jsonObject.has(StoreJSONConsts.STORE_CURRENCIES)) {
            JSONArray virtualCurrencies = jsonObject.getJSONArray(StoreJSONConsts.STORE_CURRENCIES);
            for (int i = 0; i < virtualCurrencies.length(); i++) {
                JSONObject o = virtualCurrencies.getJSONObject(i);
                VirtualCurrency c = new VirtualCurrency(o);
                mCurrencies.add(c);

                mVirtualItems.put(c.getItemId(), c);
            }
        }

        if (jsonObject.has(StoreJSONConsts.STORE_CURRENCYPACKS)) {
            JSONArray currencyPacks = jsonObject.getJSONArray(StoreJSONConsts.STORE_CURRENCYPACKS);
            for (int i = 0; i < currencyPacks.length(); i++) {
                JSONObject o = currencyPacks.getJSONObject(i);
                VirtualCurrencyPack pack = new VirtualCurrencyPack(o);
                mCurrencyPacks.add(pack);

                mVirtualItems.put(pack.getItemId(), pack);

                PurchaseType purchaseType = pack.getPurchaseType();
                if (purchaseType instanceof PurchaseWithMarket) {
                    mPurchasableItems.put(((PurchaseWithMarket) purchaseType)
                            .getMarketItem().getProductId(), pack);
                }
            }
        }

        // The order in which VirtualGoods are created matters!
        // For example: VGU and VGP depend on other VGs
        if (jsonObject.has(StoreJSONConsts.STORE_GOODS)) {
            JSONObject virtualGoods = jsonObject.getJSONObject(StoreJSONConsts.STORE_GOODS);

            if (virtualGoods.has(StoreJSONConsts.STORE_GOODS_SU)) {
                JSONArray suGoods = virtualGoods.getJSONArray(StoreJSONConsts.STORE_GOODS_SU);
                for (int i = 0; i < suGoods.length(); i++) {
                    JSONObject o = suGoods.getJSONObject(i);
                    SingleUseVG g = new SingleUseVG(o);
                    addVG(g);
                }
            }


            if (virtualGoods.has(StoreJSONConsts.STORE_GOODS_LT)) {
                JSONArray ltGoods = virtualGoods.getJSONArray(StoreJSONConsts.STORE_GOODS_LT);
                for (int i = 0; i < ltGoods.length(); i++) {
                    JSONObject o = ltGoods.getJSONObject(i);
                    LifetimeVG g = new LifetimeVG(o);
                    addVG(g);
                }
            }


            if (virtualGoods.has(StoreJSONConsts.STORE_GOODS_EQ)) {
                JSONArray eqGoods = virtualGoods.getJSONArray(StoreJSONConsts.STORE_GOODS_EQ);
                for (int i = 0; i < eqGoods.length(); i++) {
                    JSONObject o = eqGoods.getJSONObject(i);
                    EquippableVG g = new EquippableVG(o);
                    addVG(g);
                }
            }

            if (virtualGoods.has(StoreJSONConsts.STORE_GOODS_PA)) {
                JSONArray paGoods = virtualGoods.getJSONArray(StoreJSONConsts.STORE_GOODS_PA);
                for (int i = 0; i < paGoods.length(); i++) {
                    JSONObject o = paGoods.getJSONObject(i);
                    SingleUsePackVG g = new SingleUsePackVG(o);
                    addVG(g);
                }
            }


            if (virtualGoods.has(StoreJSONConsts.STORE_GOODS_UP)) {
                JSONArray upGoods = virtualGoods.getJSONArray(StoreJSONConsts.STORE_GOODS_UP);
                for (int i = 0; i < upGoods.length(); i++) {
                    JSONObject o = upGoods.getJSONObject(i);
                    UpgradeVG g = new UpgradeVG(o);
                    addVG(g);

                    List<UpgradeVG> upgrades = mGoodsUpgrades.get(g.getGoodItemId());
                    if (upgrades == null) {
                        upgrades = new ArrayList<UpgradeVG>();
                        mGoodsUpgrades.put(g.getGoodItemId(), upgrades);
                    }
                    upgrades.add(g);
                }
            }

        }

        // Categories depend on virtual goods. That's why the have to be initialized after!
        if (jsonObject.has(StoreJSONConsts.STORE_CATEGORIES)) {
            JSONArray virtualCategories = jsonObject.getJSONArray(StoreJSONConsts.STORE_CATEGORIES);
            for (int i = 0; i < virtualCategories.length(); i++) {
                JSONObject o = virtualCategories.getJSONObject(i);
                VirtualCategory category = new VirtualCategory(o);
                mCategories.add(category);
                for (String goodItemId : category.getGoodsItemIds()) {
                    mGoodsCategories.put(goodItemId, category);
                }
            }
        }

        // This is only for NonConsumable balance migration to LifetimeVGs.
        // Remove this code when no longer needed.
        if (mNonConsumableMigrationNeeded) {
            SoomlaUtils.LogDebug(TAG, "NonConsumables balance migration is required. Doing it now.");
            nonConsBalancesToLTVGs();
        }
    }

    /**
     * Adds the given virtual good to <code>StoreInfo</code>'s <code>mGoods</code>,
     * <code>mVirtualItems</code>, and if the good has purchase type <code>PurchaseWithMarket</code>
     * then it is also added to <code>mPurchasableItems</code>.
     *
     * @param g virtual good to be added
     */
    private static void addVG(VirtualGood g) {
        mGoods.add(g);

        mVirtualItems.put(g.getItemId(), g);

        PurchaseType purchaseType = g.getPurchaseType();
        if (purchaseType instanceof PurchaseWithMarket) {
            mPurchasableItems.put(((PurchaseWithMarket) purchaseType)
                    .getMarketItem().getProductId(), g);
        }
    }

    /**
     * Converts <code>StoreInfo</code> to a <code>JSONObject</code>.
     *
     * @return a <code>JSONObject</code> representation of <code>StoreInfo</code>.
     */
    public static JSONObject toJSONObject() {

        JSONArray currencies = new JSONArray();
        for (VirtualCurrency c : mCurrencies) {
            currencies.put(c.toJSONObject());
        }

        JSONArray currencyPacks = new JSONArray();
        for (VirtualCurrencyPack pack : mCurrencyPacks) {
            currencyPacks.put(pack.toJSONObject());
        }

        JSONObject goods = new JSONObject();
        JSONArray suGoods = new JSONArray();
        JSONArray ltGoods = new JSONArray();
        JSONArray eqGoods = new JSONArray();
        JSONArray paGoods = new JSONArray();
        JSONArray upGoods = new JSONArray();
        for (VirtualGood good : mGoods) {
            if (good instanceof SingleUseVG) {
                suGoods.put(good.toJSONObject());
            } else if (good instanceof UpgradeVG) {
                upGoods.put(good.toJSONObject());
            } else if (good instanceof EquippableVG) {
                eqGoods.put(good.toJSONObject());
            } else if (good instanceof SingleUsePackVG) {
                paGoods.put(good.toJSONObject());
            } else if (good instanceof LifetimeVG) {
                ltGoods.put(good.toJSONObject());
            }
        }


        JSONArray categories = new JSONArray();
        for (VirtualCategory cat : mCategories) {
            categories.put(cat.toJSONObject());
        }

        JSONObject jsonObject = new JSONObject();
        try {
            goods.put(StoreJSONConsts.STORE_GOODS_SU, suGoods);
            goods.put(StoreJSONConsts.STORE_GOODS_LT, ltGoods);
            goods.put(StoreJSONConsts.STORE_GOODS_EQ, eqGoods);
            goods.put(StoreJSONConsts.STORE_GOODS_PA, paGoods);
            goods.put(StoreJSONConsts.STORE_GOODS_UP, upGoods);

            jsonObject.put(StoreJSONConsts.STORE_CATEGORIES, categories);
            jsonObject.put(StoreJSONConsts.STORE_CURRENCIES, currencies);
            jsonObject.put(StoreJSONConsts.STORE_GOODS, goods);
            jsonObject.put(StoreJSONConsts.STORE_CURRENCYPACKS, currencyPacks);
        } catch (JSONException e) {
            SoomlaUtils.LogError(TAG, "An error occurred while generating JSON object.");
        }

        return jsonObject;
    }

    /**
     * Saves the store's metadata in the database as JSON.
     */
    public static void save() {
        String store_json = toJSONObject().toString();
        SoomlaUtils.LogDebug(TAG, "saving StoreInfo to DB. json is: " + store_json);
        String key = keyMetaStoreInfo();
        KeyValueStorage.setValue(key, store_json);
    }

    /**
     * Replaces the given virtual item, and then saves the store's metadata.
     *
     * @param virtualItem the virtual item to replace.
     */
    public static void save(VirtualItem virtualItem) { save(virtualItem, true); }

    /**
     * Replaces the given virtual item, and then saves the store's metadata
     * (if requested).
     *
     * @param virtualItem the virtual item to replace.
     * @param saveToDB should the store's metadata be saved.
     */
    public static void save(VirtualItem virtualItem, boolean saveToDB) {
        replaceVirtualItem(virtualItem);

        if (saveToDB) {
            save();
        }
    }

    /**
     * Replaces the given virtual items, and then saves the store's metadata.
     *
     * @param virtualItems the virtual items to replace.
     */
    public static void save(List<VirtualItem> virtualItems) {
        save(virtualItems, true);
    }

    /**
     * Replaces the given virtual items, and then saves the store's metadata
     * (if requested).
     *
     * @param virtualItems the virtual items to replace.
     * @param saveToDB should the store's metadata be saved.
     */
    public static void save(List<VirtualItem> virtualItems, boolean saveToDB) {
        if ((virtualItems == null) || virtualItems.isEmpty()) {
            return;
        }

        for (VirtualItem virtualItem : virtualItems) {
            replaceVirtualItem(virtualItem);
        }

        if (saveToDB) {
            save();
        }
    }

    /**
     * Replaces an old virtual item with a new one by doing the following:
     * 1. Determines the type of the given virtual item.
     * 2. Looks for the given virtual item in the relevant list, according to its type.
     * 3. If found, removes it.
     * 4. Adds the given virtual item.
     *
     * @param virtualItem the virtual item that replaces the old one if exists.
     */
    public static void replaceVirtualItem(VirtualItem virtualItem) {
        mVirtualItems.put(virtualItem.getItemId(), virtualItem);

        if (virtualItem instanceof VirtualCurrency) {
            for (int i = 0; i < mCurrencies.size(); i++) {
                if (mCurrencies.get(i).getItemId().equals(virtualItem.getItemId())) {
                    mCurrencies.remove(i);
                    break;
                }
            }
            mCurrencies.add((VirtualCurrency) virtualItem);
        }

        if (virtualItem instanceof VirtualCurrencyPack) {
            VirtualCurrencyPack vcp = (VirtualCurrencyPack) virtualItem;
            PurchaseType purchaseType = vcp.getPurchaseType();
            if (purchaseType instanceof PurchaseWithMarket) {
                mPurchasableItems.put(((PurchaseWithMarket) purchaseType).getMarketItem()
                        .getProductId(), vcp);
            }

            for (int i = 0; i < mCurrencyPacks.size(); i++) {
                if (mCurrencyPacks.get(i).getItemId().equals(vcp.getItemId())) {
                    mCurrencyPacks.remove(i);
                    break;
                }
            }
            mCurrencyPacks.add(vcp);
        }

        if (virtualItem instanceof VirtualGood) {
            VirtualGood vg = (VirtualGood) virtualItem;

            if (vg instanceof UpgradeVG) {
                List<UpgradeVG> upgrades = mGoodsUpgrades.get(((UpgradeVG) vg).getGoodItemId());
                if (upgrades == null) {
                    upgrades = new ArrayList<UpgradeVG>();
                    mGoodsUpgrades.put(((UpgradeVG) vg).getGoodItemId(), upgrades);
                }
                upgrades.add((UpgradeVG) vg);
            }

            PurchaseType purchaseType = vg.getPurchaseType();
            if (purchaseType instanceof PurchaseWithMarket) {
                mPurchasableItems.put(((PurchaseWithMarket) purchaseType).getMarketItem()
                        .getProductId(), vg);
            }

            for (int i = 0; i < mGoods.size(); i++) {
                if (mGoods.get(i).getItemId().equals(vg.getItemId())) {
                    mGoods.remove(i);
                    break;
                }
            }
            mGoods.add(vg);
        }
    }

    /**
     * Initializes from <code>IStoreAssets</code>.
     * This happens only once - when the game is loaded for the first time.
     *
     * @param storeAssets game economy
     */
    private static void initializeWithStoreAssets(IStoreAssets storeAssets) {
        // fall-back here if the json doesn't exist,
        // we load the store from the given {@link IStoreAssets}.
        mCurrencies = new LinkedList<VirtualCurrency>(Arrays.asList(storeAssets.getCurrencies()));
        mCurrencyPacks = new LinkedList<VirtualCurrencyPack>(Arrays.asList(storeAssets.getCurrencyPacks()));
        mGoods = new LinkedList<VirtualGood>(Arrays.asList(storeAssets.getGoods()));
        mCategories = new LinkedList<VirtualCategory>(Arrays.asList(storeAssets.getCategories()));

        mVirtualItems = new HashMap<String, VirtualItem>();
        mPurchasableItems = new HashMap<String, PurchasableVirtualItem>();
        mGoodsCategories = new HashMap<String, VirtualCategory>();
        mGoodsUpgrades = new HashMap<String, List<UpgradeVG>>();

        for (VirtualCurrency vi : mCurrencies) {
            mVirtualItems.put(vi.getItemId(), vi);
        }

        for (VirtualCurrencyPack vi : mCurrencyPacks) {
            mVirtualItems.put(vi.getItemId(), vi);

            PurchaseType purchaseType = vi.getPurchaseType();
            if (purchaseType instanceof PurchaseWithMarket) {
                mPurchasableItems.put(((PurchaseWithMarket) purchaseType).getMarketItem()
                        .getProductId(), vi);
            }
        }

        for (VirtualGood vi : mGoods) {
            mVirtualItems.put(vi.getItemId(), vi);

            if (vi instanceof UpgradeVG) {
                List<UpgradeVG> upgrades = mGoodsUpgrades.get(((UpgradeVG) vi).getGoodItemId());
                if (upgrades == null) {
                    upgrades = new ArrayList<UpgradeVG>();
                    mGoodsUpgrades.put(((UpgradeVG) vi).getGoodItemId(), upgrades);
                }
                upgrades.add((UpgradeVG) vi);
            }

            PurchaseType purchaseType = vi.getPurchaseType();
            if (purchaseType instanceof PurchaseWithMarket) {
                mPurchasableItems.put(((PurchaseWithMarket) purchaseType).getMarketItem()
                        .getProductId(), vi);
            }
        }

        for (VirtualCategory category : mCategories) {
            for (String goodItemId : category.getGoodsItemIds()) {
                mGoodsCategories.put(goodItemId, category);
            }
        }

        // This is only for NonConsumable balance migration to LifetimeVGs.
        // Remove this code when no longer needed.
        if (mNonConsumableMigrationNeeded) {
            SoomlaUtils.LogDebug(TAG, "NonConsumables balance migration is required. Doing it now.");
            nonConsBalancesToLTVGs();
        }

        save();
    }

    private static void checkAndResetMetadata() {
        SoomlaUtils.LogDebug(TAG, "checking metadata version ...");
        SharedPreferences prefs = SoomlaApp.getAppContext().getSharedPreferences(SoomlaConfig.PREFS_NAME,
                Context.MODE_PRIVATE);
        boolean resetMeta = false;
        try {
            int mt_ver = prefs.getInt("MT_VER", 0);
            int sa_ver_old = prefs.getInt("SA_VER_OLD", -1);

            if (mt_ver < StoreConfig.METADATA_VERSION) {
                mNonConsumableMigrationNeeded = true;
            }

            resetMeta = mt_ver < StoreConfig.METADATA_VERSION || sa_ver_old < mCurrentAssetsVersion;
        } catch (Exception e) {
            SoomlaUtils.LogDebug(TAG, "This is probably an older (obfuscated) sharedPrefs");
            resetMeta = true;
        }

        if (resetMeta) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("MT_VER", StoreConfig.METADATA_VERSION);
            edit.putInt("SA_VER_OLD", mCurrentAssetsVersion);
            edit.commit();

            KeyValueStorage.deleteKeyValue(keyMetaStoreInfo());
        }
    }

    /**
     * IStoreAssets was changed and version number was bumped but we need to check if we need to do balance migration for non-consumables.
     * The metadata in DB was deleted and we're overwriting it.
     * We just need to set the balances of the lifetime items instead of the non-consumables.
     */
    private static void nonConsBalancesToLTVGs() {
        for(VirtualGood good : mGoods) {
            if ((good instanceof LifetimeVG) && good.getPurchaseType() instanceof PurchaseWithMarket) {
                String keyNonConsExist = DB_NONCONSUMABLE_KEY_PREFIX + good.getItemId() + ".exists";
                if (KeyValueStorage.getValue(keyNonConsExist) != null) {
                    good.give(1);
                    KeyValueStorage.deleteKeyValue(keyNonConsExist);
                }
            }
        }
        mNonConsumableMigrationNeeded = false;
    }

    /**
     * Private Members *
     */

    private static String keyMetaStoreInfo() {
        return "meta.storeinfo";
    }

    private static final String TAG = "SOOMLA StoreInfo"; //used for Log messages
    public static final String DB_NONCONSUMABLE_KEY_PREFIX = "nonconsumable.";

    private static boolean mNonConsumableMigrationNeeded = false;

    // convenient hash of virtual items
    private static HashMap<String, VirtualItem> mVirtualItems;

    // convenient hash of purchasable virtual items
    private static HashMap<String, PurchasableVirtualItem> mPurchasableItems;

    // convenient hash of goods-categories
    private static HashMap<String, VirtualCategory> mGoodsCategories;

    // convenient hash of good-upgrades
    private static HashMap<String, List<UpgradeVG>> mGoodsUpgrades;

    // list of virtual currencies
    private static List<VirtualCurrency> mCurrencies;

    // list of currency-packs
    private static List<VirtualCurrencyPack> mCurrencyPacks;

    // list of virtual goods
    private static List<VirtualGood> mGoods;

    // list of virtul categories
    private static List<VirtualCategory> mCategories;

    private static int mCurrentAssetsVersion = 0;
}
