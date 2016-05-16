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
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <stdio.h>
#include <stdlib.h>
#include "color_one.h"
#include "common.h"
#include "vertex.h"
#include "shader.h"

#define  LOG_TAG    "color_one"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

static const GLfloat gTriangleVertices[] = { 0.0f, 0.5f, -0.5f, -0.5f,
        0.5f, -0.5f };
GLuint gProgram;
GLuint gvPositionHandle;
GLuint gvColorHandle;
int width;
int height;

float dataFloat[4];
void initColorOne(int w, int h){
    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, color_one_vertex_shader_one);
    GLuint fragmentShader = loadShader(GL_FRAGMENT_SHADER, color_one_fragment_shader_one);
    gProgram = glCreateProgram();
    LOGI("Program %d\n", gProgram);
    width = w;
    height = h;
    glAttachShader(gProgram, vertexShader);
    checkGlError("glAttachShader");
    glAttachShader(gProgram, fragmentShader);
    checkGlError("glAttachShader");
    glBindAttribLocation(gProgram, 0, "vPosition");
    glBindAttribLocation(gProgram, 1, "vColor");
    glLinkProgram(gProgram);
    GLint linkStatus = GL_FALSE;
    glGetProgramiv(gProgram, GL_LINK_STATUS, &linkStatus);
    if (linkStatus != GL_TRUE) {
        GLint bufLength = 0;
        glGetProgramiv(gProgram, GL_INFO_LOG_LENGTH, &bufLength);
        if (bufLength) {
            char* buf = (char*) malloc(bufLength);
            if (buf) {
                glGetProgramInfoLog(gProgram, bufLength, NULL, buf);
                LOGE("Could not link program:\n%s\n", buf);
                free(buf);
             }
        }
    }
    LOGI("w %d, h %d\n",w, h);
    glViewport(0, 0, w, h);

    checkGlError("glViewport");
    gvPositionHandle = glGetAttribLocation(gProgram, "vPosition");
    gvColorHandle = glGetAttribLocation(gProgram, "vColor");
    GLsizei maxCount = 10;
    GLsizei count;
    GLuint shaders[maxCount];

    glGetAttachedShaders(gProgram, maxCount,
         &count,
         shaders);
    LOGI("Attached Shader First element :  %d\n", *shaders);
    LOGI("ShaderCount %d\n", count);
    GLint error = glGetError();
    return;
}

float* drawColorOne(float mColor[]){
     LOGI("drawColorOne start");
    static float grey;
    grey = 0.01f;

    glClearColor(grey, grey, grey, 1.0f);
    checkGlError("glClearColor");
    glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
    checkGlError("glClear");

    glUseProgram(gProgram);
    checkGlError("glUseProgram");

    glVertexAttribPointer(gvPositionHandle, 2, GL_FLOAT, GL_FALSE, 0, gTriangleVertices);
    checkGlError("glVertexAttribPointer");
    glEnableVertexAttribArray(0);
    checkGlError("glEnableVertexAttribArray");

    glVertexAttribPointer(gvColorHandle,4, GL_FLOAT, GL_FALSE, 0, mColor);
    checkGlError("glVertexAttribPointer");
    glEnableVertexAttribArray(1);
    checkGlError("glEnableVertexAttribArray");

    glDrawArrays(GL_TRIANGLES, 0, 3);
    checkGlError("glDrawArrays");
    GLubyte data[4*1];


    glReadPixels(width/2, height/2, 1,1, GL_RGBA, GL_UNSIGNED_BYTE, (GLvoid*)&data);
    for(int i = 0; i < sizeof(data); i++){
        dataFloat[i] = data[i];
    }

    return dataFloat;
}

void deleteColorOne() {
     glDeleteProgram(gProgram);
}

static void checkGlError(const char* op) {
    for (GLint error = glGetError(); error; error
            = glGetError()) {
        LOGI("after %s() glError (0x%x)\n", op, error);
    }
}
