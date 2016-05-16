/*
 * Copyright 2013 Google Inc.
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

#include "SkFrontBufferedStream.h"

SkFrontBufferedStream* SkFrontBufferedStream::Create(SkStream* stream, size_t bufferSize) {
    if (NULL == stream) {
        return NULL;
    }
    return SkNEW_ARGS(SkFrontBufferedStream, (stream, bufferSize));
}

SkFrontBufferedStream::SkFrontBufferedStream(SkStream* stream, size_t bufferSize)
    : fStream(SkRef(stream))
    , fOffset(0)
    , fBufferedSoFar(0)
    , fBufferSize(bufferSize)
    , fBuffer(bufferSize) {}

bool SkFrontBufferedStream::isAtEnd() const {
    if (fOffset < fBufferedSoFar) {
        // Even if the underlying stream is at the end, this stream has been
        // rewound after buffering, so it is not at the end.
        return false;
    }

    return fStream->isAtEnd();
}

bool SkFrontBufferedStream::rewind() {
    // Only allow a rewind if we have not exceeded the buffer.
    if (fOffset <= fBufferSize) {
        fOffset = 0;
        return true;
    }
    return false;
}

bool SkFrontBufferedStream::hasLength() const {
    return fStream->hasLength();
}

size_t SkFrontBufferedStream::getLength() const {
    return fStream->getLength();
}

size_t SkFrontBufferedStream::read(void* voidDst, size_t size) {
    // Cast voidDst to a char* for easy addition.
    char* dst = reinterpret_cast<char*>(voidDst);
    SkDEBUGCODE(const size_t totalSize = size;)
    size_t readSoFar = 0;

    // First, read any data that was previously buffered.
    if (fOffset < fBufferedSoFar) {
        // Some data has already been copied to fBuffer. Read up to the
        // lesser of the size requested and the remainder of the buffered
        // data.
        const size_t bytesToCopy = SkTMin(size, fBufferedSoFar - fOffset);
        if (dst != NULL) {
            memcpy(dst, this->getBufferAtOffset(), bytesToCopy);
        }

        // Update fOffset to the new position. It is guaranteed to be
        // within the buffered data.
        fOffset += bytesToCopy;
        SkASSERT(fOffset <= fBufferedSoFar);

        if (bytesToCopy == size) {
            // The entire requested read was inside the already buffered
            // data.
            SkASSERT(fOffset <= fBufferedSoFar);
            return size;
        }

        // The requested read extends beyond the buffered data. Update
        // the remaining number of bytes needed to read, the number of
        // bytes read so far, and the destination buffer.
        size -= bytesToCopy;
        readSoFar += bytesToCopy;
        SkASSERT(size + readSoFar == totalSize);
        if (dst != NULL) {
            dst += bytesToCopy;
        }
    }

    // If we got here, we have read everything that was already buffered.
    SkASSERT(fOffset >= fBufferedSoFar);

    // Buffer any more data that should be buffered, and copy it to the
    // destination.
    if (fBufferedSoFar < fBufferSize) {
        // Data needs to be buffered. Buffer up to the lesser of the size requested
        // and the remainder of the max buffer size.
        const size_t bytesToBuffer = SkTMin(size, fBufferSize - fBufferedSoFar);
        char* buffer = this->getBufferAtOffset();
        const size_t buffered = fStream->read(buffer, bytesToBuffer);
        fBufferedSoFar += buffered;
        fOffset = fBufferedSoFar;

        // Copy the buffer to the destination buffer and update the amount read.
        if (dst != NULL) {
            memcpy(dst, buffer, buffered);
        }
        readSoFar += buffered;

        if (buffered == size || fStream->isAtEnd()) {
            // We were able to buffer all of the data requested (or all the data
            // remaining in the stream) and provide it to the caller.
            SkASSERT(fBufferedSoFar <= fBufferSize);
            SkASSERT(totalSize == readSoFar || fStream->isAtEnd());
            return readSoFar;
        }

        // The requested read extends beyond the length of the buffer. Update
        // the remaining number of bytes needed to read and the destination
        // buffer.
        size -= buffered;
        SkASSERT(size + readSoFar == totalSize);
        if (dst != NULL) {
            dst += buffered;
        }
    }

    // If we get here, we have buffered all that can be buffered.
    SkASSERT(fBufferSize == fBufferedSoFar && fOffset >= fBufferSize);

    // Read directly from the stream.
    const size_t bytesReadDirectly = fStream->read(dst, size);
    fOffset += bytesReadDirectly;
    if (bytesReadDirectly > 0) {
        sk_free(fBuffer.detach());
    }
    return bytesReadDirectly + readSoFar;
}
