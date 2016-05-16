/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

// test RemoteAudio with fake TCP

#include <unistd.h>

#include <utils/String8.h>

#include <gtest/gtest.h>

#include <utils/StrongPointer.h>

#include <Log.h>
#include <audio/AudioHardware.h>
#include <audio/AudioProtocol.h>
#include <audio/AudioSignalFactory.h>
#include <ClientSocket.h>
#include <audio/RemoteAudio.h>



void assertTrue(bool cond) {
    ASSERT_TRUE(cond);
}
void assertData(const char* data1, const char* data2, int len) {
    for (int i = 0; i < len; i++) {
        //LOGD("0x%x vs 0x%x", data1[i], data2[i]);
        ASSERT_TRUE(data1[i] == data2[i]);
    }
}

class ClientSocketForTest: public ClientSocket {
public:
    ClientSocketForTest()
        : mToRead(NULL),
          mReadLength(0) {};

    virtual ~ClientSocketForTest() {
        close(mSocket);
        close(mPipeWrFd);
    }
    virtual bool init(const char* hostIp, int port, bool enableTimeout = false) {
        LOGD("ClientSocketForTest::init");
        // use this fd to work with poll
        int pipefd[2];
        if (pipe(pipefd)  == -1) {
            LOGE("cannot create pipe");
            return false;
        }
        LOGD("pipe %d %d", pipefd[0], pipefd[1]);
        mSocket = pipefd[0];
        mPipeWrFd = pipefd[1];
        const char ipExpectation[] = "127.0.0.1";
        assertTrue(memcmp(ipExpectation, hostIp, sizeof(ipExpectation)) == 0);
        return true;
    }
    virtual bool readData(char* data, int len, int timeoutInMs = 0) {
        read(mSocket, data, len);
        return true;
    }
    virtual bool sendData(const char* data, int len) {
        assertTrue((len + mSendPointer) <= mSendLength);
        assertData(data, mToSend + mSendPointer, len);
        mSendPointer += len;
        if ((mToRead != NULL) && (mReadLength != 0)) {
            LOGD("fake TCP copy reply %d", mReadLength);
            write(mPipeWrFd, mToRead, mReadLength);
            mToRead = NULL; // prevent writing the same data again
        }
        return true;
    }

    void setSendExpectation(const char* data, int len) {
        mToSend = data;
        mSendLength = len;
        mSendPointer = 0;
    }
    void setReadExpectation(char* data, int len) {
        mToRead = data;
        mReadLength = len;
    }
public:
    int mPipeWrFd; // for writing
    const char* mToRead;
    int mReadLength;
    const char* mToSend;
    int mSendLength;
    int mSendPointer;
};

class RemoteAudioFakeTcpTest : public testing::Test {
protected:
    android::sp<RemoteAudio> mRemoteAudio;
    ClientSocketForTest mTestSocket;

protected:
    virtual void SetUp() {
        ASSERT_TRUE(U32_ENDIAN_SWAP(0x12345678) == 0x78563412);
        mRemoteAudio = new RemoteAudio(mTestSocket);
        ASSERT_TRUE(mRemoteAudio != NULL);
        ASSERT_TRUE(mRemoteAudio->init(1234));
    }

    virtual void TearDown() {
        mRemoteAudio->release();
        mRemoteAudio.clear();
    }

    void doDownload() {
        android::sp<Buffer> buffer = AudioSignalFactory::generateZeroSound(AudioHardware::E2BPS, 2,
                false);
        uint32_t prepareSend[] = {
                U32_ENDIAN_SWAP(AudioProtocol::ECmdDownload),
                U32_ENDIAN_SWAP(8),
                U32_ENDIAN_SWAP(0), //id
                U32_ENDIAN_SWAP(0)
        };
        uint32_t prepareReply[] = {
                U32_ENDIAN_SWAP((AudioProtocol::ECmdDownload & 0xffff) | 0x43210000),
                0,
                0
        };
        LOGD("reply 0x%x", prepareReply[0]);

        mTestSocket.setSendExpectation((char*)prepareSend, sizeof(prepareSend));
        // this is reply, but set expectation for reply first as it is sent after send
        mTestSocket.setReadExpectation((char*)prepareReply, sizeof(prepareReply));

        int id = -1;
        android::String8 name("1");
        ASSERT_TRUE(mRemoteAudio->downloadData(name, buffer, id));
        ASSERT_TRUE(id >= 0);
    }
};

