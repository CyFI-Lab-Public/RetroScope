/*
  This file is part of Valgrind, a dynamic binary instrumentation
  framework.

  Copyright (C) 2008-2008 Google Inc
     opensource@google.com

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License as
  published by the Free Software Foundation; either version 2 of the
  License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
  02111-1307, USA.

  The GNU General Public License is contained in the file COPYING.
*/

/* Author: Timur Iskhodzhanov <opensource@google.com>

 This file contains a set of Windows-specific unit tests for
 a data race detection tool.
*/

#include <gtest/gtest.h>
#include "test_utils.h"
#include "gtest_fixture_injection.h"

void DummyWorker() {
}

void LongWorker() {
  Sleep(1);
  volatile int i = 1 << 20;
  while(i--);
}

void WriteWorker(int *var) {
  LongWorker();
  *var = 42;
}

void VeryLongWriteWorker(int *var) {
  Sleep(1000);
  *var = 42;
}

TEST(NegativeTests, WindowsCreateThreadFailureTest) {  // {{{1
  HANDLE t = ::CreateThread(0, -1,
                           (LPTHREAD_START_ROUTINE)DummyWorker, 0, 0, 0);
  CHECK(t == 0);
}

TEST(NegativeTests, DISABLED_WindowsCreateThreadSuspendedTest) {  // {{{1
  // Hangs under TSan, see
  // http://code.google.com/p/data-race-test/issues/detail?id=61
  int *var = new int;
  HANDLE t = ::CreateThread(0, 0,
                           (LPTHREAD_START_ROUTINE)WriteWorker, var,
                           CREATE_SUSPENDED, 0);
  CHECK(t > 0);
  EXPECT_EQ(WAIT_TIMEOUT,  ::WaitForSingleObject(t, 200));
  *var = 1;
  EXPECT_EQ(1, ResumeThread(t));
  EXPECT_EQ(WAIT_OBJECT_0, ::WaitForSingleObject(t, INFINITE));
  EXPECT_EQ(42, *var);
  delete var;
}

TEST(NegativeTests, WindowsThreadStackSizeTest) {  // {{{1
// Just spawn few threads with different stack sizes.
  int sizes[3] = {1 << 19, 1 << 21, 1 << 22};
  for (int i = 0; i < 3; i++) {
    HANDLE t = ::CreateThread(0, sizes[i],
                             (LPTHREAD_START_ROUTINE)DummyWorker, 0, 0, 0);
    CHECK(t > 0);
    EXPECT_EQ(WAIT_OBJECT_0, ::WaitForSingleObject(t, INFINITE));
    CloseHandle(t);
  }
}

TEST(NegativeTests, WindowsJoinWithTimeout) {  // {{{1
  HANDLE t = ::CreateThread(0, 0,
                            (LPTHREAD_START_ROUTINE)LongWorker, 0, 0, 0);
  ASSERT_TRUE(t > 0);
  EXPECT_EQ(WAIT_TIMEOUT,  ::WaitForSingleObject(t, 1));
  EXPECT_EQ(WAIT_OBJECT_0, ::WaitForSingleObject(t, INFINITE));
  CloseHandle(t);
}

TEST(NegativeTests, HappensBeforeOnThreadJoin) {  // {{{1
  int *var = new int;
  HANDLE t = ::CreateThread(0, 0,
                            (LPTHREAD_START_ROUTINE)WriteWorker, var, 0, 0);
  ASSERT_TRUE(t > 0);
  // Calling WaitForSingleObject two times to make sure the H-B arc
  // is created on the second call. There was a bug that the thread handle
  // was deleted even when WaitForSingleObject timed out.
  EXPECT_EQ(WAIT_TIMEOUT,  ::WaitForSingleObject(t, 1));
  EXPECT_EQ(WAIT_OBJECT_0, ::WaitForSingleObject(t, INFINITE));
  EXPECT_EQ(*var, 42);
  CloseHandle(t);
  delete var;
}

