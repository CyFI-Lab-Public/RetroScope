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

#include <audio_utils/sndfile.h>
#include <audio_utils/primitives.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>

#define WAVE_FORMAT_PCM         1
#define WAVE_FORMAT_IEEE_FLOAT  3
#define WAVE_FORMAT_EXTENSIBLE  0xFFFE

struct SNDFILE_ {
    int mode;
    uint8_t *temp;  // realloc buffer used for shrinking 16 bits to 8 bits and byte-swapping
    FILE *stream;
    size_t bytesPerFrame;
    size_t remaining;   // frames unread for SFM_READ, frames written for SFM_WRITE
    SF_INFO info;
};

static unsigned little2u(unsigned char *ptr)
{
    return (ptr[1] << 8) + ptr[0];
}

static unsigned little4u(unsigned char *ptr)
{
    return (ptr[3] << 24) + (ptr[2] << 16) + (ptr[1] << 8) + ptr[0];
}

static int isLittleEndian(void)
{
    static const short one = 1;
    return *((const char *) &one) == 1;
}

// "swab" conflicts with OS X <string.h>
static void my_swab(short *ptr, size_t numToSwap)
{
    while (numToSwap > 0) {
        *ptr = little2u((unsigned char *) ptr);
        --numToSwap;
        ++ptr;
    }
}

