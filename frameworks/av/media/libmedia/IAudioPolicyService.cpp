/*
**
** Copyright 2009, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#define LOG_TAG "IAudioPolicyService"
#include <utils/Log.h>

#include <stdint.h>
#include <sys/types.h>

#include <binder/Parcel.h>

#include <media/IAudioPolicyService.h>

#include <system/audio.h>

namespace android {

enum {
    SET_DEVICE_CONNECTION_STATE = IBinder::FIRST_CALL_TRANSACTION,
    GET_DEVICE_CONNECTION_STATE,
    SET_PHONE_STATE,
    SET_RINGER_MODE,    // reserved, no longer used
    SET_FORCE_USE,
    GET_FORCE_USE,
    GET_OUTPUT,
    START_OUTPUT,
    STOP_OUTPUT,
    RELEASE_OUTPUT,
    GET_INPUT,
    START_INPUT,
    STOP_INPUT,
    RELEASE_INPUT,
    INIT_STREAM_VOLUME,
    SET_STREAM_VOLUME,
    GET_STREAM_VOLUME,
    GET_STRATEGY_FOR_STREAM,
    GET_OUTPUT_FOR_EFFECT,
    REGISTER_EFFECT,
    UNREGISTER_EFFECT,
    IS_STREAM_ACTIVE,
    IS_SOURCE_ACTIVE,
    GET_DEVICES_FOR_STREAM,
    QUERY_DEFAULT_PRE_PROCESSING,
    SET_EFFECT_ENABLED,
    IS_STREAM_ACTIVE_REMOTELY,
    IS_OFFLOAD_SUPPORTED
};

class BpAudioPolicyService : public BpInterface<IAudioPolicyService>
{
public:
    BpAudioPolicyService(const sp<IBinder>& impl)
        : BpInterface<IAudioPolicyService>(impl)
    {
    }

    virtual status_t setDeviceConnectionState(
                                    audio_devices_t device,
                                    audio_policy_dev_state_t state,
                                    const char *device_address)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(static_cast <uint32_t>(device));
        data.writeInt32(static_cast <uint32_t>(state));
        data.writeCString(device_address);
        remote()->transact(SET_DEVICE_CONNECTION_STATE, data, &reply);
        return static_cast <status_t> (reply.readInt32());
    }

    virtual audio_policy_dev_state_t getDeviceConnectionState(
                                    audio_devices_t device,
                                    const char *device_address)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(static_cast <uint32_t>(device));
        data.writeCString(device_address);
        remote()->transact(GET_DEVICE_CONNECTION_STATE, data, &reply);
        return static_cast <audio_policy_dev_state_t>(reply.readInt32());
    }

    virtual status_t setPhoneState(audio_mode_t state)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(state);
        remote()->transact(SET_PHONE_STATE, data, &reply);
        return static_cast <status_t> (reply.readInt32());
    }

    virtual status_t setForceUse(audio_policy_force_use_t usage, audio_policy_forced_cfg_t config)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(static_cast <uint32_t>(usage));
        data.writeInt32(static_cast <uint32_t>(config));
        remote()->transact(SET_FORCE_USE, data, &reply);
        return static_cast <status_t> (reply.readInt32());
    }

    virtual audio_policy_forced_cfg_t getForceUse(audio_policy_force_use_t usage)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(static_cast <uint32_t>(usage));
        remote()->transact(GET_FORCE_USE, data, &reply);
        return static_cast <audio_policy_forced_cfg_t> (reply.readInt32());
    }

    virtual audio_io_handle_t getOutput(
                                        audio_stream_type_t stream,
                                        uint32_t samplingRate,
                                        audio_format_t format,
                                        audio_channel_mask_t channelMask,
                                        audio_output_flags_t flags,
                                        const audio_offload_info_t *offloadInfo)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(static_cast <uint32_t>(stream));
        data.writeInt32(samplingRate);
        data.writeInt32(static_cast <uint32_t>(format));
        data.writeInt32(channelMask);
        data.writeInt32(static_cast <uint32_t>(flags));
        if (offloadInfo == NULL) {
            data.writeInt32(0);
        } else {
            data.writeInt32(1);
            data.write(offloadInfo, sizeof(audio_offload_info_t));
        }
        remote()->transact(GET_OUTPUT, data, &reply);
        return static_cast <audio_io_handle_t> (reply.readInt32());
    }

    virtual status_t startOutput(audio_io_handle_t output,
                                 audio_stream_type_t stream,
                                 int session)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(output);
        data.writeInt32((int32_t) stream);
        data.writeInt32(session);
        remote()->transact(START_OUTPUT, data, &reply);
        return static_cast <status_t> (reply.readInt32());
    }

    virtual status_t stopOutput(audio_io_handle_t output,
                                audio_stream_type_t stream,
                                int session)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(output);
        data.writeInt32((int32_t) stream);
        data.writeInt32(session);
        remote()->transact(STOP_OUTPUT, data, &reply);
        return static_cast <status_t> (reply.readInt32());
    }

    virtual void releaseOutput(audio_io_handle_t output)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(output);
        remote()->transact(RELEASE_OUTPUT, data, &reply);
    }

    virtual audio_io_handle_t getInput(
                                    audio_source_t inputSource,
                                    uint32_t samplingRate,
                                    audio_format_t format,
                                    audio_channel_mask_t channelMask,
                                    int audioSession)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32((int32_t) inputSource);
        data.writeInt32(samplingRate);
        data.writeInt32(static_cast <uint32_t>(format));
        data.writeInt32(channelMask);
        data.writeInt32(audioSession);
        remote()->transact(GET_INPUT, data, &reply);
        return static_cast <audio_io_handle_t> (reply.readInt32());
    }

    virtual status_t startInput(audio_io_handle_t input)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(input);
        remote()->transact(START_INPUT, data, &reply);
        return static_cast <status_t> (reply.readInt32());
    }

    virtual status_t stopInput(audio_io_handle_t input)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(input);
        remote()->transact(STOP_INPUT, data, &reply);
        return static_cast <status_t> (reply.readInt32());
    }

    virtual void releaseInput(audio_io_handle_t input)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(input);
        remote()->transact(RELEASE_INPUT, data, &reply);
    }

    virtual status_t initStreamVolume(audio_stream_type_t stream,
                                    int indexMin,
                                    int indexMax)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(static_cast <uint32_t>(stream));
        data.writeInt32(indexMin);
        data.writeInt32(indexMax);
        remote()->transact(INIT_STREAM_VOLUME, data, &reply);
        return static_cast <status_t> (reply.readInt32());
    }

    virtual status_t setStreamVolumeIndex(audio_stream_type_t stream,
                                          int index,
                                          audio_devices_t device)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(static_cast <uint32_t>(stream));
        data.writeInt32(index);
        data.writeInt32(static_cast <uint32_t>(device));
        remote()->transact(SET_STREAM_VOLUME, data, &reply);
        return static_cast <status_t> (reply.readInt32());
    }

    virtual status_t getStreamVolumeIndex(audio_stream_type_t stream,
                                          int *index,
                                          audio_devices_t device)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(static_cast <uint32_t>(stream));
        data.writeInt32(static_cast <uint32_t>(device));

        remote()->transact(GET_STREAM_VOLUME, data, &reply);
        int lIndex = reply.readInt32();
        if (index) *index = lIndex;
        return static_cast <status_t> (reply.readInt32());
    }

    virtual uint32_t getStrategyForStream(audio_stream_type_t stream)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(static_cast <uint32_t>(stream));
        remote()->transact(GET_STRATEGY_FOR_STREAM, data, &reply);
        return reply.readInt32();
    }

    virtual audio_devices_t getDevicesForStream(audio_stream_type_t stream)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(static_cast <uint32_t>(stream));
        remote()->transact(GET_DEVICES_FOR_STREAM, data, &reply);
        return (audio_devices_t) reply.readInt32();
    }

    virtual audio_io_handle_t getOutputForEffect(const effect_descriptor_t *desc)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.write(desc, sizeof(effect_descriptor_t));
        remote()->transact(GET_OUTPUT_FOR_EFFECT, data, &reply);
        return static_cast <audio_io_handle_t> (reply.readInt32());
    }

    virtual status_t registerEffect(const effect_descriptor_t *desc,
                                        audio_io_handle_t io,
                                        uint32_t strategy,
                                        int session,
                                        int id)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.write(desc, sizeof(effect_descriptor_t));
        data.writeInt32(io);
        data.writeInt32(strategy);
        data.writeInt32(session);
        data.writeInt32(id);
        remote()->transact(REGISTER_EFFECT, data, &reply);
        return static_cast <status_t> (reply.readInt32());
    }

    virtual status_t unregisterEffect(int id)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(id);
        remote()->transact(UNREGISTER_EFFECT, data, &reply);
        return static_cast <status_t> (reply.readInt32());
    }

    virtual status_t setEffectEnabled(int id, bool enabled)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(id);
        data.writeInt32(enabled);
        remote()->transact(SET_EFFECT_ENABLED, data, &reply);
        return static_cast <status_t> (reply.readInt32());
    }

    virtual bool isStreamActive(audio_stream_type_t stream, uint32_t inPastMs) const
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32((int32_t) stream);
        data.writeInt32(inPastMs);
        remote()->transact(IS_STREAM_ACTIVE, data, &reply);
        return reply.readInt32();
    }

    virtual bool isStreamActiveRemotely(audio_stream_type_t stream, uint32_t inPastMs) const
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32((int32_t) stream);
        data.writeInt32(inPastMs);
        remote()->transact(IS_STREAM_ACTIVE_REMOTELY, data, &reply);
        return reply.readInt32();
    }

    virtual bool isSourceActive(audio_source_t source) const
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32((int32_t) source);
        remote()->transact(IS_SOURCE_ACTIVE, data, &reply);
        return reply.readInt32();
    }

    virtual status_t queryDefaultPreProcessing(int audioSession,
                                               effect_descriptor_t *descriptors,
                                               uint32_t *count)
    {
        if (descriptors == NULL || count == NULL) {
            return BAD_VALUE;
        }
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.writeInt32(audioSession);
        data.writeInt32(*count);
        status_t status = remote()->transact(QUERY_DEFAULT_PRE_PROCESSING, data, &reply);
        if (status != NO_ERROR) {
            return status;
        }
        status = static_cast <status_t> (reply.readInt32());
        uint32_t retCount = reply.readInt32();
        if (retCount != 0) {
            uint32_t numDesc = (retCount < *count) ? retCount : *count;
            reply.read(descriptors, sizeof(effect_descriptor_t) * numDesc);
        }
        *count = retCount;
        return status;
    }

    virtual bool isOffloadSupported(const audio_offload_info_t& info)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IAudioPolicyService::getInterfaceDescriptor());
        data.write(&info, sizeof(audio_offload_info_t));
        remote()->transact(IS_OFFLOAD_SUPPORTED, data, &reply);
        return reply.readInt32();    }
};

IMPLEMENT_META_INTERFACE(AudioPolicyService, "android.media.IAudioPolicyService");

// ----------------------------------------------------------------------


status_t BnAudioPolicyService::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    switch (code) {
        case SET_DEVICE_CONNECTION_STATE: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_devices_t device =
                    static_cast <audio_devices_t>(data.readInt32());
            audio_policy_dev_state_t state =
                    static_cast <audio_policy_dev_state_t>(data.readInt32());
            const char *device_address = data.readCString();
            reply->writeInt32(static_cast<uint32_t> (setDeviceConnectionState(device,
                                                                              state,
                                                                              device_address)));
            return NO_ERROR;
        } break;

        case GET_DEVICE_CONNECTION_STATE: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_devices_t device =
                    static_cast<audio_devices_t> (data.readInt32());
            const char *device_address = data.readCString();
            reply->writeInt32(static_cast<uint32_t> (getDeviceConnectionState(device,
                                                                              device_address)));
            return NO_ERROR;
        } break;

        case SET_PHONE_STATE: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            reply->writeInt32(static_cast <uint32_t>(setPhoneState(
                    (audio_mode_t) data.readInt32())));
            return NO_ERROR;
        } break;

        case SET_FORCE_USE: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_policy_force_use_t usage = static_cast <audio_policy_force_use_t>(
                    data.readInt32());
            audio_policy_forced_cfg_t config =
                    static_cast <audio_policy_forced_cfg_t>(data.readInt32());
            reply->writeInt32(static_cast <uint32_t>(setForceUse(usage, config)));
            return NO_ERROR;
        } break;

        case GET_FORCE_USE: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_policy_force_use_t usage = static_cast <audio_policy_force_use_t>(
                    data.readInt32());
            reply->writeInt32(static_cast <uint32_t>(getForceUse(usage)));
            return NO_ERROR;
        } break;

        case GET_OUTPUT: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_stream_type_t stream =
                    static_cast <audio_stream_type_t>(data.readInt32());
            uint32_t samplingRate = data.readInt32();
            audio_format_t format = (audio_format_t) data.readInt32();
            audio_channel_mask_t channelMask = data.readInt32();
            audio_output_flags_t flags =
                    static_cast <audio_output_flags_t>(data.readInt32());
            bool hasOffloadInfo = data.readInt32() != 0;
            audio_offload_info_t offloadInfo;
            if (hasOffloadInfo) {
                data.read(&offloadInfo, sizeof(audio_offload_info_t));
            }
            audio_io_handle_t output = getOutput(stream,
                                                 samplingRate,
                                                 format,
                                                 channelMask,
                                                 flags,
                                                 hasOffloadInfo ? &offloadInfo : NULL);
            reply->writeInt32(static_cast <int>(output));
            return NO_ERROR;
        } break;

        case START_OUTPUT: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_io_handle_t output = static_cast <audio_io_handle_t>(data.readInt32());
            uint32_t stream = data.readInt32();
            int session = data.readInt32();
            reply->writeInt32(static_cast <uint32_t>(startOutput(output,
                                                                 (audio_stream_type_t)stream,
                                                                 session)));
            return NO_ERROR;
        } break;

        case STOP_OUTPUT: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_io_handle_t output = static_cast <audio_io_handle_t>(data.readInt32());
            uint32_t stream = data.readInt32();
            int session = data.readInt32();
            reply->writeInt32(static_cast <uint32_t>(stopOutput(output,
                                                                (audio_stream_type_t)stream,
                                                                session)));
            return NO_ERROR;
        } break;

        case RELEASE_OUTPUT: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_io_handle_t output = static_cast <audio_io_handle_t>(data.readInt32());
            releaseOutput(output);
            return NO_ERROR;
        } break;

        case GET_INPUT: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_source_t inputSource = (audio_source_t) data.readInt32();
            uint32_t samplingRate = data.readInt32();
            audio_format_t format = (audio_format_t) data.readInt32();
            audio_channel_mask_t channelMask = data.readInt32();
            int audioSession = data.readInt32();
            audio_io_handle_t input = getInput(inputSource,
                                               samplingRate,
                                               format,
                                               channelMask,
                                               audioSession);
            reply->writeInt32(static_cast <int>(input));
            return NO_ERROR;
        } break;

        case START_INPUT: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_io_handle_t input = static_cast <audio_io_handle_t>(data.readInt32());
            reply->writeInt32(static_cast <uint32_t>(startInput(input)));
            return NO_ERROR;
        } break;

        case STOP_INPUT: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_io_handle_t input = static_cast <audio_io_handle_t>(data.readInt32());
            reply->writeInt32(static_cast <uint32_t>(stopInput(input)));
            return NO_ERROR;
        } break;

        case RELEASE_INPUT: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_io_handle_t input = static_cast <audio_io_handle_t>(data.readInt32());
            releaseInput(input);
            return NO_ERROR;
        } break;

        case INIT_STREAM_VOLUME: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_stream_type_t stream =
                    static_cast <audio_stream_type_t>(data.readInt32());
            int indexMin = data.readInt32();
            int indexMax = data.readInt32();
            reply->writeInt32(static_cast <uint32_t>(initStreamVolume(stream, indexMin,indexMax)));
            return NO_ERROR;
        } break;

        case SET_STREAM_VOLUME: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_stream_type_t stream =
                    static_cast <audio_stream_type_t>(data.readInt32());
            int index = data.readInt32();
            audio_devices_t device = static_cast <audio_devices_t>(data.readInt32());
            reply->writeInt32(static_cast <uint32_t>(setStreamVolumeIndex(stream,
                                                                          index,
                                                                          device)));
            return NO_ERROR;
        } break;

        case GET_STREAM_VOLUME: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_stream_type_t stream =
                    static_cast <audio_stream_type_t>(data.readInt32());
            audio_devices_t device = static_cast <audio_devices_t>(data.readInt32());
            int index;
            status_t status = getStreamVolumeIndex(stream, &index, device);
            reply->writeInt32(index);
            reply->writeInt32(static_cast <uint32_t>(status));
            return NO_ERROR;
        } break;

        case GET_STRATEGY_FOR_STREAM: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_stream_type_t stream =
                    static_cast <audio_stream_type_t>(data.readInt32());
            reply->writeInt32(getStrategyForStream(stream));
            return NO_ERROR;
        } break;

        case GET_DEVICES_FOR_STREAM: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_stream_type_t stream =
                    static_cast <audio_stream_type_t>(data.readInt32());
            reply->writeInt32(static_cast <int>(getDevicesForStream(stream)));
            return NO_ERROR;
        } break;

        case GET_OUTPUT_FOR_EFFECT: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            effect_descriptor_t desc;
            data.read(&desc, sizeof(effect_descriptor_t));
            audio_io_handle_t output = getOutputForEffect(&desc);
            reply->writeInt32(static_cast <int>(output));
            return NO_ERROR;
        } break;

        case REGISTER_EFFECT: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            effect_descriptor_t desc;
            data.read(&desc, sizeof(effect_descriptor_t));
            audio_io_handle_t io = data.readInt32();
            uint32_t strategy = data.readInt32();
            int session = data.readInt32();
            int id = data.readInt32();
            reply->writeInt32(static_cast <int32_t>(registerEffect(&desc,
                                                                   io,
                                                                   strategy,
                                                                   session,
                                                                   id)));
            return NO_ERROR;
        } break;

        case UNREGISTER_EFFECT: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            int id = data.readInt32();
            reply->writeInt32(static_cast <int32_t>(unregisterEffect(id)));
            return NO_ERROR;
        } break;

        case SET_EFFECT_ENABLED: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            int id = data.readInt32();
            bool enabled = static_cast <bool>(data.readInt32());
            reply->writeInt32(static_cast <int32_t>(setEffectEnabled(id, enabled)));
            return NO_ERROR;
        } break;

        case IS_STREAM_ACTIVE: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_stream_type_t stream = (audio_stream_type_t) data.readInt32();
            uint32_t inPastMs = (uint32_t)data.readInt32();
            reply->writeInt32( isStreamActive((audio_stream_type_t) stream, inPastMs) );
            return NO_ERROR;
        } break;

        case IS_STREAM_ACTIVE_REMOTELY: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_stream_type_t stream = (audio_stream_type_t) data.readInt32();
            uint32_t inPastMs = (uint32_t)data.readInt32();
            reply->writeInt32( isStreamActiveRemotely((audio_stream_type_t) stream, inPastMs) );
            return NO_ERROR;
        } break;

        case IS_SOURCE_ACTIVE: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_source_t source = (audio_source_t) data.readInt32();
            reply->writeInt32( isSourceActive(source));
            return NO_ERROR;
        }

        case QUERY_DEFAULT_PRE_PROCESSING: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            int audioSession = data.readInt32();
            uint32_t count = data.readInt32();
            uint32_t retCount = count;
            effect_descriptor_t *descriptors =
                    (effect_descriptor_t *)new char[count * sizeof(effect_descriptor_t)];
            status_t status = queryDefaultPreProcessing(audioSession, descriptors, &retCount);
            reply->writeInt32(status);
            if (status != NO_ERROR && status != NO_MEMORY) {
                retCount = 0;
            }
            reply->writeInt32(retCount);
            if (retCount) {
                if (retCount < count) {
                    count = retCount;
                }
                reply->write(descriptors, sizeof(effect_descriptor_t) * count);
            }
            delete[] descriptors;
            return status;
        }

        case IS_OFFLOAD_SUPPORTED: {
            CHECK_INTERFACE(IAudioPolicyService, data, reply);
            audio_offload_info_t info;
            data.read(&info, sizeof(audio_offload_info_t));
            bool isSupported = isOffloadSupported(info);
            reply->writeInt32(isSupported);
            return NO_ERROR;
        }

        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

// ----------------------------------------------------------------------------

}; // namespace android
