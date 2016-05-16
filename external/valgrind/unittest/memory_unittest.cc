/* Copyright (c) 2010, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ---
 * Author: Timur Iskhodzhanov
 *
 * This file contains a set of unit tests for memory error detection tools.
 */

#include "test_utils.h"
#include "old_test_suite.h"

#include <gtest/gtest.h>

#ifdef WIN32
#include <Wbemidl.h>
#pragma comment(lib, "Wbemuuid.lib")
#pragma comment(lib, "Ole32.lib")
#endif

void Noop() {}

namespace NoopTest {
  REGISTER_TEST(Noop, 0);
  // Dummy to initialize 'TheMapOfTests'
}

TEST(Wrappers, StrchrTest) {
  // There were bugs in TSan and Dr. Memory with strchr wrappers.
  // Fix for TSan bug: http://code.google.com/p/data-race-test/source/diff?spec=svn1641&old=1527&r=1645&format=side&path=/trunk/tsan/ts_replace.h
  // Dr. Memory bug:   http://code.google.com/p/dynamorio/issues/detail?id=275
  char foo[8] = {10, 20, 127, (char)128, (char)250, -50, 0};
  EXPECT_TRUE(strchr(foo, 10) != 0);
  EXPECT_TRUE(strchr(foo, 127) != 0);
  EXPECT_TRUE(strchr(foo, 128) != 0);
  EXPECT_TRUE(strchr(foo, 250) != 0);
  EXPECT_TRUE(strchr(foo, -50) != 0);
  EXPECT_TRUE(strchr(foo, -60) == 0);
  EXPECT_TRUE(strchr(foo, 0) != 0);
  EXPECT_TRUE(strchr(foo, 0) == foo + strlen(foo));

  EXPECT_TRUE(strrchr(foo, 10) != 0);
  EXPECT_TRUE(strrchr(foo, 0) != 0);
  EXPECT_TRUE(strrchr(foo, 0) == foo + strlen(foo));
  EXPECT_TRUE(strrchr(foo, 250) != 0);
  EXPECT_TRUE(strrchr(foo, -60) == 0);

#ifdef WIN32
  EXPECT_TRUE(lstrlenA(NULL) == 0);
  EXPECT_TRUE(lstrlenW(NULL) == 0);
#endif
  //EXPECT_EQ(
}

TEST(Threads, EmptyThreadTest) {
  // DrMemory bug http://code.google.com/p/dynamorio/issues/detail?id=286
  MyThread mt(Noop);
  mt.Start();
  mt.Join();
}

#ifdef WIN32
TEST(SyscallTests, OutputDebugStringTest) {
  // DrMemory bug http://code.google.com/p/dynamorio/issues/detail?id=281
  OutputDebugString("Hello!\n");
}

TEST(ComTests, IWbemLocator_ConnectServerTest) {
  // DrMemory crashes on this test,
  // see http://code.google.com/p/drmemory/issues/detail?id=21
  HRESULT hr;
  ::CoInitialize(NULL);
  IWbemLocator *wmi_locator = NULL;
  hr = ::CoCreateInstance(CLSID_WbemLocator, NULL, CLSCTX_INPROC_SERVER,
                          __uuidof(IWbemLocator),
                          reinterpret_cast<void**>(&wmi_locator));
  ASSERT_FALSE(FAILED(hr));

  printf("before ConnectServer...\n");
  IWbemServices *wmi_services_r = NULL;
  hr = wmi_locator->ConnectServer(L"ROOT\\CIMV2", NULL, NULL, 0,
                                  NULL, 0, 0, &wmi_services_r);
  printf("after  ConnectServer...\n");
  EXPECT_FALSE(FAILED(hr));

  wmi_locator->Release();
  wmi_services_r->Release();
  ::CoUninitialize();
}

namespace HeapTests {
class MyMutex {
 public:
  MyMutex() {
    ::InitializeCriticalSectionAndSpinCount(&lock_, 2000);
  }
  ~MyMutex() {
    ::DeleteCriticalSection(&lock_);
  }
 private:
  CRITICAL_SECTION lock_;
};

TEST(HeapTest, MutexAllocatedOnHeapTest) {
  MyMutex *m = new MyMutex();
  delete m;
}

class MyClass {
 public:
  explicit MyClass(int size) : ptr_(NULL) {
    ptr_ = realloc(ptr_, size);
  }
  ~MyClass() {
    free(ptr_);
  }
 private:
  void *ptr_;
};

TEST(HeapTest, ReallocInHeapObjectTest) {
  const MyClass m(50031);
  MyClass *obj = new MyClass(50031);
  delete obj;
}

}  // namespace HeapTests

#endif
