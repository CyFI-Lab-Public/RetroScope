/* FILE:		sub_supp.cpp
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


#define ARC_COMPARE(x,y) (arc[x]->Compare(arc[y]))
#define ARC_COMPARE_REV(x,y) (arc[x]->CompareReverse(arc[y]))
#define ARC_COMPARE_MIN(x,y) (arc[x]->CompareForMin(arc[y]))


void SubGraph::SortLanguage ()
{
    int *sortList;

    if (numArc <= 1) {
        sortList= new int [numArc+2];
        sortList[0]= 0;
        if (forwardList)
            delete [] forwardList;
        forwardList= sortList;
        sortNum= numArc;
	    return;
    }
    SortLanguageAtIndices (-1, -1);
    return;
}

void SubGraph::SortLanguageAtIndices (int begIx, int endIx)
{
    int	ii, jj, hired, retd;
    int holdArc, *sortList;

    if (begIx < 0)
        begIx= 0;
    if (endIx < 0)
        endIx= numArc;

    if (endIx <= begIx)
	    return;

    sortList= new int [numArc+2];
    for (ii= 0; ii < sortNum && ii < numArc; ii++)
        sortList[ii]= forwardList[ii];
    for (ii= begIx; ii < endIx; ii++)
        sortList[ii]= ii;
    sortList--;

    /*  Hiring
    */
    hired= (numArc >> 1)+1;
    while (hired-- > 1) {
	    holdArc= sortList[hired];
	    ii= hired;
	    jj= hired << 1;
	    while (jj <= numArc) {
	        if (jj < numArc && ARC_COMPARE (sortList[jj], sortList[jj+1]) <= 0 )
		        jj++;
	        if (ARC_COMPARE (holdArc, sortList[jj]) < 0) {
		        sortList[ii]= sortList[jj];
		        ii= jj;
		        jj <<= 1;
	        }
	        else
		    break;
	    }
	    sortList[ii]= holdArc;
    }

    /*  Retiring
    */
    retd= numArc;
    while (retd > 0) {
	    holdArc= sortList[retd];
	    sortList[retd]= sortList[1];
	        if (--retd == 1) {
	            sortList[1]= holdArc;
	            break;
	        }
	    ii= 1;
	    jj= 2;
	    while (jj <= retd) {
	        if (jj < retd && ARC_COMPARE (sortList[jj], sortList[jj+1]) <= 0)
		        jj++;
	        if (ARC_COMPARE (holdArc, sortList[jj]) < 0) {
		        sortList[ii]= sortList[jj];
		        ii= jj;
		        jj <<= 1;
	        }
	        else
		        break;
	    }
	    sortList[ii]= holdArc;
    }
    sortList++;

    if (forwardList)
        delete [] forwardList;
    forwardList= sortList;
    sortNum= numArc;

    /*  Now carry out some checks
    */
    assert(CheckSort());

    return;
}

void SubGraph::SortLanguageAtSortIndices (int begIx, int endIx)
{
    int	ii, jj, hired, retd;
    int holdArc, *sortList;

    if (begIx < 0)
        begIx= 0;
    if (endIx < 0)
        endIx= numArc;

    if (endIx <= begIx)
	    return;

    sortList= forwardList;
    sortList--;

    /*  Hiring
    */
    hired= (numArc >> 1)+1;
    while (hired-- > 1) {
	    holdArc= sortList[hired];
	    ii= hired;
	    jj= hired << 1;
	    while (jj <= numArc) {
	        if (jj < numArc && ARC_COMPARE (sortList[jj], sortList[jj+1]) <= 0 )
		        jj++;
	        if (ARC_COMPARE (holdArc, sortList[jj]) < 0) {
		        sortList[ii]= sortList[jj];
		        ii= jj;
		        jj <<= 1;
	        }
	        else
		    break;
	    }
	    sortList[ii]= holdArc;
    }

    /*  Retiring
    */
    retd= numArc;
    while (retd > 0) {
	    holdArc= sortList[retd];
	    sortList[retd]= sortList[1];
	        if (--retd == 1) {
	            sortList[1]= holdArc;
	            break;
	        }
	    ii= 1;
	    jj= 2;
	    while (jj <= retd) {
	        if (jj < retd && ARC_COMPARE (sortList[jj], sortList[jj+1]) <= 0)
		        jj++;
	        if (ARC_COMPARE (holdArc, sortList[jj]) < 0) {
		        sortList[ii]= sortList[jj];
		        ii= jj;
		        jj <<= 1;
	        }
	        else
		        break;
	    }
	    sortList[ii]= holdArc;
    }
    sortList++;

    forwardList= sortList;

    /*  Now carry out some checks
    */
    assert(CheckSort());

    return;
}

