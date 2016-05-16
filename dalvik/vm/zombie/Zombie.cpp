
#include "Zombie.h"
#include "MemMap.h"
#include "NewMemMap.h"
#include "StructLib.h"

#include <sys/types.h>
#include <signal.h>

//#include "interp/Jit.h"

#include "./ds/rb_tree.h"
#include "./ds/list.h"
//#include "./ds/hash_table.h"
#include <utility>

//#include "Patch_Internal.h"
#include <unistd.h>
//#include <gui/Surface.h>
//#include <ui/GraphicBuffer.h>
#include <ctype.h>
#include "JniInternal.h"
#include "ScopedPthreadMutexLock.h"
#include <cstdlib>
#include <dlfcn.h>
#include <setjmp.h>
#include "hijack/hijack.h"
#include "zmb_elf.h"

#include <algorithm>

DvmGlobals* zmb_gDvm = NULL;
void* gHs_ptr = NULL;
struct ExecMetrics * curr_metrics = NULL;
Object * goodAttachInfo = NULL;
Object * newWM;
BDS_List<Object *> allViews;
#define TREE_SIZE_THRESHOLD 5


static const char view_name[] = "Landroid/view/View;";
static const char viewGroup_name[] = "Landroid/view/ViewGroup;";
static const char attachInfo_name[] = "Landroid/view/View$AttachInfo;";
static const char viewRootImpl_name[] = "Landroid/view/ViewRootImpl;";
static const char displayList_name[] = "Landroid/view/DisplayList;";
static const char context_name[] = "Landroid/content/Context;";
static const char textView_name[] = "Landroid/widget/TextView;";
static const char skypeCircleView_name[] = "Lcom/skype/android/widget/CircleImageView;";
static const char seg_heap[] = "dalvik-heap";


//////////////////////////////////////////////////////////////////////////
///////////// MALLOC HOOKS //////////////////////////////////////////////

void* (*originalMalloc) (size_t);
void* zmbMallocHook (size_t size) {
  ZMB_LOG("\n\n\n\n\n THIS WAS CALLED?!?! \n\n\n\n\n\n\n");
  zmb_incr_old_c_ds();
  hijack_pause((void *)originalMalloc);
  void* ret = originalMalloc(size);
  hijack_resume((void *)originalMalloc);
  return ret;
}

void* (*originalDLMalloc) (size_t);
void* zmbDLMallocHook (size_t size) {
  ZMB_LOG("\n\n\n\n\n THIS WAS CALLED?!?! \n\n\n\n\n\n\n");
  zmb_incr_old_c_ds();
  hijack_pause((void *)originalDLMalloc);
  void* ret = originalDLMalloc(size);
  hijack_resume((void *)originalDLMalloc);
  return ret;
}

void hookOldFuncs()
{
  Elf_obj * elf_libc = zmbGetELFObjForSeg("/system/lib/libc.so");
  uintptr_t elf_addr = (uintptr_t) elf_libc->maddr;
  
  Elf32_Sym * sym_malloc = elf_get_dysym_for_name(elf_libc, "malloc");
  originalMalloc = (typeof originalMalloc)(elf_addr + sym_malloc->st_value);
  hijack_start((void *)originalMalloc, (void*)zmbMallocHook);
  
  Elf32_Sym * sym_dlmalloc = elf_get_dysym_for_name(elf_libc, "dlmalloc");
  originalDLMalloc = (typeof originalDLMalloc)(elf_addr + sym_dlmalloc->st_value);
  hijack_start((void *)originalDLMalloc, (void*)zmbDLMallocHook);
}

//////////////////////////////////////////////////////////////////////////
//////////// zmbInit -- get this thing running ///////////////////////////

void (*zmbReleaseLocks) ();
int output_bitmap_width, output_bitmap_height;

void zmbInit(int bitmap_width, int bitmap_height)
{
  output_bitmap_width = bitmap_width;
  output_bitmap_height = bitmap_height;
  ZMB_LOG("Started. %s %s", map_file_name, mem_file_name);
  zmbMemMapInit(map_file_name, mem_file_name);
  zmbNewMemMapInit();
  zmbFillFromMemFile();  
 
  zmbReleaseLocks =  (void (*)()) dlsym(RTLD_DEFAULT, "zmbReleaseLocks");
  void (*zmbSetupMallocHook)(bool (*)(const void *), void (*)()) =  (void (*)(bool (*)(const void *), void (*)())) dlsym(RTLD_DEFAULT, "zmbSetupMallocHook");
  zmbSetupMallocHook(zmbIsSafeAddr, zmb_incr_new_c_ds);

  zmbFindOldGlobals(&zmb_gDvm, &gHs_ptr);
  ZMB_LOG("Found old gDvm @ %p" , zmb_gDvm);
  RS_LOG("Found old gDvm @ %p" , zmb_gDvm);
  ZMB_LOG("Found old gHs @ %p" , gHs_ptr);
}

bool zmbIsRunning() { return zmb_gDvm!=NULL; }

//////////////////////////////////////////////////////////////////////////
////////////////////// few Object helpers ////////////////////////////////

Object * getViewParent(Object* view) {
  return zmbGetInstanceOldObjectField(view, "mParent", NULL);
}

void setViewParent(Object* view, Object * parent) {
  dvmSetFieldObject(view, dvmFindInstanceFieldHier(view->clazz, "mParent", "Landroid/view/ViewParent;")->byteOffset, parent);
}

Object ** getChildrenFromViewGroup(Object* vg, unsigned * children_length) {
  ArrayObject * mChildren = (ArrayObject *)zmbGetInstanceOldObjectField(vg, "mChildren", NULL);
  if (mChildren==NULL) {
    *children_length = 0;
    return NULL;
  } else {
    *children_length = (unsigned) mChildren->length;
    return (Object**)(void*)(mChildren->contents);
  }
}

