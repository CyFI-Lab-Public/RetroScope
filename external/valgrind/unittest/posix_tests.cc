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
#ifdef WIN32
#error "Don't build this file on Windows!"
#endif

#include <fcntl.h>
#include <fenv.h>
#include <queue>
#include <signal.h>
#include <stdlib.h>
#include <string>
#include <vector>
#include <unistd.h>

#ifdef OS_linux
# include <sys/epoll.h>
#endif  // OS_linux

#include "test_utils.h"
#include <gtest/gtest.h>
#include "gtest_fixture_injection.h"

static CondVar CV;
static int     COND = 0;

// test11: FP. Synchronization via CondVar, 2 workers. {{{1
// This test is properly synchronized, but currently (Dec 2007)
// helgrind reports a false positive.
//
// Parent:                              Worker1, Worker2:
// 1. Start(workers)                    a. read(GLOB)
// 2. MU.Lock()                         b. MU.Lock()
// 3. while(COND != 2)        /-------- c. CV.Signal()
//      CV.Wait(&MU) <-------/          d. MU.Unlock()
// 4. MU.Unlock()
// 5. write(GLOB)
//
namespace test11 {
int     GLOB = 0;
Mutex   MU;
void Worker() {
  usleep(200000);
  CHECK(GLOB != 777);

  MU.Lock();
  COND++;
  CV.Signal();
  MU.Unlock();
}

void Parent() {
  COND = 0;

  MyThreadArray t(Worker, Worker);
  t.Start();

  MU.Lock();
  while(COND != 2) {
    CV.Wait(&MU);
  }
  MU.Unlock();

  GLOB = 2;

  t.Join();
}

TEST(NegativeTests, test11) {
//  ANNOTATE_EXPECT_RACE(&GLOB, "test11. FP. Fixed by MSMProp1.");
  printf("test11: negative\n");
  Parent();
  printf("\tGLOB=%d\n", GLOB);
}
}  // namespace test11


// test75: TN. Test for sem_post, sem_wait, sem_trywait. {{{1
namespace test75 {
int     GLOB = 0;
sem_t   sem[2];

void Poster() {
  GLOB = 1;
  sem_post(&sem[0]);
  sem_post(&sem[1]);
}

void Waiter() {
  sem_wait(&sem[0]);
  CHECK(GLOB==1);
}
void TryWaiter() {
  usleep(500000);
  sem_trywait(&sem[1]);
  CHECK(GLOB==1);
}

TEST(NegativeTests, test75) {
#ifndef NO_UNNAMED_SEM
  sem_init(&sem[0], 0, 0);
  sem_init(&sem[1], 0, 0);

  printf("test75: negative\n");
  {
    MyThreadArray t(Poster, Waiter);
    t.Start();
    t.Join();
  }
  GLOB = 2;
  {
    MyThreadArray t(Poster, TryWaiter);
    t.Start();
    t.Join();
  }
  printf("\tGLOB=%d\n", GLOB);

  sem_destroy(&sem[0]);
  sem_destroy(&sem[1]);
#endif // NO_UNNAMED_SEM
}
}  // namespace test75


// test98: Synchronization via read/write (or send/recv). {{{1
namespace test98 {
// The synchronization here is done by a pair of read/write calls
// that create a happens-before arc. Same may be done with send/recv.
// Such synchronization is quite unusual in real programs
// (why would one synchronizae via a file or socket?), but
// quite possible in unittests where one threads runs for producer
// and one for consumer.
//
// A race detector has to create a happens-before arcs for
// {read,send}->{write,recv} even if the file descriptors are different.
//
int     GLOB = 0;
int fd_out = -1;
int fd_in  = -1;

void Writer() {
  usleep(1000);
  GLOB = 1;
  const char *str = "Hey there!\n";
  const int size = strlen(str) + 1;
  CHECK(size == write(fd_out, str, size));
}

void Reader() {
  char buff[100];
  while (read(fd_in, buff, 100) == 0)
    sleep(1);
  printf("read: %s\n", buff);
  GLOB = 2;
}

#ifndef __APPLE__
// Tsan for Mac OS is missing the unlink() syscall handler.
// TODO(glider): add the syscall handler to Valgrind.
TEST(NegativeTests, test98) {
  printf("test98: negative, synchronization via I/O\n");
  char in_name[100];
  char out_name[100];
  // we open two files, on for reading and one for writing,
  // but the files are actually the same (symlinked).
  sprintf(in_name,  "/tmp/racecheck_unittest_in.%d", getpid());
  sprintf(out_name, "/tmp/racecheck_unittest_out.%d", getpid());
  fd_out = creat(out_name, O_WRONLY | S_IRWXU);
  CHECK(0 == symlink(out_name, in_name));
  fd_in  = open(in_name, 0, O_RDONLY);
  CHECK(fd_out >= 0);
  CHECK(fd_in  >= 0);
  MyThreadArray t(Writer, Reader);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
  // cleanup
  close(fd_in);
  close(fd_out);
  unlink(in_name);
  unlink(out_name);
}
#endif  // __APPLE__
}  // namespace test98


