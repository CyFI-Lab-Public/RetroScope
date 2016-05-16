/*---------------------------------------------------------------------------*
 *  riff.c  *
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

#include "plog.h"
#include "riff.h"

#define MTAG NULL

typedef struct ChunkContext_t
{
  char tag[4];
  long  start;
  int  length;
}
ChunkContext;

typedef enum
{
  FIND_RIFF,
  FIND_CHUNK,
  FIND_LIST
} DescendType;

int isLittleEndian()
{
  char b[4];
  
  *(int *)b = 1;
  return (int)b[0];
}

/* waveReadFunc
 * - converts data to an array of signed shorts
 * - fills in *length with the number of samples converted
 * - allocates memory for *samples
 * - returns GS_OK if conversion was successful or GS_ERROR and an error
 *    message in res if not.  If the conversion fails the function must free
 *    all the memory it had allocated before returning.
 * On entry
 *  wf   - points to the WaveFormat structure that describes the data format
 *  cb   - data read from the RIFF file
 *  data - descriptor for the "data" chunk
 */
typedef ESR_ReturnCode(waveReadFunc)(WaveFormat *wf, ChunkContext *data,
                                     char *cb, short **samples, int *length, int doSwap);
                                     
static ESR_ReturnCode readPCMWave(WaveFormat *wf, ChunkContext *data,
                                  char *cb, short **samples, int *length, int doSwap);
static ESR_ReturnCode readMulawWave(WaveFormat *wf, ChunkContext *data,
                                    char *cb, short **samples, int *length, int doSwap);
static ESR_ReturnCode readAlawWave(WaveFormat *wf, ChunkContext *data,
                                   char *cb, short **samples, int *length, int doSwap);
                                   
static struct
{
  int id;
  waveReadFunc *func;
}
WaveCodecs[] = {
                 {WAVEFORMAT_PCM, readPCMWave},
                 {WAVEFORMAT_MULAW, readMulawWave},
                 {WAVEFORMAT_ALAW, readAlawWave},
                 {0, 0},
               };
               
/************* FIXME: regroup all swap routines outahere;
 *                    ditto for audio conversion routines
 */

static void swapInt(int *i)
{
  char *a = (char *)i, t;
  t = a[0];
  a[0] = a[3];
  a[3] = t;
  t = a[1];
  a[1] = a[2];
  a[2] = t;
}

static void swapShort(short *s)
{
  char *a = (char *)s, t;
  t = a[0];
  a[0] = a[1];
  a[1] = t;
}

static int swapConstInt(const int value)
{
  int converted = value;
  unsigned char *cp = (unsigned char *) & converted;
  
  *cp ^= *(cp + 3);
  *(cp + 3) ^= *cp;
  *cp ^= *(cp + 3);
  *(cp + 1) ^= *(cp + 2);
  *(cp + 2) ^= *(cp + 1);
  *(cp + 1) ^= *(cp + 2);
  
  return converted;
}

static short swapConstShort(const short value)
{
  short converted = value;
  unsigned char *cp = (unsigned char *) & converted;
  unsigned char tmp = *cp;
  
  *cp = *(cp + 1);
  *++cp = tmp;
  
  return converted;
}

/* len == number of bytes to swap
 */
static void short_byte_swap(short *buf, int len)
{
  char *cp, *end, tmp;
  
  end = ((char *)buf) + (len << 1);
  for (cp = (char *)buf; cp < end; cp++)
  {
    tmp = *cp;
    *cp = *(cp + 1);
    *++cp = tmp;
  }
}

/* len == number of bytes to swap
 */
static void int_byte_swap(int *buf, int len)
{
  char *cp, *end;
  
  end = ((char *)buf) + (len << 2);
  for (cp = (char *)buf; cp < end; cp += 4)
  {
    *cp ^= *(cp + 3);
    *(cp + 3) ^= *cp;
    *cp ^= *(cp + 3);
    *(cp + 1) ^= *(cp + 2);
    *(cp + 2) ^= *(cp + 1);
    *(cp + 1) ^= *(cp + 2);
  }
}

static void swapWaveFormat(WaveFormat *wf)
{
  swapShort(&wf->nFormatTag);
  swapShort(&wf->nChannels);
  swapInt(&wf->nSamplesPerSec);
  swapInt(&wf->nAvgBytesPerSec);
  swapShort(&wf->nBlockAlign);
  swapShort(&wf->wBitsPerSample);
  //swapShort(&wf->cbSize);
}

static int ulaw2linear(unsigned char ulawbyte)
{
  static int exp_lut[8] =
    {
      0, 132, 396, 924, 1980, 4092, 8316, 16764
    };
  int sign, exponent, mantissa, sample;
  
  ulawbyte = ~ulawbyte;
  sign = (ulawbyte & 0x80);
  exponent = (ulawbyte >> 4) & 0x07;
  mantissa = ulawbyte & 0x0F;
  sample = exp_lut[exponent] + (mantissa << (exponent + 3));
  if (sign != 0) sample = -sample;
  return sample;
}

