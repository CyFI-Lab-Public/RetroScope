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

import java.util.List;

/**
 * {@code IParameterizedType} models a parameterized type. A parameterized type
 * instantiates a generic class definition with actual type arguments.
 * 
 * <pre>
 * IClassDefinition:
 * public class A<T> {}
 *
 * public class XXX {
 * IParameterizedType (raw type: A , type arguments: Number)
 *   public A<Number> fields;
 * }
 * </pre>
 */
public interface IParameterizedType extends ITypeReference {

    /**
     * Returns the actual type arguments of this parameterized type.
     * 
     * @return the actual type arguments of this parameterized type
     */
    List<ITypeReference> getTypeArguments();

    /**
     * Returns the raw type of this parameterized type.
     * 
     * @return the raw type of this parameterized type
     */
    IClassReference getRawType();

    /**
     * Returns the owner type of this parameterized type or {@code null}.
     * 
     * <pre>
     * class Y&lt;T&gt; {
     *     class Z&lt;S&gt; {
     *     }
     *
     *     Y&lt;Integer&gt;.Z&lt;String&gt; a;
     * }
     * </pre>
     */
    ITypeReference getOwnerType(); // A.B<String> -> A
}
