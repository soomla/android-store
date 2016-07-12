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
 * This event is fired when a specific <code>PurchasableVirtualItem</code> has been purchased.
 */
public class ItemPurchasedEvent extends SoomlaEvent {

    /**
     * Constructor
     *
     * @param itemId
     */
    public ItemPurchasedEvent(String itemId, String payload) {
        this(itemId, payload, null);
    }

    public ItemPurchasedEvent(String itemId, boolean isRestored, String payload) {
        this(itemId, isRestored, payload, null);
    }

    public ItemPurchasedEvent(String itemId, String payload, Object sender) {
        this(itemId, false, payload, sender);
    }

    public ItemPurchasedEvent(String itemId, boolean isRestored, String payload, Object sender) {
        super(sender);
        mItemId = itemId;
        mRestored = isRestored;
        mPayload = payload;
    }

    /** Setters and Getters */

    public String getItemId() {
        return mItemId;
    }

    public String getPayload() {
        return mPayload;
    }

    public boolean isRestored() {
        return mRestored;
    }

    /** Private Members */

    private String mItemId;
    private String mPayload;
    private boolean mRestored;
}
