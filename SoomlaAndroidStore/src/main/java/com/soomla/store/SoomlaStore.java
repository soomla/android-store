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

package com.soomla.store;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import com.soomla.BusProvider;
import com.soomla.SoomlaApp;
import com.soomla.SoomlaConfig;
import com.soomla.SoomlaUtils;
import com.soomla.store.billing.IIabService;
import com.soomla.store.billing.IabHelper;
import com.soomla.store.billing.IabCallbacks;
import com.soomla.store.billing.IabException;
import com.soomla.store.billing.IabPurchase;
import com.soomla.store.billing.IabSkuDetails;
import com.soomla.store.data.StorageManager;
import com.soomla.store.data.StoreInfo;
import com.soomla.store.domain.MarketItem;
import com.soomla.store.domain.PurchasableVirtualItem;
import com.soomla.store.domain.VirtualItem;
import com.soomla.store.domain.virtualGoods.VirtualGood;
import com.soomla.store.events.BillingNotSupportedEvent;
import com.soomla.store.events.BillingSupportedEvent;
import com.soomla.store.events.IabServiceStartedEvent;
import com.soomla.store.events.ItemPurchasedEvent;
import com.soomla.store.events.MarketItemsRefreshFailedEvent;
import com.soomla.store.events.MarketItemsRefreshFinishedEvent;
import com.soomla.store.events.MarketItemsRefreshStartedEvent;
import com.soomla.store.events.MarketPurchaseCancelledEvent;
import com.soomla.store.events.MarketPurchaseEvent;
import com.soomla.store.events.MarketPurchaseStartedEvent;
import com.soomla.store.events.MarketRefundEvent;
import com.soomla.store.events.RestoreTransactionsFinishedEvent;
import com.soomla.store.events.RestoreTransactionsStartedEvent;
import com.soomla.store.events.SoomlaStoreInitializedEvent;
import com.soomla.store.events.UnexpectedStoreErrorEvent;
import com.soomla.store.events.VerificationStartedEvent;
import com.soomla.store.exceptions.VirtualItemNotFoundException;
import com.soomla.store.purchaseTypes.PurchaseWithMarket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class holds the basic assets needed to operate the Store.
 * You can use it to perform any operation related to the mobile store.
 *
 * This is the only class you need to initialize in order to use the SOOMLA SDK.
 *
 * To properly work with this class you must initialize it with the {@link #initialize} method.
 */
public class SoomlaStore {

    public static final String VERSION = "3.6.21";

    /**
     * Initializes the SOOMLA SDK.
     * This initializer also initializes {@link StoreInfo}.
     *
     * @param storeAssets the definition of your application specific assets.
     */
    public boolean initialize(IStoreAssets storeAssets) {
        if (mInitialized) {
            String err = "SoomlaStore is already initialized. You can't initialize it twice!";
            handleErrorResult(UnexpectedStoreErrorEvent.ErrorCode.GENERAL, err);
            return false;
        }

        SoomlaUtils.LogDebug(TAG, "SoomlaStore Initializing ...");

        if (!loadBillingService()) return false;

        StoreInfo.setStoreAssets(storeAssets);

        refreshInventory();

        mInitialized = true;
        BusProvider.getInstance().post(new SoomlaStoreInitializedEvent());
        return true;
    }

    /**
     * Starts in-app billing service in background.
     */
    public void startIabServiceInBg() {
        if (mInAppBillingService == null) {
            SoomlaUtils.LogError(TAG, "Billing service is not loaded. Can't invoke startIabServiceInBg.");
            return;
        }

        mInAppBillingService.startIabServiceInBg(new IabCallbacks.IabInitListener() {

            @Override
            public void success(boolean alreadyInBg) {
                if (!alreadyInBg) {
                    notifyIabServiceStarted();
                    SoomlaUtils.LogDebug(TAG, "Successfully started billing service in background.");
                } else {
                    SoomlaUtils.LogDebug(TAG, "Couldn't start billing service in background. "
                            + "Was already started.");
                }
            }

            @Override
            public void fail(String message) {
                SoomlaUtils.LogError(TAG, "Couldn't start billing service in background. error: "
                        + message);
            }
        });
    }

    /**
     * Stops in-app billing service in background.
     *
     * IMPORTANT: This function is not supported in all billing providers (Amazon for example).
     */
    public void stopIabServiceInBg() {
        if (mInAppBillingService == null) {
            SoomlaUtils.LogError(TAG, "Billing service is not loaded. Can't invoke stopIabServiceInBg.");
            return;
        }

        mInAppBillingService.stopIabServiceInBg(new IabCallbacks.IabInitListener() {

            @Override
            public void success(boolean alreadyInBg) {
                SoomlaUtils.LogDebug(TAG, "Successfully stopped billing service in background.");
            }

            @Override
            public void fail(String message) {
                SoomlaUtils.LogError(TAG, "Couldn't stop billing service in background. error: "
                        + message);
            }
        });
    }

    /**
     * Restoring old purchases for the current user (device).
     */
    public void restoreTransactions() {
        if (mInAppBillingService == null) {
            SoomlaUtils.LogError(TAG, "Billing service is not loaded. Can't invoke restoreTransactions.");
            return;
        }

        mInAppBillingService.initializeBillingService(
                new IabCallbacks.IabInitListener() {

                    @Override
                    public void success(boolean alreadyInBg) {
                        if (!alreadyInBg) {
                            notifyIabServiceStarted();
                        }

                        SoomlaUtils.LogDebug(TAG,
                                "Setup successful, restoring purchases");

                        IabCallbacks.OnRestorePurchasesListener restorePurchasesListener = new IabCallbacks.OnRestorePurchasesListener() {
                            @Override
                            public void success(List<IabPurchase> purchases) {
                                SoomlaUtils.LogDebug(TAG, "Transactions restored");

                                if (purchases.size() > 0) {

                                    if (SoomlaConfig.logDebug) {
                                        String ownedSkus = "";
                                        for (IabPurchase purchase : purchases) {
                                            ownedSkus += purchase.getSku() + " / ";
                                        }
                                        SoomlaUtils.LogDebug(TAG, "Got owned items: " + ownedSkus);
                                    }

                                    handleSuccessfulPurchases(purchases, true, new HandleSuccessfulPurchasesFinishedHandler() {
                                        @Override
                                        public void onFinished() {

                                            // Restore transactions always finished successfully even if
                                            // something wrong happened when handling a specific item.

                                            BusProvider.getInstance().post(
                                                    new RestoreTransactionsFinishedEvent(true));
                                        }
                                    });
                                } else {
                                    BusProvider.getInstance().post(
                                            new RestoreTransactionsFinishedEvent(true));
                                }
                            }

                            @Override
                            public void fail(String message) {
                                BusProvider.getInstance().post(new RestoreTransactionsFinishedEvent(false));
                                handleErrorResult(UnexpectedStoreErrorEvent.ErrorCode.GENERAL, message);
                            }

                            @Override
                            public void verificationStarted(List<IabPurchase> purchases) {
                                handleVerificationStarted(purchases);
                            }
                        };

                        IabCallbacks.OnRestorePurchasesListener restoreSubscriptionsListener = new IabCallbacks.OnRestorePurchasesListener() {
                            @Override
                            public void success(List<IabPurchase> purchases) {

                                // collect subscription ids list
                                List<String> subscriptionIds = new ArrayList<String>();
                                for (IabPurchase purchase : purchases) {
                                    subscriptionIds.add(purchase.getSku());
                                }

                                // collect subscriptionVG list
                                List<VirtualGood> subscriptions = new ArrayList<VirtualGood>();
                                for (VirtualGood good : StoreInfo.getGoods()) {
                                    if ((good.getPurchaseType() instanceof PurchaseWithMarket) && ((PurchaseWithMarket)good.getPurchaseType()).isSubscription()) {
                                        subscriptions.add(good);
                                    }
                                }

                                // give unset subscriptions and take expired
                                for (VirtualGood subscription : subscriptions) {
                                    String productId = ((PurchaseWithMarket)subscription.getPurchaseType()).getMarketItem().getProductId();
                                    if (subscriptionIds.contains(productId)) {
                                        // TODO: is here should be 1 to give? Maybe current item has not only just 0/1 state
                                        subscription.give(1, false);
                                    } else {
                                        try {
                                            subscription.take(StoreInventory.getVirtualItemBalance(subscription.getItemId()), false);
                                        }
                                        catch (VirtualItemNotFoundException ex) {
                                            // possibly unreachable block
                                        }
                                    }
                                }
                                // TODO: Should we notify user about repaired or expired subscriptions?
                            }

                            @Override
                            public void fail(String message) {
                                SoomlaUtils.LogDebug(TAG, "Subscriptions restoring failed: " + message);
                            }

                            @Override
                            public void verificationStarted(List<IabPurchase> purchases) {
                                // should we do it in subscription restoring? possibly it should be empty
                            }
                        };

                        // no events like in restore purchases - keep subscription restoring silent for end-user

                        try {
                            mInAppBillingService.restorePurchasesAsync(restorePurchasesListener);
                        } catch (IllegalStateException ex) {
                            SoomlaUtils.LogError(TAG, "Can't proceed with restorePurchases. error: " + ex.getMessage());
                            restorePurchasesListener.fail("Can't proceed with restorePurchases. error: " + ex.getMessage());
                        }
                    }

                    @Override
                    public void fail(String message) {
                        reportIabInitFailure(message);
                    }
                }
        );
    }

    /**
     * Queries the store for the details for all of the game's market items by product ids.
     * Here we just call the private function without refreshing market items details.
     */
    public void refreshMarketItemsDetails() {
        refreshMarketItemsDetails(false);
    }

    /**
     * Queries the store for the details for all of the game's market items by product ids.
     * This operation will "fill" up the MarketItem objects with the information you provided in
     * the developer console including: localized price (as string), title and description.
     *
     * @param followedByRestoreTransactions determines weather we should perform a restore
     *                                      transactions operation right after a refresh ends.
     */
    private void refreshMarketItemsDetails(final boolean followedByRestoreTransactions) {
        if (mInAppBillingService == null) {
            SoomlaUtils.LogError(TAG, "Billing service is not loaded. Can't invoke refreshMarketItemsDetails.");
            return;
        }

        mInAppBillingService.initializeBillingService(
                new IabCallbacks.IabInitListener() {

                    @Override
                    public void success(boolean alreadyInBg) {
                        if (!alreadyInBg) {
                            notifyIabServiceStarted();
                        }
                        SoomlaUtils.LogDebug(TAG,
                                "Setup successful, refreshing market items details");

                        IabCallbacks.OnFetchSkusDetailsListener fetchSkusDetailsListener =
                                new IabCallbacks.OnFetchSkusDetailsListener() {

                                    @Override
                                    public void success(List<IabSkuDetails> skuDetails) {
                                        SoomlaUtils.LogDebug(TAG, "Market items details refreshed");

                                        List<MarketItem> marketItems = new ArrayList<MarketItem>();
                                        if (skuDetails.size() > 0) {
                                            List<VirtualItem> virtualItems = new ArrayList<VirtualItem>();

                                            for (IabSkuDetails iabSkuDetails : skuDetails) {
                                                String productId = iabSkuDetails.getSku();
                                                String price = iabSkuDetails.getPrice();
                                                String title = iabSkuDetails.getTitle();
                                                String desc = iabSkuDetails.getDescription();
                                                String currencyCode = iabSkuDetails.getCurrencyCode();
                                                long priceMicros = iabSkuDetails.getPriceMicros();

                                                SoomlaUtils.LogDebug(TAG, "Got item details: " +
                                                        "\ntitle:\t" + iabSkuDetails.getTitle() +
                                                        "\nprice:\t" + iabSkuDetails.getPrice() +
                                                        "\nproductId:\t" + iabSkuDetails.getSku() +
                                                        "\ndesc:\t" + iabSkuDetails.getDescription());

                                                try {
                                                    PurchasableVirtualItem pvi = StoreInfo.
                                                            getPurchasableItem(productId);
                                                    MarketItem mi = ((PurchaseWithMarket)
                                                            pvi.getPurchaseType()).getMarketItem();
                                                    mi.setMarketInformation(price, title, desc, currencyCode, priceMicros);

                                                    marketItems.add(mi);
                                                    virtualItems.add(pvi);
                                                } catch (VirtualItemNotFoundException e) {
                                                    String msg = "(refreshInventory) Couldn't find a "
                                                            + "purchasable item associated with: " + productId;
                                                    SoomlaUtils.LogError(TAG, msg);
                                                }
                                            }

                                            StoreInfo.save(virtualItems);
                                        }
                                        BusProvider.getInstance().post(new MarketItemsRefreshFinishedEvent(marketItems));

                                        if (followedByRestoreTransactions) {
                                            restoreTransactions();
                                        }
                                    }

                                    @Override
                                    public void fail(String message) {
                                        SoomlaUtils.LogError(TAG, "Market items details failed to refresh " + message);

                                        BusProvider.getInstance().post(new MarketItemsRefreshFailedEvent(message));

                                        if (followedByRestoreTransactions) {
                                            restoreTransactions();
                                        }
                                    }
                                };

                        final List<String> purchasableProductIds = StoreInfo.getAllProductIds();

                        BusProvider.getInstance().post(new MarketItemsRefreshStartedEvent());

                        try {
                            mInAppBillingService.fetchSkusDetailsAsync(purchasableProductIds, fetchSkusDetailsListener);
                        } catch (IllegalStateException ex) {
                            SoomlaUtils.LogError(TAG, "Can't proceed with fetchSkusDetails. error: " + ex.getMessage());
                            fetchSkusDetailsListener.fail("Can't proceed with fetchSkusDetails. error: " + ex.getMessage());

                            if (followedByRestoreTransactions) {
                                restoreTransactions();
                            }
                        }
                    }

                    @Override
                    public void fail(String message) {
                        reportIabInitFailure(message);
                    }
                }
        );
    }

    /**
     * This runs restoreTransactions followed by market items refresh.
     * There are docs that explains restoreTransactions and refreshMarketItemsDetails on the actual
     * functions in this file.
     */
    public void refreshInventory() {
        refreshMarketItemsDetails(true);
    }

    /**
     * Starts a purchase process in the market.
     *
     * @param marketItem The item to purchase - this item has to be defined EXACTLY the same in
     *                   the market
     * @param payload A payload to get back when this purchase is finished.
     * @throws IllegalStateException
     */
    public void buyWithMarket(final MarketItem marketItem, final String payload) throws IllegalStateException {
        buyWithMarket(marketItem, false, payload);
    }

    /**
     * Starts a purchase process in the market.
     *
     * @param marketItem The item to purchase - this item has to be defined EXACTLY the same in
     *                   the market
     * @param isSubscription determines if subscription is purchasing
     * @param payload A payload to get back when this purchase is finished.
     * @throws IllegalStateException
     */
    public void buyWithMarket(final MarketItem marketItem, final boolean isSubscription, final String payload) throws IllegalStateException {
        if (mInAppBillingService == null) {
            SoomlaUtils.LogError(TAG, "Billing service is not loaded. Can't invoke buyWithMarket.");
            return;
        }

        final PurchasableVirtualItem pvi;
        try {
            pvi = StoreInfo.getPurchasableItem(marketItem.getProductId());
        } catch (VirtualItemNotFoundException e) {
            SoomlaUtils.LogError(TAG, "Couldn't find a purchasable item associated with: " + marketItem.getProductId());
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(UnexpectedStoreErrorEvent.ErrorCode.PURCHASE_FAIL));
            return;
        }

        mInAppBillingService.initializeBillingService
                (new IabCallbacks.IabInitListener() {

                    @Override
                    public void success(boolean alreadyInBg) {
                        if (!alreadyInBg) {
                            notifyIabServiceStarted();
                        }

                        IabCallbacks.OnPurchaseListener purchaseListener =
                                new IabCallbacks.OnPurchaseListener() {

                                    @Override
                                    public void success(IabPurchase purchase) {
                                        handleSuccessfulPurchase(purchase, false);
                                    }

                                    @Override
                                    public void cancelled(IabPurchase purchase) {
                                        handleCancelledPurchase(purchase);
                                    }

                                    @Override
                                    public void alreadyOwned(IabPurchase purchase) {
                                        String sku = purchase.getSku();
                                        SoomlaUtils.LogDebug(TAG, "Tried to buy an item that was not" +
                                                " consumed (maybe it's an already owned " +
                                                "non-consumable). productId: " + sku);

                                        try {
                                            PurchasableVirtualItem pvi = StoreInfo.getPurchasableItem(sku);
                                            consumeIfConsumable(purchase, pvi);

                                            if (StoreInfo.isItemNonConsumable(pvi)) {
                                                SoomlaUtils.LogDebug(TAG,
                                                        "(alreadyOwned) the user tried to " +
                                                        "buy a non-consumable that was already " +
                                                        "owned. itemId: " + pvi.getItemId() +
                                                        "    productId: " + sku);
                                                BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(UnexpectedStoreErrorEvent.ErrorCode.PURCHASE_FAIL));
                                            }
                                        } catch (VirtualItemNotFoundException e) {
                                            SoomlaUtils.LogError(TAG,
                                                    "(alreadyOwned) ERROR : Couldn't find the " +
                                                    "VirtualCurrencyPack with productId: " + sku +
                                                    ". It's unexpected so an unexpected error is being emitted.");
                                            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(UnexpectedStoreErrorEvent.ErrorCode.PURCHASE_FAIL));
                                        }
                                    }

                                    @Override
                                    public void fail(String message) {
                                        handleErrorResult(UnexpectedStoreErrorEvent.ErrorCode.PURCHASE_FAIL, message);
                                    }

                                    @Override
                                    public void verificationStarted(List<IabPurchase> purchases) {
                                        handleVerificationStarted(purchases);
                                    }
                                };

                        BusProvider.getInstance().post(new MarketPurchaseStartedEvent(pvi, getInAppBillingService().shouldVerifyPurchases()));

                        try {
                            if (isSubscription) {
                                mInAppBillingService.launchPurchaseFlow(IabHelper.ITEM_TYPE_SUBS, marketItem.getProductId(), purchaseListener, payload);
                            } else {
                                mInAppBillingService.launchPurchaseFlow(IabHelper.ITEM_TYPE_INAPP, marketItem.getProductId(), purchaseListener, payload);
                            }
                        } catch (IllegalStateException ex) {
                            SoomlaUtils.LogError(TAG, "Can't proceed with launchPurchaseFlow. error: " + ex.getMessage());
                            purchaseListener.fail("Can't proceed with launchPurchaseFlow. error: " + ex.getMessage());
                        }

                    }

                    @Override
                    public void fail(String message) {
                        reportIabInitFailure(message);
                    }

                });
    }


    /**
     * Fetches the current billing service.
     *
     * @return the current billing service.
     */
    public IIabService getInAppBillingService() {
        return mInAppBillingService;
    }

    /**
     * This function loads the billing service that was set in the AndroidManifest.xml
     * This is automatically ran when you initialize SoomlaStore and you're usually not supposed to
     * run it manually.
     *
     * @return true if succeeds
     */
    public boolean loadBillingService() {
        if (mInAppBillingService == null) {
            SoomlaUtils.LogDebug(TAG, "Searching for the attached IAB Service.");

            Class<?> aClass = null;
            aClass = tryFetchIabService();
            if (aClass == null) {
                String err = "You don't have a billing service attached. " +
                        "Decide which billing service you want, add it to AndroidManifest.xml " +
                        "and add its jar to the path.";
                handleErrorResult(UnexpectedStoreErrorEvent.ErrorCode.GENERAL, err);
                return false;
            }

            try {
                SoomlaUtils.LogDebug(TAG, "IAB Service found. Initializing it.");
                mInAppBillingService = (IIabService) aClass.newInstance();
            } catch (Exception e) {
                String err = "Couldn't instantiate IIabService class. Something's totally wrong here.";
                handleErrorResult(UnexpectedStoreErrorEvent.ErrorCode.GENERAL, err);
                return false;
            }
        }
        return true;
    }


    /*==================== Common callbacks for success \ failure \ finish ====================*/

    /**
     * Notifies the user that the billing service is supported and started.
     */
    private void notifyIabServiceStarted() {
        BusProvider.getInstance().post(new BillingSupportedEvent());
        BusProvider.getInstance().post(new IabServiceStartedEvent());
    }

    /**
     * Reports that in-app billing service initialization failed.
     *
     * @param message error message.
     */
    private void reportIabInitFailure(String message) {
        String msg = "There's no connectivity with the billing service. error: " + message;
        SoomlaUtils.LogDebug(TAG, msg);
        BusProvider.getInstance().post(new BillingNotSupportedEvent());
        //BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(msg));
    }


    private interface HandleSuccessfulPurchasesFinishedHandler {
        void onFinished();
    }

    private void handleSuccessfulPurchases(List<IabPurchase> purchases, boolean isRestoring, HandleSuccessfulPurchasesFinishedHandler handler) {
        for (IabPurchase purchase : purchases) {
            handleSuccessfulPurchase(purchase, isRestoring);
        }

        if (handler != null) {
            handler.onFinished();
        }

    }

    /**
     * Checks the state of the purchase and responds accordingly, giving the user an item,
     * throwing an error, or taking the item away and paying the user back.
     *
     * @param purchase purchase whose state is to be checked.
     */
    private void handleSuccessfulPurchase(IabPurchase purchase, boolean isRestoring) {
        String sku = purchase.getSku();

        PurchasableVirtualItem pvi;
        try {
            pvi = StoreInfo.getPurchasableItem(sku);
        } catch (VirtualItemNotFoundException e) {
            SoomlaUtils.LogError(TAG, "(handleSuccessfulPurchase - purchase or query-inventory) "
                    + "ERROR : Couldn't find the " +
                    " VirtualCurrencyPack OR MarketItem  with productId: " + sku +
                    ". It's unexpected so an unexpected error is being emitted.");
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(
                    UnexpectedStoreErrorEvent.ErrorCode.PURCHASE_FAIL));
            return;
        }

        switch (purchase.getPurchaseState()) {
            case 0: {
                if (purchase.isServerVerified()) {
                    this.finalizeTransaction(purchase, pvi, isRestoring);
                } else {
                    BusProvider.getInstance().post(
                            new UnexpectedStoreErrorEvent(
                                    purchase.getVerificationErrorCode() != null?
                                            purchase.getVerificationErrorCode() :
                                            UnexpectedStoreErrorEvent.ErrorCode.GENERAL));
                }

                break;
            }
            case 1:
            case 2:
                SoomlaUtils.LogDebug(TAG, "IabPurchase refunded.");
                if (!StoreConfig.friendlyRefunds) {
                    pvi.take(1);
                }
                BusProvider.getInstance().post(new MarketRefundEvent(pvi, purchase.getDeveloperPayload()));
                break;
        }
    }

    /**
     * Handles a cancelled purchase by either posting an event containing a
     * <code>PurchasableVirtualItem</code> corresponding to the given purchase, or an unexpected
     * error event if the item was not found.
     *
     * @param purchase cancelled purchase to handle.
     */
    private void handleCancelledPurchase(IabPurchase purchase) {
        String sku = purchase.getSku();
        try {
            PurchasableVirtualItem v = StoreInfo.getPurchasableItem(sku);
            BusProvider.getInstance().post(new MarketPurchaseCancelledEvent(v));
        } catch (VirtualItemNotFoundException e) {
            SoomlaUtils.LogError(TAG, "(purchaseActionResultCancelled) ERROR : Couldn't find the "
                    + "VirtualCurrencyPack OR MarketItem  with productId: " + sku
                    + ". It's unexpected so an unexpected error is being emitted.");
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
        }
    }

    /**
     * Posts an event about the start of verification for the purchase, or an unexpected
     * error event if the item was not found.
     *
     * @param purchases List of purchases to handle.
     */
    private void handleVerificationStarted(List<IabPurchase> purchases) {
        for (IabPurchase purchase : purchases) {
            String sku = purchase.getSku();
            try {
                PurchasableVirtualItem v = StoreInfo.getPurchasableItem(sku);
                BusProvider.getInstance().post(new VerificationStartedEvent(v));
            } catch (VirtualItemNotFoundException e) {
                SoomlaUtils.LogError(TAG, "(purchaseActionResultCancelled) ERROR : Couldn't find the "
                        + "VirtualCurrencyPack OR MarketItem  with productId: " + sku
                        + ". It's unexpected so an unexpected error is being emitted.");
                BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
            }
        }
    }

    /**
     * Consumes the given purchase, or writes error message to log if unable to consume
     *
     * @param purchase purchase to be consumed
     */
    private void consumeIfConsumable(IabPurchase purchase, PurchasableVirtualItem pvi) {
        try {
            if (!StoreInfo.isItemNonConsumable(pvi)) {
                mInAppBillingService.consume(purchase);
            }
        } catch (IabException e) {
            SoomlaUtils.LogDebug(TAG, "Error while consuming: itemId: " + pvi.getItemId() +
                    "   productId: " + purchase.getSku());
            SoomlaUtils.LogError(TAG, e.getMessage());
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(UnexpectedStoreErrorEvent.ErrorCode.PURCHASE_FAIL));
        }
    }

    /**
     * Fetches the associated billing service according to the meta-data tag in AndroidManifest.xml.
     *
     * @return the Main class of the associated billing service or null if none was found.
     */
    private Class<?> tryFetchIabService() {
        String iabServiceClassName;
        try {
            ApplicationInfo ai = SoomlaApp.getAppContext().getPackageManager().getApplicationInfo(
                    SoomlaApp.getAppContext().getPackageName(), PackageManager.GET_META_DATA);
            assert ai.metaData != null;
            iabServiceClassName = ai.metaData.getString("billing.service");
        } catch (Exception e) {
            SoomlaUtils.LogError(TAG, "Failed to load billing service from AndroidManifest.xml, NullPointer: " + e.getMessage());
            return null;
        }

        Class<?> aClass = null;
        try {
            SoomlaUtils.LogDebug(TAG, "Trying to find " + iabServiceClassName);
            aClass = Class.forName("com.soomla.store.billing." + iabServiceClassName);
        } catch (ClassNotFoundException e) {
            SoomlaUtils.LogDebug(TAG, "Failed finding " + iabServiceClassName);
        }
        return aClass;
    }

    /**
     * Posts an unexpected error event saying the purchase failed.
     *
     * @param errorCode
     * @param message error message.
     */
    private void handleErrorResult(UnexpectedStoreErrorEvent.ErrorCode errorCode, String message) {
        BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(errorCode));
        SoomlaUtils.LogError(TAG, "ERROR: SoomlaStore failure: " + message);
    }

    private void finalizeTransaction(IabPurchase purchase, PurchasableVirtualItem pvi, boolean isRestoring) {
        SoomlaUtils.LogDebug(TAG, "IabPurchase successful. Finalizing transaction");

        // if the purchasable item is non-consumable and it already exists then we
        // don't fire any events.
        // fixes: https://github.com/soomla/unity3d-store/issues/192
        // TODO: update on the issue in github
        if (StoreInfo.isItemNonConsumable(pvi)) {
            if (StorageManager.getVirtualItemStorage(pvi).getBalance(pvi.getItemId()) == 1) {
                return;
            }
        }


        String developerPayload = purchase.getDeveloperPayload();
        final String token = purchase.getToken();
        final String orderId = purchase.getOrderId();
        final String originalJson = purchase.getOriginalJson();
        final String signature = purchase.getSignature();
        final String userId = purchase.getUserId();

        BusProvider.getInstance().post(new MarketPurchaseEvent(pvi, isRestoring, developerPayload, new HashMap<String, String>() {{
                    put("token", token);
                    put("orderId", orderId);
                    put("originalJson", originalJson);
                    put("signature", signature);
                    put("userId", userId);
                }}, null));

        pvi.give(1);
        BusProvider.getInstance().post(new ItemPurchasedEvent(pvi.getItemId(), isRestoring, developerPayload));

        consumeIfConsumable(purchase, pvi);
    }

    /* Singleton */
    private static SoomlaStore sInstance = null;

    /**
     * Retrieves the singleton instance of <code>SoomlaStore</code>
     *
     * @return singleton instance of <code>SoomlaStore</code>
     */
    public static SoomlaStore getInstance() {
        if (sInstance == null) {
            sInstance = new SoomlaStore();
        }
        return sInstance;
    }

    /**
     * Constructor
     */
    private SoomlaStore() {
        BusProvider.getInstance().register(this);
    }

    /* Private Members */

    private static final String TAG = "SOOMLA SoomlaStore"; //used for Log messages
    private boolean mInitialized = false;
    private IIabService mInAppBillingService;

}
