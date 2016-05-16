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

# Example python script for signal processing in CTS audio
# There should be a function with the same name as the script
# Here, function example in example.py

# inputData : list of inputs with different types like int64, double,
#             mono or stereo audio data
# inputTypes: list of types for each input. Types are defined as TYPE_XXX
#             consts from consts.py
# return value: 3 elements list
#     element 0 : execution result value as defined as RESULT_XXX in consts.py
#     element 1 : outputData
#     element 2 : outputTypes
#
# This example takes 2 stereo data, 2 mono data, 2 i64, and 2 doubles
# and returns average as 1 stereo data, 1 mono data, 1 i64, and 1 double
# inputTypes for this function is expected to be
#   [ TYPE_STEREO, TYPE_STEREO, TYPE_MONO, TYPE_MONO, TYPE_I64, TYPE_I64,
#     TYPE_DOUBLE, TYPE_DOUBLE ]
# outputTypes will be [ TYPE_STEREO, TYPE_MONO, TYPE_I64, TYPE_DOUBLE ]
def example(inputData, inputTypes):
    output = []
    outputData = []
    outputTypes = []
    stereoInt = (inputData[0].astype(int) + inputData[1].astype(int))/2
    stereo = stereoInt.astype(np.int16)
    #print len(inputData[0]), len(inputData[1]), len(stereoInt), len(stereo)
    monoInt = (inputData[2].astype(int) + inputData[3].astype(int))/2
    mono = monoInt.astype(np.int16)
    #print len(inputData[2]), len(inputData[3]), len(monoInt), len(mono)
    i64Val = (inputData[4] + inputData[5])/2
    doubleVal = (inputData[6] + inputData[7])/2
    outputData.append(stereo)
    outputTypes.append(TYPE_STEREO)
    outputData.append(mono)
    outputTypes.append(TYPE_MONO)
    outputData.append(i64Val)
    outputTypes.append(TYPE_I64)
    outputData.append(doubleVal)
    outputTypes.append(TYPE_DOUBLE)
    output.append(RESULT_OK)
    output.append(outputData)
    output.append(outputTypes)

    return output
