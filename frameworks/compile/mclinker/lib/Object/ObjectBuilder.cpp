//===- ObjectBuilder.cpp --------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Object/ObjectBuilder.h>

#include <mcld/Module.h>
#include <mcld/LinkerConfig.h>
#include <mcld/IRBuilder.h>
#include <mcld/Object/SectionMap.h>
#include <mcld/LD/LDSection.h>
#include <mcld/LD/SectionData.h>
#include <mcld/LD/RelocData.h>
#include <mcld/LD/EhFrame.h>
#include <mcld/Fragment/Relocation.h>
#include <mcld/Fragment/AlignFragment.h>
#include <mcld/Fragment/NullFragment.h>
#include <mcld/Fragment/FillFragment.h>

#include <llvm/Support/Casting.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// ObjectBuilder
//===----------------------------------------------------------------------===//
ObjectBuilder::ObjectBuilder(const LinkerConfig& pConfig, Module& pTheModule)
  : m_Config(pConfig), m_Module(pTheModule) {
}

/// CreateSection - create an output section.
LDSection* ObjectBuilder::CreateSection(const std::string& pName,
                                        LDFileFormat::Kind pKind,
                                        uint32_t pType,
                                        uint32_t pFlag,
                                        uint32_t pAlign)
{
  // try to get one from output LDSection
  const SectionMap::NamePair& pair = m_Module.getScript().sectionMap().find(pName);
  std::string output_name = (pair.isNull())?pName:pair.to;
  LDSection* output_sect = LDSection::Create(output_name, pKind, pType, pFlag);
  output_sect->setAlign(pAlign);
  m_Module.getSectionTable().push_back(output_sect);
  return output_sect;
}

/// MergeSection - merge the pInput section to the pOutput section
LDSection* ObjectBuilder::MergeSection(LDSection& pInputSection)
{
  const SectionMap::NamePair& pair =
              m_Module.getScript().sectionMap().find(pInputSection.name());
  std::string output_name = (pair.isNull())?pInputSection.name():pair.to;
  LDSection* target = m_Module.getSection(output_name);

  if (NULL == target) {
    target = LDSection::Create(output_name,
                               pInputSection.kind(),
                               pInputSection.type(),
                               pInputSection.flag());
    target->setAlign(pInputSection.align());
    m_Module.getSectionTable().push_back(target);
  }

  switch (target->kind()) {
    // Some *OUTPUT sections should not be merged.
    case LDFileFormat::Relocation:
    case LDFileFormat::NamePool:
      /** do nothing **/
      return target;
    case LDFileFormat::EhFrame: {
      EhFrame* eh_frame = NULL;
      if (target->hasEhFrame())
        eh_frame = target->getEhFrame();
      else
        eh_frame = IRBuilder::CreateEhFrame(*target);

      eh_frame->merge(*pInputSection.getEhFrame());
			UpdateSectionAlign(*target, pInputSection);
      return target;
    }
    default: {
      SectionData* data = NULL;
      if (target->hasSectionData())
        data = target->getSectionData();
      else
        data = IRBuilder::CreateSectionData(*target);

      if (MoveSectionData(*pInputSection.getSectionData(), *data)) {
        UpdateSectionAlign(*target, pInputSection);
        return target;
      }
      return NULL;
    }
  }
  return target;
}

/// MoveSectionData - move the fragments of pTO section data to pTo
bool ObjectBuilder::MoveSectionData(SectionData& pFrom, SectionData& pTo)
{
  assert(&pFrom != &pTo && "Cannot move section data to itself!");

  uint32_t offset = pTo.getSection().size();
  AlignFragment* align = NULL;
  if (pFrom.getSection().align() > 1) {
    // if the align constraint is larger than 1, append an alignment
    align = new AlignFragment(pFrom.getSection().align(), // alignment
                              0x0, // the filled value
                              1u,  // the size of filled value
                              pFrom.getSection().align() - 1 // max bytes to emit
                              );
    align->setOffset(offset);
    align->setParent(&pTo);
    pTo.getFragmentList().push_back(align);
    offset += align->size();
  }

  // move fragments from pFrom to pTO
  SectionData::FragmentListType& from_list = pFrom.getFragmentList();
  SectionData::FragmentListType& to_list = pTo.getFragmentList();
  SectionData::FragmentListType::iterator frag, fragEnd = from_list.end();
  for (frag = from_list.begin(); frag != fragEnd; ++frag) {
    frag->setParent(&pTo);
    frag->setOffset(offset);
    offset += frag->size();
  }
  to_list.splice(to_list.end(), from_list);

  // set up pTo's header
  pTo.getSection().setSize(offset);

  return true;
}

/// UpdateSectionFlags - update alignment for input section
void ObjectBuilder::UpdateSectionAlign(LDSection& pTo, const LDSection& pFrom)
{
  if (pFrom.align() > pTo.align())
    pTo.setAlign(pFrom.align());
}

/// AppendFragment - To append pFrag to the given SectionData pSD.
uint64_t ObjectBuilder::AppendFragment(Fragment& pFrag,
                                       SectionData& pSD,
                                       uint32_t pAlignConstraint)
{
  // get initial offset.
  uint32_t offset = 0;
  if (!pSD.empty())
    offset = pSD.back().getOffset() + pSD.back().size();

  AlignFragment* align = NULL;
  if (pAlignConstraint > 1) {
    // if the align constraint is larger than 1, append an alignment
    align = new AlignFragment(pAlignConstraint, // alignment
                              0x0, // the filled value
                              1u,  // the size of filled value
                              pAlignConstraint - 1 // max bytes to emit
                              );
    align->setOffset(offset);
    align->setParent(&pSD);
    pSD.getFragmentList().push_back(align);
    offset += align->size();
  }

  // append the fragment
  pFrag.setParent(&pSD);
  pFrag.setOffset(offset);
  pSD.getFragmentList().push_back(&pFrag);

  // append the null fragment
  offset += pFrag.size();
  NullFragment* null = new NullFragment(&pSD);
  null->setOffset(offset);

  if (NULL != align)
    return align->size() + pFrag.size();
  else
    return pFrag.size();
}

