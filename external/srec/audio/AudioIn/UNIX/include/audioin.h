/*---------------------------------------------------------------------------*
 *  audioin.h  *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                               *
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the 'License');          *
 *  you may not use this file except in compliance with the License.         *
 *                                                                           *
 *  You may obtain a copy of the License at                                  *
 *      http://www.apache.org/licenses/LICENSE-2.0                           *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an 'AS IS' BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * 
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *---------------------------------------------------------------------------*/

#ifndef __AUDIOIN_H__
#define __AUDIOIN_H__
/* -------------------------------------------------------------------------+
 |                               ScanSoft Inc.                              |
 + -------------------------------------------------------------------------*/



/* -------------------------------------------------------------------------+
 | Project       : ScanSoft AudioIn component
 | Module        : AUDIOIN
 | File name     : audioin.h
 | Description   : Interface definition for AUDIOIN
 | Reference(s)  : wavein, audioin.chm, audioin.doc, audioin.hlp
 |                 SltGl00001_audioin_gl1.doc
 | Status        : Version 1.2
 + -------------------------------------------------------------------------*/
/*     Oct/8/2002: Fixes for Windows 2000, and memory leaks. Version 1.1    */
/*  PVP   Jan/8/2004: Default audio format changed to 16kHz. Version 2.0       */
/*--------------------------------------------------------------------------*/

/* @doc AUDIOININTERFACE */

 
#include "audioinerr.h"

