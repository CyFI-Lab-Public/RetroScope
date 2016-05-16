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

#ifndef ANDROID_RS_CPP_UTILS_H
#define ANDROID_RS_CPP_UTILS_H

#if !defined(RS_SERVER) && !defined(RS_COMPATIBILITY_LIB)
#include <utils/Log.h>
#include <utils/String8.h>
#include <utils/Vector.h>
#include <cutils/atomic.h>
#endif

#include <stdint.h>

#include <stdlib.h>
#include <pthread.h>
#include <time.h>

#include <math.h>

#ifdef RS_COMPATIBILITY_LIB
#include <android/log.h>
#endif

#if defined(RS_SERVER) || defined(RS_COMPATIBILITY_LIB)

#define ATRACE_TAG
#define ATRACE_CALL(...)

#include <string>
#include <vector>
#include <algorithm>

#define ALOGE(...) \
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);
#define ALOGW(...) \
    __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__);
#define ALOGD(...) \
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__);
#define ALOGV(...) \
    __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__);

namespace android {

    // server has no Vector or String8 classes; implement on top of STL
    class String8: public std::string {
    public:
    String8(const char *ptr) : std::string(ptr) {

        }
    String8(const char *ptr, size_t len) : std::string(ptr, len) {

        }
    String8() : std::string() {

        }

        const char* string() const {
            return this->c_str();
        }

        void setTo(const char* str, ssize_t len) {
            this->assign(str, len);
        }
        void setTo(const char* str) {
            this->assign(str);
        }
        String8 getPathDir(void) const {
            const char* cp;
            const char*const str = this->c_str();

            cp = strrchr(str, OS_PATH_SEPARATOR);
            if (cp == NULL)
                return String8("");
            else
                return String8(str, cp - str);
        }
    };

    template <class T> class Vector: public std::vector<T> {
    public:
        void push(T obj) {
            this->push_back(obj);
        }
        void removeAt(uint32_t index) {
            this->erase(this->begin() + index);
        }
        ssize_t add(const T& obj) {
            this->push_back(obj);
            return this->size() - 1;
        }
        void setCapacity(ssize_t capacity) {
            this->resize(capacity);
        }

        T* editArray() {
            return (T*)(this->begin());
        }

        const T* array() {
            return (const T*)(this->begin());
        }

    };

    template<> class Vector<bool>: public std::vector<char> {
    public:
        void push(bool obj) {
            this->push_back(obj);
        }
        void removeAt(uint32_t index) {
            this->erase(this->begin() + index);
        }
        ssize_t add(const bool& obj) {
            this->push_back(obj);
            return this->size() - 1;
        }
        void setCapacity(ssize_t capacity) {
            this->resize(capacity);
        }

        bool* editArray() {
            return (bool*)(this->begin());
        }

        const bool* array() {
            return (const bool*)(this->begin());
        }
    };

}

typedef int64_t nsecs_t;  // nano-seconds

enum {
    SYSTEM_TIME_REALTIME = 0,  // system-wide realtime clock
    SYSTEM_TIME_MONOTONIC = 1, // monotonic time since unspecified starting point
    SYSTEM_TIME_PROCESS = 2,   // high-resolution per-process clock
    SYSTEM_TIME_THREAD = 3,    // high-resolution per-thread clock
};

static inline nsecs_t systemTime(int clock)
{
#if defined(HAVE_POSIX_CLOCKS)
    static const clockid_t clocks[] = {
            CLOCK_REALTIME,
            CLOCK_MONOTONIC,
            CLOCK_PROCESS_CPUTIME_ID,
            CLOCK_THREAD_CPUTIME_ID
    };
    struct timespec t;
    t.tv_sec = t.tv_nsec = 0;
    clock_gettime(clocks[clock], &t);
    return nsecs_t(t.tv_sec)*1000000000LL + t.tv_nsec;
#else
    // we don't support the clocks here.
    struct timeval t;
    t.tv_sec = t.tv_usec = 0;
    gettimeofday(&t, NULL);
    return nsecs_t(t.tv_sec)*1000000000LL + nsecs_t(t.tv_usec)*1000LL;
#endif
}

