/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "EffectsFactory"
//#define LOG_NDEBUG 0

#include "EffectsFactory.h"
#include <string.h>
#include <stdlib.h>
#include <dlfcn.h>

#include <cutils/misc.h>
#include <cutils/config_utils.h>
#include <audio_effects/audio_effects_conf.h>

static list_elem_t *gEffectList; // list of effect_entry_t: all currently created effects
static list_elem_t *gLibraryList; // list of lib_entry_t: all currently loaded libraries
// list of effect_descriptor and list of sub effects : all currently loaded
// It does not contain effects without sub effects.
static list_sub_elem_t *gSubEffectList;
static pthread_mutex_t gLibLock = PTHREAD_MUTEX_INITIALIZER; // controls access to gLibraryList
static uint32_t gNumEffects;         // total number number of effects
static list_elem_t *gCurLib;    // current library in enumeration process
static list_elem_t *gCurEffect; // current effect in enumeration process
static uint32_t gCurEffectIdx;       // current effect index in enumeration process
static lib_entry_t *gCachedLibrary;  // last library accessed by getLibrary()

static int gInitDone; // true is global initialization has been preformed
static int gCanQueryEffect; // indicates that call to EffectQueryEffect() is valid, i.e. that the list of effects
                          // was not modified since last call to EffectQueryNumberEffects()


/////////////////////////////////////////////////
//      Local functions prototypes
/////////////////////////////////////////////////

static int init();
static int loadEffectConfigFile(const char *path);
static int loadLibraries(cnode *root);
static int loadLibrary(cnode *root, const char *name);
static int loadEffects(cnode *root);
static int loadEffect(cnode *node);
// To get and add the effect pointed by the passed node to the gSubEffectList
static int addSubEffect(cnode *root);
static lib_entry_t *getLibrary(const char *path);
static void resetEffectEnumeration();
static uint32_t updateNumEffects();
static int findEffect(const effect_uuid_t *type,
               const effect_uuid_t *uuid,
               lib_entry_t **lib,
               effect_descriptor_t **desc);
// To search a subeffect in the gSubEffectList
int findSubEffect(const effect_uuid_t *uuid,
               lib_entry_t **lib,
               effect_descriptor_t **desc);
static void dumpEffectDescriptor(effect_descriptor_t *desc, char *str, size_t len);
static int stringToUuid(const char *str, effect_uuid_t *uuid);
static int uuidToString(const effect_uuid_t *uuid, char *str, size_t maxLen);

/////////////////////////////////////////////////
//      Effect Control Interface functions
/////////////////////////////////////////////////

int Effect_Process(effect_handle_t self, audio_buffer_t *inBuffer, audio_buffer_t *outBuffer)
{
    int ret = init();
    if (ret < 0) {
        return ret;
    }
    effect_entry_t *fx = (effect_entry_t *)self;
    pthread_mutex_lock(&gLibLock);
    if (fx->lib == NULL) {
        pthread_mutex_unlock(&gLibLock);
        return -EPIPE;
    }
    pthread_mutex_lock(&fx->lib->lock);
    pthread_mutex_unlock(&gLibLock);

    ret = (*fx->subItfe)->process(fx->subItfe, inBuffer, outBuffer);
    pthread_mutex_unlock(&fx->lib->lock);
    return ret;
}

int Effect_Command(effect_handle_t self,
                   uint32_t cmdCode,
                   uint32_t cmdSize,
                   void *pCmdData,
                   uint32_t *replySize,
                   void *pReplyData)
{
    int ret = init();
    if (ret < 0) {
        return ret;
    }
    effect_entry_t *fx = (effect_entry_t *)self;
    pthread_mutex_lock(&gLibLock);
    if (fx->lib == NULL) {
        pthread_mutex_unlock(&gLibLock);
        return -EPIPE;
    }
    pthread_mutex_lock(&fx->lib->lock);
    pthread_mutex_unlock(&gLibLock);

    ret = (*fx->subItfe)->command(fx->subItfe, cmdCode, cmdSize, pCmdData, replySize, pReplyData);
    pthread_mutex_unlock(&fx->lib->lock);
    return ret;
}

