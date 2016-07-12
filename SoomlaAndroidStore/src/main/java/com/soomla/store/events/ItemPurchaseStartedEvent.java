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
 * This event is fired when a specific <code>PurchasableVirtualItem</code> purchase process has
 * started.
 */
public class ItemPurchaseStartedEvent extends SoomlaEvent {

    /**
     * Constructor
     *
     * @param itemId
     */
    public ItemPurchaseStartedEvent(String itemId) {
        this(itemId, null);
    }

    public ItemPurchaseStartedEvent(String itemId, Object sender) {
        super(sender);
        mItemId = itemId;
    }


    /** Setters and Getters */

    public String getItemId() {
        return mItemId;
    }


    /** Private Members */

    private String mItemId;
}
