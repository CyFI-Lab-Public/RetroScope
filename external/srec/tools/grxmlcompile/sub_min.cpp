/* FILE:		sub_min.cpp
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
#include <string>
#include <assert.h>
#include <cstdio>

#include "sub_grph.h"

#define SYMBOL_COMPARE(x,y) (arc[x]->CompareSymbol(arc[y]))
#define COMPARE(x,y) (arc[x]->Compare(arc[y]))

//  Minimization to facilitate moving output symbols - mainly for phoneme networks
//      Check partial merge
//      Move the output symbol if only one node

/*
static int checkEntry (int *itemList, int count, int item);

static int checkEntry (int *itemList, int count, int item)
{
    for (int ii= 0; ii < count; ii++)
        if (item == itemList[ii])
            return ii;
    return -1;
}
*/

void SubGraph::ViewNode (int currId)
{
    int  rix;

    printf ("Node: %d\n", currId);
    rix= FindFromIndex (currId);
    if (rix < 0)
        return;
    while (rix < numArc && arc[forwardList[rix]]->GetFromId() == currId) {
        arc[forwardList[rix]]->Print();
        rix++;
    }
    return;
}


bool SubGraph::EquivalenceTestForward (int firstId, int secondId, int *equivMap)
{
    int  fix, six, fnxt, snxt, tof, tos, count;

    assert (firstId != secondId);
    fix= FindFromIndex (firstId);
    six= FindFromIndex (secondId);
    if (fix < 0 || six < 0)
        return false;

    count= 0;
    do {

        //  Move to next valid transitions
        while (fix < numArc && arc[forwardList[fix]]->GetFromId() == firstId
            && arc[forwardList[fix]]->GetToId() == DISCARD_LABEL)
            fix++;
        if (fix < numArc && arc[forwardList[fix]]->GetFromId() == firstId)
            fnxt= arc[forwardList[fix]]->GetFromId();
        else
            fnxt= -1;

        while (six < numArc && arc[forwardList[six]]->GetFromId() == secondId
            && arc[forwardList[six]]->GetToId() == DISCARD_LABEL)
            six++;
        if (six < numArc && arc[forwardList[six]]->GetFromId() == secondId)
            snxt= arc[forwardList[six]]->GetFromId();
        else
            snxt= -1;

        if (fnxt != firstId && snxt != secondId)
            return true;
        else if (fnxt != firstId || snxt != secondId)
            return false;

	tos= arc[forwardList[six]]->GetToId();
	tof= arc[forwardList[fix]]->GetToId();
        // printf ("Debug inner %d %d, %d %d\n", tof, tos, equivMap[tof], equivMap[tos]);
	// if (tof >= 0)		bogus assert
	    // assert (tof == equivMap[tof]);
	// if (tos >= 0)
	    // assert (tos == equivMap[tos]);

        //  Test
        if (!arc[forwardList[fix]]->HasSameLabels (arc[forwardList[six]])
	    || tos != tof)
            return false;
	count++;

        fix++;
        six++;

    }  while (fnxt >= 0 && snxt >= 0);

    return false;
}

void SubGraph::IdentifyEquivalence (int *depthMap, int *equivMap)
{
    int ii, jj, dd, maxDepth, count, itCnt;

    maxDepth= 0;
    for (ii= 0; ii < numVertex; ii++) {
        if (maxDepth < depthMap[ii])
            maxDepth= depthMap[ii];
    }

    itCnt= 0;
    do {
        count= 0;
        for (dd= 0; dd <= maxDepth; dd++)
            for (ii= 0; ii < numVertex; ii++)
                if (depthMap[ii] == dd && ii == equivMap[ii]) {
                    CheckForChangeAndResort (ii, equivMap);
                    for (jj= ii+1; jj < numVertex; jj++)
                        if (depthMap[jj] == dd && jj == equivMap[jj]) {
                            // Equivalence test
                            CheckForChangeAndResort (jj, equivMap);
                            // printf ("Try %d %d, ", ii, jj);
                            if (EquivalenceTestForward (ii, jj, equivMap)) {
                                equivMap[jj]= ii;
                                // printf ("Merged %d %d\n", ii, jj);
                                count++;
                            }
                        }
                }

        itCnt++;
        // printf ("Total %d mergers\n", count);
    } while (count > 0 && itCnt < MAXITS);

    return;
}

void SubGraph::CheckForChangeAndResort (int currId, int *mapList)
{
    int  rix, rixBegin, nextId;
    bool needSort;

    needSort= false;
    rixBegin= rix= FindFromIndex (currId);
    if (rix < 0)
        return;
    while (rix < numArc && arc[forwardList[rix]]->GetFromId() == currId) {
        nextId= arc[forwardList[rix]]->GetToId();
        if (nextId >= 0 && mapList[nextId] != nextId) {
            needSort= true;
            arc[forwardList[rix]]->AssignToId(mapList[nextId]);
        }
        rix++;
    }

    //  Resort
    if (needSort)
        RemoveDuplicatesAtNode (rixBegin, rix);

    return;
}