int Effect_GetDescriptor(effect_handle_t self,
                         effect_descriptor_t *desc)
{
    int ret = init();
    if (ret < 0) {
        return ret;
    }
    effect_entry_t *fx = (effect_entry_t *)self;
    pthread_mutex_lock(&gLibLock);
    if (fx->lib == NULL) {
        pthread_mutex_unlock(&gLibLock);
        return -EPIPE;
    }
    pthread_mutex_lock(&fx->lib->lock);
    pthread_mutex_unlock(&gLibLock);

    ret = (*fx->subItfe)->get_descriptor(fx->subItfe, desc);
    pthread_mutex_unlock(&fx->lib->lock);
    return ret;
}

int Effect_ProcessReverse(effect_handle_t self, audio_buffer_t *inBuffer, audio_buffer_t *outBuffer)
{
    int ret = init();
    if (ret < 0) {
        return ret;
    }
    effect_entry_t *fx = (effect_entry_t *)self;
    pthread_mutex_lock(&gLibLock);
    if (fx->lib == NULL) {
        pthread_mutex_unlock(&gLibLock);
        return -EPIPE;
    }
    pthread_mutex_lock(&fx->lib->lock);
    pthread_mutex_unlock(&gLibLock);

    if ((*fx->subItfe)->process_reverse != NULL) {
        ret = (*fx->subItfe)->process_reverse(fx->subItfe, inBuffer, outBuffer);
    } else {
        ret = -ENOSYS;
    }
    pthread_mutex_unlock(&fx->lib->lock);
    return ret;
}


const struct effect_interface_s gInterface = {
        Effect_Process,
        Effect_Command,
        Effect_GetDescriptor,
        NULL
};

const struct effect_interface_s gInterfaceWithReverse = {
        Effect_Process,
        Effect_Command,
        Effect_GetDescriptor,
        Effect_ProcessReverse
};

/////////////////////////////////////////////////
//      Effect Factory Interface functions
/////////////////////////////////////////////////

int EffectQueryNumberEffects(uint32_t *pNumEffects)
{
    int ret = init();
    if (ret < 0) {
        return ret;
    }
    if (pNumEffects == NULL) {
        return -EINVAL;
    }

    pthread_mutex_lock(&gLibLock);
    *pNumEffects = gNumEffects;
    gCanQueryEffect = 1;
    pthread_mutex_unlock(&gLibLock);
    ALOGV("EffectQueryNumberEffects(): %d", *pNumEffects);
    return ret;
}

int EffectQueryEffect(uint32_t index, effect_descriptor_t *pDescriptor)
{
    int ret = init();
    if (ret < 0) {
        return ret;
    }
    if (pDescriptor == NULL ||
        index >= gNumEffects) {
        return -EINVAL;
    }
    if (gCanQueryEffect == 0) {
        return -ENOSYS;
    }

    pthread_mutex_lock(&gLibLock);
    ret = -ENOENT;
    if (index < gCurEffectIdx) {
        resetEffectEnumeration();
    }
    while (gCurLib) {
        if (gCurEffect) {
            if (index == gCurEffectIdx) {
                *pDescriptor = *(effect_descriptor_t *)gCurEffect->object;
                ret = 0;
                break;
            } else {
                gCurEffect = gCurEffect->next;
                gCurEffectIdx++;
            }
        } else {
            gCurLib = gCurLib->next;
            gCurEffect = ((lib_entry_t *)gCurLib->object)->effects;
        }
    }

#if (LOG_NDEBUG == 0)
    char str[256];
    dumpEffectDescriptor(pDescriptor, str, 256);
    ALOGV("EffectQueryEffect() desc:%s", str);
#endif
    pthread_mutex_unlock(&gLibLock);
    return ret;
}

int EffectGetDescriptor(const effect_uuid_t *uuid, effect_descriptor_t *pDescriptor)
{
    lib_entry_t *l = NULL;
    effect_descriptor_t *d = NULL;

    int ret = init();
    if (ret < 0) {
        return ret;
    }
    if (pDescriptor == NULL || uuid == NULL) {
        return -EINVAL;
    }
    pthread_mutex_lock(&gLibLock);
    ret = findEffect(NULL, uuid, &l, &d);
    if (ret == 0) {
        *pDescriptor = *d;
    }
    pthread_mutex_unlock(&gLibLock);
    return ret;
}

