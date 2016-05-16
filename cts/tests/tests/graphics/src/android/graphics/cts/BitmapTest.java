/*
 * Copyright (C) 2008 The Android Open Source Project
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
package android.graphics.cts;

import com.android.cts.stub.R;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.os.Parcel;
import android.test.AndroidTestCase;
import android.util.DisplayMetrics;
import android.widget.cts.WidgetTestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class BitmapTest extends AndroidTestCase {
    private Resources mRes;
    private Bitmap mBitmap;
    private BitmapFactory.Options mOptions;

    // small alpha values cause color values to be pre-multiplied down, losing accuracy
    private final int PREMUL_COLOR = Color.argb(2, 255, 254, 253);
    private final int PREMUL_ROUNDED_COLOR = Color.argb(2, 255, 255, 255);
    private final int PREMUL_STORED_COLOR = Color.argb(2, 2, 2, 2);

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mRes = getContext().getResources();
        mOptions = new BitmapFactory.Options();
        mOptions.inScaled = false;
        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.start, mOptions);
    }

    public void testCompress(){
        mBitmap.recycle();

        //abnormal case: the bitmap has been recycled
        try{
            mBitmap.compress(CompressFormat.JPEG, 0, null);
            fail("shouldn't come to here");
        }catch(IllegalStateException e){
        }

        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.start, mOptions);

        // abnormal case: out stream is null
        try{
            mBitmap.compress(CompressFormat.JPEG, 0, null);
            fail("shouldn't come to here");
        }catch(NullPointerException e){
        }

        // abnormal case: quality less than 0
        try{
            mBitmap.compress(CompressFormat.JPEG, -1, new ByteArrayOutputStream());
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        // abnormal case: quality bigger than 100
        try{
            mBitmap.compress(CompressFormat.JPEG, 101, new ByteArrayOutputStream());
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        //normal case
        assertTrue(mBitmap.compress(CompressFormat.JPEG, 50, new ByteArrayOutputStream()));
    }

    public void testCopy(){
        mBitmap.recycle();

        //abnormal case: the bitmap has been recycled
        try{
            mBitmap.copy(Config.RGB_565, false);
            fail("shouldn't come to here");
        }catch(IllegalStateException e){
            // expected
        }

        mBitmap = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
        Bitmap bitmap = mBitmap.copy(Config.ARGB_8888, false);
        WidgetTestUtils.assertEquals(mBitmap, bitmap);
    }

    public void testCopyPixelsToBuffer(){
        final int pixSize = mBitmap.getRowBytes() * mBitmap.getHeight();
        final int tooSmall = pixSize / 2;

        // abnormal case: unsupported Buffer subclass
        try{
            mBitmap.copyPixelsToBuffer(CharBuffer.allocate(pixSize));
            fail("shouldn't come to here");
        }catch(RuntimeException e1){
        }

        // abnormal case: Buffer not large enough for pixels
        try{
            mBitmap.copyPixelsToBuffer(ByteBuffer.allocate(tooSmall));
            fail("shouldn't come to here");
        }catch(RuntimeException e2){
        }

        // normal case
        ByteBuffer byteBuf = ByteBuffer.allocate(pixSize);
        assertEquals(0, byteBuf.position());
        mBitmap.copyPixelsToBuffer(byteBuf);
        assertEquals(pixSize, byteBuf.position());

        // abnormal case: Buffer not large enough for pixels
        try{
            mBitmap.copyPixelsToBuffer(ByteBuffer.allocate(tooSmall));
            fail("shouldn't come to here");
        }catch(RuntimeException e3){
        }

        // normal case
        ShortBuffer shortBuf = ShortBuffer.allocate(pixSize);
        assertEquals(0, shortBuf.position());
        mBitmap.copyPixelsToBuffer(shortBuf);
        assertEquals(pixSize >> 1, shortBuf.position());

        // abnormal case: Buffer not large enough for pixels
        try{
            mBitmap.copyPixelsToBuffer(ByteBuffer.allocate(tooSmall));
            fail("shouldn't come to here");
        }catch(RuntimeException e4){
        }

        // normal case
        IntBuffer intBuf1 = IntBuffer.allocate(pixSize);
        assertEquals(0, intBuf1.position());
        mBitmap.copyPixelsToBuffer(intBuf1);
        assertEquals(pixSize >> 2, intBuf1.position());

        Bitmap bitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(),
                mBitmap.getConfig());
        intBuf1.position(0); // copyPixelsToBuffer adjusted the position, so rewind to start
        bitmap.copyPixelsFromBuffer(intBuf1);
        IntBuffer intBuf2 = IntBuffer.allocate(pixSize);
        bitmap.copyPixelsToBuffer(intBuf2);

        assertEquals(pixSize >> 2, intBuf2.position());
        assertEquals(intBuf1.position(), intBuf2.position());
        int size = intBuf1.position();
        intBuf1.position(0);
        intBuf2.position(0);
        for (int i = 0; i < size; i++) {
            assertEquals("mismatching pixels at position " + i, intBuf1.get(), intBuf2.get());
        }
    }

    public void testCreateBitmap1(){
        int[] colors = createColors(100);
        Bitmap bitmap = Bitmap.createBitmap(colors, 10, 10, Config.RGB_565);
        Bitmap ret = Bitmap.createBitmap(bitmap);
        assertNotNull(ret);
        assertEquals(10, ret.getWidth());
        assertEquals(10, ret.getHeight());
        assertEquals(Config.RGB_565, ret.getConfig());
    }

    public void testCreateBitmap2(){
        //abnormal case: Illegal Argument
        try{
            Bitmap.createBitmap(mBitmap, -100, 50, 50, 200);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        // special case: output bitmap is equal to the input bitmap
        mBitmap = Bitmap.createBitmap(new int[100 * 100], 100, 100, Config.ARGB_8888);
        Bitmap ret = Bitmap.createBitmap(mBitmap, 0, 0, 100, 100);
        assertNotNull(ret);
        assertTrue(mBitmap.equals(ret));

        //normal case
        mBitmap = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
        ret = Bitmap.createBitmap(mBitmap, 10, 10, 50, 50);
        assertNotNull(ret);
        assertFalse(mBitmap.equals(ret));
    }

    public void testCreateBitmap3(){
        mBitmap = Bitmap.createBitmap(100, 100, Config.ARGB_8888);

        //abnormal case: x and/or y less than 0
        try{
            Bitmap.createBitmap(mBitmap, -1, -1, 10, 10, null, false);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        //abnormal case: width and/or height less than 0
        try{
            Bitmap.createBitmap(mBitmap, 1, 1, -10, -10, null, false);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        //abnormal case: (x + width) bigger than source bitmap's width
        try{
            Bitmap.createBitmap(mBitmap, 10, 10, 95, 50, null, false);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        //abnormal case: (y + height) bigger than source bitmap's height
        try{
            Bitmap.createBitmap(mBitmap, 10, 10, 50, 95, null, false);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        // special case: output bitmap is equal to the input bitmap
        mBitmap = Bitmap.createBitmap(new int[100 * 100], 100, 100, Config.ARGB_8888);
        Bitmap ret = Bitmap.createBitmap(mBitmap, 0, 0, 100, 100, null, false);
        assertNotNull(ret);
        assertTrue(mBitmap.equals(ret));

        // normal case
        mBitmap = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
        ret = Bitmap.createBitmap(mBitmap, 10, 10, 50, 50, new Matrix(), true);
        assertNotNull(ret);
        assertFalse(mBitmap.equals(ret));
    }

    public void testCreateBitmap4(){
        Bitmap ret = Bitmap.createBitmap(100, 200, Config.RGB_565);
        assertNotNull(ret);
        assertEquals(100, ret.getWidth());
        assertEquals(200, ret.getHeight());
        assertEquals(Config.RGB_565, ret.getConfig());
    }

    public void testCreateBitmap6(){
        int[] colors = createColors(100);

        //abnormal case: width and/or height less than 0
        try{
            Bitmap.createBitmap(colors, 0, 100, -1, 100, Config.RGB_565);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        //abnormal case: stride less than width and bigger than -width
        try{
            Bitmap.createBitmap(colors, 10, 10, 100, 100, Config.RGB_565);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        //abnormal case: offset less than 0
        try{
            Bitmap.createBitmap(colors, -10, 100, 100, 100, Config.RGB_565);
            fail("shouldn't come to here");
        }catch(ArrayIndexOutOfBoundsException e){
        }

        //abnormal case: (offset + width) bigger than colors' length
        try{
            Bitmap.createBitmap(colors, 10, 100, 100, 100, Config.RGB_565);
            fail("shouldn't come to here");
        }catch(ArrayIndexOutOfBoundsException e){
        }

        //abnormal case: (lastScanline + width) bigger than colors' length
        try{
            Bitmap.createBitmap(colors, 10, 100, 50, 100, Config.RGB_565);
            fail("shouldn't come to here");
        }catch(ArrayIndexOutOfBoundsException e){
        }

        // normal case
        Bitmap ret = Bitmap.createBitmap(colors, 5, 10, 10, 5, Config.RGB_565);
        assertNotNull(ret);
        assertEquals(10, ret.getWidth());
        assertEquals(5, ret.getHeight());
        assertEquals(Config.RGB_565, ret.getConfig());
    }

    public void testCreateScaledBitmap(){
        mBitmap = Bitmap.createBitmap(100, 200, Config.RGB_565);
        Bitmap ret = Bitmap.createScaledBitmap(mBitmap, 50, 100, false);
        assertNotNull(ret);
        assertEquals(50, ret.getWidth());
        assertEquals(100, ret.getHeight());
    }

    public void testDescribeContents(){
        assertEquals(0, mBitmap.describeContents());
    }

    public void testEraseColor(){
        mBitmap.recycle();

        //abnormal case: the bitmap has been recycled
        try{
            mBitmap.eraseColor(0);
            fail("shouldn't come to here");
        }catch(IllegalStateException e){
        }

        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.start, mOptions);

        //abnormal case: bitmap is immutable
        try{
            mBitmap.eraseColor(0);
            fail("shouldn't come to here");
        }catch(IllegalStateException e){
        }

        // normal case
        mBitmap = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
        mBitmap.eraseColor(0xffff0000);
        assertEquals(0xffff0000, mBitmap.getPixel(10, 10));
        assertEquals(0xffff0000, mBitmap.getPixel(50, 50));
    }

    public void testExtractAlpha1(){
        mBitmap.recycle();

        //abnormal case: the bitmap has been recycled
        try{
            mBitmap.extractAlpha();
            fail("shouldn't come to here");
        }catch(IllegalStateException e){
        }

        // normal case
        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.start, mOptions);
        Bitmap ret = mBitmap.extractAlpha();
        assertNotNull(ret);
        int color = ret.getPixel(10, 20);
        assertEquals(0x00, Color.alpha(color));
    }

    public void testExtractAlpha2(){
        mBitmap.recycle();

        //abnormal case: the bitmap has been recycled
        try{
            mBitmap.extractAlpha(new Paint(), new int[]{0, 1});
            fail("shouldn't come to here");
        }catch(IllegalStateException e){
        }

        // normal case
        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.start, mOptions);
        Bitmap ret = mBitmap.extractAlpha(new Paint(), new int[]{0, 1});
        assertNotNull(ret);
        int color = ret.getPixel(10, 20);
        assertEquals(0x00, Color.alpha(color));
    }

    public void testGetAllocationByteCount() {
        mBitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ALPHA_8);
        int alloc = mBitmap.getAllocationByteCount();
        assertEquals(mBitmap.getByteCount(), alloc);

        // reconfigure same size
        mBitmap.reconfigure(50, 100, Bitmap.Config.ARGB_8888);
        assertEquals(mBitmap.getByteCount(), alloc);
        assertEquals(mBitmap.getAllocationByteCount(), alloc);

        // reconfigure different size
        mBitmap.reconfigure(10, 10, Bitmap.Config.ALPHA_8);
        assertEquals(mBitmap.getByteCount(), 100);
        assertEquals(mBitmap.getAllocationByteCount(), alloc);
    }

    public void testGetConfig(){
        Bitmap bm0 = Bitmap.createBitmap(100, 200, Bitmap.Config.ALPHA_8);
        Bitmap bm1 = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        Bitmap bm2 = Bitmap.createBitmap(100, 200, Bitmap.Config.RGB_565);
        Bitmap bm3 = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_4444);

        assertEquals(Bitmap.Config.ALPHA_8, bm0.getConfig());
        assertEquals(Bitmap.Config.ARGB_8888, bm1.getConfig());
        assertEquals(Bitmap.Config.RGB_565, bm2.getConfig());
        // Attempting to create a 4444 bitmap actually creates an 8888 bitmap.
        assertEquals(Bitmap.Config.ARGB_8888, bm3.getConfig());
    }

    public void testGetHeight(){
        assertEquals(31, mBitmap.getHeight());
        mBitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        assertEquals(200, mBitmap.getHeight());
    }

    public void testGetNinePatchChunk(){
        assertNull(mBitmap.getNinePatchChunk());
    }

    public void testGetPixel(){
        mBitmap.recycle();

        //abnormal case: the bitmap has been recycled
        try{
            mBitmap.getPixel(10, 16);
            fail("shouldn't come to here");
        }catch(IllegalStateException e){
        }

        mBitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.RGB_565);

        //abnormal case: x bigger than the source bitmap's width
        try{
            mBitmap.getPixel(200, 16);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        //abnormal case: y bigger than the source bitmap's height
        try{
            mBitmap.getPixel(10, 300);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        // normal case
        mBitmap.setPixel(10, 16, 0xFF << 24);
        assertEquals(0xFF << 24, mBitmap.getPixel(10, 16));
    }

    public void testGetRowBytes(){
        Bitmap bm0 = Bitmap.createBitmap(100, 200, Bitmap.Config.ALPHA_8);
        Bitmap bm1 = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        Bitmap bm2 = Bitmap.createBitmap(100, 200, Bitmap.Config.RGB_565);
        Bitmap bm3 = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_4444);

        assertEquals(100, bm0.getRowBytes());
        assertEquals(400, bm1.getRowBytes());
        assertEquals(200, bm2.getRowBytes());
        // Attempting to create a 4444 bitmap actually creates an 8888 bitmap.
        assertEquals(400, bm3.getRowBytes());
    }

    public void testGetWidth(){
        assertEquals(31, mBitmap.getWidth());
        mBitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        assertEquals(100, mBitmap.getWidth());
    }

    public void testHasAlpha(){
        assertFalse(mBitmap.hasAlpha());
        mBitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        assertTrue(mBitmap.hasAlpha());
    }

    public void testIsMutable(){
        assertFalse(mBitmap.isMutable());
        mBitmap = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
        assertTrue(mBitmap.isMutable());
    }

    public void testIsRecycled(){
        assertFalse(mBitmap.isRecycled());
        mBitmap.recycle();
        assertTrue(mBitmap.isRecycled());
    }

    public void testReconfigure() {
        mBitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.RGB_565);
        int alloc = mBitmap.getAllocationByteCount();

        // test shrinking
        mBitmap.reconfigure(50, 100, Bitmap.Config.ALPHA_8);
        assertEquals(mBitmap.getAllocationByteCount(), alloc);
        assertEquals(mBitmap.getByteCount() * 8, alloc);

        // test expanding
        try {
            mBitmap.reconfigure(101, 201, Bitmap.Config.ARGB_8888);
            fail("shouldn't come to here");
        } catch (IllegalArgumentException e) {
        }

        // test mutable
        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.start, mOptions);
        try {
            mBitmap.reconfigure(1, 1, Bitmap.Config.ALPHA_8);
            fail("shouldn't come to here");
        } catch (IllegalStateException e) {
        }
    }

    public void testSetConfig() {
        mBitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.RGB_565);
        int alloc = mBitmap.getAllocationByteCount();

        // test shrinking
        mBitmap.setConfig(Bitmap.Config.ALPHA_8);
        assertEquals(mBitmap.getAllocationByteCount(), alloc);
        assertEquals(mBitmap.getByteCount() * 2, alloc);

        // test expanding
        try {
            mBitmap.setConfig(Bitmap.Config.ARGB_8888);
            fail("shouldn't come to here");
        } catch (IllegalArgumentException e) {
        }

        // test mutable
        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.start, mOptions);
        try {
            mBitmap.setConfig(Bitmap.Config.ALPHA_8);
            fail("shouldn't come to here");
        } catch (IllegalStateException e) {
        }
    }

    public void testSetHeight() {
        mBitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        int alloc = mBitmap.getAllocationByteCount();

        // test shrinking
        mBitmap.setHeight(100);
        assertEquals(mBitmap.getAllocationByteCount(), alloc);
        assertEquals(mBitmap.getByteCount() * 2, alloc);

        // test expanding
        try {
            mBitmap.setHeight(201);
            fail("shouldn't come to here");
        } catch (IllegalArgumentException e) {
        }

        // test mutable
        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.start, mOptions);
        try {
            mBitmap.setHeight(1);
            fail("shouldn't come to here");
        } catch (IllegalStateException e) {
        }
    }

    public void testSetPixel(){
        int color = 0xff << 24;

        mBitmap.recycle();

        //abnormal case: the bitmap has been recycled
        try{
            mBitmap.setPixel(10, 16, color);
            fail("shouldn't come to here");
        }catch(IllegalStateException e){
        }

        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.start, mOptions);

        //abnormal case: the bitmap is immutable
        try{
            mBitmap.setPixel(10, 16, color);
            fail("shouldn't come to here");
        }catch(IllegalStateException e){
        }

        mBitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.RGB_565);

        //abnormal case: x bigger than the source bitmap's width
        try{
            mBitmap.setPixel(200, 16, color);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        //abnormal case: y bigger than the source bitmap's height
        try{
            mBitmap.setPixel(10, 300, color);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        // normal case
        mBitmap.setPixel(10, 16, 0xFF << 24);
        assertEquals(0xFF << 24, mBitmap.getPixel(10, 16));
    }

    public void testSetPixels(){
        int[] colors = createColors(100);

        //abnormal case: the bitmap has been recycled
        mBitmap.recycle();

        try{
            mBitmap.setPixels(colors, 0, 0, 0, 0, 0, 0);
            fail("shouldn't come to here");
        }catch(IllegalStateException e){
        }

        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.start, mOptions);

        // abnormal case: the bitmap is immutable
        try{
            mBitmap.setPixels(colors, 0, 0, 0, 0, 0, 0);
            fail("shouldn't come to here");
        }catch(IllegalStateException e){
        }

        mBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        // abnormal case: x and/or y less than 0
        try{
            mBitmap.setPixels(colors, 0, 0, -1, -1, 200, 16);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        // abnormal case: width and/or height less than 0
        try{
            mBitmap.setPixels(colors, 0, 0, 0, 0, -1, -1);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        // abnormal case: (x + width) bigger than the source bitmap's width
        try{
            mBitmap.setPixels(colors, 0, 0, 10, 10, 95, 50);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        // abnormal case: (y + height) bigger than the source bitmap's height
        try{
            mBitmap.setPixels(colors, 0, 0, 10, 10, 50, 95);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        // abnormal case: stride less than width and bigger than -width
        try{
            mBitmap.setPixels(colors, 0, 10, 10, 10, 50, 50);
            fail("shouldn't come to here");
        }catch(IllegalArgumentException e){
        }

        // abnormal case: offset less than 0
        try{
            mBitmap.setPixels(colors, -1, 50, 10, 10, 50, 50);
            fail("shouldn't come to here");
        }catch(ArrayIndexOutOfBoundsException e){
        }

        // abnormal case: (offset + width) bigger than the length of colors
        try{
            mBitmap.setPixels(colors, 60, 50, 10, 10, 50, 50);
            fail("shouldn't come to here");
        }catch(ArrayIndexOutOfBoundsException e){
        }

        // abnormal case: lastScanline less than 0
        try{
            mBitmap.setPixels(colors, 10, -50, 10, 10, 50, 50);
            fail("shouldn't come to here");
        }catch(ArrayIndexOutOfBoundsException e){
        }

        // abnormal case: (lastScanline + width) bigger than the length of colors
        try{
            mBitmap.setPixels(colors, 10, 50, 10, 10, 50, 50);
            fail("shouldn't come to here");
        }catch(ArrayIndexOutOfBoundsException e){
        }

        // normal case
        colors = createColors(100 * 100);
        mBitmap.setPixels(colors, 0, 100, 0, 0, 100, 100);
        int[] ret = new int[100 * 100];
        mBitmap.getPixels(ret, 0, 100, 0, 0, 100, 100);

        for(int i = 0; i < 10000; i++){
            assertEquals(ret[i], colors[i]);
        }
    }

    private void checkPremultipliedBitmapConfig(Config config, boolean expectedPremul) {
        Bitmap bitmap = Bitmap.createBitmap(1, 1, config);
        bitmap.setPremultiplied(true);
        bitmap.setPixel(0, 0, Color.TRANSPARENT);
        assertTrue(bitmap.isPremultiplied() == expectedPremul);

        bitmap.setHasAlpha(false);
        assertFalse(bitmap.isPremultiplied());
    }

    public void testSetPremultipliedSimple() {
        checkPremultipliedBitmapConfig(Bitmap.Config.ALPHA_8, true);
        checkPremultipliedBitmapConfig(Bitmap.Config.RGB_565, false);
        checkPremultipliedBitmapConfig(Bitmap.Config.ARGB_4444, true);
        checkPremultipliedBitmapConfig(Bitmap.Config.ARGB_8888, true);
    }

    public void testSetPremultipliedData() {
        // with premul, will store 2,2,2,2, so it doesn't get value correct
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        bitmap.setPixel(0, 0, PREMUL_COLOR);
        assertEquals(bitmap.getPixel(0, 0), PREMUL_ROUNDED_COLOR);

        // read premultiplied value directly
        bitmap.setPremultiplied(false);
        assertEquals(bitmap.getPixel(0, 0), PREMUL_STORED_COLOR);

        // value can now be stored/read correctly
        bitmap.setPixel(0, 0, PREMUL_COLOR);
        assertEquals(bitmap.getPixel(0, 0), PREMUL_COLOR);

        // verify with array methods
        int testArray[] = new int[] { PREMUL_COLOR };
        bitmap.setPixels(testArray, 0, 1, 0, 0, 1, 1);
        bitmap.getPixels(testArray, 0, 1, 0, 0, 1, 1);
        assertEquals(bitmap.getPixel(0, 0), PREMUL_COLOR);
    }

    public void testPremultipliedCanvas() {
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        bitmap.setHasAlpha(true);
        bitmap.setPremultiplied(false);
        assertFalse(bitmap.isPremultiplied());

        Canvas c = new Canvas();
        try {
            c.drawBitmap(bitmap, 0, 0, null);
            fail("canvas should fail with exception");
        } catch (RuntimeException e) {
        }
    }

    private int getBitmapRawInt(Bitmap bitmap) {
        IntBuffer buffer = IntBuffer.allocate(1);
        bitmap.copyPixelsToBuffer(buffer);
        return buffer.get(0);
    }

    private void bitmapStoreRawInt(Bitmap bitmap, int value) {
        IntBuffer buffer = IntBuffer.allocate(1);
        buffer.put(0, value);
        bitmap.copyPixelsFromBuffer(buffer);
    }

    public void testSetPremultipliedToBuffer() {
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        bitmap.setPixel(0, 0, PREMUL_COLOR);
        int storedPremul = getBitmapRawInt(bitmap);

        bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        bitmap.setPremultiplied(false);
        bitmap.setPixel(0, 0, PREMUL_STORED_COLOR);

        assertEquals(getBitmapRawInt(bitmap), storedPremul);
    }

    public void testSetPremultipliedFromBuffer() {
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        bitmap.setPremultiplied(false);
        bitmap.setPixel(0, 0, PREMUL_COLOR);
        int rawTestColor = getBitmapRawInt(bitmap);

        bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        bitmap.setPremultiplied(false);
        bitmapStoreRawInt(bitmap, rawTestColor);
        assertEquals(bitmap.getPixel(0, 0), PREMUL_COLOR);
    }

    public void testSetWidth() {
        mBitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        int alloc = mBitmap.getAllocationByteCount();

        // test shrinking
        mBitmap.setWidth(50);
        assertEquals(mBitmap.getAllocationByteCount(), alloc);
        assertEquals(mBitmap.getByteCount() * 2, alloc);

        // test expanding
        try {
            mBitmap.setWidth(101);
            fail("shouldn't come to here");
        } catch (IllegalArgumentException e) {
        }

        // test mutable
        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.start, mOptions);
        try {
            mBitmap.setWidth(1);
            fail("shouldn't come to here");
        } catch (IllegalStateException e) {
        }
    }

    public void testWriteToParcel(){
        mBitmap.recycle();

        // abnormal case: the bitmap to be written has been recycled
        try{
            mBitmap.writeToParcel(null, 0);
            fail("shouldn't come to here");
        }catch(IllegalStateException e){
        }

        // abnormal case: failed to unparcel Bitmap
        mBitmap = BitmapFactory.decodeResource(mRes, R.drawable.start, mOptions);
        Parcel p = Parcel.obtain();
        mBitmap.writeToParcel(p, 0);

        try{
            Bitmap.CREATOR.createFromParcel(p);
            fail("shouldn't come to here");
        }catch(RuntimeException e){
        }

        // normal case
        mBitmap = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
        mBitmap.writeToParcel(p, 0);
        p.setDataPosition(0);
        mBitmap.equals(Bitmap.CREATOR.createFromParcel(p));
    }

    public void testGetScaledHeight1() {
        int dummyDensity = 5;
        Bitmap ret = Bitmap.createBitmap(100, 200, Config.RGB_565);
        int scaledHeight = scaleFromDensity(ret.getHeight(), ret.getDensity(), dummyDensity);
        assertNotNull(ret);
        assertEquals(scaledHeight, ret.getScaledHeight(dummyDensity));
    }

    public void testGetScaledHeight2() {
        Bitmap ret = Bitmap.createBitmap(100, 200, Config.RGB_565);
        DisplayMetrics metrics = new DisplayMetrics();
        metrics = getContext().getResources().getDisplayMetrics();
        int scaledHeight = scaleFromDensity(ret.getHeight(), ret.getDensity(), metrics.densityDpi);
        assertEquals(scaledHeight, ret.getScaledHeight(metrics));
    }

    public void testGetScaledHeight3() {
        Bitmap ret = Bitmap.createBitmap(100, 200, Config.RGB_565);
        Bitmap mMutableBitmap = Bitmap.createBitmap(100, 200, Config.ARGB_8888);
        Canvas mCanvas = new Canvas(mMutableBitmap);
        // set Density
        mCanvas.setDensity(DisplayMetrics.DENSITY_HIGH);
        int scaledHeight = scaleFromDensity(
                ret.getHeight(), ret.getDensity(), mCanvas.getDensity());
        assertEquals(scaledHeight, ret.getScaledHeight(mCanvas));
    }

    public void testGetScaledWidth1() {
        int dummyDensity = 5;
        Bitmap ret = Bitmap.createBitmap(100, 200, Config.RGB_565);
        int scaledWidth = scaleFromDensity(ret.getWidth(), ret.getDensity(), dummyDensity);
        assertNotNull(ret);
        assertEquals(scaledWidth, ret.getScaledWidth(dummyDensity));
    }

    public void testGetScaledWidth2() {
        Bitmap ret = Bitmap.createBitmap(100, 200, Config.RGB_565);
        DisplayMetrics metrics = new DisplayMetrics();
        metrics = getContext().getResources().getDisplayMetrics();
        int scaledWidth = scaleFromDensity(ret.getWidth(), ret.getDensity(), metrics.densityDpi);
        assertEquals(scaledWidth, ret.getScaledWidth(metrics));
    }

    public void testGetScaledWidth3() {
        Bitmap ret = Bitmap.createBitmap(100, 200, Config.RGB_565);
        Bitmap mMutableBitmap = Bitmap.createBitmap(100, 200, Config.ARGB_8888);
        Canvas mCanvas = new Canvas(mMutableBitmap);
        // set Density
        mCanvas.setDensity(DisplayMetrics.DENSITY_HIGH);
        int scaledWidth = scaleFromDensity(ret.getWidth(), ret.getDensity(),  mCanvas.getDensity());
        assertEquals(scaledWidth, ret.getScaledWidth(mCanvas));
    }

    private int scaleFromDensity(int size, int sdensity, int tdensity) {
        if (sdensity == Bitmap.DENSITY_NONE || sdensity == tdensity) {
            return size;
        }

        // Scale by tdensity / sdensity, rounding up.
        return ((size * tdensity) + (sdensity >> 1)) / sdensity;
    }

    private int[] createColors(int size){
        int[] colors = new int[size];

        for (int i = 0; i < size; i++) {
            colors[i] = (0xFF << 24) | (i << 16) | (i << 8) | i;
        }

        return colors;
    }
}