TEST(NegativeTests, HappensBeforeOnThreadJoinTidReuse) {  // {{{1
  HANDLE t1 = ::CreateThread(0, 0, (LPTHREAD_START_ROUTINE)DummyWorker, 0, 0, 0);
  CloseHandle(t1);
  Sleep(1000);

  int *var = new int;
  HANDLE t2 = ::CreateThread(0, 0,
                             (LPTHREAD_START_ROUTINE)WriteWorker, var, 0, 0);
  printf("t1 = %d, t2 = %d\n");
  CHECK(t2 > 0);
  CHECK(WAIT_OBJECT_0 == ::WaitForSingleObject(t2, INFINITE));
  CHECK(*var == 42);
  delete var;
}

TEST(NegativeTests, WaitForMultipleObjectsWaitAllTest) {
  int var1 = 13,
      var2 = 13;
  HANDLE t1 = ::CreateThread(0, 0,
                            (LPTHREAD_START_ROUTINE)WriteWorker, &var1, 0, 0),
         t2 = ::CreateThread(0, 0,
                            (LPTHREAD_START_ROUTINE)WriteWorker, &var2, 0, 0);
  ASSERT_TRUE(t1 > 0);
  ASSERT_TRUE(t2 > 0);

  HANDLE handles[2] = {t1, t2};
  // Calling WaitForMultipleObjectsTest two times to make sure the H-B arc
  // are created on the second call.
  EXPECT_EQ(WAIT_TIMEOUT,  ::WaitForMultipleObjects(2, handles, TRUE, 1));
  EXPECT_EQ(WAIT_OBJECT_0, ::WaitForMultipleObjects(2, handles, TRUE, INFINITE));
  EXPECT_EQ(var1, 42);
  EXPECT_EQ(var2, 42);
  CloseHandle(t1);
  CloseHandle(t2);
}

TEST(NegativeTests, WaitForMultipleObjectsWaitOneTest) {
  int var1 = 13,
      var2 = 13;
  HANDLE t1 = ::CreateThread(0, 0,
                            (LPTHREAD_START_ROUTINE)VeryLongWriteWorker, &var1, 0, 0),
         t2 = ::CreateThread(0, 0,
                            (LPTHREAD_START_ROUTINE)WriteWorker, &var2, 0, 0);
  ASSERT_TRUE(t1 > 0);
  ASSERT_TRUE(t2 > 0);

  HANDLE handles[2] = {t1, t2};
  // Calling WaitForMultipleObjectsTest two times to make sure the H-B arc
  // are created on the second call.
  EXPECT_EQ(WAIT_TIMEOUT,  ::WaitForMultipleObjects(2, handles, FALSE, 1));
  EXPECT_EQ(WAIT_OBJECT_0 + 1, ::WaitForMultipleObjects(2, handles, FALSE, INFINITE));
  EXPECT_EQ(var2, 42);
  EXPECT_EQ(WAIT_OBJECT_0, ::WaitForMultipleObjects(1, handles, FALSE, INFINITE));
  EXPECT_EQ(var1, 42);
  CloseHandle(t1);
  CloseHandle(t2);
}

namespace RegisterWaitForSingleObjectTest {  // {{{1
StealthNotification *n = NULL;
HANDLE monitored_object = NULL;

void SignalStealthNotification() {
  n->wait();
  SetEvent(monitored_object);
}

void foo() { }

void CALLBACK DoneWaiting(void *param, BOOLEAN timed_out) {
  int *i = (int*)param;
  foo();  // make sure this function has a call. See issue 24.
  (*i)++;
}

TEST(NegativeTests, WindowsRegisterWaitForSingleObjectTest) {  // {{{1
  // These are very tricky false positive found while testing Chromium.
  //
  // Report #1:
  //   Everything after UnregisterWaitEx(*, INVALID_HANDLE_VALUE) happens-after
  //   execution of DoneWaiting callback. Currently, we don't catch this h-b.
  //
  // Report #2:
  //   The callback thread is re-used between Registet/Unregister/Register calls
  //   so we miss h-b between "int *obj = ..." and DoneWaiting on the second
  //   iteration.
  for (int i = 0; i < 2; i++) {
    n = new StealthNotification();
    int *obj = new int(0);
    HANDLE wait_object = NULL;

    monitored_object = ::CreateEvent(NULL, false, false, NULL);
    printf("monitored_object = %p\n", monitored_object);
    MyThread mt(SignalStealthNotification);
    mt.Start();
    ANNOTATE_TRACE_MEMORY(obj);
    CHECK(0 != ::RegisterWaitForSingleObject(&wait_object, monitored_object,
                                             DoneWaiting, obj, INFINITE,
                                             WT_EXECUTEINWAITTHREAD | WT_EXECUTEONLYONCE));
    printf("wait_object      = %p\n", wait_object);
    n->signal();
    mt.Join();
    Sleep(1000);
    CHECK(0 != ::UnregisterWaitEx(wait_object, INVALID_HANDLE_VALUE));
    (*obj)++;
    CHECK(*obj == 2);
    CloseHandle(monitored_object);
    delete n;
    delete obj;
  }
}
}