int EffectCreate(const effect_uuid_t *uuid, int32_t sessionId, int32_t ioId, effect_handle_t *pHandle)
{
    list_elem_t *e = gLibraryList;
    lib_entry_t *l = NULL;
    effect_descriptor_t *d = NULL;
    effect_handle_t itfe;
    effect_entry_t *fx;
    int found = 0;
    int ret;

    if (uuid == NULL || pHandle == NULL) {
        return -EINVAL;
    }

    ALOGV("EffectCreate() UUID: %08X-%04X-%04X-%04X-%02X%02X%02X%02X%02X%02X\n",
            uuid->timeLow, uuid->timeMid, uuid->timeHiAndVersion,
            uuid->clockSeq, uuid->node[0], uuid->node[1],uuid->node[2],
            uuid->node[3],uuid->node[4],uuid->node[5]);

    ret = init();

    if (ret < 0) {
        ALOGW("EffectCreate() init error: %d", ret);
        return ret;
    }

    pthread_mutex_lock(&gLibLock);

    ret = findEffect(NULL, uuid, &l, &d);
    if (ret < 0){
        // Sub effects are not associated with the library->effects,
        // so, findEffect will fail. Search for the effect in gSubEffectList.
        ret = findSubEffect(uuid, &l, &d);
        if (ret < 0 ) {
            goto exit;
        }
    }

    // create effect in library
    ret = l->desc->create_effect(uuid, sessionId, ioId, &itfe);
    if (ret != 0) {
        ALOGW("EffectCreate() library %s: could not create fx %s, error %d", l->name, d->name, ret);
        goto exit;
    }

    // add entry to effect list
    fx = (effect_entry_t *)malloc(sizeof(effect_entry_t));
    fx->subItfe = itfe;
    if ((*itfe)->process_reverse != NULL) {
        fx->itfe = (struct effect_interface_s *)&gInterfaceWithReverse;
        ALOGV("EffectCreate() gInterfaceWithReverse");
    }   else {
        fx->itfe = (struct effect_interface_s *)&gInterface;
        ALOGV("EffectCreate() gInterface");
    }
    fx->lib = l;

    e = (list_elem_t *)malloc(sizeof(list_elem_t));
    e->object = fx;
    e->next = gEffectList;
    gEffectList = e;

    *pHandle = (effect_handle_t)fx;

    ALOGV("EffectCreate() created entry %p with sub itfe %p in library %s", *pHandle, itfe, l->name);

exit:
    pthread_mutex_unlock(&gLibLock);
    return ret;
}

int EffectRelease(effect_handle_t handle)
{
    effect_entry_t *fx;
    list_elem_t *e1;
    list_elem_t *e2;

    int ret = init();
    if (ret < 0) {
        return ret;
    }

    // remove effect from effect list
    pthread_mutex_lock(&gLibLock);
    e1 = gEffectList;
    e2 = NULL;
    while (e1) {
        if (e1->object == handle) {
            if (e2) {
                e2->next = e1->next;
            } else {
                gEffectList = e1->next;
            }
            fx = (effect_entry_t *)e1->object;
            free(e1);
            break;
        }
        e2 = e1;
        e1 = e1->next;
    }
    if (e1 == NULL) {
        ret = -ENOENT;
        pthread_mutex_unlock(&gLibLock);
        goto exit;
    }

    // release effect in library
    if (fx->lib == NULL) {
        ALOGW("EffectRelease() fx %p library already unloaded", handle);
        pthread_mutex_unlock(&gLibLock);
    } else {
        pthread_mutex_lock(&fx->lib->lock);
        // Releasing the gLibLock here as the list access is over as the
        // effect is removed from the list.
        // If the gLibLock is not released, we will have a deadlock situation
        // since we call the sub effect release inside the EffectRelease of Proxy
        pthread_mutex_unlock(&gLibLock);
        fx->lib->desc->release_effect(fx->subItfe);
        pthread_mutex_unlock(&fx->lib->lock);
    }
    free(fx);

exit:
    return ret;
}

int EffectIsNullUuid(const effect_uuid_t *uuid)
{
    if (memcmp(uuid, EFFECT_UUID_NULL, sizeof(effect_uuid_t))) {
        return 0;
    }
    return 1;
}

