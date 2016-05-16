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

package android.text.cts;


import android.test.AndroidTestCase;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;

public class SelectionTest extends AndroidTestCase {
    public void testGetSelectionStart() {
        CharSequence text = "hello, world";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        assertEquals(-1, Selection.getSelectionStart(builder));

        Selection.setSelection(builder, 3, 8);
        assertEquals(3, Selection.getSelectionStart(builder));

        Selection.setSelection(builder, 3, 9);
        assertEquals(3, Selection.getSelectionStart(builder));

        Selection.setSelection(builder, 5, 7);
        assertEquals(5, Selection.getSelectionStart(builder));

        assertEquals(-1, Selection.getSelectionStart(null));
    }

    public void testGetSelectionEnd() {
        CharSequence text = "hello, world";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        assertEquals(-1, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 0, 10);
        assertEquals(10, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 1, 8);
        assertEquals(8, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 2, 8);
        assertEquals(8, Selection.getSelectionEnd(builder));

        assertEquals(-1, Selection.getSelectionStart(null));
    }

    public void testSetSelection1() {
        CharSequence text = "hello, world";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 3, 6);
        assertEquals(3, Selection.getSelectionStart(builder));
        assertEquals(6, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 3, 7);
        assertEquals(3, Selection.getSelectionStart(builder));
        assertEquals(7, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 3, 7);
        assertEquals(3, Selection.getSelectionStart(builder));
        assertEquals(7, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 6, 2);
        assertEquals(6, Selection.getSelectionStart(builder));
        assertEquals(2, Selection.getSelectionEnd(builder));

        try {
            Selection.setSelection(builder, -1, 100);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public void testSetSelection2() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello, world");
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 1, 4);
        assertEquals(1, Selection.getSelectionStart(builder));
        assertEquals(4, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 3);
        assertEquals(3, Selection.getSelectionStart(builder));
        assertEquals(3, Selection.getSelectionEnd(builder));

