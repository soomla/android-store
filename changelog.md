### v3.6.21 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.20...v3.6.21)

* Features
  * Implement subscriptions

### v3.6.20 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.19...v3.6.20)

* Changes
  * Changed price of MarketItem to have only 2 decimal digits

### v3.6.19 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.18...v3.6.19)

* Changes
  * Added `isRestoring` flag for purchase events

### v3.6.18 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.17...v3.6.18)

* Changes
  * Removed binaries from repo

### v3.6.17 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.16...v3.6.17)

* Changes
  * Added validation - not allowing null pointers in IStoreAssets

### v3.6.16 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.15...v3.6.16)

* New Features
  * Add new event - VerificationStarted

### v3.6.15 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.14...v3.6.15)

* Fixes
  * Wrong behaviour if purchasable items have the same product ID

* Changes
  * Removed binaries and improved build scripts

### v3.6.14 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.13...v3.6.14)

* Changes
  * Added Fraud Protection flag to OnMarketPurchaseStarted event
  * Improved documentation

### v3.6.13 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.12...v3.6.13)

* Changes
  * Small change in saving version and build
  * Making a call to purchaseListener safer

### v3.6.12 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.11...v3.6.12)

* New Features
  * Google Play receipt validation is available

* Changes
  * Market purchase events are fired with params hash for different parameters
  * Changed the order: restore transactions now follows refresh market items (it used to be the other way)
  * UnexpectedErrorInStoreEvent now accepts errorCode and changed to UnexpectedStoreErrorEvent. **BREAKING CHANGE**

### v3.6.11 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.10...v3.6.11)

* Fixes
  * Copying lists in allItemsBalances to prevent crash if this happens in parallel to market refresh

* Changes
  * Supporting changes to Foreground from Core
  * Removed the MANAGED enum from MarketItem

### v3.6.10 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.9...v3.6.10)

* Features
  * Added original JSON (Google), signature (Google) and User ID (Amazon) in MarketPruchaseEvent
* Fixes
  * Importing Amazon plugin v2.0.2 with fixes [see here](https://github.com/soomla/android-store-amazon/blob/master/changelog.md)

### v3.6.9 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.8...v3.6.9)

* Fixes
  * Importing Amazon plugin v2.0.1 with fixes [see here](https://github.com/soomla/android-store-amazon/blob/master/changelog.md)

### v3.6.8 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.7...v3.6.8)

* Changes
  * Making sure we send failure events on all store operations
  * Updating submodule

### v3.6.7 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.6...v3.6.7)

* New Features
  * Store will now save your market item details to local storage

### v3.6.6 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.5...v3.6.6)

* Fixes
  * Fixed issues with purchases on edge cases on Google Play

### v3.6.5 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.4...v3.6.5)

* Fixes
  * Supporting foreground/background events on versions >= ICS

### v3.6.4 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.3...v3.6.4)

* New Features
  * Added new function in StoreInventory to reset local state
  * Added some support for Core's Foreground service

### v3.6.3 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.2...v3.6.3)

* Fixes
  * New Amazon jar with fixes for issues: empty skus and getting price micros exception

### v3.6.2 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.1...v3.6.2)

* Fixes
  * Fixed issues with non-consumable migration inside StoreInfo
  * Fixed issues with Amazon billing service

* New Features
  * Now supporting Amazon v2

### v3.6.1 [view commit logs](https://github.com/soomla/android-store/compare/v3.6.0...v3.6.1)
* Fixes
  * Fixed the point-in-time RestoreTransactionsStartedEvent is fired.

* Changes
  * Pulled changes and fixes from Core module v1.0.0

### v3.6.0 [view commit logs](https://github.com/soomla/android-store/compare/v3.5.0...v3.6.0)
* Changes
  * Events and Storages work with itemIds instead of actual model objects (Simplification)

* Fixes
  * Fixed a crash on multiple purchases at once.

* New Features
  * Soomla events can now send the event sender.
  * Another function in StoreInventory fetches all balances for all itemIds at once.

### v3.5.0 [view commit logs](https://github.com/soomla/android-store/compare/v3.4.2...v3.5.0)

***Important***: this is a breaking release. Read the changes carefully.

* Changes:
  * **BREAKING**: `NonConsumableItem` class was **removed**. To create non-consumable item, use `LifeTimeVG` instead.
  * Fetching price and currency symbol separately when refreshing `MarketItem`s.
  * Added orderId to MarketPurchaseEvent

* Fixes:
  * Fixed an issue with backward compatibility of obfuscated sharePrefs.

### v3.4.2 [view commit logs](https://github.com/soomla/android-store/compare/v3.4.1...v3.4.2)

* New Features
  * Added the option to provide a payload to the 'buy' function

* Fixes
  * Firing UnexpectedErrorInStoreEvent when the user tried to buy a NonConsumableItem when it was already owned
  * Fixed issues with the way Rewards are generated

### v3.4.1 [view commit logs](https://github.com/soomla/android-store/compare/v3.4.0...v3.4.1)

* New Features
  * Some core objects and features were extracted to a separate project [soomla-android-core](https://github.com/soomla/soomla-android-core).
  * SOOM_SEC is no longer relevant. You only supply one secret called Soomla Secret when you initialize "Soomle" (soomla core).

* Changes
  * StoreController is now called SoomlaStore.

### v3.4.0 [view commit logs](https://github.com/soomla/android-store/compare/v3.3.3...v3.4.0)

* Fixes
  * Fixed an issue with crashes in some situations. Thanks @HolymarsHsieh

* New Features
  * Added and Fixed docs
  * We now support multiple billing services. Officially: Google Play and Amazon.
  * You can now restore purchases and refresh market items separately (and also query inventory which does both).
  * Removed submodules and added billing services' jars to billing-services folder.

### v3.3.3 [view commit logs](https://github.com/soomla/android-store/compare/v3.3.2...v3.3.3)

* Fixes
  * Correctly fetching products' details from market.
  * Handle large sku-details query (Google limits to no more than 20 skus)

* New Features
  * Added docs


### v3.3.2 [view commit logs](https://github.com/soomla/android-store/compare/v3.3.1...v3.3.2)

* New Features
  * Added purchaseToken to MarketPurchaseEvent.
  * Added isInitialized to StoreController.
  * Added the option to overwrite an object in StoreInfo and save it.
  * You can now save StoreInfo to DB if you made changes to it.
  * You can now change price of MarketItem.
  * You can now change amount of PurchaseWithVirtualItem.

* Fixes
  * Enforce providing a SOOM_SEC in obfuscator.
  * If the purchasable item is NonConsumableItem and it already exists then we don't fire any events.


### v3.3.1 [view commit logs](https://github.com/soomla/android-store/compare/v3.3.0...v3.3.1)

* Billing Services
  * Add interface for async consume.
  * Add onDestroy() to IabActivity.
  * Allowing empty signatures for test purchases only when you specifically ask for it in StoreConfig.

* New Features
  * android-store will now refresh details of market items on initialization.

* Optimizations
  * Fixed Names of objects and events so they match in all SOOMLA plugins.
  * Added the changed market items to the MarketItemsRefreshed event.

### v3.3.0 [view commit logs](https://github.com/soomla/android-store/compare/v3.2.1...v3.3.0)

* Billing Services
  * We've split out Google Play so we can add more billing services.
  * Google Play is the default billing services and it's in a different project: https://github.com/soomla/android-store-google-play
