/*
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

package com.android.dialer;

import android.content.Context;
import android.content.res.Resources;
import android.provider.CallLog.Calls;
import android.test.AndroidTestCase;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import com.android.dialer.calllog.CallTypeHelper;
import com.android.dialer.calllog.TestPhoneNumberUtilsWrapper;
import com.android.dialer.util.LocaleTestUtils;

import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Unit tests for {@link PhoneCallDetailsHelper}.
 */
public class PhoneCallDetailsHelperTest extends AndroidTestCase {
    /** The number to be used to access the voicemail. */
    private static final String TEST_VOICEMAIL_NUMBER = "125";
    /** The date of the call log entry. */
    private static final long TEST_DATE =
        new GregorianCalendar(2011, 5, 3, 13, 0, 0).getTimeInMillis();
    /** A test duration value for phone calls. */
    private static final long TEST_DURATION = 62300;
    /** The number of the caller/callee in the log entry. */
    private static final String TEST_NUMBER = "14125555555";
    /** The formatted version of {@link #TEST_NUMBER}. */
    private static final String TEST_FORMATTED_NUMBER = "1-412-255-5555";
    /** The country ISO name used in the tests. */
    private static final String TEST_COUNTRY_ISO = "US";
    /** The geocoded location used in the tests. */
    private static final String TEST_GEOCODE = "United States";