void SubGraph::DeterminizeAtVertex (int baseId, DetCache *cache)
{
    int  fix, six, firstId, secondId, vertEnd;

    fix= FindFromIndex (baseId);
    if (fix < 0)
        return;

    // Remove duplicates
    vertEnd= fix;
    six= -1;
    while (vertEnd < sortNum && arc[forwardList[vertEnd]]->GetFromId() == baseId) {
        vertEnd++;
    }
    vertEnd++;

    //  Iteration over first node
    firstId= -1;
    while (fix < sortNum && arc[forwardList[fix]]->GetFromId() == baseId) {

        //  Iterator for firstId
        while (fix < sortNum && arc[forwardList[fix]]->GetFromId() == baseId
            && (arc[forwardList[fix]]->GetToId() == firstId
		    || arc[forwardList[fix]]->GetInput() == TERMINAL_LABEL
		    || arc[forwardList[fix]]->GetInput() == DISCARD_LABEL))
            fix++;

	// Terminal Condition
	if (fix >= sortNum || arc[forwardList[fix]]->GetFromId() != baseId)
	    break;
	else
	    firstId= arc[forwardList[fix]]->GetToId();

        //  Iteration over second node
	six= fix;
        secondId= firstId;
        while (six < sortNum && arc[forwardList[six]]->GetFromId() == baseId) {

	        //  Iterator for secondId
            while (six < sortNum && arc[forwardList[six]]->GetFromId() == baseId
                && (arc[forwardList[six]]->GetToId() == secondId
		        || arc[forwardList[six]]->GetInput() == TERMINAL_LABEL
		        || arc[forwardList[six]]->GetInput() == DISCARD_LABEL))
		        six++;

	        //  Terminal condition
	        if (six >= sortNum || arc[forwardList[six]]->GetFromId() != baseId)
		    break;
	        else
		    secondId= arc[forwardList[six]]->GetToId();

            //  Now we have both Ids worked out
	        assert (firstId >= 0);
	        assert (secondId >= 0);
                assert (arc[forwardList[fix]]->GetInput() != DISCARD_LABEL);
                assert (arc[forwardList[six]]->GetInput() != DISCARD_LABEL);

            if (PairwiseDeterminize (baseId, firstId, fix, secondId, six, cache) > 0) {
                SortLanguageAtSortIndices (fix, vertEnd);

                //  Are we done with the first node?
                while (fix < sortNum && arc[forwardList[fix]]->GetFromId() == baseId
                   && arc[forwardList[fix]]->GetInput() == DISCARD_LABEL)
                    fix++;

	            //  Terminal condition
	            if (fix >= sortNum || arc[forwardList[fix]]->GetFromId() != baseId
                 || arc[forwardList[fix]]->GetToId() != firstId)
		            break;
            }
        }
    }
    return;
}

int SubGraph::PairwiseDeterminize (int baseId, int firstId, int fixStart, int secondId,
                                   int sixStart, DetCache *cache)
{
    int  fix, six, fmiss, smiss, nmatch, symTst, newId;
    bool isCached;

    fix= fixStart;
    six= sixStart;

    assert (arc[forwardList[fix]]->GetInput() != DISCARD_LABEL);
    assert (arc[forwardList[six]]->GetInput() != DISCARD_LABEL);

    //  Count
    isCached= false;
    fmiss= smiss= nmatch= 0;
    newId= -1;
    while (six < sortNum && fix < sortNum) {

        assert (arc[forwardList[fix]]->GetInput() != DISCARD_LABEL);
        assert (arc[forwardList[six]]->GetInput() != DISCARD_LABEL);

        symTst= SYMBOL_COMPARE (forwardList[fix], forwardList[six]);
        if (symTst == 0) {
            if (newId == -1) {
                newId= cache->QueryEntry (firstId, secondId);
		if (newId < 0)
                    newId= NewVertexId ();
                else
                    isCached= true;
		// printf ("Forming %d with %d and %d at %d\n", newId, firstId, secondId, baseId);
            }

            //  Assign second to new Vertex
            arc[forwardList[six]]->AssignToId (newId);

            //  Assign first to DISCARD Index
            arc[forwardList[fix]]->AssignInput (DISCARD_LABEL);

            //  Increment to next
            do {
                fix++;
            } while (fix < sortNum && arc[forwardList[fix]]->GetInput() == DISCARD_LABEL);
            if (fix >= sortNum || arc[forwardList[fix]]->GetFromId() != baseId
                || arc[forwardList[fix]]->GetToId() != firstId)
                fix= sortNum;

            do {
                six++;
            } while (six < sortNum && arc[forwardList[six]]->GetInput() == DISCARD_LABEL);
            if (six >= sortNum || arc[forwardList[six]]->GetFromId() != baseId
                || arc[forwardList[six]]->GetToId() != secondId)
                six= sortNum;

            nmatch++;
        }
        else if (symTst < 0) {
            do {
                fix++;
            } while (fix < sortNum && arc[forwardList[fix]]->GetInput() == DISCARD_LABEL);
            if (fix >= sortNum || arc[forwardList[fix]]->GetFromId() != baseId
                || arc[forwardList[fix]]->GetToId() != firstId)
                fix= sortNum;
            fmiss++;
        }
        else if (symTst > 0) {
            do {
                six++;
            } while (six < sortNum && arc[forwardList[six]]->GetInput() == DISCARD_LABEL);
            if (six >= sortNum || arc[forwardList[six]]->GetFromId() != baseId
                || arc[forwardList[six]]->GetToId() != secondId)
                six= sortNum;
            smiss++;
        }
    }

    // SortLanguageAtSortIndices (fixStart, fix);

    if (newId >= 0) {
        if (isCached == false) {
            // numLast= numArc;
            MergeVertices (newId, firstId, secondId);
            cache->AddEntry (newId, firstId, secondId);
            // UpdateVisitationCache (numLast);
        }
        assert (nmatch > 0);
    }

    //  Update fan-in count
    if (nmatch > 0) {
        // printf ("Merging %d %d to create %d\n", firstId, secondId, newId);
        for (int ii= 0; ii < nmatch; ii++) {
            // IncNodeProperty (newId);
            IncVisitationCache (newId);
            DecVisitationCache (firstId);
            DecVisitationCache (secondId);
        }
    }
    return nmatch;
}