// Function to get the sub effect descriptors of the effect whose uuid
// is pointed by the first argument. It searches the gSubEffectList for the
// matching uuid and then copies the corresponding sub effect descriptors
// to the inout param
int EffectGetSubEffects(const effect_uuid_t *uuid,
                        effect_descriptor_t *pDescriptors, size_t size)
{
   ALOGV("EffectGetSubEffects() UUID: %08X-%04X-%04X-%04X-%02X%02X%02X%02X%02X"
          "%02X\n",uuid->timeLow, uuid->timeMid, uuid->timeHiAndVersion,
          uuid->clockSeq, uuid->node[0], uuid->node[1],uuid->node[2],
          uuid->node[3],uuid->node[4],uuid->node[5]);

   // Check if the size of the desc buffer is large enough for 2 subeffects
   if ((uuid == NULL) || (pDescriptors == NULL) ||
       (size < 2*sizeof(effect_descriptor_t))) {
       ALOGW("NULL pointer or insufficient memory. Cannot query subeffects");
       return -EINVAL;
   }
   int ret = init();
   if (ret < 0)
      return ret;
   list_sub_elem_t *e = gSubEffectList;
   sub_effect_entry_t *subeffect;
   effect_descriptor_t *d;
   int count = 0;
   while (e != NULL) {
       d = (effect_descriptor_t*)e->object;
       if (memcmp(uuid, &d->uuid, sizeof(effect_uuid_t)) == 0) {
           ALOGV("EffectGetSubEffects: effect found in the list");
           list_elem_t *subefx = e->sub_elem;
           while (subefx != NULL) {
               subeffect = (sub_effect_entry_t*)subefx->object;
               d = (effect_descriptor_t*)(subeffect->object);
               pDescriptors[count++] = *d;
               subefx = subefx->next;
           }
           ALOGV("EffectGetSubEffects end - copied the sub effect descriptors");
           return count;
       }
       e = e->next;
   }
   return -ENOENT;
}
/////////////////////////////////////////////////
//      Local functions
/////////////////////////////////////////////////

int init() {
    int hdl;

    if (gInitDone) {
        return 0;
    }

    pthread_mutex_init(&gLibLock, NULL);

    if (access(AUDIO_EFFECT_VENDOR_CONFIG_FILE, R_OK) == 0) {
        loadEffectConfigFile(AUDIO_EFFECT_VENDOR_CONFIG_FILE);
    } else if (access(AUDIO_EFFECT_DEFAULT_CONFIG_FILE, R_OK) == 0) {
        loadEffectConfigFile(AUDIO_EFFECT_DEFAULT_CONFIG_FILE);
    }

    updateNumEffects();
    gInitDone = 1;
    ALOGV("init() done");
    return 0;
}

int loadEffectConfigFile(const char *path)
{
    cnode *root;
    char *data;

    data = load_file(path, NULL);
    if (data == NULL) {
        return -ENODEV;
    }
    root = config_node("", "");
    config_load(root, data);
    loadLibraries(root);
    loadEffects(root);
    config_free(root);
    free(root);
    free(data);

    return 0;
}

int loadLibraries(cnode *root)
{
    cnode *node;

    node = config_find(root, LIBRARIES_TAG);
    if (node == NULL) {
        return -ENOENT;
    }
    node = node->first_child;
    while (node) {
        loadLibrary(node, node->name);
        node = node->next;
    }
    return 0;
}

int loadLibrary(cnode *root, const char *name)
{
    cnode *node;
    void *hdl;
    audio_effect_library_t *desc;
    list_elem_t *e;
    lib_entry_t *l;

    node = config_find(root, PATH_TAG);
    if (node == NULL) {
        return -EINVAL;
    }

    hdl = dlopen(node->value, RTLD_NOW);
    if (hdl == NULL) {
        ALOGW("loadLibrary() failed to open %s", node->value);
        goto error;
    }

    desc = (audio_effect_library_t *)dlsym(hdl, AUDIO_EFFECT_LIBRARY_INFO_SYM_AS_STR);
    if (desc == NULL) {
        ALOGW("loadLibrary() could not find symbol %s", AUDIO_EFFECT_LIBRARY_INFO_SYM_AS_STR);
        goto error;
    }

    if (AUDIO_EFFECT_LIBRARY_TAG != desc->tag) {
        ALOGW("getLibrary() bad tag %08x in lib info struct", desc->tag);
        goto error;
    }

    if (EFFECT_API_VERSION_MAJOR(desc->version) !=
            EFFECT_API_VERSION_MAJOR(EFFECT_LIBRARY_API_VERSION)) {
        ALOGW("loadLibrary() bad lib version %08x", desc->version);
        goto error;
    }

    // add entry for library in gLibraryList
    l = malloc(sizeof(lib_entry_t));
    l->name = strndup(name, PATH_MAX);
    l->path = strndup(node->value, PATH_MAX);
    l->handle = hdl;
    l->desc = desc;
    l->effects = NULL;
    pthread_mutex_init(&l->lock, NULL);

    e = malloc(sizeof(list_elem_t));
    e->object = l;
    pthread_mutex_lock(&gLibLock);
    e->next = gLibraryList;
    gLibraryList = e;
    pthread_mutex_unlock(&gLibLock);
    ALOGV("getLibrary() linked library %p for path %s", l, node->value);

    return 0;

error:
    if (hdl != NULL) {
        dlclose(hdl);
    }
    return -EINVAL;
}

