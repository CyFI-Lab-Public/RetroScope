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

// Proxy for media player implementations

//#define LOG_NDEBUG 0
#define LOG_TAG "MediaPlayerService"
#include <utils/Log.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <dirent.h>
#include <unistd.h>

#include <string.h>

#include <cutils/atomic.h>
#include <cutils/properties.h> // for property_get

#include <utils/misc.h>

#include <binder/IPCThreadState.h>
#include <binder/IServiceManager.h>
#include <binder/MemoryHeapBase.h>
#include <binder/MemoryBase.h>
#include <gui/Surface.h>
#include <utils/Errors.h>  // for status_t
#include <utils/String8.h>
#include <utils/SystemClock.h>
#include <utils/Vector.h>

#include <media/IRemoteDisplay.h>
#include <media/IRemoteDisplayClient.h>
#include <media/MediaPlayerInterface.h>
#include <media/mediarecorder.h>
#include <media/MediaMetadataRetrieverInterface.h>
#include <media/Metadata.h>
#include <media/AudioTrack.h>
#include <media/MemoryLeakTrackUtil.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/AudioPlayer.h>
#include <media/stagefright/foundation/ADebug.h>

#include <system/audio.h>

#include <private/android_filesystem_config.h>

#include "ActivityManager.h"
#include "MediaRecorderClient.h"
#include "MediaPlayerService.h"
#include "MetadataRetrieverClient.h"
#include "MediaPlayerFactory.h"

#include "MidiFile.h"
#include "TestPlayerStub.h"
#include "StagefrightPlayer.h"
#include "nuplayer/NuPlayerDriver.h"

#include <OMX.h>

#include "Crypto.h"
#include "Drm.h"
#include "HDCP.h"
#include "HTTPBase.h"
#include "RemoteDisplay.h"

namespace {
using android::media::Metadata;
using android::status_t;
using android::OK;
using android::BAD_VALUE;
using android::NOT_ENOUGH_DATA;
using android::Parcel;

// Max number of entries in the filter.
const int kMaxFilterSize = 64;  // I pulled that out of thin air.

// FIXME: Move all the metadata related function in the Metadata.cpp


// Unmarshall a filter from a Parcel.
// Filter format in a parcel:
//
//  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
// +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
// |                       number of entries (n)                   |
// +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
// |                       metadata type 1                         |
// +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
// |                       metadata type 2                         |
// +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//  ....
// +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
// |                       metadata type n                         |
// +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//
// @param p Parcel that should start with a filter.
// @param[out] filter On exit contains the list of metadata type to be
//                    filtered.
// @param[out] status On exit contains the status code to be returned.
// @return true if the parcel starts with a valid filter.
bool unmarshallFilter(const Parcel& p,
                      Metadata::Filter *filter,
                      status_t *status)
{
    int32_t val;
    if (p.readInt32(&val) != OK)
    {
        ALOGE("Failed to read filter's length");
        *status = NOT_ENOUGH_DATA;
        return false;
    }

    if( val > kMaxFilterSize || val < 0)
    {
        ALOGE("Invalid filter len %d", val);
        *status = BAD_VALUE;
        return false;
    }

    const size_t num = val;

    filter->clear();
    filter->setCapacity(num);

    size_t size = num * sizeof(Metadata::Type);


    if (p.dataAvail() < size)
    {
        ALOGE("Filter too short expected %d but got %d", size, p.dataAvail());
        *status = NOT_ENOUGH_DATA;
        return false;
    }

    const Metadata::Type *data =
            static_cast<const Metadata::Type*>(p.readInplace(size));

    if (NULL == data)
    {
        ALOGE("Filter had no data");
        *status = BAD_VALUE;
        return false;
    }

    // TODO: The stl impl of vector would be more efficient here
    // because it degenerates into a memcpy on pod types. Try to
    // replace later or use stl::set.
    for (size_t i = 0; i < num; ++i)
    {
        filter->add(*data);
        ++data;
    }
    *status = OK;
    return true;
}

// @param filter Of metadata type.
// @param val To be searched.
// @return true if a match was found.
bool findMetadata(const Metadata::Filter& filter, const int32_t val)
{
    // Deal with empty and ANY right away
    if (filter.isEmpty()) return false;
    if (filter[0] == Metadata::kAny) return true;

    return filter.indexOf(val) >= 0;
}

}  // anonymous namespace


