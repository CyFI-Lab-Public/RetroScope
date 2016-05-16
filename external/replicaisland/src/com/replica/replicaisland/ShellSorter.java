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

public class ShellSorter<Type> extends Sorter<Type> {
/** 
 * Shell sort implementation based on the one found here:
 * http://www.augustana.ab.ca/~mohrj/courses/2004.winter/csc310/source/ShellSort.java.html
 * Note that the running time can be tuned by adjusting the size of the increment used
 * to pass over the array each time.  Currently this function uses Robert Cruse's suggestion
 * of increment = increment / 3 + 1.
 */
 
public void sort(Type[] array, int count, Comparator<Type> comparator) {
    int increment = count / 3 + 1;

    // Sort by insertion sort at diminishing increments.
    while ( increment > 1 ) {
        for ( int start = 0; start < increment; start++ ) {
            insertionSort(array, count, start, increment, comparator);
        }
        increment = increment / 3 + 1;
    } 
    
    // Do a final pass with an increment of 1.
    // (This has to be outside the previous loop because the formula above for calculating the
    // next increment will keep generating 1 repeatedly.)
    insertionSort(array, count, 0, 1, comparator );
 }

    
/**
  *  Insertion sort modified to sort elements at a
  *  fixed increment apart.
  *
  *  The code can be revised to eliminate the initial
  *  'if', but I found that it made the sort slower.
  *
  *  @param start      the start position 
  *  @param increment  the increment
  */
public void insertionSort(Type[] array, int count, int start, int increment,
        Comparator<Type> comparator) {
    int j;
    int k;
    Type temp;

    for (int i = start + increment; i < count; i += increment) {
       j = i;
       k = j - increment;
       int delta = comparator.compare(array[j], array[k]);
       
       if ( delta < 0 ) {
          // Shift all previous entries down by the current
    	  // increment until the proper place is found.
          temp = array[j];
          do {
             array[j] = array[k];
             j = k;
             k = j - increment;
          } while ( j != start && comparator.compare(array[k], temp) > 0 );
          array[j] = temp;
       }
    }
  }
}
