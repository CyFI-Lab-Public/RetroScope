/*---------------------------------------------------------------------------*
 *  text_parser.c  *
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

#include"pstdio.h"
#include"srec_context.h"
#include"astar.h"

#include "passert.h"
#include "portable.h"


#define MAX_LOCAL_LEN 256
#define PARSE_PASS 0
#define PARSE_FAIL 1


static int check_word_path(srec_context* context, arc_token* atok,
                           const char* transcription, int tlen)
{
  const char    *wd, *p;
  char          *q;
  arc_token*    next_atok;
  wordID        wdID;
  int           q_position;

  if ( strlen ( transcription ) >= MAX_LOCAL_LEN - 1)
  {
    PLogError("Transcription too long [%s]\n", transcription);
    return PARSE_FAIL;
  }

  while (1) {
    char copy_of_word[MAX_LOCAL_LEN]; /* save heap on recursive function */
    
    /* wd points to the first char of last word */
    wd = transcription;
    if (tlen > 0)
    {
      for (wd = transcription + tlen - 1; wd > transcription; wd--)
      {
        if (*wd == ' ')
        {
          wd++;
          break;
        }
      }
    }
    for (p = wd, q = copy_of_word; ; p++, q++)
    {
      q_position = q - copy_of_word;
      if (q_position < 0 || (size_t)q_position >= MAX_LOCAL_LEN)
      {
        PLogError("Word too long in transcription [%s]\n", transcription);
        return PARSE_FAIL;
      }
      *q = *p;
      if (*p == ' ' || *p == '\0')
      {
        *q = 0;
        break;
      }
    }
    wdID = wordmap_find_index(context->olabels, copy_of_word);
    
    if (wdID < MAXwordID)
    {
      next_atok = get_arc_for_word(atok, wdID, context, context->beg_silence_word);
    }
    else
    {
      next_atok = get_arc_for_word_without_slot_annotation(atok, wd, context, context->beg_silence_word);
      if (!next_atok) return PARSE_FAIL;
    }
    
    if (!next_atok) return PARSE_FAIL;
  
    int whether_final_atok = 0;
    arc_token* tmp;
    for (tmp = ARC_TOKEN_PTR(context->arc_token_list, next_atok->first_next_arc); tmp != NULL;
         tmp = ARC_TOKEN_PTR(context->arc_token_list, tmp->next_token_index))
    {
      if (tmp->ilabel == MAXwordID) whether_final_atok = 1;
    }
    
    if (wd == transcription && whether_final_atok) return PARSE_PASS;
    if (wd == transcription) return PARSE_FAIL;
    tlen--;
    while (transcription[tlen] != ' ' && tlen > 0) tlen--;
  
    atok = next_atok;
  }
}

int FST_CheckPath_Simple(srec_context* context, const char* transcription)
{
  arc_token* atok = &context->arc_token_list[0];
  int transcription_len = strlen(transcription);
  int rc;
  
  for (; transcription_len > 0; transcription_len--)
    if (transcription[transcription_len-1] != ' ') break;
  rc = check_word_path(context, atok, transcription, transcription_len);
  return rc;
}

int FST_CheckPath_Complex(srec_context* context, const char* transcription,
                          char* literal, size_t max_literal_len)
{
  int i, j, rc;
  int num_spaces;
  char copy_of_transcription[MAX_LOCAL_LEN];
  char* spaces[24], *p; /* can't go too high here!! */
  ASSERT(strlen(transcription) < MAX_LOCAL_LEN);
  
  strcpy(copy_of_transcription, transcription);
  for (num_spaces = 0, p = copy_of_transcription; *p; p++)
  {
    if (*p == ' ')
    {
      if ((size_t)num_spaces >= sizeof(spaces) / sizeof(char*))
      {
        PLogError("FST_CheckPath_Complex() failed on too many words\n");
        return PARSE_FAIL;
      }
      spaces[num_spaces++] = p;
    }
  }
  
  if (num_spaces == 0)
  {
    rc = FST_CheckPath_Simple(context, transcription);
    if (rc == PARSE_PASS)
    {
      ASSERT(strlen(copy_of_transcription) < max_literal_len);
      strcpy(literal, copy_of_transcription);
    }
    return rc;
  }
  
  for (i = 0; i < (1 << num_spaces); i++)
  {
    /* find the space pointers */
    for (j = 0; j < num_spaces; j++)
      *spaces[j] = i & (1 << j) ? '_' : ' ';
    /* check each word, potentially within a rule! */
    for (p = strtok(copy_of_transcription, " "); p; p = strtok(NULL, " "))
    {
      wordID k, wdid = wordmap_find_index(context->olabels, p);
      if (wdid < MAXwordID) continue;
      for (k = 1; k < context->olabels->num_slots; k++)
      {
        wdid = wordmap_find_index_in_rule(context->olabels, p, k);
        if (wdid < MAXwordID) break;
      }
      if (wdid == MAXwordID)
        goto next_i;
    }
    /* fix the nulls back */
    for (j = 0; j < num_spaces; j++)
      *spaces[j] = i & (1 << j) ? '_' : ' ';
    rc = FST_CheckPath_Simple(context, copy_of_transcription);
    if (rc == PARSE_PASS)
    {
      ASSERT(strlen(copy_of_transcription) < max_literal_len);
      strcpy(literal, copy_of_transcription);
      return rc;
    }
next_i:
    continue;
  }
  return PARSE_FAIL;
}

static void clean_up_sentence(char* s);

int FST_CheckPath(srec_context* context, const char* transcription,
                  char* literal, size_t max_literal_len)
{
  char mytranscription[256];
  passert(strlen(transcription) < sizeof(mytranscription));
  strcpy(mytranscription, transcription);
  clean_up_sentence(mytranscription);
  if (!context->arc_token_list)
    return 2;
  else
    return FST_CheckPath_Complex(context, mytranscription, literal, max_literal_len);
}

static void clean_up_sentence(char* s)
{
  char* p, *q;
  if (0) printf("sentence: '%s'\n", s);
  /* change speech codes to spaces */
  for (p = s; *p; p++)
  {
    if (*p == '[')
      for (;*p && *p != ']'; p++)
        *p = ' ';
    if (*p == ']') *p = ' ';
  }
  /* trim leading spaces */
  for (p = s; *p == ' ';)
    for (q = p; *q; q++) *q = *(q + 1);
  /* trim middle spaces */
  for (p = s; p && *p;)
  {
    if (!*p) break;
    p = strchr(p, ' ');
    if (!p) break;
    for (;*(p + 1) == ' ';)
      for (q = p; *q; q++) *q = *(q + 1);
    p++;
  }
  /* trim ending spaces */
  for (p = s + strlen(s); p != s;)
    if (*(--p) == ' ') *p = 0;
    else break;
    
  if (0) printf("clean_sentence: '%s'\n", s);
}



