/*---------------------------------------------------------------------------*
 *  SR_EventLog.h  *
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

#ifndef __SR_EventLog_H
#define __SR_EventLog_H



#include "SR_EventLogPrefix.h"
#include "ptypes.h"
#include "ESR_ReturnCode.h"


/**
 * @addtogroup SR_EventLogModule SR_EventLog API functions
 * Provides OSI logging.
 *
 * @{
 */

/**
 * OSI Event Log levels
 */

/**
 * Basic logging level.
 */
#define OSI_LOG_LEVEL_BASIC 0x01
/**
 * Log audio data.
 */
#define OSI_LOG_LEVEL_AUDIO 0x02
/**
 * Log ADDWORD commands.
 */
#define OSI_LOG_LEVEL_ADDWD 0x04

/**
 * Log a string token using the basic logging level.
 */
#define SR_EventLogToken_BASIC(log, loglevel, tokenName, value) \
  ((loglevel & OSI_LOG_LEVEL_BASIC) ? \
   log->token(log, tokenName, value) : ESR_SUCCESS ) \

/**
 * Log an integer token using the basic logging level.
 */
#define SR_EventLogTokenInt_BASIC(log, loglevel, tokenName, value)  \
  ((loglevel & OSI_LOG_LEVEL_BASIC) ? \
   log->tokenInt(log, tokenName, value) : ESR_SUCCESS ) \

/**
 * Log a uint16 token using the basic logging level.
 */
#define SR_EventLogTokenUint16_t_BASIC(log, loglevel, tokenName, value)  \
  ((loglevel & OSI_LOG_LEVEL_BASIC) ? \
   log->tokenUint16_t(log, tokenName, value) : ESR_SUCCESS ) \

/**
 * Log a size_t token using the basic logging level.
 */
#define SR_EventLogTokenSize_t_BASIC(log, loglevel, tokenName, value)  \
  ((loglevel & OSI_LOG_LEVEL_BASIC) ? \
   log->tokenSize_t(log, tokenName, value) : ESR_SUCCESS ) \

/**
 * Log a boolean token using the basic logging level.
 */
#define SR_EventLogTokenBool_BASIC(log, loglevel, tokenName, value)  \
  ((loglevel & OSI_LOG_LEVEL_BASIC) ? \
   log->tokenBool(log, tokenName, value) : ESR_SUCCESS ) \

/**
 * Log a float token using the basic logging level.
 */
#define SR_EventLogTokenFloat_BASIC(log, loglevel, tokenName, value)  \
  ((loglevel & OSI_LOG_LEVEL_BASIC) ? \
   log->tokenFloat(log, tokenName, value) : ESR_SUCCESS ) \

/**
 * Log an event using the basic logging level.
 */
#define SR_EventLogEvent_BASIC(log, loglevel, eventName) \
  ((loglevel & OSI_LOG_LEVEL_BASIC) ? \
   log->event(log, eventName) : ESR_SUCCESS ) \

/**
 * Log a string token using the audio logging level.
 */
#define SR_EventLogToken_AUDIO(log, loglevel, tokenName, value) \
  ((loglevel & OSI_LOG_LEVEL_AUDIO) ? \
   log->token(log, tokenName, value) : ESR_SUCCESS ) \

/**
 * Log an integer token using the audio logging level.
 */
#define SR_EventLogTokenInt_AUDIO(log, loglevel, tokenName, value)  \
  ((loglevel & OSI_LOG_LEVEL_AUDIO) ? \
   log->tokenInt(log, tokenName, value) : ESR_SUCCESS ) \

/**
 * Log an event using the audio logging level.
 */
#define SR_EventLogEvent_AUDIO(log, loglevel, eventName) \
  ((loglevel & OSI_LOG_LEVEL_AUDIO) ? \
   log->event(log, eventName) : ESR_SUCCESS ) \

/**
 * Represents a EventLog.
 */
