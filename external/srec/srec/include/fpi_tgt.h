/*---------------------------------------------------------------------------*
 *  fpi_tgt.h  *
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

/*
 *
 *
 */

#ifndef __fpi_tgt_h
#define __fpi_tgt_h

#ifdef SET_RCSID
static const char fpi_tgt_h[] = "$Id: fpi_tgt.h,v 1.1.10.3 2007/08/31 17:44:53 dahan Exp $";
#endif




#include <stdlib.h>
#include <string.h>

#include "creccons.h"   /* CREC Public Constants    */

#include "hmm_type.h"
#include "specnorm.h"
#include "voicing.h"

#define QUICK 0

/***********************************
 * MACROs for Pointer Manipulation *
 ***********************************/

#if QUICK
#define NEXT_FRAME_POINTER(PKT,X)   (((X) + (PKT)->frameSize) & (PKT)->stackMask)
#define PREV_FRAME_POINTER(PKT,X)   (((X) - (PKT)->frameSize) & (PKT)->stackMask)
#define FIX_FRAME_POINTER(PKT,X)    ((X) & (PKT)->stackMask)
#define POINTER_GAP(PKT, LEAD, LAG) ((((LEAD) - (LAG)) & (PKT)->stackMask)/(PKT)->frameSize)
#define CHECK_BOUND(PKT, X)         ((X) < (PKT)->frameStack ? NULL : (X) > (PKT)->lastFrameInStack ? NULL : (X))
#else
#define NEXT_FRAME_POINTER(PKT,X)   ((X) >= (PKT)->lastFrameInStack ? (PKT)->frameStack : (X) + (PKT)->frameSize)
#define PREV_FRAME_POINTER(PKT,X)   ((X) <= (PKT)->frameStack ? (PKT)->lastFrameInStack : (X) - (PKT)->frameSize)
#define FIX_FRAME_POINTER(PKT,X)    ((X) < (PKT)->frameStack ? (X) + (PKT)->featuresInStack \
                                     : (X) > (PKT)->lastFrameInStack ? (X) - (PKT)->featuresInStack : (X))
#define POINTER_GAP(PKT, LEAD, LAG) ((LEAD) >= (LAG) ? ((LEAD) - (LAG))/(PKT)->frameSize : ((PKT)->featuresInStack + (LEAD) - (LAG))/(PKT)->frameSize)
#define CHECK_BOUND(PKT, X)         ((X) < (PKT)->frameStack ? NULL : (X) > (PKT)->lastFrameInStack ? NULL : (X))
#endif

#ifdef DEBUGSEM
volatile int releaseCalled = 0;
volatile int waitCalled = 0;
#endif

/************************************************************************
 * Structures                                                           *
 ************************************************************************/

/****************
 * Frame Buffer *
 ****************/

typedef struct
{
  volatile int  isCollecting;      /* Frame buffer is collecting */
  featdata*     frameStack;        /* Pointer to Frame Stack          */
  int           frameSize;         /* How many data items per frame?  */
  int           uttDim;            /* How many spectral parameters?   */
  int           frameStackSize;    /* How many frames in the Stack?   */
  unsigned long stackMask;      /* Mask to fit stack size  */
  int           featuresInStack;   /* How many features in the Stack? */
  featdata*     lastFrameInStack;  /* Pointer to last frame           */
  int           haveVoiced;      /* whether voice stack is valid */
  volatile int  voicingDetected;   /* Voicing present in this buffer  */
  volatile int  utt_ended;         /* end of utterance flag           */
  volatile int  quietFrames;      /* consecutive quiet frames to end */
  volatile int  uttTimeout;      /* Voicing present in this buffer  */
  int           holdOffPeriod;     /* Copy of 'holdOff' argument      */
  int           holdOff;           /* countdown of 'holdOffPeriod     */
  
  featdata* volatile pushp;        /* Ptr to frame being written      */
  featdata* volatile pullp;        /* Ptr to next frame to be read    */
  featdata* volatile pushBlkp;     /* pushp blocker                   */
  
  int    blockLen;      /* Blocking enabled to this length */
  volatile int  pushBlocked;       /* Set if pushp ever gets blocked  */
  unsigned long blockTime;         /* Last time frame was blocked     */
  unsigned long pushTime;          /* Time of FEP frame               */
  unsigned long pullTime;          /* Time of REC frame               */
  unsigned long startTime;         /* Time of first frame of this utt */
  unsigned long stopTime;          /* Time of first frame of this utt */
  
  volatile featdata  maxC0;      /* Maximum C0 tracked  */
  volatile featdata  minC0;      /* Minimum C0 tracked  */
}
fepFramePkt;

/************************************************************************
 * Prototypes                                                           *
 ************************************************************************/

/*************************
 * Both FEP and REC units
 *************************/
void      startFrameCollection(fepFramePkt* frmPkt);
int          stopFrameCollection(fepFramePkt* frmPkt);

/*************************
 * For FEP unit Only (?) *
 *************************/

fepFramePkt* createFrameBuffer(int fCnt, int dimen, int blockLen, int doVoice);
int          resizeFrameBuffer(fepFramePkt *frmPkt, int fCnt);
int          clearFrameBuffer(fepFramePkt* frmPkt);
int          destroyFrameBuffer(fepFramePkt* frmPkt);
int          pushSingleFEPframe(fepFramePkt* frmPkt, featdata* parPtr, int voiceData);
void      clearEndOfUtterance(fepFramePkt* frmPkt);
void      setupEndOfUtterance(fepFramePkt* frmPkt, long timeout, long holdOff);

/*************************
 * For REC unit Only (?) *
 *************************/

int         setRECframePtr(fepFramePkt* frmPkt, int fCnt, int mode);
featdata*   currentRECframePtr(fepFramePkt* frmPkt);
featdata*   currentFEPframePtr(fepFramePkt* frmPkt);
int         incRECframePtr(fepFramePkt* frmPkt);
int         decRECframePtr(fepFramePkt* frmPkt);
int         framesInBuffer(fepFramePkt* frmPkt);
featdata    getVoicingCode(fepFramePkt *frmPkt, featdata *frmptr);
void     setVoicingCode(fepFramePkt *frmPkt, featdata *frmptr, featdata vcode);
unsigned long getRECframeTime(fepFramePkt* frmPkt);
unsigned long getFEPframeTime(fepFramePkt* frmPkt);
featdata    getCurrentC0(fepFramePkt* frmPkt);
featdata    getMaximumC0(fepFramePkt* frmPkt);
featdata    getMinimumC0(fepFramePkt* frmPkt);
featdata    get_c0_peak_over_range(fepFramePkt *frmPkt, int start, int end);

void        clearC0Entries(fepFramePkt* frmPkt);
void     releaseBlockedFramesInBuffer(fepFramePkt* frmPkt);

void    get_channel_statistics(fepFramePkt *frmPkt, int start, int end,
                               spect_dist_info** spec, int num, int relative_to_pullp);
int     get_background_statistics(fepFramePkt *frmPkt, int start, int end,
                                  spect_dist_info** spec, int num, int relative_to_pullp);
int     rec_frame_voicing_status(fepFramePkt *frmPkt);
void    utterance_detection_fixup(fepFramePkt *frmPkt, featdata **last_pushp,
                                  int voice_duration, int quite_duration,
                                  int unsure_duration);
                                  
#endif
