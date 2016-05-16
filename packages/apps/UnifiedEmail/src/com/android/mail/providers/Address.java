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
package com.android.mail.providers;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;

import com.android.common.Rfc822Validator;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;
import com.google.common.annotations.VisibleForTesting;

import org.apache.james.mime4j.codec.EncoderUtil;
import org.apache.james.mime4j.decoder.DecoderUtil;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * This class represent email address.
 *
 * RFC822 email address may have following format.
 *   "name" <address> (comment)
 *   "name" <address>
 *   name <address>
 *   address
 * Name and comment part should be MIME/base64 encoded in header if necessary.
 *
 */
public class Address implements Parcelable {
    public static final String ADDRESS_DELIMETER = ",";
    /**
     *  Address part, in the form local_part@domain_part. No surrounding angle brackets.
     */
    private String mAddress;

    /**
     * Name part. No surrounding double quote, and no MIME/base64 encoding.
     * This must be null if Address has no name part.
     */
    private String mName;

    /**
     * When personal is set, it will return the first token of the personal
     * string. Otherwise, it will return the e-mail address up to the '@' sign.
     */
    private String mSimplifiedName;

    // Regex that matches address surrounded by '<>' optionally. '^<?([^>]+)>?$'
    private static final Pattern REMOVE_OPTIONAL_BRACKET = Pattern.compile("^<?([^>]+)>?$");
    // Regex that matches personal name surrounded by '""' optionally. '^"?([^"]+)"?$'
    private static final Pattern REMOVE_OPTIONAL_DQUOTE = Pattern.compile("^\"?([^\"]*)\"?$");
    // Regex that matches escaped character '\\([\\"])'
    private static final Pattern UNQUOTE = Pattern.compile("\\\\([\\\\\"])");

    private static final Address[] EMPTY_ADDRESS_ARRAY = new Address[0];

    // delimiters are chars that do not appear in an email address, used by pack/unpack
    private static final char LIST_DELIMITER_EMAIL = '\1';
    private static final char LIST_DELIMITER_PERSONAL = '\2';

    private static final String LOG_TAG = LogTag.getLogTag();

    public Address(String name, String address) {
        setName(name);
        setAddress(address);
    }



    /**
     * Returns a simplified string for this e-mail address.
     * When a name is known, it will return the first token of that name. Otherwise, it will
     * return the e-mail address up to the '@' sign.
     */
    public String getSimplifiedName() {
        if (mSimplifiedName == null) {
            if (TextUtils.isEmpty(mName) && !TextUtils.isEmpty(mAddress)) {
                int atSign = mAddress.indexOf('@');
                mSimplifiedName = (atSign != -1) ? mAddress.substring(0, atSign) : "";
            } else if (!TextUtils.isEmpty(mName)) {

                // TODO: use Contacts' NameSplitter for more reliable first-name extraction

                int end = mName.indexOf(' ');
                while (end > 0 && mName.charAt(end - 1) == ',') {
                    end--;
                }
                mSimplifiedName = (end < 1) ? mName : mName.substring(0, end);

            } else {
                LogUtils.w(LOG_TAG, "Unable to get a simplified name");
                mSimplifiedName = "";
            }
        }
        return mSimplifiedName;
    }

    public static synchronized Address getEmailAddress(String rawAddress) {
        if (TextUtils.isEmpty(rawAddress)) {
            return null;
        }
        String name, address;
        final Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(rawAddress);
        if (tokens.length > 0) {
            final String tokenizedName = tokens[0].getName();
            name = tokenizedName != null ? Utils.convertHtmlToPlainText(tokenizedName.trim())
                    .toString() : "";
            address = Utils.convertHtmlToPlainText(tokens[0].getAddress()).toString();
        } else {
            name = "";
            address = rawAddress == null ?
                    "" : Utils.convertHtmlToPlainText(rawAddress).toString();
        }
        return new Address(name, address);
    }

