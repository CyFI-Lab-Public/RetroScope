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
import com.google.doclava.apicheck.ApiCheck;
import com.google.doclava.apicheck.ApiInfo;
import com.google.doclava.apicheck.ApiParseException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Applies version information to the Doclava class model from apicheck XML files. Sample usage:
 * 
 * <pre>
 *   ClassInfo[] classInfos = ...
 *
 *   SinceTagger sinceTagger = new SinceTagger()
 *   sinceTagger.addVersion("frameworks/base/api/1.xml", "product 1.0")
 *   sinceTagger.addVersion("frameworks/base/api/2.xml", "product 1.5")
 *   sinceTagger.tagAll(...);
 * </pre>
 */
public class SinceTagger {

  private final Map<String, String> xmlToName = new LinkedHashMap<String, String>();

  /**
   * Specifies the apicheck XML file and the API version it holds. Calls to this method should be
   * called in order from oldest version to newest.
   */
  public void addVersion(String file, String name) {
    xmlToName.put(file, name);
  }

  public void tagAll(ClassInfo[] classDocs) {
    // read through the XML files in order, applying their since information
    // to the Javadoc models
    for (Map.Entry<String, String> versionSpec : xmlToName.entrySet()) {
      String xmlFile = versionSpec.getKey();
      String versionName = versionSpec.getValue();
      
      ApiInfo specApi;
      try {
        specApi = new ApiCheck().parseApi(xmlFile);
      } catch (ApiParseException e) {
        StringWriter stackTraceWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTraceWriter));
        Errors.error(Errors.BROKEN_SINCE_FILE, null, "Failed to parse " + xmlFile
                + " for " + versionName + " since data.\n" + stackTraceWriter.toString());
        continue;
      }

