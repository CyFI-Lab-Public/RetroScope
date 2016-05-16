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
   Author: Timur Iskhodzhanov <opensource@google.com>

 This file contains a set of demonstration tests for ThreadSanitizer.
*/

#include <gtest/gtest.h>

#include "test_utils.h"

int main(int argc, char** argv) {
  testing::InitGoogleTest(&argc, argv);
  return RUN_ALL_TESTS();
}

namespace RaceReportDemoTest {  // {{{1
Mutex mu1;  // This Mutex guards var.
Mutex mu2;  // This Mutex is not related to var.
int   var;  // GUARDED_BY(mu1)

void Thread1() {  // Runs in thread named 'test-thread-1'.
  MutexLock lock(&mu1);  // Correct Mutex.
  var = 1;
}

void Thread2() {  // Runs in thread named 'test-thread-2'.
  MutexLock lock(&mu2);  // Wrong Mutex.
  var = 2;
}

TEST(DemoTests, RaceReportDemoTest) {
  ANNOTATE_TRACE_MEMORY(&var);
  var = 0;
  MyThread t1(Thread1, NULL, "test-thread-1");
  MyThread t2(Thread2, NULL, "test-thread-2");
  t1.Start();
  t2.Start();
  t1.Join();
  t2.Join();
}
}  // namespace RaceReportDemoTest

// test302: Complex race which happens at least twice.  {{{1
namespace test302 {
// In this test we have many different accesses to GLOB and only one access
// is not synchronized properly.
int     GLOB = 0;

Mutex MU1;
Mutex MU2;
void Worker() {
  for(int i = 0; i < 100; i++) {
    switch(i % 4) {
      case 0:
        // This read is protected correctly.
        MU1.Lock(); CHECK(GLOB >= 0); MU1.Unlock();
        break;
      case 1:
        // Here we used the wrong lock! The reason of the race is here.
        MU2.Lock(); CHECK(GLOB >= 0); MU2.Unlock();
        break;
      case 2:
        // This read is protected correctly.
        MU1.Lock(); CHECK(GLOB >= 0); MU1.Unlock();
        break;
      case 3:
        // This write is protected correctly.
        MU1.Lock(); GLOB++; MU1.Unlock();
        break;
    }
    // sleep a bit so that the threads interleave
    // and the race happens at least twice.
    usleep(100);
  }
}

TEST(DemoTests, test302) {
  printf("test302: Complex race that happens twice.\n");
  MyThread t1(Worker), t2(Worker);
  t1.Start();
  t2.Start();
  t1.Join();   t2.Join();
}
}  // namespace test302


// test303: Need to trace the memory to understand the report. {{{1
namespace test303 {
int     GLOB = 0;

Mutex MU;
void Worker1() { CHECK(GLOB >= 0); }
void Worker2() { MU.Lock(); GLOB=1;  MU.Unlock();}

TEST(DemoTests, test303) {
  printf("test303: a race that needs annotations.\n");
  ANNOTATE_TRACE_MEMORY(&GLOB);
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
}
}  // namespace test303



// test304: Can not trace the memory, since it is a library object. {{{1
namespace test304 {
string *STR;
Mutex   MU;

void Worker1() {
  sleep(0);
  ANNOTATE_CONDVAR_SIGNAL((void*)0xDEADBEAF);
  MU.Lock(); CHECK(STR->length() >= 4); MU.Unlock();
}
void Worker2() {
  sleep(1);
  ANNOTATE_CONDVAR_SIGNAL((void*)0xDEADBEAF);
  CHECK(STR->length() >= 4); // Unprotected!
}
void Worker3() {
  sleep(2);
  ANNOTATE_CONDVAR_SIGNAL((void*)0xDEADBEAF);
  MU.Lock(); CHECK(STR->length() >= 4); MU.Unlock();
}
void Worker4() {
  sleep(3);
  ANNOTATE_CONDVAR_SIGNAL((void*)0xDEADBEAF);
  MU.Lock(); *STR += " + a very very long string"; MU.Unlock();
}

TEST(DemoTests, test304) {
  STR = new string ("The String");
  printf("test304: a race where memory tracing does not work.\n");
  MyThreadArray t(Worker1, Worker2, Worker3, Worker4);
  t.Start();
  t.Join();

  printf("%s\n", STR->c_str());
  delete STR;
}
}  // namespace test304



