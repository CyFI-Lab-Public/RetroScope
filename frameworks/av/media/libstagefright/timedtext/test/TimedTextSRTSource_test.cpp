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

#define LOG_TAG "TimedTextSRTSource_test"
#include <utils/Log.h>

#include <gtest/gtest.h>

#include <binder/Parcel.h>
#include <media/stagefright/foundation/AString.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/MediaErrors.h>
#include <utils/misc.h>

#include <TimedTextSource.h>
#include <TimedTextSRTSource.h>

namespace android {
namespace test {

static const int kSecToUsec = 1000000;
static const int kSecToMsec = 1000;
static const int kMsecToUsec = 1000;

/* SRT format (http://en.wikipedia.org/wiki/SubRip)
 *   Subtitle number
 *   Start time --> End time
 *   Text of subtitle (one or more lines)
 *   Blank lines
 */
static const char *kSRTString =
    "1\n00:00:1,000 --> 00:00:1,500\n1\n\n"
    "2\n00:00:2,000 --> 00:00:2,500\n2\n\n"
    "3\n00:00:3,000 --> 00:00:3,500\n3\n\n"
    "4\n00:00:4,000 --> 00:00:4,500\n4\n\n"
    "5\n00:00:5,000 --> 00:00:5,500\n5\n\n"
    // edge case : previos end time = next start time
    "6\n00:00:5,500 --> 00:00:5,800\n6\n\n"
    "7\n00:00:5,800 --> 00:00:6,000\n7\n\n"
    "8\n00:00:6,000 --> 00:00:7,000\n8\n\n";

class SRTDataSourceStub : public DataSource {
public:
    SRTDataSourceStub(const char *data, size_t size) :
        mData(data), mSize(size) {}
    virtual ~SRTDataSourceStub() {}

    virtual status_t initCheck() const {
        return OK;
    }

    virtual ssize_t readAt(off64_t offset, void *data, size_t size) {
        if (offset >= mSize) return 0;

        ssize_t avail = mSize - offset;
        if (avail > size) {
            avail = size;
        }
        memcpy(data, mData + offset, avail);
        return avail;
    }

private:
    const char *mData;
    size_t mSize;
};

class TimedTextSRTSourceTest : public testing::Test {
protected:
    void SetUp() {
        sp<DataSource> stub= new SRTDataSourceStub(
                kSRTString,
                strlen(kSRTString));
        mSource = new TimedTextSRTSource(stub);
        mSource->start();
    }

    void CheckStartTimeMs(const Parcel& parcel, int32_t timeMs) {
        int32_t intval;
        parcel.setDataPosition(8);
        parcel.readInt32(&intval);
        EXPECT_EQ(timeMs, intval);
    }

    void CheckDataEquals(const Parcel& parcel, const char* content) {
        int32_t intval;
        parcel.setDataPosition(16);
        parcel.readInt32(&intval);
        parcel.setDataPosition(24);
        const char* data = (const char*) parcel.readInplace(intval);

        int32_t content_len = strlen(content);
        EXPECT_EQ(content_len, intval);
        EXPECT_TRUE(strncmp(data, content, content_len) == 0);
    }

