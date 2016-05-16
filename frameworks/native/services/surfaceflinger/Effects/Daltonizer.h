/*
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

#ifndef SF_EFFECTS_DALTONIZER_H_
#define SF_EFFECTS_DALTONIZER_H_

#include <ui/mat4.h>

namespace android {

class Daltonizer {
public:
    enum ColorBlindnessTypes {
        protanopia,         // L (red) cone missing
        deuteranopia,       // M (green) cone missing
        tritanopia,         // S (blue) cone missing
        protanomaly,        // L (red) cone deficient
        deuteranomaly,      // M (green) cone deficient (most common)
        tritanomaly         // S (blue) cone deficient
    };

    enum Mode {
        simulation,
        correction
    };

    Daltonizer();
    ~Daltonizer();

    void setType(ColorBlindnessTypes type);
    void setMode(Mode mode);

    // returns the color transform to apply in the shader
    const mat4& operator()();

private:
    void update();

    ColorBlindnessTypes mType;
    Mode mMode;
    bool mDirty;
    mat4 mColorTransform;
};

} /* namespace android */
#endif /* SF_EFFECTS_DALTONIZER_H_ */
