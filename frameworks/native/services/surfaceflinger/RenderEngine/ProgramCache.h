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

#ifndef SF_RENDER_ENGINE_PROGRAMCACHE_H
#define SF_RENDER_ENGINE_PROGRAMCACHE_H

#include <GLES2/gl2.h>

#include <utils/Singleton.h>
#include <utils/KeyedVector.h>
#include <utils/TypeHelpers.h>

#include "Description.h"

namespace android {

class Description;
class Program;
class String8;

/*
 * This class generates GLSL programs suitable to handle a given
 * Description. It's responsible for figuring out what to
 * generate from a Description.
 * It also maintains a cache of these Programs.
 */
class ProgramCache : public Singleton<ProgramCache> {
public:
    /*
     * Key is used to retrieve a Program in the cache.
     * A Key is generated from a Description.
     */
    class Key {
        friend class ProgramCache;
        typedef uint32_t key_t;
        key_t mKey;
    public:
        enum {
            BLEND_PREMULT           =       0x00000001,
            BLEND_NORMAL            =       0x00000000,
            BLEND_MASK              =       0x00000001,

            OPACITY_OPAQUE          =       0x00000002,
            OPACITY_TRANSLUCENT     =       0x00000000,
            OPACITY_MASK            =       0x00000002,

            PLANE_ALPHA_LT_ONE      =       0x00000004,
            PLANE_ALPHA_EQ_ONE      =       0x00000000,
            PLANE_ALPHA_MASK        =       0x00000004,

            TEXTURE_OFF             =       0x00000000,
            TEXTURE_EXT             =       0x00000008,
            TEXTURE_2D              =       0x00000010,
            TEXTURE_MASK            =       0x00000018,

            COLOR_MATRIX_OFF        =       0x00000000,
            COLOR_MATRIX_ON         =       0x00000020,
            COLOR_MATRIX_MASK       =       0x00000020,
        };

        inline Key() : mKey(0) { }
        inline Key(const Key& rhs) : mKey(rhs.mKey) { }

        inline Key& set(key_t mask, key_t value) {
            mKey = (mKey & ~mask) | value;
            return *this;
        }

        inline bool isTexturing() const {
            return (mKey & TEXTURE_MASK) != TEXTURE_OFF;
        }
        inline int getTextureTarget() const {
            return (mKey & TEXTURE_MASK);
        }
        inline bool isPremultiplied() const {
            return (mKey & BLEND_MASK) == BLEND_PREMULT;
        }
        inline bool isOpaque() const {
            return (mKey & OPACITY_MASK) == OPACITY_OPAQUE;
        }
        inline bool hasPlaneAlpha() const {
            return (mKey & PLANE_ALPHA_MASK) == PLANE_ALPHA_LT_ONE;
        }
        inline bool hasColorMatrix() const {
            return (mKey & COLOR_MATRIX_MASK) == COLOR_MATRIX_ON;
        }

        // this is the definition of a friend function -- not a method of class Needs
        friend inline int strictly_order_type(const Key& lhs, const Key& rhs) {
            return  (lhs.mKey < rhs.mKey) ? 1 : 0;
        }
    };

    ProgramCache();
    ~ProgramCache();

    // useProgram lookup a suitable program in the cache or generates one
    // if none can be found.
    void useProgram(const Description& description);

private:
    // compute a cache Key from a Description
    static Key computeKey(const Description& description);
    // generates a program from the Key
    static Program* generateProgram(const Key& needs);
    // generates the vertex shader from the Key
    static String8 generateVertexShader(const Key& needs);
    // generates the fragment shader from the Key
    static String8 generateFragmentShader(const Key& needs);

    // Key/Value map used for caching Programs. Currently the cache
    // is never shrunk.
    DefaultKeyedVector<Key, Program*> mCache;
};


ANDROID_BASIC_TYPES_TRAITS(ProgramCache::Key)

} /* namespace android */

#endif /* SF_RENDER_ENGINE_PROGRAMCACHE_H */
