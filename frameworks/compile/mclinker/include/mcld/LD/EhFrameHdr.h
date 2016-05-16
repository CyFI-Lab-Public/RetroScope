//===- EhFrameHdr.h -------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_EHFRAMEHDR_H
#define MCLD_EHFRAMEHDR_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <mcld/ADT/SizeTraits.h>
#include <cassert>

#include <mcld/LD/EhFrame.h>
namespace mcld {

class LDSection;
class MemoryArea;
class MemoryRegion;

/** \class EhFrameHdr
 *  \brief EhFrameHdr represents .eh_frame_hdr section.
 *
 *  @ref lsb core generic 4.1
 *  .eh_frame_hdr section format
 *  uint8_t : version
 *  uint8_t : eh_frame_ptr_enc
 *  uint8_t : fde_count_enc
 *  uint8_t : table_enc
 *  uint32_t : eh_frame_ptr
 *  uint32_t : fde_count
 *  __________________________ when fde_count > 0
 *  <uint32_t, uint32_t>+ : binary search table
 */
class EhFrameHdr
{
public:
  EhFrameHdr(LDSection& pEhFrameHdr, const LDSection& pEhFrame);

  ~EhFrameHdr();

  /// sizeOutput - base on the fde count to size output
  void sizeOutput();

  /// emitOutput - write out eh_frame_hdr
  template<size_t size>
  void emitOutput(MemoryArea& pOutput)
  { assert(false && "Call invalid EhFrameHdr::emitOutput"); }

private:
  /// computePCBegin - return the address of FDE's pc
  /// @ref binutils gold: ehframe.cc:222
  uint32_t computePCBegin(const EhFrame::FDE& pFDE, const MemoryRegion& pEhFrameRegion);

private:
  /// .eh_frame_hdr section
  LDSection& m_EhFrameHdr;

  /// eh_frame
  const LDSection& m_EhFrame;
};

//===----------------------------------------------------------------------===//
// Template Specification Functions
//===----------------------------------------------------------------------===//
/// emitOutput - write out eh_frame_hdr
template<>
void EhFrameHdr::emitOutput<32>(MemoryArea& pOutput);

} // namespace of mcld

#endif

