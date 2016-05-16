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

import com.google.clearsilver.jsilver.autoescape.AutoEscapeOptions;
import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.compiler.TemplateCompiler;
import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.data.DataFactory;
import com.google.clearsilver.jsilver.data.HDFDataFactory;
import com.google.clearsilver.jsilver.exceptions.JSilverBadSyntaxException;
import com.google.clearsilver.jsilver.exceptions.JSilverException;
import com.google.clearsilver.jsilver.functions.Function;
import com.google.clearsilver.jsilver.functions.FunctionRegistry;
import com.google.clearsilver.jsilver.functions.TextFilter;
import com.google.clearsilver.jsilver.functions.bundles.ClearSilverCompatibleFunctions;
import com.google.clearsilver.jsilver.functions.bundles.CoreOperators;
import com.google.clearsilver.jsilver.interpreter.InterpretedTemplateLoader;
import com.google.clearsilver.jsilver.interpreter.LoadingTemplateFactory;
import com.google.clearsilver.jsilver.interpreter.OptimizerProvider;
import com.google.clearsilver.jsilver.interpreter.OptimizingTemplateFactory;
import com.google.clearsilver.jsilver.interpreter.TemplateFactory;
import com.google.clearsilver.jsilver.output.InstanceOutputBufferProvider;
import com.google.clearsilver.jsilver.output.OutputBufferProvider;
import com.google.clearsilver.jsilver.output.ThreadLocalOutputBufferProvider;
import com.google.clearsilver.jsilver.precompiler.PrecompiledTemplateLoader;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;
import com.google.clearsilver.jsilver.syntax.DataCommandConsolidator;
import com.google.clearsilver.jsilver.syntax.SyntaxTreeOptimizer;
import com.google.clearsilver.jsilver.syntax.StructuralWhitespaceStripper;
import com.google.clearsilver.jsilver.syntax.node.Switch;
import com.google.clearsilver.jsilver.template.DelegatingTemplateLoader;
import com.google.clearsilver.jsilver.template.HtmlWhiteSpaceStripper;
import com.google.clearsilver.jsilver.template.Template;
import com.google.clearsilver.jsilver.template.TemplateLoader;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * JSilver templating system.
 * 
 * <p>
 * This is a pure Java version of ClearSilver.
 * </p>
 * 
 * <h2>Example Usage</h2>
 * 
 * <pre>
 * // Load resources (e.g. templates) from directory.
 * JSilver jSilver = new JSilver(new FileResourceLoader("/path/to/templates"));
 *
 * // Set up some data.
 * Data data = new Data();
 * data.setValue("name.first", "Mr");
 * data.setValue("name.last", "Man");
 *
 * // Render template to System.out. Writer output = ...;
 * jSilver.render("say-hello", data, output);
 * </pre>
 * 
 * For example usage, see java/com/google/clearsilver/jsilver/examples.
 * 
 * Additional options can be passed to the constructor using JSilverOptions.
 * 
 * @see <a href="http://go/jsilver">JSilver Docs</a>
 * @see <a href="http://clearsilver.net">ClearSilver Docs</a>
 * @see JSilverOptions
 * @see Data
 * @see ResourceLoader
 */
public final class JSilver implements TemplateRenderer, DataLoader {

  private final JSilverOptions options;

  private final TemplateLoader templateLoader;

  /**
   * If caching enabled, the cached wrapper (otherwise null). Kept here so we can call clearCache()
   * later.
   */

  private final FunctionRegistry globalFunctions = new ClearSilverCompatibleFunctions();

  private final ResourceLoader defaultResourceLoader;

  private final DataFactory dataFactory;

  // Object used to return Appendable output buffers when needed.
  private final OutputBufferProvider outputBufferProvider;
  public static final String VAR_ESCAPE_MODE_KEY = "Config.VarEscapeMode";
  public static final String AUTO_ESCAPE_KEY = "Config.AutoEscape";

