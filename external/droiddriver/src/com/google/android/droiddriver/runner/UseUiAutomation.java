/*
 * Copyright (C) 2013 DroidDriver committers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.droiddriver.runner;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates that its target needs
 * {@link android.app.UiAutomation}. It is effectively equivalent to
 * {@code @MinSdkVersion(Build.VERSION_CODES.JELLY_BEAN_MR2)}, just more
 * explicit.
 * <p>
 * As any annotations, it is useful only if it is processed by tools.
 * {@link TestRunner} filters out tests with this annotation if the current
 * device has SDK version below 18 (JELLY_BEAN_MR2).
 */
@Inherited
@Target({TYPE, METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UseUiAutomation {
}
