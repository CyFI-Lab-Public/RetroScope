//===- Linker.h -----------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//

#ifndef ALONE_LINKER_H
#define ALONE_LINKER_H

#include <string>

namespace mcld {

class Module;
class IRBuilder;
class LinkerConfig;
class Linker;
class Input;
class MemoryArea;

namespace sys { namespace fs {

class Path;

} } // end namespace sys::fs

} // end namespace mcld

namespace alone {

class LinkerConfig;

class Linker {
public:
  enum ErrorCode {
    kSuccess,
    kDoubleConfig,
    kDelegateLDInfo,
    kFindNameSpec,
    kOpenObjectFile,
    kOpenMemory,
    kNotConfig,
    kNotSetUpOutput,
    kOpenOutput,
    kReadSections,
    kReadSymbols,
    kAddAdditionalSymbols,
    kMaxErrorCode
  };

  static const char *GetErrorString(enum ErrorCode pErrCode);

private:
  const mcld::LinkerConfig *mLDConfig;
  mcld::Module *mModule;
  mcld::Linker *mLinker;
  mcld::IRBuilder *mBuilder;
  std::string mSOName;
  std::string mOutputPath;
  int mOutputHandler;

public:
  Linker();

  Linker(const LinkerConfig& pConfig);

  ~Linker();

  enum ErrorCode config(const LinkerConfig& pConfig);

  enum ErrorCode addNameSpec(const std::string &pNameSpec);

  enum ErrorCode addObject(const std::string &pObjectPath);

  enum ErrorCode addObject(void* pMemory, size_t pSize);

  enum ErrorCode addCode(void* pMemory, size_t pSize);

  enum ErrorCode setOutput(const std::string &pPath);

  enum ErrorCode setOutput(int pFileHandler);

  enum ErrorCode link();

private:
  enum ErrorCode extractFiles(const LinkerConfig& pConfig);
};

} // end namespace alone

#endif // ALONE_LINKER_H
