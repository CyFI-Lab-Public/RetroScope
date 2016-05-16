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

package com.android.mail.providers;

import com.google.common.collect.ImmutableSet;

import java.lang.IllegalArgumentException;
import java.lang.String;
import java.util.Arrays;
import java.util.Set;


/**
 * A helper class to validate projections for the UIProvider queries.
 *
 * TODO(pwestbro): Consider creating an abstract ContentProvider that contains this
 * functionionality.
 */
public class UIProviderValidator {
    /**
     * Validates and returns the projection that can be used for an account query.
     */
    public static String[] validateAccountProjection(String[] projection) {
        return getValidProjection(projection, UIProvider.ACCOUNTS_PROJECTION);
    }

    /**
     * Validates and returns the projection that can be used for a folder query.
     */
    public static String[] validateFolderProjection(String[] projection) {
        return getValidProjection(projection, UIProvider.FOLDERS_PROJECTION);
    }

    /**
     * Validates and returns the projection that can be used for a account cookie query.
     */
    public static String[] validateAccountCookieProjection(String[] projection) {
        return getValidProjection(projection, UIProvider.ACCOUNT_COOKIE_PROJECTION);
    }

    /**
     * Validates and returns the projection that can be used for a conversation query.
     */
    public static String[] validateConversationProjection(String[] projection) {
        return getValidProjection(projection, UIProvider.CONVERSATION_PROJECTION);
    }

    /**
     * Validates and returns the projection that can be used for a message query.
     */
    public static String[] validateMessageProjection(String[] projection) {
        return getValidProjection(projection, UIProvider.MESSAGE_PROJECTION);
    }

    /**
     * Validates and returns the projection that can be used for a attachment query.
     */
    public static String[] validateAttachmentProjection(String[] projection) {
        return getValidProjection(projection, UIProvider.ATTACHMENT_PROJECTION);
    }


    private static String[] getValidProjection(String[] requestedProjection,
            String[] allColumnProjection) {
        final String[] resultProjection;
        if (requestedProjection != null) {
            if (isValidProjection(requestedProjection, ImmutableSet.copyOf(allColumnProjection))) {
                // The requested projection is valid, use it.
                resultProjection = requestedProjection;
            } else {
                throw new IllegalArgumentException(
                        "Invalid projection: " + Arrays.toString(requestedProjection));
            }
        } else {
            // If the caller specified a null projection, they want all columns
            resultProjection = allColumnProjection;
        }
        return resultProjection;
    }

    private static boolean isValidProjection(String[] projection, Set<String> validColumns) {
        for (String column : projection) {
            if (!validColumns.contains(column)) {
                return false;
            }
        }
        return true;
    }
}