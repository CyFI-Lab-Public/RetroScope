/*---------------------------------------------------------------------------*
 *  SR_RecognizerImpl.h  *
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

#ifndef __SR_RECOGNIZERIMPL_H
#define __SR_RECOGNIZERIMPL_H



#include "ArrayList.h"
#include "CircularBuffer.h"
#include "ESR_ReturnCode.h"
#include "ESR_SessionType.h"
#include "HashMap.h"
#include "SR_AcousticState.h"
#include "SR_Recognizer.h"
#include "SR_EventLog.h"
#include "ptimestamp.h"
#include "SR_Grammar.h"
#include "SR_Nametag.h"


#include "frontapi.h"
#include "simapi.h"

/***
 * Recognizer timings to be written to OSI logs
 */

typedef struct RecogLogTimings_t
{
  size_t BORT;    /* beginning of recognition time (millisec) */
  size_t DURS;    /* amount of speech processed (millisec) */
  size_t EORT;    /* end of recognition time (millisec) */
  size_t EOSD;    /* num of frames of speech before EOSS (frames) */
  size_t EOSS;    /* frame where end of speech signal occurred (frames) */
  size_t BOSS;    /* frame where start of speech signal occurred (frames) */
  size_t EOST;    /* instant where end of speech signal occurred (millisec) */
}
RecogLogTimings;


typedef enum
{
  /**
   * Initial state.
   */
  SR_RECOGNIZER_INTERNAL_BEGIN,
  /**
   * Timeout before beginning of speech.
   */
  SR_RECOGNIZER_INTERNAL_BOS_TIMEOUT,
  /**
   * Got end of input before beginning of speech.
   */
  SR_RECOGNIZER_INTERNAL_BOS_NO_MATCH,
  /**
   * Waiting for beginning of speech.
   */
  SR_RECOGNIZER_INTERNAL_BOS_DETECTION,
  /**
   * Waiting for end of speech or input.
   */
  SR_RECOGNIZER_INTERNAL_EOS_DETECTION,
  /**
   * Got end of input.
   */
  SR_RECOGNIZER_INTERNAL_EOI,
  /**
   * Detected end of speech (not due to end of input).
   */
  SR_RECOGNIZER_INTERNAL_EOS,
  /**
   * Final state.
   */
  SR_RECOGNIZER_INTERNAL_END,
} SR_RecognizerInternalStatus;


/**
 * Waveform Buffering stuff (for Nametags)
 **/

#define DEFAULT_WAVEFORM_BUFFER_MAX_SIZE       65  /* kBytes, will not grow */
#define DEFAULT_WAVEFORM_WINDBACK_FRAMES       50  /* will convert frames to bytes, will not grow */
#define DEFAULT_BOS_COMFORT_FRAMES              2
#define DEFAULT_EOS_COMFORT_FRAMES              2

typedef enum
{
  WAVEFORM_BUFFERING_OFF,             /* no buffering */
  WAVEFORM_BUFFERING_ON_CIRCULAR,     /* buffer but, do not grow past a certain upper bound, just loop & overwrite */
  WAVEFORM_BUFFERING_ON_LINEAR,       /* buffer and report overflow if necessary */
} waveform_buffering_state_t;

/* audio buffer which supports windback */

typedef struct WaveformBuffer_t
{
  void   *windback_buffer;        /* a temp buffer used for windback functionality (malloc only at init)*/
  size_t windback_buffer_sz;      /* sizeof buffer */
  waveform_buffering_state_t state; /* state of the buffer (considered only when writing to buffer) */
  CircularBuffer* cbuffer;        /* the actual buffer */
  size_t   overflow_count;        /* indicates the total number of bytes the overflowed */
  size_t read_size;
  size_t eos_comfort_frames;
  size_t bos_comfort_frames;
}
WaveformBuffer;


/* create the buffer */
ESR_ReturnCode WaveformBuffer_Create(WaveformBuffer** waveformBuffer, size_t frame_size);

/* reset the buffer... do not release memeory */
ESR_ReturnCode WaveformBuffer_Reset(WaveformBuffer* waveformBuffer);

/* get size */
ESR_ReturnCode WaveformBuffer_GetSize(WaveformBuffer* waveformBuffer, size_t* size);

/* write to buffer. will grow only if buffering state is set to allow it */
ESR_ReturnCode WaveformBuffer_Write(WaveformBuffer* waveformBuffer, void *data, size_t num_bytes);

/* read the whole buffer (starting from start offset, up to read_size) into a chunk allocated outside */
ESR_ReturnCode WaveformBuffer_Read(WaveformBuffer* waveformBuffer, void *data, size_t* num_bytes);

/* does the windback after bos detected */
ESR_ReturnCode WaveformBuffer_WindBack(WaveformBuffer* waveformBuffer, const size_t num_bytes);

