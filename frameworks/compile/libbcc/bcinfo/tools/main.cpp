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

#include <bcinfo/BitcodeTranslator.h>
#include <bcinfo/BitcodeWrapper.h>
#include <bcinfo/MetadataExtractor.h>

#include <llvm/ADT/OwningPtr.h>
#include <llvm/ADT/StringRef.h>
#include <llvm/Assembly/AssemblyAnnotationWriter.h>
#include <llvm/Bitcode/ReaderWriter.h>
#include <llvm/IR/LLVMContext.h>
#include <llvm/IR/Module.h>
#include <llvm/Support/ManagedStatic.h>
#include <llvm/Support/MemoryBuffer.h>
#include <llvm/Support/ToolOutputFile.h>

#include <ctype.h>
#include <dlfcn.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <getopt.h>

#include <errno.h>
#include <sys/stat.h>
#include <sys/types.h>

#include <unistd.h>

#include <string>
#include <vector>

// This file corresponds to the standalone bcinfo tool. It prints a variety of
// information about a supplied bitcode input file.

std::string inFile;
std::string outFile;
std::string infoFile;

extern int opterr;
extern int optind;

bool translateFlag = false;
bool infoFlag = false;
bool verbose = true;

static int parseOption(int argc, char** argv) {
  int c;
  while ((c = getopt(argc, argv, "itv")) != -1) {
    opterr = 0;

    switch(c) {
      case '?':
        // ignore any error
        break;

      case 't':
        translateFlag = true;
        break;

      case 'i':
        // Turn off verbose so that we only generate the .info file.
        infoFlag = true;
        verbose = false;
        break;

      case 'v':
        verbose = true;
        break;

      default:
        // Critical error occurs
        return 0;
        break;
    }
  }

  if(optind >= argc) {
    fprintf(stderr, "input file required\n");
    return 0;
  }

  inFile = argv[optind];

  int l = inFile.length();
  if (l > 3 && inFile[l-3] == '.' && inFile[l-2] == 'b' && inFile[l-1] == 'c') {
    outFile = std::string(inFile.begin(), inFile.end() - 3) + ".ll";
    infoFile = std::string(inFile.begin(), inFile.end() - 3) + ".bcinfo";
  } else {
    outFile = inFile + ".ll";
    infoFile = inFile + ".bcinfo";
  }
  return 1;
}


static int dumpInfo(bcinfo::MetadataExtractor *ME) {
  if (!ME) {
    return 1;
  }

  FILE *info = fopen(infoFile.c_str(), "w");
  if (!info) {
    fprintf(stderr, "Could not open info file %s\n", infoFile.c_str());
    return 2;
  }

  fprintf(info, "exportVarCount: %u\n", ME->getExportVarCount());
  const char **varNameList = ME->getExportVarNameList();
  for (size_t i = 0; i < ME->getExportVarCount(); i++) {
    fprintf(info, "%s\n", varNameList[i]);
  }

  fprintf(info, "exportFuncCount: %u\n", ME->getExportFuncCount());
  const char **funcNameList = ME->getExportFuncNameList();
  for (size_t i = 0; i < ME->getExportFuncCount(); i++) {
    fprintf(info, "%s\n", funcNameList[i]);
  }

  fprintf(info, "exportForEachCount: %u\n",
          ME->getExportForEachSignatureCount());
  const char **nameList = ME->getExportForEachNameList();
  const uint32_t *sigList = ME->getExportForEachSignatureList();
  for (size_t i = 0; i < ME->getExportForEachSignatureCount(); i++) {
    fprintf(info, "%u - %s\n", sigList[i], nameList[i]);
  }

  fprintf(info, "objectSlotCount: %u\n", ME->getObjectSlotCount());
  const uint32_t *slotList = ME->getObjectSlotList();
  for (size_t i = 0; i < ME->getObjectSlotCount(); i++) {
    fprintf(info, "%u\n", slotList[i]);
  }

  fclose(info);
  return 0;
}


static void dumpMetadata(bcinfo::MetadataExtractor *ME) {
  if (!ME) {
    return;
  }

  printf("RSFloatPrecision: ");
  switch (ME->getRSFloatPrecision()) {
  case bcinfo::RS_FP_Full:
    printf("Full\n\n");
    break;
  case bcinfo::RS_FP_Relaxed:
    printf("Relaxed\n\n");
    break;
  case bcinfo::RS_FP_Imprecise:
    printf("Imprecise\n\n");
    break;
  default:
    printf("UNKNOWN\n\n");
    break;
  }

  printf("exportVarCount: %u\n", ME->getExportVarCount());
  const char **varNameList = ME->getExportVarNameList();
  for (size_t i = 0; i < ME->getExportVarCount(); i++) {
    printf("var[%u]: %s\n", i, varNameList[i]);
  }
  printf("\n");

  printf("exportFuncCount: %u\n", ME->getExportFuncCount());
  const char **funcNameList = ME->getExportFuncNameList();
  for (size_t i = 0; i < ME->getExportFuncCount(); i++) {
    printf("func[%u]: %s\n", i, funcNameList[i]);
  }
  printf("\n");

  printf("exportForEachSignatureCount: %u\n",
         ME->getExportForEachSignatureCount());
  const char **nameList = ME->getExportForEachNameList();
  const uint32_t *sigList = ME->getExportForEachSignatureList();
  for (size_t i = 0; i < ME->getExportForEachSignatureCount(); i++) {
    printf("exportForEachSignatureList[%u]: %s - %u\n", i, nameList[i],
           sigList[i]);
  }
  printf("\n");

  printf("pragmaCount: %u\n", ME->getPragmaCount());
  const char **keyList = ME->getPragmaKeyList();
  const char **valueList = ME->getPragmaValueList();
  for (size_t i = 0; i < ME->getPragmaCount(); i++) {
    printf("pragma[%u]: %s - %s\n", i, keyList[i], valueList[i]);
  }
  printf("\n");

  printf("objectSlotCount: %u\n", ME->getObjectSlotCount());
  const uint32_t *slotList = ME->getObjectSlotList();
  for (size_t i = 0; i < ME->getObjectSlotCount(); i++) {
    printf("objectSlotList[%u]: %u\n", i, slotList[i]);
  }
  printf("\n");

  return;
}


