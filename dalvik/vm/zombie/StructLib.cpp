
#include "StructLib.h"
#include "MemMap.h"
#include "ds/rb_tree.h"
#include <stdio.h>
#include <ctype.h>
#include <stdlib.h>
#include <string.h>

#include "alloc/HeapSource.h"

/****************************
 * Copied from UtfString.cpp
 */
//static int utf16_utf8ByteLen(const u2* utf16Str, int len)
//{
//    int utf8Len = 0;
//
//    while (len--) {
//        unsigned int uic = *utf16Str++;
//
//        /*
//         * The most common case is (uic > 0 && uic <= 0x7f).
//         */
//        if (uic == 0 || uic > 0x7f) {
//            if (uic > 0x07ff)
//                utf8Len += 3;
//            else /*(uic > 0x7f || uic == 0) */
//                utf8Len += 2;
//        } else
//            utf8Len++;
//    }
//    return utf8Len;
//}
//void convertUtf16ToUtf8(char* utf8Str, const u2* utf16Str, int len)
//{
//    assert(len >= 0);
//
//    while (len--) {
//        unsigned int uic = *utf16Str++;
//
//        /*
//         * The most common case is (uic > 0 && uic <= 0x7f).
//         */
//        if (uic == 0) break;
//        else if(uic > 0x7f) {
//            if (uic > 0x07ff) {
//                *utf8Str++ = (uic >> 12) | 0xe0;
//                *utf8Str++ = ((uic >> 6) & 0x3f) | 0x80;
//                *utf8Str++ = (uic & 0x3f) | 0x80;
//            } else /*uic > 0x7f*/ {
//                *utf8Str++ = (uic >> 6) | 0xc0;
//                *utf8Str++ = (uic & 0x3f) | 0x80;
//            }
//        } else {
//            *utf8Str++ = uic;
//        }
//    }
//
//    *utf8Str = '\0';
//}
///****************************
// * End copy from UtfString.cpp
// */
//
//
//char * zmbAllocCString(StringObject* jstr)
//{
//  int len = dvmGetFieldInt(jstr, gDvm.offJavaLangString_count);
//  int offset = dvmGetFieldInt(jstr, gDvm.offJavaLangString_offset);
//  ArrayObject* chars =
//    (ArrayObject*) zmbTranslateFieldObject(jstr, gDvm.offJavaLangString_value);
//  u2* data = ((u2*)((void*)chars->contents)) + offset;
//  int byteLen = utf16_utf8ByteLen(data, len);
//  char* newStr = (char*) malloc(byteLen+1);
//  if (newStr == NULL) {
//      return NULL;
//  }
//  convertUtf16ToUtf8(newStr, data, len);
//  return newStr; 
//}
//
//char * zmbGetThreadName(Thread* translatedTP)
//{
//  Object* tObj = (Object*)zmbTranslateAddr(translatedTP->threadObj);
//  StringObject* nameObj =
//    (StringObject*) zmbTranslateFieldObject(tObj,  gDvm.offJavaLangThread_name);
//  return zmbAllocCString(nameObj);
//}


/**
 * Brute force for the actual structure...
 */
DvmGlobals* checkSegForGDvm(MemSeg *m)
{
  static const char core_jar[] = "/system/framework/core.jar";

  ZMB_LOG("Checking Segment %s for gDvm", m->name);
  u1* ptr = (u1*) m->start;
  u1* seg_end = (u1*)((char*)ptr + m->len);

  for(; ptr < seg_end; ptr++)
  {
    DvmGlobals* gDvm_ptr = (DvmGlobals*)ptr;
    char * bootClassPathStr = 
        gDvm_ptr->bootClassPathStr;

    if (bootClassPathStr && zmbIsSafeAddr(bootClassPathStr, sizeof(core_jar)*3))
    {
      bool printable_string = true;
      for (unsigned i=0; i<sizeof(core_jar)*3; i++)
      {
        if (!isprint(bootClassPathStr[i]) || bootClassPathStr[i]==255) 
        {
          printable_string = false;
          break;
        }
      }

      if (printable_string && strstr(bootClassPathStr, core_jar))
      {
        if (gDvm_ptr->heapMaximumSize > 0) // That's enough ... I hope :P
        {
          return gDvm_ptr;
        }
      }
    }
  }
  return NULL;
}

