/*---------------------------------------------------------------------------*
 *  plog.c  *
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


#include <stdio.h>
#include <stdarg.h>
#include "PFileSystem.h"
#include "ptypes.h"
#include "plog.h"
#include "pmemory.h"
#include "pstdio.h"
#include "ptimestamp.h"
#include "passert.h"
#ifdef USE_STACKTRACE
#include "PStackTrace.h"
#endif

#ifdef USE_THREAD
#include "ptrd.h"
#include "pmutex.h"
#endif


#if defined (ANDROID)
#if defined (HAVE_ANDROID_OS)
#define LOG_TAG "Srec" 
#include <utils/Log.h>
#endif
#endif

#include "phashtable.h"

#define MTAG __FILE__

#define FILTER_MSG_1		"ESR_BUFFER_OVERFLOW"
#define FILTER_MSG_1_SIZE	( sizeof ( FILTER_MSG_1 ) - 1 )

#define FILTER_MSG_2		"ESR_NO_MATCH_ERROR"
#define FILTER_MSG_2_SIZE	( sizeof ( FILTER_MSG_2 ) - 1 )

static unsigned int GlogLevel = 0;
static PLogger *Glogger = NULL;
static LOG_OUTPUT_FORMAT GlogFormat = LOG_OUTPUT_FORMAT_MODULE_NAME |
                                      LOG_OUTPUT_FORMAT_DATE_TIME;
/**
 * Used to detect endless recursion where the PLog module calls itself.
 */
static ESR_BOOL locked = ESR_FALSE;
#ifdef USE_THREAD

static PtrdMutex* Gmutex = NULL;
#endif

typedef struct FileLogger_t
{
  PLogger base;
  PFile* fp;
}
FileLogger;

/**
 * Prints and formats a message to the log.
 *
 * @param self the PLogger.
 *
 * @param format the format string specifying the next arguments (a la
 * printf).
 *
 * @param args variable argument list.
 *
 * @return The number of bytes written to the PLogger or -1 if an error
 * occurs.
 */
static ESR_ReturnCode FileLoggerPrintf(PLogger *self, const LCHAR *format, ...)
{
  FileLogger *p = STATIC_CAST(self, FileLogger, base);
  ESR_ReturnCode rc;
  va_list args;
  
  va_start(args, format);
  rc = pvfprintf(p->fp, format, args);
  va_end(args);
  return rc;
}

static ESR_ReturnCode FileLoggerFlush(PLogger *self)
{
  FileLogger *p = STATIC_CAST(self, FileLogger, base);
  return pfflush(p->fp) == 0 ? ESR_SUCCESS : ESR_FATAL_ERROR;
}


/**
 * Destroys the logger.  This function is responsible to deallocate any
 * resources used by the logger.  In particular, if buffering is internally
 * used, it needs to flush the buffer.
 */
static void FileLoggerDestroy(PLogger *self)
{
  FileLogger *p = STATIC_CAST(self, FileLogger, base);
  pfflush(p->fp);
  
  if (p->fp != PSTDERR && p->fp != PSTDOUT)
    pfclose(p->fp);
  FREE(p);
}

static ESR_ReturnCode createPFileLogger(PFile* fp, PLogger** logger)
{
  FileLogger* fileLogger;
  
  if (fp == NULL)
    return ESR_INVALID_ARGUMENT;
  fileLogger = NEW(FileLogger, MTAG);
  if (fileLogger == NULL)
    return ESR_OUT_OF_MEMORY;
    
  fileLogger->base.printf = FileLoggerPrintf;
  fileLogger->base.flush = FileLoggerFlush;
  fileLogger->base.destroy = FileLoggerDestroy;
  fileLogger->fp = fp;
  
  *logger = &fileLogger->base;
  return ESR_SUCCESS;
}

/**
 * Initializes the LOG library.  This function must be called before any
 * logging can take place.
 *
 * @param logger The logger to be used to output the messages.  If NULL, then
 * logging goes to PSTDERR.  @param logLevel The level of logging requested.
 *
 * @return ESR_SUCCESS if success, anything else if an error occurs.
 *
 */