/* sets the start offset and read_size at the end of recognition when endpointed transcription is known */
ESR_ReturnCode WaveformBuffer_ParseEndPointedResultAndTrim(WaveformBuffer* waveformBuffer, const LCHAR* end_pointed_result, const size_t bytes_per_frame);

/* free the memory allocated for blocks and for windback */
ESR_ReturnCode WaveformBuffer_Destroy(WaveformBuffer* waveformBuffer);

/* sets the state of buffer */
ESR_ReturnCode WaveformBuffer_SetBufferingState(WaveformBuffer* waveformBuffer, waveform_buffering_state_t state);

/* gets the state of buffer */
ESR_ReturnCode WaveformBuffer_GetBufferingState(WaveformBuffer* waveformBuffer, waveform_buffering_state_t* state);

/* skip the first few bytes (moves read pointer forward */
ESR_ReturnCode WaveformBuffer_Skip(WaveformBuffer* waveformBuffer, const size_t bytes);



/**
 * Speech recognizer.
 */
typedef struct SR_RecognizerImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  SR_Recognizer Interface;

  /**
   * Legacy CREC frontend.
   */
  CA_Frontend* frontend;
  /**
   * Legacy CREC Input waveform object.
   */
  CA_Wave* wavein;
  /**
   * Legacy CREC Utterance object.
   */
  CA_Utterance* utterance;
  /**
   * Legacy CREC confidence score calculator.
   */
  CA_ConfidenceScorer* confidenceScorer;
  /**
   * Legacy CREC recognizer.
   */
  CA_Recog* recognizer;
  /**
   * AcousticModels associated with Recognizer.
   */
  SR_AcousticModels* models;
  /**
  * Active Recognizer grammars.
  */
  HashMap* grammars;
  /**
   * Recognition result.
   */
  SR_RecognizerResult* result;
  /**
   * Recognizer parameters.
   */
  ESR_SessionType* parameters;
  /**
   * AcousticState associated with Recognizer.
   */
  SR_AcousticState* acousticState;
  /**
   * Total number of frames pushed by SR_RecognizerPutAudio().
   */
  size_t frames;
  /**
   * Number of processed frames.
   */
  size_t processed;
  /**
   * The number of frames up until the windback point (where -pau- starts).
   */
  size_t beginningOfSpeechOffset;
  /**
   * Internal recognizer state.
   */
  SR_RecognizerInternalStatus internalState;
  /**
   * Indicates if SR_RecognizerStart() was called.
   */
  ESR_BOOL isStarted;
  /**
   * Indicates if PutAudio() was called with the last audio frame.
   */
  ESR_BOOL gotLastFrame;
  /**
   * Audio buffer used by PutAudio().
   */
  CircularBuffer* buffer;
  /**
   * Temporary buffer used to transfer audio data (PutAudio).
   **/
  asr_int16_t *audioBuffer;
  /**
   * Recognizer sample rate.
   */
  size_t sampleRate;
  /**
   * Whether reconition has begun after begiing of speech detection
   */
  ESR_BOOL isRecognizing;
  /**
   * Max number of frames to process before BOS timeout
   */
  size_t utterance_timeout;
  /**
   * Locking function associated.
   */
  SR_RecognizerLockFunction lockFunction;
  /**
   * Locking function data.
   */
  void* lockData;

  /**
   * OSI logging level
   * if bit0 (OSI_LOG_LEVEL_BASIC) is set: do basic logging
   * if bit1 (OSI_LOG_LEVEL_AUDIO) is set: do audio waveform logging
   * if bit2 (OSI_LOG_LEVEL_ADDWD) is set: do dynamic grammar addword logging
   */
  size_t osi_log_level;

  /**
   * EventLog pointer
   */
  SR_EventLog* eventLog;
  /**
   * Data that should be logged in OSI
   */
  RecogLogTimings recogLogTimings;
  /**
   * Timestamp reference used for calculating timings
   */
  PTimeStamp timestamp;

  /**
   * Waveform buffer (for nametags) .
   */
  WaveformBuffer* waveformBuffer;

  /**
   * Reason for eos detected
   */
  LCHAR* eos_reason;

  /**
   * Indicates if signal quality variables have been initialized.
   */
  ESR_BOOL isSignalQualityInitialized;
  /**
   * True if signal is being clipped.
   */
  ESR_BOOL isSignalClipping;
  /**
   * True if DCOffset is present in signal.
   */
  ESR_BOOL isSignalDCOffset;
  /**
   * True if signal is noisy.
   */
  ESR_BOOL isSignalNoisy;
  /**
   * True if signal is too quiet.
   */
  ESR_BOOL isSignalTooQuiet;
  /**
   * True if signal contains too few samples.
   */
  ESR_BOOL isSignalTooFewSamples;
  /**
   * True if signal contains too many samples.
   */
  ESR_BOOL isSignalTooManySamples;

  /**
   * Number of bytes in a frame.
   **/
  size_t FRAME_SIZE;

  /**
   * If TRUE, beginning of speech detection is enabled.
   */
  ESR_BOOL gatedMode;

  /**
   * The minimum number of frames to sniff before beginning recognition.
   */
  size_t bgsniff;
  /**
   * Indicates if we've skipped holdOffPeriod frames at the beginning of the waveform.
   */
  ESR_BOOL holdOffPeriodSkipped;
}
SR_RecognizerImpl;

