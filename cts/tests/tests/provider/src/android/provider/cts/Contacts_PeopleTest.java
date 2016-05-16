/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.provider.cts;


import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.Contacts;
import android.provider.Contacts.Groups;
import android.provider.Contacts.GroupsColumns;
import android.provider.Contacts.People;
import android.test.InstrumentationTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Contacts_PeopleTest extends InstrumentationTestCase {
    private ContentResolver mContentResolver;
    private ContentProviderClient mProvider;

    private ArrayList<Uri> mPeopleRowsAdded;
    private ArrayList<Uri> mGroupRowsAdded;
    private ArrayList<Uri> mRowsAdded;

    private static final String[] PEOPLE_PROJECTION = new String[] {
            People._ID,
            People.LAST_TIME_CONTACTED
        };
    private static final int PEOPLE_ID_INDEX = 0;
    private static final int PEOPLE_LAST_CONTACTED_INDEX = 1;

    private static final int MEMBERSHIP_PERSON_ID_INDEX = 1;
    private static final int MEMBERSHIP_GROUP_ID_INDEX = 5;

    private static final String[] GROUPS_PROJECTION = new String[] {
        Groups._ID,
        Groups.NAME
    };
    private static final int GROUPS_ID_INDEX = 0;
    private static final int GROUPS_NAME_INDEX = 1;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContentResolver = getInstrumentation().getTargetContext().getContentResolver();
        mProvider = mContentResolver.acquireContentProviderClient(Contacts.AUTHORITY);

        mPeopleRowsAdded = new ArrayList<Uri>();
        mGroupRowsAdded = new ArrayList<Uri>();
        mRowsAdded = new ArrayList<Uri>();

        // insert some lines in people table and groups table to be used in test case.
        for (int i=0; i<3; i++) {
            ContentValues value = new ContentValues();
            value.put(People.NAME, "test_people_" + i);
            value.put(People.TIMES_CONTACTED, 0);
            value.put(People.LAST_TIME_CONTACTED, 0);
            mPeopleRowsAdded.add(mProvider.insert(People.CONTENT_URI, value));
        }

        ContentValues value = new ContentValues();
        value.put(Groups.NAME, "test_group_0");
        mGroupRowsAdded.add(mProvider.insert(Groups.CONTENT_URI, value));
        value.put(Groups.NAME, "test_group_1");
        mGroupRowsAdded.add(mProvider.insert(Groups.CONTENT_URI, value));
    }

    @Override
    protected void tearDown() throws Exception {
        // remove the lines we inserted in setup and added in test cases.
        for (Uri row : mRowsAdded) {
            mProvider.delete(row, null, null);
        }
        mRowsAdded.clear();

        for (Uri row : mPeopleRowsAdded) {
            mProvider.delete(row, null, null);
        }
        mPeopleRowsAdded.clear();

        for (Uri row : mGroupRowsAdded) {
            mProvider.delete(row, null, null);
        }
        mGroupRowsAdded.clear();

        super.tearDown();
    }

    public void testAddToGroup() {
        Cursor cursor;
        try {
            // Add the My Contacts group, since it is no longer automatically created.
            ContentValues testValues = new ContentValues();
            testValues.put(GroupsColumns.SYSTEM_ID, Groups.GROUP_MY_CONTACTS);
            mProvider.insert(Groups.CONTENT_URI, testValues);

            // People: test_people_0, Group: Groups.GROUP_MY_CONTACTS
            cursor = mProvider.query(mPeopleRowsAdded.get(0), PEOPLE_PROJECTION,
                    null, null, null, null);
            cursor.moveToFirst();
            int personId = cursor.getInt(PEOPLE_ID_INDEX);
            cursor.close();
            mRowsAdded.add(People.addToMyContactsGroup(mContentResolver, personId));
            cursor = mProvider.query(Groups.CONTENT_URI, GROUPS_PROJECTION,
                    Groups.SYSTEM_ID + "='" + Groups.GROUP_MY_CONTACTS + "'", null, null, null);
            cursor.moveToFirst();
            int groupId = cursor.getInt(GROUPS_ID_INDEX);
            cursor.close();
            cursor = People.queryGroups(mContentResolver, personId);
            cursor.moveToFirst();
            assertEquals(personId, cursor.getInt(MEMBERSHIP_PERSON_ID_INDEX));
            assertEquals(groupId, cursor.getInt(MEMBERSHIP_GROUP_ID_INDEX));
            cursor.close();

            // People: test_people_create, Group: Groups.GROUP_MY_CONTACTS
            ContentValues values = new ContentValues();
            values.put(People.NAME, "test_people_create");
            values.put(People.TIMES_CONTACTED, 0);
            values.put(People.LAST_TIME_CONTACTED, 0);
            mRowsAdded.add(People.createPersonInMyContactsGroup(mContentResolver, values));
            cursor = mProvider.query(People.CONTENT_URI, PEOPLE_PROJECTION,
                    People.NAME + " = 'test_people_create'", null, null, null);

            cursor.moveToFirst();
            personId = cursor.getInt(PEOPLE_ID_INDEX);
            mRowsAdded.add(ContentUris.withAppendedId(People.CONTENT_URI, personId));
            cursor.close();
            cursor = mProvider.query(Groups.CONTENT_URI, GROUPS_PROJECTION,
                    Groups.SYSTEM_ID + "='" + Groups.GROUP_MY_CONTACTS + "'", null, null, null);
            cursor.moveToFirst();
            groupId = cursor.getInt(GROUPS_ID_INDEX);
            cursor.close();
            cursor = People.queryGroups(mContentResolver, personId);
            cursor.moveToFirst();
            assertEquals(personId, cursor.getInt(MEMBERSHIP_PERSON_ID_INDEX));
            assertEquals(groupId, cursor.getInt(MEMBERSHIP_GROUP_ID_INDEX));
            cursor.close();

            // People: test_people_1, Group: test_group_0
            cursor = mProvider.query(mPeopleRowsAdded.get(1), PEOPLE_PROJECTION,
                    null, null, null, null);
            cursor.moveToFirst();
            personId = cursor.getInt(PEOPLE_ID_INDEX);
            cursor.close();
            cursor = mProvider.query(mGroupRowsAdded.get(0), GROUPS_PROJECTION,
                    null, null, null, null);
            cursor.moveToFirst();
            groupId = cursor.getInt(GROUPS_ID_INDEX);
            cursor.close();
            mRowsAdded.add(People.addToGroup(mContentResolver, personId, groupId));
            cursor = People.queryGroups(mContentResolver, personId);
            boolean found = false;
            while (cursor.moveToNext()) {
                assertEquals(personId, cursor.getInt(MEMBERSHIP_PERSON_ID_INDEX));
                if (cursor.getInt(MEMBERSHIP_GROUP_ID_INDEX) == groupId) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);

            cursor.close();

            // People: test_people_2, Group: test_group_1
            cursor = mProvider.query(mPeopleRowsAdded.get(2), PEOPLE_PROJECTION,
                    null, null, null, null);
            cursor.moveToFirst();
            personId = cursor.getInt(PEOPLE_ID_INDEX);
            cursor.close();
            String groupName = "test_group_1";
            mRowsAdded.add(People.addToGroup(mContentResolver, personId, groupName));
            cursor = People.queryGroups(mContentResolver, personId);
            List<Integer> groupIds = new ArrayList<Integer>();
            while (cursor.moveToNext()) {
                assertEquals(personId, cursor.getInt(MEMBERSHIP_PERSON_ID_INDEX));
                groupIds.add(cursor.getInt(MEMBERSHIP_GROUP_ID_INDEX));
            }
            cursor.close();

            found = false;
            for (int id : groupIds) {
                cursor = mProvider.query(Groups.CONTENT_URI, GROUPS_PROJECTION,
                        Groups._ID + "=" + id, null, null, null);
                cursor.moveToFirst();
                if (groupName.equals(cursor.getString(GROUPS_NAME_INDEX))) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
            cursor.close();
        } catch (RemoteException e) {
            fail("Unexpected RemoteException");
        }
    }

    public void testMarkAsContacted() {
        Cursor cursor;
        try {
            cursor = mProvider.query(mPeopleRowsAdded.get(0), PEOPLE_PROJECTION,
                    null, null, null, null);
            cursor.moveToFirst();
            int personId = cursor.getInt(PEOPLE_ID_INDEX);
            long oldLastContacted = cursor.getLong(PEOPLE_LAST_CONTACTED_INDEX);
            cursor.close();
            People.markAsContacted(mContentResolver, personId);
            cursor = mProvider.query(mPeopleRowsAdded.get(0), PEOPLE_PROJECTION,
                    null, null, null, null);
            cursor.moveToFirst();
            long lastContacted = cursor.getLong(PEOPLE_LAST_CONTACTED_INDEX);
            assertTrue(oldLastContacted < lastContacted);
            oldLastContacted = lastContacted;
            cursor.close();

            People.markAsContacted(mContentResolver, personId);
            cursor = mProvider.query(mPeopleRowsAdded.get(0), PEOPLE_PROJECTION,
                    null, null, null, null);
            cursor.moveToFirst();
            lastContacted = cursor.getLong(PEOPLE_LAST_CONTACTED_INDEX);
            assertTrue(oldLastContacted < lastContacted);
            cursor.close();
        } catch (RemoteException e) {
            fail("Unexpected RemoteException");
        }
    }

    public void testAccessPhotoData() {
        Context context = getInstrumentation().getTargetContext();
        try {
            InputStream inputStream = context.getResources().openRawResource(
                    com.android.cts.stub.R.drawable.testimage);
            int size = inputStream.available();
            byte[] data =  new byte[size];
            inputStream.read(data);

            People.setPhotoData(mContentResolver, mPeopleRowsAdded.get(0), data);
            InputStream photoStream = People.openContactPhotoInputStream(
                    mContentResolver, mPeopleRowsAdded.get(0));
            assertNotNull(photoStream);
            Bitmap bitmap = BitmapFactory.decodeStream(photoStream, null, null);
            assertEquals(96, bitmap.getWidth());
            assertEquals(64, bitmap.getHeight());

            photoStream = People.openContactPhotoInputStream(mContentResolver,
                    mPeopleRowsAdded.get(1));
            assertNull(photoStream);

            bitmap = People.loadContactPhoto(context, mPeopleRowsAdded.get(0),
                    com.android.cts.stub.R.drawable.size_48x48, null);
            assertEquals(96, bitmap.getWidth());
            assertEquals(64, bitmap.getHeight());

            bitmap = People.loadContactPhoto(context, null,
                    com.android.cts.stub.R.drawable.size_48x48, null);
            assertNotNull(bitmap);
        } catch (IOException e) {
            fail("Unexpected IOException");
        }
    }
}
