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


package com.soomla.example;

import android.os.Handler;
import android.widget.Toast;
import com.soomla.BusProvider;
import com.soomla.SoomlaApp;
import com.soomla.SoomlaConfig;
import com.soomla.store.events.BillingNotSupportedEvent;
import com.soomla.store.events.BillingSupportedEvent;
import com.soomla.store.events.CurrencyBalanceChangedEvent;
import com.soomla.store.events.GoodBalanceChangedEvent;
import com.soomla.store.events.GoodEquippedEvent;
import com.soomla.store.events.GoodUnEquippedEvent;
import com.soomla.store.events.IabServiceStartedEvent;
import com.soomla.store.events.IabServiceStoppedEvent;
import com.soomla.store.events.ItemPurchaseStartedEvent;
import com.soomla.store.events.ItemPurchasedEvent;
import com.soomla.store.events.MarketPurchaseCancelledEvent;
import com.soomla.store.events.MarketPurchaseEvent;
import com.soomla.store.events.MarketPurchaseStartedEvent;
import com.soomla.store.events.MarketRefundEvent;
import com.soomla.store.events.RestoreTransactionsFinishedEvent;
import com.soomla.store.events.RestoreTransactionsStartedEvent;
import com.soomla.store.events.SoomlaStoreInitializedEvent;
import com.soomla.store.events.UnexpectedStoreErrorEvent;
import com.squareup.otto.Subscribe;

/**
 * This class contains functions that receive events that they are subscribed to. Annotating with
 * <code>@subscribe</code> before each function lets the function receive a notification when an
 * event has occurred.
 */
public class ExampleEventHandler {

    /**
     * Constructor method.
     * In order to receive events, this class instance needs to register with the bus.
     *
     * @param handler event handler
     * @param activityI StoreExampleActivity
     */
    public ExampleEventHandler(Handler handler, StoreExampleActivity activityI){
        mHandler = handler;
        mActivityI = activityI;
        BusProvider.getInstance().register(this);
    }

    /**
     * Listens for the given <code>marketPurchaseEvent</code> that was fired. Upon receiving such
     * an event, if the debugging setting is on, displays a message stating the name of the market
     * item that was purchased.
     * Note: The item in the given <code>marketPurchasedEvent</code> is an item that was purchased
     * via the Market (with money, not with virtual items).
     *
     * @param marketPurchaseEvent the "market purchase" event that was fired
     */
    @Subscribe
    public void onMarketPurchase(MarketPurchaseEvent marketPurchaseEvent) {
        showToastIfDebug(marketPurchaseEvent.PurchasableVirtualItem.getName()
                + " was just purchased");
    }

    /**
     * Listens for the given <code>marketRefundEvent</code> that was fired. Upon receiving such an
     * event, if the debugging setting is on, displays a message stating the name of the item that
     * was refunded.
     *
     * @param marketRefundEvent the "market refund" event that was fired
     */
    @Subscribe
    public void onMarketRefund(MarketRefundEvent marketRefundEvent) {
        showToastIfDebug(marketRefundEvent.getPurchasableVirtualItem().getName()
                + " was just refunded");
    }

    /**
     * Listens for the given <code>itemPurchasedEvent</code> that was fired. Upon receiving such an
     * event, if the debugging setting is on, displays a message stating the name of the virtual
     * item that was purchased.
     *
     * @param itemPurchasedEvent the "item purchased" event that was fired
     */
    @Subscribe
    public void onVirtualItemPurchased(ItemPurchasedEvent itemPurchasedEvent) {
        showToastIfDebug(itemPurchasedEvent.getItemId()
                + " was just purchased. The payload was: " + itemPurchasedEvent.getPayload());
    }

    /**
     * Listens for the given <code>virtualGoodEquippedEvent</code> that was fired. Upon receiving
     * such an event, if the debugging setting is on, displays a message stating the name of the
     * virtual good that was equipped.
     *
     * @param virtualGoodEquippedEvent the "virtual good equipped" event that was fired
     */
    @Subscribe
    public void onVirtualGoodEquipped(GoodEquippedEvent virtualGoodEquippedEvent) {
        showToastIfDebug(virtualGoodEquippedEvent.getGoodItemId() + " was just equipped");
    }

