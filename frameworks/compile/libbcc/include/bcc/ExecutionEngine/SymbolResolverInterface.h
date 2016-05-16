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

#ifndef BCC_EXECUTION_ENGINE_SYMBOL_RESOLVER_INTERFACE_H
#define BCC_EXECUTION_ENGINE_SYMBOL_RESOLVER_INTERFACE_H

#include <cstddef>

namespace bcc {

class SymbolResolverInterface {
public:
  static void *LookupFunction(void *pResolver, const char *pName) {
    SymbolResolverInterface *resolver =
        reinterpret_cast<SymbolResolverInterface*>(pResolver);
    return ((resolver != NULL) ? resolver->getAddress(pName) : NULL);
  }

  // Should this be a const method?
  virtual void *getAddress(const char *pName) = 0;

  virtual ~SymbolResolverInterface() { }
};

} // end namespace bcc

#endif // BCC_EXECUTION_ENGINE_SYMBOL_RESOLVER_INTERFACE_H
