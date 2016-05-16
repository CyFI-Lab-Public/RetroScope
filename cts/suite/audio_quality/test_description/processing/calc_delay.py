#!/usr/bin/python

# Copyright (C) 2012 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import numpy as np
import numpy.linalg
import scipy as sp
import scipy.fftpack
import scipy.signal
import math
import sys
from multiprocessing import Pool

def convolution(data0, data1reversed, n):
    """calculate convolution part of data0 with data1 from pos n"""
    N = len(data1reversed)
    return np.dot(data0[n:N+n], data1reversed)


def convolutionstar(args):
    return convolution(*args)

def calc_delay(data0, data1):
    """Calcuate delay between two data. data0 is assumed to be recorded first,
       and will have longer length than data1
       returns delay between data0 and data1 in number of samples in data0's point of view"""

    len0 = len(data0)
    len1 = len(data1)
    if len1 > len0:
        print "data1 longer than data0"
        return -1
    searchLen = len0 - len1
    data1reverse = data1[::-1]

    # This is faster than signal.correlate as there is no need to process
    # full data, but still it is slow. about 18 secs for data0 of 4 secs with data1 of 1 secs
    print "***Caluclating delay, may take some time***"
    gData0 = data0
    gData1 = data1reverse
    pool = Pool(processes = 4)
    TASK = [(data0, data1reverse, i) for i in range(searchLen)]
    result = pool.map(convolutionstar, TASK)

    return np.argmax(result)


# test code
if __name__=="__main__":
    samplingRate = 44100
    durationInSec = 0.001
    if len(sys.argv) > 1:
        durationInSec = float(sys.argv[1])
    signalFrequency = 1000
    samples = float(samplingRate) * float(durationInSec)
    index = np.linspace(0.0, samples, num=samples, endpoint=False)
    time = index / samplingRate
    multiplier = 2.0 * np.pi * signalFrequency / float(samplingRate)
    data0 = np.sin(index * multiplier)
    DELAY = durationInSec / 2.0 * samplingRate
    data1 = data0[DELAY:]
    delay = calc_delay(data0, data1)
    print "calc_delay returned", delay, " while expecting ", DELAY


