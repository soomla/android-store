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

package com.soomla.store.data;


import com.soomla.BusProvider;
import com.soomla.store.events.CurrencyBalanceChangedEvent;

/**
 * This class provides basic storage operations on virtual currencies.
 */
public class VirtualCurrencyStorage extends VirtualItemStorage{

    /**
     * Constructor
     */
    public VirtualCurrencyStorage() {
        mTag = "SOOMLA VirtualCurrencyStorage";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected String keyBalance(String itemId) {
        return keyCurrencyBalance(itemId);
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected void postBalanceChangeEvent(String itemId, int balance, int amountAdded) {
        BusProvider.getInstance().post(new CurrencyBalanceChangedEvent(itemId,
                balance, amountAdded));
    }

    private static String keyCurrencyBalance(String itemId) {
        return DB_CURRENCY_KEY_PREFIX + itemId + ".balance";
    }

    public final static String DB_CURRENCY_KEY_PREFIX = "currency.";
}