static SNDFILE *sf_open_read(const char *path, SF_INFO *info)
{
    FILE *stream = fopen(path, "rb");
    if (stream == NULL) {
        fprintf(stderr, "fopen %s failed errno %d\n", path, errno);
        return NULL;
    }

    SNDFILE *handle = (SNDFILE *) malloc(sizeof(SNDFILE));
    handle->mode = SFM_READ;
    handle->temp = NULL;
    handle->stream = stream;
    handle->info.format = SF_FORMAT_WAV;

    // don't attempt to parse all valid forms, just the most common ones
    unsigned char wav[12];
    size_t actual;
    actual = fread(wav, sizeof(char), sizeof(wav), stream);
    if (actual < 12) {
        fprintf(stderr, "actual %u < 44\n", actual);
        goto close;
    }
    if (memcmp(wav, "RIFF", 4)) {
        fprintf(stderr, "wav != RIFF\n");
        goto close;
    }
    unsigned riffSize = little4u(&wav[4]);
    if (riffSize < 4) {
        fprintf(stderr, "riffSize %u < 4\n", riffSize);
        goto close;
    }
    if (memcmp(&wav[8], "WAVE", 4)) {
        fprintf(stderr, "missing WAVE\n");
        goto close;
    }
    size_t remaining = riffSize - 4;
    int hadFmt = 0;
    int hadData = 0;
    while (remaining >= 8) {
        unsigned char chunk[8];
        actual = fread(chunk, sizeof(char), sizeof(chunk), stream);
        if (actual != sizeof(chunk)) {
            fprintf(stderr, "actual %u != %u\n", actual, sizeof(chunk));
            goto close;
        }
        remaining -= 8;
        unsigned chunkSize = little4u(&chunk[4]);
        if (chunkSize > remaining) {
            fprintf(stderr, "chunkSize %u > remaining %u\n", chunkSize, remaining);
            goto close;
        }
        if (!memcmp(&chunk[0], "fmt ", 4)) {
            if (hadFmt) {
                fprintf(stderr, "multiple fmt\n");
                goto close;
            }
            if (chunkSize < 2) {
                fprintf(stderr, "chunkSize %u < 2\n", chunkSize);
                goto close;
            }
            unsigned char fmt[40];
            actual = fread(fmt, sizeof(char), 2, stream);
            if (actual != 2) {
                fprintf(stderr, "actual %u != 2\n", actual);
                goto close;
            }
            unsigned format = little2u(&fmt[0]);
            size_t minSize = 0;
            switch (format) {
            case WAVE_FORMAT_PCM:
            case WAVE_FORMAT_IEEE_FLOAT:
                minSize = 16;
                break;
            case WAVE_FORMAT_EXTENSIBLE:
                minSize = 40;
                break;
            default:
                fprintf(stderr, "unsupported format %u\n", format);
                goto close;
            }
            if (chunkSize < minSize) {
                fprintf(stderr, "chunkSize %u < minSize %u\n", chunkSize, minSize);
                goto close;
            }
            actual = fread(&fmt[2], sizeof(char), minSize - 2, stream);
            if (actual != minSize - 2) {
                fprintf(stderr, "actual %u != %u\n", actual, minSize - 16);
                goto close;
            }
            if (chunkSize > minSize) {
                fseek(stream, (long) (chunkSize - minSize), SEEK_CUR);
            }
            unsigned channels = little2u(&fmt[2]);
            if (channels != 1 && channels != 2) {
                fprintf(stderr, "channels %u != 1 or 2\n", channels);
                goto close;
            }
            unsigned samplerate = little4u(&fmt[4]);
            if (samplerate == 0) {
                fprintf(stderr, "samplerate %u == 0\n", samplerate);
                goto close;
            }
            // ignore byte rate
            // ignore block alignment
            unsigned bitsPerSample = little2u(&fmt[14]);
            if (bitsPerSample != 8 && bitsPerSample != 16 && bitsPerSample != 32) {
                fprintf(stderr, "bitsPerSample %u != 8 or 16 or 32\n", bitsPerSample);
                goto close;
            }
            unsigned bytesPerFrame = (bitsPerSample >> 3) * channels;
            handle->bytesPerFrame = bytesPerFrame;
            handle->info.samplerate = samplerate;
            handle->info.channels = channels;
            switch (bitsPerSample) {
            case 8:
                handle->info.format |= SF_FORMAT_PCM_U8;
                break;
            case 16:
                handle->info.format |= SF_FORMAT_PCM_16;
                break;
            case 32:
                if (format == WAVE_FORMAT_IEEE_FLOAT)
                    handle->info.format |= SF_FORMAT_FLOAT;
                else
                    handle->info.format |= SF_FORMAT_PCM_32;
                break;
            }
            hadFmt = 1;
        } else if (!memcmp(&chunk[0], "data", 4)) {
            if (!hadFmt) {
                fprintf(stderr, "data not preceded by fmt\n");
                goto close;
            }
            if (hadData) {
                fprintf(stderr, "multiple data\n");
                goto close;
            }
            handle->remaining = chunkSize / handle->bytesPerFrame;
            handle->info.frames = handle->remaining;
            hadData = 1;
        } else if (!memcmp(&chunk[0], "fact", 4)) {
            // ignore fact
            if (chunkSize > 0) {
                fseek(stream, (long) chunkSize, SEEK_CUR);
            }
        } else {
            // ignore unknown chunk
            fprintf(stderr, "ignoring unknown chunk %c%c%c%c\n",
                    chunk[0], chunk[1], chunk[2], chunk[3]);
            if (chunkSize > 0) {
                fseek(stream, (long) chunkSize, SEEK_CUR);
            }
        }
        remaining -= chunkSize;
    }
    if (remaining > 0) {
        fprintf(stderr, "partial chunk at end of RIFF, remaining %u\n", remaining);
        goto close;
    }
    if (!hadData) {
        fprintf(stderr, "missing data\n");
        goto close;
    }
    *info = handle->info;
    return handle;

close:
    free(handle);
    fclose(stream);
    return NULL;
}

static void write4u(unsigned char *ptr, unsigned u)
{
    ptr[0] = u;
    ptr[1] = u >> 8;
    ptr[2] = u >> 16;
    ptr[3] = u >> 24;
}