namespace QueueUserWorkItemTests {
DWORD CALLBACK Callback(void *param) {
  int *ptr = (int*)param;
  (*ptr)++;
  delete ptr;
  return 0;
}

TEST(NegativeTests, WindowsQueueUserWorkItemTest) {
  // False positive:
  //   The callback thread is allocated from a thread pool and can be re-used.
  //   As a result, we may miss h-b between "int *obj = ..." and Callback execution.
  for (int i = 0; i < 5; i++) {
    int *obj = new int(0);
    ANNOTATE_TRACE_MEMORY(obj);
    CHECK(QueueUserWorkItem(Callback, obj, i % 2 ? WT_EXECUTELONGFUNCTION : 0));
    Sleep(500);
  }
}

int GLOB = 42;

DWORD CALLBACK Callback2(void *param) {
  StealthNotification *ptr = (StealthNotification*)param;
  GLOB++;
  Sleep(100);
  ptr->signal();
  return 0;
}

TEST(PositiveTests, WindowsQueueUserWorkItemTest) {
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "PositiveTests.WindowsQueueUserWorkItemTest");

  const int N_THREAD = 5;
  StealthNotification n[N_THREAD];

  for (int i = 0; i < N_THREAD; i++)
    CHECK(QueueUserWorkItem(Callback2, &n[i], i % 2 ? WT_EXECUTELONGFUNCTION : 0));

  for (int i = 0; i < N_THREAD; i++)
    n[i].wait();
}
}

namespace WindowsCriticalSectionTest {  // {{{1
CRITICAL_SECTION cs;

TEST(NegativeTests, WindowsCriticalSectionTest) {
  InitializeCriticalSection(&cs);
  EnterCriticalSection(&cs);
  TryEnterCriticalSection(&cs);
  LeaveCriticalSection(&cs);
  DeleteCriticalSection(&cs);
}
}  // namespace


namespace WindowsSRWLockTest {  // {{{1
#if WINVER >= 0x0600 // Vista or Windows Server 2000
SRWLOCK SRWLock;
int *obj;

void Reader() {
  AcquireSRWLockShared(&SRWLock);
  CHECK(*obj <= 2 && *obj >= 0);
  ReleaseSRWLockShared(&SRWLock);
}

void Writer() {
  AcquireSRWLockExclusive(&SRWLock);
  (*obj)++;
  ReleaseSRWLockExclusive(&SRWLock);
}

#if 0  // This doesn't work in older versions of Windows.
void TryReader() {
  if (TryAcquireSRWLockShared(&SRWLock)) {
    CHECK(*obj <= 2 && *obj >= 0);
    ReleaseSRWLockShared(&SRWLock);
  }
}

void TryWriter() {
  if (TryAcquireSRWLockExclusive(&SRWLock)) {
    (*obj)++;
    ReleaseSRWLockExclusive(&SRWLock);
  }
}
#endif

TEST(NegativeTests, WindowsSRWLockTest) {
  InitializeSRWLock(&SRWLock);
  obj = new int(0);
  ANNOTATE_TRACE_MEMORY(obj);
  MyThreadArray t(Reader, Writer, Reader, Writer);
  t.Start();
  t.Join();
  AcquireSRWLockShared(&SRWLock);
  ReleaseSRWLockShared(&SRWLock);
  CHECK(*obj == 2);
  delete obj;
}

TEST(NegativeTests, WindowsSRWLockHackyInitializationTest) {
  // A similar pattern has been found on Chromium media_unittests
  InitializeSRWLock(&SRWLock);
  AcquireSRWLockExclusive(&SRWLock);
  // Leave the lock acquired

  // Reset the lock
  InitializeSRWLock(&SRWLock);
  obj = new int(0);
  MyThreadArray t(Reader, Writer, Reader, Writer);
  t.Start();
  t.Join();
  delete obj;
}
#endif // WINVER >= 0x0600
}  // namespace

