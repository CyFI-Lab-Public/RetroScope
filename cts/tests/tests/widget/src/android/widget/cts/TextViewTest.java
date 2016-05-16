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

package android.widget.cts;

import com.android.cts.stub.R;
import com.android.internal.util.FastMath;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources.NotFoundException;
import android.cts.util.PollingCheck;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.UiThreadTest;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.DateKeyListener;
import android.text.method.DateTimeKeyListener;
import android.text.method.DialerKeyListener;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.QwertyKeyListener;
import android.text.method.SingleLineTransformationMethod;
import android.text.method.TextKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.text.method.TimeKeyListener;
import android.text.method.TransformationMethod;
import android.text.style.URLSpan;
import android.text.style.cts.MockURLSpanTestActivity;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.TextView.OnEditorActionListener;

import java.io.IOException;

/**
 * Test {@link TextView}.
 */
public class TextViewTest extends ActivityInstrumentationTestCase2<TextViewStubActivity> {

    private TextView mTextView;
    private Activity mActivity;
    private Instrumentation mInstrumentation;
    private static final String LONG_TEXT = "This is a really long string which exceeds "
            + "the width of the view. New devices have a much larger screen which "
            + "actually enables long strings to be displayed with no fading. "
            + "I have made this string longer to fix this case. If you are correcting "
            + "this text, I would love to see the kind of devices you guys now use!";
    private static final long TIMEOUT = 5000;
    private CharSequence mTransformedText;

