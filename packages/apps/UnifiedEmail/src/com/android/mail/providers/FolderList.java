package com.android.mail.providers;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Simple class to encapsulate an immutable list of {@link Folder} objects, and handle serialization
 * and de-serialization.
 */
public class FolderList implements Parcelable {

    private static final FolderList EMPTY = new FolderList(Collections.<Folder> emptyList());

    public final ImmutableList<Folder> folders;

    // Private to reinforce the copyOf() API, which makes it more clear that creating a FolderList
    // has copy overhead.
    private FolderList(Collection<Folder> in) {
        if (in == null) {
            folders = ImmutableList.of();
        } else {
            folders = ImmutableList.copyOf(in);
        }
    }

    public FolderList(Parcel in) {
        folders = ImmutableList.copyOf(in.createTypedArrayList(Folder.CREATOR));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(folders);
    }

    public byte[] toBlob() {
        final Parcel p = Parcel.obtain();
        writeToParcel(p, 0);
        final byte[] result = p.marshall();
        p.recycle();
        return result;
    }

    /**
     * Directly turns a list of {@link Folder}s into a byte-array. Avoids the
     * list-copy overhead of {@link #copyOf(Collection)} + {@link #toBlob()}.
     *
     * @param in a list of Folders
     * @return the marshalled byte-array form of a {@link FolderList}
     */
    public static byte[] listToBlob(List<Folder> in) {
        final Parcel p = Parcel.obtain();
        p.writeTypedList(in);
        final byte[] result = p.marshall();
        p.recycle();
        return result;
    }

    public static FolderList fromBlob(byte[] blob) {
        if (blob == null) {
            return EMPTY;
        }

        final Parcel p = Parcel.obtain();
        p.unmarshall(blob, 0, blob.length);
        p.setDataPosition(0);
        final FolderList result = CREATOR.createFromParcel(p);
        p.recycle();
        return result;
    }

    public static FolderList copyOf(Collection<Folder> in) {
        return new FolderList(in);
    }

    @Override
    public boolean equals(Object o) {
        return folders.equals(o);
    }

    @Override
    public int hashCode() {
        return folders.hashCode();
    }

    public static final Creator<FolderList> CREATOR = new Creator<FolderList>() {

        @Override
        public FolderList createFromParcel(Parcel source) {
            return new FolderList(source);
        }

        @Override
        public FolderList[] newArray(int size) {
            return new FolderList[size];
        }
    };

}