namespace NegativeTests_PthreadOnce {  // {{{1
int     *GLOB = NULL;
static pthread_once_t once = PTHREAD_ONCE_INIT;
void Init() {
  GLOB = new int;
  ANNOTATE_TRACE_MEMORY(GLOB);
  *GLOB = 777;
}

void Worker0() {
  pthread_once(&once, Init);
}
void Worker1() {
  usleep(100000);
  pthread_once(&once, Init);
  CHECK(*GLOB == 777);
}


TEST(NegativeTests, PthreadOnceTest) {
  MyThreadArray t(Worker0, Worker1, Worker1, Worker1);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", *GLOB);
}
}  // namespace


// test110: TP. Simple races with stack, global and heap objects. {{{1
namespace test110 {
int        GLOB = 0;
static int STATIC;

char       *STACK = 0;

int       *MALLOC;
int       *CALLOC;
int       *REALLOC;
int       *VALLOC;
int       *PVALLOC;
int       *MEMALIGN;
int       *POSIX_MEMALIGN;
int       *MMAP;

int       *NEW;
int       *NEW_ARR;

void Worker() {
  GLOB++;
  STATIC++;

  (*STACK)++;

  (*MALLOC)++;
  (*CALLOC)++;
  (*REALLOC)++;
  (*VALLOC)++;
  (*PVALLOC)++;
  (*MEMALIGN)++;
  (*POSIX_MEMALIGN)++;
  (*MMAP)++;

  (*NEW)++;
  (*NEW_ARR)++;
}
TEST(PositiveTests, test110) {
  printf("test110: positive (race on a stack object)\n");

  char x = 0;
  STACK = &x;

  MALLOC = (int*)malloc(sizeof(int));
  CALLOC = (int*)calloc(1, sizeof(int));
  REALLOC = (int*)realloc(NULL, sizeof(int));
  VALLOC = (int*)valloc(sizeof(int));
  PVALLOC = (int*)valloc(sizeof(int));  // TODO: pvalloc breaks helgrind.
  MEMALIGN = (int*)memalign(64, sizeof(int));
  CHECK(0 == posix_memalign((void**)&POSIX_MEMALIGN, 64, sizeof(int)));
  MMAP = (int*)mmap(NULL, sizeof(int), PROT_READ | PROT_WRITE,
                    MAP_PRIVATE | MAP_ANON, -1, 0);

  NEW     = new int;
  NEW_ARR = new int[10];


  ANNOTATE_EXPECT_RACE(STACK, "real race on stack object");
  ANNOTATE_EXPECT_RACE(&GLOB, "real race on global object");
  ANNOTATE_EXPECT_RACE(&STATIC, "real race on a static global object");
  ANNOTATE_EXPECT_RACE(MALLOC, "real race on a malloc-ed object");
  ANNOTATE_EXPECT_RACE(CALLOC, "real race on a calloc-ed object");
  ANNOTATE_EXPECT_RACE(REALLOC, "real race on a realloc-ed object");
  ANNOTATE_EXPECT_RACE(VALLOC, "real race on a valloc-ed object");
  ANNOTATE_EXPECT_RACE(PVALLOC, "real race on a pvalloc-ed object");
  ANNOTATE_EXPECT_RACE(MEMALIGN, "real race on a memalign-ed object");
  ANNOTATE_EXPECT_RACE(POSIX_MEMALIGN, "real race on a posix_memalign-ed object");
  ANNOTATE_EXPECT_RACE(MMAP, "real race on a mmap-ed object");

  ANNOTATE_EXPECT_RACE(NEW, "real race on a new-ed object");
  ANNOTATE_EXPECT_RACE(NEW_ARR, "real race on a new[]-ed object");

  MyThreadArray t(Worker, Worker, Worker);
  t.Start();
  t.Join();
  printf("\tSTACK=%d\n", *STACK);
  CHECK(GLOB <= 3);
  CHECK(STATIC <= 3);

  free(MALLOC);
  free(CALLOC);
  free(REALLOC);
  free(VALLOC);
  free(PVALLOC);
  free(MEMALIGN);
  free(POSIX_MEMALIGN);
  munmap(MMAP, sizeof(int));
  delete NEW;
  delete [] NEW_ARR;
}
}  // namespace test110


