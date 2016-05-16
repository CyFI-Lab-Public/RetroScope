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

package com.android.gallery3d.filtershow.editors;

import com.android.gallery3d.filtershow.EditorPlaceHolder;
import com.android.gallery3d.filtershow.editors.BasicEditor;
import com.android.gallery3d.filtershow.editors.EditorCurves;
import com.android.gallery3d.filtershow.editors.EditorZoom;

public class EditorManager {

    public static void addEditors(EditorPlaceHolder editorPlaceHolder) {
        editorPlaceHolder.addEditor(new EditorGrad());
        editorPlaceHolder.addEditor(new EditorChanSat());
        editorPlaceHolder.addEditor(new EditorZoom());
        editorPlaceHolder.addEditor(new EditorCurves());
        editorPlaceHolder.addEditor(new EditorTinyPlanet());
        editorPlaceHolder.addEditor(new EditorDraw());
        editorPlaceHolder.addEditor(new EditorVignette());
        editorPlaceHolder.addEditor(new EditorColorBorder());
        editorPlaceHolder.addEditor(new EditorMirror());
        editorPlaceHolder.addEditor(new EditorRotate());
        editorPlaceHolder.addEditor(new EditorStraighten());
        editorPlaceHolder.addEditor(new EditorCrop());
    }

}
