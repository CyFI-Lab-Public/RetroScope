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
#include <stdlib.h>
#include <string.h>

#include <hardware/gralloc.h>
#include <system/window.h>

#include <EGL/egl.h>
#include <EGL/eglext.h>

#include <cutils/log.h>
#include <cutils/atomic.h>
#include <cutils/properties.h>
#include <cutils/memory.h>

#include <utils/CallStack.h>
#include <utils/String8.h>

#include "../egl_impl.h"
#include "../glestrace.h"

#include "egl_tls.h"
#include "egldefs.h"
#include "Loader.h"

#include "egl_display.h"
#include "egl_object.h"

// ----------------------------------------------------------------------------
namespace android {
// ----------------------------------------------------------------------------

egl_connection_t gEGLImpl;
gl_hooks_t gHooks[2];
gl_hooks_t gHooksNoContext;
pthread_key_t gGLWrapperKey = -1;

// ----------------------------------------------------------------------------

#if EGL_TRACE

EGLAPI pthread_key_t gGLTraceKey = -1;

// ----------------------------------------------------------------------------

/**
 * There are three different tracing methods:
 * 1. libs/EGL/trace.cpp: Traces all functions to systrace.
 *    To enable:
 *      - set system property "debug.egl.trace" to "systrace" to trace all apps.
 * 2. libs/EGL/trace.cpp: Logs a stack trace for GL errors after each function call.
 *    To enable:
 *      - set system property "debug.egl.trace" to "error" to trace all apps.
 * 3. libs/EGL/trace.cpp: Traces all functions to logcat.
 *    To enable:
 *      - set system property "debug.egl.trace" to 1 to trace all apps.
 *      - or call setGLTraceLevel(1) from an app to enable tracing for that app.
 * 4. libs/GLES_trace: Traces all functions via protobuf to host.
 *    To enable:
 *        - set system property "debug.egl.debug_proc" to the application name.
 *      - or call setGLDebugLevel(1) from the app.
 */
static int sEGLTraceLevel;
static int sEGLApplicationTraceLevel;

static bool sEGLSystraceEnabled;
static bool sEGLGetErrorEnabled;

static volatile int sEGLDebugLevel;

extern gl_hooks_t gHooksTrace;
extern gl_hooks_t gHooksSystrace;
extern gl_hooks_t gHooksErrorTrace;

int getEGLDebugLevel() {
    return sEGLDebugLevel;
}

void setEGLDebugLevel(int level) {
    sEGLDebugLevel = level;
}

static inline void setGlTraceThreadSpecific(gl_hooks_t const *value) {
    pthread_setspecific(gGLTraceKey, value);
}

gl_hooks_t const* getGLTraceThreadSpecific() {
    return static_cast<gl_hooks_t*>(pthread_getspecific(gGLTraceKey));
}

void initEglTraceLevel() {
    char value[PROPERTY_VALUE_MAX];
    property_get("debug.egl.trace", value, "0");

    sEGLGetErrorEnabled = !strcasecmp(value, "error");
    if (sEGLGetErrorEnabled) {
        sEGLSystraceEnabled = false;
        sEGLTraceLevel = 0;
        return;
    }

    sEGLSystraceEnabled = !strcasecmp(value, "systrace");
    if (sEGLSystraceEnabled) {
        sEGLTraceLevel = 0;
        return;
    }

    int propertyLevel = atoi(value);
    int applicationLevel = sEGLApplicationTraceLevel;
    sEGLTraceLevel = propertyLevel > applicationLevel ? propertyLevel : applicationLevel;
}

void initEglDebugLevel() {
    if (getEGLDebugLevel() == 0) {
        char value[PROPERTY_VALUE_MAX];

        // check system property only on userdebug or eng builds
        property_get("ro.debuggable", value, "0");
        if (value[0] == '0')
            return;

        property_get("debug.egl.debug_proc", value, "");
        if (strlen(value) > 0) {
            FILE * file = fopen("/proc/self/cmdline", "r");
            if (file) {
                char cmdline[256];
                if (fgets(cmdline, sizeof(cmdline), file)) {
                    if (!strncmp(value, cmdline, strlen(value))) {
                        // set EGL debug if the "debug.egl.debug_proc" property
                        // matches the prefix of this application's command line
                        setEGLDebugLevel(1);
                    }
                }
                fclose(file);
            }
        }
    }

    if (getEGLDebugLevel() > 0) {
        if (GLTrace_start() < 0) {
            ALOGE("Error starting Tracer for OpenGL ES. Disabling..");
            setEGLDebugLevel(0);
        }
    }
}

void setGLHooksThreadSpecific(gl_hooks_t const *value) {
    if (sEGLGetErrorEnabled) {
        setGlTraceThreadSpecific(value);
        setGlThreadSpecific(&gHooksErrorTrace);
    } else if (sEGLSystraceEnabled) {
        setGlTraceThreadSpecific(value);
        setGlThreadSpecific(&gHooksSystrace);
    } else if (sEGLTraceLevel > 0) {
        setGlTraceThreadSpecific(value);
        setGlThreadSpecific(&gHooksTrace);
    } else if (getEGLDebugLevel() > 0 && value != &gHooksNoContext) {
        setGlTraceThreadSpecific(value);
        setGlThreadSpecific(GLTrace_getGLHooks());
    } else {
        setGlTraceThreadSpecific(NULL);
        setGlThreadSpecific(value);
    }
}

/*
 * Global entry point to allow applications to modify their own trace level.
 * The effective trace level is the max of this level and the value of debug.egl.trace.
 */
extern "C"
void setGLTraceLevel(int level) {
    sEGLApplicationTraceLevel = level;
}

/*
 * Global entry point to allow applications to modify their own debug level.
 * Debugging is enabled if either the application requested it, or if the system property
 * matches the application's name.
 * Note that this only sets the debug level. The value is read and used either in
 * initEglDebugLevel() if the application hasn't initialized its display yet, or when
 * eglSwapBuffers() is called next.
 */
void EGLAPI setGLDebugLevel(int level) {
    setEGLDebugLevel(level);
}

#else

void setGLHooksThreadSpecific(gl_hooks_t const *value) {
    setGlThreadSpecific(value);
}

#endif

/*****************************************************************************/

static int gl_no_context() {
    if (egl_tls_t::logNoContextCall()) {
        char const* const error = "call to OpenGL ES API with "
                "no current context (logged once per thread)";
        if (LOG_NDEBUG) {
            ALOGE(error);
        } else {
            LOG_ALWAYS_FATAL(error);
        }
        char value[PROPERTY_VALUE_MAX];
        property_get("debug.egl.callstack", value, "0");
        if (atoi(value)) {
            CallStack stack(LOG_TAG);
        }
    }
    return 0;
}

static void early_egl_init(void)
{
#if EGL_TRACE
    pthread_key_create(&gGLTraceKey, NULL);
    initEglTraceLevel();
#endif
    uint32_t addr = (uint32_t)((void*)gl_no_context);
    android_memset32(
            (uint32_t*)(void*)&gHooksNoContext,
            addr,
            sizeof(gHooksNoContext));

    setGLHooksThreadSpecific(&gHooksNoContext);
}

static pthread_once_t once_control = PTHREAD_ONCE_INIT;
static int sEarlyInitState = pthread_once(&once_control, &early_egl_init);

// ----------------------------------------------------------------------------

egl_display_ptr validate_display(EGLDisplay dpy) {
    egl_display_ptr dp = get_display(dpy);
    if (!dp)
        return setError(EGL_BAD_DISPLAY, egl_display_ptr(NULL));
    if (!dp->isReady())
        return setError(EGL_NOT_INITIALIZED, egl_display_ptr(NULL));

    return dp;
}

egl_display_ptr validate_display_connection(EGLDisplay dpy,
        egl_connection_t*& cnx) {
    cnx = NULL;
    egl_display_ptr dp = validate_display(dpy);
    if (!dp)
        return dp;
    cnx = &gEGLImpl;
    if (cnx->dso == 0) {
        return setError(EGL_BAD_CONFIG, egl_display_ptr(NULL));
    }
    return dp;
}

// ----------------------------------------------------------------------------

const GLubyte * egl_get_string_for_current_context(GLenum name) {
    // NOTE: returning NULL here will fall-back to the default
    // implementation.

    EGLContext context = egl_tls_t::getContext();
    if (context == EGL_NO_CONTEXT)
        return NULL;

    egl_context_t const * const c = get_context(context);
    if (c == NULL) // this should never happen, by construction
        return NULL;

    if (name != GL_EXTENSIONS)
        return NULL;

    return (const GLubyte *)c->gl_extensions.string();
}

// ----------------------------------------------------------------------------

// this mutex protects:
//    d->disp[]
//    egl_init_drivers_locked()
//
static EGLBoolean egl_init_drivers_locked() {
    if (sEarlyInitState) {
        // initialized by static ctor. should be set here.
        return EGL_FALSE;
    }

    // get our driver loader
    Loader& loader(Loader::getInstance());

    // dynamically load our EGL implementation
    egl_connection_t* cnx = &gEGLImpl;
    if (cnx->dso == 0) {
        cnx->hooks[egl_connection_t::GLESv1_INDEX] =
                &gHooks[egl_connection_t::GLESv1_INDEX];
        cnx->hooks[egl_connection_t::GLESv2_INDEX] =
                &gHooks[egl_connection_t::GLESv2_INDEX];
        cnx->dso = loader.open(cnx);
    }

    return cnx->dso ? EGL_TRUE : EGL_FALSE;
}

static pthread_mutex_t sInitDriverMutex = PTHREAD_MUTEX_INITIALIZER;

EGLBoolean egl_init_drivers() {
    EGLBoolean res;
    pthread_mutex_lock(&sInitDriverMutex);
    res = egl_init_drivers_locked();
    pthread_mutex_unlock(&sInitDriverMutex);
    return res;
}

void gl_unimplemented() {
    ALOGE("called unimplemented OpenGL ES API");
    char value[PROPERTY_VALUE_MAX];
    property_get("debug.egl.callstack", value, "0");
    if (atoi(value)) {
        CallStack stack(LOG_TAG);
    }
}

void gl_noop() {
}

// ----------------------------------------------------------------------------

void setGlThreadSpecific(gl_hooks_t const *value) {
    gl_hooks_t const * volatile * tls_hooks = get_tls_hooks();
    tls_hooks[TLS_SLOT_OPENGL_API] = value;
}

// ----------------------------------------------------------------------------
// GL / EGL hooks
// ----------------------------------------------------------------------------

#undef GL_ENTRY
#undef EGL_ENTRY
#define GL_ENTRY(_r, _api, ...) #_api,
#define EGL_ENTRY(_r, _api, ...) #_api,

char const * const gl_names[] = {
    #include "../entries.in"
    NULL
};

char const * const egl_names[] = {
    #include "egl_entries.in"
    NULL
};

#undef GL_ENTRY
#undef EGL_ENTRY


// ----------------------------------------------------------------------------
}; // namespace android
// ----------------------------------------------------------------------------

