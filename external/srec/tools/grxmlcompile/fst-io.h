#ifndef __FST_IO_H__
#define __FST_IO_H__

// fst-io.h 
// This is a copy of the OPENFST SDK application sample files ...
// except for the main functions ifdef'ed out
// 2007, 2008 Nuance Communications
//
// print-main.h compile-main.h
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//
// \file
// Classes and functions to compile a binary Fst from textual input.
// Includes helper function for fstcompile.cc that templates the main
// on the arc type to support multiple and extensible arc types.

#include <fstream>
#include <sstream>

#include "fst/lib/fst.h"
#include "fst/lib/fstlib.h"
#include "fst/lib/fst-decl.h"
#include "fst/lib/vector-fst.h"
#include "fst/lib/arcsort.h"
#include "fst/lib/invert.h"

namespace fst {
  
  template <class A> class FstPrinter {
  public:
    typedef A Arc;
    typedef typename A::StateId StateId;
    typedef typename A::Label Label;
    typedef typename A::Weight Weight;
    
    FstPrinter(const Fst<A> &fst,
	       const SymbolTable *isyms,
	       const SymbolTable *osyms,
	       const SymbolTable *ssyms,
	       bool accep)
      : fst_(fst), isyms_(isyms), osyms_(osyms), ssyms_(ssyms),
      accep_(accep && fst.Properties(kAcceptor, true)), ostrm_(0) {}
    
    // Print Fst to an output strm
    void Print(ostream *ostrm, const string &dest) {
      ostrm_ = ostrm;
      dest_ = dest;
      StateId start = fst_.Start();
      if (start == kNoStateId)
	return;
      // initial state first
      PrintState(start);
      for (StateIterator< Fst<A> > siter(fst_);
	   !siter.Done();
	   siter.Next()) {
	StateId s = siter.Value();
	if (s != start)
	  PrintState(s);
      }
    }
    
  private:
    // Maximum line length in text file.
    static const int kLineLen = 8096;
    
    void PrintId(int64 id, const SymbolTable *syms,
		 const char *name) const {
      if (syms) {
	string symbol = syms->Find(id);
	if (symbol == "") {
	  LOG(ERROR) << "FstPrinter: Integer " << id
		     << " is not mapped to any textual symbol"
		     << ", symbol table = " << syms->Name()
		     << ", destination = " << dest_;
	  exit(1);
	}
	*ostrm_ << symbol;
      } else {
	*ostrm_ << id;
      }
    }
    
    void PrintStateId(StateId s) const {
      PrintId(s, ssyms_, "state ID");
    }
    
    void PrintILabel(Label l) const {
      PrintId(l, isyms_, "arc input label");
    }
    
    void PrintOLabel(Label l) const {
      PrintId(l, osyms_, "arc output label");
    }
    
    void PrintState(StateId s) const {
      bool output = false;
      for (ArcIterator< Fst<A> > aiter(fst_, s);
	   !aiter.Done();
	   aiter.Next()) {
	Arc arc = aiter.Value();
	PrintStateId(s);
	*ostrm_ << "\t";
	PrintStateId(arc.nextstate);
	*ostrm_ << "\t";
	PrintILabel(arc.ilabel);
	if (!accep_) {
	  *ostrm_ << "\t";
	  PrintOLabel(arc.olabel);
	}
	if (arc.weight != Weight::One())
	  *ostrm_ << "\t" << arc.weight;
	*ostrm_ << "\n";
	output = true;
      }
      Weight final = fst_.Final(s);
      if (final != Weight::Zero() || !output) {
	PrintStateId(s);
	if (final != Weight::One()) {
	  *ostrm_ << "\t" << final;
	}
	*ostrm_ << "\n";
      }
    }
    
    const Fst<A> &fst_;
    const SymbolTable *isyms_;     // ilabel symbol table
    const SymbolTable *osyms_;     // olabel symbol table
    const SymbolTable *ssyms_;     // slabel symbol table
    bool accep_;                   // print as acceptor when possible
    ostream *ostrm_;                // binary FST destination
    string dest_;                  // binary FST destination name
    DISALLOW_EVIL_CONSTRUCTORS(FstPrinter);
  };
  
#if 0
  // Main function for fstprint templated on the arc type.
  template <class Arc>
    int PrintMain(int argc, char **argv, istream &istrm,
		  const FstReadOptions &opts) {
    Fst<Arc> *fst = Fst<Arc>::Read(istrm, opts);
    if (!fst) return 1;
    
    string dest = "standard output";
    ostream *ostrm = &std::cout;
    if (argc == 3) {
      dest = argv[2];
      ostrm = new ofstream(argv[2]);
      if (!*ostrm) {
	LOG(ERROR) << argv[0] << ": Open failed, file = " << argv[2];
	return 0;
      }
    }
    ostrm->precision(9);
    
    const SymbolTable *isyms = 0, *osyms = 0, *ssyms = 0;
    
    if (!FLAGS_isymbols.empty() && !FLAGS_numeric) {
      isyms = SymbolTable::ReadText(FLAGS_isymbols);
      if (!isyms) exit(1);
    }
    
    if (!FLAGS_osymbols.empty() && !FLAGS_numeric) {
      osyms = SymbolTable::ReadText(FLAGS_osymbols);
      if (!osyms) exit(1);
    }
    
    if (!FLAGS_ssymbols.empty() && !FLAGS_numeric) {
      ssyms = SymbolTable::ReadText(FLAGS_ssymbols);
      if (!ssyms) exit(1);
    }
    
    if (!isyms && !FLAGS_numeric)
      isyms = fst->InputSymbols();
    if (!osyms && !FLAGS_numeric)
      osyms = fst->OutputSymbols();
    
    FstPrinter<Arc> fstprinter(*fst, isyms, osyms, ssyms, FLAGS_acceptor);
    fstprinter.Print(ostrm, dest);
    
    if (isyms && !FLAGS_save_isymbols.empty())
      isyms->WriteText(FLAGS_save_isymbols);
    
    if (osyms && !FLAGS_save_osymbols.empty())
      osyms->WriteText(FLAGS_save_osymbols);
    
    if (ostrm != &std::cout)
      delete ostrm;
    return 0;
  }
#endif
  
  
  template <class A> class FstReader {
  public:
    typedef A Arc;
    typedef typename A::StateId StateId;
    typedef typename A::Label Label;
    typedef typename A::Weight Weight;
    
    FstReader(istream &istrm, const string &source,
	      const SymbolTable *isyms, const SymbolTable *osyms,
	      const SymbolTable *ssyms, bool accep, bool ikeep,
	      bool okeep, bool nkeep)
      : nline_(0), source_(source),
      isyms_(isyms), osyms_(osyms), ssyms_(ssyms),
      nstates_(0), keep_state_numbering_(nkeep) {
      char line[kLineLen];
      while (istrm.getline(line, kLineLen)) {
	++nline_;
	vector<char *> col;
	SplitToVector(line, "\n\t ", &col, true);
	if (col.size() == 0 || col[0][0] == '\0')  // empty line
	  continue;
	if (col.size() > 5 ||
	    col.size() > 4 && accep ||
	    col.size() == 3 && !accep) {
	  LOG(ERROR) << "FstReader: Bad number of columns, source = " << source_
		     << ", line = " << nline_;
	  exit(1);
	}
	StateId s = StrToStateId(col[0]);
	while (s >= fst_.NumStates())
	  fst_.AddState();
	if (nline_ == 1)
	  fst_.SetStart(s);
	
	Arc arc;
	StateId d = s;
	switch (col.size()) {
	case 1:
	  fst_.SetFinal(s, Weight::One());
	  break;
	case 2:
	  fst_.SetFinal(s, StrToWeight(col[1], true));
	  break;
	case 3:
	  arc.nextstate = d = StrToStateId(col[1]);
	  arc.ilabel = StrToILabel(col[2]);
	  arc.olabel = arc.ilabel;
	  arc.weight = Weight::One();
	  fst_.AddArc(s, arc);
	  break;
	case 4:
	  arc.nextstate = d = StrToStateId(col[1]);
	  arc.ilabel = StrToILabel(col[2]);
	  if (accep) {
	    arc.olabel = arc.ilabel;
	    arc.weight = StrToWeight(col[3], false);
	  } else {
	    arc.olabel = StrToOLabel(col[3]);
	    arc.weight = Weight::One();
	  }
	  fst_.AddArc(s, arc);
	  break;
	case 5:
	  arc.nextstate = d = StrToStateId(col[1]);
	  arc.ilabel = StrToILabel(col[2]);
	  arc.olabel = StrToOLabel(col[3]);
	  arc.weight = StrToWeight(col[4], false);
	  fst_.AddArc(s, arc);
	}
	while (d >= fst_.NumStates())
	  fst_.AddState();
      }
      if (ikeep)
	fst_.SetInputSymbols(isyms);
      if (okeep)
	fst_.SetOutputSymbols(osyms);
    }
    
    const VectorFst<A> &Fst() const { return fst_; }
    
  private:
    // Maximum line length in text file.
    static const int kLineLen = 8096;
    
    int64 StrToId(const char *s, const SymbolTable *syms,
		  const char *name) const {
      int64 n;
      
      if (syms) {
	n = syms->Find(s);
	if (n < 0) {
	  LOG(ERROR) << "FstReader: Symbol \"" << s
		     << "\" is not mapped to any integer " << name
		     << ", symbol table = " << syms->Name()
		     << ", source = " << source_ << ", line = " << nline_;
	  exit(1);
	}
      } else {
	char *p;
	n = strtoll(s, &p, 10);
	if (p < s + strlen(s) || n < 0) {
	  LOG(ERROR) << "FstReader: Bad " << name << " integer = \"" << s
		     << "\", source = " << source_ << ", line = " << nline_;
	  exit(1);
	}
      }
      return n;
    }
    
    StateId StrToStateId(const char *s) {
      StateId n = StrToId(s, ssyms_, "state ID");
      
      if (keep_state_numbering_)
	return n;
      
      // remap state IDs to make dense set
      typename hash_map<StateId, StateId>::const_iterator it = states_.find(n);
      if (it == states_.end()) {
	states_[n] = nstates_;
	return nstates_++;
      } else {
	return it->second;
      }
    }
    
    StateId StrToILabel(const char *s) const {
      return StrToId(s, isyms_, "arc ilabel");
    }
    
    StateId StrToOLabel(const char *s) const {
      return StrToId(s, osyms_, "arc olabel");
    }
    
    Weight StrToWeight(const char *s, bool allow_zero) const {
      Weight w;
      istringstream strm(s);
      strm >> w;
      if (strm.fail() || !allow_zero && w == Weight::Zero()) {
	LOG(ERROR) << "FstReader: Bad weight = \"" << s
		   << "\", source = " << source_ << ", line = " << nline_;
	exit(1);
      }
      return w;
    }
    
    VectorFst<A> fst_;
    size_t nline_;
    string source_;                      // text FST source name
    const SymbolTable *isyms_;           // ilabel symbol table
    const SymbolTable *osyms_;           // olabel symbol table
    const SymbolTable *ssyms_;           // slabel symbol table
    hash_map<StateId, StateId> states_;  // state ID map
    StateId nstates_;                    // number of seen states
    bool keep_state_numbering_;
    DISALLOW_EVIL_CONSTRUCTORS(FstReader);
  };
  
#if 0
  // Main function for fstcompile templated on the arc type.  Last two
  // arguments unneeded since fstcompile passes the arc type as a flag
  // unlike the other mains, which infer the arc type from an input Fst.
  template <class Arc>
    int CompileMain(int argc, char **argv, istream& /* strm */,
		    const FstReadOptions & /* opts */) {
    char *ifilename = "standard input";
    istream *istrm = &std::cin;
    if (argc > 1 && strcmp(argv[1], "-") != 0) {
      ifilename = argv[1];
      istrm = new ifstream(ifilename);
      if (!*istrm) {
	LOG(ERROR) << argv[0] << ": Open failed, file = " << ifilename;
	return 1;
      }
    }
    const SymbolTable *isyms = 0, *osyms = 0, *ssyms = 0;
    
    if (!FLAGS_isymbols.empty()) {
      isyms = SymbolTable::ReadText(FLAGS_isymbols);
      if (!isyms) exit(1);
    }
    
    if (!FLAGS_osymbols.empty()) {
      osyms = SymbolTable::ReadText(FLAGS_osymbols);
      if (!osyms) exit(1);
    }
    
    if (!FLAGS_ssymbols.empty()) {
      ssyms = SymbolTable::ReadText(FLAGS_ssymbols);
      if (!ssyms) exit(1);
    }
    
    FstReader<Arc> fstreader(*istrm, ifilename, isyms, osyms, ssyms,
			     FLAGS_acceptor, FLAGS_keep_isymbols,
			     FLAGS_keep_osymbols, FLAGS_keep_state_numbering);
    
    const Fst<Arc> *fst = &fstreader.Fst();
    if (FLAGS_fst_type != "vector") {
      fst = Convert<Arc>(*fst, FLAGS_fst_type);
      if (!fst) return 1;
    }
    fst->Write(argc > 2 ? argv[2] : "");
    if (istrm != &std::cin)
      delete istrm;
    return 0;
  }
#endif
  
}  // namespace fst

#endif /* __FST_IO_H__ */

