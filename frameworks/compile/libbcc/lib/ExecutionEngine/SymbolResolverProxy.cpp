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

#include "bcc/ExecutionEngine/SymbolResolverProxy.h"

using namespace bcc;

void *SymbolResolverProxy::getAddress(const char *pName) {
  // Search the address of the symbol by following the chain of resolvers.
  for (size_t i = 0; i < mChain.size(); i++) {
    void *addr = mChain[i]->getAddress(pName);
    if (addr != NULL) {
      return addr;
    }
  }
  // Symbol not found or there's no resolver containing in the chain.
  return NULL;
}

void SymbolResolverProxy::chainResolver(SymbolResolverInterface &pResolver) {
  mChain.push_back(&pResolver);
}
