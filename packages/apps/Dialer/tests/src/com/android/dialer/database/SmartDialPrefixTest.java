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
 * limitations under the License.
 */

package com.android.dialer.database;

import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.Suppress;
import android.test.AndroidTestCase;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;

import com.android.dialer.database.DialerDatabaseHelper;
import com.android.dialer.database.DialerDatabaseHelper.ContactNumber;
import com.android.dialer.dialpad.SmartDialNameMatcher;
import com.android.dialer.dialpad.SmartDialPrefix;

import java.lang.Exception;
import java.lang.FindBugsSuppressWarnings;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * To run this test, use the command:
 * adb shell am instrument -w -e class com.android.dialer.dialpad.SmartDialPrefixTest /
 * com.android.dialer.tests/android.test.InstrumentationTestRunner
 */
@SmallTest
public class SmartDialPrefixTest extends AndroidTestCase {

    private DialerDatabaseHelper mTestHelper;

    public void testIsCountryNanp_CaseInsensitive() {
        assertFalse(SmartDialPrefix.isCountryNanp(null));
        assertFalse(SmartDialPrefix.isCountryNanp("CN"));
        assertFalse(SmartDialPrefix.isCountryNanp("HK"));
        assertFalse(SmartDialPrefix.isCountryNanp("uk"));
        assertFalse(SmartDialPrefix.isCountryNanp("sg"));
        assertTrue(SmartDialPrefix.isCountryNanp("US"));
        assertTrue(SmartDialPrefix.isCountryNanp("CA"));
        assertTrue(SmartDialPrefix.isCountryNanp("AS"));
        assertTrue(SmartDialPrefix.isCountryNanp("AI"));
        assertTrue(SmartDialPrefix.isCountryNanp("AG"));
        assertTrue(SmartDialPrefix.isCountryNanp("BS"));
        assertTrue(SmartDialPrefix.isCountryNanp("BB"));
        assertTrue(SmartDialPrefix.isCountryNanp("bm"));
        assertTrue(SmartDialPrefix.isCountryNanp("vg"));
        assertTrue(SmartDialPrefix.isCountryNanp("ky"));
        assertTrue(SmartDialPrefix.isCountryNanp("dm"));
        assertTrue(SmartDialPrefix.isCountryNanp("do"));
        assertTrue(SmartDialPrefix.isCountryNanp("gd"));
        assertTrue(SmartDialPrefix.isCountryNanp("gu"));
        assertTrue(SmartDialPrefix.isCountryNanp("jm"));
        assertTrue(SmartDialPrefix.isCountryNanp("pr"));
        assertTrue(SmartDialPrefix.isCountryNanp("ms"));
        assertTrue(SmartDialPrefix.isCountryNanp("mp"));
        assertTrue(SmartDialPrefix.isCountryNanp("kn"));
        assertTrue(SmartDialPrefix.isCountryNanp("lc"));
        assertTrue(SmartDialPrefix.isCountryNanp("vc"));
        assertTrue(SmartDialPrefix.isCountryNanp("tt"));
        assertTrue(SmartDialPrefix.isCountryNanp("tc"));
        assertTrue(SmartDialPrefix.isCountryNanp("vi"));
    }

