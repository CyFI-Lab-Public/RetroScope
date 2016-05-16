//===- Relocator.cpp ------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Fragment/Fragment.h>
#include <mcld/LD/LDSymbol.h>
#include <mcld/LD/Relocator.h>
#include <mcld/LD/ResolveInfo.h>
#include <mcld/LD/SectionData.h>
#include <mcld/Module.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// Relocator
//===----------------------------------------------------------------------===//
Relocator::~Relocator()
{
}

void Relocator::partialScanRelocation(Relocation& pReloc,
                                      Module& pModule,
                                      const LDSection& pSection)
{
  // if we meet a section symbol
  if (pReloc.symInfo()->type() == ResolveInfo::Section) {
    LDSymbol* input_sym = pReloc.symInfo()->outSymbol();

    // 1. update the relocation target offset
    assert(input_sym->hasFragRef());
    uint64_t offset = input_sym->fragRef()->getOutputOffset();
    pReloc.target() += offset;

    // 2. get output section symbol
    // get the output LDSection which the symbol defined in
    const LDSection& out_sect =
                        input_sym->fragRef()->frag()->getParent()->getSection();
    ResolveInfo* sym_info =
                     pModule.getSectionSymbolSet().get(out_sect)->resolveInfo();
    // set relocation target symbol to the output section symbol's resolveInfo
    pReloc.setSymInfo(sym_info);
  }
}