    public Address(String address) {
        setAddress(address);
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        mAddress = REMOVE_OPTIONAL_BRACKET.matcher(address).replaceAll("$1");
    }

    /**
     * Get name part as UTF-16 string. No surrounding double quote, and no MIME/base64 encoding.
     *
     * @return Name part of email address. Returns null if it is omitted.
     */
    public String getName() {
        return mName;
    }

    /**
     * Set name part from UTF-16 string. Optional surrounding double quote will be removed.
     * It will be also unquoted and MIME/base64 decoded.
     *
     * @param name name part of email address as UTF-16 string. Null is acceptable.
     */
    public void setName(String name) {
        mName = decodeAddressName(name);
    }

    /**
     * Decodes name from UTF-16 string. Optional surrounding double quote will be removed.
     * It will be also unquoted and MIME/base64 decoded.
     *
     * @param name name part of email address as UTF-16 string. Null is acceptable.
     */
    public static String decodeAddressName(String name) {
        if (name != null) {
            name = REMOVE_OPTIONAL_DQUOTE.matcher(name).replaceAll("$1");
            name = UNQUOTE.matcher(name).replaceAll("$1");
            name = DecoderUtil.decodeEncodedWords(name);
            if (name.length() == 0) {
                name = null;
            }
        }
        return name;
    }

