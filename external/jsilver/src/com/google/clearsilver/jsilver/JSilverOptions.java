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

package com.google.clearsilver.jsilver;

import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.data.NoOpStringInternStrategy;
import com.google.clearsilver.jsilver.data.StringInternStrategy;

import java.util.Map;

/**
 * Options for JSilver.
 * 
 * Note: Setter methods also return reference to this, allowing options to be defined in one
 * statement.
 * 
 * e.g. new JSilver(..., new JSilverOptions().setSomething(true).setAnother(false));
 * 
 * @see JSilver
 */
public class JSilverOptions implements Cloneable {

  private boolean cacheTemplates = true;
  private boolean compileTemplates = false;
  private int initialBufferSize = 8192;
  private boolean ignoreAttributes = false;
  private Map<Object, String> precompiledTemplateMap = null;
  private boolean useStrongCacheReferences = false;
  private EscapeMode escapeMode = EscapeMode.ESCAPE_NONE;
  private boolean propagateEscapeStatus = false;

  /**
   * A pool of strings used to optimize HDF parsing.
   * 
   * String interning has been shown to improve GC performance, but also to increase CPU usage. To
   * avoid any possible unexpected changes in behavior it is disabled by default.
   */
  private StringInternStrategy stringInternStrategy = new NoOpStringInternStrategy();

  /**
   * This flag is used to enable logging of all variables whose values are modified by auto escaping
   * or &lt;cs escape&gt; commands. These will be logged at {@code Level.WARNING}.
   */
  private boolean logEscapedVariables = false;
  private boolean useOutputBufferPool = false;
  private boolean stripHtmlWhiteSpace = false;
  private boolean stripStructuralWhiteSpace = false;
  private boolean allowGlobalDataModification = false;
  private boolean keepTemplateCacheFresh = false;
  private int loadPathCacheSize = 1000;

  // When adding fields, ensure you:
  // * add getter.
  // * add setter (which returns this).
  // * add to clone() method if necessary.

  /**
   * Set the initial size of the load path cache container. Setting this to 0 causes load path cache
   * to be disabled.
   */
  public JSilverOptions setLoadPathCacheSize(int loadPathCacheSize) {
    this.loadPathCacheSize = loadPathCacheSize;
    return this;
  }

  /**
   * @see #setLoadPathCacheSize(int)
   */
  public int getLoadPathCacheSize() {
    return loadPathCacheSize;
  }

  /**
   * Whether to cache templates. This will only ever load and parse a template from disk once. Best
   * switched on for production but left off for production (so you can update templates without
   * restarting).
   */
  public JSilverOptions setCacheTemplates(boolean cacheTemplates) {
    this.cacheTemplates = cacheTemplates;
    return this;
  }

  /**
   * @see #setCacheTemplates(boolean)
   */
  public boolean getCacheTemplates() {
    return cacheTemplates;
  }

  /**
   * Compile templates to Java byte code. This slows down the initial render as it performs a
   * compilation step, but then subsequent render are faster.
   * 
   * Compiled templates are always cached.
   * 
   * WARNING: This functionality is experimental. Use with caution.
   */
  public JSilverOptions setCompileTemplates(boolean compileTemplates) {
    this.compileTemplates = compileTemplates;
    return this;
  }

  /**
   * @see #setCompileTemplates(boolean)
   */
  public boolean getCompileTemplates() {
    return compileTemplates;
  }

  /**
   * If set, then HDF attributes in HDF files will be ignored and not stored in the Data object
   * filled by the parser. Default is {@code false}. Many applications use HDF attributes only in
   * template preprocessing (like translation support) and never in production servers where the
   * templates are rendered. By disabling attribute parsing, these applications can save on memory
   * for storing HDF structures read from files.
   */
  public JSilverOptions setIgnoreAttributes(boolean ignoreAttributes) {
    this.ignoreAttributes = ignoreAttributes;
    return this;
  }

  /**
   * @see #setIgnoreAttributes(boolean)
   */
  public boolean getIgnoreAttributes() {
    return ignoreAttributes;
  }

  /**
   * Initial buffer size used when rendering directly to a string.
   */
  public JSilverOptions setInitialBufferSize(int initialBufferSize) {
    this.initialBufferSize = initialBufferSize;
    return this;
  }

  /**
   * @see #setInitialBufferSize(int)
   */
  public int getInitialBufferSize() {
    return initialBufferSize;
  }

