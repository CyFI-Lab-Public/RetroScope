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

    manual_tonemap = [0,0, 1,1] # Linear
    manual_transform = its.objects.int_to_rational([1,2,3, 4,5,6, 7,8,9])
    manual_gains = [1,2,3,4]
    manual_region = [8,8,128,128]
    manual_exp_time = 100*1000*1000
    manual_sensitivity = 100

    auto_req = its.objects.auto_capture_request()
    auto_req["android.statistics.lensShadingMapMode"] = 1

    manual_req = {
        "android.control.mode": 0,
        "android.control.aeMode": 0,
        "android.control.awbMode": 0,
        "android.control.afMode": 0,
        "android.sensor.frameDuration": 0,
        "android.sensor.sensitivity": manual_sensitivity,
        "android.sensor.exposureTime": manual_exp_time,
        "android.colorCorrection.mode": 0,
        "android.colorCorrection.transform": manual_transform,
        "android.colorCorrection.gains": manual_gains,
        "android.tonemap.mode": 0,
        "android.tonemap.curveRed": manual_tonemap,
        "android.tonemap.curveGreen": manual_tonemap,
        "android.tonemap.curveBlue": manual_tonemap,
        "android.control.aeRegions": manual_region,
        "android.control.afRegions": manual_region,
        "android.control.awbRegions": manual_region,
        "android.statistics.lensShadingMapMode":1
        }

    def r2f(r):
        return float(r["numerator"]) / float(r["denominator"])

    # A very loose definition for two floats being close to each other;
    # there may be different interpolation and rounding used to get the
    # two values, and all this test is looking at is whether there is
    # something obviously broken; it's not looking for a perfect match.
    def is_close_float(n1, n2):
        return abs(n1 - n2) < 0.05

    def is_close_rational(n1, n2):
        return is_close_float(r2f(n1), r2f(n2))

    def draw_lsc_plot(w_map, h_map, lsc_map, name):
        fig = matplotlib.pyplot.figure()
        ax = fig.gca(projection='3d')
        xs = numpy.array([range(h_map)] * w_map).reshape(w_map, h_map)
        ys = numpy.array([[i]*h_map for i in range(w_map)]).reshape(w_map, h_map)
        for ch in range(4):
            size = w_map*h_map
            start = ch * size
            zs = numpy.array(lsc_map[start:start+size]).reshape(w_map, h_map)
            ax.plot_wireframe(xs, ys, zs)
        matplotlib.pyplot.savefig("%s_plot_lsc_%s.png" % (NAME, name))

    def test_auto(cam, w_map, h_map):
        # Get 3A lock first, so the auto values in the capture result are
        # populated properly.
        rect = [0,0,1,1]
        cam.do_3a(rect, rect, rect, True, True, False)

        fname, w, h, cap_res = cam.do_capture(auto_req)
        gains = cap_res["android.colorCorrection.gains"]
        transform = cap_res["android.colorCorrection.transform"]
        exp_time = cap_res['android.sensor.exposureTime']
        lsc_map = cap_res["android.statistics.lensShadingMap"]
        ctrl_mode = cap_res["android.control.mode"]

        print "Control mode:", ctrl_mode
        print "Gains:", gains
        print "Transform:", [r2f(t) for t in transform]
        print "AE region:", cap_res['android.control.aeRegions']
        print "AF region:", cap_res['android.control.afRegions']
        print "AWB region:", cap_res['android.control.awbRegions']
        print "LSC map:", w_map, h_map, lsc_map[:8]

        assert(ctrl_mode == 1)

        # Color correction gain and transform must be valid.
        assert(len(gains) == 4)
        assert(len(transform) == 9)
        assert(all([g > 0 for g in gains]))
        assert(all([t["denominator"] != 0 for t in transform]))

        # Color correction should not match the manual settings.
        assert(any([not is_close_float(gains[i], manual_gains[i])
                    for i in xrange(4)]))
        assert(any([not is_close_rational(transform[i], manual_transform[i])
                    for i in xrange(9)]))

        # Exposure time must be valid.
        assert(exp_time > 0)

        # 3A regions must be valid.
        assert(len(cap_res['android.control.aeRegions']) == 5)
        assert(len(cap_res['android.control.afRegions']) == 5)
        assert(len(cap_res['android.control.awbRegions']) == 5)

        # Lens shading map must be valid.
        assert(w_map > 0 and h_map > 0 and w_map * h_map * 4 == len(lsc_map))
        assert(all([m >= 1 for m in lsc_map]))

        draw_lsc_plot(w_map, h_map, lsc_map, "auto")

        return lsc_map

    def test_manual(cam, w_map, h_map, lsc_map_auto):
        fname, w, h, cap_res = cam.do_capture(manual_req)
        gains = cap_res["android.colorCorrection.gains"]
        transform = cap_res["android.colorCorrection.transform"]
        curves = [cap_res["android.tonemap.curveRed"],
                  cap_res["android.tonemap.curveGreen"],
                  cap_res["android.tonemap.curveBlue"]]
        exp_time = cap_res['android.sensor.exposureTime']
        lsc_map = cap_res["android.statistics.lensShadingMap"]
        pred_gains = cap_res["android.statistics.predictedColorGains"]
        pred_transform = cap_res["android.statistics.predictedColorTransform"]
        ctrl_mode = cap_res["android.control.mode"]

        print "Control mode:", ctrl_mode
        print "Gains:", gains
        print "Transform:", [r2f(t) for t in transform]
        print "Predicted gains:", pred_gains
        print "Predicted transform:", [r2f(t) for t in pred_transform]
        print "Tonemap:", curves[0][1::16]
        print "AE region:", cap_res['android.control.aeRegions']
        print "AF region:", cap_res['android.control.afRegions']
        print "AWB region:", cap_res['android.control.awbRegions']
        print "LSC map:", w_map, h_map, lsc_map[:8]

        assert(ctrl_mode == 0)

        # Color correction gain and transform must be valid.
        # Color correction gains and transform should be the same size and
        # values as the manually set values.
        assert(len(gains) == 4)
        assert(len(transform) == 9)
        assert(all([is_close_float(gains[i], manual_gains[i])
                    for i in xrange(4)]))
        assert(all([is_close_rational(transform[i], manual_transform[i])
                    for i in xrange(9)]))

        # The predicted gains and transform must also be valid.
        assert(len(pred_gains) == 4)
        assert(len(pred_transform) == 9)

        # Tonemap must be valid.
        # The returned tonemap must be linear.
        for c in curves:
            assert(len(c) > 0)
            assert(all([is_close_float(c[i], c[i+1])
                        for i in xrange(0,len(c),2)]))

        # Exposure time must be close to the requested exposure time.
        assert(is_close_float(exp_time/1000000.0, manual_exp_time/1000000.0))

        # 3A regions must be valid. They don't need to actually match what was
        # requesed, since the SOC may not support those regions exactly.
        assert(len(cap_res['android.control.aeRegions']) == 5)
        assert(len(cap_res['android.control.afRegions']) == 5)
        assert(len(cap_res['android.control.awbRegions']) == 5)

        # Lens shading map must be valid.
        assert(w_map > 0 and h_map > 0 and w_map * h_map * 4 == len(lsc_map))
        assert(all([m >= 1 for m in lsc_map]))

        # Lens shading map must take into account the manual color correction
        # settings. Test this by ensuring that the map is different between
        # the auto and manual test cases.
        assert(lsc_map != lsc_map_auto)

        draw_lsc_plot(w_map, h_map, lsc_map, "manual")

    with its.device.ItsSession() as cam:
        props = cam.get_camera_properties()
        w_map = props["android.lens.info.shadingMapSize"]["width"]
        h_map = props["android.lens.info.shadingMapSize"]["height"]

        print "Testing auto capture results"
        lsc_map_auto = test_auto(cam, w_map, h_map)
        print "Testing manual capture results"
        test_manual(cam, w_map, h_map, lsc_map_auto)
        print "Testing auto capture results again"
        test_auto(cam, w_map, h_map)

if __name__ == '__main__':
    main()

