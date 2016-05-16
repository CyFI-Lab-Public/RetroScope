/*---------------------------------------------------------------------------*
 *  audioin.c                                                                *
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

/* -------------------------------------------------------------------------+
   |                               ScanSoft Inc.                              |
   + -------------------------------------------------------------------------*/



/* -------------------------------------------------------------------------+
   | Project       : ScanSoft AudioIn
   | Module        : audioin
   | File name     : audioin.c
   | Description   : This module contains the main implementation for the audioIn
   |                 component.
   | Reference(s)  : wavein, audioout, audioin.chm, audioin.doc, audioin.hlp,
   |                 SltGl00001_audioin_gl1.doc
   | Status        : Version 1.2
   + -------------------------------------------------------------------------*/
/*     Feb/25/2002: First QNX/SH4 "draft" version. Version 1.1              */
/*     Nov/25/2004: clean up and minor changes like choice of the codec     */
/*                  frame size which is now automatically selected          */
/*--------------------------------------------------------------------------*/

#if !defined(ANDROID) || defined(__ARM_ARCH_5__) || defined(__ARM_ARCH_4__)



#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <pthread.h>
#include <sched.h>
#include <time.h>
#include <unistd.h>
#include <sys/select.h>
#include "plog.h"
#include "audioin.h"

#if defined(ANDROID)
#include "audioinwrapper.h"
#else
#include <alsa/asoundlib.h>
#endif

// #define SAVE_RAW_AUDIO              1

#ifdef SAVE_RAW_AUDIO
#include <sys/time.h>
#include <stdio.h>

static FILE *audio_data;
static struct timeval buffer_save_audio;
#endif

/*#define FILTER_ON*/

#ifdef FILTER_ON
#include "filter.h"
#endif

/* -------------------------------------------------------------------------+
   |   EXTERNAL DATA (+ meaning)                                              |
   + -------------------------------------------------------------------------*/

/* none */

/* -------------------------------------------------------------------------+
   |   MACROS                                                                 |
   + -------------------------------------------------------------------------*/

#define NR_OF_CHANNELS            1 

#if defined(ANDROID)
/* size in samples */
/* We really no longer use this for ANDROID but more changes are needed to remove it. SteveR */
#define SAMPLES_BUFFER_SIZE             (8*1024)
#define SAMPLES_BUFFER_HIGH_WATERMARK   (6*1024)
#else
#define SAMPLES_BUFFER_SIZE            (50*4410)
#define SAMPLES_BUFFER_HIGH_WATERMARK  (40*4410)
#endif

/* IMPORTANT NOTE:
   Here a "frame" is an ALSA term.  A frame is comprised of 1 sample if mono,
   and 2 samples if stereo.  This should be distinguished from what the
   ASR engine and lhs_audioin*() API functions refer to as a frame which is
   a set of consecutive samples.
   (see http://www.alsa-project.org/alsa-doc/alsa-lib/pcm.html) */
#if defined(ANDROID)
#define CODEC_FRAGMENT_SIZE_IN_FRAMES                    1024
#else
//read the equivalent of 100 ms per buffer. Note: we are recording at 44 kHz
#define CODEC_FRAGMENT_SIZE_IN_FRAMES                    4410 
#endif

/* -------------------------------------------------------------------------+
   |   TYPE DEFINITIONS                                                       |
   + -------------------------------------------------------------------------*/

/* -------------------------------------------------------------------------+
   |   GLOBAL CONSTANTS                                                       |
   + -------------------------------------------------------------------------*/


/* -------------------------------------------------------------------------+
   |   GLOBAL VARIABLES                                                       |
   + -------------------------------------------------------------------------*/

#if !defined(ANDROID)
static snd_pcm_t       *ghPCM;                   /* handle to the PCM recording device */
#endif

static int              gCodecFragmentSizeInFrames = CODEC_FRAGMENT_SIZE_IN_FRAMES;    /* fragment size used by the codec driver */
static audioinSample    gSamplesBufferCircularFifo[SAMPLES_BUFFER_SIZE]; /* circular buffer that buffers the incoming samples */

static int              gWriteIndexPointer = 0;  /* write pointer in the circular FIFO samples buffer */
static int              gReadIndexPointer  = 0;  /* read  pointer in the circular FIFO samples buffer */
static AUDIOIN_INFO     gAudioInInfo;            /* to store the info about the acquisition */
static pthread_mutex_t  gAudioMutex;             /* to prevent using the read/write pointers at the same time in both threads */

static pthread_cond_t   gThreadRunning;          /* synchronize when the AcquisitionThreadID is running*/
static int              gThreadRunningSignaled = 0;

static pthread_cond_t   gOpenExCalled;          /* synchronize when the lhs_audioinOpenEx is called*/
static int              gOpenExCalledSignaled = 0;

static pthread_cond_t   gCloseCalled;          /* synchronize when the lhs_audioinClose is called*/
static int              gCloseCalledSignaled = 0;

static pthread_t        AcquisitionThreadID;     /* acquisition thread id */

static int              gInitialized = 0; /* did we initialize some of the variables*/
static int              gTerminateThread = 0;
static struct timeval   timer;                   /* timer used by select to relinquish cpu times */

static int              gRecordingVolume = -1;   /* recording volume ; number between 0 and 15 */
static int              bRecord = 0;             /* recording state is off */   
static int              bClose  = 1;             /* audio pipe is closed */ 

#ifdef FILTER_ON
static FIR_struct      *pFIR = NULL;             /* pointer to FIR structure */
#endif

#ifdef AUDIOIN_SUPPORT_CALLBACK
static pCallbackFunc    gpCallback         = NULL;
static void            *gpCallbackInstance = NULL;
static unsigned long    gnCallbackSamples  = 0;
#endif

/* -------------------------------------------------------------------------+
   |   LOCAL FUNCTION PROTOTYPES                                              |
   + -------------------------------------------------------------------------*/

static void *AcquisitionThread(void *data);                /* Entry function for the acquisition thread */
static int OpenAndPrepareSound(unsigned long ulFrequency);

/**
 * returns 0 if success
 */