  /**
   * Optional mapping of TemplateLoader keys to Template instances that will be queried when loading
   * a template. If the Template is found it is returned. If not, the next template loader is
   * consulted.
   * 
   * @param precompiledTemplateMap map of TemplateLoader keys to corresponding class names that
   *        should be valid BaseCompiledTemplate subclasses. Set to {@code null} (default) to not
   *        load precompiled templates.
   */
  public JSilverOptions setPrecompiledTemplateMap(Map<Object, String> precompiledTemplateMap) {
    this.precompiledTemplateMap = precompiledTemplateMap;
    return this;
  }

  /**
   * @see #setPrecompiledTemplateMap(java.util.Map)
   * @return a mapping of TemplateLoader keys to corresponding BaseCompiledTemplate class names, or
   *         {@code null} (default) if no precompiled templates should be preloaded.
   */
  public Map<Object, String> getPrecompiledTemplateMap() {
    return precompiledTemplateMap;
  }

  /**
   * If {@code true}, then the template cache will use strong persistent references for the values.
   * If {@code false} (default) it will use soft references. Warning: The cache size is unbounded so
   * only use this option if you have sufficient memory to load all the Templates into memory.
   */
  public JSilverOptions setUseStrongCacheReferences(boolean value) {
    this.useStrongCacheReferences = value;
    return this;
  }

  /**
   * @see #setUseStrongCacheReferences(boolean)
   */
  public boolean getUseStrongCacheReferences() {
    return useStrongCacheReferences;
  }

  /**
   * @see #setEscapeMode(com.google.clearsilver.jsilver.autoescape.EscapeMode)
   */
  public EscapeMode getEscapeMode() {
    return escapeMode;
  }

  /**
   * Escape any template being rendered with the given escaping mode. If the mode is ESCAPE_HTML,
   * ESCAPE_URL or ESCAPE_JS, the corresponding escaping will be all variables in the template. If
   * the mode is ESCAPE_AUTO, enable <a href="http://go/autoescapecs">auto escaping</a> on
   * templates. For each variable in the template, this will determine what type of escaping should
   * be applied to the variable, and automatically apply this escaping. This flag can be overriden
   * by setting appropriate HDF variables before loading a template. If Config.AutoEscape is 1, auto
   * escaping is enabled. If Config.VarEscapeMode is set to one of 'html', 'js' or 'url', the
   * corresponding escaping is applied to all variables.
   * 
   * @param escapeMode
   */
  public JSilverOptions setEscapeMode(EscapeMode escapeMode) {
    this.escapeMode = escapeMode;
    return this;
  }

  /**
   * @see #setPropagateEscapeStatus
   */
  public boolean getPropagateEscapeStatus() {
    return propagateEscapeStatus;
  }

  /**
   * Only used for templates that are being <a href="http://go/autoescapecs">auto escaped</a>. If
   * {@code true} and auto escaping is enabled, variables created by &lt;cs set&gt; or &lt;cs
   * call&gt; commands are not auto escaped if they are assigned constant or escaped values. This is
   * disabled by default.
   * 
   * @see #setEscapeMode
   */
  public JSilverOptions setPropagateEscapeStatus(boolean propagateEscapeStatus) {
    this.propagateEscapeStatus = propagateEscapeStatus;
    return this;
  }

  /**
   * Sets the {@link StringInternStrategy} object that will be used to optimize HDF parsing.
   * 
   * <p>
   * Set value should not be {@code null} as it can cause {@link NullPointerException}.
   * 
   * @param stringInternStrategy - {@link StringInternStrategy} object
   */
  public void setStringInternStrategy(StringInternStrategy stringInternStrategy) {
    if (stringInternStrategy == null) {
      throw new IllegalArgumentException("StringInternStrategy should not be null.");
    }
    this.stringInternStrategy = stringInternStrategy;
  }

  /**
   * Returns {@link StringInternStrategy} object that is used for optimization of HDF parsing.
   * 
   * <p>
   * The returned value should never be {@code null}.
   * 
   * @return currently used {@link StringInternStrategy} object.
   */
  public StringInternStrategy getStringInternStrategy() {
    return stringInternStrategy;
  }

