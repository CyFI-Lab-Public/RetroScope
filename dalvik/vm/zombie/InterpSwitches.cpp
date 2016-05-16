
#include "Zombie.h"
#include "MemMap.h"
#include "StructLib.h"

//extern void zmbAppScreenChange();

//ClassObject * windowManager = NULL;

//extern "C" void zmbScreenChange(Method * m){
//  if (!windowManager) windowManager = dvmFindLoadedClass("Landroid/view/WindowManagerGlobal;");
//  if (m->clazz == windowManager && strcmp(m->name, "removeView")==0){
//    zmbAppScreenChange();
//  }
//}


bool isInBlacklistSkip(const char * clazzName, const char* methodName){
  static const int whiteListSize = 1;

  static const char * whiteListFns[whiteListSize][2] = {
		{"Lcom/tencent/mm/model/c;", "isSDCardAvailable"} ,
		};

  for (int i = 0; i < whiteListSize; i++) {
    if (strcmp(whiteListFns[i][0], clazzName)==0 && strcmp(whiteListFns[i][1], methodName)==0) {
      ZMB_LOG("BLACKLIST for %s:%s", clazzName, methodName);
      return true;
    }
  }
  return false;
}

extern "C" int zmbSkipFunction(){
  Thread * self = dvmThreadSelf();
  if (!self) ZMB_LOG("SELF IS NULL!");
  const Method * m = SAVEAREA_FROM_FP(self->interpSave.curFrame)->method;
//  const Method * m = self->interpSave.method;
  if (!m) ZMB_LOG("Method IS NULL!");
  if (isInBlacklistSkip(m->clazz->descriptor, m->name)) return 1;
  else return 0;
}

extern "C" void * zmbGetBlacklistReturnValue(){
  Thread * self = dvmThreadSelf();
  const Method * m = SAVEAREA_FROM_FP(self->interpSave.curFrame)->method;
  //const Method * m = self->interpSave.method;
  if (isInBlacklistSkip(m->clazz->descriptor, m->name)) return NULL;
  ZMB_LOG("SHOULD NOT BE HERE returning for NON BLACKLISTED!!");
  return NULL;
}

/***** "clazz" is the class who defines/inherits the method */
extern "C" const Method* zmb_INVOKE_VIRTUAL_QUICK_SwitchMethodC(Method * lookedUp, unsigned int offset, ClassObject * clazz, void * pc)
{
  if (zmbIsSafeAddr(clazz) && zmbIsSafeAddr(pc)) return lookedUp;  

  if (!zmbIsSafeAddr(clazz) && !zmbIsSafeAddr(pc)) return lookedUp;

  if (zmbIsSafeAddr(clazz) && !zmbIsSafeAddr(pc))
  {
    ClassObject* new_class = NULL;
    ClassObject* search_in_class = clazz;
    while (new_class==NULL && search_in_class!=NULL) {
      new_class = dvmFindLoadedClass(search_in_class->descriptor);
      search_in_class = search_in_class->super;
    }
    
    Method * new_method = new_class->vtable[offset];
    ZMB_LOG("Finding Old Virtual Method with Signature for %s:%s", clazz->descriptor, new_method->name);
    RS_LOG("Finding Old Virtual Method with Signature for %s:%s", clazz->descriptor, new_method->name);
    return zmbFindMatchingOldMethod(zmb_gDvm, clazz, new_method);
  }
      
  if (!zmbIsSafeAddr(clazz) && zmbIsSafeAddr(pc))
  {
    ClassObject * old_class = zmbFindOldLoadedClass(clazz->descriptor);
    Method * old_method = old_class->vtable[offset];
    ZMB_LOG("Finding New Virtual Method with Signature for %s:%s", clazz->descriptor, old_method->name);
    RS_LOG("Finding New Virtual Method with Signature for %s:%s", clazz->descriptor, old_method->name);
    return zmbFindMatchingNewMethod(old_class, old_method);
  }

  ZMB_LOG("\n\n\n\n\n\nSHOULD NOT BE HERE");
  return NULL;
}



bool isInWhiteList(const ClassObject* clazz, const char* fieldName, const char* signature){
  static const int whiteListSize = 2;

  static const char * whiteListFields[whiteListSize][2] = {
		{"Landroid/view/GLES20RecordingCanvas;", "sPool"} ,
		{"Landroid/view/GLES20RecordingCanvas;", "POOL_LIMIT"} ,
		};

  for (int i = 0; i < whiteListSize; i++) {
    if (strcmp(whiteListFields[i][0], clazz->descriptor)==0 && strcmp(whiteListFields[i][1], fieldName)==0) {
      ZMB_LOG("Using New StaticField for %s:%s", clazz->descriptor, fieldName);
      return true;
    }
  }
  ZMB_LOG("Using Old StaticField for %s:%s", clazz->descriptor, fieldName);
  return false;
}

