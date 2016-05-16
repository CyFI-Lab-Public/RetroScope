/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android.bordeaux.services;

/**
 * Example of a secondary interface associated with a service.  (Note that
 * the interface itself doesn't impact, it is just a matter of how you
 * retrieve it from the service.)
 */
import android.bordeaux.services.StringFloat;


interface ILearning_StochasticLinearRanker {

    boolean UpdateClassifier(in List<StringFloat> sample_1, in List<StringFloat> sample_2);
    float ScoreSample(in List<StringFloat> sample);
    void ResetRanker();
    boolean SetModelPriorWeight(in List<StringFloat> weight);
    boolean SetModelParameter(in String key, in String value);
}
