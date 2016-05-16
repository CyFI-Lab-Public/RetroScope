/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.doclava;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class Doclava2 {
    private static final boolean DEBUG_MODE = false;
    public static void main(String args[]) throws Exception {
        if (DEBUG_MODE) {
            ArrayList<String> files = new ArrayList<String>();
            files.add("frameworks/base/core/java/android/preference/VolumePreference.java");
            files.add("frameworks/base/core/java/android/preference/SeekBarDialogPreference.java");
            files.add("frameworks/base/core/java/android/view/ViewGroup.java");
            files.add("frameworks/base/core/java/android/widget/FrameLayout.java");
            files.add("frameworks/base/core/java/android/widget/DatePicker.java");
            files.add("frameworks/base/core/java/android/widget/GridLayout.java");

            for (String filename : files) {
                InfoBuilder infoBuilder = new InfoBuilder(filename);
                System.out.println(filename);
                infoBuilder.parseFile();
            }

            ClassInfo cl = InfoBuilder.Caches.getClass("android.preference.VolumePreference");
            if (cl != null) {
                InfoBuilder.printClassInfo(cl);

                InfoBuilder.resolve();
                cl.printResolutions();
            } else {
                System.out.println("You're looking for a class that does not exist.");
            }
        } else {
            BufferedReader buf = new BufferedReader(new FileReader(args[0]));

            String line = buf.readLine();

            ArrayList<String> files = new ArrayList<String>();
            while (line != null) {
                String[] names = line.split(" ");

                for (String name : names) {
                    files.add(name);
                }

                line = buf.readLine();
            }

            for (String filename : files) {
                InfoBuilder infoBuilder = new InfoBuilder(filename);
                System.out.println(filename);
                infoBuilder.parseFile();
            }

            InfoBuilder.resolve();


            System.out.println("\n\n\n\n\n\n\n");
            System.out.println("************************************************");

            InfoBuilder.Caches.printResolutions();

            System.out.println("************************************************");

            InfoBuilder.resolve();
            InfoBuilder.Caches.printResolutions();
        }
    }
}