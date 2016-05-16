/*---------------------------------------------------------------------------*
 *  PFileSystemUNIXImpl.c  *
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

#include "PANSIFileImpl.h"
#include "PANSIFileSystemImpl.h"
#include "PFileSystem.h"
#include "PFileSystemImpl.h"
#include "PANSIFileSystem.h"
#include "phashtable.h"
#include "plog.h"
#include "pmemory.h"


#ifdef USE_THREAD
	/* Prototype of private function */
	PORTABLE_API ESR_ReturnCode PtrdFlush();
#endif


/** 
 * Initializes STDIN, STDOUT, STDERR.
 */
ESR_ReturnCode PFileSystemInitializeStreamsImpl(void)
{
	ESR_ReturnCode rc;
	PANSIFileImpl* impl;
#ifdef USE_THREAD
	ESR_BOOL threadingEnabled;
#endif
	ESR_BOOL isLittleEndian;
	PANSIFileSystemImpl* ANSIImpl = NULL;

#if __BYTE_ORDER==__LITTLE_ENDIAN
	isLittleEndian = ESR_TRUE;
#else
	isLittleEndian = ESR_FALSE;
#endif
	CHKLOG(rc, PANSIFileSystemCreate());
	ANSIImpl = (PANSIFileSystemImpl*) PANSIFileSystemSingleton;
	CHKLOG(rc, PMemSetLogEnabled(ESR_FALSE));
	CHKLOG(rc, PHashTablePutValue(PFileSystemPathMap, L("/"), PANSIFileSystemSingleton, NULL));
	CHKLOG(rc, PHashTablePutValue(ANSIImpl->directoryMap, L("/"), L("/"), NULL));
	CHKLOG(rc, PANSIFileSystemSingleton->createPFile(PANSIFileSystemSingleton, L("/dev/stdin"), isLittleEndian, &PSTDIN));
	impl = (PANSIFileImpl*) PSTDIN;
	impl->value = stdin;

	CHKLOG(rc, PANSIFileSystemSingleton->createPFile(PANSIFileSystemSingleton, L("/dev/stdout"), isLittleEndian, &PSTDOUT));
	impl = (PANSIFileImpl*) PSTDOUT;
		setvbuf(stdout, NULL, _IONBF, 0);
	impl->value = stdout;

	CHKLOG(rc, PANSIFileSystemSingleton->createPFile(PANSIFileSystemSingleton, L("/dev/stderr"), isLittleEndian, &PSTDERR));
	impl = (PANSIFileImpl*) PSTDERR;
		setvbuf(stderr, NULL, _IONBF, 0);
	impl->value = stderr;

	#ifdef USE_THREAD
	/* Have STDERR and STDOUT share the same lock */
	CHKLOG(rc, PtrdIsEnabled(&threadingEnabled));
	if (threadingEnabled)
	{
		CHKLOG(rc, PtrdMonitorDestroy(impl->Interface.lock));
		impl->Interface.lock = ((PANSIFileImpl*) PSTDOUT)->Interface.lock;
	}
	#endif
	CHKLOG(rc, PHashTableRemoveValue(PFileSystemPathMap, L("/"), NULL));
	CHKLOG(rc, PHashTableRemoveValue(ANSIImpl->directoryMap, L("/"), NULL));
	CHKLOG(rc, PMemSetLogEnabled(ESR_TRUE));
	return ESR_SUCCESS;
CLEANUP:
	PHashTableRemoveValue(PFileSystemPathMap, L("/"), NULL);
	if (ANSIImpl!=NULL)
		PHashTableRemoveValue(ANSIImpl->directoryMap, L("/"), NULL);
	PMemSetLogEnabled(ESR_TRUE);
	return rc;
}

ESR_ReturnCode PFileSystemShutdownStreamsImpl(void)
{
	ESR_ReturnCode rc;
	PANSIFileImpl* impl;

	/* It is illegal to log to file after the file system has shutdown so we do it now */
#ifdef USE_THREAD
	PtrdFlush();
#endif
	PMemDumpLogFile();

	if (PSTDIN!=NULL)
	{
		CHKLOG(rc, PFileFlush(PSTDIN));
		impl = (PANSIFileImpl*) PSTDIN;
		impl->value = NULL;
		CHKLOG(rc, PFileDestroy(PSTDIN));
		PSTDIN = NULL;
	}
	if (PSTDOUT!=NULL)
	{
#ifdef USE_THREAD
		if (PSTDERR!=NULL)
		{
			/* stdout, stderr share the same lock, only one of them should destroy it */
			PFileImpl* impl = (PFileImpl*) PSTDOUT;
			
			impl->lock = NULL;
		}
#endif
		CHKLOG(rc, PFileFlush(PSTDOUT));
		impl = (PANSIFileImpl*) PSTDOUT;
		impl->value = NULL;
		CHKLOG(rc, PFileDestroy(PSTDOUT));
		PSTDOUT = NULL;
	}
	if (PSTDERR!=NULL)
	{
		CHKLOG(rc, PFileFlush(PSTDERR));
		impl = (PANSIFileImpl*) PSTDERR;
		impl->value = NULL;
		CHKLOG(rc, PFileDestroy(PSTDERR));
		PSTDERR = NULL;
	}
	CHKLOG(rc, PANSIFileSystemDestroy());
	return ESR_SUCCESS;
CLEANUP:
	return rc;
}
