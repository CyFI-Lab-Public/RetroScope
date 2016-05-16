//===- ARMGOT.h -----------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_ARM_GOT_H
#define MCLD_ARM_GOT_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <llvm/ADT/DenseMap.h>

#include <mcld/Target/GOT.h>

namespace mcld {

class LDSection;
class MemoryRegion;

/** \class ARMGOTEntry
 *  \brief GOT Entry with size of 4 bytes
 */
class ARMGOTEntry : public GOT::Entry<4>
{
public:
  ARMGOTEntry(uint64_t pContent, SectionData* pParent)
   : GOT::Entry<4>(pContent, pParent)
  {}
};

/** \class ARMGOT
 *  \brief ARM Global Offset Table.
 *
 *  ARM GOT integrates traditional .got.plt and .got sections into one.
 *  Traditional .got.plt is placed in the front part of GOT (PLTGOT), and
 *  traditional .got is placed in the rear part of GOT (GOT).
 *
 *  ARM .got
 *            +--------------+
 *            |    GOT0      |
 *            +--------------+
 *            |    GOTPLT    |
 *            +--------------+
 *            |    GOT       |
 *            +--------------+
 *
 */
class ARMGOT : public GOT
{
public:
  ARMGOT(LDSection &pSection);

  ~ARMGOT();

  void reserve(size_t pNum = 1);

  void reserveGOTPLT();

  void reserveGOT();

  ARMGOTEntry* consume();

  ARMGOTEntry* consumeGOT();

  ARMGOTEntry* consumeGOTPLT();

  uint64_t emit(MemoryRegion& pRegion);

  void applyGOT0(uint64_t pAddress);

  void applyGOTPLT(uint64_t pPLTBase);

  bool hasGOT1() const;

private:
  struct Part {
  public:
    Part() : front(NULL), last_used(NULL) { }

  public:
    ARMGOTEntry* front;
    ARMGOTEntry* last_used;
  };

private:
  Part m_GOTPLT;
  Part m_GOT;

  ARMGOTEntry* m_pLast; ///< the last consumed entry
};

} // namespace of mcld

#endif

