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

import java.util.Arrays;
import java.util.Comparator;

/**
 * FixedSizeArray is an alternative to a standard Java collection like ArrayList.  It is designed
 * to provide a contiguous array of fixed length which can be accessed, sorted, and searched without
 * requiring any runtime allocation.  This implementation makes a distinction between the "capacity"
 * of an array (the maximum number of objects it can contain) and the "count" of an array
 * (the current number of objects inserted into the array).  Operations such as set() and remove()
 * can only operate on objects that have been explicitly add()-ed to the array; that is, indexes
 * larger than getCount() but smaller than getCapacity() can't be used on their own.
 * @param <T> The type of object that this array contains.
 */
public class FixedSizeArray<T> extends AllocationGuard {
    private final static int LINEAR_SEARCH_CUTOFF = 16;
    private final T[] mContents;
    private int mCount;
    private Comparator<T> mComparator;
    private boolean mSorted;
    private Sorter<T> mSorter;
    
    public FixedSizeArray(int size) {
        super();
        assert size > 0;
        // Ugh!  No generic array construction in Java.
        mContents = (T[])new Object[size];
        mCount = 0;
        mSorted = false;
        
        mSorter = new StandardSorter<T>();        
    }
    
    public FixedSizeArray(int size, Comparator<T> comparator) {
        super();
        assert size > 0;
        mContents = (T[])new Object[size];
        mCount = 0;
        mComparator = comparator;
        mSorted = false;
        
        mSorter = new StandardSorter<T>();
    }
    
    /** 
     * Inserts a new object into the array.  If the array is full, an assert is thrown and the
     * object is ignored.
     */
    public final void add(T object) {
        assert mCount < mContents.length : "Array exhausted!";
        if (mCount < mContents.length) {
            mContents[mCount] = object;
            mSorted = false;
            mCount++;
        }
    }
    
    /** 
     * Searches for an object and removes it from the array if it is found.  Other indexes in the
     * array are shifted up to fill the space left by the removed object.  Note that if
     * ignoreComparator is set to true, a linear search of object references will be performed.
     * Otherwise, the comparator set on this array (if any) will be used to find the object.
     */
    public void remove(T object, boolean ignoreComparator) {
        final int index = find(object, ignoreComparator);
        if (index != -1) {
            remove(index);
        }
    }
    
    /** 
     * Removes the specified index from the array.  Subsequent entries in the array are shifted up
     * to fill the space.
     */
    public void remove(int index) {
        assert index < mCount;
        // ugh
        if (index < mCount) {
            for (int x = index; x < mCount; x++) {
                if (x + 1 < mContents.length && x + 1 < mCount) {
                    mContents[x] = mContents[x + 1];
                } else {
                    mContents[x]  = null;
                }
            }
            mCount--;
        }
    }
    
    /**
     * Removes the last element in the array and returns it.  This method is faster than calling
     * remove(count -1);
     * @return The contents of the last element in the array.
     */
    public T removeLast() {
        T object = null;
        if (mCount > 0) {
            object = mContents[mCount - 1];
            mContents[mCount - 1] = null;
            mCount--;
        }
        return object;
    }
    
    /**
     * Swaps the element at the passed index with the element at the end of the array.  When
     * followed by removeLast(), this is useful for quickly removing array elements.
     */
    public void swapWithLast(int index) {
        if (mCount > 0 && index < mCount - 1) {
            T object = mContents[mCount - 1];
            mContents[mCount - 1] = mContents[index];
            mContents[index] = object;
            mSorted = false;
        }
    }
    
    /**
     * Sets the value of a specific index in the array.  An object must have already been added to
     * the array at that index for this command to complete.
     */
    public void set(int index, T object) {
        assert index < mCount;
        if (index < mCount) {
            mContents[index] = object; 
        }
    }
    
    /**
     * Clears the contents of the array, releasing all references to objects it contains and 
     * setting its count to zero.
     */
    public void clear() {
        for (int x = 0; x < mCount; x++) {
            mContents[x] = null;
        }
        mCount = 0;
        mSorted = false;
    }
       
    /**
     * Returns an entry from the array at the specified index.
     */
    public T get(int index) {
        assert index < mCount;
        T result = null;
        if (index < mCount && index >= 0) {
            result = mContents[index];
        }
        return result;
    }
    
    /** 
     * Returns the raw internal array.  Exposed here so that tight loops can cache this array
     * and walk it without the overhead of repeated function calls.  Beware that changing this array
     * can leave FixedSizeArray in an undefined state, so this function is potentially dangerous
     * and should be used in read-only cases.
     * @return The internal storage array.
     */
    public final Object[] getArray() {
        return mContents;
    }
    
    
    /**
     * Searches the array for the specified object.  If the array has been sorted with sort(),
     * and if no other order-changing events have occurred since the sort (e.g. add()), a
     * binary search will be performed.  If a comparator has been specified with setComparator(),
     * it will be used to perform the search.  If not, the default comparator for the object type
     * will be used.  If the array is unsorted, a linear search is performed.
     * Note that if ignoreComparator is set to true, a linear search of object references will be 
     * performed. Otherwise, the comparator set on this array (if any) will be used to find the
     * object.
     * @param object  The object to search for.
     * @return  The index of the object in the array, or -1 if the object is not found.
     */
    public int find(T object, boolean ignoreComparator) {
        int index = -1;
        final int count = mCount;
    	final boolean sorted = mSorted;
    	final Comparator comparator = mComparator;
    	final T[] contents = mContents;
        if (sorted && !ignoreComparator && count > LINEAR_SEARCH_CUTOFF) {
            if (comparator != null) {
                index = Arrays.binarySearch(contents, object, comparator);
            } else {
                index = Arrays.binarySearch(contents, object);
            }
            // Arrays.binarySearch() returns a negative insertion index if the object isn't found,
            // but we just want a boolean.
            if (index < 0) {
                index = -1;
            }
        } else {
            // unsorted, linear search
        	
            if (comparator != null && !ignoreComparator) {
                for (int x = 0; x < count; x++) {
                	final int result = comparator.compare(contents[x], object);
                    if (result == 0) {
                        index = x;
                        break;
                    } else if (result > 0 && sorted) {
                    	// we've passed the object, early out
                    	break;
                    }
                }
            } else {
                for (int x = 0; x < count; x++) {
                    if (contents[x] == object) {
                        index = x;
                        break;
                    } 
                }  
            }
        }
        return index;
    }
    
    /**
     * Sorts the array.  If the array is already sorted, no work will be performed unless
     * the forceResort parameter is set to true.  If a comparator has been specified with
     * setComparator(), it will be used for the sort; otherwise the object's natural ordering will
     * be used.
     * @param forceResort  If set to true, the array will be resorted even if the order of the
     * objects in the array has not changed since the last sort.
     */
    public void sort(boolean forceResort) {
        if (!mSorted || forceResort) {
           if (mComparator != null) {
               mSorter.sort(mContents, mCount, mComparator);
           } else {
               DebugLog.d("FixedSizeArray", "No comparator specified for this type, using Arrays.sort().");
               
               Arrays.sort(mContents, 0, mCount);
           }
           mSorted = true;
        }
    }
    
    /** Returns the number of objects in the array. */
    public int getCount() {
        return mCount;
    }
    
    /** Returns the maximum number of objects that can be inserted inot this array. */
    public int getCapacity() {
        return mContents.length;
    }
    
    /** Sets a comparator to use for sorting and searching. */
    public void setComparator(Comparator<T> comparator) {
        mComparator = comparator;
        mSorted = false;
    }
    
    public void setSorter(Sorter<T> sorter) {
        mSorter = sorter;
    }
}
