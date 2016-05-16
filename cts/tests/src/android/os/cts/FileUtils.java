/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.os.cts;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Bits and pieces copied from hidden API of android.os.FileUtils. */
public class FileUtils {

    public static final int S_IFMT  = 0170000;
    public static final int S_IFSOCK = 0140000;
    public static final int S_IFLNK = 0120000;
    public static final int S_IFREG = 0100000;
    public static final int S_IFBLK = 0060000;
    public static final int S_IFDIR = 0040000;
    public static final int S_IFCHR = 0020000;
    public static final int S_IFIFO = 0010000;

    public static final int S_ISUID = 0004000;
    public static final int S_ISGID = 0002000;
    public static final int S_ISVTX = 0001000;

    public static final int S_IRWXU = 00700;
    public static final int S_IRUSR = 00400;
    public static final int S_IWUSR = 00200;
    public static final int S_IXUSR = 00100;

    public static final int S_IRWXG = 00070;
    public static final int S_IRGRP = 00040;
    public static final int S_IWGRP = 00020;
    public static final int S_IXGRP = 00010;

    public static final int S_IRWXO = 00007;
    public static final int S_IROTH = 00004;
    public static final int S_IWOTH = 00002;
    public static final int S_IXOTH = 00001;

    static {
        System.loadLibrary("cts_jni");
    }

    public static class FileStatus {

        public int dev;
        public int ino;
        public int mode;
        public int nlink;
        public int uid;
        public int gid;
        public int rdev;
        public long size;
        public int blksize;
        public long blocks;
        public long atime;
        public long mtime;
        public long ctime;

        public boolean hasModeFlag(int flag) {
            if (((S_IRWXU | S_IRWXG | S_IRWXO) & flag) != flag) {
                throw new IllegalArgumentException("Inappropriate flag " + flag);
            }
            return (mode & flag) == flag;
        }

        public boolean isOfType(int type) {
            if ((type & S_IFMT) != type) {
                throw new IllegalArgumentException("Unknown type " + type);
            }
            return (mode & S_IFMT) == type;
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

    public native static int setPermissions(String file, int mode);

    /**
     * Copy data from a source stream to destFile.
     * Return true if succeed, return false if failed.
     */
    public static boolean copyToFile(InputStream inputStream, File destFile) {
        try {
            if (destFile.exists()) {
                destFile.delete();
            }
            FileOutputStream out = new FileOutputStream(destFile);
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.flush();
                try {
                    out.getFD().sync();
                } catch (IOException e) {
                }
                out.close();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void createFile(File file, int numBytes) throws IOException {
        File parentFile = file.getParentFile();
        if (parentFile != null) {
            parentFile.mkdirs();
        }
        byte[] buffer = new byte[numBytes];
        FileOutputStream output = new FileOutputStream(file);
        try {
            output.write(buffer);
        } finally {
            output.close();
        }
    }

    public static byte[] readInputStreamFully(InputStream is) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[32768];
        int count;
        try {
            while ((count = is.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return os.toByteArray();
    }
}
