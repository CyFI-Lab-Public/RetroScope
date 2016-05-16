/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

#include "GLUtils.h"
#include <stdlib.h>
#include <sys/time.h>

#include <android/asset_manager_jni.h>

#define LOG_TAG "CTS_OPENGL"
#define LOG_NDEBUG 0
#include <utils/Log.h>

static JNIEnv* sEnv = NULL;
static jobject sAssetManager = NULL;

void GLUtils::setEnvAndAssetManager(JNIEnv* env, jobject assetManager) {
    sEnv = env;
    sAssetManager = assetManager;
}

static AAsset* loadAsset(const char* path) {
    AAssetManager* nativeManager = AAssetManager_fromJava(sEnv, sAssetManager);
    if (nativeManager == NULL) {
        return NULL;
    }
    return AAssetManager_open(nativeManager, path, AASSET_MODE_UNKNOWN);;
}

char* GLUtils::openTextFile(const char* path) {
    AAsset* asset = loadAsset(path);
    if (asset == NULL) {
        ALOGE("Couldn't load %s", path);
        return NULL;
    }
    off_t length = AAsset_getLength(asset);
    char* buffer = new char[length + 1];
    int num = AAsset_read(asset, buffer, length);
    AAsset_close(asset);
    if (num != length) {
        ALOGE("Couldn't read %s", path);
        delete[] buffer;
        return NULL;
    }
    buffer[length] = '\0';
    return buffer;
}

GLuint GLUtils::loadTexture(const char* path) {
    GLuint textureId = 0;
    jclass activityClass = sEnv->FindClass("com/android/cts/opengl/reference/GLGameActivity");
    if (activityClass == NULL) {
        ALOGE("Couldn't find activity class");
        return -1;
    }
    jmethodID loadTexture = sEnv->GetStaticMethodID(activityClass, "loadTexture",
            "(Landroid/content/res/AssetManager;Ljava/lang/String;)I");
    if (loadTexture == NULL) {
        ALOGE("Couldn't find loadTexture method");
        return -1;
    }
    jstring pathStr = sEnv->NewStringUTF(path);
    textureId = sEnv->CallStaticIntMethod(activityClass, loadTexture, sAssetManager, pathStr);
    sEnv->DeleteLocalRef(pathStr);
    return textureId;
}

static int readInt(char* b) {
    return (((int) b[0]) << 24) | (((int) b[1]) << 16) | (((int) b[2]) << 8) | ((int) b[3]);
}

static float readFloat(char* b) {
    union {
        int input;
        float output;
    } data;
    data.input = readInt(b);
    return data.output;
}

Mesh* GLUtils::loadMesh(const char* path) {
    char* buffer = openTextFile(path);
    if (buffer == NULL) {
        return NULL;
    }
    int index = 0;
    int numVertices = readInt(buffer + index);
    index += 4;
    float* vertices = new float[numVertices * 3];
    float* normals = new float[numVertices * 3];
    float* texCoords = new float[numVertices * 2];
    for (int i = 0; i < numVertices; i++) {
        // Vertices
        int vIndex = i * 3;
        vertices[vIndex + 0] = readFloat(buffer + index);
        index += 4;
        vertices[vIndex + 1] = readFloat(buffer + index);
        index += 4;
        vertices[vIndex + 2] = readFloat(buffer + index);
        index += 4;
        // Normals
        normals[vIndex + 0] = readFloat(buffer + index);
        index += 4;
        normals[vIndex + 1] = readFloat(buffer + index);
        index += 4;
        normals[vIndex + 2] = readFloat(buffer + index);
        index += 4;
        // Texture Coordinates
        int tIndex = i * 2;
        texCoords[tIndex + 0] = readFloat(buffer + index);
        index += 4;
        texCoords[tIndex + 1] = readFloat(buffer + index);
        index += 4;
    }
    return new Mesh(vertices, normals, texCoords, numVertices);
}

// Loads the given source code as a shader of the given type.
static GLuint loadShader(GLenum shaderType, const char** source) {
    GLuint shader = glCreateShader(shaderType);
    if (shader) {
        glShaderSource(shader, 1, source, NULL);
        glCompileShader(shader);
        GLint compiled = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        if (!compiled) {
            GLint infoLen = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
            if (infoLen > 0) {
                char* infoLog = (char*) malloc(sizeof(char) * infoLen);
                glGetShaderInfoLog(shader, infoLen, NULL, infoLog);
                ALOGE("Error compiling shader:\n%s\n", infoLog);
                free(infoLog);
            }
            glDeleteShader(shader);
            shader = 0;
        }
    }
    return shader;
}

GLuint GLUtils::createProgram(const char** vertexSource, const char** fragmentSource) {
    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource);
    if (!vertexShader) {
        return 0;
    }

    GLuint fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource);
    if (!fragmentShader) {
        return 0;
    }

    GLuint program = glCreateProgram();
    if (program) {
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);

        GLint linkStatus;
        glLinkProgram(program);
        glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);

        if (!linkStatus) {
            GLint infoLen = 0;
            glGetProgramiv(program, GL_INFO_LOG_LENGTH, &infoLen);
            if (infoLen > 0) {
                char* infoLog = (char*) malloc(sizeof(char) * infoLen);
                glGetProgramInfoLog(program, infoLen, NULL, infoLog);
                ALOGE("Error linking program:\n%s\n", infoLog);
                free(infoLog);
            }
            glDeleteProgram(program);
            program = 0;
        }
    }
    return program;
}

double GLUtils::currentTimeMillis() {
    struct timeval tv;
    gettimeofday(&tv, (struct timezone *) NULL);
    return tv.tv_sec * 1000.0 + tv.tv_usec / 1000.0;
}

// Rounds a number up to the smallest power of 2 that is greater than or equal to x.
int GLUtils::roundUpToSmallestPowerOf2(int x) {
    if (x < 0) {
        return 0;
    }
    --x;
    x |= x >> 1;
    x |= x >> 2;
    x |= x >> 4;
    x |= x >> 8;
    x |= x >> 16;
    return x + 1;
}

GLuint GLUtils::genTexture(int texWidth, int texHeight, int fill) {
    GLuint textureId = 0;
    int w = roundUpToSmallestPowerOf2(texWidth);
    int h = roundUpToSmallestPowerOf2(texHeight);
    uint32_t* m = new uint32_t[w * h];
    if (m != NULL) {
        uint32_t* d = m;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (fill == RANDOM_FILL) {
                    *d = 0xff000000 | ((y & 0xff) << 16) | ((x & 0xff) << 8) | ((x + y) & 0xff);
                } else {
                    *d = 0xff000000 | fill;
                }
                d++;
            }
        }
        glGenTextures(1, &textureId);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, m);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    }
    delete[] m;
    return textureId;
}
