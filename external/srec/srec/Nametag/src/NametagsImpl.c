/*---------------------------------------------------------------------------*
 *  NametagsImpl.c  *
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
#include "HashMap.h"
#include "LCHAR.h"
#include "plog.h"
#include "pmemory.h"
#include "SR_NametagImpl.h"
#include "SR_NametagsImpl.h"

#define MTAG NULL

ESR_ReturnCode SR_NametagsCreate(SR_Nametags** self)
{
  SR_NametagsImpl* impl;
  ESR_ReturnCode rc;

  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  impl = NEW(SR_NametagsImpl, MTAG);
  if (impl == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }

  impl->Interface.load = &SR_NametagsLoadImpl;
  impl->Interface.save = &SR_NametagsSaveImpl;
  impl->Interface.add = &SR_NametagsAddImpl;
  impl->Interface.remove = &SR_NametagsRemoveImpl;
  impl->Interface.getSize = &SR_NametagsGetSizeImpl;
  impl->Interface.get = &SR_NametagsGetImpl;
  impl->Interface.getAtIndex = &SR_NametagsGetAtIndexImpl;
  impl->Interface.contains = &SR_NametagsContainsImpl;
  impl->Interface.destroy = &SR_NametagsDestroyImpl;
  impl->value = NULL;
  impl->eventLog = NULL;

  CHKLOG(rc, HashMapCreate(&impl->value));
  CHKLOG(rc, ESR_SessionGetSize_t(L("SREC.Recognizer.osi_log_level"), &impl->logLevel));
  if (impl->logLevel > 0)
    CHKLOG(rc, ESR_SessionGetProperty(L("eventlog"), (void **)&impl->eventLog, TYPES_SR_EVENTLOG));
  CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->logLevel, L("pointer"), (int) self));
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->logLevel, L("SR_NametagsCreate")));
  *self = (SR_Nametags*) impl;
  return ESR_SUCCESS;
CLEANUP:
  impl->Interface.destroy(&impl->Interface);
  return rc;
}

ESR_ReturnCode SR_NametagsLoadImpl(SR_Nametags* self, const LCHAR* filename)
{
  SR_NametagsImpl* impl = (SR_NametagsImpl*) self;
  ESR_ReturnCode rc;
  PFile* file = NULL;
  LCHAR line[256];
  LCHAR* result = NULL;
  LCHAR* id;
  LCHAR* value;
  SR_Nametag* newNametag = NULL;
  SR_Nametag* oldNametag;
  HashMap* nametags = impl->value;
  size_t size, len, i;
  LCHAR devicePath[P_PATH_MAX];
  LCHAR number[MAX_UINT_DIGITS+1];
#define NAMETAGID_LENGTH 20
  /* strlen("token\0") == 6 */
#define TOKEN_LENGTH 6 + NAMETAGID_LENGTH
  LCHAR tokenName[TOKEN_LENGTH];

  if (filename == NULL)
  {
    rc = ESR_INVALID_STATE;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  size = P_PATH_MAX;
  CHKLOG(rc, ESR_SessionGetLCHAR(L("cmdline.nametagPath"), devicePath, &size));
  /* check if the filename has the path */
  if (LSTRNCMP(filename, devicePath, LSTRLEN(devicePath)) != 0)
    LSTRCAT(devicePath, filename);
  else
    LSTRCPY(devicePath, filename);
  file = pfopen ( devicePath, L("r"));
/*  CHKLOG(rc, PFileSystemCreatePFile(devicePath, ESR_TRUE, &file));
  CHKLOG(rc, file->open(file, L("r")));*/

  if ( file == NULL )
    goto CLEANUP;

  /* Flush collection */
  CHKLOG(rc, nametags->getSize(nametags, &size));
  for (i = 0; i < size; ++i)
  {
    CHKLOG(rc, nametags->getValueAtIndex(nametags, 0, (void **)&oldNametag));
    CHKLOG(rc, nametags->removeAtIndex(nametags, 0));
    CHKLOG(rc, oldNametag->destroy(oldNametag));
  }
  len = MAX_UINT_DIGITS + 1;
  CHKLOG(rc, lultostr(size, number, &len, 10));
  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("removeCount"), number));

  while (ESR_TRUE)
  {
    result = pfgets ( line, 256, file );
    if (result == NULL)
      break;
    if (LSTRLEN(line) == 255)
    {
      rc = ESR_BUFFER_OVERFLOW;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
    lstrtrim(line);

    /* Get the Nametag ID */
    id = line;

    /* Find next whitespace */
    for (value = id + 1; *value != L('\0') && !LISSPACE(*value); ++value);
    if (*value == L('\0'))
    {
      rc = ESR_INVALID_STATE;
      PLogError(L("%s: Cannot find end of Nametag id"), ESR_rc2str(rc));
      goto CLEANUP;
    }
    /* Delimit end of nametag ID */
    *value = L('\0');

    /* Find next non-whitespace */
    for (++value; *value != L('\0') && LISSPACE(*value); ++value);
    if (*value == L('\0'))
    {
      rc = ESR_INVALID_STATE;
      PLogError(L("%s: Cannot find Nametag value"), ESR_rc2str(rc));
      goto CLEANUP;
    }

    /* We now have both the Nametag ID and value */
	len = (LSTRLEN(value)+1) * sizeof(LCHAR) ;
    CHKLOG(rc, SR_NametagCreateFromValue(id, (const char*)value, len, &newNametag));
    /* Add Nametag to collection */
    CHKLOG(rc, impl->value->put(impl->value, id, newNametag));

    if (LSTRLEN(id) > NAMETAGID_LENGTH)
    {
      rc = ESR_BUFFER_OVERFLOW;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
    psprintf(tokenName, L("nametag[%s]"), id);
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, tokenName, value));
    newNametag = NULL;
  }
  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("filename"), filename));
  CHKLOG(rc, nametags->getSize(nametags, &size));
  len = MAX_UINT_DIGITS + 1;
  CHKLOG(rc, lultostr(size, number, &len, 10));
  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("addCount"), number));
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->logLevel, L("SR_NametagsLoad")));
  pfclose (file);
  return ESR_SUCCESS;
