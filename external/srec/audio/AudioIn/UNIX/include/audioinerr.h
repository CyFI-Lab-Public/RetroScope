/*---------------------------------------------------------------------------*
 *  audioinerr.h  *
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

#ifndef _AUDIOINERRAPI_H
#define _AUDIOINERRAPI_H

/* -------------------------------------------------------------------------+
 |                               ScanSoft Inc.                              |
 + -------------------------------------------------------------------------*/


 
/* -------------------------------------------------------------------------+
 | Project       : ScanSoft AudioIn component
 | Module        : AUDIOINERROR
 | File name     : audioinerr.h
 | Description   : Definition file for the error handling of the AUDIOIN
 | Reference(s)  : wavein, audioin.chm, audioin.doc, audioin.hlp
 |                 SltGl00001_audioin_gl1.doc
 | Status        : Version 1.0
 + -------------------------------------------------------------------------*/

/* @doc AUDIOININTERFACE */




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
  
#define TCHAR char
  
/* @type LHS_AUDIOIN_ERROR | LONG value for Error Code. (i386 win32 wince specific)
 * @comm Type is declared as a LONG.
 * @xref LHS_U32, LHS_U16, LHS_BOOL */
typedef long  LHS_AUDIOIN_ERROR;
  
  /* ERROR CODES */
  
  /* general errors */
#define LHS_AUDIOIN_OK                     0
#define LHS_E_AUDIOIN_BASE                 1000
  /* AUDIOIN general errors */
#define LHS_E_AUDIOIN_NOTIMPLEMENTED       (LHS_E_AUDIOIN_BASE+0)  
#define LHS_E_AUDIOIN_NULLPOINTER          (LHS_E_AUDIOIN_BASE+1)
#define LHS_E_AUDIOIN_OUTOFMEMORY          (LHS_E_AUDIOIN_BASE+2)
#define LHS_E_AUDIOIN_INVALIDARG           (LHS_E_AUDIOIN_BASE+3)
  /* AUDIOIN specific errors */
#define LHS_E_AUDIOIN_INVALIDDEVICEID      (LHS_E_AUDIOIN_BASE+4)
#define LHS_E_AUDIOIN_NOAUDIODRIVER        (LHS_E_AUDIOIN_BASE+5)
#define LHS_E_AUDIOIN_COULDNOTOPENDEVICE   (LHS_E_AUDIOIN_BASE+6)
#define LHS_E_AUDIOIN_BADFORMAT            (LHS_E_AUDIOIN_BASE+7)
#define LHS_E_AUDIOIN_WRONGSTATE           (LHS_E_AUDIOIN_BASE+8)
#define LHS_E_AUDIOIN_OVERRUN              (LHS_E_AUDIOIN_BASE+9)
#define LHS_E_AUDIOIN_NOSAMPLES            (LHS_E_AUDIOIN_BASE+10)
#define LHS_E_AUDIOIN_GETSETVOLUME         (LHS_E_AUDIOIN_BASE+11)
#define LHS_E_AUDIOIN_AUDIOINOPENTIMEDOUT  (LHS_E_AUDIOIN_BASE+12)
#define LHS_E_AUDIOIN_AUDIOINBUSY          (LHS_E_AUDIOIN_BASE+13)
#define LHS_E_AUDIOIN_CREATEEVENTERROR     (LHS_E_AUDIOIN_BASE+14)
#define LHS_E_AUDIOIN_CANNOTRESETAUDIODEV  (LHS_E_AUDIOIN_BASE+15)
#define LHS_E_AUDIOIN_CANNOTCLOSEAUDIODEV  (LHS_E_AUDIOIN_BASE+16)
#define LHS_E_AUDIOIN_CANNOTSTARTAUDIODEV  (LHS_E_AUDIOIN_BASE+17)
#define LHS_E_AUDIOIN_CANNOTSTOPAUDIODEV   (LHS_E_AUDIOIN_BASE+18)
  
// *******************  Error Type Definitions *********************
typedef struct _AUDIOIN_ERRORINFO
{
  long  u32ErrorCode;

  const TCHAR *szExplanation;

  
}AUDIOIN_ERRORINFO;
  
  
/* -------------------------------------------------------------------------+
|   EXTERNAL DATA (+ meaning)                                              |
+ -------------------------------------------------------------------------*/
  
  /* none */
  
/* -------------------------------------------------------------------------+
 |   GLOBAL FUNCTION PROTOTYPES                                             |
 + -------------------------------------------------------------------------*/
  
#if ( !defined(_MSC_VER) && !defined(WINAPI) )
#define WINAPI
#endif
  
/* -------------------------------------------------------------------------+
 |   END                                                                    |
 + -------------------------------------------------------------------------*/
  
  
#if defined( __cplusplus )
}
#endif

#endif /* _AUDIOINERRAPI_H*/

