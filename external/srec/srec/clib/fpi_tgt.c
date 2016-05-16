/*---------------------------------------------------------------------------*
 *  fpi_tgt.c  *
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





#ifndef _RTT
#include <stdio.h>
#endif
#include <assert.h>

#include "all_defs.h"
#include "fpi_tgt.h"
#include "voicing.h"
#include "portable.h"

#include "fpi_tgt.inl"

#define DEBUG_REWIND 0

/************************************************************************
 * Create a Frame Buffer                                                *
 ************************************************************************
 *
 * On the Real Time Target (_RTT) the caller of this function is
 * responsible for publically declaring the location of the Frame Buffer
 * so that the REC unit can locate it.  This is achived by use of the
 * 'setPublicLocation()' and 'publicLocation()' functions.
 *
 ************************************************************************
 *
 * Arguments: "fCnt"       Size of Frame Stack
 *            "dimen"      Size of Frame
 *            "blockLen"   Blocking length (if Using)
 *            "doVoice"    Reserve voicing parameter
 *
 * Returns:   fepFramePkt* Pointer to frame buffer
 *                          NULL on error
 *
 ************************************************************************/

static int  incThisFramePtr(fepFramePkt* frmPkt, featdata** parPtr);
static int  decThisFramePtr(fepFramePkt* frmPkt, featdata** parPtr);



fepFramePkt* createFrameBuffer(int fCnt, int dimen, int blockLen, int doVoice)
{
  fepFramePkt* frmPkt;
#if QUICK
  unsigned long frame_mask = 1, tmpsiz;
#endif

  ASSERT(fCnt > 0);
  ASSERT(dimen > 0);

  /* Allocate space for the Frame Packet  *
   * and then accommodate the Frame Stack */

  frmPkt = (fepFramePkt*) CALLOC_CLR(1, sizeof(fepFramePkt), "clib.Frame_Buffer");
  if (frmPkt == NULL)
    return NULL;

#if QUICK
  tmpsiz = blockLen;
  frame_mask = 1;
  tmpsiz >>= 1;
  while (tmpsiz)
  {
    frame_mask = (frame_mask << 1) | 0x01;
    tmpsiz >>= 1;
  }
  blockLen = frame_mask + 1;
  frmPkt->stackMask = frame_mask;
#endif

  frmPkt->uttDim = dimen;
  if (doVoice) dimen++;

  frmPkt->frameStackSize  = fCnt;
  frmPkt->frameSize       = dimen;
  frmPkt->featuresInStack = fCnt * dimen;
  frmPkt->blockLen = blockLen;
  if (doVoice) frmPkt->haveVoiced = True;
  else frmPkt->haveVoiced = False;

  frmPkt->frameStack = (featdata *) CALLOC(fCnt,
                       sizeof(featdata) * dimen, "clib.Frame_Stack");
  if (frmPkt == NULL)
    return NULL;
  frmPkt->lastFrameInStack = frmPkt->frameStack + (fCnt - 1) * dimen;

  /* Use standard function to clear the buffer,    *
   * we don't care about the return code because   *
   * we built it, others should care...            */

  (void) clearFrameBuffer(frmPkt);

  frmPkt->uttTimeout = 20;             /* default setting */

  return frmPkt;
}


/************************************************************************
 * Clear an existing Frame Buffer                                       *
 ************************************************************************
 *
 * Given a pointer to a previously created frame buffer structure
 * this funtion will reset its member components to their initial
 * values.
 *
 ************************************************************************
 *
 * Arguments: "frmPkt"     Frame Buffer Structure Pointer
 *
 * Returns:   int          non-ZERO on Error
 *
 ************************************************************************/

