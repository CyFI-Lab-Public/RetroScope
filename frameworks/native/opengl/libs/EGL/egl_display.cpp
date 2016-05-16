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

#define __STDC_LIMIT_MACROS 1

#include <string.h>

#include "../egl_impl.h"

#include "egl_cache.h"
#include "egl_display.h"
#include "egl_object.h"
#include "egl_tls.h"
#include "Loader.h"
#include <cutils/properties.h>

// ----------------------------------------------------------------------------
namespace android {
// ----------------------------------------------------------------------------

static char const * const sVendorString     = "Android";
static char const * const sVersionString    = "1.4 Android META-EGL";
static char const * const sClientApiString  = "OpenGL_ES";

extern char const * const gBuiltinExtensionString;
extern char const * const gExtensionString;

extern void initEglTraceLevel();
extern void initEglDebugLevel();
extern void setGLHooksThreadSpecific(gl_hooks_t const *value);

// ----------------------------------------------------------------------------

static bool findExtension(const char* exts, const char* name, size_t nameLen) {
    if (exts) {
        const char* match = strstr(exts, name);
        if (match && (match[nameLen] == '\0' || match[nameLen] == ' ')) {
            return true;
        }
    }
    return false;
}

egl_display_t egl_display_t::sDisplay[NUM_DISPLAYS];

egl_display_t::egl_display_t() :
    magic('_dpy'), finishOnSwap(false), traceGpuCompletion(false), refs(0) {
}

egl_display_t::~egl_display_t() {
    magic = 0;
    egl_cache_t::get()->terminate();
}

egl_display_t* egl_display_t::get(EGLDisplay dpy) {
    uintptr_t index = uintptr_t(dpy)-1U;
    return (index >= NUM_DISPLAYS) ? NULL : &sDisplay[index];
}

void egl_display_t::addObject(egl_object_t* object) {
    Mutex::Autolock _l(lock);
    objects.add(object);
}

void egl_display_t::removeObject(egl_object_t* object) {
    Mutex::Autolock _l(lock);
    objects.remove(object);
}

bool egl_display_t::getObject(egl_object_t* object) const {
    Mutex::Autolock _l(lock);
    if (objects.indexOf(object) >= 0) {
        if (object->getDisplay() == this) {
            object->incRef();
            return true;
        }
    }
    return false;
}

EGLDisplay egl_display_t::getFromNativeDisplay(EGLNativeDisplayType disp) {
    if (uintptr_t(disp) >= NUM_DISPLAYS)
        return NULL;

    return sDisplay[uintptr_t(disp)].getDisplay(disp);
}

EGLDisplay egl_display_t::getDisplay(EGLNativeDisplayType display) {

    Mutex::Autolock _l(lock);

    // get our driver loader
    Loader& loader(Loader::getInstance());

    egl_connection_t* const cnx = &gEGLImpl;
    if (cnx->dso && disp.dpy == EGL_NO_DISPLAY) {
        EGLDisplay dpy = cnx->egl.eglGetDisplay(display);
        disp.dpy = dpy;
        if (dpy == EGL_NO_DISPLAY) {
            loader.close(cnx->dso);
            cnx->dso = NULL;
        }
    }

    return EGLDisplay(uintptr_t(display) + 1U);
}

EGLBoolean egl_display_t::initialize(EGLint *major, EGLint *minor) {

    Mutex::Autolock _l(lock);

    if (refs > 0) {
        if (major != NULL)
            *major = VERSION_MAJOR;
        if (minor != NULL)
            *minor = VERSION_MINOR;
        refs++;
        return EGL_TRUE;
    }

#if EGL_TRACE

    // Called both at early_init time and at this time. (Early_init is pre-zygote, so
    // the information from that call may be stale.)
    initEglTraceLevel();
    initEglDebugLevel();

#endif

    setGLHooksThreadSpecific(&gHooksNoContext);

    // initialize each EGL and
    // build our own extension string first, based on the extension we know
    // and the extension supported by our client implementation

    egl_connection_t* const cnx = &gEGLImpl;
    cnx->major = -1;
    cnx->minor = -1;
    if (cnx->dso) {
        EGLDisplay idpy = disp.dpy;
        if (cnx->egl.eglInitialize(idpy, &cnx->major, &cnx->minor)) {
            //ALOGD("initialized dpy=%p, ver=%d.%d, cnx=%p",
            //        idpy, cnx->major, cnx->minor, cnx);

            // display is now initialized
            disp.state = egl_display_t::INITIALIZED;

            // get the query-strings for this display for each implementation
            disp.queryString.vendor = cnx->egl.eglQueryString(idpy,
                    EGL_VENDOR);
            disp.queryString.version = cnx->egl.eglQueryString(idpy,
                    EGL_VERSION);
            disp.queryString.extensions = cnx->egl.eglQueryString(idpy,
                    EGL_EXTENSIONS);
            disp.queryString.clientApi = cnx->egl.eglQueryString(idpy,
                    EGL_CLIENT_APIS);

        } else {
            ALOGW("eglInitialize(%p) failed (%s)", idpy,
                    egl_tls_t::egl_strerror(cnx->egl.eglGetError()));
        }
    }

    // the query strings are per-display
    mVendorString.setTo(sVendorString);
    mVersionString.setTo(sVersionString);
    mClientApiString.setTo(sClientApiString);

    mExtensionString.setTo(gBuiltinExtensionString);
    char const* start = gExtensionString;
    char const* end;
    do {
        // find the space separating this extension for the next one
        end = strchr(start, ' ');
        if (end) {
            // length of the extension string
            const size_t len = end - start;
            if (len) {
                // NOTE: we could avoid the copy if we had strnstr.
                const String8 ext(start, len);
                if (findExtension(disp.queryString.extensions, ext.string(),
                        len)) {
                    mExtensionString.append(start, len+1);
                }
            }
            // process the next extension string, and skip the space.
            start = end + 1;
        }
    } while (end);

    egl_cache_t::get()->initialize(this);

    char value[PROPERTY_VALUE_MAX];
    property_get("debug.egl.finish", value, "0");
    if (atoi(value)) {
        finishOnSwap = true;
    }

    property_get("debug.egl.traceGpuCompletion", value, "0");
    if (atoi(value)) {
        traceGpuCompletion = true;
    }

    refs++;
    if (major != NULL)
        *major = VERSION_MAJOR;
    if (minor != NULL)
        *minor = VERSION_MINOR;

    mHibernation.setDisplayValid(true);

    return EGL_TRUE;
}

EGLBoolean egl_display_t::terminate() {

    Mutex::Autolock _l(lock);

    if (refs == 0) {
        /*
         * From the EGL spec (3.2):
         * "Termination of a display that has already been terminated,
         *  (...), is allowed, but the only effect of such a call is
         *  to return EGL_TRUE (...)
         */
        return EGL_TRUE;
    }

    // this is specific to Android, display termination is ref-counted.
    if (refs > 1) {
        refs--;
        return EGL_TRUE;
    }

    EGLBoolean res = EGL_FALSE;
    egl_connection_t* const cnx = &gEGLImpl;
    if (cnx->dso && disp.state == egl_display_t::INITIALIZED) {
        if (cnx->egl.eglTerminate(disp.dpy) == EGL_FALSE) {
            ALOGW("eglTerminate(%p) failed (%s)", disp.dpy,
                    egl_tls_t::egl_strerror(cnx->egl.eglGetError()));
        }
        // REVISIT: it's unclear what to do if eglTerminate() fails
        disp.state = egl_display_t::TERMINATED;
        res = EGL_TRUE;
    }

    mHibernation.setDisplayValid(false);

    // Reset the extension string since it will be regenerated if we get
    // reinitialized.
    mExtensionString.setTo("");

    // Mark all objects remaining in the list as terminated, unless
    // there are no reference to them, it which case, we're free to
    // delete them.
    size_t count = objects.size();
    ALOGW_IF(count, "eglTerminate() called w/ %d objects remaining", count);
    for (size_t i=0 ; i<count ; i++) {
        egl_object_t* o = objects.itemAt(i);
        o->destroy();
    }

    // this marks all object handles are "terminated"
    objects.clear();

    refs--;
    return res;
}

void egl_display_t::loseCurrent(egl_context_t * cur_c)
{
    if (cur_c) {
        egl_display_t* display = cur_c->getDisplay();
        if (display) {
            display->loseCurrentImpl(cur_c);
        }
    }
}

void egl_display_t::loseCurrentImpl(egl_context_t * cur_c)
{
    // by construction, these are either 0 or valid (possibly terminated)
    // it should be impossible for these to be invalid
    ContextRef _cur_c(cur_c);
    SurfaceRef _cur_r(cur_c ? get_surface(cur_c->read) : NULL);
    SurfaceRef _cur_d(cur_c ? get_surface(cur_c->draw) : NULL);

    { // scope for the lock
        Mutex::Autolock _l(lock);
        cur_c->onLooseCurrent();

    }

    // This cannot be called with the lock held because it might end-up
    // calling back into EGL (in particular when a surface is destroyed
    // it calls ANativeWindow::disconnect
    _cur_c.release();
    _cur_r.release();
    _cur_d.release();
}

EGLBoolean egl_display_t::makeCurrent(egl_context_t* c, egl_context_t* cur_c,
        EGLSurface draw, EGLSurface read, EGLContext ctx,
        EGLSurface impl_draw, EGLSurface impl_read, EGLContext impl_ctx)
{
    EGLBoolean result;

    // by construction, these are either 0 or valid (possibly terminated)
    // it should be impossible for these to be invalid
    ContextRef _cur_c(cur_c);
    SurfaceRef _cur_r(cur_c ? get_surface(cur_c->read) : NULL);
    SurfaceRef _cur_d(cur_c ? get_surface(cur_c->draw) : NULL);

    { // scope for the lock
        Mutex::Autolock _l(lock);
        if (c) {
            result = c->cnx->egl.eglMakeCurrent(
                    disp.dpy, impl_draw, impl_read, impl_ctx);
            if (result == EGL_TRUE) {
                c->onMakeCurrent(draw, read);
                if (!cur_c) {
                    mHibernation.incWakeCount(HibernationMachine::STRONG);
                }
            }
        } else {
            result = cur_c->cnx->egl.eglMakeCurrent(
                    disp.dpy, impl_draw, impl_read, impl_ctx);
            if (result == EGL_TRUE) {
                cur_c->onLooseCurrent();
                mHibernation.decWakeCount(HibernationMachine::STRONG);
            }
        }
    }

    if (result == EGL_TRUE) {
        // This cannot be called with the lock held because it might end-up
        // calling back into EGL (in particular when a surface is destroyed
        // it calls ANativeWindow::disconnect
        _cur_c.release();
        _cur_r.release();
        _cur_d.release();
    }

    return result;
}

bool egl_display_t::haveExtension(const char* name, size_t nameLen) const {
    if (!nameLen) {
        nameLen = strlen(name);
    }
    return findExtension(mExtensionString.string(), name, nameLen);
}

// ----------------------------------------------------------------------------

bool egl_display_t::HibernationMachine::incWakeCount(WakeRefStrength strength) {
    Mutex::Autolock _l(mLock);
    ALOGE_IF(mWakeCount < 0 || mWakeCount == INT32_MAX,
             "Invalid WakeCount (%d) on enter\n", mWakeCount);

    mWakeCount++;
    if (strength == STRONG)
        mAttemptHibernation = false;

    if (CC_UNLIKELY(mHibernating)) {
        ALOGV("Awakening\n");
        egl_connection_t* const cnx = &gEGLImpl;

        // These conditions should be guaranteed before entering hibernation;
        // we don't want to get into a state where we can't wake up.
        ALOGD_IF(!mDpyValid || !cnx->egl.eglAwakenProcessIMG,
                 "Invalid hibernation state, unable to awaken\n");

        if (!cnx->egl.eglAwakenProcessIMG()) {
            ALOGE("Failed to awaken EGL implementation\n");
            return false;
        }
        mHibernating = false;
    }
    return true;
}

void egl_display_t::HibernationMachine::decWakeCount(WakeRefStrength strength) {
    Mutex::Autolock _l(mLock);
    ALOGE_IF(mWakeCount <= 0, "Invalid WakeCount (%d) on leave\n", mWakeCount);

    mWakeCount--;
    if (strength == STRONG)
        mAttemptHibernation = true;

    if (mWakeCount == 0 && CC_UNLIKELY(mAttemptHibernation)) {
        egl_connection_t* const cnx = &gEGLImpl;
        mAttemptHibernation = false;
        if (mAllowHibernation && mDpyValid &&
                cnx->egl.eglHibernateProcessIMG &&
                cnx->egl.eglAwakenProcessIMG) {
            ALOGV("Hibernating\n");
            if (!cnx->egl.eglHibernateProcessIMG()) {
                ALOGE("Failed to hibernate EGL implementation\n");
                return;
            }
            mHibernating = true;
        }
    }
}

void egl_display_t::HibernationMachine::setDisplayValid(bool valid) {
    Mutex::Autolock _l(mLock);
    mDpyValid = valid;
}

// ----------------------------------------------------------------------------
}; // namespace android
// ----------------------------------------------------------------------------
