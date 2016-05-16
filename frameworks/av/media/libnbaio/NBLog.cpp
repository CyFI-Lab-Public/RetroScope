/*
 * Copyright (C) 2013 The Android Open Source Project
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

#define LOG_TAG "NBLog"
//#define LOG_NDEBUG 0

#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <new>
#include <cutils/atomic.h>
#include <media/nbaio/NBLog.h>
#include <utils/Log.h>

namespace android {

int NBLog::Entry::readAt(size_t offset) const
{
    // FIXME This is too slow, despite the name it is used during writing
    if (offset == 0)
        return mEvent;
    else if (offset == 1)
        return mLength;
    else if (offset < (size_t) (mLength + 2))
        return ((char *) mData)[offset - 2];
    else if (offset == (size_t) (mLength + 2))
        return mLength;
    else
        return 0;
}

// ---------------------------------------------------------------------------

#if 0   // FIXME see note in NBLog.h
NBLog::Timeline::Timeline(size_t size, void *shared)
    : mSize(roundup(size)), mOwn(shared == NULL),
      mShared((Shared *) (mOwn ? new char[sharedSize(size)] : shared))
{
    new (mShared) Shared;
}

NBLog::Timeline::~Timeline()
{
    mShared->~Shared();
    if (mOwn) {
        delete[] (char *) mShared;
    }
}
#endif

/*static*/
size_t NBLog::Timeline::sharedSize(size_t size)
{
    return sizeof(Shared) + roundup(size);
}

// ---------------------------------------------------------------------------

NBLog::Writer::Writer()
    : mSize(0), mShared(NULL), mRear(0), mEnabled(false)
{
}

NBLog::Writer::Writer(size_t size, void *shared)
    : mSize(roundup(size)), mShared((Shared *) shared), mRear(0), mEnabled(mShared != NULL)
{
}

NBLog::Writer::Writer(size_t size, const sp<IMemory>& iMemory)
    : mSize(roundup(size)), mShared(iMemory != 0 ? (Shared *) iMemory->pointer() : NULL),
      mIMemory(iMemory), mRear(0), mEnabled(mShared != NULL)
{
}

void NBLog::Writer::log(const char *string)
{
    if (!mEnabled) {
        return;
    }
    size_t length = strlen(string);
    if (length > 255) {
        length = 255;
    }
    log(EVENT_STRING, string, length);
}

void NBLog::Writer::logf(const char *fmt, ...)
{
    if (!mEnabled) {
        return;
    }
    va_list ap;
    va_start(ap, fmt);
    Writer::logvf(fmt, ap);     // the Writer:: is needed to avoid virtual dispatch for LockedWriter
    va_end(ap);
}

void NBLog::Writer::logvf(const char *fmt, va_list ap)
{
    if (!mEnabled) {
        return;
    }
    char buffer[256];
    int length = vsnprintf(buffer, sizeof(buffer), fmt, ap);
    if (length >= (int) sizeof(buffer)) {
        length = sizeof(buffer) - 1;
        // NUL termination is not required
        // buffer[length] = '\0';
    }
    if (length >= 0) {
        log(EVENT_STRING, buffer, length);
    }
}

void NBLog::Writer::logTimestamp()
{
    if (!mEnabled) {
        return;
    }
    struct timespec ts;
    if (!clock_gettime(CLOCK_MONOTONIC, &ts)) {
        log(EVENT_TIMESTAMP, &ts, sizeof(struct timespec));
    }
}

void NBLog::Writer::logTimestamp(const struct timespec& ts)
{
    if (!mEnabled) {
        return;
    }
    log(EVENT_TIMESTAMP, &ts, sizeof(struct timespec));
}

void NBLog::Writer::log(Event event, const void *data, size_t length)
{
    if (!mEnabled) {
        return;
    }
    if (data == NULL || length > 255) {
        return;
    }
    switch (event) {
    case EVENT_STRING:
    case EVENT_TIMESTAMP:
        break;
    case EVENT_RESERVED:
    default:
        return;
    }
    Entry entry(event, data, length);
    log(&entry, true /*trusted*/);
}

