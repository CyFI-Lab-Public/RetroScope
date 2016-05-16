/*
 * Copyright (C) 2012 Unknown
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

#pragma version(1)
#pragma rs java_package_name(com.android.gallery3d.filtershow.filters)

#define MAX_POINTS 16

uint32_t inputWidth;
uint32_t inputHeight;
static const float Rf = 0.2999f;
static const float Gf = 0.587f;
static const float Bf = 0.114f;
//static const float size_scale = 0.01f;

typedef struct {
    rs_matrix3x3 colorMatrix;
    float rgbOff;
    float dx;
    float dy;
    float off;
} UPointData;
int mNumberOfLines;
// input data
bool mask[MAX_POINTS];
int xPos1[MAX_POINTS];
int yPos1[MAX_POINTS];
int xPos2[MAX_POINTS];
int yPos2[MAX_POINTS];
int size[MAX_POINTS];
int brightness[MAX_POINTS];
int contrast[MAX_POINTS];
int saturation[MAX_POINTS];

// generated data
static UPointData grads[MAX_POINTS];

void setupGradParams() {
    int k = 0;
    for (int i = 0; i < MAX_POINTS; i++) {
      if (!mask[i]) {
         continue;
      }
      float x1 = xPos1[i];
      float y1 = yPos1[i];
      float x2 = xPos2[i];
      float y2 = yPos2[i];

      float denom = (y2 * y2 - 2 * y1 * y2 + x2 * x2 - 2 * x1 * x2 + y1 * y1 + x1 * x1);
      if (denom == 0) {
         continue;
      }
      grads[k].dy = (y1 - y2) / denom;
      grads[k].dx = (x1 - x2) / denom;
      grads[k].off = (y2 * y2 + x2 * x2 - x1 * x2 - y1 * y2) / denom;

      float S = 1+saturation[i]/100.f;
      float MS = 1-S;
      float Rt = Rf * MS;
      float Gt = Gf * MS;
      float Bt = Bf * MS;

      float b = 1+brightness[i]/100.f;
      float c = 1+contrast[i]/100.f;
      b *= c;
      grads[k].rgbOff = .5f - c/2.f;
      rsMatrixSet(&grads[i].colorMatrix, 0, 0, b * (Rt + S));
      rsMatrixSet(&grads[i].colorMatrix, 1, 0, b * Gt);
      rsMatrixSet(&grads[i].colorMatrix, 2, 0, b * Bt);
      rsMatrixSet(&grads[i].colorMatrix, 0, 1, b * Rt);
      rsMatrixSet(&grads[i].colorMatrix, 1, 1, b * (Gt + S));
      rsMatrixSet(&grads[i].colorMatrix, 2, 1, b * Bt);
      rsMatrixSet(&grads[i].colorMatrix, 0, 2, b * Rt);
      rsMatrixSet(&grads[i].colorMatrix, 1, 2, b * Gt);
      rsMatrixSet(&grads[i].colorMatrix, 2, 2, b * (Bt + S));

      k++;
    }
    mNumberOfLines = k;
}

void init() {

}

uchar4 __attribute__((kernel)) selectiveAdjust(const uchar4 in, uint32_t x,
    uint32_t y) {
    float4 pixel = rsUnpackColor8888(in);

    float4 wsum = pixel;
    wsum.a = 0.f;
    for (int i = 0; i < mNumberOfLines; i++) {
        UPointData* grad = &grads[i];
        float t = clamp(x*grad->dx+y*grad->dy+grad->off,0.f,1.0f);
        wsum.xyz = wsum.xyz*(1-t)+
            t*(rsMatrixMultiply(&grad->colorMatrix ,wsum.xyz)+grad->rgbOff);

    }

    pixel.rgb = wsum.rgb;
    pixel.a = 1.0f;

    uchar4 out = rsPackColorTo8888(clamp(pixel, 0.f, 1.0f));
    return out;
}



