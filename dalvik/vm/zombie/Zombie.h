#ifndef DALVIK_ZOMBIE_ZOMBIE_H_
#define DALVIK_ZOMBIE_ZOMBIE_H_

#ifndef ZMB_EXTERNAL

#include "../Dalvik.h"
#include "./ds/list.h"
#endif // !ZMB_EXTERNAL

#include "Log.h"

// someday we may receive these from somewhere
static const char mem_file_name[] = "/sdcard/mem.m";
static const char map_file_name[] = "/system/usr/data/map.m";

extern bool zmbIsRunning();

#ifndef ZMB_EXTERNAL

extern DvmGlobals* zmb_gDvm;
extern "C" Object * getTopViewOnStack();

/**
 * Get the system running
 */
void zmbInit(int bitmap_width, int bitmap_height); 

void zmbFindViewRoot(int * more_dls, jobject wm);
void zmbGenerateDL(jobject* dl);

void zmbAddZombieObject(const Object * obj);
bool zmbIsZombieObject(const Object * obj);
void zmbRemoveZombieObject(const Object * obj);

#endif // !ZMB_EXTERNAL

struct ExecMetrics {
  unsigned byte_insns;  // GOTO_OPCODE(ip)
  unsigned native_calls; // invoke-native
  unsigned java_ds; // dvmAlloc
  unsigned new_c_ds; // malloc
  unsigned old_c_ds;  // mallocHook
};

extern struct ExecMetrics * curr_metrics;

#ifdef __cplusplus
extern "C" {
#endif
void zmb_incr_old_c_ds();
void zmb_incr_new_c_ds();
void zmb_incr_java_ds();
void zmb_incr_native_calls();
void zmb_incr_insns();
#ifdef __cplusplus
}
#endif

#endif // DALVIK_ZOMBIE_ZOMBIE_H_
