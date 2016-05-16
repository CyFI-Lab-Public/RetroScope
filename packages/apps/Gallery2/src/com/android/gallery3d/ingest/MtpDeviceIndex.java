/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.gallery3d.ingest;

import android.mtp.MtpConstants;
import android.mtp.MtpDevice;
import android.mtp.MtpObjectInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * MTP objects in the index are organized into "buckets," or groupings.
 * At present, these buckets are based on the date an item was created.
 *
 * When the index is created, the buckets are sorted in their natural
 * order, and the items within the buckets sorted by the date they are taken.
 *
 * The index enables the access of items and bucket labels as one unified list.
 * For example, let's say we have the following data in the index:
 *    [Bucket A]: [photo 1], [photo 2]
 *    [Bucket B]: [photo 3]
 *
 * Then the items can be thought of as being organized as a 5 element list:
 *   [Bucket A], [photo 1], [photo 2], [Bucket B], [photo 3]
 *
 * The data can also be accessed in descending order, in which case the list
 * would be a bit different from simply reversing the ascending list, since the
 * bucket labels need to always be at the beginning:
 *   [Bucket B], [photo 3], [Bucket A], [photo 2], [photo 1]
 *
 * The index enables all the following operations in constant time, both for
 * ascending and descending views of the data:
 *   - get/getAscending/getDescending: get an item at a specified list position
 *   - size: get the total number of items (bucket labels and MTP objects)
 *   - getFirstPositionForBucketNumber
 *   - getBucketNumberForPosition
 *   - isFirstInBucket
 *
 * See the comments in buildLookupIndex for implementation notes.
 */
public class MtpDeviceIndex {

    public static final int FORMAT_MOV = 0x300D; // For some reason this is not in MtpConstants

    public static final Set<Integer> SUPPORTED_IMAGE_FORMATS;
    public static final Set<Integer> SUPPORTED_VIDEO_FORMATS;

