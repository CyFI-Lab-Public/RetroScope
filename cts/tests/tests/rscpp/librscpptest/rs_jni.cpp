/*
 * Copyright (C) 2013 The Android Open Source Project
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

#include <jni.h>
#include <android/log.h>

#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#include <RenderScript.h>

#define  LOG_TAG    "rscpptest"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

using namespace android::RSC;

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSInitTest_initTest(JNIEnv * env,
                                                                               jclass obj)
{
    bool r = true;
    for (int i = 0; i < 1000; i++) {
        sp<RS> rs = new RS();
        r &= rs->init();
        LOGE("Native iteration %i, returned %i", i, (int)r);
    }
    return r;
}

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSBlurTest_blurTest(JNIEnv * env,
                                                                                 jclass obj,
                                                                                 jint X,
                                                                                 jint Y,
                                                                                 jbyteArray inputByteArray,
                                                                                 jbyteArray outputByteArray,
                                                                                 jboolean singleChannel)
{
    jbyte * input = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray, 0);
    jbyte * output = (jbyte *) env->GetPrimitiveArrayCritical(outputByteArray, 0);

    sp<RS> rs = new RS();
    rs->init();

    sp<const Element> e;
    if (singleChannel) {
        e = Element::A_8(rs);
    } else {
        e = Element::RGBA_8888(rs);
    }

    sp<Allocation> inputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<Allocation> outputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<ScriptIntrinsicBlur> blur = ScriptIntrinsicBlur::create(rs, e);

    inputAlloc->copy2DRangeFrom(0, 0, X, Y, input);

    blur->setRadius(15);
    blur->setInput(inputAlloc);
    blur->forEach(outputAlloc);
    outputAlloc->copy2DRangeTo(0, 0, X, Y, output);

    env->ReleasePrimitiveArrayCritical(inputByteArray, input, 0);
    env->ReleasePrimitiveArrayCritical(outputByteArray, output, 0);
    return (rs->getError() == RS_SUCCESS);

}

extern "C" JNIEXPORT jboolean JNICALL
Java_android_cts_rscpp_RSConvolveTest_convolveTest(JNIEnv * env, jclass obj, jint X,
                                                   jint Y, jbyteArray inputByteArray,
                                                   jbyteArray outputByteArray,
                                                   jfloatArray coeffArray,
                                                   jboolean is3x3)
{
    jfloat * coeffs = env->GetFloatArrayElements(coeffArray, NULL);
    jbyte * input = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray, 0);
    jbyte * output = (jbyte *) env->GetPrimitiveArrayCritical(outputByteArray, 0);


    sp<RS> rs = new RS();
    rs->init();

    sp<const Element> e = Element::A_8(rs);

    sp<Allocation> inputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<Allocation> outputAlloc = Allocation::createSized2D(rs, e, X, Y);

    inputAlloc->copy2DRangeFrom(0, 0, X, Y, input);


    if (is3x3) {
        sp<ScriptIntrinsicConvolve3x3> convolve = ScriptIntrinsicConvolve3x3::create(rs, e);
        convolve->setInput(inputAlloc);
        convolve->setCoefficients(coeffs);
        convolve->forEach(outputAlloc);
    } else {
        sp<ScriptIntrinsicConvolve5x5> convolve = ScriptIntrinsicConvolve5x5::create(rs, e);
        convolve->setInput(inputAlloc);
        convolve->setCoefficients(coeffs);
        convolve->forEach(outputAlloc);
    }

    outputAlloc->copy2DRangeTo(0, 0, X, Y, output);

    env->ReleasePrimitiveArrayCritical(inputByteArray, input, 0);
    env->ReleasePrimitiveArrayCritical(outputByteArray, output, 0);
    env->ReleaseFloatArrayElements(coeffArray, coeffs, JNI_ABORT);
    return (rs->getError() == RS_SUCCESS);

}

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSLUTTest_lutTest(JNIEnv * env,
                                                                               jclass obj,
                                                                               jint X,
                                                                               jint Y,
                                                                               jbyteArray inputByteArray,
                                                                               jbyteArray outputByteArray)
{
    jbyte * input = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray, 0);
    jbyte * output = (jbyte *) env->GetPrimitiveArrayCritical(outputByteArray, 0);

    sp<RS> rs = new RS();
    rs->init();

    sp<const Element> e = Element::RGBA_8888(rs);

    sp<Allocation> inputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<Allocation> outputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<ScriptIntrinsicLUT> lut = ScriptIntrinsicLUT::create(rs, e);

    inputAlloc->copy2DRangeFrom(0, 0, X, Y, input);
    unsigned char lutValues[256];
    for (int i = 0; i < 256; i++) {
        lutValues[i] = 255-i;
    }
    lut->setRed(0, 256, lutValues);
    lut->setGreen(0, 256, lutValues);
    lut->setBlue(0, 256, lutValues);

    lut->forEach(inputAlloc,outputAlloc);
    outputAlloc->copy2DRangeTo(0, 0, X, Y, output);

    env->ReleasePrimitiveArrayCritical(inputByteArray, input, 0);
    env->ReleasePrimitiveArrayCritical(outputByteArray, output, 0);
    return (rs->getError() == RS_SUCCESS);

}

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RS3DLUTTest_lutTest(JNIEnv * env,
                                                                                 jclass obj,
                                                                                 jint X,
                                                                                 jint Y,
                                                                                 jint lutSize,
                                                                                 jbyteArray inputByteArray,
                                                                                 jbyteArray inputByteArray2,
                                                                                 jbyteArray outputByteArray)
{
    jbyte * input = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray, 0);
    jbyte * input2 = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray2, 0);
    jbyte * output = (jbyte *) env->GetPrimitiveArrayCritical(outputByteArray, 0);

    sp<RS> rs = new RS();
    rs->init();

    sp<const Element> e = Element::RGBA_8888(rs);

    Type::Builder builder(rs, e);

    builder.setX(lutSize);
    builder.setY(lutSize);
    builder.setZ(lutSize);

    sp<Allocation> inputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<Allocation> colorCube = Allocation::createTyped(rs, builder.create());
    sp<Allocation> outputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<ScriptIntrinsic3DLUT> lut = ScriptIntrinsic3DLUT::create(rs, e);

    inputAlloc->copy2DRangeFrom(0, 0, X, Y, input);
    colorCube->copy3DRangeFrom(0, 0, 0, lutSize, lutSize, lutSize, input2);

    lut->setLUT(colorCube);
    lut->forEach(inputAlloc,outputAlloc);

    outputAlloc->copy2DRangeTo(0, 0, X, Y, output);

    env->ReleasePrimitiveArrayCritical(inputByteArray, input, 0);
    env->ReleasePrimitiveArrayCritical(inputByteArray2, input2, 0);
    env->ReleasePrimitiveArrayCritical(outputByteArray, output, 0);
    return (rs->getError() == RS_SUCCESS);

}

extern "C" JNIEXPORT jboolean JNICALL
Java_android_cts_rscpp_RSColorMatrixTest_colorMatrixTest(JNIEnv * env, jclass obj, jint X,
                                                         jint Y, jbyteArray inputByteArray,
                                                         jbyteArray outputByteArray,
                                                         jfloatArray coeffArray,
                                                         jint optionFlag)
{
    jfloat * coeffs = env->GetFloatArrayElements(coeffArray, NULL);
    jbyte * input = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray, 0);
    jbyte * output = (jbyte *) env->GetPrimitiveArrayCritical(outputByteArray, 0);

    sp<RS> rs = new RS();
    rs->init();

    sp<const Element> e = Element::RGBA_8888(rs);

    sp<Allocation> inputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<Allocation> outputAlloc = Allocation::createSized2D(rs, e, X, Y);

    inputAlloc->copy2DRangeFrom(0, 0, X, Y, input);

    sp<ScriptIntrinsicColorMatrix> cm = ScriptIntrinsicColorMatrix::create(rs);
    if (optionFlag == 0) {
        cm->setColorMatrix3(coeffs);
    } else if (optionFlag == 1) {
        cm->setGreyscale();
    } else if (optionFlag == 2) {
        cm->setColorMatrix4(coeffs);
    } else if (optionFlag == 3) {
        cm->setYUVtoRGB();
    } else if (optionFlag == 4) {
        cm->setRGBtoYUV();
    } else if (optionFlag == 5) {
        cm->setColorMatrix4(coeffs);
        float add[4] = {5.3f, 2.1f, 0.3f, 4.4f};
        cm->setAdd(add);
    }
    cm->forEach(inputAlloc, outputAlloc);

    outputAlloc->copy2DRangeTo(0, 0, X, Y, output);

    env->ReleasePrimitiveArrayCritical(inputByteArray, input, 0);
    env->ReleasePrimitiveArrayCritical(outputByteArray, output, 0);
    env->ReleaseFloatArrayElements(coeffArray, coeffs, JNI_ABORT);
    return (rs->getError() == RS_SUCCESS);

}

extern "C" JNIEXPORT jboolean JNICALL
Java_android_cts_rscpp_RSBlendTest_blendTest(JNIEnv * env, jclass obj, jint X,
                                             jint Y, jbyteArray inputByteArray,
                                             jbyteArray outputByteArray,
                                             jint optionFlag)
{
    jbyte * input = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray, 0);
    jbyte * output = (jbyte *) env->GetPrimitiveArrayCritical(outputByteArray, 0);

    sp<RS> rs = new RS();
    rs->init();

    sp<const Element> e = Element::RGBA_8888(rs);

    sp<Allocation> inputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<Allocation> outputAlloc = Allocation::createSized2D(rs, e, X, Y);

    inputAlloc->copy2DRangeFrom(0, 0, X, Y, input);
    outputAlloc->copy2DRangeFrom(0, 0, X, Y, output);

    sp<ScriptIntrinsicBlend> blend = ScriptIntrinsicBlend::create(rs, e);
    switch(optionFlag) {
    case 0:
        blend->forEachAdd(inputAlloc, outputAlloc);
        break;
    case 1:
        blend->forEachClear(inputAlloc, outputAlloc);
        break;
    case 2:
        blend->forEachDst(inputAlloc, outputAlloc);
        break;
    case 3:
        blend->forEachDstAtop(inputAlloc, outputAlloc);
        break;
    case 4:
        blend->forEachDstIn(inputAlloc, outputAlloc);
        break;
    case 5:
        blend->forEachDstOut(inputAlloc, outputAlloc);
        break;
    case 6:
        blend->forEachDstOver(inputAlloc, outputAlloc);
        break;
    case 7:
        blend->forEachMultiply(inputAlloc, outputAlloc);
        break;
    case 8:
        blend->forEachSrc(inputAlloc, outputAlloc);
        break;
    case 9:
        blend->forEachSrcAtop(inputAlloc, outputAlloc);
        break;
    case 10:
        blend->forEachSrcIn(inputAlloc, outputAlloc);
        break;
    case 11:
        blend->forEachSrcOut(inputAlloc, outputAlloc);
        break;
    case 12:
        blend->forEachSrcOver(inputAlloc, outputAlloc);
        break;
    case 13:
        blend->forEachSubtract(inputAlloc, outputAlloc);
        break;
    case 14:
        blend->forEachXor(inputAlloc, outputAlloc);
        break;
    default:
        break;
    }

    outputAlloc->copy2DRangeTo(0, 0, X, Y, output);

    env->ReleasePrimitiveArrayCritical(inputByteArray, input, 0);
    env->ReleasePrimitiveArrayCritical(outputByteArray, output, 0);
    return (rs->getError() == RS_SUCCESS);

}

