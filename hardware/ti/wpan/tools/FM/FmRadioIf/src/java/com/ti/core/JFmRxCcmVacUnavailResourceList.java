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

import com.ti.jfm.core.JFmRxCcmVacResourceOwner;

public class JFmRxCcmVacUnavailResourceList {

    final int MAX_NUM_OF_PROPERTIES_PER_RESOURCE = 5;

    private JFmRxCcmVacResourceOwner[] ccmVacResourceOwner = new JFmRxCcmVacResourceOwner[MAX_NUM_OF_PROPERTIES_PER_RESOURCE];

    private long uNumOfUnavailResources;

    public JFmRxCcmVacUnavailResourceList(JFmRxCcmVacResourceOwner[] ccmVacResourceOwner,
          long uNumOfUnavailResources) {
       this.ccmVacResourceOwner = ccmVacResourceOwner;
       this.uNumOfUnavailResources = uNumOfUnavailResources;
    }

    public JFmRxCcmVacResourceOwner[] getResourceOwner() {
       return ccmVacResourceOwner;
    }

    public void setResourceOwner(JFmRxCcmVacResourceOwner[] resource) {
       this.ccmVacResourceOwner = resource;
    }

    public long getNumOfUnavailResources() {
       return uNumOfUnavailResources;
    }

    public void setNumOfUnavailResources(Long numUnavailableResource) {
       this.uNumOfUnavailResources = numUnavailableResource;
    }

}