void NBLog::Writer::log(const NBLog::Entry *entry, bool trusted)
{
    if (!mEnabled) {
        return;
    }
    if (!trusted) {
        log(entry->mEvent, entry->mData, entry->mLength);
        return;
    }
    size_t rear = mRear & (mSize - 1);
    size_t written = mSize - rear;      // written = number of bytes that have been written so far
    size_t need = entry->mLength + 3;   // mEvent, mLength, data[length], mLength
                                        // need = number of bytes remaining to write
    if (written > need) {
        written = need;
    }
    size_t i;
    // FIXME optimize this using memcpy for the data part of the Entry.
    // The Entry could have a method copyTo(ptr, offset, size) to optimize the copy.
    for (i = 0; i < written; ++i) {
        mShared->mBuffer[rear + i] = entry->readAt(i);
    }
    if (rear + written == mSize && (need -= written) > 0)  {
        for (i = 0; i < need; ++i) {
            mShared->mBuffer[i] = entry->readAt(written + i);
        }
        written += need;
    }
    android_atomic_release_store(mRear += written, &mShared->mRear);
}

bool NBLog::Writer::isEnabled() const
{
    return mEnabled;
}

bool NBLog::Writer::setEnabled(bool enabled)
{
    bool old = mEnabled;
    mEnabled = enabled && mShared != NULL;
    return old;
}

// ---------------------------------------------------------------------------

NBLog::LockedWriter::LockedWriter()
    : Writer()
{
}

NBLog::LockedWriter::LockedWriter(size_t size, void *shared)
    : Writer(size, shared)
{
}

void NBLog::LockedWriter::log(const char *string)
{
    Mutex::Autolock _l(mLock);
    Writer::log(string);
}

void NBLog::LockedWriter::logf(const char *fmt, ...)
{
    // FIXME should not take the lock until after formatting is done
    Mutex::Autolock _l(mLock);
    va_list ap;
    va_start(ap, fmt);
    Writer::logvf(fmt, ap);
    va_end(ap);
}

void NBLog::LockedWriter::logvf(const char *fmt, va_list ap)
{
    // FIXME should not take the lock until after formatting is done
    Mutex::Autolock _l(mLock);
    Writer::logvf(fmt, ap);
}

void NBLog::LockedWriter::logTimestamp()
{
    // FIXME should not take the lock until after the clock_gettime() syscall
    Mutex::Autolock _l(mLock);
    Writer::logTimestamp();
}

void NBLog::LockedWriter::logTimestamp(const struct timespec& ts)
{
    Mutex::Autolock _l(mLock);
    Writer::logTimestamp(ts);
}

bool NBLog::LockedWriter::isEnabled() const
{
    Mutex::Autolock _l(mLock);
    return Writer::isEnabled();
}

bool NBLog::LockedWriter::setEnabled(bool enabled)
{
    Mutex::Autolock _l(mLock);
    return Writer::setEnabled(enabled);
}

// ---------------------------------------------------------------------------

NBLog::Reader::Reader(size_t size, const void *shared)
    : mSize(roundup(size)), mShared((const Shared *) shared), mFront(0)
{
}

NBLog::Reader::Reader(size_t size, const sp<IMemory>& iMemory)
    : mSize(roundup(size)), mShared(iMemory != 0 ? (const Shared *) iMemory->pointer() : NULL),
      mIMemory(iMemory), mFront(0)
{
}

