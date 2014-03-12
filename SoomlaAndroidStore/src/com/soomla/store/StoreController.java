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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.soomla.billing.Consts;
import com.soomla.billing.IabException;
import com.soomla.billing.IabHelper;
import com.soomla.billing.IabResult;
import com.soomla.billing.Inventory;
import com.soomla.billing.Purchase;
import com.soomla.store.data.ObscuredSharedPreferences;
import com.soomla.store.data.StoreInfo;
import com.soomla.store.domain.GoogleMarketItem;
import com.soomla.store.domain.NonConsumableItem;
import com.soomla.store.domain.PurchasableVirtualItem;
import com.soomla.store.events.BillingNotSupportedEvent;
import com.soomla.store.events.BillingSupportedEvent;
import com.soomla.store.events.IabServiceStartedEvent;
import com.soomla.store.events.IabServiceStoppedEvent;
import com.soomla.store.events.ItemPurchasedEvent;
import com.soomla.store.events.PlayPurchaseCancelledEvent;
import com.soomla.store.events.PlayPurchaseEvent;
import com.soomla.store.events.PlayPurchaseStartedEvent;
import com.soomla.store.events.PlayRefundEvent;
import com.soomla.store.events.RestoreTransactionsEvent;
import com.soomla.store.events.RestoreTransactionsStartedEvent;
import com.soomla.store.events.StoreControllerInitializedEvent;
import com.soomla.store.events.UnexpectedStoreErrorEvent;
import com.soomla.store.exceptions.VirtualItemNotFoundException;

import java.util.List;

/**
 * This class holds the basic assets needed to operate the Store.
 * You can use it to purchase products from Google Play.
 *
 * This is the only class you need to initialize in order to use the SOOMLA SDK.
 *
 * To properly work with this class you must initialize it with the @{link #initialize} method.
 */
public class StoreController {

    /**
     * This initializer also initializes {@link StoreInfo}.
     * @param storeAssets is the definition of your application specific assets.
     * @param publicKey is the public key given to you from Google.
     * @param customSecret is your encryption secret (it's used to encrypt your data in the database)
     */
    public boolean initialize(IStoreAssets storeAssets, String publicKey, String customSecret) {
        if (mInitialized) {
            String err = "StoreController is already initialized. You can't initialize it twice!";
            StoreUtils.LogError(TAG, err);
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(err));
            return false;
        }

        StoreUtils.LogDebug(TAG, "StoreController Initializing ...");

        SharedPreferences prefs = new ObscuredSharedPreferences(SoomlaApp.getAppContext().getSharedPreferences(StoreConfig.PREFS_NAME, Context.MODE_PRIVATE));
        SharedPreferences.Editor edit = prefs.edit();