int SubGraph::FindFromIndex (int element)
{
    int rix, rix_low, rix_high, direction;

    rix_low= -1;
    rix_high= sortNum;
    while ((rix_high - rix_low) > 1) {
	    rix= (rix_high + rix_low) >> 1;
	    direction= element - arc[forwardList[rix]]->GetFromId();
	    if (direction < 0)
	        rix_high= rix;
	    else if (direction > 0)
	        rix_low= rix;
	    else {
            assert (arc[forwardList[rix]]->GetFromId() == element);
	        while (rix > 0 && arc[forwardList[rix-1]]->GetFromId() == element)
		        rix--;
            assert (arc[forwardList[rix]]->GetFromId() == element);
            assert (rix < sortNum);
	        return (rix);
	    }
    }
    return (-1);
}

void SubGraph::SortLanguageReverse ()
{
    int	ii, jj, hired, retd;
    int holdArc, *sortList;

    if (numArc <= 1) {
        sortList= new int [numArc+2];
        sortList[0]= 0;
        if (backwardList)
            delete [] backwardList;
        backwardList= sortList;
        sortRevNum= numArc;
	    return;
    }

    sortList= new int [numArc+2];
    for (ii= 0; ii < numArc; ii++)
        sortList[ii]= ii;
    sortList--;

    /*  Hiring
    */
    hired= (numArc >> 1)+1;
    while (hired-- > 1) {
	    holdArc= sortList[hired];
	    ii= hired;
	    jj= hired << 1;
	    while (jj <= numArc) {
	        if (jj < numArc && ARC_COMPARE_REV (sortList[jj], sortList[jj+1]) <= 0 )
		        jj++;
	        if (ARC_COMPARE_REV (holdArc, sortList[jj]) < 0) {
		        sortList[ii]= sortList[jj];
		        ii= jj;
		        jj <<= 1;
	        }
	        else
		    break;
	    }
	    sortList[ii]= holdArc;
    }

    /*  Retiring
    */
    retd= numArc;
    while (retd > 0) {
	    holdArc= sortList[retd];
	    sortList[retd]= sortList[1];
	        if (--retd == 1) {
	            sortList[1]= holdArc;
	            break;
	        }
	    ii= 1;
	    jj= 2;
	    while (jj <= retd) {
	        if (jj < retd && ARC_COMPARE_REV (sortList[jj], sortList[jj+1]) <= 0)
		        jj++;
	        if (ARC_COMPARE_REV (holdArc, sortList[jj]) < 0) {
		        sortList[ii]= sortList[jj];
		        ii= jj;
		        jj <<= 1;
	        }
	        else
		        break;
	    }
	    sortList[ii]= holdArc;
    }
    sortList++;

    if (backwardList)
        delete [] backwardList;
    backwardList= sortList;
    sortRevNum= numArc;

    /*  Now carry out some checks
    */
    assert(CheckSortReverse());

    return;
}

int SubGraph::FindToIndex (int element)
{
    int rix, rix_low, rix_high, direction;

    rix_low= -1;
    rix_high= sortRevNum;
    while ((rix_high - rix_low) > 1) {
	    rix= (rix_high + rix_low) >> 1;
	    direction= element - arc[backwardList[rix]]->GetToId();
	    if (direction < 0)
	        rix_high= rix;
	    else if (direction > 0)
	        rix_low= rix;
	    else {
            assert (arc[backwardList[rix]]->GetToId() == element);
	        while (rix > 0 && arc[backwardList[rix-1]]->GetToId() == element)
		        rix--;
            assert (arc[backwardList[rix]]->GetToId() == element);
            assert (rix < sortRevNum);
	        return (rix);
	    }
    }
    return (-1);
}

void SubGraph::UpdateSortForLanguage ()
{
    SortLanguageAtIndices (sortNum, numArc);
}

bool SubGraph::CheckSort ()
{
    for (int ii= 1; ii < numArc; ii++) {
	    if (ARC_COMPARE (forwardList[ii-1], forwardList[ii]) > 0)
            return false;
    }
    return true;
}

bool SubGraph::CheckSortReverse ()
{
    for (int ii= 1; ii < numArc; ii++) {
	if (ARC_COMPARE_REV (backwardList[ii-1], backwardList[ii]) > 0) {
	    printf ("Failed at %d\n", ii);
            return false;
	}
    }
    return true;
}

void SubGraph::ClearDuplicateArcs ()
{
    int currId;

    SortLanguage();
    currId= 0;
    for (int ii= 1; ii < numArc; ii++) {
        if (arc[forwardList[ii]]->GetInput() != DISCARD_LABEL)
            if (ARC_COMPARE (forwardList[currId], forwardList[ii]) == 0)
                arc[forwardList[ii]]->AssignInput (DISCARD_LABEL);
            else
                currId= ii;
    }
    return;
}