static int Initialize(AUDIOIN_H * phAudioIn)
{
  int doneWaiting = 0;

  if( gInitialized == 1 )
    return 0;

  /* creates the mutex that will be used to lock/unlock access to some variables/code */
  if (pthread_mutex_init(&gAudioMutex, NULL) != 0)
  {
    return 1;
  }

  if(pthread_cond_init(&gThreadRunning, 0) != 0 )
  {
    return 1;
  }

  if(pthread_cond_init(&gOpenExCalled, 0) != 0 )
  {
    return 1;
  }

  if(pthread_cond_init(&gCloseCalled, 0) != 0 )
  {
    return 1;
  }

  pthread_mutex_lock(&gAudioMutex);

  /* create a thread with very high priority that will do the acquisition */
  if (pthread_create(&AcquisitionThreadID, NULL, AcquisitionThread, phAudioIn) != 0)
  {
    return 1;
  }

  //wait for the thread to run
  while (!doneWaiting)
  {
    int rc = pthread_cond_wait(&gThreadRunning, &gAudioMutex);
    switch (rc)
    {
      case 0:
        if (!gThreadRunningSignaled)
        {
          // Avoid spurious wakeups
          continue;
        }
        else
        {
          gThreadRunningSignaled = 0;
          doneWaiting = 1;
          break;
        }
        break;
      default:
        pthread_mutex_unlock(&gAudioMutex);
        return 1;
    }
  }

  pthread_mutex_unlock(&gAudioMutex);


  //thread is now running.

  gInitialized = 1;

  return 0;
}

#if 0
/* disable this unused function for now until we decide what to do with this */

/**
 * returns 0 if success
 */
static int UnInitialize()
{
  //signal the thread that it has to stop running.
  pthread_mutex_lock ( &gAudioMutex );
  gTerminateThread = 1;

  //signal to tell that our thread is now running.
  if ( pthread_cond_signal ( &gOpenExCalled ) != 0 )
  {
    pthread_mutex_unlock ( &gAudioMutex );
    PLogError ( "Audio In Error pthread_cond_signal\n" );
    return 1;
  }
  gOpenExCalledSignaled = 1;
  pthread_mutex_unlock ( &gAudioMutex );

  /* wait until thread exits */
  if (pthread_join(AcquisitionThreadID, NULL) != 0)
  {
    return 1;
  }

  /* destroy the mutex */
  if (pthread_mutex_destroy(&gAudioMutex) !=0 )
  {
    return 1;
  }
  if( pthread_cond_destroy(&gThreadRunning) != 0 )
  {
    return 1;
  }
  if( pthread_cond_destroy(&gOpenExCalled) != 0 )
  {
    return 1;
  }
  if( pthread_cond_destroy(&gCloseCalled) != 0 )
  {
    return 1;
  }
  gInitialized = 0;
  return 0;
}
#endif

/* -------------------------------------------------------------------------+
   |   LOCAL FUNCTION (should be static)                                      |
   + -------------------------------------------------------------------------*/

static void setRecordOn(void)
{
  bRecord = 1;
}

static void setRecordOff(void)
{
  bRecord = 0;
}

static int getRecord(void)
{
  return bRecord;
}

static void setCloseOn(void)
{
  bClose = 1;
}

static void setCloseOff(void)
{
  bClose = 0;
}

static int getClose(void)
{
  return bClose;
}


/**************************************************************
 * AcquisitionThread                                           *
 *                                                             *
 * This function is the entry function of a thread created by  *
 * lhs_audioinOpen and which is responsible of getting the     *
 * samples from the codec and store them in a big circular     *
 * FIFO buffer.                                                *
 * The priority of this thread has been set to high in order   *
 * to prevent codec buffer overrun. Since the FIFO is limited  *
 * in size (5 sec default ; see SAMPLES_BUFFER_SIZE            *
 * parameter), the application must still be fast enough to    *
 * prevent FIFO overflow/overrun                               *
 **************************************************************/
#if defined(ANDROID)