namespace WindowsConditionVariableSRWTest {  // {{{1
#if WINVER >= 0x0600 // Vista or Windows Server 2000
SRWLOCK SRWLock;
CONDITION_VARIABLE cv;
bool cond;
int *obj;

StealthNotification n;

void WaiterSRW() {
  *obj = 1;
  n.wait();
  AcquireSRWLockExclusive(&SRWLock);
  cond = true;
  WakeConditionVariable(&cv);
  ReleaseSRWLockExclusive(&SRWLock);
}

void WakerSRW() {
  AcquireSRWLockExclusive(&SRWLock);
  n.signal();
  while (!cond) {
    SleepConditionVariableSRW(&cv, &SRWLock, 10, 0);
  }
  ReleaseSRWLockExclusive(&SRWLock);
  CHECK(*obj == 1);
  *obj = 2;
}

TEST(NegativeTests, WindowsConditionVariableSRWTest) {
  InitializeSRWLock(&SRWLock);
  InitializeConditionVariable(&cv);
  obj = new int(0);
  cond = false;
  ANNOTATE_TRACE_MEMORY(obj);
  MyThreadArray t(WaiterSRW, WakerSRW);
  t.Start();
  t.Join();
  CHECK(*obj == 2);
  delete obj;
}
#endif // WINVER >= 0x0600
}  // namespace


namespace WindowsInterlockedListTest {  // {{{1
SLIST_HEADER list;

struct Item {
  SLIST_ENTRY entry;
  int foo;
};

void Push() {
  Item *item = new Item;
  item->foo = 42;
  InterlockedPushEntrySList(&list, (PSINGLE_LIST_ENTRY)item);
}

void Pop() {
  Item *item;
  while (0 == (item = (Item*)InterlockedPopEntrySList(&list))) {
    Sleep(1);
  }
  CHECK(item->foo == 42);
  delete item;
}

TEST(NegativeTests, WindowsInterlockedListTest) {
  InitializeSListHead(&list);
  MyThreadArray t(Push, Pop);
  t.Start();
  t.Join();
}

}  // namespace

namespace FileSystemReports {  // {{{1

// This is a test for the flaky report found in
// Chromium net_unittests.
//
// Looks like the test is sensitive to memory allocations / scheduling order,
// so you shouldn't run other tests while investigating the issue.
// The report is ~50% flaky.

HANDLE hDone = NULL;

void CreateFileJob() {
  HANDLE hFile = CreateFileA("ZZZ\\tmpfile", GENERIC_READ | GENERIC_WRITE,
                      FILE_SHARE_READ, NULL, CREATE_ALWAYS,
                      FILE_ATTRIBUTE_NORMAL, NULL);
  CloseHandle(hFile);
  DWORD attr1 = GetFileAttributes("ZZZ");  // "Concurrent write" is here.
}

DWORD CALLBACK PrintDirectoryListingJob(void *param) {
  Sleep(500);
  WIN32_FIND_DATAA data;

  // "Current write" is here.
  HANDLE hFind = FindFirstFileA("ZZZ/*", &data);
  CHECK(hFind != INVALID_HANDLE_VALUE);

  CloseHandle(hFind);
  SetEvent(hDone);
  return 0;
}

// This test is not very friendly to bots environment, so you should only
// run it manually.
TEST(NegativeTests, DISABLED_CreateFileVsFindFirstFileTest) {
  hDone = ::CreateEvent(NULL, false, false, NULL);

  ::CreateDirectory("ZZZ", NULL);

  // Run PrintDirectoryListingJob in a concurrent thread.
  CHECK(::QueueUserWorkItem(PrintDirectoryListingJob, NULL,
                          WT_EXECUTELONGFUNCTION));
  CreateFileJob();

  ::WaitForSingleObject(hDone, INFINITE);
  ::CloseHandle(hDone);
  CHECK(::DeleteFile("ZZZ\\tmpfile"));
  CHECK(::RemoveDirectory("ZZZ"));
}

}  //namespace

