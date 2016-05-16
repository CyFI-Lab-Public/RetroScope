/*
 * Copyright 2013 Google Inc.
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

#include "SkFrontBufferedStream.h"
#include "SkRefCnt.h"
#include "SkTypes.h"
#include "Test.h"

static void test_read(skiatest::Reporter* reporter, SkStream* bufferedStream,
                      const void* expectations, size_t bytesToRead) {
    // output for reading bufferedStream.
    SkAutoMalloc storage(bytesToRead);

    size_t bytesRead = bufferedStream->read(storage.get(), bytesToRead);
    REPORTER_ASSERT(reporter, bytesRead == bytesToRead || bufferedStream->isAtEnd());
    REPORTER_ASSERT(reporter, memcmp(storage.get(), expectations, bytesRead) == 0);
}

static void test_rewind(skiatest::Reporter* reporter,
                        SkStream* bufferedStream, bool shouldSucceed) {
    const bool success = bufferedStream->rewind();
    REPORTER_ASSERT(reporter, success == shouldSucceed);
}

// All tests will buffer this string, and compare output to the original.
// The string is long to ensure that all of our lengths being tested are
// smaller than the string length.
const char gAbcs[] = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";

// Tests reading the stream across boundaries of what has been buffered so far and what
// the total buffer size is.
static void test_incremental_buffering(skiatest::Reporter* reporter, size_t bufferSize) {
    SkMemoryStream memStream(gAbcs, strlen(gAbcs), false);

    SkAutoTUnref<SkStream> bufferedStream(SkFrontBufferedStream::Create(&memStream, bufferSize));

    // First, test reading less than the max buffer size.
    test_read(reporter, bufferedStream, gAbcs, bufferSize >> 1);

    // Now test rewinding back to the beginning and reading less than what was
    // already buffered.
    test_rewind(reporter, bufferedStream, true);
    test_read(reporter, bufferedStream, gAbcs, bufferSize >> 2);

    // Now test reading part of what was buffered, and buffering new data.
    test_read(reporter, bufferedStream, gAbcs + bufferedStream->getPosition(), bufferSize >> 1);

    // Now test reading what was buffered, buffering new data, and
    // reading directly from the stream.
    test_rewind(reporter, bufferedStream, true);
    test_read(reporter, bufferedStream, gAbcs, bufferSize << 1);

    // We have reached the end of the buffer, so rewinding will fail.
    // This test assumes that the stream is larger than the buffer; otherwise the
    // result of rewind should be true.
    test_rewind(reporter, bufferedStream, false);
}

static void test_perfectly_sized_buffer(skiatest::Reporter* reporter, size_t bufferSize) {
    SkMemoryStream memStream(gAbcs, strlen(gAbcs), false);
    SkAutoTUnref<SkStream> bufferedStream(SkFrontBufferedStream::Create(&memStream, bufferSize));

    // Read exactly the amount that fits in the buffer.
    test_read(reporter, bufferedStream, gAbcs, bufferSize);

    // Rewinding should succeed.
    test_rewind(reporter, bufferedStream, true);

    // Once again reading buffered info should succeed
    test_read(reporter, bufferedStream, gAbcs, bufferSize);

    // Read past the size of the buffer. At this point, we cannot return.
    test_read(reporter, bufferedStream, gAbcs + bufferedStream->getPosition(), 1);
    test_rewind(reporter, bufferedStream, false);
}

static void test_skipping(skiatest::Reporter* reporter, size_t bufferSize) {
    SkMemoryStream memStream(gAbcs, strlen(gAbcs), false);
    SkAutoTUnref<SkStream> bufferedStream(SkFrontBufferedStream::Create(&memStream, bufferSize));

    // Skip half the buffer.
    bufferedStream->skip(bufferSize >> 1);

    // Rewind, then read part of the buffer, which should have been read.
    test_rewind(reporter, bufferedStream, true);
    test_read(reporter, bufferedStream, gAbcs, bufferSize >> 2);

    // Now skip beyond the buffered piece, but still within the total buffer.
    bufferedStream->skip(bufferSize >> 1);

    // Test that reading will still work.
    test_read(reporter, bufferedStream, gAbcs + bufferedStream->getPosition(), bufferSize >> 2);

    test_rewind(reporter, bufferedStream, true);
    test_read(reporter, bufferedStream, gAbcs, bufferSize);
}

static void test_buffers(skiatest::Reporter* reporter, size_t bufferSize) {
    test_incremental_buffering(reporter, bufferSize);
    test_perfectly_sized_buffer(reporter, bufferSize);
    test_skipping(reporter, bufferSize);
}

static void TestStreams(skiatest::Reporter* reporter) {
    // Test 6 and 64, which are used by Android, as well as another arbitrary length.
    test_buffers(reporter, 6);
    test_buffers(reporter, 15);
    test_buffers(reporter, 64);
}

#include "TestClassDef.h"
DEFINE_TESTCLASS("FrontBufferedStream", FrontBufferedStreamTestClass, TestStreams)
