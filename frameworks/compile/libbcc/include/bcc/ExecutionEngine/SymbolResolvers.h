/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef BCC_EXECUTION_ENGINE_SYMBOL_RESOLVERS_H
#define BCC_EXECUTION_ENGINE_SYMBOL_RESOLVERS_H

#include <cstdlib>
#include <cstring>

#include "SymbolResolverInterface.h"

namespace bcc {

/*
 * Symbol lookup via dlopen()/dlsym().
 */
class DyldSymbolResolver : public SymbolResolverInterface {
public:
  typedef void *HandleTy;

private:
  HandleTy mHandle;
  char *mError;

public:
  // If pFileName is NULL, it will search symbol in the current process image.
  DyldSymbolResolver(const char *pFileName, bool pLazyBinding = true);

  virtual void *getAddress(const char *pName);

  inline bool hasError() const
  { return (mError != NULL); }
  inline const char *getError() const
  { return mError; }

  ~DyldSymbolResolver();
};

/*
 * Symbol lookup by searching through an array of SymbolMap.
 */
template<typename Subclass>
class ArraySymbolResolver : public SymbolResolverInterface {
public:
  typedef struct {
    // Symbol name
    const char *mName;
    // Symbol address
    void *mAddr;
  } SymbolMap;

private:
  // True if the symbol name is sorted in the array.
  bool mSorted;

  static int CompareSymbolName(const void *pA, const void *pB) {
    return ::strcmp(reinterpret_cast<const SymbolMap *>(pA)->mName,
                    reinterpret_cast<const SymbolMap *>(pB)->mName);
  }

public:
  ArraySymbolResolver(bool pSorted = false) : mSorted(pSorted) { }

  virtual void *getAddress(const char *pName) {
    const SymbolMap *result = NULL;

    if (mSorted) {
      // Use binary search.
      const SymbolMap key = { pName, NULL };

      result = reinterpret_cast<SymbolMap *>(
                   ::bsearch(&key, Subclass::SymbolArray,
                                   Subclass::NumSymbols,
                                   sizeof(SymbolMap),
                                   CompareSymbolName));
    } else {
      // Use linear search.
      for (size_t i = 0; i < Subclass::NumSymbols; i++) {
        if (::strcmp(Subclass::SymbolArray[i].mName, pName) == 0) {
          result = &Subclass::SymbolArray[i];
          break;
        }
      }
    }

    return ((result != NULL) ? result->mAddr : NULL);
  }
};

template<typename ContextTy = void *>
class LookupFunctionSymbolResolver : public SymbolResolverInterface {
public:
  typedef void *(*LookupFunctionTy)(ContextTy pContext, const char *pName);

private:
  LookupFunctionTy mLookupFunc;
  ContextTy mContext;

public:
  LookupFunctionSymbolResolver(LookupFunctionTy pLookupFunc = NULL,
                               ContextTy pContext = NULL)
    : mLookupFunc(pLookupFunc), mContext(pContext) { }

  virtual void *getAddress(const char *pName) {
    return ((mLookupFunc != NULL) ? mLookupFunc(mContext, pName) : NULL);
  }

  inline LookupFunctionTy getLookupFunction() const
  { return mLookupFunc; }
  inline ContextTy getContext() const
  { return mContext; }

  inline void setLookupFunction(LookupFunctionTy pLookupFunc)
  { mLookupFunc = pLookupFunc; }
  inline void setContext(ContextTy pContext)
  { mContext = pContext; }
};

} // end namespace bcc

#endif // BCC_EXECUTION_ENGINE_SYMBOL_RESOLVERS_H
