#define LOG_TAG "ZmbSurface"

#include <android_runtime/AndroidRuntime.h>
#include <android_runtime/android_view_ZmbSurface.h>
#include <unistd.h>
#include <fstream>
#include <sstream>
#include <stdlib.h>
#include "jni.h"
#include <Dalvik.h>
#include "Zombie.h"
#include "MemMap.h"
#include "ds/list.h"

namespace android{

static jboolean nativeInitImage(JNIEnv * env, jclass clazz, jobject wm, jint bitmap_width, jint bitmap_height){
  zmbInit(bitmap_width, bitmap_height);
  int more_dls;
  zmbFindViewRoot(&more_dls, wm);
  return (more_dls!=0); 
}

static jobject nativeGetDL(JNIEnv * env, jclass clazz){
  jobject dl;
  zmbGenerateDL(&dl); 
  return dl;
}


static JNINativeMethod gZmbSurfaceMethods[] = {
    {"initImage", "(Landroid/view/WindowManager;II)Z",
            (void*)nativeInitImage },
    {"getDL", "()Landroid/view/GLES20DisplayList;",
            (void *)nativeGetDL }
};

int register_android_view_ZmbSurface(JNIEnv* env)
{
    int err = AndroidRuntime::registerNativeMethods(env, "android/view/ZmbSurface",
            gZmbSurfaceMethods, NELEM(gZmbSurfaceMethods));
    return err;
}

};
