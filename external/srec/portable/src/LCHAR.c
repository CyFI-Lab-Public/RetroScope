/*---------------------------------------------------------------------------*
 *  LCHAR.c  *
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

#include "LCHAR.h"
#include "plog.h"
#include "pmemory.h"

#define MTAG NULL

ESR_ReturnCode lstrtrim(LCHAR* text)
{
  size_t beginning, ending, len;
  
  len = LSTRLEN(text);
  
  /* locating first non-whitespace character from beginning */
  for (beginning = 0; beginning < len && LISSPACE(text[beginning]); ++beginning);
  /* locating first non-whitespace character from end */
  for (ending = len - 1; ending > beginning && LISSPACE(text[ending]); --ending);
  
  if (beginning > 0 && beginning <= ending)
    LMEMMOVE(text, text + beginning, ending - beginning + 1);
  text[ending-beginning+1] = '\0';
  return ESR_SUCCESS;
}

ESR_ReturnCode lstrinsert(const LCHAR* source, LCHAR* target, size_t offset, size_t* len)
{
  ESR_ReturnCode rc;
  
  if (source == NULL || target == NULL || len == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  if (LSTRLEN(source) + LSTRLEN(target) + 1 > *len)
  {
    *len = LSTRLEN(source) + LSTRLEN(target) + 1;
    rc = ESR_BUFFER_OVERFLOW;
    PLOG_DBG_TRACE((ESR_rc2str(rc)));
    goto CLEANUP;
  }
  memmove(target + offset + LSTRLEN(source), target + offset, LSTRLEN(target + offset) + 1);
  LSTRNCPY(target + offset, source, LSTRLEN(source));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode lstrreplace(LCHAR* text, const LCHAR source, const LCHAR target)
{
  LCHAR* index;
  
  while (ESR_TRUE)
  {
    index = LSTRCHR(text, source);
    if (index == NULL)
      break;
    *index = target;
  }
  return ESR_SUCCESS;
}

ESR_ReturnCode lstrtoi(const LCHAR* text, int* result, int base)
{
  LCHAR* endPtr;
  
  if (result == NULL)
    return ESR_INVALID_ARGUMENT;
  *result = LSTRTOL(text, &endPtr, base);
  if (endPtr == text || (!LISSPACE(*endPtr) && *endPtr != L('\0')))
    return ESR_INVALID_ARGUMENT;
  return ESR_SUCCESS;
}

ESR_ReturnCode lstrtoui(const LCHAR* text, unsigned int* result, int base)
{
  LCHAR* endPtr;
  
  if (result == NULL)
    return ESR_INVALID_ARGUMENT;
  *result = LSTRTOUL(text, &endPtr, base);
  if (endPtr == text || (!LISSPACE(*endPtr) && *endPtr != L('\0')))
    return ESR_INVALID_ARGUMENT;
  return ESR_SUCCESS;
}

ESR_ReturnCode lstrtof(const LCHAR* text, float* result)
{
  LCHAR* endPtr;
  
  if (result == NULL)
    return ESR_INVALID_ARGUMENT;
  *result = (float) LSTRTOD(text, &endPtr);
  if (endPtr == text || (!LISSPACE(*endPtr) && *endPtr != L('\0')))
    return ESR_INVALID_ARGUMENT;
  return ESR_SUCCESS;
}

ESR_ReturnCode lstrtob(const LCHAR* text, ESR_BOOL* result)
{
  ESR_ReturnCode rc = ESR_SUCCESS;
  int compare;
  unsigned int temp;
  
  if (result == NULL)
    return ESR_INVALID_ARGUMENT;
  CHKLOG(rc, lstrcasecmp(text, L("true"), &compare));
  if (compare == 0)
  {
    *result = ESR_TRUE;
    return ESR_SUCCESS;
  }
  CHKLOG(rc, lstrcasecmp(text, L("yes"), &compare));
  if (compare == 0)
  {
    *result = ESR_TRUE;
    return ESR_SUCCESS;
  }
  CHKLOG(rc, lstrcasecmp(text, L("false"), &compare));
  if (compare == 0)
  {
    *result = ESR_FALSE;
    return ESR_SUCCESS;
  }
  CHKLOG(rc, lstrcasecmp(text, L("no"), &compare));
  if (compare == 0)
  {
    *result = ESR_FALSE;
    return ESR_SUCCESS;
  }
  
  /* Check for boolean expressed as an integer value */
  CHK(rc, lstrtoui(text, &temp, 10));
  *result = (temp != 0);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode LCHARGetInt( LCHAR* text, int* value, LCHAR** finalPosition)
{
  LCHAR *beg, *end;
  LCHAR temp;
  ESR_ReturnCode rc;
  
  /* Skip whitespace */
  for (beg = text; *beg != L('\0') && LISSPACE(*beg); ++beg);
  if (beg == NULL)
    return ESR_INVALID_ARGUMENT; /* invalid command syntax */
  /* Find next whitespace */
  for (end = beg; *end != L('\0') && !LISSPACE(*end); ++end);
  if (end == NULL)
    return ESR_INVALID_ARGUMENT; /* invalid command syntax */
    
  temp = *end;
  *end = L('\0');
  rc = lstrtoi(beg, value, 10);
  if (rc != ESR_SUCCESS)
  {
    *end = temp;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  *end = temp;
  if (finalPosition != NULL)
    *finalPosition = end;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode lstrlwr(LCHAR* string)
{
  if (string)
  {
    while (*string)
    {
      if (LISALPHA(*string))
        *string = (LCHAR) LTOLOWER(*string);
      ++string;
    }
  }
  else
    return ESR_INVALID_ARGUMENT;
    
  return ESR_SUCCESS;
}

ESR_ReturnCode lstrupr(LCHAR* string)
{
  if (string)
  {
    while (*string)
    {
      if (LISALPHA(*string))
        *string = (LCHAR) LTOUPPER(*string);
      ++string;
    }
  }
  else
    return ESR_INVALID_ARGUMENT;
    
  return ESR_SUCCESS;
}

/* strcasecmp is not POSIX.4 API */
ESR_ReturnCode lstrcasecmp(const LCHAR *string1, const LCHAR *string2, int *result)
{

  if (!string1 || !string2)
    return ESR_INVALID_ARGUMENT;
    
  while (LTOUPPER(*string1) == LTOUPPER(*string2++))
  {
    if (!*string1++)
    {
      *result = 0;
      return ESR_SUCCESS;
    }
  }
  
  *result = LTOUPPER(*string1) - LTOUPPER(*--string2);
  return ESR_SUCCESS;
}

/**
 * This code is from MS SDK: C:\PROGRAM FILES\MICROSOFT SDK\src\crt\xtoa.c
 * Buffer overflow checking is left up to the caller.
 *
 * @param value Number to be converted
 * @param string String result
 * @param radix Base of value; must be in the range 2 - 36
 */
static void pxtoa(unsigned long val, LCHAR *buf, unsigned radix, int is_neg)
{
  LCHAR *p;                /* pointer to traverse string */
  LCHAR *firstdig;         /* pointer to first digit */
  LCHAR temp;              /* temp char */
  unsigned digval;        /* value of digit */
  
  p = buf;
  
  if (is_neg)
  {
    /* negative, so output '-' and negate */
    *p++ = '-';
    val = (unsigned long)(-(long)val);
  }
  
  firstdig = p;           /* save pointer to first digit */
  
  do
  {
    digval = (unsigned)(val % radix);
    val /= radix;       /* get next digit */
    
    /* convert to ascii and store */
    if (digval > 9)
      *p++ = (LCHAR)(digval - 10 + 'a');  /* a letter */
    else
      *p++ = (LCHAR)(digval + '0');       /* a digit */
  }
  while (val > 0);
  
  /* We now have the digit of the number in the buffer, but in reverse
     order.  Thus we reverse them now. */
  
  *p-- = '\0';            /* terminate string; p points to last digit */
  
  do
  {
    temp = *p;
    *p = *firstdig;
    *firstdig = temp;   /* swap *p and *firstdig */
    --p;
    ++firstdig;         /* advance to next two digits */
  }
  while (firstdig < p); /* repeat until halfway */
}

/*
 * Convert an integer to a string.
 */
ESR_ReturnCode litostr(int value, LCHAR *string, size_t *len, int radix)
{
  size_t size;
  /* pxtoa() is guaranteed not to overflow past 33 characters */
  LCHAR buffer[33];
  
  if (!string)
    return ESR_INVALID_ARGUMENT;
    
  if (radix == 10 && value < 0)
    pxtoa((unsigned long) value, buffer, radix, 1);
  else
    pxtoa((unsigned long) value, buffer, radix, 0);
    
  size = LSTRLEN(buffer);
  
  if (size >= *len)   /* + null-terminated character */
  {
    *len = size;
    return ESR_BUFFER_OVERFLOW;
  }
  else
    LSTRCPY(string, buffer);
    
  return ESR_SUCCESS;
}


/* Convert an unsigned long integer to a string. */
ESR_ReturnCode lultostr(unsigned long  value, LCHAR *string, size_t *len, int radix)
{
  size_t size;
  LCHAR buffer[33];
  
  if (!string)
    return ESR_INVALID_ARGUMENT;
    
  pxtoa(value, buffer, radix, 0);
  
  size = LSTRLEN(buffer);
  
  if (size >= *len)   /* + null-terminated character */
  {
    *len = size;
    return ESR_BUFFER_OVERFLOW;
  }
  else
  {
    *len = size;
    LSTRCPY(string, buffer);
  }
  
  return ESR_SUCCESS;
}
