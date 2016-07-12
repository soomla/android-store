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

package com.soomla.store.purchaseTypes;

import com.soomla.store.domain.PurchasableVirtualItem;
import com.soomla.store.exceptions.InsufficientFundsException;

/**
 * A <code>PurchaseType</code> is a way to purchase a <code>PurchasableVirtualItem</code>. This
 * abstract class describes basic features of the actual implementations of <code>PurchaseType<code>.
 */
public abstract class PurchaseType {

    /*
     * Buys the purchasable virtual item.
     * Implementation in subclasses will be according to specific type of purchase.
     *
     * @param payload a string you want to be assigned to the purchase. This string
     *   is saved in a static variable and will be given bacl to you when the
     *   purchase is completed.
     * @throws InsufficientFundsException
     */
    public abstract void buy(String payload) throws InsufficientFundsException;


    /** Setters and Getters */

    public void setAssociatedItem(PurchasableVirtualItem associatedItem) {
        mAssociatedItem = associatedItem;
    }

    public PurchasableVirtualItem getAssociatedItem() {
        return mAssociatedItem;
    }


    /** Private Members */

    //the PurchasableVirtualItem associated with this PurchaseType
    private PurchasableVirtualItem mAssociatedItem;

}
