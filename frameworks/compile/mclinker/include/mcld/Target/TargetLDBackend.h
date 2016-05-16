//===-- llvm/Target/TargetLDBackend.h - Target LD Backend -----*- C++ -*-===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_TARGET_TARGETLDBACKEND_H
#define MCLD_TARGET_TARGETLDBACKEND_H

#include <llvm/Support/DataTypes.h>

namespace mcld {

class Module;
class LinkerConfig;
class IRBuilder;
class Relocation;
class RelocationFactory;
class Relocator;
class Layout;
class ArchiveReader;
class ObjectReader;
class DynObjReader;
class BinaryReader;
class ObjectWriter;
class DynObjWriter;
class ExecWriter;
class BinaryWriter;
class LDFileFormat;
class LDSymbol;
class LDSection;
class SectionData;
class Input;
class GOT;
class MemoryArea;
class MemoryAreaFactory;
class BranchIslandFactory;
class StubFactory;
class ObjectBuilder;

//===----------------------------------------------------------------------===//
/// TargetLDBackend - Generic interface to target specific assembler backends.
//===----------------------------------------------------------------------===//
class TargetLDBackend
{
  TargetLDBackend(const TargetLDBackend &);   // DO NOT IMPLEMENT
  void operator=(const TargetLDBackend &);  // DO NOT IMPLEMENT

protected:
  TargetLDBackend(const LinkerConfig& pConfig);

public:
  virtual ~TargetLDBackend();

  // -----  target dependent  ----- //
  virtual void initTargetSegments(IRBuilder& pBuilder) { }
  virtual void initTargetSections(Module& pModule, ObjectBuilder& pBuilder) { }
  virtual void initTargetSymbols(IRBuilder& pBuilder, Module& pModule) { }
  virtual void initTargetRelocation(IRBuilder& pBuilder) { }
  virtual bool initStandardSymbols(IRBuilder& pBuilder, Module& pModule) = 0;

  virtual bool initRelocator() = 0;

  virtual Relocator* getRelocator() = 0;

  // -----  format dependent  ----- //
  virtual ArchiveReader* createArchiveReader(Module&) = 0;
  virtual ObjectReader*  createObjectReader(IRBuilder&) = 0;
  virtual DynObjReader*  createDynObjReader(IRBuilder&) = 0;
  virtual BinaryReader*  createBinaryReader(IRBuilder&) = 0;
  virtual ObjectWriter*  createWriter() = 0;

  virtual bool initStdSections(ObjectBuilder& pBuilder) = 0;

  /// layout - layout method
  virtual void layout(Module& pModule) = 0;

  /// preLayout - Backend can do any needed modification before layout
  virtual void preLayout(Module& pModule, IRBuilder& pBuilder) = 0;

  /// postLayout - Backend can do any needed modification after layout
  virtual void postLayout(Module& pModule, IRBuilder& pBuilder) = 0;

  /// postProcessing - Backend can do any needed modification in the final stage
  virtual void postProcessing(MemoryArea& pOutput) = 0;

  /// section start offset in the output file
  virtual size_t sectionStartOffset() const = 0;

  /// computeSectionOrder - compute the layout order of the given section
  virtual unsigned int getSectionOrder(const LDSection& pSectHdr) const = 0;

  /// sizeNamePools - compute the size of regular name pools
  /// In ELF executable files, regular name pools are .symtab, .strtab.,
  /// .dynsym, .dynstr, and .hash
  virtual void sizeNamePools(Module& pModule) = 0;

  /// finalizeSymbol - Linker checks pSymbol.reserved() if it's not zero,
  /// then it will ask backend to finalize the symbol value.
  /// @return ture - if backend set the symbol value sucessfully
  /// @return false - if backend do not recognize the symbol
  virtual bool finalizeSymbols() = 0;

  /// finalizeTLSSymbol - Linker asks backend to set the symbol value when it
  /// meets a TLS symbol
  virtual bool finalizeTLSSymbol(LDSymbol& pSymbol) = 0;

  /// allocateCommonSymbols - allocate common symbols in the corresponding
  /// sections.
  virtual bool allocateCommonSymbols(Module& pModule) = 0;

  /// mergeSection - merge target dependent sections.
  virtual bool mergeSection(Module& pModule, LDSection& pInputSection)
  { return true; }

  /// updateSectionFlags - update pTo's flags when merging pFrom
  /// update the output section flags based on input section flags.
  /// FIXME: (Luba) I know ELF need to merge flags, but I'm not sure if
  /// MachO and COFF also need this.
  virtual bool updateSectionFlags(LDSection& pTo, const LDSection& pFrom)
  { return true; }

  /// readSection - read a target dependent section
  virtual bool readSection(Input& pInput, SectionData& pSD)
  { return true; }

  /// sizeInterp - compute the size of program interpreter's name
  /// In ELF executables, this is the length of dynamic linker's path name
  virtual void sizeInterp() = 0;

  // -----  relaxation  ----- //
  virtual bool initBRIslandFactory() = 0;
  virtual bool initStubFactory() = 0;
  virtual bool initTargetStubs() { return true; }

  virtual BranchIslandFactory* getBRIslandFactory() = 0;
  virtual StubFactory*         getStubFactory() = 0;

  /// relax - the relaxation pass
  virtual bool relax(Module& pModule, IRBuilder& pBuilder) = 0;

  /// mayRelax - return true if the backend needs to do relaxation
  virtual bool mayRelax() = 0;

protected:
  const LinkerConfig& config() const { return m_Config; }

private:
  const LinkerConfig& m_Config;
};

} // End mcld namespace

#endif
