/*---------------------------------------------------------------------------*
 *  EventLogImpl.c  *
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

#ifdef ANDROID
#include <sys/time.h>
#endif

#include "errno.h"
#include "ESR_Session.h"
#include "ESR_SessionType.h"
#include "IntArrayList.h"
#include "LCHAR.h"
#include "PFileSystem.h"
#include "SR_EventLog.h"
#include "SR_EventLogImpl.h"
#include "SR_Session.h"
#include "plog.h"
#include "pmemory.h"
#include "ptimestamp.h"
#include "riff.h"
#include "pstdio.h"

#define MTAG NULL

#define localtime_r(clock, result) ((result)->tm_sec = 0, localtime(clock))


/*********************************************************************/
/* move this to portable lib */
#ifdef _WIN32
#include <windows.h>
#include <direct.h>
#else
#include <time.h> /* For CLK_TCK / CLOCKS_PER_SEC */
#include <sys/times.h>  /* for times() */
#ifndef CLK_TCK
#define CLK_TCK CLOCKS_PER_SEC
#endif
#endif

/**
 *
 * @param userTime milliseconds spent in user mode
 * @param kernelTime milliseconds spent in kernel mode
 */
ESR_ReturnCode PGetCPUTimes(long* userTime, long* kernelTime)
{
#ifdef _WIN32
  FILETIME dummy;
  FILETIME k, u;
  LARGE_INTEGER lk, lu;

  if ((! userTime) || (! kernelTime))
    return -1;

  if (GetThreadTimes(GetCurrentThread(), &dummy, &dummy, &k, &u) == ESR_FALSE)
    return -1;

  lk.LowPart  = k.dwLowDateTime;
  lk.HighPart = k.dwHighDateTime;
  *kernelTime = (long)(lk.QuadPart / 10000);

  lu.LowPart  = u.dwLowDateTime;
  lu.HighPart = u.dwHighDateTime;
  *userTime   = (long)(lu.QuadPart / 10000);


#elif !defined(__vxworks)
  struct tms timeBuf;

  if ((! userTime) || (! kernelTime))
    return -1;

  times(&timeBuf);
  *userTime = (long)timeBuf.tms_utime * 1000 / CLK_TCK;
  *kernelTime = (long)timeBuf.tms_stime * 1000 / CLK_TCK;
#endif
  return 0;
}
/*********************************************************************/

