/*
 * Copyright (C) 2010 NXP Semiconductors
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
  
/**
 * \file  phOsalNfc_Utils.c
 * \brief OSAL Implementation.
 *
 * Project: NFC-FRI 1.1
 *
 * $Date: Wed Jun 24 10:35:10 2009 $
 * $Author: ravindrau $
 * $Revision: 1.1 $
 * $Aliases: NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

#include <phNfcStatus.h>
#include <phNfcCompId.h>
#include <phNfcConfig.h>
#include <phOsalNfc.h>


/*The function does a comparison of two strings and returns a non zero value 
if two strings are unequal*/
int phOsalNfc_MemCompare ( void *src, void *dest, unsigned int n )
{
    int8_t *b1  =(int8_t *)src;
    int8_t *b2  =(int8_t *)dest;
    int8_t   diff = 0;
#ifdef VERIFY_MEMORY
    if((NULL != src) && (NULL != dest))
#endif
    {
        for(;((n>0)&&(diff==0));n--,b1++,b2++)
        {
            diff = *b1 - *b2;
        }
    }
    return (int)diff;
}




