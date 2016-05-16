/*
 * Copyright 2011, The Android Open Source Project
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

#include "ELFSymbol.h"
#include "ELF.h"

char const *
ELFSymbolHelperMixin::getTypeStr(uint8_t type) {
  switch (type) {
    default: return "(UNKNOWN)";

#define CASE(TYPE) \
    case STT_##TYPE: return #TYPE;

    CASE(NOTYPE)
    CASE(OBJECT)
    CASE(FUNC)
    CASE(SECTION)
    CASE(FILE)
    CASE(COMMON)
    CASE(TLS)
    CASE(LOOS)
    CASE(HIOS)
    CASE(LOPROC)
    CASE(HIPROC)

#undef CASE
  }
}

char const *
ELFSymbolHelperMixin::getBindingAttributeStr(uint8_t type) {
  switch (type) {
    default: return "(UNKNOWN)";

#define CASE(TYPE) \
    case STB_##TYPE: return #TYPE;

    CASE(LOCAL)
    CASE(GLOBAL)
    CASE(WEAK)
    CASE(LOOS)
    CASE(HIOS)
    CASE(LOPROC)
    CASE(HIPROC)

#undef CASE
  }
}
char const *
ELFSymbolHelperMixin::getVisibilityStr(uint8_t type) {
  switch (type) {
    default: return "(UNKNOWN)";

#define CASE(TYPE) \
    case STV_##TYPE: return #TYPE;

    CASE(DEFAULT)
    CASE(INTERNAL)
    CASE(HIDDEN)
    CASE(PROTECTED)

#undef CASE
  }
}
