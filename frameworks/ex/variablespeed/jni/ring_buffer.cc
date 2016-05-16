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

#include "ring_buffer.h"

#include "integral_types.h"

namespace video_editing {

void RingBuffer::Init(int size, int num_channels, int num_readers) {
  size_ = size;
  num_channels_ = num_channels;
  num_readers_ = num_readers;
  temp_read_buffer_size_ = 1024;
  initialized_ = true;
  Reset();
}

RingBuffer::RingBuffer()
    : initialized_(false), samples_(NULL),
      num_readers_(0), temp_read_buffer_(NULL) {
}

RingBuffer::~RingBuffer() {
  delete[] samples_;
  delete[] temp_read_buffer_;
}

void RingBuffer::Reset() {
  delete[] samples_;
  samples_ = new float[size_ * num_channels_];
  memset(samples_, 0,
         size_ * num_channels_ * sizeof(samples_[0]));

  temp_read_buffer_size_ = 1024;
  delete[] temp_read_buffer_;
  temp_read_buffer_ = new float[temp_read_buffer_size_ * num_channels_];
  memset(temp_read_buffer_, 0,
         temp_read_buffer_size_ * num_channels_ * sizeof(samples_[0]));
  readers_.clear();
  for (int i = 0; i < num_readers_; ++i) {
    readers_.push_back(0LL);
  }
  head_logical_ = 0LL;
  head_ = 0;
}

int RingBuffer::available(int reader) const {
  return head_logical_ - readers_[reader];
}

int RingBuffer::overhead() const {
  int64 tail = GetTail();
  return tail + size_ - head_logical_;
}

int64 RingBuffer::GetTail() const {
  return *min_element(readers_.begin(), readers_.end());
}

int64 RingBuffer::Tell(int reader) const {
  return readers_[reader];
}

void RingBuffer::Seek(int reader, int64 position) {
  readers_[reader] = position;
}

void RingBuffer::Write(const float* samples, int num_frames) {
  if (!num_frames) {
    return;
  }
  if (head_ + num_frames <= size_) {
    memcpy(samples_ + head_ * num_channels_, samples,
           num_frames * num_channels_ * sizeof(samples[0]));
    head_ += num_frames;
  } else {
    int overhead = size_ - head_;
    memcpy(samples_ + head_ * num_channels_, samples,
           num_channels_ * overhead * sizeof(samples[0]));
    head_ = num_frames - overhead;
    memcpy(samples_, samples + overhead * num_channels_,
           num_channels_ * head_ * sizeof(samples[0]));
  }
  head_logical_ += num_frames;
}

void RingBuffer::Copy(int reader, float* destination, int num_frames) const {
  int pos = Tell(reader) % size_;
  if (pos + num_frames <= size_) {
    memcpy(destination, samples_ + pos * num_channels_,
           num_channels_ * num_frames * sizeof(destination[0]));
  } else {
    int wrapped = size_ - pos;
    memcpy(destination, samples_ + pos * num_channels_,
           num_channels_ * wrapped * sizeof(destination[0]));
    int remaining = num_frames - wrapped;
    memcpy(destination + wrapped * num_channels_, samples_,
           num_channels_ * remaining * sizeof(destination[0]));
  }
}

float* RingBuffer::GetPointer(int reader, int num_frames) {
  int pos = Tell(reader) % size_;
  if (pos + num_frames <= size_) {
    return samples_ + pos * num_channels_;
  } else {
    if (num_frames > temp_read_buffer_size_) {
      temp_read_buffer_size_ = num_frames;
      delete[] temp_read_buffer_;
      temp_read_buffer_ =
          new float[temp_read_buffer_size_ * num_channels_];  // NOLINT
    }
    Copy(reader, temp_read_buffer_, num_frames);
    return temp_read_buffer_;
  }
}

void RingBuffer::MergeBack(int reader, const float* source, int num_frames) {
  // If the source pointer is not the temporary buffer,
  // data updates were performed in place, so there is nothing to do.
  // Otherwise, copy samples from the temp buffer back to the ring buffer.
  if (source == temp_read_buffer_) {
    int pos = Tell(reader) % size_;
    if (pos + num_frames <= size_) {
      memcpy(samples_ + (pos * num_channels_), source,
             num_channels_ * num_frames * sizeof(source[0]));
    } else {
      int wrapped = size_ - pos;
      memcpy(samples_ + (pos * num_channels_), source,
             num_channels_ * wrapped * sizeof(source[0]));
      int remaining = num_frames - wrapped;
      memcpy(samples_, source + (wrapped * num_channels_),
             num_channels_ * remaining * sizeof(source[0]));
    }
  }
}

}  // namespace video_editing
