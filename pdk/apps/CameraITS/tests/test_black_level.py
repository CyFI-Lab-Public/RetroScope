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
    """Black level consistence test.

    Test: capture dark frames and check if black level correction is done
    correctly.
    1. Black level should be roughly consistent for repeating shots.
    2. Noise distribution should be roughly centered at black level.

    Shoot with the camera covered (i.e.) dark/black. The test varies the
    sensitivity parameter.
    """
    NAME = os.path.basename(__file__).split(".")[0]

    NUM_REPEAT = 3
    NUM_SENSITIVITY_STEPS = 3

    # Only check the center part where LSC has little effects.
    R = 200

    # The most frequent pixel value in each image; assume this is the black
    # level, since the images are all dark (shot with the lens covered).
    ymodes = []
    umodes = []
    vmodes = []

    with its.device.ItsSession() as cam:
        props = cam.get_camera_properties()
        sens_range = props['android.sensor.info.sensitivityRange']
        sensitivities = range(sens_range[0],
                              sens_range[1]+1,
                              int((sens_range[1] - sens_range[0]) /
                                  (NUM_SENSITIVITY_STEPS - 1)))
        print "Sensitivities:", sensitivities
        for si, s in enumerate(sensitivities):
            for rep in xrange(NUM_REPEAT):
                req = its.objects.manual_capture_request(100, 1)
                req["android.blackLevel.lock"] = True
                req["android.sensor.sensitivity"] = s
                fname, w, h, cap_md = cam.do_capture(req)
                yimg,uimg,vimg = its.image.load_yuv420_to_yuv_planes(fname,w,h)

                # Magnify the noise in saved images to help visualize.
                its.image.write_image(yimg * 2,
                                      "%s_s=%05d_y.jpg" % (NAME, s), True)
                its.image.write_image(numpy.absolute(uimg - 0.5) * 2,
                                      "%s_s=%05d_u.jpg" % (NAME, s), True)

                yimg = yimg[w/2-R:w/2+R, h/2-R:h/2+R]
                uimg = uimg[w/4-R/2:w/4+R/2, w/4-R/2:w/4+R/2]
                vimg = vimg[w/4-R/2:w/4+R/2, w/4-R/2:w/4+R/2]
                yhist,_ = numpy.histogram(yimg*255, 256, (0,256))
                ymodes.append(numpy.argmax(yhist))
                uhist,_ = numpy.histogram(uimg*255, 256, (0,256))
                umodes.append(numpy.argmax(uhist))
                vhist,_ = numpy.histogram(vimg*255, 256, (0,256))
                vmodes.append(numpy.argmax(vhist))

                # Take 32 bins from Y, U, and V.
                # Histograms of U and V are cropped at the center of 128.
                pylab.plot(range(32), yhist.tolist()[0:32], 'rgb'[si])
                pylab.plot(range(32), uhist.tolist()[112:144], 'rgb'[si]+'--')
                pylab.plot(range(32), vhist.tolist()[112:144], 'rgb'[si]+'--')

    pylab.xlabel("DN: Y[0:32], U[112:144], V[112:144]")
    pylab.ylabel("Pixel count")
    pylab.title("Histograms for different sensitivities")
    matplotlib.pyplot.savefig("%s_plot_histograms.png" % (NAME))

    print "Y black levels:", ymodes
    print "U black levels:", umodes
    print "V black levels:", vmodes

if __name__ == '__main__':
    main()