// This will find the library and UUID tags of the sub effect pointed by the
// node, gets the effect descriptor and lib_entry_t and adds the subeffect -
// sub_entry_t to the gSubEffectList
int addSubEffect(cnode *root)
{
    ALOGV("addSubEffect");
    cnode *node;
    effect_uuid_t uuid;
    effect_descriptor_t *d;
    lib_entry_t *l;
    list_elem_t *e;
    node = config_find(root, LIBRARY_TAG);
    if (node == NULL) {
        return -EINVAL;
    }
    l = getLibrary(node->value);
    if (l == NULL) {
        ALOGW("addSubEffect() could not get library %s", node->value);
        return -EINVAL;
    }
    node = config_find(root, UUID_TAG);
    if (node == NULL) {
        return -EINVAL;
    }
    if (stringToUuid(node->value, &uuid) != 0) {
        ALOGW("addSubEffect() invalid uuid %s", node->value);
        return -EINVAL;
    }
    d = malloc(sizeof(effect_descriptor_t));
    if (l->desc->get_descriptor(&uuid, d) != 0) {
        char s[40];
        uuidToString(&uuid, s, 40);
        ALOGW("Error querying effect %s on lib %s", s, l->name);
        free(d);
        return -EINVAL;
    }
#if (LOG_NDEBUG==0)
    char s[256];
    dumpEffectDescriptor(d, s, 256);
    ALOGV("addSubEffect() read descriptor %p:%s",d, s);
#endif
    if (EFFECT_API_VERSION_MAJOR(d->apiVersion) !=
            EFFECT_API_VERSION_MAJOR(EFFECT_CONTROL_API_VERSION)) {
        ALOGW("Bad API version %08x on lib %s", d->apiVersion, l->name);
        free(d);
        return -EINVAL;
    }
    sub_effect_entry_t *sub_effect = malloc(sizeof(sub_effect_entry_t));
    sub_effect->object = d;
    // lib_entry_t is stored since the sub effects are not linked to the library
    sub_effect->lib = l;
    e = malloc(sizeof(list_elem_t));
    e->object = sub_effect;
    e->next = gSubEffectList->sub_elem;
    gSubEffectList->sub_elem = e;
    ALOGV("addSubEffect end");
    return 0;
}

int loadEffects(cnode *root)
{
    cnode *node;

    node = config_find(root, EFFECTS_TAG);
    if (node == NULL) {
        return -ENOENT;
    }
    node = node->first_child;
    while (node) {
        loadEffect(node);
        node = node->next;
    }
    return 0;
}