extern "C" const Field* zmb_Static_SwitchFieldC(const Field* old_field) {
  if (zmbIsSafeAddr(old_field) && isInWhiteList(old_field->clazz, old_field->name, old_field->signature)){
    ClassObject * clazz = dvmFindLoadedClass(old_field->clazz->descriptor);
    return dvmFindFieldHier(clazz, old_field->name, old_field->signature);  
  }
  else return old_field;
}


extern "C" const Field* zmb_Inst_SwitchFieldC(const Field* field, Object * obj) 
{
  if (zmbIsSafeAddr(field) && zmbIsSafeAddr(obj)) return field;  

  if (!zmbIsSafeAddr(field) && !zmbIsSafeAddr(obj)) return field;  

  if (zmbIsSafeAddr(field) && !zmbIsSafeAddr(obj))
  {
    ZMB_LOG("Switching Old InstField to New for %s:%s", field->clazz->descriptor, field->name);
    RS_LOG("Switching Old InstField to New for %s:%s", field->clazz->descriptor, field->name);
    ClassObject * new_class = dvmFindLoadedClass(field->clazz->descriptor);
    return dvmFindFieldHier(new_class, field->name, field->signature);  
  }  
  
  if (!zmbIsSafeAddr(field) && zmbIsSafeAddr(obj))
  {
    ZMB_LOG("Switching New InstField to Old for %s:%s", field->clazz->descriptor, field->name);
    RS_LOG("Switching New InstField to Old for %s:%s", field->clazz->descriptor, field->name);
    ClassObject * old_class = zmbFindOldLoadedClass(field->clazz->descriptor);
    return dvmFindFieldHier(old_class, field->name, field->signature);  
  }
  
  ZMB_LOG("\n\n\n\n\n\nSHOULD NOT BE HERE");
  return NULL;
}


extern "C" int zmb_Inst_Quick_SwitchFieldOffsetC(int offset, Object * obj, void * pc)
{
  if (zmbIsSafeAddr(obj) && zmbIsSafeAddr(pc)) return offset;  
  
  if (!zmbIsSafeAddr(obj) && !zmbIsSafeAddr(pc)) return offset;  

  ClassObject * switched_class = NULL;
  ClassObject * orig_class = obj->clazz;
  
  if (zmbIsSafeAddr(obj) && !zmbIsSafeAddr(pc))
  {
    ZMB_LOG("Switching New InstField offset to Old for %s", obj->clazz->descriptor);
    RS_LOG("Switching New InstField offset to Old for %s", obj->clazz->descriptor);

    ClassObject* search_in_class = orig_class;
    while (switched_class==NULL && search_in_class!=NULL) {
      switched_class = dvmFindLoadedClass(search_in_class->descriptor);
      search_in_class = search_in_class->super;
    }
//    switched_class = dvmFindLoadedClass(orig_class->descriptor);
  }

  if (!zmbIsSafeAddr(obj) && zmbIsSafeAddr(pc))
  {
    ZMB_LOG("Switching Old InstField offset to New for %s", obj->clazz->descriptor);
    RS_LOG("Switching Old InstField offset to New for %s", obj->clazz->descriptor);
    switched_class = zmbFindOldLoadedClass(orig_class->descriptor);
  }
  
  InstField* orig_field = NULL;
  zmbForEachFieldInObjectNoTranslate(orig_field, switched_class)
  {
    if (orig_field->byteOffset==offset) 
    {
      InstField * switched_field = (InstField *)dvmFindFieldHier(orig_class, orig_field->name, orig_field->signature);
      return switched_field->byteOffset;
    }
  }
  
  ZMB_LOG("\n\n\n\n\n\nSHOULD NOT BE HERE");
  return 0;
}



