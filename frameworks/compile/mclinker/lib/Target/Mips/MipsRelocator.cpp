//===- MipsRelocator.cpp  -----------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//

#include <llvm/ADT/Twine.h>
#include <llvm/Support/ELF.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/Target/OutputRelocSection.h>
#include <mcld/LinkerConfig.h>
#include <mcld/IRBuilder.h>

#include "MipsRelocator.h"
#include "MipsRelocationFunctions.h"

using namespace mcld;

//===----------------------------------------------------------------------===//
// Relocation Functions and Tables
//===----------------------------------------------------------------------===//
DECL_MIPS_APPLY_RELOC_FUNCS

/// the prototype of applying function
typedef Relocator::Result (*ApplyFunctionType)(Relocation&, MipsRelocator&);

// the table entry of applying functions
struct ApplyFunctionTriple
{
  ApplyFunctionType func;
  unsigned int type;
  const char* name;
  unsigned int size;
};

// declare the table of applying functions
static const ApplyFunctionTriple ApplyFunctions[] = {
  DECL_MIPS_APPLY_RELOC_FUNC_PTRS
};

//===----------------------------------------------------------------------===//
// MipsRelocator
//===----------------------------------------------------------------------===//
MipsRelocator::MipsRelocator(MipsGNULDBackend& pParent,
                             const LinkerConfig& pConfig)
  : Relocator(pConfig),
    m_Target(pParent),
    m_pApplyingInput(NULL),
    m_AHL(0)
{
}

Relocator::Result
MipsRelocator::applyRelocation(Relocation& pRelocation)
{
  Relocation::Type type = pRelocation.type();

  if (type >= sizeof(ApplyFunctions) / sizeof(ApplyFunctions[0])) {
    return Unknown;
  }

  // apply the relocation
  return ApplyFunctions[type].func(pRelocation, *this);
}

const char* MipsRelocator::getName(Relocation::Type pType) const
{
  return ApplyFunctions[pType].name;
}

Relocator::Size MipsRelocator::getSize(Relocation::Type pType) const
{
  return ApplyFunctions[pType].size;
}

void MipsRelocator::scanRelocation(Relocation& pReloc,
                                   IRBuilder& pBuilder,
                                   Module& pModule,
                                   LDSection& pSection)
{
  // rsym - The relocation target symbol
  ResolveInfo* rsym = pReloc.symInfo();
  assert(NULL != rsym && "ResolveInfo of relocation not set while scanRelocation");

  // Skip relocation against _gp_disp
  if (NULL != getTarget().getGpDispSymbol()) {
    if (pReloc.symInfo() == getTarget().getGpDispSymbol()->resolveInfo())
      return;
  }

  pReloc.updateAddend();

  assert(NULL != pSection.getLink());
  if (0 == (pSection.getLink()->flag() & llvm::ELF::SHF_ALLOC))
    return;

  // We test isLocal or if pInputSym is not a dynamic symbol
  // We assume -Bsymbolic to bind all symbols internaly via !rsym->isDyn()
  // Don't put undef symbols into local entries.
  if ((rsym->isLocal() || !getTarget().isDynamicSymbol(*rsym) ||
      !rsym->isDyn()) && !rsym->isUndef())
    scanLocalReloc(pReloc, pBuilder, pSection);
  else
    scanGlobalReloc(pReloc, pBuilder, pSection);

  // check if we shoule issue undefined reference for the relocation target
  // symbol
  if (rsym->isUndef() && !rsym->isDyn() && !rsym->isWeak() && !rsym->isNull())
    fatal(diag::undefined_reference) << rsym->name();
}

bool MipsRelocator::initializeScan(Input& pInput)
{
  getTarget().getGOT().initializeScan(pInput);
  return true;
}

bool MipsRelocator::finalizeScan(Input& pInput)
{
  getTarget().getGOT().finalizeScan(pInput);
  return true;
}

bool MipsRelocator::initializeApply(Input& pInput)
{
  m_pApplyingInput = &pInput;
  return true;
}

bool MipsRelocator::finalizeApply(Input& pInput)
{
  m_pApplyingInput = NULL;
  return true;
}

