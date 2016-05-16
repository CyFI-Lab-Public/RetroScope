/*
 * Copyright (C) 2009 The Android Open Source Project
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

package dex.structure;

/**
 * {@code DexAnnotationValue} is implemented by annotation values. In the
 * following example:
 * 
 * <pre>
 * &#064;Text(value=&quot;hello&quot;)
 * </pre>
 * 
 * 'value="hello"' is represented by a {@code DexAnnotationValue}. "value" is
 * its name and "hello" is its encoded value.
 */
public interface DexAnnotationAttribute extends NamedElement {
    /**
     * Returns the encoded value of this {@code DexAnnotationValue}.
     * 
     * @return the encoded value of this {@code DexAnnotationValue}
     */
    public DexEncodedValue getEncodedValue();

    public DexAnnotation getAnnotation();
}