/**
 * Groups grammar with meta-data.
 */
typedef struct GrammarBag_t
{
  /**
   * Grammar object.
   */
  SR_Grammar* grammar;
  /**
   * Grammar weight.
   */
  unsigned int weight;
  /**
   * Grammar ID.
   */
  LCHAR* grammarID;
}
GrammarBag;


/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerStartImpl(SR_Recognizer* self);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerStopImpl(SR_Recognizer* self);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerDestroyImpl(SR_Recognizer* self);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerSetupImpl(SR_Recognizer* self);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerUnsetupImpl(SR_Recognizer* self);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsSetupImpl(SR_Recognizer* self, ESR_BOOL* isSetup);

/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerGetParameterImpl(SR_Recognizer* self, const LCHAR* key, LCHAR* value, size_t* len);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerGetSize_tParameterImpl(SR_Recognizer* self, const LCHAR* key, size_t* value);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerGetBoolParameterImpl(SR_Recognizer* self, const LCHAR* key, ESR_BOOL* value);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerSetParameterImpl(SR_Recognizer* self, const LCHAR* key, LCHAR* value);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerSetSize_tParameterImpl(SR_Recognizer* self, const LCHAR* key, size_t value);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerSetBoolParameterImpl(SR_Recognizer* self, const LCHAR* key, ESR_BOOL value);

/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerHasSetupRulesImpl(SR_Recognizer* self,
    ESR_BOOL* hasSetupRules);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerActivateRuleImpl(SR_Recognizer* self,
    SR_Grammar* grammar,
    const LCHAR* ruleName,
    unsigned int weight);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerDeactivateRuleImpl(SR_Recognizer* self,
    SR_Grammar* grammar,
    const LCHAR* ruleName);

/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerDeactivateAllRulesImpl(SR_Recognizer* self);

/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsActiveRuleImpl(SR_Recognizer* self,
    SR_Grammar* grammar,
    const LCHAR* ruleName,
    ESR_BOOL* isActiveRule);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerSetWordAdditionCeilingImpl(SR_Recognizer* self,
    SR_Grammar* grammar);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerCheckGrammarConsistencyImpl(SR_Recognizer* self,
    SR_Grammar* grammar,
    ESR_BOOL* isConsistent);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerGetModelsImpl(SR_Recognizer* self,
															  SR_AcousticModels** models);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerPutAudioImpl(SR_Recognizer* self,
    asr_int16_t* buffer,
    size_t* bufferSize,
    ESR_BOOL isLast);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerAdvanceImpl(SR_Recognizer* self,
    SR_RecognizerStatus* status,
    SR_RecognizerResultType* type,
    SR_RecognizerResult** result);

/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerClearAcousticStateImpl(SR_Recognizer* self);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLoadAcousticStateImpl(SR_Recognizer* self,
    const LCHAR* filename);

/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLoadUtteranceImpl(SR_Recognizer* self, const LCHAR* filename);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLoadWaveFileImpl(SR_Recognizer* self, const LCHAR* filename);

/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLogTokenImpl(SR_Recognizer* self, const LCHAR* token, const LCHAR* value);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLogTokenIntImpl(SR_Recognizer* self, const LCHAR* token, int value);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLogEventImpl(SR_Recognizer* self, const LCHAR* event);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLogSessionStartImpl(SR_Recognizer* self, const LCHAR* sessionName);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLogSessionEndImpl(SR_Recognizer* self);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLogWaveformDataImpl(SR_Recognizer* self,
    const LCHAR* waveformFilename,
    const LCHAR* transcription,
    const double bos,
    const double eos,
    ESR_BOOL isInvocab);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerSetLockFunctionImpl(SR_Recognizer *self, SR_RecognizerLockFunction function, void* data);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsSignalClippingImpl(SR_Recognizer* self, ESR_BOOL* isClipping);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsSignalDCOffsetImpl(SR_Recognizer* self, ESR_BOOL* isDCOffset);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsSignalNoisyImpl(SR_Recognizer* self, ESR_BOOL* isNoisy);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsSignalTooQuietImpl(SR_Recognizer* self, ESR_BOOL* isTooQuiet);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsSignalTooFewSamplesImpl(SR_Recognizer* self, ESR_BOOL* isTooFewSamples);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsSignalTooManySamplesImpl(SR_Recognizer* self, ESR_BOOL* isTooManySamples);

SREC_RECOGNIZER_API ESR_ReturnCode SR_Recognizer_Change_Sample_RateImpl ( SR_Recognizer *self, size_t new_sample_rate );

#endif /* __SR_RECOGNIZERIMPL_H */
