/*---------------------------------------------------------------------------*
 *  SR_EventLogImpl.h  *
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

#ifndef __SR_EventLogIMPL_H
#define __SR_EventLogIMPL_H



#include <stdlib.h>
#include "ESR_ReturnCode.h"
#include "ESR_SessionTypeListener.h"

#define TOK_BUFLEN (2*P_PATH_MAX)
#define MAX_LOG_RECORD (16*1024)

/**
 * EventLog implementation.
 */

typedef enum
{
  FILE_OK,
  SPACE_SETTING,
  UNINITIALIZED,
  NO_FILE,
  FILE_ERROR,
  SEEK_ERROR
} EventLogFileState;

typedef struct SR_EventLogImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  SR_EventLog Interface;
  
  LCHAR tokenBuf[MAX_LOG_RECORD];
  
  long serviceStartUserCPU;
  long serviceStartKernelCPU;
  
  EventLogFileState logFile_state;
  ESR_SessionTypeListener sessionListener;
  ESR_SessionTypeListenerPair sessionListenerPair;
  
  PFile* logFile;
  size_t logLevel;
  LCHAR logFilename[P_PATH_MAX];
  LCHAR waveformFilename[P_PATH_MAX];
  PFile* waveformFile;
  size_t waveformCounter;
  size_t waveform_num_bytes;
  size_t waveform_sample_rate;
  size_t waveform_bytes_per_sample;
}
SR_EventLogImpl;

SREC_EVENTLOG_API ESR_ReturnCode SR_EventLog_Destroy(SR_EventLog* self);

SREC_EVENTLOG_API ESR_ReturnCode SR_EventLog_Token(SR_EventLog* self, const LCHAR* token, const LCHAR *value);

SREC_EVENTLOG_API ESR_ReturnCode SR_EventLog_TokenInt(SR_EventLog* self, const LCHAR* token, int value);

SREC_EVENTLOG_API ESR_ReturnCode SR_EventLog_TokenUint16_t(SR_EventLog* self, const LCHAR* token, asr_uint16_t value);

SREC_EVENTLOG_API ESR_ReturnCode SR_EventLog_TokenSize_t(SR_EventLog* self, const LCHAR* token, size_t value);

SREC_EVENTLOG_API ESR_ReturnCode SR_EventLog_TokenBool(SR_EventLog* self, const LCHAR* token, ESR_BOOL value);

SREC_EVENTLOG_API ESR_ReturnCode SR_EventLog_TokenFloat(SR_EventLog* self, const LCHAR* token, float value);

SREC_EVENTLOG_API ESR_ReturnCode SR_EventLogEventSessionImpl(SR_EventLog* self);

SREC_EVENTLOG_API ESR_ReturnCode SR_EventLog_Event(SR_EventLog* self, const LCHAR* eventName);

SREC_EVENTLOG_API ESR_ReturnCode SR_EventLog_AudioOpen(SR_EventLog* self, const LCHAR* audio_type, size_t sample_rate, size_t sample_size);

SREC_EVENTLOG_API ESR_ReturnCode SR_EventLog_AudioClose(SR_EventLog* self);

SREC_EVENTLOG_API ESR_ReturnCode SR_EventLog_AudioWrite(SR_EventLog* self, void* buffer, size_t num_bytes);

SREC_EVENTLOG_API ESR_ReturnCode SR_EventLog_AudioGetFilename(SR_EventLog* self, LCHAR* waveformFilename, size_t* len);

#endif /* __SR_EventLogIMPL_H */
