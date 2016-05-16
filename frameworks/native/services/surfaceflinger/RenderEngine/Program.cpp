/*Gluint
 * Copyright 2013 The Android Open Source Project
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

#include <stdint.h>

#include <log/log.h>

#include "Program.h"
#include "ProgramCache.h"
#include "Description.h"
#include <utils/String8.h>

namespace android {

Program::Program(const ProgramCache::Key& needs, const char* vertex, const char* fragment)
        : mInitialized(false) {
    GLuint vertexId = buildShader(vertex, GL_VERTEX_SHADER);
    GLuint fragmentId = buildShader(fragment, GL_FRAGMENT_SHADER);
    GLuint programId = glCreateProgram();
    glAttachShader(programId, vertexId);
    glAttachShader(programId, fragmentId);
    glBindAttribLocation(programId, position, "position");
    glBindAttribLocation(programId, texCoords, "texCoords");
    glLinkProgram(programId);

    GLint status;
    glGetProgramiv(programId, GL_LINK_STATUS, &status);
    if (status != GL_TRUE) {
        ALOGE("Error while linking shaders:");
        GLint infoLen = 0;
        glGetProgramiv(programId, GL_INFO_LOG_LENGTH, &infoLen);
        if (infoLen > 1) {
            GLchar log[infoLen];
            glGetProgramInfoLog(programId, infoLen, 0, &log[0]);
            ALOGE("%s", log);
        }
        glDetachShader(programId, vertexId);
        glDetachShader(programId, fragmentId);
        glDeleteShader(vertexId);
        glDeleteShader(fragmentId);
        glDeleteProgram(programId);
    } else {
        mProgram = programId;
        mVertexShader = vertexId;
        mFragmentShader = fragmentId;
        mInitialized = true;

        mColorMatrixLoc = glGetUniformLocation(programId, "colorMatrix");
        mProjectionMatrixLoc = glGetUniformLocation(programId, "projection");
        mTextureMatrixLoc = glGetUniformLocation(programId, "texture");
        mSamplerLoc = glGetUniformLocation(programId, "sampler");
        mColorLoc = glGetUniformLocation(programId, "color");
        mAlphaPlaneLoc = glGetUniformLocation(programId, "alphaPlane");

        // set-up the default values for our uniforms
        glUseProgram(programId);
        const GLfloat m[16] = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1 };
        glUniformMatrix4fv(mProjectionMatrixLoc, 1, GL_FALSE, m);
        glEnableVertexAttribArray(0);
    }
}

Program::~Program() {
}

bool Program::isValid() const {
    return mInitialized;
}

void Program::use() {
    glUseProgram(mProgram);
}

GLuint Program::getAttrib(const char* name) const {
    // TODO: maybe use a local cache
    return glGetAttribLocation(mProgram, name);
}

GLint Program::getUniform(const char* name) const {
    // TODO: maybe use a local cache
    return glGetUniformLocation(mProgram, name);
}

GLuint Program::buildShader(const char* source, GLenum type) {
    GLuint shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, 0);
    glCompileShader(shader);
    GLint status;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &status);
    if (status != GL_TRUE) {
        // Some drivers return wrong values for GL_INFO_LOG_LENGTH
        // use a fixed size instead
        GLchar log[512];
        glGetShaderInfoLog(shader, sizeof(log), 0, log);
        ALOGE("Error while compiling shader: \n%s\n%s", source, log);
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

String8& Program::dumpShader(String8& result, GLenum type) {
    GLuint shader = GL_FRAGMENT_SHADER ? mFragmentShader : mVertexShader;
    GLint l;
    glGetShaderiv(shader, GL_SHADER_SOURCE_LENGTH, &l);
    char* src = new char[l];
    glGetShaderSource(shader, l, NULL, src);
    result.append(src);
    delete [] src;
    return result;
}

void Program::setUniforms(const Description& desc) {

    // TODO: we should have a mechanism here to not always reset uniforms that
    // didn't change for this program.

    if (mSamplerLoc >= 0) {
        glUniform1i(mSamplerLoc, 0);
        glUniformMatrix4fv(mTextureMatrixLoc, 1, GL_FALSE, desc.mTexture.getMatrix().asArray());
    }
    if (mAlphaPlaneLoc >= 0) {
        glUniform1f(mAlphaPlaneLoc, desc.mPlaneAlpha);
    }
    if (mColorLoc >= 0) {
        glUniform4fv(mColorLoc, 1, desc.mColor);
    }
    if (mColorMatrixLoc >= 0) {
        glUniformMatrix4fv(mColorMatrixLoc, 1, GL_FALSE, desc.mColorMatrix.asArray());
    }
    // these uniforms are always present
    glUniformMatrix4fv(mProjectionMatrixLoc, 1, GL_FALSE, desc.mProjectionMatrix.asArray());
}

} /* namespace android */
