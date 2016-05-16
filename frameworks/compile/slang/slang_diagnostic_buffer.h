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

#ifndef _FRAMEWORKS_COMPILE_SLANG_SLANG_DIAGNOSTIC_BUFFER_H_  // NOLINT
#define _FRAMEWORKS_COMPILE_SLANG_SLANG_DIAGNOSTIC_BUFFER_H_

#include <string>

#include "clang/Basic/Diagnostic.h"

#include "llvm/Support/raw_ostream.h"

namespace llvm {
  class raw_string_ostream;
}

namespace slang {

// The diagnostics consumer instance (for reading the processed diagnostics)
class DiagnosticBuffer : public clang::DiagnosticConsumer {
 private:
  std::string mDiags;
  llvm::OwningPtr<llvm::raw_string_ostream> mSOS;

 public:
  DiagnosticBuffer();

  explicit DiagnosticBuffer(DiagnosticBuffer const &src);

  virtual ~DiagnosticBuffer();

  virtual void HandleDiagnostic(clang::DiagnosticsEngine::Level DiagLevel,
                                const clang::Diagnostic& Info);

  virtual clang::DiagnosticConsumer *
    clone(clang::DiagnosticsEngine &Diags) const;

  inline const std::string &str() const {
    mSOS->flush();
    return mDiags;
  }

  inline void reset() {
    this->mSOS->str().clear();
  }
};

}  // namespace slang

#endif  // _FRAMEWORKS_COMPILE_SLANG_SLANG_DIAGNOSTIC_BUFFER_H_  NOLINT