static int alaw2linear(unsigned char alawbyte)
{

  int sign, achord, astep, delta, sample;
  unsigned char alawcode;
  static int exp_lut[8] =
    {
      1, 1, 2, 4, 8, 16, 32, 64
    };
  alawcode = alawbyte ^ 0x55;
  sign = (alawcode & 0x80);
  achord = (alawcode >> 4) & 0x07;
  astep = alawcode & 0x0F;
  delta = ((achord == 0) ? 1 : 0);
  sample = ((2 * astep + 33) * exp_lut[achord]) - 32 * delta;
  if (sign != 0) sample = -sample;
  sample = sample * 8;
  
  return sample;
}

/* Converts PCM wave data
 *  cb: input :1

 */
static ESR_ReturnCode readPCMWave(WaveFormat *wf, ChunkContext *data,
                                  char *cb, short **samples, int *length, int doSwap)
{
  int i;
  if (wf->nChannels != 1)
  {
    //GS_SetResult(res,"PCM WAVE file contains more than one data channel",
    //GS_STATIC);
    return ESR_FATAL_ERROR;
  }
  if (wf->wBitsPerSample != 16 && wf->wBitsPerSample != 8)
  {
    //GS_SetResult(res,GS_Spf(0,"%d bits per sample PCM format not supported",
    //wf->wBitsPerSample),GS_VOLATILE);
    return ESR_FATAL_ERROR;
  }
  *length = data->length * 8 / wf->wBitsPerSample;
  *samples = MALLOC(*length * sizeof(short), MTAG);
  if (wf->wBitsPerSample == 16)
  {
    memcpy(*samples, cb, *length*sizeof(short));
    if (doSwap)
      for (i = 0;i < *length;i++) swapShort(*samples + i);
  }
  else
  {
    for (i = 0;i < *length;i++)(*samples)[i] = (short)((unsigned)(cb[i]) - 128) << 8;
  }
  return ESR_SUCCESS;
}

/* Converts CCITT u-law wave data
 */
static ESR_ReturnCode readMulawWave(WaveFormat *wf, ChunkContext *data,
                                    char *cb, short **samples, int *length, int doSwap)
{
  int i;
  if (wf->nChannels != 1)
  {
    //GS_SetResult(res,"u-law WAVE file contains more than one data channel",
    //GS_STATIC);
    return ESR_FATAL_ERROR;
  }
  if (wf->wBitsPerSample != 8)
  {
    //GS_SetResult(res,GS_Spf(0,"%d bits per sample u-law format not supported",
    //wf->wBitsPerSample),GS_VOLATILE);
    return ESR_FATAL_ERROR;
  }
  *length = data->length;
  *samples = MALLOC(*length * sizeof(short), MTAG);
  for (i = 0;i < *length;i++)
    (*samples)[i] = (short) ulaw2linear(cb[i]);
  return ESR_SUCCESS;
}

/* Converts a-law wave data
 */
static ESR_ReturnCode readAlawWave(WaveFormat *wf, ChunkContext *data,
                                   char *cb, short **samples, int *length, int doSwap)
{
  int i;
  if (wf->nChannels != 1)
  {
    //GS_SetResult(res,"u-law WAVE file contains more than one data channel",
    //GS_STATIC);
    return ESR_FATAL_ERROR;
  }
  if (wf->wBitsPerSample != 8)
  {
    //GS_SetResult(res,GS_Spf(0,"%d bits per sample u-law format not supported",
    //wf->wBitsPerSample),GS_VOLATILE);
    return ESR_FATAL_ERROR;
  }
  *length = data->length;
  samples = MALLOC(*length * sizeof(short), MTAG);
  for (i = 0;i < *length;i++)(*samples)[i] = alaw2linear(cb[i]);
  return ESR_SUCCESS;
}

/* ------------------------------------------------------------------------- */

/* RIFF INTERFACE UTILS  */

void free_swiRiff(SwiRiffStruct *swichunk)
{
  if (swichunk->segs.num_tuples)
  {
    FREE(swichunk->segs.tuples);
    swichunk->segs.num_tuples = 0;
  }
  if (swichunk->kvals.num_pairs)
  {
    FREE(swichunk->kvals.kvpairs[0].key);
    FREE(swichunk->kvals.kvpairs);
    swichunk->kvals.num_pairs = 0;
  }
}

char *getSwiRiffKVal(SwiRiffStruct *swichunk, char *key)
{
  int i;
  
  for (i = 0; i < swichunk->kvals.num_pairs; i++)
    if (! strcmp(swichunk->kvals.kvpairs[i].key, key))
      return swichunk->kvals.kvpairs[i].value;
      
  return NULL;
}

