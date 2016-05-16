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

package signature.model;

/**
 * {@code IPrimitiveType} models a primitive type.
 */
public interface IPrimitiveType extends ITypeReference {

    /**
     * Returns the name of this primitive type, i.e. one of the following
     * strings:
     * <ul>
     * <li><code>boolean</code>
     * <li><code>byte</code>
     * <li><code>char</code>
     * <li><code>short</code>
     * <li><code>int</code>
     * <li><code>float</code>
     * <li><code>double</code>
     * <li><code>void</code>
     * </ul>
     * 
     * @return the name of this primitive type
     */
    public String getName();
}
