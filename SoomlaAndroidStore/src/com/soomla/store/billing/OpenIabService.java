package com.soomla.store.billing;

/**
 * Created by akarimova on 05.02.14.
 */

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import com.soomla.store.SoomlaApp;
import com.soomla.store.StoreConfig;
import com.soomla.store.StoreController;
import com.soomla.store.StoreUtils;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;

public class OpenIabService {
    private static final int RC_REQUEST = 10001;
    /* Private Members */
    private static final String TAG = "SOOMLA OpenIabService";
    private OpenIabHelper mHelper;
    private boolean keepIabServiceOpen = false;
    private BroadcastReceiver broadcastReceiver;

    public void initializeBillingService(final IabCallbacks.IabInitListener iabListener) {

        // Set up helper for the first time, querying and synchronizing inventory
        startIabHelper(new OnIabSetupFinishedListener(iabListener));
    }

    public void stopBillingService(IabCallbacks.IabInitListener iabListener) {
        stopIabHelper(iabListener);
    }

    public void startIabServiceInBg(IabCallbacks.IabInitListener iabListener) {
        keepIabServiceOpen = true;
        startIabHelper(new OnIabSetupFinishedListener(iabListener));
    }

    public void stopIabServiceInBg(IabCallbacks.IabInitListener iabListener) {
        keepIabServiceOpen = false;
        stopIabHelper(iabListener);
    }

