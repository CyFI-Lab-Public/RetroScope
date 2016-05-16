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
#include <arpa/inet.h>
#include <errno.h>
#include <strings.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <unistd.h>
#include <fcntl.h>

#include "Log.h"

#include "ClientSocket.h"

ClientSocket::ClientSocket()
    : mSocket(-1),
      mTimeoutEnabled(false)
{

}

ClientSocket::~ClientSocket()
{
    release();
}

bool ClientSocket::init(const char* hostIp, int port, bool enableTimeout)
{
    LOGD("ClientSocket::init");
    mSocket = socket(AF_INET, SOCK_STREAM, 0);
    if (mSocket < 0) {
        LOGE("cannot open socket %d", errno);
        return false;
    }
    int reuse = 1;
    if (setsockopt(mSocket, SOL_SOCKET, SO_REUSEADDR, &reuse, sizeof(reuse)) == -1) {
        LOGE("setsockopt error %d", errno);
        release();
        return false;
    }

    struct sockaddr_in serverAddr;
    bzero((char*)&serverAddr, sizeof(serverAddr));
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(port);
    if (inet_pton(AF_INET, hostIp, &serverAddr.sin_addr) != 1) {
        release();
        LOGE("inet_pton failed %d", errno);
        return false;
    }
    if (connect(mSocket, (struct sockaddr *)&serverAddr, sizeof(serverAddr)) < 0) {
        release();
        LOGE("cannot connect socket %d", errno);
        return false;
    }
    mTimeoutEnabled = enableTimeout;
    return true;
}

const int ZERO_RW_SLEEP_TIME_US = 10;

// make non-blocking mode only during read. This allows supporting time-out for read
bool ClientSocket::readData(char* data, int len, int timeoutInMs)
{
    bool useTimeout = (mTimeoutEnabled && (timeoutInMs > 0));
    int flOriginal = 0;
    int timeInSec = 0;
    int timeInUs = 0;
    if (useTimeout) {
        flOriginal = fcntl(mSocket, F_GETFL,0);
        if (flOriginal == -1) {
            LOGE("fcntl error %d", errno);
            return false;
        }
        if (fcntl(mSocket, F_SETFL, flOriginal | O_NONBLOCK) == -1) {
            LOGE("fcntl error %d", errno);
            return false;
        }
        timeInSec = timeoutInMs / 1000;
        timeInUs = (timeoutInMs % 1000) * 1000;
    }
    bool result = true;
    int read;
    int toRead = len;
    while (toRead > 0) {
        if (useTimeout) {
            fd_set rfds;
            struct timeval tv;
            tv.tv_sec = timeInSec;
            tv.tv_usec = timeInUs;
            FD_ZERO(&rfds);
            FD_SET(mSocket, &rfds);
            if (select(mSocket + 1, &rfds, NULL, NULL, &tv) == -1) {
                LOGE("select failed");
                result = false;
                break;
            }
            if (!FD_ISSET(mSocket, &rfds)) {
                LOGE("socket read timeout");
                result = false;
                break;
            }
        }
        read = recv(mSocket, (void*)data, toRead, 0);
        if (read > 0) {
            toRead -= read;
            data += read;
        } else if (read == 0) {
            // in blocking mode, zero read mean's peer closed.
            // in non-blocking mode, select said that there is data. so it should not happen
            LOGE("zero read, peer closed or what?, nonblocking: %d", useTimeout);
            result = false;
            break;
        } else {
            LOGE("recv returned %d", read);
            result = false;
            break;
        }
    }
    if (useTimeout) {
        fcntl(mSocket, F_SETFL, flOriginal); // now blocking again
    }
    return result;
}

bool ClientSocket::sendData(const char* data, int len)
{
    int sent;
    int toSend = len;
    while (toSend > 0) {
        sent = send(mSocket, (void*)data, (size_t)toSend, 0);
        if (sent > 0) {
            toSend -= sent;
            data += sent;
        } else if (sent == 0) { // no more buffer?
            usleep(ZERO_RW_SLEEP_TIME_US); // just wait
        } else {
            LOGE("send returned %d, error %d", sent, errno);
            return false;
        }
    }
    return true;
}

void ClientSocket::release()
{
    if (mSocket != -1) {
        close(mSocket);
        mSocket = -1;
    }
}