/* ------------------------------------------------------------------------- */

static int riffDescend(FILE *f, ChunkContext *c, ChunkContext *parent, DescendType t, int doSwap)
{
  char form[4], tag[4];
  int len;
  long start, end;
  
  end = 0;
  if (!parent) start = 0;
  else
  {
    start = parent->start;
    end = start + parent->length;
  }
  if (fseek(f, start, SEEK_SET) < 0)
  {
    //GS_SetResult(res,"seek failed",GS_STATIC);
    return ESR_FATAL_ERROR;
  }
  
  switch (t)
  {
  
    case FIND_RIFF:
      while (1)
      {
        if (fread(form, 1, 4, f) != 4) break;
        if (fread(&len, sizeof(int), 1, f) != 1)
          return ESR_FATAL_ERROR;
        if (doSwap)
          swapInt(&len);
        if (strncmp(form, "RIFF", 4))
        {  /* skip this non-RIFF chunk */
          if (fseek(f, (long)len, SEEK_CUR) < 0) break;
          start += len + 8;
          if (end && start >= end)
          {
            //GS_SetResult(res,"RIFF form type not found",GS_STATIC);
            return ESR_FATAL_ERROR;
          }
          continue;
        }
        if (fread(tag, 1, 4, f) != 4) break;
        if (!strncmp(tag, c->tag, 4))
        {
          c->start = start + 12;
          c->length = len - 4;
          return ESR_SUCCESS;
        }
      }
      //if(feof(f)) GS_SetResult(res,"RIFF form type not found",GS_STATIC);
      //else GS_SetResult(res,"Corrupt RIFF file",GS_STATIC);
      return ESR_FATAL_ERROR;
      
    case FIND_CHUNK:
      while (1)
      {
        if (fread(tag, 1, 4, f) != 4) break;
        if (fread(&len, sizeof(int), 1, f) != 1)
          return ESR_FATAL_ERROR;
        if (doSwap)
          swapInt(&len);
        if (!strncmp(tag, c->tag, 4))
        {
          c->start = start + 8;
          c->length = len;
          return ESR_SUCCESS;
        }
        if (fseek(f, (long)len, SEEK_CUR) < 0) break;
        start += len + 8;
        if (end && start >= end)
        {
          //GS_SetResult(res,"RIFF chunk not found",GS_STATIC);
          return ESR_FATAL_ERROR;
        }
      }
      //if(feof(f)) GS_SetResult(res,"RIFF chunk not found",GS_STATIC);
      //else GS_SetResult(res,"corrupt RIFF file",GS_STATIC);
      return ESR_FATAL_ERROR;
      
    case FIND_LIST:
      while (1)
      {
        if (fread(form, 1, 4, f) != 4) break;
        if (fread(&len, sizeof(int), 1, f) != 1)
          return ESR_FATAL_ERROR;
        if (doSwap)
          swapInt(&len);
        if (strncmp(form, "LIST", 4))
        {  /* skip this non-LIST chunk */
          if (fseek(f, (long)len, SEEK_CUR) < 0) break;
          start += len + 8;
          if (end && start >= end)
          {
            //GS_SetResult(res,"RIFF form type not found",GS_STATIC);
            return ESR_FATAL_ERROR;
          }
          continue;
        }
        if (fread(tag, 1, 4, f) != 4) break;
        if (!strncmp(tag, c->tag, 4))
        {
          c->start = start + 12;
          c->length = len - 4;
          return ESR_SUCCESS;
        }
      }
      
      //if(feof(f)) GS_SetResult(res,"RIFF form type not found",GS_STATIC);
      //else GS_SetResult(res,"Corrupt RIFF file",GS_STATIC);
      return ESR_FATAL_ERROR;
  }
  
  //GS_AppendResult(res,"bad search flag",GS_STATIC);
  return ESR_FATAL_ERROR;
}

static int riffAscend(FILE *f, ChunkContext *c)
{
  if (fseek(f, c->start + c->length, SEEK_SET) < 0)
  {
    //GS_SetResult(res,"seek failed",GS_STATIC);
    return ESR_FATAL_ERROR;
  }
  return ESR_SUCCESS;
}


