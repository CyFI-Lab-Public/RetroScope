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

#ifndef BCC_EXECUTION_ENGINE_SYMBOL_RESOLVER_PROXY_H
#define BCC_EXECUTION_ENGINE_SYMBOL_RESOLVER_PROXY_H

#include "bcc/ExecutionEngine/SymbolResolverInterface.h"
#include "bcc/Support/Log.h"

#include <utils/Vector.h>

namespace bcc {

class SymbolResolverProxy : public SymbolResolverInterface {
private:
  android::Vector<SymbolResolverInterface *> mChain;

public:
  SymbolResolverProxy() { }

  void chainResolver(SymbolResolverInterface &pResolver);

  virtual void *getAddress(const char *pName);
};

} // end namespace bcc

#endif // BCC_EXECUTION_ENGINE_SYMBOL_RESOLVER_PROXY_H
