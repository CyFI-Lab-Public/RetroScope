/* FILE:		sub_grph.cpp
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


static int checkEntry (int *itemList, int count, int item);

static int checkEntry (int *itemList, int count, int item)
{
    for (int ii= 0; ii < count; ii++)
        if (item == itemList[ii])
            return ii;
    return -1;
}

bool IsSlot (std::string label)
{
        int count= label.size();
        int fPos= label.find_first_of ("___");
        int lPos= label.find_last_of ("___") + 1;
	// std::cout << label << " " << count << " " << fPos << " " << lPos << std::endl;
        if (fPos >= 0 && lPos == count)
            return true;
        else
            return false;
}


void SubGraph::pushScope ()
{
    opStack[popOp++]= lastScope;
    opStack[popOp++]= startId;
    opStack[popOp++]= endId;
    opStack[popOp++]= lastId;
    opStack[popOp++]= arg1;
    opStack[popOp++]= arg2;
    opStack[popOp++]= numArc;
    return;
}

void SubGraph::popScope ()
{
    prevStartId= startId;
    prevEndId= endId;
    arcLoc= opStack[--popOp];
    arg2= opStack[--popOp];
    arg1= opStack[--popOp];
    lastId= opStack[--popOp];
    endId= opStack[--popOp];
    startId= opStack[--popOp];
    lastScope= opStack[--popOp];
    return;
}

void SubGraph::BeginScope (int scopeType, int newArg1, int newArg2)
//  Begin a new scope
//  Create nodes for item and /item but no transitions
{
    pushScope();

    arg1= newArg1;
    arg2= newArg2;
    lastScope= scopeType;
    arcLoc= numArc;

    startId= NewVertexId();

    switch (scopeType) {
    case SCOPE_ITEM:
    case SCOPE_RULE:
    case SCOPE_COUNT:
    case SCOPE_REPEAT:
    case SCOPE_OPT:
        endId= -1;
        lastId= startId;
        break;
    case SCOPE_ONEOF:
        endId= NewVertexId();
        lastId= endId;
        break;
    default:
        printf ("Shouldn't be getting here\n");
    }
    return;
}

void SubGraph::EndScope ()
//  End the current scope
{
    int closeId= CloseScope();
    lastId= ConnectLastScope (startId, closeId);
}

int SubGraph::ConnectLastScope (int beginScopeId, int endScopeId)
//  Connect up the child network to the parent
{
    int begLabel, endLabel, begOutLabel, endOutLabel;

    if (endId == -1)
        endId= lastId;
    if (lastScope == SCOPE_RULE) {
        begLabel= BEGINRULE_LABEL;
        begOutLabel= BEGINRULE_LABEL;
        endLabel= ENDRULE_LABEL;
        endOutLabel= arg1;  //  For inserting into closing brace
    }
    else {
        begLabel= BEGINSCOPE_LABEL;
        begOutLabel= BEGINRULE_LABEL;
        endLabel= ENDSCOPE_LABEL;
        endOutLabel= ENDSCOPE_LABEL;
    }

    popScope();

    switch (lastScope) {
    case SCOPE_ITEM:
    case SCOPE_COUNT:
    case SCOPE_REPEAT:
    case SCOPE_OPT:
        (void) CreateArc (begLabel, begOutLabel, lastId, beginScopeId);
        lastId= NewVertexId();
        (void) CreateArc (endLabel, endOutLabel, endScopeId, lastId);
        break;
    case SCOPE_ONEOF:
        (void) CreateArc (begLabel, begOutLabel, startId, beginScopeId);
        (void) CreateArc (endLabel, endOutLabel, endScopeId, endId);
        break;
    case SCOPE_RULE:
        (void) CreateArc (begLabel, begOutLabel, lastId, beginScopeId);
        lastId= NewVertexId();
        (void) CreateArc (endLabel, ruleId, endScopeId, lastId);
        break;
    case SCOPE_ROOT:
        (void) CreateArc (begLabel, begOutLabel, lastId, beginScopeId);
        lastId= NewVertexId();
        (void) CreateArc (endLabel, endOutLabel, endScopeId, lastId);
        break;
    default:
        printf ("Shouldn't be getting here\n");
    }
    return lastId;
}

int SubGraph::CloseScope ()
//  Closes the transitions and returns to the previous scope
//  Special case for count and repeats
{
    int ii, finalId, endLoc, blockCount;

    switch (lastScope) {
    case SCOPE_ITEM:
    case SCOPE_RULE:
    case SCOPE_ONEOF:
        break;
    case SCOPE_OPT:
        (void) CreateArc (NONE_LABEL, NONE_LABEL, startId, lastId);  // start to end
        break;
    case SCOPE_COUNT:
        endLoc= numArc;
        blockCount= numVertex - startId - 1;
	    //  Special case, min-count= 0

        for (ii= 1; ii < arg1; ii++)
            CopyFastArcs (this, arcLoc, endLoc, ii * blockCount, -1, -1, -1, -1);
        finalId= lastId + (arg2 - 1) * blockCount;
        for ( ; ii < arg2; ii++) {
            CopyFastArcs (this, arcLoc, endLoc, ii * blockCount, -1, -1, -1, -1);
            (void) CreateArc (NONE_LABEL, NONE_LABEL, lastId + (ii - 1) * blockCount, finalId);
        }
	    if (arg1 <= 0)
	        (void) CreateArc (NONE_LABEL, NONE_LABEL, startId, finalId);  // start to end

        UpdateVertexCount (endLoc);
        lastId= finalId;
        break;
    case SCOPE_REPEAT:
        endLoc= numArc;
        blockCount= numVertex - startId - 1;
#if 1
        for (ii= 1; ii < arg1; ii++)
            CopyFastArcs (this, arcLoc, endLoc, ii * blockCount, -1, -1, -1, -1);
        finalId= lastId + (arg1 - 1) * blockCount;

        // loop
        CopyFastArcs (this, arcLoc, endLoc, blockCount, startId, finalId, lastId, finalId);

        // start to end
        if (arg1 == 0)
            (void) CreateArc (NONE_LABEL, NONE_LABEL, startId, finalId);
#else
        // loop
        CopyFastArcs (this, arcLoc, endLoc, blockCount, startId, lastId, lastId, lastId);
        UpdateVertexCount (endLoc);

        // second part
        blockCount= numVertex - startId;
        CopyFastArcs (this, arcLoc, endLoc, blockCount, startId, lastId, lastId, -1);
        UpdateVertexCount (endLoc);

        // once
        finalId= lastId + blockCount;
        blockCount= numVertex - startId;
        if (arg1 <= 1)
            CopyFastArcs (this, arcLoc, endLoc, blockCount, startId, startId, lastId, finalId);

        // start to end
        if (arg1 == 0)
            (void) CreateArc (NONE_LABEL, NONE_LABEL, startId, finalId);
#endif

        UpdateVertexCount (endLoc);
        lastId= finalId;
        break;
    default:
        printf ("Shouldn't be getting here\n");
    }
    return lastId;
}

int SubGraph::AddItem (int inputLabel, int tagLabel)
{
    int newId;

    switch (lastScope) {
    case SCOPE_RULE:
        arg1= tagLabel;
    case SCOPE_ITEM:
    case SCOPE_OPT:
    case SCOPE_COUNT:
    case SCOPE_REPEAT:
        newId= NewVertexId();
        (void) CreateArc (inputLabel, tagLabel, lastId, newId);
        lastId= newId;
        break;
    case SCOPE_ONEOF:
        (void) CreateArc (inputLabel, tagLabel, startId, endId);
        lastId= endId;
        break;
    default: ;
        printf ("Shouldn't be getting here\n");
    }
    return lastId;
}

int SubGraph::AddTag (int tagLabel)
{
    int newId;

    switch (lastScope) {
    case SCOPE_RULE:
    case SCOPE_ITEM:
    case SCOPE_OPT:
    case SCOPE_COUNT:
    case SCOPE_REPEAT:
        newId= NewVertexId();
        (void) CreateArc (TAG_LABEL, tagLabel, lastId, newId);
        lastId= newId;
        break;
    case SCOPE_ONEOF:
        (void) CreateArc (TAG_LABEL, tagLabel, startId, endId);
        lastId= endId;
        break;
    default:
        printf ("Shouldn't be getting here\n");
    }
    return lastId;
}

void SubGraph::ExpandRules (SubGraph **subList, int *lookupList, int numSubs)
{
    int initialId, finalId, ruleId, pos;

    for (int ii= 0; ii < numArc; ii++) {
        ruleId= arc[ii]->GetInput();
        if (ruleId < 0 && ruleId > NONE_LABEL) {
            initialId= arc[ii]->GetFromId();
            finalId= arc[ii]->GetToId();
            ruleId= checkEntry (lookupList, numSubs, -ruleId);
            assert (ruleId >= 0);
	    // printf ("Expanding rule (%d) %s\n", -ruleId, subList[ruleId]->title);

            arc[ii]->AssignInput (DISCARD_LABEL);       //  Clear down the rule
	    // printf ("------------------------");
	    // subList[ruleId]->SortLanguage();
	    // subList[ruleId]->Print();
	    // printf ("------------------------////");
	    pos= numArc;
            CopyFastArcs (subList[ruleId], 0, subList[ruleId]->numArc, numVertex,
                        subList[ruleId]->startId, initialId, subList[ruleId]->lastId, finalId);
	    UpdateVertexCount (pos);
        }
    }
    UpdateVertexCount (0);
    SortLanguage ();
    return;
}

void SubGraph::UpdateVertexCount (int startLoc)
{
    int vertId, maxVertId;

    maxVertId= -1;
    for (int ii= startLoc; ii < numArc; ii++) {
        vertId= arc[ii]->GetFromId();
        if (maxVertId < vertId)
            maxVertId= vertId;
        vertId= arc[ii]->GetToId();
        if (maxVertId < vertId)
            maxVertId= vertId;
    }
    maxVertId++;
    if (startLoc <= 0)       //  i.e. a fresh start
        numVertex= maxVertId;
    else if (numVertex < maxVertId)
        numVertex= maxVertId;
    return;
}

void SubGraph::AddTerminalConnections ()
{
    //  Add terminal transition
    (void) CreateArc (TERMINAL_LABEL, NONE_LABEL, lastId, TERMINAL_LABEL);
    return;
}

void SubGraph::RemoveRuleConnections ()
{
    AddTerminalConnections ();
    RemoveTagConnections (-1, -1);
    RemoveRuleStarts (-1, -1);
    RemoveRuleEnds (-1, -1);
    RemoveNulls (-1, -1);
    ClearRuleIds ();

    ClearDuplicateArcs ();
    RemoveUnvisitedArcs (startId, lastId);
    RemoveDiscardedArcs ();
    RenumberNodes ();
    UpdateVertexCount (0);

    SortLanguage ();
    return;
}

void SubGraph::RemoveRuleStarts (int startPoint, int endPoint)
{
    if (startPoint == -1 && endPoint == -1) {
        startPoint= startId;
        endPoint= lastId;
    }

    int *nodeList= new int [numVertex];
    int *visitList= new int [numVertex];
    for (int ii= 0; ii < numVertex; ii++)
        visitList[ii]= 0;

    SortLanguage ();
    ProcessBegins (startPoint, endPoint, BEGINRULE_LABEL, nodeList, 0, visitList, numVertex);
    ClearLabelledConnections (BEGINRULE_LABEL);

    delete [] nodeList;
    delete [] visitList;
    return;
}

void SubGraph::RemoveRuleEnds (int startPoint, int endPoint)
{
    if (startPoint == -1 && endPoint == -1) {
        startPoint= startId;
        endPoint= lastId;
    }

    int *nodeList= new int [numVertex];
    int *visitList= new int [numVertex];
    for (int ii= 0; ii < numVertex; ii++)
        visitList[ii]= 0;

    SortLanguageReverse ();
    ProcessEnds (endPoint, startPoint, ENDRULE_LABEL, nodeList, 0, visitList, numVertex);
    ClearLabelledConnections (ENDRULE_LABEL);

    delete [] nodeList;
    delete [] visitList;
    return;
}

void SubGraph::RemoveNulls (int startPoint, int endPoint)
{
    if (startPoint == -1 && endPoint == -1) {
        startPoint= startId;
        endPoint= lastId;
    }

    int *nodeList= new int [numVertex];
    int *visitList= new int [numVertex];
    for (int ii= 0; ii < numVertex; ii++)
        visitList[ii]= 0;

    SortLanguage ();
    ProcessBegins (startPoint, endPoint, NONE_LABEL, nodeList, 0, visitList, numVertex);
    ClearLabelledConnections (NONE_LABEL);

    delete [] nodeList;
    delete [] visitList;
    return;
}

void SubGraph::RemoveInternalConnections ()
{
    RemoveForwardConnections (-1, -1);
    RemoveBackwardConnections (-1, -1);
    ClearDuplicateArcs ();
    RemoveUnvisitedArcs (startId, lastId);
    RemoveDiscardedArcs ();
    RenumberNodes ();
    UpdateVertexCount (0);

    SortLanguage ();
    return;
}

void SubGraph::RemoveForwardConnections (int startPoint, int endPoint)
{
    //  Code to pull up nodes for forward connecting transitions

    if (startPoint == -1 && endPoint == -1) {
        startPoint= startId;
        endPoint= lastId;
    }

    int *nodeList= new int [numVertex];
    int *visitList= new int [numVertex];
    for (int ii= 0; ii < numVertex; ii++)
        visitList[ii]= 0;

    SortLanguage ();
    ProcessBegins (startPoint, endPoint, BEGINSCOPE_LABEL, nodeList, 0, visitList, numVertex);
    ClearLabelledConnections (BEGINSCOPE_LABEL);
    RemoveDiscardedArcs ();

    delete [] nodeList;
    delete [] visitList;
    return;
}

void SubGraph::PullUpBegins (int currId, int baseId, int finalId, int procLabel,
                             int *nodeList, int currNum, int maxNum)
{
    int rix;

    nodeList[currNum]= currId;
    rix= FindFromIndex (currId);
    if (rix < 0)
        return;
    while (rix < sortNum && arc[forwardList[rix]]->GetFromId() == currId) {
        if (arc[forwardList[rix]]->GetInput() == procLabel) {
            PullUpBegins (arc[forwardList[rix]]->GetToId(), baseId,
                                finalId, procLabel, nodeList, currNum+1, maxNum);
        }
        else if (arc[forwardList[rix]]->GetInput() != DISCARD_LABEL)
            InheritArc (arc[forwardList[rix]], baseId);
        rix++;
    }
    return;
}

void SubGraph::ProcessBegins (int currId, int finalId, int procLabel,
                              int *nodeList, int currNum, int *visitMark, int maxNum)
{
    int rix, nextId;

    nodeList[currNum]= currId;
    rix= FindFromIndex (currId);
    if (rix < 0) {
	    visitMark[currId]= 1;
        return;
    }
    while (rix < sortNum && arc[forwardList[rix]]->GetFromId() == currId) {
        if (arc[forwardList[rix]]->GetInput() == procLabel) {
            PullUpBegins (arc[forwardList[rix]]->GetToId(), currId,
                        finalId, procLabel, nodeList, currNum, maxNum);
        }

        nextId= arc[forwardList[rix]]->GetToId();
        if (nextId >= 0 && nextId != finalId && checkEntry (nodeList, currNum, nextId) < 0
         && visitMark[nextId] == 0)
            ProcessBegins (nextId, finalId, procLabel, nodeList, currNum+1, visitMark, maxNum);
        rix++;
    }
    visitMark[currId]= 1;
    return;
}

void SubGraph::RemoveBackwardConnections (int startPoint, int endPoint)
{
    //  Code to push back nodes for reverse connecting transitions

    if (startPoint == -1 && endPoint == -1) {
        startPoint= startId;
        endPoint= lastId;
    }

    int *nodeList= new int [numVertex];
    int *visitList= new int [numVertex];
    for (int ii= 0; ii < numVertex; ii++)
        visitList[ii]= 0;

    SortLanguageReverse ();
    ProcessEnds (endPoint, startPoint, ENDSCOPE_LABEL, nodeList, 0, visitList, numVertex);
    ClearLabelledConnections (ENDSCOPE_LABEL);
    RemoveDiscardedArcs ();

    delete [] nodeList;
    delete [] visitList;
    return;
}

void SubGraph::PullUpEnds (int currId, int baseId, int initialId, int procLabel,
                           int *nodeList, int currNum, int maxNum)
{
    int rix;

    nodeList[currNum]= currId;
    rix= FindToIndex (currId);
    if (rix < 0)
        return;
    while (rix < sortRevNum && arc[backwardList[rix]]->GetToId() == currId) {
        if (arc[backwardList[rix]]->GetInput() == procLabel) {
            PullUpEnds (arc[backwardList[rix]]->GetFromId(), baseId,
                                initialId, procLabel, nodeList, currNum+1, maxNum);
        }
        else if (arc[backwardList[rix]]->GetInput() != DISCARD_LABEL)
            InheritReverseArc (arc[backwardList[rix]], baseId);
        rix++;
    }
    return;
}

void SubGraph::ProcessEnds (int currId, int initialId, int procLabel,
                            int *nodeList, int currNum, int *visitMark, int maxNum)
{
    int rix, nextId;

    nodeList[currNum]= currId;
    rix= FindToIndex (currId);
    if (rix < 0) {
	visitMark[currId]= 1;
        return;
    }
    while (rix < sortRevNum && arc[backwardList[rix]]->GetToId() == currId) {
        if (arc[backwardList[rix]]->GetInput() == procLabel) {
            PullUpEnds (arc[backwardList[rix]]->GetFromId(), currId,
                        initialId, procLabel, nodeList, currNum, maxNum);
        }
        nextId= arc[backwardList[rix]]->GetFromId();
        if (nextId != initialId && checkEntry (nodeList, currNum, nextId) < 0
         && visitMark[nextId] == 0)
            ProcessEnds (nextId, initialId, procLabel, nodeList, currNum+1, visitMark, maxNum);
        rix++;
    }
    visitMark[currId]= 1;
    return;
}

void SubGraph::RemoveUnreachedConnections (int startPoint, int endPoint)
{
    //  Obtain nodes with real transitions
    //  Code to pull up nodes for forward connecting transitions
    //  Code to push back nodes for reverse connecting transitions

    if (startPoint == -1 && endPoint == -1) {
        startPoint= startId;
        endPoint= lastId;
    }

    ClearDuplicateArcs ();
    RemoveUnvisitedArcs (startPoint, endPoint);
    RemoveDiscardedArcs ();
    RenumberNodes ();
    UpdateVertexCount (0);

    return;
}

void SubGraph::RemoveUnreachedConnectionsDebug (int startPoint, int endPoint)
{
    //  Obtain nodes with real transitions
    //  Code to pull up nodes for forward connecting transitions
    //  Code to push back nodes for reverse connecting transitions

    if (startPoint == -1 && endPoint == -1) {
        startPoint= startId;
        endPoint= lastId;
    }

    // ClearDuplicateArcs ();
    RemoveUnvisitedArcs (startPoint, endPoint);
    RemoveDiscardedArcs ();
    // RenumberNodes ();
    UpdateVertexCount (0);

    return;
}

void SubGraph::ReduceArcsByEquivalence ()
{
    int ii, *equivMap, *depthMap;

    //  Sort by Input, Output and to nodes
    SortLanguage ();
    SortLanguageReverse ();

    //  Calculate depth
    depthMap= new int [numVertex];
    for (ii= 0; ii < numVertex; ii++)
        depthMap[ii]= MAXNUM;
    if (lastId >= 0)
        ReverseDepthData (lastId, depthMap, 1);
    ReverseDepthOnTerminal (depthMap);
    for (ii= 0; ii < numVertex; ii++)
        if (depthMap[ii] == MAXNUM)
            depthMap[ii]= -1;

    //  Create equivalence list
    equivMap= new int [numVertex];
    for (ii= 0; ii < numVertex; ii++)
        equivMap[ii]= ii;

    //  Equivalence test to use equivalence list, duplicate transitions ignored
    //  Test nodes with same equivalence
    IdentifyEquivalence (depthMap, equivMap);

    //  On identification of an equivalence
    //      Update equivalence entry
    MapGraphVertices (equivMap);
    RemoveDiscardedArcs ();

    //  Sort for general access
    SortLanguage ();

    delete [] depthMap;
    delete [] equivMap;
    return;
}

void SubGraph::DeterminizeArcs ()
{
    int ii;
    bool allDone;

    SortLanguage ();
    DetCache *cache= new DetCache;

    SetupVisitationCache ();
    assert (VisitationConsistencyCheck () == 0);
    printf ("Determinization\n");
    allDone= false;
    while (!allDone) {
        allDone= true;
        for (ii= 0; ii < numVertex; ii++) {
	    if (QueryMinProperty(ii) == false) {
                if (startId == ii || QueryNodeProperty(ii) > 0) {
                    DeterminizeAtVertex (ii, cache);
		    SetMinProperty (ii);
        	    allDone= false;
	            //printf ("    Node %d, Total %d, Arcs %d\n", ii, numVertex, numArc);
	        }
                assert (VisitationConsistencyCheck () == 0);
		// hmm .. this seems harmless but ..
		// printf("assert(0) in SubGraph::DeterminizeArcs() ii=%d allDone=%d\n", ii, allDone);
		// assert (0);
            }
        }
    }

    ClearVisitationCache ();

    delete cache;
    return;
}

int SubGraph::IsDeterminized (int currId)
{
    int count, rix, lastInput;

    count= 0;
    lastInput= -1;
    rix= FindFromIndex (currId);
    if (rix < 0)
        return -1;
    while (rix < sortNum && arc[forwardList[rix]]->GetFromId() == currId) {
        if (lastInput >= 0 && lastInput == arc[forwardList[rix]]->GetInput())
            count++;
        else
            lastInput= arc[forwardList[rix]]->GetInput();
        rix++;
    }
    return count;
}

void SubGraph::ReverseGraphForOutput ()
{
    int     ii, incId, outId;

    assert (startId == 0);
    UpdateVertexCount (0);
    for (ii= 0; ii < numArc; ii++) {
        incId= arc[ii]->GetFromId();
        outId= arc[ii]->GetToId();
        if (outId == TERMINAL_LABEL) {
            arc[ii]->AssignFromId (0);
            arc[ii]->AssignInput (NONE_LABEL);
            arc[ii]->AssignOutput (NONE_LABEL);
            arc[ii]->AssignToId (incId);
        }
        else {
            arc[ii]->AssignFromId (outId);
            if (incId == 0)
                arc[ii]->AssignToId (numVertex);
            else
                arc[ii]->AssignToId (incId);
        }
    }
    (void) CreateArc (TERMINAL_LABEL, NONE_LABEL, numVertex, TERMINAL_LABEL);  //  Add terminal transition
    numVertex++;

    return;
}

void SubGraph::RemoveTagConnections (int startPoint, int endPoint)
{
    //  Code to push back nodes for reverse connecting transitions

    if (startPoint == -1 && endPoint == -1) {
        startPoint= startId;
        endPoint= lastId;
    }

    int *nodeList= new int [numVertex];
    int *visitList= new int [numVertex];
    for (int ii= 0; ii < numVertex; ii++)
        visitList[ii]= 0;

    SortLanguageReverse ();
    ProcessTags (endPoint, startPoint, nodeList, 0, visitList, numVertex);
    ClearLabelledConnections (TAG_LABEL);
    RemoveDiscardedArcs ();

    delete [] nodeList;
    delete [] visitList;
    return;
}

bool SubGraph::PullUpTags (int currId, int baseId, int initialId,
                           int outTag, int *nodeList, int currNum, int maxNum)
{
    int rix;
    bool hasCascade= false;

    nodeList[currNum]= currId;
    rix= FindToIndex (currId);
    if (rix < 0)
        return hasCascade;
    while (rix < sortRevNum && arc[backwardList[rix]]->GetToId() == currId) {
        if (arc[backwardList[rix]]->GetInput() == TAG_LABEL) {
            if (PullUpTags (arc[backwardList[rix]]->GetFromId(), baseId, initialId,
                arc[backwardList[rix]]->GetOutput(), nodeList, currNum+1, maxNum))
                CreateCopyWithOutput (arc[backwardList[rix]], NONE_LABEL);
            hasCascade= true;
        }
        else if (arc[backwardList[rix]]->GetInput() != DISCARD_LABEL)
            InheritReverseArcWithTag (arc[backwardList[rix]], baseId, outTag);
        rix++;
    }
    return hasCascade;
}

void SubGraph::ProcessTags (int currId, int initialId, int *nodeList, int currNum,
                            int *visitMark, int maxNum)
{
    int rix, nextId;

    nodeList[currNum]= currId;
    rix= FindToIndex (currId);
    if (rix < 0) {
	    visitMark[currId]= 1;
        return;
    }
    while (rix < sortRevNum && arc[backwardList[rix]]->GetToId() == currId) {
        if (arc[backwardList[rix]]->GetInput() == TAG_LABEL) {
            if (PullUpTags (arc[backwardList[rix]]->GetFromId(), currId, initialId,
                arc[backwardList[rix]]->GetOutput(), nodeList, currNum, maxNum))
                CreateCopyWithOutput (arc[backwardList[rix]], NONE_LABEL);
        }
        nextId= arc[backwardList[rix]]->GetFromId();
        if (nextId != initialId && checkEntry (nodeList, currNum, nextId) < 0
         && visitMark[nextId] == 0)
            ProcessTags (nextId, initialId, nodeList, currNum+1, visitMark, maxNum);
        rix++;
    }
    visitMark[currId]= 1;
    return;
}
