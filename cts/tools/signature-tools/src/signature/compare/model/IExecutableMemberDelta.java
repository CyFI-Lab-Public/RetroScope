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

import signature.model.IExecutableMember;

/**
 * {@code IExecutableMemberDelta} models the delta between two
 * {@link IExecutableMember} instances.
 */
public interface IExecutableMemberDelta<T extends IExecutableMember> extends
        IDelta<T>, ITypeVariableDeltas, IAnnotatableElementDelta {

    /**
     * Returns a set of modifier deltas or {@code null} if no deltas are
     * available.
     * 
     * @return a set of modifier deltas, maybe {@code null}
     */
    Set<IModifierDelta> getModifierDeltas();

    /**
     * Returns a set of exception deltas or {@code null} if no deltas are
     * available.
     * 
     * @return a set of exception deltas, maybe {@code null}
     */
    Set<ITypeReferenceDelta<?>> getExceptionDeltas();

    /**
     * Returns a set of parameter deltas or {@code null} if no deltas are
     * available.
     * 
     * @return a set of parameter deltas, maybe {@code null}
     */
    Set<IParameterDelta> getParameterDeltas();
}