/*
void* checkSegForGHs(MemSeg *m)
{
  u4* ptr = (u4*) zmbTranslateAddr((void*)m->start);
  u4* seg_end = (u4*)((char*)ptr + m->len);
  
  for(; ptr < seg_end; ptr++)
  {
    if(zmbIsGoodHeapSource((void*)ptr))
      return (void*)ptr;
  }
  return NULL;
}
*/

struct checkGlobalsArgs {
  DvmGlobals* gDvm_ptr;
  void* gHs_ptr;
};

int checkGlobals(MemSeg* m, void* arg)
{
  static const char seg_libdvm[] = "/system/lib/libdvm.so";
  checkGlobalsArgs* args = (checkGlobalsArgs*)arg;
  if(strstr(m->name, seg_libdvm) != NULL)
  {
    if(args->gDvm_ptr == NULL)
      args->gDvm_ptr = checkSegForGDvm(m);
 //   if(args->gHs_ptr == NULL)
 //     args->gHs_ptr = checkSegForGHs(m);
 //   if(args->gDvm_ptr != NULL && args->gHs_ptr != NULL) return 1; // stop looping
    if(args->gDvm_ptr != NULL) return 1; // stop looping
  }
  return 0; //keep looping
}

/**
 * Find the old gDvm and gHs struct in the old memory
 */
void zmbFindOldGlobals (DvmGlobals** out_gDvm, void** out_gHs)
{
  checkGlobalsArgs args = {NULL, NULL};
  zmbForEachMemSeg(checkGlobals, &args);
  *out_gDvm = args.gDvm_ptr;
  *out_gHs = args.gHs_ptr;
}

ClassObject* zmbFindOldLoadedClass(DvmGlobals* gDvm_ptr, const char* descriptor)
{
  HashIter iter;
  for (zmbHashIterBegin((HashTable*)zmbTranslateAddr(gDvm_ptr->loadedClasses), &iter);
       !zmbHashIterDone(&iter);
       zmbHashIterNext(&iter))
  {
    ClassObject* old_clazz = (ClassObject*) zmbHashIterData_NoTranslate(&iter);
    const char* descr = old_clazz->descriptor; 
    if(strcmp(descr, descriptor) == 0) return old_clazz;
  }
  return NULL;
}

Method* zmbFindMatchingNewMethod(const ClassObject* old_class, const Method* old_method)
{
  char* old_c_desc = (char*)zmbTranslateAddr((void*)old_class->descriptor);
  char* old_m_name = (char*)zmbTranslateAddr((void*)old_method->name);
  char* old_m_desc = zmbGetDescFromProto(&(old_method->prototype));

  ClassObject* new_class = dvmFindLoadedClass(old_c_desc);

  Method* method = dvmFindDirectMethodHierByDescriptor(new_class,
           old_m_name, old_m_desc);

  if (method == NULL) {
    method = dvmFindVirtualMethodHierByDescriptor(new_class,
      old_m_name, old_m_desc);
  }

  free(old_m_desc);
  return method;
}


// DO NOT CALL THIS FUNCTION DIRECTLY!!!!!!!!!!!!!!!!
// CALL zmbFindMatchingOldMethod
Method* findOldMethod(const Method* new_meth, Method* old_meths, int count)
{
  char* new_m_desc = dexProtoCopyMethodDescriptor(&(new_meth->prototype));
  Method* ret = NULL;
  for (int i = 0; i < count; i++) {
    Method* old_meth = old_meths + i;
    char* old_m_desc = zmbGetDescFromProto(&(old_meth->prototype));
    if(strcmp(new_meth->name, (char*)old_meth->name) == 0 &&
       strcmp(new_m_desc, old_m_desc) == 0) {
      ret = old_meths + i; // return the UNTRANSLATED ptr!
      break;
    }
  }
  free(new_m_desc);
  return ret;
}