bool isChildOf(Object* view, Object* parent) {
  if(zmbIsTypeOrHeir(parent, viewGroup_name, zmbTranslateAddr)) {
    unsigned children_length;
    Object ** children = getChildrenFromViewGroup(parent, &children_length);
    unsigned n_children = (unsigned) dvmGetFieldInt(parent, dvmFindInstanceFieldHier(parent->clazz, "mChildrenCount", "I")->byteOffset);
    if(n_children != 0 && children == NULL) return false;
    if(n_children != 0 && n_children > children_length) return false;
    for(unsigned i = 0; i < n_children; i++) {
      if(children[i] == view) return true;
    }
    return false;
  } else if(zmbIsTypeOrHeir(parent, viewRootImpl_name, zmbTranslateAddr)) {
    return zmbGetInstanceOldObjectField(parent,"mView", NULL) == view;
  }
  return false;
}


//////////////////////////////////////////////////////////////////////////
//// For tracking if any trees get broken during re-ex  //////////////////

struct ObjPair {
  Object * keyView;
  Object * fieldView;
  ObjPair(Object * k, Object * v): keyView(k), fieldView(v) {}
};

class ObjCompare {
public:
  bool operator()(const ObjPair& a, const ObjPair& b)
  { return (unsigned)(a.keyView) < (unsigned)(b.keyView); }
};

BDS_RB_Tree<ObjPair, ObjCompare> originalViews;

static inline void addViewToOriginal(Object * rootView, Object * newRoot) {
  Object * originalView = (originalViews.contains(ObjPair(rootView, (Object*)NULL)))->fieldView;
  originalViews.insert(ObjPair(newRoot, originalView));
}


//////////////////////////////////////////////////////////////////////////
/////////////// The current list of Views pending re-ex //////////////////

extern BDS_List<Object *> dumpedViews;
BDS_List<Object *> viewsToDraw;
int n_viewsToDraw=0;

static inline void addViewToDraw(Object* view) {
// Commented out dumpedViews check, to break and insert wechatTTFC into dumpedViews and originalViews
//  for (BDS_List<Object *>::iterator it = dumpedViews.begin(); it != dumpedViews.end(); ++it) {
//    if (*it==view) return;
//  }
  viewsToDraw.push_back(view);
  n_viewsToDraw++;
}

static inline void killViewToDraw(Object* view) {
  BDS_List<Object *>::iterator it2 = viewsToDraw.begin();
  while(*it2 != view) { it2++; }
  viewsToDraw.erase(it2);
  n_viewsToDraw--;
}

//////////////////////////////////////////////////////////////////////////
Object * viewToDump; // set every time getTopViewOnStack is called

extern "C" Object * getTopViewOnStack()
{
  const StackSaveArea *saveArea;
  
  for(u4 *fp = dvmThreadSelf()->interpSave.curFrame;
         fp != NULL;
         fp = saveArea->prevFrame)
  {
    saveArea = SAVEAREA_FROM_FP(fp);
    const Method* method = saveArea->method;
    if(method==NULL) continue;

    Object * thisArg = (Object *) fp[(u4)method->registersSize - (u4)method->insSize];
    if (thisArg && zmbIsSafeAddr(thisArg) && zmbIsTypeOrHeir(thisArg, "Landroid/view/View;", zmbTranslateAddr) && 
        (strcmp(method->name, "draw")==0 || strcmp(method->name, "getDisplayList")==0))
    {
      viewToDump = thisArg;  
      return viewToDump;
    }
  }
  return viewToDump;
}
//////////////////////////////////////////////////////////////////////////

//////////////////////////////////////////////////////////////////////////
/// These Views caused problems during Re-Ex... kill them from the trees /
BDS_List<Object *> dumpedViews;

static inline void dumpViewWhileDrawing(Object * rootView, Object * viewToDump) {
  unsigned children_length;
  dumpedViews.push_back(viewToDump);
  ZMB_LOG("For RootView %p", rootView);
  if (zmbIsTypeOrHeir(viewToDump, "Landroid/view/ViewGroup;", zmbTranslateAddr)) {
    Object ** children = getChildrenFromViewGroup(viewToDump, &children_length);
    int mChildrenCountOffset = dvmFindInstanceFieldHier(viewToDump->clazz, "mChildrenCount", "I")->byteOffset;
    int n_children = dvmGetFieldInt(viewToDump, mChildrenCountOffset);
    for(int i = 0; i < n_children; i++) {
      ZMB_LOG("Adding Dumped View's child %p to draw", children[i]);
      addViewToOriginal(rootView, children[i]);
      addViewToDraw(children[i]);
    }
  }
  if (rootView != viewToDump) addViewToDraw(rootView);
}


static inline void dumpViewWhileDrawingSignal(Object * rootView) {
  Object * viewToDump = getTopViewOnStack();
  if (viewToDump && rootView) dumpViewWhileDrawing(rootView, viewToDump);
}

static inline void dumpViewWhileDrawingExcept(Object * rootView) {
  if (viewToDump && rootView) dumpViewWhileDrawing(rootView, viewToDump);
}

static inline void correctDumpedViews() 
{
  for (BDS_List<Object *>::iterator it = dumpedViews.begin(); it != dumpedViews.end(); ++it)
  {
    Object * viewToDump = *it;
    Object * parent = getViewParent(viewToDump);
    unsigned children_length;
    if (parent && zmbIsTypeOrHeir(parent, "Landroid/view/ViewGroup;", zmbTranslateAddr)) {
      Object ** children = getChildrenFromViewGroup(parent, &children_length);
      int mChildrenCountOffset = dvmFindInstanceFieldHier(parent->clazz, "mChildrenCount", "I")->byteOffset;
      int n_children = dvmGetFieldInt(parent, mChildrenCountOffset);
      for(int i = 0; i < n_children; i++) {
        if (children[i] == viewToDump) {
          ZMB_LOG("Dumping View %p", children[i]);
          for (int idx=i; idx<n_children-1; idx++) 
            children[idx] = children[idx+1];
          n_children--;
          i--;
          dvmSetFieldInt(parent, mChildrenCountOffset, n_children);
        }
      }
    }
  }
}

//////////////////////////////////////////////////////////////////////////
///////////////////// Do the Re-Ex ///////////////////////////////////////

jmp_buf jbuf;

void sigHandler(int sig)
{
  ZMB_LOG("\n\n\n\nGot a Signal %d: %s\n\n\n\n\n", sig, strsignal(sig));
  longjmp(jbuf, 2);
}

