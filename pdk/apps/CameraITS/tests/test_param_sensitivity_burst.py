# Copyright 2013 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import its.image
import its.device
import its.objects
import pylab
import os.path
import matplotlib
import matplotlib.pyplot

def main():
    """Test that the android.sensor.sensitivity parameter is applied properly
    within a burst. Inspects the output metadata.
    """
    NAME = os.path.basename(__file__).split(".")[0]

    sensitivities = range(350, 400, 7)
    reqs = [its.objects.manual_capture_request(s,10) for s in sensitivities]

    with its.device.ItsSession() as cam:
        fnames, w, h, cap_mds = cam.do_capture(reqs)
        for i,md in enumerate(cap_mds):
            s_req = sensitivities[i]
            s_res = md["android.sensor.sensitivity"]
            print s_req, s_res

if __name__ == '__main__':
    main()

