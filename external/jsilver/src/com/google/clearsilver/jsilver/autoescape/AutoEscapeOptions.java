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

package com.google.clearsilver.jsilver.autoescape;

/**
 * Global configuration options specific to <a href="http://go/autoescapecs">auto escaping</a>.
 */
public class AutoEscapeOptions {

  private boolean propagateEscapeStatus = false;
  private boolean logEscapedVariables = false;

  public boolean getLogEscapedVariables() {
    return logEscapedVariables;
  }

  public void setLogEscapedVariables(boolean logEscapedVariables) {
    this.logEscapedVariables = logEscapedVariables;
  }

  public boolean getPropagateEscapeStatus() {
    return propagateEscapeStatus;
  }

  public void setPropagateEscapeStatus(boolean propagateEscapeStatus) {
    this.propagateEscapeStatus = propagateEscapeStatus;
  }

}
