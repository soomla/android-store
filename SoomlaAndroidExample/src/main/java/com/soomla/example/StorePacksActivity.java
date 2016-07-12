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


package com.soomla.example;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.soomla.BusProvider;
import com.soomla.store.data.StorageManager;
import com.soomla.store.data.StoreInfo;
import com.soomla.store.domain.virtualCurrencies.VirtualCurrencyPack;
import com.soomla.store.events.CurrencyBalanceChangedEvent;
import com.soomla.store.exceptions.InsufficientFundsException;
import com.soomla.store.purchaseTypes.PurchaseWithMarket;
import com.squareup.otto.Subscribe;

import java.util.HashMap;

/**
 * This class represents Muffin Rush's store of available currency-packs.
 */
public class StorePacksActivity extends Activity {

    /**
     * Receives the given <code>currencyBalanceChangedEvent</code>, and upon notification, fetches
     * the currency balance and places it in the balance label.
     *
     * @param currencyBalanceChangedEvent event to receive
     */
    @Subscribe
    public void onCurrencyBalanceChanged(CurrencyBalanceChangedEvent currencyBalanceChangedEvent) {
        TextView muffinsBalance = (TextView)findViewById(R.id.balance);
        muffinsBalance.setText("" + currencyBalanceChangedEvent.getBalance());
    }

    /**
     * Called when the activity starts.
     *
     * @param savedInstanceState if the activity should be re-initialized after previously being
     *                           shut down then this <code>Bundle</code> will contain the most
     *                           recent data, otherwise it will be null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview);

        LinearLayout getMore = (LinearLayout)findViewById(R.id.getMore);
        TextView title = (TextView)findViewById(R.id.title);

        getMore.setVisibility(View.INVISIBLE);
        title.setText("Virtual Currency Packs");

        mImages = generateImagesHash();

        mStoreAdapter = new StoreAdapter();

        /* configuring the list with an adapter */

        final Activity activity = this;
        ListView list = (ListView) findViewById(R.id.list);
        list.setAdapter(mStoreAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

               /*
                * The user decided to make an actual purchase of virtual goods. We try to buy() the
                * user's desired good and SoomlaStore tells us if the user has enough funds to
                * make the purchase. If he/she doesn't have enough then an
                * InsufficientFundsException will be thrown.
                */
                PurchaseWithMarket pwm = null;

                VirtualCurrencyPack pack = StoreInfo.getCurrencyPacks().get(i);
                pwm = (PurchaseWithMarket) pack.getPurchaseType();

                try {
                    pwm.buy("this is just a payload");
                } catch (InsufficientFundsException e) {
                    AlertDialog ad = new AlertDialog.Builder(activity).create();
                    ad.setCancelable(false); // This blocks the 'BACK' button
                    ad.setMessage("Can't continue with purchase (You don't have enough muffins !)");
                    ad.setButton(DialogInterface.BUTTON_NEGATIVE, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    ad.show();
                }

                /* fetching the currency balance and placing it in the balance label */
                TextView muffinsBalance = (TextView)activity.findViewById(R.id.balance);
                muffinsBalance.setText("" + StorageManager.getVirtualCurrencyStorage().
                        getBalance(StoreInfo.getCurrencies().get(0).getItemId()));
            }
        });

    }

    /**
     * Called after the activity has been paused, and now this activity will start interacting with
     * your user once again.
     * Fetches the currency balance and places it in the balance label.
     */
    @Override
    protected void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
        TextView muffinsBalance = (TextView)findViewById(R.id.balance);
        muffinsBalance.setText("" + StorageManager.getVirtualCurrencyStorage().
                getBalance(StoreInfo.getCurrencies().get(0).getItemId()));
    }

    /**
     * Called when your user leaves your activity but does not quit, or in other words, upon a call
     * to <code>onPause()</code> your activity goes to the background.
     */
    @Override
    protected void onPause() {
        super.onPause();

        BusProvider.getInstance().unregister(this);
    }

    /**
     * Creates a hashmap of images of all currency-packs and non-consumable items in Muffin Rush.
     *
     * @return hashmap of dessert images needed in Muffin Rush
     */
    private HashMap<String, Object> generateImagesHash() {
        final HashMap<String, Object> images = new HashMap<String, Object>();
        images.put(MuffinRushAssets.TENMUFF_PACK_PRODUCT_ID, R.drawable.muffins01);
        images.put(MuffinRushAssets.FIFTYMUFF_PACK_PRODUCT_ID, R.drawable.muffins02);
        images.put(MuffinRushAssets.FOURHUNDMUFF_PACK_PRODUCT_ID, R.drawable.muffins03);
        images.put(MuffinRushAssets.THOUSANDMUFF_PACK_PRODUCT_ID, R.drawable.muffins04);

        return images;
    }

    private class StoreAdapter extends BaseAdapter {

        public StoreAdapter() {
        }

        public int getCount() {
            return mImages.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View vi = convertView;
            if(convertView == null){
                vi = getLayoutInflater().inflate(R.layout.list_item, null);
            }

            TextView title = (TextView)vi.findViewById(R.id.title);
            TextView content = (TextView)vi.findViewById(R.id.content);
            TextView info = (TextView)vi.findViewById(R.id.item_info);
            ImageView thumb_image=(ImageView)vi.findViewById(R.id.list_image);

            // Setting all values in listview

            VirtualCurrencyPack pack = StoreInfo.getCurrencyPacks().get(position);
            title.setText(pack.getName());
            content.setText(pack.getDescription());
            PurchaseWithMarket pwm = (PurchaseWithMarket) pack.getPurchaseType();
            info.setText("price: $" + pwm.getMarketItem().getPrice());
            thumb_image.setImageResource((Integer)mImages.get(pwm.getMarketItem().getProductId()));

            return vi;
        }
    }


    /** Private Members */

    private StoreAdapter mStoreAdapter;

    private HashMap<String, Object> mImages;

}