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

#ifndef JERR_HOOK_H_
#define JERR_HOOK_H_

extern "C" {
#include "jinclude.h"
#include "jpeglib.h"
#include "jerror.h"
}

#include <setjmp.h>

/**
 * ErrManager replaces libjpeg's default error handling with
 * the following behavior:
 * - libjpeg function calls return to the position set by
 *   setjmp for error cleanup.
 * - libjpeg error and warning messages are printed to
 *   logcat's error output.
 */
typedef struct {
    struct jpeg_error_mgr mgr;
    jmp_buf setjmp_buf;
} ErrManager;

void SetupErrMgr(j_common_ptr cinfo, ErrManager* errMgr);

#endif // JERR_HOOK_H_
