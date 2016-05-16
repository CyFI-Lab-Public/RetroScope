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

#include <include/chromium_http_stub.h>
#include <include/ChromiumHTTPDataSource.h>
#include <include/DataUriSource.h>

namespace android {

HTTPBase *createChromiumHTTPDataSource(uint32_t flags) {
    return new ChromiumHTTPDataSource(flags);
}

status_t UpdateChromiumHTTPDataSourceProxyConfig(
        const char *host, int32_t port, const char *exclusionList) {
    return ChromiumHTTPDataSource::UpdateProxyConfig(host, port, exclusionList);
}

DataSource *createDataUriSource(const char *uri) {
    return new DataUriSource(uri);
}

}
