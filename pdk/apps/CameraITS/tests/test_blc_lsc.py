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
    """Test that BLC and LSC look reasonable.
    """
    NAME = os.path.basename(__file__).split(".")[0]

    r_means_center = []
    g_means_center = []
    b_means_center = []
    r_means_corner = []
    g_means_corner = []
    b_means_corner = []

    with its.device.ItsSession() as cam:
        # Get AE+AWB lock first, so the auto values in the capture result are
        # populated properly.
        r = [0,0,1,1]
        ae_sen,ae_exp,awb_gains,awb_transform,_ = \
                cam.do_3a(r,r,r,True,True,False)
        ae_exp = ae_exp / 1000000.0
        print "AE:", ae_sen, ae_exp
        print "AWB:", awb_gains, awb_transform

        # Set analog gain (sensitivity) to 800
        ae_exp = ae_exp * ae_sen / 800
        ae_sen = 800

        # Capture range of exposures from 1/100x to 4x of AE estimate.
        exposures = [ae_exp*x/100.0 for x in [1]+range(10,401,20)]
        print "Exposures:", exposures

        # Convert the transform back to rational.
        awb_transform_rat = [{"numerator":int(100*x),"denominator":100}
                             for x in awb_transform]

        # Linear tonemap
        tmap = sum([[i/63.0,i/63.0] for i in range(64)], [])

        reqs = []
        for e in exposures:
            req = its.objects.manual_capture_request(ae_sen,e)
            req["android.tonemap.mode"] = 0
            req["android.tonemap.curveRed"] = tmap
            req["android.tonemap.curveGreen"] = tmap
            req["android.tonemap.curveBlue"] = tmap
            req["android.colorCorrection.transform"] = awb_transform_rat
            req["android.colorCorrection.gains"] = awb_gains
            reqs.append(req)

        fnames, w, h, cap_mds = cam.do_capture(reqs)
        for i,fname in enumerate(fnames):
            img = its.image.load_yuv420_to_rgb_image(fname, w, h)

            tile_center = its.image.get_image_patch(img, 0.45, 0.45, 0.1, 0.1)
            rgb_means = its.image.compute_image_means(tile_center)
            r_means_center.append(rgb_means[0])
            g_means_center.append(rgb_means[1])
            b_means_center.append(rgb_means[2])

            tile_corner = its.image.get_image_patch(img, 0.0, 0.0, 0.1, 0.1)
            rgb_means = its.image.compute_image_means(tile_corner)
            r_means_corner.append(rgb_means[0])
            g_means_corner.append(rgb_means[1])
            b_means_corner.append(rgb_means[2])

    fig = matplotlib.pyplot.figure()
    pylab.plot(exposures, r_means_center, 'r')
    pylab.plot(exposures, g_means_center, 'g')
    pylab.plot(exposures, b_means_center, 'b')
    pylab.ylim([0,1])
    matplotlib.pyplot.savefig("%s_plot_means_center.png" % (NAME))

    fig = matplotlib.pyplot.figure()
    pylab.plot(exposures, r_means_corner, 'r')
    pylab.plot(exposures, g_means_corner, 'g')
    pylab.plot(exposures, b_means_corner, 'b')
    pylab.ylim([0,1])
    matplotlib.pyplot.savefig("%s_plot_means_corner.png" % (NAME))

if __name__ == '__main__':
    main()

