/*---------------------------------------------------------------------------*
 *  SR_Recognizer.h  *
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

#ifndef __SR_RECOGNIZER_H
#define __SR_RECOGNIZER_H



#include "ESR_ReturnCode.h"
#include "SR_RecognizerPrefix.h"
#include "SR_AcousticModels.h"
#include "SR_Grammar.h"
#include "SR_RecognizerResult.h"
#include "SR_Nametags.h"
#include "pstdio.h"
#include "ptypes.h"

/* forward decl needed because of SR_Recognizer.h <-> SR_Grammar.h include loop */
struct SR_Grammar_t;

/**
 * Recognizer status.
 */
typedef enum SR_RecognizerStatus_t
{
  /**
   * Reserved value.
   */
  SR_RECOGNIZER_EVENT_INVALID,
  /**
   * Recognizer could not find a match for the utterance.
   */
  SR_RECOGNIZER_EVENT_NO_MATCH,
  /**
   * Recognizer processed one frame of audio.
   */
  SR_RECOGNIZER_EVENT_INCOMPLETE,
  /**
   * Recognizer has just been started.
   */
  SR_RECOGNIZER_EVENT_STARTED,
  /**
   * Recognizer is stopped.
   */
  SR_RECOGNIZER_EVENT_STOPPED,
  /**
   * Beginning of speech detected.
   */
  SR_RECOGNIZER_EVENT_START_OF_VOICING,
  /**
   * End of speech detected.
   */
  SR_RECOGNIZER_EVENT_END_OF_VOICING,
  /**
   * Beginning of utterance occured too soon.
   */
  SR_RECOGNIZER_EVENT_SPOKE_TOO_SOON,
  /**
   * Recognition match detected.
   */
  SR_RECOGNIZER_EVENT_RECOGNITION_RESULT,
  /**
   * Timeout occured before beginning of utterance.
   */
  SR_RECOGNIZER_EVENT_START_OF_UTTERANCE_TIMEOUT,
  /**
   * Timeout occured before speech recognition could complete.
   */
  SR_RECOGNIZER_EVENT_RECOGNITION_TIMEOUT,
  /**
   * Not enough samples to process one frame.
   */
  SR_RECOGNIZER_EVENT_NEED_MORE_AUDIO,
  /**
   * More audio encountered than is allowed by 'swirec_max_speech_duration'.
   */
  SR_RECOGNIZER_EVENT_MAX_SPEECH,
} SR_RecognizerStatus;

/**
 * Type of RecognizerResult returned by SR_RecognizerAdvance().
 */
typedef enum SR_RecognizerResultType_t
{
  /**
   * Reserved value.
   */
  SR_RECOGNIZER_RESULT_TYPE_INVALID,
  /**
   * The result is complete from a full recognition of audio.
   */
  SR_RECOGNIZER_RESULT_TYPE_COMPLETE,
  /**
   * No results at this time.
   */
  SR_RECOGNIZER_RESULT_TYPE_NONE,
} SR_RecognizerResultType;

/**
 * SR_Utterance stubbed out.
 */
typedef void* SR_Utterance;

typedef enum
{
  ESR_LOCK,
  ESR_UNLOCK
} ESR_LOCKMODE;

/**
 * Function which will be invoked before accessing internal variables.
 */
typedef ESR_ReturnCode(*SR_RecognizerLockFunction)(ESR_LOCKMODE mode, void* data);

/**
 * @addtogroup SR_RecognizerModule SR_Recognizer API functions
 * Synchronous speech recognizer.
 *
 * @{
 */

/**
 * Synchronous speech recognizer.
 */
