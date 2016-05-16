/*---------------------------------------------------------------------------*
 *  android_speech_srec_Recognizer.cpp                                                              *
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
#include <jni.h>

#define LOG_TAG "srec_jni"
#include "utils/Log.h"

#include "passert.h"
#include "ESR_CommandLine.h"
#include "ESR_Session.h"
#include "LCHAR.h"
#include "PFile.h"
#include "PFileSystem.h"
#include "PANSIFileSystem.h"
//#include "PMemoryFileSystem.h"
#include "plog.h"
#include "pmemory.h"
#include "ptypes.h"
#include "SR_Grammar.h"
#include "SR_Recognizer.h"
#include "SR_RecognizerImpl.h"
#include "SR_RecognizerResult.h"
#include "SR_Session.h"
#include "SR_Vocabulary.h"
#include "SR_AcousticState.h"
#include "SR_Nametag.h"
#include "PStackSize.h"



//
// helper function to throw an exception
//
static void JNU_ThrowByName(JNIEnv *env, const char* name, const char* msg) {
    jclass cls = env->FindClass(name);
    if (cls != NULL) {
        env->ThrowNew(cls, msg);
    }
    env->DeleteLocalRef(cls);
}

static void unimplemented(JNIEnv* env) {
    JNU_ThrowByName(env, "java/lang/IllegalStateException", "unimplemented!!!");
}

static void checkEsrError(JNIEnv* env, ESR_ReturnCode esr_status) {
    if (esr_status != ESR_SUCCESS) {
        JNU_ThrowByName(env, "java/lang/IllegalStateException", ESR_rc2str(esr_status));
    }
}


///////////////////////////////////////////////////////////////////////////
// PMem JNI implementations
///////////////////////////////////////////////////////////////////////////

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_PMemInit
        (JNIEnv *env, jclass clazz) {
    checkEsrError(env, PMemInit());
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_PMemShutdown
        (JNIEnv *env, jclass clazz) {
    checkEsrError(env, PMemShutdown());
}


//////////////////////////////////////////////////////////////////////////
// SR_Session JNI implementations
//////////////////////////////////////////////////////////////////////////

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1SessionCreate
        (JNIEnv *env, jclass clazz, jstring fileName) {
    // TODO: check for fileName NPE
    const char* fn = env->GetStringUTFChars(fileName, 0);
    checkEsrError(env, SR_SessionCreate(fn));  // TODO: can I post this before freeing the string?
    env->ReleaseStringUTFChars(fileName, fn);
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1SessionDestroy
        (JNIEnv *env, jclass clazz) {
    checkEsrError(env, SR_SessionDestroy());
}


//////////////////////////////////////////////////////////////////////////
// SR_Recognizer JNI implementations
//////////////////////////////////////////////////////////////////////////

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerStart
        (JNIEnv *env, jclass clazz, jint recognizer) {
    checkEsrError(env, SR_RecognizerStart((SR_Recognizer*)recognizer));
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerStop
        (JNIEnv *env, jclass clazz, jint recognizer) {
    checkEsrError(env, SR_RecognizerStop((SR_Recognizer*)recognizer));
}

static JNIEXPORT jint JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerCreate
        (JNIEnv *env, jclass clazz) {
    SR_Recognizer* recognizer = NULL;
    // TODO: do we need to clean up the recognizer if this fails?
    checkEsrError(env, SR_RecognizerCreate(&recognizer));
    return (jint)recognizer;
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerDestroy
        (JNIEnv *env, jclass clazz, jint recognizer) {
    checkEsrError(env, SR_RecognizerDestroy((SR_Recognizer*)recognizer));
    return;
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerSetup
        (JNIEnv *env, jclass clazz, jint recognizer) {
    checkEsrError(env, SR_RecognizerSetup((SR_Recognizer*)recognizer));
    return;
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerUnsetup
        (JNIEnv *env, jclass clazz, jint recognizer) {
    checkEsrError(env, SR_RecognizerUnsetup((SR_Recognizer*)recognizer));
    return;
}

static JNIEXPORT jboolean JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerIsSetup
        (JNIEnv *env, jclass clazz, jint recognizer) {
    ESR_BOOL rtn = ESR_FALSE;
    checkEsrError(env, SR_RecognizerIsSetup((SR_Recognizer*)recognizer, &rtn));
    return rtn;
}

static JNIEXPORT jstring JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerGetParameter
  (JNIEnv *env, jclass clazz, jint recognizer, jstring key) {
    unimplemented(env);
    return 0;
}

static JNIEXPORT jint JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerGetSize_1tParameter
  (JNIEnv *env, jclass clazz, jint recognizer, jstring key) {
    unimplemented(env);
    return 0;
}

static JNIEXPORT jboolean JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerGetBoolParameter
  (JNIEnv *env, jclass clazz, jint recognizer, jstring key) {
    unimplemented(env);
    return JNI_FALSE;
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerSetParameter
  (JNIEnv *env, jclass clazz, jint recognizer, jstring key, jstring value) {
    unimplemented(env);
    return;
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerSetSize_1tParameter
  (JNIEnv *env, jclass clazz, jint recognizer, jstring key, jint value) {
    unimplemented(env);
    return;
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerSetBoolParameter
  (JNIEnv *env, jclass clazz, jint recognizer, jstring key , jboolean value) {
    unimplemented(env);
    return;
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerSetupRule
        (JNIEnv *env, jclass clazz, jint recognizer, jint grammar, jstring ruleName) {
    const char* rn = env->GetStringUTFChars(ruleName, 0);
    checkEsrError(env, SR_RecognizerSetupRule((SR_Recognizer*)recognizer, (SR_Grammar*)grammar, rn));
    env->ReleaseStringUTFChars(ruleName, rn);
}

static JNIEXPORT jboolean JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerHasSetupRules
        (JNIEnv *env, jclass clazz, jint recognizer) {
    ESR_BOOL rtn = ESR_FALSE;
    checkEsrError(env, SR_RecognizerHasSetupRules((SR_Recognizer*)recognizer, &rtn));
    return rtn;
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerActivateRule
        (JNIEnv *env, jclass clazz, jint recognizer, jint grammar, jstring ruleName, jint weight) {
    const char* rn = env->GetStringUTFChars(ruleName, 0);
    checkEsrError(env, SR_RecognizerActivateRule((SR_Recognizer*)recognizer, (SR_Grammar*)grammar, rn, weight));
    env->ReleaseStringUTFChars(ruleName, rn);
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerDeactivateRule
        (JNIEnv *env, jclass clazz, jint recognizer, jint grammar, jstring ruleName) {
    const char* rn = env->GetStringUTFChars(ruleName, 0);
    checkEsrError(env, SR_RecognizerDeactivateRule((SR_Recognizer*)recognizer, (SR_Grammar*)grammar, rn));
    env->ReleaseStringUTFChars(ruleName, rn);
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerDeactivateAllRules
        (JNIEnv *env, jclass clazz, jint recognizer) {
    checkEsrError(env, SR_RecognizerDeactivateAllRules((SR_Recognizer*)recognizer));
}

static JNIEXPORT jboolean JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerIsActiveRule
        (JNIEnv *env, jclass clazz, jint recognizer, jint grammar, jstring ruleName) {
    ESR_BOOL rtn = ESR_FALSE;
    const char* rn = env->GetStringUTFChars(ruleName, 0);
    checkEsrError(env, SR_RecognizerIsActiveRule((SR_Recognizer*)recognizer, (SR_Grammar*)grammar, rn, &rtn));
    env->ReleaseStringUTFChars(ruleName, rn);
    return rtn;
}

static JNIEXPORT jboolean JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerCheckGrammarConsistency
        (JNIEnv *env, jclass clazz, jint recognizer, jint grammar) {
    ESR_BOOL rtn = ESR_FALSE;
    checkEsrError(env, SR_RecognizerCheckGrammarConsistency((SR_Recognizer*)recognizer, (SR_Grammar*)grammar, &rtn));
    return rtn;
}

static JNIEXPORT jint JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerPutAudio
        (JNIEnv *env, jclass clazz, jint recognizer, jbyteArray audio, jint offset, jint length, jboolean isLast) {
    // set the length
    jbyte buffer[1024];
    if (length > (int)sizeof(buffer)) length = sizeof(buffer);
    size_t samples = length / sizeof(asr_int16_t);
    length = samples * sizeof(asr_int16_t);

    // fetch data from java
    env->GetByteArrayRegion(audio, offset, length, buffer);

    // put the samples into the recognizer
    checkEsrError(env, SR_RecognizerPutAudio((SR_Recognizer*)recognizer,
            (asr_int16_t*)buffer, &samples, isLast ? ESR_TRUE : ESR_FALSE));
    if (samples != length / sizeof(asr_int16_t)) {
        checkEsrError(env, ESR_READ_ERROR);
        return 0;
    }

    return samples * sizeof(asr_int16_t);
}

static JNIEXPORT jint JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerAdvance
        (JNIEnv *env, jclass clazz, jint recognizer) {
    SR_RecognizerStatus status = SR_RECOGNIZER_EVENT_INVALID;
    SR_RecognizerResultType type = SR_RECOGNIZER_RESULT_TYPE_INVALID;
    SR_RecognizerResult* recognizerResult = NULL;
    checkEsrError(env, SR_RecognizerAdvance((SR_Recognizer*)recognizer, &status, &type, &recognizerResult));
    return status;
}

static JNIEXPORT jboolean JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerIsSignalClipping
        (JNIEnv *env, jclass clazz, jint recognizer) {
    ESR_BOOL rtn = ESR_FALSE;
    checkEsrError(env, SR_RecognizerIsSignalClipping((SR_Recognizer*)recognizer, &rtn));
    return rtn;
}

static JNIEXPORT jboolean JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerIsSignalDCOffset
        (JNIEnv *env, jclass clazz, jint recognizer) {
    ESR_BOOL rtn = ESR_FALSE;
    checkEsrError(env, SR_RecognizerIsSignalDCOffset((SR_Recognizer*)recognizer, &rtn));
    return rtn;
}

static JNIEXPORT jboolean JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerIsSignalNoisy
        (JNIEnv *env, jclass clazz, jint recognizer) {
    ESR_BOOL rtn = ESR_FALSE;
    checkEsrError(env, SR_RecognizerIsSignalNoisy((SR_Recognizer*)recognizer, &rtn));
    return rtn;
}

static JNIEXPORT jboolean JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerIsSignalTooQuiet
        (JNIEnv *env, jclass clazz, jint recognizer) {
    ESR_BOOL rtn = ESR_FALSE;
    checkEsrError(env, SR_RecognizerIsSignalTooQuiet((SR_Recognizer*)recognizer, &rtn));
    return rtn;
}

static JNIEXPORT jboolean JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerIsSignalTooFewSamples
        (JNIEnv *env, jclass clazz, jint recognizer) {
    ESR_BOOL rtn = ESR_FALSE;
    checkEsrError(env, SR_RecognizerIsSignalTooFewSamples((SR_Recognizer*)recognizer, &rtn));
    return rtn;
}

static JNIEXPORT jboolean JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerIsSignalTooManySamples
        (JNIEnv *env, jclass clazz, jint recognizer) {
    ESR_BOOL rtn = ESR_FALSE;
    checkEsrError(env, SR_RecognizerIsSignalTooManySamples((SR_Recognizer*)recognizer, &rtn));
    return rtn;
}


//////////////////////////////////////////////////////////////////////////
// SR_AcousticState JNI implementations
//////////////////////////////////////////////////////////////////////////

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1AcousticStateReset
        (JNIEnv *env, jclass clazz, jint recognizer) {
    checkEsrError(env, SR_AcousticStateReset((SR_Recognizer*)recognizer));
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1AcousticStateSet
        (JNIEnv *env, jclass clazz, jint recognizer, jstring state) {
    const char* st = env->GetStringUTFChars(state, 0);
    checkEsrError(env, SR_AcousticStateSet((SR_Recognizer*)recognizer, st));
    env->ReleaseStringUTFChars(state, st);
}

static JNIEXPORT jstring JNICALL Java_android_speech_srec_Recognizer_SR_1AcousticStateGet
        (JNIEnv *env, jclass clazz, jint recognizer) {
    char rtn[1000];
    size_t rtnLength = sizeof(rtn) - 1;
    ESR_ReturnCode esr_status = SR_AcousticStateGet((SR_Recognizer*)recognizer, rtn, &rtnLength);
    if (esr_status) {
        checkEsrError(env, esr_status);
        return NULL;
    }
    rtn[rtnLength] = 0;
    return env->NewStringUTF(rtn);
}


//////////////////////////////////////////////////////////////////////////
// SR_Grammar JNI implementations
//////////////////////////////////////////////////////////////////////////

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1GrammarCompile
        (JNIEnv *env, jclass clazz, jint grammar) {
    checkEsrError(env, SR_GrammarCompile((SR_Grammar*)grammar));
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1GrammarAddWordToSlot
        (JNIEnv *env, jclass clazz, jint grammar, jstring slot, jstring word, jstring pronunciation, jint weight, jstring tag) {
    const char* sl = 0;
    const char* wo = 0;
    const char* pr = 0;
    const char* ta = 0;
    if ((sl = env->GetStringUTFChars(slot, 0)) &&
            (wo = env->GetStringUTFChars(word, 0)) &&
            (!pronunciation || (pr = env->GetStringUTFChars(pronunciation, 0))) &&
            (ta = env->GetStringUTFChars(tag, 0))) {
        checkEsrError(env, SR_GrammarAddWordToSlot((SR_Grammar*)grammar, sl, wo, pr, weight, ta));
    }
    if (ta) env->ReleaseStringUTFChars(tag, ta);
    if (pr) env->ReleaseStringUTFChars(pronunciation, pr);
    if (wo) env->ReleaseStringUTFChars(word, wo);
    if (sl) env->ReleaseStringUTFChars(slot, sl);
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1GrammarResetAllSlots
        (JNIEnv *env, jclass clazz, jint grammar) {
    checkEsrError(env, SR_GrammarResetAllSlots((SR_Grammar*)grammar));
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1GrammarSetupVocabulary
        (JNIEnv *env, jclass clazz, jint grammar, jint vocabulary) {
    checkEsrError(env, SR_GrammarSetupVocabulary((SR_Grammar*)grammar, (SR_Vocabulary*)vocabulary));
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1GrammarSetupRecognizer
        (JNIEnv *env, jclass clazz, jint grammar, jint recognizer) {
    checkEsrError(env, SR_GrammarSetupRecognizer((SR_Grammar*)grammar, (SR_Recognizer*)recognizer));
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1GrammarUnsetupRecognizer
        (JNIEnv *env, jclass clazz, jint grammar) {
    checkEsrError(env, SR_GrammarUnsetupRecognizer((SR_Grammar*)grammar));
}

static JNIEXPORT jint JNICALL Java_android_speech_srec_Recognizer_SR_1GrammarCreate
        (JNIEnv *env, jclass clazz) {
    SR_Grammar* grammar = 0;
    checkEsrError(env, SR_GrammarCreate(&grammar));
    return (jint)grammar;
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1GrammarDestroy
        (JNIEnv *env, jclass clazz, jint grammar) {
    checkEsrError(env, SR_GrammarDestroy((SR_Grammar*)grammar));
}

static JNIEXPORT jint JNICALL Java_android_speech_srec_Recognizer_SR_1GrammarLoad
        (JNIEnv *env, jclass clazz, jstring fileName) {
    // TODO: check for fileName NPE
    SR_Grammar* grammar = 0;
    const char* fn = env->GetStringUTFChars(fileName, 0);
    checkEsrError(env, SR_GrammarLoad(fn, &grammar));
    env->ReleaseStringUTFChars(fileName, fn);
    return (jint)grammar;
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1GrammarSave
        (JNIEnv *env, jclass clazz, jint grammar, jstring fileName) {
    const char* fn = env->GetStringUTFChars(fileName, 0);
    checkEsrError(env, SR_GrammarSave((SR_Grammar*)grammar, fn));
    env->ReleaseStringUTFChars(fileName, fn);
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1GrammarAllowOnly
        (JNIEnv *env, jclass clazz, jint grammar, jstring transcription) {
    const char* tr = env->GetStringUTFChars(transcription, 0);
    checkEsrError(env, SR_GrammarSave((SR_Grammar*)grammar, tr));
    env->ReleaseStringUTFChars(transcription, tr);
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1GrammarAllowAll
        (JNIEnv *env, jclass clazz, jint grammar) {
    checkEsrError(env, SR_GrammarAllowAll((SR_Grammar*)grammar));
}


//////////////////////////////////////////////////////////////////////////
// SR_Vocabulary JNI implementations
//////////////////////////////////////////////////////////////////////////

static JNIEXPORT jint JNICALL Java_android_speech_srec_Recognizer_SR_1VocabularyLoad
        (JNIEnv *env, jclass clazz) {
    char filename[P_PATH_MAX];
    size_t flen = sizeof(filename) - 1;
    checkEsrError(env, ESR_SessionGetLCHAR ( L("cmdline.vocabulary"), filename, &flen ));
    filename[sizeof(filename) - 1] = 0;
    // TODO: do we need to clean up the recognizer if this fails?
    SR_Vocabulary* vocabulary = NULL;
    checkEsrError(env, SR_VocabularyLoad(filename, &vocabulary));
    return (jint)vocabulary;
}

static JNIEXPORT void JNICALL Java_android_speech_srec_Recognizer_SR_1VocabularyDestroy
        (JNIEnv *env, jclass clazz, jint vocabulary) {
    // TODO: do we need to clean up the recognizer if this fails?
    checkEsrError(env, SR_VocabularyDestroy((SR_Vocabulary*)vocabulary));
}

static JNIEXPORT jstring JNICALL Java_android_speech_srec_Recognizer_SR_1VocabularyGetPronunciation
        (JNIEnv *env, jclass clazz, jint vocabulary, jstring word) {
    char rtn[1000];
    size_t rtnLength = sizeof(rtn) - 1;
    const char* wo = env->GetStringUTFChars(word, 0);
    ESR_ReturnCode esr_status = SR_VocabularyGetPronunciation((SR_Vocabulary*)vocabulary, wo, rtn, &rtnLength);
    env->ReleaseStringUTFChars(word, wo);
    if (esr_status) {
        checkEsrError(env, esr_status);
        return NULL;
    }
    rtn[rtnLength] = 0;
    return env->NewStringUTF(rtn);
}


//////////////////////////////////////////////////////////////////////////
// SR_RecognizerResult JNI implementations
//////////////////////////////////////////////////////////////////////////

static JNIEXPORT jbyteArray JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerResultGetWaveform
        (JNIEnv *env, jclass clazz, jint recognizer) {
    unimplemented(env);
    return 0;
}

static JNIEXPORT jint JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerResultGetSize
        (JNIEnv *env, jclass clazz, jint recognizer) {
    size_t size = 0;
    checkEsrError(env, SR_RecognizerResultGetSize(((SR_RecognizerImpl*)recognizer)->result, &size));
    return size;
}

static JNIEXPORT jint JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerResultGetKeyCount
        (JNIEnv *env, jclass clazz, jint recognizer, jint nbest) {
    size_t size = 0;
    checkEsrError(env, SR_RecognizerResultGetKeyCount(((SR_RecognizerImpl*)recognizer)->result, nbest, &size));
    return size;
}

static JNIEXPORT jobjectArray JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerResultGetKeyList
        (JNIEnv *env, jclass clazz, jint recognizer, jint nbest) {
    // fetch list
    LCHAR* list[200];
    size_t listSize = sizeof(list) / sizeof(list[0]);
    ESR_ReturnCode esr_status = SR_RecognizerResultGetKeyList(((SR_RecognizerImpl*)recognizer)->result, nbest, list, &listSize);
    if (esr_status) {
        checkEsrError(env, esr_status);
        return NULL;
    }

    // create String[] of keys
    jclass stringClass = env->FindClass("java/lang/String");
    if (!stringClass) return NULL;
    jobjectArray array = env->NewObjectArray(listSize, stringClass, NULL);
    if (!array) return NULL;

    // fill it
    for (size_t i = 0; i < listSize; i++) {
        // generate new String for key
        jstring key = env->NewStringUTF(list[i]);
        if (!key) return NULL;
        // set the array
        env->SetObjectArrayElement(array, i, key);
        env->DeleteLocalRef(key);
    }

    return array;
}

static JNIEXPORT jstring JNICALL Java_android_speech_srec_Recognizer_SR_1RecognizerResultGetValue
        (JNIEnv *env, jclass clazz, jint recognizer, jint nbest, jstring key) {
    char rtn[1000];
    size_t rtnLength = sizeof(rtn) - 1;
    const char* ke = env->GetStringUTFChars(key, 0);
    ESR_ReturnCode esr_status = SR_RecognizerResultGetValue(((SR_RecognizerImpl*)recognizer)->result, nbest, ke, rtn, &rtnLength);
    env->ReleaseStringUTFChars(key, ke);
    if (esr_status) {
        checkEsrError(env, esr_status);
        return NULL;
    }
    rtn[rtnLength] = 0;
    return env->NewStringUTF(rtn);
}


/*
 * Table of methods associated with a single class.
 */
