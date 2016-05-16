/*
 * Copyright (C) 2009 The Android Open Source Project
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

package dex.reader;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class DexBuffer {

    private ByteBuffer b;

    public DexBuffer(String fileName) throws IOException {

        // FIXME channel? allocate fix size?
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);

            BufferedInputStream bis = new BufferedInputStream(fis);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = bis.read(buf)) > 0) {
                bos.write(buf, 0, len);
            }
            byte[] bytes = bos.toByteArray();
            initialize(ByteBuffer.wrap(bytes));
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    public DexBuffer(byte[] bytes) {
        initialize(ByteBuffer.wrap(bytes));
    }

    private DexBuffer(ByteBuffer slice) {
        initialize(slice);
    }

    private void initialize(ByteBuffer buffer) {
        b = buffer.asReadOnlyBuffer();
        b.clear();
        b.order(ByteOrder.LITTLE_ENDIAN);
    }

    public void setPosition(int offset) {
        b.position(offset);
    }

    public void readBytes(byte[] dst) {
        b.get(dst, 0, dst.length);
    }

    /**
     * FIXME make endian dependent
     */
    public int readUleb128() {
        int endValue = 0;
        int value = 0;
        int nr = 0;
        do {
            value = (b.get() & 0xFF);
            endValue |= ((value & 0x7F) << 7 * nr);// cut away left most bit
            nr++;
        } while ((value & 0x80) != 0); // highest bit set?
        return endValue;
    }

    /**
     * pre 0 < nBytes <=4
     */
    public int readInt(int nBytes) {
        int endValue = 0;
        int tmp = 0;
        for (int i = 0; i < nBytes; i++) {
            tmp = b.get() & 0xFF;
            endValue |= (tmp << i * 8);
        }
        return endValue;
    }

    /**
     * pre 0 < nBytes <=1 FIXME: Sign extension
     */
    public short readShort(int nBytes) {
        short endValue = 0;
        int tmp = 0;
        for (int i = 0; i < nBytes; i++) {
            tmp = b.get() & 0xFF;
            endValue |= (tmp << i * 8);
        }
        return endValue;
    }

    /**
     * pre 0 < nBytes <=1
     */
    public char readChar(int nBytes) {
        char endValue = 0;
        int tmp = 0;
        for (int i = 0; i < nBytes; i++) {
            tmp = b.get() & 0xFF;
            endValue |= (tmp << i * 8);
        }
        return endValue;
    }

    /**
     * pre 0 < nBytes <=7 FIXME: Sign extension
     */
    public long readLong(int nBytes) {
        long endValue = 0;
        int tmp = 0;
        for (int i = 0; i < nBytes; i++) {
            tmp = b.get() & 0xFF;
            endValue |= (tmp << i * 8);
        }
        return endValue;
    }

    /**
     * pre 0 < nBytes <=4
     */
    public float readFloat(int nBytes) {
        int bits = readInt(nBytes);
        int bytesToMove = (4 - nBytes) * 8;
        bits <<= bytesToMove;
        return Float.intBitsToFloat(bits);
    }

    // returns int form current position
    public int readUInt() {
        int value = b.getInt();
        // assert value >= 0;
        return value;
    }

    public int readUShort() {
        return b.getShort() & 0xFFFF;
    }

    // returns byte form current position
    public byte readUByte() {
        return b.get();
    }

    public DexBuffer createCopy() {
        return new DexBuffer(b.duplicate());
    }

    public double readDouble(int nBytes) {
        long bits = readLong(nBytes);
        int bytesToMove = (8 - nBytes) * 8;
        bits <<= bytesToMove;
        return Double.longBitsToDouble(bits);
    }

    public void skip(int nBytes) {
        b.position(b.position() + nBytes);
    }
}
