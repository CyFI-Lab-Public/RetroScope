//===- FragmentGraph.cpp --------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Fragment/FragmentGraph.h>
#include <mcld/Fragment/Fragment.h>
#include <mcld/Fragment/Relocation.h>
#include <mcld/LD/LDContext.h>
#include <mcld/LD/LDFileFormat.h>
#include <mcld/LD/LDSection.h>
#include <mcld/LD/LDSymbol.h>
#include <mcld/LD/SectionData.h>
#include <mcld/LD/RelocData.h>
#include <mcld/LinkerConfig.h>
#include <mcld/Module.h>
#include <mcld/Support/MsgHandling.h>

#include <llvm/Support/Casting.h>
#include <llvm/Support/ELF.h>

#include <iostream>

using namespace mcld;

//===----------------------------------------------------------------------===//
// non-member functions
//===----------------------------------------------------------------------===//
static int get_state(Fragment::Type pKind)
{
  switch(pKind) {
    case Fragment::Alignment:
      return 0;
    case Fragment::Fillment:
    case Fragment::Region:
      return 1;
    case Fragment::Null:
      return 2;
    default:
      unreachable(diag::unexpected_frag_type) << pKind;
  }
  return 0;
}

//===----------------------------------------------------------------------===//
// ReachMatrix
//===----------------------------------------------------------------------===//
FragmentGraph::ReachMatrix::ReachMatrix(size_t pSize)
{
  assert(pSize != 0);
  m_Data.assign(pSize * pSize, 0x0);
  m_N = pSize;
}

uint32_t& FragmentGraph::ReachMatrix::at(uint32_t pX, uint32_t pY)
{
  return m_Data[pX * m_N + pY];
}

uint32_t FragmentGraph::ReachMatrix::at(uint32_t pX, uint32_t pY) const
{
  return m_Data[pX * m_N + pY];
}

//===----------------------------------------------------------------------===//
// FragmentGraph
//===----------------------------------------------------------------------===//
FragmentGraph::FragmentGraph()
 : m_pMatrix(NULL), m_NumOfPNodes(0x0), m_NumOfRNodes(0x0), m_NumOfEdges(0x0)
{
  m_pPseudoNodeFactory = new NodeFactoryType();
  m_pRegularNodeFactory = new NodeFactoryType();
  m_pFragNodeMap = new FragHashTableType(256);
  m_pSymNodeMap = new SymHashTableType(256);
}

FragmentGraph::~FragmentGraph()
{
  delete m_pPseudoNodeFactory;
  delete m_pRegularNodeFactory;
  delete m_pFragNodeMap;
}

FGNode* FragmentGraph::getNode(const Fragment& pFrag)
{
  FragHashTableType::iterator entry = m_pFragNodeMap->find(&pFrag);
  if (entry == m_pFragNodeMap->end())
    return NULL;
  return entry.getEntry()->value();
}

const FGNode* FragmentGraph::getNode(const Fragment& pFrag) const
{
  FragHashTableType::iterator entry = m_pFragNodeMap->find(&pFrag);
  if (entry == m_pFragNodeMap->end())
    return NULL;
  return entry.getEntry()->value();
}

FGNode* FragmentGraph::getNode(const ResolveInfo& pSym)
{
  SymHashTableType::iterator entry = m_pSymNodeMap->find(&pSym);
  if (entry == m_pSymNodeMap->end())
    return NULL;
  return entry.getEntry()->value();
}

const FGNode* FragmentGraph::getNode(const ResolveInfo& pSym) const
{
  SymHashTableType::iterator entry = m_pSymNodeMap->find(&pSym);
  if (entry == m_pSymNodeMap->end())
    return NULL;
  return entry.getEntry()->value();
}

FGNode* FragmentGraph::producePseudoNode()
{
  FGNode* result = m_pPseudoNodeFactory->allocate();
  new (result) FGNode(m_NumOfPNodes + m_NumOfRNodes);
  ++m_NumOfPNodes;
  return result;
}

FGNode* FragmentGraph::produceRegularNode()
{
  FGNode* result = m_pRegularNodeFactory->allocate();
  new (result) FGNode(m_NumOfPNodes + m_NumOfRNodes);
  ++m_NumOfRNodes;
  return result;
}