CLEANUP:
  if (file != NULL)
    pfclose (file);
  if (newNametag != NULL)
    newNametag->destroy(newNametag);
  return rc;
}

ESR_ReturnCode SR_NametagsSaveImpl(SR_Nametags* self, const LCHAR* filename)
{
  SR_NametagsImpl* impl = (SR_NametagsImpl*) self;
  ESR_ReturnCode rc;
  PFile* file = NULL;
  size_t size, i;
  HashMap* nametags = impl->value;
  SR_NametagImpl* nametag;
  LCHAR* id;
  size_t len;
  LCHAR devicePath[P_PATH_MAX];
#define NAMETAG_LENGTH 200
  LCHAR nametagBuffer[NAMETAG_LENGTH];
  LCHAR number[MAX_UINT_DIGITS+1];
#define NAMETAGID_LENGTH 20
  /* "token\0" == 6 */
#define TOKEN_LENGTH 6 + NAMETAGID_LENGTH
  LCHAR tokenName[TOKEN_LENGTH];
  size_t num_written;

  if (filename == NULL)
  {
    rc = ESR_INVALID_STATE;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  size = P_PATH_MAX;
  CHKLOG(rc, ESR_SessionGetLCHAR(L("cmdline.nametagPath"), devicePath, &size));

  if (LSTRNCMP(filename, devicePath, LSTRLEN(devicePath)) != 0)
    LSTRCAT(devicePath, filename);
  else
    LSTRCPY(devicePath, filename);

  file = pfopen ( devicePath, L("w"));
/*  CHKLOG(rc, PFileSystemCreatePFile(devicePath, ESR_TRUE, &file));
  CHKLOG(rc, file->open(file, L("w")));*/
  CHKLOG(rc, nametags->getSize(nametags, &size));

  if ( file == NULL )
    goto CLEANUP;

  for (i = 0; i < size; ++i)
  {
    CHKLOG(rc, nametags->getValueAtIndex(nametags, i, (void **)&nametag));

    CHKLOG(rc, nametag->Interface.getID(&nametag->Interface, &id));

    if (LSTRLEN(id) + 1 + LSTRLEN(nametag->value) + 2 >= NAMETAG_LENGTH)
    {
      rc = ESR_BUFFER_OVERFLOW;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
    psprintf(nametagBuffer, L("%s %s\n"), id, nametag->value);
    len = LSTRLEN(nametagBuffer);
/*    CHKLOG(rc, file->write(file, nametagBuffer, sizeof(LCHAR), &len));*/
    num_written = pfwrite ( nametagBuffer, sizeof ( LCHAR ), len, file );

    if ( num_written != len )
        goto CLEANUP;

    if (LSTRLEN(id) > NAMETAGID_LENGTH)
    {
      rc = ESR_BUFFER_OVERFLOW;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
    psprintf(tokenName, L("nametag[%s]"), id);
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, tokenName, nametag->value));
  }
  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("filename"), filename));
  len = MAX_UINT_DIGITS + 1;
  CHKLOG(rc, lultostr(size, (LCHAR*) &number, &len, 10));
  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("saveCount"), number));
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->logLevel, L("SR_NametagsSave")));
  pfclose (file);
  return ESR_SUCCESS;
