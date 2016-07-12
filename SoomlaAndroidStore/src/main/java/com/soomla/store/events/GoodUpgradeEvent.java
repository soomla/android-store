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
 * This event is fired when a specific <code>UpgradeVG</code> has been upgraded/downgraded.
 *
 * Real Game Example:
 *  Example Inventory: { currency_coin: 100, Characters: Robot_X_1 }
 *  Suppose your user upgrades "Characters". Robot_X_1 is the first Robot_X in the series.
 *  After the upgrade, his/her new balance of Characters will be { Characters: Robot_X_2 }.
 *  A <code>GoodUpgradeEvent</code> is fired.
 */
public class GoodUpgradeEvent extends SoomlaEvent {

    /**
     * Constructor
     *
     * @param itemId good that has been upgraded/downgraded
     * @param upgradeVGItemId upgrade details
     */
    public GoodUpgradeEvent(String itemId, String upgradeVGItemId) {
        this(itemId, upgradeVGItemId, null);
    }

    public GoodUpgradeEvent(String itemId, String upgradeVGItemId, Object sender) {
        super(sender);
        mItemId = itemId;
        mCurrentUpgradeItemId = upgradeVGItemId;
    }


    /** Setters and Getters */

    public String getGoodItemId() {
        return mItemId;
    }

    public String getCurrentUpgrade() {
        return mCurrentUpgradeItemId;
    }


    /** Private Members */

    private String mItemId; //good that has been upgraded/downgraded

    private String mCurrentUpgradeItemId; //upgrade details

}
