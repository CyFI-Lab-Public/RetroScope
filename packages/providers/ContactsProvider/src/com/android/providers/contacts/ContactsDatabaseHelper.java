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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.location.CountryDetector;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.Photo;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.DisplayNameSources;
import android.provider.ContactsContract.DisplayPhoto;
import android.provider.ContactsContract.FullNameStyle;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.PhoneticNameStyle;
import android.provider.ContactsContract.PhotoFiles;
import android.provider.ContactsContract.PinnedPositions;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.provider.ContactsContract.StatusUpdates;
import android.provider.ContactsContract.StreamItemPhotos;
import android.provider.ContactsContract.StreamItems;
import android.provider.VoicemailContract;
import android.provider.VoicemailContract.Voicemails;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;

import com.android.common.content.SyncStateContentProviderHelper;
import com.android.providers.contacts.aggregation.util.CommonNicknameCache;
import com.android.providers.contacts.database.ContactsTableUtil;
import com.android.providers.contacts.database.DeletedContactsTableUtil;
import com.android.providers.contacts.database.MoreDatabaseUtils;
import com.android.providers.contacts.util.NeededForTesting;
import com.google.android.collect.Sets;

import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import libcore.icu.ICU;

/**
 * Database helper for contacts. Designed as a singleton to make sure that all
 * {@link android.content.ContentProvider} users get the same reference.
 * Provides handy methods for maintaining package and mime-type lookup tables.
 */
