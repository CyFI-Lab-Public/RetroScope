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
    """Test that the android.sensor.exposureTime parameter is applied.
    """
    NAME = os.path.basename(__file__).split(".")[0]

    # Pass/fail thresholds.
    THRESHOLD_MAX_MIN_DIFF = 0.3
    THRESHOLD_MAX_MIN_RATIO = 2.0

    req = {
        "android.control.mode": 0,
        "android.control.aeMode": 0,
        "android.control.awbMode": 0,
        "android.control.afMode": 0,
        "android.sensor.frameDuration": 0,
        "android.sensor.sensitivity": 200
        }

    exposures = range(1,101,20) # ms
    r_means = []
    g_means = []
    b_means = []

    with its.device.ItsSession() as cam:
        for e in exposures:
            req["android.sensor.exposureTime"] = e*1000*1000
            fname, w, h, cap_md = cam.do_capture(req)
            img = its.image.load_yuv420_to_rgb_image(fname, w, h)
            its.image.write_image(
                    img, "%s_time=%03dms.jpg" % (NAME, e))
            tile = its.image.get_image_patch(img, 0.45, 0.45, 0.1, 0.1)
            rgb_means = its.image.compute_image_means(tile)
            r_means.append(rgb_means[0])
            g_means.append(rgb_means[1])
            b_means.append(rgb_means[2])

    # Draw a plot.
    pylab.plot(exposures, r_means, 'r')
    pylab.plot(exposures, g_means, 'g')
    pylab.plot(exposures, b_means, 'b')
    pylab.ylim([0,1])
    matplotlib.pyplot.savefig("%s_plot_means.png" % (NAME))

    # Test for pass/fail: just check that that images get brighter by an amount
    # that is more than could be expected by random noise. Don't assume the
    # curve has any specific shape or gradient. This test is just checking that
    # the sensitivity parameter actually does something. Note the intensity
    # may be clamped to 0 or 1 for part of the ramp, so only test that the
    # brightness difference between the first and last samples are above a
    # given threshold.
    for means in [r_means, g_means, b_means]:
        for i in range(len(means)-1):
            assert(means[i] <= means[i+1])
        assert(means[-1] - means[0] > THRESHOLD_MAX_MIN_DIFF)
        assert(means[-1] / means[0] > THRESHOLD_MAX_MIN_RATIO)

if __name__ == '__main__':
    main()

