/*
 * Copyright (C) 2009 The Android Open Source Project
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

#include "EmojiFactory.h"

#define LOG_TAG "EmojiFactory"
#include <utils/Log.h>
#include <utils/Vector.h>

#include <cutils/properties.h>

#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>


namespace android {

static pthread_once_t g_once = PTHREAD_ONCE_INIT;
static Vector<EmojiFactory *> *g_factories = NULL;
static Vector<void *> *g_handles = NULL;

class EmojiFactoryManager {
 public:
  void Init();
  virtual ~EmojiFactoryManager();
 private:
  void TryRegisterEmojiFactory(const char *library_name);
};

// Note: I previously did this procedure in the construcor. However,
// property_get() didn't return a correct value in that context. I guess
// property_get() does not return correct values before AndroidRuntime
// instance (or exactly, AppRuntime in instance app_main.cpp) is
// fully ready (see AndroidRunitem.cpp and app_main.cpp).
// So, instead of doing this in constructor, I decided this shoud be done
// when a user requires to EmojiFactory, which makes better sense to me.
void EmojiFactoryManager::Init() {
  g_handles = new Vector<void *>();
  g_factories = new Vector<EmojiFactory *>();

  char *emoji_libraries = new char[PROPERTY_VALUE_MAX];
  int len = property_get("ro.config.libemoji", emoji_libraries, "");
  // ALOGD("ro.config.libemoji: %s", emoji_libraries);
  if (len > 0) {
    char *saveptr, *ptr;
    ptr = emoji_libraries;
    while (true) {
      ptr = strtok_r(ptr, ":", &saveptr);
      if (NULL == ptr) {
        break;
      }
      TryRegisterEmojiFactory(ptr);
      ptr = NULL;
    }
  }

  delete [] emoji_libraries;
}

void EmojiFactoryManager::TryRegisterEmojiFactory(const char *library_name) {
  void *handle = dlopen(library_name, RTLD_LAZY | RTLD_LOCAL);
  if (handle == NULL) {
    const char* error_str = dlerror();
    if (error_str) {
      error_str = "Unknown reason";
    }
    ALOGE("Failed to load shared library %s: %s", library_name, error_str);
    return;
  }
  EmojiFactory *(*get_emoji_factory)() =
      reinterpret_cast<EmojiFactory *(*)()>(dlsym(handle,
                                                  "GetEmojiFactory"));
  if (get_emoji_factory == NULL) {
    const char* error_str = dlerror();
    if (error_str) {
      error_str = "Unknown reason";
    }
    ALOGE("Failed to call GetEmojiFactory: %s", error_str);
    dlclose(handle);
    return;
  }

  EmojiFactory *factory = (*get_emoji_factory)();
  if (NULL == factory) {
    ALOGE("Returned factory is NULL");
    dlclose(handle);
    return;
  }

  const char *name = factory->Name();

  size_t size = g_factories->size();
  for (size_t i = 0; i < size; ++i) {
    EmojiFactory *f = g_factories->itemAt(i);
    if (!strcmp(name, f->Name())) {
      ALOGE("Same EmojiFactory was found: %s", name);
      delete factory;
      dlclose(handle);
      return;
    }
  }
  g_factories->push(factory);
  // dlclose() must not be called here, since returned factory may point to
  // static data in the shared library (like "static const char* = "emoji";")
  g_handles->push(handle);
}

EmojiFactoryManager::~EmojiFactoryManager() {
  if (g_factories != NULL) {
    size_t size = g_factories->size();
    for (size_t i = 0; i < size; ++i) {
      delete g_factories->itemAt(i);
    }
    delete g_factories;
  }

  if (g_handles != NULL) {
    size_t size = g_handles->size();
    for (size_t i = 0; i < size; ++i) {
      dlclose(g_handles->itemAt(i));
    }
    delete g_handles;
  }
}

static EmojiFactoryManager g_registrar;

static void InitializeEmojiFactory() {
  g_registrar.Init();
}

/* static */
EmojiFactory *EmojiFactory::GetImplementation(const char *name) {
  pthread_once(&g_once, InitializeEmojiFactory);
  if (NULL == name) {
    return NULL;
  }
  size_t size = g_factories->size();
  for (size_t i = 0; i < size; ++i) {
    EmojiFactory *factory = g_factories->itemAt(i);
    if (!strcmp(name, factory->Name())) {
      return factory;
    }
  }
  return NULL;
}

/* static */
EmojiFactory *EmojiFactory::GetAvailableImplementation() {
  pthread_once(&g_once, InitializeEmojiFactory);
  size_t size = g_factories->size();
  for (size_t i = 0; i < size; ++i) {
    EmojiFactory *factory = g_factories->itemAt(i);
    return factory;
  }
  return NULL;
}

}  // namespace android

extern "C" android::EmojiFactory *GetImplementation(
    const char *name) {
  return android::EmojiFactory::GetImplementation(name);
}

extern "C" android::EmojiFactory *GetAvailableImplementation() {
  return android::EmojiFactory::GetAvailableImplementation();
}
