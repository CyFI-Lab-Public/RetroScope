//===- HexagonPLT.h -------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_TARGET_HEXAGON_PLT_H
#define MCLD_TARGET_HEXAGON_PLT_H

#include "HexagonGOT.h"
#include "HexagonGOTPLT.h"
#include <mcld/Target/GOT.h>
#include <mcld/Target/PLT.h>

namespace {

const uint8_t hexagon_plt0[] = {
 0x00, 0x40, 0x00, 0x00,  // { immext (#0)
 0x1c, 0xc0, 0x49, 0x6a,  //   r28 = add (pc, ##GOT0@PCREL) } # address of GOT0
 0x0e, 0x42, 0x9c, 0xe2,  // { r14 -= add (r28, #16)  # offset of GOTn from GOTa
 0x4f, 0x40, 0x9c, 0x91,  //   r15 = memw (r28 + #8)  # object ID at GOT2
 0x3c, 0xc0, 0x9c, 0x91,  //   r28 = memw (r28 + #4) }# dynamic link at GOT1
 0x0e, 0x42, 0x0e, 0x8c,  // { r14 = asr (r14, #2)    # index of PLTn
 0x00, 0xc0, 0x9c, 0x52,  //   jumpr r28 }            # call dynamic linker
 0x00, 0x00, 0x00, 0x00,
 0x00, 0x00, 0x00, 0x00,
 0x00, 0x00, 0x00, 0x00,
 0x00, 0x00, 0x00, 0x00,
 0x00, 0x00, 0x00, 0x00,
};

const uint8_t hexagon_plt1[] = {
  0x00, 0x40, 0x00, 0x00, // { immext (#0)
  0x0e, 0xc0, 0x49, 0x6a, //   r14 = add (pc, ##GOTn@PCREL) } # address of GOTn
  0x1c, 0xc0, 0x8e, 0x91, // r28 = memw (r14)                 # contents of GOTn
  0x00, 0xc0, 0x9c, 0x52, // jumpr r28                        # call it
};

} // anonymous namespace

namespace mcld {

class GOTEntry;
class LinkerConfig;
class MemoryRegion;
class HexagonPLT1;

//===----------------------------------------------------------------------===//
// HexagonPLT Entry
//===----------------------------------------------------------------------===//
class HexagonPLT0 : public PLT::Entry<sizeof(hexagon_plt0)>
{
public:
  HexagonPLT0(SectionData& pParent);
};

//===----------------------------------------------------------------------===//
// HexagonPLT
//===----------------------------------------------------------------------===//
/** \class HexagonPLT
 *  \brief Hexagon Procedure Linkage Table
 */
class HexagonPLT : public PLT
{
public:
  HexagonPLT(LDSection& pSection,
             HexagonGOTPLT& pGOTPLT,
             const LinkerConfig& pConfig);
  ~HexagonPLT();

  // finalizeSectionSize - set LDSection size
  void finalizeSectionSize();

  // hasPLT1 - return if this PLT has any PLT1 entry
  bool hasPLT1() const;

  void reserveEntry(size_t pNum = 1) ;

  HexagonPLT1* consume();

  void applyPLT0();

  void applyPLT1();

  uint64_t emit(MemoryRegion& pRegion);

  PLTEntryBase* getPLT0() const;

private:
  HexagonGOTPLT& m_GOTPLT;

  // the last consumed entry.
  SectionData::iterator m_Last;

  const uint8_t *m_PLT0;
  unsigned int m_PLT0Size;

  const LinkerConfig& m_Config;
};

class HexagonPLT1 : public PLT::Entry<sizeof(hexagon_plt1)>
{
public:
  HexagonPLT1(SectionData& pParent);
};

} // namespace of mcld

#endif

