/******************************************************************************
 *
 *  Copyright (C) 1999-2012 Broadcom Corporation
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

/******************************************************************************
 * Construct a buffer that contains multiple Type-Length-Value contents
 * that is used by the HAL in a CORE_SET_CONFIG NCI command.
 ******************************************************************************/

#define LOG_TAG "NfcNciHal"
#include "OverrideLog.h"
#include "StartupConfig.h"


const UINT8 StartupConfig::mMaxLength = 255;


/*******************************************************************************
**
** Function:        initialize
**
** Description:     Initialize all member variables.
**
** Returns:         None
**
*******************************************************************************/
StartupConfig::StartupConfig ()
{
    //set first byte to 0, which is length of payload
    mBuffer.append ((uint8_string::size_type) 1, (uint8_string::value_type) 0);
}


/*******************************************************************************
**
** Function:        initialize
**
** Description:     Reset all member variables.
**
** Returns:         None
**
*******************************************************************************/
void StartupConfig::initialize ()
{
    mBuffer.clear ();
    //set first byte to 0, which is length of payload
    mBuffer.append ((uint8_string::size_type) 1, (uint8_string::value_type) 0);
}


/*******************************************************************************
**
** Function:        getInternalBuffer
**
** Description:     Get the pointer to buffer that contains multiple
**                  Type-Length-Value contents.
**
** Returns:         Pointer to buffer.
**
*******************************************************************************/
const UINT8* StartupConfig::getInternalBuffer ()
{
    return mBuffer.data ();
}


/*******************************************************************************
**
** Function:        append
**
** Description:     Append new config data to internal buffer.
**                  newContent: buffer containing new content; newContent[0] is
**                          payload length; newContent[1..end] is payload.
**
** Returns:         True if ok.
**
*******************************************************************************/
bool StartupConfig::append (const UINT8* newContent)
{
    static const char fn [] = "StartupConfig::append";
    if ((newContent[0]+mBuffer.size()) > mMaxLength)
    {
        ALOGE ("%s: exceed max length", fn);
        return false;
    }
    ALOGD ("%s: try append %u bytes", fn, (uint8_string::size_type) (newContent[0]));
    //append new payload into private buffer
    mBuffer.append (newContent+1, (uint8_string::size_type) (newContent[0]));
    //increase size counter of payload in private buffer
    mBuffer[0] = mBuffer[0] + newContent[0];
    ALOGD ("%s: new size %u bytes", fn, mBuffer[0]);
    return true;
};


/*******************************************************************************
**
** Function:        append
**
** Description:     Append new config data to internal buffer.
**                  newContent: buffer containing new content; newContent[0] is
**                          payload length; newContent[1..end] is payload.
**                  newContentLen: total length of newContent.
**
** Returns:         True if ok.
**
*******************************************************************************/
bool StartupConfig::append (const UINT8* newContent, UINT8 newContentLen)
{
    static const char fn [] = "StartupConfig::append";
    if ((newContent[0]+1) != newContentLen)
    {
        ALOGE ("%s: invalid length at newContent[0]", fn);
        return false;
    }
    return append (newContent);
};


/*******************************************************************************
**
** Function:        disableSecureElement
**
** Description:     Adjust a TLV to disable secure element(s).  The TLV's type is 0xC2.
**                  bitmask: 0xC0 = do not detect any secure element.
**                           0x40 = do not detect secure element in slot 0.
**                           0x80 = do not detect secure element in slot 1.
**
** Returns:         True if ok.
**
*******************************************************************************/
bool StartupConfig::disableSecureElement (UINT8 bitmask)
{
    const UINT8 maxLen = mBuffer[0];
    UINT8 index = 1, tlvType = 0, tlvLen = 0;
    bool found0xC2 = false;

    while (true)
    {
        if (index > maxLen)
            break;
        tlvType = mBuffer [index];
        index++;
        tlvLen = mBuffer [index];
        index++;
        if (tlvType == 0xC2) //this TLV controls secure elements
        {
            index++; //index of second byte in TLV's value
            mBuffer [index] = mBuffer [index] | bitmask; //turn on certain bits
            found0xC2 = true;
        }
        else
            index += tlvLen;
    }

    if (found0xC2 == false)
    {
        UINT8 tlv [] = {0x04, 0xC2, 0x02, 0x61, 0x00};
        tlv [4] = tlv [4] | bitmask;
        found0xC2 = append (tlv);
    }
    return found0xC2;
}
