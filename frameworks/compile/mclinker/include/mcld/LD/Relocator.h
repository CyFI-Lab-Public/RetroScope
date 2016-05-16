//===- Relocator.h --------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_RELOCATOR_H
#define MCLD_RELOCATOR_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <mcld/Fragment/Relocation.h>

namespace mcld
{

class FragmentLinker;
class TargetLDBackend;
class IRBuilder;
class Module;
class Input;

/** \class Relocator
 *  \brief Relocator provides the interface of performing relocations
 */
class Relocator
{
public:
  typedef Relocation::Type    Type;
  typedef Relocation::Address Address;
  typedef Relocation::DWord   DWord;
  typedef Relocation::SWord   SWord;
  typedef Relocation::Size    Size;

public:
  enum Result {
    OK,
    BadReloc,
    Overflow,
    Unsupport,
    Unknown
  };

public:
  Relocator(const LinkerConfig& pConfig)
    : m_Config(pConfig)
  {}

  virtual ~Relocator() = 0;

  /// apply - general apply function
  virtual Result applyRelocation(Relocation& pRelocation) = 0;

  /// scanRelocation - When read in relocations, backend can do any modification
  /// to relocation and generate empty entries, such as GOT, dynamic relocation
  /// entries and other target dependent entries. These entries are generated
  /// for layout to adjust the ouput offset.
  /// @param pReloc - a read in relocation entry
  /// @param pInputSym - the input LDSymbol of relocation target symbol
  /// @param pSection - the section of relocation applying target
  virtual void scanRelocation(Relocation& pReloc,
                              IRBuilder& pBuilder,
                              Module& pModule,
                              LDSection& pSection) = 0;

  /// initializeScan - do initialization before scan relocations in pInput
  /// @return - return true for initialization success
  virtual bool initializeScan(Input& pInput)
  { return true; }

  /// finalizeScan - do finalization after scan relocations in pInput
  /// @return - return true for finalization success
  virtual bool finalizeScan(Input& pInput)
  { return true; }

  /// initializeApply - do initialization before apply relocations in pInput
  /// @return - return true for initialization success
  virtual bool initializeApply(Input& pInput)
  { return true; }

  /// finalizeApply - do finalization after apply relocations in pInput
  /// @return - return true for finalization success
  virtual bool finalizeApply(Input& pInput)
  { return true; }

  /// partialScanRelocation - When doing partial linking, backend can do any
  /// modification to relocation to fix the relocation offset after section
  /// merge
  /// @param pReloc - a read in relocation entry
  /// @param pInputSym - the input LDSymbol of relocation target symbol
  /// @param pSection - the section of relocation applying target
  virtual void partialScanRelocation(Relocation& pReloc,
                                     Module& pModule,
                                     const LDSection& pSection);

  // ------ observers -----//
  virtual TargetLDBackend& getTarget() = 0;

  virtual const TargetLDBackend& getTarget() const = 0;

  /// getName - get the name of a relocation
  virtual const char* getName(Type pType) const = 0;

  /// getSize - get the size of a relocation in bit
  virtual Size getSize(Type pType) const = 0;

protected:
  const LinkerConfig& config() const { return m_Config; }

private:
  const LinkerConfig& m_Config;
};

} // namespace of mcld

#endif

