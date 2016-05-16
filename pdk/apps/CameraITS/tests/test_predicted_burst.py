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
import copy

def main():
    """Test that predicted AWB values come out on the right frames.
    """
    NAME = os.path.basename(__file__).split(".")[0]

    def r2f(r):
        return float(r["numerator"]) / float(r["denominator"])

    reqs = \
        [its.objects.manual_capture_request(100,0.1)]*7 + \
        [its.objects.manual_capture_request(300,33)]*7 + \
        [its.objects.manual_capture_request(100,0.1)]*7

    curve = sum([[i/63.0, pow(i/63.0, 1/2.2)] for i in range(64)], [])
    for r in reqs:
        r["android.tonemap.mode"] = 0
        r["android.tonemap.curveRed"] = curve
        r["android.tonemap.curveGreen"] = curve
        r["android.tonemap.curveBlue"] = curve

    with its.device.ItsSession() as cam:
        fnames, w, h, mds = cam.do_capture(reqs)
        for i, fname in enumerate(fnames):
            img = its.image.load_yuv420_to_rgb_image(fname, w, h)
            its.image.write_image(img, "%s_i=%02d.jpg" % (NAME, i))
            md = mds[i]
            print "Predicted:", \
                  md["android.statistics.predictedColorGains"], \
                  [r2f(t)
                   for t in md["android.statistics.predictedColorTransform"]]

if __name__ == '__main__':
    main()

