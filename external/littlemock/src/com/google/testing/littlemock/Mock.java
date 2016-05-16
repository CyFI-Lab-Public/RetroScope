/*
 * Copyright 2011 Google Inc.
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

package com.google.testing.littlemock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this to inject a mock into your test class.
 *
 * <p>Just add the correct annotation to your field like this:
 * <pre>
 *     &#64;Mock private MyInterface mMockMyInterface;
 * </pre>
 *
 * <p>You just have to make sure that {@link LittleMock#initMocks(Object)} is called from
 * your setUp() (or from a test base class), and then all your mocks will be automatically
 * constructed for you.
 *
 * @author hugohudson@gmail.com (Hugo Hudson)
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Mock {
}
