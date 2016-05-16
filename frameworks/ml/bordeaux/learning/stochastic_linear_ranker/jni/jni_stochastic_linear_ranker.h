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

#ifndef LEARNING_JNI_STOCHASTIC_LINEAR_RANKER_H
#define LEAENING_JNI_STOCHASTIC_LINEAR_RANKER_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/*  Counts the number of learning iterations. */
const char * ITR_NUM = "IterationNumber";

/*  The maximum norm of the weight vector. If norm of weights are larger than NormConstraint
    they will be reprojected using RegularizationType to satisfy this constraint. */
const char * NORM_CONSTRAINT = "NormConstraint";

/*  Ddetermines type of the regularization used in learning.
    This regularization can be based on different norms.
    Options: "L0", "L1", "L2", "L1L2", "L1LInf".
    Default : LINEAR */
const char * REG_TYPE = "RegularizationType";

/*  Lambda is a factor that is multiplied with the step size in learning. This can be used
    to change the step size.
    Default : 1.0 */
const char * LAMBDA = "Lambda";

/*  This parameter determines the update type in learning process.
    Options: "FULL_CS" , "CLIP_CS", "REG_CS", "SL", "ADAPTIVE_REG"
    Default : "SL" */
const char * UPDATE_TYPE = "UpdateType";

/*  Options: "CONST", "INV_LINEAR", "INV_QUADRATIC", "INV_SQRT"
    Default: "INV_LINEAR". */
const char * ADAPT_MODE = "AdaptationMode";

/*  Three differnt kernels are supported: Linear "LINEAR", Polynomial "POLY", and RBF "RBF"
    Default : "LINEAR" */
const char * KERNEL_TYPE = "KernelType";

/*  Kernel param is kernel-specific. In case of polynomial kernel, it is the degree of the
    polynomial. In case of RBF kernel, it implies the sigma parameter. In case of linear
    kernel, it is not used. */
const char * KERNEL_PARAM = "KernelParameter";

/*  Kernel gain is typically a multiplicative factor to the dot product while calculating
    the kernel function. In most use cases, gain should be set to 1.0. */
const char * KERNEL_GAIN = "KernelGain";

/*  Kernel bias is typically an additive factors to the dot product while calculating
    the kernel function. In most use cases, bias should be set to 0.0. */
const char * KERNEL_BIAS = "KernelBias";

/*  This parameter determines the type of loss function to minimize.
    Options : "PAIRWISE", "RECIPROCAL_RANK"
    Default : "PAIRWISE" */
const char * LOSS_TYPE = "LossType";

/*  The minimum percent of training pairs that are used in training.
    Default : "0.1" */
const char * ACC_PROB = "AcceptaceProbability";

/*  The code averages out gradient updates for MinimumBatchSize samples
    before performing an iteration of the algorithm. */
const char * MIN_BATCH_SIZE = "MinimumBatchSize";

/*  Specifies the number of non-zero entries allowed in a gradient.
    Default is -1 which means we take the gradient as given by data without
    adding any new constraints. positive number is treated as an L0 constraint */
const char * GRAD_L0_NORM = "GradientL0Nrom";

const char * REG_TYPE_L0 = "L0";
const char * REG_TYPE_L1 = "L1";
const char * REG_TYPE_L2 = "L2";
const char * REG_TYPE_L1L2 = "L1L2";
const char * REG_TYPE_L1LInf = "L1LInf";
const char * UPDATE_TYPE_FULL_CS = "FULL_CS";
const char * UPDATE_TYPE_CLIP_CS = "CLIP_CS";
const char * UPDATE_TYPE_REG_CS = "REG_CS";
const char * UPDATE_TYPE_SL = "SL";
const char * UPDATE_TYPE_ADAPTIVE_REG = "ADAPTIVE_REG";
const char * ADAPT_MODE_CONST = "CONST";
const char * ADAPT_MODE_INV_LINEAR = "INV_LINEAR";
const char * ADAPT_MODE_INV_QUADRATIC = "INV_QUADRATIC";
const char * ADAPT_MODE_INV_SQRT = "INV_SQRT";
const char * KERNEL_TYPE_LINEAR = "LINEAR";
const char * KERNEL_TYPE_POLY = "POLY";
const char * KERNEL_TYPE_RBF = "RBF";
const char * LOSS_TYPE_PAIRWISE = "PAIRWISE";
const char * LOSS_TYPE_RECIPROCAL_RANK = "RECIPROCAL_RANK";

JNIEXPORT jint JNICALL
Java_android_bordeaux_learning_StochasticLinearRanker_initNativeClassifier(
    JNIEnv* env,
    jobject thiz);


JNIEXPORT jboolean JNICALL
Java_android_bordeaux_learning_StochasticLinearRanker_deleteNativeClassifier(
    JNIEnv* env,
    jobject thiz,
    jint paPtr);

JNIEXPORT jboolean JNICALL
Java_android_bordeaux_learning_StochasticLinearRanker_nativeUpdateClassifier(
    JNIEnv* env,
    jobject thiz,
    jobjectArray key_array_positive,
    jfloatArray value_array_positive,
    jobjectArray key_array_negative,
    jfloatArray value_array_negative,
    jint paPtr);

JNIEXPORT jfloat JNICALL
Java_android_bordeaux_learning_StochasticLinearRanker_nativeScoreSample(
    JNIEnv* env,
    jobject thiz,
    jobjectArray key_array,
    jfloatArray value_array,
    jint paPtr);

JNIEXPORT void JNICALL
Java_android_bordeaux_learning_StochasticLinearRanker_nativeGetWeightClassifier(
    JNIEnv* env,
    jobject thiz,
    jobjectArray key_array_weight,
    jfloatArray value_array_weight,
    jfloat normalizer,
    jint paPtr);

JNIEXPORT void JNICALL
Java_android_bordeaux_learning_StochasticLinearRanker_nativeGetParameterClassifier(
    JNIEnv* env,
    jobject thiz,
    jobjectArray key_array_param,
    jobjectArray value_array_param,
    jint paPtr);

JNIEXPORT jint JNICALL
Java_android_bordeaux_learning_StochasticLinearRanker_nativeGetLengthClassifier(
    JNIEnv* env,
    jobject thiz,
    jint paPtr);

JNIEXPORT jboolean JNICALL
Java_android_bordeaux_learning_StochasticLinearRanker_nativeSetWeightClassifier(
    JNIEnv* env,
    jobject thiz,
    jobjectArray key_array_model,
    jfloatArray value_array_model,
    jfloat normalizer_model,
    jint paPtr);

JNIEXPORT jboolean JNICALL
Java_android_bordeaux_learning_StochasticLinearRanker_nativeSetParameterClassifier(
    JNIEnv* env,
    jobject thiz,
    jstring key,
    jstring value,
    jint paPtr);

#ifdef __cplusplus
}
#endif

#endif /* ANDROID_LERNING_JNI_STOCHASTIC_LINEAR_RANKER_H */
