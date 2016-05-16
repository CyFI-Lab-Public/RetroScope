//===- MCLDDirectory.h ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_MCLDDIRECTORY_H
#define MCLD_MCLDDIRECTORY_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include "mcld/Support/Directory.h"
#include "mcld/Support/FileSystem.h"
#include <llvm/ADT/StringRef.h>
#include <string>

namespace mcld
{

/** \class MCLDDirectory
 *  \brief MCLDDirectory is an directory entry for library search.
 *
 */
class MCLDDirectory : public sys::fs::Directory
{
public:
  MCLDDirectory();
  MCLDDirectory(const char* pName);
  MCLDDirectory(const std::string& pName);
  MCLDDirectory(llvm::StringRef pName);
  virtual ~MCLDDirectory();

public:
  MCLDDirectory &assign(llvm::StringRef pName);
  bool isInSysroot() const;

  /// setSysroot - if MCLDDirectory is in sysroot, modify the path.
  void setSysroot(const sys::fs::Path& pPath);

  const std::string& name() const
  { return m_Name; }

private:
  std::string m_Name;
  bool m_bInSysroot;
};

} // namespace of mcld

#endif