bool FragmentGraph::setNodeSlots(Module& pModule)
{
  // symbols are the slots of nodes, push the symbols into the corresponding
  // nodes.

  // Traverse all defined symbols, including global and local symbols, to add
  // symbols into the corresponding nodes
  Module::SymbolTable& sym_tab = pModule.getSymbolTable();
  SymbolCategory::iterator sym_it, sym_end = sym_tab.end();
  for (sym_it = sym_tab.begin(); sym_it != sym_end; ++sym_it) {
    // only the defined symbols with FragmnentRef can form a slot. The defined
    // symbol with no FragmentRef such as ABS symbol should be skipped
    LDSymbol* sym = *sym_it;
    if (!sym->resolveInfo()->isDefine() ||
        !sym->hasFragRef())
      continue;

    // FIXME: judge by getNode() is NULL or not
    LDFileFormat::Kind sect_kind =
                       sym->fragRef()->frag()->getParent()->getSection().kind();
    if (sect_kind != LDFileFormat::Regular &&
        sect_kind != LDFileFormat::BSS)
      continue;

    FGNode* node = getNode(*sym->fragRef()->frag());
    assert(NULL != node);
    node->addSlot(sym->resolveInfo());
  }

  return true;
}

bool FragmentGraph::createRegularEdges(Module& pModule)
{
  // The reference between nodes are presented by the relocations. Set the
  // reachability matrix to present the connection

  // Traverse all input relocations to set connection
  Module::obj_iterator input, inEnd = pModule.obj_end();
  for (input = pModule.obj_begin(); input != inEnd; ++input) {
    LDContext::sect_iterator rs, rsEnd = (*input)->context()->relocSectEnd();
    for (rs = (*input)->context()->relocSectBegin(); rs != rsEnd; ++rs) {
      // bypass the discarded relocations
      // 1. its section kind is changed to Ignore. (The target section is a
      // discarded group section.)
      // 2. it has no reloc data. (All symbols in the input relocs are in the
      // discarded group sections)
      if (LDFileFormat::Ignore == (*rs)->kind() || !(*rs)->hasRelocData())
        continue;
      RelocData::iterator reloc_it, rEnd = (*rs)->getRelocData()->end();
      for (reloc_it = (*rs)->getRelocData()->begin(); reloc_it != rEnd;
                                                                   ++reloc_it) {
        Relocation* reloc = llvm::cast<Relocation>(reloc_it);
        ResolveInfo* sym = reloc->symInfo();
        // only the target symbols defined in the input fragments can make the
        // connection
        if (NULL == sym)
          continue;
        if (!sym->isDefine() || !sym->outSymbol()->hasFragRef())
          continue;

        // only the relocation target places which defined in the concerned
        // sections can make the connection
        // FIXME: judge by getNode() is NULL or not
        LDFileFormat::Kind sect_kind =
                   reloc->targetRef().frag()->getParent()->getSection().kind();
        if (sect_kind != LDFileFormat::Regular &&
            sect_kind != LDFileFormat::BSS)
          continue;

        // only the target symbols defined in the concerned sections can make
        // the connection
        // FIXME: judge by getNode() is NULL or not
        sect_kind =
          sym->outSymbol()->fragRef()->frag()->getParent()->getSection().kind();
        if (sect_kind != LDFileFormat::Regular &&
            sect_kind != LDFileFormat::BSS)
          continue;

        connect(reloc, sym);
      }
    }
  }
  return true;
}

bool FragmentGraph::createPseudoEdges(Module& pModule)
{
  // the pseudo edges are the edges from pseudo nodes to regular nodes, which
  // present the reference from out-side world when building shared library

  // Traverse all pseudo relocations in the pseudo nodes to set the connection
  node_iterator node_it, node_end = m_pPseudoNodeFactory->end();
  for (node_it = m_pPseudoNodeFactory->begin(); node_it != node_end; ++node_it) {
    FGNode& node = *node_it;
    FGNode::signal_iterator sig_it, sig_end = node.signal_end();
    for (sig_it = node.signal_begin(); sig_it != sig_end; ++sig_it) {
      connect(node, (*sig_it)->symInfo());
    }
  }
  return true;
}

bool FragmentGraph::connect(Signal pSignal, Slot pSlot)
{
  FGNode* from = getNode(*pSignal->targetRef().frag());
  assert(NULL != from);

  FGNode* to = getNode(*pSlot->outSymbol()->fragRef()->frag());
  assert(NULL != to);

  // maintain edge counter
  if (0 == m_pMatrix->at(from->getIndex(), to->getIndex()))
    ++m_NumOfEdges;
  ++m_pMatrix->at(from->getIndex(), to->getIndex());
  return true;
}

bool FragmentGraph::connect(FGNode& pFrom, Slot pSlot)
{
  FGNode* to = getNode(*pSlot->outSymbol()->fragRef()->frag());
  assert(NULL != to);

  // maintain edge counter
  if (0 == m_pMatrix->at(pFrom.getIndex(), to->getIndex()))
    ++m_NumOfEdges;
  ++m_pMatrix->at(pFrom.getIndex(), to->getIndex());
  return true;
}

