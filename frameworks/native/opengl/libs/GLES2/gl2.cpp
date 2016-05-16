/*
 ** Copyright 2007, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

#include <ctype.h>
#include <string.h>
#include <errno.h>

#include <sys/ioctl.h>

#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>
#include <GLES2/gl2ext.h>

#include <cutils/log.h>
#include <cutils/properties.h>

#include "../hooks.h"
#include "../egl_impl.h"

using namespace android;

// ----------------------------------------------------------------------------
// Actual GL entry-points
// ----------------------------------------------------------------------------

#undef API_ENTRY
#undef CALL_GL_API
#undef CALL_GL_API_RETURN

#if defined(__arm__) && !USE_SLOW_BINDING

    #define GET_TLS(reg) "mrc p15, 0, " #reg ", c13, c0, 3 \n"

    #define API_ENTRY(_api) __attribute__((noinline)) _api

    #define CALL_GL_API(_api, ...)                              \
         asm volatile(                                          \
            GET_TLS(r12)                                        \
            "ldr   r12, [r12, %[tls]] \n"                       \
            "cmp   r12, #0            \n"                       \
            "ldrne pc,  [r12, %[api]] \n"                       \
            :                                                   \
            : [tls] "J"(TLS_SLOT_OPENGL_API*4),                 \
              [api] "J"(__builtin_offsetof(gl_hooks_t, gl._api))    \
            :                                                   \
            );

#elif defined(__mips__) && !USE_SLOW_BINDING

    #define API_ENTRY(_api) __attribute__((noinline)) _api

    #define CALL_GL_API(_api, ...)                               \
        register unsigned int _t0 asm("t0");                     \
        register unsigned int _fn asm("t1");                     \
        register unsigned int _tls asm("v1");                    \
        register unsigned int _v0 asm("v0");                     \
        asm volatile(                                            \
            ".set  push\n\t"                                     \
            ".set  noreorder\n\t"                                \
            ".set mips32r2\n\t"                                  \
            "rdhwr %[tls], $29\n\t"                              \
            "lw    %[t0], %[OPENGL_API](%[tls])\n\t"             \
            "beqz  %[t0], 1f\n\t"                                \
            " move %[fn],$ra\n\t"                                \
            "lw    %[fn], %[API](%[t0])\n\t"                     \
            "movz  %[fn], $ra, %[fn]\n\t"                        \
            "1:\n\t"                                             \
            "j     %[fn]\n\t"                                    \
            " move %[v0], $0\n\t"                                \
            ".set  pop\n\t"                                      \
            : [fn] "=c"(_fn),                                    \
              [tls] "=&r"(_tls),                                 \
              [t0] "=&r"(_t0),                                   \
              [v0] "=&r"(_v0)                                    \
            : [OPENGL_API] "I"(TLS_SLOT_OPENGL_API*4),           \
              [API] "I"(__builtin_offsetof(gl_hooks_t, gl._api)) \
            :                                                    \
            );

#else

    #define API_ENTRY(_api) _api

    #define CALL_GL_API(_api, ...)                                       \
        gl_hooks_t::gl_t const * const _c = &getGlThreadSpecific()->gl;  \
        if (_c) return _c->_api(__VA_ARGS__);

#endif

#define CALL_GL_API_RETURN(_api, ...) \
    CALL_GL_API(_api, __VA_ARGS__) \
    return 0;



extern "C" {
#include "gl3_api.in"
#include "gl2ext_api.in"
#include "gl3ext_api.in"
}

#undef API_ENTRY
#undef CALL_GL_API
#undef CALL_GL_API_RETURN

/*
 * glGetString() is special because we expose some extensions in the wrapper
 */

extern "C" const GLubyte * __glGetString(GLenum name);

const GLubyte * glGetString(GLenum name)
{
    const GLubyte * ret = egl_get_string_for_current_context(name);
    if (ret == NULL) {
        gl_hooks_t::gl_t const * const _c = &getGlThreadSpecific()->gl;
        ret = _c->glGetString(name);
    }
    return ret;
}
