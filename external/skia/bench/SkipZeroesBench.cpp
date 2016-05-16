/*
 * Copyright 2013 Google Inc.
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

#include "SkBenchmark.h"
#include "SkBitmap.h"
#include "SkData.h"
#include "SkForceLinking.h"
#include "SkImageDecoder.h"
#include "SkOSFile.h"
#include "SkStream.h"
#include "SkString.h"

__SK_FORCE_IMAGE_DECODER_LINKING;

class SkCanvas;

class SkipZeroesBench : public SkBenchmark {
public:
    SkipZeroesBench(const char* filename, bool skipZeroes)
    : fName("SkipZeroes_")
    , fDecoder(NULL)
    , fFilename(filename)
    , fStream()
    , fSkipZeroes(skipZeroes)
    , fValid(false) {
        fName.append(SkOSPath::SkBasename(filename));
        if (skipZeroes) {
            fName.append("_skip_zeroes");
        } else {
            fName.append("_write_zeroes");
        }
        fIsRendering = false;
    }

    ~SkipZeroesBench() {
        SkDELETE(fDecoder);
    }
protected:
    virtual const char* onGetName() SK_OVERRIDE {
        return fName.c_str();
    }

    virtual void onPreDraw() SK_OVERRIDE {
        SkFILEStream fileStream(fFilename.c_str());
        fValid = fileStream.isValid() && fileStream.getLength() > 0;
        if (fValid) {
            const size_t size = fileStream.getLength();
            void* data = sk_malloc_throw(size);
            if (fileStream.read(data, size) < size) {
                fValid = false;
            } else {
                SkAutoTUnref<SkData> skdata(SkData::NewFromMalloc(data, size));
                fStream.setData(skdata.get());
                fDecoder = SkImageDecoder::Factory(&fStream);
                if (fDecoder) {
                    fDecoder->setSkipWritingZeroes(fSkipZeroes);
                } else {
                    fValid = false;
                }
            }
        }
    }

    virtual void onDraw(SkCanvas*) SK_OVERRIDE {
#ifdef SK_DEBUG
        if (!fValid) {
            SkDebugf("stream was invalid: %s\n", fName.c_str());
            return;
        }
#endif
        // Decode a bunch of times
        SkBitmap bm;
        for (int i = 0; i < this->getLoops(); ++i) {
            SkDEBUGCODE(bool success =) fDecoder->decode(&fStream, &bm,
                                                         SkImageDecoder::kDecodePixels_Mode);
#ifdef SK_DEBUG
            if (!success) {
                SkDebugf("failed to decode %s\n", fName.c_str());
                return;
            }
#endif
            SkDEBUGCODE(success =) fStream.rewind();
#ifdef SK_DEBUG
            if (!success) {
                SkDebugf("failed to rewind %s\n", fName.c_str());
                return;
            }
#endif
        }
    }

private:
    SkString        fName;
    SkImageDecoder* fDecoder;
    const SkString  fFilename;
    SkMemoryStream  fStream;
    bool            fSkipZeroes;
    bool            fValid;

    typedef SkBenchmark INHERITED;
};

//DEF_BENCH( return SkNEW_ARGS(SkipZeroesBench, ("/sdcard/skia/images/arrow.png", true)));
//DEF_BENCH( return SkNEW_ARGS(SkipZeroesBench, ("/sdcard/skia/images/arrow.png", false)));
