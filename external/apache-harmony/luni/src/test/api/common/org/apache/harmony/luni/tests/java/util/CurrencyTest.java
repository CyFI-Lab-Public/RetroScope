/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.luni.tests.java.util;

import java.util.Currency;
import java.util.Locale;

public class CurrencyTest extends junit.framework.TestCase {

    private static Locale defaultLocale = Locale.getDefault();

    /**
     * @tests java.util.Currency#getInstance(java.lang.String)
     */
    public void test_getInstanceLjava_lang_String() {
        // see test_getInstanceLjava_util_Locale() tests
    }

    /**
     * @tests java.util.Currency#getInstance(java.util.Locale)
     */
    public void test_getInstanceLjava_util_Locale() {
        /*
         * the behaviour in all these three cases should be the same since this
         * method ignores language and variant component of the locale.
         */
        Currency c0 = Currency.getInstance("CAD");
        Currency c1 = Currency.getInstance(new Locale("en", "CA"));
        assertTrue(
                "Currency.getInstance(new Locale(\"en\",\"CA\")) isn't equal to Currency.getInstance(\"CAD\")",
                c1 == c0);
        Currency c2 = Currency.getInstance(new Locale("fr", "CA"));
        assertTrue(
                "Currency.getInstance(new Locale(\"fr\",\"CA\")) isn't equal to Currency.getInstance(\"CAD\")",
                c2 == c0);
        Currency c3 = Currency.getInstance(new Locale("", "CA"));
        assertTrue(
                "Currency.getInstance(new Locale(\"\",\"CA\")) isn't equal to Currency.getInstance(\"CAD\")",
                c3 == c0);

        c0 = Currency.getInstance("JPY");
        c1 = Currency.getInstance(new Locale("ja", "JP"));
        assertTrue(
                "Currency.getInstance(new Locale(\"ja\",\"JP\")) isn't equal to Currency.getInstance(\"JPY\")",
                c1 == c0);
        c2 = Currency.getInstance(new Locale("", "JP"));
        assertTrue(
                "Currency.getInstance(new Locale(\"\",\"JP\")) isn't equal to Currency.getInstance(\"JPY\")",
                c2 == c0);
        c3 = Currency.getInstance(new Locale("bogus", "JP"));
        assertTrue(
                "Currency.getInstance(new Locale(\"bogus\",\"JP\")) isn't equal to Currency.getInstance(\"JPY\")",
                c3 == c0);

        Locale localeGu = new Locale("gu", "IN");
        Currency cGu = Currency.getInstance(localeGu);
        Locale localeKn = new Locale("kn", "IN");
        Currency cKn = Currency.getInstance(localeKn);
        assertTrue("Currency.getInstance(Locale_" + localeGu.toString() + "))"
                + "isn't equal to " + "Currency.getInstance(Locale_"
                + localeKn.toString() + "))", cGu == cKn);

        // some territories do not have currencies, like Antarctica(AQ)
        Locale loc = new Locale("", "AQ");
        Currency curr = Currency.getInstance(loc);
        assertNull(curr);
        
        try {
            Currency.getInstance((Locale) null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        
        try {
            Currency.getInstance(new Locale("ABC","DEF"));
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        // unsupported/legacy iso3 countries
        // RI does not support them.
        loc = new Locale("", "ZR");
        Currency.getInstance(loc);

        loc = new Locale("", "ZAR");
        Currency.getInstance(loc);

        // loc = new Locale("", "FX");
        // Currency.getInstance(loc);

        // loc = new Locale("", "FXX");
        // Currency.getInstance(loc);
    }

    /**
     * @tests java.util.Currency#getSymbol()
     */
    public void test_getSymbol() {

        Currency currK = Currency.getInstance("KRW");
        Currency currI = Currency.getInstance("INR");
        Currency currUS = Currency.getInstance("USD");

        Locale.setDefault(Locale.US);
        assertEquals("currK.getSymbol()", "\u20a9", currK.getSymbol());
        // Some non-bug differences below because of CLDR data.
        assertEquals("currI.getSymbol()", "Rs", currI
                .getSymbol());
        assertEquals("currUS.getSymbol()", "$", currUS.getSymbol());

        Locale.setDefault(new Locale("ko", "KR"));
        assertEquals("currK.getSymbol()", "\u20a9", currK.getSymbol());
        assertEquals("currI.getSymbol()", "Rs", currI
                .getSymbol());
        assertEquals("currUS.getSymbol()", "US$", currUS.getSymbol());

        // test what happens if this is an invalid locale,
        // one with Korean country but an India language
        // this method should return the currency codes in that case
        Locale.setDefault(new Locale("kr", "KR"));
        assertEquals("currK.getSymbol()", "\u20a9", currK.getSymbol());
        assertEquals("currI.getSymbol()", "Rs", currI
                .getSymbol());
        assertEquals("currUS.getSymbol()", "US$", currUS.getSymbol());
    }

    /**
     * @tests java.util.Currency#getSymbol(java.util.Locale)
     */
    public void test_getSymbolLjava_util_Locale() {
        Locale.setDefault(Locale.US);
        Currency currE = Currency.getInstance("EUR");
        // Some non-bug differences below because of CLDR data.
        assertEquals("\u20ac", currE.getSymbol(Locale.JAPAN));
        assertEquals("EUR", currE.getSymbol(Locale.JAPANESE));
        assertEquals("\u20ac", currE.getSymbol(new Locale("", "FR")));
        assertEquals("\u20ac", currE.getSymbol(Locale.FRANCE));
        assertEquals("EUR", currE.getSymbol(Locale.FRENCH));

        Currency currJ = Currency.getInstance("JPY");
        assertEquals("\uffe5", currJ.getSymbol(Locale.JAPAN));
        assertEquals("JPY", currJ.getSymbol(Locale.JAPANESE));
        assertEquals("\u00a5JP", currJ.getSymbol(Locale.FRANCE));
        assertEquals("JPY", currJ.getSymbol(Locale.FRENCH));

        Currency currUS = Currency.getInstance("USD");
        assertEquals("$", currUS.getSymbol(Locale.JAPAN));

        Locale.setDefault(new Locale("ja", "JP"));
        assertEquals("JP\u00a5", currJ.getSymbol(new Locale("", "JP")));
        assertEquals("US$", currUS.getSymbol(new Locale("", "JP")));

        Locale.setDefault(Locale.US);
        assertEquals("JP\u00a5", currJ.getSymbol(new Locale("", "JP")));
        assertEquals("US$", currUS.getSymbol(new Locale("", "JP")));

        assertEquals("USD", currUS.getSymbol(Locale.JAPANESE));
        assertEquals("$US", currUS.getSymbol(Locale.FRANCE));
        assertEquals("USD", currUS.getSymbol(Locale.FRENCH));
        assertEquals("$US", currUS.getSymbol(new Locale("fr", "FR")));
        assertEquals("US$", currUS.getSymbol(new Locale("", "FR"))); // default
        // locale

        assertEquals("$", currUS.getSymbol(Locale.US));
        assertEquals("USD", currUS.getSymbol(Locale.ENGLISH));

        assertEquals("$", currUS.getSymbol(new Locale("en", "US")));
        assertEquals("US$", currUS.getSymbol(new Locale("", "US")));

        Currency currCA = Currency.getInstance("CAD");
        assertEquals("CA$", currCA.getSymbol(Locale.JAPAN));
        assertEquals("CAD", currCA.getSymbol(Locale.JAPANESE));
        assertEquals("$CA", currCA.getSymbol(Locale.FRANCE));
        assertEquals("CAD", currCA.getSymbol(Locale.FRENCH));
        assertEquals("CA$", currCA.getSymbol(Locale.US));
        assertEquals("CAD", currCA.getSymbol(Locale.ENGLISH));
        assertEquals("CA$", currCA.getSymbol(new Locale("es", "US")));
        assertEquals("CA$", currCA.getSymbol(new Locale("en", "US")));

        assertEquals("$", currCA.getSymbol(Locale.CANADA));
        assertEquals("$", currCA.getSymbol(Locale.CANADA_FRENCH));
        assertEquals("$", currCA.getSymbol(new Locale("en", "CA")));
        assertEquals("$", currCA.getSymbol(new Locale("fr", "CA")));
        assertEquals("CA$", currCA.getSymbol(new Locale("", "CA")));

        // tests what happens with improper locales, i.e. countries without the
        // given language
        assertEquals("currUS.getSymbol(new Locale(\"ar\", \"US\"))", "US$",
                currUS.getSymbol(new Locale("ar", "US")));
        assertEquals("currUS.getSymbol(new Locale(\"ar\", \"CA\"))", "US$",
                currUS.getSymbol(new Locale("ar", "CA")));
        assertEquals("currCA.getSymbol(new Locale(\"ar\", \"US\"))", "CA$",
                currCA.getSymbol(new Locale("ar", "US")));
        assertEquals("currCA.getSymbol(new Locale(\"ar\", \"CA\"))", "CA$",
                currCA.getSymbol(new Locale("ar", "CA")));
        assertEquals("currJ.getSymbol(new Locale(\"ja\", \"US\"))", "\uffe5",
                currJ.getSymbol(new Locale("ja", "US")));
        assertEquals("currUS.getSymbol(new Locale(\"ja\", \"US\"))", "$",
                currUS.getSymbol(new Locale("ja", "US")));

        // cross testing between USD and JPY when locale is JAPANESE JAPAN

        // set default locale to Locale_ja_JP
        Locale.setDefault(new Locale("ja", "JP"));

        Currency currJ2 = Currency.getInstance("JPY");
        Currency currUS2 = Currency.getInstance("USD");

        // the real JAPAN locale
        assertEquals("\uffe5", currJ2.getSymbol(new Locale("ja", "JP")));

        // no language
        assertEquals("JP\u00a5", currJ2.getSymbol(new Locale("", "JP")));

        // no country
        assertEquals("JPY", currJ2.getSymbol(new Locale("ja", "")));

        // no language
        assertEquals("JP\u00a5", currJ2.getSymbol(new Locale("", "US")));

        // no country
        assertEquals("JPY", currJ2.getSymbol(new Locale("en", "")));

        // bogus Locales , when default locale is Locale_ja_JP
        assertEquals("JP\u00a5", currJ2.getSymbol(new Locale("ar", "JP")));
        assertEquals("JP\u00a5", currJ2.getSymbol(new Locale("ar", "US")));
        assertEquals("\uffe5", currJ2.getSymbol(new Locale("ja", "AE")));
        assertEquals("\u00a5", currJ2.getSymbol(new Locale("en", "AE")));
        assertEquals("currJ.getSymbol(new Locale(\"ja\", \"US\"))", "\uffe5",
                currJ.getSymbol(new Locale("ja", "US")));

        // the real US locale
        assertEquals("$", currUS2.getSymbol(new Locale("en", "US")));

        // no country
        assertEquals("USD", currUS2.getSymbol(new Locale("ja", "")));

        // no language
        assertEquals("US$", currUS2.getSymbol(new Locale("", "JP")));

        // no language
        assertEquals("US$", currUS2.getSymbol(new Locale("", "US")));

        // no country
        assertEquals("USD", currUS2.getSymbol(new Locale("en", "")));

        // bogus Locales , when default locale is Locale_ja_JP
        assertEquals("US$", currUS2.getSymbol(new Locale("ar", "JP")));
        assertEquals("US$", currUS2.getSymbol(new Locale("ar", "US")));
        assertEquals("$", currUS2.getSymbol(new Locale("ja", "AE")));
        assertEquals("$", currUS2.getSymbol(new Locale("en", "AE")));
        assertEquals("currUS.getSymbol(new Locale(\"ja\", \"US\"))", "$",
                currUS.getSymbol(new Locale("ja", "US")));

        Locale.setDefault(Locale.US);

        // euro tests
        Currency currDKK = Currency.getInstance("DKK");
        assertEquals("\u20ac", currE.getSymbol(new Locale("da", "DK")));
        assertEquals("kr", currDKK.getSymbol(new Locale("da", "DK")));

        assertEquals("EUR", currE.getSymbol(new Locale("da", "")));
        assertEquals("DKK", currDKK.getSymbol(new Locale("da", "")));

        assertEquals("\u20ac", currE.getSymbol(new Locale("", "DK")));
        assertEquals("Dkr", currDKK.getSymbol(new Locale("", "DK")));

        Locale.setDefault(new Locale("da", "DK"));
        assertEquals("\u20ac", currE.getSymbol(new Locale("da", "DK")));
        assertEquals("kr", currDKK.getSymbol(new Locale("da", "DK")));

        assertEquals("EUR", currE.getSymbol(new Locale("da", "")));
        assertEquals("DKK", currDKK.getSymbol(new Locale("da", "")));

        assertEquals("\u20ac", currE.getSymbol(new Locale("", "DK")));
        assertEquals("Dkr", currDKK.getSymbol(new Locale("", "DK")));

        assertEquals("\u20ac", currE.getSymbol(new Locale("ar", "AE")));
        assertEquals("Dkr", currDKK.getSymbol(new Locale("ar", "AE")));
    }

    /**
     * @tests java.util.Currency#getDefaultFractionDigits()
     */
    public void test_getDefaultFractionDigits() {
        Currency c1 = Currency.getInstance("EUR");
        c1.getDefaultFractionDigits();
        assertEquals(" Currency.getInstance(\"" + c1
                + "\") returned incorrect number of digits. ", 2, c1
                .getDefaultFractionDigits());

        Currency c2 = Currency.getInstance("JPY");
        c2.getDefaultFractionDigits();
        assertEquals(" Currency.getInstance(\"" + c2
                + "\") returned incorrect number of digits. ", 0, c2
                .getDefaultFractionDigits());

        Currency c3 = Currency.getInstance("XBD");
        // A non-bug differences below because of CLDR data.
        c3.getDefaultFractionDigits();
        assertEquals(" Currency.getInstance(\"" + c3
                + "\") returned incorrect number of digits. ", 2, c3
                .getDefaultFractionDigits());

    }

    protected void setUp() {
        Locale.setDefault(defaultLocale);
    }

    protected void tearDown() {
    }

    /**
     * Helper method to display Currency info
     * 
     * @param c
     */
    private void printCurrency(Currency c) {
        System.out.println();
        System.out.println(c.getCurrencyCode());
        System.out.println(c.getSymbol());
        System.out.println(c.getDefaultFractionDigits());
    }

    /**
     * helper method to display Locale info
     */
    private static void printLocale(Locale loc) {
        System.out.println();
        System.out.println(loc.getDisplayName());
        System.out.println(loc.getCountry());
        System.out.println(loc.getLanguage());
        System.out.println(loc.getDisplayCountry());
        System.out.println(loc.getDisplayLanguage());
        System.out.println(loc.getDisplayName());
        System.out.println(loc.getISO3Country());
        System.out.println(loc.getISO3Language());
    }
}