  /**
   * @param defaultResourceLoader Where resources (templates, HDF files) should be loaded from. e.g.
   *        directory, classpath, memory, etc.
   * @param options Additional options.
   * @see JSilverOptions
   */
  public JSilver(ResourceLoader defaultResourceLoader, JSilverOptions options) {
    // To ensure that options cannot be changed externally, we clone them and
    // use the frozen clone.
    options = options.clone();

    this.defaultResourceLoader = defaultResourceLoader;
    this.dataFactory =
        new HDFDataFactory(options.getIgnoreAttributes(), options.getStringInternStrategy());
    this.options = options;

    // Setup the output buffer provider either with a threadlocal pool
    // or creating a new instance each time it is asked for.
    int bufferSize = options.getInitialBufferSize();
    if (options.getUseOutputBufferPool()) {
      // Use a ThreadLocal to reuse StringBuilder objects.
      outputBufferProvider = new ThreadLocalOutputBufferProvider(bufferSize);
    } else {
      // Create a new StringBuilder each time.
      outputBufferProvider = new InstanceOutputBufferProvider(bufferSize);
    }

    // Loads the template from the resource loader, manipulating the AST as
    // required for correctness.
    TemplateFactory templateFactory = new LoadingTemplateFactory();

    // Applies optimizations to improve performance.
    // These steps are entirely optional, and are not required for correctness.
    templateFactory = setupOptimizerFactory(templateFactory);

    TemplateLoader templateLoader;
    List<DelegatingTemplateLoader> delegatingTemplateLoaders =
        new LinkedList<DelegatingTemplateLoader>();
    AutoEscapeOptions autoEscapeOptions = new AutoEscapeOptions();
    autoEscapeOptions.setPropagateEscapeStatus(options.getPropagateEscapeStatus());
    autoEscapeOptions.setLogEscapedVariables(options.getLogEscapedVariables());
    if (options.getCompileTemplates()) {
      // Compiled templates.
      TemplateCompiler compiler =
          new TemplateCompiler(templateFactory, globalFunctions, autoEscapeOptions);
      delegatingTemplateLoaders.add(compiler);
      templateLoader = compiler;
    } else {
      // Walk parse tree every time.
      InterpretedTemplateLoader interpreter =
          new InterpretedTemplateLoader(templateFactory, globalFunctions, autoEscapeOptions);
      delegatingTemplateLoaders.add(interpreter);
      templateLoader = interpreter;
    }

    // Do we want to load precompiled Template class objects?
    if (options.getPrecompiledTemplateMap() != null) {
      // Load precompiled template classes.
      PrecompiledTemplateLoader ptl =
          new PrecompiledTemplateLoader(templateLoader, options.getPrecompiledTemplateMap(),
              globalFunctions, autoEscapeOptions);
      delegatingTemplateLoaders.add(ptl);
      templateLoader = ptl;
    }

    for (DelegatingTemplateLoader loader : delegatingTemplateLoaders) {
      loader.setTemplateLoaderDelegate(templateLoader);
    }
    this.templateLoader = templateLoader;
  }

  /**
   * Applies optimizations to improve performance. These steps are entirely optional, and are not
   * required for correctness.
   */
  private TemplateFactory setupOptimizerFactory(TemplateFactory templateFactory) {
    // DataCommandConsolidator saves state so we need to create a new one
    // every time we run it.
    OptimizerProvider dataCommandConsolidatorProvider = new OptimizerProvider() {
      public Switch getOptimizer() {
        return new DataCommandConsolidator();
      }
    };

    // SyntaxTreeOptimizer has no state so we can use the same object
    // concurrently, but it is cheap to make so lets be consistent.
    OptimizerProvider syntaxTreeOptimizerProvider = new OptimizerProvider() {
      public Switch getOptimizer() {
        return new SyntaxTreeOptimizer();
      }
    };

    OptimizerProvider stripStructuralWhitespaceProvider = null;
    if (options.getStripStructuralWhiteSpace()) {
      // StructuralWhitespaceStripper has state so create a new one each time.
      stripStructuralWhitespaceProvider = new OptimizerProvider() {
        public Switch getOptimizer() {
          return new StructuralWhitespaceStripper();
        }
      };
    }

    return new OptimizingTemplateFactory(templateFactory, dataCommandConsolidatorProvider,
        syntaxTreeOptimizerProvider, stripStructuralWhitespaceProvider);
  }

