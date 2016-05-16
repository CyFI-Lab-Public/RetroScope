/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.filesystemperf;

import android.cts.util.CtsAndroidTestCase;
import com.android.cts.util.TimeoutReq;

public class RandomRWTest extends CtsAndroidTestCase {
    private static final String DIR_RANDOM_WR = "RANDOM_WR";
    private static final String DIR_RANDOM_RD = "RANDOM_RD";

    @Override
    protected void tearDown() throws Exception {
        FileUtil.removeFileOrDir(getContext(), DIR_RANDOM_WR);
        FileUtil.removeFileOrDir(getContext(), DIR_RANDOM_RD);
        super.tearDown();
    }

    @TimeoutReq(minutes = 60)
    public void testRandomRead() throws Exception {
        final int READ_BUFFER_SIZE = 4 * 1024;
        final long fileSize = FileUtil.getFileSizeExceedingMemory(getContext(), READ_BUFFER_SIZE);
        if (fileSize == 0) { // not enough space, give up
            return;
        }
        FileUtil.doRandomReadTest(getContext(), DIR_RANDOM_RD, getReportLog(), fileSize,
                READ_BUFFER_SIZE);
    }

    // It is taking too long in some device, and thus cannot run multiple times
    @TimeoutReq(minutes = 60)
    public void testRandomUpdate() throws Exception {
        final int WRITE_BUFFER_SIZE = 4 * 1024;
        final long fileSize = 256 * 1024 * 1024;
        FileUtil.doRandomWriteTest(getContext(), DIR_RANDOM_WR, getReportLog(), fileSize,
                WRITE_BUFFER_SIZE);
    }
}
