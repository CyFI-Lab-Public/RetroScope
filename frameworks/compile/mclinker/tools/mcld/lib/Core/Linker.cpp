//===- Linker.cpp ---------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//

#include "alone/Linker.h"
#include "alone/Support/LinkerConfig.h"
#include "alone/Support/Log.h"

#include <llvm/Support/ELF.h>

#include <mcld/Module.h>
#include <mcld/IRBuilder.h>
#include <mcld/MC/MCLDInput.h>
#include <mcld/Linker.h>
#include <mcld/LD/LDSection.h>
#include <mcld/LD/LDContext.h>
#include <mcld/Support/Path.h>

using namespace alone;

const char* Linker::GetErrorString(enum Linker::ErrorCode pErrCode) {
  static const char* ErrorString[] = {
    /* kSuccess */
    "Successfully compiled.",
    /* kDoubleConfig */
    "Configure Linker twice.",
    /* kDelegateLDInfo */
    "Cannot get linker information",
    /* kFindNameSpec */
    "Cannot find -lnamespec",
    /* kOpenObjectFile */
    "Cannot open object file",
    /* kNotConfig */
    "Linker::config() is not called",
    /* kNotSetUpOutput */
    "Linker::setOutput() is not called before add input files",
    /* kOpenOutput */
    "Cannot open output file",
    /* kReadSections */
    "Cannot read sections",
    /* kReadSymbols */
    "Cannot read symbols",
    /* kAddAdditionalSymbols */
    "Cannot add standard and target symbols",
    /* kMaxErrorCode */
    "(Unknown error code)"
  };

  if (pErrCode > kMaxErrorCode) {
    pErrCode = kMaxErrorCode;
  }

  return ErrorString[ static_cast<size_t>(pErrCode) ];
}

//===----------------------------------------------------------------------===//
// Linker
//===----------------------------------------------------------------------===//
Linker::Linker()
  : mLDConfig(NULL), mModule(NULL), mLinker(NULL), mBuilder(NULL),
    mOutputHandler(-1) {
}

Linker::Linker(const LinkerConfig& pConfig)
  : mLDConfig(NULL), mModule(NULL), mLinker(NULL), mBuilder(NULL),
    mOutputHandler(-1) {

  const std::string &triple = pConfig.getTriple();

  enum ErrorCode err = config(pConfig);
  if (kSuccess != err) {
    ALOGE("%s (%s)", GetErrorString(err), triple.c_str());
    return;
  }

  return;
}

Linker::~Linker() {
  delete mModule;
  delete mLinker;
  delete mBuilder;
}

enum Linker::ErrorCode Linker::extractFiles(const LinkerConfig& pConfig) {
  mLDConfig = pConfig.getLDConfig();
  if (mLDConfig == NULL) {
    return kDelegateLDInfo;
  }
  return kSuccess;
}

enum Linker::ErrorCode Linker::config(const LinkerConfig& pConfig) {
  if (mLDConfig != NULL) {
    return kDoubleConfig;
  }

  extractFiles(pConfig);

  mModule = new mcld::Module(mLDConfig->options().soname(),
                   const_cast<mcld::LinkerScript&>(*pConfig.getLDScript()));

  mBuilder = new mcld::IRBuilder(*mModule, *mLDConfig);

  mLinker = new mcld::Linker();

  mLinker->emulate(const_cast<mcld::LinkerScript&>(*pConfig.getLDScript()),
                   const_cast<mcld::LinkerConfig&>(*mLDConfig));

  return kSuccess;
}

enum Linker::ErrorCode Linker::addNameSpec(const std::string &pNameSpec) {
  mcld::Input* input = mBuilder->ReadInput(pNameSpec);
  if (NULL == input)
    return kFindNameSpec;
  return kSuccess;
}

/// addObject - Add a object file by the filename.
enum Linker::ErrorCode Linker::addObject(const std::string &pObjectPath) {
  mcld::Input* input = mBuilder->ReadInput(pObjectPath, pObjectPath);
  if (NULL == input)
    return kOpenObjectFile;
  return kSuccess;
}

/// addObject - Add a piece of memory. The memory is of ELF format.
enum Linker::ErrorCode Linker::addObject(void* pMemory, size_t pSize) {
  mcld::Input* input = mBuilder->ReadInput("NAN", pMemory, pSize);
  if (NULL == input)
    return kOpenMemory;
  return kSuccess;
}

enum Linker::ErrorCode Linker::addCode(void* pMemory, size_t pSize) {
  mcld::Input* input = mBuilder->CreateInput("NAN", "NAN", mcld::Input::Object);
  mcld::LDSection* sect = mBuilder->CreateELFHeader(*input, ".text",
                                llvm::ELF::SHT_PROGBITS,
                                llvm::ELF::SHF_ALLOC | llvm::ELF::SHF_EXECINSTR,
                                0x1);
  mcld::SectionData* data = mBuilder->CreateSectionData(*sect);
  mcld::Fragment* frag = mBuilder->CreateRegion(pMemory, pSize);
  mBuilder->AppendFragment(*frag, *data);
  return kSuccess;
}

enum Linker::ErrorCode Linker::setOutput(const std::string &pPath) {
  mOutputPath = pPath;
  return kSuccess;
}

enum Linker::ErrorCode Linker::setOutput(int pFileHandler) {
  mOutputHandler = pFileHandler;
  return kSuccess;
}

enum Linker::ErrorCode Linker::link() {
  mLinker->link(*mModule, *mBuilder);
  if (!mOutputPath.empty()) {
    mLinker->emit(mOutputPath);
    return kSuccess;
  }

  if (-1 != mOutputHandler) {
    mLinker->emit(mOutputHandler);
    return kSuccess;
  }
  return kNotSetUpOutput;
}