    sp<TimedTextSource> mSource;
    int64_t startTimeUs;
    int64_t endTimeUs;
    Parcel parcel;
    AString subtitle;
    status_t err;
};

TEST_F(TimedTextSRTSourceTest, readAll) {
    for (int i = 1; i <= 5; i++) {
        err = mSource->read(&startTimeUs, &endTimeUs, &parcel);
        EXPECT_EQ(OK, err);
        CheckStartTimeMs(parcel, i * kSecToMsec);
        subtitle = StringPrintf("%d\n\n", i);
        CheckDataEquals(parcel, subtitle.c_str());
    }
    // read edge cases
    err = mSource->read(&startTimeUs, &endTimeUs, &parcel);
    EXPECT_EQ(OK, err);
    CheckStartTimeMs(parcel, 5500);
    subtitle = StringPrintf("6\n\n");
    CheckDataEquals(parcel, subtitle.c_str());

    err = mSource->read(&startTimeUs, &endTimeUs, &parcel);
    EXPECT_EQ(OK, err);
    CheckStartTimeMs(parcel, 5800);
    subtitle = StringPrintf("7\n\n");
    CheckDataEquals(parcel, subtitle.c_str());

    err = mSource->read(&startTimeUs, &endTimeUs, &parcel);
    EXPECT_EQ(OK, err);
    CheckStartTimeMs(parcel, 6000);
    subtitle = StringPrintf("8\n\n");
    CheckDataEquals(parcel, subtitle.c_str());

    err = mSource->read(&startTimeUs, &endTimeUs, &parcel);
    EXPECT_EQ(ERROR_END_OF_STREAM, err);
}

TEST_F(TimedTextSRTSourceTest, seekTimeIsEarlierThanFirst) {
    MediaSource::ReadOptions options;
    options.setSeekTo(500, MediaSource::ReadOptions::SEEK_PREVIOUS_SYNC);
    err = mSource->read(&startTimeUs, &endTimeUs, &parcel, &options);
    EXPECT_EQ(OK, err);
    EXPECT_EQ(1 * kSecToUsec, startTimeUs);
    CheckStartTimeMs(parcel, 1 * kSecToMsec);
}

TEST_F(TimedTextSRTSourceTest, seekTimeIsLaterThanLast) {
    MediaSource::ReadOptions options;
    options.setSeekTo(7 * kSecToUsec, MediaSource::ReadOptions::SEEK_PREVIOUS_SYNC);
    err = mSource->read(&startTimeUs, &endTimeUs, &parcel, &options);
    EXPECT_EQ(ERROR_END_OF_STREAM, err);

    options.setSeekTo(8 * kSecToUsec, MediaSource::ReadOptions::SEEK_PREVIOUS_SYNC);
    err = mSource->read(&startTimeUs, &endTimeUs, &parcel, &options);
    EXPECT_EQ(ERROR_END_OF_STREAM, err);
}

TEST_F(TimedTextSRTSourceTest, seekTimeIsMatched) {
    for (int i = 1; i <= 5; i++) {
        MediaSource::ReadOptions options;
        options.setSeekTo(i * kSecToUsec, MediaSource::ReadOptions::SEEK_PREVIOUS_SYNC);
        err = mSource->read(&startTimeUs, &endTimeUs, &parcel, &options);
        EXPECT_EQ(OK, err);
        EXPECT_EQ(i * kSecToUsec, startTimeUs);

        options.setSeekTo(i * kSecToUsec + 100, MediaSource::ReadOptions::SEEK_PREVIOUS_SYNC);
        err = mSource->read(&startTimeUs, &endTimeUs, &parcel, &options);
        EXPECT_EQ(OK, err);
        EXPECT_EQ(i * kSecToUsec, startTimeUs);
    }
}

TEST_F(TimedTextSRTSourceTest, seekTimeInBetweenTwo) {
    for (int i = 1; i <= 4; i++) {
        MediaSource::ReadOptions options;
        options.setSeekTo(i * kSecToUsec + 500000, MediaSource::ReadOptions::SEEK_PREVIOUS_SYNC);
        err = mSource->read(&startTimeUs, &endTimeUs, &parcel, &options);
        EXPECT_EQ(OK, err);
        EXPECT_EQ((i + 1) * kSecToUsec, startTimeUs);

        options.setSeekTo(i * kSecToUsec + 600000, MediaSource::ReadOptions::SEEK_PREVIOUS_SYNC);
        err = mSource->read(&startTimeUs, &endTimeUs, &parcel, &options);
        EXPECT_EQ(OK, err);
        EXPECT_EQ((i + 1) * kSecToUsec, startTimeUs);
    }
}

TEST_F(TimedTextSRTSourceTest, checkEdgeCase) {
    MediaSource::ReadOptions options;
    options.setSeekTo(5500 * kMsecToUsec, MediaSource::ReadOptions::SEEK_PREVIOUS_SYNC);
    err = mSource->read(&startTimeUs, &endTimeUs, &parcel, &options);
    EXPECT_EQ(OK, err);
    EXPECT_EQ(5500 * kMsecToUsec, startTimeUs);
    subtitle = StringPrintf("6\n\n");
    CheckDataEquals(parcel, subtitle.c_str());

    options.setSeekTo(5800 * kMsecToUsec, MediaSource::ReadOptions::SEEK_PREVIOUS_SYNC);
    err = mSource->read(&startTimeUs, &endTimeUs, &parcel, &options);
    EXPECT_EQ(OK, err);
    EXPECT_EQ(5800 * kMsecToUsec, startTimeUs);
    subtitle = StringPrintf("7\n\n");
    CheckDataEquals(parcel, subtitle.c_str());

    options.setSeekTo(6000 * kMsecToUsec, MediaSource::ReadOptions::SEEK_PREVIOUS_SYNC);
    err = mSource->read(&startTimeUs, &endTimeUs, &parcel, &options);
    EXPECT_EQ(OK, err);
    EXPECT_EQ(6000 * kMsecToUsec, startTimeUs);
    subtitle = StringPrintf("8\n\n");
    CheckDataEquals(parcel, subtitle.c_str());
}

}  // namespace test
}  // namespace android
