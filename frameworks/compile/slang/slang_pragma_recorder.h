/*
 * Copyright 2010, The Android Open Source Project
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

#ifndef _FRAMEWORKS_COMPILE_SLANG_SLANG_PRAGMA_RECORDER_H_  // NOLINT
#define _FRAMEWORKS_COMPILE_SLANG_SLANG_PRAGMA_RECORDER_H_

#include <list>
#include <string>
#include <utility>

#include "clang/Lex/Pragma.h"

namespace clang {
  class Token;
  class Preprocessor;
}

namespace slang {

typedef std::list< std::pair<std::string, std::string> > PragmaList;

class PragmaRecorder : public clang::PragmaHandler {
 private:
  PragmaList *mPragmas;

  static bool GetPragmaNameFromToken(const clang::Token &Token,
                                     std::string &PragmaName);

  static bool GetPragmaValueFromToken(const clang::Token &Token,
                                      std::string &PragmaValue);

 public:
  explicit PragmaRecorder(PragmaList *Pragmas);

  virtual void HandlePragma(clang::Preprocessor &PP,
                            clang::PragmaIntroducerKind Introducer,
                            clang::Token &FirstToken);
};
}  // namespace slang

#endif  // _FRAMEWORKS_COMPILE_SLANG_SLANG_PRAGMA_RECORDER_H_  NOLINT