    /**
     * A wrapper to access IabHelper.handleActivityResult from outside
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return /*isIabServiceInitialized() && */mHelper.handleActivityResult(requestCode, resultCode, data);
    }

    public void queryInventoryAsync(boolean querySkuDetails,
                                    List<String> moreSkus,
                                    IabCallbacks.OnQueryInventoryListener queryInventoryListener) {

        mHelper.queryInventoryAsync(querySkuDetails, moreSkus, new QueryInventoryFinishedListener(queryInventoryListener));
    }


    public boolean isIabServiceInitialized() {
        return mHelper != null;
    }

    public void consume(Purchase purchase) throws org.onepf.oms.appstore.googleUtils.IabException {
        mHelper.consume(purchase);
    }

    public void launchPurchaseFlow(Activity act,
                                   String sku,
                                   final IabCallbacks.OnPurchaseListener purchaseListener,
                                   String extraData) {
        mHelper.launchPurchaseFlow(act, sku, RC_REQUEST, new IabHelper.OnIabPurchaseFinishedListener() {

            @Override
            public void onIabPurchaseFinished(org.onepf.oms.appstore.googleUtils.IabResult result, Purchase purchase) {
                /**
                 * Wait to see if the purchase succeeded, then start the consumption process.
                 */
                StoreUtils.LogDebug(TAG, "IabPurchase finished: " + result + ", purchase: " + purchase);
                if (result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_OK) {

                    purchaseListener.success(purchase);
                } else if (result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED) {

                    purchaseListener.cancelled(purchase);
                } else if (result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {

                    purchaseListener.alreadyOwned(purchase);
                } else {

                    purchaseListener.fail(result.getMessage());
                }

                stopIabHelper(null);
            }
        }, extraData);
    }


    /*====================   Private Utility Methods   ====================*/

    /**
     * Create a new IAB helper and set it up.
     *
     * @param onIabSetupFinishedListener is a callback that lets users to add their own implementation for when the Iab is started
     */
    private synchronized void startIabHelper(final OnIabSetupFinishedListener onIabSetupFinishedListener) {
        if (isIabServiceInitialized()) {
            StoreUtils.LogDebug(TAG, "The helper is started. Just running the post start function.");

            if (onIabSetupFinishedListener != null && onIabSetupFinishedListener.getIabInitListener() != null) {
                onIabSetupFinishedListener.getIabInitListener().success(true);
            }
            return;
        }

        StoreUtils.LogDebug(TAG, "Creating IAB helper.");
        final OpenIabHelper.Options billingOptions = StoreConfig.billingOptions;
        billingOptions.verifyMode = OpenIabHelper.Options.VERIFY_SKIP;
        final Context appContext = SoomlaApp.getAppContext();
        if (StoreConfig.hasSamsungSKUs()) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mHelper = new OpenIabHelper(StoreController.IabActivity.instance, billingOptions);
                    StoreUtils.LogDebug(TAG, "IAB helper Starting setup.");
                    mHelper.startSetup(onIabSetupFinishedListener);
                    appContext.unregisterReceiver(broadcastReceiver);
                }
            };
            appContext.registerReceiver(broadcastReceiver, new IntentFilter(StoreController.IabActivity.OPENPF_ACTION_ACTIVITY_STARTED));
            Intent intent = new Intent(appContext, StoreController.IabActivity.class);
            intent.setAction(StoreController.IabActivity.OPENPF_ACTION_START_ACTIVITY);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(intent);
        } else {
            mHelper = new OpenIabHelper(appContext, billingOptions);
            StoreUtils.LogDebug(TAG, "IAB helper Starting setup.");
            mHelper.startSetup(onIabSetupFinishedListener);
        }
    }

    /**
     * Dispose of the helper to prevent memory leaks
     */
    private synchronized void stopIabHelper(IabCallbacks.IabInitListener iabInitListener) {
        if (keepIabServiceOpen) {
            String msg = "Not stopping OpenIabService Service b/c the user run 'startIabServiceInBg'. Keeping it open.";
            if (iabInitListener != null) {
                iabInitListener.fail(msg);
            } else {
                StoreUtils.LogDebug(TAG, msg);
            }
            return;
        }

        if (mHelper == null) {
            String msg = "Tried to stop OpenIabService Service when it was null.";
            if (iabInitListener != null) {
                iabInitListener.fail(msg);
            } else {
                StoreUtils.LogDebug(TAG, msg);
            }
            return;
        }

//        if (!mHelper.isAsyncInProgress()) {
        StoreUtils.LogDebug(TAG, "Stopping OpenIabService Service");
        mHelper.dispose();
        mHelper = null;
        if (iabInitListener != null) {
            iabInitListener.success(true);
        }
//        } else {
//            String msg = "Cannot stop Google Service during async process. Will be stopped when async operation is finished.";
//            if (iabInitListener != null) {
//                iabInitListener.fail(msg);
//            } else {
//                StoreUtils.LogDebug(TAG, msg);
//            }
//        }
    }


    /**
     * Handle incomplete purchase and refund after initialization
     */
    private class QueryInventoryFinishedListener implements IabHelper.QueryInventoryFinishedListener {


        private IabCallbacks.OnQueryInventoryListener mQueryInventoryListener;

        public QueryInventoryFinishedListener(IabCallbacks.OnQueryInventoryListener queryInventoryListener) {
            this.mQueryInventoryListener = queryInventoryListener;
        }

        @Override
        public void onQueryInventoryFinished(org.onepf.oms.appstore.googleUtils.IabResult result, Inventory inventory) {
            StoreUtils.LogDebug(TAG, "Query inventory succeeded");
            if (result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_OK) {
                List<String> itemSkus = inventory.getAllOwnedSkus(IabHelper.ITEM_TYPE_INAPP);
                for (String sku : itemSkus) {
                    Purchase purchase = inventory.getPurchase(sku);
                    if (this.mQueryInventoryListener != null) {
                        this.mQueryInventoryListener.success(purchase);
                    }
                }
            } else {
                StoreUtils.LogError(TAG, "Query inventory error: " + result.getMessage());
                if (this.mQueryInventoryListener != null) this.mQueryInventoryListener.fail(result.getMessage());
            }

            stopIabHelper(null);
        }
    }


    private class OnIabSetupFinishedListener implements IabHelper.OnIabSetupFinishedListener {

        private IabCallbacks.IabInitListener mIabInitListener;

        public IabCallbacks.IabInitListener getIabInitListener() {
            return mIabInitListener;
        }

        public OnIabSetupFinishedListener(IabCallbacks.IabInitListener iabListener) {
            this.mIabInitListener = iabListener;
        }

        @Override
        public void onIabSetupFinished(org.onepf.oms.appstore.googleUtils.IabResult result) {
            StoreUtils.LogDebug(TAG, "IAB helper Setup finished.");
            if (result.isFailure()) {
                if (mIabInitListener != null) mIabInitListener.fail(result.getMessage());
                return;
            }
            if (mIabInitListener != null) mIabInitListener.success(false);
        }
    }

}