typedef struct SR_EventLog_t
{
	/**
	 * Destroys a EventLog.
	 *
	 * @param self EventLog handle
	 */
	ESR_ReturnCode(*destroy)(struct SR_EventLog_t* self);

	/**
	 * Logs an OSI log token.
	 *
	 * @param self SR_EventLog handle
	 * @param token Token name
	 * @param value Token value
	 */
	ESR_ReturnCode(*token)(struct SR_EventLog_t* self, const LCHAR* token, const LCHAR *value);

	/**
	 * Logs an OSI log token.
	 *
	 * @param self SR_EventLog handle
	 * @param token Token name
	 * @param value Token value
	 */
	ESR_ReturnCode(*tokenInt)(struct SR_EventLog_t* self, const LCHAR* token, int value);

	/**
	 * Logs an OSI log token.
	 *
	 * @param self SR_EventLog handle
	 * @param token Token name
	 * @param value Token value
	 */
	ESR_ReturnCode(*tokenUint16_t)(struct SR_EventLog_t* self, const LCHAR* token, asr_uint16_t value);

	/**
	 * Logs an OSI log token.
	 *
	 * @param self SR_EventLog handle
	 * @param token Token name
	 * @param value Token value
	 */
	ESR_ReturnCode(*tokenSize_t)(struct SR_EventLog_t* self, const LCHAR* token, size_t value);

	/**
	 * Logs an OSI log token.
	 *
	 * @param self SR_EventLog handle
	 * @param token Token name
	 * @param value Token value
	 */
        ESR_ReturnCode(*tokenBool)(struct SR_EventLog_t* self, const LCHAR* token, ESR_BOOL value);

	/**
	 * Logs an OSI log token.
	 *
	 * @param self SR_EventLog handle
	 * @param token Token name
	 * @param value Token value
	 */
	ESR_ReturnCode(*tokenFloat)(struct SR_EventLog_t* self, const LCHAR* token, float value);

	/**
	 * Commits all previously accumulated log tokens.
	 *
	 * @param self SR_EventLog handle
	 * @param eventName Name of the event associated with the tokens
	 */
	ESR_ReturnCode(*event)(struct SR_EventLog_t* self, const LCHAR* eventName);


	/**
	 * Log the contents of the ESR_Session.
	 *
	 * @param self SR_EventLog handle
	 */
	ESR_ReturnCode(*eventSession)(struct SR_EventLog_t* self);

	/**
	 * Opens a new file for recording a waveform of audio. Filename is automatically generated. Opened file
	 * becomes the current one where data is written to until closed.
	 *
	 * @param self SR_EventLog handle
	 * @param audio_type String identifying type of audio e.g. L("audio/L16")
	 * @param sample_rate Sampling rate
	 * @param sample_size Size of sampling in bytes.
	 */
	ESR_ReturnCode(*audioOpen)(struct SR_EventLog_t* self, const LCHAR* audio_type, size_t sample_rate, size_t sample_size);

	/**
	 * Closes the current file.
	 *
	 * @param self SR_EventLog handle
	 * @param eventName Name of the event associated with the tokens
	 */
	ESR_ReturnCode(*audioClose)(struct SR_EventLog_t* self);

	/**
	 * Writes datat to the current audio file.
	 *
	 * @param self SR_EventLog handle
	 * @param buffer Buffer holding the data to write
	 * @param num_bytes The number of bytes in the buffer.
	 */
	ESR_ReturnCode(*audioWrite)(struct SR_EventLog_t* self, void* buffer, size_t num_bytes);

	/**
	 * Returns the filename of the current audio file used for logging.
	 *
	 * @param self SR_EventLog handle
	 * @param waveformFilename Name of the current audio file.
	 * @param len Length of buffer.
	 */
	ESR_ReturnCode(*audioGetFilename)(struct SR_EventLog_t* self, LCHAR* waveformFilename, size_t* len);
}
SR_EventLog;

/**
 * Create a new EventLog
 *
 * @param self EventLog handle
 */
SREC_EVENTLOG_API ESR_ReturnCode SR_EventLogCreate(SR_EventLog** self);

