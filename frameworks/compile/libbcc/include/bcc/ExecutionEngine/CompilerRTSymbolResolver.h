/*
 * Copyright 2013, The Android Open Source Project
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

#ifndef BCC_EXECUTION_ENGINE_COMPILER_RT_SYMBOL_RESOLVERS_H
#define BCC_EXECUTION_ENGINE_COMPILER_RT_SYMBOL_RESOLVERS_H

#include "bcc/ExecutionEngine/SymbolResolvers.h"

namespace bcc {

class CompilerRTSymbolResolver : public DyldSymbolResolver {
public:
 CompilerRTSymbolResolver() :
      DyldSymbolResolver("/system/lib/libcompiler_rt.so") { }

 virtual void *getAddress(const char *pName) {
   // Compiler runtime functions are always prefixed by "__"
   if (pName && (pName[0] == '_') && (pName[1] == '_')) {
     return DyldSymbolResolver::getAddress(pName);
   } else {
     return NULL;
   }
 }
};

} // end namespace bcc

#endif // BCC_EXECUTION_ENGINE_COMPILER_RT_SYMBOL_RESOLVERS_H