void *AcquisitionThread ( void *data )
{
  int doneWaiting = 0;
  audioinSample   *CodecBuffer;
  long            x;
  long            y;
#ifdef AUDIOIN_SUPPORT_CALLBACK
  AUDIOIN_H       *phAudioIn = (AUDIOIN_H *)data;
  AUDIOIN_WAVEHDR *pwhdr;
#endif


  pthread_mutex_lock ( &gAudioMutex );

  //signal to tell that our thread is now running.
  if ( pthread_cond_signal ( &gThreadRunning ) != 0 )
  {
    pthread_mutex_unlock ( &gAudioMutex );
    PLogError ( "Audio In Error pthread_cond_signal\n" );
    exit ( 1 );
  }
  gThreadRunningSignaled = 1;

  while( 1 )
  {

    while (!doneWaiting)
    {
      int rc = pthread_cond_wait(&gOpenExCalled, &gAudioMutex);
      switch (rc)
      {
        case 0:
          if (!gOpenExCalledSignaled)
          {
            // Avoid spurious wakeups
            continue;
          }
          else
          {
            gOpenExCalledSignaled = 0;
            doneWaiting = 1;
            break;
          }
          break;
        default:
          PLogError ( "Audio In Error pthread_cond_signal\n" );
          pthread_mutex_unlock(&gAudioMutex);
          return ( (void *)NULL );
      }
    }
    doneWaiting = 0;
    pthread_mutex_unlock(&gAudioMutex);

    if( gTerminateThread == 1 )
      break;



    /* buffer of 16 bits samples */
    CodecBuffer = (audioinSample *)malloc ( gCodecFragmentSizeInFrames * sizeof ( audioinSample ) );

    if ( CodecBuffer == NULL )
    {
      PLogError ( "Audio In Error malloc\n" );
      exit ( 1 );
    }
    pwhdr = malloc ( sizeof ( AUDIOIN_WAVEHDR ) );

    if ( pwhdr == NULL )
    {
      PLogError ( "Audio In Error malloc\n" );
      exit ( 1 );
    }

    while ( !getClose ( ) )
    {

      int iReadFrames  = 0;  /* number of frames acquired by the codec */
      /* NOTE: here a frame is comprised of 1 sample if mono, 2 samples if stereo, etc */
      int iReadSamples = 0;  /* number of samples acquired by the codec */
      int frames_to_read;     /* Actual number to read */
      int frames_read;        /* Frames read on one read */

      iReadFrames = 0;

      do
      {
        frames_to_read = gCodecFragmentSizeInFrames - iReadFrames;
        /* AudioRead() - output: number of frames (mono: 1 sample, stereo: 2 samples)*/
        frames_read = AudioRead ( CodecBuffer + iReadFrames, frames_to_read );

        if ( frames_read > 0 )
          iReadFrames += frames_read;
      }
      while ( ( iReadFrames < gCodecFragmentSizeInFrames ) && ( frames_read > 0 ) );
      iReadSamples = iReadFrames;

      if ( getRecord ( ) )  /* else continue to read from driver but discard samples */
      {
        if ( iReadSamples < 0 )
        {
          iReadSamples = 0;
          gAudioInInfo.eStatusInfo = AUDIOIN_HWOVERRUN;
        }
        else
        {
#ifdef FILTER_ON
          /* x: index for start of input samples; y: index for output sample */
          for ( x = 0, y = 0; x < iReadSamples; x += pFIR->factor_down )
          {
            FIR_downsample ( pFIR->factor_down, &( CodecBuffer[x] ), &( CodecBuffer[y++] ), pFIR );
          }
          /* update the number samples */
          iReadSamples = y;
#endif
          pthread_mutex_lock ( &gAudioMutex );

          if ( gAudioInInfo.u32SamplesAvailable + iReadSamples > SAMPLES_BUFFER_SIZE )
          {
            gAudioInInfo.u32SamplesAvailable = SAMPLES_BUFFER_SIZE;
            gAudioInInfo.eStatusInfo = AUDIOIN_FIFOOVERRUN;
          }
          else
          {
            if ( gAudioInInfo.u32SamplesAvailable + iReadSamples > SAMPLES_BUFFER_HIGH_WATERMARK )
            {
              gAudioInInfo.eStatusInfo = AUDIOIN_HIGHWATERMARK;
            }
            else if ( gAudioInInfo.eStatusInfo != AUDIOIN_FIFOOVERRUN )
            {
              gAudioInInfo.eStatusInfo = AUDIOIN_NORMAL;
            }
            gAudioInInfo.u32SamplesAvailable += iReadSamples;
          }
          if ( gWriteIndexPointer + iReadSamples <= SAMPLES_BUFFER_SIZE )
          {
            memcpy ( &( gSamplesBufferCircularFifo[gWriteIndexPointer] ), CodecBuffer,
                iReadSamples * sizeof ( audioinSample ) );
            gWriteIndexPointer += iReadSamples;

            if ( gWriteIndexPointer >= SAMPLES_BUFFER_SIZE )
              gWriteIndexPointer = 0;
          }
          else
          {
            int NbToCopy;

            NbToCopy = SAMPLES_BUFFER_SIZE - gWriteIndexPointer;
            memcpy ( &( gSamplesBufferCircularFifo [gWriteIndexPointer] ), CodecBuffer,
                NbToCopy * sizeof ( audioinSample ) );
            gWriteIndexPointer = 0;
            memcpy ( gSamplesBufferCircularFifo, &( CodecBuffer [NbToCopy] ),
                ( iReadSamples-NbToCopy ) * sizeof ( audioinSample ) );
            gWriteIndexPointer = iReadSamples - NbToCopy;
          }

#ifdef AUDIOIN_SUPPORT_CALLBACK
          /* Callback notification.  Ideally this audio acquisition thread should be very lean.
             It should simply read from the low level driver, store the filtered samples in
             the FIFO, then go back to reading from the driver.  The additional data copy
             for the callback function is ok despite the overhead incurred, but only because
             there's some buffering done by the low level driver.  This design should be
             revisited to make it more general purpose.
             */
          if ( gpCallback != NULL )
          {
            pwhdr->nBufferLength  = iReadSamples * sizeof ( audioinSample );
            pwhdr->nBytesRecorded = pwhdr->nBufferLength;
            pwhdr->status = AUDIOIN_NORMAL;
            pwhdr->pData = CodecBuffer;
            /* pass samples to callback function who should deallocate the buffer and structure */
            gpCallback ( *phAudioIn, AUDIOIN_MSG_DATA, gpCallbackInstance, pwhdr, NULL );
          }
#endif
          /* samples are available to read */
          pthread_mutex_unlock ( &gAudioMutex );
          timer.tv_sec = 0;
          timer.tv_usec = 200;
          select ( 0, NULL, NULL, NULL, &timer );
        }
      } /* if (getRecord()) */

    } /* while (!getClose()) */
    if ( AudioClose ( ) !=0 )
    {
      PLogError ( "Audio In Error Closing Hardware\n" );
    }
    free ( CodecBuffer );

    pthread_mutex_lock ( &gAudioMutex );
    //signal to tell that our thread is now running.
    if ( pthread_cond_signal ( &gCloseCalled ) != 0 )
    {
      pthread_mutex_unlock ( &gAudioMutex );
      PLogError ( "Audio In Error pthread_cond_signal\n" );
      exit ( 1 );
    }
    gCloseCalledSignaled = 1;
  }

  pthread_exit ( (void *)NULL );
  return ( (void *)NULL );
}

#else
/* non-ANDROID version */

