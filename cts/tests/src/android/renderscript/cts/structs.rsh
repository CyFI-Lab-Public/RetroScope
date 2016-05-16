// Copyright (C) 2011 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

typedef struct ConstMatrix {
    rs_matrix4x4 MATRIX;
} ConstMatrix_s;
ConstMatrix_s *c1;

typedef struct ConstComplex {
    rs_matrix4x4 MATRIX;
    rs_matrix4x4 EXTRA;
    float extra1;
    float2 extra2;
    float3 extra3;
    float4 extra4;
} ConstComplex_s;
ConstComplex_s *c2;

typedef struct ConstExtra {
    rs_matrix4x4 EXTRA;
    float extra1;
    float2 extra2;
    float3 extra3;
    float4 extra4;
} ConstExtra_s;
ConstExtra_s *c3;

typedef struct AllVectorTypes {
    char2 b2;
    char3 b3;
    char4 b4;

    short2 s2;
    short3 s3;
    short4 s4;

    int2 i2;
    int3 i3;
    int4 i4;

    float2 f2;
    float3 f3;
    float4 f4;
} AllVectorTypes_s;
AllVectorTypes_s *avt;
