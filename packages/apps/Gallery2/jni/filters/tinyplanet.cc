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

#include "filters.h"
#include <math.h>

#ifdef __cplusplus
extern "C" {
#endif


#define PI_F 3.141592653589f

class ImageRGBA {
 public:
  ImageRGBA(unsigned char* image, int width, int height)
   : image_(image), width_(width), height_(height) {
    width_step_ = width * 4;
  }

  int Width() const {
    return width_;
  }

  int Height() const {
    return height_;
  }

  // Pixel accessor.
  unsigned char* operator()(int x, int y) {
    return image_ + y * width_step_ + x * 4;
  }
  const unsigned char* operator()(int x, int y) const {
    return image_ + y * width_step_ + x * 4;
  }

 private:
  unsigned char* image_;
  int width_;
  int height_;
  int width_step_;
};

// Interpolate a pixel in a 3 channel image.
inline void InterpolatePixel(const ImageRGBA &image, float x, float y,
                             unsigned char* dest) {
  // Get pointers and scale factors for the source pixels.
  float ax = x - floor(x);
  float ay = y - floor(y);
  float axn = 1.0f - ax;
  float ayn = 1.0f - ay;
  const unsigned char *p = image(x, y);
  const unsigned char *p2 = image(x, y + 1);

  // Interpolate each image color plane.
  dest[0] = static_cast<unsigned char>(axn * ayn * p[0] + ax * ayn * p[4] +
             ax * ay * p2[4] + axn * ay * p2[0] + 0.5f);
  p++;
  p2++;

  dest[1] = static_cast<unsigned char>(axn * ayn * p[0] + ax * ayn * p[4] +
             ax * ay * p2[4] + axn * ay * p2[0] + 0.5f);
  p++;
  p2++;

  dest[2] = static_cast<unsigned char>(axn * ayn * p[0] + ax * ayn * p[4] +
             ax * ay * p2[4] + axn * ay * p2[0] + 0.5f);
  p++;
  p2++;
  dest[3] = 0xFF;
}

// Wrap circular coordinates around the globe
inline float wrap(float value, float dimension) {
  return value - (dimension * floor(value/dimension));
}

void StereographicProjection(float scale, float angle, unsigned char* input_image,
                             int input_width, int input_height,
                             unsigned char* output_image, int output_width,
                             int output_height) {
  ImageRGBA input(input_image, input_width, input_height);
  ImageRGBA output(output_image, output_width, output_height);

  const float image_scale = output_width * scale;

  for (int x = 0; x < output_width; x++) {
    // Center and scale x
    float xf = (x - output_width / 2.0f) / image_scale;

    for (int y = 0; y < output_height; y++) {
      // Center and scale y
      float yf = (y - output_height / 2.0f) / image_scale;

      // Convert to polar
      float r = hypotf(xf, yf);
      float theta = angle+atan2(yf, xf);
      if (theta>PI_F) theta-=2*PI_F;

      // Project onto plane
      float phi = 2 * atan(1 / r);
      // (theta stays the same)

      // Map to panorama image
      float px = (theta / (2 * PI_F)) * input_width;
      float py = (phi / PI_F) * input_height;

      // Wrap around the globe
      px = wrap(px, input_width);
      py = wrap(py, input_height);

      // Write the interpolated pixel
      InterpolatePixel(input, px, py, output(x, y));
    }
  }
}


void JNIFUNCF(ImageFilterTinyPlanet, nativeApplyFilter, jobject bitmap_in, jint width, jint height, jobject bitmap_out, jint output_size, jfloat scale,jfloat angle)
{
    char* source = 0;
    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap_in, (void**) &source);
    AndroidBitmap_lockPixels(env, bitmap_out, (void**) &destination);
    unsigned char * rgb_in = (unsigned char * )source;
    unsigned char * rgb_out = (unsigned char * )destination;

    StereographicProjection(scale,angle, rgb_in, width, height, rgb_out, output_size, output_size);
    AndroidBitmap_unlockPixels(env, bitmap_in);
    AndroidBitmap_unlockPixels(env, bitmap_out);
}

#ifdef __cplusplus
}
#endif