int clearFrameBuffer(fepFramePkt* frmPkt)
{
  ASSERT(frmPkt != NULL);

  /* Clear the frame stack to ZERO as  *
   * this is done by model_allocate()  */

  memset(frmPkt->frameStack, 0,  /*  TODO: do we need this? */
         sizeof(featdata) * frmPkt->frameSize * frmPkt->frameStackSize);

  /* Reset Structure Members           */

  frmPkt->isCollecting = FB_IDLE;
  frmPkt->pullp       = frmPkt->frameStack;

  frmPkt->pushp       = frmPkt->frameStack;
  frmPkt->pushBlkp    = frmPkt->frameStack;
  frmPkt->pushBlocked = False;
  frmPkt->blockTime   = 0;
  frmPkt->pushTime    = 1;    /* 0 == invalid frame ID, 1 == first    */
  frmPkt->pullTime    = 1;
  frmPkt->startTime   = 0;    /* 0 == start hasn't been called        */
  frmPkt->stopTime    = 0;    /* 0 == stop  hasn't been called        */

  clearEndOfUtterance(frmPkt);
  clearC0Entries(frmPkt);

  return False;
}

/************************************************************************
 * Destroy a Previously Created Frame Buffer                            *
 ************************************************************************
 *
 * On the Real Time Target (_RTT) the caller of this function is
 * responsible for publically declaring the location of the Frame Buffer
 * so that the REC unit can locate it.  This is achived by use of the
 * 'setPublicLocation()' and 'publicLocation()' functions.
 *
 ************************************************************************
 *
 * Arguments: fepFramePkt* Pointer to frame buffer to destroy
 *
 * Returns:   int          non-ZERO on error
 *
 ************************************************************************/

int destroyFrameBuffer(fepFramePkt* frmPkt)
{
  ASSERT(frmPkt);
  /* De-allocate space for the Frame Stack *
   * and then the Frame Packet             */

  FREE(frmPkt->frameStack);
  FREE(frmPkt);
  return False;
}

/************************************************************************
 * To Start Collecting Frames                                           *
 ***********************************************************************/

void startFrameCollection(fepFramePkt* frmPkt)
{
  ASSERT(frmPkt);
  if (frmPkt->isCollecting == FB_IDLE)
  {
    clearEndOfUtterance(frmPkt);
    clearC0Entries(frmPkt);

    frmPkt->startTime = frmPkt->pushTime;
    frmPkt->stopTime = 0;
    frmPkt->isCollecting = FB_ACTIVE;
  }
  return;
}

/************************************************************************
 * To Stop Collecting Frames                                            *
 ***********************************************************************/

int stopFrameCollection(fepFramePkt* frmPkt)
{
  ASSERT(frmPkt);
  ASSERT(frmPkt->startTime != 0);

  if (frmPkt->isCollecting == FB_ACTIVE)
  {

    /* Remember, pushTime is the ID of the next frame to arrive
     * The buffer starts empty, with pushTime == 1.
     * So if Stop occurs at this point, then the start and end frames
     * will be
     */

    frmPkt->stopTime = frmPkt->pushTime;
    frmPkt->isCollecting = FB_IDLE;

    return (True);
  }

  return (False);
}

/************************************************************************
 ***********************************************************************/

void setupEndOfUtterance(fepFramePkt* frmPkt, long timeout, long holdOff)
{
  ASSERT(frmPkt);
  ASSERT(timeout >= 0);
  ASSERT(holdOff >= 0);
  frmPkt->uttTimeout = timeout;
  frmPkt->holdOffPeriod = holdOff;
  frmPkt->holdOff = 0;
  return;
}

/************************************************************************
 ***********************************************************************/

void clearEndOfUtterance(fepFramePkt* frmPkt)
{
  ASSERT(frmPkt);
  ASSERT(frmPkt->holdOffPeriod >= 0);
  frmPkt->voicingDetected = 0;
  frmPkt->quietFrames = 0;
  frmPkt->utt_ended = False;
  frmPkt->holdOff = frmPkt->holdOffPeriod;

  return;
}

void releaseBlockedFramesInBuffer(fepFramePkt* frmPkt)
{
  frmPkt->pullp = frmPkt->pushp;     /*  Move the Blocker to pullp */
  frmPkt->pushBlkp = frmPkt->pushp;     /*  Move the Blocker to pullp */
  frmPkt->pullTime = frmPkt->pushTime;
  frmPkt->blockTime = frmPkt->pushTime;

  return;
}

