package com.xtremelabs.robolectric.shadows;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

import android.content.pm.Signature;
import android.os.Parcel;
import android.os.Parcelable.Creator;

import java.util.Arrays;

@Implements(Signature.class)
public class ShadowSignature {

    private byte[] mSignature;

    public void __constructor__(byte[] signature) {
        mSignature = signature.clone();
    }

    @Implementation
    public byte[] toByteArray() {
        byte[] bytes = new byte[mSignature.length];
        System.arraycopy(mSignature, 0, bytes, 0, mSignature.length);
        return bytes;
    }

    @Override
    @Implementation
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Signature)) {
            return false;
        }
        ShadowSignature otherShadow = shadowOf((Signature) obj);
        return Arrays.equals(mSignature, otherShadow.mSignature);
    }

    @Override
    @Implementation
    public int hashCode() {
        return Arrays.hashCode(mSignature);
    }

    @Implementation
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(mSignature);
    }

    public static final Creator<Signature> CREATOR =
        new Creator<Signature>() {
            @Override
            public Signature createFromParcel(Parcel source) {
                byte[] signature = source.createByteArray();
                return new Signature(signature);
            }

            @Override
            public Signature[] newArray(int size) {
                return new Signature[size];
            }
        };

    public static void reset() {
        Robolectric.Reflection.setFinalStaticField(Signature.class, "CREATOR", CREATOR);
    }
}
