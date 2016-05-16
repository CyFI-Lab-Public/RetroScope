/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


#ifndef CTASUDIO_CLIENTSOCKET_H
#define CTASUDIO_CLIENTSOCKET_H

class ClientSocket {
public:
    ClientSocket();
    virtual ~ClientSocket();
    virtual bool init(const char* hostIp, int port, bool enableTimeout = false);
    /**
     * @param timeoutInMs 0 means no time-out
     */
    virtual bool readData(char* data, int len, int timeoutInMs = 0);
    virtual bool sendData(const char* data, int len);
    int getFD() {
        return mSocket;
    }
    virtual void release();
protected:
    int mSocket;
    bool mTimeoutEnabled;
};


#endif // CTASUDIO_CLIENTSOCKET_H
