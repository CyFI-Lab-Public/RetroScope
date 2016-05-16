//===- MCLinker.cpp -------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
//
// This file implements the MCLinker class.
//
//===----------------------------------------------------------------------===//
#include <mcld/CodeGen/MCLinker.h>

#include <mcld/Module.h>
#include <mcld/LinkerConfig.h>
#include <mcld/InputTree.h>
#include <mcld/Linker.h>
#include <mcld/IRBuilder.h>
#include <mcld/MC/InputBuilder.h>
#include <mcld/MC/FileAction.h>
#include <mcld/MC/CommandAction.h>
#include <mcld/Object/ObjectLinker.h>
#include <mcld/Support/CommandLine.h>
#include <mcld/Support/FileSystem.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/Support/FileHandle.h>
#include <mcld/Support/raw_ostream.h>
#include <mcld/Support/MemoryArea.h>

#include <llvm/IR/Module.h>
#include <llvm/Support/CommandLine.h>

#include <algorithm>
#include <vector>
#include <string>

using namespace mcld;
using namespace llvm;

char MCLinker::m_ID = 0;

//===----------------------------------------------------------------------===//
// Help Functions
//===----------------------------------------------------------------------===//
static inline bool CompareAction(const InputAction* X, const InputAction* Y)
{
  return (X->position() < Y->position());
}

//===----------------------------------------------------------------------===//
// Positional Options
// There are four kinds of positional options:
//   1. Inputs, object files, such as /tmp/XXXX.o
//   2. Namespecs, short names of libraries. A namespec may refer to an archive
//      or a shared library. For example, -lm.
//   3. Attributes of inputs. Attributes describe inputs appears after them.
//      For example, --as-needed and --whole-archive.
//   4. Groups. A Group is a set of archives. Linkers repeatedly read archives
//      in groups until there is no new undefined symbols.
//   5. Bitcode. Bitcode is a kind of object files. MCLinker compiles it to
//      object file first, then link it as a object file. (Bitcode is recorded
//      in BitcodeOption, not be read by LLVM Command Line library.)
//===----------------------------------------------------------------------===//
// Inputs
//===----------------------------------------------------------------------===//
static cl::list<mcld::sys::fs::Path>
ArgInputObjectFiles(cl::Positional,
                    cl::desc("[input object files]"),
                    cl::ZeroOrMore);

//===----------------------------------------------------------------------===//
// Namespecs
//===----------------------------------------------------------------------===//
static cl::list<std::string>
ArgNameSpecList("l",
            cl::ZeroOrMore,
            cl::desc("Add the archive or object file specified by namespec to "
                     "the list of files to link."),
            cl::value_desc("namespec"),
            cl::Prefix);

static cl::alias
ArgNameSpecListAlias("library",
                 cl::desc("alias for -l"),
                 cl::aliasopt(ArgNameSpecList));

//===----------------------------------------------------------------------===//
// Attributes
//===----------------------------------------------------------------------===//
static cl::list<bool>
ArgWholeArchiveList("whole-archive",
               cl::ValueDisallowed,
               cl::desc("For each archive mentioned on the command line after "
                        "the --whole-archive option, include all object files "
                        "in the archive."));

static cl::list<bool>
ArgNoWholeArchiveList("no-whole-archive",
               cl::ValueDisallowed,
               cl::desc("Turn off the effect of the --whole-archive option for "
                        "subsequent archive files."));

static cl::list<bool>
ArgAsNeededList("as-needed",
               cl::ValueDisallowed,
               cl::desc("This option affects ELF DT_NEEDED tags for dynamic "
                        "libraries mentioned on the command line after the "
                        "--as-needed option."));

static cl::list<bool>
ArgNoAsNeededList("no-as-needed",
               cl::ValueDisallowed,
               cl::desc("Turn off the effect of the --as-needed option for "
                        "subsequent dynamic libraries"));

static cl::list<bool>
ArgAddNeededList("add-needed",
                cl::ValueDisallowed,
                cl::desc("--add-needed causes DT_NEEDED tags are always "
                         "emitted for those libraries from DT_NEEDED tags. "
                         "This is the default behavior."));

static cl::list<bool>
ArgNoAddNeededList("no-add-needed",
                cl::ValueDisallowed,
                cl::desc("--no-add-needed causes DT_NEEDED tags will never be "
                         "emitted for those libraries from DT_NEEDED tags"));

static cl::list<bool>
ArgBDynamicList("Bdynamic",
                cl::ValueDisallowed,
                cl::desc("Link against dynamic library"));

static cl::alias
ArgBDynamicListAlias1("dy",
                cl::desc("alias for --Bdynamic"),
                cl::aliasopt(ArgBDynamicList));

