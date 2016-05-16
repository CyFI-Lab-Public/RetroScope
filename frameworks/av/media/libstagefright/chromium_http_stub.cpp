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

#include <dlfcn.h>

#include <media/stagefright/DataSource.h>

#include "include/chromium_http_stub.h"
#include "include/HTTPBase.h"

namespace android {

static bool gFirst = true;
static void *gHandle;
static Mutex gLibMutex;

HTTPBase *(*gLib_createChromiumHTTPDataSource)(uint32_t flags);
DataSource *(*gLib_createDataUriSource)(const char *uri);

status_t (*gLib_UpdateChromiumHTTPDataSourceProxyConfig)(
        const char *host, int32_t port, const char *exclusionList);

static bool load_libstagefright_chromium_http() {
    Mutex::Autolock autoLock(gLibMutex);
    void *sym;

    if (!gFirst) {
        return (gHandle != NULL);
    }

    gFirst = false;

    gHandle = dlopen("libstagefright_chromium_http.so", RTLD_NOW);
    if (gHandle == NULL) {
        return false;
    }

    sym = dlsym(gHandle, "createChromiumHTTPDataSource");
    if (sym == NULL) {
        gHandle = NULL;
        return false;
    }
    gLib_createChromiumHTTPDataSource = (HTTPBase *(*)(uint32_t))sym;

    sym = dlsym(gHandle, "createDataUriSource");
    if (sym == NULL) {
        gHandle = NULL;
        return false;
    }
    gLib_createDataUriSource = (DataSource *(*)(const char *))sym;

    sym = dlsym(gHandle, "UpdateChromiumHTTPDataSourceProxyConfig");
    if (sym == NULL) {
        gHandle = NULL;
        return false;
    }
    gLib_UpdateChromiumHTTPDataSourceProxyConfig =
        (status_t (*)(const char *, int32_t, const char *))sym;

    return true;
}

HTTPBase *createChromiumHTTPDataSource(uint32_t flags) {
    if (!load_libstagefright_chromium_http()) {
        return NULL;
    }

    return gLib_createChromiumHTTPDataSource(flags);
}

status_t UpdateChromiumHTTPDataSourceProxyConfig(
        const char *host, int32_t port, const char *exclusionList) {
    if (!load_libstagefright_chromium_http()) {
        return INVALID_OPERATION;
    }

    return gLib_UpdateChromiumHTTPDataSourceProxyConfig(
            host, port, exclusionList);
}

DataSource *createDataUriSource(const char *uri) {
    if (!load_libstagefright_chromium_http()) {
        return NULL;
    }

    return gLib_createDataUriSource(uri);
}

}
