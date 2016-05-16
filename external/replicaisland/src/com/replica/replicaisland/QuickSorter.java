/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.replica.replicaisland;

import java.util.Comparator;

public class QuickSorter<Type> extends Sorter<Type> {
    public void sort(Type[] array, int count, Comparator<Type> comparator) {
        quicksort(array, 0, count - 1, comparator);
    }
    
    // Quicksort implementation based on the one here:
    // http://www.cs.princeton.edu/introcs/42sort/QuickSort.java.html
    /*************************************************************************
     *
     *  Generate N random real numbers between 0 and 1 and quicksort them.
     *
     *  On average, this quicksort algorithm runs in time proportional to
     *  N log N, independent of the input distribution. The algorithm
     *  uses Sedgewick's partitioning method which stops on equal keys. 
     *  This protects against cases that make many textbook implementations, 
     *  even randomized ones, go quadratic (e.g., all keys are the same).
     *
     *************************************************************************/
    
    /***********************************************************************
     *  Quicksort code from Sedgewick 7.1, 7.2.
     ***********************************************************************/
     
        // quicksort a[left] to a[right]
    public void quicksort(Type[] a, int left, int right, Comparator<Type> comparator) {
        if (right <= left) return;
        int i = partition(a, left, right, comparator);
        quicksort(a, left, i - 1, comparator);
        quicksort(a, i + 1, right, comparator);
    }
       
    // partition a[left] to a[right], assumes left < right
    private int partition(Type[] a, int left, int right, Comparator<Type> comparator) {
        int i = left - 1;
        int j = right;
        while (true) {
            while (comparator.compare(a[++i], a[right]) < 0) {     // find item on left to swap
            }                              // a[right] acts as sentinel
            while (comparator.compare(a[right], a[--j]) < 0) {    // find item on right to swap
                if (j == left) { 
                    break;                 // don't go out-of-bounds
                }
            }
            if (i >= j) {
                break;                     // check if pointers cross
            }
            Type swap = a[i];                 // swap two elements into place
            a[i] = a[j];
            a[j] = swap;
        }
        Type swap = a[i]; // swap with partition element
        a[i] = a[right];
        a[right] = swap;
        return i;
    }
}
