/*
 * Copyright 2010, The Android Open Source Project
 * Copyright 2010, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "harfbuzz-external.h"

#include   <unicode/uchar.h>
#include   <unicode/utypes.h>

/*
   We use ICU APIs to get the character's Unicode property.
   This module replaces the harfbuzz-unicode-tables.c which is
   using static Unicode tables.
*/

static int
hb_category_for_char(HB_UChar32 ch) {
  switch (u_charType(ch)) {
    case U_CONTROL_CHAR:
      return HB_Other_Control;
    case U_FORMAT_CHAR:
      return HB_Other_Format;
    case U_UNASSIGNED:
      return HB_Other_NotAssigned;
    case U_PRIVATE_USE_CHAR:
      return HB_Other_PrivateUse;
    case U_SURROGATE:
      return HB_Other_Surrogate;
    case U_LOWERCASE_LETTER:
      return HB_Letter_Lowercase;
    case U_MODIFIER_LETTER:
      return HB_Letter_Modifier;
    case U_OTHER_LETTER:
      return HB_Letter_Other;
    case U_TITLECASE_LETTER:
      return HB_Letter_Titlecase;
    case U_UPPERCASE_LETTER:
      return HB_Letter_Uppercase;
    case U_COMBINING_SPACING_MARK:
      return HB_Mark_SpacingCombining;
    case U_ENCLOSING_MARK:
      return HB_Mark_Enclosing;
    case U_NON_SPACING_MARK:
      return HB_Mark_NonSpacing;
    case U_DECIMAL_DIGIT_NUMBER :
      return HB_Number_DecimalDigit;
    case U_LETTER_NUMBER:
      return HB_Number_Letter;
    case U_OTHER_NUMBER:
      return HB_Number_Other;
    case U_CONNECTOR_PUNCTUATION:
      return HB_Punctuation_Connector;
    case U_DASH_PUNCTUATION:
      return HB_Punctuation_Dash;
    case U_END_PUNCTUATION:
      return HB_Punctuation_Close;
    case U_FINAL_PUNCTUATION:
      return HB_Punctuation_FinalQuote;
    case U_INITIAL_PUNCTUATION:
      return HB_Punctuation_InitialQuote;
    case U_OTHER_PUNCTUATION:
      return HB_Punctuation_Other;
    case U_START_PUNCTUATION:
      return HB_Punctuation_Open;
    case U_CURRENCY_SYMBOL:
      return HB_Symbol_Currency;
    case U_MODIFIER_SYMBOL:
      return HB_Symbol_Modifier;
    case U_MATH_SYMBOL:
      return HB_Symbol_Math;
    case U_OTHER_SYMBOL:
      return HB_Symbol_Other;
    case U_LINE_SEPARATOR:
      return HB_Separator_Line;
    case U_PARAGRAPH_SEPARATOR:
      return HB_Separator_Paragraph;
    case U_SPACE_SEPARATOR:
      return HB_Separator_Space;
    default:
      return HB_Symbol_Other;
  }
}

HB_LineBreakClass
HB_GetLineBreakClass(HB_UChar32 ch) {
  switch ((ULineBreak)u_getIntPropertyValue(ch, UCHAR_LINE_BREAK)) {
    case U_LB_MANDATORY_BREAK:
      return HB_LineBreak_BK;
    case U_LB_CARRIAGE_RETURN:
      return HB_LineBreak_CR;
    case U_LB_LINE_FEED:
      return HB_LineBreak_LF;
    case U_LB_COMBINING_MARK:
      return HB_LineBreak_CM;
    case U_LB_SURROGATE:
      return HB_LineBreak_SG;
    case U_LB_ZWSPACE:
      return HB_LineBreak_ZW;
    case U_LB_INSEPARABLE:
      return HB_LineBreak_IN;
    case U_LB_GLUE:
      return HB_LineBreak_GL;
    case U_LB_CONTINGENT_BREAK:
      return HB_LineBreak_AL;
    case U_LB_SPACE:
      return HB_LineBreak_SP;
    case U_LB_BREAK_AFTER:
      return HB_LineBreak_BA;
    case U_LB_BREAK_BEFORE:
      return HB_LineBreak_BB;
    case U_LB_BREAK_BOTH:
      return HB_LineBreak_B2;
    case U_LB_HYPHEN:
      return HB_LineBreak_HY;
    case U_LB_NONSTARTER:
      return HB_LineBreak_NS;
    case U_LB_OPEN_PUNCTUATION:
      return HB_LineBreak_OP;
    case U_LB_CLOSE_PUNCTUATION:
      return HB_LineBreak_CL;
    case U_LB_QUOTATION:
      return HB_LineBreak_QU;
    case U_LB_EXCLAMATION:
      return HB_LineBreak_EX;
    case U_LB_IDEOGRAPHIC:
      return HB_LineBreak_ID;
    case U_LB_NUMERIC:
      return HB_LineBreak_NU;
    case U_LB_INFIX_NUMERIC:
      return HB_LineBreak_IS;
    case U_LB_BREAK_SYMBOLS:
      return HB_LineBreak_SY;
    case U_LB_ALPHABETIC:
      return HB_LineBreak_AL;
    case U_LB_PREFIX_NUMERIC:
      return HB_LineBreak_PR;
    case U_LB_POSTFIX_NUMERIC:
      return HB_LineBreak_PO;
    case U_LB_COMPLEX_CONTEXT:
      return HB_LineBreak_SA;
    case U_LB_AMBIGUOUS:
      return HB_LineBreak_AL;
    case U_LB_UNKNOWN:
      return HB_LineBreak_AL;
    case U_LB_NEXT_LINE:
      return HB_LineBreak_AL;
    case U_LB_WORD_JOINER:
      return HB_LineBreak_WJ;
    case U_LB_JL:
      return HB_LineBreak_JL;
    case U_LB_JV:
      return HB_LineBreak_JV;
    case U_LB_JT:
      return HB_LineBreak_JT;
    case U_LB_H2:
      return HB_LineBreak_H2;
    case U_LB_H3:
      return HB_LineBreak_H3;
    default:
      return HB_LineBreak_AL;
  }
}

int
HB_GetUnicodeCharCombiningClass(HB_UChar32 ch) {
  return u_getCombiningClass(ch);
}

void
HB_GetUnicodeCharProperties(HB_UChar32 ch,
                            HB_CharCategory *category,
                            int *combiningClass) {
  *category = hb_category_for_char(ch);
  *combiningClass = u_getCombiningClass(ch);
}

HB_CharCategory
HB_GetUnicodeCharCategory(HB_UChar32 ch) {
  return hb_category_for_char(ch);
}
