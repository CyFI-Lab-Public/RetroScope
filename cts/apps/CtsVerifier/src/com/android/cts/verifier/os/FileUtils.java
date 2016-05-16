/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.cts.verifier.os;

/** Bits and pieces copied from hidden API of android.os.FileUtils. */
public class FileUtils {

    private static final int S_IFSOCK = 0140000;
    private static final int S_IFLNK = 0120000;
    private static final int S_IFREG = 0100000;
    private static final int S_IFBLK = 0060000;
    private static final int S_IFDIR = 0040000;
    private static final int S_IFCHR = 0020000;
    private static final int S_IFIFO = 0010000;

    private static final int S_ISUID = 0004000;
    private static final int S_ISGID = 0002000;
    private static final int S_ISVTX = 0001000;

    private static final int S_IRUSR = 00400;
    private static final int S_IWUSR = 00200;
    private static final int S_IXUSR = 00100;

    private static final int S_IRGRP = 00040;
    private static final int S_IWGRP = 00020;
    private static final int S_IXGRP = 00010;

    private static final int S_IROTH = 00004;
    private static final int S_IWOTH = 00002;
    private static final int S_IXOTH = 00001;

    static {
        System.loadLibrary("ctsverifier_jni");
    }

    public static class FileStatus {

        private int dev;
        private int ino;
        private int mode;
        private int nlink;
        private int uid;
        private int gid;
        private int rdev;
        private long size;
        private int blksize;
        private long blocks;
        private long atime;
        private long mtime;
        private long ctime;
        private boolean executable;

        public int getUid() {
            return uid;
        }

        public int getGid() {
            return gid;
        }

        public int getMode() {
            return mode;
        }

        public boolean isDirectory() {
            return hasModeFlag(mode, S_IFDIR);
        }

        public boolean isSymbolicLink() {
            return hasModeFlag(mode, S_IFLNK);
        }

        public boolean isSetUid() {
            return hasModeFlag(mode, S_ISUID);
        }

        public boolean isSetGid() {
            return hasModeFlag(mode, S_ISGID);
        }

        public boolean isExecutableByCTS() {
            return executable;
        }
    }

    /**
     * @param path of the file to stat
     * @param status object to set the fields on
     * @param statLinks or don't stat links (lstat vs stat)
     * @return whether or not we were able to stat the file
     */
    public native static boolean getFileStatus(String path, FileStatus status, boolean statLinks);

    public native static String getUserName(int uid);

    public native static String getGroupName(int gid);

    /** Display the file's mode like "ls -l" does. */
    public static String getFormattedPermissions(int mode) {
        StringBuilder permissions = new StringBuilder("-rwxrwxrwx");

        int[] typeMasks = {S_IFSOCK, S_IFLNK, S_IFREG, S_IFBLK, S_IFDIR, S_IFCHR, S_IFIFO};
        char[] typeSymbols = {'s', 'l', '-', 'b', 'd', 'c', 'p'};
        for (int i = 0; i < typeMasks.length; i++) {
            if (hasModeFlag(mode, typeMasks[i])) {
                permissions.setCharAt(0, typeSymbols[i]);
                break;
            }
        }

        int[] masks = {S_IRUSR, S_IWUSR, S_IXUSR, S_IRGRP, S_IWGRP, S_IXGRP,
                S_IROTH, S_IWOTH, S_IXOTH};
        for (int i = 0; i < masks.length; i++) {
            if (!hasModeFlag(mode, masks[i])) {
                permissions.setCharAt(1 + i, '-');
            }
        }


        if (hasModeFlag(mode, S_ISUID)) {
            permissions.setCharAt(3, hasModeFlag(mode, S_IXUSR) ? 's' : 'S');
        }

        if (hasModeFlag(mode, S_ISGID)) {
            permissions.setCharAt(6, hasModeFlag(mode, S_IXGRP) ? 's' : 'S');
        }

        if (hasModeFlag(mode, S_ISVTX)) {
            permissions.setCharAt(9, hasModeFlag(mode, S_IXOTH) ? 't' : 'T');
        }

        return permissions.toString();
    }

    private static boolean hasModeFlag(int mode, int flag) {
        return (mode & flag) == flag;
    }
}