/************************************************************************
 * Push a Single Frame into Frame Buffer                                *
 ************************************************************************
 *
 * Inserts a new frame into the frame buffer.
 *
 * If there is no room in the buffer (the frame maker has exhausted the
 * space which is being slowly 'eaten' by the associated recognizer) then
 * the data is not inserted and an error value is returned.  For this to
 * happen, blockLen member must be set.  Otherwise pushBlkp will always
 * point to the oldest valid frame in the buffer.
 *
 ************************************************************************
 *
 * Arguments: "frmPkt"  Frame Buffer Pointer
 *            "parPtr"  Pointer to contiguous block of Frame Parameters
 *
 * Returns:   int       non-ZERO on ERROR
 *
 ************************************************************************/

int pushSingleFEPframe(fepFramePkt* frmPkt, featdata* parPtr, int voiceData)
{
  featdata*   destFrmPtr;
  featdata*   nextFrmPtr;

  ASSERT(frmPkt);
  ASSERT(parPtr);

  /* 'pushp' must be either within the frame buffer or NULL. *
   * If it is NULL then the frame is just discarded.         */

  if (frmPkt->isCollecting != FB_ACTIVE) return True;
  if ((destFrmPtr = nextFrmPtr = (featdata*) frmPkt->pushp) == NULL)
    return (0);

#if DEBUG_REWIND
  log_report("U: voicing at %d was %x\n", frmPkt->pushTime, voiceData);
#endif

  /* Copy the frame into the buffer.  Once this is done     *
   * advance 'pushp' (unless it is up against the 'blocker' *
   * The frame consists of Parameters and Signal Data       */

  memcpy(destFrmPtr, parPtr, frmPkt->uttDim * sizeof(featdata));
  if (frmPkt->haveVoiced)
    destFrmPtr[frmPkt->uttDim] = voiceData;

  /* The following (vocing detection which triggers EOU),
   * is only active when the 'holdOff' member is 0.
   * The intension is to delay 'voicing' signal for at least
  * 'holdOffPeriod' frames.
   */
  if (frmPkt->holdOff <= 0)
  {
    if (frmPkt->haveVoiced && frmPkt->utt_ended == False)
    {
      if (voiceData & VOICE_BIT)
      {
        frmPkt->voicingDetected = 1;
      }
      if (voiceData & QUIET_BIT)
      {
        frmPkt->quietFrames++;
        if (frmPkt->voicingDetected
            && frmPkt->quietFrames > frmPkt->uttTimeout)
        {
          log_report("Level based utterance ended at %d\n",
                     frmPkt->pushTime);
          frmPkt->utt_ended = True;
        }
      }
      else
        frmPkt->quietFrames = 0;
    }
  }
  else
  {
    ASSERT(frmPkt->holdOff > 0);
    frmPkt->holdOff--;
  }

  /*  Track C0 values
  */
  if (frmPkt->maxC0 < parPtr[0])      /* only works if the 0th entry - */
    frmPkt->maxC0 = parPtr[0];       /* is C0 */

  if (frmPkt->minC0 > parPtr[0])      /* only works if the 0th entry - */
    frmPkt->minC0 = parPtr[0];       /* is C0 */

  frmPkt->pushTime++;
  if (frmPkt->pushTime == 0L)          /* Check for wrap - and ensure */
    frmPkt->pushTime++;              /* ZERO is NEVER used          */

  /* Try to move the push pointer on, if it meets the *
   * push blocker it should not increment.            */

  nextFrmPtr = (featdata *) NEXT_FRAME_POINTER(frmPkt, frmPkt->pushp);

  if (nextFrmPtr == frmPkt->pullp)
  {
    /* Latest Frame was blocked, so record the fact and then *
    * record the frame time that this occured (useful?)     */

    frmPkt->pushBlocked++;
    frmPkt->blockTime = frmPkt->pushTime;

    return True;
  }

  else if (nextFrmPtr == frmPkt->pushBlkp)
  {
    if (frmPkt->blockLen == 0)
    {
      /* Simply move pushBlkp along */

      frmPkt->pushBlkp = NEXT_FRAME_POINTER(frmPkt, frmPkt->pushBlkp);
    }
    else
    {
      /* Latest Frame was blocked, so record the fact and then *
       * record the frame time that this occured (useful?)     */

      frmPkt->pushBlocked++;
      frmPkt->blockTime = frmPkt->pushTime;

      return True;
    }
  }

  /* Free to move ahead, so increment the push pointer     *
   * and increase the frame-count between pull & push      */

  frmPkt->pushp = nextFrmPtr;
  /*      Increment semaphore count for each frame pushed.
   Decrement is in waitforsinglefepframe */
  return False;
}

