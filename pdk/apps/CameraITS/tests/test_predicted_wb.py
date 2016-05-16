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
    """Test that valid data comes back in CaptureResult objects.
    """
    NAME = os.path.basename(__file__).split(".")[0]

    def r2f(r):
        return float(r["numerator"]) / float(r["denominator"])

    if not its.device.reboot_device_on_argv():
        its.device.reboot_device()

    # Run a first pass, which starts with a 3A convergence step.
    with its.device.ItsSession() as cam:
        # Get 3A lock first, so the auto values in the capture result are
        # populated properly.
        r = [0,0,1,1]
        sens,exp,awb_gains,awb_transform,_ = cam.do_3a(r,r,r,True,True,False)

        # Capture an auto shot using the converged 3A.
        req = its.objects.auto_capture_request()
        fname, w, h, cap_res = cam.do_capture(req)
        img = its.image.load_yuv420_to_rgb_image(fname, w, h)
        its.image.write_image(img, "%s_n=1_pass=1_auto.jpg" % (NAME))
        auto_gains = cap_res["android.colorCorrection.gains"]
        auto_transform = cap_res["android.colorCorrection.transform"]

        # Capture a request using default (unit/identify) gains, and get the
        # predicted gains and transform.
        req = its.objects.manual_capture_request(sens, exp/(1000.0*1000.0))
        fname, w, h, cap_res = cam.do_capture(req)
        img = its.image.load_yuv420_to_rgb_image(fname, w, h)
        its.image.write_image(img, "%s_n=2_pass=1_identity.jpg" % (NAME))
        pred_gains_1 = cap_res["android.statistics.predictedColorGains"]
        pred_transform_1 = cap_res["android.statistics.predictedColorTransform"]

        # Capture a request using the predicted gains/transform.
        req = its.objects.manual_capture_request(sens, exp/(1000.0*1000.0))
        req["android.colorCorrection.transform"] = pred_transform_1
        req["android.colorCorrection.gains"] = pred_gains_1
        fname, w, h, md_obj = cam.do_capture(req)
        img = its.image.load_yuv420_to_rgb_image(fname, w, h)
        its.image.write_image(img, "%s_n=3_pass=1_predicted.jpg" % (NAME))

        print "Pass 1 metering gains:", awb_gains
        print "Pass 1 metering transform:", awb_transform
        print "Pass 1 auto shot gains:", auto_gains
        print "Pass 1 auto shot transform:", [r2f(t) for t in auto_transform]
        print "Pass 1 predicted gains:", pred_gains_1
        print "Pass 1 predicted transform:", [r2f(t) for t in pred_transform_1]

    if not its.device.reboot_device_on_argv():
        its.device.reboot_device()

    # Run a second pass after rebooting that doesn't start with 3A convergence.
    with its.device.ItsSession() as cam:
        # Capture a request using default (unit/identify) gains, and get the
        # predicted gains and transform.
        req = its.objects.manual_capture_request(sens, exp/(1000.0*1000.0))
        fname, w, h, cap_res = cam.do_capture(req)
        img = its.image.load_yuv420_to_rgb_image(fname, w, h)
        its.image.write_image(img, "%s_n=4_pass=2_identity.jpg" % (NAME))
        pred_gains_2 = cap_res["android.statistics.predictedColorGains"]
        pred_transform_2 = cap_res["android.statistics.predictedColorTransform"]

        # Capture a request using the predicted gains/transform.
        req = its.objects.manual_capture_request(sens, exp/(1000.0*1000.0))
        req["android.colorCorrection.transform"] = pred_transform_2
        req["android.colorCorrection.gains"] = pred_gains_2
        fname, w, h, md_obj = cam.do_capture(req)
        img = its.image.load_yuv420_to_rgb_image(fname, w, h)
        its.image.write_image(img, "%s_n=5_pass=2_predicted.jpg" % (NAME))

        print "Pass 2 predicted gains:", pred_gains_2
        print "Pass 2 predicted transform:", [r2f(t) for t in pred_transform_2]

if __name__ == '__main__':
    main()

