//===- EhFrameReader.cpp --------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/LD/EhFrameReader.h>

#include <llvm/ADT/StringRef.h>
#include <llvm/Support/Dwarf.h>

#include <mcld/MC/MCLDInput.h>
#include <mcld/LD/EhFrame.h>
#include <mcld/LD/LDSection.h>
#include <mcld/Support/MemoryArea.h>
#include <mcld/Support/MsgHandling.h>

using namespace mcld;
using namespace llvm::dwarf;

//===----------------------------------------------------------------------===//
// Helper Functions
//===----------------------------------------------------------------------===//
/// skip_LEB128 - skip the first LEB128 encoded value from *pp, update *pp
/// to the next character.
/// @return - false if we ran off the end of the string.
/// @ref - GNU gold 1.11, ehframe.h, Eh_frame::skip_leb128.
static bool
skip_LEB128(EhFrameReader::ConstAddress* pp, EhFrameReader::ConstAddress pend)
{
  for (EhFrameReader::ConstAddress p = *pp; p < pend; ++p) {
    if (0x0 == (*p & 0x80)) {
      *pp = p + 1;
      return true;
    }
  }
  return false;
}

//===----------------------------------------------------------------------===//
// EhFrameReader
//===----------------------------------------------------------------------===//
template<> EhFrameReader::Token
EhFrameReader::scan<true>(ConstAddress pHandler,
                          uint64_t pOffset,
                          const MemoryRegion& pData) const
{
  Token result;
  result.file_off = pOffset;

  const uint32_t* data = (const uint32_t*)pHandler;
  size_t cur_idx = 0;

  // Length Field
  uint32_t length = data[cur_idx++];
  if (0x0 == length) {
    // terminator
    result.kind = Terminator;
    result.data_off = 4;
    result.size = 4;
    return result;
  }

  // Extended Field
  uint64_t extended = 0x0;
  if (0xFFFFFFFF == length) {
    extended = data[cur_idx++];
    extended <<= 32;
    extended |= data[cur_idx++];
    result.size = extended + 12;
    result.data_off = 16;
  }
  else {
    result.size = length + 4;
    result.data_off = 8;
  }

  // ID Field
  uint32_t ID = data[cur_idx++];
  if (0x0 == ID)
    result.kind = CIE;
  else
    result.kind = FDE;

  return result;
}

template<>
bool EhFrameReader::read<32, true>(Input& pInput, EhFrame& pEhFrame)
{
  // Alphabet:
  //   {CIE, FDE, CIEt}
  //
  // Regular Expression:
  //   (CIE FDE*)+ CIEt
  //
  // Autometa:
  //   S = {Q0, Q1, Q2}, Start = Q0, Accept = Q2
  //
  //              FDE
  //             +---+
  //        CIE   \ /   CIEt
  //   Q0 -------> Q1 -------> Q2
  //    |         / \           ^
  //    |        +---+          |
  //    |         CIE           |
  //    +-----------------------+
  //              CIEt
  const State autometa[NumOfStates][NumOfTokenKinds] = {
  //     CIE     FDE    Term  Unknown
    {     Q1, Reject, Accept, Reject }, // Q0
    {     Q1,     Q1, Accept, Reject }, // Q1
  };

  const Action transition[NumOfStates][NumOfTokenKinds] = {
   /*    CIE     FDE     Term Unknown */
    { addCIE, reject, addTerm, reject}, // Q0
    { addCIE, addFDE, addTerm, reject}, // Q1
  };

  LDSection& section = pEhFrame.getSection();
  if (section.size() == 0x0) {
    NullFragment* frag = new NullFragment();
    pEhFrame.addFragment(*frag);
    return true;
  }

  // get file offset and address
  uint64_t file_off = pInput.fileOffset() + section.offset();
  MemoryRegion* sect_reg =
                       pInput.memArea()->request(file_off, section.size());
  ConstAddress handler = (ConstAddress)sect_reg->start();

  State cur_state = Q0;
  while (Reject != cur_state && Accept != cur_state) {

    Token token = scan<true>(handler, file_off, *sect_reg);
    MemoryRegion* entry = pInput.memArea()->request(token.file_off, token.size);

    if (!transition[cur_state][token.kind](pEhFrame, *entry, token)) {
      // fail to scan
      debug(diag::debug_cannot_scan_eh) << pInput.name();
      return false;
    }

    file_off += token.size;
    handler += token.size;

    if (handler == sect_reg->end())
      cur_state = Accept;
    else if (handler > sect_reg->end()) {
      cur_state = Reject;
    }
    else
      cur_state = autometa[cur_state][token.kind];
  } // end of while

  if (Reject == cur_state) {
    // fail to parse
    debug(diag::debug_cannot_parse_eh) << pInput.name();
    return false;
  }
  return true;
}

