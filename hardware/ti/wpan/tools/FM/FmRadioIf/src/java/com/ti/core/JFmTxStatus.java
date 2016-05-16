/*
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ti.jfm.core;

public enum JFmTxStatus implements IJFmEnum<Integer> {

    SUCCESS(0), FAILED(1), PENDING(2), INVALID_PARM(3), IN_PROGRESS(4), NOT_APPLICABLE(5), NOT_SUPPORTED(
          6), INTERNAL_ERROR(7), TRANSPORT_INIT_ERR(8), HARDWARE_ERR(9), NO_VALUE_AVAILABLE(10), CONTEXT_DOESNT_EXIST(
          11), CONTEXT_NOT_DESTROYED(12), CONTEXT_NOT_ENABLED(13), CONTEXT_NOT_DISABLED(14), NOT_DE_INITIALIZED(
          15), NOT_INITIALIZED(16), TOO_MANY_PENDING_CMDS(17), DISABLING_IN_PROGRESS(18), NO_RESOURCES(
          19), FM_COMMAND_FAILED(20), SCRIPT_EXEC_FAILED(21), PROCESS_TIMEOUT_FAILURE(22),

    FAILED_BT_NOT_INITIALIZED(23), AUDIO_OPERATION_UNAVAILIBLE_RESOURCES(24),
    /* LAST (AUDIO_OPERATION_UNAVAILIBLE_RESOURCES), */

    FMC_FIRST_FM_RX_STATUS_CODE(100), FMC_FIRST_FM_TX_STATUS_CODE(200);

    private final int value;

    private JFmTxStatus(int value) {
       this.value = value;
    }

    public Integer getValue() {
       return value;
    }
}
