/*---------------------------------------------------------------------------*
 *  pLastError.c  *
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

#include "pLastError.h"
#include "plog.h"

void printGetLastErrorInternal(const LCHAR* text, char* file, int line)
{
#ifdef _WIN32
  LCHAR* msg;
  
  if (FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER |
                    FORMAT_MESSAGE_FROM_SYSTEM |
                    FORMAT_MESSAGE_IGNORE_INSERTS,
                    NULL,
                    GetLastError(),
                    MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), /* Default language */
                    (LPTSTR) &msg,
                    0,
                    NULL))
  {
    ESR_BOOL isInit;
    ESR_ReturnCode rc;
    msg[LSTRLEN(msg)-2] = L('\0'); /* cut off newline character */
    
    rc = PLogIsInitialized(&isInit);
    if (rc != ESR_SUCCESS)
      isInit = FALSE;
    if (isInit)
      PLogError(L("%s: %s"), text, msg);
    else
      pfprintf(PSTDERR, L("[%s:%d] %s: %s\n"), file, line, text, msg);
    LocalFree(msg);
  }
#elif defined(__vxworks)
  int err;
  
  err = errnoGet(); /* get the error status value of the calling task */
#ifndef NDEBUG
  /*  printErrno(err); */ /* need special flag to build Simulator */
#endif
  pfprintf(PSTDERR, "[%s:%d] %s, errno = %x\n", file, line, text, err);
  
#elif (OS == OS_UNIX)

#else
#error("Have not implemented yet!!!")
#endif
}