void MipsRelocator::scanLocalReloc(Relocation& pReloc,
                                   IRBuilder& pBuilder,
                                   const LDSection& pSection)
{
  ResolveInfo* rsym = pReloc.symInfo();

  switch (pReloc.type()){
    case llvm::ELF::R_MIPS_NONE:
    case llvm::ELF::R_MIPS_16:
      break;
    case llvm::ELF::R_MIPS_32:
      if (LinkerConfig::DynObj == config().codeGenType()) {
        // TODO: (simon) The gold linker does not create an entry in .rel.dyn
        // section if the symbol section flags contains SHF_EXECINSTR.
        // 1. Find the reason of this condition.
        // 2. Check this condition here.
        getTarget().getRelDyn().reserveEntry();
        rsym->setReserved(rsym->reserved() | ReserveRel);
        getTarget().checkAndSetHasTextRel(*pSection.getLink());

        // Remeber this rsym is a local GOT entry (as if it needs an entry).
        // Actually we don't allocate an GOT entry.
        getTarget().getGOT().setLocal(rsym);
      }
      break;
    case llvm::ELF::R_MIPS_REL32:
    case llvm::ELF::R_MIPS_26:
    case llvm::ELF::R_MIPS_HI16:
    case llvm::ELF::R_MIPS_LO16:
    case llvm::ELF::R_MIPS_PC16:
    case llvm::ELF::R_MIPS_SHIFT5:
    case llvm::ELF::R_MIPS_SHIFT6:
    case llvm::ELF::R_MIPS_64:
    case llvm::ELF::R_MIPS_GOT_PAGE:
    case llvm::ELF::R_MIPS_GOT_OFST:
    case llvm::ELF::R_MIPS_SUB:
    case llvm::ELF::R_MIPS_INSERT_A:
    case llvm::ELF::R_MIPS_INSERT_B:
    case llvm::ELF::R_MIPS_DELETE:
    case llvm::ELF::R_MIPS_HIGHER:
    case llvm::ELF::R_MIPS_HIGHEST:
    case llvm::ELF::R_MIPS_SCN_DISP:
    case llvm::ELF::R_MIPS_REL16:
    case llvm::ELF::R_MIPS_ADD_IMMEDIATE:
    case llvm::ELF::R_MIPS_PJUMP:
    case llvm::ELF::R_MIPS_RELGOT:
    case llvm::ELF::R_MIPS_JALR:
    case llvm::ELF::R_MIPS_GLOB_DAT:
    case llvm::ELF::R_MIPS_COPY:
    case llvm::ELF::R_MIPS_JUMP_SLOT:
      break;
    case llvm::ELF::R_MIPS_GOT16:
    case llvm::ELF::R_MIPS_CALL16:
    case llvm::ELF::R_MIPS_GOT_HI16:
    case llvm::ELF::R_MIPS_CALL_HI16:
    case llvm::ELF::R_MIPS_GOT_LO16:
    case llvm::ELF::R_MIPS_CALL_LO16:
      if (getTarget().getGOT().reserveLocalEntry(*rsym)) {
        if (getTarget().getGOT().hasMultipleGOT())
          getTarget().checkAndSetHasTextRel(*pSection.getLink());
        // Remeber this rsym is a local GOT entry
        getTarget().getGOT().setLocal(rsym);
      }
      break;
    case llvm::ELF::R_MIPS_GPREL32:
    case llvm::ELF::R_MIPS_GPREL16:
    case llvm::ELF::R_MIPS_LITERAL:
    case llvm::ELF::R_MIPS_GOT_DISP:
      break;
    case llvm::ELF::R_MIPS_TLS_DTPMOD32:
    case llvm::ELF::R_MIPS_TLS_DTPREL32:
    case llvm::ELF::R_MIPS_TLS_DTPMOD64:
    case llvm::ELF::R_MIPS_TLS_DTPREL64:
    case llvm::ELF::R_MIPS_TLS_GD:
    case llvm::ELF::R_MIPS_TLS_LDM:
    case llvm::ELF::R_MIPS_TLS_DTPREL_HI16:
    case llvm::ELF::R_MIPS_TLS_DTPREL_LO16:
    case llvm::ELF::R_MIPS_TLS_GOTTPREL:
    case llvm::ELF::R_MIPS_TLS_TPREL32:
    case llvm::ELF::R_MIPS_TLS_TPREL64:
    case llvm::ELF::R_MIPS_TLS_TPREL_HI16:
    case llvm::ELF::R_MIPS_TLS_TPREL_LO16:
      break;
    default:
      fatal(diag::unknown_relocation) << (int)pReloc.type()
                                      << pReloc.symInfo()->name();
  }
}

