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
package android.opengl.cts;

import android.content.Intent;
import android.opengl.GLES20;
import android.test.ActivityInstrumentationTestCase2;

public class ProgramTest extends ActivityInstrumentationTestCase2<OpenGLES20ActivityOne> {

    private OpenGLES20ActivityOne mActivity;

    public ProgramTest() {
        super(OpenGLES20ActivityOne.class);
    }

    private OpenGLES20ActivityOne getShaderActivity(int viewType, int viewIndex) {
        Intent intent = new Intent();
        intent.putExtra(OpenGLES20NativeActivityOne.EXTRA_VIEW_TYPE, viewType);
        intent.putExtra(OpenGLES20NativeActivityOne.EXTRA_VIEW_INDEX, viewIndex);
        setActivityIntent(intent);
        return getActivity();
    }

    public void test_glAttachShader_program() throws Throwable {
        mActivity = getShaderActivity(Constants.PROGRAM, 1);
        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_INVALID_OPERATION, error);
    }
}
