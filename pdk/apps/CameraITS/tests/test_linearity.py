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
import sys
import numpy
import Image
import pprint
import math
import pylab
import os.path
import matplotlib
import matplotlib.pyplot

def main():
    """Test that device processing can be inverted to linear pixels.

    Captures a sequence of shots with the device pointed at a uniform
    target. Attempts to invert all the ISP processing to get back to
    linear R,G,B pixel data.
    """
    NAME = os.path.basename(__file__).split(".")[0]

    # TODO: Query the allowable tonemap curve sizes; here, it's hardcoded to
    # a length=64 list of tuples. The max allowed length should be inside the
    # camera properties object.
    L = 64
    LM1 = float(L-1)

    gamma_lut = numpy.array(
            sum([[i/LM1, math.pow(i/LM1, 1/2.2)] for i in xrange(L)], []))
    inv_gamma_lut = numpy.array(
            sum([[i/LM1, math.pow(i/LM1, 2.2)] for i in xrange(L)], []))

    req = {
        "android.sensor.exposureTime": 10*1000*1000,
        "android.sensor.frameDuration": 0,
        "android.control.mode": 0,
        "android.control.aeMode": 0,
        "android.control.awbMode": 0,
        "android.control.afMode": 0,
        "android.blackLevel.lock": True,

        # Each channel is a simple gamma curve.
        "android.tonemap.mode": 0,
        "android.tonemap.curveRed": gamma_lut.tolist(),
        "android.tonemap.curveGreen": gamma_lut.tolist(),
        "android.tonemap.curveBlue": gamma_lut.tolist(),
        }

    sensitivities = range(100,500,50)+range(500,1000,100)+range(1000,3000,300)

    with its.device.ItsSession() as cam:

        # For i=0, don't set manual color correction gains and transform. Graph
        # with solid R,G,B curves.
        #
        # For i=1, set identity transform and unit gains. Graph with dashed
        # curves.

        for i in xrange(2):

            r_means = []
            g_means = []
            b_means = []

            if i == 1:
                req["android.colorCorrection.mode"] = 0
                req["android.colorCorrection.transform"] = (
                        its.objects.int_to_rational([1,0,0, 0,1,0, 0,0,1]))
                req["android.colorCorrection.gains"] = [1,1,1,1]

            for sens in sensitivities:
                req["android.sensor.sensitivity"] = sens
                fname, w, h, cap_md = cam.do_capture(req)
                img = its.image.load_yuv420_to_rgb_image(fname, w, h)
                its.image.write_image(
                        img, "%s_sens=%04d.jpg" % (NAME, sens))
                img = its.image.apply_lut_to_image(img, inv_gamma_lut[1::2] * LM1)
                tile = its.image.get_image_patch(img, 0.45, 0.45, 0.1, 0.1)
                rgb_means = its.image.compute_image_means(tile)
                r_means.append(rgb_means[0])
                g_means.append(rgb_means[1])
                b_means.append(rgb_means[2])

            pylab.plot(sensitivities, r_means, ['r','r--'][i])
            pylab.plot(sensitivities, g_means, ['g','g--'][i])
            pylab.plot(sensitivities, b_means, ['b','b--'][i])

    pylab.ylim([0,1])
    matplotlib.pyplot.savefig("%s_plot_means.png" % (NAME))

if __name__ == '__main__':
    main()