// test115: TN. sem_open. {{{1
namespace    test115 {
int tid = 0;
Mutex mu;
const char *kSemName = "drt-test-sem";

int GLOB = 0;

sem_t *DoSemOpen() {
  // TODO: there is some race report inside sem_open
  // for which suppressions do not work... (???)
  ANNOTATE_IGNORE_WRITES_BEGIN();
  sem_t *sem = sem_open(kSemName, O_CREAT, 0600, 3);
  ANNOTATE_IGNORE_WRITES_END();
  return sem;
}

void Worker() {
  mu.Lock();
  int my_tid = tid++;
  mu.Unlock();

  if (my_tid == 0) {
    GLOB = 1;
  }

  // if the detector observes a happens-before arc between
  // sem_open and sem_wait, it will be silent.
  sem_t *sem = DoSemOpen();
  usleep(100000);
  CHECK(sem != SEM_FAILED);
  CHECK(sem_wait(sem) == 0);

  if (my_tid > 0) {
    CHECK(GLOB == 1);
  }
}

#ifndef __APPLE__
/* This test is disabled for Darwin because of the tricky implementation of
 * sem_open on that platform: subsequent attempts to open an existing semafore
 * create new ones. */
TEST(NegativeTests, test115) {
  printf("test115: stab (sem_open())\n");

  // just check that sem_open is not completely broken
  sem_unlink(kSemName);
  sem_t* sem = DoSemOpen();
  CHECK(sem != SEM_FAILED);
  CHECK(sem_wait(sem) == 0);
  sem_unlink(kSemName);

  // check that sem_open and sem_wait create a happens-before arc.
  MyThreadArray t(Worker, Worker, Worker);
  t.Start();
  t.Join();
  // clean up
  sem_unlink(kSemName);
}
#endif  // __APPLE__
}  // namespace test115


// test122 TP: Simple test with RWLock {{{1
namespace  test122 {
int     VAR1 = 0;
int     VAR2 = 0;
RWLock mu;

void WriteWhileHoldingReaderLock(int *p) {
  usleep(100000);
  ReaderLockScoped lock(&mu);  // Reader lock for writing. -- bug.
  (*p)++;
}

void CorrectWrite(int *p) {
  WriterLockScoped lock(&mu);
  (*p)++;
}

void Thread1() { WriteWhileHoldingReaderLock(&VAR1); }
void Thread2() { CorrectWrite(&VAR1); }
void Thread3() { CorrectWrite(&VAR2); }
void Thread4() { WriteWhileHoldingReaderLock(&VAR2); }


TEST(PositiveTests, test122) {
  printf("test122: positive (rw-lock)\n");
  VAR1 = 0;
  VAR2 = 0;
  ANNOTATE_TRACE_MEMORY(&VAR1);
  ANNOTATE_TRACE_MEMORY(&VAR2);
  if (!Tsan_PureHappensBefore()) {
    ANNOTATE_EXPECT_RACE_FOR_TSAN(&VAR1, "test122. TP. ReaderLock-ed while writing");
    ANNOTATE_EXPECT_RACE_FOR_TSAN(&VAR2, "test122. TP. ReaderLock-ed while writing");
  }
  MyThreadArray t(Thread1, Thread2, Thread3, Thread4);
  t.Start();
  t.Join();
}
}  // namespace test122


