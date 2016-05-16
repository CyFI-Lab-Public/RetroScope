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
// Author: Timur Iskhodzhanov
//
// For ThreadSanitizer tests, we want ANNOTATE_FLUSH_EXPECTED_RACES to be called
// after running each test case.
// This file contains a hack that replaces the googletest TEST macro and injects
// a custom fixture class instead of ::testing::Test for TEST tests
// (i.e. those test that don't have fixtures)
//
// IMPORTANT NOTICE: please inherit your custom fixtures from RacecheckFixture,
// not ::testing::Test
// and call RacecheckFixture::TearDown() from your TearDown()

#ifndef TEST
#error Please include gtest_fixture_injection.h after <gtest/gtest.h>
#endif

class RacecheckFixture : public ::testing::Test {
 public:
  virtual void TearDown() {
    ANNOTATE_FLUSH_EXPECTED_RACES();
  }
};

#undef TEST
#define TEST(test_case_name, test_name)\
  GTEST_TEST_(test_case_name, test_name, \
              ::RacecheckFixture, ::testing::internal::GetTestTypeId())