    static {
        SUPPORTED_IMAGE_FORMATS = new HashSet<Integer>();
        SUPPORTED_IMAGE_FORMATS.add(MtpConstants.FORMAT_JFIF);
        SUPPORTED_IMAGE_FORMATS.add(MtpConstants.FORMAT_EXIF_JPEG);
        SUPPORTED_IMAGE_FORMATS.add(MtpConstants.FORMAT_PNG);
        SUPPORTED_IMAGE_FORMATS.add(MtpConstants.FORMAT_GIF);
        SUPPORTED_IMAGE_FORMATS.add(MtpConstants.FORMAT_BMP);

        SUPPORTED_VIDEO_FORMATS = new HashSet<Integer>();
        SUPPORTED_VIDEO_FORMATS.add(MtpConstants.FORMAT_3GP_CONTAINER);
        SUPPORTED_VIDEO_FORMATS.add(MtpConstants.FORMAT_AVI);
        SUPPORTED_VIDEO_FORMATS.add(MtpConstants.FORMAT_MP4_CONTAINER);
        SUPPORTED_VIDEO_FORMATS.add(MtpConstants.FORMAT_MPEG);
        // TODO: add FORMAT_MOV once Media Scanner supports .mov files
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mDevice == null) ? 0 : mDevice.getDeviceId());
        result = prime * result + mGeneration;
        return result;
    }

    public interface ProgressListener {
        public void onObjectIndexed(MtpObjectInfo object, int numVisited);

        public void onSorting();

        public void onIndexFinish();
    }

    public enum SortOrder {
        Ascending, Descending
    }

    private MtpDevice mDevice;
    private int[] mUnifiedLookupIndex;
    private MtpObjectInfo[] mMtpObjects;
    private DateBucket[] mBuckets;
    private int mGeneration = 0;

    public enum Progress {
        Uninitialized, Initialized, Pending, Started, Sorting, Finished
    }

    private Progress mProgress = Progress.Uninitialized;
    private ProgressListener mProgressListener;

    private static final MtpDeviceIndex sInstance = new MtpDeviceIndex();
    private static final MtpObjectTimestampComparator sMtpObjectComparator =
            new MtpObjectTimestampComparator();

    public static MtpDeviceIndex getInstance() {
        return sInstance;
    }

    private MtpDeviceIndex() {
    }

    synchronized public MtpDevice getDevice() {
        return mDevice;
    }

    /**
     * Sets the MtpDevice that should be indexed and initializes state, but does
     * not kick off the actual indexing task, which is instead done by using
     * {@link #getIndexRunnable()}
     *
     * @param device The MtpDevice that should be indexed
     */
    synchronized public void setDevice(MtpDevice device) {
        if (device == mDevice) return;
        mDevice = device;
        resetState();
    }

    /**
     * Provides a Runnable for the indexing task assuming the state has already
     * been correctly initialized (by calling {@link #setDevice(MtpDevice)}) and
     * has not already been run.
     *
     * @return Runnable for the main indexing task
     */
    synchronized public Runnable getIndexRunnable() {
        if (mProgress != Progress.Initialized) return null;
        mProgress = Progress.Pending;
        return new IndexRunnable(mDevice);
    }

    synchronized public boolean indexReady() {
        return mProgress == Progress.Finished;
    }

    synchronized public Progress getProgress() {
        return mProgress;
    }

    /**
     * @param listener Listener to change to
     * @return Progress at the time the listener was added (useful for
     *         configuring initial UI state)
     */
    synchronized public Progress setProgressListener(ProgressListener listener) {
        mProgressListener = listener;
        return mProgress;
    }

    /**
     * Make the listener null if it matches the argument
     *
     * @param listener Listener to unset, if currently registered
     */
    synchronized public void unsetProgressListener(ProgressListener listener) {
        if (mProgressListener == listener)
            mProgressListener = null;
    }

    /**
     * @return The total number of elements in the index (labels and items)
     */
    public int size() {
        return mProgress == Progress.Finished ? mUnifiedLookupIndex.length : 0;
    }

    /**
     * @param position Index of item to fetch, where 0 is the first item in the
     *            specified order
     * @param order
     * @return the bucket label or MtpObjectInfo at the specified position and
     *         order
     */
    public Object get(int position, SortOrder order) {
        if (mProgress != Progress.Finished) return null;
        if(order == SortOrder.Ascending) {
            DateBucket bucket = mBuckets[mUnifiedLookupIndex[position]];
            if (bucket.unifiedStartIndex == position) {
                return bucket.bucket;
            } else {
                return mMtpObjects[bucket.itemsStartIndex + position - 1
                                   - bucket.unifiedStartIndex];
            }
        } else {
            int zeroIndex = mUnifiedLookupIndex.length - 1 - position;
            DateBucket bucket = mBuckets[mUnifiedLookupIndex[zeroIndex]];
            if (bucket.unifiedEndIndex == zeroIndex) {
                return bucket.bucket;
            } else {
                return mMtpObjects[bucket.itemsStartIndex + zeroIndex
                                   - bucket.unifiedStartIndex];
            }
        }
    }

    /**
     * @param position Index of item to fetch from a view of the data that doesn't
     *            include labels and is in the specified order
     * @return position-th item in specified order, when not including labels
     */
    public MtpObjectInfo getWithoutLabels(int position, SortOrder order) {
        if (mProgress != Progress.Finished) return null;
        if (order == SortOrder.Ascending) {
            return mMtpObjects[position];
        } else {
            return mMtpObjects[mMtpObjects.length - 1 - position];
        }
    }

    /**
     * Although this is O(log(number of buckets)), and thus should not be used
     * in hotspots, even if the attached device has items for every day for
     * a five-year timeframe, it would still only take 11 iterations at most,
     * so shouldn't be a huge issue.
     * @param position Index of item to map from a view of the data that doesn't
     *            include labels and is in the specified order
     * @param order
     * @return position in a view of the data that does include labels
     */
    public int getPositionFromPositionWithoutLabels(int position, SortOrder order) {
        if (mProgress != Progress.Finished) return -1;
        if (order == SortOrder.Descending) {
            position = mMtpObjects.length - 1 - position;
        }
        int bucketNumber = 0;
        int iMin = 0;
        int iMax = mBuckets.length - 1;
        while (iMax >= iMin) {
            int iMid = (iMax + iMin) / 2;
            if (mBuckets[iMid].itemsStartIndex + mBuckets[iMid].numItems <= position) {
                iMin = iMid + 1;
            } else if (mBuckets[iMid].itemsStartIndex > position) {
                iMax = iMid - 1;
            } else {
                bucketNumber = iMid;
                break;
            }
        }
        if (mBuckets.length == 0 || mUnifiedLookupIndex.length == 0) {
            return -1;
        }
        int mappedPos = mBuckets[bucketNumber].unifiedStartIndex
                + position - mBuckets[bucketNumber].itemsStartIndex;
        if (order == SortOrder.Descending) {
            mappedPos = mUnifiedLookupIndex.length - 1 - mappedPos;
        }
        return mappedPos;
    }

    public int getPositionWithoutLabelsFromPosition(int position, SortOrder order) {
        if (mProgress != Progress.Finished) return -1;
        if(order == SortOrder.Ascending) {
            DateBucket bucket = mBuckets[mUnifiedLookupIndex[position]];
            if (bucket.unifiedStartIndex == position) position++;
            return bucket.itemsStartIndex + position - 1 - bucket.unifiedStartIndex;
        } else {
            int zeroIndex = mUnifiedLookupIndex.length - 1 - position;
            if (mBuckets.length == 0 || mUnifiedLookupIndex.length == 0) {
                return -1;
            }
            DateBucket bucket = mBuckets[mUnifiedLookupIndex[zeroIndex]];
            if (bucket.unifiedEndIndex == zeroIndex) zeroIndex--;
            return mMtpObjects.length - 1 - bucket.itemsStartIndex
                    - zeroIndex + bucket.unifiedStartIndex;
        }
    }

    /**
     * @return The number of MTP items in the index (without labels)
     */
    public int sizeWithoutLabels() {
        return mProgress == Progress.Finished ? mMtpObjects.length : 0;
    }

    public int getFirstPositionForBucketNumber(int bucketNumber, SortOrder order) {
        if (order == SortOrder.Ascending) {
            return mBuckets[bucketNumber].unifiedStartIndex;
        } else {
            return mUnifiedLookupIndex.length - mBuckets[mBuckets.length - 1 - bucketNumber].unifiedEndIndex - 1;
        }
    }

    public int getBucketNumberForPosition(int position, SortOrder order) {
        if (order == SortOrder.Ascending) {
            return mUnifiedLookupIndex[position];
        } else {
            return mBuckets.length - 1 - mUnifiedLookupIndex[mUnifiedLookupIndex.length - 1 - position];
        }
    }

    public boolean isFirstInBucket(int position, SortOrder order) {
        if (order == SortOrder.Ascending) {
            return mBuckets[mUnifiedLookupIndex[position]].unifiedStartIndex == position;
        } else {
            position = mUnifiedLookupIndex.length - 1 - position;
            return mBuckets[mUnifiedLookupIndex[position]].unifiedEndIndex == position;
        }
    }

    private Object[] mCachedReverseBuckets;

    public Object[] getBuckets(SortOrder order) {
        if (mBuckets == null) return null;
        if (order == SortOrder.Ascending) {
            return mBuckets;
        } else {
            if (mCachedReverseBuckets == null) {
                computeReversedBuckets();
            }
            return mCachedReverseBuckets;
        }
    }

    /*
     * See the comments for buildLookupIndex for notes on the specific fields of
     * this class.
     */
    private class DateBucket implements Comparable<DateBucket> {
        SimpleDate bucket;
        List<MtpObjectInfo> tempElementsList = new ArrayList<MtpObjectInfo>();
        int unifiedStartIndex;
        int unifiedEndIndex;
        int itemsStartIndex;
        int numItems;

        public DateBucket(SimpleDate bucket) {
            this.bucket = bucket;
        }

        public DateBucket(SimpleDate bucket, MtpObjectInfo firstElement) {
            this(bucket);
            tempElementsList.add(firstElement);
        }

        void sortElements(Comparator<MtpObjectInfo> comparator) {
            Collections.sort(tempElementsList, comparator);
        }

        @Override
        public String toString() {
            return bucket.toString();
        }

        @Override
        public int hashCode() {
            return bucket.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (!(obj instanceof DateBucket)) return false;
            DateBucket other = (DateBucket) obj;
            if (bucket == null) {
                if (other.bucket != null) return false;
            } else if (!bucket.equals(other.bucket)) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(DateBucket another) {
            return this.bucket.compareTo(another.bucket);
        }
    }

    /**
     * Comparator to sort MtpObjectInfo objects by date created.
     */
    private static class MtpObjectTimestampComparator implements Comparator<MtpObjectInfo> {
        @Override
        public int compare(MtpObjectInfo o1, MtpObjectInfo o2) {
            long diff = o1.getDateCreated() - o2.getDateCreated();
            if (diff < 0) {
                return -1;
            } else if (diff == 0) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    private void resetState() {
        mGeneration++;
        mUnifiedLookupIndex = null;
        mMtpObjects = null;
        mBuckets = null;
        mCachedReverseBuckets = null;
        mProgress = (mDevice == null) ? Progress.Uninitialized : Progress.Initialized;
    }


    private class IndexRunnable implements Runnable {
        private int[] mUnifiedLookupIndex;
        private MtpObjectInfo[] mMtpObjects;
        private DateBucket[] mBuckets;
        private Map<SimpleDate, DateBucket> mBucketsTemp;
        private MtpDevice mDevice;
        private int mNumObjects = 0;

        private class IndexingException extends Exception {};

        public IndexRunnable(MtpDevice device) {
            mDevice = device;
        }

        /*
         * Implementation note: this is the way the index supports a lot of its operations in
         * constant time and respecting the need to have bucket names always come before items
         * in that bucket when accessing the list sequentially, both in ascending and descending
         * orders.
         *
         * Let's say the data we have in the index is the following:
         *  [Bucket A]: [photo 1], [photo 2]
         *  [Bucket B]: [photo 3]
         *
         *  In this case, the lookup index array would be
         *  [0, 0, 0, 1, 1]
         *
         *  Now, whether we access the list in ascending or descending order, we know which bucket
         *  to look in (0 corresponds to A and 1 to B), and can return the bucket label as the first
         *  item in a bucket as needed. The individual IndexBUckets have a startIndex and endIndex
         *  that correspond to indices in this lookup index array, allowing us to calculate the
         *  offset of the specific item we want from within a specific bucket.
         */
        private void buildLookupIndex() {
            int numBuckets = mBuckets.length;
            mUnifiedLookupIndex = new int[mNumObjects + numBuckets];
            int currentUnifiedIndexEntry = 0;
            int nextUnifiedEntry;

            mMtpObjects = new MtpObjectInfo[mNumObjects];
            int currentItemsEntry = 0;
            for (int i = 0; i < numBuckets; i++) {
                DateBucket bucket = mBuckets[i];
                nextUnifiedEntry = currentUnifiedIndexEntry + bucket.tempElementsList.size() + 1;
                Arrays.fill(mUnifiedLookupIndex, currentUnifiedIndexEntry, nextUnifiedEntry, i);
                bucket.unifiedStartIndex = currentUnifiedIndexEntry;
                bucket.unifiedEndIndex = nextUnifiedEntry - 1;
                currentUnifiedIndexEntry = nextUnifiedEntry;

                bucket.itemsStartIndex = currentItemsEntry;
                bucket.numItems = bucket.tempElementsList.size();
                for (int j = 0; j < bucket.numItems; j++) {
                    mMtpObjects[currentItemsEntry] = bucket.tempElementsList.get(j);
                    currentItemsEntry++;
                }
                bucket.tempElementsList = null;
            }
        }

        private void copyResults() {
            MtpDeviceIndex.this.mUnifiedLookupIndex = mUnifiedLookupIndex;
            MtpDeviceIndex.this.mMtpObjects = mMtpObjects;
            MtpDeviceIndex.this.mBuckets = mBuckets;
            mUnifiedLookupIndex = null;
            mMtpObjects = null;
            mBuckets = null;
        }

        @Override
        public void run() {
            try {
                indexDevice();
            } catch (IndexingException e) {
                synchronized (MtpDeviceIndex.this) {
                    resetState();
                    if (mProgressListener != null) {
                        mProgressListener.onIndexFinish();
                    }
                }
            }
        }

        private void indexDevice() throws IndexingException {
            synchronized (MtpDeviceIndex.this) {
                mProgress = Progress.Started;
            }
            mBucketsTemp = new HashMap<SimpleDate, DateBucket>();
            for (int storageId : mDevice.getStorageIds()) {
                if (mDevice != getDevice()) throw new IndexingException();
                Stack<Integer> pendingDirectories = new Stack<Integer>();
                pendingDirectories.add(0xFFFFFFFF); // start at the root of the device
                while (!pendingDirectories.isEmpty()) {
                    if (mDevice != getDevice()) throw new IndexingException();
                    int dirHandle = pendingDirectories.pop();
                    for (int objectHandle : mDevice.getObjectHandles(storageId, 0, dirHandle)) {
                        MtpObjectInfo objectInfo = mDevice.getObjectInfo(objectHandle);
                        if (objectInfo == null) throw new IndexingException();
                        int format = objectInfo.getFormat();
                        if (format == MtpConstants.FORMAT_ASSOCIATION) {
                            pendingDirectories.add(objectHandle);
                        } else if (SUPPORTED_IMAGE_FORMATS.contains(format)
                                || SUPPORTED_VIDEO_FORMATS.contains(format)) {
                            addObject(objectInfo);
                        }
                    }
                }
            }
            Collection<DateBucket> values = mBucketsTemp.values();
            mBucketsTemp = null;
            mBuckets = values.toArray(new DateBucket[values.size()]);
            values = null;
            synchronized (MtpDeviceIndex.this) {
                mProgress = Progress.Sorting;
                if (mProgressListener != null) {
                    mProgressListener.onSorting();
                }
            }
            sortAll();
            buildLookupIndex();
            synchronized (MtpDeviceIndex.this) {
                if (mDevice != getDevice()) throw new IndexingException();
                copyResults();

                /*
                 * In order for getBuckets to operate in constant time for descending
                 * order, we must precompute a reversed array of the buckets, mainly
                 * because the android.widget.SectionIndexer interface which adapters
                 * that call getBuckets implement depends on section numbers to be
                 * ascending relative to the scroll position, so we must have this for
                 * descending order or the scrollbar goes crazy.
                 */
                computeReversedBuckets();

                mProgress = Progress.Finished;
                if (mProgressListener != null) {
                    mProgressListener.onIndexFinish();
                }
            }
        }

        private SimpleDate mDateInstance = new SimpleDate();

        private void addObject(MtpObjectInfo objectInfo) {
            mNumObjects++;
            mDateInstance.setTimestamp(objectInfo.getDateCreated());
            DateBucket bucket = mBucketsTemp.get(mDateInstance);
            if (bucket == null) {
                bucket = new DateBucket(mDateInstance, objectInfo);
                mBucketsTemp.put(mDateInstance, bucket);
                mDateInstance = new SimpleDate(); // only create new date
                                                  // objects when they are used
                return;
            } else {
                bucket.tempElementsList.add(objectInfo);
            }
            if (mProgressListener != null) {
                mProgressListener.onObjectIndexed(objectInfo, mNumObjects);
            }
        }

        private void sortAll() {
            Arrays.sort(mBuckets);
            for (DateBucket bucket : mBuckets) {
                bucket.sortElements(sMtpObjectComparator);
            }
        }

    }

    private void computeReversedBuckets() {
        mCachedReverseBuckets = new Object[mBuckets.length];
        for (int i = 0; i < mCachedReverseBuckets.length; i++) {
            mCachedReverseBuckets[i] = mBuckets[mBuckets.length - 1 - i];
        }
    }
}
