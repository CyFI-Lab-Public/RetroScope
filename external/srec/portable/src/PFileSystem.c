/*---------------------------------------------------------------------------*
 *  PFileSystem.c  *
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

#include "ArrayList.h"
#include "LCHAR.h"
#include "PFileSystem.h"
#include "PFileSystemImpl.h"
#include "phashtable.h"
#include "plog.h"
#include "pmemory.h"


#define MTAG NULL

/**
 * Indicates if PFileSystem is initialized.
 */
extern ESR_BOOL PFileSystemCreated;

/**
 * [file path, PFileSystem*] mapping.
 */
extern PHashTable* PFileSystemPathMap;

/**
 * Current working directory.
 */
extern LCHAR PFileSystemCurrentDirectory[P_PATH_MAX];

PORTABLE_API ESR_ReturnCode PFileSystemCanonicalSlashes(LCHAR* path)
{
  ESR_ReturnCode rc;
  
  if (path == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  
  lstrtrim(path);
  CHKLOG(rc, lstrreplace(path, L('\\'), L('/')));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode PFileSystemLinearToPathTokens(const LCHAR* path, LCHAR*** tokenArray, size_t* count)
{
  ESR_ReturnCode rc;
  const LCHAR* beginning;
  const LCHAR* ending;
  LCHAR linear[P_PATH_MAX];
  ArrayList* arrayList = NULL;
  LCHAR* value = NULL;
  size_t i;
  
  if (path == NULL || tokenArray == NULL || count == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  LSTRCPY(linear, path);
  CHKLOG(rc, PFileSystemCanonicalSlashes(linear));
  CHKLOG(rc, ArrayListCreate(&arrayList));
  beginning = linear;
  while (ESR_TRUE)
  {
    ending = LSTRCHR(beginning, L('/'));
    if (ending == NULL)
      ending = beginning + LSTRLEN(beginning);
    value = MALLOC(sizeof(LCHAR) * (ending - beginning + 1 + 1), MTAG);
    if (value == NULL)
    {
      rc = ESR_OUT_OF_MEMORY;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
    LSTRNCPY(value, beginning, ending - beginning + 1);
    value[ending-beginning+1] = L('\0');
    CHKLOG(rc, lstrtrim(value));
    if (LSTRLEN(value) == 0)
    {
      FREE(value);
      value = NULL;
    }
    else
    {
      CHKLOG(rc, arrayList->add(arrayList, value));
      value = NULL;
    }
    if (*ending == 0)
      break;
    beginning = ending + 1;
  }
  
  /* Build static token array */
  CHKLOG(rc, arrayList->getSize(arrayList, count));
  *tokenArray = MALLOC(*count * sizeof(LCHAR*), MTAG);
  if (*tokenArray == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    goto CLEANUP;
  }
  for (i = 0; i < *count; ++i)
  {
    rc = arrayList->get(arrayList, i, (void**)(&(*tokenArray)[i]));
    if (rc != ESR_SUCCESS)
      goto CLEANUP;
  }
  rc = arrayList->destroy(arrayList);
  if (rc != ESR_SUCCESS)
    goto CLEANUP;
  return ESR_SUCCESS;
CLEANUP:
  FREE(value);
  if (arrayList != NULL)
  {
    ESR_ReturnCode cleanRC;
    
    cleanRC = arrayList->getSize(arrayList, count);
    if (cleanRC != ESR_SUCCESS)
      return rc;
    for (i = 0; i < *count; ++i)
    {
      cleanRC = arrayList->get(arrayList, 0, (void**)&value);
      if (cleanRC != ESR_SUCCESS)
        return rc;
      FREE(value);
      cleanRC = arrayList->remove(arrayList, 0);
      if (cleanRC != ESR_SUCCESS)
        return rc;
    }
    arrayList->destroy(arrayList);
  }
  return rc;
}

ESR_ReturnCode PFileSystemIsAbsolutePath(const LCHAR* path, ESR_BOOL* isAbsolute)
{
  LCHAR canonical[P_PATH_MAX];
  ESR_ReturnCode rc;
  
  if (isAbsolute == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  LSTRCPY(canonical, path);
  CHKLOG(rc, PFileSystemCanonicalSlashes(canonical));
  
  *isAbsolute = (canonical[0] == '/' ||
                 (LISALPHA(canonical[0]) && canonical[1] == ':' && canonical[2] == '/'));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode PFileSystemGetAbsolutePath(LCHAR* path, size_t* len)
{
  ESR_ReturnCode rc;
#define MAX_PATH_TOKENS 20
  LCHAR** tokens = NULL;
  size_t tokenLen = 0, i;
  ESR_BOOL isAbsolute;
  
  if (path == NULL || len == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  CHKLOG(rc, PFileSystemIsAbsolutePath(path, &isAbsolute));
  
  /* Prefix relative paths with the current working directory */
  if (!isAbsolute)
  {
    LCHAR cwd[P_PATH_MAX];
    size_t len2;
    
    len2 = P_PATH_MAX;
    CHKLOG(rc, PFileSystemGetcwd(cwd, &len2));
    len2 = *len;
    CHKLOG(rc, lstrinsert(cwd, path, 0, &len2));
  }
  
  CHKLOG(rc, PFileSystemCanonicalSlashes(path));
  tokenLen = MAX_PATH_TOKENS;
  CHKLOG(rc, PFileSystemLinearToPathTokens(path, &tokens, &tokenLen));
  
  LSTRCPY(path, L(""));
  for (i = 0; i < tokenLen; ++i)
  {
    if (LSTRCMP(tokens[i], L("../")) == 0)
    {
      size_t len2;
      
      len2 = *len;
      passert(path[LSTRLEN(path)-1] == L('/'));
      CHKLOG(rc, PFileSystemGetParentDirectory(path, &len2));
    }
    else if (LSTRCMP(tokens[i], L("./")) == 0)
    {
      if (i > 0)
      {
        /* do nothing */
      }
      else
      {
        LSTRCPY(path, L("./"));
      }
    }
    else
      LSTRCAT(path, tokens[i]);
    FREE(tokens[i]);
    tokens[i] = NULL;
  }
  FREE(tokens);
  return ESR_SUCCESS;
CLEANUP:
  if (tokens != NULL)
  {
    for (i = 0; i < tokenLen; ++i)
    {
      FREE(tokens[i]);
      tokens[i] = NULL;
    }
  }
  return rc;
}

ESR_ReturnCode PFileSystemIsCreated(ESR_BOOL* isCreated)
{
  ESR_ReturnCode rc;
  
  if (isCreated == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  *isCreated = PFileSystemCreated;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

/**
 * Given a path, returns the associated file-system and relative path.
 *
 * @param path Path to look up
 * @param fs [out] File-system which matches the path
 * @param relativePath [out] Relative path associated with match (should have length P_PATH_MAX)
 */
ESR_ReturnCode PFileSystemGetFS(const LCHAR* path, PFileSystem** fileSystem, LCHAR* relativePath)
{
  ESR_ReturnCode rc;
  PHashTableEntry* entry;
  LCHAR* key;
  PFileSystem* value;
  LCHAR* bestKey = NULL;
  PFileSystem* bestValue = NULL;
  
  CHKLOG(rc, PHashTableEntryGetFirst(PFileSystemPathMap, &entry));
  while (entry != NULL)
  {
    CHKLOG(rc, PHashTableEntryGetKeyValue(entry, (void **)&key, (void **)&value));
    if (LSTRSTR(path, key) == path)
    {
      /* File-system handles file path */
      
      if (bestKey == NULL || LSTRLEN(key) > LSTRLEN(bestKey))
      {
        /* Found a better match -- the new key is a subdirectory of the previous bestKey */
        bestKey = key;
        bestValue = value;
      }
    }
    CHKLOG(rc, PHashTableEntryAdvance(&entry));
  }
  if (bestKey == NULL)
  {
    rc = ESR_INVALID_STATE;
    PLogError(L("No file-system handles the specified path (%s)"), path);
    goto CLEANUP;
  }
  *fileSystem = bestValue;
  if (relativePath != NULL)
  {
    ESR_BOOL isAbsolute;
    
    CHKLOG(rc, PFileSystemIsAbsolutePath(path + LSTRLEN(bestKey), &isAbsolute));
    LSTRCPY(relativePath, L(""));
    if (!isAbsolute)
    {
      /* Make sure that the relative path is relative to the root of the file-system */
      LSTRCAT(relativePath, L("/"));
    }
    LSTRCAT(relativePath, path + LSTRLEN(bestKey));
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode PFileSystemCreatePFile(const LCHAR* filename, ESR_BOOL littleEndian, PFile** self)
{
  ESR_ReturnCode rc;
  LCHAR absolutePath[P_PATH_MAX];
  PFileSystem* fileSystem;
  size_t len;
  
  if (filename == NULL || self == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  LSTRCPY(absolutePath, filename);
  lstrtrim(absolutePath);
  len = P_PATH_MAX;
  CHKLOG(rc, PFileSystemGetAbsolutePath(absolutePath, &len));
  CHKLOG(rc, PFileSystemGetFS(absolutePath, &fileSystem, NULL));
  rc = fileSystem->createPFile(fileSystem, absolutePath, littleEndian, self);
  if (rc == ESR_NO_MATCH_ERROR)
    rc = ESR_OPEN_ERROR;
  if (rc != ESR_SUCCESS)
  {
    PLogError("%s, %s, %s", ESR_rc2str(rc), filename, absolutePath);
    goto CLEANUP;
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode PFileSystemMkdir(const LCHAR* path)
{
  ESR_ReturnCode rc;
  LCHAR absolutePath[P_PATH_MAX];
  PFileSystem* fileSystem;
  size_t len;
  
  if (path == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  LSTRCPY(absolutePath, path);
  lstrtrim(absolutePath);
  len = P_PATH_MAX;
  CHKLOG(rc, PFileSystemGetAbsolutePath(absolutePath, &len));
  CHKLOG(rc, PFileSystemGetFS(absolutePath, &fileSystem, NULL));
  CHK(rc, fileSystem->mkdir(fileSystem, absolutePath));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode PFileSystemGetcwd(LCHAR* path, size_t* len)
{
  ESR_ReturnCode rc;
  
  if (path == NULL || len == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  if (LSTRLEN(PFileSystemCurrentDirectory) + 1 > *len)
  {
    rc = ESR_BUFFER_OVERFLOW;
    *len = LSTRLEN(PFileSystemCurrentDirectory) + 1;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  LSTRCPY(path, PFileSystemCurrentDirectory);
  /* Check function postcondition */
  passert(path[LSTRLEN(path)-1] == L('/'));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode PFileSystemChdir(const LCHAR* path)
{
  ESR_ReturnCode rc;
  LCHAR absolutePath[P_PATH_MAX];
  PFileSystem* fileSystem;
  size_t len;
  
  if (path == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  LSTRCPY(absolutePath, path);
  /* Ensure path ends with '/' */
  if (absolutePath[LSTRLEN(absolutePath)-1] != L('/'))
    LSTRCAT(absolutePath, L("/"));
  lstrtrim(absolutePath);
  len = P_PATH_MAX;
  CHKLOG(rc, PFileSystemGetAbsolutePath(absolutePath, &len));
  CHKLOG(rc, PFileSystemGetFS(absolutePath, &fileSystem, NULL));
  rc = fileSystem->chdir(fileSystem, absolutePath);
  if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  if (absolutePath[LSTRLEN(absolutePath)-1] != L('/'))
    LSTRCAT(absolutePath, L("/"));
  LSTRCPY(PFileSystemCurrentDirectory, absolutePath);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

/**
 * PRECONDITION: Directory names must end with '/'
 */
ESR_ReturnCode PFileSystemGetParentDirectory(LCHAR* path, size_t* len)
{
  LCHAR* lastSlash;
  LCHAR clone[P_PATH_MAX];
  ESR_ReturnCode rc;
  size_t len2;
  
  if (path == NULL || len == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  LSTRCPY(clone, path);
  lstrtrim(clone);
  len2 = P_PATH_MAX;
  CHKLOG(rc, PFileSystemGetAbsolutePath(clone, &len2));
  
  /* 1.0 - Strip filename */
  lastSlash = LSTRRCHR(clone, L('/'));
  if (lastSlash == NULL)
  {
    /* path contains only a filename */
    LSTRCPY(path, L("../"));
    return ESR_SUCCESS;
  }
  else if (lastSlash < clone + LSTRLEN(clone) - 1)
  {
    
    *(lastSlash + 1) = L('\0');
    if (LSTRLEN(clone) > *len)
    {
      *len = LSTRLEN(clone);
      rc = ESR_BUFFER_OVERFLOW;
      goto CLEANUP;
    }
    LSTRCPY(path, clone);
    *len = LSTRLEN(path);
    return ESR_SUCCESS;
  }
  
  /* Get parent directory */
  if (lastSlash -clone + 2 == 3 && LSTRNCMP(clone, L("../"), 3) == 0)
  {
    LSTRCAT(clone, L("../"));
    if (LSTRLEN(clone) > *len)
    {
      *len = LSTRLEN(clone);
      rc = ESR_BUFFER_OVERFLOW;
      goto CLEANUP;
    }
    LSTRCPY(path, clone);
    *len = LSTRLEN(path);
    return ESR_SUCCESS;
  }
  if (lastSlash -clone + 1 == 2 && LSTRNCMP(clone, L("./"), 2) == 0)
  {
    if (LSTRLEN(L("../")) > *len)
    {
      *len = LSTRLEN(L("../"));
      rc = ESR_BUFFER_OVERFLOW;
      goto CLEANUP;
    }
    LSTRCPY(path, L("../"));
    *len = LSTRLEN(path);
    return ESR_SUCCESS;
  }
  else if (lastSlash == clone && LSTRNCMP(clone, L("/"), 1) == 0)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  *lastSlash = 0;
  lastSlash = LSTRRCHR(clone, L('/'));
  if (lastSlash != NULL)
  {
    *(lastSlash + 1) = 0;
    if (LSTRLEN(clone) > *len)
    {
      *len = LSTRLEN(clone);
      rc = ESR_BUFFER_OVERFLOW;
      goto CLEANUP;
    }
    LSTRCPY(path, clone);
    *len = LSTRLEN(path);
  }
  else
  {
    LSTRCPY(path, L(""));
    *len = 0;
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode PFileSystemIsDirectoryPath(const LCHAR* path, ESR_BOOL* isDirectory)
{
  LCHAR temp[P_PATH_MAX];
  ESR_ReturnCode rc;
  
  passert(isDirectory != NULL);
  LSTRCPY(temp, path);
  lstrtrim(temp);
  CHKLOG(rc, PFileSystemCanonicalSlashes(temp));
  *isDirectory = temp[LSTRLEN(temp)-1] == '/';
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}
