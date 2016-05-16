/*
 * Copyright 2011, The Android Open Source Project
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

#ifndef STUB_LAYOUT_H
#define STUB_LAYOUT_H

#include <map>
#include <stdlib.h>

class StubLayout {
private:
  unsigned char *table;
  size_t count;

  std::map<void *, void *> stub_index;

public:
  StubLayout();
  virtual ~StubLayout() { }

  void initStubTable(unsigned char *table, size_t count);
  void *allocateStub(void *addr = 0);

  size_t calcStubTableSize(size_t count) const;
  virtual size_t getUnitStubSize() const = 0;

private:
  virtual void setStubAddress(void *stub, void *addr) = 0;

};

class StubLayoutARM : public StubLayout {
public:
  StubLayoutARM() { }
  size_t getUnitStubSize() const;

private:
  virtual void setStubAddress(void *stub, void *addr);
};

class StubLayoutMIPS : public StubLayout {
public:
  StubLayoutMIPS() { }
  size_t getUnitStubSize() const;

private:
  virtual void setStubAddress(void *stub, void *addr);
};


#endif // STUB_LAYOUT_H