void SubGraph::MergeVertices (int newId, int firstId, int secondId)
{
    int fix, six, symTst, numStart;
    NUANArc *arcOne;

    numStart= numArc;
    fix= FindFromIndex (firstId);
    six= FindFromIndex (secondId);
    if (fix < 0 || six < 0) {
        if (fix >= 0)
            while (fix < sortNum && arc[forwardList[fix]]->GetFromId() == firstId) {
                if (arc[forwardList[fix]]->GetInput() != DISCARD_LABEL)
                    InheritArc (arc[forwardList[fix]], newId);
                fix++;
            }
        else if (six >= 0)
            while (six < sortNum && arc[forwardList[six]]->GetFromId() == secondId) {
                if (arc[forwardList[six]]->GetInput() != DISCARD_LABEL)
                    InheritArc (arc[forwardList[six]], newId);
                six++;
            }
    }
    else {
        while (six < sortNum && fix < sortNum) {

            symTst= SYMBOL_COMPARE (forwardList[fix], forwardList[six]);

            if (symTst == 0) {
                if (arc[forwardList[fix]]->GetToId() == firstId
                 && arc[forwardList[six]]->GetToId() == secondId) {
                    arcOne= InheritArc (arc[forwardList[fix]], newId);
                    arcOne->AssignToId (newId);
                }
                else if (arc[forwardList[fix]]->GetToId()
                 == arc[forwardList[six]]->GetToId()) {
                    InheritArc (arc[forwardList[fix]], newId);
		}
                else {
                    InheritArc (arc[forwardList[fix]], newId);
                    InheritArc (arc[forwardList[six]], newId);
                }

                //  Increment to next
                do {
                    fix++;
                } while (fix < sortNum && arc[forwardList[fix]]->GetInput() == DISCARD_LABEL);
                if (fix >= sortNum || arc[forwardList[fix]]->GetFromId() != firstId
                    || arc[forwardList[fix]]->GetFromId() != firstId)
                    fix= sortNum;

                do {
                    six++;
                } while (six < sortNum && arc[forwardList[six]]->GetInput() == DISCARD_LABEL);
                if (six >= sortNum || arc[forwardList[six]]->GetFromId() != secondId)
                    six= sortNum;
            }
            else if (symTst < 0) {
                InheritArc (arc[forwardList[fix]], newId);
                do {
                    fix++;
                } while (fix < sortNum && arc[forwardList[fix]]->GetInput() == DISCARD_LABEL);
                if (fix >= sortNum || arc[forwardList[fix]]->GetFromId() != firstId
                    || arc[forwardList[fix]]->GetFromId() != firstId)
                    fix= sortNum;
            }
            else if (symTst > 0) {
                InheritArc (arc[forwardList[six]], newId);
                do {
                    six++;
                } while (six < sortNum && arc[forwardList[six]]->GetInput() == DISCARD_LABEL);
                if (six >= sortNum || arc[forwardList[six]]->GetFromId() != secondId
                    || arc[forwardList[six]]->GetFromId() != secondId)
                    six= sortNum;
            }
        }

        while (fix < sortNum && arc[forwardList[fix]]->GetFromId() == firstId) {
            if (arc[forwardList[fix]]->GetInput() != DISCARD_LABEL)
                InheritArc (arc[forwardList[fix]], newId);
            fix++;
        }

        while (six < sortNum && arc[forwardList[six]]->GetFromId() == secondId) {
            if (arc[forwardList[six]]->GetInput() != DISCARD_LABEL)
                InheritArc (arc[forwardList[six]], newId);
            six++;
        }
    }

    //  Update the sort list
    UpdateSortForLanguage ();
    // RemoveDuplicatesAtNode (numStart, numArc);
    // ViewNode (newId);
    assert (CheckSort());

    // printf ("Merging %d %d to create %d\n", firstId, secondId, newId);

    return;
}