static ESR_ReturnCode readSwiChunk(FILE *f,  ChunkContext *parent, SwiRiffStruct *swichunk, int doSwap)
{
  ESR_ReturnCode rc = ESR_SUCCESS;
  ChunkContext chunk, list_chunk;
  int sub_length;
  swichunk->segs.num_tuples = 0;
  swichunk->kvals.num_pairs = 0;
  
  strncpy(chunk.tag, "swi ", 4);
  if (riffDescend(f, &chunk, parent, FIND_LIST, doSwap) == ESR_SUCCESS)
  {
    /* is it as "swi " list? */
    strncpy(list_chunk.tag, "segs", 4);
    if (riffDescend(f, &list_chunk, &chunk, FIND_CHUNK, doSwap) == ESR_SUCCESS)
    {
      fread(&swichunk->segs.num_tuples, 1, sizeof(int), f);
      if (doSwap) swapInt(&swichunk->segs.num_tuples);
      
      sub_length = list_chunk.length - sizeof(int);
      if (sub_length)
      {
        swichunk->segs.tuples = MALLOC(sub_length, MTAG);
        if (!swichunk->segs.tuples)
        {
          swichunk->segs.num_tuples = 0;  /* so that the free routine will work */
          rc = ESR_OUT_OF_MEMORY;
        }
        else if (fread(swichunk->segs.tuples, 1, sub_length, f) != (size_t)sub_length)
        {
          rc = ESR_FATAL_ERROR;
        }
        if (rc != ESR_SUCCESS)
          goto swichunk_cleanup;
          
      }
      else
        swichunk->segs.tuples = NULL;
    }
    strncpy(list_chunk.tag, "kvs ", 4);
    /* start searching from after "swi" */
    if (riffDescend(f, &list_chunk, &chunk, FIND_CHUNK, doSwap) == ESR_SUCCESS)
    {
      int i, num_pairs;
      
      fread(&num_pairs, 1, sizeof(int), f);
      if (doSwap) swapInt(&num_pairs);
      swichunk->kvals.num_pairs = num_pairs;
      
      sub_length = list_chunk.length - sizeof(int);
      if (sub_length)
      {
        char *kvpair_buf = NULL;
        RiffKVPair *pairs;
        
        swichunk->kvals.kvpairs = (RiffKVPair *)CALLOC(num_pairs, sizeof(RiffKVPair), MTAG);
        kvpair_buf = CALLOC(sub_length, sizeof(char), MTAG);
        if (!swichunk->kvals.kvpairs || !kvpair_buf)
        {
          if (kvpair_buf) FREE(kvpair_buf);
          if (swichunk->kvals.kvpairs) FREE(swichunk->kvals.kvpairs);
          swichunk->kvals.num_pairs = 0;
          rc = ESR_OUT_OF_MEMORY;
          goto swichunk_cleanup;
        }
        
        swichunk->kvals.kvpairs[0].key = kvpair_buf;
        if (fread(kvpair_buf, 1, sub_length, f) != (size_t)sub_length)
        {
          rc = ESR_FATAL_ERROR;
          goto swichunk_cleanup;
        }
        for (pairs = swichunk->kvals.kvpairs, i = 0; i < swichunk->kvals.num_pairs; i++, pairs++)
        {
          pairs->key = kvpair_buf;
          kvpair_buf +=  strlen(kvpair_buf) + 1;
          pairs->value = kvpair_buf;
          kvpair_buf +=  strlen(kvpair_buf) + 1;
        }
      }
      else
        swichunk->kvals.kvpairs = NULL;
    }
  }
  /* no matter what was found or not found, return with the file pointer in
   * the state that it was upon entering this function */
  if (riffAscend(f, parent) != ESR_SUCCESS)
  {
    rc = ESR_FATAL_ERROR;
    goto swichunk_cleanup;
  }
  
swichunk_cleanup:
  if (rc == ESR_FATAL_ERROR) free_swiRiff(swichunk);
  return rc;
}


/* Reads RIFF format WAVE files
 */
