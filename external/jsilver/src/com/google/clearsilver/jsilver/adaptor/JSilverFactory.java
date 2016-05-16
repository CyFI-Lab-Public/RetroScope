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
import com.google.clearsilver.jsilver.JSilverOptions;
import com.google.clearsilver.jsilver.data.DataFactory;
import com.google.clearsilver.jsilver.data.DefaultData;
import com.google.clearsilver.jsilver.data.HDFDataFactory;

import org.clearsilver.ClearsilverFactory;
import org.clearsilver.DelegatedHdf;
import org.clearsilver.HDF;

/**
 * ClearsilverFactory that adapts JSilver for use as any HDF/CS rendering engine.
 */
public class JSilverFactory implements ClearsilverFactory {

  private static final JSilverOptions DEFAULT_OPTIONS = new JSilverOptions();

  private final boolean unwrapDelegatedHdfs;
  private final JSilver jSilver;
  private final JSilverOptions options;
  private final DataFactory dataFactory;

  private final LoadPathToFileCache loadPathCache;

  /**
   * Default constructor. enables unwrapping of DelegatedHdfs.
   */
  public JSilverFactory() {
    this(DEFAULT_OPTIONS);
  }

  public JSilverFactory(JSilverOptions options) {
    this(options, true);
  }

  public JSilverFactory(JSilverOptions options, boolean unwrapDelegatedHdfs) {
    this(new JSilver(null, options), unwrapDelegatedHdfs);
  }

  /**
   * This constructor is available for those who already use JSilver and want to use the same
   * attributes and caches for their Java Clearsilver Framework code. Users who use only JCF should
   * use a different constructor.
   * 
   * @param jSilver existing instance of JSilver to use for parsing and rendering.
   * @param unwrapDelegatedHdfs whether to unwrap DelegetedHdfs or not before casting.
   */
  public JSilverFactory(JSilver jSilver, boolean unwrapDelegatedHdfs) {
    this.unwrapDelegatedHdfs = unwrapDelegatedHdfs;
    this.jSilver = jSilver;
    this.options = jSilver.getOptions();
    if (this.options.getLoadPathCacheSize() == 0) {
      this.loadPathCache = null;
    } else {
      this.loadPathCache = new LoadPathToFileCache(this.options.getLoadPathCacheSize());
    }
    this.dataFactory =
        new HDFDataFactory(options.getIgnoreAttributes(), options.getStringInternStrategy());
  }

  @Override
  public JCs newCs(HDF hdf) {
    if (unwrapDelegatedHdfs) {
      hdf = DelegatedHdf.getFullyUnwrappedHdf(hdf);
    }
    return new JCs(JHdf.cast(hdf), jSilver, loadPathCache);
  }

  @Override
  public JCs newCs(HDF hdf, HDF globalHdf) {
    if (unwrapDelegatedHdfs) {
      hdf = DelegatedHdf.getFullyUnwrappedHdf(hdf);
      globalHdf = DelegatedHdf.getFullyUnwrappedHdf(globalHdf);
    }
    JCs cs = new JCs(JHdf.cast(hdf), jSilver, loadPathCache);
    cs.setGlobalHDF(globalHdf);
    return cs;
  }

  @Override
  public JHdf newHdf() {
    return new JHdf(new DefaultData(), dataFactory, loadPathCache, options);
  }
}
