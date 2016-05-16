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

package android.media.cts;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A simple reader for an IVF file.
 *
 * IVF format is a simple container format for VP8 encoded frames.
 * This reader is capable of getting frame count, width and height
 * from the header, and access individual frames randomly by
 * frame number.
 */

public class IvfReader {
    private static final byte HEADER_END = 32;
    private static final byte FOURCC_HEAD = 8;
    private static final byte WIDTH_HEAD = 12;
    private static final byte HEIGHT_HEAD = 14;
    private static final byte FRAMECOUNT_HEAD = 24;
    private static final byte FRAME_HEADER_SIZE = 12;

    private RandomAccessFile mIvfFile;
    private boolean mHeaderValid;
    private int mWidth;
    private int mHeight;
    private int mFrameCount;
    private int[] mFrameHeads;  // Head of frame header
    private int[] mFrameSizes;  // Frame size excluding header


    /**
     * Initializes the IVF file reader.
     *
     * Only minimal verification is done to check if this
     * is indeed a valid IVF file. (fourcc, signature)
     *
     * All frame headers are read in advance.
     *
     * @param filename   name of the IVF file
     */
    public IvfReader(String filename) throws IOException{
        mIvfFile = new RandomAccessFile(filename, "r");

        mHeaderValid = verifyHeader();
        readHeaderData();
        readFrameMetadata();
    }

    /**
     * Tells if file header seems to be valid.
     *
     * Only minimal verification is done to check if this
     * is indeed a valid IVF file. (fourcc, signature)
     */
    public boolean isHeaderValid(){
        return mHeaderValid;
    }

    /**
     * Returns frame width according to header information.
     */
    public int getWidth(){
        return mWidth;
    }

    /**
     * Returns frame height according to header information.
     */
    public int getHeight(){
        return mHeight;
    }

    /**
     * Returns frame count according to header information.
     */
    public int getFrameCount(){
        return mFrameCount;
    }

    /**
     * Returns frame data by index.
     *
     * @param frameIndex index of the frame to read, greater-equal
     * than 0 and less than frameCount.
     */
    public byte[] readFrame(int frameIndex) throws IOException {
        if (frameIndex > mFrameCount | frameIndex < 0){
            return null;
        }
        int frameSize = mFrameSizes[frameIndex];
        int frameHead = mFrameHeads[frameIndex];

        byte[] frame = new byte[frameSize];
        mIvfFile.seek(frameHead + FRAME_HEADER_SIZE);
        mIvfFile.read(frame);

        return frame;
    }

    /**
     * Closes IVF file.
     */
    public void close() throws IOException{
        mIvfFile.close();
    }

    private boolean verifyHeader() throws IOException{
        mIvfFile.seek(0);

        if (mIvfFile.length() < HEADER_END){
            return false;
        }

        // DKIF signature
        boolean signatureMatch = ((mIvfFile.readByte() == (byte)'D') &&
                (mIvfFile.readByte() == (byte)'K') &&
                (mIvfFile.readByte() == (byte)'I') &&
                (mIvfFile.readByte() == (byte)'F'));

        // Fourcc
        mIvfFile.seek(FOURCC_HEAD);
        boolean fourccMatch = ((mIvfFile.readByte() == (byte)'V') &&
                (mIvfFile.readByte() == (byte)'P') &&
                (mIvfFile.readByte() == (byte)'8') &&
                (mIvfFile.readByte() == (byte)'0'));

        return signatureMatch && fourccMatch;
    }

    private void readHeaderData() throws IOException{
        // width
        mIvfFile.seek(WIDTH_HEAD);
        mWidth = (int) changeEndianness(mIvfFile.readShort());

        // height
        mIvfFile.seek(HEIGHT_HEAD);
        mHeight = (int) changeEndianness(mIvfFile.readShort());

        // frame count
        mIvfFile.seek(FRAMECOUNT_HEAD);
        mFrameCount = changeEndianness(mIvfFile.readInt());

        // allocate frame metadata
        mFrameHeads = new int[mFrameCount];
        mFrameSizes = new int[mFrameCount];
    }

    private void readFrameMetadata() throws IOException{
        int frameHead = HEADER_END;
        for(int i = 0; i < mFrameCount; i++){
            mIvfFile.seek(frameHead);
            int frameSize = changeEndianness(mIvfFile.readInt());
            mFrameHeads[i] = frameHead;
            mFrameSizes[i] = frameSize;
            // next frame
            frameHead += FRAME_HEADER_SIZE + frameSize;
        }
    }

    private static short changeEndianness(short value){
        // Rationale for down-cast;
        // Java Language specification 15.19:
        //  "The type of the shift expression is the promoted type of the left-hand operand."
        // Java Language specification 5.6:
        //  "...if the operand is of compile-time type byte, short, or char,
        //  unary numeric promotion promotes it to a value of type int by a widening conversion."
        return (short) (((value << 8) & 0XFF00) | ((value >> 8) & 0X00FF));
    }

    private static int changeEndianness(int value){
        return (((value << 24) & 0XFF000000) |
                ((value << 8)  & 0X00FF0000) |
                ((value >> 8)  & 0X0000FF00) |
                ((value >> 24) & 0X000000FF));
    }
}
