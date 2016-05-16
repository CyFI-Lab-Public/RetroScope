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

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Looper;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.android.incallui.service.PhoneNumberService;
import com.android.incalluibind.ServiceFactory;
import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.CallIdentification;
import com.android.services.telephony.common.MoreStrings;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import com.google.android.collect.Sets;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Class responsible for querying Contact Information for Call objects. Can perform asynchronous
 * requests to the Contact Provider for information as well as respond synchronously for any data
 * that it currently has cached from previous queries. This class always gets called from the UI
 * thread so it does not need thread protection.
 */
public class ContactInfoCache implements ContactsAsyncHelper.OnImageLoadCompleteListener {

    private static final String TAG = ContactInfoCache.class.getSimpleName();
    private static final int TOKEN_UPDATE_PHOTO_FOR_CALL_STATE = 0;

    private final Context mContext;
    private final PhoneNumberService mPhoneNumberService;
    private final HashMap<Integer, ContactCacheEntry> mInfoMap = Maps.newHashMap();
    private final HashMap<Integer, Set<ContactInfoCacheCallback>> mCallBacks = Maps.newHashMap();

    private static ContactInfoCache sCache = null;

    public static synchronized ContactInfoCache getInstance(Context mContext) {
        if (sCache == null) {
            sCache = new ContactInfoCache(mContext);
        }
        return sCache;
    }

    private ContactInfoCache(Context context) {
        mContext = context;
        mPhoneNumberService = ServiceFactory.newPhoneNumberService(context);
    }

    public ContactCacheEntry getInfo(int callId) {
        return mInfoMap.get(callId);
    }

    public static ContactCacheEntry buildCacheEntryFromCall(Context context,
            CallIdentification identification, boolean isIncoming) {
        final ContactCacheEntry entry = new ContactCacheEntry();

        // TODO: get rid of caller info.
        final CallerInfo info = CallerInfoUtils.buildCallerInfo(context, identification);
        ContactInfoCache.populateCacheEntry(context, info, entry,
                identification.getNumberPresentation(), isIncoming);
        return entry;
    }

    private class FindInfoCallback implements CallerInfoAsyncQuery.OnQueryCompleteListener {
        private final boolean mIsIncoming;

        public FindInfoCallback(boolean isIncoming) {
            mIsIncoming = isIncoming;
        }

        @Override
        public void onQueryComplete(int token, Object cookie, CallerInfo callerInfo) {
            final CallIdentification identification = (CallIdentification) cookie;
            findInfoQueryComplete(identification, callerInfo, mIsIncoming, true);
        }
    }

    /**
     * Requests contact data for the Call object passed in.
     * Returns the data through callback.  If callback is null, no response is made, however the
     * query is still performed and cached.
     *
     * @param identification The call identification
     * @param callback The function to call back when the call is found. Can be null.
     */
    public void findInfo(final CallIdentification identification, final boolean isIncoming,
            ContactInfoCacheCallback callback) {
        Preconditions.checkState(Looper.getMainLooper().getThread() == Thread.currentThread());
        Preconditions.checkNotNull(callback);

        final int callId = identification.getCallId();
        final ContactCacheEntry cacheEntry = mInfoMap.get(callId);
        Set<ContactInfoCacheCallback> callBacks = mCallBacks.get(callId);

        // If we have a previously obtained intermediate result return that now
        if (cacheEntry != null) {
            Log.d(TAG, "Contact lookup. In memory cache hit; lookup "
                    + (callBacks == null ? "complete" : "still running"));
            callback.onContactInfoComplete(callId, cacheEntry);
            // If no other callbacks are in flight, we're done.
            if (callBacks == null) {
                return;
            }
        }

        // If the entry already exists, add callback
        if (callBacks != null) {
            callBacks.add(callback);
            return;
        }
        Log.d(TAG, "Contact lookup. In memory cache miss; searching provider.");
        // New lookup
        callBacks = Sets.newHashSet();
        callBacks.add(callback);
        mCallBacks.put(callId, callBacks);

        /**
         * Performs a query for caller information.
         * Save any immediate data we get from the query. An asynchronous query may also be made
         * for any data that we do not already have. Some queries, such as those for voicemail and
         * emergency call information, will not perform an additional asynchronous query.
         */
        final CallerInfo callerInfo = CallerInfoUtils.getCallerInfoForCall(
                mContext, identification, new FindInfoCallback(isIncoming));

        findInfoQueryComplete(identification, callerInfo, isIncoming, false);
    }

