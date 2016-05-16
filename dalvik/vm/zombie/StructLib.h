#ifndef DALVIK_ZOMBIE_STRUCTLIB_H_
#define DALVIK_ZOMBIE_STRUCTLIB_H_


#include "Zombie.h"

//void convertUtf16ToUtf8(char* utf8Str, const u2* utf16Str, int len);
//
//////////////////////////////////////////////
////// ALL FUNCTIONS IN THIS FILE ASSUME THAT
////// THE MEMORY IMAGE HAS NOT BEEN MOVED
////// INTO THE NEW ADDRESS SPACE!!
//////////////////////////////////////////////

/**
 * Find the old gDvm and gHs struct in the old memory
 */
void zmbFindOldGlobals (DvmGlobals** out_gDvm, /*HeapSource*/void** out_gHs);

//char * zmbGetThreadName(Thread* translatedTP);
//
//char * zmbAllocCString(StringObject* jstr);

ClassObject* zmbFindOldLoadedClass(DvmGlobals* gDvm_ptr, const char* descriptor);

static inline ClassObject* zmbFindOldLoadedClass(const char* descriptor) {
  return zmbFindOldLoadedClass(zmb_gDvm, descriptor);
}

// Find the new method for the given old instance method
//extern "C" const Method* zmbFindMatchingNewMethodC(const Method* old_method);
Method* zmbFindMatchingNewMethod(const ClassObject* old_class, const Method* old_method);

// Returns an UNTRANSLATED pointer to the old method matching the given new method.
Method* zmbFindMatchingOldMethod(DvmGlobals* gDvm_ptr, const ClassObject* old_class, const Method* new_method);

//
//// Get a translated object* from the given field in the translated host object
//Object* zmbGetInstanceObjectField(const Object* host,
//    const char* fieldName, const char* signature);

// Get a NOT translated object* from the given field in the translated host object
Object* zmbGetInstanceOldObjectField(const Object* host,
    const char* fieldName, const char* signature);


JValue* zmbGetInstanceOldField(const Object* host,
    const char* fieldName, const char* signature);

// loop each instance field in a translated host object -- No Translate
#define zmbForEachFieldInObjectNoTranslate(pFld, clz) 			\
  for(ClassObject* __clz = clz;							\
      __clz != NULL;								\
      __clz = (ClassObject*)(__clz->super))					\
    for(InstField* clz_end = 							\
          ({ pFld = (InstField*)(__clz->ifields);				\
             pFld + __clz->ifieldCount; });					\
        (pFld!=NULL && pFld < clz_end);						\
        pFld++)


// loop each instance field in a translated host object
#define zmbForEachFieldInObject(pJv, pFld, host) 				\
  for(ClassObject* __clz = (ClassObject*)zmbTranslateAddr(host->clazz);		\
      __clz != NULL;								\
      __clz = (ClassObject*)zmbTranslateAddr(__clz->super))			\
    for(InstField* clz_end = 							\
          ({ pFld = (InstField*)zmbTranslateAddr(__clz->ifields);		\
             pFld + __clz->ifieldCount; });					\
        (pFld!=NULL && ({ (pFld < clz_end							\
             ? ({ pJv = (JValue*)((void*)(((u1*)(host)) + (pFld->byteOffset))); true; }) 	\
             : false ); }));							\
        pFld++)


// Is this object (or one of it's super's) a type of the given descriptor? Uses the given addr trans func)
bool zmbIsTypeOrHeir(Object* o, const char* descriptor, void *(*trans_func)(const void*));

//// Get the name of the class of the host object
//char* zmbGetClazzDescriptor(const Object* host);
//
//Thread* zmbThreadMain(DvmGlobals* gDvm_ptr);
//
//// Get a translated pointer to the ActivityThread object in old_t's stack
//Object* zmbActivityThread(DvmGlobals* gDvm_ptr, Thread* old_t);
//
///**
// * Call cb_func with NOT translated pointers to each ActivityClientRecord Object registered to 
// * the given translated ActivityThread pointer.
// */
//void zmbLoopOldActivityClientRecords(Object* old_ActivityThread, void (*cb_func)(Object*, void*), void* args);
//
///**
// * Call cb_func with NOT translated pointers to each Activity Object registered to 
// * the given translated ActivityThread pointer.
// */
//void zmbLoopOldActivities(Object* old_ActivityThread, void (*cb_func)(Object*, void*), void* args);
//
/**
 * An iterator for a HashTable in the memory image. Use the same HashIter as normal.
 */
void zmbHashIterNext(HashIter* pIter);
void zmbHashIterBegin(HashTable* pHashTable, HashIter* pIter);
bool zmbHashIterDone(HashIter* pIter);
void* zmbHashIterData(HashIter* pIter);
void* zmbHashIterData_NoTranslate(HashIter* pIter);

char* zmbDexStringByTypeIdx(const DexFile* pDexFile, u4 idx);
char* zmbGetDescFromProto(const DexProto* pProto);


///**
// * Walk the objects in the given threads stack (starting at start_fp). Calls trans_func to
// * handle addr translations. cb_func is called with a pointer to the Object prt in the
// * stack (so it can be changed). If cb_func returns non-zero then walking stops.
// */
//void zmbWalkStackObjs(Thread* t, const u4* start_fp, void *(*trans_func)(const void*),
//                   int (*cb_func)(Object**, Method*, void*), void* arg);
//
//void zmbWalkStackObjs(Thread* t, void *(*trans_func)(const void*),
//                   int (*cb_func)(Object**, Method*, void*), void* arg);
#endif // DALVIK_ZOMBIE_STRUCTLIB_H_
