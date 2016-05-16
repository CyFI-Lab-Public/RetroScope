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

package com.google.clearsilver.jsilver.values;

import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.data.DataContext;
import com.google.clearsilver.jsilver.data.TypeConverter;

/**
 * A value linked to a variable reference. When this value is evaluated in an expression a Data
 * object will be fetched from the provided DataContext and and its value returned or {@code null}
 * if there is no Data object associated with the given name.
 * 
 * @see Value
 * @see Data
 */
public class VariableValue extends VariantValue {
  // TODO: Make this non-public and come up with a smarter way
  // for it to be used elsewhere without having to be used in a cast.

  private final String name;
  private final DataContext dataContext;

  private boolean gotRef = false;
  private Data reference;

  public VariableValue(String name, DataContext dataContext) {
    // VariableValues always have partiallyEscaped=false since they represent
    // a Data object, not a compound expression containing escaping functions.
    // We override getEscapeMode() to return the Data object's escape mode,
    // so the mode we pass here is just ignored.
    super(EscapeMode.ESCAPE_NONE, false);
    this.dataContext = dataContext;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Data getReference() {
    if (!gotRef) {
      reference = dataContext.findVariable(name, false);
      gotRef = true;
    }
    return reference;
  }

  @Override
  protected String value() {
    Data data = getReference();
    return data == null ? null : data.getValue();
  }

  @Override
  public boolean exists() {
    return TypeConverter.exists(getReference());
  }

  // Note we are not overriding asString as we want that to return the value
  // of the node located at this address.
  @Override
  public String toString() {
    return name;
  }

  @Override
  public EscapeMode getEscapeMode() {
    Data data = getReference();
    if (data == null) {
      return super.getEscapeMode();
    }
    return data.getEscapeMode();
  }

}
