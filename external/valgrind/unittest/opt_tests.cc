/*
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

#include "old_test_suite.h"
#include "test_utils.h"

#include <gtest/gtest.h>


namespace OptIgnoreTests {  // {{{1 Test how the tool works with indirect calls to fun_r functions
int GLOB = 0;

void NOINLINE NotIgnoredRacey() {
  GLOB++;
}

void NOINLINE IntermediateJumpHereFunction() {
  NotIgnoredRacey();
  usleep(1);  // Prevent tail call optimization.
}

void NOINLINE DoTailCall() {
  IntermediateJumpHereFunction();  // This tail call should be optimized.
}

// Test that a function that is called using jump is ignored.
TEST(IgnoreTests, DISABLED_TailCall) {
  MyThreadArray mta(DoTailCall, DoTailCall);
  mta.Start();
  mta.Join();
  usleep(1);
}

}