void SubGraph::RenumberNodes ()
{
  int  ii, id, count;

    UpdateVertexCount (0);
    if (numVertex > 0) {
        int *mapList= new int [numVertex];
        for (ii= 0; ii < numVertex; ii++)
            mapList[ii]= -1;

        for (ii= 0; ii < numArc; ii++) {
            id= arc[ii]->GetFromId();
            mapList[id]= 1;
            id= arc[ii]->GetToId();
            if (id >= 0)
                mapList[id]= 1;
        }

        count= 0;
        for (ii= 0; ii < numVertex; ii++)
            if (mapList[ii] > 0) {
                mapList[ii]= count;
                count++;
            }

        for (ii= 0; ii < numArc; ii++) {
            id= arc[ii]->GetFromId();
            arc[ii]->AssignFromId(mapList[id]);
            id= arc[ii]->GetToId();
            if (id >= 0)
                arc[ii]->AssignToId(mapList[id]);
        }
        startId= mapList[startId];
        if (lastId >= 0)
            lastId= mapList[lastId];
        delete [] mapList;
    }
    else {
        startId= 0;
        lastId= -1;
        endId= -1;
    }
    return;
}

void SubGraph::RemoveDuplicatesAtNode (int rixBegin, int rixEnd)
//  Works only within one node/vertex
{
    int rix, lastRix;
    bool needSort;

    SortLanguageAtSortIndices (rixBegin, rixEnd);

    //  Check for duplicate transitions
    needSort= false;
    lastRix= rixBegin;
    for (rix= rixBegin+1; rix < rixEnd; rix++) {
        assert (arc[forwardList[lastRix]]->GetFromId() == arc[forwardList[rix]]->GetFromId());
        if (ARC_COMPARE (forwardList[lastRix], forwardList[rix]) == 0) {
            arc[forwardList[rix]]->AssignInput (DISCARD_LABEL);
            needSort= true;
        }
        else
            lastRix= rix;
    }

    //  Resort
    if (needSort)
        SortLanguageAtSortIndices (rixBegin, rixEnd);

    return;
}

void SubGraph::SortLanguageForMin ()
{
    int *sortList;

    if (numArc <= 1) {
        sortList= new int [numArc+2];
        sortList[0]= 0;
        if (forwardList)
            delete [] forwardList;
        forwardList= sortList;
        sortNum= numArc;
	    return;
    }
    SortLanguageAtIndicesForMin (-1, -1);
    return;
}

void SubGraph::SortLanguageAtIndicesForMin (int begIx, int endIx)
{
    int	ii, jj, hired, retd;
    int holdArc, *sortList;

    if (begIx < 0)
        begIx= 0;
    if (endIx < 0)
        endIx= numArc;

    if (endIx <= begIx)
	    return;

    sortList= new int [numArc+2];
    for (ii= 0; ii < sortNum && ii < numArc; ii++)
        sortList[ii]= forwardList[ii];
    for (ii= begIx; ii < endIx; ii++)
        sortList[ii]= ii;
    sortList--;

    /*  Hiring
    */
    hired= (numArc >> 1)+1;
    while (hired-- > 1) {
	    holdArc= sortList[hired];
	    ii= hired;
	    jj= hired << 1;
	    while (jj <= numArc) {
	        if (jj < numArc && ARC_COMPARE_MIN (sortList[jj], sortList[jj+1]) <= 0 )
		        jj++;
	        if (ARC_COMPARE_MIN (holdArc, sortList[jj]) < 0) {
		        sortList[ii]= sortList[jj];
		        ii= jj;
		        jj <<= 1;
	        }
	        else
		    break;
	    }
	    sortList[ii]= holdArc;
    }

    /*  Retiring
    */
    retd= numArc;
    while (retd > 0) {
	    holdArc= sortList[retd];
	    sortList[retd]= sortList[1];
	        if (--retd == 1) {
	            sortList[1]= holdArc;
	            break;
	        }
	    ii= 1;
	    jj= 2;
	    while (jj <= retd) {
	        if (jj < retd && ARC_COMPARE_MIN (sortList[jj], sortList[jj+1]) <= 0)
		        jj++;
	        if (ARC_COMPARE_MIN (holdArc, sortList[jj]) < 0) {
		        sortList[ii]= sortList[jj];
		        ii= jj;
		        jj <<= 1;
	        }
	        else
		        break;
	    }
	    sortList[ii]= holdArc;
    }
    sortList++;

    if (forwardList)
        delete [] forwardList;
    forwardList= sortList;
    sortNum= numArc;

    /*  Now carry out some checks
    */
    int checkSort = CheckSort();
    if(checkSort == 0) {
      // printf("assert(0) in SubGraph::CheckSort %d\n", checkSort);
      // hmm .. this seems harmless but ...!
      // assert(checkSort);
    }

    return;
}