    public TextViewTest() {
        super("com.android.cts.stub", TextViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        new PollingCheck() {
            @Override
                protected boolean check() {
                return mActivity.hasWindowFocus();
            }
        }.run();
        mInstrumentation = getInstrumentation();
    }

    public void testConstructor() {
        new TextView(mActivity);

        new TextView(mActivity, null);

        new TextView(mActivity, null, 0);
    }

    @UiThreadTest
    public void testAccessText() {
        TextView tv = findTextView(R.id.textview_text);

        String expected = mActivity.getResources().getString(R.string.text_view_hello);
        tv.setText(expected);
        assertEquals(expected, tv.getText().toString());

        tv.setText(null);
        assertEquals("", tv.getText().toString());
    }

    public void testGetLineHeight() {
        mTextView = new TextView(mActivity);
        assertTrue(mTextView.getLineHeight() > 0);

        mTextView.setLineSpacing(1.2f, 1.5f);
        assertTrue(mTextView.getLineHeight() > 0);
    }

    public void testGetLayout() {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView = findTextView(R.id.textview_text);
                mTextView.setGravity(Gravity.CENTER);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertNotNull(mTextView.getLayout());

        TestLayoutRunnable runnable = new TestLayoutRunnable(mTextView) {
            public void run() {
                // change the text of TextView.
                mTextView.setText("Hello, Android!");
                saveLayout();
            }
        };
        mActivity.runOnUiThread(runnable);
        mInstrumentation.waitForIdleSync();
        assertNull(runnable.getLayout());
        assertNotNull(mTextView.getLayout());
    }

    public void testAccessKeyListener() {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView = findTextView(R.id.textview_text);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertNull(mTextView.getKeyListener());

        final KeyListener digitsKeyListener = DigitsKeyListener.getInstance();

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setKeyListener(digitsKeyListener);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertSame(digitsKeyListener, mTextView.getKeyListener());

        final QwertyKeyListener qwertyKeyListener
                = QwertyKeyListener.getInstance(false, Capitalize.NONE);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setKeyListener(qwertyKeyListener);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertSame(qwertyKeyListener, mTextView.getKeyListener());
    }

    public void testAccessMovementMethod() {
        final CharSequence LONG_TEXT = "Scrolls the specified widget to the specified "
                + "coordinates, except constrains the X scrolling position to the horizontal "
                + "regions of the text that will be visible after scrolling to "
                + "the specified Y position.";
        final int selectionStart = 10;
        final int selectionEnd = LONG_TEXT.length();
        final MovementMethod movementMethod = ArrowKeyMovementMethod.getInstance();
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView = findTextView(R.id.textview_text);
                mTextView.setMovementMethod(movementMethod);
                mTextView.setText(LONG_TEXT, BufferType.EDITABLE);
                Selection.setSelection((Editable) mTextView.getText(),
                        selectionStart, selectionEnd);
                mTextView.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();

        assertSame(movementMethod, mTextView.getMovementMethod());
        assertEquals(selectionStart, Selection.getSelectionStart(mTextView.getText()));
        assertEquals(selectionEnd, Selection.getSelectionEnd(mTextView.getText()));
        sendKeys(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_ALT_LEFT,
                KeyEvent.KEYCODE_DPAD_UP);
        // the selection has been removed.
        assertEquals(selectionStart, Selection.getSelectionStart(mTextView.getText()));
        assertEquals(selectionStart, Selection.getSelectionEnd(mTextView.getText()));

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setMovementMethod(null);
                Selection.setSelection((Editable) mTextView.getText(),
                        selectionStart, selectionEnd);
                mTextView.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();

        assertNull(mTextView.getMovementMethod());
        assertEquals(selectionStart, Selection.getSelectionStart(mTextView.getText()));
        assertEquals(selectionEnd, Selection.getSelectionEnd(mTextView.getText()));
        sendKeys(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_ALT_LEFT,
                KeyEvent.KEYCODE_DPAD_UP);
        // the selection will not be changed.
        assertEquals(selectionStart, Selection.getSelectionStart(mTextView.getText()));
        assertEquals(selectionEnd, Selection.getSelectionEnd(mTextView.getText()));
    }

    @UiThreadTest
    public void testLength() {
        mTextView = findTextView(R.id.textview_text);

        String content = "This is content";
        mTextView.setText(content);
        assertEquals(content.length(), mTextView.length());

        mTextView.setText("");
        assertEquals(0, mTextView.length());

        mTextView.setText(null);
        assertEquals(0, mTextView.length());
    }

    @UiThreadTest
    public void testAccessGravity() {
        mActivity.setContentView(R.layout.textview_gravity);

        mTextView = findTextView(R.id.gravity_default);
        assertEquals(Gravity.TOP | Gravity.START, mTextView.getGravity());

        mTextView = findTextView(R.id.gravity_bottom);
        assertEquals(Gravity.BOTTOM | Gravity.START, mTextView.getGravity());

        mTextView = findTextView(R.id.gravity_right);
        assertEquals(Gravity.TOP | Gravity.RIGHT, mTextView.getGravity());

        mTextView = findTextView(R.id.gravity_center);
        assertEquals(Gravity.CENTER, mTextView.getGravity());

        mTextView = findTextView(R.id.gravity_fill);
        assertEquals(Gravity.FILL, mTextView.getGravity());

        mTextView = findTextView(R.id.gravity_center_vertical_right);
        assertEquals(Gravity.CENTER_VERTICAL | Gravity.RIGHT, mTextView.getGravity());

        mTextView.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        assertEquals(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, mTextView.getGravity());
        mTextView.setGravity(Gravity.FILL);
        assertEquals(Gravity.FILL, mTextView.getGravity());
        mTextView.setGravity(Gravity.CENTER);
        assertEquals(Gravity.CENTER, mTextView.getGravity());

        mTextView.setGravity(Gravity.NO_GRAVITY);
        assertEquals(Gravity.TOP | Gravity.START, mTextView.getGravity());

        mTextView.setGravity(Gravity.RIGHT);
        assertEquals(Gravity.TOP | Gravity.RIGHT, mTextView.getGravity());

        mTextView.setGravity(Gravity.FILL_VERTICAL);
        assertEquals(Gravity.FILL_VERTICAL | Gravity.START, mTextView.getGravity());

        //test negative input value.
        mTextView.setGravity(-1);
        assertEquals(-1, mTextView.getGravity());
    }

    public void testAccessAutoLinkMask() {
        mTextView = findTextView(R.id.textview_text);
        final CharSequence text1 =
                new SpannableString("URL: http://www.google.com. mailto: account@gmail.com");
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setAutoLinkMask(Linkify.ALL);
                mTextView.setText(text1, BufferType.EDITABLE);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(Linkify.ALL, mTextView.getAutoLinkMask());

        Spannable spanString = (Spannable) mTextView.getText();
        URLSpan[] spans = spanString.getSpans(0, spanString.length(), URLSpan.class);
        assertNotNull(spans);
        assertEquals(2, spans.length);
        assertEquals("http://www.google.com", spans[0].getURL());
        assertEquals("mailto:account@gmail.com", spans[1].getURL());

        final CharSequence text2 =
            new SpannableString("name: Jack. tel: +41 44 800 8999");
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setAutoLinkMask(Linkify.PHONE_NUMBERS);
                mTextView.setText(text2, BufferType.EDITABLE);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(Linkify.PHONE_NUMBERS, mTextView.getAutoLinkMask());

        spanString = (Spannable) mTextView.getText();
        spans = spanString.getSpans(0, spanString.length(), URLSpan.class);
        assertNotNull(spans);
        assertEquals(1, spans.length);
        assertEquals("tel:+41448008999", spans[0].getURL());

        layout(R.layout.textview_autolink);
        // 1 for web, 2 for email, 4 for phone, 7 for all(web|email|phone)
        assertEquals(0, getAutoLinkMask(R.id.autolink_default));
        assertEquals(Linkify.WEB_URLS, getAutoLinkMask(R.id.autolink_web));
        assertEquals(Linkify.EMAIL_ADDRESSES, getAutoLinkMask(R.id.autolink_email));
        assertEquals(Linkify.PHONE_NUMBERS, getAutoLinkMask(R.id.autolink_phone));
        assertEquals(Linkify.ALL, getAutoLinkMask(R.id.autolink_all));
        assertEquals(Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES,
                getAutoLinkMask(R.id.autolink_compound1));
        assertEquals(Linkify.WEB_URLS | Linkify.PHONE_NUMBERS,
                getAutoLinkMask(R.id.autolink_compound2));
        assertEquals(Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS,
                getAutoLinkMask(R.id.autolink_compound3));
        assertEquals(Linkify.PHONE_NUMBERS | Linkify.ALL,
                getAutoLinkMask(R.id.autolink_compound4));
    }

    public void testAccessTextSize() {
        DisplayMetrics metrics = mActivity.getResources().getDisplayMetrics();

        mTextView = new TextView(mActivity);
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f);
        assertEquals(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 20f, metrics),
                mTextView.getTextSize(), 0.01f);

        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f);
        assertEquals(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, metrics),
                mTextView.getTextSize(), 0.01f);

        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        assertEquals(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, metrics),
                mTextView.getTextSize(), 0.01f);

        // setTextSize by default unit "sp"
        mTextView.setTextSize(20f);
        assertEquals(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, metrics),
                mTextView.getTextSize(), 0.01f);

        mTextView.setTextSize(200f);
        assertEquals(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 200f, metrics),
                mTextView.getTextSize(), 0.01f);
    }

    public void testAccessTextColor() {
        mTextView = new TextView(mActivity);

        mTextView.setTextColor(Color.GREEN);
        assertEquals(Color.GREEN, mTextView.getCurrentTextColor());
        assertSame(ColorStateList.valueOf(Color.GREEN), mTextView.getTextColors());

        mTextView.setTextColor(Color.BLACK);
        assertEquals(Color.BLACK, mTextView.getCurrentTextColor());
        assertSame(ColorStateList.valueOf(Color.BLACK), mTextView.getTextColors());

        mTextView.setTextColor(Color.RED);
        assertEquals(Color.RED, mTextView.getCurrentTextColor());
        assertSame(ColorStateList.valueOf(Color.RED), mTextView.getTextColors());

        // using ColorStateList
        // normal
        ColorStateList colors = new ColorStateList(new int[][] {
                new int[] { android.R.attr.state_focused}, new int[0] },
                new int[] { Color.rgb(0, 255, 0), Color.BLACK });
        mTextView.setTextColor(colors);
        assertSame(colors, mTextView.getTextColors());
        assertEquals(Color.BLACK, mTextView.getCurrentTextColor());

        // exceptional
        try {
            mTextView.setTextColor(null);
            fail("Should thrown exception if the colors is null");
        } catch (NullPointerException e){
        }
    }

    public void testGetTextColor() {
        // TODO: How to get a suitable TypedArray to test this method.

        try {
            TextView.getTextColor(mActivity, null, -1);
            fail("There should be a NullPointerException thrown out.");
        } catch (NullPointerException e) {
        }
    }

    public void testSetHighlightColor() {
        mTextView = new TextView(mActivity);

        mTextView.setHighlightColor(0x00ff00ff);
    }

    public void testSetShadowLayer() {
        MockTextView textView = new MockTextView(mActivity);

        // shadow is placed to the left and below the text
        textView.setShadowLayer(1.0f, 0.3f, 0.3f, Color.CYAN);
        assertTrue(textView.isPaddingOffsetRequired());
        assertEquals(0, textView.getLeftPaddingOffset());
        assertEquals(0, textView.getTopPaddingOffset());
        assertEquals(1, textView.getRightPaddingOffset());
        assertEquals(1, textView.getBottomPaddingOffset());

        // shadow is placed to the right and above the text
        textView.setShadowLayer(1.0f, -0.8f, -0.8f, Color.CYAN);
        assertTrue(textView.isPaddingOffsetRequired());
        assertEquals(-1, textView.getLeftPaddingOffset());
        assertEquals(-1, textView.getTopPaddingOffset());
        assertEquals(0, textView.getRightPaddingOffset());
        assertEquals(0, textView.getBottomPaddingOffset());

        // no shadow
        textView.setShadowLayer(0.0f, 0.0f, 0.0f, Color.CYAN);
        assertFalse(textView.isPaddingOffsetRequired());
        assertEquals(0, textView.getLeftPaddingOffset());
        assertEquals(0, textView.getTopPaddingOffset());
        assertEquals(0, textView.getRightPaddingOffset());
        assertEquals(0, textView.getBottomPaddingOffset());
    }

    @UiThreadTest
    public void testSetSelectAllOnFocus() {
        mActivity.setContentView(R.layout.textview_selectallonfocus);
        String content = "This is the content";
        String blank = "";
        mTextView = findTextView(R.id.selectAllOnFocus_default);
        mTextView.setText(blank, BufferType.SPANNABLE);
        // change the focus
        findTextView(R.id.selectAllOnFocus_dummy).requestFocus();
        assertFalse(mTextView.isFocused());
        mTextView.requestFocus();
        assertTrue(mTextView.isFocused());

        assertEquals(-1, mTextView.getSelectionStart());
        assertEquals(-1, mTextView.getSelectionEnd());

        mTextView.setText(content, BufferType.SPANNABLE);
        mTextView.setSelectAllOnFocus(true);
        // change the focus
        findTextView(R.id.selectAllOnFocus_dummy).requestFocus();
        assertFalse(mTextView.isFocused());
        mTextView.requestFocus();
        assertTrue(mTextView.isFocused());

        assertEquals(0, mTextView.getSelectionStart());
        assertEquals(content.length(), mTextView.getSelectionEnd());

        Selection.setSelection((Spannable) mTextView.getText(), 0);
        mTextView.setSelectAllOnFocus(false);
        // change the focus
        findTextView(R.id.selectAllOnFocus_dummy).requestFocus();
        assertFalse(mTextView.isFocused());
        mTextView.requestFocus();
        assertTrue(mTextView.isFocused());

        assertEquals(0, mTextView.getSelectionStart());
        assertEquals(0, mTextView.getSelectionEnd());

        mTextView.setText(blank, BufferType.SPANNABLE);
        mTextView.setSelectAllOnFocus(true);
        // change the focus
        findTextView(R.id.selectAllOnFocus_dummy).requestFocus();
        assertFalse(mTextView.isFocused());
        mTextView.requestFocus();
        assertTrue(mTextView.isFocused());

        assertEquals(0, mTextView.getSelectionStart());
        assertEquals(blank.length(), mTextView.getSelectionEnd());

        Selection.setSelection((Spannable) mTextView.getText(), 0);
        mTextView.setSelectAllOnFocus(false);
        // change the focus
        findTextView(R.id.selectAllOnFocus_dummy).requestFocus();
        assertFalse(mTextView.isFocused());
        mTextView.requestFocus();
        assertTrue(mTextView.isFocused());

        assertEquals(0, mTextView.getSelectionStart());
        assertEquals(0, mTextView.getSelectionEnd());
    }

    public void testGetPaint() {
        mTextView = new TextView(mActivity);
        TextPaint tp = mTextView.getPaint();
        assertNotNull(tp);

        assertEquals(mTextView.getPaintFlags(), tp.getFlags());
    }

    @UiThreadTest
    public void testAccessLinksClickable() {
        mActivity.setContentView(R.layout.textview_hint_linksclickable_freezestext);

        mTextView = findTextView(R.id.hint_linksClickable_freezesText_default);
        assertTrue(mTextView.getLinksClickable());

        mTextView = findTextView(R.id.linksClickable_true);
        assertTrue(mTextView.getLinksClickable());

        mTextView = findTextView(R.id.linksClickable_false);
        assertFalse(mTextView.getLinksClickable());

        mTextView.setLinksClickable(false);
        assertFalse(mTextView.getLinksClickable());

        mTextView.setLinksClickable(true);
        assertTrue(mTextView.getLinksClickable());

        assertNull(mTextView.getMovementMethod());

        final CharSequence text = new SpannableString("name: Jack. tel: +41 44 800 8999");

        mTextView.setAutoLinkMask(Linkify.PHONE_NUMBERS);
        mTextView.setText(text, BufferType.EDITABLE);

        // Movement method will be automatically set to LinkMovementMethod
        assertTrue(mTextView.getMovementMethod() instanceof LinkMovementMethod);
    }

    public void testAccessHintTextColor() {
        mTextView = new TextView(mActivity);
        // using int values
        // normal
        mTextView.setHintTextColor(Color.GREEN);
        assertEquals(Color.GREEN, mTextView.getCurrentHintTextColor());
        assertSame(ColorStateList.valueOf(Color.GREEN), mTextView.getHintTextColors());

        mTextView.setHintTextColor(Color.BLUE);
        assertSame(ColorStateList.valueOf(Color.BLUE), mTextView.getHintTextColors());
        assertEquals(Color.BLUE, mTextView.getCurrentHintTextColor());

        mTextView.setHintTextColor(Color.RED);
        assertSame(ColorStateList.valueOf(Color.RED), mTextView.getHintTextColors());
        assertEquals(Color.RED, mTextView.getCurrentHintTextColor());

        // using ColorStateList
        // normal
        ColorStateList colors = new ColorStateList(new int[][] {
                new int[] { android.R.attr.state_focused}, new int[0] },
                new int[] { Color.rgb(0, 255, 0), Color.BLACK });
        mTextView.setHintTextColor(colors);
        assertSame(colors, mTextView.getHintTextColors());
        assertEquals(Color.BLACK, mTextView.getCurrentHintTextColor());

        // exceptional
        mTextView.setHintTextColor(null);
        assertNull(mTextView.getHintTextColors());
        assertEquals(mTextView.getCurrentTextColor(), mTextView.getCurrentHintTextColor());
    }

    public void testAccessLinkTextColor() {
        mTextView = new TextView(mActivity);
        // normal
        mTextView.setLinkTextColor(Color.GRAY);
        assertSame(ColorStateList.valueOf(Color.GRAY), mTextView.getLinkTextColors());
        assertEquals(Color.GRAY, mTextView.getPaint().linkColor);

        mTextView.setLinkTextColor(Color.YELLOW);
        assertSame(ColorStateList.valueOf(Color.YELLOW), mTextView.getLinkTextColors());
        assertEquals(Color.YELLOW, mTextView.getPaint().linkColor);

        mTextView.setLinkTextColor(Color.WHITE);
        assertSame(ColorStateList.valueOf(Color.WHITE), mTextView.getLinkTextColors());
        assertEquals(Color.WHITE, mTextView.getPaint().linkColor);

        ColorStateList colors = new ColorStateList(new int[][] {
                new int[] { android.R.attr.state_expanded}, new int[0] },
                new int[] { Color.rgb(0, 255, 0), Color.BLACK });
        mTextView.setLinkTextColor(colors);
        assertSame(colors, mTextView.getLinkTextColors());
        assertEquals(Color.BLACK, mTextView.getPaint().linkColor);

        mTextView.setLinkTextColor(null);
        assertNull(mTextView.getLinkTextColors());
        assertEquals(Color.BLACK, mTextView.getPaint().linkColor);
    }

    public void testAccessPaintFlags() {
        mTextView = new TextView(mActivity);
        assertEquals(Paint.DEV_KERN_TEXT_FLAG | Paint.EMBEDDED_BITMAP_TEXT_FLAG
                | Paint.ANTI_ALIAS_FLAG, mTextView.getPaintFlags());

        mTextView.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG | Paint.FAKE_BOLD_TEXT_FLAG);
        assertEquals(Paint.UNDERLINE_TEXT_FLAG | Paint.FAKE_BOLD_TEXT_FLAG,
                mTextView.getPaintFlags());

        mTextView.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.LINEAR_TEXT_FLAG);
        assertEquals(Paint.STRIKE_THRU_TEXT_FLAG | Paint.LINEAR_TEXT_FLAG,
                mTextView.getPaintFlags());
    }

    public void testHeightAndWidth() {
        mTextView = findTextView(R.id.textview_text);
        int originalWidth = mTextView.getWidth();
        setWidth(mTextView.getWidth() >> 3);
        int originalHeight = mTextView.getHeight();

        setMaxHeight(originalHeight + 1);
        assertEquals(originalHeight, mTextView.getHeight());

        setMaxHeight(originalHeight - 1);
        assertEquals(originalHeight - 1, mTextView.getHeight());

        setMaxHeight(-1);
        assertEquals(0, mTextView.getHeight());

        setMaxHeight(Integer.MAX_VALUE);
        assertEquals(originalHeight, mTextView.getHeight());

        setMinHeight(originalHeight + 1);
        assertEquals(originalHeight + 1, mTextView.getHeight());

        setMinHeight(originalHeight - 1);
        assertEquals(originalHeight, mTextView.getHeight());

        setMinHeight(-1);
        assertEquals(originalHeight, mTextView.getHeight());

        setMinHeight(0);
        setMaxHeight(Integer.MAX_VALUE);

        setHeight(originalHeight + 1);
        assertEquals(originalHeight + 1, mTextView.getHeight());

        setHeight(originalHeight - 1);
        assertEquals(originalHeight - 1, mTextView.getHeight());

        setHeight(-1);
        assertEquals(0, mTextView.getHeight());

        setHeight(originalHeight);
        assertEquals(originalHeight, mTextView.getHeight());

        assertEquals(originalWidth >> 3, mTextView.getWidth());

        // Min Width
        setMinWidth(originalWidth + 1);
        assertEquals(1, mTextView.getLineCount());
        assertEquals(originalWidth + 1, mTextView.getWidth());

        setMinWidth(originalWidth - 1);
        assertEquals(2, mTextView.getLineCount());
        assertEquals(originalWidth - 1, mTextView.getWidth());

        // Width
        setWidth(originalWidth + 1);
        assertEquals(1, mTextView.getLineCount());
        assertEquals(originalWidth + 1, mTextView.getWidth());

        setWidth(originalWidth - 1);
        assertEquals(2, mTextView.getLineCount());
        assertEquals(originalWidth - 1, mTextView.getWidth());
    }

    public void testSetMinEms() {
        mTextView = findTextView(R.id.textview_text);
        assertEquals(1, mTextView.getLineCount());

        int originalWidth = mTextView.getWidth();
        int originalEms = originalWidth / mTextView.getLineHeight();

        setMinEms(originalEms + 1);
        assertEquals((originalEms + 1) * mTextView.getLineHeight(), mTextView.getWidth());

        setMinEms(originalEms - 1);
        assertEquals(originalWidth, mTextView.getWidth());
    }

    public void testSetMaxEms() {
        mTextView = findTextView(R.id.textview_text);
        assertEquals(1, mTextView.getLineCount());
        int originalWidth = mTextView.getWidth();
        int originalEms = originalWidth / mTextView.getLineHeight();

        setMaxEms(originalEms + 1);
        assertEquals(1, mTextView.getLineCount());
        assertEquals(originalWidth, mTextView.getWidth());

        setMaxEms(originalEms - 1);
        assertTrue(1 < mTextView.getLineCount());
        assertEquals((originalEms - 1) * mTextView.getLineHeight(),
                mTextView.getWidth());
    }

    public void testSetEms() {
        mTextView = findTextView(R.id.textview_text);
        assertEquals("check height", 1, mTextView.getLineCount());
        int originalWidth = mTextView.getWidth();
        int originalEms = originalWidth / mTextView.getLineHeight();

        setEms(originalEms + 1);
        assertEquals(1, mTextView.getLineCount());
        assertEquals((originalEms + 1) * mTextView.getLineHeight(),
                mTextView.getWidth());

        setEms(originalEms - 1);
        assertTrue((1 < mTextView.getLineCount()));
        assertEquals((originalEms - 1) * mTextView.getLineHeight(),
                mTextView.getWidth());
    }

    public void testSetLineSpacing() {
        mTextView = new TextView(mActivity);
        int originalLineHeight = mTextView.getLineHeight();

        // normal
        float add = 1.2f;
        float mult = 1.4f;
        setLineSpacing(add, mult);
        assertEquals(FastMath.round(originalLineHeight * mult + add), mTextView.getLineHeight());
        add = 0.0f;
        mult = 1.4f;
        setLineSpacing(add, mult);
        assertEquals(FastMath.round(originalLineHeight * mult + add), mTextView.getLineHeight());

        // abnormal
        add = -1.2f;
        mult = 1.4f;
        setLineSpacing(add, mult);
        assertEquals(FastMath.round(originalLineHeight * mult + add), mTextView.getLineHeight());
        add = -1.2f;
        mult = -1.4f;
        setLineSpacing(add, mult);
        assertEquals(FastMath.round(originalLineHeight * mult + add), mTextView.getLineHeight());
        add = 1.2f;
        mult = 0.0f;
        setLineSpacing(add, mult);
        assertEquals(FastMath.round(originalLineHeight * mult + add), mTextView.getLineHeight());

        // edge
        add = Float.MIN_VALUE;
        mult = Float.MIN_VALUE;
        setLineSpacing(add, mult);
        float expected = originalLineHeight * mult + add;
        assertEquals(FastMath.round(expected), mTextView.getLineHeight());
        add = Float.MAX_VALUE;
        mult = Float.MAX_VALUE;
        setLineSpacing(add, mult);
        expected = originalLineHeight * mult + add;
        assertEquals(FastMath.round(expected), mTextView.getLineHeight());
    }

    public void testInstanceState() {
        // Do not test. Implementation details.
    }

    public void testAccessFreezesText() throws Throwable {
        layout(R.layout.textview_hint_linksclickable_freezestext);

        mTextView = findTextView(R.id.hint_linksClickable_freezesText_default);
        assertFalse(mTextView.getFreezesText());

        mTextView = findTextView(R.id.freezesText_true);
        assertTrue(mTextView.getFreezesText());

        mTextView = findTextView(R.id.freezesText_false);
        assertFalse(mTextView.getFreezesText());

        mTextView.setFreezesText(false);
        assertFalse(mTextView.getFreezesText());

        final CharSequence text = "Hello, TextView.";
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setText(text);
            }
        });
        mInstrumentation.waitForIdleSync();

        final URLSpan urlSpan = new URLSpan("ctstest://TextView/test");
        // TODO: How to simulate the TextView in frozen icicles.
        Instrumentation instrumentation = getInstrumentation();
        ActivityMonitor am = instrumentation.addMonitor(MockURLSpanTestActivity.class.getName(),
                null, false);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                Uri uri = Uri.parse(urlSpan.getURL());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                mActivity.startActivity(intent);
            }
        });

        Activity newActivity = am.waitForActivityWithTimeout(TIMEOUT);
        assertNotNull(newActivity);
        newActivity.finish();
        instrumentation.removeMonitor(am);
        // the text of TextView is removed.
        mTextView = findTextView(R.id.freezesText_false);

        assertEquals(text.toString(), mTextView.getText().toString());

        mTextView.setFreezesText(true);
        assertTrue(mTextView.getFreezesText());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setText(text);
            }
        });
        mInstrumentation.waitForIdleSync();
        // TODO: How to simulate the TextView in frozen icicles.
        am = instrumentation.addMonitor(MockURLSpanTestActivity.class.getName(),
                null, false);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                Uri uri = Uri.parse(urlSpan.getURL());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                mActivity.startActivity(intent);
            }
        });

        Activity oldActivity = newActivity;
        while (true) {
            newActivity = am.waitForActivityWithTimeout(TIMEOUT);
            assertNotNull(newActivity);
            if (newActivity != oldActivity) {
                break;
            }
        }
        newActivity.finish();
        instrumentation.removeMonitor(am);
        // the text of TextView is still there.
        mTextView = findTextView(R.id.freezesText_false);
        assertEquals(text.toString(), mTextView.getText().toString());
    }

    public void testSetEditableFactory() {
        mTextView = new TextView(mActivity);
        String text = "sample";
        MockEditableFactory factory = new MockEditableFactory();
        mTextView.setEditableFactory(factory);

        factory.reset();
        mTextView.setText(text);
        assertFalse(factory.hasCalledNewEditable());

        factory.reset();
        mTextView.setText(text, BufferType.SPANNABLE);
        assertFalse(factory.hasCalledNewEditable());

        factory.reset();
        mTextView.setText(text, BufferType.NORMAL);
        assertFalse(factory.hasCalledNewEditable());

        factory.reset();
        mTextView.setText(text, BufferType.EDITABLE);
        assertTrue(factory.hasCalledNewEditable());
        assertEquals(text, factory.getSource());

        mTextView.setKeyListener(DigitsKeyListener.getInstance());
        factory.reset();
        mTextView.setText(text, BufferType.EDITABLE);
        assertTrue(factory.hasCalledNewEditable());
        assertEquals(text, factory.getSource());

        try {
            mTextView.setEditableFactory(null);
            fail("The factory can not set to null!");
        } catch (NullPointerException e) {
        }
    }

    public void testSetSpannableFactory() {
        mTextView = new TextView(mActivity);
        String text = "sample";
        MockSpannableFactory factory = new MockSpannableFactory();
        mTextView.setSpannableFactory(factory);

        factory.reset();
        mTextView.setText(text);
        assertFalse(factory.hasCalledNewSpannable());

        factory.reset();
        mTextView.setText(text, BufferType.EDITABLE);
        assertFalse(factory.hasCalledNewSpannable());

        factory.reset();
        mTextView.setText(text, BufferType.NORMAL);
        assertFalse(factory.hasCalledNewSpannable());

        factory.reset();
        mTextView.setText(text, BufferType.SPANNABLE);
        assertTrue(factory.hasCalledNewSpannable());
        assertEquals(text, factory.getSource());

        mTextView.setMovementMethod(LinkMovementMethod.getInstance());
        factory.reset();
        mTextView.setText(text, BufferType.NORMAL);
        assertTrue(factory.hasCalledNewSpannable());
        assertEquals(text, factory.getSource());

        try {
            mTextView.setSpannableFactory(null);
            fail("The factory can not set to null!");
        } catch (NullPointerException e) {
        }
    }

    public void testTextChangedListener() {
        mTextView = new TextView(mActivity);
        MockTextWatcher watcher0 = new MockTextWatcher();
        MockTextWatcher watcher1 = new MockTextWatcher();

        mTextView.addTextChangedListener(watcher0);
        mTextView.addTextChangedListener(watcher1);

        watcher0.reset();
        watcher1.reset();
        mTextView.setText("Changed");
        assertTrue(watcher0.hasCalledBeforeTextChanged());
        assertTrue(watcher0.hasCalledOnTextChanged());
        assertTrue(watcher0.hasCalledAfterTextChanged());
        assertTrue(watcher1.hasCalledBeforeTextChanged());
        assertTrue(watcher1.hasCalledOnTextChanged());
        assertTrue(watcher1.hasCalledAfterTextChanged());

        watcher0.reset();
        watcher1.reset();
        // BeforeTextChanged and OnTextChanged are called though the strings are same
        mTextView.setText("Changed");
        assertTrue(watcher0.hasCalledBeforeTextChanged());
        assertTrue(watcher0.hasCalledOnTextChanged());
        assertTrue(watcher0.hasCalledAfterTextChanged());
        assertTrue(watcher1.hasCalledBeforeTextChanged());
        assertTrue(watcher1.hasCalledOnTextChanged());
        assertTrue(watcher1.hasCalledAfterTextChanged());

        watcher0.reset();
        watcher1.reset();
        // BeforeTextChanged and OnTextChanged are called twice (The text is not
        // Editable, so in Append() it calls setText() first)
        mTextView.append("and appended");
        assertTrue(watcher0.hasCalledBeforeTextChanged());
        assertTrue(watcher0.hasCalledOnTextChanged());
        assertTrue(watcher0.hasCalledAfterTextChanged());
        assertTrue(watcher1.hasCalledBeforeTextChanged());
        assertTrue(watcher1.hasCalledOnTextChanged());
        assertTrue(watcher1.hasCalledAfterTextChanged());

        watcher0.reset();
        watcher1.reset();
        // Methods are not called if the string does not change
        mTextView.append("");
        assertFalse(watcher0.hasCalledBeforeTextChanged());
        assertFalse(watcher0.hasCalledOnTextChanged());
        assertFalse(watcher0.hasCalledAfterTextChanged());
        assertFalse(watcher1.hasCalledBeforeTextChanged());
        assertFalse(watcher1.hasCalledOnTextChanged());
        assertFalse(watcher1.hasCalledAfterTextChanged());

        watcher0.reset();
        watcher1.reset();
        mTextView.removeTextChangedListener(watcher1);
        mTextView.setText(null);
        assertTrue(watcher0.hasCalledBeforeTextChanged());
        assertTrue(watcher0.hasCalledOnTextChanged());
        assertTrue(watcher0.hasCalledAfterTextChanged());
        assertFalse(watcher1.hasCalledBeforeTextChanged());
        assertFalse(watcher1.hasCalledOnTextChanged());
        assertFalse(watcher1.hasCalledAfterTextChanged());
    }

    public void testSetTextKeepState1() {
        mTextView = new TextView(mActivity);

        String longString = "very long content";
        String shortString = "short";

        // selection is at the exact place which is inside the short string
        mTextView.setText(longString, BufferType.SPANNABLE);
        Selection.setSelection((Spannable) mTextView.getText(), 3);
        mTextView.setTextKeepState(shortString);
        assertEquals(shortString, mTextView.getText().toString());
        assertEquals(3, mTextView.getSelectionStart());
        assertEquals(3, mTextView.getSelectionEnd());

        // selection is at the exact place which is outside the short string
        mTextView.setText(longString);
        Selection.setSelection((Spannable) mTextView.getText(), shortString.length() + 1);
        mTextView.setTextKeepState(shortString);
        assertEquals(shortString, mTextView.getText().toString());
        assertEquals(shortString.length(), mTextView.getSelectionStart());
        assertEquals(shortString.length(), mTextView.getSelectionEnd());

        // select the sub string which is inside the short string
        mTextView.setText(longString);
        Selection.setSelection((Spannable) mTextView.getText(), 1, 4);
        mTextView.setTextKeepState(shortString);
        assertEquals(shortString, mTextView.getText().toString());
        assertEquals(1, mTextView.getSelectionStart());
        assertEquals(4, mTextView.getSelectionEnd());

        // select the sub string which ends outside the short string
        mTextView.setText(longString);
        Selection.setSelection((Spannable) mTextView.getText(), 2, shortString.length() + 1);
        mTextView.setTextKeepState(shortString);
        assertEquals(shortString, mTextView.getText().toString());
        assertEquals(2, mTextView.getSelectionStart());
        assertEquals(shortString.length(), mTextView.getSelectionEnd());

        // select the sub string which is outside the short string
        mTextView.setText(longString);
        Selection.setSelection((Spannable) mTextView.getText(),
                shortString.length() + 1, shortString.length() + 3);
        mTextView.setTextKeepState(shortString);
        assertEquals(shortString, mTextView.getText().toString());
        assertEquals(shortString.length(), mTextView.getSelectionStart());
        assertEquals(shortString.length(), mTextView.getSelectionEnd());
    }

    @UiThreadTest
    public void testGetEditableText() {
        TextView tv = findTextView(R.id.textview_text);

        String text = "Hello";
        tv.setText(text, BufferType.EDITABLE);
        assertEquals(text, tv.getText().toString());
        assertTrue(tv.getText() instanceof Editable);
        assertEquals(text, tv.getEditableText().toString());

        tv.setText(text, BufferType.SPANNABLE);
        assertEquals(text, tv.getText().toString());
        assertTrue(tv.getText() instanceof Spannable);
        assertNull(tv.getEditableText());

        tv.setText(null, BufferType.EDITABLE);
        assertEquals("", tv.getText().toString());
        assertTrue(tv.getText() instanceof Editable);
        assertEquals("", tv.getEditableText().toString());

        tv.setText(null, BufferType.SPANNABLE);
        assertEquals("", tv.getText().toString());
        assertTrue(tv.getText() instanceof Spannable);
        assertNull(tv.getEditableText());
    }

    @UiThreadTest
    public void testSetText2() {
        String string = "This is a test for setting text content by char array";
        char[] input = string.toCharArray();
        TextView tv = findTextView(R.id.textview_text);

        tv.setText(input, 0, input.length);
        assertEquals(string, tv.getText().toString());

        tv.setText(input, 0, 5);
        assertEquals(string.substring(0, 5), tv.getText().toString());

        try {
            tv.setText(input, -1, input.length);
            fail("Should throw exception if the start position is negative!");
        } catch (IndexOutOfBoundsException exception) {
        }

        try {
            tv.setText(input, 0, -1);
            fail("Should throw exception if the length is negative!");
        } catch (IndexOutOfBoundsException exception) {
        }

        try {
            tv.setText(input, 1, input.length);
            fail("Should throw exception if the end position is out of index!");
        } catch (IndexOutOfBoundsException exception) {
        }

        tv.setText(input, 1, 0);
        assertEquals("", tv.getText().toString());
    }

    @UiThreadTest
    public void testSetText1() {
        mTextView = findTextView(R.id.textview_text);

        String longString = "very long content";
        String shortString = "short";

        // selection is at the exact place which is inside the short string
        mTextView.setText(longString, BufferType.SPANNABLE);
        Selection.setSelection((Spannable) mTextView.getText(), 3);
        mTextView.setTextKeepState(shortString, BufferType.EDITABLE);
        assertTrue(mTextView.getText() instanceof Editable);
        assertEquals(shortString, mTextView.getText().toString());
        assertEquals(shortString, mTextView.getEditableText().toString());
        assertEquals(3, mTextView.getSelectionStart());
        assertEquals(3, mTextView.getSelectionEnd());

        mTextView.setText(shortString, BufferType.EDITABLE);
        assertTrue(mTextView.getText() instanceof Editable);
        assertEquals(shortString, mTextView.getText().toString());
        assertEquals(shortString, mTextView.getEditableText().toString());
        // there is no selection.
        assertEquals(-1, mTextView.getSelectionStart());
        assertEquals(-1, mTextView.getSelectionEnd());

        // selection is at the exact place which is outside the short string
        mTextView.setText(longString);
        Selection.setSelection((Spannable) mTextView.getText(), longString.length());
        mTextView.setTextKeepState(shortString, BufferType.EDITABLE);
        assertTrue(mTextView.getText() instanceof Editable);
        assertEquals(shortString, mTextView.getText().toString());
        assertEquals(shortString, mTextView.getEditableText().toString());
        assertEquals(shortString.length(), mTextView.getSelectionStart());
        assertEquals(shortString.length(), mTextView.getSelectionEnd());

        mTextView.setText(shortString, BufferType.EDITABLE);
        assertTrue(mTextView.getText() instanceof Editable);
        assertEquals(shortString, mTextView.getText().toString());
        assertEquals(shortString, mTextView.getEditableText().toString());
        // there is no selection.
        assertEquals(-1, mTextView.getSelectionStart());
        assertEquals(-1, mTextView.getSelectionEnd());

        // select the sub string which is inside the short string
        mTextView.setText(longString);
        Selection.setSelection((Spannable) mTextView.getText(), 1, shortString.length() - 1);
        mTextView.setTextKeepState(shortString, BufferType.EDITABLE);
        assertTrue(mTextView.getText() instanceof Editable);
        assertEquals(shortString, mTextView.getText().toString());
        assertEquals(shortString, mTextView.getEditableText().toString());
        assertEquals(1, mTextView.getSelectionStart());
        assertEquals(shortString.length() - 1, mTextView.getSelectionEnd());

        mTextView.setText(shortString, BufferType.EDITABLE);
        assertTrue(mTextView.getText() instanceof Editable);
        assertEquals(shortString, mTextView.getText().toString());
        assertEquals(shortString, mTextView.getEditableText().toString());
        // there is no selection.
        assertEquals(-1, mTextView.getSelectionStart());
        assertEquals(-1, mTextView.getSelectionEnd());

        // select the sub string which ends outside the short string
        mTextView.setText(longString);
        Selection.setSelection((Spannable) mTextView.getText(), 2, longString.length());
        mTextView.setTextKeepState(shortString, BufferType.EDITABLE);
        assertTrue(mTextView.getText() instanceof Editable);
        assertEquals(shortString, mTextView.getText().toString());
        assertEquals(shortString, mTextView.getEditableText().toString());
        assertEquals(2, mTextView.getSelectionStart());
        assertEquals(shortString.length(), mTextView.getSelectionEnd());

        mTextView.setText(shortString, BufferType.EDITABLE);
        assertTrue(mTextView.getText() instanceof Editable);
        assertEquals(shortString, mTextView.getText().toString());
        assertEquals(shortString, mTextView.getEditableText().toString());
        // there is no selection.
        assertEquals(-1, mTextView.getSelectionStart());
        assertEquals(-1, mTextView.getSelectionEnd());

        // select the sub string which is outside the short string
        mTextView.setText(longString);
        Selection.setSelection((Spannable) mTextView.getText(),
                shortString.length() + 1, shortString.length() + 3);
        mTextView.setTextKeepState(shortString, BufferType.EDITABLE);
        assertTrue(mTextView.getText() instanceof Editable);
        assertEquals(shortString, mTextView.getText().toString());
        assertEquals(shortString, mTextView.getEditableText().toString());
        assertEquals(shortString.length(), mTextView.getSelectionStart());
        assertEquals(shortString.length(), mTextView.getSelectionEnd());

        mTextView.setText(shortString, BufferType.EDITABLE);
        assertTrue(mTextView.getText() instanceof Editable);
        assertEquals(shortString, mTextView.getText().toString());
        assertEquals(shortString, mTextView.getEditableText().toString());
        // there is no selection.
        assertEquals(-1, mTextView.getSelectionStart());
        assertEquals(-1, mTextView.getSelectionEnd());
    }

    @UiThreadTest
    public void testSetText3() {
        TextView tv = findTextView(R.id.textview_text);

        int resId = R.string.text_view_hint;
        String result = mActivity.getResources().getString(resId);

        tv.setText(resId);
        assertEquals(result, tv.getText().toString());

        try {
            tv.setText(-1);
            fail("Should throw exception with illegal id");
        } catch (NotFoundException e) {
        }
    }

    @UiThreadTest
    public void testSetText() {
        TextView tv = findTextView(R.id.textview_text);

        int resId = R.string.text_view_hint;
        String result = mActivity.getResources().getString(resId);

        tv.setText(resId, BufferType.EDITABLE);
        assertEquals(result, tv.getText().toString());
        assertTrue(tv.getText() instanceof Editable);

        tv.setText(resId, BufferType.SPANNABLE);
        assertEquals(result, tv.getText().toString());
        assertTrue(tv.getText() instanceof Spannable);

        try {
            tv.setText(-1, BufferType.EDITABLE);
            fail("Should throw exception with illegal id");
        } catch (NotFoundException e) {
        }
    }

    @UiThreadTest
    public void testAccessHint() {
        mActivity.setContentView(R.layout.textview_hint_linksclickable_freezestext);

        mTextView = findTextView(R.id.hint_linksClickable_freezesText_default);
        assertNull(mTextView.getHint());

        mTextView = findTextView(R.id.hint_blank);
        assertEquals("", mTextView.getHint());

        mTextView = findTextView(R.id.hint_string);
        assertEquals(mActivity.getResources().getString(R.string.text_view_simple_hint),
                mTextView.getHint());

        mTextView = findTextView(R.id.hint_resid);
        assertEquals(mActivity.getResources().getString(R.string.text_view_hint),
                mTextView.getHint());

        mTextView.setHint("This is hint");
        assertEquals("This is hint", mTextView.getHint().toString());

        mTextView.setHint(R.string.text_view_hello);
        assertEquals(mActivity.getResources().getString(R.string.text_view_hello),
                mTextView.getHint().toString());

        // Non-exist resid
        try {
            mTextView.setHint(-1);
            fail("Should throw exception if id is illegal");
        } catch (NotFoundException e) {
        }
    }

    public void testAccessError() {
        mTextView = findTextView(R.id.textview_text);
        assertNull(mTextView.getError());

        final String errorText = "Opps! There is an error";

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setError(null);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertNull(mTextView.getError());

        final Drawable icon = getDrawable(R.drawable.failed);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setError(errorText, icon);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(errorText, mTextView.getError().toString());
        // can not check whether the drawable is set correctly

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setError(null, null);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertNull(mTextView.getError());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setKeyListener(DigitsKeyListener.getInstance(""));
                mTextView.setText("", BufferType.EDITABLE);
                mTextView.setError(errorText);
                mTextView.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(errorText, mTextView.getError().toString());

        mInstrumentation.sendStringSync("a");
        // a key event that will not change the TextView's text
        assertEquals("", mTextView.getText().toString());
        // The icon and error message will not be reset to null
        assertNull(mTextView.getError());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setKeyListener(DigitsKeyListener.getInstance());
                mTextView.setText("", BufferType.EDITABLE);
                mTextView.setError(errorText);
                mTextView.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();

        mInstrumentation.sendStringSync("1");
        // a key event cause changes to the TextView's text
        assertEquals("1", mTextView.getText().toString());
        // the error message and icon will be cleared.
        assertNull(mTextView.getError());
    }

    public void testAccessFilters() {
        final InputFilter[] expected = { new InputFilter.AllCaps(),
                new InputFilter.LengthFilter(2) };

        final QwertyKeyListener qwertyKeyListener
                = QwertyKeyListener.getInstance(false, Capitalize.NONE);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView = findTextView(R.id.textview_text);
                mTextView.setKeyListener(qwertyKeyListener);
                mTextView.setText("", BufferType.EDITABLE);
                mTextView.setFilters(expected);
                mTextView.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();

        assertSame(expected, mTextView.getFilters());

        mInstrumentation.sendStringSync("a");
        // the text is capitalized by InputFilter.AllCaps
        assertEquals("A", mTextView.getText().toString());
        mInstrumentation.sendStringSync("b");
        // the text is capitalized by InputFilter.AllCaps
        assertEquals("AB", mTextView.getText().toString());
        mInstrumentation.sendStringSync("c");
        // 'C' could not be accepted, because there is a length filter.
        assertEquals("AB", mTextView.getText().toString());

        try {
            mTextView.setFilters(null);
            fail("Should throw IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetFocusedRect() {
        Rect rc = new Rect();

        // Basic
        mTextView = new TextView(mActivity);
        mTextView.getFocusedRect(rc);
        assertEquals(mTextView.getScrollX(), rc.left);
        assertEquals(mTextView.getScrollX() + mTextView.getWidth(), rc.right);
        assertEquals(mTextView.getScrollY(), rc.top);
        assertEquals(mTextView.getScrollY() + mTextView.getHeight(), rc.bottom);

        // Single line
        mTextView = findTextView(R.id.textview_text);
        mTextView.getFocusedRect(rc);
        assertEquals(mTextView.getScrollX(), rc.left);
        assertEquals(mTextView.getScrollX() + mTextView.getWidth(), rc.right);
        assertEquals(mTextView.getScrollY(), rc.top);
        assertEquals(mTextView.getScrollY() + mTextView.getHeight(), rc.bottom);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setSelected(true);
                SpannableString text = new SpannableString(mTextView.getText());
                Selection.setSelection(text, 3, 13);
                mTextView.setText(text);
            }
        });
        mInstrumentation.waitForIdleSync();
        mTextView.getFocusedRect(rc);
        assertNotNull(mTextView.getLayout());
        /* Cursor coordinates from getPrimaryHorizontal() may have a fractional
         * component, while the result of getFocusedRect is in int coordinates.
         * It's not practical for these to match exactly, so we compare that the
         * integer components match - there can be a fractional pixel
         * discrepancy, which should be okay for all practical applications. */
        assertEquals((int) mTextView.getLayout().getPrimaryHorizontal(3), rc.left);
        assertEquals((int) mTextView.getLayout().getPrimaryHorizontal(13), rc.right);
        assertEquals(mTextView.getLayout().getLineTop(0), rc.top);
        assertEquals(mTextView.getLayout().getLineBottom(0), rc.bottom);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setSelected(true);
                SpannableString text = new SpannableString(mTextView.getText());
                Selection.setSelection(text, 13, 3);
                mTextView.setText(text);
            }
        });
        mInstrumentation.waitForIdleSync();
        mTextView.getFocusedRect(rc);
        assertNotNull(mTextView.getLayout());
        assertEquals((int) mTextView.getLayout().getPrimaryHorizontal(3) - 2, rc.left);
        assertEquals((int) mTextView.getLayout().getPrimaryHorizontal(3) + 2, rc.right);
        assertEquals(mTextView.getLayout().getLineTop(0), rc.top);
        assertEquals(mTextView.getLayout().getLineBottom(0), rc.bottom);

        // Multi lines
        mTextView = findTextView(R.id.textview_text_two_lines);
        mTextView.getFocusedRect(rc);
        assertEquals(mTextView.getScrollX(), rc.left);
        assertEquals(mTextView.getScrollX() + mTextView.getWidth(), rc.right);
        assertEquals(mTextView.getScrollY(), rc.top);
        assertEquals(mTextView.getScrollY() + mTextView.getHeight(), rc.bottom);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setSelected(true);
                SpannableString text = new SpannableString(mTextView.getText());
                Selection.setSelection(text, 2, 4);
                mTextView.setText(text);
            }
        });
        mInstrumentation.waitForIdleSync();
        mTextView.getFocusedRect(rc);
        assertNotNull(mTextView.getLayout());
        assertEquals((int) mTextView.getLayout().getPrimaryHorizontal(2), rc.left);
        assertEquals((int) mTextView.getLayout().getPrimaryHorizontal(4), rc.right);
        assertEquals(mTextView.getLayout().getLineTop(0), rc.top);
        assertEquals(mTextView.getLayout().getLineBottom(0), rc.bottom);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setSelected(true);
                SpannableString text = new SpannableString(mTextView.getText());
                Selection.setSelection(text, 2, 10); // cross the "\n" and two lines
                mTextView.setText(text);
            }
        });
        mInstrumentation.waitForIdleSync();
        mTextView.getFocusedRect(rc);
        Path path = new Path();
        mTextView.getLayout().getSelectionPath(2, 10, path);
        RectF rcf = new RectF();
        path.computeBounds(rcf, true);
        assertNotNull(mTextView.getLayout());
        assertEquals(rcf.left - 1, (float) rc.left);
        assertEquals(rcf.right + 1, (float) rc.right);
        assertEquals(mTextView.getLayout().getLineTop(0), rc.top);
        assertEquals(mTextView.getLayout().getLineBottom(1), rc.bottom);

        // Exception
        try {
            mTextView.getFocusedRect(null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    public void testGetLineCount() {
        mTextView = findTextView(R.id.textview_text);
        // this is an one line text with default setting.
        assertEquals(1, mTextView.getLineCount());

        // make it multi-lines
        setMaxWidth(mTextView.getWidth() / 3);
        assertTrue(1 < mTextView.getLineCount());

        // make it to an one line
        setMaxWidth(Integer.MAX_VALUE);
        assertEquals(1, mTextView.getLineCount());

        // set min lines don't effect the lines count for actual text.
        setMinLines(12);
        assertEquals(1, mTextView.getLineCount());

        mTextView = new TextView(mActivity);
        // the internal Layout has not been built.
        assertNull(mTextView.getLayout());
        assertEquals(0, mTextView.getLineCount());
    }

    public void testGetLineBounds() {
        Rect rc = new Rect();
        mTextView = new TextView(mActivity);
        assertEquals(0, mTextView.getLineBounds(0, null));

        assertEquals(0, mTextView.getLineBounds(0, rc));
        assertEquals(0, rc.left);
        assertEquals(0, rc.right);
        assertEquals(0, rc.top);
        assertEquals(0, rc.bottom);

        mTextView = findTextView(R.id.textview_text);
        assertEquals(mTextView.getBaseline(), mTextView.getLineBounds(0, null));

        assertEquals(mTextView.getBaseline(), mTextView.getLineBounds(0, rc));
        assertEquals(0, rc.left);
        assertEquals(mTextView.getWidth(), rc.right);
        assertEquals(0, rc.top);
        assertEquals(mTextView.getHeight(), rc.bottom);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setPadding(1, 2, 3, 4);
                mTextView.setGravity(Gravity.BOTTOM);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(mTextView.getBaseline(), mTextView.getLineBounds(0, rc));
        assertEquals(mTextView.getTotalPaddingLeft(), rc.left);
        assertEquals(mTextView.getWidth() - mTextView.getTotalPaddingRight(), rc.right);
        assertEquals(mTextView.getTotalPaddingTop(), rc.top);
        assertEquals(mTextView.getHeight() - mTextView.getTotalPaddingBottom(), rc.bottom);
    }

    public void testGetBaseLine() {
        mTextView = new TextView(mActivity);
        assertEquals(-1, mTextView.getBaseline());

        mTextView = findTextView(R.id.textview_text);
        assertEquals(mTextView.getLayout().getLineBaseline(0), mTextView.getBaseline());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setPadding(1, 2, 3, 4);
                mTextView.setGravity(Gravity.BOTTOM);
            }
        });
        mInstrumentation.waitForIdleSync();
        int expected = mTextView.getTotalPaddingTop() + mTextView.getLayout().getLineBaseline(0);
        assertEquals(expected, mTextView.getBaseline());
    }

    public void testPressKey() {
        final QwertyKeyListener qwertyKeyListener
                = QwertyKeyListener.getInstance(false, Capitalize.NONE);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView = findTextView(R.id.textview_text);
                mTextView.setKeyListener(qwertyKeyListener);
                mTextView.setText("", BufferType.EDITABLE);
                mTextView.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();

        mInstrumentation.sendStringSync("a");
        assertEquals("a", mTextView.getText().toString());
        mInstrumentation.sendStringSync("b");
        assertEquals("ab", mTextView.getText().toString());
        sendKeys(KeyEvent.KEYCODE_DEL);
        assertEquals("a", mTextView.getText().toString());
    }

    public void testSetIncludeFontPadding() {
        mTextView = findTextView(R.id.textview_text);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setWidth(mTextView.getWidth() / 3);
                mTextView.setPadding(1, 2, 3, 4);
                mTextView.setGravity(Gravity.BOTTOM);
            }
        });
        mInstrumentation.waitForIdleSync();

        int oldHeight = mTextView.getHeight();
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setIncludeFontPadding(false);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(mTextView.getHeight() < oldHeight);
    }

    public void testScroll() {
        mTextView = new TextView(mActivity);

        assertEquals(0, mTextView.getScrollX());
        assertEquals(0, mTextView.getScrollY());

        //don't set the Scroller, nothing changed.
        mTextView.computeScroll();
        assertEquals(0, mTextView.getScrollX());
        assertEquals(0, mTextView.getScrollY());

        //set the Scroller
        Scroller s = new Scroller(mActivity);
        assertNotNull(s);
        s.startScroll(0, 0, 320, 480, 0);
        s.abortAnimation();
        s.forceFinished(false);
        mTextView.setScroller(s);

        mTextView.computeScroll();
        assertEquals(320, mTextView.getScrollX());
        assertEquals(480, mTextView.getScrollY());
    }

    public void testDebug() {
        mTextView = new TextView(mActivity);
        mTextView.debug(0);

        mTextView.setText("Hello!");
        layout(mTextView);
        mTextView.debug(1);
    }

    public void testSelection() {
        mTextView = new TextView(mActivity);
        String text = "This is the content";
        mTextView.setText(text, BufferType.SPANNABLE);
        assertFalse(mTextView.hasSelection());

        Selection.selectAll((Spannable) mTextView.getText());
        assertEquals(0, mTextView.getSelectionStart());
        assertEquals(text.length(), mTextView.getSelectionEnd());
        assertTrue(mTextView.hasSelection());

        int selectionStart = 5;
        int selectionEnd = 7;
        Selection.setSelection((Spannable) mTextView.getText(), selectionStart);
        assertEquals(selectionStart, mTextView.getSelectionStart());
        assertEquals(selectionStart, mTextView.getSelectionEnd());
        assertFalse(mTextView.hasSelection());

        Selection.setSelection((Spannable) mTextView.getText(), selectionStart, selectionEnd);
        assertEquals(selectionStart, mTextView.getSelectionStart());
        assertEquals(selectionEnd, mTextView.getSelectionEnd());
        assertTrue(mTextView.hasSelection());
    }

    @UiThreadTest
    public void testAccessEllipsize() {
        mActivity.setContentView(R.layout.textview_ellipsize);

        mTextView = findTextView(R.id.ellipsize_default);
        assertNull(mTextView.getEllipsize());

        mTextView = findTextView(R.id.ellipsize_none);
        assertNull(mTextView.getEllipsize());

        mTextView = findTextView(R.id.ellipsize_start);
        assertSame(TruncateAt.START, mTextView.getEllipsize());

        mTextView = findTextView(R.id.ellipsize_middle);
        assertSame(TruncateAt.MIDDLE, mTextView.getEllipsize());

        mTextView = findTextView(R.id.ellipsize_end);
        assertSame(TruncateAt.END, mTextView.getEllipsize());

        mTextView.setEllipsize(TextUtils.TruncateAt.START);
        assertSame(TextUtils.TruncateAt.START, mTextView.getEllipsize());

        mTextView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        assertSame(TextUtils.TruncateAt.MIDDLE, mTextView.getEllipsize());

        mTextView.setEllipsize(TextUtils.TruncateAt.END);
        assertSame(TextUtils.TruncateAt.END, mTextView.getEllipsize());

        mTextView.setEllipsize(null);
        assertNull(mTextView.getEllipsize());

        mTextView.setWidth(10);
        mTextView.setEllipsize(TextUtils.TruncateAt.START);
        mTextView.setText("ThisIsAVeryLongVeryLongVeryLongVeryLongVeryLongWord");
        mTextView.invalidate();

        assertSame(TextUtils.TruncateAt.START, mTextView.getEllipsize());
        // there is no method to check if '...yLongVeryLongWord' is painted in the screen.
    }

    public void testSetCursorVisible() {
        mTextView = new TextView(mActivity);

        mTextView.setCursorVisible(true);
        mTextView.setCursorVisible(false);
    }

    public void testOnWindowFocusChanged() {
        // Do not test. Implementation details.
    }

    public void testOnTouchEvent() {
        // Do not test. Implementation details.
    }

    public void testOnTrackballEvent() {
        // Do not test. Implementation details.
    }

    public void testGetTextColors() {
        // TODO: How to get a suitable TypedArray to test this method.
    }

    public void testOnKeyShortcut() {
        // Do not test. Implementation details.
    }

    @UiThreadTest
    public void testPerformLongClick() {
        mTextView = findTextView(R.id.textview_text);
        mTextView.setText("This is content");
        MockOnLongClickListener onLongClickListener = new MockOnLongClickListener(true);
        MockOnCreateContextMenuListener onCreateContextMenuListener
                = new MockOnCreateContextMenuListener(false);
        mTextView.setOnLongClickListener(onLongClickListener);
        mTextView.setOnCreateContextMenuListener(onCreateContextMenuListener);
        assertTrue(mTextView.performLongClick());
        assertTrue(onLongClickListener.hasLongClicked());
        assertFalse(onCreateContextMenuListener.hasCreatedContextMenu());

        onLongClickListener = new MockOnLongClickListener(false);
        mTextView.setOnLongClickListener(onLongClickListener);
        mTextView.setOnCreateContextMenuListener(onCreateContextMenuListener);
        assertTrue(mTextView.performLongClick());
        assertTrue(onLongClickListener.hasLongClicked());
        assertTrue(onCreateContextMenuListener.hasCreatedContextMenu());

        mTextView.setOnLongClickListener(null);
        onCreateContextMenuListener = new MockOnCreateContextMenuListener(true);
        mTextView.setOnCreateContextMenuListener(onCreateContextMenuListener);
        assertFalse(mTextView.performLongClick());
        assertTrue(onCreateContextMenuListener.hasCreatedContextMenu());
    }

    @UiThreadTest
    public void testTextAttr() {
        mTextView = findTextView(R.id.textview_textAttr);
        // getText
        assertEquals(mActivity.getString(R.string.text_view_hello), mTextView.getText().toString());

        // getCurrentTextColor
        assertEquals(mActivity.getResources().getColor(R.drawable.black),
                mTextView.getCurrentTextColor());
        assertEquals(mActivity.getResources().getColor(R.drawable.red),
                mTextView.getCurrentHintTextColor());
        assertEquals(mActivity.getResources().getColor(R.drawable.red),
                mTextView.getHintTextColors().getDefaultColor());
        assertEquals(mActivity.getResources().getColor(R.drawable.blue),
                mTextView.getLinkTextColors().getDefaultColor());

        // getTextScaleX
        assertEquals(1.2f, mTextView.getTextScaleX(), 0.01f);

        // setTextScaleX
        mTextView.setTextScaleX(2.4f);
        assertEquals(2.4f, mTextView.getTextScaleX(), 0.01f);

        mTextView.setTextScaleX(0f);
        assertEquals(0f, mTextView.getTextScaleX(), 0.01f);

        mTextView.setTextScaleX(- 2.4f);
        assertEquals(- 2.4f, mTextView.getTextScaleX(), 0.01f);

        // getTextSize
        assertEquals(20f, mTextView.getTextSize(), 0.01f);

        // getTypeface
        // getTypeface will be null if android:typeface is set to normal,
        // and android:style is not set or is set to normal
        assertNull(mTextView.getTypeface());

        mTextView.setTypeface(Typeface.DEFAULT);
        assertSame(Typeface.DEFAULT, mTextView.getTypeface());
        // null type face
        mTextView.setTypeface(null);
        assertNull(mTextView.getTypeface());

        // default type face, bold style, note: the type face will be changed
        // after call set method
        mTextView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        assertSame(Typeface.BOLD, mTextView.getTypeface().getStyle());

        // null type face, BOLD style
        mTextView.setTypeface(null, Typeface.BOLD);
        assertSame(Typeface.BOLD, mTextView.getTypeface().getStyle());

        // old type face, null style
        mTextView.setTypeface(Typeface.DEFAULT, 0);
        assertEquals(Typeface.NORMAL, mTextView.getTypeface().getStyle());
    }

    @UiThreadTest
    public void testAppend() {
        mTextView = new TextView(mActivity);

        // 1: check the original length, should be blank as initialised.
        assertEquals(0, mTextView.getText().length());

        // 2: append a string use append(CharSquence) into the original blank
        // buffer, check the content. And upgrading it to BufferType.EDITABLE if it was
        // not already editable.
        assertFalse(mTextView.getText() instanceof Editable);
        mTextView.append("Append.");
        assertEquals("Append.", mTextView.getText().toString());
        assertTrue(mTextView.getText() instanceof Editable);

        // 3: append a string from 0~3.
        mTextView.append("Append", 0, 3);
        assertEquals("Append.App", mTextView.getText().toString());
        assertTrue(mTextView.getText() instanceof Editable);

        // 4: append a string from 0~0, nothing will be append as expected.
        mTextView.append("Append", 0, 0);
        assertEquals("Append.App", mTextView.getText().toString());
        assertTrue(mTextView.getText() instanceof Editable);

        // 5: append a string from -3~3. check the wrong left edge.
        try {
            mTextView.append("Append", -3, 3);
            fail("Should throw StringIndexOutOfBoundsException");
        } catch (StringIndexOutOfBoundsException e) {
        }

        // 6: append a string from 3~10. check the wrong right edge.
        try {
            mTextView.append("Append", 3, 10);
            fail("Should throw StringIndexOutOfBoundsException");
        } catch (StringIndexOutOfBoundsException e) {
        }

        // 7: append a null string.
        try {
            mTextView.append(null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testAccessTransformationMethod() {
        // check the password attribute in xml
        mTextView = findTextView(R.id.textview_password);
        assertNotNull(mTextView);
        assertSame(PasswordTransformationMethod.getInstance(),
                mTextView.getTransformationMethod());

        // check the singleLine attribute in xml
        mTextView = findTextView(R.id.textview_singleLine);
        assertNotNull(mTextView);
        assertSame(SingleLineTransformationMethod.getInstance(),
                mTextView.getTransformationMethod());

        final QwertyKeyListener qwertyKeyListener = QwertyKeyListener.getInstance(false,
                Capitalize.NONE);
        final TransformationMethod method = PasswordTransformationMethod.getInstance();
        // change transformation method by function
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setKeyListener(qwertyKeyListener);
                mTextView.setTransformationMethod(method);
                mTransformedText = method.getTransformation(mTextView.getText(), mTextView);

                mTextView.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertSame(PasswordTransformationMethod.getInstance(),
                mTextView.getTransformationMethod());

        sendKeys("H E 2*L O");
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.append(" ");
            }
        });
        mInstrumentation.waitForIdleSync();

        // it will get transformed after a while
        new PollingCheck(TIMEOUT) {
            @Override
            protected boolean check() {
                // "******"
                return mTransformedText.toString()
                        .equals("\u2022\u2022\u2022\u2022\u2022\u2022");
            }
        }.run();

        // set null
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setTransformationMethod(null);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertNull(mTextView.getTransformationMethod());
    }

    @UiThreadTest
    public void testCompound() {
        mTextView = new TextView(mActivity);
        int padding = 3;
        Drawable[] drawables = mTextView.getCompoundDrawables();
        assertNull(drawables[0]);
        assertNull(drawables[1]);
        assertNull(drawables[2]);
        assertNull(drawables[3]);

        // test setCompoundDrawablePadding and getCompoundDrawablePadding
        mTextView.setCompoundDrawablePadding(padding);
        assertEquals(padding, mTextView.getCompoundDrawablePadding());

        // using resid, 0 represents null
        mTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.start, R.drawable.pass,
                R.drawable.failed, 0);
        drawables = mTextView.getCompoundDrawables();

        // drawableLeft
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.start),
                ((BitmapDrawable) drawables[0]).getBitmap());
        // drawableTop
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.pass),
                ((BitmapDrawable) drawables[1]).getBitmap());
        // drawableRight
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.failed),
                ((BitmapDrawable) drawables[2]).getBitmap());
        // drawableBottom
        assertNull(drawables[3]);

        Drawable left = getDrawable(R.drawable.blue);
        Drawable right = getDrawable(R.drawable.yellow);
        Drawable top = getDrawable(R.drawable.red);

        // using drawables directly
        mTextView.setCompoundDrawablesWithIntrinsicBounds(left, top, right, null);
        drawables = mTextView.getCompoundDrawables();

        // drawableLeft
        assertSame(left, drawables[0]);
        // drawableTop
        assertSame(top, drawables[1]);
        // drawableRight
        assertSame(right, drawables[2]);
        // drawableBottom
        assertNull(drawables[3]);

        // check compound padding
        assertEquals(mTextView.getPaddingLeft() + padding + left.getIntrinsicWidth(),
                mTextView.getCompoundPaddingLeft());
        assertEquals(mTextView.getPaddingTop() + padding + top.getIntrinsicHeight(),
                mTextView.getCompoundPaddingTop());
        assertEquals(mTextView.getPaddingRight() + padding + right.getIntrinsicWidth(),
                mTextView.getCompoundPaddingRight());
        assertEquals(mTextView.getPaddingBottom(), mTextView.getCompoundPaddingBottom());

        // set bounds to drawables and set them again.
        left.setBounds(0, 0, 1, 2);
        right.setBounds(0, 0, 3, 4);
        top.setBounds(0, 0, 5, 6);
        // usinf drawables
        mTextView.setCompoundDrawables(left, top, right, null);
        drawables = mTextView.getCompoundDrawables();

        // drawableLeft
        assertSame(left, drawables[0]);
        // drawableTop
        assertSame(top, drawables[1]);
        // drawableRight
        assertSame(right, drawables[2]);
        // drawableBottom
        assertNull(drawables[3]);

        // check compound padding
        assertEquals(mTextView.getPaddingLeft() + padding + left.getBounds().width(),
                mTextView.getCompoundPaddingLeft());
        assertEquals(mTextView.getPaddingTop() + padding + top.getBounds().height(),
                mTextView.getCompoundPaddingTop());
        assertEquals(mTextView.getPaddingRight() + padding + right.getBounds().width(),
                mTextView.getCompoundPaddingRight());
        assertEquals(mTextView.getPaddingBottom(), mTextView.getCompoundPaddingBottom());
    }

    public void testSingleLine() {
        final TextView textView = new TextView(mActivity);
        setSpannableText(textView, "This is a really long sentence"
                + " which can not be placed in one line on the screen.");

        // Narrow layout assures that the text will get wrapped.
        FrameLayout innerLayout = new FrameLayout(mActivity);
        innerLayout.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        innerLayout.addView(textView);

        final FrameLayout layout = new FrameLayout(mActivity);
        layout.addView(innerLayout);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mActivity.setContentView(layout);
                textView.setSingleLine(true);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(SingleLineTransformationMethod.getInstance(),
                textView.getTransformationMethod());
        int singleLineWidth = textView.getLayout().getWidth();
        int singleLineHeight = textView.getLayout().getHeight();

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                textView.setSingleLine(false);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(null, textView.getTransformationMethod());
        assertTrue(textView.getLayout().getHeight() > singleLineHeight);
        assertTrue(textView.getLayout().getWidth() < singleLineWidth);

        // same behaviours as setSingLine(true)
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                textView.setSingleLine();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(SingleLineTransformationMethod.getInstance(),
                textView.getTransformationMethod());
        assertEquals(singleLineHeight, textView.getLayout().getHeight());
        assertEquals(singleLineWidth, textView.getLayout().getWidth());
    }

    @UiThreadTest
    public void testSetMaxLines() {
        mTextView = findTextView(R.id.textview_text);

        float[] widths = new float[LONG_TEXT.length()];
        mTextView.getPaint().getTextWidths(LONG_TEXT, widths);
        float totalWidth = 0.0f;
        for (float f : widths) {
            totalWidth += f;
        }
        final int stringWidth = (int) totalWidth;
        mTextView.setWidth(stringWidth >> 2);
        mTextView.setText(LONG_TEXT);

        final int maxLines = 2;
        assertTrue(mTextView.getLineCount() > maxLines);

        mTextView.setMaxLines(maxLines);
        mTextView.requestLayout();

        assertTrue(mTextView.getHeight() <= maxLines * mTextView.getLineHeight());
    }

    @UiThreadTest
    public void testSetMaxLinesException() {
        mTextView = new TextView(mActivity);
        mActivity.setContentView(mTextView);
        mTextView.setWidth(mTextView.getWidth() >> 3);
        mTextView.setMaxLines(-1);
    }

    public void testSetMinLines() {
        mTextView = findTextView(R.id.textview_text);
        setWidth(mTextView.getWidth() >> 3);
        int originalHeight = mTextView.getHeight();
        int originalLines = mTextView.getLineCount();

        setMinLines(originalLines - 1);
        assertTrue((originalLines - 1) * mTextView.getLineHeight() <= mTextView.getHeight());

        setMinLines(originalLines + 1);
        assertTrue((originalLines + 1) * mTextView.getLineHeight() <= mTextView.getHeight());
    }

    public void testSetLines() {
        mTextView = findTextView(R.id.textview_text);
        // make it multiple lines
        setWidth(mTextView.getWidth() >> 3);
        int originalLines = mTextView.getLineCount();

        setLines(originalLines - 1);
        assertTrue((originalLines - 1) * mTextView.getLineHeight() <= mTextView.getHeight());

        setLines(originalLines + 1);
        assertTrue((originalLines + 1) * mTextView.getLineHeight() <= mTextView.getHeight());
    }

    @UiThreadTest
    public void testSetLinesException() {
        mTextView = new TextView(mActivity);
        mActivity.setContentView(mTextView);
        mTextView.setWidth(mTextView.getWidth() >> 3);
        mTextView.setLines(-1);
    }

    @UiThreadTest
    public void testGetExtendedPaddingTop() {
        mTextView = findTextView(R.id.textview_text);
        // Initialized value
        assertEquals(0, mTextView.getExtendedPaddingTop());

        // After Set a Drawable
        final Drawable top = getDrawable(R.drawable.red);
        top.setBounds(0, 0, 100, 10);
        mTextView.setCompoundDrawables(null, top, null, null);
        assertEquals(mTextView.getCompoundPaddingTop(), mTextView.getExtendedPaddingTop());

        // Change line count
        mTextView.setLines(mTextView.getLineCount() - 1);
        mTextView.setGravity(Gravity.BOTTOM);

        assertTrue(mTextView.getExtendedPaddingTop() > 0);
    }

    @UiThreadTest
    public void testGetExtendedPaddingBottom() {
        mTextView = findTextView(R.id.textview_text);
        // Initialized value
        assertEquals(0, mTextView.getExtendedPaddingBottom());

        // After Set a Drawable
        final Drawable bottom = getDrawable(R.drawable.red);
        bottom.setBounds(0, 0, 100, 10);
        mTextView.setCompoundDrawables(null, null, null, bottom);
        assertEquals(mTextView.getCompoundPaddingBottom(), mTextView.getExtendedPaddingBottom());

        // Change line count
        mTextView.setLines(mTextView.getLineCount() - 1);
        mTextView.setGravity(Gravity.CENTER_VERTICAL);

        assertTrue(mTextView.getExtendedPaddingBottom() > 0);
    }

    public void testGetTotalPaddingTop() {
        mTextView = findTextView(R.id.textview_text);
        // Initialized value
        assertEquals(0, mTextView.getTotalPaddingTop());

        // After Set a Drawable
        final Drawable top = getDrawable(R.drawable.red);
        top.setBounds(0, 0, 100, 10);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setCompoundDrawables(null, top, null, null);
                mTextView.setLines(mTextView.getLineCount() - 1);
                mTextView.setGravity(Gravity.BOTTOM);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(mTextView.getExtendedPaddingTop(), mTextView.getTotalPaddingTop());

        // Change line count
        setLines(mTextView.getLineCount() + 1);
        int expected = mTextView.getHeight()
                - mTextView.getExtendedPaddingBottom()
                - mTextView.getLayout().getLineTop(mTextView.getLineCount());
        assertEquals(expected, mTextView.getTotalPaddingTop());
    }

    public void testGetTotalPaddingBottom() {
        mTextView = findTextView(R.id.textview_text);
        // Initialized value
        assertEquals(0, mTextView.getTotalPaddingBottom());

        // After Set a Drawable
        final Drawable bottom = getDrawable(R.drawable.red);
        bottom.setBounds(0, 0, 100, 10);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setCompoundDrawables(null, null, null, bottom);
                mTextView.setLines(mTextView.getLineCount() - 1);
                mTextView.setGravity(Gravity.CENTER_VERTICAL);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(mTextView.getExtendedPaddingBottom(), mTextView.getTotalPaddingBottom());

        // Change line count
        setLines(mTextView.getLineCount() + 1);
        int expected = ((mTextView.getHeight()
                - mTextView.getExtendedPaddingBottom()
                - mTextView.getExtendedPaddingTop()
                - mTextView.getLayout().getLineBottom(mTextView.getLineCount())) >> 1)
                + mTextView.getExtendedPaddingBottom();
        assertEquals(expected, mTextView.getTotalPaddingBottom());
    }

    @UiThreadTest
    public void testGetTotalPaddingLeft() {
        mTextView = findTextView(R.id.textview_text);
        // Initialized value
        assertEquals(0, mTextView.getTotalPaddingLeft());

        // After Set a Drawable
        Drawable left = getDrawable(R.drawable.red);
        left.setBounds(0, 0, 10, 100);
        mTextView.setCompoundDrawables(left, null, null, null);
        mTextView.setGravity(Gravity.RIGHT);
        assertEquals(mTextView.getCompoundPaddingLeft(), mTextView.getTotalPaddingLeft());

        // Change width
        mTextView.setWidth(Integer.MAX_VALUE);
        assertEquals(mTextView.getCompoundPaddingLeft(), mTextView.getTotalPaddingLeft());
    }

    @UiThreadTest
    public void testGetTotalPaddingRight() {
        mTextView = findTextView(R.id.textview_text);
        // Initialized value
        assertEquals(0, mTextView.getTotalPaddingRight());

        // After Set a Drawable
        Drawable right = getDrawable(R.drawable.red);
        right.setBounds(0, 0, 10, 100);
        mTextView.setCompoundDrawables(null, null, right, null);
        mTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        assertEquals(mTextView.getCompoundPaddingRight(), mTextView.getTotalPaddingRight());

        // Change width
        mTextView.setWidth(Integer.MAX_VALUE);
        assertEquals(mTextView.getCompoundPaddingRight(), mTextView.getTotalPaddingRight());
    }

    public void testGetUrls() {
        mTextView = new TextView(mActivity);

        URLSpan[] spans = mTextView.getUrls();
        assertEquals(0, spans.length);

        String url = "http://www.google.com";
        String email = "name@gmail.com";
        String string = url + " mailto:" + email;
        SpannableString spannable = new SpannableString(string);
        spannable.setSpan(new URLSpan(url), 0, url.length(), 0);
        mTextView.setText(spannable, BufferType.SPANNABLE);
        spans = mTextView.getUrls();
        assertEquals(1, spans.length);
        assertEquals(url, spans[0].getURL());

        spannable.setSpan(new URLSpan(email), 0, email.length(), 0);
        mTextView.setText(spannable, BufferType.SPANNABLE);

        spans = mTextView.getUrls();
        assertEquals(2, spans.length);
        assertEquals(url, spans[0].getURL());
        assertEquals(email, spans[1].getURL());

        // test the situation that param what is not a URLSpan
        spannable.setSpan(new Object(), 0, 9, 0);
        mTextView.setText(spannable, BufferType.SPANNABLE);
        spans = mTextView.getUrls();
        assertEquals(2, spans.length);
    }

    public void testSetPadding() {
        mTextView = new TextView(mActivity);

        mTextView.setPadding(0, 1, 2, 4);
        assertEquals(0, mTextView.getPaddingLeft());
        assertEquals(1, mTextView.getPaddingTop());
        assertEquals(2, mTextView.getPaddingRight());
        assertEquals(4, mTextView.getPaddingBottom());

        mTextView.setPadding(10, 20, 30, 40);
        assertEquals(10, mTextView.getPaddingLeft());
        assertEquals(20, mTextView.getPaddingTop());
        assertEquals(30, mTextView.getPaddingRight());
        assertEquals(40, mTextView.getPaddingBottom());
    }

    public void testSetTextAppearance() {
        mTextView = new TextView(mActivity);

        mTextView.setTextAppearance(mActivity, R.style.TextAppearance_All);
        assertEquals(mActivity.getResources().getColor(R.drawable.black),
                mTextView.getCurrentTextColor());
        assertEquals(20f, mTextView.getTextSize(), 0.01f);
        assertEquals(Typeface.BOLD, mTextView.getTypeface().getStyle());
        assertEquals(mActivity.getResources().getColor(R.drawable.red),
                mTextView.getCurrentHintTextColor());
        assertEquals(mActivity.getResources().getColor(R.drawable.blue),
                mTextView.getLinkTextColors().getDefaultColor());

        mTextView.setTextAppearance(mActivity, R.style.TextAppearance_Colors);
        assertEquals(mActivity.getResources().getColor(R.drawable.black),
                mTextView.getCurrentTextColor());
        assertEquals(mActivity.getResources().getColor(R.drawable.blue),
                mTextView.getCurrentHintTextColor());
        assertEquals(mActivity.getResources().getColor(R.drawable.yellow),
                mTextView.getLinkTextColors().getDefaultColor());

        mTextView.setTextAppearance(mActivity, R.style.TextAppearance_NotColors);
        assertEquals(17f, mTextView.getTextSize(), 0.01f);
        assertEquals(Typeface.NORMAL, mTextView.getTypeface().getStyle());

        mTextView.setTextAppearance(mActivity, R.style.TextAppearance_Style);
        assertEquals(null, mTextView.getTypeface());
    }

    public void testOnPreDraw() {
        // Do not test. Implementation details.
    }

    public void testSetHorizontallyScrolling() {
        // make the text view has more than one line
        mTextView = findTextView(R.id.textview_text);
        setWidth(mTextView.getWidth() >> 1);
        assertTrue(mTextView.getLineCount() > 1);

        setHorizontallyScrolling(true);
        assertEquals(1, mTextView.getLineCount());

        setHorizontallyScrolling(false);
        assertTrue(mTextView.getLineCount() > 1);
    }

    public void testComputeHorizontalScrollRange() {
        MockTextView textView = new MockTextView(mActivity);
        // test when layout is null
        assertNull(textView.getLayout());
        assertEquals(textView.getWidth(), textView.computeHorizontalScrollRange());

        textView.setFrame(0, 0, 40, 50);
        assertEquals(textView.getWidth(), textView.computeHorizontalScrollRange());

        // set the layout
        layout(textView);
        assertEquals(textView.getLayout().getWidth(), textView.computeHorizontalScrollRange());
    }

    public void testComputeVerticalScrollRange() {
        MockTextView textView = new MockTextView(mActivity);
        // test when layout is null
        assertNull(textView.getLayout());
        assertEquals(0, textView.computeVerticalScrollRange());

        textView.setFrame(0, 0, 40, 50);
        assertEquals(textView.getHeight(), textView.computeVerticalScrollRange());

        //set the layout
        layout(textView);
        assertEquals(textView.getLayout().getHeight(), textView.computeVerticalScrollRange());
    }

    public void testDrawableStateChanged() {
        MockTextView textView = new MockTextView(mActivity);

        textView.reset();
        textView.refreshDrawableState();
        assertTrue(textView.hasCalledDrawableStateChanged());
    }

    public void testGetDefaultEditable() {
        MockTextView textView = new MockTextView(mActivity);

        //the TextView#getDefaultEditable() does nothing, and always return false.
        assertFalse(textView.getDefaultEditable());
    }

    public void testGetDefaultMovementMethod() {
        MockTextView textView = new MockTextView(mActivity);

        //the TextView#getDefaultMovementMethod() does nothing, and always return null.
        assertNull(textView.getDefaultMovementMethod());
    }

    public void testOnCreateContextMenu() {
        // Do not test. Implementation details.
    }

    public void testOnDetachedFromWindow() {
        // Do not test. Implementation details.
    }

    public void testOnDraw() {
        // Do not test. Implementation details.
    }

    public void testOnFocusChanged() {
        // Do not test. Implementation details.
    }

    public void testOnMeasure() {
        // Do not test. Implementation details.
    }

    public void testOnTextChanged() {
        // Do not test. Implementation details.
    }

    public void testSetFrame() {
        MockTextView textView = new MockTextView(mActivity);

        //Assign a new size to this view
        assertTrue(textView.setFrame(0, 0, 320, 480));
        assertEquals(0, textView.getFrameLeft());
        assertEquals(0, textView.getFrameTop());
        assertEquals(320, textView.getFrameRight());
        assertEquals(480, textView.getFrameBottom());

        //Assign a same size to this view
        assertFalse(textView.setFrame(0, 0, 320, 480));

        //negative input
        assertTrue(textView.setFrame(-1, -1, -1, -1));
        assertEquals(-1, textView.getFrameLeft());
        assertEquals(-1, textView.getFrameTop());
        assertEquals(-1, textView.getFrameRight());
        assertEquals(-1, textView.getFrameBottom());
    }

    public void testGetFadingEdgeStrength() {
        final MockTextView textViewLeft = (MockTextView) mActivity.findViewById(
                R.id.mock_textview_left);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                textViewLeft.setEllipsize(null);
            }
        });
        mInstrumentation.waitForIdleSync();

        // fading is shown on right side if the text aligns left
        assertEquals(0.0f, textViewLeft.getLeftFadingEdgeStrength(), 0.01f);
        assertEquals(1.0f, textViewLeft.getRightFadingEdgeStrength(), 0.01f);

        final MockTextView textViewRight = (MockTextView) mActivity.findViewById(
                R.id.mock_textview_right);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                textViewRight.setEllipsize(null);
            }
        });
        mInstrumentation.waitForIdleSync();
        // fading is shown on left side if the text aligns right
        assertEquals(1.0f, textViewRight.getLeftFadingEdgeStrength(), 0.01f);
        assertEquals(0.0f, textViewRight.getRightFadingEdgeStrength(), 0.01f);

        final MockTextView textViewCenter = (MockTextView) mActivity.findViewById(
                R.id.mock_textview_center);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                textViewCenter.setEllipsize(null);
            }
        });
        mInstrumentation.waitForIdleSync();
        // fading is shown on both sides if the text aligns center
        assertEquals(1.0f, textViewCenter.getLeftFadingEdgeStrength(), 0.01f);
        assertEquals(1.0f, textViewCenter.getRightFadingEdgeStrength(), 0.01f);
    }


    public void testMarquee() {
        final MockTextView textView = new MockTextView(mActivity);
        textView.setText(LONG_TEXT);
        textView.setSingleLine();
        textView.setEllipsize(TruncateAt.MARQUEE);
        textView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));

        final FrameLayout layout = new FrameLayout(mActivity);
        layout.addView(textView);

        // make the fading to be shown
        textView.setHorizontalFadingEdgeEnabled(true);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mActivity.setContentView(layout);
            }
        });
        mInstrumentation.waitForIdleSync();

        TestSelectedRunnable runnable = new TestSelectedRunnable(textView) {
            public void run() {
                textView.setMarqueeRepeatLimit(-1);
                // force the marquee to start
                saveIsSelected1();
                textView.setSelected(true);
                saveIsSelected2();
            }
        };
        mActivity.runOnUiThread(runnable);

        // wait for the marquee to run
        // fading is shown on both sides if the marquee runs for a while
        new PollingCheck(TIMEOUT) {
            @Override
            protected boolean check() {
                return textView.getLeftFadingEdgeStrength() > 0.0f
                        && textView.getRightFadingEdgeStrength() > 0.0f;
            }
        }.run();

        final float leftFadingEdgeStrength = textView.getLeftFadingEdgeStrength();
        final float rightFadingEdgeStrength = textView.getRightFadingEdgeStrength();

        // wait for the marquee to continue
        // the left fading becomes thicker while the right fading becomes thiner
        // as the text moves towards left
        new PollingCheck(TIMEOUT) {
            @Override
            protected boolean check() {
                return leftFadingEdgeStrength < textView.getLeftFadingEdgeStrength()
                        && rightFadingEdgeStrength > textView.getRightFadingEdgeStrength();
            }
        }.run();
        assertFalse(runnable.getIsSelected1());
        assertTrue(runnable.getIsSelected2());

        runnable = new TestSelectedRunnable(textView) {
            public void run() {
                textView.setMarqueeRepeatLimit(0);
                // force the marquee to stop
                saveIsSelected1();
                textView.setSelected(false);
                saveIsSelected2();
                textView.setGravity(Gravity.LEFT);
            }
        };
        // force the marquee to stop
        mActivity.runOnUiThread(runnable);
        mInstrumentation.waitForIdleSync();
        assertTrue(runnable.getIsSelected1());
        assertFalse(runnable.getIsSelected2());
        assertEquals(0.0f, textView.getLeftFadingEdgeStrength(), 0.01f);
        assertTrue(textView.getRightFadingEdgeStrength() > 0.0f);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                textView.setGravity(Gravity.RIGHT);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertTrue(textView.getLeftFadingEdgeStrength() > 0.0f);
        assertEquals(0.0f, textView.getRightFadingEdgeStrength(), 0.01f);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                textView.setGravity(Gravity.CENTER_HORIZONTAL);
            }
        });
        mInstrumentation.waitForIdleSync();
        // there is no left fading (Is it correct?)
        assertEquals(0.0f, textView.getLeftFadingEdgeStrength(), 0.01f);
        assertTrue(textView.getRightFadingEdgeStrength() > 0.0f);
    }

    public void testOnKeyMultiple() {
        // Do not test. Implementation details.
    }

    public void testAccessInputExtras() throws XmlPullParserException, IOException {
        TextView textView = new TextView(mActivity);
        textView.setText(null, BufferType.EDITABLE);
        textView.setInputType(InputType.TYPE_CLASS_TEXT);

        // do not create the extras
        assertNull(textView.getInputExtras(false));

        // create if it does not exist
        Bundle inputExtras = textView.getInputExtras(true);
        assertNotNull(inputExtras);
        assertTrue(inputExtras.isEmpty());

        // it is created already
        assertNotNull(textView.getInputExtras(false));

        try {
            textView.setInputExtras(R.xml.input_extras);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    public void testAccessContentType() {
        TextView textView = new TextView(mActivity);
        textView.setText(null, BufferType.EDITABLE);
        textView.setKeyListener(null);
        textView.setTransformationMethod(null);

        textView.setInputType(InputType.TYPE_CLASS_DATETIME
                | InputType.TYPE_DATETIME_VARIATION_NORMAL);
        assertEquals(InputType.TYPE_CLASS_DATETIME
                | InputType.TYPE_DATETIME_VARIATION_NORMAL, textView.getInputType());
        assertTrue(textView.getKeyListener() instanceof DateTimeKeyListener);

        textView.setInputType(InputType.TYPE_CLASS_DATETIME
                | InputType.TYPE_DATETIME_VARIATION_DATE);
        assertEquals(InputType.TYPE_CLASS_DATETIME
                | InputType.TYPE_DATETIME_VARIATION_DATE, textView.getInputType());
        assertTrue(textView.getKeyListener() instanceof DateKeyListener);

        textView.setInputType(InputType.TYPE_CLASS_DATETIME
                | InputType.TYPE_DATETIME_VARIATION_TIME);
        assertEquals(InputType.TYPE_CLASS_DATETIME
                | InputType.TYPE_DATETIME_VARIATION_TIME, textView.getInputType());
        assertTrue(textView.getKeyListener() instanceof TimeKeyListener);

        textView.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);
        assertEquals(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED, textView.getInputType());
        assertSame(textView.getKeyListener(), DigitsKeyListener.getInstance(true, true));

        textView.setInputType(InputType.TYPE_CLASS_PHONE);
        assertEquals(InputType.TYPE_CLASS_PHONE, textView.getInputType());
        assertTrue(textView.getKeyListener() instanceof DialerKeyListener);

        textView.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        assertEquals(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT, textView.getInputType());
        assertSame(textView.getKeyListener(), TextKeyListener.getInstance(true, Capitalize.NONE));

        textView.setSingleLine();
        assertTrue(textView.getTransformationMethod() instanceof SingleLineTransformationMethod);
        textView.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        assertEquals(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, textView.getInputType());
        assertSame(textView.getKeyListener(),
                TextKeyListener.getInstance(false, Capitalize.CHARACTERS));
        assertNull(textView.getTransformationMethod());

        textView.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        assertEquals(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS, textView.getInputType());
        assertSame(textView.getKeyListener(),
                TextKeyListener.getInstance(false, Capitalize.WORDS));
        assertTrue(textView.getTransformationMethod() instanceof SingleLineTransformationMethod);

        textView.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        assertEquals(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, textView.getInputType());
        assertSame(textView.getKeyListener(),
                TextKeyListener.getInstance(false, Capitalize.SENTENCES));

        textView.setInputType(InputType.TYPE_NULL);
        assertEquals(InputType.TYPE_NULL, textView.getInputType());
        assertTrue(textView.getKeyListener() instanceof TextKeyListener);
    }

    public void testAccessRawContentType() {
        TextView textView = new TextView(mActivity);
        textView.setText(null, BufferType.EDITABLE);
        textView.setKeyListener(null);
        textView.setTransformationMethod(null);

        textView.setRawInputType(InputType.TYPE_CLASS_DATETIME
                | InputType.TYPE_DATETIME_VARIATION_NORMAL);
        assertEquals(InputType.TYPE_CLASS_DATETIME
                | InputType.TYPE_DATETIME_VARIATION_NORMAL, textView.getInputType());
        assertNull(textView.getTransformationMethod());
        assertNull(textView.getKeyListener());

        textView.setRawInputType(InputType.TYPE_CLASS_DATETIME
                | InputType.TYPE_DATETIME_VARIATION_DATE);
        assertEquals(InputType.TYPE_CLASS_DATETIME
                | InputType.TYPE_DATETIME_VARIATION_DATE, textView.getInputType());
        assertNull(textView.getTransformationMethod());
        assertNull(textView.getKeyListener());

        textView.setRawInputType(InputType.TYPE_CLASS_DATETIME
                | InputType.TYPE_DATETIME_VARIATION_TIME);
        assertEquals(InputType.TYPE_CLASS_DATETIME
                | InputType.TYPE_DATETIME_VARIATION_TIME, textView.getInputType());
        assertNull(textView.getTransformationMethod());
        assertNull(textView.getKeyListener());

        textView.setRawInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);
        assertEquals(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED, textView.getInputType());
        assertNull(textView.getTransformationMethod());
        assertNull(textView.getKeyListener());

        textView.setRawInputType(InputType.TYPE_CLASS_PHONE);
        assertEquals(InputType.TYPE_CLASS_PHONE, textView.getInputType());
        assertNull(textView.getTransformationMethod());
        assertNull(textView.getKeyListener());

        textView.setRawInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        assertEquals(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT, textView.getInputType());
        assertNull(textView.getTransformationMethod());
        assertNull(textView.getKeyListener());

        textView.setSingleLine();
        assertTrue(textView.getTransformationMethod() instanceof SingleLineTransformationMethod);
        textView.setRawInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        assertEquals(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, textView.getInputType());
        assertTrue(textView.getTransformationMethod() instanceof SingleLineTransformationMethod);
        assertNull(textView.getKeyListener());

        textView.setRawInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        assertEquals(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS, textView.getInputType());
        assertTrue(textView.getTransformationMethod() instanceof SingleLineTransformationMethod);
        assertNull(textView.getKeyListener());

        textView.setRawInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        assertEquals(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, textView.getInputType());
        assertTrue(textView.getTransformationMethod() instanceof SingleLineTransformationMethod);
        assertNull(textView.getKeyListener());

        textView.setRawInputType(InputType.TYPE_NULL);
        assertTrue(textView.getTransformationMethod() instanceof SingleLineTransformationMethod);
        assertNull(textView.getKeyListener());
    }

    public void testOnPrivateIMECommand() {
        // Do not test. Implementation details.
    }

    public void testFoo() {
        // Do not test. Implementation details.
    }

    public void testVerifyDrawable() {
        MockTextView textView = new MockTextView(mActivity);

        Drawable d = getDrawable(R.drawable.pass);
        assertFalse(textView.verifyDrawable(d));

        textView.setCompoundDrawables(null, d, null, null);
        assertTrue(textView.verifyDrawable(d));
    }

    public void testAccessPrivateImeOptions() {
        mTextView = findTextView(R.id.textview_text);
        assertNull(mTextView.getPrivateImeOptions());

        mTextView.setPrivateImeOptions("com.example.myapp.SpecialMode=3");
        assertEquals("com.example.myapp.SpecialMode=3", mTextView.getPrivateImeOptions());

        mTextView.setPrivateImeOptions(null);
        assertNull(mTextView.getPrivateImeOptions());
    }

    public void testSetOnEditorActionListener() {
        mTextView = findTextView(R.id.textview_text);

        MockOnEditorActionListener listener = new MockOnEditorActionListener();
        assertFalse(listener.isOnEditorActionCalled());

        mTextView.setOnEditorActionListener(listener);
        assertFalse(listener.isOnEditorActionCalled());

        mTextView.onEditorAction(EditorInfo.IME_ACTION_DONE);
        assertTrue(listener.isOnEditorActionCalled());
    }

    public void testAccessImeOptions() {
        mTextView = findTextView(R.id.textview_text);
        assertEquals(EditorInfo.IME_NULL, mTextView.getImeOptions());

        mTextView.setImeOptions(EditorInfo.IME_ACTION_GO);
        assertEquals(EditorInfo.IME_ACTION_GO, mTextView.getImeOptions());

        mTextView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        assertEquals(EditorInfo.IME_ACTION_DONE, mTextView.getImeOptions());

        mTextView.setImeOptions(EditorInfo.IME_NULL);
        assertEquals(EditorInfo.IME_NULL, mTextView.getImeOptions());
    }

    public void testAccessImeActionLabel() {
        mTextView = findTextView(R.id.textview_text);
        assertNull(mTextView.getImeActionLabel());
        assertEquals(0, mTextView.getImeActionId());

        mTextView.setImeActionLabel("pinyin", 1);
        assertEquals("pinyin", mTextView.getImeActionLabel().toString());
        assertEquals(1, mTextView.getImeActionId());
    }

    @UiThreadTest
    public void testSetExtractedText() {
        mTextView = findTextView(R.id.textview_text);
        assertEquals(mActivity.getResources().getString(R.string.text_view_hello),
                mTextView.getText().toString());

        ExtractedText et = new ExtractedText();
        et.text = "test";

        mTextView.setExtractedText(et);
        assertEquals("test", mTextView.getText().toString());
    }

    public void testMoveCursorToVisibleOffset() throws Throwable {
        mTextView = findTextView(R.id.textview_text);

        // not a spannable text
        runTestOnUiThread(new Runnable() {
            public void run() {
                assertFalse(mTextView.moveCursorToVisibleOffset());
            }
        });
        mInstrumentation.waitForIdleSync();

        // a selection range
        final String spannableText = "text";
        mTextView = new TextView(mActivity);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mTextView.setText(spannableText, BufferType.SPANNABLE);
            }
        });
        mInstrumentation.waitForIdleSync();
        Selection.setSelection((Spannable) mTextView.getText(), 0, spannableText.length());

        assertEquals(0, mTextView.getSelectionStart());
        assertEquals(spannableText.length(), mTextView.getSelectionEnd());
        runTestOnUiThread(new Runnable() {
            public void run() {
                assertFalse(mTextView.moveCursorToVisibleOffset());
            }
        });
        mInstrumentation.waitForIdleSync();

        // a spannable without range
        runTestOnUiThread(new Runnable() {
            public void run() {
                mTextView = findTextView(R.id.textview_text);
                mTextView.setText(spannableText, BufferType.SPANNABLE);
            }
        });
        mInstrumentation.waitForIdleSync();

        runTestOnUiThread(new Runnable() {
            public void run() {
                assertTrue(mTextView.moveCursorToVisibleOffset());
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    public void testIsInputMethodTarget() throws Throwable {
        mTextView = findTextView(R.id.textview_text);
        assertFalse(mTextView.isInputMethodTarget());

        assertFalse(mTextView.isFocused());
        runTestOnUiThread(new Runnable() {
           @Override
            public void run() {
               mTextView.setFocusable(true);
               mTextView.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertTrue(mTextView.isFocused());

        new PollingCheck() {
            @Override
            protected boolean check() {
                return mTextView.isInputMethodTarget();
            }
        }.run();
    }

    public void testBeginEndBatchEdit() {
        mTextView = findTextView(R.id.textview_text);

        mTextView.beginBatchEdit();
        mTextView.endBatchEdit();
    }

    @UiThreadTest
    public void testBringPointIntoView() throws Throwable {
        mTextView = findTextView(R.id.textview_text);
        assertFalse(mTextView.bringPointIntoView(1));

        mTextView.layout(0, 0, 100, 100);
        assertFalse(mTextView.bringPointIntoView(2));
    }

    public void testCancelLongPress() {
        mTextView = findTextView(R.id.textview_text);
        TouchUtils.longClickView(this, mTextView);
        mTextView.cancelLongPress();
    }

    @UiThreadTest
    public void testClearComposingText() {
        mTextView = findTextView(R.id.textview_text);
        mTextView.setText("Hello world!", BufferType.SPANNABLE);
        Spannable text = (Spannable) mTextView.getText();

        assertEquals(-1, BaseInputConnection.getComposingSpanStart(text));
        assertEquals(-1, BaseInputConnection.getComposingSpanStart(text));

        BaseInputConnection.setComposingSpans((Spannable) mTextView.getText());
        assertEquals(0, BaseInputConnection.getComposingSpanStart(text));
        assertEquals(0, BaseInputConnection.getComposingSpanStart(text));

        mTextView.clearComposingText();
        assertEquals(-1, BaseInputConnection.getComposingSpanStart(text));
        assertEquals(-1, BaseInputConnection.getComposingSpanStart(text));
    }

    public void testComputeVerticalScrollExtent() {
        MockTextView textView = new MockTextView(mActivity);
        assertEquals(0, textView.computeVerticalScrollExtent());

        Drawable d = getDrawable(R.drawable.pass);
        textView.setCompoundDrawables(null, d, null, d);

        assertEquals(0, textView.computeVerticalScrollExtent());
    }

    @UiThreadTest
    public void testDidTouchFocusSelect() {
        mTextView = new EditText(mActivity);
        assertFalse(mTextView.didTouchFocusSelect());

        mTextView.setFocusable(true);
        mTextView.requestFocus();
        assertTrue(mTextView.didTouchFocusSelect());
    }

    public void testExtractText() {
        mTextView = new TextView(mActivity);

        ExtractedTextRequest request = new ExtractedTextRequest();
        ExtractedText outText = new ExtractedText();

        request.token = 0;
        request.flags = 10;
        request.hintMaxLines = 2;
        request.hintMaxChars = 20;
        assertTrue(mTextView.extractText(request, outText));

        mTextView = findTextView(R.id.textview_text);
        assertTrue(mTextView.extractText(request, outText));

        assertEquals(mActivity.getResources().getString(R.string.text_view_hello),
                outText.text.toString());
    }

    @UiThreadTest
    public void testTextDirectionDefault() {
        TextView tv = new TextView(mActivity);
        assertEquals(View.TEXT_DIRECTION_INHERIT, tv.getRawTextDirection());
    }

    @UiThreadTest
    public void testSetGetTextDirection() {
        TextView tv = new TextView(mActivity);

        tv.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        assertEquals(View.TEXT_DIRECTION_FIRST_STRONG, tv.getRawTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        assertEquals(View.TEXT_DIRECTION_ANY_RTL, tv.getRawTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_INHERIT);
        assertEquals(View.TEXT_DIRECTION_INHERIT, tv.getRawTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_LTR);
        assertEquals(View.TEXT_DIRECTION_LTR, tv.getRawTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_RTL);
        assertEquals(View.TEXT_DIRECTION_RTL, tv.getRawTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        assertEquals(View.TEXT_DIRECTION_LOCALE, tv.getRawTextDirection());
    }

    @UiThreadTest
    public void testGetResolvedTextDirectionLtr() {
        TextView tv = new TextView(mActivity);
        tv.setText("this is a test");

        assertEquals(View.TEXT_DIRECTION_FIRST_STRONG, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        assertEquals(View.TEXT_DIRECTION_FIRST_STRONG, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        assertEquals(View.TEXT_DIRECTION_ANY_RTL, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_INHERIT);
        assertEquals(View.TEXT_DIRECTION_FIRST_STRONG, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_LTR);
        assertEquals(View.TEXT_DIRECTION_LTR, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_RTL);
        assertEquals(View.TEXT_DIRECTION_RTL, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        assertEquals(View.TEXT_DIRECTION_LOCALE, tv.getTextDirection());
    }

    @UiThreadTest
    public void testGetResolvedTextDirectionLtrWithInheritance() {
        LinearLayout ll = new LinearLayout(mActivity);
        ll.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);

        TextView tv = new TextView(mActivity);
        tv.setText("this is a test");
        ll.addView(tv);

        tv.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        assertEquals(View.TEXT_DIRECTION_FIRST_STRONG, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        assertEquals(View.TEXT_DIRECTION_ANY_RTL, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_INHERIT);
        assertEquals(View.TEXT_DIRECTION_ANY_RTL, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_LTR);
        assertEquals(View.TEXT_DIRECTION_LTR, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_RTL);
        assertEquals(View.TEXT_DIRECTION_RTL, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        assertEquals(View.TEXT_DIRECTION_LOCALE, tv.getTextDirection());
    }

    @UiThreadTest
    public void testGetResolvedTextDirectionRtl() {
        TextView tv = new TextView(mActivity);
        tv.setText("\u05DD\u05DE"); // hebrew

        assertEquals(View.TEXT_DIRECTION_FIRST_STRONG, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        assertEquals(View.TEXT_DIRECTION_FIRST_STRONG, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        assertEquals(View.TEXT_DIRECTION_ANY_RTL, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_INHERIT);
        assertEquals(View.TEXT_DIRECTION_FIRST_STRONG, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_LTR);
        assertEquals(View.TEXT_DIRECTION_LTR, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_RTL);
        assertEquals(View.TEXT_DIRECTION_RTL, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        assertEquals(View.TEXT_DIRECTION_LOCALE, tv.getTextDirection());
    }

    @UiThreadTest
    public void testGetResolvedTextDirectionRtlWithInheritance() {
        LinearLayout ll = new LinearLayout(mActivity);
        ll.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);

        TextView tv = new TextView(mActivity);
        tv.setText("\u05DD\u05DE"); // hebrew
        ll.addView(tv);

        tv.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        assertEquals(View.TEXT_DIRECTION_FIRST_STRONG, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        assertEquals(View.TEXT_DIRECTION_ANY_RTL, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_INHERIT);
        assertEquals(View.TEXT_DIRECTION_FIRST_STRONG, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_LTR);
        assertEquals(View.TEXT_DIRECTION_LTR, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_RTL);
        assertEquals(View.TEXT_DIRECTION_RTL, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        assertEquals(View.TEXT_DIRECTION_LOCALE, tv.getTextDirection());

        // Force to RTL text direction on the layout
        ll.setTextDirection(View.TEXT_DIRECTION_RTL);

        tv.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        assertEquals(View.TEXT_DIRECTION_FIRST_STRONG, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        assertEquals(View.TEXT_DIRECTION_ANY_RTL, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_INHERIT);
        assertEquals(View.TEXT_DIRECTION_RTL, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_LTR);
        assertEquals(View.TEXT_DIRECTION_LTR, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_RTL);
        assertEquals(View.TEXT_DIRECTION_RTL, tv.getTextDirection());

        tv.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        assertEquals(View.TEXT_DIRECTION_LOCALE, tv.getTextDirection());
    }

    @UiThreadTest
    public void testResetTextDirection() {
        LinearLayout ll = (LinearLayout) mActivity.findViewById(R.id.layout_textviewtest);
        TextView tv = (TextView) mActivity.findViewById(R.id.textview_rtl);

        ll.setTextDirection(View.TEXT_DIRECTION_RTL);
        tv.setTextDirection(View.TEXT_DIRECTION_INHERIT);
        assertEquals(View.TEXT_DIRECTION_RTL, tv.getTextDirection());

        // No reset when we remove the view
        ll.removeView(tv);
        assertEquals(View.TEXT_DIRECTION_RTL, tv.getTextDirection());

        // Reset is done when we add the view
        ll.addView(tv);
        assertEquals(View.TEXT_DIRECTION_FIRST_STRONG, tv.getTextDirection());
    }

    @UiThreadTest
    public void testTextAlignmentDefault() {
        TextView tv = new TextView(getActivity());
        assertEquals(View.TEXT_ALIGNMENT_GRAVITY, tv.getRawTextAlignment());
        // resolved default text alignment is GRAVITY
        assertEquals(View.TEXT_ALIGNMENT_GRAVITY, tv.getTextAlignment());
    }

    @UiThreadTest
    public void testSetGetTextAlignment() {
        TextView tv = new TextView(getActivity());

        tv.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        assertEquals(View.TEXT_ALIGNMENT_GRAVITY, tv.getRawTextAlignment());

        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        assertEquals(View.TEXT_ALIGNMENT_CENTER, tv.getRawTextAlignment());

        tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        assertEquals(View.TEXT_ALIGNMENT_TEXT_START, tv.getRawTextAlignment());

        tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        assertEquals(View.TEXT_ALIGNMENT_TEXT_END, tv.getRawTextAlignment());

        tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        assertEquals(View.TEXT_ALIGNMENT_VIEW_START, tv.getRawTextAlignment());

        tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        assertEquals(View.TEXT_ALIGNMENT_VIEW_END, tv.getRawTextAlignment());
    }

    @UiThreadTest
    public void testGetResolvedTextAlignment() {
        TextView tv = new TextView(getActivity());

        assertEquals(View.TEXT_ALIGNMENT_GRAVITY, tv.getTextAlignment());

        // Test center alignment first so that we dont hit the default case
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        assertEquals(View.TEXT_ALIGNMENT_CENTER, tv.getTextAlignment());

        // Test the default case too
        tv.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        assertEquals(View.TEXT_ALIGNMENT_GRAVITY, tv.getTextAlignment());

        tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        assertEquals(View.TEXT_ALIGNMENT_TEXT_START, tv.getTextAlignment());

        tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        assertEquals(View.TEXT_ALIGNMENT_TEXT_END, tv.getTextAlignment());

        tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        assertEquals(View.TEXT_ALIGNMENT_VIEW_START, tv.getTextAlignment());

        tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        assertEquals(View.TEXT_ALIGNMENT_VIEW_END, tv.getTextAlignment());
    }

    @UiThreadTest
    public void testGetResolvedTextAlignmentWithInheritance() {
        LinearLayout ll = new LinearLayout(getActivity());
        ll.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);

        TextView tv = new TextView(getActivity());
        ll.addView(tv);

        // check defaults
        assertEquals(View.TEXT_ALIGNMENT_GRAVITY, tv.getRawTextAlignment());
        assertEquals(View.TEXT_ALIGNMENT_GRAVITY, tv.getTextAlignment());

        // set inherit and check that child is following parent
        tv.setTextAlignment(View.TEXT_ALIGNMENT_INHERIT);
        assertEquals(View.TEXT_ALIGNMENT_INHERIT, tv.getRawTextAlignment());

        ll.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        assertEquals(View.TEXT_ALIGNMENT_CENTER, tv.getTextAlignment());

        ll.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        assertEquals(View.TEXT_ALIGNMENT_TEXT_START, tv.getTextAlignment());

        ll.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        assertEquals(View.TEXT_ALIGNMENT_TEXT_END, tv.getTextAlignment());

        ll.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        assertEquals(View.TEXT_ALIGNMENT_VIEW_START, tv.getTextAlignment());

        ll.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        assertEquals(View.TEXT_ALIGNMENT_VIEW_END, tv.getTextAlignment());

        // now get rid of the inheritance but still change the parent
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        ll.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        assertEquals(View.TEXT_ALIGNMENT_CENTER, tv.getTextAlignment());

        ll.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        assertEquals(View.TEXT_ALIGNMENT_CENTER, tv.getTextAlignment());

        ll.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        assertEquals(View.TEXT_ALIGNMENT_CENTER, tv.getTextAlignment());

        ll.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        assertEquals(View.TEXT_ALIGNMENT_CENTER, tv.getTextAlignment());

        ll.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        assertEquals(View.TEXT_ALIGNMENT_CENTER, tv.getTextAlignment());
    }

    @UiThreadTest
    public void testResetTextAlignment() {
        TextViewStubActivity activity = getActivity();

        LinearLayout ll = (LinearLayout) activity.findViewById(R.id.layout_textviewtest);
        TextView tv = (TextView) activity.findViewById(R.id.textview_rtl);

        ll.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_INHERIT);
        assertEquals(View.TEXT_ALIGNMENT_CENTER, tv.getTextAlignment());

        // No reset when we remove the view
        ll.removeView(tv);
        assertEquals(View.TEXT_ALIGNMENT_CENTER, tv.getTextAlignment());

        // Reset is done when we add the view
        // Default text alignment is GRAVITY
        ll.addView(tv);
        assertEquals(View.TEXT_ALIGNMENT_GRAVITY, tv.getTextAlignment());
    }

    @UiThreadTest
    public void testDrawableResolution() {
        final int LEFT = 0;
        final int TOP = 1;
        final int RIGHT = 2;
        final int BOTTOM = 3;

        TextViewStubActivity activity = getActivity();

        // Case 1.1: left / right drawable defined in default LTR mode
        TextView tv = (TextView) activity.findViewById(R.id.textview_drawable_1_1);
        Drawable[] drawables = tv.getCompoundDrawables();

        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_blue),
                ((BitmapDrawable) drawables[LEFT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_red),
                ((BitmapDrawable) drawables[RIGHT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_green),
                ((BitmapDrawable) drawables[TOP]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_yellow),
                ((BitmapDrawable) drawables[BOTTOM]).getBitmap());

        // Case 1.2: left / right drawable defined in default RTL mode
        tv = (TextView) activity.findViewById(R.id.textview_drawable_1_2);
        drawables = tv.getCompoundDrawables();

        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_blue),
                ((BitmapDrawable) drawables[LEFT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_red),
                ((BitmapDrawable) drawables[RIGHT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_green),
                ((BitmapDrawable) drawables[TOP]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_yellow),
                ((BitmapDrawable) drawables[BOTTOM]).getBitmap());

        // Case 2.1: start / end drawable defined in LTR mode
        tv = (TextView) activity.findViewById(R.id.textview_drawable_2_1);
        drawables = tv.getCompoundDrawables();

        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_blue),
                ((BitmapDrawable) drawables[LEFT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_red),
                ((BitmapDrawable) drawables[RIGHT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_green),
                ((BitmapDrawable) drawables[TOP]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_yellow),
                ((BitmapDrawable) drawables[BOTTOM]).getBitmap());

        // Case 2.2: start / end drawable defined in RTL mode
        tv = (TextView) activity.findViewById(R.id.textview_drawable_2_2);
        drawables = tv.getCompoundDrawables();

        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_red),
                ((BitmapDrawable) drawables[LEFT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_blue),
                ((BitmapDrawable) drawables[RIGHT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_green),
                ((BitmapDrawable) drawables[TOP]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_yellow),
                ((BitmapDrawable) drawables[BOTTOM]).getBitmap());

        // Case 3.1: left / right / start / end drawable defined in LTR mode
        tv = (TextView) activity.findViewById(R.id.textview_drawable_3_1);
        drawables = tv.getCompoundDrawables();

        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_blue),
                ((BitmapDrawable) drawables[LEFT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_red),
                ((BitmapDrawable) drawables[RIGHT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_green),
                ((BitmapDrawable) drawables[TOP]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_yellow),
                ((BitmapDrawable) drawables[BOTTOM]).getBitmap());

        // Case 3.2: left / right / start / end drawable defined in RTL mode
        tv = (TextView) activity.findViewById(R.id.textview_drawable_3_2);
        drawables = tv.getCompoundDrawables();

        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_red),
                ((BitmapDrawable) drawables[LEFT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_blue),
                ((BitmapDrawable) drawables[RIGHT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_green),
                ((BitmapDrawable) drawables[TOP]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_yellow),
                ((BitmapDrawable) drawables[BOTTOM]).getBitmap());

        // Case 4.1: start / end drawable defined in LTR mode inside a layout
        // that defines the layout direction
        tv = (TextView) activity.findViewById(R.id.textview_drawable_4_1);
        drawables = tv.getCompoundDrawables();

        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_blue),
                ((BitmapDrawable) drawables[LEFT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_red),
                ((BitmapDrawable) drawables[RIGHT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_green),
                ((BitmapDrawable) drawables[TOP]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_yellow),
                ((BitmapDrawable) drawables[BOTTOM]).getBitmap());

        // Case 4.2: start / end drawable defined in RTL mode inside a layout
        // that defines the layout direction
        tv = (TextView) activity.findViewById(R.id.textview_drawable_4_2);
        drawables = tv.getCompoundDrawables();

        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_red),
                ((BitmapDrawable) drawables[LEFT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_blue),
                ((BitmapDrawable) drawables[RIGHT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_green),
                ((BitmapDrawable) drawables[TOP]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_yellow),
                ((BitmapDrawable) drawables[BOTTOM]).getBitmap());

        // Case 5.1: left / right / start / end drawable defined in LTR mode inside a layout
        // that defines the layout direction
        tv = (TextView) activity.findViewById(R.id.textview_drawable_3_1);
        drawables = tv.getCompoundDrawables();

        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_blue),
                ((BitmapDrawable) drawables[LEFT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_red),
                ((BitmapDrawable) drawables[RIGHT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_green),
                ((BitmapDrawable) drawables[TOP]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_yellow),
                ((BitmapDrawable) drawables[BOTTOM]).getBitmap());

        // Case 5.2: left / right / start / end drawable defined in RTL mode inside a layout
        // that defines the layout direction
        tv = (TextView) activity.findViewById(R.id.textview_drawable_3_2);
        drawables = tv.getCompoundDrawables();

        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_red),
                ((BitmapDrawable) drawables[LEFT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_blue),
                ((BitmapDrawable) drawables[RIGHT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_green),
                ((BitmapDrawable) drawables[TOP]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_yellow),
                ((BitmapDrawable) drawables[BOTTOM]).getBitmap());
    }

    @UiThreadTest
    public void testDrawableResolution2() {
        final int LEFT = 0;
        final int TOP = 1;
        final int RIGHT = 2;
        final int BOTTOM = 3;

        TextViewStubActivity activity = getActivity();

        // Case 1.1: left / right drawable defined in default LTR mode
        TextView tv = (TextView) activity.findViewById(R.id.textview_drawable_1_1);
        Drawable[] drawables = tv.getCompoundDrawables();

        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_blue),
                ((BitmapDrawable) drawables[LEFT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_red),
                ((BitmapDrawable) drawables[RIGHT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_green),
                ((BitmapDrawable) drawables[TOP]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_yellow),
                ((BitmapDrawable) drawables[BOTTOM]).getBitmap());

        tv.setCompoundDrawables(null, null, getDrawable(R.drawable.icon_yellow), null);
        drawables = tv.getCompoundDrawables();

        assertNull(drawables[LEFT]);
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_yellow),
                ((BitmapDrawable) drawables[RIGHT]).getBitmap());
        assertNull(drawables[TOP]);
        assertNull(drawables[BOTTOM]);

        tv = (TextView) activity.findViewById(R.id.textview_drawable_1_2);
        drawables = tv.getCompoundDrawables();

        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_blue),
                ((BitmapDrawable) drawables[LEFT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_red),
                ((BitmapDrawable) drawables[RIGHT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_green),
                ((BitmapDrawable) drawables[TOP]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_yellow),
                ((BitmapDrawable) drawables[BOTTOM]).getBitmap());

        tv.setCompoundDrawables(getDrawable(R.drawable.icon_yellow), null, null, null);
        drawables = tv.getCompoundDrawables();

        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_yellow),
                ((BitmapDrawable) drawables[LEFT]).getBitmap());
        assertNull(drawables[RIGHT]);
        assertNull(drawables[TOP]);
        assertNull(drawables[BOTTOM]);

        tv = (TextView) activity.findViewById(R.id.textview_ltr);
        drawables = tv.getCompoundDrawables();

        assertNull(drawables[LEFT]);
        assertNull(drawables[RIGHT]);
        assertNull(drawables[TOP]);
        assertNull(drawables[BOTTOM]);

        tv.setCompoundDrawables(getDrawable(R.drawable.icon_blue), null, getDrawable(R.drawable.icon_red), null);
        drawables = tv.getCompoundDrawables();

        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_blue),
                ((BitmapDrawable) drawables[LEFT]).getBitmap());
        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_red),
                ((BitmapDrawable) drawables[RIGHT]).getBitmap());
        assertNull(drawables[TOP]);
        assertNull(drawables[BOTTOM]);

        tv.setCompoundDrawablesRelative(getDrawable(R.drawable.icon_yellow), null, null, null);
        drawables = tv.getCompoundDrawables();

        WidgetTestUtils.assertEquals(getBitmap(R.drawable.icon_yellow),
                ((BitmapDrawable) drawables[LEFT]).getBitmap());
        assertNull(drawables[RIGHT]);
        assertNull(drawables[TOP]);
        assertNull(drawables[BOTTOM]);
    }

    private static class MockOnEditorActionListener implements OnEditorActionListener {
        private boolean isOnEditorActionCalled;

        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            isOnEditorActionCalled = true;
            return true;
        }

        public boolean isOnEditorActionCalled() {
            return isOnEditorActionCalled;
        }
    }

    private void layout(final TextView textView) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mActivity.setContentView(textView);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void layout(final int layoutId) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mActivity.setContentView(layoutId);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private TextView findTextView(int id) {
        return (TextView) mActivity.findViewById(id);
    }

    private int getAutoLinkMask(int id) {
        return findTextView(id).getAutoLinkMask();
    }

    private Bitmap getBitmap(int resid) {
        return ((BitmapDrawable) getDrawable(resid)).getBitmap();
    }

    private Drawable getDrawable(int resid) {
        return mActivity.getResources().getDrawable(resid);
    }

    private void setMaxWidth(final int pixels) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setMaxWidth(pixels);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void setMinWidth(final int pixels) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setMinWidth(pixels);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void setMaxHeight(final int pixels) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setMaxHeight(pixels);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void setMinHeight(final int pixels) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setMinHeight(pixels);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void setMinLines(final int minlines) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setMinLines(minlines);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    /**
     * Convenience for {@link TextView#setText(CharSequence, BufferType)}. And
     * the buffer type is fixed to SPANNABLE.
     *
     * @param tv the text view
     * @param content the content
     */
    private void setSpannableText(final TextView tv, final String content) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                tv.setText(content, BufferType.SPANNABLE);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void setLines(final int lines) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setLines(lines);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void setHorizontallyScrolling(final boolean whether) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setHorizontallyScrolling(whether);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void setWidth(final int pixels) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setWidth(pixels);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void setHeight(final int pixels) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setHeight(pixels);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void setMinEms(final int ems) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setMinEms(ems);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void setMaxEms(final int ems) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setMaxEms(ems);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void setEms(final int ems) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setEms(ems);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void setLineSpacing(final float add, final float mult) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setLineSpacing(add, mult);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private static abstract class TestSelectedRunnable implements Runnable {
        private TextView mTextView;
        private boolean mIsSelected1;
        private boolean mIsSelected2;

        public TestSelectedRunnable(TextView textview) {
            mTextView = textview;
        }

        public boolean getIsSelected1() {
            return mIsSelected1;
        }

        public boolean getIsSelected2() {
            return mIsSelected2;
        }

        public void saveIsSelected1() {
            mIsSelected1 = mTextView.isSelected();
        }

        public void saveIsSelected2() {
            mIsSelected2 = mTextView.isSelected();
        }
    }

    private static abstract class TestLayoutRunnable implements Runnable {
        private TextView mTextView;
        private Layout mLayout;

        public TestLayoutRunnable(TextView textview) {
            mTextView = textview;
        }

        public Layout getLayout() {
            return mLayout;
        }

        public void saveLayout() {
            mLayout = mTextView.getLayout();
        }
    }

    private class MockEditableFactory extends Editable.Factory {
        private boolean mhasCalledNewEditable;
        private CharSequence mSource;

        public boolean hasCalledNewEditable() {
            return mhasCalledNewEditable;
        }

        public void reset() {
            mhasCalledNewEditable = false;
            mSource = null;
        }

        public CharSequence getSource() {
            return mSource;
        }

        @Override
        public Editable newEditable(CharSequence source) {
            mhasCalledNewEditable = true;
            mSource = source;
            return super.newEditable(source);
        }
    }

    private class MockSpannableFactory extends Spannable.Factory {
        private boolean mHasCalledNewSpannable;
        private CharSequence mSource;

        public boolean hasCalledNewSpannable() {
            return mHasCalledNewSpannable;
        }

        public void reset() {
            mHasCalledNewSpannable = false;
            mSource = null;
        }

        public CharSequence getSource() {
            return mSource;
        }

        @Override
        public Spannable newSpannable(CharSequence source) {
            mHasCalledNewSpannable = true;
            mSource = source;
            return super.newSpannable(source);
        }
    }

    private static class MockTextWatcher implements TextWatcher {
        private boolean mHasCalledAfterTextChanged;
        private boolean mHasCalledBeforeTextChanged;
        private boolean mHasOnTextChanged;

        public void reset(){
            mHasCalledAfterTextChanged = false;
            mHasCalledBeforeTextChanged = false;
            mHasOnTextChanged = false;
        }

        public boolean hasCalledAfterTextChanged() {
            return mHasCalledAfterTextChanged;
        }

        public boolean hasCalledBeforeTextChanged() {
            return mHasCalledBeforeTextChanged;
        }

        public boolean hasCalledOnTextChanged() {
            return mHasOnTextChanged;
        }

        public void afterTextChanged(Editable s) {
            mHasCalledAfterTextChanged = true;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            mHasCalledBeforeTextChanged = true;
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mHasOnTextChanged = true;
        }
    }

    /**
     * The listener interface for receiving mockOnLongClick events. The class
     * that is interested in processing a mockOnLongClick event implements this
     * interface, and the object created with that class is registered with a
     * component using the component's
     * <code>addMockOnLongClickListener<code> method. When
     * the mockOnLongClick event occurs, that object's appropriate
     * method is invoked.
     *
     * @see MockOnLongClickEvent
     */
    private static class MockOnLongClickListener implements OnLongClickListener {
        private boolean mExpectedOnLongClickResult;
        private boolean mHasLongClicked;

        MockOnLongClickListener(boolean result) {
            mExpectedOnLongClickResult = result;
        }

        public boolean hasLongClicked() {
            return mHasLongClicked;
        }

        public boolean onLongClick(View v) {
            mHasLongClicked = true;
            return mExpectedOnLongClickResult;
        }
    }

    /**
     * The listener interface for receiving mockOnCreateContextMenu events. The
     * class that is interested in processing a mockOnCreateContextMenu event
     * implements this interface, and the object created with that class is
     * registered with a component using the component's
     * <code>addMockOnCreateContextMenuListener<code> method. When the
     * mockOnCreateContextMenu event occurs, that object's appropriate method is
     * invoked.
     *
     * @see MockOnCreateContextMenuEvent
     */
    private static class MockOnCreateContextMenuListener implements OnCreateContextMenuListener {
        private boolean mIsMenuItemsBlank;
        private boolean mHasCreatedContextMenu;

        MockOnCreateContextMenuListener(boolean isBlank) {
            this.mIsMenuItemsBlank = isBlank;
        }

        public boolean hasCreatedContextMenu() {
            return mHasCreatedContextMenu;
        }

        public void reset() {
            mHasCreatedContextMenu = false;
        }

        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            mHasCreatedContextMenu = true;
            if (!mIsMenuItemsBlank) {
                menu.add("menu item");
            }
        }
    }
}
