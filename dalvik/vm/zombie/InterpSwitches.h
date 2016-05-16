#ifndef DALVIK_ZOMBIE_INTERP_SWITCHES_H_
#define DALVIK_ZOMBIE_INTERP_SWITCHES_H_

extern "C" const Method* zmb_INVOKE_VIRTUAL_QUICK_SwitchMethodC(Method * lookedUp, unsigned int offset, ClassObject * thisPtr, void * pc);

extern "C" const Field* zmb_Static_SwitchFieldC(const Field* old_field);

extern "C" const Field* zmb_Inst_SwitchFieldC(const Field* field, Object * obj);

extern "C" int zmb_Inst_Quick_SwitchFieldOffsetC(int offset, Object * obj, void * pc);

extern "C" const Method * zmb_Invoke_SwitchMethodC(Method * orig_method, Object * obj);

extern "C" const ClassObject * zmb_Invoke_Interface_SwitchClazzC(ClassObject * thisClass, Method * m);

extern "C" const Method* zmbFindMatchingNewNativeMethodC(const Method* old_method);

extern "C" void zmb_InstanceOf_SwitchClassC(ClassObject ** instP, ClassObject ** clazzP);

jfieldID zmbCorrectFieldID(JNIEnv * env, jobject obj, jfieldID fld);

void zmbPatchOldClass(ClassObject * clazz);

#endif // DALVIK_ZOMBIE_INTERP_SWITCHES_H_