    /** The object under test. */
    private PhoneCallDetailsHelper mHelper;
    /** The views to fill. */
    private PhoneCallDetailsViews mViews;
    private TextView mNameView;
    private LocaleTestUtils mLocaleTestUtils;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        Resources resources = context.getResources();
        CallTypeHelper callTypeHelper = new CallTypeHelper(resources);
        final TestPhoneNumberUtilsWrapper phoneUtils = new TestPhoneNumberUtilsWrapper(
                TEST_VOICEMAIL_NUMBER);
        mHelper = new PhoneCallDetailsHelper(resources, callTypeHelper, phoneUtils);
        mHelper.setCurrentTimeForTest(
                new GregorianCalendar(2011, 5, 4, 13, 0, 0).getTimeInMillis());
        mViews = PhoneCallDetailsViews.createForTest(context);
        mNameView = new TextView(context);
        mLocaleTestUtils = new LocaleTestUtils(getContext());
        mLocaleTestUtils.setLocale(Locale.US);
    }

    @Override
    protected void tearDown() throws Exception {
        mLocaleTestUtils.restoreLocale();
        mNameView = null;
        mViews = null;
        mHelper = null;
        super.tearDown();
    }

    public void testSetPhoneCallDetails_Unknown() {
        setPhoneCallDetailsWithNumber("", Calls.PRESENTATION_UNKNOWN, "");
        assertNameEqualsResource(R.string.unknown);
    }

    public void testSetPhoneCallDetails_Private() {
        setPhoneCallDetailsWithNumber("", Calls.PRESENTATION_RESTRICTED, "");
        assertNameEqualsResource(R.string.private_num);
    }

    public void testSetPhoneCallDetails_Payphone() {
        setPhoneCallDetailsWithNumber("", Calls.PRESENTATION_PAYPHONE, "");
        assertNameEqualsResource(R.string.payphone);
    }

    public void testSetPhoneCallDetails_Voicemail() {
        setPhoneCallDetailsWithNumber(TEST_VOICEMAIL_NUMBER,
                Calls.PRESENTATION_ALLOWED, TEST_VOICEMAIL_NUMBER);
        assertNameEqualsResource(R.string.voicemail);
    }

    public void testSetPhoneCallDetails_Normal() {
        setPhoneCallDetailsWithNumber("14125551212",
                Calls.PRESENTATION_ALLOWED, "1-412-555-1212");
        assertEquals("Yesterday", mViews.callTypeAndDate.getText().toString());
        assertEqualsHtml("<font color='#33b5e5'><b>Yesterday</b></font>",
                mViews.callTypeAndDate.getText());
    }

    /** Asserts that a char sequence is actually a Spanned corresponding to the expected HTML. */
    private void assertEqualsHtml(String expectedHtml, CharSequence actualText) {
        // In order to contain HTML, the text should actually be a Spanned.
        assertTrue(actualText instanceof Spanned);
        Spanned actualSpanned = (Spanned) actualText;
        // Convert from and to HTML to take care of alternative formatting of HTML.
        assertEquals(Html.toHtml(Html.fromHtml(expectedHtml)), Html.toHtml(actualSpanned));

    }

    public void testSetPhoneCallDetails_Date() {
        mHelper.setCurrentTimeForTest(
                new GregorianCalendar(2011, 5, 3, 13, 0, 0).getTimeInMillis());

        setPhoneCallDetailsWithDate(
                new GregorianCalendar(2011, 5, 3, 13, 0, 0).getTimeInMillis());
        assertDateEquals("0 mins ago");

        setPhoneCallDetailsWithDate(
                new GregorianCalendar(2011, 5, 3, 12, 0, 0).getTimeInMillis());
        assertDateEquals("1 hour ago");

        setPhoneCallDetailsWithDate(
                new GregorianCalendar(2011, 5, 2, 13, 0, 0).getTimeInMillis());
        assertDateEquals("Yesterday");

        setPhoneCallDetailsWithDate(
                new GregorianCalendar(2011, 5, 1, 13, 0, 0).getTimeInMillis());
        assertDateEquals("2 days ago");
    }

    public void testSetPhoneCallDetails_CallTypeIcons() {
        setPhoneCallDetailsWithCallTypeIcons(Calls.INCOMING_TYPE);
        assertCallTypeIconsEquals(Calls.INCOMING_TYPE);

        setPhoneCallDetailsWithCallTypeIcons(Calls.OUTGOING_TYPE);
        assertCallTypeIconsEquals(Calls.OUTGOING_TYPE);

        setPhoneCallDetailsWithCallTypeIcons(Calls.MISSED_TYPE);
        assertCallTypeIconsEquals(Calls.MISSED_TYPE);

        setPhoneCallDetailsWithCallTypeIcons(Calls.VOICEMAIL_TYPE);
        assertCallTypeIconsEquals(Calls.VOICEMAIL_TYPE);
    }

    public void testSetPhoneCallDetails_MultipleCallTypeIcons() {
        setPhoneCallDetailsWithCallTypeIcons(Calls.INCOMING_TYPE, Calls.OUTGOING_TYPE);
        assertCallTypeIconsEquals(Calls.INCOMING_TYPE, Calls.OUTGOING_TYPE);

        setPhoneCallDetailsWithCallTypeIcons(Calls.MISSED_TYPE, Calls.MISSED_TYPE);
        assertCallTypeIconsEquals(Calls.MISSED_TYPE, Calls.MISSED_TYPE);
    }

    public void testSetPhoneCallDetails_MultipleCallTypeIconsLastOneDropped() {
        setPhoneCallDetailsWithCallTypeIcons(Calls.MISSED_TYPE, Calls.MISSED_TYPE,
                Calls.INCOMING_TYPE, Calls.OUTGOING_TYPE);
        assertCallTypeIconsEqualsPlusOverflow("(4)",
                Calls.MISSED_TYPE, Calls.MISSED_TYPE, Calls.INCOMING_TYPE);
    }

    public void testSetPhoneCallDetails_Geocode() {
        setPhoneCallDetailsWithNumberAndGeocode("+14125555555", "1-412-555-5555", "Pennsylvania");
        assertNameEquals("1-412-555-5555");  // The phone number is shown as the name.
        assertLabelEquals("Pennsylvania"); // The geocode is shown as the label.
    }

    public void testSetPhoneCallDetails_NoGeocode() {
        setPhoneCallDetailsWithNumberAndGeocode("+14125555555", "1-412-555-5555", null);
        assertNameEquals("1-412-555-5555");  // The phone number is shown as the name.
        assertLabelEquals("-"); // The empty geocode is shown as the label.
    }

    public void testSetPhoneCallDetails_EmptyGeocode() {
        setPhoneCallDetailsWithNumberAndGeocode("+14125555555", "1-412-555-5555", "");
        assertNameEquals("1-412-555-5555");  // The phone number is shown as the name.
        assertLabelEquals("-"); // The empty geocode is shown as the label.
    }

    public void testSetPhoneCallDetails_NoGeocodeForVoicemail() {
        setPhoneCallDetailsWithNumberAndGeocode(TEST_VOICEMAIL_NUMBER, "", "United States");
        assertLabelEquals("-"); // The empty geocode is shown as the label.
    }

    public void testSetPhoneCallDetails_Highlighted() {
        setPhoneCallDetailsWithNumber(TEST_VOICEMAIL_NUMBER,
                Calls.PRESENTATION_ALLOWED, "");
    }

    public void testSetCallDetailsHeader_NumberOnly() {
        setCallDetailsHeaderWithNumber(TEST_NUMBER, Calls.PRESENTATION_ALLOWED);
        assertEquals(View.VISIBLE, mNameView.getVisibility());
        assertEquals("Add to contacts", mNameView.getText().toString());
    }

    public void testSetCallDetailsHeader_UnknownNumber() {
        setCallDetailsHeaderWithNumber("", Calls.PRESENTATION_UNKNOWN);
        assertEquals(View.VISIBLE, mNameView.getVisibility());
        assertEquals("Unknown", mNameView.getText().toString());
    }

    public void testSetCallDetailsHeader_PrivateNumber() {
        setCallDetailsHeaderWithNumber("", Calls.PRESENTATION_RESTRICTED);
        assertEquals(View.VISIBLE, mNameView.getVisibility());
        assertEquals("Private number", mNameView.getText().toString());
    }

    public void testSetCallDetailsHeader_PayphoneNumber() {
        setCallDetailsHeaderWithNumber("", Calls.PRESENTATION_PAYPHONE);
        assertEquals(View.VISIBLE, mNameView.getVisibility());
        assertEquals("Pay phone", mNameView.getText().toString());
    }

    public void testSetCallDetailsHeader_VoicemailNumber() {
        setCallDetailsHeaderWithNumber(TEST_VOICEMAIL_NUMBER, Calls.PRESENTATION_ALLOWED);
        assertEquals(View.VISIBLE, mNameView.getVisibility());
        assertEquals("Voicemail", mNameView.getText().toString());
    }

    public void testSetCallDetailsHeader() {
        setCallDetailsHeader("John Doe");
        assertEquals(View.VISIBLE, mNameView.getVisibility());
        assertEquals("John Doe", mNameView.getText().toString());
    }

    /** Asserts that the name text field contains the value of the given string resource. */
    private void assertNameEqualsResource(int resId) {
        assertNameEquals(getContext().getString(resId));
    }

    /** Asserts that the name text field contains the given string value. */
    private void assertNameEquals(String text) {
        assertEquals(text, mViews.nameView.getText().toString());
    }

    /** Asserts that the label text field contains the given string value. */
    private void assertLabelEquals(String text) {
        assertEquals(text, mViews.labelView.getText().toString());
    }

    /** Asserts that the date text field contains the given string value. */
    private void assertDateEquals(String text) {
        assertEquals(text, mViews.callTypeAndDate.getText().toString());
    }

    /** Asserts that the call type contains the images with the given drawables. */
    private void assertCallTypeIconsEquals(int... ids) {
        assertEquals(ids.length, mViews.callTypeIcons.getCount());
        for (int index = 0; index < ids.length; ++index) {
            int id = ids[index];
            assertEquals(id, mViews.callTypeIcons.getCallType(index));
        }
        assertEquals(View.VISIBLE, mViews.callTypeIcons.getVisibility());
        assertEquals("Yesterday", mViews.callTypeAndDate.getText().toString());
    }

    /**
     * Asserts that the call type contains the images with the given drawables and shows the given
     * text next to the icons.
     */
    private void assertCallTypeIconsEqualsPlusOverflow(String overflowText, int... ids) {
        assertEquals(ids.length, mViews.callTypeIcons.getCount());
        for (int index = 0; index < ids.length; ++index) {
            int id = ids[index];
            assertEquals(id, mViews.callTypeIcons.getCallType(index));
        }
        assertEquals(View.VISIBLE, mViews.callTypeIcons.getVisibility());
        assertEquals(overflowText + " Yesterday", mViews.callTypeAndDate.getText().toString());
    }

    /** Sets the phone call details with default values and the given number. */
    private void setPhoneCallDetailsWithNumber(String number, int presentation,
            String formattedNumber) {
        mHelper.setPhoneCallDetails(mViews,
                new PhoneCallDetails(number, presentation, formattedNumber,
                        TEST_COUNTRY_ISO, TEST_GEOCODE,
                        new int[]{ Calls.VOICEMAIL_TYPE }, TEST_DATE, TEST_DURATION),
                true);
    }

    /** Sets the phone call details with default values and the given number. */
    private void setPhoneCallDetailsWithNumberAndGeocode(String number, String formattedNumber,
            String geocodedLocation) {
        mHelper.setPhoneCallDetails(mViews,
                new PhoneCallDetails(number, Calls.PRESENTATION_ALLOWED,
                        formattedNumber, TEST_COUNTRY_ISO, geocodedLocation,
                        new int[]{ Calls.VOICEMAIL_TYPE }, TEST_DATE, TEST_DURATION),
                true);
    }

    /** Sets the phone call details with default values and the given date. */
    private void setPhoneCallDetailsWithDate(long date) {
        mHelper.setPhoneCallDetails(mViews,
                new PhoneCallDetails(TEST_NUMBER, Calls.PRESENTATION_ALLOWED,
                        TEST_FORMATTED_NUMBER, TEST_COUNTRY_ISO, TEST_GEOCODE,
                        new int[]{ Calls.INCOMING_TYPE }, date, TEST_DURATION),
                false);
    }

    /** Sets the phone call details with default values and the given call types using icons. */
    private void setPhoneCallDetailsWithCallTypeIcons(int... callTypes) {
        mHelper.setPhoneCallDetails(mViews,
                new PhoneCallDetails(TEST_NUMBER, Calls.PRESENTATION_ALLOWED,
                        TEST_FORMATTED_NUMBER, TEST_COUNTRY_ISO, TEST_GEOCODE,
                        callTypes, TEST_DATE, TEST_DURATION),
                false);
    }

    private void setCallDetailsHeaderWithNumber(String number, int presentation) {
        mHelper.setCallDetailsHeader(mNameView,
                new PhoneCallDetails(number, presentation,
                        TEST_FORMATTED_NUMBER, TEST_COUNTRY_ISO, TEST_GEOCODE,
                        new int[]{ Calls.INCOMING_TYPE }, TEST_DATE, TEST_DURATION));
    }

    private void setCallDetailsHeader(String name) {
        mHelper.setCallDetailsHeader(mNameView,
                new PhoneCallDetails(TEST_NUMBER, Calls.PRESENTATION_ALLOWED,
                        TEST_FORMATTED_NUMBER, TEST_COUNTRY_ISO, TEST_GEOCODE,
                        new int[]{ Calls.INCOMING_TYPE }, TEST_DATE, TEST_DURATION,
                        name, 0, "", null, null));
    }
}
