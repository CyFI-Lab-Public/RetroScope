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

# check if amplitude of DUT's playback
#  lies in the given error boundary
# input: host record
#        sampling rate
#        low frequency in Hz,
#        high frequency in Hz,
#        allowed error in negative side for pass in %,
#        allowed error ih positive side for pass
# output: min value in negative side, normalized to 1.0
#         max value in positive side
#         calculated freq spectrum in amplittude

def do_check_spectrum_playback(hostData, samplingRate, fLow, fHigh, margainLow, margainHigh):
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
    print len(Phh)
    print "Phh", abs(Phh[iLow:iHigh])
    spectrum = np.sqrt(abs(Phh[iLow:iHigh]))
    spectrumMean = np.mean(spectrum)
    spectrum = spectrum / spectrumMean
    print "Mean ", spectrumMean
    print "Normalized spectrum", spectrum
    positiveMax = abs(max(spectrum))
    negativeMin = abs(min(spectrum))
    passFail = True if (positiveMax < (margainHigh / 100.0 + 1.0)) and\
        ((1.0 - negativeMin) < margainLow / 100.0) else False
    spectrumResult = np.zeros(len(spectrum), dtype=np.int16)
    for i in range(len(spectrum)):
        spectrumResult[i] = spectrum[i] * 1024 # make fixed point
    print "positiveMax", positiveMax, "negativeMin", negativeMin
    return (passFail, negativeMin, positiveMax, spectrumResult)

def check_spectrum_playback(inputData, inputTypes):
    output = []
    outputData = []
    outputTypes = []
    # basic sanity check
    inputError = False
    if (inputTypes[0] != TYPE_MONO):
        inputError = True
    if (inputTypes[1] != TYPE_I64):
        inputError = True
    if (inputTypes[2] != TYPE_I64):
        inputError = True
    if (inputTypes[3] != TYPE_I64):
        inputError = True
    if (inputTypes[4] != TYPE_DOUBLE):
        inputError = True
    if (inputTypes[5] != TYPE_DOUBLE):
        inputError = True
    if inputError:
        output.append(RESULT_ERROR)
        output.append(outputData)
        output.append(outputTypes)
        return output
    hostData = inputData[0]
    samplingRate = inputData[1]
    fLow = inputData[2]
    fHigh = inputData[3]
    margainLow = inputData[4]
    margainHigh = inputData[5]
    (passFail, minError, maxError, Spectrum) = do_check_spectrum_playback(hostData, \
        samplingRate, fLow, fHigh, margainLow, margainHigh)

    if passFail:
        output.append(RESULT_PASS)
    else:
        output.append(RESULT_OK)
    outputData.append(minError)
    outputTypes.append(TYPE_DOUBLE)
    outputData.append(maxError)
    outputTypes.append(TYPE_DOUBLE)
    outputData.append(Spectrum)
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
    (passFail, minVal, maxVal, amp) = do_check_spectrum_playback(data, samplingRate, fLow,\
        fHigh, 1.0, 1.0)
    plt.plot(amp)
    plt.show()