ESR_ReturnCode PLogInit(PLogger *logger, unsigned int logLevel)
{
  ESR_ReturnCode rc = ESR_SUCCESS;
  
  if (Glogger != NULL)
    return ESR_INVALID_STATE;
    
  GlogLevel = logLevel;
  
#ifdef USE_THREAD
  if ((rc = PtrdMutexCreate(&Gmutex)) != ESR_SUCCESS)
    return rc;
#endif
    
  if (logger != NULL)
    Glogger = logger;
  else
  {
    rc = createPFileLogger(PSTDERR, &Glogger);
    if (rc != ESR_SUCCESS)
      goto CLEANUP;
  }
  
  return rc;
CLEANUP:
#ifdef USE_THREAD
  if (Gmutex != NULL)
  {
    PtrdMutexDestroy(Gmutex);
    Gmutex = NULL;
  }
#endif
  return rc;
}

ESR_ReturnCode PLogIsInitialized(ESR_BOOL* isInit)
{
  if (isInit == NULL)
    return ESR_INVALID_STATE;
  *isInit = Glogger != NULL;
  return ESR_SUCCESS;
}

ESR_ReturnCode PLogIsLocked(ESR_BOOL* isLocked)
{
  if (isLocked == NULL)
    return ESR_INVALID_STATE;
  *isLocked = locked;
  return ESR_SUCCESS;
}

/**
 * Shutdowns the LOG library.  Once this function is called, no logging activity can be performed.
 * Also, the logger that was given to pLogInit is destroyed.
 *
 * @return ESR_SUCCESS if success, anything else if an error occurs.
 *
 */
ESR_ReturnCode PLogShutdown()
{
  ESR_ReturnCode rc = ESR_SUCCESS;
  
  if (Glogger == NULL)
    return ESR_INVALID_STATE;
    
#ifdef USE_THREAD
  if ((rc = PtrdMutexDestroy(Gmutex)) != ESR_SUCCESS)
    return rc;
  Gmutex = NULL;
#endif
  
  if (Glogger->flush != NULL)
    Glogger->flush(Glogger);
  Glogger->destroy(Glogger);
  Glogger = NULL;
  return rc;
}

ESR_ReturnCode PLogGetLevel(unsigned int *logLevel)
{
  if (Glogger == NULL)
    return ESR_INVALID_STATE;
  if (logLevel == NULL)
    return ESR_INVALID_ARGUMENT;
    
  *logLevel = GlogLevel;
  return ESR_SUCCESS;
}

ESR_ReturnCode PLogSetLevel(unsigned int logLevel)
{
  if (Glogger == NULL)
    return ESR_INVALID_STATE;
    
  GlogLevel = logLevel;
  return ESR_SUCCESS;
}

#define TIME_BUF_SIZE 24
#define TIME_FORMAT L("%Y/%m/%d %H:%M:%S")
#define PLOG_PANIC(x, rc) \
  do \
  { \
    { \
      pfprintf(PSTDERR, L("[%s:%d] %s failed with %s\n"), __FILE__, __LINE__, x, ESR_rc2str(rc)); \
      pfflush(PSTDERR); \
    } \
  } while (0)

