//===- FileAction.h -------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_MC_FILE_ACTION_H
#define MCLD_MC_FILE_ACTION_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <mcld/MC/InputAction.h>
#include <mcld/Support/FileHandle.h>

namespace mcld {

class ContextFactory;
class MemoryAreaFactory;

/** \class ContextAction
 *  \brief ContextAction is a command object to create input's LDContext.
 */
class ContextAction : public InputAction
{
public:
  explicit ContextAction(unsigned int pPosition);

  bool activate(InputBuilder& pBuilder) const;
};

/** \class MemoryAreaAction
 *  \brief MemoryAreaAction is a command object to create input's MemoryArea.
 */
class MemoryAreaAction : public InputAction
{
public:
  MemoryAreaAction(unsigned int pPosition,
                   FileHandle::OpenMode pMode,
                   FileHandle::Permission pPerm = FileHandle::System);

  bool activate(InputBuilder& pBuilder) const;

private:
  FileHandle::OpenMode m_Mode;
  FileHandle::Permission m_Permission;
};

} // end of namespace mcld

#endif

