/* FILE:		gr_iface.cpp
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

#include "grph.h"

#define DEBUG       0

int Graph::addSubGraph (SubGraph *sbGraph)
{
    int ruleId;

    if (numSubGraph%BLKSIZE == 0) {
	if (numSubGraph >0) {
	    SubGraph **newSubGraph= new SubGraph * [numSubGraph+BLKSIZE];
	    int *newIndex= new int [numSubGraph+BLKSIZE];
	    for (int ii= 0; ii < numSubGraph; ii++) {
                newSubGraph[ii]= subGraph[ii];
                newIndex[ii]= subIndex[ii];
	    }
	    delete [] subGraph;
	    delete [] subIndex;
	    subGraph= newSubGraph;
	    subIndex= newIndex;
	}
	else {
	    subGraph= new SubGraph * [BLKSIZE];
	    subIndex= new int [BLKSIZE];
	}
    }
    ruleId= sbGraph->getRuleId();

    subGraph[numSubGraph]= sbGraph;
    subIndex[numSubGraph]= ruleId;
#if DEBUG
    char rulLabel[128];

    if (sbGraph) {
        sbGraph->getName (rulLabel, 128);
        printf ("Adding rule %s with %d\n", rulLabel, ruleId);
    }
#endif
    numSubGraph++;
    return numSubGraph;
}

int Graph::getSubGraphIndex (int subId)
{
    for (int ii= numSubGraph-1; ii >= 0; ii--)
        if (subIndex[ii] == subId)
            return ii;
    return -1;
}

int Graph::getSubGraphIndex (SubGraph *sGraph)
{
    for (int ii= numSubGraph-1; ii >= 0; ii--)
        if (subGraph[ii] == sGraph)
            return subIndex[ii];
    return -1;
}

/**  Begin and end scope */

void Graph::BeginRule (SubGraph *subg)
{
    subg->BeginScope (SCOPE_RULE, 0, 0);
#if DEBUG
    subg->DebugPrintDirective ("<ruleref>");
#endif
    return;
}

void Graph::EndRule (SubGraph *subg)
{
#if DEBUG
    subg->DebugPrintDirective ("</ruleref>");
#endif
    subg->EndScope();
    return;
}

void Graph::BeginItem (SubGraph *subg)
{
    subg->BeginScope (SCOPE_ITEM, 0, 0);
#if DEBUG
    subg->DebugPrintDirective ("<item>");
#endif
    return;
}

void Graph::BeginItemRepeat (SubGraph *subg, int minCount, int maxCount)
{
    subg->BeginScope (SCOPE_REPEAT, minCount, maxCount);
#if DEBUG
    subg->DebugPrintDirective ("<item repeat>");
#endif
    return;
}

void Graph::AddRuleRef (SubGraph *subg, int ruleNo)
{
    subg->AddItem (-ruleNo, ruleNo);
#if DEBUG
    subg->DebugPrintDirective ("<add ruleref>");
    printf ("    %d\n", ruleNo);
#endif
    return;
}

void Graph::AddLabel (SubGraph *subg, int labNo)
{
    subg->AddItem (labNo, -1);
#if DEBUG
    subg->DebugPrintLabel (labNo);
#endif
    return;
}

void Graph::AddTag (SubGraph *subg, int tagNo)
{
    subg->AddTag (tagNo);
#if DEBUG
    subg->DebugPrintLabel (tagNo);
#endif
    return;
}

void Graph::EndItem (SubGraph *subg)
{
#if DEBUG
    subg->DebugPrintDirective ("</item>");
#endif
    subg->EndScope();
    return;
}

void Graph::BeginOneOf (SubGraph *subg)
{
    subg->BeginScope (SCOPE_ONEOF, 0, 0);
#if DEBUG
    subg->DebugPrintDirective ("<one-of>");
#endif
    return;
}

void Graph::EndOneOf (SubGraph *subg)
{
#if DEBUG
    subg->DebugPrintDirective ("</one-of>");
#endif
    subg->EndScope ();
    return;
}

void Graph::BeginCount (SubGraph *subg, int minCount, int maxCount)
{
    subg->BeginScope (SCOPE_COUNT, minCount, maxCount);
#if DEBUG
    subg->DebugPrintDirective ("<count>");
#endif
    return;
}

void Graph::EndCount (SubGraph *subg)
{
#if DEBUG
    subg->DebugPrintDirective ("</count>");
#endif
    subg->EndScope();
    return;
}

void Graph::BeginOptional (SubGraph *subg)
{
    subg->BeginScope (SCOPE_OPT, 0, 0);
#if DEBUG
    subg->DebugPrintDirective ("<item repeat= 0- >");
#endif
    return;
}

void Graph::ExpandRules (SubGraph *subg)
{
    subg->ExpandRules (subGraph, subIndex, numSubGraph);
    return;
}
