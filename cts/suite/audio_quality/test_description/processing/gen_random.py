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
import scipy.fftpack as fft
import matplotlib.pyplot as plt

# generate random signal with max freq
# Input: peak amplitude,
#        duration in msec,
#        sampling rate HZ
#        high frequency,
# Output: generated sound (stereo)

def do_gen_random(peakAmpl, durationInMSec, samplingRate, fHigh, stereo=True):
    samples = durationInMSec * samplingRate / 1000
    result = np.zeros(samples * 2 if stereo else samples, dtype=np.int16)
    randomSignal = np.random.normal(scale = peakAmpl * 2 / 3, size=samples)
    fftData = fft.rfft(randomSignal)
    freqSamples = samples/2
    iHigh = freqSamples * fHigh * 2 / samplingRate + 1
    #print len(randomSignal), len(fftData), fLow, fHigh, iHigh
    if iHigh > freqSamples - 1:
        iHigh = freqSamples - 1
    fftData[0] = 0 # DC
    for i in range(iHigh, freqSamples - 1):
        fftData[ 2 * i + 1 ] = 0
        fftData[ 2 * i + 2 ] = 0
    if (samples - 2 *freqSamples) != 0:
        fftData[samples - 1] = 0

    filteredData = fft.irfft(fftData)
    #freq = np.linspace(0.0, samplingRate, num=len(fftData), endpoint=False)
    #plt.plot(freq, abs(fft.fft(filteredData)))
    #plt.plot(filteredData)
    #plt.show()
    if stereo:
        for i in range(len(filteredData)):
            result[2 * i] = filteredData[i]
            result[2 * i + 1] = filteredData[i]
    else:
        for i in range(len(filteredData)):
            result[i] = filteredData[i]
    return result


def gen_random(inputData, inputTypes):
    output = []
    outputData = []
    outputTypes = []
    # basic sanity check
    inputError = False
    if (inputTypes[0] != TYPE_I64):
        inputError = True
    if (inputTypes[1] != TYPE_I64):
        inputError = True
    if (inputTypes[2] != TYPE_I64):
        inputError = True
    if (inputTypes[3] != TYPE_I64):
        inputError = True
    if inputError:
        output.append(RESULT_ERROR)
        output.append(outputData)
        output.append(outputTypes)
        return output

    result = do_gen_random(inputData[0], inputData[1], inputData[2], inputData[3])

    output.append(RESULT_OK)
    outputData.append(result)
    outputTypes.append(TYPE_STEREO)
    output.append(outputData)
    output.append(outputTypes)
    return output

# test code
if __name__=="__main__":
    peakAmplitude = 10000
    samplingRate = 44100
    durationInMSec = 10000
    #fLow = 500
    fHigh = 15000
    result = do_gen_random(peakAmplitude, durationInMSec, samplingRate, fHigh)
    plt.plot(result)
    plt.show()
