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

package com.android.cts.apicoverage;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class that outputs a text report of {@link ApiCoverage}.
 */
class TextReport {

    public static void printTextReport(ApiCoverage api, String packageFilter, OutputStream outputStream) {
        PrintStream out = new PrintStream(outputStream);

        CoverageComparator comparator = new CoverageComparator();
        List<ApiPackage> packages = new ArrayList<ApiPackage>(api.getPackages());
        Collections.sort(packages, comparator);

        for (ApiPackage apiPackage : packages) {
            if (apiPackage.getName().startsWith(packageFilter)
                    && apiPackage.getTotalMethods() > 0) {
                printPackage(apiPackage, out);
            }
        }

        out.println();
        out.println();

        for (ApiPackage apiPackage : packages) {
            if (apiPackage.getName().startsWith(packageFilter)) {
                printPackage(apiPackage, out);

                List<ApiClass> classes = new ArrayList<ApiClass>(apiPackage.getClasses());
                Collections.sort(classes, comparator);
                for (ApiClass apiClass : classes) {
                    if (apiClass.getTotalMethods() > 0) {
                        printClass(apiClass, out);

                        List<ApiConstructor> constructors =
                                new ArrayList<ApiConstructor>(apiClass.getConstructors());
                        Collections.sort(constructors);
                        for (ApiConstructor constructor : constructors) {
                            printConstructor(constructor, out);
                        }

                        List<ApiMethod> methods = new ArrayList<ApiMethod>(apiClass.getMethods());
                        Collections.sort(methods);
                        for (ApiMethod method : methods) {
                            printMethod(method, out);
                        }
                    }
                }
            }
        }
    }

    private static void printPackage(ApiPackage apiPackage, PrintStream out) {
        out.println(apiPackage.getName() + " "
                + Math.round(apiPackage.getCoveragePercentage()) + "% ("
                + apiPackage.getNumCoveredMethods() + "/" + apiPackage.getTotalMethods() + ")");
    }

    private static void printClass(ApiClass apiClass, PrintStream out) {
        out.println("  " + apiClass.getName() + " "
                + Math.round(apiClass.getCoveragePercentage()) + "% ("
                + apiClass.getNumCoveredMethods() + "/" + apiClass.getTotalMethods() + ") ");
    }

    private static void printConstructor(ApiConstructor constructor, PrintStream out) {
        StringBuilder builder = new StringBuilder("    [")
                .append(constructor.isCovered() ? "X" : " ")
                .append("] ").append(constructor.getName()).append("(");

        List<String> parameterTypes = constructor.getParameterTypes();
        int numParameterTypes = parameterTypes.size();
        for (int i = 0; i < numParameterTypes; i++) {
            builder.append(parameterTypes.get(i));
            if (i + 1 < numParameterTypes) {
                builder.append(", ");
            }
        }
        out.println(builder.append(")"));
    }

    private static void printMethod(ApiMethod method, PrintStream out) {
        StringBuilder builder = new StringBuilder("    [")
                .append(method.isCovered() ? "X" : " ")
                .append("] ").append(method.getReturnType()).append(" ")
                .append(method.getName()).append("(");
        List<String> parameterTypes = method.getParameterTypes();
        int numParameterTypes = parameterTypes.size();
        for (int i = 0; i < numParameterTypes; i++) {
            builder.append(parameterTypes.get(i));
            if (i + 1 < numParameterTypes) {
                builder.append(", ");
            }
        }
        out.println(builder.append(")"));
    }
}
