/*
 * Copyright 2011, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "MemChunk.h"

#include "utils/flush_cpu_cache.h"
#include "utils/helper.h"

#include <llvm/Support/raw_ostream.h>

#ifndef USE_MINGW       /* TODO create a proper HAVE_MMAN_H */
#include <sys/mman.h>
#else
#include "mmanWindows.h"
#endif

#include <stdlib.h>

#ifndef MAP_32BIT
#define MAP_32BIT 0
// Note: If the <sys/mman.h> does not come with MAP_32BIT, then we
// define it as zero, so that it won't manipulate the flags.
#endif

//#define USE_FIXED_ADDR_MEM_CHUNK 1

#if USE_FIXED_ADDR_MEM_CHUNK
static uintptr_t StartAddr = 0x7e000000UL;
#endif

AllocFunc MemChunk::VendorAlloc = NULL;
FreeFunc MemChunk::VendorFree = NULL;

MemChunk::MemChunk() : buf(NULL), buf_size(0), bVendorBuf(true) {
}

MemChunk::~MemChunk() {
  if (!invalidBuf() && bVendorBuf && VendorFree) {
    (*VendorFree)(buf);
    return;
  }
  if (!invalidBuf()) {
    munmap(buf, buf_size);
  }
}

bool MemChunk::invalidBuf() const {
  return (buf == 0 || buf == (unsigned char *)MAP_FAILED);
}

bool MemChunk::allocate(size_t size) {
  if (size == 0) {
    return true;
  }
  if (VendorAlloc) {
    buf = (unsigned char*)(*VendorAlloc)(size, 0);
  }
  if (invalidBuf()) {
    bVendorBuf = false;
#if USE_FIXED_ADDR_MEM_CHUNK
    buf = (unsigned char *)mmap((void *)StartAddr, size,
                                PROT_READ | PROT_WRITE,
                                MAP_PRIVATE | MAP_ANON | MAP_32BIT,
                                -1, 0);
#else
    buf = (unsigned char *)mmap(0, size,
                                PROT_READ | PROT_WRITE,
                                MAP_PRIVATE | MAP_ANON | MAP_32BIT,
                                -1, 0);
#endif
  }

  if (invalidBuf()) {
    return false;
  }

#if USE_FIXED_ADDR_MEM_CHUNK
  StartAddr += (size + 4095) / 4096 * 4096;
#endif

  buf_size = size;
  return true;
}

void MemChunk::print() const {
  if (!invalidBuf()) {
    dump_hex(buf, buf_size, 0, buf_size);
  }
}

bool MemChunk::protect(int prot) {
  if (buf_size > 0) {
    int ret = mprotect((void *)buf, buf_size, prot);
    if (ret == -1) {
      llvm::errs() << "Error: Can't mprotect.\n";
      return false;
    }

    if (prot & PROT_EXEC) {
      FLUSH_CPU_CACHE(buf, buf + buf_size);
    }
  }

  return true;
}
