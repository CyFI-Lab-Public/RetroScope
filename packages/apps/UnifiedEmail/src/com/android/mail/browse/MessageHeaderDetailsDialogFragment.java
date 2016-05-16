/*
 * Copyright (C) 2013 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.browse;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;

import com.android.mail.R;
import com.android.mail.providers.Account;
import com.android.mail.providers.Address;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link DialogFragment} used by secure conversation views to display
 * the expanded message details as a dialog.
 */
public class MessageHeaderDetailsDialogFragment extends DialogFragment {

    private static final String ARG_ACCOUNT = "account";
    private static final String ARG_ADDRESS_CACHE = "addresses";
    private static final String ARG_FROM = "from";
    private static final String ARG_REPLY_TO = "replyto";
    private static final String ARG_TO = "to";
    private static final String ARG_CC = "cc";
    private static final String ARG_BCC = "bcc";
    private static final String ARG_RECEIVED_TIME = "received-timestamp";

    // Public no-args constructor needed for fragment re-instantiation
    public MessageHeaderDetailsDialogFragment() {}

    /**
     * Creates a new {@link MessageHeaderDetailsDialogFragment}.
     * @param addressCache a mapping of RFC822 addresses as strings to {@link Address}.
     * @param account {@link Account} used as the from address for any messages created
     *                by tapping an email address.
     * @param from from addresses for the message
     * @param replyTo replyTo addresses for the message
     * @param to to addresses for the message
     * @param cc cc addresses for the message
     * @param bcc bcc addresses for the message
     * @return a newly created {@link MessageHeaderDetailsDialogFragment}
     */
    public static MessageHeaderDetailsDialogFragment newInstance(
            Map<String, Address> addressCache, Account account, String[] from, String[] replyTo,
            String[] to, String[] cc, String[] bcc, CharSequence receivedTimestamp) {
        final MessageHeaderDetailsDialogFragment f = new MessageHeaderDetailsDialogFragment();

        // Supply needed items as arguments
        final Bundle args = new Bundle(7);
        args.putParcelable(ARG_ACCOUNT, account);

        final Bundle addresses = new Bundle();
        addAddressesToBundle(addresses, addressCache, from);
        addAddressesToBundle(addresses, addressCache, replyTo);
        addAddressesToBundle(addresses, addressCache, to);
        addAddressesToBundle(addresses, addressCache, cc);
        addAddressesToBundle(addresses, addressCache, bcc);
        args.putBundle(ARG_ADDRESS_CACHE, addresses);

        args.putStringArray(ARG_FROM, from);
        args.putStringArray(ARG_REPLY_TO, replyTo);
        args.putStringArray(ARG_TO, to);
        args.putStringArray(ARG_CC, cc);
        args.putStringArray(ARG_BCC, bcc);
        args.putCharSequence(ARG_RECEIVED_TIME, receivedTimestamp);
        f.setArguments(args);

        return f;
    }

    private static void addAddressesToBundle(
            Bundle addresses, Map<String, Address> addressCache, String[] emails) {
        for (final String email : emails) {
            addresses.putParcelable(email, MessageHeaderView.getAddress(addressCache, email));
        }
    }

    @Override
    public Dialog onCreateDialog(final Bundle onSavedInstanceState) {
        final Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final View expandedDetails = MessageHeaderView.inflateExpandedDetails(
                LayoutInflater.from(context));

        final Bundle args = getArguments();

        // turn bundle back into Map<String, Address>
        final Bundle addresses = args.getBundle(ARG_ADDRESS_CACHE);
        final Map<String, Address> addressCache = new HashMap<String, Address>();
        for (String email : addresses.keySet()) {
            addressCache.put(email, (Address) addresses.getParcelable(email));
        }

        MessageHeaderView.renderExpandedDetails(getResources(), expandedDetails, null,
                addressCache, (Account) args.getParcelable(ARG_ACCOUNT), null,
                args.getStringArray(ARG_FROM), args.getStringArray(ARG_REPLY_TO),
                args.getStringArray(ARG_TO), args.getStringArray(ARG_CC),
                args.getStringArray(ARG_BCC), args.getCharSequence(ARG_RECEIVED_TIME));

        expandedDetails.findViewById(R.id.details_expander)
                .setVisibility(View.GONE);
        builder.setView(expandedDetails)
                .setCancelable(true)
                .setTitle(context.getString(R.string.message_details_title));
        return builder.create();
    }
}
