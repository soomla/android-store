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

package com.soomla.store.billing;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import com.soomla.SoomlaUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This abstract class should be implemented by any billing service plugin to android-store.
 *
 * This class is the main class used by the implementations of the IIabService implementations.
 *
 * Based on an implementation by Google's Bruno Oliveira (Google).
 */
public abstract class IabHelper {

    /**
     * Determines if the setup process has already been called and completed.
     *
     * @return the status of the setup process of the service.
     */
    public boolean isSetupDone() {
        return mSetupDone;
    }

    /**
     * Starts the setup process. This will start up the setup process asynchronously.
     * You will be notified through the listener when the setup process is complete.
     * This method is safe to call from a UI thread.
     *
     * @param listener The listener to notify when the setup process is complete.
     */
    public synchronized void startSetup(final OnIabSetupFinishedListener listener) {
        if (mSetupDone)
        {
            SoomlaUtils.LogDebug(TAG, "The purchasing observer is already started. Just running the post listener.");

            if (listener != null) {
                listener.onIabSetupFinished(new IabResult(IabResult.BILLING_RESPONSE_RESULT_OK, "Setup successful."));
            }
            return;
        }

        if (mSetupFinishedListeners == null) {
            mSetupFinishedListeners = new ArrayList<OnIabSetupFinishedListener>();
        }
        mSetupFinishedListeners.add(listener);

        if (!mSetupStarted) {
            mSetupStarted = true;

            // Connection to IAB service
            SoomlaUtils.LogDebug(TAG, "Starting in-app billing setup.");

            startSetupInner();
        }
    }

    /**
     * Initiate the UI flow for an in-app purchase. Call this method to initiate an in-app purchase,
     * which will involve bringing up the Google Play screen. The calling activity will be paused while
     * the user interacts with Google Play, and the result will be delivered via the activity's
     * {@link android.app.Activity#onActivityResult} method, at which point you must call
     * this object's {@link #} method to continue the purchase flow. This method
     * MUST be called from the UI thread of the Activity.
     *
     * @param act The calling activity.
     * @param itemType ITEM_TYPE_INAPP or ITEM_TYPE_SUBS
     * @param sku The sku of the item to purchase.
     * @param listener The listener to notify when the purchase process finishes
     * @param extraData Extra data (developer payload), which will be returned with the purchase data
     *     when the purchase completes. This extra data will be permanently bound to that purchase
     *     and will always be returned when the purchase is queried.
     */
    public void launchPurchaseFlow(Activity act, String itemType, String sku,
                                   OnIabPurchaseFinishedListener listener, String extraData) {
        checkSetupDoneAndThrow("launchPurchaseFlow");
        flagStartAsync("launchPurchaseFlow");

        mPurchaseListener = listener;
        mLastOperationSKU = sku;
        launchPurchaseFlowInner(act, itemType, sku, extraData);
    }

    /**
     * Initiate the UI flow for an in-app purchase. Call this method to initiate an in-app purchase,
     * which will involve bringing up the Google Play screen. The calling activity will be paused while
     * the user interacts with Google Play, and the result will be delivered via the activity's
     * {@link android.app.Activity#onActivityResult} method, at which point you must call
     * this object's {@link #} method to continue the purchase flow. This method
     * MUST be called from the UI thread of the Activity.
     *
     * @param act The calling activity.
     * @param sku The sku of the item to purchase.
     * @param listener The listener to notify when the purchase process finishes
     * @param extraData Extra data (developer payload), which will be returned with the purchase data
     *     when the purchase completes. This extra data will be permanently bound to that purchase
     *     and will always be returned when the purchase is queried.
     */
    public void launchPurchaseFlow(Activity act, String sku,
                                   OnIabPurchaseFinishedListener listener, String extraData) {
        checkSetupDoneAndThrow("launchPurchaseFlow");
        flagStartAsync("launchPurchaseFlow");

        mPurchaseListener = listener;
        mLastOperationSKU = sku;
        launchPurchaseFlowInner(act, ITEM_TYPE_INAPP, sku, extraData);
    }

