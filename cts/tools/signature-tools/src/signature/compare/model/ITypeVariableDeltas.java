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

/**
 * Common interface for all deltas which may have {@code
 * ITypeVariableDefinitionDelta}s. Type variable deltas can occur, where type
 * variables can be defined. On classes, constructors and methods.
 * 
 * @see IClassDefinitionDelta
 * @see IExecutableMemberDelta (common super interface of the following two
 *      interfaces)
 * @see IConstructorDelta
 * @see IMethodDelta
 */
public interface ITypeVariableDeltas {

    /**
     * Returns a set of type variable deltas or {@code null} if no deltas are
     * available.
     * 
     * @return a set of type variable deltas, maybe {@code null}
     */
    Set<ITypeVariableDefinitionDelta> getTypeVariableDeltas();
}
