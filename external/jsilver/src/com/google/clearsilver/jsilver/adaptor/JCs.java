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

package com.google.clearsilver.jsilver.adaptor;

import com.google.clearsilver.jsilver.JSilver;
import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.data.LocalAndGlobalData;
import com.google.clearsilver.jsilver.exceptions.JSilverIOException;
import com.google.clearsilver.jsilver.template.HtmlWhiteSpaceStripper;
import com.google.clearsilver.jsilver.template.Template;

import org.clearsilver.CS;
import org.clearsilver.CSFileLoader;
import org.clearsilver.HDF;

import java.io.IOException;

/**
 * Adaptor that wraps a JSilver object so it can be used as an CS object.
 */
class JCs implements CS {

  private final JHdf localHdf;
  private JHdf globalHdf;
  private final JSilver jSilver;
  private final LoadPathToFileCache loadPathCache;
  private Template template = null;
  private CSFileLoader csFileLoader;
  private ResourceLoaderAdaptor resourceLoaderAdaptor;

  JCs(JHdf hdf, JSilver jSilver, LoadPathToFileCache loadPathCache) {
    this.localHdf = hdf;
    this.jSilver = jSilver;
    this.loadPathCache = loadPathCache;

    resourceLoaderAdaptor = localHdf.getResourceLoaderAdaptor();
    csFileLoader = resourceLoaderAdaptor.getCSFileLoader();
  }

  /**
   * Want to delay creating the JSilver object so we can specify necessary parameters.
   */
  private JSilver getJSilver() {
    return jSilver;
  }

  @Override
  public void setGlobalHDF(HDF global) {
    globalHdf = JHdf.cast(global);
  }

  @Override
  public HDF getGlobalHDF() {
    return globalHdf;
  }

  @Override
  public void close() {
    // Removing unneeded reference, although this is not expected to have the
    // performance impact seen in JHdf as in production configurations users
    // should be using cached templates so they are long-lived.
    template = null;
  }

  @Override
  public void parseFile(String filename) throws IOException {
    try {
      if (getEscapeMode().isAutoEscapingMode()) {
        if (localHdf.getData().getValue("Config.PropagateEscapeStatus") != null) {
          throw new IllegalArgumentException(
              "Config.PropagateEscapeStatus does not work with JSilver."
                  + "Use JSilverOptions.setPropagateEscapeStatus instead");
        }
      }
      template =
          getJSilver().getTemplateLoader().load(filename, resourceLoaderAdaptor, getEscapeMode());
    } catch (RuntimeException e) {
      Throwable th = e;
      if (th instanceof JSilverIOException) {
        // JSilverIOException always has an IOException as its cause.
        throw ((IOException) th.getCause());
      }
      throw e;
    }
  }

  @Override
  public void parseStr(String content) {
    if (getEscapeMode().isAutoEscapingMode()) {
      if (localHdf.getData().getValue("Config.PropagateEscapeStatus") != null) {
        throw new IllegalArgumentException(
            "Config.PropagateEscapeStatus does not work with JSilver."
                + "Use JSilverOptions.setPropagateEscapeStatus instead");
      }
    }
    template = getJSilver().getTemplateLoader().createTemp("parseStr", content, getEscapeMode());
  }

  private EscapeMode getEscapeMode() {
    Data data = localHdf.getData();
    return getJSilver().getEscapeMode(data);
  }

  @Override
  public String render() {
    if (template == null) {
      throw new IllegalStateException("Call parseFile() or parseStr() before " + "render()");
    }

    Data data;
    if (globalHdf != null) {
      // For legacy support we allow users to pass in this option to disable
      // the new modification protection for global HDF.
      data =
          new LocalAndGlobalData(localHdf.getData(), globalHdf.getData(), jSilver.getOptions()
              .getAllowGlobalDataModification());
    } else {
      data = localHdf.getData();
    }
    Appendable buffer = jSilver.createAppendableBuffer();
    try {
      Appendable output = buffer;
      // For Clearsilver compatibility we check this HDF variable to see if we
      // need to turn on whitespace stripping. The preferred approach would be
      // to turn it on in the JSilverOptions passed to JSilverFactory
      int wsStripLevel = localHdf.getIntValue("ClearSilver.WhiteSpaceStrip", 0);
      if (wsStripLevel > 0) {
        output = new HtmlWhiteSpaceStripper(output, wsStripLevel);
      }
      jSilver.render(template, data, output, resourceLoaderAdaptor);
      return output.toString();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    } finally {
      jSilver.releaseAppendableBuffer(buffer);
    }
  }

  @Override
  public CSFileLoader getFileLoader() {
    return csFileLoader;
  }

  @Override
  public void setFileLoader(CSFileLoader fileLoader) {
    if (fileLoader == null && csFileLoader == null) {
      return;
    }
    if (fileLoader != null && fileLoader.equals(csFileLoader)) {
      return;
    }
    csFileLoader = fileLoader;
    resourceLoaderAdaptor = new ResourceLoaderAdaptor(localHdf, loadPathCache, csFileLoader);
  }
}
