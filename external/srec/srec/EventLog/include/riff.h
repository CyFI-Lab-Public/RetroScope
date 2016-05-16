/*---------------------------------------------------------------------------*
 *  riff.h  *
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

#ifndef RIFF_H
#define RIFF_H

#include "ESR_ReturnCode.h"
#include "plog.h"
#include "passert.h"
#include "pmemory.h"
#include "SR_EventLogPrefix.h"

#define AURORA_BYTES_SEC 5600

/* standard codec IDs */
#define WAVEFORMAT_PCM      0x01
#define WAVEFORMAT_ALAW     0x06
#define WAVEFORMAT_MULAW    0x07

/* swi-specific codec ID */
#define WAVEFORMAT_AURORA   0x99
#define WAVEFORMAT_ES_202_050   0x9A

/**
 * WAV file format.
 */
typedef struct
{
	/**
	 * Codec ID.
	 */
  unsigned short nFormatTag;
	/**
	 * The number of channels.
	 */
  unsigned short nChannels;
	/**
	 * sampling rate: sample frames per sec.
	 */
  unsigned int   nSamplesPerSec;
	/**
	 * sampling rate * block alignment
	 */
  unsigned int   nAvgBytesPerSec;
	/**
	 * number of channels * bytes_per_sample
	 */
  unsigned short nBlockAlign;
	/**
	 * bytes_per_sample * 8 (PCM-specific field)
	 */
  unsigned short wBitsPerSample;
}
WaveFormat;

/**
 * Generic start of every RIFF chunk.
 */
typedef struct
{
  /**
	 * 4-byte signature
	 */
  char ckString[4];
	/**
	 * Chunk length.
	 */
  int ckLength;
}
ChunkInfoStruct;

/**
 * RIFF Header.
 */
typedef struct
{
	/**
	 * "RIFF"
	 */
  char riffString[4];
	/**
	 * The length of the RIFF chunk.
	 */
  unsigned int riffChunkLength;
	/**
	 * "WAVE"
	 */
  char waveString[4];  
	/**
	 * "fmt "
	 */
  char fmtString[4];
	/**
	 * The length of the format chunk.
	 */
  unsigned int fmtChunkLength;
	/**
	 * The audio format.
	 */
  WaveFormat waveinfo;
	/**
	 * "data"
	 */
  char dataString[4];
	/**
	 * The length of the audio data section.
	 */
  unsigned int dataLength;
}
RiffHeaderStruct;

/**
 * An audio segment.
 */
typedef struct
{
	/**
	 * Position (byte #) where audio segment begins.
	 */
  int pos;
	/**
	 * Length of audio segment.
	 */
  int len;
	/**
	 * SWIrec_PACKET_SUPPRESSED or SWIrec_PACKET_LOST.
	 */
  int type;
}
RiffAudioTuple;

/**
 * For "supp" or "lost" chunk.
 */
typedef struct
{
	/**
	 * The number of audio tuples.
	 */
  int num_tuples;
	/**
	 * The audio tuples.
	 */
  RiffAudioTuple *tuples;
}
SwiRiffAudio;

/**
 * Key-value pair.
 */
typedef struct
{
	/**
	 * e.g. "encoding"
	 */
  char *key;
	/**
	 * e.g. "g723"
	 */
  char *value;
}
RiffKVPair;

/**
 * For "kval" chunk.
 */
typedef struct
{
	/**
	 * The number of key-value pairs.
	 */
  int num_pairs;
	/**
	 * The key-value pairs.
	 */
  RiffKVPair *kvpairs;
}
SwiRiffKeyVals;

/**
 * A RIFF audio segment.
 */
typedef struct
{
	/**
	 * Special audio segments, lost or suppressed
	 */
  SwiRiffAudio segs;
	/**
	 * Key-value pairs.
	 */
  SwiRiffKeyVals kvals;
}
SwiRiffStruct;


SREC_EVENTLOG_API int isLittleEndian(void);

SREC_EVENTLOG_API ESR_ReturnCode riffReadWave2L16(
  FILE *f,
  double from,
  double to,
  short **samples,
  int *rate,
  int *length,
  SwiRiffStruct *swichunk);
  
SREC_EVENTLOG_API ESR_ReturnCode convertBuf2Riff(
  unsigned char *waveform,
  unsigned int num_bytes,
  wchar_t *audio_type,
  int rate,
  int bytes_per_sample,
  SwiRiffStruct *swichunk,
  unsigned char **buf,
  unsigned int *buflen);
  
SREC_EVENTLOG_API ESR_ReturnCode readRiff2Buf(
  FILE *f,
  void **waveform,
  unsigned int *num_bytes,
  const wchar_t **audio_type,
  SwiRiffStruct *swichunk);
  
SREC_EVENTLOG_API int isRiffFile(FILE *fp);
SREC_EVENTLOG_API void free_swiRiff(SwiRiffStruct *swichunk);
SREC_EVENTLOG_API char *getSwiRiffKVal(SwiRiffStruct *swichunk, char *key);
#endif










