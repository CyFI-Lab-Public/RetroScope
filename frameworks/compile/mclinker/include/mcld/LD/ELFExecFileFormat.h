//===- ELFExecFileFormat.h ------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_ELF_EXEC_FILE_FORMAT_H
#define MCLD_ELF_EXEC_FILE_FORMAT_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <mcld/LD/ELFFileFormat.h>

namespace mcld {

class ObjectBuilder;

/** \class ELFExecFileFormat
 *  \brief ELFExecFileFormat describes the format for ELF dynamic objects.
 */
class ELFExecFileFormat : public ELFFileFormat
{
  /// initObjectFormat - initialize sections that are dependent on executable
  /// objects.
  void initObjectFormat(ObjectBuilder& pBuilder, unsigned int pBitClass);
};

} // namespace of mcld

#endif

