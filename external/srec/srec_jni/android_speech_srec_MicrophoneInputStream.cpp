/*---------------------------------------------------------------------------*
 *  android_speech_srec_MicrophoneInputStream.cpp                            *
 *                                                                           *
 *  Copyright 2007 Nuance Communciations, Inc.                               *
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the 'License');          *
 *  you may not use this file except in compliance with the License.         *
 *                                                                           *
 *  You may obtain a copy of the License at                                  *
 *      http://www.apache.org/licenses/LICENSE-2.0                           *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an 'AS IS' BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *---------------------------------------------------------------------------*/


#include <string.h>
#include <stdio.h>
#include <stdlib.h>

#define LOG_TAG "srec_jni"
#include <utils/Log.h>

#include <media/AudioRecord.h>
#include <media/mediarecorder.h>

#include <system/audio.h>

#include <jni.h>

using namespace android;



//
// helper function to throw an exception
//
static void throwException(JNIEnv *env, const char* ex, const char* fmt, int data) {
    if (jclass cls = env->FindClass(ex)) {
        char msg[1000];
        sprintf(msg, fmt, data);
        env->ThrowNew(cls, msg);
        env->DeleteLocalRef(cls);
    }
}


///////////////////////////////////////////////////////////////////////////////
// MicrophoneInputStream JNI implememtations
///////////////////////////////////////////////////////////////////////////////

// Java uses an int to hold a raw pointer, which is already ugly.
// But we need to hold an sp<>, which is of unknown size.
// So we wrap the sp<> within a class, and give Java the int version of a pointer to this class.
class AudioRecordWrapper {
public:
    AudioRecordWrapper(AudioRecord *audioRecord) : mAudioRecord(audioRecord) { }
    ~AudioRecordWrapper() { }
    AudioRecord* get() const { return mAudioRecord.get(); }
private:
    const sp<AudioRecord> mAudioRecord;
};

static JNIEXPORT jint JNICALL Java_android_speech_srec_Recognizer_AudioRecordNew
        (JNIEnv *env, jclass clazz, jint sampleRate, jint fifoFrames) {

    AudioRecordWrapper *ar = new AudioRecordWrapper(new AudioRecord(
            AUDIO_SOURCE_VOICE_RECOGNITION, sampleRate,
            AUDIO_FORMAT_PCM_16_BIT, AUDIO_CHANNEL_IN_MONO,
            fifoFrames));
    status_t s = ar->get()->initCheck();
    if (s != NO_ERROR) {
        delete ar;
        ar = NULL;
        ALOGE("initCheck error %d ", s);
    }
    return (int)ar;
}

static JNIEXPORT int JNICALL Java_android_speech_srec_Recognizer_AudioRecordStart
        (JNIEnv *env, jclass clazz, jint audioRecord) {
    return (int)(((AudioRecordWrapper*)audioRecord)->get()->start());
}

static JNIEXPORT jint JNICALL Java_android_speech_srec_Recognizer_AudioRecordRead
        (JNIEnv *env, jclass clazz, jint audioRecord, jbyteArray array, jint offset, jint length) {
    jbyte buffer[4096];
    if (length > (int)sizeof(buffer)) length = sizeof(buffer);
    length = ((AudioRecordWrapper*)audioRecord)->get()->read(buffer, length);
    if (length < 0) {
        throwException(env, "java/io/IOException", "AudioRecord::read failed %d", length);
        return -1;
    }
    env->SetByteArrayRegion(array, offset, length, buffer);
    return length;
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_AudioRecordStop
        (JNIEnv *env, jclass clazz, jint audioRecord) {
    ((AudioRecordWrapper*)audioRecord)->get()->stop();
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_AudioRecordDelete
        (JNIEnv *env, jclass clazz, jint audioRecord) {
    delete (AudioRecordWrapper*)audioRecord;
}


/*
 * Table of methods associated with a single class.
 */
static JNINativeMethod gMethods[] = {
    /* name, signature, funcPtr */
    {"AudioRecordNew",    "(II)I",    (void*)Java_android_speech_srec_Recognizer_AudioRecordNew},
    {"AudioRecordStart",  "(I)I",     (void*)Java_android_speech_srec_Recognizer_AudioRecordStart},
    {"AudioRecordRead",   "(I[BII)I", (void*)Java_android_speech_srec_Recognizer_AudioRecordRead},
    {"AudioRecordStop",   "(I)V",     (void*)Java_android_speech_srec_Recognizer_AudioRecordStop},
    {"AudioRecordDelete", "(I)V",     (void*)Java_android_speech_srec_Recognizer_AudioRecordDelete},
};

/*
 * Set some test stuff up.
 *
 * Returns the JNI version on success, -1 on failure.
 */
jint register_android_speech_srec_MicrophoneInputStream(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jclass clazz = NULL;
    const char* className = "android/speech/srec/MicrophoneInputStream";

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("ERROR: GetEnv failed\n");
        return -1;
    }
    assert(env != NULL);

    clazz = env->FindClass(className);
    if (clazz == NULL) {
        ALOGE("Native registration unable to find class '%s'\n", className);
        return -1;
    }
    if (env->RegisterNatives(clazz, gMethods,
            sizeof(gMethods) / sizeof(gMethods[0])) < 0) {
        ALOGE("RegisterNatives failed for '%s'\n", className);
        return -1;
    }

    /* success -- return valid version number */
    return JNI_VERSION_1_4;;
}
