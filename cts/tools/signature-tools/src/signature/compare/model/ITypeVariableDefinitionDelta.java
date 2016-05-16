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

import signature.model.ITypeVariableDefinition;

/**
 * {@code ITypeVariableDefinitionDelta} models the delta between two
 * {@link ITypeVariableDefinition} instances.
 */
public interface ITypeVariableDefinitionDelta extends
        ITypeDefinitionDelta<ITypeVariableDefinition> {

    /**
     * Returns an upper bound delta or {@code null} if no delta is available.
     * 
     * @return an upper bound delta, maybe {@code null}
     */
    IUpperBoundsDelta getUpperBoundsDelta();

    /**
     * Returns a generic declaration delta or {@code null} if no delta is
     * available.
     * 
     * @return a generic declaration delta, maybe {@code null}
     */
    IGenericDeclarationDelta getGenericDeclarationDelta();
}
