/*
 * Copyright (C) 2009 The Android Open Source Project
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
package android.gesture.cts;

import android.gesture.Gesture;
import android.gesture.Prediction;

import java.util.ArrayList;
import java.util.Set;

/**
 * Utility interface for making calls to a GestureStore or GestureLibrary fixture.
 * <p/>
 * Exposes API for methods which exist in both GestureStore and GestureLibrary, so tests can be
 * shared.
 */
interface GestureStorageAccessor {

    void addGesture(String entryName, Gesture gesture);

    Set<String> getGestureEntries();

    ArrayList<Gesture> getGestures(String entryName);

    int getOrientationStyle();

    int getSequenceType();

    ArrayList<Prediction> recognize(Gesture gesture);

    void removeEntry(String entryName);

    void removeGesture(String entryName, Gesture gesture);

    void setOrientationStyle(int style);

    void setSequenceType(int type);

}