    /**
     * Initiates the restore purchases process. All purchases that weren't consumed will be fetched
     * and returned to the user.
     * This method is asynchronous and will invoke the listener when the process is finished.
     *
     * @param listener The listener to notify when the restore purchases process finishes
     */
    public void restorePurchasesAsync(RestorePurchasessFinishedListener listener) {
        checkSetupDoneAndThrow("restorePurchases");
        flagStartAsync("restore purchases");

        mRestorePurchasessFinishedListener = listener;
        restorePurchasesAsyncInner();
    }

    /**
     * Initiates the fetching of items details. This will fetch the price, title, description or
     * any other information associated with your items in the market.
     * This method is asynchronous and will invoke the listener when the process is finished.
     *
     * @param listener The listener to notify when the fetching of items details finishes
     */
    public void fetchSkusDetailsAsync(List<String> skus, final FetchSkusDetailsFinishedListener listener) {
        checkSetupDoneAndThrow("fetchSkusDetails");
        flagStartAsync("fetch skus details");

        mFetchSkusDetailsFinishedListener = listener;
        fetchSkusDetailsAsyncInner(skus);
    }

    /**
     * Determines if an asynchronous process is in progress.
     *
     * @return true if an asynchronous process is in progress.
     */
    public boolean isAsyncInProgress() {
        return mAsyncInProgress;
    }


    /** Listeners **/

    /**
     * Callback for setup process. This listener's {@link #onIabSetupFinished} method is called
     * when the setup process is complete.
     */
    public interface OnIabSetupFinishedListener {
        /**
         * Called to notify that setup is complete.
         *
         * @param result The result of the setup process.
         */
        public void onIabSetupFinished(IabResult result);
    }

    /**
     * Callback that notifies when a purchase is finished.
     */
    public interface OnIabPurchaseFinishedListener {
        /**
         * Called to notify that an in-app purchase finished. If the purchase was successful,
         * then the sku parameter specifies which item was purchased. If the purchase failed,
         * the sku and extraData parameters may or may not be null, depending on how far the purchase
         * process went.
         *
         * @param result The result of the purchase.
         * @param info The purchase information (null if purchase failed)
         */
        public void onIabPurchaseFinished(IabResult result, IabPurchase info);
    }

    /**
     * Callback for restore purchases.
     */
    public interface RestorePurchasessFinishedListener {
        /**
         * Called to notify that an restore purchases operation completed.
         *
         * @param result The result of the operation.
         * @param inv The inventory.
         */
        public void onRestorePurchasessFinished(IabResult result, IabInventory inv);
    }

    /**
     * Callback for fetching of skus details.
     */
    public interface FetchSkusDetailsFinishedListener {
        /**
         * Called to notify that an fetch skus details operation completed.
         *
         * @param result The result of the operation.
         * @param inv The inventory.
         */
        public void onFetchSkusDetailsFinished(IabResult result, IabInventory inv);
    }


    /** Public Consts **/

    // Item types
    public static final String ITEM_TYPE_INAPP = "inapp";
    public static final String ITEM_TYPE_SUBS = "subs";



    /** Protected Functions **/

    /**
     * see startSetup
     */
    protected abstract void startSetupInner();

    /**
     * see launchPurchaseFlow
     */
    protected abstract void launchPurchaseFlowInner(Activity act, String itemType, String sku, String extraData);

    /**
     * see restorePurchasesAsync
     */
    protected abstract void restorePurchasesAsyncInner();

    /**
     * see fetchSkusDetailsAsync
     */
    protected abstract void fetchSkusDetailsAsyncInner(final List<String> skus);

    /**
     * This will be called when the helper is disposed.
     */
    protected void dispose() {
        mSetupDone = false;
        mSetupFinishedListeners = null;
    }


    /** restore transactions and refresh market items handlers **/

