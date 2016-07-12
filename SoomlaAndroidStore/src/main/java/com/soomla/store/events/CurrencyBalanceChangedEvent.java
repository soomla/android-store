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

package com.soomla.store.events;

import com.soomla.events.SoomlaEvent;

/**
 * This event is fired when the balance of a specific <code>VirtualCurrency</code> has changed.
 *
 * Real Game Example:
 *  Example Inventory: { currency_coin: 100, green_hat: 3, blue_hat: 5 }
 *  Suppose your user buys something for 10 "Coins".
 *  His/her new balance of currency_coin will now be 90.
 *  A <code>CurrencyBalanceChangedEvent</code> is fired.
 */
public class CurrencyBalanceChangedEvent extends SoomlaEvent {

    /**
     * Constructor
     *
     * @param itemId
     * @param balance
     * @param amountAdded
     */
    public CurrencyBalanceChangedEvent(String itemId, int balance, int amountAdded) {
        super(null);
        mItemId = itemId;
        mBalance = balance;
        mAmountAdded = amountAdded;
    }

    public CurrencyBalanceChangedEvent(String itemId, int balance, int amountAdded, Object sender) {
        super(sender);
        mItemId = itemId;
        mBalance = balance;
        mAmountAdded = amountAdded;
    }


    /** Setters and Getters */

    public String getCurrencyItemId() {
        return mItemId;
    }

    public int getBalance() {
        return mBalance;
    }

    public int getAmountAdded() {
        return mAmountAdded;
    }


    /** Private Members */

    private String mItemId;

    private int mBalance;

    private int mAmountAdded;
}
