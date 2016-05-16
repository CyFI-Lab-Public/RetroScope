/*---------------------------------------------------------------------------*
 *  cheldsp4.c  *
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


#include <stdlib.h>
#include <limits.h>
#ifndef _RTT
#include <stdio.h>
#endif
#include <string.h>
#include <math.h>
#include <assert.h>


#include "hmm_desc.h"
#include "voicing.h"
#include "specnorm.h"
#include "portable.h"
#include "front.h"
#include "portable.h"

#include "sh_down.h"
#include "memmove.h"


#define DEBUG  0
#define LOG_AS_PRINT 0


#if LOG_AS_PRINT /* BP temp debug mode */
#define log_report printf
#endif



/* Rasta */
#define RASTA_SOFT_START   1   /* if this is not 1, rasta initialization is weighted */

#if RASTA_SOFT_START
#define RASTA_CONSTANT  0.92
#define RASTA_SOFT_CONST (RASTA_CONSTANT/(1-RASTA_CONSTANT))
#else
#define RASTA_CONSTANT  0.85
#endif

static void regress(cepdata *rg, const cepdata *cp_buf, unsigned short frmind,
                    int mel_dim);
static void dd_regress(cepdata *dd, const cepdata *cp_buf, unsigned short frmind,
                       int mel_dim);
static void scale_data(const front_cep *cepobj, const featdata *rpram,  featdata *pram1,
                       featdata *pram2, featdata *ddpram, const cepdata *rast,
                       const cepdata *cep, const cepdata *rcep, const cepdata *ddcep);
static void pack_frame(const front_cep *cepobj, featdata *dest_frame,
                       const featdata *rasta_data, const featdata *mel_data,
                       const featdata *del_data, const featdata *deldel_data);



static void regress(cepdata *rg, const cepdata*cp_buf, unsigned short frmind,
                    int mel_dim)
{
  int     i, j, d;
  cepdata val;
  const cepdata* cpt;
  /*
  static cepdata a = (cepdata) 0.0;

  if (a == (cepdata) 0.0)
  {
    for (j = 1; j <= DELTA; j++)
      a += j * j;
    a *= (cepdata) 2.0;
  }
  */
  /* replace above code with the following constant */
  cepdata a = (DELTA * (DELTA + 1) * (2 * DELTA + 1) / 6) * 2;
  d = DELTA;
  if (frmind < Q2 - 1)
  {
    cpt = cp_buf + (d - 1) * (mel_dim + 1);
    for (i = 0; i <= mel_dim; i++, cpt++)
      rg[i] = (cepdata) SHIFT_DOWN((*cpt - *(cpt + mel_dim + 1)), 1 + COSINE_TABLE_SHIFT);  /* Shift does rounding. */
  } /* reversed */
  else
    /* regression coefficients */
    for (i = 0; i <= mel_dim; i++)
    {
      cpt = cp_buf + i;
			val = (cepdata) 0.;
			for (j = 0; j < Q2; j++, cpt += (mel_dim + 1))
				val += (d - j) * SHIFT_DOWN(*cpt, 5);          /* note d-j from j-d */
			rg[i] = (cepdata) SHIFT_DOWN((bigdata)(val / a), COSINE_TABLE_SHIFT - 5); /* scale down the deltas here */
		}
  return;
}
static const cepdata deldel[] = {2, 0, -1, -2, -1, 0, 2};  /* delta - delta */

void dd_regress(cepdata *dd, const cepdata *cp_buf, unsigned short frmind, int mel_dim)
/*
**  Computes ALL delta delta mel cep pars. BP 8/96 */
{
  int i, j, d;
  cepdata val;
  const cepdata *cpt;

  d = DELTA;
  if (frmind < Q2 - 1)
  {
    cpt = cp_buf + (mel_dim + 1);
    for (i = 0;i <= mel_dim;i++)
    {
      dd[i] = (*(cpt + 2 * (mel_dim + 1) + i)
               + *(cpt + i)
               - 2 * (*(cpt + (mel_dim + 1) + i)));
      /* Undo cosine table shifting */
      dd[i] = (cepdata) SHIFT_DOWN((bigdata)(dd[i]), COSINE_TABLE_SHIFT);
    }
  }
  else
  {
    /* DD coefficient*/
    for (i = 0; i <= mel_dim; i++)
    {
      cpt = cp_buf + i;
      val = (cepdata) 0.;
      for (j = 0; j < Q2; j++, cpt += (mel_dim + 1))
        val += deldel[j] * SHIFT_DOWN((*cpt), 4);    /* Q2 frames forward, not around...? */
      /* Watch out for overflows here */
      dd[i] = (cepdata) SHIFT_DOWN((bigdata)(val), COSINE_TABLE_SHIFT - 4);
    }
  }
  return;
}


