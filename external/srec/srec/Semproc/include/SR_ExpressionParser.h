/*---------------------------------------------------------------------------*
 *  SR_ExpressionParser.h  *
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

#ifndef __SR_EXPRESSION_PARSER_H
#define __SR_EXPRESSION_PARSER_H



#include "SR_SemprocPrefix.h"
#include "SR_SemprocDefinitions.h"

#include "SR_LexicalAnalyzer.h"
#include "SR_SymbolTable.h"
#include "SR_ExpressionEvaluator.h"

#include "ptypes.h"
#include "pstdio.h"
#include "pmemory.h"

#include "ESR_ReturnCode.h"
#include "ESR_Session.h"
#include "SR_Grammar.h"

#define SR_SemprocFunctionPtr SR_GrammarDispatchFunction

/**
 * States in the finite state machine used for parsing.
 */
enum
{
  /**
   * Initial state for a new expression
   */
  LHS_REQUIRED,
  
  /**
   * Equal sign required next
   */
  OP_ASSIGN_REQUIRED,
  
  /**
   * Identifier (const or rule property reference) required next
   */
  IDENTIFIER_REQUIRED,
  
  /**
   * Any operand other than equal sign required next
   */
  OP_ANY_REQUIRED,
};

/**
 * Structure for holding function callbacks which may be registered by the programmer from the
 * application, or internally as built-in functions (see ExpressionEvaluator.h)
 */
typedef struct FunctionCallback_t
{
  /**
   * The pointer to the function
   */
  SR_SemprocFunctionPtr pfunction;
  
  /**
   * User data
   */
  void* userData;
  
}
FunctionCallback;


/**
 * The Parser.
 */
typedef struct ExpressionParser_t
{
  /**
   * The current state 
   */
  int    state;
  
  /**
   * buffer for holding the token on the lhs of equal sign
   */
  LCHAR  lhs[MAX_STRING_LEN];
  
  /**
   * buffer for holding the operator (which may be more than 1 char in the future!!!)
   */
  LCHAR  op[MAX_STRING_LEN];
  
  /**
   * buffers for holding the idetifiers encountered on the rhs of this expression
   */
  LCHAR  identifiers[MAX_RHS_IDENTIFIERS][MAX_STRING_LEN];
  
  /**
   * the number of identifiers encountered
   */
  size_t idCount;
  
  /**
   * pointer to the appropriate buffer (above) for storing the next token encountered
   * which may be an operator, identifier, etc...
   */
  LCHAR  *ptokenBuf;
  
  
  /***************************/
  /* function callback stuff */
  /***************************/
  
  /**
   * hashtable used for keeping track of registered function callbacks
   */
  HashMap *pfunctions;
  
  /**
   * Array storing all the function callbacks
   */
  FunctionCallback functions[MAX_FUNCTION_CALLBACKS];
  
  /**
   * Pointer to the next available function callback slot in the array
   */
  FunctionCallback *next;
  
  /**
   * Pointer to the current function to carry out in this expression (only one per expression !!!)
   * Nesting of functions is NOT SUPPORTED
   */
  SR_SemprocFunctionPtr pfunction;
  
  /**
   * Reference to current user data
   */
  void* userData;
  
  /**
   * The current function name 
   */
  LCHAR  functionName[MAX_STRING_LEN];
  
  /**
   * Indicates when a function needs to be executed at the end of a statement.
   */
  ESR_BOOL needToExecuteFunction;
}
ExpressionParser;


/**
 * Create and Initialize.
 *
 * @param self pointer to the newly created parser
 */
SREC_SEMPROC_API ESR_ReturnCode EP_Init(ExpressionParser **self);

/**
 * Free.
 *
 * @param self pointer to the parser
 */
SREC_SEMPROC_API ESR_ReturnCode EP_Free(ExpressionParser *self);

/**
 * Do the parsing of the script.
 * @param self pointer to the parser
 * @param lexAnalyzer pointer to the lexical analyzer where the parser gets tokens from
 * @param symtable pointer to the symbol table where the parser gets/sets key-value pairs
 * @param evaluator pointer to the expression evaluator where calls functiosn to evaulate expressions
 * @param hashmap pointer to a hashmap used to store the results of processing
 */
SREC_SEMPROC_API ESR_ReturnCode EP_parse(ExpressionParser* self, LexicalAnalyzer* lexAnalyzer,
    SymbolTable* symtable, ExpressionEvaluator* evaluator,
    HashMap** hashmap);
    
/**
 * Register a function.
 * @param self pointer to the parser
 * @param name name of the function, as it will appear in the script
 * @param data User data
 * @param pfunction pointer to the function
 * @return ESR_SUCCESS
 */
SREC_SEMPROC_API ESR_ReturnCode EP_RegisterFunction(ExpressionParser* self, const LCHAR* name, void* data, SR_SemprocFunctionPtr pfunction);

/**
 * Lookup pointer to a registered function.
 * @param self pointer to the parser
 * @param name name of the function, as it will appear in the script
 * @param pfunction pointer to the function
 */
SREC_SEMPROC_API ESR_ReturnCode EP_LookUpFunction(ExpressionParser* self, LCHAR* name, void** data, SR_SemprocFunctionPtr* pfunction);


#endif