static JNINativeMethod gMethods[] = {
    /* name, signature, funcPtr */
    // PMem
    {"PMemInit",                           "()V",                     (void*)Java_android_speech_srec_Recognizer_PMemInit},
    {"PMemShutdown",                       "()V",                     (void*)Java_android_speech_srec_Recognizer_PMemShutdown},
    // SR_Session
    {"SR_SessionCreate",                   "(Ljava/lang/String;)V",   (void*)Java_android_speech_srec_Recognizer_SR_1SessionCreate},
    {"SR_SessionDestroy",                  "()V",                     (void*)Java_android_speech_srec_Recognizer_SR_1SessionDestroy},
    // SR_Recognizer
    {"SR_RecognizerStart",                 "(I)V",                    (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerStart},
    {"SR_RecognizerStop",                  "(I)V",                    (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerStop},
    {"SR_RecognizerCreate",                "()I",                     (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerCreate},
    {"SR_RecognizerDestroy",               "(I)V",                    (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerDestroy},
    {"SR_RecognizerSetup",                 "(I)V",                    (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerSetup},
    {"SR_RecognizerUnsetup",               "(I)V",                    (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerUnsetup},
    {"SR_RecognizerIsSetup",               "(I)Z",                    (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerIsSetup},
    {"SR_RecognizerGetParameter",          "(ILjava/lang/String;)Ljava/lang/String;", (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerGetParameter},
    {"SR_RecognizerGetSize_tParameter",    "(ILjava/lang/String;)I",  (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerGetSize_1tParameter},
    {"SR_RecognizerGetBoolParameter",      "(ILjava/lang/String;)Z",  (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerGetBoolParameter},
    {"SR_RecognizerSetParameter",          "(ILjava/lang/String;Ljava/lang/String;)V", (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerSetParameter},
    {"SR_RecognizerSetSize_tParameter",    "(ILjava/lang/String;I)V", (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerSetSize_1tParameter},
    {"SR_RecognizerSetBoolParameter",      "(ILjava/lang/String;Z)V", (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerSetBoolParameter},
    {"SR_RecognizerSetupRule",             "(IILjava/lang/String;)V", (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerSetupRule},
    {"SR_RecognizerHasSetupRules",         "(I)Z",                    (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerHasSetupRules},
    {"SR_RecognizerActivateRule",          "(IILjava/lang/String;I)V", (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerActivateRule},
    {"SR_RecognizerDeactivateRule",        "(IILjava/lang/String;)V", (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerDeactivateRule},
    {"SR_RecognizerDeactivateAllRules",    "(I)V",                    (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerDeactivateAllRules},
    {"SR_RecognizerIsActiveRule",          "(IILjava/lang/String;)Z", (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerIsActiveRule},
    {"SR_RecognizerCheckGrammarConsistency", "(II)Z",                 (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerCheckGrammarConsistency},
    {"SR_RecognizerPutAudio",              "(I[BIIZ)I",               (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerPutAudio},
    {"SR_RecognizerAdvance",               "(I)I",                    (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerAdvance},
    {"SR_RecognizerIsSignalClipping",      "(I)Z",                    (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerIsSignalClipping},
    {"SR_RecognizerIsSignalDCOffset",      "(I)Z",                    (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerIsSignalDCOffset},
    {"SR_RecognizerIsSignalNoisy",         "(I)Z",                    (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerIsSignalNoisy},
    {"SR_RecognizerIsSignalTooQuiet",      "(I)Z",                    (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerIsSignalTooQuiet},
    {"SR_RecognizerIsSignalTooFewSamples", "(I)Z",                    (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerIsSignalTooFewSamples},
    {"SR_RecognizerIsSignalTooManySamples", "(I)Z",                   (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerIsSignalTooManySamples},
    // SR_AcousticState
    {"SR_AcousticStateReset",               "(I)V",                   (void*)Java_android_speech_srec_Recognizer_SR_1AcousticStateReset},
    {"SR_AcousticStateSet",                 "(ILjava/lang/String;)V", (void*)Java_android_speech_srec_Recognizer_SR_1AcousticStateSet},
    {"SR_AcousticStateGet",                 "(I)Ljava/lang/String;",  (void*)Java_android_speech_srec_Recognizer_SR_1AcousticStateGet},
    // SR_Grammar
    {"SR_GrammarCompile",                  "(I)V",                    (void*)Java_android_speech_srec_Recognizer_SR_1GrammarCompile},
    {"SR_GrammarAddWordToSlot",            "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V", (void*)Java_android_speech_srec_Recognizer_SR_1GrammarAddWordToSlot},
    {"SR_GrammarResetAllSlots",            "(I)V",                    (void*)Java_android_speech_srec_Recognizer_SR_1GrammarResetAllSlots},
    {"SR_GrammarSetupVocabulary",          "(II)V",                   (void*)Java_android_speech_srec_Recognizer_SR_1GrammarSetupVocabulary},
    {"SR_GrammarSetupRecognizer",          "(II)V",                   (void*)Java_android_speech_srec_Recognizer_SR_1GrammarSetupRecognizer},
    {"SR_GrammarUnsetupRecognizer",        "(I)V",                    (void*)Java_android_speech_srec_Recognizer_SR_1GrammarUnsetupRecognizer},
    {"SR_GrammarCreate",                   "()I",                     (void*)Java_android_speech_srec_Recognizer_SR_1GrammarCreate},
    {"SR_GrammarDestroy",                  "(I)V",                    (void*)Java_android_speech_srec_Recognizer_SR_1GrammarDestroy},
    {"SR_GrammarLoad",                     "(Ljava/lang/String;)I",   (void*)Java_android_speech_srec_Recognizer_SR_1GrammarLoad},
    {"SR_GrammarSave",                     "(ILjava/lang/String;)V",  (void*)Java_android_speech_srec_Recognizer_SR_1GrammarSave},
    {"SR_GrammarAllowOnly",                "(ILjava/lang/String;)V",  (void*)Java_android_speech_srec_Recognizer_SR_1GrammarAllowOnly},
    {"SR_GrammarAllowAll",                 "(I)V",                    (void*)Java_android_speech_srec_Recognizer_SR_1GrammarAllowAll},
    // SR_Vocabulary
    {"SR_VocabularyLoad",                  "()I",                     (void*)Java_android_speech_srec_Recognizer_SR_1VocabularyLoad},
    {"SR_VocabularyDestroy",               "(I)V",                    (void*)Java_android_speech_srec_Recognizer_SR_1VocabularyDestroy},
    {"SR_VocabularyGetPronunciation",      "(ILjava/lang/String;)Ljava/lang/String;",  (void*)Java_android_speech_srec_Recognizer_SR_1VocabularyGetPronunciation},
    // SR_RecognizerResult
    {"SR_RecognizerResultGetWaveform",     "(I)[B",                   (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerResultGetWaveform},
    {"SR_RecognizerResultGetSize",         "(I)I",                    (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerResultGetSize},
    {"SR_RecognizerResultGetKeyCount",     "(II)I",                   (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerResultGetKeyCount},
    {"SR_RecognizerResultGetKeyList",      "(II)[Ljava/lang/String;", (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerResultGetKeyList},
    {"SR_RecognizerResultGetValue",        "(IILjava/lang/String;)Ljava/lang/String;", (void*)Java_android_speech_srec_Recognizer_SR_1RecognizerResultGetValue},
};

/*
 * Returns the JNI version on success, -1 on failure.
 */
jint register_android_speech_srec_Recognizer(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jclass clazz = NULL;
    const char* className = "android/speech/srec/Recognizer";

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
    return JNI_VERSION_1_4;
}


extern jint register_android_speech_srec_MicrophoneInputStream(JavaVM* vm, void* reserved);

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    if (register_android_speech_srec_MicrophoneInputStream(vm, reserved) != JNI_VERSION_1_4 ||
        register_android_speech_srec_Recognizer(vm, reserved) != JNI_VERSION_1_4) return -1;
    return JNI_VERSION_1_4;
}