TEST_F(RemoteAudioFakeTcpTest, InitTest) {
    // all done in SetUp
}

TEST_F(RemoteAudioFakeTcpTest, DownloadTest) {
    doDownload();
}

TEST_F(RemoteAudioFakeTcpTest, PlayTest) {
    doDownload();

    bool stereo = false;
    int id = 0;
    int samplingF = 44100;
    int mode = AudioHardware::EModeVoice | (stereo ? 0x80000000 : 0);
    int volume = 0;
    int repeat = 1;

    uint32_t prepareSend[] = {
            U32_ENDIAN_SWAP(AudioProtocol::ECmdStartPlayback),
            U32_ENDIAN_SWAP(20),
            U32_ENDIAN_SWAP(id), //id
            U32_ENDIAN_SWAP(samplingF),
            U32_ENDIAN_SWAP(mode),
            U32_ENDIAN_SWAP(volume),
            U32_ENDIAN_SWAP(repeat)
    };
    uint32_t prepareReply[] = {
            U32_ENDIAN_SWAP((AudioProtocol::ECmdStartPlayback & 0xffff) | 0x43210000),
            0,
            0
    };

    mTestSocket.setSendExpectation((char*)prepareSend, sizeof(prepareSend));
    // this is reply, but set expectation for reply first as it is sent after send
    mTestSocket.setReadExpectation((char*)prepareReply, sizeof(prepareReply));

    ASSERT_TRUE(mRemoteAudio->startPlayback(stereo, samplingF, mode, volume, id, repeat));
    ASSERT_TRUE(mRemoteAudio->waitForPlaybackCompletion());
}

TEST_F(RemoteAudioFakeTcpTest, PlayStopTest) {
    doDownload();

    bool stereo = false;
    int id = 0;
    int samplingF = 44100;
    int mode = AudioHardware::EModeVoice | (stereo ? 0x80000000 : 0);
    int volume = 0;
    int repeat = 1;

    uint32_t startPlaybackSend[] = {
            U32_ENDIAN_SWAP(AudioProtocol::ECmdStartPlayback),
            U32_ENDIAN_SWAP(20),
            U32_ENDIAN_SWAP(id),
            U32_ENDIAN_SWAP(samplingF),
            U32_ENDIAN_SWAP(mode),
            U32_ENDIAN_SWAP(volume),
            U32_ENDIAN_SWAP(repeat)
    };
    uint32_t startReply[] = {
            U32_ENDIAN_SWAP((AudioProtocol::ECmdStartPlayback & 0xffff) | 0x43210000),
            0,
            0
    };

    uint32_t stopPlaybackSend[] = {
            U32_ENDIAN_SWAP(AudioProtocol::ECmdStopPlayback),
            U32_ENDIAN_SWAP(0)
    };

    uint32_t stopReply[] = {
            U32_ENDIAN_SWAP((AudioProtocol::ECmdStopPlayback & 0xffff) | 0x43210000),
            0,
            0
    };

    mTestSocket.setSendExpectation((char*)startPlaybackSend, sizeof(startPlaybackSend));
    // this is reply, but set expectation for reply first as it is sent after send
    mTestSocket.setReadExpectation((char*)startReply, sizeof(startReply));

    ASSERT_TRUE(mRemoteAudio->startPlayback(stereo, samplingF, mode, volume, id, repeat));
    sleep(1);
    mTestSocket.setSendExpectation((char*)stopPlaybackSend, sizeof(stopPlaybackSend));
    // this is reply, but set expectation for reply first as it is sent after send
    mTestSocket.setReadExpectation((char*)stopReply, sizeof(stopReply));
    mRemoteAudio->stopPlayback();

    mTestSocket.setSendExpectation((char*)startPlaybackSend, sizeof(startPlaybackSend));
    // this is reply, but set expectation for reply first as it is sent after send
    mTestSocket.setReadExpectation((char*)startReply, sizeof(startReply));
    ASSERT_TRUE(mRemoteAudio->startPlayback(stereo, samplingF, mode, volume, id, repeat));
    sleep(1);
    mTestSocket.setSendExpectation((char*)stopPlaybackSend, sizeof(stopPlaybackSend));
    // this is reply, but set expectation for reply first as it is sent after send
    mTestSocket.setReadExpectation((char*)stopReply, sizeof(stopReply));
    mRemoteAudio->stopPlayback();

    mTestSocket.setSendExpectation((char*)startPlaybackSend, sizeof(startPlaybackSend));
    // this is reply, but set expectation for reply first as it is sent after send
    mTestSocket.setReadExpectation((char*)startReply, sizeof(startReply));
    ASSERT_TRUE(mRemoteAudio->startPlayback(stereo, samplingF, mode, volume, id, repeat));
    ASSERT_TRUE(mRemoteAudio->waitForPlaybackCompletion());
}

