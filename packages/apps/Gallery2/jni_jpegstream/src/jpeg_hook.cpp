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

#include "error_codes.h"
#include "jni_defines.h"
#include "jpeg_hook.h"

#include <stddef.h>
#include <string.h>

void Mgr_init_destination_fcn(j_compress_ptr cinfo) {
    DestManager *dst = reinterpret_cast<DestManager*>(cinfo->dest);
    dst->mgr.next_output_byte = reinterpret_cast<JOCTET*>(dst->outStream->getBufferPtr());
    dst->mgr.free_in_buffer = dst->outStream->getBufferSize();
}

boolean Mgr_empty_output_buffer_fcn(j_compress_ptr cinfo) {
    DestManager *dst = reinterpret_cast<DestManager*>(cinfo->dest);
    int32_t len = dst->outStream->getBufferSize();
    if (dst->outStream->write(len, 0) != J_SUCCESS) {
        ERREXIT(cinfo, JERR_FILE_WRITE);
    }
    dst->mgr.next_output_byte = reinterpret_cast<JOCTET*>(dst->outStream->getBufferPtr());
    dst->mgr.free_in_buffer = len;
    return TRUE;
}

void Mgr_term_destination_fcn(j_compress_ptr cinfo) {
    DestManager *dst = reinterpret_cast<DestManager*>(cinfo->dest);
    int32_t remaining = dst->outStream->getBufferSize() - dst->mgr.free_in_buffer;
    if (dst->outStream->write(remaining, 0) != J_SUCCESS) {
        ERREXIT(cinfo, JERR_FILE_WRITE);
    }
}

int32_t MakeDst(j_compress_ptr cinfo, JNIEnv *env, jobject outStream) {
    if (cinfo->dest != NULL) {
        LOGE("DestManager already exists, cannot allocate!");
        return J_ERROR_FATAL;
    } else {
        size_t size = sizeof(DestManager);
        cinfo->dest = (struct jpeg_destination_mgr *) (*cinfo->mem->alloc_small)
                ((j_common_ptr) cinfo, JPOOL_PERMANENT, size);
        if (cinfo->dest == NULL) {
            LOGE("Could not allocate memory for DestManager.");
            return J_ERROR_FATAL;
        }
        memset(cinfo->dest, '0', size);
    }
    DestManager *d = reinterpret_cast<DestManager*>(cinfo->dest);
    d->mgr.init_destination = Mgr_init_destination_fcn;
    d->mgr.empty_output_buffer = Mgr_empty_output_buffer_fcn;
    d->mgr.term_destination = Mgr_term_destination_fcn;
    d->outStream = new OutputStreamWrapper();
    if(d->outStream->init(env, outStream)) {
        return J_SUCCESS;
    }
    return J_ERROR_FATAL;
}

void UpdateDstEnv(j_compress_ptr cinfo, JNIEnv* env) {
    DestManager* d = reinterpret_cast<DestManager*>(cinfo->dest);
    d->outStream->updateEnv(env);
}

void CleanDst(j_compress_ptr cinfo) {
    if (cinfo != NULL && cinfo->dest != NULL) {
        DestManager *d = reinterpret_cast<DestManager*>(cinfo->dest);
        if (d->outStream != NULL) {
            delete d->outStream;
            d->outStream = NULL;
        }
    }
}

boolean Mgr_fill_input_buffer_fcn(j_decompress_ptr cinfo) {
    SourceManager *src = reinterpret_cast<SourceManager*>(cinfo->src);
    int32_t bytesRead = src->inStream->read(src->inStream->getBufferSize(), 0);
    if (bytesRead == J_DONE) {
        if (src->start_of_file == TRUE) {
            ERREXIT(cinfo, JERR_INPUT_EMPTY);
        }
        WARNMS(cinfo, JWRN_JPEG_EOF);
        bytesRead = src->inStream->forceReadEOI();
    } else if (bytesRead < 0) {
        ERREXIT(cinfo, JERR_FILE_READ);
    } else if (bytesRead == 0) {
        LOGW("read 0 bytes from InputStream.");
    }
    src->mgr.next_input_byte = reinterpret_cast<JOCTET*>(src->inStream->getBufferPtr());
    src->mgr.bytes_in_buffer = bytesRead;
    if (bytesRead != 0) {
        src->start_of_file = FALSE;
    }
    return TRUE;
}