    private void findInfoQueryComplete(CallIdentification identification,
            CallerInfo callerInfo, boolean isIncoming, boolean didLocalLookup) {
        final int callId = identification.getCallId();
        int presentationMode = identification.getNumberPresentation();
        if (callerInfo.contactExists || callerInfo.isEmergencyNumber() || callerInfo.isVoiceMailNumber()) {
            presentationMode = Call.PRESENTATION_ALLOWED;
        }

        final ContactCacheEntry cacheEntry = buildEntry(mContext, callId,
                callerInfo, presentationMode, isIncoming);

        // Add the contact info to the cache.
        mInfoMap.put(callId, cacheEntry);
        sendInfoNotifications(callId, cacheEntry);

        if (didLocalLookup) {
            if (!callerInfo.contactExists && cacheEntry.name == null &&
                    mPhoneNumberService != null) {
                Log.d(TAG, "Contact lookup. Local contacts miss, checking remote");
                final PhoneNumberServiceListener listener = new PhoneNumberServiceListener(callId);
                mPhoneNumberService.getPhoneNumberInfo(cacheEntry.number, listener, listener,
                        isIncoming);
            } else if (cacheEntry.personUri != null) {
                Log.d(TAG, "Contact lookup. Local contact found, starting image load");
                // Load the image with a callback to update the image state.
                // When the load is finished, onImageLoadComplete() will be called.
                ContactsAsyncHelper.startObtainPhotoAsync(TOKEN_UPDATE_PHOTO_FOR_CALL_STATE,
                        mContext, cacheEntry.personUri, ContactInfoCache.this, callId);
            } else {
                if (callerInfo.contactExists) {
                    Log.d(TAG, "Contact lookup done. Local contact found, no image.");
                } else if (cacheEntry.name != null) {
                    Log.d(TAG, "Contact lookup done. Special contact type.");
                } else {
                    Log.d(TAG, "Contact lookup done. Local contact not found and"
                            + " no remote lookup service available.");
                }
                clearCallbacks(callId);
            }
        }
    }

    class PhoneNumberServiceListener implements PhoneNumberService.NumberLookupListener,
                                     PhoneNumberService.ImageLookupListener {
        private final int mCallId;

        PhoneNumberServiceListener(int callId) {
            mCallId = callId;
        }

        @Override
        public void onPhoneNumberInfoComplete(
                final PhoneNumberService.PhoneNumberInfo info) {
            // If we got a miss, this is the end of the lookup pipeline,
            // so clear the callbacks and return.
            if (info == null) {
                Log.d(TAG, "Contact lookup done. Remote contact not found.");
                clearCallbacks(mCallId);
                return;
            }

            ContactCacheEntry entry = new ContactCacheEntry();
            entry.name = info.getDisplayName();
            entry.number = info.getNumber();
            final int type = info.getPhoneType();
            final String label = info.getPhoneLabel();
            if (type == Phone.TYPE_CUSTOM) {
                entry.label = label;
            } else {
                final CharSequence typeStr = Phone.getTypeLabel(
                        mContext.getResources(), type, label);
                entry.label = typeStr == null ? null : typeStr.toString();
            }
            final ContactCacheEntry oldEntry = mInfoMap.get(mCallId);
            if (oldEntry != null) {
                // Location is only obtained from local lookup so persist
                // the value for remote lookups. Once we have a name this
                // field is no longer used; it is persisted here in case
                // the UI is ever changed to use it.
                entry.location = oldEntry.location;
            }

            // If no image and it's a business, switch to using the default business avatar.
            if (info.getImageUrl() == null && info.isBusiness()) {
                Log.d(TAG, "Business has no image. Using default.");
                entry.photo = mContext.getResources().getDrawable(R.drawable.business_unknown);
            }

            // Add the contact info to the cache.
            mInfoMap.put(mCallId, entry);
            sendInfoNotifications(mCallId, entry);

            // If there is no image then we should not expect another callback.
            if (info.getImageUrl() == null) {
                // We're done, so clear callbacks
                clearCallbacks(mCallId);
            }
        }

        @Override
        public void onImageFetchComplete(Bitmap bitmap) {
            onImageLoadComplete(TOKEN_UPDATE_PHOTO_FOR_CALL_STATE, null,
                    bitmap, (Integer) mCallId);
        }
    }

