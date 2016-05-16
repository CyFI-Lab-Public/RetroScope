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

#define LOG_TAG "r_submix"
//#define LOG_NDEBUG 0

#include <errno.h>
#include <pthread.h>
#include <stdint.h>
#include <sys/time.h>
#include <stdlib.h>

#include <cutils/log.h>
#include <cutils/str_parms.h>
#include <cutils/properties.h>

#include <hardware/hardware.h>
#include <system/audio.h>
#include <hardware/audio.h>

#include <media/nbaio/MonoPipe.h>
#include <media/nbaio/MonoPipeReader.h>
#include <media/AudioBufferProvider.h>

#include <utils/String8.h>
#include <media/AudioParameter.h>

extern "C" {

namespace android {

#define MAX_PIPE_DEPTH_IN_FRAMES     (1024*8)
// The duration of MAX_READ_ATTEMPTS * READ_ATTEMPT_SLEEP_MS must be stricly inferior to
//   the duration of a record buffer at the current record sample rate (of the device, not of
//   the recording itself). Here we have:
//      3 * 5ms = 15ms < 1024 frames * 1000 / 48000 = 21.333ms
#define MAX_READ_ATTEMPTS            3
#define READ_ATTEMPT_SLEEP_MS        5 // 5ms between two read attempts when pipe is empty
#define DEFAULT_RATE_HZ              48000 // default sample rate

struct submix_config {
    audio_format_t format;
    audio_channel_mask_t channel_mask;
    unsigned int rate; // sample rate for the device
    unsigned int period_size; // size of the audio pipe is period_size * period_count in frames
    unsigned int period_count;
};

struct submix_audio_device {
    struct audio_hw_device device;
    bool output_standby;
    bool input_standby;
    submix_config config;
    // Pipe variables: they handle the ring buffer that "pipes" audio:
    //  - from the submix virtual audio output == what needs to be played
    //    remotely, seen as an output for AudioFlinger
    //  - to the virtual audio source == what is captured by the component
    //    which "records" the submix / virtual audio source, and handles it as needed.
    // A usecase example is one where the component capturing the audio is then sending it over
    // Wifi for presentation on a remote Wifi Display device (e.g. a dongle attached to a TV, or a
    // TV with Wifi Display capabilities), or to a wireless audio player.
    sp<MonoPipe>       rsxSink;
    sp<MonoPipeReader> rsxSource;

    // device lock, also used to protect access to the audio pipe
    pthread_mutex_t lock;
};

struct submix_stream_out {
    struct audio_stream_out stream;
    struct submix_audio_device *dev;
};

struct submix_stream_in {
    struct audio_stream_in stream;
    struct submix_audio_device *dev;
    bool output_standby; // output standby state as seen from record thread

    // wall clock when recording starts
    struct timespec record_start_time;
    // how many frames have been requested to be read
    int64_t read_counter_frames;
};


/* audio HAL functions */

static uint32_t out_get_sample_rate(const struct audio_stream *stream)
{
    const struct submix_stream_out *out =
            reinterpret_cast<const struct submix_stream_out *>(stream);
    uint32_t out_rate = out->dev->config.rate;
    //ALOGV("out_get_sample_rate() returns %u", out_rate);
    return out_rate;
}

static int out_set_sample_rate(struct audio_stream *stream, uint32_t rate)
{
    if ((rate != 44100) && (rate != 48000)) {
        ALOGE("out_set_sample_rate(rate=%u) rate unsupported", rate);
        return -ENOSYS;
    }
    struct submix_stream_out *out = reinterpret_cast<struct submix_stream_out *>(stream);
    //ALOGV("out_set_sample_rate(rate=%u)", rate);
    out->dev->config.rate = rate;
    return 0;
}

static size_t out_get_buffer_size(const struct audio_stream *stream)
{
    const struct submix_stream_out *out =
            reinterpret_cast<const struct submix_stream_out *>(stream);
    const struct submix_config& config_out = out->dev->config;
    size_t buffer_size = config_out.period_size * popcount(config_out.channel_mask)
                            * sizeof(int16_t); // only PCM 16bit
    //ALOGV("out_get_buffer_size() returns %u, period size=%u",
    //        buffer_size, config_out.period_size);
    return buffer_size;
}

static audio_channel_mask_t out_get_channels(const struct audio_stream *stream)
{
    const struct submix_stream_out *out =
            reinterpret_cast<const struct submix_stream_out *>(stream);
    uint32_t channels = out->dev->config.channel_mask;
    //ALOGV("out_get_channels() returns %08x", channels);
    return channels;
}

static audio_format_t out_get_format(const struct audio_stream *stream)
{
    return AUDIO_FORMAT_PCM_16_BIT;
}

static int out_set_format(struct audio_stream *stream, audio_format_t format)
{
    if (format != AUDIO_FORMAT_PCM_16_BIT) {
        return -ENOSYS;
    } else {
        return 0;
    }
}

static int out_standby(struct audio_stream *stream)
{
    ALOGI("out_standby()");

    const struct submix_stream_out *out = reinterpret_cast<const struct submix_stream_out *>(stream);

    pthread_mutex_lock(&out->dev->lock);

    out->dev->output_standby = true;

    pthread_mutex_unlock(&out->dev->lock);

    return 0;
}

static int out_dump(const struct audio_stream *stream, int fd)
{
    return 0;
}

static int out_set_parameters(struct audio_stream *stream, const char *kvpairs)
{
    int exiting = -1;
    AudioParameter parms = AudioParameter(String8(kvpairs));
    // FIXME this is using hard-coded strings but in the future, this functionality will be
    //       converted to use audio HAL extensions required to support tunneling
    if ((parms.getInt(String8("exiting"), exiting) == NO_ERROR) && (exiting > 0)) {
        const struct submix_stream_out *out =
                reinterpret_cast<const struct submix_stream_out *>(stream);

        pthread_mutex_lock(&out->dev->lock);

        { // using the sink
            sp<MonoPipe> sink = out->dev->rsxSink.get();
            if (sink == 0) {
                pthread_mutex_unlock(&out->dev->lock);
                return 0;
            }

            ALOGI("shutdown");
            sink->shutdown(true);
        } // done using the sink

        pthread_mutex_unlock(&out->dev->lock);
    }

    return 0;
}

static char * out_get_parameters(const struct audio_stream *stream, const char *keys)
{
    return strdup("");
}

static uint32_t out_get_latency(const struct audio_stream_out *stream)
{
    const struct submix_stream_out *out =
            reinterpret_cast<const struct submix_stream_out *>(stream);
    const struct submix_config * config_out = &(out->dev->config);
    uint32_t latency = (MAX_PIPE_DEPTH_IN_FRAMES * 1000) / config_out->rate;
    ALOGV("out_get_latency() returns %u", latency);
    return latency;
}

static int out_set_volume(struct audio_stream_out *stream, float left,
                          float right)
{
    return -ENOSYS;
}

static ssize_t out_write(struct audio_stream_out *stream, const void* buffer,
                         size_t bytes)
{
    //ALOGV("out_write(bytes=%d)", bytes);
    ssize_t written_frames = 0;
    struct submix_stream_out *out = reinterpret_cast<struct submix_stream_out *>(stream);

    const size_t frame_size = audio_stream_frame_size(&stream->common);
    const size_t frames = bytes / frame_size;

    pthread_mutex_lock(&out->dev->lock);

    out->dev->output_standby = false;

    sp<MonoPipe> sink = out->dev->rsxSink.get();
    if (sink != 0) {
        if (sink->isShutdown()) {
            sink.clear();
            pthread_mutex_unlock(&out->dev->lock);
            // the pipe has already been shutdown, this buffer will be lost but we must
            //   simulate timing so we don't drain the output faster than realtime
            usleep(frames * 1000000 / out_get_sample_rate(&stream->common));
            return bytes;
        }
    } else {
        pthread_mutex_unlock(&out->dev->lock);
        ALOGE("out_write without a pipe!");
        ALOG_ASSERT("out_write without a pipe!");
        return 0;
    }

    pthread_mutex_unlock(&out->dev->lock);

    written_frames = sink->write(buffer, frames);

    if (written_frames < 0) {
        if (written_frames == (ssize_t)NEGOTIATE) {
            ALOGE("out_write() write to pipe returned NEGOTIATE");

            pthread_mutex_lock(&out->dev->lock);
            sink.clear();
            pthread_mutex_unlock(&out->dev->lock);

            written_frames = 0;
            return 0;
        } else {
            // write() returned UNDERRUN or WOULD_BLOCK, retry
            ALOGE("out_write() write to pipe returned unexpected %d", written_frames);
            written_frames = sink->write(buffer, frames);
        }
    }

    pthread_mutex_lock(&out->dev->lock);
    sink.clear();
    pthread_mutex_unlock(&out->dev->lock);

    if (written_frames < 0) {
        ALOGE("out_write() failed writing to pipe with %d", written_frames);
        return 0;
    } else {
        ALOGV("out_write() wrote %lu bytes)", written_frames * frame_size);
        return written_frames * frame_size;
    }
}

static int out_get_render_position(const struct audio_stream_out *stream,
                                   uint32_t *dsp_frames)
{
    return -EINVAL;
}

static int out_add_audio_effect(const struct audio_stream *stream, effect_handle_t effect)
{
    return 0;
}

static int out_remove_audio_effect(const struct audio_stream *stream, effect_handle_t effect)
{
    return 0;
}

static int out_get_next_write_timestamp(const struct audio_stream_out *stream,
                                        int64_t *timestamp)
{
    return -EINVAL;
}

/** audio_stream_in implementation **/
static uint32_t in_get_sample_rate(const struct audio_stream *stream)
{
    const struct submix_stream_in *in = reinterpret_cast<const struct submix_stream_in *>(stream);
    //ALOGV("in_get_sample_rate() returns %u", in->dev->config.rate);
    return in->dev->config.rate;
}

static int in_set_sample_rate(struct audio_stream *stream, uint32_t rate)
{
    return -ENOSYS;
}

static size_t in_get_buffer_size(const struct audio_stream *stream)
{
    const struct submix_stream_in *in = reinterpret_cast<const struct submix_stream_in *>(stream);
    ALOGV("in_get_buffer_size() returns %u",
            in->dev->config.period_size * audio_stream_frame_size(stream));
    return in->dev->config.period_size * audio_stream_frame_size(stream);
}

static audio_channel_mask_t in_get_channels(const struct audio_stream *stream)
{
    return AUDIO_CHANNEL_IN_STEREO;
}

static audio_format_t in_get_format(const struct audio_stream *stream)
{
    return AUDIO_FORMAT_PCM_16_BIT;
}

static int in_set_format(struct audio_stream *stream, audio_format_t format)
{
    if (format != AUDIO_FORMAT_PCM_16_BIT) {
        return -ENOSYS;
    } else {
        return 0;
    }
}

static int in_standby(struct audio_stream *stream)
{
    ALOGI("in_standby()");
    const struct submix_stream_in *in = reinterpret_cast<const struct submix_stream_in *>(stream);

    pthread_mutex_lock(&in->dev->lock);

    in->dev->input_standby = true;

    pthread_mutex_unlock(&in->dev->lock);

    return 0;
}

static int in_dump(const struct audio_stream *stream, int fd)
{
    return 0;
}

static int in_set_parameters(struct audio_stream *stream, const char *kvpairs)
{
    return 0;
}

static char * in_get_parameters(const struct audio_stream *stream,
                                const char *keys)
{
    return strdup("");
}

static int in_set_gain(struct audio_stream_in *stream, float gain)
{
    return 0;
}

static ssize_t in_read(struct audio_stream_in *stream, void* buffer,
                       size_t bytes)
{
    //ALOGV("in_read bytes=%u", bytes);
    ssize_t frames_read = -1977;
    struct submix_stream_in *in = reinterpret_cast<struct submix_stream_in *>(stream);
    const size_t frame_size = audio_stream_frame_size(&stream->common);
    const size_t frames_to_read = bytes / frame_size;

    pthread_mutex_lock(&in->dev->lock);

    const bool output_standby_transition = (in->output_standby != in->dev->output_standby);
    in->output_standby = in->dev->output_standby;

    if (in->dev->input_standby || output_standby_transition) {
        in->dev->input_standby = false;
        // keep track of when we exit input standby (== first read == start "real recording")
        // or when we start recording silence, and reset projected time
        int rc = clock_gettime(CLOCK_MONOTONIC, &in->record_start_time);
        if (rc == 0) {
            in->read_counter_frames = 0;
        }
    }

    in->read_counter_frames += frames_to_read;
    size_t remaining_frames = frames_to_read;

    {
        // about to read from audio source
        sp<MonoPipeReader> source = in->dev->rsxSource.get();
        if (source == 0) {
            ALOGE("no audio pipe yet we're trying to read!");
            pthread_mutex_unlock(&in->dev->lock);
            usleep((bytes / frame_size) * 1000000 / in_get_sample_rate(&stream->common));
            memset(buffer, 0, bytes);
            return bytes;
        }

        pthread_mutex_unlock(&in->dev->lock);

        // read the data from the pipe (it's non blocking)
        int attempts = 0;
        char* buff = (char*)buffer;
        while ((remaining_frames > 0) && (attempts < MAX_READ_ATTEMPTS)) {
            attempts++;
            frames_read = source->read(buff, remaining_frames, AudioBufferProvider::kInvalidPTS);
            if (frames_read > 0) {
                remaining_frames -= frames_read;
                buff += frames_read * frame_size;
                //ALOGV("  in_read (att=%d) got %ld frames, remaining=%u",
                //      attempts, frames_read, remaining_frames);
            } else {
                //ALOGE("  in_read read returned %ld", frames_read);
                usleep(READ_ATTEMPT_SLEEP_MS * 1000);
            }
        }
        // done using the source
        pthread_mutex_lock(&in->dev->lock);
        source.clear();
        pthread_mutex_unlock(&in->dev->lock);
    }

    if (remaining_frames > 0) {
        ALOGV("  remaining_frames = %d", remaining_frames);
        memset(((char*)buffer)+ bytes - (remaining_frames * frame_size), 0,
                remaining_frames * frame_size);
    }

    // compute how much we need to sleep after reading the data by comparing the wall clock with
    //   the projected time at which we should return.
    struct timespec time_after_read;// wall clock after reading from the pipe
    struct timespec record_duration;// observed record duration
    int rc = clock_gettime(CLOCK_MONOTONIC, &time_after_read);
    const uint32_t sample_rate = in_get_sample_rate(&stream->common);
    if (rc == 0) {
        // for how long have we been recording?
        record_duration.tv_sec  = time_after_read.tv_sec - in->record_start_time.tv_sec;
        record_duration.tv_nsec = time_after_read.tv_nsec - in->record_start_time.tv_nsec;
        if (record_duration.tv_nsec < 0) {
            record_duration.tv_sec--;
            record_duration.tv_nsec += 1000000000;
        }

        // read_counter_frames contains the number of frames that have been read since the beginning
        // of recording (including this call): it's converted to usec and compared to how long we've
        // been recording for, which gives us how long we must wait to sync the projected recording
        // time, and the observed recording time
        long projected_vs_observed_offset_us =
                ((int64_t)(in->read_counter_frames
                            - (record_duration.tv_sec*sample_rate)))
                        * 1000000 / sample_rate
                - (record_duration.tv_nsec / 1000);

        ALOGV("  record duration %5lds %3ldms, will wait: %7ldus",
                record_duration.tv_sec, record_duration.tv_nsec/1000000,
                projected_vs_observed_offset_us);
        if (projected_vs_observed_offset_us > 0) {
            usleep(projected_vs_observed_offset_us);
        }
    }


    ALOGV("in_read returns %d", bytes);
    return bytes;

}

static uint32_t in_get_input_frames_lost(struct audio_stream_in *stream)
{
    return 0;
}

static int in_add_audio_effect(const struct audio_stream *stream, effect_handle_t effect)
{
    return 0;
}

static int in_remove_audio_effect(const struct audio_stream *stream, effect_handle_t effect)
{
    return 0;
}

static int adev_open_output_stream(struct audio_hw_device *dev,
                                   audio_io_handle_t handle,
                                   audio_devices_t devices,
                                   audio_output_flags_t flags,
                                   struct audio_config *config,
                                   struct audio_stream_out **stream_out)
{
    ALOGV("adev_open_output_stream()");
    struct submix_audio_device *rsxadev = (struct submix_audio_device *)dev;
    struct submix_stream_out *out;
    int ret;

    out = (struct submix_stream_out *)calloc(1, sizeof(struct submix_stream_out));
    if (!out) {
        ret = -ENOMEM;
        goto err_open;
    }

    pthread_mutex_lock(&rsxadev->lock);

    out->stream.common.get_sample_rate = out_get_sample_rate;
    out->stream.common.set_sample_rate = out_set_sample_rate;
    out->stream.common.get_buffer_size = out_get_buffer_size;
    out->stream.common.get_channels = out_get_channels;
    out->stream.common.get_format = out_get_format;
    out->stream.common.set_format = out_set_format;
    out->stream.common.standby = out_standby;
    out->stream.common.dump = out_dump;
    out->stream.common.set_parameters = out_set_parameters;
    out->stream.common.get_parameters = out_get_parameters;
    out->stream.common.add_audio_effect = out_add_audio_effect;
    out->stream.common.remove_audio_effect = out_remove_audio_effect;
    out->stream.get_latency = out_get_latency;
    out->stream.set_volume = out_set_volume;
    out->stream.write = out_write;
    out->stream.get_render_position = out_get_render_position;
    out->stream.get_next_write_timestamp = out_get_next_write_timestamp;

    config->channel_mask = AUDIO_CHANNEL_OUT_STEREO;
    rsxadev->config.channel_mask = config->channel_mask;

    if ((config->sample_rate != 48000) && (config->sample_rate != 44100)) {
        config->sample_rate = DEFAULT_RATE_HZ;
    }
    rsxadev->config.rate = config->sample_rate;

    config->format = AUDIO_FORMAT_PCM_16_BIT;
    rsxadev->config.format = config->format;

    rsxadev->config.period_size = 1024;
    rsxadev->config.period_count = 4;
    out->dev = rsxadev;

    *stream_out = &out->stream;

    // initialize pipe
    {
        ALOGV("  initializing pipe");
        const NBAIO_Format format = Format_from_SR_C(config->sample_rate, 2);
        const NBAIO_Format offers[1] = {format};
        size_t numCounterOffers = 0;
        // creating a MonoPipe with optional blocking set to true.
        MonoPipe* sink = new MonoPipe(MAX_PIPE_DEPTH_IN_FRAMES, format, true/*writeCanBlock*/);
        ssize_t index = sink->negotiate(offers, 1, NULL, numCounterOffers);
        ALOG_ASSERT(index == 0);
        MonoPipeReader* source = new MonoPipeReader(sink);
        numCounterOffers = 0;
        index = source->negotiate(offers, 1, NULL, numCounterOffers);
        ALOG_ASSERT(index == 0);
        rsxadev->rsxSink = sink;
        rsxadev->rsxSource = source;
    }

    pthread_mutex_unlock(&rsxadev->lock);

    return 0;

err_open:
    *stream_out = NULL;
    return ret;
}

static void adev_close_output_stream(struct audio_hw_device *dev,
                                     struct audio_stream_out *stream)
{
    ALOGV("adev_close_output_stream()");
    struct submix_audio_device *rsxadev = (struct submix_audio_device *)dev;

    pthread_mutex_lock(&rsxadev->lock);

    rsxadev->rsxSink.clear();
    rsxadev->rsxSource.clear();
    free(stream);

    pthread_mutex_unlock(&rsxadev->lock);
}

static int adev_set_parameters(struct audio_hw_device *dev, const char *kvpairs)
{
    return -ENOSYS;
}

static char * adev_get_parameters(const struct audio_hw_device *dev,
                                  const char *keys)
{
    return strdup("");;
}

static int adev_init_check(const struct audio_hw_device *dev)
{
    ALOGI("adev_init_check()");
    return 0;
}

static int adev_set_voice_volume(struct audio_hw_device *dev, float volume)
{
    return -ENOSYS;
}

static int adev_set_master_volume(struct audio_hw_device *dev, float volume)
{
    return -ENOSYS;
}

static int adev_get_master_volume(struct audio_hw_device *dev, float *volume)
{
    return -ENOSYS;
}

static int adev_set_master_mute(struct audio_hw_device *dev, bool muted)
{
    return -ENOSYS;
}

static int adev_get_master_mute(struct audio_hw_device *dev, bool *muted)
{
    return -ENOSYS;
}

static int adev_set_mode(struct audio_hw_device *dev, audio_mode_t mode)
{
    return 0;
}

static int adev_set_mic_mute(struct audio_hw_device *dev, bool state)
{
    return -ENOSYS;
}

static int adev_get_mic_mute(const struct audio_hw_device *dev, bool *state)
{
    return -ENOSYS;
}

static size_t adev_get_input_buffer_size(const struct audio_hw_device *dev,
                                         const struct audio_config *config)
{
    //### TODO correlate this with pipe parameters
    return 4096;
}

static int adev_open_input_stream(struct audio_hw_device *dev,
                                  audio_io_handle_t handle,
                                  audio_devices_t devices,
                                  struct audio_config *config,
                                  struct audio_stream_in **stream_in)
{
    ALOGI("adev_open_input_stream()");

    struct submix_audio_device *rsxadev = (struct submix_audio_device *)dev;
    struct submix_stream_in *in;
    int ret;

    in = (struct submix_stream_in *)calloc(1, sizeof(struct submix_stream_in));
    if (!in) {
        ret = -ENOMEM;
        goto err_open;
    }

    pthread_mutex_lock(&rsxadev->lock);

    in->stream.common.get_sample_rate = in_get_sample_rate;
    in->stream.common.set_sample_rate = in_set_sample_rate;
    in->stream.common.get_buffer_size = in_get_buffer_size;
    in->stream.common.get_channels = in_get_channels;
    in->stream.common.get_format = in_get_format;
    in->stream.common.set_format = in_set_format;
    in->stream.common.standby = in_standby;
    in->stream.common.dump = in_dump;
    in->stream.common.set_parameters = in_set_parameters;
    in->stream.common.get_parameters = in_get_parameters;
    in->stream.common.add_audio_effect = in_add_audio_effect;
    in->stream.common.remove_audio_effect = in_remove_audio_effect;
    in->stream.set_gain = in_set_gain;
    in->stream.read = in_read;
    in->stream.get_input_frames_lost = in_get_input_frames_lost;

    config->channel_mask = AUDIO_CHANNEL_IN_STEREO;
    rsxadev->config.channel_mask = config->channel_mask;

    if ((config->sample_rate != 48000) && (config->sample_rate != 44100)) {
        config->sample_rate = DEFAULT_RATE_HZ;
    }
    rsxadev->config.rate = config->sample_rate;

    config->format = AUDIO_FORMAT_PCM_16_BIT;
    rsxadev->config.format = config->format;

    rsxadev->config.period_size = 1024;
    rsxadev->config.period_count = 4;

    *stream_in = &in->stream;

    in->dev = rsxadev;

    in->read_counter_frames = 0;
    in->output_standby = rsxadev->output_standby;

    pthread_mutex_unlock(&rsxadev->lock);

    return 0;

err_open:
    *stream_in = NULL;
    return ret;
}

static void adev_close_input_stream(struct audio_hw_device *dev,
                                   struct audio_stream_in *stream)
{
    ALOGV("adev_close_input_stream()");
    struct submix_audio_device *rsxadev = (struct submix_audio_device *)dev;

    pthread_mutex_lock(&rsxadev->lock);

    MonoPipe* sink = rsxadev->rsxSink.get();
    if (sink != NULL) {
        ALOGI("shutdown");
        sink->shutdown(true);
    }

    free(stream);

    pthread_mutex_unlock(&rsxadev->lock);
}

static int adev_dump(const audio_hw_device_t *device, int fd)
{
    return 0;
}

static int adev_close(hw_device_t *device)
{
    ALOGI("adev_close()");
    free(device);
    return 0;
}

static int adev_open(const hw_module_t* module, const char* name,
                     hw_device_t** device)
{
    ALOGI("adev_open(name=%s)", name);
    struct submix_audio_device *rsxadev;

    if (strcmp(name, AUDIO_HARDWARE_INTERFACE) != 0)
        return -EINVAL;

    rsxadev = (submix_audio_device*) calloc(1, sizeof(struct submix_audio_device));
    if (!rsxadev)
        return -ENOMEM;

    rsxadev->device.common.tag = HARDWARE_DEVICE_TAG;
    rsxadev->device.common.version = AUDIO_DEVICE_API_VERSION_2_0;
    rsxadev->device.common.module = (struct hw_module_t *) module;
    rsxadev->device.common.close = adev_close;

    rsxadev->device.init_check = adev_init_check;
    rsxadev->device.set_voice_volume = adev_set_voice_volume;
    rsxadev->device.set_master_volume = adev_set_master_volume;
    rsxadev->device.get_master_volume = adev_get_master_volume;
    rsxadev->device.set_master_mute = adev_set_master_mute;
    rsxadev->device.get_master_mute = adev_get_master_mute;
    rsxadev->device.set_mode = adev_set_mode;
    rsxadev->device.set_mic_mute = adev_set_mic_mute;
    rsxadev->device.get_mic_mute = adev_get_mic_mute;
    rsxadev->device.set_parameters = adev_set_parameters;
    rsxadev->device.get_parameters = adev_get_parameters;
    rsxadev->device.get_input_buffer_size = adev_get_input_buffer_size;
    rsxadev->device.open_output_stream = adev_open_output_stream;
    rsxadev->device.close_output_stream = adev_close_output_stream;
    rsxadev->device.open_input_stream = adev_open_input_stream;
    rsxadev->device.close_input_stream = adev_close_input_stream;
    rsxadev->device.dump = adev_dump;

    rsxadev->input_standby = true;
    rsxadev->output_standby = true;

    *device = &rsxadev->device.common;

    return 0;
}

static struct hw_module_methods_t hal_module_methods = {
    /* open */ adev_open,
};

struct audio_module HAL_MODULE_INFO_SYM = {
    /* common */ {
        /* tag */                HARDWARE_MODULE_TAG,
        /* module_api_version */ AUDIO_MODULE_API_VERSION_0_1,
        /* hal_api_version */    HARDWARE_HAL_API_VERSION,
        /* id */                 AUDIO_HARDWARE_MODULE_ID,
        /* name */               "Wifi Display audio HAL",
        /* author */             "The Android Open Source Project",
        /* methods */            &hal_module_methods,
        /* dso */                NULL,
        /* reserved */           { 0 },
    },
};

} //namespace android

} //extern "C"
