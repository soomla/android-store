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
package com.soomla.store;

import org.onepf.oms.OpenIabHelper;

/**
 * This class holds the store's configurations.
 */
public final class StoreConfig{
    /** Shared Preferences **/
    public static final String PREFS_NAME      = "store.prefs";
    public static final String DB_INITIALIZED  = "db_initialized";
    public static final String PUBLIC_KEY      = "PO#SU#SO#GU";
    public static final String CUSTOM_SEC      = "SU#LL#SE#RE";

    //object to configure billing in the app
    public static final OpenIabHelper.Options billingOptions = new OpenIabHelper.Options();

    /* CHANGE THIS SECRET NOW ! */
    public static String SOOM_SEC = "SINC_SSEEKK";

    // do you want to print out debug messages?
    public static final boolean logDebug = false;

    public static final boolean friendlyRefunds = false;

    // the obfuscated salt. randomly generated numbers.
    // IMPORTANT: it's recommended that you change these numbers for your specific application BUT change them only once.
    public static final byte[] obfuscationSalt = new byte[] { 64, -54, -113, -47, 98, -52, 87, -102, -65, -127, 89, 51, -11, -35, 30, 77, -45, 75, -26, 3 };

    // ***NEVER*** CHANGE THE VALUE FOR THIS VARIABLE !!!
    // This value defines the version of the metadata located in your database.
    public static final int METADATA_VERSION = 3;

    /*
    if this is true than the database will be deleted whenever the application loads.
    don't release your game with this option set to true !!!!!!!!!!!!
    otherwise, your users will lose all their data every time they load the application.

    NOTE: this feature can be useful on testing when you want to change stuff in your implementation of IStoreAssets
        and see how they look like. If you try to change things in IStoreAssets and don't delete the DB than your
        changes will not be shown.
     */
    public static final boolean DB_DELETE = false;

    /**
     * Map sku and storeSku for particular store.
     * <p>
     * The best approach is to use SKU that unique in universe like <code>com.companyname.application.item</code>.
     * Such SKU fit most of stores so it doesn't need to be mapped.
     * <p>
     * If best approach is not applicable use application inner SKU in code (usually it is SKU for Google Play)
     * and map SKU from other stores using this method. OpenIAB will map SKU in both directions,
     * so you can use only your inner SKU
     *
     * @param sku - application inner SKU
     * @param storeSku - shouldn't duplicate already mapped values
     * @param storeName - @see {@link org.onepf.oms.IOpenAppstore#getAppstoreName()} or {@link org.onepf.oms.OpenIabHelper#NAME_GOOGLE}, {@link org.onepf.oms.OpenIabHelper#NAME_AMAZON}
     * {@link org.onepf.oms.OpenIabHelper#NAME_AMAZON} and etc.
     */
    public static void mapSku(String sku, String storeName, String storeSku) {
        OpenIabHelper.mapSku(sku, storeName, storeSku);
    }
}