// test305: A bit more tricky: two locks used inconsistenly. {{{1
namespace test305 {
int     GLOB = 0;

// In this test GLOB is protected by MU1 and MU2, but inconsistently.
// The TRACES observed by helgrind are:
// TRACE[1]: Access{T2/S2 wr} -> new State{Mod; #LS=2; #SS=1; T2/S2}
// TRACE[2]: Access{T4/S9 wr} -> new State{Mod; #LS=1; #SS=2; T2/S2, T4/S9}
// TRACE[3]: Access{T5/S13 wr} -> new State{Mod; #LS=1; #SS=3; T2/S2, T4/S9, T5/S13}
// TRACE[4]: Access{T6/S19 wr} -> new State{Mod; #LS=0; #SS=4; T2/S2, T4/S9, T5/S13, T6/S19}
//
// The guilty access is either Worker2() or Worker4(), depending on
// which mutex is supposed to protect GLOB.
Mutex MU1;
Mutex MU2;
void Worker1() { MU1.Lock(); MU2.Lock(); GLOB=1; MU2.Unlock(); MU1.Unlock(); }
void Worker2() { MU1.Lock();             GLOB=2;               MU1.Unlock(); }
void Worker3() { MU1.Lock(); MU2.Lock(); GLOB=3; MU2.Unlock(); MU1.Unlock(); }
void Worker4() {             MU2.Lock(); GLOB=4; MU2.Unlock();               }

TEST(DemoTests, test305) {
  ANNOTATE_TRACE_MEMORY(&GLOB);
  printf("test305: simple race.\n");
  MyThread t1(Worker1), t2(Worker2), t3(Worker3), t4(Worker4);
  t1.Start(); usleep(100);
  t2.Start(); usleep(100);
  t3.Start(); usleep(100);
  t4.Start(); usleep(100);
  t1.Join(); t2.Join(); t3.Join(); t4.Join();
}
}  // namespace test305

// test306: Two locks are used to protect a var.  {{{1
namespace test306 {
int     GLOB = 0;
// Thread1 and Thread2 access the var under two locks.
// Thread3 uses no locks.

Mutex MU1;
Mutex MU2;
void Worker1() { MU1.Lock(); MU2.Lock(); GLOB=1; MU2.Unlock(); MU1.Unlock(); }
void Worker2() { MU1.Lock(); MU2.Lock(); GLOB=3; MU2.Unlock(); MU1.Unlock(); }
void Worker3() {                         GLOB=4;               }

TEST(DemoTests, test306) {
  ANNOTATE_TRACE_MEMORY(&GLOB);
  printf("test306: simple race.\n");
  MyThread t1(Worker1), t2(Worker2), t3(Worker3);
  t1.Start(); usleep(100);
  t2.Start(); usleep(100);
  t3.Start(); usleep(100);
  t1.Join(); t2.Join(); t3.Join();
}
}  // namespace test306

// test307: Simple race, code with control flow  {{{1
namespace test307 {
int     *GLOB = 0;
volatile /*to fake the compiler*/ bool some_condition = true;


void SomeFunc() { }

int FunctionWithControlFlow() {
  int unrelated_stuff = 0;
  unrelated_stuff++;
  SomeFunc();                // "--keep-history=1" will point somewhere here.
  if (some_condition) {      // Or here
    if (some_condition) {
      unrelated_stuff++;     // Or here.
      unrelated_stuff++;
      (*GLOB)++;             // "--keep-history=2" will point here (experimental).
    }
  }
  usleep(100000);
  return unrelated_stuff;
}

void Worker1() { FunctionWithControlFlow(); }
void Worker2() { Worker1(); }
void Worker3() { Worker2(); }
void Worker4() { Worker3(); }

TEST(DemoTests, test307) {
  GLOB = new int;
  *GLOB = 1;
  printf("test307: simple race, code with control flow\n");
  MyThreadArray t1(Worker1, Worker2, Worker3, Worker4);
  t1.Start();
  t1.Join();
}
}  // namespace test307

