/**
 * Copyright (C) 2012-2014 Soomla Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.soomla.store.data;

import com.soomla.SoomlaUtils;
import com.soomla.data.KeyValueStorage;

/**
 * This class is an abstract definition of a Virtual Item Storage.
 */
public abstract class VirtualItemStorage {

    /**
     * Retrieves the balance of the given virtual item.
     *
     * @param itemId the required virtual item's itemId
     * @return the balance of the required virtual item
     */
    public int getBalance(String itemId){
        SoomlaUtils.LogDebug(mTag, "fetching balance for virtual item with itemId: "
                + itemId);

        String key = keyBalance(itemId);
        String val = KeyValueStorage.getValue(key);

        int balance = 0;
        if (val != null) {
            balance = Integer.parseInt(val);
        }

        SoomlaUtils.LogDebug(mTag, "the balance for " + itemId + " is " + balance);

        return balance;
    }

    /**
     * Sets the balance of the given virtual item to be the given balance.
     *
     * @param itemId the required virtual item's itemId
     * @param balance the new balance to be set
     * @return the balance of the required virtual item
     */
    public int setBalance(String itemId, int balance) {
        return setBalance(itemId, balance, true);
    }

    /**
     * Sets the balance of the given virtual item to be the given balance, and if notify is true
     * posts the change in the balance to the event bus.
     *
     * @param itemId the required virtual item's itemId
     * @param balance the new balance to be set
     * @param notify if notify is true post balance change event
     * @return the balance of the required virtual item
     */
    public int setBalance(String itemId, int balance, boolean notify) {
        SoomlaUtils.LogDebug(mTag, "setting balance " + balance + " to " + itemId + ".");

        int oldBalance = getBalance(itemId);
        if (oldBalance == balance) {
            return balance;
        }

        String balanceStr = "" + balance;
        String key = keyBalance(itemId);

        KeyValueStorage.setValue(key, balanceStr);

        if (notify) {
            postBalanceChangeEvent(itemId, balance, 0);
        }

        return balance;
    }

    /**
     * Adds the given amount of items to the storage.
     *
     * @param itemId the required virtual item's itemId
     * @param amount the amount of items to add
     * @return new balance
     */
    public int add(String itemId, int amount){
        return add(itemId, amount, true);
    }

    /**
     * Adds the given amount of items to the storage, and if notify is true
     * posts the change in the balance to the event bus.
     *
     * @param itemId the required virtual item's itemId
     * @param amount the amount of items to add
     * @param notify if true posts balance change event
     * @return new balance
     */
    public int add(String itemId, int amount, boolean notify){
        SoomlaUtils.LogDebug(mTag, "adding " + amount + " " + itemId);

        int balance = getBalance(itemId);
        if (balance < 0) { /* in case the user "adds" a negative value */
            balance = 0;
            amount = 0;
        }
        String balanceStr = "" + (balance + amount);
        String key = keyBalance(itemId);
        KeyValueStorage.setValue(key, balanceStr);

        if (notify) {
            postBalanceChangeEvent(itemId, balance+amount, amount);
        }

        return balance + amount;
    }

    /**
     * Removes the given amount from the given virtual item's balance.
     *
     * @param itemId is the virtual item to remove the given amount from
     * @param amount is the amount to remove
     * @return new balance
     */
    public int remove(String itemId, int amount){
        return remove(itemId, amount, true);
    }

    /**
     * Removes the given amount from the given virtual item's balance, and if notify is true
     * posts the change in the balance to the event bus.
     *
     * @param itemId is the virtual item to remove the given amount from
     * @param amount is the amount to remove
     * @param notify if notify is true post balance change event
     * @return new balance
     */
    public int remove(String itemId, int amount, boolean notify){
        SoomlaUtils.LogDebug(mTag, "Removing " + amount + " " + itemId + ".");

        int balance = getBalance(itemId) - amount;
        if (balance < 0) {
            balance = 0;
            amount = 0;
        }
        String balanceStr = "" + balance;
        String key = keyBalance(itemId);
        KeyValueStorage.setValue(key, balanceStr);

        if (notify) {
            postBalanceChangeEvent(itemId, balance, -1*amount);
        }

        return balance;
    }

    /**
     * Retrieves the balance of the virtual item with the given itemId from the
     * <code>KeyValDatabase</code>.
     *
     * @param itemId id of the virtual item whose balance is to be retrieved
     * @return String containing name of storage base, itemId, and balance
     */
    protected abstract String keyBalance(String itemId);

    /**
     * Posts the given amount changed in the given balance of the given virtual item.
     *
     * @param itemId virtual item whose balance has changed
     * @param balance the balance that has changed
     * @param amountAdded the amount added to the item's balance
     */
    protected abstract void postBalanceChangeEvent(String itemId, int balance, int amountAdded);


    /** Private Members */

    protected String mTag = "SOOMLA VirtualItemStorage"; //used for Log messages
}
