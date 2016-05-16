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

#ifndef CAMERA_SCOPED_TRACE_H
#define CAMERA_SCOPED_TRACE_H

#include <stdint.h>
#include <cutils/trace.h>

// See <cutils/trace.h> for more tracing macros.

// CAMTRACE_NAME traces the beginning and end of the current scope.  To trace
// the correct start and end times this macro should be declared first in the
// scope body.
#define CAMTRACE_NAME(name) ScopedTrace ___tracer(ATRACE_TAG, name)
// CAMTRACE_CALL is an ATRACE_NAME that uses the current function name.
#define CAMTRACE_CALL() CAMTRACE_NAME(__FUNCTION__)

namespace default_camera_hal {

class ScopedTrace {
public:
inline ScopedTrace(uint64_t tag, const char* name)
    : mTag(tag) {
    atrace_begin(mTag,name);
}

inline ~ScopedTrace() {
    atrace_end(mTag);
}

private:
    uint64_t mTag;
};

}; // namespace default_camera_hal

#endif // CAMERA_SCOPED_TRACE_H