void *AcquisitionThread ( void *data )
{
  int doneWaiting = 0;
  audioinSample   *CodecBuffer;
#ifdef FILTER_ON
  long            x;
  long            y;
#endif
#ifdef AUDIOIN_SUPPORT_CALLBACK
  AUDIOIN_H       *phAudioIn = (AUDIOIN_H *)data;
#endif

  pthread_mutex_lock ( &gAudioMutex );

  //signal to tell that our thread is now running.
  if ( pthread_cond_signal ( &gThreadRunning ) != 0 )
  {
    pthread_mutex_unlock ( &gAudioMutex );
    PLogError ( "Audio In Error pthread_cond_signal\n" );
    exit ( 1 );
  }
  gThreadRunningSignaled = 1;

  while( 1 )
  {
    while (!doneWaiting)
    {
      int rc = pthread_cond_wait(&gOpenExCalled, &gAudioMutex);
      switch (rc)
      {
        case 0:
          if (!gOpenExCalledSignaled)
          {
            // Avoid spurious wakeups
            continue;
          }
          else
          {
            gOpenExCalledSignaled = 0;
            doneWaiting = 1;
            break;
          }
          break;
        default:
          PLogError ( "Audio In Error pthread_cond_wait\n" );
          pthread_mutex_unlock(&gAudioMutex);
          return ( (void *)NULL );
      }
    }
    doneWaiting = 0;
    pthread_mutex_unlock(&gAudioMutex);

    if( gTerminateThread == 1 )
      break;

    /* buffer of 16 bits samples */
    CodecBuffer = (audioinSample *)malloc ( gCodecFragmentSizeInFrames * sizeof ( audioinSample ) );

    if ( CodecBuffer == NULL )
    {
      PLogError ( "Audio In Error pthread_cond_signal\n" );
      exit ( 1 );
    }

    while ( !getClose ( ) )
    {
      int iReadFrames  = 0;  /* number of frames acquired by the codec */
      /* NOTE: here a frame is comprised of 1 sample if mono, 2 samples if stereo, etc */
      int iReadSamples = 0;  /* number of samples acquired by the codec */
      if ( ( iReadFrames = snd_pcm_readi ( ghPCM, (void *)CodecBuffer, gCodecFragmentSizeInFrames ) ) < 0 )
      {
        if ( iReadFrames == -EBADFD )
        {
          PLogError ( "Audio In Error PCM Not In The Right State\n" );
        }
        else if ( iReadFrames == -EPIPE )
        {
          snd_pcm_prepare(ghPCM);
          PLogError ( "Audio In Error Overrun\n" );
        }
        else if ( iReadFrames == -ESTRPIPE )
        {
          PLogError ( "Audio In Error Stream Suspended\n" );
        }
      }
      iReadSamples = iReadFrames;

      if ( getRecord ( ) )  /* else continue to read from driver but discard samples */
      {
        if ( iReadSamples < 0 )
        {
          iReadSamples = 0;
          gAudioInInfo.eStatusInfo = AUDIOIN_HWOVERRUN;
        }
        else
        {
#ifdef FILTER_ON
          /* x: index for start of input samples; y: index for output sample */
          for ( x = 0, y = 0; x < iReadSamples; x += pFIR->factor_down )
          {
            FIR_downsample ( pFIR->factor_down, &( CodecBuffer[x] ), &( CodecBuffer[y++] ), pFIR );
          }
          /* update the number samples */
          iReadSamples = y;
#endif
#ifdef SAVE_RAW_AUDIO
          if ( iReadSamples > 0 )
            fwrite ( CodecBuffer, 2, iReadSamples, audio_data );
#endif

          pthread_mutex_lock ( &gAudioMutex );

          if ( gAudioInInfo.u32SamplesAvailable + iReadSamples > SAMPLES_BUFFER_SIZE )
          {
            gAudioInInfo.u32SamplesAvailable = SAMPLES_BUFFER_SIZE;
            gAudioInInfo.eStatusInfo = AUDIOIN_FIFOOVERRUN;
          }
          else
          {
            if ( gAudioInInfo.u32SamplesAvailable + iReadSamples > SAMPLES_BUFFER_HIGH_WATERMARK )
            {
              gAudioInInfo.eStatusInfo = AUDIOIN_HIGHWATERMARK;
            }
            else if ( gAudioInInfo.eStatusInfo != AUDIOIN_FIFOOVERRUN )
            {
              gAudioInInfo.eStatusInfo = AUDIOIN_NORMAL;
            }
            gAudioInInfo.u32SamplesAvailable += iReadSamples;
          }
          if ( gWriteIndexPointer + iReadSamples <= SAMPLES_BUFFER_SIZE )
          {
            memcpy ( &( gSamplesBufferCircularFifo[gWriteIndexPointer] ), CodecBuffer,
                iReadSamples * sizeof ( audioinSample ) );
            gWriteIndexPointer += iReadSamples;

            if ( gWriteIndexPointer >= SAMPLES_BUFFER_SIZE )
              gWriteIndexPointer = 0;
          }
          else
          {
            int NbToCopy;

            NbToCopy = SAMPLES_BUFFER_SIZE - gWriteIndexPointer;
            memcpy ( &( gSamplesBufferCircularFifo [gWriteIndexPointer] ), CodecBuffer,
                NbToCopy * sizeof ( audioinSample ) );
            gWriteIndexPointer = 0;
            memcpy ( gSamplesBufferCircularFifo, &( CodecBuffer [NbToCopy] ),
                ( iReadSamples-NbToCopy ) * sizeof ( audioinSample ) );
            gWriteIndexPointer = iReadSamples - NbToCopy;
          }
#ifdef AUDIOIN_SUPPORT_CALLBACK
          /* Callback notification.  Ideally this audio acquisition thread should be very lean.
             It should simply read from the low level driver, store the filtered samples in  
             the FIFO, then go back to reading from the driver.  The additional data copy 
             for the callback function is ok despite the overhead incurred, but only because 
             there's some buffering done by the low level driver.  This design should be 
             revisited to make it more general purpose.
             */
          while ( ( gpCallback != NULL ) && ( gAudioInInfo.u32SamplesAvailable >= gnCallbackSamples ) )
          {            
            AUDIOIN_WAVEHDR *pwhdr;

            pwhdr = malloc ( sizeof ( AUDIOIN_WAVEHDR ) );

            if ( pwhdr != NULL )
            {            
              pwhdr->nBufferLength  = gnCallbackSamples * sizeof ( audioinSample );
              pwhdr->nBytesRecorded = pwhdr->nBufferLength;
              pwhdr->status = gAudioInInfo.eStatusInfo;
              pwhdr->pData = malloc ( pwhdr->nBufferLength );

              if ( pwhdr->pData != NULL )
              {
                if ( gReadIndexPointer + gnCallbackSamples <= SAMPLES_BUFFER_SIZE )
                { 
                  memcpy ( pwhdr->pData, &( gSamplesBufferCircularFifo [gReadIndexPointer] ),
                      pwhdr->nBufferLength );
                  gReadIndexPointer += gnCallbackSamples;

                  if ( gReadIndexPointer >= SAMPLES_BUFFER_SIZE )
                    gReadIndexPointer = 0;
                }
                else
                { 
                  size_t nSamplesPart1 = SAMPLES_BUFFER_SIZE - gReadIndexPointer;
                  size_t nSamplesPart2 = gnCallbackSamples - nSamplesPart1;

                  memcpy ( pwhdr->pData, &( gSamplesBufferCircularFifo [gReadIndexPointer] ),
                      nSamplesPart1*sizeof ( audioinSample ) );
                  gReadIndexPointer = 0;
                  memcpy ( pwhdr->pData + nSamplesPart1 * sizeof (audioinSample ),
                      gSamplesBufferCircularFifo, nSamplesPart2 * sizeof ( audioinSample ) );
                  gReadIndexPointer = nSamplesPart2;
                }                                         
                gAudioInInfo.u32SamplesAvailable -= gnCallbackSamples;
                /* pass samples to callback function who should deallocate the buffer and structure */
                gpCallback ( *phAudioIn, AUDIOIN_MSG_DATA, gpCallbackInstance, pwhdr, NULL );
              }
              else
              {
                // error
              }
            }
            else
            {
              // error
            }
          }
#endif
          /* samples are available to read */
          pthread_mutex_unlock ( &gAudioMutex );
          timer.tv_sec = 0;
          timer.tv_usec = 200;
          select ( 0, NULL, NULL, NULL, &timer );
        }
      } /* if (getRecord()) */

    } /* while (!getClose()) */

    if ( snd_pcm_close ( ghPCM ) !=0 )
    {
      PLogError ( "Audio In Error Closing Hardware\n" );
    }

    free ( CodecBuffer );

    pthread_mutex_lock ( &gAudioMutex );
    //signal to tell that our thread is now running.
    if ( pthread_cond_signal ( &gCloseCalled ) != 0 )
    {
      pthread_mutex_unlock ( &gAudioMutex );
      PLogError ( "Audio In Error pthread_cond_signal\n" );
      exit ( 1 );
    }
    gCloseCalledSignaled = 1;
  }
  pthread_exit ( (void *)NULL );
  return ( (void *)NULL );
}
#endif

