//===- TargetOptions.h ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_TARGET_OPTIONS_H
#define MCLD_TARGET_OPTIONS_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <llvm/ADT/Triple.h>

#include <string>

namespace mcld {

/** \class TargetOptions
 *  \brief TargetOptions collects the options that dependent on a target
 *  backend.
 */
class TargetOptions
{
public:
  enum Endian {
    Little,
    Big,
    Unknown
  };

public:
  TargetOptions();

  TargetOptions(const std::string& pTriple);

  ~TargetOptions();

  const llvm::Triple& triple() const { return m_Triple; }

  void setTriple(const std::string& pTriple);

  void setTriple(const llvm::Triple& pTriple);

  const std::string& getTargetCPU() const { return m_TargetCPU; }

  void setTargetCPU(const std::string& pCPU);

  const std::string& getTargetFeatureString() const { return m_TargetFS; }

  void setTargetFeatureString(const std::string& pFS);

  Endian endian() const { return m_Endian; }

  void setEndian(Endian pEndian) { m_Endian = pEndian; }

  bool isLittleEndian() const { return (Little == m_Endian); }
  bool isBigEndian   () const { return (Big    == m_Endian); }

  unsigned int bitclass() const { return m_BitClass; }

  void setBitClass(unsigned int pBitClass) { m_BitClass = pBitClass; }

  bool is32Bits() const { return (32 == m_BitClass); }
  bool is64Bits() const { return (64 == m_BitClass); }

private:
  llvm::Triple m_Triple;
  std::string m_TargetCPU;
  std::string m_TargetFS;
  Endian m_Endian;
  unsigned int m_BitClass;

};

} // namespace of mcld

#endif