ESR_ReturnCode riffReadWave2L16(FILE *f, double from, double to,
                                short **samples, int *rate, int *length, SwiRiffStruct *swichunk)
{
  ChunkContext chunk, parent;
  WaveFormat *wf;
  char *cb;
  ESR_ReturnCode rc;
  int i, ifrom, ito;
  int doSwap = ! isLittleEndian();
  
  /* find the WAVE chunk */
  strncpy(parent.tag, "WAVE", 4);
  if (riffDescend(f, &parent, NULL, FIND_RIFF, doSwap) != ESR_SUCCESS)
  {
    //GS_AppendResult(res,"\nnot a RIFF waveform audio file",NULL);
    return ESR_FATAL_ERROR;
  }
  
  /* Wave format */
  strncpy(chunk.tag, "fmt ", 4);
  if (riffDescend(f, &chunk, &parent, FIND_CHUNK, doSwap) != ESR_SUCCESS)
  {
    //GS_AppendResult(res,"\nwaveform audio file has no \"fmt \" chunk.",NULL);
    return ESR_FATAL_ERROR;
  }
  if (chunk.length < sizeof(WaveFormat))
    wf = MALLOC(sizeof(WaveFormat), MTAG);
  else
    wf = MALLOC(chunk.length, MTAG);
    
  if (fread(wf, 1, chunk.length, f) != (size_t)chunk.length)
  {
    FREE((char *)wf);
    //GS_SetResult(res,"fmt chunk read failed.",GS_STATIC);
    return ESR_FATAL_ERROR;
  }
  if (doSwap) swapWaveFormat(wf);
  *rate = wf->nSamplesPerSec;
  
  /* data chunk */
  if (riffAscend(f, &chunk) != ESR_SUCCESS)
  {
    return ESR_FATAL_ERROR;
  }
  strncpy(chunk.tag, "data", 4);
  if (riffDescend(f, &chunk, &parent, FIND_CHUNK, doSwap) != ESR_SUCCESS)
  {
    //GS_AppendResult(res,"\nwaveform audio file has no \"data\" chunk.",NULL);
    return ESR_FATAL_ERROR;
  }
  cb = MALLOC(chunk.length, MTAG); /* waveform */
  if (fread(cb, 1, chunk.length, f) != (size_t)chunk.length)
  {
    FREE((char *)wf);
    FREE((char *)cb);
    //GS_SetResult(res,"truncated \"data\" chunk",GS_STATIC);
    return ESR_FATAL_ERROR;
  }
  
  if (swichunk)
  {
    rc = readSwiChunk(f, &parent, swichunk, doSwap);
    if (rc != ESR_SUCCESS)
    {
      FREE((char *)wf);
      FREE((char *)cb);
      return rc;
    }
  }
  
  for (i = 0;WaveCodecs[i].func;i++)
  
    if (wf->nFormatTag == WaveCodecs[i].id)
    {
      rc = (WaveCodecs[i].func)(wf, &chunk, cb, samples, length, doSwap);
      FREE((char *)wf);
      FREE((char *)cb);
      if (rc != ESR_SUCCESS)
      {
        if (swichunk) free_swiRiff(swichunk);
        return rc;
      }
      /* handle 'from' and 'to' - this isn't very efficient, but
       * saves all the format conversion routines the trouble of doing so
       */
      if (from == 0 && to == -1) return ESR_SUCCESS;
      if (from > 0)
        ifrom = (int)(from * (*rate) / 1000.0);
      else
        ifrom = 0;
        
      if (to >= 0)
      {
        ito = (int)(to * (*rate) / 1000.0 + 0.5);
        if (ito > *length)
          ito = *length;
      }
      else
        ito = *length;
        
      *length = ito - ifrom;
      if (ifrom > 0) memmove(*samples, (*samples) + ifrom, (*length)*sizeof(short));
      return ESR_SUCCESS;
    }
    
  //GS_SetResult(res,GS_Spf(0,"WAVE format (id 0x%x) not supported",
  //wf->nFormatTag),GS_VOLATILE);
  //
  if (swichunk) free_swiRiff(swichunk);
  FREE((char *)cb);
  return ESR_FATAL_ERROR;
}



/* Reads RIFF format WAVE files and returns:
 *   waveform: allocated with size num_bytes
 *   audio_type is a constant string (not allocated)
 * If swichunk==NULL, does not look for swi-specific chunk,
 * Returns ESR_FATAL_ERROR if num_channels != 1
 * If and only if ESR_SUCCESS, caller must free waveform, and swichunk contents (if any)
 */