    /**
     * This is a utility function for the classes that inherits IabHelper to call when a restore
     * purchases process succeeds.
     *
     * @param inventory the inventory that was just restored.
     */
    protected void restorePurchasesSuccess(final IabInventory inventory) {

        // make sure to end the async operation...
        flagEndAsync();

        if (mRestorePurchasessFinishedListener != null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                public void run() {
                    IabResult result = new IabResult(IabResult.BILLING_RESPONSE_RESULT_OK, "IabInventory restore successful.");
                    mRestorePurchasessFinishedListener.onRestorePurchasessFinished(
                            result, inventory);
                    mRestorePurchasessFinishedListener = null;
                }
            });
        }
    }

    /**
     * This is a utility function for the classes that inherits IabHelper to call when a restore
     * purchases process fails.
     *
     * @param result the result containing the cause of the failure.
     */
    protected void restorePurchasesFailed(final IabResult result) {

        flagEndAsync();

        if (mRestorePurchasessFinishedListener != null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mRestorePurchasessFinishedListener.onRestorePurchasessFinished(result, null);
                    mRestorePurchasessFinishedListener = null;
                }
            });
        }
    }

    /**
     * This is a utility function for the classes that inherits IabHelper to call when a fetching
     * of items details succeeds.
     *
     * @param inventory the inventory that was just fetched.
     */
    protected void fetchSkusDetailsSuccess(final IabInventory inventory) {

        // make sure to end the async operation...
        flagEndAsync();

        if (mFetchSkusDetailsFinishedListener != null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                public void run() {
                    IabResult result = new IabResult(IabResult.BILLING_RESPONSE_RESULT_OK, "IabInventory fetch details successful.");
                    mFetchSkusDetailsFinishedListener.onFetchSkusDetailsFinished(
                            result, inventory);
                    mFetchSkusDetailsFinishedListener = null;
                }
            });
        }

    }

    /**
     * This is a utility function for the classes that inherits IabHelper to call when the fetching
     * process fails.
     *
     * @param result the result containing the cause of the failure.
     */
    protected void fetchSkusDetailsFailed(final IabResult result) {

        flagEndAsync();

        if (mFetchSkusDetailsFinishedListener != null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mFetchSkusDetailsFinishedListener.onFetchSkusDetailsFinished(result, null);
                    mFetchSkusDetailsFinishedListener = null;
                }
            });
        }
    }

    /** purchase flow handlers **/

    /**
     * This is a utility function for the classes that inherits IabHelper to call when the purchase
     * process fails.
     *
     * @param result the result containing the cause of the failure.
     * @param purchase the purchase that just failed.
     */
    protected void purchaseFailed(final IabResult result, final IabPurchase purchase) {

        // make sure to end the async operation...
        flagEndAsync();

        final Handler handler = new Handler(Looper.getMainLooper());
        final OnIabPurchaseFinishedListener purchaseListener = IabHelper.this.mPurchaseListener;
        if (purchaseListener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    purchaseListener.onIabPurchaseFinished(result, purchase);
                }
            });
        }

    }

    /**
     * This is a utility function for the classes that inherits IabHelper to call when a purchase
     * process succeeds.
     *
     * @param purchase the purchase that just succeeded.
     */
    protected void purchaseSucceeded(final IabPurchase purchase) {

        // make sure to end the async operation...
        flagEndAsync();

        final OnIabPurchaseFinishedListener purchaseListener = IabHelper.this.mPurchaseListener;
        if (purchaseListener != null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    purchaseListener.onIabPurchaseFinished(
                            new IabResult(IabResult.BILLING_RESPONSE_RESULT_OK, "Success"),
                            purchase);
                }
            });
        }
    }



    /** setup related checkers and handlers **/

    /**
     * This is a utility function for the classes that inherits IabHelper to call when a setup
     * process succeeds.
     */
    protected void setupSuccess() {
        mSetupDone = true;
        if (mSetupFinishedListeners != null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            for (final OnIabSetupFinishedListener listener : mSetupFinishedListeners) {
                handler.post(new Runnable() {


                    @Override
                    public void run() {
                        listener.onIabSetupFinished(new IabResult(IabResult.BILLING_RESPONSE_RESULT_OK, "Setup successful."));
                    }
                });
            }
        }
    }

    /**
     * This is a utility function for the classes that inherits IabHelper to call when the setup
     * process fails.
     *
     * @param result the result containing the cause of the failure.
     */
    protected void setupFailed(final IabResult result) {
        mSetupDone = false;
        if (mSetupFinishedListeners != null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            for (final OnIabSetupFinishedListener listener : mSetupFinishedListeners) {
                handler.post(new Runnable() {


                    @Override
                    public void run() {
                        listener.onIabSetupFinished(result);
                    }
                });
            }
        }
    }

    /**
     * Checks that setup was done; if not, throws an exception.
     *
     * @param operation is the operation that was just trying to be made.
     */
    protected void checkSetupDoneAndThrow(String operation) {
        if (!isSetupDone()) {
            SoomlaUtils.LogError(TAG, "Illegal state for operation (" + operation + "): IAB helper is not set up.");
            throw new IllegalStateException("IAB helper is not set up. Can't perform operation: " + operation);
        }
    }



    /** Async related functions **/

    /**
     * Sets necessary params when an async process starts.
     *
     * @param operation the async process's name.
     */
    protected synchronized void flagStartAsync(String operation) {
        if (mAsyncInProgress) throw new IllegalStateException("Can't start async operation (" +
                operation + ") because another async operation(" + mAsyncOperation + ") is in progress.");
        mAsyncInProgress = true;
        mAsyncOperation = operation;
        SoomlaUtils.LogDebug(TAG, "Starting async operation: " + operation);
    }

    /**
     * Sets necessary params when an async process ends.
     */
    protected synchronized void flagEndAsync() {
        SoomlaUtils.LogDebug(TAG, "Ending async operation: " + mAsyncOperation);
        mAsyncOperation = "";
        mAsyncInProgress = false;
    }

    /**
     * Sets the the mode of the current environment (mainly for Amazon's server side verification
     * but has been put here for future use).
     *
     * @param rvsProductionMode the mode of the current environment. If production then the value
     *                          will be "true".
     */
    protected void setRvsProductionMode(boolean rvsProductionMode) {
        mRvsProductionMode = rvsProductionMode;
    }

    /** Protected Members **/
    protected String mLastOperationSKU;


    /** Private Members **/

    private static String TAG = "SOOMLA PurchaseObserver";


    // This tells us if we're on production or sandbox environment (for server validation)
    private boolean mRvsProductionMode = false;
    // Is setup done?
    private boolean mSetupDone = false;
    // Is setup started?
    private boolean mSetupStarted = false;
    // Is an asynchronous operation in progress?
    // (only one at a time can be in progress)
    private boolean mAsyncInProgress = false;
    // (for logging/debugging)
    // if mAsyncInP!?*.java;!?*.form;!?*.class;!?*.groovy;!?*.scala;!?*.flex;!?*.kt;!?*.cljrogress == true, what asynchronous operation is in progress?
    private String mAsyncOperation = "";
    // The listeners registered on setup, which we have to call back when
    // the purchase finishes
    private List<OnIabSetupFinishedListener> mSetupFinishedListeners;
    // The listener registered on launchPurchaseFlow, which we have to call back when
    // the purchase finishes.
    // We only keep one and not a list b/c the purchase operation can only run one-at-a-time
    private OnIabPurchaseFinishedListener mPurchaseListener;
    // The listener registered on restore purchases, which we have to call back when
    // the restore process finishes.
    private RestorePurchasessFinishedListener mRestorePurchasessFinishedListener;
    // The listener registered on restore subscriptions, which we have to call back when
    // the restore process finishes.
    private RestorePurchasessFinishedListener mRestoreSubscriptionsFinishedListener;

    // The listener registered on restore purchases, which we have to call back when
    // the restore process finishes.
    private FetchSkusDetailsFinishedListener mFetchSkusDetailsFinishedListener;

}
