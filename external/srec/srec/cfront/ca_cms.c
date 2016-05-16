/*---------------------------------------------------------------------------*
 *  ca_cms.c                                                                 *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                         *
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

/************************************************************************
 * CA_Wave Methods
 ************************************************************************/

#ifndef _RTT
#include <stdio.h>
#endif
#include <stdlib.h>
#include <string.h>
#include <limits.h>

#ifdef unix
#include <unistd.h>
#endif
#include <assert.h>


#include "frontapi.h"
#include "portable.h"
#include "swicms.h"

#ifdef SET_RCSID
static const char *rcsid = 0 ? (const char *) &rcsid :
                           "$Id: ca_cms.c,v 1.7.6.11 2008/05/27 16:08:28 dahan Exp $";
#endif


ESR_ReturnCode CA_SetCMSParameters ( CA_Wave *hWave, const LCHAR *param_string )
{
  ESR_ReturnCode set_status;

  if ( hWave != NULL )
    set_status = swicms_set_cmn ( hWave->data.channel->swicms, param_string );
  else
    set_status = ESR_INVALID_STATE;
  return ( set_status );
}


ESR_ReturnCode CA_GetCMSParameters ( CA_Wave *hWave, LCHAR *param_string, size_t* len )
{
  ESR_ReturnCode get_status;

  if ( hWave != NULL )
    get_status = swicms_get_cmn ( hWave->data.channel->swicms, param_string, len );
  else
    get_status = ESR_INVALID_STATE;
  return ( get_status );
}


void CA_ReLoadCMSParameters(CA_Wave *hWave, const char *basename)
{
  ASSERT(hWave);
  if (hWave->is_configuredForAgc == False)
    SERVICE_ERROR(UNCONFIGURED_CMS_AND_AGC);
  if (!basename) {
    if( swicms_init(hWave->data.channel->swicms) )
      SERVICE_ERROR(UNEXPECTED_DATA_ERROR);
  }
  else
    SERVICE_ERROR(FEATURE_NOT_SUPPORTED);
}


void CA_SaveCMSParameters(CA_Wave *hWave, const char *basename)
{
	SERVICE_ERROR(FEATURE_NOT_SUPPORTED);
}


void CA_LoadCMSParameters(CA_Wave *hWave, const char *basename,
                          CA_FrontendInputParams *hFrontArgs)
{
  TRY_CA_EXCEPT
#if !defined(_RTT)
  ASSERT(hWave);
  /* ASSERT (basename); */
  ASSERT(hFrontArgs);

  if (hWave->is_configuredForAgc == True)
    SERVICE_ERROR(CONFIGURED_CMS_AND_AGC);
  if (hWave->is_attached == True)
    SERVICE_ERROR(ATTACHED_CMS_AND_AGC);

  hWave->data.channel->channorm = create_channel_normalization();
  /* load_channel_parameters (basename, hWave->data.channel->channorm);
     not used anymore, rather we spec this is the parfile directly */
  hWave->data.channel->channorm->dim = MAX_CHAN_DIM;
  setup_channel_normalization(hWave->data.channel->channorm,
                              hWave->data.channel->spchchan,
#if NORM_IN_IMELDA
                              hFrontArgs->mel_dim * 3, /* TODO: find appropriate number */
#else
                              hFrontArgs->mel_dim,
#endif
                              hFrontArgs->forget_factor);
  hWave->data.channel->mel_dim = hFrontArgs->mel_dim; /* TODO: more checks */

  hWave->data.channel->swicms = (swicms_norm_info*)CALLOC(1, sizeof(swicms_norm_info), "cfront.swicms");
  if( swicms_init(hWave->data.channel->swicms) )
    SERVICE_ERROR(UNEXPECTED_DATA_ERROR);
  hWave->is_configuredForAgc = True;
#else
  log_report("Channel normalization or RTT not in module\n");
  SERVICE_ERROR(FEATURE_NOT_SUPPORTED);
#endif
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hFrontend);
}