static SNDFILE *sf_open_write(const char *path, SF_INFO *info)
{
    int sub = info->format & SF_FORMAT_SUBMASK;
    if (!(
            (info->samplerate > 0) &&
            (info->channels == 1 || info->channels == 2) &&
            ((info->format & SF_FORMAT_TYPEMASK) == SF_FORMAT_WAV) &&
            (sub == SF_FORMAT_PCM_16 || sub == SF_FORMAT_PCM_U8 || sub == SF_FORMAT_FLOAT)
          )) {
        return NULL;
    }
    FILE *stream = fopen(path, "w+b");
    unsigned char wav[58];
    memset(wav, 0, sizeof(wav));
    memcpy(wav, "RIFF", 4);
    memcpy(&wav[8], "WAVEfmt ", 8);
    if (sub == SF_FORMAT_FLOAT) {
        wav[4] = 50;    // riffSize
        wav[16] = 18;   // fmtSize
        wav[20] = WAVE_FORMAT_IEEE_FLOAT;
    } else {
        wav[4] = 36;    // riffSize
        wav[16] = 16;   // fmtSize
        wav[20] = WAVE_FORMAT_PCM;
    }
    wav[22] = info->channels;
    write4u(&wav[24], info->samplerate);
    unsigned bitsPerSample;
    switch (sub) {
    case SF_FORMAT_PCM_16:
        bitsPerSample = 16;
        break;
    case SF_FORMAT_PCM_U8:
        bitsPerSample = 8;
        break;
    case SF_FORMAT_FLOAT:
        bitsPerSample = 32;
        break;
    default:    // not reachable
        bitsPerSample = 0;
        break;
    }
    unsigned blockAlignment = (bitsPerSample >> 3) * info->channels;
    unsigned byteRate = info->samplerate * blockAlignment;
    write4u(&wav[28], byteRate);
    wav[32] = blockAlignment;
    wav[34] = bitsPerSample;
    if (sub == SF_FORMAT_FLOAT) {
        memcpy(&wav[38], "fact", 4);
        wav[42] = 4;
        memcpy(&wav[50], "data", 4);
    } else
        memcpy(&wav[36], "data", 4);
    // dataSize is initially zero
    (void) fwrite(wav, sizeof(wav), 1, stream);
    SNDFILE *handle = (SNDFILE *) malloc(sizeof(SNDFILE));
    handle->mode = SFM_WRITE;
    handle->temp = NULL;
    handle->stream = stream;
    handle->bytesPerFrame = blockAlignment;
    handle->remaining = 0;
    handle->info = *info;
    return handle;
}

SNDFILE *sf_open(const char *path, int mode, SF_INFO *info)
{
    if (path == NULL || info == NULL) {
        fprintf(stderr, "path=%p info=%p\n", path, info);
        return NULL;
    }
    switch (mode) {
    case SFM_READ:
        return sf_open_read(path, info);
    case SFM_WRITE:
        return sf_open_write(path, info);
    default:
        fprintf(stderr, "mode=%d\n", mode);
        return NULL;
    }
}

void sf_close(SNDFILE *handle)
{
    if (handle == NULL)
        return;
    free(handle->temp);
    if (handle->mode == SFM_WRITE) {
        (void) fflush(handle->stream);
        rewind(handle->stream);
        unsigned char wav[58];
        size_t extra = (handle->info.format & SF_FORMAT_SUBMASK) == SF_FORMAT_FLOAT ? 14 : 0;
        (void) fread(wav, 44 + extra, 1, handle->stream);
        unsigned dataSize = handle->remaining * handle->bytesPerFrame;
        write4u(&wav[4], dataSize + 36 + extra);    // riffSize
        write4u(&wav[40 + extra], dataSize);        // dataSize
        rewind(handle->stream);
        (void) fwrite(wav, 44 + extra, 1, handle->stream);
    }
    (void) fclose(handle->stream);
    free(handle);
}

