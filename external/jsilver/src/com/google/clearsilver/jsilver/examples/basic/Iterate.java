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
import com.google.clearsilver.jsilver.resourceloader.ClassResourceLoader;

import java.io.IOException;

/**
 * A template that iterates over some items.
 */
public class Iterate {

  public static void main(String[] args) throws IOException {

    // Load resources (e.g. templates) from classpath, along side this class.
    JSilver jSilver = new JSilver(new ClassResourceLoader(Iterate.class));

    // Set up some data.
    Data data = jSilver.createData();
    data.setValue("query", "Fruit");
    data.setValue("results.0.title", "Banana");
    data.setValue("results.0.url", "http://banana.com/");
    data.setValue("results.1.title", "Apple");
    data.setValue("results.1.url", "http://apple.com/");
    data.setValue("results.2.title", "Lemon");
    data.setValue("results.2.url", "http://lemon.com/");

    // Render template to System.out.
    jSilver.render("iterate.cs", data, System.out);
  }
}