    protected void setUp() {
        mTestHelper = DialerDatabaseHelper.getNewInstanceForTest(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        final SQLiteDatabase db = mTestHelper.getWritableDatabase();
        mTestHelper.removeAllContacts(db);
        super.tearDown();
    }

    @Suppress
    public void testForNewContacts() {
    }

    @Suppress
    public void testForUpdatedContacts() {
    }

    @Suppress
    public void testForDeletedContacts() {
    }

    @Suppress
    public void testSize() {
    }


    private MatrixCursor constructNewNameCursor() {
        final MatrixCursor cursor = new MatrixCursor(new String[]{
                DialerDatabaseHelper.SmartDialDbColumns.DISPLAY_NAME_PRIMARY,
                DialerDatabaseHelper.SmartDialDbColumns.CONTACT_ID});
        return cursor;
    }

    private MatrixCursor constructNewContactCursor() {
        final MatrixCursor cursor = new MatrixCursor(new String[]{
                    Phone._ID,                          // 0
                    Phone.TYPE,                         // 1
                    Phone.LABEL,                        // 2
                    Phone.NUMBER,                       // 3
                    Phone.CONTACT_ID,                   // 4
                    Phone.LOOKUP_KEY,                   // 5
                    Phone.DISPLAY_NAME_PRIMARY,         // 6
                    Phone.PHOTO_ID,                     // 7
                    Data.LAST_TIME_USED,                // 8
                    Data.TIMES_USED,                    // 9
                    Contacts.STARRED,                   // 10
                    Data.IS_SUPER_PRIMARY,              // 11
                    Contacts.IN_VISIBLE_GROUP,          // 12
                    Data.IS_PRIMARY});                  // 13
        return cursor;
    }

    private ContactNumber constructNewContactWithDummyIds(MatrixCursor contactCursor,
            MatrixCursor nameCursor, String number, int id, String displayName) {
        return constructNewContact(contactCursor, nameCursor, id, number, 0, "", displayName, 0, 0,
                0, 0, 0, 0, 0);
    }

    private ContactNumber constructNewContact(MatrixCursor contactCursor, MatrixCursor nameCursor,
            int id, String number, int contactId, String lookupKey, String displayName, int photoId,
            int lastTimeUsed, int timesUsed, int starred, int isSuperPrimary, int inVisibleGroup,
            int isPrimary) {
        assertNotNull(contactCursor);
        assertNotNull(nameCursor);

        contactCursor.addRow(new Object[]{id, "", "", number, contactId, lookupKey, displayName,
                photoId, lastTimeUsed, timesUsed, starred, isSuperPrimary, inVisibleGroup,
                isPrimary});
        nameCursor.addRow(new Object[]{displayName, contactId});

        return new ContactNumber(contactId, id, displayName, number, lookupKey, 0);
    }

    private ArrayList<ContactNumber> getLooseMatchesFromDb(String query) {
        final SmartDialNameMatcher nameMatcher = new SmartDialNameMatcher(query,
                SmartDialPrefix.getMap());
        return mTestHelper.getLooseMatches(query, nameMatcher);
    }

    public void testPutForFullName() {
        final SQLiteDatabase db = mTestHelper.getWritableDatabase();

        final MatrixCursor nameCursor =  constructNewNameCursor();
        final MatrixCursor contactCursor = constructNewContactCursor();
        final ContactNumber jasonsmith = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "", 0, "Jason Smith");
        final ContactNumber jasonsmitt = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "", 1, "Jason Smitt");
        final ContactNumber alphabet = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "12345678", 2, "abc def ghi jkl mno pqrs tuv wxyz");

        mTestHelper.insertUpdatedContactsAndNumberPrefix(db, contactCursor, Long.valueOf(0));
        mTestHelper.insertNamePrefixes(db, nameCursor);

        nameCursor.close();
        contactCursor.close();

        final ArrayList<ContactNumber> result1 = getLooseMatchesFromDb("5276676484");
        assertFalse(result1.contains(jasonsmitt));

        final ArrayList<ContactNumber> result2 = getLooseMatchesFromDb("5276676488");
        assertFalse(result2.contains(jasonsmith));
        assertTrue(result2.contains(jasonsmitt));

        assertTrue(getLooseMatchesFromDb("22233344455566677778889999").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("33344455566677778889999").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("44455566677778889999").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("55566677778889999").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("66677778889999").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("77778889999").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("8889999").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("9999").contains(alphabet));

        // Makes sure the phone number is correctly added.
        assertTrue(getLooseMatchesFromDb("12345678").contains(alphabet));
    }

    public void testPutForPartialName() {
        final SQLiteDatabase db = mTestHelper.getWritableDatabase();

        final MatrixCursor nameCursor =  constructNewNameCursor();
        final MatrixCursor contactCursor = constructNewContactCursor();
        final ContactNumber maryjane = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "", 0, "Mary Jane");
        final ContactNumber sarahsmith = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "", 1, "Sarah Smith");
        final ContactNumber jasonsmitt = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "", 2, "Jason Smitt");

        mTestHelper.insertUpdatedContactsAndNumberPrefix(db, contactCursor, Long.valueOf(0));
        mTestHelper.insertNamePrefixes(db, nameCursor);

        nameCursor.close();
        contactCursor.close();

        final ArrayList<ContactNumber> result1 = getLooseMatchesFromDb("6279");
        assertTrue(result1.contains(maryjane));
        assertFalse(result1.contains(jasonsmitt));

        // 72 corresponds to sa = "Sarah Smith" but not "Jason Smitt" or "Mary Jane"
        final ArrayList<ContactNumber> result2 = getLooseMatchesFromDb("72");
        assertFalse(result2.contains(maryjane));
        assertTrue(result2.contains(sarahsmith));
        assertFalse(result2.contains(jasonsmitt));

        // 76 corresponds to sm = "Sarah Smith" and "Jason Smitt" but not "Mary Jane"
        final ArrayList<ContactNumber> result3 = getLooseMatchesFromDb("76");
        assertFalse(result3.contains(maryjane));
        assertTrue(result3.contains(sarahsmith));
        assertTrue(result3.contains(jasonsmitt));
    }

    public void testPutForNameTokens() {
        final SQLiteDatabase db = mTestHelper.getWritableDatabase();

        final MatrixCursor nameCursor =  constructNewNameCursor();
        final MatrixCursor contactCursor = constructNewContactCursor();
        final ContactNumber jasonfwilliams = constructNewContactWithDummyIds(contactCursor,
                nameCursor, "", 0, "Jason F. Williams");

        mTestHelper.insertUpdatedContactsAndNumberPrefix(db, contactCursor, Long.valueOf(0));
        mTestHelper.insertNamePrefixes(db, nameCursor);

        nameCursor.close();
        contactCursor.close();

        assertTrue(getLooseMatchesFromDb("527").contains(jasonfwilliams));
        // 72 corresponds to sa = "Sarah Smith" but not "Jason Smitt" or "Mary Jane"
        assertTrue(getLooseMatchesFromDb("945").contains(jasonfwilliams));
        // 76 corresponds to sm = "Sarah Smith" and "Jason Smitt" but not "Mary Jane"
        assertFalse(getLooseMatchesFromDb("66").contains(jasonfwilliams));
    }

    public void testPutForInitialMatches() {
        final SQLiteDatabase db = mTestHelper.getWritableDatabase();

        final MatrixCursor nameCursor =  constructNewNameCursor();
        final MatrixCursor contactCursor = constructNewContactCursor();
        final ContactNumber martinjuniorharry = constructNewContactWithDummyIds(contactCursor,
                nameCursor, "", 0, "Martin Jr Harry");

        mTestHelper.insertUpdatedContactsAndNumberPrefix(db, contactCursor, Long.valueOf(0));
        mTestHelper.insertNamePrefixes(db, nameCursor);

        nameCursor.close();
        contactCursor.close();

        // 654 corresponds to mjh = "(M)artin (J)r (H)arry"
        assertTrue(getLooseMatchesFromDb("654").contains(martinjuniorharry));
        // The reverse (456) does not match (for now)
        assertFalse(getLooseMatchesFromDb("456").contains(martinjuniorharry));
        // 6542 corresponds to mjha = "(M)artin (J)r (Ha)rry"
        assertTrue(getLooseMatchesFromDb("6542").contains(martinjuniorharry));
        // 542 corresponds to jha = "Martin (J)r (Ha)rry"
        assertTrue(getLooseMatchesFromDb("542").contains(martinjuniorharry));
        // 642 corresponds to mha = "(M)artin Jr (Ha)rry"
        assertTrue(getLooseMatchesFromDb("642").contains(martinjuniorharry));
        // 6542779 (M)artin (J)r (Harry)
        assertTrue(getLooseMatchesFromDb("6542779").contains(martinjuniorharry));
        // 65742779 (M)artin (Jr) (Harry)
        assertTrue(getLooseMatchesFromDb("65742779").contains(martinjuniorharry));
        // 542779 Martin (J)r (Harry)
        assertTrue(getLooseMatchesFromDb("542779").contains(martinjuniorharry));
        // 547 doesn't match
        assertFalse(getLooseMatchesFromDb("547").contains(martinjuniorharry));
        // 655 doesn't match
        assertFalse(getLooseMatchesFromDb("655").contains(martinjuniorharry));
        // 653 doesn't match
        assertFalse(getLooseMatchesFromDb("653").contains(martinjuniorharry));
        // 6543 doesn't match
        assertFalse(getLooseMatchesFromDb("6543").contains(martinjuniorharry));

        assertEquals(7, mTestHelper.countPrefixTableRows(db));
    }

    public void testPutForInitialMatchesForLongTokenNames() {

        final SQLiteDatabase db = mTestHelper.getWritableDatabase();

        final MatrixCursor nameCursor =  constructNewNameCursor();
        final MatrixCursor contactCursor = constructNewContactCursor();
        final ContactNumber alphabet = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "12345678", 0, "abc def ghi jkl mno pqrs tuv wxyz");

        mTestHelper.insertUpdatedContactsAndNumberPrefix(db, contactCursor, Long.valueOf(0));
        mTestHelper.insertNamePrefixes(db, nameCursor);

        nameCursor.close();
        contactCursor.close();

        // Makes sure only only the first two and last two token are considered for initials.
        // The cut-off constant can be set in SmartDialPrefix.java
        assertTrue(getLooseMatchesFromDb("2389999").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("239999").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("23888").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("2333").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("289999").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("2888").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("29999").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("3888").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("39999").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("389999").contains(alphabet));
        assertTrue(getLooseMatchesFromDb("89999").contains(alphabet));
    }

    public void testCheckLongToken() {
        final SQLiteDatabase db = mTestHelper.getWritableDatabase();

        final MatrixCursor nameCursor =  constructNewNameCursor();
        final MatrixCursor contactCursor = constructNewContactCursor();
        final ContactNumber alphabet = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "1", 0,  " aaaa bbbb cccc dddd eeee ffff gggg hhhh iiii jjjj kkkk llll mmmm nnnn" +
                " oooo pppp qqqq rrrr ssss tttt uuuu vvvv wwww xxxx yyyy zzzz");

        final ContactNumber alphabet2 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "1", 1, "aaaabbbbccccddddeeeeffffgggghhhhiiiijjjjkkkkllllmmmmnnnnooooppppqqqqrrrr" +
                "ssssttttuuuuvvvvwwwwxxxxyyyyzzzz");

        mTestHelper.insertUpdatedContactsAndNumberPrefix(db, contactCursor, Long.valueOf(0));
        mTestHelper.insertNamePrefixes(db, nameCursor);

        nameCursor.close();
        contactCursor.close();

        assertTrue(getLooseMatchesFromDb("2222").contains(alphabet));
        assertEquals(40, mTestHelper.countPrefixTableRows(db));
    }

    public void testAccentedCharacters() {
        final SQLiteDatabase db = mTestHelper.getWritableDatabase();

        final MatrixCursor nameCursor =  constructNewNameCursor();
        final MatrixCursor contactCursor = constructNewContactCursor();
        final ContactNumber reene = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "0", 0, "Reenée");
        final ContactNumber bronte = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "0", 1, "Brontë");

        mTestHelper.insertUpdatedContactsAndNumberPrefix(db, contactCursor, Long.valueOf(0));
        mTestHelper.insertNamePrefixes(db, nameCursor);

        nameCursor.close();
        contactCursor.close();

        assertTrue(getLooseMatchesFromDb("733633").contains(reene));
        assertTrue(getLooseMatchesFromDb("276683").contains(bronte));
    }

    public void testNumbersInName() {
        final SQLiteDatabase db = mTestHelper.getWritableDatabase();

        final MatrixCursor nameCursor =  constructNewNameCursor();
        final MatrixCursor contactCursor = constructNewContactCursor();
        final ContactNumber contact = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "0", 0, "12345678");
        final ContactNumber teacher = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "0", 1, "1st Grade Teacher");

        mTestHelper.insertUpdatedContactsAndNumberPrefix(db, contactCursor, Long.valueOf(0));
        mTestHelper.insertNamePrefixes(db, nameCursor);

        nameCursor.close();
        contactCursor.close();

        assertTrue(getLooseMatchesFromDb("12345678").contains(contact));
        assertTrue(getLooseMatchesFromDb("17847233").contains(teacher));
        assertTrue(getLooseMatchesFromDb("14832").contains(teacher));
    }

    public void testPutForNumbers() {
        final SQLiteDatabase db = mTestHelper.getWritableDatabase();

        final MatrixCursor nameCursor =  constructNewNameCursor();
        final MatrixCursor contactCursor = constructNewContactCursor();
        final ContactNumber contactno1 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "510-527-2357", 0,  "James");
        final ContactNumber contactno2 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "77212862357", 1, "James");
        final ContactNumber contactno3 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "+13684976334", 2, "James");

        mTestHelper.insertUpdatedContactsAndNumberPrefix(db, contactCursor, Long.valueOf(0));
        mTestHelper.insertNamePrefixes(db, nameCursor);

        nameCursor.close();
        contactCursor.close();

        assertTrue(getLooseMatchesFromDb("510").contains(contactno1));
        assertFalse(getLooseMatchesFromDb("511").contains(contactno1));
        assertTrue(getLooseMatchesFromDb("77212862357").contains(contactno2));
        assertFalse(getLooseMatchesFromDb("77212862356").contains(contactno2));
        assertTrue(getLooseMatchesFromDb("1368").contains(contactno3));
        assertFalse(getLooseMatchesFromDb("1367").contains(contactno3));
    }

    public void testPutNumbersCountryCode() {
        final SQLiteDatabase db = mTestHelper.getWritableDatabase();

        final MatrixCursor nameCursor =  constructNewNameCursor();
        final MatrixCursor contactCursor = constructNewContactCursor();
        final ContactNumber contactno1 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "+13684976334", 0, "James");
        final ContactNumber contactno2 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "+65 9177-6930", 1, "Jason");
        final ContactNumber contactno3 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "+85212345678", 2, "Mike");
        final ContactNumber contactno4 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "+85112345678", 3, "Invalid");
        final ContactNumber contactno5 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "+852", 4, "Invalid");

        mTestHelper.insertUpdatedContactsAndNumberPrefix(db, contactCursor, Long.valueOf(0));
        mTestHelper.insertNamePrefixes(db, nameCursor);

        nameCursor.close();
        contactCursor.close();

        assertTrue(getLooseMatchesFromDb("1368").contains(contactno1));
        assertTrue(getLooseMatchesFromDb("368497").contains(contactno1));
        assertFalse(getLooseMatchesFromDb("2368497").contains(contactno1));

        assertTrue(getLooseMatchesFromDb("6591776930").contains(contactno2));
        assertTrue(getLooseMatchesFromDb("91776930").contains(contactno2));
        assertFalse(getLooseMatchesFromDb("591776930").contains(contactno2));

        assertTrue(getLooseMatchesFromDb("85212345678").contains(contactno3));
        assertTrue(getLooseMatchesFromDb("12345678").contains(contactno3));
        assertFalse(getLooseMatchesFromDb("5212345678").contains(contactno3));

        assertTrue(getLooseMatchesFromDb("85112345678").contains(contactno4));
        assertFalse(getLooseMatchesFromDb("12345678").contains(contactno4));
    }

    // Tests special case handling for NANP numbers
    public void testPutNumbersNANP() {
        SmartDialPrefix.setUserInNanpRegion(true);
        final SQLiteDatabase db = mTestHelper.getWritableDatabase();

        final MatrixCursor nameCursor =  constructNewNameCursor();
        final MatrixCursor contactCursor = constructNewContactCursor();
        final ContactNumber contactno1 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "16503337596", 0, "James");
        final ContactNumber contactno2 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "5109921234", 1, "Michael");
        final ContactNumber contactno3 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "(415)-123-4567", 2, "Jason");
        final ContactNumber contactno4 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "1 510-284-9170", 3, "Mike");
        final ContactNumber contactno5 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "1-415-123-123", 4, "Invalid");
        final ContactNumber contactno6 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "415-123-123", 5, "Invalid2");
        final ContactNumber contactno7 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "+1-510-284-9170", 6, "Mike");
        final ContactNumber contactno8 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "+1-510-284-917", 7, "Invalid");
        final ContactNumber contactno9 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "+857-510-284-9170", 8, "Inv");

        mTestHelper.insertUpdatedContactsAndNumberPrefix(db, contactCursor, Long.valueOf(0));
        mTestHelper.insertNamePrefixes(db, nameCursor);

        nameCursor.close();
        contactCursor.close();

        assertTrue(getLooseMatchesFromDb("16503337596").contains(contactno1));
        assertTrue(getLooseMatchesFromDb("6503337596").contains(contactno1));
        assertTrue(getLooseMatchesFromDb("3337596").contains(contactno1));

        assertTrue(getLooseMatchesFromDb("5109921234").contains(contactno2));
        assertTrue(getLooseMatchesFromDb("9921234").contains(contactno2));

        assertTrue(getLooseMatchesFromDb("4151234567").contains(contactno3));
        assertTrue(getLooseMatchesFromDb("1234567").contains(contactno3));

        assertTrue(getLooseMatchesFromDb("15102849170").contains(contactno4));
        assertTrue(getLooseMatchesFromDb("5102849170").contains(contactno4));
        assertTrue(getLooseMatchesFromDb("2849170").contains(contactno4));

        assertTrue(getLooseMatchesFromDb("1415123123").contains(contactno5));
        assertFalse(getLooseMatchesFromDb("415123123").contains(contactno5));
        assertFalse(getLooseMatchesFromDb("123123").contains(contactno5));

        assertTrue(getLooseMatchesFromDb("415123123").contains(contactno6));
        assertFalse(getLooseMatchesFromDb("123123").contains(contactno6));

        assertTrue(getLooseMatchesFromDb("15102849170").contains(contactno7));
        assertTrue(getLooseMatchesFromDb("5102849170").contains(contactno7));
        assertTrue(getLooseMatchesFromDb("2849170").contains(contactno7));
        assertFalse(getLooseMatchesFromDb("849170").contains(contactno7));
        assertFalse(getLooseMatchesFromDb("10849170").contains(contactno7));

        assertTrue(getLooseMatchesFromDb("1510284917").contains(contactno8));
        assertTrue(getLooseMatchesFromDb("510284917").contains(contactno8));
        assertFalse(getLooseMatchesFromDb("2849170").contains(contactno8));

        assertTrue(getLooseMatchesFromDb("8575102849170").contains(contactno9));
        assertFalse(getLooseMatchesFromDb("5102849170").contains(contactno9));
        assertFalse(getLooseMatchesFromDb("2849170").contains(contactno9));

        // TODO(klp) Adds test for non-NANP region number matchings.
    }

    // Tests special case handling for non-NANP numbers
    public void testPutNumbersNonNANP() {
        SmartDialPrefix.setUserInNanpRegion(false);
        final SQLiteDatabase db = mTestHelper.getWritableDatabase();

        final MatrixCursor nameCursor =  constructNewNameCursor();
        final MatrixCursor contactCursor = constructNewContactCursor();

        final ContactNumber contactno0 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "(415)-123-4567", 0, "Jason");
        final ContactNumber contactno1 = constructNewContactWithDummyIds(contactCursor, nameCursor,
                "1 510-284-9170", 1, "Mike");


        mTestHelper.insertUpdatedContactsAndNumberPrefix(db, contactCursor, Long.valueOf(0));
        mTestHelper.insertNamePrefixes(db, nameCursor);

        nameCursor.close();
        contactCursor.close();

        assertTrue(getLooseMatchesFromDb("4151234567").contains(contactno0));
        assertFalse(getLooseMatchesFromDb("1234567").contains(contactno0));

        assertTrue(getLooseMatchesFromDb("15102849170").contains(contactno1));
        assertFalse(getLooseMatchesFromDb("5102849170").contains(contactno1));
        assertFalse(getLooseMatchesFromDb("2849170").contains(contactno1));
    }

    public void testParseInfo() {
        final String name = "Mcdonald Jamie-Cullum";
        final ArrayList<String> info = SmartDialPrefix.parseToIndexTokens(name);
        assertEquals(3, info.size());
        assertEquals(8, info.get(0).length());
        assertEquals(5, info.get(1).length());
        assertEquals(6, info.get(2).length());

        final String name2 = "aaa bbb ccc ddd eee fff ggg hhh iii jjj kkk";
        final ArrayList<String> info2 = SmartDialPrefix.parseToIndexTokens(name2);
        assertEquals(11, info2.size());
        assertEquals(3, info2.get(0).length());
        assertEquals(3, info2.get(10).length());

        final String name3 = "this  is- a,test    name";
        final ArrayList<String> info3 = SmartDialPrefix.parseToIndexTokens(name3);
        assertEquals(5, info3.size());
        assertEquals(2, info3.get(1).length());
        assertEquals(1, info3.get(2).length());
        assertEquals(4, info3.get(3).length());
        assertEquals(4, info3.get(4).length());

        final String name4 = "M c-Donald James";
        final ArrayList<String> info4 = SmartDialPrefix.parseToIndexTokens(name4);
        assertEquals(4, info4.size());
        assertEquals(1, info4.get(1).length());
        assertEquals(6, info4.get(2).length());

        final String name5 = "   Aa'Bb    c    dddd  e'e";
        final ArrayList<String> info5 = SmartDialPrefix.parseToIndexTokens(name5);
        assertEquals(6, info5.size());
        assertEquals(2, info5.get(0).length());
        assertEquals(1, info5.get(5).length());
    }
}
