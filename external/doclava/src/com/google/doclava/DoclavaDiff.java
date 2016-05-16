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

import com.google.clearsilver.jsilver.JSilver;
import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.resourceloader.CompositeResourceLoader;
import com.google.clearsilver.jsilver.resourceloader.FileSystemResourceLoader;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to generate a web page highlighting the differences and
 * similarities among various Java libraries.
 *
 */
public final class DoclavaDiff {
  private final String outputDir;
  private final JSilver jSilver;
  private final List<FederatedSite> sites = new ArrayList<FederatedSite>();
  
  public static void main(String[] args) {
    new DoclavaDiff(args).generateSite();
  }
  
  public DoclavaDiff(String[] args) {
    // TODO: options parsing
    try {
      sites.add(new FederatedSite("Android", new URL("http://manatee/doclava/android")));
      sites.add(new FederatedSite("GWT", new URL("http://manatee/doclava/gwt")));
      //sites.add(new FederatedSite("Crore", new URL("http://manatee/doclava/crore")));
      outputDir = "build";
    } catch (Exception e) {
      throw new AssertionError(e);
    }
    
    // TODO: accept external templates
    List<ResourceLoader> resourceLoaders = new ArrayList<ResourceLoader>();
    resourceLoaders.add(new FileSystemResourceLoader("assets/templates"));

    ResourceLoader compositeResourceLoader = new CompositeResourceLoader(resourceLoaders);
    jSilver = new JSilver(compositeResourceLoader);
  }
  
  public void generateSite() {    
    Data data = generateHdf();
    generateHtml("diff.cs", data, new File(outputDir + "/diff.html"));
  }
  
  /**
   * Creates an HDF with this structure:
   * <pre>
   * sites.0.name = projectA
   * sites.0.url = http://proja.domain.com/reference
   * sites.1.name = projectB
   * sites.1.url = http://projb.domain.com
   * packages.0.name = java.lang
   * packages.0.sites.0.hasPackage = 1
   * packages.0.sites.0.link = http://proja.domain.com/reference/java/lang
   * packages.0.sites.1.hasPackage = 0
   * packages.0.classes.0.qualifiedName = java.lang.Object
   * packages.0.classes.0.sites.0.hasClass = 1
   * packages.0.classes.0.sites.0.link = http://proja.domain.com/reference/java/lang/Object
   * packages.0.classes.0.sites.1.hasClass = 0 
   * packages.0.classes.0.methods.0.signature = wait()
   * packages.0.classes.0.methods.0.sites.0.hasMethod = 1
   * packages.0.classes.0.methods.0.sites.0.link = http://proja.domain.com/reference/java/lang/Object#wait
   * packages.0.classes.0.methods.0.sites.1.hasMethod = 0
   * </pre>
   */
  private Data generateHdf() {
    Data data = jSilver.createData();
    
    data.setValue("triangle.opened", "../assets/templates/assets/images/triangle-opened.png");
    data.setValue("triangle.closed", "../assets/templates/assets/images/triangle-closed.png");
    
    int i = 0;
    for (FederatedSite site : sites) {
      String base = "sites." + (i++);
      data.setValue(base + ".name", site.name());
      data.setValue(base + ".url", site.baseUrl().toString());
    }
    
    List<String> allPackages = knownPackages(sites);
    
    int p = 0;
    for (String pkg : allPackages) {
      PackageInfo packageInfo = new PackageInfo(pkg);
      String packageBase = "packages." + (p++);
      data.setValue(packageBase + ".name", pkg);
      
      int s = 0;
      for (FederatedSite site : sites) {
        String siteBase = packageBase + ".sites." + (s++);
        if (site.apiInfo().getPackages().containsKey(pkg)) {
          data.setValue(siteBase + ".hasPackage", "1");
          data.setValue(siteBase + ".link", site.linkFor(packageInfo.htmlPage()));
        } else {
          data.setValue(siteBase + ".hasPackage", "0");
        }
      }
      
      if (packageUniqueToSite(pkg, sites)) {
        continue;
      }
            
      List<String> packageClasses = knownClassesForPackage(pkg, sites);
      int c = 0;
      for (String qualifiedClassName : packageClasses) {
        String classBase = packageBase + ".classes." + (c++);
        data.setValue(classBase + ".qualifiedName", qualifiedClassName);
        
        s = 0;
        for (FederatedSite site : sites) {
          String siteBase = classBase + ".sites." + (s++);
          ClassInfo classInfo = site.apiInfo().findClass(qualifiedClassName);
          if (classInfo != null) {
            data.setValue(siteBase + ".hasClass", "1");
            data.setValue(siteBase + ".link", site.linkFor(classInfo.htmlPage()));
          } else {
            data.setValue(siteBase + ".hasClass", "0");
          }
        }
        
        if (agreeOnClass(qualifiedClassName, sites)) {
          continue;
        }
        
        if (classUniqueToSite(qualifiedClassName, sites)) {
          continue;
        }
        
        int m = 0;
        List<MethodInfo> methods = knownMethodsForClass(qualifiedClassName, sites);
        for (MethodInfo method : methods) {
          if (agreeOnMethod(qualifiedClassName, method, sites)) {
            continue;
          }
          
          String methodBase = classBase + ".methods." + (m++);
          data.setValue(methodBase + ".signature", method.prettySignature());
          int k = 0;
          for (FederatedSite site : sites) {
            String siteBase = methodBase + ".sites." + (k++);
            if (site.apiInfo().findClass(qualifiedClassName) == null) {
              data.setValue(siteBase + ".hasMethod", "0");
              continue;
            }
            Map<String,MethodInfo> siteMethods
                = site.apiInfo().findClass(qualifiedClassName).allMethods();
            if (siteMethods.containsKey(method.getHashableName())) {
              data.setValue(siteBase + ".hasMethod", "1");
              data.setValue(siteBase + ".link", site.linkFor(method.htmlPage()));
            } else {
              data.setValue(siteBase + ".hasMethod", "0");
            }
          }
        }
      }
    }
    
    return data;
  }
  