extern "C" bool isInWhiteListInvokeMethods(ClassObject * clazz, const char * m_name, char * m_desc)
{
  static const int whiteListSize = 0;

  static const char * whiteListFields[whiteListSize][3] = {
//		{"Landroid/view/ViewRootImpl;", "checkThread", "(V)V"} ,
		};
  
  for (int i = 0; i < whiteListSize; i++) {
    if (strcmp(whiteListFields[i][0], clazz->descriptor)==0 && strcmp(whiteListFields[i][1], m_name)==0  && strcmp(whiteListFields[i][2], m_desc)==0) {
      ZMB_LOG("Using New Invoke Method for %s:%s:%s", clazz->descriptor, m_name, m_desc);
      RS_LOG("Using New Invoke Method for %s:%s:%s", clazz->descriptor, m_name, m_desc);
      return true;
    }
  }
  return false;
}

extern "C" const Method * zmb_Invoke_SwitchMethodC(Method * orig_method, Object * obj)
{
  if (obj==NULL) return orig_method;
  
  ClassObject * orig_class = orig_method->clazz;
  ClassObject * switched_class = NULL; 
  
  const char * m_name = orig_method->name;
  char * m_desc = dexProtoCopyMethodDescriptor(&(orig_method->prototype));
 
  if (zmbIsSafeAddr(obj) && isInWhiteListInvokeMethods(orig_class, m_name, m_desc))
  {
    switched_class = dvmFindLoadedClass(orig_class->descriptor);
  }
  else if (zmbIsSafeAddr(obj) && !zmbIsSafeAddr(orig_method))
  {
    ZMB_LOG("Switching New Class to Old for %s", orig_class->descriptor);
    RS_LOG("Switching New Class to Old for %s", orig_class->descriptor);
    switched_class = zmbFindOldLoadedClass(orig_class->descriptor);
  }
  else if (!zmbIsSafeAddr(obj) && zmbIsSafeAddr(orig_method))
  {
    ZMB_LOG("Switching Old Class to New for %s", orig_class->descriptor);
    RS_LOG("Switching Old Class to New for %s", orig_class->descriptor);
    switched_class = dvmFindLoadedClass(orig_class->descriptor);
  }
  else if (zmbIsSafeAddr(obj) && zmbIsSafeAddr(orig_method)) 
  {
    return orig_method;  
  }
  else if (!zmbIsSafeAddr(obj) && !zmbIsSafeAddr(orig_method)) 
  {
    return orig_method;  
  }
  

  Method* switched_method = dvmFindDirectMethodHierByDescriptor(switched_class, m_name, m_desc);

  if (switched_method == NULL) {
    switched_method = dvmFindVirtualMethodHierByDescriptor(switched_class, m_name, m_desc);
  } 

  free(m_desc); 
  return switched_method;
}





extern "C" const ClassObject * zmb_Invoke_Interface_SwitchClazzC(ClassObject * thisClass, Method * m)
{
  if (zmbIsSafeAddr(thisClass) && zmbIsSafeAddr(m)) return thisClass;  
  if (!zmbIsSafeAddr(thisClass) && !zmbIsSafeAddr(m)) return thisClass;  
  if (zmbIsSafeAddr(thisClass) && !zmbIsSafeAddr(m)) 
  {
    ZMB_LOG("Switching Old Interface Class to New for %s", thisClass->descriptor);
    RS_LOG("Switching Old Interface Class to New for %s", thisClass->descriptor);
    return dvmFindLoadedClass(thisClass->descriptor);
  }
  if (!zmbIsSafeAddr(thisClass) && zmbIsSafeAddr(m)) 
  {
    ZMB_LOG("Switching New Interface Class to Old for %s", thisClass->descriptor);
    RS_LOG("Switching New Interface Class to Old for %s", thisClass->descriptor);
    return  zmbFindOldLoadedClass(thisClass->descriptor);
  }
  ZMB_LOG("\n\n\n\n\n\nSHOULD NOT BE HERE");
  return NULL;
}

extern "C" bool isInWhiteListNativeMethods(ClassObject * clazz)
{

  static const char * whiteListClasses[] = {"Landroid/os/SystemProperties;", 
                                            "Landroid/content/res/AssetManager;"
                                           };
#define n_classes (sizeof (whiteListClasses) / sizeof (const char *))

  for (unsigned i = 0; i < n_classes; i++) {
    if (strcmp(whiteListClasses[i], clazz->descriptor)==0) {
      ZMB_LOG("Using Old NativeMethod for %s", clazz->descriptor);
      return true;
    }
  }
  ZMB_LOG("Using New NativeMethod for %s", clazz->descriptor);
  return false;
}

