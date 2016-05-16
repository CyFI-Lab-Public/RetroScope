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
import numpy

def main():
    """Test that when the black level is locked, it doesn't change.

    Shoot with the camera covered (i.e.) dark/black. The test varies the
    sensitivity parameter and checks if the black level changes.
    """
    NAME = os.path.basename(__file__).split(".")[0]

    NUM_STEPS = 5

    req = {
        "android.blackLevel.lock": True,
        "android.control.mode": 0,
        "android.control.aeMode": 0,
        "android.control.awbMode": 0,
        "android.control.afMode": 0,
        "android.sensor.frameDuration": 0,
        "android.sensor.exposureTime": 10*1000*1000
        }

    # The most frequent pixel value in each image; assume this is the black
    # level, since the images are all dark (shot with the lens covered).
    modes = []

    with its.device.ItsSession() as cam:
        props = cam.get_camera_properties()
        sens_range = props['android.sensor.info.sensitivityRange']
        sensitivities = range(sens_range[0],
                              sens_range[1]+1,
                              int((sens_range[1] - sens_range[0]) / NUM_STEPS))
        for si, s in enumerate(sensitivities):
            req["android.sensor.sensitivity"] = s
            fname, w, h, cap_md = cam.do_capture(req)
            yimg,_,_ = its.image.load_yuv420_to_yuv_planes(fname, w, h)
            hist,_ = numpy.histogram(yimg*255, 256, (0,256))
            modes.append(numpy.argmax(hist))

            # Add this histogram to a plot; solid for shots without BL
            # lock, dashes for shots with BL lock
            pylab.plot(range(16), hist.tolist()[:16])

    pylab.xlabel("Luma DN, showing [0:16] out of full [0:256] range")
    pylab.ylabel("Pixel count")
    pylab.title("Histograms for different sensitivities")
    matplotlib.pyplot.savefig("%s_plot_histograms.png" % (NAME))

    # Check that the black levels are all the same.
    print "Black levels:", modes
    assert(all([modes[i] == modes[0] for i in range(len(modes))]))

if __name__ == '__main__':
    main()

