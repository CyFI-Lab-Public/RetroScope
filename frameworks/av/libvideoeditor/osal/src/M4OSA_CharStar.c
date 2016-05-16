/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 ************************************************************************
 * @file         M4DPAK_CharStar.c
 * @ingroup
  * @brief        definition of the Char Star set of functions.
 * @note         This file defines the Char Star set of functions.
 *
 ************************************************************************
*/


#include "M4OSA_CharStar.h"
#include "M4OSA_Memory.h"
#include "M4OSA_Debug.h"

/* WARNING: Specific Android */
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <errno.h>


/**
 ************************************************************************
 * @brief      This function mimics the functionality of the libc's strncpy().
 * @note       It copies exactly len2Copy characters from pStrIn to pStrOut,
 *             truncating  pStrIn or adding null characters to pStrOut if
 *             necessary.
 *             - If len2Copy is less than or equal to the length of pStrIn,
 *               a null character is appended automatically to the copied
 *               string.
 *             - If len2Copy is greater than the length of pStrIn, pStrOut is
 *               padded with null characters up to length len2Copy.
 *             - pStrOut and pStrIn MUST NOT OVERLAP (this is NOT CHECKED).
 * @param      pStrOut: (OUT) Destination character string.
 * @param      pStrIn: (IN) Source character string.
 * @param      len2Copy: (IN) Maximum number of characters from pStrIn to copy.
 * @return     M4NO_ERROR: there is no error.
 * @return     M4ERR_PARAMETER: pStrIn or pStrOut is M4OSA_NULL.
  ************************************************************************
*/
M4OSA_ERR M4OSA_chrNCopy(M4OSA_Char* pStrOut, M4OSA_Char   *pStrIn, M4OSA_UInt32 len2Copy)
{
    M4OSA_TRACE1_3("M4OSA_chrNCopy\t(M4OSA_Char* %x,M4OSA_Char* %x,M4OSA_UInt32 %ld)",
        pStrOut,pStrIn,len2Copy);
    M4OSA_DEBUG_IF2((M4OSA_NULL == pStrOut),M4ERR_PARAMETER,
                            "M4OSA_chrNCopy:\tpStrOut is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pStrIn),M4ERR_PARAMETER,
                            "M4OSA_chrNCopy:\tpStrIn is M4OSA_NULL");

    strncpy((char *)pStrOut, (const char *)pStrIn, (size_t)len2Copy);
    if(len2Copy <= (M4OSA_UInt32)strlen((const char *)pStrIn))
    {
        pStrOut[len2Copy] = '\0';
    }

    return M4NO_ERROR;
}

/**
 ************************************************************************
  * @brief      This function returns the boolean comparison of pStrIn1 and pStrIn2.
 * @note       The value returned in result is M4OSA_TRUE if the string
 *             pointed to by pStrIn1 is strictly identical to the string pointed
 *             to by pStrIn2, and M4OSA_FALSE otherwise.
 * @param      pStrIn1: (IN) First character string.
 * @param      pStrIn2: (IN) Second character string.
 * @param      cmpResult: (OUT) Comparison result.
 * @return     M4NO_ERROR: there is no error.
 * @return     M4ERR_PARAMETER: pStrIn1 pStrIn2 or cmpResult is M4OSA_NULL.
  ************************************************************************
*/
M4OSA_ERR M4OSA_chrAreIdentical(M4OSA_Char* pStrIn1, M4OSA_Char* pStrIn2,
                                                            M4OSA_Bool* pResult)
{
    M4OSA_UInt32 i32,len32;
    M4OSA_TRACE1_3("M4OSA_chrAreIdentical\t(M4OSA_Char* %x,M4OSA_Char* %x,"
        "M4OSA_Int32* %x)",pStrIn1,pStrIn2,pResult);
    M4OSA_DEBUG_IF2(M4OSA_NULL == pStrIn1, M4ERR_PARAMETER,
                               "M4OSA_chrAreIdentical:\tpStrIn1 is M4OSA_NULL");
    M4OSA_DEBUG_IF2(M4OSA_NULL == pStrIn2, M4ERR_PARAMETER,
                               "M4OSA_chrAreIdentical:\tpStrIn2 is M4OSA_NULL");
    M4OSA_DEBUG_IF2(M4OSA_NULL == pResult, M4ERR_PARAMETER,
                               "M4OSA_chrAreIdentical:\tpResult is M4OSA_NULL");

    len32 = (M4OSA_UInt32)strlen((const char *)pStrIn1);
    if(len32 != (M4OSA_UInt32)strlen((const char *)pStrIn2))
    {
        *pResult = M4OSA_FALSE;
        return M4NO_ERROR;
    }

    for(i32=0;i32<len32;i32++)
    {
        if(pStrIn1[i32] != pStrIn2[i32])
        {
            *pResult = M4OSA_FALSE;
            return M4NO_ERROR;
        }
    }

    *pResult = M4OSA_TRUE;

    return M4NO_ERROR;
}