/**************************************************************
 * OpenAndPrepareSound                                         *
 *************************************************************/


static int OpenAndPrepareSound(unsigned long ulFrequency)
{
#if defined(ANDROID)

  /* Only support certain frequencies.  Modify this to check frequency
     against a structure of valid frequencies */
#ifdef FILTER_ON
  if ( ulFrequency == 11025 )
  {
    if ( AudioSetInputFormat ( 44100, NR_OF_CHANNELS ) != 0 ) /* sample at 44100 then downsample */
    {
      PLogError ( "Audio In Error OpenAndPrepareSound - AudioSetInputFormat failed!\n");
      return LHS_E_AUDIOIN_COULDNOTOPENDEVICE; 
    }
  }
  else
  {
    PLogError ( "Audio In Error OpenAndPrepareSound - invalid frequency!");
    return LHS_E_AUDIOIN_COULDNOTOPENDEVICE;
  }
#else
  if ( ( ulFrequency == 11025 ) || ( ulFrequency == 8000 ) )
  {
    if ( AudioSetInputFormat ( ulFrequency, NR_OF_CHANNELS ) != 0 )
    {
      PLogError ( "Audio In Error OpenAndPrepareSound - AudioSetInputFormat failed!");
      return LHS_E_AUDIOIN_COULDNOTOPENDEVICE; 
    }
  }
  else
  {
    PLogError ( "Audio In Error OpenAndPrepareSound - invalid frequency!");
    return LHS_E_AUDIOIN_COULDNOTOPENDEVICE;
  }
#endif

  /* set some variables */
  gAudioInInfo.u32SamplesAvailable = 0;

  /* Open Audio driver */
  if (AudioOpen() < 0)
  {
    PLogError ( "Audio In Error OpenAndPrepareSound - AudioOpen failed!");
    return ~LHS_AUDIOIN_OK;
  }

#else

  snd_pcm_hw_params_t *hwparams;
  unsigned int         exact_rate;
  int                  dir;
  int                  rc;

  /* step 1 : open the sound device */
  /* ------------------------------ */
  if ((rc = snd_pcm_open(&ghPCM, "default", SND_PCM_STREAM_CAPTURE, 0)) < 0)
  {
    PLogError ( "Audio In Error snd_pcm_open() (rc = %d: %s)\n", rc, snd_strerror(rc));
    return LHS_E_AUDIOIN_COULDNOTOPENDEVICE;
  }

  if ((rc = snd_pcm_hw_params_malloc(&hwparams)) < 0)
  {
    PLogError ( "Audio In Error snd_pcm_hw_params_malloc() (rc = %d: %s)\n", rc, snd_strerror(rc));
    return LHS_E_AUDIOIN_COULDNOTOPENDEVICE;
  }

  /* step 2 : configuring the audio channel */
  /* -------------------------------------- */

  if ((rc = snd_pcm_hw_params_any(ghPCM, hwparams)) < 0)
  {
    PLogError ( "Audio In Error snd_pcm_hw_params_any() (rc = %d: %s)\n", rc, snd_strerror(rc));
    return LHS_E_AUDIOIN_COULDNOTOPENDEVICE;
  }

  if ((rc = snd_pcm_hw_params_set_access(ghPCM, hwparams, SND_PCM_ACCESS_RW_INTERLEAVED)) < 0)
  {
    PLogError ( "Audio In Error snd_pcm_hw_params_set_access() (rc = %d: %s)\n", rc, snd_strerror(rc));
    return LHS_E_AUDIOIN_COULDNOTOPENDEVICE;
  }

  if ((rc = snd_pcm_hw_params_set_format(ghPCM, hwparams, SND_PCM_FORMAT_S16_LE)) < 0)
  {
    PLogError ( "Audio In Error snd_pcm_hw_params_set_format() (rc = %d: %s)\n", rc, snd_strerror(rc));
    return LHS_E_AUDIOIN_COULDNOTOPENDEVICE;
  }

#ifdef FILTER_ON
  if (ulFrequency == 11025)
  {
    exact_rate = 44100;
  }
  else
    return LHS_E_AUDIOIN_COULDNOTOPENDEVICE;
#else
  exact_rate = ulFrequency;
#endif  

  dir = 0;

#if 0
  /* This version seems to have problems when the code is compiled into a shared library.
     The subsequent call to snd_pcm_hw_params() fails. */
  if ((rc = snd_pcm_hw_params_set_rate_near(ghPCM, hwparams, &exact_rate, &dir)) < 0)
  {
    PLogError ( "Audio In Error snd_pcm_hw_params_set_rate_near() (rc = %d: %s)\n", rc, snd_strerror(rc));
    return LHS_E_AUDIOIN_COULDNOTOPENDEVICE;
  }
#else
  /* This version works better and in fact makes more sense. */
  if ((rc = snd_pcm_hw_params_set_rate(ghPCM, hwparams, exact_rate, dir)) < 0)
  {
    PLogError ( "Audio In Error snd_pcm_hw_params_set_rate() (rc = %d: %s)\n", rc, snd_strerror(rc));
    return LHS_E_AUDIOIN_COULDNOTOPENDEVICE;
  }
#endif


  if ((rc = snd_pcm_hw_params_set_channels(ghPCM, hwparams, NR_OF_CHANNELS)) < 0)
  {
    PLogError ( "Audio In Error snd_pcm_hw_params_set_channels() (rc = %d: %s)\n", rc, snd_strerror(rc));
    return LHS_E_AUDIOIN_COULDNOTOPENDEVICE;
  }

  if ((rc = snd_pcm_hw_params(ghPCM, hwparams)) < 0)
  {
    PLogError ( "Audio In Error snd_pcm_hw_params() (rc = %d: %s)\n", rc, snd_strerror(rc));
    return LHS_E_AUDIOIN_COULDNOTOPENDEVICE;
  }

  /* step 3 : preparing for read */
  /* --------------------------- */

  /*prepare the channel */

  if ((rc = snd_pcm_prepare(ghPCM)) < 0)
  {
    PLogError ( "Audio In Error snd_pcm_prepare() (rc = %d: %s)\n", rc, snd_strerror(rc));
    return LHS_E_AUDIOIN_COULDNOTOPENDEVICE;
  }

  /* set some variables */
  gAudioInInfo.u32SamplesAvailable = 0;


#endif

  /* prepare to read samples */
  setCloseOff();

  return 0;
}