// test125 TN: Backwards lock (annotated). {{{1
namespace test125 {
// This test uses "Backwards mutex" locking protocol.
// We take a *reader* lock when writing to a per-thread data
// (GLOB[thread_num])  and we take a *writer* lock when we
// are reading from the entire array at once.
//
// Such locking protocol is not understood by ThreadSanitizer's
// hybrid state machine. So, you either have to use a pure-happens-before
// detector ("tsan --pure-happens-before") or apply pure happens-before mode
// to this particular lock by using ANNOTATE_PURE_HAPPENS_BEFORE_MUTEX(&mu).

const int n_threads = 3;
RWLock   mu;
int     GLOB[n_threads];

int adder_num; // updated atomically.

void Adder() {
  int my_num = AtomicIncrement(&adder_num, 1) - 1;
  CHECK(my_num >= 0);
  CHECK(my_num < n_threads);

  ReaderLockScoped lock(&mu);
  GLOB[my_num]++;
}

void Aggregator() {
  int sum = 0;
  {
    WriterLockScoped lock(&mu);
    for (int i = 0; i < n_threads; i++) {
      sum += GLOB[i];
    }
  }
  printf("sum=%d\n", sum);
}

TEST(NegativeTests, test125) {
  printf("test125: negative\n");

  ANNOTATE_PURE_HAPPENS_BEFORE_MUTEX(&mu);

  // run Adders, then Aggregator
  adder_num = 0;
  {
    MyThreadArray t(Adder, Adder, Adder, Aggregator);
    t.Start();
    t.Join();
  }

  // Run Aggregator first.
  adder_num = 0;
  {
    MyThreadArray t(Aggregator, Adder, Adder, Adder);
    t.Start();
    t.Join();
  }

}
}  // namespace test125


namespace MmapTest {  // {{{1

const int kMmapSize =  65536;

void SubWorker() {
  for (int i = 0; i < 500; i++) {
    int *ptr = (int*)mmap(NULL, kMmapSize, PROT_READ | PROT_WRITE,
                          MAP_PRIVATE | MAP_ANON, -1, 0);
    *ptr = 42;
    munmap(ptr, kMmapSize);
  }
}

void Worker1() {
  MyThreadArray t(SubWorker, SubWorker, SubWorker, SubWorker);
  t.Start();
  t.Join();
}
void Worker() {
  MyThreadArray t(Worker1, Worker1, Worker1, Worker1);
  t.Start();
  t.Join();
}

TEST(NegativeTests, MmapTest) {
  MyThreadArray t(Worker, Worker, Worker, Worker);
  t.Start();
  t.Join();
}
}  // namespace


// A regression test for mmap/munmap handling in Pin.
// If the tool misses munmap() calls it may report a false positive if two
// threads map the same memory region.
namespace MmapRegressionTest {  // {{{1

const int kMmapSize =  65536;
const uintptr_t kStartAddress = 0x10000;

StealthNotification n1;

void Worker() {
    int *ptr = (int*)mmap((void*)kStartAddress, kMmapSize,
                          PROT_READ | PROT_WRITE,
                          MAP_PRIVATE | MAP_ANON, -1, 0);
    *ptr = 42;
    munmap(ptr, kMmapSize);
}

TEST(NegativeTests, MmapRegressionTest) {
  MyThreadArray t(Worker, Worker);
  t.Start();
  t.Join();
}

}  // namespace

// test136. Unlock twice. {{{1
namespace test136 {
TEST(LockTests, UnlockTwice) {
  pthread_mutexattr_t attr;
  CHECK(0 == pthread_mutexattr_init(&attr));
  CHECK(0 == pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_ERRORCHECK));

  pthread_mutex_t mu;
  CHECK(0 == pthread_mutex_init(&mu, &attr));
  CHECK(0 == pthread_mutex_lock(&mu));
  CHECK(0 == pthread_mutex_unlock(&mu));
  int ret_unlock = pthread_mutex_unlock(&mu);  // unlocking twice.
  int ret_destroy = pthread_mutex_destroy(&mu);
  printf("  pthread_mutex_unlock returned %d\n", ret_unlock);
  printf("  pthread_mutex_destroy returned %d\n", ret_destroy);
}
}  // namespace test136


