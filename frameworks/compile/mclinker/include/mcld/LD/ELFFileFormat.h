//===- LDFileFormat.h -----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_ELF_FILE_FORMAT_H
#define MCLD_ELF_FILE_FORMAT_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <mcld/LD/LDFileFormat.h>
#include <mcld/LD/LDSection.h>

namespace mcld {

class ObjectBuilder;

/** \class ELFFileFormat
 *  \brief ELFFileFormat describes the common file formats in ELF.
 *  LDFileFormats control the formats of the output file.
 *
 *  @ref "Object Files," Ch. 4, in System V Application Binary Interface,
 *  Fourth Edition.
 *
 *  @ref "Object Format," Ch. 10, in ISO/IEC 23360 Part 1:2010(E), Linux
 *  Standard Base Core Specification 4.1.
 */
class ELFFileFormat : public LDFileFormat
{
private:
  /// initObjectFormat - initialize sections that are dependent on object
  /// formats. (executable, shared objects or relocatable objects).
  virtual void
  initObjectFormat(ObjectBuilder& pBuilder, unsigned int pBitClass) = 0;

public:
  ELFFileFormat();

  void initStdSections(ObjectBuilder& pBuilder, unsigned int pBitClass);

  // -----  capacity  ----- //
  /// @ref Special Sections, Ch. 4.17, System V ABI, 4th edition.
  bool hasNULLSection() const
  { return (NULL != f_pNULLSection) && (0 != f_pNULLSection->size()); }

  bool hasGOT() const
  { return (NULL != f_pGOT) && (0 != f_pGOT->size()); }

  bool hasPLT() const
  { return (NULL != f_pPLT) && (0 != f_pPLT->size()); }

  bool hasRelDyn() const
  { return (NULL != f_pRelDyn) && (0 != f_pRelDyn->size()); }

  bool hasRelPlt() const
  { return (NULL != f_pRelPlt) && (0 != f_pRelPlt->size()); }

  bool hasRelaDyn() const
  { return (NULL != f_pRelaDyn) && (0 != f_pRelaDyn->size()); }

  bool hasRelaPlt() const
  { return (NULL != f_pRelaPlt) && (0 != f_pRelaPlt->size()); }

  /// @ref 10.3.1.1, ISO/IEC 23360, Part 1:2010(E), p. 21.
  bool hasComment() const
  { return (NULL != f_pComment) && (0 != f_pComment->size()); }

  bool hasData1() const
  { return (NULL != f_pData1) && (0 != f_pData1->size()); }

  bool hasDebug() const
  { return (NULL != f_pDebug) && (0 != f_pDebug->size()); }

  bool hasDynamic() const
  { return (NULL != f_pDynamic) && (0 != f_pDynamic->size()); }

  bool hasDynStrTab() const
  { return (NULL != f_pDynStrTab) && (0 != f_pDynStrTab->size()); }

  bool hasDynSymTab() const
  { return (NULL != f_pDynSymTab) && (0 != f_pDynSymTab->size()); }

  bool hasFini() const
  { return (NULL != f_pFini) && (0 != f_pFini->size()); }

  bool hasFiniArray() const
  { return (NULL != f_pFiniArray) && (0 != f_pFiniArray->size()); }

  bool hasHashTab() const
  { return (NULL != f_pHashTab) && (0 != f_pHashTab->size()); }

  bool hasInit() const
  { return (NULL != f_pInit) && (0 != f_pInit->size()); }

  bool hasInitArray() const
  { return (NULL != f_pInitArray) && (0 != f_pInitArray->size()); }

  bool hasInterp() const
  { return (NULL != f_pInterp) && (0 != f_pInterp->size()); }

  bool hasLine() const
  { return (NULL != f_pLine) && (0 != f_pLine->size()); }

  bool hasNote() const
  { return (NULL != f_pNote) && (0 != f_pNote->size()); }

  bool hasPreInitArray() const
  { return (NULL != f_pPreInitArray) && (0 != f_pPreInitArray->size()); }

  bool hasROData1() const
  { return (NULL != f_pROData1) && (0 != f_pROData1->size()); }

  bool hasShStrTab() const
  { return (NULL != f_pShStrTab) && (0 != f_pShStrTab->size()); }

  bool hasStrTab() const
  { return (NULL != f_pStrTab) && (0 != f_pStrTab->size()); }

