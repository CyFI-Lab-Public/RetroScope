/* FILE:		netw_dump.cpp
 *  DATE MODIFIED:	31-Aug-07
 *  DESCRIPTION:	Part of the  SREC graph compiler project source files.
 *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                               *
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the 'License');          *
 *  you may not use this file except in compliance with the License.         *
 *                                                                           *
 *  You may obtain a copy of the License at                                  *
 *      http://www.apache.org/licenses/LICENSE-2.0                           *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an 'AS IS' BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *---------------------------------------------------------------------------*/

#include <fstream>
#include <iostream>
#include <sstream>

#include <string>

#include "netw_arc.h"
#include "sub_grph.h"

#include "grxmldoc.h"

void SubGraph::RemapForSortedOutput ( GRXMLDoc &p_Doc )
{
    int origIndex, sortedIndex;

    for (int ii= 0; ii < numArc; ii++) {
	origIndex= arc[ii]->GetInput();
	if (origIndex >= 0 && p_Doc.findSortedLabelIndex (origIndex, sortedIndex ))
	    arc[ii]->AssignInput (sortedIndex);
    }
    return;
}

void SubGraph::WriteForwardGraphFile( std::string & fileName, GRXMLDoc &p_Doc )
{
    // Creates file of forward graph  - the {name}.G.txt file
    int loc;
    std::string inLabel;
    std::ofstream outfile;
    std::string label;
    std::string separator = "\t";
    std::string Eps = "eps";

    RemapForSortedOutput (p_Doc);
    SortLanguageForMin ();
    p_Doc.sortLabels();
    outfile.open ( fileName.c_str() );
    for (int ii= 0; ii < numArc; ii++) {
	inLabel="";
        loc = forwardList[ii];
	switch ( arc[loc]->inputLabel ) {
	    case TERMINAL_LABEL:    //  Terminal transition
		outfile << arc[loc]->fromId << std::endl;
		break;
	    case NONE_LABEL:        //  Null transition
		outfile << arc[loc]->fromId << separator << arc[loc]->toId << separator << Eps << std::endl;
		break;
	    case TAG_LABEL:         //  Tag transition
	    case WB_LABEL:          //  Word boundary transition
	    case BEGINSCOPE_LABEL:  //  Start of scope
	    case ENDSCOPE_LABEL:    //  End of scope
	    case BEGINRULE_LABEL:   //  Start of rule
	    case ENDRULE_LABEL:     //  End of rule
	    case DISCARD_LABEL:     //  Discard (internal)
		break;
	    default:
		{
		    // if (!p_Doc.findLabel( arc[loc]->inputLabel, inLabel ) ) {
		    if (!p_Doc.findSortedLabel( arc[loc]->inputLabel, inLabel ) ) {
			std::stringstream ss;
			ss << arc[loc]->inputLabel;
			inLabel = ss.str();
		    }
		    outfile << arc[loc]->fromId << separator << arc[loc]->toId  << separator << inLabel.c_str() << std::endl;
		}
		break;
	} // switch

    }
    outfile.close();

    SortLanguage ();
    return;
}


