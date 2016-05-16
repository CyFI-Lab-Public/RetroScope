/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.providers.contacts.debug;

import com.android.providers.contacts.util.Hex;
import com.google.common.io.Closeables;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Compress all files under the app data dir into a single zip file.
 *
 * Make sure not to output dump filenames anywhere, including logcat.
 */
public class DataExporter {
    private static String TAG = "DataExporter";

    public static final String ZIP_MIME_TYPE = "application/zip";

    public static final String DUMP_FILE_DIRECTORY_NAME = "dumpedfiles";

    public static final String OUT_FILE_SUFFIX = "-contacts-db.zip";
    public static final String VALID_FILE_NAME_REGEX = "[0-9A-Fa-f]+-contacts-db\\.zip";

    /**
     * Compress all files under the app data dir into a single zip file, and return the content://
     * URI to the file, which can be read via {@link DumpFileProvider}.
     */
    public static Uri exportData(Context context) throws IOException {
        final String fileName = generateRandomName() + OUT_FILE_SUFFIX;
        final File outFile = getOutputFile(context, fileName);

        // Remove all existing ones.
        removeDumpFiles(context);

        Log.i(TAG, "Dump started...");

        ensureOutputDirectory(context);
        final ZipOutputStream os = new ZipOutputStream(new FileOutputStream(outFile));
        os.setLevel(Deflater.BEST_COMPRESSION);
        try {
            addDirectory(context, os, context.getFilesDir().getParentFile(), "contacts-files");
        } finally {
            Closeables.closeQuietly(os);
        }
        Log.i(TAG, "Dump finished.");
        return DumpFileProvider.AUTHORITY_URI.buildUpon().appendPath(fileName).build();
    }

    /** @return long random string for a file name */
    private static String generateRandomName() {
        final SecureRandom rng = new SecureRandom();
        final byte[] random = new byte[256 / 8];
        rng.nextBytes(random);

        return Hex.encodeHex(random, true);
    }

    public static void ensureValidFileName(String fileName) {
        // Do not allow queries to use relative paths to leave the root directory. Otherwise they
        // can gain access to other files such as the contacts database.
        if (fileName.contains("..")) {
            throw new IllegalArgumentException(".. path specifier not allowed. Bad file name: " +
                    fileName);
        }
        // White list dump files.
        if (!fileName.matches(VALID_FILE_NAME_REGEX)) {
            throw new IllegalArgumentException("Only " + VALID_FILE_NAME_REGEX +
                    " files are supported. Bad file name: " + fileName);
        }
    }

    private static File getOutputDirectory(Context context) {
        return new File(context.getCacheDir(), DUMP_FILE_DIRECTORY_NAME);
    }

    private static void ensureOutputDirectory(Context context) {
        final File directory = getOutputDirectory(context);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    public static File getOutputFile(Context context, String fileName) {
        return new File(getOutputDirectory(context), fileName);
    }

    public static boolean dumpFileExists(Context context) {
        return getOutputDirectory(context).exists();
    }

    public static void removeDumpFiles(Context context) {
        removeFileOrDirectory(getOutputDirectory(context));
    }

    private static void removeFileOrDirectory(File file) {
        if (!file.exists()) return;

        if (file.isFile()) {
            Log.i(TAG, "Removing " + file);
            file.delete();
            return;
        }

        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                removeFileOrDirectory(child);
            }
            Log.i(TAG, "Removing " + file);
            file.delete();
        }
    }

    /**
     * Add all files under {@code current} to {@code os} zip stream
     */
    private static void addDirectory(Context context, ZipOutputStream os, File current,
            String storedPath) throws IOException {
        for (File child : current.listFiles()) {
            final String childStoredPath = storedPath + "/" + child.getName();

            if (child.isDirectory()) {
                // Don't need the cache directory, which also contains the dump files.
                if (child.equals(context.getCacheDir())) {
                    continue;
                }
                // This check is redundant as the output directory should be in the cache dir,
                // but just in case...
                if (child.getName().equals(DUMP_FILE_DIRECTORY_NAME)) {
                    continue;
                }
                addDirectory(context, os, child, childStoredPath);
            } else if (child.isFile()) {
                addFile(os, child, childStoredPath);
            } else {
                // Shouldn't happen; skip.
            }
        }
    }

    /**
     * Add a single file {@code current} to {@code os} zip stream using the file name
     * {@code storedPath}.
     */
    private static void addFile(ZipOutputStream os, File current, String storedPath)
            throws IOException {
        Log.i(TAG, "Adding " + current.getAbsolutePath() + " ...");
        final InputStream is = new FileInputStream(current);
        os.putNextEntry(new ZipEntry(storedPath));

        final byte[] buf = new byte[32 * 1024];
        int totalLen = 0;
        while (true) {
            int len = is.read(buf);
            if (len <= 0) {
                break;
            }
            os.write(buf, 0, len);
            totalLen += len;
        }
        os.closeEntry();
        Log.i(TAG, "Added " + current.getAbsolutePath() + " as " + storedPath +
                " (" + totalLen + " bytes)");
    }
}