    /**
     * Implemented for ContactsAsyncHelper.OnImageLoadCompleteListener interface.
     * make sure that the call state is reflected after the image is loaded.
     */
    @Override
    public void onImageLoadComplete(int token, Drawable photo, Bitmap photoIcon, Object cookie) {
        Log.d(this, "Image load complete with context: ", mContext);
        // TODO: may be nice to update the image view again once the newer one
        // is available on contacts database.

        final int callId = (Integer) cookie;
        final ContactCacheEntry entry = mInfoMap.get(callId);

        if (entry == null) {
            Log.e(this, "Image Load received for empty search entry.");
            clearCallbacks(callId);
            return;
        }
        Log.d(this, "setting photo for entry: ", entry);

        // Conference call icons are being handled in CallCardPresenter.
        if (photo != null) {
            Log.v(this, "direct drawable: ", photo);
            entry.photo = photo;
        } else if (photoIcon != null) {
            Log.v(this, "photo icon: ", photoIcon);
            entry.photo = new BitmapDrawable(mContext.getResources(), photoIcon);
        } else {
            Log.v(this, "unknown photo");
            entry.photo = null;
        }

        sendImageNotifications(callId, entry);
        clearCallbacks(callId);
    }

    /**
     * Blows away the stored cache values.
     */
    public void clearCache() {
        mInfoMap.clear();
        mCallBacks.clear();
    }

    private ContactCacheEntry buildEntry(Context context, int callId,
            CallerInfo info, int presentation, boolean isIncoming) {
        // The actual strings we're going to display onscreen:
        Drawable photo = null;

        final ContactCacheEntry cce = new ContactCacheEntry();
        populateCacheEntry(context, info, cce, presentation, isIncoming);

        // This will only be true for emergency numbers
        if (info.photoResource != 0) {
            photo = context.getResources().getDrawable(info.photoResource);
        } else if (info.isCachedPhotoCurrent) {
            if (info.cachedPhoto != null) {
                photo = info.cachedPhoto;
            } else {
                photo = context.getResources().getDrawable(R.drawable.picture_unknown);
            }
        } else if (info.person_id == 0) {
            photo = context.getResources().getDrawable(R.drawable.picture_unknown);
        } else {
            Uri personUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, info.person_id);
            Log.d(TAG, "- got personUri: '" + personUri + "', based on info.person_id: " +
                    info.person_id);

            if (personUri == null) {
                Log.v(TAG, "personUri is null. Just use unknown picture.");
                photo = context.getResources().getDrawable(R.drawable.picture_unknown);
            } else {
                cce.personUri = personUri;
            }
        }

