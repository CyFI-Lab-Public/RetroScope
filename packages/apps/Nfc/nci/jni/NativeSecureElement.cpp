/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include "OverrideLog.h"
#include "SecureElement.h"
#include "JavaClassConstants.h"
#include "PowerSwitch.h"
#include "NfcTag.h"
#include <ScopedPrimitiveArray.h>

namespace android
{


extern void com_android_nfc_NfcManager_disableDiscovery (JNIEnv* e, jobject o);
extern void com_android_nfc_NfcManager_enableDiscovery (JNIEnv* e, jobject o, jint mode);

// These must match the EE_ERROR_ types in NfcService.java
static const int EE_ERROR_IO = -1;
static const int EE_ERROR_ALREADY_OPEN = -2;
static const int EE_ERROR_INIT = -3;
static const int EE_ERROR_LISTEN_MODE = -4;
static const int EE_ERROR_EXT_FIELD = -5;
static const int EE_ERROR_NFC_DISABLED = -6;

/*******************************************************************************
**
** Function:        nativeNfcSecureElement_doOpenSecureElementConnection
**
** Description:     Connect to the secure element.
**                  e: JVM environment.
**                  o: Java object.
**
** Returns:         Handle of secure element.  values < 0 represent failure.
**
*******************************************************************************/
static jint nativeNfcSecureElement_doOpenSecureElementConnection (JNIEnv*, jobject)
{
    ALOGD("%s: enter", __FUNCTION__);
    bool stat = true;
    jint secElemHandle = EE_ERROR_INIT;
    SecureElement &se = SecureElement::getInstance();

    if (se.isActivatedInListenMode()) {
        ALOGD("Denying SE open due to SE listen mode active");
        secElemHandle = EE_ERROR_LISTEN_MODE;
        goto TheEnd;
    }

    if (se.isRfFieldOn()) {
        ALOGD("Denying SE open due to SE in active RF field");
        secElemHandle = EE_ERROR_EXT_FIELD;
        goto TheEnd;
    }
    //tell the controller to power up to get ready for sec elem operations
    PowerSwitch::getInstance ().setLevel (PowerSwitch::FULL_POWER);
    PowerSwitch::getInstance ().setModeOn (PowerSwitch::SE_CONNECTED);

    //if controller is not routing AND there is no pipe connected,
    //then turn on the sec elem
    if (! se.isBusy())
        stat = se.activate(0);

    if (stat)
    {
        //establish a pipe to sec elem
        stat = se.connectEE();
        if (stat)
        {
            secElemHandle = se.mActiveEeHandle;
        }
        else
        {
            se.deactivate (0);
        }
    }

    //if code fails to connect to the secure element, and nothing is active, then
    //tell the controller to power down
    if ((!stat) && (! PowerSwitch::getInstance ().setModeOff (PowerSwitch::SE_CONNECTED)))
    {
        PowerSwitch::getInstance ().setLevel (PowerSwitch::LOW_POWER);
    }

TheEnd:
    ALOGD("%s: exit; return handle=0x%X", __FUNCTION__, secElemHandle);
    return secElemHandle;
}


/*******************************************************************************
**
** Function:        nativeNfcSecureElement_doDisconnectSecureElementConnection
**
** Description:     Disconnect from the secure element.
**                  e: JVM environment.
**                  o: Java object.
**                  handle: Handle of secure element.
**
** Returns:         True if ok.
**
*******************************************************************************/
static jboolean nativeNfcSecureElement_doDisconnectSecureElementConnection (JNIEnv*, jobject, jint handle)
{
    ALOGD("%s: enter; handle=0x%04x", __FUNCTION__, handle);
    bool stat = false;

    stat = SecureElement::getInstance().disconnectEE (handle);

    //if controller is not routing AND there is no pipe connected,
    //then turn off the sec elem
    if (! SecureElement::getInstance().isBusy())
        SecureElement::getInstance().deactivate (handle);

    //if nothing is active after this, then tell the controller to power down
    if (! PowerSwitch::getInstance ().setModeOff (PowerSwitch::SE_CONNECTED))
        PowerSwitch::getInstance ().setLevel (PowerSwitch::LOW_POWER);

    ALOGD("%s: exit", __FUNCTION__);
    return stat ? JNI_TRUE : JNI_FALSE;
}


/*******************************************************************************
**
** Function:        nativeNfcSecureElement_doTransceive
**
** Description:     Send data to the secure element; retrieve response.
**                  e: JVM environment.
**                  o: Java object.
**                  handle: Secure element's handle.
**                  data: Data to send.
**
** Returns:         Buffer of received data.
**
*******************************************************************************/
static jbyteArray nativeNfcSecureElement_doTransceive (JNIEnv* e, jobject, jint handle, jbyteArray data)
{
    const INT32 recvBufferMaxSize = 1024;
    UINT8 recvBuffer [recvBufferMaxSize];
    INT32 recvBufferActualSize = 0;
    int timeout = NfcTag::getInstance ().getTransceiveTimeout (TARGET_TYPE_ISO14443_4); //NFC service expects JNI to use ISO-DEP's timeout
    ScopedByteArrayRW bytes(e, data);

    ALOGD("%s: enter; handle=0x%X; buf len=%zu", __FUNCTION__, handle, bytes.size());
    SecureElement::getInstance().transceive(reinterpret_cast<UINT8*>(&bytes[0]), bytes.size(), recvBuffer, recvBufferMaxSize, recvBufferActualSize, timeout);

    //copy results back to java
    jbyteArray result = e->NewByteArray(recvBufferActualSize);
    if (result != NULL)
    {
        e->SetByteArrayRegion(result, 0, recvBufferActualSize, (jbyte *) recvBuffer);
    }

    ALOGD("%s: exit: recv len=%ld", __FUNCTION__, recvBufferActualSize);
    return result;
}

/*****************************************************************************
**
** Description:     JNI functions
**
*****************************************************************************/
static JNINativeMethod gMethods[] =
{
   {"doNativeOpenSecureElementConnection", "()I", (void *) nativeNfcSecureElement_doOpenSecureElementConnection},
   {"doNativeDisconnectSecureElementConnection", "(I)Z", (void *) nativeNfcSecureElement_doDisconnectSecureElementConnection},
   {"doTransceive", "(I[B)[B", (void *) nativeNfcSecureElement_doTransceive},
};


/*******************************************************************************
**
** Function:        register_com_android_nfc_NativeNfcSecureElement
**
** Description:     Regisgter JNI functions with Java Virtual Machine.
**                  e: Environment of JVM.
**
** Returns:         Status of registration.
**
*******************************************************************************/
int register_com_android_nfc_NativeNfcSecureElement(JNIEnv *e)
{
    return jniRegisterNativeMethods(e, gNativeNfcSecureElementClassName,
            gMethods, NELEM(gMethods));
}


} // namespace android
