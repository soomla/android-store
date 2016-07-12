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
import com.soomla.store.domain.PurchasableVirtualItem;

import java.util.HashMap;

/**
 * This event is fired when a Market purchase has occurred.
 */
public class MarketPurchaseEvent extends SoomlaEvent {

    /**
     * Constructor
     *
     * @param purchasableVirtualItem the item that was purchased
     * @param payload the amount paid by the user (with real money!)
     * @param extraInfo will contain extra information associated with the current purchase from the specific market
     */
    public MarketPurchaseEvent(PurchasableVirtualItem purchasableVirtualItem, String payload,
                               HashMap<String, String> extraInfo) {
        this(purchasableVirtualItem, payload, extraInfo, null);
    }

    public MarketPurchaseEvent(PurchasableVirtualItem purchasableVirtualItem, String payload,
                               HashMap<String, String> extraInfo, Object sender) {
        this(purchasableVirtualItem, false, payload, extraInfo, sender);

    }

    public MarketPurchaseEvent(PurchasableVirtualItem purchasableVirtualItem, boolean isRestored, String payload, HashMap<String, String> extraInfo, Object sender) {
        super(sender);
        PurchasableVirtualItem = purchasableVirtualItem;
        IsRestored = isRestored;
        Payload = payload;
        ExtraInfo = extraInfo;
    }

    /** Private Members */

    public final PurchasableVirtualItem PurchasableVirtualItem;

    public final boolean IsRestored;

    public final String Payload;

    public final HashMap<String, String> ExtraInfo;
}
