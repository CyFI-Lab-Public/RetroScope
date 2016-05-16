package com.xtremelabs.robolectric.shadows;

import android.util.DisplayMetrics;
import android.util.TypedValue;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.internal.RealObject;

import static android.util.TypedValue.*;


/**
 * Portions of this file were copied from the Android source code,
 * licenced under the Apache License, Version 2.0
 *
 * http://www.google.com/codesearch/p?hl=en#uX1GffpyOZk/core/java/android/util/TypedValue.java
 */

@SuppressWarnings({"UnusedDeclaration"})
@Implements(TypedValue.class)
public class ShadowTypedValue {
    
    @RealObject TypedValue typedValue;
    
    @Implementation
    public final float getFloat() {
        return Float.intBitsToFloat(typedValue.data);
    }

    private static final float MANTISSA_MULT =
        1.0f / (1<<TypedValue.COMPLEX_MANTISSA_SHIFT);
    private static final float[] RADIX_MULTS = new float[] {
        1.0f*MANTISSA_MULT, 1.0f/(1<<7)*MANTISSA_MULT,
        1.0f/(1<<15)*MANTISSA_MULT, 1.0f/(1<<23)*MANTISSA_MULT
    };
    
    @Implementation
    public static float complexToFloat(int complex)
    {
        return (complex&(TypedValue.COMPLEX_MANTISSA_MASK
                   <<TypedValue.COMPLEX_MANTISSA_SHIFT))
            * RADIX_MULTS[(complex>>TypedValue.COMPLEX_RADIX_SHIFT)
                            & TypedValue.COMPLEX_RADIX_MASK];
    }

    @Implementation
    public static float complexToDimension(int data, DisplayMetrics metrics)
    {
        return applyDimension(
            (data>>COMPLEX_UNIT_SHIFT)&COMPLEX_UNIT_MASK,
            complexToFloat(data),
            metrics);
    }
    
    @Implementation
    public static int complexToDimensionPixelOffset(int data,
            DisplayMetrics metrics)
    {
        return (int)applyDimension(
                (data>>COMPLEX_UNIT_SHIFT)&COMPLEX_UNIT_MASK,
                complexToFloat(data),
                metrics);
    }
    
    @Implementation
    public static int complexToDimensionPixelSize(int data,
            DisplayMetrics metrics)
    {
        final float value = complexToFloat(data);
        final float f = applyDimension(
                (data>>COMPLEX_UNIT_SHIFT)&COMPLEX_UNIT_MASK,
                value,
                metrics);
        final int res = (int)(f+0.5f);
        if (res != 0) return res;
        if (value == 0) return 0;
        if (value > 0) return 1;
        return -1;
    }
    
    @Implementation
    public static float complexToDimensionNoisy(int data, DisplayMetrics metrics)
    {
        float res = complexToDimension(data, metrics);
        System.out.println(
            "Dimension (0x" + ((data>>TypedValue.COMPLEX_MANTISSA_SHIFT)
                               & TypedValue.COMPLEX_MANTISSA_MASK)
            + "*" + (RADIX_MULTS[(data>>TypedValue.COMPLEX_RADIX_SHIFT)
                                & TypedValue.COMPLEX_RADIX_MASK] / MANTISSA_MULT)
            + ")" + DIMENSION_UNIT_STRS[(data>>COMPLEX_UNIT_SHIFT)
                                & COMPLEX_UNIT_MASK]
            + " = " + res);
        return res;
    }
    
    @Implementation
    public static float applyDimension(int unit, float value, DisplayMetrics metrics) {
        switch (unit) {
            case COMPLEX_UNIT_PX:
                return value;
            case COMPLEX_UNIT_DIP:
                return value * metrics.density;
            case COMPLEX_UNIT_SP:
                return value * metrics.scaledDensity;
            case COMPLEX_UNIT_PT:
                return value * metrics.xdpi * (1.0f / 72);
            case COMPLEX_UNIT_IN:
                return value * metrics.xdpi;
            case COMPLEX_UNIT_MM:
                return value * metrics.xdpi * (1.0f / 25.4f);
        }
        return 0;
    }
    
    @Implementation
    public float getDimension(DisplayMetrics metrics)
    {
        return complexToDimension(typedValue.data, metrics);
    }
    
    @Implementation
    public static float complexToFraction(int data, float base, float pbase)
    {
        switch ((data>>COMPLEX_UNIT_SHIFT)&COMPLEX_UNIT_MASK) {
        case COMPLEX_UNIT_FRACTION:
            return complexToFloat(data) * base;
        case COMPLEX_UNIT_FRACTION_PARENT:
            return complexToFloat(data) * pbase;
        }
        return 0;
    }
    
    @Implementation
    public float getFraction(float base, float pbase)
    {
        return complexToFraction(typedValue.data, base, pbase);
    }
    
    @Implementation
    public final CharSequence coerceToString()
    {
        int t = typedValue.type;
        if (t == TYPE_STRING) {
            return typedValue.string;
        }
        return coerceToString(t, typedValue.data);
    }
    
    private static final String[] DIMENSION_UNIT_STRS = new String[] {
        "px", "dip", "sp", "pt", "in", "mm"
    };
    private static final String[] FRACTION_UNIT_STRS = new String[] {
        "%", "%p"
    };
    
    @Implementation
    public static final String coerceToString(int type, int data)
    {
        switch (type) {
        case TYPE_NULL:
            return null;
        case TYPE_REFERENCE:
            return "@" + data;
        case TYPE_ATTRIBUTE:
            return "?" + data;
        case TYPE_FLOAT:
            return Float.toString(Float.intBitsToFloat(data));
        case TYPE_DIMENSION:
            return Float.toString(complexToFloat(data)) + DIMENSION_UNIT_STRS[
                (data>>COMPLEX_UNIT_SHIFT)&COMPLEX_UNIT_MASK];
        case TYPE_FRACTION:
            return Float.toString(complexToFloat(data)*100) + FRACTION_UNIT_STRS[
                (data>>COMPLEX_UNIT_SHIFT)&COMPLEX_UNIT_MASK];
        case TYPE_INT_HEX:
            return "0x" + Integer.toHexString(data);
        case TYPE_INT_BOOLEAN:
            return data != 0 ? "true" : "false";
        }

        if (type >= TYPE_FIRST_COLOR_INT && type <= TYPE_LAST_COLOR_INT) {
            return "#" + Integer.toHexString(data);
        } else if (type >= TYPE_FIRST_INT && type <= TYPE_LAST_INT) {
            return Integer.toString(data);
        }

        return null;
    }
    
    @Implementation
    public void setTo(TypedValue other)
    {
        typedValue.type = other.type;
        typedValue.string = other.string;
        typedValue.data = other.data;
        typedValue.assetCookie = other.assetCookie;
        typedValue.resourceId = other.resourceId;
        typedValue.density = other.density;
    }

    @Implementation
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TypedValue{t=0x").append(Integer.toHexString(typedValue.type));
        sb.append("/d=0x").append(Integer.toHexString(typedValue.data));
        if (typedValue.type == TYPE_STRING) {
            sb.append(" \"").append(typedValue.string != null ? typedValue.string : "<null>").append("\"");
        }
        if (typedValue.assetCookie != 0) {
            sb.append(" a=").append(typedValue.assetCookie);
        }
        if (typedValue.resourceId != 0) {
            sb.append(" r=0x").append(Integer.toHexString(typedValue.resourceId));
        }
        sb.append("}");
        return sb.toString();
    }
}