static ESR_ReturnCode logIt(const LCHAR *format, va_list args, ESR_BOOL showStackTrace)
{
  ESR_ReturnCode rc = ESR_SUCCESS;
  ESR_ReturnCode flushRC = ESR_SUCCESS;
#ifdef USE_STACKTRACE
#define BUFFER_SIZE P_MAX_STACKTRACE + 2000
#else
#define BUFFER_SIZE 2000
#endif
  LCHAR buffer[BUFFER_SIZE] = L("");

  // TODO: Remove once logging subsystem supports "warn" level
  if (strstr(format, "ESR_BUFFER_OVERFLOW")==format)
    return ESR_SUCCESS;
  
#ifdef USE_STACKTRACE
  if (Glogger == NULL)
  {
    /*
     * There are three possible scenerios for why logging would occur although the PLog module
     * is uninitialized:
     *
     * 1) The code fails before PLog is initialized (perhaps in other portable components)
     * 2) The user forgets to initialize the PLog module
     * 3) The code fails after PLog is uninitialized (on shutdown)
     *
     * We do our best by logging any errors but this might result in the memory leak of
     * the PStackTrace module in case 3.
     */
    rc = PStackTraceCreate();
    if (rc != ESR_SUCCESS)
    {
      PLOG_PANIC(L("PStackTraceCreate"), rc);
      goto CLEANUP;
    }
  }
  else
  {
#ifdef USE_THREAD
    rc = PtrdMutexLock(Gmutex);
    if (rc != ESR_SUCCESS)
      return rc;
#endif
  }
  if (locked)
    return ESR_INVALID_STATE;
  locked = ESR_TRUE;
  
  if (GlogFormat & LOG_OUTPUT_FORMAT_DATE_TIME)
  {
    PTimeStamp now;
    struct tm* loctime;
    LCHAR timeStr[TIME_BUF_SIZE];
    size_t timeStrSize;
    
    PTimeStampSet(&now);
    loctime = localtime(&now.secs);
    timeStrSize = LSTRFTIME(timeStr, TIME_BUF_SIZE, TIME_FORMAT, loctime);
    passert(timeStrSize == (TIME_BUF_SIZE - 5));
    psprintf(timeStr + (TIME_BUF_SIZE - 5), ".%03hu", now.msecs);
    
    psprintf(buffer + LSTRLEN(buffer), L("%s|"), timeStr);
    passert(LSTRLEN(buffer) < BUFFER_SIZE);
  }
  
  if (GlogFormat & LOG_OUTPUT_FORMAT_THREAD_ID)
  {
    rc = psprintf(buffer + LSTRLEN(buffer), L("trd=%u|"), PtrdGetCurrentThreadId());
    passert(LSTRLEN(buffer) < BUFFER_SIZE);
  }
  
  if (GlogFormat & LOG_OUTPUT_FORMAT_MODULE_NAME && showStackTrace)
  {
    size_t len = P_MAX_STACKTRACE;
    LCHAR text[P_MAX_STACKTRACE];
    LCHAR* index;
    size_t i;
    
    rc = PStackTraceGetValue((LCHAR*) & text, &len);
    if (rc == ESR_SUCCESS)
    {
      for (i = 0; i < 2; ++i)
      {
        rc = PStackTracePopLevel((LCHAR*) & text);
        if (rc != ESR_SUCCESS)
        {
          PLOG_PANIC(L("PStackTracePopLevel"), rc);
          goto CLEANUP;
        }
      }
      index = text;
      while (index)
      {
        index = LSTRSTR(index, L(" at\n"));
        if (index != NULL)
        {
          *(index + 1) = L('<');
          *(index + 2) = L('-');
          *(index + 3) = L(' ');
        }
      }
    }
    else if (rc == ESR_NOT_SUPPORTED)
      LSTRCPY(text, L(""));
    else if (rc != ESR_SUCCESS)
    {
      PLOG_PANIC(L("PStackTraceGetValue"), rc);
      goto CLEANUP;
    }
    rc = psprintf(buffer + LSTRLEN(buffer), L("Module=%s|"), text);
    passert(LSTRLEN(buffer) < BUFFER_SIZE);
  }
  
  pvsprintf(buffer + LSTRLEN(buffer), format, args);
#else
  pvsprintf(buffer + LSTRLEN(buffer), format, args);
#endif
  passert(LSTRLEN(buffer) < BUFFER_SIZE);
  
  psprintf(buffer + LSTRLEN(buffer), L("\n"));
  passert(LSTRLEN(buffer) < BUFFER_SIZE);
  
  if (Glogger != NULL)
  {
    rc = Glogger->printf(Glogger, L("%s"), buffer);
    if (rc != ESR_SUCCESS)
      goto CLEANUP;
    flushRC = Glogger->flush(Glogger);
  }
  else
  {
    /* We need to log but the logging module is disabled or is locked so we output to stderr instead */
    {
      pfprintf(PSTDERR, L("%s"), buffer);
      pfflush(PSTDERR);
    }
  }
  locked = ESR_FALSE;
#ifdef USE_THREAD
  PtrdMutexUnlock(Gmutex);
#endif
  return flushRC;
CLEANUP:
  if (Glogger != NULL && Glogger->flush != NULL)
    flushRC = Glogger->flush(Glogger);
  locked = ESR_FALSE;
#ifdef USE_THREAD
  PtrdMutexUnlock(Gmutex);
#endif
  return rc != ESR_SUCCESS ? rc : flushRC;
}

