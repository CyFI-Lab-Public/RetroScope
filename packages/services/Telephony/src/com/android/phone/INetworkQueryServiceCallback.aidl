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

package com.android.phone;

import com.android.internal.telephony.OperatorInfo;

/**
 * Service interface to handle callbacks into the activity from the
 * NetworkQueryService.  These objects are used to notify that a
 * query is complete and that the results are ready to process.
 */
oneway interface INetworkQueryServiceCallback {

    /**
     * Called upon query completion, handing a status value and an
     * array of OperatorInfo objects.
     *
     * @param networkInfoArray is the list of OperatorInfo. Can be
     * null, indicating no results were found, or an error.
     * @param status the status indicating if there were any
     * problems with the request.
     */
    void onQueryComplete(in List<OperatorInfo> networkInfoArray, int status);

}
