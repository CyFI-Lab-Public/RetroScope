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
package com.android.cts.verifier.p2p.testcase;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pInfo;

import com.android.cts.verifier.R;

/**
 * A test case which accepts a connection from p2p client.
 *
 * The requester device tries to join this device.
 */
public class GoTestCase extends TestCase {

    protected P2pBroadcastReceiverTest mReceiverTest;

    public GoTestCase(Context context) {
        super(context);
    }

    @Override
    protected void setUp() {
        super.setUp();
        mReceiverTest = new P2pBroadcastReceiverTest(mContext);
        mReceiverTest.init(mChannel);
    }

    @Override
    protected boolean executeTest() throws InterruptedException {

        ActionListenerTest listener = new ActionListenerTest();

        /*
         * Add renderer service
         */
        mP2pMgr.addLocalService(mChannel, LocalServices.createRendererService(),
                listener);
        if (!listener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_add_local_service_error);
            return false;
        }

        /*
         * Add IPP service
         */
        mP2pMgr.addLocalService(mChannel, LocalServices.createIppService(),
                listener);
        if (!listener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_add_local_service_error);
            return false;
        }

        /*
         * Add AFP service
         */
        mP2pMgr.addLocalService(mChannel, LocalServices.createAfpService(),
                listener);
        if (!listener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_add_local_service_error);
            return false;
        }

        /*
         * Start up an autonomous group owner.
         */
        mP2pMgr.createGroup(mChannel, listener);
        if (!listener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_ceate_group_error);
            return false;
        }

        /*
         * Check whether createGroup() is succeeded.
         */
        WifiP2pInfo info = mReceiverTest.waitConnectionNotice(TIMEOUT);
        if (info == null || !info.isGroupOwner) {
            mReason = mContext.getString(R.string.p2p_ceate_group_error);
            return false;
        }

        // wait until p2p client is joining.
        return true;
    }

    @Override
    protected void tearDown() {

        // wait until p2p client is joining.
        synchronized(this) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (mP2pMgr != null) {
            mP2pMgr.cancelConnect(mChannel, null);
            mP2pMgr.removeGroup(mChannel, null);
        }
        if (mReceiverTest != null) {
            mReceiverTest.close();
        }
        super.tearDown();
    }

    @Override
    public String getTestName() {
        return "Accept client connection test";
    }
}
