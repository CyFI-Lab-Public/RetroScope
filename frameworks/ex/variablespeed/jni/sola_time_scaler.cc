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

#include "sola_time_scaler.h"

#include <math.h>
#include <hlogging.h>
#include <algorithm>

#include "ring_buffer.h"

#define FLAGS_sola_ring_buffer 2.0
#define FLAGS_sola_enable_correlation true


namespace video_editing {

// Returns a cross-correlation score for the specified buffers.
int SolaAnalyzer::Correlate(const float* buffer1, const float* buffer2,
                            int num_frames) {
  CHECK(initialized_);

  int score = 0;
  num_frames *= num_channels_;
  while (num_frames-- > 0) {
    // Increment the score if the sign bits match.
    score += ((bit_cast<int32>(*buffer1++) ^ bit_cast<int32>(*buffer2++)) >= 0)
              ? 1 : 0;
  }
  return score;
}

// Trivial SolaAnalyzer class to bypass correlation.
class SolaBypassAnalyzer : public SolaAnalyzer {
 public:
  SolaBypassAnalyzer() { }
  virtual int Correlate(const float*, const float*, int num_frames) {
    return num_frames * num_channels_;
  }
};


// Default constructor.
SolaTimeScaler::SolaTimeScaler()
    : input_buffer_(NULL), output_buffer_(NULL), analyzer_(NULL) {
  sample_rate_ = 0;
  num_channels_ = 0;

  draining_ = false;
  initialized_ = false;
}

SolaTimeScaler::~SolaTimeScaler() {
  delete input_buffer_;
  delete output_buffer_;
  delete analyzer_;
}

// Injects a SolaAnalyzer instance for analyzing signal frames.
void SolaTimeScaler::set_analyzer(SolaAnalyzer* analyzer) {
  MutexLock lock(&mutex_);  // lock out processing while updating
  delete analyzer_;
  analyzer_ = analyzer;
}

// Initializes a SOLA timescaler.
void SolaTimeScaler::Init(double sample_rate,
                          int num_channels,
                          double initial_speed,
                          double window_duration,
                          double overlap_duration) {
  MutexLock lock(&mutex_);  // lock out processing while updating

  sample_rate_ = sample_rate;
  num_channels_ = num_channels;
  speed_ = initial_speed;
  window_duration_ = window_duration;
  overlap_duration_ = overlap_duration;

  initialized_ = true;
  GenerateParameters();
  Reset();
}

// Adjusts the rate scaling factor.
void SolaTimeScaler::set_speed(double speed) {
  MutexLock lock(&mutex_);  // lock out processing while updating

  speed_ = speed;
  GenerateParameters();
}

// Generates processing parameters from the current settings.
void SolaTimeScaler::GenerateParameters() {
  if (speed_ < 0.1) {
    LOGE("Requested speed %fx limited to 0.1x", speed_);
    speed_ = 0.1;
  } else if (speed_ > 8.0) {
    LOGE("Requested speed %fx limited to 8.0x", speed_);
    speed_ = 8.0;
  }

  ratio_ = 1.0 / speed_;

  num_window_frames_ = nearbyint(sample_rate_ * window_duration_);

  // Limit the overlap to half the window size, and round up to an odd number.
  // Half of overlap window (rounded down) is also a useful number.
  overlap_duration_ = min(overlap_duration_, window_duration_ / 2.0);
  num_overlap_frames_ = nearbyint(sample_rate_ * overlap_duration_);
  num_overlap_frames_ |= 1;
  half_overlap_frames_ = num_overlap_frames_ >> 1;

  if (speed_ >= 1.) {
    // For compression (speed up), adjacent input windows overlap in the output.
    input_window_offset_ = num_window_frames_;
    target_merge_offset_ = nearbyint(num_window_frames_ * ratio_);
  } else {
    // For expansion (slow down), each input window start point overlaps the
    // previous, and they are placed adjacently in the output
    // (+/- half the overlap size).
    input_window_offset_ = nearbyint(num_window_frames_ * speed_);
    target_merge_offset_ = num_window_frames_;
  }

  // Make sure we copy enough extra data to be able to perform a
  // frame correlation over the range of target merge point +/- half overlap,
  // even when the previous merge point was adjusted backwards a half overlap.
  max_frames_to_merge_ = max(num_window_frames_,
      target_merge_offset_ + (2 * num_overlap_frames_));
  min_output_to_hold_=
      max_frames_to_merge_ + num_overlap_frames_ - target_merge_offset_;
}

// The input buffer has one writer and reader.
// The output buffer has one reader/updater, and one reader/consumer.
static const int kInputReader = 0;
static const int kOutputAnalysis = 0;
static const int kOutputConsumer = 1;

void SolaTimeScaler::Reset() {
  CHECK(initialized_);
  double duration = max(FLAGS_sola_ring_buffer, 20. * window_duration_);
  draining_ = false;

  delete input_buffer_;
  input_buffer_ = new RingBuffer();
  input_buffer_->Init(static_cast<int>
      (sample_rate_ * duration), num_channels_, 1);

  delete output_buffer_;
  output_buffer_ = new RingBuffer();
  output_buffer_->Init(static_cast<int>
      (sample_rate_ * ratio_ * duration), num_channels_, 2);

  if (analyzer_ == NULL) {
    if (FLAGS_sola_enable_correlation) {
      analyzer_ = new SolaAnalyzer();
    } else {
      analyzer_ = new SolaBypassAnalyzer();
    }
  }
  analyzer_->Init(sample_rate_, num_channels_);
}

// Returns the number of frames that the input buffer can accept.
int SolaTimeScaler::input_limit() const {
  CHECK(initialized_);
  return input_buffer_->overhead();
}

// Returns the number of available output frames.
int SolaTimeScaler::available() {
  CHECK(initialized_);

  int available = output_buffer_->available(kOutputConsumer);
  if (available > min_output_to_hold_) {
    available -= min_output_to_hold_;
  } else if (draining_) {
    Process();
    available = output_buffer_->available(kOutputConsumer);
    if (available > min_output_to_hold_) {
      available -= min_output_to_hold_;
    }
  } else {
    available = 0;
  }
  return available;
}

void SolaTimeScaler::Drain() {
  CHECK(initialized_);

  draining_ = true;
}


// Feeds audio to the timescaler, and processes as much data as possible.
int SolaTimeScaler::InjectSamples(float* buffer, int num_frames) {
  CHECK(initialized_);

  // Do not write more frames than the buffer can accept.
  num_frames = min(input_limit(), num_frames);
  if (!num_frames) {
    return 0;
  }

  // Copy samples to the input buffer and then process whatever can be consumed.
  input_buffer_->Write(buffer, num_frames);
  Process();
  return num_frames;
}

// Retrieves audio data from the timescaler.
int SolaTimeScaler::RetrieveSamples(float* buffer, int num_frames) {
  CHECK(initialized_);

  // Do not read more frames than available.
  num_frames = min(available(), num_frames);
  if (!num_frames) {
    return 0;
  }

  output_buffer_->Copy(kOutputConsumer, buffer, num_frames);
  output_buffer_->Seek(kOutputConsumer,
                       output_buffer_->Tell(kOutputConsumer) + num_frames);

  return num_frames;
}

// Munges input samples to produce output.
bool SolaTimeScaler::Process() {
  CHECK(initialized_);
  bool generated_data = false;

  // We can only process data if there is sufficient input available
  // (or we are draining the latency), and there is sufficient room
  // for output to be merged.
  while (((input_buffer_->available(kInputReader) > max_frames_to_merge_) ||
         draining_) && (output_buffer_->overhead() >= max_frames_to_merge_)) {
    MutexLock lock(&mutex_);  // lock out updates while processing each window

    // Determine the number of samples to merge into the output.
    int input_count =
        min(input_buffer_->available(kInputReader), max_frames_to_merge_);
    if (input_count == 0) {
      break;
    }
    // The input reader always points to the next window to process.
    float* input_pointer = input_buffer_->GetPointer(kInputReader, input_count);

    // The analysis reader always points to the ideal target merge point,
    // minus half an overlap window (ie, the starting point for correlation).
    // That means the available data from that point equals the number
    // of samples that must be cross-faded.
    int output_merge_cnt = output_buffer_->available(kOutputAnalysis);
    float* output_pointer =
        output_buffer_->GetPointer(kOutputAnalysis, output_merge_cnt);

    // If there is not enough data to do a proper correlation,
    // just merge at the ideal target point. Otherwise,
    // find the best correlation score, working from the center out.
    int merge_offset = min(output_merge_cnt, half_overlap_frames_);

    if ((output_merge_cnt >= (2 * num_overlap_frames_)) &&
        (input_count >= num_overlap_frames_)) {
      int best_offset = merge_offset;
      int best_score = 0;
      int score;
      for (int i = 0; i <= half_overlap_frames_; ++i) {
        score = analyzer_->Correlate(input_pointer,
            output_pointer + ((merge_offset + i) * num_channels_),
            num_overlap_frames_);
        if (score > best_score) {
          best_score = score;
          best_offset = merge_offset + i;
          if (score == (num_overlap_frames_ * num_channels_)) {
            break;  // It doesn't get better than perfect.
          }
        }
        if (i > 0) {
          score = analyzer_->Correlate(input_pointer,
              output_pointer + ((merge_offset - i) * num_channels_),
              num_overlap_frames_);
          if (score > best_score) {
            best_score = score;
            best_offset = merge_offset - i;
            if (score == (num_overlap_frames_ * num_channels_)) {
              break;  // It doesn't get better than perfect.
            }
          }
        }
      }
      merge_offset = best_offset;
    } else if ((output_merge_cnt > 0) && !draining_) {
      LOGE("no correlation performed");
    }

    // Crossfade the overlap between input and output, and then
    // copy in the remaining input.
    int crossfade_count = max(0, (output_merge_cnt - merge_offset));
    crossfade_count = min(crossfade_count, input_count);
    int remaining_count = input_count - crossfade_count;

    float* merge_pointer = output_pointer + (merge_offset * num_channels_);
    float flt_count = static_cast<float>(crossfade_count);
    for (int i = 0; i < crossfade_count; ++i) {
      // Linear cross-fade, for now.
      float input_scale = static_cast<float>(i) / flt_count;
      float output_scale = 1. - input_scale;
      for (int j = 0; j < num_channels_; ++j) {
        *merge_pointer = (*merge_pointer * output_scale) +
                         (*input_pointer++ * input_scale);
        ++merge_pointer;
      }
    }
    // Copy the merged buffer back into the output, if necessary, and
    // append the rest of the window.
    output_buffer_->MergeBack(kOutputAnalysis,
                              output_pointer, output_merge_cnt);
    output_buffer_->Write(input_pointer, remaining_count);

    // Advance the output analysis pointer to the next target merge point,
    // minus half an overlap window.  The target merge point is always
    // calculated as a delta from the previous ideal target, not the actual
    // target, to avoid drift.
    int output_advance = target_merge_offset_;
    if (output_merge_cnt < half_overlap_frames_) {
      // On the first window, back up the pointer for the next correlation.
      // Thereafter, that compensation is preserved.
      output_advance -= half_overlap_frames_;
    }

    // Don't advance beyond the available data, when finishing up.
    if (draining_) {
      output_advance =
          min(output_advance, output_buffer_->available(kOutputAnalysis));
    }
    output_buffer_->Seek(kOutputAnalysis,
        output_buffer_->Tell(kOutputAnalysis) + output_advance);

    // Advance the input pointer beyond the frames that are no longer needed.
    input_buffer_->Seek(kInputReader, input_buffer_->Tell(kInputReader) +
                        min(input_count, input_window_offset_));

    if ((crossfade_count + remaining_count) > 0) {
      generated_data = true;
    }
  }  // while (more to process)
  return generated_data;
}

}  // namespace video_editing
