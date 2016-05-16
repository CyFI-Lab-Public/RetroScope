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

package com.google.clearsilver.jsilver.precompiler;

import com.google.clearsilver.jsilver.autoescape.AutoEscapeOptions;
import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.compiler.BaseCompiledTemplate;
import com.google.clearsilver.jsilver.functions.FunctionExecutor;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;
import com.google.clearsilver.jsilver.template.DelegatingTemplateLoader;
import com.google.clearsilver.jsilver.template.Template;
import com.google.clearsilver.jsilver.template.TemplateLoader;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

/**
 * TemplateLoader that stores objects from precompiled Template classes and serves them when asked
 * for them. If not found, it passes the request on to the delegate TemplateLoader.
 */
public class PrecompiledTemplateLoader implements DelegatingTemplateLoader {

  /**
   * This is the next TemplateLoader to ask if we don't find the template.
   */
  private final TemplateLoader nextLoader;

  private final Map<Object, BaseCompiledTemplate> templateMap;
  private final AutoEscapeOptions autoEscapeOptions;

  public PrecompiledTemplateLoader(TemplateLoader nextLoader,
      Map<Object, String> templateToClassNameMap, FunctionExecutor globalFunctionExecutor,
      AutoEscapeOptions autoEscapeOptions) {
    this.nextLoader = nextLoader;
    this.autoEscapeOptions = autoEscapeOptions;
    this.templateMap = makeTemplateMap(templateToClassNameMap, globalFunctionExecutor);
  }

  private Map<Object, BaseCompiledTemplate> makeTemplateMap(
      Map<Object, String> templateToClassNameMap, FunctionExecutor globalFunctionExecutor) {
    Map<Object, BaseCompiledTemplate> templateMap = new HashMap<Object, BaseCompiledTemplate>();
    ClassLoader classLoader = getClass().getClassLoader();
    for (Map.Entry<Object, String> entry : templateToClassNameMap.entrySet()) {
      String className = entry.getValue();
      BaseCompiledTemplate compiledTemplate = loadTemplateObject(className, classLoader);
      // Fill in the necessary
      compiledTemplate.setFunctionExecutor(globalFunctionExecutor);
      compiledTemplate.setTemplateName(entry.getKey().toString());
      compiledTemplate.setTemplateLoader(this);
      if (entry.getKey() instanceof PrecompiledTemplateMapKey) {
        PrecompiledTemplateMapKey mapKey = (PrecompiledTemplateMapKey) entry.getKey();
        // The template's escapeMode is not currently used as the autoescaping is all
        // handled at compile time. Still set it in case it is needed later on.
        compiledTemplate.setEscapeMode(mapKey.getEscapeMode());
      } else {
        compiledTemplate.setEscapeMode(EscapeMode.ESCAPE_NONE);
      }
      compiledTemplate.setAutoEscapeOptions(autoEscapeOptions);
      templateMap.put(entry.getKey(), compiledTemplate);
    }
    return ImmutableMap.copyOf(templateMap);
  }

  @VisibleForTesting
  protected BaseCompiledTemplate loadTemplateObject(String className, ClassLoader classLoader) {
    try {
      Class<?> templateClass = classLoader.loadClass(className);
      // TODO: Not safe to use in untrusted environments
      // Does not handle ClassCastException or
      // verify class type before calling newInstance.
      return (BaseCompiledTemplate) templateClass.newInstance();
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Class not found: " + className, e);
    } catch (IllegalAccessException e) {
      throw new Error(e);
    } catch (InstantiationException e) {
      throw new Error(e);
    }
  }

  @Override
  public void setTemplateLoaderDelegate(TemplateLoader templateLoaderDelegate) {
    for (BaseCompiledTemplate template : templateMap.values()) {
      template.setTemplateLoader(templateLoaderDelegate);
    }
  }

  @Override
  public Template load(String templateName, ResourceLoader resourceLoader, EscapeMode escapeMode) {
    Object key = resourceLoader.getKey(templateName);
    PrecompiledTemplateMapKey mapKey = new PrecompiledTemplateMapKey(key, escapeMode);
    Template template = templateMap.get(mapKey);
    if (template != null) {
      return template;
    } else {
      return nextLoader.load(templateName, resourceLoader, escapeMode);
    }
  }

  /**
   * We don't cache temporary templates here so we just call delegate TemplateLoader.
   */
  @Override
  public Template createTemp(String name, String content, EscapeMode escapeMode) {
    return nextLoader.createTemp(name, content, escapeMode);
  }
}
