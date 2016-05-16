/*
 * Copyright (C) 2010 Google Inc.
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

package com.google.clearsilver.jsilver.examples.basic;

import com.google.clearsilver.jsilver.JSilver;
import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.resourceloader.FileSystemResourceLoader;

import java.io.IOException;

/**
 * Command-line template renderer.
 * 
 * Usage: JSilverTest file.cs [file.hdf file2.hdf ...]
 */
public class JSilverTest {
  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.out.println("Usage: JSilverTest file.cs [file.hdf file2.hdf ...]");
      System.exit(1);
    }

    // Load resources from filesystem, relative to the current directory.
    JSilver jSilver = new JSilver(new FileSystemResourceLoader("."));

    // Load data.
    Data data = jSilver.createData();
    for (int i = 1; i < args.length; i++) {
      jSilver.loadData(args[i], data);
    }

    // Render template to System.out.
    jSilver.render(args[0], data, System.out);
  }
}
