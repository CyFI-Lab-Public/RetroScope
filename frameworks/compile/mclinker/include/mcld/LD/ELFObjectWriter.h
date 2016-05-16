//===- ELFObjectWriter.h --------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_ELF_OBJECT_WRITER_H
#define MCLD_ELF_OBJECT_WRITER_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <mcld/LD/ObjectWriter.h>
#include <cassert>

#include <llvm/Support/system_error.h>

namespace mcld {

class Module;
class LinkerConfig;
class GNULDBackend;
class FragmentLinker;
class Relocation;
class LDSection;
class SectionData;
class RelocData;
class Output;
class MemoryRegion;
class MemoryArea;

/** \class ELFObjectWriter
 *  \brief ELFObjectWriter writes the target-independent parts of object files.
 *  ELFObjectWriter reads a MCLDFile and writes into raw_ostream
 *
 */
class ELFObjectWriter : public ObjectWriter
{
public:
  ELFObjectWriter(GNULDBackend& pBackend, const LinkerConfig& pConfig);

  ~ELFObjectWriter();

  llvm::error_code writeObject(Module& pModule, MemoryArea& pOutput);

private:
  void writeSection(MemoryArea& pOutput, LDSection *section);

  GNULDBackend&       target()        { return m_Backend; }

  const GNULDBackend& target() const  { return m_Backend; }

  // writeELFHeader - emit ElfXX_Ehdr
  template<size_t SIZE>
  void writeELFHeader(const LinkerConfig& pConfig,
                      const Module& pModule,
                      MemoryArea& pOutput) const;

  uint64_t getEntryPoint(const LinkerConfig& pConfig,
                         const Module& pModule) const;

  // emitSectionHeader - emit ElfXX_Shdr
  template<size_t SIZE>
  void emitSectionHeader(const Module& pModule,
                         const LinkerConfig& pConfig,
                         MemoryArea& pOutput) const;

  // emitProgramHeader - emit ElfXX_Phdr
  template<size_t SIZE>
  void emitProgramHeader(MemoryArea& pOutput) const;

  // emitShStrTab - emit .shstrtab
  void emitShStrTab(const LDSection& pShStrTab,
                    const Module& pModule,
                    MemoryArea& pOutput);

  void emitSectionData(const LDSection& pSection,
                       MemoryRegion& pRegion) const;

  void emitRelocation(const LinkerConfig& pConfig,
                      const LDSection& pSection,
                      MemoryRegion& pRegion) const;

  // emitRel - emit ElfXX_Rel
  template<size_t SIZE>
  void emitRel(const LinkerConfig& pConfig,
               const RelocData& pRelocData,
               MemoryRegion& pRegion) const;

  // emitRela - emit ElfXX_Rela
  template<size_t SIZE>
  void emitRela(const LinkerConfig& pConfig,
                const RelocData& pRelocData,
                MemoryRegion& pRegion) const;

  // getSectEntrySize - compute ElfXX_Shdr::sh_entsize
  template<size_t SIZE>
  uint64_t getSectEntrySize(const LDSection& pSection) const;

  // getSectLink - compute ElfXX_Shdr::sh_link
  uint64_t getSectLink(const LDSection& pSection,
                       const LinkerConfig& pConfig) const;

  // getSectInfo - compute ElfXX_Shdr::sh_info
  uint64_t getSectInfo(const LDSection& pSection) const;

  template<size_t SIZE>
  uint64_t getLastStartOffset(const Module& pModule) const
  {
    assert(0 && "Call invalid ELFObjectWriter::getLastStartOffset");
    return 0;
  }

  void emitSectionData(const SectionData& pSD, MemoryRegion& pRegion) const;

private:
  GNULDBackend& m_Backend;

  const LinkerConfig& m_Config;
};

template<>
uint64_t ELFObjectWriter::getLastStartOffset<32>(const Module& pModule) const;

template<>
uint64_t ELFObjectWriter::getLastStartOffset<64>(const Module& pModule) const;

} // namespace of mcld

#endif