bool EhFrameReader::addCIE(EhFrame& pEhFrame,
                           MemoryRegion& pRegion,
                           const EhFrameReader::Token& pToken)
{
  // skip Length, Extended Length and CIE ID.
  ConstAddress handler = pRegion.start() + pToken.data_off;
  ConstAddress cie_end = pRegion.end();

  // the version should be 1 or 3
  uint8_t version = *handler++;
  if (1 != version && 3 != version) {
    return false;
  }

  // Set up the Augumentation String
  ConstAddress aug_str_front = handler;
  ConstAddress aug_str_back  = static_cast<ConstAddress>(
                         memchr(aug_str_front, '\0', cie_end - aug_str_front));
  if (NULL == aug_str_back) {
    return false;
  }

  // skip the Augumentation String field
  handler = aug_str_back + 1;

  // skip the Code Alignment Factor
  if (!skip_LEB128(&handler, cie_end)) {
    return false;
  }
  // skip the Data Alignment Factor
  if (!skip_LEB128(&handler, cie_end)) {
    return false;
  }
  // skip the Return Address Register
  if (cie_end - handler < 1) {
    return false;
  }
  ++handler;

  llvm::StringRef augment((const char*)aug_str_front);

  // we discard this CIE if the augumentation string is '\0'
  if (0 == augment.size()) {
    EhFrame::CIE* cie = new EhFrame::CIE(pRegion);
    cie->setFDEEncode(llvm::dwarf::DW_EH_PE_absptr);
    pEhFrame.addCIE(*cie);
    return true;
  }

  // the Augmentation String start with 'eh' is a CIE from gcc before 3.0,
  // in LSB Core Spec 3.0RC1. We do not support it.
  if (augment.size() > 1 && augment[0] == 'e' && augment[1] == 'h') {
    return false;
  }

  // parse the Augmentation String to get the FDE encodeing if 'z' existed
  uint8_t fde_encoding = llvm::dwarf::DW_EH_PE_absptr;
  if ('z' == augment[0]) {

    // skip the Augumentation Data Length
    if (!skip_LEB128(&handler, cie_end)) {
      return false;
    }

    // parse the Augmentation String
    for (size_t i = 1; i < augment.size(); ++i) {
      switch (augment[i]) {
        // LDSA encoding (1 byte)
        case 'L': {
          if (cie_end - handler < 1) {
            return false;
          }
          ++handler;
          break;
        }
        // Two arguments, the first one represents the encoding of the second
        // argument (1 byte). The second one is the address of personality
        // routine.
        case 'P': {
          // the first argument
          if (cie_end - handler < 1) {
            return false;
          }
          uint8_t per_encode = *handler;
          ++handler;
          // get the length of the second argument
          uint32_t per_length = 0;
          if (0x60 == (per_encode & 0x60)) {
            return false;
          }
          switch (per_encode & 7) {
            default:
              return false;
            case llvm::dwarf::DW_EH_PE_udata2:
              per_length = 2;
              break;
            case llvm::dwarf::DW_EH_PE_udata4:
              per_length = 4;
              break;
            case llvm::dwarf::DW_EH_PE_udata8:
              per_length = 8;
              break;
            case llvm::dwarf::DW_EH_PE_absptr:
              per_length = 4; // pPkg.bitclass / 8;
              break;
          }
          // skip the alignment
          if (llvm::dwarf::DW_EH_PE_aligned == (per_encode & 0xf0)) {
            uint32_t per_align = handler - cie_end;
            per_align += per_length - 1;
            per_align &= ~(per_length -1);
            if (static_cast<uint32_t>(cie_end - handler) < per_align) {
              return false;
            }
            handler += per_align;
          }
          // skip the second argument
          if (static_cast<uint32_t>(cie_end - handler) < per_length) {
            return false;
          }
          handler += per_length;
          break;
        } // end of case 'P'

        // FDE encoding (1 byte)
        case 'R': {
          if (cie_end - handler < 1) {
            return false;
          }
          fde_encoding = *handler;
          switch (fde_encoding & 7) {
            case llvm::dwarf::DW_EH_PE_udata2:
            case llvm::dwarf::DW_EH_PE_udata4:
            case llvm::dwarf::DW_EH_PE_udata8:
            case llvm::dwarf::DW_EH_PE_absptr:
              break;
            default:
              return false;
          }
          ++handler;
          break;
        }
        default:
          return false;
      } // end switch
    } // the rest chars.
  } // first char is 'z'

  // create and push back the CIE entry
  EhFrame::CIE* cie = new EhFrame::CIE(pRegion);
  cie->setFDEEncode(fde_encoding);
  pEhFrame.addCIE(*cie);
  return true;
}

bool EhFrameReader::addFDE(EhFrame& pEhFrame,
                           MemoryRegion& pRegion,
                           const EhFrameReader::Token& pToken)
{
  if (pToken.data_off == pRegion.size())
    return false;

  // create and push back the FDE entry
  EhFrame::FDE* fde = new EhFrame::FDE(pRegion,
                                       pEhFrame.cie_back(),
                                       pToken.data_off);
  pEhFrame.addFDE(*fde);
  return true;
}

bool EhFrameReader::addTerm(EhFrame& pEhFrame,
                            MemoryRegion& pRegion,
                            const EhFrameReader::Token& pToken)
{
  RegionFragment* frag = new RegionFragment(pRegion);
  pEhFrame.addFragment(*frag);
  return true;
}

bool EhFrameReader::reject(EhFrame& pEhFrame,
                           MemoryRegion& pRegion,
                           const EhFrameReader::Token& pToken)
{
  return true;
}