        if (publicKey != null && publicKey.length() != 0) {
            edit.putString(StoreConfig.PUBLIC_KEY, publicKey);
        } else if (prefs.getString(StoreConfig.PUBLIC_KEY, "").length() == 0) {
            String err = "publicKey is null or empty. Can't initialize store!!";
            StoreUtils.LogError(TAG, err);
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(err));
            return false;
        }

        if (customSecret != null && customSecret.length() != 0) {
            edit.putString(StoreConfig.CUSTOM_SEC, customSecret);
        } else if (prefs.getString(StoreConfig.CUSTOM_SEC, "").length() == 0) {
            String err = "customSecret is null or empty. Can't initialize store!!";
            StoreUtils.LogError(TAG, err);
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(err));
            return false;
        }
        edit.putInt("SA_VER_NEW", storeAssets.getVersion());
        edit.commit();

        if (storeAssets != null) {
            StoreInfo.setStoreAssets(storeAssets);
        }

        // Update SOOMLA store from DB
        StoreInfo.initializeFromDB();

        // Set up helper for the first time, querying and synchronizing inventory
        StoreUtils.LogDebug(TAG, "Querying and Synchronizing inventory ...");
        startIabHelper(new StoreController.OnIabStartedListener() {
            @Override
            public void onIabStarted() {
                StoreUtils.LogDebug(TAG, "Setup successful, consuming unconsumed items and handling refunds");
                mHelper.queryInventoryAsync(false, null, mPostInitQueryListener);
            }
        });

        mInitialized = true;
        BusProvider.getInstance().post(new StoreControllerInitializedEvent());
        return true;
    }

    public void startIabServiceInBg() {
        keepIabServiceOpen = true;
        startIabHelper(null);
    }

    public void stopIabServiceInBg() {
        keepIabServiceOpen = false;
        stopIabHelper();
    }

    /**
     * Initiate the restoreTransactions process
     * @{link #storeOpen} must be called before this method or your helper will be destroyed.
     */
    public void restoreTransactions() {
        startIabHelper(new OnIabStartedListener() {
            @Override
            public void onIabStarted() {
                mHelper.queryInventoryAsync(false, null, mPostInitQueryListener);
                BusProvider.getInstance().post(new RestoreTransactionsStartedEvent());
            }
        });
    }


    /**
     * Start a purchase process with Google Play.
     *
     * @param googleMarketItem is the item to purchase. This item has to be defined EXACTLY the same in Google Play.
     * @param payload a payload to get back when this purchase is finished.
     * @throws IllegalStateException
     */
    public void buyWithGooglePlay(GoogleMarketItem googleMarketItem, String payload) throws IllegalStateException {
        SharedPreferences prefs = new ObscuredSharedPreferences(SoomlaApp.getAppContext().getSharedPreferences(StoreConfig.PREFS_NAME, Context.MODE_PRIVATE));
        String publicKey = prefs.getString(StoreConfig.PUBLIC_KEY, "");
        if (publicKey.length() == 0 || publicKey.equals("[YOUR PUBLIC KEY FROM GOOGLE PLAY]")) {
            StoreUtils.LogError(TAG, "You didn't provide a public key! You can't make purchases.");
            throw new IllegalStateException();
        }

        try {
            final Intent intent = new Intent(SoomlaApp.getAppContext(), IabActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(PROD_ID, googleMarketItem.getProductId());
            intent.putExtra(EXTRA_DATA, payload);

            startIabHelper(new OnIabStartedListener() {
                @Override
                public void onIabStarted() {
                    SoomlaApp.getAppContext().startActivity(intent);
                }
            });
        } catch(Exception e){
            StoreUtils.LogError(TAG, "(buyWithGooglePlay) Error purchasing item " + e.getMessage());
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(e.getMessage()));
        }
    }

    /**
     *  Used for internal starting of purchase with Google Play. Do *NOT* call this on your own.
     */
    // TODO: implement checking if item is already owned in google play
    private boolean buyWithGooglePlayInner(final Activity activity, final String sku, final String payload) {
        final PurchasableVirtualItem pvi;
        try {
            pvi = StoreInfo.getPurchasableItem(sku);
        } catch (VirtualItemNotFoundException e) {
            String msg = "Couldn't find a purchasable item associated with: " + sku;
            StoreUtils.LogError(TAG, msg);
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(msg));
            return false;
        }

        startIabHelper(new OnIabStartedListener() {
            @Override
            public void onIabStarted() {
                StoreUtils.LogDebug(TAG, "Trying to buy sku: " + sku);

                mHelper.launchPurchaseFlow(activity, sku, Consts.RC_REQUEST, mPurchaseFinishedListener, payload);
                BusProvider.getInstance().post(new PlayPurchaseStartedEvent(pvi));
            }
        });

        return true;
    }

    /**
     * Create a new IAB helper and set it up.
     *
     * @param onIabSetupFinishedListener is a callback that lets users to add their own implementation for when the Iab is started
     */
    private synchronized void startIabHelper(StoreController.OnIabStartedListener onIabSetupFinishedListener) {
        if(mHelper != null)
        {
            StoreUtils.LogDebug(TAG, "The helper is started. Just running the post start function.");
            if (onIabSetupFinishedListener != null) {
                onIabSetupFinishedListener.onIabStarted();
            }
            return;
        }

        StoreUtils.LogDebug(TAG, "Creating IAB helper.");
        mHelper = new IabHelper();

        StoreUtils.LogDebug(TAG, "IAB helper Starting setup.");
        mHelper.startSetup(onIabSetupFinishedListener);
    }

    /**
     * Dispose of the helper to prevent memory leaks
     */
    private synchronized void stopIabHelper() {
        if (keepIabServiceOpen) {
            StoreUtils.LogDebug(TAG, "Not stopping Iab Helper b/c the user run 'startIabServiceInBg'. Keeping it open.");
            return;
        }

        if (mHelper == null) {
            StoreUtils.LogError(TAG, "Tried to stop IAB Helper when it was null.");
            return;
        }

        if (!mHelper.isAsyncInProgress())
        {
            StoreUtils.LogDebug(TAG, "Stopping Iab helper");
            mHelper.dispose();
            mHelper = null;
            BusProvider.getInstance().post(new IabServiceStoppedEvent());
        }
        else
        {
            String msg = "Cannot close store during async process. Will be stopped when async operation is finished.";
            StoreUtils.LogDebug(TAG, msg);
        }
    }

    /* Callbacks for the IabHelper */

    /**
     * Check the state of the purchase and respond accordingly, giving the user an item,
     * throwing an error, or taking the item away and paying him back
     *
     * @param purchase
     */
    private void purchaseActionResultOk(Purchase purchase) {
        String sku = purchase.getSku();
        String developerPayload = purchase.getDeveloperPayload();

        PurchasableVirtualItem pvi;
        try {
            pvi = StoreInfo.getPurchasableItem(sku);
        } catch (VirtualItemNotFoundException e) {
            StoreUtils.LogError(TAG, "(purchaseActionResultOk - purchase or query-inventory) ERROR : Couldn't find the " +
                    " VirtualCurrencyPack OR GoogleMarketItem  with productId: " + sku +
                    ". It's unexpected so an unexpected error is being emitted.");
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent("Couldn't find the sku of a product after purchase or query-inventory."));
            return;
        }

        switch (purchase.getPurchaseState()) {
        case 0:
            StoreUtils.LogDebug(TAG, "Purchase successful.");
            BusProvider.getInstance().post(new PlayPurchaseEvent(pvi, developerPayload));

            consumePurchaseAndAcquireItem(purchase);
            break;
        case 1:
        case 2:
            StoreUtils.LogDebug(TAG, "Purchase refunded.");
            if (!StoreConfig.friendlyRefunds) {
                pvi.take(1);
            }
            BusProvider.getInstance().post(new PlayRefundEvent(pvi, developerPayload));
            break;
        }
    }

    /**
     * Post an event containing a PurchasableVirtualItem corresponding to the purchase,
     * or an unexpected error event if the item was not found.
     *
     * @param purchase
     */
    private void purchaseActionResultCancelled(Purchase purchase) {
        String sku = purchase.getSku();
        try {
            PurchasableVirtualItem v = StoreInfo.getPurchasableItem(sku);
            BusProvider.getInstance().post(new PlayPurchaseCancelledEvent(v));
        } catch (VirtualItemNotFoundException e) {
            StoreUtils.LogError(TAG, "(purchaseActionResultCancelled) ERROR : Couldn't find the " +
                    "VirtualCurrencyPack OR GoogleMarketItem  with productId: " + sku +
                    ". It's unexpected so an unexpected error is being emitted.");
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
        }
    }

    private void consumePurchaseAndAcquireItem(Purchase purchase) {
        final String sku = purchase.getSku();
        try {
            final PurchasableVirtualItem pvi = StoreInfo.getPurchasableItem(sku);
            if ( pvi instanceof NonConsumableItem ) {
                pvi.give(1);
                BusProvider.getInstance().post(new ItemPurchasedEvent(pvi));
            }
            else {
                mHelper.consumeAsync(purchase, new IabHelper.OnConsumeFinishedListener() {
                    @Override
                    public void onConsumeFinished(Purchase purchase, IabResult result) {
                        if(result.isSuccess()) {
                            pvi.give(1);
                            BusProvider.getInstance().post(new ItemPurchasedEvent(pvi));
                        } else {
                            StoreUtils.LogDebug(TAG, "Error while consuming: " + sku);
                            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(result.getMessage()));
                        }
                        stopIabHelper();
                    }
                });
            }
        } catch (VirtualItemNotFoundException e) {
            StoreUtils.LogError(TAG, "(purchaseActionResultCancelled) ERROR : Couldn't find the " +
                    "VirtualCurrencyPack OR GoogleMarketItem  with productId: " + sku +
                    ". It's unexpected so an unexpected error is being emitted.");
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
        } catch (IllegalStateException e) {
            StoreUtils.LogDebug(TAG, "Error while consuming: " + sku);
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(e.getMessage()));
        }
    }

    /**
     * Post an unexpected error event saying the purchase failed.
     * @param result
     */
    private void purchaseActionResultUnexpected(IabResult result) {
        BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(result.getMessage()));
        StoreUtils.LogError(TAG, "ERROR: Purchase failed: " + result.getMessage());
    }


    /**
     * Wait to see if the purchase succeeded, then start the consumption process.
     */
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {

            StoreUtils.LogDebug(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            if (result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_OK) {
                purchaseActionResultOk(purchase);
            } else if (result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED) {
                purchaseActionResultCancelled(purchase);
            } else {
                if (result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
                    StoreUtils.LogDebug(TAG, "Tried to buy an item that was not consumed. Trying to consume it if it's non consumable.");
                    consumePurchaseAndAcquireItem(purchase);
                }

                purchaseActionResultUnexpected(result);
            }

            stopIabHelper();
        }
    };

    /**
     * Handle incomplete purchase and refund after initialization
     */
    IabHelper.QueryInventoryFinishedListener mPostInitQueryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            StoreUtils.LogDebug(TAG, "Query inventory succeeded");
            if (result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_OK) {
                List<String> itemSkus = inventory.getAllOwnedSkus(IabHelper.ITEM_TYPE_INAPP);
                for (String sku: itemSkus) {
                    Purchase purchase = inventory.getPurchase(sku);
                    purchaseActionResultOk(purchase);
                }
            } else {
                StoreUtils.LogError(TAG, "Query inventory error: " + result.getMessage());
                purchaseActionResultUnexpected(result);
            }

            BusProvider.getInstance().post(new RestoreTransactionsEvent(true));

            stopIabHelper();
        }
    };

    /**
     *  A wrapper to access IabHelper.handleActivityResult from outside
     */
    private boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return (mHelper != null) && mHelper.handleActivityResult(requestCode, resultCode, data);
    }

    /* Singleton */
    private static StoreController sInstance = null;

    public static StoreController getInstance() {
        if (sInstance == null) {
            sInstance = new StoreController();
        }
        return sInstance;
    }

    private StoreController() {
    }


    /* Private Members */
    private static final String PROD_ID    = "PRD#ID";
    private static final String EXTRA_DATA = "EXTR#DT";

    private static final String TAG = "SOOMLA StoreController";

    private boolean mInitialized   = false;
    private boolean keepIabServiceOpen = false;

    private IabHelper mHelper;

    private abstract class OnIabStartedListener implements IabHelper.OnIabSetupFinishedListener {

        @Override
        public void onIabSetupFinished(IabResult result) {
            StoreUtils.LogDebug(TAG, "IAB helper Setup finished.");
            if (result.isFailure()) {
                String msg = "There's no connectivity with the billing service.";
                StoreUtils.LogDebug(TAG, msg);
                BusProvider.getInstance().post(new BillingNotSupportedEvent());
                stopIabHelper();
                return;
            }

            BusProvider.getInstance().post(new BillingSupportedEvent());
            BusProvider.getInstance().post(new IabServiceStartedEvent());

            onIabStarted();
        }

        public abstract void onIabStarted();
    }
    /**
     * Android In-App Billing v3 requires and activity to receive the result of the billing process.
     * This activity's job is to do just that, it also contains the white/green IAB window.  Please
     * Do not start it on your own.
     */
    public static class IabActivity extends Activity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Intent intent = getIntent();
            String productId = intent.getStringExtra(StoreController.PROD_ID);
            String payload = intent.getStringExtra(StoreController.EXTRA_DATA);

            try {
                if (!StoreController.getInstance().buyWithGooglePlayInner(this, productId, payload)) {
                    finish();
                }
            }catch (Exception e) {
                StoreUtils.LogError(TAG, "(onCreate) Error purchasing item " + e.getMessage());
                BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(e.getMessage()));
                finish();
            }
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (!StoreController.getInstance().handleActivityResult(requestCode, resultCode, data)) {
                super.onActivityResult(requestCode, resultCode, data);

                if (StoreController.getInstance().mHelper == null)
                {
                    StoreUtils.LogError(TAG, "helper is null in onActivityResult.");
                    BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
                }
            }

            finish();
        }
    }
}
