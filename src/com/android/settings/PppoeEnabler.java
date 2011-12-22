/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import com.android.settings.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.PppoeManager;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;
import android.util.Log;

public class PppoeEnabler implements Preference.OnPreferenceChangeListener {
    private final Context mContext; 
    private final CheckBoxPreference mCheckBox;
    private final CharSequence mOriginalSummary;

    private final PppoeManager mPppoeManager;
    private final IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (PppoeManager.PPPOE_STATE_CHANGED_ACTION.equals(action)) {
                handlePppoeStateChanged(intent.getIntExtra(
                        PppoeManager.EXTRA_PPPOE_STATE, PppoeManager.PPPOE_STATE_UNKNOWN));
            } else if (PppoeManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                handleStateChanged(((NetworkInfo) intent.getParcelableExtra(
                        PppoeManager.EXTRA_NETWORK_INFO)).getDetailedState());
            }
        }
    };

    public PppoeEnabler(Context context, CheckBoxPreference checkBox) {
        mContext = context;
        mCheckBox = checkBox;
        mOriginalSummary = checkBox.getSummary();
        checkBox.setPersistent(false);

        mPppoeManager = (PppoeManager) context.getSystemService(Context.PPPOE_SERVICE);
        mIntentFilter = new IntentFilter(PppoeManager.PPPOE_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(PppoeManager.NETWORK_STATE_CHANGED_ACTION);
    }

    public void resume() {
        // Wi-Fi state is sticky, so just let the receiver update UI
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mCheckBox.setOnPreferenceChangeListener(this);
    }
    
    public void pause() {
        mContext.unregisterReceiver(mReceiver);
        mCheckBox.setOnPreferenceChangeListener(null);
    }
    
    public boolean onPreferenceChange(Preference preference, Object value) {
        boolean enable = (Boolean) value;

        if (mPppoeManager.setPppoeEnabled(enable)) {
            mCheckBox.setEnabled(false);
        } else {
            mCheckBox.setSummary(R.string.pppoe_error);
        }

        // Don't update UI to opposite state until we're sure
        return false;
    }
    
    private void handlePppoeStateChanged(int state) {
        switch (state) {
            case PppoeManager.PPPOE_STATE_ENABLING:
                mCheckBox.setSummary(R.string.pppoe_starting);
                mCheckBox.setEnabled(false);
                break;
            case PppoeManager.PPPOE_STATE_ENABLED:
                mCheckBox.setChecked(true);
                //mCheckBox.setSummary(null);
                mCheckBox.setEnabled(true);
                break;
            case PppoeManager.PPPOE_STATE_DISABLING:
                mCheckBox.setSummary(R.string.pppoe_stopping);
                mCheckBox.setEnabled(false);
                break;
            case PppoeManager.PPPOE_STATE_DISABLED:
                mCheckBox.setChecked(false);
                mCheckBox.setSummary(mOriginalSummary);
                mCheckBox.setEnabled(true);
                break;
            case PppoeManager.PPPOE_STATE_CHECK_CONNNECT:
                mCheckBox.setSummary(R.string.pppoe_check_connection);
                break;
            case PppoeManager.PPPOE_STATE_NO_ACCOUNT:
                mCheckBox.setSummary(R.string.pppoe_no_account);
                break;     
            case PppoeManager.PPPOE_STATE_MODEM_HUNGUP:
                mCheckBox.setSummary(R.string.pppoe_modem_hungup);
                break;  
            case PppoeManager.PPPOE_STATE_ACCOUNT_UNCORRECT:
                mCheckBox.setSummary(R.string.pppoe_account_uncorrect);
                break;  
                           
            default:
                mCheckBox.setChecked(false);
                mCheckBox.setSummary(R.string.pppoe_error);
                mCheckBox.setEnabled(true);

        }
    }

    private void handleStateChanged(NetworkInfo.DetailedState state) {
        // WifiInfo is valid if and only if Wi-Fi is enabled.
        // Here we use the state of the check box as an optimization.
        if (state != null && mCheckBox.isChecked()) {
        
            String[] formats = mContext.getResources().getStringArray(R.array.wifi_status );
            String   summary;
            int index = state.ordinal();

            if (index >= formats.length || formats[index].length() == 0) {
                summary = null;
            }else
            {
                summary = String.format(formats[index]);
            } 
            
            mCheckBox.setSummary(summary);
        }
    }
}
