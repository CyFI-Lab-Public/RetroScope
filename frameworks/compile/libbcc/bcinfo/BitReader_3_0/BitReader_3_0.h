//===- BitReader_3_0.h - Internal BitcodeReader 3.0 impl --------*- C++ -*-===//
//
//                     The LLVM Compiler Infrastructure
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
//
// This header defines the BitcodeReader class.
//
//===----------------------------------------------------------------------===//

#ifndef BITREADER_3_0_H
#define BITREADER_3_0_H

#include "llvm/GVMaterializer.h"
#include "llvm/IR/Attributes.h"
#include "llvm/IR/Type.h"
#include "llvm/IR/OperandTraits.h"
#include "llvm/Bitcode/BitstreamReader.h"
#include "llvm/Bitcode/LLVMBitCodes.h"
#include "llvm/Support/ValueHandle.h"
#include "llvm/ADT/DenseMap.h"
#include <string>

namespace llvm {
  class MemoryBuffer;
  class LLVMContext;
  class Module;
} // End llvm namespace

namespace llvm_3_0 {

llvm::Module *ParseBitcodeFile(llvm::MemoryBuffer *Buffer,
                               llvm::LLVMContext& Context,
                               std::string *ErrMsg);

std::string getBitcodeTargetTriple(llvm::MemoryBuffer *Buffer,
                                   llvm::LLVMContext& Context,
                                   std::string *ErrMsg);

llvm::Module *getLazyBitcodeModule(llvm::MemoryBuffer *Buffer,
                                   llvm::LLVMContext& Context,
                                  std::string *ErrMsg);
} // End llvm_3_0 namespace

#endif
