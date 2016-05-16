package com.xtremelabs.robolectric.shadows;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import android.app.Activity;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.WithTestDefaultsRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(WithTestDefaultsRunner.class)
public class TypedArrayTest {
    private android.content.res.TypedArray typedArray;

    @Before
    public void setUp() throws Exception {
      typedArray = Robolectric.newInstanceOf(android.content.res.TypedArray.class);
    }

    @Test
    public void getResources() throws Exception {
        assertNotNull(new Activity().obtainStyledAttributes(null).getResources());
    }

    @Test
    public void testBooleanDefaultValue() {
        assertThat(typedArray.getBoolean(0, true), equalTo(true));
        assertThat(typedArray.getBoolean(0, false), equalTo(false));
    }

    @Test
    public void testIntDefaultValue() {
        assertThat(typedArray.getInt(0, 15), equalTo(15));
        assertThat(typedArray.getInteger(0, 24), equalTo(24));
    }

    @Test
    public void testFloatDefaultValue() {
        assertThat(typedArray.getFloat(0, 0.5f), equalTo(0.5f));
    }

    @Test
    public void testDimensionDefaultValue() {
        assertThat(typedArray.getDimension(0, 0.5f), equalTo(0.5f));
    }

    @Test
    public void testDimensionPixelOffsetDefaultValue() {
        assertThat(typedArray.getDimensionPixelOffset(0, 2), equalTo(2));
    }

    @Test
    public void testDimensionPixelSizeDefaultValue() {
        assertThat(typedArray.getDimensionPixelSize(0, 2), equalTo(2));
    }

    @Test
    public void testLayoutDimensionDefaultValue() {
        assertThat(typedArray.getLayoutDimension(0, 2), equalTo(2));
    }

    @Test
    public void testResourceIdDefaultValue() {
        assertThat(typedArray.getResourceId(0, 2), equalTo(2));
    }
}