/* -------------------------------------------------------------------------+
   |   GLOBAL FUNCTIONS (prototypes in header file)                           |
   + -------------------------------------------------------------------------*/

/**************************************************************
 * lhs_audioinOpenEx                                             *
 *                                                             *
 * notes :                                                     *
 *  -the input parameters are in fact not used but present     *
 *    to ensure compatibility with Win32 implementations       *
 **************************************************************/
LHS_AUDIOIN_ERROR  lhs_audioinOpenEx (
    unsigned long u32AudioInID,         /*@parm [in]  Audio-in device ID (ranges from 0 to a number of available
                                          devices on the system). You can also use the following flag
                                          instead of a device identifier.
                                          <nl><nl><bold WAVE_MAPPER> = The function selects a
                                          waveform-audio input device capable of recording in the
                                          specified format. <bold Header:> Declared in Mmsystem.h from
                                          the Windows Multimedia: Platform SDK.*/
    unsigned long u32Frequency,         /*@parm [in]  Frequency of the recognition engine in Hz. */
    unsigned long u32NbrOfFrames,       /*@parm [in]  Number of frames buffered internally. */
    unsigned long u32SamplesPerFrame,   /*@parm [in]  Size, in samples, of each individual frame. */
    AUDIOIN_H * phAudioIn               /*@parm [out] Handle to the audio-in device */
    )
{
  //initialize some of the static variables.
  if( Initialize(phAudioIn) )
    return ~LHS_AUDIOIN_OK;


  /* prepare sound */
  if (OpenAndPrepareSound(u32Frequency) != 0)
  {
    return LHS_E_AUDIOIN_COULDNOTOPENDEVICE;
  }

  //signal the thread that it has to stop running.
  pthread_mutex_lock ( &gAudioMutex );
  //signal to tell that our thread is now running.
  if ( pthread_cond_signal ( &gOpenExCalled ) != 0 )
  {
    pthread_mutex_unlock ( &gAudioMutex );
    PLogError ( "Audio In Error pthread_cond_signal\n" );
    exit ( 1 );
  }
  gOpenExCalledSignaled = 1;
  pthread_mutex_unlock ( &gAudioMutex );

#ifdef FILTER_ON
  /* need to make this more generic to support different filters */
  pFIR = FIR_construct(filter_length, ps16FilterCoeff_up1_down4, u16ScaleFilterCoeff_up1_down4, FACTOR_UP, FACTOR_DOWN);
  if (pFIR == NULL)
  {
    // TO DO: HANDLE THIS (or modify for static allocation)
  }
#endif

  /* set the status to normal */
  gAudioInInfo.eStatusInfo = AUDIOIN_NORMAL;

  /* do not care, but some applications are checking a NULL handle */
  *phAudioIn = (void *)10;

#ifdef AUDIOIN_SUPPORT_CALLBACK
  gpCallback         = NULL;
  gpCallbackInstance = NULL;
  gnCallbackSamples  = 0;
#endif

  return LHS_AUDIOIN_OK;
}

/**************************************************************
 * lhs_audioinOpen                                             *
 *                                                             *
 * notes :                                                     *
 *  -the input parameters are in fact not used but present     *
 *    to ensure compatibility with Win32 implementation        *
 **************************************************************/
LHS_AUDIOIN_ERROR  lhs_audioinOpen (
    unsigned long u32AudioInID,         /*@parm [in]  Audio-in device ID (ranges from 0 to a number of available
                                          devices on the system). You can also use the following flag
                                          instead of a device identifier.
                                          <nl><nl><bold WAVE_MAPPER> = The function selects a
                                          waveform-audio input device capable of recording in the
                                          specified format. <bold Header:> Declared in Mmsystem.h from
                                          the Windows Multimedia: Platform SDK.*/
    unsigned long u32Frequency,         /*@parm [in]  Frequency of the recognition engine in Hz. */
    AUDIOIN_H * phAudioIn               /*@parm [out] Handle to the audio-in device */
    )
{
  return lhs_audioinOpenEx(u32AudioInID, u32Frequency, 0, 0, phAudioIn);
} /* lhs_audioinOpen */

#ifdef AUDIOIN_SUPPORT_CALLBACK
/**************************************************************
 * lhs_audioinOpenCallback                                     *
 *                                                             *
 * notes :                                                     *
 *  -the input parameters are in fact not used but present     *
 *    to ensure compatibility with Win32 implementation        *
 **************************************************************/
LHS_AUDIOIN_ERROR  lhs_audioinOpenCallback (
    unsigned long u32AudioInID,         /*@parm [in]  Audio-in device ID (ranges from 0 to a number of available
                                          devices on the system). You can also use the following flag
                                          instead of a device identifier.
                                          <nl><nl><bold WAVE_MAPPER> = The function selects a
                                          waveform-audio input device capable of recording in the
                                          specified format. <bold Header:> Declared in Mmsystem.h from
                                          the Windows Multimedia: Platform SDK.*/
    unsigned long u32Frequency,         /*@parm [in]  Frequency of the recognition engine in Hz. */
    unsigned long u32NbrOfSamples,      /*@parm [in] <nl><bold Input:> Number of samples requested per callback */  
    pCallbackFunc pCallback,            /*@parm [in] callback function */
    void         *pCallbackInstance,    /*@parm [in] callback instance */
    AUDIOIN_H * phAudioIn               /*@parm [out] Handle to the audio-in device */
    )
{
  LHS_AUDIOIN_ERROR lhsErr;

#ifdef FILTER_ON
  gCodecFragmentSizeInFrames = u32NbrOfSamples * 4;
#else
  gCodecFragmentSizeInFrames = u32NbrOfSamples;
#endif

  if ((pCallback == NULL) || (u32NbrOfSamples == 0))
  {
    return LHS_E_AUDIOIN_INVALIDARG;
  } 
  lhsErr = lhs_audioinOpenEx(u32AudioInID, u32Frequency, 0, 0, phAudioIn);
  if (lhsErr != LHS_AUDIOIN_OK)
  {
    return lhsErr;
  }

  /* install callback */
  gpCallback         = pCallback;
  gpCallbackInstance = pCallbackInstance;  
  gnCallbackSamples  = u32NbrOfSamples;

  /* callback notification */
  gpCallback(*phAudioIn, AUDIOIN_MSG_OPEN, gpCallbackInstance, NULL, NULL);

  return LHS_AUDIOIN_OK;

} /* lhs_audioinOpenCallback */
#endif