namespace android {

static bool checkPermission(const char* permissionString) {
#ifndef HAVE_ANDROID_OS
    return true;
#endif
    if (getpid() == IPCThreadState::self()->getCallingPid()) return true;
    bool ok = checkCallingPermission(String16(permissionString));
    if (!ok) ALOGE("Request requires %s", permissionString);
    return ok;
}

// TODO: Find real cause of Audio/Video delay in PV framework and remove this workaround
/* static */ int MediaPlayerService::AudioOutput::mMinBufferCount = 4;
/* static */ bool MediaPlayerService::AudioOutput::mIsOnEmulator = false;

void MediaPlayerService::instantiate() {
    defaultServiceManager()->addService(
            String16("media.player"), new MediaPlayerService());
}

MediaPlayerService::MediaPlayerService()
{
    ALOGV("MediaPlayerService created");
    mNextConnId = 1;

    mBatteryAudio.refCount = 0;
    for (int i = 0; i < NUM_AUDIO_DEVICES; i++) {
        mBatteryAudio.deviceOn[i] = 0;
        mBatteryAudio.lastTime[i] = 0;
        mBatteryAudio.totalTime[i] = 0;
    }
    // speaker is on by default
    mBatteryAudio.deviceOn[SPEAKER] = 1;

    MediaPlayerFactory::registerBuiltinFactories();
}

MediaPlayerService::~MediaPlayerService()
{
    ALOGV("MediaPlayerService destroyed");
}

sp<IMediaRecorder> MediaPlayerService::createMediaRecorder()
{
    pid_t pid = IPCThreadState::self()->getCallingPid();
    sp<MediaRecorderClient> recorder = new MediaRecorderClient(this, pid);
    wp<MediaRecorderClient> w = recorder;
    Mutex::Autolock lock(mLock);
    mMediaRecorderClients.add(w);
    ALOGV("Create new media recorder client from pid %d", pid);
    return recorder;
}

void MediaPlayerService::removeMediaRecorderClient(wp<MediaRecorderClient> client)
{
    Mutex::Autolock lock(mLock);
    mMediaRecorderClients.remove(client);
    ALOGV("Delete media recorder client");
}

sp<IMediaMetadataRetriever> MediaPlayerService::createMetadataRetriever()
{
    pid_t pid = IPCThreadState::self()->getCallingPid();
    sp<MetadataRetrieverClient> retriever = new MetadataRetrieverClient(pid);
    ALOGV("Create new media retriever from pid %d", pid);
    return retriever;
}

sp<IMediaPlayer> MediaPlayerService::create(const sp<IMediaPlayerClient>& client,
        int audioSessionId)
{
    pid_t pid = IPCThreadState::self()->getCallingPid();
    int32_t connId = android_atomic_inc(&mNextConnId);

    sp<Client> c = new Client(
            this, pid, connId, client, audioSessionId,
            IPCThreadState::self()->getCallingUid());

    ALOGV("Create new client(%d) from pid %d, uid %d, ", connId, pid,
         IPCThreadState::self()->getCallingUid());

    wp<Client> w = c;
    {
        Mutex::Autolock lock(mLock);
        mClients.add(w);
    }
    return c;
}

sp<IOMX> MediaPlayerService::getOMX() {
    Mutex::Autolock autoLock(mLock);

    if (mOMX.get() == NULL) {
        mOMX = new OMX;
    }

    return mOMX;
}

sp<ICrypto> MediaPlayerService::makeCrypto() {
    return new Crypto;
}

sp<IDrm> MediaPlayerService::makeDrm() {
    return new Drm;
}

sp<IHDCP> MediaPlayerService::makeHDCP(bool createEncryptionModule) {
    return new HDCP(createEncryptionModule);
}

sp<IRemoteDisplay> MediaPlayerService::listenForRemoteDisplay(
        const sp<IRemoteDisplayClient>& client, const String8& iface) {
    if (!checkPermission("android.permission.CONTROL_WIFI_DISPLAY")) {
        return NULL;
    }

    return new RemoteDisplay(client, iface.string());
}

status_t MediaPlayerService::updateProxyConfig(
        const char *host, int32_t port, const char *exclusionList) {
    return HTTPBase::UpdateProxyConfig(host, port, exclusionList);
}

status_t MediaPlayerService::AudioCache::dump(int fd, const Vector<String16>& args) const
{
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;

    result.append(" AudioCache\n");
    if (mHeap != 0) {
        snprintf(buffer, 255, "  heap base(%p), size(%d), flags(%d)\n",
                mHeap->getBase(), mHeap->getSize(), mHeap->getFlags());
        result.append(buffer);
    }
    snprintf(buffer, 255, "  msec per frame(%f), channel count(%d), format(%d), frame count(%zd)\n",
            mMsecsPerFrame, mChannelCount, mFormat, mFrameCount);
    result.append(buffer);
    snprintf(buffer, 255, "  sample rate(%d), size(%d), error(%d), command complete(%s)\n",
            mSampleRate, mSize, mError, mCommandComplete?"true":"false");
    result.append(buffer);
    ::write(fd, result.string(), result.size());
    return NO_ERROR;
}

status_t MediaPlayerService::AudioOutput::dump(int fd, const Vector<String16>& args) const
{
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;

    result.append(" AudioOutput\n");
    snprintf(buffer, 255, "  stream type(%d), left - right volume(%f, %f)\n",
            mStreamType, mLeftVolume, mRightVolume);
    result.append(buffer);
    snprintf(buffer, 255, "  msec per frame(%f), latency (%d)\n",
            mMsecsPerFrame, (mTrack != 0) ? mTrack->latency() : -1);
    result.append(buffer);
    snprintf(buffer, 255, "  aux effect id(%d), send level (%f)\n",
            mAuxEffectId, mSendLevel);
    result.append(buffer);

    ::write(fd, result.string(), result.size());
    if (mTrack != 0) {
        mTrack->dump(fd, args);
    }
    return NO_ERROR;
}

status_t MediaPlayerService::Client::dump(int fd, const Vector<String16>& args) const
{
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;
    result.append(" Client\n");
    snprintf(buffer, 255, "  pid(%d), connId(%d), status(%d), looping(%s)\n",
            mPid, mConnId, mStatus, mLoop?"true": "false");
    result.append(buffer);
    write(fd, result.string(), result.size());
    if (mPlayer != NULL) {
        mPlayer->dump(fd, args);
    }
    if (mAudioOutput != 0) {
        mAudioOutput->dump(fd, args);
    }
    write(fd, "\n", 1);
    return NO_ERROR;
}

status_t MediaPlayerService::dump(int fd, const Vector<String16>& args)
{
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;
    if (checkCallingPermission(String16("android.permission.DUMP")) == false) {
        snprintf(buffer, SIZE, "Permission Denial: "
                "can't dump MediaPlayerService from pid=%d, uid=%d\n",
                IPCThreadState::self()->getCallingPid(),
                IPCThreadState::self()->getCallingUid());
        result.append(buffer);
    } else {
        Mutex::Autolock lock(mLock);
        for (int i = 0, n = mClients.size(); i < n; ++i) {
            sp<Client> c = mClients[i].promote();
            if (c != 0) c->dump(fd, args);
        }
        if (mMediaRecorderClients.size() == 0) {
                result.append(" No media recorder client\n\n");
        } else {
            for (int i = 0, n = mMediaRecorderClients.size(); i < n; ++i) {
                sp<MediaRecorderClient> c = mMediaRecorderClients[i].promote();
                if (c != 0) {
                    snprintf(buffer, 255, " MediaRecorderClient pid(%d)\n", c->mPid);
                    result.append(buffer);
                    write(fd, result.string(), result.size());
                    result = "\n";
                    c->dump(fd, args);
                }
            }
        }

        result.append(" Files opened and/or mapped:\n");
        snprintf(buffer, SIZE, "/proc/%d/maps", gettid());
        FILE *f = fopen(buffer, "r");
        if (f) {
            while (!feof(f)) {
                fgets(buffer, SIZE, f);
                if (strstr(buffer, " /storage/") ||
                    strstr(buffer, " /system/sounds/") ||
                    strstr(buffer, " /data/") ||
                    strstr(buffer, " /system/media/")) {
                    result.append("  ");
                    result.append(buffer);
                }
            }
            fclose(f);
        } else {
            result.append("couldn't open ");
            result.append(buffer);
            result.append("\n");
        }

        snprintf(buffer, SIZE, "/proc/%d/fd", gettid());
        DIR *d = opendir(buffer);
        if (d) {
            struct dirent *ent;
            while((ent = readdir(d)) != NULL) {
                if (strcmp(ent->d_name,".") && strcmp(ent->d_name,"..")) {
                    snprintf(buffer, SIZE, "/proc/%d/fd/%s", gettid(), ent->d_name);
                    struct stat s;
                    if (lstat(buffer, &s) == 0) {
                        if ((s.st_mode & S_IFMT) == S_IFLNK) {
                            char linkto[256];
                            int len = readlink(buffer, linkto, sizeof(linkto));
                            if(len > 0) {
                                if(len > 255) {
                                    linkto[252] = '.';
                                    linkto[253] = '.';
                                    linkto[254] = '.';
                                    linkto[255] = 0;
                                } else {
                                    linkto[len] = 0;
                                }
                                if (strstr(linkto, "/storage/") == linkto ||
                                    strstr(linkto, "/system/sounds/") == linkto ||
                                    strstr(linkto, "/data/") == linkto ||
                                    strstr(linkto, "/system/media/") == linkto) {
                                    result.append("  ");
                                    result.append(buffer);
                                    result.append(" -> ");
                                    result.append(linkto);
                                    result.append("\n");
                                }
                            }
                        } else {
                            result.append("  unexpected type for ");
                            result.append(buffer);
                            result.append("\n");
                        }
                    }
                }
            }
            closedir(d);
        } else {
            result.append("couldn't open ");
            result.append(buffer);
            result.append("\n");
        }

        bool dumpMem = false;
        for (size_t i = 0; i < args.size(); i++) {
            if (args[i] == String16("-m")) {
                dumpMem = true;
            }
        }
        if (dumpMem) {
            dumpMemoryAddresses(fd);
        }
    }
    write(fd, result.string(), result.size());
    return NO_ERROR;
}

void MediaPlayerService::removeClient(wp<Client> client)
{
    Mutex::Autolock lock(mLock);
    mClients.remove(client);
}

MediaPlayerService::Client::Client(
        const sp<MediaPlayerService>& service, pid_t pid,
        int32_t connId, const sp<IMediaPlayerClient>& client,
        int audioSessionId, uid_t uid)
{
    ALOGV("Client(%d) constructor", connId);
    mPid = pid;
    mConnId = connId;
    mService = service;
    mClient = client;
    mLoop = false;
    mStatus = NO_INIT;
    mAudioSessionId = audioSessionId;
    mUID = uid;
    mRetransmitEndpointValid = false;

#if CALLBACK_ANTAGONIZER
    ALOGD("create Antagonizer");
    mAntagonizer = new Antagonizer(notify, this);
#endif
}

MediaPlayerService::Client::~Client()
{
    ALOGV("Client(%d) destructor pid = %d", mConnId, mPid);
    mAudioOutput.clear();
    wp<Client> client(this);
    disconnect();
    mService->removeClient(client);
}

void MediaPlayerService::Client::disconnect()
{
    ALOGV("disconnect(%d) from pid %d", mConnId, mPid);
    // grab local reference and clear main reference to prevent future
    // access to object
    sp<MediaPlayerBase> p;
    {
        Mutex::Autolock l(mLock);
        p = mPlayer;
        mClient.clear();
    }

    mPlayer.clear();

    // clear the notification to prevent callbacks to dead client
    // and reset the player. We assume the player will serialize
    // access to itself if necessary.
    if (p != 0) {
        p->setNotifyCallback(0, 0);
#if CALLBACK_ANTAGONIZER
        ALOGD("kill Antagonizer");
        mAntagonizer->kill();
#endif
        p->reset();
    }

    disconnectNativeWindow();

    IPCThreadState::self()->flushCommands();
}

sp<MediaPlayerBase> MediaPlayerService::Client::createPlayer(player_type playerType)
{
    // determine if we have the right player type
    sp<MediaPlayerBase> p = mPlayer;
    if ((p != NULL) && (p->playerType() != playerType)) {
        ALOGV("delete player");
        p.clear();
    }
    if (p == NULL) {
        p = MediaPlayerFactory::createPlayer(playerType, this, notify);
    }

    if (p != NULL) {
        p->setUID(mUID);
    }

    return p;
}

sp<MediaPlayerBase> MediaPlayerService::Client::setDataSource_pre(
        player_type playerType)
{
    ALOGV("player type = %d", playerType);

    // create the right type of player
    sp<MediaPlayerBase> p = createPlayer(playerType);
    if (p == NULL) {
        return p;
    }

    if (!p->hardwareOutput()) {
        mAudioOutput = new AudioOutput(mAudioSessionId, IPCThreadState::self()->getCallingUid());
        static_cast<MediaPlayerInterface*>(p.get())->setAudioSink(mAudioOutput);
    }

    return p;
}

void MediaPlayerService::Client::setDataSource_post(
        const sp<MediaPlayerBase>& p,
        status_t status)
{
    ALOGV(" setDataSource");
    mStatus = status;
    if (mStatus != OK) {
        ALOGE("  error: %d", mStatus);
        return;
    }

    // Set the re-transmission endpoint if one was chosen.
    if (mRetransmitEndpointValid) {
        mStatus = p->setRetransmitEndpoint(&mRetransmitEndpoint);
        if (mStatus != NO_ERROR) {
            ALOGE("setRetransmitEndpoint error: %d", mStatus);
        }
    }

    if (mStatus == OK) {
        mPlayer = p;
    }
}

status_t MediaPlayerService::Client::setDataSource(
        const char *url, const KeyedVector<String8, String8> *headers)
{
    ALOGV("setDataSource(%s)", url);
    if (url == NULL)
        return UNKNOWN_ERROR;

    if ((strncmp(url, "http://", 7) == 0) ||
        (strncmp(url, "https://", 8) == 0) ||
        (strncmp(url, "rtsp://", 7) == 0)) {
        if (!checkPermission("android.permission.INTERNET")) {
            return PERMISSION_DENIED;
        }
    }

    if (strncmp(url, "content://", 10) == 0) {
        // get a filedescriptor for the content Uri and
        // pass it to the setDataSource(fd) method

        String16 url16(url);
        int fd = android::openContentProviderFile(url16);
        if (fd < 0)
        {
            ALOGE("Couldn't open fd for %s", url);
            return UNKNOWN_ERROR;
        }
        setDataSource(fd, 0, 0x7fffffffffLL); // this sets mStatus
        close(fd);
        return mStatus;
    } else {
        player_type playerType = MediaPlayerFactory::getPlayerType(this, url);
        sp<MediaPlayerBase> p = setDataSource_pre(playerType);
        if (p == NULL) {
            return NO_INIT;
        }

        setDataSource_post(p, p->setDataSource(url, headers));
        return mStatus;
    }
}

status_t MediaPlayerService::Client::setDataSource(int fd, int64_t offset, int64_t length)
{
    ALOGV("setDataSource fd=%d, offset=%lld, length=%lld", fd, offset, length);
    struct stat sb;
    int ret = fstat(fd, &sb);
    if (ret != 0) {
        ALOGE("fstat(%d) failed: %d, %s", fd, ret, strerror(errno));
        return UNKNOWN_ERROR;
    }

    ALOGV("st_dev  = %llu", sb.st_dev);
    ALOGV("st_mode = %u", sb.st_mode);
    ALOGV("st_uid  = %lu", sb.st_uid);
    ALOGV("st_gid  = %lu", sb.st_gid);
    ALOGV("st_size = %llu", sb.st_size);

    if (offset >= sb.st_size) {
        ALOGE("offset error");
        ::close(fd);
        return UNKNOWN_ERROR;
    }
    if (offset + length > sb.st_size) {
        length = sb.st_size - offset;
        ALOGV("calculated length = %lld", length);
    }

    player_type playerType = MediaPlayerFactory::getPlayerType(this,
                                                               fd,
                                                               offset,
                                                               length);
    sp<MediaPlayerBase> p = setDataSource_pre(playerType);
    if (p == NULL) {
        return NO_INIT;
    }

    // now set data source
    setDataSource_post(p, p->setDataSource(fd, offset, length));
    return mStatus;
}

status_t MediaPlayerService::Client::setDataSource(
        const sp<IStreamSource> &source) {
    // create the right type of player
    player_type playerType = MediaPlayerFactory::getPlayerType(this, source);
    sp<MediaPlayerBase> p = setDataSource_pre(playerType);
    if (p == NULL) {
        return NO_INIT;
    }

    // now set data source
    setDataSource_post(p, p->setDataSource(source));
    return mStatus;
}

void MediaPlayerService::Client::disconnectNativeWindow() {
    if (mConnectedWindow != NULL) {
        status_t err = native_window_api_disconnect(mConnectedWindow.get(),
                NATIVE_WINDOW_API_MEDIA);

        if (err != OK) {
            ALOGW("native_window_api_disconnect returned an error: %s (%d)",
                    strerror(-err), err);
        }
    }
    mConnectedWindow.clear();
}

status_t MediaPlayerService::Client::setVideoSurfaceTexture(
        const sp<IGraphicBufferProducer>& bufferProducer)
{
    ALOGV("[%d] setVideoSurfaceTexture(%p)", mConnId, bufferProducer.get());
    sp<MediaPlayerBase> p = getPlayer();
    if (p == 0) return UNKNOWN_ERROR;

    sp<IBinder> binder(bufferProducer == NULL ? NULL :
            bufferProducer->asBinder());
    if (mConnectedWindowBinder == binder) {
        return OK;
    }

    sp<ANativeWindow> anw;
    if (bufferProducer != NULL) {
        anw = new Surface(bufferProducer, true /* controlledByApp */);
        status_t err = native_window_api_connect(anw.get(),
                NATIVE_WINDOW_API_MEDIA);

        if (err != OK) {
            ALOGE("setVideoSurfaceTexture failed: %d", err);
            // Note that we must do the reset before disconnecting from the ANW.
            // Otherwise queue/dequeue calls could be made on the disconnected
            // ANW, which may result in errors.
            reset();

            disconnectNativeWindow();

            return err;
        }
    }

    // Note that we must set the player's new GraphicBufferProducer before
    // disconnecting the old one.  Otherwise queue/dequeue calls could be made
    // on the disconnected ANW, which may result in errors.
    status_t err = p->setVideoSurfaceTexture(bufferProducer);

    disconnectNativeWindow();

    mConnectedWindow = anw;

    if (err == OK) {
        mConnectedWindowBinder = binder;
    } else {
        disconnectNativeWindow();
    }

    return err;
}

status_t MediaPlayerService::Client::invoke(const Parcel& request,
                                            Parcel *reply)
{
    sp<MediaPlayerBase> p = getPlayer();
    if (p == NULL) return UNKNOWN_ERROR;
    return p->invoke(request, reply);
}

// This call doesn't need to access the native player.
status_t MediaPlayerService::Client::setMetadataFilter(const Parcel& filter)
{
    status_t status;
    media::Metadata::Filter allow, drop;

    if (unmarshallFilter(filter, &allow, &status) &&
        unmarshallFilter(filter, &drop, &status)) {
        Mutex::Autolock lock(mLock);

        mMetadataAllow = allow;
        mMetadataDrop = drop;
    }
    return status;
}

status_t MediaPlayerService::Client::getMetadata(
        bool update_only, bool apply_filter, Parcel *reply)
{
    sp<MediaPlayerBase> player = getPlayer();
    if (player == 0) return UNKNOWN_ERROR;

    status_t status;
    // Placeholder for the return code, updated by the caller.
    reply->writeInt32(-1);

    media::Metadata::Filter ids;

    // We don't block notifications while we fetch the data. We clear
    // mMetadataUpdated first so we don't lose notifications happening
    // during the rest of this call.
    {
        Mutex::Autolock lock(mLock);
        if (update_only) {
            ids = mMetadataUpdated;
        }
        mMetadataUpdated.clear();
    }

    media::Metadata metadata(reply);

    metadata.appendHeader();
    status = player->getMetadata(ids, reply);

    if (status != OK) {
        metadata.resetParcel();
        ALOGE("getMetadata failed %d", status);
        return status;
    }

    // FIXME: Implement filtering on the result. Not critical since
    // filtering takes place on the update notifications already. This
    // would be when all the metadata are fetch and a filter is set.

    // Everything is fine, update the metadata length.
    metadata.updateLength();
    return OK;
}

status_t MediaPlayerService::Client::prepareAsync()
{
    ALOGV("[%d] prepareAsync", mConnId);
    sp<MediaPlayerBase> p = getPlayer();
    if (p == 0) return UNKNOWN_ERROR;
    status_t ret = p->prepareAsync();
#if CALLBACK_ANTAGONIZER
    ALOGD("start Antagonizer");
    if (ret == NO_ERROR) mAntagonizer->start();
#endif
    return ret;
}

status_t MediaPlayerService::Client::start()
{
    ALOGV("[%d] start", mConnId);
    sp<MediaPlayerBase> p = getPlayer();
    if (p == 0) return UNKNOWN_ERROR;
    p->setLooping(mLoop);
    return p->start();
}

status_t MediaPlayerService::Client::stop()
{
    ALOGV("[%d] stop", mConnId);
    sp<MediaPlayerBase> p = getPlayer();
    if (p == 0) return UNKNOWN_ERROR;
    return p->stop();
}

status_t MediaPlayerService::Client::pause()
{
    ALOGV("[%d] pause", mConnId);
    sp<MediaPlayerBase> p = getPlayer();
    if (p == 0) return UNKNOWN_ERROR;
    return p->pause();
}

status_t MediaPlayerService::Client::isPlaying(bool* state)
{
    *state = false;
    sp<MediaPlayerBase> p = getPlayer();
    if (p == 0) return UNKNOWN_ERROR;
    *state = p->isPlaying();
    ALOGV("[%d] isPlaying: %d", mConnId, *state);
    return NO_ERROR;
}

status_t MediaPlayerService::Client::getCurrentPosition(int *msec)
{
    ALOGV("getCurrentPosition");
    sp<MediaPlayerBase> p = getPlayer();
    if (p == 0) return UNKNOWN_ERROR;
    status_t ret = p->getCurrentPosition(msec);
    if (ret == NO_ERROR) {
        ALOGV("[%d] getCurrentPosition = %d", mConnId, *msec);
    } else {
        ALOGE("getCurrentPosition returned %d", ret);
    }
    return ret;
}

status_t MediaPlayerService::Client::getDuration(int *msec)
{
    ALOGV("getDuration");
    sp<MediaPlayerBase> p = getPlayer();
    if (p == 0) return UNKNOWN_ERROR;
    status_t ret = p->getDuration(msec);
    if (ret == NO_ERROR) {
        ALOGV("[%d] getDuration = %d", mConnId, *msec);
    } else {
        ALOGE("getDuration returned %d", ret);
    }
    return ret;
}

status_t MediaPlayerService::Client::setNextPlayer(const sp<IMediaPlayer>& player) {
    ALOGV("setNextPlayer");
    Mutex::Autolock l(mLock);
    sp<Client> c = static_cast<Client*>(player.get());
    mNextClient = c;

    if (c != NULL) {
        if (mAudioOutput != NULL) {
            mAudioOutput->setNextOutput(c->mAudioOutput);
        } else if ((mPlayer != NULL) && !mPlayer->hardwareOutput()) {
            ALOGE("no current audio output");
        }

        if ((mPlayer != NULL) && (mNextClient->getPlayer() != NULL)) {
            mPlayer->setNextPlayer(mNextClient->getPlayer());
        }
    }

    return OK;
}

status_t MediaPlayerService::Client::seekTo(int msec)
{
    ALOGV("[%d] seekTo(%d)", mConnId, msec);
    sp<MediaPlayerBase> p = getPlayer();
    if (p == 0) return UNKNOWN_ERROR;
    return p->seekTo(msec);
}

status_t MediaPlayerService::Client::reset()
{
    ALOGV("[%d] reset", mConnId);
    mRetransmitEndpointValid = false;
    sp<MediaPlayerBase> p = getPlayer();
    if (p == 0) return UNKNOWN_ERROR;
    return p->reset();
}

status_t MediaPlayerService::Client::setAudioStreamType(audio_stream_type_t type)
{
    ALOGV("[%d] setAudioStreamType(%d)", mConnId, type);
    // TODO: for hardware output, call player instead
    Mutex::Autolock l(mLock);
    if (mAudioOutput != 0) mAudioOutput->setAudioStreamType(type);
    return NO_ERROR;
}

status_t MediaPlayerService::Client::setLooping(int loop)
{
    ALOGV("[%d] setLooping(%d)", mConnId, loop);
    mLoop = loop;
    sp<MediaPlayerBase> p = getPlayer();
    if (p != 0) return p->setLooping(loop);
    return NO_ERROR;
}

status_t MediaPlayerService::Client::setVolume(float leftVolume, float rightVolume)
{
    ALOGV("[%d] setVolume(%f, %f)", mConnId, leftVolume, rightVolume);

    // for hardware output, call player instead
    sp<MediaPlayerBase> p = getPlayer();
    {
      Mutex::Autolock l(mLock);
      if (p != 0 && p->hardwareOutput()) {
          MediaPlayerHWInterface* hwp =
                  reinterpret_cast<MediaPlayerHWInterface*>(p.get());
          return hwp->setVolume(leftVolume, rightVolume);
      } else {
          if (mAudioOutput != 0) mAudioOutput->setVolume(leftVolume, rightVolume);
          return NO_ERROR;
      }
    }

    return NO_ERROR;
}

status_t MediaPlayerService::Client::setAuxEffectSendLevel(float level)
{
    ALOGV("[%d] setAuxEffectSendLevel(%f)", mConnId, level);
    Mutex::Autolock l(mLock);
    if (mAudioOutput != 0) return mAudioOutput->setAuxEffectSendLevel(level);
    return NO_ERROR;
}

status_t MediaPlayerService::Client::attachAuxEffect(int effectId)
{
    ALOGV("[%d] attachAuxEffect(%d)", mConnId, effectId);
    Mutex::Autolock l(mLock);
    if (mAudioOutput != 0) return mAudioOutput->attachAuxEffect(effectId);
    return NO_ERROR;
}

status_t MediaPlayerService::Client::setParameter(int key, const Parcel &request) {
    ALOGV("[%d] setParameter(%d)", mConnId, key);
    sp<MediaPlayerBase> p = getPlayer();
    if (p == 0) return UNKNOWN_ERROR;
    return p->setParameter(key, request);
}

status_t MediaPlayerService::Client::getParameter(int key, Parcel *reply) {
    ALOGV("[%d] getParameter(%d)", mConnId, key);
    sp<MediaPlayerBase> p = getPlayer();
    if (p == 0) return UNKNOWN_ERROR;
    return p->getParameter(key, reply);
}

status_t MediaPlayerService::Client::setRetransmitEndpoint(
        const struct sockaddr_in* endpoint) {

    if (NULL != endpoint) {
        uint32_t a = ntohl(endpoint->sin_addr.s_addr);
        uint16_t p = ntohs(endpoint->sin_port);
        ALOGV("[%d] setRetransmitEndpoint(%u.%u.%u.%u:%hu)", mConnId,
                (a >> 24), (a >> 16) & 0xFF, (a >> 8) & 0xFF, (a & 0xFF), p);
    } else {
        ALOGV("[%d] setRetransmitEndpoint = <none>", mConnId);
    }

    sp<MediaPlayerBase> p = getPlayer();

    // Right now, the only valid time to set a retransmit endpoint is before
    // player selection has been made (since the presence or absence of a
    // retransmit endpoint is going to determine which player is selected during
    // setDataSource).
    if (p != 0) return INVALID_OPERATION;

    if (NULL != endpoint) {
        mRetransmitEndpoint = *endpoint;
        mRetransmitEndpointValid = true;
    } else {
        mRetransmitEndpointValid = false;
    }

    return NO_ERROR;
}

status_t MediaPlayerService::Client::getRetransmitEndpoint(
        struct sockaddr_in* endpoint)
{
    if (NULL == endpoint)
        return BAD_VALUE;

