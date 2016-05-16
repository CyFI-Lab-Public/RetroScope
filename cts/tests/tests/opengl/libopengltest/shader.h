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
#ifndef _SHADER_H_
#define _SHADER_H_

static const char attach_shader_successful_complile_shader[] =
           "attribute vec3 gtf_Normal;\n"
            "attribute vec4 gtf_Vertex;\n"
            "uniform mat3 gtf_NormalMatrix;\n"
            "uniform mat4 gtf_ModelViewMatrix;\n"
            "uniform mat4 gtf_ModelViewProjectionMatrix;\n"

            "varying float lightIntensity;\n"
            "varying vec3 Position;\n"
            "uniform vec3 LightPosition;\n"
            "uniform float Scale;\n"
            "void main(void) {\n"
            "vec4 pos = gtf_ModelViewMatrix * gtf_Vertex;\n"
            "Position = vec3(gtf_Vertex) * Scale;\n"
            "vec3 tnorm = normalize(gtf_NormalMatrix * gtf_Normal);\n"
            "lightIntensity = dot(normalize(LightPosition - vec3(pos)), tnorm) * 1.5;\n"
            "gl_Position = gtf_ModelViewProjectionMatrix * gtf_Vertex;\n";

static const char color_one_fragment_shader_one[] =
        "precision mediump float;     \n"
        "varying vec4 varyColor;      \n"
        "void main()                  \n"
        "{                            \n"
        "  gl_FragColor = varyColor;  \n"
        "}       ";

static const char color_one_fragment_shader[] =
        "precision mediump float;     \n"
        "void main()                  \n"
        "{                            \n"
        "  gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);  \n"
        "}       ";

#endif