public class ContactsDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "ContactsDatabaseHelper";

    /**
     * Contacts DB version ranges:
     * <pre>
     *   0-98    Cupcake/Donut
     *   100-199 Eclair
     *   200-299 Eclair-MR1
     *   300-349 Froyo
     *   350-399 Gingerbread
     *   400-499 Honeycomb
     *   500-549 Honeycomb-MR1
     *   550-599 Honeycomb-MR2
     *   600-699 Ice Cream Sandwich
     *   700-799 Jelly Bean
     *   800-899 Kitkat
     * </pre>
     */
    static final int DATABASE_VERSION = 803;

    private static final String DATABASE_NAME = "contacts2.db";
    private static final String DATABASE_PRESENCE = "presence_db";

    public interface Tables {
        public static final String CONTACTS = "contacts";
        public static final String DELETED_CONTACTS = "deleted_contacts";
        public static final String RAW_CONTACTS = "raw_contacts";
        public static final String STREAM_ITEMS = "stream_items";
        public static final String STREAM_ITEM_PHOTOS = "stream_item_photos";
        public static final String PHOTO_FILES = "photo_files";
        public static final String PACKAGES = "packages";
        public static final String MIMETYPES = "mimetypes";
        public static final String PHONE_LOOKUP = "phone_lookup";
        public static final String NAME_LOOKUP = "name_lookup";
        public static final String AGGREGATION_EXCEPTIONS = "agg_exceptions";
        public static final String SETTINGS = "settings";
        public static final String DATA = "data";
        public static final String GROUPS = "groups";
        public static final String PRESENCE = "presence";
        public static final String AGGREGATED_PRESENCE = "agg_presence";
        public static final String NICKNAME_LOOKUP = "nickname_lookup";
        public static final String CALLS = "calls";
        public static final String STATUS_UPDATES = "status_updates";
        public static final String PROPERTIES = "properties";
        public static final String ACCOUNTS = "accounts";
        public static final String VISIBLE_CONTACTS = "visible_contacts";
        public static final String DIRECTORIES = "directories";
        public static final String DEFAULT_DIRECTORY = "default_directory";
        public static final String SEARCH_INDEX = "search_index";
        public static final String VOICEMAIL_STATUS = "voicemail_status";

        // This list of tables contains auto-incremented sequences.
        public static final String[] SEQUENCE_TABLES = new String[] {
                CONTACTS,
                RAW_CONTACTS,
                STREAM_ITEMS,
                STREAM_ITEM_PHOTOS,
                PHOTO_FILES,
                DATA,
                GROUPS,
                CALLS,
                DIRECTORIES
        };

        /**
         * For {@link ContactsContract.DataUsageFeedback}. The table structure itself
         * is not exposed outside.
         */
        public static final String DATA_USAGE_STAT = "data_usage_stat";

        public static final String DATA_JOIN_MIMETYPES = "data "
                + "JOIN mimetypes ON (data.mimetype_id = mimetypes._id)";

        public static final String DATA_JOIN_RAW_CONTACTS = "data "
                + "JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id)";

        // NOTE: If you want to refer to account name/type/data_set, AccountsColumns.CONCRETE_XXX
        // MUST be used, as upgraded raw_contacts may have the account info columns too.
        public static final String DATA_JOIN_MIMETYPE_RAW_CONTACTS = "data "
                + "JOIN mimetypes ON (data.mimetype_id = mimetypes._id) "
                + "JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id)"
                + " JOIN " + Tables.ACCOUNTS + " ON ("
                    + RawContactsColumns.CONCRETE_ACCOUNT_ID + "=" + AccountsColumns.CONCRETE_ID
                    + ")";

        // NOTE: This requires late binding of GroupMembership MIME-type
        // TODO Consolidate settings and accounts
        public static final String RAW_CONTACTS_JOIN_SETTINGS_DATA_GROUPS = Tables.RAW_CONTACTS
                + " JOIN " + Tables.ACCOUNTS + " ON ("
                +   RawContactsColumns.CONCRETE_ACCOUNT_ID + "=" + AccountsColumns.CONCRETE_ID
                    + ")"
                + "LEFT OUTER JOIN " + Tables.SETTINGS + " ON ("
                    + AccountsColumns.CONCRETE_ACCOUNT_NAME + "="
                        + SettingsColumns.CONCRETE_ACCOUNT_NAME + " AND "
                    + AccountsColumns.CONCRETE_ACCOUNT_TYPE + "="
                        + SettingsColumns.CONCRETE_ACCOUNT_TYPE + " AND "
                    + "((" + AccountsColumns.CONCRETE_DATA_SET + " IS NULL AND "
                            + SettingsColumns.CONCRETE_DATA_SET + " IS NULL) OR ("
                        + AccountsColumns.CONCRETE_DATA_SET + "="
                            + SettingsColumns.CONCRETE_DATA_SET + "))) "
                + "LEFT OUTER JOIN data ON (data.mimetype_id=? AND "
                    + "data.raw_contact_id = raw_contacts._id) "
                + "LEFT OUTER JOIN groups ON (groups._id = data." + GroupMembership.GROUP_ROW_ID
                + ")";

        // NOTE: This requires late binding of GroupMembership MIME-type
        // TODO Add missing DATA_SET join -- or just consolidate settings and accounts
        public static final String SETTINGS_JOIN_RAW_CONTACTS_DATA_MIMETYPES_CONTACTS = "settings "
                + "LEFT OUTER JOIN raw_contacts ON ("
                    + RawContactsColumns.CONCRETE_ACCOUNT_ID + "=(SELECT "
                        + AccountsColumns.CONCRETE_ID
                        + " FROM " + Tables.ACCOUNTS
                        + " WHERE "
                            + "(" + AccountsColumns.CONCRETE_ACCOUNT_NAME
                                + "=" + SettingsColumns.CONCRETE_ACCOUNT_NAME + ") AND "
                            + "(" + AccountsColumns.CONCRETE_ACCOUNT_TYPE
                                + "=" + SettingsColumns.CONCRETE_ACCOUNT_TYPE + ")))"
                + "LEFT OUTER JOIN data ON (data.mimetype_id=? AND "
                    + "data.raw_contact_id = raw_contacts._id) "
                + "LEFT OUTER JOIN contacts ON (raw_contacts.contact_id = contacts._id)";

        public static final String CONTACTS_JOIN_RAW_CONTACTS_DATA_FILTERED_BY_GROUPMEMBERSHIP =
                Tables.CONTACTS
                    + " INNER JOIN " + Tables.RAW_CONTACTS
                        + " ON (" + RawContactsColumns.CONCRETE_CONTACT_ID + "="
                            + ContactsColumns.CONCRETE_ID
                        + ")"
                    + " INNER JOIN " + Tables.DATA
                        + " ON (" + DataColumns.CONCRETE_DATA1 + "=" + GroupsColumns.CONCRETE_ID
                        + " AND "
                        + DataColumns.CONCRETE_RAW_CONTACT_ID + "=" + RawContactsColumns.CONCRETE_ID
                        + " AND "
                        + DataColumns.CONCRETE_MIMETYPE_ID + "="
                            + "(SELECT " + MimetypesColumns._ID
                            + " FROM " + Tables.MIMETYPES
                            + " WHERE "
                            + MimetypesColumns.CONCRETE_MIMETYPE + "="
                                + "'" + GroupMembership.CONTENT_ITEM_TYPE + "'"
                            + ")"
                        + ")";

        // NOTE: If you want to refer to account name/type/data_set, AccountsColumns.CONCRETE_XXX
        // MUST be used, as upgraded raw_contacts may have the account info columns too.
        public static final String DATA_JOIN_PACKAGES_MIMETYPES_RAW_CONTACTS_GROUPS = "data "
                + "JOIN mimetypes ON (data.mimetype_id = mimetypes._id) "
                + "JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id) "
                + " JOIN " + Tables.ACCOUNTS + " ON ("
                    + RawContactsColumns.CONCRETE_ACCOUNT_ID + "=" + AccountsColumns.CONCRETE_ID
                    + ")"
                + "LEFT OUTER JOIN packages ON (data.package_id = packages._id) "
                + "LEFT OUTER JOIN groups "
                + "  ON (mimetypes.mimetype='" + GroupMembership.CONTENT_ITEM_TYPE + "' "
                + "      AND groups._id = data." + GroupMembership.GROUP_ROW_ID + ") ";

        public static final String ACTIVITIES_JOIN_MIMETYPES = "activities "
                + "LEFT OUTER JOIN mimetypes ON (activities.mimetype_id = mimetypes._id)";

        public static final String ACTIVITIES_JOIN_PACKAGES_MIMETYPES_RAW_CONTACTS_CONTACTS =
                "activities "
                + "LEFT OUTER JOIN packages ON (activities.package_id = packages._id) "
                + "LEFT OUTER JOIN mimetypes ON (activities.mimetype_id = mimetypes._id) "
                + "LEFT OUTER JOIN raw_contacts ON (activities.author_contact_id = " +
                        "raw_contacts._id) "
                + "LEFT OUTER JOIN contacts ON (raw_contacts.contact_id = contacts._id)";

        public static final String NAME_LOOKUP_JOIN_RAW_CONTACTS = "name_lookup "
                + "INNER JOIN view_raw_contacts ON (name_lookup.raw_contact_id = "
                + "view_raw_contacts._id)";

        public static final String RAW_CONTACTS_JOIN_ACCOUNTS = Tables.RAW_CONTACTS
                + " JOIN " + Tables.ACCOUNTS + " ON ("
                + AccountsColumns.CONCRETE_ID + "=" + RawContactsColumns.CONCRETE_ACCOUNT_ID
                + ")";
    }

    public interface Joins {
        /**
         * Join string intended to be used with the GROUPS table/view.  The main table must be named
         * as "groups".
         *
         * Adds the "group_member_count column" to the query, which will be null if a group has
         * no members.  Use ifnull(group_member_count, 0) if 0 is needed instead.
         */
        public static final String GROUP_MEMBER_COUNT =
                " LEFT OUTER JOIN (SELECT "
                        + "data.data1 AS member_count_group_id, "
                        + "COUNT(data.raw_contact_id) AS group_member_count "
                    + "FROM data "
                    + "WHERE "
                        + "data.mimetype_id = (SELECT _id FROM mimetypes WHERE "
                            + "mimetypes.mimetype = '" + GroupMembership.CONTENT_ITEM_TYPE + "')"
                    + "GROUP BY member_count_group_id) AS member_count_table" // End of inner query
                + " ON (groups._id = member_count_table.member_count_group_id)";
    }

    public interface Views {
        public static final String DATA = "view_data";
        public static final String RAW_CONTACTS = "view_raw_contacts";
        public static final String CONTACTS = "view_contacts";
        public static final String ENTITIES = "view_entities";
        public static final String RAW_ENTITIES = "view_raw_entities";
        public static final String GROUPS = "view_groups";
        public static final String DATA_USAGE_STAT = "view_data_usage_stat";
        public static final String STREAM_ITEMS = "view_stream_items";
    }

    public interface Clauses {
        final String HAVING_NO_GROUPS = "COUNT(" + DataColumns.CONCRETE_GROUP_ID + ") == 0";

        final String GROUP_BY_ACCOUNT_CONTACT_ID = SettingsColumns.CONCRETE_ACCOUNT_NAME + ","
                + SettingsColumns.CONCRETE_ACCOUNT_TYPE + "," + RawContacts.CONTACT_ID;

        String LOCAL_ACCOUNT_ID =
                "(SELECT " + AccountsColumns._ID +
                " FROM " + Tables.ACCOUNTS +
                " WHERE " +
                    AccountsColumns.ACCOUNT_NAME + " IS NULL AND " +
                    AccountsColumns.ACCOUNT_TYPE + " IS NULL AND " +
                    AccountsColumns.DATA_SET + " IS NULL)";

        final String RAW_CONTACT_IS_LOCAL = RawContactsColumns.CONCRETE_ACCOUNT_ID
                + "=" + LOCAL_ACCOUNT_ID;

        final String ZERO_GROUP_MEMBERSHIPS = "COUNT(" + GroupsColumns.CONCRETE_ID + ")=0";

        final String OUTER_RAW_CONTACTS = "outer_raw_contacts";
        final String OUTER_RAW_CONTACTS_ID = OUTER_RAW_CONTACTS + "." + RawContacts._ID;

        final String CONTACT_IS_VISIBLE =
                "SELECT " +
                    "MAX((SELECT (CASE WHEN " +
                        "(CASE" +
                            " WHEN " + RAW_CONTACT_IS_LOCAL +
                            " THEN 1 " +
                            " WHEN " + ZERO_GROUP_MEMBERSHIPS +
                            " THEN " + Settings.UNGROUPED_VISIBLE +
                            " ELSE MAX(" + Groups.GROUP_VISIBLE + ")" +
                         "END)=1 THEN 1 ELSE 0 END)" +
                " FROM " + Tables.RAW_CONTACTS_JOIN_SETTINGS_DATA_GROUPS +
                " WHERE " + RawContactsColumns.CONCRETE_ID + "=" + OUTER_RAW_CONTACTS_ID + "))" +
                " FROM " + Tables.RAW_CONTACTS + " AS " + OUTER_RAW_CONTACTS +
                " WHERE " + RawContacts.CONTACT_ID + "=" + ContactsColumns.CONCRETE_ID +
                " GROUP BY " + RawContacts.CONTACT_ID;

        final String GROUP_HAS_ACCOUNT_AND_SOURCE_ID = Groups.SOURCE_ID + "=? AND "
                + GroupsColumns.ACCOUNT_ID + "=?";

        public static final String CONTACT_VISIBLE =
            "EXISTS (SELECT _id FROM " + Tables.VISIBLE_CONTACTS
                + " WHERE " + Tables.CONTACTS +"." + Contacts._ID
                        + "=" + Tables.VISIBLE_CONTACTS +"." + Contacts._ID + ")";
    }

    public interface ContactsColumns {
        public static final String LAST_STATUS_UPDATE_ID = "status_update_id";

        public static final String CONCRETE_ID = Tables.CONTACTS + "." + BaseColumns._ID;

        public static final String CONCRETE_PHOTO_FILE_ID = Tables.CONTACTS + "."
                + Contacts.PHOTO_FILE_ID;
        public static final String CONCRETE_TIMES_CONTACTED = Tables.CONTACTS + "."
                + Contacts.TIMES_CONTACTED;
        public static final String CONCRETE_LAST_TIME_CONTACTED = Tables.CONTACTS + "."
                + Contacts.LAST_TIME_CONTACTED;
        public static final String CONCRETE_STARRED = Tables.CONTACTS + "." + Contacts.STARRED;
        public static final String CONCRETE_PINNED = Tables.CONTACTS + "." + Contacts.PINNED;
        public static final String CONCRETE_CUSTOM_RINGTONE = Tables.CONTACTS + "."
                + Contacts.CUSTOM_RINGTONE;
        public static final String CONCRETE_SEND_TO_VOICEMAIL = Tables.CONTACTS + "."
                + Contacts.SEND_TO_VOICEMAIL;
        public static final String CONCRETE_LOOKUP_KEY = Tables.CONTACTS + "."
                + Contacts.LOOKUP_KEY;
        public static final String CONCRETE_CONTACT_LAST_UPDATED_TIMESTAMP = Tables.CONTACTS + "."
                + Contacts.CONTACT_LAST_UPDATED_TIMESTAMP;
        public static final String PHONEBOOK_LABEL_PRIMARY = "phonebook_label";
        public static final String PHONEBOOK_BUCKET_PRIMARY = "phonebook_bucket";
        public static final String PHONEBOOK_LABEL_ALTERNATIVE = "phonebook_label_alt";
        public static final String PHONEBOOK_BUCKET_ALTERNATIVE = "phonebook_bucket_alt";
    }

    public interface RawContactsColumns {
        public static final String CONCRETE_ID =
                Tables.RAW_CONTACTS + "." + BaseColumns._ID;

        public static final String ACCOUNT_ID = "account_id";
        public static final String CONCRETE_ACCOUNT_ID = Tables.RAW_CONTACTS + "." + ACCOUNT_ID;
        public static final String CONCRETE_SOURCE_ID =
                Tables.RAW_CONTACTS + "." + RawContacts.SOURCE_ID;
        public static final String CONCRETE_VERSION =
                Tables.RAW_CONTACTS + "." + RawContacts.VERSION;
        public static final String CONCRETE_DIRTY =
                Tables.RAW_CONTACTS + "." + RawContacts.DIRTY;
        public static final String CONCRETE_DELETED =
                Tables.RAW_CONTACTS + "." + RawContacts.DELETED;
        public static final String CONCRETE_SYNC1 =
                Tables.RAW_CONTACTS + "." + RawContacts.SYNC1;
        public static final String CONCRETE_SYNC2 =
                Tables.RAW_CONTACTS + "." + RawContacts.SYNC2;
        public static final String CONCRETE_SYNC3 =
                Tables.RAW_CONTACTS + "." + RawContacts.SYNC3;
        public static final String CONCRETE_SYNC4 =
                Tables.RAW_CONTACTS + "." + RawContacts.SYNC4;
        public static final String CONCRETE_CUSTOM_RINGTONE =
                Tables.RAW_CONTACTS + "." + RawContacts.CUSTOM_RINGTONE;
        public static final String CONCRETE_SEND_TO_VOICEMAIL =
                Tables.RAW_CONTACTS + "." + RawContacts.SEND_TO_VOICEMAIL;
        public static final String CONCRETE_LAST_TIME_CONTACTED =
                Tables.RAW_CONTACTS + "." + RawContacts.LAST_TIME_CONTACTED;
        public static final String CONCRETE_TIMES_CONTACTED =
                Tables.RAW_CONTACTS + "." + RawContacts.TIMES_CONTACTED;
        public static final String CONCRETE_STARRED =
                Tables.RAW_CONTACTS + "." + RawContacts.STARRED;
        public static final String CONCRETE_PINNED =
                Tables.RAW_CONTACTS + "." + RawContacts.PINNED;

        public static final String DISPLAY_NAME = RawContacts.DISPLAY_NAME_PRIMARY;
        public static final String DISPLAY_NAME_SOURCE = RawContacts.DISPLAY_NAME_SOURCE;
        public static final String AGGREGATION_NEEDED = "aggregation_needed";

        public static final String CONCRETE_DISPLAY_NAME =
                Tables.RAW_CONTACTS + "." + DISPLAY_NAME;
        public static final String CONCRETE_CONTACT_ID =
                Tables.RAW_CONTACTS + "." + RawContacts.CONTACT_ID;
        public static final String CONCRETE_NAME_VERIFIED =
            Tables.RAW_CONTACTS + "." + RawContacts.NAME_VERIFIED;
        public static final String PHONEBOOK_LABEL_PRIMARY =
            ContactsColumns.PHONEBOOK_LABEL_PRIMARY;
        public static final String PHONEBOOK_BUCKET_PRIMARY =
            ContactsColumns.PHONEBOOK_BUCKET_PRIMARY;
        public static final String PHONEBOOK_LABEL_ALTERNATIVE =
            ContactsColumns.PHONEBOOK_LABEL_ALTERNATIVE;
        public static final String PHONEBOOK_BUCKET_ALTERNATIVE =
            ContactsColumns.PHONEBOOK_BUCKET_ALTERNATIVE;
    }

    public interface ViewRawContactsColumns {
        String CONCRETE_ACCOUNT_NAME = Views.RAW_CONTACTS + "." + RawContacts.ACCOUNT_NAME;
        String CONCRETE_ACCOUNT_TYPE = Views.RAW_CONTACTS + "." + RawContacts.ACCOUNT_TYPE;
        String CONCRETE_DATA_SET = Views.RAW_CONTACTS + "." + RawContacts.DATA_SET;
    }

    public interface DataColumns {
        public static final String PACKAGE_ID = "package_id";
        public static final String MIMETYPE_ID = "mimetype_id";

        public static final String CONCRETE_ID = Tables.DATA + "." + BaseColumns._ID;
        public static final String CONCRETE_MIMETYPE_ID = Tables.DATA + "." + MIMETYPE_ID;
        public static final String CONCRETE_RAW_CONTACT_ID = Tables.DATA + "."
                + Data.RAW_CONTACT_ID;
        public static final String CONCRETE_GROUP_ID = Tables.DATA + "."
                + GroupMembership.GROUP_ROW_ID;

        public static final String CONCRETE_DATA1 = Tables.DATA + "." + Data.DATA1;
        public static final String CONCRETE_DATA2 = Tables.DATA + "." + Data.DATA2;
        public static final String CONCRETE_DATA3 = Tables.DATA + "." + Data.DATA3;
        public static final String CONCRETE_DATA4 = Tables.DATA + "." + Data.DATA4;
        public static final String CONCRETE_DATA5 = Tables.DATA + "." + Data.DATA5;
        public static final String CONCRETE_DATA6 = Tables.DATA + "." + Data.DATA6;
        public static final String CONCRETE_DATA7 = Tables.DATA + "." + Data.DATA7;
        public static final String CONCRETE_DATA8 = Tables.DATA + "." + Data.DATA8;
        public static final String CONCRETE_DATA9 = Tables.DATA + "." + Data.DATA9;
        public static final String CONCRETE_DATA10 = Tables.DATA + "." + Data.DATA10;
        public static final String CONCRETE_DATA11 = Tables.DATA + "." + Data.DATA11;
        public static final String CONCRETE_DATA12 = Tables.DATA + "." + Data.DATA12;
        public static final String CONCRETE_DATA13 = Tables.DATA + "." + Data.DATA13;
        public static final String CONCRETE_DATA14 = Tables.DATA + "." + Data.DATA14;
        public static final String CONCRETE_DATA15 = Tables.DATA + "." + Data.DATA15;
        public static final String CONCRETE_IS_PRIMARY = Tables.DATA + "." + Data.IS_PRIMARY;
        public static final String CONCRETE_PACKAGE_ID = Tables.DATA + "." + PACKAGE_ID;
    }

    // Used only for legacy API support
    public interface ExtensionsColumns {
        public static final String NAME = Data.DATA1;
        public static final String VALUE = Data.DATA2;
    }

    public interface GroupMembershipColumns {
        public static final String RAW_CONTACT_ID = Data.RAW_CONTACT_ID;
        public static final String GROUP_ROW_ID = GroupMembership.GROUP_ROW_ID;
    }

    public interface GroupsColumns {
        public static final String PACKAGE_ID = "package_id";
        public static final String CONCRETE_PACKAGE_ID = Tables.GROUPS + "." + PACKAGE_ID;

        public static final String CONCRETE_ID = Tables.GROUPS + "." + BaseColumns._ID;
        public static final String CONCRETE_SOURCE_ID = Tables.GROUPS + "." + Groups.SOURCE_ID;

        public static final String ACCOUNT_ID = "account_id";
        public static final String CONCRETE_ACCOUNT_ID = Tables.GROUPS + "." + ACCOUNT_ID;
    }

    public interface ViewGroupsColumns {
        String CONCRETE_ACCOUNT_NAME = Views.GROUPS + "." + Groups.ACCOUNT_NAME;
        String CONCRETE_ACCOUNT_TYPE = Views.GROUPS + "." + Groups.ACCOUNT_TYPE;
        String CONCRETE_DATA_SET = Views.GROUPS + "." + Groups.DATA_SET;
    }

    public interface ActivitiesColumns {
        public static final String PACKAGE_ID = "package_id";
        public static final String MIMETYPE_ID = "mimetype_id";
    }

    public interface PhoneLookupColumns {
        public static final String _ID = BaseColumns._ID;
        public static final String DATA_ID = "data_id";
        public static final String RAW_CONTACT_ID = "raw_contact_id";
        public static final String NORMALIZED_NUMBER = "normalized_number";
        public static final String MIN_MATCH = "min_match";
    }

    public interface NameLookupColumns {
        public static final String RAW_CONTACT_ID = "raw_contact_id";
        public static final String DATA_ID = "data_id";
        public static final String NORMALIZED_NAME = "normalized_name";
        public static final String NAME_TYPE = "name_type";
    }

    public final static class NameLookupType {
        public static final int NAME_EXACT = 0;
        public static final int NAME_VARIANT = 1;
        public static final int NAME_COLLATION_KEY = 2;
        public static final int NICKNAME = 3;
        public static final int EMAIL_BASED_NICKNAME = 4;

        // This is the highest name lookup type code plus one
        public static final int TYPE_COUNT = 5;

        public static boolean isBasedOnStructuredName(int nameLookupType) {
            return nameLookupType == NameLookupType.NAME_EXACT
                    || nameLookupType == NameLookupType.NAME_VARIANT
                    || nameLookupType == NameLookupType.NAME_COLLATION_KEY;
        }
    }

    public interface PackagesColumns {
        public static final String _ID = BaseColumns._ID;
        public static final String PACKAGE = "package";

        public static final String CONCRETE_ID = Tables.PACKAGES + "." + _ID;
    }

    public interface MimetypesColumns {
        public static final String _ID = BaseColumns._ID;
        public static final String MIMETYPE = "mimetype";

        public static final String CONCRETE_ID = Tables.MIMETYPES + "." + BaseColumns._ID;
        public static final String CONCRETE_MIMETYPE = Tables.MIMETYPES + "." + MIMETYPE;
    }

    public interface AggregationExceptionColumns {
        public static final String _ID = BaseColumns._ID;
    }

    public interface NicknameLookupColumns {
        public static final String NAME = "name";
        public static final String CLUSTER = "cluster";
    }

    public interface SettingsColumns {
        public static final String CONCRETE_ACCOUNT_NAME = Tables.SETTINGS + "."
                + Settings.ACCOUNT_NAME;
        public static final String CONCRETE_ACCOUNT_TYPE = Tables.SETTINGS + "."
                + Settings.ACCOUNT_TYPE;
        public static final String CONCRETE_DATA_SET = Tables.SETTINGS + "."
                + Settings.DATA_SET;
    }

    public interface PresenceColumns {
        String RAW_CONTACT_ID = "presence_raw_contact_id";
        String CONTACT_ID = "presence_contact_id";
    }

    public interface AggregatedPresenceColumns {
        String CONTACT_ID = "presence_contact_id";

        String CONCRETE_CONTACT_ID = Tables.AGGREGATED_PRESENCE + "." + CONTACT_ID;
    }

    public interface StatusUpdatesColumns {
        String DATA_ID = "status_update_data_id";

        String CONCRETE_DATA_ID = Tables.STATUS_UPDATES + "." + DATA_ID;

        String CONCRETE_PRESENCE = Tables.STATUS_UPDATES + "." + StatusUpdates.PRESENCE;
        String CONCRETE_STATUS = Tables.STATUS_UPDATES + "." + StatusUpdates.STATUS;
        String CONCRETE_STATUS_TIMESTAMP = Tables.STATUS_UPDATES + "."
                + StatusUpdates.STATUS_TIMESTAMP;
        String CONCRETE_STATUS_RES_PACKAGE = Tables.STATUS_UPDATES + "."
                + StatusUpdates.STATUS_RES_PACKAGE;
        String CONCRETE_STATUS_LABEL = Tables.STATUS_UPDATES + "." + StatusUpdates.STATUS_LABEL;
        String CONCRETE_STATUS_ICON = Tables.STATUS_UPDATES + "." + StatusUpdates.STATUS_ICON;
    }

    public interface ContactsStatusUpdatesColumns {
        String ALIAS = "contacts_" + Tables.STATUS_UPDATES;

        String CONCRETE_DATA_ID = ALIAS + "." + StatusUpdatesColumns.DATA_ID;

        String CONCRETE_PRESENCE = ALIAS + "." + StatusUpdates.PRESENCE;
        String CONCRETE_STATUS = ALIAS + "." + StatusUpdates.STATUS;
        String CONCRETE_STATUS_TIMESTAMP = ALIAS + "." + StatusUpdates.STATUS_TIMESTAMP;
        String CONCRETE_STATUS_RES_PACKAGE = ALIAS + "." + StatusUpdates.STATUS_RES_PACKAGE;
        String CONCRETE_STATUS_LABEL = ALIAS + "." + StatusUpdates.STATUS_LABEL;
        String CONCRETE_STATUS_ICON = ALIAS + "." + StatusUpdates.STATUS_ICON;
    }

    public interface StreamItemsColumns {
        final String CONCRETE_ID = Tables.STREAM_ITEMS + "." + BaseColumns._ID;
        final String CONCRETE_RAW_CONTACT_ID =
                Tables.STREAM_ITEMS + "." + StreamItems.RAW_CONTACT_ID;
        final String CONCRETE_PACKAGE = Tables.STREAM_ITEMS + "." + StreamItems.RES_PACKAGE;
        final String CONCRETE_ICON = Tables.STREAM_ITEMS + "." + StreamItems.RES_ICON;
        final String CONCRETE_LABEL = Tables.STREAM_ITEMS + "." + StreamItems.RES_LABEL;
        final String CONCRETE_TEXT = Tables.STREAM_ITEMS + "." + StreamItems.TEXT;
        final String CONCRETE_TIMESTAMP = Tables.STREAM_ITEMS + "." + StreamItems.TIMESTAMP;
        final String CONCRETE_COMMENTS = Tables.STREAM_ITEMS + "." + StreamItems.COMMENTS;
        final String CONCRETE_SYNC1 = Tables.STREAM_ITEMS + "." + StreamItems.SYNC1;
        final String CONCRETE_SYNC2 = Tables.STREAM_ITEMS + "." + StreamItems.SYNC2;
        final String CONCRETE_SYNC3 = Tables.STREAM_ITEMS + "." + StreamItems.SYNC3;
        final String CONCRETE_SYNC4 = Tables.STREAM_ITEMS + "." + StreamItems.SYNC4;
    }

    public interface StreamItemPhotosColumns {
        final String CONCRETE_ID = Tables.STREAM_ITEM_PHOTOS + "." + BaseColumns._ID;
        final String CONCRETE_STREAM_ITEM_ID = Tables.STREAM_ITEM_PHOTOS + "."
                + StreamItemPhotos.STREAM_ITEM_ID;
        final String CONCRETE_SORT_INDEX =
                Tables.STREAM_ITEM_PHOTOS + "." + StreamItemPhotos.SORT_INDEX;
        final String CONCRETE_PHOTO_FILE_ID = Tables.STREAM_ITEM_PHOTOS + "."
                + StreamItemPhotos.PHOTO_FILE_ID;
        final String CONCRETE_SYNC1 = Tables.STREAM_ITEM_PHOTOS + "." + StreamItemPhotos.SYNC1;
        final String CONCRETE_SYNC2 = Tables.STREAM_ITEM_PHOTOS + "." + StreamItemPhotos.SYNC2;
        final String CONCRETE_SYNC3 = Tables.STREAM_ITEM_PHOTOS + "." + StreamItemPhotos.SYNC3;
        final String CONCRETE_SYNC4 = Tables.STREAM_ITEM_PHOTOS + "." + StreamItemPhotos.SYNC4;
    }

    public interface PhotoFilesColumns {
        String CONCRETE_ID = Tables.PHOTO_FILES + "." + BaseColumns._ID;
        String CONCRETE_HEIGHT = Tables.PHOTO_FILES + "." + PhotoFiles.HEIGHT;
        String CONCRETE_WIDTH = Tables.PHOTO_FILES + "." + PhotoFiles.WIDTH;
        String CONCRETE_FILESIZE = Tables.PHOTO_FILES + "." + PhotoFiles.FILESIZE;
    }

    public interface PropertiesColumns {
        String PROPERTY_KEY = "property_key";
        String PROPERTY_VALUE = "property_value";
    }

    public interface AccountsColumns extends BaseColumns {
        String CONCRETE_ID = Tables.ACCOUNTS + "." + BaseColumns._ID;

        String ACCOUNT_NAME = RawContacts.ACCOUNT_NAME;
        String ACCOUNT_TYPE = RawContacts.ACCOUNT_TYPE;
        String DATA_SET = RawContacts.DATA_SET;

        String CONCRETE_ACCOUNT_NAME = Tables.ACCOUNTS + "." + ACCOUNT_NAME;
        String CONCRETE_ACCOUNT_TYPE = Tables.ACCOUNTS + "." + ACCOUNT_TYPE;
        String CONCRETE_DATA_SET = Tables.ACCOUNTS + "." + DATA_SET;
    }

    public static final class DirectoryColumns {
        public static final String TYPE_RESOURCE_NAME = "typeResourceName";
    }

    public static final class SearchIndexColumns {
        public static final String CONTACT_ID = "contact_id";
        public static final String CONTENT = "content";
        public static final String NAME = "name";
        public static final String TOKENS = "tokens";
    }

    /**
     * Private table for calculating per-contact-method ranking.
     */
    public static final class DataUsageStatColumns {
        /** type: INTEGER (long) */
        public static final String _ID = "stat_id";
        public static final String CONCRETE_ID = Tables.DATA_USAGE_STAT + "." + _ID;

        /** type: INTEGER (long) */
        public static final String DATA_ID = "data_id";
        public static final String CONCRETE_DATA_ID = Tables.DATA_USAGE_STAT + "." + DATA_ID;

        /** type: INTEGER (long) */
        public static final String LAST_TIME_USED = "last_time_used";
        public static final String CONCRETE_LAST_TIME_USED =
                Tables.DATA_USAGE_STAT + "." + LAST_TIME_USED;

        /** type: INTEGER */
        public static final String TIMES_USED = "times_used";
        public static final String CONCRETE_TIMES_USED =
                Tables.DATA_USAGE_STAT + "." + TIMES_USED;

        /** type: INTEGER */
        public static final String USAGE_TYPE_INT = "usage_type";
        public static final String CONCRETE_USAGE_TYPE =
                Tables.DATA_USAGE_STAT + "." + USAGE_TYPE_INT;

        /**
         * Integer values for USAGE_TYPE.
         *
         * @see ContactsContract.DataUsageFeedback#USAGE_TYPE
         */
        public static final int USAGE_TYPE_INT_CALL = 0;
        public static final int USAGE_TYPE_INT_LONG_TEXT = 1;
        public static final int USAGE_TYPE_INT_SHORT_TEXT = 2;
    }

    public interface Projections {
        String[] ID = new String[] {BaseColumns._ID};
        String[] LITERAL_ONE = new String[] {"1"};
    }

    /**
     * Property names for {@link ContactsDatabaseHelper#getProperty} and
     * {@link ContactsDatabaseHelper#setProperty}.
     */
    public interface DbProperties {
        String DIRECTORY_SCAN_COMPLETE = "directoryScanComplete";
        String AGGREGATION_ALGORITHM = "aggregation_v2";
        String KNOWN_ACCOUNTS = "known_accounts";
        String ICU_VERSION = "icu_version";
        String LOCALE = "locale";
        String DATABASE_TIME_CREATED = "database_time_created";
    }

    /** In-memory cache of previously found MIME-type mappings */
    private final HashMap<String, Long> mMimetypeCache = new HashMap<String, Long>();

    /** In-memory cache the packages table */
    private final HashMap<String, Long> mPackageCache = new HashMap<String, Long>();

    private long mMimeTypeIdEmail;
    private long mMimeTypeIdIm;
    private long mMimeTypeIdNickname;
    private long mMimeTypeIdOrganization;
    private long mMimeTypeIdPhone;
    private long mMimeTypeIdSip;
    private long mMimeTypeIdStructuredName;
    private long mMimeTypeIdStructuredPostal;

    /** Compiled statements for querying and inserting mappings */
    private SQLiteStatement mContactIdQuery;
    private SQLiteStatement mAggregationModeQuery;
    private SQLiteStatement mDataMimetypeQuery;

    /** Precompiled sql statement for setting a data record to the primary. */
    private SQLiteStatement mSetPrimaryStatement;
    /** Precompiled sql statement for setting a data record to the super primary. */
    private SQLiteStatement mSetSuperPrimaryStatement;
    /** Precompiled sql statement for clearing super primary of a single record. */
    private SQLiteStatement mClearSuperPrimaryStatement;
    /** Precompiled sql statement for updating a contact display name */
    private SQLiteStatement mRawContactDisplayNameUpdate;

    private SQLiteStatement mNameLookupInsert;
    private SQLiteStatement mNameLookupDelete;
    private SQLiteStatement mStatusUpdateAutoTimestamp;
    private SQLiteStatement mStatusUpdateInsert;
    private SQLiteStatement mStatusUpdateReplace;
    private SQLiteStatement mStatusAttributionUpdate;
    private SQLiteStatement mStatusUpdateDelete;
    private SQLiteStatement mResetNameVerifiedForOtherRawContacts;
    private SQLiteStatement mContactInDefaultDirectoryQuery;

    private final Context mContext;
    private final boolean mDatabaseOptimizationEnabled;
    private final SyncStateContentProviderHelper mSyncState;
    private final CountryMonitor mCountryMonitor;
    private StringBuilder mSb = new StringBuilder();

    private static ContactsDatabaseHelper sSingleton = null;

    private boolean mUseStrictPhoneNumberComparison;

    private String[] mSelectionArgs1 = new String[1];
    private NameSplitter.Name mName = new NameSplitter.Name();
    private CharArrayBuffer mCharArrayBuffer = new CharArrayBuffer(128);
    private NameSplitter mNameSplitter;

    public static synchronized ContactsDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new ContactsDatabaseHelper(context, DATABASE_NAME, true);
        }
        return sSingleton;
    }

    /**
     * Returns a new instance for unit tests.
     */
    @NeededForTesting
    static ContactsDatabaseHelper getNewInstanceForTest(Context context) {
        return new ContactsDatabaseHelper(context, null, false);
    }

    protected ContactsDatabaseHelper(
            Context context, String databaseName, boolean optimizationEnabled) {
        super(context, databaseName, null, DATABASE_VERSION);
        mDatabaseOptimizationEnabled = optimizationEnabled;
        Resources resources = context.getResources();

        mContext = context;
        mSyncState = new SyncStateContentProviderHelper();
        mCountryMonitor = new CountryMonitor(context);
        mUseStrictPhoneNumberComparison =
                resources.getBoolean(
                        com.android.internal.R.bool.config_use_strict_phone_number_comparation);
    }

    public SQLiteDatabase getDatabase(boolean writable) {
        return writable ? getWritableDatabase() : getReadableDatabase();
    }

    /**
     * Clear all the cached database information and re-initialize it.
     *
     * @param db target database
     */
    private void refreshDatabaseCaches(SQLiteDatabase db) {
        mStatusUpdateDelete = null;
        mStatusUpdateReplace = null;
        mStatusUpdateInsert = null;
        mStatusUpdateAutoTimestamp = null;
        mStatusAttributionUpdate = null;
        mResetNameVerifiedForOtherRawContacts = null;
        mRawContactDisplayNameUpdate = null;
        mSetPrimaryStatement = null;
        mClearSuperPrimaryStatement = null;
        mSetSuperPrimaryStatement = null;
        mNameLookupInsert = null;
        mNameLookupDelete = null;
        mDataMimetypeQuery = null;
        mContactIdQuery = null;
        mAggregationModeQuery = null;
        mContactInDefaultDirectoryQuery = null;

        initializeCache(db);
    }

    /**
     * (Re-)initialize the cached database information.
     *
     * @param db target database
     */
    private void initializeCache(SQLiteDatabase db) {
        mMimetypeCache.clear();
        mPackageCache.clear();

        // TODO: This could be optimized into one query instead of 7
        //        Also: We shouldn't have those fields in the first place. This should just be
        //        in the cache
        mMimeTypeIdEmail = lookupMimeTypeId(Email.CONTENT_ITEM_TYPE, db);
        mMimeTypeIdIm = lookupMimeTypeId(Im.CONTENT_ITEM_TYPE, db);
        mMimeTypeIdNickname = lookupMimeTypeId(Nickname.CONTENT_ITEM_TYPE, db);
        mMimeTypeIdOrganization = lookupMimeTypeId(Organization.CONTENT_ITEM_TYPE, db);
        mMimeTypeIdPhone = lookupMimeTypeId(Phone.CONTENT_ITEM_TYPE, db);
        mMimeTypeIdSip = lookupMimeTypeId(SipAddress.CONTENT_ITEM_TYPE, db);
        mMimeTypeIdStructuredName = lookupMimeTypeId(StructuredName.CONTENT_ITEM_TYPE, db);
        mMimeTypeIdStructuredPostal = lookupMimeTypeId(StructuredPostal.CONTENT_ITEM_TYPE, db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        refreshDatabaseCaches(db);

        mSyncState.onDatabaseOpened(db);

        db.execSQL("ATTACH DATABASE ':memory:' AS " + DATABASE_PRESENCE + ";");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_PRESENCE + "." + Tables.PRESENCE + " ("+
                StatusUpdates.DATA_ID + " INTEGER PRIMARY KEY REFERENCES data(_id)," +
                StatusUpdates.PROTOCOL + " INTEGER NOT NULL," +
                StatusUpdates.CUSTOM_PROTOCOL + " TEXT," +
                StatusUpdates.IM_HANDLE + " TEXT," +
                StatusUpdates.IM_ACCOUNT + " TEXT," +
                PresenceColumns.CONTACT_ID + " INTEGER REFERENCES contacts(_id)," +
                PresenceColumns.RAW_CONTACT_ID + " INTEGER REFERENCES raw_contacts(_id)," +
                StatusUpdates.PRESENCE + " INTEGER," +
                StatusUpdates.CHAT_CAPABILITY + " INTEGER NOT NULL DEFAULT 0," +
                "UNIQUE(" + StatusUpdates.PROTOCOL + ", " + StatusUpdates.CUSTOM_PROTOCOL
                    + ", " + StatusUpdates.IM_HANDLE + ", " + StatusUpdates.IM_ACCOUNT + ")" +
        ");");

        db.execSQL("CREATE INDEX IF NOT EXISTS " + DATABASE_PRESENCE + ".presenceIndex" + " ON "
                + Tables.PRESENCE + " (" + PresenceColumns.RAW_CONTACT_ID + ");");
        db.execSQL("CREATE INDEX IF NOT EXISTS " + DATABASE_PRESENCE + ".presenceIndex2" + " ON "
                + Tables.PRESENCE + " (" + PresenceColumns.CONTACT_ID + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS "
                + DATABASE_PRESENCE + "." + Tables.AGGREGATED_PRESENCE + " ("+
                AggregatedPresenceColumns.CONTACT_ID
                        + " INTEGER PRIMARY KEY REFERENCES contacts(_id)," +
                StatusUpdates.PRESENCE + " INTEGER," +
                StatusUpdates.CHAT_CAPABILITY + " INTEGER NOT NULL DEFAULT 0" +
        ");");


        db.execSQL("CREATE TRIGGER " + DATABASE_PRESENCE + "." + Tables.PRESENCE + "_deleted"
                + " BEFORE DELETE ON " + DATABASE_PRESENCE + "." + Tables.PRESENCE
                + " BEGIN "
                + "   DELETE FROM " + Tables.AGGREGATED_PRESENCE
                + "     WHERE " + AggregatedPresenceColumns.CONTACT_ID + " = " +
                        "(SELECT " + PresenceColumns.CONTACT_ID +
                        " FROM " + Tables.PRESENCE +
                        " WHERE " + PresenceColumns.RAW_CONTACT_ID
                                + "=OLD." + PresenceColumns.RAW_CONTACT_ID +
                        " AND NOT EXISTS" +
                                "(SELECT " + PresenceColumns.RAW_CONTACT_ID +
                                " FROM " + Tables.PRESENCE +
                                " WHERE " + PresenceColumns.CONTACT_ID
                                        + "=OLD." + PresenceColumns.CONTACT_ID +
                                " AND " + PresenceColumns.RAW_CONTACT_ID
                                        + "!=OLD." + PresenceColumns.RAW_CONTACT_ID + "));"
                + " END");

        final String replaceAggregatePresenceSql =
                "INSERT OR REPLACE INTO " + Tables.AGGREGATED_PRESENCE + "("
                        + AggregatedPresenceColumns.CONTACT_ID + ", "
                        + StatusUpdates.PRESENCE + ", "
                        + StatusUpdates.CHAT_CAPABILITY + ")"
                + " SELECT "
                        + PresenceColumns.CONTACT_ID + ","
                        + StatusUpdates.PRESENCE + ","
                        + StatusUpdates.CHAT_CAPABILITY
                + " FROM " + Tables.PRESENCE
                + " WHERE "
                    + " (ifnull(" + StatusUpdates.PRESENCE + ",0)  * 10 "
                            + "+ ifnull(" + StatusUpdates.CHAT_CAPABILITY + ", 0))"
                    + " = (SELECT "
                        + "MAX (ifnull(" + StatusUpdates.PRESENCE + ",0)  * 10 "
                                + "+ ifnull(" + StatusUpdates.CHAT_CAPABILITY + ", 0))"
                        + " FROM " + Tables.PRESENCE
                        + " WHERE " + PresenceColumns.CONTACT_ID
                            + "=NEW." + PresenceColumns.CONTACT_ID
                    + ")"
                + " AND " + PresenceColumns.CONTACT_ID + "=NEW." + PresenceColumns.CONTACT_ID + ";";

        db.execSQL("CREATE TRIGGER " + DATABASE_PRESENCE + "." + Tables.PRESENCE + "_inserted"
                + " AFTER INSERT ON " + DATABASE_PRESENCE + "." + Tables.PRESENCE
                + " BEGIN "
                + replaceAggregatePresenceSql
                + " END");

        db.execSQL("CREATE TRIGGER " + DATABASE_PRESENCE + "." + Tables.PRESENCE + "_updated"
                + " AFTER UPDATE ON " + DATABASE_PRESENCE + "." + Tables.PRESENCE
                + " BEGIN "
                + replaceAggregatePresenceSql
                + " END");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Bootstrapping database version: " + DATABASE_VERSION);

        mSyncState.createDatabase(db);

        // Create the properties table first so the create time is available as soon as possible.
        // The create time is needed by BOOT_COMPLETE to send broadcasts.
        db.execSQL("CREATE TABLE " + Tables.PROPERTIES + " (" +
                PropertiesColumns.PROPERTY_KEY + " TEXT PRIMARY KEY, " +
                PropertiesColumns.PROPERTY_VALUE + " TEXT " +
                ");");
        setProperty(db, DbProperties.DATABASE_TIME_CREATED, String.valueOf(
                System.currentTimeMillis()));

        db.execSQL("CREATE TABLE " + Tables.ACCOUNTS + " (" +
                AccountsColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                AccountsColumns.ACCOUNT_NAME + " TEXT, " +
                AccountsColumns.ACCOUNT_TYPE + " TEXT, " +
                AccountsColumns.DATA_SET + " TEXT" +
        ");");

        // One row per group of contacts corresponding to the same person
        db.execSQL("CREATE TABLE " + Tables.CONTACTS + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                Contacts.NAME_RAW_CONTACT_ID + " INTEGER REFERENCES raw_contacts(_id)," +
                Contacts.PHOTO_ID + " INTEGER REFERENCES data(_id)," +
                Contacts.PHOTO_FILE_ID + " INTEGER REFERENCES photo_files(_id)," +
                Contacts.CUSTOM_RINGTONE + " TEXT," +
                Contacts.SEND_TO_VOICEMAIL + " INTEGER NOT NULL DEFAULT 0," +
                Contacts.TIMES_CONTACTED + " INTEGER NOT NULL DEFAULT 0," +
                Contacts.LAST_TIME_CONTACTED + " INTEGER," +
                Contacts.STARRED + " INTEGER NOT NULL DEFAULT 0," +
                Contacts.PINNED + " INTEGER NOT NULL DEFAULT " + PinnedPositions.UNPINNED + "," +
                Contacts.HAS_PHONE_NUMBER + " INTEGER NOT NULL DEFAULT 0," +
                Contacts.LOOKUP_KEY + " TEXT," +
                ContactsColumns.LAST_STATUS_UPDATE_ID + " INTEGER REFERENCES data(_id)," +
                Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + " INTEGER" +
        ");");

        ContactsTableUtil.createIndexes(db);

        // deleted_contacts table
        DeletedContactsTableUtil.create(db);

        // Raw_contacts table
        db.execSQL("CREATE TABLE " + Tables.RAW_CONTACTS + " (" +
                RawContacts._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                RawContactsColumns.ACCOUNT_ID + " INTEGER REFERENCES " +
                    Tables.ACCOUNTS + "(" + AccountsColumns._ID + ")," +
                RawContacts.SOURCE_ID + " TEXT," +
                RawContacts.RAW_CONTACT_IS_READ_ONLY + " INTEGER NOT NULL DEFAULT 0," +
                RawContacts.VERSION + " INTEGER NOT NULL DEFAULT 1," +
                RawContacts.DIRTY + " INTEGER NOT NULL DEFAULT 0," +
                RawContacts.DELETED + " INTEGER NOT NULL DEFAULT 0," +
                RawContacts.CONTACT_ID + " INTEGER REFERENCES contacts(_id)," +
                RawContacts.AGGREGATION_MODE + " INTEGER NOT NULL DEFAULT " +
                        RawContacts.AGGREGATION_MODE_DEFAULT + "," +
                RawContactsColumns.AGGREGATION_NEEDED + " INTEGER NOT NULL DEFAULT 1," +
                RawContacts.CUSTOM_RINGTONE + " TEXT," +
                RawContacts.SEND_TO_VOICEMAIL + " INTEGER NOT NULL DEFAULT 0," +
                RawContacts.TIMES_CONTACTED + " INTEGER NOT NULL DEFAULT 0," +
                RawContacts.LAST_TIME_CONTACTED + " INTEGER," +
                RawContacts.STARRED + " INTEGER NOT NULL DEFAULT 0," +
                RawContacts.PINNED + " INTEGER NOT NULL DEFAULT "  + PinnedPositions.UNPINNED +
                    "," + RawContacts.DISPLAY_NAME_PRIMARY + " TEXT," +
                RawContacts.DISPLAY_NAME_ALTERNATIVE + " TEXT," +
                RawContacts.DISPLAY_NAME_SOURCE + " INTEGER NOT NULL DEFAULT " +
                        DisplayNameSources.UNDEFINED + "," +
                RawContacts.PHONETIC_NAME + " TEXT," +
                // TODO: PHONETIC_NAME_STYLE should be INTEGER. There is a
                // mismatch between how the column is created here (TEXT) and
                // how it is created in upgradeToVersion205 (INTEGER).
                RawContacts.PHONETIC_NAME_STYLE + " TEXT," +
                RawContacts.SORT_KEY_PRIMARY + " TEXT COLLATE " +
                        ContactsProvider2.PHONEBOOK_COLLATOR_NAME + "," +
                RawContactsColumns.PHONEBOOK_LABEL_PRIMARY + " TEXT," +
                RawContactsColumns.PHONEBOOK_BUCKET_PRIMARY + " INTEGER," +
                RawContacts.SORT_KEY_ALTERNATIVE + " TEXT COLLATE " +
                        ContactsProvider2.PHONEBOOK_COLLATOR_NAME + "," +
                RawContactsColumns.PHONEBOOK_LABEL_ALTERNATIVE + " TEXT," +
                RawContactsColumns.PHONEBOOK_BUCKET_ALTERNATIVE + " INTEGER," +
                RawContacts.NAME_VERIFIED + " INTEGER NOT NULL DEFAULT 0," +
                RawContacts.SYNC1 + " TEXT, " +
                RawContacts.SYNC2 + " TEXT, " +
                RawContacts.SYNC3 + " TEXT, " +
                RawContacts.SYNC4 + " TEXT " +
        ");");

        db.execSQL("CREATE INDEX raw_contacts_contact_id_index ON " + Tables.RAW_CONTACTS + " (" +
                RawContacts.CONTACT_ID +
        ");");

        db.execSQL("CREATE INDEX raw_contacts_source_id_account_id_index ON " +
                Tables.RAW_CONTACTS + " (" +
                RawContacts.SOURCE_ID + ", " +
                RawContactsColumns.ACCOUNT_ID +
        ");");

        db.execSQL("CREATE TABLE " + Tables.STREAM_ITEMS + " (" +
                StreamItems._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                StreamItems.RAW_CONTACT_ID + " INTEGER NOT NULL, " +
                StreamItems.RES_PACKAGE + " TEXT, " +
                StreamItems.RES_ICON + " TEXT, " +
                StreamItems.RES_LABEL + " TEXT, " +
                StreamItems.TEXT + " TEXT, " +
                StreamItems.TIMESTAMP + " INTEGER NOT NULL, " +
                StreamItems.COMMENTS + " TEXT, " +
                StreamItems.SYNC1 + " TEXT, " +
                StreamItems.SYNC2 + " TEXT, " +
                StreamItems.SYNC3 + " TEXT, " +
                StreamItems.SYNC4 + " TEXT, " +
                "FOREIGN KEY(" + StreamItems.RAW_CONTACT_ID + ") REFERENCES " +
                        Tables.RAW_CONTACTS + "(" + RawContacts._ID + "));");

        db.execSQL("CREATE TABLE " + Tables.STREAM_ITEM_PHOTOS + " (" +
                StreamItemPhotos._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                StreamItemPhotos.STREAM_ITEM_ID + " INTEGER NOT NULL, " +
                StreamItemPhotos.SORT_INDEX + " INTEGER, " +
                StreamItemPhotos.PHOTO_FILE_ID + " INTEGER NOT NULL, " +
                StreamItemPhotos.SYNC1 + " TEXT, " +
                StreamItemPhotos.SYNC2 + " TEXT, " +
                StreamItemPhotos.SYNC3 + " TEXT, " +
                StreamItemPhotos.SYNC4 + " TEXT, " +
                "FOREIGN KEY(" + StreamItemPhotos.STREAM_ITEM_ID + ") REFERENCES " +
                        Tables.STREAM_ITEMS + "(" + StreamItems._ID + "));");

        db.execSQL("CREATE TABLE " + Tables.PHOTO_FILES + " (" +
                PhotoFiles._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                PhotoFiles.HEIGHT + " INTEGER NOT NULL, " +
                PhotoFiles.WIDTH + " INTEGER NOT NULL, " +
                PhotoFiles.FILESIZE + " INTEGER NOT NULL);");

        // TODO readd the index and investigate a controlled use of it
//        db.execSQL("CREATE INDEX raw_contacts_agg_index ON " + Tables.RAW_CONTACTS + " (" +
//                RawContactsColumns.AGGREGATION_NEEDED +
//        ");");

        // Package name mapping table
        db.execSQL("CREATE TABLE " + Tables.PACKAGES + " (" +
                PackagesColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                PackagesColumns.PACKAGE + " TEXT NOT NULL" +
        ");");

        // Mimetype mapping table
        db.execSQL("CREATE TABLE " + Tables.MIMETYPES + " (" +
                MimetypesColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                MimetypesColumns.MIMETYPE + " TEXT NOT NULL" +
        ");");

        // Mimetype table requires an index on mime type
        db.execSQL("CREATE UNIQUE INDEX mime_type ON " + Tables.MIMETYPES + " (" +
                MimetypesColumns.MIMETYPE +
        ");");

        // Public generic data table
        db.execSQL("CREATE TABLE " + Tables.DATA + " (" +
                Data._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                DataColumns.PACKAGE_ID + " INTEGER REFERENCES package(_id)," +
                DataColumns.MIMETYPE_ID + " INTEGER REFERENCES mimetype(_id) NOT NULL," +
                Data.RAW_CONTACT_ID + " INTEGER REFERENCES raw_contacts(_id) NOT NULL," +
                Data.IS_READ_ONLY + " INTEGER NOT NULL DEFAULT 0," +
                Data.IS_PRIMARY + " INTEGER NOT NULL DEFAULT 0," +
                Data.IS_SUPER_PRIMARY + " INTEGER NOT NULL DEFAULT 0," +
                Data.DATA_VERSION + " INTEGER NOT NULL DEFAULT 0," +
                Data.DATA1 + " TEXT," +
                Data.DATA2 + " TEXT," +
                Data.DATA3 + " TEXT," +
                Data.DATA4 + " TEXT," +
                Data.DATA5 + " TEXT," +
                Data.DATA6 + " TEXT," +
                Data.DATA7 + " TEXT," +
                Data.DATA8 + " TEXT," +
                Data.DATA9 + " TEXT," +
                Data.DATA10 + " TEXT," +
                Data.DATA11 + " TEXT," +
                Data.DATA12 + " TEXT," +
                Data.DATA13 + " TEXT," +
                Data.DATA14 + " TEXT," +
                Data.DATA15 + " TEXT," +
                Data.SYNC1 + " TEXT, " +
                Data.SYNC2 + " TEXT, " +
                Data.SYNC3 + " TEXT, " +
                Data.SYNC4 + " TEXT " +
        ");");

        db.execSQL("CREATE INDEX data_raw_contact_id ON " + Tables.DATA + " (" +
                Data.RAW_CONTACT_ID +
        ");");

        /**
         * For email lookup and similar queries.
         */
        db.execSQL("CREATE INDEX data_mimetype_data1_index ON " + Tables.DATA + " (" +
                DataColumns.MIMETYPE_ID + "," +
                Data.DATA1 +
        ");");

        // Private phone numbers table used for lookup
        db.execSQL("CREATE TABLE " + Tables.PHONE_LOOKUP + " (" +
                PhoneLookupColumns.DATA_ID
                        + " INTEGER REFERENCES data(_id) NOT NULL," +
                PhoneLookupColumns.RAW_CONTACT_ID
                        + " INTEGER REFERENCES raw_contacts(_id) NOT NULL," +
                PhoneLookupColumns.NORMALIZED_NUMBER + " TEXT NOT NULL," +
                PhoneLookupColumns.MIN_MATCH + " TEXT NOT NULL" +
        ");");

        db.execSQL("CREATE INDEX phone_lookup_index ON " + Tables.PHONE_LOOKUP + " (" +
                PhoneLookupColumns.NORMALIZED_NUMBER + "," +
                PhoneLookupColumns.RAW_CONTACT_ID + "," +
                PhoneLookupColumns.DATA_ID +
        ");");

        db.execSQL("CREATE INDEX phone_lookup_min_match_index ON " + Tables.PHONE_LOOKUP + " (" +
                PhoneLookupColumns.MIN_MATCH + "," +
                PhoneLookupColumns.RAW_CONTACT_ID + "," +
                PhoneLookupColumns.DATA_ID +
        ");");

        db.execSQL("CREATE INDEX phone_lookup_data_id_min_match_index ON " + Tables.PHONE_LOOKUP +
                " (" + PhoneLookupColumns.DATA_ID + ", " + PhoneLookupColumns.MIN_MATCH + ");");

        // Private name/nickname table used for lookup
        db.execSQL("CREATE TABLE " + Tables.NAME_LOOKUP + " (" +
                NameLookupColumns.DATA_ID
                        + " INTEGER REFERENCES data(_id) NOT NULL," +
                NameLookupColumns.RAW_CONTACT_ID
                        + " INTEGER REFERENCES raw_contacts(_id) NOT NULL," +
                NameLookupColumns.NORMALIZED_NAME + " TEXT NOT NULL," +
                NameLookupColumns.NAME_TYPE + " INTEGER NOT NULL," +
                "PRIMARY KEY ("
                        + NameLookupColumns.DATA_ID + ", "
                        + NameLookupColumns.NORMALIZED_NAME + ", "
                        + NameLookupColumns.NAME_TYPE + ")" +
        ");");

        db.execSQL("CREATE INDEX name_lookup_raw_contact_id_index ON " + Tables.NAME_LOOKUP + " (" +
                NameLookupColumns.RAW_CONTACT_ID +
        ");");

        db.execSQL("CREATE TABLE " + Tables.NICKNAME_LOOKUP + " (" +
                NicknameLookupColumns.NAME + " TEXT," +
                NicknameLookupColumns.CLUSTER + " TEXT" +
        ");");

        db.execSQL("CREATE UNIQUE INDEX nickname_lookup_index ON " + Tables.NICKNAME_LOOKUP + " (" +
                NicknameLookupColumns.NAME + ", " +
                NicknameLookupColumns.CLUSTER +
        ");");

        // Groups table
        db.execSQL("CREATE TABLE " + Tables.GROUPS + " (" +
                Groups._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                GroupsColumns.PACKAGE_ID + " INTEGER REFERENCES package(_id)," +
                GroupsColumns.ACCOUNT_ID + " INTEGER REFERENCES " +
                    Tables.ACCOUNTS + "(" + AccountsColumns._ID + ")," +
                Groups.SOURCE_ID + " TEXT," +
                Groups.VERSION + " INTEGER NOT NULL DEFAULT 1," +
                Groups.DIRTY + " INTEGER NOT NULL DEFAULT 0," +
                Groups.TITLE + " TEXT," +
                Groups.TITLE_RES + " INTEGER," +
                Groups.NOTES + " TEXT," +
                Groups.SYSTEM_ID + " TEXT," +
                Groups.DELETED + " INTEGER NOT NULL DEFAULT 0," +
                Groups.GROUP_VISIBLE + " INTEGER NOT NULL DEFAULT 0," +
                Groups.SHOULD_SYNC + " INTEGER NOT NULL DEFAULT 1," +
                Groups.AUTO_ADD + " INTEGER NOT NULL DEFAULT 0," +
                Groups.FAVORITES + " INTEGER NOT NULL DEFAULT 0," +
                Groups.GROUP_IS_READ_ONLY + " INTEGER NOT NULL DEFAULT 0," +
                Groups.SYNC1 + " TEXT, " +
                Groups.SYNC2 + " TEXT, " +
                Groups.SYNC3 + " TEXT, " +
                Groups.SYNC4 + " TEXT " +
        ");");

        db.execSQL("CREATE INDEX groups_source_id_account_id_index ON " + Tables.GROUPS + " (" +
                Groups.SOURCE_ID + ", " +
                GroupsColumns.ACCOUNT_ID +
        ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.AGGREGATION_EXCEPTIONS + " (" +
                AggregationExceptionColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                AggregationExceptions.TYPE + " INTEGER NOT NULL, " +
                AggregationExceptions.RAW_CONTACT_ID1
                        + " INTEGER REFERENCES raw_contacts(_id), " +
                AggregationExceptions.RAW_CONTACT_ID2
                        + " INTEGER REFERENCES raw_contacts(_id)" +
        ");");

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS aggregation_exception_index1 ON " +
                Tables.AGGREGATION_EXCEPTIONS + " (" +
                AggregationExceptions.RAW_CONTACT_ID1 + ", " +
                AggregationExceptions.RAW_CONTACT_ID2 +
        ");");

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS aggregation_exception_index2 ON " +
                Tables.AGGREGATION_EXCEPTIONS + " (" +
                AggregationExceptions.RAW_CONTACT_ID2 + ", " +
                AggregationExceptions.RAW_CONTACT_ID1 +
        ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.SETTINGS + " (" +
                Settings.ACCOUNT_NAME + " STRING NOT NULL," +
                Settings.ACCOUNT_TYPE + " STRING NOT NULL," +
                Settings.DATA_SET + " STRING," +
                Settings.UNGROUPED_VISIBLE + " INTEGER NOT NULL DEFAULT 0," +
                Settings.SHOULD_SYNC + " INTEGER NOT NULL DEFAULT 1" +
        ");");

        db.execSQL("CREATE TABLE " + Tables.VISIBLE_CONTACTS + " (" +
                Contacts._ID + " INTEGER PRIMARY KEY" +
        ");");

        db.execSQL("CREATE TABLE " + Tables.DEFAULT_DIRECTORY + " (" +
                Contacts._ID + " INTEGER PRIMARY KEY" +
        ");");

        // The table for recent calls is here so we can do table joins
        // on people, phones, and calls all in one place.
        db.execSQL("CREATE TABLE " + Tables.CALLS + " (" +
                Calls._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                Calls.NUMBER + " TEXT," +
                Calls.NUMBER_PRESENTATION + " INTEGER NOT NULL DEFAULT " +
                        Calls.PRESENTATION_ALLOWED + "," +
                Calls.DATE + " INTEGER," +
                Calls.DURATION + " INTEGER," +
                Calls.TYPE + " INTEGER," +
                Calls.NEW + " INTEGER," +
                Calls.CACHED_NAME + " TEXT," +
                Calls.CACHED_NUMBER_TYPE + " INTEGER," +
                Calls.CACHED_NUMBER_LABEL + " TEXT," +
                Calls.COUNTRY_ISO + " TEXT," +
                Calls.VOICEMAIL_URI + " TEXT," +
                Calls.IS_READ + " INTEGER," +
                Calls.GEOCODED_LOCATION + " TEXT," +
                Calls.CACHED_LOOKUP_URI + " TEXT," +
                Calls.CACHED_MATCHED_NUMBER + " TEXT," +
                Calls.CACHED_NORMALIZED_NUMBER + " TEXT," +
                Calls.CACHED_PHOTO_ID + " INTEGER NOT NULL DEFAULT 0," +
                Calls.CACHED_FORMATTED_NUMBER + " TEXT," +
                Voicemails._DATA + " TEXT," +
                Voicemails.HAS_CONTENT + " INTEGER," +
                Voicemails.MIME_TYPE + " TEXT," +
                Voicemails.SOURCE_DATA + " TEXT," +
                Voicemails.SOURCE_PACKAGE + " TEXT," +
                Voicemails.STATE + " INTEGER" +
        ");");

        // Voicemail source status table.
        db.execSQL("CREATE TABLE " + Tables.VOICEMAIL_STATUS + " (" +
                VoicemailContract.Status._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                VoicemailContract.Status.SOURCE_PACKAGE + " TEXT UNIQUE NOT NULL," +
                VoicemailContract.Status.SETTINGS_URI + " TEXT," +
                VoicemailContract.Status.VOICEMAIL_ACCESS_URI + " TEXT," +
                VoicemailContract.Status.CONFIGURATION_STATE + " INTEGER," +
                VoicemailContract.Status.DATA_CHANNEL_STATE + " INTEGER," +
                VoicemailContract.Status.NOTIFICATION_CHANNEL_STATE + " INTEGER" +
        ");");

        db.execSQL("CREATE TABLE " + Tables.STATUS_UPDATES + " (" +
                StatusUpdatesColumns.DATA_ID + " INTEGER PRIMARY KEY REFERENCES data(_id)," +
                StatusUpdates.STATUS + " TEXT," +
                StatusUpdates.STATUS_TIMESTAMP + " INTEGER," +
                StatusUpdates.STATUS_RES_PACKAGE + " TEXT, " +
                StatusUpdates.STATUS_LABEL + " INTEGER, " +
                StatusUpdates.STATUS_ICON + " INTEGER" +
        ");");

        createDirectoriesTable(db);
        createSearchIndexTable(db, false /* we build stats table later */);

        db.execSQL("CREATE TABLE " + Tables.DATA_USAGE_STAT + "(" +
                DataUsageStatColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DataUsageStatColumns.DATA_ID + " INTEGER NOT NULL, " +
                DataUsageStatColumns.USAGE_TYPE_INT + " INTEGER NOT NULL DEFAULT 0, " +
                DataUsageStatColumns.TIMES_USED + " INTEGER NOT NULL DEFAULT 0, " +
                DataUsageStatColumns.LAST_TIME_USED + " INTERGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(" + DataUsageStatColumns.DATA_ID + ") REFERENCES "
                        + Tables.DATA + "(" + Data._ID + ")" +
        ");");
        db.execSQL("CREATE UNIQUE INDEX data_usage_stat_index ON " +
                Tables.DATA_USAGE_STAT + " (" +
                DataUsageStatColumns.DATA_ID + ", " +
                DataUsageStatColumns.USAGE_TYPE_INT +
        ");");

        // When adding new tables, be sure to also add size-estimates in updateSqliteStats
        createContactsViews(db);
        createGroupsView(db);
        createContactsTriggers(db);
        createContactsIndexes(db, false /* we build stats table later */);

        loadNicknameLookupTable(db);

        // Set sequence starts.
        initializeAutoIncrementSequences(db);

        // Add the legacy API support views, etc
        LegacyApiSupport.createDatabase(db);

        if (mDatabaseOptimizationEnabled) {
            // This will create a sqlite_stat1 table that is used for query optimization
            db.execSQL("ANALYZE;");

            updateSqliteStats(db);
        }

        ContentResolver.requestSync(null /* all accounts */,
                ContactsContract.AUTHORITY, new Bundle());

        // Only send broadcasts for regular contacts db.
        if (dbForProfile() == 0) {
            final Intent dbCreatedIntent = new Intent(
                    ContactsContract.Intents.CONTACTS_DATABASE_CREATED);
            dbCreatedIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
            mContext.sendBroadcast(dbCreatedIntent, android.Manifest.permission.READ_CONTACTS);
        }
    }

    protected void initializeAutoIncrementSequences(SQLiteDatabase db) {
        // Default implementation does nothing.
    }

    private void createDirectoriesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.DIRECTORIES + "(" +
                Directory._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                Directory.PACKAGE_NAME + " TEXT NOT NULL," +
                Directory.DIRECTORY_AUTHORITY + " TEXT NOT NULL," +
                Directory.TYPE_RESOURCE_ID + " INTEGER," +
                DirectoryColumns.TYPE_RESOURCE_NAME + " TEXT," +
                Directory.ACCOUNT_TYPE + " TEXT," +
                Directory.ACCOUNT_NAME + " TEXT," +
                Directory.DISPLAY_NAME + " TEXT, " +
                Directory.EXPORT_SUPPORT + " INTEGER NOT NULL" +
                        " DEFAULT " + Directory.EXPORT_SUPPORT_NONE + "," +
                Directory.SHORTCUT_SUPPORT + " INTEGER NOT NULL" +
                        " DEFAULT " + Directory.SHORTCUT_SUPPORT_NONE + "," +
                Directory.PHOTO_SUPPORT + " INTEGER NOT NULL" +
                        " DEFAULT " + Directory.PHOTO_SUPPORT_NONE +
        ");");

        // Trigger a full scan of directories in the system
        setProperty(db, DbProperties.DIRECTORY_SCAN_COMPLETE, "0");
    }

    public void createSearchIndexTable(SQLiteDatabase db, boolean rebuildSqliteStats) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.SEARCH_INDEX);
        db.execSQL("CREATE VIRTUAL TABLE " + Tables.SEARCH_INDEX
                + " USING FTS4 ("
                    + SearchIndexColumns.CONTACT_ID + " INTEGER REFERENCES contacts(_id) NOT NULL,"
                    + SearchIndexColumns.CONTENT + " TEXT, "
                    + SearchIndexColumns.NAME + " TEXT, "
                    + SearchIndexColumns.TOKENS + " TEXT"
                + ")");
        if (rebuildSqliteStats) {
            updateSqliteStats(db);
        }
    }

    private void createContactsTriggers(SQLiteDatabase db) {

        /*
         * Automatically delete Data rows when a raw contact is deleted.
         */
        db.execSQL("DROP TRIGGER IF EXISTS " + Tables.RAW_CONTACTS + "_deleted;");
        db.execSQL("CREATE TRIGGER " + Tables.RAW_CONTACTS + "_deleted "
                + "   BEFORE DELETE ON " + Tables.RAW_CONTACTS
                + " BEGIN "
                + "   DELETE FROM " + Tables.DATA
                + "     WHERE " + Data.RAW_CONTACT_ID
                                + "=OLD." + RawContacts._ID + ";"
                + "   DELETE FROM " + Tables.AGGREGATION_EXCEPTIONS
                + "     WHERE " + AggregationExceptions.RAW_CONTACT_ID1
                                + "=OLD." + RawContacts._ID
                + "        OR " + AggregationExceptions.RAW_CONTACT_ID2
                                + "=OLD." + RawContacts._ID + ";"
                + "   DELETE FROM " + Tables.VISIBLE_CONTACTS
                + "     WHERE " + Contacts._ID + "=OLD." + RawContacts.CONTACT_ID
                + "       AND (SELECT COUNT(*) FROM " + Tables.RAW_CONTACTS
                + "            WHERE " + RawContacts.CONTACT_ID + "=OLD." + RawContacts.CONTACT_ID
                + "           )=1;"
                + "   DELETE FROM " + Tables.DEFAULT_DIRECTORY
                + "     WHERE " + Contacts._ID + "=OLD." + RawContacts.CONTACT_ID
                + "       AND (SELECT COUNT(*) FROM " + Tables.RAW_CONTACTS
                + "            WHERE " + RawContacts.CONTACT_ID + "=OLD." + RawContacts.CONTACT_ID
                + "           )=1;"
                + "   DELETE FROM " + Tables.CONTACTS
                + "     WHERE " + Contacts._ID + "=OLD." + RawContacts.CONTACT_ID
                + "       AND (SELECT COUNT(*) FROM " + Tables.RAW_CONTACTS
                + "            WHERE " + RawContacts.CONTACT_ID + "=OLD." + RawContacts.CONTACT_ID
                + "           )=1;"
                + " END");


        db.execSQL("DROP TRIGGER IF EXISTS contacts_times_contacted;");
        db.execSQL("DROP TRIGGER IF EXISTS raw_contacts_times_contacted;");

        /*
         * Triggers that update {@link RawContacts#VERSION} when the contact is
         * marked for deletion or any time a data row is inserted, updated or
         * deleted.
         */
        db.execSQL("DROP TRIGGER IF EXISTS " + Tables.RAW_CONTACTS + "_marked_deleted;");
        db.execSQL("CREATE TRIGGER " + Tables.RAW_CONTACTS + "_marked_deleted "
                + "   AFTER UPDATE ON " + Tables.RAW_CONTACTS
                + " BEGIN "
                + "   UPDATE " + Tables.RAW_CONTACTS
                + "     SET "
                +         RawContacts.VERSION + "=OLD." + RawContacts.VERSION + "+1 "
                + "     WHERE " + RawContacts._ID + "=OLD." + RawContacts._ID
                + "       AND NEW." + RawContacts.DELETED + "!= OLD." + RawContacts.DELETED + ";"
                + " END");

        db.execSQL("DROP TRIGGER IF EXISTS " + Tables.DATA + "_updated;");
        db.execSQL("CREATE TRIGGER " + Tables.DATA + "_updated AFTER UPDATE ON " + Tables.DATA
                + " BEGIN "
                + "   UPDATE " + Tables.DATA
                + "     SET " + Data.DATA_VERSION + "=OLD." + Data.DATA_VERSION + "+1 "
                + "     WHERE " + Data._ID + "=OLD." + Data._ID + ";"
                + "   UPDATE " + Tables.RAW_CONTACTS
                + "     SET " + RawContacts.VERSION + "=" + RawContacts.VERSION + "+1 "
                + "     WHERE " + RawContacts._ID + "=OLD." + Data.RAW_CONTACT_ID + ";"
                + " END");

        db.execSQL("DROP TRIGGER IF EXISTS " + Tables.DATA + "_deleted;");
        db.execSQL("CREATE TRIGGER " + Tables.DATA + "_deleted BEFORE DELETE ON " + Tables.DATA
                + " BEGIN "
                + "   UPDATE " + Tables.RAW_CONTACTS
                + "     SET " + RawContacts.VERSION + "=" + RawContacts.VERSION + "+1 "
                + "     WHERE " + RawContacts._ID + "=OLD." + Data.RAW_CONTACT_ID + ";"
                + "   DELETE FROM " + Tables.PHONE_LOOKUP
                + "     WHERE " + PhoneLookupColumns.DATA_ID + "=OLD." + Data._ID + ";"
                + "   DELETE FROM " + Tables.STATUS_UPDATES
                + "     WHERE " + StatusUpdatesColumns.DATA_ID + "=OLD." + Data._ID + ";"
                + "   DELETE FROM " + Tables.NAME_LOOKUP
                + "     WHERE " + NameLookupColumns.DATA_ID + "=OLD." + Data._ID + ";"
                + " END");


        db.execSQL("DROP TRIGGER IF EXISTS " + Tables.GROUPS + "_updated1;");
        db.execSQL("CREATE TRIGGER " + Tables.GROUPS + "_updated1 "
                + "   AFTER UPDATE ON " + Tables.GROUPS
                + " BEGIN "
                + "   UPDATE " + Tables.GROUPS
                + "     SET "
                +         Groups.VERSION + "=OLD." + Groups.VERSION + "+1"
                + "     WHERE " + Groups._ID + "=OLD." + Groups._ID + ";"
                + " END");

        // Update DEFAULT_FILTER table per AUTO_ADD column update.
        // See also upgradeToVersion411().
        final String insertContactsWithoutAccount = (
                " INSERT OR IGNORE INTO " + Tables.DEFAULT_DIRECTORY +
                "     SELECT " + RawContacts.CONTACT_ID +
                "     FROM " + Tables.RAW_CONTACTS +
                "     WHERE " + RawContactsColumns.CONCRETE_ACCOUNT_ID +
                            "=" + Clauses.LOCAL_ACCOUNT_ID + ";");
        final String insertContactsWithAccountNoDefaultGroup = (
                " INSERT OR IGNORE INTO " + Tables.DEFAULT_DIRECTORY +
                "     SELECT " + RawContacts.CONTACT_ID +
                "         FROM " + Tables.RAW_CONTACTS +
                "     WHERE NOT EXISTS" +
                "         (SELECT " + Groups._ID +
                "             FROM " + Tables.GROUPS +
                "             WHERE " + RawContactsColumns.CONCRETE_ACCOUNT_ID + " = " +
                                    GroupsColumns.CONCRETE_ACCOUNT_ID +
                "             AND " + Groups.AUTO_ADD + " != 0" + ");");
        final String insertContactsWithAccountDefaultGroup = (
                " INSERT OR IGNORE INTO " + Tables.DEFAULT_DIRECTORY +
                "     SELECT " + RawContacts.CONTACT_ID +
                "         FROM " + Tables.RAW_CONTACTS +
                "     JOIN " + Tables.DATA +
                "           ON (" + RawContactsColumns.CONCRETE_ID + "=" +
                        Data.RAW_CONTACT_ID + ")" +
                "     WHERE " + DataColumns.MIMETYPE_ID + "=" +
                    "(SELECT " + MimetypesColumns._ID + " FROM " + Tables.MIMETYPES +
                        " WHERE " + MimetypesColumns.MIMETYPE +
                            "='" + GroupMembership.CONTENT_ITEM_TYPE + "')" +
                "     AND EXISTS" +
                "         (SELECT " + Groups._ID +
                "             FROM " + Tables.GROUPS +
                "                 WHERE " + RawContactsColumns.CONCRETE_ACCOUNT_ID + " = " +
                                        GroupsColumns.CONCRETE_ACCOUNT_ID +
                "                 AND " + Groups.AUTO_ADD + " != 0" + ");");

        db.execSQL("DROP TRIGGER IF EXISTS " + Tables.GROUPS + "_auto_add_updated1;");
        db.execSQL("CREATE TRIGGER " + Tables.GROUPS + "_auto_add_updated1 "
                + "   AFTER UPDATE OF " + Groups.AUTO_ADD + " ON " + Tables.GROUPS
                + " BEGIN "
                + "   DELETE FROM " + Tables.DEFAULT_DIRECTORY + ";"
                    + insertContactsWithoutAccount
                    + insertContactsWithAccountNoDefaultGroup
                    + insertContactsWithAccountDefaultGroup
                + " END");
    }

    private void createContactsIndexes(SQLiteDatabase db, boolean rebuildSqliteStats) {
        db.execSQL("DROP INDEX IF EXISTS name_lookup_index");
        db.execSQL("CREATE INDEX name_lookup_index ON " + Tables.NAME_LOOKUP + " (" +
                NameLookupColumns.NORMALIZED_NAME + "," +
                NameLookupColumns.NAME_TYPE + ", " +
                NameLookupColumns.RAW_CONTACT_ID + ", " +
                NameLookupColumns.DATA_ID +
        ");");

        db.execSQL("DROP INDEX IF EXISTS raw_contact_sort_key1_index");
        db.execSQL("CREATE INDEX raw_contact_sort_key1_index ON " + Tables.RAW_CONTACTS + " (" +
                RawContacts.SORT_KEY_PRIMARY +
        ");");

        db.execSQL("DROP INDEX IF EXISTS raw_contact_sort_key2_index");
        db.execSQL("CREATE INDEX raw_contact_sort_key2_index ON " + Tables.RAW_CONTACTS + " (" +
                RawContacts.SORT_KEY_ALTERNATIVE +
        ");");

        if (rebuildSqliteStats) {
            updateSqliteStats(db);
        }
    }

    private void createContactsViews(SQLiteDatabase db) {
        db.execSQL("DROP VIEW IF EXISTS " + Views.CONTACTS + ";");
        db.execSQL("DROP VIEW IF EXISTS " + Views.DATA + ";");
        db.execSQL("DROP VIEW IF EXISTS " + Views.RAW_CONTACTS + ";");
        db.execSQL("DROP VIEW IF EXISTS " + Views.RAW_ENTITIES + ";");
        db.execSQL("DROP VIEW IF EXISTS " + Views.ENTITIES + ";");
        db.execSQL("DROP VIEW IF EXISTS " + Views.DATA_USAGE_STAT + ";");
        db.execSQL("DROP VIEW IF EXISTS " + Views.STREAM_ITEMS + ";");

        String dataColumns =
                Data.IS_PRIMARY + ", "
                + Data.IS_SUPER_PRIMARY + ", "
                + Data.DATA_VERSION + ", "
                + DataColumns.CONCRETE_PACKAGE_ID + ","
                + PackagesColumns.PACKAGE + " AS " + Data.RES_PACKAGE + ","
                + DataColumns.CONCRETE_MIMETYPE_ID + ","
                + MimetypesColumns.MIMETYPE + " AS " + Data.MIMETYPE + ", "
                + Data.IS_READ_ONLY + ", "
                + Data.DATA1 + ", "
                + Data.DATA2 + ", "
                + Data.DATA3 + ", "
                + Data.DATA4 + ", "
                + Data.DATA5 + ", "
                + Data.DATA6 + ", "
                + Data.DATA7 + ", "
                + Data.DATA8 + ", "
                + Data.DATA9 + ", "
                + Data.DATA10 + ", "
                + Data.DATA11 + ", "
                + Data.DATA12 + ", "
                + Data.DATA13 + ", "
                + Data.DATA14 + ", "
                + Data.DATA15 + ", "
                + Data.SYNC1 + ", "
                + Data.SYNC2 + ", "
                + Data.SYNC3 + ", "
                + Data.SYNC4;

        String syncColumns =
                RawContactsColumns.CONCRETE_ACCOUNT_ID + ","
                + AccountsColumns.CONCRETE_ACCOUNT_NAME + " AS " + RawContacts.ACCOUNT_NAME + ","
                + AccountsColumns.CONCRETE_ACCOUNT_TYPE + " AS " + RawContacts.ACCOUNT_TYPE + ","
                + AccountsColumns.CONCRETE_DATA_SET + " AS " + RawContacts.DATA_SET + ","
                + "(CASE WHEN " + AccountsColumns.CONCRETE_DATA_SET + " IS NULL THEN "
                            + AccountsColumns.CONCRETE_ACCOUNT_TYPE
                        + " ELSE " + AccountsColumns.CONCRETE_ACCOUNT_TYPE + "||'/'||"
                            + AccountsColumns.CONCRETE_DATA_SET + " END) AS "
                                + RawContacts.ACCOUNT_TYPE_AND_DATA_SET + ","
                + RawContactsColumns.CONCRETE_SOURCE_ID + " AS " + RawContacts.SOURCE_ID + ","
                + RawContactsColumns.CONCRETE_NAME_VERIFIED + " AS "
                        + RawContacts.NAME_VERIFIED + ","
                + RawContactsColumns.CONCRETE_VERSION + " AS " + RawContacts.VERSION + ","
                + RawContactsColumns.CONCRETE_DIRTY + " AS " + RawContacts.DIRTY + ","
                + RawContactsColumns.CONCRETE_SYNC1 + " AS " + RawContacts.SYNC1 + ","
                + RawContactsColumns.CONCRETE_SYNC2 + " AS " + RawContacts.SYNC2 + ","
                + RawContactsColumns.CONCRETE_SYNC3 + " AS " + RawContacts.SYNC3 + ","
                + RawContactsColumns.CONCRETE_SYNC4 + " AS " + RawContacts.SYNC4;

        String baseContactColumns =
                Contacts.HAS_PHONE_NUMBER + ", "
                + Contacts.NAME_RAW_CONTACT_ID + ", "
                + Contacts.LOOKUP_KEY + ", "
                + Contacts.PHOTO_ID + ", "
                + Contacts.PHOTO_FILE_ID + ", "
                + "CAST(" + Clauses.CONTACT_VISIBLE + " AS INTEGER) AS "
                        + Contacts.IN_VISIBLE_GROUP + ", "
                + ContactsColumns.LAST_STATUS_UPDATE_ID + ", "
                + ContactsColumns.CONCRETE_CONTACT_LAST_UPDATED_TIMESTAMP;

        String contactOptionColumns =
                ContactsColumns.CONCRETE_CUSTOM_RINGTONE
                        + " AS " + RawContacts.CUSTOM_RINGTONE + ","
                + ContactsColumns.CONCRETE_SEND_TO_VOICEMAIL
                        + " AS " + RawContacts.SEND_TO_VOICEMAIL + ","
                + ContactsColumns.CONCRETE_LAST_TIME_CONTACTED
                        + " AS " + RawContacts.LAST_TIME_CONTACTED + ","
                + ContactsColumns.CONCRETE_TIMES_CONTACTED
                        + " AS " + RawContacts.TIMES_CONTACTED + ","
                + ContactsColumns.CONCRETE_STARRED
                        + " AS " + RawContacts.STARRED + ","
                + ContactsColumns.CONCRETE_PINNED
                        + " AS " + RawContacts.PINNED;

        String contactNameColumns =
                "name_raw_contact." + RawContacts.DISPLAY_NAME_SOURCE
                        + " AS " + Contacts.DISPLAY_NAME_SOURCE + ", "
                + "name_raw_contact." + RawContacts.DISPLAY_NAME_PRIMARY
                        + " AS " + Contacts.DISPLAY_NAME_PRIMARY + ", "
                + "name_raw_contact." + RawContacts.DISPLAY_NAME_ALTERNATIVE
                        + " AS " + Contacts.DISPLAY_NAME_ALTERNATIVE + ", "
                + "name_raw_contact." + RawContacts.PHONETIC_NAME
                        + " AS " + Contacts.PHONETIC_NAME + ", "
                + "name_raw_contact." + RawContacts.PHONETIC_NAME_STYLE
                        + " AS " + Contacts.PHONETIC_NAME_STYLE + ", "
                + "name_raw_contact." + RawContacts.SORT_KEY_PRIMARY
                        + " AS " + Contacts.SORT_KEY_PRIMARY + ", "
                + "name_raw_contact." + RawContactsColumns.PHONEBOOK_LABEL_PRIMARY
                        + " AS " + ContactsColumns.PHONEBOOK_LABEL_PRIMARY + ", "
                + "name_raw_contact." + RawContactsColumns.PHONEBOOK_BUCKET_PRIMARY
                        + " AS " + ContactsColumns.PHONEBOOK_BUCKET_PRIMARY + ", "
                + "name_raw_contact." + RawContacts.SORT_KEY_ALTERNATIVE
                        + " AS " + Contacts.SORT_KEY_ALTERNATIVE + ", "
                + "name_raw_contact." + RawContactsColumns.PHONEBOOK_LABEL_ALTERNATIVE
                        + " AS " + ContactsColumns.PHONEBOOK_LABEL_ALTERNATIVE + ", "
                + "name_raw_contact." + RawContactsColumns.PHONEBOOK_BUCKET_ALTERNATIVE
                        + " AS " + ContactsColumns.PHONEBOOK_BUCKET_ALTERNATIVE;

        String dataSelect = "SELECT "
                + DataColumns.CONCRETE_ID + " AS " + Data._ID + ","
                + Data.RAW_CONTACT_ID + ", "
                + RawContactsColumns.CONCRETE_CONTACT_ID + " AS " + RawContacts.CONTACT_ID + ", "
                + syncColumns + ", "
                + dataColumns + ", "
                + contactOptionColumns + ", "
                + contactNameColumns + ", "
                + baseContactColumns + ", "
                + buildDisplayPhotoUriAlias(RawContactsColumns.CONCRETE_CONTACT_ID,
                        Contacts.PHOTO_URI) + ", "
                + buildThumbnailPhotoUriAlias(RawContactsColumns.CONCRETE_CONTACT_ID,
                        Contacts.PHOTO_THUMBNAIL_URI) + ", "
                + dbForProfile() + " AS " + RawContacts.RAW_CONTACT_IS_USER_PROFILE + ", "
                + Tables.GROUPS + "." + Groups.SOURCE_ID + " AS " + GroupMembership.GROUP_SOURCE_ID
                + " FROM " + Tables.DATA
                + " JOIN " + Tables.MIMETYPES + " ON ("
                +   DataColumns.CONCRETE_MIMETYPE_ID + "=" + MimetypesColumns.CONCRETE_ID + ")"
                + " JOIN " + Tables.RAW_CONTACTS + " ON ("
                +   DataColumns.CONCRETE_RAW_CONTACT_ID + "=" + RawContactsColumns.CONCRETE_ID + ")"
                + " JOIN " + Tables.ACCOUNTS + " ON ("
                +   RawContactsColumns.CONCRETE_ACCOUNT_ID + "=" + AccountsColumns.CONCRETE_ID
                    + ")"
                + " JOIN " + Tables.CONTACTS + " ON ("
                +   RawContactsColumns.CONCRETE_CONTACT_ID + "=" + ContactsColumns.CONCRETE_ID + ")"
                + " JOIN " + Tables.RAW_CONTACTS + " AS name_raw_contact ON("
                +   Contacts.NAME_RAW_CONTACT_ID + "=name_raw_contact." + RawContacts._ID + ")"
                + " LEFT OUTER JOIN " + Tables.PACKAGES + " ON ("
                +   DataColumns.CONCRETE_PACKAGE_ID + "=" + PackagesColumns.CONCRETE_ID + ")"
                + " LEFT OUTER JOIN " + Tables.GROUPS + " ON ("
                +   MimetypesColumns.CONCRETE_MIMETYPE + "='" + GroupMembership.CONTENT_ITEM_TYPE
                +   "' AND " + GroupsColumns.CONCRETE_ID + "="
                        + Tables.DATA + "." + GroupMembership.GROUP_ROW_ID + ")";

        db.execSQL("CREATE VIEW " + Views.DATA + " AS " + dataSelect);

        String rawContactOptionColumns =
                RawContacts.CUSTOM_RINGTONE + ","
                + RawContacts.SEND_TO_VOICEMAIL + ","
                + RawContacts.LAST_TIME_CONTACTED + ","
                + RawContacts.TIMES_CONTACTED + ","
                + RawContacts.STARRED + ","
                + RawContacts.PINNED;

        String rawContactsSelect = "SELECT "
                + RawContactsColumns.CONCRETE_ID + " AS " + RawContacts._ID + ","
                + RawContacts.CONTACT_ID + ", "
                + RawContacts.AGGREGATION_MODE + ", "
                + RawContacts.RAW_CONTACT_IS_READ_ONLY + ", "
                + RawContacts.DELETED + ", "
                + RawContacts.DISPLAY_NAME_SOURCE  + ", "
                + RawContacts.DISPLAY_NAME_PRIMARY  + ", "
                + RawContacts.DISPLAY_NAME_ALTERNATIVE  + ", "
                + RawContacts.PHONETIC_NAME  + ", "
                + RawContacts.PHONETIC_NAME_STYLE  + ", "
                + RawContacts.SORT_KEY_PRIMARY  + ", "
                + RawContactsColumns.PHONEBOOK_LABEL_PRIMARY  + ", "
                + RawContactsColumns.PHONEBOOK_BUCKET_PRIMARY  + ", "
                + RawContacts.SORT_KEY_ALTERNATIVE + ", "
                + RawContactsColumns.PHONEBOOK_LABEL_ALTERNATIVE  + ", "
                + RawContactsColumns.PHONEBOOK_BUCKET_ALTERNATIVE  + ", "
                + dbForProfile() + " AS " + RawContacts.RAW_CONTACT_IS_USER_PROFILE + ", "
                + rawContactOptionColumns + ", "
                + syncColumns
                + " FROM " + Tables.RAW_CONTACTS
                + " JOIN " + Tables.ACCOUNTS + " ON ("
                +   RawContactsColumns.CONCRETE_ACCOUNT_ID + "=" + AccountsColumns.CONCRETE_ID
                    + ")";

        db.execSQL("CREATE VIEW " + Views.RAW_CONTACTS + " AS " + rawContactsSelect);

        String contactsColumns =
                ContactsColumns.CONCRETE_CUSTOM_RINGTONE
                        + " AS " + Contacts.CUSTOM_RINGTONE + ", "
                + contactNameColumns + ", "
                + baseContactColumns + ", "
                + ContactsColumns.CONCRETE_LAST_TIME_CONTACTED
                        + " AS " + Contacts.LAST_TIME_CONTACTED + ", "
                + ContactsColumns.CONCRETE_SEND_TO_VOICEMAIL
                        + " AS " + Contacts.SEND_TO_VOICEMAIL + ", "
                + ContactsColumns.CONCRETE_STARRED
                        + " AS " + Contacts.STARRED + ", "
                + ContactsColumns.CONCRETE_PINNED
                + " AS " + Contacts.PINNED + ", "
                + ContactsColumns.CONCRETE_TIMES_CONTACTED
                        + " AS " + Contacts.TIMES_CONTACTED;

        String contactsSelect = "SELECT "
                + ContactsColumns.CONCRETE_ID + " AS " + Contacts._ID + ","
                + contactsColumns + ", "
                + buildDisplayPhotoUriAlias(ContactsColumns.CONCRETE_ID, Contacts.PHOTO_URI) + ", "
                + buildThumbnailPhotoUriAlias(ContactsColumns.CONCRETE_ID,
                        Contacts.PHOTO_THUMBNAIL_URI) + ", "
                + dbForProfile() + " AS " + Contacts.IS_USER_PROFILE
                + " FROM " + Tables.CONTACTS
                + " JOIN " + Tables.RAW_CONTACTS + " AS name_raw_contact ON("
                +   Contacts.NAME_RAW_CONTACT_ID + "=name_raw_contact." + RawContacts._ID + ")";

        db.execSQL("CREATE VIEW " + Views.CONTACTS + " AS " + contactsSelect);

        String rawEntitiesSelect = "SELECT "
                + RawContacts.CONTACT_ID + ", "
                + RawContactsColumns.CONCRETE_DELETED + " AS " + RawContacts.DELETED + ","
                + dataColumns + ", "
                + syncColumns + ", "
                + Data.SYNC1 + ", "
                + Data.SYNC2 + ", "
                + Data.SYNC3 + ", "
                + Data.SYNC4 + ", "
                + RawContactsColumns.CONCRETE_ID + " AS " + RawContacts._ID + ", "
                + DataColumns.CONCRETE_ID + " AS " + RawContacts.Entity.DATA_ID + ","
                + RawContactsColumns.CONCRETE_STARRED + " AS " + RawContacts.STARRED + ","
                + dbForProfile() + " AS " + RawContacts.RAW_CONTACT_IS_USER_PROFILE + ","
                + Tables.GROUPS + "." + Groups.SOURCE_ID + " AS " + GroupMembership.GROUP_SOURCE_ID
                + " FROM " + Tables.RAW_CONTACTS
                + " JOIN " + Tables.ACCOUNTS + " ON ("
                +   RawContactsColumns.CONCRETE_ACCOUNT_ID + "=" + AccountsColumns.CONCRETE_ID
                    + ")"
                + " LEFT OUTER JOIN " + Tables.DATA + " ON ("
                +   DataColumns.CONCRETE_RAW_CONTACT_ID + "=" + RawContactsColumns.CONCRETE_ID + ")"
                + " LEFT OUTER JOIN " + Tables.PACKAGES + " ON ("
                +   DataColumns.CONCRETE_PACKAGE_ID + "=" + PackagesColumns.CONCRETE_ID + ")"
                + " LEFT OUTER JOIN " + Tables.MIMETYPES + " ON ("
                +   DataColumns.CONCRETE_MIMETYPE_ID + "=" + MimetypesColumns.CONCRETE_ID + ")"
                + " LEFT OUTER JOIN " + Tables.GROUPS + " ON ("
                +   MimetypesColumns.CONCRETE_MIMETYPE + "='" + GroupMembership.CONTENT_ITEM_TYPE
                +   "' AND " + GroupsColumns.CONCRETE_ID + "="
                + Tables.DATA + "." + GroupMembership.GROUP_ROW_ID + ")";

        db.execSQL("CREATE VIEW " + Views.RAW_ENTITIES + " AS "
                + rawEntitiesSelect);

        String entitiesSelect = "SELECT "
                + RawContactsColumns.CONCRETE_CONTACT_ID + " AS " + Contacts._ID + ", "
                + RawContactsColumns.CONCRETE_CONTACT_ID + " AS " + RawContacts.CONTACT_ID + ", "
                + RawContactsColumns.CONCRETE_DELETED + " AS " + RawContacts.DELETED + ","
                + dataColumns + ", "
                + syncColumns + ", "
                + contactsColumns + ", "
                + buildDisplayPhotoUriAlias(RawContactsColumns.CONCRETE_CONTACT_ID,
                        Contacts.PHOTO_URI) + ", "
                + buildThumbnailPhotoUriAlias(RawContactsColumns.CONCRETE_CONTACT_ID,
                        Contacts.PHOTO_THUMBNAIL_URI) + ", "
                + dbForProfile() + " AS " + Contacts.IS_USER_PROFILE + ", "
                + Data.SYNC1 + ", "
                + Data.SYNC2 + ", "
                + Data.SYNC3 + ", "
                + Data.SYNC4 + ", "
                + RawContactsColumns.CONCRETE_ID + " AS " + Contacts.Entity.RAW_CONTACT_ID + ", "
                + DataColumns.CONCRETE_ID + " AS " + Contacts.Entity.DATA_ID + ","
                + Tables.GROUPS + "." + Groups.SOURCE_ID + " AS " + GroupMembership.GROUP_SOURCE_ID
                + " FROM " + Tables.RAW_CONTACTS
                + " JOIN " + Tables.ACCOUNTS + " ON ("
                +   RawContactsColumns.CONCRETE_ACCOUNT_ID + "=" + AccountsColumns.CONCRETE_ID
                    + ")"
                + " JOIN " + Tables.CONTACTS + " ON ("
                +   RawContactsColumns.CONCRETE_CONTACT_ID + "=" + ContactsColumns.CONCRETE_ID + ")"
                + " JOIN " + Tables.RAW_CONTACTS + " AS name_raw_contact ON("
                +   Contacts.NAME_RAW_CONTACT_ID + "=name_raw_contact." + RawContacts._ID + ")"
                + " LEFT OUTER JOIN " + Tables.DATA + " ON ("
                +   DataColumns.CONCRETE_RAW_CONTACT_ID + "=" + RawContactsColumns.CONCRETE_ID + ")"
                + " LEFT OUTER JOIN " + Tables.PACKAGES + " ON ("
                +   DataColumns.CONCRETE_PACKAGE_ID + "=" + PackagesColumns.CONCRETE_ID + ")"
                + " LEFT OUTER JOIN " + Tables.MIMETYPES + " ON ("
                +   DataColumns.CONCRETE_MIMETYPE_ID + "=" + MimetypesColumns.CONCRETE_ID + ")"
                + " LEFT OUTER JOIN " + Tables.GROUPS + " ON ("
                +   MimetypesColumns.CONCRETE_MIMETYPE + "='" + GroupMembership.CONTENT_ITEM_TYPE
                +   "' AND " + GroupsColumns.CONCRETE_ID + "="
                + Tables.DATA + "." + GroupMembership.GROUP_ROW_ID + ")";

        db.execSQL("CREATE VIEW " + Views.ENTITIES + " AS "
                + entitiesSelect);

        String dataUsageStatSelect = "SELECT "
                + DataUsageStatColumns.CONCRETE_ID + " AS " + DataUsageStatColumns._ID + ", "
                + DataUsageStatColumns.DATA_ID + ", "
                + RawContactsColumns.CONCRETE_CONTACT_ID + " AS " + RawContacts.CONTACT_ID + ", "
                + MimetypesColumns.CONCRETE_MIMETYPE + " AS " + Data.MIMETYPE + ", "
                + DataUsageStatColumns.USAGE_TYPE_INT + ", "
                + DataUsageStatColumns.TIMES_USED + ", "
                + DataUsageStatColumns.LAST_TIME_USED
                + " FROM " + Tables.DATA_USAGE_STAT
                + " JOIN " + Tables.DATA + " ON ("
                +   DataColumns.CONCRETE_ID + "=" + DataUsageStatColumns.CONCRETE_DATA_ID + ")"
                + " JOIN " + Tables.RAW_CONTACTS + " ON ("
                +   RawContactsColumns.CONCRETE_ID + "=" + DataColumns.CONCRETE_RAW_CONTACT_ID
                    + " )"
                + " JOIN " + Tables.MIMETYPES + " ON ("
                +   MimetypesColumns.CONCRETE_ID + "=" + DataColumns.CONCRETE_MIMETYPE_ID + ")";

        db.execSQL("CREATE VIEW " + Views.DATA_USAGE_STAT + " AS " + dataUsageStatSelect);

        String streamItemSelect = "SELECT " +
                StreamItemsColumns.CONCRETE_ID + ", " +
                ContactsColumns.CONCRETE_ID + " AS " + StreamItems.CONTACT_ID + ", " +
                ContactsColumns.CONCRETE_LOOKUP_KEY +
                        " AS " + StreamItems.CONTACT_LOOKUP_KEY + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_NAME + ", " +
                AccountsColumns.CONCRETE_ACCOUNT_TYPE + ", " +
                AccountsColumns.CONCRETE_DATA_SET + ", " +
                StreamItemsColumns.CONCRETE_RAW_CONTACT_ID +
                        " as " + StreamItems.RAW_CONTACT_ID + ", " +
                RawContactsColumns.CONCRETE_SOURCE_ID +
                        " as " + StreamItems.RAW_CONTACT_SOURCE_ID + ", " +
                StreamItemsColumns.CONCRETE_PACKAGE + ", " +
                StreamItemsColumns.CONCRETE_ICON + ", " +
                StreamItemsColumns.CONCRETE_LABEL + ", " +
                StreamItemsColumns.CONCRETE_TEXT + ", " +
                StreamItemsColumns.CONCRETE_TIMESTAMP + ", " +
                StreamItemsColumns.CONCRETE_COMMENTS + ", " +
                StreamItemsColumns.CONCRETE_SYNC1 + ", " +
                StreamItemsColumns.CONCRETE_SYNC2 + ", " +
                StreamItemsColumns.CONCRETE_SYNC3 + ", " +
                StreamItemsColumns.CONCRETE_SYNC4 +
                " FROM " + Tables.STREAM_ITEMS
                + " JOIN " + Tables.RAW_CONTACTS + " ON ("
                + StreamItemsColumns.CONCRETE_RAW_CONTACT_ID + "=" + RawContactsColumns.CONCRETE_ID
                    + ")"
                + " JOIN " + Tables.ACCOUNTS + " ON ("
                +   RawContactsColumns.CONCRETE_ACCOUNT_ID + "=" + AccountsColumns.CONCRETE_ID
                    + ")"
                + " JOIN " + Tables.CONTACTS + " ON ("
                + RawContactsColumns.CONCRETE_CONTACT_ID + "=" + ContactsColumns.CONCRETE_ID + ")";

        db.execSQL("CREATE VIEW " + Views.STREAM_ITEMS + " AS " + streamItemSelect);
    }

    private static String buildDisplayPhotoUriAlias(String contactIdColumn, String alias) {
        return "(CASE WHEN " + Contacts.PHOTO_FILE_ID + " IS NULL THEN (CASE WHEN "
                + Contacts.PHOTO_ID + " IS NULL"
                + " OR " + Contacts.PHOTO_ID + "=0"
                + " THEN NULL"
                + " ELSE '" + Contacts.CONTENT_URI + "/'||"
                        + contactIdColumn + "|| '/" + Photo.CONTENT_DIRECTORY + "'"
                + " END) ELSE '" + DisplayPhoto.CONTENT_URI + "/'||"
                        + Contacts.PHOTO_FILE_ID + " END)"
                + " AS " + alias;
    }

    private static String buildThumbnailPhotoUriAlias(String contactIdColumn, String alias) {
        return "(CASE WHEN "
                + Contacts.PHOTO_ID + " IS NULL"
                + " OR " + Contacts.PHOTO_ID + "=0"
                + " THEN NULL"
                + " ELSE '" + Contacts.CONTENT_URI + "/'||"
                        + contactIdColumn + "|| '/" + Photo.CONTENT_DIRECTORY + "'"
                + " END)"
                + " AS " + alias;
    }

    /**
     * Returns the value to be returned when querying the column indicating that the contact
     * or raw contact belongs to the user's personal profile.  Overridden in the profile
     * DB helper subclass.
     */
    protected int dbForProfile() {
        return 0;
    }

    private void createGroupsView(SQLiteDatabase db) {
        db.execSQL("DROP VIEW IF EXISTS " + Views.GROUPS + ";");

        String groupsColumns =
                GroupsColumns.CONCRETE_ACCOUNT_ID + " AS " + GroupsColumns.ACCOUNT_ID + ","
                + AccountsColumns.CONCRETE_ACCOUNT_NAME + " AS " + Groups.ACCOUNT_NAME + ","
                + AccountsColumns.CONCRETE_ACCOUNT_TYPE + " AS " + Groups.ACCOUNT_TYPE + ","
                + AccountsColumns.CONCRETE_DATA_SET + " AS " + Groups.DATA_SET + ","
                + "(CASE WHEN " + AccountsColumns.CONCRETE_DATA_SET
                    + " IS NULL THEN " + AccountsColumns.CONCRETE_ACCOUNT_TYPE
                    + " ELSE " + AccountsColumns.CONCRETE_ACCOUNT_TYPE
                        + "||'/'||" + AccountsColumns.CONCRETE_DATA_SET + " END) AS "
                            + Groups.ACCOUNT_TYPE_AND_DATA_SET + ","
                + Groups.SOURCE_ID + ","
                + Groups.VERSION + ","
                + Groups.DIRTY + ","
                + Groups.TITLE + ","
                + Groups.TITLE_RES + ","
                + Groups.NOTES + ","
                + Groups.SYSTEM_ID + ","
                + Groups.DELETED + ","
                + Groups.GROUP_VISIBLE + ","
                + Groups.SHOULD_SYNC + ","
                + Groups.AUTO_ADD + ","
                + Groups.FAVORITES + ","
                + Groups.GROUP_IS_READ_ONLY + ","
                + Groups.SYNC1 + ","
                + Groups.SYNC2 + ","
                + Groups.SYNC3 + ","
                + Groups.SYNC4 + ","
                + PackagesColumns.PACKAGE + " AS " + Groups.RES_PACKAGE;

        String groupsSelect = "SELECT "
                + GroupsColumns.CONCRETE_ID + " AS " + Groups._ID + ","
                + groupsColumns
                + " FROM " + Tables.GROUPS
                + " JOIN " + Tables.ACCOUNTS + " ON ("
                    + GroupsColumns.CONCRETE_ACCOUNT_ID + "=" + AccountsColumns.CONCRETE_ID + ")"
                + " LEFT OUTER JOIN " + Tables.PACKAGES + " ON ("
                    + GroupsColumns.CONCRETE_PACKAGE_ID + "=" + PackagesColumns.CONCRETE_ID + ")";

        db.execSQL("CREATE VIEW " + Views.GROUPS + " AS " + groupsSelect);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "ContactsProvider cannot proceed because downgrading your database is not " +
                "supported. To continue, please either re-upgrade to your previous Android " +
                "version, or clear all application data in Contacts Storage (this will result " +
                "in the loss of all local contacts that are not synced). To avoid data loss, " +
                "your contacts database will not be wiped automatically.");
        super.onDowngrade(db, oldVersion, newVersion);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 99) {
            Log.i(TAG, "Upgrading from version " + oldVersion + " to " + newVersion
                    + ", data will be lost!");

            db.execSQL("DROP TABLE IF EXISTS " + Tables.CONTACTS + ";");
            db.execSQL("DROP TABLE IF EXISTS " + Tables.RAW_CONTACTS + ";");
            db.execSQL("DROP TABLE IF EXISTS " + Tables.PACKAGES + ";");
            db.execSQL("DROP TABLE IF EXISTS " + Tables.MIMETYPES + ";");
            db.execSQL("DROP TABLE IF EXISTS " + Tables.DATA + ";");
            db.execSQL("DROP TABLE IF EXISTS " + Tables.PHONE_LOOKUP + ";");
            db.execSQL("DROP TABLE IF EXISTS " + Tables.NAME_LOOKUP + ";");
            db.execSQL("DROP TABLE IF EXISTS " + Tables.NICKNAME_LOOKUP + ";");
            db.execSQL("DROP TABLE IF EXISTS " + Tables.GROUPS + ";");
            db.execSQL("DROP TABLE IF EXISTS activities;");
            db.execSQL("DROP TABLE IF EXISTS " + Tables.CALLS + ";");
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SETTINGS + ";");
            db.execSQL("DROP TABLE IF EXISTS " + Tables.STATUS_UPDATES + ";");

            // TODO: we should not be dropping agg_exceptions and contact_options. In case that
            // table's schema changes, we should try to preserve the data, because it was entered
            // by the user and has never been synched to the server.
            db.execSQL("DROP TABLE IF EXISTS " + Tables.AGGREGATION_EXCEPTIONS + ";");

            onCreate(db);
            return;
        }

        Log.i(TAG, "Upgrading from version " + oldVersion + " to " + newVersion);

        boolean upgradeViewsAndTriggers = false;
        boolean upgradeNameLookup = false;
        boolean upgradeLegacyApiSupport = false;
        boolean upgradeSearchIndex = false;
        boolean rescanDirectories = false;
        boolean rebuildSqliteStats = false;
        boolean upgradeLocaleSpecificData = false;

        if (oldVersion == 99) {
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }

        if (oldVersion == 100) {
            db.execSQL("CREATE INDEX IF NOT EXISTS mimetypes_mimetype_index ON "
                    + Tables.MIMETYPES + " ("
                            + MimetypesColumns.MIMETYPE + ","
                            + MimetypesColumns._ID + ");");
            updateIndexStats(db, Tables.MIMETYPES,
                    "mimetypes_mimetype_index", "50 1 1");

            upgradeViewsAndTriggers = true;
            oldVersion++;
        }

        if (oldVersion == 101) {
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }

        if (oldVersion == 102) {
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }

        if (oldVersion == 103) {
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }

        if (oldVersion == 104 || oldVersion == 201) {
            LegacyApiSupport.createSettingsTable(db);
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }

        if (oldVersion == 105) {
            upgradeToVersion202(db);
            upgradeNameLookup = true;
            oldVersion = 202;
        }

        if (oldVersion == 202) {
            upgradeToVersion203(db);
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }

        if (oldVersion == 203) {
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }

        if (oldVersion == 204) {
            upgradeToVersion205(db);
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }

        if (oldVersion == 205) {
            upgrateToVersion206(db);
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }

        if (oldVersion == 206) {
            upgradeToVersion300(db);
            oldVersion = 300;
        }

        if (oldVersion == 300) {
            upgradeViewsAndTriggers = true;
            oldVersion = 301;
        }

        if (oldVersion == 301) {
            upgradeViewsAndTriggers = true;
            oldVersion = 302;
        }

        if (oldVersion == 302) {
            upgradeEmailToVersion303(db);
            upgradeNicknameToVersion303(db);
            oldVersion = 303;
        }

        if (oldVersion == 303) {
            upgradeToVersion304(db);
            oldVersion = 304;
        }

        if (oldVersion == 304) {
            upgradeNameLookup = true;
            oldVersion = 305;
        }

        if (oldVersion == 305) {
            upgradeToVersion306(db);
            oldVersion = 306;
        }

        if (oldVersion == 306) {
            upgradeToVersion307(db);
            oldVersion = 307;
        }

        if (oldVersion == 307) {
            upgradeToVersion308(db);
            oldVersion = 308;
        }

        // Gingerbread upgrades
        if (oldVersion < 350) {
            upgradeViewsAndTriggers = true;
            oldVersion = 351;
        }

        if (oldVersion == 351) {
            upgradeNameLookup = true;
            oldVersion = 352;
        }

        if (oldVersion == 352) {
            upgradeToVersion353(db);
            oldVersion = 353;
        }

        // Honeycomb upgrades
        if (oldVersion < 400) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion400(db);
            oldVersion = 400;
        }

        if (oldVersion == 400) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion401(db);
            oldVersion = 401;
        }

        if (oldVersion == 401) {
            upgradeToVersion402(db);
            oldVersion = 402;
        }

        if (oldVersion == 402) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion403(db);
            oldVersion = 403;
        }

        if (oldVersion == 403) {
            upgradeViewsAndTriggers = true;
            oldVersion = 404;
        }

        if (oldVersion == 404) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion405(db);
            oldVersion = 405;
        }

        if (oldVersion == 405) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion406(db);
            oldVersion = 406;
        }

        if (oldVersion == 406) {
            upgradeViewsAndTriggers = true;
            oldVersion = 407;
        }

        if (oldVersion == 407) {
            // Obsolete
            oldVersion = 408;
        }

        if (oldVersion == 408) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion409(db);
            oldVersion = 409;
        }

        if (oldVersion == 409) {
            upgradeViewsAndTriggers = true;
            oldVersion = 410;
        }

        if (oldVersion == 410) {
            upgradeToVersion411(db);
            oldVersion = 411;
        }

        if (oldVersion == 411) {
            // Same upgrade as 353, only on Honeycomb devices
            upgradeToVersion353(db);
            oldVersion = 412;
        }

        if (oldVersion == 412) {
            upgradeToVersion413(db);
            oldVersion = 413;
        }

        if (oldVersion == 413) {
            upgradeNameLookup = true;
            oldVersion = 414;
        }

        if (oldVersion == 414) {
            upgradeToVersion415(db);
            upgradeViewsAndTriggers = true;
            oldVersion = 415;
        }

        if (oldVersion == 415) {
            upgradeToVersion416(db);
            oldVersion = 416;
        }

        if (oldVersion == 416) {
            upgradeLegacyApiSupport = true;
            oldVersion = 417;
        }

        // Honeycomb-MR1 upgrades
        if (oldVersion < 500) {
            upgradeSearchIndex = true;
        }

        if (oldVersion < 501) {
            upgradeSearchIndex = true;
            upgradeToVersion501(db);
            oldVersion = 501;
        }

        if (oldVersion < 502) {
            upgradeSearchIndex = true;
            upgradeToVersion502(db);
            oldVersion = 502;
        }

        if (oldVersion < 503) {
            upgradeSearchIndex = true;
            oldVersion = 503;
        }

        if (oldVersion < 504) {
            upgradeToVersion504(db);
            oldVersion = 504;
        }

        if (oldVersion < 600) {
            upgradeToVersion600(db);
            upgradeViewsAndTriggers = true;
            oldVersion = 600;
        }

        if (oldVersion < 601) {
            upgradeToVersion601(db);
            oldVersion = 601;
        }

        if (oldVersion < 602) {
            upgradeToVersion602(db);
            oldVersion = 602;
        }

        if (oldVersion < 603) {
            upgradeViewsAndTriggers = true;
            oldVersion = 603;
        }

        if (oldVersion < 604) {
            upgradeToVersion604(db);
            oldVersion = 604;
        }

        if (oldVersion < 605) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion605(db);
            oldVersion = 605;
        }

        if (oldVersion < 606) {
            upgradeViewsAndTriggers = true;
            upgradeLegacyApiSupport = true;
            upgradeToVersion606(db);
            oldVersion = 606;
        }

        if (oldVersion < 607) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion607(db);
            oldVersion = 607;
        }

        if (oldVersion < 608) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion608(db);
            oldVersion = 608;
        }

        if (oldVersion < 609) {
            upgradeToVersion609(db);
            oldVersion = 609;
        }

        if (oldVersion < 610) {
            upgradeToVersion610(db);
            oldVersion = 610;
        }

        if (oldVersion < 611) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion611(db);
            oldVersion = 611;
        }

        if (oldVersion < 612) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion612(db);
            oldVersion = 612;
        }

        if (oldVersion < 613) {
            upgradeToVersion613(db);
            oldVersion = 613;
        }

        if (oldVersion < 614) {
            // this creates the view "view_stream_items"
            upgradeViewsAndTriggers = true;
            oldVersion = 614;
        }

        if (oldVersion < 615) {
            upgradeToVersion615(db);
            oldVersion = 615;
        }

        if (oldVersion < 616) {
            // this updates the "view_stream_items" view
            upgradeViewsAndTriggers = true;
            oldVersion = 616;
        }

        if (oldVersion < 617) {
            // This version upgrade obsoleted the profile_raw_contact_id field of the Accounts
            // table, but we aren't removing the column because it is very little data (and not
            // referenced anymore).  We do need to upgrade the views to handle the simplified
            // per-database "is profile" columns.
            upgradeViewsAndTriggers = true;
            oldVersion = 617;
        }

        if (oldVersion < 618) {
            upgradeToVersion618(db);
            oldVersion = 618;
        }

        if (oldVersion < 619) {
            upgradeViewsAndTriggers = true;
            oldVersion = 619;
        }

        if (oldVersion < 620) {
            upgradeViewsAndTriggers = true;
            oldVersion = 620;
        }

        if (oldVersion < 621) {
            upgradeSearchIndex = true;
            oldVersion = 621;
        }

        if (oldVersion < 622) {
            upgradeToVersion622(db);
            oldVersion = 622;
        }

        if (oldVersion < 623) {
            // change FTS to normalize names using collation key
            upgradeSearchIndex = true;
            oldVersion = 623;
        }

        if (oldVersion < 624) {
            // Upgraded the sqlite index stats
            upgradeViewsAndTriggers = true;
            oldVersion = 624;
        }

        if (oldVersion < 625) {
            // Fix for search for hyphenated names
            upgradeSearchIndex = true;
            oldVersion = 625;
        }

        if (oldVersion < 626) {
            upgradeToVersion626(db);
            upgradeViewsAndTriggers = true;
            oldVersion = 626;
        }

        if (oldVersion < 700) {
            rescanDirectories = true;
            oldVersion = 700;
        }

        if (oldVersion < 701) {
            upgradeToVersion701(db);
            oldVersion = 701;
        }

        if (oldVersion < 702) {
            upgradeToVersion702(db);
            oldVersion = 702;
        }

        if (oldVersion < 703) {
            // Now names like "L'Image" will be searchable.
            upgradeSearchIndex = true;
            oldVersion = 703;
        }

        if (oldVersion < 704) {
            db.execSQL("DROP TABLE IF EXISTS activities;");
            oldVersion = 704;
        }

        if (oldVersion < 705) {
            // Before this version, we didn't rebuild the search index on locale changes, so
            // if the locale has changed after sync, the index contains gets stale.
            // To correct the issue we have to rebuild the index here.
            upgradeSearchIndex = true;
            oldVersion = 705;
        }

        if (oldVersion < 706) {
            // Prior to this version, we didn't rebuild the stats table after drop operations,
            // which resulted in losing some of the rows from the stats table.
            rebuildSqliteStats = true;
            oldVersion = 706;
        }

        if (oldVersion < 707) {
            upgradeToVersion707(db);
            upgradeViewsAndTriggers = true;
            oldVersion = 707;
        }

        if (oldVersion < 708) {
            // Sort keys, phonebook labels and buckets, and search keys have
            // changed so force a rebuild.
            upgradeLocaleSpecificData = true;
            oldVersion = 708;
        }
        if (oldVersion < 709) {
            // Added secondary locale phonebook labels; changed Japanese
            // and Chinese sort keys.
            upgradeLocaleSpecificData = true;
            oldVersion = 709;
        }

        if (oldVersion < 710) {
            upgradeToVersion710(db);
            upgradeViewsAndTriggers = true;
            oldVersion = 710;
        }

        if (oldVersion < 800) {
            upgradeToVersion800(db);
            oldVersion = 800;
        }

        if (oldVersion < 801) {
            setProperty(db, DbProperties.DATABASE_TIME_CREATED, String.valueOf(
                    System.currentTimeMillis()));
            oldVersion = 801;
        }

        if (oldVersion < 802) {
            upgradeToVersion802(db);
            upgradeViewsAndTriggers = true;
            oldVersion = 802;
        }

        if (oldVersion < 803) {
            // Rebuild the search index so that names, organizations and nicknames are
            // now indexed as names.
            upgradeSearchIndex = true;
            oldVersion = 803;
        }

        if (upgradeViewsAndTriggers) {
            createContactsViews(db);
            createGroupsView(db);
            createContactsTriggers(db);
            createContactsIndexes(db, false /* we build stats table later */);
            upgradeLegacyApiSupport = true;
            rebuildSqliteStats = true;
        }

        if (upgradeLegacyApiSupport) {
            LegacyApiSupport.createViews(db);
        }

        if (upgradeLocaleSpecificData) {
            upgradeLocaleData(db, false /* we build stats table later */);
            // Name lookups are rebuilt as part of the full locale rebuild
            upgradeNameLookup = false;
            upgradeSearchIndex = true;
            rebuildSqliteStats = true;
        }

        if (upgradeNameLookup) {
            rebuildNameLookup(db, false /* we build stats table later */);
            rebuildSqliteStats = true;
        }

        if (upgradeSearchIndex) {
            rebuildSearchIndex(db, false /* we build stats table later */);
            rebuildSqliteStats = true;
        }

        if (rescanDirectories) {
            // Force the next ContactDirectoryManager.scanAllPackages() to rescan all packages.
            // (It's called from the BACKGROUND_TASK_UPDATE_ACCOUNTS background task.)
            setProperty(db, DbProperties.DIRECTORY_SCAN_COMPLETE, "0");
        }

        if (rebuildSqliteStats) {
            updateSqliteStats(db);
        }

        if (oldVersion != newVersion) {
            throw new IllegalStateException(
                    "error upgrading the database to version " + newVersion);
        }
    }

    private void upgradeToVersion202(SQLiteDatabase db) {
        db.execSQL(
                "ALTER TABLE " + Tables.PHONE_LOOKUP +
                " ADD " + PhoneLookupColumns.MIN_MATCH + " TEXT;");

        db.execSQL("CREATE INDEX phone_lookup_min_match_index ON " + Tables.PHONE_LOOKUP + " (" +
                PhoneLookupColumns.MIN_MATCH + "," +
                PhoneLookupColumns.RAW_CONTACT_ID + "," +
                PhoneLookupColumns.DATA_ID +
        ");");

        updateIndexStats(db, Tables.PHONE_LOOKUP,
                "phone_lookup_min_match_index", "10000 2 2 1");

        SQLiteStatement update = db.compileStatement(
                "UPDATE " + Tables.PHONE_LOOKUP +
                " SET " + PhoneLookupColumns.MIN_MATCH + "=?" +
                " WHERE " + PhoneLookupColumns.DATA_ID + "=?");

        // Populate the new column
        Cursor c = db.query(Tables.PHONE_LOOKUP + " JOIN " + Tables.DATA +
                " ON (" + PhoneLookupColumns.DATA_ID + "=" + DataColumns.CONCRETE_ID + ")",
                new String[]{Data._ID, Phone.NUMBER}, null, null, null, null, null);
        try {
            while (c.moveToNext()) {
                long dataId = c.getLong(0);
                String number = c.getString(1);
                if (!TextUtils.isEmpty(number)) {
                    update.bindString(1, PhoneNumberUtils.toCallerIDMinMatch(number));
                    update.bindLong(2, dataId);
                    update.execute();
                }
            }
        } finally {
            c.close();
        }
    }

    private void upgradeToVersion203(SQLiteDatabase db) {
        // Garbage-collect first. A bug in Eclair was sometimes leaving
        // raw_contacts in the database that no longer had contacts associated
        // with them.  To avoid failures during this database upgrade, drop
        // the orphaned raw_contacts.
        db.execSQL(
                "DELETE FROM raw_contacts" +
                " WHERE contact_id NOT NULL" +
                " AND contact_id NOT IN (SELECT _id FROM contacts)");

        db.execSQL(
                "ALTER TABLE " + Tables.CONTACTS +
                " ADD " + Contacts.NAME_RAW_CONTACT_ID + " INTEGER REFERENCES raw_contacts(_id)");
        db.execSQL(
                "ALTER TABLE " + Tables.RAW_CONTACTS +
                " ADD contact_in_visible_group INTEGER NOT NULL DEFAULT 0");

        // For each Contact, find the RawContact that contributed the display name
        db.execSQL(
                "UPDATE " + Tables.CONTACTS +
                " SET " + Contacts.NAME_RAW_CONTACT_ID + "=(" +
                        " SELECT " + RawContacts._ID +
                        " FROM " + Tables.RAW_CONTACTS +
                        " WHERE " + RawContacts.CONTACT_ID + "=" + ContactsColumns.CONCRETE_ID +
                        " AND " + RawContactsColumns.CONCRETE_DISPLAY_NAME + "=" +
                                Tables.CONTACTS + "." + Contacts.DISPLAY_NAME +
                        " ORDER BY " + RawContacts._ID +
                        " LIMIT 1)"
        );

        db.execSQL("CREATE INDEX contacts_name_raw_contact_id_index ON " + Tables.CONTACTS + " (" +
                Contacts.NAME_RAW_CONTACT_ID +
        ");");

        // If for some unknown reason we missed some names, let's make sure there are
        // no contacts without a name, picking a raw contact "at random".
        db.execSQL(
                "UPDATE " + Tables.CONTACTS +
                " SET " + Contacts.NAME_RAW_CONTACT_ID + "=(" +
                        " SELECT " + RawContacts._ID +
                        " FROM " + Tables.RAW_CONTACTS +
                        " WHERE " + RawContacts.CONTACT_ID + "=" + ContactsColumns.CONCRETE_ID +
                        " ORDER BY " + RawContacts._ID +
                        " LIMIT 1)" +
                " WHERE " + Contacts.NAME_RAW_CONTACT_ID + " IS NULL"
        );

        // Wipe out DISPLAY_NAME on the Contacts table as it is no longer in use.
        db.execSQL(
                "UPDATE " + Tables.CONTACTS +
                " SET " + Contacts.DISPLAY_NAME + "=NULL"
        );

        // Copy the IN_VISIBLE_GROUP flag down to all raw contacts to allow
        // indexing on (display_name, in_visible_group)
        db.execSQL(
                "UPDATE " + Tables.RAW_CONTACTS +
                " SET contact_in_visible_group=(" +
                        "SELECT " + Contacts.IN_VISIBLE_GROUP +
                        " FROM " + Tables.CONTACTS +
                        " WHERE " + Contacts._ID + "=" + RawContacts.CONTACT_ID + ")" +
                " WHERE " + RawContacts.CONTACT_ID + " NOT NULL"
        );

        db.execSQL("CREATE INDEX raw_contact_sort_key1_index ON " + Tables.RAW_CONTACTS + " (" +
                "contact_in_visible_group" + "," +
                RawContactsColumns.DISPLAY_NAME + " COLLATE LOCALIZED ASC" +
        ");");

        db.execSQL("DROP INDEX contacts_visible_index");
        db.execSQL("CREATE INDEX contacts_visible_index ON " + Tables.CONTACTS + " (" +
                Contacts.IN_VISIBLE_GROUP +
        ");");
    }

    private void upgradeToVersion205(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.RAW_CONTACTS
                + " ADD " + RawContacts.DISPLAY_NAME_ALTERNATIVE + " TEXT;");
        db.execSQL("ALTER TABLE " + Tables.RAW_CONTACTS
                + " ADD " + RawContacts.PHONETIC_NAME + " TEXT;");
        db.execSQL("ALTER TABLE " + Tables.RAW_CONTACTS
                + " ADD " + RawContacts.PHONETIC_NAME_STYLE + " INTEGER;");
        db.execSQL("ALTER TABLE " + Tables.RAW_CONTACTS
                + " ADD " + RawContacts.SORT_KEY_PRIMARY
                + " TEXT COLLATE " + ContactsProvider2.PHONEBOOK_COLLATOR_NAME + ";");
        db.execSQL("ALTER TABLE " + Tables.RAW_CONTACTS
                + " ADD " + RawContacts.SORT_KEY_ALTERNATIVE
                + " TEXT COLLATE " + ContactsProvider2.PHONEBOOK_COLLATOR_NAME + ";");

        NameSplitter splitter = createNameSplitter();

        SQLiteStatement rawContactUpdate = db.compileStatement(
                "UPDATE " + Tables.RAW_CONTACTS +
                " SET " +
                        RawContacts.DISPLAY_NAME_PRIMARY + "=?," +
                        RawContacts.DISPLAY_NAME_ALTERNATIVE + "=?," +
                        RawContacts.PHONETIC_NAME + "=?," +
                        RawContacts.PHONETIC_NAME_STYLE + "=?," +
                        RawContacts.SORT_KEY_PRIMARY + "=?," +
                        RawContacts.SORT_KEY_ALTERNATIVE + "=?" +
                " WHERE " + RawContacts._ID + "=?");

        upgradeStructuredNamesToVersion205(db, rawContactUpdate, splitter);
        upgradeOrganizationsToVersion205(db, rawContactUpdate, splitter);

        db.execSQL("DROP INDEX raw_contact_sort_key1_index");
        db.execSQL("CREATE INDEX raw_contact_sort_key1_index ON " + Tables.RAW_CONTACTS + " (" +
                "contact_in_visible_group" + "," +
                RawContacts.SORT_KEY_PRIMARY +
        ");");

        db.execSQL("CREATE INDEX raw_contact_sort_key2_index ON " + Tables.RAW_CONTACTS + " (" +
                "contact_in_visible_group" + "," +
                RawContacts.SORT_KEY_ALTERNATIVE +
        ");");
    }

    private interface StructName205Query {
        String TABLE = Tables.DATA_JOIN_RAW_CONTACTS;

        String COLUMNS[] = {
                DataColumns.CONCRETE_ID,
                Data.RAW_CONTACT_ID,
                RawContacts.DISPLAY_NAME_SOURCE,
                RawContacts.DISPLAY_NAME_PRIMARY,
                StructuredName.PREFIX,
                StructuredName.GIVEN_NAME,
                StructuredName.MIDDLE_NAME,
                StructuredName.FAMILY_NAME,
                StructuredName.SUFFIX,
                StructuredName.PHONETIC_FAMILY_NAME,
                StructuredName.PHONETIC_MIDDLE_NAME,
                StructuredName.PHONETIC_GIVEN_NAME,
        };

        int ID = 0;
        int RAW_CONTACT_ID = 1;
        int DISPLAY_NAME_SOURCE = 2;
        int DISPLAY_NAME = 3;
        int PREFIX = 4;
        int GIVEN_NAME = 5;
        int MIDDLE_NAME = 6;
        int FAMILY_NAME = 7;
        int SUFFIX = 8;
        int PHONETIC_FAMILY_NAME = 9;
        int PHONETIC_MIDDLE_NAME = 10;
        int PHONETIC_GIVEN_NAME = 11;
    }

    private void upgradeStructuredNamesToVersion205(SQLiteDatabase db,
            SQLiteStatement rawContactUpdate, NameSplitter splitter) {

        // Process structured names to detect the style of the full name and phonetic name

        long mMimeType;
        try {
            mMimeType = DatabaseUtils.longForQuery(db,
                    "SELECT " + MimetypesColumns._ID +
                    " FROM " + Tables.MIMETYPES +
                    " WHERE " + MimetypesColumns.MIMETYPE
                            + "='" + StructuredName.CONTENT_ITEM_TYPE + "'", null);
        } catch (SQLiteDoneException e) {
            // No structured names in the database
            return;
        }

        SQLiteStatement structuredNameUpdate = db.compileStatement(
                "UPDATE " + Tables.DATA +
                " SET " +
                        StructuredName.FULL_NAME_STYLE + "=?," +
                        StructuredName.DISPLAY_NAME + "=?," +
                        StructuredName.PHONETIC_NAME_STYLE + "=?" +
                " WHERE " + Data._ID + "=?");

        NameSplitter.Name name = new NameSplitter.Name();
        StringBuilder sb = new StringBuilder();
        Cursor cursor = db.query(StructName205Query.TABLE,
                StructName205Query.COLUMNS,
                DataColumns.MIMETYPE_ID + "=" + mMimeType, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                long dataId = cursor.getLong(StructName205Query.ID);
                long rawContactId = cursor.getLong(StructName205Query.RAW_CONTACT_ID);
                int displayNameSource = cursor.getInt(StructName205Query.DISPLAY_NAME_SOURCE);
                String displayName = cursor.getString(StructName205Query.DISPLAY_NAME);

                name.clear();
                name.prefix = cursor.getString(StructName205Query.PREFIX);
                name.givenNames = cursor.getString(StructName205Query.GIVEN_NAME);
                name.middleName = cursor.getString(StructName205Query.MIDDLE_NAME);
                name.familyName = cursor.getString(StructName205Query.FAMILY_NAME);
                name.suffix = cursor.getString(StructName205Query.SUFFIX);
                name.phoneticFamilyName = cursor.getString(StructName205Query.PHONETIC_FAMILY_NAME);
                name.phoneticMiddleName = cursor.getString(StructName205Query.PHONETIC_MIDDLE_NAME);
                name.phoneticGivenName = cursor.getString(StructName205Query.PHONETIC_GIVEN_NAME);

                upgradeNameToVersion205(dataId, rawContactId, displayNameSource, displayName, name,
                        structuredNameUpdate, rawContactUpdate, splitter, sb);
            }
        } finally {
            cursor.close();
        }
    }

    private void upgradeNameToVersion205(long dataId, long rawContactId, int displayNameSource,
            String currentDisplayName, NameSplitter.Name name,
            SQLiteStatement structuredNameUpdate, SQLiteStatement rawContactUpdate,
            NameSplitter splitter, StringBuilder sb) {

        splitter.guessNameStyle(name);
        int unadjustedFullNameStyle = name.fullNameStyle;
        name.fullNameStyle = splitter.getAdjustedFullNameStyle(name.fullNameStyle);
        String displayName = splitter.join(name, true, true);

        // Don't update database with the adjusted fullNameStyle as it is locale
        // related
        structuredNameUpdate.bindLong(1, unadjustedFullNameStyle);
        DatabaseUtils.bindObjectToProgram(structuredNameUpdate, 2, displayName);
        structuredNameUpdate.bindLong(3, name.phoneticNameStyle);
        structuredNameUpdate.bindLong(4, dataId);
        structuredNameUpdate.execute();

        if (displayNameSource == DisplayNameSources.STRUCTURED_NAME) {
            String displayNameAlternative = splitter.join(name, false, false);
            String phoneticName = splitter.joinPhoneticName(name);
            String sortKey = null;
            String sortKeyAlternative = null;

            if (phoneticName != null) {
                sortKey = sortKeyAlternative = phoneticName;
            } else if (name.fullNameStyle == FullNameStyle.CHINESE ||
                    name.fullNameStyle == FullNameStyle.CJK) {
                sortKey = sortKeyAlternative = displayName;
            }

            if (sortKey == null) {
                sortKey = displayName;
                sortKeyAlternative = displayNameAlternative;
            }

            updateRawContact205(rawContactUpdate, rawContactId, displayName,
                    displayNameAlternative, name.phoneticNameStyle, phoneticName, sortKey,
                    sortKeyAlternative);
        }
    }

    private interface Organization205Query {
        String TABLE = Tables.DATA_JOIN_RAW_CONTACTS;

        String COLUMNS[] = {
                DataColumns.CONCRETE_ID,
                Data.RAW_CONTACT_ID,
                Organization.COMPANY,
                Organization.PHONETIC_NAME,
        };

        int ID = 0;
        int RAW_CONTACT_ID = 1;
        int COMPANY = 2;
        int PHONETIC_NAME = 3;
    }

    private void upgradeOrganizationsToVersion205(SQLiteDatabase db,
            SQLiteStatement rawContactUpdate, NameSplitter splitter) {
        final long mimeType = lookupMimeTypeId(db, Organization.CONTENT_ITEM_TYPE);

        SQLiteStatement organizationUpdate = db.compileStatement(
                "UPDATE " + Tables.DATA +
                " SET " +
                        Organization.PHONETIC_NAME_STYLE + "=?" +
                " WHERE " + Data._ID + "=?");

        Cursor cursor = db.query(Organization205Query.TABLE, Organization205Query.COLUMNS,
                DataColumns.MIMETYPE_ID + "=" + mimeType + " AND "
                        + RawContacts.DISPLAY_NAME_SOURCE + "=" + DisplayNameSources.ORGANIZATION,
                null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                long dataId = cursor.getLong(Organization205Query.ID);
                long rawContactId = cursor.getLong(Organization205Query.RAW_CONTACT_ID);
                String company = cursor.getString(Organization205Query.COMPANY);
                String phoneticName = cursor.getString(Organization205Query.PHONETIC_NAME);

                int phoneticNameStyle = splitter.guessPhoneticNameStyle(phoneticName);

                organizationUpdate.bindLong(1, phoneticNameStyle);
                organizationUpdate.bindLong(2, dataId);
                organizationUpdate.execute();

                String sortKey = company;

                updateRawContact205(rawContactUpdate, rawContactId, company,
                        company, phoneticNameStyle, phoneticName, sortKey, sortKey);
            }
        } finally {
            cursor.close();
        }
    }

    private void updateRawContact205(SQLiteStatement rawContactUpdate, long rawContactId,
            String displayName, String displayNameAlternative, int phoneticNameStyle,
            String phoneticName, String sortKeyPrimary, String sortKeyAlternative) {
        bindString(rawContactUpdate, 1, displayName);
        bindString(rawContactUpdate, 2, displayNameAlternative);
        bindString(rawContactUpdate, 3, phoneticName);
        rawContactUpdate.bindLong(4, phoneticNameStyle);
        bindString(rawContactUpdate, 5, sortKeyPrimary);
        bindString(rawContactUpdate, 6, sortKeyAlternative);
        rawContactUpdate.bindLong(7, rawContactId);
        rawContactUpdate.execute();
    }

    private void upgrateToVersion206(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.RAW_CONTACTS
                + " ADD " + RawContacts.NAME_VERIFIED + " INTEGER NOT NULL DEFAULT 0;");
    }

    /**
     * Fix for the bug where name lookup records for organizations would get removed by
     * unrelated updates of the data rows.
     */
    private void upgradeToVersion300(SQLiteDatabase db) {
        // No longer needed
    }

    private static final class Upgrade303Query {
        public static final String TABLE = Tables.DATA;

        public static final String SELECTION =
                DataColumns.MIMETYPE_ID + "=?" +
                    " AND " + Data._ID + " NOT IN " +
                    "(SELECT " + NameLookupColumns.DATA_ID + " FROM " + Tables.NAME_LOOKUP + ")" +
                    " AND " + Data.DATA1 + " NOT NULL";

        public static final String COLUMNS[] = {
                Data._ID,
                Data.RAW_CONTACT_ID,
                Data.DATA1,
        };

        public static final int ID = 0;
        public static final int RAW_CONTACT_ID = 1;
        public static final int DATA1 = 2;
    }

    /**
     * The {@link ContactsProvider2#update} method was deleting name lookup for new
     * emails during the sync.  We need to restore the lost name lookup rows.
     */
    private void upgradeEmailToVersion303(SQLiteDatabase db) {
        final long mimeTypeId = lookupMimeTypeId(db, Email.CONTENT_ITEM_TYPE);
        if (mimeTypeId == -1) {
            return;
        }

        ContentValues values = new ContentValues();

        // Find all data rows with the mime type "email" that are missing name lookup
        Cursor cursor = db.query(Upgrade303Query.TABLE, Upgrade303Query.COLUMNS,
                Upgrade303Query.SELECTION, new String[] {String.valueOf(mimeTypeId)},
                null, null, null);
        try {
            while (cursor.moveToNext()) {
                long dataId = cursor.getLong(Upgrade303Query.ID);
                long rawContactId = cursor.getLong(Upgrade303Query.RAW_CONTACT_ID);
                String value = cursor.getString(Upgrade303Query.DATA1);
                value = extractHandleFromEmailAddress(value);

                if (value != null) {
                    values.put(NameLookupColumns.DATA_ID, dataId);
                    values.put(NameLookupColumns.RAW_CONTACT_ID, rawContactId);
                    values.put(NameLookupColumns.NAME_TYPE, NameLookupType.EMAIL_BASED_NICKNAME);
                    values.put(NameLookupColumns.NORMALIZED_NAME, NameNormalizer.normalize(value));
                    db.insert(Tables.NAME_LOOKUP, null, values);
                }
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * The {@link ContactsProvider2#update} method was deleting name lookup for new
     * nicknames during the sync.  We need to restore the lost name lookup rows.
     */
    private void upgradeNicknameToVersion303(SQLiteDatabase db) {
        final long mimeTypeId = lookupMimeTypeId(db, Nickname.CONTENT_ITEM_TYPE);
        if (mimeTypeId == -1) {
            return;
        }

        ContentValues values = new ContentValues();

        // Find all data rows with the mime type "nickname" that are missing name lookup
        Cursor cursor = db.query(Upgrade303Query.TABLE, Upgrade303Query.COLUMNS,
                Upgrade303Query.SELECTION, new String[] {String.valueOf(mimeTypeId)},
                null, null, null);
        try {
            while (cursor.moveToNext()) {
                long dataId = cursor.getLong(Upgrade303Query.ID);
                long rawContactId = cursor.getLong(Upgrade303Query.RAW_CONTACT_ID);
                String value = cursor.getString(Upgrade303Query.DATA1);

                values.put(NameLookupColumns.DATA_ID, dataId);
                values.put(NameLookupColumns.RAW_CONTACT_ID, rawContactId);
                values.put(NameLookupColumns.NAME_TYPE, NameLookupType.NICKNAME);
                values.put(NameLookupColumns.NORMALIZED_NAME, NameNormalizer.normalize(value));
                db.insert(Tables.NAME_LOOKUP, null, values);
            }
        } finally {
            cursor.close();
        }
    }

    private void upgradeToVersion304(SQLiteDatabase db) {
        // Mimetype table requires an index on mime type
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS mime_type ON " + Tables.MIMETYPES + " (" +
                MimetypesColumns.MIMETYPE +
        ");");
    }

    private void upgradeToVersion306(SQLiteDatabase db) {
        // Fix invalid lookup that was used for Exchange contacts (it was not escaped)
        // It happened when a new contact was created AND synchronized
        final StringBuilder lookupKeyBuilder = new StringBuilder();
        final SQLiteStatement updateStatement = db.compileStatement(
                "UPDATE contacts " +
                "SET lookup=? " +
                "WHERE _id=?");
        final Cursor contactIdCursor = db.rawQuery(
                "SELECT DISTINCT contact_id " +
                "FROM raw_contacts " +
                "WHERE deleted=0 AND account_type='com.android.exchange'",
                null);
        try {
            while (contactIdCursor.moveToNext()) {
                final long contactId = contactIdCursor.getLong(0);
                lookupKeyBuilder.setLength(0);
                final Cursor c = db.rawQuery(
                        "SELECT account_type, account_name, _id, sourceid, display_name " +
                        "FROM raw_contacts " +
                        "WHERE contact_id=? " +
                        "ORDER BY _id",
                        new String[] { String.valueOf(contactId) });
                try {
                    while (c.moveToNext()) {
                        ContactLookupKey.appendToLookupKey(lookupKeyBuilder,
                                c.getString(0),
                                c.getString(1),
                                c.getLong(2),
                                c.getString(3),
                                c.getString(4));
                    }
                } finally {
                    c.close();
                }

                if (lookupKeyBuilder.length() == 0) {
                    updateStatement.bindNull(1);
                } else {
                    updateStatement.bindString(1, Uri.encode(lookupKeyBuilder.toString()));
                }
                updateStatement.bindLong(2, contactId);

                updateStatement.execute();
            }
        } finally {
            updateStatement.close();
            contactIdCursor.close();
        }
    }

    private void upgradeToVersion307(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE properties (" +
                "property_key TEXT PRIMARY_KEY, " +
                "property_value TEXT" +
        ");");
    }

    private void upgradeToVersion308(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE accounts (" +
                "account_name TEXT, " +
                "account_type TEXT " +
        ");");

        db.execSQL("INSERT INTO accounts " +
                "SELECT DISTINCT account_name, account_type FROM raw_contacts");
    }

    private void upgradeToVersion400(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.GROUPS
                + " ADD " + Groups.FAVORITES + " INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("ALTER TABLE " + Tables.GROUPS
                + " ADD " + Groups.AUTO_ADD + " INTEGER NOT NULL DEFAULT 0;");
    }

    private void upgradeToVersion353(SQLiteDatabase db) {
        db.execSQL("DELETE FROM contacts " +
                "WHERE NOT EXISTS (SELECT 1 FROM raw_contacts WHERE contact_id=contacts._id)");
    }

    private void rebuildNameLookup(SQLiteDatabase db, boolean rebuildSqliteStats) {
        db.execSQL("DROP INDEX IF EXISTS name_lookup_index");
        insertNameLookup(db);
        createContactsIndexes(db, rebuildSqliteStats);
    }

    protected void rebuildSearchIndex() {
        rebuildSearchIndex(getWritableDatabase(), true);
    }

    private void rebuildSearchIndex(SQLiteDatabase db, boolean rebuildSqliteStats) {
        createSearchIndexTable(db, rebuildSqliteStats);
        setProperty(db, SearchIndexManager.PROPERTY_SEARCH_INDEX_VERSION, "0");
    }

    /**
     * Checks whether the current ICU code version matches that used to build
     * the locale specific data in the ContactsDB.
     */
    public boolean needsToUpdateLocaleData(Locale locale) {
        final String dbLocale = getProperty(DbProperties.LOCALE, "");
        if (!dbLocale.equals(locale.toString())) {
            return true;
        }
        final String curICUVersion = ICU.getIcuVersion();
        final String dbICUVersion = getProperty(DbProperties.ICU_VERSION,
                "(unknown)");
        if (!curICUVersion.equals(dbICUVersion)) {
            Log.i(TAG, "ICU version has changed. Current version is "
                    + curICUVersion + "; DB was built with " + dbICUVersion);
            return true;
        }
        return false;
    }

    private void upgradeLocaleData(SQLiteDatabase db, boolean rebuildSqliteStats) {
        final Locale locale = Locale.getDefault();
        Log.i(TAG, "Upgrading locale data for " + locale
                + " (ICU v" + ICU.getIcuVersion() + ")");
        final long start = SystemClock.elapsedRealtime();
        initializeCache(db);
        rebuildLocaleData(db, locale, rebuildSqliteStats);
        Log.i(TAG, "Locale update completed in " + (SystemClock.elapsedRealtime() - start) + "ms");
    }

    private void rebuildLocaleData(SQLiteDatabase db, Locale locale,
            boolean rebuildSqliteStats) {
        db.execSQL("DROP INDEX raw_contact_sort_key1_index");
        db.execSQL("DROP INDEX raw_contact_sort_key2_index");
        db.execSQL("DROP INDEX IF EXISTS name_lookup_index");

        loadNicknameLookupTable(db);
        insertNameLookup(db);
        rebuildSortKeys(db);
        createContactsIndexes(db, rebuildSqliteStats);

        FastScrollingIndexCache.getInstance(mContext).invalidate();
        // Update the ICU version used to generate the locale derived data
        // so we can tell when we need to rebuild with new ICU versions.
        setProperty(db, DbProperties.ICU_VERSION, ICU.getIcuVersion());
        setProperty(db, DbProperties.LOCALE, locale.toString());
    }

    /**
     * Regenerates all locale-sensitive data if needed:
     * nickname_lookup, name_lookup and sort keys. Invalidates the fast
     * scrolling index cache.
     */
    public void setLocale(Locale locale) {
        if (!needsToUpdateLocaleData(locale)) {
            return;
        }
        Log.i(TAG, "Switching to locale " + locale
                + " (ICU v" + ICU.getIcuVersion() + ")");

        final long start = SystemClock.elapsedRealtime();
        SQLiteDatabase db = getWritableDatabase();
        db.setLocale(locale);
        db.beginTransaction();
        try {
            rebuildLocaleData(db, locale, true);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        Log.i(TAG, "Locale change completed in " + (SystemClock.elapsedRealtime() - start) + "ms");
    }

    /**
     * Regenerates sort keys for all contacts.
     */
    private void rebuildSortKeys(SQLiteDatabase db) {
        Cursor cursor = db.query(Tables.RAW_CONTACTS, new String[]{RawContacts._ID},
                null, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                long rawContactId = cursor.getLong(0);
                updateRawContactDisplayName(db, rawContactId);
            }
        } finally {
            cursor.close();
        }
    }

    private void insertNameLookup(SQLiteDatabase db) {
        db.execSQL("DELETE FROM " + Tables.NAME_LOOKUP);

        SQLiteStatement nameLookupInsert = db.compileStatement(
                "INSERT OR IGNORE INTO " + Tables.NAME_LOOKUP + "("
                        + NameLookupColumns.RAW_CONTACT_ID + ","
                        + NameLookupColumns.DATA_ID + ","
                        + NameLookupColumns.NAME_TYPE + ","
                        + NameLookupColumns.NORMALIZED_NAME +
                ") VALUES (?,?,?,?)");

        try {
            insertStructuredNameLookup(db, nameLookupInsert);
            insertEmailLookup(db, nameLookupInsert);
            insertNicknameLookup(db, nameLookupInsert);
        } finally {
            nameLookupInsert.close();
        }
    }

    private static final class StructuredNameQuery {
        public static final String TABLE = Tables.DATA;

        public static final String SELECTION =
                DataColumns.MIMETYPE_ID + "=? AND " + Data.DATA1 + " NOT NULL";

        public static final String COLUMNS[] = {
                StructuredName._ID,
                StructuredName.RAW_CONTACT_ID,
                StructuredName.DISPLAY_NAME,
        };

        public static final int ID = 0;
        public static final int RAW_CONTACT_ID = 1;
        public static final int DISPLAY_NAME = 2;
    }

    private class StructuredNameLookupBuilder extends NameLookupBuilder {

        private final SQLiteStatement mNameLookupInsert;
        private final CommonNicknameCache mCommonNicknameCache;

        public StructuredNameLookupBuilder(NameSplitter splitter,
                CommonNicknameCache commonNicknameCache, SQLiteStatement nameLookupInsert) {
            super(splitter);
            this.mCommonNicknameCache = commonNicknameCache;
            this.mNameLookupInsert = nameLookupInsert;
        }

        @Override
        protected void insertNameLookup(long rawContactId, long dataId, int lookupType,
                String name) {
            if (!TextUtils.isEmpty(name)) {
                ContactsDatabaseHelper.this.insertNormalizedNameLookup(mNameLookupInsert,
                        rawContactId, dataId, lookupType, name);
            }
        }

        @Override
        protected String[] getCommonNicknameClusters(String normalizedName) {
            return mCommonNicknameCache.getCommonNicknameClusters(normalizedName);
        }
    }

    /**
     * Inserts name lookup rows for all structured names in the database.
     */
    private void insertStructuredNameLookup(SQLiteDatabase db, SQLiteStatement nameLookupInsert) {
        NameSplitter nameSplitter = createNameSplitter();
        NameLookupBuilder nameLookupBuilder = new StructuredNameLookupBuilder(nameSplitter,
                new CommonNicknameCache(db), nameLookupInsert);
        final long mimeTypeId = lookupMimeTypeId(db, StructuredName.CONTENT_ITEM_TYPE);
        Cursor cursor = db.query(StructuredNameQuery.TABLE, StructuredNameQuery.COLUMNS,
                StructuredNameQuery.SELECTION, new String[] {String.valueOf(mimeTypeId)},
                null, null, null);
        try {
            while (cursor.moveToNext()) {
                long dataId = cursor.getLong(StructuredNameQuery.ID);
                long rawContactId = cursor.getLong(StructuredNameQuery.RAW_CONTACT_ID);
                String name = cursor.getString(StructuredNameQuery.DISPLAY_NAME);
                int fullNameStyle = nameSplitter.guessFullNameStyle(name);
                fullNameStyle = nameSplitter.getAdjustedFullNameStyle(fullNameStyle);
                nameLookupBuilder.insertNameLookup(rawContactId, dataId, name, fullNameStyle);
            }
        } finally {
            cursor.close();
        }
    }

    private static final class OrganizationQuery {
        public static final String TABLE = Tables.DATA;

        public static final String SELECTION =
                DataColumns.MIMETYPE_ID + "=? AND " + Data.DATA1 + " NOT NULL";

        public static final String COLUMNS[] = {
                Organization._ID,
                Organization.RAW_CONTACT_ID,
                Organization.COMPANY,
                Organization.TITLE,
        };

        public static final int ID = 0;
        public static final int RAW_CONTACT_ID = 1;
        public static final int COMPANY = 2;
        public static final int TITLE = 3;
    }

    private static final class EmailQuery {
        public static final String TABLE = Tables.DATA;

        public static final String SELECTION =
                DataColumns.MIMETYPE_ID + "=? AND " + Data.DATA1 + " NOT NULL";

        public static final String COLUMNS[] = {
                Email._ID,
                Email.RAW_CONTACT_ID,
                Email.ADDRESS,
        };

        public static final int ID = 0;
        public static final int RAW_CONTACT_ID = 1;
        public static final int ADDRESS = 2;
    }

    /**
     * Inserts name lookup rows for all email addresses in the database.
     */
    private void insertEmailLookup(SQLiteDatabase db, SQLiteStatement nameLookupInsert) {
        final long mimeTypeId = lookupMimeTypeId(db, Email.CONTENT_ITEM_TYPE);
        Cursor cursor = db.query(EmailQuery.TABLE, EmailQuery.COLUMNS,
                EmailQuery.SELECTION, new String[] {String.valueOf(mimeTypeId)},
                null, null, null);
        try {
            while (cursor.moveToNext()) {
                long dataId = cursor.getLong(EmailQuery.ID);
                long rawContactId = cursor.getLong(EmailQuery.RAW_CONTACT_ID);
                String address = cursor.getString(EmailQuery.ADDRESS);
                address = extractHandleFromEmailAddress(address);
                insertNameLookup(nameLookupInsert, rawContactId, dataId,
                        NameLookupType.EMAIL_BASED_NICKNAME, address);
            }
        } finally {
            cursor.close();
        }
    }

    private static final class NicknameQuery {
        public static final String TABLE = Tables.DATA;

        public static final String SELECTION =
                DataColumns.MIMETYPE_ID + "=? AND " + Data.DATA1 + " NOT NULL";

        public static final String COLUMNS[] = {
                Nickname._ID,
                Nickname.RAW_CONTACT_ID,
                Nickname.NAME,
        };

        public static final int ID = 0;
        public static final int RAW_CONTACT_ID = 1;
        public static final int NAME = 2;
    }

    /**
     * Inserts name lookup rows for all nicknames in the database.
     */
    private void insertNicknameLookup(SQLiteDatabase db, SQLiteStatement nameLookupInsert) {
        final long mimeTypeId = lookupMimeTypeId(db, Nickname.CONTENT_ITEM_TYPE);
        Cursor cursor = db.query(NicknameQuery.TABLE, NicknameQuery.COLUMNS,
                NicknameQuery.SELECTION, new String[] {String.valueOf(mimeTypeId)},
                null, null, null);
        try {
            while (cursor.moveToNext()) {
                long dataId = cursor.getLong(NicknameQuery.ID);
                long rawContactId = cursor.getLong(NicknameQuery.RAW_CONTACT_ID);
                String nickname = cursor.getString(NicknameQuery.NAME);
                insertNameLookup(nameLookupInsert, rawContactId, dataId,
                        NameLookupType.NICKNAME, nickname);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Inserts a record in the {@link Tables#NAME_LOOKUP} table.
     */
    public void insertNameLookup(SQLiteStatement stmt, long rawContactId, long dataId,
            int lookupType, String name) {
        if (TextUtils.isEmpty(name)) {
            return;
        }

        String normalized = NameNormalizer.normalize(name);
        if (TextUtils.isEmpty(normalized)) {
            return;
        }

        insertNormalizedNameLookup(stmt, rawContactId, dataId, lookupType, normalized);
    }

    private void insertNormalizedNameLookup(SQLiteStatement stmt, long rawContactId, long dataId,
            int lookupType, String normalizedName) {
        stmt.bindLong(1, rawContactId);
        stmt.bindLong(2, dataId);
        stmt.bindLong(3, lookupType);
        stmt.bindString(4, normalizedName);
        stmt.executeInsert();
    }

    /**
     * Changing the VISIBLE bit from a field on both RawContacts and Contacts to a separate table.
     */
    private void upgradeToVersion401(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.VISIBLE_CONTACTS + " (" +
                Contacts._ID + " INTEGER PRIMARY KEY" +
        ");");
        db.execSQL("INSERT INTO " + Tables.VISIBLE_CONTACTS +
                " SELECT " + Contacts._ID +
                " FROM " + Tables.CONTACTS +
                " WHERE " + Contacts.IN_VISIBLE_GROUP + "!=0");
        db.execSQL("DROP INDEX contacts_visible_index");
    }

    /**
     * Introducing a new table: directories.
     */
    private void upgradeToVersion402(SQLiteDatabase db) {
        createDirectoriesTable(db);
    }

    private void upgradeToVersion403(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS directories;");
        createDirectoriesTable(db);

        db.execSQL("ALTER TABLE raw_contacts"
                + " ADD raw_contact_is_read_only INTEGER NOT NULL DEFAULT 0;");

        db.execSQL("ALTER TABLE data"
                + " ADD is_read_only INTEGER NOT NULL DEFAULT 0;");
    }

    private void upgradeToVersion405(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS phone_lookup;");
        // Private phone numbers table used for lookup
        db.execSQL("CREATE TABLE " + Tables.PHONE_LOOKUP + " (" +
                PhoneLookupColumns.DATA_ID
                + " INTEGER REFERENCES data(_id) NOT NULL," +
                PhoneLookupColumns.RAW_CONTACT_ID
                + " INTEGER REFERENCES raw_contacts(_id) NOT NULL," +
                PhoneLookupColumns.NORMALIZED_NUMBER + " TEXT NOT NULL," +
                PhoneLookupColumns.MIN_MATCH + " TEXT NOT NULL" +
        ");");

        db.execSQL("CREATE INDEX phone_lookup_index ON " + Tables.PHONE_LOOKUP + " (" +
                PhoneLookupColumns.NORMALIZED_NUMBER + "," +
                PhoneLookupColumns.RAW_CONTACT_ID + "," +
                PhoneLookupColumns.DATA_ID +
        ");");

        db.execSQL("CREATE INDEX phone_lookup_min_match_index ON " + Tables.PHONE_LOOKUP + " (" +
                PhoneLookupColumns.MIN_MATCH + "," +
                PhoneLookupColumns.RAW_CONTACT_ID + "," +
                PhoneLookupColumns.DATA_ID +
        ");");

        final long mimeTypeId = lookupMimeTypeId(db, Phone.CONTENT_ITEM_TYPE);
        if (mimeTypeId == -1) {
            return;
        }

        Cursor cursor = db.rawQuery(
                    "SELECT _id, " + Phone.RAW_CONTACT_ID + ", " + Phone.NUMBER +
                    " FROM " + Tables.DATA +
                    " WHERE " + DataColumns.MIMETYPE_ID + "=" + mimeTypeId
                            + " AND " + Phone.NUMBER + " NOT NULL", null);

        ContentValues phoneValues = new ContentValues();
        try {
            while (cursor.moveToNext()) {
                long dataID = cursor.getLong(0);
                long rawContactID = cursor.getLong(1);
                String number = cursor.getString(2);
                String normalizedNumber = PhoneNumberUtils.normalizeNumber(number);
                if (!TextUtils.isEmpty(normalizedNumber)) {
                    phoneValues.clear();
                    phoneValues.put(PhoneLookupColumns.RAW_CONTACT_ID, rawContactID);
                    phoneValues.put(PhoneLookupColumns.DATA_ID, dataID);
                    phoneValues.put(PhoneLookupColumns.NORMALIZED_NUMBER, normalizedNumber);
                    phoneValues.put(PhoneLookupColumns.MIN_MATCH,
                            PhoneNumberUtils.toCallerIDMinMatch(normalizedNumber));
                    db.insert(Tables.PHONE_LOOKUP, null, phoneValues);
                }
            }
        } finally {
            cursor.close();
        }
    }

    private void upgradeToVersion406(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD countryiso TEXT;");
    }

    private void upgradeToVersion409(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS directories;");
        createDirectoriesTable(db);
    }

    /**
     * Adding DEFAULT_DIRECTORY table.
     * DEFAULT_DIRECTORY should contain every contact which should be shown to users in default.
     * - if a contact doesn't belong to any account (local contact), it should be in
     *   default_directory
     * - if a contact belongs to an account that doesn't have a "default" group, it should be in
     *   default_directory
     * - if a contact belongs to an account that has a "default" group (like Google directory,
     *   which has "My contacts" group as default), it should be in default_directory.
     *
     * This logic assumes that accounts with the "default" group should have at least one
     * group with AUTO_ADD (implying it is the default group) flag in the groups table.
     */
    private void upgradeToVersion411(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.DEFAULT_DIRECTORY);
        db.execSQL("CREATE TABLE default_directory (_id INTEGER PRIMARY KEY);");

        // Process contacts without an account
        db.execSQL("INSERT OR IGNORE INTO default_directory " +
                " SELECT contact_id " +
                " FROM raw_contacts " +
                " WHERE raw_contacts.account_name IS NULL " +
                "   AND raw_contacts.account_type IS NULL ");

        // Process accounts that don't have a default group (e.g. Exchange).
        db.execSQL("INSERT OR IGNORE INTO default_directory " +
                " SELECT contact_id " +
                " FROM raw_contacts " +
                " WHERE NOT EXISTS" +
                " (SELECT _id " +
                "  FROM groups " +
                "  WHERE raw_contacts.account_name = groups.account_name" +
                "    AND raw_contacts.account_type = groups.account_type" +
                "    AND groups.auto_add != 0)");

        final long mimetype = lookupMimeTypeId(db, GroupMembership.CONTENT_ITEM_TYPE);

        // Process accounts that do have a default group (e.g. Google)
        db.execSQL("INSERT OR IGNORE INTO default_directory " +
                " SELECT contact_id " +
                " FROM raw_contacts " +
                " JOIN data " +
                "   ON (raw_contacts._id=raw_contact_id)" +
                " WHERE mimetype_id=" + mimetype +
                " AND EXISTS" +
                " (SELECT _id" +
                "  FROM groups" +
                "  WHERE raw_contacts.account_name = groups.account_name" +
                "    AND raw_contacts.account_type = groups.account_type" +
                "    AND groups.auto_add != 0)");
    }

    private void upgradeToVersion413(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS directories;");
        createDirectoriesTable(db);
    }

    private void upgradeToVersion415(SQLiteDatabase db) {
        db.execSQL(
                "ALTER TABLE " + Tables.GROUPS +
                " ADD " + Groups.GROUP_IS_READ_ONLY + " INTEGER NOT NULL DEFAULT 0");
        db.execSQL(
                "UPDATE " + Tables.GROUPS +
                "   SET " + Groups.GROUP_IS_READ_ONLY + "=1" +
                " WHERE " + Groups.SYSTEM_ID + " NOT NULL");
    }

    private void upgradeToVersion416(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX phone_lookup_data_id_min_match_index ON " + Tables.PHONE_LOOKUP +
                " (" + PhoneLookupColumns.DATA_ID + ", " + PhoneLookupColumns.MIN_MATCH + ");");
    }

    private void upgradeToVersion501(SQLiteDatabase db) {
        // Remove organization rows from the name lookup, we now use search index for that
        db.execSQL("DELETE FROM name_lookup WHERE name_type=5");
    }

    private void upgradeToVersion502(SQLiteDatabase db) {
        // Remove Chinese and Korean name lookup - this data is now in the search index
        db.execSQL("DELETE FROM name_lookup WHERE name_type IN (6, 7)");
    }

    private void upgradeToVersion504(SQLiteDatabase db) {
        initializeCache(db);

        // Find all names with prefixes and recreate display name
        Cursor cursor = db.rawQuery(
                "SELECT " + StructuredName.RAW_CONTACT_ID +
                " FROM " + Tables.DATA +
                " WHERE " + DataColumns.MIMETYPE_ID + "=?"
                        + " AND " + StructuredName.PREFIX + " NOT NULL",
                new String[]{ String.valueOf(mMimeTypeIdStructuredName) });

        try {
            while(cursor.moveToNext()) {
                long rawContactId = cursor.getLong(0);
                updateRawContactDisplayName(db, rawContactId);
            }

        } finally {
            cursor.close();
        }
    }

    private void upgradeToVersion600(SQLiteDatabase db) {
        // This change used to add the profile raw contact ID to the Accounts table.  That
        // column is no longer needed (as of version 614) since the profile records are stored in
        // a separate copy of the database for security reasons.  So this change is now a no-op.
    }

    private void upgradeToVersion601(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE data_usage_stat(" +
                "stat_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "data_id INTEGER NOT NULL, " +
                "usage_type INTEGER NOT NULL DEFAULT 0, " +
                "times_used INTEGER NOT NULL DEFAULT 0, " +
                "last_time_used INTERGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(data_id) REFERENCES data(_id));");
        db.execSQL("CREATE UNIQUE INDEX data_usage_stat_index ON " +
                "data_usage_stat (data_id, usage_type)");
    }

    private void upgradeToVersion602(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD voicemail_uri TEXT;");
        db.execSQL("ALTER TABLE calls ADD _data TEXT;");
        db.execSQL("ALTER TABLE calls ADD has_content INTEGER;");
        db.execSQL("ALTER TABLE calls ADD mime_type TEXT;");
        db.execSQL("ALTER TABLE calls ADD source_data TEXT;");
        db.execSQL("ALTER TABLE calls ADD source_package TEXT;");
        db.execSQL("ALTER TABLE calls ADD state INTEGER;");
    }

    private void upgradeToVersion604(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE voicemail_status (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "source_package TEXT UNIQUE NOT NULL," +
                "settings_uri TEXT," +
                "voicemail_access_uri TEXT," +
                "configuration_state INTEGER," +
                "data_channel_state INTEGER," +
                "notification_channel_state INTEGER" +
        ");");
    }

    private void upgradeToVersion605(SQLiteDatabase db) {
        // This version used to create the stream item and stream item photos tables, but a newer
        // version of those tables is created in version 609 below.  So omitting the creation in
        // this upgrade step to avoid a create->drop->create.
    }

    private void upgradeToVersion606(SQLiteDatabase db) {
        db.execSQL("DROP VIEW IF EXISTS view_contacts_restricted;");
        db.execSQL("DROP VIEW IF EXISTS view_data_restricted;");
        db.execSQL("DROP VIEW IF EXISTS view_raw_contacts_restricted;");
        db.execSQL("DROP VIEW IF EXISTS view_raw_entities_restricted;");
        db.execSQL("DROP VIEW IF EXISTS view_entities_restricted;");
        db.execSQL("DROP VIEW IF EXISTS view_data_usage_stat_restricted;");
        db.execSQL("DROP INDEX IF EXISTS contacts_restricted_index");

        // We should remove the restricted columns here as well, but unfortunately SQLite doesn't
        // provide ALTER TABLE DROP COLUMN. As they have DEFAULT 0, we can keep but ignore them
    }

    private void upgradeToVersion607(SQLiteDatabase db) {
        // We added "action" and "action_uri" to groups here, but realized this was not a smart
        // move. This upgrade step has been removed (all dogfood phones that executed this step
        // will have those columns, but that shouldn't hurt. Unfortunately, SQLite makes it hard
        // to remove columns)
    }

    private void upgradeToVersion608(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE contacts ADD photo_file_id INTEGER REFERENCES photo_files(_id);");

        db.execSQL("CREATE TABLE photo_files(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "height INTEGER NOT NULL, " +
                "width INTEGER NOT NULL, " +
                "filesize INTEGER NOT NULL);");
    }

    private void upgradeToVersion609(SQLiteDatabase db) {
        // This version used to create the stream item and stream item photos tables, but a newer
        // version of those tables is created in version 613 below.  So omitting the creation in
        // this upgrade step to avoid a create->drop->create.
    }

    private void upgradeToVersion610(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD is_read INTEGER;");
    }

    private void upgradeToVersion611(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE raw_contacts ADD data_set TEXT DEFAULT NULL;");
        db.execSQL("ALTER TABLE groups ADD data_set TEXT DEFAULT NULL;");
        db.execSQL("ALTER TABLE accounts ADD data_set TEXT DEFAULT NULL;");

        db.execSQL("CREATE INDEX raw_contacts_source_id_data_set_index ON raw_contacts " +
                "(sourceid, account_type, account_name, data_set);");

        db.execSQL("CREATE INDEX groups_source_id_data_set_index ON groups " +
                "(sourceid, account_type, account_name, data_set);");
    }

    private void upgradeToVersion612(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD geocoded_location TEXT DEFAULT NULL;");
        // Old calls will not have a geocoded location; new calls will get it when inserted.
    }

    private void upgradeToVersion613(SQLiteDatabase db) {
        // The stream item and stream item photos APIs were not in-use by anyone in the time
        // between their initial creation (in v609) and this update.  So we're just dropping
        // and re-creating them to get appropriate columns.  The delta is as follows:
        // - In stream_items, package_id was replaced by res_package.
        // - In stream_item_photos, picture was replaced by photo_file_id.
        // - Instead of resource ids for icon and label, we use resource name strings now
        // - Added sync columns
        // - Removed action and action_uri
        // - Text and comments are now nullable

        db.execSQL("DROP TABLE IF EXISTS stream_items");
        db.execSQL("DROP TABLE IF EXISTS stream_item_photos");

        db.execSQL("CREATE TABLE stream_items(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "raw_contact_id INTEGER NOT NULL, " +
                "res_package TEXT, " +
                "icon TEXT, " +
                "label TEXT, " +
                "text TEXT, " +
                "timestamp INTEGER NOT NULL, " +
                "comments TEXT, " +
                "stream_item_sync1 TEXT, " +
                "stream_item_sync2 TEXT, " +
                "stream_item_sync3 TEXT, " +
                "stream_item_sync4 TEXT, " +
                "FOREIGN KEY(raw_contact_id) REFERENCES raw_contacts(_id));");

        db.execSQL("CREATE TABLE stream_item_photos(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "stream_item_id INTEGER NOT NULL, " +
                "sort_index INTEGER, " +
                "photo_file_id INTEGER NOT NULL, " +
                "stream_item_photo_sync1 TEXT, " +
                "stream_item_photo_sync2 TEXT, " +
                "stream_item_photo_sync3 TEXT, " +
                "stream_item_photo_sync4 TEXT, " +
                "FOREIGN KEY(stream_item_id) REFERENCES stream_items(_id));");
    }

    private void upgradeToVersion615(SQLiteDatabase db) {
        // Old calls will not have up to date values for these columns, they will be filled in
        // as needed.
        db.execSQL("ALTER TABLE calls ADD lookup_uri TEXT DEFAULT NULL;");
        db.execSQL("ALTER TABLE calls ADD matched_number TEXT DEFAULT NULL;");
        db.execSQL("ALTER TABLE calls ADD normalized_number TEXT DEFAULT NULL;");
        db.execSQL("ALTER TABLE calls ADD photo_id INTEGER NOT NULL DEFAULT 0;");
    }

    private void upgradeToVersion618(SQLiteDatabase db) {
        // The Settings table needs a data_set column which technically should be part of the
        // primary key but can't be because it may be null.  Since SQLite doesn't support nuking
        // the primary key, we'll drop the old table, re-create it, and copy the settings back in.
        db.execSQL("CREATE TEMPORARY TABLE settings_backup(" +
                "account_name STRING NOT NULL," +
                "account_type STRING NOT NULL," +
                "ungrouped_visible INTEGER NOT NULL DEFAULT 0," +
                "should_sync INTEGER NOT NULL DEFAULT 1" +
        ");");
        db.execSQL("INSERT INTO settings_backup " +
                "SELECT account_name, account_type, ungrouped_visible, should_sync" +
                " FROM settings");
        db.execSQL("DROP TABLE settings");
        db.execSQL("CREATE TABLE settings (" +
                "account_name STRING NOT NULL," +
                "account_type STRING NOT NULL," +
                "data_set STRING," +
                "ungrouped_visible INTEGER NOT NULL DEFAULT 0," +
                "should_sync INTEGER NOT NULL DEFAULT 1" +
        ");");
        db.execSQL("INSERT INTO settings " +
                "SELECT account_name, account_type, NULL, ungrouped_visible, should_sync " +
                "FROM settings_backup");
        db.execSQL("DROP TABLE settings_backup");
    }

    private void upgradeToVersion622(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD formatted_number TEXT DEFAULT NULL;");
    }

    private void upgradeToVersion626(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS accounts");

        db.execSQL("CREATE TABLE accounts (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "account_name TEXT, " +
                "account_type TEXT, " +
                "data_set TEXT" +
        ");");

        // Add "account_id" column to groups and raw_contacts
        db.execSQL("ALTER TABLE raw_contacts ADD " +
                "account_id INTEGER REFERENCES accounts(_id)");
        db.execSQL("ALTER TABLE groups ADD " +
                "account_id INTEGER REFERENCES accounts(_id)");

        // Update indexes.
        db.execSQL("DROP INDEX IF EXISTS raw_contacts_source_id_index");
        db.execSQL("DROP INDEX IF EXISTS raw_contacts_source_id_data_set_index");
        db.execSQL("DROP INDEX IF EXISTS groups_source_id_index");
        db.execSQL("DROP INDEX IF EXISTS groups_source_id_data_set_index");

        db.execSQL("CREATE INDEX raw_contacts_source_id_account_id_index ON raw_contacts ("
                + "sourceid, account_id);");
        db.execSQL("CREATE INDEX groups_source_id_account_id_index ON groups ("
                + "sourceid, account_id);");

        // Migrate account_name/account_type/data_set to accounts table

        final Set<AccountWithDataSet> accountsWithDataSets = Sets.newHashSet();
        upgradeToVersion626_findAccountsWithDataSets(accountsWithDataSets, db, "raw_contacts");
        upgradeToVersion626_findAccountsWithDataSets(accountsWithDataSets, db, "groups");

        for (AccountWithDataSet accountWithDataSet : accountsWithDataSets) {
            db.execSQL("INSERT INTO accounts (account_name,account_type,data_set)VALUES(?, ?, ?)",
                    new String[] {
                            accountWithDataSet.getAccountName(),
                            accountWithDataSet.getAccountType(),
                            accountWithDataSet.getDataSet()
                    });
        }
        upgradeToVersion626_fillAccountId(db, "raw_contacts");
        upgradeToVersion626_fillAccountId(db, "groups");
    }

    private static void upgradeToVersion626_findAccountsWithDataSets(
            Set<AccountWithDataSet> result, SQLiteDatabase db, String table) {
        Cursor c = db.rawQuery(
                "SELECT DISTINCT account_name, account_type, data_set FROM " + table, null);
        try {
            while (c.moveToNext()) {
                result.add(AccountWithDataSet.get(c.getString(0), c.getString(1), c.getString(2)));
            }
        } finally {
            c.close();
        }
    }

    private static void upgradeToVersion626_fillAccountId(SQLiteDatabase db, String table) {
        StringBuilder sb = new StringBuilder();

        // Set account_id and null out account_name, account_type and data_set

        sb.append("UPDATE " + table + " SET account_id = (SELECT _id FROM accounts WHERE ");

        addJoinExpressionAllowingNull(sb, table + ".account_name", "accounts.account_name");
        sb.append("AND");
        addJoinExpressionAllowingNull(sb, table + ".account_type", "accounts.account_type");
        sb.append("AND");
        addJoinExpressionAllowingNull(sb, table + ".data_set", "accounts.data_set");

        sb.append("), account_name = null, account_type = null, data_set = null");
        db.execSQL(sb.toString());
    }

    private void upgradeToVersion701(SQLiteDatabase db) {
        db.execSQL("UPDATE raw_contacts SET last_time_contacted =" +
                " max(ifnull(last_time_contacted, 0), " +
                " ifnull((SELECT max(last_time_used) " +
                    " FROM data JOIN data_usage_stat ON (data._id = data_usage_stat.data_id)" +
                    " WHERE data.raw_contact_id = raw_contacts._id), 0))");
        // Replace 0 with null.  This isn't really necessary, but we do this anyway for consistency.
        db.execSQL("UPDATE raw_contacts SET last_time_contacted = null" +
                " where last_time_contacted = 0");
    }

    /**
     * Pre-HC devices don't have correct "NORMALIZED_NUMBERS".  Clear them up.
     */
    private void upgradeToVersion702(SQLiteDatabase db) {
        // All the "correct" Phone.NORMALIZED_NUMBERS should begin with "+".  The upgraded data
        // don't.  Find all Phone.NORMALIZED_NUMBERS that don't begin with "+".
        final int count;
        final long[] dataIds;
        final long[] rawContactIds;
        final String[] phoneNumbers;
        final StringBuilder sbDataIds;
        final Cursor c = db.rawQuery(
                "SELECT _id, raw_contact_id, data1 FROM data " +
                " WHERE mimetype_id=" +
                    "(SELECT _id FROM mimetypes" +
                    " WHERE mimetype='vnd.android.cursor.item/phone_v2')" +
                " AND data4 not like '+%'", // "Not like" will exclude nulls too.
                null);
        try {
            count = c.getCount();
            if (count == 0) {
                return;
            }
            dataIds = new long[count];
            rawContactIds = new long[count];
            phoneNumbers = new String[count];
            sbDataIds = new StringBuilder();

            c.moveToPosition(-1);
            while (c.moveToNext()) {
                final int i = c.getPosition();
                dataIds[i] = c.getLong(0);
                rawContactIds[i] = c.getLong(1);
                phoneNumbers[i] = c.getString(2);

                if (sbDataIds.length() > 0) {
                    sbDataIds.append(",");
                }
                sbDataIds.append(dataIds[i]);
            }
        } finally {
            c.close();
        }

        final String dataIdList = sbDataIds.toString();

        // Then, update the Data and PhoneLookup tables.

        // First, just null out all Phone.NORMALIZED_NUMBERS for those.
        db.execSQL("UPDATE data SET data4 = null" +
                " WHERE _id IN (" + dataIdList + ")");

        // Then, re-create phone_lookup for them.
        db.execSQL("DELETE FROM phone_lookup" +
                " WHERE data_id IN (" + dataIdList + ")");

        for (int i = 0; i < count; i++) {
            // Mimic how DataRowHandlerForPhoneNumber.insert() works when it can't normalize
            // numbers.
            final String phoneNumber = phoneNumbers[i];
            if (TextUtils.isEmpty(phoneNumber)) continue;

            final String normalized = PhoneNumberUtils.normalizeNumber(phoneNumber);
            if (TextUtils.isEmpty(normalized)) continue;

            db.execSQL("INSERT INTO phone_lookup" +
                    "(data_id, raw_contact_id, normalized_number, min_match)" +
                    " VALUES(?,?,?,?)",
                    new String[] {
                        String.valueOf(dataIds[i]),
                        String.valueOf(rawContactIds[i]),
                        normalized,
                        PhoneNumberUtils.toCallerIDMinMatch(normalized)
                    });
        }
    }

    private void upgradeToVersion707(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE raw_contacts ADD phonebook_label TEXT;");
        db.execSQL("ALTER TABLE raw_contacts ADD phonebook_bucket INTEGER;");
        db.execSQL("ALTER TABLE raw_contacts ADD phonebook_label_alt TEXT;");
        db.execSQL("ALTER TABLE raw_contacts ADD phonebook_bucket_alt INTEGER;");
    }

    private void upgradeToVersion710(SQLiteDatabase db) {

        // Adding timestamp to contacts table.
        db.execSQL("ALTER TABLE contacts"
                + " ADD contact_last_updated_timestamp INTEGER;");

        db.execSQL("UPDATE contacts"
                + " SET contact_last_updated_timestamp"
                + " = " + System.currentTimeMillis());

        db.execSQL("CREATE INDEX contacts_contact_last_updated_timestamp_index "
                + "ON contacts(contact_last_updated_timestamp)");

        // New deleted contacts table.
        db.execSQL("CREATE TABLE deleted_contacts (" +
                "contact_id INTEGER PRIMARY KEY," +
                "contact_deleted_timestamp INTEGER NOT NULL default 0"
                + ");");

        db.execSQL("CREATE INDEX deleted_contacts_contact_deleted_timestamp_index "
                + "ON deleted_contacts(contact_deleted_timestamp)");
    }

    private void upgradeToVersion800(SQLiteDatabase db) {
        // Default Calls.PRESENTATION_ALLOWED=1
        db.execSQL("ALTER TABLE calls ADD presentation INTEGER NOT NULL DEFAULT 1;");

        // Re-map CallerInfo.{..}_NUMBER strings to Calls.PRESENTATION_{..} ints
        //  PRIVATE_NUMBER="-2" -> PRESENTATION_RESTRICTED=2
        //  UNKNOWN_NUMBER="-1" -> PRESENTATION_UNKNOWN   =3
        // PAYPHONE_NUMBER="-3" -> PRESENTATION_PAYPHONE  =4
        db.execSQL("UPDATE calls SET presentation=2, number='' WHERE number='-2';");
        db.execSQL("UPDATE calls SET presentation=3, number='' WHERE number='-1';");
        db.execSQL("UPDATE calls SET presentation=4, number='' WHERE number='-3';");
    }

    private void upgradeToVersion802(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE contacts ADD pinned INTEGER NOT NULL DEFAULT " +
                ContactsContract.PinnedPositions.UNPINNED + ";");
        db.execSQL("ALTER TABLE raw_contacts ADD pinned INTEGER NOT NULL DEFAULT  " +
                ContactsContract.PinnedPositions.UNPINNED + ";");
    }

    public String extractHandleFromEmailAddress(String email) {
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(email);
        if (tokens.length == 0) {
            return null;
        }

        String address = tokens[0].getAddress();
        int at = address.indexOf('@');
        if (at != -1) {
            return address.substring(0, at);
        }
        return null;
    }

    public String extractAddressFromEmailAddress(String email) {
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(email);
        if (tokens.length == 0) {
            return null;
        }

        return tokens[0].getAddress().trim();
    }

    private static long lookupMimeTypeId(SQLiteDatabase db, String mimeType) {
        try {
            return DatabaseUtils.longForQuery(db,
                    "SELECT " + MimetypesColumns._ID +
                    " FROM " + Tables.MIMETYPES +
                    " WHERE " + MimetypesColumns.MIMETYPE
                            + "='" + mimeType + "'", null);
        } catch (SQLiteDoneException e) {
            // No rows of this type in the database
            return -1;
        }
    }

    private void bindString(SQLiteStatement stmt, int index, String value) {
        if (value == null) {
            stmt.bindNull(index);
        } else {
            stmt.bindString(index, value);
        }
    }

    private void bindLong(SQLiteStatement stmt, int index, Number value) {
        if (value == null) {
            stmt.bindNull(index);
        } else {
            stmt.bindLong(index, value.longValue());
        }
    }

    /**
     * Add a string like "(((column1) = (column2)) OR ((column1) IS NULL AND (column2) IS NULL))"
     */
    private static StringBuilder addJoinExpressionAllowingNull(StringBuilder sb,
            String column1, String column2) {
        sb.append("(((").append(column1).append(")=(").append(column2);
        sb.append("))OR((");
        sb.append(column1).append(") IS NULL AND (").append(column2).append(") IS NULL))");
        return sb;
    }

    /**
     * Adds index stats into the SQLite database to force it to always use the lookup indexes.
     *
     * Note if you drop a table or an index, the corresponding row will be removed from this table.
     * Make sure to call this method after such operations.
     */
    private void updateSqliteStats(SQLiteDatabase db) {
        if (!mDatabaseOptimizationEnabled) {
            return; // We don't use sqlite_stat1 during tests.
        }

        // Specific stats strings are based on an actual large database after running ANALYZE
        // Important here are relative sizes. Raw-Contacts is slightly bigger than Contacts
        // Warning: Missing tables in here will make SQLite assume to contain 1000000 rows,
        // which can lead to catastrophic query plans for small tables

        // What these numbers mean is described in this file.
        // http://www.sqlite.org/cgi/src/finfo?name=src/analyze.c

        // Excerpt:
        /*
        ** Format of sqlite_stat1:
        **
        ** There is normally one row per index, with the index identified by the
        ** name in the idx column.  The tbl column is the name of the table to
        ** which the index belongs.  In each such row, the stat column will be
        ** a string consisting of a list of integers.  The first integer in this
        ** list is the number of rows in the index and in the table.  The second
        ** integer is the average number of rows in the index that have the same
        ** value in the first column of the index.  The third integer is the average
        ** number of rows in the index that have the same value for the first two
        ** columns.  The N-th integer (for N>1) is the average number of rows in
        ** the index which have the same value for the first N-1 columns.  For
        ** a K-column index, there will be K+1 integers in the stat column.  If
        ** the index is unique, then the last integer will be 1.
        **
        ** The list of integers in the stat column can optionally be followed
        ** by the keyword "unordered".  The "unordered" keyword, if it is present,
        ** must be separated from the last integer by a single space.  If the
        ** "unordered" keyword is present, then the query planner assumes that
        ** the index is unordered and will not use the index for a range query.
        **
        ** If the sqlite_stat1.idx column is NULL, then the sqlite_stat1.stat
        ** column contains a single integer which is the (estimated) number of
        ** rows in the table identified by sqlite_stat1.tbl.
        */

        try {
            db.execSQL("DELETE FROM sqlite_stat1");
            updateIndexStats(db, Tables.CONTACTS,
                    "contacts_has_phone_index", "9000 500");
            updateIndexStats(db, Tables.CONTACTS,
                    "contacts_name_raw_contact_id_index", "9000 1");
            updateIndexStats(db, Tables.CONTACTS, MoreDatabaseUtils.buildIndexName(Tables.CONTACTS,
                    Contacts.CONTACT_LAST_UPDATED_TIMESTAMP), "9000 10");

            updateIndexStats(db, Tables.RAW_CONTACTS,
                    "raw_contacts_contact_id_index", "10000 2");
            updateIndexStats(db, Tables.RAW_CONTACTS,
                    "raw_contact_sort_key2_index", "10000 2");
            updateIndexStats(db, Tables.RAW_CONTACTS,
                    "raw_contact_sort_key1_index", "10000 2");
            updateIndexStats(db, Tables.RAW_CONTACTS,
                    "raw_contacts_source_id_account_id_index", "10000 1 1 1 1");

            updateIndexStats(db, Tables.NAME_LOOKUP,
                    "name_lookup_raw_contact_id_index", "35000 4");
            updateIndexStats(db, Tables.NAME_LOOKUP,
                    "name_lookup_index", "35000 2 2 2 1");
            updateIndexStats(db, Tables.NAME_LOOKUP,
                    "sqlite_autoindex_name_lookup_1", "35000 3 2 1");

            updateIndexStats(db, Tables.PHONE_LOOKUP,
                    "phone_lookup_index", "3500 3 2 1");
            updateIndexStats(db, Tables.PHONE_LOOKUP,
                    "phone_lookup_min_match_index", "3500 3 2 2");
            updateIndexStats(db, Tables.PHONE_LOOKUP,
                    "phone_lookup_data_id_min_match_index", "3500 2 2");

            updateIndexStats(db, Tables.DATA,
                    "data_mimetype_data1_index", "60000 5000 2");
            updateIndexStats(db, Tables.DATA,
                    "data_raw_contact_id", "60000 10");

            updateIndexStats(db, Tables.GROUPS,
                    "groups_source_id_account_id_index", "50 2 2 1 1");

            updateIndexStats(db, Tables.NICKNAME_LOOKUP,
                    "nickname_lookup_index", "500 2 1");

            updateIndexStats(db, Tables.CALLS,
                    null, "250");

            updateIndexStats(db, Tables.STATUS_UPDATES,
                    null, "100");

            updateIndexStats(db, Tables.STREAM_ITEMS,
                    null, "500");
            updateIndexStats(db, Tables.STREAM_ITEM_PHOTOS,
                    null, "50");

            updateIndexStats(db, Tables.VOICEMAIL_STATUS,
                    null, "5");

            updateIndexStats(db, Tables.ACCOUNTS,
                    null, "3");

            updateIndexStats(db, Tables.VISIBLE_CONTACTS,
                    null, "2000");

            updateIndexStats(db, Tables.PHOTO_FILES,
                    null, "50");

            updateIndexStats(db, Tables.DEFAULT_DIRECTORY,
                    null, "1500");

            updateIndexStats(db, Tables.MIMETYPES,
                    "mime_type", "18 1");

            updateIndexStats(db, Tables.DATA_USAGE_STAT,
                    "data_usage_stat_index", "20 2 1");

            // Tiny tables
            updateIndexStats(db, Tables.AGGREGATION_EXCEPTIONS,
                    null, "10");
            updateIndexStats(db, Tables.SETTINGS,
                    null, "10");
            updateIndexStats(db, Tables.PACKAGES,
                    null, "0");
            updateIndexStats(db, Tables.DIRECTORIES,
                    null, "3");
            updateIndexStats(db, LegacyApiSupport.LegacyTables.SETTINGS,
                    null, "0");
            updateIndexStats(db, "android_metadata",
                    null, "1");
            updateIndexStats(db, "_sync_state",
                    "sqlite_autoindex__sync_state_1", "2 1 1");
            updateIndexStats(db, "_sync_state_metadata",
                    null, "1");
            updateIndexStats(db, "properties",
                    "sqlite_autoindex_properties_1", "4 1");

            // Search index
            updateIndexStats(db, "search_index_docsize",
                    null, "9000");
            updateIndexStats(db, "search_index_content",
                    null, "9000");
            updateIndexStats(db, "search_index_stat",
                    null, "1");
            updateIndexStats(db, "search_index_segments",
                    null, "450");
            updateIndexStats(db, "search_index_segdir",
                    "sqlite_autoindex_search_index_segdir_1", "9 5 1");

            // Force sqlite to reload sqlite_stat1.
            db.execSQL("ANALYZE sqlite_master;");
        } catch (SQLException e) {
            Log.e(TAG, "Could not update index stats", e);
        }
    }

    /**
     * Stores statistics for a given index.
     *
     * @param stats has the following structure: the first index is the expected size of
     * the table.  The following integer(s) are the expected number of records selected with the
     * index.  There should be one integer per indexed column.
     */
    private void updateIndexStats(SQLiteDatabase db, String table, String index,
            String stats) {
        if (index == null) {
            db.execSQL("DELETE FROM sqlite_stat1 WHERE tbl=? AND idx IS NULL",
                    new String[] { table });
        } else {
            db.execSQL("DELETE FROM sqlite_stat1 WHERE tbl=? AND idx=?",
                    new String[] { table, index });
        }
        db.execSQL("INSERT INTO sqlite_stat1 (tbl,idx,stat) VALUES (?,?,?)",
                new String[] { table, index, stats });
    }

    /**
     * Wipes all data except mime type and package lookup tables.
     */
    public void wipeData() {
        SQLiteDatabase db = getWritableDatabase();

        db.execSQL("DELETE FROM " + Tables.ACCOUNTS + ";");
        db.execSQL("DELETE FROM " + Tables.CONTACTS + ";");
        db.execSQL("DELETE FROM " + Tables.RAW_CONTACTS + ";");
        db.execSQL("DELETE FROM " + Tables.STREAM_ITEMS + ";");
        db.execSQL("DELETE FROM " + Tables.STREAM_ITEM_PHOTOS + ";");
        db.execSQL("DELETE FROM " + Tables.PHOTO_FILES + ";");
        db.execSQL("DELETE FROM " + Tables.DATA + ";");
        db.execSQL("DELETE FROM " + Tables.PHONE_LOOKUP + ";");
        db.execSQL("DELETE FROM " + Tables.NAME_LOOKUP + ";");
        db.execSQL("DELETE FROM " + Tables.GROUPS + ";");
        db.execSQL("DELETE FROM " + Tables.AGGREGATION_EXCEPTIONS + ";");
        db.execSQL("DELETE FROM " + Tables.SETTINGS + ";");
        db.execSQL("DELETE FROM " + Tables.CALLS + ";");
        db.execSQL("DELETE FROM " + Tables.DIRECTORIES + ";");
        db.execSQL("DELETE FROM " + Tables.SEARCH_INDEX + ";");
        db.execSQL("DELETE FROM " + Tables.DELETED_CONTACTS + ";");

        initializeCache(db);

        // Note: we are not removing reference data from Tables.NICKNAME_LOOKUP
    }

    public NameSplitter createNameSplitter() {
        return createNameSplitter(Locale.getDefault());
    }

    public NameSplitter createNameSplitter(Locale locale) {
        mNameSplitter = new NameSplitter(
                mContext.getString(com.android.internal.R.string.common_name_prefixes),
                mContext.getString(com.android.internal.R.string.common_last_name_prefixes),
                mContext.getString(com.android.internal.R.string.common_name_suffixes),
                mContext.getString(com.android.internal.R.string.common_name_conjunctions),
                locale);
        return mNameSplitter;
    }

    /**
     * Return the {@link ApplicationInfo#uid} for the given package name.
     */
    public static int getUidForPackageName(PackageManager pm, String packageName) {
        try {
            ApplicationInfo clientInfo = pm.getApplicationInfo(packageName, 0 /* no flags */);
            return clientInfo.uid;
        } catch (NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Perform an internal string-to-integer lookup using the compiled
     * {@link SQLiteStatement} provided. If a mapping isn't found in database, it will be
     * created. All new, uncached answers are added to the cache automatically.
     *
     * @param query Compiled statement used to query for the mapping.
     * @param insert Compiled statement used to insert a new mapping when no
     *            existing one is found in cache or from query.
     * @param value Value to find mapping for.
     * @param cache In-memory cache of previous answers.
     * @return An unique integer mapping for the given value.
     */
    private long lookupAndCacheId(SQLiteStatement query, SQLiteStatement insert,
            String value, HashMap<String, Long> cache) {
        long id = -1;
        try {
            // Try searching database for mapping
            DatabaseUtils.bindObjectToProgram(query, 1, value);
            id = query.simpleQueryForLong();
        } catch (SQLiteDoneException e) {
            // Nothing found, so try inserting new mapping
            DatabaseUtils.bindObjectToProgram(insert, 1, value);
            id = insert.executeInsert();
        }
        if (id != -1) {
            // Cache and return the new answer
            cache.put(value, id);
            return id;
        } else {
            // Otherwise throw if no mapping found or created
            throw new IllegalStateException("Couldn't find or create internal "
                    + "lookup table entry for value " + value);
        }
    }

    /**
     * Convert a package name into an integer, using {@link Tables#PACKAGES} for
     * lookups and possible allocation of new IDs as needed.
     */
    public long getPackageId(String packageName) {
        // Try an in-memory cache lookup
        if (mPackageCache.containsKey(packageName)) return mPackageCache.get(packageName);

        final SQLiteStatement packageQuery = getWritableDatabase().compileStatement(
                "SELECT " + PackagesColumns._ID +
                " FROM " + Tables.PACKAGES +
                " WHERE " + PackagesColumns.PACKAGE + "=?");

        final SQLiteStatement packageInsert = getWritableDatabase().compileStatement(
                "INSERT INTO " + Tables.PACKAGES + "("
                        + PackagesColumns.PACKAGE +
                ") VALUES (?)");
        try {
            return lookupAndCacheId(packageQuery, packageInsert, packageName, mPackageCache);
        } finally {
            packageQuery.close();
            packageInsert.close();
        }
    }

    /**
     * Convert a mimetype into an integer, using {@link Tables#MIMETYPES} for
     * lookups and possible allocation of new IDs as needed.
     */
    public long getMimeTypeId(String mimetype) {
        // Try an in-memory cache lookup
        if (mMimetypeCache.containsKey(mimetype)) return mMimetypeCache.get(mimetype);

        return lookupMimeTypeId(mimetype, getWritableDatabase());
    }

    private long lookupMimeTypeId(String mimetype, SQLiteDatabase db) {
        final SQLiteStatement mimetypeQuery = db.compileStatement(
                "SELECT " + MimetypesColumns._ID +
                " FROM " + Tables.MIMETYPES +
                " WHERE " + MimetypesColumns.MIMETYPE + "=?");

        final SQLiteStatement mimetypeInsert = db.compileStatement(
                "INSERT INTO " + Tables.MIMETYPES + "("
                        + MimetypesColumns.MIMETYPE +
                ") VALUES (?)");

        try {
            return lookupAndCacheId(mimetypeQuery, mimetypeInsert, mimetype, mMimetypeCache);
        } finally {
            mimetypeQuery.close();
            mimetypeInsert.close();
        }
    }

    public long getMimeTypeIdForStructuredName() {
        return mMimeTypeIdStructuredName;
    }

    public long getMimeTypeIdForStructuredPostal() {
        return mMimeTypeIdStructuredPostal;
    }

    public long getMimeTypeIdForOrganization() {
        return mMimeTypeIdOrganization;
    }

    public long getMimeTypeIdForIm() {
        return mMimeTypeIdIm;
    }

    public long getMimeTypeIdForEmail() {
        return mMimeTypeIdEmail;
    }

    public long getMimeTypeIdForPhone() {
        return mMimeTypeIdPhone;
    }

    public long getMimeTypeIdForSip() {
        return mMimeTypeIdSip;
    }

    public int getDisplayNameSourceForMimeTypeId(int mimeTypeId) {
        if (mimeTypeId == mMimeTypeIdStructuredName) {
            return DisplayNameSources.STRUCTURED_NAME;
        } else if (mimeTypeId == mMimeTypeIdEmail) {
            return DisplayNameSources.EMAIL;
        } else if (mimeTypeId == mMimeTypeIdPhone) {
            return DisplayNameSources.PHONE;
        } else if (mimeTypeId == mMimeTypeIdOrganization) {
            return DisplayNameSources.ORGANIZATION;
        } else if (mimeTypeId == mMimeTypeIdNickname) {
            return DisplayNameSources.NICKNAME;
        } else {
            return DisplayNameSources.UNDEFINED;
        }
    }

    /**
     * Find the mimetype for the given {@link Data#_ID}.
     */
    public String getDataMimeType(long dataId) {
        if (mDataMimetypeQuery == null) {
            mDataMimetypeQuery = getWritableDatabase().compileStatement(
                    "SELECT " + MimetypesColumns.MIMETYPE +
                    " FROM " + Tables.DATA_JOIN_MIMETYPES +
                    " WHERE " + Tables.DATA + "." + Data._ID + "=?");
        }
        try {
            // Try database query to find mimetype
            DatabaseUtils.bindObjectToProgram(mDataMimetypeQuery, 1, dataId);
            String mimetype = mDataMimetypeQuery.simpleQueryForString();
            return mimetype;
        } catch (SQLiteDoneException e) {
            // No valid mapping found, so return null
            return null;
        }
    }

    public void invalidateAllCache() {
        Log.w(TAG, "invalidateAllCache: [" + getClass().getSimpleName() + "]");

        mMimetypeCache.clear();
        mPackageCache.clear();
    }

    /**
     * Gets all accounts in the accounts table.
     */
    public Set<AccountWithDataSet> getAllAccountsWithDataSets() {
        final Set<AccountWithDataSet> result = Sets.newHashSet();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT DISTINCT " +  AccountsColumns._ID + "," + AccountsColumns.ACCOUNT_NAME +
                "," + AccountsColumns.ACCOUNT_TYPE + "," + AccountsColumns.DATA_SET +
                " FROM " + Tables.ACCOUNTS, null);
        try {
            while (c.moveToNext()) {
                result.add(AccountWithDataSet.get(c.getString(1), c.getString(2), c.getString(3)));
            }
        } finally {
            c.close();
        }
        return result;
    }

    /**
     * @return ID of the specified account, or null if the account doesn't exist.
     */
    public Long getAccountIdOrNull(AccountWithDataSet accountWithDataSet) {
        if (accountWithDataSet == null) {
            accountWithDataSet = AccountWithDataSet.LOCAL;
        }
        final SQLiteStatement select = getWritableDatabase().compileStatement(
                "SELECT " + AccountsColumns._ID +
                " FROM " + Tables.ACCOUNTS +
                " WHERE " +
                "((?1 IS NULL AND " + AccountsColumns.ACCOUNT_NAME + " IS NULL) OR " +
                "(" + AccountsColumns.ACCOUNT_NAME + "=?1)) AND " +
                "((?2 IS NULL AND " + AccountsColumns.ACCOUNT_TYPE + " IS NULL) OR " +
                "(" + AccountsColumns.ACCOUNT_TYPE + "=?2)) AND " +
                "((?3 IS NULL AND " + AccountsColumns.DATA_SET + " IS NULL) OR " +
                "(" + AccountsColumns.DATA_SET + "=?3))");
        try {
            DatabaseUtils.bindObjectToProgram(select, 1, accountWithDataSet.getAccountName());
            DatabaseUtils.bindObjectToProgram(select, 2, accountWithDataSet.getAccountType());
            DatabaseUtils.bindObjectToProgram(select, 3, accountWithDataSet.getDataSet());
            try {
                return select.simpleQueryForLong();
            } catch (SQLiteDoneException notFound) {
                return null;
            }
        } finally {
            select.close();
        }
    }

    /**
     * @return ID of the specified account.  This method will create a record in the accounts table
     *     if the account doesn't exist in the accounts table.
     *
     * This must be used in a transaction, so there's no need for synchronization.
     */
    public long getOrCreateAccountIdInTransaction(AccountWithDataSet accountWithDataSet) {
        if (accountWithDataSet == null) {
            accountWithDataSet = AccountWithDataSet.LOCAL;
        }
        Long id = getAccountIdOrNull(accountWithDataSet);
        if (id != null) {
            return id;
        }
        final SQLiteStatement insert = getWritableDatabase().compileStatement(
                "INSERT INTO " + Tables.ACCOUNTS +
                " (" + AccountsColumns.ACCOUNT_NAME + ", " +
                AccountsColumns.ACCOUNT_TYPE + ", " +
                AccountsColumns.DATA_SET + ") VALUES (?, ?, ?)");
        try {
            DatabaseUtils.bindObjectToProgram(insert, 1, accountWithDataSet.getAccountName());
            DatabaseUtils.bindObjectToProgram(insert, 2, accountWithDataSet.getAccountType());
            DatabaseUtils.bindObjectToProgram(insert, 3, accountWithDataSet.getDataSet());
            id = insert.executeInsert();
        } finally {
            insert.close();
        }

        return id;
    }

    /**
     * Update {@link Contacts#IN_VISIBLE_GROUP} for all contacts.
     */
    public void updateAllVisible() {
        updateCustomContactVisibility(getWritableDatabase(), -1);
    }

    /**
     * Updates contact visibility and return true iff the visibility was actually changed.
     */
    public boolean updateContactVisibleOnlyIfChanged(TransactionContext txContext, long contactId) {
        return updateContactVisible(txContext, contactId, true);
    }

    /**
     * Update {@link Contacts#IN_VISIBLE_GROUP} and
     * {@link Tables#DEFAULT_DIRECTORY} for a specific contact.
     */
    public void updateContactVisible(TransactionContext txContext, long contactId) {
        updateContactVisible(txContext, contactId, false);
    }

    public boolean updateContactVisible(
            TransactionContext txContext, long contactId, boolean onlyIfChanged) {
        SQLiteDatabase db = getWritableDatabase();
        updateCustomContactVisibility(db, contactId);

        String contactIdAsString = String.valueOf(contactId);
        long mimetype = getMimeTypeId(GroupMembership.CONTENT_ITEM_TYPE);

        // The contact will be included in the default directory if contains
        // a raw contact that is in any group or in an account that
        // does not have any AUTO_ADD groups.
        boolean newVisibility = DatabaseUtils.longForQuery(db,
                "SELECT EXISTS (" +
                    "SELECT " + RawContacts.CONTACT_ID +
                    " FROM " + Tables.RAW_CONTACTS +
                    " JOIN " + Tables.DATA +
                    "   ON (" + RawContactsColumns.CONCRETE_ID + "="
                            + Data.RAW_CONTACT_ID + ")" +
                    " WHERE " + RawContacts.CONTACT_ID + "=?1" +
                    "   AND " + DataColumns.MIMETYPE_ID + "=?2" +
                ") OR EXISTS (" +
                    "SELECT " + RawContacts._ID +
                    " FROM " + Tables.RAW_CONTACTS +
                    " WHERE " + RawContacts.CONTACT_ID + "=?1" +
                    "   AND NOT EXISTS" +
                        " (SELECT " + Groups._ID +
                        "  FROM " + Tables.GROUPS +
                        "  WHERE " + RawContactsColumns.CONCRETE_ACCOUNT_ID + " = "
                                + GroupsColumns.CONCRETE_ACCOUNT_ID +
                        "  AND " + Groups.AUTO_ADD + " != 0" +
                        ")" +
                ") OR EXISTS (" +
                    "SELECT " + RawContacts._ID +
                    " FROM " + Tables.RAW_CONTACTS +
                    " WHERE " + RawContacts.CONTACT_ID + "=?1" +
                    "   AND " + RawContactsColumns.CONCRETE_ACCOUNT_ID + "=" +
                        Clauses.LOCAL_ACCOUNT_ID +
                ")",
                new String[] {
                    contactIdAsString,
                    String.valueOf(mimetype)
                }) != 0;

        if (onlyIfChanged) {
            boolean oldVisibility = isContactInDefaultDirectory(db, contactId);
            if (oldVisibility == newVisibility) {
                return false;
            }
        }

        if (newVisibility) {
            db.execSQL("INSERT OR IGNORE INTO " + Tables.DEFAULT_DIRECTORY + " VALUES(?)",
                    new String[] { contactIdAsString });
            txContext.invalidateSearchIndexForContact(contactId);
        } else {
            db.execSQL("DELETE FROM " + Tables.DEFAULT_DIRECTORY +
                        " WHERE " + Contacts._ID + "=?",
                    new String[] { contactIdAsString });
            db.execSQL("DELETE FROM " + Tables.SEARCH_INDEX +
                        " WHERE " + SearchIndexColumns.CONTACT_ID + "=CAST(? AS int)",
                    new String[] { contactIdAsString });
        }
        return true;
    }

    public boolean isContactInDefaultDirectory(SQLiteDatabase db, long contactId) {
        if (mContactInDefaultDirectoryQuery == null) {
            mContactInDefaultDirectoryQuery = db.compileStatement(
                    "SELECT EXISTS (" +
                            "SELECT 1 FROM " + Tables.DEFAULT_DIRECTORY +
                            " WHERE " + Contacts._ID + "=?)");
        }
        mContactInDefaultDirectoryQuery.bindLong(1, contactId);
        return mContactInDefaultDirectoryQuery.simpleQueryForLong() != 0;
    }

    /**
     * Update the visible_contacts table according to the current visibility of contacts, which
     * is defined by {@link Clauses#CONTACT_IS_VISIBLE}.
     *
     * If {@code optionalContactId} is non-negative, it'll update only for the specified contact.
     */
    private void updateCustomContactVisibility(SQLiteDatabase db, long optionalContactId) {
        final long groupMembershipMimetypeId = getMimeTypeId(GroupMembership.CONTENT_ITEM_TYPE);
        String[] selectionArgs = new String[]{String.valueOf(groupMembershipMimetypeId)};

        final String contactIdSelect = (optionalContactId < 0) ? "" :
                (Contacts._ID + "=" + optionalContactId + " AND ");

        // First delete what needs to be deleted, then insert what needs to be added.
        // Since flash writes are very expensive, this approach is much better than
        // delete-all-insert-all.
        db.execSQL(
                "DELETE FROM " + Tables.VISIBLE_CONTACTS +
                " WHERE " + Contacts._ID + " IN" +
                    "(SELECT " + Contacts._ID +
                    " FROM " + Tables.CONTACTS +
                    " WHERE " + contactIdSelect + "(" + Clauses.CONTACT_IS_VISIBLE + ")=0) ",
                selectionArgs);

        db.execSQL(
                "INSERT INTO " + Tables.VISIBLE_CONTACTS +
                " SELECT " + Contacts._ID +
                " FROM " + Tables.CONTACTS +
                " WHERE " +
                    contactIdSelect +
                    Contacts._ID + " NOT IN " + Tables.VISIBLE_CONTACTS +
                    " AND (" + Clauses.CONTACT_IS_VISIBLE + ")=1 ",
                selectionArgs);
    }

    /**
     * Returns contact ID for the given contact or zero if it is NULL.
     */
    public long getContactId(long rawContactId) {
        if (mContactIdQuery == null) {
            mContactIdQuery = getWritableDatabase().compileStatement(
                    "SELECT " + RawContacts.CONTACT_ID +
                    " FROM " + Tables.RAW_CONTACTS +
                    " WHERE " + RawContacts._ID + "=?");
        }
        try {
            DatabaseUtils.bindObjectToProgram(mContactIdQuery, 1, rawContactId);
            return mContactIdQuery.simpleQueryForLong();
        } catch (SQLiteDoneException e) {
            // No valid mapping found, so return 0
            return 0;
        }
    }

    public int getAggregationMode(long rawContactId) {
        if (mAggregationModeQuery == null) {
            mAggregationModeQuery = getWritableDatabase().compileStatement(
                    "SELECT " + RawContacts.AGGREGATION_MODE +
                    " FROM " + Tables.RAW_CONTACTS +
                    " WHERE " + RawContacts._ID + "=?");
        }
        try {
            DatabaseUtils.bindObjectToProgram(mAggregationModeQuery, 1, rawContactId);
            return (int)mAggregationModeQuery.simpleQueryForLong();
        } catch (SQLiteDoneException e) {
            // No valid row found, so return "disabled"
            return RawContacts.AGGREGATION_MODE_DISABLED;
        }
    }

    public void buildPhoneLookupAndContactQuery(
            SQLiteQueryBuilder qb, String normalizedNumber, String numberE164) {
        String minMatch = PhoneNumberUtils.toCallerIDMinMatch(normalizedNumber);
        StringBuilder sb = new StringBuilder();
        appendPhoneLookupTables(sb, minMatch, true);
        qb.setTables(sb.toString());

        sb = new StringBuilder();
        appendPhoneLookupSelection(sb, normalizedNumber, numberE164);
        qb.appendWhere(sb.toString());
    }

    /**
     * Phone lookup method that uses the custom SQLite function phone_number_compare_loose
     * that serves as a fallback in case the regular lookup does not return any results.
     * @param qb The query builder.
     * @param number The phone number to search for.
     */
    public void buildFallbackPhoneLookupAndContactQuery(SQLiteQueryBuilder qb, String number) {
        final String minMatch = PhoneNumberUtils.toCallerIDMinMatch(number);
        final StringBuilder sb = new StringBuilder();
        //append lookup tables
        sb.append(Tables.RAW_CONTACTS);
        sb.append(" JOIN " + Views.CONTACTS + " as contacts_view"
                + " ON (contacts_view._id = " + Tables.RAW_CONTACTS
                + "." + RawContacts.CONTACT_ID + ")" +
                " JOIN (SELECT " + PhoneLookupColumns.DATA_ID + "," +
                PhoneLookupColumns.NORMALIZED_NUMBER + " FROM "+ Tables.PHONE_LOOKUP + " "
                + "WHERE (" + Tables.PHONE_LOOKUP + "." + PhoneLookupColumns.MIN_MATCH + " = '");
        sb.append(minMatch);
        sb.append("')) AS lookup " +
                "ON lookup." + PhoneLookupColumns.DATA_ID + "=" + Tables.DATA + "." + Data._ID
                + " JOIN " + Tables.DATA + " "
                + "ON " + Tables.DATA + "." + Data.RAW_CONTACT_ID + "=" + Tables.RAW_CONTACTS + "."
                + RawContacts._ID);

        qb.setTables(sb.toString());

        sb.setLength(0);
        sb.append("PHONE_NUMBERS_EQUAL(" + Tables.DATA + "." + Phone.NUMBER + ", ");
        DatabaseUtils.appendEscapedSQLString(sb, number);
        sb.append(mUseStrictPhoneNumberComparison ? ", 1)" : ", 0)");
        qb.appendWhere(sb.toString());
    }

    /**
     * Adds query for selecting the contact with the given {@code sipAddress} to the given
     * {@link StringBuilder}.
     *
     * @return the query arguments to be passed in with the query
     */
    public String[] buildSipContactQuery(StringBuilder sb, String sipAddress) {
        sb.append("upper(");
        sb.append(Data.DATA1);
        sb.append(")=upper(?) AND ");
        sb.append(DataColumns.MIMETYPE_ID);
        sb.append("=");
        sb.append(Long.toString(getMimeTypeIdForSip()));
        // Return the arguments to be passed to the query.
        return new String[]{ sipAddress };
    }

    public String buildPhoneLookupAsNestedQuery(String number) {
        StringBuilder sb = new StringBuilder();
        final String minMatch = PhoneNumberUtils.toCallerIDMinMatch(number);
        sb.append("(SELECT DISTINCT raw_contact_id" + " FROM ");
        appendPhoneLookupTables(sb, minMatch, false);
        sb.append(" WHERE ");
        appendPhoneLookupSelection(sb, number, null);
        sb.append(")");
        return sb.toString();
    }

    private void appendPhoneLookupTables(StringBuilder sb, final String minMatch,
            boolean joinContacts) {
        sb.append(Tables.RAW_CONTACTS);
        if (joinContacts) {
            sb.append(" JOIN " + Views.CONTACTS + " contacts_view"
                    + " ON (contacts_view._id = raw_contacts.contact_id)");
        }
        sb.append(", (SELECT data_id, normalized_number, length(normalized_number) as len "
                + " FROM phone_lookup " + " WHERE (" + Tables.PHONE_LOOKUP + "."
                + PhoneLookupColumns.MIN_MATCH + " = '");
        sb.append(minMatch);
        sb.append("')) AS lookup, " + Tables.DATA);
    }

    private void appendPhoneLookupSelection(StringBuilder sb, String number, String numberE164) {
        sb.append("lookup.data_id=data._id AND data.raw_contact_id=raw_contacts._id");
        boolean hasNumberE164 = !TextUtils.isEmpty(numberE164);
        boolean hasNumber = !TextUtils.isEmpty(number);
        if (hasNumberE164 || hasNumber) {
            sb.append(" AND ( ");
            if (hasNumberE164) {
                sb.append(" lookup.normalized_number = ");
                DatabaseUtils.appendEscapedSQLString(sb, numberE164);
            }
            if (hasNumberE164 && hasNumber) {
                sb.append(" OR ");
            }
            if (hasNumber) {
                // skip the suffix match entirely if we are using strict number comparison
                if (!mUseStrictPhoneNumberComparison) {
                    int numberLen = number.length();
                    sb.append(" lookup.len <= ");
                    sb.append(numberLen);
                    sb.append(" AND substr(");
                    DatabaseUtils.appendEscapedSQLString(sb, number);
                    sb.append(',');
                    sb.append(numberLen);
                    sb.append(" - lookup.len + 1) = lookup.normalized_number");

                    // Some countries (e.g. Brazil) can have incoming calls which contain only the local
                    // number (no country calling code and no area code). This case is handled below.
                    // Details see b/5197612.
                    // This also handles a Gingerbread -> ICS upgrade issue; see b/5638376.
                    sb.append(" OR (");
                    sb.append(" lookup.len > ");
                    sb.append(numberLen);
                    sb.append(" AND substr(lookup.normalized_number,");
                    sb.append("lookup.len + 1 - ");
                    sb.append(numberLen);
                    sb.append(") = ");
                    DatabaseUtils.appendEscapedSQLString(sb, number);
                    sb.append(")");
                } else {
                    sb.append("0");
                }
            }
            sb.append(')');
        }
    }

    public String getUseStrictPhoneNumberComparisonParameter() {
        return mUseStrictPhoneNumberComparison ? "1" : "0";
    }

    /**
     * Loads common nickname mappings into the database.
     */
    private void loadNicknameLookupTable(SQLiteDatabase db) {
        db.execSQL("DELETE FROM " + Tables.NICKNAME_LOOKUP);

        String[] strings = mContext.getResources().getStringArray(
                com.android.internal.R.array.common_nicknames);
        if (strings == null || strings.length == 0) {
            return;
        }

        SQLiteStatement nicknameLookupInsert = db.compileStatement("INSERT INTO "
                + Tables.NICKNAME_LOOKUP + "(" + NicknameLookupColumns.NAME + ","
                + NicknameLookupColumns.CLUSTER + ") VALUES (?,?)");

        try {
            for (int clusterId = 0; clusterId < strings.length; clusterId++) {
                String[] names = strings[clusterId].split(",");
                for (int j = 0; j < names.length; j++) {
                    String name = NameNormalizer.normalize(names[j]);
                    try {
                        DatabaseUtils.bindObjectToProgram(nicknameLookupInsert, 1, name);
                        DatabaseUtils.bindObjectToProgram(nicknameLookupInsert, 2,
                                String.valueOf(clusterId));
                        nicknameLookupInsert.executeInsert();
                    } catch (SQLiteException e) {

                        // Print the exception and keep going - this is not a fatal error
                        Log.e(TAG, "Cannot insert nickname: " + names[j], e);
                    }
                }
            }
        } finally {
            nicknameLookupInsert.close();
        }
    }

    public static void copyStringValue(ContentValues toValues, String toKey,
            ContentValues fromValues, String fromKey) {
        if (fromValues.containsKey(fromKey)) {
            toValues.put(toKey, fromValues.getAsString(fromKey));
        }
    }

    public static void copyLongValue(ContentValues toValues, String toKey,
            ContentValues fromValues, String fromKey) {
        if (fromValues.containsKey(fromKey)) {
            long longValue;
            Object value = fromValues.get(fromKey);
            if (value instanceof Boolean) {
                if ((Boolean)value) {
                    longValue = 1;
                } else {
                    longValue = 0;
                }
            } else if (value instanceof String) {
                longValue = Long.parseLong((String)value);
            } else {
                longValue = ((Number)value).longValue();
            }
            toValues.put(toKey, longValue);
        }
    }

    public SyncStateContentProviderHelper getSyncState() {
        return mSyncState;
    }

    /**
     * Returns the value from the {@link Tables#PROPERTIES} table.
     */
    public String getProperty(String key, String defaultValue) {
        return getProperty(getReadableDatabase(), key, defaultValue);
    }

    public String getProperty(SQLiteDatabase db, String key, String defaultValue) {
        Cursor cursor = db.query(Tables.PROPERTIES,
                new String[]{PropertiesColumns.PROPERTY_VALUE},
                PropertiesColumns.PROPERTY_KEY + "=?",
                new String[]{key}, null, null, null);
        String value = null;
        try {
            if (cursor.moveToFirst()) {
                value = cursor.getString(0);
            }
        } finally {
            cursor.close();
        }

        return value != null ? value : defaultValue;
    }

    /**
     * Stores a key-value pair in the {@link Tables#PROPERTIES} table.
     */
    public void setProperty(String key, String value) {
        setProperty(getWritableDatabase(), key, value);
    }

    private void setProperty(SQLiteDatabase db, String key, String value) {
        ContentValues values = new ContentValues();
        values.put(PropertiesColumns.PROPERTY_KEY, key);
        values.put(PropertiesColumns.PROPERTY_VALUE, value);
        db.replace(Tables.PROPERTIES, null, values);
    }

    /**
     * Test if the given column appears in the given projection.
     */
    public static boolean isInProjection(String[] projection, String column) {
        if (projection == null) {
            return true; // Null means "all columns".  We can't really tell if it's in there...
        }
        for (String test : projection) {
            if (column.equals(test)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test if any of the columns appear in the given projection.
     */
    public static boolean isInProjection(String[] projection, String... columns) {
        if (projection == null) {
            return true;
        }

        // Optimized for a single-column test
        if (columns.length == 1) {
            return isInProjection(projection, columns[0]);
        } else {
            for (String test : projection) {
                for (String column : columns) {
                    if (column.equals(test)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns a detailed exception message for the supplied URI.  It includes the calling
     * user and calling package(s).
     */
    public String exceptionMessage(Uri uri) {
        return exceptionMessage(null, uri);
    }

    /**
     * Returns a detailed exception message for the supplied URI.  It includes the calling
     * user and calling package(s).
     */
    public String exceptionMessage(String message, Uri uri) {
        StringBuilder sb = new StringBuilder();
        if (message != null) {
            sb.append(message).append("; ");
        }
        sb.append("URI: ").append(uri);
        final PackageManager pm = mContext.getPackageManager();
        int callingUid = Binder.getCallingUid();
        sb.append(", calling user: ");
        String userName = pm.getNameForUid(callingUid);
        if (userName != null) {
            sb.append(userName);
        } else {
            sb.append(callingUid);
        }

        final String[] callerPackages = pm.getPackagesForUid(callingUid);
        if (callerPackages != null && callerPackages.length > 0) {
            if (callerPackages.length == 1) {
                sb.append(", calling package:");
                sb.append(callerPackages[0]);
            } else {
                sb.append(", calling package is one of: [");
                for (int i = 0; i < callerPackages.length; i++) {
                    if (i != 0) {
                        sb.append(", ");
                    }
                    sb.append(callerPackages[i]);
                }
                sb.append("]");
            }
        }

        return sb.toString();
    }

    public void deleteStatusUpdate(long dataId) {
        if (mStatusUpdateDelete == null) {
            mStatusUpdateDelete = getWritableDatabase().compileStatement(
                    "DELETE FROM " + Tables.STATUS_UPDATES +
                    " WHERE " + StatusUpdatesColumns.DATA_ID + "=?");
        }
        mStatusUpdateDelete.bindLong(1, dataId);
        mStatusUpdateDelete.execute();
    }

    public void replaceStatusUpdate(Long dataId, long timestamp, String status, String resPackage,
            Integer iconResource, Integer labelResource) {
        if (mStatusUpdateReplace == null) {
            mStatusUpdateReplace = getWritableDatabase().compileStatement(
                    "INSERT OR REPLACE INTO " + Tables.STATUS_UPDATES + "("
                            + StatusUpdatesColumns.DATA_ID + ", "
                            + StatusUpdates.STATUS_TIMESTAMP + ","
                            + StatusUpdates.STATUS + ","
                            + StatusUpdates.STATUS_RES_PACKAGE + ","
                            + StatusUpdates.STATUS_ICON + ","
                            + StatusUpdates.STATUS_LABEL + ")" +
                    " VALUES (?,?,?,?,?,?)");
        }
        mStatusUpdateReplace.bindLong(1, dataId);
        mStatusUpdateReplace.bindLong(2, timestamp);
        bindString(mStatusUpdateReplace, 3, status);
        bindString(mStatusUpdateReplace, 4, resPackage);
        bindLong(mStatusUpdateReplace, 5, iconResource);
        bindLong(mStatusUpdateReplace, 6, labelResource);
        mStatusUpdateReplace.execute();
    }

    public void insertStatusUpdate(Long dataId, String status, String resPackage,
            Integer iconResource, Integer labelResource) {
        if (mStatusUpdateInsert == null) {
            mStatusUpdateInsert = getWritableDatabase().compileStatement(
                    "INSERT INTO " + Tables.STATUS_UPDATES + "("
                            + StatusUpdatesColumns.DATA_ID + ", "
                            + StatusUpdates.STATUS + ","
                            + StatusUpdates.STATUS_RES_PACKAGE + ","
                            + StatusUpdates.STATUS_ICON + ","
                            + StatusUpdates.STATUS_LABEL + ")" +
                    " VALUES (?,?,?,?,?)");
        }
        try {
            mStatusUpdateInsert.bindLong(1, dataId);
            bindString(mStatusUpdateInsert, 2, status);
            bindString(mStatusUpdateInsert, 3, resPackage);
            bindLong(mStatusUpdateInsert, 4, iconResource);
            bindLong(mStatusUpdateInsert, 5, labelResource);
            mStatusUpdateInsert.executeInsert();
        } catch (SQLiteConstraintException e) {
            // The row already exists - update it
            if (mStatusUpdateAutoTimestamp == null) {
                mStatusUpdateAutoTimestamp = getWritableDatabase().compileStatement(
                        "UPDATE " + Tables.STATUS_UPDATES +
                        " SET " + StatusUpdates.STATUS_TIMESTAMP + "=?,"
                                + StatusUpdates.STATUS + "=?" +
                        " WHERE " + StatusUpdatesColumns.DATA_ID + "=?"
                                + " AND " + StatusUpdates.STATUS + "!=?");
            }

            long timestamp = System.currentTimeMillis();
            mStatusUpdateAutoTimestamp.bindLong(1, timestamp);
            bindString(mStatusUpdateAutoTimestamp, 2, status);
            mStatusUpdateAutoTimestamp.bindLong(3, dataId);
            bindString(mStatusUpdateAutoTimestamp, 4, status);
            mStatusUpdateAutoTimestamp.execute();

            if (mStatusAttributionUpdate == null) {
                mStatusAttributionUpdate = getWritableDatabase().compileStatement(
                        "UPDATE " + Tables.STATUS_UPDATES +
                        " SET " + StatusUpdates.STATUS_RES_PACKAGE + "=?,"
                                + StatusUpdates.STATUS_ICON + "=?,"
                                + StatusUpdates.STATUS_LABEL + "=?" +
                        " WHERE " + StatusUpdatesColumns.DATA_ID + "=?");
            }
            bindString(mStatusAttributionUpdate, 1, resPackage);
            bindLong(mStatusAttributionUpdate, 2, iconResource);
            bindLong(mStatusAttributionUpdate, 3, labelResource);
            mStatusAttributionUpdate.bindLong(4, dataId);
            mStatusAttributionUpdate.execute();
        }
    }

    /**
     * Resets the {@link RawContacts#NAME_VERIFIED} flag to 0 on all other raw
     * contacts in the same aggregate
     */
    public void resetNameVerifiedForOtherRawContacts(long rawContactId) {
        if (mResetNameVerifiedForOtherRawContacts == null) {
            mResetNameVerifiedForOtherRawContacts = getWritableDatabase().compileStatement(
                    "UPDATE " + Tables.RAW_CONTACTS +
                    " SET " + RawContacts.NAME_VERIFIED + "=0" +
                    " WHERE " + RawContacts.CONTACT_ID + "=(" +
                            "SELECT " + RawContacts.CONTACT_ID +
                            " FROM " + Tables.RAW_CONTACTS +
                            " WHERE " + RawContacts._ID + "=?)" +
                    " AND " + RawContacts._ID + "!=?");
        }
        mResetNameVerifiedForOtherRawContacts.bindLong(1, rawContactId);
        mResetNameVerifiedForOtherRawContacts.bindLong(2, rawContactId);
        mResetNameVerifiedForOtherRawContacts.execute();
    }

    private interface RawContactNameQuery {
        public static final String RAW_SQL =
                "SELECT "
                        + DataColumns.MIMETYPE_ID + ","
                        + Data.IS_PRIMARY + ","
                        + Data.DATA1 + ","
                        + Data.DATA2 + ","
                        + Data.DATA3 + ","
                        + Data.DATA4 + ","
                        + Data.DATA5 + ","
                        + Data.DATA6 + ","
                        + Data.DATA7 + ","
                        + Data.DATA8 + ","
                        + Data.DATA9 + ","
                        + Data.DATA10 + ","
                        + Data.DATA11 +
                " FROM " + Tables.DATA +
                " WHERE " + Data.RAW_CONTACT_ID + "=?" +
                        " AND (" + Data.DATA1 + " NOT NULL OR " +
                                Data.DATA8 + " NOT NULL OR " +
                                Data.DATA9 + " NOT NULL OR " +
                                Data.DATA10 + " NOT NULL OR " +  // Phonetic name not empty
                                Organization.TITLE + " NOT NULL)";

        public static final int MIMETYPE = 0;
        public static final int IS_PRIMARY = 1;
        public static final int DATA1 = 2;
        public static final int GIVEN_NAME = 3;                         // data2
        public static final int FAMILY_NAME = 4;                        // data3
        public static final int PREFIX = 5;                             // data4
        public static final int TITLE = 5;                              // data4
        public static final int MIDDLE_NAME = 6;                        // data5
        public static final int SUFFIX = 7;                             // data6
        public static final int PHONETIC_GIVEN_NAME = 8;                // data7
        public static final int PHONETIC_MIDDLE_NAME = 9;               // data8
        public static final int ORGANIZATION_PHONETIC_NAME = 9;         // data8
        public static final int PHONETIC_FAMILY_NAME = 10;              // data9
        public static final int FULL_NAME_STYLE = 11;                   // data10
        public static final int ORGANIZATION_PHONETIC_NAME_STYLE = 11;  // data10
        public static final int PHONETIC_NAME_STYLE = 12;               // data11
    }

    /**
     * Updates a raw contact display name based on data rows, e.g. structured name,
     * organization, email etc.
     */
    public void updateRawContactDisplayName(SQLiteDatabase db, long rawContactId) {
        if (mNameSplitter == null) {
            createNameSplitter();
        }

        int bestDisplayNameSource = DisplayNameSources.UNDEFINED;
        NameSplitter.Name bestName = null;
        String bestDisplayName = null;
        String bestPhoneticName = null;
        int bestPhoneticNameStyle = PhoneticNameStyle.UNDEFINED;

        mSelectionArgs1[0] = String.valueOf(rawContactId);
        Cursor c = db.rawQuery(RawContactNameQuery.RAW_SQL, mSelectionArgs1);
        try {
            while (c.moveToNext()) {
                int mimeType = c.getInt(RawContactNameQuery.MIMETYPE);
                int source = getDisplayNameSourceForMimeTypeId(mimeType);
                if (source < bestDisplayNameSource || source == DisplayNameSources.UNDEFINED) {
                    continue;
                }

                if (source == bestDisplayNameSource
                        && c.getInt(RawContactNameQuery.IS_PRIMARY) == 0) {
                    continue;
                }

                if (mimeType == getMimeTypeIdForStructuredName()) {
                    NameSplitter.Name name;
                    if (bestName != null) {
                        name = new NameSplitter.Name();
                    } else {
                        name = mName;
                        name.clear();
                    }
                    name.prefix = c.getString(RawContactNameQuery.PREFIX);
                    name.givenNames = c.getString(RawContactNameQuery.GIVEN_NAME);
                    name.middleName = c.getString(RawContactNameQuery.MIDDLE_NAME);
                    name.familyName = c.getString(RawContactNameQuery.FAMILY_NAME);
                    name.suffix = c.getString(RawContactNameQuery.SUFFIX);
                    name.fullNameStyle = c.isNull(RawContactNameQuery.FULL_NAME_STYLE)
                            ? FullNameStyle.UNDEFINED
                            : c.getInt(RawContactNameQuery.FULL_NAME_STYLE);
                    name.phoneticFamilyName = c.getString(RawContactNameQuery.PHONETIC_FAMILY_NAME);
                    name.phoneticMiddleName = c.getString(RawContactNameQuery.PHONETIC_MIDDLE_NAME);
                    name.phoneticGivenName = c.getString(RawContactNameQuery.PHONETIC_GIVEN_NAME);
                    name.phoneticNameStyle = c.isNull(RawContactNameQuery.PHONETIC_NAME_STYLE)
                            ? PhoneticNameStyle.UNDEFINED
                            : c.getInt(RawContactNameQuery.PHONETIC_NAME_STYLE);
                    if (!name.isEmpty()) {
                        bestDisplayNameSource = source;
                        bestName = name;
                    }
                } else if (mimeType == getMimeTypeIdForOrganization()) {
                    mCharArrayBuffer.sizeCopied = 0;
                    c.copyStringToBuffer(RawContactNameQuery.DATA1, mCharArrayBuffer);
                    if (mCharArrayBuffer.sizeCopied != 0) {
                        bestDisplayNameSource = source;
                        bestDisplayName = new String(mCharArrayBuffer.data, 0,
                                mCharArrayBuffer.sizeCopied);
                        bestPhoneticName = c.getString(
                                RawContactNameQuery.ORGANIZATION_PHONETIC_NAME);
                        bestPhoneticNameStyle =
                                c.isNull(RawContactNameQuery.ORGANIZATION_PHONETIC_NAME_STYLE)
                                   ? PhoneticNameStyle.UNDEFINED
                                   : c.getInt(RawContactNameQuery.ORGANIZATION_PHONETIC_NAME_STYLE);
                    } else {
                        c.copyStringToBuffer(RawContactNameQuery.TITLE, mCharArrayBuffer);
                        if (mCharArrayBuffer.sizeCopied != 0) {
                            bestDisplayNameSource = source;
                            bestDisplayName = new String(mCharArrayBuffer.data, 0,
                                    mCharArrayBuffer.sizeCopied);
                            bestPhoneticName = null;
                            bestPhoneticNameStyle = PhoneticNameStyle.UNDEFINED;
                        }
                    }
                } else {
                    // Display name is at DATA1 in all other types.
                    // This is ensured in the constructor.

                    mCharArrayBuffer.sizeCopied = 0;
                    c.copyStringToBuffer(RawContactNameQuery.DATA1, mCharArrayBuffer);
                    if (mCharArrayBuffer.sizeCopied != 0) {
                        bestDisplayNameSource = source;
                        bestDisplayName = new String(mCharArrayBuffer.data, 0,
                                mCharArrayBuffer.sizeCopied);
                        bestPhoneticName = null;
                        bestPhoneticNameStyle = PhoneticNameStyle.UNDEFINED;
                    }
                }
            }

        } finally {
            c.close();
        }

        String displayNamePrimary;
        String displayNameAlternative;
        String sortNamePrimary;
        String sortNameAlternative;
        String sortKeyPrimary = null;
        String sortKeyAlternative = null;
        int displayNameStyle = FullNameStyle.UNDEFINED;

        if (bestDisplayNameSource == DisplayNameSources.STRUCTURED_NAME) {
            displayNameStyle = bestName.fullNameStyle;
            if (displayNameStyle == FullNameStyle.CJK
                    || displayNameStyle == FullNameStyle.UNDEFINED) {
                displayNameStyle = mNameSplitter.getAdjustedFullNameStyle(displayNameStyle);
                bestName.fullNameStyle = displayNameStyle;
            }

            displayNamePrimary = mNameSplitter.join(bestName, true, true);
            displayNameAlternative = mNameSplitter.join(bestName, false, true);

            if (TextUtils.isEmpty(bestName.prefix)) {
                sortNamePrimary = displayNamePrimary;
                sortNameAlternative = displayNameAlternative;
            } else {
                sortNamePrimary = mNameSplitter.join(bestName, true, false);
                sortNameAlternative = mNameSplitter.join(bestName, false, false);
            }

            bestPhoneticName = mNameSplitter.joinPhoneticName(bestName);
            bestPhoneticNameStyle = bestName.phoneticNameStyle;
        } else {
            displayNamePrimary = displayNameAlternative = bestDisplayName;
            sortNamePrimary = sortNameAlternative = bestDisplayName;
        }

        if (bestPhoneticName != null) {
            if (displayNamePrimary == null) {
                displayNamePrimary = bestPhoneticName;
            }
            if (displayNameAlternative == null) {
                displayNameAlternative = bestPhoneticName;
            }
            // Phonetic names disregard name order so displayNamePrimary and displayNameAlternative
            // are the same.
            sortKeyPrimary = sortKeyAlternative = bestPhoneticName;
            if (bestPhoneticNameStyle == PhoneticNameStyle.UNDEFINED) {
                bestPhoneticNameStyle = mNameSplitter.guessPhoneticNameStyle(bestPhoneticName);
            }
        } else {
            bestPhoneticNameStyle = PhoneticNameStyle.UNDEFINED;
            if (displayNameStyle == FullNameStyle.UNDEFINED) {
                displayNameStyle = mNameSplitter.guessFullNameStyle(bestDisplayName);
                if (displayNameStyle == FullNameStyle.UNDEFINED
                        || displayNameStyle == FullNameStyle.CJK) {
                    displayNameStyle = mNameSplitter.getAdjustedNameStyleBasedOnPhoneticNameStyle(
                            displayNameStyle, bestPhoneticNameStyle);
                }
                displayNameStyle = mNameSplitter.getAdjustedFullNameStyle(displayNameStyle);
            }
            if (displayNameStyle == FullNameStyle.CHINESE ||
                    displayNameStyle == FullNameStyle.CJK) {
                sortKeyPrimary = sortKeyAlternative = sortNamePrimary;
            }
        }

        if (sortKeyPrimary == null) {
            sortKeyPrimary = sortNamePrimary;
            sortKeyAlternative = sortNameAlternative;
        }

        String phonebookLabelPrimary = "";
        String phonebookLabelAlternative = "";
        int phonebookBucketPrimary = 0;
        int phonebookBucketAlternative = 0;
        ContactLocaleUtils localeUtils = ContactLocaleUtils.getInstance();

        if (sortKeyPrimary != null) {
            phonebookBucketPrimary = localeUtils.getBucketIndex(sortKeyPrimary);
            phonebookLabelPrimary = localeUtils.getBucketLabel(phonebookBucketPrimary);
        }
        if (sortKeyAlternative != null) {
            phonebookBucketAlternative = localeUtils.getBucketIndex(sortKeyAlternative);
            phonebookLabelAlternative = localeUtils.getBucketLabel(phonebookBucketAlternative);
        }

        if (mRawContactDisplayNameUpdate == null) {
            mRawContactDisplayNameUpdate = db.compileStatement(
                    "UPDATE " + Tables.RAW_CONTACTS +
                    " SET " +
                            RawContacts.DISPLAY_NAME_SOURCE + "=?," +
                            RawContacts.DISPLAY_NAME_PRIMARY + "=?," +
                            RawContacts.DISPLAY_NAME_ALTERNATIVE + "=?," +
                            RawContacts.PHONETIC_NAME + "=?," +
                            RawContacts.PHONETIC_NAME_STYLE + "=?," +
                            RawContacts.SORT_KEY_PRIMARY + "=?," +
                            RawContactsColumns.PHONEBOOK_LABEL_PRIMARY + "=?," +
                            RawContactsColumns.PHONEBOOK_BUCKET_PRIMARY + "=?," +
                            RawContacts.SORT_KEY_ALTERNATIVE + "=?," +
                            RawContactsColumns.PHONEBOOK_LABEL_ALTERNATIVE + "=?," +
                            RawContactsColumns.PHONEBOOK_BUCKET_ALTERNATIVE + "=?" +
                    " WHERE " + RawContacts._ID + "=?");
        }

        mRawContactDisplayNameUpdate.bindLong(1, bestDisplayNameSource);
        bindString(mRawContactDisplayNameUpdate, 2, displayNamePrimary);
        bindString(mRawContactDisplayNameUpdate, 3, displayNameAlternative);
        bindString(mRawContactDisplayNameUpdate, 4, bestPhoneticName);
        mRawContactDisplayNameUpdate.bindLong(5, bestPhoneticNameStyle);
        bindString(mRawContactDisplayNameUpdate, 6, sortKeyPrimary);
        bindString(mRawContactDisplayNameUpdate, 7, phonebookLabelPrimary);
        mRawContactDisplayNameUpdate.bindLong(8, phonebookBucketPrimary);
        bindString(mRawContactDisplayNameUpdate, 9, sortKeyAlternative);
        bindString(mRawContactDisplayNameUpdate, 10, phonebookLabelAlternative);
        mRawContactDisplayNameUpdate.bindLong(11, phonebookBucketAlternative);
        mRawContactDisplayNameUpdate.bindLong(12, rawContactId);
        mRawContactDisplayNameUpdate.execute();
    }

    /*
     * Sets the given dataId record in the "data" table to primary, and resets all data records of
     * the same mimetype and under the same contact to not be primary.
     *
     * @param dataId the id of the data record to be set to primary. Pass -1 to clear the primary
     * flag of all data items of this raw contacts
     */
    public void setIsPrimary(long rawContactId, long dataId, long mimeTypeId) {
        if (mSetPrimaryStatement == null) {
            mSetPrimaryStatement = getWritableDatabase().compileStatement(
                    "UPDATE " + Tables.DATA +
                    " SET " + Data.IS_PRIMARY + "=(_id=?)" +
                    " WHERE " + DataColumns.MIMETYPE_ID + "=?" +
                    "   AND " + Data.RAW_CONTACT_ID + "=?");
        }
        mSetPrimaryStatement.bindLong(1, dataId);
        mSetPrimaryStatement.bindLong(2, mimeTypeId);
        mSetPrimaryStatement.bindLong(3, rawContactId);
        mSetPrimaryStatement.execute();
    }

    /*
     * Clears the super primary of all data items of the given raw contact. does not touch
     * other raw contacts of the same joined aggregate
     */
    public void clearSuperPrimary(long rawContactId, long mimeTypeId) {
        if (mClearSuperPrimaryStatement == null) {
            mClearSuperPrimaryStatement = getWritableDatabase().compileStatement(
                    "UPDATE " + Tables.DATA +
                    " SET " + Data.IS_SUPER_PRIMARY + "=0" +
                    " WHERE " + DataColumns.MIMETYPE_ID + "=?" +
                    "   AND " + Data.RAW_CONTACT_ID + "=?");
        }
        mClearSuperPrimaryStatement.bindLong(1, mimeTypeId);
        mClearSuperPrimaryStatement.bindLong(2, rawContactId);
        mClearSuperPrimaryStatement.execute();
    }

    /*
     * Sets the given dataId record in the "data" table to "super primary", and resets all data
     * records of the same mimetype and under the same aggregate to not be "super primary".
     *
     * @param dataId the id of the data record to be set to primary.
     */
    public void setIsSuperPrimary(long rawContactId, long dataId, long mimeTypeId) {
        if (mSetSuperPrimaryStatement == null) {
            mSetSuperPrimaryStatement = getWritableDatabase().compileStatement(
                    "UPDATE " + Tables.DATA +
                    " SET " + Data.IS_SUPER_PRIMARY + "=(" + Data._ID + "=?)" +
                    " WHERE " + DataColumns.MIMETYPE_ID + "=?" +
                    "   AND " + Data.RAW_CONTACT_ID + " IN (" +
                            "SELECT " + RawContacts._ID +
                            " FROM " + Tables.RAW_CONTACTS +
                            " WHERE " + RawContacts.CONTACT_ID + " =(" +
                                    "SELECT " + RawContacts.CONTACT_ID +
                                    " FROM " + Tables.RAW_CONTACTS +
                                    " WHERE " + RawContacts._ID + "=?))");
        }
        mSetSuperPrimaryStatement.bindLong(1, dataId);
        mSetSuperPrimaryStatement.bindLong(2, mimeTypeId);
        mSetSuperPrimaryStatement.bindLong(3, rawContactId);
        mSetSuperPrimaryStatement.execute();
    }

    /**
     * Inserts a record in the {@link Tables#NAME_LOOKUP} table.
     */
    public void insertNameLookup(long rawContactId, long dataId, int lookupType, String name) {
        if (TextUtils.isEmpty(name)) {
            return;
        }

        if (mNameLookupInsert == null) {
            mNameLookupInsert = getWritableDatabase().compileStatement(
                    "INSERT OR IGNORE INTO " + Tables.NAME_LOOKUP + "("
                            + NameLookupColumns.RAW_CONTACT_ID + ","
                            + NameLookupColumns.DATA_ID + ","
                            + NameLookupColumns.NAME_TYPE + ","
                            + NameLookupColumns.NORMALIZED_NAME
                    + ") VALUES (?,?,?,?)");
        }
        mNameLookupInsert.bindLong(1, rawContactId);
        mNameLookupInsert.bindLong(2, dataId);
        mNameLookupInsert.bindLong(3, lookupType);
        bindString(mNameLookupInsert, 4, name);
        mNameLookupInsert.executeInsert();
    }

    /**
     * Deletes all {@link Tables#NAME_LOOKUP} table rows associated with the specified data element.
     */
    public void deleteNameLookup(long dataId) {
        if (mNameLookupDelete == null) {
            mNameLookupDelete = getWritableDatabase().compileStatement(
                    "DELETE FROM " + Tables.NAME_LOOKUP +
                    " WHERE " + NameLookupColumns.DATA_ID + "=?");
        }
        mNameLookupDelete.bindLong(1, dataId);
        mNameLookupDelete.execute();
    }

    public String insertNameLookupForEmail(long rawContactId, long dataId, String email) {
        if (TextUtils.isEmpty(email)) {
            return null;
        }

        String address = extractHandleFromEmailAddress(email);
        if (address == null) {
            return null;
        }

        insertNameLookup(rawContactId, dataId,
                NameLookupType.EMAIL_BASED_NICKNAME, NameNormalizer.normalize(address));
        return address;
    }

    /**
     * Normalizes the nickname and inserts it in the name lookup table.
     */
    public void insertNameLookupForNickname(long rawContactId, long dataId, String nickname) {
        if (TextUtils.isEmpty(nickname)) {
            return;
        }

        insertNameLookup(rawContactId, dataId,
                NameLookupType.NICKNAME, NameNormalizer.normalize(nickname));
    }

    public void insertNameLookupForPhoneticName(long rawContactId, long dataId, String familyName,
            String middleName, String givenName) {
        mSb.setLength(0);
        if (familyName != null) {
            mSb.append(familyName.trim());
        }
        if (middleName != null) {
            mSb.append(middleName.trim());
        }
        if (givenName != null) {
            mSb.append(givenName.trim());
        }

        if (mSb.length() > 0) {
            insertNameLookup(rawContactId, dataId, NameLookupType.NAME_COLLATION_KEY,
                    NameNormalizer.normalize(mSb.toString()));
        }
    }

    /**
     * Performs a query and returns true if any Data item of the raw contact with the given
     * id and mimetype is marked as super-primary
     */
    public boolean rawContactHasSuperPrimary(long rawContactId, long mimeTypeId) {
        final Cursor existsCursor = getReadableDatabase().rawQuery(
                "SELECT EXISTS(SELECT 1 FROM " + Tables.DATA +
                " WHERE " + Data.RAW_CONTACT_ID + "=?" +
                " AND " + DataColumns.MIMETYPE_ID + "=?" +
                " AND " + Data.IS_SUPER_PRIMARY + "<>0)",
                new String[] { String.valueOf(rawContactId), String.valueOf(mimeTypeId) });
        try {
            if (!existsCursor.moveToFirst()) throw new IllegalStateException();
            return existsCursor.getInt(0) != 0;
        } finally {
            existsCursor.close();
        }
    }

    public String getCurrentCountryIso() {
        return mCountryMonitor.getCountryIso();
    }

    @NeededForTesting
    /* package */ void setUseStrictPhoneNumberComparisonForTest(boolean useStrict) {
        mUseStrictPhoneNumberComparison = useStrict;
    }

    @NeededForTesting
    /* package */ boolean getUseStrictPhoneNumberComparisonForTest() {
        return mUseStrictPhoneNumberComparison;
    }

    @NeededForTesting
    /* package */ String querySearchIndexContentForTest(long contactId) {
        return DatabaseUtils.stringForQuery(getReadableDatabase(),
                "SELECT " + SearchIndexColumns.CONTENT +
                " FROM " + Tables.SEARCH_INDEX +
                " WHERE " + SearchIndexColumns.CONTACT_ID + "=CAST(? AS int)",
                new String[] { String.valueOf(contactId) });
    }

    @NeededForTesting
    /* package */ String querySearchIndexTokensForTest(long contactId) {
        return DatabaseUtils.stringForQuery(getReadableDatabase(),
                "SELECT " + SearchIndexColumns.TOKENS +
                " FROM " + Tables.SEARCH_INDEX +
                " WHERE " + SearchIndexColumns.CONTACT_ID + "=CAST(? AS int)",
                new String[] { String.valueOf(contactId) });
    }
}
