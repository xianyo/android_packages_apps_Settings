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

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.ContentResolver;
import android.net.NetworkInfo.DetailedState;
import android.os.Bundle;
import android.security.Credentials;
import android.security.KeyStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import android.provider.Settings;

class PppoeDialog extends AlertDialog implements View.OnClickListener,
        TextWatcher, AdapterView.OnItemSelectedListener {

    static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;
    static final int BUTTON_FORGET = DialogInterface.BUTTON_NEUTRAL;

    private final DialogInterface.OnClickListener mListener;

    private View mView;
    private TextView mSsid;
    private int mSecurity;
    private TextView mUsername;
    private TextView mPassword;
    
    PppoeDialog(Context context, DialogInterface.OnClickListener listener ) {
        super(context);
        mListener = listener;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.pppoe_dialog, null);
        setView(mView);
        setInverseBackgroundForced(true);

        Context context = getContext();
        Resources resources = context.getResources();

        final ContentResolver cr = context.getContentResolver();
        
        mUsername = (TextView) mView.findViewById(R.id.username);
        mPassword = (TextView) mView.findViewById(R.id.password);
        
        mUsername.setText(Settings.Secure.getString(cr, Settings.Secure.PPPOE_USERNAME));
        mPassword.setText(Settings.Secure.getString(cr, Settings.Secure.PPPOE_PASSWORD));
        
        
        mUsername.addTextChangedListener(this);
        mPassword.addTextChangedListener(this);
        
        ((CheckBox) mView.findViewById(R.id.show_password)).setOnClickListener(this);
            
        setButton(BUTTON_SUBMIT, context.getString(R.string.pppoe_save), mListener);

        setButton(DialogInterface.BUTTON_NEGATIVE,
                context.getString(R.string.pppoe_cancel), mListener);
        
        setButton(BUTTON_FORGET, context.getString(R.string.pppoe_forget), mListener);
                    
        super.onCreate(savedInstanceState);
        
        if (getButton(BUTTON_SUBMIT) != null) {
            validate();
        }
    }
    
    private void validate() {
        // TODO: make sure this is complete.
        if(mPassword.length() == 0 || mUsername.length() == 0) {
            getButton(BUTTON_SUBMIT).setEnabled(false);
        } else {
            getButton(BUTTON_SUBMIT).setEnabled(true);
        }
    }

    public void onClick(View view) {
        mPassword.setInputType(
                InputType.TYPE_CLASS_TEXT | (((CheckBox) view).isChecked() ?
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }
    
    public void afterTextChanged(Editable editable) {
        if (getButton(BUTTON_SUBMIT) != null) {
            validate();
        }
    }

    public void onItemSelected(AdapterView parent, View view, int position, long id) {
        mSecurity = position;
    }

    public void onNothingSelected(AdapterView parent) {
    }

    public String getUsername(){
        return mUsername.getText().toString();
    }

    public String getPassword(){
        return mPassword.getText().toString(); 
    }
    
}
