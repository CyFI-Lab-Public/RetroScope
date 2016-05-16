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
import java.util.Arrays;

/**
 * Class that searches a source directory for native gTests and outputs a
 * list of test classes and methods.
 */
public class CtsJavaScanner {

    private static void usage(String[] args) {
        System.err.println("Arguments: " + Arrays.asList(args));
        System.err.println("Usage: cts-java-scanner -s SOURCE_DIR -d DOCLET_PATH");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        File sourceDir = null;
        File docletPath = null;

        for (int i = 0; i < args.length; i++) {
            if ("-s".equals(args[i])) {
                sourceDir = new File(getArg(args, ++i, "Missing value for source directory"));
            } else if ("-d".equals(args[i])) {
                docletPath = new File(getArg(args, ++i, "Missing value for docletPath"));
            } else {
                System.err.println("Unsupported flag: " + args[i]);
                usage(args);
            }
        }

        if (sourceDir == null) {
            System.err.println("Source directory is required");
            usage(args);
        }

        if (docletPath == null) {
            System.err.println("Doclet path is required");
            usage(args);
        }

        DocletRunner runner = new DocletRunner(sourceDir, docletPath);
        System.exit(runner.runJavaDoc());
    }

    private static String getArg(String[] args, int index, String message) {
        if (index < args.length) {
            return args[index];
        } else {
            System.err.println(message);
            usage(args);
            return null;
        }
    }
}
