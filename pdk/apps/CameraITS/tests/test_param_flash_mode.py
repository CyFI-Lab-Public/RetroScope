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
    """Test that the android.flash.mode parameter is applied.
    """
    NAME = os.path.basename(__file__).split(".")[0]

    req = its.objects.auto_capture_request()

    flash_modes_reported = []
    flash_states_reported = []

    with its.device.ItsSession() as cam:
        for f in [0,1,2]:
            req["android.flash.mode"] = f
            fname, w, h, cap_md = cam.do_capture(req)
            flash_modes_reported.append(cap_md["android.flash.mode"])
            flash_states_reported.append(cap_md["android.flash.state"])
            img = its.image.load_yuv420_to_rgb_image(fname, w, h)
            its.image.write_image(img, "%s_mode=%d.jpg" % (NAME, f))

    assert(flash_modes_reported == [0,1,2])

    # TODO: Add check on flash_states_reported values.

    # TODO: Add an image check on the brightness of the captured shots, as well
    # as the exposure values in the capture result, to test that flash was
    # fired as expected (i.e.) on the shots it was expected to be fired for.

if __name__ == '__main__':
    main()

