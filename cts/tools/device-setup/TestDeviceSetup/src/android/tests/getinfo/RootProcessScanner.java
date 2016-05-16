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

package android.tests.getinfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/** Crawls /proc to find processes that are running as root. */
class RootProcessScanner {

    /** Processes that are allowed to run as root. */
    private static final Pattern ROOT_PROCESS_WHITELIST_PATTERN = getRootProcessWhitelistPattern(
            "debuggerd",
            "init",
            "installd",
            "netd",
            "servicemanager",
            "ueventd",
            "vold",
            "zygote"
    );

    /** Combine the individual patterns into one super pattern. */
    private static Pattern getRootProcessWhitelistPattern(String... patterns) {
        StringBuilder rootProcessPattern = new StringBuilder();
        for (int i = 0; i < patterns.length; i++) {
            rootProcessPattern.append(patterns[i]);
            if (i + 1 < patterns.length) {
                rootProcessPattern.append('|');
            }
        }
        return Pattern.compile(rootProcessPattern.toString());
    }

    /** Test that there are no unapproved root processes running on the system. */
    public static String[] getRootProcesses()
            throws FileNotFoundException, MalformedStatMException {
        List<File> rootProcessDirs = getRootProcessDirs();
        String[] rootProcessNames = new String[rootProcessDirs.size()];
        for (int i = 0; i < rootProcessNames.length; i++) {
            rootProcessNames[i] = getProcessName(rootProcessDirs.get(i));
        }
        return rootProcessNames;
    }

    private static List<File> getRootProcessDirs()
            throws FileNotFoundException, MalformedStatMException {
        File proc = new File("/proc");
        if (!proc.exists()) {
            throw new FileNotFoundException(proc + " is missing (man 5 proc)");
        }

        List<File> rootProcesses = new ArrayList<File>();
        File[] processDirs = proc.listFiles();
        if (processDirs != null && processDirs.length > 0) {
            for (File processDir : processDirs) {
                if (isUnapprovedRootProcess(processDir)) {
                    rootProcesses.add(processDir);
                }
            }
        }
        return rootProcesses;
    }

    /**
     * Filters out processes in /proc that are not approved.
     * @throws FileNotFoundException
     * @throws MalformedStatMException
     */
    private static boolean isUnapprovedRootProcess(File pathname)
            throws FileNotFoundException, MalformedStatMException {
        return isPidDirectory(pathname)
                && !isKernelProcess(pathname)
                && isRootProcess(pathname);
    }

    private static boolean isPidDirectory(File pathname) {
        return pathname.isDirectory() && Pattern.matches("\\d+", pathname.getName());
    }

    private static boolean isKernelProcess(File processDir)
            throws FileNotFoundException, MalformedStatMException {
        File statm = getProcessStatM(processDir);
        Scanner scanner = null;
        try {
            scanner = new Scanner(statm);

            boolean allZero = true;
            for (int i = 0; i < 7; i++) {
                if (scanner.nextInt() != 0) {
                    allZero = false;
                }
            }

            if (scanner.hasNext()) {
                throw new MalformedStatMException(processDir
                        + " statm expected to have 7 integers (man 5 proc)");
            }

            return allZero;
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private static File getProcessStatM(File processDir) {
        return new File(processDir, "statm");
    }

    public static class MalformedStatMException extends Exception {
        MalformedStatMException(String detailMessage) {
            super(detailMessage);
        }
    }

    /**
     * Return whether or not this process is running as root without being approved.
     *
     * @param processDir with the status file
     * @return whether or not it is a unwhitelisted root process
     * @throws FileNotFoundException
     */
    private static boolean isRootProcess(File processDir) throws FileNotFoundException {
        File status = getProcessStatus(processDir);
        Scanner scanner = null;
        try {
            scanner = new Scanner(status);

            scanner = findToken(scanner, "Name:");
            String name = scanner.next();

            scanner = findToken(scanner, "Uid:");
            boolean rootUid = hasRootId(scanner);

            scanner = findToken(scanner, "Gid:");
            boolean rootGid = hasRootId(scanner);

            return !ROOT_PROCESS_WHITELIST_PATTERN.matcher(name).matches()
                    && (rootUid || rootGid);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    /**
     * Get the status {@link File} that has name:value pairs.
     * <pre>
     * Name:   init
     * ...
     * Uid:    0       0       0       0
     * Gid:    0       0       0       0
     * </pre>
     */
    private static File getProcessStatus(File processDir) {
        return new File(processDir, "status");
    }

    /**
     * Convenience method to move the scanner's position to the point after the given token.
     *
     * @param scanner to call next() until the token is found
     * @param token to find like "Name:"
     * @return scanner after finding token
     */
    private static Scanner findToken(Scanner scanner, String token) {
        while (true) {
            String next = scanner.next();
            if (next.equals(token)) {
                return scanner;
            }
        }

        // Scanner will exhaust input and throw an exception before getting here.
    }

    /**
     * Uid and Gid lines have four values: "Uid:    0       0       0       0"
     *
     * @param scanner that has just processed the "Uid:" or "Gid:" token
     * @return whether or not any of the ids are root
     */
    private static boolean hasRootId(Scanner scanner) {
        int realUid = scanner.nextInt();
        int effectiveUid = scanner.nextInt();
        int savedSetUid = scanner.nextInt();
        int fileSystemUid = scanner.nextInt();
        return realUid == 0 || effectiveUid == 0 || savedSetUid == 0 || fileSystemUid == 0;
    }

    /** Returns the name of the process corresponding to its process directory in /proc. */
    private static String getProcessName(File processDir) throws FileNotFoundException {
        File status = getProcessStatus(processDir);
        Scanner scanner = new Scanner(status);
        try {
            scanner = findToken(scanner, "Name:");
            return scanner.next();
        } finally {
            scanner.close();
        }
    }
}
