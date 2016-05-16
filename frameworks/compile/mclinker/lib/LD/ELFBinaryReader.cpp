//===- ELFBinaryReader.cpp ------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/LD/ELFBinaryReader.h>

#include <mcld/IRBuilder.h>
#include <mcld/LinkerConfig.h>
#include <mcld/MC/MCLDInput.h>
#include <mcld/Support/MemoryArea.h>
#include <mcld/Target/GNULDBackend.h>

#include <llvm/Support/ELF.h>

#include <cctype>

using namespace mcld;

//===----------------------------------------------------------------------===//
// ELFBinaryReader
//===----------------------------------------------------------------------===//
/// constructor
ELFBinaryReader::ELFBinaryReader(GNULDBackend& pBackend,
                                 IRBuilder& pBuilder,
                                 const LinkerConfig& pConfig)
  : BinaryReader(),
    m_Backend(pBackend),
    m_Builder(pBuilder),
    m_Config(pConfig) {
}

/// destructor
ELFBinaryReader::~ELFBinaryReader()
{
}

bool ELFBinaryReader::readBinary(Input& pInput)
{
  // section: NULL
  m_Builder.CreateELFHeader(pInput,
                            "",
                            LDFileFormat::Null,
                            llvm::ELF::SHT_NULL,
                            0x0);

  // section: .data
  LDSection* data_sect =
    m_Builder.CreateELFHeader(pInput,
                              ".data",
                              LDFileFormat::Regular,
                              llvm::ELF::SHF_WRITE | llvm::ELF::SHF_ALLOC,
                              0x1);


  SectionData* data = m_Builder.CreateSectionData(*data_sect);
  size_t data_size = pInput.memArea()->handler()->size();
  Fragment* frag = m_Builder.CreateRegion(pInput, 0x0, data_size);
  m_Builder.AppendFragment(*frag, *data);

  // section: .shstrtab
  m_Builder.CreateELFHeader(pInput,
                            ".shstrtab",
                            LDFileFormat::NamePool,
                            llvm::ELF::SHT_STRTAB,
                            0x1);

  // section: .symtab
  m_Builder.CreateELFHeader(pInput,
                            ".symtab",
                            LDFileFormat::NamePool,
                            llvm::ELF::SHT_SYMTAB,
                            m_Config.targets().bitclass() / 8);

  // symbol: .data
  m_Builder.AddSymbol(pInput,
                      ".data",
                      ResolveInfo::Section,
                      ResolveInfo::Define,
                      ResolveInfo::Local,
                      0x0,
                      0x0,
                      data_sect);

  // Note: in Win32, the filename is wstring. Is it correct to convert
  // filename to std::string?
  std::string mangled_name = pInput.path().filename().native();
  for (std::string::iterator it = mangled_name.begin(),
    ie = mangled_name.end(); it != ie; ++it) {
    if (isalnum(*it) == 0)
      *it = '_';
  }

  // symbol: _start
  m_Builder.AddSymbol(pInput,
                      "_binary_" + mangled_name + "_start",
                      ResolveInfo::NoType,
                      ResolveInfo::Define,
                      ResolveInfo::Global,
                      0x0,
                      0x0,
                      data_sect);

  // symbol: _end
  m_Builder.AddSymbol(pInput,
                      "_binary_" + mangled_name + "_end",
                      ResolveInfo::NoType,
                      ResolveInfo::Define,
                      ResolveInfo::Global,
                      0x0,
                      data_size,
                      data_sect);

  // symbol: _size
  m_Builder.AddSymbol(pInput,
                      "_binary_" + mangled_name + "_size",
                      ResolveInfo::NoType,
                      ResolveInfo::Define,
                      ResolveInfo::Global,
                      0x0,
                      data_size,
                      data_sect);

  // section: .strtab
  m_Builder.CreateELFHeader(pInput,
                            ".strtab",
                            LDFileFormat::NamePool,
                            llvm::ELF::SHT_STRTAB,
                            0x1);

  return true;
}