static size_t readBitcode(const char **bitcode) {
  if (!inFile.length()) {
    fprintf(stderr, "input file required\n");
    return 0;
  }

  struct stat statInFile;
  if (stat(inFile.c_str(), &statInFile) < 0) {
    fprintf(stderr, "Unable to stat input file: %s\n", strerror(errno));
    return 0;
  }

  if (!S_ISREG(statInFile.st_mode)) {
    fprintf(stderr, "Input file should be a regular file.\n");
    return 0;
  }

  FILE *in = fopen(inFile.c_str(), "r");
  if (!in) {
    fprintf(stderr, "Could not open input file %s\n", inFile.c_str());
    return 0;
  }

  size_t bitcodeSize = statInFile.st_size;

  *bitcode = (const char*) calloc(1, bitcodeSize + 1);
  size_t nread = fread((void*) *bitcode, 1, bitcodeSize, in);

  if (nread != bitcodeSize)
      fprintf(stderr, "Could not read all of file %s\n", inFile.c_str());

  fclose(in);
  return nread;
}


static void releaseBitcode(const char **bitcode) {
  if (bitcode && *bitcode) {
    free((void*) *bitcode);
    *bitcode = NULL;
  }
  return;
}


int main(int argc, char** argv) {
  if(!parseOption(argc, argv)) {
    fprintf(stderr, "failed to parse option\n");
    return 1;
  }

  const char *bitcode = NULL;
  size_t bitcodeSize = readBitcode(&bitcode);

  unsigned int version = 0;

  bcinfo::BitcodeWrapper bcWrapper((const char *)bitcode, bitcodeSize);
  if (bcWrapper.getBCFileType() == bcinfo::BC_WRAPPER) {
    version = bcWrapper.getTargetAPI();
    if (verbose) {
      printf("Found bitcodeWrapper\n");
    }
  } else if (translateFlag) {
    version = 12;
  }

  if (verbose) {
    printf("targetAPI: %u\n", version);
    printf("compilerVersion: %u\n", bcWrapper.getCompilerVersion());
    printf("optimizationLevel: %u\n\n", bcWrapper.getOptimizationLevel());
  }

  llvm::OwningPtr<bcinfo::BitcodeTranslator> BT;
  BT.reset(new bcinfo::BitcodeTranslator(bitcode, bitcodeSize, version));
  if (!BT->translate()) {
    fprintf(stderr, "failed to translate bitcode\n");
    return 3;
  }

  llvm::OwningPtr<bcinfo::MetadataExtractor> ME;
  ME.reset(new bcinfo::MetadataExtractor(BT->getTranslatedBitcode(),
                                         BT->getTranslatedBitcodeSize()));
  if (!ME->extract()) {
    fprintf(stderr, "failed to get metadata\n");
    return 4;
  }

  if (verbose) {
    dumpMetadata(ME.get());

    const char *translatedBitcode = BT->getTranslatedBitcode();
    size_t translatedBitcodeSize = BT->getTranslatedBitcodeSize();

    llvm::LLVMContext &ctx = llvm::getGlobalContext();
    llvm::llvm_shutdown_obj called_on_exit;

    llvm::OwningPtr<llvm::MemoryBuffer> mem;

    mem.reset(llvm::MemoryBuffer::getMemBuffer(
        llvm::StringRef(translatedBitcode, translatedBitcodeSize),
        inFile.c_str(), false));

    llvm::OwningPtr<llvm::Module> module;
    std::string errmsg;
    module.reset(llvm::ParseBitcodeFile(mem.get(), ctx, &errmsg));
    if (module.get() != 0 && module->MaterializeAllPermanently(&errmsg)) {
      module.reset();
    }

    if (module.get() == 0) {
      if (errmsg.size()) {
        fprintf(stderr, "error: %s\n", errmsg.c_str());
      } else {
        fprintf(stderr, "error: failed to parse bitcode file\n");
      }
      return 5;
    }

    llvm::OwningPtr<llvm::tool_output_file> tof(
        new llvm::tool_output_file(outFile.c_str(), errmsg,
                                   llvm::sys::fs::F_Binary));
    llvm::OwningPtr<llvm::AssemblyAnnotationWriter> ann;
    module->print(tof->os(), ann.get());

    tof->keep();
  }

  if (infoFlag) {
    if (dumpInfo(ME.get()) != 0) {
      fprintf(stderr, "Error dumping info file\n");
      return 6;
    }
  }

  releaseBitcode(&bitcode);

  return 0;
}
