/*
 * Copyright (C) 2012-2014 Soomla Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.soomla.store.events;

import com.soomla.events.SoomlaEvent;

/**
 * This event is fired when the market items refresh process fails.
 */
public class MarketItemsRefreshFailedEvent extends SoomlaEvent{
    /**
     * The error message received from the billing provider
     */
    public String ErrorMessage;

    /**
     * Constructor.
     *
     * @param errorMessage The error message received from the billing provider
     */
    public MarketItemsRefreshFailedEvent(String errorMessage) {
        this(errorMessage, null);
    }

    public MarketItemsRefreshFailedEvent(String errorMessage, Object sender) {
        super(sender);
        ErrorMessage = errorMessage;
    }
}
