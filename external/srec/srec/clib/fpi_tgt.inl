/*---------------------------------------------------------------------------*
 *  fpi_tgt.inl  *
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

#ifndef _fpi_tgt_inl_
#define _fpi_tgt_inl_

#include "fpi_tgt.h"
#include "PortExport.h"

static PINLINE int isFrameBufferActive(fepFramePkt* frmPkt);
static PINLINE void setFrameBufferDead(fepFramePkt* frmPkt);
static PINLINE int getFrameGap(fepFramePkt* frmPkt);
static PINLINE int getBlockGap(fepFramePkt* frmPkt);


static PINLINE int isFrameBufferActive(fepFramePkt* frmPkt)
{
  ASSERT(frmPkt);
  if (frmPkt->isCollecting == FB_ACTIVE)
    return (True);
  else
    return (False);
}

static PINLINE void setFrameBufferDead(fepFramePkt* frmPkt)
{
  ASSERT(frmPkt);
  frmPkt->isCollecting = FB_DEAD;
  return;
}

/************************************************************************
 * Returns number of unread frames in buffer (Those not seen by REC)    *
 ************************************************************************
 *
 *                 "FRAMES_IN_BUF"
 *   <------------------------------------------>
 *
 *                            "pushp"
 *                               |
 *                               v
 *  +--------------------------------------------+
 *  |          |x|              |x|              |  <= Frame Buffer for
 *  +--------------------------------------------+      Channel 'n'
 *              |
 *              v
 *            "pullp"
 *
 *              <--------------->
 *                 'frameGap()'
 *
 *               Scenario A:    If: FRAME_IN_BUFFER == 1000
 *                                  pullp == frame 70
 *                                  pushp == frame 900   Gap == 830
 *
 *               Scenario B:    If: FRAME_IN_BUFFER == 1000
 *                                  pullp == frame 720
 *                                  pushp == frame 600   Gap == 880
 *
 * HOW FAR CAN WE MOVE ? (for 'moveFramePtr()' function)
 * ===================
 *
 * In Scenario A
 *
 *      Forward  = +830
 *      Backward = -168
 *
 *      We can only move forward "830" frames from our
 *          current 'pullp' position, as frame 899 is the newest
 *          COMPLETE frame.
 *      We can only move backwards safely
 *          to frame number 902 (as 'pushp' may have changed during our
 *          deliberation), i.e. "(830 - FRAMES_IN_BUF + FRAME_BACK_GUARD)"
 *
 * In Scenario B
 *
 *      Forward  = +880
 *      Backward = -118
 *
 *      We can only move forward "880" frames from our
 *      current 'pullp' position.  We can only move backwards safely
 *      to frame number 602 (as 'pushp' may have changed during our
 *      deliberation), i.e. "(880 - FRAMES_IN_BUF + FRAME_BACK_GUARD)"
 *
 ************************************************************************
 *
 * NOTES
 *
 * If 'masterREC', 'pullp' is manipulated, otherwise one of the
 * multiple recognition pointers, 'auxPullp[]' is used.
 *
 ************************************************************************
 *
 * Arguments: "n"      Channel Number of Selected Frame
 *
 * Retunrs:   int      +ve (or ZERO) = Gap Between FEP and REC frame pointers
 *                     -ve           = Error
 *
 ************************************************************************/

static PINLINE int getFrameGap(fepFramePkt* frmPkt)
{
  ASSERT(frmPkt);
  return (POINTER_GAP(frmPkt, frmPkt->pushp, frmPkt->pullp));
}

/************************************************************************
 ************************************************************************/

static PINLINE int getBlockGap(fepFramePkt* frmPkt)
{
  ASSERT(frmPkt);
  return (POINTER_GAP(frmPkt, frmPkt->pullp, frmPkt->pushBlkp));
}

#endif