    /**
     * Listens for the given <code>virtualGoodUnEquippedEvent</code> that was fired. Upon receiving
     * such an event, if the debugging setting is on, displays a message stating the name of the
     * virtual good that was unequipped.
     *
     * @param virtualGoodUnEquippedEvent the virtual good unequipped event that was fired
     */
    @Subscribe
    public void onVirtualGoodUnequipped(GoodUnEquippedEvent virtualGoodUnEquippedEvent) {
        showToastIfDebug(virtualGoodUnEquippedEvent.getGoodItemId() + " was just unequipped");
    }

    /**
     * Listens for the given <code>billingSupportedEvent</code> that was fired. Upon receiving such
     * an event, if the debugging setting is on, displays a message stating that billing is
     * supported.
     *
     * @param billingSupportedEvent the billing supported event that was fired
     */
    @Subscribe
    public void onBillingSupported(BillingSupportedEvent billingSupportedEvent) {
        showToastIfDebug("Billing is supported");
    }

    /**
     * Listens for the given <code>billingNotSupportedEvent</code> that was fired. Upon receiving
     * such an event, if the debugging setting is on, displays a message stating that billing is
     * not supported.
     *
     * @param billingNotSupportedEvent the billing not supported event that was fired
     */
    @Subscribe
    public void onBillingNotSupported(BillingNotSupportedEvent billingNotSupportedEvent) {
        showToastIfDebug("Billing is not supported");
    }

    /**
     * Listens for the given <code>marketPurchaseStartedEvent</code> that was fired. Upon receiving
     * such an event, if the debugging setting is on, displays a message stating the name of the
     * market item that is starting to be purchased.
     * Note: The item in the given <code>marketPurchaseStartedEvent</code> is an item that is being
     * purchased via the Market (with money, not with virtual items).
     *
     * @param marketPurchaseStartedEvent the market purchase started event that was fired
     */
    @Subscribe
    public void onMarketPurchaseStarted(MarketPurchaseStartedEvent marketPurchaseStartedEvent) {
        showToastIfDebug("Market purchase started for: "
                + marketPurchaseStartedEvent.getPurchasableVirtualItem().getName()
                + (marketPurchaseStartedEvent.isFraudProtection()? " with " : " without ") + "Fraud Protection");
    }

    /**
     * Listens for the given <code>marketPurchaseCancelledEvent</code> that was fired. Upon
     * receiving such an event, if the debugging setting is on, displays a message stating the name
     * of the market item that is being cancelled.
     * Note: The item in the given <code>marketPurchaseCancelledEvent</code> is an item
     * that was purchased via the Market (with money, not with virtual items).
     *
     * @param marketPurchaseCancelledEvent the market purchase cancelled event that was fired
     */
    @Subscribe
    public void onMarketPurchaseCancelled(
            MarketPurchaseCancelledEvent marketPurchaseCancelledEvent) {
        showToastIfDebug("Market purchase cancelled for: "
                + marketPurchaseCancelledEvent.getPurchasableVirtualItem().getName());
    }

    /**
     * Listens for the given <code>itemPurchaseStartedEvent</code> that was fired. Upon receiving
     * such an event, if the debugging setting is on, displays a message stating the name of the
     * item that is starting to be purchased.
     *
     * @param itemPurchaseStartedEvent the item purchase started event that was fired
     */
    @Subscribe
    public void onItemPurchaseStarted(ItemPurchaseStartedEvent itemPurchaseStartedEvent) {
        showToastIfDebug("Item purchase started for: "
                + itemPurchaseStartedEvent.getItemId());
    }

    /**
     * Listens for the given <code>unexpectedStoreErrorEvent</code> that was fired. Upon receiving
     * such an event, if the debugging setting is on, displays a message stating that an error has
     * occurred.
     *
     * @param unexpectedStoreErrorEvent the unexpected store error event that was fired
     */
    @Subscribe
    public void onUnexpectedErrorInStore(UnexpectedStoreErrorEvent unexpectedStoreErrorEvent) {
        showToastIfDebug("Unexpected error occurred!");
    }

    /**
     * Listens for the given <code>iabServiceStartedEvent</code> that was fired. Upon receiving
     * such an event, if the debugging setting is on, displays a message stating that the service
     * has started.
     *
     * @param iabServiceStartedEvent the in-app billing service started event that was fired
     */
    @Subscribe
    public void onIabServiceStarted(IabServiceStartedEvent iabServiceStartedEvent) {
        showToastIfDebug("Iab Service started");
    }

