/*---------------------------------------------------------------------------*
 *  ESR_SessionTypeListener.h  *
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

#ifndef __ESR_SESSIONTYPELISTENER_H
#define __ESR_SESSIONTYPELISTENER_H



#include "ESR_ReturnCode.h"
#include "ESR_SharedPrefix.h"
#include "ESR_VariableTypes.h"
#include "pstdio.h"
#include "ptypes.h"

/**
 * @addtogroup ESR_SessionTypeListenerModule ESR_SessionTypeListener API functions
 * ESR_Session event-listener interface functions.
 *
 * @{
 */

/**
 * Listens for changes in ESR_SessionType.
 */
typedef struct ESR_SessionTypeListener_t
{
  /**
   * A property value has changed.
   *
   * @param self ESR_SessionTypeListener handle
  * @param name Name of property that changed
   * @param oldValue Old property value
   * @param newValue New property value
  * @param variableType Type of property
  * @param data User-data passed to listener.
   */
  ESR_ReturnCode(*propertyChanged)(struct ESR_SessionTypeListener_t* self, const LCHAR* name,
                                   const void* oldValue, const void* newValue, VariableTypes variableType, void* data);
                                   
}
ESR_SessionTypeListener;

/**
 * Associates a ESR_SessionTypeListener with user-data.
 */
typedef struct ESR_SessionTypeListenerPair_t
{
  /**
   * Event-listener.
   */
  ESR_SessionTypeListener* listener;
  /**
   * User-data to pass to listener.
   */
  void* data;
}
ESR_SessionTypeListenerPair;

/**
 * @}
 */


#endif /* __ESR_SESSIONTYPELISTENER_H */