ESR_ReturnCode readRiff2Buf(FILE *f, void **waveform, unsigned int *num_bytes,
                            const wchar_t **audio_type, SwiRiffStruct *swichunk)
{
  ChunkContext chunk, parent;
  WaveFormat *wf = NULL;
  ESR_ReturnCode rc = ESR_SUCCESS;
  int doSwap = ! isLittleEndian();
  *waveform = NULL;
  
  *audio_type = NULL;  /* for error recovery higher up */
  
  if (swichunk)
  { /* for error recovery */
    swichunk->segs.num_tuples = 0;
    swichunk->kvals.num_pairs = 0;
  }
  
  /* find the WAVE chunk */
  strncpy(parent.tag, "WAVE", 4);
  if (riffDescend(f, &parent, NULL, FIND_RIFF, doSwap) != ESR_SUCCESS)
  {
    return ESR_FATAL_ERROR;
  }
  
  /* Wave format */
  strncpy(chunk.tag, "fmt ", 4);
  if (riffDescend(f, &chunk, &parent, FIND_CHUNK, doSwap) != ESR_SUCCESS)
  {
    return ESR_FATAL_ERROR;
  }
  if (chunk.length < sizeof(WaveFormat))
    wf = MALLOC(sizeof(WaveFormat), MTAG);
  else
    wf = MALLOC(chunk.length, MTAG);
    
  if (fread(wf, 1, chunk.length, f) != (size_t)chunk.length)
  {
    FREE((char *)wf);
    return ESR_FATAL_ERROR;
  }
  if (doSwap) swapWaveFormat(wf);
  
  if (wf->nChannels != 1)
  {
    FREE((char *)wf);
    return ESR_FATAL_ERROR;
  }
  if (doSwap)
  {
    swapShort(&wf->nBlockAlign);  /* usually == blockAlign / nChannels */
    swapInt(&wf->nSamplesPerSec);
    swapShort(&wf->nFormatTag);
  }
  
  /* data chunk */
  if (riffAscend(f, &chunk) != ESR_SUCCESS)
  {
    rc = ESR_FATAL_ERROR;
    goto cleanup;
  }
  
  strncpy(chunk.tag, "data", 4);
  if (riffDescend(f, &chunk, &parent, FIND_CHUNK, doSwap) != ESR_SUCCESS)
  {
    rc =  ESR_FATAL_ERROR;
    goto cleanup;
  }
  
  *num_bytes = chunk.length;  /* already swapped, if need be */
  *waveform = CALLOC(chunk.length, 1, MTAG);
  if (fread(*waveform, 1, chunk.length, f) != (size_t)chunk.length)
  {
    rc = ESR_FATAL_ERROR;
    goto cleanup;
  }
  if (doSwap)
  {
    if (wf->nBlockAlign == 2)
      short_byte_swap((short *)*waveform, chunk.length);
    else if (wf->nBlockAlign == 4)
      int_byte_swap((int *)*waveform, chunk.length);
  }
  
  if (swichunk)
  {
    rc = readSwiChunk(f, &parent, swichunk, doSwap);
    goto cleanup;
  }
  
  *audio_type = NULL;
  
  /* assuming nchannels = 1, usually bytes_per_sample==blockAlign / nchannels (not aurora!) */
  if (wf->nFormatTag == WAVEFORMAT_PCM)
  {
    if (wf->nBlockAlign == 2)
    {/* can only be L16 */
      if (wf->nSamplesPerSec == 8000)
        *audio_type = L"audio/L16;rate=8000";
      else if (wf->nSamplesPerSec == 16000)
        *audio_type = L"audio/L16;rate=16000";
    }
  }
  else if (wf->nFormatTag == WAVEFORMAT_ALAW)
  {
    if (wf->nSamplesPerSec == 8000)
      *audio_type = L"audio/x-alaw-basic;rate=8000";
  }
  else if (wf->nFormatTag == WAVEFORMAT_MULAW)
  {
    if (wf->nSamplesPerSec == 8000)
    {
      if (swichunk)
      {
        char *encoding = getSwiRiffKVal(swichunk, "orig-encoding");
        if (!encoding)
          *audio_type = L"audio/basic;rate=8000";
        else  if (! strcmp(encoding, "g723"))
          *audio_type = L"audio/basic;rate=8000;orig-encoding=g723";
        else if (! strcmp(encoding, "g729"))
          *audio_type = L"audio/basic;rate=8000;orig-encoding=g729";
        else
        {
          // FIXME: warning because unknown orig-encoding??
          // rec_test will never get here cuz already checked validity of audiotype
          // but to be careful pour l'avenir, should handle this
          *audio_type = L"audio/basic;rate=8000";
        }
      }
      else
        *audio_type = L"audio/basic;rate=8000";
    }
    else if (wf->nSamplesPerSec == 16000)
      *audio_type = L"audio/basic;rate=16000";
      
  }
  else if (wf->nFormatTag == WAVEFORMAT_AURORA)
  {
    if (wf->nSamplesPerSec == 8000)
      *audio_type = L"application/x-feature;rate=8000;encoding=swifeature_aurora";
    else if (wf->nSamplesPerSec == 11000)
      *audio_type = L"application/x-feature;rate=11000;encoding=swifeature_aurora";
    else if (wf->nSamplesPerSec == 16000)
      *audio_type = L"application/x-feature;rate=16000;encoding=swifeature_aurora";
  }
  else if (wf->nFormatTag == WAVEFORMAT_ES_202_050)
  {
    if (wf->nSamplesPerSec == 8000)
      *audio_type = L"application/x-feature;rate=8000;encoding=ES_202_050";
    else if (wf->nSamplesPerSec == 11000)
      *audio_type = L"application/x-feature;rate=11000;encoding=ES_202_050";
    else if (wf->nSamplesPerSec == 16000)
      *audio_type = L"application/x-feature;rate=16000;encoding=ES_202_050";
  }
  
  if (*audio_type == NULL)
  {
    rc = ESR_FATAL_ERROR;
    goto cleanup;
  }
  
cleanup:
  if (wf) FREE((char *)wf);
  if (rc != ESR_SUCCESS)
  {
    if (swichunk)
      free_swiRiff(swichunk);
    if (*waveform)
    {
      FREE((char *)*waveform);
      *waveform = NULL;
    }
  }
  return rc;
}

