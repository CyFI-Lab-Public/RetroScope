/* FILE:		sub_base.cpp
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


SubGraph::SubGraph (SubGraph *baseG)
{
    int count= strlen(baseG->title);
    title= new char [count+1];
    strcpy (title, baseG->title);
    ruleId= baseG->ruleId;
    arc= new NUANArc * [BLKSIZE];
    popOp= 0;
    numVertex= baseG->numVertex;
    lastScope= SCOPE_ROOT;
    startId= baseG->startId;
    lastId= baseG->lastId;
    endId= baseG->endId;
    forwardList= 0;
    backwardList= 0;
    sortNum= 0;
    numArc= 0;
    CopyFastArcs (baseG, 0, baseG->numArc, 0, -1, -1, -1, -1);
    UpdateVertexCount (0);

    return;
}

int SubGraph::NewVertexId ()
{
    return (numVertex++);
}

void SubGraph::AllocateSpaceForArc ()
{
    if (numArc == 0) {
        arc= new NUANArc * [BLKSIZE];
    }
    if (numArc%BLKSIZE == 0) {
        NUANArc **new_arc= new NUANArc * [numArc+BLKSIZE];
        for (int ii= 0; ii < numArc; ii++)
            new_arc[ii]= arc[ii];
        delete [] arc;
        arc= new_arc;
    }
}

NUANArc *SubGraph::CreateArc (int iLabel, int oLabel, int from, int to)
{
    assert (!(iLabel == NONE_LABEL && from == to));
    AllocateSpaceForArc ();
    NUANArc *arc_one= new NUANArc (iLabel, oLabel, from, to);
    arc[numArc]= arc_one;
    numArc++;
    return arc_one;
}

NUANArc *SubGraph::InheritArc (NUANArc *arcBase, int id)
{
    AllocateSpaceForArc ();
    NUANArc *arc_one= new NUANArc (arcBase);
    arc_one->AssignFromId (id);
    arc[numArc]= arc_one;
    numArc++;
    return arc_one;
}

NUANArc *SubGraph::InheritReverseArc (NUANArc *arcBase, int id)
{
    AllocateSpaceForArc ();
    NUANArc *arc_one= new NUANArc (arcBase);
    arc_one->AssignToId (id);
    arc[numArc]= arc_one;
    numArc++;
    return arc_one;
}

NUANArc *SubGraph::InheritReverseArcWithTag (NUANArc *arcBase, int id, int tagId)
{
    AllocateSpaceForArc ();
    NUANArc *arc_one= new NUANArc (arcBase);
    arc_one->AssignToId (id);
    arc_one->AssignOutput (tagId);
    arc[numArc]= arc_one;
    numArc++;
    return arc_one;
}

NUANArc *SubGraph::CreateCopyWithOutput (NUANArc *arcBase, int id)
{
    AllocateSpaceForArc ();
    NUANArc *arc_one= new NUANArc (arcBase);
    arc_one->AssignOutput (id);
    arc[numArc]= arc_one;
    numArc++;
    return arc_one;
}

void SubGraph::CopyFastArcs (SubGraph *subG, int startLoc, int endLoc, int offset, int headId, int newHeadId, int tailId, int newTailId)
{
    NUANArc *arc_one;

    for (int ii= startLoc; ii < endLoc; ii++) {
        if (numArc%BLKSIZE == 0) {
            NUANArc **new_arc= new NUANArc * [numArc+BLKSIZE];
            for (int ii= 0; ii < numArc; ii++)
                new_arc[ii]= arc[ii];
            delete [] arc;
            arc= new_arc;
        }
        arc_one= new NUANArc (subG->arc[ii], offset, headId, newHeadId, tailId, newTailId);
        arc[numArc]= arc_one;
        numArc++;
    }
    return;
}

void SubGraph::Print ()
{
    int loc;

    printf ("Graph %s (%d %d)\n", title, startId, lastId);
    for (int ii= 0; ii < numArc; ii++) {
        loc= forwardList[ii];
        // loc= ii;
        arc[loc]->Print();
    }
    return;
}

void SubGraph::PrintText ()
{
    int loc;

    printf ("Graph %s (%d %d)\n", title, startId, lastId);
    for (int ii= 0; ii < numArc; ii++) {
        loc= forwardList[ii];
        // loc= ii;
        arc[loc]->PrintText();
    }
    return;
}

void SubGraph::ReverseDepthOnTerminal (int *depthMap)
{
    for (int ii= 0; ii < numArc; ii++)
        if (arc[ii]->GetInput() == TERMINAL_LABEL)
            ReverseDepthData (arc[ii]->GetFromId(), depthMap, 1);
        return;
}

void SubGraph::ReverseDepthData (int startId, int *depthMap, int depth)
{
    int rix, nextId, nextInp;

    if (depthMap[startId] > depth)
        depthMap[startId]= depth;
    else
        return;
    rix= FindToIndex (startId);
    if (rix < 0)
        return;
    while (rix < numArc && arc[backwardList[rix]]->GetToId() == startId) {
        nextId= arc[backwardList[rix]]->GetFromId();
        nextInp= arc[backwardList[rix]]->GetInput();
        if (nextId >= 0 && nextInp != DISCARD_LABEL)
            ReverseDepthData (nextId, depthMap, depth+1);
        rix++;
    }
    return;
}

void SubGraph::ForwardDepthData (int startId, int *depthMap, int depth)
{
    int rix, nextId, nextInp;

    if (depthMap[startId] > depth)
        depthMap[startId]= depth;
    else
        return;
    rix= FindFromIndex (startId);
    if (rix < 0)
        return;
    while (rix < numArc && arc[forwardList[rix]]->GetFromId() == startId) {
        nextId= arc[forwardList[rix]]->GetToId();
        nextInp= arc[forwardList[rix]]->GetInput();
        if (nextId >= 0 && nextInp != DISCARD_LABEL)
            ForwardDepthData (nextId, depthMap, depth+1);
        rix++;
    }
    return;
}

void SubGraph::RemoveUnvisitedArcs (int initialId, int finalId)
{
    int ii;
    int *forwardDepth= new int [numVertex];
    int *reverseDepth= new int [numVertex];

    for (ii= 0; ii < numVertex; ii++)
        forwardDepth[ii]= reverseDepth[ii]= MAXNUM;     //  TODO: review
    SortLanguage();
    SortLanguageReverse();

    ForwardDepthData (initialId, forwardDepth, 1);
    if (finalId >= 0)
        ReverseDepthData (finalId, reverseDepth, 1);
    ReverseDepthOnTerminal (reverseDepth);

    for (ii= 0; ii < numVertex; ii++) {
        if (forwardDepth[ii] == MAXNUM)
            forwardDepth[ii]= -1;
        if (reverseDepth[ii] == MAXNUM)
            reverseDepth[ii]= -1;
    }
    RemoveMarkedNodes (forwardDepth, reverseDepth);
    delete [] forwardDepth;
    delete [] reverseDepth;
    return;
}

void SubGraph::RemoveMarkedNodes (int *forwardDepth, int *reverseDepth)
{
    int ii, currId, incId, outId;

    currId= 0;
    for (ii= 0; ii < numArc; ii++) {
        incId= arc[ii]->GetFromId();
        outId= arc[ii]->GetToId();
        assert (outId == DISCARD_LABEL || outId == TERMINAL_LABEL || outId >= 0);
        if (incId != DISCARD_LABEL && outId != DISCARD_LABEL
            && arc[ii]->GetInput() != DISCARD_LABEL
            && forwardDepth[incId] > 0
            && (outId == TERMINAL_LABEL || reverseDepth[outId] > 0)) {
            arc[currId]= arc[ii];
            currId++;
        }
        else
            delete arc[ii];
    }
    for (ii= currId; ii < numArc; ii++)
	arc[ii]= 0;
    numArc= currId;
    return;
}

void SubGraph::RemoveDiscardedArcs ()
{
    int ii, currId, outId, inpId;

    currId= 0;
    for (ii= 0; ii < numArc; ii++) {
        outId= arc[ii]->GetToId();
        inpId= arc[ii]->GetInput();
        if (outId != DISCARD_LABEL && inpId != DISCARD_LABEL) {
            arc[currId]= arc[ii];
            currId++;
        }
        else
            delete arc[ii];
    }
    for (ii= currId; ii < numArc; ii++)
	arc[ii]= 0;
    numArc= currId;
    return;
}

void SubGraph::MapGraphVertices (int *equivMap)
{
    int ii, fromId, toId;

    for (ii= 0; ii < numArc; ii++) {
        fromId= arc[ii]->GetFromId();
        if (fromId >= 0)
            if (equivMap[fromId] != fromId)
                arc[ii]->AssignInput (DISCARD_LABEL);
        toId= arc[ii]->GetToId();
        if (toId >= 0)
            if (equivMap[toId] != toId)
                arc[ii]->AssignToId (equivMap[toId]);
    }
    return;
}

void SubGraph::DebugPrintDirective (char *dirrData)
{
    for (int ii= 0; ii < (popOp/7); ii++)
        printf ("  ");
    printf ("%s\n", dirrData);
    return;
}

void SubGraph::DebugPrintLabel (int labId)
{
    for (int ii= 0; ii <= (popOp/7); ii++)
        printf ("  ");
    printf ("%d\n", labId);
    return;
}

void SubGraph::ClearLabelledConnections (int labItem)
{
    for (int ii= 0; ii < numArc; ii++) {
        if (arc[ii]->GetInput() == labItem)
            arc[ii]->AssignToId (DISCARD_LABEL);
    }
    return;
}

void SubGraph::ClearRuleIds ()
{
    for (int ii= 0; ii < numArc; ii++) {
        if (arc[ii]->GetInput() < 0 && arc[ii]->GetInput() > NONE_LABEL)
            arc[ii]->AssignToId (DISCARD_LABEL);
    }
    return;
}

void SubGraph::ClearOutputs ()
{
    for (int ii= 0; ii < numArc; ii++)
        arc[ii]->AssignOutput (NONE_LABEL);
    return;
}