typedef struct SR_Recognizer_t
{
  /**
   * Starts recognition.
   *
   * @param self SR_Recognizer handle
  * @return ESR_INVALID_ARGUMENT if self is null, if no acoustic models have been associated with the recognizer,
  * if no grammars have been activated, or if the recognizer cannot be started for an unknown reason
   */
  ESR_ReturnCode(*start)(struct SR_Recognizer_t* self);
  /**
   * Stops the recognizer and invalidates the recognition result object.
   * Calling this function before the recognizer receives the last frame causes the recognition
   * to abort.
   *
   * @param self SR_Recognizer handle
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_STATE if an internal error has occured
   */
  ESR_ReturnCode(*stop)(struct SR_Recognizer_t* self);
  /**
   * Destroy a recognizer.
   *
   * @param self SR_Recognizer handle
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_STATE if an internal error has occured
   */
  ESR_ReturnCode(*destroy)(struct SR_Recognizer_t* self);
  /**
   * Associates a set of models with the recognizer.
   *
   * @param self SR_Recognizer handle
  * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*setup)(struct SR_Recognizer_t* self);
  /**
   * Unconfigures recognizer.
   *
   * @param self SR_Recognizer handle
  * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*unsetup)(struct SR_Recognizer_t* self);
  /**
   * Indicates whether recognizer is configured for use.
   *
   * @param self SR_Recognizer handle
   * @param isSetup True if recognizer is configured
  * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*isSetup)(struct SR_Recognizer_t* self, ESR_BOOL* isSetup);

  /**
   * Returns copy of LCHAR recognition parameter.
   *
   * @param self SR_Recognizer handle
   * @param key Parameter name
   * @param value [out] Used to hold the parameter value
   * @param len [in/out] Length of value argument. If the return code is ESR_BUFFER_OVERFLOW,
   *            the required length is returned in this variable.
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_RESULT_TYPE if the specified property is not of
  * type LCHAR*
   */
  ESR_ReturnCode(*getParameter)(struct SR_Recognizer_t* self, const LCHAR* key, LCHAR* value, size_t* len);
  /**
   * Return copy of size_t recognition parameter.
   *
   * @param self SR_Recognizer handle
   * @param key Parameter name
   * @param value [out] Used to hold the parameter value
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_RESULT_TYPE if the specified property is not of
  * type size_t
   */
  ESR_ReturnCode(*getSize_tParameter)(struct SR_Recognizer_t* self, const LCHAR* key, size_t* value);
  /**
   * Return copy of BOOL recognition parameter.
   *
   * @param self SR_Recognizer handle
   * @param key Parameter name
   * @param value [out] Used to hold the parameter value
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_RESULT_TYPE if the specified property is not of
  * type bool
   */
  ESR_ReturnCode(*getBoolParameter)(struct SR_Recognizer_t* self, const LCHAR* key, ESR_BOOL* value);
  /**
   * Sets recognition parameters.
   *
   * Key:             Description of associated value
   *
   * VoiceEnrollment       If "true", the next recognition will produce data required
   *                              for Nametag support (i.e. Aurora bitstream).
   *
   * @param self SR_Recognizer handle
   * @param key Parameter name
   * @param value Parameter value
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY is system is out of memory
   */
  ESR_ReturnCode(*setParameter)(struct SR_Recognizer_t* self, const LCHAR* key, LCHAR* value);
  /**
   * Sets recognition parameters.
   *
   * @param self SR_Recognizer handle
   * @param key Parameter name
   * @param value Parameter value
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY is system is out of memory
   */
  ESR_ReturnCode(*setSize_tParameter)(struct SR_Recognizer_t* self, const LCHAR* key, size_t value);
  /**
   * Sets recognition parameters.
   *
   * @param self SR_Recognizer handle
   * @param key Parameter name
   * @param value Parameter value
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY is system is out of memory
   */
  ESR_ReturnCode(*setBoolParameter)(struct SR_Recognizer_t* self, const LCHAR* key, ESR_BOOL value);

  /**
   * Recognizer may be set up with multiple Grammars and multiple rules. All grammars
   * must be unsetup before the recognizer can be destroy.
   * A pre-compiled Grammar should have undergone a model consistency check with the
   * recognizer prior to this call.
   *
   * @param self SR_Recognizer handle
   * @param grammar Grammar containing rule
   * @param ruleName Name of rule to associate with recognizer
   * @see SR_GrammarCheckModelConsistency
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode (*setupRule)(struct SR_Recognizer_t* self, struct SR_Grammar_t* grammar, const LCHAR* ruleName);
  /**
   * Indicates if Recognizer is configured with any rules within the specified Grammar.
   *
   * @param self SR_Recognizer handle
   * @param hasSetupRules True if the Recognizer is configured for the Grammar
  * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*hasSetupRules)(struct SR_Recognizer_t* self, ESR_BOOL* hasSetupRules);
  /**
   * Activates rule in recognizer.
   *
   * @param self SR_Recognizer handle
   * @param grammar Grammar containing rule
   * @param ruleName Name of rule
   * @param weight Relative weight to assign to self grammar vs. other activated grammars.
   *               Values: Integers 0-2^31.
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_STATE if no models are associated with the recognizer,
  * or if the rule could not be setup, or if the acoustic models could not be setup;
  * ESR_BUFFER_OVERFLOW if ruleName is too long
   */
  ESR_ReturnCode (*activateRule)(struct SR_Recognizer_t* self, struct SR_Grammar_t* grammar,
                                const LCHAR* ruleName, unsigned int weight);
  /**
   * Deactivates rule in recognizer.
   *
   * @param self SR_Recognizer handle
   * @param grammar Grammar containing rule
   * @param ruleName Name of root rule
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_NO_MATCH_ERROR if grammar is not activated
   */
  ESR_ReturnCode (*deactivateRule)(struct SR_Recognizer_t* self, struct SR_Grammar_t* grammar,
                                  const LCHAR* ruleName);

  /**
   * Deactivates all grammar rules in recognizer.
   *
   * @param self SR_Recognizer handle
  * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*deactivateAllRules)(struct SR_Recognizer_t* self);

  /**
   * Indicates if rule is active in recognizer.
   *
   * @param self SR_Recognizer handle
   * @param grammar Grammar containing rule
   * @param ruleName Name of rule
   * @param isActiveRule True if rule is active
  * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode (*isActiveRule)(struct SR_Recognizer_t* self, struct SR_Grammar_t* grammar,
                                const LCHAR* ruleName, ESR_BOOL* isActiveRule);
   /**
   * Configures the grammar for maximum amount of word addition
   *
   * @param self SR_Recognizer handle
   * @param grammar Grammar whose ceiling to be set
   * @return ESR_INVALID_ARGUMENT if self or grammar are null
   */
  ESR_ReturnCode (*setWordAdditionCeiling)(struct SR_Recognizer_t* self, struct SR_Grammar_t* grammar );
  /**
   * Ensure the model usage in a pre-compiled grammar is consistent with the models
   * that are associated with the Recognizer. You must first have called Recognizer_Setup().
   *
   * @param self SR_Recognizer handle
   * @param grammar Grammar to check against
   * @param isConsistent True if rule is consistent
  * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode (*checkGrammarConsistency)(struct SR_Recognizer_t* self, struct SR_Grammar_t* grammar,
      ESR_BOOL* isConsistent);

 /**
   * Ensure the model usage in a pre-compiled grammar is consistent with the models
   * that are associated with the Recognizer. You must first have called Recognizer_Setup().
   *
   * @param self SR_Recognizer handle
   * @param grammar Grammar to check against
   * @param isConsistent True if rule is consistent
  * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode (*getModels)(struct SR_Recognizer_t* self, SR_AcousticModels** pmodels);

  /**
   * Get audio into the recognizer.
   *
   * We decouple the Audio and frontend processing from the Recognizer processing via an
   * internal FIFO frame buffer (aka utterance buffer). This ensures that this call is at least
   * as fast as real time so that voicing events are not unduly delayed. The audio buffer size
   * must be at least one frame buffer's worth and some reasonable maximum size for synchronous
   * behaviour. This function may be called independently of Recognizer_Advance.
   *
   * @param self SR_Recognizer handle
   * @param buffer Buffer containing audio data
   * @param bufferSize [in/out] Size of buffer in samples. In case of a buffer overflow,
   *                            ESR_BUFFER_OVERFLOW is returned and this value holds the actual
   *                            amount of samples that were pushed.
   * @param isLast Indicates if the audio frame is the last one in this recognition
  * @return ESR_INVALID_ARGUMENT if self, buffer, or bufferSize are null; ESR_INVALID_STATE if the recognizer isn't
  * started, or the recognizer has already received the last frame; ESR_BUFFER_OVERFLOW if the recognizer buffer is
  * full
   */
  ESR_ReturnCode (*putAudio)(struct SR_Recognizer_t* self, asr_int16_t* buffer, size_t* bufferSize,
                            ESR_BOOL isLast);
  /**
   * Advance the recognizer by at least one utterance frame. The number of frames advanced
   * depends on the underlying definition. We anticipate that the recognizer will keep up with
   * the supplied audio buffers when waiting for voicing. After this point, the number of frames
   * may be one (for our default frame-advance mode) or it may be more if the synchronous nature
   * of this operation is not considered a problem. The recognizer may be advanced independently
   * of the Recognizer_PutAudio call. It is permissible to advance when there is no further data.
   * A stop condition could be an appropriate consequence.
   *
   * @param self Recognizer handle
   * @param status Resulting recognizer status
   * @param type Resulting recognition result type
   * @param result Resulting recognizer result
  * @return ESR_INVALID_ARGUMENT if self, status, or type are null; ESR_INVALID_STATE if an internal error occurs
   */
  ESR_ReturnCode(*advance)(struct SR_Recognizer_t* self, SR_RecognizerStatus* status,
                           SR_RecognizerResultType* type, SR_RecognizerResult** result);


  /**
   * Loads utterance from file.
   *
   * @param self SR_Recognizer handle
   * @param filename File to read from
  * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*loadUtterance)(struct SR_Recognizer_t* self, const LCHAR* filename);
  /**
   * Loads utterance from WAVE file.
   *
   * @param self SR_Recognizer handle
   * @param filename WAVE file to read from
  * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*loadWaveFile)(struct SR_Recognizer_t* self, const LCHAR* filename);

  /**
   * Log recognizer-related event token.
   *
   * @param self SR_Recognizer handle
   * @param event Token name
   * @param value Value to be logged
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*logToken)(struct SR_Recognizer_t* self, const LCHAR* token, const LCHAR* value);

  /**
   * Log recognizer-related event token integer.
   *
   * @param self SR_Recognizer handle
   * @param event Token name
   * @param value Value to be logged
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*logTokenInt)(struct SR_Recognizer_t* self, const LCHAR* token, int value);

  /**
   * Log recognizer-related event and dump all previously accumulated tokens since last event to
   * log.
   *
   * @param self SR_Recognizer handle
   * @param event Event name
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*logEvent)(struct SR_Recognizer_t* self, const LCHAR* event);

  /**
   * Log the beginning of a new log session. A log session contains zero or more recognitions (transactions)
   * and it is up to the application to decided when the session ends and a new one begins (e.g.
   * timeout, number of recognitions, etc.)
   *
   * @param self SR_Recognizer handle
   * @param sessionName Session name
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*logSessionStart)(struct SR_Recognizer_t* self, const LCHAR* sessionName);

  /**
   * Log the end of a log session.
   *
   * @param self SR_Recognizer handle
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*logSessionEnd)(struct SR_Recognizer_t* self);

  /**
   * Log data about a waveform obtained from a TCP file. This function is not called
   * when doing live recognition.
   *
   * @param self SR_Recognizer handle
   * @param waveformFilename Session name
   * @param transcription Transcription for the utterance
   * @param bos Beginning of speech (seconds)
   * @param eos End of speech (seconds)
   * @param isInvocab True if the transcription is accepted by the grammar, False otherwise
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*logWaveformData)(struct SR_Recognizer_t* self,
                                   const LCHAR* waveformFilename,
                                   const LCHAR* transcription,
                                   const double bos,
                                   const double eos,
                                   ESR_BOOL isInvocab);

  /**
   * Associates a locking function with the recognizer. This function is used to
   * protect internal data from multithreaded access.
   *
   * @param self SR_Recognizer handle
   * @param function Locking function
   * @param data Function data
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*setLockFunction)(struct SR_Recognizer_t *self, SR_RecognizerLockFunction function, void* data);
  /**
   * Indicates if signal is getting clipped.
   *
   * @param self SR_Recognizer handle
   * @param isClipping [out] Result value
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*isSignalClipping)(struct SR_Recognizer_t* self, ESR_BOOL* isClipping);
  /**
   * Indicates if signal has a DC-offset component.
   *
   * @param self SR_Recognizer handle
   * @param isDCOffset [out] Result value
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*isSignalDCOffset)(struct SR_Recognizer_t* self, ESR_BOOL* isDCOffset);
  /**
   * Indicates if signal is noisy.
   *
   * @param self SR_Recognizer handle
   * @param isNoisy [out] Result value
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*isSignalNoisy)(struct SR_Recognizer_t* self, ESR_BOOL* isNoisy);
  /**
   * Indicates if speech contained within the signal is too quiet.
   *
   * @param self SR_Recognizer handle
   * @param isTooQuiet [out] Result value
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*isSignalTooQuiet)(struct SR_Recognizer_t* self, ESR_BOOL* isTooQuiet);
  /**
   * Indicates if there are too few samples in the signal for a proper recognition.
   *
   * @param self SR_Recognizer handle
   * @param isTooFewSamples [out] Result value
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*isSignalTooFewSamples)(struct SR_Recognizer_t* self, ESR_BOOL* isTooFewSamples);
  /**
   * Indicates if there are too many samples in the signal for a proper recognition.
   *
   * @param self SR_Recognizer handle
   * @param isTooManySamples [out] Result value
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*isSignalTooManySamples)(struct SR_Recognizer_t* self, ESR_BOOL* isTooManySamples);
}
SR_Recognizer;

/**
 * Starts recognition.
 *
 * @param self SR_Recognizer handle
 * @return ESR_INVALID_ARGUMENT if self is null, if no acoustic models have been associated with the recognizer,
 * if no grammars have been activated, or if the recognizer cannot be started for an unknown reason
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerStart(SR_Recognizer* self);
/**
 * Stops the recognizer and invalidates the recognition result object.
 * Calling this function before the recognizer receives the last frame causes the recognition
 * to abort.
 *
 * @param self SR_Recognizer handle
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_STATE if an internal error has occured
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerStop(SR_Recognizer* self);

/**
 * @name Recognizer Setup operations
 *
 * @{
 */

/**
 * Create a new recognizer.
 *
 * @param self SR_Recognizer handle
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory;
 * ESR_INVALID_STATE if an internal error occurs
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerCreate(SR_Recognizer** self);
/**
 * Destroy a recognizer.
 *
 * @param self SR_Recognizer handle
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_STATE if an internal error has occured
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerDestroy(SR_Recognizer* self);
/**
 * Associates a set of models with the recognizer. All grammars must use models consistently.
 *
 * @param self SR_Recognizer handle
 * @see SR_RecognizerCheckGrammarConsistency
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerSetup(SR_Recognizer* self);
/**
 * Unconfigures recognizer.
 *
 * @param self SR_Recognizer handle
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerUnsetup(SR_Recognizer* self);
/**
 * Indicates whether recognizer is configured for use.
 *
 * @param self SR_Recognizer handle
 * @param isSetup True if recognizer is configured
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsSetup(SR_Recognizer* self, ESR_BOOL* isSetup);

/**
 * @}
 *
 * @name Recognizer parameter operations
 *
 * @{
 */

/**
 * Returns copy of LCHAR recognition parameter.
 *
 * @param self SR_Recognizer handle
 * @param key Parameter name
 * @param value [out] Used to hold the parameter value
 * @param len [in/out] Length of value argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_RESULT_TYPE if the specified property is not of
 * type LCHAR*
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerGetParameter(SR_Recognizer* self, const LCHAR* key, LCHAR* value, size_t* len);
/**
 * Return copy of size_t recognition parameter.
 *
 * @param self SR_Recognizer handle
 * @param key Parameter name
 * @param value Used to hold the parameter value
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_RESULT_TYPE if the specified property is not of
 * type size_t
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerGetSize_tParameter(SR_Recognizer* self, const LCHAR* key, size_t* value);
/**
 * Return copy of BOOL recognition parameter.
 *
 * @param self SR_Recognizer handle
 * @param key Parameter name
 * @param value Used to hold the parameter value
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_RESULT_TYPE if the specified property is not of
 * type bool
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerGetBoolParameter(SR_Recognizer* self, const LCHAR* key, ESR_BOOL* value);
/**
 * Sets LCHAR* recognition parameters.
 *
 * Key:             Description of associated value
 *
 * VoiceEnrollment       If "true", the next recognition will produce data required
 *                              for Nametag support (i.e. Aurora bitstream).
 *
 * @param self SR_Recognizer handle
 * @param key Parameter name
 * @param value Parameter value
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY is system is out of memory
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerSetParameter(SR_Recognizer* self, const LCHAR* key, LCHAR* value);
/**
 * Sets size_t recognition parameter.
 *
 * @param self SR_Recognizer handle
 * @param key Parameter name
 * @param value Parameter value
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY is system is out of memory
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerSetSize_tParameter(SR_Recognizer* self, const LCHAR* key, size_t value);
/**
 * Sets BOOL recognition parameter.
 *
 * @param self SR_Recognizer handle
 * @param key Parameter name
 * @param value Parameter value
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY is system is out of memory
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerSetBoolParameter(SR_Recognizer* self, const LCHAR* key, ESR_BOOL value);

/**
 * @}
 *
 * @name Recognizer rule Setup/Activation operations
 *
 * @{
 */

/**
 * Recognizer may be set up with multiple Grammars and multiple rules. All grammars
 * must be unsetup before the recognizer can be destroyed.
 * A pre-compiled Grammar should have undergone a model consistency check with the
 * recognizer prior to this call.
 *
 * @param self SR_Recognizer handle
 * @param grammar Grammar containing rule
 * @param ruleName Name of rule to associate with recognizer
 * @see SR_GrammarCheckModelConsistency
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerSetupRule(SR_Recognizer* self,
                                                          struct SR_Grammar_t* grammar,
    const LCHAR* ruleName);
/**
 * Indicates if Recognizer is configured with any rules within the specified Grammar.
 *
 * @param self SR_Recognizer handle
 * @param hasSetupRules True if the Recognizer is configured for the Grammar
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerHasSetupRules(SR_Recognizer* self,
    ESR_BOOL* hasSetupRules);
/**
 * Activates rule in recognizer.
 *
 * @param self SR_Recognizer handle
 * @param grammar Grammar containing rule
 * @param ruleName Name of rule
 * @param weight Relative weight to assign to self grammar vs. other activated grammars.
 *               Values: Integers 0-2^31.
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_STATE if no models are associated with the recognizer,
 * or if the rule could not be setup, or if the acoustic models could not be setup;
 * ESR_BUFFER_OVERFLOW if ruleName is too long
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerActivateRule(SR_Recognizer* self,
                                                             struct SR_Grammar_t* grammar,
    const LCHAR* ruleName,
    unsigned int weight);
/**
 * Deactivates rule in recognizer.
 *
 * @param self SR_Recognizer handle
 * @param grammar Grammar containing rule
 * @param ruleName Name of rule
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_NO_MATCH_ERROR if grammar is not activated
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerDeactivateRule(SR_Recognizer* self,
                                                               struct SR_Grammar_t* grammar,
    const LCHAR* ruleName);

/**
 * Deactivates all grammar rule in recognizer.
 *
 * @param self SR_Recognizer handle
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerDeactivateAllRules(SR_Recognizer* self);

/**
 * Indicates if rule is active in recognizer.
 *
 * @param self SR_Recognizer handle
 * @param grammar Grammar containing rule
 * @param ruleName Name of rule
 * @param isActiveRule True if rule is active
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsActiveRule(SR_Recognizer* self,
                                                             struct SR_Grammar_t* grammar,
    const LCHAR* ruleName,
    ESR_BOOL* isActiveRule);
/**
 * Ensure the model usage in a pre-compiled grammar is consistent with the models
 * that are associated with the Recognizer. You must first have called Recognizer_Setup().
 *
 * @param self SR_Recognizer handle
 * @param grammar Grammar to check against
 * @param isConsistent True if rule is consistent
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerCheckGrammarConsistency(SR_Recognizer* self,
                                                                        struct SR_Grammar_t* grammar,
    ESR_BOOL* isConsistent);
/**
 * @}
 *
 * @name Recognizer Advance operations
 *
 * @{
 */

/**
 * Get audio into the recognizer.
 *
 * We decouple the Audio and frontend processing from the Recognizer processing via an
 * internal FIFO frame buffer (aka utterance buffer). This ensures that this call is at least
 * as fast as real time so that voicing events are not unduly delayed. The audio buffer size
 * must be at least one frame buffer's worth and some reasonable maximum size for synchronous
 * behaviour. This function may be called independently of Recognizer_Advance.
 *
 * @param self SR_Recognizer handle
 * @param buffer Buffer containing audio data
 * @param bufferSize [in/out] Size of buffer in samples. In case of a buffer overflow,
 *                            ESR_BUFFER_OVERFLOW is returned and this value holds the actual
 *                            amount of samples that were pushed.
 * @param isLast Indicates if the audio frame is the last one in this recognition
 * @return ESR_INVALID_ARGUMENT if self, buffer, or bufferSize are null; ESR_INVALID_STATE if the recognizer isn't
 * started, or the recognizer has already received the last frame; ESR_BUFFER_OVERFLOW if the recognizer buffer is
 * full
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerPutAudio(SR_Recognizer* self, asr_int16_t* buffer,
    size_t* bufferSize, ESR_BOOL isLast);
/**
 * Advance the recognizer by at least one utterance frame. The number of frames advanced
 * depends on the underlying definition. We anticipate that the recognizer will keep up with
 * the supplied audio buffers when waiting for voicing. After this point, the number of frames
 * may be one (for our default frame-advance mode) or it may be more if the synchronous nature
 * of this operation is not considered a problem. The recognizer may be advanced independently
 * of the Recognizer_PutAudio call. It is permissible to advance when there is no further data.
 * A stop condition could be an appropriate consequence.
 *
 * @param self Recognizer handle
 * @param status Resulting recognizer status
 * @param type Resulting recognition result type
 * @param result Resulting recognizer result
 * @return ESR_INVALID_ARGUMENT if self, status, or type are null; ESR_INVALID_STATE if an internal error occurs
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerAdvance(SR_Recognizer* self,
    SR_RecognizerStatus* status,
    SR_RecognizerResultType* type,
    SR_RecognizerResult** result);
/**
 * @}
 */

/**
 * Log recognizer-related event token.
 *
 * @param self SR_Recognizer handle
 * @param token Token name
 * @param value Value to be logged
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLogToken(SR_Recognizer* self, const LCHAR* token, const LCHAR* value);

/**
 * Log recognizer-related event token integer.
 *
 * @param self SR_Recognizer handle
 * @param token Token name
 * @param value Value to be logged
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLogTokenInt(SR_Recognizer* self, const LCHAR* token, int value);

/**
 * Log recognizer-related event and dump all previously accumulated tokens since last event to
 * log.
 *
 * @param self SR_Recognizer handle
 * @param event Event name
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLogEvent(SR_Recognizer* self, const LCHAR* event);

/**
 * Log the beginning of a new log session. A log session contains zero or more recognitions (transactions)
 * and it is up to the application to decided when the session ends and a new one begins (e.g.
 * timeout, number of recognitions, etc.)
 *
 * @param self SR_Recognizer handle
 * @param sessionName Session name
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLogSessionStart(SR_Recognizer* self, const LCHAR* sessionName);

/**
 * Log the end of a log session.
 *
 * @param self SR_Recognizer handle
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLogSessionEnd(SR_Recognizer* self);

/**
 * Log data about a waveform obtained from a TCP file. This function is not called
 * when doing live recognition.
 *
 * @param self SR_Recognizer handle
 * @param waveformFilename Session name
 * @param transcription Transcription for the utterance
 * @param bos Beginning of speech (seconds)
 * @param eos End of speech (seconds)
 * @param isInvocab True if the transcription is accepted by the grammar, False otherwise
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLogWaveformData(SR_Recognizer* self,
    const LCHAR* waveformFilename,
    const LCHAR* transcription,
    const double bos,
    const double eos,
    ESR_BOOL isInvocab);


/**
 * Loads utterance from file.
 *
 * @param self SR_Recognizer handle
 * @param filename File to read from
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLoadUtterance(SR_Recognizer* self, const LCHAR* filename);
/**
 * Loads utterance from WAVE file.
 *
 * @param self SR_Recognizer handle
 * @param filename WAVE file to read from
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerLoadWaveFile(SR_Recognizer* self, const LCHAR* filename);

/**
 * Associates a locking function with the recognizer. This function is used to
 * protect internal data from multithreaded access.
 *
 * @param self SR_Recognizer handle
 * @param function Locking function
 * @param data Function data
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerSetLockFunction(SR_Recognizer* self,
    SR_RecognizerLockFunction function,
    void* data);

/**
 *
 * @name Signal quality metrics
 *
 * @{
 */

/**
 * Indicates if signal is getting clipped.
 *
 * @param self SR_Recognizer handle
 * @param isClipping [out] Result value
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsSignalClipping(SR_Recognizer* self, ESR_BOOL* isClipping);
/**
 * Indicates if signal has a DC-offset component.
 *
 * @param self SR_Recognizer handle
 * @param isDCOffset [out] Result value
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsSignalDCOffset(SR_Recognizer* self, ESR_BOOL* isDCOffset);
/**
 * Indicates if signal is noisy.
 *
 * @param self SR_Recognizer handle
 * @param isNoisy [out] Result value
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsSignalNoisy(SR_Recognizer* self, ESR_BOOL* isNoisy);
/**
 * Indicates if speech contained within the signal is too quiet.
 *
 * @param self SR_Recognizer handle
 * @param isTooQuiet [out] Result value
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsSignalTooQuiet(SR_Recognizer* self, ESR_BOOL* isTooQuiet);
/**
 * Indicates if there are too few samples in the signal for a proper recognition.
 *
 * @param self SR_Recognizer handle
 * @param isTooFewSamples [out] Result value
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsSignalTooFewSamples(SR_Recognizer* self, ESR_BOOL* isTooFewSamples);
/**
 * Indicates if there are too many samples in the signal for a proper recognition.
 *
 * @param self SR_Recognizer handle
 * @param isTooManySamples [out] Result value
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerIsSignalTooManySamples(SR_Recognizer* self, ESR_BOOL* isTooManySamples);

/**
 * Changes the sample rate of audio.
 *
 * @param self SR_Recognizer handle
 * @param new_sample_rate [in] New Sample Rate
 * @return ESR_ReturnCode if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_Recognizer_Change_Sample_Rate ( SR_Recognizer *self, size_t new_sample_rate );

/**
 * @}
 */

/**
 * @}
 */


#endif /* __SR_RECOGNIZER_H */