// test308: Example of double-checked-locking  {{{1
namespace test308 {
struct Foo {
  int a;
};

static int   is_inited = 0;
static Mutex lock;
static Foo  *foo;

void InitMe() {
  if (!is_inited) {
    lock.Lock();
      if (!is_inited) {
        foo = new Foo;
        foo->a = 42;
        is_inited = 1;
      }
    lock.Unlock();
  }
}

void UseMe() {
  InitMe();
  CHECK(foo && foo->a == 42);
}

void Worker1() { UseMe(); }
void Worker2() { UseMe(); }
void Worker3() { UseMe(); }


TEST(DemoTests, test308) {
  ANNOTATE_TRACE_MEMORY(&is_inited);
  printf("test308: Example of double-checked-locking\n");
  MyThreadArray t1(Worker1, Worker2, Worker3);
  t1.Start();
  t1.Join();
}
}  // namespace test308

// test309: Simple race on an STL object.  {{{1
namespace test309 {
string  GLOB;

void Worker1() {
  GLOB="Thread1";
}
void Worker2() {
  usleep(100000);
  GLOB="Booooooooooo";
}

TEST(DemoTests, test309) {
  printf("test309: simple race on an STL object.\n");
  MyThread t1(Worker1), t2(Worker2);
  t1.Start();
  t2.Start();
  t1.Join();   t2.Join();
}
}  // namespace test309

// test310: One more simple race.  {{{1
namespace test310 {
int     *PTR = NULL;  // GUARDED_BY(mu1)

Mutex mu1;  // Protects PTR.
Mutex mu2;  // Unrelated to PTR.
Mutex mu3;  // Unrelated to PTR.

void Writer1() {
  MutexLock lock3(&mu3);  // This lock is unrelated to PTR.
  MutexLock lock1(&mu1);  // Protect PTR.
  *PTR = 1;
}

void Writer2() {
  MutexLock lock2(&mu2);  // This lock is unrelated to PTR.
  MutexLock lock1(&mu1);  // Protect PTR.
  int some_unrelated_stuff = 0;
  if (some_unrelated_stuff == 0)
    some_unrelated_stuff++;
  *PTR = 2;
}


void Reader() {
  MutexLock lock2(&mu2);  // Oh, gosh, this is a wrong mutex!
  CHECK(*PTR <= 2);
}

// Some functions to make the stack trace non-trivial.
void DoWrite1() { Writer1();  }
void Thread1()  { DoWrite1(); }

void DoWrite2() { Writer2();  }
void Thread2()  { DoWrite2(); }

void DoRead()  { Reader();  }
void Thread3() { DoRead();  }

TEST(DemoTests, test310) {
  printf("test310: simple race.\n");
  PTR = new int;
  ANNOTATE_TRACE_MEMORY(PTR);
  *PTR = 0;
  MyThread t1(Thread1, NULL, "writer1"),
           t2(Thread2, NULL, "writer2"),
           t3(Thread3, NULL, "buggy reader");
  t1.Start();
  t2.Start();
  usleep(100000);  // Let the writers go first.
  t3.Start();

  t1.Join();
  t2.Join();
  t3.Join();
}
}  // namespace test310

// test311: Yet another simple race.  {{{1
namespace test311 {
int     *PTR = NULL;  // GUARDED_BY(mu1)

Mutex mu1;  // Protects PTR.
Mutex mu2;  // Unrelated to PTR.
Mutex mu3;  // Unrelated to PTR.

void GoodWriter1() {      // Runs in Thread1
  MutexLock lock3(&mu3);  // This lock is unrelated to PTR.
  MutexLock lock1(&mu1);  // Protect PTR.
  *PTR = 1;
}

void GoodWriter2() {      // Runs in Thread2
  MutexLock lock2(&mu2);  // This lock is unrelated to PTR.
  MutexLock lock1(&mu1);  // Protect PTR.
  *PTR = 2;
}

void GoodReader() {      // Runs in Thread3
  MutexLock lock1(&mu1);  // Protect PTR.
  CHECK(*PTR >= 0);
}

void BuggyWriter() {      // Runs in Thread4
  MutexLock lock2(&mu2);  // Wrong mutex!
  *PTR = 3;
}

// Some functions to make the stack trace non-trivial.
void DoWrite1() { GoodWriter1();  }
void Thread1()  { DoWrite1(); }

void DoWrite2() { GoodWriter2();  }
void Thread2()  { DoWrite2(); }

void DoGoodRead()  { GoodReader();  }
void Thread3()     { DoGoodRead();  }

void DoBadWrite()  { BuggyWriter(); }
void Thread4()     { DoBadWrite(); }

TEST(DemoTests, test311) {
  printf("test311: simple race.\n");
  PTR = new int;
  ANNOTATE_TRACE_MEMORY(PTR);
  *PTR = 0;
  MyThread t1(Thread1, NULL, "good writer1"),
           t2(Thread2, NULL, "good writer2"),
           t3(Thread3, NULL, "good reader"),
           t4(Thread4, NULL, "buggy writer");
  t1.Start();
  t3.Start();
  // t2 goes after t3. This way a pure happens-before detector has no chance.
  usleep(10000);
  t2.Start();
  usleep(100000);  // Let the good folks go first.
  t4.Start();

  t1.Join();
  t2.Join();
  t3.Join();
  t4.Join();
}
}  // namespace test311