ESR_ReturnCode propertyChanged(ESR_SessionTypeListener* self, const LCHAR* name, const void* oldValue, const void* newValue, VariableTypes variableType, void* data)
{
  SR_EventLog* eventLog = (SR_EventLog*) data;
  IntArrayList* list;
  size_t len, i, lValueSize = 10;
  int iValue;
  LCHAR lValue[10];
  ESR_ReturnCode rc;

  switch (variableType)
  {
    case TYPES_INT:
      CHKLOG(rc, SR_EventLogTokenInt(eventLog, name, *((int*) newValue)));
      break;
    case TYPES_UINT16_T:
      CHKLOG(rc, SR_EventLogTokenUint16_t(eventLog, name, *((asr_uint16_t*) newValue)));
      break;
    case TYPES_SIZE_T:
      CHKLOG(rc, SR_EventLogTokenSize_t(eventLog, name, *((size_t*) newValue)));
      break;
    case TYPES_BOOL:
      CHKLOG(rc, SR_EventLogTokenBool(eventLog, name, *((ESR_BOOL*) newValue)));
      break;
    case TYPES_FLOAT:
      CHKLOG(rc, SR_EventLogTokenFloat(eventLog, name, *((float*) newValue)));
      break;
    case TYPES_PLCHAR:
      CHKLOG(rc, SR_EventLogToken(eventLog, name, (LCHAR*) newValue));
      break;

    case TYPES_INTARRAYLIST:
      CHKLOG(rc, ESR_SessionGetProperty(name, (void **)&list, TYPES_INTARRAYLIST));
      CHKLOG(rc, list->getSize(list, &len));
      CHKLOG(rc, SR_EventLogTokenInt(eventLog, name, len));
      for (i = 0; i < len; ++i)
      {
        CHKLOG(rc, list->get(list, i, &iValue));
        lValueSize = sizeof(lValue);
        CHKLOG(rc, litostr(i, lValue, &lValueSize, 10));
        CHKLOG(rc, SR_EventLogTokenInt(eventLog, lValue, iValue));
      }
      break;

    default:
      /* do nothing */
      ;
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

/**
 * Implementation copied from original Ian Fox implementation of ALTsleeLog
 */

ESR_ReturnCode SR_EventLogCreate(SR_EventLog** self)
{
  SR_EventLogImpl *impl, *any_existing_eventlog;
  ESR_ReturnCode rc;
  LCHAR* dataCaptureDir;
#define TIMESTAMP_LENGTH 18
  LCHAR timeStr[TIMESTAMP_LENGTH];
  struct tm *ct, ct_r;
  PTimeStamp timestamp;
#ifdef ANDROID
  struct timeval dir_stamp;
#endif

  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }

  any_existing_eventlog = NULL;
  rc = ESR_SessionGetProperty(L("eventlog"), (void **)&any_existing_eventlog, TYPES_SR_EVENTLOG);
  if (rc == ESR_SUCCESS && any_existing_eventlog)
  {
    *self = (SR_EventLog*)any_existing_eventlog;
    PLogError("eventlog was already created");
    return ESR_SUCCESS;
  }

  impl = NEW(SR_EventLogImpl, MTAG);
  if (impl == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }

  impl->Interface.destroy = &SR_EventLog_Destroy;
  impl->Interface.event = &SR_EventLog_Event;
  impl->Interface.token = &SR_EventLog_Token;
  impl->Interface.tokenInt = &SR_EventLog_TokenInt;
  impl->Interface.tokenUint16_t = &SR_EventLog_TokenUint16_t;
  impl->Interface.tokenSize_t = &SR_EventLog_TokenSize_t;
  impl->Interface.tokenBool = &SR_EventLog_TokenBool;
  impl->Interface.tokenFloat = &SR_EventLog_TokenFloat;
  impl->Interface.eventSession = &SR_EventLogEventSessionImpl;
  impl->Interface.audioOpen = &SR_EventLog_AudioOpen;
  impl->Interface.audioClose = &SR_EventLog_AudioClose;
  impl->Interface.audioWrite = &SR_EventLog_AudioWrite;
  impl->Interface.audioGetFilename = &SR_EventLog_AudioGetFilename;
  impl->sessionListenerPair.data = NULL;
  impl->sessionListenerPair.listener = &impl->sessionListener;
  impl->sessionListener.propertyChanged = &propertyChanged;
  impl->waveformCounter = 0;
  impl->logFile = NULL;
  impl->tokenBuf[0] = 0;
  impl->logFile_state = NO_FILE;
  impl->logLevel = 0;
  impl->waveformFile = NULL;
  LSTRCPY(impl->logFilename, L(""));

  CHKLOG(rc, ESR_SessionSetProperty(L("eventlog"), impl, TYPES_SR_EVENTLOG));
  rc = ESR_SessionGetSize_t(L("SREC.Recognizer.osi_log_level"), &impl->logLevel);
  if (rc == ESR_NO_MATCH_ERROR)
  {
    impl->logLevel = 7;
    CHKLOG(rc, ESR_SessionSetSize_t(L("SREC.Recognizer.osi_log_level"), impl->logLevel));
  }
  else if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }

  if (impl->logLevel > 0)
  {
    CHKLOG(rc, ESR_SessionGetProperty(L("cmdline.DataCaptureDirectory"), (void**) &dataCaptureDir, TYPES_PLCHAR));

    LSTRCPY(impl->logFilename, dataCaptureDir);
#ifdef ANDROID
/*
 * The existing functions did not work on the desired platform, hence this code for the device.
 */
    gettimeofday ( &dir_stamp, NULL );
    sprintf(timeStr, "%lu", (unsigned long) dir_stamp.tv_sec );
#else
    PTimeStampSet(&timestamp);
    ct = localtime_r(&timestamp.secs, &ct_r);
    sprintf(timeStr, "%04d%02d%02d%02d%02d%02d",
            ct->tm_year + 1900, ct->tm_mon + 1, ct->tm_mday, ct->tm_hour,
            ct->tm_min, ct->tm_sec);
#endif
    /* create capture directory if it doesn't already exist */
    rc = pf_make_dir (impl->logFilename);
    if (rc != ESR_SUCCESS && rc != ESR_IDENTIFIER_COLLISION)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }

    /* create the directory for today's log if it doesn't already exist */
    LSTRCAT(impl->logFilename, L("/"));
    LSTRCAT(impl->logFilename, timeStr);
/*
 * There used to be a while forever loop here with a break, but that caused an infinite loop
 * for the customer. With 1 second resolution, a pre-existing directory probably means a bug.
 * It's not worth trying to handle this.
 */
    rc = pf_make_dir (impl->logFilename);
    if (rc != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }

    /* create the log file */
    LSTRCAT(impl->logFilename, L("/SWIevent-"));
    LSTRCAT(impl->logFilename, timeStr);
    LSTRCAT(impl->logFilename, L(".log"));

    impl->logFile = pfopen ( impl->logFilename, L("w") );
/*    CHKLOG(rc, PFileSystemCreatePFile(impl->logFilename, ESR_TRUE, &impl->logFile));
    CHKLOG(rc, PFileOpen(impl->logFile, L("w")));*/

    if ( impl->logFile != NULL )
        impl->logFile_state = FILE_OK;
    else
        goto CLEANUP;
  }

  *self = (SR_EventLog*) impl;
  return ESR_SUCCESS;
