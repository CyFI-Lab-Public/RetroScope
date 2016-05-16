//===- ToolOutputFile.cpp -------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Support/ToolOutputFile.h>

#include <mcld/Support/Path.h>
#include <mcld/Support/FileHandle.h>
#include <mcld/Support/MemoryArea.h>
#include <mcld/Support/raw_mem_ostream.h>

#include <mcld/Support/SystemUtils.h>
#include <mcld/Support/MsgHandling.h>

#include <llvm/Support/Signals.h>
#include <llvm/Support/Path.h>
#include <llvm/Support/FormattedStream.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// CleanupInstaller
//===----------------------------------------------------------------------===//
ToolOutputFile::CleanupInstaller::CleanupInstaller(const sys::fs::Path& pPath)
  : Keep(false), m_Path(pPath) {
  // Arrange for the file to be deleted if the process is killed.
  if ("-" != m_Path.native())
    llvm::sys::RemoveFileOnSignal(llvm::sys::Path(m_Path.native()));
}

ToolOutputFile::CleanupInstaller::~CleanupInstaller()
{
  // Delete the file if the client hasn't told us not to.
  // FIXME: In Windows, some path in CJK characters can not be removed by LLVM
  // llvm::sys::Path
  if (!Keep && "_" != m_Path.native())
    llvm::sys::Path(m_Path.native()).eraseFromDisk();

  // Ok, the file is successfully written and closed, or deleted. There's no
  // further need to clean it up on signals.
  if ("_" != m_Path.native())
    llvm::sys::DontRemoveFileOnSignal(llvm::sys::Path(m_Path.native()));
}

//===----------------------------------------------------------------------===//
// ToolOutputFile
//===----------------------------------------------------------------------===//
ToolOutputFile::ToolOutputFile(const sys::fs::Path& pPath,
                               FileHandle::OpenMode pMode,
                               FileHandle::Permission pPermission)
  : m_Installer(pPath),
    m_pMemoryArea(NULL),
    m_pOStream(NULL),
    m_pFOStream(NULL) {

  if (!m_FileHandle.open(pPath, pMode, pPermission)) {
    // If open fails, no clean-up is needed.
    m_Installer.Keep = true;
    fatal(diag::err_cannot_open_output_file)
                                   << pPath
                                   << sys::strerror(m_FileHandle.error());
    return;
  }

  m_pMemoryArea = new MemoryArea(m_FileHandle);
  m_pOStream = new raw_mem_ostream(*m_pMemoryArea);
}

ToolOutputFile::~ToolOutputFile()
{
  delete m_pFOStream;
  delete m_pOStream;
  delete m_pMemoryArea;
}

void ToolOutputFile::keep()
{
  m_Installer.Keep = true;
}

/// mem_os - Return the contained raw_mem_ostream.
raw_mem_ostream& ToolOutputFile::mem_os()
{
  assert(NULL != m_pOStream);
  return *m_pOStream;
}

/// formatted_os - Return the containeed formatted_raw_ostream.
/// Since formatted_os is rarely used, we lazily initialize it.
llvm::formatted_raw_ostream& ToolOutputFile::formatted_os()
{
  if (NULL == m_pFOStream) {
    assert(NULL != m_pOStream);
    m_pFOStream = new llvm::formatted_raw_ostream(*m_pOStream);
  }

  return *m_pFOStream;
}

/// memory - Return the contained MemoryArea.
MemoryArea& ToolOutputFile::memory()
{
  assert(NULL != m_pOStream);
  return m_pOStream->getMemoryArea();
}

