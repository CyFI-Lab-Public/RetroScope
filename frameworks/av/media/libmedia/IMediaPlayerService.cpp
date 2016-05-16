/*
**
** Copyright 2008, The Android Open Source Project
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

#include <stdint.h>
#include <sys/types.h>

#include <binder/Parcel.h>
#include <binder/IMemory.h>
#include <media/ICrypto.h>
#include <media/IDrm.h>
#include <media/IHDCP.h>
#include <media/IMediaPlayerService.h>
#include <media/IMediaRecorder.h>
#include <media/IOMX.h>
#include <media/IRemoteDisplay.h>
#include <media/IRemoteDisplayClient.h>
#include <media/IStreamSource.h>

#include <utils/Errors.h>  // for status_t
#include <utils/String8.h>

namespace android {

enum {
    CREATE = IBinder::FIRST_CALL_TRANSACTION,
    DECODE_URL,
    DECODE_FD,
    CREATE_MEDIA_RECORDER,
    CREATE_METADATA_RETRIEVER,
    GET_OMX,
    MAKE_CRYPTO,
    MAKE_DRM,
    MAKE_HDCP,
    ADD_BATTERY_DATA,
    PULL_BATTERY_DATA,
    LISTEN_FOR_REMOTE_DISPLAY,
    UPDATE_PROXY_CONFIG,
};

class BpMediaPlayerService: public BpInterface<IMediaPlayerService>
{
public:
    BpMediaPlayerService(const sp<IBinder>& impl)
        : BpInterface<IMediaPlayerService>(impl)
    {
    }

    virtual sp<IMediaMetadataRetriever> createMetadataRetriever()
    {
        Parcel data, reply;
        data.writeInterfaceToken(IMediaPlayerService::getInterfaceDescriptor());
        remote()->transact(CREATE_METADATA_RETRIEVER, data, &reply);
        return interface_cast<IMediaMetadataRetriever>(reply.readStrongBinder());
    }

    virtual sp<IMediaPlayer> create(
            const sp<IMediaPlayerClient>& client, int audioSessionId) {
        Parcel data, reply;
        data.writeInterfaceToken(IMediaPlayerService::getInterfaceDescriptor());
        data.writeStrongBinder(client->asBinder());
        data.writeInt32(audioSessionId);

        remote()->transact(CREATE, data, &reply);
        return interface_cast<IMediaPlayer>(reply.readStrongBinder());
    }

    virtual sp<IMediaRecorder> createMediaRecorder()
    {
        Parcel data, reply;
        data.writeInterfaceToken(IMediaPlayerService::getInterfaceDescriptor());
        remote()->transact(CREATE_MEDIA_RECORDER, data, &reply);
        return interface_cast<IMediaRecorder>(reply.readStrongBinder());
    }

    virtual status_t decode(const char* url, uint32_t *pSampleRate, int* pNumChannels,
                               audio_format_t* pFormat,
                               const sp<IMemoryHeap>& heap, size_t *pSize)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IMediaPlayerService::getInterfaceDescriptor());
        data.writeCString(url);
        data.writeStrongBinder(heap->asBinder());
        status_t status = remote()->transact(DECODE_URL, data, &reply);
        if (status == NO_ERROR) {
            status = (status_t)reply.readInt32();
            if (status == NO_ERROR) {
                *pSampleRate = uint32_t(reply.readInt32());
                *pNumChannels = reply.readInt32();
                *pFormat = (audio_format_t)reply.readInt32();
                *pSize = (size_t)reply.readInt32();
            }
        }
        return status;
    }

    virtual status_t decode(int fd, int64_t offset, int64_t length, uint32_t *pSampleRate,
                               int* pNumChannels, audio_format_t* pFormat,
                               const sp<IMemoryHeap>& heap, size_t *pSize)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IMediaPlayerService::getInterfaceDescriptor());
        data.writeFileDescriptor(fd);
        data.writeInt64(offset);
        data.writeInt64(length);
        data.writeStrongBinder(heap->asBinder());
        status_t status = remote()->transact(DECODE_FD, data, &reply);
        if (status == NO_ERROR) {
            status = (status_t)reply.readInt32();
            if (status == NO_ERROR) {
                *pSampleRate = uint32_t(reply.readInt32());
                *pNumChannels = reply.readInt32();
                *pFormat = (audio_format_t)reply.readInt32();
                *pSize = (size_t)reply.readInt32();
            }
        }
        return status;
    }

    virtual sp<IOMX> getOMX() {
        Parcel data, reply;
        data.writeInterfaceToken(IMediaPlayerService::getInterfaceDescriptor());
        remote()->transact(GET_OMX, data, &reply);
        return interface_cast<IOMX>(reply.readStrongBinder());
    }

    virtual sp<ICrypto> makeCrypto() {
        Parcel data, reply;
        data.writeInterfaceToken(IMediaPlayerService::getInterfaceDescriptor());
        remote()->transact(MAKE_CRYPTO, data, &reply);
        return interface_cast<ICrypto>(reply.readStrongBinder());
    }

    virtual sp<IDrm> makeDrm() {
        Parcel data, reply;
        data.writeInterfaceToken(IMediaPlayerService::getInterfaceDescriptor());
        remote()->transact(MAKE_DRM, data, &reply);
        return interface_cast<IDrm>(reply.readStrongBinder());
    }

    virtual sp<IHDCP> makeHDCP(bool createEncryptionModule) {
        Parcel data, reply;
        data.writeInterfaceToken(IMediaPlayerService::getInterfaceDescriptor());
        data.writeInt32(createEncryptionModule);
        remote()->transact(MAKE_HDCP, data, &reply);
        return interface_cast<IHDCP>(reply.readStrongBinder());
    }

    virtual void addBatteryData(uint32_t params) {
        Parcel data, reply;
        data.writeInterfaceToken(IMediaPlayerService::getInterfaceDescriptor());
        data.writeInt32(params);
        remote()->transact(ADD_BATTERY_DATA, data, &reply);
    }

    virtual status_t pullBatteryData(Parcel* reply) {
        Parcel data;
        data.writeInterfaceToken(IMediaPlayerService::getInterfaceDescriptor());
        return remote()->transact(PULL_BATTERY_DATA, data, reply);
    }

    virtual sp<IRemoteDisplay> listenForRemoteDisplay(const sp<IRemoteDisplayClient>& client,
            const String8& iface)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IMediaPlayerService::getInterfaceDescriptor());
        data.writeStrongBinder(client->asBinder());
        data.writeString8(iface);
        remote()->transact(LISTEN_FOR_REMOTE_DISPLAY, data, &reply);
        return interface_cast<IRemoteDisplay>(reply.readStrongBinder());
    }

    virtual status_t updateProxyConfig(
            const char *host, int32_t port, const char *exclusionList) {
        Parcel data, reply;

        data.writeInterfaceToken(IMediaPlayerService::getInterfaceDescriptor());
        if (host == NULL) {
            data.writeInt32(0);
        } else {
            data.writeInt32(1);
            data.writeCString(host);
            data.writeInt32(port);
            data.writeCString(exclusionList);
        }

        remote()->transact(UPDATE_PROXY_CONFIG, data, &reply);

        return reply.readInt32();
    }
};

IMPLEMENT_META_INTERFACE(MediaPlayerService, "android.media.IMediaPlayerService");

// ----------------------------------------------------------------------

status_t BnMediaPlayerService::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    switch (code) {
        case CREATE: {
            CHECK_INTERFACE(IMediaPlayerService, data, reply);
            sp<IMediaPlayerClient> client =
                interface_cast<IMediaPlayerClient>(data.readStrongBinder());
            int audioSessionId = data.readInt32();
            sp<IMediaPlayer> player = create(client, audioSessionId);
            reply->writeStrongBinder(player->asBinder());
            return NO_ERROR;
        } break;
        case DECODE_URL: {
            CHECK_INTERFACE(IMediaPlayerService, data, reply);
            const char* url = data.readCString();
            sp<IMemoryHeap> heap = interface_cast<IMemoryHeap>(data.readStrongBinder());
            uint32_t sampleRate;
            int numChannels;
            audio_format_t format;
            size_t size;
            status_t status = decode(url, &sampleRate, &numChannels, &format, heap, &size);
            reply->writeInt32(status);
            if (status == NO_ERROR) {
                reply->writeInt32(sampleRate);
                reply->writeInt32(numChannels);
                reply->writeInt32((int32_t)format);
                reply->writeInt32((int32_t)size);
            }
            return NO_ERROR;
        } break;
        case DECODE_FD: {
            CHECK_INTERFACE(IMediaPlayerService, data, reply);
            int fd = dup(data.readFileDescriptor());
            int64_t offset = data.readInt64();
            int64_t length = data.readInt64();
            sp<IMemoryHeap> heap = interface_cast<IMemoryHeap>(data.readStrongBinder());
            uint32_t sampleRate;
            int numChannels;
            audio_format_t format;
            size_t size;
            status_t status = decode(fd, offset, length, &sampleRate, &numChannels, &format,
                                     heap, &size);
            reply->writeInt32(status);
            if (status == NO_ERROR) {
                reply->writeInt32(sampleRate);
                reply->writeInt32(numChannels);
                reply->writeInt32((int32_t)format);
                reply->writeInt32((int32_t)size);
            }
            return NO_ERROR;
        } break;
        case CREATE_MEDIA_RECORDER: {
            CHECK_INTERFACE(IMediaPlayerService, data, reply);
            sp<IMediaRecorder> recorder = createMediaRecorder();
            reply->writeStrongBinder(recorder->asBinder());
            return NO_ERROR;
        } break;
        case CREATE_METADATA_RETRIEVER: {
            CHECK_INTERFACE(IMediaPlayerService, data, reply);
            sp<IMediaMetadataRetriever> retriever = createMetadataRetriever();
            reply->writeStrongBinder(retriever->asBinder());
            return NO_ERROR;
        } break;
        case GET_OMX: {
            CHECK_INTERFACE(IMediaPlayerService, data, reply);
            sp<IOMX> omx = getOMX();
            reply->writeStrongBinder(omx->asBinder());
            return NO_ERROR;
        } break;
        case MAKE_CRYPTO: {
            CHECK_INTERFACE(IMediaPlayerService, data, reply);
            sp<ICrypto> crypto = makeCrypto();
            reply->writeStrongBinder(crypto->asBinder());
            return NO_ERROR;
        } break;
        case MAKE_DRM: {
            CHECK_INTERFACE(IMediaPlayerService, data, reply);
            sp<IDrm> drm = makeDrm();
            reply->writeStrongBinder(drm->asBinder());
            return NO_ERROR;
        } break;
        case MAKE_HDCP: {
            CHECK_INTERFACE(IMediaPlayerService, data, reply);
            bool createEncryptionModule = data.readInt32();
            sp<IHDCP> hdcp = makeHDCP(createEncryptionModule);
            reply->writeStrongBinder(hdcp->asBinder());
            return NO_ERROR;
        } break;
        case ADD_BATTERY_DATA: {
            CHECK_INTERFACE(IMediaPlayerService, data, reply);
            uint32_t params = data.readInt32();
            addBatteryData(params);
            return NO_ERROR;
        } break;
        case PULL_BATTERY_DATA: {
            CHECK_INTERFACE(IMediaPlayerService, data, reply);
            pullBatteryData(reply);
            return NO_ERROR;
        } break;
        case LISTEN_FOR_REMOTE_DISPLAY: {
            CHECK_INTERFACE(IMediaPlayerService, data, reply);
            sp<IRemoteDisplayClient> client(
                    interface_cast<IRemoteDisplayClient>(data.readStrongBinder()));
            String8 iface(data.readString8());
            sp<IRemoteDisplay> display(listenForRemoteDisplay(client, iface));
            reply->writeStrongBinder(display->asBinder());
            return NO_ERROR;
        } break;
        case UPDATE_PROXY_CONFIG:
        {
            CHECK_INTERFACE(IMediaPlayerService, data, reply);

            const char *host = NULL;
            int32_t port = 0;
            const char *exclusionList = NULL;

            if (data.readInt32()) {
                host = data.readCString();
                port = data.readInt32();
                exclusionList = data.readCString();
            }

            reply->writeInt32(updateProxyConfig(host, port, exclusionList));

            return OK;
        }
        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

// ----------------------------------------------------------------------------

}; // namespace android
