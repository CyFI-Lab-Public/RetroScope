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

import its.device

def main():
    """Basic test for bring-up of 3A.

    To pass, 3A must converge. Check that the returned 3A values are legal.
    """

    with its.device.ItsSession() as cam:
        rect = [0,0,1,1]
        sens, exp, gains, xform, focus = cam.do_3a(rect, rect, rect)
        print "AE: sensitivity %d, exposure %dms" % (sens, exp/1000000)
        print "AWB: gains", gains, "transform", xform
        print "AF: distance", focus
        assert(sens > 0)
        assert(exp > 0)
        assert(len(gains) == 4)
        assert(len(xform) == 9)
        assert(focus > 0)

if __name__ == '__main__':
    main()

