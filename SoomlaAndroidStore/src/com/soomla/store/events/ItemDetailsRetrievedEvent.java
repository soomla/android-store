/*
 * Copyright (C) 2012 Soomla Inc.
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


/**
 * This event is fired when item details got retrieved successfully
 */
public class ItemDetailsRetrievedEvent {

    private String productId;
    private String price;
    private String title;
    private String description;
    private boolean isFinished;

    public ItemDetailsRetrievedEvent(String productId, String price, String title, String description, boolean isFinished) {
    	this.productId = productId;
    	this.price = price;
    	this.title = title;
    	this.description = description;
    	this.isFinished = isFinished;
    }

	public String getProductId() {
		return productId;
	}

	public String getPrice() {
		return price;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public boolean isFinished() {
		return isFinished;
	}
}