extern "C" const Method* zmbFindMatchingNewNativeMethodC(const Method* method){
  zmb_incr_native_calls();
  if (!zmbIsSafeAddr(method) || isInWhiteListNativeMethods(method->clazz)) return method;
  const Method * old_method = method;
  const Method * new_method = zmbFindMatchingNewMethod(old_method->clazz, old_method);
  if (new_method == NULL)
  {
    ZMB_LOG("\n\n\n\nNew Method is Null for %s:%s\n\n\n\n\n", old_method->clazz->descriptor, old_method->name); 
    RS_LOG("\n\n\n\nNew Method is Null for %s:%s\n\n\n\n\n", old_method->clazz->descriptor, old_method->name); 
    return method;
  }
  else 
  {
    ZMB_LOG("Found New Method for %s:%s", old_method->clazz->descriptor, old_method->name); 
    RS_LOG("Found New Method for %s:%s", old_method->clazz->descriptor, old_method->name); 
    return new_method;
  }
}


extern "C" void zmb_InstanceOf_SwitchClassC(ClassObject ** instP, ClassObject ** clazzP)
{
  ClassObject * inst = *instP;
  ClassObject * clazz = *clazzP;
  if (zmbIsSafeAddr(inst) && zmbIsSafeAddr(clazz)) return;  
  if (!zmbIsSafeAddr(inst) && !zmbIsSafeAddr(clazz)) return;  
  if (zmbIsSafeAddr(inst) && !zmbIsSafeAddr(clazz))
  {
    *clazzP = zmbFindOldLoadedClass(clazz->descriptor);
    return;
  } 
  if (!zmbIsSafeAddr(inst) && zmbIsSafeAddr(clazz))
  {
    *instP = zmbFindOldLoadedClass(inst->descriptor);
    return;
  } 
  ZMB_LOG("\n\n\n\n\n\nSHOULD NOT BE HERE");
  return;
}


/* JUST FOR JNI? */
jfieldID zmbCorrectFieldID(JNIEnv * env, jobject jobj, jfieldID fld){
    Thread * self = dvmThreadSelf();
    Object * obj =  dvmDecodeIndirectRef(self, jobj);
    if (!zmbIsSafeAddr(obj)) {
         return fld;
    } else {
        IndirectRefTable* pRefTable = &(self->jniLocalRefTable);
        void* curFrame = self->interpSave.curFrame;
        u4 cookie = SAVEAREA_FROM_FP(curFrame)->xtra.localRefCookie;
        jclass jclazz = (jclass) pRefTable->add(cookie, obj->clazz);
        Field * currField = (Field *)fld;
        ZMB_LOG("Correcting Field Pointer for Old Object %s:%s", obj->clazz->descriptor, currField->name);
        RS_LOG("Correcting Field Pointer for Old Object %s:%s", obj->clazz->descriptor, currField->name);
        return env->GetFieldID(jclazz, currField->name, currField->signature);
    }
}

void zmbPatchOldClass(ClassObject * clazz)
{
  
  zmbAddZombieObject((Object *)clazz);
  clazz->super = zmbFindOldLoadedClass(clazz->super->descriptor);
  
  for (int i = 0; i < clazz->ifieldCount; i++)
  {
    zmbAddZombieObject((Object *)(&(clazz->ifields[i])));
  } 

  const StaticField* pField = &clazz->sfields[0]; 
  for (int i = 0; i < clazz->sfieldCount; i++, pField++)
  {
    zmbAddZombieObject((Object *)pField);
  } 
   
  for (int i = 0; i < clazz->iftableCount; i++)
  {
    zmbAddZombieObject((Object *)(&(clazz->iftable[i])));
  }
  
  for (int i = 0; i < clazz->directMethodCount; i++)
  {
    zmbAddZombieObject((Object *)(&(clazz->directMethods[i])));
  }
   
  for (int i = 0; i < clazz->virtualMethodCount; i++)
  {
    zmbAddZombieObject((Object *)(&(clazz->virtualMethods[i])));
  }
  
  for (int i = 0; i < clazz->interfaceCount; i++)
  {
    clazz->interfaces[i] = zmbFindOldLoadedClass(clazz->interfaces[i]->descriptor);
  }
  
  for (int i = 0; i < clazz->vtableCount; i++)
  {
    Method ** m = &(clazz->vtable[i]);
    if ((*m)->clazz == clazz)
    {
      zmbAddZombieObject((Object *)*m);
    }
    else
    {
      *m = zmbFindMatchingOldMethod(zmb_gDvm, clazz, *m); 
    }
  }
    
}