/**************************************************************
 * lhs_audioinClose                                            *
 *                                                             *
 * notes :                                                     *
 *  -the input parameters are in fact not used but present     *
 *    to ensure compatibility with Win32 implementations       *
 **************************************************************/

LHS_AUDIOIN_ERROR lhs_audioinClose(AUDIOIN_H *phAudioIn)
{
  int doneWaiting = 0;

  /* Validate the handle */
  if ((phAudioIn == NULL) || (*phAudioIn == NULL))
  {
    return LHS_E_AUDIOIN_NULLPOINTER;
  }

  /* stop recording audio samples */
  setRecordOff();

  /* stop reading audio samples */
  setCloseOn();

  //wait for the thread to stop reading samples.
  pthread_mutex_lock ( &gAudioMutex );

  while (!doneWaiting)
  {
    int rc = pthread_cond_wait(&gCloseCalled, &gAudioMutex);
    switch (rc)
    {
      case 0:
        if (!gCloseCalledSignaled)
        {
          // Avoid spurious wakeups
          continue;
        }
        else
        {
          gCloseCalledSignaled = 0;
          doneWaiting = 1;
          break;
        }
        break;
      default:
        PLogError ( "Audio In Error pthread_cond_wait\n" );
        pthread_mutex_unlock(&gAudioMutex);
        return ~LHS_AUDIOIN_OK;
    }
  }
  pthread_mutex_unlock(&gAudioMutex);

#ifdef FILTER_ON
  FIR_deconstruct(pFIR);
#endif

#ifdef AUDIOIN_SUPPORT_CALLBACK
  /* callback notification */
  if (gpCallback != NULL) gpCallback(*phAudioIn, AUDIOIN_MSG_CLOSE, gpCallbackInstance, NULL, NULL);
#endif

  return LHS_AUDIOIN_OK;
}

/**************************************************************
 * lhs_audioinStart                                            *
 *                                                             *
 * notes :                                                     *
 *  -the input parameters are in fact not used but present     *
 *    to ensure compatibility with Win32 implementations       *
 *  -in fact the recording is never stopped or started, when   *
 *    non in 'start' status, the samples are just ignored      *
 **************************************************************/

LHS_AUDIOIN_ERROR lhs_audioinStart(AUDIOIN_H hAudioIn)
{
#ifdef SAVE_RAW_AUDIO
  char file_name [256];

  gettimeofday ( &buffer_save_audio, NULL );
  sprintf ( file_name, "data_%ld_%ld.raw", buffer_save_audio.tv_sec, buffer_save_audio.tv_usec );
  audio_data = fopen ( file_name, "w" );
#endif
  if (hAudioIn == NULL)
  {
    return LHS_E_AUDIOIN_NULLPOINTER;
  }

  pthread_mutex_lock ( &gAudioMutex );

#ifdef FILTER_ON
  FIR_reset(pFIR);
#endif

  gWriteIndexPointer = 0;
  gReadIndexPointer = 0;
  gAudioInInfo.u32SamplesAvailable = 0;

  /* start recording */
  setRecordOn();

#ifdef AUDIOIN_SUPPORT_CALLBACK
  /* callback notification */
  if (gpCallback != NULL) gpCallback(hAudioIn, AUDIOIN_MSG_START, gpCallbackInstance, NULL, NULL);
#endif
  pthread_mutex_unlock ( &gAudioMutex );

  return LHS_AUDIOIN_OK;
}

/**************************************************************
 * lhs_audioinStop                                             *
 *                                                             *
 * notes :                                                     *
 *  -the input parameters are in fact not used but present     *
 *    to ensure compatibility with Win32 implementations       *
 *  -in fact the recording is never stopped or started, when   *
 *    non in 'start' status, the samples are just ignored      *
 **************************************************************/

LHS_AUDIOIN_ERROR lhs_audioinStop(AUDIOIN_H hAudioIn)
{
#ifdef SAVE_RAW_AUDIO
  fclose ( audio_data );
#endif
  if (hAudioIn == NULL)
  {
    return LHS_E_AUDIOIN_NULLPOINTER;
  }
  pthread_mutex_lock ( &gAudioMutex );

  /* stop recording (discard samples) */
  setRecordOff();

#ifdef AUDIOIN_SUPPORT_CALLBACK
  /* callback notification */
  if (gpCallback != NULL) gpCallback(hAudioIn, AUDIOIN_MSG_STOP, gpCallbackInstance, NULL, NULL);
#endif
  pthread_mutex_unlock ( &gAudioMutex );

  return LHS_AUDIOIN_OK;
}

/**************************************************************
 * lhs_audioinGetSamples                                       *
 *                                                             *
 * notes :                                                     *
 **************************************************************/

LHS_AUDIOIN_ERROR lhs_audioinGetSamples(AUDIOIN_H hAudioIn, unsigned long * u32NbrOfSamples, void * pAudioBuffer, AUDIOIN_INFO * pgAudioInInfo)
{
  unsigned long cSamples;
  //unsigned long nToCopy;

  /* Check if the handle is valid */
  if (hAudioIn == NULL)
  {
    return LHS_E_AUDIOIN_NULLPOINTER;
  }

  cSamples = 0;

  while (1)
  {
    /* wait until we have enough samples */
    if (*u32NbrOfSamples <= gAudioInInfo.u32SamplesAvailable)
    {
      /* lock the code to prevent dual access to some variables */
      pthread_mutex_lock(&gAudioMutex);

      /* TO DO: consider copying in chunks (like in AquisitionThread) 
         rather than 1 sample at a time. */

      /* copy all samples into the input buffer */
      while ((cSamples < *u32NbrOfSamples))
      {
        ((audioinSample *)pAudioBuffer)[cSamples++] = gSamplesBufferCircularFifo[gReadIndexPointer++];

        /* adapt the parameters */
        gAudioInInfo.u32SamplesAvailable -= 1;

        /* adapt circular buffer */
        if (gReadIndexPointer >= SAMPLES_BUFFER_SIZE)
        {
          gReadIndexPointer = 0;
        }

        /* enough samples */
        if (cSamples == *u32NbrOfSamples)
        {
          /* return the audioin info structure */
          memcpy(pgAudioInInfo, &gAudioInInfo, sizeof(AUDIOIN_INFO));
          pthread_mutex_unlock(&gAudioMutex);
          return LHS_AUDIOIN_OK;
        }
      }
    }
    else
    {
      /* relinquish CPU.  select() is more reliable than usleep(). */
      timer.tv_sec = 0;
      timer.tv_usec = 10000;
      select(0, NULL, NULL, NULL, &timer);  
    }
  } /* while (1) */
}