TEST_F(RemoteAudioFakeTcpTest, RecordingTest) {
    bool stereo = false;
    int id = 0;
    int samplingF = 44100;
    int mode = AudioHardware::EModeVoice | (stereo ? 0x80000000 : 0);
    int volume = 0;
    int noSamples = 44; // 1ms worth

    android::sp<Buffer> buffer(new Buffer(100, noSamples*2, false));

    uint32_t startSend[] = {
            U32_ENDIAN_SWAP(AudioProtocol::ECmdStartRecording),
            U32_ENDIAN_SWAP(16),
            U32_ENDIAN_SWAP(samplingF),
            U32_ENDIAN_SWAP(mode),
            U32_ENDIAN_SWAP(volume),
            U32_ENDIAN_SWAP(noSamples)
    };

    // 2bytes per sample, +2 for last samples rounded off
    uint32_t startReply[noSamples/2 + 2 + 3];
    memset(startReply, 0, sizeof(startReply));
    startReply[0] = U32_ENDIAN_SWAP((AudioProtocol::ECmdStartRecording & 0xffff) | 0x43210000);
    startReply[1] = 0;
    startReply[2] = U32_ENDIAN_SWAP(noSamples * 2);

    uint32_t stopSend[] = {
            U32_ENDIAN_SWAP(AudioProtocol::ECmdStopRecording),
            U32_ENDIAN_SWAP(0)
    };

    uint32_t stopReply[] = {
            U32_ENDIAN_SWAP((AudioProtocol::ECmdStopRecording & 0xffff) | 0x43210000),
            0,
            0
    };


    mTestSocket.setSendExpectation((char*)startSend, sizeof(startSend));
    // this is reply, but set expectation for reply first as it is sent after send
    mTestSocket.setReadExpectation((char*)startReply, 12 + noSamples*2);

    ASSERT_TRUE(mRemoteAudio->startRecording(stereo, samplingF, mode, volume, buffer));
    ASSERT_TRUE(mRemoteAudio->waitForRecordingCompletion());
    ASSERT_TRUE(buffer->amountHandled() == (size_t)(noSamples * 2));
    mTestSocket.setSendExpectation((char*)startSend, sizeof(startSend));
    // this is reply, but set expectation for reply first as it is sent after send
    mTestSocket.setReadExpectation((char*)startReply, 12 + noSamples*2);
    ASSERT_TRUE(mRemoteAudio->startRecording(stereo, samplingF, mode, volume, buffer));
    sleep(1);
    mTestSocket.setSendExpectation((char*)stopSend, sizeof(stopSend));
    // this is reply, but set expectation for reply first as it is sent after send
    mTestSocket.setReadExpectation((char*)stopReply, sizeof(stopReply));
    mRemoteAudio->stopRecording();
}

TEST_F(RemoteAudioFakeTcpTest, getDeviceInfoTest) {
    uint32_t prepareSend[] = {
            U32_ENDIAN_SWAP(AudioProtocol::ECmdGetDeviceInfo),
            U32_ENDIAN_SWAP(0)
    };
    uint32_t prepareReply[] = {
            U32_ENDIAN_SWAP((AudioProtocol::ECmdGetDeviceInfo & 0xffff) | 0x43210000),
            0,
            U32_ENDIAN_SWAP(4),
            U32_ENDIAN_SWAP(0x30313233)
    };

    mTestSocket.setSendExpectation((char*)prepareSend, sizeof(prepareSend));
    // this is reply, but set expectation for reply first as it is sent after send
    mTestSocket.setReadExpectation((char*)prepareReply, sizeof(prepareReply));

    android::String8 info;
    ASSERT_TRUE(mRemoteAudio->getDeviceInfo(info));
    ASSERT_TRUE(info == "0123");
}
