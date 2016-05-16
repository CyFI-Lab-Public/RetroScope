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

#include <decode_buffer.h>

namespace {

static const size_t kNumberOfBytesPerSample = 2;

}  // namespace

DecodeBuffer::DecodeBuffer(size_t sizeOfOneBuffer, size_t maxSize)
    : sizeOfOneBuffer_(sizeOfOneBuffer), maxSize_(maxSize),
      start_(0), end_(0), advancedCount_(0), data_() {
  Clear();
}

DecodeBuffer::~DecodeBuffer() {
  Clear();
}

size_t DecodeBuffer::GetSizeInBytes() const {
  return kNumberOfBytesPerSample * (end_ - start_);
}

bool DecodeBuffer::IsTooLarge() const {
  return GetSizeInBytes() > maxSize_;
}

void DecodeBuffer::AddData(int8_t* pointer, size_t lengthInBytes) {
  for (size_t i = 0; i < lengthInBytes / kNumberOfBytesPerSample; ++i) {
    PushValue(reinterpret_cast<int16*>(pointer)[i]);
  }
}

void DecodeBuffer::Clear() {
  while (data_.size() > 0) {
    delete[] data_.front();
    data_.erase(data_.begin());
  }
  start_ = 0;
  end_ = 0;
  advancedCount_ = 0;
}

size_t DecodeBuffer::GetTotalAdvancedCount() const {
  return advancedCount_;
}

void DecodeBuffer::AdvanceHeadPointerShorts(size_t numberOfShorts) {
  start_ += numberOfShorts;
  while (start_ > sizeOfOneBuffer_) {
    data_.push_back(data_.front());
    data_.erase(data_.begin());
    start_ -= sizeOfOneBuffer_;
    end_ -= sizeOfOneBuffer_;
  }
  advancedCount_ += numberOfShorts;
}

void DecodeBuffer::PushValue(int16 value) {
  size_t bufferIndex = end_ / sizeOfOneBuffer_;
  if (bufferIndex >= data_.size()) {
    data_.push_back(new int16[sizeOfOneBuffer_]);
  }
  data_.at(bufferIndex)[end_ % sizeOfOneBuffer_] = value;
  ++end_;
}

int16 DecodeBuffer::GetAtIndex(size_t index) {
  return data_.at((start_ + index) / sizeOfOneBuffer_)
      [(start_ + index) % sizeOfOneBuffer_];
}
