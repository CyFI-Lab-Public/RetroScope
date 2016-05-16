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

#ifndef __OPENSSL_HASH__
#define __OPENSSL_HASH__

#include <openssl/evp.h>

extern  const EVP_MD *sha1_md;
#define SHA1_SIGNATURE_SIZE 20
#define SHA1_CTX	EVP_MD_CTX
#define SHA1_Init(ctx)  { \
    EVP_MD_CTX_init(ctx); \
    EVP_DigestInit_ex(ctx, sha1_md, NULL); \
}
#define SHA1_Update EVP_DigestUpdate
#define SHA1_Final(digest, ctx)  { \
    int md_len; \
    EVP_DigestFinal_ex(ctx, digest, &md_len); \
}

extern const EVP_MD *md4_md;
#define MD4_CTX EVP_MD_CTX
#define MD4Init(ctx)  { \
    EVP_MD_CTX_init(ctx); \
    EVP_DigestInit_ex(ctx, md4_md, NULL); \
}
#define MD4Update   EVP_DigestUpdate
#define MD4Final    SHA1_Final

extern const EVP_MD *md5_md;
#define MD5_CTX EVP_MD_CTX
#define MD5_Init(ctx)  { \
    EVP_MD_CTX_init(ctx); \
    EVP_DigestInit_ex(ctx, md5_md, NULL); \
}
#define MD5_Update   EVP_DigestUpdate
#define MD5_Final    SHA1_Final

extern  void    openssl_hash_init();

#endif