void MipsRelocator::scanGlobalReloc(Relocation& pReloc,
                                    IRBuilder& pBuilder,
                                    const LDSection& pSection)
{
  ResolveInfo* rsym = pReloc.symInfo();

  switch (pReloc.type()){
    case llvm::ELF::R_MIPS_NONE:
    case llvm::ELF::R_MIPS_INSERT_A:
    case llvm::ELF::R_MIPS_INSERT_B:
    case llvm::ELF::R_MIPS_DELETE:
    case llvm::ELF::R_MIPS_TLS_DTPMOD64:
    case llvm::ELF::R_MIPS_TLS_DTPREL64:
    case llvm::ELF::R_MIPS_REL16:
    case llvm::ELF::R_MIPS_ADD_IMMEDIATE:
    case llvm::ELF::R_MIPS_PJUMP:
    case llvm::ELF::R_MIPS_RELGOT:
    case llvm::ELF::R_MIPS_TLS_TPREL64:
      break;
    case llvm::ELF::R_MIPS_32:
    case llvm::ELF::R_MIPS_64:
    case llvm::ELF::R_MIPS_HI16:
    case llvm::ELF::R_MIPS_LO16:
      if (getTarget().symbolNeedsDynRel(*rsym, false, true)) {
        getTarget().getRelDyn().reserveEntry();
        rsym->setReserved(rsym->reserved() | ReserveRel);
        getTarget().checkAndSetHasTextRel(*pSection.getLink());

        // Remeber this rsym is a global GOT entry (as if it needs an entry).
        // Actually we don't allocate an GOT entry.
        getTarget().getGOT().setGlobal(rsym);
      }
      break;
    case llvm::ELF::R_MIPS_GOT16:
    case llvm::ELF::R_MIPS_CALL16:
    case llvm::ELF::R_MIPS_GOT_DISP:
    case llvm::ELF::R_MIPS_GOT_HI16:
    case llvm::ELF::R_MIPS_CALL_HI16:
    case llvm::ELF::R_MIPS_GOT_LO16:
    case llvm::ELF::R_MIPS_CALL_LO16:
    case llvm::ELF::R_MIPS_GOT_PAGE:
    case llvm::ELF::R_MIPS_GOT_OFST:
      if (getTarget().getGOT().reserveGlobalEntry(*rsym)) {
        if (getTarget().getGOT().hasMultipleGOT())
          getTarget().checkAndSetHasTextRel(*pSection.getLink());
        // Remeber this rsym is a global GOT entry
        getTarget().getGOT().setGlobal(rsym);
      }
      break;
    case llvm::ELF::R_MIPS_LITERAL:
    case llvm::ELF::R_MIPS_GPREL32:
      fatal(diag::invalid_global_relocation) << (int)pReloc.type()
                                             << pReloc.symInfo()->name();
      break;
    case llvm::ELF::R_MIPS_GPREL16:
      break;
    case llvm::ELF::R_MIPS_26:
    case llvm::ELF::R_MIPS_PC16:
      break;
    case llvm::ELF::R_MIPS_16:
    case llvm::ELF::R_MIPS_SHIFT5:
    case llvm::ELF::R_MIPS_SHIFT6:
    case llvm::ELF::R_MIPS_SUB:
    case llvm::ELF::R_MIPS_HIGHER:
    case llvm::ELF::R_MIPS_HIGHEST:
    case llvm::ELF::R_MIPS_SCN_DISP:
      break;
    case llvm::ELF::R_MIPS_TLS_DTPREL32:
    case llvm::ELF::R_MIPS_TLS_GD:
    case llvm::ELF::R_MIPS_TLS_LDM:
    case llvm::ELF::R_MIPS_TLS_DTPREL_HI16:
    case llvm::ELF::R_MIPS_TLS_DTPREL_LO16:
    case llvm::ELF::R_MIPS_TLS_GOTTPREL:
    case llvm::ELF::R_MIPS_TLS_TPREL32:
    case llvm::ELF::R_MIPS_TLS_TPREL_HI16:
    case llvm::ELF::R_MIPS_TLS_TPREL_LO16:
      break;
    case llvm::ELF::R_MIPS_REL32:
      break;
    case llvm::ELF::R_MIPS_JALR:
      break;
    case llvm::ELF::R_MIPS_COPY:
    case llvm::ELF::R_MIPS_GLOB_DAT:
    case llvm::ELF::R_MIPS_JUMP_SLOT:
      fatal(diag::dynamic_relocation) << (int)pReloc.type();
      break;
    default:
      fatal(diag::unknown_relocation) << (int)pReloc.type()
                                      << pReloc.symInfo()->name();
  }
}

