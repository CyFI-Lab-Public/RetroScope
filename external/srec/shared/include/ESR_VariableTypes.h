/*---------------------------------------------------------------------------*
 *  ESR_VariableTypes.h  *
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

#ifndef __ESR_VARIABLETYPES_H
#define __ESR_VARIABLETYPES_H

/**
 * Enumeration of all possible variable types. Used to add strong-typing capability to generic 
 * containers like ArrayList.
 */
typedef enum VariableTypes_t
{
  TYPES_INT,
  TYPES_FLOAT,
  TYPES_BOOL,
  TYPES_PLCHAR,       /* (LCHAR*)     */
	TYPES_UINT16_T,
  TYPES_SIZE_T,
  TYPES_INTARRAYLIST,
  TYPES_CONFIDENCESCORER,
  TYPES_SR_ACOUSTICMODELS,
  TYPES_SR_VOCABULARY,
  TYPES_SR_EVENTLOG
} VariableTypes;

#endif /* __ESR_VARIABLETYPES_H */
