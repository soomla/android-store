/*
 * Copyright (C) 2012 Soomla Inc.
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
import android.database.Cursor;
import android.provider.Settings;
import android.text.TextUtils;
import com.soomla.billing.util.AESObfuscator;
import com.soomla.store.SoomlaApp;
import com.soomla.store.StoreConfig;
import com.soomla.store.StoreUtils;
import com.soomla.store.domain.VirtualItem;
import com.soomla.store.domain.virtualCurrencies.VirtualCurrency;
import com.soomla.store.domain.virtualGoods.VirtualGood;

/**
 * This is the place where all the relevant storage classes are created.
 * This class contains static methods for you to retrieve the various storages.
 *
 * s
 *
 */
public class StorageManager {

    /** Getters **/

    public static VirtualCurrencyStorage getVirtualCurrencyStorage(){
        return mVirtualCurrencyStorage;
    }

    public static VirtualGoodsStorage getVirtualGoodsStorage(){
        return mVirtualGoodsStorage;
    }

    public static AESObfuscator getAESObfuscator(){
        if (mObfuscator == null) {
            String deviceId = Settings.Secure.getString(SoomlaApp.getAppContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            mObfuscator = new AESObfuscator(StoreConfig.obfuscationSalt, SoomlaApp.getAppContext().getPackageName(), deviceId);
        }

        return mObfuscator;
    }

    public synchronized static KeyValDatabase getDatabase(){

        if (mKvDatabase == null) {
            mKvDatabase = new KeyValDatabase(SoomlaApp.getAppContext());

            migrateOldData();

            SharedPreferences prefs = new ObscuredSharedPreferences(SoomlaApp.getAppContext(),
                    SoomlaApp.getAppContext().getSharedPreferences(StoreConfig.PREFS_NAME, Context.MODE_PRIVATE));
            int mt_ver = prefs.getInt("MT_VER", 0);
            int sa_ver_old = prefs.getInt("SA_VER_OLD", -1);
            int sa_ver_new = prefs.getInt("SA_VER_NEW", 0);
            if (mt_ver < StoreConfig.METADATA_VERSION || sa_ver_old < sa_ver_new) {
                SharedPreferences.Editor edit = prefs.edit();
                edit.putInt("MT_VER", StoreConfig.METADATA_VERSION);
                edit.putInt("SA_VER_OLD", sa_ver_new);
                edit.commit();

                String keyStoreFront = mObfuscator.obfuscateString(KeyValDatabase.keyMetaStorefrontInfo());
                String keyStoreInfo = mObfuscator.obfuscateString(KeyValDatabase.keyMetaStoreInfo());
                mKvDatabase.deleteKeyVal(keyStoreFront);
                mKvDatabase.deleteKeyVal(keyStoreInfo);
            }
        }

        return mKvDatabase;
    }

    public static NonConsumableItemsStorage getNonConsumableItemsStorage() {
        return mNonConsumableItemsStorage;
    }

    public static KeyValueStorage getKeyValueStorage() {
        return mKeyValueStorage;
    }

    public static VirtualItemStorage getVirtualItemStorage(VirtualItem item) {
        VirtualItemStorage storage = null;


        if (item instanceof VirtualGood) {
            storage = getVirtualGoodsStorage();
        } else if (item instanceof VirtualCurrency) {
            storage = getVirtualCurrencyStorage();
        }
        return storage;
    }


    /** Private members **/
    private static final String TAG = "SOOMLA StorageManager";

    private static boolean mOldDataMigrated = false;

    private static VirtualGoodsStorage     mVirtualGoodsStorage         = new VirtualGoodsStorage();
    private static VirtualCurrencyStorage  mVirtualCurrencyStorage      = new VirtualCurrencyStorage();
    private static NonConsumableItemsStorage mNonConsumableItemsStorage = new NonConsumableItemsStorage();
    private static KeyValueStorage         mKeyValueStorage             = new KeyValueStorage();
    private static AESObfuscator           mObfuscator;
    private static KeyValDatabase          mKvDatabase;
















    /** Migration of databases for versions < v2.0 **/
    /** This code will be removed in the next version of android-store. Along with the StoreDatabase itself.**/

    private static void migrateOldData() {
        if (mOldDataMigrated) {
            return;
        }

        if (!StoreDatabase.checkDataBaseExists(SoomlaApp.getAppContext())) {
            StoreUtils.LogDebug(TAG, "Old store database doesn't exist. Nothing to migrate.");
            return;
        }

        StoreUtils.LogDebug(TAG, "Old store database exists. Migrating now!");

        StoreDatabase storeDatabase = new StoreDatabase(SoomlaApp.getAppContext());

        Cursor cursor = storeDatabase.getVirtualCurrencies();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int itemIdColIdx = cursor.getColumnIndexOrThrow(StoreDatabase.VIRTUAL_CURRENCY_COLUMN_ITEM_ID);
                int balanceColIdx = cursor.getColumnIndexOrThrow(StoreDatabase.VIRTUAL_CURRENCY_COLUMN_BALANCE);
                String itemIdStr = cursor.getString(itemIdColIdx);
                try {
                    itemIdStr = mObfuscator.unobfuscateToString(itemIdStr);

                    String balanceStr = cursor.getString(balanceColIdx);

                    String key = KeyValDatabase.keyCurrencyBalance(itemIdStr);
                    StoreUtils.LogDebug(TAG, "currency key: " + key + " val: " + balanceStr);
                    key = mObfuscator.obfuscateString(key);

                    mKvDatabase.setKeyVal(key, balanceStr);
                } catch (AESObfuscator.ValidationException e) {
                    StoreUtils.LogError(TAG, e.getMessage());
                }
            }
        }
        if (cursor != null) {
            cursor.close();
        }