//===----------------------------------------------------------------------===//
// Relocation helper function
//===----------------------------------------------------------------------===//
static const char * const GP_DISP_NAME = "_gp_disp";

// Find next R_MIPS_LO16 relocation paired to pReloc.
static
Relocation* helper_FindLo16Reloc(Relocation& pReloc)
{
  Relocation* reloc = static_cast<Relocation*>(pReloc.getNextNode());
  while (NULL != reloc)
  {
    if (llvm::ELF::R_MIPS_LO16 == reloc->type() &&
        reloc->symInfo() == pReloc.symInfo())
      return reloc;

    reloc = static_cast<Relocation*>(reloc->getNextNode());
  }
  return NULL;
}

// Check the symbol is _gp_disp.
static
bool helper_isGpDisp(const Relocation& pReloc)
{
  const ResolveInfo* rsym = pReloc.symInfo();
  return 0 == strcmp(GP_DISP_NAME, rsym->name());
}

static
Relocator::Address helper_GetGP(MipsRelocator& pParent)
{
  return pParent.getTarget().getGOT().getGPAddr(pParent.getApplyingInput());
}

static
void helper_SetupRelDynForGOTEntry(MipsGOTEntry& got_entry,
                                   Relocation& pReloc,
                                   ResolveInfo* rsym,
                                   MipsRelocator& pParent)
{
  MipsGNULDBackend& ld_backend = pParent.getTarget();

  Relocation& rel_entry = *ld_backend.getRelDyn().consumeEntry();
  rel_entry.setType(llvm::ELF::R_MIPS_REL32);
  rel_entry.targetRef() = *FragmentRef::Create(got_entry, 0);
  rel_entry.setSymInfo(rsym);
}

static
MipsGOTEntry& helper_GetGOTEntry(Relocation& pReloc, MipsRelocator& pParent)
{
  // rsym - The relocation target symbol
  ResolveInfo* rsym = pReloc.symInfo();
  MipsGNULDBackend& ld_backend = pParent.getTarget();
  MipsGOT& got = ld_backend.getGOT();
  MipsGOTEntry* got_entry;

  if (got.isLocal(rsym) && ResolveInfo::Section == rsym->type()) {
    // Local section symbols consume local got entries.
    got_entry = got.consumeLocal();
    if (got.isPrimaryGOTConsumed())
      helper_SetupRelDynForGOTEntry(*got_entry, pReloc, NULL, pParent);
    return *got_entry;
  }

  got_entry = got.lookupEntry(rsym);
  if (NULL != got_entry) {
    // found a mapping, then return the mapped entry immediately
    return *got_entry;
  }

  // not found
  if (got.isLocal(rsym))
    got_entry = got.consumeLocal();
  else
    got_entry = got.consumeGlobal();

  got.recordEntry(rsym, got_entry);

  // If we first get this GOT entry, we should initialize it.
  if (!got.isLocal(rsym) || ResolveInfo::Section != rsym->type()) {
    if (!got.isPrimaryGOTConsumed())
      got_entry->setValue(pReloc.symValue());
  }

  if (got.isPrimaryGOTConsumed())
    helper_SetupRelDynForGOTEntry(*got_entry, pReloc,
                                  got.isLocal(rsym) ? NULL : rsym, pParent);

  return *got_entry;
}

static
Relocator::Address helper_GetGOTOffset(Relocation& pReloc,
                                       MipsRelocator& pParent)
{
  MipsGNULDBackend& ld_backend = pParent.getTarget();
  MipsGOT& got = ld_backend.getGOT();
  MipsGOTEntry& got_entry = helper_GetGOTEntry(pReloc, pParent);
  return got.getGPRelOffset(pParent.getApplyingInput(), got_entry);
}

