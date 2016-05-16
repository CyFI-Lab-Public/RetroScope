/**
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

package com.android.phone;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.sip.SipProfileDb;
import com.android.phone.sip.SipSettings;
import com.android.phone.sip.SipSharedPreferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.List;

/**
 * Activity that selects the proper phone type for an outgoing call.
 *
 * This activity determines which Phone type (SIP or PSTN) should be used
 * for an outgoing phone call, depending on the outgoing "number" (which
 * may be either a PSTN number or a SIP address) as well as the user's SIP
 * preferences.  In some cases this activity has no interaction with the
 * user, but in other cases it may (by bringing up a dialog if the user's
 * preference is "Ask for each call".)
 */
public class SipCallOptionHandler extends Activity implements
        DialogInterface.OnClickListener, DialogInterface.OnCancelListener,
        CompoundButton.OnCheckedChangeListener {
    static final String TAG = "SipCallOptionHandler";
    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);

    static final int DIALOG_SELECT_PHONE_TYPE = 0;
    static final int DIALOG_SELECT_OUTGOING_SIP_PHONE = 1;
    static final int DIALOG_START_SIP_SETTINGS = 2;
    static final int DIALOG_NO_INTERNET_ERROR = 3;
    static final int DIALOG_NO_VOIP = 4;
    static final int DIALOG_SIZE = 5;

    private Intent mIntent;
    private List<SipProfile> mProfileList;
    private String mCallOption;
    private String mNumber;
    private SipSharedPreferences mSipSharedPreferences;
    private SipProfileDb mSipProfileDb;
    private Dialog[] mDialogs = new Dialog[DIALOG_SIZE];
    private SipProfile mOutgoingSipProfile;
    private TextView mUnsetPriamryHint;
    private boolean mUseSipPhone = false;
    private boolean mMakePrimary = false;

    private static final int EVENT_DELAYED_FINISH = 1;

    private static final int DELAYED_FINISH_TIME = 2000; // msec

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == EVENT_DELAYED_FINISH) {
                finish();
            } else {
                Log.wtf(TAG, "Unknown message id: " + msg.what);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();

        // This activity is only ever launched with the
        // ACTION_SIP_SELECT_PHONE action.
        if (!OutgoingCallBroadcaster.ACTION_SIP_SELECT_PHONE.equals(action)) {
            Log.wtf(TAG, "onCreate: got intent action '" + action + "', expected "
                    + OutgoingCallBroadcaster.ACTION_SIP_SELECT_PHONE);
            finish();
            return;
        }

        // mIntent is a copy of the original CALL intent that started the
        // whole outgoing-call sequence.  This intent will ultimately be
        // passed to CallController.placeCall() after displaying the SIP
        // call options dialog (if necessary).
        mIntent = (Intent) intent.getParcelableExtra(OutgoingCallBroadcaster.EXTRA_NEW_CALL_INTENT);
        if (mIntent == null) {
            finish();
            return;
        }

        // Allow this activity to be visible in front of the keyguard.
        // (This is only necessary for obscure scenarios like the user
        // initiating a call and then immediately pressing the Power
        // button.)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        // If we're trying to make a SIP call, return a SipPhone if one is
        // available.
        //
        // - If it's a sip: URI, this is definitely a SIP call, regardless
        //   of whether the data is a SIP address or a regular phone
        //   number.
        //
        // - If this is a tel: URI but the data contains an "@" character
        //   (see PhoneNumberUtils.isUriNumber()) we consider that to be a
        //   SIP number too.
        //
        // TODO: Eventually we may want to disallow that latter case
        //       (e.g. "tel:foo@example.com").
        //
        // TODO: We should also consider moving this logic into the
        //       CallManager, where it could be made more generic.
        //       (For example, each "telephony provider" could be allowed
        //       to register the URI scheme(s) that it can handle, and the
        //       CallManager would then find the best match for every
        //       outgoing call.)

        boolean voipSupported = PhoneUtils.isVoipSupported();
        if (DBG) Log.v(TAG, "voipSupported: " + voipSupported);
        mSipProfileDb = new SipProfileDb(this);
        mSipSharedPreferences = new SipSharedPreferences(this);
        mCallOption = mSipSharedPreferences.getSipCallOption();
        if (DBG) Log.v(TAG, "Call option: " + mCallOption);
        Uri uri = mIntent.getData();
        String scheme = uri.getScheme();
        mNumber = PhoneNumberUtils.getNumberFromIntent(mIntent, this);
        boolean isInCellNetwork = PhoneGlobals.getInstance().phoneMgr.isRadioOn();
        boolean isKnownCallScheme = Constants.SCHEME_TEL.equals(scheme)
                || Constants.SCHEME_SIP.equals(scheme);
        boolean isRegularCall = Constants.SCHEME_TEL.equals(scheme)
                && !PhoneNumberUtils.isUriNumber(mNumber);

        // Bypass the handler if the call scheme is not sip or tel.
        if (!isKnownCallScheme) {
            setResultAndFinish();
            return;
        }

        // Check if VoIP feature is supported.
        if (!voipSupported) {
            if (!isRegularCall) {
                showDialog(DIALOG_NO_VOIP);
            } else {
                setResultAndFinish();
            }
            return;
        }

        // Since we are not sure if anyone has touched the number during
        // the NEW_OUTGOING_CALL broadcast, we just check if the provider
        // put their gateway information in the intent. If so, it means
        // someone has changed the destination number. We then make the
        // call via the default pstn network. However, if one just alters
        // the destination directly, then we still let it go through the
        // Internet call option process.
        if (!CallGatewayManager.hasPhoneProviderExtras(mIntent)) {
            if (!isNetworkConnected()) {
                if (!isRegularCall) {
                    showDialog(DIALOG_NO_INTERNET_ERROR);
                    return;
                }
            } else {
                if (mCallOption.equals(Settings.System.SIP_ASK_ME_EACH_TIME)
                        && isRegularCall && isInCellNetwork) {
                    showDialog(DIALOG_SELECT_PHONE_TYPE);
                    return;
                }
                if (!mCallOption.equals(Settings.System.SIP_ADDRESS_ONLY)
                        || !isRegularCall) {
                    mUseSipPhone = true;
                }
            }
        }

        if (mUseSipPhone) {
            // If there is no sip profile and it is a regular call, then we
            // should use pstn network instead.
            if ((mSipProfileDb.getProfilesCount() > 0) || !isRegularCall) {
                startGetPrimarySipPhoneThread();
                return;
            } else {
                mUseSipPhone = false;
            }
        }
        setResultAndFinish();
    }

    /**
     * Starts a delayed finish() in order to give the UI
     * some time to start up.
     */
    private void startDelayedFinish() {
        mHandler.sendEmptyMessageDelayed(EVENT_DELAYED_FINISH, DELAYED_FINISH_TIME);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isFinishing()) return;
        for (Dialog dialog : mDialogs) {
            if (dialog != null) dialog.dismiss();
        }
        finish();
    }

    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch(id) {
        case DIALOG_SELECT_PHONE_TYPE:
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.pick_outgoing_call_phone_type)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setSingleChoiceItems(R.array.phone_type_values, -1, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .setOnCancelListener(this)
                    .create();
            break;
        case DIALOG_SELECT_OUTGOING_SIP_PHONE:
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.pick_outgoing_sip_phone)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setSingleChoiceItems(getProfileNameArray(), -1, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .setOnCancelListener(this)
                    .create();
            addMakeDefaultCheckBox(dialog);
            break;
        case DIALOG_START_SIP_SETTINGS:
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.no_sip_account_found_title)
                    .setMessage(R.string.no_sip_account_found)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setPositiveButton(R.string.sip_menu_add, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .setOnCancelListener(this)
                    .create();
            break;
        case DIALOG_NO_INTERNET_ERROR:
            boolean wifiOnly = SipManager.isSipWifiOnly(this);
            dialog = new AlertDialog.Builder(this)
                    .setTitle(wifiOnly ? R.string.no_wifi_available_title
                                       : R.string.no_internet_available_title)
                    .setMessage(wifiOnly ? R.string.no_wifi_available
                                         : R.string.no_internet_available)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setPositiveButton(android.R.string.ok, this)
                    .setOnCancelListener(this)
                    .create();
            break;
        case DIALOG_NO_VOIP:
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.no_voip)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setPositiveButton(android.R.string.ok, this)
                    .setOnCancelListener(this)
                    .create();
            break;
        default:
            dialog = null;
        }
        if (dialog != null) {
            mDialogs[id] = dialog;
        }
        return dialog;
    }

    private void addMakeDefaultCheckBox(Dialog dialog) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(
                com.android.internal.R.layout.always_use_checkbox, null);
        CheckBox makePrimaryCheckBox =
                (CheckBox)view.findViewById(com.android.internal.R.id.alwaysUse);
        makePrimaryCheckBox.setText(R.string.remember_my_choice);
        makePrimaryCheckBox.setOnCheckedChangeListener(this);
        mUnsetPriamryHint = (TextView)view.findViewById(
                com.android.internal.R.id.clearDefaultHint);
        mUnsetPriamryHint.setText(R.string.reset_my_choice_hint);
        mUnsetPriamryHint.setVisibility(View.GONE);
        ((AlertDialog)dialog).setView(view);
    }

    private CharSequence[] getProfileNameArray() {
        CharSequence[] entries = new CharSequence[mProfileList.size()];
        int i = 0;
        for (SipProfile p : mProfileList) {
            entries[i++] = p.getProfileName();
        }
        return entries;
    }

    public void onClick(DialogInterface dialog, int id) {
        if (id == DialogInterface.BUTTON_NEGATIVE) {
            // button negative is cancel
            finish();
            return;
        } else if(dialog == mDialogs[DIALOG_SELECT_PHONE_TYPE]) {
            String selection = getResources().getStringArray(
                    R.array.phone_type_values)[id];
            if (DBG) Log.v(TAG, "User pick phone " + selection);
            if (selection.equals(getString(R.string.internet_phone))) {
                mUseSipPhone = true;
                startGetPrimarySipPhoneThread();
                return;
            }
        } else if (dialog == mDialogs[DIALOG_SELECT_OUTGOING_SIP_PHONE]) {
            mOutgoingSipProfile = mProfileList.get(id);
        } else if ((dialog == mDialogs[DIALOG_NO_INTERNET_ERROR])
                || (dialog == mDialogs[DIALOG_NO_VOIP])) {
            finish();
            return;
        } else {
            if (id == DialogInterface.BUTTON_POSITIVE) {
                // Redirect to sip settings and drop the call.
                Intent newIntent = new Intent(this, SipSettings.class);
                newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(newIntent);
            }
            finish();
            return;
        }
        setResultAndFinish();
    }

    public void onCancel(DialogInterface dialog) {
        finish();
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mMakePrimary = isChecked;
        if (isChecked) {
            mUnsetPriamryHint.setVisibility(View.VISIBLE);
        } else {
            mUnsetPriamryHint.setVisibility(View.INVISIBLE);
        }
    }

    private void createSipPhoneIfNeeded(SipProfile p) {
        CallManager cm = PhoneGlobals.getInstance().mCM;
        if (PhoneUtils.getSipPhoneFromUri(cm, p.getUriString()) != null) return;

        // Create the phone since we can not find it in CallManager
        try {
            SipManager.newInstance(this).open(p);
            Phone phone = PhoneFactory.makeSipPhone(p.getUriString());
            if (phone != null) {
                cm.registerPhone(phone);
            } else {
                Log.e(TAG, "cannot make sipphone profile" + p);
            }
        } catch (SipException e) {
            Log.e(TAG, "cannot open sip profile" + p, e);
        }
    }

    private void setResultAndFinish() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mOutgoingSipProfile != null) {
                    if (!isNetworkConnected()) {
                        showDialog(DIALOG_NO_INTERNET_ERROR);
                        return;
                    }
                    if (DBG) Log.v(TAG, "primary SIP URI is " +
                            mOutgoingSipProfile.getUriString());
                    createSipPhoneIfNeeded(mOutgoingSipProfile);
                    mIntent.putExtra(OutgoingCallBroadcaster.EXTRA_SIP_PHONE_URI,
                            mOutgoingSipProfile.getUriString());
                    if (mMakePrimary) {
                        mSipSharedPreferences.setPrimaryAccount(
                                mOutgoingSipProfile.getUriString());
                    }
                }

                if (mUseSipPhone && mOutgoingSipProfile == null) {
                    showDialog(DIALOG_START_SIP_SETTINGS);
                    return;
                } else {
                    // Woo hoo -- it's finally OK to initiate the outgoing call!
                    PhoneGlobals.getInstance().callController.placeCall(mIntent);
                }
                startDelayedFinish();
            }
        });
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if ((ni == null) || !ni.isConnected()) return false;

            return ((ni.getType() == ConnectivityManager.TYPE_WIFI)
                    || !SipManager.isSipWifiOnly(this));
        }
        return false;
    }

    private void startGetPrimarySipPhoneThread() {
        new Thread(new Runnable() {
            public void run() {
                getPrimarySipPhone();
            }
        }).start();
    }

    private void getPrimarySipPhone() {
        String primarySipUri = mSipSharedPreferences.getPrimaryAccount();

        mOutgoingSipProfile = getPrimaryFromExistingProfiles(primarySipUri);
        if (mOutgoingSipProfile == null) {
            if ((mProfileList != null) && (mProfileList.size() > 0)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        showDialog(DIALOG_SELECT_OUTGOING_SIP_PHONE);
                    }
                });
                return;
            }
        }
        setResultAndFinish();
    }

    private SipProfile getPrimaryFromExistingProfiles(String primarySipUri) {
        mProfileList = mSipProfileDb.retrieveSipProfileList();
        if (mProfileList == null) return null;
        for (SipProfile p : mProfileList) {
            if (p.getUriString().equals(primarySipUri)) return p;
        }
        return null;
    }
}
