/*---------------------------------------------------------------------------*
 *  ExpressionParser.c  *
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

#include "SR_ExpressionParser.h"
#include "plog.h"



static const char* MTAG = __FILE__;


/**
 * These are handlers for tokens. They modify state of the parser
 */
ESR_ReturnCode handle_NewStatement(ExpressionParser *self);
ESR_ReturnCode handle_Identifier(ExpressionParser *self);
ESR_ReturnCode handle_OpAssign(ExpressionParser *self);
ESR_ReturnCode handle_OpConcat(ExpressionParser *self);
ESR_ReturnCode handle_LBracket(ExpressionParser *self);
ESR_ReturnCode handle_ParamDelim(ExpressionParser *self);
ESR_ReturnCode handle_RBracket(ExpressionParser *self);
ESR_ReturnCode handle_ConditionalExpression_IfTrue(ExpressionParser *self);
ESR_ReturnCode handle_ConditionalExpression_Else(ExpressionParser *self);
ESR_ReturnCode handle_EndOfStatement(ExpressionParser *self, SymbolTable *st, ExpressionEvaluator *ee);


ESR_ReturnCode EP_Init(ExpressionParser** self)
{
  ESR_ReturnCode rc;
  ExpressionParser* Interface;
  
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  
  Interface = NEW(ExpressionParser, MTAG);
  if (Interface == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }
  
  /* create the hashtable for looking up the function callbacks */
  CHKLOG(rc, HashMapCreate(&Interface->pfunctions));
  
  /* register the built-in callbacks */
  Interface->next = &Interface->functions[0];
  CHKLOG(rc, EP_RegisterFunction(Interface, L("concat"), NULL, EE_concat));
  CHKLOG(rc, EP_RegisterFunction(Interface, L("conditional"), NULL, EE_conditional));
  CHKLOG(rc, EP_RegisterFunction(Interface, L("add"), NULL, EE_add));
  CHKLOG(rc, EP_RegisterFunction(Interface, L("subtract"), NULL, EE_subtract));
  Interface->needToExecuteFunction = ESR_FALSE;
  *self = Interface;
  return ESR_SUCCESS;
CLEANUP:
  EP_Free(Interface);
  return rc;
}