static
int32_t helper_CalcAHL(const Relocation& pHiReloc, const Relocation& pLoReloc)
{
  assert((pHiReloc.type() == llvm::ELF::R_MIPS_HI16 ||
          pHiReloc.type() == llvm::ELF::R_MIPS_GOT16) &&
         pLoReloc.type() == llvm::ELF::R_MIPS_LO16 &&
         "Incorrect type of relocation for AHL calculation");

  // Note the addend is section symbol offset here
  assert (pHiReloc.addend() == pLoReloc.addend());

  int32_t AHI = pHiReloc.target();
  int32_t ALO = pLoReloc.target();
  int32_t AHL = ((AHI & 0xFFFF) << 16) + (int16_t)(ALO & 0xFFFF) +
                 pLoReloc.addend();
  return AHL;
}

static
void helper_DynRel(Relocation& pReloc, MipsRelocator& pParent)
{
  ResolveInfo* rsym = pReloc.symInfo();
  MipsGNULDBackend& ld_backend = pParent.getTarget();
  MipsGOT& got = ld_backend.getGOT();

  Relocation& rel_entry = *ld_backend.getRelDyn().consumeEntry();

  rel_entry.setType(llvm::ELF::R_MIPS_REL32);
  rel_entry.targetRef() = pReloc.targetRef();

  Relocator::DWord A = pReloc.target() + pReloc.addend();
  Relocator::DWord S = pReloc.symValue();

  if (got.isLocal(rsym)) {
    rel_entry.setSymInfo(NULL);
    pReloc.target() = A + S;
  }
  else {
    rel_entry.setSymInfo(rsym);
    // Don't add symbol value that will be resolved by the dynamic linker
    pReloc.target() = A;
  }
}

//=========================================//
// Relocation functions implementation     //
//=========================================//

// R_MIPS_NONE and those unsupported/deprecated relocation type
static
MipsRelocator::Result none(Relocation& pReloc, MipsRelocator& pParent)
{
  return MipsRelocator::OK;
}

// R_MIPS_32: S + A
static
MipsRelocator::Result abs32(Relocation& pReloc, MipsRelocator& pParent)
{
  ResolveInfo* rsym = pReloc.symInfo();

  Relocator::DWord A = pReloc.target() + pReloc.addend();
  Relocator::DWord S = pReloc.symValue();

  LDSection& target_sect = pReloc.targetRef().frag()->getParent()->getSection();
  // If the flag of target section is not ALLOC, we will not scan this relocation
  // but perform static relocation. (e.g., applying .debug section)
  if (0x0 == (llvm::ELF::SHF_ALLOC & target_sect.flag())) {
    pReloc.target() = S + A;
    return MipsRelocator::OK;
  }

  if (rsym->reserved() & MipsRelocator::ReserveRel) {
    helper_DynRel(pReloc, pParent);

    return MipsRelocator::OK;
  }

  pReloc.target() = (S + A);

  return MipsRelocator::OK;
}

// R_MIPS_HI16:
//   local/external: ((AHL + S) - (short)(AHL + S)) >> 16
//   _gp_disp      : ((AHL + GP - P) - (short)(AHL + GP - P)) >> 16
static
MipsRelocator::Result hi16(Relocation& pReloc, MipsRelocator& pParent)
{
  Relocation* lo_reloc = helper_FindLo16Reloc(pReloc);
  assert(NULL != lo_reloc && "There is no paired R_MIPS_LO16 for R_MIPS_HI16");

  int32_t AHL = helper_CalcAHL(pReloc, *lo_reloc);
  int32_t res = 0;

  pParent.setAHL(AHL);

  if (helper_isGpDisp(pReloc)) {
    int32_t P = pReloc.place();
    int32_t GP = helper_GetGP(pParent);
    res = ((AHL + GP - P) - (int16_t)(AHL + GP - P)) >> 16;
  }
  else {
    int32_t S = pReloc.symValue();
    res = ((AHL + S) - (int16_t)(AHL + S)) >> 16;
  }

  pReloc.target() &= 0xFFFF0000;
  pReloc.target() |= (res & 0xFFFF);

  return MipsRelocator::OK;
}

