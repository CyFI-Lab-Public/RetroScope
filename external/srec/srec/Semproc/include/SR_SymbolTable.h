/*---------------------------------------------------------------------------*
 *  SR_SymbolTable.h  *
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

#ifndef __SR_SYMBOLTABLE_H
#define __SR_SYMBOLTABLE_H



#include "SR_SemprocPrefix.h"
#include "SR_SemprocDefinitions.h"

#include "ESR_ReturnCode.h"

#include "HashMap.h"

#include "ptypes.h"
#include "pstdio.h"

#define MAX_SEMPROC_KEY   128 /* was 350 */
#define MAX_SEMPROC_VALUE 512 /* was 300 */

/**
 * Entries in the Symbol table are symbols i.e. key-value pairs.
 */
typedef struct Symbol_t
{
  /**
   * key string
   */
  LCHAR key[MAX_SEMPROC_KEY];
  /**
   * value string
   */
  LCHAR value[MAX_SEMPROC_VALUE];
}
Symbol;

/**
 * The Symbol Table
 */
typedef struct SymbolTable_t
{
  /**
   * Keep track of symbols using a hashmap of pointers 
   */
  HashMap* hashmap;
  
  /**
   * The symbols stored as an array
   */
  Symbol Symbols[MAX_SYMBOLS];
  
  /**
   * Pointer to the next available symbol slot for storing a symbol in the array 
   */
  Symbol *next;
  
  /**
   * Any special symbols that are set prior to semantic processing and read by the semantic processor
   */
  Symbol SpecialSymbols[MAX_SPECIAL_SYMBOLS];
  
  /**
   * The number of special symbols set.
   */
  size_t num_special_symbols;
  
}
SymbolTable;

/**
 * The "undefined" symbol value
 */
//static LCHAR undefined_symbol[] = UNDEFINED_SYMBOL;

/**
 * Create and Initialize
 * @param self pointer to the newly created object
 */
SREC_SEMPROC_API ESR_ReturnCode ST_Init(SymbolTable** self);

/**
 * Free
 * @param self pointer to the symbol table
 */
SREC_SEMPROC_API ESR_ReturnCode ST_Free(SymbolTable* self);

/**
 * Copies the symbols to a new hashmap (creates values dynamically)
 * @param self pointer to the symbol table
 * @param dst destination hashmap
 */
ESR_ReturnCode ST_Copy(SymbolTable* self, HashMap* dst);

/**
 * Store a key value pair
 * @param self pointer to the symbol table
 * @param key the key for the entry
 * @param value the value for the entry (associated with key)
 */
SREC_SEMPROC_API ESR_ReturnCode ST_putKeyValue(SymbolTable* self, const LCHAR* key, const LCHAR* value);

/**
 * Retrieve a value associated with the key
 * @param self pointer to the symbol table
 * @param key the key for the entry
 * @param value pointer to buffer for the storing result
 */
SREC_SEMPROC_API ESR_ReturnCode ST_getKeyValue(SymbolTable* self, const LCHAR* key, LCHAR** value);

/**
 * Ask for a new sot in the symbol table
 * @param self pointer to the symbol table
 * @param slot pointer to the slot given (NULL if none available)
 */
SREC_SEMPROC_API ESR_ReturnCode ST_getSymbolSlot(SymbolTable* self, Symbol** slot);

/**
 * Reset and clear the Symbol Table for a new script
 * @param self pointer to the symbol table
 */
SREC_SEMPROC_API ESR_ReturnCode ST_reset(SymbolTable* self);
SREC_SEMPROC_API ESR_ReturnCode ST_reset_all(SymbolTable* self);

/**
 * Store a "special" key value pair. These are special symbols that are set prior to semantic
 * processing and are ONLY read by the semantic processor during processing.
 * @param self pointer to the symbol table
 * @param key the key for the entry
 * @param value the value for the entry (associated with key)
 */
SREC_SEMPROC_API ESR_ReturnCode ST_putSpecialKeyValue(SymbolTable* self, const const LCHAR* key, const LCHAR* value);


#endif /* __SYMBOL_TABLE_H */