Object * getDLFromView(Object * view)
{
  if (curr_metrics) free(curr_metrics);
  curr_metrics = NULL; 

  InterpSaveState interpSaveState = dvmThreadSelf()->interpSave;

  sighandler_t old_sigsegv;
  sighandler_t old_sigill;
  old_sigsegv = signal(SIGSEGV, sigHandler);
  old_sigill = signal(SIGILL, sigHandler);

  if (setjmp(jbuf) != 0) 
  {
    zmbReleaseLocks();
    dumpViewWhileDrawingSignal(view);
    dvmThreadSelf()->interpSave = interpSaveState;
    signal(SIGSEGV, old_sigsegv);
    signal(SIGILL, old_sigill);
    return NULL;
  }
  
  Method * getDisplayListMthd = dvmFindVirtualMethodHierByDescriptor(view->clazz, "getDisplayList", "()Landroid/view/DisplayList;");
  ZMB_LOG("getDisplayList Method address is %p", getDisplayListMthd);

  Thread * selfThread = dvmThreadSelf();

  JValue result;
  result.l = NULL;
  
  curr_metrics = new ExecMetrics();
  dvmCallMethod(selfThread, getDisplayListMthd, view, &result);
  
  if(dvmCheckException(selfThread))
  {
    ZMB_LOG("getDisplayList has exception: Object * %p:", dvmGetException(selfThread));
    dvmLogExceptionStackTrace();
    dvmClearException(selfThread);
    dumpViewWhileDrawingExcept(view);
    signal(SIGSEGV, old_sigsegv);
    signal(SIGILL, old_sigill);
    return NULL;
  }
 
  Object * dlJava = (Object *)result.l; 
  if (dlJava) {
    Object * mFinalizer  = zmbGetInstanceOldObjectField(dlJava, "mFinalizer", NULL); 
    void * cDL  = (void *) zmbGetInstanceOldObjectField(mFinalizer, "mNativeDisplayList", NULL); 
    ZMB_LOG("Called getDisplayList(), result is Java:%p, C:%p", dlJava, cDL);
  }
  signal(SIGSEGV, old_sigsegv);
  signal(SIGILL, old_sigill);
  return (Object *)result.l;
}

//////////////////////////////////////////////////////////////////////////
////////// Scan the memory image looking for View Objects ////////////////

//Do Not Call This, Check viewsToDraw for possible views
static inline bool __isConsideredRoot(Object* view) {
  Object* parent = getViewParent(view);
  if(parent == NULL) return true;

  if(zmbIsTypeOrHeir(parent, viewRootImpl_name, zmbTranslateAddr)) {
    if(zmbGetInstanceOldObjectField(parent,"mView", NULL) != view)
      ZMB_LOG("This view's ViewRootImpl Parent points to a different view as child");
    return true;
  }

  // WE CHECK VIEWROOTIMPL BEFORE THIS BECAUSE
  // THE CHILD OF A VRI WILL RETURN TRUE!!!!!!!
  if(!isChildOf(view, parent)) return true;

  return false;
}

static inline bool checkClazz(ClassObject * c) {
    if(c == NULL) return false;
    if (!zmbIsSafeAddr(c, sizeof(ClassObject))) return false;
    if(c->descriptorAlloc != NULL && zmbTranslateAddr(c->descriptorAlloc)==NULL) return false;
    if(c->clazz!=NULL && zmbTranslateAddr(c->clazz)==NULL) return false;
    MemSeg * dexSeg = zmbMemSegForAddr(c->pDvmDex);
    if (dexSeg==NULL || (void *)(c->pDvmDex)!=(void *)(dexSeg->start)) return false;
    if(c->classLoader!=NULL && zmbTranslateAddr(c->classLoader)==NULL) return false;
    return true;
}

int findViews(MemSeg* m, void* __unused_because_I_am_lazy)
{
  if(strstr(m->name, seg_heap) == NULL) return 0;
  
  for(void * start = (void*)m->start,
           * end = (void*)m->end;
      start != end;
      start = (void*)(((uintptr_t)start) + sizeof(void*)))
  {
    Object* ptr = (Object*) start;

    if(!checkClazz((ClassObject *) zmbTranslateAddr(ptr->clazz))) continue;
    if(!zmbIsTypeOrHeir(ptr, view_name, zmbTranslateAddr)) continue;

    ZMB_LOG("1View:%p (%s)", ptr, ptr->clazz->descriptor);

    Object * parent = getViewParent(ptr);
    if(parent!=NULL && !(zmbIsSafeAddr(parent) && checkClazz((ClassObject *) zmbTranslateAddr(parent->clazz)))) continue;
    ZMB_LOG("1.5View:%p (%s)", ptr, ptr->clazz->descriptor);
    ZMB_LOG("1.5Parent:%p", parent);

    bool not_a_view = false;
    for(ClassObject* clz = ptr->clazz; clz != NULL; clz = (ClassObject*)(clz->super))
    {
      if(!checkClazz((ClassObject *) zmbTranslateAddr(clz)))
      {
        not_a_view = true;
        goto kill_it;
      }

      for(unsigned i = 0; i < (unsigned)(clz->ifieldCount); i++) 
      {
        InstField * fld = &(clz->ifields[i]);
        if(fld->signature[0] != 'L') continue;
        Object * temp = dvmGetFieldObject(ptr, fld->byteOffset);
        if(temp!=NULL && !(zmbIsSafeAddr(temp) && zmbIsSafeAddr((void *)((unsigned)temp + sizeof(Object))) && checkClazz((ClassObject *) zmbTranslateAddr(temp->clazz))))
        {
          not_a_view = true;
          goto kill_it;
        }
      }
    }
kill_it:
    if(not_a_view) continue;

    //////////////////// NO SIDE EFFECTS ABOVE THIS LINE!!!!! ///////////////////////////////

    ZMB_LOG("View:%p (%s)", ptr, ptr->clazz->descriptor);
    ZMB_LOG("Parent:%p", parent);

    allViews.push_back(ptr);

    if (zmbIsTypeOrHeir(ptr, viewGroup_name, zmbTranslateAddr))
    {
      unsigned n_children = dvmGetFieldInt(ptr, dvmFindInstanceFieldHier(ptr->clazz, "mChildrenCount", "I")->byteOffset);
      ZMB_LOG("Children:[%p]%d",
        zmbGetInstanceOldObjectField(ptr, "mChildren", NULL), n_children);
    }
  
    const char * wechatTTFC_name = "Lcom/tencent/mm/ui/tools/TestTimeForChatting;";
    if (zmbIsTypeOrHeir(ptr, wechatTTFC_name, zmbTranslateAddr))
    {
      parent = NULL;
      setViewParent(ptr, parent);
      dumpedViews.push_back(ptr);
    }

    if(__isConsideredRoot(ptr))
    {
      originalViews.insert(ObjPair(ptr, ptr));
      addViewToDraw(ptr);
      
      if(parent!=NULL && zmbIsTypeOrHeir(parent, viewRootImpl_name, zmbTranslateAddr))
      {
        unsigned mSeq = dvmGetFieldInt(parent, dvmFindInstanceFieldHier(parent->clazz, "mSeq", "I")->byteOffset);
        ZMB_LOG("ViewRoot Parent Seq Num: %d", mSeq);
        Object * temp = zmbGetInstanceOldObjectField(ptr, "mAttachInfo", NULL);
        if(temp && zmbIsTypeOrHeir(temp, attachInfo_name, zmbTranslateAddr))
          goodAttachInfo = temp;
      }
    }
    ZMB_LOG("\n\n");
  }
  return 0;
}