int loadEffect(cnode *root)
{
    cnode *node;
    effect_uuid_t uuid;
    lib_entry_t *l;
    effect_descriptor_t *d;
    list_elem_t *e;

    node = config_find(root, LIBRARY_TAG);
    if (node == NULL) {
        return -EINVAL;
    }

    l = getLibrary(node->value);
    if (l == NULL) {
        ALOGW("loadEffect() could not get library %s", node->value);
        return -EINVAL;
    }

    node = config_find(root, UUID_TAG);
    if (node == NULL) {
        return -EINVAL;
    }
    if (stringToUuid(node->value, &uuid) != 0) {
        ALOGW("loadEffect() invalid uuid %s", node->value);
        return -EINVAL;
    }

    d = malloc(sizeof(effect_descriptor_t));
    if (l->desc->get_descriptor(&uuid, d) != 0) {
        char s[40];
        uuidToString(&uuid, s, 40);
        ALOGW("Error querying effect %s on lib %s", s, l->name);
        free(d);
        return -EINVAL;
    }
#if (LOG_NDEBUG==0)
    char s[256];
    dumpEffectDescriptor(d, s, 256);
    ALOGV("loadEffect() read descriptor %p:%s",d, s);
#endif
    if (EFFECT_API_VERSION_MAJOR(d->apiVersion) !=
            EFFECT_API_VERSION_MAJOR(EFFECT_CONTROL_API_VERSION)) {
        ALOGW("Bad API version %08x on lib %s", d->apiVersion, l->name);
        free(d);
        return -EINVAL;
    }
    e = malloc(sizeof(list_elem_t));
    e->object = d;
    e->next = l->effects;
    l->effects = e;

    // After the UUID node in the config_tree, if node->next is valid,
    // that would be sub effect node.
    // Find the sub effects and add them to the gSubEffectList
    node = node->next;
    int count = 2;
    bool hwSubefx = false, swSubefx = false;
    list_sub_elem_t *sube = NULL;
    if (node != NULL) {
        ALOGV("Adding the effect to gEffectSubList as there are sub effects");
        sube = malloc(sizeof(list_sub_elem_t));
        sube->object = d;
        sube->sub_elem = NULL;
        sube->next = gSubEffectList;
        gSubEffectList = sube;
    }
    while (node != NULL && count) {
       if (addSubEffect(node)) {
           ALOGW("loadEffect() could not add subEffect %s", node->value);
           // Change the gSubEffectList to point to older list;
           gSubEffectList = sube->next;
           free(sube->sub_elem);// Free an already added sub effect
           sube->sub_elem = NULL;
           free(sube);
           return -ENOENT;
       }
       sub_effect_entry_t *subEntry = (sub_effect_entry_t*)gSubEffectList->sub_elem->object;
       effect_descriptor_t *subEffectDesc = (effect_descriptor_t*)(subEntry->object);
       // Since we return a dummy descriptor for the proxy during
       // get_descriptor call,we replace it with the correspoding
       // sw effect descriptor, but with Proxy UUID
       // check for Sw desc
        if (!((subEffectDesc->flags & EFFECT_FLAG_HW_ACC_MASK) ==
                                           EFFECT_FLAG_HW_ACC_TUNNEL)) {
             swSubefx = true;
             *d = *subEffectDesc;
             d->uuid = uuid;
             ALOGV("loadEffect() Changed the Proxy desc");
       } else
           hwSubefx = true;
       count--;
       node = node->next;
    }
    // 1 HW and 1 SW sub effect found. Set the offload flag in the Proxy desc
    if (hwSubefx && swSubefx) {
        d->flags |= EFFECT_FLAG_OFFLOAD_SUPPORTED;
    }
    return 0;
}

// Searches the sub effect matching to the specified uuid
// in the gSubEffectList. It gets the lib_entry_t for
// the matched sub_effect . Used in EffectCreate of sub effects
int findSubEffect(const effect_uuid_t *uuid,
               lib_entry_t **lib,
               effect_descriptor_t **desc)
{
    list_sub_elem_t *e = gSubEffectList;
    list_elem_t *subefx;
    sub_effect_entry_t *effect;
    lib_entry_t *l = NULL;
    effect_descriptor_t *d = NULL;
    int found = 0;
    int ret = 0;

    if (uuid == NULL)
        return -EINVAL;

    while (e != NULL && !found) {
        subefx = (list_elem_t*)(e->sub_elem);
        while (subefx != NULL) {
            effect = (sub_effect_entry_t*)subefx->object;
            l = (lib_entry_t *)effect->lib;
            d = (effect_descriptor_t *)effect->object;
            if (memcmp(&d->uuid, uuid, sizeof(effect_uuid_t)) == 0) {
                ALOGV("uuid matched");
                found = 1;
                break;
            }
            subefx = subefx->next;
        }
        e = e->next;
    }
    if (!found) {
        ALOGV("findSubEffect() effect not found");
        ret = -ENOENT;
    } else {
        ALOGV("findSubEffect() found effect: %s in lib %s", d->name, l->name);
        *lib = l;
        if (desc != NULL) {
            *desc = d;
        }
    }
    return ret;
}

lib_entry_t *getLibrary(const char *name)
{
    list_elem_t *e;

    if (gCachedLibrary &&
            !strncmp(gCachedLibrary->name, name, PATH_MAX)) {
        return gCachedLibrary;
    }

    e = gLibraryList;
    while (e) {
        lib_entry_t *l = (lib_entry_t *)e->object;
        if (!strcmp(l->name, name)) {
            gCachedLibrary = l;
            return l;
        }
        e = e->next;
    }

    return NULL;
}


