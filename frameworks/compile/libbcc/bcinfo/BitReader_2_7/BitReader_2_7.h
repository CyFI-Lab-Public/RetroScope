
//===- BitReader_2_7.h - Internal BitcodeReader 2.7 impl --------*- C++ -*-===//
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

#ifndef BITREADER_2_7_H
#define BITREADER_2_7_H

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

namespace llvm_2_7 {
  
llvm::Module *ParseBitcodeFile(llvm::MemoryBuffer *Buffer,
                               llvm::LLVMContext& Context,
                               std::string *ErrMsg);

std::string getBitcodeTargetTriple(llvm::MemoryBuffer *Buffer,
                                   llvm::LLVMContext& Context,
                                   std::string *ErrMsg);

llvm::Module *getLazyBitcodeModule(llvm::MemoryBuffer *Buffer,
                                   llvm::LLVMContext& Context,
                                  std::string *ErrMsg);
} // End llvm_2_7 namespace

#endif
