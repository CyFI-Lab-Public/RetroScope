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

#ifndef PARSER_H_

#define PARSER_H_

#include <media/stagefright/foundation/AHandler.h>
#include <media/stagefright/DataSource.h>
#include <utils/Vector.h>

namespace android {

struct ABuffer;

struct FragmentedMP4Parser : public AHandler {
    struct Source : public RefBase {
        Source() {}

        virtual ssize_t readAt(off64_t offset, void *data, size_t size) = 0;
        virtual bool isSeekable() = 0;

        protected:
        virtual ~Source() {}

        private:
        DISALLOW_EVIL_CONSTRUCTORS(Source);
    };

    FragmentedMP4Parser();

    void start(const char *filename);
    void start(const sp<Source> &source);
    void start(sp<DataSource> &source);

    sp<AMessage> getFormat(bool audio, bool synchronous = false);
    status_t dequeueAccessUnit(bool audio, sp<ABuffer> *accessUnit, bool synchronous = false);
    status_t seekTo(bool audio, int64_t timeUs);
    bool isSeekable() const;

    virtual void onMessageReceived(const sp<AMessage> &msg);

protected:
    virtual ~FragmentedMP4Parser();

private:
    enum {
        kWhatStart,
        kWhatProceed,
        kWhatReadMore,
        kWhatGetFormat,
        kWhatDequeueAccessUnit,
        kWhatSeekTo,
    };

    struct TrackFragment;
    struct DynamicTrackFragment;
    struct StaticTrackFragment;

    struct DispatchEntry {
        uint32_t mType;
        uint32_t mParentType;
        status_t (FragmentedMP4Parser::*mHandler)(uint32_t, size_t, uint64_t);
    };

    struct Container {
        uint64_t mOffset;
        uint64_t mBytesRemaining;
        uint32_t mType;
        bool mExtendsToEOF;
    };

    struct SampleDescription {
        uint32_t mType;
        uint16_t mDataRefIndex;

        sp<AMessage> mFormat;
    };

    struct SampleInfo {
        off64_t mOffset;
        size_t mSize;
        uint32_t mPresentationTime;
        size_t mSampleDescIndex;
        uint32_t mFlags;
    };

    struct MediaDataInfo {
        sp<ABuffer> mBuffer;
        off64_t mOffset;
    };

    struct SidxEntry {
        size_t mSize;
        uint32_t mDurationUs;
    };

    struct TrackInfo {
        enum Flags {
            kTrackEnabled     = 0x01,
            kTrackInMovie     = 0x02,
            kTrackInPreview   = 0x04,
        };

        uint32_t mTrackID;
        uint32_t mFlags;
        uint32_t mDuration;  // This is the duration in terms of movie timescale!
        uint64_t mSidxDuration; // usec, from sidx box, which can use a different timescale

        uint32_t mMediaTimeScale;

        uint32_t mMediaHandlerType;
        Vector<SampleDescription> mSampleDescs;

        // from track extends:
        uint32_t mDefaultSampleDescriptionIndex;
        uint32_t mDefaultSampleDuration;
        uint32_t mDefaultSampleSize;
        uint32_t mDefaultSampleFlags;

        uint32_t mDecodingTime;

        Vector<SidxEntry> mSidx;
        sp<StaticTrackFragment> mStaticFragment;
        List<sp<TrackFragment> > mFragments;
    };

    struct TrackFragmentHeaderInfo {
        enum Flags {
            kBaseDataOffsetPresent         = 0x01,
            kSampleDescriptionIndexPresent = 0x02,
            kDefaultSampleDurationPresent  = 0x08,
            kDefaultSampleSizePresent      = 0x10,
            kDefaultSampleFlagsPresent     = 0x20,
            kDurationIsEmpty               = 0x10000,
        };

        uint32_t mTrackID;
        uint32_t mFlags;
        uint64_t mBaseDataOffset;
        uint32_t mSampleDescriptionIndex;
        uint32_t mDefaultSampleDuration;
        uint32_t mDefaultSampleSize;
        uint32_t mDefaultSampleFlags;

        uint64_t mDataOffset;
    };

    static const DispatchEntry kDispatchTable[];

    sp<Source> mSource;
    off_t mBufferPos;
    bool mSuspended;
    bool mDoneWithMoov;
    off_t mFirstMoofOffset; // used as the starting point for offsets calculated from the sidx box
    sp<ABuffer> mBuffer;
    Vector<Container> mStack;
    KeyedVector<uint32_t, TrackInfo> mTracks;  // TrackInfo by trackID
    Vector<MediaDataInfo> mMediaData;

    uint32_t mCurrentTrackID;

    status_t mFinalResult;

    TrackFragmentHeaderInfo mTrackFragmentHeaderInfo;

    status_t onProceed();
    status_t onDequeueAccessUnit(size_t trackIndex, sp<ABuffer> *accessUnit);
    status_t onSeekTo(bool wantAudio, int64_t position);

    void enter(off64_t offset, uint32_t type, uint64_t size);

    uint16_t readU16(size_t offset);
    uint32_t readU32(size_t offset);
    uint64_t readU64(size_t offset);
    void skip(off_t distance);
    status_t need(size_t size);
    bool fitsContainer(uint64_t size) const;

    status_t parseTrackHeader(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseMediaHeader(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseMediaHandler(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseTrackExtends(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseTrackFragmentHeader(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseTrackFragmentRun(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseVisualSampleEntry(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseAudioSampleEntry(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseSampleSizes(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseCompactSampleSizes(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseSampleToChunk(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseChunkOffsets(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseChunkOffsets64(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseAVCCodecSpecificData(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseESDSCodecSpecificData(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseMediaData(
            uint32_t type, size_t offset, uint64_t size);

    status_t parseSegmentIndex(
            uint32_t type, size_t offset, uint64_t size);

    TrackInfo *editTrack(uint32_t trackID, bool createIfNecessary = false);

    ssize_t findTrack(bool wantAudio) const;

    status_t makeAccessUnit(
            TrackInfo *info,
            const SampleInfo &sample,
            const MediaDataInfo &mdatInfo,
            sp<ABuffer> *accessUnit);

    status_t getSample(
            TrackInfo *info,
            sp<TrackFragment> *fragment,
            SampleInfo *sampleInfo);

    static int CompareSampleLocation(
        const SampleInfo &sample, const MediaDataInfo &mdatInfo);

    void resumeIfNecessary();

    void copyBuffer(
            sp<ABuffer> *dst,
            size_t offset, uint64_t size) const;

    DISALLOW_EVIL_CONSTRUCTORS(FragmentedMP4Parser);
};

}  // namespace android

#endif  // PARSER_H_