void resetEffectEnumeration()
{
    gCurLib = gLibraryList;
    gCurEffect = NULL;
    if (gCurLib) {
        gCurEffect = ((lib_entry_t *)gCurLib->object)->effects;
    }
    gCurEffectIdx = 0;
}

uint32_t updateNumEffects() {
    list_elem_t *e;
    uint32_t cnt = 0;

    resetEffectEnumeration();

    e = gLibraryList;
    while (e) {
        lib_entry_t *l = (lib_entry_t *)e->object;
        list_elem_t *efx = l->effects;
        while (efx) {
            cnt++;
            efx = efx->next;
        }
        e = e->next;
    }
    gNumEffects = cnt;
    gCanQueryEffect = 0;
    return cnt;
}

int findEffect(const effect_uuid_t *type,
               const effect_uuid_t *uuid,
               lib_entry_t **lib,
               effect_descriptor_t **desc)
{
    list_elem_t *e = gLibraryList;
    lib_entry_t *l = NULL;
    effect_descriptor_t *d = NULL;
    int found = 0;
    int ret = 0;

    while (e && !found) {
        l = (lib_entry_t *)e->object;
        list_elem_t *efx = l->effects;
        while (efx) {
            d = (effect_descriptor_t *)efx->object;
            if (type != NULL && memcmp(&d->type, type, sizeof(effect_uuid_t)) == 0) {
                found = 1;
                break;
            }
            if (uuid != NULL && memcmp(&d->uuid, uuid, sizeof(effect_uuid_t)) == 0) {
                found = 1;
                break;
            }
            efx = efx->next;
        }
        e = e->next;
    }
    if (!found) {
        ALOGV("findEffect() effect not found");
        ret = -ENOENT;
    } else {
        ALOGV("findEffect() found effect: %s in lib %s", d->name, l->name);
        *lib = l;
        if (desc) {
            *desc = d;
        }
    }

    return ret;
}

void dumpEffectDescriptor(effect_descriptor_t *desc, char *str, size_t len) {
    char s[256];

    snprintf(str, len, "\nEffect Descriptor %p:\n", desc);
    strncat(str, "- TYPE: ", len);
    uuidToString(&desc->uuid, s, 256);
    snprintf(str, len, "- UUID: %s\n", s);
    uuidToString(&desc->type, s, 256);
    snprintf(str, len, "- TYPE: %s\n", s);
    sprintf(s, "- apiVersion: %08X\n- flags: %08X\n",
            desc->apiVersion, desc->flags);
    strncat(str, s, len);
    sprintf(s, "- name: %s\n", desc->name);
    strncat(str, s, len);
    sprintf(s, "- implementor: %s\n", desc->implementor);
    strncat(str, s, len);
}

int stringToUuid(const char *str, effect_uuid_t *uuid)
{
    int tmp[10];

    if (sscanf(str, "%08x-%04x-%04x-%04x-%02x%02x%02x%02x%02x%02x",
            tmp, tmp+1, tmp+2, tmp+3, tmp+4, tmp+5, tmp+6, tmp+7, tmp+8, tmp+9) < 10) {
        return -EINVAL;
    }
    uuid->timeLow = (uint32_t)tmp[0];
    uuid->timeMid = (uint16_t)tmp[1];
    uuid->timeHiAndVersion = (uint16_t)tmp[2];
    uuid->clockSeq = (uint16_t)tmp[3];
    uuid->node[0] = (uint8_t)tmp[4];
    uuid->node[1] = (uint8_t)tmp[5];
    uuid->node[2] = (uint8_t)tmp[6];
    uuid->node[3] = (uint8_t)tmp[7];
    uuid->node[4] = (uint8_t)tmp[8];
    uuid->node[5] = (uint8_t)tmp[9];

    return 0;
}

int uuidToString(const effect_uuid_t *uuid, char *str, size_t maxLen)
{

    snprintf(str, maxLen, "%08x-%04x-%04x-%04x-%02x%02x%02x%02x%02x%02x",
            uuid->timeLow,
            uuid->timeMid,
            uuid->timeHiAndVersion,
            uuid->clockSeq,
            uuid->node[0],
            uuid->node[1],
            uuid->node[2],
            uuid->node[3],
            uuid->node[4],
            uuid->node[5]);

    return 0;
}

