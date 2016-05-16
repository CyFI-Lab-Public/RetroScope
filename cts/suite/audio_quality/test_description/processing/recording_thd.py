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

# calculate THD for dut_recording_thd case
# Input: host recording (mono), device recording (mono),
#        frequency of sine in Hz (i64)
#        THD pass level in percentile (double)
# Output:THD host (double), THD device (double) in percentile
# host recording will be longer than device recording
# the function works in following steps:
# 1. match the start of device recording with host recording
#    As the host recording starts eariler and longer than device recording,
#    matching process is required.
# 2. calculate THD of host recording and client recording
# 3. check pass/fail

def recording_thd(inputData, inputTypes):
    output = []
    outputData = []
    outputTypes = []
    # basic sanity check
    inputError = False
    if (inputTypes[0] != TYPE_MONO):
        inputError = True
    if (inputTypes[1] != TYPE_MONO):
        inputError = True
    if (inputTypes[2] != TYPE_I64):
        inputError = True
    if (inputTypes[3] != TYPE_DOUBLE):
        inputError = True
    if inputError:
        output.append(RESULT_ERROR)
        output.append(outputData)
        output.append(outputTypes)
        return output

    hostRecording = inputData[0]
    deviceRecording = inputData[1]
    signalFrequency = inputData[2]
    thdPassPercentile = inputData[3]
    samplingRate = 44100

    delay = calc_delay.calc_delay(hostRecording, deviceRecording)
    N = len(deviceRecording)
    print "delay ", delay, "deviceRecording samples ", N
    thdHost = calc_thd(hostRecording[delay:delay+N], signalFrequency, samplingRate, 0.02) * 100
    thdDevice = calc_thd(deviceRecording, signalFrequency, samplingRate, 0.02) * 100
    print "THD Host %", thdHost, "THD device %", thdDevice, "Margain % ", thdPassPercentile
    if (thdDevice < (thdHost + thdPassPercentile)) and (thdHost < thdPassPercentile):
        output.append(RESULT_PASS)
    else:
        output.append(RESULT_OK)
    outputData.append(thdHost)
    outputTypes.append(TYPE_DOUBLE)
    outputData.append(thdDevice)
    outputTypes.append(TYPE_DOUBLE)
    output.append(outputData)
    output.append(outputTypes)
    return output