  bool hasSymTab() const
  { return (NULL != f_pSymTab) && (0 != f_pSymTab->size()); }

  bool hasTBSS() const
  { return (NULL != f_pTBSS) && (0 != f_pTBSS->size()); }

  bool hasTData() const
  { return (NULL != f_pTData) && (0 != f_pTData->size()); }

  /// @ref 10.3.1.2, ISO/IEC 23360, Part 1:2010(E), p. 24.
  bool hasCtors() const
  { return (NULL != f_pCtors) && (0 != f_pCtors->size()); }

  bool hasDataRelRo() const
  { return (NULL != f_pDataRelRo) && (0 != f_pDataRelRo->size()); }

  bool hasDtors() const
  { return (NULL != f_pDtors) && (0 != f_pDtors->size()); }

  bool hasEhFrame() const
  { return (NULL != f_pEhFrame) && (0 != f_pEhFrame->size()); }

  bool hasEhFrameHdr() const
  { return (NULL != f_pEhFrameHdr) && (0 != f_pEhFrameHdr->size()); }

  bool hasGCCExceptTable() const
  { return (NULL != f_pGCCExceptTable) && (0 != f_pGCCExceptTable->size()); }

  bool hasGNUVersion() const
  { return (NULL != f_pGNUVersion) && (0 != f_pGNUVersion->size()); }

  bool hasGNUVersionD() const
  { return (NULL != f_pGNUVersionD) && (0 != f_pGNUVersionD->size()); }

  bool hasGNUVersionR() const
  { return (NULL != f_pGNUVersionR) && (0 != f_pGNUVersionR->size()); }

  bool hasGOTPLT() const
  { return (NULL != f_pGOTPLT) && (0 != f_pGOTPLT->size()); }

  bool hasJCR() const
  { return (NULL != f_pJCR) && (0 != f_pJCR->size()); }

  bool hasNoteABITag() const
  { return (NULL != f_pNoteABITag) && (0 != f_pNoteABITag->size()); }

  bool hasStab() const
  { return (NULL != f_pStab) && (0 != f_pStab->size()); }

  bool hasStabStr() const
  { return (NULL != f_pStabStr) && (0 != f_pStabStr->size()); }

  bool hasStack() const
  { return (NULL != f_pStack) && (0 != f_pStack->size()); }

  bool hasStackNote() const
  { return (NULL != f_pStackNote); }

  bool hasDataRelRoLocal() const
  { return (NULL != f_pDataRelRoLocal) && (0 != f_pDataRelRoLocal->size()); }

  bool hasGNUHashTab() const
  { return (NULL != f_pGNUHashTab) && (0 != f_pGNUHashTab->size()); }

  // -----  access functions  ----- //
  /// @ref Special Sections, Ch. 4.17, System V ABI, 4th edition.
  LDSection& getNULLSection() {
    assert(NULL != f_pNULLSection);
    return *f_pNULLSection;
  }

  const LDSection& getNULLSection() const {
    assert(NULL != f_pNULLSection);
    return *f_pNULLSection;
  }

  LDSection& getGOT() {
    assert(NULL != f_pGOT);
    return *f_pGOT;
  }

  const LDSection& getGOT() const {
    assert(NULL != f_pGOT);
    return *f_pGOT;
  }

  LDSection& getPLT() {
    assert(NULL != f_pPLT);
    return *f_pPLT;
  }

  const LDSection& getPLT() const {
    assert(NULL != f_pPLT);
    return *f_pPLT;
  }

  LDSection& getRelDyn() {
    assert(NULL != f_pRelDyn);
    return *f_pRelDyn;
  }

  const LDSection& getRelDyn() const {
    assert(NULL != f_pRelDyn);
    return *f_pRelDyn;
  }

  LDSection& getRelPlt() {
    assert(NULL != f_pRelPlt);
    return *f_pRelPlt;
  }

  const LDSection& getRelPlt() const {
    assert(NULL != f_pRelPlt);
    return *f_pRelPlt;
  }

  LDSection& getRelaDyn() {
    assert(NULL != f_pRelaDyn);
    return *f_pRelaDyn;
  }

  const LDSection& getRelaDyn() const {
    assert(NULL != f_pRelaDyn);
    return *f_pRelaDyn;
  }

