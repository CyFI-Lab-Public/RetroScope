/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.emailcommon.utility;

import com.android.emailcommon.internet.MimeHeader;
import com.android.emailcommon.internet.MimeUtility;
import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.mail.Part;

import android.text.TextUtils;

import java.util.ArrayList;

public class ConversionUtilities {
    /**
     * Values for HEADER_ANDROID_BODY_QUOTED_PART to tag body parts
     */
    public static final String BODY_QUOTED_PART_REPLY = "quoted-reply";
    public static final String BODY_QUOTED_PART_FORWARD = "quoted-forward";
    public static final String BODY_QUOTED_PART_INTRO = "quoted-intro";

    /**
     * Helper function to append text to a StringBuffer, creating it if necessary.
     * Optimization:  The majority of the time we are *not* appending - we should have a path
     * that deals with single strings.
     */
    private static StringBuffer appendTextPart(StringBuffer sb, String newText) {
        if (newText == null) {
            return sb;
        }
        else if (sb == null) {
            sb = new StringBuffer(newText);
        } else {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(newText);
        }
        return sb;
    }

    /**
     * Plain-Old-Data class to return parsed body data from
     * {@link ConversionUtilities#parseBodyFields}
     */
    public static class BodyFieldData {
        public String textContent;
        public String htmlContent;
        public String textReply;
        public String htmlReply;
        public String introText;
        public String snippet;
        public boolean isQuotedReply;
        public boolean isQuotedForward;
    }

    /**
     * Parse body text (plain and/or HTML) from MimeMessage to {@link BodyFieldData}.
     */
    public static BodyFieldData parseBodyFields(ArrayList<Part> viewables)
    throws MessagingException {
        final BodyFieldData data = new BodyFieldData();
        StringBuffer sbHtml = null;
        StringBuffer sbText = null;
        StringBuffer sbHtmlReply = null;
        StringBuffer sbTextReply = null;
        StringBuffer sbIntroText = null;

        for (Part viewable : viewables) {
            String text = MimeUtility.getTextFromPart(viewable);
            String[] replyTags = viewable.getHeader(MimeHeader.HEADER_ANDROID_BODY_QUOTED_PART);
            String replyTag = null;
            if (replyTags != null && replyTags.length > 0) {
                replyTag = replyTags[0];
            }
            // Deploy text as marked by the various tags
            boolean isHtml = "text/html".equalsIgnoreCase(viewable.getMimeType());

            if (replyTag != null) {
                data.isQuotedReply = BODY_QUOTED_PART_REPLY.equalsIgnoreCase(replyTag);
                data.isQuotedForward = BODY_QUOTED_PART_FORWARD.equalsIgnoreCase(replyTag);
                boolean isQuotedIntro = BODY_QUOTED_PART_INTRO.equalsIgnoreCase(replyTag);

                if (data.isQuotedReply || data.isQuotedForward) {
                    if (isHtml) {
                        sbHtmlReply = appendTextPart(sbHtmlReply, text);
                    } else {
                        sbTextReply = appendTextPart(sbTextReply, text);
                    }
                    continue;
                }
                if (isQuotedIntro) {
                    sbIntroText = appendTextPart(sbIntroText, text);
                    continue;
                }
            }

            // Most of the time, just process regular body parts
            if (isHtml) {
                sbHtml = appendTextPart(sbHtml, text);
            } else {
                sbText = appendTextPart(sbText, text);
            }
        }

        // write the combined data to the body part
        if (!TextUtils.isEmpty(sbText)) {
            String text = sbText.toString();
            data.textContent = text;
            data.snippet = TextUtilities.makeSnippetFromPlainText(text);
        }
        if (!TextUtils.isEmpty(sbHtml)) {
            String text = sbHtml.toString();
            data.htmlContent = text;
            if (data.snippet == null) {
                data.snippet = TextUtilities.makeSnippetFromHtmlText(text);
            }
        }
        if (sbHtmlReply != null && sbHtmlReply.length() != 0) {
            data.htmlReply = sbHtmlReply.toString();
        }
        if (sbTextReply != null && sbTextReply.length() != 0) {
            data.textReply = sbTextReply.toString();
        }
        if (sbIntroText != null && sbIntroText.length() != 0) {
            data.introText = sbIntroText.toString();
        }
        return data;
    }
}