//////////////////////////////////////////////////////////////////////////
///////////////////// Walk a view tree and call callbacks ////////////////

static inline void
walkTree(Object * view, void (*node_cb_func)(Object*, void*),
         void (*up_cb_func)(void*), void (*down_cb_func)(void*), void* arg )
{
  node_cb_func(view, arg);
  
  if(down_cb_func != NULL) down_cb_func(arg);

  Object ** children = NULL;
  int n_children = 0;
  if (zmbIsTypeOrHeir(view, viewGroup_name, zmbTranslateAddr))
  {
    unsigned children_length;
    children = getChildrenFromViewGroup(view, &children_length);
    int mChildrenCountOffset = dvmFindInstanceFieldHier(view->clazz, "mChildrenCount", "I")->byteOffset;
    n_children = dvmGetFieldInt(view, mChildrenCountOffset);
    for(int i = 0; i < n_children; i++)
    {
      walkTree(children[i], node_cb_func, up_cb_func, down_cb_func, arg);
    }
  }
  
  if(up_cb_func != NULL) up_cb_func(arg);
}

//////////////////////////////////////////////////////////////////////////
///////////////////// Patch the fields in a view before re-ex ////////////

struct Rect {
  Object * view;
  int left, right, top, bottom; 
  Rect(Object* vw, int l, int r, int t, int b): view(vw), left(l), right(r), top(t), bottom(b) { }
  bool equals(Rect a){return left==a.left && right==a.right && top==a.top && bottom==a.bottom;}
};

