/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.os.cts;



import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.ResultReceiver;
import android.test.AndroidTestCase;

public class ResultReceiverTest extends AndroidTestCase {
    private Handler mHandler = new Handler();
    private static final long DURATION = 100l;

    public void testResultReceiver() throws InterruptedException {
        MockResultReceiver sender = new MockResultReceiver(mHandler);
        Bundle bundle = new Bundle();
        int resultCode = 1;
        sender.send(resultCode, bundle);
        Thread.sleep(DURATION);
        assertEquals(resultCode, sender.getResultCode());
        assertSame(bundle, sender.getResultData());

        ResultReceiver receiver = new ResultReceiver(mHandler);
        assertEquals(0, receiver.describeContents());

        Parcel p = Parcel.obtain();
        receiver.writeToParcel(p, 0);
        p.setDataPosition(0);
        ResultReceiver target = ResultReceiver.CREATOR.createFromParcel(p);
        assertNotNull(target);
    }

    private class MockResultReceiver extends ResultReceiver {

        private Bundle mResultData;
        private int mResultCode;

        public MockResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            mResultData = resultData;
            mResultCode = resultCode;
        }

        public Bundle getResultData() {
            return mResultData;
        }

        public int getResultCode() {
            return mResultCode;
        }
    }
}
