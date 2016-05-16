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

import sys
import numpy as np
import scipy as sp
import socket
import struct
sys.path.append(sys.path[0] + "/processing")
from consts import *

builtinFunctions = [
    "echo", # send back whatever is received
    "intsum", # returns int64 + int64
]

CMD_HEADER    = 0x0
CMD_TERMINATE = 0x1
CMD_FUNCTION  = 0x2
CMD_AUDIO_MONO = 0x4
CMD_AUDIO_STEREO = 0x5
CMD_INT64 = 0x8
CMD_DOUBLE = 0x9
CMD_RESULT = 0x10

def echo(inputData, inputTypes):
    output = []
    print "echo received ", inputData
    output.append(RESULT_OK)
    output.append(inputData)
    output.append(inputTypes)
    return output

def intsum(inputData, inputTypes):
    output = []
    output.append(RESULT_OK)
    sum = inputData[0] + inputData[1]
    print "intsum sum is ", sum
    outputData = []
    outputData.append(sum)
    outputTypes = []
    outputTypes.append(TYPE_I64)
    output.append(outputData)
    output.append(outputTypes)
    return output


class CommandHandler(object):

    def __init__(self, conn):
        self.conn = conn
    def __del__(self):
        self.conn.close()
    def run(self):
        header = self.readI32()
        if header == CMD_TERMINATE:
            print "terminate cmd, will exit"
            sys.exit(0)
        nParam = 0
        if header == CMD_HEADER:
            nParam = self.readI32()
            if nParam < 1:
                protocolError("wrong number of params")
            cmdFunction = self.readI32()
            if cmdFunction != CMD_FUNCTION:
                protocolError("not function")
            nameLen = self.readI32()
            self.functionName = self.readRaw(nameLen)
            print "Processing function:", self.functionName
            inputData = []
            inputTypes = []
            for i in range(nParam - 1):
                cmd = self.readI32()
                if (cmd == CMD_AUDIO_STEREO) or (cmd == CMD_AUDIO_MONO):
                    dataLen = self.readI32()
                    data = self.readI16Array(dataLen / 2)
                    inputData.append(data)
                    if (cmd == CMD_AUDIO_STEREO):
                        inputTypes.append(TYPE_STEREO)
                    else:
                        inputTypes.append(TYPE_MONO)
                    print i, "-th input received audio data ", dataLen, cmd
                elif cmd == CMD_INT64:
                    i64 = self.readI64()
                    inputData.append(i64)
                    inputTypes.append(TYPE_I64)
                elif cmd == CMD_DOUBLE:
                    val = self.readDouble()
                    inputData.append(val)
                    inputTypes.append(TYPE_DOUBLE)
                else:
                    self.protocolError("unknown command " + str(cmd))
            print "inputTypes ", inputTypes
            # length 3 list
            # output[0]: int, execution result, RESULT_XXX values
            # output[1]: output data list
            # output[2]: output type list
            output = []
            if not self.functionName in builtinFunctions:
                mod = __import__(self.functionName)
                output = getattr(mod, self.functionName)(inputData, inputTypes)
            else:
                output = globals()[self.functionName](inputData, inputTypes)
            nOutputParams = len(output[1])
            self.sendI32(CMD_HEADER)
            self.sendI32(nOutputParams + 1) # 1 for result
            self.sendI32(CMD_RESULT)
            self.sendI32(output[0])
            outputData = output[1]
            outputTypes = output[2]
            print "outputTypes ", outputTypes
            for i in range(nOutputParams):
                if (outputTypes[i] == TYPE_I64):
                    self.sendI32(CMD_INT64)
                    self.sendI64(outputData[i])
                elif (outputTypes[i] == TYPE_DOUBLE):
                    self.sendI32(CMD_DOUBLE)
                    self.sendDouble(outputData[i])
                elif (outputTypes[i] == TYPE_STEREO):
                    self.sendI32(CMD_AUDIO_STEREO)
                    self.sendI32(len(outputData[i]) * 2)
                    self.sendI16Array(outputData[i])
                elif (outputTypes[i] == TYPE_MONO):
                    self.sendI32(CMD_AUDIO_MONO)
                    self.sendI32(len(outputData[i]) * 2)
                    self.sendI16Array(outputData[i])
                else:
                    print "unknown type ", outputTypes[i], \
                        " returned from funcion ", self.functionName
                    sys.exit(1)

    def readRaw(self, length):
        result = []
        totalRead = 0
        while totalRead < length:
            raw = self.conn.recv(length - totalRead)
            justRead = len(raw)
            if justRead == 0: # socket closed
                sys.exit(1)
            totalRead += justRead
            result.append(raw)
        return ''.join(result)

    def readI32(self):
        raw = self.readRaw(4)
        i32 = struct.unpack("<i", raw)
        return i32[0]

    def readI64(self):
        raw = self.readRaw(8)
        i64 = struct.unpack("<q", raw)
        return i64[0]

    def readDouble(self):
        raw = self.readRaw(8)
        val = struct.unpack("<d", raw)
        return val[0]

    def readI16Array(self, length):
        raw = self.readRaw(length * 2)
        data = np.fromstring(raw, dtype=np.int16)
        return data

    def sendI32(self, i32):
        raw = struct.pack("<i", i32)
        self.sendRaw(raw)

    def sendI64(self, i64):
        raw = struct.pack("<q", i64)
        self.sendRaw(raw)

    def sendDouble(self, val):
        raw = struct.pack("<d", val)
        self.sendRaw(raw)

    def sendI16Array(self, arry):
        raw = arry.tostring()
        self.sendRaw(raw)

    def sendRaw(self, rawString):
        totalSent = 0
        stringLen = len(rawString)
        while totalSent < stringLen:
            sent = self.conn.send(rawString[totalSent:])
            totalSent += sent

    def protocolError(self, message):
        print message
        sys.exit(1)


if __name__=="__main__":
    HOST = "localhost"
    PORT = 15010
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind((HOST, PORT))
    s.listen(1)

    conn, addr = s.accept()
    print "client connected"
    # close the server socket to allow other instance to run
    s.close()
    handler = CommandHandler(conn)
    while 1:
        handler.run()
