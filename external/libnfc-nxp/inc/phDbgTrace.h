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
 * \file phDbgTrace.h
 * Project: NFC-FRI-1.1 / HAL4.0
 *
 * $Date: Tue Apr 28 11:48:33 2009 $
 * $Author: ing08203 $
 * $Revision: 1.17 $
 * $Aliases: NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

/*@{*/
#ifndef PHDBGTRACE_H
#define PHDBGTRACE_H
/*@}*/

#include <stdio.h>


#ifdef PHDBG_TRACES 
#define MAX_TRACE_BUFFER	300

#ifndef PHDBG_TRACES_LEVEL_0
#ifndef PHDBG_TRACES_LEVEL_1
#ifndef PHDBG_TRACES_LEVEL_2
#define PHDBG_TRACES_LEVEL_0
#endif
#endif
#endif

	extern char phOsalNfc_DbgTraceBuffer[];

	#ifdef PHDBG_TRACES_LEVEL_0
	
	/*indicates an error that causes a program to abort.*/
	#define PHDBG_FATAL_ERROR(Str)		{\
											snprintf(phOsalNfc_DbgTraceBuffer,MAX_TRACE_BUFFER, \
											"FATAL ERROR in Module :%s\n",__FILE__);\
											phOsalNfc_DbgString(phOsalNfc_DbgTraceBuffer);\
											snprintf(phOsalNfc_DbgTraceBuffer, MAX_TRACE_BUFFER, \
															"In Function:%s\n",__FUNCTION__ );	\
											phOsalNfc_DbgString (phOsalNfc_DbgTraceBuffer);\
										}

	#define PHDBG_CRITICAL_ERROR(Str)	{\
											snprintf(phOsalNfc_DbgTraceBuffer, MAX_TRACE_BUFFER, \
															"CRITICAL ERROR in Module :%s\n",__FILE__);\
											phOsalNfc_DbgString(phOsalNfc_DbgTraceBuffer);\
											snprintf(phOsalNfc_DbgTraceBuffer, MAX_TRACE_BUFFER, \
															"In Function:%s\n",__FUNCTION__ );	\
											phOsalNfc_DbgString (phOsalNfc_DbgTraceBuffer);\
										}
	#define PHDBG_WARNING(Str)
	#define PHDBG_INFO(Str)	
	#endif  /*End of PHDBG_TRACES_LEVEL_0 */

	#ifdef PHDBG_TRACES_LEVEL_1

	/*indicates an error that causes a program to abort.*/
	#define PHDBG_FATAL_ERROR(Str)		{\
											snprintf(phOsalNfc_DbgTraceBuffer, MAX_TRACE_BUFFER, \
															"FATAL ERROR in Module :%s\n",__FILE__);\
											phOsalNfc_DbgString(phOsalNfc_DbgTraceBuffer);\
											snprintf(phOsalNfc_DbgTraceBuffer, MAX_TRACE_BUFFER, \
															"In Function:%s\n",__FUNCTION__ );	\
											phOsalNfc_DbgString (phOsalNfc_DbgTraceBuffer);\
										}

	#define PHDBG_CRITICAL_ERROR(Str)	{\
											snprintf(phOsalNfc_DbgTraceBuffer, MAX_TRACE_BUFFER, \
											"CRITICAL ERROR in Module :%s\n",__FILE__);\
											phOsalNfc_DbgString(phOsalNfc_DbgTraceBuffer);\
											snprintf(phOsalNfc_DbgTraceBuffer, MAX_TRACE_BUFFER, \
															"In Function:%s\n",__FUNCTION__ );	\
											phOsalNfc_DbgString (phOsalNfc_DbgTraceBuffer);\
										}
	/*Normally this macro shall be used indicate system state that might cause problems in future.*/
	#define PHDBG_WARNING(Str)			{\
											snprintf(phOsalNfc_DbgTraceBuffer, MAX_TRACE_BUFFER, \
															"WARNING :%s\n",__FILE__);\
											phOsalNfc_DbgString(phOsalNfc_DbgTraceBuffer);\
											phOsalNfc_DbgString (Str);\
											phOsalNfc_DbgString ("\n");\
										}
	#define PHDBG_INFO(Str)	
	#endif /*End of PHDBG_TRACES_LEVEL_1 */

	#ifdef PHDBG_TRACES_LEVEL_2

	/*indicates an error that causes a program to abort.*/
	#define PHDBG_FATAL_ERROR(Str)		{\
											snprintf(phOsalNfc_DbgTraceBuffer, MAX_TRACE_BUFFER, \
											"FATAL ERROR in Module :%s\n",__FILE__);\
											phOsalNfc_DbgString(phOsalNfc_DbgTraceBuffer);\
											snprintf(phOsalNfc_DbgTraceBuffer, MAX_TRACE_BUFFER, \
															"In Function:%s\n",__FUNCTION__ );	\
											phOsalNfc_DbgString (phOsalNfc_DbgTraceBuffer);\
										}

	#define PHDBG_CRITICAL_ERROR(Str)	{\
											snprintf(phOsalNfc_DbgTraceBuffer, MAX_TRACE_BUFFER, \
											"CRITICAL ERROR in Module :%s\n",__FILE__);\
											phOsalNfc_DbgString(phOsalNfc_DbgTraceBuffer);\
											snprintf(phOsalNfc_DbgTraceBuffer, MAX_TRACE_BUFFER, \
															"In Function:%s\n",__FUNCTION__ );	\
											phOsalNfc_DbgString (phOsalNfc_DbgTraceBuffer);\
										}
	/*Normally this macro shall be used indicate system state that might cause problems in future.*/
	#define PHDBG_WARNING(Str)			{\
											snprintf(phOsalNfc_DbgTraceBuffer, MAX_TRACE_BUFFER, \
											"WARNING :%s\n",__FILE__);\
											phOsalNfc_DbgString(phOsalNfc_DbgTraceBuffer);\
											phOsalNfc_DbgString (Str);\
											phOsalNfc_DbgString ("\n");\
										}
	
	#define PHDBG_INFO(Str)				{\
											snprintf(phOsalNfc_DbgTraceBuffer, MAX_TRACE_BUFFER, \
											"DBG INFO :%s\n",__FILE__);\
											phOsalNfc_DbgString(phOsalNfc_DbgTraceBuffer);\
											phOsalNfc_DbgString (Str);\
											phOsalNfc_DbgString ("\n");\
										}
	


#endif  /*End of PHDBG_TRACES_LEVEL_2 */
#else
#define PHDBG_FATAL_ERROR(Str)
#define PHDBG_CRITICAL_ERROR(Str)	
#define PHDBG_WARNING(Str)
#define PHDBG_INFO(Str)	


#endif /*end of DEBUG trace*/
#endif /* end of PHDBGTRACE_H */
