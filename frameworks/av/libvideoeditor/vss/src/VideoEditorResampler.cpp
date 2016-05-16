/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_NDEBUG 1
#include <audio_utils/primitives.h>
#include <utils/Log.h>
#include "AudioMixer.h"
#include "VideoEditorResampler.h"

namespace android {

struct VideoEditorResampler : public AudioBufferProvider {

    public:

        virtual status_t getNextBuffer(Buffer* buffer, int64_t pts);
        virtual void releaseBuffer(Buffer* buffer);

    enum { //Sampling freq
     kFreq8000Hz = 8000,
     kFreq11025Hz = 11025,
     kFreq12000Hz = 12000,
     kFreq16000Hz = 16000,
     kFreq22050Hz = 22050,
     kFreq24000Hz = 24000,
     kFreq32000Hz = 32000,
     kFreq44100 = 44100,
     kFreq48000 = 48000,
    };

    AudioResampler *mResampler;
    int16_t* mInput;
    int nbChannels;
    int nbSamples;
    M4OSA_Int32 outSamplingRate;
    M4OSA_Int32 inSamplingRate;

    int16_t *mTmpInBuffer;
};

#define MAX_SAMPLEDURATION_FOR_CONVERTION 40 //ms

status_t VideoEditorResampler::getNextBuffer(AudioBufferProvider::Buffer *pBuffer, int64_t pts) {

    uint32_t dataSize = pBuffer->frameCount * this->nbChannels * sizeof(int16_t);
    mTmpInBuffer = (int16_t*)malloc(dataSize);
    memcpy(mTmpInBuffer, this->mInput, dataSize);
    pBuffer->raw = (void*)mTmpInBuffer;

    return OK;
}

void VideoEditorResampler::releaseBuffer(AudioBufferProvider::Buffer *pBuffer) {

    if(pBuffer->raw != NULL) {
        free(pBuffer->raw);
        pBuffer->raw = NULL;
        mTmpInBuffer = NULL;
    }
    pBuffer->frameCount = 0;
}

extern "C" {

M4OSA_Context  LVAudioResamplerCreate(M4OSA_Int32 bitDepth, M4OSA_Int32 inChannelCount,
                                     M4OSA_Int32 sampleRate, M4OSA_Int32 quality) {

    VideoEditorResampler *context = new VideoEditorResampler();
    context->mResampler = AudioResampler::create(
        bitDepth, inChannelCount, sampleRate);
    if (context->mResampler == NULL) {
        return NULL;
    }
    context->mResampler->setSampleRate(android::VideoEditorResampler::kFreq32000Hz);
    context->mResampler->setVolume(0x1000, 0x1000);
    context->nbChannels = inChannelCount;
    context->outSamplingRate = sampleRate;
    context->mInput = NULL;
    context->mTmpInBuffer = NULL;

    return ((M4OSA_Context )context);
}


void LVAudiosetSampleRate(M4OSA_Context resamplerContext, M4OSA_Int32 inSampleRate) {

    VideoEditorResampler *context =
      (VideoEditorResampler *)resamplerContext;
    context->mResampler->setSampleRate(inSampleRate);
    /*
     * nbSamples is calculated for 40ms worth of data;hence sample rate
     * is used to calculate the nbSamples
     */
    context->inSamplingRate = inSampleRate;
    // Allocate buffer for maximum allowed number of samples.
    context->mInput = (int16_t*)malloc( (inSampleRate * MAX_SAMPLEDURATION_FOR_CONVERTION *
                                   context->nbChannels * sizeof(int16_t)) / 1000);
}

void LVAudiosetVolume(M4OSA_Context resamplerContext, M4OSA_Int16 left, M4OSA_Int16 right) {

    VideoEditorResampler *context =
       (VideoEditorResampler *)resamplerContext;
    context->mResampler->setVolume(left,right);
}

void LVDestroy(M4OSA_Context resamplerContext) {

    VideoEditorResampler *context =
       (VideoEditorResampler *)resamplerContext;

    if (context->mTmpInBuffer != NULL) {
        free(context->mTmpInBuffer);
        context->mTmpInBuffer = NULL;
    }

    if (context->mInput != NULL) {
        free(context->mInput);
        context->mInput = NULL;
    }

    if (context->mResampler != NULL) {
        delete context->mResampler;
        context->mResampler = NULL;
    }

    if (context != NULL) {
        delete context;
        context = NULL;
    }
}

void LVAudioresample_LowQuality(M4OSA_Int16* out, M4OSA_Int16* input,
                                     M4OSA_Int32 outFrameCount, M4OSA_Context resamplerContext) {

    VideoEditorResampler *context =
      (VideoEditorResampler *)resamplerContext;
    int32_t *pTmpBuffer = NULL;

    context->nbSamples = (context->inSamplingRate * outFrameCount) / context->outSamplingRate;
    memcpy(context->mInput,input,(context->nbSamples * context->nbChannels * sizeof(int16_t)));

    /*
     SRC module always gives stereo output, hence 2 for stereo audio
    */
    pTmpBuffer = (int32_t*)malloc(outFrameCount * 2 * sizeof(int32_t));
    memset(pTmpBuffer, 0x00, outFrameCount * 2 * sizeof(int32_t));

    context->mResampler->resample((int32_t *)pTmpBuffer,
       (size_t)outFrameCount, (VideoEditorResampler *)resamplerContext);
    // Convert back to 16 bits
    ditherAndClamp((int32_t*)out, pTmpBuffer, outFrameCount);
    free(pTmpBuffer);
    pTmpBuffer = NULL;
}

}

} //namespace android
