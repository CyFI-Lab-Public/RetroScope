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

public class AttachShaderTest extends ActivityInstrumentationTestCase2<OpenGLES20ActivityOne> {

    private OpenGLES20ActivityOne mActivity;

    public AttachShaderTest() {
        super(OpenGLES20ActivityOne.class);
    }

    private OpenGLES20ActivityOne getShaderActivity(int viewType, int viewIndex) {
        Intent intent = new Intent();
        intent.putExtra(OpenGLES20NativeActivityOne.EXTRA_VIEW_TYPE, viewType);
        intent.putExtra(OpenGLES20NativeActivityOne.EXTRA_VIEW_INDEX, viewIndex);
        setActivityIntent(intent);
        OpenGLES20ActivityOne activity = getActivity();
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
        int shaderCount = mActivity.getNoOfAttachedShaders();
        assertEquals(2,shaderCount);
        int error = mActivity.glGetError();
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
/* some devices crash for wrong parameter, and that cannot be reliably tested.
    public void test_glAttachedShaders_invalidshader() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 2);
        int error = mActivity.glGetError();
        assertTrue(GLES20.GL_NO_ERROR != error);
    }
*/
    /**
     * Test: Attach two shaders of the same type to the program
     * <pre>
     * shader count : 1
     * error        : GLES20.GL_INVALID_OPERATION
     * </pre>
     * @throws Throwable
     */
/* some devices crash for wrong parameter, and that cannot be reliably tested.
    public void test_glAttachedShaders_attach_same_shader() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 3);
        int error = mActivity.glGetError();
        assertTrue(GLES20.GL_NO_ERROR != error);
    }
*/

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
        int shaderCount = mActivity.getNoOfAttachedShaders();
        assertEquals(0, shaderCount);
        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_NO_ERROR, error);
    }

/* Only one frag shader should be attached.
    public void test_glAttachShaders_emptyfragshader_emptyfragshader() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 5);
        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_INVALID_OPERATION, error);
    }
*/

    public void test_glAttachShaders_emptyfragshader_emptyvertexshader() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 6);
        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_NO_ERROR, error);
    }

/* This test is wrong in that glAttachShader can attach only one vertex shader
   to a program

    public void test_glAttachShaders_emptyvertexshader_emptyvertexshader() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 7);
        int error = mActivity.glGetError();
        assertTrue(GLES20.GL_NO_ERROR != error);
    }
*/
    public void test_glAttachShaders_programobject_attach_fragshaderobject() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 8);
        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_NO_ERROR, error);
    }

    public void test_glAttachShaders_invalidshader_attach_valid_handle() throws Throwable{
        mActivity = getShaderActivity(Constants.SHADER, 9);
        int error = mActivity.glGetError();
        assertTrue(GLES20.GL_NO_ERROR != error);
    }

    public void test_glAttachShaders_successfulcompile_attach_frag() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 10);
        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_NO_ERROR, error);
    }

    public void test_glAttachShaders_successfulcompile_attach_vert() throws Throwable {
        mActivity = getShaderActivity(Constants.SHADER, 11);
        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_NO_ERROR, error);
    }
}
