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

package com.google.doclava;

import com.google.clearsilver.jsilver.data.Data;

import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class DocFile {
  public static final Pattern LINE = Pattern.compile("(.*)[\r]?\n", Pattern.MULTILINE);
  public static final Pattern PROP = Pattern.compile("([^=]+)=(.*)");

  public static String readFile(String filename) {
    try {
      File f = new File(filename);
      int length = (int) f.length();
      FileInputStream is = new FileInputStream(f);
      InputStreamReader reader = new InputStreamReader(is, "UTF-8");
      char[] buf = new char[length];
      int index = 0;
      int amt;
      while (true) {
        amt = reader.read(buf, index, length - index);

        if (amt < 1) {
          break;
        }

        index += amt;
      }
      return new String(buf, 0, index);
    } catch (IOException e) {
      return null;
    }
  }

  public static String[] DEVSITE_VALID_LANGS = {"en", "es","ja", "ko", "ru", "zh-cn"};

  public static String getPathRoot(String filename) {
    String[] stripStr = filename.split("\\/");
    String outFrag = stripStr[0];
    if (stripStr.length > 0) {
      for (String t : DEVSITE_VALID_LANGS) {
        if (stripStr[0].equals("intl")) {
          if (stripStr[1].equals(t)) {
            outFrag = stripStr[2];
            break;
          }
        } else if (stripStr[0].equals(t)) {
            outFrag = stripStr[1];
            break;
        }
      }
    }
    return outFrag;
  }
  
  public static void writePage(String docfile, String relative, String outfile, Data hdf) {

    /*
     * System.out.println("docfile='" + docfile + "' relative='" + relative + "'" + "' outfile='" +
     * outfile + "'");
     */
    if (hdf == null) {
      hdf = Doclava.makeHDF(); 
    } 
    String filedata = readFile(docfile);

    // The document is properties up until the line "@jd:body".
    // Any blank lines are ignored.
    int start = -1;
    int lineno = 1;
    Matcher lines = LINE.matcher(filedata);
    String line = null;
    while (lines.find()) {
      line = lines.group(1);
      if (line.length() > 0) {
        if (line.equals("@jd:body")) {
          start = lines.end();
          break;
        }
        Matcher prop = PROP.matcher(line);
        if (prop.matches()) {
          String key = prop.group(1);
          String value = prop.group(2);
          hdf.setValue(key, value);
        } else {
          break;
        }
      }
      lineno++;
    }
    if (start < 0) {
      System.err.println(docfile + ":" + lineno + ": error parsing docfile");
      if (line != null) {
        System.err.println(docfile + ":" + lineno + ":" + line);
      }
      System.exit(1);
    }

    // if they asked to only be for a certain template, maybe skip it
    String fromTemplate = hdf.getValue("template.which", "");
    String fromPage = hdf.getValue("page.onlyfortemplate", "");
    if (!"".equals(fromPage) && !fromTemplate.equals(fromPage)) {
      return;
    }

    // and the actual text after that
    String commentText = filedata.substring(start);

    Comment comment = new Comment(commentText, null, new SourcePositionInfo(docfile, lineno, 1));
    TagInfo[] tags = comment.tags();

    TagInfo.makeHDF(hdf, "root.descr", tags);

    hdf.setValue("commentText", commentText);

    // write the page using the appropriate root template, based on the
    // whichdoc value supplied by build
    String fromWhichmodule = hdf.getValue("android.whichmodule", "");
    if (fromWhichmodule.equals("online-pdk")) {
      // leaving this in just for temporary compatibility with pdk doc
      hdf.setValue("online-pdk", "true");
      // add any conditional login for root template here (such as
      // for custom left nav based on tab etc.
      ClearPage.write(hdf, "docpage.cs", outfile);
    } else {
      String filename = outfile;
      // Strip out the intl and lang id substr and get back just the 
      // guide, design, distribute, etc. 
      filename = getPathRoot(filename);
      if (filename.indexOf("design") == 0) {
        hdf.setValue("design", "true");
      } else if (filename.indexOf("develop") == 0) {
        hdf.setValue("develop", "true");
      } else if (filename.indexOf("guide") == 0) {
        hdf.setValue("guide", "true");
      } else if (filename.indexOf("training") == 0) {
        hdf.setValue("training", "true");
      } else if (filename.indexOf("more") == 0) {
        hdf.setValue("more", "true");
      } else if (filename.indexOf("google") == 0) {
        hdf.setValue("google", "true");
      } else if (filename.indexOf("samples") == 0) {
        hdf.setValue("samples", "true");
      } else if (filename.indexOf("distribute") == 0) {
        hdf.setValue("distribute", "true");
      } else if (filename.indexOf("about") == 0) {
        hdf.setValue("about", "true");
      } else if ((filename.indexOf("tools") == 0) || (filename.indexOf("sdk") == 0)) {
        hdf.setValue("tools", "true");
        fromTemplate = hdf.getValue("page.template", "");
      } else if (filename.indexOf("devices") == 0) {
        hdf.setValue("devices", "true");
      } else if (filename.indexOf("source") == 0) {
        hdf.setValue("source", "true");
      } else if (filename.indexOf("accessories") == 0) {
        hdf.setValue("accessories", "true");
      } else if (filename.indexOf("compatibility") == 0) {
        hdf.setValue("compatibility", "true");
      }
      if (fromTemplate.equals("sdk")) {
        ClearPage.write(hdf, "sdkpage.cs", outfile);
      } else {
        ClearPage.write(hdf, "docpage.cs", outfile);
      }
    }
  } // writePage
}
