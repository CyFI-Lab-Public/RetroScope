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

#include <cstdlib>
#include <pthread.h>
#include <hardware/camera3.h>
#include <sync/sync.h>
#include <system/camera_metadata.h>
#include <system/graphics.h>
#include "CameraHAL.h"
#include "Metadata.h"
#include "Stream.h"

//#define LOG_NDEBUG 0
#define LOG_TAG "Camera"
#include <cutils/log.h>

#define ATRACE_TAG (ATRACE_TAG_CAMERA | ATRACE_TAG_HAL)
#include <cutils/trace.h>
#include "ScopedTrace.h"

#include "Camera.h"

#define CAMERA_SYNC_TIMEOUT 5000 // in msecs

#define ARRAY_SIZE(a) (sizeof(a) / sizeof(a[0]))

namespace default_camera_hal {

extern "C" {
// Shim passed to the framework to close an opened device.
static int close_device(hw_device_t* dev)
{
    camera3_device_t* cam_dev = reinterpret_cast<camera3_device_t*>(dev);
    Camera* cam = static_cast<Camera*>(cam_dev->priv);
    return cam->close();
}
} // extern "C"

Camera::Camera(int id)
  : mId(id),
    mStaticInfo(NULL),
    mBusy(false),
    mCallbackOps(NULL),
    mStreams(NULL),
    mNumStreams(0),
    mSettings(NULL)
{
    pthread_mutex_init(&mMutex, NULL);
    pthread_mutex_init(&mStaticInfoMutex, NULL);

    memset(&mDevice, 0, sizeof(mDevice));
    mDevice.common.tag    = HARDWARE_DEVICE_TAG;
    mDevice.common.version = CAMERA_DEVICE_API_VERSION_3_0;
    mDevice.common.close  = close_device;
    mDevice.ops           = const_cast<camera3_device_ops_t*>(&sOps);
    mDevice.priv          = this;
}

Camera::~Camera()
{
    pthread_mutex_destroy(&mMutex);
    pthread_mutex_destroy(&mStaticInfoMutex);
}

int Camera::open(const hw_module_t *module, hw_device_t **device)
{
    ALOGI("%s:%d: Opening camera device", __func__, mId);
    CAMTRACE_CALL();
    pthread_mutex_lock(&mMutex);
    if (mBusy) {
        pthread_mutex_unlock(&mMutex);
        ALOGE("%s:%d: Error! Camera device already opened", __func__, mId);
        return -EBUSY;
    }

    // TODO: open camera dev nodes, etc
    mBusy = true;
    mDevice.common.module = const_cast<hw_module_t*>(module);
    *device = &mDevice.common;

    pthread_mutex_unlock(&mMutex);
    return 0;
}

int Camera::getInfo(struct camera_info *info)
{
    info->facing = CAMERA_FACING_FRONT;
    info->orientation = 0;
    info->device_version = mDevice.common.version;

    pthread_mutex_lock(&mStaticInfoMutex);
    if (mStaticInfo == NULL) {
        mStaticInfo = initStaticInfo();
    }
    pthread_mutex_unlock(&mStaticInfoMutex);

    info->static_camera_characteristics = mStaticInfo;

    return 0;
}

int Camera::close()
{
    ALOGI("%s:%d: Closing camera device", __func__, mId);
    CAMTRACE_CALL();
    pthread_mutex_lock(&mMutex);
    if (!mBusy) {
        pthread_mutex_unlock(&mMutex);
        ALOGE("%s:%d: Error! Camera device not open", __func__, mId);
        return -EINVAL;
    }

    // TODO: close camera dev nodes, etc
    mBusy = false;

    pthread_mutex_unlock(&mMutex);
    return 0;
}

int Camera::initialize(const camera3_callback_ops_t *callback_ops)
{
    ALOGV("%s:%d: callback_ops=%p", __func__, mId, callback_ops);
    mCallbackOps = callback_ops;
    // Create standard settings templates
    // 0 is invalid as template
    mTemplates[0] = NULL;
    // CAMERA3_TEMPLATE_PREVIEW = 1
    mTemplates[1] = new Metadata(ANDROID_CONTROL_MODE_OFF,
            ANDROID_CONTROL_CAPTURE_INTENT_PREVIEW);
    // CAMERA3_TEMPLATE_STILL_CAPTURE = 2
    mTemplates[2] = new Metadata(ANDROID_CONTROL_MODE_OFF,
            ANDROID_CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
    // CAMERA3_TEMPLATE_VIDEO_RECORD = 3
    mTemplates[3] = new Metadata(ANDROID_CONTROL_MODE_OFF,
            ANDROID_CONTROL_CAPTURE_INTENT_VIDEO_RECORD);
    // CAMERA3_TEMPLATE_VIDEO_SNAPSHOT = 4
    mTemplates[4] = new Metadata(ANDROID_CONTROL_MODE_OFF,
            ANDROID_CONTROL_CAPTURE_INTENT_VIDEO_SNAPSHOT);
    // CAMERA3_TEMPLATE_STILL_ZERO_SHUTTER_LAG = 5
    mTemplates[5] = new Metadata(ANDROID_CONTROL_MODE_OFF,
            ANDROID_CONTROL_CAPTURE_INTENT_ZERO_SHUTTER_LAG);
    // Pre-generate metadata structures
    for (int i = 1; i < CAMERA3_TEMPLATE_COUNT; i++) {
        mTemplates[i]->generate();
    }
    // TODO: create vendor templates
    return 0;
}

camera_metadata_t *Camera::initStaticInfo()
{
    /*
     * Setup static camera info.  This will have to customized per camera
     * device.
     */
    Metadata m;

    /* android.control */
    int32_t android_control_ae_available_target_fps_ranges[] = {30, 30};
    m.addInt32(ANDROID_CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES,
            ARRAY_SIZE(android_control_ae_available_target_fps_ranges),
            android_control_ae_available_target_fps_ranges);

    int32_t android_control_ae_compensation_range[] = {-4, 4};
    m.addInt32(ANDROID_CONTROL_AE_COMPENSATION_RANGE,
            ARRAY_SIZE(android_control_ae_compensation_range),
            android_control_ae_compensation_range);

    camera_metadata_rational_t android_control_ae_compensation_step[] = {{2,1}};
    m.addRational(ANDROID_CONTROL_AE_COMPENSATION_STEP,
            ARRAY_SIZE(android_control_ae_compensation_step),
            android_control_ae_compensation_step);

    int32_t android_control_max_regions[] = {1};
    m.addInt32(ANDROID_CONTROL_MAX_REGIONS,
            ARRAY_SIZE(android_control_max_regions),
            android_control_max_regions);

    /* android.jpeg */
    int32_t android_jpeg_available_thumbnail_sizes[] = {0, 0, 128, 96};
    m.addInt32(ANDROID_JPEG_AVAILABLE_THUMBNAIL_SIZES,
            ARRAY_SIZE(android_jpeg_available_thumbnail_sizes),
            android_jpeg_available_thumbnail_sizes);

    /* android.lens */
    float android_lens_info_available_focal_lengths[] = {1.0};
    m.addFloat(ANDROID_LENS_INFO_AVAILABLE_FOCAL_LENGTHS,
            ARRAY_SIZE(android_lens_info_available_focal_lengths),
            android_lens_info_available_focal_lengths);

    /* android.request */
    int32_t android_request_max_num_output_streams[] = {0, 3, 1};
    m.addInt32(ANDROID_REQUEST_MAX_NUM_OUTPUT_STREAMS,
            ARRAY_SIZE(android_request_max_num_output_streams),
            android_request_max_num_output_streams);

    /* android.scaler */
    int32_t android_scaler_available_formats[] = {
            HAL_PIXEL_FORMAT_RAW_SENSOR,
            HAL_PIXEL_FORMAT_BLOB,
            HAL_PIXEL_FORMAT_RGBA_8888,
            HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED,
            // These are handled by YCbCr_420_888
            //        HAL_PIXEL_FORMAT_YV12,
            //        HAL_PIXEL_FORMAT_YCrCb_420_SP,
            HAL_PIXEL_FORMAT_YCbCr_420_888};
    m.addInt32(ANDROID_SCALER_AVAILABLE_FORMATS,
            ARRAY_SIZE(android_scaler_available_formats),
            android_scaler_available_formats);

    int64_t android_scaler_available_jpeg_min_durations[] = {1};
    m.addInt64(ANDROID_SCALER_AVAILABLE_JPEG_MIN_DURATIONS,
            ARRAY_SIZE(android_scaler_available_jpeg_min_durations),
            android_scaler_available_jpeg_min_durations);

    int32_t android_scaler_available_jpeg_sizes[] = {640, 480};
    m.addInt32(ANDROID_SCALER_AVAILABLE_JPEG_SIZES,
            ARRAY_SIZE(android_scaler_available_jpeg_sizes),
            android_scaler_available_jpeg_sizes);

    float android_scaler_available_max_digital_zoom[] = {1};
    m.addFloat(ANDROID_SCALER_AVAILABLE_MAX_DIGITAL_ZOOM,
            ARRAY_SIZE(android_scaler_available_max_digital_zoom),
            android_scaler_available_max_digital_zoom);

    int64_t android_scaler_available_processed_min_durations[] = {1};
    m.addInt64(ANDROID_SCALER_AVAILABLE_PROCESSED_MIN_DURATIONS,
            ARRAY_SIZE(android_scaler_available_processed_min_durations),
            android_scaler_available_processed_min_durations);

    int32_t android_scaler_available_processed_sizes[] = {640, 480};
    m.addInt32(ANDROID_SCALER_AVAILABLE_PROCESSED_SIZES,
            ARRAY_SIZE(android_scaler_available_processed_sizes),
            android_scaler_available_processed_sizes);

    int64_t android_scaler_available_raw_min_durations[] = {1};
    m.addInt64(ANDROID_SCALER_AVAILABLE_RAW_MIN_DURATIONS,
            ARRAY_SIZE(android_scaler_available_raw_min_durations),
            android_scaler_available_raw_min_durations);

    int32_t android_scaler_available_raw_sizes[] = {640, 480};
    m.addInt32(ANDROID_SCALER_AVAILABLE_RAW_SIZES,
            ARRAY_SIZE(android_scaler_available_raw_sizes),
            android_scaler_available_raw_sizes);

    /* android.sensor */

    int32_t android_sensor_info_active_array_size[] = {0, 0, 640, 480};
    m.addInt32(ANDROID_SENSOR_INFO_ACTIVE_ARRAY_SIZE,
            ARRAY_SIZE(android_sensor_info_active_array_size),
            android_sensor_info_active_array_size);

    int32_t android_sensor_info_sensitivity_range[] =
            {100, 1600};
    m.addInt32(ANDROID_SENSOR_INFO_SENSITIVITY_RANGE,
            ARRAY_SIZE(android_sensor_info_sensitivity_range),
            android_sensor_info_sensitivity_range);

    int64_t android_sensor_info_max_frame_duration[] = {30000000000};
    m.addInt64(ANDROID_SENSOR_INFO_MAX_FRAME_DURATION,
            ARRAY_SIZE(android_sensor_info_max_frame_duration),
            android_sensor_info_max_frame_duration);

    float android_sensor_info_physical_size[] = {3.2, 2.4};
    m.addFloat(ANDROID_SENSOR_INFO_PHYSICAL_SIZE,
            ARRAY_SIZE(android_sensor_info_physical_size),
            android_sensor_info_physical_size);

    int32_t android_sensor_info_pixel_array_size[] = {640, 480};
    m.addInt32(ANDROID_SENSOR_INFO_PIXEL_ARRAY_SIZE,
            ARRAY_SIZE(android_sensor_info_pixel_array_size),
            android_sensor_info_pixel_array_size);

    int32_t android_sensor_orientation[] = {0};
    m.addInt32(ANDROID_SENSOR_ORIENTATION,
            ARRAY_SIZE(android_sensor_orientation),
            android_sensor_orientation);

    /* End of static camera characteristics */

    return clone_camera_metadata(m.generate());
}

int Camera::configureStreams(camera3_stream_configuration_t *stream_config)
{
    camera3_stream_t *astream;
    Stream **newStreams = NULL;

    CAMTRACE_CALL();
    ALOGV("%s:%d: stream_config=%p", __func__, mId, stream_config);

    if (stream_config == NULL) {
        ALOGE("%s:%d: NULL stream configuration array", __func__, mId);
        return -EINVAL;
    }
    if (stream_config->num_streams == 0) {
        ALOGE("%s:%d: Empty stream configuration array", __func__, mId);
        return -EINVAL;
    }

    // Create new stream array
    newStreams = new Stream*[stream_config->num_streams];
    ALOGV("%s:%d: Number of Streams: %d", __func__, mId,
            stream_config->num_streams);

    pthread_mutex_lock(&mMutex);

    // Mark all current streams unused for now
    for (int i = 0; i < mNumStreams; i++)
        mStreams[i]->mReuse = false;
    // Fill new stream array with reused streams and new streams
    for (unsigned int i = 0; i < stream_config->num_streams; i++) {
        astream = stream_config->streams[i];
        if (astream->max_buffers > 0) {
            ALOGV("%s:%d: Reusing stream %d", __func__, mId, i);
            newStreams[i] = reuseStream(astream);
        } else {
            ALOGV("%s:%d: Creating new stream %d", __func__, mId, i);
            newStreams[i] = new Stream(mId, astream);
        }

        if (newStreams[i] == NULL) {
            ALOGE("%s:%d: Error processing stream %d", __func__, mId, i);
            goto err_out;
        }
        astream->priv = newStreams[i];
    }

    // Verify the set of streams in aggregate
    if (!isValidStreamSet(newStreams, stream_config->num_streams)) {
        ALOGE("%s:%d: Invalid stream set", __func__, mId);
        goto err_out;
    }

    // Set up all streams (calculate usage/max_buffers for each)
    setupStreams(newStreams, stream_config->num_streams);

    // Destroy all old streams and replace stream array with new one
    destroyStreams(mStreams, mNumStreams);
    mStreams = newStreams;
    mNumStreams = stream_config->num_streams;

    // Clear out last seen settings metadata
    setSettings(NULL);

    pthread_mutex_unlock(&mMutex);
    return 0;

err_out:
    // Clean up temporary streams, preserve existing mStreams/mNumStreams
    destroyStreams(newStreams, stream_config->num_streams);
    pthread_mutex_unlock(&mMutex);
    return -EINVAL;
}

void Camera::destroyStreams(Stream **streams, int count)
{
    if (streams == NULL)
        return;
    for (int i = 0; i < count; i++) {
        // Only destroy streams that weren't reused
        if (streams[i] != NULL && !streams[i]->mReuse)
            delete streams[i];
    }
    delete [] streams;
}

Stream *Camera::reuseStream(camera3_stream_t *astream)
{
    Stream *priv = reinterpret_cast<Stream*>(astream->priv);
    // Verify the re-used stream's parameters match
    if (!priv->isValidReuseStream(mId, astream)) {
        ALOGE("%s:%d: Mismatched parameter in reused stream", __func__, mId);
        return NULL;
    }
    // Mark stream to be reused
    priv->mReuse = true;
    return priv;
}

bool Camera::isValidStreamSet(Stream **streams, int count)
{
    int inputs = 0;
    int outputs = 0;

    if (streams == NULL) {
        ALOGE("%s:%d: NULL stream configuration streams", __func__, mId);
        return false;
    }
    if (count == 0) {
        ALOGE("%s:%d: Zero count stream configuration streams", __func__, mId);
        return false;
    }
    // Validate there is at most one input stream and at least one output stream
    for (int i = 0; i < count; i++) {
        // A stream may be both input and output (bidirectional)
        if (streams[i]->isInputType())
            inputs++;
        if (streams[i]->isOutputType())
            outputs++;
    }
    ALOGV("%s:%d: Configuring %d output streams and %d input streams",
            __func__, mId, outputs, inputs);
    if (outputs < 1) {
        ALOGE("%s:%d: Stream config must have >= 1 output", __func__, mId);
        return false;
    }
    if (inputs > 1) {
        ALOGE("%s:%d: Stream config must have <= 1 input", __func__, mId);
        return false;
    }
    // TODO: check for correct number of Bayer/YUV/JPEG/Encoder streams
    return true;
}

void Camera::setupStreams(Stream **streams, int count)
{
    /*
     * This is where the HAL has to decide internally how to handle all of the
     * streams, and then produce usage and max_buffer values for each stream.
     * Note, the stream array has been checked before this point for ALL invalid
     * conditions, so it must find a successful configuration for this stream
     * array.  The HAL may not return an error from this point.
     *
     * In this demo HAL, we just set all streams to be the same dummy values;
     * real implementations will want to avoid USAGE_SW_{READ|WRITE}_OFTEN.
     */
    for (int i = 0; i < count; i++) {
        uint32_t usage = 0;

        if (streams[i]->isOutputType())
            usage |= GRALLOC_USAGE_SW_WRITE_OFTEN |
                     GRALLOC_USAGE_HW_CAMERA_WRITE;
        if (streams[i]->isInputType())
            usage |= GRALLOC_USAGE_SW_READ_OFTEN |
                     GRALLOC_USAGE_HW_CAMERA_READ;

        streams[i]->setUsage(usage);
        streams[i]->setMaxBuffers(1);
    }
}

int Camera::registerStreamBuffers(const camera3_stream_buffer_set_t *buf_set)
{
    ALOGV("%s:%d: buffer_set=%p", __func__, mId, buf_set);
    if (buf_set == NULL) {
        ALOGE("%s:%d: NULL buffer set", __func__, mId);
        return -EINVAL;
    }
    if (buf_set->stream == NULL) {
        ALOGE("%s:%d: NULL stream handle", __func__, mId);
        return -EINVAL;
    }
    Stream *stream = reinterpret_cast<Stream*>(buf_set->stream->priv);
    return stream->registerBuffers(buf_set);
}

const camera_metadata_t* Camera::constructDefaultRequestSettings(int type)
{
    ALOGV("%s:%d: type=%d", __func__, mId, type);

    if (type < 1 || type >= CAMERA3_TEMPLATE_COUNT) {
        ALOGE("%s:%d: Invalid template request type: %d", __func__, mId, type);
        return NULL;
    }
    return mTemplates[type]->generate();
}

int Camera::processCaptureRequest(camera3_capture_request_t *request)
{
    camera3_capture_result result;

    ALOGV("%s:%d: request=%p", __func__, mId, request);
    CAMTRACE_CALL();

    if (request == NULL) {
        ALOGE("%s:%d: NULL request recieved", __func__, mId);
        return -EINVAL;
    }

    ALOGV("%s:%d: Request Frame:%d Settings:%p", __func__, mId,
            request->frame_number, request->settings);

    // NULL indicates use last settings
    if (request->settings == NULL) {
        if (mSettings == NULL) {
            ALOGE("%s:%d: NULL settings without previous set Frame:%d Req:%p",
                    __func__, mId, request->frame_number, request);
            return -EINVAL;
        }
    } else {
        setSettings(request->settings);
    }

    if (request->input_buffer != NULL) {
        ALOGV("%s:%d: Reprocessing input buffer %p", __func__, mId,
                request->input_buffer);

        if (!isValidReprocessSettings(request->settings)) {
            ALOGE("%s:%d: Invalid settings for reprocess request: %p",
                    __func__, mId, request->settings);
            return -EINVAL;
        }
    } else {
        ALOGV("%s:%d: Capturing new frame.", __func__, mId);

        if (!isValidCaptureSettings(request->settings)) {
            ALOGE("%s:%d: Invalid settings for capture request: %p",
                    __func__, mId, request->settings);
            return -EINVAL;
        }
    }

    if (request->num_output_buffers <= 0) {
        ALOGE("%s:%d: Invalid number of output buffers: %d", __func__, mId,
                request->num_output_buffers);
        return -EINVAL;
    }
    result.num_output_buffers = request->num_output_buffers;
    result.output_buffers = new camera3_stream_buffer_t[result.num_output_buffers];
    for (unsigned int i = 0; i < request->num_output_buffers; i++) {
        int res = processCaptureBuffer(&request->output_buffers[i],
                const_cast<camera3_stream_buffer_t*>(&result.output_buffers[i]));
        if (res)
            goto err_out;
    }

    result.frame_number = request->frame_number;
    // TODO: return actual captured/reprocessed settings
    result.result = request->settings;
    // TODO: asynchronously return results
    notifyShutter(request->frame_number, 0);
    mCallbackOps->process_capture_result(mCallbackOps, &result);

    return 0;

err_out:
    delete [] result.output_buffers;
    // TODO: this should probably be a total device failure; transient for now
    return -EINVAL;
}

void Camera::setSettings(const camera_metadata_t *new_settings)
{
    if (mSettings != NULL) {
        free_camera_metadata(mSettings);
        mSettings = NULL;
    }

    if (new_settings != NULL)
        mSettings = clone_camera_metadata(new_settings);
}

bool Camera::isValidCaptureSettings(const camera_metadata_t* /*settings*/)
{
    // TODO: reject settings that cannot be captured
    return true;
}

bool Camera::isValidReprocessSettings(const camera_metadata_t* /*settings*/)
{
    // TODO: reject settings that cannot be reprocessed
    // input buffers unimplemented, use this to reject reprocessing requests
    ALOGE("%s:%d: Input buffer reprocessing not implemented", __func__, mId);
    return false;
}

int Camera::processCaptureBuffer(const camera3_stream_buffer_t *in,
        camera3_stream_buffer_t *out)
{
    if (in->acquire_fence != -1) {
        int res = sync_wait(in->acquire_fence, CAMERA_SYNC_TIMEOUT);
        if (res == -ETIME) {
            ALOGE("%s:%d: Timeout waiting on buffer acquire fence",
                    __func__, mId);
            return res;
        } else if (res) {
            ALOGE("%s:%d: Error waiting on buffer acquire fence: %s(%d)",
                    __func__, mId, strerror(-res), res);
            return res;
        }
    }

    out->stream = in->stream;
    out->buffer = in->buffer;
    out->status = CAMERA3_BUFFER_STATUS_OK;
    // TODO: use driver-backed release fences
    out->acquire_fence = -1;
    out->release_fence = -1;

    // TODO: lock and software-paint buffer
    return 0;
}

void Camera::notifyShutter(uint32_t frame_number, uint64_t timestamp)
{
    int res;
    struct timespec ts;

    // If timestamp is 0, get timestamp from right now instead
    if (timestamp == 0) {
        ALOGW("%s:%d: No timestamp provided, using CLOCK_BOOTTIME",
                __func__, mId);
        res = clock_gettime(CLOCK_BOOTTIME, &ts);
        if (res == 0) {
            timestamp = ts.tv_sec * 1000000000ULL + ts.tv_nsec;
        } else {
            ALOGE("%s:%d: No timestamp and failed to get CLOCK_BOOTTIME %s(%d)",
                    __func__, mId, strerror(errno), errno);
        }
    }
    camera3_notify_msg_t m;
    memset(&m, 0, sizeof(m));
    m.type = CAMERA3_MSG_SHUTTER;
    m.message.shutter.frame_number = frame_number;
    m.message.shutter.timestamp = timestamp;
    mCallbackOps->notify(mCallbackOps, &m);
}

void Camera::getMetadataVendorTagOps(vendor_tag_query_ops_t *ops)
{
    ALOGV("%s:%d: ops=%p", __func__, mId, ops);
    // TODO: return vendor tag ops
}

void Camera::dump(int fd)
{
    ALOGV("%s:%d: Dumping to fd %d", __func__, mId, fd);
    // TODO: dprintf all relevant state to fd
}

extern "C" {
// Get handle to camera from device priv data
static Camera *camdev_to_camera(const camera3_device_t *dev)
{
    return reinterpret_cast<Camera*>(dev->priv);
}

static int initialize(const camera3_device_t *dev,
        const camera3_callback_ops_t *callback_ops)
{
    return camdev_to_camera(dev)->initialize(callback_ops);
}

static int configure_streams(const camera3_device_t *dev,
        camera3_stream_configuration_t *stream_list)
{
    return camdev_to_camera(dev)->configureStreams(stream_list);
}

static int register_stream_buffers(const camera3_device_t *dev,
        const camera3_stream_buffer_set_t *buffer_set)
{
    return camdev_to_camera(dev)->registerStreamBuffers(buffer_set);
}

static const camera_metadata_t *construct_default_request_settings(
        const camera3_device_t *dev, int type)
{
    return camdev_to_camera(dev)->constructDefaultRequestSettings(type);
}

static int process_capture_request(const camera3_device_t *dev,
        camera3_capture_request_t *request)
{
    return camdev_to_camera(dev)->processCaptureRequest(request);
}

static void get_metadata_vendor_tag_ops(const camera3_device_t *dev,
        vendor_tag_query_ops_t *ops)
{
    camdev_to_camera(dev)->getMetadataVendorTagOps(ops);
}

static void dump(const camera3_device_t *dev, int fd)
{
    camdev_to_camera(dev)->dump(fd);
}
} // extern "C"

const camera3_device_ops_t Camera::sOps = {
    .initialize              = default_camera_hal::initialize,
    .configure_streams       = default_camera_hal::configure_streams,
    .register_stream_buffers = default_camera_hal::register_stream_buffers,
    .construct_default_request_settings =
            default_camera_hal::construct_default_request_settings,
    .process_capture_request = default_camera_hal::process_capture_request,
    .get_metadata_vendor_tag_ops =
            default_camera_hal::get_metadata_vendor_tag_ops,
    .dump                    = default_camera_hal::dump
};

} // namespace default_camera_hal
