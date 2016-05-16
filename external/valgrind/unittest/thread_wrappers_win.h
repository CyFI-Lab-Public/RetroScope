/*
  This file is part of ThreadSanitizer, a dynamic data race detector.

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

// Author: Konstantin Serebryany <opensource@google.com>
//
// Here we define few simple classes that wrap pthread primitives.
//
// We need this to create unit tests for helgrind (or similar tool)
// that will work with different threading frameworks.
//
// If one needs to test helgrind's support for another threading library,
// he/she can create a copy of this file and replace pthread_ calls
// with appropriate calls to his/her library.
//
// Note, that some of the methods defined here are annotated with
// ANNOTATE_* macros defined in dynamic_annotations.h.
//
// DISCLAIMER: the classes defined in this header file
// are NOT intended for general use -- only for unit tests.
//
#ifndef THREAD_WRAPPERS_WIN_H
#define THREAD_WRAPPERS_WIN_H

#define _WIN32_WINNT 0x0500 // Require Windows 2000.
#include <windows.h>
#include <mmsystem.h>

#pragma comment(lib, "winmm.lib")

#define NO_BARRIER
#define NO_UNNAMED_SEM
#define TLS __declspec(thread)
#define NO_SPINLOCK // TODO(timurrrr): implement SpinLock
#define usleep(x) Sleep((x)/1000)
#define sleep(x) Sleep((x)*1000)
#define NOINLINE __declspec(noinline)
#define ALIGNED(x) __declspec (align(x))

int GetTimeInMs() {
  return (int)timeGetTime();
}

typedef unsigned char  uint8_t;
typedef unsigned short uint16_t;
typedef unsigned int   uint32_t;
typedef unsigned long long uint64_t;
typedef long long int64_t;

// This constant is true if malloc() uses mutex on your platform as this may
// introduce a happens-before arc for a pure happens-before race detector.
static const bool kMallocUsesMutex = false;

int AtomicIncrement(volatile int *value, int increment) {
  return InterlockedExchangeAdd(reinterpret_cast<volatile LONG*>(value),
                                increment) + increment;
}

class Mutex {
  friend class CondVar;
 public:
  Mutex()  { ::InitializeCriticalSection(&cs_); }
  ~Mutex() { ::DeleteCriticalSection(&cs_); }
  void Lock()          { ::EnterCriticalSection(&cs_);}
  bool TryLock()       { return ::TryEnterCriticalSection(&cs_); }
  void Unlock() {
    ANNOTATE_HAPPENS_BEFORE(this);
    /*
    // TODO(timurrrr): do we need this?
    if (signal_at_unlock_) {
      CHECK(0 == pthread_cond_signal(&cv_));
    }
    */
    ::LeaveCriticalSection(&cs_);
  }
  void ReaderLock()    { Lock(); }
  bool ReaderTryLock() { return TryLock();}
  void ReaderUnlock()  { Unlock(); }

  void LockWhen(Condition cond)            { Lock(); WaitLoop(cond); }
  void ReaderLockWhen(Condition cond)      { Lock(); WaitLoop(cond); }
  void Await(Condition cond)               { WaitLoop(cond); }

  bool ReaderLockWhenWithTimeout(Condition cond, int millis)
    { Lock(); return WaitLoopWithTimeout(cond, millis); }
  bool LockWhenWithTimeout(Condition cond, int millis)
    { Lock(); return WaitLoopWithTimeout(cond, millis); }
  bool AwaitWithTimeout(Condition cond, int millis)
    { return WaitLoopWithTimeout(cond, millis); }

 private:

  void WaitLoop(Condition cond) {
    while(cond.Eval() == false) {
      Unlock();
      // TODO(timurrrr)
      Sleep(10);
      Lock();
    }
    ANNOTATE_HAPPENS_AFTER(this);
  }

  bool WaitLoopWithTimeout(Condition cond, int millis) {
    int start_time = GetTimeInMs();

    while (cond.Eval() == false && GetTimeInMs() - start_time < millis) {
      Unlock();
      // TODO(timurrrr)
      Sleep(10);
      Lock();
    }

    if (cond.Eval() == 0) {
      return false;
    } else {
      ANNOTATE_HAPPENS_AFTER(this);
      return true;
    }
  }

  CRITICAL_SECTION cs_;
};

class CondVar {
 public:
  CondVar()   {
    signaled_ = false;
    hSignal_  = CreateEvent(NULL, false, false, NULL);
    CHECK(hSignal_ != NULL);
  }
  ~CondVar()  {
    CloseHandle(hSignal_);
  }
  void Wait(Mutex *mu) {
    while (!signaled_) {
      mu->Unlock();
      WaitForSingleObject(hSignal_, INFINITE);
      mu->Lock();
    }
    signaled_ = false;
    ANNOTATE_HAPPENS_AFTER(this);
  }
  bool WaitWithTimeout(Mutex *mu, int millis) {
    int start_time = GetTimeInMs();

    while (!signaled_ && GetTimeInMs() - start_time < millis) {
      int curr_time = GetTimeInMs();
      if (curr_time - start_time >= millis)
        break;
      mu->Unlock();
      WaitForSingleObject(hSignal_, start_time + millis - curr_time);
      mu->Lock();
    }
    if (signaled_) {
      ANNOTATE_HAPPENS_AFTER(this);
      signaled_ = false;
      return true;
    }
    return false;
  }
  void Signal() {
    signaled_ = true;
    ANNOTATE_HAPPENS_BEFORE(this);
    SetEvent(hSignal_);
  }
// TODO(timurrrr): this isn't used anywhere - do we need these?
//  void SignalAll();
 private:
  HANDLE hSignal_;
  bool signaled_;
};

class MyThread {
 public:
  typedef void *(*worker_t)(void*);

  MyThread(worker_t worker, void *arg = NULL, const char *name = NULL)
      :w_(worker), arg_(arg), name_(name), t_(NULL) {}
  MyThread(void (*worker)(void), void *arg = NULL, const char *name = NULL)
      :w_(reinterpret_cast<worker_t>(worker)), arg_(arg), name_(name), t_(NULL) {}
  MyThread(void (*worker)(void *), void *arg = NULL, const char *name = NULL)
      :w_(reinterpret_cast<worker_t>(worker)), arg_(arg), name_(name), t_(NULL) {}

  ~MyThread(){
    CloseHandle(t_);
    t_ = NULL;
  }
  void Start() {
    DWORD thr_id;
    t_ = ::CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE)ThreadBody, this, 0, &thr_id);
    CHECK(t_ > 0);
  }
  void Join() {
    CHECK(t_ > 0);
    CHECK(WAIT_OBJECT_0 == ::WaitForSingleObject(t_, INFINITE));
  }
  HANDLE tid() const { return t_; }
 private:
  static DWORD WINAPI ThreadBody(MyThread *my_thread) {
    if (my_thread->name_) {
      ANNOTATE_THREAD_NAME(my_thread->name_);
    }
    my_thread->w_(my_thread->arg_);
    return 0;
  }
  HANDLE t_;
  DWORD ret_;
  worker_t  w_;
  void     *arg_;
  const char *name_;
};
#endif  // THREAD_WRAPPERS_WIN_H
