/*---------------------------------------------------------------------------*
 *  SR_ExpressionEvaluator.h  *
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

#ifndef __SR_EXPRESSION_EVALUATOR_H
#define __SR_EXPRESSION_EVALUATOR_H



#include "SR_SemprocPrefix.h"
#include "SR_SemprocDefinitions.h"

#include "ESR_ReturnCode.h"

#include "ptypes.h"
#include "pmemory.h"
#include "pstdio.h"


/**
 * Stub for the Expression Evaluator.
 * In the future this may be replaced by an actual data structure.
 */
typedef void ExpressionEvaluator;

/**
 * Create and Initialize.
 *
 * @param self pointer to the newly created expression evaluator
 */
SREC_SEMPROC_API ESR_ReturnCode EE_Init(ExpressionEvaluator** self);

/**
 * Free memory.
 *
 * @param self pointer to the expression evaluator to destroy
 *
 */
SREC_SEMPROC_API ESR_ReturnCode EE_Free(ExpressionEvaluator* self);

/**
 * Built-in function to concatenate strings.
 *
 * @param data user data reference used in callback function
 * @param operands array of strings holding operands to concatenate
 * @param opCount number of operands
 * @param resultBuf pointer to string buffer where result will be stored
 * @param resultLen the length of the result
 *
 */
SREC_SEMPROC_API ESR_ReturnCode EE_concat(LCHAR* name, LCHAR** operands, size_t opCount, void* data, LCHAR* resultBuf, size_t* resultLen);

/**
 * Built-in function to support conditional expressions (with 3 operands only!!!)
 * @param data user data reference used in callback function
 * @param operands first op is the condition, second is the true val, third is the false val
 * @param opCount number of operands
 * @param resultBuf pointer to string buffer where result will be stored
 * @param resultLen the length of the result
 */
SREC_SEMPROC_API ESR_ReturnCode EE_conditional(LCHAR* name, LCHAR** operands, size_t opCount, void* data, LCHAR* resultBuf, size_t* resultLen);

/**
 * Built-in function to support string value addition
 * @param data user data reference used in callback function
 * @param operands strings to interpret as integers and then add together
 * @param opCount number of operands
 * @param resultBuf pointer to string buffer where result will be stored
 * @param resultLen the length of the result
 */
SREC_SEMPROC_API ESR_ReturnCode EE_add(LCHAR* name, LCHAR** operands, size_t opCount, void* data, LCHAR* resultBuf, size_t* resultLen);

/**
 * Built-in function to support string value substraction
 * @param data user data reference used in callback function
 * @param operands strings to interpret as integers and then subtract from the first operand
 * @param opCount number of operands
 * @param resultBuf pointer to string buffer where result will be stored
 * @param resultLen the length of the result
 */
SREC_SEMPROC_API ESR_ReturnCode EE_subtract(LCHAR* name, LCHAR** operands, size_t opCount, void* data, LCHAR* resultBuf, size_t* resultLen);

#endif /* __EXPRESSION_EVALUATOR_H */