  LDSection& getRelaPlt() {
    assert(NULL != f_pRelaPlt);
    return *f_pRelaPlt;
  }

  const LDSection& getRelaPlt() const {
    assert(NULL != f_pRelaPlt);
    return *f_pRelaPlt;
  }

  LDSection& getComment() {
    assert(NULL != f_pComment);
    return *f_pComment;
  }

  /// @ref 10.3.1.1, ISO/IEC 23360, Part 1:2010(E), p. 21.
  const LDSection& getComment() const {
    assert(NULL != f_pComment);
    return *f_pComment;
  }

  LDSection& getData1() {
    assert(NULL != f_pData1);
    return *f_pData1;
  }

  const LDSection& getData1() const {
    assert(NULL != f_pData1);
    return *f_pData1;
  }

  LDSection& getDebug() {
    assert(NULL != f_pDebug);
    return *f_pDebug;
  }

  const LDSection& getDebug() const {
    assert(NULL != f_pDebug);
    return *f_pDebug;
  }

  LDSection& getDynamic() {
    assert(NULL != f_pDynamic);
    return *f_pDynamic;
  }

  const LDSection& getDynamic() const {
    assert(NULL != f_pDynamic);
    return *f_pDynamic;
  }

  LDSection& getDynStrTab() {
    assert(NULL != f_pDynStrTab);
    return *f_pDynStrTab;
  }

  const LDSection& getDynStrTab() const {
    assert(NULL != f_pDynStrTab);
    return *f_pDynStrTab;
  }

  LDSection& getDynSymTab() {
    assert(NULL != f_pDynSymTab);
    return *f_pDynSymTab;
  }

  const LDSection& getDynSymTab() const {
    assert(NULL != f_pDynSymTab);
    return *f_pDynSymTab;
  }

  LDSection& getFini() {
    assert(NULL != f_pFini);
    return *f_pFini;
  }

  const LDSection& getFini() const {
    assert(NULL != f_pFini);
    return *f_pFini;
  }

  LDSection& getFiniArray() {
    assert(NULL != f_pFiniArray);
    return *f_pFiniArray;
  }

  const LDSection& getFiniArray() const {
    assert(NULL != f_pFiniArray);
    return *f_pFiniArray;
  }

  LDSection& getHashTab() {
    assert(NULL != f_pHashTab);
    return *f_pHashTab;
  }

  const LDSection& getHashTab() const {
    assert(NULL != f_pHashTab);
    return *f_pHashTab;
  }

  LDSection& getInit() {
    assert(NULL != f_pInit);
    return *f_pInit;
  }

  const LDSection& getInit() const {
    assert(NULL != f_pInit);
    return *f_pInit;
  }

  LDSection& getInitArray() {
    assert(NULL != f_pInitArray);
    return *f_pInitArray;
  }

  const LDSection& getInitArray() const {
    assert(NULL != f_pInitArray);
    return *f_pInitArray;
  }

  LDSection& getInterp() {
    assert(NULL != f_pInterp);
    return *f_pInterp;
  }

  const LDSection& getInterp() const {
    assert(NULL != f_pInterp);
    return *f_pInterp;
  }

  LDSection& getLine() {
    assert(NULL != f_pLine);
    return *f_pLine;
  }

  const LDSection& getLine() const {
    assert(NULL != f_pLine);
    return *f_pLine;
  }

  LDSection& getNote() {
    assert(NULL != f_pNote);
    return *f_pNote;
  }

  const LDSection& getNote() const {
    assert(NULL != f_pNote);
    return *f_pNote;
  }

  LDSection& getPreInitArray() {
    assert(NULL != f_pPreInitArray);
    return *f_pPreInitArray;
  }

  const LDSection& getPreInitArray() const {
    assert(NULL != f_pPreInitArray);
    return *f_pPreInitArray;
  }

  LDSection& getROData1() {
    assert(NULL != f_pROData1);
    return *f_pROData1;
  }

  const LDSection& getROData1() const {
    assert(NULL != f_pROData1);
    return *f_pROData1;
  }

  LDSection& getShStrTab() {
    assert(NULL != f_pShStrTab);
    return *f_pShStrTab;
  }

  const LDSection& getShStrTab() const {
    assert(NULL != f_pShStrTab);
    return *f_pShStrTab;
  }