      applyVersionsFromSpec(versionName, specApi, classDocs);
    }

    if (!xmlToName.isEmpty()) {
      warnForMissingVersions(classDocs);
    }
  }

  public boolean hasVersions() {
    return !xmlToName.isEmpty();
  }

  /**
   * Writes an index of the version names to {@code data}.
   */
  public void writeVersionNames(Data data) {
    int index = 1;
    for (String version : xmlToName.values()) {
      data.setValue("since." + index + ".name", version);
      index++;
    }
  }

  /**
   * Applies the version information to {@code classDocs} where not already present.
   * 
   * @param versionName the version name
   * @param specApi the spec for this version. If a symbol is in this spec, it was present in the
   *        named version
   * @param classDocs the doc model to update
   */
  private void applyVersionsFromSpec(String versionName, ApiInfo specApi, ClassInfo[] classDocs) {
    for (ClassInfo classDoc : classDocs) {
      PackageInfo packageSpec
          = specApi.getPackages().get(classDoc.containingPackage().name());

      if (packageSpec == null) {
        continue;
      }

      ClassInfo classSpec = packageSpec.allClasses().get(classDoc.name());

      if (classSpec == null) {
        continue;
      }

      versionPackage(versionName, classDoc.containingPackage());
      versionClass(versionName, classSpec, classDoc);
      versionConstructors(versionName, classSpec, classDoc);
      versionFields(versionName, classSpec, classDoc);
      versionMethods(versionName, classSpec, classDoc);
    }
  }

  /**
   * Applies version information to {@code doc} where not already present.
   */
  private void versionPackage(String versionName, PackageInfo doc) {
    if (doc.getSince() == null) {
      doc.setSince(versionName);
    }
  }

  /**
   * Applies version information to {@code doc} where not already present.
   */
  private void versionClass(String versionName, ClassInfo spec, ClassInfo doc) {
    if (doc.getSince() == null) {
      doc.setSince(versionName);
    }

    // Set deprecated version
    if (doc.isDeprecated() && doc.getDeprecatedSince() == null) {
      if (spec.isDeprecated()) {
        doc.setDeprecatedSince(versionName);
      }
    }
  }

  /**
   * Applies version information from {@code spec} to {@code doc} where not already present.
   */
  private void versionConstructors(String versionName, ClassInfo spec, ClassInfo doc) {
    for (MethodInfo constructor : doc.constructors()) {
      if (constructor.getSince() == null
          && spec.hasConstructor(constructor)) {
        constructor.setSince(versionName);
      }

      // Set deprecated version
      if (constructor.isDeprecated() && constructor.getDeprecatedSince() == null) {
        // Find matching field from API spec
        if (spec.allConstructorsMap().containsKey(constructor.getHashableName())) {
          MethodInfo specConstructor = spec.allConstructorsMap().get(constructor.getHashableName());
          if (specConstructor.isDeprecated()) {
            constructor.setDeprecatedSince(versionName);
          }
        }
      }
    }
  }

  /**
   * Applies version information from {@code spec} to {@code doc} where not already present.
   */
  private void versionFields(String versionName, ClassInfo spec, ClassInfo doc) {
    for (FieldInfo field : doc.fields()) {
      if (field.getSince() == null && (spec.allFields().containsKey(field.name()) ||
                                       spec.allEnums().containsKey(field.name()))) {
        field.setSince(versionName);
      }

      // Set deprecated version
      if (field.isDeprecated() && field.getDeprecatedSince() == null) {
        // Find matching field from API spec
        if (spec.allFields().containsKey(field.name())) {
          FieldInfo specField = spec.allFields().get(field.name());
          if (specField.isDeprecated()) {
            field.setDeprecatedSince(versionName);
          }
        }
      }
    }
  }

  /**
   * Applies version information from {@code spec} to {@code doc} where not already present.
   */
  private void versionMethods(String versionName, ClassInfo spec, ClassInfo doc) {
    for (MethodInfo method : doc.methods()) {

      // Set deprecated version
      if (method.isDeprecated() && method.getDeprecatedSince() == null) {
        // Find matching method from API spec
        if (spec.allMethods().containsKey(method.getHashableName())) {
          MethodInfo specMethod = spec.allMethods().get(method.getHashableName());
          if (specMethod.isDeprecated()) {
            method.setDeprecatedSince(versionName);
          }
        }
      }

      if (method.getSince() != null) {
        continue;
      }

      for (ClassInfo superclass : spec.hierarchy()) {
        if (superclass.allMethods().containsKey(method.getHashableName())) {
          method.setSince(versionName);
          break;
        }
      }
    }
  }

  /**
   * Warns if any symbols are missing version information. When configured properly, this will yield
   * zero warnings because {@code apicheck} guarantees that all symbols are present in the most
   * recent API.
   */
  private void warnForMissingVersions(ClassInfo[] classDocs) {
    for (ClassInfo claz : classDocs) {
      if (!checkLevelRecursive(claz)) {
        continue;
      }

      if (claz.getSince() == null) {
        Errors.error(Errors.NO_SINCE_DATA, claz.position(), "XML missing class "
            + claz.qualifiedName());
      }

      for (FieldInfo field : missingVersions(claz.fields())) {
        Errors.error(Errors.NO_SINCE_DATA, field.position(), "XML missing field "
            + claz.qualifiedName() + "#" + field.name());
      }

      for (MethodInfo constructor : missingVersions(claz.constructors())) {
        Errors.error(Errors.NO_SINCE_DATA, constructor.position(), "XML missing constructor "
            + claz.qualifiedName() + "#" + constructor.getHashableName());
      }

      for (MethodInfo method : missingVersions(claz.methods())) {
        Errors.error(Errors.NO_SINCE_DATA, method.position(), "XML missing method "
            + claz.qualifiedName() + "#" + method.getHashableName());
      }
    }
  }

  /**
   * Returns the DocInfos in {@code all} that are documented but do not have since tags.
   */
  private <T extends MemberInfo> Iterable<T> missingVersions(ArrayList<T> all) {
    List<T> result = Collections.emptyList();
    for (T t : all) {
      // if this member has version info or isn't documented, skip it
      if (t.getSince() != null || t.isHidden() || !checkLevelRecursive(t.realContainingClass())) {
        continue;
      }

      if (result.isEmpty()) {
        result = new ArrayList<T>(); // lazily construct a mutable list
      }
      result.add(t);
    }
    return result;
  }

  /**
   * Returns true if {@code claz} and all containing classes are documented. The result may be used
   * to filter out members that exist in the API data structure but aren't a part of the API.
   */
  private boolean checkLevelRecursive(ClassInfo claz) {
    for (ClassInfo c = claz; c != null; c = c.containingClass()) {
      if (!c.checkLevel()) {
        return false;
      }
    }
    return true;
  }
}
