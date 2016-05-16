/* Copyright (c) 2008-2010, Google Inc.
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
 */

// This file is a part of a test suite for ThreadSanitizer, a race detector.
// Author: Konstantin Serebryany.

// These 4 lines need to go before any include from libstdc++.
// See include/bits/c++config in libstdc++ for details.
#include "dynamic_annotations.h"
#define _GLIBCXX_SYNCHRONIZATION_HAPPENS_BEFORE(a) ANNOTATE_HAPPENS_BEFORE(a)
#define _GLIBCXX_SYNCHRONIZATION_HAPPENS_AFTER(a) ANNOTATE_HAPPENS_AFTER(a)
#define _GLIBCXX_EXTERN_TEMPLATE -1

#include <stdio.h>
#include <pthread.h>
#include <unistd.h>
#include <assert.h>
#include <memory>
#include <string>

#include "test_utils.h"
#include <gtest/gtest.h>

#if defined(__cplusplus) && defined(__GNUC__)
#include <features.h>
// These tests verify that no false positives are reported due to custom
// synchronization in libstdc++.
// As of gcc 4.6, libstdc++ has HAPPENS_BEFORE/AFTER annotations in key places.

namespace LibStdCPlusPlus_basic_string_Test {  // {{{1
// If reference counting inside basic_string is not understood
// by a race detector, a false race may be reported between
// basic_string::some_accessor and basic_string::~basic_string

string *s;

pthread_mutex_t mu;
pthread_cond_t cv;
int done = 0;

void *Thread(void*) {
  string x = *s;  // calls _M_refcopy().

  pthread_mutex_lock(&mu);
  done++;
  pthread_cond_signal(&cv);
  pthread_mutex_unlock(&mu);

  assert(x == "foo");
  // x is destructed, calls _M_dispose
  return NULL;
}

const int kNThreads = 3;

TEST(LibStdCPlusPlus, basic_string_Test) {
  if (!__GNUC_PREREQ(4, 6)) {
    printf("This test is likely to produce a false race report "
           "with libstdc++ earlier than 4.6; not running\n");
    return;
  }

  s = new string("foo");
  pthread_t t[kNThreads];
  pthread_mutex_init(&mu, 0);
  pthread_cond_init(&cv, 0);
  // start threads.
  for (int i = 0; i < kNThreads; i++) {
    pthread_create(&t[i], 0, Thread, 0);
  }
  // wait for threads to copy 's', but don't wait for threads to exit.
  pthread_mutex_lock(&mu);
  while (done != kNThreads)
    pthread_cond_wait(&cv, &mu);
  pthread_mutex_unlock(&mu);
  // s has been copied few times, now delete it.
  // Last of the destructors (either here ot in Thread() will call _M_destroy).
  delete s;  // calls _M_dispose.
}
}  // namespace

namespace LibStdCPlusPlus_shared_ptr_Test {  // {{{1
// If reference counting inside shared_ptr is not understood
// by a race detector, a false race may be reported between
// Foo::Check() and Foo:~Foo().
class Foo {
 public:
 Foo() : a_(777) { }
 ~Foo() {
   a_ = 0xDEAD;
 }
 void Check() {
   assert(a_ == 777);
 }
 private:
 int a_;
};

shared_ptr<Foo> *s;

pthread_mutex_t mu;
pthread_cond_t cv;
int done = 0;

void *Thread(void*) {
 shared_ptr<Foo> x(*s);

 pthread_mutex_lock(&mu);
 done++;
 pthread_cond_signal(&cv);
 pthread_mutex_unlock(&mu);

 x->Check();
 // x is destructed.
 return NULL;
}

TEST(LibStdCPlusPlus, shared_ptr_Test) {

  if (!__GNUC_PREREQ(4, 6)) {
    printf("This test is likely to produce a false race report "
           "with libstdc++ earlier than 4.6; not running\n");
    return;
  }
  const int kNThreads = 3;
  s = new shared_ptr<Foo>(new Foo);
  pthread_t t[kNThreads];
  pthread_mutex_init(&mu, 0);
  pthread_cond_init(&cv, 0);
  // start threads.
  for (int i = 0; i < kNThreads; i++) {
    pthread_create(&t[i], 0, Thread, 0);
  }
  // wait for threads to copy 's', but don't wait for threads to exit.
  pthread_mutex_lock(&mu);
  while (done != kNThreads)
    pthread_cond_wait(&cv, &mu);
  pthread_mutex_unlock(&mu);

  delete s;
}
}  // namespace

#endif  // __GNUC__
// End {{{1
 // vim:shiftwidth=2:softtabstop=2:expandtab:foldmethod=marker
