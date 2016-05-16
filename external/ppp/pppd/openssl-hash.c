/*
 * Copyright (C) 2009 The Android Open Source Project
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

#include <openssl/evp.h>

const EVP_MD *sha1_md;
const EVP_MD *md4_md;
const EVP_MD *md5_md;

void    openssl_hash_init() {
    /* Use the SHA1 functions in openssl to save the flash space.*/
    OpenSSL_add_all_digests();
    sha1_md = EVP_get_digestbyname("sha1");
    if (!sha1_md) {
        dbglog("Error Unknown message digest SHA1\n");
        exit(1);
    }
    md4_md = EVP_get_digestbyname("md4");
    if (!md4_md) {
        dbglog("Error Unknown message digest MD4\n");
        exit(1);
    }
    md5_md = EVP_get_digestbyname("md5");
    if (!md5_md) {
        dbglog("Error Unknown message digest MD5\n");
        exit(1);
    }
}
