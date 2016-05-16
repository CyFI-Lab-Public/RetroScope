//===- FragmentLinker.cpp -------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
//
// This file implements the FragmentLinker class
//
//===----------------------------------------------------------------------===//
#include <mcld/Fragment/FragmentLinker.h>

#include <llvm/Support/Host.h>
#include <llvm/Support/raw_ostream.h>
#include <llvm/Support/Casting.h>

#include <mcld/LinkerConfig.h>
#include <mcld/Module.h>
#include <mcld/LD/LDSection.h>
#include <mcld/MC/MCLDInput.h>
#include <mcld/LD/LDSection.h>
#include <mcld/LD/BranchIslandFactory.h>
#include <mcld/LD/Resolver.h>
#include <mcld/LD/LDContext.h>
#include <mcld/LD/RelocationFactory.h>
#include <mcld/LD/RelocData.h>
#include <mcld/LD/Relocator.h>
#include <mcld/Support/MemoryRegion.h>
#include <mcld/Support/MemoryArea.h>
#include <mcld/Support/FileHandle.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/Target/TargetLDBackend.h>
#include <mcld/Fragment/Relocation.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// FragmentLinker
//===----------------------------------------------------------------------===//
/// Constructor
FragmentLinker::FragmentLinker(const LinkerConfig& pConfig,
                               Module& pModule,
                               TargetLDBackend& pBackend)

  : m_Config(pConfig),
    m_Module(pModule),
    m_Backend(pBackend) {
}

/// Destructor
FragmentLinker::~FragmentLinker()
{
}

bool FragmentLinker::finalizeSymbols()
{
  Module::sym_iterator symbol, symEnd = m_Module.sym_end();
  for (symbol = m_Module.sym_begin(); symbol != symEnd; ++symbol) {

    if ((*symbol)->resolveInfo()->isAbsolute() ||
        (*symbol)->resolveInfo()->type() == ResolveInfo::File) {
      // absolute symbols or symbols with function type should have
      // zero value
      (*symbol)->setValue(0x0);
      continue;
    }

    if ((*symbol)->resolveInfo()->type() == ResolveInfo::ThreadLocal) {
      m_Backend.finalizeTLSSymbol(**symbol);
      continue;
    }

    if ((*symbol)->hasFragRef()) {
      // set the virtual address of the symbol. If the output file is
      // relocatable object file, the section's virtual address becomes zero.
      // And the symbol's value become section relative offset.
      uint64_t value = (*symbol)->fragRef()->getOutputOffset();
      assert(NULL != (*symbol)->fragRef()->frag());
      uint64_t addr = (*symbol)->fragRef()->frag()->getParent()->getSection().addr();
      (*symbol)->setValue(value + addr);
      continue;
    }
  }

  return true;
}

//===----------------------------------------------------------------------===//
// Relocation Operations
//===----------------------------------------------------------------------===//
bool FragmentLinker::applyRelocations()
{
  // when producing relocatables, no need to apply relocation
  if (LinkerConfig::Object == m_Config.codeGenType())
    return true;

  // apply all relocations of all inputs
  Module::obj_iterator input, inEnd = m_Module.obj_end();
  for (input = m_Module.obj_begin(); input != inEnd; ++input) {
    m_Backend.getRelocator()->initializeApply(**input);
    LDContext::sect_iterator rs, rsEnd = (*input)->context()->relocSectEnd();
    for (rs = (*input)->context()->relocSectBegin(); rs != rsEnd; ++rs) {
      // bypass the reloc section if
      // 1. its section kind is changed to Ignore. (The target section is a
      // discarded group section.)
      // 2. it has no reloc data. (All symbols in the input relocs are in the
      // discarded group sections)
      if (LDFileFormat::Ignore == (*rs)->kind() || !(*rs)->hasRelocData())
        continue;
      RelocData::iterator reloc, rEnd = (*rs)->getRelocData()->end();
      for (reloc = (*rs)->getRelocData()->begin(); reloc != rEnd; ++reloc) {
        Relocation* relocation = llvm::cast<Relocation>(reloc);
        relocation->apply(*m_Backend.getRelocator());
      } // for all relocations
    } // for all relocation section
    m_Backend.getRelocator()->finalizeApply(**input);
  } // for all inputs

  // apply relocations created by relaxation
  BranchIslandFactory* br_factory = m_Backend.getBRIslandFactory();
  BranchIslandFactory::iterator facIter, facEnd = br_factory->end();
  for (facIter = br_factory->begin(); facIter != facEnd; ++facIter) {
    BranchIsland& island = *facIter;
    BranchIsland::reloc_iterator iter, iterEnd = island.reloc_end();
    for (iter = island.reloc_begin(); iter != iterEnd; ++iter)
      (*iter)->apply(*m_Backend.getRelocator());
  }
  return true;
}


void FragmentLinker::syncRelocationResult(MemoryArea& pOutput)
{
  if (LinkerConfig::Object != m_Config.codeGenType())
    normalSyncRelocationResult(pOutput);
  else
    partialSyncRelocationResult(pOutput);
  return;
}