        cursor = storeDatabase.getVirtualGoods();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int itemIdColIdx = cursor.getColumnIndexOrThrow(StoreDatabase.VIRTUAL_GOODS_COLUMN_ITEM_ID);
                int balanceColIdx = cursor.getColumnIndexOrThrow(StoreDatabase.VIRTUAL_GOODS_COLUMN_BALANCE);
                int equippedColIdx = cursor.getColumnIndexOrThrow(StoreDatabase.VIRTUAL_GOODS_COLUMN_EQUIPPED);
                String itemIdStr = cursor.getString(itemIdColIdx);
                try {
                    itemIdStr = mObfuscator.unobfuscateToString(itemIdStr);
                } catch (AESObfuscator.ValidationException e) {
                    e.printStackTrace();
                }
                String balanceStr = cursor.getString(balanceColIdx);
                int equippedInt = cursor.getInt(equippedColIdx);

                String key = KeyValDatabase.keyGoodBalance(itemIdStr);
                try {
                    StoreUtils.LogDebug(TAG, "good key: " + key + " val: " + mObfuscator.unobfuscateToString(balanceStr));

                    key = mObfuscator.obfuscateString(key);
                    mKvDatabase.setKeyVal(key, balanceStr);
                    if (equippedInt > 0) {
                        mKvDatabase.setKeyVal(key, "");
                    }
                } catch (AESObfuscator.ValidationException e) {
                    e.printStackTrace();
                }
            }
        }
        if (cursor != null) {
            cursor.close();
        }

        cursor = storeDatabase.getMetaData();
        if (cursor != null) {
            int storeinfoIdx = cursor.getColumnIndexOrThrow(StoreDatabase.METADATA_COLUMN_STOREINFO);
            int storefrontinfoIdx = cursor.getColumnIndexOrThrow(StoreDatabase.METADATA_COLUMN_STOREFRONTINFO);
            if (cursor.moveToNext()) {
                String storeInfo = cursor.getString(storeinfoIdx);
                String storefrontInfo = cursor.getString(storefrontinfoIdx);

                String key = KeyValDatabase.keyMetaStoreInfo();
                key = mObfuscator.obfuscateString(key);
                if (!TextUtils.isEmpty(storeInfo)) {
                    mKvDatabase.setKeyVal(key, storeInfo);
                }

                key = KeyValDatabase.keyMetaStorefrontInfo();
                key = mObfuscator.obfuscateString(key);
                if (!TextUtils.isEmpty(storefrontInfo)) {
                    mKvDatabase.setKeyVal(key, storefrontInfo);
                }
            }
        }
        if (cursor != null) {
            cursor.close();
        }

        cursor = storeDatabase.getGoogleManagedItems();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int productIdColIdx = cursor.getColumnIndexOrThrow(StoreDatabase.GOOGLE_MANAGED_ITEMS_COLUMN_PRODUCT_ID);
                String productIdStr = cursor.getString(productIdColIdx);
                try {
                    productIdStr = mObfuscator.unobfuscateToString(productIdStr);
                } catch (AESObfuscator.ValidationException e) {
                    e.printStackTrace();
                }

                String key = KeyValDatabase.keyNonConsExists(productIdStr);
                key = mObfuscator.obfuscateString(key);
                mKvDatabase.setKeyVal(key, "");
            }
        }
        if (cursor != null) {
            cursor.close();
        }

        cursor = storeDatabase.getKeyVals();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int keyColIdx = cursor.getColumnIndexOrThrow(StoreDatabase.KEYVAL_COLUMN_KEY);
                int valColIdx = cursor.getColumnIndexOrThrow(StoreDatabase.KEYVAL_COLUMN_KEY);
                String keyStr = cursor.getString(keyColIdx);
                String valStr = cursor.getString(valColIdx);

                mKvDatabase.setKeyVal(keyStr, valStr);
            }
        }
        if (cursor != null) {
            cursor.close();
        }

        storeDatabase.purgeDatabase(SoomlaApp.getAppContext());
        storeDatabase.close();

        mOldDataMigrated = true;

        StoreUtils.LogDebug(TAG, "Finished migrating old database.");
    }
}
