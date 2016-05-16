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
package com.android.cts.javascanner;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class DocletRunner {

    private final File mSourceDir;
    private final File mDocletPath;

    DocletRunner(File sourceDir, File docletPath) {
        mSourceDir = sourceDir;
        mDocletPath = docletPath;
    }

    int runJavaDoc() throws IOException, InterruptedException {
        List<String> args = new ArrayList<String>();
        args.add("javadoc");
        args.add("-doclet");
        args.add("com.android.cts.javascannerdoclet.CtsJavaScannerDoclet");
        args.add("-docletpath");
        args.add(mDocletPath.toString());
        args.add("-sourcepath");
        args.add(getSourcePath(mSourceDir));
        args.add("-classpath");
        args.add(getClassPath());
        args.addAll(getSourceFiles(mSourceDir));

        Process process = new ProcessBuilder(args).start();
        Scanner scanner = null;
        try {
            scanner = new Scanner(process.getInputStream());
            while (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
            }
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

        return process.waitFor();
    }

    private String getSourcePath(File sourceDir) {
        List<String> sourcePath = new ArrayList<String>();
        sourcePath.add("./frameworks/base/core/java");
        sourcePath.add("./frameworks/base/test-runner/src");
        sourcePath.add("./external/junit/src");
        sourcePath.add("./development/tools/hosttestlib/src");
        sourcePath.add("./libcore/dalvik/src/main/java");
        sourcePath.add("./cts/tests/src");
        sourcePath.add("./cts/libs/commonutil/src");
        sourcePath.add("./cts/libs/deviceutil/src");
        sourcePath.add("./frameworks/testing/uiautomator/library/testrunner-src");
        sourcePath.add("./frameworks/testing/uiautomator_test_libraries/src");
        sourcePath.add(sourceDir.toString());
        return join(sourcePath, ":");
    }

    private String getClassPath() {
        List<String> classPath = new ArrayList<String>();
        classPath.add("./prebuilts/misc/common/tradefed/tradefed-prebuilt.jar");
        return join(classPath, ":");
    }

    private List<String> getSourceFiles(File sourceDir) {
        List<String> sourceFiles = new ArrayList<String>();

        File[] files = sourceDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() || pathname.toString().endsWith(".java");
            }
        });

        for (File file : files) {
            if (file.isDirectory()) {
                sourceFiles.addAll(getSourceFiles(file));
            } else {
                sourceFiles.add(file.toString());
            }
        }

        return sourceFiles;
    }

    private String join(List<String> options, String delimiter) {
        StringBuilder builder = new StringBuilder();
        int numOptions = options.size();
        for (int i = 0; i < numOptions; i++) {
            builder.append(options.get(i));
            if (i + 1 < numOptions) {
                builder.append(delimiter);
            }
        }
        return builder.toString();
    }
}
