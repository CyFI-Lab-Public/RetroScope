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

import java.util.List;
import java.util.Set;

import signature.model.ITypeReference;

/**
 * {@code IUpperBoundsDelta} models the delta between two {@link List
 * &lt;ITypeReference&gt;} instances.
 * <p>
 * This interface is aware, that for the first argument, the order of the upper
 * bounds is relevant (for erasure).
 */
public interface IUpperBoundsDelta extends IDelta<List<ITypeReference>> {
    
    /**
     * Returns the upper bound delta of the first upper bound or {@code null} if
     * no delta is available.
     *
     * @return the upper bound delta of the first upper bound, maybe {@code
     *         null}
     */
    ITypeReferenceDelta<?> getFirstUpperBoundDelta();

    /**
     * Returns a set of remaining upper bound deltas or {@code null} if no
     * deltas are available.
     *
     * @return a set of remaining upper bound deltas, maybe {@code null}
     */
    Set<ITypeReferenceDelta<?>> getRemainingUpperBoundDeltas();
}
