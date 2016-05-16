//===- ELFBinaryReader.h --------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_ELF_Binary_READER_H
#define MCLD_ELF_Binary_READER_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <mcld/LD/BinaryReader.h>

namespace mcld {

class Module;
class Input;
class IRBuilder;
class GNULDBackend;
class LinkerConfig;

/** \lclass ELFBinaryReader
 *  \brief ELFBinaryReader reads target-independent parts of Binary file
 */
class ELFBinaryReader : public BinaryReader
{
public:
  ELFBinaryReader(GNULDBackend& pBackend,
                  IRBuilder& pBuilder,
                  const LinkerConfig& pConfig);

  ~ELFBinaryReader();

  virtual bool readBinary(Input& pInput);

private:
  GNULDBackend& m_Backend;
  IRBuilder& m_Builder;
  const LinkerConfig& m_Config;
};

} // namespace of mcld

#endif

