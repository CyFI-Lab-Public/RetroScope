/*---------------------------------------------------------------------------*
 *  vcc_helper.h  *
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

#ifndef _VCC_HELPER_H
#define _VCC_HELPER_H

#define OS_MSDOS 1
#define OS_WIN32 2
#define OS_MAC 4
#define OS_UNIX 8
#define OS_EMBEDDED 16
#define OS_VXWORKS 32
#define OS_PSOS 64
#define OS_WINCE 128
#define OS_PALM 256
#define OS_JAVA 512
#define OS_QNX 1024

#define CPU_I86 1
#define CPU_68K 2
#define CPU_MIPS 4
#define CPU_ALPHA 8
#define CPU_PPC 16
#define CPU_SPARC 32
#define CPU_ARM 64
#define CPU_STRONGARM 128
#define CPU_TMS320X 256
#define CPU_SH3 512
#define CPU_SH4 = 1024

#define C_BORLAND 1
#define C_MICROSOFT 2
#define C_INTEL 4
#define C_HIGH 8
#define C_ZORTECH 16
#define C_WATCOM 32
#define C_GNU 64
#define C_SUNPRO 128
#define C_DECCXX 256
#define C_METROWERKS 512
#define C_GHS 1024
#define C_TICXC 2048
#define C_ARM 4096
#define C_DIABDATA 8192

#define BUILD_SHIP 1
#define BUILD_INHOUSE 2
#define BUILD_DEBUGO 4
#define BUILD_DEBUG 8
#define BUILD_PROFILE 16
#define BUILD_TRACE 32

//Developers should define:
//
//OS
//HOST_OS
//TARGET_OS
//CPU
//HOST_CPU
//TARGET_CPU
//COMPILER
//BUILD
//
// Other keywords that were used in the makefile and were not migrated:
//-DDISABLE_EXCEPTION_HANDLING /DWIN32 /D_WIN32 /DSOUNDBLASTER /D_DEBUG /D_DLL /D_ASCPP

#endif //_VCC_HELPER_H