/************************************************************************
 * Sets oldest frame pointer (Use with caution)                         *
 ************************************************************************
 *
 * NOTES
 *
 * If 'masterREC', 'pullp' is manipulated, otherwise one of the
 * multiple recognition pointers, 'auxPullp[]' is used.
 *
 ************************************************************************
 *
 * CAUTION
 *
 * With multiple recognizers, the gap-test doesn't work !!!
 *
 ************************************************************************
 *
 * Arguments: "frmPkt"     Pointer to Frame Packet
 *            "fCnt"       Frame offset from Newest Frame
 *                              +ve == Increase Distance between oldest & newest
 *                              -ve == Decrease Distance
 *            "mode"           ZERO means movement wrt Newest Frame
 *                         non-ZERO means movement wrt Current Oldest Frame
 *
 * Retunrs:   int          Status of operation
 *                          No Problems: False
 *                          No FEP     : DUKRC_NOFEP
 *
 * Critical section code!
 *
 ************************************************************************/

int setRECframePtr(fepFramePkt* frmPkt, int fCnt, int mode)
{
  int   gap;

  ASSERT(frmPkt);

  if (mode != 0) /* wrt Current Oldest Frame */
  {
    /************
    * Relative *
    ************/

    /* Can it go backwards? */

    gap = POINTER_GAP(frmPkt, frmPkt->pullp, frmPkt->pushBlkp);
    if (fCnt > gap)                         /* Limit movement      */
      fCnt = gap;

    /* Can it go forwards? */

    gap = POINTER_GAP(frmPkt, frmPkt->pushp, frmPkt->pullp);
    if (fCnt < -gap)                         /* Limit movement      */
      fCnt = -gap;

    frmPkt->pullp = FIX_FRAME_POINTER(frmPkt,
                                      frmPkt->pullp - fCnt * frmPkt->frameSize);
    frmPkt->pullTime -= fCnt;

  }
  else  /* wrt Newest Frame */
  {
    /************
    * Absolute *
    ************/

    /* ASSERT(fCnt); moved from the above block, do we need this? */
    ASSERT(frmPkt->isCollecting != FB_DEAD);

    gap = POINTER_GAP(frmPkt, frmPkt->pushp, frmPkt->pushBlkp);

    if (fCnt > gap)                         /* Limit movement      */
      fCnt = gap;

    frmPkt->pullp = FIX_FRAME_POINTER(frmPkt,
                                      frmPkt->pushp - fCnt * frmPkt->frameSize);
    frmPkt->pullTime = frmPkt->pushTime - fCnt;

  }

  return (fCnt);              
  ;
}

/************************************************************************
 * Returns Pointer to Oldest unread REC frame                           *
 ************************************************************************
 *
 * Arguments: "frmPkt"  Frame Buffer Pointer
 *
 * Retunrs:   featdata*   Pointer to newest frame
 *                          NULL on Error
 *
 ************************************************************************/

featdata* currentRECframePtr(fepFramePkt* frmPkt)
{
  ASSERT(frmPkt);
  if (frmPkt->pushp == frmPkt->pushBlkp)            /* uninitialized? */
    return NULL;
  return ((featdata *)frmPkt->pullp);
}

/************************************************************************
 * Returns Pointer to Newest Complete frame of given channel            *
 ************************************************************************
 *
 * Arguments: "frmPkt"  Frame Buffer Pointer
 *
 * Retunrs:   featdata*   Pointer to newest frame
 *                          NULL on Error.
 *
 ************************************************************************/