  /**
   * Returns a list of all known packages from all sites.
   */
  private List<String> knownPackages(List<FederatedSite> sites) {
    Set<String> allPackages = new LinkedHashSet<String>();
    for (FederatedSite site : sites) {
      Map<String, PackageInfo> packages = site.apiInfo().getPackages();
      for (String pkg : packages.keySet()) {
        allPackages.add(pkg);
      }
    }
    
    List<String> packages = new ArrayList<String>(allPackages);
    Collections.sort(packages);
    return packages;
  }
  
  /**
   * Returns all known classes from all sites for a given package.
   */
  private List<String> knownClassesForPackage(String pkg, List<FederatedSite> sites) {
    Set<String> allClasses = new LinkedHashSet<String>();
    for (FederatedSite site : sites) {
      PackageInfo packageInfo = site.apiInfo().getPackages().get(pkg);
      if (packageInfo == null) {
        continue;
      }
      HashMap<String, ClassInfo> classes = packageInfo.allClasses();
      for (Map.Entry<String, ClassInfo> entry : classes.entrySet()) {
        allClasses.add(entry.getValue().qualifiedName());
      }
    }
    
    List<String> classes = new ArrayList<String>(allClasses);
    Collections.sort(classes);
    return classes;
  }
  
  /**
   * Returns all known methods from all sites for a given class.
   */
  private List<MethodInfo> knownMethodsForClass(String qualifiedClassName,
      List<FederatedSite> sites) {
    
    Map<String, MethodInfo> allMethods = new HashMap<String, MethodInfo>();
    for (FederatedSite site : sites) {
      ClassInfo classInfo = site.apiInfo().findClass(qualifiedClassName);
      if (classInfo == null) {
        continue;
      }
      
      for (Map.Entry<String, MethodInfo> entry: classInfo.allMethods().entrySet()) {
        allMethods.put(entry.getKey(), entry.getValue());
      }
    }
    
    List<MethodInfo> methods = new ArrayList<MethodInfo>();
    methods.addAll(allMethods.values());
    return methods;
  }
  
  /**
   * Returns true if the list of sites all completely agree on the given
   * package. All sites must possess the package, all classes it contains, and
   * all methods of each class.
   */
  private boolean agreeOnPackage(String pkg, List<FederatedSite> sites) {
    for (FederatedSite site : sites) {
      if (site.apiInfo().getPackages().get(pkg) == null) {
        return false;
      }
    }
    
    List<String> classes = knownClassesForPackage(pkg, sites);
    for (String clazz : classes) {
      if (!agreeOnClass(clazz, sites)) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Returns true if the list of sites all agree on the given class. Each site
   * must have the class and agree on its methods.
   */
  private boolean agreeOnClass(String qualifiedClassName, List<FederatedSite> sites) {
    List<MethodInfo> methods = knownMethodsForClass(qualifiedClassName, sites);
    for (MethodInfo method : methods) {
      if (!agreeOnMethod(qualifiedClassName, method, sites)) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Returns true if the list of sites all contain the given method.
   */
  private boolean agreeOnMethod(String qualifiedClassName, MethodInfo method,
      List<FederatedSite> sites) {
    
    for (FederatedSite site : sites) {
      ClassInfo siteClass = site.apiInfo().findClass(qualifiedClassName);
      if (siteClass == null) {
        return false;
      }
      
      if (!siteClass.supportsMethod(method)) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Returns true if the given package is known to exactly one of the given sites.
   */
  private boolean packageUniqueToSite(String pkg, List<FederatedSite> sites) {
    int numSites = 0;
    for (FederatedSite site : sites) {
      if (site.apiInfo().getPackages().containsKey(pkg)) {
        numSites++;
      }
    }
    return numSites == 1;
  }
  
  /**
   * Returns true if the given class is known to exactly one of the given sites.
   */
  private boolean classUniqueToSite(String qualifiedClassName, List<FederatedSite> sites) {
    int numSites = 0;
    for (FederatedSite site : sites) {
      if (site.apiInfo().findClass(qualifiedClassName) != null) {
        numSites++;
      }
    }
    return numSites == 1;
  }
  
  private void generateHtml(String template, Data data, File file) {
    ClearPage.ensureDirectory(file);
    
    OutputStreamWriter stream = null;
    try {
      stream = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
      String rendered = jSilver.render(template, data);
      stream.write(rendered, 0, rendered.length());
    } catch (IOException e) {
      System.out.println("error: " + e.getMessage() + "; when writing file: " + file.getAbsolutePath());
    } finally {
      if (stream != null) {
        try {
          stream.close();
        } catch (IOException ignored) {}
      }
    }
  }
}
