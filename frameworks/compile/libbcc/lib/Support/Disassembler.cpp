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

#include "bcc/Support/Disassembler.h"

#include "bcc/Config/Config.h"
#if USE_DISASSEMBLER

#include <string>

#include <llvm/IR/LLVMContext.h>

#include <llvm/MC/MCAsmInfo.h>
#include <llvm/MC/MCDisassembler.h>
#include <llvm/MC/MCInst.h>
#include <llvm/MC/MCInstPrinter.h>
#include <llvm/MC/MCInstrInfo.h>
#include <llvm/MC/MCRegisterInfo.h>
#include <llvm/MC/MCSubtargetInfo.h>

#include <llvm/Support/MemoryObject.h>
#include <llvm/Support/TargetRegistry.h>
#include <llvm/Support/raw_ostream.h>

#include "bcc/Support/OutputFile.h"
#include "bcc/Support/Log.h"

namespace {

class BufferMemoryObject : public llvm::MemoryObject {
private:
  const uint8_t *mBytes;
  uint64_t mLength;

public:
  BufferMemoryObject(const uint8_t *pBytes, uint64_t pLength)
    : mBytes(pBytes), mLength(pLength) {
  }

  virtual uint64_t getBase() const { return 0; }
  virtual uint64_t getExtent() const { return mLength; }

  virtual int readByte(uint64_t pAddr, uint8_t *pByte) const {
    if (pAddr > getExtent())
      return -1;
    *pByte = mBytes[pAddr];
    return 0;
  }
};

} // namespace anonymous

namespace bcc {

DisassembleResult Disassemble(llvm::raw_ostream &pOutput, const char *pTriple,
                              const char *pFuncName, const uint8_t *pFunc,
                              size_t pFuncSize) {
  DisassembleResult result = kDisassembleSuccess;
  uint64_t i = 0;

  const llvm::MCSubtargetInfo *subtarget_info = NULL;
  const llvm::MCDisassembler *disassembler = NULL;
  const llvm::MCInstrInfo *mc_inst_info = NULL;
  const llvm::MCRegisterInfo *mc_reg_info = NULL;
  const llvm::MCAsmInfo *asm_info = NULL;
  llvm::MCInstPrinter *inst_printer = NULL;

  BufferMemoryObject *input_function = NULL;

  std::string error;
  const llvm::Target* target =
      llvm::TargetRegistry::lookupTarget(pTriple, error);

  if (target == NULL) {
    ALOGE("Invalid target triple for disassembler: %s (%s)!",
          pTriple, error.c_str());
    return kDisassembleUnknownTarget;
  }

  subtarget_info =
      target->createMCSubtargetInfo(pTriple, /* CPU */"", /* Features */"");;

  if (subtarget_info == NULL) {
    result = kDisassembleFailedSetup;
    goto bail;
  }

  disassembler = target->createMCDisassembler(*subtarget_info);

  mc_inst_info = target->createMCInstrInfo();

  mc_reg_info = target->createMCRegInfo(pTriple);

  asm_info = target->createMCAsmInfo(pTriple);

  if ((disassembler == NULL) || (mc_inst_info == NULL) ||
      (mc_reg_info == NULL) || (asm_info == NULL)) {
    result = kDisassembleFailedSetup;
    goto bail;
  }

  inst_printer = target->createMCInstPrinter(asm_info->getAssemblerDialect(),
                                             *asm_info, *mc_inst_info,
                                             *mc_reg_info, *subtarget_info);

  if (inst_printer == NULL) {
    result = kDisassembleFailedSetup;
    goto bail;
  }

  input_function = new (std::nothrow) BufferMemoryObject(pFunc, pFuncSize);

  if (input_function == NULL) {
    result = kDisassembleOutOfMemory;
    goto bail;
  }

  // Disassemble the given function
  pOutput << "Disassembled code: " << pFuncName << "\n";

  while (i < pFuncSize) {
    llvm::MCInst inst;
    uint64_t inst_size;

    llvm::MCDisassembler::DecodeStatus decode_result =
        disassembler->getInstruction(inst, inst_size, *input_function, i,
                                     llvm::nulls(), llvm::nulls());

    switch (decode_result) {
      case llvm::MCDisassembler::Fail: {
        ALOGW("Invalid instruction encoding encountered at %llu of function %s "
              "under %s.", i, pFuncName, pTriple);
        i++;
        break;
      }
      case llvm::MCDisassembler::SoftFail: {
        ALOGW("Potentially undefined instruction encoding encountered at %llu "
              "of function %s under %s.", i, pFuncName, pTriple);
        // fall-through
      }
      case llvm::MCDisassembler::Success : {
        const uint8_t *inst_addr = pFunc + i;

        pOutput.indent(4);
        pOutput << "0x";
        pOutput.write_hex(reinterpret_cast<uintptr_t>(inst_addr));
        pOutput << ": 0x";
        pOutput.write_hex(*reinterpret_cast<const uint32_t *>(inst_addr));
        inst_printer->printInst(&inst, pOutput, /* Annot */"");
        pOutput << "\n";

        i += inst_size;
        break;
      }
    }
  }

  pOutput << "\n";

bail:
  // Clean up
  delete input_function;
  delete inst_printer;
  delete asm_info;
  delete mc_reg_info;
  delete mc_inst_info;
  delete disassembler;
  delete subtarget_info;

  return result;
}

DisassembleResult Disassemble(OutputFile &pOutput, const char *pTriple,
                              const char *pFuncName, const uint8_t *pFunc,
                              size_t FuncSize) {
  // Check the state of the specified output file.
  if (pOutput.hasError()) {
    return kDisassembleInvalidOutput;
  }

  // Open the output file decorated in llvm::raw_ostream.
  llvm::raw_ostream *output = pOutput.dup();
  if (output == NULL) {
    return kDisassembleFailedPrepareOutput;
  }

  // Delegate the request.
  DisassembleResult result =
      Disassemble(*output, pTriple, pFuncName, pFunc, FuncSize);

  // Close the output before return.
  delete output;

  return result;
}

} // namespace bcc

#else

bcc::DisassembleResult Disassemble(llvm::raw_ostream &pOutput,
                                   const char *pTriple, const char *pFuncName,
                                   const uint8_t *pFunc, size_t pFuncSize) {
  return bcc::kDisassemblerNotAvailable;
}

bcc::DisassembleResult bcc::Disassemble(OutputFile &pOutput,
                                        const char *pTriple,
                                        const char *pFuncName,
                                        const uint8_t *pFunc,
                                        size_t pFuncSize) {
  return bcc::kDisassemblerNotAvailable;
}

#endif // USE_DISASSEMBLER
