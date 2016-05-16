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
import os.path
import pprint
import math
import numpy
import matplotlib.pyplot
import mpl_toolkits.mplot3d

def main():
    """Run 3A remotely (from this script).
    """
    NAME = os.path.basename(__file__).split(".")[0]

    def r2f(r):
        return float(r["numerator"]) / float(r["denominator"])

    with its.device.ItsSession() as cam:
        props = cam.get_camera_properties()
        w_map = props["android.lens.info.shadingMapSize"]["width"]
        h_map = props["android.lens.info.shadingMapSize"]["height"]

        # TODO: Test for 3A convergence, and exit this test once converged.

        triggered = False
        while True:
            req = its.objects.auto_capture_request()
            req["android.statistics.lensShadingMapMode"] = 1
            req['android.control.aePrecaptureTrigger'] = (0 if triggered else 1)
            req['android.control.afTrigger'] = (0 if triggered else 1)
            triggered = True

            fname, w, h, cap_res = cam.do_capture(req)

            ae_state = cap_res["android.control.aeState"]
            awb_state = cap_res["android.control.awbState"]
            af_state = cap_res["android.control.afState"]
            gains = cap_res["android.colorCorrection.gains"]
            transform = cap_res["android.colorCorrection.transform"]
            exp_time = cap_res['android.sensor.exposureTime']
            lsc_map = cap_res["android.statistics.lensShadingMap"]
            foc_dist = cap_res['android.lens.focusDistance']
            foc_range = cap_res['android.lens.focusRange']

            print "States (AE,AWB,AF):", ae_state, awb_state, af_state
            print "Gains:", gains
            print "Transform:", [r2f(t) for t in transform]
            print "AE region:", cap_res['android.control.aeRegions']
            print "AF region:", cap_res['android.control.afRegions']
            print "AWB region:", cap_res['android.control.awbRegions']
            print "LSC map:", w_map, h_map, lsc_map[:8]
            print "Focus (dist,range):", foc_dist, foc_range
            print ""

if __name__ == '__main__':
    main()