void SubGraph::WriteForwardGraphWithSemantic ( std::string & fileName, GRXMLDoc &p_Doc )
{
    int loc;
    std::string inLabel, outLabel;
    std::string tag;
    std::string Eps = "eps";
    std::string OpenBrace = "{";
    std::string CloseBrace = "}";
    std::string Separator ="\t";

    std::ofstream outfile;
    std::string label;
    std::string separator = "\t";
    outfile.open ( fileName.c_str() );

    RemapForSortedOutput (p_Doc);
    SortLanguageForMin ();
    p_Doc.sortLabels();
    for ( int ii= 0; ii < numArc; ii++ ) {
        loc= forwardList[ii];
	inLabel = "";
	switch ( arc[loc]->inputLabel ) {
	    case BEGINRULE_LABEL:   //  Start of rule
		inLabel = Eps;
		outLabel = OpenBrace;
		break;
	    case ENDRULE_LABEL:     //  End of rule
		{
		    inLabel = Eps;
		    if (!p_Doc.findRule( arc[loc]->outputLabel, outLabel ) ) {
			std::stringstream ss;
			ss << arc[loc]->outputLabel;
			outLabel = "(" + ss.str() + ")";
		    }
		    outLabel = outLabel + CloseBrace;
		}
		break;
	    case NONE_LABEL:        //  Null transition
		inLabel = Eps;
                outLabel= Eps;
                break;
	    case TAG_LABEL:         //  Tag transition
		inLabel = Eps;
                {
		    std::stringstream ss;
		    ss << SCRIPT_LABEL_PREFIX << arc[loc]->outputLabel;
		    outLabel = ss.str();
                }
                break;
	    case TERMINAL_LABEL:    //  Terminal transition
		outLabel = "";
		break;
	    case WB_LABEL:          //  Word boundary transition
	    case BEGINSCOPE_LABEL:  //  Start of scope
	    case ENDSCOPE_LABEL:    //  End of scope
	    case DISCARD_LABEL:     //  Discard (internal)
		break;
	    default:
                //  Input label
		// if (!p_Doc.findLabel( arc[loc]->inputLabel, inLabel ) ) {
		if (!p_Doc.findSortedLabel( arc[loc]->inputLabel, inLabel ) ) {
		    inLabel = arc[loc]->inputLabel;
		}

                //  Output label
                if (arc[loc]->outputLabel == -1)
                    outLabel= Eps;
                else {
		    std::stringstream ss;
		    ss << SCRIPT_LABEL_PREFIX << arc[loc]->outputLabel;
		    outLabel = ss.str();
                }
		break;
	}
	if ( outLabel.empty() )
	    outfile << arc[loc]->fromId << std::endl;
	else {
	    outfile << arc[loc]->fromId << Separator << arc[loc]->toId  << Separator << inLabel.c_str() << Separator << outLabel.c_str()  << std::endl;
	    p_Doc.addOLabelToOList( outLabel);
	}
    }
    outfile.close();

    return;
}

void SubGraph::WriteHMMGraphFile( std::string & fileName, GRXMLDoc &p_Doc )
{
    // Creates file of forward graph  - the {name}.G.txt file
    int loc;
    std::string inLabel;
    std::string outLabel;
    std::string phLabel;
    std::string metaname;
    std::string wtw;
    std::ofstream outfile;
    std::string label;
    std::string separator = "\t";
    std::string Eps = "eps";
    std::string Pau = "-pau-";
    std::string Pau2 = "-pau2-";
    bool bRes;

    metaname = "word_penalty";
    bRes = p_Doc.findMeta(metaname, wtw);

    outfile.open ( fileName.c_str() );
    for (int ii= 0; ii < numArc; ii++) {
	inLabel="";
        loc = forwardList[ii];
	switch ( arc[loc]->inputLabel ) {
	    case TERMINAL_LABEL:    //  Terminal transition
		outfile << arc[loc]->fromId << std::endl;
		break;
	    case NONE_LABEL:        //  Null transition
		if (arc[loc]->outputLabel >= 0) {
		    if (!p_Doc.findLabel( arc[loc]->outputLabel, outLabel ) ) {
			std::stringstream ss;
			ss << arc[loc]->outputLabel;
			outLabel = ss.str();
		    }
		    outfile << arc[loc]->fromId << separator << arc[loc]->toId  << separator << Eps << separator << outLabel.c_str() << std::endl;
		}
		else
		    outfile << arc[loc]->fromId << separator << arc[loc]->toId  << separator << Eps << separator << Eps << std::endl;
		break;
	    case WB_LABEL:          //  Word boundary transition
		if (arc[loc]->outputLabel >= 0) {
		    if (!p_Doc.findLabel( arc[loc]->outputLabel, outLabel ) ) {
			std::stringstream ss;
			ss << arc[loc]->outputLabel;
			outLabel = ss.str();
		    }
		    outfile << arc[loc]->fromId << separator << arc[loc]->toId  << separator << ".wb" << separator << outLabel.c_str() << std::endl;
		}
		else
		    outfile << arc[loc]->fromId << separator << arc[loc]->toId  << separator << ".wb" << separator << Eps << std::endl;
		break;
	    case TAG_LABEL:         //  Tag transition
	    case BEGINSCOPE_LABEL:  //  Start of scope
	    case ENDSCOPE_LABEL:    //  End of scope
	    case BEGINRULE_LABEL:   //  Start of rule
	    case ENDRULE_LABEL:     //  End of rule
	    case DISCARD_LABEL:     //  Discard (internal)
		break;
	    default:
                 {
		    if (arc[loc]->outputLabel >= 0) {
		        if (!p_Doc.findLabel( arc[loc]->outputLabel, outLabel ) ) {
			    std::stringstream ss;
			    ss << arc[loc]->outputLabel;
			    outLabel = ss.str();
		        }
		    }
		    else if (arc[loc]->outputLabel == INITIAL_LABEL)
			outLabel= Pau;
		    else if (arc[loc]->outputLabel == FINAL_LABEL)
			outLabel= Pau2;
		    else
		        outLabel= Eps;
                }
		break;
	} // switch

    }
    outfile.close();
    return;
}