void FragmentLinker::normalSyncRelocationResult(MemoryArea& pOutput)
{
  MemoryRegion* region = pOutput.request(0, pOutput.handler()->size());

  uint8_t* data = region->getBuffer();

  // sync all relocations of all inputs
  Module::obj_iterator input, inEnd = m_Module.obj_end();
  for (input = m_Module.obj_begin(); input != inEnd; ++input) {
    LDContext::sect_iterator rs, rsEnd = (*input)->context()->relocSectEnd();
    for (rs = (*input)->context()->relocSectBegin(); rs != rsEnd; ++rs) {
      // bypass the reloc section if
      // 1. its section kind is changed to Ignore. (The target section is a
      // discarded group section.)
      // 2. it has no reloc data. (All symbols in the input relocs are in the
      // discarded group sections)
      if (LDFileFormat::Ignore == (*rs)->kind() || !(*rs)->hasRelocData())
        continue;
      RelocData::iterator reloc, rEnd = (*rs)->getRelocData()->end();
      for (reloc = (*rs)->getRelocData()->begin(); reloc != rEnd; ++reloc) {
        Relocation* relocation = llvm::cast<Relocation>(reloc);

        // bypass the relocation with NONE type. This is to avoid overwrite the
        // target result by NONE type relocation if there is a place which has
        // two relocations to apply to, and one of it is NONE type. The result
        // we want is the value of the other relocation result. For example,
        // in .exidx, there are usually an R_ARM_NONE and R_ARM_PREL31 apply to
        // the same place
        if (0x0 == relocation->type())
          continue;
        writeRelocationResult(*relocation, data);
      } // for all relocations
    } // for all relocation section
  } // for all inputs

  // sync relocations created by relaxation
  BranchIslandFactory* br_factory = m_Backend.getBRIslandFactory();
  BranchIslandFactory::iterator facIter, facEnd = br_factory->end();
  for (facIter = br_factory->begin(); facIter != facEnd; ++facIter) {
    BranchIsland& island = *facIter;
    BranchIsland::reloc_iterator iter, iterEnd = island.reloc_end();
    for (iter = island.reloc_begin(); iter != iterEnd; ++iter) {
      Relocation* reloc = *iter;
      writeRelocationResult(*reloc, data);
    }
  }

  pOutput.clear();
}

void FragmentLinker::partialSyncRelocationResult(MemoryArea& pOutput)
{
  MemoryRegion* region = pOutput.request(0, pOutput.handler()->size());

  uint8_t* data = region->getBuffer();

  // traverse outputs' LDSection to get RelocData
  Module::iterator sectIter, sectEnd = m_Module.end();
  for (sectIter = m_Module.begin(); sectIter != sectEnd; ++sectIter) {
    if (LDFileFormat::Relocation != (*sectIter)->kind())
      continue;

    RelocData* reloc_data = (*sectIter)->getRelocData();
    RelocData::iterator relocIter, relocEnd = reloc_data->end();
    for (relocIter = reloc_data->begin(); relocIter != relocEnd; ++relocIter) {
      Relocation* reloc = llvm::cast<Relocation>(relocIter);

      // bypass the relocation with NONE type. This is to avoid overwrite the
      // target result by NONE type relocation if there is a place which has
      // two relocations to apply to, and one of it is NONE type. The result
      // we want is the value of the other relocation result. For example,
      // in .exidx, there are usually an R_ARM_NONE and R_ARM_PREL31 apply to
      // the same place
      if (0x0 == reloc->type())
        continue;
      writeRelocationResult(*reloc, data);
    }
  }

  pOutput.clear();
}

void FragmentLinker::writeRelocationResult(Relocation& pReloc, uint8_t* pOutput)
{
  // get output file offset
  size_t out_offset =
                 pReloc.targetRef().frag()->getParent()->getSection().offset() +
                 pReloc.targetRef().getOutputOffset();

  uint8_t* target_addr = pOutput + out_offset;
  // byte swapping if target and host has different endian, and then write back
  if(llvm::sys::IsLittleEndianHost != m_Config.targets().isLittleEndian()) {
     uint64_t tmp_data = 0;

     switch(pReloc.size(*m_Backend.getRelocator())) {
       case 8u:
         std::memcpy(target_addr, &pReloc.target(), 1);
         break;

       case 16u:
         tmp_data = mcld::bswap16(pReloc.target());
         std::memcpy(target_addr, &tmp_data, 2);
         break;

       case 32u:
         tmp_data = mcld::bswap32(pReloc.target());
         std::memcpy(target_addr, &tmp_data, 4);
         break;

       case 64u:
         tmp_data = mcld::bswap64(pReloc.target());
         std::memcpy(target_addr, &tmp_data, 8);
         break;

       default:
         break;
    }
  }
  else
    std::memcpy(target_addr, &pReloc.target(),
                                      pReloc.size(*m_Backend.getRelocator())/8);
}