static cl::alias
ArgBDynamicListAlias2("call_shared",
                cl::desc("alias for --Bdynamic"),
                cl::aliasopt(ArgBDynamicList));

static cl::list<bool>
ArgBStaticList("Bstatic",
                cl::ValueDisallowed,
                cl::desc("Link against static library"));

static cl::alias
ArgBStaticListAlias1("dn",
                cl::desc("alias for --Bstatic"),
                cl::aliasopt(ArgBStaticList));

static cl::alias
ArgBStaticListAlias2("static",
                cl::desc("alias for --Bstatic"),
                cl::aliasopt(ArgBStaticList));

static cl::alias
ArgBStaticListAlias3("non_shared",
                cl::desc("alias for --Bstatic"),
                cl::aliasopt(ArgBStaticList));

//===----------------------------------------------------------------------===//
// Groups
//===----------------------------------------------------------------------===//
static cl::list<bool>
ArgStartGroupList("start-group",
                  cl::ValueDisallowed,
                  cl::desc("start to record a group of archives"));

static cl::alias
ArgStartGroupListAlias("(",
                       cl::desc("alias for --start-group"),
                       cl::aliasopt(ArgStartGroupList));

static cl::list<bool>
ArgEndGroupList("end-group",
                cl::ValueDisallowed,
                cl::desc("stop recording a group of archives"));

static cl::alias
ArgEndGroupListAlias(")",
                     cl::desc("alias for --end-group"),
                     cl::aliasopt(ArgEndGroupList));

//===----------------------------------------------------------------------===//
// MCLinker
//===----------------------------------------------------------------------===//
MCLinker::MCLinker(LinkerConfig& pConfig,
                   mcld::Module& pModule,
                   MemoryArea& pOutput)
  : MachineFunctionPass(m_ID),
    m_Config(pConfig),
    m_Module(pModule),
    m_Output(pOutput),
    m_pBuilder(NULL),
    m_pLinker(NULL) {
}

MCLinker::~MCLinker()
{
  delete m_pLinker;
  delete m_pBuilder;
}

bool MCLinker::doInitialization(llvm::Module &pM)
{
  // Now, all input arguments are prepared well, send it into ObjectLinker
  m_pLinker = new Linker();

  if (!m_pLinker->emulate(m_Module.getScript(), m_Config))
    return false;

  m_pBuilder = new IRBuilder(m_Module, m_Config);

  initializeInputTree(*m_pBuilder);

  return true;
}

bool MCLinker::doFinalization(llvm::Module &pM)
{
  if (!m_pLinker->link(m_Module, *m_pBuilder))
    return true;

  if (!m_pLinker->emit(m_Output))
    return true;

  return false;
}

bool MCLinker::runOnMachineFunction(MachineFunction& pF)
{
  // basically, linkers do nothing during function is generated.
  return false;
}

