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
import os
import sys
import numpy
import Image
import math
import time
import os.path

def main():
    """Test that the android.tonemap.mode param is applied.

    Applies different tonemap curves to each R,G,B channel, and checks
    that the output images are modified as expected.
    """
    NAME = os.path.basename(__file__).split(".")[0]

    THRESHOLD_RATIO_MIN_DIFF = 0.1
    THRESHOLD_DIFF_MAX_DIFF = 0.05

    # TODO: Query the allowable tonemap curve sizes; here, it's hardcoded to
    # a length=64 list of tuples. The max allowed length should be inside the
    # camera properties object.
    L = 32
    LM1 = float(L-1)

    with its.device.ItsSession() as cam:

        # Test 1: that the tonemap curves have the expected effect. Take two
        # shots, with n in [0,1], where each has a linear tonemap, with the
        # n=1 shot having a steeper gradient. The gradient for each R,G,B
        # channel increases (i.e.) R[n=1] should be brighter than R[n=0],
        # and G[n=1] should be brighter than G[n=0] by a larger margin, etc.
        rgb_means = []

        for n in [0,1]:
            req = its.objects.manual_capture_request(100,50)
            req["android.tonemap.mode"] = 0
            req["android.tonemap.curveRed"] = (
                    sum([[i/LM1, (1+0.5*n)*i/LM1] for i in range(L)], []))
            req["android.tonemap.curveGreen"] = (
                    sum([[i/LM1, (1+1.0*n)*i/LM1] for i in range(L)], []))
            req["android.tonemap.curveBlue"] = (
                    sum([[i/LM1, (1+1.5*n)*i/LM1] for i in range(L)], []))
            fname, w, h, md = cam.do_capture(req)
            img = its.image.load_yuv420_to_rgb_image(fname, w, h)
            its.image.write_image(
                    img, "%s_n=%d.jpg" %(NAME, n))
            tile = its.image.get_image_patch(img, 0.45, 0.45, 0.1, 0.1)
            rgb_means.append(its.image.compute_image_means(tile))

        rgb_ratios = [rgb_means[1][i] / rgb_means[0][i] for i in xrange(3)]
        print "Test 1: RGB ratios:", rgb_ratios
        assert(rgb_ratios[0] + THRESHOLD_RATIO_MIN_DIFF < rgb_ratios[1])
        assert(rgb_ratios[1] + THRESHOLD_RATIO_MIN_DIFF < rgb_ratios[2])


        # Test 2: that the length of the tonemap curve (i.e. number of control
        # points) doesn't affect the output.
        rgb_means = []

        for size in [32,64]:
            m = float(size-1)
            curve = sum([[i/m, i/m] for i in range(size)], [])
            req = its.objects.manual_capture_request(100,50)
            req["android.tonemap.mode"] = 0
            req["android.tonemap.curveRed"] = curve
            req["android.tonemap.curveGreen"] = curve
            req["android.tonemap.curveBlue"] = curve
            fname, w, h, md = cam.do_capture(req)
            img = its.image.load_yuv420_to_rgb_image(fname, w, h)
            its.image.write_image(
                    img, "%s_size=%02d.jpg" %(NAME, size))
            tile = its.image.get_image_patch(img, 0.45, 0.45, 0.1, 0.1)
            rgb_means.append(its.image.compute_image_means(tile))

        rgb_diffs = [rgb_means[1][i] - rgb_means[0][i] for i in xrange(3)]
        print "Test 2: RGB diffs:", rgb_diffs
        assert(abs(rgb_diffs[0]) < THRESHOLD_DIFF_MAX_DIFF)
        assert(abs(rgb_diffs[1]) < THRESHOLD_DIFF_MAX_DIFF)
        assert(abs(rgb_diffs[2]) < THRESHOLD_DIFF_MAX_DIFF)

if __name__ == '__main__':
    main()