void CA_ClearCMSParameters(CA_Wave *hWave)
{
  TRY_CA_EXCEPT
#if NORM_IN_IMELDA
  int dim = hWave->data.channel->mel_dim * 3;
#else
  int dim = hWave->data.channel->mel_dim;
#endif

  ASSERT(hWave);
  if (hWave->is_configuredForAgc == False)
    SERVICE_ERROR(UNCONFIGURED_CMS_AND_AGC);
  if (hWave->is_attached == True)
    SERVICE_ERROR(ATTACHED_CMS_AND_AGC);

  clear_channel_normalization(hWave->data.channel->spchchan, dim);
  destroy_channel_normalization(hWave->data.channel->channorm);
  hWave->data.channel->channorm = NULL;
  hWave->is_configuredForAgc = False;

  FREE(hWave->data.channel->swicms);
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hWave);
}

void CA_AttachCMStoUtterance(CA_Wave *hWave, CA_Utterance *hUtt)
{
  /* Link the utt's spchchan to the wave object's. This is checked in AGC fn
      to ensure that the correct Utt & Wave objects have been supplied.
  */

  TRY_CA_EXCEPT
  ASSERT(hUtt);
  ASSERT(hWave);
  if (hWave->is_configuredForAgc == False)
    SERVICE_ERROR(UNCONFIGURED_CMS_AND_AGC);
  if (hWave->is_attached == True)
    SERVICE_ERROR(ATTACHED_CMS_AND_AGC);

  ASSERT(hWave->data.channel->channorm);
  hUtt->data.gen_utt.spchchan = hWave->data.channel->spchchan;
  hUtt->data.gen_utt.channorm = hWave->data.channel->channorm;
  hUtt->data.gen_utt.swicms  = hWave->data.channel->swicms;
  hUtt->data.gen_utt.do_channorm = True;
#if NORM_IN_IMELDA       /* TODO: find appropriate number */
  hUtt->data.gen_utt.num_chan = 3 * hWave->data.channel->mel_dim;
#else
  hUtt->data.gen_utt.num_chan = hWave->data.channel->mel_dim;
#endif
  hWave->is_configuredForAgc = True;
  hWave->is_attached = True;
  return;
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hFrontend);
}

ESR_ReturnCode CA_IsCMSAttachedtoUtterance(CA_Wave* hWave, ESR_BOOL* isAttached)
{
  if (hWave == NULL || isAttached == NULL)
    return ESR_INVALID_ARGUMENT;
  *isAttached = hWave->is_attached;
  return ESR_SUCCESS;
}

ESR_ReturnCode CA_IsConfiguredForAgc(CA_Wave* hWave, ESR_BOOL* isConfigured)
{
  if (hWave == NULL || isConfigured == NULL)
    return ESR_INVALID_ARGUMENT;
  *isConfigured = hWave->is_configuredForAgc;
  return ESR_SUCCESS;
}

void CA_DetachCMSfromUtterance(CA_Wave *hWave, CA_Utterance *hUtt)
{
  TRY_CA_EXCEPT
  ASSERT(hWave);

  if (hWave->is_configuredForAgc == False)
    SERVICE_ERROR(UNCONFIGURED_CMS_AND_AGC);
  if (hUtt && hUtt->data.gen_utt.do_channorm == False)
    SERVICE_ERROR(UTTERANCE_INVALID);
  if (hWave->is_attached == False)
    SERVICE_ERROR(UNATTACHED_CMS_AND_AGC);
  if (hWave->data.channel->spchchan && hUtt->data.gen_utt.spchchan
      && hWave->data.channel->spchchan != hUtt->data.gen_utt.spchchan)
  {
    log_report("Mismatched channel and utterance\n");
    SERVICE_ERROR(BAD_CHANNEL);
  } /* TODO: find a better code */

  hUtt->data.gen_utt.channorm = NULL;
  hUtt->data.gen_utt.spchchan = NULL;
  hUtt->data.gen_utt.do_channorm = False;
  hWave->is_attached = False;

  return;
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hWave)
}

void CA_CalculateCMSParameters(CA_Wave *hWave)
{
  TRY_CA_EXCEPT

  if (hWave->is_configuredForAgc == False)
    SERVICE_ERROR(UNCONFIGURED_CMS_AND_AGC);
  if (hWave->is_attached == False)
    SERVICE_ERROR(UNATTACHED_CMS_AND_AGC);

  estimate_normalization_parameters(hWave->data.channel->channorm,
                                    hWave->data.channel->spchchan,
#if NORM_IN_IMELDA       /* TODO: find appropriate number */
                                    hWave->data.channel->mel_dim * 3);
#else
                                    hWave->data.channel->mel_dim);
#endif
  return;
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hWave);
}