static inline nsecs_t nanoseconds_to_milliseconds(nsecs_t secs)
{
    return secs/1000000;
}


#endif // RS_SERVER || RS_COMPATIBILITY_LIB

namespace android {
namespace renderscript {

const char * rsuCopyString(const char *name);
const char * rsuCopyString(const char *name, size_t len);

#if 1
#define rsAssert(v) do {if(!(v)) ALOGE("rsAssert failed: %s, in %s at %i", #v, __FILE__, __LINE__);} while (0)
#else
#define rsAssert(v) while (0)
#endif

template<typename T>
T rsMin(T in1, T in2)
{
    if (in1 > in2) {
        return in2;
    }
    return in1;
}

template<typename T>
T rsMax(T in1, T in2) {
    if (in1 < in2) {
        return in2;
    }
    return in1;
}

template<typename T>
T rsFindHighBit(T val) {
    uint32_t bit = 0;
    while (val > 1) {
        bit++;
        val>>=1;
    }
    return bit;
}

template<typename T>
bool rsIsPow2(T val) {
    return (val & (val-1)) == 0;
}

template<typename T>
T rsHigherPow2(T v) {
    if (rsIsPow2(v)) {
        return v;
    }
    return 1 << (rsFindHighBit(v) + 1);
}

template<typename T>
T rsLowerPow2(T v) {
    if (rsIsPow2(v)) {
        return v;
    }
    return 1 << rsFindHighBit(v);
}

template<typename T>
T rsRound(T v, unsigned int r) {
    // Only valid for rounding up to powers of 2.
    if ((r & (r - 1)) != 0) {
        rsAssert(false && "Must be power of 2 for rounding up");
        return v;
    }
    T res = v + (r - 1);
    if (res < v) {
        rsAssert(false && "Overflow of rounding operation");
        return v;
    }
    res &= ~(r - 1);
    return res;
}

static inline uint16_t rs888to565(uint32_t r, uint32_t g, uint32_t b) {
    uint16_t t = 0;
    t |= b >> 3;
    t |= (g >> 2) << 5;
    t |= (r >> 3) << 11;
    return t;
}

static inline uint16_t rsBoxFilter565(uint16_t i1, uint16_t i2, uint16_t i3, uint16_t i4) {
    uint32_t r = ((i1 & 0x1f) + (i2 & 0x1f) + (i3 & 0x1f) + (i4 & 0x1f));
    uint32_t g = ((i1 >> 5) & 0x3f) + ((i2 >> 5) & 0x3f) + ((i3 >> 5) & 0x3f) + ((i4 >> 5) & 0x3f);
    uint32_t b = ((i1 >> 11) + (i2 >> 11) + (i3 >> 11) + (i4 >> 11));
    return (r >> 2) | ((g >> 2) << 5) | ((b >> 2) << 11);
}

static inline uint32_t rsBoxFilter8888(uint32_t i1, uint32_t i2, uint32_t i3, uint32_t i4) {
    uint32_t r = (i1 & 0xff) +         (i2 & 0xff) +         (i3 & 0xff) +         (i4 & 0xff);
    uint32_t g = ((i1 >> 8) & 0xff) +  ((i2 >> 8) & 0xff) +  ((i3 >> 8) & 0xff) +  ((i4 >> 8) & 0xff);
    uint32_t b = ((i1 >> 16) & 0xff) + ((i2 >> 16) & 0xff) + ((i3 >> 16) & 0xff) + ((i4 >> 16) & 0xff);
    uint32_t a = ((i1 >> 24) & 0xff) + ((i2 >> 24) & 0xff) + ((i3 >> 24) & 0xff) + ((i4 >> 24) & 0xff);
    return (r >> 2) | ((g >> 2) << 8) | ((b >> 2) << 16) | ((a >> 2) << 24);
}

}
}

#endif //ANDROID_RS_OBJECT_BASE_H