static void scale_data(const front_cep *cepobj, const featdata *rpram,  featdata *pram1,
                       featdata *pram2, featdata *ddpram, const cepdata *rast,
                       const cepdata *cep, const cepdata *rcep, const cepdata *ddcep)
{
  size_t   i;
  bigdata a;

  if (pram1)
    for (i = 0; i <= cepobj->mel_dim; i++)
    {
      /* Now take the costable scaling off the ceps. */
      ASSERT((cepobj->melA_scale[i] *(float)SHIFT_DOWN(cep[i], COSINE_TABLE_SHIFT))
             < LONG_MAX);
      ASSERT((cepobj->melA_scale[i] *(float)SHIFT_DOWN(cep[i], COSINE_TABLE_SHIFT))
             > -LONG_MAX);
      a = (bigdata)(SHIFT_DOWN((bigdata)cepobj->melA_scale[i]
                               * (bigdata) SHIFT_DOWN(cep[i], COSINE_TABLE_SHIFT)
                               + (bigdata)cepobj->melB_scale[i], BYTERANGE_SHIFT + LOG_SCALE_SHIFT));
      pram1[i] = (featdata) MAKEBYTE(a);
    }
  if (pram2)
    for (i = 0; i <= cepobj->mel_dim; i++)
    {
      ASSERT((cepobj->dmelA_scale[i] *(float)rcep[i]) < LONG_MAX);
      ASSERT((cepobj->dmelA_scale[i] *(float)rcep[i]) > -LONG_MAX);
      a = (bigdata) SHIFT_DOWN((bigdata)cepobj->dmelA_scale[i] * (bigdata)rcep[i] +
                               (bigdata)cepobj->dmelB_scale[i], BYTERANGE_SHIFT + LOG_SCALE_SHIFT);
      pram2[i] = (featdata) MAKEBYTE(a);
    }

  /* Double-deltas parameter scaling */
  if (cepobj->do_dd_mel && ddpram)
    for (i = 0; i <= cepobj->mel_dim; i++)
    {
      ASSERT((cepobj->ddmelA_scale[i] *(float)ddcep[i]) < LONG_MAX);
      ASSERT((cepobj->ddmelA_scale[i] *(float)ddcep[i]) > -LONG_MAX);
      a = (bigdata) SHIFT_DOWN((bigdata)cepobj->ddmelA_scale[i] * (bigdata)ddcep[i] +
                               (bigdata)cepobj->ddmelB_scale[i], BYTERANGE_SHIFT + LOG_SCALE_SHIFT);
      ddpram[i] = (featdata) MAKEBYTE(a); /* sort out scaling of deldel? */
    }
  return;
}

static void pack_frame(const front_cep *cepobj, featdata *dest_frame,
                       const featdata *rasta_data, const featdata *mel_data,
                       const featdata *del_data, const featdata *deldel_data)
{
  size_t ii, cnt;

  cnt = 0;
  for (ii = 0; ii < cepobj->mel_dim; ii++, cnt++)
    dest_frame[cnt] = (featdata) mel_data[ii];
  for (ii = 0; ii < cepobj->mel_dim; ii++, cnt++)
    dest_frame[cnt] = (featdata) del_data[ii];
  if (cepobj->do_dd_mel)
    for (ii = 0; ii < cepobj->mel_dim; ii++, cnt++)
      dest_frame[cnt] = (featdata) deldel_data[ii];

#if DEBUG
  log_report("Frame: ");
  for (ii = 0; ii < 24; ii++)
    log_report("%d ", dest_frame[ii]);
  if (cepobj->do_dd_mel)
    for (ii = 0; ii < cepobj->mel_dim; ii++)
      log_report("%d ", dest_frame[2*cepobj->mel_dim+ii]);
  log_report("\n");
#endif
  return;
}

int make_std_frame(front_channel *channel, front_cep *cepobj, featdata *hFrame)
{
  featdata rpram[MAX_CEP_DIM+1], spram1[MAX_CEP_DIM+1], spram2[MAX_CEP_DIM+1], ddpram[MAX_CEP_DIM+1];
  cepdata  rgmcep[MAX_CEP_DIM+1];     /*regression MCEP coef. */
  cepdata  ddmcep[MAX_CEP_DIM+1];     /*del-del-MCEP coef. */
  cepdata  rastapar[MAX_CEP_DIM+1];     /*del-del-MCEP coef. */

  channel->frame_valid = False;
  if (channel->frame_count >= channel->frame_delay)
  {
    /*  Part III.   Delta Cepstrum calculations
    */
    regress(rgmcep, channel->cep, (unsigned short) channel->frame_count, channel->mel_dim);
    if (cepobj->do_dd_mel)
      dd_regress(ddmcep, channel->cep, (unsigned short) channel->frame_count, channel->mel_dim);
#if DEBUG
    log_report("Cep before scaling: ");
    write_scaled_frames(channel->mel_dim + 1, 1,
                        channel->cep  + (DELTA) *(channel->mel_dim + 1),
                        D_FIXED, (float)1 / (0x01 << (LOG_SCALE_SHIFT + COSINE_TABLE_SHIFT)));
    log_report("Delta Cep before scaling: ");
    write_scaled_frames(channel->mel_dim + 1, 1, rgmcep, D_FIXED, (float)1 / (0x01 << LOG_SCALE_SHIFT));
    log_report("DeltaDelta Cep before scaling: ");
    write_scaled_frames(channel->mel_dim + 1, 1, ddmcep, D_FIXED, (float)1 / (0x01 << LOG_SCALE_SHIFT));
#endif
    scale_data(cepobj, rpram, spram1, spram2, ddpram, rastapar,
               channel->cep  + (DELTA) *(channel->mel_dim + 1),  rgmcep, ddmcep);
#if DEBUG
    log_report("Cep after scaling: ");
    write_frames(channel->mel_dim + 1, 1, spram1, D_CHAR);
    log_report("Delta Cep after scaling: ");
    write_frames(channel->mel_dim + 1, 1, spram2, D_CHAR);
    log_report("DeltaDelta Cep after scaling: ");
    write_frames(channel->mel_dim + 1, 1, ddpram, D_CHAR);
#endif
    channel->frame_valid = True;     /* True even if do_skip_even_frames, */
    pack_frame(cepobj, hFrame, rpram, spram1, spram2, ddpram);
  } /* >=DELTA */

  channel->frame_count++;
  return (channel->frame_valid);
}