        cce.photo = photo;
        return cce;
    }

    /**
     * Populate a cache entry from a caller identification (which got converted into a caller info).
     */
    public static void populateCacheEntry(Context context, CallerInfo info, ContactCacheEntry cce,
            int presentation, boolean isIncoming) {
        Preconditions.checkNotNull(info);
        String displayName = null;
        String displayNumber = null;
        String displayLocation = null;
        String label = null;
        boolean isSipCall = false;

            // It appears that there is a small change in behaviour with the
            // PhoneUtils' startGetCallerInfo whereby if we query with an
            // empty number, we will get a valid CallerInfo object, but with
            // fields that are all null, and the isTemporary boolean input
            // parameter as true.

            // In the past, we would see a NULL callerinfo object, but this
            // ends up causing null pointer exceptions elsewhere down the
            // line in other cases, so we need to make this fix instead. It
            // appears that this was the ONLY call to PhoneUtils
            // .getCallerInfo() that relied on a NULL CallerInfo to indicate
            // an unknown contact.

            // Currently, infi.phoneNumber may actually be a SIP address, and
            // if so, it might sometimes include the "sip:" prefix. That
            // prefix isn't really useful to the user, though, so strip it off
            // if present. (For any other URI scheme, though, leave the
            // prefix alone.)
            // TODO: It would be cleaner for CallerInfo to explicitly support
            // SIP addresses instead of overloading the "phoneNumber" field.
            // Then we could remove this hack, and instead ask the CallerInfo
            // for a "user visible" form of the SIP address.
            String number = info.phoneNumber;

            if (!TextUtils.isEmpty(number)) {
                isSipCall = PhoneNumberUtils.isUriNumber(number);
                if (number.startsWith("sip:")) {
                    number = number.substring(4);
                }
            }

            if (TextUtils.isEmpty(info.name)) {
                // No valid "name" in the CallerInfo, so fall back to
                // something else.
                // (Typically, we promote the phone number up to the "name" slot
                // onscreen, and possibly display a descriptive string in the
                // "number" slot.)
                if (TextUtils.isEmpty(number)) {
                    // No name *or* number! Display a generic "unknown" string
                    // (or potentially some other default based on the presentation.)
                    displayName = getPresentationString(context, presentation);
                    Log.d(TAG, "  ==> no name *or* number! displayName = " + displayName);
                } else if (presentation != Call.PRESENTATION_ALLOWED) {
                    // This case should never happen since the network should never send a phone #
                    // AND a restricted presentation. However we leave it here in case of weird
                    // network behavior
                    displayName = getPresentationString(context, presentation);
                    Log.d(TAG, "  ==> presentation not allowed! displayName = " + displayName);
                } else if (!TextUtils.isEmpty(info.cnapName)) {
                    // No name, but we do have a valid CNAP name, so use that.
                    displayName = info.cnapName;
                    info.name = info.cnapName;
                    displayNumber = number;
                    Log.d(TAG, "  ==> cnapName available: displayName '" + displayName +
                            "', displayNumber '" + displayNumber + "'");
                } else {
                    // No name; all we have is a number. This is the typical
                    // case when an incoming call doesn't match any contact,
                    // or if you manually dial an outgoing number using the
                    // dialpad.
                    displayNumber = number;

                    // Display a geographical description string if available
                    // (but only for incoming calls.)
                    if (isIncoming) {
                        // TODO (CallerInfoAsyncQuery cleanup): Fix the CallerInfo
                        // query to only do the geoDescription lookup in the first
                        // place for incoming calls.
                        displayLocation = info.geoDescription; // may be null
                        Log.d(TAG, "Geodescrption: " + info.geoDescription);
                    }

                    Log.d(TAG, "  ==>  no name; falling back to number:"
                            + " displayNumber '" + displayNumber
                            + "', displayLocation '" + displayLocation + "'");
                }
            } else {
                // We do have a valid "name" in the CallerInfo. Display that
                // in the "name" slot, and the phone number in the "number" slot.
                if (presentation != Call.PRESENTATION_ALLOWED) {
                    // This case should never happen since the network should never send a name
                    // AND a restricted presentation. However we leave it here in case of weird
                    // network behavior
                    displayName = getPresentationString(context, presentation);
                    Log.d(TAG, "  ==> valid name, but presentation not allowed!" +
                            " displayName = " + displayName);
                } else {
                    displayName = info.name;
                    displayNumber = number;
                    label = info.phoneLabel;
                    Log.d(TAG, "  ==>  name is present in CallerInfo: displayName '" + displayName
                            + "', displayNumber '" + displayNumber + "'");
                }
            }

        cce.name = displayName;
        cce.number = displayNumber;
        cce.location = displayLocation;
        cce.label = label;
        cce.isSipCall = isSipCall;
    }

    /**
     * Sends the updated information to call the callbacks for the entry.
     */
    private void sendInfoNotifications(int callId, ContactCacheEntry entry) {
        final Set<ContactInfoCacheCallback> callBacks = mCallBacks.get(callId);
        if (callBacks != null) {
            for (ContactInfoCacheCallback callBack : callBacks) {
                callBack.onContactInfoComplete(callId, entry);
            }
        }
    }

    private void sendImageNotifications(int callId, ContactCacheEntry entry) {
        final Set<ContactInfoCacheCallback> callBacks = mCallBacks.get(callId);
        if (callBacks != null && entry.photo != null) {
            for (ContactInfoCacheCallback callBack : callBacks) {
                callBack.onImageLoadComplete(callId, entry);
            }
        }
    }

    private void clearCallbacks(int callId) {
        mCallBacks.remove(callId);
    }

    /**
     * Gets name strings based on some special presentation modes.
     */
    private static String getPresentationString(Context context, int presentation) {
        String name = context.getString(R.string.unknown);
        if (presentation == Call.PRESENTATION_RESTRICTED) {
            name = context.getString(R.string.private_num);
        } else if (presentation == Call.PRESENTATION_PAYPHONE) {
            name = context.getString(R.string.payphone);
        }
        return name;
    }

    /**
     * Callback interface for the contact query.
     */
    public interface ContactInfoCacheCallback {
        public void onContactInfoComplete(int callId, ContactCacheEntry entry);
        public void onImageLoadComplete(int callId, ContactCacheEntry entry);
    }

    public static class ContactCacheEntry {
        public String name;
        public String number;
        public String location;
        public String label;
        public Drawable photo;
        public boolean isSipCall;
        public Uri personUri; // Used for local photo load

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("name", MoreStrings.toSafeString(name))
                    .add("number", MoreStrings.toSafeString(number))
                    .add("location", MoreStrings.toSafeString(location))
                    .add("label", label)
                    .add("photo", photo)
                    .add("isSipCall", isSipCall)
                    .toString();
        }
    }
}
