/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.providers.contacts;

import static android.provider.VoicemailContract.SOURCE_PACKAGE_FIELD;
import static com.android.providers.contacts.util.DbQueryUtils.concatenateClauses;
import static com.android.providers.contacts.util.DbQueryUtils.getEqualityClause;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.provider.VoicemailContract;
import android.provider.VoicemailContract.Voicemails;
import android.util.Log;

import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.util.SelectionBuilder;
import com.android.providers.contacts.util.TypedUriMatcherImpl;
import com.google.common.annotations.VisibleForTesting;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * An implementation of the Voicemail content provider. This class in the entry point for both
 * voicemail content ('calls') table and 'voicemail_status' table. This class performs all common
 * permission checks and then delegates database level operations to respective table delegate
 * objects.
 */
public class VoicemailContentProvider extends ContentProvider
        implements VoicemailTable.DelegateHelper {
    private VoicemailPermissions mVoicemailPermissions;
    private VoicemailTable.Delegate mVoicemailContentTable;
    private VoicemailTable.Delegate mVoicemailStatusTable;

    @Override
    public boolean onCreate() {
        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "VoicemailContentProvider.onCreate start");
        }
        Context context = context();
        mVoicemailPermissions = new VoicemailPermissions(context);
        mVoicemailContentTable = new VoicemailContentTable(Tables.CALLS, context,
                getDatabaseHelper(context), this, createCallLogInsertionHelper(context));
        mVoicemailStatusTable = new VoicemailStatusTable(Tables.VOICEMAIL_STATUS, context,
                getDatabaseHelper(context), this);
        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "VoicemailContentProvider.onCreate finish");
        }
        return true;
    }

    @VisibleForTesting
    /*package*/ CallLogInsertionHelper createCallLogInsertionHelper(Context context) {
        return DefaultCallLogInsertionHelper.getInstance(context);
    }

    @VisibleForTesting
    /*package*/ ContactsDatabaseHelper getDatabaseHelper(Context context) {
        return ContactsDatabaseHelper.getInstance(context);
    }

    @VisibleForTesting
    /*package*/ Context context() {
        return getContext();
    }

    @Override
    public String getType(Uri uri) {
        UriData uriData = null;
        try {
            uriData = UriData.createUriData(uri);
        } catch (IllegalArgumentException ignored) {
            // Special case: for illegal URIs, we return null rather than thrown an exception.
            return null;
        }
        return getTableDelegate(uriData).getType(uriData);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        UriData uriData = checkPermissionsAndCreateUriData(uri, values);
        return getTableDelegate(uriData).insert(uriData, values);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        UriData uriData = checkPermissionsAndCreateUriDataForReadOperation(uri);
        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);
        selectionBuilder.addClause(getPackageRestrictionClause());
        return getTableDelegate(uriData).query(uriData, projection, selectionBuilder.build(),
                selectionArgs, sortOrder);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        UriData uriData = checkPermissionsAndCreateUriData(uri, values);
        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);
        selectionBuilder.addClause(getPackageRestrictionClause());
        return getTableDelegate(uriData).update(uriData, values, selectionBuilder.build(),
                selectionArgs);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        UriData uriData = checkPermissionsAndCreateUriData(uri);
        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);
        selectionBuilder.addClause(getPackageRestrictionClause());
        return getTableDelegate(uriData).delete(uriData, selectionBuilder.build(), selectionArgs);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        UriData uriData = null;
        if (mode.equals("r")) {
            uriData = checkPermissionsAndCreateUriDataForReadOperation(uri);
        } else {
            uriData = checkPermissionsAndCreateUriData(uri);
        }
        // openFileHelper() relies on "_data" column to be populated with the file path.
        return getTableDelegate(uriData).openFile(uriData, mode);
    }

    /** Returns the correct table delegate object that can handle this URI. */
    private VoicemailTable.Delegate getTableDelegate(UriData uriData) {
        switch (uriData.getUriType()) {
            case STATUS:
            case STATUS_ID:
                return mVoicemailStatusTable;
            case VOICEMAILS:
            case VOICEMAILS_ID:
                return mVoicemailContentTable;
            case NO_MATCH:
                throw new IllegalStateException("Invalid uri type for uri: " + uriData.getUri());
            default:
                throw new IllegalStateException("Impossible, all cases are covered.");
        }
    }

    /**
     * Decorates a URI by providing methods to get various properties from the URI.
     */
    public static class UriData {
        private final Uri mUri;
        private final String mId;
        private final String mSourcePackage;
        private final VoicemailUriType mUriType;

        public UriData(Uri uri, VoicemailUriType uriType, String id, String sourcePackage) {
            mUriType = uriType;
            mUri = uri;
            mId = id;
            mSourcePackage = sourcePackage;
        }

        /** Gets the original URI to which this {@link UriData} corresponds. */
        public final Uri getUri() {
            return mUri;
        }

        /** Tells us if our URI has an individual voicemail id. */
        public final boolean hasId() {
            return mId != null;
        }

        /** Gets the ID for the voicemail. */
        public final String getId() {
            return mId;
        }

        /** Tells us if our URI has a source package string. */
        public final boolean hasSourcePackage() {
            return mSourcePackage != null;
        }

        /** Gets the source package. */
        public final String getSourcePackage() {
            return mSourcePackage;
        }

        /** Gets the Voicemail URI type. */
        public final VoicemailUriType getUriType() {
            return mUriType;
        }

        /** Builds a where clause from the URI data. */
        public final String getWhereClause() {
            return concatenateClauses(
                    (hasId() ? getEqualityClause(BaseColumns._ID, getId()) : null),
                    (hasSourcePackage() ? getEqualityClause(SOURCE_PACKAGE_FIELD,
                            getSourcePackage()) : null));
        }

        /** Create a {@link UriData} corresponding to a given uri. */
        public static UriData createUriData(Uri uri) {
            String sourcePackage = uri.getQueryParameter(
                    VoicemailContract.PARAM_KEY_SOURCE_PACKAGE);
            List<String> segments = uri.getPathSegments();
            VoicemailUriType uriType = createUriMatcher().match(uri);
            switch (uriType) {
                case VOICEMAILS:
                case STATUS:
                    return new UriData(uri, uriType, null, sourcePackage);
                case VOICEMAILS_ID:
                case STATUS_ID:
                    return new UriData(uri, uriType, segments.get(1), sourcePackage);
                case NO_MATCH:
                    throw new IllegalArgumentException("Invalid URI: " + uri);
                default:
                    throw new IllegalStateException("Impossible, all cases are covered");
            }
        }

        private static TypedUriMatcherImpl<VoicemailUriType> createUriMatcher() {
            return new TypedUriMatcherImpl<VoicemailUriType>(
                    VoicemailContract.AUTHORITY, VoicemailUriType.values());
        }
    }

    @Override
    // VoicemailTable.DelegateHelper interface.
    public void checkAndAddSourcePackageIntoValues(UriData uriData, ContentValues values) {
        // If content values don't contain the provider, calculate the right provider to use.
        if (!values.containsKey(SOURCE_PACKAGE_FIELD)) {
            String provider = uriData.hasSourcePackage() ?
                    uriData.getSourcePackage() : getCallingPackage_();
            values.put(SOURCE_PACKAGE_FIELD, provider);
        }
        // You must have access to the provider given in values.
        if (!mVoicemailPermissions.callerHasFullAccess()) {
            checkPackagesMatch(getCallingPackage_(),
                    values.getAsString(VoicemailContract.SOURCE_PACKAGE_FIELD),
                    uriData.getUri());
        }
    }

    /**
     * Checks that the source_package field is same in uriData and ContentValues, if it happens
     * to be set in both.
     */
    private void checkSourcePackageSameIfSet(UriData uriData, ContentValues values) {
        if (uriData.hasSourcePackage() && values.containsKey(SOURCE_PACKAGE_FIELD)) {
            if (!uriData.getSourcePackage().equals(values.get(SOURCE_PACKAGE_FIELD))) {
                throw new SecurityException(
                        "source_package in URI was " + uriData.getSourcePackage() +
                        " but doesn't match source_package in ContentValues which was "
                        + values.get(SOURCE_PACKAGE_FIELD));
            }
        }
    }

    @Override
    /** Implementation of  {@link VoicemailTable.DelegateHelper#openDataFile(UriData, String)} */
    public ParcelFileDescriptor openDataFile(UriData uriData, String mode)
            throws FileNotFoundException {
        return openFileHelper(uriData.getUri(), mode);
    }

    /**
     * Performs necessary voicemail permission checks common to all operations and returns
     * the structured representation, {@link UriData}, of the supplied uri.
     */
    private UriData checkPermissionsAndCreateUriDataForReadOperation(Uri uri) {
        // If the caller has been explicitly granted read permission to this URI then no need to
        // check further.
        if (context().checkCallingUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                == PackageManager.PERMISSION_GRANTED) {
            return UriData.createUriData(uri);
        }
        return checkPermissionsAndCreateUriData(uri);
    }

    /**
     * Performs necessary voicemail permission checks common to all operations and returns
     * the structured representation, {@link UriData}, of the supplied uri.
     */
    private UriData checkPermissionsAndCreateUriData(Uri uri) {
        mVoicemailPermissions.checkCallerHasOwnVoicemailAccess();
        UriData uriData = UriData.createUriData(uri);
        checkPackagePermission(uriData);
        return uriData;
    }

    /**
     * Same as {@link #checkPackagePermission(UriData)}. In addition does permission check
     * on the ContentValues.
     */
    private UriData checkPermissionsAndCreateUriData(Uri uri, ContentValues... valuesArray) {
        UriData uriData = checkPermissionsAndCreateUriData(uri);
        for (ContentValues values : valuesArray) {
            checkSourcePackageSameIfSet(uriData, values);
        }
        return uriData;
    }

    /**
     * Checks that the callingPackage is same as voicemailSourcePackage. Throws {@link
     * SecurityException} if they don't match.
     */
    private final void checkPackagesMatch(String callingPackage, String voicemailSourcePackage,
            Uri uri) {
        if (!voicemailSourcePackage.equals(callingPackage)) {
            String errorMsg = String.format("Permission denied for URI: %s\n. " +
                    "Package %s cannot perform this operation for %s. Requires %s permission.",
                    uri, callingPackage, voicemailSourcePackage,
                    Manifest.permission.READ_WRITE_ALL_VOICEMAIL);
            throw new SecurityException(errorMsg);
        }
    }

    /**
     * Checks that either the caller has READ_WRITE_ALL_VOICEMAIL permission, or has the
     * ADD_VOICEMAIL permission and is using a URI that matches
     * /voicemail/?source_package=[source-package] where [source-package] is the same as the calling
     * package.
     *
     * @throws SecurityException if the check fails.
     */
    private void checkPackagePermission(UriData uriData) {
        if (!mVoicemailPermissions.callerHasFullAccess()) {
            if (!uriData.hasSourcePackage()) {
                // You cannot have a match if this is not a provider URI.
                throw new SecurityException(String.format(
                        "Provider %s does not have %s permission." +
                                "\nPlease set query parameter '%s' in the URI.\nURI: %s",
                        getCallingPackage_(), Manifest.permission.READ_WRITE_ALL_VOICEMAIL,
                        VoicemailContract.PARAM_KEY_SOURCE_PACKAGE, uriData.getUri()));
            }
            checkPackagesMatch(getCallingPackage_(), uriData.getSourcePackage(), uriData.getUri());
        }
    }

    /**
     * Gets the name of the calling package.
     * <p>
     * It's possible (though unlikely) for there to be more than one calling package (requires that
     * your manifest say you want to share process ids) in which case we will return an arbitrary
     * package name. It's also possible (though very unlikely) for us to be unable to work out what
     * your calling package is, in which case we will return null.
     */
    /* package for test */String getCallingPackage_() {
        int caller = Binder.getCallingUid();
        if (caller == 0) {
            return null;
        }
        String[] callerPackages = context().getPackageManager().getPackagesForUid(caller);
        if (callerPackages == null || callerPackages.length == 0) {
            return null;
        }
        if (callerPackages.length == 1) {
            return callerPackages[0];
        }
        // If we have more than one caller package, which is very unlikely, let's return the one
        // with the highest permissions. If more than one has the same permission, we don't care
        // which one we return.
        String bestSoFar = callerPackages[0];
        for (String callerPackage : callerPackages) {
            if (mVoicemailPermissions.packageHasFullAccess(callerPackage)) {
                // Full always wins, we can return early.
                return callerPackage;
            }
            if (mVoicemailPermissions.packageHasOwnVoicemailAccess(callerPackage)) {
                bestSoFar = callerPackage;
            }
        }
        return bestSoFar;
    }

    /**
     * Creates a clause to restrict the selection to the calling provider or null if the caller has
     * access to all data.
     */
    private String getPackageRestrictionClause() {
        if (mVoicemailPermissions.callerHasFullAccess()) {
            return null;
        }
        return getEqualityClause(Voicemails.SOURCE_PACKAGE, getCallingPackage_());
    }
}
