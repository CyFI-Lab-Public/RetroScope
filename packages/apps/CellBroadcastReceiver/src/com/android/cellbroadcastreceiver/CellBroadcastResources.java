/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.cellbroadcastreceiver;

import android.content.Context;
import android.graphics.Typeface;
import android.telephony.CellBroadcastMessage;
import android.telephony.SmsCbCmasInfo;
import android.telephony.SmsCbEtwsInfo;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

/**
 * Returns the string resource ID's for CMAS and ETWS emergency alerts.
 */
public class CellBroadcastResources {

    private CellBroadcastResources() {
    }

    /**
     * Returns a styled CharSequence containing the message date/time and alert details.
     * @param context a Context for resource string access
     * @return a CharSequence for display in the broadcast alert dialog
     */
    public static CharSequence getMessageDetails(Context context, CellBroadcastMessage cbm) {
        SpannableStringBuilder buf = new SpannableStringBuilder();

        // Alert date/time
        int start = buf.length();
        buf.append(context.getString(R.string.delivery_time_heading));
        int end = buf.length();
        buf.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        buf.append(" ");
        buf.append(cbm.getDateString(context));

        if (cbm.isCmasMessage()) {
            // CMAS category, response type, severity, urgency, certainty
            appendCmasAlertDetails(context, buf, cbm.getCmasWarningInfo());
        }

        return buf;
    }

    private static void appendCmasAlertDetails(Context context, SpannableStringBuilder buf,
            SmsCbCmasInfo cmasInfo) {
        // CMAS category
        int categoryId = getCmasCategoryResId(cmasInfo);
        if (categoryId != 0) {
            appendMessageDetail(context, buf, R.string.cmas_category_heading, categoryId);
        }

        // CMAS response type
        int responseId = getCmasResponseResId(cmasInfo);
        if (responseId != 0) {
            appendMessageDetail(context, buf, R.string.cmas_response_heading, responseId);
        }

        // CMAS severity
        int severityId = getCmasSeverityResId(cmasInfo);
        if (severityId != 0) {
            appendMessageDetail(context, buf, R.string.cmas_severity_heading, severityId);
        }

        // CMAS urgency
        int urgencyId = getCmasUrgencyResId(cmasInfo);
        if (urgencyId != 0) {
            appendMessageDetail(context, buf, R.string.cmas_urgency_heading, urgencyId);
        }

        // CMAS certainty
        int certaintyId = getCmasCertaintyResId(cmasInfo);
        if (certaintyId != 0) {
            appendMessageDetail(context, buf, R.string.cmas_certainty_heading, certaintyId);
        }
    }

    private static void appendMessageDetail(Context context, SpannableStringBuilder buf,
            int typeId, int valueId) {
        if (buf.length() != 0) {
            buf.append("\n");
        }
        int start = buf.length();
        buf.append(context.getString(typeId));
        int end = buf.length();
        buf.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        buf.append(" ");
        buf.append(context.getString(valueId));
    }

    /**
     * Returns the string resource ID for the CMAS category.
     * @return a string resource ID, or 0 if the CMAS category is unknown or not present
     */
    private static int getCmasCategoryResId(SmsCbCmasInfo cmasInfo) {
        switch (cmasInfo.getCategory()) {
            case SmsCbCmasInfo.CMAS_CATEGORY_GEO:
                return R.string.cmas_category_geo;

            case SmsCbCmasInfo.CMAS_CATEGORY_MET:
                return R.string.cmas_category_met;

            case SmsCbCmasInfo.CMAS_CATEGORY_SAFETY:
                return R.string.cmas_category_safety;

            case SmsCbCmasInfo.CMAS_CATEGORY_SECURITY:
                return R.string.cmas_category_security;

            case SmsCbCmasInfo.CMAS_CATEGORY_RESCUE:
                return R.string.cmas_category_rescue;

            case SmsCbCmasInfo.CMAS_CATEGORY_FIRE:
                return R.string.cmas_category_fire;

            case SmsCbCmasInfo.CMAS_CATEGORY_HEALTH:
                return R.string.cmas_category_health;

            case SmsCbCmasInfo.CMAS_CATEGORY_ENV:
                return R.string.cmas_category_env;

            case SmsCbCmasInfo.CMAS_CATEGORY_TRANSPORT:
                return R.string.cmas_category_transport;

            case SmsCbCmasInfo.CMAS_CATEGORY_INFRA:
                return R.string.cmas_category_infra;

            case SmsCbCmasInfo.CMAS_CATEGORY_CBRNE:
                return R.string.cmas_category_cbrne;

            case SmsCbCmasInfo.CMAS_CATEGORY_OTHER:
                return R.string.cmas_category_other;

            default:
                return 0;
        }
    }

