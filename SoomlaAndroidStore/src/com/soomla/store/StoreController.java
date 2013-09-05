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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import com.soomla.billing.BillingService;
import com.soomla.billing.Consts;
import com.soomla.billing.PurchaseObserver;
import com.soomla.billing.ResponseHandler;
import com.soomla.store.data.ObscuredSharedPreferences;
import com.soomla.store.data.StoreInfo;
import com.soomla.store.domain.GoogleMarketItem;
import com.soomla.store.domain.PurchasableVirtualItem;
import com.soomla.store.events.*;
import com.soomla.store.exceptions.VirtualItemNotFoundException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class holds the basic assets needed to operate the Store.
 * You can use it to purchase products from Google Play.
 *
 * This is the only class you need to initialize in order to use the SOOMLA SDK.
 *
 * In addition to initializing this class, you'll also have to call
 * {@link StoreController#storeOpening(android.app.Activity)} and
 * {@link com.soomla.store.StoreController#storeClosing()} when you open the store window or close it. These two
 * calls initializes important components that support billing and storage information (see implementation below).
 * IMPORTANT: if you use the SOOMLA's storefront, then DON'T call these 2 functions.
 *
 */
public class StoreController extends PurchaseObserver {

    /**
     * This initializer also initializes {@link StoreInfo}.
     * @param storeAssets is the definition of your application specific assets.
     * @param publicKey is the public key given to you from Google.
     * @param customSecret is your encryption secret (it's used to encrypt your data in the database)
     */
    public void initialize(IStoreAssets storeAssets,
                          String publicKey,
                          String customSecret){

        if (mInitialized) {
            StoreUtils.LogError(TAG, "StoreController is already initialized. You can't initialize it twice!");
            return;
        }

        SharedPreferences prefs = new ObscuredSharedPreferences(SoomlaApp.getAppContext().getSharedPreferences(StoreConfig.PREFS_NAME, Context.MODE_PRIVATE));
        SharedPreferences.Editor edit = prefs.edit();
        if (publicKey != null && publicKey.length() != 0) {
            edit.putString(StoreConfig.PUBLIC_KEY, publicKey);
        } else if (prefs.getString(StoreConfig.PUBLIC_KEY, "").length() == 0) {
            StoreUtils.LogError(TAG, "publicKey is null or empty. can't initialize store !!");
            return;
        }
        if (customSecret != null && customSecret.length() != 0) {
            edit.putString(StoreConfig.CUSTOM_SEC, customSecret);
        } else if (prefs.getString(StoreConfig.CUSTOM_SEC, "").length() == 0) {
            StoreUtils.LogError(TAG, "customSecret is null or empty. can't initialize store !!");
            return;
        }
        edit.putInt("SA_VER_NEW", storeAssets.getVersion());
        edit.commit();

        if (storeAssets != null) {
            StoreInfo.setStoreAssets(storeAssets);
        }

        if (startBillingService()) {
            // We're not restoring transactions automatically anymore.
            // Call storeController.getInstance().restoreTransactions() when you want to do that.
//            tryRestoreTransactions();
        }

        mInitialized = true;
        BusProvider.getInstance().post(new StoreControllerInitializedEvent());
    }

    /**
     * Start a purchase process with Google Play.
     *
     * @param googleMarketItem is the item to purchase. This item has to be defined EXACTLY the same in Google Play.
     * @param payload a payload to get back when this purchase is finished.
     */
    public boolean buyWithGooglePlay(GoogleMarketItem googleMarketItem, String payload) {

        SharedPreferences prefs = new ObscuredSharedPreferences(SoomlaApp.getAppContext().getSharedPreferences(StoreConfig.PREFS_NAME, Context.MODE_PRIVATE));
        String publicKey = prefs.getString(StoreConfig.PUBLIC_KEY, "");
        if (publicKey.length() == 0 || publicKey.equals("[YOUR PUBLIC KEY FROM GOOGLE PLAY]")) {
            StoreUtils.LogError(TAG, "You didn't provide a public key! You can't make purchases.");
            return false;
        }

        if (!mBillingService.requestPurchase(googleMarketItem.getProductId(), Consts.ITEM_TYPE_INAPP, payload)){
            return false;
        }
        try {
            BusProvider.getInstance().post(new PlayPurchaseStartedEvent(StoreInfo.getPurchasableItem(googleMarketItem.getProductId())));
        } catch (VirtualItemNotFoundException e) {
            StoreUtils.LogError(TAG, "Couldn't find a purchasable item with productId: " + googleMarketItem.getProductId());
        }
        return true;
    }

    /**
     * Call this function when you open the actual store window
     * @param activity is the activity being opened (or the activity that contains the store)/
     */
    public void storeOpening(Activity activity){

        mLock.lock();
        if (mStoreOpen) {
            StoreUtils.LogError(TAG, "Store is already open !");
            mLock.unlock();
            return;
        }

        initCompatibilityLayer(activity);

        /* Initialize StoreInfo from database in case any changes were done to it while the store was closed */
        StoreInfo.initializeFromDB();

        /* Billing */
        startBillingService();

        BusProvider.getInstance().post(new OpeningStoreEvent());

        mStoreOpen = true;
        mLock.unlock();
    }

    /**
     * Call this function when you close the actual store window.
     */
    public void storeClosing(){
        if (!mStoreOpen) return;

        mStoreOpen = false;

        BusProvider.getInstance().post(new ClosingStoreEvent());

        stopBillingService();
//        ResponseHandler.unregister(this);
    }

    /**
     * Initiate the restoreTransactions process
     */
    public void restoreTransactions() {
        StoreUtils.LogDebug(TAG, "Sending restore transaction request");
        mBillingService.restoreTransactions();

        BusProvider.getInstance().post(new RestoreTransactionsStartedEvent());
    }

    /**
     * Answers the question: "Were transactions already restored for this game?"
     */
    public boolean transactionsAlreadyRestored() {
        SharedPreferences prefs = new ObscuredSharedPreferences(SoomlaApp.getAppContext().getSharedPreferences(StoreConfig.PREFS_NAME, Context.MODE_PRIVATE));
        return prefs.getBoolean("RESTORED", false);
    }


    /** PurchaseObserver overridden functions**/

    /**
     * docs in {@link PurchaseObserver#onBillingSupported(boolean supported, String type)}.
     */
    @Override
    public void onBillingSupported(boolean supported, String type) {
        if (type == null || type.equals(Consts.ITEM_TYPE_INAPP)) {
            if (supported) {
                StoreUtils.LogDebug(TAG, "billing is supported !");

                BusProvider.getInstance().post(new BillingSupportedEvent());
            } else {
                // purchase is not supported. just send a message to JS to disable the "get more ..." button.

                StoreUtils.LogDebug(TAG, "billing is not supported !");

                BusProvider.getInstance().post(new BillingNotSupportedEvent());
            }
        } else if (type.equals(Consts.ITEM_TYPE_SUBSCRIPTION)) {
            // subscription is not supported
            // Soomla doesn't support subscriptions yet. doing nothing here ...
        } else {
            // subscription is not supported
            // Soomla doesn't support subscriptions yet. doing nothing here ...
        }
    }

    /**
     * docs in {@link PurchaseObserver#onPurchaseStateChange(com.soomla.billing.Consts.PurchaseState, String, long, String)}.
     */
    @Override
    public void onPurchaseStateChange(Consts.PurchaseState purchaseState, String productId, long purchaseTime, String developerPayload) {
        try {
            PurchasableVirtualItem purchasableVirtualItem = StoreInfo.getPurchasableItem(productId);

            BusProvider.getInstance().post(new PlayPurchaseEvent(purchasableVirtualItem, developerPayload));

            if (purchaseState == Consts.PurchaseState.PURCHASED) {
                purchasableVirtualItem.give(1);
            }

            if (purchaseState == Consts.PurchaseState.REFUNDED){
                if (!StoreConfig.friendlyRefunds) {
                    purchasableVirtualItem.take(1);
                }
            }

            BusProvider.getInstance().post(new ItemPurchasedEvent(purchasableVirtualItem));
        } catch (VirtualItemNotFoundException e) {
            StoreUtils.LogError(TAG, "ERROR : Couldn't find the " + purchaseState.name() +
                    " VirtualCurrencyPack OR GoogleMarketItem  with productId: " + productId +
                    ". It's unexpected so an unexpected error is being emitted.");
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
        }
    }

    /**
     * docs in {@link PurchaseObserver#onRequestPurchaseResponse(com.soomla.billing.BillingService.RequestPurchase, com.soomla.billing.Consts.ResponseCode)}.
     */
    @Override
    public void onRequestPurchaseResponse(BillingService.RequestPurchase request, Consts.ResponseCode responseCode) {
        if (responseCode == Consts.ResponseCode.RESULT_OK) {
            // purchase was sent to server
        } else if (responseCode == Consts.ResponseCode.RESULT_USER_CANCELED) {

            try {
                BusProvider.getInstance().post(new PlayPurchaseCancelledEvent(StoreInfo.getPurchasableItem(request.mProductId)));
            } catch (VirtualItemNotFoundException e) {
                StoreUtils.LogError(TAG, "ERROR : Couldn't find the CANCELLED VirtualCurrencyPack OR GoogleMarketItem  with productId: " + request.mProductId +
                        ". It's unexpected so an unexpected error is being emitted.");
                BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
            }

        } else {
            // purchase failed !

            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
            StoreUtils.LogError(TAG, "ERROR : Purchase failed for productId: " + request.mProductId);
        }
    }

    /**
     * docs in {@link PurchaseObserver#onRestoreTransactionsResponse(com.soomla.billing.BillingService.RestoreTransactions, com.soomla.billing.Consts.ResponseCode)}.
     */
    @Override
    public void onRestoreTransactionsResponse(BillingService.RestoreTransactions request, Consts.ResponseCode responseCode) {

        if (responseCode == Consts.ResponseCode.RESULT_OK) {
            StoreUtils.LogDebug(TAG, "RestoreTransactions succeeded");

            SharedPreferences prefs = new ObscuredSharedPreferences(SoomlaApp.getAppContext().getSharedPreferences(StoreConfig.PREFS_NAME, Context.MODE_PRIVATE));
            SharedPreferences.Editor edit = prefs.edit();

            edit.putBoolean("RESTORED", true);
            edit.commit();

            BusProvider.getInstance().post(new RestoreTransactionsEvent(true));
        } else {
            StoreUtils.LogDebug(TAG, "RestoreTransactions error: " + responseCode);

            BusProvider.getInstance().post(new RestoreTransactionsEvent(false));
        }

        // we're stopping the billing service only if the store was not opened while the request was sent
        if (!mStoreOpen) {
            stopBillingService();
        }
    }

    public boolean isTestMode() {
        return mTestMode;
    }

    public void setTestMode(boolean mTestMode) {
        this.mTestMode = mTestMode;
    }

    /** Private methods **/

    private boolean startBillingService() {
        mLock.lock();
        if (mBillingService == null) {
            ResponseHandler.register(this);
            mBillingService = new BillingService();
            mBillingService.setContext(SoomlaApp.getAppContext());

            if (!mBillingService.checkBillingSupported(Consts.ITEM_TYPE_INAPP)){
                StoreUtils.LogDebug(TAG, "There's no connectivity with the billing service.");

                mLock.unlock();
                return false;
            }
        }

        mLock.unlock();
        return true;
    }

    private void stopBillingService() {
        mLock.lock();
        if (mBillingService != null) {
            mBillingService.unbind();
            mBillingService = null;
        }
        mLock.unlock();
    }

    public BillingService getBillingService() {
        return mBillingService;
    }

    /** Singleton **/

    private static StoreController sInstance = null;

    public static StoreController getInstance(){
        if (sInstance == null){
            sInstance = new StoreController();
        }

        return sInstance;
    }

    private StoreController() {
    }


    /** Private Members**/

    private static final String TAG       = "SOOMLA StoreController";

    private boolean mStoreOpen            = false;
    private boolean mInitialized          = false;
    private boolean mTestMode             = false;

    private BillingService mBillingService;
    private Lock    mLock = new ReentrantLock();
}