void patchView(Object* view, void* _unused)
{
  ZMB_LOG("Patching View %p", view);
  InstField * javaDLField = dvmFindInstanceFieldHier(view->clazz, "mDisplayList", displayList_name);
  dvmSetFieldObject(view, javaDLField->byteOffset, (Object *)NULL);

  InstField * contextField = dvmFindInstanceFieldHier(view->clazz, "mContext", context_name);
  Object * activity = dvmGetFieldObject(view, contextField->byteOffset);
  if (activity) 
  {
    InstField * wmField = dvmFindInstanceFieldHier(activity->clazz, "mWindowManager", "Landroid/view/WindowManager;");
    if (wmField)
    {
      dvmSetFieldObject(activity, wmField->byteOffset, newWM);
        ZMB_LOG("Set Activity %p 's WM to %p", activity, newWM);
    }
  }

  if(zmbIsTypeOrHeir(view, textView_name, zmbTranslateAddr))
  {
    Object * mEditor  = zmbGetInstanceOldObjectField(view, "mEditor", NULL);
    if (mEditor!=NULL)
    {
      InstField * dlsField = dvmFindInstanceFieldHier(mEditor->clazz, "mTextDisplayLists", "[Landroid/view/DisplayList;");
      dvmSetFieldObject(mEditor, dlsField->byteOffset, (Object *)NULL);
      ZMB_LOG("Java TextView:%p, Editor:%p", view, mEditor);
    }
  }
  
  if(zmbIsTypeOrHeir(view, skypeCircleView_name, zmbTranslateAddr))
  {
    InstField * canvasField = dvmFindInstanceFieldHier(view->clazz, "i", "Landroid/graphics/Canvas;");
    dvmSetFieldObject(view, canvasField->byteOffset, (Object *)NULL);
    ZMB_LOG("Java Skype CircleImageView:%p", view);
  }
  
  InstField * recreateDLField = dvmFindInstanceFieldHier(view->clazz, "mRecreateDisplayList", "Z");
  dvmSetFieldBoolean(view, recreateDLField->byteOffset, true);

  Object * parentView = getViewParent(view);
  if (parentView && zmbIsTypeOrHeir(parentView, viewRootImpl_name, zmbTranslateAddr))
  {
    Object * viewRootImpl = parentView;
    Thread * selfThread = dvmThreadSelf();
    InstField * mThreadField = dvmFindInstanceFieldHier(viewRootImpl->clazz, "mThread", "Ljava/lang/Thread;");
    dvmSetFieldObject(viewRootImpl, mThreadField->byteOffset, selfThread->threadObj);

    InstField * mViewField = dvmFindInstanceFieldHier(viewRootImpl->clazz, "mView", "Landroid/view/View;");
    dvmSetFieldObject(viewRootImpl, mViewField->byteOffset, view);
  }
  else 
  {
    InstField * AIField = dvmFindInstanceFieldHier(view->clazz, "mAttachInfo", "Landroid/view/View$AttachInfo;");
    dvmSetFieldObject(view, AIField->byteOffset, goodAttachInfo);
  }
  
  int mLeftOffset = dvmFindInstanceFieldHier(view->clazz, "mLeft", "I")->byteOffset;
  int mRightOffset = dvmFindInstanceFieldHier(view->clazz, "mRight", "I")->byteOffset;
  int mBottomOffset = dvmFindInstanceFieldHier(view->clazz, "mBottom", "I")->byteOffset;
  int mTopOffset = dvmFindInstanceFieldHier(view->clazz, "mTop", "I")->byteOffset;
  int mLeft = dvmGetFieldInt(view, mLeftOffset);
  int mRight = dvmGetFieldInt(view, mRightOffset);
  int mTop = dvmGetFieldInt(view, mTopOffset);
  int mBottom = dvmGetFieldInt(view, mBottomOffset);
  int width = mRight - mLeft;
  //int height = mBottom - mTop;

  if (mRight > output_bitmap_width) {
    ZMB_LOG("Right Besides Drawing Area");
    if (find(viewsToDraw.begin(), viewsToDraw.end(), view)!=viewsToDraw.end()){
      if (width > output_bitmap_width) ZMB_LOG("View Too Wide For Bitmap");
      dvmSetFieldInt(view, mLeftOffset, 0); 
      dvmSetFieldInt(view, mRightOffset, width); 
      ZMB_LOG("Corrected THIS ONE");
    }
  }
  if (mLeft < 0) { 
    ZMB_LOG("Left Besides Drawing Area");
    if (find(viewsToDraw.begin(), viewsToDraw.end(), view)!=viewsToDraw.end()){
      if (width > output_bitmap_width) ZMB_LOG("View Too Wide For Bitmap");
      dvmSetFieldInt(view, mLeftOffset, 0); 
      dvmSetFieldInt(view, mRightOffset, width); 
      ZMB_LOG("Corrected THIS ONE");
    }
  }
  if (mBottom > output_bitmap_height) ZMB_LOG("Bottom Besides Drawing Area");
  if (mTop < 0) ZMB_LOG("Top Besides Drawing Area");

/*
  if (width==0){
    int newLeft = (parentView && !zmbIsTypeOrHeir(parentView, viewRootImpl_name, zmbTranslateAddr)) ? dvmGetFieldInt(parentView, mLeftOffset) : 0;
    int newRight = (parentView && !zmbIsTypeOrHeir(parentView, viewRootImpl_name, zmbTranslateAddr)) ? dvmGetFieldInt(parentView, mRightOffset) : output_bitmap_width;
    dvmSetFieldInt(view, mLeftOffset, newLeft);
    dvmSetFieldInt(view, mRightOffset, newRight);
  }  
  if (height==0){
    int newTop = (parentView && !zmbIsTypeOrHeir(parentView, viewRootImpl_name, zmbTranslateAddr)) ? dvmGetFieldInt(parentView, mTopOffset) : 0;
    int newBottom = (parentView && !zmbIsTypeOrHeir(parentView, viewRootImpl_name, zmbTranslateAddr)) ? dvmGetFieldInt(parentView, mBottomOffset) : output_bitmap_height;
    dvmSetFieldInt(view, mTopOffset, newTop);
    dvmSetFieldInt(view, mBottomOffset, newBottom);
  }  
*/


  Object ** children = NULL;
  int n_children = 0;
  if (zmbIsTypeOrHeir(view, viewGroup_name, zmbTranslateAddr))
  {
    unsigned children_length;
    children = getChildrenFromViewGroup(view, &children_length);
    int mChildrenCountOffset = dvmFindInstanceFieldHier(view->clazz, "mChildrenCount", "I")->byteOffset;
    n_children = dvmGetFieldInt(view, mChildrenCountOffset);
    BDS_List<Rect> childDimensions;
    for(int i = 0; i < n_children; i++)
    {
      Object * child = children[i];
      if (find(allViews.begin(), allViews.end(), child)==allViews.end() || find(dumpedViews.begin(), dumpedViews.end(), child)!=dumpedViews.end())
      {
        ZMB_LOG("%p is being removed from the parent", child);
        for (int idx=i; idx<n_children-1; idx++)
          children[idx] = children[idx+1];
        i--;
        n_children--;
        dvmSetFieldInt(view, mChildrenCountOffset, n_children);
      }
    }
  }
}

static inline void patchTree(Object *root) { walkTree(root, patchView, NULL, NULL, NULL); }

//////////////////////////////////////////////////////////////////////////
///////////////////// Print a tree of views //////////////////////////////

struct printTreeArg {
  int level;
  bool legit_child;
  BDS_List<Object *> * printedViews;
  unsigned int * tree_sz;
};

static inline void printTreeUp(void * _arg)
{
  printTreeArg * arg = (printTreeArg *)_arg;
  arg->level--;
}
static inline void printTreeDown(void * _arg)
{
  printTreeArg * arg = (printTreeArg *)_arg;
  arg->level++;
  arg->legit_child = true;
}

static inline void printTreeView(Object * view, void * _arg);

static inline void
printTree(Object * root, void * arg)
{
  walkTree(root, printTreeView, printTreeUp, printTreeDown, arg); 
}