  LDSection& getStrTab() {
    assert(NULL != f_pStrTab);
    return *f_pStrTab;
  }

  const LDSection& getStrTab() const {
    assert(NULL != f_pStrTab);
    return *f_pStrTab;
  }

  LDSection& getSymTab() {
    assert(NULL != f_pSymTab);
    return *f_pSymTab;
  }

  const LDSection& getSymTab() const {
    assert(NULL != f_pSymTab);
    return *f_pSymTab;
  }

  LDSection& getTBSS() {
    assert(NULL != f_pTBSS);
    return *f_pTBSS;
  }

  const LDSection& getTBSS() const {
    assert(NULL != f_pTBSS);
    return *f_pTBSS;
  }

  LDSection& getTData() {
    assert(NULL != f_pTData);
    return *f_pTData;
  }

  const LDSection& getTData() const {
    assert(NULL != f_pTData);
    return *f_pTData;
  }

  /// @ref 10.3.1.2, ISO/IEC 23360, Part 1:2010(E), p. 24.
  LDSection& getCtors() {
    assert(NULL != f_pCtors);
    return *f_pCtors;
  }

  const LDSection& getCtors() const {
    assert(NULL != f_pCtors);
    return *f_pCtors;
  }

  LDSection& getDataRelRo() {
    assert(NULL != f_pDataRelRo);
    return *f_pDataRelRo;
  }

  const LDSection& getDataRelRo() const {
    assert(NULL != f_pDataRelRo);
    return *f_pDataRelRo;
  }

  LDSection& getDtors() {
    assert(NULL != f_pDtors);
    return *f_pDtors;
  }

  const LDSection& getDtors() const {
    assert(NULL != f_pDtors);
    return *f_pDtors;
  }

  LDSection& getEhFrame() {
    assert(NULL != f_pEhFrame);
    return *f_pEhFrame;
  }

  const LDSection& getEhFrame() const {
    assert(NULL != f_pEhFrame);
    return *f_pEhFrame;
  }

  LDSection& getEhFrameHdr() {
    assert(NULL != f_pEhFrameHdr);
    return *f_pEhFrameHdr;
  }

  const LDSection& getEhFrameHdr() const {
    assert(NULL != f_pEhFrameHdr);
    return *f_pEhFrameHdr;
  }

  LDSection& getGCCExceptTable() {
    assert(NULL != f_pGCCExceptTable);
    return *f_pGCCExceptTable;
  }

  const LDSection& getGCCExceptTable() const {
    assert(NULL != f_pGCCExceptTable);
    return *f_pGCCExceptTable;
  }

  LDSection& getGNUVersion() {
    assert(NULL != f_pGNUVersion);
    return *f_pGNUVersion;
  }

  const LDSection& getGNUVersion() const {
    assert(NULL != f_pGNUVersion);
    return *f_pGNUVersion;
  }

  LDSection& getGNUVersionD() {
    assert(NULL != f_pGNUVersionD);
    return *f_pGNUVersionD;
  }

  const LDSection& getGNUVersionD() const {
    assert(NULL != f_pGNUVersionD);
    return *f_pGNUVersionD;
  }

  LDSection& getGNUVersionR() {
    assert(NULL != f_pGNUVersionR);
    return *f_pGNUVersionR;
  }

  const LDSection& getGNUVersionR() const {
    assert(NULL != f_pGNUVersionR);
    return *f_pGNUVersionR;
  }

  LDSection& getGOTPLT() {
    assert(NULL != f_pGOTPLT);
    return *f_pGOTPLT;
  }

  const LDSection& getGOTPLT() const {
    assert(NULL != f_pGOTPLT);
    return *f_pGOTPLT;
  }

  LDSection& getJCR() {
    assert(NULL != f_pJCR);
    return *f_pJCR;
  }

  const LDSection& getJCR() const {
    assert(NULL != f_pJCR);
    return *f_pJCR;
  }

  LDSection& getNoteABITag() {
    assert(NULL != f_pNoteABITag);
    return *f_pNoteABITag;
  }

  const LDSection& getNoteABITag() const {
    assert(NULL != f_pNoteABITag);
    return *f_pNoteABITag;
  }

  LDSection& getStab() {
    assert(NULL != f_pStab);
    return *f_pStab;
  }

  const LDSection& getStab() const {
    assert(NULL != f_pStab);
    return *f_pStab;
  }