/**
 * Conditionally PLogs a message.  The message is logged only if module is enabled.
 *
 * @param msg The message format specification (ala printf).
 * @return ESR_SUCCESS if success, anything else if an error occurs.
 */
ESR_ReturnCode PLogMessage(const char* msg, ...)
{
  va_list args;
  ESR_ReturnCode rc;
#if USE_STACKTRACE
  size_t depth;
#endif
  
#if defined (ANDROID)
#if defined (HAVE_ANDROID_OS)
  return ( ESR_SUCCESS );/* Get rid of this for phone device */
#endif
#endif

  if (Glogger == NULL)
    return ESR_INVALID_STATE;
#ifdef USE_STACKTRACE
  return ESR_SUCCESS;
  rc = PStackTraceGetDepth(&depth);
  
  if (rc == ESR_NOT_SUPPORTED)
  {
    /* Debugging symbols are missing */
    return ESR_SUCCESS;
  }
  else if (rc != ESR_SUCCESS)
  {
    pfprintf(PSTDERR, L("PStackTraceGetDepth"), ESR_rc2str(rc));
    goto CLEANUP;
  }
  
  /* Remove PLogMessage() from depth */
  --depth;
  if (GlogLevel < depth)
    return ESR_SUCCESS;
#endif
    
  va_start(args, msg);
  rc = logIt(msg, args, ESR_FALSE);
  va_end(args);
  return rc;
#if USE_STACKTRACE
CLEANUP:
  return rc;
#endif
}


/**
 * Unconditionally logs an error message.
 *
 * @param msg The message format specification (ala printf).
 * @return ESR_SUCCESS if success, anything else if an error occurs.
 */
ESR_ReturnCode PLogError(const char* msg, ...)
{
  va_list args;
  ESR_ReturnCode rc;
#if defined (ANDROID)
#if defined (HAVE_ANDROID_OS)
  char log_text [2048];
#endif
#endif

  va_start(args, msg);
#if defined (ANDROID)
#if defined (HAVE_ANDROID_OS)
  pvsprintf ( log_text, msg, args);
/* We need to disable some error messages because they are frequently not
 * errors but due to sloppy use of some functions. This prevents us from
 * getting flooded with bad error messages. SteveR
 */
  if ( ( strncmp ( log_text, FILTER_MSG_1, FILTER_MSG_1_SIZE ) != 0 ) &&
    ( strncmp ( log_text, FILTER_MSG_2, FILTER_MSG_2_SIZE ) != 0 ) )
  {
    ALOGE ("%s", log_text );
  }
  rc = 0;
#else
  rc = logIt(msg, args, ESR_TRUE);
#endif
#else
  rc = logIt(msg, args, ESR_TRUE);
#endif
  va_end(args);
  
  return rc;
}



ESR_ReturnCode PLogCreateFileLogger(PFile* file, PLogger **logger)
{
  if (logger == NULL || file == NULL)
    return ESR_INVALID_ARGUMENT;
    
  return createPFileLogger(file, logger);
}

/**
 * Creates a logger that logs to a circular file.
 *
 * @param filename The name of the file to be created.
 * @param maxsize The maximum number of bytes that the file may have.
 * @param logger logger handle receiving the created logger.
 */
ESR_ReturnCode PLogCreateCircularFileLogger(const LCHAR *filename,
    unsigned int maxsize,
    PLogger **logger)
{
  return ESR_NOT_SUPPORTED;
}


ESR_ReturnCode PLogSetFormat(LOG_OUTPUT_FORMAT format)
{
  GlogFormat = format;
  return ESR_SUCCESS;
}
