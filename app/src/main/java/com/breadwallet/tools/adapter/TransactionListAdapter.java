package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.presenter.fragments.FragmentSettingsAll;
import com.breadwallet.presenter.fragments.FragmentTransactionExpanded;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.util.CustomLogger;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 7/7/16.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class TransactionListAdapter extends BaseAdapter {
    public static final String TAG = TransactionListAdapter.class.getName();

    private Activity activity;
    private ArrayList<TransactionListItem> data;
    private static LayoutInflater inflater = null;
    private static int unconfirmedColor;
    private static int sentColor;
    private static int receivedColor;

    public TransactionListAdapter(Activity a, TransactionListItem[] d) {
        activity = a;
        data = new ArrayList<>();
        if (d != null)
            Collections.addAll(data, d);
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        unconfirmedColor = ContextCompat.getColor(a, R.color.white);
        sentColor = Color.parseColor("#FF5454");
        receivedColor = Color.parseColor("#00BF00");
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        View tmpLayout = convertView;
        if (convertView == null)
            tmpLayout = inflater.inflate(R.layout.transaction_list_item, null);
        TextView sentReceivedTextView = (TextView) tmpLayout.findViewById(R.id.transaction_sent_received_label);
        TextView dateTextView = (TextView) tmpLayout.findViewById(R.id.transaction_date);
        TextView bitsTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_bits);
        TextView dollarsTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_dollars);
        TextView bitsTotalTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_bits_total);
        TextView dollarsTotalTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_dollars_total);
        Utils.overrideFonts(sentReceivedTextView, dateTextView, bitsTextView, dollarsTextView, bitsTotalTextView, dollarsTotalTextView);
        tmpLayout.setBackgroundResource(R.drawable.clickable_layout);
        final TransactionListItem item = data.get(position);

        tmpLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.e(TAG, "clicked: " + ((TextView) tmpLayout.findViewById(R.id.transaction_date)).getText().toString());
                if (BRAnimator.checkTheMultipressingAvailability()) {
                    FragmentSettingsAll fragmentSettingsAll = (FragmentSettingsAll) activity.
                            getFragmentManager().findFragmentByTag(FragmentSettingsAll.class.getName());
                    FragmentTransactionExpanded fragmentTransactionExpanded = new FragmentTransactionExpanded();
                    fragmentTransactionExpanded.setCurrentObject(item);
                    BRAnimator.animateSlideToLeft((MainActivity) activity, fragmentTransactionExpanded, fragmentSettingsAll);
                }
            }
        });

        boolean received = item.getSent() == 0;
        CustomLogger.logThis("TX getReceived", String.valueOf(item.getReceived()), "TX getSent", String.valueOf(item.getSent()),
                "TX getBalanceAfterTx", String.valueOf(item.getBalanceAfterTx()));
        int blockHeight = item.getBlockHeight();
        int estimatedBlockHeight = BRPeerManager.getEstimatedBlockHeight();
        int confirms = blockHeight == Integer.MAX_VALUE ? 0 : estimatedBlockHeight - blockHeight + 1;
        Log.e(TAG, "confirms: " + confirms);

        if (item.getSent() > 0 && item.getSent() == item.getReceived()) {
            sentReceivedTextView.setBackgroundResource(R.drawable.unconfirmed_label);
            sentReceivedTextView.setText(R.string.moved);
            sentReceivedTextView.setTextColor(unconfirmedColor);
        } else if (blockHeight != Integer.MAX_VALUE && confirms >= 6) {
            sentReceivedTextView.setBackgroundResource(received ? R.drawable.received_label : R.drawable.sent_label);
            sentReceivedTextView.setText(received ? R.string.received : R.string.sent);
            sentReceivedTextView.setTextColor(received ? receivedColor : sentColor);
        } else {
            sentReceivedTextView.setBackgroundResource(R.drawable.unconfirmed_label);
            sentReceivedTextView.setTextColor(unconfirmedColor);
            if (!BRWalletManager.getInstance(activity).transactionIsVerified(item.getHexId())) {
                sentReceivedTextView.setText(R.string.unverified);
            } else {
                Log.e(TAG, "item.getBlockHeight(): " + blockHeight + ", confirms: " + confirms + ", lastBlock: " + estimatedBlockHeight);
                Resources res = activity.getResources();
                int confsNr = confirms >= 0 && confirms <= 5 ? confirms : 0;
                String confs = res.getQuantityString(R.plurals.nr_confirmations, confsNr);
                String message = String.format(confs, confsNr);
                sentReceivedTextView.setText(message);
            }
        }

        long itemTimeStamp = item.getTimeStamp();
        Log.e(TAG, "item.getTimeStamp(): " + itemTimeStamp);
        dateTextView.setText(itemTimeStamp != 0 ? Utils.getFormattedDateFromLong(itemTimeStamp * 1000) : Utils.getFormattedDateFromLong(System.currentTimeMillis()));

        long satoshisAmount = received ? item.getReceived() : (item.getSent() - item.getReceived()) * -1;

        bitsTextView.setText(BRStringFormatter.getFormattedCurrencyString("BTC", satoshisAmount));
        dollarsTextView.setText(String.format("(%s)", BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(activity), SharedPreferencesManager.getIso(activity), new BigDecimal(satoshisAmount), activity)));
        long satoshisAfterTx = item.getBalanceAfterTx();

        bitsTotalTextView.setText(BRStringFormatter.getFormattedCurrencyString("BTC", satoshisAfterTx));
        dollarsTotalTextView.setText(String.format("(%s)", BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(activity), SharedPreferencesManager.getIso(activity), new BigDecimal(satoshisAfterTx), activity)));
        return tmpLayout;
    }
}