// Returns an UNTRANSLATED pointer to the old method matching the given new method.
Method* zmbFindMatchingOldMethod(DvmGlobals* gDvm_ptr, const ClassObject* old_class, const Method* new_method)
{
  Method* ret = NULL;
  while (ret == NULL && old_class != NULL) 
  {
    ret = findOldMethod(new_method, old_class->directMethods, old_class->directMethodCount);
    if(ret == NULL) ret = findOldMethod(new_method, old_class->virtualMethods, old_class->virtualMethodCount);
    if(ret == NULL) old_class = old_class->super;
  }
  return ret;
}

//// Get a translated object* from the given field in the translated host object
//Object* zmbGetInstanceObjectField(const Object* host,
//    const char* fieldName, const char* signature)
//{
//  return (Object*)zmbTranslateAddr(
//	zmbGetInstanceOldObjectField(host, fieldName, signature));
//}
//
// Get a NOT translated object* from the given field in the translated host object
Object* zmbGetInstanceOldObjectField(const Object* host,
    const char* fieldName, const char* signature)
{
  JValue* jv = zmbGetInstanceOldField(host, fieldName, signature);
  if(jv == NULL) return NULL;
  return jv->l;
}

// Get a JValue for the given (already translated) object's field
JValue* zmbGetInstanceOldField(const Object* host,
    const char* fieldName, const char* signature)
{
  JValue* jv;
  InstField* fld;

  zmbForEachFieldInObject(jv, fld, host)
  {
    char * fldName = (char*)zmbTranslateAddr((void*)fld->name);
    if(fldName == NULL) return NULL; // something is wrong!
    if(strcmp(fieldName, fldName) != 0) continue; // cannot be a match
    if(signature != NULL)
    {
      char * fldSig = (char*)zmbTranslateAddr((void*)fld->signature);
      if(fldSig == NULL) return NULL; // something is wrong!
      if(strcmp(signature, fldSig) != 0) continue; // cannot be a match
    }
    return jv; // all have passed. so match!
  }
  return NULL;
}

//// Get the name of the class of the host object
//char* zmbGetClazzDescriptor(const Object* host)
//{
//  ClassObject* clz = (ClassObject*)zmbTranslateAddr(host->clazz);
//  return (char*)zmbTranslateAddr((void*)clz->descriptor);
//}

bool zmbIsTypeOrHeir(Object* o, const char* descriptor, void *(*trans_func)(const void*))
{
  BDS_RB_Tree<ClassObject *> seen;
  ClassObject* clz = (ClassObject*)trans_func(o->clazz);
  while(clz != NULL)
  {
    char * clz_desc = (char*)trans_func(clz->descriptor);
    if(clz_desc == NULL || clz_desc[0] != 'L') return false;
    if(strcmp(clz_desc, descriptor) == 0) return true;
    seen.insert(clz);

    InterfaceEntry* ifArray = (InterfaceEntry*)trans_func(clz->iftable);
    if (ifArray==NULL && clz->iftableCount!=0) return false;
    for (int i = 0; i < clz->iftableCount; i++) {
        ClassObject* ifclz = (ClassObject*)trans_func(ifArray[i].clazz);
        if (ifclz==NULL) return false;
        char * ifclz_desc = (char*)trans_func(ifclz->descriptor);
        if(ifclz_desc == NULL || ifclz_desc[0] != 'L') return false;
        if(strcmp(ifclz_desc, descriptor) == 0) return true;
    }

    clz = (ClassObject*)trans_func(clz->super);
    if(seen.contains(clz) != seen.end()) return false; // loop :(
  }
  return false;
}

