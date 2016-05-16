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

package android.text.cts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Picture;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.webkit.cts.WebViewOnUiThread;
import android.widget.TextView;
import android.widget.EditText;

public class EmojiTest extends ActivityInstrumentationTestCase2<EmojiStubActivity> {

    public EmojiTest() {
        super("com.android.cts.stub", EmojiStubActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Tests all Emoji are defined in Character class
     */
    public void testEmojiCodePoints() {
        for (int i = 0; i < EmojiConstants.emojiCodePoints.length; i++) {
            assertTrue(Character.isDefined(EmojiConstants.emojiCodePoints[i]));
        }
    }

    /**
     * Tests Emoji has different glyph for different meaning characters.
     * Test on Canvas, TextView, EditText and WebView
     */
    public void testEmojiGlyph() {
        CaptureCanvas ccanvas = new CaptureCanvas(getInstrumentation().getContext());
        CaptureWebView cwebview = new CaptureWebView(getInstrumentation().getContext());

        Bitmap mBitmapA, mBitmapB;  // Emoji displayed Bitmaps to compare

        int comparedCodePoints[][] = {   // Emojis should have different characters
            {0x1F436, 0x1F435},      // Dog(U+1F436) and Monkey(U+1F435)
            {0x26BD, 0x26BE},        // Soccer ball(U+26BD) and Baseball(U+26BE)
            {0x1F47B, 0x1F381},      // Ghost(U+1F47B) and wrapped present(U+1F381)
            {0x2764, 0x1F494},       // Heavy black heart(U+2764) and broken heart(U+1F494)
            {0x1F603, 0x1F33B}       // Smiling face with open mouth(U+1F603) and sunflower(U+1F33B)
        };

        for (int i = 0; i < comparedCodePoints.length; i++) {

            mBitmapA = ccanvas.capture(Character.toChars(comparedCodePoints[i][0]));
            mBitmapB = ccanvas.capture(Character.toChars(comparedCodePoints[i][1]));

            assertFalse(mBitmapA.sameAs(mBitmapB));

            // cannot reuse CaptureTextView as 2nd setText call throws NullPointerException
            CaptureTextView cviewA = new CaptureTextView(getInstrumentation().getContext());
            mBitmapA = cviewA.capture(Character.toChars(comparedCodePoints[i][0]));
            CaptureTextView cviewB = new CaptureTextView(getInstrumentation().getContext());
            mBitmapB = cviewB.capture(Character.toChars(comparedCodePoints[i][1]));

            assertFalse(mBitmapA.sameAs(mBitmapB));

            CaptureEditText cedittextA = new CaptureEditText(getInstrumentation().getContext());
            mBitmapA = cedittextA.capture(Character.toChars(comparedCodePoints[i][0]));
            CaptureEditText cedittextB = new CaptureEditText(getInstrumentation().getContext());
            mBitmapB = cedittextB.capture(Character.toChars(comparedCodePoints[i][1]));

            assertFalse(mBitmapA.sameAs(mBitmapB));

            mBitmapA = cwebview.capture(Character.toChars(comparedCodePoints[i][0]));
            mBitmapB = cwebview.capture(Character.toChars(comparedCodePoints[i][1]));

            assertFalse(mBitmapA.sameAs(mBitmapB));

        }
    }

    /**
     * Tests EditText handles Emoji
     */
    public void testEmojiEditable() {
        int testedCodePoints[] = {
            0xAE,    // registered mark
            0x2764,    // heavy black heart
            0x1F353    // strawberry - surrogate pair sample. Count as two characters.
        };

        String origStr, newStr;

        // delete Emoji by sending KEYCODE_DEL
        for (int i = 0; i < testedCodePoints.length; i++) {
            origStr = "Test character  ";
            // cannot reuse CaptureTextView as 2nd setText call throws NullPointerException
            EditText editText = new EditText(getInstrumentation().getContext());
            editText.setText(origStr + String.valueOf(Character.toChars(testedCodePoints[i])));

            // confirm the emoji is added.
            newStr = editText.getText().toString();
            assertEquals(newStr.codePointCount(0, newStr.length()), origStr.length() + 1);

            // Delete added character by sending KEYCODE_DEL event
            editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));

            newStr = editText.getText().toString();
            assertEquals(newStr.codePointCount(0, newStr.length()), origStr.length() + 1);
        }
    }

    private class CaptureCanvas extends View {

        String mTestStr;
        Paint paint = new Paint();

        CaptureCanvas(Context context) {
            super(context);
        }

        public void onDraw(Canvas canvas) {
            if (mTestStr != null) {
                canvas.drawText(mTestStr, 50, 50, paint);
            }
            return;
        }

        Bitmap capture(char c[]) {
            mTestStr = String.valueOf(c);
            invalidate();

            setDrawingCacheEnabled(true);
            measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            layout(0, 0, 200,200);

            Bitmap bitmap = Bitmap.createBitmap(getDrawingCache());
            setDrawingCacheEnabled(false);
            return bitmap;
        }

    }

    private class CaptureTextView extends TextView {

        CaptureTextView(Context context) {
            super(context);
        }

        Bitmap capture(char c[]) {
            setText(String.valueOf(c));

            invalidate();

            setDrawingCacheEnabled(true);
            measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            layout(0, 0, 200,200);

            Bitmap bitmap = Bitmap.createBitmap(getDrawingCache());
            setDrawingCacheEnabled(false);
            return bitmap;
        }

    }

    private class CaptureEditText extends EditText {

        CaptureEditText(Context context) {
            super(context);
        }

        Bitmap capture(char c[]) {
            setText(String.valueOf(c));

            invalidate();

            setDrawingCacheEnabled(true);
            measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            layout(0, 0, 200,200);

            Bitmap bitmap = Bitmap.createBitmap(getDrawingCache());
            setDrawingCacheEnabled(false);
            return bitmap;
        }

    }


    private class CaptureWebView {

        WebViewOnUiThread webViewOnUiThread;
        Bitmap bitmap;
        CaptureWebView(Context context) {
            webViewOnUiThread = new WebViewOnUiThread(EmojiTest.this, getActivity().getWebView());
        }

        Bitmap capture(char c[]) {

            webViewOnUiThread.loadDataAndWaitForCompletion("<html><body>" + String.valueOf(c) + "</body></html>",
                    "text/html; charset=utf-8", "utf-8");
            // The Chromium-powered WebView renders asynchronously and there's nothing reliable
            // we can easily wait for to be sure that capturePicture will return a fresh frame.
            // So, just sleep for a sufficient time.
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                return null;
            }

            Picture picture = webViewOnUiThread.capturePicture();
            if (picture == null || picture.getHeight() <= 0 || picture.getWidth() <= 0) {
                return null;
            } else {
                bitmap = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                picture.draw(canvas);
            }

            return bitmap;
        }

    }

}

