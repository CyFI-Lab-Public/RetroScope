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

import com.google.doclava.apicheck.ApiInfo;
import com.google.clearsilver.jsilver.data.Data;

import com.sun.javadoc.*;
import java.util.*;

public class PackageInfo extends DocInfo implements ContainerInfo {
  public static final String DEFAULT_PACKAGE = "default package";

  public static final Comparator<PackageInfo> comparator = new Comparator<PackageInfo>() {
    public int compare(PackageInfo a, PackageInfo b) {
      return a.name().compareTo(b.name());
    }
  };

  public PackageInfo(PackageDoc pkg, String name, SourcePositionInfo position) {
    super(pkg.getRawCommentText(), position);
    if (name.isEmpty()) {
      mName = DEFAULT_PACKAGE;
    } else {
      mName = name;
    }

    mPackage = pkg;
    initializeMaps();
  }

  public PackageInfo(String name) {
    super("", null);
    mName = name;
    initializeMaps();
  }

  public PackageInfo(String name, SourcePositionInfo position) {
    super("", position);

    if (name.isEmpty()) {
      mName = "default package";
    } else {
      mName = name;
    }
    initializeMaps();
  }

  private void initializeMaps() {
      mInterfacesMap = new HashMap<String, ClassInfo>();
      mOrdinaryClassesMap = new HashMap<String, ClassInfo>();
      mEnumsMap = new HashMap<String, ClassInfo>();
      mExceptionsMap = new HashMap<String, ClassInfo>();
      mErrorsMap = new HashMap<String, ClassInfo>();
  }

  public String htmlPage() {
    String s = mName;
    s = s.replace('.', '/');
    s += "/package-summary.html";
    s = Doclava.javadocDir + s;
    return s;
  }

  @Override
  public ContainerInfo parent() {
    return null;
  }

  @Override
  public boolean isHidden() {
    return comment().isHidden();
  }

  public boolean checkLevel() {
    // TODO should return false if all classes are hidden but the package isn't.
    // We don't have this so I'm not doing it now.
    return !isHidden();
  }

  public String name() {
    return mName;
  }

  public String qualifiedName() {
    return mName;
  }

  public TagInfo[] inlineTags() {
    return comment().tags();
  }

  public TagInfo[] firstSentenceTags() {
    return comment().briefTags();
  }

  public static ClassInfo[] filterHidden(ClassInfo[] classes) {
    ArrayList<ClassInfo> out = new ArrayList<ClassInfo>();

    for (ClassInfo cl : classes) {
      if (!cl.isHidden()) {
        out.add(cl);
      }
    }

    return out.toArray(new ClassInfo[0]);
  }

  public void makeLink(Data data, String base) {
    if (checkLevel()) {
      data.setValue(base + ".link", htmlPage());
    }
    data.setValue(base + ".name", name());
    data.setValue(base + ".since", getSince());
  }

  public void makeClassLinkListHDF(Data data, String base) {
    makeLink(data, base);
    ClassInfo.makeLinkListHDF(data, base + ".interfaces", interfaces());
    ClassInfo.makeLinkListHDF(data, base + ".classes", ordinaryClasses());
    ClassInfo.makeLinkListHDF(data, base + ".enums", enums());
    ClassInfo.makeLinkListHDF(data, base + ".exceptions", exceptions());
    ClassInfo.makeLinkListHDF(data, base + ".errors", errors());
    data.setValue(base + ".since", getSince());
  }

  public ClassInfo[] interfaces() {
    if (mInterfaces == null) {
      mInterfaces =
          ClassInfo.sortByName(filterHidden(Converter.convertClasses(mPackage.interfaces())));
    }
    return mInterfaces;
  }

  public ClassInfo[] ordinaryClasses() {
    if (mOrdinaryClasses == null) {
      mOrdinaryClasses =
          ClassInfo.sortByName(filterHidden(Converter.convertClasses(mPackage.ordinaryClasses())));
    }
    return mOrdinaryClasses;
  }

  public ClassInfo[] enums() {
    if (mEnums == null) {
      mEnums = ClassInfo.sortByName(filterHidden(Converter.convertClasses(mPackage.enums())));
    }
    return mEnums;
  }

