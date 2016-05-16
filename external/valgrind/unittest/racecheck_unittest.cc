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

/* Author: Konstantin Serebryany <opensource@google.com>

 This file contains a set of unit tests for a data race detection tool.

 These tests can be compiled with pthreads (default) or
 with any other library that supports threads, locks, cond vars, etc.

*/

#include <fcntl.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>

#include <string>
#include <queue>
#include <vector>

#include "old_test_suite.h"
#include "test_utils.h"

#include <gtest/gtest.h>
#include "gtest_fixture_injection.h"

// The tests are
// - Stability tests (marked STAB)
// - Performance tests (marked PERF)
// - Feature tests
//   - TN (true negative) : no race exists and the tool is silent.
//   - TP (true positive) : a race exists and reported.
//   - FN (false negative): a race exists but not reported.
//   - FP (false positive): no race exists but the tool reports it.
//
// The feature tests are marked according to the behavior of ThreadSanitizer.
//
// TP and FP tests are annotated with ANNOTATE_EXPECT_RACE,
// so, no error reports should be seen when running under ThreadSanitizer.
//
// When some of the FP cases are fixed in helgrind we'll need
// to update these tests.
//
// Each test resides in its own namespace.
// Namespaces are named test01, test02, ...
// Please, *DO NOT* change the logic of existing tests nor rename them.
// Create a new test instead.
//
// Some tests use sleep()/usleep().
// This is not a synchronization, but a simple way to trigger
// some specific behaviour of the race detector's scheduler.

// Globals and utilities used by several tests. {{{1
static CondVar CV;
static int     COND = 0;

// test00: {{{1
namespace test00 {
int     GLOB = 0;
void Run() {
  printf("test00: negative\n");
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 00)
}  // namespace test00


// test01: TP. Simple race (write vs write). {{{1
namespace test01 {
int     GLOB = 0;

void Worker1() {
  GLOB = 1;
}

void Worker2() {
  GLOB = 2;
}

void Run() {
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test01. TP.");
  ANNOTATE_TRACE_MEMORY(&GLOB);
  printf("test01: positive\n");
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 1);
}  // namespace test01


// test02: TN. Synchronization via CondVar. {{{1
namespace test02 {
int     GLOB = 0;
// Two write accesses to GLOB are synchronized because
// the pair of CV.Signal() and CV.Wait() establish happens-before relation.
//
// Waiter:                      Waker:
// 1. COND = 0
// 2. Start(Waker)
// 3. MU.Lock()                 a. write(GLOB)
//                              b. MU.Lock()
//                              c. COND = 1
//                         /--- d. CV.Signal()
//  4. while(COND)        /     e. MU.Unlock()
//       CV.Wait(MU) <---/
//  5. MU.Unlock()
//  6. write(GLOB)
Mutex   MU;

void Waker() {
  usleep(200000);  // Make sure the waiter blocks.
  GLOB = 1;

  MU.Lock();
  COND = 1;
  CV.Signal();
  MU.Unlock();
}

void Waiter() {
  ThreadPool pool(1);
  pool.StartWorkers();
  COND = 0;
  pool.Add(NewCallback(Waker));
  MU.Lock();
  while(COND != 1)
    CV.Wait(&MU);
  MU.Unlock();
  GLOB = 2;
}
void Run() {
  printf("test02: negative\n");
  Waiter();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 2);
}  // namespace test02


