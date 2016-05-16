//===-  X86Relocator.h --------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef X86_RELOCATION_FACTORY_H
#define X86_RELOCATION_FACTORY_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <mcld/LD/Relocator.h>
#include <mcld/Target/GOT.h>
#include <mcld/Target/PLT.h>
#include <mcld/Target/SymbolEntryMap.h>
#include "X86LDBackend.h"

namespace mcld {

class ResolveInfo;
class LinkerConfig;

/** \class X86Relocator
 *  \brief X86Relocator creates and destroys the X86 relocations.
 *
 */
class X86Relocator : public Relocator
{
public:
  typedef SymbolEntryMap<PLTEntryBase> SymPLTMap;

  /** \enum ReservedEntryType
   *  \brief The reserved entry type of reserved space in ResolveInfo.
   *
   *  This is used for sacnRelocation to record what kinds of entries are
   *  reserved for this resolved symbol
   *
   *  In X86, there are three kinds of entries, GOT, PLT, and dynamic reloction.
   *  GOT may needs a corresponding relocation to relocate itself, so we
   *  separate GOT to two situations: GOT and GOTRel. Besides, for the same
   *  symbol, there might be two kinds of entries reserved for different location.
   *  For example, reference to the same symbol, one may use GOT and the other may
   *  use dynamic relocation.
   *
   *  bit:  3       2      1     0
   *   | PLT | GOTRel | GOT | Rel |
   *
   *  value    Name         - Description
   *
   *  0000     None         - no reserved entry
   *  0001     ReserveRel   - reserve an dynamic relocation entry
   *  0010     ReserveGOT   - reserve an GOT entry
   *  0011     GOTandRel    - For different relocation, we've reserved GOT and
   *                          Rel for different location.
   *  0100     GOTRel       - reserve an GOT entry and the corresponding Dyncamic
   *                          relocation entry which relocate this GOT entry
   *  0101     GOTRelandRel - For different relocation, we've reserved GOTRel
   *                          and relocation entry for different location.
   *  1000     ReservePLT   - reserve an PLT entry and the corresponding GOT,
   *                          Dynamic relocation entries
   *  1001     PLTandRel    - For different relocation, we've reserved PLT and
   *                          Rel for different location.
   */
  enum ReservedEntryType {
    None         = 0,
    ReserveRel   = 1,
    ReserveGOT   = 2,
    GOTandRel    = 3,
    GOTRel       = 4,
    GOTRelandRel = 5,
    ReservePLT   = 8,
    PLTandRel    = 9
  };

public:
  X86Relocator(const LinkerConfig& pConfig);
  ~X86Relocator();

  virtual Result applyRelocation(Relocation& pRelocation) = 0;

  virtual const char* getName(Relocation::Type pType) const = 0;

  const SymPLTMap& getSymPLTMap() const { return m_SymPLTMap; }
  SymPLTMap&       getSymPLTMap()       { return m_SymPLTMap; }

  /// scanRelocation - determine the empty entries are needed or not and create
  /// the empty entries if needed.
  /// For X86, following entries are check to create:
  /// - GOT entry (for .got and .got.plt sections)
  /// - PLT entry (for .plt section)
  /// - dynamin relocation entries (for .rel.plt and .rel.dyn sections)
  void scanRelocation(Relocation& pReloc,
                      IRBuilder& pBuilder,
                      Module& pModule,
                      LDSection& pSection);

protected:
  /// addCopyReloc - add a copy relocation into .rel.dyn for pSym
  /// @param pSym - A resolved copy symbol that defined in BSS section
  void addCopyReloc(ResolveInfo& pSym, X86GNULDBackend& pTarget);

  /// defineSymbolforCopyReloc - allocate a space in BSS section and
  /// and force define the copy of pSym to BSS section
  /// @return the output LDSymbol of the copy symbol
  LDSymbol& defineSymbolforCopyReloc(IRBuilder& pLinker,
                                     const ResolveInfo& pSym,
                                     X86GNULDBackend& pTarget);

private:
  virtual void scanLocalReloc(Relocation& pReloc,
                              IRBuilder& pBuilder,
                              Module& pModule,
                              LDSection& pSection) = 0;

