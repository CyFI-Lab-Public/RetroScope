/*******************************************************************************
 *      Copyright (C) 2013 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.analytics;

import com.android.mail.R;

public class AnalyticsUtils {

    /**
     * Map of email address suffixes to tags sent to analytics.
     */
    private static final String[][] SUFFIX_ACCOUNT_TYPES = {
        {"@gmail.com", "gmail"},
        {"@googlemail.com", "gmail"},
        {"@google.com", "google-corp"},
        {"@hotmail.com", "hotmail"},
        {"@outlook.com", "outlook"},
        {"@yahoo.com", "yahoo"},
    };

    // individual apps should chain this method call with their own lookup tables if they have
    // app-specific menu items
    public static String getMenuItemString(int id) {
        final String s;
        if (id == R.id.archive) {
            s = "archive";
        } else if (id == R.id.remove_folder) {
            s = "remove_folder";
        } else if (id == R.id.delete) {
            s = "delete";
        } else if (id == R.id.discard_drafts) {
            s = "discard_drafts";
        } else if (id == R.id.mark_important) {
            s = "mark important";
        } else if (id == R.id.mark_not_important) {
            s = "mark not important";
        } else if (id == R.id.mute) {
            s = "mute";
        } else if (id == R.id.report_phishing) {
            s = "report_phishing";
        } else if (id == R.id.report_spam) {
            s = "report_spam";
        } else if (id == R.id.mark_not_spam) {
            s = "mark_not_spam";
        } else if (id == R.id.report_phishing) {
            s = "report_phishing";
        } else if (id == R.id.compose) {
            s = "compose";
        } else if (id == R.id.refresh) {
            s = "refresh";
        } else if (id == R.id.settings) {
            s = "settings";
        } else if (id == R.id.folder_options) {
            s = "folder_options";
        } else if (id == R.id.help_info_menu_item) {
            s = "help";
        } else if (id == R.id.feedback_menu_item) {
            s = "feedback";
        } else if (id == R.id.manage_folders_item) {
            s = "manage_folders";
        } else if (id == R.id.move_to) {
            s = "move_to";
        } else if (id == R.id.change_folders) {
            s = "change_folders";
        } else if (id == R.id.move_to_inbox) {
            s = "move_to_inbox";
        } else if (id == R.id.empty_trash) {
            s = "empty_trash";
        } else if (id == R.id.empty_spam) {
            s = "empty_spam";
        } else if (id == android.R.id.home) {
            s = "home";
        } else if (id == R.id.inside_conversation_unread) {
            s = "inside_conversation_unread";
        } else if (id == R.id.read) {
            s = "mark_read";
        } else if (id == R.id.unread) {
            s = "mark_unread";
        } else if (id == R.id.show_original) {
            s = "show_original";
        } else if (id == R.id.add_photo_attachment) {
            s = "add_photo_attachment";
        } else if (id == R.id.add_video_attachment) {
            s = "add_video_attachment";
        } else if (id == R.id.add_cc_bcc) {
            s = "add_cc_bcc";
        } else if (id == R.id.save) {
            s = "save_draft";
        } else if (id == R.id.send) {
            s = "send_message";
        } else if (id == R.id.discard) {
            s = "compose_discard_draft";
        } else if (id == R.id.search) {
            s = "search";
        } else {
            s = null;
        }
        return s;
    }

    public static String getAccountTypeForAccount(String name) {
        if (name == null) {
            return "unknown";
        }

        for (int i = 0; i < SUFFIX_ACCOUNT_TYPES.length; i++) {
            final String[] row = SUFFIX_ACCOUNT_TYPES[i];
            if (name.endsWith(row[0])) {
                return row[1];
            }
        }

        return "other";
    }

}