static int getFormatTag(wchar_t *audio_type)
{
  if (!wcsncmp(audio_type, L"audio/basic", 11))
  {
    return WAVEFORMAT_MULAW;
  }
  if (!wcsncmp(audio_type, L"application/x-feature;", 14) &&
      wcsstr(audio_type, L"encoding=swifeature_aurora"))
  {
    return WAVEFORMAT_AURORA;
  }
  else if (!wcsncmp(audio_type, L"application/x-feature;", 14) &&
           wcsstr(audio_type, L"encoding=ES_202_050"))
  {
    return WAVEFORMAT_ES_202_050;
  }
  else if (!wcsncmp(audio_type, L"audio/x-alaw-basic", 18))
  {
    return WAVEFORMAT_ALAW;
  }
  else if (!wcsncmp(audio_type, L"audio/L16", 9))
  {
    return WAVEFORMAT_PCM;
  }
  return -1;
}

/* we are assuming that riffaudio->num_tuples!=0, and hence riffaudio->tuples!=NULL
 */
static unsigned char *writeSwiAudioChunk(int doSwap, int chunk_len, SwiRiffAudio *riffaudio,
    unsigned char *workbuf)
{
  ChunkInfoStruct ck;
  int chunkinfosize = sizeof(ChunkInfoStruct);
  
  strncpy(ck.ckString, "segs", 4);
  ck.ckLength = chunk_len;
  if (doSwap)
  {
    swapInt(&ck.ckLength);
    memcpy(workbuf, &ck, chunkinfosize);
    workbuf += chunkinfosize;
    
    memcpy(workbuf, &riffaudio->num_tuples, sizeof(int));
    workbuf += sizeof(int);
    chunk_len -= sizeof(int);
    
    memcpy(workbuf, riffaudio->tuples, chunk_len);
    int_byte_swap((int *)workbuf, riffaudio->num_tuples*3 + 1);
    /* count every tuple (3) + num_tuples itself */
    return workbuf + chunk_len;
  }
  else
  {
    memcpy(workbuf, &ck, chunkinfosize);
    workbuf += chunkinfosize;
    memcpy(workbuf, &riffaudio->num_tuples, sizeof(int));
    workbuf += sizeof(int);
    chunk_len -= sizeof(int);
    
    memcpy(workbuf, riffaudio->tuples, chunk_len);
    return workbuf + chunk_len;
  }
}

/* WARNING:  returns with file pointer past the 4 first chars
 *
 * If the first 4 bytes of the specified file are "RIFF",
 * then we assume it's a RIFF file
 */
int isRiffFile(FILE *fp)
{
  char tmpbuf[4];
  fseek(fp, 0, SEEK_SET);
  fread(tmpbuf, 4, sizeof(char), fp);
  return !strncmp(tmpbuf, "RIFF", 4);
  
}

/*
 * WARNING: assuming num_channels = 1
 * INPUT: waveform, num_bytes (waveform length), audio_type, 
 *        swichunk (it returns unmodified, including not swapped)
 * OUTPUT: buf (entire riff chunk) and buflen
 *
 * AURORA special case:
 *   sampling rate = bytes_per_sample = -1; other fields of WaveFormat undefined
 */