  LDSection& getStabStr() {
    assert(NULL != f_pStabStr);
    return *f_pStabStr;
  }

  const LDSection& getStabStr() const {
    assert(NULL != f_pStabStr);
    return *f_pStabStr;
  }

  LDSection& getStack() {
    assert(NULL != f_pStack);
    return *f_pStack;
  }

  const LDSection& getStack() const {
    assert(NULL != f_pStack);
    return *f_pStack;
  }

  LDSection& getStackNote() {
    assert(NULL != f_pStackNote);
    return *f_pStackNote;
  }

  const LDSection& getStackNote() const {
    assert(NULL != f_pStackNote);
    return *f_pStackNote;
  }

  LDSection& getDataRelRoLocal() {
    assert(NULL != f_pDataRelRoLocal);
    return *f_pDataRelRoLocal;
  }

  const LDSection& getDataRelRoLocal() const {
    assert(NULL != f_pDataRelRoLocal);
    return *f_pDataRelRoLocal;
  }

  LDSection& getGNUHashTab() {
    assert(NULL != f_pGNUHashTab);
    return *f_pGNUHashTab;
  }

  const LDSection& getGNUHashTab() const {
    assert(NULL != f_pGNUHashTab);
    return *f_pGNUHashTab;
  }

protected:
  //         variable name         :  ELF
  /// @ref Special Sections, Ch. 4.17, System V ABI, 4th edition.
  LDSection* f_pNULLSection;
  LDSection* f_pGOT;               // .got
  LDSection* f_pPLT;               // .plt
  LDSection* f_pRelDyn;            // .rel.dyn
  LDSection* f_pRelPlt;            // .rel.plt
  LDSection* f_pRelaDyn;           // .rela.dyn
  LDSection* f_pRelaPlt;           // .rela.plt

  /// @ref 10.3.1.1, ISO/IEC 23360, Part 1:2010(E), p. 21.
  LDSection* f_pComment;           // .comment
  LDSection* f_pData1;             // .data1
  LDSection* f_pDebug;             // .debug
  LDSection* f_pDynamic;           // .dynamic
  LDSection* f_pDynStrTab;         // .dynstr
  LDSection* f_pDynSymTab;         // .dynsym
  LDSection* f_pFini;              // .fini
  LDSection* f_pFiniArray;         // .fini_array
  LDSection* f_pHashTab;           // .hash
  LDSection* f_pInit;              // .init
  LDSection* f_pInitArray;         // .init_array
  LDSection* f_pInterp;            // .interp
  LDSection* f_pLine;              // .line
  LDSection* f_pNote;              // .note
  LDSection* f_pPreInitArray;      // .preinit_array
  LDSection* f_pROData1;           // .rodata1
  LDSection* f_pShStrTab;          // .shstrtab
  LDSection* f_pStrTab;            // .strtab
  LDSection* f_pSymTab;            // .symtab
  LDSection* f_pTBSS;              // .tbss
  LDSection* f_pTData;             // .tdata

  /// @ref 10.3.1.2, ISO/IEC 23360, Part 1:2010(E), p. 24.
  LDSection* f_pCtors;             // .ctors
  LDSection* f_pDataRelRo;         // .data.rel.ro
  LDSection* f_pDtors;             // .dtors
  LDSection* f_pEhFrame;           // .eh_frame
  LDSection* f_pEhFrameHdr;        // .eh_frame_hdr
  LDSection* f_pGCCExceptTable;    // .gcc_except_table
  LDSection* f_pGNUVersion;        // .gnu.version
  LDSection* f_pGNUVersionD;       // .gnu.version_d
  LDSection* f_pGNUVersionR;       // .gnu.version_r
  LDSection* f_pGOTPLT;            // .got.plt
  LDSection* f_pJCR;               // .jcr
  LDSection* f_pNoteABITag;        // .note.ABI-tag
  LDSection* f_pStab;              // .stab
  LDSection* f_pStabStr;           // .stabstr

  /// practical
  LDSection* f_pStack;             // .stack
  LDSection* f_pStackNote;         // .note.GNU-stack
  LDSection* f_pDataRelRoLocal;    // .data.rel.ro.local
  LDSection* f_pGNUHashTab;        // .gnu.hash
};

} // namespace of mcld

#endif

