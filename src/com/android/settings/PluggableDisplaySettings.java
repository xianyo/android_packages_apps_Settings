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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ActivityManagerNative;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.os.Message;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.DisplayManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.SeekBarPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.IWindowManager;
import android.view.Surface;


import java.util.ArrayList;

public class PluggableDisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener,DialogInterface.OnClickListener {

    private static final String TAG = "PluggableDisplaySettings";
    private static final boolean DBG = true;

    private static final int DISPLAY_ENABLE_MSG = 1;
    private static final int DISPLAY_MIRROR_MSG = 2;
    private static final int DISPLAY_ROTATION_MSG = 3;
    private static final int DISPLAY_OVERSCAN_MSG = 4;
    private static final int DISPLAY_MODE_MSG = 5;    
    private static final int DISPLAY_COLORDEPTH_MSG = 6;    
    
    private static final int MAX_DISPLAY_DEVICE = 4;    

    private static final String[] KEY_DISPLAY_ENABLE     = {"display_enable_1","display_enable_2","display_enable_3","display_enable_4"};
    private static final String[] KEY_DISPLAY_MIRROR     = {"display_mirror_1","display_mirror_2","display_mirror_3","display_mirror_4"};
    private static final String[] KEY_DISPLAY_OVERSCAN   = {"display_overscan_1","display_overscan_2","display_overscan_3","display_overscan_4"};
    private static final String[] KEY_DISPLAY_MODE       = {"display_mode_1","display_mode_2","display_mode_3","display_mode_4"};
    private static final String[] KEY_DISPLAY_ROTATION   = {"display_rotation_1","display_rotation_2","display_rotation_3","display_rotation_4"};
    private static final String[] KEY_DISPLAY_COLORDEPTH = {"display_colordepth_1","display_colordepth_2","display_colordepth_3","display_colordepth_4"};
    private static final String[] KEY_DISPLAY_CATEGORY   = {"display_category_1","display_category_2","display_category_3","display_category_4"};

    private DisplayManager mDisplayManager;
    
    private final Configuration mCurConfig = new Configuration();

    private CheckBoxPreference[] mDisplayEnablePref = new CheckBoxPreference[MAX_DISPLAY_DEVICE];
    private CheckBoxPreference[] mMirrorPref        = new CheckBoxPreference[MAX_DISPLAY_DEVICE];
    private CheckBoxPreference[] mRotationPref      = new CheckBoxPreference[MAX_DISPLAY_DEVICE];
    private SeekBarPreference[]  mOverScanPref      = new SeekBarPreference[MAX_DISPLAY_DEVICE];
    private ListPreference[]     mDisplayModePref   = new ListPreference[MAX_DISPLAY_DEVICE];
    private ListPreference[]     mColorDepthPref    = new ListPreference[MAX_DISPLAY_DEVICE];
    private PreferenceCategory[] mCategoryPref      = new PreferenceCategory[MAX_DISPLAY_DEVICE];
    
    private IntentFilter mIntentFilter;
    
    private Dialog mConfirmDialog;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (DisplayManager.ACTION_DISPLAY_DEVICE_1_ATTACHED.equals(action))
            {
                int dispid = intent.getIntExtra(DisplayManager.EXTRA_DISPLAY_DEVICE, -1);
                boolean connect = intent.getBooleanExtra(DisplayManager.EXTRA_DISPLAY_CONNECT, false);
                handleDisplayConnected(dispid, connect);
            } else if (DisplayManager.ACTION_DISPLAY_DEVICE_2_ATTACHED.equals(action))
            {
                int dispid = intent.getIntExtra(DisplayManager.EXTRA_DISPLAY_DEVICE, -1);
                boolean connect = intent.getBooleanExtra(DisplayManager.EXTRA_DISPLAY_CONNECT, false);
                handleDisplayConnected(dispid, connect);
            } else if (DisplayManager.ACTION_DISPLAY_DEVICE_3_ATTACHED.equals(action))
            {
                int dispid = intent.getIntExtra(DisplayManager.EXTRA_DISPLAY_DEVICE, -1);
                boolean connect = intent.getBooleanExtra(DisplayManager.EXTRA_DISPLAY_CONNECT, false);
                handleDisplayConnected(dispid, connect);
            } else if (DisplayManager.ACTION_DISPLAY_DEVICE_4_ATTACHED.equals(action))
            {
                int dispid = intent.getIntExtra(DisplayManager.EXTRA_DISPLAY_DEVICE, -1);
                boolean connect = intent.getBooleanExtra(DisplayManager.EXTRA_DISPLAY_CONNECT, false);
                handleDisplayConnected(dispid, connect);
            }else if (DisplayManager.ACTION_DISPLAY_STATE.equals(action)) {
                int dispid = intent.getIntExtra(DisplayManager.EXTRA_DISPLAY_DEVICE, -1);
                handleDisplayStateChanged(dispid, intent.getIntExtra(
                    DisplayManager.EXTRA_DISPLAY_STATE, DisplayManager.DISPLAY_STATE_UNKNOWN));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.pluggable_display_settings);

