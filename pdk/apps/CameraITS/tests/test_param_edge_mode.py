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
    """Test that the android.edge.mode parameter is applied.
    """
    NAME = os.path.basename(__file__).split(".")[0]

    req = {
        "android.control.mode": 0,
        "android.control.aeMode": 0,
        "android.control.awbMode": 0,
        "android.control.afMode": 0,
        "android.sensor.frameDuration": 0,
        "android.sensor.exposureTime": 30*1000*1000,
        "android.sensor.sensitivity": 100
        }

    with its.device.ItsSession() as cam:
        rect = [0,0,1,1]
        sens, exp, gains, xform, focus = cam.do_3a(rect, rect, rect)
        for e in [0,1,2]:
            req["android.edge.mode"] = e
            fname, w, h, cap_md = cam.do_capture(req)
            img = its.image.load_yuv420_to_rgb_image(fname, w, h)
            its.image.write_image(img, "%s_mode=%d.jpg" % (NAME, e))

if __name__ == '__main__':
    main()