    sp<MediaPlayerBase> p = getPlayer();

    if (p != NULL)
        return p->getRetransmitEndpoint(endpoint);

    if (!mRetransmitEndpointValid)
        return NO_INIT;

    *endpoint = mRetransmitEndpoint;

    return NO_ERROR;
}

void MediaPlayerService::Client::notify(
        void* cookie, int msg, int ext1, int ext2, const Parcel *obj)
{
    Client* client = static_cast<Client*>(cookie);
    if (client == NULL) {
        return;
    }

    sp<IMediaPlayerClient> c;
    {
        Mutex::Autolock l(client->mLock);
        c = client->mClient;
        if (msg == MEDIA_PLAYBACK_COMPLETE && client->mNextClient != NULL) {
            if (client->mAudioOutput != NULL)
                client->mAudioOutput->switchToNextOutput();
            client->mNextClient->start();
            client->mNextClient->mClient->notify(MEDIA_INFO, MEDIA_INFO_STARTED_AS_NEXT, 0, obj);
        }
    }

    if (MEDIA_INFO == msg &&
        MEDIA_INFO_METADATA_UPDATE == ext1) {
        const media::Metadata::Type metadata_type = ext2;

        if(client->shouldDropMetadata(metadata_type)) {
            return;
        }

        // Update the list of metadata that have changed. getMetadata
        // also access mMetadataUpdated and clears it.
        client->addNewMetadataUpdate(metadata_type);
    }

    if (c != NULL) {
        ALOGV("[%d] notify (%p, %d, %d, %d)", client->mConnId, cookie, msg, ext1, ext2);
        c->notify(msg, ext1, ext2, obj);
    }
}


bool MediaPlayerService::Client::shouldDropMetadata(media::Metadata::Type code) const
{
    Mutex::Autolock lock(mLock);

    if (findMetadata(mMetadataDrop, code)) {
        return true;
    }

    if (mMetadataAllow.isEmpty() || findMetadata(mMetadataAllow, code)) {
        return false;
    } else {
        return true;
    }
}


void MediaPlayerService::Client::addNewMetadataUpdate(media::Metadata::Type metadata_type) {
    Mutex::Autolock lock(mLock);
    if (mMetadataUpdated.indexOf(metadata_type) < 0) {
        mMetadataUpdated.add(metadata_type);
    }
}

#if CALLBACK_ANTAGONIZER
const int Antagonizer::interval = 10000; // 10 msecs

Antagonizer::Antagonizer(notify_callback_f cb, void* client) :
    mExit(false), mActive(false), mClient(client), mCb(cb)
{
    createThread(callbackThread, this);
}

void Antagonizer::kill()
{
    Mutex::Autolock _l(mLock);
    mActive = false;
    mExit = true;
    mCondition.wait(mLock);
}

int Antagonizer::callbackThread(void* user)
{
    ALOGD("Antagonizer started");
    Antagonizer* p = reinterpret_cast<Antagonizer*>(user);
    while (!p->mExit) {
        if (p->mActive) {
            ALOGV("send event");
            p->mCb(p->mClient, 0, 0, 0);
        }
        usleep(interval);
    }
    Mutex::Autolock _l(p->mLock);
    p->mCondition.signal();
    ALOGD("Antagonizer stopped");
    return 0;
}
#endif

status_t MediaPlayerService::decode(const char* url, uint32_t *pSampleRate, int* pNumChannels,
                                       audio_format_t* pFormat,
                                       const sp<IMemoryHeap>& heap, size_t *pSize)
{
    ALOGV("decode(%s)", url);
    sp<MediaPlayerBase> player;
    status_t status = BAD_VALUE;

    // Protect our precious, precious DRMd ringtones by only allowing
    // decoding of http, but not filesystem paths or content Uris.
    // If the application wants to decode those, it should open a
    // filedescriptor for them and use that.
    if (url != NULL && strncmp(url, "http://", 7) != 0) {
        ALOGD("Can't decode %s by path, use filedescriptor instead", url);
        return BAD_VALUE;
    }

    player_type playerType =
        MediaPlayerFactory::getPlayerType(NULL /* client */, url);
    ALOGV("player type = %d", playerType);

    // create the right type of player
    sp<AudioCache> cache = new AudioCache(heap);
    player = MediaPlayerFactory::createPlayer(playerType, cache.get(), cache->notify);
    if (player == NULL) goto Exit;
    if (player->hardwareOutput()) goto Exit;

    static_cast<MediaPlayerInterface*>(player.get())->setAudioSink(cache);

    // set data source
    if (player->setDataSource(url) != NO_ERROR) goto Exit;

    ALOGV("prepare");
    player->prepareAsync();

    ALOGV("wait for prepare");
    if (cache->wait() != NO_ERROR) goto Exit;

    ALOGV("start");
    player->start();

    ALOGV("wait for playback complete");
    cache->wait();
    // in case of error, return what was successfully decoded.
    if (cache->size() == 0) {
        goto Exit;
    }

    *pSize = cache->size();
    *pSampleRate = cache->sampleRate();
    *pNumChannels = cache->channelCount();
    *pFormat = cache->format();
    ALOGV("return size %d sampleRate=%u, channelCount = %d, format = %d",
          *pSize, *pSampleRate, *pNumChannels, *pFormat);
    status = NO_ERROR;

Exit:
    if (player != 0) player->reset();
    return status;
}

status_t MediaPlayerService::decode(int fd, int64_t offset, int64_t length,
                                       uint32_t *pSampleRate, int* pNumChannels,
                                       audio_format_t* pFormat,
                                       const sp<IMemoryHeap>& heap, size_t *pSize)
{
    ALOGV("decode(%d, %lld, %lld)", fd, offset, length);
    sp<MediaPlayerBase> player;
    status_t status = BAD_VALUE;

    player_type playerType = MediaPlayerFactory::getPlayerType(NULL /* client */,
                                                               fd,
                                                               offset,
                                                               length);
    ALOGV("player type = %d", playerType);

    // create the right type of player
    sp<AudioCache> cache = new AudioCache(heap);
    player = MediaPlayerFactory::createPlayer(playerType, cache.get(), cache->notify);
    if (player == NULL) goto Exit;
    if (player->hardwareOutput()) goto Exit;

    static_cast<MediaPlayerInterface*>(player.get())->setAudioSink(cache);

    // set data source
    if (player->setDataSource(fd, offset, length) != NO_ERROR) goto Exit;

    ALOGV("prepare");
    player->prepareAsync();

    ALOGV("wait for prepare");
    if (cache->wait() != NO_ERROR) goto Exit;

    ALOGV("start");
    player->start();

    ALOGV("wait for playback complete");
    cache->wait();
    // in case of error, return what was successfully decoded.
    if (cache->size() == 0) {
        goto Exit;
    }

    *pSize = cache->size();
    *pSampleRate = cache->sampleRate();
    *pNumChannels = cache->channelCount();
    *pFormat = cache->format();
    ALOGV("return size %d, sampleRate=%u, channelCount = %d, format = %d",
          *pSize, *pSampleRate, *pNumChannels, *pFormat);
    status = NO_ERROR;

Exit:
    if (player != 0) player->reset();
    ::close(fd);
    return status;
}


#undef LOG_TAG
#define LOG_TAG "AudioSink"
MediaPlayerService::AudioOutput::AudioOutput(int sessionId, int uid)
    : mCallback(NULL),
      mCallbackCookie(NULL),
      mCallbackData(NULL),
      mBytesWritten(0),
      mSessionId(sessionId),
      mUid(uid),
      mFlags(AUDIO_OUTPUT_FLAG_NONE) {
    ALOGV("AudioOutput(%d)", sessionId);
    mStreamType = AUDIO_STREAM_MUSIC;
    mLeftVolume = 1.0;
    mRightVolume = 1.0;
    mPlaybackRatePermille = 1000;
    mSampleRateHz = 0;
    mMsecsPerFrame = 0;
    mAuxEffectId = 0;
    mSendLevel = 0.0;
    setMinBufferCount();
}

MediaPlayerService::AudioOutput::~AudioOutput()
{
    close();
    delete mCallbackData;
}

void MediaPlayerService::AudioOutput::setMinBufferCount()
{
    char value[PROPERTY_VALUE_MAX];
    if (property_get("ro.kernel.qemu", value, 0)) {
        mIsOnEmulator = true;
        mMinBufferCount = 12;  // to prevent systematic buffer underrun for emulator
    }
}

bool MediaPlayerService::AudioOutput::isOnEmulator()
{
    setMinBufferCount();
    return mIsOnEmulator;
}

int MediaPlayerService::AudioOutput::getMinBufferCount()
{
    setMinBufferCount();
    return mMinBufferCount;
}

ssize_t MediaPlayerService::AudioOutput::bufferSize() const
{
    if (mTrack == 0) return NO_INIT;
    return mTrack->frameCount() * frameSize();
}

ssize_t MediaPlayerService::AudioOutput::frameCount() const
{
    if (mTrack == 0) return NO_INIT;
    return mTrack->frameCount();
}

ssize_t MediaPlayerService::AudioOutput::channelCount() const
{
    if (mTrack == 0) return NO_INIT;
    return mTrack->channelCount();
}

ssize_t MediaPlayerService::AudioOutput::frameSize() const
{
    if (mTrack == 0) return NO_INIT;
    return mTrack->frameSize();
}

uint32_t MediaPlayerService::AudioOutput::latency () const
{
    if (mTrack == 0) return 0;
    return mTrack->latency();
}

float MediaPlayerService::AudioOutput::msecsPerFrame() const
{
    return mMsecsPerFrame;
}

status_t MediaPlayerService::AudioOutput::getPosition(uint32_t *position) const
{
    if (mTrack == 0) return NO_INIT;
    return mTrack->getPosition(position);
}

status_t MediaPlayerService::AudioOutput::getFramesWritten(uint32_t *frameswritten) const
{
    if (mTrack == 0) return NO_INIT;
    *frameswritten = mBytesWritten / frameSize();
    return OK;
}

status_t MediaPlayerService::AudioOutput::setParameters(const String8& keyValuePairs)
{
    if (mTrack == 0) return NO_INIT;
    return mTrack->setParameters(keyValuePairs);
}

String8  MediaPlayerService::AudioOutput::getParameters(const String8& keys)
{
    if (mTrack == 0) return String8::empty();
    return mTrack->getParameters(keys);
}

void MediaPlayerService::AudioOutput::deleteRecycledTrack()
{
    ALOGV("deleteRecycledTrack");

    if (mRecycledTrack != 0) {

        if (mCallbackData != NULL) {
            mCallbackData->setOutput(NULL);
            mCallbackData->endTrackSwitch();
        }

        if ((mRecycledTrack->getFlags() & AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD) == 0) {
            mRecycledTrack->flush();
        }
        // An offloaded track isn't flushed because the STREAM_END is reported
        // slightly prematurely to allow time for the gapless track switch
        // but this means that if we decide not to recycle the track there
        // could be a small amount of residual data still playing. We leave
        // AudioFlinger to drain the track.

        mRecycledTrack.clear();
        delete mCallbackData;
        mCallbackData = NULL;
        close();
    }
}

status_t MediaPlayerService::AudioOutput::open(
        uint32_t sampleRate, int channelCount, audio_channel_mask_t channelMask,
        audio_format_t format, int bufferCount,
        AudioCallback cb, void *cookie,
        audio_output_flags_t flags,
        const audio_offload_info_t *offloadInfo)
{
    mCallback = cb;
    mCallbackCookie = cookie;

    // Check argument "bufferCount" against the mininum buffer count
    if (bufferCount < mMinBufferCount) {
        ALOGD("bufferCount (%d) is too small and increased to %d", bufferCount, mMinBufferCount);
        bufferCount = mMinBufferCount;

    }
    ALOGV("open(%u, %d, 0x%x, 0x%x, %d, %d 0x%x)", sampleRate, channelCount, channelMask,
                format, bufferCount, mSessionId, flags);
    uint32_t afSampleRate;
    size_t afFrameCount;
    uint32_t frameCount;

    // offloading is only supported in callback mode for now.
    // offloadInfo must be present if offload flag is set
    if (((flags & AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD) != 0) &&
            ((cb == NULL) || (offloadInfo == NULL))) {
        return BAD_VALUE;
    }

    if ((flags & AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD) != 0) {
        frameCount = 0; // AudioTrack will get frame count from AudioFlinger
    } else {
        uint32_t afSampleRate;
        size_t afFrameCount;

        if (AudioSystem::getOutputFrameCount(&afFrameCount, mStreamType) != NO_ERROR) {
            return NO_INIT;
        }
        if (AudioSystem::getOutputSamplingRate(&afSampleRate, mStreamType) != NO_ERROR) {
            return NO_INIT;
        }

        frameCount = (sampleRate*afFrameCount*bufferCount)/afSampleRate;
    }

    if (channelMask == CHANNEL_MASK_USE_CHANNEL_ORDER) {
        channelMask = audio_channel_out_mask_from_count(channelCount);
        if (0 == channelMask) {
            ALOGE("open() error, can\'t derive mask for %d audio channels", channelCount);
            return NO_INIT;
        }
    }

    // Check whether we can recycle the track
    bool reuse = false;
    bool bothOffloaded = false;

    if (mRecycledTrack != 0) {
        // check whether we are switching between two offloaded tracks
        bothOffloaded = (flags & mRecycledTrack->getFlags()
                                & AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD) != 0;

        // check if the existing track can be reused as-is, or if a new track needs to be created.
        reuse = true;

        if ((mCallbackData == NULL && mCallback != NULL) ||
                (mCallbackData != NULL && mCallback == NULL)) {
            // recycled track uses callbacks but the caller wants to use writes, or vice versa
            ALOGV("can't chain callback and write");
            reuse = false;
        } else if ((mRecycledTrack->getSampleRate() != sampleRate) ||
                (mRecycledTrack->channelCount() != (uint32_t)channelCount) ) {
            ALOGV("samplerate, channelcount differ: %u/%u Hz, %u/%d ch",
                  mRecycledTrack->getSampleRate(), sampleRate,
                  mRecycledTrack->channelCount(), channelCount);
            reuse = false;
        } else if (flags != mFlags) {
            ALOGV("output flags differ %08x/%08x", flags, mFlags);
            reuse = false;
        } else if (mRecycledTrack->format() != format) {
            reuse = false;
        }
    } else {
        ALOGV("no track available to recycle");
    }

    ALOGV_IF(bothOffloaded, "both tracks offloaded");

    // If we can't recycle and both tracks are offloaded
    // we must close the previous output before opening a new one
    if (bothOffloaded && !reuse) {
        ALOGV("both offloaded and not recycling");
        deleteRecycledTrack();
    }

    sp<AudioTrack> t;
    CallbackData *newcbd = NULL;

    // We don't attempt to create a new track if we are recycling an
    // offloaded track. But, if we are recycling a non-offloaded or we
    // are switching where one is offloaded and one isn't then we create
    // the new track in advance so that we can read additional stream info

    if (!(reuse && bothOffloaded)) {
        ALOGV("creating new AudioTrack");

        if (mCallback != NULL) {
            newcbd = new CallbackData(this);
            t = new AudioTrack(
                    mStreamType,
                    sampleRate,
                    format,
                    channelMask,
                    frameCount,
                    flags,
                    CallbackWrapper,
                    newcbd,
                    0,  // notification frames
                    mSessionId,
                    AudioTrack::TRANSFER_CALLBACK,
                    offloadInfo,
                    mUid);
        } else {
            t = new AudioTrack(
                    mStreamType,
                    sampleRate,
                    format,
                    channelMask,
                    frameCount,
                    flags,
                    NULL, // callback
                    NULL, // user data
                    0, // notification frames
                    mSessionId,
                    AudioTrack::TRANSFER_DEFAULT,
                    NULL, // offload info
                    mUid);
        }

        if ((t == 0) || (t->initCheck() != NO_ERROR)) {
            ALOGE("Unable to create audio track");
            delete newcbd;
            return NO_INIT;
        }
    }

    if (reuse) {
        CHECK(mRecycledTrack != NULL);

        if (!bothOffloaded) {
            if (mRecycledTrack->frameCount() != t->frameCount()) {
                ALOGV("framecount differs: %u/%u frames",
                      mRecycledTrack->frameCount(), t->frameCount());
                reuse = false;
            }
        }

        if (reuse) {
            ALOGV("chaining to next output and recycling track");
            close();
            mTrack = mRecycledTrack;
            mRecycledTrack.clear();
            if (mCallbackData != NULL) {
                mCallbackData->setOutput(this);
            }
            delete newcbd;
            return OK;
        }
    }

    // we're not going to reuse the track, unblock and flush it
    // this was done earlier if both tracks are offloaded
    if (!bothOffloaded) {
        deleteRecycledTrack();
    }

    CHECK((t != NULL) && ((mCallback == NULL) || (newcbd != NULL)));

    mCallbackData = newcbd;
    ALOGV("setVolume");
    t->setVolume(mLeftVolume, mRightVolume);

    mSampleRateHz = sampleRate;
    mFlags = flags;
    mMsecsPerFrame = mPlaybackRatePermille / (float) sampleRate;
    uint32_t pos;
    if (t->getPosition(&pos) == OK) {
        mBytesWritten = uint64_t(pos) * t->frameSize();
    }
    mTrack = t;

    status_t res = NO_ERROR;
    if ((flags & AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD) == 0) {
        res = t->setSampleRate(mPlaybackRatePermille * mSampleRateHz / 1000);
        if (res == NO_ERROR) {
            t->setAuxEffectSendLevel(mSendLevel);
            res = t->attachAuxEffect(mAuxEffectId);
        }
    }
    ALOGV("open() DONE status %d", res);
    return res;
}

status_t MediaPlayerService::AudioOutput::start()
{
    ALOGV("start");
    if (mCallbackData != NULL) {
        mCallbackData->endTrackSwitch();
    }
    if (mTrack != 0) {
        mTrack->setVolume(mLeftVolume, mRightVolume);
        mTrack->setAuxEffectSendLevel(mSendLevel);
        return mTrack->start();
    }
    return NO_INIT;
}

void MediaPlayerService::AudioOutput::setNextOutput(const sp<AudioOutput>& nextOutput) {
    mNextOutput = nextOutput;
}


void MediaPlayerService::AudioOutput::switchToNextOutput() {
    ALOGV("switchToNextOutput");
    if (mNextOutput != NULL) {
        if (mCallbackData != NULL) {
            mCallbackData->beginTrackSwitch();
        }
        delete mNextOutput->mCallbackData;
        mNextOutput->mCallbackData = mCallbackData;
        mCallbackData = NULL;
        mNextOutput->mRecycledTrack = mTrack;
        mTrack.clear();
        mNextOutput->mSampleRateHz = mSampleRateHz;
        mNextOutput->mMsecsPerFrame = mMsecsPerFrame;
        mNextOutput->mBytesWritten = mBytesWritten;
        mNextOutput->mFlags = mFlags;
    }
}

ssize_t MediaPlayerService::AudioOutput::write(const void* buffer, size_t size)
{
    LOG_FATAL_IF(mCallback != NULL, "Don't call write if supplying a callback.");

    //ALOGV("write(%p, %u)", buffer, size);
    if (mTrack != 0) {
        ssize_t ret = mTrack->write(buffer, size);
        mBytesWritten += ret;
        return ret;
    }
    return NO_INIT;
}

void MediaPlayerService::AudioOutput::stop()
{
    ALOGV("stop");
    if (mTrack != 0) mTrack->stop();
}

void MediaPlayerService::AudioOutput::flush()
{
    ALOGV("flush");
    if (mTrack != 0) mTrack->flush();
}

void MediaPlayerService::AudioOutput::pause()
{
    ALOGV("pause");
    if (mTrack != 0) mTrack->pause();
}

void MediaPlayerService::AudioOutput::close()
{
    ALOGV("close");
    mTrack.clear();
}

void MediaPlayerService::AudioOutput::setVolume(float left, float right)
{
    ALOGV("setVolume(%f, %f)", left, right);
    mLeftVolume = left;
    mRightVolume = right;
    if (mTrack != 0) {
        mTrack->setVolume(left, right);
    }
}

status_t MediaPlayerService::AudioOutput::setPlaybackRatePermille(int32_t ratePermille)
{
    ALOGV("setPlaybackRatePermille(%d)", ratePermille);
    status_t res = NO_ERROR;
    if (mTrack != 0) {
        res = mTrack->setSampleRate(ratePermille * mSampleRateHz / 1000);
    } else {
        res = NO_INIT;
    }
    mPlaybackRatePermille = ratePermille;
    if (mSampleRateHz != 0) {
        mMsecsPerFrame = mPlaybackRatePermille / (float) mSampleRateHz;
    }
    return res;
}

status_t MediaPlayerService::AudioOutput::setAuxEffectSendLevel(float level)
{
    ALOGV("setAuxEffectSendLevel(%f)", level);
    mSendLevel = level;
    if (mTrack != 0) {
        return mTrack->setAuxEffectSendLevel(level);
    }
    return NO_ERROR;
}

status_t MediaPlayerService::AudioOutput::attachAuxEffect(int effectId)
{
    ALOGV("attachAuxEffect(%d)", effectId);
    mAuxEffectId = effectId;
    if (mTrack != 0) {
        return mTrack->attachAuxEffect(effectId);
    }
    return NO_ERROR;
}

// static
void MediaPlayerService::AudioOutput::CallbackWrapper(
        int event, void *cookie, void *info) {
    //ALOGV("callbackwrapper");
    CallbackData *data = (CallbackData*)cookie;
    data->lock();
    AudioOutput *me = data->getOutput();
    AudioTrack::Buffer *buffer = (AudioTrack::Buffer *)info;
    if (me == NULL) {
        // no output set, likely because the track was scheduled to be reused
        // by another player, but the format turned out to be incompatible.
        data->unlock();
        if (buffer != NULL) {
            buffer->size = 0;
        }
        return;
    }

    switch(event) {
    case AudioTrack::EVENT_MORE_DATA: {
        size_t actualSize = (*me->mCallback)(
                me, buffer->raw, buffer->size, me->mCallbackCookie,
                CB_EVENT_FILL_BUFFER);

        if (actualSize == 0 && buffer->size > 0 && me->mNextOutput == NULL) {
            // We've reached EOS but the audio track is not stopped yet,
            // keep playing silence.

            memset(buffer->raw, 0, buffer->size);
            actualSize = buffer->size;
        }

        buffer->size = actualSize;
        } break;


    case AudioTrack::EVENT_STREAM_END:
        ALOGV("callbackwrapper: deliver EVENT_STREAM_END");
        (*me->mCallback)(me, NULL /* buffer */, 0 /* size */,
                me->mCallbackCookie, CB_EVENT_STREAM_END);
        break;

    case AudioTrack::EVENT_NEW_IAUDIOTRACK :
        ALOGV("callbackwrapper: deliver EVENT_TEAR_DOWN");
        (*me->mCallback)(me,  NULL /* buffer */, 0 /* size */,
                me->mCallbackCookie, CB_EVENT_TEAR_DOWN);
        break;

    default:
        ALOGE("received unknown event type: %d inside CallbackWrapper !", event);
    }

    data->unlock();
}

int MediaPlayerService::AudioOutput::getSessionId() const
{
    return mSessionId;
}

#undef LOG_TAG
#define LOG_TAG "AudioCache"
MediaPlayerService::AudioCache::AudioCache(const sp<IMemoryHeap>& heap) :
    mHeap(heap), mChannelCount(0), mFrameCount(1024), mSampleRate(0), mSize(0),
    mError(NO_ERROR),  mCommandComplete(false)
{
}

uint32_t MediaPlayerService::AudioCache::latency () const
{
    return 0;
}

float MediaPlayerService::AudioCache::msecsPerFrame() const
{
    return mMsecsPerFrame;
}

status_t MediaPlayerService::AudioCache::getPosition(uint32_t *position) const
{
    if (position == 0) return BAD_VALUE;
    *position = mSize;
    return NO_ERROR;
}

status_t MediaPlayerService::AudioCache::getFramesWritten(uint32_t *written) const
{
    if (written == 0) return BAD_VALUE;
    *written = mSize;
    return NO_ERROR;
}

////////////////////////////////////////////////////////////////////////////////

struct CallbackThread : public Thread {
    CallbackThread(const wp<MediaPlayerBase::AudioSink> &sink,
                   MediaPlayerBase::AudioSink::AudioCallback cb,
                   void *cookie);

protected:
    virtual ~CallbackThread();