        mDisplayManager = (DisplayManager)getSystemService(Context.DISPLAYMANAGER_SERVICE);

        for(int i=0; i<MAX_DISPLAY_DEVICE; i++){
            mDisplayModePref[i] = (ListPreference) findPreference(KEY_DISPLAY_MODE[i]);
            mDisplayModePref[i].setOnPreferenceChangeListener(this);
        
            mDisplayEnablePref[i] = (CheckBoxPreference) findPreference(KEY_DISPLAY_ENABLE[i]);
            mDisplayEnablePref[i].setOnPreferenceChangeListener(this);

            mMirrorPref[i]        = (CheckBoxPreference) findPreference(KEY_DISPLAY_MIRROR[i]);
            mMirrorPref[i].setOnPreferenceChangeListener(this);
        
            mOverScanPref[i]      = (SeekBarPreference) findPreference(KEY_DISPLAY_OVERSCAN[i]);
            mOverScanPref[i].setOnPreferenceChangeListener(this);        
            mRotationPref[i]      = (CheckBoxPreference) findPreference(KEY_DISPLAY_ROTATION[i]);
            mRotationPref[i].setOnPreferenceChangeListener(this);
            mColorDepthPref[i]    = (ListPreference) findPreference(KEY_DISPLAY_COLORDEPTH[i]);
            mColorDepthPref[i].setOnPreferenceChangeListener(this);
            
            mCategoryPref[i]      = (PreferenceCategory) findPreference(KEY_DISPLAY_CATEGORY[i]);
        }
        
        // remove the unused display ui , when connection is dectect;
        // main display device can not set enable, mirror
            getPreferenceScreen().removePreference(mDisplayEnablePref[0]);
            getPreferenceScreen().removePreference(mMirrorPref[0]);
        
        // remove the unused display ui , when connection is dectect;
        for(int i=1; i<MAX_DISPLAY_DEVICE; i++){
            getPreferenceScreen().removePreference(mDisplayModePref[i]);
            getPreferenceScreen().removePreference(mDisplayEnablePref[i]);
            getPreferenceScreen().removePreference(mMirrorPref[i]);
            getPreferenceScreen().removePreference(mOverScanPref[i]);
            getPreferenceScreen().removePreference(mRotationPref[i]);    
            getPreferenceScreen().removePreference(mColorDepthPref[i]); 
            getPreferenceScreen().removePreference(mCategoryPref[i]);               
        }
        
