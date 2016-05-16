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

#ifndef ANDROID_EGL_DISPLAY_H
#define ANDROID_EGL_DISPLAY_H


#include <ctype.h>
#include <stdint.h>
#include <stdlib.h>

#include <EGL/egl.h>
#include <EGL/eglext.h>

#include <cutils/compiler.h>
#include <utils/SortedVector.h>
#include <utils/threads.h>
#include <utils/String8.h>

#include "egldefs.h"
#include "../hooks.h"

// ----------------------------------------------------------------------------
namespace android {
// ----------------------------------------------------------------------------

class egl_object_t;
class egl_context_t;
class egl_connection_t;

// ----------------------------------------------------------------------------

class EGLAPI egl_display_t { // marked as EGLAPI for testing purposes
    static egl_display_t sDisplay[NUM_DISPLAYS];
    EGLDisplay getDisplay(EGLNativeDisplayType display);
    void loseCurrentImpl(egl_context_t * cur_c);

public:
    enum {
        NOT_INITIALIZED = 0,
        INITIALIZED     = 1,
        TERMINATED      = 2
    };

    egl_display_t();
    ~egl_display_t();

    EGLBoolean initialize(EGLint *major, EGLint *minor);
    EGLBoolean terminate();

    // add object to this display's list
    void addObject(egl_object_t* object);
    // remove object from this display's list
    void removeObject(egl_object_t* object);
    // add reference to this object. returns true if this is a valid object.
    bool getObject(egl_object_t* object) const;

    // These notifications allow the display to keep track of how many window
    // surfaces exist, which it uses to decide whether to hibernate the
    // underlying EGL implementation. They can be called by any thread without
    // holding a lock, but must be called via egl_display_ptr to ensure
    // proper hibernate/wakeup sequencing. If a surface destruction triggers
    // hibernation, hibernation will be delayed at least until the calling
    // thread's egl_display_ptr is destroyed.
    void onWindowSurfaceCreated() {
        mHibernation.incWakeCount(HibernationMachine::STRONG);
    }
    void onWindowSurfaceDestroyed() {
        mHibernation.decWakeCount(HibernationMachine::STRONG);
    }

    static egl_display_t* get(EGLDisplay dpy);
    static EGLDisplay getFromNativeDisplay(EGLNativeDisplayType disp);

    EGLBoolean makeCurrent(egl_context_t* c, egl_context_t* cur_c,
            EGLSurface draw, EGLSurface read, EGLContext ctx,
            EGLSurface impl_draw, EGLSurface impl_read, EGLContext impl_ctx);
    static void loseCurrent(egl_context_t * cur_c);

    inline bool isReady() const { return (refs > 0); }
    inline bool isValid() const { return magic == '_dpy'; }
    inline bool isAlive() const { return isValid(); }

    char const * getVendorString() const { return mVendorString.string(); }
    char const * getVersionString() const { return mVersionString.string(); }
    char const * getClientApiString() const { return mClientApiString.string(); }
    char const * getExtensionString() const { return mExtensionString.string(); }

    bool haveExtension(const char* name, size_t nameLen = 0) const;

    inline uint32_t getRefsCount() const { return refs; }

    struct strings_t {
        char const * vendor;
        char const * version;
        char const * clientApi;
        char const * extensions;
    };

    struct DisplayImpl {
        DisplayImpl() : dpy(EGL_NO_DISPLAY), state(NOT_INITIALIZED) { }
        EGLDisplay  dpy;
        EGLint      state;
        strings_t   queryString;
    };

private:
    uint32_t        magic;

public:
    DisplayImpl     disp;
    bool    finishOnSwap;       // property: debug.egl.finish
    bool    traceGpuCompletion; // property: debug.egl.traceGpuCompletion

private:
    friend class egl_display_ptr;
    bool enter() { return mHibernation.incWakeCount(HibernationMachine::WEAK); }
    void leave() { return mHibernation.decWakeCount(HibernationMachine::WEAK); }

            uint32_t                    refs;
    mutable Mutex                       lock;
            SortedVector<egl_object_t*> objects;
            String8 mVendorString;
            String8 mVersionString;
            String8 mClientApiString;
            String8 mExtensionString;