void MCLinker::initializeInputTree(IRBuilder& pBuilder)
{
  if (0 == ArgInputObjectFiles.size() &&
      0 == ArgNameSpecList.size() &&
      !m_Config.bitcode().hasDefined()) {
    fatal(diag::err_no_inputs);
    return;
  }

  size_t num_actions = ArgInputObjectFiles.size() +
                       ArgNameSpecList.size() +
                       ArgWholeArchiveList.size() +
                       ArgNoWholeArchiveList.size() +
                       ArgAsNeededList.size() +
                       ArgNoAsNeededList.size() +
                       ArgAddNeededList.size() +
                       ArgNoAddNeededList.size() +
                       ArgBDynamicList.size() +
                       ArgBStaticList.size() +
                       ArgStartGroupList.size() +
                       ArgEndGroupList.size() +
                       1; // bitcode
  std::vector<InputAction*> actions;
  actions.reserve(num_actions);

  // -----  inputs  ----- //
  cl::list<mcld::sys::fs::Path>::iterator input, inBegin, inEnd;
  inBegin = ArgInputObjectFiles.begin();
  inEnd = ArgInputObjectFiles.end();
  for (input = inBegin; input != inEnd; ++input) {
    unsigned int pos = ArgInputObjectFiles.getPosition(input - inBegin);
    actions.push_back(new InputFileAction(pos, *input));
    actions.push_back(new ContextAction(pos));
    actions.push_back(new MemoryAreaAction(pos, FileHandle::ReadOnly));
  }

  // -----  namespecs  ----- //
  cl::list<std::string>::iterator namespec, nsBegin, nsEnd;
  nsBegin = ArgNameSpecList.begin();
  nsEnd = ArgNameSpecList.end();
  mcld::Module& module = pBuilder.getModule();
  for (namespec = nsBegin; namespec != nsEnd; ++namespec) {
    unsigned int pos = ArgNameSpecList.getPosition(namespec - nsBegin);
    actions.push_back(new NamespecAction(pos, *namespec,
                                         module.getScript().directories()));
    actions.push_back(new ContextAction(pos));
    actions.push_back(new MemoryAreaAction(pos, FileHandle::ReadOnly));
  }

  // -----  attributes  ----- //
  /// --whole-archive
  cl::list<bool>::iterator attr, attrBegin, attrEnd;
  attrBegin = ArgWholeArchiveList.begin();
  attrEnd   = ArgWholeArchiveList.end();
  for (attr = attrBegin; attr != attrEnd; ++attr) {
    unsigned int pos = ArgWholeArchiveList.getPosition(attr - attrBegin);
    actions.push_back(new WholeArchiveAction(pos));
  }

  /// --no-whole-archive
  attrBegin = ArgNoWholeArchiveList.begin();
  attrEnd   = ArgNoWholeArchiveList.end();
  for (attr = attrBegin; attr != attrEnd; ++attr) {
    unsigned int pos = ArgNoWholeArchiveList.getPosition(attr - attrBegin);
    actions.push_back(new NoWholeArchiveAction(pos));
  }

  /// --as-needed
  attrBegin = ArgAsNeededList.begin();
  attrEnd   = ArgAsNeededList.end();
  for (attr = attrBegin; attr != attrEnd; ++attr) {
    unsigned int pos = ArgAsNeededList.getPosition(attr - attrBegin);
    actions.push_back(new AsNeededAction(pos));
  }

  /// --no-as-needed
  attrBegin = ArgNoAsNeededList.begin();
  attrEnd   = ArgNoAsNeededList.end();
  for (attr = attrBegin; attr != attrEnd; ++attr) {
    unsigned int pos = ArgNoAsNeededList.getPosition(attr - attrBegin);
    actions.push_back(new NoAsNeededAction(pos));
  }

  /// --add--needed
  attrBegin = ArgAddNeededList.begin();
  attrEnd   = ArgAddNeededList.end();
  for (attr = attrBegin; attr != attrEnd; ++attr) {
    unsigned int pos = ArgAddNeededList.getPosition(attr - attrBegin);
    actions.push_back(new AddNeededAction(pos));
  }

  /// --no-add--needed
  attrBegin = ArgNoAddNeededList.begin();
  attrEnd   = ArgNoAddNeededList.end();
  for (attr = attrBegin; attr != attrEnd; ++attr) {
    unsigned int pos = ArgNoAddNeededList.getPosition(attr - attrBegin);
    actions.push_back(new NoAddNeededAction(pos));
  }

  /// --Bdynamic
  attrBegin = ArgBDynamicList.begin();
  attrEnd   = ArgBDynamicList.end();
  for (attr = attrBegin; attr != attrEnd; ++attr) {
    unsigned int pos = ArgBDynamicList.getPosition(attr - attrBegin);
    actions.push_back(new BDynamicAction(pos));
  }

  /// --Bstatic
  attrBegin = ArgBStaticList.begin();
  attrEnd   = ArgBStaticList.end();
  for (attr = attrBegin; attr != attrEnd; ++attr) {
    unsigned int pos = ArgBStaticList.getPosition(attr - attrBegin);
    actions.push_back(new BStaticAction(pos));
  }

  // -----  groups  ----- //
  /// --start-group
  cl::list<bool>::iterator group, gsBegin, gsEnd;
  gsBegin = ArgStartGroupList.begin();
  gsEnd   = ArgStartGroupList.end();
  for (group = gsBegin; group != gsEnd; ++group) {
    unsigned int pos = ArgStartGroupList.getPosition(group - gsBegin);
    actions.push_back(new StartGroupAction(pos));
  }

  /// --end-group
  gsBegin = ArgEndGroupList.begin();
  gsEnd   = ArgEndGroupList.end();
  for (group = gsBegin; group != gsEnd; ++group) {
    unsigned int pos = ArgEndGroupList.getPosition(group - gsBegin);
    actions.push_back(new EndGroupAction(pos));
  }

  // -----  bitcode  ----- //
  if (m_Config.bitcode().hasDefined()) {
    actions.push_back(new BitcodeAction(m_Config.bitcode().getPosition(),
                                        m_Config.bitcode().getPath()));
  }

  // stable sort
  std::stable_sort(actions.begin(), actions.end(), CompareAction);

  // build up input tree
  std::vector<InputAction*>::iterator action, actionEnd = actions.end();
  for (action = actions.begin(); action != actionEnd; ++action) {
    (*action)->activate(pBuilder.getInputBuilder());
    delete *action;
  }

  if (pBuilder.getInputBuilder().isInGroup())
    report_fatal_error("no matched --start-group and --end-group");
}

