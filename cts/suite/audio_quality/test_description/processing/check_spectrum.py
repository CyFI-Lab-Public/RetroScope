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
import sys
sys.path.append(sys.path[0])
import calc_delay

# check if amplitude ratio of DUT / Host signal
#  lies in the given error boundary
# input: host record
#        device record,
#        sampling rate
#        low frequency in Hz,
#        high frequency in Hz,
#        allowed error in negative side for pass in %,
#        allowed error ih positive side for pass
# output: min value in negative side, normalized to 1.0
#         max value in positive side
#         calculated amplittude ratio in magnitude (DUT / Host)

def do_check_spectrum(hostData, DUTData, samplingRate, fLow, fHigh, margainLow, margainHigh):
    # reduce FFT resolution to have averaging effects
    N = 512 if (len(hostData) > 512) else len(hostData)
    iLow = N * fLow / samplingRate + 1 # 1 for DC
    if iLow > (N / 2 - 1):
        iLow = (N / 2 - 1)
    iHigh = N * fHigh / samplingRate + 1 # 1 for DC
    if iHigh > (N / 2 + 1):
        iHigh = N / 2 + 1
    print fLow, iLow, fHigh, iHigh, samplingRate

    Phh, freqs = plt.psd(hostData, NFFT=N, Fs=samplingRate, Fc=0, detrend=plt.mlab.detrend_none,\
        window=plt.mlab.window_hanning, noverlap=0, pad_to=None, sides='onesided',\
        scale_by_freq=False)
    Pdd, freqs = plt.psd(DUTData, NFFT=N, Fs=samplingRate, Fc=0, detrend=plt.mlab.detrend_none,\
        window=plt.mlab.window_hanning, noverlap=0, pad_to=None, sides='onesided',\
        scale_by_freq=False)
    print len(Phh), len(Pdd)
    print "Phh", abs(Phh[iLow:iHigh])
    print "Pdd", abs(Pdd[iLow:iHigh])
    amplitudeRatio = np.sqrt(abs(Pdd[iLow:iHigh]/Phh[iLow:iHigh]))
    ratioMean = np.mean(amplitudeRatio)
    amplitudeRatio = amplitudeRatio / ratioMean
    print "Normialized ratio", amplitudeRatio
    print "ratio mean for normalization", ratioMean
    positiveMax = abs(max(amplitudeRatio))
    negativeMin = abs(min(amplitudeRatio))
    passFail = True if (positiveMax < (margainHigh / 100.0 + 1.0)) and\
        ((1.0 - negativeMin) < margainLow / 100.0) else False
    RatioResult = np.zeros(len(amplitudeRatio), dtype=np.int16)
    for i in range(len(amplitudeRatio)):
        RatioResult[i] = amplitudeRatio[i] * 1024 # make fixed point
    print "positiveMax", positiveMax, "negativeMin", negativeMin
    return (passFail, negativeMin, positiveMax, RatioResult)

def toMono(stereoData):
    n = len(stereoData)/2
    monoData = np.zeros(n)
    for i in range(n):
        monoData[i] = stereoData[2 * i]
    return monoData

def check_spectrum(inputData, inputTypes):
    output = []
    outputData = []
    outputTypes = []
    # basic sanity check
    inputError = False
    if (inputTypes[0] != TYPE_MONO) and (inputTypes[0] != TYPE_STEREO):
        inputError = True
    if (inputTypes[1] != TYPE_MONO) and (inputTypes[1] != TYPE_STEREO):
        inputError = True
    if (inputTypes[2] != TYPE_I64):
        inputError = True
    if (inputTypes[3] != TYPE_I64):
        inputError = True
    if (inputTypes[4] != TYPE_I64):
        inputError = True
    if (inputTypes[5] != TYPE_DOUBLE):
        inputError = True
    if (inputTypes[6] != TYPE_DOUBLE):
        inputError = True
    if inputError:
        print "input error"
        output.append(RESULT_ERROR)
        output.append(outputData)
        output.append(outputTypes)
        return output
    hostData = inputData[0]
    if inputTypes[0] == TYPE_STEREO:
        hostData = toMono(hostData)
    dutData = inputData[1]
    if inputTypes[1] == TYPE_STEREO:
        dutData = toMono(dutData)
    samplingRate = inputData[2]
    fLow = inputData[3]
    fHigh = inputData[4]
    margainLow = inputData[5]
    margainHigh = inputData[6]
    delay = 0
    N = 0
    hostData_ = hostData
    dutData_ = dutData
    if len(hostData) > len(dutData):
        delay = calc_delay.calc_delay(hostData, dutData)
        N = len(dutData)
        hostData_ = hostData[delay:delay+N]
    if len(hostData) < len(dutData):
        delay = calc_delay.calc_delay(dutData, hostData)
        N = len(hostData)
        dutData_ = dutData[delay:delay+N]

    print "delay ", delay, "deviceRecording samples ", N
    (passFail, minError, maxError, TF) = do_check_spectrum(hostData_, dutData_,\
        samplingRate, fLow, fHigh, margainLow, margainHigh)

    if passFail:
        output.append(RESULT_PASS)
    else:
        output.append(RESULT_OK)
    outputData.append(minError)
    outputTypes.append(TYPE_DOUBLE)
    outputData.append(maxError)
    outputTypes.append(TYPE_DOUBLE)
    outputData.append(TF)
    outputTypes.append(TYPE_MONO)
    output.append(outputData)
    output.append(outputTypes)
    return output

# test code
if __name__=="__main__":
    sys.path.append(sys.path[0])
    mod = __import__("gen_random")
    peakAmpl = 10000
    durationInMSec = 1000
    samplingRate = 44100
    fLow = 500
    fHigh = 15000
    data = getattr(mod, "do_gen_random")(peakAmpl, durationInMSec, samplingRate, fHigh,\
        stereo=False)
    print len(data)
    (passFail, minVal, maxVal, ampRatio) = do_check_spectrum(data, data, samplingRate, fLow, fHigh,\
        1.0, 1.0)
    plt.plot(ampRatio)
    plt.show()
