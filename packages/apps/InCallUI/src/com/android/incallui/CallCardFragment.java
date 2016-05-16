/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.incallui;

import android.animation.LayoutTransition;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.services.telephony.common.Call;

import java.util.List;

/**
 * Fragment for call card.
 */
public class CallCardFragment extends BaseFragment<CallCardPresenter, CallCardPresenter.CallCardUi>
        implements CallCardPresenter.CallCardUi {

    // Primary caller info
    private TextView mPhoneNumber;
    private TextView mNumberLabel;
    private TextView mPrimaryName;
    private TextView mCallStateLabel;
    private TextView mCallTypeLabel;
    private ImageView mPhoto;
    private TextView mElapsedTime;
    private View mProviderInfo;
    private TextView mProviderLabel;
    private TextView mProviderNumber;
    private ViewGroup mSupplementaryInfoContainer;

    // Secondary caller info
    private ViewStub mSecondaryCallInfo;
    private TextView mSecondaryCallName;
    private ImageView mSecondaryPhoto;
    private View mSecondaryPhotoOverlay;

    // Cached DisplayMetrics density.
    private float mDensity;

    @Override
    CallCardPresenter.CallCardUi getUi() {
        return this;
    }

    @Override
    CallCardPresenter createPresenter() {
        return new CallCardPresenter();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final CallList calls = CallList.getInstance();
        final Call call = calls.getFirstCall();
        getPresenter().init(getActivity(), call);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mDensity = getResources().getDisplayMetrics().density;

        return inflater.inflate(R.layout.call_card, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPhoneNumber = (TextView) view.findViewById(R.id.phoneNumber);
        mPrimaryName = (TextView) view.findViewById(R.id.name);
        mNumberLabel = (TextView) view.findViewById(R.id.label);
        mSecondaryCallInfo = (ViewStub) view.findViewById(R.id.secondary_call_info);
        mPhoto = (ImageView) view.findViewById(R.id.photo);
        mCallStateLabel = (TextView) view.findViewById(R.id.callStateLabel);
        mCallTypeLabel = (TextView) view.findViewById(R.id.callTypeLabel);
        mElapsedTime = (TextView) view.findViewById(R.id.elapsedTime);
        mProviderInfo = view.findViewById(R.id.providerInfo);
        mProviderLabel = (TextView) view.findViewById(R.id.providerLabel);
        mProviderNumber = (TextView) view.findViewById(R.id.providerAddress);
        mSupplementaryInfoContainer =
            (ViewGroup) view.findViewById(R.id.supplementary_info_container);
    }

    @Override
    public void setVisible(boolean on) {
        if (on) {
            getView().setVisibility(View.VISIBLE);
        } else {
            getView().setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void setPrimaryName(String name, boolean nameIsNumber) {
        if (TextUtils.isEmpty(name)) {
            mPrimaryName.setText("");
        } else {
            mPrimaryName.setText(name);

            // Set direction of the name field
            int nameDirection = View.TEXT_DIRECTION_INHERIT;
            if (nameIsNumber) {
                nameDirection = View.TEXT_DIRECTION_LTR;
            }
            mPrimaryName.setTextDirection(nameDirection);
        }
    }

    @Override
    public void setPrimaryImage(Drawable image) {
        if (image != null) {
            setDrawableToImageView(mPhoto, image);
        }
    }

    @Override
    public void setPrimaryPhoneNumber(String number) {
        // Set the number
        if (TextUtils.isEmpty(number)) {
            mPhoneNumber.setText("");
            mPhoneNumber.setVisibility(View.GONE);
        } else {
            mPhoneNumber.setText(number);
            mPhoneNumber.setVisibility(View.VISIBLE);
            mPhoneNumber.setTextDirection(View.TEXT_DIRECTION_LTR);
        }
    }

    @Override
    public void setPrimaryLabel(String label) {
        if (!TextUtils.isEmpty(label)) {
            mNumberLabel.setText(label);
            mNumberLabel.setVisibility(View.VISIBLE);
        } else {
            mNumberLabel.setVisibility(View.GONE);
        }

    }

    @Override
    public void setPrimary(String number, String name, boolean nameIsNumber, String label,
            Drawable photo, boolean isConference, boolean isGeneric, boolean isSipCall) {
        Log.d(this, "Setting primary call");

        if (isConference) {
            name = getConferenceString(isGeneric);
            photo = getConferencePhoto(isGeneric);
            nameIsNumber = false;
        }

        setPrimaryPhoneNumber(number);

        // set the name field.
        setPrimaryName(name, nameIsNumber);

        // Set the label (Mobile, Work, etc)
        setPrimaryLabel(label);

        showInternetCallLabel(isSipCall);

        setDrawableToImageView(mPhoto, photo);
    }

    @Override
    public void setSecondary(boolean show, String name, boolean nameIsNumber, String label,
            Drawable photo, boolean isConference, boolean isGeneric) {

        if (show) {
            if (isConference) {
                name = getConferenceString(isGeneric);
                photo = getConferencePhoto(isGeneric);
                nameIsNumber = false;
            }

            showAndInitializeSecondaryCallInfo();
            mSecondaryCallName.setText(name);

            int nameDirection = View.TEXT_DIRECTION_INHERIT;
            if (nameIsNumber) {
                nameDirection = View.TEXT_DIRECTION_LTR;
            }
            mSecondaryCallName.setTextDirection(nameDirection);

            setDrawableToImageView(mSecondaryPhoto, photo);
        } else {
            mSecondaryCallInfo.setVisibility(View.GONE);
        }
    }

    @Override
    public void setSecondaryImage(Drawable image) {
        if (image != null) {
            setDrawableToImageView(mSecondaryPhoto, image);
        }
    }

    @Override
    public void setCallState(int state, Call.DisconnectCause cause, boolean bluetoothOn,
            String gatewayLabel, String gatewayNumber) {
        String callStateLabel = null;

        // States other than disconnected not yet supported
        callStateLabel = getCallStateLabelFromState(state, cause);

        Log.v(this, "setCallState " + callStateLabel);
        Log.v(this, "DisconnectCause " + cause);
        Log.v(this, "bluetooth on " + bluetoothOn);
        Log.v(this, "gateway " + gatewayLabel + gatewayNumber);

        // There are cases where we totally skip the animation, in which case remove the transition
        // animation here and restore it afterwards.
        final boolean skipAnimation = (Call.State.isDialing(state)
                || state == Call.State.DISCONNECTED || state == Call.State.DISCONNECTING);
        LayoutTransition transition = null;
        if (skipAnimation) {
            transition = mSupplementaryInfoContainer.getLayoutTransition();
            mSupplementaryInfoContainer.setLayoutTransition(null);
        }

        // Update the call state label.
        if (!TextUtils.isEmpty(callStateLabel)) {
            mCallStateLabel.setVisibility(View.VISIBLE);
            mCallStateLabel.setText(callStateLabel);

            if (Call.State.INCOMING == state) {
                setBluetoothOn(bluetoothOn);
            }
        } else {
            mCallStateLabel.setVisibility(View.GONE);
            // Gravity is aligned left when receiving an incoming call in landscape.
            // In that rare case, the gravity needs to be reset to the right.
            // Also, setText("") is used since there is a delay in making the view GONE,
            // so the user will otherwise see the text jump to the right side before disappearing.
            if(mCallStateLabel.getGravity() != Gravity.END) {
                mCallStateLabel.setText("");
                mCallStateLabel.setGravity(Gravity.END);
            }
        }

        // Provider info: (e.g. "Calling via <gatewayLabel>")
        if (!TextUtils.isEmpty(gatewayLabel) && !TextUtils.isEmpty(gatewayNumber)) {
            mProviderLabel.setText(gatewayLabel);
            mProviderNumber.setText(gatewayNumber);
            mProviderInfo.setVisibility(View.VISIBLE);
        } else {
            mProviderInfo.setVisibility(View.GONE);
        }

        // Restore the animation.
        if (skipAnimation) {
            mSupplementaryInfoContainer.setLayoutTransition(transition);
        }
    }

    private void showInternetCallLabel(boolean show) {
        if (show) {
            final String label = getView().getContext().getString(
                    R.string.incall_call_type_label_sip);
            mCallTypeLabel.setVisibility(View.VISIBLE);
            mCallTypeLabel.setText(label);
        } else {
            mCallTypeLabel.setVisibility(View.GONE);
        }
    }

    @Override
    public void setPrimaryCallElapsedTime(boolean show, String callTimeElapsed) {
        if (show) {
            if (mElapsedTime.getVisibility() != View.VISIBLE) {
                AnimationUtils.Fade.show(mElapsedTime);
            }
            mElapsedTime.setText(callTimeElapsed);
        } else {
            // hide() animation has no effect if it is already hidden.
            AnimationUtils.Fade.hide(mElapsedTime, View.INVISIBLE);
        }
    }

    private void setDrawableToImageView(ImageView view, Drawable photo) {
        if (photo == null) {
            photo = view.getResources().getDrawable(R.drawable.picture_unknown);
        }

        final Drawable current = view.getDrawable();
        if (current == null) {
            view.setImageDrawable(photo);
            AnimationUtils.Fade.show(view);
        } else {
            AnimationUtils.startCrossFade(view, current, photo);
            view.setVisibility(View.VISIBLE);
        }
    }

    private String getConferenceString(boolean isGeneric) {
        Log.v(this, "isGenericString: " + isGeneric);
        final int resId = isGeneric ? R.string.card_title_in_call : R.string.card_title_conf_call;
        return getView().getResources().getString(resId);
    }

    private Drawable getConferencePhoto(boolean isGeneric) {
        Log.v(this, "isGenericPhoto: " + isGeneric);
        final int resId = isGeneric ? R.drawable.picture_dialing : R.drawable.picture_conference;
        return getView().getResources().getDrawable(resId);
    }

    private void setBluetoothOn(boolean onOff) {
        // Also, display a special icon (alongside the "Incoming call"
        // label) if there's an incoming call and audio will be routed
        // to bluetooth when you answer it.
        final int bluetoothIconId = R.drawable.ic_in_call_bt_dk;

        if (onOff) {
            mCallStateLabel.setCompoundDrawablesWithIntrinsicBounds(bluetoothIconId, 0, 0, 0);
            mCallStateLabel.setCompoundDrawablePadding((int) (mDensity * 5));
        } else {
            // Clear out any icons
            mCallStateLabel.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    /**
     * Gets the call state label based on the state of the call and
     * cause of disconnect
     */
    private String getCallStateLabelFromState(int state, Call.DisconnectCause cause) {
        final Context context = getView().getContext();
        String callStateLabel = null;  // Label to display as part of the call banner

        if (Call.State.IDLE == state) {
            // "Call state" is meaningless in this state.

        } else if (Call.State.ACTIVE == state) {
            // We normally don't show a "call state label" at all in
            // this state (but see below for some special cases).

        } else if (Call.State.ONHOLD == state) {
            callStateLabel = context.getString(R.string.card_title_on_hold);
        } else if (Call.State.DIALING == state) {
            callStateLabel = context.getString(R.string.card_title_dialing);
        } else if (Call.State.REDIALING == state) {
            callStateLabel = context.getString(R.string.card_title_redialing);
        } else if (Call.State.INCOMING == state || Call.State.CALL_WAITING == state) {
            callStateLabel = context.getString(R.string.card_title_incoming_call);

        } else if (Call.State.DISCONNECTING == state) {
            // While in the DISCONNECTING state we display a "Hanging up"
            // message in order to make the UI feel more responsive.  (In
            // GSM it's normal to see a delay of a couple of seconds while
            // negotiating the disconnect with the network, so the "Hanging
            // up" state at least lets the user know that we're doing
            // something.  This state is currently not used with CDMA.)
            callStateLabel = context.getString(R.string.card_title_hanging_up);

        } else if (Call.State.DISCONNECTED == state) {
            callStateLabel = getCallFailedString(cause);

        } else {
            Log.wtf(this, "updateCallStateWidgets: unexpected call: " + state);
        }

        return callStateLabel;
    }

    /**
     * Maps the disconnect cause to a resource string.
     */
    private String getCallFailedString(Call.DisconnectCause cause) {
        int resID = R.string.card_title_call_ended;

        // TODO: The card *title* should probably be "Call ended" in all
        // cases, but if the DisconnectCause was an error condition we should
        // probably also display the specific failure reason somewhere...

        switch (cause) {
            case BUSY:
                resID = R.string.callFailed_userBusy;
                break;

            case CONGESTION:
                resID = R.string.callFailed_congestion;
                break;

            case TIMED_OUT:
                resID = R.string.callFailed_timedOut;
                break;

            case SERVER_UNREACHABLE:
                resID = R.string.callFailed_server_unreachable;
                break;

            case NUMBER_UNREACHABLE:
                resID = R.string.callFailed_number_unreachable;
                break;

            case INVALID_CREDENTIALS:
                resID = R.string.callFailed_invalid_credentials;
                break;

            case SERVER_ERROR:
                resID = R.string.callFailed_server_error;
                break;

            case OUT_OF_NETWORK:
                resID = R.string.callFailed_out_of_network;
                break;

            case LOST_SIGNAL:
            case CDMA_DROP:
                resID = R.string.callFailed_noSignal;
                break;

            case LIMIT_EXCEEDED:
                resID = R.string.callFailed_limitExceeded;
                break;

            case POWER_OFF:
                resID = R.string.callFailed_powerOff;
                break;

            case ICC_ERROR:
                resID = R.string.callFailed_simError;
                break;

            case OUT_OF_SERVICE:
                resID = R.string.callFailed_outOfService;
                break;

            case INVALID_NUMBER:
            case UNOBTAINABLE_NUMBER:
                resID = R.string.callFailed_unobtainable_number;
                break;

            default:
                resID = R.string.card_title_call_ended;
                break;
        }
        return this.getView().getContext().getString(resID);
    }

    private void showAndInitializeSecondaryCallInfo() {
        mSecondaryCallInfo.setVisibility(View.VISIBLE);

        // mSecondaryCallName is initialized here (vs. onViewCreated) because it is inaccesible
        // until mSecondaryCallInfo is inflated in the call above.
        if (mSecondaryCallName == null) {
            mSecondaryCallName = (TextView) getView().findViewById(R.id.secondaryCallName);
        }
        if (mSecondaryPhoto == null) {
            mSecondaryPhoto = (ImageView) getView().findViewById(R.id.secondaryCallPhoto);
        }

        if (mSecondaryPhotoOverlay == null) {
            mSecondaryPhotoOverlay = getView().findViewById(R.id.dim_effect_for_secondary_photo);
            mSecondaryPhotoOverlay.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    getPresenter().secondaryPhotoClicked();
                }
            });
            mSecondaryPhotoOverlay.setOnTouchListener(new SmallerHitTargetTouchListener());
        }
    }

    public void dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            dispatchPopulateAccessibilityEvent(event, mPrimaryName);
            dispatchPopulateAccessibilityEvent(event, mPhoneNumber);
            return;
        }
        dispatchPopulateAccessibilityEvent(event, mCallStateLabel);
        dispatchPopulateAccessibilityEvent(event, mPrimaryName);
        dispatchPopulateAccessibilityEvent(event, mPhoneNumber);
        dispatchPopulateAccessibilityEvent(event, mCallTypeLabel);
        dispatchPopulateAccessibilityEvent(event, mSecondaryCallName);

        return;
    }

    private void dispatchPopulateAccessibilityEvent(AccessibilityEvent event, View view) {
        if (view == null) return;
        final List<CharSequence> eventText = event.getText();
        int size = eventText.size();
        view.dispatchPopulateAccessibilityEvent(event);
        // if no text added write null to keep relative position
        if (size == eventText.size()) {
            eventText.add(null);
        }
    }
}