  /**
   * If {@code true}, then use a threadlocal buffer pool for holding rendered output when outputting
   * to String. Assumes that render() is called from a thread and that thread will not reenter
   * render() while it is running. If {@code false}, a new buffer is allocated for each request
   * where an Appendable output object was not provided.
   */
  public JSilverOptions setUseOutputBufferPool(boolean value) {
    this.useOutputBufferPool = value;
    return this;
  }

  /**
   * @see #setUseOutputBufferPool(boolean)
   */
  public boolean getUseOutputBufferPool() {
    return useOutputBufferPool;
  }

  /**
   * If {@code true}, then unnecessary whitespace will be stripped from the output. 'Unnecessary' is
   * meant in terms of HTML output. See
   * {@link com.google.clearsilver.jsilver.template.HtmlWhiteSpaceStripper} for more info.
   */
  public JSilverOptions setStripHtmlWhiteSpace(boolean value) {
    this.stripHtmlWhiteSpace = value;
    return this;
  }

  /**
   * @see #setStripHtmlWhiteSpace(boolean)
   */
  public boolean getStripHtmlWhiteSpace() {
    return stripHtmlWhiteSpace;
  }

  /**
   * If {@code true}, then structural whitespace will be stripped from the output. This allows
   * templates to be written to more closely match normal programming languages. See
   * {@link com.google.clearsilver.jsilver.syntax.StructuralWhitespaceStripper} for more info.
   */
  public JSilverOptions setStripStructuralWhiteSpace(boolean value) {
    this.stripStructuralWhiteSpace = value;
    return this;
  }

  /**
   * @see #setStripHtmlWhiteSpace(boolean)
   */
  public boolean getStripStructuralWhiteSpace() {
    return stripStructuralWhiteSpace;
  }

  /**
   * Use this method to disable wrapping the global HDF with an UnmodifiableData object which
   * prevents modification. This flag is here in case there are corner cases or performance reasons
   * that someone may want to disable this protection.
   * 
   * Should not be set to {@code true} unless incompatibilities or performance issues found. Note,
   * that setting to {@code true} could introduce bugs in templates that acquire local references to
   * the global data structure and then try to modify them, which is not the intended behavior.
   * Allowing global data modification during rendering is not compatible with the recently fixed
   * JNI Clearsilver library.
   * 
   * TODO: Remove once legacy mode is no longer needed.
   * 
   * @param allowGlobalDataModification {@code true} if you want to avoid wrapping the global HDF so
   *        that all writes to it during rendering are prevented and throw an exception.
   * @return this object.
   */
  public JSilverOptions setAllowGlobalDataModification(boolean allowGlobalDataModification) {
    this.allowGlobalDataModification = allowGlobalDataModification;
    return this;
  }

  /**
   * @see #setAllowGlobalDataModification(boolean)
   */
  public boolean getAllowGlobalDataModification() {
    return allowGlobalDataModification;
  }

  /**
   * @param keepTemplateCacheFresh {@code true} to have the template cache call
   *        {@link com.google.clearsilver.jsilver.resourceloader.ResourceLoader#getResourceVersionId(String)}
   *        to check if it should refresh its cache entries (this incurs a small performance penalty
   *        each time the cache is accessed)
   * @return this object
   */
  public JSilverOptions setKeepTemplateCacheFresh(boolean keepTemplateCacheFresh) {
    this.keepTemplateCacheFresh = keepTemplateCacheFresh;
    return this;
  }

  /**
   * @see #setKeepTemplateCacheFresh(boolean)
   */
  public boolean getKeepTemplateCacheFresh() {
    return keepTemplateCacheFresh;
  }

  @Override
  public JSilverOptions clone() {
    try {
      return (JSilverOptions) super.clone();
    } catch (CloneNotSupportedException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * @see #setLogEscapedVariables
   */
  public boolean getLogEscapedVariables() {
    return logEscapedVariables;
  }

  /**
   * Use this method to enable logging of all variables whose values are modified by auto escaping
   * or &lt;cs escape&gt; commands. These will be logged at {@code Level.WARNING}. This is useful
   * for detecting variables that should be exempt from auto escaping.
   * 
   * <p>
   * It is recommended to only enable this flag during testing or debugging and not for production
   * jobs.
   * 
   * @see #setEscapeMode
   */
  public JSilverOptions setLogEscapedVariables(boolean logEscapedVariables) {
    this.logEscapedVariables = logEscapedVariables;
    return this;
  }
}