bool FragmentGraph::createPseudoNodes(Module& pModule)
{
  // when generating shared library, we need to create pseudo node for every
  // global defined symbols to present the fan-in of a regular node.

  // Traverse all global defined symbols to build the pseudo nodes.
  Module::SymbolTable& sym_tab = pModule.getSymbolTable();
  SymbolCategory::iterator sym_it, sym_end = sym_tab.dynamicEnd();
  for (sym_it = sym_tab.dynamicBegin(); sym_it != sym_end; ++sym_it) {
    ResolveInfo* sym = (*sym_it)->resolveInfo();
    if (!sym->isDefine() || !sym->outSymbol()->hasFragRef())
      continue;
    FGNode* node = producePseudoNode();
    // create the pseudo relocation to present the fan-out of the pseudo node
    Relocation* reloc = Relocation::Create();
    reloc->setSymInfo(sym);

    // set the signal of the pseudo node
    node->addSignal(reloc);

    // maintain the map for symbol to pseudo node
    SymHashTableType::entry_type* entry = 0;
    bool exist = false;
    entry = m_pSymNodeMap->insert(sym, exist);
    entry->setValue(node);

  }
  return true;
}

bool FragmentGraph::createRegularNodes(Module& pModule)
{
  // Traverse all sections to build the Nodes. We build nodes only for Regular,
  // and BSS
  Module::iterator sect_it, sect_end = pModule.end();
  for (sect_it = pModule.begin(); sect_it != sect_end; ++sect_it) {
    LDSection* section = *sect_it;
    SectionData* sect_data = NULL;

    if (LDFileFormat::Regular != section->kind() &&
        LDFileFormat::BSS != section->kind())
      continue;

    sect_data = section->getSectionData();
    if (NULL == sect_data)
      continue;

    // Traverse all fragments in the sections, create Nodes and push the
    // fragments into Nodes. Each Region or Fillment fragment belongs to a
    // unique Node. The corresponding Align fragments and Null fragments belong
    // to the same Node as the Region or Fillment fragment.
    SectionData::iterator frag_it  = sect_data->begin();
    SectionData::iterator frag_end = sect_data->end();
    if (frag_it == frag_end)
      continue;

    int cur_stat = 0;
    int last_stat = 0;
    // FIXME:
    // To prevent some cases that we add the redundant NULL or Align fragments
    // and lead a Region/Fillment fragment has more than one NULL or Align
    // fragment. We should put all of them into the same Node.
    static int stat_matrix[3][3] = {{0, 1, 1},
                                    {0, 1, 1},
                                    {0, 0, 0}};

    FragHashTableType::entry_type* entry = 0;
    bool exist = false;

    FGNode* node = produceRegularNode();
    Fragment* frag = NULL;

    frag = &(*frag_it);
    cur_stat = get_state(frag->getKind());

    node->addFragment(frag);
    // maintain the fragment to Node map
    entry = m_pFragNodeMap->insert(frag, exist);
    entry->setValue(node);
    ++frag_it;

    while (frag_it != frag_end) {
      last_stat = cur_stat;
      frag = &(*frag_it);

      cur_stat = get_state(frag->getKind());

      if (stat_matrix[cur_stat][last_stat]) {
        node = produceRegularNode();
      }
      node->addFragment(frag);
      // maintain the fragment to Node map
      entry = m_pFragNodeMap->insert(frag, exist);
      entry->setValue(node);

      ++frag_it;
    }
  }
  return true;
}

void FragmentGraph::initMatrix()
{
  m_pMatrix = new ReachMatrix(m_NumOfPNodes + m_NumOfRNodes);
}

bool FragmentGraph::getEdges(FGNode& pNode, EdgeListType& pEdges)
{
  // Traverse all regular nodes to find the connection to pNode
  node_iterator it, itEnd = m_pRegularNodeFactory->end();
  for (it = m_pRegularNodeFactory->begin(); it != itEnd; ++it) {
    FGNode& node_to = *it;
    uint32_t weight = m_pMatrix->at(pNode.getIndex(), node_to.getIndex());
    if (weight > 0) {
      // build an Edge
      pEdges.push_back(FGEdge(pNode, node_to, weight));
    }
  }

  return true;
}

bool FragmentGraph::construct(const LinkerConfig& pConfig, Module& pModule)
{
  // create nodes - traverse all fragments to create the regular nodes, and
  // then traverse all global defined symbols to create pseudo nodes
  if (!createRegularNodes(pModule))
    return false;
  if (!createPseudoNodes(pModule))
    return false;

  // after all nodes created, we know the number of the nodes and then can
  // create the reachability matrix
  initMatrix();

  // set slots - traverse all symbols to set the slots of regular nodes
  if(!setNodeSlots(pModule))
    return false;

  // connect edges - traverse all relocations to set the edges
  if(!createRegularEdges(pModule))
    return false;
  if(!createPseudoEdges(pModule))
    return false;

  return true;
}