// test312: A test with a very deep stack. {{{1
namespace test312 {
int     GLOB = 0;
void RaceyWrite() { GLOB++; }
void Func1() { RaceyWrite(); }
void Func2() { Func1(); }
void Func3() { Func2(); }
void Func4() { Func3(); }
void Func5() { Func4(); }
void Func6() { Func5(); }
void Func7() { Func6(); }
void Func8() { Func7(); }
void Func9() { Func8(); }
void Func10() { Func9(); }
void Func11() { Func10(); }
void Func12() { Func11(); }
void Func13() { Func12(); }
void Func14() { Func13(); }
void Func15() { Func14(); }
void Func16() { Func15(); }
void Func17() { Func16(); }
void Func18() { Func17(); }
void Func19() { Func18(); }
void Worker() { Func19(); }
TEST(DemoTests, test312) {
  printf("test312: simple race with deep stack.\n");
  MyThreadArray t(Worker, Worker, Worker);
  t.Start();
  t.Join();
}
}  // namespace test312

// test313 TP: test for thread graph output {{{1
namespace  test313 {
BlockingCounter *blocking_counter;
int     GLOB = 0;

// Worker(N) will do 2^N increments of GLOB, each increment in a separate thread
void Worker(int depth) {
  CHECK(depth >= 0);
  if (depth > 0) {
    ThreadPool pool(2);
    pool.StartWorkers();
    pool.Add(NewCallback(Worker, depth-1));
    pool.Add(NewCallback(Worker, depth-1));
  } else {
    GLOB++; // Race here
  }
}
TEST(DemoTests, test313) {
  printf("test313: positive\n");
  Worker(4);
  printf("\tGLOB=%d\n", GLOB);
}
}  // namespace test313

// test314: minimalistic test for race in vptr. {{{1
namespace test314 {
// Race on vptr. Will run A::F() or B::F() depending on the timing.
class A {
 public:
  A() : done_(false) { }
  virtual void F() {
    printf ("A::F()\n");
  }
  void Done() {
    MutexLock lock(&mu_);
    done_ = true;
  }
  virtual ~A() {
    while (true) {
      MutexLock lock(&mu_);
      if (done_) break;
    }
  }
 private:
  Mutex mu_;
  bool  done_;
};

class B : public A {
 public:
  virtual void F() {
    printf ("B::F()\n");
  }
};

static A *a;

void Thread1() {
  a->F();
  a->Done();
};

void Thread2() {
  delete a;
}
TEST(DemoTests, test314) {
  printf("test314: race on vptr; May print A::F() or B::F().\n");
  { // Will print B::F()
    a = new B;
    MyThreadArray t(Thread1, Thread2);
    t.Start();
    t.Join();
  }
  { // Will print A::F()
    a = new B;
    MyThreadArray t(Thread2, Thread1);
    t.Start();
    t.Join();
  }
}
}  // namespace test314

// test315: demo for hybrid's false positive. {{{1
namespace test315 {
int GLOB;
Mutex mu;
int flag;

void Thread1() {
  sleep(1);
  mu.Lock();
  bool f = flag;
  mu.Unlock();
  if (f) {
    GLOB++;
  }
}

void Thread2() {
  GLOB++;
  mu.Lock();
  flag = true;
  mu.Unlock();
}

TEST(DemoTests, test315) {
  GLOB = 0;
  printf("test315: false positive of the hybrid state machine\n");
  MyThreadArray t(Thread1, Thread2);
  t.Start();
  t.Join();
}
}  // namespace test315
