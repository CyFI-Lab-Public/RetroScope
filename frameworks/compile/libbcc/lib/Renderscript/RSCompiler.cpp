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

#include "bcc/Renderscript/RSCompiler.h"

#include <llvm/IR/Module.h>
#include <llvm/PassManager.h>
#include <llvm/Transforms/IPO.h>

#include "bcc/Renderscript/RSExecutable.h"
#include "bcc/Renderscript/RSInfo.h"
#include "bcc/Renderscript/RSScript.h"
#include "bcc/Renderscript/RSTransforms.h"
#include "bcc/Source.h"
#include "bcc/Support/Log.h"

using namespace bcc;

bool RSCompiler::addInternalizeSymbolsPass(Script &pScript, llvm::PassManager &pPM) {
  // Add a pass to internalize the symbols that don't need to have global
  // visibility.
  RSScript &script = static_cast<RSScript &>(pScript);
  const RSInfo *info = script.getInfo();

  // The vector contains the symbols that should not be internalized.
  std::vector<const char *> export_symbols;

  // Special RS functions should always be global symbols.
  const char **special_functions = RSExecutable::SpecialFunctionNames;
  while (*special_functions != NULL) {
    export_symbols.push_back(*special_functions);
    special_functions++;
  }

  // Visibility of symbols appeared in rs_export_var and rs_export_func should
  // also be preserved.
  const RSInfo::ExportVarNameListTy &export_vars = info->getExportVarNames();
  const RSInfo::ExportFuncNameListTy &export_funcs = info->getExportFuncNames();

  for (RSInfo::ExportVarNameListTy::const_iterator
           export_var_iter = export_vars.begin(),
           export_var_end = export_vars.end();
       export_var_iter != export_var_end; export_var_iter++) {
    export_symbols.push_back(*export_var_iter);
  }

  for (RSInfo::ExportFuncNameListTy::const_iterator
           export_func_iter = export_funcs.begin(),
           export_func_end = export_funcs.end();
       export_func_iter != export_func_end; export_func_iter++) {
    export_symbols.push_back(*export_func_iter);
  }

  // Expanded foreach functions should not be internalized, too.
  const RSInfo::ExportForeachFuncListTy &export_foreach_func =
      info->getExportForeachFuncs();
  std::vector<std::string> expanded_foreach_funcs;
  for (RSInfo::ExportForeachFuncListTy::const_iterator
           foreach_func_iter = export_foreach_func.begin(),
           foreach_func_end = export_foreach_func.end();
       foreach_func_iter != foreach_func_end; foreach_func_iter++) {
    std::string name(foreach_func_iter->first);
    expanded_foreach_funcs.push_back(name.append(".expand"));
  }

  // Need to wait until ForEachExpandList is fully populated to fill in
  // exported symbols.
  for (size_t i = 0; i < expanded_foreach_funcs.size(); i++) {
    export_symbols.push_back(expanded_foreach_funcs[i].c_str());
  }

  pPM.add(llvm::createInternalizePass(export_symbols));

  return true;
}

bool RSCompiler::addExpandForEachPass(Script &pScript, llvm::PassManager &pPM) {
  // Script passed to RSCompiler must be a RSScript.
  RSScript &script = static_cast<RSScript &>(pScript);
  const RSInfo *info = script.getInfo();
  llvm::Module &module = script.getSource().getModule();

  if (info == NULL) {
    ALOGE("Missing RSInfo in RSScript to run the pass for foreach expansion on "
          "%s!", module.getModuleIdentifier().c_str());
    return false;
  }

  // Expand ForEach on CPU path to reduce launch overhead.
  bool pEnableStepOpt = true;
  pPM.add(createRSForEachExpandPass(info->getExportForeachFuncs(),
                                    pEnableStepOpt));
  if (script.getEmbedInfo())
    pPM.add(createRSEmbedInfoPass(info));

  return true;
}

bool RSCompiler::beforeAddLTOPasses(Script &pScript, llvm::PassManager &pPM) {
  if (!addExpandForEachPass(pScript, pPM))
    return false;

  if (!addInternalizeSymbolsPass(pScript, pPM))
    return false;

  return true;
}
