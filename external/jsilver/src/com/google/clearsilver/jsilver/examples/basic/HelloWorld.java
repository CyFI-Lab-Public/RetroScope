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
 * Hello world of templates.
 */
public class HelloWorld {

  public static void main(String[] args) throws IOException {

    // Load resources (e.g. templates) from classpath, along side this class.
    JSilver jSilver = new JSilver(new ClassResourceLoader(HelloWorld.class));

    // Set up some data.
    Data data = jSilver.createData();
    data.setValue("name.first", "Mr");
    data.setValue("name.last", "Man");

    // Render template to System.out.
    jSilver.render("hello-world.cs", data, System.out);
  }
}
