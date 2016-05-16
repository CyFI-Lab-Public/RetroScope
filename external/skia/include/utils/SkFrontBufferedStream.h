/*
 * Copyright 2013 Google Inc.
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

#include "SkStream.h"
#include "SkTemplates.h"

/**
 *  Specialized stream that only buffers the first X bytes of a stream,
 *  where X is passed in by the user. Note that unlike some buffered
 *  stream APIs, once more than those bytes are read, no more buffering
 *  is done. This stream is designed for a use case where the caller
 *  knows that rewind will only be called from within X bytes (inclusive),
 *  and the wrapped stream is not necessarily able to rewind at all.
 */
class SkFrontBufferedStream : public SkStreamRewindable {
public:
    /**
     *  Creates a new stream that wraps and buffers SkStream.
     *  @param stream SkStream to buffer. If NULL, NULL is returned. After
     *      this call, unref stream and do not refer to it.
     *      SkFrontBufferedStream is expected to be its only owner.
     *  @param bufferSize Exact size of the buffer to be used.
     *  @return An SkStream that can buffer up to bufferSize.
     */
    static SkFrontBufferedStream* Create(SkStream* stream, size_t bufferSize);

    virtual size_t read(void* buffer, size_t size) SK_OVERRIDE;

    virtual bool isAtEnd() const SK_OVERRIDE;

    virtual bool rewind() SK_OVERRIDE;

    virtual bool hasPosition() const SK_OVERRIDE { return true; }

    virtual size_t getPosition() const SK_OVERRIDE { return fOffset; }

    virtual bool hasLength() const SK_OVERRIDE;

    virtual size_t getLength() const SK_OVERRIDE;

    virtual SkStreamRewindable* duplicate() const SK_OVERRIDE { return NULL; }

private:
    SkAutoTUnref<SkStream>  fStream;
    // Current offset into the stream. Always >= 0.
    size_t                  fOffset;
    // Amount that has been buffered by calls to read. Will always be less than
    // fBufferSize.
    size_t                  fBufferedSoFar;
    // Total size of the buffer.
    const size_t            fBufferSize;
    SkAutoTMalloc<char>     fBuffer;

    // Unimplemented.
    SkFrontBufferedStream();

    // Private. Use Create.
    SkFrontBufferedStream(SkStream*, size_t bufferSize);

    char* getBufferAtOffset() {
        return fBuffer.get() + fOffset;
    }

    typedef SkStream INHERITED;
};
