/*---------------------------------------------------------------------------*
 *  sample.h  *
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

#ifndef _h_sample_
#define _h_sample_


#include "all_defs.h"
#ifndef _RTT
#include "duk_io.h"
#endif
#include "front.h"

#ifdef _WIN32
#include "windows.h"
#endif

/*  The known wave types here
*/
/* #define DEVICE_RAW_PCM 1 */
/* #define DEVICE_MULAW  2 */
#define FILE_FORMATTED  3

/*  The known device (op) types here
*/

#define WAVE_DEVICE_INPUT   1
#define WAVE_DEVICE_OUTPUT  2
#define WAVE_FILE_INPUT     3
#define WAVE_FILE_OUTPUT    4

/*  The known wave-file types are
** RIFF (R), NIST (N), RAW-PCM (P), RAW-MU-LAW (M)
*/

#if !defined(_WIN32)     /* TODO: do we want to support RIFF header files? */
/* Add definition for use in RIFF header r/w */
#if defined unix || defined PSOS || defined POSIX
/* VxWorks simulator defines DWORD and WORD already */
#if !(defined(__vxworks) && (CPU & SIMNT))
typedef asr_uint32_t DWORD;
#endif
typedef unsigned char  BYTE;
/* following two lines does not help. It only works when WORD is defined by MACRO: #define WORD unsigned short */
#ifdef WORD
#undef WORD
#endif
#if !(defined(__vxworks) && (CPU & SIMNT))
typedef asr_uint16_t WORD;
#endif
#define WAVE_FORMAT_PCM 0x01
#endif

//typedef DWORD  FOURCC;         /* a four character code */
#define MAKEFOURCC(ch0, ch1, ch2, ch3) ((DWORD)(BYTE)(ch0) | ((DWORD)(BYTE)(ch1) << 8) | ((DWORD)(BYTE)(ch2) << 16) | ((DWORD)(BYTE)(ch3) << 24 ))
#define mmioFOURCC MAKEFOURCC

/**
 * @todo document
 */
typedef struct
{
  WORD  wFormatTag;
  WORD  nChannels;
  DWORD nSamplesPerSec;
  DWORD nAvgBytesPerSec;
  WORD  nBlockAlign;
}
WAVEFORMAT;

/**
 * @todo document
 */
typedef struct
{
  WAVEFORMAT wf;
  WORD       wBitsPerSample;
}
PCMWAVEFORMAT;
#else
/* disable nameless union/struct warning in mmsytem.h but restore them to
disallow such code of our own.
*/
#pragma warning (push)
#pragma warning (disable: 4201)
#include <mmsystem.h>
#pragma warning (pop)
#endif


#ifndef _RTT

/**
 * @todo document
 */
typedef struct
{
  char  typ;  /* R (RIFF), N (NIST), P (RAW PCM) M (MU-LAW) */
  int   op;      /* read or write */
  int   endian;  /* 0 is little 1 is big */
  unsigned long len; /* length of file */
  PFile* file;  /* pointer to file */
  char  name[MAX_FILE_NAME]; /* file name */
}
wav_file_info;

#endif

/**
 * @todo document
 */
typedef struct
{
  char typ;  /* -Undefined as yet- */
  int  op;      /* read (i/p) or write (o/p) */
}
wav_device_info;

/**
 * @todo document
 */
typedef union {
#ifndef _RTT
  wav_file_info   file;
#endif
  wav_device_info ext;
} gen_device_info;

#define MAXHISTBITS 33  /* one more than bitrange for signed */
/* int bit usage - this could be 17 if */
/* we assume shorts   */

/**
 * @todo document
 */
typedef struct
{
  int nsam;
  int sum;
  int sum2;
  int sumsqu;
  int sumsqu2;
  int sumabs;
  int sumabs2;
  int highclip;
  int lowclip;
  int bithist[MAXHISTBITS];
  samdata highclip_level;
  samdata lowclip_level;
  int max_per10000_clip;
  int max_dc_offset;
  int high_noise_level_bit;
  int low_speech_level_bit;
  int min_samples;
}
wave_stats;

/**
 * @todo document
 */
typedef struct
{
  int   wave_type;
  int   device_type;
  int   samplerate;
  int   frame_size;
  int   window_size;
  int   num_samples;
  samdata  *income;
  samdata  *outgo;
  booldata  initialised;
  float  scale;
  int   offset;
  /* The channel object here is the set of data streams used in making frames.
      IN CA, it is convenient to store channel as part of CA_Wave (wave_info).
      It could have a many-to-one relationship with wave_info. */
  front_channel *channel;
  gen_device_info     device;
  wave_stats  stats;
  booldata  do_stats;
}
wave_info;


void reset_sig_check(wave_stats *ws);
void get_sig_check(wave_stats *ws, int *nsam, int *pclowclip, int *pchighclip,
                   int *dc_offset, int *amp, int *pc5, int *pc95, int* overflow);
void acc_wave_stats(wave_info* wave);

void create_sample_buffer(wave_info *wave, int frame_size, int window_size);
void free_sample_buffer(wave_info *wave);
#ifndef _RTT
int init_wavfile_stream(wave_info *wave, char *filename, int type);
int close_wavfile_stream(wave_info *wave);
int load_wavfile_data(wave_info* wave);
int save_wavfile_data(wave_info* wave);
int seek_wavfile_data(wave_info* wave, long offset, int origin);
int read_riff_header(PFile* waveFile, PCMWAVEFORMAT *pcmWaveFormat, unsigned long *datalen);
void add_riff_header(PFile* waveFile, int samplerate, int bitspersample);
void fix_riff_header(PFile* waveFile, int samplerate, int bitspersample);
#endif
void copy_wave_data(wave_info* dest, wave_info* src);



#endif