void SubGraph::SetupVisitationCache ()
{
    int ii;

    CreateNodeProperty ();
    for (ii= 0; ii < numArc; ii++)
        if (arc[ii]->GetInput() != DISCARD_LABEL && arc[ii]->GetToId() >= 0)
            IncNodeProperty (arc[ii]->GetToId());
    return;
}

void SubGraph::ClearVisitationCache ()
{
    DeleteNodeProperty ();
    return;
}

void SubGraph::UpdateVisitationCache (int startNo)
{
    int ii;

    for (ii= startNo; ii < numArc; ii++)
        if (arc[ii]->GetInput() != DISCARD_LABEL && arc[ii]->GetToId() >= 0) {
            IncNodeProperty (arc[ii]->GetToId());
            // std::cout << " (" << arc[ii]->GetToId() << " " << QueryNodeProperty (arc[ii]->GetToId()) << ") ";
        }
    // std::cout << std::endl;
    return;
}

void SubGraph::DecVisitationCache (int currId)
{
    int rix;

    DecNodeProperty (currId);
    // std::cout << " [" << currId << " " << QueryNodeProperty (currId) << "] ";
    if (QueryNodeProperty (currId) <= 0) {
        // std::cout << " [" << currId << "] ";
        rix= FindFromIndex (currId);
        if (rix < 0)
            return;
        while (rix < sortNum && arc[forwardList[rix]]->GetFromId() == currId) {
            if (arc[forwardList[rix]]->GetInput() != DISCARD_LABEL
                && arc[forwardList[rix]]->GetToId() >= 0) {
                if (arc[forwardList[rix]]->GetFromId() == arc[forwardList[rix]]->GetToId())
                    DecNodeProperty (currId);
                else if (QueryNodeProperty (arc[forwardList[rix]]->GetToId()) > 0)
                    DecVisitationCache (arc[forwardList[rix]]->GetToId());
            }
            rix++;
        }
    }
    return;
}

void SubGraph::IncVisitationCache (int currId)
{
    int rix;

    if (QueryNodeProperty (currId) <= 0) {
        IncNodeProperty (currId);
        // std::cout << " (" << currId << ") ";
        rix= FindFromIndex (currId);
        if (rix < 0)
            return;
        while (rix < sortNum && arc[forwardList[rix]]->GetFromId() == currId) {
            if (arc[forwardList[rix]]->GetInput() != DISCARD_LABEL
                && arc[forwardList[rix]]->GetToId() >= 0) {
                // IncNodeProperty (arc[forwardList[rix]]->GetToId());
		// if (currId != arc[forwardList[rix]]->GetToId())
                    IncVisitationCache (arc[forwardList[rix]]->GetToId());
            }
            rix++;
        }
    }
    else
        IncNodeProperty (currId);
    // std::cout << " (" << currId << " " << QueryNodeProperty (currId) << ") ";
    return;
}

int SubGraph::VisitationConsistencyCheck ()
{
    int ii, failCount;

    int *nodeCount= new int [numVertex];

    UpdateVertexCount (0);

    // std::cout << "Count: ";
    for (ii= 0; ii <numVertex; ii++) {
        // std::cout << ii << " (" << vertexProp[ii] << ") ";
        nodeCount[ii]= 0;
    }
    // std::cout << std::endl;
    for (ii= 0; ii <numArc; ii++)
        if (arc[ii]->GetInput() != DISCARD_LABEL && arc[ii]->GetToId() >= 0
            && (vertexProp[arc[ii]->GetFromId()] > 0 || arc[ii]->GetFromId() == startId))
            nodeCount[arc[ii]->GetToId()]++;
    failCount= 0;
    // std::cout << "Failure list: ";
    for (ii= 0; ii <numVertex; ii++)
        if (vertexProp[ii] != nodeCount[ii]) {
            // std::cout << ii << " (" << vertexProp[ii] << " " << nodeCount[ii] << ") ";
            failCount++;
        }
    // std::cout << std::endl;

    // return failCount;
    delete [] nodeCount;

    return 0;
}
