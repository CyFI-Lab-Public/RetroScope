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

 This file contains a few macros useful for implementing
 unit-tests for data race detection tools.

*/

#ifndef TEST_UTILS_H__
#define TEST_UTILS_H__

// This test must not include any other file specific to threading library,
// everything should be inside THREAD_WRAPPERS.
#ifndef THREAD_WRAPPERS
# define THREAD_WRAPPERS "thread_wrappers.h"
#endif
#include THREAD_WRAPPERS

#ifndef NEEDS_SEPERATE_RW_LOCK
#define RWLock Mutex // Mutex does work as an rw-lock.
#define WriterLockScoped MutexLock
#define ReaderLockScoped ReaderMutexLock
#endif // !NEEDS_SEPERATE_RW_LOCK

static bool ArgIsOne(int *arg) { return *arg == 1; };
static bool ArgIsZero(int *arg) { return *arg == 0; };
static bool ArgIsTrue(bool *arg) { return *arg == true; };


// If run under ThreadSanitizerQuery, this function is replaced by the tool
// and a non-NULL string is returned. See the usage below.
extern "C" const char *ThreadSanitizerQuery(const char *query);

// Apply ANNOTATE_EXPECT_RACE only if running under ThreadSanitizer.
#define ANNOTATE_EXPECT_RACE_FOR_TSAN(mem, descr) \
    do {\
      if (ThreadSanitizerQuery("") != NULL) {\
        ANNOTATE_EXPECT_RACE(mem, descr); \
      } \
    } while(0)\

inline bool ThreadSanitizerQueryMatch(const char *query, const char *expected_answer) {
  const char *answer = ThreadSanitizerQuery(query);
  if (answer == NULL) {
    // Not running under ThreadSanitizer at all.
    return false;
  }
  return string(answer) == expected_answer;
}

inline bool Tsan_PureHappensBefore() {
  static bool ret = ThreadSanitizerQueryMatch("pure_happens_before", "1");
  return ret;
}

inline bool Tsan_RaceVerifier() {
  static bool ret = ThreadSanitizerQueryMatch("race_verifier", "1");
  return ret;
}

// An array of threads. Create/start/join all elements at once.
class MyThreadArray {
 public:
  static const int kSize = 5;
  typedef void (*F) (void);
  MyThreadArray(F f1, F f2 = NULL, F f3 = NULL, F f4 = NULL, F f5 = NULL) {
    ar_[0] = new MyThread(f1);
    ar_[1] = f2 ? new MyThread(f2) : NULL;
    ar_[2] = f3 ? new MyThread(f3) : NULL;
    ar_[3] = f4 ? new MyThread(f4) : NULL;
    ar_[4] = f5 ? new MyThread(f5) : NULL;
  }
  void Start() {
    for(int i = 0; i < kSize; i++) {
      if(ar_[i]) {
        ar_[i]->Start();
        usleep(10);
      }
    }
  }

  void Join() {
    for(int i = 0; i < kSize; i++) {
      if(ar_[i]) {
        ar_[i]->Join();
      }
    }
  }

  ~MyThreadArray() {
    for(int i = 0; i < kSize; i++) {
      delete ar_[i];
    }
  }
 private:
  MyThread *ar_[kSize];
};


// This class does not implement a signal-wait synchronization
// primitive, even if it looks like one. Its purpose is to enforce an
// order of execution of threads in unit tests in a way that is
// invisible to ThreadSanitizer and similar tools. It lacks memory
// barriers, therefore it only works reliably if there is a real
// synchronization primitive before signal() or after wait().
class StealthNotification {
 public:
  StealthNotification() : flag_(0) {}

  void signal() {
    ANNOTATE_IGNORE_READS_AND_WRITES_BEGIN();
    CHECK(!flag_);
    flag_ = 1;
    ANNOTATE_IGNORE_READS_AND_WRITES_END();
  }

  void wait() {
    while (!flag_) {
#ifdef WIN32
      usleep(1000);
#else
      sched_yield();
#endif
    }
  }

 private:
  volatile int flag_;
};

#endif  // TEST_UTILS_H__
// End {{{1
 // vim:shiftwidth=2:softtabstop=2:expandtab:foldmethod=marker
