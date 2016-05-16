
package com.android.loaderapp.model;

import com.android.loaderapp.util.DataStatus;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;

import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Entity;
import android.content.EntityIterator;
import android.content.Loader.ForceLoadContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.DisplayNameSources;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.provider.ContactsContract.StatusUpdates;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Loads a single Contact and all it constituent RawContacts.
 */
public class ContactLoader extends AsyncTaskLoader<ContactLoader.ContactData> {
    Uri mLookupUri;
    ContactData mContact;
    ForceLoadContentObserver mObserver;
    boolean mDestroyed;

    public interface Callbacks {
        public void onContactLoaded(ContactData contact);
    }
    
    public static final class ContactData {
        public Uri uri;
        public ArrayList<Entity> entities;
        public HashMap<Long, DataStatus> statuses;
        public long nameRawContactId = -1;
        public int displayNameSource = DisplayNameSources.UNDEFINED;
    }
    
    interface StatusQuery {
        final String[] PROJECTION = new String[] {
                Data._ID, Data.STATUS, Data.STATUS_RES_PACKAGE, Data.STATUS_ICON,
                Data.STATUS_LABEL, Data.STATUS_TIMESTAMP, Data.PRESENCE,
        };
        
        final int _ID = 0;
    }

    @Override
    public ContactData loadInBackground() {
        ContentResolver resolver = getContext().getContentResolver();
        ContactData result = new ContactData();

        // Undo the lookup URI
        Uri contactUri = null;
        if (mLookupUri != null) {
            mLookupUri = Contacts.getLookupUri(resolver, mLookupUri);
            if (mLookupUri != null) {
                contactUri = Contacts.lookupContact(resolver, mLookupUri);
            }
        }

        if (contactUri == null) {
            return null;
        }
        result.uri = contactUri;

        // Read available social rows
        final Uri dataUri = Uri.withAppendedPath(contactUri, Contacts.Data.CONTENT_DIRECTORY);
        Cursor cursor = resolver.query(dataUri, StatusQuery.PROJECTION, StatusUpdates.PRESENCE
                + " IS NOT NULL OR " + StatusUpdates.STATUS + " IS NOT NULL", null, null);

        if (cursor != null) {
            try {
                HashMap<Long, DataStatus> statuses = Maps.newHashMap();
                
                // Walk found statuses, creating internal row for each
                while (cursor.moveToNext()) {
                    final DataStatus status = new DataStatus(cursor);
                    final long dataId = cursor.getLong(StatusQuery._ID);
                    statuses.put(dataId, status);
                }
                result.statuses = statuses;
            } finally {
                cursor.close();
            }
        }

        // Read out the info about the display name
        cursor = resolver.query(dataUri, new String[] {
                Contacts.NAME_RAW_CONTACT_ID, Contacts.DISPLAY_NAME_SOURCE
        }, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    result.nameRawContactId = cursor.getLong(cursor
                            .getColumnIndex(Contacts.NAME_RAW_CONTACT_ID));
                    result.displayNameSource = cursor.getInt(cursor
                            .getColumnIndex(Contacts.DISPLAY_NAME_SOURCE));
                }
            } finally {
                cursor.close();
            }
        }

        // Read the constituent raw contacts
        final long contactId = ContentUris.parseId(contactUri);
        cursor = resolver.query(RawContactsEntity.CONTENT_URI, null, RawContacts.CONTACT_ID
                + "=" + contactId, null, null);
        if (cursor != null) {
            ArrayList<Entity> entities = Lists.newArrayList();
            EntityIterator iterator = RawContacts.newEntityIterator(cursor);
            try {
                while (iterator.hasNext()) {
                    Entity entity = iterator.next();
                    entities.add(entity);
                }
            } finally {
                iterator.close();
            }
            result.entities = entities;
        }

        return result;
    }

    @Override
    public void deliverResult(ContactData result) {
        // The creator isn't interested in any further updates
        if (mDestroyed) {
            return;
        }

        mContact = result;
        if (result != null) {
            if (mObserver == null) {
                mObserver = new ForceLoadContentObserver();
            }
            getContext().getContentResolver().registerContentObserver(mLookupUri, true, mObserver);
            super.deliverResult(result);
        }
    }

    public ContactLoader(Context context, Uri lookupUri) {
        super(context);
        mLookupUri = lookupUri;
    }

    @Override
    public void startLoading() {
        if (mContact != null) {
            deliverResult(mContact);
        } else {
            forceLoad();
        }
    }

    @Override
    public void stopLoading() {
        mContact = null;
        if (mObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(mObserver);
        }
    }

    @Override
    public void destroy() {
        mContact = null;
        mDestroyed = true;
    }
}
