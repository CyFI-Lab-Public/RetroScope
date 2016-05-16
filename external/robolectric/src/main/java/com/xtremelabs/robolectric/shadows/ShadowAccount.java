package com.xtremelabs.robolectric.shadows;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.text.TextUtils;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.internal.RealObject;

import java.lang.reflect.Field;

@Implements(Account.class)
public class ShadowAccount {
    @RealObject
    private Account realObject;

    public void __constructor__(String name, String type) throws Exception {
        set(name, type);
    }

    public void __constructor__(Parcel parcel) throws Exception {
        set(parcel.readString(), parcel.readString());
    }

    @Override
    @Implementation
    public String toString() {
        return "Account {name=" + realObject.name + ", type=" + realObject.type + "}";
    }

    @Implementation
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(realObject.name);
        dest.writeString(realObject.type);
    }

    public static final Creator<Account> CREATOR =
        new Creator<Account>() {
            @Override
            public Account createFromParcel(Parcel source) {
                return new Account(source);
            }

            @Override
            public Account[] newArray(int size) {
                return new Account[size];
            }
        };

    private void set(String name, String type) throws Exception {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(type)) throw new IllegalArgumentException();

        Field nameF = realObject.getClass().getField("name");
        nameF.setAccessible(true);
        nameF.set(realObject, name);

        Field typeF = realObject.getClass().getField("type");
        typeF.setAccessible(true);
        typeF.set(realObject, type);
    }

    @Override
    @Implementation
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Account)) return false;
        final Account other = (Account)o;
        return realObject.name.equals(other.name) && realObject.type.equals(other.type);
    }

    @Override
    @Implementation
    public int hashCode() {
        int result = 17;
        result = 31 * result + realObject.name.hashCode();
        result = 31 * result + realObject.type.hashCode();
        return result;
    }

    public static void reset() {
        Robolectric.Reflection.setFinalStaticField(Account.class, "CREATOR", CREATOR);
    }
}
