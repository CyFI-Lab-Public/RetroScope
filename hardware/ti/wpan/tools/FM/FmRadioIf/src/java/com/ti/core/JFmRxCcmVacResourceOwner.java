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

import com.ti.jfm.core.*;

public class JFmRxCcmVacResourceOwner {

    public static enum JFmRxEcalResource implements IJFmEnum<Integer> {

       CAL_RESOURCE_I2SH(0x00), CAL_RESOURCE_PCMH(0x01), CAL_RESOURCE_PCMT_1(0x02), CAL_RESOURCE_PCMT_2(
             0x03), CAL_RESOURCE_PCMT_3(0x04), CAL_RESOURCE_PCMT_4(0x05), CAL_RESOURCE_PCMT_5(
             0x06), CAL_RESOURCE_PCMT_6(0x07), CAL_RESOURCE_FM_ANALOG(0x08), CAL_RESOURCE_LAST_EL_RESOURCE(
             0x08), CAL_RESOURCE_PCMIF(0x09), CAL_RESOURCE_FMIF(0x0A), CAL_RESOURCE_CORTEX(0x0B), CAL_RESOURCE_FM_CORE(
             0x0C), CAL_RESOURCE_MAX_NUM(0x0D), CAL_RESOURCE_INVALID(0x0E);

       private final int ecalResource;

       private JFmRxEcalResource(int ecalResource) {
          this.ecalResource = ecalResource;
       }

       public Integer getValue() {
          return ecalResource;
       }
    }

    public static enum JFmRxEcalOperation implements IJFmEnum<Integer> {

       CAL_OPERATION_FM_TX(0x00), CAL_OPERATION_FM_RX(0x01), CAL_OPERATION_A3DP(0x02), CAL_OPERATION_BT_VOICE(
             0x03), CAL_OPERATION_WBS(0x04), CAL_OPERATION_AWBS(0x05), CAL_OPERATION_FM_RX_OVER_SCO(
             0x06), CAL_OPERATION_FM_RX_OVER_A3DP(0x07), CAL_OPERATION_MAX_NUM(0x08), CAL_OPERATION_INVALID(
             0x09);

       private final int ecalOperation;

       private JFmRxEcalOperation(int ecalOperation) {
          this.ecalOperation = ecalOperation;
       }

       public Integer getValue() {
          return ecalOperation;
       }
    }

    private JFmRxEcalResource resource;

    private JFmRxEcalOperation operation;

    public JFmRxCcmVacResourceOwner(JFmRxEcalResource resource, JFmRxEcalOperation operation) {
       this.resource = resource;
       this.operation = operation;
    }

    public JFmRxEcalResource getResource() {
       return resource;
    }

    public void setResource(JFmRxEcalResource resource) {
       this.resource = resource;
    }

    public JFmRxEcalOperation getOperation() {
       return operation;
    }

    public void setOperation(JFmRxEcalOperation operation) {
       this.operation = operation;
    }

}
