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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.soomla.store.StoreConfig;

/**
 * The KeyValDatabase provides basic key-value store above SQLite.
 */
public class KeyValDatabase {

    public KeyValDatabase(Context context) {

        if (StoreConfig.DB_DELETE){
            context.deleteDatabase(DATABASE_NAME);
        }

        mDatabaseHelper = new DatabaseHelper(context);
        mStoreDB = mDatabaseHelper.getWritableDatabase();
    }

    /**
     * Closes the database.
     */
    public synchronized void close() {
        mDatabaseHelper.close();
    }

    /**
     * Sets the given value to the given key
     * @param key the key of the key-val pair.
     * @param val the val of the key-val pair.
     */
    public synchronized void setKeyVal(String key, String val) {
        ContentValues values = new ContentValues();
        values.put(KEYVAL_COLUMN_VAL, val);

        int affected = mStoreDB.update(KEYVAL_TABLE_NAME, values, KEYVAL_COLUMN_KEY + "='" + key + "'", null);
        if (affected == 0){
            values.put(KEYVAL_COLUMN_KEY, key);
            mStoreDB.replace(KEYVAL_TABLE_NAME, null, values);
        }
    }

    /**
     * Gets the value for the given key.
     * @param key the key of the key-val pair.
     * @return a value for the given key.
     */
    public synchronized String getKeyVal(String key) {
        Cursor cursor = mStoreDB.query(KEYVAL_TABLE_NAME, KEYVAL_COLUMNS, KEYVAL_COLUMN_KEY + "='" + key + "'",
                null, null, null, null);
 
        if (cursor != null && cursor.moveToNext()) {
            int valColIdx = cursor.getColumnIndexOrThrow(KEYVAL_COLUMN_VAL);
            String ret = cursor.getString(valColIdx);
            cursor.close();
            return ret;
        }
        
        if(cursor != null) {
        	cursor.close();
        }
        
        return null;
    }

    public synchronized void deleteKeyVal(String key) {
        mStoreDB.delete(KEYVAL_TABLE_NAME, KEYVAL_COLUMN_KEY + "=?", new String[] { key });
    }

    private class DatabaseHelper extends SQLiteOpenHelper{

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            if (!sqLiteDatabase.isReadOnly()){
                sqLiteDatabase.execSQL("PRAGMA foreign_key=ON");
            }

            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + KEYVAL_TABLE_NAME + "(" +
                    KEYVAL_COLUMN_KEY + " TEXT PRIMARY KEY, " +
                    KEYVAL_COLUMN_VAL + " TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            // Nothing to do here ...
        }
    }


    /** SOOMLA keys **/

    public static String keyGoodBalance(String itemId) {
        return "good." + itemId + ".balance";
    }

    public static String keyGoodEquipped(String itemId) {
        return "good." + itemId + ".equipped";
    }

    public static String keyCurrencyBalance(String itemId) {
        return "currency." + itemId + ".balance";
    }

    public static String keyNonConsExists(String productId) {
        return "nonconsumable." + productId + ".exists";
    }

    public static String keyMetaStoreInfo() {
        return "meta.storeinfo";
    }

    public static String keyMetaStorefrontInfo() {
        return "meta.storefrontinfo";
    }


    // General key-value storage
    private static final String KEYVAL_TABLE_NAME = "kv_store";
    public static final String KEYVAL_COLUMN_KEY = "key";
    public static final String KEYVAL_COLUMN_VAL = "val";
    private static final String[] KEYVAL_COLUMNS = {
            KEYVAL_COLUMN_KEY, KEYVAL_COLUMN_VAL
    };


    /** Private Members**/

    private static final String TAG = "KeyValDatabase";
    private static final String DATABASE_NAME               = "store.kv.db";

    private SQLiteDatabase mStoreDB;
    private DatabaseHelper mDatabaseHelper;
}