    /**
     * Listens for the given <code>iabServiceStoppedEvent</code> that was fired. Upon receiving
     * such an event, if the debugging setting is on, displays a message stating that the service
     * has stopped.
     *
     * @param iabServiceStoppedEvent the in-app billing service stopped event that was fired
     */
    @Subscribe
    public void onIabServiceStopped(IabServiceStoppedEvent iabServiceStoppedEvent) {
        showToastIfDebug("Iab Service stopped");
    }

    /**
     * Listens for the given <code>currencyBalanceChangedEvent</code> that was fired. Upon
     * receiving such an event, if the debugging setting is on, displays a message stating which
     * currency's balance has changed, and what its new balance is.
     *
     * @param currencyBalanceChangedEvent the currency balance changed event that was fired
     */
    @Subscribe
    public void onCurrencyBalanceChanged(CurrencyBalanceChangedEvent currencyBalanceChangedEvent) {
        showToastIfDebug("(currency) " + currencyBalanceChangedEvent.getCurrencyItemId()
                + " balance was changed to " + currencyBalanceChangedEvent.getBalance() + ".");
    }

    /**
     * Listens for the given <code>goodBalanceChangedEvent</code> that was fired. Upon receiving
     * such an event, if the debugging setting is on, displays a message stating which good's
     * balance has changed, and what its new balance is.
     *
     * @param goodBalanceChangedEvent the good balance changed event that was fired
     */
    @Subscribe
    public void onGoodBalanceChanged(GoodBalanceChangedEvent goodBalanceChangedEvent) {
        showToastIfDebug("(good) " + goodBalanceChangedEvent.getGoodItemId()
                + " balance was changed to " + goodBalanceChangedEvent.getBalance() + ".");
    }

    /**
     * Listens for the given <code>restoreTransactionsFinishedEvent</code> that was fired. Upon
     * receiving such an event, if the debugging setting is on, displays a message stating that
     * <code>restoreTransactions</code> finished successfully or unsuccessfully.
     *
     * @param restoreTransactionsFinishedEvent the restore transactions finished event that was
     *                                         fired
     */
    @Subscribe
    public void onRestoreTransactionsFinished(
            RestoreTransactionsFinishedEvent restoreTransactionsFinishedEvent) {
        showToastIfDebug("restoreTransactions: "
                + restoreTransactionsFinishedEvent.isSuccess() + ".");
    }

    /**
     * Listens for the given <code>restoreTransactionsStartedEvent</code> that was fired. Upon
     * receiving such an event, if the debugging setting is on, displays a message stating that
     * <code>restoreTransactions</code> has started.
     *
     * @param restoreTransactionsStartedEvent the restore transactions started event that was fired
     */
    @Subscribe
    public void onRestoreTransactionsStarted(
            RestoreTransactionsStartedEvent restoreTransactionsStartedEvent) {
        showToastIfDebug("restoreTransactions Started");
    }

    /**
     * Listens for the given <code>SoomlaStoreInitializedEvent</code> that was fired. Upon
     * receiving such an event, if the debugging setting is on, displays a message stating that
     * <code>SoomlaStore</code> has been initialized.
     *
     * @param SoomlaStoreInitializedEvent the store controller initialized event that was fired
     */
    @Subscribe
    public void onSoomlaStoreInitialized(
            SoomlaStoreInitializedEvent SoomlaStoreInitializedEvent) {
        String [] s = {"no_ads"};
        // SoomlaStore.getInstance().getItemDetails(s);
        showToastIfDebug("SoomlaStoreInitialized");
    }

    /**
     * Posts to Handler if <code>StoreConfig</code>'s <code>logDebug</code> is set to true.
     * Enqueues a <code>Runnable</code> object to be called by the message queue when it is
     * received. The <code>Runnable</code> displays a debug message.
     *
     * @param msg message to be displayed as a part of the <code>Runnable</code>'s <code>run</code> method.
     */
    private void showToastIfDebug(final String msg) {
        if (SoomlaConfig.logDebug){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(SoomlaApp.getAppContext(), msg, Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }
    }


    /** Private Members */

    private Handler mHandler;

    private StoreExampleActivity mActivityI;

}
