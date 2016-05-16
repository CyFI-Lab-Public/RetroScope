/*
 * Copyright 2010 Google Inc.
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
package com.google.android.testing.mocking;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic.Kind;

/**
 * @author swoodward@google.com (Stephen Woodward)
 * 
 */
class ProcessorLogger {
  private final OutputStream logFile;
  private final ProcessingEnvironment processingEnv;

  ProcessorLogger(OutputStream logFile, ProcessingEnvironment processingEnv) {
    this.logFile = logFile;
    this.processingEnv = processingEnv;
  }

  ProcessorLogger(String logFileName, ProcessingEnvironment processingEnv) {
    this.logFile = openLogFile(logFileName);
    this.processingEnv = processingEnv;
  }

  void reportClasspathError(String clazz, Throwable e) {
    printMessage(Kind.ERROR, "Could not find " + clazz);
    printMessage(Kind.ERROR, e);
    printMessage(Kind.NOTE, "Known Classpath: ");
    URL[] allUrls = ((URLClassLoader) this.getClass().getClassLoader()).getURLs();
    for (URL url : allUrls) {
      printMessage(Kind.NOTE, url.toString());
    }
  }

  void printMessage(Kind kind, String message) {
    processingEnv.getMessager().printMessage(kind, message);
    if (logFile != null) {
      try {
        logFile.write((SimpleDateFormat.getDateTimeInstance().format(new Date()) + " - "
            + kind.toString() + " : " + message + "\n").getBytes());
      } catch (IOException e) {
        // That's unfortunate, but not much to do about it.
        processingEnv.getMessager().printMessage(Kind.WARNING,
            "IOException logging to file" + e.toString());
      }
    }
  }

  void printMessage(Kind kind, Throwable e) {
    ByteArrayOutputStream stackTraceByteStream = new ByteArrayOutputStream();
    PrintStream stackTraceStream = new PrintStream(stackTraceByteStream);
    e.printStackTrace(stackTraceStream);
    printMessage(kind, stackTraceByteStream.toString());
  }

  FileOutputStream openLogFile(String logFileName) {
    try {
      if (logFileName != null) {
        File log = new File(logFileName);
        if (!log.exists() && log.getParentFile() != null) {
          log.getParentFile().mkdirs();
        }
        return new FileOutputStream(log, true);
      }
    } catch (FileNotFoundException e) {
      printMessage(Kind.WARNING, e);
    }
    return null;
  }

  void close() {
    if (logFile != null) {
      try {
        logFile.close();
      } catch (IOException e) {
        // That's ok
      }
    }
  }
}
