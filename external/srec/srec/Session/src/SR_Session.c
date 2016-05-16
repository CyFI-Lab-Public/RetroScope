/*---------------------------------------------------------------------------*
 *  SR_Session.c  *
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


#include "ESR_Session.h"
#include "SR_EventLog.h"
#include "SR_Session.h"
#include "plog.h"


ESR_ReturnCode SR_SessionCreate(const LCHAR* filename)
{
  ESR_ReturnCode rc;
  LCHAR baseDirectory[P_PATH_MAX];
  LCHAR* fwdSlash;
  LCHAR* backSlash;
  ESR_BOOL exists;
  SR_EventLog* eventLog = NULL;
  size_t logLevel;
  
  CHKLOG(rc, ESR_SessionCreate(filename));
  rc = ESR_SessionGetSize_t(L("SREC.Recognizer.osi_log_level"), &logLevel);
  if (rc == ESR_SUCCESS)
    CHKLOG(rc, SR_EventLogCreate(&eventLog));
  else if (rc == ESR_NO_MATCH_ERROR)
    CHKLOG(rc, ESR_SessionSetSize_t(L("SREC.Recognizer.osi_log_level"), 0));
  else
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  LSTRCPY(baseDirectory, filename);
  fwdSlash = LSTRRCHR(baseDirectory, '\\');
  backSlash = LSTRRCHR(baseDirectory, '/');
  
  if (fwdSlash == NULL && backSlash == NULL)
    LSTRCPY(baseDirectory, "");
  else if (fwdSlash != NULL && fwdSlash > backSlash)
    *(fwdSlash + 1) = L('\0');
  else
    *(backSlash + 1) = L('\0');
  CHKLOG(rc, ESR_SessionSetLCHAR(L("parFile.baseDirectory"), baseDirectory));
  return ESR_SUCCESS;
CLEANUP:
  if (ESR_SessionExists(&exists) == ESR_SUCCESS)
  {
    if (exists)
      ESR_SessionDestroy();
  }
  return rc;
}

ESR_ReturnCode SR_SessionDestroy()
{
  ESR_ReturnCode rc;
  SR_EventLog* eventLog = NULL;
  
  ESR_SessionGetProperty(L("eventlog"), (void **)&eventLog, TYPES_SR_EVENTLOG);
  if (eventLog != NULL)
  {
    CHKLOG(rc, eventLog->destroy(eventLog));
    ESR_SessionRemoveProperty(L("eventlog")); /* failure is ok */
  }
  
  
  CHKLOG(rc, ESR_SessionDestroy());
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}