        try {
            Selection.setSelection(builder, -1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            Selection.setSelection(builder, 100);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public void testRemoveSelection() {
        CharSequence text = "hello, world";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        Selection.removeSelection(builder);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 1, 6);
        assertEquals(1, Selection.getSelectionStart(builder));
        assertEquals(6, Selection.getSelectionEnd(builder));

        Selection.removeSelection(builder);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));
    }

    public void testSelectAll() {
        CharSequence text = "hello, world";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));;

        Selection.selectAll(builder);
        assertEquals(0, Selection.getSelectionStart(builder));
        assertEquals(text.length(), Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 4, 5);
        Selection.selectAll(builder);
        assertEquals(0, Selection.getSelectionStart(builder));
        assertEquals(text.length(), Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 8, 4);
        Selection.selectAll(builder);
        assertEquals(0, Selection.getSelectionStart(builder));
        assertEquals(text.length(), Selection.getSelectionEnd(builder));

        SpannableStringBuilder empty = new SpannableStringBuilder();
        Selection.selectAll(empty);
        assertEquals(0, Selection.getSelectionStart(empty));
        assertEquals(0, Selection.getSelectionEnd(empty));
    }

    public void testMoveLeft() {
        CharSequence text = "hello\nworld";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        StaticLayout layout = new StaticLayout(text, new TextPaint(), 50, null, 0, 0, false);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 6, 8);
        assertTrue(Selection.moveLeft(builder, layout));
        assertEquals(6, Selection.getSelectionStart(builder));
        assertEquals(6, Selection.getSelectionEnd(builder));

        assertTrue(Selection.moveLeft(builder, layout));
        assertEquals(5, Selection.getSelectionStart(builder));
        assertEquals(5, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 0, 0);
        assertFalse(Selection.moveLeft(builder, layout));
        assertEquals(0, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));

        Selection.selectAll(builder);
        assertTrue(Selection.moveLeft(builder, layout));
        assertEquals(0, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));
    }

    public void testMoveRight() {
        CharSequence text = "hello\nworld";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        StaticLayout layout = new StaticLayout(text, new TextPaint(), 200, null, 0, 0, false);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder,1, 5);
        assertTrue(Selection.moveRight(builder, layout));
        assertEquals(5, Selection.getSelectionStart(builder));
        assertEquals(5, Selection.getSelectionEnd(builder));

        assertTrue(Selection.moveRight(builder, layout));
        assertEquals(6, Selection.getSelectionStart(builder));
        assertEquals(6, Selection.getSelectionEnd(builder));

        assertTrue(Selection.moveRight(builder, layout));
        assertEquals(7, Selection.getSelectionStart(builder));
        assertEquals(7, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, text.length(), text.length());
        assertFalse(Selection.moveRight(builder, layout));
        assertEquals(text.length(), Selection.getSelectionStart(builder));
        assertEquals(text.length(), Selection.getSelectionEnd(builder));

        Selection.selectAll(builder);
        assertTrue(Selection.moveRight(builder, layout));
        assertEquals(text.length(), Selection.getSelectionStart(builder));
        assertEquals(text.length(), Selection.getSelectionEnd(builder));
    }

    public void testMoveUp() {
        CharSequence text = "Google\nhello,world";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        StaticLayout layout = new StaticLayout(text, new TextPaint(), 200, null, 0, 0, false);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        Selection.moveUp(builder, layout);

        Selection.setSelection(builder, 7, 10);
        assertTrue(Selection.moveUp(builder, layout));
        assertEquals(7, Selection.getSelectionStart(builder));
        assertEquals(7, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 0, text.length());
        assertFalse(Selection.moveUp(builder, layout));
        assertEquals(0, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 14);
        assertTrue(Selection.moveUp(builder, layout));
        assertEquals(4, Selection.getSelectionStart(builder));
        assertEquals(4, Selection.getSelectionEnd(builder));

        assertFalse(Selection.moveUp(builder, layout));
        assertEquals(4, Selection.getSelectionStart(builder));
        assertEquals(4, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 5);
        assertFalse(Selection.moveUp(builder, layout));
        assertEquals(5, Selection.getSelectionStart(builder));
        assertEquals(5, Selection.getSelectionEnd(builder));
    }

    public void testMoveDown() {
        CharSequence text = "hello,world\nGoogle";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        StaticLayout layout = new StaticLayout(text, new TextPaint(), 200, null, 0, 0, false);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 1, 3);
        assertTrue(Selection.moveDown(builder, layout));
        assertEquals(3, Selection.getSelectionStart(builder));
        assertEquals(3, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 0, text.length());
        assertFalse(Selection.moveDown(builder, layout));
        assertEquals(text.length(), Selection.getSelectionStart(builder));
        assertEquals(text.length(), Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 5);
        assertTrue(Selection.moveDown(builder, layout));
        assertEquals(14, Selection.getSelectionStart(builder));
        assertEquals(14, Selection.getSelectionEnd(builder));

        assertFalse(Selection.moveDown(builder, layout));
        assertEquals(14, Selection.getSelectionStart(builder));
        assertEquals(14, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 10);
        Selection.moveDown(builder, layout);
        assertEquals(18, Selection.getSelectionStart(builder));
        assertEquals(18, Selection.getSelectionEnd(builder));
    }

    public void testExtendSelection() {
        CharSequence text = "hello, world";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 3, 6);
        Selection.extendSelection(builder, 6);
        assertEquals(3, Selection.getSelectionStart(builder));
        assertEquals(6, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 3, 6);
        Selection.extendSelection(builder, 8);
        assertEquals(3, Selection.getSelectionStart(builder));
        assertEquals(8, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 3, 6);
        Selection.extendSelection(builder, 1);
        assertEquals(3, Selection.getSelectionStart(builder));
        assertEquals(1, Selection.getSelectionEnd(builder));

        try {
            Selection.extendSelection(builder, -1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            Selection.extendSelection(builder, 100);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            Selection.extendSelection(null, 3);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        try {
            Selection.extendSelection(new SpannableStringBuilder(), 3);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public void testExtendLeft() {
        CharSequence text = "Google\nhello, world";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        StaticLayout layout = new StaticLayout(text, new TextPaint(), 200, null, 0, 0, false);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 7, 8);
        assertTrue(Selection.extendLeft(builder, layout));
        assertEquals(7, Selection.getSelectionStart(builder));
        assertEquals(7, Selection.getSelectionEnd(builder));

        assertTrue(Selection.extendLeft(builder, layout));
        assertEquals(7, Selection.getSelectionStart(builder));
        assertEquals(6, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 0, 1);
        assertTrue(Selection.extendLeft(builder, layout));
        assertEquals(0, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));

        assertTrue(Selection.extendLeft(builder, layout));
        assertEquals(0, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));
    }

    public void testExtendRight() {
        CharSequence text = "Google\nhello, world";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        StaticLayout layout = new StaticLayout(text, new TextPaint(), 200, null, 0, 0, false);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 1, 6);
        assertTrue(Selection.extendRight(builder, layout));
        assertEquals(1, Selection.getSelectionStart(builder));
        assertEquals(7, Selection.getSelectionEnd(builder));

        assertTrue(Selection.extendRight(builder, layout));
        assertEquals(1, Selection.getSelectionStart(builder));
        assertEquals(8, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 12, text.length());
        assertTrue(Selection.extendRight(builder, layout));
        assertEquals(12, Selection.getSelectionStart(builder));
        assertEquals(text.length(), Selection.getSelectionEnd(builder));
    }

    public void testExtendUp() {
        CharSequence text = "Google\nhello, world";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        StaticLayout layout = new StaticLayout(text, new TextPaint(), 200, null, 0, 0, false);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        assertTrue(Selection.extendUp(builder, layout));
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 8, 15);
        assertTrue(Selection.extendUp(builder, layout));
        assertEquals(8, Selection.getSelectionStart(builder));
        assertEquals(4, Selection.getSelectionEnd(builder));

        assertTrue(Selection.extendUp(builder, layout));
        assertEquals(8, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));

        assertTrue(Selection.extendUp(builder, layout));
        assertEquals(8, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));

        builder = new SpannableStringBuilder();
        assertTrue(Selection.extendUp(builder, layout));
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));
    }

    public void testExtendDown() {
        CharSequence text = "Google\nhello, world";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        StaticLayout layout = new StaticLayout(text, new TextPaint(), 200, null, 0, 0, false);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 1, 3);
        assertTrue(Selection.extendDown(builder, layout));
        assertEquals(1, Selection.getSelectionStart(builder));
        assertEquals(14, Selection.getSelectionEnd(builder));

        assertTrue(Selection.extendDown(builder, layout));
        assertEquals(1, Selection.getSelectionStart(builder));
        assertEquals(text.length(), Selection.getSelectionEnd(builder));

        assertTrue(Selection.extendDown(builder, layout));
        assertEquals(1, Selection.getSelectionStart(builder));
        assertEquals(text.length(), Selection.getSelectionEnd(builder));
    }

    public void testExtendToLeftEdge() {
        CharSequence text = "hello\nworld";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        StaticLayout layout = new StaticLayout(text, new TextPaint(), 50, null, 0, 0, false);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        assertTrue(Selection.extendToLeftEdge(builder, layout));
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 4, 9);
        assertTrue(Selection.extendToLeftEdge(builder, layout));
        assertEquals(4, Selection.getSelectionStart(builder));
        assertEquals(6, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 1, 5);
        assertTrue(Selection.extendToLeftEdge(builder, layout));
        assertEquals(1, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 2, 2);
        assertTrue(Selection.extendToLeftEdge(builder, layout));
        assertEquals(2, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));

        builder = new SpannableStringBuilder();
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        assertTrue(Selection.extendToLeftEdge(builder, layout));
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));
    }

    public void testExtendToRightEdge() {
        CharSequence text = "hello\nworld";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        StaticLayout layout = new StaticLayout(text, new TextPaint(), 50, null, 0, 0, false);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        assertTrue(Selection.extendToRightEdge(builder, layout));
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(5, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 1, 3);
        assertTrue(Selection.extendToRightEdge(builder, layout));
        assertEquals(1, Selection.getSelectionStart(builder));
        assertEquals(5, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 1, 7);
        assertTrue(Selection.extendToRightEdge(builder, layout));
        assertEquals(1, Selection.getSelectionStart(builder));
        assertEquals(text.length(), Selection.getSelectionEnd(builder));

        assertTrue(Selection.extendToRightEdge(builder, layout));
        assertEquals(1, Selection.getSelectionStart(builder));
        assertEquals(text.length(), Selection.getSelectionEnd(builder));
    }

    public void testMoveToLeftEdge() {
        CharSequence text = "hello\nworld";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        StaticLayout layout = new StaticLayout(text, new TextPaint(), 200, null, 0, 0, false);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        assertTrue(Selection.moveToLeftEdge(builder, layout));
        assertEquals(0, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 1, 10);
        assertTrue(Selection.moveToLeftEdge(builder, layout));
        assertEquals(6, Selection.getSelectionStart(builder));
        assertEquals(6, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 1, 3);
        assertTrue(Selection.moveToLeftEdge(builder, layout));
        assertEquals(0, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));

        assertTrue(Selection.moveToLeftEdge(builder, layout));
        assertEquals(0, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));

        builder = new SpannableStringBuilder();
        assertTrue(Selection.moveToLeftEdge(builder, layout));
        assertEquals(0, Selection.getSelectionStart(builder));
        assertEquals(0, Selection.getSelectionEnd(builder));
    }

    public void testMoveToRightEdge() {
        CharSequence text = "hello\nworld";
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        StaticLayout layout = new StaticLayout(text, new TextPaint(), 200, null, 0, 0,false);
        assertEquals(-1, Selection.getSelectionStart(builder));
        assertEquals(-1, Selection.getSelectionEnd(builder));

        assertTrue(Selection.moveToRightEdge(builder, layout));
        assertEquals(5, Selection.getSelectionStart(builder));
        assertEquals(5, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 1, 2);
        assertTrue(Selection.moveToRightEdge(builder, layout));
        assertEquals(5, Selection.getSelectionStart(builder));
        assertEquals(5, Selection.getSelectionEnd(builder));

        Selection.setSelection(builder, 1, 7);
        assertTrue(Selection.moveToRightEdge(builder, layout));
        assertEquals(text.length(), Selection.getSelectionStart(builder));
        assertEquals(text.length(), Selection.getSelectionEnd(builder));

        assertTrue(Selection.moveToRightEdge(builder, layout));
        assertEquals(text.length(), Selection.getSelectionStart(builder));
        assertEquals(text.length(), Selection.getSelectionEnd(builder));
    }
}
