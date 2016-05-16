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

package signature.compare.model;

import java.util.Set;

import signature.model.IParameterizedType;

/**
 * {@code IParameterizedTypeDelta} models the delta between two
 * {@link IParameterizedType} instances.
 */
public interface IParameterizedTypeDelta extends
        ITypeReferenceDelta<IParameterizedType> {

    /**
     * Returns a raw type delta or {@code null} if no delta is available.
     * 
     * @return a raw type delta, maybe {@code null}
     */
    ITypeReferenceDelta<?> getRawTypeDelta();

    /**
     * Returns a owner type delta or {@code null} if no delta is available.
     * 
     * @return a owner type delta, maybe {@code null}
     */
    ITypeReferenceDelta<?> getOwnerTypeDelta();

    /**
     * Returns a set of argument type deltas or {@code null} if no deltas are
     * available.
     * 
     * @return a set of argument type deltas, maybe {@code null}
     */
    Set<ITypeReferenceDelta<?>> getArgumentTypeDeltas();
}
