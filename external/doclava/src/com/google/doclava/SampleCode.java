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

import java.util.*;
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SampleCode {
  String mSource;
  String mDest;
  String mTitle;
  String mProjectDir;
  String mTags;

  public SampleCode(String source, String dest, String title) {
    mSource = source;
    mTitle = title;
    mTags = null;

    if (dest != null) {
      int len = dest.length();
      if (len > 1 && dest.charAt(len - 1) != '/') {
        mDest = dest + '/';
      } else {
        mDest = dest;
      }
    }
    //System.out.println("SampleCode init: source: " + mSource);
    //System.out.println("SampleCode init: dest: " + mDest);
    //System.out.println("SampleCode init: title: " + mTitle);

  }

  public Node write(boolean offlineMode) {
    List<Node> filelist = new ArrayList<Node>();
    File f = new File(mSource);
    mProjectDir = f.getName();
    String name = mProjectDir;
    String startname = name;
    String subdir = mDest;
    String mOut = subdir + name;
    if (!f.isDirectory()) {
      System.out.println("-samplecode not a directory: " + mSource);
      return null;
    }

    if (offlineMode)
      writeIndexOnly(f, mDest, offlineMode);
    else {
      Data hdf = Doclava.makeHDF();
      hdf.setValue("samples", "true");
      hdf.setValue("projectDir", mProjectDir);
      writeProjectDirectory(filelist, f, mDest, false, hdf, "Files.");
      writeProjectStructure(name, hdf);
      hdf.removeTree("parentdirs");
      hdf.setValue("parentdirs.0.Name", name);
      //Write root _index.jd to out and add metadata to Node.
      Node rootNode = writeProjectIndexCs(hdf, f, null, new Node(mProjectDir,
          "samples/" + startname + "/index.html", null, null, filelist, null));
      // return a root SC node for the sample with children appended
      return rootNode;
    }
    return null;
  }

  public static String convertExtension(String s, String ext) {
    return s.substring(0, s.lastIndexOf('.')) + ext;
  }

  public static String[] IMAGES = {".png", ".jpg", ".gif"};
  public static String[] TEMPLATED = {".java", ".xml", ".aidl", ".rs",".txt", ".TXT"};

  public static boolean inList(String s, String[] list) {
    for (String t : list) {
      if (s.endsWith(t)) {
        return true;
      }
    }
    return false;
  }

  public static String mapTypes(String name) {
    String type = name.substring(name.lastIndexOf('.') + 1, name.length());
    if (type.equals("xml") || type.equals("java")) {
      if (name.equals("AndroidManifest.xml")) type = "manifest";
      return type;
    } else {
      return type = "file";
    }
  }

  public void writeProjectDirectory(List<Node> parent, File dir, String relative, Boolean recursed, Data hdf, String newkey) {
    TreeSet<String> dirs = new TreeSet<String>(); //dirs for project structure and breadcrumb
    TreeSet<String> files = new TreeSet<String>(); //files for project structure and breadcrumb

    String subdir = relative;
    String name = "";
    String label = "";
    String link = "";
    String type = "";
    int i = 0;
    String expansion = ".Sub.";
    String key = newkey;

    if (recursed) {
      key = (key + expansion);
    } else {
      expansion = "";
    }

    File[] dirContents = dir.listFiles();
    Arrays.sort(dirContents, byTypeAndName);
    for (File f: dirContents) {
      name = f.getName();
      // don't process certain types of files
      if (name.startsWith(".") ||
          name.startsWith("_") ||
          name.equals("default.properties") ||
          name.equals("build.properties") ||
          name.endsWith(".ttf") ||
          name.endsWith(".gradle") ||
          name.endsWith(".bat") ||
          name.equals("Android.mk")) {
         //System.out.println("Invalid File Type, bypassing: " + name);
         continue;
       }
       if (f.isFile() && name.contains(".")){
         String path = relative + name;
         type = mapTypes(name);
         link = convertExtension(path, ".html");
         hdf.setValue("samples", "true");//dd needed?
         if (inList(path, IMAGES)) {
           // copy these files to output directly
           type = "img";
           ClearPage.copyFile(false, f, path);
           writeImagePage(f, convertExtension(path, Doclava.htmlExtension), relative);
           files.add(name);
           hdf.setValue(key + i + ".Type", "img");
           hdf.setValue(key + i + ".Name", name);
           hdf.setValue(key + i + ".Href", link);
           hdf.setValue(key + i + ".RelPath", relative);
         }
         if (inList(path, TEMPLATED)) {
           // copied and goes through the template
           ClearPage.copyFile(false, f, path);
           writePage(f, convertExtension(path, Doclava.htmlExtension), relative);
           files.add(name);
           hdf.setValue(key + i + ".Type", type);
           hdf.setValue(key + i + ".Name", name);
           hdf.setValue(key + i + ".Href", link);
           hdf.setValue(key + i + ".RelPath", relative);
         }
         // add file to the navtree
         parent.add(new Node(name, link, null, null, null, type));
         i++;
       } else if (f.isDirectory()) {
         List<Node> mchildren = new ArrayList<Node>();
         type = "dir";
         String dirpath = relative + name;
         link = dirpath + "/index.html";
         String hdfkeyName = (key + i + ".Name");
         String hdfkeyType = (key + i + ".Type");
         String hdfkeyHref = (key + i + ".Href");
         hdf.setValue(hdfkeyName, name);
         hdf.setValue(hdfkeyType, type);
         hdf.setValue(hdfkeyHref, relative + name + "/" + "index.html");
         //System.out.println("Found directory, recursing. Current key: " + hdfkeyName);
         writeProjectDirectory(mchildren, f, relative + name + "/", true, hdf, (key + i));
         if (mchildren.size() > 0) {
           //dir is processed, now add it to the navtree
           //don't link sidenav subdirs at this point (but can use "link" to do so)
          parent.add(new Node(name, null, null, null, mchildren, type));
         }
         dirs.add(name);
         i++;
       }

    }
    //dd not working yet
    //Get summary from any _index files in any project dirs (currently disabled)
    //  getSummaryFromDir(hdf, dir, newkey);
    //If this is an index for the project root (assumed root if split length is 3 (development/samples/nn)),
    //then remove the root dir so that it won't appear in the breadcrumb. Else just pass it through to
    //setParentDirs as usual.
    String mpath = dir + "";
    String sdir[] = mpath.split("/");
    if (sdir.length == 3 ) {
      System.out.println("-----------------> this must be the root: [sdir len]" + sdir.length + "[dir]" + dir);
      hdf.setValue("showProjectPaths","true");//dd remove here?
    }
    setParentDirs(hdf, relative, name, false);
    //Generate an index.html page for each dir being processed
    ClearPage.write(hdf, "sampleindex.cs", relative + "/index" + Doclava.htmlExtension);
    //concatenate dirs in the navtree. Comment out or remove to restore normal navtree
    squashNodes(parent);
  }

  public void writeProjectStructure(String dir, Data hdf) {
      //System.out.println(">>-- writing project structure for " + dir );
      hdf.setValue("projectStructure", "true");
      hdf.setValue("projectDir", mProjectDir);
      hdf.setValue("page.title", mProjectDir + " Structure");
      hdf.setValue("projectTitle", mTitle);
      //write the project.html file
      ClearPage.write(hdf, "sampleindex.cs", mDest + "project" + Doclava.htmlExtension);
      hdf.setValue("projectStructure", "");
  }

  /**
  * Processes a templated project index page from _index.jd in a project root.
  * Each sample project must have an index, and each index locally defines it's own
  * page.tags and sample.group cs vars. This method takes a SC node on input, reads
  * any local vars from the _index.jd, generates an html file to out, then updates
  * the SC node with the page vars and returns it to the caller.
  *
  */
  public Node writeProjectIndexCs(Data hdf, File dir, String key, Node tnode) {
    //hdf.setValue("summary", "");
    //hdf.setValue("summaryFlag", "");
    String filename = dir.getAbsolutePath() + "/_index.jd";
    File f = new File(filename);
    String rel = dir.getPath();
    String mGroup = "";
    hdf.setValue("samples", "true");
    //set any default page variables for root index
    hdf.setValue("page.title", mProjectDir);
    hdf.setValue("projectDir", mProjectDir);
    hdf.setValue("projectTitle", mTitle);

    if (!f.isFile()) {
      //The sample didn't have any _index.jd, so create a stub.
      ClearPage.write(hdf, "sampleindex.cs", mDest + "index" + Doclava.htmlExtension);
      //Errors.error(Errors.INVALID_SAMPLE_INDEX, null, "Sample " + mProjectDir
      //          + ": Root _index.jd must be present and must define sample.group"
      //          + " tag. Please see ... for details.");
    } else {
      DocFile.writePage(filename, rel, mDest + "index" + Doclava.htmlExtension, hdf);
      tnode.setTags(hdf.getValue("page.tags", ""));
      mGroup = hdf.getValue("sample.group", "");
      if (mGroup.equals("")) {
        //Errors.error(Errors.INVALID_SAMPLE_INDEX, null, "Sample " + mProjectDir
        //          + ": Root _index.jd must be present and must define sample.group"
        //          + " tag. Please see ... for details.");
      } else {
      tnode.setGroup(hdf.getValue("sample.group", ""));
      }
    }
    return tnode;
  }

  /**
  * Keep track of file parents
  */
  Data setParentDirs(Data hdf, String subdir, String name, Boolean isFile) {
    //set whether to linkify the crumb dirs on each sample code page
    hdf.setValue("pathCrumbLinks", "");
        //isFile = false;
    int iter;
    hdf.removeTree("parentdirs");
    String s = subdir;
    String urlParts[] = s.split("/");
    //int n, l = (isFile)?1:0;
    int n, l = 1;
    //System.out.println("setParentDirs for " + subdir + name);
    for (iter=1; iter < urlParts.length; iter++) {
      n = iter-1;
      //System.out.println("parentdirs." + n + ".Name == " + urlParts[iter]);
      hdf.setValue("parentdirs." + n + ".Name", urlParts[iter]);
      hdf.setValue("parentdirs." + n + ".Link", subdir + "index" + Doclava.htmlExtension);
    }
    return hdf;
  }

  /**
  * Write a templated source code file to out.
  */
  public void writePage(File f, String out, String subdir) {
    String name = f.getName();
    String path = f.getPath();
    String data =
        SampleTagInfo.readFile(new SourcePositionInfo(path, -1, -1), path, "sample code",
            true, true, true, true);
    data = Doclava.escape(data);

    Data hdf = Doclava.makeHDF();

    String relative = subdir.replaceFirst("samples/", "");
    hdf.setValue("samples", "true");
    setParentDirs(hdf, subdir, name, true);
    hdf.setValue("projectTitle", mTitle);
    hdf.setValue("projectDir", mProjectDir);
    hdf.setValue("page.title", name);
    hdf.setValue("subdir", subdir);
    hdf.setValue("relative", relative);
    hdf.setValue("realFile", name);
    hdf.setValue("fileContents", data);
    hdf.setValue("resTag", "sample");
    hdf.setValue("resType", "Sample Code");

    ClearPage.write(hdf, "sample.cs", out);
  }

  /**
  * Write a templated image file to out.
  */
  public void writeImagePage(File f, String out, String subdir) {
    String name = f.getName();

    String data = "<img src=\"" + name + "\" title=\"" + name + "\" />";

    Data hdf = Doclava.makeHDF();
    hdf.setValue("samples", "true");
    setParentDirs(hdf, subdir, name, true);
    hdf.setValue("page.title", name);
    hdf.setValue("projectTitle", mTitle);
    hdf.setValue("projectDir", mProjectDir);
    hdf.setValue("subdir", subdir);
    hdf.setValue("realFile", name);
    hdf.setValue("fileContents", data);
    hdf.setValue("resTag", "sample");
    hdf.setValue("resType", "Sample Code");
    ClearPage.write(hdf, "sample.cs", out);
  }

  /**
  * Render a SC node tree to a navtree js file. 
  */
  public static void writeSamplesNavTree(List<Node> tnode, List<Node> groupnodes) {

    Node node = new Node("Reference", "packages.html", null, null, tnode, null);

    if (groupnodes != null) {
      for (int i = 0; i < tnode.size(); i++) {
        groupnodes = appendNodeGroups(tnode.get(i), groupnodes);
      }
      for (int n = 0; n < groupnodes.size(); n++) {
        if (groupnodes.get(n).getChildren() == null) {
          groupnodes.remove(n);
          n--;
        }
      }
      node.setChildren(groupnodes);
    }

    StringBuilder buf = new StringBuilder();
    if (false) {
    // if you want a root node
      buf.append("[");
      node.render(buf);
      buf.append("]");
    } else {
      // if you don't want a root node
      node.renderChildren(buf);
    }

    Data data = Doclava.makeHDF();
    data.setValue("reference_tree", buf.toString());
    ClearPage.write(data, "samples_navtree_data.cs", "samples_navtree_data.js");
  }

  /**
  * For a given project root node, get the group and then iterate the list of valid
  * groups looking for a match. If found, append the project to that group node.
  * Samples the reference a valid sample group tag are added to a list for that
  * group. Samples declare a sample.group tag in their _index.jd files.
  */
  private static List<Node> appendNodeGroups(Node gNode, List<Node> groupnodes) {
    List<Node> mgrouplist = new ArrayList<Node>();
    for (int i = 0; i < groupnodes.size(); i++) {
      if (groupnodes.get(i).getLabel().equals(gNode.getGroup())) {
        if (groupnodes.get(i).getChildren() == null) {
          mgrouplist.add(gNode);
          groupnodes.get(i).setChildren(mgrouplist);
        } else {
          groupnodes.get(i).getChildren().add(gNode);
        }
        break;
      }
    }
    return groupnodes;
  }

  /**
  * Sort by type and name (alpha), with manifest and src always at top.
  */
  Comparator<File> byTypeAndName = new Comparator<File>() {
    public int compare (File one, File other) {
      if (one.isDirectory() && !other.isDirectory()) {
        return 1;
      } else if (!one.isDirectory() && other.isDirectory()) {
        return -1;
      } else if (one.getName().equals("AndroidManifest.xml")) {
        return -1;
      } else if (one.getName().equals("src")) {
        return -1;
      } else {
        return one.compareTo(other);
      }
    }
  };

  /**
  * Concatenate dirs that only hold dirs to simplify nav tree
  */
  public static List<Node> squashNodes(List<Node> tnode) {
    List<Node> list = tnode;

    for(int i = 0; i < list.size(); ++i) {
      //only squash dirs that contain another dir whose list size is 1 and
      //that don't contain endpoints
      if ((list.get(i).getType().equals("dir")) &&
          (list.size() == 1) &&
          (list.get(i).getChildren().get(0).getChildren() != null)) {
        String thisLabel = list.get(i).getLabel();
        String childLabel =  list.get(i).getChildren().get(0).getLabel();
        String newLabel = thisLabel + "/" + childLabel;
        //Set label of parent and mChildren to those of child-child, skipping
        //squashed dir
        list.get(i).setLabel(newLabel);
        list.get(i).setChildren(list.get(i).getChildren().get(0).getChildren());
      } else {
        continue;
      }
    }
    return list;
  }

  /**
  * SampleCode variant of NavTree node.
  */
  public static class Node {
    private String mLabel;
    private String mLink;
    private String mGroup; // from sample.group in _index.jd
    private List<String> mTags; // from page.tags in _index.jd
    private List<Node> mChildren;
    private String mType;

    Node(String label, String link, String group, List<String> tags, List<Node> children, String type) {
      mLabel = label;
      mLink = link;
      mGroup = group;
      mTags = tags;
      mChildren = children;
      mType = type;
    }

    static void renderString(StringBuilder buf, String s) {
      if (s == null) {
        buf.append("null");
      } else {
        buf.append('"');
        final int N = s.length();
        for (int i = 0; i < N; i++) {
          char c = s.charAt(i);
          if (c >= ' ' && c <= '~' && c != '"' && c != '\\') {
            buf.append(c);
          } else {
            buf.append("\\u");
            for (int j = 0; i < 4; i++) {
              char x = (char) (c & 0x000f);
              if (x > 10) {
                x = (char) (x - 10 + 'a');
              } else {
                x = (char) (x + '0');
              }
              buf.append(x);
              c >>= 4;
            }
          }
        }
        buf.append('"');
      }
    }

    void renderChildren(StringBuilder buf) {
      List<Node> list = mChildren;
      if (list == null || list.size() == 0) {
        // We output null for no children. That way empty lists here can just
        // be a byproduct of how we generate the lists.
        buf.append("null");
      } else {
        buf.append("[ ");
        final int N = list.size();
        for (int i = 0; i < N; i++) {
          list.get(i).render(buf);
          if (i != N - 1) {
            buf.append(", ");
          }
        }
        buf.append(" ]\n");
      }
    }

    void renderTags(StringBuilder buf) {
      List<String> list = mTags;
      if (list == null || list.size() == 0) {
        buf.append("null");
      } else {
        buf.append("[ ");
        final int N = list.size();
        for (int i = 0; i < N; i++) {
          String tagval = list.get(i).toString();
          buf.append('"');
          final int L = tagval.length();
          for (int t = 0; t < L; t++) {
            char c = tagval.charAt(t);
            if (c >= ' ' && c <= '~' && c != '"' && c != '\\') {
              buf.append(c);
            } else {
              buf.append("\\u");
              for (int m = 0; m < 4; m++) {
                char x = (char) (c & 0x000f);
                if (x > 10) {
                  x = (char) (x - 10 + 'a');
                } else {
                  x = (char) (x + '0');
                }
                buf.append(x);
                c >>= 4;
              }
            } 
          }
          buf.append('"');
          if (i != N - 1) {
            buf.append(", ");
          } 
        }
        buf.append(" ]");
      }
    }

    void render(StringBuilder buf) {
      buf.append("[ ");
      renderString(buf, mLabel);
      buf.append(", ");
      renderString(buf, mLink);
      buf.append(", ");
      renderString(buf, mGroup);
      buf.append(", ");
      renderTags(buf);
      buf.append(", ");
      renderChildren(buf);
      buf.append(", ");
      renderString(buf, mType);
      buf.append(" ]");
    }

    public List<Node> getChildren() {
      if (mChildren != null) {
        return mChildren;
      } else {
        return null;
      }
    }

    public void setChildren(List<Node> node) {
        mChildren = node;
    }

    public String getLabel() {
      return mLabel;
    }

    public void setLabel(String label) {
       mLabel = label;
    }

    public String getType() {
      return mType.toString();
    }

    public String getHref() {
      return mLink;
    }

    public void setHref(String link) {
      mLink = link;
    }

    public String getGroup() {
      return mGroup;
    }

    public void setGroup(String group) {
      mGroup = group;
    }

    public void setTags(String tags) {
      List<String> tagList = new ArrayList();
      String[] tagParts = tags.split(",");

      for (int iter = 0; iter < tagParts.length; iter++) {
        String item = tagParts[iter].replaceAll("\"", "").trim();
        tagList.add(item);
      }
      mTags = tagList;
    }
  }

  /**
  * Write the project's templated _index.html
  * @deprecated
  *
  */
  public void writeProjectIndex(Data hdf) {
    //System.out.println(">>-- writing project index for " + mDest );
    hdf.setValue("projectDir", mProjectDir);
    hdf.setValue("page.title", mProjectDir + " Sample");
    hdf.setValue("projectTitle", mTitle);
    ClearPage.write(hdf, "sampleindex.cs", mDest + "index" + Doclava.htmlExtension);
  }

  /**
  * Grab the contents of an _index.html summary from a dir.
  * @deprecated
  */
  public void getSummaryFromDir(Data hdf, File dir, String key) {
    //System.out.println("Getting summary for " + dir + "/_index.html");
    hdf.setValue("summary", "");
    hdf.setValue("summaryFlag", "");
    String filename = dir.getPath() + "/_index.html";
    String summary = SampleTagInfo.readFile(new SourcePositionInfo(filename,
                          -1,-1), filename, "sample code", true, false, false, true);
    if (summary != null) {
      hdf.setValue(key + "SummaryFlag", "true");
      hdf.setValue("summary", summary);
      //set the target for [info] link
      //hdf.setValue(key + "Href", dir + "/index.html");
      //return true;
    }
  }

  /**
  * @deprecated
  */
  public void writeDirectory(File dir, String relative, boolean offline) {
    TreeSet<String> dirs = new TreeSet<String>();
    TreeSet<String> files = new TreeSet<String>();

    String subdir = relative; // .substring(mDest.length());

    for (File f : dir.listFiles()) {
      String name = f.getName();
      if (name.startsWith(".") || name.startsWith("_")) {
        continue;
      }
      if (f.isFile()) {
        String out = relative + name;
        if (inList(out, IMAGES)) {
          // copied directly
          ClearPage.copyFile(false, f, out);
          writeImagePage(f, convertExtension(out, Doclava.htmlExtension), subdir);
          files.add(name);
        }
        if (inList(out, TEMPLATED)) {
          // copied and goes through the template
          ClearPage.copyFile(false, f, out);
          writePage(f, convertExtension(out, Doclava.htmlExtension), subdir);
          files.add(name);

        }
        // else ignored
      } else if (f.isDirectory()) {
        writeDirectory(f, relative + name + "/", offline);
        dirs.add(name);
      }
    }

    // write the index page
    int i;

    Data hdf = writeIndex(dir);
    hdf.setValue("subdir", subdir);
    i = 0;
    for (String d : dirs) {
      hdf.setValue("subdirs." + i + ".Name", d);
      hdf.setValue("files." + i + ".Href", convertExtension(d, ".html"));
      i++;
    }
    i = 0;
    for (String f : files) {
      hdf.setValue("files." + i + ".Name", f);
      hdf.setValue("files." + i + ".Href", convertExtension(f, ".html"));
      i++;
    }

    if (!offline) relative = "/" + relative;
    ClearPage.write(hdf, "sampleindex.cs", relative + "index" + Doclava.htmlExtension);
  }

  /**
  * @deprecated
  */
  public void writeIndexOnly(File dir, String relative, Boolean offline) {
    Data hdf = writeIndex(dir);
    if (!offline) relative = "/" + relative;

      System.out.println("writing indexonly at " + relative + "/index" + Doclava.htmlExtension);
      ClearPage.write(hdf, "sampleindex.cs", relative + "index" + Doclava.htmlExtension);
  }
  
  /**
  * @deprecated
  */
  public Data writeIndex(File dir) {
    Data hdf = Doclava.makeHDF();
    hdf.setValue("page.title", dir.getName() + " - " + mTitle);
    hdf.setValue("projectTitle", mTitle);

    String filename = dir.getPath() + "/_index.html";
    String summary =
        SampleTagInfo.readFile(new SourcePositionInfo(filename, -1, -1), filename, "sample code",
            true, false, false, true);

    if (summary == null) {
      summary = "";
    }
    hdf.setValue("summary", summary);

    return hdf;
  }

}