#if defined( __cplusplus )
extern "C"
{
#endif
  
/* -------------------------------------------------------------------------+
 |   MACROS                                                                 |
 + -------------------------------------------------------------------------*/
  
/* none */
  
/* -------------------------------------------------------------------------+
 |   TYPE DEFINITIONS                                                       |
 + -------------------------------------------------------------------------*/
  
typedef short audioinSample;

  
/* Type Definitions for SCANSOFT-TYPES (re-definition)*/

/* @type AUDIOIN_H | Handle to an audio-in component.
 * @comm Type is declared as a void *. The actual implementation is
 *       done by the implementation engineer. */
typedef void *  AUDIOIN_H;
#define WAVE_MAPPER 0
/* @enum AUDIOIN_STATUSINFO | Enumerator for the Status Information of the AudioIn component. 
 * @comm The information contained in this definition concerns not only to the status of 
 *       the FIFO but also the general status of the audio component. 
 *
 * @xref <f lh_audioinGetSamples>(), AudioIn Diagram State */
typedef enum _AUDIOIN_STATUSINFO {
  AUDIOIN_NORMAL,       /* @emem Normal state of the audio buffer. No problems detected while retrieving 
                                 samples.*/
  AUDIOIN_TIMEOUT,      /* @emem The audio-in component timed out after no audio was received from the 
                                 audio device. The MMSystem is not providing any more samples, or the
                                 lh_audioinGetSamples function may be called much faster than the actual
                                 thread filling the buffer (Probable issue with the audio device). This could
                                 be fixed by decreasing the number of samples that you want to retrieve or by
                                 waiting till samples are available. A time out period is set internally on
                                 the audioin implementation. Default of:
                                 <nlpar>const DWORD GETSAMPLESTIMEOUT_PERIOD = 10000; (time in milliseconds)*/
  AUDIOIN_HIGHWATERMARK,/* @emem The buffer has been filled out with 75% or more. A high watermark on 
                                 the audio buffer has been detected and the buffer could be close to an OVERRUN 
                                 state.*/ 
  AUDIOIN_FIFOOVERRUN,  /* @emem The buffer has been overfilled with audio samples. You can still retrieve
                                 samples from the FIFO with the lhs_audioinGetSamples function, but the audio-in 
                                 component will not buffer any new audio into the FIFO. AudioinStop must be
                                 called to reset the audio-in component.*/
  AUDIOIN_FLUSHED,      /* @emem The buffer has been overfilled with audio samples. You can no longer retrieve
                                 samples from the FIFO, since you already emptied it with lhs_audioinGetSamples.
                                 The audio-in component will not buffer any new audio into the FIFO.
                                 lhs_audioinStop must be called in the audio-in component since
                                 lhs_audioinGetSamples will not longer work.*/
  AUDIOIN_HWOVERRUN,    /* @emem The buffer has been overfilled with audio samples inside the device component
                                 (audio device). lhs_audioinStop should be called to reset the contents of the
                                 FIFO and the codec. This state is caused by an error in the MMSystem. It is 
                                 recommended to initialize the audio-in component before retrieving samples
                                 again.*/  
} AUDIOIN_STATUSINFO;
  
  
/* @struct AUDIOIN_INFO | Structure for the AudioIn Information 
 * @comm The AUDIOIN_INFO contains information about several parts of the audio-in component.
 *       It gives information about the FIFO buffering audio and at the same time about the audio 
 *       component. 
 *
 * @xref <f lh_audioinGetSamples>(), AudioIn Diagram State */
typedef struct _AUDIOIN_INFO{
  AUDIOIN_STATUSINFO eStatusInfo;    /* @field The state in which the audio Buffer of the audio-in component
                                               is. This is detailed in AUDIOIN_STATUSINFO. */
  unsigned long u32SamplesAvailable; /* @field The number of Samples still available in the audio Buffer after
                                               lhs_audioinGetSamples is called. This value can help you to
                                               detect over-runs in the audio buffer.*/
} AUDIOIN_INFO;

#ifdef AUDIOIN_SUPPORT_CALLBACK
typedef enum _AUDIOIN_MSG {
  AUDIOIN_MSG_OPEN,           // audio device was opened
  AUDIOIN_MSG_START,          // start audio acquisition
  AUDIOIN_MSG_DATA,           // audio samples are available
  AUDIOIN_MSG_STOP,           // stop audio acquisition
  AUDIOIN_MSG_CLOSE,          // audio device was closed
  AUDIOIN_MSG_INVALID,        // bogus
} AUDIOIN_MSG;
  
/* callback function for "samples ready" notification */
typedef void (*pCallbackFunc)(AUDIOIN_H hAudioIn, AUDIOIN_MSG uMsg, void* dwInstance, void* dwParam1, void* dwParam2);

/* data structure passed to callback function; loosely based on Windows' WAVEHDR */
typedef struct { 
    void               *pData; 
    unsigned long       nBufferLength; 
    unsigned long       nBytesRecorded; 
    AUDIOIN_STATUSINFO  status;
} AUDIOIN_WAVEHDR; 
#endif

/* -------------------------------------------------------------------------+
|   EXTERNAL DATA (+ meaning)                                              |
+ -------------------------------------------------------------------------*/

/* none */

/* -------------------------------------------------------------------------+
|   GLOBAL FUNCTION PROTOTYPES                                             |
+ -------------------------------------------------------------------------*/
#if 0
 LHS_AUDIOIN_ERROR  lhs_audioinOpenEx (
  unsigned long u32AudioInID,         /*@parm [in]  Audio-in device ID (ranges from 0 to a number of available
                                                    devices on the system). You can also use the following flag
                                                    instead of a device identifier.
                                                    <nl><nl><bold WAVE_MAPPER> = The function selects a
                                                    waveform-audio input device capable of recording in the
                                                    specified format. <bold Header:> Declared in Mmsystem.h from
                                                    the Windows Multimedia: Platform SDK.*/
  unsigned long u32Frequency,         /*@parm [in]  Frequency of the recognition engine in Hz. */
  unsigned long u32NbrOfFrames,       /*@parm [in]  (not used) Number of frames buffered internally */
  unsigned long u32SamplesPerFrame,   /*@parm [in]  Size, in samples, of each individual frame. */
  AUDIOIN_H * phAudioIn               /*@parm [out] Handle to the audio-in device */
 );
#endif

LHS_AUDIOIN_ERROR  lhs_audioinOpen(
  unsigned long u32AudioInID, /* [in] audio-in device ID (ranges from 0 to a number of available devices on the
                                      system). You can also use the following flag instead of a device identifier.
                                      <nl><nl><bold WAVE_MAPPER> = The function selects a waveform-audio input
                                      device capable of recording in the specified format. <bold Header:>
                                      Declared in Mmsystem.h from the Windows Multimedia: Platform SDK.*/      
  unsigned long u32Frequency, /* [in] Frequency of the recognition engine in Hz. */
  AUDIOIN_H * phAudioIn       /* [out] Handle to the audio-in device */
);

#ifdef AUDIOIN_SUPPORT_CALLBACK
LHS_AUDIOIN_ERROR  lhs_audioinOpenCallback(
  unsigned long u32AudioInID,      /* [in] audio-in device ID (ranges from 0 to a number of available devices on the
                                      system). You can also use the following flag instead of a device identifier.
                                      <nl><nl><bold WAVE_MAPPER> = The function selects a waveform-audio input
                                      device capable of recording in the specified format. <bold Header:>
                                      Declared in Mmsystem.h from the Windows Multimedia: Platform SDK.*/      
  unsigned long u32Frequency,      /* [in] Frequency of the recognition engine in Hz. */
  unsigned long u32NbrOfSamples,   /*[in] <nl><bold Input:> Number of samples requested per callback */
  
  pCallbackFunc pCallback,         /* [in] callback function */
  void         *pCallbackInstance, /* [in] callback instance */
  AUDIOIN_H * phAudioIn            /* [out] Handle to the audio-in device */
);
#endif

LHS_AUDIOIN_ERROR lhs_audioinClose(
  AUDIOIN_H * phAudioIn /*[in] Pointer to the handle of the audio-in device to be closed.*/
);

LHS_AUDIOIN_ERROR lhs_audioinStart( 
  AUDIOIN_H hAudioIn    /*[in] Handle of the audio-in device */
); 

LHS_AUDIOIN_ERROR lhs_audioinStop(
  AUDIOIN_H hAudioIn    /*[in] Handle of the audio-in device*/
);
  
LHS_AUDIOIN_ERROR lhs_audioinGetSamples (
  AUDIOIN_H hAudioIn,              /*[in] Handle of the audio-in device */
  unsigned long * u32NbrOfSamples, /*[in/out] <nl><bold Input:> The requested number of samples to be filled in 
                                              the pAudioBuffer. Note that the memory used for pBuffer should be large enough 
                                              to contain the requested number of samples, also note how the u32NbrOfSamples 
                                              is different than the Buffer size, since each frame has an specific size depending 
                                              on the audio format.
                                              <nl><bold Output:> The actual number of recorded audio samples written in pBuffer.
                                              If you pass 0 to this parameter then you may still retrieve the AUDIOIN_STATUSINFO
                                              on the audio component. */
  void * pAudioBuffer,              /*[out] Buffer that contains the recorded samples. The memory used for this 
                                            buffer is allocated by the client. So, it is the responsibility of the client to 
                                            make sure that this memory can contain the requested number of samples. The memory 
                                            for this buffer needs also to be freed by the client. */
  AUDIOIN_INFO * pAudioInInfo       /*[out] Information about the audio internal buffer described in 
                                            AUDIOIN_INFO.*/
);
  
  
LHS_AUDIOIN_ERROR lhs_audioinGetVersion(
  unsigned long * pu32Version       /*[out] The version number of the API implementation. */
);
  
  
LHS_AUDIOIN_ERROR lhs_audioinGetVolume(
  AUDIOIN_H hAudioIn,             /*[in] Handle of the audio-in device.*/
  unsigned long * pu32Volume      /*[out] Pointer to a variable that will be filled with the current volume 
                                          (normally range of  0 - 65535). Depending on the platform the volume 
                                          value may be set to in sizes of 16, 32 or other size, the range also
                                          depends on the platform (Implementations of this interface may
                                          change because of the compatibility of the internal function).*/
);
  
LHS_AUDIOIN_ERROR lhs_audioinSetVolume(
  AUDIOIN_H hAudioIn,         /*[in] Handle of the audio-in device.*/
  unsigned long u32Volume     /*[in] The volume to be set (normal range 0-65535). Depending on the 
                                     platform the volume value may be set to in sizes of 16, 32 or other size, 
                                     the range also depends on the platform. (Implementations of this interface 
                                     may change because of the compatibility of the internal function)*/
);
  

const TCHAR * lhs_audioinErrorGetString(
  const LHS_AUDIOIN_ERROR Error    /*[in] The Error code.*/
);
  
  
/* -------------------------------------------------------------------------+
 |   END                                                                    |
 + -------------------------------------------------------------------------*/
  
#if defined( __cplusplus )
}
#endif


#endif /* #ifndef __AUDIOIN_H__ */
