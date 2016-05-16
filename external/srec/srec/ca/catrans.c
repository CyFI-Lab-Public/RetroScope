/*---------------------------------------------------------------------------*
 *  catrans.c  *
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


static const char pat_tran[] = "$Id: catrans.c,v 1.3.10.2 2007/08/31 17:44:51 dahan Exp $";

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#ifdef unix
#include <unistd.h>
#endif
#include <assert.h>

#include"simapi.h"

CA_Transform *CA_AllocateTransform(void)
{
  CA_Transform *hTransform = NULL;
  TRY_CA_EXCEPT
  
  
  hTransform = (CA_Transform *) VAR_ALLOCATE_CLR(1, sizeof(CA_Transform), "ca.hTransform");
  hTransform->is_loaded = False;
  hTransform->is_setup = False;
  
  return (hTransform);
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hTransform)
}


void CA_FreeTransform(CA_Transform *hTransform)
{
  TRY_CA_EXCEPT
  
  ASSERT(hTransform);
  VAR_FREE((char *) hTransform, "hTransform");
  return;
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hTransform)
}


int CA_LoadTransform(CA_Transform *hTransform, int dimen)
{
  TRY_CA_EXCEPT
  
  ASSERT(hTransform);
  ASSERT(dimen > 0);
  
  if (hTransform->is_loaded == True)
    SERVICE_ERROR(PATTERN_ALREADY_LOADED);
    
  hTransform->dim = dimen;
  
  hTransform->imelda_acc.between = create_accumulate_matrix(hTransform->dim);
  hTransform->imelda_acc.bmean = (accdata *) VAR_ALLOCATE(hTransform->dim,
                                 sizeof(accdata), "ca.hTransform->imelda_acc.bmean");
  hTransform->imelda_acc.within = create_accumulate_matrix(dimen);
  hTransform->imelda_acc.wmean = (accdata *) VAR_ALLOCATE(hTransform->dim,
                                 sizeof(accdata), "ca.hTransform->imelda_acc.wmean");
  hTransform->mllr_acc.between = create_accumulate_matrix(hTransform->dim + 1);
  hTransform->mllr_acc.within = create_accumulate_matrix(hTransform->dim + 1);
  
  hTransform->is_loaded = True;
  
  return (True);
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hTransform)
}


void CA_ConfigureTransform(CA_Transform *hTransform, int do_mllr, int do_imelda)
{
  TRY_CA_EXCEPT
  
  ASSERT(hTransform);
  
  hTransform->do_mllr = do_mllr;
  hTransform->do_imelda = do_imelda;
  
  return;
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hTransform)
}


void CA_UnloadTransform(CA_Transform *hTransform)
{
  TRY_CA_EXCEPT
  ASSERT(hTransform);
  if (hTransform->is_loaded == False)
    SERVICE_ERROR(PATTERN_NOT_LOADED);
    
  delete_accumulate_matrix(hTransform->imelda_acc.between, hTransform->dim);
  delete_accumulate_matrix(hTransform->imelda_acc.within, hTransform->dim);
  delete_accumulate_matrix(hTransform->mllr_acc.between, hTransform->dim + 1);
  delete_accumulate_matrix(hTransform->mllr_acc.within, hTransform->dim + 1);
  VAR_FREE(hTransform->imelda_acc.bmean, "hTransform->imelda_acc.bmean");
  VAR_FREE(hTransform->imelda_acc.wmean, "hTransform->imelda_acc.wmean");
  
  hTransform->is_loaded = False;
  
  return;
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hTransform)
}


void CA_ClearTransform(CA_Transform *hTransform)
{
  TRY_CA_EXCEPT
  int ii, jj;
  
  if (hTransform->is_loaded == False)
    SERVICE_ERROR(PATTERN_NOT_LOADED);
    
  ASSERT(hTransform->imelda_acc.within);
  ASSERT(hTransform->imelda_acc.between);
  ASSERT(hTransform->imelda_acc.wmean);
  ASSERT(hTransform->imelda_acc.bmean);
  ASSERT(hTransform->mllr_acc.within);
  ASSERT(hTransform->mllr_acc.between);
  
  for (ii = 0; ii <= hTransform->dim; ii++)
  {
    for (jj = 0; jj <= hTransform->dim; jj++)
    {
      hTransform->mllr_acc.between[ii][jj] = 0;
      hTransform->mllr_acc.within[ii][jj] = 0;
    }
  }
  
  for (ii = 0; ii < hTransform->dim; ii++)
  {
    hTransform->imelda_acc.wmean[ii] = 0;
    hTransform->imelda_acc.bmean[ii] = 0;
    for (jj = 0; jj < hTransform->dim; jj++)
    {
      hTransform->imelda_acc.between[ii][jj] = 0;
      hTransform->imelda_acc.within[ii][jj] = 0;
    }
  }
  hTransform->imelda_acc.num = 0;
  return;
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hTransform)
}


void CA_InheritAccumulates(CA_Transform *hTransform, CA_Pattern *hPattern)
{
  TRY_CA_EXCEPT
  int ii, jj;
  
  ASSERT(hPattern);
  if (hPattern->is_loaded == False)
    SERVICE_ERROR(PATTERN_NOT_LOADED);
  ASSERT(hTransform);
  if (hTransform->is_loaded == False)
    SERVICE_ERROR(PATTERN_NOT_LOADED);
    
  ASSERT(hTransform->dim == hPattern->data.dim);
  
  if (hTransform->do_mllr)
  {
  
    ASSERT(hPattern->true_accumulates);
    ASSERT(hPattern->data.do_mllr);
    ASSERT(hPattern->data.mllr_acc.between);
    ASSERT(hTransform->mllr_acc.between);
    
    for (ii = 0; ii <= hTransform->dim; ii++)
      for (jj = 0; jj <= hTransform->dim; jj++)
      {
      
        hTransform->mllr_acc.between[ii][jj] +=
          hPattern->data.mllr_acc.between[ii][jj];
        hTransform->mllr_acc.within[ii][jj] +=
          hPattern->data.mllr_acc.within[ii][jj];
          
      }
      
    log_report("\nCA_InheritAccumulates MLLR inheriting %f frames (total now %f)\n\n", hPattern->data.mllr_acc.within[hTransform->dim][hTransform->dim], hTransform->mllr_acc.within[hTransform->dim][hTransform->dim]);
  }
  
  if (hTransform->do_imelda)
  {
  
    ASSERT(hPattern->data.do_imelda);
    ASSERT(hPattern->data.mllr_acc.between);
    ASSERT(hTransform->mllr_acc.between);
    
    for (ii = 0; ii < hTransform->dim; ii++)
      for (jj = 0; jj < hTransform->dim; jj++)
      {
      
        hTransform->imelda_acc.between[ii][jj] +=
          hPattern->data.imelda_acc.between[ii][jj];
        hTransform->imelda_acc.within[ii][jj] +=
          hPattern->data.imelda_acc.within[ii][jj];
      }
      
    for (ii = 0; ii < hTransform->dim; ii++)
    {
    
      hTransform->imelda_acc.bmean[ii] +=
        hPattern->data.imelda_acc.bmean[ii];
      hTransform->imelda_acc.wmean[ii] +=
        hPattern->data.imelda_acc.wmean[ii];
    }
    
    hTransform->imelda_acc.num += hPattern->data.imelda_acc.num;
    
    log_report("\nCA_InheritAccumulates Imelda inheriting %d frames (total now %d)\n\n", hPattern->data.imelda_acc.num, hTransform->imelda_acc.num);
  }
  
  return;
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hPattern)
}


void CA_LoadTransformAccumulates(CA_Pattern *hPattern,
                                 CA_Transform *hTransform)
{
  TRY_CA_EXCEPT
  int ii, jj;
  
  ASSERT(hPattern);
  if (hPattern->is_loaded == False)
    SERVICE_ERROR(PATTERN_NOT_LOADED);
  ASSERT(hTransform);
  if (hTransform->is_loaded == False)
    SERVICE_ERROR(PATTERN_NOT_LOADED);
    
  ASSERT(hTransform->dim == hPattern->data.dim);
  
  if (hTransform->do_mllr)
  {
  
    ASSERT(hPattern->data.do_mllr);
    ASSERT(hPattern->data.mllr_acc.between);
    ASSERT(hTransform->mllr_acc.between);
    
    for (ii = 0; ii <= hTransform->dim; ii++)
      for (jj = 0; jj <= hTransform->dim; jj++)
      {
      
        hPattern->data.mllr_acc.between[ii][jj] =
          hTransform->mllr_acc.between[ii][jj];
        hPattern->data.mllr_acc.within[ii][jj] =
          hTransform->mllr_acc.within[ii][jj];
      }
  }
  
  if (hTransform->do_imelda)
  {
  
    ASSERT(hPattern->data.do_imelda);
    ASSERT(hPattern->data.mllr_acc.between);
    ASSERT(hTransform->mllr_acc.between);
    
    for (ii = 0; ii < hTransform->dim; ii++)
      for (jj = 0; jj < hTransform->dim; jj++)
      {
      
        hPattern->data.imelda_acc.between[ii][jj] =
          hTransform->imelda_acc.between[ii][jj];
        hPattern->data.imelda_acc.within[ii][jj] =
          hTransform->imelda_acc.within[ii][jj];
      }
      
    for (ii = 0; ii < hTransform->dim; ii++)
    {
    
      hPattern->data.imelda_acc.bmean[ii] =
        hTransform->imelda_acc.bmean[ii];
      hPattern->data.imelda_acc.wmean[ii] =
        hTransform->imelda_acc.wmean[ii];
    }
    
    hPattern->data.imelda_acc.num = hTransform->imelda_acc.num;
    
  }
  
  return;
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hPattern)
}


