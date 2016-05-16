/*---------------------------------------------------------------------------*
 *  port_fileio.h  *
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



#ifndef _PORT_FILEIO_H__
#define _PORT_FILEIO_H__

#ifdef TI_DSP
#include <dsp_file.h>
#define PORT_FILE DSP_FILE
#define PORT_FOPEN dsp_fopen
#define PORT_FCLOSE dsp_fclose
#define PORT_FREAD_CHAR dsp_fread_char
#define PORT_FREAD_INT16 dsp_fread_int16
#define PORT_FGETS dsp_fgets
#else
#define uint16 int
#include <stdio.h>
#include "PFile.h"
#define PORT_FILE PFile 
#define PORT_FOPEN pfopen
#define PORT_FCLOSE pfclose
#define PORT_FREAD_CHAR pfread
#define PORT_FREAD_INT16 pfread 
#define PORT_FGETS pfgets
#endif

#endif /* _PORT_FILEIO_H__ */
