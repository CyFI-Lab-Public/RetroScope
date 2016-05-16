/*---------------------------------------------------------------------------*
 *  audioinwrapper.cpp                                                       *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                         *
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


#if defined(ANDROID) && (defined(__ARM_ARCH_5__) || defined(__ARM_ARCH_4__))

//#define USE_DEV_EAC_FILE 1

#if defined(USE_DEV_EAC_FILE)
#include <fcntl.h>
#define N_CHANNELS 1
#else
#include <system/audio.h>
#include <media/AudioRecord.h>
#include <media/mediarecorder.h>
using namespace android;
#endif

#endif // defined(ANDROID) && (defined(__ARM_ARCH_5__) || defined(__ARM_ARCH_4__))

#include "plog.h"

// #define SAVE_RAW_AUDIO              1

#ifdef SAVE_RAW_AUDIO
#include <sys/time.h>
#include <stdio.h>


static FILE *audio_data;
static struct timeval buffer_save_audio;
#endif


extern "C" 
{

#if defined(ANDROID) && (defined(__ARM_ARCH_5__) || defined(__ARM_ARCH_4__))
    
#if defined(USE_DEV_EAC_FILE)
static int audiofd = -1;
#else
static AudioRecord* record;
static int sampleRate = 8000;
static int numChannels = 1;
#endif

// called before AudioOpen
int AudioSetInputFormat(int sample_rate, int channel_count)
{
#if defined(USE_DEV_EAC_FILE)
  return 0;
#else
  sampleRate = sample_rate;
  numChannels = channel_count;
  return 0;
#endif
}

int AudioOpen(void)
{
#if defined(USE_DEV_EAC_FILE)
  audiofd = open("/dev/eac", O_RDONLY, 0666);
  if (audiofd >= 0) {
    //fcntl(audiofd, F_SETFL, O_NONBLOCK);
  
    // possibly lame attempt to get Sooner audio input working
    struct { unsigned long param1, param2, param3; } params = { 11025, 0, 0 };
    ioctl(audiofd, 317, &params, sizeof(params));
  }

  return audiofd;
#else
    #ifdef SAVE_RAW_AUDIO
        char file_name [256];

        gettimeofday ( &buffer_save_audio, NULL );
        sprintf ( file_name, "data_%ld_%ld.raw", buffer_save_audio.tv_sec, buffer_save_audio.tv_usec );
        audio_data = fopen ( file_name, "w" );
    #endif
// TODO: get record buffer size from hardware.
    record = new android::AudioRecord(
                            AUDIO_SOURCE_DEFAULT,
                            sampleRate,
                            AUDIO_FORMAT_PCM_16_BIT,
                            (numChannels > 1) ? AUDIO_CHANNEL_IN_STEREO : AUDIO_CHANNEL_IN_MONO,
                            8*1024);
  
  if (!record) return -1;
  
  return record->start() == NO_ERROR ? 0 : -1;
#endif
}

int AudioClose(void)
{
#if defined(USE_DEV_EAC_FILE)
  return close(audiofd);
#else
  record->stop();
  delete record;
    #ifdef SAVE_RAW_AUDIO
        fclose ( audio_data );
    #endif
  return 0;
#endif
}

int AudioRead(short *buffer, int frame_count)
{
  int n;
#if defined(USE_DEV_EAC_FILE)
  n = read(audiofd, buffer, frame_count*sizeof(short)*N_CHANNELS);
  n /= sizeof(short)*N_CHANNELS;
  return n;
#else
  int nreq = frame_count * sizeof(short);
  n = record->read(buffer, nreq);
  if (n > 0) {
    if (n != nreq) {
      PLogError ( "AudioRead error: not enough data %d vs %d\n", n, nreq );
    }
    n /= sizeof(short);
  }
    #ifdef SAVE_RAW_AUDIO
        if ( n > 0 )
            fwrite ( buffer, 2, n, audio_data );
    #endif
  return n;
#endif
}

int AudioSetVolume(int stream_type, int volume)
{
#if defined(USE_DEV_EAC_FILE)
  return 0;
#else
  return AudioSystem::setStreamVolume(stream_type, volume, 0);
#endif
}

int AudioGetVolume(int stream_type)
{
#if defined(USE_DEV_EAC_FILE)
  return 0;
#else
  float v = 0;
  AudioSystem::getStreamVolume(stream_type, &v, 0);
  return int(v * 100.0f);
#endif
}

#else

int AudioOpen(void)
{
  return -1;
}

int AudioClose(void)
{
  return -1;
}

int AudioSetInputFormat(int sample_rate, int channel_count)
{
  return -1;
}

int AudioSetOutputFormat(int sample_rate, int channel_count)
{
  return -1;
}

int AudioRead(short *buffer, int frame_count)
{
  return -1;
}

int AudioWrite(short *buffer, int frame_count)
{
  return -1;
}

int AudioSetStreamType(int stream_type)
{
  return -1;
}

int AudioSetVolume(int stream_type, int volume)
{
  return -1;
}

int AudioGetVolume(int stream_type)
{
  return -1;
}

#endif

} // extern "C"
