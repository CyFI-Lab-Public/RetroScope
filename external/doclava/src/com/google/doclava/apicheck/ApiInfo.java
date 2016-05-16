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

package com.google.doclava.apicheck;

import com.google.doclava.ClassInfo;
import com.google.doclava.Errors;
import com.google.doclava.PackageInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ApiInfo {

  private HashMap<String, PackageInfo> mPackages
      = new HashMap<String, PackageInfo>();
  private HashMap<String, ClassInfo> mAllClasses
      = new HashMap<String, ClassInfo>();
  private Map<ClassInfo,String> mClassToSuper
      = new HashMap<ClassInfo, String>();
  private Map<ClassInfo, ArrayList<String>> mClassToInterface
      = new HashMap<ClassInfo, ArrayList<String>>();


  public ClassInfo findClass(String name) {
    return mAllClasses.get(name);
  }

  protected void resolveInterfaces() {
    for (ClassInfo cl : mAllClasses.values()) {
      ArrayList<String> ifaces = mClassToInterface.get(cl);
      if (ifaces == null) {
        continue;
      }
      for (String iface : ifaces) {
        cl.addInterface(mAllClasses.get(iface));
      }
    }
  }

  /**
   * Checks to see if this api is consistent with a newer version.
   */
  public boolean isConsistent(ApiInfo otherApi) {
    boolean consistent = true;
    for (PackageInfo pInfo : mPackages.values()) {
      if (otherApi.getPackages().containsKey(pInfo.name())) {
        if (!pInfo.isConsistent(otherApi.getPackages().get(pInfo.name()))) {
          consistent = false;
        }
      } else {
        Errors.error(Errors.REMOVED_PACKAGE, pInfo.position(), "Removed package " + pInfo.name());
        consistent = false;
      }
    }
    for (PackageInfo pInfo : otherApi.mPackages.values()) {
      if (!mPackages.containsKey(pInfo.name())) {
        Errors.error(Errors.ADDED_PACKAGE, pInfo.position(), "Added package " + pInfo.name());
        consistent = false;
      }
    }
    return consistent;
  }

  public HashMap<String, PackageInfo> getPackages() {
    return mPackages;
  }
  
  protected void mapClassToSuper(ClassInfo classInfo, String superclass) {
    mClassToSuper.put(classInfo, superclass);
  }
  
  protected void mapClassToInterface(ClassInfo classInfo, String iface) {
    if (!mClassToInterface.containsKey(classInfo)) {
      mClassToInterface.put(classInfo, new ArrayList<String>());
    }
    mClassToInterface.get(classInfo).add(iface);
  }

  protected void addPackage(PackageInfo pInfo) {
    // track the set of organized packages in the API
    pInfo.setContainingApi(this);
    mPackages.put(pInfo.name(), pInfo);

    // accumulate a direct map of all the classes in the API
    for (ClassInfo cl : pInfo.allClasses().values()) {
      mAllClasses.put(cl.qualifiedName(), cl);
    }
  }

  protected void resolveSuperclasses() {
    for (ClassInfo cl : mAllClasses.values()) {
      // java.lang.Object has no superclass
      if (!cl.qualifiedName().equals("java.lang.Object")) {
        String scName = mClassToSuper.get(cl);
        if (scName == null) {
          scName = "java.lang.Object";
        }
        ClassInfo superclass = mAllClasses.get(scName);
        if (superclass == null) {
          // Superclass not provided by this codebase. Inject a stub.
          superclass = new ClassInfo(scName);
        }
        cl.setSuperClass(superclass);
      }
    }
  }
}