  virtual void scanGlobalReloc(Relocation& pReloc,
                               IRBuilder& pBuilder,
                               Module& pModule,
                               LDSection& pSection) = 0;

private:
  SymPLTMap m_SymPLTMap;
};

/** \class X86_32Relocator
 *  \brief X86_32Relocator creates and destroys the X86-32 relocations.
 *
 */
class X86_32Relocator : public X86Relocator
{
public:
  typedef SymbolEntryMap<X86_32GOTEntry> SymGOTMap;
  typedef SymbolEntryMap<X86_32GOTEntry> SymGOTPLTMap;

  enum {
    R_386_TLS_OPT = 44 // mcld internal relocation type
  };

public:
  X86_32Relocator(X86_32GNULDBackend& pParent, const LinkerConfig& pConfig);

  Result applyRelocation(Relocation& pRelocation);

  X86_32GNULDBackend& getTarget()
  { return m_Target; }

  const X86_32GNULDBackend& getTarget() const
  { return m_Target; }

  const char* getName(Relocation::Type pType) const;

  Size getSize(Relocation::Type pType) const;

  const SymGOTMap& getSymGOTMap() const { return m_SymGOTMap; }
  SymGOTMap&       getSymGOTMap()       { return m_SymGOTMap; }

  const SymGOTPLTMap& getSymGOTPLTMap() const { return m_SymGOTPLTMap; }
  SymGOTPLTMap&       getSymGOTPLTMap()       { return m_SymGOTPLTMap; }

  X86_32GOTEntry& getTLSModuleID();

private:
  void scanLocalReloc(Relocation& pReloc,
                      IRBuilder& pBuilder,
                      Module& pModule,
                      LDSection& pSection);

  void scanGlobalReloc(Relocation& pReloc,
                       IRBuilder& pBuilder,
                       Module& pModule,
                       LDSection& pSection);

  /// -----  tls optimization  ----- ///
  /// convert R_386_TLS_IE to R_386_TLS_LE
  void convertTLSIEtoLE(Relocation& pReloc, LDSection& pSection);

private:
  X86_32GNULDBackend& m_Target;
  SymGOTMap m_SymGOTMap;
  SymGOTPLTMap m_SymGOTPLTMap;
};

/** \class X86_64Relocator
 *  \brief X86_64Relocator creates and destroys the X86-64 relocations.
 *
 */
class X86_64Relocator : public X86Relocator
{
public:
  typedef SymbolEntryMap<X86_64GOTEntry> SymGOTMap;
  typedef SymbolEntryMap<X86_64GOTEntry> SymGOTPLTMap;

public:
  X86_64Relocator(X86_64GNULDBackend& pParent, const LinkerConfig& pConfig);

  Result applyRelocation(Relocation& pRelocation);

  X86_64GNULDBackend& getTarget()
  { return m_Target; }

  const X86_64GNULDBackend& getTarget() const
  { return m_Target; }

  const char* getName(Relocation::Type pType) const;

  Size getSize(Relocation::Type pType) const;

  const SymGOTMap& getSymGOTMap() const { return m_SymGOTMap; }
  SymGOTMap&       getSymGOTMap()       { return m_SymGOTMap; }

  const SymGOTPLTMap& getSymGOTPLTMap() const { return m_SymGOTPLTMap; }
  SymGOTPLTMap&       getSymGOTPLTMap()       { return m_SymGOTPLTMap; }

private:
  void scanLocalReloc(Relocation& pReloc,
                      IRBuilder& pBuilder,
                      Module& pModule,
                      LDSection& pSection);

  void scanGlobalReloc(Relocation& pReloc,
                       IRBuilder& pBuilder,
                       Module& pModule,
                       LDSection& pSection);

private:
  X86_64GNULDBackend& m_Target;
  SymGOTMap m_SymGOTMap;
  SymGOTPLTMap m_SymGOTPLTMap;
};

} // namespace of mcld

#endif