    /**
     * This method is used to check that all the addresses that the user
     * entered in a list (e.g. To:) are valid, so that none is dropped.
     */
    public static boolean isAllValid(String addressList) {
        // This code mimics the parse() method below.
        // I don't know how to better avoid the code-duplication.
        if (addressList != null && addressList.length() > 0) {
            Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(addressList);
            for (int i = 0, length = tokens.length; i < length; ++i) {
                Rfc822Token token = tokens[i];
                String address = token.getAddress();
                if (!TextUtils.isEmpty(address) && !isValidAddress(address)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Parse a comma-delimited list of addresses in RFC822 format and return an
     * array of Address objects.
     *
     * @param addressList Address list in comma-delimited string.
     * @return An array of 0 or more Addresses.
     */
    public static Address[] parse(String addressList) {
        if (addressList == null || addressList.length() == 0) {
            return EMPTY_ADDRESS_ARRAY;
        }
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(addressList);
        ArrayList<Address> addresses = new ArrayList<Address>();
        for (int i = 0, length = tokens.length; i < length; ++i) {
            Rfc822Token token = tokens[i];
            String address = token.getAddress();
            if (!TextUtils.isEmpty(address)) {
                if (isValidAddress(address)) {
                    String name = token.getName();
                    if (TextUtils.isEmpty(name)) {
                        name = null;
                    }
                    addresses.add(new Address(name, address));
                }
            }
        }
        return addresses.toArray(new Address[] {});
    }

    /**
     * Checks whether a string email address is valid.
     * E.g. name@domain.com is valid.
     */
    @VisibleForTesting
    static boolean isValidAddress(String address) {
        if (TextUtils.isEmpty(address)) {
            return false;
        }
        int index = address.indexOf("@");
        return index == -1 ? false : new Rfc822Validator(address.substring(0, index))
                .isValid(address);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Address) {
            // It seems that the spec says that the "user" part is case-sensitive,
            // while the domain part in case-insesitive.
            // So foo@yahoo.com and Foo@yahoo.com are different.
            // This may seem non-intuitive from the user POV, so we
            // may re-consider it if it creates UI trouble.
            // A problem case is "replyAll" sending to both
            // a@b.c and to A@b.c, which turn out to be the same on the server.
            // Leave unchanged for now (i.e. case-sensitive).
            return getAddress().equals(((Address) o).getAddress());
        }
        return super.equals(o);
    }

    /**
     * Get human readable address string.
     * Do not use this for email header.
     *
     * @return Human readable address string.  Not quoted and not encoded.
     */
    @Override
    public String toString() {
        if (mName != null && !mName.equals(mAddress)) {
            if (mName.matches(".*[\\(\\)<>@,;:\\\\\".\\[\\]].*")) {
                return Utils.ensureQuotedString(mName) + " <" + mAddress + ">";
            } else {
                return mName + " <" + mAddress + ">";
            }
        } else {
            return mAddress;
        }
    }

    /**
     * Get human readable comma-delimited address string.
     *
     * @param addresses Address array
     * @return Human readable comma-delimited address string.
     */
    public static String toString(Address[] addresses) {
        return toString(addresses, ADDRESS_DELIMETER);
    }

    /**
     * Get human readable address strings joined with the specified separator.
     *
     * @param addresses Address array
     * @param separator Separator
     * @return Human readable comma-delimited address string.
     */
    public static String toString(Address[] addresses, String separator) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        if (addresses.length == 1) {
            return addresses[0].toString();
        }
        StringBuffer sb = new StringBuffer(addresses[0].toString());
        for (int i = 1; i < addresses.length; i++) {
            sb.append(separator);
            // TODO: investigate why this .trim() is needed.
            sb.append(addresses[i].toString().trim());
        }
        return sb.toString();
    }

    /**
     * Get RFC822/MIME compatible address string.
     *
     * @return RFC822/MIME compatible address string.
     * It may be surrounded by double quote or quoted and MIME/base64 encoded if necessary.
     */
    public String toHeader() {
        if (mName != null) {
            return EncoderUtil.encodeAddressDisplayName(mName) + " <" + mAddress + ">";
        } else {
            return mAddress;
        }
    }

    /**
     * Get RFC822/MIME compatible comma-delimited address string.
     *
     * @param addresses Address array
     * @return RFC822/MIME compatible comma-delimited address string.
     * it may be surrounded by double quoted or quoted and MIME/base64 encoded if necessary.
     */
    public static String toHeader(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        if (addresses.length == 1) {
            return addresses[0].toHeader();
        }
        StringBuffer sb = new StringBuffer(addresses[0].toHeader());
        for (int i = 1; i < addresses.length; i++) {
            // We need space character to be able to fold line.
            sb.append(", ");
            sb.append(addresses[i].toHeader());
        }
        return sb.toString();
    }

    /**
     * Get Human friendly address string.
     *
     * @return the personal part of this Address, or the address part if the
     * personal part is not available
     */
    public String toFriendly() {
        if (mName != null && mName.length() > 0) {
            return mName;
        } else {
            return mAddress;
        }
    }

    /**
     * Creates a comma-delimited list of addresses in the "friendly" format (see toFriendly() for
     * details on the per-address conversion).
     *
     * @param addresses Array of Address[] values
     * @return A comma-delimited string listing all of the addresses supplied.  Null if source
     * was null or empty.
     */
    public static String toFriendly(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        if (addresses.length == 1) {
            return addresses[0].toFriendly();
        }
        StringBuffer sb = new StringBuffer(addresses[0].toFriendly());
        for (int i = 1; i < addresses.length; i++) {
            sb.append(", ");
            sb.append(addresses[i].toFriendly());
        }
        return sb.toString();
    }

    /**
     * Returns exactly the same result as Address.toString(Address.unpack(packedList)).
     */
    public static String unpackToString(String packedList) {
        return toString(unpack(packedList));
    }

    /**
     * Returns exactly the same result as Address.pack(Address.parse(textList)).
     */
    public static String parseAndPack(String textList) {
        return Address.pack(Address.parse(textList));
    }

    /**
     * Returns null if the packedList has 0 addresses, otherwise returns the first address.
     * The same as Address.unpack(packedList)[0] for non-empty list.
     * This is an utility method that offers some performance optimization opportunities.
     */
    public static Address unpackFirst(String packedList) {
        Address[] array = unpack(packedList);
        return array.length > 0 ? array[0] : null;
    }

    /**
     * Convert a packed list of addresses to a form suitable for use in an RFC822 header.
     * This implementation is brute-force, and could be replaced with a more efficient version
     * if desired.
     */
    public static String packedToHeader(String packedList) {
        return toHeader(unpack(packedList));
    }

    /**
     * Unpacks an address list previously packed with pack()
     * @param addressList String with packed addresses as returned by pack()
     * @return array of addresses resulting from unpack
     */
    public static Address[] unpack(String addressList) {
        if (addressList == null || addressList.length() == 0) {
            return EMPTY_ADDRESS_ARRAY;
        }
        ArrayList<Address> addresses = new ArrayList<Address>();
        int length = addressList.length();
        int pairStartIndex = 0;
        int pairEndIndex = 0;

        /* addressEndIndex is only re-scanned (indexOf()) when a LIST_DELIMITER_PERSONAL
           is used, not for every email address; i.e. not for every iteration of the while().
           This reduces the theoretical complexity from quadratic to linear,
           and provides some speed-up in practice by removing redundant scans of the string.
        */
        int addressEndIndex = addressList.indexOf(LIST_DELIMITER_PERSONAL);

        while (pairStartIndex < length) {
            pairEndIndex = addressList.indexOf(LIST_DELIMITER_EMAIL, pairStartIndex);
            if (pairEndIndex == -1) {
                pairEndIndex = length;
            }
            Address address;
            if (addressEndIndex == -1 || pairEndIndex <= addressEndIndex) {
                // in this case the DELIMITER_PERSONAL is in a future pair,
                // so don't use personal, and don't update addressEndIndex
                address = new Address(null, addressList.substring(pairStartIndex, pairEndIndex));
            } else {
                address = new Address(addressList.substring(addressEndIndex + 1, pairEndIndex),
                        addressList.substring(pairStartIndex, addressEndIndex));
                // only update addressEndIndex when we use the LIST_DELIMITER_PERSONAL
                addressEndIndex = addressList.indexOf(LIST_DELIMITER_PERSONAL, pairEndIndex + 1);
            }
            addresses.add(address);
            pairStartIndex = pairEndIndex + 1;
        }
        return addresses.toArray(EMPTY_ADDRESS_ARRAY);
    }

    /**
     * Packs an address list into a String that is very quick to read
     * and parse. Packed lists can be unpacked with unpack().
     * The format is a series of packed addresses separated by LIST_DELIMITER_EMAIL.
     * Each address is packed as
     * a pair of address and personal separated by LIST_DELIMITER_PERSONAL,
     * where the personal and delimiter are optional.
     * E.g. "foo@x.com\1joe@x.com\2Joe Doe"
     * @param addresses Array of addresses
     * @return a string containing the packed addresses.
     */
    public static String pack(Address[] addresses) {
        // TODO: return same value for both null & empty list
        if (addresses == null) {
            return null;
        }
        final int nAddr = addresses.length;
        if (nAddr == 0) {
            return "";
        }

        // shortcut: one email with no displayName
        if (nAddr == 1 && addresses[0].getName() == null) {
            return addresses[0].getAddress();
        }

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < nAddr; i++) {
            if (i != 0) {
                sb.append(LIST_DELIMITER_EMAIL);
            }
            final Address address = addresses[i];
            sb.append(address.getAddress());
            final String displayName = address.getName();
            if (displayName != null) {
                sb.append(LIST_DELIMITER_PERSONAL);
                sb.append(displayName);
            }
        }
        return sb.toString();
    }

    /**
     * Produces the same result as pack(array), but only packs one (this) address.
     */
    public String pack() {
        final String address = getAddress();
        final String personal = getName();
        if (personal == null) {
            return address;
        } else {
            return address + LIST_DELIMITER_PERSONAL + personal;
        }
    }

    public static final Creator<Address> CREATOR = new Creator<Address>() {
        @Override
        public Address createFromParcel(Parcel parcel) {
            return new Address(parcel);
        }

        @Override
        public Address[] newArray(int size) {
            return new Address[size];
        }
    };

    public Address(Parcel in) {
        setName(in.readString());
        setAddress(in.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mName);
        out.writeString(mAddress);
    }
}