// test141 FP. unlink/fopen, rmdir/opendir. {{{1
namespace test141 {
int GLOB1 = 0,
    GLOB2 = 0;
char *dir_name = NULL,
     *filename = NULL;

void Waker1() {
  usleep(100000);
  GLOB1 = 1;  // Write
  // unlink deletes a file 'filename'
  // which exits spin-loop in Waiter1().
  printf("  Deleting file...\n");
  CHECK(unlink(filename) == 0);
}

void Waiter1() {
  FILE *tmp;
  while ((tmp = fopen(filename, "r")) != NULL) {
    fclose(tmp);
    usleep(10000);
  }
  printf("  ...file has been deleted\n");
  GLOB1 = 2;  // Write
}

void Waker2() {
  usleep(100000);
  GLOB2 = 1;  // Write
  // rmdir deletes a directory 'dir_name'
  // which exit spin-loop in Waker().
  printf("  Deleting directory...\n");
  CHECK(rmdir(dir_name) == 0);
}

void Waiter2() {
  DIR *tmp;
  while ((tmp = opendir(dir_name)) != NULL) {
    closedir(tmp);
    usleep(10000);
  }
  printf("  ...directory has been deleted\n");
  GLOB2 = 2;
}

TEST(NegativeTests, test141) {
  printf("test141: FP. unlink/fopen, rmdir/opendir.\n");

  dir_name = strdup("/tmp/tsan-XXXXXX");
  CHECK(mkdtemp(dir_name) != 0);

  filename = strdup((std::string() + dir_name + "/XXXXXX").c_str());
  const int fd = mkstemp(filename);
  CHECK(fd >= 0);
  close(fd);

  MyThreadArray mta1(Waker1, Waiter1);
  mta1.Start();
  mta1.Join();

  MyThreadArray mta2(Waker2, Waiter2);
  mta2.Start();
  mta2.Join();

  free(filename);
  filename = 0;
  free(dir_name);
  dir_name = 0;
}
}  // namespace test141


// test146: TP. Unit test for RWLock::TryLock and RWLock::ReaderTryLock. {{{1
namespace test146 {
// Worker1 locks the globals for writing for a long time.
// Worker2 tries to write to globals twice: without a writer lock and with it.
// Worker3 tries to read from globals twice: without a reader lock and with it.
int     GLOB1 = 0;
char    padding1[64];
int     GLOB2 = 0;
char    padding2[64];
int     GLOB3 = 0;
char    padding3[64];
int     GLOB4 = 0;
RWLock  MU;
StealthNotification n1, n2, n3, n4, n5;

void Worker1() {
  MU.Lock();
  GLOB1 = 1;
  GLOB2 = 1;
  GLOB3 = 1;
  GLOB4 = 1;
  n1.signal();
  n2.wait();
  n3.wait();
  MU.Unlock();
  n4.signal();
}

void Worker2() {
  n1.wait();
  if (MU.TryLock()) CHECK(0);
  else
    GLOB1 = 2;
  n2.signal();
  n5.wait();
  if (MU.TryLock()) {
    GLOB2 = 2;
    MU.Unlock();
  } else {
    CHECK(0);
  }
}

void Worker3() {
  n1.wait();
  if (MU.ReaderTryLock()) CHECK(0);
  else
    printf("\treading GLOB3: %d\n", GLOB3);
  n3.signal();
  n4.wait();
  if (MU.ReaderTryLock()) {
    printf("\treading GLOB4: %d\n", GLOB4);
    MU.ReaderUnlock();
  } else {
    CHECK(0);
  }
  n5.signal();
}

TEST(PositiveTests, test146) {
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB1, "test146. TP: a data race on GLOB1.");
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB3, "test146. TP: a data race on GLOB3.");
  ANNOTATE_TRACE_MEMORY(&GLOB1);
  ANNOTATE_TRACE_MEMORY(&GLOB2);
  ANNOTATE_TRACE_MEMORY(&GLOB3);
  ANNOTATE_TRACE_MEMORY(&GLOB4);
  printf("test146: positive\n");
  MyThreadArray t(Worker1, Worker2, Worker3);
  t.Start();
  t.Join();
  printf("\tGLOB1=%d\n", GLOB1);
  printf("\tGLOB2=%d\n", GLOB2);
  printf("\tGLOB3=%d\n", GLOB3);
  printf("\tGLOB4=%d\n", GLOB4);
}
} // namespace test146

