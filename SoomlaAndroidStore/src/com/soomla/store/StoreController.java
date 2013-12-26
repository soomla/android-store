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
import com.soomla.store.events.ClosingStoreEvent;
import com.soomla.store.events.ItemPurchasedEvent;
import com.soomla.store.events.OpeningStoreEvent;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
        startIabHelper(true);
        
        mInitialized = true;
        BusProvider.getInstance().post(new StoreControllerInitializedEvent());
        return true;
    }

    public void storeOpening() {
    	startIabHelper(false);
    }

    /**
     * Call this function when you close the actual store window.
     */
    public void storeClosing() {
    	//mark mStoreOpen as false, all async operations check this flag.
        mStoreOpen = false;
        stopIabHelper();
    }

    /**
     * Create a new IAB helper and set it up.
     *
     * @param queryInventory if we should query the inventory after setup.
     */
    private void startIabHelper(final boolean queryInventory) {
        // Setup IabHelper
        mLock.lock();
        if(mHelper != null)
        {
        	StoreUtils.LogDebug(TAG, "An IAB helper is existed.");
        	BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
        	mLock.unlock();
        	return;
        }
        
        StoreUtils.LogDebug(TAG, "Creating IAB helper.");
        mHelper = new IabHelper();
    	BusProvider.getInstance().post(new OpeningStoreEvent());
        mLock.unlock();
    	
        // Start the setup and call the listener when the setup is over
        StoreUtils.LogDebug(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                StoreUtils.LogDebug(TAG, "Setup finished.");
                if (result.isFailure()) {
                    StoreUtils.LogDebug(TAG, "There's no connectivity with the billing service.");
                    BusProvider.getInstance().post(new BillingNotSupportedEvent());
                    stopIabHelper();
                    return;
                }

                BusProvider.getInstance().post(new BillingSupportedEvent());
                mStoreOpen = true;
                
                if (queryInventory) {
                    StoreUtils.LogDebug(TAG, "Setup successful, consuming unconsumed items and handling refunds");
                    mHelper.queryInventoryAsync(false, null, mPostInitQueryListener);
                }
            }
        });
    }

    /**
     * Dispose of the helper to prevent memory leaks
     */
    private void stopIabHelper() {
    	mLock.lock();
        if (mHelper != null && !mHelper.isAsyncInProgress())
        {
        	mHelper.dispose();
        	mHelper = null;
            BusProvider.getInstance().post(new ClosingStoreEvent());
        }
        else
        {
        	StoreUtils.LogDebug(TAG, "Cannot close store during async process.");
        	BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
        }
        mLock.unlock();
    }

    /**
     * Start a purchase process with Google Play.
     *
     * @param googleMarketItem is the item to purchase. This item has to be defined EXACTLY the same in Google Play.
     * @param payload a payload to get back when this purchase is finished.
     * @throws VirtualItemNotFoundException 
     */
    public void buyWithGooglePlay(GoogleMarketItem googleMarketItem, String payload, PurchasableVirtualItem v) throws IllegalStateException {
    	if(mHelper == null)
    	{
    		StoreUtils.LogDebug(TAG, "Billing service is not connected.");
    		BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
    		return;
    	}
    	
        BusProvider.getInstance().post(new PlayPurchaseStartedEvent(v));
        
        SharedPreferences prefs = new ObscuredSharedPreferences(SoomlaApp.getAppContext().getSharedPreferences(StoreConfig.PREFS_NAME, Context.MODE_PRIVATE));
        String publicKey = prefs.getString(StoreConfig.PUBLIC_KEY, "");
        if (publicKey.length() == 0 || publicKey.equals("[YOUR PUBLIC KEY FROM GOOGLE PLAY]")) {
            StoreUtils.LogError(TAG, "You didn't provide a public key! You can't make purchases.");
            throw new IllegalStateException();
        }
        
        try {
	        Intent intent = new Intent(SoomlaApp.getAppContext(), IabActivity.class);
	        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        intent.putExtra(PROD_ID, googleMarketItem.getProductId());
	        intent.putExtra(EXTRA_DATA, payload);
	        SoomlaApp.getAppContext().startActivity(intent);
        } catch(Exception e){
        	StoreUtils.LogError(TAG, "Error purchasing item " + e.getMessage());
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(e.getMessage()));
        }
    }

    /**
     * Initiate the restoreTransactions process
     * @{link #storeOpen} must be called before this method or your helper will be destroyed.
     */
    public void restoreTransactions() {
    	// the inventory is automatically sync with Google Play Service after initialization
    	// allow user manually query inventory by calling this function
    	
    	BusProvider.getInstance().post(new RestoreTransactionsStartedEvent());
    	if(mHelper == null) {
    		StoreUtils.LogDebug(TAG, "Billing service is not connected.");
    		BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
    		return;
    	}
        
    	mHelper.queryInventoryAsync(false, null, mPostInitQueryListener);
    	return;
    }

    /**
     * Check if transactions were restored already.
     * @return if transactions were restored already.
     */
    public boolean transactionsAlreadyRestored() {
    	// the inventory is automatically sync with Google Play Service after initialization
    	return true;
    }

    /**
     * Check the state of the purchase and respond accordingly, giving the user an item,
     * throwing an error, or taking the item away and paying him back
     *
     * @param purchase the purchase data as received by the helper
     */
    private void syncPurchaseState(Purchase purchase, boolean isAfterPurchase) throws VirtualItemNotFoundException {
    	String sku = purchase.getSku();
        String developerPayload = purchase.getDeveloperPayload();
        PurchasableVirtualItem vItem = StoreInfo.getPurchasableItem(sku);
        boolean isNonConsumable =  vItem instanceof NonConsumableItem;
        boolean exist = StoreInventory.nonConsumableItemExists(vItem.getItemId());
               
        // give item immediately after successful purchase, then sync purchase state later.
        if( isAfterPurchase )
    	{
        	vItem.give(1);
        	BusProvider.getInstance().post(new ItemPurchasedEvent(vItem));
        	if(isNonConsumable)
        		BusProvider.getInstance().post(new PlayPurchaseEvent(vItem, developerPayload));
    	}
        // sync nonconsumable item after re-installation.
        else if(isNonConsumable && !exist && purchase.getPurchaseState() == 0)
        {
        	vItem.give(1);
        	BusProvider.getInstance().post(new ItemPurchasedEvent(vItem));
        	BusProvider.getInstance().post(new PlayPurchaseEvent(vItem, developerPayload));
        }
        	
        // consume all consumable items
    	if( !isNonConsumable ) 
    	{
    		try
    		{
    			mHelper.consume(purchase);
    			StoreUtils.LogDebug(TAG, "Purchase successful.");
            	BusProvider.getInstance().post(new PlayPurchaseEvent(vItem, developerPayload));
    		}
        	catch (IabException e)
        	{
        		StoreUtils.LogDebug(TAG, "Error while consuming: " + sku);
        		BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(e.getMessage()));
        	}
    	}

    	// handle if an existing item is cancelled or refunded
    	if(purchase.getPurchaseState() != 0 && !StoreConfig.friendlyRefunds)
    	{
    		vItem.take(1);
            BusProvider.getInstance().post(new PlayRefundEvent(vItem, purchase.getDeveloperPayload()));
    	}
    }

    /* Callbacks for the IabHelper */

    /**
     * Wait to see if the purchase succeeded, then start the consumption process.
     */
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            StoreUtils.LogDebug(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            String sku = purchase.getSku();
            String packageName = SoomlaApp.getAppContext().getPackageName();
            
			try {
				if (result.isSuccess()) {
					syncPurchaseState(purchase, true);
				}
				else if (result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED) {
					BusProvider.getInstance().post(
							new PlayPurchaseCancelledEvent(StoreInfo.getPurchasableItem(sku)));
				}
				else {
					BusProvider.getInstance().post(
							new UnexpectedStoreErrorEvent(result.getMessage()));
				}
			} catch (VirtualItemNotFoundException e) {
				StoreUtils.LogError( TAG,
					"ERROR : Couldn't find the "
					+ packageName
					+ " VirtualCurrencyPack OR GoogleMarketItem  with productId: "
					+ sku
					+ ". It's unexpected so an unexpected error is being emitted.");
				BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
			}
            
            if(!mStoreOpen)
            	stopIabHelper();
        }
    };

    /**
     * Handle incomplete purchase and refund after initialization
     */
    IabHelper.QueryInventoryFinishedListener mPostInitQueryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (result.isFailure()) {
            	String err = "Query inventory error: " + result.getMessage();
                StoreUtils.LogDebug(TAG, err);
                BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(err));
                return;
            }
            StoreUtils.LogDebug(TAG, "Query inventory succeeded");

            List<String> itemSkus = inventory.getAllOwnedSkus(IabHelper.ITEM_TYPE_INAPP);
            for (String sku: itemSkus) {
            	Purchase purchase = inventory.getPurchase(sku);
            	try {
            		syncPurchaseState(purchase, false);
            	} catch (VirtualItemNotFoundException e) {
            		//if a product id don't exist in store info, simply ignore it.
            	}
            }
            
            BusProvider.getInstance().post(new RestoreTransactionsEvent(true));
            
            if(!mStoreOpen)
            	stopIabHelper();
        }
    };

    /**
     *  A wrapper to access IabHelper.handleActivityResult from outside
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
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
    public static final String PROD_ID    = "PRD#ID";
    public static final String EXTRA_DATA = "EXTR#DT";

    private static final String TAG = "SOOMLA StoreController";

    private boolean mInitialized   = false;
    private boolean mStoreOpen     = false;

    private IabHelper mHelper;

    private Lock mLock = new ReentrantLock();

    /**
     * Android In-App Billing v3 requires and activity to receive the result of the billing process.
     * This activity's job is to do just that, it also contains the white/green IAB window.  Please
     * Do not start it on your own.
     */
    public static class IabActivity extends Activity {
    	private boolean created = false;
    	
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            if(created)
            {
            	finish();
            	return;
            }
            
            created = true;
            Intent intent = getIntent();
            String productId = intent.getStringExtra(StoreController.PROD_ID);
            String payload = intent.getStringExtra(StoreController.EXTRA_DATA);

            try {
                StoreController sc = StoreController.getInstance();
                sc.mHelper.launchPurchaseFlow( this, productId, Consts.RC_REQUEST, sc.mPurchaseFinishedListener, payload);

            } catch (Exception e) {
                StoreUtils.LogError(TAG, "Error purchasing item " + e.getMessage());
                BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(e.getMessage()));
                finish();
            }
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
			if (StoreController.getInstance().mHelper == null)
			{
				StoreUtils.LogError(TAG, "helper is null in onActivityResult.");
				BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
				super.onActivityResult(requestCode, resultCode, data);
				finish();
				return;
			}

			if (!StoreController.getInstance().mHelper.handleActivityResult(requestCode, resultCode, data))
				super.onActivityResult(requestCode, resultCode, data);				

			finish();
        }
    }
}