void Mgr_init_source_fcn(j_decompress_ptr cinfo) {
    SourceManager *s = reinterpret_cast<SourceManager*>(cinfo->src);
    s->start_of_file = TRUE;
    Mgr_fill_input_buffer_fcn(cinfo);
}

void Mgr_skip_input_data_fcn(j_decompress_ptr cinfo, long num_bytes) {
    // Cannot skip negative or 0 bytes.
    if (num_bytes <= 0) {
        LOGW("skipping 0 bytes in InputStream");
        return;
    }
    SourceManager *src = reinterpret_cast<SourceManager*>(cinfo->src);
    if (src->mgr.bytes_in_buffer >= num_bytes) {
        src->mgr.bytes_in_buffer -= num_bytes;
        src->mgr.next_input_byte += num_bytes;
    } else {
        // if skipping more bytes than remain in buffer, set skip_bytes
        int64_t skip = num_bytes - src->mgr.bytes_in_buffer;
        src->mgr.next_input_byte += src->mgr.bytes_in_buffer;
        src->mgr.bytes_in_buffer = 0;
        int64_t actual = src->inStream->skip(skip);
        if (actual < 0) {
            ERREXIT(cinfo, JERR_FILE_READ);
        }
        skip -= actual;
        while (skip > 0) {
            actual = src->inStream->skip(skip);
            if (actual < 0) {
                ERREXIT(cinfo, JERR_FILE_READ);
            }
            skip -= actual;
            if (actual == 0) {
                // Multiple zero byte skips, likely EOF
                WARNMS(cinfo, JWRN_JPEG_EOF);
                return;
            }
        }
    }
}

void Mgr_term_source_fcn(j_decompress_ptr cinfo) {
    //noop
}

int32_t MakeSrc(j_decompress_ptr cinfo, JNIEnv *env, jobject inStream){
    if (cinfo->src != NULL) {
        LOGE("SourceManager already exists, cannot allocate!");
        return J_ERROR_FATAL;
    } else {
        size_t size = sizeof(SourceManager);
        cinfo->src = (struct jpeg_source_mgr *) (*cinfo->mem->alloc_small)
                ((j_common_ptr) cinfo, JPOOL_PERMANENT, size);
        if (cinfo->src == NULL) {
            // Could not allocate memory.
            LOGE("Could not allocate memory for SourceManager.");
            return J_ERROR_FATAL;
        }
        memset(cinfo->src, '0', size);
    }
    SourceManager *s = reinterpret_cast<SourceManager*>(cinfo->src);
    s->start_of_file = TRUE;
    s->mgr.init_source = Mgr_init_source_fcn;
    s->mgr.fill_input_buffer = Mgr_fill_input_buffer_fcn;
    s->mgr.skip_input_data = Mgr_skip_input_data_fcn;
    s->mgr.resync_to_restart = jpeg_resync_to_restart;  // use default restart
    s->mgr.term_source = Mgr_term_source_fcn;
    s->inStream = new InputStreamWrapper();
    if(s->inStream->init(env, inStream)) {
        return J_SUCCESS;
    }
    return J_ERROR_FATAL;
}

void UpdateSrcEnv(j_decompress_ptr cinfo, JNIEnv* env) {
    SourceManager* s = reinterpret_cast<SourceManager*>(cinfo->src);
    s->inStream->updateEnv(env);
}

void CleanSrc(j_decompress_ptr cinfo) {
    if (cinfo != NULL && cinfo->src != NULL) {
        SourceManager *s = reinterpret_cast<SourceManager*>(cinfo->src);
        if (s->inStream != NULL) {
            delete s->inStream;
            s->inStream = NULL;
        }
    }
}
