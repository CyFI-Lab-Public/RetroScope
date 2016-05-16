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

#ifndef FRAMEWORKS_EX_VARIABLESPEED_JNI_SOLA_TIME_SCALER_H_
#define FRAMEWORKS_EX_VARIABLESPEED_JNI_SOLA_TIME_SCALER_H_

#include <android/log.h>

#include <no_synchronization.h>

#include <list>
#include <vector>

#include "macros.h"

// Time-domain audio playback rate scaler using phase-aligned Synchronized
// OverLap Add (SOLA).

namespace video_editing {

class RingBuffer;

// The default SolaAnalyzer implements a sign-bit cross-correlation
// function for determining the best fit between two signals.
class SolaAnalyzer {
 public:
  SolaAnalyzer() : initialized_(false) { }
  virtual ~SolaAnalyzer() { }

  // Initializes a SolaAnalyzer.
  // @param sample_rate sample rate of the audio signal.
  // @param num_channels number of interleaved channels in the signal.
  void Init(int sample_rate, int num_channels) {
    sample_rate_ = sample_rate;
    num_channels_ = num_channels;
    initialized_ = true;
  }

  // Returns a cross-correlation score for the specified buffers.
  // The correlation is performed over all channels of a multi-channel signal.
  // @param buffer1 pointer to interleaved input samples
  // @param buffer2 pointer to interleaved input samples
  // @param num_frames number of input frames (that is to say, number of
  //                   samples / number of channels)
  // @param returns a correlation score in the range zero to num_frames
  virtual int Correlate(const float* buffer1, const float* buffer2,
                        int num_frames);

 protected:
  bool initialized_;
  int sample_rate_;
  int num_channels_;

  DISALLOW_COPY_AND_ASSIGN(SolaAnalyzer);
};


class SolaTimeScaler {
 public:
  // Default constructor.
  SolaTimeScaler();
  virtual ~SolaTimeScaler();

  // Injects a SolaAnalyzer instance for analyzing signal frames.
  // The scaler takes ownership of this instance.
  // This is normally called once, before Init().
  // @param analyzer SolaAnalyzer instance
  void set_analyzer(SolaAnalyzer* analyzer);

  // Initializes a SOLA timescaler.
  // @param sample_rate sample rate of the signal to process
  // @param num_channels number of channels of the signal to process
  // @param initial_speed starting rate scaling factor
  // @param window_duration processing window size, in seconds
  // @param overlap_duration correlation overlap size, in seconds
  void Init(double sample_rate, int num_channels, double initial_speed,
            double window_duration, double overlap_duration);

  // Adjusts the rate scaling factor.
  // This may be called concurrently with processing, and will
  // take effect on the next processing window.
  // @param speed rate scaling factor
  void set_speed(double speed);

  // Indicates that we are done with the input and won't call Process anymore
  // This processes all the data reamining in the analysis buffer.
  void Drain();

  // Flushes the buffers associated with the scaler.
  void Reset();

  // Feeds audio to the timescaler, and processes as much data as possible.
  // @param buffer pointer to interleaved float input samples
  // @param num_frames number of frames (num_samples / num_channels)
  // @returns number of frames actually accepted
  int InjectSamples(float* buffer, int num_frames);

  // Retrieves audio data from the timescaler.
  // @param buffer pointer to buffer to receive interleaved float output
  // @param num_frames maximum desired number of frames
  // @returns number of frames actually returned
  int RetrieveSamples(float* buffer, int num_frames);

  // Returns the number of frames that the input buffer can accept.
  // @returns number of frames for the next Process() call
  int input_limit() const;

  // Returns the number of available output frames.
  // @returns number of frames that can be retrieved
  int available();

  int num_channels() const { return num_channels_; }

 private:
  mutable Mutex mutex_;       // allows concurrent produce/consume/param change
  bool initialized_;          // set true when input parameters have been set
  bool draining_;             // set true to drain latency

  // Input parameters.
  int num_channels_;          // channel valence of audio stream
  double sample_rate_;        // sample rate of audio stream
  double window_duration_;    // the nominal time quantum for processing
  double overlap_duration_;   // the maximum slip for correlating windows
  double speed_;              // varispeed rate

  // Derived parameters.
  double ratio_;              // inverse of speed
  int num_window_frames_;     // window_duration_ expressed as frame count
  int num_overlap_frames_;    // overlap_duration_ expressed as frame count
  int half_overlap_frames_;   // half of the overlap
  int input_window_offset_;   // frame delta between input windows
  int target_merge_offset_;   // ideal frame delta between output windows
  int max_frames_to_merge_;   // ideal frame count to merge to output
  int min_output_to_hold_;    // number of output frames needed for next merge

  RingBuffer* input_buffer_;
  RingBuffer* output_buffer_;
  SolaAnalyzer* analyzer_;

  // Generates processing parameters from the current settings.
  void GenerateParameters();

  // Munges input samples to produce output.
  // @returns true if any output samples were generated
  bool Process();

  DISALLOW_COPY_AND_ASSIGN(SolaTimeScaler);
};

}  // namespace video_editing

#endif  // FRAMEWORKS_EX_VARIABLESPEED_JNI_SOLA_TIME_SCALER_H_
