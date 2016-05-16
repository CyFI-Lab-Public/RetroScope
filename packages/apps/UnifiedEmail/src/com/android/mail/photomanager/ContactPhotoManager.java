/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.mail.photomanager;

import android.content.ContentResolver;
import android.content.Context;
import android.text.TextUtils;
import android.util.LruCache;

import com.android.mail.ContactInfo;
import com.android.mail.SenderInfoLoader;
import com.android.mail.ui.ImageCanvas;
import com.android.mail.utils.LogUtils;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Asynchronously loads contact photos and maintains a cache of photos.
 */
public class ContactPhotoManager extends PhotoManager {
    public static final String CONTACT_PHOTO_SERVICE = "contactPhotos";

    /**
     * An LRU cache for photo ids mapped to contact addresses.
     */
    private final LruCache<String, Long> mPhotoIdCache;
    private final LetterTileProvider mLetterTileProvider;

    /** Cache size for {@link #mPhotoIdCache}. Starting with 500 entries. */
    private static final int PHOTO_ID_CACHE_SIZE = 500;

    /**
     * Requests the singleton instance with data bound from the available authenticators. This
     * method can safely be called from the UI thread.
     */
    public static ContactPhotoManager getInstance(Context context) {
        Context applicationContext = context.getApplicationContext();
        ContactPhotoManager service =
                (ContactPhotoManager) applicationContext.getSystemService(CONTACT_PHOTO_SERVICE);
        if (service == null) {
            service = createContactPhotoManager(applicationContext);
            LogUtils.e(TAG, "No contact photo service in context: " + applicationContext);
        }
        return service;
    }

    public static synchronized ContactPhotoManager createContactPhotoManager(Context context) {
        return new ContactPhotoManager(context);
    }

    public static int generateHash(ImageCanvas view, int pos, Object key) {
        return Objects.hashCode(view, pos, key);
    }

    private ContactPhotoManager(Context context) {
        super(context);
        mPhotoIdCache = new LruCache<String, Long>(PHOTO_ID_CACHE_SIZE);
        mLetterTileProvider = new LetterTileProvider(context);
    }

    @Override
    protected DefaultImageProvider getDefaultImageProvider() {
        return mLetterTileProvider;
    }

    @Override
    protected int getHash(PhotoIdentifier id, ImageCanvas view) {
        final ContactIdentifier contactId = (ContactIdentifier) id;
        return generateHash(view, contactId.pos, contactId.getKey());
    }

    @Override
    protected PhotoLoaderThread getLoaderThread(ContentResolver contentResolver) {
        return new ContactPhotoLoaderThread(contentResolver);
    }

    @Override
    public void clear() {
        super.clear();
        mPhotoIdCache.evictAll();
    }

    public static class ContactIdentifier extends PhotoIdentifier {
        public final String name;
        public final String emailAddress;
        public final int pos;

        public ContactIdentifier(String name, String emailAddress, int pos) {
            this.name = name;
            this.emailAddress = emailAddress;
            this.pos = pos;
        }

        @Override
        public boolean isValid() {
            return !TextUtils.isEmpty(emailAddress);
        }

        @Override
        public Object getKey() {
            return emailAddress;
        }

        @Override
        public int hashCode() {
            int hash = 17;
            hash = 31 * hash + (emailAddress != null ? emailAddress.hashCode() : 0);
            hash = 31 * hash + (name != null ? name.hashCode() : 0);
            hash = 31 * hash + pos;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ContactIdentifier other = (ContactIdentifier) obj;
            return Objects.equal(emailAddress, other.emailAddress)
                    && Objects.equal(name, other.name) && Objects.equal(pos, other.pos);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("{");
            sb.append(super.toString());
            sb.append(" name=");
            sb.append(name);
            sb.append(" email=");
            sb.append(emailAddress);
            sb.append(" pos=");
            sb.append(pos);
            sb.append("}");
            return sb.toString();
        }

        @Override
        public int compareTo(PhotoIdentifier another) {
            return 0;
        }
    }

    public class ContactPhotoLoaderThread extends PhotoLoaderThread {
        public ContactPhotoLoaderThread(ContentResolver resolver) {
            super(resolver);
        }

        @Override
        protected Map<String, BitmapHolder> loadPhotos(Collection<Request> requests) {
            Map<String, BitmapHolder> photos = new HashMap<String, BitmapHolder>(requests.size());

            Set<String> addresses = new HashSet<String>();
            Set<Long> photoIds = new HashSet<Long>();
            HashMap<Long, String> photoIdMap = new HashMap<Long, String>();

            Long match;
            String emailAddress;
            for (Request request : requests) {
                emailAddress = (String) request.getKey();
                match = mPhotoIdCache.get(emailAddress);
                if (match != null) {
                    photoIds.add(match);
                    photoIdMap.put(match, emailAddress);
                } else {
                    addresses.add(emailAddress);
                }
            }

            // get the Map of email addresses to ContactInfo
            ImmutableMap<String, ContactInfo> emailAddressToContactInfoMap =
                    SenderInfoLoader.loadContactPhotos(
                    getResolver(), addresses, false /* decodeBitmaps */);

            // Put all entries into photos map: a mapping of email addresses to photoBytes.
            // If there is no ContactInfo, it means we couldn't get a photo for this
            // address so just put null in for the bytes so that the crazy caching
            // works properly and we don't get an infinite loop of GC churn.
            if (emailAddressToContactInfoMap != null) {
                for (final String address : addresses) {
                    final ContactInfo info = emailAddressToContactInfoMap.get(address);
                    photos.put(address,
                            new BitmapHolder(info != null ? info.photoBytes : null, -1, -1));
                }
            } else {
                // Still need to set a null result for all addresses, otherwise we end
                // up in the loop where photo manager attempts to load these again.
                for (final String address: addresses) {
                    photos.put(address, new BitmapHolder(null, -1, -1));
                }
            }

            return photos;
        }
    }
}
