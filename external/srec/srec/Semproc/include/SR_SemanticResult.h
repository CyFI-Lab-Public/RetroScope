/*---------------------------------------------------------------------------*
 *  SR_SemanticResult.h  *
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

#ifndef __SR_SEMANTICRESULT_H
#define __SR_SEMANTICRESULT_H



#include "ESR_ReturnCode.h"
#include "SR_SemprocPrefix.h"

/**
 * Semantic result.
 */
typedef struct SR_SemanticResult_t
{
  /**
   * Returns number of [key, value] pairs in the current results.
   *
   * @param self SemanticResult handler
   * @param count The number keys
   */
  ESR_ReturnCode(*getKeyCount)(struct SR_SemanticResult_t* self, size_t* count);
  /**
   * Given an array of pointers to <code>LCHAR*</code>, populates that array with pointers
    * to the keys used internally by the recognition result. These keys should not be modified!
   *
   * @param self SemanticResult handler
   * @param list [out] List of keys associated with n-best list entry.
    * @param size [in/out] Size of list. If the return code is ESR_BUFFER_OVERFLOW, the required size 
    *             is returned in this variable.
   */
  ESR_ReturnCode(*getKeyList)(struct SR_SemanticResult_t* self, LCHAR** list, size_t* size);
  /**
   * Returns copy of semantic value.
   *
   * @param self SemanticResult handler
    * @param key The key to look up
   * @param value [out] The buffer used to hold the resulting value
   * @param len [in/out] Length of value argument. If the return code is ESR_BUFFER_OVERFLOW,
   *            the required length is returned in this variable.
   */
  ESR_ReturnCode(*getValue)(struct SR_SemanticResult_t* self, const LCHAR* key, LCHAR* value, size_t* len);
  /**
   * Destroys a semantic result.
   *
   * @param self SemanticResult handler
   */
  ESR_ReturnCode(*destroy)(struct SR_SemanticResult_t* self);
}
SR_SemanticResult;


/**
 * Create a new semantic result.
 *
 * @param self SemanticResult handle
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticResultCreate(SR_SemanticResult** self);
/**
 * Returns number of [key, value] pairs in the current results.
 *
 * @param self SemanticResult handler
 * @param count The number keys
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticResultGetKeyCount(SR_SemanticResult* self, size_t* count);
/**
 * Given an array of pointers to <code>LCHAR*</code>, populates that array with pointers
 * to the keys used internally by the recognition result. These keys should not be modified!
 *
 * @param self SemanticResult handler
 * @param list [out] List of keys associated with n-best list entry.
 * @param size [in/out] Size of list. If the return code is ESR_BUFFER_OVERFLOW, the required size
 *             is returned in this variable.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticResultGetKeyList(SR_SemanticResult* self, LCHAR** list,
    size_t* size);
/**
 * Returns value component of [key, value] pair.
 *
 * @param self SemanticResult handler
 * @param key The key to look up
 * @param value [out] The buffer used to hold the resulting value
 * @param len [in/out] Length of value argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticResultGetValue(SR_SemanticResult* self, const LCHAR* key, LCHAR* value, size_t* len);
/**
 * Destroys a semantic result.
 *
 * @param self SemanticResult handler
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticResultDestroy(SR_SemanticResult* self);


#endif /* __SR_SEMANTICRESULT_H */
