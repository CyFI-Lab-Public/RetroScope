/*
 *  Copyright 2010 Google Inc.
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
package com.google.android.testing.mocking;

/**
 * Defines the getDelegate___AndroidMock method used by Android Mock for
 * delegating Android Mock calls to the EasyMock generated MockObject.
 * 
 * @author swoodward@google.com (Stephen Woodward)
 */
public interface MockObject {
  /**
   * Accessor method to get the wrapped EasyMock mock object.
   * 
   * @return a mock object created by EasyMock and wrapped by the object
   *         implementing this method.
   */
  Object getDelegate___AndroidMock();
}
