/*---------------------------------------------------------------------------*
 *  SR_SemprocDefinitions.h  *
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

#ifndef __SR_SEMPROCDEFINITIONS_H
#define __SR_SEMPROCDEFINITIONS_H



#include "ptypes.h"
#include "pstdio.h"

/**
 * Whether to display verbose error message when parse fails.
 */
#define SEMPROC_VERBOSE_PARSE_ERROR 1

/**
* Max number of semantic results
*/
#define MAX_SEM_RESULTS         1

/**
 * Maximum number of symbols allowed in the Symbol table when parsing a single script
 */
#define MAX_SYMBOLS            40

/**
 * Maximum number of special symbols allowed in the Symbol table set before parsing and read during
 */
#define MAX_SPECIAL_SYMBOLS     1


/**
 * Maximum size of strings
 */
#define MAX_STRING_LEN        350

/**
 * Maximum length of accumulated scripts
 */
#define MAX_SCRIPT_LEN       8192

/**
 * Maximum number of identifiers allowed on the RHS of equal sign
 */
#define MAX_RHS_IDENTIFIERS    10

/**
 * Maximum number of function callbacks the may be registered
 * fix: 2004-05-20, this was limiting the number of concurrently
 * loaded grammars, now patched but source problem not repaired
 */
#define MAX_FUNCTION_CALLBACKS 32

/**
 * Max depth of a graph (tokens including scope markers, scripts, words etc)
 */
#define MAX_SEM_GRAPH_DEPTH       128

/**
 * Maximum number of partial paths which will be used when parsing
 */
#define MAX_SEM_PARTIAL_PATHS     512

/**
 * Maximum number of tokens encountered on all partial paths combined
 */
#define MAX_PATH_OLABELS          2048

/**
 * Maximum number of scripts accumulated on a path through grammar
 */
#define MAX_SCRIPTS                512

/**
 * Offset used for denoting scripts (since integer used as label in graph)
 */
#define SEMGRAPH_SCRIPT_OFFSET  30000

/**
 * Offset used for denoting scope markers (since integer used as label in graph)
 */
#define SEMGRAPH_SCOPE_OFFSET   40000

/**
 * Assignment operator
 */
#define OP_ASSIGN        L('=')

/**
 * String concatenation operator
 */
#define OP_CONCAT        L('+')

/**
 * Left bracket
 */
#define LBRACKET         L('(')

/**
 * Delimiter for parameters in a function call
 */
#define PARAM_DELIM      L(',')

/**
 * Right bracket
 */
#define RBRACKET         L(')')

/**
 * Question mark used in conditional expressions to signify end of condition part
 */
#define OP_CONDITION_IFTRUE  L('?')

/**
 * Colon used in conditional expressions to signify the alternative (false) return value
 */
#define OP_CONDITION_ELSE    L(':')

/**
 * End of statement operator
 */
#define EO_STATEMENT     L(';')

/**
 * Delimiter for constant string identifiers
 */
#define STRING_DELIM     L('\'')

/**
 * Dot used for rule property referencing
 */
#define DOT              L('.')

/**
 * Underscore sometimes used in identifiers
 */
#define USCORE           L('_')

/**
 * Newline character
 */
#define NL               L('\n')

/**
 * End of string character
 */
#define EO_STRING        L('\0')

/**
 * Escape character.
 **/
#define ESC_CHAR L('\\')
/**
 * CHAR used for joining (union) multiple meanings for same word
 */
#define MULTIPLE_MEANING_JOIN_CHAR L('#')

/**
 * String used for undefined string variables
 */
#define UNDEFINED_SYMBOL L("undefined")

/**
 * Boolean symbol true
 */
#define TRUE_SYMBOL      L("true")

/**
* Boolean symbol false
*/
#define FALSE_SYMBOL     L("false")

/** 
 * markers
 */
#define BEGIN_SCOPE_MARKER L('{')
#define END_SCOPE_MARKER   L('}')
#define IS_BEGIN_SCOPE(wW) (wW[0] == BEGIN_SCOPE_MARKER && wW[1] == 0)
#define IS_END_SCOPE(wW) ((_tMp=LSTRCHR(wW,END_SCOPE_MARKER))!=0 && _tMp[1]==0)
#define IS_SCOPE_MARKER(wW) ( IS_BEGIN_SCOPE(wW) || IS_END_SCOPE(wW))
#define IS_SCRIPT_MARKER(wW) (wW[0] == '_' && isnum(&wW[1]))
#define IS_OPERATOR(p) ((*p)==','|| (*p)=='+' || (*p)=='=' || (*p)=='(' || (*p)==')' || (*p)==':' || (*p)=='?')
#define IS_LOCAL_IDENTIFIER(p, len) ( (*p)!=';' && !IS_OPERATOR(p) && *p!='\'' && !LSTRNCHR2(p,'.','(',len))


/**
 * This macro checks if memory allocation is possible (against internal limit values)
 * and returns ESR_OUT_OF_MEMORY otherwise.
 */
#define MEMCHK(rc, val, threshold) \
  do { \
    if(val > threshold) \
    { \
      rc = ESR_OUT_OF_MEMORY; \
      PLogError(L("%s: %d > %d\n"), ESR_rc2str(rc), (val), (threshold)); \
      goto CLEANUP; \
    } \
  } while(0);
#define LENCHK(rc, val, threshold) \
  do { \
    if(LSTRLEN(val) > threshold) \
    { \
      rc = ESR_OUT_OF_MEMORY; \
      PLogError(L("%s: %s > %d\n"), ESR_rc2str(rc), (val), (threshold)); \
      goto CLEANUP; \
    } \
  } while(0);

/**
 * Base 10 used for ITOA macro
 */
#define BASE_10 10

#endif /* __DEFINTIONS_H */