/**************************************************************
 * lhs_audioinGetVersion                                       *
 *                                                             *
 * notes : not implemented                                     *
 **************************************************************/

LHS_AUDIOIN_ERROR lhs_audioinGetVersion(unsigned long *pu32Version)
{
  return LHS_E_AUDIOIN_NOTIMPLEMENTED;
}


/**************************************************************
 * lhs_audioinGetVolume/lhs_audioinSetVolume                   *
 *                                                             *
 * notes : not implemented                                     *
 **************************************************************/

LHS_AUDIOIN_ERROR lhs_audioinGetVolume(AUDIOIN_H hAudioIn, unsigned long *pu32Volume)
{
  *pu32Volume = gRecordingVolume;
  return LHS_AUDIOIN_OK;
}

LHS_AUDIOIN_ERROR lhs_audioinSetVolume(AUDIOIN_H hAudioIn, unsigned long u32Volume)
{
  gRecordingVolume = u32Volume;
  return LHS_E_AUDIOIN_NOTIMPLEMENTED;
}

/**************************************************************
 * lhs_audioinErrorGetString                                   *
 *                                                             *
 * notes : not implemented                                     *
 **************************************************************/

const char *lhs_audioinErrorGetString(const LHS_AUDIOIN_ERROR Error)
{
  return ("unknown error");
}


#else
/******************************************************************************/
/* STUB FUNCTIONS FOR SIMULATOR BUILD (DOES NOT SUPPORT THREADS)              */
/* This code is enabled if both ANDROID and __ARM_ARCH_5__ are defined.       */
/******************************************************************************/

#include "audioin.h"

LHS_AUDIOIN_ERROR  lhs_audioinOpenEx (
    unsigned long u32AudioInID,         /*@parm [in]  Audio-in device ID (ranges from 0 to a number of available
                                          devices on the system). You can also use the following flag
                                          instead of a device identifier.
                                          <nl><nl><bold WAVE_MAPPER> = The function selects a
                                          waveform-audio input device capable of recording in the
                                          specified format. <bold Header:> Declared in Mmsystem.h from
                                          the Windows Multimedia: Platform SDK.*/
    unsigned long u32Frequency,         /*@parm [in]  Frequency of the recognition engine in Hz. */
    unsigned long u32NbrOfFrames,       /*@parm [in]  Number of frames buffered internally. */
    unsigned long u32SamplesPerFrame,   /*@parm [in]  Size, in samples, of each individual frame. */
    AUDIOIN_H * phAudioIn               /*@parm [out] Handle to the audio-in device */
    )
{
  return LHS_E_AUDIOIN_NOTIMPLEMENTED;
}

LHS_AUDIOIN_ERROR  lhs_audioinOpen (
    unsigned long u32AudioInID,         /*@parm [in]  Audio-in device ID (ranges from 0 to a number of available
                                          devices on the system). You can also use the following flag
                                          instead of a device identifier.
                                          <nl><nl><bold WAVE_MAPPER> = The function selects a
                                          waveform-audio input device capable of recording in the
                                          specified format. <bold Header:> Declared in Mmsystem.h from
                                          the Windows Multimedia: Platform SDK.*/
    unsigned long u32Frequency,         /*@parm [in]  Frequency of the recognition engine in Hz. */
    AUDIOIN_H * phAudioIn               /*@parm [out] Handle to the audio-in device */
    )
{
  return LHS_E_AUDIOIN_NOTIMPLEMENTED;
}

#ifdef AUDIOIN_SUPPORT_CALLBACK
LHS_AUDIOIN_ERROR lhs_audioinOpenCallback(unsigned long u32AudioInID, unsigned long u32Frequency, unsigned long u32NbrOfSamples, pCallbackFunc pCallback, void* pCallbackInstance, AUDIOIN_H * phAudioIn)
{
  return LHS_E_AUDIOIN_NOTIMPLEMENTED;
}
#endif

LHS_AUDIOIN_ERROR lhs_audioinClose(AUDIOIN_H *phAudioIn)
{
  return LHS_E_AUDIOIN_NOTIMPLEMENTED;
}

LHS_AUDIOIN_ERROR lhs_audioinStart(AUDIOIN_H hAudioIn)
{
  return LHS_E_AUDIOIN_NOTIMPLEMENTED;
}

LHS_AUDIOIN_ERROR lhs_audioinStop(AUDIOIN_H hAudioIn)
{
  return LHS_E_AUDIOIN_NOTIMPLEMENTED;
}

LHS_AUDIOIN_ERROR lhs_audioinGetSamples(AUDIOIN_H hAudioIn, unsigned long * u32NbrOfSamples, void * pAudioBuffer, AUDIOIN_INFO * pgAudioInInfo)
{
  return LHS_E_AUDIOIN_NOTIMPLEMENTED;
}

LHS_AUDIOIN_ERROR lhs_audioinGetVersion(unsigned long *pu32Version)
{
  return LHS_E_AUDIOIN_NOTIMPLEMENTED;
}

LHS_AUDIOIN_ERROR lhs_audioinGetVolume(AUDIOIN_H hAudioIn, unsigned long *pu32Volume)
{
  return LHS_E_AUDIOIN_NOTIMPLEMENTED;
}

LHS_AUDIOIN_ERROR lhs_audioinSetVolume(AUDIOIN_H hAudioIn, unsigned long u32Volume)
{
  return LHS_E_AUDIOIN_NOTIMPLEMENTED;
}

const char *lhs_audioinErrorGetString(const LHS_AUDIOIN_ERROR Error)
{
  return "LHS_E_AUDIOIN_NOTIMPLEMENTED";
}

#endif