static inline void
printTreeView(Object * view, void * _arg)
{
  printTreeArg * arg = (printTreeArg *)_arg;
  int level = arg->level;
  bool legit_child = arg->legit_child;

  int n_spaces = level*2;
  char spaces[n_spaces+1];

  for(int i = 0; i < n_spaces; i++)
    spaces[i] = ' ';
  if(level != 0) {
    if(legit_child) {
      spaces[n_spaces-1] = '>';
      spaces[n_spaces-2] = '-';
    }
    else {
      spaces[n_spaces-1] = '-';
      spaces[n_spaces-2] = '<';
    }
  }
  spaces[n_spaces] = 0;

  int mLeftOffset = dvmFindInstanceFieldHier(view->clazz, "mLeft", "I")->byteOffset;
  int mRightOffset = dvmFindInstanceFieldHier(view->clazz, "mRight", "I")->byteOffset;
  int mBottomOffset = dvmFindInstanceFieldHier(view->clazz, "mBottom", "I")->byteOffset;
  int mTopOffset = dvmFindInstanceFieldHier(view->clazz, "mTop", "I")->byteOffset;
  int mLeft = dvmGetFieldInt(view, mLeftOffset);
  int mRight = dvmGetFieldInt(view, mRightOffset);
  int mTop = dvmGetFieldInt(view, mTopOffset);
  int mBottom = dvmGetFieldInt(view, mBottomOffset);

  unsigned mID = dvmGetFieldInt(view, dvmFindInstanceFieldHier(view->clazz, "mID", "I")->byteOffset);
  ZMB_LOG("%s[%p, %s], mId:%d, Dimensions:(%d,%d,%d,%d)", spaces, view, view->clazz->descriptor, mID, 
                                                          mLeft, mRight, mTop, mBottom);
  if(arg->printedViews != NULL)
    arg->printedViews->push_back(view);
  if(arg->tree_sz != NULL)
    *(arg->tree_sz) = *(arg->tree_sz) + 1;


  if (zmbIsTypeOrHeir(view, viewGroup_name, zmbTranslateAddr))
  {
    Object ** children = NULL;
    int n_children = 0;
    unsigned children_length;
    children = getChildrenFromViewGroup(view, &children_length);
    int mChildrenCountOffset = dvmFindInstanceFieldHier(view->clazz, "mChildrenCount", "I")->byteOffset;
    n_children = dvmGetFieldInt(view, mChildrenCountOffset);
  //  for(int i = 0; i < n_children; i++)
  //  {
  //    printTree(children[i], level+1, true, printedViews, tree_sz);
  //  }

    for (BDS_List<Object *>::iterator it = allViews.begin();
         it != allViews.end(); ++it)
    {
      Object * parent = getViewParent(*it);
      if(parent == view) {
        bool not_in_children = true;
        for(int i = 0; i < n_children; i++)
        {
          if(*it == children[i])
          {
            not_in_children = false;
            break;
          }
        }
        if(not_in_children) {
         printTreeArg new_args = *arg;
         new_args.level++;
         new_args.legit_child = false;
         printTree(*it, &new_args);
        }
      }
    }
  }
}


/////////////////////////////////////////////////////////////////////////////////////
/////////// App enters here at zmbGenerateDL to get the next new DL to output   /////
/////////////////////////////////////////////////////////////////////////////////////

void zmbFindViewRoot(int *more_dls, jobject wm) {

  Thread * self = dvmThreadSelf();
  newWM =  dvmDecodeIndirectRef(self, wm);
  ZMB_LOG("newWM is %p", newWM);

  ZMB_LOG("\n\n\nFinding Views:\n");
  zmbForEachMemSeg(findViews, NULL);

  if(goodAttachInfo == NULL)
    ZMB_LOG("\nBIG PROBLEM!!!!!! No Good AttachInfo Found!!!!!!!\n\n");
/*
  BDS_List<Object *> printedViews;
  for (BDS_List<Object *>::iterator it = allViews.begin();
       it != allViews.end(); ++it)
  {
    unsigned tree_sz = 0;
    printTreeArg args;
    args.level = 0;
    args.legit_child = true;
    args.printedViews = &printedViews;
    args.tree_sz = &tree_sz;
    printTree(*it, &args);
  } 
    
  for (BDS_List<Object *>::iterator it = allViews.begin(); it != allViews.end(); ++it) {
    bool wasPrinted = false;
    for (BDS_List<Object *>::iterator it2 = printedViews.begin(); it2 != printedViews.end(); ++it2) {
      if(*it2 == *it) {
        wasPrinted = true;
        break;
      }
    }
    if(!wasPrinted)
    {
      ZMB_LOG("MISSED VIEW: %p (%s)", *it, (*it)->clazz->descriptor);
      Object* parent = getViewParent(*it);
      if(parent != NULL)
        ZMB_LOG("  Parent:%p (%s)", parent, parent->clazz->descriptor);
      ZMB_LOG("\n\n"); 
    }
  }
*/
  *more_dls = (n_viewsToDraw > 0 ? 1 : 0);
  return;
}

/////////////////////////////////////////////////////////////////////////////////////
/////////// tracks the buffers which belong to any broken trees /////////////////////

struct ObjToBufPair {
  Object * keyView;
  BDS_List<int> * buffer;
  ObjToBufPair(Object * k): keyView(k) { buffer = new BDS_List<int>();}
};

class ObjToBufCompare {
public:
  bool operator()(const ObjToBufPair& a, const ObjToBufPair& b)
  { return (unsigned)(a.keyView) < (unsigned)(b.keyView); }
};

BDS_RB_Tree<ObjToBufPair, ObjToBufCompare> objToBuf;

/////////////////////////////////////////////////////////////////////////////////////
/////////// App enters here at zmbGenerateDL to get the next new DL to output   /////
/////////////////////////////////////////////////////////////////////////////////////

unsigned int buffer_idx = 0;
unsigned int gdb_idx = 0;

void zmbGenerateDL(jobject* dl)
{
  *dl = NULL;
  while(*dl == NULL && !viewsToDraw.empty())
  {
    RS_LOG("");RS_LOG("");RS_LOG("");
    RS_LOG("Processing Screen %d:", buffer_idx);
    RS_LOG("");RS_LOG("");RS_LOG("");
    // redo the init stuff
    BDS_List<Object *>::iterator it = viewsToDraw.begin();

    zmbFillFromMemFile();
    hookOldFuncs();  
    patchTree(*it);
//    correctDumpedViews();

    unsigned tree_sz = 0;
    printTreeArg args;
    args.level = 0;
    args.legit_child = true;
    args.printedViews = NULL;
    args.tree_sz = &tree_sz;
    printTree(*it, &args);
 
    ZMB_LOG("GDB IDX: %d, BUF IDX: %d", ++gdb_idx, ++buffer_idx);
    Object * new_dl = NULL;
    BDS_RB_Tree<ObjPair, ObjCompare>::iterator obj_it = originalViews.contains(ObjPair(*it, (Object*)NULL));
    if(tree_sz < TREE_SIZE_THRESHOLD && (obj_it != originalViews.end()) && (*it == obj_it->fieldView)) {
      ZMB_LOG("WAS TOO SMALL!");
    } else {
      new_dl = getDLFromView(*it);
    }

    if(new_dl == NULL)
    {
      buffer_idx--;
      *dl = NULL;
    }
    else
    {
      Object * originalView = originalViews.contains(ObjPair(*it, NULL))->fieldView;
      std::pair<BDS_RB_Tree<ObjToBufPair, ObjToBufCompare>::iterator, bool> containsPair = 
                                               objToBuf.insert(ObjToBufPair(originalView));
      containsPair.first->buffer->push_back(buffer_idx);
      
   
      ScopedPthreadMutexLock lock(&gDvm.jniGlobalRefLock);
      *dl = (jobject) gDvm.jniGlobalRefTable.add(IRT_FIRST_SEGMENT, new_dl);
      ZMB_LOG("Metric: %d insns, %d native calls, and (java:%d, c:%d, c_old:%d) ds", curr_metrics->byte_insns, curr_metrics->native_calls,
              curr_metrics->java_ds, curr_metrics->new_c_ds, curr_metrics->old_c_ds);
    }
  
    viewsToDraw.erase(it);
  }


  // Log the break ups
  if(viewsToDraw.empty())
  {
    for (BDS_RB_Tree<ObjToBufPair, ObjToBufCompare>::iterator it = objToBuf.begin();
          it!=objToBuf.end(); ++it)
    {
      ZMB_LOG("For Original View %p", it->keyView);
      for (BDS_List<int>::iterator it1 = it->buffer->begin(); it1!=it->buffer->end(); ++it1){
        ZMB_LOG("        Buffer %d", *it1);
      }
      ZMB_LOG("\n\n\n\n");
    }
  }
}