ESR_ReturnCode convertBuf2Riff(
  unsigned char *waveform,
  unsigned int num_bytes,
  wchar_t *audio_type,
  int rate,
  int bytes_per_sample,
  SwiRiffStruct *swichunk,
  unsigned char **buf,
  unsigned int *buflen)
{
  unsigned int total_buflen;
  unsigned char *ptr, *workbuf;
  short num_channels = 1;
  int num_samples;
  int bytes_sec;
  short block_align;
  int doSwap;
  ChunkInfoStruct ck;
  RiffHeaderStruct header;
  int headerSize = sizeof(RiffHeaderStruct);
  int chunkInfoSize = sizeof(ChunkInfoStruct);
  int listChunkSize = 0;
  int segs_chunk_size, kvals_chunk_size;
  short format_tag = (short) getFormatTag(audio_type);
  
  if (format_tag == -1 ||
      (bytes_per_sample == -1 && format_tag != WAVEFORMAT_AURORA &&
       format_tag != WAVEFORMAT_ES_202_050))
  {
    PLogError(L("audio type not supported for RIFF conversion"));
    return ESR_FATAL_ERROR;
    
  }
  if (bytes_per_sample > 0)
    num_samples = num_bytes / bytes_per_sample;
  else
    num_samples = num_bytes;
    
  strncpy(header.riffString, "RIFF", 4);
  strncpy(header.waveString, "WAVE", 4);
  strncpy(header.fmtString, "fmt ", 4);
  strncpy(header.dataString, "data", 4);
  
  total_buflen = headerSize + num_bytes;
  
  if (swichunk->segs.num_tuples || swichunk->kvals.num_pairs)
  {
    listChunkSize = chunkInfoSize  /* LIST chunk info */ + 4 * sizeof(char);  /* "swi " */
    if (swichunk->segs.num_tuples)
    {
      segs_chunk_size = sizeof(int) + (swichunk->segs.num_tuples) * sizeof(RiffAudioTuple);
      listChunkSize += chunkInfoSize + segs_chunk_size;
    }
    if (swichunk->kvals.num_pairs)
    {
      int i;
      kvals_chunk_size = 0;
      for (i = 0; i < swichunk->kvals.num_pairs; i++)
      {
        kvals_chunk_size += (strlen(swichunk->kvals.kvpairs[i].key) + 1
                             + strlen(swichunk->kvals.kvpairs[i].value) + 1) * sizeof(char);
      }
      kvals_chunk_size += sizeof(int);  /* num_pairs */
      listChunkSize += chunkInfoSize + kvals_chunk_size;
    }
    total_buflen += listChunkSize;
  }
  if (total_buflen > *buflen)
  {
    PLogError(L("ESR_BUFFER_OVERFLOW"));
    return ESR_BUFFER_OVERFLOW;
  }
  workbuf = *buf;
  
  *buflen = total_buflen;
  ptr = workbuf;
  if (format_tag == WAVEFORMAT_AURORA || format_tag == WAVEFORMAT_ES_202_050)
  {
    bytes_sec = AURORA_BYTES_SEC;
    block_align = 4;
  }
  else
  {
    bytes_sec = (short)(rate * num_channels * bytes_per_sample);
    block_align = bytes_per_sample * num_channels;
  }
  doSwap = !isLittleEndian();
  if (doSwap)
  {
    header.riffChunkLength = swapConstInt(*buflen - chunkInfoSize);
    header.fmtChunkLength = swapConstInt(sizeof(WaveFormat));
    header.waveinfo.nFormatTag = swapConstShort(format_tag);  /* codec */
    header.waveinfo.nChannels = swapConstShort(num_channels);
    header.waveinfo.nSamplesPerSec = swapConstInt(rate);
    header.waveinfo.nAvgBytesPerSec = swapConstInt(bytes_sec);
    header.waveinfo.nBlockAlign = swapConstShort(block_align);
    header.waveinfo.wBitsPerSample = swapConstShort((short)((bytes_sec * 8) / rate));
    header.dataLength = swapConstInt(num_bytes);
    memcpy(ptr, &header, headerSize);
    
    memcpy(ptr + headerSize, waveform, header.dataLength);
    if (bytes_per_sample == 2)
      short_byte_swap((short *)(ptr + headerSize), num_samples);
  }
  else
  {
    header.riffChunkLength = total_buflen - chunkInfoSize;
    header.fmtChunkLength = sizeof(WaveFormat);
    header.waveinfo.nFormatTag = format_tag;  /* codec */
    header.waveinfo.nChannels = num_channels;
    header.waveinfo.nSamplesPerSec = rate;
    header.waveinfo.nAvgBytesPerSec = bytes_sec;
    header.waveinfo.nBlockAlign = (short) block_align;
    header.waveinfo.wBitsPerSample = (bytes_sec * 8) / rate;
    header.dataLength = num_bytes;
    
    memcpy(ptr, &header, headerSize);
    memcpy(ptr + headerSize, waveform, header.dataLength);
  }
  ptr += headerSize + header.dataLength;
  
  if (swichunk->segs.num_tuples || swichunk->kvals.num_pairs)
  {
    strncpy(ck.ckString, "LIST", 4);
    ck.ckLength = listChunkSize - chunkInfoSize;
    if (doSwap) swapInt(&ck.ckLength);
    memcpy(ptr, &ck, chunkInfoSize);   /* copy LIST */
    ptr += chunkInfoSize;
    
    strncpy((char *)ptr, "swi ", 4);
    ptr += 4;
    
    if (swichunk->segs.num_tuples)
    {
      ptr = writeSwiAudioChunk(doSwap, segs_chunk_size, &swichunk->segs, ptr);
    }
    if (swichunk->kvals.num_pairs)
    {
      int i, num_pairs;
      RiffKVPair *pairs;
      
      strncpy(ck.ckString, "kvs ", 4);
      ck.ckLength = kvals_chunk_size;   /* num_pairs and pairs themselves */
      if (doSwap) swapInt(&ck.ckLength);
      memcpy(ptr, &ck, chunkInfoSize);
      ptr += chunkInfoSize;
      
      num_pairs = (doSwap) ? swapConstInt(swichunk->kvals.num_pairs) : swichunk->kvals.num_pairs;
      memcpy(ptr, &num_pairs, sizeof(int));
      ptr += sizeof(int);
      
      for (pairs = swichunk->kvals.kvpairs, i = 0; i < num_pairs; i++, pairs++)
      {
        strcpy((char *)ptr, pairs->key);
        ptr += strlen(pairs->key) + 1;
        
        strcpy((char *)ptr, pairs->value);
        ptr += strlen(pairs->value) + 1;
      }
    }
  }
  passert((unsigned int)(ptr - workbuf) == *buflen);
  
  return ESR_SUCCESS;
}