void SubGraph::WritePhonemeGraphFile( std::string & fileName, GRXMLDoc &p_Doc )
{
    // Creates file of forward graph  - the {name}.G.txt file
    int loc;
    std::string inLabel;
    std::string outLabel;
    std::ofstream outfile;
    std::string label;
    std::string separator = "\t";
    std::string Eps = "eps";

    outfile.open ( fileName.c_str() );
    for (int ii= 0; ii < numArc; ii++) {
	inLabel="";
        loc = forwardList[ii];
	switch ( arc[loc]->inputLabel ) {
	    case TERMINAL_LABEL:    //  Terminal transition
		outfile << arc[loc]->fromId << std::endl;
		break;
	    case NONE_LABEL:        //  Null transition
		if (arc[loc]->outputLabel >= 0) {
		    if (!p_Doc.findLabel( arc[loc]->outputLabel, outLabel ) ) {
			std::stringstream ss;
			ss << arc[loc]->outputLabel;
			outLabel = ss.str();
		    }
		    outfile << arc[loc]->fromId << separator << arc[loc]->toId  << separator << Eps << separator << outLabel.c_str() << std::endl;
		}
		else
		    outfile << arc[loc]->fromId << separator << arc[loc]->toId  << separator << Eps << separator << Eps << std::endl;
		break;
	    case WB_LABEL:          //  Word boundary transition
		if (arc[loc]->outputLabel >= 0) {
		    if (!p_Doc.findLabel( arc[loc]->outputLabel, outLabel ) ) {
			std::stringstream ss;
			ss << arc[loc]->outputLabel;
			outLabel = ss.str();
		    }
		    outfile << arc[loc]->fromId << separator << arc[loc]->toId  << separator << ".wb" << separator << outLabel.c_str() << std::endl;
		}
		else
		    outfile << arc[loc]->fromId << separator << arc[loc]->toId  << separator << ".wb" << separator << Eps << std::endl;
		break;
	    case TAG_LABEL:         //  Tag transition
	    case BEGINSCOPE_LABEL:  //  Start of scope
	    case ENDSCOPE_LABEL:    //  End of scope
	    case BEGINRULE_LABEL:   //  Start of rule
	    case ENDRULE_LABEL:     //  End of rule
	    case DISCARD_LABEL:     //  Discard (internal)
		break;
	    default:
	        if ( arc[loc]->inputLabel >= 0) {
		  
		}
                else {
		    //  Note negative index
		    if (!p_Doc.findLabel( -arc[loc]->inputLabel, inLabel ) ) {
			std::stringstream ss;
			ss << arc[loc]->inputLabel;
			inLabel = ss.str();
		    }
		    outfile << arc[loc]->fromId << separator << arc[loc]->toId  << separator << inLabel.c_str() << separator << Eps << std::endl;
                }
		break;
	} // switch

    }
        outfile.close();

    return;
}


void SubGraph::PrintWithLabels( GRXMLDoc &p_Doc )
{
    int loc;
    std::string inLabel, outLabel;
    std::string label;

    printf ("Graph %s (%d %d)\n", title, startId, lastId);
    for (int ii= 0; ii < numArc; ii++) {
        loc= forwardList[ii];
	if (!p_Doc.findLabel( arc[loc]->inputLabel, inLabel ) ) {
	    inLabel = arc[loc]->inputLabel;
	    //std::stringstream  ss;
	    //ss << arc[loc]->inputLabel;
	    //inLabel = ss.str();
	}
	if (!p_Doc.findTag( arc[loc]->outputLabel, outLabel ) ) {
	    outLabel = arc[loc]->outputLabel;
	    //std::stringstream  ss;
	    //ss << arc[loc]->outputLabel;
	    //outLabel = ss.str();
	}
	std::cout << arc[loc]->fromId <<" " << arc[loc]->toId  << " "  << inLabel.c_str() <<" " << outLabel.c_str()  << std::endl;
    }

    return;
}

void NUANArc::Dump(GRXMLDoc &p_Doc )
{
    // I need  a handle to the grxml doc object in order to access labels
    printf ("%d %d %d %d\n", fromId, toId, inputLabel, outputLabel);
    return;
}



