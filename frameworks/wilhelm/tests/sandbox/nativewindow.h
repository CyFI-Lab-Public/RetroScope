#include <android/native_window_jni.h>

#ifdef __cplusplus
extern "C" {
#endif

extern ANativeWindow *getNativeWindow();
extern void disposeNativeWindow();

#ifdef __cplusplus
}
#endif
