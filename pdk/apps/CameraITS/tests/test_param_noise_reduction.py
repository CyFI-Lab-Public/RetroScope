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
    """Test that the android.noiseReduction.mode param is applied when set.

    Capture images with the camera dimly lit. Uses a long exposure
    and a high analog gain to ensure the captured image is noisy.

    Captures three images, for NR off, "fast", and "high quality".
    Also captures an image with low gain and NR off, and uses the variance
    of this as the baseline.
    """
    NAME = os.path.basename(__file__).split(".")[0]

    THRESHOLD_MIN_VARIANCE_RATIO = 0.7

    req = {
        "android.control.mode": 0,
        "android.control.aeMode": 0,
        "android.control.awbMode": 0,
        "android.control.afMode": 0,
        "android.sensor.frameDuration": 0
        }

    # List of variances for Y,U,V.
    variances = [[],[],[]]

    # Reference (baseline) variance for each of Y,U,V.
    ref_variance = []

    nr_modes_reported = []

    with its.device.ItsSession() as cam:
        # NR mode 0 with low gain
        req["android.noiseReduction.mode"] = 0
        req["android.sensor.sensitivity"] = 100
        req["android.sensor.exposureTime"] = 20*1000*1000
        fname, w, h, md_obj = cam.do_capture(req)
        its.image.write_image(
                its.image.load_yuv420_to_rgb_image(fname, w, h),
                "%s_low_gain.jpg" % (NAME))
        planes = its.image.load_yuv420_to_yuv_planes(fname, w, h)
        for j in range(3):
            img = planes[j]
            tile = its.image.get_image_patch(img, 0.45, 0.45, 0.1, 0.1)
            ref_variance.append(its.image.compute_image_variances(tile)[0])

        for i in range(3):
            # NR modes 0, 1, 2 with high gain
            req["android.noiseReduction.mode"] = i
            req["android.sensor.sensitivity"] = 100*16
            req["android.sensor.exposureTime"] = (20*1000*1000/16)
            fname, w, h, md_obj = cam.do_capture(req)
            nr_modes_reported.append(md_obj["android.noiseReduction.mode"])
            its.image.write_image(
                    its.image.load_yuv420_to_rgb_image(fname, w, h),
                    "%s_high_gain_nr=%d.jpg" % (NAME, i))
            planes = its.image.load_yuv420_to_yuv_planes(fname, w, h)
            for j in range(3):
                img = planes[j]
                tile = its.image.get_image_patch(img, 0.45, 0.45, 0.1, 0.1)
                variance = its.image.compute_image_variances(tile)[0]
                variances[j].append(variance / ref_variance[j])

    # Draw a plot.
    for j in range(3):
        pylab.plot(range(3), variances[j], "rgb"[j])
    matplotlib.pyplot.savefig("%s_plot_variances.png" % (NAME))

    assert(nr_modes_reported == [0,1,2])

    # Check that the variance of the NR=0 image is much higher than for the
    # NR=1 and NR=2 images.
    for j in range(3):
        for i in range(1,3):
            assert(variances[j][i] / variances[j][0] <
                   THRESHOLD_MIN_VARIANCE_RATIO)

if __name__ == '__main__':
    main()

