/*---------------------------------------------------------------------------*
 *  duk_io.h  *
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


#ifndef _h_dukio_
#define _h_dukio_

#ifdef SET_RCSID
static const char duk_io_h[] = "$Id: duk_io.h,v 1.7.6.4 2007/08/31 17:44:53 dahan Exp $";
#endif

#ifndef _RTT
#include <stdio.h>
#endif

#include "all_defs.h"
#include "duk_err.h"
#include "pstdio.h"

/*  File types for whole word model and tcp files
*/
#define OS_DIR_DELIM    '/'  /* OS Directory Delimiter          */
#define OS_EXT_DELIM    '.'  /* OS Filename Extension Delimeter */
#define TCP_EXT         "tcp"  /* Transcription Extension   */
#define MDL_EXT         "mdl"  /* Model         Extension   */
#define FIELD_DELIM     '#'  /* Field         Delimeter   */

#define filtered_fgets pfgets

void extractBase(char*);
int  extractFS(char*);
char extractFT(char*);

#if !defined(_RTT)
#if defined(__cplusplus) && !defined(_ASCPP)
extern "C"
{
#endif
#if defined(__cplusplus) && !defined(_ASCPP)
}
#endif

int  check_file_extension(char *filename, char *extension);
void get_file_extension(char *filename, char *extension);

void skip_line(PFile* fileptr);

int  extractFS(char *name);
char extractFT(char *name);
#endif

#endif /* _h_dukio_ */
