package com.xtremelabs.robolectric.shadows;

import android.util.DisplayMetrics;
import android.util.TypedValue;
import com.xtremelabs.robolectric.WithTestDefaultsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(WithTestDefaultsRunner.class)
public class TypedValueTest {

    @Test
    public void testApplyDimensionIsWired() throws Exception {
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.density = 0.5f;
        float convertedValue = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, metrics);
        assertThat(convertedValue, equalTo(50f));
    }
    
    @Test
    public void testCoerceBooleanToString() {
        String booleanFalseString = TypedValue.coerceToString(TypedValue.TYPE_INT_BOOLEAN, 0);
        String booleanTrueString = TypedValue.coerceToString(TypedValue.TYPE_INT_BOOLEAN, 1);
        
        assertThat(booleanFalseString, equalTo("false"));
        assertThat(booleanTrueString, equalTo("true"));
    }
    
    @Test
    public void testCoerceNullToString() {
        String nullString = TypedValue.coerceToString(TypedValue.TYPE_NULL, 0);
        
        assertThat(nullString, equalTo(null));
    }
    
    @Test
    public void testCoerceIntegerToString() {
        String intString = TypedValue.coerceToString(TypedValue.TYPE_INT_DEC, 37);
        
        assertThat(intString, equalTo("37"));
    }
    
    @Test
    public void testCoerceIntegerToHexString() {
        String hexString = TypedValue.coerceToString(TypedValue.TYPE_INT_HEX, 0xcafebabe);
        
        assertThat(hexString, equalTo("0xcafebabe"));
    }
    
    @Test
    public void testCoerceColorToString() {
        String colorString = TypedValue.coerceToString(TypedValue.TYPE_INT_COLOR_RGB8, 0xcafebabe);
        
        assertThat(colorString, equalTo("#cafebabe"));
    }
    
    @Test
    public void testSetTo() {
        TypedValue expectedValue = new TypedValue();
        expectedValue.assetCookie = 1;
        expectedValue.data = 3;
        expectedValue.density = 4;
        expectedValue.resourceId = 5;
        expectedValue.string = "string";
        expectedValue.type = 6;
        
        TypedValue actualValue = new TypedValue();
        actualValue.setTo(expectedValue);
        
        assertThat(expectedValue.assetCookie, equalTo(actualValue.assetCookie));
        assertThat(expectedValue.data, equalTo(actualValue.data));
        assertThat(expectedValue.density, equalTo(actualValue.density));
        assertThat(expectedValue.resourceId, equalTo(actualValue.resourceId));
        assertThat(expectedValue.string, equalTo(actualValue.string));
        assertThat(expectedValue.type, equalTo(actualValue.type));
    }
}
