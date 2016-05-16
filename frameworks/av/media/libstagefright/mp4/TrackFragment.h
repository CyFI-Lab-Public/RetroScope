/*
 * Copyright (C) 2012 The Android Open Source Project
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

#ifndef TRACK_FRAGMENT_H_

#define TRACK_FRAGMENT_H_

#include "include/FragmentedMP4Parser.h"

namespace android {

struct FragmentedMP4Parser::TrackFragment : public RefBase {
    TrackFragment() {}

    virtual status_t getSample(SampleInfo *info) = 0;
    virtual void advance() = 0;

    virtual status_t signalCompletion() = 0;
    virtual bool complete() const = 0;

protected:
    virtual ~TrackFragment() {}

private:
    DISALLOW_EVIL_CONSTRUCTORS(TrackFragment);
};

struct FragmentedMP4Parser::DynamicTrackFragment : public FragmentedMP4Parser::TrackFragment {
    DynamicTrackFragment();

    virtual status_t getSample(SampleInfo *info);
    virtual void advance();

    void addSample(
            off64_t dataOffset, size_t sampleSize,
            uint32_t presentationTime,
            size_t sampleDescIndex,
            uint32_t flags);

    // No more samples will be added to this fragment.
    virtual status_t signalCompletion();

    virtual bool complete() const;

protected:
    virtual ~DynamicTrackFragment();

private:
    bool mComplete;
    size_t mSampleIndex;
    Vector<SampleInfo> mSamples;

    DISALLOW_EVIL_CONSTRUCTORS(DynamicTrackFragment);
};

struct FragmentedMP4Parser::StaticTrackFragment : public FragmentedMP4Parser::TrackFragment {
    StaticTrackFragment();

    virtual status_t getSample(SampleInfo *info);
    virtual void advance();

    virtual status_t signalCompletion();
    virtual bool complete() const;

    status_t parseSampleSizes(
            FragmentedMP4Parser *parser, uint32_t type, size_t offset, uint64_t size);

    status_t parseCompactSampleSizes(
            FragmentedMP4Parser *parser, uint32_t type, size_t offset, uint64_t size);

    status_t parseSampleToChunk(
            FragmentedMP4Parser *parser, uint32_t type, size_t offset, uint64_t size);

    status_t parseChunkOffsets(
            FragmentedMP4Parser *parser, uint32_t type, size_t offset, uint64_t size);

    status_t parseChunkOffsets64(
            FragmentedMP4Parser *parser, uint32_t type, size_t offset, uint64_t size);

protected:
    virtual ~StaticTrackFragment();

private:
    size_t mSampleIndex;
    size_t mSampleCount;
    uint32_t mChunkIndex;

    SampleInfo mSampleInfo;

    sp<ABuffer> mSampleSizes;
    sp<ABuffer> mCompactSampleSizes;

    sp<ABuffer> mSampleToChunk;
    ssize_t mSampleToChunkIndex;
    size_t mSampleToChunkRemaining;

    sp<ABuffer> mChunkOffsets;
    sp<ABuffer> mChunkOffsets64;
    uint32_t mPrevChunkIndex;
    uint64_t mNextSampleOffset;

    void updateSampleInfo();

    DISALLOW_EVIL_CONSTRUCTORS(StaticTrackFragment);
};

}  // namespace android

#endif  // TRACK_FRAGMENT_H_
