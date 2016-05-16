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

#include "bcc/Assert.h"
#include "bcc/Renderscript/RSTransforms.h"

#include <cstdlib>
#include <vector>

#include <llvm/IR/DerivedTypes.h>
#include <llvm/IR/Function.h>
#include <llvm/IR/Instructions.h>
#include <llvm/IR/IRBuilder.h>
#include <llvm/IR/Module.h>
#include <llvm/Pass.h>
#include <llvm/Support/raw_ostream.h>
#include <llvm/IR/Type.h>

#include "bcc/Config/Config.h"
#include "bcc/Renderscript/RSInfo.h"
#include "bcc/Support/Log.h"

using namespace bcc;

namespace {

/* RSForEachExpandPass - This pass operates on functions that are able to be
 * called via rsForEach() or "foreach_<NAME>". We create an inner loop for the
 * ForEach-able function to be invoked over the appropriate data cells of the
 * input/output allocations (adjusting other relevant parameters as we go). We
 * support doing this for any ForEach-able compute kernels. The new function
 * name is the original function name followed by ".expand". Note that we
 * still generate code for the original function.
 */
class RSEmbedInfoPass : public llvm::ModulePass {
private:
  static char ID;

  llvm::Module *M;
  llvm::LLVMContext *C;

  const RSInfo *mInfo;

public:
  RSEmbedInfoPass(const RSInfo *info)
      : ModulePass(ID),
        mInfo(info) {
  }

  virtual bool runOnModule(llvm::Module &M) {
    this->M = &M;
    C = &M.getContext();

    std::string str;
    llvm::raw_string_ostream s(str);

    // We use a simple text format here that the compatibility library can
    // easily parse. Each section starts out with its name followed by a count.
    // The count denotes the number of lines to parse for that particular
    // category. Variables and Functions merely put the appropriate identifier
    // on the line, while ForEach kernels have the encoded int signature,
    // followed by a hyphen followed by the identifier (function to look up).
    // Object Slots are just listed as one integer per line.
    const RSInfo::ExportVarNameListTy &export_vars = mInfo->getExportVarNames();
    s << "exportVarCount: " << (unsigned int) export_vars.size() << "\n";
    for (RSInfo::ExportVarNameListTy::const_iterator
             export_var_iter = export_vars.begin(),
             export_var_end = export_vars.end();
         export_var_iter != export_var_end; export_var_iter++) {
      s << *export_var_iter << "\n";
    }

    const RSInfo::ExportFuncNameListTy &export_funcs =
        mInfo->getExportFuncNames();
    s << "exportFuncCount: " << (unsigned int) export_funcs.size() << "\n";
    for (RSInfo::ExportFuncNameListTy::const_iterator
             export_func_iter = export_funcs.begin(),
             export_func_end = export_funcs.end();
         export_func_iter != export_func_end; export_func_iter++) {
      s << *export_func_iter << "\n";
    }

    const RSInfo::ExportForeachFuncListTy &export_foreach_funcs =
        mInfo->getExportForeachFuncs();
    s << "exportForEachCount: "
      << (unsigned int) export_foreach_funcs.size() << "\n";
    for (RSInfo::ExportForeachFuncListTy::const_iterator
             foreach_func_iter = export_foreach_funcs.begin(),
             foreach_func_end = export_foreach_funcs.end();
         foreach_func_iter != foreach_func_end; foreach_func_iter++) {
      std::string name(foreach_func_iter->first);
      s << foreach_func_iter->second << " - "
        << foreach_func_iter->first << "\n";
    }

    std::vector<unsigned int> object_slot_numbers;
    unsigned int i = 0;
    const RSInfo::ObjectSlotListTy &object_slots = mInfo->getObjectSlots();
    for (RSInfo::ObjectSlotListTy::const_iterator
             slots_iter = object_slots.begin(),
             slots_end = object_slots.end();
         slots_iter != slots_end; slots_iter++) {
      if (*slots_iter) {
        object_slot_numbers.push_back(i);
      }
      i++;
    }
    s << "objectSlotCount: " << (unsigned int) object_slot_numbers.size()
      << "\n";
    for (i = 0; i < object_slot_numbers.size(); i++) {
      s << object_slot_numbers[i] << "\n";
    }

    s.flush();

    // Embed this as the global variable .rs.info so that it will be
    // accessible from the shared object later.
    llvm::Constant *Init = llvm::ConstantDataArray::getString(*C, str);
    llvm::GlobalVariable *InfoGV =
        new llvm::GlobalVariable(M, Init->getType(), true,
                                 llvm::GlobalValue::ExternalLinkage, Init,
                                 ".rs.info");
    (void) InfoGV;

    return true;
  }

  virtual const char *getPassName() const {
    return "Embed Renderscript Info";
  }

};  // end RSEmbedInfoPass

}  // end anonymous namespace

char RSEmbedInfoPass::ID = 0;

namespace bcc {

llvm::ModulePass *
createRSEmbedInfoPass(const RSInfo *info) {
  return new RSEmbedInfoPass(info);
}

}  // end namespace bcc