void NBLog::Reader::dump(int fd, size_t indent)
{
    int32_t rear = android_atomic_acquire_load(&mShared->mRear);
    size_t avail = rear - mFront;
    if (avail == 0) {
        return;
    }
    size_t lost = 0;
    if (avail > mSize) {
        lost = avail - mSize;
        mFront += lost;
        avail = mSize;
    }
    size_t remaining = avail;       // remaining = number of bytes left to read
    size_t front = mFront & (mSize - 1);
    size_t read = mSize - front;    // read = number of bytes that have been read so far
    if (read > remaining) {
        read = remaining;
    }
    // make a copy to avoid race condition with writer
    uint8_t *copy = new uint8_t[avail];
    // copy first part of circular buffer up until the wraparound point
    memcpy(copy, &mShared->mBuffer[front], read);
    if (front + read == mSize) {
        if ((remaining -= read) > 0) {
            // copy second part of circular buffer starting at beginning
            memcpy(&copy[read], mShared->mBuffer, remaining);
            read += remaining;
            // remaining = 0 but not necessary
        }
    }
    mFront += read;
    size_t i = avail;
    Event event;
    size_t length;
    struct timespec ts;
    time_t maxSec = -1;
    while (i >= 3) {
        length = copy[i - 1];
        if (length + 3 > i || copy[i - length - 2] != length) {
            break;
        }
        event = (Event) copy[i - length - 3];
        if (event == EVENT_TIMESTAMP) {
            if (length != sizeof(struct timespec)) {
                // corrupt
                break;
            }
            memcpy(&ts, &copy[i - length - 1], sizeof(struct timespec));
            if (ts.tv_sec > maxSec) {
                maxSec = ts.tv_sec;
            }
        }
        i -= length + 3;
    }
    if (i > 0) {
        lost += i;
        if (fd >= 0) {
            fdprintf(fd, "%*swarning: lost %u bytes worth of events\n", indent, "", lost);
        } else {
            ALOGI("%*swarning: lost %u bytes worth of events\n", indent, "", lost);
        }
    }
    size_t width = 1;
    while (maxSec >= 10) {
        ++width;
        maxSec /= 10;
    }
    char prefix[32];
    if (maxSec >= 0) {
        snprintf(prefix, sizeof(prefix), "[%*s] ", width + 4, "");
    } else {
        prefix[0] = '\0';
    }
    while (i < avail) {
        event = (Event) copy[i];
        length = copy[i + 1];
        const void *data = &copy[i + 2];
        size_t advance = length + 3;
        switch (event) {
        case EVENT_STRING:
            if (fd >= 0) {
                fdprintf(fd, "%*s%s%.*s\n", indent, "", prefix, length, (const char *) data);
            } else {
                ALOGI("%*s%s%.*s", indent, "", prefix, length, (const char *) data);
            } break;
        case EVENT_TIMESTAMP: {
            // already checked that length == sizeof(struct timespec);
            memcpy(&ts, data, sizeof(struct timespec));
            long prevNsec = ts.tv_nsec;
            long deltaMin = LONG_MAX;
            long deltaMax = -1;
            long deltaTotal = 0;
            size_t j = i;
            for (;;) {
                j += sizeof(struct timespec) + 3;
                if (j >= avail || (Event) copy[j] != EVENT_TIMESTAMP) {
                    break;
                }
                struct timespec tsNext;
                memcpy(&tsNext, &copy[j + 2], sizeof(struct timespec));
                if (tsNext.tv_sec != ts.tv_sec) {
                    break;
                }
                long delta = tsNext.tv_nsec - prevNsec;
                if (delta < 0) {
                    break;
                }
                if (delta < deltaMin) {
                    deltaMin = delta;
                }
                if (delta > deltaMax) {
                    deltaMax = delta;
                }
                deltaTotal += delta;
                prevNsec = tsNext.tv_nsec;
            }
            size_t n = (j - i) / (sizeof(struct timespec) + 3);
            if (n >= kSquashTimestamp) {
                if (fd >= 0) {
                    fdprintf(fd, "%*s[%d.%03d to .%.03d by .%.03d to .%.03d]\n", indent, "",
                            (int) ts.tv_sec, (int) (ts.tv_nsec / 1000000),
                            (int) ((ts.tv_nsec + deltaTotal) / 1000000),
                            (int) (deltaMin / 1000000), (int) (deltaMax / 1000000));
                } else {
                    ALOGI("%*s[%d.%03d to .%.03d by .%.03d to .%.03d]\n", indent, "",
                            (int) ts.tv_sec, (int) (ts.tv_nsec / 1000000),
                            (int) ((ts.tv_nsec + deltaTotal) / 1000000),
                            (int) (deltaMin / 1000000), (int) (deltaMax / 1000000));
                }
                i = j;
                advance = 0;
                break;
            }
            if (fd >= 0) {
                fdprintf(fd, "%*s[%d.%03d]\n", indent, "", (int) ts.tv_sec,
                        (int) (ts.tv_nsec / 1000000));
            } else {
                ALOGI("%*s[%d.%03d]", indent, "", (int) ts.tv_sec,
                        (int) (ts.tv_nsec / 1000000));
            }
            } break;
        case EVENT_RESERVED:
        default:
            if (fd >= 0) {
                fdprintf(fd, "%*s%swarning: unknown event %d\n", indent, "", prefix, event);
            } else {
                ALOGI("%*s%swarning: unknown event %d", indent, "", prefix, event);
            }
            break;
        }
        i += advance;
    }
    // FIXME it would be more efficient to put a char mCopy[256] as a member variable of the dumper
    delete[] copy;
}

bool NBLog::Reader::isIMemory(const sp<IMemory>& iMemory) const
{
    return iMemory.get() == mIMemory.get();
}

}   // namespace android
