/**
 * Copyright (c) 2011, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mail.utils;

import android.content.res.Resources;
import android.text.TextUtils;

import com.android.mail.R;
import com.android.mail.providers.Account;
import com.android.mail.providers.AccountObserver;
import com.android.mail.ui.AccountController;

import java.util.regex.Pattern;

/**
 * A veiled email address is where we don't want to display the email address, because the address
 * might be throw-away, or temporary. For these veiled addresses, we want to display some alternate
 * information. To find if an email address is veiled, call the method
 * {@link #isVeiledAddress(String)}
 */
public final class VeiledAddressMatcher{
    /**
     * Resource for the regex pattern that specifies a veiled addresses.
     */
    private static final int VEILED_RESOURCE = R.string.veiled_address;

    /**
     * Resource that specifies whether veiled address matching is enabled.
     */
    private static final int VEILED_MATCHING_ENABLED = R.bool.veiled_address_enabled;

    /**
     * Similar to {@link #VEILED_ALTERNATE_TEXT} except this is for addresses where we don't have
     * the name corresponding to the veiled address. Since we don't show the address, we should
     * indicate that the recipient is unknown to us.
     */
    public static final int VEILED_ALTERNATE_TEXT_UNKNOWN_PERSON =
            R.string.veiled_alternate_text_unknown_person;

    /**
     * When we show a veiled address, we should show an alternate string rather than the email
     * address. This is the resource that specifies the alternate string.
     */
    public static final int VEILED_ALTERNATE_TEXT = R.string.veiled_alternate_text;

    /**
     * A summary string (short-string) for an unknown veiled recipient.
     */
    public static final int VEILED_SUMMARY_UNKNOWN = R.string.veiled_summary_unknown_person;

    /**
     * Private object that does the actual matching.
     */
    private Pattern mMatcher = null;

    /**
     * True if veiled address matching is enabled, false otherwise.
     */
    protected boolean mVeiledMatchingEnabled = false;

    /**
     * The hash code of the last profile pattern retrieved . This allows us to avoid recompiling the
     * patterns when nothing has changed.
     */
    private int mProfilePatternLastHash = -1;

    private final AccountObserver mObserver = new AccountObserver() {
        @Override
        public void onChanged(Account newAccount) {
            loadPattern(newAccount.settings.veiledAddressPattern);
        }
    };

    /**
     * Make instantiation impossible.
     */
    private VeiledAddressMatcher() {
        // Do nothing.
    }

    /**
     * Loads the regular expression that corresponds to veiled addresses. It is safe to call this
     * method repeatedly with the same pattern. If the pattern has not changed, little extra work
     * is done.
     * @param pattern
     */
    private final void loadPattern(String pattern) {
        if (!TextUtils.isEmpty(pattern)) {
            final int hashCode = pattern.hashCode();
            if (hashCode != mProfilePatternLastHash) {
                mProfilePatternLastHash = hashCode;
                mMatcher = Pattern.compile(pattern);
                // Since we have a non-empty pattern now, enable pattern matching.
                mVeiledMatchingEnabled = true;
            }
        }
    }

    /**
     * Default constructor
     * @return
     */
    public static final VeiledAddressMatcher newInstance(Resources resources) {
        final VeiledAddressMatcher instance = new VeiledAddressMatcher();
        instance.mVeiledMatchingEnabled = resources.getBoolean(VEILED_MATCHING_ENABLED);
        if (instance.mVeiledMatchingEnabled) {
            instance.loadPattern(resources.getString(VEILED_RESOURCE));
        }
        return instance;
    }

    /**
     * Initialize the object to listen for account changes. Without this, we cannot obtain updated
     * values of the veiled address pattern and the value is read once from resources.
     * @param controller
     */
    public final void initialize(AccountController controller) {
        mObserver.initialize(controller);
    }

    /**
     * Returns true if the given email address is a throw-away (or veiled) address. Such addresses
     * are created using special server-side logic for the purpose of keeping the real address of
     * the user hidden.
     * @param address
     * @return true if the address is veiled, false otherwise.
     */
    public final boolean isVeiledAddress (String address) {
        if (!mVeiledMatchingEnabled || mMatcher == null) {
            // Veiled address matching is explicitly disabled: Match nothing.
            return false;
        }
        return mMatcher.matcher(address).matches();
    }
}