/////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////

/*
void zmbStackRollback()
{
  const StackSaveArea *saveArea;
  Thread * t = dvmThreadSelf();
  for(u4 *fp = t->interpSave.curFrame;
         fp != NULL;
         fp = saveArea->prevFrame)
  {
    saveArea = SAVEAREA_FROM_FP(fp);
    const Method* method = saveArea->method;
    if (method && strcmp(method->name, "getDL")==0) 
    {
      ZMB_LOG("Correcting Frame To %p with method %p", fp, method);
      t->interpSave.curFrame = fp;
      return;
    }
  }
  ZMB_LOG("Didn't Find The Frame? WHY ANDROID WHY!!!");
  zmbStackBT(t);
}
*/

/////////////////////////////////////////////////////////////////////////////////////
////// Print the Java Stack to help with so so so much debugging ////////////////////

void zmbStackBT(Thread* t, int n_frames)
{
  int printed_frames = 0;
  const StackSaveArea *saveArea;

  for(u4 *fp = t->interpSave.curFrame;
         fp != NULL;
         fp = saveArea->prevFrame)
  {
    saveArea = SAVEAREA_FROM_FP(fp);
    const Method* method = saveArea->method;
    if(method==NULL) continue;

    u4* offset = NULL;
    if (!dvmIsNativeMethod(method))
      offset = (u4*)((((u1*)(saveArea->xtra.currentPc)) - ((u1*)(method->insns)))/2);

    ZMB_LOG("======");
    ZMB_LOG("%s", method->clazz->descriptor);
    ZMB_LOG("%p : %s", offset, method->name);
    ZMB_LOG("insns : %p", method->insns);
    ZMB_LOG("currentPc : %p", saveArea->xtra.currentPc);
    ZMB_LOG("savedPc (caller) : %p", saveArea->savedPc);
    
    size_t locals;
    for (locals = 0; locals < ((u4)method->registersSize - (u4)method->insSize); ++locals) {
      ZMB_LOG("[v%d] %p", locals, (void*)fp[locals]);
    }
    for (size_t p = locals; p < method->registersSize; ++p) {
      ZMB_LOG("[p%d] %p", p - locals, (void*)fp[p]);
    }
   
    if(n_frames != 0 && ++printed_frames == n_frames) break;
  }
  ZMB_LOG("");
  ZMB_LOG("");
  ZMB_LOG("");
  ZMB_LOG("");
  ZMB_LOG("");
  ZMB_LOG("");
}

void zmbStackBT(Thread* t)
{
  zmbStackBT(t, 100);
}

/////////////////////////////////////////////////////////////////////////////////////
////////////////////////// New "Old" Object tracking ////////////////////////////////

BDS_RB_Tree<const Object*> zombie_objects;

void zmbAddZombieObject(const Object * obj) {
  if (!zmbIsZombieObject(obj)) zombie_objects.insert(obj);
}

bool zmbIsZombieObject(const Object * obj) {
  return zombie_objects.contains(obj) != zombie_objects.end();
}

void zmbRemoveZombieObject(const Object * obj) {
  if(zmbIsZombieObject(obj)) zombie_objects.erase(zombie_objects.contains(obj));
}



/////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////COUNT!!!!//////////////////////////////////////////

extern "C" void zmb_incr_insns(){
  if (curr_metrics) curr_metrics->byte_insns++;
}

extern "C" void zmb_incr_native_calls(){
  if (curr_metrics) curr_metrics->native_calls++;
}

extern "C" void zmb_incr_java_ds(){
  if (curr_metrics) curr_metrics->java_ds++;
}

extern "C" void zmb_incr_new_c_ds(){
  if (curr_metrics) curr_metrics->new_c_ds++;
}

extern "C" void zmb_incr_old_c_ds(){
  if (curr_metrics) curr_metrics->old_c_ds++;
}

/////////////////////////////////////////////////////////////////////////////////////
/////////////// This is only here to avoid adding libdl to everything ///////////////
void zmbPatchVT(const void * old) {
  if(zmbIsSafeAddr(old))
  {
    void * addr = ((void **)old)[0];
    if (zmbIsSafeAddr(addr))
    {
      char * name = zmbGetNameForOldSymbolValue(addr);
      if (name) {
        void * vtbl = dlsym(RTLD_DEFAULT, name);
	vtbl = (void *)((uintptr_t)vtbl + 0x8);	
        void ** old_v = (void**)old;
        old_v[0] = vtbl;
      }
    }
  }
}

/*
static ClassObject * viewClass = NULL;

extern "C" bool zmb_in_draw()
{
  const StackSaveArea *saveArea;

  if (dvmThreadSelf() == NULL) return false;
 
  for(u4 *fp = dvmThreadSelf()->interpSave.curFrame;
         fp != NULL;
         fp = saveArea->prevFrame)
  {
    saveArea = SAVEAREA_FROM_FP(fp);
    const Method* method = saveArea->method;
    if(method==NULL) continue;

    if (!viewClass) viewClass = dvmFindLoadedClass("Landroid/view/View;");
    if (dvmIsSubClass(method->clazz, viewClass) && strcmp(method->name, "draw")==0)
    {
      return true;
    }
  }
  return false;
}
*/
//unsigned zmbNative_gui_mem=0; 
//unsigned zmbNative_internal_mem=0;

