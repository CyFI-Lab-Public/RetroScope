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

public class NativeAttachShaderTest
        extends ActivityInstrumentationTestCase2<OpenGLES20NativeActivityOne> {

    private OpenGLES20NativeActivityOne mActivity;

    public NativeAttachShaderTest() {
        super(OpenGLES20NativeActivityOne.class);
    }

    private OpenGLES20NativeActivityOne getShaderActivity(int viewType, int viewIndex) {
        Intent intent = new Intent();
        intent.putExtra(OpenGLES20NativeActivityOne.EXTRA_VIEW_TYPE, viewType);
        intent.putExtra(OpenGLES20NativeActivityOne.EXTRA_VIEW_INDEX, viewIndex);
        setActivityIntent(intent);
        OpenGLES20NativeActivityOne activity = getActivity();
        assertTrue(activity.waitForFrameDrawn());
        return activity;
    }

    /**
     *Test: Attach an two valid shaders to a program
     * <pre>
     * shader count : 2
     * error        : GLES20.GL_NO_ERROR
     * </pre>
     */
    public void test_glAttachedShaders_validshader() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 1);
        int shaderCount = mActivity.mRenderer.mShaderCount;
        assertEquals(2, shaderCount);
        int error = mActivity.mRenderer.mAttachShaderError;
        assertEquals(GLES20.GL_NO_ERROR, error);
    }

    /**
     * Test: Attach an invalid vertex shader  to the program handle
     * <pre>
     * shader count : 1
     * error        : GLES20.GL_INVALID_VALUE
     * </pre>
     * @throws Throwable
     */
    public void test_glAttachedShaders_invalidshader() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 2);
        int error = mActivity.mRenderer.mAttachShaderError;
        assertTrue(GLES20.GL_NO_ERROR != error);
    }

    /**
     * Test: Attach two shaders of the same type to the program
     * <pre>
     * shader count : 1
     * error        : GLES20.GL_INVALID_OPERATION
     * </pre>
     * @throws Throwable
     */
    public void test_glAttachedShaders_attach_same_shader() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 3);
        int error = mActivity.mRenderer.mAttachShaderError;
        assertTrue(GLES20.GL_NO_ERROR != error);
    }

    /**
     * Test: No shader is attached to a program, glGetAttachedShaders returns
     * <pre>
     * shader count : 0
     * error        : GLES20.GL_NO_ERROR
     * </pre>
     * @throws Throwable
     */
    public void test_glAttachedShaders_noshader() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 4);
        int shaderCount = GL2JniLibOne.getAttachedShaderCount();
        assertEquals(0, shaderCount);

        int error = mActivity.mRenderer.mAttachShaderError;
        assertEquals(GLES20.GL_NO_ERROR, error);
    }

/* only one frag shader can be attached
    public void test_glAttachShaders_emptyfragshader_emptyfragshader() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 5);
        int error = mActivity.mRenderer.mAttachShaderError;
        assertTrue(GLES20.GL_NO_ERROR != error);
    }
*/
    public void test_glAttachShaders_emptyfragshader_emptyvertexshader() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 6);
        int error = mActivity.mRenderer.mAttachShaderError;;
        assertEquals(GLES20.GL_NO_ERROR, error);
    }

/* only one vertex shader can be attached
    public void test_glAttachShaders_emptyvertexshader_emptyvertexshader() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 7);
        int error = mActivity.mRenderer.mAttachShaderError;
        assertTrue(GLES20.GL_NO_ERROR != error);
    }
*/
    public void test_glAttachShaders_programobject_attach_fragshaderobject() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 8);
        int error = mActivity.mRenderer.mAttachShaderError;
        // The operations are valid
        assertEquals(GLES20.GL_NO_ERROR, error);
    }

    public void test_glAttachShaders_invalidshader_attach_valid_handle() throws Throwable{
        mActivity = getShaderActivity(Constants.SHADER, 9);
        int error = mActivity.mRenderer.mAttachShaderError;
        assertTrue(GLES20.GL_NO_ERROR != error);
    }

    public void test_glAttachShaders_successfulcompile_attach_frag() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 10);
        int shaderCount = mActivity.mRenderer.mShaderCount;
        assertEquals(1,shaderCount);
        int error = mActivity.mRenderer.mAttachShaderError;
        assertEquals(GLES20.GL_NO_ERROR, error);
    }

    public void test_glAttachShaders_successfulcompile_attach_vert() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 11);
        int error = mActivity.mRenderer.mAttachShaderError;
        assertEquals(GLES20.GL_NO_ERROR, error);
    }
}