featdata* currentFEPframePtr(fepFramePkt* frmPkt)
{
  featdata* frmPtr;

  ASSERT(frmPkt);
  frmPtr = frmPkt->pushp;  /* Where is FEP?    */
  if (frmPtr == NULL)
    return NULL;
  (void) decThisFramePtr(frmPkt, &frmPtr);/* Move backwards   */
  return frmPtr;
}

/************************************************************************
 * Moves REC's Frame Pointer backwards one Frame (if it can)            *
 ************************************************************************
 *
 * NOTES
 *
 * If 'masterREC', 'pullp' is manipulated, otherwise one of the
 * multiple recognition pointers, 'auxPullp[]' is used. (not sure about this)
 * The pushBlkp is also moved accordingly.
 *
 ************************************************************************
 *
 * Arguments: "n"      Channel Number of Selected Frame
 *
 * Retunrs:   int      Non-zero on error
 *
 ************************************************************************/

int incRECframePtr(fepFramePkt* frmPkt)
{
  ASSERT(frmPkt);

  /* Ensure that the frame buffer for *
   * the channel specified exists     */

  if (frmPkt->pullp == frmPkt->pushp)
    return True;


  frmPkt->pullp = NEXT_FRAME_POINTER(frmPkt, frmPkt->pullp);

  frmPkt->pullTime++;
  if (frmPkt->pullTime == 0)  /* Check for wrap and ensure */
    frmPkt->pullTime++;     /* that it is never ZERO     */

  /* New 'pushBlkp' */
  if (frmPkt->blockLen > 0 && frmPkt->isCollecting == FB_ACTIVE)
  {
    if (POINTER_GAP(frmPkt, frmPkt->pullp, frmPkt->pushBlkp) >= frmPkt->blockLen)
    {
      frmPkt->pushBlkp = NEXT_FRAME_POINTER(frmPkt, frmPkt->pushBlkp);
    }
  }

  return False;
}

/************************************************************************
 * Moves REC's Frame Pointer backwards one Frame (if it can)            *
 ************************************************************************
 *
 * NOTES
 *
 * If 'masterREC', 'pullp' is manipulated, otherwise one of the
 * multiple recognition pointers, 'auxPullp[]' is used. (not sure about this)
 * The pushBlkp is also moved accordingly.
 *
 ************************************************************************
 *
 * Arguments: "n"      Channel Number of Selected Frame
 *
 * Retunrs:   int      Non-zero on error
 *
 ************************************************************************/

int decRECframePtr(fepFramePkt* frmPkt)
{
  ASSERT(frmPkt);

  /* Ensure that the frame buffer for *
   * the channel specified exists     */

  /* New 'pullp' */

  if (frmPkt->pullp == frmPkt->pushBlkp) return True;
  frmPkt->pullp = PREV_FRAME_POINTER(frmPkt, frmPkt->pullp);
  frmPkt->pullTime--;
  return False;
}

/************************************************************************
 * Moves a Frame Pointer forwards one Frame (if it can)                 *
 ************************************************************************
 *
 * Arguments: "n"      Channel Number of Selected Frame
 *            "parPtr" Current Frame Pointer
 *
 * Retunrs:   int      Non-zero on error
 *                     "parPtr" and "sigPtr" may have changed
 *
 * Caution:            Does not test to see whether 'parPtr' lies
 *                     legally within the appropriate buffer or on an
 *                     appropriate valid frame boundary
 *
 *                     The caller should NEVER modify frame buffer
 *                     pointers by hand, always call an RTT-supplied function
 *
 ************************************************************************/

static int incThisFramePtr(fepFramePkt* frmPkt, featdata** parPtr)
{
  ASSERT(frmPkt);
  ASSERT(parPtr);
  if (*parPtr == frmPkt->pushp)
    return True;
  *parPtr = NEXT_FRAME_POINTER(frmPkt, *parPtr);
  return False;
}