namespace PositiveTests_CyclicBarrierTest {  // {{{1
#ifndef NO_BARRIER
// regression test for correct support of cyclic barrier.
// This test was suggested by Julian Seward.
// There is a race on L here between a1 and b1,
// but a naive support of barrier makes us miss this race.
pthread_barrier_t B;
int L;

// A1/A2: write L, then wait for barrier, then sleep
void a1() {
  L = 1;
  pthread_barrier_wait(&B);
  sleep(1);
}
void a2() {
  pthread_barrier_wait(&B);
  sleep(1);
}

// B1/B2: sleep, wait for barrier, then write L
void b1() {
  sleep(1);
  pthread_barrier_wait(&B);
  L = 1;
}
void b2() {
  sleep(1);
  pthread_barrier_wait(&B);
}

TEST(PositiveTests, CyclicBarrierTest) {
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&L, "real race, may be hidden"
                                " by incorrect implementation of barrier");
  pthread_barrier_init(&B, NULL, 3);
  MyThreadArray t1(a1, a2, a2),
                t2(b1, b2, b2);
  t1.Start();
  t2.Start();
  t1.Join();
  t2.Join();
}


int *G = NULL;

void Worker() {
  pthread_barrier_wait(&B);
  (*G) = 1;
  pthread_barrier_wait(&B);
}

TEST(PositiveTests, CyclicBarrierTwoCallsTest) {
  pthread_barrier_init(&B, NULL, 2);
  G = new int(0);
  ANNOTATE_TRACE_MEMORY(G);
  ANNOTATE_EXPECT_RACE_FOR_TSAN(G, "real race, may be hidden"
                                " by incorrect implementation of barrier");
  MyThreadArray t1(Worker, Worker);
  t1.Start();
  t1.Join();
  CHECK(*G == 1);
  delete G;
}



#endif  // NO_BARRIER
}  // namespace

TEST(NegativeTests, Mmap84GTest) {  // {{{1
#ifdef ARCH_amd64
#ifdef OS_linux
  // test that we can mmap 84G and can do it fast.
  size_t size = (1ULL << 32) * 21;  // 21 * 4G
  void *mem_ptr = mmap((void *) 0,
                       size,
                       PROT_EXEC | PROT_READ | PROT_WRITE,
                       MAP_ANONYMOUS | MAP_PRIVATE | MAP_NORESERVE,
                       -1,
                       (off_t) 0);
  printf("res=%p\n", mem_ptr);
#endif
#endif
}

namespace NegativeTests_PthreadCreateFailureTest {  // {{{1
#ifdef OS_linux
void* ThreadRoutine(void *) {
  return NULL;
}

TEST(NegativeTests, PthreadCreateFailureTest) {
  pthread_attr_t attributes;
  pthread_attr_init(&attributes);
  pthread_attr_setstacksize(&attributes, -1);
  pthread_t handle;
  CHECK(pthread_create(&handle, &attributes, ThreadRoutine, NULL) != 0);
  pthread_attr_destroy(&attributes);
}
#endif  // OS_linux
}  // namespace NegativeTests_PthreadCreateFailureTest