  /**
   * @param defaultResourceLoader Where resources (templates, HDF files) should be loaded from. e.g.
   *        directory, classpath, memory, etc.
   * @param cacheTemplates Whether to cache templates. Cached templates are much faster but do not
   *        check the filesystem for updates. Use true in prod, false in dev.
   * @deprecated Use {@link #JSilver(ResourceLoader, JSilverOptions)}.
   */
  @Deprecated
  public JSilver(ResourceLoader defaultResourceLoader, boolean cacheTemplates) {
    this(defaultResourceLoader, new JSilverOptions().setCacheTemplates(cacheTemplates));
  }

  /**
   * Creates a JSilver instance with default options.
   * 
   * @param defaultResourceLoader Where resources (templates, HDF files) should be loaded from. e.g.
   *        directory, classpath, memory, etc.
   * @see JSilverOptions
   */
  public JSilver(ResourceLoader defaultResourceLoader) {
    this(defaultResourceLoader, new JSilverOptions());
  }

  /**
   * Renders a given template and provided data, writing to an arbitrary output.
   * 
   * @param templateName Name of template to load (e.g. "things/blah.cs").
   * @param data Data to be used in template.
   * @param output Where template should be rendered to. This can be a Writer, PrintStream,
   *        System.out/err), StringBuffer/StringBuilder or anything that implements Appendable
   * @param resourceLoader How to find the template data to render and any included files it depends
   *        on.
   */
  @Override
  public void render(String templateName, Data data, Appendable output,
      ResourceLoader resourceLoader) throws IOException, JSilverException {
    EscapeMode escapeMode = getEscapeMode(data);
    render(templateLoader.load(templateName, resourceLoader, escapeMode), data, output,
        resourceLoader);
  }

  /**
   * Renders a given template and provided data, writing to an arbitrary output.
   * 
   * @param templateName Name of template to load (e.g. "things/blah.cs").
   * @param data Data to be used in template.
   * @param output Where template should be rendered to. This can be a Writer, PrintStream,
   *        System.out/err), StringBuffer/StringBuilder or anything that implements
   */
  @Override
  public void render(String templateName, Data data, Appendable output) throws IOException,
      JSilverException {
    render(templateName, data, output, defaultResourceLoader);
  }

  /**
   * Same as {@link TemplateRenderer#render(String, Data, Appendable)}, except returns rendered
   * template as a String.
   */
  @Override
  public String render(String templateName, Data data) throws IOException, JSilverException {
    Appendable output = createAppendableBuffer();
    try {
      render(templateName, data, output);
      return output.toString();
    } finally {
      releaseAppendableBuffer(output);
    }
  }

  /**
   * Renders a given template and provided data, writing to an arbitrary output.
   * 
   * @param template Template to load.
   * @param data Data to be used in template.
   * @param output Where template should be rendered to. This can be a Writer, PrintStream,
   *        System.out/err), StringBuffer/StringBuilder or anything that implements
   *        java.io.Appendable.
   */
  @Override
  public void render(Template template, Data data, Appendable output, ResourceLoader resourceLoader)
      throws IOException, JSilverException {
    if (options.getStripHtmlWhiteSpace() && !(output instanceof HtmlWhiteSpaceStripper)) {
      // Strip out whitespace from rendered HTML content.
      output = new HtmlWhiteSpaceStripper(output);
    }
    template.render(data, output, resourceLoader);
  }

  /**
   * Renders a given template and provided data, writing to an arbitrary output.
   * 
   * @param template Template to load.
   * @param data Data to be used in template.
   * @param output Where template should be rendered to. This can be a Writer, PrintStream,
   *        System.out/err), StringBuffer/StringBuilder or anything that implements
   *        java.io.Appendable.
   */
  @Override
  public void render(Template template, Data data, Appendable output) throws IOException,
      JSilverException {
    render(template, data, output, defaultResourceLoader);
  }

  @Override
  public String render(Template template, Data data) throws IOException, JSilverException {
    Appendable output = createAppendableBuffer();
    try {
      render(template, data, output);
      return output.toString();
    } finally {
      releaseAppendableBuffer(output);
    }
  }

