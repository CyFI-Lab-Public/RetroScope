/*---------------------------------------------------------------------------*
 *  frontapi.h                                                               *
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



#ifndef _h_frontapi_
#define _h_frontapi_

#include "creccons.h"   /* CREC Public Constants    */

#include "front.h"
#include "sample.h"
#include "utteranc.h"
#include "caexcept.h"

#ifdef __cplusplus
extern "C"
{
#endif

  /**
   ************************************************************************
   * SwigImageBegin       <- DO NOT MOVE THIS LINE !
   *
   * This is a CADOC Keyword section.
   *
   * If CADOC is instructed to create a SWIG I-File and this is one of the
   * files in the input list, everything between the 'SwigImage Begin' and
   * 'SwigImage End' keywords comment blocks will be copied 'as-is' to the
   * SWIG I-File specified on the CADOC command line.
   *
   ************************************************************************
   */
#include "mutualob.h"

#ifndef SWIGBUILD

  typedef struct
  {
    int                 ca_rtti;
    booldata            is_configured;
    booldata            is_configuredForAgc;
    booldata            is_configuredForVoicing;
    booldata            is_attached;
    wave_info           data;
    voicing_info        voice;
  }
  CA_Wave;

  typedef struct
  {
    int                 ca_rtti;
    booldata            is_configured;
    booldata            is_filter_loaded;
    int                 status;
    int                 samplerate;
    float               src_scale;
    float               sink_scale;
    int                 offset;
    front_config        *config;
  }
  CA_Frontend;

#endif

  /**
   ************************************************************************
   * SwigImageEnd         <- DO NOT MOVE THIS LINE !
   ************************************************************************
   */

  /*
  **  Frontend
  */

  CA_Frontend* CA_AllocateFrontend(float srcscale,
                                   int offset,
                                   float sinkscale);
  /**
   *
   * Params       srcscale    Gain applied to incoming wave data
   *              offset      DC offset applied to incoming wave data
   *              sinkscale   Gain applied to any wave data sinks
   *
   * Returns      Handle to new Front End object
   *
   * See          CA_FreeFrontend
   *
   ************************************************************************
   * Allocates a front-end object
   ************************************************************************
   */


  void CA_FreeFrontend(CA_Frontend* hFrontend);
  /**
   *
   * Params       hFrontend   Handle to valid front-end object
   *
   * Returns      void
   *
   * See          CA_AllocateFrontend
   *
   ************************************************************************
   * Deletes a front-end object
   ************************************************************************
   */


  void CA_ConfigureFrontend(CA_Frontend *hFrontend,
                            CA_FrontendInputParams *hFrontArgs);
  /**
   *
   * Params       hFrontend   Handle to valid front-end object
   *              hFrontpars  Handle to valid front-end input parameters object
   *
   * Returns      void
   *
   * See          CA_UnconfigureFrontend
   *
   ************************************************************************
   * Set up the front end using the paramteters. This function
   * configures the member Wave, Freq and Cep objects, by calling their
   * create and setup functions.
   ************************************************************************
   */


  void CA_SetWarpScale(CA_Frontend *hFrontend, float wscale);
  /**
   *
   * Params       hFrontend   Handle to valid front-end object
   *              wscale      warp scale
   *
   * Returns      void
   *
   * See          CA_SetFrontendParameter
   *
   ************************************************************************
   ************************************************************************
   */

  void CA_UnconfigureFrontend(CA_Frontend *hFrontend);
  /**
   *
   * Params       hFrontend   Handle to valid front-end object
   *
   * Returns      void
   *
   * See          CA_ConfigureFrontend
   *
   ************************************************************************
   * Undo all of the front end configurations
   ************************************************************************
   */


  int  CA_MakeFrame(CA_Frontend* hFrontend,
                    CA_Utterance* hUtt,
                    CA_Wave* hWave);
  /**
   *
   * Params       hFrontend   Handle to valid front-end object
   *              hUtt        Handle to valid utterance object
   *              hWave       Handle to valid wave object
   *
   * Returns      non-zero if a frame was constructed
   *
   ************************************************************************
   * Constructs a single frame from audio in the wave object.  The output
   * frame is inserted into the utterance.
   *
   * Frames may not be built. An initial start-up condition prevents
   * the first few frames of audio from being used for frames, in this case
   * this method returns zero to the caller.
   ************************************************************************
   */


  int  CA_GetFrontendFramesPerValidFrame(CA_Frontend *hFrontend);
  int  CA_GetFrontendSampleRate(CA_Frontend *hFrontend);
  /**
   *
   * Params       hFrontend   Handle to valid front-end object
   *
   * Returns       Current sample rate (Hz)
   *
   * See          CA_ConfigureFrontend
   *
   ************************************************************************
   * Gets the sample rate from the front end object
   ************************************************************************
   */


  int  CA_GetFrontendUtteranceDimension(CA_Frontend *hFrontend);
  /**
   *
   * Params       hFrontend   Handle to valid front-end object
   *
   * Returns      The dimension of utterance that the front end will build
   *
   * See          CA_InitUtteranceForFile
   *              CA_InitUtteranceForFrontend
   *              CA_LoadUtteranceFrame
   *
   ************************************************************************
   * Returns the number of items in an utterance.
   ************************************************************************
   */


  int  CA_GetRecognitionHoldoff(CA_FrontendInputParams *hFrontPar);
  /**
   *
   * Params       hFrontpar  Handle to valid front-end input parameters object
   *
   * Returns      The minimum recognition holdoff period, i.e. the minimum
   *              gap between the frontend and recognizer.
   *
   ************************************************************************
   * Computes the Recognition Holdoff Parameter value.  Setting the par in
   * a par file is unnecessary if this function is used.
   ************************************************************************
   */


  /*
  **  Channel normalization
  */
ESR_ReturnCode CA_GetCMSParameters ( CA_Wave *hWave, LCHAR *param_string, size_t* len );
ESR_ReturnCode CA_SetCMSParameters ( CA_Wave *hWave, const LCHAR *param_string );


  void CA_ReLoadCMSParameters(CA_Wave *hWave,
                              const char *basename);

  void CA_LoadCMSParameters(CA_Wave *hWave,
                            const char *basename,
                            CA_FrontendInputParams
                            *hFrontArgs);
  /**
   *
   * Params       hWave       Handle to valid wave object
   *              basename    Forename of .cmn and .tmn files that store the parameters
   *              hFrontPar       Handle to a valid CRhFrontendInputParams
   *
   * Returns      Nothing
   *
   * See          CA_ConfigureCMSparameters
   *              CA_SaveCMSParameters
   *              CA_AttachCMStoUtterance
   *
   ************************************************************************
   * Loads CMS parameters from file.  Sets up the CMS calculations
   * The .cmn and .tmn files must obviously have the same basename.
   ************************************************************************
   */


  void CA_SaveCMSParameters(CA_Wave *hWave,
                            const char *basename);
  /**
   *
   * Params       hWave       Handle to valid wave object
   *              basename    Forename of .cmn and .tmn files that are
   *                          to store the parameters
   *
   * Returns      Nothing
   *
   * See          CA_LoadCMSParameters
   *              CA_ClearCMSParameters
   *
   ************************************************************************
   * This method writes out .CMN and .TMN files.
   *
   * The .CMN file may differ from the one used during a call to
   * CA_LoadCMSParameters() due to channel adaptation.
   *
   * The .cmn and .tmn files will obviously have the same basename.
   ************************************************************************
   */


  void CA_ClearCMSParameters(CA_Wave *hWave);
  /**
   *
   * Params       hWave       Handle to valid wave object
   *
   * Returns      Nothing
   *
   * See          CA_LoadCMSParameters
   *              CA_SaveCMSParameters
   *              CA_DetachCMSfromUtterance
   *
   ************************************************************************
   * This method clears any CMS information.  Must call
   * CA_DetachCMSfromUtterance on the Wave first, if CA_AttachCMStoUtterance
   * has been called.
   ************************************************************************
   */

  void CA_AttachCMStoUtterance(CA_Wave *hWave,
                               CA_Utterance *hUtt);
  /**
   *
   * Params       hWave   Handle to valid Wave structure
   *              hUtt    Handle to an utterance object to clear
   *
   * Returns      Nothing.
   *
   * See          CA_LoadCMSParameters
   *              CA_DetachCMStoUtterance
   *
   ************************************************************************
   *  The CMS data items are inherited from the CA_Wave object to the
   *  CA_Utterance.  All calculations carried out with this utterance
   *  object will now result in new statistics being inherited by the
   *  CA_Wave object.
   ************************************************************************
   */

  /**
   * Returns true if CMS is attached to utterance.
   *
   * @param hWave wave handle
   * @param isAttached [out] True if attached
   */
  ESR_ReturnCode CA_IsCMSAttachedtoUtterance(CA_Wave* hWave, ESR_BOOL* isAttached);

  /**
   * Returns true if CA_Wave is configured for Agc.
   *
   * @param hWave wave handle
   * @param isAttached [out] True if attached
   */
  ESR_ReturnCode CA_IsConfiguredForAgc(CA_Wave* hWave, ESR_BOOL* isConfigured);

  void CA_DetachCMSfromUtterance(CA_Wave *hWave,
                                 CA_Utterance *hUtt);
  /**
   *
   * Params       hWave   Handle to valid Wave structure
   *              hUtt    Handle to an utterance object to clear
   *
   * Returns      Nothing.
   *
   * See          CA_LoadCMSParameters
   *              CA_AttachCMStoUtterance
   *              CA_ClearCMSParameters
   *
   ************************************************************************
   *  The CMS data items are now dis-inherited by the CA_Utterance object.
   *  Calculations carried out with this utterance
   *  object will no longer result in new statistics being inherited by the
   *  CA_Wave object.
   ************************************************************************
   */


  void CA_CalculateCMSParameters(CA_Wave *hWave);
  /**
   *
   * Params       hWave       Handle to valid wave object
   *
   * Returns
   *
   * See          CA_LoadCMSParameters
   *              CA_AttachCMStoUtterance
   *              CA_DiscardCMSAccumulates
   *
   ************************************************************************
   * This routine updates the values used for cepstrum mean subtraction
   * using a running estimation algorithm.
   * A call must first have been made to CA_LoadCMSparameters and
   * subsequently to a CA_AttachCMStoUtterance.
   ************************************************************************
   */


  void CA_DiscardCMSAccumulates(CA_Wave *hWave);
  /**
   *
   * Params       hWave       Handle to valid wave object
   *
   * Returns
   *
   * See          CA_LoadCMSParameters
   *              CA_AttachCMStoUtterance
   *              CA_CalculateCMSParameters
   *
   ************************************************************************
   * This routine clears the updates used for the running estimation of
   * cepstrum mean subtraction.
   * CMS parameters must have been loaded and attached to the wave.
   ************************************************************************
   */



  /*
  **  Wave
  */
  /**
   ************************************************************************
   * CA_Wave methods
   *
   * The wave-input object that represents a file or a device.
   * It maintains sample buffers and data associated with that input stream
   * such as agc, talk-over etc.
   *
   * However the process of getting samples into the CA_Wave object from
   * a physical device is external to CREC-API.
   ************************************************************************
   */

  CA_Wave* CA_AllocateWave(char typ);
  /**
   *
   * Params       typ     A waveform type charcater
   *
   * Returns      Handle to a new Wave structure
   *
   * See          CA_FreeWave
   *              CA_ConfigureWave
   *
   ************************************************************************
   * Creates a Wave structure.
   *
   * There are several options for the 'typ' character, each is listed below:
   *  'M' mu-Law
   *  'P' PCM  (i.e. 8k or 11kHz PCM Files)
   *  'R' RIFF
   *  'N' NIST
   *
   * Once allocated, the returned object should be configured with a call
   * to CA_ConfigureWave().
   ************************************************************************
   */


  void CA_ConfigureWave(CA_Wave *hWave,
                        CA_Frontend *hFrontend);
  /**
   *
   * Params       hWave   Handle to a previously created Wave structure
   *              hFrontend   Handle to valid front-end object
   *
   * Returns      void
   *
   * See          CA_UnconfigureWave
   *
   ************************************************************************
   * Initializes a Wave structure.
   *
   * This should be called before any other Wave methods.
   ************************************************************************
   */


  void CA_ConfigureVoicingAnalysis(CA_Wave *hWave,
                                   CA_FrontendInputParams *hFrontPar);
  /**
   *
   * Params       hWave           Handle to a previously created Wave structure
   *              hFrontPar       Handle to a valid CRhFrontendInputParams
   *
   * Returns      void
   *
   ************************************************************************
   * Initializes a Wave's voicin analysis module.
   ************************************************************************
   */


  void CA_ResetWave(CA_Wave *hWave);
  /**
   *
   * Params       hWave   Handle to a previously created Wave structure
   *
   * Returns      Nothing
   *
   ************************************************************************
   * This prepares the wave object for re-use.  The voicing information
   * is cleared. It is *essential* to call this function if the Wave object
   * is used for streaming live data, or multiple-utterance wave files.
   *
   ************************************************************************
   */


  void CA_UnconfigureWave(CA_Wave *hWave);
  /**
   *
   * Params       hWave   Handle to a previously created Wave structure
   *
   * Returns      void
   *
   * See          CA_ConfigureWave
   *
   ************************************************************************
   * Uninitializes a Wave structure.
   ************************************************************************
   */


  void CA_FreeWave(CA_Wave *hWave);
  /**
   *
   * Params       hWave   Handle to a previously created Wave structure
   *
   * Returns      Nothing, the Wave object is no longer valid
   *
   * See          CA_AllocateWave
   *
   ************************************************************************
   * Removes a previously allocated Wave structure
   ************************************************************************
   */


  int  CA_OpenWaveFromFile(CA_Wave *hWave,
                           char* filename,
                           char typ,
                           int endian,
                           int do_write,
                           int samplerate);
  /**
   *
   * Params       hWave       Handle to valid Wave structure
   *              filename    ASCII, null-terminated filename string
   *              typ         File attribute character
   *              endian      Binary storage, use 'LITTLE' or 'BIG'
   *              do_write    Adds a RIFF header if non-ZERO
   *              samplerate  File's sample rate (Hz)
   *
   * Returns      non-ZERO if successful
   *
   * See          CA_CloseFile
   *
   ************************************************************************
   * Initializes the Wave structure for use with a known filename.
   *
   * There are several options for the 'typ' character, each is listed below:
   *  'M' mu-Law Files
   *  'P' PCM Files (i.e. 8k or 11kHz PCM Files)
   *  'R' RIFF
   *  'N' NIST
   *
   * It is an error to call this function without first configuring hWave
   ************************************************************************
   */


  void CA_CloseFile(CA_Wave *hWave);
  /**
   *
   * Params       hWave   Handle to valid Wave structure
   *
   * Returns      Nothing, the opened file is closed
   *
   * See          CA_OpenWaveFromFile
   *
   ************************************************************************
   * It is an error to call this function without first configuring hWave
   ************************************************************************
   */


  int  CA_OpenWaveFromDevice(CA_Wave *hWave,
                             int wave_type,
                             int samplerate,
                             int device_id,
                             int device_type);
  /**
   *
   * Params       hWave       Handle to valid Wave structure
   *              wave_type   Audio format
   *              samplerate  Device sample rate (Hz)
   *              device_id   The Physical device number of the
   *                          waveform hardware (normally starting at 0)
   *              device_type The type of device
   *
   * Returns      non-ZERO if successful
   *
   * See          CA_CloseDevice
   *
   ************************************************************************
   * Initializes the Wave structure for use with a known filename.
   *
   * 'wave_type' should be either DEVICE_MULAW or DEVICE_RAW_PCM.  This
   * enables correct internal interpretation of the audio samples.  If the
   * device is an output device 'wave_type' should be 0 (ZERO).
   *
   * It is an error to call this function without first configuring hWave
   *
   * 'device_type' should be one of WAVE_DEVICE_MSWAVE (for real devices)
   * or WAVE_DEVICE_RAW (if using the raw interface - CA_LoadSamples)
   ************************************************************************
   */


  void CA_CloseDevice(CA_Wave *hWave);
  /**
   *
   * Params       hWave   Handle to valid Wave structure
   *
   * Returns      Nothing, the opened device is closed
   *
   * See          CA_OpenWaveFromDevice
   *
   ************************************************************************
   * It is an error to call this function without first configuring hWave
   ************************************************************************
   */


  void CA_SetWaveBoostValue(CA_Wave *hWave,
                            int waveBoost);
  /**
   *
   * Params       hWave       Handle to valid Wave structure
   *              waveBoost   The input sample scaling value (in %)
   *
   * Returns      void
   *
   ************************************************************************
   * The 'offset' and 'gain' parameters are applied to the current
   * waveform buffer and the resultant samples clamped to a 16-bit
   * audio range.
   *
   * It is an error to call this function without first configuring hWave
   ************************************************************************
   */


  int  CA_GetWaveBoostValue(CA_Wave *hWave);
  /**
   *
   * Params       hWave   Handle to valid Wave structure
   *
   * Returns      The input sample scaling value (in %)
   *
   ************************************************************************
   * It is an error to call this function without first configuring hWave
   ************************************************************************
   */


  int  CA_GetSampleRate(CA_Wave *hWave);
  /**
   *
   * Params       hWave       Handle to valid Wave structure
   *
   * Returns      Sample rate used by the wave device
   *
   * See          CA_LoadSamples
   *              CA_ConfigureWave
   *
   ************************************************************************
   * It is an error to call this function without first configuring hWave
   ************************************************************************
   */


  void CA_CopyWaveSegment(CA_Wave *source,
                          CA_Wave *sink,
                          unsigned long offset,
                          unsigned long length);
  /**
   *
   * Params       source  Handle to valid Wave structure for source
   *              sink    Handle to valid Wave structure for destination
   *              offset  Sample number starting the segment (first sample == 0)
   *              length  Number of samples in the segment
   *
   * Returns      Nothing, the wave segment in 'source' is copied to 'sink'
   *
   ************************************************************************
   * This is useful when copying selected sections of a waveform.  It is
   * employed within the SDXCollect_SinkWaveSegment() method.
   *
   * 'source' and 'sinks' must be different objects.
   *
   * It is an error to call this function without first configuring
   * the 'source' and 'sink' wave objects.
   ************************************************************************
   */


  int  CA_GetBufferSize(CA_Wave *hWave);
  /**
   *
   * Params       hWave       Handle to valid Wave structure
   *
   * Returns      Buffer size (in bytes) used by the wave device for
   *              the construction of a single frame.
   *
   * See          CA_LoadSamples
   *              CA_ConfigureWave
   *
   ************************************************************************
   * This function is supplied for use in conjunction with CA_LoadSamples.
   * The application should call CA_CetBufferSIze on the current input Wave
   * object and use the result to create the wave sample buffer that it will
   * supply to CA_LoadSamples.
   *
   * It is an error to call this function without first configuring hWave
   ************************************************************************
   */


#ifndef SWIGBUILD
  int  CA_LoadSamples(CA_Wave *hWave,
                      samdata *pPCMData,
                      int sampleCount);
#else
  int  CA_LoadSamples(CA_Wave *hWave,
                      short *pPCMData,
                      int sampleCount);
#endif
  /**
   *
   * Params       hWave       Handle to valid Wave structure
   *              pPCMData    Pointer to a buffer created by the application.
   *              sampleCount The number of samples in the supplied buffer.
   *
   * Returns      Buffer size (in bytes) used by the wave device for
   *              the construction of a single frame.
   *
   * See          CA_GetBufferSize
   *              CA_ConfigureWave
   *
   ************************************************************************
   * The buffer contains exactly enough wave data to make one frame of an
   * utterance. The required buffer size can be got by calling CA_GetBufferSize.
   * The application is responsible for ensuring that the supplied sample
   * data is in the correct format. The sample rate should match that of the
   * recognizer, and the sample size should be of size 16 bits. CA_LoadSamples
   * will check the sample count, and report an error if it does not match
   * the front end's required buffer size.
   *
   * It is an error to call this function without first configuring hWave
   ************************************************************************
   */


  int  CA_ReadSamplesForFrame(CA_Wave *hWave);
  /**
   *
   * Params       hWave   Handle to valid Wave structure
   *
   * Returns      The number of samples read; <0 on failure.
   *
   * See          CA_SaveSamplesForFrame
   *
   ************************************************************************
   * It is an error to call this function without first configuring hWave
   ************************************************************************
   */


  int  CA_SaveSamplesForFrame(CA_Wave *hWave);
  /**
   *
   * Params       hWave   Handle to valid Wave structure
   *
   * Returns      The number of samples read; <0 on failure.
   *
   * See          CA_ReadSamplesForFrame
   *
   ************************************************************************
   * It is an error to call this function without first configuring hWave
   ************************************************************************
   */


  void CA_ConditionSamples(CA_Wave *hWave);
  /**
   *
   * Params       hWave   Handle to valid Wave structure
   *
   * Returns      Nothing. The incoming audio buffer is 'conditioned'.
   *
   ************************************************************************
   * The 'offset' and 'gain' parameters are applied to the current
   * waveform buffer and the resultant samples clamped to a 16-bit
   * audio range.
   *
   * It is an error to call this function without first configuring hWave
   ************************************************************************
   */


  void CA_CopyWaveSamples(CA_Wave *hWaveIn,
                          CA_Wave *hWaveOut);
  /**
   *
   * Params       hWaveIn     Handle to valid Wave object
   *              hWaveOut    Handle to valid Wave object
   *
   * Returns      The audio samples in the In channel are
   *              copied to the Out channel
   *
   ************************************************************************
   * This method is provided to permit incoming audio sample to be copied
   * to the sink for file storage.  The wave objects should have been
   * created as appropriate sources or sinks.
   *
   * 'hWaveIn' and 'hWaveOut' must be different objects.
   *
   * It is an error to call this function without first configuring
   * the 'hWaveIn' and 'hWaveOut' wave objects.
   ************************************************************************
   */


  int  CA_WaveIsOutput(CA_Wave *hWave);
  /**
   *
   * Params       hWave   Handle to valid Wave structure
   *
   * Returns      Boolean.
   *
   ************************************************************************
   * Returns true if the Wave is setup as a sink of data
   *
   * It is an error to call this function without first configuring hWave
   * and setting it up as a device or a file
   ************************************************************************
   */


  int  CA_WaveIsInput(CA_Wave *hWave);
  /**
   *
   * Params       hWave   Handle to valid Wave structure
   *
   * Returns      Boolean.
   *
   ************************************************************************
   * Returns true if the Wave is setup as a source of data
   *
   * It is an error to call this function without first configuring hWave
   * and setting it up as a device or a file
   ************************************************************************
   */


  int  CA_WaveIsADevice(CA_Wave *hWave);
  /**
   *
   * Params       hWave   Handle to valid Wave structure
   *
   * Returns      Boolean.
   *
   ************************************************************************
   * Returns true if the Wave is setup as a device
   *
   * It is an error to call this function without first configuring hWave
   * and setting it up as a device or a file
   ************************************************************************
   */


  int  CA_WaveIsAFile(CA_Wave *hWave);
  /**
   *
   * Params       hWave   Handle to valid Wave structure
   *
   * Returns      Boolean.
   *
   ************************************************************************
   * Returns true if the Wave is setup as a file of data
   *
   * It is an error to call this function without first configuring hWave
   * and setting it up as a device or a file
   ************************************************************************
   */

  void CA_StartSigCheck(CA_Wave *hWave);
  void CA_StopSigCheck(CA_Wave *hWave);
  void CA_ResetSigCheck(CA_Wave *hWave);
  void CA_GetSigStats(CA_Wave *hWave, int *nsam, int *pclowclip, int *pchighclip,
                      int *dc_offset, int *amp, int *pc5, int *pc95,
                      int *overflow);
  ESR_BOOL CA_DoSignalCheck(CA_Wave *hWave, ESR_BOOL *clipping, ESR_BOOL *dcoffset,
                        ESR_BOOL *highnoise, ESR_BOOL *quietspeech, ESR_BOOL *too_few_samples,
                        ESR_BOOL *too_many_samples);



  /*  Frontend parameter API
  */
  /**
   ************************************************************************
   * CA_FrontendInputParams methods
   *
   * To load the front-end parameters from a par file
   *
   * This object holds input parameters (objtained from an ASCII
   * parameter file) for the Front-End object.
   *
   * Apart from allocating and freeing the object it also has a method
   * used to read a given parameter file.
   ************************************************************************
   */



  CA_FrontendInputParams* CA_AllocateFrontendParameters(void);
  /**
   *
   * Params       void
   *
   * Returns      Handle to new front-end input object
   *
   * See          CA_FreeFrontendParameters
   *              CA_ConfigureFrontend
   *
   ************************************************************************
   * Creates a new front-end input paramater onject
   ************************************************************************
   */


  void CA_LoadFrontendParameters(CA_FrontendInputParams* hFrontpar,
                                 const char* parfile);
  /**
   *
   * Params       hFrontpar   valid front-end input object handle
   *              parfile     par file
   *
   * Returns      void
   *
   * See          CA_SaveFrontendParameters
   *              CA_ConfigureFrontend
   *
   ************************************************************************
   * Loads known front-end parameters from the given .PAR file.
   * The file is a .par file
   ************************************************************************
   */


  void CA_FreeFrontendParameters(CA_FrontendInputParams* hFrontpar);
  /**
   *
   * Params       hFrontpar   valid front-end input object handle
   *
   * Returns      void        front-end object is no longer valid
   *
   * See          CA_AllocateFrontendParameters
   *
   ************************************************************************
   * Removes a previously allocated parameter object
   ************************************************************************
   */


  void CA_SaveFrontendParameters(CA_FrontendInputParams* hFrontpar,
                                 const char* parfile);
  /**
   *
   * Params       hFrontpar   valid front-end input object handle
   *              parfile     parameter (.par) file to read
   *
   * Returns      void
   *
   * See          CA_LoadFrontendParameters
   *              CA_ConfigureFrontend
   *
   ************************************************************************
   * Saves a previously loaded (modified) parameter file
   *
   * It is an error to call this function without first loading
   * front-end input parameters.
   ************************************************************************
   */


  int  CA_SetFrontendParameter(CA_FrontendInputParams *hFrontpar,
                               char *key,
                               char *value);
  /**
   *
   * Params       hFrontpar   valid Front End Parameter handle
   *              key         parameter key (text label)
   *              value       new parameter value (text)
   *
   * Returns      Zero on error
   *
   * See          CA_GetFrontendStringParameter
   *              CA_GetFrontendIntParameter
   *              CA_GetFrontendFloatParameter
   *              CA_LoadFrontendParameters
   *
   ************************************************************************
   * Sets/Modifies a known Front End Input parameter.
   *
   * It is an error to call this function without first loading
   * front-end input parameters.
   ************************************************************************
   */


  int  CA_GetFrontendParameter(CA_FrontendInputParams *hFrontpar,
                               char *key,
                               void *value);
  /**
   *
   * Params       hFrontpar   valid Front End Parameter handle
   *              key         parameter key (text label)
   *              value       pointer to store parameter value (text)
   *              valueLen    size of value buffer
   *
   * Returns      False on error
   *
   * See          CA_SetFrontendParameter
   *              CA_LoadFrontendParameters
   *
   ************************************************************************
   * Reads a known Front End Parameter.
   *
   * It is an error to call this function without first loading
   * front-end input parameters.
   ************************************************************************
   */

  int  CA_GetFrontendStringParameter(CA_FrontendInputParams *hFrontpar,
                                     char *key,
                                     char *value,
                                     int valueLen,
                                     int *bytes_required);
  /**
   *
   * Params       hFrontpar   valid Front End Parameter handle
   *              key         parameter key (text label)
   *              value       pointer to store parameter value
   *  value_len   number of bytes pointed to by value
   *  bytes_required holds the number of bytes neededf to store the data
   *
   * Returns      False on error
   *
   * See          CA_SetFrontendParameter
   *              CA_LoadFrontendParameters
   *
   ************************************************************************
   * Reads a known Front End Parameter.
   *
   * It is an error to call this function without first loading
   * front-end input parameters.
   ************************************************************************
   */

  int CA_LoadSpectrumFilter(CA_Frontend *hFrontend, char *basename);
  /**
   *
   * Params       hFrontend   valid Frontend handle
   *              basename    basename of filter file (a text file)
   *              hFrontArs   valid Frontend parameters handle
   *
   * Returns      False on error
   *
   * See          CA_SetFrontendParameter
   *              CA_LoadFrontendParameters
   *
   ************************************************************************
   * Loads a spectrum filter from a file.
   *
   * It is an error to call this function without first loading
   * front-end input parameters.
   ************************************************************************
   */

  void CA_ClearSpectrumFilter(CA_Frontend *hFrontend);
  /**
   *
   * Params       hFrontend   valid Frontend handle
   *
   * Returns      void
   *
   * See          CA_LoadSpectrumFilter
   *
   ************************************************************************
   * Clears a front end spectrum filter.
   *
   * It is an error to call this function without first loading
   * front-end input parameters.
   ************************************************************************
   */

  int CA_IsSpectrumFilterLoaded(CA_Frontend *hFrontend);
  /**
   *
   * Params       hFrontend   valid Frontend handle
   *
   * Returns      True if front end is loaded with a spectrum filter.
   *
   * See          CA_LoadSpectrumFilter
   *
   ************************************************************************
   *
   ************************************************************************
   */

  void CA_EnableNonlinearFilter(CA_Frontend *hFrontend);
  void CA_DisableNonlinearFilter(CA_Frontend *hFrontend);
  void CA_EnableSpectrumFilter(CA_Frontend *hFrontend);
  void CA_DisableSpectrumFilter(CA_Frontend *hFrontend);

#ifdef __cplusplus
}
#endif


#endif