/**
 * Destroys a EventLog.
 *
 * @param self EventLog handle
 */
SREC_EVENTLOG_API ESR_ReturnCode SR_EventLogDestroy(SR_EventLog* self);

/**
 * Logs an OSI log token.
 *
 * @param self SR_EventLog handle
 * @param token Token name
 * @param value Token value
 */
SREC_EVENTLOG_API ESR_ReturnCode SR_EventLogToken(SR_EventLog* self, const LCHAR* token, const LCHAR *value);

/**
 * Logs an OSI log token.
 *
 * @param self SR_EventLog handle
 * @param token Token name
 * @param value Token value
 */
SREC_EVENTLOG_API ESR_ReturnCode SR_EventLogTokenInt(SR_EventLog* self, const LCHAR* token, int value);

/**
 * Logs an OSI log token.
 *
 * @param self SR_EventLog handle
 * @param token Token name
 * @param value Token value
 */
SREC_EVENTLOG_API ESR_ReturnCode SR_EventLogTokenUint16_t(SR_EventLog* self, const LCHAR* token, asr_uint16_t value);

/**
 * Logs an OSI log token.
 *
 * @param self SR_EventLog handle
 * @param token Token name
 * @param value Token value
 */
SREC_EVENTLOG_API ESR_ReturnCode SR_EventLogTokenSize_t(SR_EventLog* self, const LCHAR* token, size_t value);

/**
 * Logs an OSI log token.
 *
 * @param self SR_EventLog handle
 * @param token Token name
 * @param value Token value
 */
SREC_EVENTLOG_API ESR_ReturnCode SR_EventLogTokenBool(SR_EventLog* self, const LCHAR* token, ESR_BOOL value);

/**
 * Logs an OSI log token.
 *
 * @param self SR_EventLog handle
 * @param token Token name
 * @param value Token value
 */
SREC_EVENTLOG_API ESR_ReturnCode SR_EventLogTokenFloat(SR_EventLog* self, const LCHAR* token, float value);

/**
 * Log the contents of the ESR_Session.
 *
 * @param self SR_EventLog handle
 */
SREC_EVENTLOG_API ESR_ReturnCode SR_EventLogEventSession(SR_EventLog* self);

/**
 * Commits all previously accumulated log tokens.
 *
 * @param self SR_EventLog handle
 * @param eventName Name of the event associated with the tokens
 */
SREC_EVENTLOG_API ESR_ReturnCode SR_EventLogEvent(SR_EventLog* self, const LCHAR* eventName);

/**
 * Opens a new file for recording a waveform of audio. Filename is automatically generated. Opened file
 * becomes the current one where data is written to until closed.
 *
 * @param self SR_EventLog handle
 * @param audio_type String identifying type of audio e.g. L("audio/L16")
 * @param sample_rate Sampling rate
 * @param sample_size Size of sampling in bytes.
 */
SREC_EVENTLOG_API ESR_ReturnCode SR_EventLogAudioOpen(SR_EventLog* self, const LCHAR* audio_type, size_t sample_rate, size_t sample_size);

/**
 * Closes the current file.
 *
 * @param self SR_EventLog handle
 */
SREC_EVENTLOG_API ESR_ReturnCode SR_EventLogAudioClose(SR_EventLog* self);

/**
 * Writes datat to the current audio file.
 *
 * @param self SR_EventLog handle
 * @param buffer Buffer holding the data to write
 * @param num_bytes The number of bytes in the buffer.
 */
SREC_EVENTLOG_API ESR_ReturnCode SR_EventLogAudioWrite(SR_EventLog* self, void* buffer, size_t num_bytes);

/**
 * Returns the filename of the current audio file used for logging.
 *
 * @param self SR_EventLog handle
 * @param waveformFilename Name of the current audio file.
 * @param len Length of buffer.
 */
SREC_EVENTLOG_API ESR_ReturnCode SR_EventLogAudioGetFilename(SR_EventLog* self, LCHAR* waveformFilename, size_t* len);

/**
* @}
*/


#endif /* __SR_EventLog_H */