    // HibernationMachine uses its own internal mutex to protect its own data.
    // The owning egl_display_t's lock may be but is not required to be held
    // when calling HibernationMachine methods. As a result, nothing in this
    // class may call back up to egl_display_t directly or indirectly.
    class HibernationMachine {
    public:
        // STRONG refs cancel (inc) or initiate (dec) a hibernation attempt
        // the next time the wakecount reaches zero. WEAK refs don't affect
        // whether a hibernation attempt will be made. Use STRONG refs only
        // for infrequent/heavy changes that are likely to indicate the
        // EGLDisplay is entering or leaving a long-term idle state.
        enum WakeRefStrength {
            WEAK   = 0,
            STRONG = 1,
        };

        HibernationMachine(): mWakeCount(0), mHibernating(false),
                mAttemptHibernation(false), mDpyValid(false),
#if BOARD_ALLOW_EGL_HIBERNATION
                mAllowHibernation(true)
#else
                mAllowHibernation(false)
#endif
        {}
        ~HibernationMachine() {}

        bool incWakeCount(WakeRefStrength strenth);
        void decWakeCount(WakeRefStrength strenth);

        void setDisplayValid(bool valid);

    private:
        Mutex      mLock;
        int32_t    mWakeCount;
        bool       mHibernating;
        bool       mAttemptHibernation;
        bool       mDpyValid;
        const bool mAllowHibernation;
    };
    HibernationMachine mHibernation;
};

// ----------------------------------------------------------------------------

// An egl_display_ptr is a kind of smart pointer for egl_display_t objects.
// It doesn't refcount the egl_display_t, but does ensure that the underlying
// EGL implementation is "awake" (not hibernating) and ready for use as long
// as the egl_display_ptr exists.
class egl_display_ptr {
public:
    explicit egl_display_ptr(egl_display_t* dpy): mDpy(dpy) {
        if (mDpy) {
            if (CC_UNLIKELY(!mDpy->enter())) {
                mDpy = NULL;
            }
        }
    }

    // We only really need a C++11 move constructor, not a copy constructor.
    // A move constructor would save an enter()/leave() pair on every EGL API
    // call. But enabling -std=c++0x causes lots of errors elsewhere, so I
    // can't use a move constructor until those are cleaned up.
    //
    // egl_display_ptr(egl_display_ptr&& other) {
    //     mDpy = other.mDpy;
    //     other.mDpy = NULL;
    // }
    //
    egl_display_ptr(const egl_display_ptr& other): mDpy(other.mDpy) {
        if (mDpy) {
            mDpy->enter();
        }
    }

    ~egl_display_ptr() {
        if (mDpy) {
            mDpy->leave();
        }
    }

    const egl_display_t* operator->() const { return mDpy; }
          egl_display_t* operator->()       { return mDpy; }

    const egl_display_t* get() const { return mDpy; }
          egl_display_t* get()       { return mDpy; }

    operator bool() const { return mDpy != NULL; }

private:
    egl_display_t* mDpy;

    // non-assignable
    egl_display_ptr& operator=(const egl_display_ptr&);
};

// ----------------------------------------------------------------------------

inline egl_display_ptr get_display(EGLDisplay dpy) {
    return egl_display_ptr(egl_display_t::get(dpy));
}

// Does not ensure EGL is unhibernated. Use with caution: calls into the
// underlying EGL implementation are not safe.
inline egl_display_t* get_display_nowake(EGLDisplay dpy) {
    return egl_display_t::get(dpy);
}

// ----------------------------------------------------------------------------

egl_display_ptr validate_display(EGLDisplay dpy);
egl_display_ptr validate_display_connection(EGLDisplay dpy,
        egl_connection_t*& cnx);
EGLBoolean validate_display_context(EGLDisplay dpy, EGLContext ctx);
EGLBoolean validate_display_surface(EGLDisplay dpy, EGLSurface surface);

// ----------------------------------------------------------------------------
}; // namespace android
// ----------------------------------------------------------------------------

#endif // ANDROID_EGL_DISPLAY_H