CLEANUP:
  if (file != NULL)
    pfclose (file);
  return rc;
}

ESR_ReturnCode SR_NametagsAddImpl(SR_Nametags* self, SR_Nametag* nametag)
{
  SR_NametagsImpl* impl = (SR_NametagsImpl*) self;
  HashMap* nametags = impl->value;
  LCHAR* id;
  ESR_BOOL exists;
  ESR_ReturnCode rc;

  CHKLOG(rc, nametag->getID(nametag, &id));
  CHKLOG(rc, nametags->containsKey(nametags, id, &exists));
  if (exists)
  {
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("id"), id));
    rc = ESR_IDENTIFIER_COLLISION;
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("rc"), ESR_rc2str(rc)));
    CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->logLevel, L("SR_NametagsAdd")));
    rc = ESR_IDENTIFIER_COLLISION;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  CHKLOG(rc, nametags->put(nametags, id, nametag));

  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("id"), id));
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->logLevel, L("SR_NametagsAdd")));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_NametagsRemoveImpl(SR_Nametags* self, const LCHAR* id)
{
  SR_NametagsImpl* impl = (SR_NametagsImpl*) self;
  HashMap* nametags = impl->value;
  ESR_ReturnCode rc;

  CHKLOG(rc, nametags->remove(nametags, id));

  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("id"), id));
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->logLevel, L("SR_NametagsRemove")));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_NametagsGetSizeImpl(SR_Nametags* self, size_t* result)
{
  SR_NametagsImpl* impl = (SR_NametagsImpl*) self;
  HashMap* nametags = impl->value;
  ESR_ReturnCode rc;

  CHKLOG(rc, nametags->getSize(nametags, result));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_NametagsGetImpl(SR_Nametags* self, const LCHAR* id, SR_Nametag** nametag)
{
  SR_NametagsImpl* impl = (SR_NametagsImpl*) self;
  HashMap* nametags = impl->value;
  ESR_ReturnCode rc;

  CHKLOG(rc, nametags->get(nametags, id, (void **)nametag));

  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("id"), id));
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->logLevel, L("SR_NametagsGet")));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_NametagsGetAtIndexImpl(SR_Nametags* self, size_t index, SR_Nametag** nametag)
{
  SR_NametagsImpl* impl = (SR_NametagsImpl*) self;
  HashMap* nametags = impl->value;
  LCHAR* id;
  ESR_ReturnCode rc;

  CHKLOG(rc, nametags->getValueAtIndex(nametags, index, (void **)nametag));

  CHKLOG(rc, (*nametag)->getID(*nametag, &id));
  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("id"), id));
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->logLevel, L("SR_NametagsGetAtIndex")));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_NametagsContainsImpl(SR_Nametags* self, const LCHAR* id, ESR_BOOL* result)
{
  SR_NametagsImpl* impl = (SR_NametagsImpl*) self;
  HashMap* nametags = impl->value;
  ESR_ReturnCode rc;

  CHKLOG(rc, nametags->containsKey(nametags, id, result));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_NametagsDestroyImpl(SR_Nametags* self)
{
  SR_NametagsImpl* impl = (SR_NametagsImpl*) self;
  LCHAR number[MAX_UINT_DIGITS+1];
  ESR_ReturnCode rc;

  if (impl->value != NULL)
  {
    size_t size, i, len;
    HashMap* list = impl->value;
    SR_Nametag* nametag;

    CHKLOG(rc, list->getSize(list, &size));
    for (i = 0; i < size; ++i)
    {
      CHKLOG(rc, list->getValueAtIndex(list, 0, (void **)&nametag));
      CHKLOG(rc, list->removeAtIndex(list, 0));
      CHKLOG(rc, nametag->destroy(nametag));
    }

    len = MAX_UINT_DIGITS + 1;
    CHKLOG(rc, lultostr(size, (LCHAR*) &number, &len, 10));
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("removeCount"), number));
    CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->logLevel, L("SR_NametagsDestroy")));
    list->destroy(list);
    impl->value = NULL;
  }
  CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->logLevel, L("pointer"), (int) self));
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->logLevel, L("SR_NametagsDestroy")));
  impl->eventLog = NULL;
  FREE(self);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}
