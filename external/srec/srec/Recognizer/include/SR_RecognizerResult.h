/*---------------------------------------------------------------------------*
 *  SR_RecognizerResult.h  *
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

#ifndef __SR_RECOGNIZERRESULT_H
#define __SR_RECOGNIZERRESULT_H



#include <stddef.h>
#include "ESR_Locale.h"
#include "ESR_ReturnCode.h"
#include "SR_RecognizerPrefix.h"
#include "ptypes.h"

/**
 * @addtogroup SR_RecognizerResultModule SR_RecognizerResult API functions
 * Recognition result.
 *
 * @{
 */

/**
 * Recognition result.
 */
typedef struct SR_RecognizerResult_t
{
  /**
   * Returns the endpointed waveform that was used for recognition. This returns a read-only buffer,
   * and may not be modified externally.
   *
   * @param self RecognizerResult handler
   * @param waveform [out] Waveform buffer
   * @param size [out] Size of waveform buffer (in bytes)
   * @return ESR_INVALID_ARGUMENT if self, or waveform are null
   */
  ESR_ReturnCode(*getWaveform)(const struct SR_RecognizerResult_t* self, const asr_int16_t** waveform,
                               size_t* size);
  /**
   * Returns number of entries in the n-best list.
   *
   * @param self RecognizerResult handler
    * @param resultSize [out] Number of entries
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*getSize)(const struct SR_RecognizerResult_t* self, size_t* resultSize);
  /**
   * Returns number of [key, value] pairs in the current results.
   *
   * @param self SR_RecognizerResult handler
    * @param nbest Index of n-best list element (0-based)
   * @param count The number keys
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*getKeyCount)(const struct SR_RecognizerResult_t* self, const size_t nbest,
                               size_t* count);
  /**
   * Given an array of pointers to <code>LCHAR*</code>, populates that array with pointers
    * to the keys used internally by the recognition result. These keys should not be modified!
   *
   * @param self SemanticResult handler
    * @param nbest Index of n-best list element (0-based)
   * @param list [out] List of keys associated with n-best list entry.
    * @param listSize [in/out] Size of list. If the return code is ESR_BUFFER_OVERFLOW, the required size 
    *                 is returned in this variable.
   * @return ESR_INVALID_ARGUMENT if self or list are null; ESR_OUT_OF_BOUNDS if nbest entry does not exist;
   * ESR_BUFFER_OVERFLOW if the list that was passed in was too small
   */
  ESR_ReturnCode(*getKeyList)(const struct SR_RecognizerResult_t* self, const size_t nbest,
                              LCHAR** list, size_t* listSize);
  /**
   * Returns copy of semantic value.
   *
   * @param self SemanticResult handler
    * @param nbest Index of n-best list element (0-based)
    * @param key The key to look up
   * @param value [out] The buffer used to hold the resulting value
   * @param len [in/out] Length of value argument. If the return code is ESR_BUFFER_OVERFLOW,
   *            the required length is returned in this variable.
   * @return ESR_INVALID_ARGUMENT if self or list are null; ESR_OUT_OF_BOUNDS if nbest entry does not exist;
   * ESR_BUFFER_OVERFLOW if the buffer that was passed in was too small
   */
  ESR_ReturnCode(*getValue)(const struct SR_RecognizerResult_t* self, const size_t nbest,
                            const LCHAR* key, LCHAR* value, size_t* len);
                            
  /**
   * Returns locale of the grammar that produced this recognition result
   *
   * @param self SR_RecognizerResult handle
   * @param locale The locale associated with the result
  * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*getLocale)(const struct SR_RecognizerResult_t* self,  ESR_Locale* locale);
  
}
SR_RecognizerResult;

/**
 * Returns the endpointed waveform that was used for recognition. This returns a read-only buffer,
 * and may not be modified externally.
 *
 * @param self RecognizerResult handler
 * @param waveform [out] Waveform buffer
 * @param size [out] Size of waveform buffer (in bytes)
 * @return ESR_INVALID_ARGUMENT if self, or waveform are null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerResultGetWaveform(const SR_RecognizerResult* self,
    const asr_int16_t** waveform, size_t* size);
/**
 * Returns number of entries in the n-best list.
 *
 * @param self RecognizerResult handler
 * @param resultSize [out] Number of entries
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerResultGetSize(const SR_RecognizerResult* self,
    size_t* resultSize);
/**
 * Returns number of [key, value] pairs in the current results.
 *
 * @param nbest Index of n-best list element (0-based)
 * @param self SemanticResult handler
 * @param count The number keys
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerResultGetKeyCount(const SR_RecognizerResult* self,
    const size_t nbest,
    size_t* count);
/**
 * Given an array of pointers to <code>LCHAR*</code>, populates that array with pointers
 * to the keys used internally by the recognition result. These keys should not be modified!
 *
 * @param self SemanticResult handler
 * @param nbest Index of n-best list element (0-based)
 * @param list [out] List of keys associated with n-best list entry.
 * @param listSize [in/out] Size of list. If the return code is ESR_BUFFER_OVERFLOW, the required size
 *                 is returned in this variable.
 * @return ESR_INVALID_ARGUMENT if self or list are null; ESR_OUT_OF_BOUNDS if nbest entry does not exist;
 * ESR_BUFFER_OVERFLOW if the list that was passed in was too small
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerResultGetKeyList(const SR_RecognizerResult* self,
    const size_t nbest,
    LCHAR** list,
    size_t* listSize);
/**
 * Returns copy of semantic value.
 *
 * @param self SemanticResult handler
 * @param nbest Index of n-best list element (0-based)
 * @param key The key to look up
 * @param value [out] The buffer used to hold the resulting value
 * @param len [in/out] Length of value argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 * @return ESR_INVALID_ARGUMENT if self or list are null; ESR_OUT_OF_BOUNDS if nbest entry does not exist;
 * ESR_BUFFER_OVERFLOW if the buffer that was passed in was too small
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerResultGetValue(const SR_RecognizerResult* self,
    const size_t nbest,
    const LCHAR* key,
    LCHAR* value, size_t* len);
    
/**
 * Returns locale of grammar that produced this result
 *
 * @param self SR_RecognizerResult handle
 * @param locale The locale associated with the result
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerResultGetLocale(const SR_RecognizerResult* self,
    ESR_Locale* locale);
    
/**
 * @}
 */


#endif /* __SR_RECOGNIZERRESULT_H */