/**
 ************************************************************************
 * @brief      This function gets a M4OSA_UInt32 from string.
 * @note       This function converts the first set of non-whitespace
 *             characters of pStrIn to a M4OSA_UInt32 value pVal, assuming a
 *             representation in base provided by the parameter base. pStrOut is
 *             set to the first character of the string following the last
 *             character of the number that has been converted.
 *             - in case of a failure during the conversion, pStrOut is not
 *               updated, and pVal is set to null.
 *             - in case of negative number, pStrOut is not updated, and pVal is
 *               set to null.
 *             - in case of numerical overflow, pVal is set to M4OSA_UINT32_MAX.
 *             - if pStrOut is not to be used, it can be set to M4OSA_NULL.
 * @param      pStrIn: (IN) Character string.
 * @param      pVal: (OUT) read value.
 * @param      pStrOut: (OUT) Output character string.
 * @param      base: (IN) Base of the character string representation.
 * @return     M4NO_ERROR: there is no error.
 * @return     M4ERR_PARAMETER: pStrIn or pVal is M4OSA_NULL.
 * @return     M4ERR_CHR_CONV_FAILED: conversion failure.
 * @return     M4WAR_CHR_NUM_RANGE: the character string represents a number
 *             greater than M4OSA_UINT32_MAX.
 * @return     M4WAR_CHR_NEGATIVE: the character string represents a negative
 *             number.
 ************************************************************************
*/
M4OSA_ERR M4OSA_chrGetUInt32(M4OSA_Char*    pStrIn,
                             M4OSA_UInt32*    pVal,
                             M4OSA_Char**    pStrOut,
                             M4OSA_chrNumBase base)
{
    M4OSA_UInt32 ul;
    char*        pTemp;

    M4OSA_TRACE1_4("M4OSA_chrGetUInt32\t(M4OSA_Char* %x, M4OSA_UInt32* %x"
        "M4OSA_Char** %x,M4OSA_chrNumBase %d)",pStrIn,pVal,pStrOut,base);
    M4OSA_DEBUG_IF2(M4OSA_NULL == pStrIn, M4ERR_PARAMETER,
                                   "M4OSA_chrGetUInt32:\tpStrIn is M4OSA_NULL");
    M4OSA_DEBUG_IF2(M4OSA_NULL == pVal, M4ERR_PARAMETER,
                                     "M4OSA_chrGetUInt32:\tpVal is M4OSA_NULL");

    errno = 0;
    switch(base)
    {
    case M4OSA_kchrDec:
        ul = strtoul((const char *)pStrIn, &pTemp, 10);
        break;
    case M4OSA_kchrHexa:
        ul = strtoul((const char *)pStrIn, &pTemp,16);
        break;
    case M4OSA_kchrOct:
        ul = strtoul((const char *)pStrIn, &pTemp,8);
        break;
    default:
        return M4ERR_PARAMETER;
    }

    /* has conversion failed ? */
    if((M4OSA_Char*)pTemp == pStrIn)
    {
        *pVal = 0;
        return M4ERR_CHR_CONV_FAILED;
    }

    /* was the number negative ? */
    if(*(pStrIn+strspn((const char *)pStrIn," \t")) == '-')
    {
        *pVal = 0;
        return M4WAR_CHR_NEGATIVE;
    }

    /* has an overflow occured ? */
    if(errno == ERANGE)
    {
        *pVal = M4OSA_UINT32_MAX;
        if(M4OSA_NULL != pStrOut)
        {
            *pStrOut = (M4OSA_Char*)pTemp;
        }
        return M4WAR_CHR_NUM_RANGE;
    }

    /* nominal case */
    *pVal = (M4OSA_UInt32)ul;
    if(M4OSA_NULL != pStrOut)
    {
        *pStrOut = (M4OSA_Char*)pTemp;
    }

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * @brief      This function gets a M4OSA_UInt16 from string.
 * @note       This function converts the first set of non-whitespace
 *             characters of pStrIn to a M4OSA_UInt16 value pVal, assuming a
 *             representation in base provided by the parameter base. pStrOut is
 *             set to the first character of the string following the last
 *             character of the number that has been converted.
 *             - in case of a failure during the conversion, pStrOut is not
 *               updated, and pVal is set to null.
 *             - in case of negative number, pStrOut is not updated, and pVal is
 *               set to null.
 *             - in case of numerical overflow, pVal is set to M4OSA_UINT16_MAX.
 *             - if pStrOut is not to be used, it can be set to M4OSA_NULL.
 * @param      pStrIn: (IN) Character string.
 * @param      pVal: (OUT) read value.
 * @param      pStrOut: (OUT) Output character string.
 * @param      base: (IN) Base of the character string representation.
 * @return     M4NO_ERROR: there is no error.
 * @return     M4ERR_PARAMETER: pStrIn or pVal is M4OSA_NULL.
 * @return     M4ERR_CHR_CONV_FAILED: conversion failure.
 * @return     M4WAR_CHR_NUM_RANGE: the character string represents a number
 *             greater than M4OSA_UINT16_MAX.
 * @return     M4WAR_CHR_NEGATIVE: the character string represents a negative
 *             number.
 ************************************************************************
*/
M4OSA_ERR M4OSA_chrGetUInt16 (M4OSA_Char* pStrIn, M4OSA_UInt16 *pVal,
                              M4OSA_Char** pStrOut, M4OSA_chrNumBase base)
{
    M4OSA_UInt32 ul;
    char*        pTemp;

    M4OSA_TRACE1_4("M4OSA_chrGetUInt16\t(M4OSA_Char* %x, M4OSA_UInt16* %x"
        "M4OSA_Char** %x,M4OSA_chrNumBase %d)",pStrIn,pVal,pStrOut,base);
    M4OSA_DEBUG_IF2(M4OSA_NULL == pStrIn,M4ERR_PARAMETER,
                                   "M4OSA_chrGetUInt16:\tpStrIn is M4OSA_NULL");
    M4OSA_DEBUG_IF2(M4OSA_NULL == pVal, M4ERR_PARAMETER,
                                     "M4OSA_chrGetUInt16:\tpVal is M4OSA_NULL");

    switch(base)
    {
    case M4OSA_kchrDec:
        ul = strtoul((const char *)pStrIn, &pTemp,10);
        break;
    case M4OSA_kchrHexa:
        ul = strtoul((const char *)pStrIn, &pTemp,16);
        break;
    case M4OSA_kchrOct:
        ul = strtoul((const char *)pStrIn, &pTemp,8);
        break;
    default:
        return M4ERR_PARAMETER;
    }

    /* has conversion failed ? */
    if((M4OSA_Char*)pTemp == pStrIn)
    {
        *pVal = 0;
        return M4ERR_CHR_CONV_FAILED;
    }

    /* was the number negative ? */
    if(*(pStrIn+strspn((const char *)pStrIn," \t")) == '-')
    {
        *pVal = 0;
        return M4WAR_CHR_NEGATIVE;
    }

    /* has an overflow occured ? */
    if(ul>M4OSA_UINT16_MAX)
    {
        *pVal = M4OSA_UINT16_MAX;
        if(M4OSA_NULL != pStrOut)
        {
            *pStrOut = (M4OSA_Char*)pTemp;
        }
        return M4WAR_CHR_NUM_RANGE;
    }

    /* nominal case */
    *pVal = (M4OSA_UInt16)ul;
    if(M4OSA_NULL != pStrOut)
    {
        *pStrOut = (M4OSA_Char*)pTemp;
    }
    return M4NO_ERROR;
}

M4OSA_ERR M4OSA_chrSPrintf(M4OSA_Char  *pStrOut, M4OSA_UInt32 strOutMaxLen,
                           M4OSA_Char   *format, ...)
{
    va_list       marker;
    M4OSA_Char   *pTemp;
    M4OSA_Char   *percentPointer;
    M4OSA_Char   *newFormat;
    M4OSA_Int32  newFormatLength=0;
    M4OSA_UInt32  count_ll = 0;
    M4OSA_UInt32  count_tm = 0;
    M4OSA_UInt32  count_aa = 0;
    M4OSA_UInt32  count;
    M4OSA_UInt32  nbChar;
    M4OSA_Int32     err;
    M4OSA_Char flagChar[]             = "'-+ #0";
    M4OSA_Char widthOrPrecisionChar[] = "*0123456789";
    M4OSA_Char otherPrefixChar[]      = "hlL";
    M4OSA_Char conversionChar[]       = "diouxXnfeEgGcCsSp%";

    M4OSA_TRACE1_3("M4OSA_chrSPrintf\t(M4OSA_Char* %x, M4OSA_UInt32 %ld"
        "M4OSA_Char* %x)",pStrOut,strOutMaxLen,format);
    M4OSA_DEBUG_IF2(M4OSA_NULL == pStrOut, M4ERR_PARAMETER,
                                    "M4OSA_chrSPrintf:\tpStrOut is M4OSA_NULL");
    M4OSA_DEBUG_IF2(M4OSA_NULL == format, M4ERR_PARAMETER,
                                     "M4OSA_chrSPrintf:\tformat is M4OSA_NULL");

    va_start(marker,format);

    /* count the number of %[flags][width][.precision]ll[conversion] */
    pTemp = format;
    while(*pTemp)
    {
        percentPointer = (M4OSA_Char *)strchr((const char *)pTemp,'%'); /* get the next percent character */
        if(!percentPointer)
            break;                            /* "This is the End", (c) J. Morrisson */
        pTemp = percentPointer+1;           /* span it */
        if(!*pTemp)
            break;                            /* "This is the End", (c) J. Morrisson */
        pTemp += strspn((const char *)pTemp,(const char *)flagChar);    /* span the optional flags */
        if(!*pTemp)
            break;                            /* "This is the End", (c) J. Morrisson */
        pTemp += strspn((const char *)pTemp,(const char *)widthOrPrecisionChar); /* span the optional width */
        if(!*pTemp)
            break;                            /* "This is the End", (c) J. Morrisson */
        if(*pTemp=='.')
        {
            pTemp++;
            pTemp += strspn((const char *)pTemp, (const char *)widthOrPrecisionChar); /* span the optional precision */
        }
        if(!*pTemp)
            break;                            /* "This is the End", (c) J. Morrisson */
        if(strlen((const char *)pTemp)>=2)
        {
            if(!strncmp((const char *)pTemp,"ll",2))
            {
                count_ll++;                 /* I got ONE */
                pTemp +=2;                  /* span the "ll" prefix */
            }
            else if(!strncmp((const char *)pTemp,"tm",2))
            {
                count_tm++;
                pTemp +=2;
            }
            else if(!strncmp((const char *)pTemp,"aa",2))
            {
                count_aa++;
                pTemp +=2;
            }
        }
        pTemp += strspn((const char *)pTemp, (const char *)otherPrefixChar); /* span the other optional prefix */
        if(!*pTemp)
            break;                        /* "This is the End", (c) J. Morrisson */
        pTemp += strspn((const char *)pTemp, (const char *)conversionChar);
        if(!*pTemp)
            break;                        /* "This is the End", (c) J. Morrisson */
    }

    count = count_ll + count_tm + count_aa;

    if(!count)
    {
        err= vsnprintf((char *)pStrOut, (size_t)strOutMaxLen + 1, (const char *)format, marker);
        va_end(marker);
        if ((err<0) || ((M4OSA_UInt32)err>strOutMaxLen))
        {
            pStrOut[strOutMaxLen] = '\0';
            return M4ERR_CHR_STR_OVERFLOW;
        }
        else
        {
            return M4NO_ERROR;
        }
    }


    newFormatLength = strlen((const char *)format) + 1;

    newFormatLength -= (count_ll+count_tm+count_aa);

    newFormat =(M4OSA_Char*)M4OSA_32bitAlignedMalloc(newFormatLength,
        M4OSA_CHARSTAR,(M4OSA_Char*)"M4OSA_chrPrintf: newFormat");
    if(M4OSA_NULL == newFormat)
        return M4ERR_ALLOC;
    newFormat[newFormatLength-1] = '\0';
    pTemp = newFormat;

    /* copy format to newFormat, replacing %[flags][width][.precision]ll[conversion]
     * by %[flags][width][.precision]I64[conversion] */
    while(*format)
    {
        nbChar = strcspn((const char *)format, "%");
        if(nbChar)
        {
            strncpy((char *)pTemp, (const char *)format, nbChar);      /* copy characters before the % character */
            format +=nbChar;
            pTemp   +=nbChar;
        }
        if(!*format) break;
        *pTemp++ = *format++;                 /* copy the % character */
        nbChar = strspn((const char *)format, (const char *)flagChar);
        if(nbChar)
        {
            strncpy((char *)pTemp, (const char *)format, nbChar);      /* copy the flag characters */
            format +=nbChar;
            pTemp   +=nbChar;
        }
        if(!*format) break;
        nbChar = strspn((const char *)format, (const char *)widthOrPrecisionChar);
        if(nbChar)
        {
            strncpy((char *)pTemp, (const char *)format, nbChar);      /* copy the width characters */
            format +=nbChar;
            pTemp   +=nbChar;
        }
        if(!*format) break;
        if(*format=='.')
        {
            *pTemp++ = *format++;              /* copy the dot character */
            if(!format) break;
            nbChar = strspn((const char *)format, (const char *)widthOrPrecisionChar);
            if(nbChar)
            {
                strncpy((char *)pTemp, (const char *)format, nbChar);      /* copy the width characters */
                format +=nbChar;
                pTemp   +=nbChar;
            }
            if(!format) break;
        }
        if(strlen((const char *)format)>=2)
        {
            if(!strncmp((const char *)format, "ll", 2))
            {
                *pTemp++ = 'l'; /* %l */
                format +=2;                         /* span the "ll" prefix */
            }
            else if(!strncmp((const char *)format, "tm", 2))
            {
                *pTemp++ = 'l'; /* %l */
                format +=2;                         /* span the "tm" prefix */
            }
            else if(!strncmp((const char *)format, "aa", 2))
            {
                *pTemp++ = 'l';
                format +=2;                         /* span the "aa" prefix */
            }
        }
        nbChar = strspn((const char *)format, (const char *)otherPrefixChar);
        if(nbChar)
        {
            strncpy((char *)pTemp, (const char *)format, nbChar);      /* copy the other Prefix */
            format +=nbChar;
            pTemp   +=nbChar;
        }
        if(!*format) break;
        nbChar = strspn((const char *)format, (const char *)conversionChar);
        if(nbChar)
        {
            strncpy((char *)pTemp, (const char *)format, nbChar);
            format += nbChar;
            pTemp   += nbChar;
        }
        if(!*format) break;
    }

    /* Zero terminate the format string. */
    (*pTemp) = '\0';

    err = vsnprintf((char *)pStrOut, (size_t)strOutMaxLen + 1, (const char *)newFormat, marker);
    va_end(marker);
    free(newFormat);
    if ((err<0) || ((M4OSA_UInt32)err>strOutMaxLen))
    {
        pStrOut[strOutMaxLen] = '\0';
        return M4ERR_CHR_STR_OVERFLOW;
    }
    else
    {
        return M4NO_ERROR;
    }
}

