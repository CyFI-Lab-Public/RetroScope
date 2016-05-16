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
#include "attach_shader_eleven.h"
#include "common.h"
#include "vertex.h"

#define  LOG_TAG    "attach_shader_eleven"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

Data attachShaderEleven(){
    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, attach_shader_successful_complile_vertex);
    GLuint program = glCreateProgram();
    glAttachShader(program, vertexShader);

    GLsizei maxCount = 10;
    GLsizei count;
    GLuint shaders[maxCount];

    glGetAttachedShaders(program, maxCount,
         &count,
         shaders);
    LOGI("Attached Shader First element :  %d\n", *shaders);
    LOGI("ShaderCount %d\n", count);
    GLint error = glGetError();
    Data data = {error, count, -1};

    glDeleteProgram(program);
    return data;
}
