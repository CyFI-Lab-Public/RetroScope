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

package com.google.clearsilver.jsilver.template;

/**
 * Interface that extends TemplateLoader with a method to set the TemplateLoader that Templates
 * provided by this TemplateLoader should use to resolve includes and such. Useful callback for
 * informing TemplateLoaders in the chain what the topmost TemplateLoader is.
 */
public interface DelegatingTemplateLoader extends TemplateLoader {

  /**
   * TemplateLoader that Templates will delegate back to for includes etc.
   */
  public void setTemplateLoaderDelegate(TemplateLoader templateLoaderDelegate);
}
