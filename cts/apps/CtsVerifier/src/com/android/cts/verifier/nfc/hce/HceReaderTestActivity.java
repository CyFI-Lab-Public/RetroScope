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

package com.android.cts.verifier.nfc.hce;

import com.android.cts.verifier.ArrayTestListAdapter;
import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.TestListAdapter.TestListItem;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

/** Activity that lists all the NFC HCE reader tests. */
public class HceReaderTestActivity extends PassFailButtons.TestListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_list);
        setInfoResources(R.string.nfc_test, R.string.nfc_hce_reader_test_info, 0);
        setPassFailButtonClickListeners();

        ArrayTestListAdapter adapter = new ArrayTestListAdapter(this);

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
            adapter.add(TestListItem.newCategory(this, R.string.nfc_hce_reader_tests));
            /*
             * Only add this test when supported in platform
            adapter.add(TestListItem.newTest(this, R.string.nfc_hce_default_route_reader,
                    SimpleReaderActivity.class.getName(),
                    DefaultRouteEmulatorActivity.buildReaderIntent(this), null));
             */

            adapter.add(TestListItem.newTest(this, R.string.nfc_hce_protocol_params_reader,
                    ProtocolParamsReaderActivity.class.getName(),
                    new Intent(this, ProtocolParamsReaderActivity.class), null));

            adapter.add(TestListItem.newTest(this, R.string.nfc_hce_single_payment_reader,
                    SimpleReaderActivity.class.getName(),
                    SinglePaymentEmulatorActivity.buildReaderIntent(this), null));

            adapter.add(TestListItem.newTest(this, R.string.nfc_hce_dual_payment_reader,
                    SimpleReaderActivity.class.getName(),
                    DualPaymentEmulatorActivity.buildReaderIntent(this), null));

            adapter.add(TestListItem.newTest(this, R.string.nfc_hce_change_default_reader,
                    SimpleReaderActivity.class.getName(),
                    ChangeDefaultEmulatorActivity.buildReaderIntent(this), null));

            adapter.add(TestListItem.newTest(this, R.string.nfc_hce_single_non_payment_reader,
                    SimpleReaderActivity.class.getName(),
                    SingleNonPaymentEmulatorActivity.buildReaderIntent(this), null));

            adapter.add(TestListItem.newTest(this, R.string.nfc_hce_dual_non_payment_reader,
                    SimpleReaderActivity.class.getName(),
                    DualNonPaymentEmulatorActivity.buildReaderIntent(this), null));

            adapter.add(TestListItem.newTest(this, R.string.nfc_hce_conflicting_non_payment_reader,
                    SimpleReaderActivity.class.getName(),
                    ConflictingNonPaymentEmulatorActivity.buildReaderIntent(this), null));

            adapter.add(TestListItem.newTest(this, R.string.nfc_hce_throughput_reader,
                    SimpleReaderActivity.class.getName(),
                    ThroughputEmulatorActivity.buildReaderIntent(this), null));

            adapter.add(TestListItem.newTest(this, R.string.nfc_hce_tap_test_reader,
                    SimpleReaderActivity.class.getName(),
                    TapTestEmulatorActivity.buildReaderIntent(this), null));

            adapter.add(TestListItem.newTest(this, R.string.nfc_hce_offhost_service_reader,
                    SimpleReaderActivity.class.getName(),
                    OffHostEmulatorActivity.buildReaderIntent(this), null));

            adapter.add(TestListItem.newTest(this, R.string.nfc_hce_on_and_offhost_service_reader,
                    SimpleReaderActivity.class.getName(),
                    OnAndOffHostEmulatorActivity.buildReaderIntent(this), null));
        }

        setTestListAdapter(adapter);
    }
}