  public ClassInfo[] exceptions() {
    if (mExceptions == null) {
      mExceptions =
          ClassInfo.sortByName(filterHidden(Converter.convertClasses(mPackage.exceptions())));
    }
    return mExceptions;
  }

  public ClassInfo[] errors() {
    if (mErrors == null) {
      mErrors = ClassInfo.sortByName(filterHidden(Converter.convertClasses(mPackage.errors())));
    }
    return mErrors;
  }

  public ApiInfo containingApi() {
    return mContainingApi;
  }

  public void setContainingApi(ApiInfo api) {
    mContainingApi = api;
  }

  // in hashed containers, treat the name as the key
  @Override
  public int hashCode() {
    return mName.hashCode();
  }

  private String mName;
  private PackageDoc mPackage;
  private ApiInfo mContainingApi;
  private ClassInfo[] mInterfaces;
  private ClassInfo[] mOrdinaryClasses;
  private ClassInfo[] mEnums;
  private ClassInfo[] mExceptions;
  private ClassInfo[] mErrors;

  private HashMap<String, ClassInfo> mInterfacesMap;
  private HashMap<String, ClassInfo> mOrdinaryClassesMap;
  private HashMap<String, ClassInfo> mEnumsMap;
  private HashMap<String, ClassInfo> mExceptionsMap;
  private HashMap<String, ClassInfo> mErrorsMap;


  public ClassInfo getClass(String className) {
      ClassInfo cls = mInterfacesMap.get(className);

      if (cls != null) {
          return cls;
      }

      cls = mOrdinaryClassesMap.get(className);

      if (cls != null) {
          return cls;
      }

      cls = mEnumsMap.get(className);

      if (cls != null) {
          return cls;
      }

      cls = mEnumsMap.get(className);

      if (cls != null) {
          return cls;
      }

      return mErrorsMap.get(className);
  }

  public void addInterface(ClassInfo cls) {
      cls.setPackage(this);
      mInterfacesMap.put(cls.name(), cls);
  }

  public ClassInfo getInterface(String interfaceName) {
      return mInterfacesMap.get(interfaceName);
  }

  public ClassInfo getOrdinaryClass(String className) {
      return mOrdinaryClassesMap.get(className);
  }

  public void addOrdinaryClass(ClassInfo cls) {
      cls.setPackage(this);
      mOrdinaryClassesMap.put(cls.name(), cls);
  }

  public ClassInfo getEnum(String enumName) {
      return mEnumsMap.get(enumName);
  }

  public void addEnum(ClassInfo cls) {
      cls.setPackage(this);
      this.mEnumsMap.put(cls.name(), cls);
  }

  public ClassInfo getException(String exceptionName) {
      return mExceptionsMap.get(exceptionName);
  }

  public ClassInfo getError(String errorName) {
      return mErrorsMap.get(errorName);
  }

  // TODO: Leftovers from ApiCheck that should be better merged.
  private HashMap<String, ClassInfo> mClasses = new HashMap<String, ClassInfo>();

  public void addClass(ClassInfo cls) {
    cls.setPackage(this);
    mClasses.put(cls.name(), cls);
  }

  public HashMap<String, ClassInfo> allClasses() {
    return mClasses;
  }

  public boolean isConsistent(PackageInfo pInfo) {
    boolean consistent = true;
    for (ClassInfo cInfo : mClasses.values()) {
      if (pInfo.mClasses.containsKey(cInfo.name())) {
        if (!cInfo.isConsistent(pInfo.mClasses.get(cInfo.name()))) {
          consistent = false;
        }
      } else {
        Errors.error(Errors.REMOVED_CLASS, cInfo.position(), "Removed public class "
            + cInfo.qualifiedName());
        consistent = false;
      }
    }
    for (ClassInfo cInfo : pInfo.mClasses.values()) {
      if (!mClasses.containsKey(cInfo.name())) {
        Errors.error(Errors.ADDED_CLASS, cInfo.position(), "Added class " + cInfo.name()
            + " to package " + pInfo.name());
        consistent = false;
      }
    }
    return consistent;
  }
}
