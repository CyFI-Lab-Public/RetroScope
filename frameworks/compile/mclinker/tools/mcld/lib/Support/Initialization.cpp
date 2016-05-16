//===- Initialization.cpp -------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//

#include "alone/Support/Initialization.h"

#include <cstdlib>

#include <llvm/Support/ErrorHandling.h>
#include <llvm/Support/TargetSelect.h>

#include <mcld/Support/TargetSelect.h>
#include <mcld/Support/TargetRegistry.h>

#include "alone/Config/Config.h"
#include "alone/Support/Log.h"

namespace {

void llvm_error_handler(void *pUserData, const std::string &pMessage) {
  ALOGE("%s", pMessage.c_str());
  ::exit(1);
}

} // end anonymous namespace

void alone::init::Initialize() {
  static bool is_initialized = false;

  if (is_initialized) {
    return;
  }

  // Setup error handler for LLVM.
  llvm::remove_fatal_error_handler();
  llvm::install_fatal_error_handler(llvm_error_handler, NULL);

#if defined(PROVIDE_ARM_CODEGEN)
  LLVMInitializeARMAsmPrinter();
# if USE_DISASSEMBLER
  LLVMInitializeARMDisassembler();
# endif
  LLVMInitializeARMTargetMC();
  LLVMInitializeARMTargetInfo();
  LLVMInitializeARMTarget();
  MCLDInitializeARMLDTargetInfo();
  MCLDInitializeARMLDTarget();
  MCLDInitializeARMLDBackend();
  MCLDInitializeARMDiagnosticLineInfo();
#endif

#if defined(PROVIDE_MIPS_CODEGEN)
  LLVMInitializeMipsAsmPrinter();
# if USE_DISASSEMBLER
  LLVMInitializeMipsDisassembler();
# endif
  LLVMInitializeMipsTargetMC();
  LLVMInitializeMipsTargetInfo();
  LLVMInitializeMipsTarget();
  MCLDInitializeMipsLDTargetInfo();
  MCLDInitializeMipsLDTarget();
  MCLDInitializeMipsLDBackend();
  MCLDInitializeMipsDiagnosticLineInfo();
#endif

#if defined(PROVIDE_X86_CODEGEN)
  LLVMInitializeX86AsmPrinter();
# if USE_DISASSEMBLER
  LLVMInitializeX86Disassembler();
# endif
  LLVMInitializeX86TargetMC();
  LLVMInitializeX86TargetInfo();
  LLVMInitializeX86Target();
  MCLDInitializeX86LDTargetInfo();
  MCLDInitializeX86LDTarget();
  MCLDInitializeX86LDBackend();
  MCLDInitializeX86DiagnosticLineInfo();
#endif

  is_initialized = true;

  return;
}