namespace Signals {  // {{{1

typedef void (*Sigaction)(int, siginfo_t *, void *);

int     GLOB = 0;

static void EnableSigprof(Sigaction SignalHandler) {
  struct sigaction sa;
  sa.sa_sigaction = SignalHandler;
  sa.sa_flags = SA_RESTART | SA_SIGINFO;
  sigemptyset(&sa.sa_mask);
  if (sigaction(SIGPROF, &sa, NULL) != 0) {
    perror("sigaction");
    abort();
  }
  struct itimerval timer;
  timer.it_interval.tv_sec = 0;
  timer.it_interval.tv_usec = 1000000 / 10000;
  timer.it_value = timer.it_interval;
  if (setitimer(ITIMER_PROF, &timer, 0) != 0) {
    perror("setitimer");
    abort();
  }
}

static void DisableSigprof() {
  // Disable the profiling timer.
  struct itimerval timer;
  timer.it_interval.tv_sec = 0;
  timer.it_interval.tv_usec = 0;
  timer.it_value = timer.it_interval;
  if (setitimer(ITIMER_PROF, &timer, 0) != 0) {
    perror("setitimer");
    abort();
  }
  // Ignore SIGPROFs from now on.
  struct sigaction sa;
  sa.sa_handler = SIG_IGN;
  sa.sa_flags = SA_RESTART | SA_SIGINFO;
  sigemptyset(&sa.sa_mask);
  if (sigaction(SIGPROF, &sa, NULL) != 0) {
    perror("sigaction");
    abort();
  }
}

void MallocTestWorker() {
  for (int i = 0; i < 100000; i++) {
    void *x = malloc((i % 64) + 1);
    free (x);
  }
}

// Regression test for
// http://code.google.com/p/data-race-test/issues/detail?id=13 .
// Make sure that locking events are handled in signal handlers.
//
// For some reason, invoking the signal handlers causes deadlocks on Mac OS.
#ifndef __APPLE__
Mutex mu;

void SignalHandlerWithMutex(int, siginfo_t*, void*) {
  mu.Lock();
  GLOB++;
  mu.Unlock();
}

TEST(Signals, SignalsAndMallocTestWithMutex) {
  EnableSigprof(SignalHandlerWithMutex);
  MyThreadArray t(MallocTestWorker, MallocTestWorker, MallocTestWorker);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
  DisableSigprof();
}
#endif

// Another regression test for
// http://code.google.com/p/data-race-test/issues/detail?id=13 .
// Make sure that locking events are handled in signal handlers.
SpinLock sl;

void SignalHandlerWithSpinlock(int, siginfo_t*, void*) {
  sl.Lock();
  GLOB++;
  sl.Unlock();
}

TEST(Signals, DISABLED_SignalsAndMallocTestWithSpinlock) {
  EnableSigprof(SignalHandlerWithSpinlock);
  MyThreadArray t(MallocTestWorker, MallocTestWorker, MallocTestWorker);
  t.Start();
  t.Join();
  printf("\tGLOB=%d\n", GLOB);
  DisableSigprof();
}

// Regression test for
// http://code.google.com/p/data-race-test/issues/detail?id=14.
static void WaitTestSignalHandler(int, siginfo_t*, void*) {
  ANNOTATE_HAPPENS_AFTER((void*)0x1234);
}

void WaitTestWorker() {
  for (int i = 0; i < 1000000; i++) {
    ANNOTATE_HAPPENS_AFTER((void*)0x1234);
  }
}

TEST(Signals, SignalsAndWaitTest) {
  EnableSigprof(WaitTestSignalHandler);
  MyThreadArray t(WaitTestWorker, WaitTestWorker, WaitTestWorker);
  t.Start();
  t.Join();
  DisableSigprof();
}

#ifndef __APPLE__
// this test crashes on Mac in debug TSan build, see
// http://code.google.com/p/data-race-test/issues/detail?id=47
pid_t child_pid = 0;

void child_handler(int signum) {
  if (signum == SIGCHLD && child_pid == 0) {
    printf("Whoops, PID shouldn't be 0!\n");
  }
}

TEST(Signals, PositiveTests_RaceInSignal) {
  // Currently the data race on child_pid can't be found,
  // see http://code.google.com/p/data-race-test/issues/detail?id=46
  //ANNOTATE_EXPECT_RACE(&child_pid, "Race on pid: fork vs signal handler");
  signal(SIGCHLD, child_handler);
  child_pid = fork();
  if (child_pid == 0) {
    // in child
    exit(0);
  }
  wait(NULL);
}
#endif  // __APPLE__

}  // namespace;

TEST(WeirdSizesTests, FegetenvTest) {
  // http://code.google.com/p/data-race-test/issues/detail?id=36
  fenv_t tmp;
  if (fegetenv(&tmp) != 0)
    FAIL() << "fegetenv failed";
}

namespace NegativeTests_epoll {  // {{{1
#ifdef OS_linux
int GLOB;

// Currently, ThreadSanitizer should create hb arcs between 
// epoll_ctl and epoll_wait regardless of the parameters. Check that.

void Worker1() {
  GLOB++;
  struct epoll_event event;
  epoll_ctl(0, 0, 0, &event);
}
void Worker2() {
  struct epoll_event event;
  epoll_wait(0, &event, 0, 0);
  GLOB++;
}

TEST(NegativeTests,epollTest) {
  MyThreadArray mta(Worker1, Worker2);
  mta.Start();
  mta.Join();
}
#endif  // OS_linux
}
namespace NegativeTests_LockfTest {  // {{{1

class ShmMutex {
 public:
  ShmMutex() : fd_(-1) { }
  void set_fd(int fd) {
    CHECK(fd_ == -1);
    fd_ = fd;
  }
  void Lock() {
    LockOrUnlockInternal(true);
  }
  void Unlock() {
    LockOrUnlockInternal(false);
  }
 private:
  void LockOrUnlockInternal(bool lock) {
    CHECK(fd_ >= 0);
    while (lockf(fd_, lock ? F_LOCK : F_ULOCK, 0) < 0) {
      if (errno == EINTR) {
        continue;
      } else if (errno == ENOLCK) {
        usleep(5000);
        continue;
      }
      CHECK(0);
    }

  }