// test03: TN. Synchronization via LockWhen, signaller gets there first. {{{1
namespace test03 {
int     GLOB = 0;
// Two write accesses to GLOB are synchronized via conditional critical section.
// Note that LockWhen() happens first (we use sleep(1) to make sure)!
//
// Waiter:                           Waker:
// 1. COND = 0
// 2. Start(Waker)
//                                   a. write(GLOB)
//                                   b. MU.Lock()
//                                   c. COND = 1
//                              /--- d. MU.Unlock()
// 3. MU.LockWhen(COND==1) <---/
// 4. MU.Unlock()
// 5. write(GLOB)
Mutex   MU;

void Waker() {
  usleep(100000);  // Make sure the waiter blocks.
  GLOB = 1;

  MU.Lock();
  COND = 1; // We are done! Tell the Waiter.
  MU.Unlock(); // calls ANNOTATE_CONDVAR_SIGNAL;
}
void Waiter() {
  ThreadPool pool(1);
  pool.StartWorkers();
  COND = 0;
  pool.Add(NewCallback(Waker));
  MU.LockWhen(Condition(&ArgIsOne, &COND));  // calls ANNOTATE_CONDVAR_WAIT
  MU.Unlock();  // Waker is done!

  GLOB = 2;
}
void Run() {
  printf("test03: negative\n");
  Waiter();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 3, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test03

// test04: TN. Synchronization via PCQ. {{{1
namespace test04 {
int     GLOB = 0;
ProducerConsumerQueue Q(INT_MAX);
// Two write accesses to GLOB are separated by PCQ Put/Get.
//
// Putter:                        Getter:
// 1. write(GLOB)
// 2. Q.Put() ---------\          .
//                      \-------> a. Q.Get()
//                                b. write(GLOB)


void Putter() {
  GLOB = 1;
  Q.Put(NULL);
}

void Getter() {
  Q.Get();
  GLOB = 2;
}

void Run() {
  printf("test04: negative\n");
  MyThreadArray t(Putter, Getter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 4);
}  // namespace test04


// test05: FP. Synchronization via CondVar, but waiter does not block. {{{1
// Since CondVar::Wait() is not called, we get a false positive.
namespace test05 {
int     GLOB = 0;
// Two write accesses to GLOB are synchronized via CondVar.
// But race detector can not see it.
// See this for details:
// http://www.valgrind.org/docs/manual/hg-manual.html#hg-manual.effective-use.
//
// Waiter:                                  Waker:
// 1. COND = 0
// 2. Start(Waker)
// 3. MU.Lock()                             a. write(GLOB)
//                                          b. MU.Lock()
//                                          c. COND = 1
//                                          d. CV.Signal()
//  4. while(COND)                          e. MU.Unlock()
//       CV.Wait(MU) <<< not called
//  5. MU.Unlock()
//  6. write(GLOB)
Mutex   MU;

void Waker() {
  GLOB = 1;
  MU.Lock();
  COND = 1;
  CV.Signal();
  MU.Unlock();
}

void Waiter() {
  usleep(100000);  // Make sure the signaller gets first.
  MU.Lock();
  while(COND != 1)
    CV.Wait(&MU);
  MU.Unlock();
  GLOB = 2;
}
void Run() {
  printf("test05: unavoidable false positive\n");
  COND = 0;
  if (!Tsan_PureHappensBefore())
    ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test05. FP. Unavoidable in hybrid scheme.");
  MyThreadArray t(Waker, Waiter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 5);
}  // namespace test05


// test06: TN. Synchronization via CondVar, but Waker gets there first.  {{{1
namespace test06 {
int     GLOB = 0;
// Same as test05 but we annotated the Wait() loop.
//
// Waiter:                                            Waker:
// 1. COND = 0
// 2. Start(Waker)
// 3. MU.Lock()                                       a. write(GLOB)
//                                                    b. MU.Lock()
//                                                    c. COND = 1
//                                           /------- d. CV.Signal()
//  4. while(COND)                          /         e. MU.Unlock()
//       CV.Wait(MU) <<< not called        /
//  6. ANNOTATE_CONDVAR_WAIT(CV, MU) <----/
//  5. MU.Unlock()
//  6. write(GLOB)

Mutex   MU;

void Waker() {
  GLOB = 1;
  MU.Lock();
  COND = 1;
  CV.Signal();
  MU.Unlock();
}

void Waiter() {
  ThreadPool pool(1);
  pool.StartWorkers();
  COND = 0;
  pool.Add(NewCallback(Waker));
  usleep(500000);  // Make sure the signaller gets first.
  MU.Lock();
  while(COND != 1)
    CV.Wait(&MU);
  ANNOTATE_CONDVAR_LOCK_WAIT(&CV, &MU);

  MU.Unlock();
  GLOB = 2;
}
void Run() {
  printf("test06: negative\n");
  Waiter();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 6, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test06


// test07: TN. Synchronization via LockWhen(), Signaller is observed first. {{{1
namespace test07 {
int     GLOB = 0;
bool    COND = 0;
// Two write accesses to GLOB are synchronized via conditional critical section.
// LockWhen() is observed after COND has been set (due to sleep).
// Unlock() calls ANNOTATE_CONDVAR_SIGNAL().
//
// Waiter:                           Signaller:
// 1. COND = 0
// 2. Start(Signaller)
//                                   a. write(GLOB)
//                                   b. MU.Lock()
//                                   c. COND = 1
//                              /--- d. MU.Unlock calls ANNOTATE_CONDVAR_SIGNAL
// 3. MU.LockWhen(COND==1) <---/
// 4. MU.Unlock()
// 5. write(GLOB)

Mutex   MU;
void Signaller() {
  GLOB = 1;
  MU.Lock();
  COND = true; // We are done! Tell the Waiter.
  MU.Unlock(); // calls ANNOTATE_CONDVAR_SIGNAL;
}
void Waiter() {
  COND = false;
  MyThread t(Signaller);
  t.Start();
  usleep(100000);  // Make sure the signaller gets there first.

  MU.LockWhen(Condition(&ArgIsTrue, &COND));  // calls ANNOTATE_CONDVAR_WAIT
  MU.Unlock();  // Signaller is done!

  GLOB = 2; // If LockWhen didn't catch the signal, a race may be reported here.
  t.Join();
}
void Run() {
  printf("test07: negative\n");
  Waiter();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 7, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test07

// test08: TN. Synchronization via thread start/join. {{{1
namespace test08 {
int     GLOB = 0;
// Three accesses to GLOB are separated by thread start/join.
//
// Parent:                        Worker:
// 1. write(GLOB)
// 2. Start(Worker) ------------>
//                                a. write(GLOB)
// 3. Join(Worker) <------------
// 4. write(GLOB)
void Worker() {
  GLOB = 2;
}

void Parent() {
  MyThread t(Worker);
  GLOB = 1;
  t.Start();
  t.Join();
  GLOB = 3;
}
void Run() {
  printf("test08: negative\n");
  Parent();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 8);
}  // namespace test08


// test09: TP. Simple race (read vs write). {{{1
namespace test09 {
int     GLOB = 0;
// A simple data race between writer and reader.
// Write happens after read (enforced by sleep).
// Usually, easily detectable by a race detector.
void Writer() {
  usleep(100000);
  GLOB = 3;
}
void Reader() {
  CHECK(GLOB != -777);
}

void Run() {
  ANNOTATE_TRACE_MEMORY(&GLOB);
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test09. TP.");
  printf("test09: positive\n");
  MyThreadArray t(Writer, Reader);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 9);
}  // namespace test09


// test10: FN. Simple race (write vs read). {{{1
namespace test10 {
int     GLOB = 0;
// A simple data race between writer and reader.
// Write happens before Read (enforced by sleep),
// otherwise this test is the same as test09.
//
// Writer:                    Reader:
// 1. write(GLOB)             a. sleep(long enough so that GLOB
//                                is most likely initialized by Writer)
//                            b. read(GLOB)
//
//
// Eraser algorithm does not detect the race here,
// see Section 2.2 of http://citeseer.ist.psu.edu/savage97eraser.html.
//
void Writer() {
  GLOB = 3;
}
void Reader() {
  usleep(100000);
  CHECK(GLOB != -777);
}

void Run() {
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test10. TP. FN in MSMHelgrind.");
  printf("test10: positive\n");
  MyThreadArray t(Writer, Reader);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 10);
}  // namespace test10


// test12: FP. Synchronization via Mutex, then via PCQ. {{{1
namespace test12 {
int     GLOB = 0;
// This test is properly synchronized, but currently (Dec 2007)
// helgrind reports a false positive.
//
// First, we write to GLOB under MU, then we synchronize via PCQ,
// which is essentially a semaphore.
//
// Putter:                       Getter:
// 1. MU.Lock()                  a. MU.Lock()
// 2. write(GLOB) <---- MU ----> b. write(GLOB)
// 3. MU.Unlock()                c. MU.Unlock()
// 4. Q.Put()   ---------------> d. Q.Get()
//                               e. write(GLOB)

ProducerConsumerQueue Q(INT_MAX);
Mutex   MU;

void Putter() {
  MU.Lock();
  GLOB++;
  MU.Unlock();

  Q.Put(NULL);
}

void Getter() {
  MU.Lock();
  GLOB++;
  MU.Unlock();

  Q.Get();
  GLOB++;
}

void Run() {
//  ANNOTATE_EXPECT_RACE(&GLOB, "test12. FP. Fixed by MSMProp1.");
  printf("test12: negative\n");
  MyThreadArray t(Putter, Getter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 12);
}  // namespace test12


// test13: FP. Synchronization via Mutex, then via LockWhen. {{{1
namespace test13 {
int     GLOB = 0;
// This test is essentially the same as test12, but uses LockWhen
// instead of PCQ.
//
// Waker:                                     Waiter:
// 1. MU.Lock()                               a. MU.Lock()
// 2. write(GLOB) <---------- MU ---------->  b. write(GLOB)
// 3. MU.Unlock()                             c. MU.Unlock()
// 4. MU.Lock()                               .
// 5. COND = 1                                .
// 6. ANNOTATE_CONDVAR_SIGNAL -------\        .
// 7. MU.Unlock()                     \       .
//                                     \----> d. MU.LockWhen(COND == 1)
//                                            e. MU.Unlock()
//                                            f. write(GLOB)
Mutex   MU;

void Waker() {
  MU.Lock();
  GLOB++;
  MU.Unlock();

  MU.Lock();
  COND = 1;
  ANNOTATE_CONDVAR_SIGNAL(&MU);
  MU.Unlock();
}

void Waiter() {
  MU.Lock();
  GLOB++;
  MU.Unlock();

  MU.LockWhen(Condition(&ArgIsOne, &COND));
  MU.Unlock();
  GLOB++;
}

void Run() {
//  ANNOTATE_EXPECT_RACE(&GLOB, "test13. FP. Fixed by MSMProp1.");
  printf("test13: negative\n");
  COND = 0;

  MyThreadArray t(Waker, Waiter);
  t.Start();
  t.Join();

  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 13, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test13


// test14: FP. Synchronization via PCQ, reads, 2 workers. {{{1
namespace test14 {
int     GLOB = 0;
// This test is properly synchronized, but currently (Dec 2007)
// helgrind reports a false positive.
//
// This test is similar to test11, but uses PCQ (semaphore).
//
// Putter2:                  Putter1:                     Getter:
// 1. read(GLOB)             a. read(GLOB)
// 2. Q2.Put() ----\         b. Q1.Put() -----\           .
//                  \                          \--------> A. Q1.Get()
//                   \----------------------------------> B. Q2.Get()
//                                                        C. write(GLOB)
ProducerConsumerQueue Q1(INT_MAX), Q2(INT_MAX);

void Putter1() {
  CHECK(GLOB != 777);
  Q1.Put(NULL);
}
void Putter2() {
  CHECK(GLOB != 777);
  Q2.Put(NULL);
}
void Getter() {
  Q1.Get();
  Q2.Get();
  GLOB++;
}
void Run() {
//  ANNOTATE_EXPECT_RACE(&GLOB, "test14. FP. Fixed by MSMProp1.");
  printf("test14: negative\n");
  MyThreadArray t(Getter, Putter1, Putter2);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 14);
}  // namespace test14


// test15: TN. Synchronization via LockWhen. One waker and 2 waiters. {{{1
namespace test15 {
// Waker:                                   Waiter1, Waiter2:
// 1. write(GLOB)
// 2. MU.Lock()
// 3. COND = 1
// 4. ANNOTATE_CONDVAR_SIGNAL ------------> a. MU.LockWhen(COND == 1)
// 5. MU.Unlock()                           b. MU.Unlock()
//                                          c. read(GLOB)

int     GLOB = 0;
Mutex   MU;

void Waker() {
  GLOB = 2;

  MU.Lock();
  COND = 1;
  ANNOTATE_CONDVAR_SIGNAL(&MU);
  MU.Unlock();
};

void Waiter() {
  MU.LockWhen(Condition(&ArgIsOne, &COND));
  MU.Unlock();
  CHECK(GLOB != 777);
}


void Run() {
  COND = 0;
  printf("test15: negative\n");
  MyThreadArray t(Waker, Waiter, Waiter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 15);
}  // namespace test15


// test16: FP. Barrier (emulated by CV), 2 threads. {{{1
namespace test16 {
// Worker1:                                     Worker2:
// 1. MU.Lock()                                 a. MU.Lock()
// 2. write(GLOB) <------------ MU ---------->  b. write(GLOB)
// 3. MU.Unlock()                               c. MU.Unlock()
// 4. MU2.Lock()                                d. MU2.Lock()
// 5. COND--                                    e. COND--
// 6. ANNOTATE_CONDVAR_SIGNAL(MU2) ---->V       .
// 7. MU2.Await(COND == 0) <------------+------ f. ANNOTATE_CONDVAR_SIGNAL(MU2)
// 8. MU2.Unlock()                      V-----> g. MU2.Await(COND == 0)
// 9. read(GLOB)                                h. MU2.Unlock()
//                                              i. read(GLOB)
//
//
// TODO: This way we may create too many edges in happens-before graph.
// Arndt Mühlenfeld in his PhD (TODO: link) suggests creating special nodes in
// happens-before graph to reduce the total number of edges.
// See figure 3.14.
//
//
int     GLOB = 0;
Mutex   MU;
Mutex MU2;

void Worker() {
  MU.Lock();
  GLOB++;
  MU.Unlock();

  MU2.Lock();
  COND--;
  ANNOTATE_CONDVAR_SIGNAL(&MU2);
  MU2.Await(Condition(&ArgIsZero, &COND));
  MU2.Unlock();

  CHECK(GLOB == 2);
}

void Run() {
//  ANNOTATE_EXPECT_RACE(&GLOB, "test16. FP. Fixed by MSMProp1 + Barrier support.");
  COND = 2;
  printf("test16: negative\n");
  MyThreadArray t(Worker, Worker);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 16, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test16


// test17: FP. Barrier (emulated by CV), 3 threads. {{{1
namespace test17 {
// Same as test16, but with 3 threads.
int     GLOB = 0;
Mutex   MU;
Mutex MU2;

void Worker() {
  MU.Lock();
  GLOB++;
  MU.Unlock();

  MU2.Lock();
  COND--;
  ANNOTATE_CONDVAR_SIGNAL(&MU2);
  MU2.Await(Condition(&ArgIsZero, &COND));
  MU2.Unlock();

  CHECK(GLOB == 3);
}

void Run() {
  COND = 3;
  printf("test17: negative\n");
  MyThreadArray t(Worker, Worker, Worker);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 17, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test17


// test18: TN. Synchronization via Await(), signaller gets there first. {{{1
namespace test18 {
int     GLOB = 0;
Mutex   MU;
// Same as test03, but uses Mutex::Await() instead of Mutex::LockWhen().

void Waker() {
  usleep(100000);  // Make sure the waiter blocks.
  GLOB = 1;

  MU.Lock();
  COND = 1; // We are done! Tell the Waiter.
  MU.Unlock(); // calls ANNOTATE_CONDVAR_SIGNAL;
}
void Waiter() {
  ThreadPool pool(1);
  pool.StartWorkers();
  COND = 0;
  pool.Add(NewCallback(Waker));

  MU.Lock();
  MU.Await(Condition(&ArgIsOne, &COND));  // calls ANNOTATE_CONDVAR_WAIT
  MU.Unlock();  // Waker is done!

  GLOB = 2;
}
void Run() {
  printf("test18: negative\n");
  Waiter();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 18, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test18

// test19: TN. Synchronization via AwaitWithTimeout(). {{{1
namespace test19 {
int     GLOB = 0;
// Same as test18, but with AwaitWithTimeout. Do not timeout.
Mutex   MU;
void Waker() {
  usleep(100000);  // Make sure the waiter blocks.
  GLOB = 1;

  MU.Lock();
  COND = 1; // We are done! Tell the Waiter.
  MU.Unlock(); // calls ANNOTATE_CONDVAR_SIGNAL;
}
void Waiter() {
  ThreadPool pool(1);
  pool.StartWorkers();
  COND = 0;
  pool.Add(NewCallback(Waker));

  MU.Lock();
  CHECK(MU.AwaitWithTimeout(Condition(&ArgIsOne, &COND), INT_MAX));
  MU.Unlock();

  GLOB = 2;
}
void Run() {
  printf("test19: negative\n");
  Waiter();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 19, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test19

// test20: TP. Incorrect synchronization via AwaitWhen(), timeout. {{{1
namespace test20 {
int     GLOB = 0;
Mutex   MU;
// True race. We timeout in AwaitWhen.
void Waker() {
  GLOB = 1;
  usleep(100 * 1000);
}
void Waiter() {
  MU.Lock();
  CHECK(!MU.AwaitWithTimeout(Condition(&ArgIsOne, &COND), 100));
  MU.Unlock();

  GLOB = 2;
}
void Run() {
  printf("test20: positive\n");
  COND = 0;
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test20. TP.");
  MyThreadArray t(Waker, Waiter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 20, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test20

// test21: TP. Incorrect synchronization via LockWhenWithTimeout(). {{{1
namespace test21 {
int     GLOB = 0;
// True race. We timeout in LockWhenWithTimeout().
Mutex   MU;
void Waker() {
  GLOB = 1;
  usleep(100 * 1000);
}
void Waiter() {
  CHECK(!MU.LockWhenWithTimeout(Condition(&ArgIsOne, &COND), 100));
  MU.Unlock();

  GLOB = 2;
}
void Run() {
  printf("test21: positive\n");
  COND = 0;
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test21. TP.");
  MyThreadArray t(Waker, Waiter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 21, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test21

// test22: TP. Incorrect synchronization via CondVar::WaitWithTimeout(). {{{1
namespace test22 {
int     GLOB = 0;
Mutex   MU;
// True race. We timeout in CondVar::WaitWithTimeout().
void Waker() {
  GLOB = 1;
  usleep(100 * 1000);
}
void Waiter() {
  int ms_left_to_wait = 100;
  int deadline_ms = GetTimeInMs() + ms_left_to_wait;
  MU.Lock();
  while(COND != 1 && ms_left_to_wait > 0) {
    CV.WaitWithTimeout(&MU, ms_left_to_wait);
    ms_left_to_wait = deadline_ms - GetTimeInMs();
  }
  MU.Unlock();

  GLOB = 2;
}
void Run() {
  printf("test22: positive\n");
  COND = 0;
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test22. TP.");
  MyThreadArray t(Waker, Waiter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 22);
}  // namespace test22

// test23: TN. TryLock, ReaderLock, ReaderTryLock. {{{1
namespace test23 {
// Correct synchronization with TryLock, Lock, ReaderTryLock, ReaderLock.
int     GLOB = 0;
Mutex   MU;
void Worker_TryLock() {
  for (int i = 0; i < 20; i++) {
    while (true) {
      if (MU.TryLock()) {
        GLOB++;
        MU.Unlock();
        break;
      }
      usleep(1000);
    }
  }
}

void Worker_ReaderTryLock() {
  for (int i = 0; i < 20; i++) {
    while (true) {
      if (MU.ReaderTryLock()) {
        CHECK(GLOB != 777);
        MU.ReaderUnlock();
        break;
      }
      usleep(1000);
    }
  }
}

void Worker_ReaderLock() {
  for (int i = 0; i < 20; i++) {
    MU.ReaderLock();
    CHECK(GLOB != 777);
    MU.ReaderUnlock();
    usleep(1000);
  }
}

void Worker_Lock() {
  for (int i = 0; i < 20; i++) {
    MU.Lock();
    GLOB++;
    MU.Unlock();
    usleep(1000);
  }
}

void Run() {
  printf("test23: negative\n");
  MyThreadArray t(Worker_TryLock,
                  Worker_ReaderTryLock,
                  Worker_ReaderLock,
                  Worker_Lock
                  );
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 23);
}  // namespace test23

// test24: TN. Synchronization via ReaderLockWhen(). {{{1
namespace test24 {
int     GLOB = 0;
Mutex   MU;
// Same as test03, but uses ReaderLockWhen().

void Waker() {
  usleep(100000);  // Make sure the waiter blocks.
  GLOB = 1;

  MU.Lock();
  COND = 1; // We are done! Tell the Waiter.
  MU.Unlock(); // calls ANNOTATE_CONDVAR_SIGNAL;
}
void Waiter() {
  ThreadPool pool(1);
  pool.StartWorkers();
  COND = 0;
  pool.Add(NewCallback(Waker));
  MU.ReaderLockWhen(Condition(&ArgIsOne, &COND));
  MU.ReaderUnlock();

  GLOB = 2;
}
void Run() {
  printf("test24: negative\n");
  Waiter();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 24, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test24

// test25: TN. Synchronization via ReaderLockWhenWithTimeout(). {{{1
namespace test25 {
int     GLOB = 0;
Mutex   MU;
// Same as test24, but uses ReaderLockWhenWithTimeout().
// We do not timeout.

void Waker() {
  usleep(100000);  // Make sure the waiter blocks.
  GLOB = 1;

  MU.Lock();
  COND = 1; // We are done! Tell the Waiter.
  MU.Unlock(); // calls ANNOTATE_CONDVAR_SIGNAL;
}
void Waiter() {
  ThreadPool pool(1);
  pool.StartWorkers();
  COND = 0;
  pool.Add(NewCallback(Waker));
  CHECK(MU.ReaderLockWhenWithTimeout(Condition(&ArgIsOne, &COND), INT_MAX));
  MU.ReaderUnlock();

  GLOB = 2;
}
void Run() {
  printf("test25: negative\n");
  Waiter();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 25, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test25

// test26: TP. Incorrect synchronization via ReaderLockWhenWithTimeout(). {{{1
namespace test26 {
int     GLOB = 0;
Mutex   MU;
// Same as test25, but we timeout and incorrectly assume happens-before.

void Waker() {
  GLOB = 1;
  usleep(10000);
}
void Waiter() {
  CHECK(!MU.ReaderLockWhenWithTimeout(Condition(&ArgIsOne, &COND), 100));
  MU.ReaderUnlock();

  GLOB = 2;
}
void Run() {
  printf("test26: positive\n");
  COND = 0;
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test26. TP");
  MyThreadArray t(Waker, Waiter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 26, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test26


// test27: TN. Simple synchronization via SpinLock. {{{1
namespace test27 {
#ifndef NO_SPINLOCK
int     GLOB = 0;
SpinLock MU;
void Worker() {
  MU.Lock();
  GLOB++;
  MU.Unlock();
  usleep(10000);
}

void Run() {
  printf("test27: negative\n");
  MyThreadArray t(Worker, Worker, Worker, Worker);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 27, FEATURE|NEEDS_ANNOTATIONS);
#endif
}  // namespace test27


// test28: TN. Synchronization via Mutex, then PCQ. 3 threads {{{1
namespace test28 {
// Putter1:                       Getter:                         Putter2:
// 1. MU.Lock()                                                   A. MU.Lock()
// 2. write(GLOB)                                                 B. write(GLOB)
// 3. MU.Unlock()                                                 C. MU.Unlock()
// 4. Q.Put() ---------\                                 /------- D. Q.Put()
// 5. MU.Lock()         \-------> a. Q.Get()            /         E. MU.Lock()
// 6. read(GLOB)                  b. Q.Get() <---------/          F. read(GLOB)
// 7. MU.Unlock()                   (sleep)                       G. MU.Unlock()
//                                c. read(GLOB)
ProducerConsumerQueue Q(INT_MAX);
int     GLOB = 0;
Mutex   MU;

void Putter() {
  MU.Lock();
  GLOB++;
  MU.Unlock();

  Q.Put(NULL);

  MU.Lock();
  CHECK(GLOB != 777);
  MU.Unlock();
}

void Getter() {
  Q.Get();
  Q.Get();
  usleep(100000);
  CHECK(GLOB == 2);
}

void Run() {
  printf("test28: negative\n");
  MyThreadArray t(Getter, Putter, Putter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 28);
}  // namespace test28


// test29: TN. Synchronization via Mutex, then PCQ. 4 threads. {{{1
namespace test29 {
// Similar to test28, but has two Getters and two PCQs.
ProducerConsumerQueue *Q1, *Q2;
Mutex   MU;
int     GLOB = 0;

void Putter(ProducerConsumerQueue *q) {
  MU.Lock();
  GLOB++;
  MU.Unlock();

  q->Put(NULL);
  q->Put(NULL);

  MU.Lock();
  CHECK(GLOB != 777);
  MU.Unlock();

}

void Putter1() { Putter(Q1); }
void Putter2() { Putter(Q2); }

void Getter() {
  Q1->Get();
  Q2->Get();
  usleep(100000);
  CHECK(GLOB == 2);
  usleep(48000); //  TODO: remove this when FP in test32 is fixed.
}

void Run() {
  printf("test29: negative\n");
  Q1 = new ProducerConsumerQueue(INT_MAX);
  Q2 = new ProducerConsumerQueue(INT_MAX);
  MyThreadArray t(Getter, Getter, Putter1, Putter2);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
  delete Q1;
  delete Q2;
}
REGISTER_TEST(Run, 29);
}  // namespace test29


// test30: TN. Synchronization via 'safe' race. Writer vs multiple Readers. {{{1
namespace test30 {
// This test shows a very risky kind of synchronization which is very easy
// to get wrong. Actually, I am not sure I've got it right.
//
// Writer:                                 Reader1, Reader2, ..., ReaderN:
// 1. write(GLOB[i]: i >= BOUNDARY)        a. n = BOUNDARY
// 2. HAPPENS_BEFORE(BOUNDARY+1)  -------> b. HAPPENS_AFTER(n)
// 3. BOUNDARY++;                          c. read(GLOB[i]: i < n)
//
// Here we have a 'safe' race on accesses to BOUNDARY and
// no actual races on accesses to GLOB[]:
// Writer writes to GLOB[i] where i>=BOUNDARY and then increments BOUNDARY.
// Readers read BOUNDARY and read GLOB[i] where i<BOUNDARY.
//
// I am not completely sure that this scheme guaranties no race between
// accesses to GLOB since compilers and CPUs
// are free to rearrange memory operations.
// I am actually sure that this scheme is wrong unless we use
// some smart memory fencing...


const int N = 48;
static int GLOB[N];
volatile int BOUNDARY = 0;

void Writer() {
  for (int i = 0; i < N; i++) {
    CHECK(BOUNDARY == i);
    for (int j = i; j < N; j++) {
      GLOB[j] = j;
    }
    ANNOTATE_HAPPENS_BEFORE(reinterpret_cast<void*>(BOUNDARY+1));
    BOUNDARY++;
    usleep(1000);
  }
}

void Reader() {
  int n;
  do {
    n = BOUNDARY;
    if (n == 0) continue;
    ANNOTATE_HAPPENS_AFTER(reinterpret_cast<void*>(n));
    for (int i = 0; i < n; i++) {
      CHECK(GLOB[i] == i);
    }
    usleep(100);
  } while(n < N);
}

void Run() {
  ANNOTATE_EXPECT_RACE((void*)(&BOUNDARY), "test30. Sync via 'safe' race.");
  printf("test30: negative\n");
  MyThreadArray t(Writer, Reader, Reader, Reader);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB[N-1]);
}
REGISTER_TEST2(Run, 30, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test30


// test31: TN. Synchronization via 'safe' race. Writer vs Writer. {{{1
namespace test31 {
// This test is similar to test30, but
// it has one Writer instead of mulitple Readers.
//
// Writer1:                                Writer2
// 1. write(GLOB[i]: i >= BOUNDARY)        a. n = BOUNDARY
// 2. HAPPENS_BEFORE(BOUNDARY+1)  -------> b. HAPPENS_AFTER(n)
// 3. BOUNDARY++;                          c. write(GLOB[i]: i < n)
//

const int N = 48;
static int GLOB[N];
volatile int BOUNDARY = 0;

void Writer1() {
  for (int i = 0; i < N; i++) {
    CHECK(BOUNDARY == i);
    for (int j = i; j < N; j++) {
      GLOB[j] = j;
    }
    ANNOTATE_HAPPENS_BEFORE(reinterpret_cast<void*>(BOUNDARY+1));
    BOUNDARY++;
    usleep(1000);
  }
}

void Writer2() {
  int n;
  do {
    n = BOUNDARY;
    if (n == 0) continue;
    ANNOTATE_HAPPENS_AFTER(reinterpret_cast<void*>(n));
    for (int i = 0; i < n; i++) {
      if(GLOB[i] == i) {
        GLOB[i]++;
      }
    }
    usleep(100);
  } while(n < N);
}

void Run() {
  ANNOTATE_EXPECT_RACE((void*)(&BOUNDARY), "test31. Sync via 'safe' race.");
  printf("test31: negative\n");
  MyThreadArray t(Writer1, Writer2);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB[N-1]);
}
REGISTER_TEST2(Run, 31, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test31


// test32: FP. Synchronization via thread create/join. W/R. {{{1
namespace test32 {
// This test is well synchronized but helgrind 3.3.0 reports a race.
//
// Parent:                   Writer:               Reader:
// 1. Start(Reader) -----------------------\       .
//                                          \      .
// 2. Start(Writer) ---\                     \     .
//                      \---> a. MU.Lock()    \--> A. sleep(long enough)
//                            b. write(GLOB)
//                      /---- c. MU.Unlock()
// 3. Join(Writer) <---/
//                                                 B. MU.Lock()
//                                                 C. read(GLOB)
//                                   /------------ D. MU.Unlock()
// 4. Join(Reader) <----------------/
// 5. write(GLOB)
//
//
// The call to sleep() in Reader is not part of synchronization,
// it is required to trigger the false positive in helgrind 3.3.0.
//
int     GLOB = 0;
Mutex   MU;

void Writer() {
  MU.Lock();
  GLOB = 1;
  MU.Unlock();
}

void Reader() {
  usleep(480000);
  MU.Lock();
  CHECK(GLOB != 777);
  MU.Unlock();
}

void Parent() {
  MyThread r(Reader);
  MyThread w(Writer);
  r.Start();
  w.Start();

  w.Join();  // 'w' joins first.
  r.Join();

  GLOB = 2;
}

void Run() {
//  ANNOTATE_EXPECT_RACE(&GLOB, "test32. FP. Fixed by MSMProp1.");
  printf("test32: negative\n");
  Parent();
  printf("\tGLOB=%d\n", GLOB);
}

REGISTER_TEST(Run, 32);
}  // namespace test32


// test33: STAB. Stress test for the number of thread sets (TSETs). {{{1
namespace test33 {
int     GLOB = 0;
// Here we access N memory locations from within log(N) threads.
// We do it in such a way that helgrind creates nearly all possible TSETs.
// Then we join all threads and start again (N_iter times).
const int N_iter = 48;
const int Nlog  = 15;
const int N     = 1 << Nlog;
static int ARR[N];
Mutex   MU;

void Worker() {
  MU.Lock();
  int n = ++GLOB;
  MU.Unlock();

  n %= Nlog;
  for (int i = 0; i < N; i++) {
    // ARR[i] is accessed by threads from i-th subset
    if (i & (1 << n)) {
        CHECK(ARR[i] == 0);
    }
  }
}

void Run() {
  printf("test33:\n");

  std::vector<MyThread*> vec(Nlog);

  for (int j = 0; j < N_iter; j++) {
    // Create and start Nlog threads
    for (int i = 0; i < Nlog; i++) {
      vec[i] = new MyThread(Worker);
    }
    for (int i = 0; i < Nlog; i++) {
      vec[i]->Start();
    }
    // Join all threads.
    for (int i = 0; i < Nlog; i++) {
      vec[i]->Join();
      delete vec[i];
    }
    printf("------------------\n");
  }

  printf("\tGLOB=%d; ARR[1]=%d; ARR[7]=%d; ARR[N-1]=%d\n",
         GLOB, ARR[1], ARR[7], ARR[N-1]);
}
REGISTER_TEST2(Run, 33, STABILITY|EXCLUDE_FROM_ALL);
}  // namespace test33


// test34: STAB. Stress test for the number of locks sets (LSETs). {{{1
namespace test34 {
// Similar to test33, but for lock sets.
int     GLOB = 0;
const int N_iter = 48;
const int Nlog = 10;
const int N    = 1 << Nlog;
static int ARR[N];
static Mutex *MUs[Nlog];

void Worker() {
    for (int i = 0; i < N; i++) {
      // ARR[i] is protected by MUs from i-th subset of all MUs
      for (int j = 0; j < Nlog; j++)  if (i & (1 << j)) MUs[j]->Lock();
      CHECK(ARR[i] == 0);
      for (int j = 0; j < Nlog; j++)  if (i & (1 << j)) MUs[j]->Unlock();
    }
}

void Run() {
  printf("test34:\n");
  for (int iter = 0; iter < N_iter; iter++) {
    for (int i = 0; i < Nlog; i++) {
      MUs[i] = new Mutex;
    }
    MyThreadArray t(Worker, Worker);
    t.Start();
    t.Join();
    for (int i = 0; i < Nlog; i++) {
      delete MUs[i];
    }
    printf("------------------\n");
  }
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 34, STABILITY|EXCLUDE_FROM_ALL);
}  // namespace test34


// test35: PERF. Lots of mutexes and lots of call to free().  {{{1
namespace test35 {
// Helgrind 3.3.0 has very slow in shadow_mem_make_NoAccess(). Fixed locally.
// With the fix helgrind runs this test about a minute.
// Without the fix -- about 5 minutes. (on c2d 2.4GHz).
//
// TODO: need to figure out the best way for performance testing.
int **ARR;
const int N_mu   = 25000;
const int N_free = 48000;

void Worker() {
  for (int i = 0; i < N_free; i++)
    CHECK(777 == *ARR[i]);
}

void Run() {
  printf("test35:\n");
  std::vector<Mutex*> mus;

  ARR = new int *[N_free];
  for (int i = 0; i < N_free; i++) {
    const int c = N_free / N_mu;
    if ((i % c) == 0) {
      mus.push_back(new Mutex);
      mus.back()->Lock();
      mus.back()->Unlock();
    }
    ARR[i] = new int(777);
  }

  // Need to put all ARR[i] into shared state in order
  // to trigger the performance bug.
  MyThreadArray t(Worker, Worker);
  t.Start();
  t.Join();

  for (int i = 0; i < N_free; i++) delete ARR[i];
  delete [] ARR;

  for (size_t i = 0; i < mus.size(); i++) {
    delete mus[i];
  }
}
REGISTER_TEST2(Run, 35, PERFORMANCE|EXCLUDE_FROM_ALL);
}  // namespace test35


// test36: TN. Synchronization via Mutex, then PCQ. 3 threads. W/W {{{1
namespace test36 {
// variation of test28 (W/W instead of W/R)

// Putter1:                       Getter:                         Putter2:
// 1. MU.Lock();                                                  A. MU.Lock()
// 2. write(GLOB)                                                 B. write(GLOB)
// 3. MU.Unlock()                                                 C. MU.Unlock()
// 4. Q.Put() ---------\                                 /------- D. Q.Put()
// 5. MU1.Lock()        \-------> a. Q.Get()            /         E. MU1.Lock()
// 6. MU.Lock()                   b. Q.Get() <---------/          F. MU.Lock()
// 7. write(GLOB)                                                 G. write(GLOB)
// 8. MU.Unlock()                                                 H. MU.Unlock()
// 9. MU1.Unlock()                  (sleep)                       I. MU1.Unlock()
//                                c. MU1.Lock()
//                                d. write(GLOB)
//                                e. MU1.Unlock()
ProducerConsumerQueue Q(INT_MAX);
int     GLOB = 0;
Mutex   MU, MU1;

void Putter() {
  MU.Lock();
  GLOB++;
  MU.Unlock();

  Q.Put(NULL);

  MU1.Lock();
  MU.Lock();
  GLOB++;
  MU.Unlock();
  MU1.Unlock();
}

void Getter() {
  Q.Get();
  Q.Get();
  usleep(100000);
  MU1.Lock();
  GLOB++;
  MU1.Unlock();
}

void Run() {
  printf("test36: negative \n");
  MyThreadArray t(Getter, Putter, Putter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 36);
}  // namespace test36


// test37: TN. Simple synchronization (write vs read). {{{1
namespace test37 {
int     GLOB = 0;
Mutex   MU;
// Similar to test10, but properly locked.
// Writer:             Reader:
// 1. MU.Lock()
// 2. write
// 3. MU.Unlock()
//                    a. MU.Lock()
//                    b. read
//                    c. MU.Unlock();

void Writer() {
  MU.Lock();
  GLOB = 3;
  MU.Unlock();
}
void Reader() {
  usleep(100000);
  MU.Lock();
  CHECK(GLOB != -777);
  MU.Unlock();
}

void Run() {
  printf("test37: negative\n");
  MyThreadArray t(Writer, Reader);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 37);
}  // namespace test37


// test38: TN. Synchronization via Mutexes and PCQ. 4 threads. W/W {{{1
namespace test38 {
// Fusion of test29 and test36.

// Putter1:            Putter2:           Getter1:       Getter2:
//    MU1.Lock()          MU1.Lock()
//    write(GLOB)         write(GLOB)
//    MU1.Unlock()        MU1.Unlock()
//    Q1.Put()            Q2.Put()
//    Q1.Put()            Q2.Put()
//    MU1.Lock()          MU1.Lock()
//    MU2.Lock()          MU2.Lock()
//    write(GLOB)         write(GLOB)
//    MU2.Unlock()        MU2.Unlock()
//    MU1.Unlock()        MU1.Unlock()     sleep          sleep
//                                         Q1.Get()       Q1.Get()
//                                         Q2.Get()       Q2.Get()
//                                         MU2.Lock()     MU2.Lock()
//                                         write(GLOB)    write(GLOB)
//                                         MU2.Unlock()   MU2.Unlock()
//


ProducerConsumerQueue *Q1, *Q2;
int     GLOB = 0;
Mutex   MU, MU1, MU2;

void Putter(ProducerConsumerQueue *q) {
  MU1.Lock();
  GLOB++;
  MU1.Unlock();

  q->Put(NULL);
  q->Put(NULL);

  MU1.Lock();
  MU2.Lock();
  GLOB++;
  MU2.Unlock();
  MU1.Unlock();

}

void Putter1() { Putter(Q1); }
void Putter2() { Putter(Q2); }

void Getter() {
  usleep(100000);
  Q1->Get();
  Q2->Get();

  MU2.Lock();
  GLOB++;
  MU2.Unlock();

  usleep(48000); //  TODO: remove this when FP in test32 is fixed.
}

void Run() {
  printf("test38: negative\n");
  Q1 = new ProducerConsumerQueue(INT_MAX);
  Q2 = new ProducerConsumerQueue(INT_MAX);
  MyThreadArray t(Getter, Getter, Putter1, Putter2);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
  delete Q1;
  delete Q2;
}
REGISTER_TEST(Run, 38);
}  // namespace test38

namespace NegativeTests_Barrier {  // {{{1
#ifndef NO_BARRIER
// Same as test17 but uses Barrier class (pthread_barrier_t).
int     GLOB = 0;
const int N_threads = 3;
Barrier barrier(N_threads);
Mutex   MU;

void Worker() {
  MU.Lock();
  GLOB++;
  MU.Unlock();
  barrier.Block();
  CHECK(GLOB == N_threads);
}

TEST(NegativeTests, Barrier) {
  ANNOTATE_TRACE_MEMORY(&GLOB);
  {
    ThreadPool pool(N_threads);
    pool.StartWorkers();
    for (int i = 0; i < N_threads; i++) {
      pool.Add(NewCallback(Worker));
    }
  } // all folks are joined here.
  CHECK(GLOB == 3);
}
#endif // NO_BARRIER
}  // namespace test39


// test40: FP. Synchronization via Mutexes and PCQ. 4 threads. W/W {{{1
namespace test40 {
// Similar to test38 but with different order of events (due to sleep).

// Putter1:            Putter2:           Getter1:       Getter2:
//    MU1.Lock()          MU1.Lock()
//    write(GLOB)         write(GLOB)
//    MU1.Unlock()        MU1.Unlock()
//    Q1.Put()            Q2.Put()
//    Q1.Put()            Q2.Put()
//                                        Q1.Get()       Q1.Get()
//                                        Q2.Get()       Q2.Get()
//                                        MU2.Lock()     MU2.Lock()
//                                        write(GLOB)    write(GLOB)
//                                        MU2.Unlock()   MU2.Unlock()
//
//    MU1.Lock()          MU1.Lock()
//    MU2.Lock()          MU2.Lock()
//    write(GLOB)         write(GLOB)
//    MU2.Unlock()        MU2.Unlock()
//    MU1.Unlock()        MU1.Unlock()


ProducerConsumerQueue *Q1, *Q2;
int     GLOB = 0;
Mutex   MU, MU1, MU2;

void Putter(ProducerConsumerQueue *q) {
  MU1.Lock();
  GLOB++;
  MU1.Unlock();

  q->Put(NULL);
  q->Put(NULL);
  usleep(100000);

  MU1.Lock();
  MU2.Lock();
  GLOB++;
  MU2.Unlock();
  MU1.Unlock();

}

void Putter1() { Putter(Q1); }
void Putter2() { Putter(Q2); }

void Getter() {
  Q1->Get();
  Q2->Get();

  MU2.Lock();
  GLOB++;
  MU2.Unlock();

  usleep(48000); //  TODO: remove this when FP in test32 is fixed.
}

void Run() {
//  ANNOTATE_EXPECT_RACE(&GLOB, "test40. FP. Fixed by MSMProp1. Complex Stuff.");
  printf("test40: negative\n");
  Q1 = new ProducerConsumerQueue(INT_MAX);
  Q2 = new ProducerConsumerQueue(INT_MAX);
  MyThreadArray t(Getter, Getter, Putter1, Putter2);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
  delete Q1;
  delete Q2;
}
REGISTER_TEST(Run, 40);
}  // namespace test40

// test41: TN. Test for race that appears when loading a dynamic symbol. {{{1
namespace test41 {
void Worker() {
  ANNOTATE_NO_OP(NULL); // An empty function, loaded from dll.
}
void Run() {
  printf("test41: negative\n");
  MyThreadArray t(Worker, Worker, Worker);
  t.Start();
  t.Join();
}
REGISTER_TEST2(Run, 41, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test41


// test42: TN. Using the same cond var several times. {{{1
namespace test42 {
int GLOB = 0;
int COND = 0;
int N_threads = 3;
Mutex   MU;

void Worker1() {
  GLOB=1;

  MU.Lock();
  COND = 1;
  CV.Signal();
  MU.Unlock();

  MU.Lock();
  while (COND != 0)
    CV.Wait(&MU);
  ANNOTATE_CONDVAR_LOCK_WAIT(&CV, &MU);
  MU.Unlock();

  GLOB=3;

}

void Worker2() {

  MU.Lock();
  while (COND != 1)
    CV.Wait(&MU);
  ANNOTATE_CONDVAR_LOCK_WAIT(&CV, &MU);
  MU.Unlock();

  GLOB=2;

  MU.Lock();
  COND = 0;
  CV.Signal();
  MU.Unlock();

}

void Run() {
//  ANNOTATE_EXPECT_RACE(&GLOB, "test42. TN. debugging.");
  printf("test42: negative\n");
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 42, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test42



// test43: TN. {{{1
namespace test43 {
//
// Putter:            Getter:
// 1. write
// 2. Q.Put() --\     .
// 3. read       \--> a. Q.Get()
//                    b. read
int     GLOB = 0;
ProducerConsumerQueue Q(INT_MAX);
void Putter() {
  GLOB = 1;
  Q.Put(NULL);
  CHECK(GLOB == 1);
}
void Getter() {
  Q.Get();
  usleep(100000);
  CHECK(GLOB == 1);
}
void Run() {
  printf("test43: negative\n");
  MyThreadArray t(Putter, Getter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 43)
}  // namespace test43


// test44: FP. {{{1
namespace test44 {
//
// Putter:            Getter:
// 1. read
// 2. Q.Put() --\     .
// 3. MU.Lock()  \--> a. Q.Get()
// 4. write
// 5. MU.Unlock()
//                    b. MU.Lock()
//                    c. write
//                    d. MU.Unlock();
int     GLOB = 0;
Mutex   MU;
ProducerConsumerQueue Q(INT_MAX);
void Putter() {
  CHECK(GLOB == 0);
  Q.Put(NULL);
  MU.Lock();
  GLOB = 1;
  MU.Unlock();
}
void Getter() {
  Q.Get();
  usleep(100000);
  MU.Lock();
  GLOB = 1;
  MU.Unlock();
}
void Run() {
//  ANNOTATE_EXPECT_RACE(&GLOB, "test44. FP. Fixed by MSMProp1.");
  printf("test44: negative\n");
  MyThreadArray t(Putter, Getter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 44)
}  // namespace test44


// test45: TN. {{{1
namespace test45 {
//
// Putter:            Getter:
// 1. read
// 2. Q.Put() --\     .
// 3. MU.Lock()  \--> a. Q.Get()
// 4. write
// 5. MU.Unlock()
//                    b. MU.Lock()
//                    c. read
//                    d. MU.Unlock();
int     GLOB = 0;
Mutex   MU;
ProducerConsumerQueue Q(INT_MAX);
void Putter() {
  CHECK(GLOB == 0);
  Q.Put(NULL);
  MU.Lock();
  GLOB++;
  MU.Unlock();
}
void Getter() {
  Q.Get();
  usleep(100000);
  MU.Lock();
  CHECK(GLOB <= 1);
  MU.Unlock();
}
void Run() {
  printf("test45: negative\n");
  MyThreadArray t(Putter, Getter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 45)
}  // namespace test45


// test46: FN. {{{1
namespace test46 {
//
// First:                             Second:
// 1. write
// 2. MU.Lock()
// 3. write
// 4. MU.Unlock()                      (sleep)
//                                    a. MU.Lock()
//                                    b. write
//                                    c. MU.Unlock();
int     GLOB = 0;
Mutex   MU;
void First() {
  GLOB++;
  MU.Lock();
  GLOB++;
  MU.Unlock();
}
void Second() {
  usleep(480000);
  MU.Lock();
  GLOB++;
  MU.Unlock();

  // just a print.
  // If we move it to Run()  we will get report in MSMHelgrind
  // due to its false positive (test32).
  MU.Lock();
  printf("\tGLOB=%d\n", GLOB);
  MU.Unlock();
}
void Run() {
  ANNOTATE_TRACE_MEMORY(&GLOB);
  MyThreadArray t(First, Second);
  t.Start();
  t.Join();
}
REGISTER_TEST(Run, 46)
}  // namespace test46


// test47: TP. Not detected by pure happens-before detectors. {{{1
namespace test47 {
// A true race that can not be detected by a pure happens-before
// race detector.
//
// First:                             Second:
// 1. write
// 2. MU.Lock()
// 3. MU.Unlock()                      (sleep)
//                                    a. MU.Lock()
//                                    b. MU.Unlock();
//                                    c. write
int     GLOB = 0;
Mutex   MU;
void First() {
  GLOB=1;
  MU.Lock();
  MU.Unlock();
}
void Second() {
  usleep(480000);
  MU.Lock();
  MU.Unlock();
  GLOB++;
}
void Run() {
  if (!Tsan_PureHappensBefore())
    ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test47. TP. Not detected by pure HB.");
  printf("test47: positive\n");
  MyThreadArray t(First, Second);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 47)
}  // namespace test47


// test48: FN. Simple race (single write vs multiple reads). {{{1
namespace test48 {
int     GLOB = 0;
// same as test10 but with single writer and  multiple readers
// A simple data race between single writer and  multiple readers.
// Write happens before Reads (enforced by sleep(1)),

//
// Writer:                    Readers:
// 1. write(GLOB)             a. sleep(long enough so that GLOB
//                                is most likely initialized by Writer)
//                            b. read(GLOB)
//
//
// Eraser algorithm does not detect the race here,
// see Section 2.2 of http://citeseer.ist.psu.edu/savage97eraser.html.
//
void Writer() {
  GLOB = 3;
}
void Reader() {
  usleep(100000);
  CHECK(GLOB != -777);
}

void Run() {
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test48. TP. FN in MSMHelgrind.");
  printf("test48: positive\n");
  MyThreadArray t(Writer, Reader,Reader,Reader);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 48)
}  // namespace test48


// test49: FN. Simple race (single write vs multiple reads). {{{1
namespace test49 {
int     GLOB = 0;
// same as test10 but with multiple read operations done by a single reader
// A simple data race between writer and readers.
// Write happens before Read (enforced by sleep(1)),
//
// Writer:                    Reader:
// 1. write(GLOB)             a. sleep(long enough so that GLOB
//                                is most likely initialized by Writer)
//                            b. read(GLOB)
//                            c. read(GLOB)
//                            d. read(GLOB)
//                            e. read(GLOB)
//
//
// Eraser algorithm does not detect the race here,
// see Section 2.2 of http://citeseer.ist.psu.edu/savage97eraser.html.
//
void Writer() {
  GLOB = 3;
}
void Reader() {
  usleep(100000);
  CHECK(GLOB != -777);
  CHECK(GLOB != -777);
  CHECK(GLOB != -777);
  CHECK(GLOB != -777);
}

void Run() {
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test49. TP. FN in MSMHelgrind.");
  printf("test49: positive\n");
  MyThreadArray t(Writer, Reader);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 49);
}  // namespace test49


// test50: TP. Synchronization via CondVar. {{{1
namespace test50 {
int     GLOB = 0;
Mutex   MU;
// Two last write accesses to GLOB are not synchronized
//
// Waiter:                      Waker:
// 1. COND = 0
// 2. Start(Waker)
// 3. MU.Lock()                 a. write(GLOB)
//                              b. MU.Lock()
//                              c. COND = 1
//                         /--- d. CV.Signal()
//  4. while(COND != 1)   /     e. MU.Unlock()
//       CV.Wait(MU) <---/
//  5. MU.Unlock()
//  6. write(GLOB)              f. MU.Lock()
//                              g. write(GLOB)
//                              h. MU.Unlock()


void Waker() {
  usleep(100000);  // Make sure the waiter blocks.

  GLOB = 1;

  MU.Lock();
  COND = 1;
  CV.Signal();
  MU.Unlock();

  usleep(100000);
  MU.Lock();
  GLOB = 3;
  MU.Unlock();
}

void Waiter() {
  MU.Lock();
  while(COND != 1)
    CV.Wait(&MU);
  ANNOTATE_CONDVAR_LOCK_WAIT(&CV, &MU);
  MU.Unlock();

  GLOB = 2;
}
void Run() {
  printf("test50: positive\n");
  COND = 0;
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test50. TP.");
  MyThreadArray t(Waker, Waiter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 50, FEATURE|NEEDS_ANNOTATIONS);
}  // namespace test50


// test51: TP. Synchronization via CondVar: problem with several signals. {{{1
namespace test51 {
int     GLOB = 0;
int     COND = 0;
Mutex   MU;
StealthNotification n1, n2;

// scheduler dependent results because of several signals
// second signal will be lost
//
// Waiter:                      Waker:
// 1. Start(Waker)
// 2. MU.Lock()
// 3. while(COND)
//       CV.Wait(MU)<-\         .
// 4. MU.Unlock()      \        .
// 5. write(GLOB)       \       a. write(GLOB)
//                       \      b. MU.Lock()
//                        \     c. COND = 1
//                         \--- d. CV.Signal()
//                              e. MU.Unlock()
//
//                              f. write(GLOB)
//
//                              g. MU.Lock()
//                              h. COND = 1
//                    LOST<---- i. CV.Signal()
//                              j. MU.Unlock()

void Waker() {
  n1.wait();  // Make sure the waiter blocks.

  GLOB = 1;

  MU.Lock();
  COND = 1;
  CV.Signal();
  MU.Unlock();

  n2.wait();  // Make sure the waiter continued.

  GLOB = 2;

  MU.Lock();
  COND = 1;
  CV.Signal();   //Lost Signal
  MU.Unlock();
}

void Waiter() {
  MU.Lock();
  n1.signal();  // Ready to get the first signal.
  while(COND != 1)
    CV.Wait(&MU);
  MU.Unlock();

  GLOB = 3;
  n2.signal(); // Ready to miss the second signal.
}
void Run() {
  ANNOTATE_EXPECT_RACE(&GLOB, "test51. TP.");
  printf("test51: positive\n");
  MyThreadArray t(Waiter, Waker);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 51);
}  // namespace test51


// test52: TP. Synchronization via CondVar: problem with several signals. {{{1
namespace test52 {
int     GLOB = 0;
int     COND = 0;
Mutex   MU;
StealthNotification n1, n2;

// same as test51 but the first signal will be lost
// scheduler dependent results because of several signals
//
// Waiter:                      Waker:
// 1. Start(Waker)
//                              a. write(GLOB)
//                              b. MU.Lock()
//                              c. COND = 1
//                    LOST<---- d. CV.Signal()
//                              e. MU.Unlock()
//
// 2. MU.Lock()
// 3. while(COND)
//       CV.Wait(MU)<-\         .
// 4. MU.Unlock()      \        f. write(GLOB)
// 5. write(GLOB)       \       .
//                       \      g. MU.Lock()
//                        \     h. COND = 1
//                         \--- i. CV.Signal()
//                              j. MU.Unlock()

void Waker() {

  GLOB = 1;

  MU.Lock();
  COND = 1;
  CV.Signal();    //lost signal
  MU.Unlock();

  n1.signal();  // Ok, now we may block.
  n2.wait();    // We blocked.

  GLOB = 2;

  MU.Lock();
  COND = 1;
  CV.Signal();
  MU.Unlock();
}

void Waiter() {
  n1.wait();  // The first signal is lost.

  MU.Lock();
  n2.signal(); // The 2-nd signal may go.
  while(COND != 1)
    CV.Wait(&MU);
  MU.Unlock();

  GLOB = 3;
}
void Run() {
  printf("test52: positive\n");
  ANNOTATE_EXPECT_RACE(&GLOB, "test52. TP.");
  MyThreadArray t(Waker, Waiter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 52);
}  // namespace test52


// test53: FP. Synchronization via implicit semaphore. {{{1
namespace test53 {
// Correctly synchronized test, but the common lockset is empty.
// The variable FLAG works as an implicit semaphore.
// MSMHelgrind still does not complain since it does not maintain the lockset
// at the exclusive state. But MSMProp1 does complain.
// See also test54.
//
//
// Initializer:                  Users
// 1. MU1.Lock()
// 2. write(GLOB)
// 3. FLAG = true
// 4. MU1.Unlock()
//                               a. MU1.Lock()
//                               b. f = FLAG;
//                               c. MU1.Unlock()
//                               d. if (!f) goto a.
//                               e. MU2.Lock()
//                               f. write(GLOB)
//                               g. MU2.Unlock()
//

int     GLOB = 0;
bool    FLAG = false;
Mutex   MU1, MU2;

void Initializer() {
  MU1.Lock();
  GLOB = 1000;
  FLAG = true;
  MU1.Unlock();
  usleep(100000); // just in case
}

void User() {
  bool f = false;
  while(!f) {
    MU1.Lock();
    f = FLAG;
    MU1.Unlock();
    usleep(10000);
  }
  // at this point Initializer will not access GLOB again
  MU2.Lock();
  CHECK(GLOB >= 1000);
  GLOB++;
  MU2.Unlock();
}

void Run() {
  if (!Tsan_PureHappensBefore())
    ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test53. FP. Implicit semaphore");
  printf("test53: FP. false positive, Implicit semaphore\n");
  MyThreadArray t(Initializer, User, User);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 53)
}  // namespace test53


// test54: TN. Synchronization via implicit semaphore. Annotated {{{1
namespace test54 {
// Same as test53, but annotated.
int     GLOB = 0;
bool    FLAG = false;
Mutex   MU1, MU2;

void Initializer() {
  MU1.Lock();
  GLOB = 1000;
  FLAG = true;
  ANNOTATE_CONDVAR_SIGNAL(&GLOB);
  MU1.Unlock();
  usleep(100000); // just in case
}

void User() {
  bool f = false;
  while(!f) {
    MU1.Lock();
    f = FLAG;
    MU1.Unlock();
    usleep(10000);
  }
  // at this point Initializer will not access GLOB again
  ANNOTATE_CONDVAR_WAIT(&GLOB);
  MU2.Lock();
  CHECK(GLOB >= 1000);
  GLOB++;
  MU2.Unlock();
}

void Run() {
  printf("test54: negative\n");
  MyThreadArray t(Initializer, User, User);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 54, FEATURE|NEEDS_ANNOTATIONS)
}  // namespace test54


// test55: FP. Synchronization with TryLock. Not easy for race detectors {{{1
namespace test55 {
// "Correct" synchronization with TryLock and Lock.
//
// This scheme is actually very risky.
// It is covered in detail in this video:
// http://youtube.com/watch?v=mrvAqvtWYb4 (slide 36, near 50-th minute).
int     GLOB = 0;
Mutex   MU;

void Worker_Lock() {
  GLOB = 1;
  MU.Lock();
}

void Worker_TryLock() {
  while (true) {
    if (!MU.TryLock()) {
      MU.Unlock();
      break;
    }
    else
      MU.Unlock();
    usleep(100);
  }
  GLOB = 2;
}

void Run() {
  printf("test55:\n");
  MyThreadArray t(Worker_Lock, Worker_TryLock);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 55, FEATURE|EXCLUDE_FROM_ALL);
}  // namespace test55



// test56: TP. Use of ANNOTATE_BENIGN_RACE. {{{1
namespace test56 {
// For whatever reason the user wants to treat
// a race on GLOB as a benign race.
int     GLOB = 0;
int     GLOB2 = 0;

void Worker() {
  GLOB++;
}

void Run() {
  ANNOTATE_BENIGN_RACE(&GLOB, "test56. Use of ANNOTATE_BENIGN_RACE.");
  ANNOTATE_BENIGN_RACE(&GLOB2, "No race. The tool should be silent");
  printf("test56: positive\n");
  MyThreadArray t(Worker, Worker, Worker, Worker);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 56, FEATURE|NEEDS_ANNOTATIONS)
}  // namespace test56


// test57: TN: Correct use of atomics. {{{1
namespace test57 {
int     GLOB = 0;
void Writer() {
  for (int i = 0; i < 10; i++) {
    AtomicIncrement(&GLOB, 1);
    usleep(1000);
  }
}
void Reader() {
  while (GLOB < 20) usleep(1000);
}
void Run() {
  printf("test57: negative\n");
  MyThreadArray t(Writer, Writer, Reader, Reader);
  t.Start();
  t.Join();
  CHECK(GLOB == 20);
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 57)
}  // namespace test57


// test58: TN. User defined synchronization. {{{1
namespace test58 {
int     GLOB1 = 1;
int     GLOB2 = 2;
int     FLAG1 = 0;
int     FLAG2 = 0;

// Correctly synchronized test, but the common lockset is empty.
// The variables FLAG1 and FLAG2 used for synchronization and as
// temporary variables for swapping two global values.
// Such kind of synchronization is rarely used (Excluded from all tests??).

void Worker2() {
  FLAG1=GLOB2;

  while(!FLAG2)
    ;
  GLOB2=FLAG2;
}

void Worker1() {
  FLAG2=GLOB1;

  while(!FLAG1)
    ;
  GLOB1=FLAG1;
}

void Run() {
  printf("test58:\n");
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
  printf("\tGLOB1=%d\n", GLOB1);
  printf("\tGLOB2=%d\n", GLOB2);
}
REGISTER_TEST2(Run, 58, FEATURE|EXCLUDE_FROM_ALL)
}  // namespace test58



// test59: TN. User defined synchronization. Annotated {{{1
namespace test59 {
int     COND1 = 0;
int     COND2 = 0;
int     GLOB1 = 1;
int     GLOB2 = 2;
int     FLAG1 = 0;
int     FLAG2 = 0;
// same as test 58 but annotated

void Worker2() {
  FLAG1=GLOB2;
  ANNOTATE_CONDVAR_SIGNAL(&COND2);
  while(!FLAG2) usleep(1);
  ANNOTATE_CONDVAR_WAIT(&COND1);
  GLOB2=FLAG2;
}

void Worker1() {
  FLAG2=GLOB1;
  ANNOTATE_CONDVAR_SIGNAL(&COND1);
  while(!FLAG1) usleep(1);
  ANNOTATE_CONDVAR_WAIT(&COND2);
  GLOB1=FLAG1;
}

void Run() {
  printf("test59: negative\n");
  ANNOTATE_BENIGN_RACE(&FLAG1, "synchronization via 'safe' race");
  ANNOTATE_BENIGN_RACE(&FLAG2, "synchronization via 'safe' race");
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
  printf("\tGLOB1=%d\n", GLOB1);
  printf("\tGLOB2=%d\n", GLOB2);
}
REGISTER_TEST2(Run, 59, FEATURE|NEEDS_ANNOTATIONS)
}  // namespace test59


// test60: TN. Correct synchronization using signal-wait {{{1
namespace test60 {
int     COND1 = 0;
int     COND2 = 0;
int     GLOB1 = 1;
int     GLOB2 = 2;
int     FLAG2 = 0;
int     FLAG1 = 0;
Mutex   MU;
// same as test 59 but synchronized with signal-wait.

void Worker2() {
  FLAG1=GLOB2;

  MU.Lock();
  COND1 = 1;
  CV.Signal();
  MU.Unlock();

  MU.Lock();
  while(COND2 != 1)
    CV.Wait(&MU);
  ANNOTATE_CONDVAR_LOCK_WAIT(&CV, &MU);
  MU.Unlock();

  GLOB2=FLAG2;
}

void Worker1() {
  FLAG2=GLOB1;

  MU.Lock();
  COND2 = 1;
  CV.Signal();
  MU.Unlock();

  MU.Lock();
  while(COND1 != 1)
    CV.Wait(&MU);
  ANNOTATE_CONDVAR_LOCK_WAIT(&CV, &MU);
  MU.Unlock();

  GLOB1=FLAG1;
}

void Run() {
  printf("test60: negative\n");
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
  printf("\tGLOB1=%d\n", GLOB1);
  printf("\tGLOB2=%d\n", GLOB2);
}
REGISTER_TEST2(Run, 60, FEATURE|NEEDS_ANNOTATIONS)
}  // namespace test60


// test61: TN. Synchronization via Mutex as in happens-before, annotated. {{{1
namespace test61 {
Mutex MU;
int     GLOB = 0;
int     *P1 = NULL, *P2 = NULL;

// In this test Mutex lock/unlock operations introduce happens-before relation.
// We annotate the code so that MU is treated as in pure happens-before detector.


void Putter() {
  ANNOTATE_PURE_HAPPENS_BEFORE_MUTEX(&MU);
  MU.Lock();
  if (P1 == NULL) {
    P1 = &GLOB;
    *P1 = 1;
  }
  MU.Unlock();
}

void Getter() {
  bool done  = false;
  while (!done) {
    MU.Lock();
    if (P1) {
      done = true;
      P2 = P1;
      P1 = NULL;
    }
    MU.Unlock();
  }
  *P2 = 2;
}


void Run() {
  printf("test61: negative\n");
  MyThreadArray t(Putter, Getter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 61, FEATURE|NEEDS_ANNOTATIONS)
}  // namespace test61


// test62: STAB. Create as many segments as possible. {{{1
namespace test62 {
// Helgrind 3.3.0 will fail as it has a hard limit of < 2^24 segments.
// A better scheme is to implement garbage collection for segments.
ProducerConsumerQueue Q(INT_MAX);
const int N = 1 << 22;

void Putter() {
  for (int i = 0; i < N; i++){
    if ((i % (N / 8)) == 0) {
      printf("i=%d\n", i);
    }
    Q.Put(NULL);
  }
}

void Getter() {
  for (int i = 0; i < N; i++)
    Q.Get();
}

void Run() {
  printf("test62:\n");
  MyThreadArray t(Putter, Getter);
  t.Start();
  t.Join();
}
REGISTER_TEST2(Run, 62, STABILITY|EXCLUDE_FROM_ALL)
}  // namespace test62


// test63: STAB. Create as many segments as possible and do it fast. {{{1
namespace test63 {
// Helgrind 3.3.0 will fail as it has a hard limit of < 2^24 segments.
// A better scheme is to implement garbage collection for segments.
const int N = 1 << 24;
int C = 0;

void Putter() {
  for (int i = 0; i < N; i++){
    if ((i % (N / 8)) == 0) {
      printf("i=%d\n", i);
    }
    ANNOTATE_CONDVAR_SIGNAL(&C);
  }
}

void Getter() {
}

void Run() {
  printf("test63:\n");
  MyThreadArray t(Putter, Getter);
  t.Start();
  t.Join();
}
REGISTER_TEST2(Run, 63, STABILITY|EXCLUDE_FROM_ALL)
}  // namespace test63


// test64: TP. T2 happens-before T3, but T1 is independent. Reads in T1/T2. {{{1
namespace test64 {
// True race between T1 and T3:
//
// T1:                   T2:                   T3:
// 1. read(GLOB)         (sleep)
//                       a. read(GLOB)
//                       b. Q.Put() ----->    A. Q.Get()
//                                            B. write(GLOB)
//
//

int     GLOB = 0;
ProducerConsumerQueue Q(INT_MAX);

void T1() {
  CHECK(GLOB == 0);
}

void T2() {
  usleep(100000);
  CHECK(GLOB == 0);
  Q.Put(NULL);
}

void T3() {
  Q.Get();
  GLOB = 1;
}


void Run() {
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test64: TP.");
  printf("test64: positive\n");
  MyThreadArray t(T1, T2, T3);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 64)
}  // namespace test64


// test65: TP. T2 happens-before T3, but T1 is independent. Writes in T1/T2. {{{1
namespace test65 {
// Similar to test64.
// True race between T1 and T3:
//
// T1:                   T2:                   T3:
// 1. MU.Lock()
// 2. write(GLOB)
// 3. MU.Unlock()         (sleep)
//                       a. MU.Lock()
//                       b. write(GLOB)
//                       c. MU.Unlock()
//                       d. Q.Put() ----->    A. Q.Get()
//                                            B. write(GLOB)
//
//

int     GLOB = 0;
Mutex   MU;
ProducerConsumerQueue Q(INT_MAX);

void T1() {
  MU.Lock();
  GLOB++;
  MU.Unlock();
}

void T2() {
  usleep(100000);
  MU.Lock();
  GLOB++;
  MU.Unlock();
  Q.Put(NULL);
}

void T3() {
  Q.Get();
  GLOB = 1;
}


void Run() {
  if (!Tsan_PureHappensBefore())
    ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test65. TP.");
  printf("test65: positive\n");
  MyThreadArray t(T1, T2, T3);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 65)
}  // namespace test65


// test66: TN. Two separate pairs of signaller/waiter using the same CV. {{{1
namespace test66 {
int     GLOB1 = 0;
int     GLOB2 = 0;
int     C1 = 0;
int     C2 = 0;
Mutex   MU;

void Signaller1() {
  GLOB1 = 1;
  MU.Lock();
  C1 = 1;
  CV.Signal();
  MU.Unlock();
}

void Signaller2() {
  GLOB2 = 1;
  usleep(100000);
  MU.Lock();
  C2 = 1;
  CV.Signal();
  MU.Unlock();
}

void Waiter1() {
  MU.Lock();
  while (C1 != 1) CV.Wait(&MU);
  ANNOTATE_CONDVAR_WAIT(&CV);
  MU.Unlock();
  GLOB1 = 2;
}

void Waiter2() {
  MU.Lock();
  while (C2 != 1) CV.Wait(&MU);
  ANNOTATE_CONDVAR_WAIT(&CV);
  MU.Unlock();
  GLOB2 = 2;
}

void Run() {
  printf("test66: negative\n");
  MyThreadArray t(Signaller1, Signaller2, Waiter1, Waiter2);
  t.Start();
  t.Join();
  printf("\tGLOB=%d/%d\n", GLOB1, GLOB2);
}
REGISTER_TEST2(Run, 66, FEATURE|NEEDS_ANNOTATIONS)
}  // namespace test66


// test67: FN. Race between Signaller1 and Waiter2 {{{1
namespace test67 {
// Similar to test66, but there is a real race here.
//
// Here we create a happens-before arc between Signaller1 and Waiter2
// even though there should be no such arc.
// However, it's probably improssible (or just very hard) to avoid it.
int     GLOB = 0;
int     C1 = 0;
int     C2 = 0;
Mutex   MU;

void Signaller1() {
  GLOB = 1;
  MU.Lock();
  C1 = 1;
  CV.Signal();
  MU.Unlock();
}

void Signaller2() {
  usleep(100000);
  MU.Lock();
  C2 = 1;
  CV.Signal();
  MU.Unlock();
}

void Waiter1() {
  MU.Lock();
  while (C1 != 1) CV.Wait(&MU);
  ANNOTATE_CONDVAR_WAIT(&CV);
  MU.Unlock();
}

void Waiter2() {
  MU.Lock();
  while (C2 != 1) CV.Wait(&MU);
  ANNOTATE_CONDVAR_WAIT(&CV);
  MU.Unlock();
  GLOB = 2;
}

void Run() {
  ANNOTATE_EXPECT_RACE(&GLOB, "test67. FN. Race between Signaller1 and Waiter2");
  printf("test67: positive\n");
  MyThreadArray t(Signaller1, Signaller2, Waiter1, Waiter2);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 67, FEATURE|NEEDS_ANNOTATIONS|EXCLUDE_FROM_ALL)
}  // namespace test67


// test68: TP. Writes are protected by MU, reads are not. {{{1
namespace test68 {
// In this test, all writes to GLOB are protected by a mutex
// but some reads go unprotected.
// This is certainly a race, but in some cases such code could occur in
// a correct program. For example, the unprotected reads may be used
// for showing statistics and are not required to be precise.
int     GLOB = 0;
int     COND = 0;
const int N_writers = 3;
Mutex MU, MU1;

void Writer() {
  for (int i = 0; i < 100; i++) {
    MU.Lock();
    GLOB++;
    MU.Unlock();
  }

  // we are done
  MU1.Lock();
  COND++;
  MU1.Unlock();
}

void Reader() {
  bool cont = true;
  while (cont) {
    CHECK(GLOB >= 0);

    // are we done?
    MU1.Lock();
    if (COND == N_writers)
      cont = false;
    MU1.Unlock();
    usleep(100);
  }
}

void Run() {
  ANNOTATE_EXPECT_RACE(&GLOB, "TP. Writes are protected, reads are not.");
  printf("test68: positive\n");
  MyThreadArray t(Reader, Writer, Writer, Writer);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 68)
}  // namespace test68


// test69:  {{{1
namespace test69 {
// This is the same as test68, but annotated.
// We do not want to annotate GLOB as a benign race
// because we want to allow racy reads only in certain places.
//
// TODO:
int     GLOB = 0;
int     COND = 0;
const int N_writers = 3;
int     FAKE_MU = 0;
Mutex MU, MU1;

void Writer() {
  for (int i = 0; i < 10; i++) {
    MU.Lock();
    GLOB++;
    MU.Unlock();
  }

  // we are done
  MU1.Lock();
  COND++;
  MU1.Unlock();
}

void Reader() {
  bool cont = true;
  while (cont) {
    ANNOTATE_IGNORE_READS_BEGIN();
    CHECK(GLOB >= 0);
    ANNOTATE_IGNORE_READS_END();

    // are we done?
    MU1.Lock();
    if (COND == N_writers)
      cont = false;
    MU1.Unlock();
    usleep(100);
  }
}

void Run() {
  printf("test69: negative\n");
  MyThreadArray t(Reader, Writer, Writer, Writer);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 69)
}  // namespace test69

// test70: STAB. Check that TRACE_MEMORY works. {{{1
namespace test70 {
int     GLOB = 0;
void Run() {
  printf("test70: negative\n");
  ANNOTATE_TRACE_MEMORY(&GLOB);
  GLOB = 1;
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 70)
}  // namespace test70



namespace NegativeTests_Strlen {  // {{{1
// This test is a reproducer for a benign race in strlen (as well as index, etc).
// Some implementations of strlen may read up to 7 bytes past the end of the string
// thus touching memory which may not belong to this string.
// Such race is benign because the data read past the end of the string is not used.
//
// Here, we allocate a 8-byte aligned string str and initialize first 5 bytes.
// Then one thread calls strlen(str) (as well as index & rindex)
// and another thread initializes str[5]..str[7].
//
// This can be fixed in Helgrind by intercepting strlen and replacing it
// with a simpler implementation.

char    *str;
char    *tmp2;
void WorkerX() {
  usleep(100000);
  ASSERT_TRUE(strlen(str) == 4);
#ifndef WIN32
  EXPECT_TRUE(index(str, 'X') == str);
  EXPECT_TRUE(index(str, 'x') == str+1);
  EXPECT_TRUE(index(str, 'Y') == NULL);
#ifndef ANDROID
  EXPECT_TRUE(rindex(str, 'X') == str+2);
  EXPECT_TRUE(rindex(str, 'x') == str+3);
  EXPECT_TRUE(rindex(str, 'Y') == NULL);
#endif
#else
  EXPECT_TRUE(lstrlenA(NULL) == 0);
  EXPECT_TRUE(lstrlenW(NULL) == 0);
#endif
  EXPECT_TRUE(strchr(str, 'X') == str);
  EXPECT_TRUE(strchr(str, 'x') == str+1);
  EXPECT_TRUE(strchr(str, 'Y') == NULL);
  EXPECT_TRUE(memchr(str, 'X', 8) == str);
  EXPECT_TRUE(memchr(str, 'x', 8) == str+1);
  char tmp[100] = "Zzz";
  EXPECT_TRUE(memmove(tmp, str, strlen(str) + 1) == tmp);
  EXPECT_TRUE(strcmp(tmp,str) == 0);
  EXPECT_TRUE(strncmp(tmp,str, 4) == 0);
  EXPECT_TRUE(memmove(str, tmp, strlen(tmp) + 1) == str);
#ifndef WIN32
#ifndef ANDROID
  EXPECT_TRUE(stpcpy(tmp2, str) == tmp2+4);
#endif
  EXPECT_TRUE(strcpy(tmp2, str) == tmp2);
  EXPECT_TRUE(strncpy(tmp, str, 4) == tmp);
  // These may not be properly intercepted since gcc -O1 may inline
  // strcpy/stpcpy in presence of a statically sized array. Damn.
  // EXPECT_TRUE(stpcpy(tmp, str) == tmp+4);
  // EXPECT_TRUE(strcpy(tmp, str) == tmp);
#endif
  EXPECT_TRUE(strrchr(str, 'X') == str+2);
  EXPECT_TRUE(strrchr(str, 'x') == str+3);
  EXPECT_TRUE(strrchr(str, 'Y') == NULL);
}
void WorkerY() {
  str[5] = 'Y';
  str[6] = 'Y';
  str[7] = '\0';
}

TEST(NegativeTests, StrlenAndFriends) {
  str = new char[8];
  tmp2 = new char[8];
  str[0] = 'X';
  str[1] = 'x';
  str[2] = 'X';
  str[3] = 'x';
  str[4] = '\0';
  MyThread t1(WorkerY);
  MyThread t2(WorkerX);
  t1.Start();
  t2.Start();
  t1.Join();
  t2.Join();
  ASSERT_STREQ("XxXx", str);
  ASSERT_STREQ("YY", str+5);

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
  delete [] str;
  delete [] tmp2;
  // TODO(kcc): add more tests to check that interceptors are correct.
}
}  // namespace test71

namespace NegativeTests_EmptyRep {  // {{{1
void Worker() {
  string s;
  s.erase();
}

TEST(NegativeTests, DISABLED_EmptyRepTest) {
  // This is a test for the reports on an internal race in std::string implementation.
  // See http://gcc.gnu.org/bugzilla/show_bug.cgi?id=40518
  // ThreadSanitizer should be silent on this, but currently it is silent
  // only on Valgrind/Linux.
  MyThreadArray mta(Worker, Worker);
  mta.Start();
  mta.Join();
}
}  //namespace NegativeTests_EmptyRep

namespace NegativeTests_StdStringDtor {  // {{{1
// Some implementations of std::string (including the one on Linux)
// are unfriendly to race detectors since they use atomic reference counting
// in a way that race detectors can not understand.
//
// See http://code.google.com/p/data-race-test/issues/detail?id=40
string *s = NULL;
BlockingCounter *counter = NULL;

void DestroyWorker() {
  string x = *s;  // force string copy (increments ref count).
  counter->DecrementCount();
  // x is destructed, ref count is decremented.
}

void AssignWorker() {
  string x = *s;  // force string copy (increments ref count).
  counter->DecrementCount();
  // x is assigned, the ref count is decremented.
  usleep(100000);
  x = "ZZZ";
}

TEST(NegativeTests, StdStringDtorVsDtor) {
  MyThreadArray mta(DestroyWorker, DestroyWorker, DestroyWorker);
  counter = new BlockingCounter(3);
  s = new string ("foo");
  mta.Start();

  counter->Wait();

  delete s;  // ref count becomes zero and the object is destroyed.
  mta.Join();
  delete counter;
}

TEST(NegativeTests, DISABLED_StdStringDtorVsAssign) {
  MyThreadArray mta(AssignWorker, AssignWorker, AssignWorker);
  counter = new BlockingCounter(3);
  s = new string ("foo");
  mta.Start();

  counter->Wait();

  delete s;  // ref count becomes zero and the object is destroyed.
  mta.Join();
  delete counter;
}
}  //namespace NegativeTests_EmptyRep

namespace PositiveTests_MutexDtorNoSync {
// Check that Mutex::~Mutex() doesn't introduce h-b arcs.
int *GLOB = NULL;

void WriteThenScopedLocalMutex() {
  *GLOB = 1;
  {
    Mutex l;
  }
}

void ScopedLocalMutexThenWrite() {
  {
    Mutex l;
  }
  *GLOB = 2;
}

TEST(PositiveTests, MutexDtorNoSyncTest) {
  GLOB = new int(0);
  ANNOTATE_EXPECT_RACE(GLOB, "TP: PositiveTests.MutexDtorNoSyncTest");
  MyThreadArray t(WriteThenScopedLocalMutex,
                  ScopedLocalMutexThenWrite);
  t.Start();
  t.Join();
  delete GLOB;
}

void WriteThenScopedLocalMutexLockUnlock() {
  *GLOB = 1;
  {
    Mutex l;
    l.Lock();
    l.Unlock();
  }
}

void ScopedLocalMutexLockUnlockThenWrite() {
  {
    Mutex l;
    l.Lock();
    l.Unlock();
  }
  *GLOB = 2;
}

TEST(PositiveTests, MutexDtorNoSyncTest2) {
  GLOB = new int(0);
  ANNOTATE_EXPECT_RACE(GLOB, "TP: PositiveTests.MutexDtorNoSyncTest2");
  MyThreadArray t(WriteThenScopedLocalMutexLockUnlock,
                  ScopedLocalMutexLockUnlockThenWrite);
  t.Start();
  t.Join();
  delete GLOB;
}

}  // namespace PositiveTests_MutexDtorSync

namespace PositiveTests_FprintfThreadCreateTest {
// Check that fprintf doesn't introduce h-b with the start of the
// following thread
int *GLOB;
StealthNotification *n;

void Worker1() {
  *GLOB = 1;
  fprintf(stdout, "Hello, world!\n");
  n->signal();
}

void Worker2() {
  *GLOB = 2;
}

#if !defined(_MSC_VER)
// TODO(timurrrr): investigate Windows FN and un-#if
TEST(PositiveTests, FprintfThreadCreateTest) {
  GLOB = new int;
  ANNOTATE_EXPECT_RACE(GLOB, "TP: PositiveTests.FprintfThreadCreateTest");
  n = new StealthNotification;
  MyThread t1(Worker1);
  t1.Start();
  n->wait();
  MyThread t2(Worker2);
  t2.Start();
  t2.Join();
  t1.Join();
  delete n;
  delete GLOB;
}
#endif

} // namespace PositiveTests_FprintfThreadCreateTest

// test72: STAB. Stress test for the number of segment sets (SSETs). {{{1
namespace test72 {
#ifndef NO_BARRIER
// Variation of test33.
// Instead of creating Nlog*N_iter threads,
// we create Nlog threads and do N_iter barriers.
int     GLOB = 0;
const int N_iter = 30;
const int Nlog  = 16;
const int N     = 1 << Nlog;
static int64_t ARR1[N];
static int64_t ARR2[N];
Barrier *barriers[N_iter];
Mutex   MU;

void Worker() {
  MU.Lock();
  int n = ++GLOB;
  MU.Unlock();

  n %= Nlog;

  for (int it = 0; it < N_iter; it++) {
    // Iterate N_iter times, block on barrier after each iteration.
    // This way Helgrind will create new segments after each barrier.

    for (int x = 0; x < 2; x++) {
      // run the inner loop twice.
      // When a memory location is accessed second time it is likely
      // that the state (SVal) will be unchanged.
      // The memory machine may optimize this case.
      for (int i = 0; i < N; i++) {
        // ARR1[i] and ARR2[N-1-i] are accessed by threads from i-th subset
        if (i & (1 << n)) {
          CHECK(ARR1[i] == 0);
          CHECK(ARR2[N-1-i] == 0);
        }
      }
    }
    barriers[it]->Block();
  }
}


void Run() {
  printf("test72:\n");

  std::vector<MyThread*> vec(Nlog);

  for (int i = 0; i < N_iter; i++)
    barriers[i] = new Barrier(Nlog);

  // Create and start Nlog threads
  for (int i = 0; i < Nlog; i++) {
    vec[i] = new MyThread(Worker);
    vec[i]->Start();
  }

  // Join all threads.
  for (int i = 0; i < Nlog; i++) {
    vec[i]->Join();
    delete vec[i];
  }
  for (int i = 0; i < N_iter; i++)
    delete barriers[i];

  /*printf("\tGLOB=%d; ARR[1]=%d; ARR[7]=%d; ARR[N-1]=%d\n",
         GLOB, (int)ARR1[1], (int)ARR1[7], (int)ARR1[N-1]);*/
}
REGISTER_TEST2(Run, 72, STABILITY|PERFORMANCE|EXCLUDE_FROM_ALL);
#endif // NO_BARRIER
}  // namespace test72


// test73: STAB. Stress test for the number of (SSETs), different access sizes. {{{1
namespace test73 {
#ifndef NO_BARRIER
// Variation of test72.
// We perform accesses of different sizes to the same location.
int     GLOB = 0;
const int N_iter = 2;
const int Nlog  = 16;
const int N     = 1 << Nlog;
static int64_t ARR1[N];
static int ARR2[N];
Barrier *barriers[N_iter];
Mutex   MU;

void Worker() {
  MU.Lock();
  int n = ++GLOB;
  MU.Unlock();

  n %= Nlog;

  for (int it = 0; it < N_iter; it++) {
    // Iterate N_iter times, block on barrier after each iteration.
    // This way Helgrind will create new segments after each barrier.

    for (int x = 0; x < 4; x++) {
      for (int i = 0; i < N; i++) {
        // ARR1[i] are accessed by threads from i-th subset
        if (i & (1 << n)) {
          for (int off = 0; off < (1 << x); off++) {
            switch(x) {
              case 0: CHECK(          ARR1  [i * (1<<x) + off] == 0); break;
              case 1: CHECK(((int*)  (ARR1))[i * (1<<x) + off] == 0); break;
              case 2: CHECK(((short*)(ARR1))[i * (1<<x) + off] == 0); break;
              case 3: CHECK(((char*) (ARR1))[i * (1<<x) + off] == 0); break;
            }
            switch(x) {
              case 1: CHECK(((int*)  (ARR2))[i * (1<<x) + off] == 0); break;
              case 2: CHECK(((short*)(ARR2))[i * (1<<x) + off] == 0); break;
              case 3: CHECK(((char*) (ARR2))[i * (1<<x) + off] == 0); break;
            }
          }
        }
      }
    }
    barriers[it]->Block();
  }
}



void Run() {
  printf("test73:\n");

  std::vector<MyThread*> vec(Nlog);

  for (int i = 0; i < N_iter; i++)
    barriers[i] = new Barrier(Nlog);

  // Create and start Nlog threads
  for (int i = 0; i < Nlog; i++) {
    vec[i] = new MyThread(Worker);
    vec[i]->Start();
  }

  // Join all threads.
  for (int i = 0; i < Nlog; i++) {
    vec[i]->Join();
    delete vec[i];
  }
  for (int i = 0; i < N_iter; i++)
    delete barriers[i];

  /*printf("\tGLOB=%d; ARR[1]=%d; ARR[7]=%d; ARR[N-1]=%d\n",
         GLOB, (int)ARR1[1], (int)ARR1[7], (int)ARR1[N-1]);*/
}
REGISTER_TEST2(Run, 73, STABILITY|PERFORMANCE|EXCLUDE_FROM_ALL);
#endif // NO_BARRIER
}  // namespace test73


// test74: PERF. A lot of lock/unlock calls. {{{1
namespace    test74 {
const int N = 100000;
Mutex   MU;
TEST(StressTests, ManyLocksUnlocks) {
  for (int i = 0; i < N; i++ ) {
    MU.Lock();
    MU.Unlock();
  }
}
}  // namespace test74

// RefCountedClass {{{1
struct RefCountedClass {
 public:
  RefCountedClass() {
    annotate_unref_ = false;
    ref_ = 0;
    data_ = 0;
  }

  ~RefCountedClass() {
    CHECK(ref_ == 0);     // race may be reported here
    int data_val = data_; // and here
                          // if MU is not annotated
    data_ = 0;
    ref_ = -1;
    printf("\tRefCountedClass::data_ = %d\n", data_val);
  }

  void AccessData() {
    this->mu_.Lock();
    this->data_++;
    this->mu_.Unlock();
  }

  void Ref() {
    MU.Lock();
    CHECK(ref_ >= 0);
    ref_++;
    MU.Unlock();
  }

  void Unref() {
    MU.Lock();
    CHECK(ref_ > 0);
    ref_--;
    bool do_delete = ref_ == 0;
    if (annotate_unref_) {
      ANNOTATE_HAPPENS_BEFORE(this);
    }
    MU.Unlock();
    if (do_delete) {
      if (annotate_unref_) {
        ANNOTATE_HAPPENS_AFTER(this);
      }
      delete this;
    }
  }

  static void Annotate_MU() {
    ANNOTATE_PURE_HAPPENS_BEFORE_MUTEX(&MU);
  }
  void AnnotateUnref() {
    annotate_unref_ = true;
  }
  void Annotate_Race() {
    ANNOTATE_BENIGN_RACE_SIZED(this, sizeof(*this), "needs annotation");
  }
 private:
  bool annotate_unref_;

  int data_;
  Mutex mu_; // protects data_

  int ref_;
  static Mutex MU; // protects ref_
};

Mutex RefCountedClass::MU;

// test76: FP. Ref counting, no annotations. {{{1
namespace test76 {
#ifndef NO_BARRIER
int     GLOB = 0;
Barrier barrier(4);
RefCountedClass *object = NULL;
void Worker() {
  object->Ref();
  barrier.Block();
  object->AccessData();
  object->Unref();
}
void Run() {
  printf("test76: false positive (ref counting)\n");
  object = new RefCountedClass;
  object->Annotate_Race();
  MyThreadArray t(Worker, Worker, Worker, Worker);
  t.Start();
  t.Join();
}
REGISTER_TEST2(Run, 76, FEATURE)
#endif // NO_BARRIER
}  // namespace test76



// test77: TN. Ref counting, MU is annotated. {{{1
namespace test77 {
#ifndef NO_BARRIER
// same as test76, but RefCountedClass::MU is annotated.
int     GLOB = 0;
Barrier barrier(4);
RefCountedClass *object = NULL;
void Worker() {
  object->Ref();
  barrier.Block();
  object->AccessData();
  object->Unref();
}
void Run() {
  printf("test77: true negative (ref counting), mutex is annotated\n");
  RefCountedClass::Annotate_MU();
  object = new RefCountedClass;
  MyThreadArray t(Worker, Worker, Worker, Worker);
  t.Start();
  t.Join();
}
REGISTER_TEST(Run, 77)
#endif // NO_BARRIER
}  // namespace test77



// test78: TN. Ref counting, Unref is annotated. {{{1
namespace test78 {
#ifndef NO_BARRIER
// same as test76, but RefCountedClass::Unref is annotated.
int     GLOB = 0;
Barrier barrier(4);
RefCountedClass *object = NULL;
void Worker() {
  object->Ref();
  barrier.Block();
  object->AccessData();
  object->Unref();
}
void Run() {
  printf("test78: true negative (ref counting), Unref is annotated\n");
  RefCountedClass::Annotate_MU();
  object = new RefCountedClass;
  MyThreadArray t(Worker, Worker, Worker, Worker);
  t.Start();
  t.Join();
}
REGISTER_TEST(Run, 78)
#endif // NO_BARRIER
}  // namespace test78



// test79 TN. Swap. {{{1
namespace test79 {
#if 0
typedef __gnu_cxx::hash_map<int, int> map_t;
#else
typedef std::map<int, int> map_t;
#endif
map_t   MAP;
Mutex   MU;

// Here we use swap to pass MAP between threads.
// The synchronization is correct, but w/o ANNOTATE_PURE_HAPPENS_BEFORE_MUTEX
// Helgrind will complain.

void Worker1() {
  map_t tmp;
  MU.Lock();
  // We swap the new empty map 'tmp' with 'MAP'.
  MAP.swap(tmp);
  MU.Unlock();
  // tmp (which is the old version of MAP) is destroyed here.
}

void Worker2() {
  MU.Lock();
  MAP[1]++;  // Just update MAP under MU.
  MU.Unlock();
}

void Worker3() { Worker1(); }
void Worker4() { Worker2(); }

void Run() {
  ANNOTATE_PURE_HAPPENS_BEFORE_MUTEX(&MU);
  printf("test79: negative\n");
  MyThreadArray t(Worker1, Worker2, Worker3, Worker4);
  t.Start();
  t.Join();
}
REGISTER_TEST(Run, 79)
}  // namespace test79


// AtomicRefCountedClass. {{{1
// Same as RefCountedClass, but using atomic ops instead of mutex.
struct AtomicRefCountedClass {
 public:
  AtomicRefCountedClass() {
    annotate_unref_ = false;
    ref_ = 0;
    data_ = 0;
  }

  ~AtomicRefCountedClass() {
    CHECK(ref_ == 0);     // race may be reported here
    int data_val = data_; // and here
    data_ = 0;
    ref_ = -1;
    printf("\tRefCountedClass::data_ = %d\n", data_val);
  }

  void AccessData() {
    this->mu_.Lock();
    this->data_++;
    this->mu_.Unlock();
  }

  void Ref() {
    AtomicIncrement(&ref_, 1);
  }

  void Unref() {
    // DISCLAIMER: I am not sure I've implemented this correctly
    // (might require some memory barrier, etc).
    // But this implementation of reference counting is enough for
    // the purpose of Helgrind demonstration.
    AtomicIncrement(&ref_, -1);
    if (annotate_unref_) { ANNOTATE_HAPPENS_BEFORE(this); }
    if (ref_ == 0) {
      if (annotate_unref_) { ANNOTATE_HAPPENS_AFTER(this); }
      delete this;
    }
  }

  void AnnotateUnref() {
    annotate_unref_ = true;
  }
  void Annotate_Race() {
    ANNOTATE_BENIGN_RACE(&this->data_, "needs annotation");
  }
 private:
  bool annotate_unref_;

  Mutex mu_;
  int data_; // under mu_

  int ref_;  // used in atomic ops.
};

// test80: FP. Ref counting with atomics, no annotations. {{{1
namespace test80 {
#ifndef NO_BARRIER
int     GLOB = 0;
Barrier barrier(4);
AtomicRefCountedClass *object = NULL;
void Worker() {
  object->Ref();
  barrier.Block();
  object->AccessData();
  object->Unref(); // All the tricky stuff is here.
}
void Run() {
  printf("test80: false positive (ref counting)\n");
  object = new AtomicRefCountedClass;
  object->Annotate_Race();
  MyThreadArray t(Worker, Worker, Worker, Worker);
  t.Start();
  t.Join();
}
REGISTER_TEST2(Run, 80, FEATURE|EXCLUDE_FROM_ALL)
#endif // NO_BARRIER
}  // namespace test80


// test81: TN. Ref counting with atomics, Unref is annotated. {{{1
namespace test81 {
#ifndef NO_BARRIER
// same as test80, but Unref is annotated.
int     GLOB = 0;
Barrier barrier(4);
AtomicRefCountedClass *object = NULL;
void Worker() {
  object->Ref();
  barrier.Block();
  object->AccessData();
  object->Unref(); // All the tricky stuff is here.
}
void Run() {
  printf("test81: negative (annotated ref counting)\n");
  object = new AtomicRefCountedClass;
  object->AnnotateUnref();
  MyThreadArray t(Worker, Worker, Worker, Worker);
  t.Start();
  t.Join();
}
REGISTER_TEST2(Run, 81, FEATURE|EXCLUDE_FROM_ALL)
#endif // NO_BARRIER
}  // namespace test81


// test82: Object published w/o synchronization. {{{1
namespace test82 {

// Writer creates a new object and makes the pointer visible to the Reader.
// Reader waits until the object pointer is non-null and reads the object.
//
// On Core 2 Duo this test will sometimes (quite rarely) fail in
// the CHECK below, at least if compiled with -O2.
//
// The sequence of events::
// Thread1:                  Thread2:
//   a. arr_[...] = ...
//   b. foo[i]    = ...
//                           A. ... = foo[i]; // non NULL
//                           B. ... = arr_[...];
//
//  Since there is no proper synchronization, during the even (B)
//  Thread2 may not see the result of the event (a).
//  On x86 and x86_64 this happens due to compiler reordering instructions.
//  On other arcitectures it may also happen due to cashe inconsistency.

class FOO {
 public:
  FOO() {
    idx_ = rand() % 1024;
    arr_[idx_] = 77777;
  //   __asm__ __volatile__("" : : : "memory"); // this fixes!
  }
  static void check(volatile FOO *foo) {
    CHECK(foo->arr_[foo->idx_] == 77777);
  }
 private:
  int idx_;
  int arr_[1024];
};

const int N = 100000;
static volatile FOO *foo[N];
Mutex   MU;

void Writer() {
  for (int i = 0; i < N; i++) {
    foo[i] = new FOO;
    usleep(100);
  }
}

void Reader() {
  for (int i = 0; i < N; i++) {
    while (!foo[i]) {
      MU.Lock();   // this is NOT a synchronization,
      MU.Unlock(); // it just helps foo[i] to become visible in Reader.
    }
    if ((i % 100) == 0) {
      printf("rd %d\n", i);
    }
    // At this point Reader() sees the new value of foo[i]
    // but in very rare cases will not see the new value of foo[i]->arr_.
    // Thus this CHECK will sometimes fail.
    FOO::check(foo[i]);
  }
}

void Run() {
  printf("test82: positive\n");
  MyThreadArray t(Writer, Reader);
  t.Start();
  t.Join();
}
REGISTER_TEST2(Run, 82, FEATURE|EXCLUDE_FROM_ALL)
}  // namespace test82


// test83: Object published w/o synchronization (simple version){{{1
namespace test83 {
// A simplified version of test83 (example of a wrong code).
// This test, though incorrect, will almost never fail.
volatile static int *ptr = NULL;
Mutex   MU;

void Writer() {
  usleep(100);
  ptr = new int(777);
}

void Reader() {
  while(!ptr) {
    MU.Lock(); // Not a synchronization!
    MU.Unlock();
  }
  CHECK(*ptr == 777);
}

void Run() {
//  printf("test83: positive\n");
  MyThreadArray t(Writer, Reader);
  t.Start();
  t.Join();
}
REGISTER_TEST2(Run, 83, FEATURE|EXCLUDE_FROM_ALL)
}  // namespace test83


// test84: TP. True race (regression test for a bug related to atomics){{{1
namespace test84 {
// Helgrind should not create HB arcs for the bus lock even when
// --pure-happens-before=yes is used.
// Bug found in by Bart Van Assche, the test is taken from
// valgrind file drd/tests/atomic_var.c.
static int s_x = 0;
/* s_dummy[] ensures that s_x and s_y are not in the same cache line. */
static char s_dummy[512] = {0};
static int s_y;

void thread_func_1()
{
  s_y = 1;
  AtomicIncrement(&s_x, 1);
}

void thread_func_2()
{
  while (AtomicIncrement(&s_x, 0) == 0)
    ;
  printf("y = %d\n", s_y);
}


void Run() {
  CHECK(s_dummy[0] == 0);  // Avoid compiler warning about 's_dummy unused'.
  printf("test84: positive\n");
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&s_y, "test84: TP. true race.");
  MyThreadArray t(thread_func_1, thread_func_2);
  t.Start();
  t.Join();
}
REGISTER_TEST(Run, 84)
}  // namespace test84


// Test for RunningOnValgrind(). {{{1
TEST(NegativeTests, RunningOnValgrindTest) {
  printf("RunningOnValgrind() = %d\n", RunningOnValgrind());
}

namespace NegativeTests_BenignRaceInDtor {  // {{{
// Test for race inside DTOR: racey write to vptr. Benign.
// This test shows a racey access to vptr (the pointer to vtbl).
// We have class A and class B derived from A.
// Both classes have a virtual function f() and a virtual DTOR.
// We create an object 'A *a = new B'
// and pass this object from Thread1 to Thread2.
// Thread2 calls a->f(). This call reads a->vtpr.
// Thread1 deletes the object. B::~B waits untill the object can be destroyed
// (flag_stopped == true) but at the very beginning of B::~B
// a->vptr is written to.
// So, we have a race on a->vptr.
// On this particular test this race is benign, but HarmfulRaceInDtor shows
// how such race could harm.
//
//
//
// Threa1:                                            Thread2:
// 1. A a* = new B;
// 2. Q.Put(a); ------------\                         .
//                           \-------------------->   a. a = Q.Get();
//                                                    b. a->f();
//                                       /---------   c. flag_stopped = true;
// 3. delete a;                         /
//    waits untill flag_stopped <------/
//    inside the dtor
//

bool flag_stopped = false;
Mutex mu;

ProducerConsumerQueue Q(INT_MAX);  // Used to pass A* between threads.

struct A {
  A()  { printf("A::A()\n"); }
  virtual ~A() { printf("A::~A()\n"); }
  virtual void f() { }

  uintptr_t padding[15];
} ALIGNED(64);

struct B: A {
  B()  { printf("B::B()\n"); }
  virtual ~B() {
    // The race is here.    <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    printf("B::~B()\n");
    // wait until flag_stopped is true.
    mu.LockWhen(Condition(&ArgIsTrue, &flag_stopped));
    mu.Unlock();
    printf("B::~B() done\n");
  }
  virtual void f() { }
};

void Waiter() {
  A *a = new B;
  printf("Waiter: B created\n");
  Q.Put(a);
  usleep(100000); // so that Worker calls a->f() first.
  printf("Waiter: deleting B\n");
  delete a;
  printf("Waiter: B deleted\n");
  usleep(100000);
  printf("Waiter: done\n");
}

void Worker() {
  A *a = reinterpret_cast<A*>(Q.Get());
  printf("Worker: got A\n");
  a->f();

  mu.Lock();
  flag_stopped = true;
  mu.Unlock();
  usleep(200000);
  printf("Worker: done\n");
}

TEST(NegativeTests, BenignRaceInDtor) {
  MyThreadArray t(Waiter, Worker);
  t.Start();
  t.Join();
}
}  // namespace


namespace PositiveTests_HarmfulRaceInDtor {  // {{{
// A variation of BenignRaceInDtor where the race is harmful.
// Race on vptr. Will run A::F() or B::F() depending on the timing.
class A {
 public:
  A() : done_(false) {
    // W/o this annotation tsan may produce additional warnings in hybrid mode.
    ANNOTATE_PURE_HAPPENS_BEFORE_MUTEX(&mu_);
  }
  virtual void F() {
    printf ("A::F()\n");
  }
  void Done() {
    MutexLock lock(&mu_);
    done_ = true;
  }
  virtual ~A() {
    while (true) {
      {
        MutexLock lock(&mu_);
        if (done_) break;
      }
      usleep(10);  // yield.
    }
  }
 private:
  Mutex mu_;
  bool  done_;
};

class B : public A {
 public:
  virtual void F() {
    // TODO(kcc): enable this printf back once issue 57 is fixed.
    // printf ("B::F()\n");
  }
};

static A *a;

void Thread1() {
  a->F();
  a->Done();
  sleep(1);
};

void Thread2() {
  delete a;
}
TEST(PositiveTests, HarmfulRaceInDtorB) {
  ANNOTATE_FLUSH_EXPECTED_RACES();
  // Will print B::F()
  a = new B;
  ANNOTATE_EXPECT_RACE(a, "HarmfulRaceInDtor #1: expected race on a->vptr");
  ANNOTATE_TRACE_MEMORY(a);
  MyThreadArray t(Thread1, Thread2);
  t.Start();
  t.Join();
  ANNOTATE_FLUSH_EXPECTED_RACES();
}

TEST(PositiveTests, HarmfulRaceInDtorA) {
  ANNOTATE_FLUSH_EXPECTED_RACES();
  // Will print A::F()
  a = new B;
  ANNOTATE_EXPECT_RACE(a, "HarmfulRaceInDtor #2: expected race on a->vptr");
  ANNOTATE_TRACE_MEMORY(a);
  MyThreadArray t(Thread2, Thread1);
  t.Start();
  t.Join();
  ANNOTATE_FLUSH_EXPECTED_RACES();
}

}  // namespace


namespace AnnotateIgnoreTests {  // {{{1

int racey_write = 0;

void RaceyWriter() {
  ANNOTATE_IGNORE_WRITES_BEGIN();
  racey_write = 1;
  ANNOTATE_IGNORE_WRITES_END();
}

TEST(NegativeTests, AnnotateIgnoreWritesTest) {
  MyThread t(RaceyWriter);
  t.Start();
  racey_write = 1;
  t.Join();
}

int racey_read = 0;

void RaceyReader1() {
  ANNOTATE_IGNORE_READS_BEGIN();
  CHECK(racey_read != 777);
  ANNOTATE_IGNORE_READS_END();
}

void RaceyReader2() {
  CHECK(ANNOTATE_UNPROTECTED_READ(racey_read) != 777);
}

TEST(NegativeTests, AnnotateIgnoreReadsTest) {
  MyThreadArray t(RaceyReader1, RaceyReader2);
  t.Start();
  racey_read = 1;
  t.Join();
}

int incorrectly_annotated_racey_write = 0;

void IncorrectlyAnnotatedRaceyWriter() {
  ANNOTATE_IGNORE_READS_BEGIN();
  incorrectly_annotated_racey_write = 1;
  ANNOTATE_IGNORE_READS_END();
}

TEST(PositiveTests, AnnotateIgnoreReadsOnWriteTest) {
  ANNOTATE_EXPECT_RACE(&incorrectly_annotated_racey_write, "expected race");
  MyThread t(IncorrectlyAnnotatedRaceyWriter);
  t.Start();
  incorrectly_annotated_racey_write = 1;
  t.Join();
  ANNOTATE_FLUSH_EXPECTED_RACES();
}

int incorrectly_annotated_racey_read = 0;

void IncorrectlyAnnotatedRaceyReader() {
  ANNOTATE_IGNORE_WRITES_BEGIN();
  CHECK(incorrectly_annotated_racey_read != 777);
  ANNOTATE_IGNORE_WRITES_END();
}

TEST(PositiveTests, AnnotateIgnoreWritesOnReadTest) {
  ANNOTATE_EXPECT_RACE(&incorrectly_annotated_racey_read, "expected race");
  MyThread t(IncorrectlyAnnotatedRaceyReader);
  t.Start();
  incorrectly_annotated_racey_read = 1;
  t.Join();
  ANNOTATE_FLUSH_EXPECTED_RACES();
}

}  // namespace


// test89: Test for debug info. {{{1
namespace test89 {
// Simlpe races with different objects (stack, heap globals; scalars, structs).
// Also, if run with --trace-level=2 this test will show a sequence of
// CTOR and DTOR calls.
struct STRUCT {
  int a, b, c;
};

struct A {
  int a;
  A() {
    ANNOTATE_TRACE_MEMORY(&a);
    a = 1;
  }
  virtual ~A() {
    a = 4;
  }
};

struct B : A {
  B()  { CHECK(a == 1); }
  virtual ~B() { CHECK(a == 3); }
};
struct C : B {
  C()  { a = 2; }
  virtual ~C() { a = 3; }
};

int            GLOBAL = 0;
int           *STACK  = 0;
STRUCT         GLOB_STRUCT;
STRUCT        *STACK_STRUCT;
STRUCT        *HEAP_STRUCT;

void Worker() {
  GLOBAL = 1;
  *STACK = 1;
  GLOB_STRUCT.b   = 1;
  STACK_STRUCT->b = 1;
  HEAP_STRUCT->b  = 1;
}

void Run() {
  int stack_var = 0;
  STACK = &stack_var;

  STRUCT stack_struct;
  STACK_STRUCT = &stack_struct;

  HEAP_STRUCT = new STRUCT;

  printf("test89: negative\n");
  MyThreadArray t(Worker, Worker);
  t.Start();
  t.Join();

  delete HEAP_STRUCT;

  A *a = new C;
  printf("Using 'a->a':  %d\n", a->a);
  delete a;
}
REGISTER_TEST2(Run, 89, FEATURE|EXCLUDE_FROM_ALL)
}  // namespace test89


// test90: FP. Test for a safely-published pointer (read-only). {{{1
namespace test90 {
// The Publisher creates an object and safely publishes it under a mutex.
// Readers access the object read-only.
// See also test91.
//
// Without annotations Helgrind will issue a false positive in Reader().
//
// Choices for annotations:
//   -- ANNOTATE_CONDVAR_SIGNAL/ANNOTATE_CONDVAR_WAIT
//   -- ANNOTATE_PURE_HAPPENS_BEFORE_MUTEX
//   -- ANNOTATE_PUBLISH_MEMORY_RANGE.

int     *GLOB = 0;
Mutex   MU;

StealthNotification n1;

void Publisher() {
  MU.Lock();
  GLOB = (int*)malloc(128 * sizeof(int));
  ANNOTATE_TRACE_MEMORY(&GLOB[42]);
  GLOB[42] = 777;
  if (!Tsan_PureHappensBefore())
    ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB[42], "test90. FP. This is a false positve");
  MU.Unlock();
  n1.signal();
  usleep(200000);
}

void Reader() {
  n1.wait();
  while (true) {
    MU.Lock();
    int *p = &GLOB[42];
    MU.Unlock();
    if (p) {
      CHECK(*p == 777);  // Race is reported here.
      break;
    }
  }
}

void Run() {
  printf("test90: false positive (safely published pointer).\n");
  MyThreadArray t(Publisher, Reader, Reader, Reader);
  t.Start();
  t.Join();
  free(GLOB);
}
REGISTER_TEST(Run, 90)
}  // namespace test90


// test91: FP. Test for a safely-published pointer (read-write). {{{1
namespace test91 {
// Similar to test90.
// The Publisher creates an object and safely publishes it under a mutex MU1.
// Accessors get the object under MU1 and access it (read/write) under MU2.
//
// Without annotations Helgrind will issue a false positive in Accessor().
//

int     *GLOB = 0;
Mutex   MU, MU1, MU2;

void Publisher() {
  MU1.Lock();
  GLOB = (int*)malloc(128 * sizeof(int));
  GLOB[42] = 777;
  if (!Tsan_PureHappensBefore())
    ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB[42], "test91. FP. This is a false positve");
  MU1.Unlock();
}

void Accessor() {
  usleep(10000);
  while (true) {
    MU1.Lock();
    int *p = &GLOB[42];
    MU1.Unlock();
    if (p) {
      MU2.Lock();
      (*p)++;  // Race is reported here.
      CHECK(*p >  777);
      MU2.Unlock();
      break;
    }
  }
}

void Run() {
  printf("test91: false positive (safely published pointer, read/write).\n");
  MyThreadArray t(Publisher, Accessor, Accessor, Accessor);
  t.Start();
  t.Join();
  free(GLOB);
}
REGISTER_TEST(Run, 91)
}  // namespace test91


// test92: TN. Test for a safely-published pointer (read-write), annotated. {{{1
namespace test92 {
// Similar to test91, but annotated with ANNOTATE_PUBLISH_MEMORY_RANGE.
//
//
// Publisher:                                       Accessors:
//
// 1. MU1.Lock()
// 2. Create GLOB.
// 3. ANNOTATE_PUBLISH_...(GLOB) -------\            .
// 4. MU1.Unlock()                       \           .
//                                        \          a. MU1.Lock()
//                                         \         b. Get GLOB
//                                          \        c. MU1.Unlock()
//                                           \-->    d. Access GLOB
//
//  A happens-before arc is created between ANNOTATE_PUBLISH_MEMORY_RANGE and
//  accesses to GLOB.

struct ObjType {
  int arr[10];
};

ObjType *GLOB = 0;
Mutex   MU, MU1, MU2;

void Publisher() {
  MU1.Lock();
  GLOB = new ObjType;
  for (int i = 0; i < 10; i++) {
    GLOB->arr[i] = 777;
  }
  // This annotation should go right before the object is published.
  ANNOTATE_PUBLISH_MEMORY_RANGE(GLOB, sizeof(*GLOB));
  MU1.Unlock();
}

void Accessor(int index) {
  while (true) {
    MU1.Lock();
    ObjType *p = GLOB;
    MU1.Unlock();
    if (p) {
      MU2.Lock();
      p->arr[index]++;  // W/o the annotations the race will be reported here.
      CHECK(p->arr[index] ==  778);
      MU2.Unlock();
      break;
    }
  }
}

void Accessor0() { Accessor(0); }
void Accessor5() { Accessor(5); }
void Accessor9() { Accessor(9); }

void Run() {
  printf("test92: safely published pointer, read/write, annotated.\n");
  MyThreadArray t(Publisher, Accessor0, Accessor5, Accessor9);
  t.Start();
  t.Join();
  printf("\t*GLOB=%d\n", GLOB->arr[0]);
}
REGISTER_TEST(Run, 92)
}  // namespace test92


// test93: TP. Test for incorrect usage of ANNOTATE_PUBLISH_MEMORY_RANGE. {{{1
namespace test93 {
int     GLOB = 0;

void Reader() {
  CHECK(GLOB == 0);
}

void Publisher() {
  usleep(10000);
  // Incorrect, used after the memory has been accessed in another thread.
  ANNOTATE_PUBLISH_MEMORY_RANGE(&GLOB, sizeof(GLOB));
}

void Run() {
  printf("test93: positive, misuse of ANNOTATE_PUBLISH_MEMORY_RANGE\n");
  MyThreadArray t(Reader, Publisher);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 93, FEATURE|EXCLUDE_FROM_ALL)
}  // namespace test93


// test94: TP. Check do_cv_signal/fake segment logic {{{1
namespace test94 {
int     GLOB;

int COND  = 0;
int COND2 = 0;
Mutex MU, MU2;
CondVar CV, CV2;

StealthNotification n1, n2, n3;

void Thr1() {

  n2.wait();  // Make sure the waiter blocks.
  GLOB = 1; // WRITE

  MU.Lock();
  COND = 1;
  CV.Signal();
  MU.Unlock();
  n1.signal();
}
void Thr2() {
  // Make sure CV2.Signal() "happens after" CV.Signal()
  n1.wait();
  // Make sure the waiter blocks.
  n3.wait();

  MU2.Lock();
  COND2 = 1;
  CV2.Signal();
  MU2.Unlock();
}
void Thr3() {
  MU.Lock();
  n2.signal();
  while(COND != 1)
    CV.Wait(&MU);
  MU.Unlock();
}
void Thr4() {
  MU2.Lock();
  n3.signal();
  while(COND2 != 1)
    CV2.Wait(&MU2);
  MU2.Unlock();
  GLOB = 2; // READ: no HB-relation between CV.Signal and CV2.Wait !
}
void Run() {
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test94: TP.");
  printf("test94: TP. Check do_cv_signal/fake segment logic\n");
  MyThreadArray mta(Thr1, Thr2, Thr3, Thr4);
  mta.Start();
  mta.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 94);
}  // namespace test94

// test95: TP. Check do_cv_signal/fake segment logic {{{1
namespace test95 {
int     GLOB = 0;

int COND  = 0;
int COND2 = 0;
Mutex MU, MU2;
CondVar CV, CV2;

void Thr1() {
  usleep(1000*1000); // Make sure CV2.Signal() "happens before" CV.Signal()
  usleep(10000);  // Make sure the waiter blocks.

  GLOB = 1; // WRITE

  MU.Lock();
  COND = 1;
  CV.Signal();
  MU.Unlock();
}
void Thr2() {
  usleep(10000);  // Make sure the waiter blocks.

  MU2.Lock();
  COND2 = 1;
  CV2.Signal();
  MU2.Unlock();
}
void Thr3() {
  MU.Lock();
  while(COND != 1)
    CV.Wait(&MU);
  MU.Unlock();
}
void Thr4() {
  MU2.Lock();
  while(COND2 != 1)
    CV2.Wait(&MU2);
  MU2.Unlock();
  GLOB = 2; // READ: no HB-relation between CV.Signal and CV2.Wait !
}
void Run() {
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test95: TP.");
  printf("test95: TP. Check do_cv_signal/fake segment logic\n");
  MyThreadArray mta(Thr1, Thr2, Thr3, Thr4);
  mta.Start();
  mta.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 95);
}  // namespace test95

// test96: TN. tricky LockSet behaviour {{{1
// 3 threads access the same memory with three different
// locksets: {A, B}, {B, C}, {C, A}.
// These locksets have empty intersection
namespace test96 {
int     GLOB = 0;

Mutex A, B, C;

void Thread1() {
  MutexLock a(&A);
  MutexLock b(&B);
  GLOB++;
}

void Thread2() {
  MutexLock b(&B);
  MutexLock c(&C);
  GLOB++;
}

void Thread3() {
  MutexLock a(&A);
  MutexLock c(&C);
  GLOB++;
}

void Run() {
  printf("test96: FP. tricky LockSet behaviour\n");
  ANNOTATE_TRACE_MEMORY(&GLOB);
  MyThreadArray mta(Thread1, Thread2, Thread3);
  mta.Start();
  mta.Join();
  CHECK(GLOB == 3);
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 96);
}  // namespace test96

namespace FalseNegativeOfFastModeTest {  // {{{1
// This test shows false negative with --fast-mode=yes.
const int HG_CACHELINE_SIZE = 64;

StealthNotification n1, n2;

const int ARRAY_SIZE = HG_CACHELINE_SIZE * 4 / sizeof(int);
int array[ARRAY_SIZE];
int * GLOB = &array[ARRAY_SIZE/2];
/*
  We use sizeof(array) == 4 * HG_CACHELINE_SIZE to be sure that GLOB points
  to a memory inside a CacheLineZ which is inside array's memory range
 */

void Reader() {
  n1.wait();
  CHECK(0 != *GLOB);
  n2.signal();
}

TEST(PositiveTests, FalseNegativeOfFastModeTest) {
  MyThreadArray t(Reader);
  ANNOTATE_TRACE_MEMORY(GLOB);
  ANNOTATE_EXPECT_RACE_FOR_TSAN(GLOB, __FUNCTION__);

  t.Start();
  *GLOB = 0x12345;
  n1.signal();
  n2.wait();
  t.Join();
}
}  // namespace

// test99: TP. Unit test for a bug in LockWhen*. {{{1
namespace test99 {


bool GLOB = false;
Mutex mu;

static void Thread1() {
  for (int i = 0; i < 100; i++) {
    mu.LockWhenWithTimeout(Condition(&ArgIsTrue, &GLOB), 5);
    GLOB = false;
    mu.Unlock();
    usleep(10000);
  }
}

static void Thread2() {
  for (int i = 0; i < 100; i++) {
    mu.Lock();
    mu.Unlock();
    usleep(10000);
  }
}

void Run() {
  printf("test99: regression test for LockWhen*\n");
  MyThreadArray t(Thread1, Thread2);
  t.Start();
  t.Join();
}
REGISTER_TEST(Run, 99);
}  // namespace test99


// test100: Test for initialization bit. {{{1
namespace test100 {
int     G1 = 0;
int     G2 = 0;
int     G3 = 0;
int     G4 = 0;

void Creator() {
  G1 = 1; CHECK(G1);
  G2 = 1;
  G3 = 1; CHECK(G3);
  G4 = 1;
}

void Worker1() {
  usleep(100000);
  CHECK(G1);
  CHECK(G2);
  G3 = 3;
  G4 = 3;
}

void Worker2() {

}


void Run() {
  printf("test100: test for initialization bit. \n");
  MyThreadArray t(Creator, Worker1, Worker2);
  ANNOTATE_TRACE_MEMORY(&G1);
  ANNOTATE_TRACE_MEMORY(&G2);
  ANNOTATE_TRACE_MEMORY(&G3);
  ANNOTATE_TRACE_MEMORY(&G4);
  t.Start();
  t.Join();
}
REGISTER_TEST2(Run, 100, FEATURE|EXCLUDE_FROM_ALL)
}  // namespace test100


// test101: TN. Two signals and two waits. {{{1
namespace test101 {
Mutex MU;
CondVar CV;
int     GLOB = 0;

int C1 = 0, C2 = 0;

void Signaller() {
  usleep(100000);
  MU.Lock();
  C1 = 1;
  CV.Signal();
  printf("signal\n");
  MU.Unlock();

  GLOB = 1;

  usleep(500000);
  MU.Lock();
  C2 = 1;
  CV.Signal();
  printf("signal\n");
  MU.Unlock();
}

void Waiter() {
  MU.Lock();
  while(!C1)
    CV.Wait(&MU);
  printf("wait\n");
  MU.Unlock();

  MU.Lock();
  while(!C2)
    CV.Wait(&MU);
  printf("wait\n");
  MU.Unlock();

  GLOB = 2;

}

void Run() {
  printf("test101: negative\n");
  MyThreadArray t(Waiter, Signaller);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 101)
}  // namespace test101

// test102: --fast-mode=yes vs. --initialization-bit=yes {{{1
namespace test102 {
const int HG_CACHELINE_SIZE = 64;

Mutex MU;

const int ARRAY_SIZE = HG_CACHELINE_SIZE * 4 / sizeof(int);
int array[ARRAY_SIZE + 1];
int * GLOB = &array[ARRAY_SIZE/2];
/*
  We use sizeof(array) == 4 * HG_CACHELINE_SIZE to be sure that GLOB points
  to a memory inside a CacheLineZ which is inside array's memory range
*/

StealthNotification n1, n2, n3;

void Reader() {
  n1.wait();
  CHECK(777 == GLOB[0]);
  n2.signal();
  n3.wait();
  CHECK(777 == GLOB[1]);
}

void Run() {
  MyThreadArray t(Reader);
  ANNOTATE_EXPECT_RACE_FOR_TSAN(GLOB+0, "test102: TP. FN with --fast-mode=yes");
  ANNOTATE_EXPECT_RACE_FOR_TSAN(GLOB+1, "test102: TP");
  printf("test102: --fast-mode=yes vs. --initialization-bit=yes\n");

  t.Start();
  GLOB[0] = 777;
  n1.signal();
  n2.wait();
  GLOB[1] = 777;
  n3.signal();
  t.Join();
}

REGISTER_TEST2(Run, 102, FEATURE)
}  // namespace test102

// test103: Access different memory locations with different LockSets {{{1
namespace test103 {
const int N_MUTEXES = 6;
const int LOCKSET_INTERSECTION_SIZE = 3;

int data[1 << LOCKSET_INTERSECTION_SIZE] = {0};
Mutex MU[N_MUTEXES];

inline int LS_to_idx (int ls) {
  return (ls >> (N_MUTEXES - LOCKSET_INTERSECTION_SIZE))
      & ((1 << LOCKSET_INTERSECTION_SIZE) - 1);
}

void Worker() {
  for (int ls = 0; ls < (1 << N_MUTEXES); ls++) {
    if (LS_to_idx(ls) == 0)
      continue;
    for (int m = 0; m < N_MUTEXES; m++)
      if (ls & (1 << m))
        MU[m].Lock();

    data[LS_to_idx(ls)]++;

    for (int m = N_MUTEXES - 1; m >= 0; m--)
      if (ls & (1 << m))
        MU[m].Unlock();
  }
}

void Run() {
  printf("test103: Access different memory locations with different LockSets\n");
  MyThreadArray t(Worker, Worker, Worker, Worker);
  t.Start();
  t.Join();
}
REGISTER_TEST2(Run, 103, FEATURE)
}  // namespace test103

// test104: TP. Simple race (write vs write). Heap mem. {{{1
namespace test104 {
int     *GLOB = NULL;
void Worker() {
  GLOB[42] = 1;
}

void Parent() {
  MyThread t(Worker);
  t.Start();
  usleep(100000);
  GLOB[42] = 2;
  t.Join();
}
void Run() {
  GLOB = (int*)malloc(128 * sizeof(int));
  GLOB[42] = 0;
  ANNOTATE_EXPECT_RACE(&GLOB[42], "test104. TP.");
  ANNOTATE_TRACE_MEMORY(&GLOB[42]);
  printf("test104: positive\n");
  Parent();
  printf("\tGLOB=%d\n", GLOB[42]);
  free(GLOB);
}
REGISTER_TEST(Run, 104);
}  // namespace test104


// test105: Checks how stack grows. {{{1
namespace test105 {
int     GLOB = 0;

void F1() {
  int ar[32];
//  ANNOTATE_TRACE_MEMORY(&ar[0]);
//  ANNOTATE_TRACE_MEMORY(&ar[31]);
  ar[0] = 1;
  ar[31] = 1;
  CHECK(ar[0] == 1);
}

void Worker() {
  int ar[32];
//  ANNOTATE_TRACE_MEMORY(&ar[0]);
//  ANNOTATE_TRACE_MEMORY(&ar[31]);
  ar[0] = 1;
  ar[31] = 1;
  CHECK(ar[0] == 1);
  F1();
}

void Run() {
  printf("test105: negative\n");
  Worker();
  MyThread t(Worker);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 105)
}  // namespace test105


// test107: Test for ANNOTATE_EXPECT_RACE {{{1
namespace test107 {
int     GLOB = 0;
void Run() {
  printf("test107: negative\n");
  ANNOTATE_EXPECT_RACE(&GLOB, "No race in fact. Just checking the tool.");
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 107, FEATURE|EXCLUDE_FROM_ALL)
}  // namespace test107


// test108: TN. initialization of static object. {{{1
namespace test108 {
// Here we have a function-level static object.
// Starting from gcc 4 this is therad safe,
// but is is not thread safe with many other compilers.
//
// Helgrind/ThreadSanitizer supports this kind of initialization by
// intercepting __cxa_guard_acquire/__cxa_guard_release
// and ignoring all accesses between them.
// pthread_once is supported in the same manner.
class Foo {
 public:
  Foo() {
    ANNOTATE_TRACE_MEMORY(&a_);
    a_ = 42;
  }
  void Check() const { CHECK(a_ == 42); }
 private:
  int a_;
};

const Foo *GetFoo() {
  static const Foo *foo = new Foo();
  return foo;
}
void Worker0() {
  GetFoo();
}

void Worker() {
  usleep(200000);
  const Foo *foo = GetFoo();
  foo->Check();
}


void Run() {
  printf("test108: negative, initialization of static object\n");
  MyThreadArray t(Worker0, Worker, Worker);
  t.Start();
  t.Join();
}
#ifdef __GNUC__
REGISTER_TEST2(Run, 108, FEATURE)
#endif
}  // namespace test108


// test109: TN. Checking happens before between parent and child threads. {{{1
namespace test109 {
// Check that the detector correctly connects
//   pthread_create with the new thread
// and
//   thread exit with pthread_join
const int N = 32;
static int GLOB[N];

void Worker(void *a) {
  usleep(10000);
//  printf("--Worker : %ld %p\n", (int*)a - GLOB, (void*)pthread_self());
  int *arg = (int*)a;
  (*arg)++;
}

void Run() {
  printf("test109: negative\n");
  MyThread *t[N];
  for (int i  = 0; i < N; i++) {
    t[i] = new MyThread(Worker, &GLOB[i]);
  }
  for (int i  = 0; i < N; i++) {
    ANNOTATE_TRACE_MEMORY(&GLOB[i]);
    GLOB[i] = 1;
    t[i]->Start();
//    printf("--Started: %p\n", (void*)t[i]->tid());
  }
  for (int i  = 0; i < N; i++) {
//    printf("--Joining: %p\n", (void*)t[i]->tid());
    t[i]->Join();
//    printf("--Joined : %p\n", (void*)t[i]->tid());
    GLOB[i]++;
  }
  for (int i  = 0; i < N; i++) delete t[i];

  printf("\tGLOB=%d\n", GLOB[13]);
}
REGISTER_TEST(Run, 109)
}  // namespace test109


// test111: TN. Unit test for a bug related to stack handling. {{{1
namespace test111 {
char     *GLOB = 0;
bool COND = false;
Mutex mu;
const int N = 3000;

void write_to_p(char *p, int val) {
  for (int i = 0; i < N; i++)
    p[i] = val;
}

void f1() {
  char some_stack[N];
  write_to_p(some_stack, 1);
  mu.LockWhen(Condition(&ArgIsTrue, &COND));
  mu.Unlock();
}

void f2() {
  char some_stack[N];
  char some_more_stack[N];
  write_to_p(some_stack, 2);
  write_to_p(some_more_stack, 2);
}

void f0() { f2(); }

void Worker1() {
  f0();
  f1();
  f2();
}

void Worker2() {
  usleep(100000);
  mu.Lock();
  COND = true;
  mu.Unlock();
}

void Run() {
  printf("test111: regression test\n");
  MyThreadArray t(Worker1, Worker1, Worker2);
  t.Start();
  t.Join();
}
REGISTER_TEST2(Run, 111, FEATURE)
}  // namespace test111

// test112: STAB. Test for ANNOTATE_PUBLISH_MEMORY_RANGE{{{1
namespace test112 {
char     *GLOB = 0;
const int N = 64 * 5;
Mutex mu;
bool ready = false; // under mu
int beg, end; // under mu

Mutex mu1;

void Worker() {

  bool is_ready = false;
  int b, e;
  while (!is_ready) {
    mu.Lock();
    is_ready = ready;
    b = beg;
    e = end;
    mu.Unlock();
    usleep(1000);
  }

  mu1.Lock();
  for (int i = b; i < e; i++) {
    GLOB[i]++;
  }
  mu1.Unlock();
}

void PublishRange(int b, int e) {
  MyThreadArray t(Worker, Worker);
  ready = false; // runs before other threads
  t.Start();

  ANNOTATE_NEW_MEMORY(GLOB + b, e - b);
  ANNOTATE_TRACE_MEMORY(GLOB + b);
  for (int j = b; j < e; j++) {
    GLOB[j] = 0;
  }
  ANNOTATE_PUBLISH_MEMORY_RANGE(GLOB + b, e - b);

  // hand off
  mu.Lock();
  ready = true;
  beg = b;
  end = e;
  mu.Unlock();

  t.Join();
}

void Run() {
  printf("test112: stability (ANNOTATE_PUBLISH_MEMORY_RANGE)\n");
  GLOB = new char [N];

  PublishRange(0, 10);
  PublishRange(3, 5);

  PublishRange(12, 13);
  PublishRange(10, 14);

  PublishRange(15, 17);
  PublishRange(16, 18);

  // do few more random publishes.
  for (int i = 0; i < 20; i++) {
    const int begin = rand() % N;
    const int size = (rand() % (N - begin)) + 1;
    CHECK(size > 0);
    CHECK(begin + size <= N);
    PublishRange(begin, begin + size);
  }

  printf("GLOB = %d\n", (int)GLOB[0]);
}
REGISTER_TEST2(Run, 112, STABILITY)
}  // namespace test112


// test113: PERF. A lot of lock/unlock calls. Many locks {{{1
namespace    test113 {
const int kNumIter = 100000;
const int kNumLocks = 7;
Mutex   MU[kNumLocks];
TEST (StressTests, ManyLocksUnlocks2) {
  printf("test113: perf\n");
  for (int i = 0; i < kNumIter; i++ ) {
    for (int j = 0; j < kNumLocks; j++) {
      if (i & (1 << j)) MU[j].Lock();
    }
    for (int j = kNumLocks - 1; j >= 0; j--) {
      if (i & (1 << j)) MU[j].Unlock();
    }
  }
}
}  // namespace test113


// test114: STAB. Recursive static initialization. {{{1
namespace    test114 {
int Bar() {
  static int bar = 1;
  return bar;
}
int Foo() {
  static int foo = Bar();
  return foo;
}
void Worker() {
  static int x = Foo();
  CHECK(x == 1);
}
void Run() {
  printf("test114: stab\n");
  MyThreadArray t(Worker, Worker);
  t.Start();
  t.Join();
}
#ifdef __GNUC__
REGISTER_TEST(Run, 114)
#endif
}  // namespace test114

// test116: TN. some operations with string<> objects. {{{1
namespace test116 {

void Worker() {
  string A[10], B[10], C[10];
  for (int i = 0; i < 1000; i++) {
    for (int j = 0; j < 10; j++) {
      string &a = A[j];
      string &b = B[j];
      string &c = C[j];
      a = "sdl;fkjhasdflksj df";
      b = "sdf sdf;ljsd ";
      c = "'sfdf df";
      c = b;
      a = c;
      b = a;
      swap(a,b);
      swap(b,c);
    }
    for (int j = 0; j < 10; j++) {
      string &a = A[j];
      string &b = B[j];
      string &c = C[j];
      a.clear();
      b.clear();
      c.clear();
    }
  }
}

void Run() {
  printf("test116: negative (strings)\n");
  MyThreadArray t(Worker, Worker, Worker);
  t.Start();
  t.Join();
}
REGISTER_TEST2(Run, 116, FEATURE|EXCLUDE_FROM_ALL)
}  // namespace test116

// test117: TN. Many calls to function-scope static init. {{{1
namespace test117 {
const int N = 50;

int Foo() {
  usleep(20000);
  return 1;
}

void Worker(void *a) {
  static int foo = Foo();
  CHECK(foo == 1);
}

void Run() {
  printf("test117: negative\n");
  MyThread *t[N];
  for (int i  = 0; i < N; i++) {
    t[i] = new MyThread(Worker);
  }
  for (int i  = 0; i < N; i++) {
    t[i]->Start();
  }
  for (int i  = 0; i < N; i++) {
    t[i]->Join();
  }
  for (int i  = 0; i < N; i++) delete t[i];
}
#ifndef WIN32
// This is racey on Windows!
REGISTER_TEST(Run, 117)
#endif
}  // namespace test117



// test118 PERF: One signal, multiple waits. {{{1
namespace   test118 {
int     GLOB = 0;
const int kNumIter = 2000000;
void Signaller() {
  usleep(50000);
  ANNOTATE_CONDVAR_SIGNAL(&GLOB);
}
void Waiter() {
  for (int i = 0; i < kNumIter; i++) {
    ANNOTATE_CONDVAR_WAIT(&GLOB);
    if (i == kNumIter / 2)
      usleep(100000);
  }
}
TEST(StressTests, OneSignalManyWaits) {
  printf("test118: perf\n");
  MyThreadArray t(Signaller, Waiter, Signaller, Waiter);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
}  // namespace test118


// test119: TP. Testing that malloc does not introduce any HB arc. {{{1
namespace test119 {
int     GLOB = 0;
void Worker1() {
  GLOB = 1;
  free(malloc(123));
}
void Worker2() {
  usleep(100000);
  free(malloc(345));
  GLOB = 2;
}
void Run() {
  printf("test119: positive (checking if malloc creates HB arcs)\n");
  if (!(Tsan_PureHappensBefore() && kMallocUsesMutex))
    ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "true race");
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 119)
}  // namespace test119


// test120: TP. Thread1: write then read. Thread2: read. {{{1
namespace test120 {
int     GLOB = 0;

void Thread1() {
  GLOB = 1;           // write
  CHECK(GLOB);        // read
}

void Thread2() {
  usleep(100000);
  CHECK(GLOB >= 0);   // read
}

void Run() {
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "TP (T1: write then read, T2: read)");
  printf("test120: positive\n");
  MyThreadArray t(Thread1, Thread2);
  GLOB = 1;
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 120)
}  // namespace test120


namespace DoubleCheckedLocking {  // {{{1
struct Foo {
  uintptr_t padding1[16];
  uintptr_t a;
  uintptr_t padding2[16];
};

static Mutex mu;
static Foo  *foo;

void InitMe() {
  if (!foo) {
    MutexLock lock(&mu);
    if (!foo) {
      ANNOTATE_EXPECT_RACE_FOR_TSAN(&foo, "Double-checked locking (ptr)");
      foo = new Foo;
      if (Tsan_PureHappensBefore()) {
        // A pure h-b detector may or may not detect this.
        ANNOTATE_BENIGN_RACE(&foo->a, "real race");
      } else {
        // ThreadSanitizer in full hybrid mode must detect it.
        ANNOTATE_EXPECT_RACE_FOR_TSAN(&foo->a, "Double-checked locking (obj)");
      }
      foo->a = 42;
    }
  }
}

void UseMe() {
  InitMe();
  CHECK(foo);
  if (foo->a != 42) {
    printf("foo->a = %d (should be 42)\n", (int)foo->a);
  }
}

void Worker1() { UseMe(); }
void Worker2() { UseMe(); }
void Worker3() { UseMe(); }


TEST(PositiveTests, DoubleCheckedLocking1) {
  foo = NULL;
  MyThreadArray t1(Worker1, Worker2, Worker3);
  t1.Start();
  t1.Join();
  delete foo;
}
}  // namespace DoubleCheckedLocking

namespace DoubleCheckedLocking2 {  // {{{1
struct Foo {
  uintptr_t padding1[16];
  uintptr_t a;
  uintptr_t padding2[16];
};

Foo *foo;
Mutex mu;

void InitMe() {
  if (foo) return;
  Foo *x = new Foo;
  ANNOTATE_BENIGN_RACE(&x->a, "may or may not detect this race");
  x->a = 42;
  {
    MutexLock lock(&mu);
    if (!foo) {
      foo = x;
      x = NULL;
    }
  }
  if (x) delete x;
}

void Worker() {
  InitMe();
  CHECK(foo);
  CHECK(foo->a == 42);
}

TEST(PositiveTests, DoubleCheckedLocking2) {
  foo = NULL;
  ANNOTATE_EXPECT_RACE(&foo, "real race");
  MyThreadArray t1(Worker, Worker, Worker, Worker);
  t1.Start();
  t1.Join();
  delete foo;
}

}  // namespace DoubleCheckedLocking2

namespace PositiveTests_DifferentSizeAccessTest {  // {{{1

uint64_t arr[1000];
size_t    arr_index = 0;
uint64_t *MEM;
size_t size[3];
size_t offset[3];

void GenericWrite(size_t s, size_t off) {
  switch(s) {
    case 8:
      CHECK(off == 0);
      ((uint64_t*)MEM)[off] = 1;
      break;
    case 4:
      CHECK(off < 2);
      ((uint32_t*)MEM)[off] = 1;
      break;
    case 2:
      CHECK(off < 4);
      ((uint16_t*)MEM)[off] = 1;
      break;
    case 1:
      CHECK(off < 8);
      ((uint8_t*)MEM)[off] = 1;
      break;
    default: CHECK(0); break;
  }
}

void Thread1() { GenericWrite(size[0], offset[0]); }
void Thread2() { GenericWrite(size[1], offset[1]); }

bool TwoRangesIntersect(size_t beg1, size_t end1, size_t beg2, size_t end2) {
  if (beg1 <= beg2 && end1 > beg2) return true;
  if (beg2 <= beg1 && end2 > beg1) return true;
  return false;
}

void RunTwoThreads(size_t size1, size_t offset1, size_t size2, size_t offset2) {
  size[0] = size1;
  size[1] = size2;
  offset[0] = offset1;
  offset[1] = offset2;
  long beg1 = offset1 * size1;
  long end1 = beg1 + size1;
  long beg2 = offset2 * size2;
  long end2 = beg2 + size2;
  bool have_intersection = TwoRangesIntersect(beg1, end1, beg2, end2);
  char descr[1024];
  MEM = &arr[arr_index++];
  sprintf(descr, "Testing: [%ld, %ld) vs [%ld, %ld] (%s intersection); p=%p",
          beg1, end1, beg2, end2, have_intersection ? "have" : "no", MEM);
  fprintf(stderr, "%s\n", descr);
  char *racey_addr_beg = (char*)MEM + max(beg1, beg2);
  char *racey_addr_end = (char*)MEM + min(end1, end2);
  if (have_intersection) {
    ANNOTATE_EXPECT_RACE(racey_addr_beg, descr);
    if (racey_addr_end - racey_addr_beg >= 2) {
      // We expect a race on the first racey byte, but we may also see some
      // races in other bytes (e.g. if a 8-byte store is implemented via two
      // 4-byte stores on a 32-bit arch). Ignore these extra races.
      ANNOTATE_BENIGN_RACE_SIZED(racey_addr_beg+1, racey_addr_end - racey_addr_beg - 1,
                           "race");
    }
  }
  MyThreadArray t1(Thread1, Thread2);
  t1.Start();
  t1.Join();
}

void TestTwoSizes(size_t size1, size_t offset1, size_t size2, size_t offset2) {
  RunTwoThreads(size1, offset1, size2, offset2);
  RunTwoThreads(size2, offset2, size1, offset1);
}

TEST(PositiveTests, DifferentSizeAccessTest) {
  for(int size1_log = 3; size1_log >= 0; size1_log--) {
    for (int size2_log = size1_log; size2_log >= 0; size2_log--) {
      for (int off1 = 0; off1 < (1 << (3-size1_log)); off1++) {
        for (int off2 = 0; off2 < (1 << (3-size2_log)); off2++) {
          RunTwoThreads(1 << size1_log, off1, 1 << size2_log, off2);
        }
      }
    }
  }
}


const int kStressArrSize = 100;
char stress_arr[kStressArrSize];

void StressWorker() {
  const int n = 100000;
  char foo[kStressArrSize];
  memset(foo, 0, sizeof(foo));
  for (int i = 0; i < n; i++) {
    memcpy(stress_arr + i % (kStressArrSize / 2), foo, i % (kStressArrSize / 3));
  }
}

TEST(StressTests, DifferentSizeAccessStressTest) {
  ANNOTATE_BENIGN_RACE_SIZED(stress_arr, sizeof(stress_arr), "race");
  MyThreadArray t(StressWorker, StressWorker, StressWorker);
  t.Start();
  t.Join();
}
}  // namespace

// test124: What happens if we delete an unlocked lock? {{{1
namespace test124 {
// This test does not worg with pthreads (you can't call
// pthread_mutex_destroy on a locked lock).
int     GLOB = 0;
const int N = 1000;
void Worker() {
  Mutex *a_large_local_array_of_mutexes;
  a_large_local_array_of_mutexes = new Mutex[N];
  for (int i = 0; i < N; i++) {
    a_large_local_array_of_mutexes[i].Lock();
  }
  delete []a_large_local_array_of_mutexes;
  GLOB = 1;
}

void Run() {
  printf("test124: negative\n");
  MyThreadArray t(Worker, Worker, Worker);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 124, FEATURE|EXCLUDE_FROM_ALL)
}  // namespace test124


// test126 TN: test for BlockingCounter {{{1
namespace  test126 {
BlockingCounter *blocking_counter;
int     GLOB = 0;
void Worker() {
  CHECK(blocking_counter);
  CHECK(GLOB == 0);
  blocking_counter->DecrementCount();
}
void Run() {
  printf("test126: negative\n");
  MyThreadArray t(Worker, Worker, Worker);
  blocking_counter = new BlockingCounter(3);
  t.Start();
  blocking_counter->Wait();
  GLOB = 1;
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST(Run, 126)
}  // namespace test126


// test127. Bad code: unlocking a mutex locked by another thread. {{{1
namespace test127 {
Mutex mu;
void Thread1() {
  mu.Lock();
  usleep(1); // avoid tail call elimination
}
void Thread2() {
  usleep(100000);
  mu.Unlock();
  usleep(1); // avoid tail call elimination
}
TEST(LockTests, UnlockingALockHeldByAnotherThread) {
  MyThreadArray t(Thread1, Thread2);
  t.Start();
  t.Join();
}
}  // namespace test127

// test128. Suppressed code in concurrent accesses {{{1
// Please use --suppressions=unittest.supp flag when running this test.
namespace test128 {
Mutex mu;
int GLOB = 0;
void Worker() {
  usleep(100000);
  mu.Lock();
  GLOB++;
  mu.Unlock();
}
void ThisFunctionShouldBeSuppressed() {
  GLOB++;
}
void Run() {
  printf("test128: Suppressed code in concurrent accesses.\n");
  MyThreadArray t(Worker, ThisFunctionShouldBeSuppressed);
  t.Start();
  t.Join();
}
REGISTER_TEST2(Run, 128, FEATURE | EXCLUDE_FROM_ALL)
}  // namespace test128

// test129: TN. Synchronization via ReaderLockWhen(). {{{1
namespace test129 {
int     GLOB = 0;
Mutex   MU;
bool WeirdCondition(int* param) {
  *param = GLOB;  // a write into Waiter's memory
  return GLOB > 0;
}
void Waiter() {
  int param = 0;
  MU.ReaderLockWhen(Condition(WeirdCondition, &param));
  MU.ReaderUnlock();
  CHECK(GLOB > 0);
  CHECK(param > 0);
}
void Waker() {
  usleep(100000);  // Make sure the waiter blocks.
  MU.Lock();
  GLOB++;
  MU.Unlock();     // calls ANNOTATE_CONDVAR_SIGNAL;
}
void Run() {
  printf("test129: Synchronization via ReaderLockWhen()\n");
  MyThread mt(Waiter, NULL, "Waiter Thread");
  mt.Start();
  Waker();
  mt.Join();
  printf("\tGLOB=%d\n", GLOB);
}
REGISTER_TEST2(Run, 129, FEATURE);
}  // namespace test129

namespace NegativeTests_PerThreadTest {  // {{{1
#ifdef TLS
// This test verifies that the race detector handles
// thread-local storage (TLS) correctly.
// As of 09-03-30 ThreadSanitizer has a bug:
//   - Thread1 starts
//   - Thread1 touches per_thread_global
//   - Thread1 ends
//   - Thread2 starts (and there is no happens-before relation between it and
//   Thread1)
//   - Thread2 touches per_thread_global
// It may happen so that Thread2 will have per_thread_global in the same address
// as Thread1. Since there is no happens-before relation between threads,
// ThreadSanitizer reports a race.
//
// test131 does the same for stack.

static TLS int per_thread_global[10] = {0};

void RealWorker() {  // Touch per_thread_global.
  per_thread_global[1]++;
  per_thread_global[9]++;
  errno++;
}

void Worker() {  // Spawn few threads that touch per_thread_global.
  MyThreadArray t(RealWorker, RealWorker);
  t.Start();
  t.Join();
}
void Worker0() { usleep(0);      Worker(); }
void Worker1() { usleep(100000); Worker(); }
void Worker2() { usleep(200000); Worker(); }
void Worker3() { usleep(300000); Worker(); }

#ifdef WIN32
TEST(NegativeTests, DISABLED_PerThreadTest) {  // issue #23
#else
TEST(NegativeTests, PerThreadTest) {
#endif
  MyThreadArray t1(Worker0, Worker1, Worker2, Worker3);
  t1.Start();
  t1.Join();
}
#endif // TLS
}  // namespace test130


namespace NegativeTests_StackReuseTest {  // {{{1
// Same as PerThreadTest, but for stack.

void RealWorker() {  // Touch stack.
  int stack_var = 0;
  stack_var++;
}

void Worker() {  // Spawn few threads that touch stack.
  MyThreadArray t(RealWorker, RealWorker);
  t.Start();
  t.Join();
}
void Worker0() { usleep(0);      Worker(); }
void Worker1() { usleep(100000); Worker(); }
void Worker2() { usleep(200000); Worker(); }
void Worker3() { usleep(300000); Worker(); }

TEST(NegativeTests, StackReuseTest) {
  MyThreadArray t(Worker0, Worker1, Worker2, Worker3);
  t.Start();
  t.Join();
}

TEST(NegativeTests, StackReuseWithFlushTest) {
  MyThreadArray t1(Worker0, Worker1, Worker2, Worker3);
  MyThreadArray t2(Worker0, Worker1, Worker2, Worker3);
  t1.Start();
  ANNOTATE_FLUSH_STATE();
  usleep(400000);
  t2.Start();
  t2.Join();
  t1.Join();
}
}  // namespace test131


// test132: TP. Simple race (write vs write). Works in fast-mode. {{{1
namespace test132 {
int     GLOB = 0;
void Worker() { GLOB = 1; }

void Run1() {
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test132");
  printf("test132: positive; &GLOB=%p\n", &GLOB);
  ANNOTATE_TRACE_MEMORY(&GLOB);
  GLOB = 7;
  MyThreadArray t(Worker, Worker);
  t.Start();
  t.Join();
}

void Run() {
  Run1();
}
REGISTER_TEST(Run, 132);
}  // namespace test132


// test133: TP. Simple race (write vs write). Works in fast mode. {{{1
namespace test133 {
// Same as test132, but everything is run from a separate thread spawned from
// the main thread.
int     GLOB = 0;
void Worker() { GLOB = 1; }

void Run1() {
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "test133");
  printf("test133: positive; &GLOB=%p\n", &GLOB);
  ANNOTATE_TRACE_MEMORY(&GLOB);
  GLOB = 7;
  MyThreadArray t(Worker, Worker);
  t.Start();
  t.Join();
}
void Run() {
  MyThread t(Run1);
  t.Start();
  t.Join();
}
REGISTER_TEST(Run, 133);
}  // namespace test133


// test134 TN. Swap. Variant of test79. {{{1
namespace test134 {
#if 0
typedef __gnu_cxx::hash_map<int, int> map_t;
#else
typedef std::map<int, int> map_t;
#endif
map_t   map;
Mutex   mu;
// Here we use swap to pass map between threads.
// The synchronization is correct, but w/o the annotation
// any hybrid detector will complain.

// Swap is very unfriendly to the lock-set (and hybrid) race detectors.
// Since tmp is destructed outside the mutex, we need to have a happens-before
// arc between any prior access to map and here.
// Since the internals of tmp are created ouside the mutex and are passed to
// other thread, we need to have a h-b arc between here and any future access.
// These arcs can be created by HAPPENS_{BEFORE,AFTER} annotations, but it is
// much simpler to apply pure-happens-before mode to the mutex mu.
void Swapper() {
  map_t tmp;
  MutexLock lock(&mu);
  ANNOTATE_HAPPENS_AFTER(&map);
  // We swap the new empty map 'tmp' with 'map'.
  map.swap(tmp);
  ANNOTATE_HAPPENS_BEFORE(&map);
  // tmp (which is the old version of map) is destroyed here.
}

void Worker() {
  MutexLock lock(&mu);
  ANNOTATE_HAPPENS_AFTER(&map);
  map[1]++;
  ANNOTATE_HAPPENS_BEFORE(&map);
}

void Run() {
  printf("test134: negative (swap)\n");
  // ********************** Shorter way: ***********************
  // ANNOTATE_PURE_HAPPENS_BEFORE_MUTEX(&mu);
  MyThreadArray t(Worker, Worker, Swapper, Worker, Worker);
  t.Start();
  t.Join();
}
REGISTER_TEST(Run, 134)
}  // namespace test134

// test137 TP. Races on stack variables. {{{1
namespace test137 {
int GLOB = 0;
ProducerConsumerQueue q(10);

void Worker() {
  int stack;
  int *tmp = (int*)q.Get();
  (*tmp)++;
  int *racey = &stack;
  q.Put(racey);
  (*racey)++;
  usleep(150000);
  // We may miss the races if we sleep less due to die_memory events...
}

void Run() {
  int tmp = 0;
  printf("test137: TP. Races on stack variables.\n");
  q.Put(&tmp);
  MyThreadArray t(Worker, Worker, Worker, Worker);
  t.Start();
  t.Join();
  q.Get();
}

REGISTER_TEST2(Run, 137, FEATURE | EXCLUDE_FROM_ALL)
}  // namespace test137

namespace ThreadPoolFNTests {  // {{{1

// When using thread pools, two concurrent callbacks might be scheduled
// onto the same executor thread. As a result, unnecessary happens-before
// relation may be introduced between callbacks.
// If we set the number of executor threads to 1, any known data
// race detector will be silent.
// However, the a similar situation may happen with any number of
// executor threads (with some probability).

void Worker(int *var) {
  usleep(100000);
  *var = 42;
}

TEST(ThreadPoolFNTests, OneProducerOneConsumer) {
  int RACEY = 0;
  printf("FN. Two closures hit the same thread in ThreadPool.\n");

  ThreadPool tp(1);
  tp.StartWorkers();
  tp.Add(NewCallback(Worker, &RACEY));
  tp.Add(NewCallback(Worker, &RACEY));
}

void PutWorkerOn(ThreadPool *tp, int *var) {
  usleep(100000);
  tp->Add(NewCallback(Worker, var));
  usleep(100000);
}

TEST(ThreadPoolFNTests, TwoProducersOneConsumer) {
  int RACEY = 0;
  printf("FN. Two closures hit the same thread in ThreadPool.\n");

  ThreadPool consumers_tp(1);
  consumers_tp.StartWorkers();

  ThreadPool producers_tp(2);
  producers_tp.StartWorkers();
  producers_tp.Add(NewCallback(PutWorkerOn, &consumers_tp, &RACEY));
  producers_tp.Add(NewCallback(PutWorkerOn, &consumers_tp, &RACEY));
}
}  // namespace ThreadPoolFNTests

// test139: FN. A true race hidden by reference counting annotation. {{{1
namespace test139 {
int GLOB = 0;
RefCountedClass *obj;

void Worker1() {
  GLOB++;  // First access.
  obj->Unref();
}

void Worker2() {
  usleep(100000);
  obj->Unref();
  GLOB++;  // Second access.
}

void Run() {
  printf("test139: FN. A true race hidden by reference counting annotation.\n");

  obj = new RefCountedClass;
  obj->AnnotateUnref();
  obj->Ref();
  obj->Ref();
  MyThreadArray mt(Worker1, Worker2);
  mt.Start();
  mt.Join();
}

REGISTER_TEST2(Run, 139, FEATURE)
}  // namespace test139

// Simple FIFO queue annotated with PCQ annotations. {{{1
class FifoMessageQueue {
 public:
  FifoMessageQueue() { ANNOTATE_PCQ_CREATE(this); }
  ~FifoMessageQueue() { ANNOTATE_PCQ_DESTROY(this); }
  // Send a message. 'message' should be positive.
  void Put(int message) {
    CHECK(message);
    MutexLock lock(&mu_);
    ANNOTATE_PCQ_PUT(this);
    q_.push(message);
  }
  // Return the message from the queue and pop it
  // or return 0 if there are no messages.
  int Get() {
    MutexLock lock(&mu_);
    if (q_.empty()) return 0;
    int res = q_.front();
    q_.pop();
    ANNOTATE_PCQ_GET(this);
    return res;
  }
 private:
  Mutex mu_;
  queue<int> q_;
};


// test142: TN. Check PCQ_* annotations. {{{1
namespace test142 {
// Putter writes to array[i] and sends a message 'i'.
// Getters receive messages and read array[message].
// PCQ_* annotations calm down the hybrid detectors.

const int N = 1000;
int array[N+1];

FifoMessageQueue q;

void Putter() {
  for (int i = 1; i <= N; i++) {
    array[i] = i*i;
    q.Put(i);
    usleep(1000);
  }
}

void Getter() {
  int non_zero_received  = 0;
  for (int i = 1; i <= N; i++) {
    int res = q.Get();
    if (res > 0) {
      CHECK(array[res] = res * res);
      non_zero_received++;
    }
    usleep(1000);
  }
#ifndef WIN32
#ifdef OS_darwin
  printf("T=%p: non_zero_received=%d\n",
         (void*)pthread_self(), non_zero_received);
#else
  printf("T=%d: non_zero_received=%d\n",
         (int)pthread_self(), non_zero_received);
#endif
#endif
}

void Run() {
  printf("test142: tests PCQ annotations\n");
  MyThreadArray t(Putter, Getter, Getter);
  t.Start();
  t.Join();
}
REGISTER_TEST(Run, 142)
}  // namespace test142


// test143: TP. Check PCQ_* annotations. {{{1
namespace test143 {
// True positive.
// We have a race on GLOB between Putter and one of the Getters.
// Pure h-b will not see it.
// If FifoMessageQueue was annotated using HAPPENS_BEFORE/AFTER, the race would
// be missed too.
// PCQ_* annotations do not hide this race.
int     GLOB = 0;
StealthNotification n;

FifoMessageQueue q;

void Putter() {
  GLOB = 1;
  q.Put(1);
  n.signal();
}

void Getter() {
  n.wait();
  q.Get();
  CHECK(GLOB == 1);  // Race here
}

void Run() {
  q.Put(1);
  if (!Tsan_PureHappensBefore()) {
    ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "true races");
  }
  printf("test143: tests PCQ annotations (true positive)\n");
  MyThreadArray t(Putter, Getter, Getter);
  t.Start();
  t.Join();
}
REGISTER_TEST(Run, 143);
}  // namespace test143

// test144: Unit-test for a bug in fast-mode {{{1
namespace test144 {
struct Foo {
  int a, b;
} ALIGNED(64);

struct Foo GLOB;
int &RACEY = GLOB.a;

void Worker() {
  RACEY++;
}

void Run() {
  printf("test144: fast-mode bug\n");
  ANNOTATE_TRACE_MEMORY(&RACEY);
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&RACEY, "Real race");

  // This line resets GLOB's creator_tid (bug).
  ANNOTATE_NEW_MEMORY(&GLOB.b, sizeof(GLOB.b));

  MyThreadArray t(Worker, Worker);
  t.Start();
  t.Join();
}

REGISTER_TEST(Run, 144);
}  // namespace test144

// test145: Unit-test for a bug in fast-mode {{{1
namespace test145 {
// A variation of test144 for dynamic memory.

struct Foo {
  int a, b;
} ALIGNED(64);

struct Foo *GLOB;
int *RACEY = NULL;

void Worker() {
  (*RACEY)++;
}

void Run() {
  printf("test145: fast-mode bug\n");

  GLOB = new Foo;
  RACEY = &(GLOB->a);
  ANNOTATE_TRACE_MEMORY(RACEY);
  ANNOTATE_EXPECT_RACE_FOR_TSAN(RACEY, "Real race");

  // This line resets GLOB's creator_tid (bug).
  ANNOTATE_NEW_MEMORY(&(GLOB->b), sizeof(GLOB->b));

  MyThreadArray t(Worker, Worker);
  t.Start();
  t.Join();
  delete GLOB;
}

REGISTER_TEST(Run, 145);
}  // namespace test145

// test147: allocating 1.5G of mem in one chunk. {{{1
namespace test147 {
void Run() {
  printf("test147: malloc 1.5G\n");
  free(malloc((1 << 30) + (1 << 29)));
}
REGISTER_TEST(Run, 147)
}  // namespace test147

// test148: FN. 3 threads, h-b hides race between T1 and T3. {{{1
namespace test148 {
int GLOB = 0;
int COND = 0;
Mutex mu;
CondVar cv;

void Signaller() {
  usleep(1000000);
  GLOB = 1;
  mu.Lock();
  COND = 1;
  cv.Signal();
  mu.Unlock();
}

void Waiter() {
  mu.Lock();
  while (COND == 0)
    cv.Wait(&mu);
  ANNOTATE_CONDVAR_LOCK_WAIT(&cv, &mu);
  GLOB = 2;
  mu.Unlock();
}

void Racer() {
  usleep(2000000);
  mu.Lock();
  GLOB = 3;
  mu.Unlock();
}

void Run() {
  printf("test148: FN. 3 threads, h-b hides race between T1 and T3.\n");
  MyThreadArray mta(Signaller, Waiter, Racer);
  mta.Start();
  mta.Join();
}
REGISTER_TEST(Run, 148)
}  // namespace test148

// test149: allocate and memset lots of of mem in several chunks. {{{1
namespace test149 {
void Run() {
  int kChunkSize = 1 << 26;
  printf("test149: malloc 8x%dM\n", kChunkSize / (1 << 20));
  void *mem[8];
  for (int i = 0; i < 8; i++) {
    mem[i] = malloc(kChunkSize);
    memset(mem[i], 0, kChunkSize);
    printf("+");
  }
  for (int i = 0; i < 8; i++) {
    free(mem[i]);
    printf("-");
  }
  printf(" Done\n");
}
REGISTER_TEST2(Run, 149, EXCLUDE_FROM_ALL)  // TODO(kcc): enable it back
}  // namespace test149

// test150: race which is detected after one of the thread has joined. {{{1
namespace test150 {
int     GLOB = 0;
StealthNotification n;
void Writer1() { GLOB++; }
void Writer2() {
  n.wait();
  GLOB++;
}
TEST(PositiveTests, RaceDetectedAfterJoin) {
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "real race");
  MyThread t1(Writer1);
  MyThread t2(Writer2);
  t1.Start();
  t2.Start();
  t1.Join();
  n.signal();
  t2.Join();
  printf("\tGLOB=%d\n", GLOB);
}
}  // namespace test150


// test151: stress for the size of vector time clock. {{{1
namespace test151 {
int kNumThreads = 100;
int kNumSegments = 5000000;
void Void() { }
void Run() {
  printf("test151: stress\n");
  printf("Creating %d threads\n", kNumThreads);
  for (int i = 0; i < kNumThreads; i++) {
    MyThread t(Void); 
    t.Start();
    t.Join();
  }
  printf("Creating %d segments\n", kNumSegments);
  for (int i = 0; i < kNumSegments; i++) {
    if (i % (kNumSegments / 50) == 0)
      printf(".");
    ANNOTATE_HAPPENS_BEFORE(NULL);
  }
  printf(" done\n");
}
REGISTER_TEST2(Run, 151, PERFORMANCE | EXCLUDE_FROM_ALL)  // TODO(kcc): enable
}  // namespace test151

// test152: atexit -> exit creates a h-b arc. {{{1
namespace test152 {
int     GLOB = 0;
MyThread *t;

void AtExitCallback() {
  GLOB++;
}

void AtExitThread() {
  GLOB++;
  atexit(AtExitCallback);
}

TEST(NegativeTests, AtExitTest) {
  t = new MyThread(AtExitThread);
  t->Start(); // We don't join it.
}
}  // namespace test152

// test153:  test for vanilla pthread_spinlock_t {{{1
namespace test153 {
#ifndef NO_SPINLOCK
// pthread_spinlock_t is tricky because pthread_spin_unlock and
// pthread_spin_init are the same symbol.
int     GLOB = 0;
pthread_spinlock_t lock;

void Worker1() {
  pthread_spin_lock(&lock);
  GLOB++;
  pthread_spin_unlock(&lock);
}

void Worker2() {
  while (pthread_spin_trylock(&lock) != 0) { }
  GLOB++;
  pthread_spin_unlock(&lock);
}


void Run() {
  printf("test153: pthread_spin_t\n");
  for (int i = 0; i < 3; i++) {
    // test few times on the same lock to check how init/destroy are handled.
    pthread_spin_init(&lock, 0);
    MyThreadArray t(Worker1, Worker1, Worker2, Worker2);
    t.Start();
    t.Join();
    pthread_spin_destroy(&lock);
  }
}
REGISTER_TEST(Run, 153)
#endif // NO_SPINLOCK
}  // namespace test153

// test154: long test with lots of races. {{{1
namespace test154 {
const int kNumIters = 100000;
const int kArraySize = 100000;
int *arr;

void RaceyAccess(int *a) {
  (*a)++;
}

void RaceyLoop() {
  for (int j = 0; j < kArraySize; j++) {
    RaceyAccess(&arr[j]);
  }
}

void Worker() {
  for (int i = 0; i < kNumIters; i++) {
    usleep(1);
    printf(".");
    if ((i % 40) == 39)
      printf("\n");
    RaceyLoop();
  }
}

void Run() {
  arr = new int[kArraySize];
  printf("test154: positive; long test with lots of races\n");
  MyThreadArray t(Worker, Worker);
  t.Start();
  t.Join();
  delete arr;
}
REGISTER_TEST2(Run, 154, EXCLUDE_FROM_ALL)
}  // namespace test154

namespace PositiveTests_RaceInMemcpy {  // {{{1
char *GLOB;

void DoMemcpy() {
  memcpy(GLOB, GLOB + 1, 1);
}

void DoMemmove() {
  memmove(GLOB, GLOB + 1, 1);
}

void Write0() {
  GLOB[0] = 'z';
}

void DoStrlen() {
  CHECK(strlen(GLOB) == 3);
}

void DoStrcpy() {
  CHECK(strcpy(GLOB, "zzz") == GLOB);
}

void DoStrchr() {
  CHECK(strchr(GLOB, 'o') == (GLOB + 1));
}

void DoMemchr() {
  CHECK(memchr(GLOB, 'o', 4) == (GLOB + 1));
}

void DoStrrchr() {
  CHECK(strrchr(GLOB, '!') == NULL);
}

void DoStrcmp() {
  CHECK(strcmp(GLOB, "xxx") != 0);
}

void DoStrncmp() {
  CHECK(strncmp(GLOB, "xxx", 3) != 0);
}


void RunThreads(void (*f1)(void), void (*f2)(void), char *mem) {
  GLOB = mem;
  strcpy(GLOB, "foo");
  ANNOTATE_EXPECT_RACE_FOR_TSAN(GLOB, "expected race");
  MyThreadArray t(f1, f2);
  t.Start();
  t.Join();
}

TEST(PositiveTests, RaceInMemcpy) {
  static char mem[4];
  RunThreads(DoMemcpy, DoMemcpy, mem);
}

TEST(PositiveTests, RaceInMemmove) {
  static char mem[4];
  RunThreads(DoMemmove, DoMemmove, mem);
}

TEST(PositiveTests, RaceInStrlen1) {
  static char mem[4];
  RunThreads(DoStrlen, Write0, mem);
}

TEST(PositiveTests, RaceInStrlen2) {
  static char mem[4];
  RunThreads(Write0, DoStrlen, mem);
}

TEST(PositiveTests, RaceInStrcpy) {
  static char mem[4];
  RunThreads(Write0, DoStrcpy, mem);
}

TEST(PositiveTests, RaceInStrchr) {
  static char mem[4];
  RunThreads(Write0, DoStrchr, mem);
}

TEST(PositiveTests, RaceInMemchr) {
  static char mem[4];
  RunThreads(Write0, DoMemchr, mem);
}

TEST(PositiveTests, RaceInStrrchr) {
  static char mem[4];
  RunThreads(Write0, DoStrrchr, mem);
}

TEST(PositiveTests, RaceInStrcmp) {
  static char mem[4];
  RunThreads(Write0, DoStrcmp, mem);
}

TEST(PositiveTests, RaceInStrncmp) {
  static char mem[4];
  RunThreads(Write0, DoStrncmp, mem);
}

}  // namespace

// test157: TN. Test for stack traces (using ANNOTATE_NO_OP). {{{1
namespace test157 {

void func3() {
  ANNOTATE_NO_OP((void*)__LINE__);
}
void func2() {
  func3();
}
void func1() {
  func2();
}
void Worker1() {
  func1();
  ANNOTATE_NO_OP((void*)__LINE__);
}
void Worker2() {
  func2();
  ANNOTATE_NO_OP((void*)__LINE__);
}
void Worker3() {
  func3();
  ANNOTATE_NO_OP((void*)__LINE__);
}
void Run() {
  ANNOTATE_NO_OP((void*)__LINE__);
  printf("test157: negative\n");
  ANNOTATE_NO_OP((void*)__LINE__);
  MyThreadArray t(Worker1, Worker2, Worker3);
  ANNOTATE_NO_OP((void*)__LINE__);
  t.Start();
  ANNOTATE_NO_OP((void*)__LINE__);
  t.Join();
  ANNOTATE_NO_OP((void*)__LINE__);
}
REGISTER_TEST(Run, 157);
}  // namespace test157


namespace MemoryTypes {  // {{{1
  void WriteChar(void *param) {
    *(char*)param = 1;
    usleep(500000);  // let other threads hit this before exiting.
  }

  void RaceOnMemory(void (*callback)(void *), char *mem) {
    ANNOTATE_FLUSH_EXPECTED_RACES();
    ANNOTATE_EXPECT_RACE(mem, "race");
    MyThread t1(callback, mem),
             t2(callback, mem);
    t1.Start();
    t2.Start();
    t1.Join();
    t2.Join();
    CHECK(*mem == 1);
    ANNOTATE_FLUSH_EXPECTED_RACES();
  }

  void RaceOnLocalStack(void (*callback)(void *)) {
    char object_on_stack = 0;
    // We may have had races on the main stack before -- forget about them.
    ANNOTATE_NEW_MEMORY(&object_on_stack, 1);
    RaceOnMemory(callback, &object_on_stack);
  }

  // create a new function to make reports different.
  void WriteChar1(void *param) { WriteChar(param); }

  TEST(MemoryTypes, RaceOnMainThreadStack) {
    RaceOnLocalStack(WriteChar1);
  }

  void WriteChar2(void *param) { WriteChar(param); }

  TEST(MemoryTypes, RaceOnNonMainThreadStack) {
    MyThread t((void (*)(void*))(RaceOnLocalStack), (void*)WriteChar2);
    t.Start();
    t.Join();
  }

  void WriteChar3(void *param) { WriteChar(param); }

  TEST(MemoryTypes, RaceOnMallocedMemory) {
    char *mem = (char*)malloc(100);
    RaceOnMemory(WriteChar3, mem+42);
    free(mem);
  }

  void WriteChar4(void *param) { WriteChar(param); }

  TEST(MemoryTypes, RaceOnCallocedMemory) {
    char *mem = (char*)calloc(30, 4);
    RaceOnMemory(WriteChar4, mem+42);
    free(mem);
  }

  void WriteChar5(void *param) { WriteChar(param); }

  TEST(MemoryTypes, RaceOnMemoryFromNew) {
    char *mem = new char;
    RaceOnMemory(WriteChar5, mem);
    delete mem;
  }

  void WriteChar6(void *param) { WriteChar(param); }

  TEST(MemoryTypes, RaceOnMemoryFromNewA) {
    char *mem = new char [100];
    RaceOnMemory(WriteChar6, mem+42);
    delete [] mem;
  }

  void WriteChar7(void *param) { WriteChar(param); }

  TEST(MemoryTypes, RaceOnMemoryFromNewNoThrow) {
    char *mem = new (std::nothrow) char;
    RaceOnMemory(WriteChar7, mem);
    operator delete (mem, std::nothrow);
  }
  void WriteChar8(void *param) { WriteChar(param); }

  TEST(MemoryTypes, RaceOnMemoryFromNewNoThrowA) {
    char *mem = new (std::nothrow) char [100];
    RaceOnMemory(WriteChar8, mem+42);
    operator delete [] (mem, std::nothrow);
  }

  void AllocateAndDeallocateUsingVariousAllocs() {
    for (int i = 0; i < 10000; i++) {
      char *p;
      switch (i % 5) {
        case 0:
          p = (char*)malloc(10);
          free(p);
          break;
        case 1:
          p = new char;
          delete p;
          break;
        case 2:
          p = new char [10];
          delete [] p;
        case 3:
          p = new (std::nothrow) char;
          operator delete (p, std::nothrow);
          break;
        case 4:
          p = new (std::nothrow) char[10];
          operator delete [](p, std::nothrow);
          break;
      }
    }
  }
  TEST(MemoryTypes, VariousAllocs) {
    void (*f)(void) = AllocateAndDeallocateUsingVariousAllocs;
    MyThreadArray t(f, f, f, f);
    t.Start();
    t.Join();
  }

  void ReallocThread() {
    void *ptr = NULL;
    for (int i = 8; i < 128; i++) {
      int size = (1 << (i / 8)) - 1;
      ptr = realloc(ptr, size);
      ANNOTATE_TRACE_MEMORY(ptr);
      memset(ptr, 42, size);
    }
    free(ptr);
  }
  TEST(MemoryTypes, Reallocs) {
    MyThreadArray t(ReallocThread, ReallocThread, ReallocThread, ReallocThread);
    t.Start();
    t.Join();
  }
}  // namespace


namespace  StressTests_ThreadTree {  //{{{1
int     GLOB = 0;

// Worker(N) will do 2^N increments of GLOB, each increment in a separate thread
void Worker(int depth) {
  CHECK(depth >= 0);
  if (depth > 0) {
    MyThread t1((MyThread::worker_t)Worker, (void*)(intptr_t)(depth - 1));
    MyThread t2((MyThread::worker_t)Worker, (void*)(intptr_t)(depth - 1));
    t1.Start();
    t2.Start();
    t1.Join();
    t2.Join();
  } else {
    GLOB++; // Race here
  }
}

TEST(StressTests, ThreadTree3) {
  ANNOTATE_EXPECT_RACE(&GLOB, "StressTests.ThreadTree3 race");
  ANNOTATE_TRACE_MEMORY(&GLOB);
  Worker(3);
}

TEST(StressTests, DISABLED_ThreadTree7) {
  ANNOTATE_EXPECT_RACE(&GLOB, "StressTests.ThreadTree7 race");
  ANNOTATE_TRACE_MEMORY(&GLOB);
  Worker(7);
}
}  // namespace test313

namespace  StressTests_StartAndJoinManyThreads {  //{{{1

void Worker() {
}

// Too slow. Need to run it separately.
TEST(StressTests, StartAndJoinManyThreads) {
  ANNOTATE_FLUSH_STATE();
  for (int i = 0; i < 1100; i++) {
    if ((i % 100) == 0)
      printf(".");
    MyThread t1(Worker);
    MyThread t2(Worker);
    t1.Start();
    t2.Start();
    t1.Join();
    t2.Join();
  }
  printf("\n");
}
}  // namespace

namespace StressTests_ManyAccesses {  // {{{1
#ifndef NO_BARRIER
const int kArrayLen = 128;  // Small size, so that everything fits into cache.
const int kNumIter = 1024 * 1024 * 2;
int thread_id;
int *array = NULL;
Barrier *barrier;

void IncrementMe(int *x) {
  (*x)++;
}

void NoRaceWorker() {
  int id = AtomicIncrement(&thread_id, 1);
  barrier->Block();
  int *ptr = array + id * (kArrayLen + 64);  // pad to avoid false sharing.
  for (int it = 0; it < kNumIter; it++) {
    for (int i = 0; i < kArrayLen; i++) {
      IncrementMe(ptr + i);
    }
  }
}

void RunThreads(int n_threads, void (*f)(void)) {
  thread_id = -1;
  barrier = new Barrier(n_threads);
  // Allocate a lot so that operator new uses mmap, unless forced to use brk.
  array = new int[(kArrayLen + 64) * n_threads + (1 << 22)];
  printf("ptr = %p\n", array);
  MyThread **t = new MyThread*[n_threads];
  for (int i = 0; i < n_threads; i++) t[i] = new MyThread(NoRaceWorker);
  for (int i = 0; i < n_threads; i++) t[i]->Start();
  for (int i = 0; i < n_threads; i++) t[i]->Join();
  for (int i = 0; i < n_threads; i++) delete t[i];
  delete [] t;
  delete [] array;
}

// Just one thread.
TEST(StressTests, DISABLED_ManyAccessesNoRace1Test) {
  RunThreads(1, NoRaceWorker);
}

// 2 threads accessing different memory.
TEST(StressTests, DISABLED_ManyAccessesNoRace2Test) {
  RunThreads(2, NoRaceWorker);
}
// 4 threads accessing different memory.
TEST(StressTests, DISABLED_ManyAccessesNoRace4Test) {
  RunThreads(4, NoRaceWorker);
}
// 8 threads accessing different memory.
TEST(StressTests, DISABLED_ManyAccessesNoRace8Test) {
  RunThreads(8, NoRaceWorker);
}
// 16 threads accessing different memory.
TEST(StressTests, DISABLED_ManyAccessesNoRace16Test) {
  RunThreads(16, NoRaceWorker);
}
#endif  // NO_BARRIER
}  // namespace

namespace NegativeTests_EnableRaceDetectionTest {  // {{{1
const size_t size = 10000;
const size_t n_iter = 1000;
int GLOB[size];

void Worker() {
  for (size_t i = 0; i < n_iter; i++) {
    for (size_t j = 0; j < size; j++) {
      GLOB[j]++;
    }
  }
}

TEST(NegativeTests, EnableRaceDetectionTest) {
  ANNOTATE_ENABLE_RACE_DETECTION(0);
  MyThreadArray t(Worker, Worker, Worker, Worker);
  t.Start();
  t.Join();
  ANNOTATE_ENABLE_RACE_DETECTION(1);
}
}

namespace PositiveTests_MopVsFree {  // {{{1
int *p;
const int kIdx = 77;
StealthNotification n;

void Read() {
  CHECK(p[kIdx] == 777);
  n.signal();
}
void Free() {
  n.wait();
  free(p);
}

TEST(PositiveTests, ReadVsFree) {
  p = (int*)malloc(100 * sizeof(int));
  p[kIdx] = 777;
  ANNOTATE_EXPECT_RACE(&p[kIdx], "race: read vs free");
  MyThreadArray t(Read, Free);
  t.Start();
  t.Join();
}

}  // namespace

namespace ManySmallObjectsTest {  // {{{1
void Worker() {
  const int N = 1 << 21;
  struct T {
    int a, b, c, d;
    T() : a(1), b(2), c(3), d(4) { }
  };
  T **a = new T*[N];
  for (int i = 0; i < N; i++) {
    if ((i % (N / 16)) == 0)
      printf("+");
    a[i] = new T;
    CHECK(a[i]->a == 1);
  }
  printf("\n");
  for (int i = 0; i < N; i++) {
    if ((i % (N / 16)) == 0)
      printf("-");
    delete a[i];
  }
  printf("\n");
  delete [] a;
}

TEST(StressTests, DISABLED_ManySmallObjectsOneThreadTest) {
  Worker();
}

TEST(StressTests, DISABLED_ManySmallObjectsTwoThreadsTest) {
  MyThreadArray t(Worker, Worker);
  t.Start();
  t.Join();
}
}  // namespace

namespace  RepPrefixedInstructionsTest {  //{{{1

#if defined (__GNUC__) && (defined(ARCH_x86) || defined(ARCH_amd64))
void rep_clr_1(uint8_t *s, long n)
{
  intptr_t d0, d1;
      __asm__ __volatile__ (
      "rep ; stosb"
      : "=&c" (d0), "=&D" (d1)
      : "a" (0), "1" (s), "0" (n)
      : "memory");
}

uint8_t mem1[1000];

void Clr1_0_10()  { rep_clr_1(mem1+ 0, 10); }
void Clr1_10_10() { rep_clr_1(mem1+10, 10); }
void Clr1_10_0() { rep_clr_1(mem1+10, 0); }

void Clr1_25_1() { rep_clr_1(mem1+25, 1); }
void Clr1_25_0() { rep_clr_1(mem1+25, 0); }

void Clr1_50_30() { rep_clr_1(mem1+50, 30); }
void Clr1_60_0() { rep_clr_1(mem1+60, 0); }
void Clr1_60_1() { rep_clr_1(mem1+60, 1); }
void Clr1_70_10() { rep_clr_1(mem1+70, 10); }


void RunThreads(void (*f1)(void), void (*f2)(void)) {
  MyThreadArray t(f1, f2);
  t.Start();
  t.Join();
}

TEST(NegativeTests, RepSanityTest) {
  memset(mem1, 0xff, sizeof(mem1));
  rep_clr_1(mem1, 0);
  CHECK(mem1[0] != 0);
  rep_clr_1(mem1, 1);
  CHECK(mem1[0] == 0);
  CHECK(mem1[1] != 0);
  rep_clr_1(mem1, 5);
  CHECK(mem1[4] == 0);
  CHECK(mem1[5] != 0);
}

TEST(NegativeTests, RepNegativeTest) {
  memset(mem1, 0xff, sizeof(mem1));
  RunThreads(Clr1_0_10, Clr1_10_10);
  RunThreads(Clr1_10_0, Clr1_10_10);
  RunThreads(Clr1_25_0, Clr1_25_1);
  RunThreads(Clr1_50_30, Clr1_60_0);
}

TEST(PositiveTests, RepPositive1Test) {
  memset(mem1, 0xff, sizeof(mem1));
  ANNOTATE_EXPECT_RACE(mem1+10, "real race");
  for (int i = 11; i < 20; i++) ANNOTATE_BENIGN_RACE(mem1 + i, "");
  RunThreads(Clr1_10_10, Clr1_10_10);
}
TEST(PositiveTests, RepPositive2Test) {
  memset(mem1, 0xff, sizeof(mem1));
  ANNOTATE_EXPECT_RACE(mem1+25, "real race");
  RunThreads(Clr1_25_1, Clr1_25_1);
}

TEST(PositiveTests, RepPositive3Test) {
  memset(mem1, 0xff, sizeof(mem1));
  ANNOTATE_EXPECT_RACE(mem1+60, "real race");
  RunThreads(Clr1_50_30, Clr1_60_1);
}

TEST(PositiveTests, RepPositive4Test) {
  memset(mem1, 0xff, sizeof(mem1));
  ANNOTATE_EXPECT_RACE(mem1+70, "real race");
  for (int i = 71; i < 80; i++) ANNOTATE_BENIGN_RACE(mem1 + i, "");
  RunThreads(Clr1_50_30, Clr1_70_10);
}
#endif  // __GNUC__ ...
}  // namespace

// test400: Demo of a simple false positive. {{{1
namespace test400 {
static Mutex mu;
static vector<int> *vec; // GUARDED_BY(mu);

void InitAllBeforeStartingThreads() {
  vec = new vector<int>;
  vec->push_back(1);
  vec->push_back(2);
}

void Thread1() {
  MutexLock lock(&mu);
  vec->pop_back();
}

void Thread2() {
  MutexLock lock(&mu);
  vec->pop_back();
}

//---- Sub-optimal code ---------
size_t NumberOfElementsLeft() {
  MutexLock lock(&mu);
  return vec->size();
}

void WaitForAllThreadsToFinish_InefficientAndTsanUnfriendly() {
  while(NumberOfElementsLeft()) {
    ; // sleep or print or do nothing.
  }
  // It is now safe to access vec w/o lock.
  // But a hybrid detector (like ThreadSanitizer) can't see it.
  // Solutions:
  //   1. Use pure happens-before detector (e.g. "tsan --pure-happens-before")
  //   2. Call ANNOTATE_PURE_HAPPENS_BEFORE_MUTEX(&mu)
  //      in InitAllBeforeStartingThreads()
  //   3. (preferred) Use WaitForAllThreadsToFinish_Good() (see below).
  CHECK(vec->empty());
  delete vec;
}

//----- Better code -----------

bool NoElementsLeft(vector<int> *v) {
  return v->empty();
}

void WaitForAllThreadsToFinish_Good() {
  mu.LockWhen(Condition(NoElementsLeft, vec));
  mu.Unlock();

  // It is now safe to access vec w/o lock.
  CHECK(vec->empty());
  delete vec;
}


void Run() {
  MyThreadArray t(Thread1, Thread2);
  InitAllBeforeStartingThreads();
  t.Start();
  WaitForAllThreadsToFinish_InefficientAndTsanUnfriendly();
//  WaitForAllThreadsToFinish_Good();
  t.Join();
}
REGISTER_TEST2(Run, 400, RACE_DEMO)
}  // namespace test400

// test401: Demo of false positive caused by reference counting. {{{1
namespace test401 {
// A simplified example of reference counting.
// DecRef() does ref count increment in a way unfriendly to race detectors.
// DecRefAnnotated() does the same in a friendly way.

static vector<int> *vec;
static int ref_count;

void InitAllBeforeStartingThreads(int number_of_threads) {
  vec = new vector<int>;
  vec->push_back(1);
  ref_count = number_of_threads;
}

// Correct, but unfriendly to race detectors.
int DecRef() {
  return AtomicIncrement(&ref_count, -1);
}

// Correct and friendly to race detectors.
int DecRefAnnotated() {
  ANNOTATE_HAPPENS_BEFORE(&ref_count);
  int res = AtomicIncrement(&ref_count, -1);
  if (res == 0) {
    ANNOTATE_HAPPENS_AFTER(&ref_count);
  }
  return res;
}

void ThreadWorker() {
  CHECK(ref_count > 0);
  CHECK(vec->size() == 1);
  if (DecRef() == 0) {  // Use DecRefAnnotated() instead!
    // No one uses vec now ==> delete it.
    delete vec;  // A false race may be reported here.
    vec = NULL;
  }
}

void Run() {
  MyThreadArray t(ThreadWorker, ThreadWorker, ThreadWorker);
  InitAllBeforeStartingThreads(3 /*number of threads*/);
  t.Start();
  t.Join();
  CHECK(vec == 0);
}
REGISTER_TEST2(Run, 401, RACE_DEMO)
}  // namespace test401


// test502: produce lots of segments without cross-thread relations {{{1
namespace test502 {

/*
 * This test produces ~1Gb of memory usage when run with the following options:
 *
 * --tool=helgrind
 * --trace-after-race=0
 * --num-callers=2
 * --more-context=no
 */

Mutex MU;
int     GLOB = 0;

void TP() {
   for (int i = 0; i < 750000; i++) {
      MU.Lock();
      GLOB++;
      MU.Unlock();
   }
}

void Run() {
   MyThreadArray t(TP, TP);
   printf("test502: produce lots of segments without cross-thread relations\n");

   t.Start();
   t.Join();
}

REGISTER_TEST2(Run, 502, MEMORY_USAGE | PRINT_STATS | EXCLUDE_FROM_ALL
                              | PERFORMANCE)
}  // namespace test502

// test503: produce lots of segments with simple HB-relations {{{1
// HB cache-miss rate is ~55%
namespace test503 {

//  |- |  |  |  |  |
//  | \|  |  |  |  |
//  |  |- |  |  |  |
//  |  | \|  |  |  |
//  |  |  |- |  |  |
//  |  |  | \|  |  |
//  |  |  |  |- |  |
//  |  |  |  | \|  |
//  |  |  |  |  |- |
//  |  |  |  |  | \|
//  |  |  |  |  |  |----
//->|  |  |  |  |  |
//  |- |  |  |  |  |
//  | \|  |  |  |  |
//     ...

const int N_threads = 32;
const int ARRAY_SIZE = 128;
int       GLOB[ARRAY_SIZE];
ProducerConsumerQueue *Q[N_threads];
int GLOB_limit = 100000;
int count = -1;

void Worker(){
   int myId = AtomicIncrement(&count, 1);

   ProducerConsumerQueue &myQ = *Q[myId], &nextQ = *Q[(myId+1) % N_threads];

   // this code produces a new SS with each new segment
   while (myQ.Get() != NULL) {
      for (int i = 0; i < ARRAY_SIZE; i++)
         GLOB[i]++;

      if (myId == 0 && GLOB[0] > GLOB_limit) {
         // Stop all threads
         for (int i = 0; i < N_threads; i++)
            Q[i]->Put(NULL);
      } else
         nextQ.Put(GLOB);
   }
}

void Run() {
   printf("test503: produce lots of segments with simple HB-relations\n");
   for (int i = 0; i < N_threads; i++)
      Q[i] = new ProducerConsumerQueue(1);
   Q[0]->Put(GLOB);

   {
      ThreadPool pool(N_threads);
      pool.StartWorkers();
      for (int i = 0; i < N_threads; i++) {
         pool.Add(NewCallback(Worker));
      }
   } // all folks are joined here.

   for (int i = 0; i < N_threads; i++)
      delete Q[i];
}

REGISTER_TEST2(Run, 503, MEMORY_USAGE | PRINT_STATS
                  | PERFORMANCE | EXCLUDE_FROM_ALL)
}  // namespace test503

// test504: force massive cache fetch-wback (50% misses, mostly CacheLineZ) {{{1
namespace test504 {
#if !defined(WINE) and !defined(ANDROID) // Valgrind+wine hate large static objects
const int N_THREADS = 2,
          HG_CACHELINE_COUNT = 1 << 16,
          HG_CACHELINE_SIZE  = 1 << 6,
          HG_CACHE_SIZE = HG_CACHELINE_COUNT * HG_CACHELINE_SIZE;

// int gives us ~4x speed of the byte test
// 4x array size gives us
// total multiplier of 16x over the cachesize
// so we can neglect the cached-at-the-end memory
const int ARRAY_SIZE = 4 * HG_CACHE_SIZE,
          ITERATIONS = 30;
int array[ARRAY_SIZE];

int count = 0;
Mutex count_mu;

void Worker() {
   count_mu.Lock();
   int myId = ++count;
   count_mu.Unlock();

   // all threads write to different memory locations,
   // so no synchronization mechanisms are needed
   int lower_bound = ARRAY_SIZE * (myId-1) / N_THREADS,
       upper_bound = ARRAY_SIZE * ( myId ) / N_THREADS;
   for (int j = 0; j < ITERATIONS; j++)
   for (int i = lower_bound; i < upper_bound;
            i += HG_CACHELINE_SIZE / sizeof(array[0])) {
      array[i] = i; // each array-write generates a cache miss
   }
}

void Run() {
   printf("test504: force massive CacheLineZ fetch-wback\n");
   MyThreadArray t(Worker, Worker);
   t.Start();
   t.Join();
}

REGISTER_TEST2(Run, 504, PERFORMANCE | PRINT_STATS | EXCLUDE_FROM_ALL)
#endif // WINE
}  // namespace test504

// test505: force massive cache fetch-wback (60% misses) {{{1
// modification of test504 - more threads, byte accesses and lots of mutexes
// so it produces lots of CacheLineF misses (30-50% of CacheLineZ misses)
namespace test505 {
#if !defined(WINE) and !defined(ANDROID) // Valgrind+wine hate large static objects

const int N_THREADS = 2,
          HG_CACHELINE_COUNT = 1 << 16,
          HG_CACHELINE_SIZE  = 1 << 6,
          HG_CACHE_SIZE = HG_CACHELINE_COUNT * HG_CACHELINE_SIZE;

const int ARRAY_SIZE = 4 * HG_CACHE_SIZE,
          ITERATIONS = 3;
int64_t array[ARRAY_SIZE];

int count = 0;
Mutex count_mu;

void Worker() {
   const int N_MUTEXES = 5;
   Mutex mu[N_MUTEXES];
   count_mu.Lock();
   int myId = ++count;
   count_mu.Unlock();

   // all threads write to different memory locations,
   // so no synchronization mechanisms are needed
   int lower_bound = ARRAY_SIZE * (myId-1) / N_THREADS,
       upper_bound = ARRAY_SIZE * ( myId ) / N_THREADS;
   for (int j = 0; j < ITERATIONS; j++)
   for (int mutex_id = 0; mutex_id < N_MUTEXES; mutex_id++) {
      Mutex *m = & mu[mutex_id];
      m->Lock();
      for (int i = lower_bound + mutex_id, cnt = 0;
               i < upper_bound;
               i += HG_CACHELINE_SIZE / sizeof(array[0]), cnt++) {
         array[i] = i; // each array-write generates a cache miss
      }
      m->Unlock();
   }
}

void Run() {
   printf("test505: force massive CacheLineF fetch-wback\n");
   MyThreadArray t(Worker, Worker);
   t.Start();
   t.Join();
}

REGISTER_TEST2(Run, 505, PERFORMANCE | PRINT_STATS | EXCLUDE_FROM_ALL)
#endif // WINE
}  // namespace test505

// test506: massive HB's using Barriers {{{1
// HB cache miss is ~40%
// segments consume 10x more memory than SSs
// modification of test39
namespace test506 {
#ifndef NO_BARRIER
// Same as test17 but uses Barrier class (pthread_barrier_t).
int     GLOB = 0;
const int N_threads = 64,
          ITERATIONS = 1000;
Barrier *barrier[ITERATIONS];
Mutex   MU;

void Worker() {
  for (int i = 0; i < ITERATIONS; i++) {
     MU.Lock();
     GLOB++;
     MU.Unlock();
     barrier[i]->Block();
  }
}
void Run() {
  printf("test506: massive HB's using Barriers\n");
  for (int i = 0; i < ITERATIONS; i++) {
     barrier[i] = new Barrier(N_threads);
  }
  {
    ThreadPool pool(N_threads);
    pool.StartWorkers();
    for (int i = 0; i < N_threads; i++) {
      pool.Add(NewCallback(Worker));
    }
  } // all folks are joined here.
  CHECK(GLOB == N_threads * ITERATIONS);
  for (int i = 0; i < ITERATIONS; i++) {
     delete barrier[i];
  }
}
REGISTER_TEST2(Run, 506, PERFORMANCE | PRINT_STATS | EXCLUDE_FROM_ALL);
#endif // NO_BARRIER
}  // namespace test506

// test507: vgHelgrind_initIterAtFM/stackClear benchmark {{{1
// vgHelgrind_initIterAtFM/stackClear consume ~8.5%/5.5% CPU
namespace test507 {
const int N_THREADS    = 1,
          BUFFER_SIZE  = 1,
          ITERATIONS   = 1 << 20;

void Foo() {
  struct T {
    char temp;
    T() {
      ANNOTATE_RWLOCK_CREATE(&temp);
    }
    ~T() {
      ANNOTATE_RWLOCK_DESTROY(&temp);
    }
  } s[BUFFER_SIZE];
  s->temp = '\0';
}

void Worker() {
  for (int j = 0; j < ITERATIONS; j++) {
    Foo();
  }
}

void Run() {
  printf("test507: vgHelgrind_initIterAtFM/stackClear benchmark\n");
  {
    ThreadPool pool(N_THREADS);
    pool.StartWorkers();
    for (int i = 0; i < N_THREADS; i++) {
      pool.Add(NewCallback(Worker));
    }
  } // all folks are joined here.
}
REGISTER_TEST2(Run, 507, EXCLUDE_FROM_ALL);
}  // namespace test507

// test508: cmp_WordVecs_for_FM benchmark {{{1
// 50+% of CPU consumption by cmp_WordVecs_for_FM
namespace test508 {
const int N_THREADS    = 1,
          BUFFER_SIZE  = 1 << 10,
          ITERATIONS   = 1 << 9;

void Foo() {
  struct T {
    char temp;
    T() {
      ANNOTATE_RWLOCK_CREATE(&temp);
    }
    ~T() {
      ANNOTATE_RWLOCK_DESTROY(&temp);
    }
  } s[BUFFER_SIZE];
  s->temp = '\0';
}

void Worker() {
  for (int j = 0; j < ITERATIONS; j++) {
    Foo();
  }
}

void Run() {
  printf("test508: cmp_WordVecs_for_FM benchmark\n");
  {
    ThreadPool pool(N_THREADS);
    pool.StartWorkers();
    for (int i = 0; i < N_THREADS; i++) {
      pool.Add(NewCallback(Worker));
    }
  } // all folks are joined here.
}
REGISTER_TEST2(Run, 508, EXCLUDE_FROM_ALL);
}  // namespace test508

// test509: avl_find_node benchmark {{{1
// 10+% of CPU consumption by avl_find_node
namespace test509 {
const int N_THREADS    = 16,
          ITERATIONS   = 1 << 8;

void Worker() {
  std::vector<Mutex*> mu_list;
  for (int i = 0; i < ITERATIONS; i++) {
    Mutex * mu = new Mutex();
    mu_list.push_back(mu);
    mu->Lock();
  }
  for (int i = ITERATIONS - 1; i >= 0; i--) {
    Mutex * mu = mu_list[i];
    mu->Unlock();
    delete mu;
  }
}

void Run() {
  printf("test509: avl_find_node benchmark\n");
  {
    ThreadPool pool(N_THREADS);
    pool.StartWorkers();
    for (int i = 0; i < N_THREADS; i++) {
      pool.Add(NewCallback(Worker));
    }
  } // all folks are joined here.
}
REGISTER_TEST2(Run, 509, EXCLUDE_FROM_ALL);
}  // namespace test509

// test510: SS-recycle test {{{1
// this tests shows the case where only ~1% of SS are recycled
namespace test510 {
const int N_THREADS    = 16,
          ITERATIONS   = 1 << 10;
int GLOB = 0;

void Worker() {
  usleep(100000);
  for (int i = 0; i < ITERATIONS; i++) {
    ANNOTATE_CONDVAR_SIGNAL((void*)0xDeadBeef);
    GLOB++;
    usleep(10);
  }
}

void Run() {
  //ANNOTATE_BENIGN_RACE(&GLOB, "Test");
  printf("test510: SS-recycle test\n");
  {
    ThreadPool pool(N_THREADS);
    pool.StartWorkers();
    for (int i = 0; i < N_THREADS; i++) {
      pool.Add(NewCallback(Worker));
    }
  } // all folks are joined here.
}
REGISTER_TEST2(Run, 510, MEMORY_USAGE | PRINT_STATS | EXCLUDE_FROM_ALL);
}  // namespace test510

// test511: Segment refcounting test ('1' refcounting) {{{1
namespace test511 {
int GLOB = 0;

void Run () {
   for (int i = 0; i < 300; i++) {
      ANNOTATE_CONDVAR_SIGNAL(&GLOB);
      usleep(1000);
      GLOB++;
      ANNOTATE_CONDVAR_WAIT(&GLOB);
   }
}
REGISTER_TEST2(Run, 511, MEMORY_USAGE | PRINT_STATS | EXCLUDE_FROM_ALL);
}  // namespace test511

// test512: Access the same memory with big intersecting LockSets {{{1
namespace test512 {
const int N_MUTEXES = 128;
const int DATA_SIZE = 1024;

Mutex mu[N_MUTEXES];
int   GLOB[DATA_SIZE];

void TP() {
  Mutex thread_mu;
  thread_mu.Lock();
  for (int j = 0; j < 10; j++) {
    for (int m = 0; m < N_MUTEXES; m++)
      mu[m].Lock();
    for (int i = 0; i < 3000; i++) {
      ANNOTATE_CONDVAR_SIGNAL(&GLOB);  // Force new segment
      for (int k = 0; k < DATA_SIZE; k++)
        GLOB[k] = 42;
    }
    for (int m = 0; m < N_MUTEXES; m++)
      mu[m].Unlock();
  }
  thread_mu.Unlock();
}

void Run() {
   MyThreadArray t(TP, TP);
   printf("test512: Access the same memory with big intersecting LockSets.\n");

   t.Start();
   t.Join();
}

REGISTER_TEST2(Run, 512, EXCLUDE_FROM_ALL | PERFORMANCE)
}  // namespace test512

// test513: --fast-mode benchmark {{{1
namespace test513 {

const int N_THREADS = 2,
          HG_CACHELINE_SIZE  = 1 << 6,
          ARRAY_SIZE = HG_CACHELINE_SIZE * 512,
          MUTEX_ID_BITS = 8,
          MUTEX_ID_MASK = (1 << MUTEX_ID_BITS) - 1;

// Each thread has its own cacheline and tackles with it intensively
const int ITERATIONS = 1024;
int array[N_THREADS][ARRAY_SIZE];

int count = 0;
Mutex count_mu;
Mutex mutex_arr[N_THREADS][MUTEX_ID_BITS];

void Worker() {
   count_mu.Lock();
   int myId = count++;
   count_mu.Unlock();

   // all threads write to different memory locations
   for (int j = 0; j < ITERATIONS; j++) {
      int mutex_mask = j & MUTEX_ID_BITS;
      for (int m = 0; m < MUTEX_ID_BITS; m++)
         if (mutex_mask & (1 << m))
            mutex_arr[myId][m].Lock();

      for (int i = 0; i < ARRAY_SIZE; i++) {
         array[myId][i] = i;
      }

      for (int m = 0; m < MUTEX_ID_BITS; m++)
         if (mutex_mask & (1 << m))
            mutex_arr[myId][m].Unlock();
   }
}

void Run() {
   printf("test513: --fast-mode benchmark\n");
   {
      ThreadPool pool(N_THREADS);
      pool.StartWorkers();
      for (int i = 0; i < N_THREADS; i++) {
         pool.Add(NewCallback(Worker));
      }
   } // all folks are joined here.
}

REGISTER_TEST2(Run, 513, PERFORMANCE | PRINT_STATS | EXCLUDE_FROM_ALL)
}  // namespace test513

namespace ThreadChainTest {  // {{{1 Reg test for thread creation
void Thread1() { }
void Thread2() {
  MyThread t(Thread1);
  t.Start();
  t.Join();
}
void Thread3() {
  MyThread t(Thread2);
  t.Start();
  t.Join();
}
void Thread4() {
  MyThread t(Thread3);
  t.Start();
  t.Join();
}

TEST(RegTests, ThreadChainTest) {
  Thread4();
}

}  // namespace

#ifndef ANDROID // GTest does not support ASSERT_DEBUG_DEATH.
namespace SimpleDeathTest {  // {{{1 Make sure that the tool handles death tests correctly
#ifdef WIN32
TEST(DeathTests, DISABLED_SimpleDeathTest) {
#else
TEST(DeathTests, SimpleDeathTest) {
#endif
  ASSERT_DEBUG_DEATH(CHECK(false), "");
}
}  // namespace
#endif

namespace IgnoreTests {  // {{{1 Test how the tool works with indirect calls to fun_r functions
int GLOB = 0;
void (*f)() = NULL;

void NotIgnoredRacey() {
  GLOB++;
}

void FunRFunction() {
  NotIgnoredRacey();
  usleep(1); // avoid tail call elimination
}

void DoDirectCall() {
  FunRFunction();
  usleep(1); // avoid tail call elimination
}

void DoIndirectCall() {
  (*f)();
  usleep(1); // avoid tail call elimination
}

TEST(IgnoreTests, DirectCallToFunR) {
  MyThreadArray mta(DoDirectCall, DoDirectCall);
  mta.Start();
  mta.Join();
}

TEST(IgnoreTests, IndirectCallToFunR) {
  f = FunRFunction;
  MyThreadArray mta(DoIndirectCall, DoIndirectCall);
  mta.Start();
  mta.Join();
}
}  // namespace

namespace MutexNotPhbTests {

int GLOB = 0;
Mutex mu;
StealthNotification n;

void SignalThread() {
  GLOB = 1;
  mu.Lock();
  mu.Unlock();
  n.signal();
}

void WaitThread() {
  n.wait();
  mu.Lock();
  mu.Unlock();
  GLOB = 2;
}

TEST(MutexNotPhbTests, MutexNotPhbTest) {
  ANNOTATE_NOT_HAPPENS_BEFORE_MUTEX(&mu);
  ANNOTATE_EXPECT_RACE(&GLOB, "MutexNotPhbTest. TP.");
  MyThreadArray mta(SignalThread, WaitThread);
  mta.Start();
  mta.Join();
}
} // namespace

namespace RaceVerifierTests_Simple {
int     GLOB = 0;

void Worker1() {
  GLOB = 1;
}

void Worker2() {
  GLOB = 2;
}

TEST(RaceVerifierTests, Simple) {
  ANNOTATE_EXPECT_RACE(&GLOB, "SimpleRace.");
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
}
}  // namespace

namespace RaceVerifierTests_Unverifiable {
StealthNotification n;
int GLOB = 0;

void Worker1() {
  if (!GLOB)
    GLOB = 1;
  n.signal();
}

void Worker2() {
  n.wait();
  GLOB = 2;
}

TEST(RaceVerifierTests, Unverifiable) {
  ANNOTATE_EXPECT_RACE(&GLOB, "SimpleRace. UNVERIFIABLE.");
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
}
}  // namespace


namespace RaceVerifierTests_ManyRacesInOneTrace {
StealthNotification n;
int     array[2];

void Worker1() {
  array[0] = 1;
  array[1] = 2;
}

void Worker2() {
  array[1] = array[0];
}

TEST(RaceVerifierTests, ManyRacesInOneTrace) {
  ANNOTATE_EXPECT_RACE(array + 0, "RaceVerifierTests_ManyRacesInOneTrace: race 1.");
  ANNOTATE_EXPECT_RACE(array + 1, "RaceVerifierTests_ManyRacesInOneTrace: race 2.");
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
}
}  // namespace

namespace PrintfTests_Simple {

void Worker1() {
  // This one is a printf() => vfprintf()
  fprintf(stderr, "Hello from a thread: %d\n", 2);
  // This one is a puts()
  fprintf(stderr, "Hello from a thread\n");
  fprintf(stdout, "Hello from a thread: %d\n", 2);
  fprintf(stdout, "Hello from a thread\n");
}

TEST(PrintfTests, DISABLED_Simple) {
  MyThreadArray t(Worker1, Worker1);
  t.Start();
  t.Join();
}
}  // namespace

namespace PrintfTests_RaceOnFwriteArgument {

char s[] = "abracadabra\n";

void Worker1() {
  fwrite(s, 1, sizeof(s) - 1, stdout);
}

void Worker2() {
  s[3] = 'z';
}

TEST(PrintfTests, RaceOnFwriteArgument) {
  ANNOTATE_TRACE_MEMORY(s + 3);
  ANNOTATE_EXPECT_RACE(s + 3, "PrintfTests_RaceOnFwriteArgument.");
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
}
}  // namespace

namespace PrintfTests_RaceOnPutsArgument {

char s[] = "abracadabra";

void Worker1() {
  puts(s);
}

void Worker2() {
  s[3] = 'z';
}

TEST(PrintfTests, RaceOnPutsArgument) {
  ANNOTATE_TRACE_MEMORY(s + 3);
  ANNOTATE_EXPECT_RACE(s + 3, "PrintfTests_RaceOnPutsArgument.");
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
}
}  // namespace

namespace PrintfTests_RaceOnPrintfArgument {

volatile char s[] = "abracadabra";
volatile char s2[] = "abracadabra";

void Worker1() {
  fprintf(stdout, "printing a string: %s\n", s);
  fprintf(stderr, "printing a string: %s\n", s2);
}

void Worker2() {
  s[3] = 'z';
  s2[3] = 'z';
}

TEST(PrintfTests, DISABLED_RaceOnPrintfArgument) {
  ANNOTATE_EXPECT_RACE(s + 3, "PrintfTests_RaceOnPrintfArgument (stdout).");
  ANNOTATE_EXPECT_RACE(s2 + 3, "PrintfTests_RaceOnPrintfArgument (stderr).");
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
}
}  // namespace

// Apparently, %n is not supported in windows
#ifndef WIN32
namespace PrintfTests_RaceOnOutputArgument {

volatile char s[] = "abracadabra";
volatile int a = 0;

void Worker1() {
  fprintf(stdout, "printing a string: %s%n\n", s, &a);
}

void Worker2() {
  fprintf(stdout, "the other thread have already printed %d characters\n", a);
}

TEST(PrintfTests, DISABLED_RaceOnOutputArgument) {
  ANNOTATE_EXPECT_RACE(&a, "PrintfTests_RaceOnOutputArgument:int.");
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
}
}  // namespace
#endif

namespace PrintfTests_Fflush {

volatile char s[] = "abracadabra";
volatile int a = 0;

void Worker1() {
  fflush(NULL);
}

void Worker2() {
  fflush(NULL);
}

TEST(PrintfTests, DISABLED_Fflush) {
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
}
}  // namespace

namespace BenignRaceTest {  // {{{1
const int kArrayLen = 97;
char X[kArrayLen];
char *P;

int counter;


void Worker() {
  (*P)++;
  ANNOTATE_HAPPENS_BEFORE(P);
  AtomicIncrement(&counter, -1);
}

TEST(NegativeTests, BenignRaceTest) {
  ThreadPool pool1(1);
  ThreadPool pool2(1);
  ThreadPool pool3(1);
  pool1.StartWorkers();
  pool2.StartWorkers();
  pool3.StartWorkers();

  ANNOTATE_BENIGN_RACE(&counter, "");
  const int kNIter = 1000;

  for (int i = 0; i < kNIter; i++) {
    counter = 3;
    long len = (i % (kArrayLen / 3)) + 1;
    long beg = i % (kArrayLen - len);
    long end = beg + len;
    CHECK(beg < kArrayLen);
    CHECK(end <= kArrayLen);
    bool is_expected = i % 2;
    long pos = i % len;
    P = X + beg + pos;
    CHECK(P < X + kArrayLen);
    // printf("[%d] b=%ld e=%ld p=%ld is_expected=%d\n",
    //       i, beg, end, pos, is_expected);
    ANNOTATE_NEW_MEMORY(X, kArrayLen);
    if (is_expected) {
      ANNOTATE_EXPECT_RACE(P, "expected race in BenignRaceTest");
    } else {
      ANNOTATE_BENIGN_RACE_SIZED(X + beg, len, "");
    }
    if ((i % (kNIter / 10)) == 0) {
      ANNOTATE_FLUSH_STATE();
    }
    pool1.Add(NewCallback(Worker));
    pool2.Add(NewCallback(Worker));
    pool3.Add(NewCallback(Worker));

    while(AtomicIncrement(&counter, 0) != 0)
      usleep(1000);
    ANNOTATE_HAPPENS_AFTER(P);

    ANNOTATE_FLUSH_EXPECTED_RACES();
  }
}
}

namespace StressTests_FlushStateTest {  // {{{1
// Stress test for FlushState which happens in parallel with some work.
const int N = 1000;
int array[N];

void Flusher() {
  for (int i = 0; i < 10; i++) {
    usleep(1000);
    ANNOTATE_FLUSH_STATE();
  }
}

void Write1(int i) { array[i]++; }
void Write2(int i) { array[i]--; }
int Read1(int i) { volatile int z = array[i]; return z; }
int Read2(int i) { volatile int z = array[i]; return z; }

void Worker() {
  for (int iter = 0; iter < 10; iter++) {
    usleep(1000);
    for (int i = 0; i < N; i++) {
      Write1(i);
      Write2(i);
      Read1(i);
      Read2(i);
    }
  }
}

TEST(StressTests, FlushStateTest) {
  MyThreadArray t(Flusher, Worker, Worker, Worker);
  t.Start();
  t.Join();
}

}  // namespace

// End {{{1
 // vim:shiftwidth=2:softtabstop=2:expandtab:foldmethod=marker
