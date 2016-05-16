/*---------------------------------------------------------------------------*
 *  LexicalAnalyzer.c  *
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

#include "SR_LexicalAnalyzer.h"
#include "plog.h"
#include "pmemory.h"


static const char* MTAG = __FILE__;

ESR_BOOL isIdentifierChar(LCHAR p);

ESR_ReturnCode LA_Init(LexicalAnalyzer** self)
{
  LexicalAnalyzer* Interface;
  
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  
  Interface = NEW(LexicalAnalyzer, MTAG);
  if (Interface == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }
  *self = Interface;
  return ESR_SUCCESS;
}

ESR_ReturnCode LA_Analyze(LexicalAnalyzer *lex, LCHAR *script)
{
  if (lex == NULL || script == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  
  /* point to the first char */
  lex->nextToken = lex->script = script;
  return ESR_SUCCESS;
}

ESR_ReturnCode LA_Free(LexicalAnalyzer *lex)
{
  if (lex == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  FREE(lex);
  return ESR_SUCCESS;
}


ESR_ReturnCode LA_nextToken(LexicalAnalyzer *lex, LCHAR *tokenBuf, size_t* tokenLen)
{
  LCHAR *p;
  LCHAR *q;
  
  while (LISSPACE(*lex->nextToken))
    ++lex->nextToken;
    
  switch (*lex->nextToken)
  {
    case OP_ASSIGN:
    case OP_CONCAT:
    case LBRACKET:
    case PARAM_DELIM:
    case RBRACKET:
    case OP_CONDITION_IFTRUE:
    case OP_CONDITION_ELSE:
    case EO_STATEMENT:
      tokenBuf[0] = *lex->nextToken;
      tokenBuf[1] = EO_STRING;
      *tokenLen = 1;
      break;
    case STRING_DELIM:
      p = lex->nextToken;
      q = tokenBuf;
      *q++ = *p++;
/* finds the end of the constant string also protects against going past end of string
 * The parser above will handle the incomplete string. SteveR
 */ 
      while ( ( *p != STRING_DELIM ) && ( *p != '\0' ) )
      {
        if (*p == ESC_CHAR)
          *q++ = *p++;
        *q++ = *p++;
      }
      
      *q++ = *p++; 
      *tokenLen = q - tokenBuf;
      tokenBuf[*tokenLen] = EO_STRING; /* make sure its there */
      break;
    default:
      p = lex->nextToken;
      while (isIdentifierChar(*p))  /* finds the end of the name of this identifier */
        ++p;
      *tokenLen = p - lex->nextToken;
      LSTRNCPY(tokenBuf, lex->nextToken, *tokenLen);
      tokenBuf[*tokenLen] = EO_STRING; /* make sure its there */
  }
  lex->nextToken += *tokenLen;
  return ESR_SUCCESS;
}

/**
 * Indicates if character is in range [a-z] or [A-Z] or [0-9] or ['.'].
 **/
ESR_BOOL isIdentifierChar(LCHAR p)
{
  return (p == DOT ||                     /* the dot */
         p == USCORE ||                  /* the underscore */
         (p <= L('z') && p >= L('a')) || /* lowercase alpha */
         (p <= L('Z') && p >= L('A')) || /* uppercase alpha */
         (p <= L('9') && p >= L('0'))) ? ESR_TRUE : ESR_FALSE;   /* numbers */
}