//Thread* zmbThreadMain(DvmGlobals* gDvm_ptr)
//{
//  //std::string new_name = dvmGetThreadName(new_t);
//  static const char m[] = "main";
//  Thread *t;
//  zmbForEachOldThread(t, gDvm_ptr)
//  {
//    char* name = zmbGetThreadName(t);
// //    if(strcmp(name, new_name.c_str()) == 0) {
//     if(strcmp(name, m) == 0) {
//      free(name);
//      return t;
//    }
//    free(name);
//  }
//
//  return NULL;
//}
//
//void zmbWalkStackObjs(Thread* t, const u4* start_fp, void *(*trans_func)(const void*),
//                   int (*cb_func)(Object**, Method*, void*), void* arg)
//{
//  const StackSaveArea *saveArea;
//  for(u4 *fp = (u4 *)trans_func((void*)start_fp);
//         fp != NULL;
//         fp = (u4 *)trans_func(saveArea->prevFrame))
//  {
//    Method *method;
//    saveArea = SAVEAREA_FROM_FP(fp);
//    method = (Method *)trans_func((void*)saveArea->method);
//    
//    if (method == NULL || dvmIsNativeMethod(method)) continue;
//
//    for (size_t i = 0; i < method->registersSize; ++i) {
//      Object* o = (Object *)fp[i];
//      if (o != NULL && ((uintptr_t)o & (8-1)) == 0) {
//        if(cb_func((Object**)&fp[i], method, arg) != 0)
//          return;
//      }
//    }
//  }
//}
//
//void zmbWalkStackObjs(Thread* t, void *(*trans_func)(const void*),
//                   int (*cb_func)(Object**, Method*, void*), void* arg)
//{
//  zmbWalkStackObjs(t, t->interpSave.curFrame, trans_func, cb_func, arg);
//}
//
//
//
//int catchActivityThreadObj(Object **o, Method* meth, void *arg)
//{
//  static const char obj_name[] = "Landroid/app/ActivityThread;";
//  Object* obj = (Object*)zmbTranslateAddr(*o);
//  if (obj == NULL) return 0;
//  if(strcmp(zmbGetClazzDescriptor(obj), obj_name) == 0) {
//    Object** a_obj = (Object**)arg;
//    (*a_obj) = *o;
//    return 1;
//  }
//  return 0;
//}
//
//// Get a translated pointer to the ActivityThread object in old_t's stack
//Object* zmbActivityThread(DvmGlobals* gDvm_ptr, Thread* old_t)
//{
//  Object* p = NULL;
//  zmbWalkStackObjs(old_t, zmbTranslateAddr, catchActivityThreadObj, &p);
//  return (Object*)zmbTranslateAddr(p);
//}
//
////////////////////////////////////////////////////////////////////////////////
//Object* getmActivities(Object* old_ActivityThread)
//{
//  static const char f_sig[] = "Ljava/util/HashMap;";
//  static const char f_name[] = "mActivities";
//  return zmbGetInstanceObjectField(old_ActivityThread, f_name, f_sig);
//}
//
//ArrayObject* getmActTable(Object* old_mActivities)
//{
//  static const char f_sig[] = "[Ljava/util/HashMap$HashMapEntry;";
//  static const char f_name[] = "table";
//  return (ArrayObject*)zmbGetInstanceObjectField(old_mActivities, f_name, f_sig);
//}
//
//Object* getHMEValue(Object* old_hme)
//{
//  static const char f_sig[] = "Ljava/lang/Object;";
//  static const char f_name[] = "value";
//  return zmbGetInstanceOldObjectField(old_hme, f_name, f_sig);
//}
//
///**
// * Call cb_func with NOT translated pointers to each ActivityClientRecord Object registered to 
// * the given translated ActivityThread pointer.
// */
//void zmbLoopOldActivityClientRecords(Object* old_ActivityThread, void (*cb_func)(Object*, void*), void* args)
//{
//  Object* mActivities = getmActivities(old_ActivityThread);
//  ArrayObject* tab = getmActTable(mActivities);
//
//  Object** hme_ptr = (Object**)((void*)tab->contents);
//  for(u4 i = 0; i < tab->length; i++, hme_ptr++)
//  {
//    Object* hme = (Object*)zmbTranslateAddr(*hme_ptr);
//    if (hme == NULL) continue;
//    Object* activity_client_record = getHMEValue(hme);
//    cb_func(activity_client_record, args);
//  }
//}
//
//
//Object* getActivity(Object* activity_client_record)
//{
//  static const char f_sig[] = "Landroid/app/Activity;";
//  static const char f_name[] = "activity";
//  return zmbGetInstanceOldObjectField(activity_client_record, f_name, f_sig);
//}
//
//struct loopActivsInfo {
//  void (*cb_func)(Object*, void*);
//  void *args;
//};
//
//void loopActivsCB(Object* a, void* arg)
//{
//  Object* activity_client_record = (Object*)zmbTranslateAddr(a);
//  Object* activity = getActivity(activity_client_record);
//  loopActivsInfo* info = (loopActivsInfo*)arg;
//  info->cb_func(activity, info->args);
//}
//
///**
// * Call cb_func with NOT translated pointers to each Activity Object registered to 
// * the given translated ActivityThread pointer.
// */
//void zmbLoopOldActivities(Object* old_ActivityThread, void (*cb_func)(Object*, void*), void* args)
//{
//  loopActivsInfo info = {cb_func, args};
//  zmbLoopOldActivityClientRecords(old_ActivityThread, loopActivsCB, &info);
//}

