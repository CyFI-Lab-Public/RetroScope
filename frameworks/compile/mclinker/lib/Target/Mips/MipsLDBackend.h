//===- MipsLDBackend.h ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MIPS_LDBACKEND_H
#define MIPS_LDBACKEND_H
#include <mcld/Target/GNULDBackend.h>
#include "MipsELFDynamic.h"
#include "MipsGOT.h"

namespace mcld {

class LinkerConfig;
class OutputRelocSection;
class SectionMap;
class MemoryArea;
class MipsGNUInfo;

//===----------------------------------------------------------------------===//
/// MipsGNULDBackend - linker backend of Mips target of GNU ELF format
///
class MipsGNULDBackend : public GNULDBackend
{
public:
  typedef std::vector<LDSymbol*> SymbolListType;

public:
  MipsGNULDBackend(const LinkerConfig& pConfig, MipsGNUInfo* pInfo);
  ~MipsGNULDBackend();

public:
  /// initTargetSections - initialize target dependent sections in output
  void initTargetSections(Module& pModule, ObjectBuilder& pBuilder);

  /// initTargetSymbols - initialize target dependent symbols in output.
  void initTargetSymbols(IRBuilder& pBuilder, Module& pModule);

  /// initRelocator - create and initialize Relocator.
  bool initRelocator();

  /// getRelocator - return relocator.
  Relocator* getRelocator();

  /// preLayout - Backend can do any needed modification before layout
  void doPreLayout(IRBuilder& pBuilder);

  /// postLayout - Backend can do any needed modification after layout
  void doPostLayout(Module& pModule, IRBuilder& pBuilder);

  /// dynamic - the dynamic section of the target machine.
  /// Use co-variant return type to return its own dynamic section.
  MipsELFDynamic& dynamic();

  /// dynamic - the dynamic section of the target machine.
  /// Use co-variant return type to return its own dynamic section.
  const MipsELFDynamic& dynamic() const;

  /// emitSectionData - write out the section data into the memory region.
  /// When writers get a LDSection whose kind is LDFileFormat::Target, writers
  /// call back target backend to emit the data.
  ///
  /// Backends handle the target-special tables (plt, gp,...) by themselves.
  /// Backend can put the data of the tables in SectionData directly
  ///  - LDSection.getSectionData can get the section data.
  /// Or, backend can put the data into special data structure
  ///  - backend can maintain its own map<LDSection, table> to get the table
  /// from given LDSection.
  ///
  /// @param pSection - the given LDSection
  /// @param pRegion - the region to write out data
  /// @return the size of the table in the file.
  uint64_t emitSectionData(const LDSection& pSection,
                           MemoryRegion& pRegion) const;

  /// hasEntryInStrTab - symbol has an entry in a .strtab
  bool hasEntryInStrTab(const LDSymbol& pSym) const;

  /// orderSymbolTable - order symbol table before emitting
  void orderSymbolTable(Module& pModule);

  MipsGOT& getGOT();
  const MipsGOT& getGOT() const;

  OutputRelocSection& getRelDyn();
  const OutputRelocSection& getRelDyn() const;

  LDSymbol*             getGOTSymbo()            { return m_pGOTSymbol;    }
  const LDSymbol*       getGOTSymbo() const      { return m_pGOTSymbol;    }

  LDSymbol*             getGpDispSymbol()        { return m_pGpDispSymbol; }
  const LDSymbol*       getGpDispSymbol() const  { return m_pGpDispSymbol; }

  SymbolListType&       getGlobalGOTSyms()       { return m_GlobalGOTSyms; }
  const SymbolListType& getGlobalGOTSyms() const { return m_GlobalGOTSyms; }

  /// getTargetSectionOrder - compute the layout order of ARM target sections
  unsigned int getTargetSectionOrder(const LDSection& pSectHdr) const;

  /// finalizeSymbol - finalize the symbol value
  bool finalizeTargetSymbols();

  /// allocateCommonSymbols - allocate common symbols in the corresponding
  /// sections.
  bool allocateCommonSymbols(Module& pModule);

private:
  void defineGOTSymbol(IRBuilder& pBuilder);

  /// emitSymbol32 - emit an ELF32 symbol, override parent's function
  void emitSymbol32(llvm::ELF::Elf32_Sym& pSym32,
                    LDSymbol& pSymbol,
                    char* pStrtab,
                    size_t pStrtabsize,
                    size_t pSymtabIdx);

  /// getRelEntrySize - the size in BYTE of rel type relocation
  size_t getRelEntrySize()
  { return 8; }

  /// getRelEntrySize - the size in BYTE of rela type relocation
  size_t getRelaEntrySize()
  { return 12; }

  /// doCreateProgramHdrs - backend can implement this function to create the
  /// target-dependent segments
  void doCreateProgramHdrs(Module& pModule);

private:
  Relocator* m_pRelocator;

  MipsGOT* m_pGOT;                      // .got
  OutputRelocSection* m_pRelDyn;        // .rel.dyn

  MipsELFDynamic* m_pDynamic;
  LDSymbol* m_pGOTSymbol;
  LDSymbol* m_pGpDispSymbol;

  SymbolListType m_GlobalGOTSyms;
};

} // namespace of mcld

#endif

