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

from consts import *
import numpy as np
import scipy as sp
from calc_thd import *
import calc_delay

# calculate THD for dut_playback_thd case
# Input: host recording (mono),
#        frequency of sine in Hz (i64)
#        THD pass level in percentile (double)
# Output: THD device (double) in percentile

def playback_thd(inputData, inputTypes):
    output = []
    outputData = []
    outputTypes = []
    # basic sanity check
    inputError = False
    if (inputTypes[0] != TYPE_MONO):
        inputError = True
    if (inputTypes[1] != TYPE_I64):
        inputError = True
    if (inputTypes[2] != TYPE_DOUBLE):
        inputError = True
    if inputError:
        output.append(RESULT_ERROR)
        output.append(outputData)
        output.append(outputTypes)
        return output

    hostRecording = inputData[0]
    signalFrequency = inputData[1]
    thdPassPercentile = inputData[2]
    samplingRate = 44100

    thd = calc_thd(hostRecording, signalFrequency, samplingRate, 0.02) * 100
    print "THD %", thd, "Margain % ", thdPassPercentile
    if (thd < thdPassPercentile):
        output.append(RESULT_PASS)
    else:
        output.append(RESULT_OK)
    outputData.append(thd)
    outputTypes.append(TYPE_DOUBLE)
    output.append(outputData)
    output.append(outputTypes)
    return output
