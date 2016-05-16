/**
 * Copyright (c) 2013, Google Inc.
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

package com.android.mail.providers.protos.mock;

import android.os.Bundle;
import android.os.Parcelable;

import com.android.mail.utils.LogUtils;
import com.android.mail.utils.MatrixCursorWithCachedColumns;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MockRespondMatrixCursor extends MatrixCursorWithCachedColumns{
    private static final String LOG_TAG = "MockProvider";

    static final String MOCK_RESPOND_PREFIX = "respond_";

    final List<Map<String, Object>> mResultList;

    public MockRespondMatrixCursor(final String[] columnNames, final int initialCapacity,
            List<Map<String, Object>> queryResults) {
        super(columnNames, initialCapacity);
        mResultList = queryResults;
    }

    @Override
    public Bundle respond(Bundle request) {
        final Bundle response = new Bundle();

        final int pos = getPosition();
        if (pos >= mResultList.size()) {
            LogUtils.wtf(LOG_TAG, "Unexpected position");
            return response;
        }

        final Map<String, Object> rowData = mResultList.get(pos);

        // For each of the keys in the request, we want to see if there is a mock response for the
        // request
        final Set<String> bundleKeys = request.keySet();
        for (String key : bundleKeys) {
            final Object responseData = rowData.get(MOCK_RESPOND_PREFIX + key);
            if (responseData != null) {
                response.putParcelable(key, (Parcelable)responseData);
            }
        }

        return response;
    }
}