  /**
   * Renders a given template from the content passed in. That is, the first parameter is the actual
   * template content rather than the filename to load.
   * 
   * @param content Content of template (e.g. "Hello &lt;cs var:name ?&gt;").
   * @param data Data to be used in template.
   * @param output Where template should be rendered to. This can be a Writer, PrintStream,
   *        System.out/err), StringBuffer/StringBuilder or anything that implements
   *        java.io.Appendable
   */
  @Override
  public void renderFromContent(String content, Data data, Appendable output) throws IOException,
      JSilverException {
    EscapeMode escapeMode = getEscapeMode(data);
    render(templateLoader.createTemp("[renderFromContent]", content, escapeMode), data, output);
  }

  /**
   * Same as {@link #renderFromContent(String, Data, Appendable)}, except returns rendered template
   * as a String.
   */
  @Override
  public String renderFromContent(String content, Data data) throws IOException, JSilverException {
    Appendable output = createAppendableBuffer();
    try {
      renderFromContent(content, data, output);
      return output.toString();
    } finally {
      releaseAppendableBuffer(output);
    }
  }

  /**
   * Determine the escaping to apply based on Config variables in HDF. If there is no escaping
   * specified in the HDF, check whether JSilverOptions has any escaping configured.
   * 
   * @param data HDF Data to check
   * @return EscapeMode
   */
  public EscapeMode getEscapeMode(Data data) {
    EscapeMode escapeMode =
        EscapeMode.computeEscapeMode(data.getValue(VAR_ESCAPE_MODE_KEY), data
            .getBooleanValue(AUTO_ESCAPE_KEY));
    if (escapeMode.equals(EscapeMode.ESCAPE_NONE)) {
      escapeMode = options.getEscapeMode();
    }

    return escapeMode;
  }

  /**
   * Override this to change the type of Appendable buffer used in {@link #render(String, Data)}.
   */
  public Appendable createAppendableBuffer() {
    return outputBufferProvider.get();
  }

  public void releaseAppendableBuffer(Appendable buffer) {
    outputBufferProvider.release(buffer);
  }

  /**
   * Registers a global Function that can be used from any template.
   */
  public void registerGlobalFunction(String name, Function function) {
    globalFunctions.registerFunction(name, function);
  }

  /**
   * Registers a global TextFilter as function that can be used from any template.
   */
  public void registerGlobalFunction(String name, TextFilter textFilter) {
    globalFunctions.registerFunction(name, textFilter);
  }

  /**
   * Registers a global escaper. This also makes it available as a Function named with "_escape"
   * suffix (e.g. "html_escape").
   */
  public void registerGlobalEscaper(String name, TextFilter escaper) {
    globalFunctions.registerFunction(name + "_escape", escaper, true);
    globalFunctions.registerEscapeMode(name, escaper);
  }

  /**
   * Create new Data instance, ready to be populated.
   */
  public Data createData() {
    return dataFactory.createData();
  }

  /**
   * Loads data in Hierarchical Data Format (HDF) into an existing Data object.
   */
  @Override
  public void loadData(String dataFileName, Data output) throws JSilverBadSyntaxException,
      IOException {
    dataFactory.loadData(dataFileName, defaultResourceLoader, output);
  }

  /**
   * Loads data in Hierarchical Data Format (HDF) into a new Data object.
   */
  @Override
  public Data loadData(String dataFileName) throws IOException {
    return dataFactory.loadData(dataFileName, defaultResourceLoader);
  }

  /**
   * Gets underlying ResourceLoader so you can access arbitrary files using the same mechanism as
   * JSilver.
   */
  public ResourceLoader getResourceLoader() {
    return defaultResourceLoader;
  }

  /**
   * Force all cached templates to be cleared.
   */
  public void clearCache() {

  }

  /**
   * Returns the TemplateLoader used by this JSilver template renderer. Needed for HDF/CS
   * compatbility.
   */
  public TemplateLoader getTemplateLoader() {
    return templateLoader;
  }

  /**
   * Returns a copy of the JSilverOptions used by this JSilver instance.
   */
  public JSilverOptions getOptions() {
    return options.clone();
  }
}
