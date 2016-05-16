/*
 * Copyright 2011-2012, The Android Open Source Project
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

#ifndef BCC_SUPPORT_DISASSEMBLER_H
#define BCC_SUPPORT_DISASSEMBLER_H

#include <stdint.h>
#include <stddef.h>

namespace llvm {
  class raw_ostream;
} // end namespace llvm

namespace bcc {

class OutputFile;

enum DisassembleResult {
  kDisassembleSuccess,
  kDisassemblerNotAvailable,
  kDisassembleInvalidOutput,
  kDisassembleFailedPrepareOutput,
  kDisassembleUnknownTarget,
  kDisassembleFailedSetup,
  kDisassembleOutOfMemory,
  kDisassembleInvalidInstruction,
};

DisassembleResult Disassemble(llvm::raw_ostream &pOutput, const char *pTriple,
                              const char *pFuncName, const uint8_t *pFunc,
                              size_t pFuncSize);

DisassembleResult Disassemble(OutputFile &pOutput, const char *pTriple,
                              const char *pFuncName, const uint8_t *pFunc,
                              size_t pFuncSize);

} // end namespace bcc

#endif // BCC_SUPPORT_DISASSEMBLER_H