  int fd_;
} mu;

int GLOB;

void Worker() {
  mu.Lock();
  GLOB++;
  mu.Unlock();
}

TEST(NegativeTests,DISABLED_LockfTest) {
  mu.set_fd(1 /* stdout */);
  MyThreadArray mta(Worker, Worker);
  mta.Start();
  mta.Join();
}

}
namespace PositiveTests_LockThenNoLock {  // {{{1
// Regression test for a bug fixed by r2312
int GLOB;
pthread_mutex_t mu;
StealthNotification n1, n2;

void Worker1() {
  pthread_mutex_lock(&mu);
  GLOB = 1;
  pthread_mutex_unlock(&mu);
  n1.signal();
  n2.wait();
  GLOB = 2;
}

void Worker2() {
  pthread_mutex_lock(&mu);
  GLOB = 3;
  pthread_mutex_unlock(&mu);
  n2.signal();
  n1.wait();
  GLOB = 4;
}

TEST(PositiveTests, LockThenNoLock) {
  pthread_mutex_init(&mu, NULL);
  ANNOTATE_TRACE_MEMORY(&GLOB);
  ANNOTATE_EXPECT_RACE(&GLOB, "race");
  ANNOTATE_NOT_HAPPENS_BEFORE_MUTEX(&mu);
  MyThreadArray t(Worker1, Worker2);
  t.Start();
  t.Join();
  pthread_mutex_destroy(&mu);
}
}  // namespace

#ifdef __APPLE__
namespace NegativeTests_PthreadCondWaitRelativeNp {  // {{{1
int GLOB = 0;
pthread_mutex_t mu;
pthread_cond_t cv;

void Waiter() {
  struct timespec tv = {1000, 1000};
  pthread_mutex_lock(&mu);
  pthread_cond_timedwait_relative_np(&cv, &mu, &tv);
  GLOB = 2;
  pthread_mutex_unlock(&mu);
}

void Waker() {
  pthread_mutex_lock(&mu);
  GLOB = 1;
  pthread_cond_signal(&cv);
  pthread_mutex_unlock(&mu);
}

TEST(NegativeTests, PthreadCondWaitRelativeNpTest) {
  pthread_mutex_init(&mu, NULL);
  pthread_cond_init(&cv, NULL);
  MyThreadArray t(Waiter, Waker);
  t.Start();
  t.Join();
  pthread_mutex_destroy(&mu);
  pthread_cond_destroy(&cv);
}
}  // namespace
#endif  // __APPLE__

namespace PositiveTests_RWLockVsRWLockTest {  // {{{1
// Test that reader lock/unlock do not create a hb-arc.
RWLock mu;
int GLOB;
StealthNotification n;

void Thread1() {
  GLOB = 1;
  mu.ReaderLock();
  mu.ReaderUnlock();
  n.signal();
}

void Thread2() {
  n.wait();
  mu.ReaderLock();
  mu.ReaderUnlock();
  GLOB = 1;
}

TEST(PositiveTests, RWLockVsRWLockTest) {
  ANNOTATE_PURE_HAPPENS_BEFORE_MUTEX(&mu);
  ANNOTATE_EXPECT_RACE(&GLOB, "rwunlock/rwlock is not a hb-arc");
  MyThreadArray t(Thread1, Thread2);
  t.Start();
  t.Join();
}

}  // namespace

namespace TSDTests {
// Test the support for libpthread TSD destructors.
pthread_key_t key;
const int kInitialValue = 0xfeedface;
int tsd_array[2];

void Destructor(void *ptr) {
  int *write = (int*) ptr;
  *write = kInitialValue;
}

void DoWork(int index) {
  int *value = &(tsd_array[index]);
  *value = 42;
  pthread_setspecific(key, value);
  int *read = (int*) pthread_getspecific(key);
  CHECK(read == value);
}

void Worker0() { DoWork(0); }
void Worker1() { DoWork(1); }

TEST(TSDTests, TSDDestructorTest) {
  pthread_key_create(&key, Destructor);
  MyThreadArray t(Worker0, Worker1);
  t.Start();
  t.Join();
  for (int i = 0; i < 2; ++i) {
    CHECK(tsd_array[i] == kInitialValue);
  }
}

}

// End {{{1
 // vim:shiftwidth=2:softtabstop=2:expandtab:foldmethod=marker
