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
#include "attach_shader_seven.h"

#define  LOG_TAG    "attach_shader_seven"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

Data attachShaderSeven(){
    GLuint vertexShaderOne = glCreateShader(GL_VERTEX_SHADER);
    GLuint vertexShaderTwo = glCreateShader(GL_VERTEX_SHADER);

    GLuint program = glCreateProgram();
    glAttachShader(program, vertexShaderOne);
    glAttachShader(program, vertexShaderTwo);


    GLint error = glGetError();
    Data data = {error, -9 , -1};
    glDeleteShader(vertexShaderOne);
    glDeleteShader(vertexShaderTwo);
    glDeleteProgram(program);
    return data;
}
