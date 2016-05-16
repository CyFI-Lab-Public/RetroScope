/* FILE:		sub_phon.cpp
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

#include <iostream>
#include <sstream>
#include <string>
#include <assert.h>

#define DEBUG           0

#include "sub_grph.h"
#include "grxmldoc.h"

void SubGraph::ExpandPhonemes ( GRXMLDoc &doc )
{
    int ii, wordId, phoneId, currId, newId, nextId, arcCount;
    Pronunciation pron;
    int pronCount;
    NUANArc *arcOne;
    std::string modelLabel, word;

    {
        std::stringstream ss;
        ss << SILENCE_CONTEXT;
        modelLabel= ss.str();
        silenceId = doc.addPhonemeToList(modelLabel);
    }
    {
        std::stringstream ss;
        ss << INTRA_SILENCE_CONTEXT;
        modelLabel= ss.str();
        intraId = doc.addPhonemeToList(modelLabel);
    }
    UpdateVertexCount (0);
    arcCount= numArc;
    for (ii= 0; ii < arcCount; ii++) {
        wordId= arc[ii]->GetInput();
        if (wordId >= 0) {
	    doc.findLabel(wordId, word );
            if (IsSlot (word)) {
	        // std::cout << "Found slot "<< word <<std::endl;
	        newId= NewVertexId();
	        arcOne= CreateArc (NONE_LABEL, NONE_LABEL, arc[ii]->GetFromId(), newId);
		arcOne->AssignCentre (NONE_LABEL);
	        nextId= NewVertexId();
	        //  special case
	        arcOne= CreateArc (-wordId, wordId, newId, nextId);
		arcOne->AssignCentre (NONE_LABEL);
	        // (void) CreateArc (-wordId, NONE_LABEL, arc[ii]->GetFromId(), newId);
	        arcOne= CreateArc (WB_LABEL, NONE_LABEL, nextId, arc[ii]->GetToId());
		arcOne->AssignCentre (NONE_LABEL);
	        // (void) CreateArc (WB_LABEL, wordId, newId, arc[ii]->GetToId());
            }
            else {
	        pron.clear();
	        pron.lookup( *(doc.getVocabulary()), word );
	        pronCount = pron.getPronCount();
	        for (int jj= 0; jj < pronCount; jj++) {
	            currId= arc[ii]->GetFromId();
	            int modelCount = pron.getPhonemeCount(jj);
	            for (int kk= 0; kk < modelCount; kk++) {
	                newId= NewVertexId();
	                pron.getPhoneme(jj, kk, modelLabel);
	                //std::cout << "ExpandPhonemes adding "<< modelLabel <<std::endl;
	                phoneId = doc.addPhonemeToList( modelLabel );
	                arcOne= CreateArc (phoneId, NONE_LABEL, currId, newId);
                        if (phoneId == intraId)
		            arcOne->AssignCentre (silenceId);
                        else
		            arcOne->AssignCentre (phoneId);
	                currId= newId;
	            }
	            arcOne= CreateArc (WB_LABEL, wordId, currId, arc[ii]->GetToId());
		    arcOne->AssignCentre (NONE_LABEL);
	        }
	        //  End of loop
            }
	    arc[ii]->AssignInput (DISCARD_LABEL);   //  Delete original arc
        }
    }
    RemoveDiscardedArcs ();

    for (ii= 0; ii < numArc; ii++) {
	arc[ii]->AssignLeft (NONE_LABEL);
	arc[ii]->AssignRight (NONE_LABEL);
    }

    SortLanguage ();

    return;
}

void SubGraph::AddLeftContexts ()
{
    int ii, rix, currId, leftC;

    SortLanguage();
    SortLanguageReverse();
    for (ii= 0; ii < numArc; ii++) {
        if (arc[ii]->GetInput() >= 0) {
            currId= arc[ii]->GetFromId();
            rix= FindToIndex (currId);
            if (rix >= 0) {
                leftC= arc[backwardList[rix]]->GetCentre();
                arc[ii]->AssignLeft(leftC);
            }
            else if (currId != startId)
                printf ("Shouldn't get here (L) %d\n", currId);
        }
        else
            arc[ii]->AssignLeft (NONE_LABEL);
    }
    return;
}

void SubGraph::AddRightContexts ()
{
    int ii, rix, currId, rightC;

    SortLanguage();
    SortLanguageReverse();
    for (ii= 0; ii < numArc; ii++) {
        if (arc[ii]->GetInput() >= 0) {
            currId= arc[ii]->GetToId();
            rix= FindFromIndex (currId);
            if (rix >= 0) {
                rightC= arc[forwardList[rix]]->GetCentre();
                arc[ii]->AssignRight (rightC);
            }
            else
                printf ("Shouldn't get here (R) %d\n", currId);
        }
        else
            arc[ii]->AssignRight (NONE_LABEL);
    }
    return;
}

void SubGraph::ExpandToHMMs ( GRXMLDoc &doc )
{
    int ii, currId, newId, arcCount, left, right, centre;
    int modelCount;
    NUANArc *arcOne;

    UpdateVertexCount (0);
    arcCount= numArc;
    for (ii= 0; ii < arcCount; ii++) {
        std::vector<int> modelSequence;
	if (arc[ii]->GetInput() >= 0) {      //  i.e. proper phoneme
            centre= arc[ii]->GetCentre();
	    left= arc[ii]->GetLeft();
	    right= arc[ii]->GetRight();
#if DEBUG
            std::cout << "HMM PIC:" << left <<" " << centre <<" " << right << std::endl;
#endif
	    doc.getHMMSequence (centre, left, right, modelSequence);
	    modelCount = modelSequence.size();
#if DEBUG
            std::cout << "HMM: " << centre << " number of HMMs = " << modelCount <<std::endl;
#endif
	    if (modelCount >= 0) {
		currId= arc[ii]->GetFromId();
		for (int jj= 0; jj < modelCount; jj++) {
                    if (jj == (modelCount - 1))
                        newId= arc[ii]->GetToId();
                    else
		        newId= NewVertexId();
		    arcOne= CreateArc (modelSequence[jj], NONE_LABEL, currId, newId);
		    arcOne->AssignCentre (arc[ii]->GetInput());
		    arcOne->AssignLeft (arc[ii]->GetLeft());
		    arcOne->AssignRight (arc[ii]->GetRight());
#if DEBUG
                    std::cout << "HMM phoneme: " << modelSequence[jj] << " ";
#endif
		    currId= newId;
		}
#if DEBUG
                std::cout << " centre " << arc[ii]->GetInput() << std::endl;
#endif
                arc[ii]->AssignInput (DISCARD_LABEL);       //  Delete original arc
            }
        }
    }
    RemoveDiscardedArcs ();

    SortLanguage ();

    return;
}

void SubGraph::ExpandIntraWordSilence ( GRXMLDoc &doc )
{
    int ii, fix, bix, firstId, newId, modelCount, followCount, currId, count;
    int left, centre, right;
    NUANArc *arcOne;

    SortLanguage();
    SortLanguageReverse();

#if DEBUG
    std::cout << "Intra sil search " << intraId << std::endl;
#endif
    count= numArc;
    for (ii= 0; ii < count; ii++) {
        if (arc[ii]->GetCentre() == intraId) {
#if DEBUG
            std::cout << "Intra sil: " << arc[ii]->GetFromId() << " " << arc[ii]->GetToId() << std::endl;
#endif

            fix= FindToIndex (arc[ii]->GetFromId());
            if (fix < 0)
                return;
            while (fix < sortRevNum
             && arc[backwardList[fix]]->GetToId() == arc[ii]->GetFromId()) {
		//  left triphone
                newId= NewVertexId();
		left= arc[backwardList[fix]]->GetLeft();
		centre= arc[ii]->GetLeft();
		right= arc[ii]->GetRight();
#if DEBUG
                std::cout << "HMM PIC:" << left <<" " << centre <<" " << right << std::endl;
#endif
                std::vector<int> modelSequence;
	        doc.getHMMSequence (centre, left, right, modelSequence);
	        modelCount = modelSequence.size();
#if DEBUG
                std::cout << "HMM: " << centre << " number of HMMs = " << modelCount <<std::endl;
#endif
	        if (modelCount >= 0) {
		    currId= arc[backwardList[fix]]->GetFromId();
		    for (int jj= 0; jj < modelCount; jj++) {
		        newId= NewVertexId();
		        arcOne= CreateArc (modelSequence[jj],
			    arc[backwardList[fix]]->GetOutput(), currId, newId);
		        arcOne->AssignCentre (centre);
#if DEBUG
                        std::cout << "HMM phoneme: " << modelSequence[jj] << " ";
#endif
		        currId= newId;
		    }
#if DEBUG
                    std::cout << " " << centre << std::endl;
#endif
		}
		firstId= newId;

		//  right block
                bix= FindFromIndex (arc[ii]->GetToId());
                if (bix < 0)
                    return;
                while (bix < sortNum
                 && arc[forwardList[bix]]->GetFromId() == arc[ii]->GetToId()) {
                    fix++;
		    //  right triphone
		    left= arc[ii]->GetLeft();
		    centre= arc[ii]->GetRight();
		    right= arc[forwardList[bix]]->GetRight();

#if DEBUG
                    std::cout << "HMM PIC:" << left <<" " << centre <<" " << right << std::endl;
#endif
                    std::vector<int> followSequence;
	            doc.getHMMSequence (centre, left, right, followSequence);
	            followCount = followSequence.size();
#if DEBUG
                    std::cout << "HMM: " << centre << " number of HMMs = " << followCount <<std::endl;
#endif

	            if (followCount >= 0) {
		        currId= firstId;
		        for (int jj= 0; jj < followCount; jj++) {
                            if (jj == (followCount - 1))
                                newId= arc[forwardList[bix]]->GetToId();
                            else
		                newId= NewVertexId();
		            arcOne= CreateArc (followSequence[jj],
				arc[forwardList[bix]]->GetOutput(), currId, newId);
		            arcOne->AssignCentre (centre);
#if DEBUG
                            std::cout << "HMM phoneme: " << followSequence[jj] << " ";
#endif
		            currId= newId;
		        }
#if DEBUG
                        std::cout << " " << centre << std::endl;
#endif
		    }
                    bix++;
                }
		fix++;
            }
            // arc[ii]->AssignInput (silenceId);
        }
    }
    return;
}

void SubGraph::ShiftOutputsToLeft ()
{
    UpdateVertexCount (0);
    SortLanguage();
    SortLanguageReverse();
    ReverseMarkArcs();
    MarkNodesByOutputAndClearArcs();
    return;
}

void SubGraph::ReverseMarkArcs ()
{
    int ii;

    for (ii= 0; ii < numArc; ii++)
        if (arc[ii]->GetInput() == WB_LABEL)
            ReverseMarkOutput (arc[ii]->GetFromId(), startId, arc[ii]->GetOutput());
    return;
}

void SubGraph::ReverseMarkOutput (int currId, int initialId, int outId)
{
    int rix;

    rix= FindToIndex (currId);
    if (rix < 0)
        return;
    while (rix < sortRevNum && arc[backwardList[rix]]->GetToId() == currId) {
        if (arc[backwardList[rix]]->GetOutput() != DISCARD_LABEL    //  not resolved yet
         && arc[backwardList[rix]]->GetInput() >= 0) { // excludes word boundary
            if (arc[backwardList[rix]]->GetOutput() == NONE_LABEL)
                arc[backwardList[rix]]->AssignOutput (outId);
            else if (outId != arc[backwardList[rix]]->GetOutput())
                arc[backwardList[rix]]->AssignOutput(DISCARD_LABEL);
            ReverseMarkOutput (arc[backwardList[rix]]->GetFromId(), initialId, outId);
        }
        rix++;
    }
    return;
}

void SubGraph::MarkNodesByOutputAndClearArcs ()
{
    int ii, currId, rix;

    int *nodeList= new int [numVertex];
    for (ii= 0; ii < numVertex; ii++)
        nodeList[ii]= NONE_LABEL;

    //  Associate outputs with destination node
    for (ii= 0; ii < numArc; ii++) {
        currId= arc[ii]->GetToId();
        if (currId >= 0) {
	    if (arc[ii]->GetInput() == WB_LABEL)
                nodeList[currId]= DISCARD_LABEL;
            else if (nodeList[currId] != DISCARD_LABEL) {
                if (nodeList[currId] == NONE_LABEL)
                    nodeList[currId]= arc[ii]->GetOutput();
                else if (nodeList[currId] != arc[ii]->GetOutput())
                    nodeList[currId]= DISCARD_LABEL;
            }
        }
    }

    //  Now discard all arcs other than those emanating from unique assignments
    for (ii= 0; ii < numArc; ii++) {
        currId= arc[ii]->GetFromId();
        if (nodeList[currId] >= 0 && arc[ii]->GetOutput() >= 0) // unique ones
            arc[ii]->AssignOutput(DISCARD_LABEL);
    }

    //  Finally, special case for intra-word silence
    for (ii= 0; ii < numArc; ii++) {
	if (arc[ii]->GetOutput() >= 0 && arc[ii]->GetCentre() == intraId) {
            currId= arc[ii]->GetToId();
#if DEBUG
            std::cout << "Intra silence: " << currId << " " << arc[ii]->GetFromId() << std::endl;
#endif
            rix= FindFromIndex (currId);
            if (rix < 0)
                continue;
            while (rix < sortNum && arc[forwardList[rix]]->GetFromId() == currId) {
                assert (arc[forwardList[rix]]->GetOutput() == DISCARD_LABEL);
                arc[forwardList[rix]]->AssignOutput(arc[ii]->GetOutput());
		rix++;
	    }
            arc[ii]->AssignOutput(DISCARD_LABEL);
        }
    }

    delete [] nodeList;
    return;
}

void SubGraph::FinalProcessing (GRXMLDoc &doc)
{
    ExpandWordBoundaries (doc);
    AddInitialFinalSilences (doc);
    return;
}

void SubGraph::ExpandWordBoundaries (GRXMLDoc &doc)
{
    int ii, newId, count;
    NUANArc  *arcOne;

    count= numArc;
    for (ii= 0; ii < count; ii++) {
        std::vector<int> modelSequence;
        doc.getHMMSequence (silenceId, -1, -1, modelSequence);
        if (arc[ii]->GetInput() == WB_LABEL) {
            newId= NewVertexId();
            // (void) CreateArc (NONE_LABEL, NONE_LABEL, arc[ii]->GetFromId(), newId);
            // arcOne= CreateArc (modelSequence[0], NONE_LABEL, arc[ii]->GetFromId(), newId);
            arcOne= CreateArc (modelSequence[0], NONE_LABEL, arc[ii]->GetFromId(), newId);
	    arcOne->AssignCentre (silenceId);
            (void) CreateArc (WB_LABEL, arc[ii]->GetOutput(), newId, arc[ii]->GetToId());
            // arc[ii]->AssignInput (DISCARD_LABEL);
        }
    }
    return;
}

void SubGraph::AddInitialFinalSilences (GRXMLDoc &doc)
{
    int ii, rix, newId, intId, count;
    NUANArc *arcOne;

    SortLanguage();
    newId= NewVertexId();
    rix= FindFromIndex (startId);
    if (rix < 0)
        return;
    while (rix < sortNum && arc[forwardList[rix]]->GetFromId() == startId) {
        arc[forwardList[rix]]->AssignFromId (newId);
        rix++;
    }
    std::vector<int> modelSequence;
    doc.getHMMSequence (silenceId, -1, -1, modelSequence);
    intId= NewVertexId();
    arcOne= CreateArc (modelSequence[0], INITIAL_LABEL, startId, intId);
    arcOne->AssignCentre (silenceId);
    (void) CreateArc (WB_LABEL, NONE_LABEL, intId, newId);

    count= numArc;
    newId= NewVertexId();
    for (ii= 0; ii < count; ii++) {
        if (arc[ii]->GetInput() == TERMINAL_LABEL) {
            arc[ii]->AssignInput (modelSequence[0]);
            arc[ii]->AssignCentre (silenceId);
            arc[ii]->AssignOutput (FINAL_LABEL);
            arc[ii]->AssignToId (newId);
        }
    }
    (void) CreateArc (TERMINAL_LABEL, TERMINAL_LABEL, newId, newId);

    return;
}
