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

import signature.model.IClassDefinition;

/**
 * {@code IClassDefinitionDelta} models the delta between two
 * {@link IClassDefinition} instances.
 */
public interface IClassDefinitionDelta extends
        ITypeDefinitionDelta<IClassDefinition>, IAnnotatableElementDelta,
        ITypeVariableDeltas {

    /**
     * Returns a set of field deltas or {@code null} if no deltas are available.
     * 
     * @return a set of field deltas, maybe {@code null}
     */
    Set<IFieldDelta> getFieldDeltas();

    /**
     * Returns a set of enum constant deltas or {@code null} if no deltas are
     * available.
     * 
     * @return a set of enum constant deltas, maybe {@code null}
     */
    Set<IEnumConstantDelta> getEnumConstantDeltas();

    /**
     * Returns a set of annotation field deltas or {@code null} if no deltas are
     * available.
     * 
     * @return a set of annotation field deltas, maybe {@code null}
     */
    Set<IAnnotationFieldDelta> getAnnotationFieldDeltas();

    /**
     * Returns a set of method deltas or {@code null} if no deltas are
     * available.
     * 
     * @return a set of method deltas, maybe {@code null}
     */
    Set<IMethodDelta> getMethodDeltas();

    /**
     * Returns a set of constructor deltas or {@code null} if no deltas are
     * available.
     * 
     * @return a set of constructor deltas, maybe {@code null}
     */
    Set<IConstructorDelta> getConstructorDeltas();

    /**
     * Returns a set of modifier deltas or {@code null} if no deltas are
     * available.
     * 
     * @return a set of modifier deltas, maybe {@code null}
     */
    Set<IModifierDelta> getModifierDeltas();

    /**
     * Returns a super class delta or {@code null} if no delta is available.
     * 
     * @return a super class delta, maybe {@code null}
     */
    ITypeReferenceDelta<?> getSuperClassDelta();

    /**
     * Returns a set of interface deltas or {@code null} if no deltas are
     * available.
     * 
     * @return a set of interface deltas, maybe {@code null}
     */
    Set<ITypeReferenceDelta<?>> getInterfaceDeltas();
}
