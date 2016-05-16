/*
 * Copyright 2010-2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef BCC_SCRIPT_H
#define BCC_SCRIPT_H

namespace bcc {

class Source;

class Script {
private:
  // This is the source associated with this object and is going to be
  // compiled.
  Source *mSource;

protected:
  // This hook will be invoked after the script object is successfully reset.
  virtual bool doReset()
  { return true; }

public:
  Script(Source &pSource) : mSource(&pSource) { }

  virtual ~Script() { }

  // Reset this object with the new source supplied. Return false if this
  // object remains unchanged after the call (e.g., the supplied source is
  // the same with the one contain in this object.) If pPreserveCurrent is
  // false, the current containing source will be destroyed after successfully
  // reset.
  bool reset(Source &pSource, bool pPreserveCurrent = false);

  // Merge (or link) another source into the current source associated with
  // this Script object. Return false on error.
  //
  // This is equivalent to the call to Script::merge(...) on mSource.
  bool mergeSource(Source &pSource, bool pPreserveSource = false);

  inline Source &getSource()
  { return *mSource; }
  inline const Source &getSource() const
  { return *mSource; }
};

} // end namespace bcc

#endif  // BCC_SCRIPT_H
