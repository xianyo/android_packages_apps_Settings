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
/* Copyright (C) 2011 Freescale Semiconductor,Inc. */

package com.android.settings;

import com.android.settings.ProgressCategory;
import com.android.settings.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ContentResolver;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.net.PppoeManager;
import android.net.EthernetManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.security.Credentials;
import android.security.KeyStore;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;
import android.widget.Button;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class EthernetSettings extends PreferenceActivity implements DialogInterface.OnClickListener {
    
    private static final String TAG = "ETHERNET_SETTING";

    private static final String KEY_TOGGLE_ETHERNET = "toggle_ethernet";
    private static final String KEY_TOGGLE_PPPOE = "toggle_pppoe";

    private static final int MENU_ID_SCAN = Menu.FIRST;
    private static final int MENU_ID_ADVANCED = Menu.FIRST + 1;
    private static final int MENU_ID_CONNECT = Menu.FIRST + 2;
    private static final int MENU_ID_FORGET = Menu.FIRST + 3;
    private static final int MENU_ID_MODIFY = Menu.FIRST + 4;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;

    private EthernetManager mEthernetManager;
    private EthernetEnabler mEthernetEnabler;
    
    private PppoeManager mPppoeManager;
    private PppoeEnabler mPppoeEnabler;
    private Preference mManageAccount;
    
    private CheckBoxPreference mEthernetCheckBox;
    private CheckBoxPreference mPppoeCheckBox;
    
    private DetailedState mLastState;

    private Button mAddAccountButton;
    
    private PppoeDialog mDialog;

    public EthernetSettings() {
        mFilter = new IntentFilter();
        mFilter.addAction(PppoeManager.PPPOE_STATE_CHANGED_ACTION);
        mFilter.addAction(EthernetManager.ETHERNET_STATE_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(intent);
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPppoeManager = (PppoeManager) getSystemService(Context.PPPOE_SERVICE);
        addPreferencesFromResource(R.xml.ethernet_settings);
        
        mEthernetCheckBox = (CheckBoxPreference) findPreference(KEY_TOGGLE_ETHERNET);
        mPppoeCheckBox = (CheckBoxPreference) findPreference(KEY_TOGGLE_PPPOE);
                
        mEthernetEnabler  = new EthernetEnabler(this, mEthernetCheckBox);
        mPppoeEnabler  = new PppoeEnabler(this, mPppoeCheckBox);

        mManageAccount = findPreference("manage_account");
        
        if(Settings.Secure.getString(getContentResolver(), Settings.Secure.PPPOE_USERNAME) != null)
            mManageAccount.setSummary(R.string.pppoe_account_inputted);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPppoeEnabler != null) {
            mPppoeEnabler.resume();
        }
        if (mEthernetEnabler != null) {
            mEthernetEnabler.resume();
        }
        registerReceiver(mReceiver, mFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPppoeEnabler != null) {
            mPppoeEnabler.pause();
        }
        if (mEthernetEnabler != null) {
            mEthernetEnabler.pause();
        }
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        unregisterReceiver(mReceiver);
    }



    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference == mManageAccount) {
            showDialog();
        }
        return true;
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == PppoeDialog.BUTTON_FORGET ) {
            forget();
            mManageAccount.setSummary(R.string.pppoe_account_toggle_summary);
        } else if (button == PppoeDialog.BUTTON_SUBMIT && mDialog != null) {
        
            Log.i(TAG, "username " +mDialog.getUsername() + " password "+mDialog.getPassword());
            if(mDialog.getUsername().isEmpty() || mDialog.getPassword().isEmpty())
            {
                Log.w(TAG, "please input the adsl username and password");
                mManageAccount.setSummary(R.string.pppoe_account_error);
            }else
            {
                mPppoeManager.setPppoeAccount(mDialog.getUsername(),mDialog.getPassword());
                mManageAccount.setSummary(R.string.pppoe_account_inputted);
            }
        }
    }

    private void showDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = new PppoeDialog(this, this);
        mDialog.show();
    }

    private void forget() {
        // disconnect
        mPppoeManager.removePppoeAccount();
        saveNetworks();
    }

    private void connect() {
        // connect
    }

    private void enableNetworks() {
    }

    private void saveNetworks() {
    }


    private void handleEvent(Intent intent) {
        String action = intent.getAction();
        if (PppoeManager.PPPOE_STATE_CHANGED_ACTION.equals(action)) {
            updatePppoeState(intent.getIntExtra(PppoeManager.EXTRA_PPPOE_STATE,
                    PppoeManager.PPPOE_STATE_UNKNOWN));
        } else if (EthernetManager.ETHERNET_STATE_CHANGED_ACTION.equals(action)) {
                updateEthernetState(intent.getIntExtra(
                        EthernetManager.EXTRA_ETHERNET_STATE, EthernetManager.ETHERNET_STATE_UNKNOWN));
        } 
    }

    private void updatePppoeState(int state) {
        switch (state) {
            case PppoeManager.PPPOE_STATE_ENABLED:
                mEthernetCheckBox.setEnabled(false);
                Log.w("pppoe settings", "enabled");
                break;

            case PppoeManager.PPPOE_STATE_DISABLED:
                mEthernetCheckBox.setEnabled(true);
                break;
            default:
                break;
        }
    }
    
    private void updateEthernetState(int state) {
        switch (state) {
            case EthernetManager.ETHERNET_STATE_ENABLED:
                mPppoeCheckBox.setEnabled(false);
                Log.w("ethernet settings", "enabled");
                break;

            case EthernetManager.ETHERNET_STATE_DISABLED:
                mPppoeCheckBox.setEnabled(true);
                break;
            default:
                break;
        }
    }

}