/************************************************************************
 * Moves a Frame Pointer backwards one Frame (if it can)                *
 ************************************************************************
 *
 * Arguments: "frmPkt" Frame Buffer Pointer
 *            "parPtr" Current Frame Pointer
 *            "sigPtr" Signal Pointer
 *                          Set to NULL if not required
 *
 * Retunrs:   int      Non-zero on error
 *                     "parPtr" may have changed
 *
 * Caution:            Checks for bound within pushBlkp.
 *                     The caller should NEVER modify frame buffer
 *                     pointers by hand, always call an RTT-supplied function
 *
 ************************************************************************/

static int decThisFramePtr(fepFramePkt* frmPkt, featdata** parPtr)
{
  ASSERT(frmPkt);
  ASSERT(parPtr);
  if (*parPtr == frmPkt->pushBlkp)
    return True;
  *parPtr = PREV_FRAME_POINTER(frmPkt, *parPtr);
  return False;
}

/************************************************************************
 ************************************************************************/

featdata getVoicingCode(fepFramePkt* frmPkt, featdata *frmptr)
{
  ASSERT(frmPkt);
  frmptr = CHECK_BOUND(frmPkt, frmptr);
  if (frmptr && frmPkt->haveVoiced)
    return (frmptr[frmPkt->uttDim]);
  else
    return (0);
}

/************************************************************************
 ************************************************************************/

void setVoicingCode(fepFramePkt* frmPkt, featdata *frmptr, featdata vcode)
{
  ASSERT(frmPkt);
  frmptr = CHECK_BOUND(frmPkt, frmptr);
  if (frmptr && frmPkt->haveVoiced)
    frmptr[frmPkt->uttDim] =
      SET_VOICING_CODES(frmptr[frmPkt->uttDim], vcode);
  return;
}

/************************************************************************
 ************************************************************************/

void clearC0Entries(fepFramePkt* frmPkt)
{
  ASSERT(frmPkt);
  frmPkt->maxC0 = 0;           /*  Assuming a normal range of 0 - 255 */
  frmPkt->minC0 = 255;
  return;
}

int get_background_statistics(fepFramePkt *frmPkt, int start, int end,
                              spect_dist_info **spec, int num,
                              int relative_to_pullp)
{
  int len, /* count= 0, */ num_frames = 0;
  int ii, jj, got;
  featdata  *frame_ptr;
#ifndef NDEBUG
  featdata  *base_ptr = NULL;
#endif

  ASSERT(frmPkt);
  ASSERT(spec);
  if (!frmPkt->haveVoiced) return(0);
  if (start == end || (start == 0 && !relative_to_pullp))
    return (0);

  /*  Cannot access the frames
  */
  if (relative_to_pullp && getBlockGap(frmPkt) < start)
    return (0);

  ASSERT(base_ptr = frmPkt->pullp);
  got = setRECframePtr(frmPkt, end, relative_to_pullp);
  if (got != end)
  {
    (void) setRECframePtr(frmPkt, -got, relative_to_pullp);
    ASSERT(base_ptr == currentRECframePtr(frmPkt));
    return (0);
  }
  len = start - end;

  for (ii = 0; ii < len; ii++)
  {
    decRECframePtr(frmPkt);
    frame_ptr   = currentRECframePtr(frmPkt);
#if DEBUG
    log_report("%d %d %x\n", frame_ptr[0],
               frame_ptr[frmPkt->uttDim], frame_ptr);
#endif
    if ((frame_ptr[frmPkt->uttDim] & BELOW_THRESHOLD_BIT))
    {
      num_frames++;
      for (jj = 0; jj < num; jj++)
      {
        ASSERT(spec[jj]);
        add_distribution_data(spec[jj], (int) frame_ptr[jj]);
      }
    }
  }
#if DEBUG
  log_report("End of chunk\n");
#endif

  /* Put it back in the same place !
  */
  if (start != 0)
    (void) setRECframePtr(frmPkt, -start, relative_to_pullp);
  ASSERT(base_ptr == currentRECframePtr(frmPkt));
  return (num_frames);
}

