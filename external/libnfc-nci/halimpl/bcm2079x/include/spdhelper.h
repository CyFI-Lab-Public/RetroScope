/******************************************************************************
 *
 *  Copyright (C) 2012 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

#pragma once

#include <string>

using namespace std;
#define DEFAULT_SPD_MAXRETRYCOUNT (3)

class SpdHelper
{
public:
    static bool isPatchBad(UINT8* prm, UINT32 len);
    static void setPatchAsBad();
    static void incErrorCount();
    static bool isSpdDebug();

private:
    SpdHelper();
    static SpdHelper& getInstance();

    bool isPatchBadImpl(UINT8* prm, UINT32 len);
    void setPatchAsBadImpl();
    void incErrorCountImpl();
    bool isSpdDebugImpl() {return mSpdDebug;}
    string mPatchId;
    int  mErrorCount;
    int  mMaxErrorCount;
    bool mIsPatchBad;
    bool mSpdDebug;
};