    /**
     * Returns the string resource ID for the CMAS response type.
     * @return a string resource ID, or 0 if the CMAS response type is unknown or not present
     */
    private static int getCmasResponseResId(SmsCbCmasInfo cmasInfo) {
        switch (cmasInfo.getResponseType()) {
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_SHELTER:
                return R.string.cmas_response_shelter;

            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_EVACUATE:
                return R.string.cmas_response_evacuate;

            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_PREPARE:
                return R.string.cmas_response_prepare;

            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_EXECUTE:
                return R.string.cmas_response_execute;

            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_MONITOR:
                return R.string.cmas_response_monitor;

            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_AVOID:
                return R.string.cmas_response_avoid;

            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_ASSESS:
                return R.string.cmas_response_assess;

            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_NONE:
                return R.string.cmas_response_none;

            default:
                return 0;
        }
    }

    /**
     * Returns the string resource ID for the CMAS severity.
     * @return a string resource ID, or 0 if the CMAS severity is unknown or not present
     */
    private static int getCmasSeverityResId(SmsCbCmasInfo cmasInfo) {
        switch (cmasInfo.getSeverity()) {
            case SmsCbCmasInfo.CMAS_SEVERITY_EXTREME:
                return R.string.cmas_severity_extreme;

            case SmsCbCmasInfo.CMAS_SEVERITY_SEVERE:
                return R.string.cmas_severity_severe;

            default:
                return 0;
        }
    }

    /**
     * Returns the string resource ID for the CMAS urgency.
     * @return a string resource ID, or 0 if the CMAS urgency is unknown or not present
     */
    private static int getCmasUrgencyResId(SmsCbCmasInfo cmasInfo) {
        switch (cmasInfo.getUrgency()) {
            case SmsCbCmasInfo.CMAS_URGENCY_IMMEDIATE:
                return R.string.cmas_urgency_immediate;

            case SmsCbCmasInfo.CMAS_URGENCY_EXPECTED:
                return R.string.cmas_urgency_expected;

            default:
                return 0;
        }
    }

    /**
     * Returns the string resource ID for the CMAS certainty.
     * @return a string resource ID, or 0 if the CMAS certainty is unknown or not present
     */
    private static int getCmasCertaintyResId(SmsCbCmasInfo cmasInfo) {
        switch (cmasInfo.getCertainty()) {
            case SmsCbCmasInfo.CMAS_CERTAINTY_OBSERVED:
                return R.string.cmas_certainty_observed;

            case SmsCbCmasInfo.CMAS_CERTAINTY_LIKELY:
                return R.string.cmas_certainty_likely;

            default:
                return 0;
        }
    }

    public static int getDialogTitleResource(CellBroadcastMessage cbm) {
        // ETWS warning types
        SmsCbEtwsInfo etwsInfo = cbm.getEtwsWarningInfo();
        if (etwsInfo != null) {
            switch (etwsInfo.getWarningType()) {
                case SmsCbEtwsInfo.ETWS_WARNING_TYPE_EARTHQUAKE:
                    return R.string.etws_earthquake_warning;

                case SmsCbEtwsInfo.ETWS_WARNING_TYPE_TSUNAMI:
                    return R.string.etws_tsunami_warning;

                case SmsCbEtwsInfo.ETWS_WARNING_TYPE_EARTHQUAKE_AND_TSUNAMI:
                    return R.string.etws_earthquake_and_tsunami_warning;

                case SmsCbEtwsInfo.ETWS_WARNING_TYPE_TEST_MESSAGE:
                    return R.string.etws_test_message;

                case SmsCbEtwsInfo.ETWS_WARNING_TYPE_OTHER_EMERGENCY:
                default:
                    return R.string.etws_other_emergency_type;
            }
        }

        // CMAS warning types
        SmsCbCmasInfo cmasInfo = cbm.getCmasWarningInfo();
        if (cmasInfo != null) {
            switch (cmasInfo.getMessageClass()) {
                case SmsCbCmasInfo.CMAS_CLASS_PRESIDENTIAL_LEVEL_ALERT:
                    return R.string.cmas_presidential_level_alert;

                case SmsCbCmasInfo.CMAS_CLASS_EXTREME_THREAT:
                    return R.string.cmas_extreme_alert;

                case SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT:
                    return R.string.cmas_severe_alert;

                case SmsCbCmasInfo.CMAS_CLASS_CHILD_ABDUCTION_EMERGENCY:
                    return R.string.cmas_amber_alert;

                case SmsCbCmasInfo.CMAS_CLASS_REQUIRED_MONTHLY_TEST:
                    return R.string.cmas_required_monthly_test;

                case SmsCbCmasInfo.CMAS_CLASS_CMAS_EXERCISE:
                    return R.string.cmas_exercise_alert;

                case SmsCbCmasInfo.CMAS_CLASS_OPERATOR_DEFINED_USE:
                    return R.string.cmas_operator_defined_alert;

                default:
                    return R.string.pws_other_message_identifiers;
            }
        }

        if (CellBroadcastConfigService.isEmergencyAlertMessage(cbm)) {
            return R.string.pws_other_message_identifiers;
        } else {
            return R.string.cb_other_message_identifiers;
        }
    }
}