namespace WindowsAtomicsTests { // {{{1
// This test should not give us any reports if compiled with proper MSVS flags.
// The Atomic_{Read,Write} functions are ignored in racecheck_unittest.ignore

int GLOB = 42;

inline int Atomic_Read(volatile const int* ptr) {
  // MSVS volatile gives us atomicity.
  int value = *ptr;
  return value;
}

inline void Atomic_Write(volatile int* ptr, int value) {
  // MSVS volatile gives us atomicity.
  *ptr = value;
}

void Worker() {
  int value = Atomic_Read(&GLOB);
  Atomic_Write(&GLOB, ~value);
}

TEST(NegativeTests, WindowsAtomicsTests) {
  MyThreadArray mta(Worker, Worker);
  mta.Start();
  mta.Join();
}

}  // namespace

namespace WindowsSemaphoreTests {
void Poster(int *var, HANDLE sem) {
  *var = 1;
  ReleaseSemaphore(sem, 1, NULL);
}

void Waiter(int *var, HANDLE sem) {
  DWORD ret = ::WaitForSingleObject(sem, INFINITE);
  ASSERT_EQ(ret, WAIT_OBJECT_0);
  EXPECT_EQ(*var, 1);
}

TEST(NegativeTests, SimpleSemaphoreTest) {
  HANDLE sem = CreateSemaphore(NULL,
                               0 /* initial count */,
                               20 /* max count */,
                               NULL);
  ASSERT_TRUE(sem != NULL);

  {
    int VAR = 0;
    ThreadPool tp(2);
    tp.StartWorkers();
    tp.Add(NewCallback(Waiter, &VAR, sem));
    tp.Add(NewCallback(Poster, &VAR, sem));
  }

  CloseHandle(sem);
}

TEST(NegativeTests, DISABLED_SemaphoreNameReuseTest) {
  // TODO(timurrrr): Semaphore reuse is not yet understood by TSan.
  const char NAME[] = "SemaphoreZZZ";
  HANDLE h1 = CreateSemaphore(NULL, 0, 10, NAME),
         h2 = CreateSemaphore(NULL, 0, 15, NAME);
  ASSERT_TRUE(h1 != NULL);
  ASSERT_TRUE(h2 != NULL);

  // h1 and h2 refer to the same semaphore but are not equal.
  EXPECT_NE(h1, h2);

  {
    int VAR = 0;
    ThreadPool tp(2);
    tp.StartWorkers();
    tp.Add(NewCallback(Waiter, &VAR, h1));
    tp.Add(NewCallback(Poster, &VAR, h2));
  }

  CloseHandle(h1);
  CloseHandle(h2);
}

}

namespace HandleReuseTests {

void Waker(int *var, HANDLE h) {
  *var = 1;
  SetEvent(h);
}

void Waiter(int *var, HANDLE h) {
  DWORD ret = ::WaitForSingleObject(h, INFINITE);
  ASSERT_EQ(ret, WAIT_OBJECT_0);
  EXPECT_EQ(*var, 1);
}

TEST(NegativeTests, DISABLED_EventHandleReuseTest) {
  // TODO(timurrrr): DuplicateHandle is not yet understood by TSan.
  HANDLE h1 = CreateEvent(NULL, false, false, NULL);
  ASSERT_TRUE(h1 != NULL);
  HANDLE h2 = NULL;
  DuplicateHandle(GetCurrentProcess(), h1,
                  GetCurrentProcess(), &h2,
                  0 /* access */, FALSE /* inherit*/, DUPLICATE_SAME_ACCESS);
  ASSERT_TRUE(h2 != NULL);

  // h1 and h2 refer to the same Event but are not equal.
  EXPECT_NE(h1, h2);

  {
    int VAR = 0;
    ThreadPool tp(2);
    tp.StartWorkers();
    tp.Add(NewCallback(Waiter, &VAR, h1));
    tp.Add(NewCallback(Waker,  &VAR, h2));
  }

  CloseHandle(h1);
  CloseHandle(h2);
}

}
// End {{{1
 // vim:shiftwidth=2:softtabstop=2:expandtab:foldmethod=marker
