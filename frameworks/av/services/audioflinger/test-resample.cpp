/*
 * Copyright (C) 2012 The Android Open Source Project
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

#include "AudioResampler.h"
#include <media/AudioBufferProvider.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <errno.h>
#include <time.h>
#include <math.h>

using namespace android;

struct HeaderWav {
    HeaderWav(size_t size, int nc, int sr, int bits) {
        strncpy(RIFF, "RIFF", 4);
        chunkSize = size + sizeof(HeaderWav);
        strncpy(WAVE, "WAVE", 4);
        strncpy(fmt,  "fmt ", 4);
        fmtSize = 16;
        audioFormat = 1;
        numChannels = nc;
        samplesRate = sr;
        byteRate = sr * numChannels * (bits/8);
        align = nc*(bits/8);
        bitsPerSample = bits;
        strncpy(data, "data", 4);
        dataSize = size;
    }

    char RIFF[4];           // RIFF
    uint32_t chunkSize;     // File size
    char WAVE[4];        // WAVE
    char fmt[4];            // fmt\0
    uint32_t fmtSize;       // fmt size
    uint16_t audioFormat;   // 1=PCM
    uint16_t numChannels;   // num channels
    uint32_t samplesRate;   // sample rate in hz
    uint32_t byteRate;      // Bps
    uint16_t align;         // 2=16-bit mono, 4=16-bit stereo
    uint16_t bitsPerSample; // bits per sample
    char data[4];           // "data"
    uint32_t dataSize;      // size
};

static int usage(const char* name) {
    fprintf(stderr,"Usage: %s [-p] [-h] [-s] [-q {dq|lq|mq|hq|vhq}] [-i input-sample-rate] "
                   "[-o output-sample-rate] [<input-file>] <output-file>\n", name);
    fprintf(stderr,"    -p    enable profiling\n");
    fprintf(stderr,"    -h    create wav file\n");
    fprintf(stderr,"    -s    stereo\n");
    fprintf(stderr,"    -q    resampler quality\n");
    fprintf(stderr,"              dq  : default quality\n");
    fprintf(stderr,"              lq  : low quality\n");
    fprintf(stderr,"              mq  : medium quality\n");
    fprintf(stderr,"              hq  : high quality\n");
    fprintf(stderr,"              vhq : very high quality\n");
    fprintf(stderr,"    -i    input file sample rate\n");
    fprintf(stderr,"    -o    output file sample rate\n");
    return -1;
}

int main(int argc, char* argv[]) {

    const char* const progname = argv[0];
    bool profiling = false;
    bool writeHeader = false;
    int channels = 1;
    int input_freq = 0;
    int output_freq = 0;
    AudioResampler::src_quality quality = AudioResampler::DEFAULT_QUALITY;

    int ch;
    while ((ch = getopt(argc, argv, "phsq:i:o:")) != -1) {
        switch (ch) {
        case 'p':
            profiling = true;
            break;
        case 'h':
            writeHeader = true;
            break;
        case 's':
            channels = 2;
            break;
        case 'q':
            if (!strcmp(optarg, "dq"))
                quality = AudioResampler::DEFAULT_QUALITY;
            else if (!strcmp(optarg, "lq"))
                quality = AudioResampler::LOW_QUALITY;
            else if (!strcmp(optarg, "mq"))
                quality = AudioResampler::MED_QUALITY;
            else if (!strcmp(optarg, "hq"))
                quality = AudioResampler::HIGH_QUALITY;
            else if (!strcmp(optarg, "vhq"))
                quality = AudioResampler::VERY_HIGH_QUALITY;
            else {
                usage(progname);
                return -1;
            }
            break;
        case 'i':
            input_freq = atoi(optarg);
            break;
        case 'o':
            output_freq = atoi(optarg);
            break;
        case '?':
        default:
            usage(progname);
            return -1;
        }
    }
    argc -= optind;
    argv += optind;

    const char* file_in = NULL;
    const char* file_out = NULL;
    if (argc == 1) {
        file_out = argv[0];
    } else if (argc == 2) {
        file_in = argv[0];
        file_out = argv[1];
    } else {
        usage(progname);
        return -1;
    }

    // ----------------------------------------------------------

    size_t input_size;
    void* input_vaddr;
    if (argc == 2) {
        struct stat st;
        if (stat(file_in, &st) < 0) {
            fprintf(stderr, "stat: %s\n", strerror(errno));
            return -1;
        }

        int input_fd = open(file_in, O_RDONLY);
        if (input_fd < 0) {
            fprintf(stderr, "open: %s\n", strerror(errno));
            return -1;
        }

        input_size = st.st_size;
        input_vaddr = mmap(0, input_size, PROT_READ, MAP_PRIVATE, input_fd, 0);
        if (input_vaddr == MAP_FAILED ) {
            fprintf(stderr, "mmap: %s\n", strerror(errno));
            return -1;
        }
    } else {
        double k = 1000; // Hz / s
        double time = (input_freq / 2) / k;
        size_t input_frames = size_t(input_freq * time);
        input_size = channels * sizeof(int16_t) * input_frames;
        input_vaddr = malloc(input_size);
        int16_t* in = (int16_t*)input_vaddr;
        for (size_t i=0 ; i<input_frames ; i++) {
            double t = double(i) / input_freq;
            double y = sin(M_PI * k * t * t);
            int16_t yi = floor(y * 32767.0 + 0.5);
            for (size_t j=0 ; j<(size_t)channels ; j++) {
                in[i*channels + j] = yi / (1+j);
            }
        }
    }

    // ----------------------------------------------------------

    class Provider: public AudioBufferProvider {
        int16_t* mAddr;
        size_t mNumFrames;
    public:
        Provider(const void* addr, size_t size, int channels) {
            mAddr = (int16_t*) addr;
            mNumFrames = size / (channels*sizeof(int16_t));
        }
        virtual status_t getNextBuffer(Buffer* buffer,
                int64_t pts = kInvalidPTS) {
            buffer->frameCount = mNumFrames;
            buffer->i16 = mAddr;
            return NO_ERROR;
        }
        virtual void releaseBuffer(Buffer* buffer) {
        }
    } provider(input_vaddr, input_size, channels);

    size_t input_frames = input_size / (channels * sizeof(int16_t));
    size_t output_size = 2 * 4 * ((int64_t) input_frames * output_freq) / input_freq;
    output_size &= ~7; // always stereo, 32-bits

    void* output_vaddr = malloc(output_size);

    if (profiling) {
        AudioResampler* resampler = AudioResampler::create(16, channels,
                output_freq, quality);

        size_t out_frames = output_size/8;
        resampler->setSampleRate(input_freq);
        resampler->setVolume(0x1000, 0x1000);

        memset(output_vaddr, 0, output_size);
        timespec start, end;
        clock_gettime(CLOCK_MONOTONIC, &start);
        resampler->resample((int*) output_vaddr, out_frames, &provider);
        resampler->resample((int*) output_vaddr, out_frames, &provider);
        resampler->resample((int*) output_vaddr, out_frames, &provider);
        resampler->resample((int*) output_vaddr, out_frames, &provider);
        clock_gettime(CLOCK_MONOTONIC, &end);
        int64_t start_ns = start.tv_sec * 1000000000LL + start.tv_nsec;
        int64_t end_ns = end.tv_sec * 1000000000LL + end.tv_nsec;
        int64_t time = (end_ns - start_ns)/4;
        printf("%f Mspl/s\n", out_frames/(time/1e9)/1e6);

        delete resampler;
    }

    AudioResampler* resampler = AudioResampler::create(16, channels,
            output_freq, quality);
    size_t out_frames = output_size/8;
    resampler->setSampleRate(input_freq);
    resampler->setVolume(0x1000, 0x1000);

    memset(output_vaddr, 0, output_size);
    resampler->resample((int*) output_vaddr, out_frames, &provider);

    // down-mix (we just truncate and keep the left channel)
    int32_t* out = (int32_t*) output_vaddr;
    int16_t* convert = (int16_t*) malloc(out_frames * channels * sizeof(int16_t));
    for (size_t i = 0; i < out_frames; i++) {
        for (int j=0 ; j<channels ; j++) {
            int32_t s = out[i * 2 + j] >> 12;
            if (s > 32767)       s =  32767;
            else if (s < -32768) s = -32768;
            convert[i * channels + j] = int16_t(s);
        }
    }

    // write output to disk
    int output_fd = open(file_out, O_WRONLY | O_CREAT | O_TRUNC,
            S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
    if (output_fd < 0) {
        fprintf(stderr, "open: %s\n", strerror(errno));
        return -1;
    }

    if (writeHeader) {
        HeaderWav wav(out_frames * channels * sizeof(int16_t), channels, output_freq, 16);
        write(output_fd, &wav, sizeof(wav));
    }

    write(output_fd, convert, out_frames * channels * sizeof(int16_t));
    close(output_fd);

    return 0;
}