    virtual bool threadLoop();

private:
    wp<MediaPlayerBase::AudioSink> mSink;
    MediaPlayerBase::AudioSink::AudioCallback mCallback;
    void *mCookie;
    void *mBuffer;
    size_t mBufferSize;

    CallbackThread(const CallbackThread &);
    CallbackThread &operator=(const CallbackThread &);
};

CallbackThread::CallbackThread(
        const wp<MediaPlayerBase::AudioSink> &sink,
        MediaPlayerBase::AudioSink::AudioCallback cb,
        void *cookie)
    : mSink(sink),
      mCallback(cb),
      mCookie(cookie),
      mBuffer(NULL),
      mBufferSize(0) {
}

CallbackThread::~CallbackThread() {
    if (mBuffer) {
        free(mBuffer);
        mBuffer = NULL;
    }
}

bool CallbackThread::threadLoop() {
    sp<MediaPlayerBase::AudioSink> sink = mSink.promote();
    if (sink == NULL) {
        return false;
    }

    if (mBuffer == NULL) {
        mBufferSize = sink->bufferSize();
        mBuffer = malloc(mBufferSize);
    }

    size_t actualSize =
        (*mCallback)(sink.get(), mBuffer, mBufferSize, mCookie,
                MediaPlayerBase::AudioSink::CB_EVENT_FILL_BUFFER);

    if (actualSize > 0) {
        sink->write(mBuffer, actualSize);
    }

    return true;
}

////////////////////////////////////////////////////////////////////////////////

status_t MediaPlayerService::AudioCache::open(
        uint32_t sampleRate, int channelCount, audio_channel_mask_t channelMask,
        audio_format_t format, int bufferCount,
        AudioCallback cb, void *cookie, audio_output_flags_t flags,
        const audio_offload_info_t *offloadInfo)
{
    ALOGV("open(%u, %d, 0x%x, %d, %d)", sampleRate, channelCount, channelMask, format, bufferCount);
    if (mHeap->getHeapID() < 0) {
        return NO_INIT;
    }

    mSampleRate = sampleRate;
    mChannelCount = (uint16_t)channelCount;
    mFormat = format;
    mMsecsPerFrame = 1.e3 / (float) sampleRate;

    if (cb != NULL) {
        mCallbackThread = new CallbackThread(this, cb, cookie);
    }
    return NO_ERROR;
}

status_t MediaPlayerService::AudioCache::start() {
    if (mCallbackThread != NULL) {
        mCallbackThread->run("AudioCache callback");
    }
    return NO_ERROR;
}

void MediaPlayerService::AudioCache::stop() {
    if (mCallbackThread != NULL) {
        mCallbackThread->requestExitAndWait();
    }
}

ssize_t MediaPlayerService::AudioCache::write(const void* buffer, size_t size)
{
    ALOGV("write(%p, %u)", buffer, size);
    if ((buffer == 0) || (size == 0)) return size;

    uint8_t* p = static_cast<uint8_t*>(mHeap->getBase());
    if (p == NULL) return NO_INIT;
    p += mSize;
    ALOGV("memcpy(%p, %p, %u)", p, buffer, size);
    if (mSize + size > mHeap->getSize()) {
        ALOGE("Heap size overflow! req size: %d, max size: %d", (mSize + size), mHeap->getSize());
        size = mHeap->getSize() - mSize;
    }
    memcpy(p, buffer, size);
    mSize += size;
    return size;
}

// call with lock held
status_t MediaPlayerService::AudioCache::wait()
{
    Mutex::Autolock lock(mLock);
    while (!mCommandComplete) {
        mSignal.wait(mLock);
    }
    mCommandComplete = false;

    if (mError == NO_ERROR) {
        ALOGV("wait - success");
    } else {
        ALOGV("wait - error");
    }
    return mError;
}

void MediaPlayerService::AudioCache::notify(
        void* cookie, int msg, int ext1, int ext2, const Parcel *obj)
{
    ALOGV("notify(%p, %d, %d, %d)", cookie, msg, ext1, ext2);
    AudioCache* p = static_cast<AudioCache*>(cookie);

    // ignore buffering messages
    switch (msg)
    {
    case MEDIA_ERROR:
        ALOGE("Error %d, %d occurred", ext1, ext2);
        p->mError = ext1;
        break;
    case MEDIA_PREPARED:
        ALOGV("prepared");
        break;
    case MEDIA_PLAYBACK_COMPLETE:
        ALOGV("playback complete");
        break;
    default:
        ALOGV("ignored");
        return;
    }

    // wake up thread
    Mutex::Autolock lock(p->mLock);
    p->mCommandComplete = true;
    p->mSignal.signal();
}

int MediaPlayerService::AudioCache::getSessionId() const
{
    return 0;
}

void MediaPlayerService::addBatteryData(uint32_t params)
{
    Mutex::Autolock lock(mLock);

    int32_t time = systemTime() / 1000000L;

    // change audio output devices. This notification comes from AudioFlinger
    if ((params & kBatteryDataSpeakerOn)
            || (params & kBatteryDataOtherAudioDeviceOn)) {

        int deviceOn[NUM_AUDIO_DEVICES];
        for (int i = 0; i < NUM_AUDIO_DEVICES; i++) {
            deviceOn[i] = 0;
        }

        if ((params & kBatteryDataSpeakerOn)
                && (params & kBatteryDataOtherAudioDeviceOn)) {
            deviceOn[SPEAKER_AND_OTHER] = 1;
        } else if (params & kBatteryDataSpeakerOn) {
            deviceOn[SPEAKER] = 1;
        } else {
            deviceOn[OTHER_AUDIO_DEVICE] = 1;
        }

        for (int i = 0; i < NUM_AUDIO_DEVICES; i++) {
            if (mBatteryAudio.deviceOn[i] != deviceOn[i]){

                if (mBatteryAudio.refCount > 0) { // if playing audio
                    if (!deviceOn[i]) {
                        mBatteryAudio.lastTime[i] += time;
                        mBatteryAudio.totalTime[i] += mBatteryAudio.lastTime[i];
                        mBatteryAudio.lastTime[i] = 0;
                    } else {
                        mBatteryAudio.lastTime[i] = 0 - time;
                    }
                }

                mBatteryAudio.deviceOn[i] = deviceOn[i];
            }
        }
        return;
    }

    // an sudio stream is started
    if (params & kBatteryDataAudioFlingerStart) {
        // record the start time only if currently no other audio
        // is being played
        if (mBatteryAudio.refCount == 0) {
            for (int i = 0; i < NUM_AUDIO_DEVICES; i++) {
                if (mBatteryAudio.deviceOn[i]) {
                    mBatteryAudio.lastTime[i] -= time;
                }
            }
        }

        mBatteryAudio.refCount ++;
        return;

    } else if (params & kBatteryDataAudioFlingerStop) {
        if (mBatteryAudio.refCount <= 0) {
            ALOGW("Battery track warning: refCount is <= 0");
            return;
        }

        // record the stop time only if currently this is the only
        // audio being played
        if (mBatteryAudio.refCount == 1) {
            for (int i = 0; i < NUM_AUDIO_DEVICES; i++) {
                if (mBatteryAudio.deviceOn[i]) {
                    mBatteryAudio.lastTime[i] += time;
                    mBatteryAudio.totalTime[i] += mBatteryAudio.lastTime[i];
                    mBatteryAudio.lastTime[i] = 0;
                }
            }
        }

        mBatteryAudio.refCount --;
        return;
    }

    int uid = IPCThreadState::self()->getCallingUid();
    if (uid == AID_MEDIA) {
        return;
    }
    int index = mBatteryData.indexOfKey(uid);

    if (index < 0) { // create a new entry for this UID
        BatteryUsageInfo info;
        info.audioTotalTime = 0;
        info.videoTotalTime = 0;
        info.audioLastTime = 0;
        info.videoLastTime = 0;
        info.refCount = 0;

        if (mBatteryData.add(uid, info) == NO_MEMORY) {
            ALOGE("Battery track error: no memory for new app");
            return;
        }
    }

    BatteryUsageInfo &info = mBatteryData.editValueFor(uid);

    if (params & kBatteryDataCodecStarted) {
        if (params & kBatteryDataTrackAudio) {
            info.audioLastTime -= time;
            info.refCount ++;
        }
        if (params & kBatteryDataTrackVideo) {
            info.videoLastTime -= time;
            info.refCount ++;
        }
    } else {
        if (info.refCount == 0) {
            ALOGW("Battery track warning: refCount is already 0");
            return;
        } else if (info.refCount < 0) {
            ALOGE("Battery track error: refCount < 0");
            mBatteryData.removeItem(uid);
            return;
        }

        if (params & kBatteryDataTrackAudio) {
            info.audioLastTime += time;
            info.refCount --;
        }
        if (params & kBatteryDataTrackVideo) {
            info.videoLastTime += time;
            info.refCount --;
        }

        // no stream is being played by this UID
        if (info.refCount == 0) {
            info.audioTotalTime += info.audioLastTime;
            info.audioLastTime = 0;
            info.videoTotalTime += info.videoLastTime;
            info.videoLastTime = 0;
        }
    }
}

status_t MediaPlayerService::pullBatteryData(Parcel* reply) {
    Mutex::Autolock lock(mLock);

    // audio output devices usage
    int32_t time = systemTime() / 1000000L; //in ms
    int32_t totalTime;

    for (int i = 0; i < NUM_AUDIO_DEVICES; i++) {
        totalTime = mBatteryAudio.totalTime[i];

        if (mBatteryAudio.deviceOn[i]
            && (mBatteryAudio.lastTime[i] != 0)) {
                int32_t tmpTime = mBatteryAudio.lastTime[i] + time;
                totalTime += tmpTime;
        }

        reply->writeInt32(totalTime);
        // reset the total time
        mBatteryAudio.totalTime[i] = 0;
   }

    // codec usage
    BatteryUsageInfo info;
    int size = mBatteryData.size();

    reply->writeInt32(size);
    int i = 0;

    while (i < size) {
        info = mBatteryData.valueAt(i);

        reply->writeInt32(mBatteryData.keyAt(i)); //UID
        reply->writeInt32(info.audioTotalTime);
        reply->writeInt32(info.videoTotalTime);

        info.audioTotalTime = 0;
        info.videoTotalTime = 0;

        // remove the UID entry where no stream is being played
        if (info.refCount <= 0) {
            mBatteryData.removeItemsAt(i);
            size --;
            i --;
        }
        i++;
    }
    return NO_ERROR;
}
} // namespace android