CLEANUP:
  if (impl->logFile)
    pfclose (impl->logFile);
  return rc;
}

ESR_ReturnCode SR_EventLog_Destroy(SR_EventLog* self)
{
  SR_EventLogImpl* impl = (SR_EventLogImpl*) self;
  ESR_ReturnCode rc;

  if (impl->logFile_state == FILE_OK)
  {
    pfflush(impl->logFile);

    pfclose(impl->logFile);
    impl->logFile = NULL;
    impl->logFile_state = NO_FILE;
  }
  CHKLOG(rc, ESR_SessionRemoveProperty(L("eventlog")));
  FREE(impl);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}


static int quote_delimiter(LCHAR *record, size_t len)
{
  LCHAR qrecord[TOK_BUFLEN * 2];
  LCHAR *s, *d;

  s = record;
  d = qrecord;
  while (*s)
  {
    if (*s == '|')
      *d++ = '|';
    *d++ = *s++;
  }
  *d = L('\0');

  if (LSTRLEN(qrecord) >= len)
    return -1;

  LSTRCPY(record, qrecord);

  return 0;
}


ESR_ReturnCode SR_EventLog_Token(SR_EventLog* self, const LCHAR* token, const LCHAR *value)
{
  SR_EventLogImpl *impl = (SR_EventLogImpl *)self;
  LCHAR buf[TOK_BUFLEN];

  if (self == NULL || token == NULL || value == NULL)
    return ESR_INVALID_ARGUMENT;
  if (impl->logLevel == 0)
    return ESR_SUCCESS;

  /* token cannot contain '=' */
  if (LSTRCHR(token, L('=')) != NULL)
  {
    PLogError(L("SLEE: Token '%s' contains illegal '=' character"), token);
    return ESR_INVALID_ARGUMENT;
  }
  /* value cannot contain newline */
  if (value && LSTRCHR(value, L('\n')) != NULL)
  {
    PLogError(L("SLEE: Value for token '%s' contains illegal newline character"), token);
    return ESR_INVALID_ARGUMENT;
  }

  /* the number 2 in this if statement refers to the '=' and the '|'. */
  if (LSTRLEN(token) + LSTRLEN(value) + 2 +
      LSTRLEN(impl->tokenBuf) < MAX_LOG_RECORD)
  {
    if (LSTRLEN(token) + LSTRLEN(value) + 3 > TOK_BUFLEN)
    {
      PLogError(L("ESR_BUFFER_OVERFLOW: SLEE '|%s=%s'"), token, value);
      return ESR_BUFFER_OVERFLOW;
    }
    sprintf(buf, "%s=%s", token, value);
    if (quote_delimiter(buf, TOK_BUFLEN - 2) != 0)
    {
      PLogError(L("ESR_BUFFER_OVERFLOW: SLEE '|%s'"), buf);
      return ESR_BUFFER_OVERFLOW;
    }
    if (LSTRLEN(buf) + 1 + LSTRLEN(impl->tokenBuf) >= MAX_LOG_RECORD)
    {
      PLogError(L("ESR_BUFFER_OVERFLOW: SLEE '|%s'"), buf);
      return ESR_BUFFER_OVERFLOW;
    }
    strcat(impl->tokenBuf, "|");
    strcat(impl->tokenBuf, buf);
  }
  else
  {
    PLogError(L("ESR_BUFFER_OVERFLOW: SLEE '|%s=%s'"), token, value);
    return ESR_BUFFER_OVERFLOW;
  }
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_EventLog_TokenInt(SR_EventLog* self, const LCHAR* token, int value)
{
  SR_EventLogImpl *impl = (SR_EventLogImpl *)self;
  ESR_ReturnCode rc;
  LCHAR alpha[MAX_INT_DIGITS+1];
  size_t size = MAX_INT_DIGITS+1;

  if (impl->logLevel == 0)
    return ESR_SUCCESS;
  CHK(rc, litostr(value, alpha, &size, 10));
  return self->token(self, token, alpha);
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_EventLog_TokenUint16_t(SR_EventLog* self, const LCHAR* token, asr_uint16_t value)
{
  SR_EventLogImpl *impl = (SR_EventLogImpl *)self;
  ESR_ReturnCode rc;
  LCHAR alpha[MAX_INT_DIGITS+1];
  size_t size = MAX_INT_DIGITS+1;

  if (impl->logLevel == 0)
    return ESR_SUCCESS;
  CHK(rc, lultostr(value, alpha, &size, 10));
  return self->token(self, token, alpha);
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_EventLog_TokenSize_t(SR_EventLog* self, const LCHAR* token, size_t value)
{
  SR_EventLogImpl *impl = (SR_EventLogImpl *)self;
  ESR_ReturnCode rc;
  LCHAR alpha[MAX_UINT_DIGITS+1];
  size_t size = MAX_INT_DIGITS+1;

  if (impl->logLevel == 0)
    return ESR_SUCCESS;
  CHK(rc, lultostr(value, alpha, &size, 10));
  return self->token(self, token, alpha);
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_EventLog_TokenBool(SR_EventLog* self, const LCHAR* token, ESR_BOOL value)
{
  if (value)
    return self->token(self, token, L("TRUE"));
  else
    return self->token(self, token, L("FALSE"));
}

ESR_ReturnCode SR_EventLog_TokenFloat(SR_EventLog* self, const LCHAR* token, float value)
{
  SR_EventLogImpl *impl = (SR_EventLogImpl *)self;
  LCHAR alpha[MAX_INT_DIGITS+1];

  if (impl->logLevel == 0)
    return ESR_SUCCESS;
  sprintf(alpha, "%.2f", value);
  return self->token(self, token, alpha);
}

ESR_ReturnCode logIt(SR_EventLogImpl *impl, LCHAR* evtt, LCHAR* log_record, size_t* writtenSize)
{
  struct tm *ct, ct_r;
  LCHAR header[128], header2[64];
  PTimeStamp timestamp;
  const size_t sizeof_LCHAR = sizeof(LCHAR);
  const LCHAR* bar = "|";
  const LCHAR* nl = "\n";
  size_t i, len;
  const LCHAR* toWrite[5];

  toWrite[0] = header;
  toWrite[1] = bar;
  toWrite[2] = evtt;
  toWrite[3] = log_record;
  toWrite[4] = nl;

  ct = &ct_r;
  memset(ct, 0, sizeof(struct tm));

  switch (impl->logFile_state)
  {
    case FILE_OK:
    case SPACE_SETTING:
      PTimeStampSet(&timestamp);
      ct = localtime_r(&timestamp.secs, &ct_r);

      sprintf(header, "TIME=%04d%02d%02d%02d%02d%02d%03d",
              ct->tm_year + 1900, ct->tm_mon + 1, ct->tm_mday, ct->tm_hour,
              ct->tm_min, ct->tm_sec, timestamp.msecs);
      quote_delimiter(header, 128);

      sprintf(header2, "CHAN=%s", L("0")); /* default is channel 0 in ESR */
      quote_delimiter(header2, 128);

      LSTRCAT(header, bar);
      LSTRCAT(header, header2);

      /* write the header,bar,evtt, and record */
      for (*writtenSize = 0, i = 0; i < 5; i++)
      {
        len = LSTRLEN(toWrite[i]);
        if (pfwrite(toWrite[i], sizeof_LCHAR, len, impl->logFile))
          *writtenSize += len;
      }

      if (*writtenSize <= 0)
      {
        PLogError(L("Could not write to log file; logging halted"));
        impl->logFile_state = FILE_ERROR;
        break;
      }
      else
      {
        pfflush(impl->logFile);
      }

      break;

      /* If couldn't open file or error previously, just return */
    case UNINITIALIZED:
    case NO_FILE:
    case FILE_ERROR:
    case SEEK_ERROR:
    default:
      return ESR_INVALID_STATE;

  }

  return ESR_SUCCESS;
}


ESR_ReturnCode SR_EventLog_Event(SR_EventLog* self, const LCHAR* event)
{
  SR_EventLogImpl *impl = (SR_EventLogImpl *)self;
  ESR_ReturnCode rc;
  long userTime, kernelTime;
  long cpuTime;
  LCHAR buf[P_PATH_MAX];  /* allow space for EVNT=<blah> */
  size_t writtenSize;

  if (impl == NULL || event == NULL)
    return ESR_INVALID_ARGUMENT;
  if (impl->logLevel == 0)
    return ESR_SUCCESS;

  /* event cannot contain '=' */
  if (LSTRCHR(event, L('=')) != NULL)
  {
    PLogError(L("SLEE: SR_EventLog_Event: warning: "
                "SR_EventLog_Event failed.  Event '%s' contains illegal '=' "
                "character\n"), event);
    return ESR_INVALID_ARGUMENT;
  }

  CHKLOG(rc, PGetCPUTimes(&userTime, &kernelTime));

  if (!LSTRCMP(event, "SWIrcst"))
  {
    impl->serviceStartUserCPU = userTime;
    impl->serviceStartKernelCPU = kernelTime;
  }

  LSTRCPY(buf, event);
  if (quote_delimiter(buf, LSTRLEN(buf) + 1) != 0)
  {
    PLogError(L("ESR_BUFFER_OVERFLOW: '%s' exceeds 8 characters when '|' characters are quoted"), buf);
    return ESR_BUFFER_OVERFLOW;
  }

  /* if this event is an end-of-recognition event then check to see if we
     want to capture this waveform. */

  if (!LSTRCMP(event, "SWIrcnd"))
  {
    /* what to do ??? ALTsleeLogCheckWaveCapture(data); */
  }

  cpuTime = userTime - impl->serviceStartUserCPU;
  SR_EventLogTokenInt(self, L("UCPU"), cpuTime);
  cpuTime = kernelTime - impl->serviceStartKernelCPU;
  SR_EventLogTokenInt(self, L("SCPU"), cpuTime);


  sprintf(buf, "EVNT=%s", event);
  /* This call will set writtenSize to be some value >= 0 */
  logIt(impl, buf, impl->tokenBuf, &writtenSize);
  impl->tokenBuf[0] = 0;

  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode writeRiffHeader(SR_EventLog* self)
{
  SR_EventLogImpl *impl = (SR_EventLogImpl *)self;
  unsigned int total_buflen;
  int num_samples;
  unsigned int bytes_sec;

  RiffHeaderStruct header;

  num_samples = impl->waveform_num_bytes / impl->waveform_bytes_per_sample;

  strncpy(header.riffString, "RIFF", 4);
  strncpy(header.waveString, "WAVE", 4);
  strncpy(header.fmtString, "fmt ", 4);
  strncpy(header.dataString, "data", 4);

  total_buflen = sizeof(RiffHeaderStruct) + impl->waveform_num_bytes;
  bytes_sec = impl->waveform_sample_rate * impl->waveform_bytes_per_sample;

  header.riffChunkLength = total_buflen - sizeof(ChunkInfoStruct);
  header.fmtChunkLength = sizeof(WaveFormat);
  header.waveinfo.nFormatTag = WAVEFORMAT_PCM;  /* codec */
  header.waveinfo.nChannels = 1;
  header.waveinfo.nSamplesPerSec = impl->waveform_sample_rate;
  header.waveinfo.nAvgBytesPerSec = bytes_sec;
  header.waveinfo.nBlockAlign = (unsigned short) impl->waveform_bytes_per_sample;
  header.waveinfo.wBitsPerSample = (unsigned short)((bytes_sec * 8) / impl->waveform_sample_rate);
  header.dataLength = (unsigned int) impl->waveform_num_bytes;

  pfseek(impl->waveformFile, 0, SEEK_SET);

  /* RiffHeaderStruct */
  pfwrite(&header.riffString, 1, sizeof(header.riffString), impl->waveformFile);
  pfwrite(&header.riffChunkLength, sizeof(header.riffChunkLength), 1, impl->waveformFile);
  pfwrite(&header.waveString, 1, sizeof(header.waveString), impl->waveformFile);
  pfwrite(&header.fmtString, 1, sizeof(header.fmtString), impl->waveformFile);
  pfwrite(&header.fmtChunkLength, sizeof(header.fmtChunkLength), 1, impl->waveformFile);

  /* WaveFormat */
  pfwrite(&header.waveinfo.nFormatTag, sizeof(header.waveinfo.nFormatTag), 1, impl->waveformFile);
  pfwrite(&header.waveinfo.nChannels, sizeof(header.waveinfo.nChannels), 1, impl->waveformFile);
  pfwrite(&header.waveinfo.nSamplesPerSec, sizeof(header.waveinfo.nSamplesPerSec), 1, impl->waveformFile);
  pfwrite(&header.waveinfo.nAvgBytesPerSec, sizeof(header.waveinfo.nAvgBytesPerSec), 1, impl->waveformFile);
  pfwrite(&header.waveinfo.nBlockAlign, sizeof(header.waveinfo.nBlockAlign), 1, impl->waveformFile);
  pfwrite(&header.waveinfo.wBitsPerSample, sizeof(header.waveinfo.wBitsPerSample), 1, impl->waveformFile);

  /* Continuation of RiffHeaderStruct */
  pfwrite(&header.dataString, 1, sizeof(header.dataString), impl->waveformFile);
  pfwrite(&header.dataLength, sizeof(header.dataLength), 1, impl->waveformFile);

  return ESR_SUCCESS;
}

ESR_ReturnCode SR_EventLog_AudioOpen(SR_EventLog* self, const LCHAR* audio_type, size_t sample_rate, size_t sample_size)
{
  SR_EventLogImpl *impl = (SR_EventLogImpl*) self;
  LCHAR *p;

  LSTRCPY(impl->waveformFilename, impl->logFilename);
  p = LSTRSTR(impl->waveformFilename, L(".log"));
  if (p == NULL)
  {
    PLogError(L("ESR_OPEN_ERROR: %s"), impl->waveformFilename);
    return ESR_OPEN_ERROR;
  }
  *p = 0; /* trunc the name */

  psprintf(impl->waveformFilename, L("%s-%04lu.wav"), impl->waveformFilename, (unsigned long) ++impl->waveformCounter);

  impl->waveformFile = pfopen ( impl->waveformFilename, L("wb+") );

  if (impl->waveformFile == NULL)
  {
    PLogError(L("ESR_OPEN_ERROR: %s"), impl->waveformFilename);
    return ESR_OPEN_ERROR;
  }
  impl->waveform_num_bytes = 0;
  impl->waveform_bytes_per_sample = sample_size;
  impl->waveform_sample_rate = sample_rate;
  return writeRiffHeader(self);
}

ESR_ReturnCode SR_EventLog_AudioClose(SR_EventLog* self)
{
  SR_EventLogImpl *impl = (SR_EventLogImpl*) self;
  ESR_ReturnCode rc;

  /* impl->waveform_num_bytes has likely grown so we need to update the header before closing the file */
  CHKLOG(rc, writeRiffHeader(self));
  if (pfclose(impl->waveformFile))
  {
    rc = ESR_CLOSE_ERROR;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  impl->waveformFile = NULL;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_EventLog_AudioWrite(SR_EventLog* self, void* buffer, size_t num_bytes)
{
  SR_EventLogImpl *impl = (SR_EventLogImpl*) self;
  ESR_ReturnCode rc;
  size_t size = num_bytes / impl->waveform_bytes_per_sample;

  if (num_bytes > 0 && pfwrite(buffer, impl->waveform_bytes_per_sample, size, impl->waveformFile) != size)
  {
    LCHAR cwd[P_PATH_MAX];
    size_t len;

    len = P_PATH_MAX;
    CHKLOG(rc, pf_get_cwd (cwd, &len));
    PLogError(L("ESR_WRITE_ERROR: %s, cwd=%s"), impl->waveformFilename, cwd);
    return ESR_WRITE_ERROR;
  }

  impl->waveform_num_bytes += num_bytes;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_EventLog_AudioGetFilename(SR_EventLog* self, LCHAR* waveformFilename, size_t* len)
{
  SR_EventLogImpl *impl = (SR_EventLogImpl*) self;

  if (waveformFilename == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }

  if (*len < LSTRLEN(impl->waveformFilename))
  {
    PLogError(L("ESR_BUFFER_OVERFLOW"));
    return ESR_BUFFER_OVERFLOW;
  }

  LSTRCPY(waveformFilename, impl->waveformFilename);
  *len = LSTRLEN(waveformFilename);
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_EventLogEventSessionImpl(SR_EventLog* self)
{
  size_t size, i, j;
  ESR_ReturnCode rc;
  LCHAR* key;
  LCHAR lValue[256];
  int iValue;
  float fValue;
  size_t size_tValue;
  asr_uint16_t asr_uint16_tValue;
  ESR_BOOL bValue;
  IntArrayList* list;
  VariableTypes type;
  size_t lValueSize = 256;
  size_t len;

  CHKLOG(rc, ESR_SessionGetSize(&size));
  for (i = 0; i < size; ++i)
  {
    CHKLOG(rc, ESR_SessionGetKeyAtIndex(i, &key));
    CHKLOG(rc, ESR_SessionGetPropertyType(key, &type));

    switch (type)
    {
      case TYPES_INT:
        CHKLOG(rc, ESR_SessionGetInt(key, &iValue));
        CHKLOG(rc, SR_EventLogTokenInt(self, key, iValue));
        break;
      case TYPES_UINT16_T:
        CHKLOG(rc, ESR_SessionGetUint16_t(key, &asr_uint16_tValue));
        CHKLOG(rc, SR_EventLogTokenUint16_t(self, key, asr_uint16_tValue));
        break;
      case TYPES_SIZE_T:
        CHKLOG(rc, ESR_SessionGetSize_t(key, &size_tValue));
        CHKLOG(rc, SR_EventLogTokenSize_t(self, key, size_tValue));
        break;
      case TYPES_BOOL:
        CHKLOG(rc, ESR_SessionGetBool(key, &bValue));
        CHKLOG(rc, SR_EventLogTokenBool(self, key, bValue));
        break;
      case TYPES_FLOAT:
        CHKLOG(rc, ESR_SessionGetFloat(key, &fValue));
        CHKLOG(rc, SR_EventLogTokenFloat(self, key, fValue));
        break;
      case TYPES_PLCHAR:
        len = 256;
        CHKLOG(rc, ESR_SessionGetLCHAR(key, (LCHAR*) &lValue, &len));
        CHKLOG(rc, SR_EventLogToken(self, key, (LCHAR*) lValue));
        break;

      case TYPES_INTARRAYLIST:
        CHKLOG(rc, ESR_SessionGetProperty(key, (void **)&list, TYPES_INTARRAYLIST));
        CHKLOG(rc, list->getSize(list, &len));
        CHKLOG(rc, SR_EventLogTokenInt(self, key, len));
        for (j = 0; j < len; ++j)
        {
          CHKLOG(rc, list->get(list, j, &iValue));
          lValueSize = sizeof(lValue);
          CHKLOG(rc, litostr(j, lValue, &lValueSize, 10));
          CHKLOG(rc, SR_EventLogTokenInt(self, lValue, iValue));
        }
        break;

      default:
        /* do nothing */
        ;
    }
  }
  CHKLOG(rc, SR_EventLogEvent(self, L("ESRsession")));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}