//struct zmbPair {
//    BDS_RB_Tree<unsigned> memory;
//    unsigned bytes;
//    zmbPair() : memory() {
//      bytes=0;
//    }
//};
//
//BDS_List<zmbPair *> zmbNative_gui_mem;
//zmbPair zmbNative_internal_mem;
//BDS_List<zmbPair *> zmbApp_gui_mem;
//zmbPair zmbApp_internal_mem;
//
//void zmbAppScreenChange(){
//  zmbApp_gui_mem.push_front(new zmbPair);
//  zmbNative_gui_mem.push_front(new zmbPair);
//}
//
//unsigned lastTime=0;
//static void zmbAppPrint(){
//  unsigned currTime = (unsigned)time(NULL);
//  if (currTime>lastTime+1){ 
//    ZMB_LOG("\n\n\nCOUNT_METRIC===============================================");
//    ZMB_LOG("COUNT_METRIC== ZMBAPP pid %d:: GUI::", getpid());
//    unsigned screen=0;
//    for (BDS_List<zmbPair *>::iterator it = zmbApp_gui_mem.begin(), it1 = zmbNative_gui_mem.begin(); it != zmbApp_gui_mem.end() && it1 != zmbNative_gui_mem.end(); ++it, ++it1, ++screen) {
//      zmbPair * pairApp = *it;
//      zmbPair * pairNative = *it1;
//      unsigned allocAppCount = 0;
//      for (BDS_RB_Tree<unsigned>::iterator it01 = pairApp->memory.begin(); it01 != pairApp->memory.end(); ++it01, ++allocAppCount) ;
//      unsigned allocNativeCount = 0;
//      for (BDS_RB_Tree<unsigned>::iterator it01 = pairNative->memory.begin(); it01 != pairNative->memory.end(); ++it01, ++allocNativeCount) ;
//      ZMB_LOG("COUNT_METRIC== \t\tScreen %d: %d + %d = %d", screen, allocAppCount, allocNativeCount, allocAppCount + allocNativeCount);
//    }
//    
//    unsigned allocAppInternalCount=0;
//    for (BDS_RB_Tree<unsigned>::iterator it01 = zmbApp_internal_mem.memory.begin(); it01 != zmbApp_internal_mem.memory.end(); ++it01, ++allocAppInternalCount) ;
//    unsigned allocNativeInternalCount=0;
//    for (BDS_RB_Tree<unsigned>::iterator it01 = zmbNative_internal_mem.memory.begin(); it01 != zmbNative_internal_mem.memory.end(); ++it01, ++allocNativeInternalCount) ;
//    ZMB_LOG("COUNT_METRIC== \tINTERNAL:: %d + %d = %d", allocAppInternalCount, allocNativeInternalCount, allocNativeInternalCount + allocAppInternalCount);
//    ZMB_LOG("\n\n\n");
//    lastTime = currTime;
//  }
//}
//
//static bool inZmbFuncs = false;
//
//static inline void zmbMemAdd(BDS_List<zmbPair *> * zmb_gui_mem, zmbPair * zmb_internal_mem, void * memory, unsigned bytes){
//  if (!inZmbFuncs){
//    inZmbFuncs = true;
//    zmbPair * right_pair = NULL;
//    if (zmb_in_draw()){
//      right_pair = *(zmb_gui_mem->begin());
//    } else {
//      right_pair = zmb_internal_mem;
//    }
//    right_pair->bytes += bytes;
//    right_pair->memory.insert((unsigned)memory);
//    zmbAppPrint();
//    inZmbFuncs = false;
//  }
//}
//
//static inline void zmbMemRemove(BDS_List<zmbPair *> * zmb_gui_mem, zmbPair * zmb_internal_mem, void * memory, unsigned bytes){
//  if (!inZmbFuncs){
//    inZmbFuncs = true;
//    
//    BDS_RB_Tree<unsigned>::iterator it1;
//    zmbPair * pair;
//    
//    for (BDS_List<zmbPair *>::iterator it = zmb_gui_mem->begin(); it != zmb_gui_mem->end(); ++it) {
//      pair = *it;
//      it1 = pair->memory.contains((unsigned)memory);
//      if (it1 != pair->memory.end()) {
//        pair->memory.erase(it1);
//        pair->bytes -= bytes;
//        goto justRet;
//      }
//    }
//
//    pair = zmb_internal_mem;
//    it1 = pair->memory.contains((unsigned)memory);
//    if (it1 != pair->memory.end()) {
//      pair->memory.erase(it1);
//      pair->bytes -= bytes;
//      goto justRet;
//    }
//   
//    ZMB_LOG("COUNT_METRIC==  %p not found!", memory);
//
//justRet:
//    zmbAppPrint();
//    inZmbFuncs = false;
//  }
//}
//
//
//void zmbNativeMemAdd(void * memory, unsigned bytes){
//  zmbMemAdd(&zmbNative_gui_mem, &zmbNative_internal_mem, memory, bytes);
//}
//
//void zmbNativeMemRemove(void * memory, unsigned bytes){
//  zmbMemRemove(&zmbNative_gui_mem, &zmbNative_internal_mem, memory, bytes);
//}
//
//void zmbAppMemAdd(void * memory, unsigned bytes){
//  zmbMemAdd(&zmbApp_gui_mem, &zmbApp_internal_mem, memory, bytes);
//}
//
//void zmbAppMemRemove(void * memory, unsigned bytes){
//  zmbMemRemove(&zmbApp_gui_mem, &zmbApp_internal_mem, memory, bytes);
//}

//void zmbAppMemCountInit(){
//  zmbAppScreenChange();
//  void (*zmbSetupCountHook)(void *, void *) = (void(*) (void *, void *)) dlsym(RTLD_DEFAULT, "zmbSetupCountHook");
//  zmbSetupCountHook((void *)zmbNativeMemAdd, (void *)zmbNativeMemRemove);
//  zmb_status = true;
//}