sf_count_t sf_readf_short(SNDFILE *handle, short *ptr, sf_count_t desiredFrames)
{
    if (handle == NULL || handle->mode != SFM_READ || ptr == NULL || !handle->remaining ||
            desiredFrames <= 0) {
        return 0;
    }
    if (handle->remaining < (size_t) desiredFrames)
        desiredFrames = handle->remaining;
    // does not check for numeric overflow
    size_t desiredBytes = desiredFrames * handle->bytesPerFrame;
    size_t actualBytes;
    void *temp = NULL;
    unsigned format = handle->info.format & SF_FORMAT_SUBMASK;
    if (format == SF_FORMAT_PCM_32 || format == SF_FORMAT_FLOAT) {
        temp = malloc(desiredBytes);
        actualBytes = fread(temp, sizeof(char), desiredBytes, handle->stream);
    } else {
        actualBytes = fread(ptr, sizeof(char), desiredBytes, handle->stream);
    }
    size_t actualFrames = actualBytes / handle->bytesPerFrame;
    handle->remaining -= actualFrames;
    switch (format) {
    case SF_FORMAT_PCM_U8:
        memcpy_to_i16_from_u8(ptr, (unsigned char *) ptr, actualFrames * handle->info.channels);
        break;
    case SF_FORMAT_PCM_16:
        if (!isLittleEndian())
            my_swab(ptr, actualFrames * handle->info.channels);
        break;
    case SF_FORMAT_PCM_32:
        memcpy_to_i16_from_i32(ptr, (const int *) temp, actualFrames * handle->info.channels);
        free(temp);
        break;
    case SF_FORMAT_FLOAT:
        memcpy_to_i16_from_float(ptr, (const float *) temp, actualFrames * handle->info.channels);
        free(temp);
        break;
    default:
        memset(ptr, 0, actualFrames * handle->info.channels * sizeof(short));
        break;
    }
    return actualFrames;
}

sf_count_t sf_readf_float(SNDFILE *handle, float *ptr, sf_count_t desiredFrames)
{
    return 0;
}

sf_count_t sf_writef_short(SNDFILE *handle, const short *ptr, sf_count_t desiredFrames)
{
    if (handle == NULL || handle->mode != SFM_WRITE || ptr == NULL || desiredFrames <= 0)
        return 0;
    size_t desiredBytes = desiredFrames * handle->bytesPerFrame;
    size_t actualBytes = 0;
    switch (handle->info.format & SF_FORMAT_SUBMASK) {
    case SF_FORMAT_PCM_U8:
        handle->temp = realloc(handle->temp, desiredBytes);
        memcpy_to_u8_from_i16(handle->temp, ptr, desiredBytes);
        actualBytes = fwrite(handle->temp, sizeof(char), desiredBytes, handle->stream);
        break;
    case SF_FORMAT_PCM_16:
        // does not check for numeric overflow
        if (isLittleEndian()) {
            actualBytes = fwrite(ptr, sizeof(char), desiredBytes, handle->stream);
        } else {
            handle->temp = realloc(handle->temp, desiredBytes);
            memcpy(handle->temp, ptr, desiredBytes);
            my_swab((short *) handle->temp, desiredFrames * handle->info.channels);
            actualBytes = fwrite(handle->temp, sizeof(char), desiredBytes, handle->stream);
        }
        break;
    case SF_FORMAT_FLOAT:   // transcoding from short to float not yet implemented
    default:
        break;
    }
    size_t actualFrames = actualBytes / handle->bytesPerFrame;
    handle->remaining += actualFrames;
    return actualFrames;
}

sf_count_t sf_writef_float(SNDFILE *handle, const float *ptr, sf_count_t desiredFrames)
{
    if (handle == NULL || handle->mode != SFM_WRITE || ptr == NULL || desiredFrames <= 0)
        return 0;
    size_t desiredBytes = desiredFrames * handle->bytesPerFrame;
    size_t actualBytes = 0;
    switch (handle->info.format & SF_FORMAT_SUBMASK) {
    case SF_FORMAT_FLOAT:
        actualBytes = fwrite(ptr, sizeof(char), desiredBytes, handle->stream);
        break;
    case SF_FORMAT_PCM_U8:  // transcoding from float to byte/short not yet implemented
    case SF_FORMAT_PCM_16:
    default:
        break;
    }
    size_t actualFrames = actualBytes / handle->bytesPerFrame;
    handle->remaining += actualFrames;
    return actualFrames;
}