void utterance_detection_fixup(fepFramePkt *frmPkt, featdata **last_pushp,
                               int voice_duration, int quite_duration, int unsure_duration)
{
  featdata  *fram;
  long   gotstat, count, voistat;
  featdata  *fepFrmPtr, *recFrmPtr, *last_push, voice_result;

  /* Adjust for delay in decision making by voicing_analysis
  */
  ASSERT(frmPkt);
  ASSERT(last_pushp);
  fepFrmPtr = currentFEPframePtr(frmPkt);
  last_push = *last_pushp;
  if (last_push == fepFrmPtr)
    return;

  recFrmPtr = currentRECframePtr(frmPkt);
  if (last_push == NULL)
  {
    last_push = recFrmPtr;
    voistat = FAST_MATCH_DATA(getVoicingCode(frmPkt, last_push));
  }
  else if (decThisFramePtr(frmPkt, &last_push) == False)
  {
    voistat = FAST_MATCH_DATA(getVoicingCode(frmPkt, last_push));
    (void) incThisFramePtr(frmPkt, &last_push);
  }
  else
    voistat = FAST_MATCH_DATA(getVoicingCode(frmPkt, last_push));

  while (last_push != fepFrmPtr)
  {

    gotstat = FAST_MATCH_DATA(getVoicingCode(frmPkt, last_push));
    if (gotstat != voistat)
    {
      /*  Voicing status has changed
      */
      fram = last_push;
      voice_result = getVoicingCode(frmPkt, fram);
      if (FAST_BIT_SET(voice_result))
      {
        for (count = voice_duration; count > 0 && fram != recFrmPtr;
             count--)
        {
          if (decThisFramePtr(frmPkt, &fram) != False)
            break;
#if DEBUG_REWIND
          log_report("U: voice rewind at %d was %x\n", frmPkt->pullTime
                     + POINTER_GAP(frmPkt, fram, recFrmPtr),
                     getVoicingCode(frmPkt, fram));
#endif
          setVoicingCode(frmPkt, fram, REC_VOICE_BIT);
        }

        /* set to unsure for start period of voicing
        */
        for (count = 0; count < unsure_duration && fram != recFrmPtr;
             count++)
        {
          if (decThisFramePtr(frmPkt, &fram) != False)
            break;
#if DEBUG_REWIND
          log_report("U: unsure rewind at %d was %x\n", frmPkt->pullTime
                     + POINTER_GAP(frmPkt, fram, recFrmPtr),
                     getVoicingCode(frmPkt, fram));
#endif
          setVoicingCode(frmPkt, fram, REC_UNSURE_BIT);
        }
      }

      else if (QUIET_BIT_SET(voice_result))
      {
        for (count = quite_duration; count > 0 && fram != recFrmPtr;
             count--)
        {
          if (decThisFramePtr(frmPkt, &fram) != False)
            break;
#if DEBUG_REWIND
          log_report("U: quiet rewind at %d was %x\n", frmPkt->pullTime
                     + POINTER_GAP(frmPkt, fram, recFrmPtr),
                     getVoicingCode(frmPkt, fram));
#endif
          setVoicingCode(frmPkt, fram, REC_QUIET_BIT);
        }
      }

      voistat = gotstat;
    }

    /* copy to recognizer bits if status not changed */
#if DEBUG_REWIND
    log_report("U: copying at %d was %x\n", frmPkt->pullTime
               + POINTER_GAP(frmPkt, last_push, recFrmPtr),
               getVoicingCode(frmPkt, last_push));
#endif
    if (QUIET_BIT_SET(getVoicingCode(frmPkt, last_push)))
      setVoicingCode(frmPkt, last_push, REC_QUIET_BIT);
    else if (FAST_BIT_SET(getVoicingCode(frmPkt, last_push)))
      setVoicingCode(frmPkt, last_push, REC_VOICE_BIT);
    else
      setVoicingCode(frmPkt, last_push, REC_UNSURE_BIT);

    if (incThisFramePtr(frmPkt, &last_push) != False) break;
  }

  *last_pushp = last_push;
  return;
}

int rec_frame_voicing_status(fepFramePkt *frmPkt)
{
  ASSERT(frmPkt);
  return (getVoicingCode(frmPkt, (featdata *)frmPkt->pullp));
}
