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

package android.bordeaux.services;

import android.bordeaux.services.IBordeauxServiceCallback;

/**
 * Main learning interface of bordeaux service
 * (running in another process).
 */
interface IBordeauxService {
    /* Request a Ranker
    */
    IBinder getRanker(String name);

    /* Request a MulticlassPA
    */
    IBinder getClassifier(String name);

    /* Request to access AggregatorManager
    */
    IBinder getAggregatorManager();
    /* Request a Predictor
    */
    IBinder getPredictor(String name);
    /**
     * Often you want to allow a service to call back to its clients.
     * This shows how to do so, by registering a callback interface with
     * the service.
     */
    void registerCallback(IBordeauxServiceCallback cb);

    /**
     * Remove a previously registered callback interface.
     */
    void unregisterCallback(IBordeauxServiceCallback cb);
}
