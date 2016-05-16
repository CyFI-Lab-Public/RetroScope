/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.accounts.Account;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.Extensions;
import android.provider.Contacts.People;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.provider.ContactsContract.StatusUpdates;
import android.text.TextUtils;
import android.util.Log;

import com.android.providers.contacts.ContactsDatabaseHelper.AccountsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.DataColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.ExtensionsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.GroupsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.MimetypesColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupType;
import com.android.providers.contacts.ContactsDatabaseHelper.PhoneLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.PresenceColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.RawContactsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.StatusUpdatesColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;

import java.util.HashMap;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class LegacyApiSupport {

    private static final String TAG = "ContactsProviderV1";

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int PEOPLE = 1;
    private static final int PEOPLE_ID = 2;
    private static final int PEOPLE_UPDATE_CONTACT_TIME = 3;
    private static final int ORGANIZATIONS = 4;
    private static final int ORGANIZATIONS_ID = 5;
    private static final int PEOPLE_CONTACTMETHODS = 6;
    private static final int PEOPLE_CONTACTMETHODS_ID = 7;
    private static final int CONTACTMETHODS = 8;
    private static final int CONTACTMETHODS_ID = 9;
    private static final int PEOPLE_PHONES = 10;
    private static final int PEOPLE_PHONES_ID = 11;
    private static final int PHONES = 12;
    private static final int PHONES_ID = 13;
    private static final int EXTENSIONS = 14;
    private static final int EXTENSIONS_ID = 15;
    private static final int PEOPLE_EXTENSIONS = 16;
    private static final int PEOPLE_EXTENSIONS_ID = 17;
    private static final int GROUPS = 18;
    private static final int GROUPS_ID = 19;
    private static final int GROUPMEMBERSHIP = 20;
    private static final int GROUPMEMBERSHIP_ID = 21;
    private static final int PEOPLE_GROUPMEMBERSHIP = 22;
    private static final int PEOPLE_GROUPMEMBERSHIP_ID = 23;
    private static final int PEOPLE_PHOTO = 24;
    private static final int PHOTOS = 25;
    private static final int PHOTOS_ID = 26;
    private static final int PEOPLE_FILTER = 29;
    private static final int DELETED_PEOPLE = 30;
    private static final int DELETED_GROUPS = 31;
    private static final int SEARCH_SUGGESTIONS = 32;
    private static final int SEARCH_SHORTCUT = 33;
    private static final int PHONES_FILTER = 34;
    private static final int LIVE_FOLDERS_PEOPLE = 35;
    private static final int LIVE_FOLDERS_PEOPLE_GROUP_NAME = 36;
    private static final int LIVE_FOLDERS_PEOPLE_WITH_PHONES = 37;
    private static final int LIVE_FOLDERS_PEOPLE_FAVORITES = 38;
    private static final int CONTACTMETHODS_EMAIL = 39;
    private static final int GROUP_NAME_MEMBERS = 40;
    private static final int GROUP_SYSTEM_ID_MEMBERS = 41;
    private static final int PEOPLE_ORGANIZATIONS = 42;
    private static final int PEOPLE_ORGANIZATIONS_ID = 43;
    private static final int SETTINGS = 44;

    private static final String PEOPLE_JOINS =
            " JOIN " + Tables.ACCOUNTS + " ON ("
                + RawContactsColumns.CONCRETE_ACCOUNT_ID + "=" + AccountsColumns.CONCRETE_ID + ")"
            + " LEFT OUTER JOIN data name ON (raw_contacts._id = name.raw_contact_id"
            + " AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = name.mimetype_id)"
                    + "='" + StructuredName.CONTENT_ITEM_TYPE + "')"
            + " LEFT OUTER JOIN data organization ON (raw_contacts._id = organization.raw_contact_id"
            + " AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = organization.mimetype_id)"
                    + "='" + Organization.CONTENT_ITEM_TYPE + "' AND organization.is_primary)"
            + " LEFT OUTER JOIN data email ON (raw_contacts._id = email.raw_contact_id"
            + " AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = email.mimetype_id)"
                    + "='" + Email.CONTENT_ITEM_TYPE + "' AND email.is_primary)"
            + " LEFT OUTER JOIN data note ON (raw_contacts._id = note.raw_contact_id"
            + " AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = note.mimetype_id)"
                    + "='" + Note.CONTENT_ITEM_TYPE + "')"
            + " LEFT OUTER JOIN data phone ON (raw_contacts._id = phone.raw_contact_id"
            + " AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = phone.mimetype_id)"
                    + "='" + Phone.CONTENT_ITEM_TYPE + "' AND phone.is_primary)";

    public static final String DATA_JOINS =
            " JOIN mimetypes ON (mimetypes._id = data.mimetype_id)"
            + " JOIN raw_contacts ON (raw_contacts._id = data.raw_contact_id)"
            + PEOPLE_JOINS;

    public static final String PRESENCE_JOINS =
            " LEFT OUTER JOIN " + Tables.PRESENCE +
            " ON (" + Tables.PRESENCE + "." + StatusUpdates.DATA_ID + "=" +
                    "(SELECT MAX(" + StatusUpdates.DATA_ID + ")" +
                    " FROM " + Tables.PRESENCE +
                    " WHERE people._id = " + PresenceColumns.RAW_CONTACT_ID + ")" +
            " )";

    private static final String PHONETIC_NAME_SQL = "trim(trim("
            + "ifnull(name." + StructuredName.PHONETIC_GIVEN_NAME + ",' ')||' '||"
            + "ifnull(name." + StructuredName.PHONETIC_MIDDLE_NAME + ",' '))||' '||"
            + "ifnull(name." + StructuredName.PHONETIC_FAMILY_NAME + ",' ')) ";

    private static final String CONTACT_METHOD_KIND_SQL =
            "CAST ((CASE WHEN mimetype='" + Email.CONTENT_ITEM_TYPE + "'"
                + " THEN " + android.provider.Contacts.KIND_EMAIL
                + " ELSE"
                    + " (CASE WHEN mimetype='" + Im.CONTENT_ITEM_TYPE +"'"
                        + " THEN " + android.provider.Contacts.KIND_IM
                        + " ELSE"
                        + " (CASE WHEN mimetype='" + StructuredPostal.CONTENT_ITEM_TYPE + "'"
                            + " THEN "  + android.provider.Contacts.KIND_POSTAL
                            + " ELSE"
                                + " NULL"
                            + " END)"
                        + " END)"
                + " END) AS INTEGER)";

    private static final String IM_PROTOCOL_SQL =
            "(CASE WHEN " + StatusUpdates.PROTOCOL + "=" + Im.PROTOCOL_CUSTOM
                + " THEN 'custom:'||" + StatusUpdates.CUSTOM_PROTOCOL
                + " ELSE 'pre:'||" + StatusUpdates.PROTOCOL
                + " END)";

    private static String CONTACT_METHOD_DATA_SQL =
            "(CASE WHEN " + Data.MIMETYPE + "='" + Im.CONTENT_ITEM_TYPE + "'"
                + " THEN (CASE WHEN " + Tables.DATA + "." + Im.PROTOCOL + "=" + Im.PROTOCOL_CUSTOM
                    + " THEN 'custom:'||" + Tables.DATA + "." + Im.CUSTOM_PROTOCOL
                    + " ELSE 'pre:'||" + Tables.DATA + "." + Im.PROTOCOL
                    + " END)"
                + " ELSE " + Tables.DATA + "." + Email.DATA
                + " END)";

    private static final Uri LIVE_FOLDERS_CONTACTS_URI = Uri.withAppendedPath(
            ContactsContract.AUTHORITY_URI, "live_folders/contacts");

    private static final Uri LIVE_FOLDERS_CONTACTS_WITH_PHONES_URI = Uri.withAppendedPath(
            ContactsContract.AUTHORITY_URI, "live_folders/contacts_with_phones");

    private static final Uri LIVE_FOLDERS_CONTACTS_FAVORITES_URI = Uri.withAppendedPath(
            ContactsContract.AUTHORITY_URI, "live_folders/favorites");

    private static final String CONTACTS_UPDATE_LASTTIMECONTACTED =
            "UPDATE " + Tables.CONTACTS +
            " SET " + Contacts.LAST_TIME_CONTACTED + "=? " +
            "WHERE " + Contacts._ID + "=?";
    private static final String RAWCONTACTS_UPDATE_LASTTIMECONTACTED =
            "UPDATE " + Tables.RAW_CONTACTS + " SET "
            + RawContacts.LAST_TIME_CONTACTED + "=? WHERE "
            + RawContacts._ID + "=?";

    private String[] mSelectionArgs1 = new String[1];
    private String[] mSelectionArgs2 = new String[2];

    public interface LegacyTables {
        public static final String PEOPLE = "view_v1_people";
        public static final String PEOPLE_JOIN_PRESENCE = "view_v1_people people " + PRESENCE_JOINS;
        public static final String GROUPS = "view_v1_groups";
        public static final String ORGANIZATIONS = "view_v1_organizations";
        public static final String CONTACT_METHODS = "view_v1_contact_methods";
        public static final String PHONES = "view_v1_phones";
        public static final String EXTENSIONS = "view_v1_extensions";
        public static final String GROUP_MEMBERSHIP = "view_v1_group_membership";
        public static final String PHOTOS = "view_v1_photos";
        public static final String SETTINGS = "v1_settings";
    }

    private static final String[] ORGANIZATION_MIME_TYPES = new String[] {
        Organization.CONTENT_ITEM_TYPE
    };

    private static final String[] CONTACT_METHOD_MIME_TYPES = new String[] {
        Email.CONTENT_ITEM_TYPE,
        Im.CONTENT_ITEM_TYPE,
        StructuredPostal.CONTENT_ITEM_TYPE,
    };

    private static final String[] PHONE_MIME_TYPES = new String[] {
        Phone.CONTENT_ITEM_TYPE
    };

    private static final String[] PHOTO_MIME_TYPES = new String[] {
        Photo.CONTENT_ITEM_TYPE
    };

    private static final String[] GROUP_MEMBERSHIP_MIME_TYPES = new String[] {
        GroupMembership.CONTENT_ITEM_TYPE
    };

    private static final String[] EXTENSION_MIME_TYPES = new String[] {
        android.provider.Contacts.Extensions.CONTENT_ITEM_TYPE
    };

    private interface IdQuery {
        String[] COLUMNS = { BaseColumns._ID };

        int _ID = 0;
    }

    /**
     * A custom data row that is used to store legacy photo data fields no
     * longer directly supported by the API.
     */
    private interface LegacyPhotoData {
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/photo_v1_extras";

        public static final String PHOTO_DATA_ID = Data.DATA1;
        public static final String LOCAL_VERSION = Data.DATA2;
        public static final String DOWNLOAD_REQUIRED = Data.DATA3;
        public static final String EXISTS_ON_SERVER = Data.DATA4;
        public static final String SYNC_ERROR = Data.DATA5;
    }

    public static final String LEGACY_PHOTO_JOIN =
            " LEFT OUTER JOIN data legacy_photo ON (raw_contacts._id = legacy_photo.raw_contact_id"
            + " AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = legacy_photo.mimetype_id)"
                + "='" + LegacyPhotoData.CONTENT_ITEM_TYPE + "'"
            + " AND " + DataColumns.CONCRETE_ID + " = legacy_photo." + LegacyPhotoData.PHOTO_DATA_ID
            + ")";

    private static final HashMap<String, String> sPeopleProjectionMap;
    private static final HashMap<String, String> sOrganizationProjectionMap;
    private static final HashMap<String, String> sContactMethodProjectionMap;
    private static final HashMap<String, String> sPhoneProjectionMap;
    private static final HashMap<String, String> sExtensionProjectionMap;
    private static final HashMap<String, String> sGroupProjectionMap;
    private static final HashMap<String, String> sGroupMembershipProjectionMap;
    private static final HashMap<String, String> sPhotoProjectionMap;

    static {

        // Contacts URI matching table
        UriMatcher matcher = sUriMatcher;

        String authority = android.provider.Contacts.AUTHORITY;
        matcher.addURI(authority, "extensions", EXTENSIONS);
        matcher.addURI(authority, "extensions/#", EXTENSIONS_ID);
        matcher.addURI(authority, "groups", GROUPS);
        matcher.addURI(authority, "groups/#", GROUPS_ID);
        matcher.addURI(authority, "groups/name/*/members", GROUP_NAME_MEMBERS);
//        matcher.addURI(authority, "groups/name/*/members/filter/*",
//                GROUP_NAME_MEMBERS_FILTER);
        matcher.addURI(authority, "groups/system_id/*/members", GROUP_SYSTEM_ID_MEMBERS);
//        matcher.addURI(authority, "groups/system_id/*/members/filter/*",
//                GROUP_SYSTEM_ID_MEMBERS_FILTER);
        matcher.addURI(authority, "groupmembership", GROUPMEMBERSHIP);
        matcher.addURI(authority, "groupmembership/#", GROUPMEMBERSHIP_ID);
//        matcher.addURI(authority, "groupmembershipraw", GROUPMEMBERSHIP_RAW);
        matcher.addURI(authority, "people", PEOPLE);
//        matcher.addURI(authority, "people/strequent", PEOPLE_STREQUENT);
//        matcher.addURI(authority, "people/strequent/filter/*", PEOPLE_STREQUENT_FILTER);
        matcher.addURI(authority, "people/filter/*", PEOPLE_FILTER);
//        matcher.addURI(authority, "people/with_phones_filter/*",
//                PEOPLE_WITH_PHONES_FILTER);
//        matcher.addURI(authority, "people/with_email_or_im_filter/*",
//                PEOPLE_WITH_EMAIL_OR_IM_FILTER);
        matcher.addURI(authority, "people/#", PEOPLE_ID);
        matcher.addURI(authority, "people/#/extensions", PEOPLE_EXTENSIONS);
        matcher.addURI(authority, "people/#/extensions/#", PEOPLE_EXTENSIONS_ID);
        matcher.addURI(authority, "people/#/phones", PEOPLE_PHONES);
        matcher.addURI(authority, "people/#/phones/#", PEOPLE_PHONES_ID);
//        matcher.addURI(authority, "people/#/phones_with_presence",
//                PEOPLE_PHONES_WITH_PRESENCE);
        matcher.addURI(authority, "people/#/photo", PEOPLE_PHOTO);
//        matcher.addURI(authority, "people/#/photo/data", PEOPLE_PHOTO_DATA);
        matcher.addURI(authority, "people/#/contact_methods", PEOPLE_CONTACTMETHODS);
//        matcher.addURI(authority, "people/#/contact_methods_with_presence",
//                PEOPLE_CONTACTMETHODS_WITH_PRESENCE);
        matcher.addURI(authority, "people/#/contact_methods/#", PEOPLE_CONTACTMETHODS_ID);
        matcher.addURI(authority, "people/#/organizations", PEOPLE_ORGANIZATIONS);
        matcher.addURI(authority, "people/#/organizations/#", PEOPLE_ORGANIZATIONS_ID);
        matcher.addURI(authority, "people/#/groupmembership", PEOPLE_GROUPMEMBERSHIP);
        matcher.addURI(authority, "people/#/groupmembership/#", PEOPLE_GROUPMEMBERSHIP_ID);
//        matcher.addURI(authority, "people/raw", PEOPLE_RAW);
//        matcher.addURI(authority, "people/owner", PEOPLE_OWNER);
        matcher.addURI(authority, "people/#/update_contact_time",
                PEOPLE_UPDATE_CONTACT_TIME);
        matcher.addURI(authority, "deleted_people", DELETED_PEOPLE);
        matcher.addURI(authority, "deleted_groups", DELETED_GROUPS);
        matcher.addURI(authority, "phones", PHONES);
//        matcher.addURI(authority, "phones_with_presence", PHONES_WITH_PRESENCE);
        matcher.addURI(authority, "phones/filter/*", PHONES_FILTER);
//        matcher.addURI(authority, "phones/filter_name/*", PHONES_FILTER_NAME);
//        matcher.addURI(authority, "phones/mobile_filter_name/*",
//                PHONES_MOBILE_FILTER_NAME);
        matcher.addURI(authority, "phones/#", PHONES_ID);
        matcher.addURI(authority, "photos", PHOTOS);
        matcher.addURI(authority, "photos/#", PHOTOS_ID);
        matcher.addURI(authority, "contact_methods", CONTACTMETHODS);
        matcher.addURI(authority, "contact_methods/email", CONTACTMETHODS_EMAIL);
//        matcher.addURI(authority, "contact_methods/email/*", CONTACTMETHODS_EMAIL_FILTER);
        matcher.addURI(authority, "contact_methods/#", CONTACTMETHODS_ID);
//        matcher.addURI(authority, "contact_methods/with_presence",
//                CONTACTMETHODS_WITH_PRESENCE);
        matcher.addURI(authority, "organizations", ORGANIZATIONS);
        matcher.addURI(authority, "organizations/#", ORGANIZATIONS_ID);
//        matcher.addURI(authority, "voice_dialer_timestamp", VOICE_DIALER_TIMESTAMP);
        matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY,
                SEARCH_SUGGESTIONS);
        matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
                SEARCH_SUGGESTIONS);
        matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*",
                SEARCH_SHORTCUT);
        matcher.addURI(authority, "settings", SETTINGS);

        matcher.addURI(authority, "live_folders/people", LIVE_FOLDERS_PEOPLE);
        matcher.addURI(authority, "live_folders/people/*",
                LIVE_FOLDERS_PEOPLE_GROUP_NAME);
        matcher.addURI(authority, "live_folders/people_with_phones",
                LIVE_FOLDERS_PEOPLE_WITH_PHONES);
        matcher.addURI(authority, "live_folders/favorites",
                LIVE_FOLDERS_PEOPLE_FAVORITES);


        HashMap<String, String> peopleProjectionMap = new HashMap<String, String>();
        peopleProjectionMap.put(People.NAME, People.NAME);
        peopleProjectionMap.put(People.DISPLAY_NAME, People.DISPLAY_NAME);
        peopleProjectionMap.put(People.PHONETIC_NAME, People.PHONETIC_NAME);
        peopleProjectionMap.put(People.NOTES, People.NOTES);
        peopleProjectionMap.put(People.TIMES_CONTACTED, People.TIMES_CONTACTED);
        peopleProjectionMap.put(People.LAST_TIME_CONTACTED, People.LAST_TIME_CONTACTED);
        peopleProjectionMap.put(People.CUSTOM_RINGTONE, People.CUSTOM_RINGTONE);
        peopleProjectionMap.put(People.SEND_TO_VOICEMAIL, People.SEND_TO_VOICEMAIL);
        peopleProjectionMap.put(People.STARRED, People.STARRED);
        peopleProjectionMap.put(People.PRIMARY_ORGANIZATION_ID, People.PRIMARY_ORGANIZATION_ID);
        peopleProjectionMap.put(People.PRIMARY_EMAIL_ID, People.PRIMARY_EMAIL_ID);
        peopleProjectionMap.put(People.PRIMARY_PHONE_ID, People.PRIMARY_PHONE_ID);

        sPeopleProjectionMap = new HashMap<String, String>(peopleProjectionMap);
        sPeopleProjectionMap.put(People._ID, People._ID);
        sPeopleProjectionMap.put(People.NUMBER, People.NUMBER);
        sPeopleProjectionMap.put(People.TYPE, People.TYPE);
        sPeopleProjectionMap.put(People.LABEL, People.LABEL);
        sPeopleProjectionMap.put(People.NUMBER_KEY, People.NUMBER_KEY);
        sPeopleProjectionMap.put(People.IM_PROTOCOL, IM_PROTOCOL_SQL + " AS " + People.IM_PROTOCOL);
        sPeopleProjectionMap.put(People.IM_HANDLE, People.IM_HANDLE);
        sPeopleProjectionMap.put(People.IM_ACCOUNT, People.IM_ACCOUNT);
        sPeopleProjectionMap.put(People.PRESENCE_STATUS, People.PRESENCE_STATUS);
        sPeopleProjectionMap.put(People.PRESENCE_CUSTOM_STATUS,
                "(SELECT " + StatusUpdates.STATUS +
                " FROM " + Tables.STATUS_UPDATES +
                " JOIN " + Tables.DATA +
                "   ON(" + StatusUpdatesColumns.DATA_ID + "=" + DataColumns.CONCRETE_ID + ")" +
                " WHERE " + DataColumns.CONCRETE_RAW_CONTACT_ID + "=people." + People._ID +
                " ORDER BY " + StatusUpdates.STATUS_TIMESTAMP + " DESC " +
                " LIMIT 1" +
                ") AS " + People.PRESENCE_CUSTOM_STATUS);

        sOrganizationProjectionMap = new HashMap<String, String>();
        sOrganizationProjectionMap.put(android.provider.Contacts.Organizations._ID,
                android.provider.Contacts.Organizations._ID);
        sOrganizationProjectionMap.put(android.provider.Contacts.Organizations.PERSON_ID,
                android.provider.Contacts.Organizations.PERSON_ID);
        sOrganizationProjectionMap.put(android.provider.Contacts.Organizations.ISPRIMARY,
                android.provider.Contacts.Organizations.ISPRIMARY);
        sOrganizationProjectionMap.put(android.provider.Contacts.Organizations.COMPANY,
                android.provider.Contacts.Organizations.COMPANY);
        sOrganizationProjectionMap.put(android.provider.Contacts.Organizations.TYPE,
                android.provider.Contacts.Organizations.TYPE);
        sOrganizationProjectionMap.put(android.provider.Contacts.Organizations.LABEL,
                android.provider.Contacts.Organizations.LABEL);
        sOrganizationProjectionMap.put(android.provider.Contacts.Organizations.TITLE,
                android.provider.Contacts.Organizations.TITLE);

        sContactMethodProjectionMap = new HashMap<String, String>(peopleProjectionMap);
        sContactMethodProjectionMap.put(ContactMethods._ID, ContactMethods._ID);
        sContactMethodProjectionMap.put(ContactMethods.PERSON_ID, ContactMethods.PERSON_ID);
        sContactMethodProjectionMap.put(ContactMethods.KIND, ContactMethods.KIND);
        sContactMethodProjectionMap.put(ContactMethods.ISPRIMARY, ContactMethods.ISPRIMARY);
        sContactMethodProjectionMap.put(ContactMethods.TYPE, ContactMethods.TYPE);
        sContactMethodProjectionMap.put(ContactMethods.DATA, ContactMethods.DATA);
        sContactMethodProjectionMap.put(ContactMethods.LABEL, ContactMethods.LABEL);
        sContactMethodProjectionMap.put(ContactMethods.AUX_DATA, ContactMethods.AUX_DATA);

        sPhoneProjectionMap = new HashMap<String, String>(peopleProjectionMap);
        sPhoneProjectionMap.put(android.provider.Contacts.Phones._ID,
                android.provider.Contacts.Phones._ID);
        sPhoneProjectionMap.put(android.provider.Contacts.Phones.PERSON_ID,
                android.provider.Contacts.Phones.PERSON_ID);
        sPhoneProjectionMap.put(android.provider.Contacts.Phones.ISPRIMARY,
                android.provider.Contacts.Phones.ISPRIMARY);
        sPhoneProjectionMap.put(android.provider.Contacts.Phones.NUMBER,
                android.provider.Contacts.Phones.NUMBER);
        sPhoneProjectionMap.put(android.provider.Contacts.Phones.TYPE,
                android.provider.Contacts.Phones.TYPE);
        sPhoneProjectionMap.put(android.provider.Contacts.Phones.LABEL,
                android.provider.Contacts.Phones.LABEL);
        sPhoneProjectionMap.put(android.provider.Contacts.Phones.NUMBER_KEY,
                android.provider.Contacts.Phones.NUMBER_KEY);

        sExtensionProjectionMap = new HashMap<String, String>();
        sExtensionProjectionMap.put(android.provider.Contacts.Extensions._ID,
                android.provider.Contacts.Extensions._ID);
        sExtensionProjectionMap.put(android.provider.Contacts.Extensions.PERSON_ID,
                android.provider.Contacts.Extensions.PERSON_ID);
        sExtensionProjectionMap.put(android.provider.Contacts.Extensions.NAME,
                android.provider.Contacts.Extensions.NAME);
        sExtensionProjectionMap.put(android.provider.Contacts.Extensions.VALUE,
                android.provider.Contacts.Extensions.VALUE);

        sGroupProjectionMap = new HashMap<String, String>();
        sGroupProjectionMap.put(android.provider.Contacts.Groups._ID,
                android.provider.Contacts.Groups._ID);
        sGroupProjectionMap.put(android.provider.Contacts.Groups.NAME,
                android.provider.Contacts.Groups.NAME);
        sGroupProjectionMap.put(android.provider.Contacts.Groups.NOTES,
                android.provider.Contacts.Groups.NOTES);
        sGroupProjectionMap.put(android.provider.Contacts.Groups.SYSTEM_ID,
                android.provider.Contacts.Groups.SYSTEM_ID);

        sGroupMembershipProjectionMap = new HashMap<String, String>(sGroupProjectionMap);
        sGroupMembershipProjectionMap.put(android.provider.Contacts.GroupMembership._ID,
                android.provider.Contacts.GroupMembership._ID);
        sGroupMembershipProjectionMap.put(android.provider.Contacts.GroupMembership.PERSON_ID,
                android.provider.Contacts.GroupMembership.PERSON_ID);
        sGroupMembershipProjectionMap.put(android.provider.Contacts.GroupMembership.GROUP_ID,
                android.provider.Contacts.GroupMembership.GROUP_ID);
        sGroupMembershipProjectionMap.put(
                android.provider.Contacts.GroupMembership.GROUP_SYNC_ID,
                android.provider.Contacts.GroupMembership.GROUP_SYNC_ID);
        sGroupMembershipProjectionMap.put(
                android.provider.Contacts.GroupMembership.GROUP_SYNC_ACCOUNT,
                android.provider.Contacts.GroupMembership.GROUP_SYNC_ACCOUNT);
        sGroupMembershipProjectionMap.put(
                android.provider.Contacts.GroupMembership.GROUP_SYNC_ACCOUNT_TYPE,
                android.provider.Contacts.GroupMembership.GROUP_SYNC_ACCOUNT_TYPE);

        sPhotoProjectionMap = new HashMap<String, String>();
        sPhotoProjectionMap.put(android.provider.Contacts.Photos._ID,
                android.provider.Contacts.Photos._ID);
        sPhotoProjectionMap.put(android.provider.Contacts.Photos.PERSON_ID,
                android.provider.Contacts.Photos.PERSON_ID);
        sPhotoProjectionMap.put(android.provider.Contacts.Photos.DATA,
                android.provider.Contacts.Photos.DATA);
        sPhotoProjectionMap.put(android.provider.Contacts.Photos.LOCAL_VERSION,
                android.provider.Contacts.Photos.LOCAL_VERSION);
        sPhotoProjectionMap.put(android.provider.Contacts.Photos.DOWNLOAD_REQUIRED,
                android.provider.Contacts.Photos.DOWNLOAD_REQUIRED);
        sPhotoProjectionMap.put(android.provider.Contacts.Photos.EXISTS_ON_SERVER,
                android.provider.Contacts.Photos.EXISTS_ON_SERVER);
        sPhotoProjectionMap.put(android.provider.Contacts.Photos.SYNC_ERROR,
                android.provider.Contacts.Photos.SYNC_ERROR);
    }

    private final Context mContext;
    private final ContactsDatabaseHelper mDbHelper;
    private final ContactsProvider2 mContactsProvider;
    private final NameSplitter mPhoneticNameSplitter;
    private final GlobalSearchSupport mGlobalSearchSupport;

    private final SQLiteStatement mDataMimetypeQuery;
    private final SQLiteStatement mDataRawContactIdQuery;

    private final ContentValues mValues = new ContentValues();
    private final ContentValues mValues2 = new ContentValues();
    private final ContentValues mValues3 = new ContentValues();
    private boolean mDefaultAccountKnown;
    private Account mAccount;

    private final long mMimetypeEmail;
    private final long mMimetypeIm;
    private final long mMimetypePostal;


    public LegacyApiSupport(Context context, ContactsDatabaseHelper contactsDatabaseHelper,
            ContactsProvider2 contactsProvider, GlobalSearchSupport globalSearchSupport) {
        mContext = context;
        mContactsProvider = contactsProvider;
        mDbHelper = contactsDatabaseHelper;
        mGlobalSearchSupport = globalSearchSupport;

        mPhoneticNameSplitter = new NameSplitter("", "", "", context
                .getString(com.android.internal.R.string.common_name_conjunctions), Locale
                .getDefault());

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        mDataMimetypeQuery = db.compileStatement(
                "SELECT " + DataColumns.MIMETYPE_ID +
                " FROM " + Tables.DATA +
                " WHERE " + Data._ID + "=?");

        mDataRawContactIdQuery = db.compileStatement(
                "SELECT " + Data.RAW_CONTACT_ID +
                " FROM " + Tables.DATA +
                " WHERE " + Data._ID + "=?");

        mMimetypeEmail = mDbHelper.getMimeTypeId(Email.CONTENT_ITEM_TYPE);
        mMimetypeIm = mDbHelper.getMimeTypeId(Im.CONTENT_ITEM_TYPE);
        mMimetypePostal = mDbHelper.getMimeTypeId(StructuredPostal.CONTENT_ITEM_TYPE);
    }

    private void ensureDefaultAccount() {
        if (!mDefaultAccountKnown) {
            mAccount = mContactsProvider.getDefaultAccount();
            mDefaultAccountKnown = true;
        }
    }

    public static void createDatabase(SQLiteDatabase db) {
        Log.i(TAG, "Bootstrapping database legacy support");
        createViews(db);
        createSettingsTable(db);
    }

    public static void createViews(SQLiteDatabase db) {

        String peopleColumns = "name." + StructuredName.DISPLAY_NAME
                        + " AS " + People.NAME + ", " +
                Tables.RAW_CONTACTS + "." + RawContactsColumns.DISPLAY_NAME
                        + " AS " + People.DISPLAY_NAME + ", " +
                PHONETIC_NAME_SQL
                        + " AS " + People.PHONETIC_NAME + " , " +
                "note." + Note.NOTE
                        + " AS " + People.NOTES + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_NAME + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_TYPE + ", " +
                Tables.RAW_CONTACTS + "." + RawContacts.TIMES_CONTACTED
                        + " AS " + People.TIMES_CONTACTED + ", " +
                Tables.RAW_CONTACTS + "." + RawContacts.LAST_TIME_CONTACTED
                        + " AS " + People.LAST_TIME_CONTACTED + ", " +
                Tables.RAW_CONTACTS + "." + RawContacts.CUSTOM_RINGTONE
                        + " AS " + People.CUSTOM_RINGTONE + ", " +
                Tables.RAW_CONTACTS + "." + RawContacts.SEND_TO_VOICEMAIL
                        + " AS " + People.SEND_TO_VOICEMAIL + ", " +
                Tables.RAW_CONTACTS + "." + RawContacts.STARRED
                        + " AS " + People.STARRED + ", " +
                "organization." + Data._ID
                        + " AS " + People.PRIMARY_ORGANIZATION_ID + ", " +
                "email." + Data._ID
                        + " AS " + People.PRIMARY_EMAIL_ID + ", " +
                "phone." + Data._ID
                        + " AS " + People.PRIMARY_PHONE_ID + ", " +
                "phone." + Phone.NUMBER
                        + " AS " + People.NUMBER + ", " +
                "phone." + Phone.TYPE
                        + " AS " + People.TYPE + ", " +
                "phone." + Phone.LABEL
                        + " AS " + People.LABEL + ", " +
                "_PHONE_NUMBER_STRIPPED_REVERSED(phone." + Phone.NUMBER + ")"
                        + " AS " + People.NUMBER_KEY;

        db.execSQL("DROP VIEW IF EXISTS " + LegacyTables.PEOPLE + ";");
        db.execSQL("CREATE VIEW " + LegacyTables.PEOPLE + " AS SELECT " +
                RawContactsColumns.CONCRETE_ID
                        + " AS " + android.provider.Contacts.People._ID + ", " +
                peopleColumns +
                " FROM " + Tables.RAW_CONTACTS + PEOPLE_JOINS +
                " WHERE " + Tables.RAW_CONTACTS + "." + RawContacts.DELETED + "=0;");

        db.execSQL("DROP VIEW IF EXISTS " + LegacyTables.ORGANIZATIONS + ";");
        db.execSQL("CREATE VIEW " + LegacyTables.ORGANIZATIONS + " AS SELECT " +
                DataColumns.CONCRETE_ID
                        + " AS " + android.provider.Contacts.Organizations._ID + ", " +
                Data.RAW_CONTACT_ID
                        + " AS " + android.provider.Contacts.Organizations.PERSON_ID + ", " +
                Data.IS_PRIMARY
                        + " AS " + android.provider.Contacts.Organizations.ISPRIMARY + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_NAME + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_TYPE + ", " +
                Organization.COMPANY
                        + " AS " + android.provider.Contacts.Organizations.COMPANY + ", " +
                Organization.TYPE
                        + " AS " + android.provider.Contacts.Organizations.TYPE + ", " +
                Organization.LABEL
                        + " AS " + android.provider.Contacts.Organizations.LABEL + ", " +
                Organization.TITLE
                        + " AS " + android.provider.Contacts.Organizations.TITLE +
                " FROM " + Tables.DATA_JOIN_MIMETYPE_RAW_CONTACTS +
                " WHERE " + MimetypesColumns.CONCRETE_MIMETYPE + "='"
                        + Organization.CONTENT_ITEM_TYPE + "'"
                        + " AND " + Tables.RAW_CONTACTS + "." + RawContacts.DELETED + "=0" +
        ";");

        db.execSQL("DROP VIEW IF EXISTS " + LegacyTables.CONTACT_METHODS + ";");
        db.execSQL("CREATE VIEW " + LegacyTables.CONTACT_METHODS + " AS SELECT " +
                DataColumns.CONCRETE_ID
                        + " AS " + ContactMethods._ID + ", " +
                DataColumns.CONCRETE_RAW_CONTACT_ID
                        + " AS " + ContactMethods.PERSON_ID + ", " +
                CONTACT_METHOD_KIND_SQL
                        + " AS " + ContactMethods.KIND + ", " +
                DataColumns.CONCRETE_IS_PRIMARY
                        + " AS " + ContactMethods.ISPRIMARY + ", " +
                Tables.DATA + "." + Email.TYPE
                        + " AS " + ContactMethods.TYPE + ", " +
                CONTACT_METHOD_DATA_SQL
                        + " AS " + ContactMethods.DATA + ", " +
                Tables.DATA + "." + Email.LABEL
                        + " AS " + ContactMethods.LABEL + ", " +
                DataColumns.CONCRETE_DATA14
                        + " AS " + ContactMethods.AUX_DATA + ", " +
                peopleColumns +
                " FROM " + Tables.DATA + DATA_JOINS +
                " WHERE " + ContactMethods.KIND + " IS NOT NULL"
                    + " AND " + Tables.RAW_CONTACTS + "." + RawContacts.DELETED + "=0" +
        ";");


        db.execSQL("DROP VIEW IF EXISTS " + LegacyTables.PHONES + ";");
        db.execSQL("CREATE VIEW " + LegacyTables.PHONES + " AS SELECT DISTINCT " +
                DataColumns.CONCRETE_ID
                        + " AS " + android.provider.Contacts.Phones._ID + ", " +
                DataColumns.CONCRETE_RAW_CONTACT_ID
                        + " AS " + android.provider.Contacts.Phones.PERSON_ID + ", " +
                DataColumns.CONCRETE_IS_PRIMARY
                        + " AS " + android.provider.Contacts.Phones.ISPRIMARY + ", " +
                Tables.DATA + "." + Phone.NUMBER
                        + " AS " + android.provider.Contacts.Phones.NUMBER + ", " +
                Tables.DATA + "." + Phone.TYPE
                        + " AS " + android.provider.Contacts.Phones.TYPE + ", " +
                Tables.DATA + "." + Phone.LABEL
                        + " AS " + android.provider.Contacts.Phones.LABEL + ", " +
                "_PHONE_NUMBER_STRIPPED_REVERSED(" + Tables.DATA + "." + Phone.NUMBER + ")"
                        + " AS " + android.provider.Contacts.Phones.NUMBER_KEY + ", " +
                peopleColumns +
                " FROM " + Tables.DATA
                        + " JOIN " + Tables.PHONE_LOOKUP
                        + " ON (" + Tables.DATA + "._id = "
                                + Tables.PHONE_LOOKUP + "." + PhoneLookupColumns.DATA_ID + ")"
                        + DATA_JOINS +
                " WHERE " + MimetypesColumns.CONCRETE_MIMETYPE + "='"
                        + Phone.CONTENT_ITEM_TYPE + "'"
                        + " AND " + Tables.RAW_CONTACTS + "." + RawContacts.DELETED + "=0" +
        ";");

        db.execSQL("DROP VIEW IF EXISTS " + LegacyTables.EXTENSIONS + ";");
        db.execSQL("CREATE VIEW " + LegacyTables.EXTENSIONS + " AS SELECT " +
                DataColumns.CONCRETE_ID
                        + " AS " + android.provider.Contacts.Extensions._ID + ", " +
                DataColumns.CONCRETE_RAW_CONTACT_ID
                        + " AS " + android.provider.Contacts.Extensions.PERSON_ID + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_NAME + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_TYPE + ", " +
                ExtensionsColumns.NAME
                        + " AS " + android.provider.Contacts.Extensions.NAME + ", " +
                ExtensionsColumns.VALUE
                        + " AS " + android.provider.Contacts.Extensions.VALUE +
                " FROM " + Tables.DATA_JOIN_MIMETYPE_RAW_CONTACTS +
                " WHERE " + MimetypesColumns.CONCRETE_MIMETYPE + "='"
                        + android.provider.Contacts.Extensions.CONTENT_ITEM_TYPE + "'"
                        + " AND " + Tables.RAW_CONTACTS + "." + RawContacts.DELETED + "=0" +
        ";");

        db.execSQL("DROP VIEW IF EXISTS " + LegacyTables.GROUPS + ";");
        db.execSQL("CREATE VIEW " + LegacyTables.GROUPS + " AS SELECT " +
                GroupsColumns.CONCRETE_ID + " AS " + android.provider.Contacts.Groups._ID + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_NAME + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_TYPE + ", " +
                Groups.TITLE + " AS " + android.provider.Contacts.Groups.NAME + ", " +
                Groups.NOTES + " AS " + android.provider.Contacts.Groups.NOTES + " , " +
                Groups.SYSTEM_ID + " AS " + android.provider.Contacts.Groups.SYSTEM_ID +
                " FROM " + Tables.GROUPS +
                " JOIN " + Tables.ACCOUNTS + " ON (" +
                GroupsColumns.CONCRETE_ACCOUNT_ID + "=" + AccountsColumns.CONCRETE_ID + ")" +
        ";");

        db.execSQL("DROP VIEW IF EXISTS " + LegacyTables.GROUP_MEMBERSHIP + ";");
        db.execSQL("CREATE VIEW " + LegacyTables.GROUP_MEMBERSHIP + " AS SELECT " +
                DataColumns.CONCRETE_ID
                        + " AS " + android.provider.Contacts.GroupMembership._ID + ", " +
                DataColumns.CONCRETE_RAW_CONTACT_ID
                        + " AS " + android.provider.Contacts.GroupMembership.PERSON_ID + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_NAME + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_TYPE + ", " +
                GroupMembership.GROUP_ROW_ID
                        + " AS " + android.provider.Contacts.GroupMembership.GROUP_ID + ", " +
                Groups.TITLE
                        + " AS " + android.provider.Contacts.GroupMembership.NAME + ", " +
                Groups.NOTES
                        + " AS " + android.provider.Contacts.GroupMembership.NOTES + ", " +
                Groups.SYSTEM_ID
                        + " AS " + android.provider.Contacts.GroupMembership.SYSTEM_ID + ", " +
                GroupsColumns.CONCRETE_SOURCE_ID
                        + " AS "
                        + android.provider.Contacts.GroupMembership.GROUP_SYNC_ID + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_NAME
                        + " AS "
                        + android.provider.Contacts.GroupMembership.GROUP_SYNC_ACCOUNT + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_TYPE
                        + " AS "
                        + android.provider.Contacts.GroupMembership.GROUP_SYNC_ACCOUNT_TYPE +
                " FROM " + Tables.DATA_JOIN_PACKAGES_MIMETYPES_RAW_CONTACTS_GROUPS +
                " WHERE " + MimetypesColumns.CONCRETE_MIMETYPE + "='"
                        + GroupMembership.CONTENT_ITEM_TYPE + "'"
                        + " AND " + Tables.RAW_CONTACTS + "." + RawContacts.DELETED + "=0" +
        ";");

        db.execSQL("DROP VIEW IF EXISTS " + LegacyTables.PHOTOS + ";");
        db.execSQL("CREATE VIEW " + LegacyTables.PHOTOS + " AS SELECT " +
                DataColumns.CONCRETE_ID
                        + " AS " + android.provider.Contacts.Photos._ID + ", " +
                DataColumns.CONCRETE_RAW_CONTACT_ID
                        + " AS " + android.provider.Contacts.Photos.PERSON_ID + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_NAME + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_TYPE + ", " +
                Tables.DATA + "." + Photo.PHOTO
                        + " AS " + android.provider.Contacts.Photos.DATA + ", " +
                "legacy_photo." + LegacyPhotoData.EXISTS_ON_SERVER
                        + " AS " + android.provider.Contacts.Photos.EXISTS_ON_SERVER + ", " +
                "legacy_photo." + LegacyPhotoData.DOWNLOAD_REQUIRED
                        + " AS " + android.provider.Contacts.Photos.DOWNLOAD_REQUIRED + ", " +
                "legacy_photo." + LegacyPhotoData.LOCAL_VERSION
                        + " AS " + android.provider.Contacts.Photos.LOCAL_VERSION + ", " +
                "legacy_photo." + LegacyPhotoData.SYNC_ERROR
                        + " AS " + android.provider.Contacts.Photos.SYNC_ERROR +
                " FROM " + Tables.DATA + DATA_JOINS + LEGACY_PHOTO_JOIN +
                " WHERE " + MimetypesColumns.CONCRETE_MIMETYPE + "='"
                        + Photo.CONTENT_ITEM_TYPE + "'"
                        + " AND " + Tables.RAW_CONTACTS + "." + RawContacts.DELETED + "=0" +
        ";");

    }

    public static void createSettingsTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + LegacyTables.SETTINGS + ";");
        db.execSQL("CREATE TABLE " + LegacyTables.SETTINGS + " (" +
                android.provider.Contacts.Settings._ID + " INTEGER PRIMARY KEY," +
                android.provider.Contacts.Settings._SYNC_ACCOUNT + " TEXT," +
                android.provider.Contacts.Settings._SYNC_ACCOUNT_TYPE + " TEXT," +
                android.provider.Contacts.Settings.KEY + " STRING NOT NULL," +
                android.provider.Contacts.Settings.VALUE + " STRING " +
        ");");
    }

    public Uri insert(Uri uri, ContentValues values) {
        ensureDefaultAccount();
        final int match = sUriMatcher.match(uri);
        long id = 0;
        switch (match) {
            case PEOPLE:
                id = insertPeople(values);
                break;

            case ORGANIZATIONS:
                id = insertOrganization(values);
                break;

            case PEOPLE_CONTACTMETHODS: {
                long rawContactId = Long.parseLong(uri.getPathSegments().get(1));
                id = insertContactMethod(rawContactId, values);
                break;
            }

            case CONTACTMETHODS: {
                long rawContactId = getRequiredValue(values, ContactMethods.PERSON_ID);
                id = insertContactMethod(rawContactId, values);
                break;
            }

            case PHONES: {
                long rawContactId = getRequiredValue(values,
                        android.provider.Contacts.Phones.PERSON_ID);
                id = insertPhone(rawContactId, values);
                break;
            }

            case PEOPLE_PHONES: {
                long rawContactId = Long.parseLong(uri.getPathSegments().get(1));
                id = insertPhone(rawContactId, values);
                break;
            }

            case EXTENSIONS: {
                long rawContactId = getRequiredValue(values,
                        android.provider.Contacts.Extensions.PERSON_ID);
                id = insertExtension(rawContactId, values);
                break;
            }

            case GROUPS:
                id = insertGroup(values);
                break;

            case GROUPMEMBERSHIP: {
                long rawContactId = getRequiredValue(values,
                        android.provider.Contacts.GroupMembership.PERSON_ID);
                long groupId = getRequiredValue(values,
                        android.provider.Contacts.GroupMembership.GROUP_ID);
                id = insertGroupMembership(rawContactId, groupId);
                break;
            }

            default:
                throw new UnsupportedOperationException(mDbHelper.exceptionMessage(uri));
        }

        if (id < 0) {
            return null;
        }

        final Uri result = ContentUris.withAppendedId(uri, id);
        onChange(result);
        return result;
    }

    private long getRequiredValue(ContentValues values, String column) {
        final Long value = values.getAsLong(column);
        if (value == null) {
            throw new RuntimeException("Required value: " + column);
        }

        return value;
    }

    private long insertPeople(ContentValues values) {
        parsePeopleValues(values);

        Uri contactUri = mContactsProvider.insertInTransaction(RawContacts.CONTENT_URI, mValues);
        long rawContactId = ContentUris.parseId(contactUri);

        if (mValues2.size() != 0) {
            mValues2.put(Data.RAW_CONTACT_ID, rawContactId);
            mContactsProvider.insertInTransaction(Data.CONTENT_URI, mValues2);
        }
        if (mValues3.size() != 0) {
            mValues3.put(Data.RAW_CONTACT_ID, rawContactId);
            mContactsProvider.insertInTransaction(Data.CONTENT_URI, mValues3);
        }

        return rawContactId;
    }

    private long insertOrganization(ContentValues values) {
        parseOrganizationValues(values);
        ContactsDatabaseHelper.copyLongValue(mValues, Data.RAW_CONTACT_ID,
                values, android.provider.Contacts.Organizations.PERSON_ID);

        Uri uri = mContactsProvider.insertInTransaction(Data.CONTENT_URI, mValues);

        return ContentUris.parseId(uri);
    }

    private long insertPhone(long rawContactId, ContentValues values) {
        parsePhoneValues(values);
        mValues.put(Data.RAW_CONTACT_ID, rawContactId);

        Uri uri = mContactsProvider.insertInTransaction(Data.CONTENT_URI, mValues);

        return ContentUris.parseId(uri);
    }

    private long insertContactMethod(long rawContactId, ContentValues values) {
        Integer kind = values.getAsInteger(ContactMethods.KIND);
        if (kind == null) {
            throw new RuntimeException("Required value: " + ContactMethods.KIND);
        }

        parseContactMethodValues(kind, values);

        mValues.put(Data.RAW_CONTACT_ID, rawContactId);
        Uri uri = mContactsProvider.insertInTransaction(Data.CONTENT_URI, mValues);
        return ContentUris.parseId(uri);
    }

    private long insertExtension(long rawContactId, ContentValues values) {
        mValues.clear();

        mValues.put(Data.RAW_CONTACT_ID, rawContactId);
        mValues.put(Data.MIMETYPE, android.provider.Contacts.Extensions.CONTENT_ITEM_TYPE);

        parseExtensionValues(values);

        Uri uri = mContactsProvider.insertInTransaction(Data.CONTENT_URI, mValues);
        return ContentUris.parseId(uri);
    }

    private long insertGroup(ContentValues values) {
        parseGroupValues(values);

        if (mAccount != null) {
            mValues.put(Groups.ACCOUNT_NAME, mAccount.name);
            mValues.put(Groups.ACCOUNT_TYPE, mAccount.type);
        }

        Uri uri = mContactsProvider.insertInTransaction(Groups.CONTENT_URI, mValues);
        return ContentUris.parseId(uri);
    }

    private long insertGroupMembership(long rawContactId, long groupId) {
        mValues.clear();

        mValues.put(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
        mValues.put(GroupMembership.RAW_CONTACT_ID, rawContactId);
        mValues.put(GroupMembership.GROUP_ROW_ID, groupId);

        Uri uri = mContactsProvider.insertInTransaction(Data.CONTENT_URI, mValues);
        return ContentUris.parseId(uri);
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        ensureDefaultAccount();

        int match = sUriMatcher.match(uri);
        int count = 0;
        switch(match) {
            case PEOPLE_UPDATE_CONTACT_TIME: {
                count = updateContactTime(uri, values);
                break;
            }

            case PEOPLE_PHOTO: {
                long rawContactId = Long.parseLong(uri.getPathSegments().get(1));
                return updatePhoto(rawContactId, values);
            }

            case SETTINGS: {
                return updateSettings(values);
            }

            case GROUPMEMBERSHIP:
            case GROUPMEMBERSHIP_ID:
            case -1: {
                throw new UnsupportedOperationException(mDbHelper.exceptionMessage(uri));
            }

            default: {
                count = updateAll(uri, match, values, selection, selectionArgs);
            }
        }

        if (count > 0) {
            mContext.getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    private int updateAll(Uri uri, final int match, ContentValues values, String selection,
            String[] selectionArgs) {
        Cursor c = query(uri, IdQuery.COLUMNS, selection, selectionArgs, null, null);
        if (c == null) {
            return 0;
        }

        int count = 0;
        try {
            while (c.moveToNext()) {
                long id = c.getLong(IdQuery._ID);
                count += update(match, id, values);
            }
        } finally {
            c.close();
        }

        return count;
    }

    public int update(int match, long id, ContentValues values) {
        int count = 0;
        switch(match) {
            case PEOPLE:
            case PEOPLE_ID: {
                count = updatePeople(id, values);
                break;
            }

            case ORGANIZATIONS:
            case ORGANIZATIONS_ID: {
                count = updateOrganizations(id, values);
                break;
            }

            case PHONES:
            case PHONES_ID: {
                count = updatePhones(id, values);
                break;
            }

            case CONTACTMETHODS:
            case CONTACTMETHODS_ID: {
                count = updateContactMethods(id, values);
                break;
            }

            case EXTENSIONS:
            case EXTENSIONS_ID: {
                count = updateExtensions(id, values);
                break;
            }

            case GROUPS:
            case GROUPS_ID: {
                count = updateGroups(id, values);
                break;
            }

            case PHOTOS:
            case PHOTOS_ID:
                count = updatePhotoByDataId(id, values);
                break;
        }

        return count;
    }

    private int updatePeople(long rawContactId, ContentValues values) {
        parsePeopleValues(values);

        int count = mContactsProvider.updateInTransaction(RawContacts.CONTENT_URI,
                mValues, RawContacts._ID + "=" + rawContactId, null);

        if (count == 0) {
            return 0;
        }

        if (mValues2.size() != 0) {
            Uri dataUri = findFirstDataRow(rawContactId, StructuredName.CONTENT_ITEM_TYPE);
            if (dataUri != null) {
                mContactsProvider.updateInTransaction(dataUri, mValues2, null, null);
            } else {
                mValues2.put(Data.RAW_CONTACT_ID, rawContactId);
                mContactsProvider.insertInTransaction(Data.CONTENT_URI, mValues2);
            }
        }

        if (mValues3.size() != 0) {
            Uri dataUri = findFirstDataRow(rawContactId, Note.CONTENT_ITEM_TYPE);
            if (dataUri != null) {
                mContactsProvider.updateInTransaction(dataUri, mValues3, null, null);
            } else {
                mValues3.put(Data.RAW_CONTACT_ID, rawContactId);
                mContactsProvider.insertInTransaction(Data.CONTENT_URI, mValues3);
            }
        }

        if (values.containsKey(People.LAST_TIME_CONTACTED) &&
                !values.containsKey(People.TIMES_CONTACTED)) {
            updateContactTime(rawContactId, values);
        }

        return count;
    }

    private int updateOrganizations(long dataId, ContentValues values) {
        parseOrganizationValues(values);

        return mContactsProvider.updateInTransaction(Data.CONTENT_URI, mValues,
                Data._ID + "=" + dataId, null);
    }

    private int updatePhones(long dataId, ContentValues values) {
        parsePhoneValues(values);

        return mContactsProvider.updateInTransaction(Data.CONTENT_URI, mValues,
                Data._ID + "=" + dataId, null);
    }

    private int updateContactMethods(long dataId, ContentValues values) {
        int kind;

        mDataMimetypeQuery.bindLong(1, dataId);
        long mimetype_id;
        try {
            mimetype_id = mDataMimetypeQuery.simpleQueryForLong();
        } catch (SQLiteDoneException e) {
            // Data row not found
            return 0;
        }

        if (mimetype_id == mMimetypeEmail) {
            kind = android.provider.Contacts.KIND_EMAIL;
        } else if (mimetype_id == mMimetypeIm) {
            kind = android.provider.Contacts.KIND_IM;
        } else if (mimetype_id == mMimetypePostal) {
            kind = android.provider.Contacts.KIND_POSTAL;
        } else {

            // Non-legacy kind: return "Not found"
            return 0;
        }

        parseContactMethodValues(kind, values);

        return mContactsProvider.updateInTransaction(Data.CONTENT_URI, mValues,
                Data._ID + "=" + dataId, null);
    }

    private int updateExtensions(long dataId, ContentValues values) {
        parseExtensionValues(values);

        return mContactsProvider.updateInTransaction(Data.CONTENT_URI, mValues,
                Data._ID + "=" + dataId, null);
    }

    private int updateGroups(long groupId, ContentValues values) {
        parseGroupValues(values);

        return mContactsProvider.updateInTransaction(Groups.CONTENT_URI, mValues,
                Groups._ID + "=" + groupId, null);
    }

    private int updateContactTime(Uri uri, ContentValues values) {
        long rawContactId = Long.parseLong(uri.getPathSegments().get(1));
        updateContactTime(rawContactId, values);
        return 1;
    }

    private void updateContactTime(long rawContactId, ContentValues values) {
        final Long storedTimeContacted = values.getAsLong(People.LAST_TIME_CONTACTED);
        final long lastTimeContacted = storedTimeContacted != null ?
            storedTimeContacted : System.currentTimeMillis();

        // TODO check sanctions
        long contactId = mDbHelper.getContactId(rawContactId);
        SQLiteDatabase mDb = mDbHelper.getWritableDatabase();
        mSelectionArgs2[0] = String.valueOf(lastTimeContacted);
        if (contactId != 0) {
            mSelectionArgs2[1] = String.valueOf(contactId);
            mDb.execSQL(CONTACTS_UPDATE_LASTTIMECONTACTED, mSelectionArgs2);
            // increment times_contacted column
            mSelectionArgs1[0] = String.valueOf(contactId);
            mDb.execSQL(ContactsProvider2.UPDATE_TIMES_CONTACTED_CONTACTS_TABLE, mSelectionArgs1);
        }
        mSelectionArgs2[1] = String.valueOf(rawContactId);
        mDb.execSQL(RAWCONTACTS_UPDATE_LASTTIMECONTACTED, mSelectionArgs2);
        // increment times_contacted column
        mSelectionArgs1[0] = String.valueOf(contactId);
        mDb.execSQL(ContactsProvider2.UPDATE_TIMES_CONTACTED_RAWCONTACTS_TABLE, mSelectionArgs1);
    }

    private int updatePhoto(long rawContactId, ContentValues values) {

        // TODO check sanctions

        int count;

        long dataId = findFirstDataId(rawContactId, Photo.CONTENT_ITEM_TYPE);

        mValues.clear();
        byte[] bytes = values.getAsByteArray(android.provider.Contacts.Photos.DATA);
        mValues.put(Photo.PHOTO, bytes);

        if (dataId == -1) {
            mValues.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
            mValues.put(Data.RAW_CONTACT_ID, rawContactId);
            Uri dataUri = mContactsProvider.insertInTransaction(Data.CONTENT_URI, mValues);
            dataId = ContentUris.parseId(dataUri);
            count = 1;
        } else {
            Uri dataUri = ContentUris.withAppendedId(Data.CONTENT_URI, dataId);
            count = mContactsProvider.updateInTransaction(dataUri, mValues, null, null);
        }

        updateLegacyPhotoData(rawContactId, dataId, values);

        return count;
    }

    private int updatePhotoByDataId(long dataId, ContentValues values) {

        mDataRawContactIdQuery.bindLong(1, dataId);
        long rawContactId;

        try {
            rawContactId = mDataRawContactIdQuery.simpleQueryForLong();
        } catch (SQLiteDoneException e) {
            // Data row not found
            return 0;
        }

        if (values.containsKey(android.provider.Contacts.Photos.DATA)) {
            byte[] bytes = values.getAsByteArray(android.provider.Contacts.Photos.DATA);
            mValues.clear();
            mValues.put(Photo.PHOTO, bytes);
            mContactsProvider.updateInTransaction(Data.CONTENT_URI, mValues,
                    Data._ID + "=" + dataId, null);
        }

        updateLegacyPhotoData(rawContactId, dataId, values);

        return 1;
    }

    private void updateLegacyPhotoData(long rawContactId, long dataId, ContentValues values) {
        mValues.clear();
        ContactsDatabaseHelper.copyStringValue(mValues, LegacyPhotoData.LOCAL_VERSION,
                values, android.provider.Contacts.Photos.LOCAL_VERSION);
        ContactsDatabaseHelper.copyStringValue(mValues, LegacyPhotoData.DOWNLOAD_REQUIRED,
                values, android.provider.Contacts.Photos.DOWNLOAD_REQUIRED);
        ContactsDatabaseHelper.copyStringValue(mValues, LegacyPhotoData.EXISTS_ON_SERVER,
                values, android.provider.Contacts.Photos.EXISTS_ON_SERVER);
        ContactsDatabaseHelper.copyStringValue(mValues, LegacyPhotoData.SYNC_ERROR,
                values, android.provider.Contacts.Photos.SYNC_ERROR);

        int updated = mContactsProvider.updateInTransaction(Data.CONTENT_URI, mValues,
                Data.MIMETYPE + "='" + LegacyPhotoData.CONTENT_ITEM_TYPE + "'"
                        + " AND " + Data.RAW_CONTACT_ID + "=" + rawContactId
                        + " AND " + LegacyPhotoData.PHOTO_DATA_ID + "=" + dataId, null);
        if (updated == 0) {
            mValues.put(Data.RAW_CONTACT_ID, rawContactId);
            mValues.put(Data.MIMETYPE, LegacyPhotoData.CONTENT_ITEM_TYPE);
            mValues.put(LegacyPhotoData.PHOTO_DATA_ID, dataId);
            mContactsProvider.insertInTransaction(Data.CONTENT_URI, mValues);
        }
    }

    private int updateSettings(ContentValues values) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String accountName = values.getAsString(android.provider.Contacts.Settings._SYNC_ACCOUNT);
        String accountType =
                values.getAsString(android.provider.Contacts.Settings._SYNC_ACCOUNT_TYPE);
        String key = values.getAsString(android.provider.Contacts.Settings.KEY);
        if (key == null) {
            throw new IllegalArgumentException("you must specify the key when updating settings");
        }
        updateSetting(db, accountName, accountType, values);
        if (key.equals(android.provider.Contacts.Settings.SYNC_EVERYTHING)) {
            mValues.clear();
            mValues.put(Settings.SHOULD_SYNC,
                    values.getAsInteger(android.provider.Contacts.Settings.VALUE));
            String selection;
            String[] selectionArgs;
            if (accountName != null && accountType != null) {

                selectionArgs = new String[]{accountName, accountType};
                selection = Settings.ACCOUNT_NAME + "=?"
                        + " AND " + Settings.ACCOUNT_TYPE + "=?"
                        + " AND " + Settings.DATA_SET + " IS NULL";
            } else {
                selectionArgs = null;
                selection = Settings.ACCOUNT_NAME + " IS NULL"
                        + " AND " + Settings.ACCOUNT_TYPE + " IS NULL"
                        + " AND " + Settings.DATA_SET + " IS NULL";
            }
            int count = mContactsProvider.updateInTransaction(Settings.CONTENT_URI, mValues,
                    selection, selectionArgs);
            if (count == 0) {
                mValues.put(Settings.ACCOUNT_NAME, accountName);
                mValues.put(Settings.ACCOUNT_TYPE, accountType);
                mContactsProvider.insertInTransaction(Settings.CONTENT_URI, mValues);
            }
        }
        return 1;
    }

    private void updateSetting(SQLiteDatabase db, String accountName, String accountType,
            ContentValues values) {
        final String key = values.getAsString(android.provider.Contacts.Settings.KEY);
        if (accountName == null || accountType == null) {
            db.delete(LegacyTables.SETTINGS, "_sync_account IS NULL AND key=?", new String[]{key});
        } else {
            db.delete(LegacyTables.SETTINGS, "_sync_account=? AND _sync_account_type=? AND key=?",
                    new String[]{accountName, accountType, key});
        }
        long rowId = db.insert(LegacyTables.SETTINGS,
                android.provider.Contacts.Settings.KEY, values);
        if (rowId < 0) {
            throw new SQLException("error updating settings with " + values);
        }
    }

    private interface SettingsMatchQuery {
        String SQL =
            "SELECT "
                    + ContactsContract.Settings.ACCOUNT_NAME + ","
                    + ContactsContract.Settings.ACCOUNT_TYPE + ","
                    + ContactsContract.Settings.SHOULD_SYNC +
            " FROM " + Tables.SETTINGS + " LEFT OUTER JOIN " + LegacyTables.SETTINGS +
            " ON (" + ContactsContract.Settings.ACCOUNT_NAME + "="
                              + android.provider.Contacts.Settings._SYNC_ACCOUNT +
                      " AND " + ContactsContract.Settings.ACCOUNT_TYPE + "="
                              + android.provider.Contacts.Settings._SYNC_ACCOUNT_TYPE +
                      " AND " + ContactsContract.Settings.DATA_SET + " IS NULL" +
                      " AND " + android.provider.Contacts.Settings.KEY + "='"
                              + android.provider.Contacts.Settings.SYNC_EVERYTHING + "'" +
            ")" +
            " WHERE " + ContactsContract.Settings.SHOULD_SYNC + "<>"
                            + android.provider.Contacts.Settings.VALUE;

        int ACCOUNT_NAME = 0;
        int ACCOUNT_TYPE = 1;
        int SHOULD_SYNC = 2;
    }

    /**
     * Brings legacy settings table in sync with the new settings.
     */
    public void copySettingsToLegacySettings() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery(SettingsMatchQuery.SQL, null);
        try {
            while(cursor.moveToNext()) {
                String accountName = cursor.getString(SettingsMatchQuery.ACCOUNT_NAME);
                String accountType = cursor.getString(SettingsMatchQuery.ACCOUNT_TYPE);
                String value = cursor.getString(SettingsMatchQuery.SHOULD_SYNC);
                mValues.clear();
                mValues.put(android.provider.Contacts.Settings._SYNC_ACCOUNT, accountName);
                mValues.put(android.provider.Contacts.Settings._SYNC_ACCOUNT_TYPE, accountType);
                mValues.put(android.provider.Contacts.Settings.KEY,
                        android.provider.Contacts.Settings.SYNC_EVERYTHING);
                mValues.put(android.provider.Contacts.Settings.VALUE, value);
                updateSetting(db, accountName, accountType, mValues);
            }
        } finally {
            cursor.close();
        }
    }

    private void parsePeopleValues(ContentValues values) {
        mValues.clear();
        mValues2.clear();
        mValues3.clear();

        ContactsDatabaseHelper.copyStringValue(mValues, RawContacts.CUSTOM_RINGTONE,
                values, People.CUSTOM_RINGTONE);
        ContactsDatabaseHelper.copyLongValue(mValues, RawContacts.SEND_TO_VOICEMAIL,
                values, People.SEND_TO_VOICEMAIL);
        ContactsDatabaseHelper.copyLongValue(mValues, RawContacts.LAST_TIME_CONTACTED,
                values, People.LAST_TIME_CONTACTED);
        ContactsDatabaseHelper.copyLongValue(mValues, RawContacts.TIMES_CONTACTED,
                values, People.TIMES_CONTACTED);
        ContactsDatabaseHelper.copyLongValue(mValues, RawContacts.STARRED,
                values, People.STARRED);
        if (mAccount != null) {
            mValues.put(RawContacts.ACCOUNT_NAME, mAccount.name);
            mValues.put(RawContacts.ACCOUNT_TYPE, mAccount.type);
        }

        if (values.containsKey(People.NAME) || values.containsKey(People.PHONETIC_NAME)) {
            mValues2.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
            ContactsDatabaseHelper.copyStringValue(mValues2, StructuredName.DISPLAY_NAME,
                    values, People.NAME);
            if (values.containsKey(People.PHONETIC_NAME)) {
                String phoneticName = values.getAsString(People.PHONETIC_NAME);
                NameSplitter.Name parsedName = new NameSplitter.Name();
                mPhoneticNameSplitter.split(parsedName, phoneticName);
                mValues2.put(StructuredName.PHONETIC_GIVEN_NAME, parsedName.getGivenNames());
                mValues2.put(StructuredName.PHONETIC_MIDDLE_NAME, parsedName.getMiddleName());
                mValues2.put(StructuredName.PHONETIC_FAMILY_NAME, parsedName.getFamilyName());
            }
        }

        if (values.containsKey(People.NOTES)) {
            mValues3.put(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE);
            ContactsDatabaseHelper.copyStringValue(mValues3, Note.NOTE, values, People.NOTES);
        }
    }

    private void parseOrganizationValues(ContentValues values) {
        mValues.clear();

        mValues.put(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE);

        ContactsDatabaseHelper.copyLongValue(mValues, Data.IS_PRIMARY,
                values, android.provider.Contacts.Organizations.ISPRIMARY);

        ContactsDatabaseHelper.copyStringValue(mValues, Organization.COMPANY,
                values, android.provider.Contacts.Organizations.COMPANY);

        // TYPE values happen to remain the same between V1 and V2 - can just copy the value
        ContactsDatabaseHelper.copyLongValue(mValues, Organization.TYPE,
                values, android.provider.Contacts.Organizations.TYPE);

        ContactsDatabaseHelper.copyStringValue(mValues, Organization.LABEL,
                values, android.provider.Contacts.Organizations.LABEL);
        ContactsDatabaseHelper.copyStringValue(mValues, Organization.TITLE,
                values, android.provider.Contacts.Organizations.TITLE);
    }

    private void parsePhoneValues(ContentValues values) {
        mValues.clear();

        mValues.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);

        ContactsDatabaseHelper.copyLongValue(mValues, Data.IS_PRIMARY,
                values, android.provider.Contacts.Phones.ISPRIMARY);

        ContactsDatabaseHelper.copyStringValue(mValues, Phone.NUMBER,
                values, android.provider.Contacts.Phones.NUMBER);

        // TYPE values happen to remain the same between V1 and V2 - can just copy the value
        ContactsDatabaseHelper.copyLongValue(mValues, Phone.TYPE,
                values, android.provider.Contacts.Phones.TYPE);

        ContactsDatabaseHelper.copyStringValue(mValues, Phone.LABEL,
                values, android.provider.Contacts.Phones.LABEL);
    }

    private void parseContactMethodValues(int kind, ContentValues values) {
        mValues.clear();

        ContactsDatabaseHelper.copyLongValue(mValues, Data.IS_PRIMARY, values,
                ContactMethods.ISPRIMARY);

        switch (kind) {
            case android.provider.Contacts.KIND_EMAIL: {
                copyCommonFields(values, Email.CONTENT_ITEM_TYPE, Email.TYPE, Email.LABEL,
                        Data.DATA14);
                ContactsDatabaseHelper.copyStringValue(mValues, Email.DATA, values,
                        ContactMethods.DATA);
                break;
            }

            case android.provider.Contacts.KIND_IM: {
                String protocol = values.getAsString(ContactMethods.DATA);
                if (protocol.startsWith("pre:")) {
                    mValues.put(Im.PROTOCOL, Integer.parseInt(protocol.substring(4)));
                } else if (protocol.startsWith("custom:")) {
                    mValues.put(Im.PROTOCOL, Im.PROTOCOL_CUSTOM);
                    mValues.put(Im.CUSTOM_PROTOCOL, protocol.substring(7));
                }

                copyCommonFields(values, Im.CONTENT_ITEM_TYPE, Im.TYPE, Im.LABEL, Data.DATA14);
                break;
            }

            case android.provider.Contacts.KIND_POSTAL: {
                copyCommonFields(values, StructuredPostal.CONTENT_ITEM_TYPE, StructuredPostal.TYPE,
                        StructuredPostal.LABEL, Data.DATA14);
                ContactsDatabaseHelper.copyStringValue(mValues, StructuredPostal.FORMATTED_ADDRESS,
                        values, ContactMethods.DATA);
                break;
            }
        }
    }

    private void copyCommonFields(ContentValues values, String mimeType, String typeColumn,
            String labelColumn, String auxDataColumn) {
        mValues.put(Data.MIMETYPE, mimeType);
        ContactsDatabaseHelper.copyLongValue(mValues, typeColumn, values,
                ContactMethods.TYPE);
        ContactsDatabaseHelper.copyStringValue(mValues, labelColumn, values,
                ContactMethods.LABEL);
        ContactsDatabaseHelper.copyStringValue(mValues, auxDataColumn, values,
                ContactMethods.AUX_DATA);
    }

    private void parseGroupValues(ContentValues values) {
        mValues.clear();

        ContactsDatabaseHelper.copyStringValue(mValues, Groups.TITLE,
                values, android.provider.Contacts.Groups.NAME);
        ContactsDatabaseHelper.copyStringValue(mValues, Groups.NOTES,
                values, android.provider.Contacts.Groups.NOTES);
        ContactsDatabaseHelper.copyStringValue(mValues, Groups.SYSTEM_ID,
                values, android.provider.Contacts.Groups.SYSTEM_ID);
    }

    private void parseExtensionValues(ContentValues values) {
        ContactsDatabaseHelper.copyStringValue(mValues, ExtensionsColumns.NAME,
                values, android.provider.Contacts.People.Extensions.NAME);
        ContactsDatabaseHelper.copyStringValue(mValues, ExtensionsColumns.VALUE,
                values, android.provider.Contacts.People.Extensions.VALUE);
    }

    private Uri findFirstDataRow(long rawContactId, String contentItemType) {
        long dataId = findFirstDataId(rawContactId, contentItemType);
        if (dataId == -1) {
            return null;
        }

        return ContentUris.withAppendedId(Data.CONTENT_URI, dataId);
    }

    private long findFirstDataId(long rawContactId, String mimeType) {
        long dataId = -1;
        Cursor c = mContactsProvider.query(Data.CONTENT_URI, IdQuery.COLUMNS,
                Data.RAW_CONTACT_ID + "=" + rawContactId + " AND "
                        + Data.MIMETYPE + "='" + mimeType + "'",
                null, null);
        try {
            if (c.moveToFirst()) {
                dataId = c.getLong(IdQuery._ID);
            }
        } finally {
            c.close();
        }
        return dataId;
    }


    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        if (match == -1 || match == SETTINGS) {
            throw new UnsupportedOperationException(mDbHelper.exceptionMessage(uri));
        }

        Cursor c = query(uri, IdQuery.COLUMNS, selection, selectionArgs, null, null);
        if (c == null) {
            return 0;
        }

        int count = 0;
        try {
            while (c.moveToNext()) {
                long id = c.getLong(IdQuery._ID);
                count += delete(uri, match, id);
            }
        } finally {
            c.close();
        }

        return count;
    }

    public int delete(Uri uri, int match, long id) {
        int count = 0;
        switch (match) {
            case PEOPLE:
            case PEOPLE_ID:
                count = mContactsProvider.deleteRawContact(id, mDbHelper.getContactId(id), false);
                break;

            case PEOPLE_PHOTO:
                mValues.clear();
                mValues.putNull(android.provider.Contacts.Photos.DATA);
                updatePhoto(id, mValues);
                break;

            case ORGANIZATIONS:
            case ORGANIZATIONS_ID:
                count = mContactsProvider.deleteData(id, ORGANIZATION_MIME_TYPES);
                break;

            case CONTACTMETHODS:
            case CONTACTMETHODS_ID:
                count = mContactsProvider.deleteData(id, CONTACT_METHOD_MIME_TYPES);
                break;

            case PHONES:
            case PHONES_ID:
                count = mContactsProvider.deleteData(id, PHONE_MIME_TYPES);
                break;

            case EXTENSIONS:
            case EXTENSIONS_ID:
                count = mContactsProvider.deleteData(id, EXTENSION_MIME_TYPES);
                break;

            case PHOTOS:
            case PHOTOS_ID:
                count = mContactsProvider.deleteData(id, PHOTO_MIME_TYPES);
                break;

            case GROUPMEMBERSHIP:
            case GROUPMEMBERSHIP_ID:
                count = mContactsProvider.deleteData(id, GROUP_MEMBERSHIP_MIME_TYPES);
                break;

            case GROUPS:
            case GROUPS_ID:
                count = mContactsProvider.deleteGroup(uri, id, false);
                break;

            default:
                throw new UnsupportedOperationException(mDbHelper.exceptionMessage(uri));
        }

        return count;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder, String limit) {
        ensureDefaultAccount();

        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String groupBy = null;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PEOPLE: {
                qb.setTables(LegacyTables.PEOPLE_JOIN_PRESENCE);
                qb.setProjectionMap(sPeopleProjectionMap);
                applyRawContactsAccount(qb);
                break;
            }

            case PEOPLE_ID:
                qb.setTables(LegacyTables.PEOPLE_JOIN_PRESENCE);
                qb.setProjectionMap(sPeopleProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + People._ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;

            case PEOPLE_FILTER: {
                qb.setTables(LegacyTables.PEOPLE_JOIN_PRESENCE);
                qb.setProjectionMap(sPeopleProjectionMap);
                applyRawContactsAccount(qb);
                String filterParam = uri.getPathSegments().get(2);
                qb.appendWhere(" AND " + People._ID + " IN "
                        + getRawContactsByFilterAsNestedQuery(filterParam));
                break;
            }

            case GROUP_NAME_MEMBERS:
                qb.setTables(LegacyTables.PEOPLE_JOIN_PRESENCE);
                qb.setProjectionMap(sPeopleProjectionMap);
                applyRawContactsAccount(qb);
                String group = uri.getPathSegments().get(2);
                qb.appendWhere(" AND " + buildGroupNameMatchWhereClause(group));
                break;

            case GROUP_SYSTEM_ID_MEMBERS:
                qb.setTables(LegacyTables.PEOPLE_JOIN_PRESENCE);
                qb.setProjectionMap(sPeopleProjectionMap);
                applyRawContactsAccount(qb);
                String systemId = uri.getPathSegments().get(2);
                qb.appendWhere(" AND " + buildGroupSystemIdMatchWhereClause(systemId));
                break;

            case ORGANIZATIONS:
                qb.setTables(LegacyTables.ORGANIZATIONS + " organizations");
                qb.setProjectionMap(sOrganizationProjectionMap);
                applyRawContactsAccount(qb);
                break;

            case ORGANIZATIONS_ID:
                qb.setTables(LegacyTables.ORGANIZATIONS + " organizations");
                qb.setProjectionMap(sOrganizationProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + android.provider.Contacts.Organizations._ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;

            case PEOPLE_ORGANIZATIONS:
                qb.setTables(LegacyTables.ORGANIZATIONS + " organizations");
                qb.setProjectionMap(sOrganizationProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + android.provider.Contacts.Organizations.PERSON_ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;

            case PEOPLE_ORGANIZATIONS_ID:
                qb.setTables(LegacyTables.ORGANIZATIONS + " organizations");
                qb.setProjectionMap(sOrganizationProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + android.provider.Contacts.Organizations.PERSON_ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                qb.appendWhere(" AND " + android.provider.Contacts.Organizations._ID + "=");
                qb.appendWhere(uri.getPathSegments().get(3));
                break;

            case CONTACTMETHODS:
                qb.setTables(LegacyTables.CONTACT_METHODS + " contact_methods");
                qb.setProjectionMap(sContactMethodProjectionMap);
                applyRawContactsAccount(qb);
                break;

            case CONTACTMETHODS_ID:
                qb.setTables(LegacyTables.CONTACT_METHODS + " contact_methods");
                qb.setProjectionMap(sContactMethodProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + ContactMethods._ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;

            case CONTACTMETHODS_EMAIL:
                qb.setTables(LegacyTables.CONTACT_METHODS + " contact_methods");
                qb.setProjectionMap(sContactMethodProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + ContactMethods.KIND + "="
                        + android.provider.Contacts.KIND_EMAIL);
                break;

            case PEOPLE_CONTACTMETHODS:
                qb.setTables(LegacyTables.CONTACT_METHODS + " contact_methods");
                qb.setProjectionMap(sContactMethodProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + ContactMethods.PERSON_ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                qb.appendWhere(" AND " + ContactMethods.KIND + " IS NOT NULL");
                break;

            case PEOPLE_CONTACTMETHODS_ID:
                qb.setTables(LegacyTables.CONTACT_METHODS + " contact_methods");
                qb.setProjectionMap(sContactMethodProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + ContactMethods.PERSON_ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                qb.appendWhere(" AND " + ContactMethods._ID + "=");
                qb.appendWhere(uri.getPathSegments().get(3));
                qb.appendWhere(" AND " + ContactMethods.KIND + " IS NOT NULL");
                break;

            case PHONES:
                qb.setTables(LegacyTables.PHONES + " phones");
                qb.setProjectionMap(sPhoneProjectionMap);
                applyRawContactsAccount(qb);
                break;

            case PHONES_ID:
                qb.setTables(LegacyTables.PHONES + " phones");
                qb.setProjectionMap(sPhoneProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + android.provider.Contacts.Phones._ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;

            case PHONES_FILTER:
                qb.setTables(LegacyTables.PHONES + " phones");
                qb.setProjectionMap(sPhoneProjectionMap);
                applyRawContactsAccount(qb);
                if (uri.getPathSegments().size() > 2) {
                    String filterParam = uri.getLastPathSegment();
                    qb.appendWhere(" AND person =");
                    qb.appendWhere(mDbHelper.buildPhoneLookupAsNestedQuery(filterParam));
                    qb.setDistinct(true);
                }
                break;

            case PEOPLE_PHONES:
                qb.setTables(LegacyTables.PHONES + " phones");
                qb.setProjectionMap(sPhoneProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + android.provider.Contacts.Phones.PERSON_ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;

            case PEOPLE_PHONES_ID:
                qb.setTables(LegacyTables.PHONES + " phones");
                qb.setProjectionMap(sPhoneProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + android.provider.Contacts.Phones.PERSON_ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                qb.appendWhere(" AND " + android.provider.Contacts.Phones._ID + "=");
                qb.appendWhere(uri.getPathSegments().get(3));
                break;

            case EXTENSIONS:
                qb.setTables(LegacyTables.EXTENSIONS + " extensions");
                qb.setProjectionMap(sExtensionProjectionMap);
                applyRawContactsAccount(qb);
                break;

            case EXTENSIONS_ID:
                qb.setTables(LegacyTables.EXTENSIONS + " extensions");
                qb.setProjectionMap(sExtensionProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + android.provider.Contacts.Extensions._ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;

            case PEOPLE_EXTENSIONS:
                qb.setTables(LegacyTables.EXTENSIONS + " extensions");
                qb.setProjectionMap(sExtensionProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + android.provider.Contacts.Extensions.PERSON_ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;

            case PEOPLE_EXTENSIONS_ID:
                qb.setTables(LegacyTables.EXTENSIONS + " extensions");
                qb.setProjectionMap(sExtensionProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + android.provider.Contacts.Extensions.PERSON_ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                qb.appendWhere(" AND " + android.provider.Contacts.Extensions._ID + "=");
                qb.appendWhere(uri.getPathSegments().get(3));
                break;

            case GROUPS:
                qb.setTables(LegacyTables.GROUPS + " groups");
                qb.setProjectionMap(sGroupProjectionMap);
                applyGroupAccount(qb);
                break;

            case GROUPS_ID:
                qb.setTables(LegacyTables.GROUPS + " groups");
                qb.setProjectionMap(sGroupProjectionMap);
                applyGroupAccount(qb);
                qb.appendWhere(" AND " + android.provider.Contacts.Groups._ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;

            case GROUPMEMBERSHIP:
                qb.setTables(LegacyTables.GROUP_MEMBERSHIP + " groupmembership");
                qb.setProjectionMap(sGroupMembershipProjectionMap);
                applyRawContactsAccount(qb);
                break;

            case GROUPMEMBERSHIP_ID:
                qb.setTables(LegacyTables.GROUP_MEMBERSHIP + " groupmembership");
                qb.setProjectionMap(sGroupMembershipProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + android.provider.Contacts.GroupMembership._ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;

            case PEOPLE_GROUPMEMBERSHIP:
                qb.setTables(LegacyTables.GROUP_MEMBERSHIP + " groupmembership");
                qb.setProjectionMap(sGroupMembershipProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + android.provider.Contacts.GroupMembership.PERSON_ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;

            case PEOPLE_GROUPMEMBERSHIP_ID:
                qb.setTables(LegacyTables.GROUP_MEMBERSHIP + " groupmembership");
                qb.setProjectionMap(sGroupMembershipProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + android.provider.Contacts.GroupMembership.PERSON_ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                qb.appendWhere(" AND " + android.provider.Contacts.GroupMembership._ID + "=");
                qb.appendWhere(uri.getPathSegments().get(3));
                break;

            case PEOPLE_PHOTO:
                qb.setTables(LegacyTables.PHOTOS + " photos");
                qb.setProjectionMap(sPhotoProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + android.provider.Contacts.Photos.PERSON_ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                limit = "1";
                break;

            case PHOTOS:
                qb.setTables(LegacyTables.PHOTOS + " photos");
                qb.setProjectionMap(sPhotoProjectionMap);
                applyRawContactsAccount(qb);
                break;

            case PHOTOS_ID:
                qb.setTables(LegacyTables.PHOTOS + " photos");
                qb.setProjectionMap(sPhotoProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND " + android.provider.Contacts.Photos._ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;

            case SEARCH_SUGGESTIONS:
                return mGlobalSearchSupport.handleSearchSuggestionsQuery(
                        db, uri, projection, limit, null);

            case SEARCH_SHORTCUT: {
                String lookupKey = uri.getLastPathSegment();
                String filter = ContactsProvider2.getQueryParameter(uri, "filter");
                return mGlobalSearchSupport.handleSearchShortcutRefresh(
                        db, projection, lookupKey, filter, null);
            }

            case LIVE_FOLDERS_PEOPLE:
                return mContactsProvider.query(LIVE_FOLDERS_CONTACTS_URI,
                        projection, selection, selectionArgs, sortOrder);

            case LIVE_FOLDERS_PEOPLE_WITH_PHONES:
                return mContactsProvider.query(LIVE_FOLDERS_CONTACTS_WITH_PHONES_URI,
                        projection, selection, selectionArgs, sortOrder);

            case LIVE_FOLDERS_PEOPLE_FAVORITES:
                return mContactsProvider.query(LIVE_FOLDERS_CONTACTS_FAVORITES_URI,
                        projection, selection, selectionArgs, sortOrder);

            case LIVE_FOLDERS_PEOPLE_GROUP_NAME:
                return mContactsProvider.query(Uri.withAppendedPath(LIVE_FOLDERS_CONTACTS_URI,
                        Uri.encode(uri.getLastPathSegment())),
                        projection, selection, selectionArgs, sortOrder);

            case DELETED_PEOPLE:
            case DELETED_GROUPS:
                throw new UnsupportedOperationException(mDbHelper.exceptionMessage(uri));

            case SETTINGS:
                copySettingsToLegacySettings();
                qb.setTables(LegacyTables.SETTINGS);
                break;

            default:
                throw new IllegalArgumentException(mDbHelper.exceptionMessage(uri));
        }

        // Perform the query and set the notification uri
        final Cursor c = qb.query(db, projection, selection, selectionArgs,
                groupBy, null, sortOrder, limit);
        if (c != null) {
            c.setNotificationUri(mContext.getContentResolver(),
                    android.provider.Contacts.CONTENT_URI);
        }
        return c;
    }

    private void applyRawContactsAccount(SQLiteQueryBuilder qb) {
        StringBuilder sb = new StringBuilder();
        appendRawContactsAccount(sb);
        qb.appendWhere(sb.toString());
    }

    private void appendRawContactsAccount(StringBuilder sb) {
        if (mAccount != null) {
            sb.append(RawContacts.ACCOUNT_NAME + "=");
            DatabaseUtils.appendEscapedSQLString(sb, mAccount.name);
            sb.append(" AND " + RawContacts.ACCOUNT_TYPE + "=");
            DatabaseUtils.appendEscapedSQLString(sb, mAccount.type);
        } else {
            sb.append(RawContacts.ACCOUNT_NAME + " IS NULL" +
                    " AND " + RawContacts.ACCOUNT_TYPE + " IS NULL");
        }
    }

    private void applyGroupAccount(SQLiteQueryBuilder qb) {
        StringBuilder sb = new StringBuilder();
        appendGroupAccount(sb);
        qb.appendWhere(sb.toString());
    }

    private void appendGroupAccount(StringBuilder sb) {
        if (mAccount != null) {
            sb.append(Groups.ACCOUNT_NAME + "=");
            DatabaseUtils.appendEscapedSQLString(sb, mAccount.name);
            sb.append(" AND " + Groups.ACCOUNT_TYPE + "=");
            DatabaseUtils.appendEscapedSQLString(sb, mAccount.type);
        } else {
            sb.append(Groups.ACCOUNT_NAME + " IS NULL" +
                    " AND " + Groups.ACCOUNT_TYPE + " IS NULL");
        }
    }

    /**
     * Build a WHERE clause that restricts the query to match people that are a member of
     * a group with a particular name. The projection map of the query must include
     * {@link People#_ID}.
     *
     * @param groupName The name of the group
     * @return The where clause.
     */
    private String buildGroupNameMatchWhereClause(String groupName) {
        return "people._id IN "
                + "(SELECT " + DataColumns.CONCRETE_RAW_CONTACT_ID
                + " FROM " + Tables.DATA_JOIN_MIMETYPES
                + " WHERE " + Data.MIMETYPE + "='" + GroupMembership.CONTENT_ITEM_TYPE
                        + "' AND " + GroupMembership.GROUP_ROW_ID + "="
                                + "(SELECT " + Tables.GROUPS + "." + Groups._ID
                                + " FROM " + Tables.GROUPS
                                + " WHERE " + Groups.TITLE + "="
                                        + DatabaseUtils.sqlEscapeString(groupName) + "))";
    }

    /**
     * Build a WHERE clause that restricts the query to match people that are a member of
     * a group with a particular system id. The projection map of the query must include
     * {@link People#_ID}.
     *
     * @param groupName The name of the group
     * @return The where clause.
     */
    private String buildGroupSystemIdMatchWhereClause(String systemId) {
        return "people._id IN "
                + "(SELECT " + DataColumns.CONCRETE_RAW_CONTACT_ID
                + " FROM " + Tables.DATA_JOIN_MIMETYPES
                + " WHERE " + Data.MIMETYPE + "='" + GroupMembership.CONTENT_ITEM_TYPE
                        + "' AND " + GroupMembership.GROUP_ROW_ID + "="
                                + "(SELECT " + Tables.GROUPS + "." + Groups._ID
                                + " FROM " + Tables.GROUPS
                                + " WHERE " + Groups.SYSTEM_ID + "="
                                        + DatabaseUtils.sqlEscapeString(systemId) + "))";
    }

    private String getRawContactsByFilterAsNestedQuery(String filterParam) {
        StringBuilder sb = new StringBuilder();
        String normalizedName = NameNormalizer.normalize(filterParam);
        if (TextUtils.isEmpty(normalizedName)) {
            // Effectively an empty IN clause - SQL syntax does not allow an actual empty list here
            sb.append("(0)");
        } else {
            sb.append("(" +
                    "SELECT " + NameLookupColumns.RAW_CONTACT_ID +
                    " FROM " + Tables.NAME_LOOKUP +
                    " WHERE " + NameLookupColumns.NORMALIZED_NAME +
                    " GLOB '");
            // Should not use a "?" argument placeholder here, because
            // that would prevent the SQL optimizer from using the index on NORMALIZED_NAME.
            sb.append(normalizedName);
            sb.append("*' AND " + NameLookupColumns.NAME_TYPE + " IN ("
                    + NameLookupType.NAME_COLLATION_KEY + ","
                    + NameLookupType.NICKNAME);
            if (true) {
                sb.append("," + NameLookupType.EMAIL_BASED_NICKNAME);
            }
            sb.append("))");
        }
        return sb.toString();
    }

    /**
     * Called when a change has been made.
     *
     * @param uri the uri that the change was made to
     */
    private void onChange(Uri uri) {
        mContext.getContentResolver().notifyChange(android.provider.Contacts.CONTENT_URI, null);
    }

    public String getType(Uri uri) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case EXTENSIONS:
            case PEOPLE_EXTENSIONS:
                return Extensions.CONTENT_TYPE;
            case EXTENSIONS_ID:
            case PEOPLE_EXTENSIONS_ID:
                return Extensions.CONTENT_ITEM_TYPE;
            case PEOPLE:
                return "vnd.android.cursor.dir/person";
            case PEOPLE_ID:
                return "vnd.android.cursor.item/person";
            case PEOPLE_PHONES:
                return "vnd.android.cursor.dir/phone";
            case PEOPLE_PHONES_ID:
                return "vnd.android.cursor.item/phone";
            case PEOPLE_CONTACTMETHODS:
                return "vnd.android.cursor.dir/contact-methods";
            case PEOPLE_CONTACTMETHODS_ID:
                return getContactMethodType(uri);
            case PHONES:
                return "vnd.android.cursor.dir/phone";
            case PHONES_ID:
                return "vnd.android.cursor.item/phone";
            case PHONES_FILTER:
                return "vnd.android.cursor.dir/phone";
            case PHOTOS_ID:
                return "vnd.android.cursor.item/photo";
            case PHOTOS:
                return "vnd.android.cursor.dir/photo";
            case PEOPLE_PHOTO:
                return "vnd.android.cursor.item/photo";
            case CONTACTMETHODS:
                return "vnd.android.cursor.dir/contact-methods";
            case CONTACTMETHODS_ID:
                return getContactMethodType(uri);
            case ORGANIZATIONS:
                return "vnd.android.cursor.dir/organizations";
            case ORGANIZATIONS_ID:
                return "vnd.android.cursor.item/organization";
            case SEARCH_SUGGESTIONS:
                return SearchManager.SUGGEST_MIME_TYPE;
            case SEARCH_SHORTCUT:
                return SearchManager.SHORTCUT_MIME_TYPE;
            default:
                throw new IllegalArgumentException(mDbHelper.exceptionMessage(uri));
        }
    }

    private String getContactMethodType(Uri url) {
        String mime = null;

        Cursor c = query(url, new String[] {ContactMethods.KIND}, null, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    int kind = c.getInt(0);
                    switch (kind) {
                    case android.provider.Contacts.KIND_EMAIL:
                        mime = "vnd.android.cursor.item/email";
                        break;

                    case android.provider.Contacts.KIND_IM:
                        mime = "vnd.android.cursor.item/jabber-im";
                        break;

                    case android.provider.Contacts.KIND_POSTAL:
                        mime = "vnd.android.cursor.item/postal-address";
                        break;
                    }
                }
            } finally {
                c.close();
            }
        }
        return mime;
    }
}
