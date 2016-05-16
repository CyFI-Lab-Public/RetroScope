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

#ifndef FRAMEWORKS_EX_VARIABLESPEED_JNI_RING_BUFFER_H_
#define FRAMEWORKS_EX_VARIABLESPEED_JNI_RING_BUFFER_H_

#include <vector>

#include "integral_types.h"
#include "macros.h"

// Circular buffer of multichannel audio, maintaining the position of the
// writing head and several reading head, and providing a method for
// accessing a contiguous block (through direct reference or the copy in a
// temporary read buffer in case the requested block crosses the buffer
// boundaries).
//
// This code is not thread-safe.

namespace video_editing {

class RingBuffer {
 public:
  RingBuffer();
  virtual ~RingBuffer();

  // Initializes a RingBuffer.
  // @param size: size of the buffer in frames.
  // @param num_channels: number of channels of the original audio.
  // @param num_readers: number of reading heads.
  void Init(int size, int num_channels, int num_readers);

  // Gets the position of a reading head.
  // @param reader reading head index.
  // @returns position pointed to by the #reader reading head.
  int64 Tell(int reader) const;

  // Moves a reading head.
  // @param reader reading head index.
  // @param position target position.
  void Seek(int reader, int64 position);

  // Reads samples for a reading head.
  // @param reader reading head index.
  // @param num_frames number of frames to read.
  // @param destination float buffer to which the samples will be written.
  void Copy(int reader, float* destination, int num_frames) const;

  // Writes samples.
  // @param samples float buffer containing the samples.
  // @param num_frames number of frames to write.
  void Write(const float* samples, int num_frames);

  // Flushes the content of the buffer and reset the position of the heads.
  void Reset();

  // Returns the number of frames we can still write.
  int overhead() const;

  // Returns the number of frames we can read for a given reader.
  // @param reader reading head index.
  int available(int reader) const;

  // Returns a pointer to num_frames x num_channels contiguous samples for
  // a given reader. In most cases this directly returns a pointer to the
  // data in the ring buffer. However, if the required block wraps around the
  // boundaries of the ring buffer, the data is copied to a temporary buffer
  // owned by the RingBuffer object.
  // @param reader reading head index.
  // @param num_frames number of frames to read.
  // @returns pointer to a continuous buffer containing num_frames.
  float* GetPointer(int reader, int num_frames);

  // Merges updated data back into the ring buffer, if it was updated in
  // the temporary buffer.  This operation follows a GetPointer() that
  // was used to obtain data to rewrite.  The buffer address supplied
  // here must match the one returned by GetPointer().
  // @param reader reading head index.
  // @param source pointer to a continuous buffer containing num_frames.
  // @param num_frames number of frames to copy back to the ring buffer.
  void MergeBack(int reader, const float* source, int num_frames);

 private:
  // Returns the position of the laziest reader.
  int64 GetTail() const;

  bool initialized_;
  float* samples_;
  std::vector<int64> readers_;
  int size_;
  int num_channels_;
  int num_readers_;
  int64 head_logical_;
  int head_;

  float* temp_read_buffer_;
  int temp_read_buffer_size_;

  DISALLOW_COPY_AND_ASSIGN(RingBuffer);
};

}  // namespace video_editing

#endif  // FRAMEWORKS_EX_VARIABLESPEED_JNI_RING_BUFFER_H_
