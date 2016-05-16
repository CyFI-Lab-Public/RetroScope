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


#include "jerr_hook.h"
#include "jni_defines.h"

/**
 * Replaces libjpeg's error_exit function, returns control to
 * the point
 */
void ErrExit(j_common_ptr cinfo) {
    ErrManager* mgr = reinterpret_cast<ErrManager*>(cinfo->err);
    (*cinfo->err->output_message) (cinfo);
    // Returns control to error handling in jpeg_writer
    longjmp(mgr->setjmp_buf, 1);
}

/**
 * Replaces libjpeg's output_message function, writes message
 * to logcat's error log.
 */
void ErrOutput(j_common_ptr cinfo) {
    ErrManager* mgr = reinterpret_cast<ErrManager*>(cinfo->err);
    char buf[JMSG_LENGTH_MAX];
    (*cinfo->err->format_message) (cinfo, buf);
    buf[JMSG_LENGTH_MAX - 1] = '\0';  // Force null terminator
    // Output error message in ndk logcat.
    LOGE("%s\n", buf);
}

void SetupErrMgr(j_common_ptr cinfo, ErrManager* errMgr) {
    jpeg_std_error(&(errMgr->mgr));
    errMgr->mgr.error_exit = ErrExit;
    errMgr->mgr.output_message = ErrOutput;
    cinfo->err = reinterpret_cast<struct jpeg_error_mgr*>(errMgr);
}