ESR_ReturnCode EP_Free(ExpressionParser* self)
{
  ESR_ReturnCode rc;
  
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  CHKLOG(rc, HashMapRemoveAll(self->pfunctions));
  
  /* free all the memory lots by simply resetting the next pointer */
  self->next = &self->functions[0];
  
  /* delete the hash table */
  CHKLOG(rc, HashMapDestroy(self->pfunctions));
  FREE(self);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode EP_parse(ExpressionParser* parser, LexicalAnalyzer* lexAnalyzer,
                        SymbolTable* symtable, ExpressionEvaluator* evaluator,
                        HashMap** hashmap)
{
  ESR_ReturnCode rc;
  size_t tokenLen;
  ESR_BOOL verbose = ESR_FALSE;
  ESR_BOOL sessionExists = ESR_FALSE;
  
  /* init */
  CHKLOG(rc, ST_reset(symtable)); /* reset the symbol table, for a new set of keys and values */
  CHKLOG(rc, handle_NewStatement(parser));
  
  while (ESR_TRUE)
  {
    CHKLOG(rc, LA_nextToken(lexAnalyzer, parser->ptokenBuf, &tokenLen));
    if (!tokenLen)
      break; /* no more tokens */
      
    switch (parser->ptokenBuf[0])
    {
      case OP_ASSIGN:
        CHKLOG(rc, handle_OpAssign(parser));
        break;
      case OP_CONCAT:
        CHKLOG(rc, handle_OpConcat(parser));
        break;
      case LBRACKET:
        CHKLOG(rc, handle_LBracket(parser));
        break;
      case PARAM_DELIM:
        CHKLOG(rc, handle_ParamDelim(parser));
        break;
      case RBRACKET:
        CHKLOG(rc, handle_RBracket(parser));
        break;
      case OP_CONDITION_IFTRUE:
        CHKLOG(rc, handle_ConditionalExpression_IfTrue(parser));
        break;
      case OP_CONDITION_ELSE:
        CHKLOG(rc, handle_ConditionalExpression_Else(parser));
        break;
      case EO_STATEMENT:
        CHKLOG(rc, handle_EndOfStatement(parser, symtable, evaluator));
        break;
      default:
        CHKLOG(rc, handle_Identifier(parser));
        break;
    }
  }
  
  if (rc == ESR_SUCCESS)
    CHKLOG(rc, ST_Copy(symtable, *hashmap));
  else
    *hashmap = NULL; /* don't give access to hashtable if something went wrong */
  return ESR_SUCCESS;
  
CLEANUP:
  CHKLOG(rc, ESR_SessionExists(&sessionExists));
  
  if (sessionExists)
    rc = ESR_SessionGetBool(L("cmdline.semproc_verbose"), &verbose);
  else
    verbose = ESR_TRUE; /* apps like parseStringTest will not init session, but I want a
                         descriptive error message regardless */
  
  if (rc == ESR_NO_MATCH_ERROR)
    rc = ESR_SUCCESS;
    
  if (verbose)
  {
    PLogError(L("\n\nSemproc: error parsing symbol '%s'\nbefore: '%s'\nin script:\n%s\n\n"),
              parser->ptokenBuf,
              (lexAnalyzer->nextToken ? lexAnalyzer->nextToken : L("<end-of-script>")),
              lexAnalyzer->script);
  }
  return rc;
}

ESR_ReturnCode handle_NewStatement(ExpressionParser* self)
{
  /* initially I want ptokenBuf to point to the lhs */
  self->ptokenBuf = self->lhs;
  self->state = LHS_REQUIRED;
  self->idCount = 0;
  self->pfunction = 0;
  return ESR_SUCCESS;
}

ESR_ReturnCode handle_Identifier(ExpressionParser* self)
{
  ESR_ReturnCode rc;
  
  switch (self->state)
  {
    case LHS_REQUIRED:
      self->ptokenBuf = self->op;
      self->state = OP_ASSIGN_REQUIRED;
      return ESR_SUCCESS;
    case IDENTIFIER_REQUIRED:
      self->ptokenBuf = self->op;
      self->state = OP_ANY_REQUIRED;
      self->idCount++; /* index to the next id slot */
      return ESR_SUCCESS;
    default:
      rc = ESR_INVALID_STATE;
      PLogError(L("%s: state=%d - are there reserved chars in the tag?"), ESR_rc2str(rc), self->state);
      return rc;
  }
}

ESR_ReturnCode handle_OpAssign(ExpressionParser* self)
{
  ESR_ReturnCode rc;
  
  if (self->state == OP_ASSIGN_REQUIRED)
  {
    MEMCHK(rc, self->idCount, MAX_RHS_IDENTIFIERS - 1);
    self->ptokenBuf = self->identifiers[self->idCount];
    self->state = IDENTIFIER_REQUIRED;
    return ESR_SUCCESS;
  }
  return ESR_INVALID_STATE;
CLEANUP:
  return rc;
}

ESR_ReturnCode handle_OpConcat(ExpressionParser* self)
{
  ESR_ReturnCode rc;
  
  if (self->state == OP_ANY_REQUIRED)
  {
    MEMCHK(rc, self->idCount, MAX_RHS_IDENTIFIERS - 1);
    /* pointer to function to carry out in the Expression Evaluator */
    CHKLOG(rc, EP_LookUpFunction(self, "concat", &self->userData, &self->pfunction));
    self->needToExecuteFunction = ESR_TRUE;
    self->ptokenBuf = self->identifiers[self->idCount];
    self->state = IDENTIFIER_REQUIRED;
    return ESR_SUCCESS;
  }
  return ESR_INVALID_STATE;
CLEANUP:
  return rc;
}

ESR_ReturnCode handle_LBracket(ExpressionParser* self)
{
  ESR_ReturnCode rc;
  
  switch (self->state)
  {
    case IDENTIFIER_REQUIRED :
      MEMCHK(rc, self->idCount, MAX_RHS_IDENTIFIERS - 1);
      self->ptokenBuf = self->identifiers[self->idCount];
      self->state = IDENTIFIER_REQUIRED;
      return ESR_SUCCESS;
      
    case OP_ANY_REQUIRED :
      MEMCHK(rc, self->idCount, MAX_RHS_IDENTIFIERS - 1);
      
      /* the name of the function is stored as the most recent identifier encountered */
      rc = EP_LookUpFunction(self, self->identifiers[self->idCount-1], &self->userData, &self->pfunction);
      if (rc == ESR_NO_MATCH_ERROR)
      {
        self->pfunction = NULL;
        /*
        PLogError(L("%s: Function '%s' is undefined"), ESR_rc2str(rc), self->identifiers[self->idCount-1]);
        return rc;
        */
      }
      self->needToExecuteFunction = ESR_TRUE;
      /* save the function name for future reference */
      LSTRCPY(self->functionName, self->identifiers[self->idCount-1]);
      /* now reuse old identifier slot */
      --self->idCount;
      self->ptokenBuf = self->identifiers[self->idCount];
      
      self->state = IDENTIFIER_REQUIRED;
      return ESR_SUCCESS;
    default:
      return ESR_INVALID_STATE;
  }
CLEANUP:
  return rc;
}

ESR_ReturnCode handle_ParamDelim(ExpressionParser* self)
{
  switch (self->state)
  {
    case OP_ANY_REQUIRED :
      self->ptokenBuf = self->identifiers[self->idCount];
      self->state = IDENTIFIER_REQUIRED;
      return ESR_SUCCESS;
    default:
      return ESR_INVALID_STATE;
  }
}


ESR_ReturnCode handle_RBracket(ExpressionParser* self)
{
  switch (self->state)
  {
    case OP_ANY_REQUIRED :
      self->ptokenBuf = self->op;
      self->state = OP_ANY_REQUIRED;
      return ESR_SUCCESS;
    default:
      return ESR_INVALID_STATE;
  }
}

ESR_ReturnCode handle_ConditionalExpression_IfTrue(ExpressionParser* self)
{
  ESR_ReturnCode rc;
  
  switch (self->state)
  {
    case OP_ANY_REQUIRED :
      self->ptokenBuf = self->identifiers[self->idCount];
      CHKLOG(rc, EP_LookUpFunction(self, "conditional", &self->userData, &self->pfunction));
      self->needToExecuteFunction = ESR_TRUE;
      self->state = IDENTIFIER_REQUIRED;
      return ESR_SUCCESS;
    default:
      return ESR_INVALID_STATE;
  }
CLEANUP:
  return rc;
}

ESR_ReturnCode handle_ConditionalExpression_Else(ExpressionParser* self)
{
  switch (self->state)
  {
    case OP_ANY_REQUIRED :
      self->ptokenBuf = self->identifiers[self->idCount];
      self->state = IDENTIFIER_REQUIRED;
      return ESR_SUCCESS;
    default:
      return ESR_INVALID_STATE;
  }
}


ESR_ReturnCode handle_EndOfStatement(ExpressionParser* self, SymbolTable* symtable, ExpressionEvaluator* evaluator)
{
  size_t i;
  LCHAR *operands[MAX_RHS_IDENTIFIERS];
  LCHAR result[MAX_SEMPROC_VALUE];
  size_t resultLen;
  LCHAR *p;
  size_t offset;
  ESR_ReturnCode rc;
  
  switch (self->state)
  {
    case OP_ANY_REQUIRED:
      /* LHS cannot be a constant!!! */
      if (self->lhs[0] == STRING_DELIM)
      {
        PLogError(L("ESR_INVALID_ARGUMENT: %s"), self->lhs);
        return ESR_INVALID_ARGUMENT;
      }
      
      
      /* check to see whether identifiers are constants or variables
       and remap to the value of variable when necessary */
      for (i = 0; i < self->idCount; i++)
      {
        if (self->identifiers[i][0] != STRING_DELIM)
          CHKLOG(rc, ST_getKeyValue(symtable, self->identifiers[i], &operands[i]));
        else
        {
          /* be sure to remove the string delimiters before I work with identifiers */
          
          /* remove leading delim */
          p = operands[i] = &self->identifiers[i][1];
          offset = 0;
          
          /* replace all \' by ' */
          while (*p != '\'')
          {
            if (*p == '\\')
            {
              ++offset;
              ++p;
            }
            if (offset > 0)
            {
              *(p - offset) = *p;
            }
            ++p;
          }
          *(p - offset) = '\0';
        }
      }
      
      /* if expression has to be evaluated */
      if (self->needToExecuteFunction)
      {
        if (self->pfunction)
        {
          result[0] = EO_STRING; /* empty it by default */
          resultLen = sizeof(result);
          CHKLOG(rc, (*self->pfunction)(self->functionName, operands, self->idCount, self->userData, result, &resultLen));
          CHKLOG(rc, ST_putKeyValue(symtable, self->lhs, result));
        }
        else
          CHKLOG(rc, ST_putKeyValue(symtable, self->lhs, L("undefined")));
        self->needToExecuteFunction = ESR_FALSE;
      }
      else
      {
        /* if there is no function to execute */
        CHKLOG(rc, ST_putKeyValue(symtable, self->lhs, operands[0]));
      }
      return handle_NewStatement(self);
      
    case LHS_REQUIRED : /* for handling empty statements e.g. ";;;;" */
      return ESR_SUCCESS;
      
    default:
      PLogError(L("ESR_INVALID_ARGUMENT: %d"), self->state);
      return ESR_INVALID_STATE;
  }
CLEANUP:
  return rc;
}

ESR_ReturnCode EP_RegisterFunction(ExpressionParser* self,
                                   const LCHAR* name,
                                   void* userData,
                                   SR_SemprocFunctionPtr pfunction)
{
  FunctionCallback* callback = self->next++;
  ESR_ReturnCode rc;
  
  MEMCHK(rc, self->next, &self->functions[MAX_FUNCTION_CALLBACKS-1]);
  
  callback->pfunction = pfunction;
  callback->userData = userData;
  /* creates a new entry if it does not already exist */
  return HashMapPut(self->pfunctions, name, callback);
CLEANUP:
  return rc;
}

ESR_ReturnCode EP_LookUpFunction(ExpressionParser* self,
                                 LCHAR* name,
                                 void** userData,
                                 SR_SemprocFunctionPtr* pfunction)
{
  ESR_ReturnCode rc;
  FunctionCallback* callback;
  
  CHK(rc, HashMapGet(self->pfunctions, name, (void**) &callback));
  *userData = callback->userData;
  *pfunction = callback->pfunction;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}
