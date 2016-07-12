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

package com.soomla.store.billing;

 import java.util.List;

 /**
  * A utility class that defines interfaces for passing callbacks to in-app billing events.
  */
public class IabCallbacks {

     /**
      * Listens for in-app billing service initialization
      */
    public interface IabInitListener {

         /**
          * Performs the following function upon success.
          *
          * @param alreadyInBg true if the listener has already been initialized and is in
          *                    background, false otherwise.
          */
        public void success(boolean alreadyInBg);

         /**
          * Performs the following function upon failure and prints the given message.
          *
          * @param message reason for failure
          */
        public void fail(String message);
    }

     /**
      * Listens for in-app purchases being made.
      */
    public interface OnPurchaseListener {

         /**
          * The user has successfully completed a purchase.
          *
          * @param purchase the successful purchase
          */
        public void success(IabPurchase purchase);

         /**
          * The user has cancelled a purchase.
          *
          * @param purchase the cancelled purchase
          */
        public void cancelled(IabPurchase purchase);

         /**
          * The user tries to buy an item he/she already owns.
          *
          * @param purchase the purchase that is already owned
          */
        public void alreadyOwned(IabPurchase purchase);

         /**
          * The user fails to make the purchase.
          *
          * @param message reason for failure
          */
        public void fail(String message);

         /**
          * The purchase verification has started
          *
          * @param purchase the purchase being verified
          */
         public void verificationStarted(List<IabPurchase> purchases);

    }

     /**
      * Listens for restore purchases queries
      */
    public interface OnRestorePurchasesListener {

         /**
          * Restore Purchase is successful.
          *
          * @param purchases list of purchases from he/she inventory
          */
        public void success(List<IabPurchase> purchases);

         /**
          * Query inventory fails.
          *
          * @param message reason for failure
          */
        public void fail(String message);

         /**
          * The purchase verification has started
          *
          * @param purchase the purchase being verified
          */
         public void verificationStarted(List<IabPurchase> purchases);
    }

     /**
      * Listens for fetch skus details queries
      */
    public interface OnFetchSkusDetailsListener {

        /**
         * Fetch skus details is successful.
         *
         * @param skuDetails list of skus details
         */
        public void success(List<IabSkuDetails> skuDetails);
        /**
         * Query inventory fails.
         *
         * @param message reason for failure
         */
         public void fail(String message);
    }


     /**
      * Listens for consumptions of purchases
      */
    public interface OnConsumeListener {
         /**
          * Purchase consumption is successful
          *
          * @param purchase consumed purchase
          */
        public void success(IabPurchase purchase);

         /**
          * Purchase consumption fails
          *
          * @param message reason for failure
          */
        public void fail(String message);
    }

}
