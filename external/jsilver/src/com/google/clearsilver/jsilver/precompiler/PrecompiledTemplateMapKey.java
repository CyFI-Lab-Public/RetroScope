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

import com.google.clearsilver.jsilver.autoescape.EscapeMode;

/**
 * Object to use as key when looking up precompiled templates. It encapsulates the template name, as
 * well as the {@link EscapeMode} for which the template was compiled.
 */
public class PrecompiledTemplateMapKey {
  private final Object templateName;
  private final EscapeMode escapeMode;
  private final String toStringName;

  public PrecompiledTemplateMapKey(Object templateName, EscapeMode escapeMode) {
    this.templateName = templateName;
    this.escapeMode = escapeMode;

    if (escapeMode == EscapeMode.ESCAPE_NONE) {
      toStringName = templateName.toString();
    } else {
      toStringName = templateName.toString() + "_" + escapeMode.getEscapeCommand();
    }
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PrecompiledTemplateMapKey that = (PrecompiledTemplateMapKey) o;

    return templateName.equals(that.templateName) && (escapeMode == that.escapeMode);
  }

  public int hashCode() {
    int hash = 17;

    hash = 31 * hash + templateName.hashCode();
    hash = 31 * hash + escapeMode.hashCode();
    return hash;
  }

  /**
   * String representation of key. If the template was auto escaped, it appends the
   * {@link EscapeMode} to the template name.
   * 
   */
  public String toString() {
    return toStringName;
  }

  /**
   * Return the escape mode used for this template.
   */
  public EscapeMode getEscapeMode() {
    return escapeMode;
  }
}
