/*---------------------------------------------------------------------------*
 *  SR_AcousticModels.h  *
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

#ifndef __SR_ACOUSTICMODELS_H
#define __SR_ACOUSTICMODELS_H




#include <stddef.h>
#include <stdlib.h>
#include "SR_AcousticModelsPrefix.h"
#include "pstdio.h"
#include "ptypes.h"
#include "ESR_ReturnCode.h"

/**
 * AcousticModel ID.
 */
typedef asr_uint8_t SR_AcousticModelID;

/**
 * @addtogroup SR_AcousticModelsModule SR_AcousticModels API functions
 * Acoustic model collection.
 *
 * @{
 */

/**
 * Acoustic model collection.
 */
typedef struct SR_AcousticModels_t
{
  /**
   * Destroys an AcousticModel collection.
   *
   * @param self SR_AcousticModels handle
   */
  ESR_ReturnCode(*destroy)(struct SR_AcousticModels_t* self);
  /**
   * Saves an AcousticModel collection to file.
   *
   * @param self SR_AcousticModels handle
   * @param filename File to write to
   * @deprecated Not supported
   */
  ESR_ReturnCode(*save)(struct SR_AcousticModels_t* self, const LCHAR* filename);
  /**
   * Sets AcousticModels parameter, overriding session defaults.
   *
   * @param self SR_AcousticModels handle
   * @param key Parameter name
   * @param value Parameter value
   */
  ESR_ReturnCode(*setParameter)(struct SR_AcousticModels_t* self, const LCHAR* key, LCHAR* value);
  /**
   * Returns AcousticModels parameter value.
   *
   * @param self SR_AcousticModels handle
   * @param key Parameter name
   * @param value [in/out] Parameter value
   * @param len [in/out] Length of value argument. If the return code is ESR_BUFFER_OVERFLOW,
   *            the required length is returned in this variable.
   */
  ESR_ReturnCode(*getParameter)(struct SR_AcousticModels_t* self, const LCHAR* key, LCHAR* value, size_t* len);
  /**
   * Returns the number of AcousticModels in the collection.
   *
   * @param self SR_AcousticModels handle
   * @param size The result
   */
  ESR_ReturnCode(*getCount)(struct SR_AcousticModels_t* self, size_t* size);
  /**
   * Returns modelID of a component.
   *
   * @param self SR_AcousticModels handle
   * @param index Index of model within collection (0-based)
   * @param id [out] Resulting model ID
   * @param size [in/out] Length of id argument. If the return code is ESR_BUFFER_OVERFLOW, 
   *             the required length is returned in this variable.
   */
  ESR_ReturnCode(*getID)(struct SR_AcousticModels_t* self, size_t index, SR_AcousticModelID* id, size_t* size);
  /**
   * Sets the modelID of a component.
   *
   * @param self SR_AcousticModels handle
   * @param index Index of model within collection (0-based)
   * @param id New model ID
   */
  ESR_ReturnCode(*setID)(struct SR_AcousticModels_t* self, size_t index, SR_AcousticModelID* id);
  /**
   * Returns the arbdata (CA_Arbdata*) of a component.
   *
   * @param self SR_AcousticModels handle
   */
  void* (*getArbdata)(struct SR_AcousticModels_t* self);

}
SR_AcousticModels;

/**
 * @name Models operations
 *
 * @{
 */

/**
 * Destroys an AcousticModel collection.
 *
 * @param self SR_AcousticModels handle
 */
SREC_ACOUSTICMODELS_API ESR_ReturnCode SR_AcousticModelsDestroy(SR_AcousticModels* self);
/**
 * Loads an AcousticModel collection from file.
 *
 * @param filename File to read from
 * @param self SR_AcousticModels handle
 */
SREC_ACOUSTICMODELS_API ESR_ReturnCode SR_AcousticModelsLoad(const LCHAR* filename, 
                                                             SR_AcousticModels** self);
/**
 * Saves an AcousticModel collection to file.
 *
 * @param self SR_AcousticModels handle
 * @param filename File to write to
 */
SREC_ACOUSTICMODELS_API ESR_ReturnCode SR_AcousticModelsSave(SR_AcousticModels* self, const LCHAR* filename);
/**
 * Sets AcousticModel parameter, overriding session defaults.
 *
 * @param self SR_AcousticModels handle
 * @param key Parameter name
 * @param value Parameter value
 */
SREC_ACOUSTICMODELS_API ESR_ReturnCode SR_AcousticModelsSetParameter(SR_AcousticModels* self, const LCHAR* key, LCHAR* value);
/**
 * Returns AcousticModel parameter value.
 *
 * @param self SR_AcousticModels handle
 * @param key Parameter name
 * @param value [out] Parameter value
 * @param len [in/out] Length of value argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 */
SREC_ACOUSTICMODELS_API ESR_ReturnCode SR_AcousticModelsGetParameter(SR_AcousticModels* self, const LCHAR* key, LCHAR* value, size_t* len);
/**
 * Returns the number of AcousticModels in the collection.
 *
 * @param self SR_AcousticModels handle
 * @param size The result
 */
SREC_ACOUSTICMODELS_API ESR_ReturnCode SR_AcousticModelsGetCount(SR_AcousticModels* self, size_t* size);
/**
 * Returns modelID of a component.
 *
 * @param self SR_AcousticModels handle
 * @param index Index of model within collection (0-based)
 * @param id [out] Resulting model ID
 * @param idLen [in/out] Length of id argument. If the return code is ESR_BUFFER_OVERFLOW, the
 *              required length is returned in this variable.
 */
SREC_ACOUSTICMODELS_API ESR_ReturnCode SR_AcousticModelsGetID(SR_AcousticModels* self, size_t index, SR_AcousticModelID* id,  size_t* idLen);
/**
 * Sets the modelID of a component.
 *
 * @param self SR_AcousticModels handle
 * @param index Index of model within collection (0-based)
 * @param id New model ID
 */
SREC_ACOUSTICMODELS_API ESR_ReturnCode SR_AcousticModelsSetID(SR_AcousticModels* self, size_t index, SR_AcousticModelID* id);

/**
 * @}
 */

/**
 * @}
 */


#endif /* __SR_ACOUSTICMODELS_H */