// R_MIPS_LO16:
//   local/external: AHL + S
//   _gp_disp      : AHL + GP - P + 4
static
MipsRelocator::Result lo16(Relocation& pReloc, MipsRelocator& pParent)
{
  int32_t res = 0;

  if (helper_isGpDisp(pReloc)) {
    int32_t P = pReloc.place();
    int32_t GP = helper_GetGP(pParent);
    int32_t AHL = pParent.getAHL();
    res = AHL + GP - P + 4;
  }
  else {
    int32_t S = pReloc.symValue();
    // The previous AHL may be for other hi/lo pairs.
    // We need to calcuate the lo part now.  It is easy.
    // Remember to add the section offset to ALO.
    int32_t ALO = (pReloc.target() & 0xFFFF) + pReloc.addend();
    res = ALO + S;
  }

  pReloc.target() &= 0xFFFF0000;
  pReloc.target() |= (res & 0xFFFF);

  return MipsRelocator::OK;
}

// R_MIPS_GOT16:
//   local   : G (calculate AHL and put high 16 bit to GOT)
//   external: G
static
MipsRelocator::Result got16(Relocation& pReloc, MipsRelocator& pParent)
{
  MipsGNULDBackend& ld_backend = pParent.getTarget();
  MipsGOT& got = ld_backend.getGOT();
  ResolveInfo* rsym = pReloc.symInfo();
  Relocator::Address G = 0;

  if (rsym->isLocal()) {
    Relocation* lo_reloc = helper_FindLo16Reloc(pReloc);
    assert(NULL != lo_reloc && "There is no paired R_MIPS_LO16 for R_MIPS_GOT16");

    int32_t AHL = helper_CalcAHL(pReloc, *lo_reloc);
    int32_t S = pReloc.symValue();

    pParent.setAHL(AHL);

    int32_t res = (AHL + S + 0x8000) & 0xFFFF0000;
    MipsGOTEntry& got_entry = helper_GetGOTEntry(pReloc, pParent);

    got_entry.setValue(res);
    G = got.getGPRelOffset(pParent.getApplyingInput(), got_entry);
  }
  else {
    G = helper_GetGOTOffset(pReloc, pParent);
  }

  pReloc.target() &= 0xFFFF0000;
  pReloc.target() |= (G & 0xFFFF);

  return MipsRelocator::OK;
}

// R_MIPS_GOTHI16:
//   external: (G - (short)G) >> 16 + A
static
MipsRelocator::Result gothi16(Relocation& pReloc, MipsRelocator& pParent)
{
  int32_t res = 0;

  Relocator::Address G = helper_GetGOTOffset(pReloc, pParent);
  int32_t A = pReloc.target() + pReloc.addend();

  res = (G - (int16_t)G) >> (16 + A);

  pReloc.target() &= 0xFFFF0000;
  pReloc.target() |= (res & 0xFFFF);

  return MipsRelocator::OK;
}

// R_MIPS_GOTLO16:
//   external: G & 0xffff
static
MipsRelocator::Result gotlo16(Relocation& pReloc, MipsRelocator& pParent)
{
  Relocator::Address G = helper_GetGOTOffset(pReloc, pParent);

  pReloc.target() &= 0xFFFF0000;
  pReloc.target() |= (G & 0xFFFF);

  return MipsRelocator::OK;
}

// R_MIPS_CALL16: G
static
MipsRelocator::Result call16(Relocation& pReloc, MipsRelocator& pParent)
{
  Relocator::Address G = helper_GetGOTOffset(pReloc, pParent);

  pReloc.target() &= 0xFFFF0000;
  pReloc.target() |= (G & 0xFFFF);

  return MipsRelocator::OK;
}

// R_MIPS_GPREL32: A + S + GP0 - GP
static
MipsRelocator::Result gprel32(Relocation& pReloc, MipsRelocator& pParent)
{
  // Remember to add the section offset to A.
  int32_t A = pReloc.target() + pReloc.addend();
  int32_t S = pReloc.symValue();
  int32_t GP = helper_GetGP(pParent);

  // llvm does not emits SHT_MIPS_REGINFO section.
  // Assume that GP0 is zero.
  pReloc.target() = (A + S - GP) & 0xFFFFFFFF;

  return MipsRelocator::OK;
}