/**
  struct HashIter {
    void*       data;
    HashTable*  pHashTable;
    int         idx;
  };
**/

void zmbHashIterNext(HashIter* pIter) {
    int i = pIter->idx +1;
    int lim = pIter->pHashTable->tableSize;
    for ( ; i < lim; i++) {
        HashEntry* he = (HashEntry*)zmbTranslateAddr(pIter->pHashTable->pEntries);
        void* data = he[i].data;
        if (data != NULL && data != HASH_TOMBSTONE)
            break;
    }
    pIter->idx = i;
}
void zmbHashIterBegin(HashTable* pHashTable, HashIter* pIter) {
    pIter->pHashTable = pHashTable;
    pIter->idx = -1;
    zmbHashIterNext(pIter);
}
bool zmbHashIterDone(HashIter* pIter) {
  return (pIter->idx >= pIter->pHashTable->tableSize);
}
void* zmbHashIterData_NoTranslate(HashIter* pIter) {
    HashEntry* he = (HashEntry*)zmbTranslateAddr(pIter->pHashTable->pEntries);
    void* data = he[pIter->idx].data;
    return data;
}
void* zmbHashIterData(HashIter* pIter) {
    return zmbTranslateAddr(zmbHashIterData_NoTranslate(pIter));
}


#define getTranslate(type, dexF, idx) \
  (Dex ## type *)zmbTranslateAddr((void*)dexGet ## type (dexF, idx))

char* zmbDexStringByTypeIdx(const DexFile* pDexFile, u4 idx) {
  DexTypeId* typeId = getTranslate(TypeId , pDexFile, idx);
  DexStringId* pStringId = getTranslate(StringId, pDexFile, typeId->descriptorIdx);
  u1* ptr = (u1*)zmbTranslateAddr((void*)(pDexFile->baseAddr + pStringId->stringDataOff));
  while (*(ptr++) > 0x7f) /* empty */ ;
  return (char*)ptr;
}

char * zmbGetDescFromProto(const DexProto* pProto)
{
  DexFile* dexFile = (DexFile*)zmbTranslateAddr((void*)pProto->dexFile);
  DexProtoId* protoId = getTranslate(ProtoId, dexFile, pProto->protoIdx);

  // return type
  char* retType = zmbDexStringByTypeIdx(dexFile, protoId->returnTypeIdx);

  // args
  const DexTypeList* typeList = (DexTypeList*)zmbTranslateAddr(
	(void*)dexGetProtoParameters(dexFile, protoId));

  // calc len
  size_t length = 3 + strlen(retType); // parens and terminating '\0'
  u4 paramCount = (typeList == NULL) ? 0 : typeList->size;

  for (u4 i = 0; i < paramCount; i++) {
    const DexTypeItem* pItem = dexGetTypeItem(typeList, i);
    u4 idx = pItem->typeIdx;
    length += strlen(zmbDexStringByTypeIdx(dexFile, idx));
  }

  char *ret;
  char *ret_save;
  ret = ret_save = (char*) malloc(length);
  *ret = '(';
  ret++;
  for (u4 i = 0; i < paramCount; i++) {
    const DexTypeItem* pItem = dexGetTypeItem(typeList, i);
    u4 idx = pItem->typeIdx;
    char *desc = zmbDexStringByTypeIdx(dexFile, idx);
    strcpy(ret, desc);
    ret += strlen(desc);
  }
  *ret = ')';
  ret++;
  strcpy(ret, retType);
  return ret_save;
} 

#undef getTranslate