        mIntentFilter = new IntentFilter(DisplayManager.ACTION_DISPLAY_DEVICE_1_ATTACHED);
        mIntentFilter.addAction(DisplayManager.ACTION_DISPLAY_DEVICE_2_ATTACHED);
        mIntentFilter.addAction(DisplayManager.ACTION_DISPLAY_DEVICE_3_ATTACHED);
        mIntentFilter.addAction(DisplayManager.ACTION_DISPLAY_DEVICE_4_ATTACHED);        
        mIntentFilter.addAction(DisplayManager.ACTION_DISPLAY_STATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
        getActivity().registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mReceiver);
    }

    private void updateState() {

    }

    
    private void updateDisplayModePreferenceDescription(int dispid, String CurrentDisplayMode) {
        ListPreference preference = mDisplayModePref[dispid];
        preference.setSummary(CurrentDisplayMode);
    }

    private void updateDisplayColorDepthPreferenceDescription(int dispid,int CurrentDisplayColorDepth) {
        ListPreference preference = mColorDepthPref[dispid];
        preference.setSummary(String.format("%d bit", CurrentDisplayColorDepth));
    }
    
    private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what){
                case DISPLAY_ENABLE_MSG: {
                
                    break;
                }
                case DISPLAY_MIRROR_MSG: {
                
                    break;
                }
                case DISPLAY_ROTATION_MSG: {
                
                    break;
                }            
                case DISPLAY_OVERSCAN_MSG: {
                    mDisplayManager.setDisplayOverScan(msg.arg1, msg.arg2);
                    break;
                }            
                case DISPLAY_MODE_MSG: {
                
                    break;
                }
                case DISPLAY_COLORDEPTH_MSG: {
                
                    break;
                }
                default:
                    super.handleMessage(msg);            
            }
        
        }
    };



    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if(DBG) Log.w(TAG, "onPreferenceTreeClick ");

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if(DBG) Log.w(TAG, "onPreferenceChange ");
        final String key = preference.getKey();
        int dispid = 0;

        for(int i=0; i<MAX_DISPLAY_DEVICE; i++ ) {
            dispid = i;
            if (KEY_DISPLAY_MODE[i].equals(key)) {
                String value = (String) objValue;
                mDisplayManager.setDisplayMode(dispid, value);
                updateDisplayModePreferenceDescription(dispid, value);

                // display 0 need to reboot;
                if(i==0) {
                    //show diaglog
                    showRebootDialog();
                }

                break;
            }
        
            if (KEY_DISPLAY_ENABLE[i].equals(key)) {
                boolean value = (Boolean) objValue;
                mDisplayManager.setDisplayEnable(dispid, value);
                break;
            }
        
            if (KEY_DISPLAY_MIRROR[i].equals(key)) {
                boolean value = (Boolean) objValue;
                mDisplayManager.setDisplayMirror(dispid, value);      
                break;
            }
            
            if (KEY_DISPLAY_OVERSCAN[i].equals(key)) {
                int value = Integer.parseInt(objValue.toString());            
                Message msg = Message.obtain();
                msg.what = DISPLAY_OVERSCAN_MSG;
                msg.arg1 = dispid;
                msg.arg2 = value;
                mHandler.removeMessages(DISPLAY_OVERSCAN_MSG);
                mHandler.sendMessageDelayed(msg, 10);
                break;
            }

            if (KEY_DISPLAY_ROTATION[i].equals(key)) {
                boolean value = (Boolean) objValue;
                mDisplayManager.setDisplayRotation(dispid, value);        
                break;
            }
        
            if (KEY_DISPLAY_COLORDEPTH[i].equals(key)) {
                int value = Integer.parseInt((String) objValue);
                mDisplayManager.setDisplayColorDepth(dispid, value);         
                updateDisplayColorDepthPreferenceDescription(dispid, value);

                // display 0 need to reboot;
                if(i==0) {
                    //show diaglog
                    showRebootDialog();
                }
                break;
            }
        }
        
        return true;
    }
    
    private void handleDisplayConnected(int dispid, boolean connection){
        String[] display_modes;
        if(DBG) Log.w(TAG, "handleDisplayConnected " + connection + "dispid "+ dispid);
        if (dispid < 0) {
            Log.w(TAG, "dispid is no valid");
            return;
        }
        
        if(dispid == 0){

            String currentDisplayMode = mDisplayManager.getDisplayMode(dispid);

            display_modes = mDisplayManager.getDisplayModeList(dispid);
            
            ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
            ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();            
            for (String imode : display_modes) {
                revisedEntries.add(imode);
                revisedValues.add(imode);
            }

            mDisplayModePref[dispid].setEntries(
                revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            mDisplayModePref[dispid].setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));

            mDisplayModePref[dispid].setValue(String.valueOf(currentDisplayMode));
            mDisplayModePref[dispid].setOnPreferenceChangeListener(this);
            updateDisplayModePreferenceDescription(dispid, currentDisplayMode);
            
            int currentDisplayColorDepth = mDisplayManager.getDisplayColorDepth(dispid);
            mColorDepthPref[dispid].setValue(String.valueOf(currentDisplayColorDepth));
            mColorDepthPref[dispid].setOnPreferenceChangeListener(this);
            updateDisplayColorDepthPreferenceDescription(dispid, currentDisplayColorDepth);       
            mColorDepthPref[dispid].setEnabled(false);

            boolean currentDisplayRotation = mDisplayManager.getDisplayRotation(dispid);
            if(DBG) Log.w(TAG,"currentDisplayRotation of 0 " +currentDisplayRotation);
            mRotationPref[dispid].setChecked(currentDisplayRotation);                       
            mRotationPref[dispid].setOnPreferenceChangeListener(this);
            mRotationPref[dispid].setEnabled(false);
                
            int currentDisplayOverScan = mDisplayManager.getDisplayOverScan(dispid);
            mOverScanPref[dispid].setProgress(currentDisplayOverScan);
            mOverScanPref[dispid].setOnPreferenceChangeListener(this);
            mOverScanPref[dispid].setEnabled(false);
                         
            return ;            
        }
        
        // dispid > 0;
        if (connection){
            getPreferenceScreen().addPreference(mDisplayModePref[dispid]);
            getPreferenceScreen().addPreference(mDisplayEnablePref[dispid]);
            getPreferenceScreen().addPreference(mMirrorPref[dispid]);
            getPreferenceScreen().addPreference(mOverScanPref[dispid]);
            getPreferenceScreen().addPreference(mRotationPref[dispid]);    
            getPreferenceScreen().addPreference(mColorDepthPref[dispid]); 
            getPreferenceScreen().addPreference(mCategoryPref[dispid]); 
                               
            // read the display mode list
            String currentDisplayMode = mDisplayManager.getDisplayMode(dispid);
                            
            display_modes = mDisplayManager.getDisplayModeList(dispid);
            
            ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
            ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();            
            for (String imode : display_modes) {
                revisedEntries.add(imode);
                revisedValues.add(imode);
            }

            mDisplayModePref[dispid].setEntries(
                revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            mDisplayModePref[dispid].setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));

            mDisplayModePref[dispid].setValue(String.valueOf(currentDisplayMode));
            mDisplayModePref[dispid].setOnPreferenceChangeListener(this);
            updateDisplayModePreferenceDescription(dispid, currentDisplayMode);
            
                boolean currentDisplayEnable = mDisplayManager.getDisplayEnable(dispid);
                if(DBG) Log.w(TAG,"currentDisplayEnable " +currentDisplayEnable);
                mDisplayEnablePref[dispid].setChecked(currentDisplayEnable);
                mDisplayEnablePref[dispid].setOnPreferenceChangeListener(this);
                
                boolean currentDisplayMirror = mDisplayManager.getDisplayMirror(dispid);
                if(DBG) Log.w(TAG,"currentDisplayMirror " +currentDisplayMirror);
                mMirrorPref[dispid].setChecked(currentDisplayMirror);
                mMirrorPref[dispid].setEnabled(false);
                mMirrorPref[dispid].setOnPreferenceChangeListener(this);
                
                boolean currentDisplayRotation = mDisplayManager.getDisplayRotation(dispid);
                if(DBG) Log.w(TAG,"currentDisplayRotation " +currentDisplayRotation);
                mRotationPref[dispid].setChecked(currentDisplayRotation);                       
                mRotationPref[dispid].setOnPreferenceChangeListener(this);
                mRotationPref[dispid].setEnabled(false);
                
                int currentDisplayOverScan = mDisplayManager.getDisplayOverScan(dispid);
                if(DBG) Log.w(TAG,"currentDisplayOverScan " +currentDisplayOverScan);
                mOverScanPref[dispid].setProgress(currentDisplayOverScan);
                mOverScanPref[dispid].setOnPreferenceChangeListener(this);
                //mOverScanPref[dispid].setEnabled(false);
                
                int currentDisplayColorDepth = mDisplayManager.getDisplayColorDepth(dispid);
                if(DBG) Log.w(TAG,"currentDisplayColorDepth " +currentDisplayColorDepth);
                mColorDepthPref[dispid].setValue(String.valueOf(currentDisplayColorDepth));
                mColorDepthPref[dispid].setOnPreferenceChangeListener(this);
                updateDisplayColorDepthPreferenceDescription(dispid, currentDisplayColorDepth);           

            // set preference entry and value;
        } else {
        
            // delete the preferenc entry and value;
            ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
            ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();              
            mDisplayModePref[dispid].setEntries(
                revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            mDisplayModePref[dispid].setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));     
            mDisplayModePref[dispid].setOnPreferenceChangeListener(this);

            getPreferenceScreen().removePreference(mDisplayModePref[dispid]);
            getPreferenceScreen().removePreference(mDisplayEnablePref[dispid]);
            getPreferenceScreen().removePreference(mMirrorPref[dispid]);
            getPreferenceScreen().removePreference(mOverScanPref[dispid]);
            getPreferenceScreen().removePreference(mRotationPref[dispid]);    
            getPreferenceScreen().removePreference(mColorDepthPref[dispid]);
            getPreferenceScreen().removePreference(mCategoryPref[dispid]);
        }
    }
    
    private void handleDisplayStateChanged(int dispid, int state) {
        if(DBG) Log.w(TAG, "handleDisplayStateChanged");
        if (dispid < 0) {
            Log.w(TAG, "dispid is no valid");
            return;
        }
        
    }

    private void showRebootDialog() {
    
        CharSequence msg = getResources().getText(R.string.alarm_reboot_msg);
        mConfirmDialog = new AlertDialog.Builder(getActivity()).setMessage(msg)
                .setTitle(R.string.alarm_reboot_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .show();
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            Log.w(TAG," changed the setting, ok to reboot");
            mDisplayManager.rebootSystem();
        } else {
            Log.w(TAG," changed the setting, but cancel to reboot");
        }
    }
        
}


